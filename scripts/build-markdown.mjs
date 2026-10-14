import {readFile,writeFile,readdir,mkdir} from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import TurndownService from 'turndown';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const dist=path.join(root,'dist');
const site=JSON.parse(await readFile(path.join(root,'data/site.json'),'utf8')).siteUrl;
async function files(dir){const out=[];for(const f of await readdir(dir,{withFileTypes:true})){const p=path.join(dir,f.name);out.push(...(f.isDirectory()?await files(p):[p]));}return out;}
const converter=new TurndownService({headingStyle:'atx',bulletListMarker:'-',codeBlockStyle:'fenced'});
converter.remove(['script','style','nav','header','footer','button','input','noscript']);
converter.remove(node=>node.getAttribute?.('aria-hidden')==='true'||node.hasAttribute?.('hidden'));
converter.addRule('absoluteLinks',{filter:'a',replacement(content,node){const href=node.getAttribute('href');if(!href)return content;const label=content.trim()||node.textContent.trim();return label?` [${label}](${new URL(href,site).href}) `:'';}});
converter.addRule('imageDescription',{filter:'img',replacement(_content,node){return node.getAttribute('alt')||'';}});
let count=0;
for(const file of await files(dist)){
 if(!file.endsWith('.html'))continue;
 const html=await readFile(file,'utf8');
 const main=html.match(/<main\b[^>]*>([\s\S]*?)<\/main>/i)?.[1];
 if(!main)throw Error('Missing main content in '+file);
 const relative=path.relative(dist,file);
 const title=converter.turndown(html.match(/<title>([\s\S]*?)<\/title>/i)?.[1]||'Mythforge');
 const markdown=`---\ntitle: ${JSON.stringify(title)}\n---\n\n${converter.turndown(main)}\n`;
 const output=path.join(dist,'assets/markdown',relative.replace(/\.html$/,'.md'));
 await mkdir(path.dirname(output),{recursive:true});await writeFile(output,markdown);count++;
}
console.log(`Generated ${count} Markdown page representations.`);
