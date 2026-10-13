import http from 'node:http';
import {readFile,stat} from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'../dist');
const mime={'.html':'text/html; charset=utf-8','.css':'text/css; charset=utf-8','.js':'text/javascript; charset=utf-8','.json':'application/json','.webp':'image/webp','.png':'image/png','.jpg':'image/jpeg','.xml':'application/xml','.txt':'text/plain'};
http.createServer(async(req,res)=>{
  try{
    const pathname=decodeURIComponent(new URL(req.url,'http://localhost').pathname);
    let file=path.resolve(root,'.'+pathname);
    if(file!==root&&!file.startsWith(root+path.sep)){res.writeHead(403);res.end();return}
    if((await stat(file).catch(()=>null))?.isDirectory())file=path.join(file,'index.html');
    let body;let code=200;
    try{body=await readFile(file)}catch{file=path.join(root,'404.html');body=await readFile(file);code=404}
    res.writeHead(code,{'Content-Type':mime[path.extname(file)]||'application/octet-stream','X-Content-Type-Options':'nosniff'});
    res.end(req.method==='HEAD'?undefined:body);
  }catch{res.writeHead(400);res.end('Bad request')}
}).listen(8080,'127.0.0.1',()=>console.log('Mythforge local preview: http://localhost:8080 - Ctrl+C to stop'));
