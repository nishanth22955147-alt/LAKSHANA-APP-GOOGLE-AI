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
  items: [],
  purchases: [],
  suppliers: [],
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

    // 5. Cloudflare Pages Static Assets Handler (index.html, styles.css, app.js, etc.)
    if (env && env.ASSETS && typeof env.ASSETS.fetch === 'function') {
      return env.ASSETS.fetch(request);
    }

    // Fallback if not inside Cloudflare Pages
    return new Response('Lakshana Veggie Cloud Server Online', {
      status: 200,
      headers: { 'Content-Type': 'text/plain' }
    });
  }
};
