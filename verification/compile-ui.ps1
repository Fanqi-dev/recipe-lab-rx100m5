$ErrorActionPreference = 'Stop'
$project = Split-Path $PSScriptRoot -Parent
$audit = Split-Path $project -Parent
$java = Join-Path $audit 'tools/zulu21/zulu21.52.203-ca-jdk21.0.12.1-win_x64/bin'
$android = Join-Path $audit 'tools/platform28/android-9/android.jar'
$bt = Join-Path $audit 'tools/build-tools30/android-11'
$build = Join-Path $project 'out-safe/blue-compile'
foreach($dir in 'gen','classes','dex') { New-Item -ItemType Directory -Force "$build/$dir" | Out-Null }
function Run([string]$program,[string[]]$arguments) { & $program @arguments; if($LASTEXITCODE -ne 0){throw 'UI compile failed'} }
Run "$bt/aapt.exe" @('package','-f','-m','--custom-package','com.voxivoid.recipelab','-J',"$build/gen",'-M',"$project/AndroidManifest.xml",'-S',"$project/res",'-A',"$project/assets",'-I',$android)
$sources = @((Get-ChildItem "$project/src/com/voxivoid/recipelab" -Filter '*.java').FullName) + "$build/gen/com/voxivoid/recipelab/R.java"
Run "$java/javac.exe" (@('-encoding','UTF-8','--release','8','-Xlint:-options','-cp',$android,'-d',"$build/classes") + $sources)
$classes = @((Get-ChildItem "$build/classes" -Recurse -Filter '*.class').FullName)
Run "$java/java.exe" (@('-cp',"$audit/tools/r8-8.3.37.jar",'com.android.tools.r8.D8','--release','--min-api','10','--lib',$android,'--output',"$build/dex") + $classes)
Write-Host 'Production resources, Java and min-API-10 DEX compile passed. No APK signed or installed.'
