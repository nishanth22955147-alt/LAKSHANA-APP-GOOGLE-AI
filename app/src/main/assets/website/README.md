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

## 🛠️ Step-by-Step DNS Records for `lakshanaveggie.trade`

Log in to where you bought your domain (GoDaddy, Namecheap, Cloudflare, Hostinger), go to **DNS Management**, and configure:

| Type | Name / Host | Value / Target | TTL |
|---|---|---|---|
| **A** | `@` (or leave blank) | `76.76.21.21` (or your server IP) | Auto / 3600 |
| **CNAME** | `www` | `cname.vercel-dns.com` (or `lakshanaveggie.trade`) | Auto / 3600 |

*Important: Delete any old default `A` records pointing to parking pages (e.g. GoDaddy parking page IP).*

---

## 📱 Connecting Your Android App to Your Custom Domain

Once your domain is live:
1. Open the **Lakshana Veggie** Android App.
2. Tap the **Website / ⚡ Web Sync & Backup** tab.
3. Under **Custom Domain & Cloud Endpoint**, select or type:
   ```text
   https://lakshanaveggie.trade
   ```
4. Tap **Test Ping** (verifies connection).
5. Tap **Save URL** and press **Synchronize with Website Now**.

Your Android Room database and Web Portal are now fully linked through your custom domain!
