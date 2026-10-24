import {readFile,writeFile,mkdir,rename} from 'node:fs/promises';
import {fileURLToPath} from 'node:url';
import path from 'node:path';
const root=path.dirname(fileURLToPath(import.meta.url));
const config=JSON.parse(await readFile(path.join(root,'connector.json'),'utf8'));
const endpoint=new URL(config.endpoint);
if(endpoint.protocol!=='https:'||endpoint.origin!=='https://mythforge.win')throw Error('Unexpected connector destination');
const local=new URL('http://127.0.0.1:18766/whitelist');
const bridgeToken=(await readFile(path.join(root,'bridge-token.txt'),'utf8')).trim();
if(typeof config.secret!=='string'||config.secret.length<32||bridgeToken.length<48)throw Error('Connector credentials are missing or invalid');
if(process.argv.includes('--check-config')){console.log('Whitelist connector configuration and Node runtime validated.');process.exit(0);}
const pendingFile=path.join(root,'connector-pending.json');
const healthFile=path.join(root,'connector-health.json');
const health={startedAt:new Date().toISOString(),lastPollAt:null,lastAckAt:null,state:'starting'};
let lastHealthWrite=0;
async function heartbeat(force=false){if(!force&&Date.now()-lastHealthWrite<30000)return;await writeFile(healthFile+'.tmp',JSON.stringify(health));await rename(healthFile+'.tmp',healthFile);lastHealthWrite=Date.now();}
async function call(route,data){const r=await fetch(new URL('/api/whitelist-connector/'+route,endpoint),{method:'POST',headers:{Authorization:'Bearer '+config.secret,'Content-Type':'application/json','User-Agent':'Mythforge-Whitelist-Connector/1.0'},body:JSON.stringify(data),signal:AbortSignal.timeout(25000)});if(!r.ok)throw Error('Upstream unavailable');return r.json();}
async function save(data){await writeFile(pendingFile+'.tmp',JSON.stringify(data));await rename(pendingFile+'.tmp',pendingFile);}
let stopped=false;process.on('SIGINT',()=>{stopped=true;});process.on('SIGTERM',()=>{stopped=true;});
await mkdir(root,{recursive:true});
await heartbeat(true);
console.log(new Date().toISOString(),'Whitelist connector started.');
while(!stopped){
 try {
  let pending;try{pending=JSON.parse(await readFile(pendingFile,'utf8'));}catch{}
  if(pending){const ack=await call('ack',pending);if(ack.ok)await save(null);else await save(null);}
  const {job}=await call('poll',{});
  health.lastPollAt=new Date().toISOString();health.state='connected';await heartbeat();
  if(job){
   if(job.operation==='IDENTITY'){
    if(!/^[A-Za-z0-9_]{3,16}$/.test(job.name)||typeof job.receipt!=='string')throw Error('Invalid identity task');
    let profile;
    for(const base of ['https://api.minecraftservices.com/minecraft/profile/lookup/name/','https://api.mojang.com/users/profiles/minecraft/']){
     try{const r=await fetch(base+encodeURIComponent(job.name),{signal:AbortSignal.timeout(6000)});if(!r.ok)continue;const value=await r.json();if(/^[a-f0-9]{32}$/.test(value.id)&&value.name?.toLowerCase()===job.name.toLowerCase()){profile={id:value.id,name:value.name};break;}}catch{}
    }
    pending={id:job.id,receipt:job.receipt,...(profile?{profile}:{error:'Identity unavailable'})};await save(pending);
    const ack=await call('ack',pending);if(ack.ok){await save(null);health.lastAckAt=new Date().toISOString();await heartbeat(true);}
    continue;
   }
   if(!/^[a-f0-9]{32}$/.test(job.uuid)||!/^[A-Za-z0-9_]{3,16}$/.test(job.name)||!['ADD','CHECK'].includes(job.operation)||typeof job.receipt!=='string')throw Error('Invalid task');
   let result;
   try {
    const r=await fetch(local,{method:'POST',headers:{Authorization:'Bearer '+bridgeToken,'Content-Type':'application/json'},body:JSON.stringify({uuid:job.uuid,name:job.name,operation:job.operation}),signal:AbortSignal.timeout(20000)});
    const value=await r.json();
    if(value.error==='LOCAL_BAN'){result={error:'LOCAL_BAN'};}
    else {
    if(value.uuid!==job.uuid||typeof value.present!=='boolean'||typeof value.verified!=='boolean')throw Error('Unconfirmed local operation');
    result=value;
    if(job.operation==='CHECK'&&value.present===false){
     try{const r=await fetch('https://sessionserver.mojang.com/session/minecraft/profile/'+job.uuid,{signal:AbortSignal.timeout(6000)});if(r.ok){const p=await r.json();if(p.id===job.uuid){const prop=p.properties?.find(x=>x.name==='textures');const texture=prop?JSON.parse(Buffer.from(prop.value,'base64').toString()):null;if(!texture||texture.profileId===job.uuid){const custom=!!texture?.textures?.SKIN?.url;result.skin={status:custom?'VERIFIED_PASS':'VERIFIED_FLAG',kind:custom?'custom':'default'};}}}}catch{}
    }
    }
   }catch{result={error:'Local server did not confirm whitelist operation'};}
   pending={id:job.id,receipt:job.receipt,...result};await save(pending);
   const ack=await call('ack',pending);if(ack.ok){await save(null);health.lastAckAt=new Date().toISOString();await heartbeat(true);}
  }
 }catch(error){health.state='retrying';health.errorKind=error?.name||'Error';await heartbeat().catch(()=>{});console.error(new Date().toISOString(),'Whitelist connector retry scheduled.');}
 await new Promise(resolve=>setTimeout(resolve,5000));
}
