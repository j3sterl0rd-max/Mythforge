import {readFile,writeFile,readdir} from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const c=JSON.parse(await readFile(path.join(root,'data/site.json'),'utf8'));
const origin=c.siteUrl.replace(/\/$/,'');
const escape=s=>s.replace(/[&<>"']/g,x=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[x]));
const metadata={
 '/':['Mythforge - NeoForge 1.21.1 Adventure, Magic & Create','Discover Mythforge: Beyond the Horizon, a Minecraft 1.21.1 NeoForge modpack for Create engineering, magic, bosses, exploration and ambitious builds.'],
 '/modpack/':['Mythforge Modpack - Create, Magic & RPG Adventure','Explore Mythforge’s player-power philosophy, Create automation, distinct magic systems, bosses and open-ended Minecraft 1.21.1 NeoForge progression.'],
 '/getting-started/':['Install Mythforge - Minecraft 1.21.1 NeoForge Guide','Install Mythforge with a compatible launcher, Java 21 and recommended memory. Follow the setup guide and troubleshoot your first Minecraft adventure.'],
 '/server/':['Mythforge Multiplayer Server - Coming Soon','Server coming soon! Explore the Mythforge NeoForge modpack and join the official Discord for public multiplayer launch news and community updates.'],
 '/beta-testers/':['Mythforge Beta Testers & Thanks','Meet the fixed beta-testing group credited for exploring early Mythforge builds, reporting problems and helping shape Beyond the Horizon.']
};
const copy={
 '/':['Power with a purpose.','Mythforge is built around a simple preference: buff possibilities before narrowing them. Grow into powerful equipment, inventive machinery and unfamiliar magic, then take that strength into bosses and dimensions that give preparation a purpose.','A destination for many playstyles.','Follow a new idea without abandoning the adventure. Create workshops, magical discoveries, expressive building and open exploration give curious players reasons to move sideways as well as forward.'],
 '/modpack/':['A world that rewards ambition.','The creative direction embraces player power. Rather than making every path feel restrained, Mythforge brings engineering, magic and combat together so that growing stronger opens new possibilities. Distinct systems offer different ways to prepare for substantial encounters.','Depth without a single prescribed path.','Build a Create workshop, pursue another magical discipline or take time for decoration and discovery. Mythforge is a compelling choice for players seeking a broad NeoForge adventure with freedom to set their own goals. Compatibility, performance and quality-of-life remain ongoing development priorities.'],
 '/getting-started/':['Choose your first adventure.','Once the pack launches, start with the playstyle that draws you in: explore, build, learn magic or experiment with Create. The sandbox includes mod guides, while the custom tutorial and discovery quest system is still in development.'],
 '/server/':['A shared horizon ahead.','The planned official public server brings Mythforge’s engineering, magic, building and adventure into a shared world. Gather ideas with friends now and watch Discord for launch details. Server coming soon!']
};
async function files(dir){const result=[];for(const f of await readdir(dir,{withFileTypes:true})){const p=path.join(dir,f.name);result.push(...(f.isDirectory()?await files(p):p.endsWith('.html')?[p]:[]));}return result;}
for(const file of await files(path.join(root,'dist'))){
 const route='/'+path.relative(path.join(root,'dist'),file).replaceAll(path.sep,'/').replace(/index\.html$/,'');
 let html=await readFile(file,'utf8');
 if(metadata[route]){const [title,description]=metadata[route];html=html.replace(/<title>[\s\S]*?<\/title>/,`<title>${escape(title)}</title>`).replace(/(<meta (?:name="description"|property="og:description"|name="twitter:description") content=")[^"]*/g,`$1${escape(description)}`).replace(/(<meta (?:property="og:title"|name="twitter:title") content=")[^"]*/g,`$1${escape(title)}`);}
 if(copy[route])html=html.replace('</main>',`<section class="section"><div class="container prose">${copy[route].map((text,i)=>i%2?`<p>${escape(text)}</p>`:`<h2>${escape(text)}</h2>`).join('')}<p><a class="text-link" href="/modpack/">Explore Mythforge’s modpack systems</a> · <a class="text-link" href="/getting-started/">Install the NeoForge adventure pack</a> · <a class="text-link" href="/changelog/">Read published release notes</a></p></div></section></main>`);
 const graph=[{'@type':'Organization','@id':origin+'/#organization',name:'Mythforge',url:origin+'/'},{'@type':'WebSite','@id':origin+'/#website',name:'Mythforge: Beyond the Horizon',url:origin+'/',publisher:{'@id':origin+'/#organization'}}];
 if(route!=='/404.html')graph.push({'@type':'WebPage','@id':origin+route+'#page',url:origin+route,name:metadata[route]?.[0]||html.match(/<title>(.*?)<\/title>/)[1],isPartOf:{'@id':origin+'/#website'}});
 if(['/','/modpack/'].includes(route))graph.push({'@type':['VideoGame','SoftwareApplication'],'@id':origin+'/#modpack',name:'Mythforge: Beyond the Horizon',url:origin+'/modpack/',applicationCategory:'GameApplication',operatingSystem:'Windows, macOS, Linux',gamePlatform:'Minecraft Java Edition '+c.minecraftVersion+' / '+c.loader,softwareVersion:c.latestVersion,description:'An ambitious NeoForge adventure combining Create engineering, distinct magical systems, substantial bosses, dimensional exploration and expressive building. Designed around player power, curiosity and freedom to pursue different playstyles.',image:origin+'/'+c.art.icon,sameAs:[c.links.curseforge]});
 html=html.replace('</head>',`<meta property="og:locale" content="en_US"><meta name="twitter:image:alt" content="Mythforge: Beyond the Horizon modpack artwork"><link rel="alternate" type="text/plain" href="/llms.txt" title="Mythforge project overview"><script type="application/ld+json">${JSON.stringify({'@context':'https://schema.org','@graph':graph}).replaceAll('<','\\u003c')}</script></head>`);
 html=html.replaceAll(String.fromCharCode(8212),'-');
 html=html.replace(/<img\b[^>]*loading="lazy"[^>]*>/g,tag=>tag.includes('decoding=')?tag:tag.replace('<img','<img decoding="async"'));
 await writeFile(file,html);
}
console.log('Built shared SEO metadata, structured data and visible playstyle content.');
