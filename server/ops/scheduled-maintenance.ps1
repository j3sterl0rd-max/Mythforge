param([ValidateSet('Warn','Restart')][string]$Mode,[int]$Minutes=0,[switch]$DryRun)
$ErrorActionPreference='Stop'
$root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
if($root -ne 'C:\Mythforge Server'){throw 'Unexpected server root'}
. (Join-Path $PSScriptRoot 'server-endpoint.ps1')
$properties=Get-MythforgeProperties $root
if($Mode -eq 'Warn' -and $Minutes -notin @(30,15,5)){throw 'Warnings must be 30, 15 or 5 minutes'}
if($DryRun){Write-Output "$Mode plan: 06:00/18:00 Chicago; warning=$Minutes; skip stopped servers; graceful save/stop; await exit; Simple Backups on E:; move existing archives to E:; launch Minecraft GUI and verify readiness.";return}
$log=Join-Path $root 'ops\maintenance.log'
function Log([string]$message){$writer=[Threading.Mutex]::new($false,'Local\MythforgeMaintenanceLog');try{if(-not $writer.WaitOne(30000)){throw 'Maintenance log is busy'};try{if((Test-Path -LiteralPath $log) -and (Get-Item -LiteralPath $log).Length -gt 1MB){Move-Item -LiteralPath $log -Destination ($log+'.previous') -Force};Add-Content -LiteralPath $log -Value ((Get-Date -Format o)+' '+$message)}finally{$writer.ReleaseMutex()}}finally{$writer.Dispose()}}
$serverProcess=Get-MythforgeServerProcess $properties
if($null -eq $serverProcess){Log "$Mode skipped: server is stopped";return}
$serverPid=$serverProcess.Id
# Acquire a handle before stop so a quick exit remains observable.
$null=$serverProcess.Handle
if($serverProcess.ProcessName -notin @('java','javaw')){throw 'RCON port is not owned by Java'}
function Rcon([string]$command){
    $encoded=[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($command))
    & 'C:\Users\Dad\AppData\Local\Programs\Python\Python311\python.exe' (Join-Path $PSScriptRoot 'maintenance-rcon.py') $encoded
    if($LASTEXITCODE -ne 0){throw 'Server RCON command failed'}
}
if($Mode -eq 'Warn'){
    $message=@{text="[Mythforge] Scheduled restart in $Minutes minutes. Please prepare to log out. The server will return automatically.";color='gold'} | ConvertTo-Json -Compress
    Rcon ('tellraw @a '+$message)
    Log "Sent $Minutes-minute restart warning"
    return
}
$mutex=[Threading.Mutex]::new($false,'Local\MythforgeScheduledRestart')
if(-not $mutex.WaitOne(0)){throw 'Another scheduled restart is already running'}
try {
    Log "Scheduled restart beginning; server PID $serverPid"
    Rcon 'tellraw @a {"text":"[Mythforge] Restarting now. Simple Backups is configured for E:. Please reconnect shortly.","color":"gold"}'
    # stop performs the normal world save; avoid a second forced flush immediately before it.
    try { Rcon 'stop' } catch { Log ('Stop reply failed; checking original Java process: '+$_.Exception.Message) }
    if(-not $serverProcess.WaitForExit(900000)){throw 'Server has not exited after 15 minutes; leaving it alone, not forcing shutdown'}
    Log 'Original Java process exited; Simple Backups handles world backups; separate settings backups remain paused'
    $remainingArchives=@(Get-ChildItem -LiteralPath (Join-Path $root 'backups') -Recurse -File -Force -ErrorAction SilentlyContinue | Select-Object -First 1)
    if($remainingArchives.Count){
        try { & (Join-Path $PSScriptRoot 'migrate-backups-to-e.ps1') } catch { Log ('Archive relocation failed; preserving remaining sources and continuing restart: '+$_.Exception.Message) }
    } else { Log 'Archive relocation already complete; no C: archives to move' }
    Start-ScheduledTask -TaskName 'Mythforge-Launch-GUI'
    Log 'Requested independent interactive GUI launch task'
    $ready=$false
    $deadline=(Get-Date).AddMinutes(10)
    do {
        Start-Sleep -Seconds 5
        try {
            $newProcess=Get-MythforgeServerProcess $properties
            if($null -ne $newProcess -and $newProcess.Id -ne $serverPid){
                $reply=Rcon 'list'
                if(($reply -join ' ') -match 'There are .*players online'){$ready=$true;break}
            }
        } catch { }
    } while((Get-Date) -lt $deadline)
    if(-not $ready){throw 'GUI launch requested but server did not answer RCON list within 10 minutes; see gui-launch.log and latest.log'}
    Log ('Restart verified: server is responding on RCON; PID '+$newProcess.Id)
} catch {
    Log ('RESTART FAILED: '+$_.Exception.Message)
    throw

} finally {$mutex.ReleaseMutex();$mutex.Dispose()}
