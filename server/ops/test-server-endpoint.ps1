$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'server-endpoint.ps1')
$lan='192.168.1.25'
$listeners=@(
    [pscustomobject]@{LocalAddress='fd7a::1';LocalPort=25576;OwningProcess=11},
    [pscustomobject]@{LocalAddress='100.69.81.40';LocalPort=25576;OwningProcess=11},
    [pscustomobject]@{LocalAddress=$lan;LocalPort=25576;OwningProcess=22}
)
$selected=@(Select-MythforgeEndpointListeners $listeners $lan 25576)
if($selected.Count -ne 1 -or $selected[0].OwningProcess -ne 22){throw 'Selected private relay rather than Minecraft'}
if(@(Select-MythforgeEndpointListeners $listeners[0..1] $lan 25576).Count){throw 'Private relay blocks a stopped-server launch'}
$wildcard=@([pscustomobject]@{LocalAddress='0.0.0.0';LocalPort=25576;OwningProcess=33})
if(@(Select-MythforgeEndpointListeners $wildcard $lan 25576).Count -ne 1){throw 'Wildcard conflict was not detected'}
if(@(Select-MythforgeEndpointListeners $listeners '' 25576).Count -ne 3){throw 'Wildcard server bind ignored a real conflict'}
if(@(Select-MythforgeEndpointListeners $listeners $lan 25565).Count){throw 'Unrelated port selected'}
'Endpoint regression checks: 5 passed.'
