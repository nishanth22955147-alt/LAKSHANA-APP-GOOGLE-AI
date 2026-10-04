const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = process.env.PORT || 3000;
const PUBLIC_DIR = path.join(__dirname, 'website');
const SYNC_FILE = path.join(PUBLIC_DIR, 'sync-data.json');

// Initialize in-memory cache from sync-data.json if exists
let cachedSyncData = null;
try {
  if (fs.existsSync(SYNC_FILE)) {
    cachedSyncData = JSON.parse(fs.readFileSync(SYNC_FILE, 'utf-8'));
  }
} catch (e) {
  console.log('No prior sync data found.');
}

const MIME_TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon'
};

const server = http.createServer((req, res) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS, PUT, DELETE');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

  if (req.method === 'OPTIONS') {
    res.writeHead(204);
    res.end();
    return;
  }

  // Handle API sync endpoint for mobile & web
  if (req.url.startsWith('/api/v1/sync') || req.url.startsWith('/sync')) {
    if (req.method === 'POST') {
      let body = '';
      req.on('data', chunk => { body += chunk; });
      req.on('end', () => {
        try {
          const parsed = JSON.parse(body);
          cachedSyncData = {
            ...parsed,
            syncedAt: Date.now(),
            serverTime: new Date().toISOString()
          };

          // Persist to sync-data.json
          fs.writeFileSync(SYNC_FILE, JSON.stringify(cachedSyncData, null, 2), 'utf-8');

          res.writeHead(200, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({
            status: 'success',
            syncedAt: Date.now(),
            itemsCount: (parsed.items || parsed.inventory || []).length,
            purchasesCount: (parsed.purchases || []).length,
            suppliersCount: (parsed.suppliers || []).length,
            message: 'Real-time sync successful with lakshanaveggie.trade'
          }));
        } catch (err) {
          res.writeHead(400, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ status: 'error', message: 'Invalid JSON payload: ' + err.message }));
        }
      });
      return;
    } else {
      // GET: Return cached sync data to web portal or mobile app
      res.writeHead(200, { 'Content-Type': 'application/json' });
      if (cachedSyncData) {
        res.end(JSON.stringify(cachedSyncData));
      } else {
        res.end(JSON.stringify({
          status: 'online',
          domain: 'lakshanaveggie.trade',
          timestamp: Date.now(),
          items: [],
          purchases: [],
          suppliers: [],
          users: []
        }));
      }
      return;
    }
  }

  let safePath = path.normalize(req.url.split('?')[0]);
  if (safePath === '/' || safePath === '') {
    safePath = '/index.html';
  }

  let filePath = path.join(PUBLIC_DIR, safePath);

  if (!fs.existsSync(filePath) || fs.statSync(filePath).isDirectory()) {
    filePath = path.join(PUBLIC_DIR, 'index.html');
  }

  const ext = path.extname(filePath).toLowerCase();
  const contentType = MIME_TYPES[ext] || 'application/octet-stream';

  fs.readFile(filePath, (err, content) => {
    if (err) {
      res.writeHead(500, { 'Content-Type': 'text/plain' });
      res.end('500 Server Error: ' + err.message);
    } else {
      res.writeHead(200, { 'Content-Type': contentType });
      res.end(content);
    }
  });
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`Lakshana Veggie Web Server listening on http://0.0.0.0:${PORT}`);
});
