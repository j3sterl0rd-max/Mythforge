"""Prepare local firewall scopes. Pass the server/phone IPs from tailscale status.

Example: python ops/build-firewall-scopes.py SERVER_V4 PHONE_V4 SERVER_V6 PHONE_V6
The output contains private device addresses and stays in the local audit folder.
"""
from pathlib import Path
import ipaddress
import json
import sys

ROOT = Path(__file__).resolve().parent.parent
SHIELD = ['198.178.119.0/24', '104.234.6.0/25']


def blocked_unicast(allowed):
    networks = [ipaddress.ip_network('0.0.0.0/0')]
    # Windows rejects some unspecified/reserved address prefixes. They cannot
    # establish ordinary remote unicast TCP sessions; cover usable IPv4 space.
    for text in allowed + ['0.0.0.0/8', '224.0.0.0/3']:
        cut = ipaddress.ip_network(text)
        networks = [part for net in networks
                    for part in (net.address_exclude(cut) if cut.subnet_of(net) else [net])]
    return [str(net) for net in networks]


def scopes(addresses):
    devices = [ipaddress.ip_address(address) for address in addresses]
    if not devices or any(not (device in ipaddress.ip_network('100.64.0.0/10') if device.version == 4
                               else device in ipaddress.ip_network('fd7a:115c:a1e0::/48')) for device in devices):
        raise ValueError('Provide only the actual Tailscale addresses of the server and authorized phone')
    allowed = ['192.168.1.0/24', '127.0.0.0/8'] + [str(device) + ('/32' if device.version == 4 else '/128') for device in devices]
    return {
        'shield': SHIELD,
        'gameBlocked': blocked_unicast(SHIELD) + ['2000::/3', 'fc00::/7', 'fe80::/10'],
        'rconAllowed': allowed,
        'rconBlocked': blocked_unicast([value for value in allowed if ':' not in value]) + ['2000::/3'],
    }


if __name__ == '__main__':
    data = scopes(sys.argv[1:])
    directory = ROOT / 'ops/audit/firewall-20261023'
    directory.mkdir(parents=True, exist_ok=True)
    (directory / 'scopes.json').write_text(json.dumps(data, indent=2), encoding='utf-8')
    print('Prepared private local firewall scopes; review before applying.')
