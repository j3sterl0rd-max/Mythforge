import {readFile,readdir,stat} from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {spawnSync} from 'node:child_process';
import vm from 'node:vm';
import assert from 'node:assert/strict';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const dist=path.join(root,'dist');let errors=[];let count=0;
async function walk(dir){let out=[];for(const f of await readdir(dir,{withFileTypes:true})){const p=path.join(dir,f.name);out.push(...(f.isDirectory()?await walk(p):[p]))}return out}
for(const file of await walk(dist)){
  if(!file.endsWith('.html'))continue;count++;
  const html=await readFile(file,'utf8');
  for(const marker of ['<html lang="en">','name="viewport"','name="description"','property="og:image"','<main id="main">','Skip to content','aria-label="Main navigation"'])if(!html.includes(marker))errors.push(`${file}: missing ${marker}`);
  if((html.match(/<h1[ >]/g)||[]).length!==1)errors.push(`${file}: must have one h1`);
  const ids=[...html.matchAll(/\bid="([^"]+)"/g)].map(m=>m[1]);if(new Set(ids).size!==ids.length)errors.push(`${file}: duplicate id`);
  for(const img of html.matchAll(/<img\b[^>]*>/g))if(!/\balt="/.test(img[0]))errors.push(`${file}: image without alt`);
  for(const match of html.matchAll(/\b(?:href|src)="([^"]+)"/g)){
    const target=match[1];if(!target||target==='#'||target.startsWith('javascript:')){errors.push(`${file}: empty/unsafe link`);continue}
    if(/^https?:|^mailto:/.test(target))continue;
    const [local,hash]=target.split('?')[0].split('#');
    const resolved=local?path.resolve(dist,'.'+local):file;
    const info=await stat(resolved).catch(()=>null);if(!info){errors.push(`${file}: missing ${target}`);continue}
    const targetFile=info.isDirectory()?path.join(resolved,'index.html'):resolved;
    if(hash&&targetFile.endsWith('.html')){const content=await readFile(targetFile,'utf8');if(!content.includes(`id="${hash}"`))errors.push(`${file}: missing anchor ${target}`)}
  }
}
for(const f of ['scripts/build.mjs','scripts/serve.mjs','public/assets/site.js']){const r=spawnSync(process.execPath,['--check',path.join(root,f)],{encoding:'utf8'});if(r.status)errors.push(r.stderr)}
const css=await readFile(path.join(dist,'assets/site.css'),'utf8');
for(const size of ['900px','600px'])assert(css.includes(`max-width:${size}`));
assert(css.includes('prefers-reduced-motion'));assert(css.includes(':focus-visible'));assert(css.includes('[hidden]'));
// Exercise clipboard fallback, keyboard menu, and status endpoint states without a browser.
const source=await readFile(path.join(dist,'assets/site.js'),'utf8');
function harness({endpoint='',response=null,reject=false,clipboard=true,packVersion=''}={}){
  const events={};let interval=()=>{};const fields={};const dot={className:'status-dot'};
  const refreshButton={disabled:false,addEventListener:(key,fn)=>events.refresh=fn};
  const component={dataset:{endpoint,timeout:'6000',interval:'60000',packVersion},querySelector:s=>s==='[data-refresh]'?refreshButton:s==='.status-dot'?dot:(fields[s]??={textContent:''})};
  const attrs={'aria-expanded':'false'};let open=false,focused=false;
  const menu={getAttribute:k=>attrs[k],setAttribute:(k,v)=>attrs[k]=v,addEventListener:(k,fn)=>events.menu=fn,click:()=>events.menu(),focus:()=>focused=true};
  const nav={classList:{toggle:(_,v)=>open=v}};const toast={textContent:''};let input;
  const copy={dataset:{copy:'play.mythforge.win'},addEventListener:(k,fn)=>events.copy=fn,parentElement:{querySelector:()=>null},after:x=>input=x};let copied;
  const doc={hidden:false,documentElement:{classList:{add(){}}},querySelector:s=>({'[data-status]':component,'.menu-toggle':menu,'#site-nav':nav,'#toast':toast})[s]||null,querySelectorAll:s=>s==='[data-copy]'?[copy]:[],addEventListener:(k,fn)=>events.key=fn,createElement:()=>({setAttribute(){},focus(){},select(){}})};
  const ctx={document:doc,navigator:{clipboard:clipboard?{writeText:async s=>copied=s}:undefined},setTimeout:()=>1,clearTimeout(){},setInterval:fn=>interval=fn,AbortSignal,Date,Math,Number,Error,fetch:async()=>{if(reject)throw Error('network');return {ok:true,json:async()=>response}}};
  vm.runInNewContext(source,ctx);
  return {events,fields,dot,toast,refreshButton,get open(){return open},get focused(){return focused},get input(){return input},get copied(){return copied}};
}
let h=harness();h.events.menu();assert(h.open);h.events.key({key:'Escape'});assert(!h.open);assert(h.focused);await h.events.copy();assert.equal(h.copied,'play.mythforge.win');assert(h.toast.textContent.includes('copied'));
h=harness({clipboard:false});await h.events.copy();assert.equal(h.input.value,'play.mythforge.win');
for(const [response,reject,state] of [[{online:true,players:{online:3,max:25},version:'1.2.3',ping:48},false,'Online'],[{online:false},false,'Offline'],[{online:'fake'},false,'Status unavailable'],[null,true,'Status unavailable']]){
 h=harness({endpoint:'https://status.example.invalid/',response,reject});await new Promise(r=>setImmediate(r));assert.equal(h.fields['[data-field="state"]'].textContent,state);assert.equal(h.refreshButton.disabled,false);
}
h=harness({endpoint:'https://api.mcstatus.io/v2/status/java/play.mythforge.win',packVersion:'1.2.4',response:{online:true,version:{name_clean:'1.21.1'},players:{online:2,max:10},retrieved_at:Date.now()}});
await new Promise(r=>setImmediate(r));
assert.equal(h.fields['[data-field="version"]'].textContent,'1.2.4');
assert.equal(h.fields['[data-field="players"]'].textContent,'2 / 10');
h=harness({endpoint:'https://api.mcstatus.io/v2/status/java/play.mythforge.win',packVersion:'1.2.4',reject:true});
await new Promise(r=>setImmediate(r));
assert.equal(h.fields['[data-field="state"]'].textContent,'Status unavailable');
if(errors.length){console.error(errors.join('\n'));process.exit(1)}
console.log(`PASS: ${count} pages; internal links, assets, anchors, metadata, alt text, JavaScript syntax, responsive rules, keyboard menu, clipboard, and server-status states.`);
