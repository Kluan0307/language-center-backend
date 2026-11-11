param([Parameter(Mandatory=$true)][string]$RunDir)
$ErrorActionPreference='Stop'
$root=(Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$RunDir=(Resolve-Path -LiteralPath $RunDir).Path
if(-not $RunDir.StartsWith((Join-Path $root 'storage/acceptance')+'\',[StringComparison]::OrdinalIgnoreCase)){throw 'Invalid demo directory'}
$run=Get-Content (Join-Path $RunDir 'runtime.json') -Raw | ConvertFrom-Json
if($run.database -notmatch '^lc_rehearsal_demo_g4_\w+$'){throw 'Only fresh demo schemas allowed'}
$credentials=Get-Content (Join-Path $RunDir 'bootstrap.private.json') -Raw | ConvertFrom-Json
$keys=@('DB_URL','DB_USERNAME','DB_PASSWORD','JWT_SECRET','SPRING_CONFIG_IMPORT','AUTH_BOOTSTRAP_ENABLED','AUTH_BOOTSTRAP_USERNAME','AUTH_BOOTSTRAP_PASSWORD','SERVER_ADDRESS')
$previous=@{};foreach($key in $keys){$previous[$key]=[Environment]::GetEnvironmentVariable($key,'Process')}
$jar=Join-Path $RunDir 'bootstrap/auth-bootstrap.jar'
try {
 foreach($line in Get-Content (Join-Path $root '.env.local') -Encoding UTF8){if($line -match '^(DB_URL|DB_USERNAME|DB_PASSWORD|JWT_SECRET)=(.*)$'){[Environment]::SetEnvironmentVariable($Matches[1],$Matches[2],'Process')}}
 $env:DB_URL=$env:DB_URL -replace '(jdbc:mysql://[^/]+/)[^?]+',('${1}'+$run.database)
 $env:SPRING_CONFIG_IMPORT='optional:file:/nonexistent-demo-config.properties';$env:SERVER_ADDRESS='127.0.0.1'
 $env:AUTH_BOOTSTRAP_ENABLED='true';$env:AUTH_BOOTSTRAP_USERNAME=$credentials.username;$env:AUTH_BOOTSTRAP_PASSWORD=$credentials.password
 if(Get-NetTCPConnection -LocalPort 28081 -State Listen -ErrorAction SilentlyContinue){throw 'Private bootstrap port occupied'}
 $jars=@(Get-ChildItem (Join-Path $root 'auth-service/target/*.jar'));if($jars.Count -ne 1){throw 'Expected packaged auth JAR'}
 Copy-Item -LiteralPath $jars[0].FullName -Destination $jar
 Start-Process java -ArgumentList @('-Xmx256m','-jar',$jar,'--server.port=28081','--spring.profiles.active=bootstrap','--spring.jpa.hibernate.ddl-auto=validate') -WindowStyle Hidden -WorkingDirectory $root -RedirectStandardOutput (Join-Path $RunDir 'bootstrap.out.log') -RedirectStandardError (Join-Path $RunDir 'bootstrap.err.log') | Out-Null
 $ready=$false
 for($i=0;$i -lt 100;$i++){
  try{$body=@{username=$credentials.username;password=$credentials.password}|ConvertTo-Json;$login=Invoke-RestMethod 'http://127.0.0.1:28081/api/v1/auth/login' -Method Post -ContentType 'application/json' -Body $body -TimeoutSec 2;if($login.token){$ready=$true;break}}catch{}
  Start-Sleep -Milliseconds 500
 }
 if(-not $ready){throw 'Bootstrap failed; inspect private logs'}
 Write-Output 'Super Admin provisioned by application bootstrap; credentials kept private.'
} finally {
 $needle=$jar.Replace('\','/')
 Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object {$_.CommandLine -and $_.CommandLine.Replace('\','/').Contains($needle)} | ForEach-Object {Stop-Process -Id $_.ProcessId -ErrorAction SilentlyContinue;Wait-Process -Id $_.ProcessId -Timeout 10 -ErrorAction SilentlyContinue}
 foreach($key in $keys){[Environment]::SetEnvironmentVariable($key,$previous[$key],'Process')}
}
