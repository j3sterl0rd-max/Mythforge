import net from 'node:net';
import fs from 'node:fs';
const port = Number(process.argv[2] || 25566);
if (!Number.isInteger(port) || port < 1 || port > 65535) throw new Error('Invalid port');
function varint(value) {
  const bytes = [];
  do { const byte = value & 127; value >>>= 7; bytes.push(value ? byte | 128 : byte); } while (value);
  return Buffer.from(bytes);
}
function read(buffer, offset) {
  let value = 0;
  for (let shift = 0; shift < 35; shift += 7) {
    if (offset >= buffer.length) return null;
    const byte = buffer[offset++]; value |= (byte & 127) << shift;
    if (!(byte & 128)) return { value, offset };
  }
  throw new Error('Invalid VarInt');
}
const address = Buffer.from('127.0.0.1');
const portBytes = Buffer.alloc(2); portBytes.writeUInt16BE(port);
const hello = Buffer.concat([varint(0), varint(767), varint(address.length), address, portBytes, varint(1)]);
const result = await new Promise((resolve, reject) => {
  const socket = net.createConnection({host:'127.0.0.1', port}); let buffer = Buffer.alloc(0);
  socket.setTimeout(15000, () => socket.destroy(new Error('Status timeout')));
  socket.on('error', reject);
  socket.on('connect', () => socket.write(Buffer.concat([varint(hello.length), hello, Buffer.from([1,0])])));
  socket.on('data', data => {
    try {
      buffer = Buffer.concat([buffer,data]); const frame = read(buffer,0);
      if (!frame || buffer.length < frame.offset + frame.value) return;
      const id = read(buffer,frame.offset); const text = read(buffer,id.offset);
      if (id.value !== 0) throw new Error('Unexpected status packet');
      resolve(JSON.parse(buffer.subarray(text.offset,text.offset+text.value).toString())); socket.end();
    } catch(error) { socket.destroy(); reject(error); }
  });
});
const evidence = {checkedAt:new Date().toISOString(),host:'127.0.0.1',port,version:result.version,online:result.players.online,maxPlayers:result.players.max,description:result.description};
fs.writeFileSync('ops/evidence/server-status.json',JSON.stringify(evidence,null,2)+'\n');
console.log(JSON.stringify(evidence,null,2));
