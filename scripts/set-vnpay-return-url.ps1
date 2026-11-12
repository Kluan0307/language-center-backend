param(
  [string]$BaseUrl,
  [string]$EnvFile=(Join-Path $PSScriptRoot '../.env.local')
)
$ErrorActionPreference='Stop'

if(!$BaseUrl){
  try{$taskTunnels=(Invoke-RestMethod -Uri 'http://127.0.0.1:4040/api/tunnels' -TimeoutSec 5).tunnels}
  catch{throw 'Start ngrok http 8080 first. Its local inspector must be available on port 4040.'}
  $taskCandidates=@($taskTunnels|Where-Object {
    if($_.proto -ne 'https'){return $false}
    $taskAddress=[string]$_.config.addr
    if($taskAddress -eq '8080'){return $true}
    if($taskAddress -notmatch '^https?://'){$taskAddress='http://'+$taskAddress}
    $taskTarget=$null
    [Uri]::TryCreate($taskAddress,[UriKind]::Absolute,[ref]$taskTarget) -and
      $taskTarget.Host -in @('localhost','127.0.0.1','[::1]','::1') -and $taskTarget.Port -eq 8080
  })
  if($taskCandidates.Count -ne 1){throw 'Expected one HTTPS ngrok tunnel forwarding to local port 8080. Alternatively supply -BaseUrl with the real backend HTTPS origin.'}
  $BaseUrl=$taskCandidates[0].public_url
}

$taskPublic=$null
if(![Uri]::TryCreate($BaseUrl,[UriKind]::Absolute,[ref]$taskPublic) -or
  $taskPublic.Scheme -ne 'https' -or !$taskPublic.Host -or $taskPublic.UserInfo -or
  $taskPublic.Query -or $taskPublic.Fragment -or $taskPublic.AbsolutePath -ne '/'){
  throw 'BaseUrl must be a real HTTPS origin without credentials, path, query or fragment.'
}
$taskOrigin=$taskPublic.AbsoluteUri.TrimEnd('/')
$taskReturnUrl=$taskOrigin+'/api/v1/finance/payment-providers/vnpay/return'
try{
  $taskResponse=Invoke-WebRequest -UseBasicParsing -Uri $taskReturnUrl -TimeoutSec 15 -Headers @{'ngrok-skip-browser-warning'='true'}
}catch{throw 'The public Return endpoint is unreachable. Restart the current Finance/Gateway code, keep ngrok running, then retry. No environment values were changed.'}
if($taskResponse.StatusCode -ne 200 -or
  !$taskResponse.Content.Contains('<title>Language Center</title>') -or
  !([string]$taskResponse.Headers['Cache-Control']).Contains('no-store') -or
  !([string]$taskResponse.Headers['Content-Security-Policy']).Contains("frame-ancestors 'none'")){
  throw 'This URL does not serve the Language Center VNPAY Return endpoint. No environment values were changed.'
}
if(!(Test-Path -LiteralPath $EnvFile -PathType Leaf)){throw 'Environment file not found.'}
$taskEnvPath=(Resolve-Path -LiteralPath $EnvFile).Path
$taskText=[IO.File]::ReadAllText($taskEnvPath)
$taskPattern=[regex]'(?m)^[ \t]*VNPAY_RETURN_URL[ \t]*=[^\r\n]*'
if($taskPattern.Matches($taskText).Count -gt 1){throw 'Duplicate VNPAY_RETURN_URL keys; remove duplicates before retrying.'}
if($taskPattern.IsMatch($taskText)){
  $taskText=$taskPattern.Replace($taskText,[System.Text.RegularExpressions.MatchEvaluator]{param($taskMatch) 'VNPAY_RETURN_URL='+$taskReturnUrl})
}else{
  $taskNewline=if($taskText.Contains("`r`n")){"`r`n"}else{"`n"}
  if(!$taskText.EndsWith("`n")){$taskText+=$taskNewline}
  $taskText+='VNPAY_RETURN_URL='+$taskReturnUrl+$taskNewline
}
[IO.File]::WriteAllText($taskEnvPath,$taskText,(New-Object System.Text.UTF8Encoding($false)))
Write-Output 'Updated VNPAY_RETURN_URL after verifying the public backend endpoint.'
Write-Output ('Return: '+$taskReturnUrl)
Write-Output ('IPN to register with VNPAY: '+$taskOrigin+'/api/v1/finance/payment-providers/vnpay/ipn')
Write-Output 'Restart Finance to load the new URL. Payment enablement and all other values were preserved.'
