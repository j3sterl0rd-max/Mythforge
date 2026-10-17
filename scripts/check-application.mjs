import {readFile} from 'node:fs/promises';
import vm from 'node:vm';
import assert from 'node:assert/strict';
import worker from '../worker.mjs';
const code=await readFile(new URL('../public/assets/apply.js',import.meta.url),'utf8');
async function scenario({approved=false,offline=false,slow=false}={}) {
 let clock=0,submit,tokenCallback,shownAt;
 const nodes=new Map();
 const node=key=>{if(!nodes.has(key))nodes.set(key,{hidden:true,textContent:'',disabled:true,focus(){shownAt=clock;}});return nodes.get(key);};
 const form={dataset:{},hidden:true,elements:{username:{value:'TestPlayer'}},querySelector:node,reportValidity:()=>true,addEventListener(type,handler){submit=handler;}};
 const result=node('#application-result');result.querySelector=node;
 const document={querySelector:key=>key==='#whitelist-form'?form:key==='#application-result'?result:node(key),createElement:()=>({}),head:{append(){}}};
 let polls=0;
 const context=vm.createContext({document,performance:{now:()=>clock},setTimeout:(fn,ms)=>{clock+=ms;queueMicrotask(fn);},AbortSignal,window:{turnstile:{render(selector,options){tokenCallback=options.callback;return 1;},remove(){}}},fetch:async path=>{
  if(path.endsWith('/config'))return Response.json({available:!offline,sitekey:'public-key'});
  if(path==='/api/applications')return Response.json({reference:'opaque'});
  polls++;return Response.json({state:slow&&polls<4?'processing':approved?'approved':'not_approved'});
 }});
 vm.runInContext(code,context);
 for(let i=0;i<10;i++)await new Promise(resolve=>setImmediate(resolve));
 if(offline){assert.equal(form.hidden,true);return;}
 context.window.mythforgeTurnstileReady();tokenCallback('proof');
 assert.equal(node('button[type="submit"]').disabled,false);
 const pending=submit({preventDefault(){}});
 assert.equal(node('button[type="submit"]').disabled,true);
 assert.equal(node('#application-status').hidden,false);
 await pending;
 assert.ok(shownAt>=5000,`Result shown too soon: ${shownAt}`);
 assert.equal(node('[data-approved]').hidden,!approved);
 assert.equal(node('[data-support]').hidden,approved);
 assert.equal(node('input').readOnly,true);
}
await scenario({approved:true});await scenario();await scenario({approved:true,slow:true});await scenario({offline:true});
const unavailable=await worker.fetch(new Request('https://mythforge.win/api/applications/config'),{});
assert.deepEqual(await unavailable.json(),{available:false});
const denied=await worker.fetch(new Request('https://mythforge.win/api/applications',{method:'POST'}),{});
assert.deepEqual(await denied.json(),{state:'not_approved'});
const html=await readFile(new URL('../dist/apply/index.html',import.meta.url),'utf8');
assert.ok(html.includes('href="/contact/"'));
assert.ok(html.includes('Minecraft Java Username'));
console.log('PASS: application minimum display time, slow processing, generic results, duplicate click guard, disabled deployment and contact links.');
