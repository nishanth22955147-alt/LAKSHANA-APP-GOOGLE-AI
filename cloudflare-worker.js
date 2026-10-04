/**
 * Cloudflare Worker: Lakshana Veggie Real-Time Backend & Storage Hub
 * Domain: lakshanaveggie.trade
 * 
 * Provides:
 * 1. Persistent Cloud KV Database Storage across Android Mobile App & Web Portal
 * 2. Real-Time Bidirectional Synchronization & Record Merging
 * 3. Fallback Web Portal Landing & Health Diagnostic API
 */

// In-memory cache fallback (used if KV namespace is not yet bound)
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

    // 1. CORS Preflight Handlers
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

    // Helper: Get active KV binding if configured in Cloudflare Dashboard
    const kv = (env && (env.SYNC_KV || env.LAKSHANA_KV || env.STORAGE || env.DATABASE));

    // 2. Health & Diagnostics Endpoint
    if (cleanPath === '/api/v1/health' || cleanPath === '/health') {
      return new Response(JSON.stringify({
        status: 'online',
        service: 'Lakshana Veggie Procurement Backend',
        domain: 'lakshanaveggie.trade',
        storageType: kv ? 'Cloudflare KV (Persistent Global Storage)' : 'In-Memory Edge Cache (KV not bound)',
        kvConfigured: !!kv,
        timestamp: Date.now(),
        serverTime: new Date().toISOString(),
        activeEndpoints: ['/api/v1/sync', '/api/v1/inventory', '/api/v1/purchases', '/api/v1/suppliers']
      }), {
        status: 200,
        headers: { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' }
      });
    }

    // 3. Main Real-Time Sync Endpoint (/api/v1/sync or /sync)
    if (cleanPath === '/api/v1/sync' || cleanPath === '/sync' || cleanPath.startsWith('/api/v1/sync')) {
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

          // Smart Bidirectional Merge Logic
          const incomingItems = incoming.items || incoming.inventory || [];
          const incomingPurchases = incoming.purchases || [];
          const incomingSuppliers = incoming.suppliers || [];

          // Merge Purchases (deduplicate by PO number / batchId / id)
          const purchaseMap = new Map();
          (current.purchases || []).forEach(p => {
            const key = p.poNumber || p.batchId || p.id || `${p.date}_${p.name}_${p.qtyKgs}`;
            purchaseMap.set(key, p);
          });
          incomingPurchases.forEach(p => {
            const key = p.poNumber || p.batchId || p.id || `${p.date}_${p.name}_${p.qtyKgs}`;
            purchaseMap.set(key, p); // newer incoming record
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
                  stockKgs: i.stockKgs !== undefined ? i.stockKgs : existing.stockKgs,
                  boxes: i.boxes !== undefined ? i.boxes : existing.boxes
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
            // Keep latest backup timestamp
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
            storage: kv ? 'Persistent Cloudflare KV' : 'In-Memory Cache'
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
        endpoint: '/api/v1/sync',
        domain: 'lakshanaveggie.trade',
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

    // 5. Default Route (e.g. root / or web portal)
    // If an origin exists, proxy through. Otherwise, return status & connection instructions.
    try {
      const originResponse = await fetch(request);
      if (originResponse.status !== 404) {
        return originResponse;
      }
    } catch (e) {
      // No origin configured, return web portal status
    }

    return new Response(`<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Lakshana Veggie Server • lakshanaveggie.trade</title>
  <style>
    body { font-family: system-ui, -apple-system, sans-serif; background: #f0fdf4; color: #14532d; display: flex; align-items: center; justify-content: center; min-height: 100vh; margin: 0; padding: 1.5rem; }
    .card { background: white; border-radius: 16px; padding: 2rem; max-width: 520px; box-shadow: 0 10px 25px rgba(0,0,0,0.08); border: 1px solid #bbf7d0; text-align: center; }
    .badge { display: inline-block; background: #dcfce7; color: #15803d; padding: 0.25rem 0.75rem; border-radius: 9999px; font-weight: 700; font-size: 0.8rem; margin-bottom: 1rem; }
    h1 { margin: 0 0 0.5rem 0; font-size: 1.5rem; color: #166534; }
    p { color: #475569; font-size: 0.9rem; line-height: 1.5; margin: 0 0 1.25rem 0; }
    .endpoint { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 0.75rem; font-family: monospace; font-size: 0.85rem; color: #0f172a; word-break: break-all; margin-bottom: 1.25rem; }
    .btn { display: inline-block; background: #16a34a; color: white; padding: 0.75rem 1.5rem; border-radius: 8px; text-decoration: none; font-weight: 700; font-size: 0.9rem; }
    .btn:hover { background: #15803d; }
  </style>
</head>
<body>
  <div class="card">
    <div class="badge">🟢 BACKEND SERVER ACTIVE</div>
    <h1>Lakshana Veggie Backend</h1>
    <p>Persistent Cloudflare Server & Real-Time Sync API is running live at <strong>lakshanaveggie.trade</strong>.</p>
    <div class="endpoint">API: https://lakshanaveggie.trade/api/v1/sync</div>
    <a href="/api/v1/health" class="btn">View System Health & Storage Status</a>
  </div>
</body>
</html>`, {
      status: 200,
      headers: { 'Content-Type': 'text/html; charset=utf-8' }
    });
  }
};
