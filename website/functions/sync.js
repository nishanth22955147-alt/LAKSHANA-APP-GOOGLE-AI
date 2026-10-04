// Cloudflare Pages Function: /api/v1/sync
// Persistent Online Backend Storage with Smart Merging

let inMemorySyncCache = {
  items: [],
  purchases: [],
  suppliers: [],
  version: 2,
  syncedAt: Date.now()
};

export async function onRequestOptions() {
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

export async function onRequestPost({ request, env }) {
  try {
    const incoming = await request.json();
    const kv = (env && (env.SYNC_KV || env.LAKSHANA_KV || env.STORAGE || env.DATABASE));

    // Read current state
    let current = inMemorySyncCache;
    if (kv) {
      const raw = await kv.get('lakshana_db_v1');
      if (raw) {
        try { current = JSON.parse(raw); } catch (e) { current = inMemorySyncCache; }
      }
    }

    // Merge Purchases (deduplicate by PO number / id)
    const purchaseMap = new Map();
    (current.purchases || []).forEach(p => {
      const key = p.poNumber || p.batchId || p.id || `${p.date}_${p.name}_${p.qtyKgs}`;
      purchaseMap.set(key, p);
    });
    (incoming.purchases || []).forEach(p => {
      const key = p.poNumber || p.batchId || p.id || `${p.date}_${p.name}_${p.qtyKgs}`;
      purchaseMap.set(key, p);
    });
    const mergedPurchases = Array.from(purchaseMap.values());

    // Merge Items (deduplicate by name)
    const itemMap = new Map();
    (current.items || []).forEach(i => {
      const key = (i.name || '').trim().toLowerCase();
      if (key) itemMap.set(key, i);
    });
    (incoming.items || incoming.inventory || []).forEach(i => {
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
    (incoming.suppliers || []).forEach(s => {
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
      domain: 'lakshanaveggie.trade'
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
      message: 'Invalid sync payload: ' + err.message
    }), {
      status: 400,
      headers: {
        'Content-Type': 'application/json',
        'Access-Control-Allow-Origin': '*'
      }
    });
  }
}

export async function onRequestGet({ env }) {
  const kv = (env && (env.SYNC_KV || env.LAKSHANA_KV || env.STORAGE || env.DATABASE));
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
    storage: kv ? 'Persistent Cloudflare KV (Active)' : 'In-Memory Cache (KV not bound yet)',
    timestamp: Date.now(),
    items: data.items || [],
    purchases: data.purchases || [],
    suppliers: data.suppliers || [],
    itemsCount: (data.items || []).length,
    purchasesCount: (data.purchases || []).length,
    suppliersCount: (data.suppliers || []).length
  }), {
    status: 200,
    headers: {
      'Content-Type': 'application/json',
      'Access-Control-Allow-Origin': '*'
    }
  });
}
