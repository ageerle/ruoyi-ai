#requires -Version 7.0
[CmdletBinding()]
param(
    [string]$DockerExecutable = "C:/Program Files/Docker/Docker/resources/bin/docker.exe",
    [string]$ExpectedDockerExecutableSha256 = "83f31577a39b1c9f48c571cc2c31c99d74f74df1ab44003b4022301ed30a9831",
    [string]$ExpectedDockerfileSha256 = "72c262c9396e59359e52edddb9963360da4c581298247231f6ab2e369ce43eef",
    [string]$ExpectedDockerignoreSha256 = "16150e43ce98c333f93ad82a4991269a89bdd5596c427b4f5066ad266d6f871d",
    [string]$BuildDockerConfigDirectory = "C:/Users/wl/.ruoyi-harness/evaluator/docker-build-config",
    [string]$RuntimeDockerConfigDirectory = "C:/Users/wl/.ruoyi-harness/evaluator/docker-runtime-empty-config",
    [string]$DockerHost = "npipe:////./pipe/docker_engine",
    [string]$SourceContextDirectory = $PSScriptRoot,
    [string]$ImageTag = "ruoyi-harness-toolchain:local"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
$sourceContext = [System.IO.Path]::GetFullPath($SourceContextDirectory)
if (-not [System.IO.Directory]::Exists($sourceContext) -or
    ([System.IO.DirectoryInfo]::new($sourceContext).Attributes -band [System.IO.FileAttributes]::ReparsePoint) -ne 0 -or
    -not [System.IO.File]::Exists((Join-Path $sourceContext "Dockerfile")) -or
    -not [System.IO.File]::Exists((Join-Path $sourceContext ".dockerignore"))) {
    throw "BUILD_SOURCE_CONTEXT_INVALID"
}

function Assert-NoBroadWriteAcl {
    param([Parameter(Mandatory)][string]$LiteralPath)

    $trustedWriterSids = [System.Collections.Generic.HashSet[string]]::new(
        [System.StringComparer]::OrdinalIgnoreCase)
    $null = $trustedWriterSids.Add("S-1-5-18")      # SYSTEM
    $null = $trustedWriterSids.Add("S-1-5-32-544") # BUILTIN\Administrators
    $null = $trustedWriterSids.Add([System.Security.Principal.WindowsIdentity]::GetCurrent().User.Value)
    try {
        $trustedInstallerSid = ([System.Security.Principal.NTAccount]::new(
            "NT SERVICE", "TrustedInstaller")).Translate(
                [System.Security.Principal.SecurityIdentifier]).Value
        $null = $trustedWriterSids.Add($trustedInstallerSid)
    } catch {
        throw "TRUSTED_INSTALLER_IDENTITY_UNRESOLVED"
    }
    $writeMask = [System.Security.AccessControl.FileSystemRights]::WriteData -bor
        [System.Security.AccessControl.FileSystemRights]::AppendData -bor
        [System.Security.AccessControl.FileSystemRights]::WriteExtendedAttributes -bor
        [System.Security.AccessControl.FileSystemRights]::WriteAttributes -bor
        [System.Security.AccessControl.FileSystemRights]::DeleteSubdirectoriesAndFiles -bor
        [System.Security.AccessControl.FileSystemRights]::Delete -bor
        [System.Security.AccessControl.FileSystemRights]::ChangePermissions -bor
        [System.Security.AccessControl.FileSystemRights]::TakeOwnership
    $target = [System.IO.Path]::GetFullPath($LiteralPath)
    $paths = @($target)
    $parent = [System.IO.Directory]::GetParent($target)
    if ($null -ne $parent) { $paths += $parent.FullName }
    foreach ($path in $paths) {
        $acl = Get-Acl -LiteralPath $path
        $ownerSid = ([System.Security.Principal.NTAccount]$acl.Owner).Translate(
            [System.Security.Principal.SecurityIdentifier]).Value
        if (-not $trustedWriterSids.Contains($ownerSid)) {
            throw "UNTRUSTED_EVALUATOR_ACL_OWNER"
        }
        foreach ($rule in $acl.Access) {
            if ($rule.AccessControlType -ne [System.Security.AccessControl.AccessControlType]::Allow -or
                ($rule.FileSystemRights -band $writeMask) -eq 0) {
                continue
            }
            try {
                $sid = $rule.IdentityReference.Translate([System.Security.Principal.SecurityIdentifier]).Value
            } catch {
                throw "UNRESOLVED_EVALUATOR_ACL_IDENTITY"
            }
            if (-not $trustedWriterSids.Contains($sid)) {
                throw "UNTRUSTED_WRITE_EVALUATOR_ACL_DENIED"
            }
        }
    }
}

function Invoke-IsolatedDocker {
    param(
        [Parameter(Mandatory)][string]$Executable,
        [Parameter(Mandatory)][string[]]$ArgumentList,
        [Parameter(Mandatory)][string]$TempDirectory,
        [ValidateRange(1, 600000)][int]$TimeoutMs = 120000,
        [switch]$EchoOutput
    )

    $startInfo = [System.Diagnostics.ProcessStartInfo]::new()
    $startInfo.FileName = $Executable
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    foreach ($argument in $ArgumentList) {
        $startInfo.ArgumentList.Add($argument)
    }
    $startInfo.Environment.Clear()
    $windowsDirectory = [Environment]::GetFolderPath([Environment+SpecialFolder]::Windows)
    $startInfo.Environment["SystemRoot"] = $windowsDirectory
    $startInfo.Environment["WINDIR"] = $windowsDirectory
    $startInfo.Environment["TEMP"] = $TempDirectory
    $startInfo.Environment["TMP"] = $TempDirectory

    $process = [System.Diagnostics.Process]::new()
    $process.StartInfo = $startInfo
    if (-not $process.Start()) {
        throw "TRUSTED_DOCKER_PROCESS_START_FAILED"
    }
    $stdoutTask = $process.StandardOutput.ReadToEndAsync()
    $stderrTask = $process.StandardError.ReadToEndAsync()
    if (-not $process.WaitForExit($TimeoutMs)) {
        try {
            $process.Kill($true)
            $null = $process.WaitForExit(5000)
        } finally {
            $process.Dispose()
        }
        throw "TRUSTED_DOCKER_PROCESS_TIMEOUT"
    }
    $stdout = $stdoutTask.GetAwaiter().GetResult()
    $stderr = $stderrTask.GetAwaiter().GetResult()
    if ($EchoOutput) {
        if (-not [string]::IsNullOrEmpty($stdout)) { [Console]::Out.Write($stdout) }
        if (-not [string]::IsNullOrEmpty($stderr)) { [Console]::Error.Write($stderr) }
    }
    $result = [pscustomobject]@{
        ExitCode = $process.ExitCode
        Stdout = $stdout
        Stderr = $stderr
    }
    $process.Dispose()
    $result
}

$docker = [System.IO.Path]::GetFullPath($DockerExecutable)
if (-not [System.IO.File]::Exists($docker) -or
    -not [System.IO.Path]::GetFileName($docker).Equals("docker.exe", [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "TRUSTED_DOCKER_CLI_REQUIRED"
}
$dockerInfo = [System.IO.FileInfo]::new($docker)
if (($dockerInfo.Attributes -band [System.IO.FileAttributes]::ReparsePoint) -ne 0) {
    throw "TRUSTED_DOCKER_CLI_REPARSE_DENIED"
}
if ($ExpectedDockerExecutableSha256 -notmatch '^[0-9a-fA-F]{64}$') {
    throw "TRUSTED_DOCKER_CLI_DIGEST_REQUIRED"
}
$dockerSha256 = (Get-FileHash -LiteralPath $docker -Algorithm SHA256).Hash.ToLowerInvariant()
if (-not $dockerSha256.Equals($ExpectedDockerExecutableSha256.ToLowerInvariant(), [System.StringComparison]::Ordinal)) {
    throw "TRUSTED_DOCKER_CLI_DIGEST_MISMATCH"
}
Assert-NoBroadWriteAcl -LiteralPath $docker

$buildConfigRoot = [System.IO.Path]::GetFullPath($BuildDockerConfigDirectory)
$runtimeConfig = [System.IO.Path]::GetFullPath($RuntimeDockerConfigDirectory)
[System.IO.Directory]::CreateDirectory($buildConfigRoot) | Out-Null
[System.IO.Directory]::CreateDirectory($runtimeConfig) | Out-Null
$buildConfigRootInfo = [System.IO.DirectoryInfo]::new($buildConfigRoot)
$runtimeConfigInfo = [System.IO.DirectoryInfo]::new($runtimeConfig)
if (($buildConfigRootInfo.Attributes -band [System.IO.FileAttributes]::ReparsePoint) -ne 0) {
    throw "CREDENTIAL_FREE_BUILD_DOCKER_CONFIG_REQUIRED"
}
if (($runtimeConfigInfo.Attributes -band [System.IO.FileAttributes]::ReparsePoint) -ne 0 -or
    @($runtimeConfigInfo.EnumerateFileSystemInfos()).Count -ne 0) {
    throw "EMPTY_LINK_FREE_RUNTIME_DOCKER_CONFIG_REQUIRED"
}
Assert-NoBroadWriteAcl -LiteralPath $buildConfigRoot
Assert-NoBroadWriteAcl -LiteralPath $runtimeConfig
$buildConfig = [System.IO.Path]::GetFullPath((Join-Path $buildConfigRoot ("session-" + [guid]::NewGuid().ToString("N"))))
if (-not ([System.IO.DirectoryInfo]::new($buildConfig).Parent.FullName).Equals($buildConfigRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "BUILD_DOCKER_CONFIG_BOUNDARY_INVALID"
}
[System.IO.Directory]::CreateDirectory($buildConfig) | Out-Null
$buildConfigInfo = [System.IO.DirectoryInfo]::new($buildConfig)
if (($buildConfigInfo.Attributes -band [System.IO.FileAttributes]::ReparsePoint) -ne 0 -or
    @($buildConfigInfo.EnumerateFileSystemInfos()).Count -ne 0) {
    throw "EMPTY_LINK_FREE_BUILD_DOCKER_CONFIG_REQUIRED"
}
Assert-NoBroadWriteAcl -LiteralPath $buildConfig
if ($DockerHost -ne "npipe:////./pipe/docker_engine") {
    throw "LOCAL_DOCKER_ENGINE_REQUIRED"
}

$processRoot = [System.IO.Path]::GetFullPath((Join-Path $buildConfigRoot ("process-" + [guid]::NewGuid().ToString("N"))))
try {
if (-not ([System.IO.DirectoryInfo]::new($processRoot).Parent.FullName).Equals($buildConfigRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "BUILD_PROCESS_BOUNDARY_INVALID"
}
$context = Join-Path $processRoot "context"
$processTemp = Join-Path $processRoot "temp"
[System.IO.Directory]::CreateDirectory($context) | Out-Null
[System.IO.Directory]::CreateDirectory($processTemp) | Out-Null
Assert-NoBroadWriteAcl -LiteralPath $processRoot
Assert-NoBroadWriteAcl -LiteralPath $context
Assert-NoBroadWriteAcl -LiteralPath $processTemp

if ($ExpectedDockerfileSha256 -notmatch '^[0-9a-fA-F]{64}$' -or
    $ExpectedDockerignoreSha256 -notmatch '^[0-9a-fA-F]{64}$') {
    throw "APPROVED_BUILD_CONTEXT_DIGESTS_REQUIRED"
}
$dockerfile = Join-Path $context "Dockerfile"
$dockerignore = Join-Path $context ".dockerignore"
[System.IO.File]::Copy((Join-Path $sourceContext "Dockerfile"), $dockerfile, $false)
[System.IO.File]::Copy((Join-Path $sourceContext ".dockerignore"), $dockerignore, $false)
$dockerfileSha256 = (Get-FileHash -LiteralPath $dockerfile -Algorithm SHA256).Hash.ToLowerInvariant()
$dockerignoreSha256 = (Get-FileHash -LiteralPath $dockerignore -Algorithm SHA256).Hash.ToLowerInvariant()
if (-not $dockerfileSha256.Equals($ExpectedDockerfileSha256.ToLowerInvariant(), [System.StringComparison]::Ordinal) -or
    -not $dockerignoreSha256.Equals($ExpectedDockerignoreSha256.ToLowerInvariant(), [System.StringComparison]::Ordinal)) {
    throw "BUILD_CONTEXT_DIGEST_MISMATCH"
}
    $buildResult = Invoke-IsolatedDocker -Executable $docker -TempDirectory $processTemp -EchoOutput `
        -ArgumentList @("--config", $buildConfig, "--host", $DockerHost, "build",
            "--platform", "linux/amd64", "--pull", "--tag", $ImageTag,
            "--file", $dockerfile, $context)
    if ($buildResult.ExitCode -ne 0) {
        throw "SANDBOX_IMAGE_BUILD_FAILED"
    }

    $inspectResult = Invoke-IsolatedDocker -Executable $docker -TempDirectory $processTemp -TimeoutMs 10000 `
        -ArgumentList @("--config", $runtimeConfig, "--host", $DockerHost,
            "image", "inspect", $ImageTag)
    if ($inspectResult.ExitCode -ne 0) {
        throw "SANDBOX_IMAGE_INSPECT_FAILED"
    }
    $inspectJson = $inspectResult.Stdout
    $records = @($inspectJson | ConvertFrom-Json -Depth 32)
    if ($records.Count -ne 1) {
        throw "SANDBOX_IMAGE_IDENTITY_AMBIGUOUS"
    }
    $image = $records[0]
    $volumesProperty = $image.Config.PSObject.Properties['Volumes']
    $cmdProperty = $image.Config.PSObject.Properties['Cmd']
    $healthcheckProperty = $image.Config.PSObject.Properties['Healthcheck']
    $entrypointProperty = $image.Config.PSObject.Properties['Entrypoint']
    $volumes = if ($null -eq $volumesProperty) { $null } else { $volumesProperty.Value }
    $commandValue = if ($null -eq $cmdProperty) { $null } else { $cmdProperty.Value }
    $healthcheck = if ($null -eq $healthcheckProperty) { $null } else { $healthcheckProperty.Value }
    $entrypoint = if ($null -eq $entrypointProperty) { $null } else { $entrypointProperty.Value }
    if ([string]$image.Id -notmatch '^sha256:[0-9a-f]{64}$' -or
        [string]$image.Os -ne "linux" -or [string]$image.Architecture -ne "amd64" -or
        [string]$image.Config.User -ne "65532:65532" -or
        [string]$image.Config.WorkingDir -ne "/workspace" -or
        $null -ne $volumes -or $null -ne $commandValue -or
        $null -ne $healthcheck -or $null -ne $entrypoint) {
        throw "SANDBOX_IMAGE_METADATA_UNTRUSTED"
    }

    $approvedEnvironment = @{
        PATH = "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
        JAVA_HOME = "/opt/java/openjdk"
        MAVEN_HOME = "/usr/share/maven"
        HOME = "/tmp"
        LANG = "C.UTF-8"
    }
    $seenEnvironment = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::Ordinal)
    foreach ($entry in @($image.Config.Env)) {
        $separator = ([string]$entry).IndexOf('=')
        if ($separator -le 0) {
            throw "SANDBOX_IMAGE_ENVIRONMENT_UNTRUSTED"
        }
        $name = ([string]$entry).Substring(0, $separator)
        $value = ([string]$entry).Substring($separator + 1)
        if (-not $approvedEnvironment.ContainsKey($name) -or
            -not ([string]$approvedEnvironment[$name]).Equals($value, [System.StringComparison]::Ordinal) -or
            -not $seenEnvironment.Add($name)) {
            throw "SANDBOX_IMAGE_ENVIRONMENT_UNTRUSTED"
        }
    }
    if ($seenEnvironment.Count -ne $approvedEnvironment.Count) {
        throw "SANDBOX_IMAGE_ENVIRONMENT_UNTRUSTED"
    }

    [ordered]@{
        schemaVersion = 1
        status = "BUILT_AND_INSPECTED"
        imageId = [string]$image.Id
        dockerExecutableSha256 = $dockerSha256
        dockerfileSha256 = $dockerfileSha256
        dockerignoreSha256 = $dockerignoreSha256
        dockerConfigDirectory = $runtimeConfig
        dockerHost = $DockerHost
        baseManifests = @(
            "maven@sha256:edf045813426842617b1667456ddec0026885146465e890b897655d877ba3386"
            "node@sha256:9137a20e25879e0b557227b57e3ee4e9af4bde29eb3db66134cd1723e84f830b"
        )
    } | ConvertTo-Json -Depth 6 -Compress
} finally {
    $sessionInfo = [System.IO.DirectoryInfo]::new($buildConfig)
    if ([System.IO.Directory]::Exists($buildConfig) -and
        $sessionInfo.Parent.FullName.Equals($buildConfigRoot, [System.StringComparison]::OrdinalIgnoreCase) -and
        $sessionInfo.Name.StartsWith("session-", [System.StringComparison]::Ordinal)) {
        Remove-Item -LiteralPath $buildConfig -Recurse -Force
    }
    $processInfo = [System.IO.DirectoryInfo]::new($processRoot)
    if ([System.IO.Directory]::Exists($processRoot) -and
        $processInfo.Parent.FullName.Equals($buildConfigRoot, [System.StringComparison]::OrdinalIgnoreCase) -and
        $processInfo.Name.StartsWith("process-", [System.StringComparison]::Ordinal)) {
        Remove-Item -LiteralPath $processRoot -Recurse -Force
    }
}
