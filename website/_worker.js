export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);
    if (url.pathname.startsWith("/api/v1/sync") || url.pathname.startsWith("/sync")) {
      return new Response(JSON.stringify({ status: "success", message: "Cloud sync active" }), {
        headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" }
      });
    }
    return new Response("Lakshana Veggie Web Portal Active", {
      headers: { "Content-Type": "text/plain", "Access-Control-Allow-Origin": "*" }
    });
  }
};
