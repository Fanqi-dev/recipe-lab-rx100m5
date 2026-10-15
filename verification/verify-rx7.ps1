$ErrorActionPreference='Stop'
$project=Split-Path $PSScriptRoot -Parent
$audit=Split-Path $project -Parent
$env:JAVA_HOME=Join-Path $audit 'tools/zulu21/zulu21.52.203-ca-jdk21.0.12.1-win_x64'
$env:REA_JADX_MCP_JAR=Join-Path $audit 'tools/jadx-headless-mcp-0.7.1-all.jar'
$rea=Join-Path $audit 'tools/rea/node_modules/rea-agents/scripts/rea.mjs'
$apk=Join-Path $audit 'RecipeLab-RX100M5-UnifiedUI-1.6.2.7-TEST.apk'
$folder=Join-Path $project 'out-safe/evidence-rx7'
New-Item -ItemType Directory -Force $folder | Out-Null
function Inspect([string]$class,[string]$method,[string[]]$required){
    $json=node $rea inspect-android-method $apk ("com.voxivoid.recipelab."+$class) $method --format json
    if($LASTEXITCODE -ne 0){throw ('REA failed: '+$class+'.'+$method)}
    $result=$json|ConvertFrom-Json
    if(!$result.evidence_id -or $result.normalized_result.body_status -ne 'available'){throw ('Missing method: '+$method)}
    $json|Out-File "$folder/$class-$method.json" -Encoding utf8
    $text=$result.normalized_result.source.text
    foreach($token in $required){if(!$text.Contains($token)){throw ('Absent in '+$class+'.'+$method+': '+$token)}}
    return $text
}
Push-Location $project
try{
    $protected=@('SafeSettings','SafeRecipeFiles','Rx100M5Policy','Params','NativeBackup')|ForEach-Object{"src/com/voxivoid/recipelab/$_.java"}
    if(@(git diff --name-only d1e930b -- @protected).Count -ne 0 -or $LASTEXITCODE -ne 0){throw 'Protected storage/policy code changed'}
    if((Get-FileHash $apk).Hash.ToLowerInvariant() -ne 'acd55b48b1f7bc57076fd4a6b6bd420d1c423f9d961cfce37ef42b61a85e1eab'){throw 'APK hash changed'}
    $zip=[IO.Compression.ZipFile]::OpenRead($apk)
    try{
        $stream=$zip.GetEntry('lib/armeabi/librecipelab.so').Open();$sha=[Security.Cryptography.SHA256]::Create()
        try{$hash=[Convert]::ToHexString($sha.ComputeHash($stream)).ToLowerInvariant()}finally{$stream.Dispose();$sha.Dispose()}
        if($hash -ne '59606b2d7a58438953fae314d0082ce3ff43c0ed37f90a1838b341ebbab96f4d'){throw 'JNI changed'}
        foreach($entry in $zip.Entries){if($entry.FullName -match '(^verification/|PreviewBridge|server\.mjs|\.keystore$)'){throw 'Development/private artifact shipped'}}
    }finally{$zip.Dispose()}
    $java=Join-Path $env:JAVA_HOME 'bin/java.exe';$bt=Join-Path $audit 'tools/build-tools30/android-11'
    $signature=& $java -jar "$bt/lib/apksigner.jar" verify --min-sdk-version 10 --verbose --print-certs $apk
    if($LASTEXITCODE -ne 0 -or !($signature -match '92673172ec26c9282f47d53282a0ac48a46a5fc15fc1e1935e09d8ee13e47bd7')){throw 'Wrong signing identity'}
    & "$bt/zipalign.exe" -c 4 $apk
    if($LASTEXITCODE -ne 0){throw 'Alignment failed'}
    $badging=& "$bt/aapt.exe" dump badging $apk
    if($LASTEXITCODE -ne 0 -or !($badging -match "package: name='com.voxivoid.recipelab.rx100m5safe' versionCode='10603007' versionName='1.6.2-rx100m5.7-test'")){throw 'Wrong package version'}
    $decompiled=Join-Path $audit 'decompiled-rx7-final'
    if((Get-Content "$audit/jadx-rx7-final.log" -Tail 1) -ne 'JADX_ERRORS=0'){throw 'Full DEX export failed'}
    foreach($removed in 'PhotoReviewView.java','ReviewIndex.java','PreviewBridge.java'){
        if(Test-Path "$decompiled/com/voxivoid/recipelab/$removed"){throw 'Old viewer or desktop bridge shipped'}
    }
    if((Test-Path "$decompiled/android") -or (Test-Path "$decompiled/java/awt")){throw 'Desktop shims shipped'}
    $write=Inspect 'MainActivity' 'writeAll' @('safeSettings.apply','this.settingsReady','this.previewOk','Params.lockedFrom','Read-back mismatch','collapseApplied')
    if($write.Contains('openPrompt(')){throw 'Secondary apply confirmation returned'}
    Inspect 'MainActivity' 'openPhotos' @('photoReview.prepare','verifiedPreview','stopPreview','removeCallback','registerSession(true)','photoReview.open')|Out-Null
    Inspect 'MainActivity' 'resumeAfterReview' @('reviewResourceFailed','activityResumed','load()','applyPreview()','Preview reopen failed')|Out-Null
    Inspect 'MainActivity' 'onKeyDown' @('ReviewLogic.command','photoReview.command','openPhotos()')|Out-Null
    Inspect 'MainActivity' 'onKeyUp' @('photoReview.isOpen','reviewCloseScan','reviewCloseKey')|Out-Null
    Inspect 'UiFont' 'cameraLayout' @('UiScale.of','scaleLayout')|Out-Null
    Inspect 'UiChrome' 'header' @('CanvasText.fit','CanvasText.baseline','separator')|Out-Null
    Inspect 'UiChrome' 'status' @('CanvasText.statusWidth','statusFrame','statusText','CanvasText.baseline')|Out-Null
    Inspect 'LiveRecipeView' 'onDraw' @('this.chrome.status','drawSummary','this.legend.drawWrapped','state_edited')|Out-Null
    Inspect 'LiveRecipeView' 'onMeasure' @('72.0f','54.0f','this.showHints')|Out-Null
    Inspect 'PromptView' 'onDraw' @('this.chrome.selection','CanvasText.baseline','CanvasText.fit')|Out-Null
    Inspect 'SonyPhotoReview' 'close' @('tickets.close','worker.execute','source.close','releaseView.invoke','listener.closed')|Out-Null
    Write-Host '12 final-APK REA method checks passed; signature/version/alignment verified, storage guards/JNI preserved, developer artifacts excluded.'
    Write-Host 'Camera UI, native photo interfaces and return-to-shooting still require device testing.'
}finally{Pop-Location}
