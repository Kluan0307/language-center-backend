param()
$ErrorActionPreference = 'Stop'
$workspaceRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$jarFile = Join-Path $workspaceRoot 'assistant-service/target/assistant-service-0.0.1-SNAPSHOT.jar'
if (-not (Test-Path -LiteralPath $jarFile)) { throw 'Build assistant-service first: mvn verify' }
$jarFile = (Resolve-Path -LiteralPath $jarFile).Path
$logDirectory = Join-Path $workspaceRoot 'storage/c3-smoke'
[IO.Directory]::CreateDirectory($logDirectory) | Out-Null
$outFile = Join-Path $logDirectory 'stdout.log'
$errFile = Join-Path $logDirectory 'stderr.log'
$javaBin = (Get-Command java -ErrorAction Stop).Source
$smokeAppId = $null
$smokeProcess = Start-Process -FilePath $javaBin -ArgumentList @('-jar', $jarFile, '--spring.config.import=', '--server.port=0', '--assistant.mcp.enabled=false') -WorkingDirectory (Join-Path $workspaceRoot 'assistant-service') -WindowStyle Hidden -RedirectStandardOutput $outFile -RedirectStandardError $errFile -PassThru
try {
  $smokePort = $null
  for ($attempt = 0; $attempt -lt 100; $attempt++) {
    $smokeProcess.Refresh()
    if ($smokeProcess.HasExited) { throw "Smoke process exited; see $errFile" }
    $smokeText = Get-Content -LiteralPath $outFile -Raw
    if ($null -ne $smokeText) {
      $appIdMatch = [regex]::Match($smokeText, 'Starting AssistantServiceApplication[^\r\n]* with PID (\d+)')
      if ($appIdMatch.Success) { $smokeAppId = [int]$appIdMatch.Groups[1].Value }
      $portMatch = [regex]::Match($smokeText, 'Tomcat started on port (\d+)')
      if ($portMatch.Success) { $smokePort = [int]$portMatch.Groups[1].Value; break }
    }
    Start-Sleep -Milliseconds 200
  }
  if (-not $smokePort) { throw "Smoke startup timeout; see $outFile" }
  $health = Invoke-RestMethod -Uri "http://127.0.0.1:$smokePort/actuator/health" -TimeoutSec 5
  $mcpStatus = 0
  try {
    Invoke-WebRequest -UseBasicParsing -Method Post -Uri "http://127.0.0.1:$smokePort/mcp" -ContentType 'application/json' -Body '{}' -TimeoutSec 5 | Out-Null
  } catch {
    if ($_.Exception.Response) { $mcpStatus = [int]$_.Exception.Response.StatusCode } else { throw }
  }
  if ($health.status -ne 'UP' -or $mcpStatus -ne 401) { throw 'Smoke assertion failed' }
  $smokeResult = @{ health = $health.status; mcpPostStatus = $mcpStatus; port = $smokePort; processId = $smokeAppId; jar = $jarFile; configuration = 'MCP disabled, no env file import' }
} finally {
  # Windows Process.HasExited was observed true for a still-live launched JVM.
  # The PID below comes only from this run's fresh, redirected startup log.
  if ($null -eq $smokeAppId -and (Test-Path -LiteralPath $outFile)) {
    $appIdMatch = [regex]::Match((Get-Content -LiteralPath $outFile -Raw), 'Starting AssistantServiceApplication[^\r\n]* with PID (\d+)')
    if ($appIdMatch.Success) { $smokeAppId = [int]$appIdMatch.Groups[1].Value }
  }
  if ($null -eq $smokeAppId) { $smokeAppId = $smokeProcess.Id }
  & taskkill /PID $smokeAppId /F 2>$null | Out-Null
  if ($smokePort) {
    Start-Sleep -Milliseconds 150
    $stillServing = $false
    try { Invoke-RestMethod -Uri "http://127.0.0.1:$smokePort/actuator/health" -TimeoutSec 2 | Out-Null; $stillServing = $true } catch { }
    if ($stillServing) { throw 'Owned smoke process is still serving after cleanup.' }
  }
  $smokeProcess.Dispose()
}
$smokeResult.cleanup = 'PASS: health port closed'
$smokeResult | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $logDirectory 'result.json') -Encoding UTF8
Write-Output 'PASS: packaged JAR health UP; HTTP MCP denied 401; owned health port closed after cleanup. No Auth, database or Gemini calls.'
