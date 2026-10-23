"""Apply the prepared beta roster JAR at the next normal, stopped-server launch."""
from pathlib import Path
import datetime
import hashlib
import json
import shutil
import socket
import sys

root = Path(__file__).resolve().parent.parent
marker = root / 'ops/updates/beta-roster-pending.json'
if not marker.exists():
    sys.exit(0)
pending = json.loads(marker.read_text())
source = root / 'ops/updates/mythforge-server-tools-1.0.9.jar'
if hashlib.sha256(source.read_bytes()).hexdigest() != pending['sha256']:
    sys.exit('Prepared beta roster update hash does not match; launch cancelled.')
props = dict(line.split('=', 1) for line in (root / 'server.properties').read_text(encoding='utf-8-sig').splitlines() if '=' in line and not line.startswith('#'))
for key in ('server-port', 'rcon.port'):
    with socket.socket() as probe:
        probe.setsockopt(socket.SOL_SOCKET, socket.SO_EXCLUSIVEADDRUSE, 1)
        try:
            probe.bind((props.get('server-ip', '') or '0.0.0.0', int(props[key])))
        except OSError:
            sys.exit('Server is running; the beta roster update remains pending until the next normal launch.')
backup = root / 'ops/updates/before-beta-roster' / datetime.datetime.now().strftime('%Y%m%d-%H%M%S-%f')
backup.mkdir(parents=True)
target = root / 'mods' / source.name
old_files = [path for path in (root / 'mods').glob('mythforge-server-tools-*.jar') if path != target]
shutil.copy2(source, target)
try:
    for old in old_files:
        shutil.move(str(old), str(backup / old.name))
except Exception:
    target.unlink(missing_ok=True)
    for saved in backup.glob('mythforge-server-tools-*.jar'):
        shutil.move(str(saved), str(root / 'mods' / saved.name))
    raise
marker.rename(backup / 'applied-beta-roster.json')
print('Applied the prepared beta roster update. Starting the server normally.')
