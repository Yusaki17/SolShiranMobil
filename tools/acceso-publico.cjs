// Adaptador de desarrollo: solo publica la API y una página de retorno neutral.
const http = require('node:http');
const pagina = `<!doctype html><html lang="es"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Volver a Sol de Shirán</title><style>body{background:#f5efe3;color:#213e2b;font:18px system-ui;padding:32px;max-width:600px;margin:40px auto}main{background:white;padding:28px;border-radius:20px}h1{color:#245439}</style><main><h1>Sol de Shirán</h1><p>Regresa a la aplicación y pulsa <strong>Consultar estado del pago</strong>.</p><p>Esta página no confirma el pago. El resultado se verifica con Mercado Pago desde el servidor.</p></main></html>`;
const servidor = http.createServer((req, res) => {
  const ruta = new URL(req.url, 'http://localhost').pathname;
  if (ruta === '/pago/resultado' && req.method === 'GET') {
    res.writeHead(200, {'Content-Type':'text/html; charset=utf-8','Cache-Control':'no-store','Content-Security-Policy':"default-src 'none'; style-src 'unsafe-inline'; frame-ancestors 'none'",'Referrer-Policy':'no-referrer'});
    return res.end(pagina);
  }
  if (!ruta.startsWith('/api/v1/')) { res.writeHead(404); return res.end(); }
  const headers = {...req.headers, host:'localhost:18061'};
  delete headers['x-user-id']; delete headers['x-user-role'];
  const upstream = http.request({hostname:'127.0.0.1',port:18061,path:req.url,method:req.method,headers}, respuesta => {
    res.writeHead(respuesta.statusCode, respuesta.headers); respuesta.pipe(res);
  });
  upstream.setTimeout(45000, () => upstream.destroy());
  upstream.on('error', () => { if(!res.headersSent) res.writeHead(502, {'Content-Type':'application/json'}); res.end('{"detail":"Gateway no disponible. Comprueba los microservicios."}'); });
  req.on('aborted', () => upstream.destroy());
  req.pipe(upstream);
});
servidor.headersTimeout = 15000;
servidor.requestTimeout = 60000;
servidor.listen(18100, '127.0.0.1', () => console.log('Acceso público local: 127.0.0.1:18100 -> gateway 18061'));
