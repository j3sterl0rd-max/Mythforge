import {readFile} from 'node:fs/promises';
import vm from 'node:vm';
import assert from 'node:assert/strict';
import worker from '../worker.mjs';
const code=await readFile(new URL('../public/assets/apply.js',import.meta.url),'utf8');
async function scenario({approved=false,offline=false,slow=false,query='',expectedUsername='',correction='TestPlayer'}={}) {
 let clock=0,submit,tokenCallback,shownAt;
 const nodes=new Map();
 const node=key=>{if(!nodes.has(key))nodes.set(key,{hidden:true,textContent:'',disabled:true,focus(){shownAt=clock;}});return nodes.get(key);};
 const username={value:'',readOnly:false};
 const form={dataset:{},hidden:true,elements:{username},querySelector:key=>key==='input'?username:node(key),reportValidity:()=>true,addEventListener(type,handler){submit=handler;}};
 const result=node('#application-result');result.querySelector=node;
 const document={querySelector:key=>key==='#whitelist-form'?form:key==='#application-result'?result:node(key),createElement:()=>({}),head:{append(){}}};
 let polls=0,applications=0,submittedBody;
 const context=vm.createContext({document,performance:{now:()=>clock},setTimeout:(fn,ms)=>{clock+=ms;queueMicrotask(fn);},AbortSignal,URLSearchParams,window:{location:{search:query},turnstile:{render(selector,options){tokenCallback=options.callback;return 1;},remove(){}}},fetch:async (path,options={})=>{
  if(path.endsWith('/config'))return Response.json({available:!offline,sitekey:'public-key'});
  if(path==='/api/applications'){applications++;submittedBody=JSON.parse(options.body);return Response.json({reference:'opaque'});}
  polls++;return Response.json({state:slow&&polls<4?'processing':approved?'approved':'not_approved'});
 }});
 vm.runInContext(code,context);
 for(let i=0;i<10;i++)await new Promise(resolve=>setImmediate(resolve));
 assert.equal(username.value,expectedUsername,'Only valid query usernames should prefill');
 assert.equal(username.readOnly,false,'Prefilled usernames remain editable');
 assert.equal(applications,0,'Loading the page must not submit an application');
 if(offline){assert.equal(form.hidden,true);return;}
 assert.equal(node('button[type="submit"]').disabled,true);
 await submit({preventDefault(){}});
 assert.equal(applications,0,'Submitting without Turnstile proof must do nothing');
 username.value=correction;
 context.window.mythforgeTurnstileReady();tokenCallback('proof');
 assert.equal(node('button[type="submit"]').disabled,false);
 const pending=submit({preventDefault(){}});
 assert.equal(node('button[type="submit"]').disabled,true);
 assert.equal(node('#application-status').hidden,false);
 await pending;
 assert.ok(shownAt>=5000,`Result shown too soon: ${shownAt}`);
 assert.equal(node('[data-approved]').hidden,!approved);
 assert.equal(node('[data-support]').hidden,approved);
 assert.equal(username.readOnly,true);
 assert.equal(applications,1);
 assert.deepEqual(submittedBody,{username:correction,token:'proof'},'Submit uses the editable field and existing Turnstile token');
}
await scenario({approved:true});await scenario();await scenario({approved:true,slow:true});await scenario({offline:true});
for(const name of ['Abc','Procyonine','_Player123456789']) {
 await scenario({query:'?username='+encodeURIComponent(name),expectedUsername:name,correction:'Changed_Player',approved:true});
}
for(const name of ['', 'ab', 'Player_1234567890', ' Player', 'Player ', 'Player-Name', 'Player.Name', 'Pläyer', '<img src=x onerror=alert(1)>']) {
 await scenario({query:'?username='+encodeURIComponent(name)});
}
await scenario({query:'?other=Procyonine'});
await scenario({query:'?username=Procyonine',expectedUsername:'Procyonine',offline:true});
const unavailable=await worker.fetch(new Request('https://mythforge.win/api/applications/config'),{});
assert.deepEqual(await unavailable.json(),{available:false});
const denied=await worker.fetch(new Request('https://mythforge.win/api/applications',{method:'POST'}),{});
assert.deepEqual(await denied.json(),{state:'not_approved'});
const html=await readFile(new URL('../dist/apply/index.html',import.meta.url),'utf8');
assert.ok(html.includes('href="/contact/"'));
assert.ok(html.includes('Minecraft Java Username'));
console.log('PASS: valid/invalid username prefilling, editable corrections, no automatic submission, Turnstile gate, application timing/status, offline handling and contact links.');
