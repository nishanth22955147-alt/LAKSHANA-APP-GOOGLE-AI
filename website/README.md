# Lakshana Veggie Express - Online Web Portal

This is the official Web Portal for **Lakshana Veggie Express**, synchronized directly with the Android mobile application.

## Key Features
- **Online Synchronize Hub**: Two-way synchronization with Android Room database (Purchases, Inventory, Suppliers, Daily Sheets).
- **Daily Sheet Bulk Upload**: Direct Excel (.xlsx / .csv) and Mandi Bill Image table extraction.
- **Auto-Calculated Totals**: Real-time calculation of `Total Amount = Qty(kgs) * Rate per kg`.
- **Live Inventory Ledger**: Instant stock levels in kgs and crates/boxes with automated increment upon intake.
- **Mandi Supplier Directory**: Track partner farmers, APMC yard merchants, and outstanding balances.
- **Printable Manifests & Invoices**: Clean printable purchase vouchers for Mandi settlement.

## Quick Hosting Deployment

## How to Fix: "Site Not Found / You haven't deployed an app yet / Setting up custom domain"

If you see the message:
> **"There are a few potential reasons: You haven't deployed an app yet. You may have deployed an empty directory. This is a custom domain, but we haven't finished setting it up yet."**

Here is exactly how to resolve each cause:

### Cause 1: You haven't deployed the files yet
Firebase Hosting needs you to upload the files from this `website/` directory.
Run in your project root:
```bash
# 1. Install Firebase CLI (if not installed)
npm install -g firebase-tools

# 2. Login to your Google / Firebase account
firebase login

# 3. Select your Firebase project
firebase use --add

# 4. Deploy only hosting
firebase deploy --only hosting
```
The newly created `firebase.json` is pre-configured with `"public": "website"`.

### Cause 2: Deployed empty directory
Make sure your `firebase.json` points to `"website"`:
```json
{
  "hosting": {
    "public": "website",
    "rewrites": [{ "source": "**", "destination": "/index.html" }]
  }
}
```
Do NOT use `"public": "public"` if your HTML files are located in `website/`.

### Cause 3: Custom Domain is still setting up (SSL / DNS Verification)
When connecting a custom domain in Firebase Hosting or Cloudflare:
1. **DNS Verification**: Ensure the two `A` records (IP addresses) or the `CNAME` provided by Firebase are added in your DNS management (GoDaddy, Namecheap, Cloudflare).
2. **SSL Certificate Provisioning**: Firebase automatically requests a Let's Encrypt SSL certificate once DNS records are detected. This status will show **"Needs setup"** or **"Pending"** in the Firebase Console and takes between **15 minutes and 2 hours** to become active.
3. Once the status turns to **"Connected"** (green checkmark in Firebase Hosting Console), your custom domain will instantly display the live Lakshana Veggie Express portal.

### 2. GitHub Pages
1. Push this `website/` folder to your GitHub repository.
2. Under **Settings > Pages**, select `main` branch and `/website` folder.
3. Your web portal is live at `https://<your-username>.github.io/<repo-name>`.

### 3. Netlify / Vercel
Drag and drop the `website` folder into [Netlify Drop](https://app.netlify.com/drop) to get a free live URL in 10 seconds.
