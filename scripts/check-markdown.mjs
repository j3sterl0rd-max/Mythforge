import assert from 'node:assert/strict';
import {readFile,readdir} from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import worker,{prefersMarkdown} from '../worker.mjs';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'../dist');
const requests=[];
const env={ASSETS:{async fetch(request){
 requests.push(request);
 let pathname=new URL(request.url).pathname;
 if(pathname.endsWith('/'))pathname+='index.html';
 let content,status=200;
 try{content=await readFile(path.join(root,pathname));}catch{content=await readFile(path.join(root,'404.html'));status=404;}
 const html=pathname.endsWith('.html')||status===404;
 const headers={'Content-Type':html?'text/html; charset=utf-8':pathname.endsWith('.md')?'text/plain':'image/png','ETag':'html-validator','Content-Length':String(content.length),'Content-Security-Policy':"default-src 'self'",'Vary':'Accept-Encoding'};
 return new Response(request.method==='HEAD'?null:content,{status,headers});
}}};
for(const [header,expected] of [['',false],['*/*',false],['text/html,application/xhtml+xml,*/*;q=0.8',false],['text/markdown',true],['text/markdown;q=0',false],['text/markdown;q=0.5,text/html;q=1',false],['text/html;q=0.5,text/markdown;q=1',true],['text/markdown; charset=utf-8',true],['text/markdown;q=bad',false]])assert.equal(prefersMarkdown(header),expected,header);
async function pages(dir){const result=[];for(const f of await readdir(dir,{withFileTypes:true})){const p=path.join(dir,f.name);result.push(...(f.isDirectory()?await pages(p):p.endsWith('.html')?[p]:[]));}return result;}
let count=0;
for(const file of await pages(root)){
 const route='/'+path.relative(root,file).replaceAll(path.sep,'/').replace(/index\.html$/,'');
 const url='https://mythforge.win'+route;
 const html=await worker.fetch(new Request(url),env);assert.match(html.headers.get('Content-Type'),/text\/html/);assert.match(html.headers.get('Vary'),/Accept-Encoding, Accept/);assert.match(await html.text(),/<!doctype html>/i);
 const md=await worker.fetch(new Request(url,{headers:{Accept:'text/markdown','If-None-Match':'html-validator','Range':'bytes=0-5'}}),env);
 assert.equal(md.status,200);assert.equal(md.headers.get('Content-Type'),'text/markdown; charset=utf-8');assert.equal(md.headers.get('ETag'),null);assert.equal(md.headers.get('Content-Length'),null);assert.equal(md.headers.get('Content-Security-Policy'),"default-src 'self'");
 const text=await md.text();assert.match(text,/^---\ntitle:/);assert.ok(!/<(?:html|head|script|style|nav|footer)\b/i.test(text));assert.ok(!text.includes('Footer navigation'));assert.ok(!text.includes('play.mythforge.win'));assert.ok(!text.includes(String.fromCharCode(8212)));
 const head=await worker.fetch(new Request(url,{method:'HEAD',headers:{Accept:'text/markdown'}}),env);assert.equal(await head.text(),'');assert.equal(head.headers.get('Content-Type'),'text/markdown; charset=utf-8');
 count++;
}
const missing=await worker.fetch(new Request('https://mythforge.win/missing/',{headers:{Accept:'text/markdown'}}),env);assert.equal(missing.status,404);assert.match(missing.headers.get('Content-Type'),/text\/markdown/);
const image=await worker.fetch(new Request('https://mythforge.win/assets/art/favicon.png',{headers:{Accept:'text/markdown'}}),env);assert.equal(image.headers.get('Content-Type'),'image/png');
const credits=await worker.fetch(new Request('https://mythforge.win/beta-testers/',{headers:{Accept:'text/markdown'}}),env);const names=await credits.text();assert.match(names,/iDonnyboy23/);assert.ok(!names.includes('SSlenana'));
assert.ok(requests.filter(r=>r.headers.get('Accept')==='text/html'&&r.method==='GET').some(r=>!r.headers.has('If-None-Match')&&!r.headers.has('Range')));
console.log(`PASS: ${count} pages in HTML, Markdown, and HEAD; Accept preferences, cache variants, security headers, assets, 404, and content checks.`);
