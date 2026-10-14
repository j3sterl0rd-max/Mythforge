import {inflateRawSync} from 'node:zlib';

// Read the exact pack requirements, rather than the latest loader available.
export function requirementsFromZip(bytes){
 const zip=Buffer.from(bytes);
 let end=-1;
 for(let i=zip.length-22;i>=Math.max(0,zip.length-65557);i--)if(zip.readUInt32LE(i)===0x06054b50&&i+22+zip.readUInt16LE(i+20)===zip.length){end=i;break;}
 if(end<0)throw Error('Release ZIP directory missing');
 let offset=zip.readUInt32LE(end+16);
 const entries=zip.readUInt16LE(end+10);
 for(let i=0;i<entries;i++){
  if(zip.readUInt32LE(offset)!==0x02014b50)throw Error('Invalid release ZIP directory');
  const nameLength=zip.readUInt16LE(offset+28);
  const name=zip.subarray(offset+46,offset+46+nameLength).toString('utf8');
  if(name==='manifest.json'){
   const local=zip.readUInt32LE(offset+42);
   if(zip.readUInt32LE(local)!==0x04034b50)throw Error('Invalid manifest ZIP entry');
   const start=local+30+zip.readUInt16LE(local+26)+zip.readUInt16LE(local+28);
   const compressed=zip.subarray(start,start+zip.readUInt32LE(offset+20));
   const method=zip.readUInt16LE(offset+10);
   if(![0,8].includes(method))throw Error('Unsupported manifest compression');
   const manifest=JSON.parse((method===8?inflateRawSync(compressed,{maxOutputLength:2000000}):compressed).toString('utf8'));
   const loaders=manifest.minecraft?.modLoaders?.filter(l=>l.primary===true);
   const match=loaders?.length===1&&loaders[0].id.match(/^neoforge-(\d+\.\d+\.\d+(?:-[a-zA-Z0-9.]+)?)$/);
   if(!match||!/^\d+\.\d+(?:\.\d+)?$/.test(manifest.minecraft.version))throw Error('Release manifest must declare a primary NeoForge loader and Minecraft version');
   return {minecraftVersion:manifest.minecraft.version,loader:'NeoForge',loaderVersion:match[1]};
  }
  offset+=46+nameLength+zip.readUInt16LE(offset+30)+zip.readUInt16LE(offset+32);
 }
 throw Error('Release ZIP has no root manifest.json');
}
