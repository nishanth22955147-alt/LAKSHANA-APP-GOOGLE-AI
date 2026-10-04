let inMemorySyncCache = null;

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);

    if (request.method === 'OPTIONS') {
      return new Response(null, {
        status: 204,
        headers: {
          'Access-Control-Allow-Origin': '*',
          'Access-Control-Allow-Methods': 'GET, POST, OPTIONS, PUT, DELETE',
          'Access-Control-Allow-Headers': 'Content-Type, Authorization',
          'Access-Control-Max-Age': '86400'
        }
      });
    }

    if (url.pathname === '/api/v1/sync' || url.pathname === '/sync') {
      if (request.method === 'POST') {
        const payload = await request.json();
        inMemorySyncCache = {
          ...payload,
          syncedAt: Date.now(),
          serverTime: new Date().toISOString()
        };

        return new Response(JSON.stringify({
          status: 'success',
          syncedAt: Date.now(),
          items: inMemorySyncCache.items || inMemorySyncCache.inventory || [],
          purchases: inMemorySyncCache.purchases || [],
          suppliers: inMemorySyncCache.suppliers || [],
          message: 'Real-time sync successful with Cloudflare Worker'
        }), {
          status: 200,
          headers: {
            'Content-Type': 'application/json',
            'Access-Control-Allow-Origin': '*'
          }
        });
      }

      return new Response(JSON.stringify(inMemorySyncCache || {
        status: 'online',
        domain: 'lakshanaveggie.trade',
        timestamp: Date.now()
      }), {
        status: 200,
        headers: {
          'Content-Type': 'application/json',
          'Access-Control-Allow-Origin': '*'
        }
      });
    }

    return fetch(request);
  }
};