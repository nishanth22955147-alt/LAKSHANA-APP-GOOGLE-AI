export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);
    if (url.pathname.startsWith("/api/v1/sync")) {
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
    return new Response("Lakshana Veggie Web Portal & Sync API", {
      headers: { "Content-Type": "text/plain", "Access-Control-Allow-Origin": "*" }
    });
  }
};
