import {releaseResponse} from './release-service.mjs';
// Serve build-time Markdown from the same URL while retaining static HTML.
export function prefersMarkdown(accept='') {
 let markdown=0,html=0;
 for(const entry of accept.split(',')) {
  const [type,...params]=entry.trim().toLowerCase().split(';');
  const quality=params.find(p=>p.trim().startsWith('q='));
  const q=quality===undefined?1:Number(quality.trim().slice(2));
  if(!Number.isFinite(q)||q<0||q>1)continue;
  if(type==='text/markdown')markdown=Math.max(markdown,q);
  if(type==='text/html')html=Math.max(html,q);
 }
 return markdown>0&&markdown>=html;
}
function varyAccept(headers){const vary=headers.get('Vary');if(vary==='*')return;if(!vary?.split(',').some(v=>v.trim().toLowerCase()==='accept'))headers.set('Vary',vary?`${vary}, Accept`:'Accept');}
export default {
 async fetch(request,env,context) {
  const resource=new URL(request.url);
  if(resource.pathname==='/api/release')return releaseResponse(request,env,context);
  if(resource.pathname==='/api/applications'||resource.pathname.startsWith('/api/applications/')||resource.pathname.startsWith('/api/whitelist-connector/')||resource.pathname.startsWith('/api/whitelist-staff/')) {
   const headers={'Content-Type':'application/json; charset=utf-8','Cache-Control':'no-store','X-Content-Type-Options':'nosniff','Referrer-Policy':'no-referrer'};
   try {
    if(env.WHITELIST_SERVICE) {
     const response=await env.WHITELIST_SERVICE.fetch(request);
     return new Response(response.body,{status:response.status,headers});
    }
   } catch {}
   return new Response(JSON.stringify(resource.pathname.endsWith('/config')?{available:false}:{state:'not_approved'}),{headers});
  }
  if(resource.pathname==='/.well-known/api-catalog'){
   if(!['GET','HEAD'].includes(request.method))return new Response('Method not allowed',{status:405,headers:{Allow:'GET, HEAD'}});
   resource.pathname='/api/catalog.json';resource.search='';
   const asset=await env.ASSETS.fetch(new Request(resource,{headers:{Accept:'application/json'}}));
   if(asset.status!==200)return asset;
   const headers=new Headers(asset.headers);
   headers.set('Content-Type','application/linkset+json');
   headers.set('Link','</.well-known/api-catalog>; rel="api-catalog"; type="application/linkset+json"');
   headers.set('Cache-Control','public, max-age=0, must-revalidate');
   return new Response(request.method==='HEAD'?null:asset.body,{status:200,headers});
  }
  const negotiable=request.method==='GET'||request.method==='HEAD';
  const markdown=negotiable&&prefersMarkdown(request.headers.get('Accept')||'');
  const originHeaders=new Headers(request.headers);
  originHeaders.set('Accept','text/html');
  if(markdown)for(const key of ['If-None-Match','If-Modified-Since','Range','If-Range'])originHeaders.delete(key);
  const html=await env.ASSETS.fetch(new Request(request,{headers:originHeaders,method:markdown?'GET':request.method}));
  if(!negotiable||!html.headers.get('Content-Type')?.includes('text/html'))return html;
  const headers=new Headers(html.headers);varyAccept(headers);
  if(!markdown||![200,404].includes(html.status))return new Response(html.body,{status:html.status,statusText:html.statusText,headers});
  const url=new URL(request.url);
  let page=url.pathname;
  if(page.endsWith('/'))page+='index.html';
  if(!page.endsWith('.html'))page+='/index.html';
  url.pathname=html.status===404?'/assets/markdown/404.md':'/assets/markdown'+page.replace(/\.html$/,'.md');
  url.search='';
  const representation=await env.ASSETS.fetch(new Request(url,{headers:{Accept:'text/plain'}}));
  if(representation.status!==200)return new Response(request.method==='HEAD'?null:html.body,{status:html.status,headers});
  headers.set('Content-Type','text/markdown; charset=utf-8');
  // Validators and lengths from HTML cannot describe the Markdown variant.
  for(const key of ['Content-Length','Content-Encoding','Content-Range','Transfer-Encoding','ETag','Last-Modified'])headers.delete(key);
  headers.set('Cache-Control','public, max-age=0, must-revalidate');
  return new Response(request.method==='HEAD'?null:representation.body,{status:html.status,headers});
 }
};
