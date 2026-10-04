/**
 * Cloudflare Worker for lakshanaveggie.trade
 * Handles real-time Android Mobile App & Web Portal synchronization
 * 
 * SETUP IN CLOUDFLARE DASHBOARD:
 * 1. Go to Workers & Pages -> Create Application -> Create Worker
 * 2. Paste this entire code into the worker editor and click "Deploy"
 * 3. Go to Worker Settings -> Domains & Routes -> Add Custom Domain or Route:
 *    Route: *lakshanaveggie.trade/api/* (or Custom Domain: api.lakshanaveggie.trade)
 * 4. Done! Online sync will now return HTTP 200 OK.
 */

// In-memory cache fallback (persists across warm worker instances)
let inMemorySyncCache = null;

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);

    // Handle CORS preflight options
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

    // Match the API sync endpoint
    if (url.pathname === '/api/v1/sync' || url.pathname === '/sync') {
      if (request.method === 'POST') {
        try {
          const payload = await request.json();
          const itemsCount = (payload.items || payload.inventory || []).length;
          const purchasesCount = (payload.purchases || []).length;
          const suppliersCount = (payload.suppliers || []).length;

          const syncRecord = {
            ...payload,
            syncedAt: Date.now(),
            serverTime: new Date().toISOString()
          };

          // Save to Cloudflare KV if bound (e.g. SYNC_KV)
          if (env && env.SYNC_KV) {
            await env.SYNC_KV.put('latest_sync', JSON.stringify(syncRecord));
          } else {
            inMemorySyncCache = syncRecord;
          }

          return new Response(JSON.stringify({
            status: 'success',
            syncedAt: Date.now(),
            serverTime: new Date().toISOString(),
            itemsCount,
            purchasesCount,
            suppliersCount,
            message: 'Real-time sync successful with Cloudflare Worker',
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

      // GET request: Return latest synced data or status
      let data = inMemorySyncCache;
      if (env && env.SYNC_KV) {
        const raw = await env.SYNC_KV.get('latest_sync');
        if (raw) data = JSON.parse(raw);
      }

      return new Response(JSON.stringify(data || {
        status: 'online',
        endpoint: '/api/v1/sync',
        domain: 'lakshanaveggie.trade',
        provider: 'Cloudflare Edge Worker',
        timestamp: Date.now(),
        message: 'Sync API is ready to accept mobile & web procurement payloads'
      }), {
        status: 200,
        headers: {
          'Content-Type': 'application/json',
          'Access-Control-Allow-Origin': '*'
        }
      });
    }

    // If request is not for /api/v1/sync, proxy through to the origin (e.g. GitHub Pages)
    return fetch(request);
  }
};
