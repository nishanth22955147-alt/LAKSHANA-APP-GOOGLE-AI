// Cloudflare Pages Function: /api/v1/sync
// Automatically runs when deployed on Cloudflare Pages (lakshanaveggie.trade)

export async function onRequestOptions() {
  return new Response(null, {
    status: 204,
    headers: {
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
      'Access-Control-Allow-Headers': 'Content-Type, Authorization'
    }
  });
}

export async function onRequestPost({ request, env }) {
  try {
    const payload = await request.json();
    const itemsCount = (payload.items || payload.inventory || []).length;
    const purchasesCount = (payload.purchases || []).length;
    const suppliersCount = (payload.suppliers || []).length;

    // If Cloudflare KV is bound (e.g. env.SYNC_KV), store it in KV
    if (env && env.SYNC_KV) {
      await env.SYNC_KV.put('latest_sync', JSON.stringify({
        ...payload,
        syncedAt: Date.now(),
        serverTime: new Date().toISOString()
      }));
    }

    return new Response(JSON.stringify({
      status: 'success',
      syncedAt: Date.now(),
      serverTime: new Date().toISOString(),
      itemsCount,
      purchasesCount,
      suppliersCount,
      message: 'Real-time sync successful with Cloudflare on lakshanaveggie.trade',
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
  let cached = null;
  if (env && env.SYNC_KV) {
    const raw = await env.SYNC_KV.get('latest_sync');
    if (raw) {
      cached = JSON.parse(raw);
    }
  }

  return new Response(JSON.stringify(cached || {
    status: 'online',
    endpoint: '/api/v1/sync',
    domain: 'lakshanaveggie.trade',
    provider: 'Cloudflare Pages Functions',
    timestamp: Date.now(),
    items: [],
    purchases: [],
    suppliers: [],
    users: []
  }), {
    status: 200,
    headers: {
      'Content-Type': 'application/json',
      'Access-Control-Allow-Origin': '*'
    }
  });
}
