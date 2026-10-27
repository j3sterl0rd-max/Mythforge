import {readFile,writeFile,readdir} from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const c=JSON.parse(await readFile(path.join(root,'data/site.json'),'utf8'));
const origin=c.siteUrl.replace(/\/$/,'');
const escape=s=>s.replace(/[&<>"']/g,x=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[x]));
const metadata={
 '/':['Mythforge - 400+ Mods, Create & Magic on NeoForge 1.21.1','Discover an ambitious NeoForge 1.21.1 adventure with over 400 integrated mods, optimized gameplay, Create, magic and bosses that reward player power.'],
 '/modpack/':['Mythforge Modpack - Create, Magic & RPG Adventure','Explore Mythforge’s player-power philosophy, Create automation, distinct magic systems, bosses and open-ended Minecraft 1.21.1 NeoForge progression.'],
 '/getting-started/':['Install Mythforge - Minecraft 1.21.1 NeoForge Guide','Install Mythforge with a compatible launcher, Java 21 and recommended memory. Follow the setup guide and troubleshoot your first Minecraft adventure.'],
 '/server/':['Mythforge Server - Live at play.mythforge.win','Join the live Mythforge Minecraft server at play.mythforge.win. Apply for the whitelist, install the matching pack and explore our shared NeoForge world.'],
 '/beta-testers/':['Mythforge Beta Testers & Thanks','Meet the players who explored early Mythforge builds, reported problems, and helped shape Beyond the Horizon.']
};
async function files(dir){const result=[];for(const f of await readdir(dir,{withFileTypes:true})){const p=path.join(dir,f.name);result.push(...(f.isDirectory()?await files(p):p.endsWith('.html')?[p]:[]));}return result;}
for(const file of await files(path.join(root,'dist'))){
 const route='/'+path.relative(path.join(root,'dist'),file).replaceAll(path.sep,'/').replace(/index\.html$/,'');
 let html=await readFile(file,'utf8');
 if(metadata[route]){const [title,description]=metadata[route];html=html.replace(/<title>[\s\S]*?<\/title>/,`<title>${escape(title)}</title>`).replace(/(<meta (?:name="description"|property="og:description"|name="twitter:description") content=")[^"]*/g,`$1${escape(description)}`).replace(/(<meta (?:property="og:title"|name="twitter:title") content=")[^"]*/g,`$1${escape(title)}`);}
 const graph=[{'@type':'Organization','@id':origin+'/#organization',name:'Mythforge',url:origin+'/'},{'@type':'WebSite','@id':origin+'/#website',name:'Mythforge: Beyond the Horizon',url:origin+'/',publisher:{'@id':origin+'/#organization'}}];
 if(route!=='/404.html')graph.push({'@type':'WebPage','@id':origin+route+'#page',url:origin+route,name:metadata[route]?.[0]||html.match(/<title>(.*?)<\/title>/)[1],isPartOf:{'@id':origin+'/#website'}});
 if(['/','/modpack/'].includes(route))graph.push({'@type':['VideoGame','SoftwareApplication'],'@id':origin+'/#modpack',name:'Mythforge: Beyond the Horizon',url:origin+'/modpack/',applicationCategory:'GameApplication',operatingSystem:'Windows, macOS, Linux',gamePlatform:'Minecraft Java Edition '+c.minecraftVersion+' / '+c.loader,softwareVersion:c.latestVersion,description:'An ambitious, carefully optimized NeoForge 1.21.1 destination pack with over 400 integrated mods. Create engineering, rewarding magic, substantial bosses, dimensions and expressive building come together around player power and exceptional playstyle freedom.',image:origin+'/'+c.art.icon,sameAs:[c.links.curseforge]});
 html=html.replace('</head>',`<meta property="og:locale" content="en_US"><meta name="twitter:image:alt" content="Mythforge: Beyond the Horizon modpack artwork"><link rel="alternate" type="text/plain" href="/llms.txt" title="Mythforge project overview"><script type="application/ld+json">${JSON.stringify({'@context':'https://schema.org','@graph':graph}).replaceAll('<','\\u003c')}</script></head>`);
 html=html.replaceAll(String.fromCharCode(8212),'-');
 html=html.replace(/<img\b[^>]*loading="lazy"[^>]*>/g,tag=>tag.includes('decoding=')?tag:tag.replace('<img','<img decoding="async"'));
 await writeFile(file,html);
}
console.log('Built shared SEO metadata, structured data and visible playstyle content.');
