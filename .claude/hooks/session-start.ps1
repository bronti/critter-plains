# SessionStart hook - marks a session boundary in the log.
#
# Fixes a known gap from the old analyze-log.ps1: it approximated "this session"
# with a hardcoded 24h cutoff. session-end.ps1 now uses the most recent
# session-start marker instead.

$ErrorActionPreference = 'SilentlyContinue'

$root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$logDir = Join-Path $root ".claude\logs"
if (-not (Test-Path $logDir)) { New-Item -ItemType Directory -Force -Path $logDir | Out-Null }
$logFile = Join-Path $logDir "cmd.log"

$stdin = [Console]::In.ReadToEnd()
$payload = $null
try { $payload = $stdin | ConvertFrom-Json } catch {}

$entry = [ordered]@{
    ts      = (Get-Date -Format "yyyy-MM-ddTHH:mm:ss")
    kind    = "session-start"
    session = if ($payload) { $payload.session_id } else { $null }
    source  = if ($payload) { $payload.source } else { $null }
}
$entry | ConvertTo-Json -Compress | Add-Content -Path $logFile -Encoding UTF8
exit 0
