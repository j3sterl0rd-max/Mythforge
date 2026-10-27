import {readFile,writeFile,readdir,mkdir} from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {createHash} from 'node:crypto';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const dist=path.join(root,'dist');
const config=JSON.parse(await readFile(path.join(root,'data/site.json'),'utf8'));
const affiliate='<aside class="hosting-card" aria-label="Wabbanode hosting partner"><p class="eyebrow">Host Mythforge with friends</p><a class="hosting-banner" href="https://wabbanode.com/affiliate/mythforgebeyondthehorizon" rel="sponsored noopener noreferrer"><img src="/assets/art/wabbanode-banner.png" alt="Wabbanode partner hosting - use code MYTHFORGE for 25% off. Create your server." width="850" height="170"></a><p class="small">Affiliate link: Mythforge may earn a commission if you purchase. Discount eligibility and terms are set by Wabbanode.</p></aside>';
async function files(dir){let out=[];for(const f of await readdir(dir,{withFileTypes:true})){const p=path.join(dir,f.name);out.push(...(f.isDirectory()?await files(p):[p]));}return out;}
for(const f of await files(dist)){
 if(!/\.(html|js|css|xml|txt)$/.test(f)&&path.basename(f)!=='_headers')continue;
 let s=await readFile(f,'utf8');
 if(f.endsWith('.html')){
  if(config.serverComingSoon){
   s=s.replace(/<div><div class="copy-label">Minecraft server address<\/div><button class="copy-address"[\s\S]*?<\/button><\/div>/g,'<div class="server-coming-soon"><p class="eyebrow">The public server</p><strong>Server coming soon!</strong></div>');
   s=s.replace(/<section class="panel"[^>]*data-status[\s\S]*?<\/section>/g,'<aside class="panel"><h2>Server coming soon!</h2><p>Watch Discord for launch news.</p></aside>');
   if(f===path.join(dist,'server/index.html'))s=s.replace(/<main id="main">[\s\S]*?<\/main>/,'<main id="main"><section class="page-head"><div class="container"><p class="eyebrow">The public world</p><h1>Server coming soon!</h1><p>We’re preparing the next shared Mythforge adventure. Watch Discord for launch news.</p><div class="actions"><a class="btn primary" href="/modpack/">Explore the modpack</a><a class="btn" href="'+config.links.discord+'">Join Discord ↗</a></div></div></section></main>');
   if(f===path.join(dist,'getting-started/index.html'))s=s.replace(/<li[^>]*>[\s\S]*?<\/li>/g,item=>/Multiplayer|Add Server|play\.mythforge\.win|Connect to the server/i.test(item)?'':item);
   s=s.replaceAll('play.mythforge.win','Server coming soon!').replaceAll('Join the Server','Server coming soon!').replaceAll('Install the pack before joining','Explore the installation guide').replaceAll('Install the matching server pack version shown on the Server page.','Server coming soon! Watch Discord for launch news.').replaceAll('Connection & server status','Server launch news');
  }
  s=s.replaceAll('Check the server’s required version before connecting.','Install the matching server pack version shown on the Server page.');
  s=s.replaceAll('The server’s required pack version is awaiting publication. Confirm it through the official download or community channel before connecting.','Install the server pack version shown on the Server page before connecting.');
  if(f===path.join(dist,'index.html')){
   s=s.replace('</main>','<section class="hosting-section"><div class="container">'+affiliate+'</div></section></main>');
  }
  if(f===path.join(dist,'support/index.html'))s=s.replace('</main>','<section class="section"><div class="container">'+affiliate+'</div></section></main>');
  if(f===path.join(dist,'privacy/index.html'))s=s.replace('<div class="actions">','<h2>Whitelist applications</h2><p>When you apply, we process your Minecraft username, public account identifier and profile information, application history, and relevant moderation records to assess access and operate the whitelist. Cloudflare Turnstile processes browser and network information to prevent abuse. Staff can access application records and receive private review alerts. We retain application records for up to 90 days, short-lived request identifiers for up to 24 hours, and necessary staff decisions while they remain applicable. Contact admin@mythforge.win for access, correction or deletion requests. Applying does not verify account ownership; Minecraft sign-in is required to join.</p><div class="actions">');
  if(f===path.join(dist,'privacy/index.html'))s=s.replace('<div class="actions">','<h2>Player heads and external services</h2><p>The Beta Testers &amp; Thanks page loads Minecraft player heads from Minotar using the listed players’ public UUIDs. Your browser contacts minotar.net, which receives your IP address and normal request metadata. Head requests send no referrer. Skin updates follow the provider’s cache and may take time to appear. If you are credited and want your name or head removed, email admin@mythforge.win or contact staff privately through the Contact page. A local initial remains available if loading fails.</p><p>Wabbanode links are affiliate links. Following one opens Wabbanode, where its privacy practices apply; Mythforge may earn a commission from purchases.</p><div class="actions">');
 }
 if(path.basename(f)==='_headers')s=s.replace("img-src 'self'","img-src 'self' https://minotar.net");
 s=s.replaceAll('-','-').replaceAll('\-','-');
 await writeFile(f,s);
}
// Content hashes ensure returning visitors receive changed CSS and JavaScript.
const cssHash=createHash('sha256').update(await readFile(path.join(dist,'assets/site.css'))).digest('hex').slice(0,12);
const jsHash=createHash('sha256').update(await readFile(path.join(dist,'assets/site.js'))).digest('hex').slice(0,12);
for(const f of await files(dist))if(f.endsWith('.html')){
 const html=(await readFile(f,'utf8')).replaceAll('/assets/site.css"',`/assets/site.css?v=${cssHash}"`).replaceAll('/assets/site.js"',`/assets/site.js?v=${jsHash}"`);
 await writeFile(f,html);
}
