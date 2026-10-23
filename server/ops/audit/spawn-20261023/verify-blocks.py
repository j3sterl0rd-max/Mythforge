"""Read saved Overworld chunks and verify every block in the requested box."""
from pathlib import Path
import collections
import gzip
import json
import math
import struct
import zlib

HERE=Path(__file__).resolve().parent
ROOT=HERE.parents[2]
scope={}
exec((HERE/'inspect-world.py').read_text().split("world=Path(")[0],scope)
NBT=scope['NBT']
cache={};counts=collections.Counter();errors=[];checked=0
def chunk(cx,cz):
    region=(cx//32,cz//32)
    if region not in cache:cache[region]=(ROOT/'Mythforge Official Server/region'/f'r.{region[0]}.{region[1]}.mca').read_bytes()
    data=cache[region]; entry=struct.unpack_from('>I',data,4*((cx%32)+(cz%32)*32))[0]
    if not entry:raise ValueError(f'Missing chunk {cx},{cz}')
    offset=(entry>>8)*4096;length=struct.unpack_from('>I',data,offset)[0];kind=data[offset+4];payload=data[offset+5:offset+4+length]
    raw=zlib.decompress(payload) if kind==2 else gzip.decompress(payload) if kind==1 else payload if kind==3 else None
    if raw is None:raise ValueError(f'Unexpected compression {kind}')
    return NBT(raw).root()
for cx in range(-7,7):
    for cz in range(-7,7):
        data=chunk(cx,cz); sections={section['Y']:section for section in data['sections']}
        for sy in range(-4,20):
            states=sections.get(sy,{}).get('block_states',{'palette':[{'Name':'minecraft:air'}]})
            palette=[item['Name'] for item in states['palette']];bits=max(4,(len(palette)-1).bit_length());per_long=64//bits;mask=(1<<bits)-1;packed=states.get('data',[])
            for ly in range(16):
                y=sy*16+ly
                for lz in range(max(0,-100-cz*16),min(15,100-cz*16)+1):
                    z=cz*16+lz
                    for lx in range(max(0,-100-cx*16),min(15,100-cx*16)+1):
                        x=cx*16+lx;index=lx+lz*16+ly*256
                        actual=palette[0] if len(palette)==1 else palette[(packed[index//per_long]>>((index%per_long)*bits))&mask]
                        if x in (-100,100) or z in (-100,100) or y in (-64,319):expected='minecraft:bedrock'
                        elif x in (-99,99) or z in (-99,99) or y in (-63,318):expected='minecraft:black_concrete'
                        elif (x,y,z)==(0,127,0):expected='minecraft:glowstone'
                        else:expected='minecraft:air'
                        checked+=1;counts[actual]+=1
                        equivalent_air=expected=='minecraft:air' and actual in ('minecraft:air','minecraft:cave_air','minecraft:void_air')
                        if actual!=expected and not equivalent_air and len(errors)<30:errors.append({'position':[x,y,z],'actual':actual,'expected':expected})
    print('Verified chunk column',cx,flush=True)
level=NBT(gzip.decompress((ROOT/'Mythforge Official Server/level.dat').read_bytes())).root()['Data']
spawn={k:level[k] for k in ('SpawnX','SpawnY','SpawnZ')}
result={'success':not errors and spawn=={'SpawnX':0,'SpawnY':128,'SpawnZ':0},'checkedBlocks':checked,'counts':dict(counts),'mismatches':errors,'spawn':spawn,'dimension':'minecraft:overworld'}
(HERE/'block-verification.json').write_text(json.dumps(result,indent=2));print(json.dumps(result,indent=2))
if not result['success']:raise SystemExit(1)
