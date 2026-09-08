# Coding harness sandbox image

This directory builds the evaluator-owned Linux toolchain used by `execute_process`.
Runtime code never builds or pulls an image; it accepts only the final local `sha256:<image-id>`.

The build uses official Node and Maven manifests pinned for `linux/amd64`, then copies the
assembled filesystem into a `scratch` stage so inherited volumes, commands, entrypoints,
healthchecks, and environment do not cross the trust boundary. The final image contains Java,
Maven, Node, npm, pnpm 10.14.0, Python 3, ripgrep, and Git, and runs as `65532:65532`.

`build-trusted-sandbox.ps1` verifies the Docker CLI digest and owner/ACL before its first Docker
call. Every build gets a fresh empty CLI config, a minimal process environment, a bounded process
timeout, and a private context containing only digest-approved copies of `Dockerfile` and
`.dockerignore`. Image inspection uses a separate runtime config directory that must stay
completely empty and checks the exact platform, non-root user, working directory, absent implicit
commands/volumes/healthcheck, and an exact five-variable environment allowlist. The script prints
the final image ID, CLI digest, and build-definition digests; deployments must pass the exact image
and CLI values through `CODING_HARNESS_SANDBOX_IMAGE` and
`CODING_HARNESS_DOCKER_EXECUTABLE_SHA256`.

The digest defaults in this repository prevent accidental drift but are not an external trust
anchor: an operator release must approve the script/build-definition digests from an ACL- or
signature-protected evaluator boundary. APT package resolution and the Corepack download are not
yet hermetic, so the approved final image ID—not a rebuilt tag—is the runtime authority.

This image alone does not make the sandbox complete. Runtime create/start/inspect/remove state,
workspace identity, restart cleanup, and live adversarial canaries remain independent release
gates. Dependency caches are deliberately not inherited from the host and are not yet baked in.
