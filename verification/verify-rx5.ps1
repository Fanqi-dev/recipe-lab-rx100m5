$ErrorActionPreference='Stop'
$project=Split-Path $PSScriptRoot -Parent
$audit=Split-Path $project -Parent
$env:JAVA_HOME=Join-Path $audit 'tools/zulu21/zulu21.52.203-ca-jdk21.0.12.1-win_x64'
$env:REA_JADX_MCP_JAR=Join-Path $audit 'tools/jadx-headless-mcp-0.7.1-all.jar'
$rea=Join-Path $audit 'tools/rea/node_modules/rea-agents/scripts/rea.mjs'
$apk=Join-Path $audit 'RecipeLab-RX100M5-ProUI-1.6.2.5-TEST.apk'
$folder=Join-Path $project 'out-safe/evidence-rx5'
New-Item -ItemType Directory -Force $folder | Out-Null
function Inspect([string]$name,[string]$class,[string]$method) {
    $json=node $rea inspect-android-method $apk ("com.voxivoid.recipelab."+$class) $method --format json
    if($LASTEXITCODE -ne 0){throw ('REA failed: '+$name)}
    $result=$json | ConvertFrom-Json
    if(!$result.evidence_id -or $result.normalized_result.body_status -ne 'available'){throw ('Missing method: '+$name)}
    $json | Out-File "$folder/$name.json" -Encoding utf8
    return $result.normalized_result.source.text
}
Push-Location $project
try {
    $protected=@('SafeSettings','SafeRecipeFiles','Rx100M5Policy','Params','NativeBackup') | ForEach-Object {"src/com/voxivoid/recipelab/$_.java"}
    $changed=@(git diff --name-only d1e930b -- @protected)
    if($LASTEXITCODE -ne 0 -or $changed.Count -ne 0){throw 'Protected storage/policy code changed'}
    if((Get-FileHash $apk).Hash.ToLowerInvariant() -ne '5a7a6ce9d64d47635ca6875232768d8e28a76fe2111ba9936b5e129ce0589fcc'){throw 'APK hash changed'}
    $zip=[IO.Compression.ZipFile]::OpenRead($apk)
    try {
        $stream=$zip.GetEntry('lib/armeabi/librecipelab.so').Open(); $sha=[Security.Cryptography.SHA256]::Create()
        try {$hash=[Convert]::ToHexString($sha.ComputeHash($stream)).ToLowerInvariant()} finally {$stream.Dispose();$sha.Dispose()}
        if($hash -ne '59606b2d7a58438953fae314d0082ce3ff43c0ed37f90a1838b341ebbab96f4d'){throw 'Native payload changed'}
    } finally {$zip.Dispose()}
    $decompiled=Join-Path $audit 'decompiled-rx5'
    if((Get-Content "$audit/jadx-rx5.log" -Tail 1) -ne 'JADX_ERRORS=0'){throw 'Full DEX export failed'}
    foreach($removed in 'PhotoReviewView.java','ReviewIndex.java'){
        if(Test-Path "$decompiled/com/voxivoid/recipelab/$removed"){throw 'Removed photo viewer shipped'}
    }
    if(Test-Path "$decompiled/android"){throw 'Desktop shims shipped'}
    $write=Inspect 'direct-apply' 'MainActivity' 'writeAll'
    foreach($required in 'safeSettings.apply','this.settingsReady','this.previewOk','Params.lockedFrom','Read-back mismatch','collapseApplied'){
        if(!$write.Contains($required)){throw ('Apply guard absent: '+$required)}
    }
    if($write.Contains('openPrompt(')){throw 'Secondary apply confirmation still present'}
    foreach($method in 'pick','pickInBrowser'){
        $text=Inspect $method 'MainActivity' $method
        if(!$text.Contains('writeAll()') -or $text.Contains('openPrompt(')){throw 'Selection is not direct apply'}
    }
    foreach($method in 'onKeyDown','onKeyUp'){
        $text=Inspect $method 'MainActivity' $method
        if(!$text.Contains('207') -or !$text.Contains(('super.'+$method)) -or $text.Contains('photoReview')){throw 'Native PLAY delegation missing'}
    }
    $collapse=Inspect 'compact-success' 'MainActivity' 'collapseApplied'
    if(!$collapse.Contains('this.row = 0') -or !$collapse.Contains('this.onActions = false')){throw 'Apply does not collapse editor'}
    $keys=Inspect 'native-session' 'SonyAppSession' 'pullingBackKeys'
    if(!$keys.Contains('KEY_PLAY') -or !$keys.Contains('KEY_USB_CONNECT')){throw 'Native session handoff missing'}
    Inspect 'strip-height' 'LiveRecipeView' 'onMeasure' | Out-Null
    Inspect 'strip-style' 'LiveRecipeView' 'onDraw' | Out-Null
    Write-Host 'Final APK checks passed: direct selection, native PLAY delegation, compact status and original storage guards.'
    Write-Host 'Native return-to-app, Sony firmware behaviour and visual performance remain untested on camera.'
} finally {Pop-Location}
