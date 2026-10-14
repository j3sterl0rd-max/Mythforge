import {readFile,writeFile,mkdir} from 'node:fs/promises';
import {fileURLToPath} from 'node:url';
import path from 'node:path';
import {requirementsFromZip} from './release-manifest.mjs';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const configPath=path.join(root,'data/site.json');
const config=JSON.parse(await readFile(configPath,'utf8'));
const key=process.env.CURSEFORGE_API_KEY;
if(!key)throw Error('Set CURSEFORGE_API_KEY in your build environment or GitHub secret; never put it in public files.');
const id=config.curseforge.projectId;
async function api(route){const response=await fetch('https://api.curseforge.com/v1'+route,{headers:{'x-api-key':key,Accept:'application/json'},signal:AbortSignal.timeout(20000)});if(!response.ok)throw Error('CurseForge API status '+response.status);return (await response.json()).data;}
const text=html=>String(html).replace(/<script\b[^>]*>[\s\S]*?<\/script>/gi,'').replace(/<style\b[^>]*>[\s\S]*?<\/style>/gi,'').replace(/<\/p>|<br\s*\/?>|<\/li>|<\/h[1-6]>/gi,'\n').replace(/<[^>]*>/g,'').replace(/&nbsp;/g,' ').replace(/&amp;/g,'&').replace(/&quot;/g,'"').replace(/&#39;/g,"'").replaceAll(String.fromCharCode(8212),'-').replace(/&mdash;|&#8212;|&#x2014;/gi,'-').trim();
const [mod,description,files]=await Promise.all([api('/mods/'+id),api('/mods/'+id+'/description'),api('/mods/'+id+'/files?pageSize=50&gameVersion='+encodeURIComponent(config.minecraftVersion)+'&modLoaderType=6')]);
if(mod.id!==id||mod.slug!=='mythforge-beyond-the-horizon')throw Error('Project identity mismatch');
const releases=files.filter(f=>f.releaseType===1&&!f.isServerPack&&f.gameVersions.includes(config.minecraftVersion)).sort((a,b)=>Date.parse(b.fileDate)-Date.parse(a.fileDate));
if(!releases.length)throw Error('No compatible stable releases returned; existing content preserved');
const version=f=>f.fileName.match(/(\d+\.\d+(?:\.\d+)*)\.zip$/i)?.[1];
if(!version(releases[0]))throw Error('Cannot parse latest version; existing content preserved');
const downloadUrl=releases[0].downloadUrl||await api('/mods/'+id+'/files/'+releases[0].id+'/download-url');
const download=new URL(downloadUrl);
if(download.protocol!=='https:'||!/(^|\.)forgecdn\.net$/.test(download.hostname))throw Error('Unexpected release download host');
const archive=await fetch(download,{signal:AbortSignal.timeout(120000)});
if(!archive.ok)throw Error('Release ZIP download failed: '+archive.status);
const bytes=await archive.arrayBuffer();
if(bytes.byteLength>300000000)throw Error('Release ZIP exceeds supported size');
const requirements=requirementsFromZip(bytes);
if(requirements.minecraftVersion!==config.minecraftVersion)throw Error('Release manifest Minecraft version mismatch');
Object.assign(config,requirements);
const notes=await Promise.all(releases.slice(0,12).map(async f=>({version:version(f)||f.displayName,date:f.fileDate.slice(0,10),title:f.displayName,sourceUrl:mod.links.websiteUrl+'/files/'+f.id,notes:text(await api('/mods/'+id+'/files/'+f.id+'/changelog'))})));
config.latestVersion=version(releases[0]);if(config.serverTracksLatest)config.currentVersion=config.latestVersion;config.latestReleaseUrl=notes[0].sourceUrl;config.links.curseforge=mod.links.websiteUrl;
const invitations=[...description.matchAll(/href=["'](https:\/\/(?:discord\.gg\/|discord\.com\/invite\/)[^"'\s]+)["']/gi)].map(m=>m[1]);
if(invitations.length===1){const old=config.links.discord;config.links.discord=invitations[0];if(config.links.contact===old)config.links.contact=invitations[0];}
const plain=text(description);const ram=plain.match(/Recommended (?:memory|RAM)\s*:\s*([^\n]+)/i);if(ram)config.recommendedRam=ram[1].replace(/\s*allocated.*$/i,'').replaceAll(String.fromCharCode(8212),'-').replace(/&mdash;|&#8212;|&#x2014;/gi,'-').trim();
const java=plain.match(/Java\s*:\s*(\d+)/i);if(java)config.javaVersion=java[1];
// Server version follows the latest pack only when serverTracksLatest is explicitly enabled. Permissions, commerce and legal decisions remain operator-owned.
config.changelog=[...notes,...config.changelog.filter(r=>!notes.some(n=>n.version===r.version))];
const images=[];
await mkdir(path.join(root,'public/assets/art/curseforge'),{recursive:true});
for(const image of mod.screenshots||[]){
 const target=new URL(image.url);
 if(target.protocol!=='https:'||target.hostname!=='media.forgecdn.net')continue;
 const response=await fetch(target,{signal:AbortSignal.timeout(20000)});
 if(!response.ok)throw Error('Gallery image download failed');
 const bytes=new Uint8Array(await response.arrayBuffer());
 const ext=bytes[0]===137&&bytes[1]===80?'png':bytes[0]===255&&bytes[1]===216?'jpg':null;
 if(!ext||bytes.length>15000000)throw Error('Unsupported or oversized gallery image');
 const src='assets/art/curseforge/'+Number(image.id)+'.'+ext;
 if(!Number.isSafeInteger(image.id))throw Error('Invalid screenshot id');
 await writeFile(path.join(root,'public',src),bytes);
 images.push({src,alt:image.title||'Mythforge in-game screenshot',caption:image.description||image.title||'From the official CurseForge gallery'});
}
if(images.length)config.art.screenshots=images;
config.curseforge.lastChecked=new Date().toISOString();
await writeFile(configPath,JSON.stringify(config,null,2)+'\n');
console.log('Synced latest release, official notes, Discord, gallery, and declared installation requirements. Server version preserved.');
