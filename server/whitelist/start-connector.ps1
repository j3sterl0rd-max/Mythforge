param([switch]$Check)
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
$node='C:\Program Files\nodejs\node.exe'
if(!(Test-Path -LiteralPath $node)){throw 'Whitelist connector Node runtime is missing'}
if($Check){& $node (Join-Path $PSScriptRoot 'connector.mjs') --check-config;exit $LASTEXITCODE}
$mutex=[Threading.Mutex]::new($false,'Local\MythforgeWhitelistConnector')
if(!$mutex.WaitOne(0)){throw 'Whitelist connector is already running'}
try {
    foreach($name in @('connector.log','connector.err.log')){
        $path=Join-Path $PSScriptRoot $name
        if((Test-Path -LiteralPath $path) -and (Get-Item -LiteralPath $path).Length -gt 2MB){Move-Item -LiteralPath $path -Destination ($path+'.previous') -Force}
    }
    # Redirect at process creation, not through PowerShell's NativeCommandError pipeline.
    $child=Start-Process -FilePath $node -ArgumentList ('"'+(Join-Path $PSScriptRoot 'connector.mjs')+'"') -WorkingDirectory $PSScriptRoot -WindowStyle Hidden -RedirectStandardOutput (Join-Path $PSScriptRoot 'connector.log') -RedirectStandardError (Join-Path $PSScriptRoot 'connector.err.log') -PassThru
    $null=$child.Handle
    $child.WaitForExit()
    exit $child.ExitCode
} finally {$mutex.ReleaseMutex();$mutex.Dispose()}
