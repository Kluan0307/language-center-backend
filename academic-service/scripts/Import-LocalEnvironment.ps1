function Import-LocalEnvironment([string]$AuthProperties) {
 if ($AuthProperties) {
  if (-not (Test-Path -LiteralPath $AuthProperties)) { throw "Auth properties file does not exist." }
  $properties=Get-Content -LiteralPath $AuthProperties -Raw | ConvertFrom-StringData
  $env:DB_PASSWORD=$properties['spring.datasource.password']
  $env:DB_USERNAME=$properties['spring.datasource.username']
  $env:JWT_SECRET=$properties['jwt.secret']
  $env:JWT_EXPIRATION=$properties['jwt.expiration']
 }
 if (-not $env:DB_PASSWORD -or -not $env:JWT_SECRET) {
  throw "Set DB_PASSWORD and JWT_SECRET, or explicitly pass -AuthProperties pointing to your local auth configuration."
 }
}
