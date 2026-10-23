"""Run the real oasis mixin in a disposable, loopback-only NeoForge world.

Build server-tools' oasisIntegrationJar first. No production configuration is copied.
"""
from pathlib import Path
from datetime import datetime, timezone
import json
import shutil
import subprocess


def main():
    root = Path(__file__).resolve().parents[1]
    stamp = datetime.now(timezone.utc).strftime('%Y%m%d-%H%M%S')
    runtime = root / 'ops' / 'audit' / f'oasis-regression-{stamp}'
    runtime.mkdir(parents=True, exist_ok=False)
    (runtime / 'mods').mkdir()
    for source in [root / 'mods/Galosphere-1.21.1-1.5.5-NeoForge.jar',
                   root / 'server-tools/build/libs/mythforge-oasis-integration-test.jar']:
        shutil.copy2(source, runtime / 'mods' / source.name)
    # Reuse immutable installed libraries, not production mods, configs or worlds.
    link = str(runtime / 'libraries').replace("'", "''")
    target = str(root / 'libraries').replace("'", "''")
    subprocess.run(['powershell', '-NoProfile', '-Command',
                    f"New-Item -ItemType Junction -Path '{link}' -Target '{target}' | Out-Null"], check=True)
    shutil.copy2(root / 'eula.txt', runtime / 'eula.txt')
    (runtime / 'server.properties').write_text(
        'server-ip=127.0.0.1\nserver-port=25589\nenable-rcon=false\nonline-mode=true\n'
        'level-name=oasis-regression\nlevel-type=mythforge_oasis_test:oasis\n'
        'level-seed=104729\nview-distance=2\nsimulation-distance=2\n'
        'max-tick-time=120000\nspawn-protection=0\n', encoding='utf-8')
    java = shutil.which('java')
    if not java:
        raise RuntimeError('Java 21 is required')
    with (runtime / 'console.log').open('w', encoding='utf-8') as log:
        child = subprocess.Popen([java, '-Xms1G', '-Xmx4G', '-Dmixin.debug.export=true',
            '@libraries/net/neoforged/neoforge/21.1.263/win_args.txt', 'nogui'],
            cwd=runtime, stdin=subprocess.DEVNULL, stdout=log, stderr=subprocess.STDOUT)
        try:
            return_code = child.wait(timeout=180)
        except subprocess.TimeoutExpired:
            child.kill()  # Only this disposable test process.
            child.wait()
            raise RuntimeError(f'Isolated test timed out; inspect {runtime / "console.log"}')
    result = json.loads((runtime / 'oasis-test-result.json').read_text(encoding='utf-8'))
    console = (runtime / 'console.log').read_text(encoding='utf-8', errors='replace')
    if return_code or not result['passed'] or 'All dimensions are saved' not in console:
        raise RuntimeError(f'Regression or clean shutdown failed: {runtime}')
    print(json.dumps(dict(result, runtime=str(runtime)), indent=2))


if __name__ == '__main__':
    main()
