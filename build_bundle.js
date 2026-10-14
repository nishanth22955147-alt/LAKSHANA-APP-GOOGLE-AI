const fs = require('fs');

const html = fs.readFileSync('index.html', 'utf8');
const css = fs.readFileSync('styles.css', 'utf8');
const js = fs.readFileSync('app.js', 'utf8');

// Replace stylesheet link with inline style
let bundled = html.replace('<link rel="stylesheet" href="styles.css" />', `<style>\n${css}\n</style>`);
// Replace app.js script tag with inline script
bundled = bundled.replace('<script src="app.js"></script>', `<script>\n${js}\n</script>`);

fs.writeFileSync('website/bundle.html', bundled, 'utf8');
console.log('Built website/bundle.html successfully, size:', bundled.length);
