import net from 'node:net';
import fs from 'node:fs';
import crypto from 'node:crypto';
const config=JSON.parse(fs.readFileSync('config/votifier/config.json','utf8'));
const results=[];
async function send(mode,valid=true){
 return new Promise((resolve,reject)=>{
  const socket=net.connect(8193,'127.0.0.1');let greeted=false,answer='';
  socket.setTimeout(5000,()=>socket.destroy(new Error('Votifier timeout')));
  socket.on('error',reject);
  socket.on('data',data=>{
   if(greeted){answer+=data.toString();return;}greeted=true;
   const challenge=data.toString().trim().split(' ')[2];
   if(mode==='v1')socket.write(crypto.publicEncrypt({key:fs.readFileSync('config/votifier/public.pem'),padding:crypto.constants.RSA_PKCS1_PADDING},Buffer.from('VOTE\nMythforgeLocalTest\nMF_VoteTest\n127.0.0.1\n'+Date.now()+'\n')));
   else {
    const payload=JSON.stringify({serviceName:'MythforgeLocalTest',username:'MF_VoteTest',address:'127.0.0.1',timestamp:String(Date.now()),challenge});
    const signature=crypto.createHmac('sha256',valid?config.token:'intentionally-wrong-test-token').update(payload).digest('base64');
    const body=Buffer.from(JSON.stringify({payload,signature}));const header=Buffer.alloc(4);header.writeUInt16BE(0x733a);header.writeUInt16BE(body.length,2);socket.write(Buffer.concat([header,body]));
   }
  });
  socket.on('close',()=>resolve({mode,valid,response:answer.replace(/^[^{]+/,'' )}));
 });
}
results.push(await send('v2',false));results.push(await send('v2'));results.push(await send('v1'));
const saved=JSON.parse(fs.readFileSync('config/votifier/config.json','utf8'));
const count=saved.pendingVotes?.mf_votetest?.length??0;
const passed=results[0].response.includes('error')&&results[1].response.includes('ok')&&count===2;
fs.writeFileSync('ops/evidence/votifier-test.json',JSON.stringify({passed,offlineVotes:count,results},null,2));
console.log(JSON.stringify({passed,offlineVotes:count}));if(!passed)process.exitCode=1;
