param([string]$AuthProperties)
$ErrorActionPreference="Stop"
. "$PSScriptRoot/Import-LocalEnvironment.ps1"
Import-LocalEnvironment $AuthProperties
$env:DEBUG="false"
& "$PSScriptRoot/../mvnw.cmd" spring-boot:run "-Dspring-boot.run.arguments=--debug=false"
exit $LASTEXITCODE
