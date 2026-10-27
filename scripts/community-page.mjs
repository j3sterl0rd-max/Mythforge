import {icon} from './homepage.mjs';

// This is a fixed credit list, rather than a live player leaderboard.
export const betaTesters = [
  ['Slenana','3fab6d20-f7c2-4d23-ad65-4503348ace93'],
  ['Regranaas47','48f2f005-71ce-4cfb-bf46-1e750def5ceb'],
  ['kaididit','aa705957-d779-49e1-95d6-ece6ad98216b'],
  ['Winterberry99','d14db3c0-198b-41e4-a7d0-a6d800d31fa4'],
  ['Irenoir','81622cdc-6dc3-4518-b6e0-ecbbc5934a54'],
  ['Saku1849','ed2473cd-addc-4de9-9f3c-a5fed26e09e7'],
  ['EnderHayes18','049f2b50-8c1a-4c0a-b8b8-dfbd7aabec6f'],
  ['iDonnyboy23','1bb4e05a-10a1-46f3-a1a9-c810ad745409'],
  ['SealedHorror','c56f9c59-4c83-4e80-a4ba-2613b7a76974'],
  ['YVKVZCO','6acd1d1e-b9d5-4dcd-90d1-fa2d4fd659fd'],
];

export function communityPage(c, e) {
  return `<section class="credits-hero">
  <img class="credits-hero-art" src="/assets/art/background.webp" alt="" width="1672" height="941" fetchpriority="high">
  <div class="container"><p class="eyebrow">The people behind the journey</p><h1>Every horizon starts<br>with a few explorers.</h1><p>To the people who stepped into early builds, found the rough edges, and helped shape Mythforge: thank you.</p><a class="text-link" href="#beta-roster">Meet our beta testers ${icon('arrow')}</a></div>
</section>
<section class="section credits-section" id="beta-roster"><div class="container">
  <div class="section-heading"><div><p class="eyebrow">Beta testers &amp; thanks</p><h2>The first through the portal.</h2></div><p>Early adventures. Patient bug reports. Ideas that made the pack better. These are the players who helped us get here.</p></div>
  <ul class="tester-list tester-roster" aria-label="Mythforge beta testers">${betaTesters.map(([name, uuid], i) => `<li><span class="player-head"><span class="head-fallback" aria-hidden="true">${e(name[0])}</span><img data-player-head src="https://minotar.net/helm/${uuid.replaceAll('-','')}/64.png" alt="" width="48" height="48" referrerpolicy="no-referrer" loading="lazy"></span><div><span class="tester-name">${e(name)}</span><span class="tester-role">Beta tester</span></div><span class="tester-number" aria-hidden="true">${String(i+1).padStart(2,'0')}</span></li>`).join('')}</ul>
  <div class="credits-note">${icon('compass')}<p>Every report, test run, and shared discovery helped make Beyond the Horizon what it is today.</p><a class="text-link" href="/changelog/">See how far we’ve come ${icon('arrow')}</a></div>
</div></section>
<section class="section credits-community"><div class="container community-banner"><div class="community-mark">${icon('discord')}</div><div><p class="eyebrow">There’s room for your next adventure</p><h2>Come be part of the story.</h2><p>Meet the community, share what you’re building, and help shape what comes next.</p></div><a class="btn discord-btn" href="${e(c.links.discord)}">Join the community ${icon('arrow')}</a></div></section>`;
}
