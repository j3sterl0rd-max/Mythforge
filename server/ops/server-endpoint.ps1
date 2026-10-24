# Shared by maintenance and launch checks. A private forwarding listener is not Minecraft.
function Get-MythforgeProperties([string]$Root) {
    $values=@{}
    Get-Content -LiteralPath (Join-Path $Root 'server.properties') | ForEach-Object {
        if($_ -match '^([^#=]+)=(.*)$'){$values[$matches[1]]=$matches[2].Trim()}
    }
    return $values
}
function Select-MythforgeEndpointListeners($Listeners,[string]$Address,[int]$Port) {
    @($Listeners | Where-Object {
        $_.LocalPort -eq $Port -and (
            [string]::IsNullOrWhiteSpace($Address) -or $Address -in @('0.0.0.0','::') -or
            $_.LocalAddress -eq $Address -or $_.LocalAddress -in @('0.0.0.0','::')
        )
    })
}
function Get-MythforgeEndpointListeners([string]$Address,[int]$Port) {
    # Query failure must not be mistaken for a stopped server.
    $all=@(Get-NetTCPConnection -State Listen -ErrorAction Stop)
    Select-MythforgeEndpointListeners $all $Address $Port
}
function Get-MythforgeServerProcess($Properties) {
    $listeners=@(Get-MythforgeEndpointListeners $Properties['server-ip'] ([int]$Properties['rcon.port']))
    if(!$listeners.Count){return $null}
    $ids=@($listeners | Select-Object -ExpandProperty OwningProcess -Unique)
    if($ids.Count -ne 1){throw 'Ambiguous process ownership at the configured RCON endpoint'}
    $process=Get-Process -Id $ids[0] -ErrorAction Stop
    if($process.ProcessName -notin @('java','javaw')){throw 'Configured RCON endpoint is not owned by Java'}
    $game=@(Get-MythforgeEndpointListeners $Properties['server-ip'] ([int]$Properties['server-port']))
    if(!$game.Count -or @($game | Where-Object OwningProcess -ne $process.Id).Count){throw 'Minecraft and RCON endpoint ownership do not agree'}
    return $process
}
