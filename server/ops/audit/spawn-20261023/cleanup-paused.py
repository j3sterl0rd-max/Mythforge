from pathlib import Path
import base64
import json
import subprocess

ROOT=Path(__file__).resolve().parents[3]
HERE=Path(__file__).resolve().parent
def console(command):
    subprocess.run(['python',str(ROOT/'ops/maintenance-rcon.py'),base64.b64encode(command.encode()).decode()],check=True,cwd=ROOT)
cfg=ROOT/'config/mythforge-spawn.json'
data=json.loads(cfg.read_text());data['built']=False;cfg.write_text(json.dumps(data,indent=2))
console('kubejs reload server-scripts')
console('say Spawn cleanup: world simulation will pause briefly, then resume automatically. No restart.')
marker=HERE/'simulation-paused.json'
console('tick freeze');marker.write_text(json.dumps({'paused':True}))
try:
    subprocess.run(['python','-u',str(HERE/'build-spawn.py'),'--cleanup'],check=True,cwd=ROOT)
    console('execute in minecraft:overworld run kill @e[type=minecraft:falling_block,x=-100,y=-64,z=-100,dx=200,dy=383,dz=200]')
finally:
    console('tick unfreeze')
    marker.write_text(json.dumps({'paused':False}))
    console('say Spawn cleanup pause finished. World simulation is running again.')
