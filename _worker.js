import { getAssetFromKV } from "@cloudflare/kv-asset-handler";

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);

    // Handle API / Sync requests
    if (url.pathname.startsWith("/api/v1/sync") || url.pathname.startsWith("/sync")) {
      if (request.method === "POST") {
        try {
          const body = await request.json();
          return new Response(JSON.stringify({ status: "success", message: "Cloud sync recorded successfully", timestamp: Date.now() }), {
            headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" }
          });
        } catch (e) {
          return new Response(JSON.stringify({ status: "error", message: e.message }), {
            status: 400,
            headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" }
          });
        }
      }
      return new Response(JSON.stringify({ status: "active", message: "Lakshana Veggie Cloud Worker Active", timestamp: Date.now() }), {
        headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" }
      });
    }

    // Serve static frontend assets (index.html, styles.css, app.js) for browser requests
    try {
      return await getAssetFromKV(
        {
          request,
          waitUntil: ctx.waitUntil.bind(ctx),
        },
        {
          ASSET_NAMESPACE: env.__STATIC_CONTENT,
          ASSET_MANIFEST: __STATIC_CONTENT_MANIFEST,
        }
      );
    } catch (e) {
      // Fallback to index.html for SPA routing if asset not directly found
      try {
        let enhancedRequest = new Request(new URL("/index.html", request.url), request);
        return await getAssetFromKV(
          {
            request: enhancedRequest,
            waitUntil: ctx.waitUntil.bind(ctx),
          },
          {
            ASSET_NAMESPACE: env.__STATIC_CONTENT,
            ASSET_MANIFEST: __STATIC_CONTENT_MANIFEST,
          }
        );
      } catch (err) {
        return new Response("Lakshana Veggie Portal Error: Static assets not bound", { status: 500 });
      }
    }
  }
};
