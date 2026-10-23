param([switch]$Rollback)
$ErrorActionPreference='Stop'
$root='C:\Mythforge Server'
$audit=Join-Path $root 'ops\audit\firewall-20261023'
$result=Join-Path $audit 'result.json'
$prefix='Mythforge-Restricted-'
$disabledFile=Join-Path $audit 'disabled-rules.json'
$mutating=$false
$newRules=@('Game-Allow','Game-Block-Other-Sources','Rcon-Allow-LAN','Rcon-Block-Public','UDP-Block')
function Undo-Changes {
    foreach($suffix in $newRules){Get-NetFirewallRule -Name ($prefix+$suffix) -ErrorAction SilentlyContinue | Remove-NetFirewallRule}
    if(Test-Path -LiteralPath $disabledFile){foreach($name in @(Get-Content -LiteralPath $disabledFile -Raw | ConvertFrom-Json)){Enable-NetFirewallRule -Name $name | Out-Null}}
}
try {
    if($Rollback){Undo-Changes; @{success=$true;rolledBack=$true}|ConvertTo-Json|Set-Content -LiteralPath $result;exit}
    if(Get-NetFirewallRule -Name ($prefix+'*') -ErrorAction SilentlyContinue){throw 'Restricted rules already exist; inspect rather than overwrite them'}
    if(-not (Test-Path -LiteralPath (Join-Path $audit 'before.wfw'))){
        & netsh advfirewall export (Join-Path $audit 'before.wfw') | Out-File (Join-Path $audit 'backup.log')
        if($LASTEXITCODE -ne 0){throw 'Firewall export failed'}
    }
    $scopes=Get-Content -LiteralPath (Join-Path $audit 'scopes.json') -Raw | ConvertFrom-Json
    $listener=Get-NetTCPConnection -State Listen -LocalPort 25565
    if(@($listener).Count -ne 1 -or $listener.LocalAddress -ne '192.168.1.25'){throw 'Unexpected Minecraft listener'}
    $java=(Get-Process -Id $listener.OwningProcess).Path
    if(-not $java -or [IO.Path]::GetFileName($java) -ne 'java.exe'){throw 'Minecraft is not owned by the expected Java process'}
    $disabled=@()
    Get-NetFirewallRule -Enabled True -Direction Inbound -Action Allow | ForEach-Object {
        $rule=$_
        $app=$rule|Get-NetFirewallApplicationFilter
        $port=$rule|Get-NetFirewallPortFilter
        # Replace Java's broad automatic exceptions and the old open game rule.
        if(($app.Program -match '(?i)\\javaw?\.exe$' -and @($port.LocalPort) -contains 'Any') -or $rule.DisplayName -eq 'Minecraft Server 25565 TCP'){
            $disabled += $rule.Name
        }
    }
    ConvertTo-Json -InputObject @($disabled) | Set-Content -LiteralPath $disabledFile
    $mutating=$true
    New-NetFirewallRule -Name ($prefix+'Game-Allow') -DisplayName 'Mythforge Minecraft - TCPShield only' -Direction Inbound -Action Allow -Profile Any -Protocol TCP -LocalPort 25565 -Program $java -RemoteAddress $scopes.shield | Out-Null
    # Explicit source blocks prevent other broad app rules bypassing this scope.
    New-NetFirewallRule -Name ($prefix+'Game-Block-Other-Sources') -DisplayName 'Mythforge Minecraft - block direct connections' -Direction Inbound -Action Block -Profile Any -Protocol TCP -LocalPort 25565 -RemoteAddress $scopes.gameBlocked | Out-Null
    New-NetFirewallRule -Name ($prefix+'Rcon-Allow-LAN') -DisplayName 'Mythforge RCON - home and private Tailscale devices' -Direction Inbound -Action Allow -Profile Any -Protocol TCP -LocalPort 25576 -Program $java -RemoteAddress $scopes.rconAllowed | Out-Null
    New-NetFirewallRule -Name ($prefix+'Rcon-Block-Public') -DisplayName 'Mythforge RCON - block public connections' -Direction Inbound -Action Block -Profile Any -Protocol TCP -LocalPort 25576 -RemoteAddress $scopes.rconBlocked | Out-Null
    New-NetFirewallRule -Name ($prefix+'UDP-Block') -DisplayName 'Mythforge services - no UDP' -Direction Inbound -Action Block -Profile Any -Protocol UDP -LocalPort 25565,25576,8193 | Out-Null
    foreach($name in $disabled){Disable-NetFirewallRule -Name $name | Out-Null}
    $rules=Get-NetFirewallRule -Name ($prefix+'*') | ForEach-Object {
        $port=$_|Get-NetFirewallPortFilter;$addr=$_|Get-NetFirewallAddressFilter;$app=$_|Get-NetFirewallApplicationFilter
        [pscustomobject]@{Name=$_.Name;Enabled=[string]$_.Enabled;Action=[string]$_.Action;LocalPort=$port.LocalPort;Protocol=$port.Protocol;RemoteAddress=$addr.RemoteAddress;Program=$app.Program}
    }
    @{success=$true;java=$java;disabledRules=$disabled;rules=$rules;voting='Unchanged: loopback only; external voting setup pending';tailscale='Use Serve TCP proxy to existing LAN RCON listener'} | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $result
} catch {
    $failure=$_.Exception.Message+' at line '+$_.InvocationInfo.ScriptLineNumber+': '+$_.InvocationInfo.Line
    if($mutating){try{Undo-Changes}catch{ $failure+='; rollback: '+$_.Exception.Message }}
    @{success=$false;error=$failure}|ConvertTo-Json|Set-Content -LiteralPath $result
    exit 1
}
