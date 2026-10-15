$ErrorActionPreference='Stop'
$project=Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$audit=Split-Path $project -Parent
$java=Join-Path $audit 'tools/zulu21/zulu21.52.203-ca-jdk21.0.12.1-win_x64/bin'
$build=Join-Path $project 'out-safe/live-preview/classes'
New-Item -ItemType Directory -Force $build | Out-Null
Push-Location $project
try {
    $units='Recipes','Params','Favourites','CustomRecipes','RecipeFormat','RecipeFormats','RecipeFormatV1','Library','NameEntry','DevTools','Keys','KeyProbe','Lang','TextEn','TextZhHans','TextZhHant','SafeSettings','SafeRecipeFiles','Rx100M5Policy','SonyAppSession','SonyUi','ReviewLogic'
    $sources=@($units | ForEach-Object {"src/com/voxivoid/recipelab/$_.java"})
    & "$java/javac.exe" -encoding UTF-8 --release 8 -Xlint:-options -d $build @sources
    if($LASTEXITCODE -ne 0){throw 'Model compile failed'}
    $views='MenuView','PickerView','PromptView','KeyboardView','LiveRecipeView','ParameterCardView','PhotoReviewChrome','Legend','CanvasText','UiScale','UiChrome'
    $sources=@((Get-ChildItem verification/ui-shim/android -Filter '*.java' -Recurse).FullName)
    $sources+=@($views | ForEach-Object {"src/com/voxivoid/recipelab/$_.java"})
    $sources+="$PSScriptRoot/com/voxivoid/recipelab/PreviewBridge.java"
    & "$java/javac.exe" -encoding UTF-8 --release 8 -Xlint:-options -cp $build -d $build @sources
    if($LASTEXITCODE -ne 0){throw 'View compile failed'}
} finally {Pop-Location}
