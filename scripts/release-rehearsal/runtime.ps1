param([ValidateSet('start','stop','restart')][string]$Action='start',[string]$RunDir,[ValidateSet('all','auth-service','academic-service','teaching-service','finance-service','api-gateway')][string]$Service='all')
$ErrorActionPreference='Stop'
$backendRoot=(Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$RunDir=(Resolve-Path -LiteralPath $RunDir).Path
if(!$RunDir.StartsWith((Join-Path $backendRoot 'storage/acceptance')+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)){throw 'Invalid rehearsal path'}
$file=Join-Path $RunDir 'runtime.json';$run=Get-Content $file -Raw -Encoding UTF8|ConvertFrom-Json
$release=(Resolve-Path -LiteralPath $run.releaseRoot).Path
if(!$release.StartsWith((Join-Path $backendRoot 'storage/releases')+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)){throw 'Release must be an isolated local package'}
if($run.database -notmatch '^lc_rehearsal_[A-Za-z0-9_]+$' -or $run.runId -notmatch '^[A-Za-z0-9-]+$'){throw 'Only isolated rehearsal database/run allowed'}
$entries=@(@('auth-service',1),@('academic-service',2),@('teaching-service',3),@('finance-service',4),@('api-gateway',0))
$chosen=@($entries|Where-Object{$Service -eq 'all' -or $_[0] -eq $Service})
$tag="-Dlc.rehearsal=$($run.runId)"
if($Action -in @('stop','restart')){
 foreach($entry in $chosen){
  $jar=(Join-Path $release "backend/$($entry[0]).jar").Replace('\','/')
  Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object { $_.CommandLine -and $_.CommandLine.Contains($tag) -and $_.CommandLine.Replace('\','/').Contains($jar) } | ForEach-Object {
   Stop-Process -Id $_.ProcessId -ErrorAction SilentlyContinue
   Wait-Process -Id $_.ProcessId -Timeout 15 -ErrorAction SilentlyContinue
   Add-Content (Join-Path $RunDir 'lifecycle.log') "Stopped packaged $($entry[0]) PID=$($_.ProcessId)"
  }
 }
 if($Action -eq 'stop'){
  if($Service -eq 'all'){
   if($run.staticPid){$p=Get-CimInstance Win32_Process -Filter "ProcessId=$($run.staticPid)";if($p -and $p.CommandLine.Replace('\','/').Contains($release.Replace('\','/')+'/scripts/serve-release.cjs')){Stop-Process -Id $p.ProcessId -ErrorAction SilentlyContinue}}
   $profile=$RunDir.Replace('\','/')+'/browser-profile'
   Get-CimInstance Win32_Process -Filter "Name='msedge.exe'" | Where-Object {$_.CommandLine -and $_.CommandLine.Replace('\','/').Contains($profile)} | ForEach-Object {Stop-Process -Id $_.ProcessId -ErrorAction SilentlyContinue}
  }
  exit 0
 }
}
& node (Join-Path $release 'scripts/verify-release.cjs') $release
if($LASTEXITCODE -ne 0){throw 'Release verification failed'}
$values=@{}
foreach($line in Get-Content (Join-Path $backendRoot '.env.local') -Encoding UTF8){if($line -match '^(DB_URL|DB_USERNAME|DB_PASSWORD|JWT_SECRET)=(.*)$'){$values[$Matches[1]]=$Matches[2]}}
if($values.DB_URL -notmatch '^jdbc:mysql://([^/]+)/[^?]+(\?.*)?$'){throw 'Expected MySQL URL'}
$authority=$Matches[1]
$values.DB_URL="jdbc:mysql://$authority/$($run.database)?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh"
$values.SPRING_PROFILES_ACTIVE='prod';$values.SPRING_CONFIG_IMPORT='optional:file:/nonexistent-release-env.properties'
$values.SPRING_JPA_HIBERNATE_DDL_AUTO='validate';$values.SERVER_ADDRESS='127.0.0.1';$values.CORS_ALLOWED_ORIGINS="http://127.0.0.1:$($run.frontendPort)"
$rehearsal=Get-Content (Join-Path $run.backup 'rehearsal.json') -Raw -Encoding UTF8|ConvertFrom-Json
$values.DOCUMENT_STORAGE=$rehearsal.restoredDocuments
$values.AUTH_URL="http://127.0.0.1:$([int]$run.basePort+1)";$values.ACADEMIC_URL="http://127.0.0.1:$([int]$run.basePort+2)";$values.TEACHING_URL="http://127.0.0.1:$([int]$run.basePort+3)";$values.FINANCE_URL="http://127.0.0.1:$([int]$run.basePort+4)"
foreach($entry in $chosen){
 $name=$entry[0];$port=[int]$run.basePort+[int]$entry[1]
 if(Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue){throw "Rehearsal port $port occupied"}
 $values.SERVER_PORT=$port
 $values.LOGGING_FILE_NAME=Join-Path $RunDir "$name.application.log"
 $privateEnv=Join-Path $RunDir "$name.private.env"
 $values.GetEnumerator()|ForEach-Object {"$($_.Key)=$($_.Value)"}|Set-Content $privateEnv -Encoding UTF8
 $stamp=[DateTime]::UtcNow.ToString('yyyyMMddHHmmssfff')
 foreach($suffix in @('out','err')){if(Test-Path (Join-Path $RunDir "$name.$suffix.log")){Copy-Item (Join-Path $RunDir "$name.$suffix.log") (Join-Path $RunDir "$name.$stamp.previous.$suffix.log")}}
 $launcher=Join-Path $release 'scripts/start-release.ps1'
 Start-Process powershell.exe -ArgumentList @('-NoProfile','-File',"`"$launcher`"",'-Release',"`"$release`"",'-Service',$name,'-EnvFile',"`"$privateEnv`"",'-RunTag',$run.runId) -WindowStyle Hidden -WorkingDirectory $release -RedirectStandardOutput (Join-Path $RunDir "$name.out.log") -RedirectStandardError (Join-Path $RunDir "$name.err.log")|Out-Null
 Add-Content (Join-Path $RunDir 'lifecycle.log') "Started packaged $name port=$port sha256=$((Get-FileHash (Join-Path $release "backend/$name.jar")).Hash)"
}
if($Action -eq 'start' -and $Service -eq 'all'){
 foreach($port in @($run.frontendPort,$run.browserPort)){if(Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue){throw 'Private frontend/CDP port occupied'}}
 $server=Join-Path $release 'scripts/serve-release.cjs'
 $p=Start-Process (Get-Command node).Source -ArgumentList @("`"$server`"","`"$release`"",$run.frontendPort,$run.basePort) -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $RunDir 'static.out.log') -RedirectStandardError (Join-Path $RunDir 'static.err.log');$run|Add-Member staticPid $p.Id -Force
 Start-Process 'C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe' -ArgumentList @('--headless=new','--disable-gpu','--no-first-run','--remote-debugging-address=127.0.0.1',"--remote-debugging-port=$($run.browserPort)","--user-data-dir=$(Join-Path $RunDir 'browser-profile')",'about:blank') -WindowStyle Hidden|Out-Null
 $run|ConvertTo-Json -Depth 8|Set-Content $file -Encoding UTF8
}
Write-Output "Packaged runtime $Action $Service schema=$($run.database)"
