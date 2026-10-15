param([string]$BindAddress = '', [int]$Port = 25565)
$ErrorActionPreference = 'Stop'
$serverRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
if ($serverRoot -ne 'C:\Mythforge Server') { throw 'Unexpected server directory.' }
if ($Port -lt 1 -or $Port -gt 65535) { throw 'Invalid Minecraft port.' }
$parsedBind=[Net.IPAddress]::Any
if ($BindAddress -and ![Net.IPAddress]::TryParse($BindAddress,[ref]$parsedBind)) { throw 'Use an IP address or blank for the bind interface.' }
$listeners = [Net.NetworkInformation.IPGlobalProperties]::GetIPGlobalProperties().GetActiveTcpListeners()
if ($listeners | Where-Object Port -eq $Port) { throw "Port $Port is occupied. Stop the original server normally before cutover." }
if ($listeners | Where-Object Port -eq 25566) { throw 'This copy is already running. Stop it normally before production cutover.' }
$lockPath=Join-Path $serverRoot 'world/session.lock'
if (Test-Path -LiteralPath $lockPath) {
    $lock=[IO.File]::Open($lockPath,[IO.FileMode]::Open,[IO.FileAccess]::ReadWrite,[IO.FileShare]::None)
    $lock.Dispose()
}
Set-Location -LiteralPath $serverRoot
$propertiesPath=Join-Path $serverRoot 'server.properties'
$properties=Get-Content -LiteralPath $propertiesPath -Raw
if ($properties -notmatch '(?m)^online-mode=true\r?$' -or $properties -notmatch '(?m)^enable-rcon=false\r?$') { throw 'Online authentication must remain enabled and RCON disabled.' }
Copy-Item -LiteralPath $propertiesPath -Destination (Join-Path $serverRoot 'ops/private/server.properties.before-cutover') -Force
$properties=$properties -replace '(?m)^server-ip=.*$',("server-ip="+$BindAddress)
$properties=$properties -replace '(?m)^server-port=.*$',("server-port="+$Port)
Set-Content -LiteralPath $propertiesPath -Value $properties -Encoding utf8
Write-Output "Starting production copy on $BindAddress`:$Port. Whitelist remains enabled."
& java '@user_jvm_args.txt' '@libraries/net/neoforged/neoforge/21.1.253/win_args.txt' nogui
exit $LASTEXITCODE
