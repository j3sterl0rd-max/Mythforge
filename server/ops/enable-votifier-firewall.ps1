$ErrorActionPreference='Stop'
$root='C:\Mythforge Server'
$result=Join-Path $root 'ops\private\voting-setup\firewall-result.json'
try {
    $previous=Get-Content -LiteralPath (Join-Path $root 'ops\audit\firewall-20261023\result.json') -Raw | ConvertFrom-Json
    $java=$previous.java
    if(-not (Test-Path -LiteralPath $java) -or [IO.Path]::GetFileName($java) -ne 'java.exe'){throw 'Expected server Java executable is missing'}
    $name='Mythforge-Restricted-Votifier-Allow'
    $existing=Get-NetFirewallRule -Name $name -ErrorAction SilentlyContinue
    if($existing){
        $port=$existing | Get-NetFirewallPortFilter
        $app=$existing | Get-NetFirewallApplicationFilter
        if($port.LocalPort -ne '8193' -or $app.Program -ne $java){throw 'Existing voting rule differs; inspect before changing'}
        Enable-NetFirewallRule -Name $name | Out-Null
    } else {
        New-NetFirewallRule -Name $name -DisplayName 'Mythforge Votifier - Java TCP 8193' -Direction Inbound -Action Allow -Profile Any -Protocol TCP -LocalAddress 192.168.1.25 -LocalPort 8193 -Program $java -RemoteAddress Any | Out-Null
    }
    $rule=Get-NetFirewallRule -Name $name
    $port=$rule | Get-NetFirewallPortFilter
    $app=$rule | Get-NetFirewallApplicationFilter
    @{success=$true;enabled=[string]$rule.Enabled;port=$port.LocalPort;program=$app.Program;checkedAt=(Get-Date -Format o)} | ConvertTo-Json | Set-Content -LiteralPath $result
} catch {
    @{success=$false;error=$_.Exception.Message} | ConvertTo-Json | Set-Content -LiteralPath $result
    exit 1
}
