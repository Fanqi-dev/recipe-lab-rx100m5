$ErrorActionPreference = 'Stop'
$project = $PSScriptRoot
$audit = Split-Path $project -Parent
$java = Join-Path $audit 'tools\zulu21\zulu21.52.203-ca-jdk21.0.12.1-win_x64\bin'
$bt = Join-Path $audit 'tools\build-tools30\android-11'
$android = Join-Path $audit 'tools\platform28\android-9\android.jar'
$build = Join-Path $project 'out-safe\build-rx4'
$native = Join-Path $audit 'librecipelab.so'
foreach ($directory in 'gen','classes','dex','apklib\lib\armeabi') { New-Item -ItemType Directory -Force (Join-Path $build $directory) | Out-Null }
function Run([string]$program, [string[]]$arguments) { & $program @arguments; if ($LASTEXITCODE -ne 0) { throw "$program failed ($LASTEXITCODE)" } }
# The upstream ARM JNI bridge is deliberately reused byte-for-byte, not rebuilt or patched.
Copy-Item -LiteralPath $native -Destination (Join-Path $build 'apklib\lib\armeabi\librecipelab.so')
Run "$bt\aapt.exe" @('package','-f','-m','--custom-package','com.voxivoid.recipelab','-J',"$build\gen",'-M',"$project\AndroidManifest.xml",'-S',"$project\res",'-A',"$project\assets",'-I',$android)
$sources = @((Get-ChildItem "$project\src\com\voxivoid\recipelab" -Filter '*.java').FullName) + "$build\gen\com\voxivoid\recipelab\R.java"
Run "$java\javac.exe" (@('-encoding','UTF-8','--release','8','-Xlint:-options','-cp',$android,'-d',"$build\classes") + $sources)
$classes = @((Get-ChildItem "$build\classes" -Recurse -Filter '*.class').FullName)
Run "$java\java.exe" (@('-cp',"$audit\tools\r8-8.3.37.jar",'com.android.tools.r8.D8','--release','--min-api','10','--lib',$android,'--output',"$build\dex") + $classes)
Run "$bt\aapt.exe" @('package','-f','--custom-package','com.voxivoid.recipelab','-M',"$project\AndroidManifest.xml",'-S',"$project\res",'-A',"$project\assets",'-I',$android,'-F',"$build\unsigned.apk")
Push-Location "$build\dex"
try { Run "$bt\aapt.exe" @('add',"$build\unsigned.apk",'classes.dex') } finally { Pop-Location }
Push-Location "$build\apklib"
try { Run "$bt\aapt.exe" @('add',"$build\unsigned.apk",'lib/armeabi/librecipelab.so') } finally { Pop-Location }
Run "$bt\zipalign.exe" @('-f','4',"$build\unsigned.apk","$build\aligned.apk")
$key = Join-Path $project 'rx100m5-local.keystore'
if (!(Test-Path -LiteralPath $key)) { Run "$java\keytool.exe" @('-genkeypair','-keystore',$key,'-alias','rx100m5-local','-keyalg','RSA','-keysize','2048','-validity','10000','-storepass','android','-keypass','android','-dname','CN=RecipeLab RX100M5 Local Build') }
$apk = Join-Path $audit 'RecipeLab-RX100M5-ProUI-1.6.2.4-PREVIEW.apk'
Run "$java\java.exe" @('-jar',"$bt\lib\apksigner.jar",'sign','--ks',$key,'--ks-key-alias','rx100m5-local','--ks-pass','pass:android','--key-pass','pass:android','--min-sdk-version','10','--v1-signing-enabled','true','--v2-signing-enabled','false','--v3-signing-enabled','false','--out',$apk,"$build\aligned.apk")
Run "$java\java.exe" @('-jar',"$bt\lib\apksigner.jar",'verify','--min-sdk-version','10','--verbose','--print-certs',$apk)
Run "$bt\zipalign.exe" @('-c','4',$apk)
Get-FileHash -LiteralPath $apk -Algorithm SHA256
