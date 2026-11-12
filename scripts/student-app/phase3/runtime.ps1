param([ValidateSet('start','stop')][string]$Action='start',[ValidateSet(3,4,5,6,7,8,9)][int]$Phase=3)
$ErrorActionPreference='Stop'
$root=(Resolve-Path "$PSScriptRoot/../../..").Path
$dir=(Get-Content "$root/storage/student-phase${Phase}-latest.txt" -Raw).Trim()
$run=Get-Content "$dir/runtime.json" -Raw | ConvertFrom-Json
if($run.database -notmatch '^lc_student_g[3456789]_\d+$'){throw 'Only private student qualification database permitted'}
$tag="-Dlc.studentg$Phase=$($run.database)"
if($Action -eq 'stop'){
 Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object {$_.CommandLine -and $_.CommandLine.Contains($tag)} | ForEach-Object {Stop-Process -Id $_.ProcessId -ErrorAction SilentlyContinue}
 exit 0
}
$keys=@('DB_URL','DB_USERNAME','DB_PASSWORD','JWT_SECRET','AUTH_URL','ACADEMIC_URL','TEACHING_URL','FINANCE_URL','CORS_ALLOWED_ORIGINS','SPRING_CONFIG_IMPORT','LOG_FILE','SPRING_PROFILES_ACTIVE','DOCUMENT_STORAGE','VNPAY_ENABLED','VNPAY_TMN_CODE','VNPAY_HASH_SECRET','VNPAY_CHECKOUT_URL','VNPAY_QUERY_URL','VNPAY_RETURN_URL','PAYMENT_JOBS_ENABLED','VNPAY_SERVER_IP','VNPAY_TTL_MINUTES','NOTIFICATION_JOBS_ENABLED','NOTIFICATIONS_PUSH_ENABLED','EXPO_PROJECT_ID','EXPO_PUSH_ACCESS_TOKEN')
$previous=@{};foreach($key in $keys){$previous[$key]=[Environment]::GetEnvironmentVariable($key,'Process')}
try{
 foreach($line in Get-Content "$root/.env.local" -Encoding UTF8){if($line -match '^(DB_URL|DB_USERNAME|DB_PASSWORD|JWT_SECRET)=(.*)$'){[Environment]::SetEnvironmentVariable($Matches[1],$Matches[2],'Process')}}
 $env:DB_URL=$env:DB_URL -replace '(jdbc:mysql://[^/]+/)[^?]+',("`${1}"+$run.database)
 $env:AUTH_URL='http://127.0.0.1:28181';$env:ACADEMIC_URL='http://127.0.0.1:28182';$env:TEACHING_URL='http://127.0.0.1:28183';$env:FINANCE_URL='http://127.0.0.1:28184'
 $env:CORS_ALLOWED_ORIGINS='http://localhost:18091,http://127.0.0.1:18091,http://localhost:18092,http://127.0.0.1:18092'
 $env:DOCUMENT_STORAGE=Join-Path $dir 'documents';New-Item $env:DOCUMENT_STORAGE -ItemType Directory -Force | Out-Null
 $env:SPRING_CONFIG_IMPORT='optional:file:/nonexistent-student-test.properties';$env:SPRING_PROFILES_ACTIVE='prod'
 $items=@(@('api-gateway',0),@('auth-service',1),@('academic-service',2),@('teaching-service',3),@('finance-service',4))
 foreach($item in $items){
 $env:SPRING_PROFILES_ACTIVE='prod';$env:VNPAY_ENABLED='false';$env:VNPAY_TMN_CODE='';$env:VNPAY_HASH_SECRET='';$env:NOTIFICATION_JOBS_ENABLED='false';$env:NOTIFICATIONS_PUSH_ENABLED='false';$env:EXPO_PROJECT_ID='';$env:EXPO_PUSH_ACCESS_TOKEN=''
 if($Phase -eq 9){$env:NOTIFICATION_JOBS_ENABLED='true';$env:EXPO_PROJECT_ID='99999999-9999-4999-8999-999999999999'}
 if($Phase -eq 8 -and $item[0] -eq 'finance-service'){$bank=Get-Content "$dir/protocol-secrets.json" -Raw|ConvertFrom-Json;$env:SPRING_PROFILES_ACTIVE='payment-test';$env:VNPAY_ENABLED='true';$env:VNPAY_TMN_CODE=$bank.merchant;$env:VNPAY_HASH_SECRET=$bank.secret;$env:VNPAY_CHECKOUT_URL='http://127.0.0.1:29180/checkout';$env:VNPAY_QUERY_URL='http://127.0.0.1:29180/query';$env:VNPAY_RETURN_URL='http://127.0.0.1:28180/api/v1/finance/payment-providers/vnpay/return';$env:PAYMENT_JOBS_ENABLED='false';$env:VNPAY_TTL_MINUTES='15';$env:VNPAY_SERVER_IP='127.0.0.1'}
 $port=$run.basePort+$item[1];if(Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue){throw "Private port occupied: $port"};$jar=@(Get-ChildItem "$root/$($item[0])/target/*.jar");if($jar.Count -ne 1){throw 'Expected one JAR'};$env:LOG_FILE="$dir/$($item[0]).application.log";$process=Start-Process java -ArgumentList @('-Xmx256m',$tag,'-jar',$jar[0].FullName,"--server.port=$port",'--server.address=127.0.0.1','--spring.jpa.hibernate.ddl-auto=validate','--logging.level.org.hibernate.SQL=OFF','--debug=false') -WorkingDirectory $root -WindowStyle Hidden -PassThru -RedirectStandardOutput "$dir/$($item[0]).out.log" -RedirectStandardError "$dir/$($item[0]).err.log";Write-Output "Started private $($item[0]) launcher $($process.Id)"}
}finally{foreach($key in $keys){[Environment]::SetEnvironmentVariable($key,$previous[$key],'Process')}}
