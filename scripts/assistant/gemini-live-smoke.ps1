param()
$ErrorActionPreference = 'Stop'
$workspaceRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$settingsFile = Join-Path $workspaceRoot '.env.local'
$beforeKey = $env:GEMINI_API_KEY
$beforeModel = $env:GEMINI_MODEL
$beforeFlag = $env:GEMINI_LIVE_SMOKE
try {
  if (Test-Path -LiteralPath $settingsFile) {
    foreach ($line in Get-Content -LiteralPath $settingsFile -Encoding UTF8) {
      if ($line -match '^\s*(GEMINI_API_KEY|GEMINI_MODEL)=(.*)$') { [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2].Trim(), 'Process') }
    }
  }
  if (-not $env:GEMINI_API_KEY) { throw 'GEMINI_API_KEY is not configured.' }
  $env:GEMINI_LIVE_SMOKE = 'true'
  Push-Location (Join-Path $workspaceRoot 'assistant-service')
  try {
    $logFile = Join-Path $workspaceRoot 'storage/c4-gemini-live.log'
    # Only the key/model are loaded. No DB/JWT credentials or real business data are sent.
    & mvn "-Dmaven.repo.local=$(Join-Path $env:USERPROFILE '.m2/repository')" '-Dtest=GeminiLiveSmokeTest' test *> $logFile
    if ($LASTEXITCODE -ne 0) { throw 'Gemini smoke did not pass; see sanitized status in assistant-service/target/c4-gemini-live.json and storage/c4-gemini-live.log.' }
    Write-Output 'PASS: Gemini + MCP completed the synthetic assistant checks. Key was not printed.'
  } finally { Pop-Location }
} finally {
  [Environment]::SetEnvironmentVariable('GEMINI_API_KEY', $beforeKey, 'Process')
  [Environment]::SetEnvironmentVariable('GEMINI_MODEL', $beforeModel, 'Process')
  [Environment]::SetEnvironmentVariable('GEMINI_LIVE_SMOKE', $beforeFlag, 'Process')
}
