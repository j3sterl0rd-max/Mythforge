import {readFile,writeFile,mkdir,readdir} from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const dist=path.join(root,'dist');
const site=JSON.parse(await readFile(path.join(root,'data/site.json'),'utf8')).siteUrl.replace(/\/$/,'');
async function pages(dir){const out=[];for(const f of await readdir(dir,{withFileTypes:true})){const p=path.join(dir,f.name);out.push(...(f.isDirectory()?await pages(p):p.endsWith('.html')?[p]:[]));}return out;}
const paths={};
for(const p of await pages(dist)){
 if(path.basename(p)==='404.html')continue;
 const route='/'+path.relative(dist,p).replaceAll(path.sep,'/').replace(/index\.html$/,'');
 const response={description:'Published page in the requested representation.',headers:{Vary:{schema:{type:'string'},description:'Includes Accept to separate HTML and Markdown cache variants.'}},content:{'text/html':{schema:{type:'string'}},'text/markdown':{schema:{type:'string'}}}};
 paths[route]={get:{summary:'Read '+(route==='/'?'the Mythforge homepage':route.replaceAll('/',' ').trim()+' page'),parameters:[{name:'Accept',in:'header',required:false,description:'Use text/markdown for clean main content; HTML is the default.',schema:{type:'string',default:'text/html'},example:'text/markdown'}],responses:{'200':response}},head:{summary:'Read representation headers without a response body',responses:{'200':{description:'Same representation headers as GET; no body.'}}}};
}
await mkdir(path.join(dist,'api'),{recursive:true});
const spec={openapi:'3.1.0',info:{title:'Mythforge public content API',version:'1.0.0',description:'Read-only access to published website pages as HTML or Markdown. No accounts, payments, server administration, or live game-status API is provided.',contact:{email:'admin@mythforge.win'}},servers:[{url:site}],paths};
const catalog={linkset:[{anchor:site+'/',item:[{href:site+'/'}],'service-desc':[{href:site+'/api/openapi.json',type:'application/vnd.oai.openapi+json',title:'Mythforge content API OpenAPI specification'}],'service-doc':[{href:site+'/api/docs.md',type:'text/markdown',title:'Mythforge content API documentation'}]}]};
await writeFile(path.join(dist,'api/catalog.json'),JSON.stringify(catalog,null,2)+'\n');
await writeFile(path.join(dist,'api/openapi.json'),JSON.stringify(spec,null,2)+'\n');
await writeFile(path.join(dist,'api/docs.md'),`# Mythforge public content API

Read published Mythforge website pages without scraping page layout. This read-only API needs no authentication. Send GET to a page URL with Accept: text/markdown for main content, or Accept: text/html for the default browser representation. HEAD returns headers without a body.

Example: curl -H "Accept: text/markdown" ${site}/modpack/

## Discovery

- Catalog: ${site}/.well-known/api-catalog
- OpenAPI: ${site}/api/openapi.json
- Project overview: ${site}/llms.txt

## Pages

${Object.keys(paths).map(p=>'- '+site+p).join('\n')}

## Response behavior

Successful pages return HTTP 200. Missing pages return HTTP 404 and an HTML or Markdown error page according to Accept. Canonical page routes may redirect to a trailing slash. Vary: Accept separates cache representations. Static images and other files retain their usual media types. Markdown and HTML reflect the latest deployed website snapshot, rather than a live query to CurseForge or Minecraft.

The site publishes Content-Signal: ai-train=no, search=yes, ai-input=yes. Respect these preferences, cache responses where practical, and avoid excessive requests. There are no write operations, checkout endpoints, private player records, or game-server control endpoints. Contact admin@mythforge.win for questions.
`);
console.log('Generated RFC 9727 catalog, OpenAPI specification, and public API documentation.');
await writeFile(path.join(dist,'_headers'),(await readFile(path.join(dist,'_headers'),'utf8'))+'\n/api/docs.md\n  Content-Type: text/markdown; charset=utf-8\n/api/openapi.json\n  Content-Type: application/vnd.oai.openapi+json\n');
