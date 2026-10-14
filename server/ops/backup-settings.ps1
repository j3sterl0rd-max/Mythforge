$ErrorActionPreference = 'Stop'
$serverRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
if ($serverRoot -ne 'C:\Mythforge Server') { throw 'Unexpected server directory.' }
$sessionPath = Join-Path $serverRoot 'world\session.lock'
if (Test-Path -LiteralPath $sessionPath) {
    $handle = [IO.File]::Open($sessionPath, 'Open', 'ReadWrite', 'None')
    $handle.Dispose()
}
$target = Join-Path $serverRoot ('backups\private-settings\settings-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '.zip')
New-Item -ItemType Directory -Path ([IO.Path]::GetDirectoryName($target)) -Force | Out-Null
$paths = @('config','defaultconfigs','kubejs','datapacks','mods\tebex','server.properties','user_jvm_args.txt','ops.json','whitelist.json','banned-players.json','banned-ips.json') |
    ForEach-Object { Join-Path $serverRoot $_ } | Where-Object { Test-Path -LiteralPath $_ }
Compress-Archive -LiteralPath $paths -DestinationPath $target
Write-Host ('Private settings archive created: ' + $target + '. Contains secrets; keep private and copy off-host.')
