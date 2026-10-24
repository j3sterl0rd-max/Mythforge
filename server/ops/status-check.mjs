import net from 'node:net';
import fs from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {execFile} from 'node:child_process';
import {promisify} from 'node:util';

const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const args=process.argv.slice(2);
if(args.some(arg=>!['--local','--public'].includes(arg))||args.includes('--local')&&args.includes('--public'))throw Error('Usage: node ops/status-check.mjs [--local | --public]');
let evidence;
if(!args.includes('--public')){
  const python=process.env.MYTHFORGE_PYTHON||'C:/Users/Dad/AppData/Local/Programs/Python/Python311/python.exe';
  const invoke=async command=>(await promisify(execFile)(python,[path.join(root,'ops/maintenance-rcon.py'),Buffer.from(command).toString('base64')],{cwd:root,timeout:30000,windowsHide:true})).stdout.trim();
  const match=/There are (\d+) of a max of (\d+) players online:/.exec(await invoke('list'));
  if(!match)throw Error('Local Minecraft server did not return a valid player count');
  const tick=await invoke('tick query');
  evidence={checkedAt:new Date().toISOString(),mode:'local-rcon',online:Number(match[1]),maxPlayers:Number(match[2]),averageTickMs:Number(/Average time per tick: ([\d.]+)ms/.exec(tick)?.[1])||null};
}else{
  // Public checks traverse TCPShield; never send a plain handshake to the backend.
  const host='play.mythforge.win',port=25565;
  function varint(value){const bytes=[];do{const byte=value&127;value>>>=7;bytes.push(value?byte|128:byte);}while(value);return Buffer.from(bytes);}
  function read(buffer,offset){let value=0;for(let shift=0;shift<35;shift+=7){if(offset>=buffer.length)return null;const byte=buffer[offset++];value|=(byte&127)<<shift;if(!(byte&128))return{value,offset};}throw Error('Invalid status VarInt');}
  const address=Buffer.from(host),portBytes=Buffer.alloc(2);portBytes.writeUInt16BE(port);
  const hello=Buffer.concat([varint(0),varint(767),varint(address.length),address,portBytes,varint(1)]);
  const result=await new Promise((resolve,reject)=>{
    const socket=net.createConnection({host,port});let buffer=Buffer.alloc(0);
    socket.setTimeout(15000,()=>socket.destroy(Error('Public TCPShield status timeout')));
    socket.on('error',reject);
    socket.on('connect',()=>socket.write(Buffer.concat([varint(hello.length),hello,Buffer.from([1,0])])));
    socket.on('data',data=>{try{
      buffer=Buffer.concat([buffer,data]);if(buffer.length>2*1024*1024)throw Error('Oversized status response');
      const frame=read(buffer,0);if(!frame)return;if(frame.value<0||frame.value>2*1024*1024)throw Error('Invalid status frame');if(buffer.length<frame.offset+frame.value)return;
      const id=read(buffer,frame.offset);if(!id||id.value!==0)throw Error('Unexpected status packet');const text=read(buffer,id.offset);
      if(!text||text.value<0||text.offset+text.value>frame.offset+frame.value)throw Error('Invalid status JSON length');
      resolve(JSON.parse(buffer.subarray(text.offset,text.offset+text.value).toString()));socket.end();
    }catch(error){socket.destroy();reject(error);}});
    socket.on('end',()=>reject(Error('Public status connection ended before a complete response')));
  });
  evidence={checkedAt:new Date().toISOString(),mode:'public-tcpshield',host,port,version:result.version,online:result.players.online,maxPlayers:result.players.max,description:result.description};
}
await fs.mkdir(path.join(root,'ops/evidence'),{recursive:true});
await fs.writeFile(path.join(root,'ops/evidence',evidence.mode==='local-rcon'?'server-status.json':'server-public-status.json'),JSON.stringify(evidence,null,2)+'\n');
console.log(JSON.stringify(evidence,null,2));
