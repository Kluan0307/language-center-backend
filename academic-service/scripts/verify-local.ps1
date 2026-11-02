param([string]$BaseUrl="http://localhost:8082/api/v1/academic",[string]$AuthProperties)
$ErrorActionPreference="Stop"
. "$PSScriptRoot/Import-LocalEnvironment.ps1"
Import-LocalEnvironment $AuthProperties
$connector=Get-ChildItem (Join-Path $env:USERPROFILE ".m2/repository/com/mysql/mysql-connector-j") -Recurse -Filter "mysql-connector-j-*.jar" |
 Sort-Object FullName -Descending | Select-Object -First 1 -ExpandProperty FullName
if (-not $connector) { throw "MySQL Connector/J not found in the local Maven repository." }
$prefixFile="$PSScriptRoot/../target/smoke-prefix.txt"
$previousPrefix=if(Test-Path $prefixFile){Get-Content $prefixFile -Raw}else{""}
try {
 & "$PSScriptRoot/smoke.ps1" -BaseUrl $BaseUrl
} finally {
 if(Test-Path $prefixFile){
  $prefix=Get-Content $prefixFile -Raw
  if($prefix-ne$previousPrefix){
   & java --class-path $connector "$PSScriptRoot/CleanupSmoke.java" $prefix
   if($LASTEXITCODE-ne0){throw "Smoke cleanup failed for $prefix"}
  }
 }
}
