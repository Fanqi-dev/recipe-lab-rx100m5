$ErrorActionPreference = 'Stop'
Push-Location (Split-Path $PSScriptRoot -Parent)
try {
    $java = '..\tools\zulu21\zulu21.52.203-ca-jdk21.0.12.1-win_x64\bin'
    New-Item -ItemType Directory -Force 'out-safe\ui-rx5-classes' | Out-Null
    $sources = @((Get-ChildItem 'verification\ui-shim' -Recurse -Filter '*.java').FullName)
    $sources += @('MenuView','PickerView','PromptView','KeyboardView','LiveRecipeView','ParameterCardView','PhotoReviewChrome','Legend','CanvasText','UiScale','UiChrome') | ForEach-Object { "src/com/voxivoid/recipelab/$_.java" }
    & "$java/javac.exe" -encoding UTF-8 --release 8 -Xlint:-options -cp 'out-safe/test-rx5-classes' -d 'out-safe/ui-rx5-classes' @sources
    if ($LASTEXITCODE -ne 0) { throw 'UI preview compile failed' }
    & "$java/java.exe" '-Djava.awt.headless=true' -cp 'out-safe/ui-rx5-classes;out-safe/test-rx5-classes' com.voxivoid.recipelab.UiPreview 'out-safe/ui-rx5-previews'
    if ($LASTEXITCODE -ne 0) { throw 'UI preview checks failed' }
} finally { Pop-Location }
