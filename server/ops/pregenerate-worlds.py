"""Supervise installed Chunky, pausing for players, slow ticks or low disk space.

Run --step for one check, otherwise run until complete. Create
ops/pregeneration-stop.txt to pause generation and stop this supervisor.
Chunky's own progress files allow resuming interrupted generation.
"""
from pathlib import Path
import importlib.util
import json
import re
import shutil
import socket
import sys
import time
import msvcrt
from datetime import datetime, timezone

ROOT = Path(__file__).resolve().parent.parent
STATE = ROOT / 'ops/pregeneration-5000.json'
STOP = ROOT / 'ops/pregeneration-stop.txt'
WORLDS = ['minecraft:overworld', 'minecraft:the_nether', 'minecraft:the_end']
spec = importlib.util.spec_from_file_location('maintenance', ROOT / 'ops/maintenance-rcon.py')
rcon = importlib.util.module_from_spec(spec)
spec.loader.exec_module(rcon)


def console(command):
    props = dict(line.split('=', 1) for line in (ROOT / 'server.properties').read_text(encoding='utf-8-sig').splitlines()
                 if '=' in line and not line.startswith('#'))
    host = props.get('server-ip', '').strip() or '127.0.0.1'
    with socket.create_connection((host, int(props['rcon.port'])), timeout=90) as stream:
        rcon.request(stream, 1, 3, props['rcon.password'])
        return rcon.request(stream, 2, 2, command).strip()


def save(state):
    state['updatedUtc'] = datetime.now(timezone.utc).isoformat()
    temporary = STATE.with_suffix('.tmp')
    temporary.write_text(json.dumps(state, indent=2))
    temporary.replace(STATE)


def step(state):
    free = shutil.disk_usage(ROOT).free
    state['freeGiB'] = round(free / 1024**3, 2)
    players = console('list')
    match = re.search(r'There are (\d+) of', players)
    if not match:
        raise RuntimeError('Could not read online player count')
    state['players'] = int(match[1])
    progress = console('chunky progress')
    state['progress'] = progress
    running = 'No tasks running.' not in progress
    if running and 'Task running for ' not in progress:
        raise RuntimeError('Unrecognized Chunky progress response: ' + progress)
    if running:
        count_match = re.search(r'Processed: ([\d,]+) chunks', progress)
        if count_match:
            count = int(count_match[1].replace(',', ''))
            if count != state.get('lastChunkCount'):
                state['lastChunkCount'] = count
                state['lastProgressTime'] = time.time()
            elif time.time() - state.get('lastProgressTime', time.time()) > 300:
                console('chunky pause')
                state['paused'] = True
                state['status'] = 'stopped: no progress for five minutes; inspect generation'
                save(state)
                return False
    if STOP.exists() or free < 60 * 1024**3:
        if running:
            console('chunky pause')
        state['status'] = 'stopped by request' if STOP.exists() else 'stopped: under 60 GiB free'
        state['paused'] = True
        save(state)
        return False
    if state['players']:
        if running:
            console('chunky pause')
            state['paused'] = True
        state['status'] = 'paused for players'
        save(state)
        return True
    if running:
        ticks = console('tick query')
        timing = re.search(r'Average time per tick: ([\d.]+)ms', ticks)
        if timing:
            state['averageTickMs'] = float(timing[1])
            if float(timing[1]) > 45:
                console('chunky pause')
                state['paused'] = True
                state['resumeAfter'] = time.time() + 60
                state['status'] = 'cooling down after slow ticks'
                save(state)
                return True
        state['status'] = 'generating'
        save(state)
        return True
    if time.time() < state.get('resumeAfter', 0):
        save(state)
        return True
    if state.get('paused') and state.get('active'):
        reply = console('chunky continue ' + state['active'])
        if 'Task continuing' not in reply:
            raise RuntimeError('Chunky did not resume: ' + reply)
        state['paused'] = False
        state['lastProgressTime'] = time.time()
        state['status'] = 'generating'
        save(state)
        return True
    if state.get('active'):
        # A paused/cancelled task must never be mistaken for completion.
        name = state['active'].split(':')[1]
        task = ROOT / f'config/chunky/tasks/minecraft/{name}.properties'
        properties = dict(line.split('=', 1) for line in task.read_text().splitlines() if '=' in line) if task.exists() else {}
        # Chunky marks completed tasks "cancelled=true" too. Require its
        # explicit completion message and the matching saved selection.
        with (ROOT / 'logs/latest.log').open('rb') as log:
            log.seek(max(0, log.seek(0, 2) - 2 * 1024**2))
            tail = log.read().decode(errors='replace')
        finished = ('Task finished for ' + state['active'] + '.') in tail
        matching = float(properties.get('radius', 0)) == 5000 and int(properties.get('chunks', 0)) > 300000
        if not (finished and matching):
            state['status'] = 'stopped: task interrupted externally; review before resuming'
            save(state)
            return False
        state['completed'].append(state['active'])
        state['active'] = None
    pending = [world for world in WORLDS if world not in state['completed']]
    if not pending:
        state['status'] = 'complete'
        save(state)
        return False
    world = pending[0]
    for command in ['chunky world ' + world, 'chunky center 0 0', 'chunky shape circle',
                    'chunky radius 5000', 'chunky pattern region', 'chunky quiet 60']:
        console(command)
    state['active'] = world
    state['paused'] = False
    state['lastChunkCount'] = -1
    state['lastProgressTime'] = time.time()
    state['status'] = 'starting'
    save(state)
    reply = console('chunky start')
    if 'Task started' not in reply:
        raise RuntimeError('Chunky did not start: ' + reply)
    state['status'] = 'generating'
    save(state)
    return True


if __name__ == '__main__':
    lock = (ROOT / 'ops/pregeneration-supervisor.lock').open('a+b')
    lock.seek(0)
    if not lock.read(1):
        lock.write(b'0')
        lock.flush()
    lock.seek(0)
    msvcrt.locking(lock.fileno(), msvcrt.LK_NBLCK, 1)
    state = json.loads(STATE.read_text()) if STATE.exists() else {
        'radius': 5000, 'shape': 'circle', 'center': [0, 0], 'completed': [], 'active': None,
        'paused': False, 'status': 'pending'}
    try:
        disconnected_since = None
        while True:
            try:
                keep_running = step(state)
                disconnected_since = None
            except (ConnectionError, TimeoutError, EOFError, OSError) as error:
                # Normal server restarts preserve Chunky's checkpoint. Wait for
                # the same backend, then continue only our active dimension.
                disconnected_since = disconnected_since or time.time()
                state['status'] = 'waiting for server: ' + type(error).__name__
                state['paused'] = bool(state.get('active'))
                save(state)
                if time.time() - disconnected_since > 600:
                    raise
                keep_running = True
            if not keep_running or '--step' in sys.argv:
                break
            time.sleep(10)
    except Exception as error:
        # Fail closed: leave Chunky paused if supervision stops unexpectedly.
        try:
            console('chunky pause')
        except Exception:
            pass
        state['status'] = 'error: ' + str(error)
        state['paused'] = True
        save(state)
        raise
