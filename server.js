const http = require('http');
const fs = require('fs');
const path = require('path');

process.on('uncaughtException', (err) => {
  console.error('Server caught uncaught exception:', err);
});
process.on('unhandledRejection', (reason, promise) => {
  console.error('Server unhandled rejection:', reason);
});

const PORT = 3000;
const PUBLIC_DIR = __dirname;

const MIME_TYPES = {
  '.html': 'text/html; charset=UTF-8',
  '.css': 'text/css; charset=UTF-8',
  '.js': 'application/javascript; charset=UTF-8',
  '.json': 'application/json; charset=UTF-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon',
  '.webp': 'image/webp'
};

const server = http.createServer((req, res) => {
  // Handle CORS
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

  if (req.method === 'OPTIONS') {
    res.writeHead(204);
    res.end();
    return;
  }

  // Basic API mock / health
  if (req.url === '/health' || req.url === '/api/health') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ status: 'ok', time: Date.now() }));
    return;
  }

  const urlPath = req.url.split('?')[0];

  // Handle Local Sync Endpoint
  if (urlPath === '/api/v1/sync' || urlPath === '/sync' || urlPath.startsWith('/api/v1/sync')) {
    if (req.method === 'POST') {
      let body = '';
      req.on('data', chunk => { body += chunk; });
      req.on('end', () => {
        try {
          const parsed = JSON.parse(body || '{}');
          res.writeHead(200, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({
            status: 'success',
            syncedAt: Date.now(),
            items: parsed.items || [],
            purchases: parsed.purchases || [],
            suppliers: parsed.suppliers || [],
            domain: 'localhost:3000'
          }));
        } catch (e) {
          res.writeHead(400, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ status: 'error', message: e.message }));
        }
      });
      return;
    } else {
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ status: 'online', domain: 'localhost:3000', syncedAt: Date.now() }));
      return;
    }
  }
  let safePath = path.normalize(urlPath).replace(/^(\.\.[\/\\])+/, '');
  if (safePath === '/' || safePath === '') {
    safePath = '/index.html';
  }

  let filePath = path.join(PUBLIC_DIR, safePath);

  // If path is a directory, check for index.html
  if (fs.existsSync(filePath) && fs.statSync(filePath).isDirectory()) {
    filePath = path.join(filePath, 'index.html');
  }

  // Check if file exists in root or fallback to index.html for SPA
  if (!fs.existsSync(filePath)) {
    filePath = path.join(PUBLIC_DIR, 'index.html');
  }

  const ext = path.extname(filePath).toLowerCase();
  const contentType = MIME_TYPES[ext] || 'application/octet-stream';

  fs.readFile(filePath, (err, content) => {
    if (err) {
      res.writeHead(500);
      res.end('Server Error: ' + err.code);
    } else {
      res.writeHead(200, { 'Content-Type': contentType });
      res.end(content);
    }
  });
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`Web portal server listening on port ${PORT}`);
});
