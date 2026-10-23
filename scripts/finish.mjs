import {readFile,writeFile,readdir,mkdir} from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {createHash} from 'node:crypto';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const dist=path.join(root,'dist');
const config=JSON.parse(await readFile(path.join(root,'data/site.json'),'utf8'));
const roster=[['Slenana','3fab6d20-f7c2-4d23-ad65-4503348ace93'],['Regranaas47','48f2f005-71ce-4cfb-bf46-1e750def5ceb'],['kaididit','aa705957-d779-49e1-95d6-ece6ad98216b'],['Winterberry99','d14db3c0-198b-41e4-a7d0-a6d800d31fa4'],['Irenoir','81622cdc-6dc3-4518-b6e0-ecbbc5934a54'],['Saku1849','ed2473cd-addc-4de9-9f3c-a5fed26e09e7'],['EnderHayes18','049f2b50-8c1a-4c0a-b8b8-dfbd7aabec6f'],['iDonnyboy23','1bb4e05a-10a1-46f3-a1a9-c810ad745409']];
roster.push(
 ['Procyonine','3b054a0b-7088-4b00-9262-0fbc22a6ef36'],
 ['tony332211','04fc914c-0584-43ff-af3c-28ee0c05a6b5'],
 ['SealedHorror','c56f9c59-4c83-4e80-a4ba-2613b7a76974'],
 ['YVKVZCO','6acd1d1e-b9d5-4dcd-90d1-fa2d4fd659fd']
);
const affiliate='<aside class="hosting-card" aria-label="Wabbanode hosting partner"><p class="eyebrow">Host Mythforge with friends</p><a class="hosting-banner" href="https://wabbanode.com/affiliate/mythforgebeyondthehorizon" rel="sponsored noopener noreferrer"><img src="/assets/art/wabbanode-banner.png" alt="Wabbanode partner hosting - use code MYTHFORGE for 25% off. Create your server." width="850" height="170"></a><p class="small">Affiliate link: Mythforge may earn a commission if you purchase. Discount eligibility and terms are set by Wabbanode.</p></aside>';
async function files(dir){let out=[];for(const f of await readdir(dir,{withFileTypes:true})){const p=path.join(dir,f.name);out.push(...(f.isDirectory()?await files(p):[p]));}return out;}
let home=await readFile(path.join(dist,'index.html'),'utf8');
const beta=home.slice(0,home.indexOf('<main id="main">'))+'<main id="main"><section class="page-head"><div class="container"><p class="eyebrow">The people behind the journey</p><h1>Beta Testers &amp; Thanks</h1><p>Thank you for exploring early builds, finding problems, and helping Mythforge grow.</p></div></section><section class="section"><div class="container thanks-layout"><div><h2>Our beta testers</h2><p>To the explorers who tested early builds and helped us find our footing: thank you.</p><ul class="tester-list">'+roster.map(([name,uuid])=>`<li><span class="player-head"><span class="head-fallback" aria-hidden="true">${name[0]}</span><img data-player-head src="https://minotar.net/helm/${uuid.replaceAll('-','')}/40.png" alt="" width="40" height="40" referrerpolicy="no-referrer" loading="lazy"></span><span>${name}</span></li>`).join('')+'</ul><p class="small"></p></div><img class="thanks-art" src="/assets/art/tester-tools.webp" alt="A lantern illuminates a map, compass, hammer, and worn journal on a stone workbench" width="1672" height="941"></div></section></main>'+home.slice(home.indexOf('<footer'));
await mkdir(path.join(dist,'beta-testers'),{recursive:true});await writeFile(path.join(dist,'beta-testers/index.html'),beta.replaceAll('https://mythforge.win/"','https://mythforge.win/beta-testers/"').replace('<title>Mythforge: Beyond the Horizon</title>','<title>Beta Testers &amp; Thanks | Mythforge</title>').replace('href="/" aria-current="page"','href="/"'));
for(const f of await files(dist)){
 if(!/\.(html|js|css|xml|txt)$/.test(f)&&path.basename(f)!=='_headers')continue;
 let s=await readFile(f,'utf8');
 if(f.endsWith('.html')){
  if(config.serverComingSoon){
   s=s.replace(/<div><div class="copy-label">Minecraft server address<\/div><button class="copy-address"[\s\S]*?<\/button><\/div>/g,'<div class="server-coming-soon"><p class="eyebrow">The public server</p><strong>Server coming soon!</strong></div>');
   s=s.replace(/<section class="panel"[^>]*data-status[\s\S]*?<\/section>/g,'<aside class="panel"><h2>Server coming soon!</h2><p>Watch Discord for launch news.</p></aside>');
   if(f===path.join(dist,'server/index.html'))s=s.replace(/<main id="main">[\s\S]*?<\/main>/,'<main id="main"><section class="page-head"><div class="container"><p class="eyebrow">The public world</p><h1>Server coming soon!</h1><p>We’re preparing the next shared Mythforge adventure. Watch Discord for launch news.</p><div class="actions"><a class="btn primary" href="/modpack/">Explore the modpack</a><a class="btn" href="'+config.links.discord+'">Join Discord ↗</a></div></div></section></main>');
   if(f===path.join(dist,'getting-started/index.html'))s=s.replace(/<li[^>]*>[\s\S]*?<\/li>/g,item=>/Multiplayer|Add Server|play\.mythforge\.win|Connect to the server/i.test(item)?'':item);
   s=s.replaceAll('play.mythforge.win','Server coming soon!').replaceAll('Join the Server','Server coming soon!').replaceAll('Install the pack before joining','Explore the installation guide').replaceAll('The server always follows the latest pack release.','Server coming soon! Watch Discord for launch news.').replaceAll('Connection & server status','Server launch news');
  }
  s=s.replace('<a href="/terms/">','<a href="/beta-testers/">Beta Testers &amp; Thanks</a><a href="/terms/">');
  s=s.replaceAll('Check the server’s required version before connecting.','The server always follows the latest pack release.');
  s=s.replaceAll('The server’s required pack version is awaiting publication. Confirm it through the official download or community channel before connecting.','The server always follows the latest pack release. Install the latest Mythforge release before connecting.');
  s=s.replace('<dt>Required modpack version</dt><dd>To be announced</dd>','<dt>Required modpack version</dt><dd>Latest pack release</dd>');
  if(f===path.join(dist,'index.html')){
   s=s.replace('src="/assets/art/background.webp"','src="/assets/art/orrery-ravine.webp"');
   s=s.replace(/<figure class="art-figure">[\s\S]*?<\/figure>/,'<div class="intro-note"><p class="eyebrow">Built together</p><h3>Every horizon starts with a few brave explorers.</h3><p>Meet the beta testers who helped shape the journey.</p><a class="text-link" href="/beta-testers/">Beta Testers &amp; Thanks →</a></div>');
   s=s.replace('<section class="section">','<section class="hosting-section"><div class="container">'+affiliate+'</div></section><section class="section">');
  }
  if(f===path.join(dist,'support/index.html'))s=s.replace('</main>','<section class="section"><div class="container">'+affiliate+'</div></section></main>');
  if(f===path.join(dist,'privacy/index.html'))s=s.replace('<div class="actions">','<h2>Whitelist applications</h2><p>When you apply, we process your Minecraft username, public account identifier and profile information, application history, and relevant moderation records to assess access and operate the whitelist. Cloudflare Turnstile processes browser and network information to prevent abuse. Staff can access application records and receive private review alerts. We retain application records for up to 90 days, short-lived request identifiers for up to 24 hours, and necessary staff decisions while they remain applicable. Contact admin@mythforge.win for access, correction or deletion requests. Applying does not verify account ownership; Minecraft sign-in is required to join.</p><div class="actions">');
  if(f===path.join(dist,'privacy/index.html'))s=s.replace('<div class="actions">','<h2>Player heads and external services</h2><p>The Beta Testers &amp; Thanks page loads Minecraft player heads from Minotar using the listed players’ public UUIDs. Your browser contacts minotar.net, which receives your IP address and normal request metadata. Head requests send no referrer. Skin updates follow the provider’s cache and may take time to appear. If you are credited and want your name or head removed, email admin@mythforge.win or contact staff privately through the Contact page. A local initial remains available if loading fails.</p><p>Wabbanode links are affiliate links. Following one opens Wabbanode, where its privacy practices apply; Mythforge may earn a commission from purchases.</p><div class="actions">');
 }
 if(path.basename(f)==='_headers')s=s.replace("img-src 'self'","img-src 'self' https://minotar.net");
 s=s.replaceAll('-','-').replaceAll('\-','-');
 await writeFile(f,s);
}
const sitemap=path.join(dist,'sitemap.xml');await writeFile(sitemap,(await readFile(sitemap,'utf8')).replace('</urlset>','<url><loc>https://mythforge.win/beta-testers/</loc></url></urlset>'));
// Content hashes ensure returning visitors receive changed CSS and JavaScript.
const cssHash=createHash('sha256').update(await readFile(path.join(dist,'assets/site.css'))).digest('hex').slice(0,12);
const jsHash=createHash('sha256').update(await readFile(path.join(dist,'assets/site.js'))).digest('hex').slice(0,12);
for(const f of await files(dist))if(f.endsWith('.html')){
 const html=(await readFile(f,'utf8')).replaceAll('/assets/site.css"',`/assets/site.css?v=${cssHash}"`).replaceAll('/assets/site.js"',`/assets/site.js?v=${jsHash}"`);
 await writeFile(f,html);
}
