/**
 * Cloudflare Worker: Lakshana Veggie Real-Time Backend & Storage Hub
 * Domain: lakshanaveggie.trade
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
        serverTime: new Date().toISOString()
      }), {
        status: 200,
        headers: { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' }
      });
    }

    // 3. Main Real-Time Sync Endpoint
    if (cleanPath === '/api/v1/sync' || cleanPath === '/sync' || cleanPath === '/api/sync' || cleanPath.startsWith('/api/v1/sync')) {
      if (request.method === 'POST') {
        try {
          const incoming = await request.json();

          let current = inMemorySyncCache;
          if (kv) {
            const raw = await kv.get('lakshana_db_v1');
            if (raw) {
              try { current = JSON.parse(raw); } catch (e) { current = inMemorySyncCache; }
            }
          }

          const incomingItems = incoming.items || incoming.inventory || [];
          const incomingPurchases = incoming.purchases || [];
          const incomingSuppliers = incoming.suppliers || [];

          // Merge Purchases
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

          // Merge Items
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
                itemMap.set(key, { ...existing, ...i });
              } else {
                itemMap.set(key, i);
              }
            }
          });
          const mergedItems = Array.from(itemMap.values());

          // Merge Suppliers
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
            serverTime: new Date().toISOString()
          };

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
            storage: kv ? 'Persistent Cloudflare KV' : 'In-Memory Cache'
          }), {
            status: 200,
            headers: { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' }
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

      // GET request
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
        suppliers: data.suppliers || []
      }), {
        status: 200,
        headers: { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' }
      });
    }

    if (env && env.ASSETS && typeof env.ASSETS.fetch === 'function') {
      return env.ASSETS.fetch(request);
    }

    return new Response('Lakshana Veggie Cloud Server Online', {
      status: 200,
      headers: { 'Content-Type': 'text/plain' }
    });
  }
};
