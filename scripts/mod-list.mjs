export function modList(data, escape) {
  if (!data?.mods?.length) throw Error('Published release mod list is missing');
  const e = escape;
  return `<section class="section" id="included-mods"><div class="container" data-mod-list>
<div class="section-heading"><div><p class="eyebrow">Inside the pack</p><h2>Included mods & packs</h2></div><p>${data.mods.length} projects in release ${e(data.version)}, including libraries, resource packs, data packs and bundled mods. Updated automatically from the published client pack.</p></div>
<p class="small"><a class="text-link" href="${e(data.sourceUrl)}" rel="noopener noreferrer">View this release on CurseForge ↗</a></p>
<div class="filters"><label for="mod-search">Find a mod or creator</label><input type="search" id="mod-search" placeholder="Name, creator or project type" autocomplete="off"><span class="small" id="mod-count" role="status" aria-live="polite">${data.mods.length} projects shown</span></div>
<ul class="mod-grid" aria-label="Included mods and packs">${data.mods.map(mod => {
    const type = mod.bundled ? 'Bundled mod' : mod.url.includes('/data-packs/') ? 'Data pack' : mod.url.includes('/texture-packs/') ? 'Resource pack' : 'Mod';
    return `<li class="mod-entry">${mod.url ? `<a href="${e(mod.url)}" rel="noopener noreferrer">${e(mod.name)}</a>` : `<strong>${e(mod.name)}</strong>`}<span>${mod.author ? `by ${e(mod.author)} · ` : ''}${type}</span></li>`;
  }).join('')}</ul><p id="no-mods" hidden>No matching projects. Try another name or creator.</p>
</div></section>`;
}
