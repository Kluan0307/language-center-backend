param(
  [Parameter(Mandatory=$true)]
  [ValidateSet('auth-service','academic-service','finance-service','teaching-service','api-gateway')]
  [string]$Service,
  [string]$EnvFile=(Join-Path $PSScriptRoot '../.env.local')
)
$ErrorActionPreference='Stop'
$taskRoot=[System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
if(Test-Path -LiteralPath $EnvFile) {
  foreach($taskLine in Get-Content -LiteralPath $EnvFile -Encoding UTF8) {
    if($taskLine -match '^\s*(DB_URL|DB_USERNAME|DB_PASSWORD|JWT_SECRET|JWT_EXPIRATION|DDL_AUTO|AUTH_URL|ACADEMIC_URL|TEACHING_URL|FINANCE_URL|AUTH_BOOTSTRAP_ENABLED|AUTH_BOOTSTRAP_USERNAME|AUTH_BOOTSTRAP_PASSWORD|DOCUMENT_STORAGE)=(.*)$') {
      [Environment]::SetEnvironmentVariable($Matches[1],$Matches[2],'Process')
    }
  }
}
if($Service -ne 'api-gateway' -and (-not $env:DB_PASSWORD -or -not $env:JWT_SECRET)) {
  throw 'Set DB_PASSWORD and JWT_SECRET in the environment or .env.local.'
}
Push-Location (Join-Path $taskRoot $Service)
try {
  & .\mvnw.cmd spring-boot:run
  if($LASTEXITCODE -ne 0){throw "Service exited with code $LASTEXITCODE"}
} finally {Pop-Location}
