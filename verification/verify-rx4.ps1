$ErrorActionPreference = 'Stop'
$project = Split-Path $PSScriptRoot -Parent
$audit = Split-Path $project -Parent
$env:JAVA_HOME = Join-Path $audit 'tools/zulu21/zulu21.52.203-ca-jdk21.0.12.1-win_x64'
$env:REA_JADX_MCP_JAR = Join-Path $audit 'tools/jadx-headless-mcp-0.7.1-all.jar'
$rea = Join-Path $audit 'tools/rea/node_modules/rea-agents/scripts/rea.mjs'
$apk = Join-Path $audit 'RecipeLab-RX100M5-ProUI-1.6.2.4-PREVIEW.apk'
$folder = Join-Path $project 'out-safe/evidence-rx4'
New-Item -ItemType Directory -Force $folder | Out-Null
function Inspect([string]$name,[string[]]$arguments) {
    $json = node $rea @arguments --format json
    if ($LASTEXITCODE -ne 0) { throw ('REA failed: ' + $name) }
    $result = $json | ConvertFrom-Json
    if (!$result.evidence_id) { throw ('No evidence: ' + $name) }
    $json | Out-File "$folder/$name.json" -Encoding utf8
    return $result
}
Push-Location $project
try {
    $protected = @('SafeSettings','SafeRecipeFiles','Rx100M5Policy','Params','NativeBackup','ReviewIndex','SonyAppSession') | ForEach-Object { "src/com/voxivoid/recipelab/$_.java" }
    $changed = @(git diff --name-only d787407 -- @protected)
    if ($LASTEXITCODE -ne 0 -or $changed.Count -ne 0) { throw 'Protected source changed' }
    $zip = [IO.Compression.ZipFile]::OpenRead($apk)
    try {
        $stream = $zip.GetEntry('lib/armeabi/librecipelab.so').Open(); $sha=[Security.Cryptography.SHA256]::Create()
        try { $hash=[Convert]::ToHexString($sha.ComputeHash($stream)).ToLowerInvariant() } finally { $stream.Dispose(); $sha.Dispose() }
        if ($hash -ne '59606b2d7a58438953fae314d0082ce3ff43c0ed37f90a1838b341ebbab96f4d') { throw 'Native payload changed' }
    } finally { $zip.Dispose() }
    $e = Inspect 'package' @('inspect-android-package',$apk)
    if ($e.normalized_result.version_name -ne '1.6.2-rx100m5.4-preview') { throw 'Wrong version' }
    foreach ($case in @(
        @('live','LiveRecipeView','onDraw'), @('parameter','ParameterCardView','onDraw'),
        @('render','MainActivity','render'), @('menu','MenuView','onDraw'), @('picker','PickerView','onDraw'),
        @('review','MainActivity','onKeyDown'), @('release','MainActivity','onKeyUp'), @('resume','MainActivity','onResume'),
        @('review-exit','Keys','reviewCloses'), @('transaction','SafeSettings','apply')
    )) {
        $e = Inspect $case[0] @('inspect-android-method',$apk,("com.voxivoid.recipelab."+$case[1]),$case[2])
        if ($e.normalized_result.body_status -ne 'available') { throw ('Method unavailable: ' + $case[0]) }
        $source=$e.normalized_result.source.text
        if ($case[0] -eq 'review' -and $source -notmatch 'Keys.reviewCloses') { throw 'Review key routing absent' }
        if ($case[0] -eq 'release' -and $source -notmatch 'reviewCloseScan') { throw 'Review release consumption absent' }
        if ($case[0] -eq 'resume' -and $source -notmatch 'CATEGORY_REC') { throw 'Sony session registration absent' }
        if ($case[0] -eq 'render' -and ($source -notmatch 'parameterCard' -or $source -notmatch 'compactPanel')) { throw 'Approved UI absent' }
        Write-Host ($case[0] + ': available')
    }
    $found = $false
    foreach ($overload in 0,1) {
        $e = Inspect ("apply-$overload") @('inspect-android-method',$apk,'com.voxivoid.recipelab.MainActivity','writeAll','--overload-index',"$overload")
        if ($e.normalized_result.body_status -ne 'available') { throw 'Cannot inspect writeAll overload' }
        if ($e.normalized_result.source.text -match 'safeSettings.apply') { $found=$true }
    }
    if (!$found) { throw 'Transaction call not present in final APK' }
    if (Test-Path "$audit/decompiled-rx4/android") { throw 'Desktop UI shims leaked into APK' }
    if ((Get-Content "$audit/jadx-rx4.log" -Tail 1) -ne 'JADX_ERRORS=0') { throw 'Full DEX export did not complete cleanly' }
    Write-Host 'Final APK static checks passed; Sony firmware and real-camera input remain untested.'
} finally { Pop-Location }
