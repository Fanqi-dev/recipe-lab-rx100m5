$ErrorActionPreference='Stop'
$project=Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$runtime=Join-Path $project 'out-safe/live-preview'
$info=Join-Path $runtime 'server-info.json'
New-Item -ItemType Directory -Force $runtime | Out-Null
if(Test-Path -LiteralPath $info){
    $previous=Get-Content -LiteralPath $info -Raw | ConvertFrom-Json
    if($previous.url -match '^http://127\.0\.0\.1:\d+/$'){
        try{
            $status=Invoke-RestMethod ($previous.url+'status') -TimeoutSec 2
            if($null -ne $status.revision){Write-Output $previous.url;return}
        }catch{}
    }
}
$env:PREVIEW_POWERSHELL=(Get-Command pwsh).Source
$node=(Get-Command node).Source
$server=Join-Path $PSScriptRoot 'server.mjs'
$launched=Start-Process -FilePath $node -ArgumentList @(('"'+$server+'"')) -WorkingDirectory $project -WindowStyle Hidden -RedirectStandardOutput (Join-Path $runtime 'server.log') -RedirectStandardError (Join-Path $runtime 'server-error.log') -PassThru
for($attempt=0;$attempt -lt 20;$attempt++){
    if(Test-Path -LiteralPath $info){$current=Get-Content -LiteralPath $info -Raw | ConvertFrom-Json;if($current.pid -eq $launched.Id){Write-Output $current.url;return}}
    Start-Sleep -Milliseconds 150
}
throw 'Preview startup did not produce its local URL; inspect out-safe/live-preview/server-error.log'
