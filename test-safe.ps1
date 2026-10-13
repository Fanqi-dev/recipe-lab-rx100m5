$ErrorActionPreference = 'Stop'
$audit = Split-Path $PSScriptRoot -Parent
$java = "$audit\tools\zulu21\zulu21.52.203-ca-jdk21.0.12.1-win_x64\bin"
Push-Location $PSScriptRoot
try {
    New-Item -ItemType Directory -Force 'out-safe\test-classes' | Out-Null
    $units = 'Recipes','Params','Favourites','CustomRecipes','RecipeFormat','RecipeFormats','RecipeFormatV1','Library','NameEntry','DevTools','Keys','KeyProbe','Lang','TextEn','TextZhHans','TextZhHant','SafeSettings','SafeRecipeFiles','Rx100M5Policy','ReviewIndex','SonyAppSession'
    $sources = $units | ForEach-Object { "src\com\voxivoid\recipelab\$_.java" }
    & "$java\javac.exe" -encoding UTF-8 --release 8 -Xlint:-options -d 'out-safe\test-classes' @sources
    if ($LASTEXITCODE -ne 0) { throw 'Compile failed' }
    $tests = (Get-ChildItem 'test' -Filter '*.java' -Recurse).FullName
    & "$java\javac.exe" -encoding UTF-8 --release 8 -Xlint:-options -cp "out-safe\test-classes;$audit\tools\junit.jar" -d 'out-safe\test-classes' @tests
    if ($LASTEXITCODE -ne 0) { throw 'Test compile failed' }
    & "$java\java.exe" -jar "$audit\tools\junit.jar" execute --class-path 'out-safe\test-classes' --scan-class-path --details summary --disable-banner --reports-dir 'out-safe\test-reports'
    if ($LASTEXITCODE -ne 0) { throw 'Tests failed' }
} finally { Pop-Location }
