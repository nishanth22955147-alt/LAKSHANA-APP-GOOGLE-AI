/**
 * Cloudflare Pages & Workers Native Handler: _worker.js
 * Domain: lakshanaveggie.trade
 *
 * Automatically handles:
 * 1. POST /api/v1/sync, /api/sync, /sync - Real-Time Android App & Web Sync
 * 2. GET /api/v1/sync, /api/sync, /sync - Read current synced database state
 * 3. GET /api/v1/health, /health - Edge Health & Diagnostics
 * 4. All other requests -> env.ASSETS.fetch(request) to serve the web portal
 */

let inMemorySyncCache = {
  items: [
    { id: 101, name: 'Country Tomato (நாட்டு தக்காளி)', category: 'Vegetables', currentStockKgs: 450, totalBoxes: 18, defaultRatePerKg: 28 },
    { id: 102, name: 'Hybrid Bangalore Tomato (பெங்களூரு தக்காளி)', category: 'Vegetables', currentStockKgs: 620, totalBoxes: 24, defaultRatePerKg: 32 },
    { id: 103, name: 'Small Onion / Shallots (சின்ன வெங்காயம்)', category: 'Vegetables', currentStockKgs: 380, totalBoxes: 15, defaultRatePerKg: 65 },
    { id: 104, name: 'Bellary Big Onion (பெரிய வெங்காயம்)', category: 'Vegetables', currentStockKgs: 1250, totalBoxes: 50, defaultRatePerKg: 35 },
    { id: 105, name: 'Ooty Fresh Carrot (ஊட்டி கேரட்)', category: 'Vegetables', currentStockKgs: 290, totalBoxes: 12, defaultRatePerKg: 45 },
    { id: 106, name: 'Kolar Potato (உருளைக்கிழங்கு)', category: 'Vegetables', currentStockKgs: 850, totalBoxes: 34, defaultRatePerKg: 30 },
    { id: 107, name: 'Green Chilli (பச்சை மிளகாய்)', category: 'Vegetables', currentStockKgs: 140, totalBoxes: 7, defaultRatePerKg: 55 },
    { id: 108, name: 'Cabbage (முட்டைகோஸ்)', category: 'Vegetables', currentStockKgs: 310, totalBoxes: 10, defaultRatePerKg: 18 },
    { id: 109, name: 'Fresh Ginger (இஞ்சி)', category: 'Vegetables', currentStockKgs: 95, totalBoxes: 4, defaultRatePerKg: 110 },
    { id: 110, name: 'Garlic (பூண்டு)', category: 'Vegetables', currentStockKgs: 120, totalBoxes: 6, defaultRatePerKg: 180 },
    { id: 111, name: 'Beetroot (பீட்ரூட்)', category: 'Vegetables', currentStockKgs: 210, totalBoxes: 9, defaultRatePerKg: 25 },
    { id: 112, name: 'Drumstick (முருங்கைக்காய்)', category: 'Vegetables', currentStockKgs: 85, totalBoxes: 5, defaultRatePerKg: 60 }
  ],
  purchases: [
    { id: 1, poNumber: 'PO-TODAY-01', itemName: 'Country Tomato (நாட்டு தக்காளி)', boxes: 18, qtyKgs: 450, rate: 28, totalAmount: 12600, supplierName: 'Hosur Direct Farm Producers', date: new Date().toISOString().slice(0, 10), paymentStatus: 'UNPAID' },
    { id: 2, poNumber: 'PO-TODAY-02', itemName: 'Bellary Big Onion (பெரிய வெங்காயம்)', boxes: 50, qtyKgs: 1250, rate: 35, totalAmount: 43750, supplierName: 'Nashik Onion Logistics Hub', date: new Date().toISOString().slice(0, 10), paymentStatus: 'PARTIAL' },
    { id: 3, poNumber: 'PO-TODAY-03', itemName: 'Ooty Fresh Carrot (ஊட்டி கேரட்)', boxes: 12, qtyKgs: 290, rate: 45, totalAmount: 13050, supplierName: 'Ooty Hill Vegetable Cooperative', date: new Date().toISOString().slice(0, 10), paymentStatus: 'UNPAID' },
    { id: 4, poNumber: 'PO-TODAY-04', itemName: 'Small Onion / Shallots (சின்ன வெங்காயம்)', boxes: 15, qtyKgs: 380, rate: 65, totalAmount: 24700, supplierName: 'Koyambedu APMC Merchant 14', date: new Date().toISOString().slice(0, 10), paymentStatus: 'PAID' }
  ],
  suppliers: [
    { id: 1, name: 'Koyambedu APMC Merchant 14', contactNumber: '9840123456', location: 'Chennai APMC', outstandingBalance: 42500 },
    { id: 2, name: 'Hosur Direct Farm Producers', contactNumber: '9443210987', location: 'Hosur Valley', outstandingBalance: 28000 },
    { id: 3, name: 'Ooty Hill Vegetable Cooperative', contactNumber: '9789012345', location: 'Nilgiris / Ooty', outstandingBalance: 15400 },
    { id: 4, name: 'Nashik Onion Logistics Hub', contactNumber: '9123456789', location: 'Nashik APMC', outstandingBalance: 65000 }
  ],
  version: 2,
  syncedAt: Date.now()
};

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);

    // 1. CORS Preflight Handlers for Mobile & Web
    if (request.method === 'OPTIONS') {
      return new Response(null, {
        status: 204,
        headers: {
          'Access-Control-Allow-Origin': '*',
          'Access-Control-Allow-Methods': 'GET, POST, OPTIONS, PUT, DELETE',
          'Access-Control-Allow-Headers': 'Content-Type, Authorization, X-Requested-With',
          'Access-Control-Max-Age': '86400'
        }
      });
    }

    const cleanPath = url.pathname.replace(/\/+$/, '').toLowerCase();
    const kv = (env && (env.SYNC_KV || env.LAKSHANA_KV || env.STORAGE || env.DATABASE));

    // 2. Health & Diagnostics Endpoint
    if (cleanPath === '/api/v1/health' || cleanPath === '/health') {
      return new Response(JSON.stringify({
        status: 'online',
        service: 'Lakshana Veggie Procurement Backend',
        domain: url.hostname || 'lakshanaveggie.trade',
        storageType: kv ? 'Cloudflare KV (Persistent Global Storage)' : 'In-Memory Edge Cache',
        kvConfigured: !!kv,
        timestamp: Date.now(),
        serverTime: new Date().toISOString(),
        activeEndpoints: ['/api/v1/sync', '/api/sync', '/sync', '/api/v1/inventory', '/api/v1/purchases', '/api/v1/suppliers']
      }), {
        status: 200,
        headers: { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' }
      });
    }

    // 3. Main Real-Time Sync Endpoint (/api/v1/sync, /api/sync, /sync)
    const isSyncRoute = cleanPath === '/api/v1/sync' ||
                        cleanPath === '/api/sync' ||
                        cleanPath === '/sync' ||
                        cleanPath.startsWith('/api/v1/sync');

    if (isSyncRoute) {
      if (request.method === 'POST') {
        try {
          const incoming = await request.json();

          // Fetch existing database state
          let current = inMemorySyncCache;
          if (kv) {
            const raw = await kv.get('lakshana_db_v1');
            if (raw) {
              try { current = JSON.parse(raw); } catch (e) { current = inMemorySyncCache; }
            }
          }

          // Bidirectional Merge Logic
          const incomingItems = incoming.items || incoming.inventory || [];
          const incomingPurchases = incoming.purchases || [];
          const incomingSuppliers = incoming.suppliers || [];

          // Merge Purchases (deduplicate by PO number / batchId / id)
          const purchaseMap = new Map();
          (current.purchases || []).forEach(p => {
            const key = p.poNumber || p.batchId || p.id || `${p.date}_${p.name || p.itemName}_${p.qtyKgs}`;
            purchaseMap.set(key, p);
          });
          incomingPurchases.forEach(p => {
            const key = p.poNumber || p.batchId || p.id || `${p.date}_${p.name || p.itemName}_${p.qtyKgs}`;
            purchaseMap.set(key, p);
          });
          const mergedPurchases = Array.from(purchaseMap.values());

          // Merge Inventory Items (deduplicate by item name)
          const itemMap = new Map();
          (current.items || []).forEach(i => {
            const key = (i.name || '').trim().toLowerCase();
            if (key) itemMap.set(key, i);
          });
          incomingItems.forEach(i => {
            const key = (i.name || '').trim().toLowerCase();
            if (key) {
              const existing = itemMap.get(key);
              if (existing) {
                itemMap.set(key, {
                  ...existing,
                  ...i,
                  stockKgs: i.stockKgs !== undefined ? i.stockKgs : (i.currentStockKgs !== undefined ? i.currentStockKgs : existing.stockKgs),
                  boxes: i.boxes !== undefined ? i.boxes : (i.totalBoxes !== undefined ? i.totalBoxes : existing.boxes)
                });
              } else {
                itemMap.set(key, i);
              }
            }
          });
          const mergedItems = Array.from(itemMap.values());

          // Merge Suppliers (deduplicate by supplier name)
          const supplierMap = new Map();
          (current.suppliers || []).forEach(s => {
            const key = (s.name || '').trim().toLowerCase();
            if (key) supplierMap.set(key, s);
          });
          incomingSuppliers.forEach(s => {
            const key = (s.name || '').trim().toLowerCase();
            if (key) supplierMap.set(key, s);
          });
          const mergedSuppliers = Array.from(supplierMap.values());

          const updatedDatabase = {
            items: mergedItems,
            purchases: mergedPurchases,
            suppliers: mergedSuppliers,
            version: 2,
            syncedAt: Date.now(),
            serverTime: new Date().toISOString(),
            lastModifiedBy: incoming.deviceSource || 'Lakshana Mobile/Web Client'
          };

          // Save to Persistent KV
          if (kv) {
            await kv.put('lakshana_db_v1', JSON.stringify(updatedDatabase));
            await kv.put('last_sync_timestamp', Date.now().toString());
          } else {
            inMemorySyncCache = updatedDatabase;
          }

          return new Response(JSON.stringify({
            status: 'success',
            syncedAt: updatedDatabase.syncedAt,
            serverTime: updatedDatabase.serverTime,
            items: updatedDatabase.items,
            purchases: updatedDatabase.purchases,
            suppliers: updatedDatabase.suppliers,
            itemsCount: mergedItems.length,
            purchasesCount: mergedPurchases.length,
            suppliersCount: mergedSuppliers.length,
            storage: kv ? 'Persistent Cloudflare KV' : 'In-Memory Cache',
            domain: url.hostname
          }), {
            status: 200,
            headers: {
              'Content-Type': 'application/json',
              'Access-Control-Allow-Origin': '*'
            }
          });
        } catch (err) {
          return new Response(JSON.stringify({
            status: 'error',
            message: 'Sync payload parse error: ' + err.message
          }), {
            status: 400,
            headers: { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' }
          });
        }
      }

      // GET request: Return current server database state
      let data = inMemorySyncCache;
      if (kv) {
        const raw = await kv.get('lakshana_db_v1');
        if (raw) {
          try { data = JSON.parse(raw); } catch (e) { data = inMemorySyncCache; }
        }
      }

      return new Response(JSON.stringify({
        status: 'online',
        endpoint: cleanPath,
        domain: url.hostname || 'lakshanaveggie.trade',
        storage: kv ? 'Persistent Cloudflare KV' : 'In-Memory Edge Cache',
        syncedAt: data.syncedAt || Date.now(),
        items: data.items || [],
        purchases: data.purchases || [],
        suppliers: data.suppliers || [],
        itemsCount: (data.items || []).length,
        purchasesCount: (data.purchases || []).length,
        suppliersCount: (data.suppliers || []).length
      }), {
        status: 200,
        headers: { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' }
      });
    }

    // 4. Granular REST Endpoints
    if (cleanPath === '/api/v1/inventory') {
      let data = inMemorySyncCache;
      if (kv) {
        const raw = await kv.get('lakshana_db_v1');
        if (raw) try { data = JSON.parse(raw); } catch (e) {}
      }
      return new Response(JSON.stringify(data.items || []), {
        status: 200,
        headers: { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' }
      });
    }

    if (cleanPath === '/api/v1/purchases') {
      let data = inMemorySyncCache;
      if (kv) {
        const raw = await kv.get('lakshana_db_v1');
        if (raw) try { data = JSON.parse(raw); } catch (e) {}
      }
      return new Response(JSON.stringify(data.purchases || []), {
        status: 200,
        headers: { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' }
      });
    }

    // 5. Cloudflare Pages Static Assets Handler
    if (env && env.ASSETS && typeof env.ASSETS.fetch === 'function') {
      const assetRes = await env.ASSETS.fetch(request);
      if (assetRes.status !== 404) {
        return assetRes;
      }
      // If 404 on root or html route, attempt fetching /index.html
      const indexReq = new Request(new URL('/index.html', request.url), request);
      const indexRes = await env.ASSETS.fetch(indexReq);
      if (indexRes.status !== 404) {
        return indexRes;
      }
    }

    // Direct HTML response so browser always shows the Mandi portal content
    return new Response(`<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Lakshana Veggie - Online Mandi Portal</title>
  <meta http-equiv="refresh" content="1; url=/index.html">
  <style>
    body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; background: #0f172a; color: #f8fafc; display: flex; align-items: center; justify-content: center; min-height: 100vh; margin: 0; }
    .card { background: #1e293b; border: 1px solid #334155; padding: 2.5rem; border-radius: 16px; max-width: 480px; text-align: center; box-shadow: 0 20px 25px -5px rgba(0,0,0,0.5); }
    .emoji { font-size: 3rem; margin-bottom: 0.5rem; }
    h1 { margin: 0 0 0.5rem 0; font-size: 1.5rem; color: #38bdf8; }
    p { color: #94a3b8; font-size: 0.95rem; line-height: 1.5; margin: 0.5rem 0 1.5rem 0; }
    .btn { display: inline-block; background: #10b981; color: #ffffff; text-decoration: none; padding: 0.75rem 1.5rem; border-radius: 10px; font-weight: 700; font-size: 1rem; transition: background 0.2s; }
    .btn:hover { background: #059669; }
    .badge { display: inline-block; background: rgba(16, 185, 129, 0.15); color: #10b981; font-size: 0.75rem; font-weight: 700; padding: 0.25rem 0.75rem; border-radius: 9999px; margin-bottom: 1rem; }
  </style>
</head>
<body>
  <div class="card">
    <div class="emoji">🥕</div>
    <div class="badge">Cloudflare Edge Online</div>
    <h1>LAKSHANA VEGGIE</h1>
    <p>Direct Mandi Procurement & Live Inventory Management System.<br>Domain: <strong>lakshanaveggie.trade</strong></p>
    <a href="/index.html" class="btn">Open Web Portal Dashboard</a>
  </div>
</body>
</html>`, {
      status: 200,
      headers: { 'Content-Type': 'text/html; charset=utf-8' }
    });
  }
};
