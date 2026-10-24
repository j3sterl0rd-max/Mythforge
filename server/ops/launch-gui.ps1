$ErrorActionPreference='Stop'
$root='C:\Mythforge Server'
. (Join-Path $PSScriptRoot 'server-endpoint.ps1')
$log=Join-Path $root 'ops\gui-launch.log'
function Log([string]$message){Add-Content -LiteralPath $log -Value ((Get-Date -Format o)+' '+$message)}
$mutex=[Threading.Mutex]::new($false,'Local\MythforgeGuiLaunch')
if(-not $mutex.WaitOne(0)){return}
try {
    $props=Get-MythforgeProperties $root
    foreach($port in @([int]$props['server-port'],[int]$props['rcon.port'])){
        if(@(Get-MythforgeEndpointListeners $props['server-ip'] $port).Count){Log "Launch skipped: configured endpoint on port $port is already in use";return}
    }
    $world=[IO.Path]::GetFullPath((Join-Path $root $props['level-name']))
    if(-not $world.StartsWith($root+'\',[StringComparison]::OrdinalIgnoreCase)){throw 'Unexpected world directory'}
    $lock=Join-Path $world 'session.lock'
    if(Test-Path -LiteralPath $lock){$handle=[IO.File]::Open($lock,'Open','ReadWrite','None');$handle.Dispose()}
    # A separate interactive task owns this window, not the timed maintenance task.
    Start-Process -FilePath $env:ComSpec -ArgumentList '/c','""C:\Mythforge Server\run.bat""' -WorkingDirectory $root -WindowStyle Hidden | Out-Null
    Log 'Launched run.bat with Minecraft GUI enabled in the logged-in desktop session'
    Start-Sleep -Seconds 15
} catch {Log ('LAUNCH FAILED: '+$_.Exception.Message);throw}
finally {$mutex.ReleaseMutex();$mutex.Dispose()}
