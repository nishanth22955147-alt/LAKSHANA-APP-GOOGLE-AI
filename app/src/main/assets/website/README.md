# Connecting Your Custom Domain (lakshanaveggie.trade) & Fixing "Page Not Found" (404)

This guide provides step-by-step instructions to connect your custom domain **`https://lakshanaveggie.trade`** to your web portal and Android application.

---

## 🚨 Why Does "Page Not Found" (404) Show Up?

When you open your domain and see **"Page Not Found"**, it is caused by one of these reasons:

| Cause | What Happened | How to Fix |
|---|---|---|
| **1. DNS Records Still Propagating** | DNS changes take between **5 minutes to 2 hours** (sometimes up to 24 hours) for global DNS servers to update. | Wait 15–30 minutes, or flush DNS cache with `ipconfig /flushdns`. |
| **2. Domain Not Claimed in Host** | You set DNS in GoDaddy/Namecheap, but didn't claim the domain in your hosting dashboard. The server receives the request, but doesn't know which app it belongs to. | Add the custom domain in your Hosting Dashboard. |
| **3. SSL Certificate Provisioning** | Google / Let's Encrypt provisions a free SSL (HTTPS) certificate. During this initial setup (15–60 mins), HTTPS requests may display a temporary error or pending page. | Wait for the SSL certificate status to turn active. |
| **4. Deployed from Wrong Directory** | The web files (`index.html`, `styles.css`, `app.js`) are inside `/website`. If you deployed root without setting the public directory to `website`, the host serves nothing. | Ensure the root or public folder is configured as `website`. |

---

## 🛠️ Step-by-Step DNS & Cloudflare Worker for `lakshanaveggie.trade`

Your domain is managed on **Cloudflare**. To sync your mobile app and website:

1. **Website Hosting**: Your website files (`index.html`, `styles.css`, `app.js`) are served directly on your custom domain via GitHub Pages or Cloudflare Pages with `CNAME lakshanaveggie.trade`.
2. **Cloud Sync Engine**: The `cloudflare-worker.js` script handles real-time synchronization between the web portal and Android phone:
   - Go to Cloudflare Dashboard ➔ **Workers & Pages** ➔ **Create Worker**
   - Paste the code from `cloudflare-worker.js`
   - Click **Deploy**
   - Under Worker **Settings** ➔ **Domains & Routes**, add route `*lakshanaveggie.trade/api/*` (or add Custom Domain `sync.lakshanaveggie.trade`)

---

## 📱 Connecting Your Android App to Cloudflare Sync

1. Open the **Lakshana Veggie** Android App.
2. Tap the **Website / ⚡ Web Sync & Backup** tab (or **Online Sync Hub** in header).
3. Under **Cloud Endpoint**, verify or set:
   ```text
   https://lakshanaveggie.trade/api/v1/sync
   ```
4. Tap **Test Ping** (verifies connection).
5. Tap **Synchronize Now**. All records are synchronized in real-time!
