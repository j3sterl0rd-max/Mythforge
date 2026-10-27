// The public feed is also used by the scheduled importer. Never expose API keys.
export function selectRelease(feed, snapshot) {
  if (feed?.id !== snapshot.projectId || !Array.isArray(feed.files)) throw Error('Unexpected release feed');
  const files = feed.files.filter(file => file.type === 'release'
    && Array.isArray(file.versions) && file.versions.includes(snapshot.minecraftVersion)
    && file.versions.includes(snapshot.loader) && Number.isSafeInteger(file.id)
    && file.id > snapshot.fileId && !/server/i.test(file.name || '')
    && Number.isFinite(Date.parse(file.uploaded_at))
    && Date.parse(file.uploaded_at) >= Date.parse(snapshot.publishedAt))
    .sort((a,b) => b.id - a.id);
  for (const file of files) {
    const match = (file.name || '').match(/(?:^|[-_\s])v?(\d+(?:\.\d+){1,3})\.zip$/i);
    if (!match) continue; // Don't guess a version from an ambiguous filename.
    return {...snapshot, version:match[1], fileId:file.id,
      url:`https://www.curseforge.com/minecraft/modpacks/mythforge-beyond-the-horizon/files/${file.id}`,
      publishedAt:file.uploaded_at};
  }
  return snapshot; // Cached discovery must never downgrade a verified release.
}

export async function releaseResponse(request, env, context, dependencies = {}) {
  if (!['GET','HEAD'].includes(request.method)) return new Response('Method not allowed', {status:405, headers:{Allow:'GET, HEAD'}});
  const fetchFeed = dependencies.fetch || fetch;
  const cache = dependencies.cache === null ? null : dependencies.cache || globalThis.caches?.default;
  const key = new Request(new URL('/api/release', request.url), {method:'GET'});
  const cached = await cache?.match(key);
  if (cached) return new Response(request.method === 'HEAD' ? null : cached.body, cached);
  const snapshotRequest = new Request(new URL('/assets/release.json', request.url));
  const asset = await env.ASSETS.fetch(snapshotRequest);
  if (!asset.ok) return new Response('Release information unavailable', {status:503});
  const snapshot = await asset.json();
  let latest = snapshot, source = 'snapshot';
  try {
    const response = await fetchFeed(`https://api.cfwidget.com/${snapshot.projectId}`, {
      headers:{Accept:'application/json','User-Agent':'Mythforge-website/2.0 (+https://mythforge.win)'},
      signal:AbortSignal.timeout(5000),
    });
    if (!response.ok) throw Error('Release feed unavailable');
    latest = selectRelease(await response.json(), snapshot);
    source = 'cfwidget';
  } catch { /* Preserve the last verified release through timeouts or provider failures. */ }
  const data = {version:latest.version, fileId:latest.fileId, url:latest.url,
    minecraftVersion:snapshot.minecraftVersion, loader:snapshot.loader,
    publishedAt:latest.publishedAt, source, checkedAt:new Date().toISOString(),
    serverVersion:snapshot.serverVersion, serverTracksLatest:snapshot.serverTracksLatest};
  const headers = {'Content-Type':'application/json; charset=utf-8','X-Content-Type-Options':'nosniff',
    'Cache-Control':source === 'snapshot' ? 'public, max-age=30' : 'public, max-age=60, s-maxage=300'};
  const result = Response.json(data, {headers});
  if (cache && source !== 'snapshot' && context?.waitUntil) context.waitUntil(cache.put(key,result.clone()));
  return new Response(request.method === 'HEAD' ? null : result.body, result);
}
