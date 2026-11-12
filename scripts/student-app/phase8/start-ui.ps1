$ErrorActionPreference='Stop'
$dir=(Get-Content "$PSScriptRoot/../../../storage/student-phase8-latest.txt" -Raw).Trim()
$root=(Resolve-Path "$PSScriptRoot/../../..").Path
$app=(Resolve-Path "$root/../../language-center-student-app").Path
$web=(Resolve-Path "$root/../../language-center-web-frontend").Path
foreach($port in @(18091,18092,19390)){if(Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue){throw "Private UI port occupied: $port"}}
$oldProtocol=$env:EXPO_PUBLIC_PAYMENT_PROTOCOL_TEST;$env:EXPO_PUBLIC_PAYMENT_PROTOCOL_TEST='1'
$oldApp=$env:EXPO_PUBLIC_API_BASE_URL;$oldWeb=$env:VITE_API_BASE_URL
try{
 $env:EXPO_PUBLIC_API_BASE_URL='http://127.0.0.1:28180/api/v1'
 $expo=Start-Process node -ArgumentList @('node_modules/expo/bin/cli','start','--web','--port','18091') -WorkingDirectory $app -WindowStyle Hidden -PassThru -RedirectStandardOutput "$dir/expo.out.log" -RedirectStandardError "$dir/expo.err.log"
 $env:VITE_API_BASE_URL='http://127.0.0.1:28180/api/v1'
 $vite=Start-Process node -ArgumentList @('node_modules/vite/bin/vite.js','--host','127.0.0.1','--port','18092','--strictPort') -WorkingDirectory $web -WindowStyle Hidden -PassThru -RedirectStandardOutput "$dir/vite.out.log" -RedirectStandardError "$dir/vite.err.log"
 $edge=Start-Process 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe' -ArgumentList @('--headless=new','--disable-gpu','--no-first-run','--disable-extensions','--disable-sync','--no-default-browser-check','--remote-debugging-port=19390',('--user-data-dir='+$dir+'/edge-profile'),'http://localhost:18091') -WindowStyle Hidden -PassThru
 @{expo=$expo.Id;vite=$vite.Id;edge=$edge.Id}|ConvertTo-Json|Set-Content "$dir/ui-processes.json" -Encoding UTF8
 Write-Output 'Started dedicated G8 UI processes'
}finally{$env:EXPO_PUBLIC_PAYMENT_PROTOCOL_TEST=$oldProtocol;$env:EXPO_PUBLIC_API_BASE_URL=$oldApp;$env:VITE_API_BASE_URL=$oldWeb}
