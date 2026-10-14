const fs = require('fs');

const bundleHtml = fs.readFileSync('website/bundle.html', 'utf8');
const workerSrc = fs.readFileSync('_worker.js', 'utf8');

const marker = '    // Direct HTML response so browser always shows the Mandi portal content';
const splitParts = workerSrc.split(marker);

if (splitParts.length < 2) {
  console.error('Marker not found');
  process.exit(1);
}

// Convert bundleHtml to JSON string literal to avoid any escaping issues
const jsonString = JSON.stringify(bundleHtml);

const newTail = marker + '\n' +
'    return new Response(' + jsonString + ', {\n' +
'      status: 200,\n' +
'      headers: { \'Content-Type\': \'text/html; charset=utf-8\', \'Access-Control-Allow-Origin\': \'*\' }\n' +
'    });\n' +
'  }\n};\n';

const updated = splitParts[0] + newTail;
fs.writeFileSync('_worker.js', updated, 'utf8');
fs.writeFileSync('website/_worker.js', updated, 'utf8');
fs.writeFileSync('cloudflare-worker.js', updated, 'utf8');
fs.writeFileSync('website/cloudflare-worker.js', updated, 'utf8');
fs.writeFileSync('app/src/main/assets/website/cloudflare-worker.js', updated, 'utf8');
console.log('Successfully injected bundle into all worker targets! Length:', updated.length);
