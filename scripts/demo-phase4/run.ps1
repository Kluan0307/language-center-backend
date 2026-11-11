param([switch]$SkipBuild)
$ErrorActionPreference='Stop'
$root=(Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
Push-Location $root
$runDir=$null
try {
 if(-not $SkipBuild){& mvn "-Dmaven.repo.local=$(Join-Path $env:USERPROFILE '.m2/repository')" -DskipTests package *> 'storage/demo-phase4-package.log';if($LASTEXITCODE -ne 0){throw 'Backend package failed'}}
 & node scripts/demo-phase4/prepare.cjs;if($LASTEXITCODE -ne 0){throw 'Demo schema preparation failed'}
 $runDir=(Get-Content 'storage/demo-phase4-latest.txt' -Raw).Trim()
 & "$PSScriptRoot/bootstrap.ps1" -RunDir $runDir
 & "$PSScriptRoot/../acceptance/runtime.ps1" -Action start -Service all -RunDir $runDir
 & node scripts/demo-phase4/build-demo.mjs *> (Join-Path $runDir 'fixture.console.log');if($LASTEXITCODE -ne 0){throw 'Demo API creation failed; inspect fixture.console.log'}
 & node scripts/demo-phase4/verify.cjs *> (Join-Path $runDir 'database.console.log');if($LASTEXITCODE -ne 0){throw 'Demo SQL verification failed'}
 & node scripts/demo-phase4/browser.mjs *> (Join-Path $runDir 'browser.console.log');if($LASTEXITCODE -ne 0){throw 'Demo UI reads failed'}
 & node scripts/demo-phase4/summarize.cjs *> (Join-Path $runDir 'summary.console.log');if($LASTEXITCODE -ne 0){throw 'Demo summary incomplete'}
 Write-Output "PASSED:40 API checks,8 SQL checks,4 real UI checks. Data retained: $runDir"
} finally {
 if($runDir){& "$PSScriptRoot/../acceptance/runtime.ps1" -Action stop -Service all -RunDir $runDir}
 Pop-Location
}
