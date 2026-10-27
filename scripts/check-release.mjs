import assert from 'node:assert/strict';
import vm from 'node:vm';
import {readFile} from 'node:fs/promises';
import {selectRelease,releaseResponse} from '../release-service.mjs';

const snapshot={projectId:1696232,minecraftVersion:'1.21.1',loader:'NeoForge',version:'1.3.1',fileId:9333841,
  publishedAt:'2026-10-26T14:30:50Z',url:'https://www.curseforge.com/minecraft/modpacks/mythforge-beyond-the-horizon/files/9333841',
  serverVersion:'1.3.1',serverTracksLatest:false};
const file={id:9334000,name:'Mythforge - Beyond the Horizon-1.3.2.zip',type:'release',versions:['1.21.1','NeoForge'],uploaded_at:'2026-10-27T14:30:50Z'};
const feed=(...files)=>({id:1696232,files});
assert.equal(selectRelease(feed(file),snapshot).version,'1.3.2');
assert.equal(selectRelease(feed(file),snapshot).serverVersion,'1.3.1');
for(const change of [{type:'beta'},{versions:['1.21.1','Forge']},{versions:['1.22','NeoForge']},
  {name:'Mythforge-server-1.3.2.zip'},{name:'Mythforge-latest.zip'},{id:9333000},
  {uploaded_at:'invalid'},{uploaded_at:'2026-10-25T00:00:00Z'}]) {
  assert.equal(selectRelease(feed({...file,...change}),snapshot).version,'1.3.1',JSON.stringify(change));
}
assert.throws(()=>selectRelease({id:123,files:[file]},snapshot));
const env={ASSETS:{fetch:async()=>Response.json(snapshot)}};
const request=new Request('https://mythforge.win/api/release');
let calls=0;
const live=await releaseResponse(request,env,null,{cache:null,fetch:async()=>{calls++;return Response.json(feed(file));}});
assert.equal(live.headers.get('content-type'),'application/json; charset=utf-8');
assert.equal((await live.json()).version,'1.3.2');
for(const fetchFeed of [async()=>{throw Error('offline')},async()=>new Response('',{status:503}),async()=>Response.json({id:0})]) {
  const fallback=await releaseResponse(request,env,null,{cache:null,fetch:fetchFeed});
  const data=await fallback.json();assert.equal(data.source,'snapshot');assert.equal(data.version,'1.3.1');
}
const cached=Response.json({version:'1.3.2'});
const hit=await releaseResponse(request,env,null,{cache:{match:async()=>cached},fetch:async()=>{throw Error('Cache must prevent upstream calls');}});
assert.equal((await hit.json()).version,'1.3.2');
let stored,storage;
await releaseResponse(request,env,{waitUntil:promise=>{storage=promise;}},{cache:{match:async()=>null,put:async(key,response)=>{stored=await response.json();}},fetch:async()=>Response.json(feed(file))});
await storage;assert.equal(stored.version,'1.3.2');
const head=await releaseResponse(new Request(request,{method:'HEAD'}),env,null,{cache:null,fetch:async()=>Response.json(feed(file))});
assert.equal(await head.text(),'');assert.equal(head.status,200);
assert.equal((await releaseResponse(new Request(request,{method:'POST'}),env,null,{cache:null})).status,405);

// Exercise visitor-side enhancements, including a client release ahead of the server.
const code=await readFile(new URL('../public/assets/site.js',import.meta.url),'utf8');
async function client(data, fail=false) {
  const events={};
  const fields=new Map();
  const node=selector=>{if(!fields.has(selector))fields.set(selector,{textContent:'1.3.1',href:snapshot.url,dataset:{fileId:String(snapshot.fileId)}});return fields.get(selector);};
  const latest=node('[data-latest-version]');
  const server={dataset:{endpoint:'',packVersion:'1.3.1'},querySelector:selector=>selector==='[data-refresh]'?null:node(selector)};
  const document={hidden:false,documentElement:{classList:{add(){}}},
    querySelector:selector=>selector==='[data-status]'?server:selector==='[data-latest-version]'?latest:selector==='[data-release-check]'?node(selector):null,
    querySelectorAll:selector=>selector.startsWith('[data-latest-')||selector==='[data-release-link]'?[node(selector)]:[],
    addEventListener:(type,handler)=>{events[type]=handler;}};
  vm.runInNewContext(code,{document,navigator:{},URL,Date,AbortSignal,setTimeout,clearTimeout,setInterval(){},
    fetch:async()=>{if(fail)throw Error('offline');return Response.json(data);}});
  await new Promise(resolve=>setImmediate(resolve));
  return {fields,node,server};
}
const data={...snapshot,version:'1.3.2',fileId:9334000,url:'https://www.curseforge.com/minecraft/modpacks/mythforge-beyond-the-horizon/files/9334000',source:'cfwidget'};
let browser=await client(data);
assert.equal(browser.node('[data-latest-version]').textContent,'1.3.2');
assert.equal(browser.server.dataset.packVersion,'1.3.1');
assert.equal(browser.node('[data-release-link]').href,data.url);
browser=await client({...data,serverTracksLatest:true});assert.equal(browser.server.dataset.packVersion,'1.3.2');
for(const [input,fail] of [[data,true],[{...data,url:'https://example.com/download'},false],[{...data,version:'wrong'},false],[{...data,fileId:9333000},false]]) {
  browser=await client(input,fail);assert.equal(browser.node('[data-latest-version]').textContent,'1.3.1');
  assert(browser.node('[data-release-check]').textContent.includes('last verified'));
}
console.log('PASS: live release discovery, compatibility filters, no downgrade, cache, failures, HEAD/methods, client updates, safe links, and independent server version.');
