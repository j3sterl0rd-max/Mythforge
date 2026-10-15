$ErrorActionPreference = 'Stop'
$serverRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
if ($serverRoot -ne 'C:\Mythforge Server') { throw 'Unexpected server directory.' }
Set-Location -LiteralPath $serverRoot
$properties = Get-Content -LiteralPath (Join-Path $serverRoot 'server.properties')
if ($properties -notcontains 'server-ip=127.0.0.1' -or $properties -notcontains 'server-port=25566') {
    throw 'Staging must bind 127.0.0.1:25566. Review server.properties before starting.'
}
& java '@ops/isolated_jvm_args.txt' '@libraries/net/neoforged/neoforge/21.1.253/win_args.txt' nogui
exit $LASTEXITCODE
