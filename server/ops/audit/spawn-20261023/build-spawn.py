"""Resumable, dimension-explicit spawn construction through the server console."""
from pathlib import Path
import hashlib
import importlib.util
import json
import shutil
import socket
import sys
import time

ROOT=Path(__file__).resolve().parents[3]
HERE=Path(__file__).resolve().parent
WORLD=ROOT/'Mythforge Official Server'
cfg=json.loads((ROOT/'config/mythforge-spawn.json').read_text())
assert cfg['dimension']=='minecraft:overworld'
assert (cfg['minX'],cfg['maxX'],cfg['minZ'],cfg['maxZ'],cfg['minY'],cfg['maxY'])==(-100,100,-100,100,-64,319)
spec=importlib.util.spec_from_file_location('maintenance',ROOT/'ops/maintenance-rcon.py')
rcon=importlib.util.module_from_spec(spec);spec.loader.exec_module(rcon)
props=dict(line.split('=',1) for line in (ROOT/'server.properties').read_text(encoding='utf-8-sig').splitlines() if '=' in line and not line.startswith('#'))
stream=socket.create_connection((props['server-ip'],int(props['rcon.port'])),timeout=180)
rcon.request(stream,1,3,props['rcon.password']); sequence=2
def command(text):
    global sequence
    result=rcon.request(stream,sequence,2,text);sequence+=1
    return result.strip()
def in_world(text):return command('execute in minecraft:overworld run '+text)

if '--backup' in sys.argv:
    destination=HERE/'before'
    if (destination/'manifest.json').exists():
        print('Existing pre-construction backup retained.');sys.exit(0)
    destination.mkdir(exist_ok=True)
    print(command('save-off'),flush=True)
    try:
        print(command('save-all flush'),flush=True)
        entries=[]
        paths=[WORLD/'level.dat',WORLD/'level.dat_old',WORLD/'data/chunks.dat',ROOT/'server.properties']
        for folder in ('region','entities','poi'):
            paths += [WORLD/folder/f'r.{x}.{z}.mca' for x in (-1,0) for z in (-1,0)]
        for source in paths:
            relative=source.relative_to(ROOT)
            entry={'path':str(relative),'exists':source.exists()}
            if source.is_file():
                target=destination/relative;target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(source,target)
                entry['sha256']=hashlib.sha256(target.read_bytes()).hexdigest();entry['bytes']=target.stat().st_size
            entries.append(entry)
        (destination/'manifest.json').write_text(json.dumps({'dimension':'minecraft:overworld','bounds':cfg,'files':entries},indent=2))
        print('Verified pre-construction backup saved:',sum(e.get('bytes',0) for e in entries),'bytes',flush=True)
    finally:print(command('save-on'),flush=True)
    sys.exit(0)

assert (HERE/'before/manifest.json').is_file(),'Back up the affected world files first'
progress_path=HERE/'construction-progress.json'
progress=json.loads(progress_path.read_text()) if progress_path.exists() else {'shell':[],'interior':[]}
if '--cleanup' in sys.argv:progress.setdefault('cleanup',[])
def fill(x1,y1,z1,x2,y2,z2,block):
    assert -100<=x1<=x2<=100 and -100<=z1<=z2<=100 and -64<=y1<=y2<=319
    assert (x2-x1+1)*(y2-y1+1)*(z2-z1+1)<=32768
    result=in_world(f'fill {x1} {y1} {z1} {x2} {y2} {z2} minecraft:{block} replace')
    if 'Successfully filled' not in result and 'No blocks were filled' not in result:
        raise RuntimeError('Fill did not succeed: '+result)

chunks=[(x,z) for x in range(-7,7) for z in range(-7,7)]
for stage in (('cleanup',) if '--cleanup' in sys.argv else ('shell','interior')):
    for cx,cz in chunks:
        key=f'{cx},{cz}'
        if key in progress[stage]:continue
        x1=max(-100,cx*16);x2=min(100,cx*16+15);z1=max(-100,cz*16);z2=min(100,cz*16+15)
        query=in_world(f'forceload query {cx*16} {cz*16}')
        was_forced='is not marked' not in query
        if not was_forced:in_world(f'forceload add {cx*16} {cz*16}')
        try:
            nonce='load-'+str(time.monotonic_ns())
            request_path=HERE/'load-request.json'
            temporary=HERE/'load-request.tmp'
            temporary.write_text(json.dumps({'cx':cx,'cz':cz,'nonce':nonce}))
            temporary.replace(request_path)
            deadline=time.monotonic()+180
            while True:
                ready_path=HERE/'load-ready.json'
                try: ready=json.loads(ready_path.read_text())
                except (FileNotFoundError,json.JSONDecodeError):ready={}
                if ready.get('nonce')==nonce:break
                if time.monotonic()>deadline:raise TimeoutError(f'Chunk {key} is still generating')
                time.sleep(0.25)
            if stage=='shell':
                fill(x1,-64,z1,x2,-64,z2,'bedrock');fill(x1,319,z1,x2,319,z2,'bedrock')
                for x in (-100,100):
                    if x1<=x<=x2:fill(x,-63,z1,x,318,z2,'bedrock')
                for z in (-100,100):
                    if z1<=z<=z2:fill(x1,-63,z,x2,318,z,'bedrock')
            else:
                a=max(x1,-99);b=min(x2,99);c=max(z1,-99);d=min(z2,99)
                if a<=b and c<=d:
                    for y in range(-63,319,128):fill(a,y,c,b,min(y+127,318),d,'air')
                    fill(a,-63,c,b,-63,d,'black_concrete');fill(a,318,c,b,318,d,'black_concrete')
                    for x in (-99,99):
                        if a<=x<=b:fill(x,-62,c,x,317,d,'black_concrete')
                    for z in (-99,99):
                        if c<=z<=d:fill(a,-62,z,b,317,z,'black_concrete')
            progress[stage].append(key);progress_path.write_text(json.dumps(progress,indent=2))
            if len(progress[stage])%10==0 or len(progress[stage])==len(chunks):print(stage,len(progress[stage]),'/',len(chunks),flush=True)
        finally:
            if not was_forced:in_world(f'forceload remove {cx*16} {cz*16}')
in_world('forceload add 0 0')
print(in_world('setblock 0 127 0 minecraft:glowstone'),flush=True)
print(in_world('setworldspawn 0 128 0 0'),flush=True)
print(in_world('gamerule spawnRadius 0'),flush=True)
print(command('save-all flush'),flush=True)
(HERE/'load-request.json').unlink(missing_ok=True)
progress['complete']=True;progress_path.write_text(json.dumps(progress,indent=2))
print('Construction complete; spawn-center chunk intentionally remains force-loaded.',flush=True)
