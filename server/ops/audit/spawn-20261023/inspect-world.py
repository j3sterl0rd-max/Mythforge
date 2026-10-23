import gzip
import json
from pathlib import Path
import struct

class NBT:
    def __init__(self, data): self.data=data; self.i=0
    def read(self,n):
        value=self.data[self.i:self.i+n]; self.i+=n; return value
    def number(self,kind): return struct.unpack('>'+kind,self.read(struct.calcsize('>'+kind)))[0]
    def string(self): return self.read(self.number('H')).decode('utf-8')
    def payload(self,t):
        if t in range(1,7): return self.number({1:'b',2:'h',3:'i',4:'q',5:'f',6:'d'}[t])
        if t==7: return self.read(self.number('i'))
        if t==8: return self.string()
        if t==9:
            subtype=self.number('B'); count=self.number('i'); return [self.payload(subtype) for _ in range(count)]
        if t==10:
            value={}
            while True:
                subtype=self.number('B')
                if subtype==0:return value
                name=self.string(); value[name]=self.payload(subtype)
        if t in (11,12): return [self.number('i' if t==11 else 'q') for _ in range(self.number('i'))]
        raise ValueError(t)
    def root(self):
        kind=self.number('B'); self.string(); return self.payload(kind)

world=Path('Mythforge Official Server')
data=NBT(gzip.decompress((world/'level.dat').read_bytes())).root()['Data']
print('World spawn:', {k:data[k] for k in ('SpawnX','SpawnY','SpawnZ','SpawnAngle') if k in data})
low=[10**9]*3; high=[-10**9]*3; total=0
for path in Path('config/paxi/datapacks/mythforge-refuge/data/mythforge/structure/refuge').glob('*.nbt'):
    import re
    match=re.match(r'x(\d+)_y(\d+)_z(\d+)',path.stem)
    if not match:continue
    offset=[int(v)*32 for v in match.groups()]; offset[1]+=64
    template=NBT(gzip.decompress(path.read_bytes())).root()
    palette=template['palette']
    for entry in template['blocks']:
        if palette[entry['state']]['Name'] in ('minecraft:air','minecraft:cave_air','minecraft:void_air','minecraft:structure_void'):continue
        pos=[a+b for a,b in zip(offset,entry['pos'])]
        low=[min(a,b) for a,b in zip(low,pos)]; high=[max(a,b) for a,b in zip(high,pos)]; total+=1
result={'spawn':{k:data[k] for k in ('SpawnX','SpawnY','SpawnZ','SpawnAngle') if k in data},'refugeBounds':{'min':low,'max':high,'size':[b-a+1 for a,b in zip(low,high)],'nonAirBlocks':total}}
print(json.dumps(result,indent=2)); Path('ops/audit/spawn-20261023/measurement.json').write_text(json.dumps(result,indent=2))
