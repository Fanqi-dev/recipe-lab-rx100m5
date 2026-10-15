$ErrorActionPreference='Stop'
$project=Split-Path $PSScriptRoot -Parent
$audit=Split-Path $project -Parent
$env:JAVA_HOME=Join-Path $audit 'tools/zulu21/zulu21.52.203-ca-jdk21.0.12.1-win_x64'
$env:REA_JADX_MCP_JAR=Join-Path $audit 'tools/jadx-headless-mcp-0.7.1-all.jar'
$rea=Join-Path $audit 'tools/rea/node_modules/rea-agents/scripts/rea.mjs'
$apk=Join-Path $audit 'RecipeLab-RX100M5-NativeReview-1.6.2.6-TEST.apk'
$folder=Join-Path $project 'out-safe/evidence-rx6'
New-Item -ItemType Directory -Force $folder | Out-Null
function Inspect([string]$class,[string]$method,[string[]]$required) {
    $json=node $rea inspect-android-method $apk ("com.voxivoid.recipelab."+$class) $method --format json
    if($LASTEXITCODE -ne 0){throw ('REA failed: '+$class+'.'+$method)}
    $result=$json | ConvertFrom-Json
    if(!$result.evidence_id -or $result.normalized_result.body_status -ne 'available'){throw ('Missing method: '+$method)}
    $json | Out-File "$folder/$class-$method.json" -Encoding utf8
    $text=$result.normalized_result.source.text
    foreach($token in $required){if(!$text.Contains($token)){throw ('Absent in '+$class+'.'+$method+': '+$token)}}
    return $text
}
Push-Location $project
try {
    $protected=@('SafeSettings','SafeRecipeFiles','Rx100M5Policy','Params','NativeBackup') | ForEach-Object {"src/com/voxivoid/recipelab/$_.java"}
    if(@(git diff --name-only d1e930b -- @protected).Count -ne 0 -or $LASTEXITCODE -ne 0){throw 'Protected storage/policy code changed'}
    if((Get-FileHash $apk).Hash.ToLowerInvariant() -ne 'ebdaf15e3affa43a3b9d19929f1f7e4ff198ef6364f7ef74b68ab72cf5b4a398'){throw 'APK hash changed'}
    $zip=[IO.Compression.ZipFile]::OpenRead($apk)
    try {
        $stream=$zip.GetEntry('lib/armeabi/librecipelab.so').Open();$sha=[Security.Cryptography.SHA256]::Create()
        try {$hash=[Convert]::ToHexString($sha.ComputeHash($stream)).ToLowerInvariant()}finally{$stream.Dispose();$sha.Dispose()}
        if($hash -ne '59606b2d7a58438953fae314d0082ce3ff43c0ed37f90a1838b341ebbab96f4d'){throw 'JNI changed'}
    }finally{$zip.Dispose()}
    $decompiled=Join-Path $audit 'decompiled-rx6-final'
    if((Get-Content "$audit/jadx-rx6-final.log" -Tail 1) -ne 'JADX_ERRORS=0'){throw 'Full DEX export failed'}
    foreach($removed in 'PhotoReviewView.java','ReviewIndex.java','PreviewBridge.java'){
        if(Test-Path "$decompiled/com/voxivoid/recipelab/$removed"){throw 'Old viewer or desktop bridge shipped'}
    }
    if((Test-Path "$decompiled/android") -or (Test-Path "$decompiled/java/awt")){throw 'Desktop shims shipped'}
    $write=Inspect 'MainActivity' 'writeAll' @('safeSettings.apply','this.settingsReady','this.previewOk','Params.lockedFrom','Read-back mismatch','collapseApplied')
    if($write.Contains('openPrompt(')){throw 'Secondary apply confirmation returned'}
    Inspect 'MainActivity' 'openPhotos' @('photoReview.prepare','verifiedPreview','stopPreview','removeCallback','registerSession(true)','photoReview.open') | Out-Null
    Inspect 'MainActivity' 'resumeAfterReview' @('reviewResourceFailed','activityResumed','load()','applyPreview()','Preview reopen failed') | Out-Null
    Inspect 'MainActivity' 'onKeyDown' @('ReviewLogic.command','photoReview.command','openPhotos()') | Out-Null
    Inspect 'MainActivity' 'onKeyUp' @('photoReview.isOpen','reviewCloseScan','reviewCloseKey') | Out-Null
    Inspect 'LiveRecipeView' 'onDraw' @('CanvasText.statusWidth','CanvasText.baseline') | Out-Null
    Inspect 'PickerView' 'onDraw' @('CanvasText.statusWidth','CanvasText.baseline') | Out-Null
    Inspect 'MenuView' 'onDraw' @('valueColumnWidth()','CanvasText.baseline') | Out-Null
    Inspect 'CanvasText' 'baseline' @('ascent()','descent()') | Out-Null
    Inspect 'CanvasText' 'statusWidth' @('Math.max','state_active','state_preview') | Out-Null
    Inspect 'SonyPhotoSource' 'query' @('AvindexStore','getContentResolver().query','content_created_utc_date_time DESC') | Out-Null
    Inspect 'SonyPhotoSource' 'decode' @('MAIN','SCN','imageType','this.decode.invoke') | Out-Null
    Inspect 'SonyPhotoReview' 'request' @('tickets.next','worker.getQueue().clear','tickets.accepts','registerDisplayListener','SonyPhotoSource.release') | Out-Null
    Inspect 'SonyPhotoReview' 'close' @('tickets.close','worker.execute','source.close','releaseView.invoke','listener.closed') | Out-Null
    Write-Host '14 REA method checks passed; original storage guards/JNI preserved; desktop bridge and old bitmap viewer excluded.'
    Write-Host 'Native decode/display, firmware button delivery, resource lifetime and camera return still require device testing.'
}finally{Pop-Location}
