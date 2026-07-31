[CmdletBinding()]
param(
    [switch]$Force
)

$ErrorActionPreference = 'Stop'

function Find-Executable([string]$Name) {
    $command = Get-Command $Name -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    $roots = @(
        (Join-Path $env:LOCALAPPDATA 'Microsoft\WinGet\Packages'),
        (Join-Path $env:USERPROFILE 'scoop\apps\ffmpeg'),
        'C:\ProgramData\chocolatey\bin',
        'C:\ffmpeg'
    )
    foreach ($root in $roots) {
        if (-not (Test-Path $root)) { continue }
        $match = Get-ChildItem -Path $root -Filter "$Name.exe" -Recurse -ErrorAction SilentlyContinue |
            Select-Object -First 1 -ExpandProperty FullName
        if ($match) { return $match }
    }
    return $null
}

$ffmpeg = Find-Executable 'ffmpeg'
$ffprobe = Find-Executable 'ffprobe'

if ($Force -or -not $ffmpeg -or -not $ffprobe) {
    $winget = Get-Command winget -ErrorAction SilentlyContinue
    if (-not $winget) {
        throw '未找到 winget。请先从 Microsoft Store 安装“应用安装程序”，或使用项目 Dockerfile 运行后端。'
    }

    Write-Host '正在通过 winget 安装 FFmpeg...' -ForegroundColor Cyan
    & winget install --id Gyan.FFmpeg --exact --source winget `
        --accept-package-agreements --accept-source-agreements
    if ($LASTEXITCODE -ne 0) {
        throw "winget 安装失败，退出码: $LASTEXITCODE"
    }

    $ffmpeg = Find-Executable 'ffmpeg'
    $ffprobe = Find-Executable 'ffprobe'
}

if (-not $ffmpeg -or -not $ffprobe) {
    throw 'FFmpeg 已安装但未能定位 ffmpeg.exe 或 ffprobe.exe。请重新打开 PowerShell 后再次运行脚本。'
}

$binDirectory = Split-Path -Parent $ffmpeg
[Environment]::SetEnvironmentVariable('FFMPEG_PATH', $ffmpeg, 'User')
[Environment]::SetEnvironmentVariable('FFPROBE_PATH', $ffprobe, 'User')

$userPath = [Environment]::GetEnvironmentVariable('Path', 'User')
$pathParts = @($userPath -split ';' | Where-Object { $_ })
if ($pathParts -notcontains $binDirectory) {
    $newPath = (@($pathParts) + $binDirectory) -join ';'
    [Environment]::SetEnvironmentVariable('Path', $newPath, 'User')
}

$env:FFMPEG_PATH = $ffmpeg
$env:FFPROBE_PATH = $ffprobe
if (($env:Path -split ';') -notcontains $binDirectory) {
    $env:Path = "$binDirectory;$env:Path"
}

Write-Host "ffmpeg:  $ffmpeg" -ForegroundColor Green
Write-Host "ffprobe: $ffprobe" -ForegroundColor Green

& $ffmpeg -hide_banner -version | Select-Object -First 1
& $ffprobe -hide_banner -version | Select-Object -First 1

$encoders = & $ffmpeg -hide_banner -encoders 2>&1 | Out-String
if ($encoders -notmatch '\blibx264\b') {
    throw '当前 FFmpeg 不包含 libx264 编码器，无法满足短剧合成配置。'
}
if ($encoders -notmatch '\baac\b') {
    throw '当前 FFmpeg 不包含 AAC 编码器，无法满足短剧合成配置。'
}

Write-Host ''
Write-Host '安装与配置完成。请完全重启 IntelliJ IDEA 和 ruoyi-ai 后端服务。' -ForegroundColor Yellow
Write-Host '重启后，Spring 将从 FFMPEG_PATH 和 FFPROBE_PATH 读取绝对路径。'