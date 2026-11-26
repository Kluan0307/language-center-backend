param()
$ErrorActionPreference = 'Stop'
$taskRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$oldKey=$env:GEMINI_API_KEY
$oldModel=$env:GEMINI_MODEL
$oldFlag=$env:ASSISTANT_R8_LIVE
try {
  $taskEnv=Join-Path $taskRoot '.env.local'
  if(Test-Path -LiteralPath $taskEnv){foreach($line in Get-Content -LiteralPath $taskEnv -Encoding UTF8){if($line -match '^\s*(GEMINI_API_KEY|GEMINI_MODEL)=(.*)$'){[Environment]::SetEnvironmentVariable($Matches[1],$Matches[2].Trim(),'Process')}}}
  $env:ASSISTANT_R8_LIVE='true'
  Push-Location -LiteralPath $taskRoot
  try {
    $taskReport=Join-Path $taskRoot 'assistant-service/target/r8-live-acceptance.json'
    if(Test-Path -LiteralPath $taskReport){Remove-Item -LiteralPath $taskReport}
    New-Item -ItemType Directory -Path (Join-Path $taskRoot 'storage') -Force | Out-Null
    & mvn "-Dmaven.repo.local=$(Join-Path $env:USERPROFILE '.m2/repository')" '-DargLine=-Xmx320m' '-pl' 'assistant-service' '-Dtest=ReadonlyLiveAcceptanceTest' 'test' *> (Join-Path $taskRoot 'storage/r8-live-acceptance.log')
    $taskExit=$LASTEXITCODE
    $taskReport=Join-Path $taskRoot 'assistant-service/target/r8-live-acceptance.json'
    if(Test-Path -LiteralPath $taskReport){
      Copy-Item -LiteralPath $taskReport -Destination (Join-Path $taskRoot 'storage/r8-live-acceptance.json')
      $archive=Join-Path $taskRoot 'storage/r8-live-runs'
      New-Item -ItemType Directory -Path $archive -Force | Out-Null
      Copy-Item -LiteralPath $taskReport -Destination (Join-Path $archive ((Get-Date).ToUniversalTime().ToString('yyyyMMddTHHmmssZ')+'.json'))
    }
    if($taskExit -ne 0){throw 'R8 live acceptance did not pass. See storage/r8-live-acceptance.json; no key was printed and no real center data was sent.'}
    Write-Output 'PASS: R8 real-model matrix against synthetic Auth/business fixtures.'
  } finally {Pop-Location}
} finally {
  [Environment]::SetEnvironmentVariable('GEMINI_API_KEY',$oldKey,'Process')
  [Environment]::SetEnvironmentVariable('GEMINI_MODEL',$oldModel,'Process')
  [Environment]::SetEnvironmentVariable('ASSISTANT_R8_LIVE',$oldFlag,'Process')
}
