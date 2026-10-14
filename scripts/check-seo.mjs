import {readFile,readdir} from 'node:fs/promises';
import assert from 'node:assert/strict';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const dist=new URL('../dist/',import.meta.url);
const titles=new Set();
async function check(dir){for(const entry of await readdir(dir,{withFileTypes:true})){
 const file=path.join(dir,entry.name);if(entry.isDirectory()){await check(file);continue;}if(!file.endsWith('.html'))continue;
 const html=await readFile(file,'utf8');
 assert.equal((html.match(/<h1\b/g)||[]).length,1,file+' must have one H1');
 const title=html.match(/<title>(.*?)<\/title>/)[1];assert(!titles.has(title),'Duplicate title: '+title);titles.add(title);
 const graph=JSON.parse(html.match(/<script type="application\/ld\+json">(.*?)<\/script>/s)[1]);assert.equal(graph['@context'],'https://schema.org');
 const canonical=html.match(/rel="canonical" href="([^"]+)"/)[1];
 assert(graph['@graph'].some(item=>item['@type']==='WebSite'));
 if(!file.endsWith('404.html'))assert(graph['@graph'].some(item=>item['@type']==='WebPage'&&item.url===canonical),file+' canonical/schema mismatch');
 assert(!html.includes(String.fromCharCode(8212)),file+' contains an em dash');
}}
await check(fileURLToPath(dist));
const server=await readFile(new URL('server/index.html',dist),'utf8');assert(server.includes('<h1>Server coming soon!</h1>'));
console.log('PASS: unique titles, one H1, valid JSON-LD, canonical agreement, hyphens and server launch status.');
