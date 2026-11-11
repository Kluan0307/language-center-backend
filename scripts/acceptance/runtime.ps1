param([ValidateSet('start','stop','restart')][string]$Action='start',[string]$RunDir,[ValidateSet('all','auth-service','academic-service','teaching-service','finance-service','api-gateway')][string]$Service='all')
$ErrorActionPreference='Stop'
$backendRoot=(Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
if(!$RunDir){$RunDir=(Get-Content (Join-Path $backendRoot 'storage/acceptance/latest.txt') -Raw).Trim()}
$RunDir=(Resolve-Path -LiteralPath $RunDir).Path
$allowedRoot=Join-Path $backendRoot 'storage/acceptance'
if(!$RunDir.StartsWith($allowedRoot+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)){throw 'Run path outside acceptance root'}
$runtimeFile=Join-Path $RunDir 'runtime.json';$run=Get-Content $runtimeFile -Raw -Encoding UTF8|ConvertFrom-Json
if($run.releaseRoot){& (Join-Path $PSScriptRoot '../release-rehearsal/runtime.ps1') -Action $Action -Service $Service -RunDir $RunDir;exit $LASTEXITCODE}
$rehearsal=Get-Content (Join-Path $run.backup 'rehearsal.json') -Raw -Encoding UTF8|ConvertFrom-Json
if($rehearsal.database -notmatch '^lc_rehearsal_[a-zA-Z0-9_]+$'){throw 'Refuse non-rehearsal database'}
$run|Add-Member database $rehearsal.database -Force
$run|ConvertTo-Json -Depth 8|Set-Content $runtimeFile -Encoding UTF8
function Write-Lifecycle([string]$Message) {
  $path=Join-Path $RunDir 'lifecycle.log'
  for($attempt=0;$attempt -lt 5;$attempt++) {
    try {[IO.File]::AppendAllText($path,$Message+[Environment]::NewLine,(New-Object Text.UTF8Encoding($false)));return}
    catch [IO.IOException] {if($attempt -eq 4){throw};Start-Sleep -Milliseconds 100}
  }
}
$entries=@(@('auth-service',1),@('academic-service',2),@('teaching-service',3),@('finance-service',4),@('api-gateway',0))
$chosen=@($entries|Where-Object{$Service -eq 'all' -or $_[0] -eq $Service})
$jarDir=Join-Path $RunDir 'jars';$normalizedJarDir=$jarDir.Replace('\','/')
if($Action -in @('stop','restart')) {
  foreach($entry in $chosen) {
    $ownedJar=$normalizedJarDir+'/'+$entry[0]+'.jar'
    # Match our copied JAR even before Tomcat binds a port; Java launcher PID can differ.
    $processes=@(Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object { $_.CommandLine -and $_.CommandLine.Replace('\','/').Contains($ownedJar) })
    foreach($process in $processes) {
      Stop-Process -Id $process.ProcessId -ErrorAction SilentlyContinue
      Wait-Process -Id $process.ProcessId -Timeout 15 -ErrorAction SilentlyContinue
      Write-Lifecycle "Stopped $($entry[0]) private PID=$($process.ProcessId)"
    }
  }
  if($Action -eq 'stop') {
    if($Service -eq 'all') {
      if($run.vitePid) {
        $viteProcess=Get-CimInstance Win32_Process -Filter "ProcessId=$($run.vitePid)"
        if($viteProcess -and $viteProcess.CommandLine.Contains('vite')) { Stop-Process -Id $viteProcess.ProcessId -ErrorAction SilentlyContinue }
      }
      $profilePath=$RunDir.Replace('\','/')+'/browser-profile'
      Get-CimInstance Win32_Process -Filter "Name='msedge.exe'" | Where-Object { $_.CommandLine -and $_.CommandLine.Replace('\','/').Contains($profilePath) } | ForEach-Object { Stop-Process -Id $_.ProcessId -ErrorAction SilentlyContinue }
    }
    exit 0
  }
}
$environmentKeys=@('DB_URL','DB_USERNAME','DB_PASSWORD','JWT_SECRET','SPRING_PROFILES_ACTIVE','SPRING_CONFIG_IMPORT','CORS_ALLOWED_ORIGINS','DOCUMENT_STORAGE','SERVER_ADDRESS','AUTH_URL','ACADEMIC_URL','TEACHING_URL','FINANCE_URL','VITE_API_BASE_URL')
$previousEnvironment=@{}
foreach($key in $environmentKeys){$previousEnvironment[$key]=[Environment]::GetEnvironmentVariable($key,'Process')}
try {
foreach($line in Get-Content (Join-Path $backendRoot '.env.local') -Encoding UTF8){if($line -match '^(DB_URL|DB_USERNAME|DB_PASSWORD|JWT_SECRET)=(.*)$'){[Environment]::SetEnvironmentVariable($Matches[1],$Matches[2],'Process')}}
if($env:DB_URL -notmatch '^jdbc:mysql://([^/]+)/[^?]+(\?.*)?$'){throw 'Expected MySQL JDBC URL'}
$dbAuthority=$Matches[1]
$env:DB_URL="jdbc:mysql://$dbAuthority/$($rehearsal.database)?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh";$env:SPRING_PROFILES_ACTIVE='prod';$env:SPRING_CONFIG_IMPORT='optional:file:/nonexistent-acceptance-secrets.properties';$env:CORS_ALLOWED_ORIGINS="http://127.0.0.1:$($run.frontendPort)";$env:DOCUMENT_STORAGE=$rehearsal.restoredDocuments;$env:SERVER_ADDRESS='127.0.0.1';$env:AUTH_URL="http://127.0.0.1:$([int]$run.basePort+1)";$env:ACADEMIC_URL="http://127.0.0.1:$([int]$run.basePort+2)";$env:TEACHING_URL="http://127.0.0.1:$([int]$run.basePort+3)";$env:FINANCE_URL="http://127.0.0.1:$([int]$run.basePort+4)"
New-Item -ItemType Directory -Path $jarDir -Force|Out-Null
foreach($e in $chosen){$name=$e[0];$port=[int]$run.basePort+[int]$e[1];if(Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue){throw 'Private port occupied'};$jars=@(Get-ChildItem (Join-Path $backendRoot "$name/target/*.jar"));if($jars.Count -ne 1){throw 'Expected one packaged JAR'};$target=Join-Path $jarDir "$name.jar";Copy-Item -LiteralPath $jars[0].FullName -Destination $target -Force;$log=Join-Path $RunDir $name;$stamp=[DateTime]::UtcNow.ToString("yyyyMMddHHmmssfff");foreach($suffix in @("out","err")){if(Test-Path -LiteralPath "$log.$suffix.log"){Copy-Item -LiteralPath "$log.$suffix.log" -Destination "$log.$stamp.previous.$suffix.log"}};$p=Start-Process java -ArgumentList @('-Xmx256m','-jar',$target,"--server.port=$port",'--spring.jpa.hibernate.ddl-auto=validate','--logging.level.root=INFO') -WindowStyle Hidden -WorkingDirectory $backendRoot -PassThru -RedirectStandardOutput "$log.out.log" -RedirectStandardError "$log.err.log";Write-Lifecycle "Started $name port=$port launcherPID=$($p.Id) sha256=$((Get-FileHash $target -Algorithm SHA256).Hash)"}
if($Action -eq 'start' -and $Service -eq 'all'){
if(Get-NetTCPConnection -LocalPort $run.frontendPort -State Listen -ErrorAction SilentlyContinue){throw 'Private FE port occupied'}
$env:VITE_API_BASE_URL="http://127.0.0.1:$($run.basePort)/api/v1";$front=(Resolve-Path (Join-Path $backendRoot '../../language-center-web-frontend')).Path
$vite=Start-Process (Get-Command node -ErrorAction Stop).Source -ArgumentList @('node_modules/vite/bin/vite.js','--host','127.0.0.1','--port',"$($run.frontendPort)",'--strictPort') -WorkingDirectory $front -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $RunDir 'vite.out.log') -RedirectStandardError (Join-Path $RunDir 'vite.err.log');$run|Add-Member vitePid $vite.Id -Force
if(Get-NetTCPConnection -LocalPort $run.browserPort -State Listen -ErrorAction SilentlyContinue){throw 'Private CDP port occupied'};$browser=Start-Process 'C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe' -ArgumentList @('--headless=new','--disable-gpu','--no-first-run','--remote-debugging-address=127.0.0.1',"--remote-debugging-port=$($run.browserPort)","--user-data-dir=$(Join-Path $RunDir 'browser-profile')",'about:blank') -WindowStyle Hidden -PassThru;$run|Add-Member browserLauncherPid $browser.Id -Force
$run|ConvertTo-Json -Depth 8|Set-Content $runtimeFile -Encoding UTF8}
Write-Output "Private runtime action=$Action service=$Service database=$($rehearsal.database)"

} finally {
  foreach($key in $environmentKeys){[Environment]::SetEnvironmentVariable($key,$previousEnvironment[$key],'Process')}
}
