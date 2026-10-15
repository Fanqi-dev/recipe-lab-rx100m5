import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import { spawn } from 'node:child_process';
import { fileURLToPath } from 'node:url';
const here=path.dirname(fileURLToPath(import.meta.url));
const project=path.resolve(here,'../..');
const audit=path.dirname(project);
const java=path.join(audit,'tools/zulu21/zulu21.52.203-ca-jdk21.0.12.1-win_x64/bin/java.exe');
const runtime=path.join(project,'out-safe/live-preview');
fs.mkdirSync(runtime,{recursive:true});
let backend=null,backendPort=0,revision=0,building=false,error='',snapshot='',pending=false,timer;
async function proxy(route,options){if(!backendPort)throw new Error('Preview is building');return await fetch(`http://127.0.0.1:${backendPort}${route}`,options);}
async function rebuild(){
  if(building){pending=true;return;}building=true;error='';
  if(backendPort){try{snapshot=(await (await proxy('/state')).json()).snapshot;}catch{} }
  backendPort=0;if(backend){backend.kill();backend=null;}
  try{
    await new Promise((resolve,reject)=>{
      const child=spawn(process.env.PREVIEW_POWERSHELL||'pwsh',['-NoProfile','-File',path.join(here,'compile.ps1')],{cwd:project,windowsHide:true});
      let log='';child.stdout.on('data',d=>{log+=d;});child.stderr.on('data',d=>{log+=d;});
      child.on('error',reject);child.on('exit',code=>code===0?resolve():reject(new Error(log||'Compile failed')));
    });
    await new Promise((resolve,reject)=>{
      const child=spawn(java,['-Djava.awt.headless=true','-cp',path.join(runtime,'classes'),'com.voxivoid.recipelab.PreviewBridge'],{cwd:project,windowsHide:true});
      backend=child;let log='',ready=false;
      child.stdout.on('data',d=>{log+=d;const match=log.match(/READY_PORT=(\d+)/);if(match&&!ready){ready=true;backendPort=Number(match[1]);resolve();}});
      child.stderr.on('data',d=>{process.stderr.write(d);});child.on('error',reject);
      child.on('exit',code=>{if(!ready)reject(new Error(`Preview backend exited (${code})`));});
    });
    if(snapshot)await proxy('/restore',{method:'POST',body:new URLSearchParams({snapshot})});
    revision++;console.log(`Preview revision ${revision} ready`);
  }catch(failure){error=String(failure.message);console.error(error);}
  finally{building=false;if(pending){pending=false;setTimeout(rebuild,200);}}
}
const server=http.createServer(async(req,res)=>{
  res.setHeader('Cache-Control','no-store');res.setHeader('X-Content-Type-Options','nosniff');
  const host=req.headers.host;
  if(!host||!/^127\.0\.0\.1:\d+$/.test(host)){res.writeHead(403);res.end();return;}
  if(req.headers.origin&&req.headers.origin!==`http://${host}`){res.writeHead(403);res.end();return;}
  const pathname=new URL(req.url,`http://${host}`).pathname;
  try{
    if(pathname==='/'){res.setHeader('Content-Type','text/html; charset=utf-8');res.end(fs.readFileSync(path.join(here,'index.html')));return;}
    if(pathname==='/status'){res.setHeader('Content-Type','application/json');res.end(JSON.stringify({revision,building,error,ready:!!backendPort}));return;}
    if(!['/state','/frame.png','/command'].includes(pathname)){res.writeHead(404);res.end();return;}
    if(!backendPort){res.writeHead(503);res.end('Preview rebuilding');return;}
    if(pathname==='/command'&&req.method!=='POST'){res.writeHead(405);res.end();return;}
    let body='';if(req.method==='POST')for await(const block of req){body+=block;if(body.length>1024){res.writeHead(413);res.end();return;}}
    const reply=await proxy(pathname,{method:req.method,body:req.method==='POST'?body:undefined});
    res.writeHead(reply.status,{'Content-Type':reply.headers.get('content-type')||'application/octet-stream'});res.end(Buffer.from(await reply.arrayBuffer()));
  }catch(failure){res.writeHead(503);res.end('Preview temporarily unavailable');}
});
server.listen(0,'127.0.0.1',()=>{
  const url=`http://127.0.0.1:${server.address().port}/`;
  fs.writeFileSync(path.join(runtime,'server-info.json'),JSON.stringify({url,pid:process.pid,startedAt:new Date().toISOString()}));
  console.log('LIVE_PREVIEW_URL='+url);rebuild();
});
function watch(root){fs.watch(root,{recursive:true},(_,file)=>{if(!file||(!/\.(java|xml|ttf|html|ps1)$/i.test(file)))return;clearTimeout(timer);timer=setTimeout(rebuild,500);});}
watch(path.join(project,'src'));watch(path.join(project,'res'));watch(path.join(project,'assets/fonts'));watch(path.join(project,'verification/ui-shim'));watch(here);
for(const signal of ['SIGINT','SIGTERM'])process.on(signal,()=>{if(backend)backend.kill();server.close();process.exit(0);});
