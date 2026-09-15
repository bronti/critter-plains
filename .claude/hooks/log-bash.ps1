# PostToolUse hook (matcher: Bash) - logs every Bash tool call to .claude/logs/cmd.log.
#
# Replaces the old Continue-era convention of dot-sourcing _log.ps1 and remembering to
# call Write-CmdLog by hand: this fires automatically from the harness on every Bash
# call, so logging can no longer be forgotten.
#
# Never blocks: always exits 0, swallows its own errors.

$ErrorActionPreference = 'SilentlyContinue'

$root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$logDir = Join-Path $root ".claude\logs"
if (-not (Test-Path $logDir)) { New-Item -ItemType Directory -Force -Path $logDir | Out-Null }
$logFile = Join-Path $logDir "cmd.log"

$stdin = [Console]::In.ReadToEnd()
try { $payload = $stdin | ConvertFrom-Json } catch { exit 0 }

$command = $payload.tool_input.command
if (-not $command) { exit 0 }

$response = $payload.tool_response
$outputText = ""
if ($response) {
    foreach ($field in @('stdout', 'output', 'error', 'stderr')) {
        if ($response.$field) { $outputText += "$($response.$field)`n" }
    }
}

$isError = $false
if ($response -and ($response.PSObject.Properties.Name -contains 'is_error')) {
    $isError = [bool]$response.is_error
} elseif ($response -and ($response.PSObject.Properties.Name -contains 'success')) {
    $isError = -not [bool]$response.success
}

$shellErrorPatterns = @(
    "CommandNotFoundException", "is not recognized as the name", "ParserError",
    "Cannot find path", "does not exist", "ObjectNotFound", "FileNotFoundException"
)
$kind = if (-not $isError) {
    "ok"
} elseif ($shellErrorPatterns | Where-Object { $outputText -match $_ }) {
    "shell-error"
} else {
    "build-failure"
}

$entry = [ordered]@{
    ts      = (Get-Date -Format "yyyy-MM-ddTHH:mm:ss")
    kind    = $kind
    session = $payload.session_id
    cmd     = $command
    output  = if ($outputText.Length -gt 1000) { $outputText.Substring(0, 1000) + "...[truncated]" } else { $outputText }
}
$entry | ConvertTo-Json -Compress | Add-Content -Path $logFile -Encoding UTF8
exit 0
