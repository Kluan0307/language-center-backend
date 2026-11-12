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
    if($taskLine -match '^\s*(DB_URL|DB_USERNAME|DB_PASSWORD|JWT_SECRET|JWT_EXPIRATION|DDL_AUTO|AUTH_URL|ACADEMIC_URL|TEACHING_URL|FINANCE_URL|AUTH_BOOTSTRAP_ENABLED|AUTH_BOOTSTRAP_USERNAME|AUTH_BOOTSTRAP_PASSWORD|DOCUMENT_STORAGE|VNPAY_ENABLED|VNPAY_TMN_CODE|VNPAY_HASH_SECRET|VNPAY_RETURN_URL|VNPAY_CHECKOUT_URL|VNPAY_QUERY_URL|VNPAY_SERVER_IP|VNPAY_TTL_MINUTES|PAYMENT_JOBS_ENABLED|NOTIFICATION_JOBS_ENABLED|NOTIFICATIONS_PUSH_ENABLED|EXPO_PROJECT_ID|EXPO_PUSH_ACCESS_TOKEN)=(.*)$') {
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
