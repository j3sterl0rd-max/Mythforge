// Progressive enhancement only: page content and navigation work without JavaScript.
document.documentElement.classList.add('js');
// Normalize future imported content, including accessible labels.
const emDash = String.fromCharCode(8212);
function normalizeDashes(node) {
  if (!node) return;
  if (node.nodeType === Node.TEXT_NODE && node.nodeValue.includes(emDash)) node.nodeValue = node.nodeValue.replaceAll(emDash, '-');
  if (node.nodeType === Node.ELEMENT_NODE) {
    for (const attr of node.attributes) if (attr.value.includes(emDash)) node.setAttribute(attr.name, attr.value.replaceAll(emDash, '-'));
  }
  for (const child of node.childNodes) normalizeDashes(child);
}
normalizeDashes(document.body);
if (document.body) new MutationObserver(records => {
  for (const record of records) {
    if (record.type === 'childList') for (const node of record.addedNodes) normalizeDashes(node);
    else normalizeDashes(record.target);
  }
}).observe(document.body, {subtree:true,childList:true,characterData:true,attributes:true});
for (const head of document.querySelectorAll('[data-player-head]')) {
  const failed = () => { head.hidden = true; };
  head.addEventListener('error', failed);
  if (head.complete && !head.naturalWidth) failed();
}
const menu = document.querySelector('.menu-toggle');
const nav = document.querySelector('#site-nav');
menu?.addEventListener('click', () => {
  const open = menu.getAttribute('aria-expanded') !== 'true';
  menu.setAttribute('aria-expanded', String(open));
  nav.classList.toggle('open', open);
  menu.textContent = open ? 'Close menu' : 'Menu';
});
document.addEventListener('keydown', event => {
  if (event.key === 'Escape' && menu?.getAttribute('aria-expanded') === 'true') {
    menu.click(); menu.focus();
  }
});
let toastTimer;
function announce(message) {
  const toast = document.querySelector('#toast');
  toast.textContent = message;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { toast.textContent = ''; }, 5000);
}
for (const button of document.querySelectorAll('[data-copy]')) {
  button.addEventListener('click', async () => {
    try {
      if (!navigator.clipboard?.writeText) throw new Error('Clipboard unavailable');
      await navigator.clipboard.writeText(button.dataset.copy);
      announce('Server address copied: ' + button.dataset.copy);
    } catch {
      const input = document.createElement('input');
      input.className = 'manual-copy'; input.readOnly = true;
      input.value = button.dataset.copy; input.setAttribute('aria-label', 'Server address - select and copy');
      button.parentElement.querySelector('.manual-copy')?.remove();
      button.after(input); input.focus(); input.select();
      announce('Select and copy the server address below.');
    }
  });
}
const search = document.querySelector('#release-search');
search?.addEventListener('input', () => {
  let visible = 0;
  for (const release of document.querySelectorAll('.release')) {
    release.hidden = !release.textContent.toLowerCase().includes(search.value.toLowerCase().trim());
    if (!release.hidden) visible++;
  }
  document.querySelector('#no-releases').hidden = visible > 0;
  document.querySelector('#release-count').textContent = `${visible} release${visible === 1 ? '' : 's'} shown`;
});
const modSearch = document.querySelector('#mod-search');
modSearch?.addEventListener('input', () => {
  const query = modSearch.value.toLocaleLowerCase().trim();
  let visible = 0;
  for (const entry of document.querySelectorAll('.mod-entry')) {
    entry.hidden = !entry.textContent.toLocaleLowerCase().includes(query);
    if (!entry.hidden) visible++;
  }
  document.querySelector('#no-mods').hidden = visible > 0;
  document.querySelector('#mod-count').textContent = `${visible} project${visible === 1 ? '' : 's'} shown`;
});
// Optional JSON endpoint contract is documented in README. No endpoint means no request.
const component = document.querySelector('[data-status]');
if (component) {
  const endpoint = component.dataset.endpoint;
  const button = component.querySelector('[data-refresh]');
  const write = (key, value) => { const field=component.querySelector(`[data-field="${key}"]`); if(field)field.textContent = value; };
  let pending = false;
  async function refresh() {
    if (!endpoint || pending) return;
    pending = true; button.disabled = true;
    write('note', 'Checking server status…');
    try {
      const response = await fetch(endpoint, {cache:'no-store', signal:AbortSignal.timeout(Number(component.dataset.timeout))});
      if (!response.ok) throw new Error('Status response failed');
      const data = await response.json();
      if (typeof data.online !== 'boolean') throw new Error('Invalid status schema');
      write('state', data.online ? 'Online' : 'Offline');
      const dot = component.querySelector('.status-dot');
      dot.className = 'status-dot ' + (data.online ? 'online' : 'offline');
      write('players', data.online && Number.isInteger(data.players?.online) && data.players.online >= 0 ? String(data.players.online) + (Number.isInteger(data.players.max) && data.players.max >= data.players.online ? ' / ' + data.players.max : '') : 'Unavailable');
      write('version', component.dataset.packVersion || (typeof data.version === 'string' ? data.version : 'Unavailable'));
      write('ping', data.online && Number.isFinite(data.ping) && data.ping >= 0 ? Math.round(data.ping) + ' ms' : 'Unavailable');
      write('note', 'Checked ' + (Number.isFinite(data.retrieved_at) ? new Date(data.retrieved_at) : new Date()).toLocaleTimeString() + '. Status may be cached by the provider.');
    } catch {
      for (const field of ['players','ping']) write(field, 'Unavailable');
      write('state', 'Status unavailable');
      component.querySelector('.status-dot').className = 'status-dot';
      write('note', 'Status could not be checked. This does not mean the server is offline. Try connecting or check maintenance updates.');
    } finally { pending = false; button.disabled = false; }
  }
  button?.addEventListener('click', refresh);
  if (endpoint) {
    refresh();
    setInterval(() => { if (!document.hidden) refresh(); }, Math.max(30000, Number(component.dataset.interval)));
  }
}

// Package IDs select only preconfigured URLs; query strings never supply checkout URLs.
const checkout=document.querySelector('[data-checkout]');
if(checkout){
 const selected=new URLSearchParams(location.search).get('package');
 const packages=JSON.parse(checkout.dataset.packages||'[]');
 const item=packages.find(p=>p.id===selected);
 if(item){checkout.querySelector('[data-checkout-title]').textContent=item.name;checkout.querySelector('[data-checkout-price]').textContent=item.price+' · Review currency, taxes, and billing at checkout.';checkout.querySelector('[data-checkout-link]').href=item.url;}
 else if(selected){checkout.querySelector('[data-checkout-title]').textContent='Package unavailable';checkout.querySelector('[data-checkout-price]').textContent='This package is not currently available. Browse the official store for current offers.';}
}
