import {icon} from './homepage.mjs';
import {modList} from './mod-list.mjs';

export function modpackPage(c, mods, e, art, pillars) {
  const captions = {
    '2049989': ['A little room for character','A cozy room with colorful decorations, books, and a bubbling potion'],
    '2049990': ['Build something worth coming home to','An illuminated Minecraft cinema under a starry sky'],
    '2049991': ['Prepare for a bigger fight','A towering fiery boss inside a lava-lit fortress'],
    '2049992': ['Think a little bigger','An enormous suspended sphere above a stone platform'],
    '2049993': ['Make every corner your own','A detailed Minecraft interior with vending machines and warm lanterns'],
    '2049994': ['Follow the river','Sunlight over a wide waterfall in a forested valley'],
  };
  return `<section class="page-head illustrated-head"><div class="container"><p class="eyebrow">Mythforge / Beyond the Horizon</p><h1>Follow your curiosity.</h1><p>From a first shelter to a workshop that runs itself. Explore, learn, build, and become powerful in your own way.</p><div class="actions"><a class="btn primary" href="${e(c.links.curseforge)}">${icon('download')}Download the modpack</a><a class="btn ghost" href="#included-mods">Browse included mods ${icon('arrow')}</a></div></div></section>
<section class="pack-profile-section"><div class="container pack-profile"><div class="pack-profile-brand"><img src="${art(c.art.icon)}" alt="Mythforge modpack icon" width="76" height="76"><div><p class="eyebrow">Your next adventure</p><h2>Mythforge</h2><p>Beyond the Horizon</p></div></div><dl class="pack-profile-facts"><div><dt>Minecraft</dt><dd>${e(c.minecraftVersion)}</dd></div><div><dt>Loader</dt><dd>${e(c.loader)}</dd></div><div><dt>Latest release</dt><dd data-latest-version data-file-id="${mods.fileId}">${e(c.latestVersion)}</dd></div><div><dt>Allocated RAM</dt><dd>${e(c.recommendedRam)}</dd></div></dl><a class="text-link" href="/getting-started/">Installation guide ${icon('arrow')}</a></div></section>
<section class="section"><div class="container"><div class="section-heading"><div><p class="eyebrow">One pack. Your direction.</p><h2>Find your next obsession.</h2></div><p>Mythforge is an open-ended sandbox with mod guides. Follow an interest, master a system, then discover what connects to it.</p></div>${pillars()}</div></section>
<section class="section encounter-section"><div class="container encounter-feature"><div class="encounter-art"><img src="${art(c.art.boss)}" alt="A fiery boss waits inside a lava-lit Minecraft fortress" width="1600" height="860" loading="lazy"></div><div class="encounter-copy"><p class="eyebrow">Power with a purpose</p><h2>Bring more than<br>a sharp sword.</h2><p>Bosses across the game and its dimensions give your equipment, magical discoveries, and preparation a purpose. Learn what your tools can do, then take on the next encounter.</p><p class="small">Use the included mod guides to explore each system. There is no single prescribed path through the pack.</p><a class="text-link" href="/getting-started/">Start your adventure ${icon('arrow')}</a></div></div></section>
<section class="section"><div class="container"><div class="section-heading"><div><p class="eyebrow">From the official pack gallery</p><h2>A little of what awaits.</h2></div><p>Places to explore, things to build, and details that make a world feel like yours.</p></div><div class="gallery pack-gallery">${c.art.screenshots.map((s,i) => {
    const key=s.src.match(/(\d+)\.[a-z]+$/)?.[1];
    const [caption, alt]=captions[key] || [s.caption,s.alt];
    return `<figure><img src="${art(s.src)}" alt="${e(alt)}" width="1600" height="860" loading="lazy"><figcaption><span>${String(i+1).padStart(2,'0')}</span>${e(caption)}</figcaption></figure>`;
  }).join('')}</div></div></section>
${modList(mods,e)}`;
}
