// Loopback static/SPA server and same-origin gateway proxy for local release rehearsal.
const http = require('node:http'), fs = require('node:fs'), path = require('node:path');
const {verify} = require('./verify-release.cjs');
const [release, portText = '25173', gatewayText = '28080'] = process.argv.slice(2);
const port = Number(portText), gateway = Number(gatewayText);
if (![port, gateway].every(p => Number.isInteger(p) && p > 1024 && p < 65536)) throw Error('Invalid local port');
const checked = verify(release), root = fs.realpathSync(path.join(checked.release, 'frontend'));
const mime = {'.html':'text/html; charset=utf-8','.js':'text/javascript; charset=utf-8','.css':'text/css; charset=utf-8','.svg':'image/svg+xml','.png':'image/png','.ico':'image/x-icon','.woff2':'font/woff2','.json':'application/json'};
http.createServer((req, res) => {
  if (req.url.startsWith('/api/v1/')) {
    const proxy = http.request({host:'127.0.0.1',port:gateway,path:req.url,method:req.method,headers:{...req.headers,host:'127.0.0.1:'+gateway}}, upstream => {res.writeHead(upstream.statusCode, upstream.headers);upstream.pipe(res);});
    proxy.on('error', () => {if (!res.headersSent) res.writeHead(502, {'Content-Type':'application/json'});res.end('{"message":"Local gateway unavailable; verify service readiness"}');});
    req.pipe(proxy); return;
  }
  if (!['GET','HEAD'].includes(req.method)) {res.writeHead(405);res.end();return;}
  let route;
  try {route = decodeURIComponent(new URL(req.url,'http://localhost').pathname);} catch {res.writeHead(400);res.end();return;}
  let file = path.resolve(root, '.' + route);
  if (file !== root && !file.startsWith(root + path.sep)) {res.writeHead(403);res.end();return;}
  if (!fs.existsSync(file) || !fs.statSync(file).isFile()) {
    if (path.extname(route)) {res.writeHead(404);res.end();return;}
    file = path.join(root, 'index.html');
  }
  if (!fs.realpathSync(file).startsWith(root + path.sep)) {res.writeHead(403);res.end();return;}
  res.writeHead(200, {'Content-Type':mime[path.extname(file)]||'application/octet-stream','Cache-Control':file.endsWith('index.html')?'no-store':'public, max-age=31536000, immutable'});
  if(req.method==='HEAD')res.end();else fs.createReadStream(file).pipe(res);
}).listen(port,'127.0.0.1',() => console.log(JSON.stringify({frontend:port,gateway,mode:'packaged static frontend; loopback only'})));
