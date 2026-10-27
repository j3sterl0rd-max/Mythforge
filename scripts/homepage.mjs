// Homepage content stays in the static build; live data only enhances it.
export const icon = name => {
  const paths = {
    arrow: '<path d="M5 12h14m-6-6 6 6-6 6"/>',
    download: '<path d="M12 3v12m-5-5 5 5 5-5M4 16v5h16v-5"/>',
    compass: '<circle cx="12" cy="12" r="9"/><path d="m16 8-2.5 5.5L8 16l2.5-5.5L16 8Z"/>',
    magic: '<path d="m12 2 2.5 7.5L22 12l-7.5 2.5L12 22l-2.5-7.5L2 12l7.5-2.5L12 2Z"/>',
    gear: '<path d="m9 3-1 3-3 1-2 3 2 2-1 3 2 3 3-1 2 3h4l1-3 3-1 2-3-2-2 1-3-2-3-3 1-2-3H9Z"/><circle cx="12" cy="12" r="3"/>',
    server: '<rect x="4" y="3" width="16" height="7" rx="1"/><rect x="4" y="14" width="16" height="7" rx="1"/><path d="M8 6.5h.01M8 17.5h.01M13 6.5h4M13 17.5h4"/>',
    book: '<path d="M12 5v16M3 3c4-1 6 0 9 2 3-2 5-3 9-2v15c-4-1-6 0-9 2-3-2-5-3-9-2V3Z"/>',
    copy: '<rect x="8" y="8" width="12" height="13" rx="1"/><path d="M16 8V3H3v13h5"/>',
    discord: '<path d="M8 5 5 6C3 9 2 13 3 17l4 2 1-2m8-12 3 1c2 3 3 7 2 11l-4 2-1-2M7 16c3 2 7 2 10 0M9 11v2m6-2v2M8 5l1 2m7-2-1 2"/>',
  };
  return `<svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${paths[name] || paths.arrow}</svg>`;
};

export function homePage(c, mods, e, art) {
  const latest = c.latestVersion || mods.version;
  const release = c.latestReleaseUrl || c.links.curseforge;
  const serverVersion = (c.serverTracksLatest ? latest : c.currentVersion) || 'See server details';
  const modCount = mods.mods.filter(m => m.bundled || !/\/(texture-packs|data-packs|customization)\//.test(m.url || '')).length;
  const date = value => new Date(value + 'T12:00:00Z').toLocaleDateString('en-US', {month:'short', day:'numeric', year:'numeric', timeZone:'UTC'});
  const features = [
    ['compass','Exploration & dimensions','Take the long way home.','New biomes, distant dimensions, and structures worth getting lost in.','wonderful-rivers.jpg','Sunlight over a broad waterfall in a forested Minecraft valley','adventure'],
    ['magic','Magic & progression','Find your kind of power.','Learn magical systems, develop your character, and prepare for the next boss.','epic-enemies.jpg','A towering fiery boss inside a lava-lit Minecraft fortress','magic'],
    ['gear','Engineering & building','Make something of it.','Build with Create, automate your workshop, and give your next big idea a home.','curseforge/2049990.png','An illuminated cinema built in Minecraft beneath a starry purple sky','technology'],
  ];
  return `
<section class="hero" aria-labelledby="hero-title">
  <img class="hero-art" src="${art(c.art.hero)}" alt="" fetchpriority="high" width="1672" height="941">
  <div class="container hero-layout">
    <div class="hero-content">
      <p class="eyebrow hero-kicker"><span></span> A Minecraft adventure beyond the familiar</p>
      <h1 id="hero-title">Beyond<br><span>the Horizon.</span></h1>
      <p class="hero-copy">Ancient machines. Uncharted dimensions.<br>Power waiting to be earned.</p>
      <p class="hero-description">A world of exploration, magic, and machinery.<br class="desktop-break"> Pick a direction. Make it your own.</p>
      <div class="actions"><a class="btn primary" href="${e(c.links.curseforge)}">${icon('download')}Download on CurseForge${icon('arrow')}</a><a class="btn ghost" href="/modpack/">Explore the modpack${icon('arrow')}</a></div>
      <a class="hero-release" data-release-link href="${e(release)}"><span class="release-indicator"></span> Latest release <strong data-latest-version data-file-id="${mods.fileId}">${e(latest)}</strong><span aria-hidden="true">↗</span></a>
    </div>
    <aside class="home-server" data-status data-pack-version="${e(serverVersion)}" data-endpoint="${e(c.status.endpoint || '')}" data-timeout="${e(c.status.timeoutMs)}" data-interval="${e(c.status.refreshMs)}" aria-labelledby="home-server-title">
      <div class="server-heading">${icon('server')}<div><p class="eyebrow">The official server</p><h2 id="home-server-title">A world worth sharing.</h2></div></div>
      <p class="server-live" role="status"><span class="status-dot" aria-hidden="true"></span><span data-field="state">Checking server status…</span><span class="server-players" data-field="players"></span></p>
      <button class="server-copy" data-copy="${e(c.serverAddress)}" aria-label="Copy server address ${e(c.serverAddress)}"><span>${e(c.serverAddress)}</span>${icon('copy')}</button>
      <p class="server-version">Server pack <strong data-field="version">${e(serverVersion)}</strong><span>Whitelist required</span></p>
      <div class="actions"><a class="btn server-apply" href="/apply/">Apply for the whitelist${icon('arrow')}</a><button class="status-refresh" data-refresh aria-label="Refresh server status">↻</button></div>
      <p class="status-note" data-field="note">Checking with mcstatus.io. Status may be cached.</p>
    </aside>
  </div>
</section>
<section class="pack-strip" aria-label="Modpack requirements"><div class="container pack-facts">
  <div>${icon('compass')}<dl><dt>Minecraft</dt><dd>${e(c.minecraftVersion)}</dd></dl></div>
  <div>${icon('gear')}<dl><dt>Mod loader</dt><dd>${e(c.loader)}</dd></dl></div>
  <div>${icon('download')}<dl><dt>Latest version</dt><dd data-latest-version>${e(latest)}</dd></dl></div>
  <div>${icon('book')}<dl><dt>Included mods</dt><dd>${modCount}<small>in ${e(mods.version)}</small></dd></dl></div>
  <div>${icon('server')}<dl><dt>Allocated RAM</dt><dd>${e(c.recommendedRam)}</dd></dl></div>
</div></section>
<section class="section home-showcase" id="discover"><div class="container">
  <div class="section-heading"><div><p class="eyebrow">Explore · Create · Make it yours</p><h2>A world without a script.</h2></div><p>Chase a discovery. Build an unlikely machine. Take on something bigger. There’s room for all of it here.</p></div>
  <div class="showcase-grid">${features.map(([symbol,tag,title,body,file,alt,id]) => `<a class="showcase-card" href="/modpack/#${id}"><div class="showcase-image"><img src="/assets/art/${file}" alt="${alt}" width="1600" height="860" loading="lazy"><span class="showcase-icon">${icon(symbol)}</span></div><div class="showcase-copy"><p class="eyebrow">${tag}</p><h3>${title}</h3><p>${body}</p><span class="card-link">Explore this path ${icon('arrow')}</span></div></a>`).join('')}</div>
  <div class="showcase-footnote"><span>From the official Mythforge gallery</span><a class="text-link" href="/modpack/#included-mods">Explore all ${mods.mods.length} included projects ${icon('arrow')}</a></div>
</div></section>
<section class="section home-start" id="start"><div class="container">
  <div class="section-heading"><div><p class="eyebrow">Your first steps</p><h2>Less setup. More adventure.</h2></div><a class="text-link" href="/getting-started/">Full installation guide ${icon('arrow')}</a></div>
  <ol class="quick-steps">
    <li><span class="step-index">01</span><div><h3>Get the pack.</h3><p>Install Mythforge from CurseForge into a fresh launcher profile.</p><a href="${e(c.links.curseforge)}">Download Mythforge ↗</a></div></li>
    <li><span class="step-index">02</span><div><h3>Give it some room.</h3><p>Allocate ${e(c.recommendedRam)} of RAM. Use Java ${e(c.javaVersion)} and let your launcher handle ${e(c.loader)}.</p><a href="/getting-started/">Check your setup →</a></div></li>
    <li><span class="step-index">03</span><div><h3>Find your people.</h3><p>Apply for the whitelist, install the server’s matching pack version, and join us.</p><a href="/apply/">Apply to join →</a></div></li>
  </ol>
</div></section>
<section class="section home-journal"><div class="container journal-layout">
  <div><p class="eyebrow">From the forge</p><h2>The next chapter<br>is taking shape.</h2><p class="muted">Release history, fixes, and the latest from Mythforge.</p><a class="text-link" href="/changelog/">Read the changelog ${icon('arrow')}</a></div>
  <div class="journal-releases">${c.changelog.slice(0,3).map((r,i) => `<a class="journal-entry" ${i===0?'data-release-link':''} href="${e(r.sourceUrl || '/changelog/')}"><div><span class="journal-version" ${i===0?'data-latest-version':''}>${e(r.version)}</span>${i===0?'<span class="release-tag">Latest release</span>':''}<h3 ${i===0?'data-latest-title':''}>${e(r.title)}</h3></div><time ${i===0?'data-latest-date':''} datetime="${e(r.date)}">${date(r.date)}</time>${icon('arrow')}</a>`).join('')}<p class="release-check" data-release-check>Showing the last verified release. Checking for updates…</p></div>
</div></section>
<section class="section"><div class="container community-banner"><div class="community-mark">${icon('discord')}</div><div><p class="eyebrow">Meet us beyond the horizon</p><h2>Good adventures deserve good company.</h2><p>Share a build, ask a question, or find your next adventuring party.</p></div><a class="btn discord-btn" href="${e(c.links.discord)}">Join the Discord ${icon('arrow')}</a></div></section>
<section class="home-credits"><div class="container"><p>Built with a little help from our explorers. <a href="/beta-testers/">Meet the beta testers →</a></p><a href="/support/">Support the server →</a></div></section>`;
}
