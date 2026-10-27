import http from 'node:http';
import {readFile,stat} from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {releaseResponse} from '../release-service.mjs';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'../dist');
const port=Number(process.env.MYTHFORGE_PREVIEW_PORT||8080);
const mime={'.html':'text/html; charset=utf-8','.css':'text/css; charset=utf-8','.js':'text/javascript; charset=utf-8','.json':'application/json','.webp':'image/webp','.png':'image/png','.jpg':'image/jpeg','.xml':'application/xml','.txt':'text/plain'};
http.createServer(async(req,res)=>{
  try{
    const pathname=decodeURIComponent(new URL(req.url,'http://localhost').pathname);
    if(pathname==='/api/release'){
      const response=await releaseResponse(new Request('http://localhost:8080/api/release',{method:req.method}),
        {ASSETS:{fetch:async()=>Response.json(JSON.parse(await readFile(path.join(root,'assets/release.json'),'utf8')))}},
        null,{cache:null});
      res.writeHead(response.status,Object.fromEntries(response.headers));res.end(Buffer.from(await response.arrayBuffer()));return;
    }
    let file=path.resolve(root,'.'+pathname);
    if(file!==root&&!file.startsWith(root+path.sep)){res.writeHead(403);res.end();return}
    if((await stat(file).catch(()=>null))?.isDirectory())file=path.join(file,'index.html');
    let body;let code=200;
    try{body=await readFile(file)}catch{file=path.join(root,'404.html');body=await readFile(file);code=404}
    res.writeHead(code,{'Content-Type':mime[path.extname(file)]||'application/octet-stream','X-Content-Type-Options':'nosniff'});
    res.end(req.method==='HEAD'?undefined:body);
  }catch{res.writeHead(400);res.end('Bad request')}
}).listen(port,'127.0.0.1',()=>console.log(`Mythforge local preview: http://localhost:${port} - Ctrl+C to stop`));
