import {readFile} from 'node:fs/promises';

export async function descriptionPreview() {
 const body=await readFile(new URL('../data/curseforge-description.html',import.meta.url),'utf8');
 return `<link rel="stylesheet" href="/assets/description-copy.css">
 <section class="page-head"><div class="container"><p class="eyebrow">Description draft</p><h1>Ready to copy.</h1><p>Copy the formatted description below, then paste it into CurseForge's visual editor. The banners are already in position.</p><button class="btn primary" type="button" id="copy-description">Copy description with banners</button> <button class="btn" type="button" id="select-description">Select description</button><p id="copy-description-status" role="status" aria-live="polite">This preview does not change your CurseForge listing.</p></div></section>
 <section class="section"><div class="container"><article id="description-copy-content" class="description-copy-content" aria-label="Formatted CurseForge description" tabindex="0">${body}</article></div></section>
 <script src="/assets/description-copy.js" defer></script>`;
}
