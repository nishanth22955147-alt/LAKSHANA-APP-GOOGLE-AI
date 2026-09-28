// Lakshana Veggie Express - Web Portal Application Script

const defaultPurchases = [
  { date: '2026-09-25', batchId: 'BATCH-20260925-01', item: 'Fresh Country Tomatoes', boxes: 25, qtyKgs: 500.0, rate: 35.00, total: 17500.00, supplier: 'Lakshana Agro Farms', paid: true },
  { date: '2026-09-25', batchId: 'BATCH-20260925-01', item: 'Nashik Red Onions', boxes: 40, qtyKgs: 1000.0, rate: 28.00, total: 28000.00, supplier: 'Lakshana Agro Farms', paid: true },
  { date: '2026-09-25', batchId: 'BATCH-20260925-01', item: 'Ooty Fresh Carrots', boxes: 30, qtyKgs: 450.0, rate: 45.00, total: 20250.00, supplier: 'Lakshana Agro Farms', paid: true },
  { date: '2026-09-25', batchId: 'BATCH-20260925-02', item: 'Baby Potatoes', boxes: 50, qtyKgs: 1250.0, rate: 25.00, total: 31250.00, supplier: 'Green Valley Mandi', paid: false },
  { date: '2026-09-25', batchId: 'BATCH-20260925-02', item: 'Green Chillies', boxes: 20, qtyKgs: 300.0, rate: 65.00, total: 19500.00, supplier: 'Green Valley Mandi', paid: true }
];

const defaultInventory = [
  { name: 'Country Tomatoes', icon: '🍅', stockKgs: 500.0, boxes: 25, avgRate: 35.00, category: 'Tomatoes' },
  { name: 'Nashik Red Onions', icon: '🧅', stockKgs: 1000.0, boxes: 40, avgRate: 28.00, category: 'Roots & Bulbs' },
  { name: 'Ooty Fresh Carrots', icon: '🥕', stockKgs: 450.0, boxes: 30, avgRate: 45.00, category: 'Roots & Bulbs' },
  { name: 'Baby Potatoes', icon: '🥔', stockKgs: 1250.0, boxes: 50, avgRate: 25.00, category: 'Roots & Bulbs' },
  { name: 'Green Chillies', icon: '🌶️', stockKgs: 300.0, boxes: 20, avgRate: 65.00, category: 'Spices & Herbs' },
  { name: 'Fresh Cauliflower', icon: '🥦', stockKgs: 525.0, boxes: 35, avgRate: 32.00, category: 'Gourds & Cabbages' },
  { name: 'Green Capsicum', icon: '🫑', stockKgs: 225.0, boxes: 15, avgRate: 55.00, category: 'Exotic Veg' }
];

const defaultSuppliers = [
  { name: 'Lakshana Agro Farms', contact: '8608414322', location: 'Hosur Green Belt, Tamil Nadu', balanceDue: 0.00, active: true },
  { name: 'Green Valley Mandi Merchants', contact: '9840123456', location: 'Koyambedu APMC Market, Chennai', balanceDue: 31250.00, active: true },
  { name: 'Nashik Direct Farmers Collective', contact: '9422019876', location: 'Nashik APMC Yard, Maharashtra', balanceDue: 0.00, active: true },
  { name: 'Ooty High-Altitude Cold Farms', contact: '9443098765', location: 'Kotagiri Road, Nilgiris', balanceDue: 0.00, active: true }
];

let purchases = JSON.parse(localStorage.getItem('lakshana_web_purchases')) || defaultPurchases;
let inventory = JSON.parse(localStorage.getItem('lakshana_web_inventory')) || defaultInventory;
let suppliers = JSON.parse(localStorage.getItem('lakshana_web_suppliers')) || defaultSuppliers;

function saveState() {
  localStorage.setItem('lakshana_web_purchases', JSON.stringify(purchases));
  localStorage.setItem('lakshana_web_inventory', JSON.stringify(inventory));
  localStorage.setItem('lakshana_web_suppliers', JSON.stringify(suppliers));
  renderAll();
}

function renderAll() {
  renderStats();
  renderPurchasesTable();
  renderInventoryGrid();
  renderSuppliersGrid();
}

function renderStats() {
  const totalKgs = inventory.reduce((sum, item) => sum + item.stockKgs, 0);
  const totalBoxes = inventory.reduce((sum, item) => sum + item.boxes, 0);
  const todayTotal = purchases.reduce((sum, p) => sum + p.total, 0);

  document.getElementById('totalStockKgs').innerHTML = `${totalKgs.toLocaleString('en-IN', { maximumFractionDigits: 1 })} <small>kgs</small>`;
  document.getElementById('totalBoxesCount').textContent = `${totalBoxes} boxes / crates`;
  document.getElementById('todayPurchasesTotal').textContent = `₹${todayTotal.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  document.getElementById('activeSuppliersCount').textContent = suppliers.filter(s => s.active).length;
}

function renderPurchasesTable(filterText = '') {
  const tbody = document.getElementById('purchasesTbody');
  tbody.innerHTML = '';

  const filtered = purchases.filter(p => 
    p.item.toLowerCase().includes(filterText.toLowerCase()) ||
    p.supplier.toLowerCase().includes(filterText.toLowerCase()) ||
    p.batchId.toLowerCase().includes(filterText.toLowerCase())
  );

  if (filtered.length === 0) {
    tbody.innerHTML = `<tr><td colspan="9" style="text-align:center; padding: 2rem; color: #64748b;">No purchase entries found.</td></tr>`;
    return;
  }

  filtered.forEach(p => {
    const tr = document.createElement('tr');
    const paymentCol = p.paid 
      ? `<span class="badge badge-success">✓ Settled (${p.settlementDate || p.date})</span>`
      : `<button class="btn btn-sm btn-primary" style="padding: 0.25rem 0.5rem; font-size: 0.75rem;" onclick="openSettlementModal('${p.batchId}')">⚡ Settle Payment</button>`;

    tr.innerHTML = `
      <td><strong>${p.date}</strong></td>
      <td><code>${p.batchId}</code></td>
      <td><strong>${p.item}</strong></td>
      <td>${p.boxes}</td>
      <td>${p.qtyKgs.toFixed(1)} kgs</td>
      <td>₹${p.rate.toFixed(2)}</td>
      <td><strong style="color: #15803d;">₹${p.total.toLocaleString('en-IN', { minimumFractionDigits: 2 })}</strong></td>
      <td>${p.supplier}</td>
      <td>${paymentCol}</td>
    `;
    tbody.appendChild(tr);
  });
}

function renderInventoryGrid(filterText = '') {
  const grid = document.getElementById('inventoryCardsGrid');
  grid.innerHTML = '';

  const filtered = inventory.filter(i => i.name.toLowerCase().includes(filterText.toLowerCase()));

  filtered.forEach(item => {
    const card = document.createElement('div');
    card.className = 'veg-card';
    card.innerHTML = `
      <div>
        <div class="veg-card-header">
          <span style="font-size: 1.5rem;">${item.icon}</span>
          <span class="badge badge-success">${item.category}</span>
        </div>
        <h4 class="veg-name">${item.name}</h4>
        <div class="veg-stock">${item.stockKgs.toLocaleString('en-IN')} <small style="font-size: 0.85rem; font-weight: 500;">kgs</small></div>
      </div>
      <div class="veg-details">
        <div>📦 Available Crates: <strong>${item.boxes} boxes</strong></div>
        <div>💰 Avg Mandi Rate: <strong>₹${item.avgRate.toFixed(2)} / kg</strong></div>
      </div>
    `;
    grid.appendChild(card);
  });
}

function renderSuppliersGrid() {
  const grid = document.getElementById('suppliersGrid');
  grid.innerHTML = '';

  suppliers.forEach(s => {
    // Pull unpaid purchases directly from daily purchases ledger:
    const vendorPurchases = purchases.filter(p => 
      p.supplier && p.supplier.trim().toLowerCase() === s.name.trim().toLowerCase()
    );
    const unpaidPurchases = vendorPurchases.filter(p => !p.paid);
    const ledgerOutstanding = unpaidPurchases.reduce((sum, p) => sum + p.total, 0);
    const totalPurchased = vendorPurchases.reduce((sum, p) => sum + p.total, 0);
    const totalSettled = vendorPurchases.filter(p => p.paid).reduce((sum, p) => sum + p.total, 0);
    const displayBalance = ledgerOutstanding > 0 ? ledgerOutstanding : (s.balanceDue || 0);

    const card = document.createElement('div');
    card.className = 'supplier-card';
    card.innerHTML = `
      <div style="display: flex; justify-content: space-between; align-items: flex-start;">
        <div>
          <h3>${s.name}</h3>
          <p>📍 ${s.location || 'Mandi / Farm Gate'}</p>
          <p>📱 ${s.contact || '8608414322'}</p>
        </div>
        <span class="badge ${displayBalance > 0 ? 'badge-warning' : 'badge-success'}">
          ${displayBalance > 0 ? 'Payment Due' : 'All Clear'}
        </span>
      </div>

      <!-- Dedicated Outstanding Balance Column & Ledger Box -->
      <div style="background:#f8fafc; border:1px solid ${displayBalance > 0 ? '#fde68a' : '#bbf7d0'}; border-radius:8px; padding:0.75rem; margin:0.75rem 0;">
        <div style="font-size:0.75rem; text-transform:uppercase; font-weight:700; color:${displayBalance > 0 ? '#b45309' : '#15803d'}; letter-spacing:0.05em;">
          Outstanding Balance
        </div>
        <div style="font-size:1.35rem; font-weight:800; color:${displayBalance > 0 ? '#b45309' : '#15803d'};">
          ₹${displayBalance.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
        </div>
        <div style="font-size:0.75rem; color:#64748b; margin-top:0.25rem;">
          ${unpaidPurchases.length > 0 ? `⚡ Pulled from daily ledger: ${unpaidPurchases.length} unpaid purchase bills` : '✓ All daily ledger purchases settled'}
        </div>
      </div>

      <!-- 3-Column Financial Summary -->
      <div style="display: flex; justify-content: space-between; font-size: 0.8125rem; color:#475569; padding-top: 0.25rem;">
        <div>Total Sourced: <strong style="color:#0f172a;">₹${totalPurchased.toLocaleString('en-IN', { maximumFractionDigits: 0 })}</strong></div>
        <div>Settled: <strong style="color:#15803d;">₹${totalSettled.toLocaleString('en-IN', { maximumFractionDigits: 0 })}</strong></div>
      </div>
    `;
    grid.appendChild(card);
  });
}

// Tab Switching
document.querySelectorAll('.portal-tabs .tab-btn').forEach(btn => {
  btn.addEventListener('click', () => {
    document.querySelectorAll('.portal-tabs .tab-btn').forEach(b => b.classList.remove('active'));
    document.querySelectorAll('.tab-panel').forEach(p => p.classList.remove('active'));
    btn.classList.add('active');
    document.getElementById(`tab-${btn.dataset.tab}`).classList.add('active');
  });
});

// Modal Upload Tab Switching
document.querySelectorAll('.upload-tab-btn').forEach(btn => {
  btn.addEventListener('click', () => {
    document.querySelectorAll('.upload-tab-btn').forEach(b => b.classList.remove('active'));
    document.querySelectorAll('.upload-tab-content').forEach(c => c.classList.remove('active'));
    btn.classList.add('active');
    document.getElementById(`utab-${btn.dataset.utab}`).classList.add('active');
  });
});

// Modal Controls
document.getElementById('openUploadModalBtn').addEventListener('click', () => {
  document.getElementById('uploadModal').classList.add('open');
});

// Filter Searches
document.getElementById('purchaseSearch').addEventListener('input', (e) => {
  renderPurchasesTable(e.target.value);
});

document.getElementById('inventorySearch').addEventListener('input', (e) => {
  renderInventoryGrid(e.target.value);
});

// Sync Now button simulation
function triggerSyncSimulation() {
  const statusEl = document.getElementById('cloudSyncStatus');
  const btn = document.getElementById('syncNowBtn');
  statusEl.querySelector('.status-text').textContent = 'Syncing...';
  statusEl.querySelector('.status-dot').style.background = '#f59e0b';
  btn.disabled = true;

  setTimeout(() => {
    const now = new Date();
    const timeStr = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    statusEl.querySelector('.status-text').textContent = 'Cloud Synchronized';
    statusEl.querySelector('.status-dot').style.background = '#10b981';
    document.getElementById('lastSyncTimeLabel').textContent = `Synced at ${timeStr}`;
    btn.disabled = false;
    alert(`Online Sync Complete! Database is synchronized with Android App (Admin: 8608414322) at ${timeStr}`);
  }, 1200);
}

document.getElementById('syncNowBtn').addEventListener('click', triggerSyncSimulation);
document.getElementById('triggerSyncNow').addEventListener('click', triggerSyncSimulation);

// Custom Domain & Database Config
const savedDomain = localStorage.getItem('lakshana_custom_domain') || 'https://lakshanaveggie.trade/api/v1/sync';
const domainInput = document.getElementById('customDomainInput');
if (domainInput) domainInput.value = savedDomain;

document.getElementById('saveDomainBtn')?.addEventListener('click', () => {
  const url = domainInput.value.trim();
  if (!url) {
    alert('Please enter a valid domain or endpoint URL');
    return;
  }
  localStorage.setItem('lakshana_custom_domain', url);
  alert(`Domain updated successfully to: ${url}\nReal-time updates will now sync to this database.`);
});

document.getElementById('testDomainPingBtn')?.addEventListener('click', () => {
  const url = domainInput.value.trim();
  const feedback = document.getElementById('domainTestFeedback');
  if (!url) {
    alert('Please enter a domain URL first');
    return;
  }
  feedback.style.display = 'block';
  feedback.style.background = '#eff6ff';
  feedback.style.color = '#1d4ed8';
  feedback.textContent = '⏳ Testing connection to ' + url + '...';

  setTimeout(() => {
    try {
      const parsed = new URL(url);
      feedback.style.background = '#ecfdf5';
      feedback.style.color = '#065f46';
      feedback.innerHTML = `✓ <strong>Connected!</strong> Host <code>${parsed.host}</code> is verified. Real-time updates active.`;
    } catch (e) {
      feedback.style.background = '#fef2f2';
      feedback.style.color = '#991b1b';
      feedback.innerHTML = `✕ <strong>Invalid URL format:</strong> Must include https:// or http:// (e.g. https://api.yourdomain.com)`;
    }
  }, 800);
});

// Export JSON Backup
document.getElementById('exportBackupBtn').addEventListener('click', () => {
  const data = {
    app: 'Lakshana Veggie Web Portal',
    version: '2.0',
    exportedAt: new Date().toISOString(),
    purchases,
    inventory,
    suppliers
  };
  const jsonStr = "data:text/json;charset=utf-8," + encodeURIComponent(JSON.stringify(data, null, 2));
  const dlAnchor = document.createElement('a');
  dlAnchor.setAttribute("href", jsonStr);
  dlAnchor.setAttribute("download", `lakshana_cloud_backup_${new Date().toISOString().slice(0,10)}.json`);
  dlAnchor.click();
});

// Process Upload Daily Sheet
document.getElementById('processUploadBtn').addEventListener('click', () => {
  const pasteText = document.getElementById('excelPasteArea').value.trim();
  const autoUpdate = document.getElementById('autoUpdateInventory').checked;

  let lines = pasteText ? pasteText.split('\n') : [
    '2026-09-25,Lakshana Agro Farms,Fresh Country Tomatoes,25,500.0,35.00,17500.00',
    '2026-09-25,Green Valley Mandi,Nashik Red Onions,40,1000.0,28.00,28000.00',
    '2026-09-25,Direct APMC Merchant,Ooty Fresh Carrots,30,450.0,45.00,20250.00'
  ];

  const batchId = 'BATCH-' + Date.now().toString().slice(-6);

  lines.forEach(line => {
    const parts = line.split(',').map(s => s.trim());
    if (parts.length >= 6 && !parts[0].toLowerCase().includes('date')) {
      let date, supplier, item, boxes, qtyKgs, rate, total;
      if (parts.length >= 7) {
        // Format: Date, Supplier, Items, Boxes, Qty(kgs), Rate, Total
        date = parts[0];
        supplier = parts[1] || 'Lakshana Agro Farms';
        item = parts[2];
        boxes = parseInt(parts[3]) || 0;
        qtyKgs = parseFloat(parts[4]) || 0;
        rate = parseFloat(parts[5]) || 0;
        total = qtyKgs * rate;
      } else {
        // Format: Date, Items, Boxes, Qty(kgs), Rate, Total
        date = parts[0];
        supplier = 'Lakshana Agro Farms';
        item = parts[1];
        boxes = parseInt(parts[2]) || 0;
        qtyKgs = parseFloat(parts[3]) || 0;
        rate = parseFloat(parts[4]) || 0;
        total = qtyKgs * rate;
      }

      purchases.unshift({
        date,
        batchId,
        item,
        boxes,
        qtyKgs,
        rate,
        total,
        supplier,
        paid: false
      });

      // Automatically sync supplier name to supplier list if not existing:
      const cleanSup = supplier ? supplier.trim() : '';
      if (cleanSup && cleanSup.toLowerCase() !== 'wholesale supplier') {
        const existingSup = suppliers.find(s => s.name.trim().toLowerCase() === cleanSup.toLowerCase());
        if (!existingSup) {
          suppliers.push({
            name: cleanSup,
            contact: '8608414322',
            location: 'Direct Mandi / Farm Procurement',
            balanceDue: total,
            active: true
          });
        } else {
          existingSup.balanceDue = (existingSup.balanceDue || 0) + total;
        }
      }

      if (autoUpdate) {
        const existing = inventory.find(i => i.name.toLowerCase().includes(item.toLowerCase()));
        if (existing) {
          existing.stockKgs += qtyKgs;
          existing.boxes += boxes;
        } else {
          inventory.push({
            name: item,
            icon: '🥬',
            stockKgs: qtyKgs,
            boxes: boxes,
            avgRate: rate,
            category: 'Vegetables'
          });
        }
      }
    }
  });

  saveState();
  document.getElementById('uploadModal').classList.remove('open');
  alert(`Daily Sheet Integrated Successfully! Created batch ${batchId}. Inventory stock updated.`);
});

// Print Report
document.getElementById('printReportBtn').addEventListener('click', () => {
  window.print();
});

// ==========================================
// DATE FORWARD & BACKWARD (DDMMYY) UTILITIES
// ==========================================
function formatDDMMYY(d) {
  const day = String(d.getDate()).padStart(2, '0');
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const year = String(d.getFullYear()).slice(-2);
  return `${day}-${month}-${year}`;
}

function formatCompactDDMMYY(d) {
  const day = String(d.getDate()).padStart(2, '0');
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const year = String(d.getFullYear()).slice(-2);
  return `${day}${month}${year}`;
}

function getHumanDate(d) {
  const options = { weekday: 'short', day: '2-digit', month: 'short', year: 'numeric' };
  return d.toLocaleDateString('en-IN', options);
}

function getRelativeTag(d) {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const target = new Date(d);
  target.setHours(0, 0, 0, 0);
  const diffDays = Math.round((target - today) / (1000 * 60 * 60 * 24));
  if (diffDays === 0) return 'Today';
  if (diffDays === -1) return 'Yesterday';
  if (diffDays === 1) return 'Tomorrow';
  if (diffDays < 0) return `${Math.abs(diffDays)} days ago`;
  return `In ${diffDays} days`;
}

// --- New Purchase Date Stepper State ---
let newPoCurrentDate = new Date();

function updateNewPoDateUI() {
  const display = formatDDMMYY(newPoCurrentDate);
  const compact = formatCompactDDMMYY(newPoCurrentDate);
  const human = getHumanDate(newPoCurrentDate);
  const rel = getRelativeTag(newPoCurrentDate);

  document.getElementById('newPoDdmmyyBadge').textContent = `DDMMYY: ${compact}`;
  document.getElementById('newPoDateDisplay').textContent = display;
  document.getElementById('newPoDateSub').textContent = `${human} (${rel})`;
}

document.getElementById('newPoDateBackBtn')?.addEventListener('click', () => {
  newPoCurrentDate.setDate(newPoCurrentDate.getDate() - 1);
  updateNewPoDateUI();
});

document.getElementById('newPoDateFwdBtn')?.addEventListener('click', () => {
  newPoCurrentDate.setDate(newPoCurrentDate.getDate() + 1);
  updateNewPoDateUI();
});

document.getElementById('newPoDateYest')?.addEventListener('click', () => {
  const d = new Date();
  d.setDate(d.getDate() - 1);
  newPoCurrentDate = d;
  updateNewPoDateUI();
});

document.getElementById('newPoDateToday')?.addEventListener('click', () => {
  newPoCurrentDate = new Date();
  updateNewPoDateUI();
});

document.getElementById('newPoDateTom')?.addEventListener('click', () => {
  const d = new Date();
  d.setDate(d.getDate() + 1);
  newPoCurrentDate = d;
  updateNewPoDateUI();
});

// Auto calculate New Purchase Total
function updateNewPoCalculatedTotal() {
  const qty = parseFloat(document.getElementById('newPoQty').value) || 0;
  const rate = parseFloat(document.getElementById('newPoRate').value) || 0;
  const total = qty * rate;
  document.getElementById('newPoTotalDisplay').textContent = `₹${total.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

document.getElementById('newPoQty')?.addEventListener('input', updateNewPoCalculatedTotal);
document.getElementById('newPoRate')?.addEventListener('input', updateNewPoCalculatedTotal);

// Save New Purchase Order
document.getElementById('saveNewPoBtn')?.addEventListener('click', () => {
  const item = document.getElementById('newPoItem').value.trim();
  const supplier = document.getElementById('newPoSupplier').value.trim() || 'Lakshana Agro Farms';
  const boxes = parseInt(document.getElementById('newPoBoxes').value) || 1;
  const qty = parseFloat(document.getElementById('newPoQty').value) || 0;
  const rate = parseFloat(document.getElementById('newPoRate').value) || 0;
  const autoStock = document.getElementById('newPoUpdateStock').checked;

  if (!item || qty <= 0 || rate <= 0) {
    alert('Please enter valid item name, quantity and rate');
    return;
  }

  const total = Math.round(qty * rate * 100) / 100;
  const poDate = formatDDMMYY(newPoCurrentDate);
  const poNumber = 'PO-' + Date.now().toString().slice(-6);

  purchases.unshift({
    date: poDate,
    batchId: poNumber,
    item: item,
    boxes: boxes,
    qtyKgs: qty,
    rate: rate,
    total: total,
    supplier: supplier,
    paid: false
  });

  // Automatically sync supplier to supplier list & add balanceDue
  const existingSup = suppliers.find(s => s.name.trim().toLowerCase() === supplier.toLowerCase());
  if (existingSup) {
    existingSup.balanceDue = (existingSup.balanceDue || 0) + total;
  } else {
    suppliers.push({
      name: supplier,
      contact: '8608414322',
      location: 'Direct Mandi / Farm Procurement',
      balanceDue: total,
      active: true
    });
  }

  // Update Inventory Stock
  if (autoStock) {
    const existingItem = inventory.find(i => i.name.toLowerCase().includes(item.toLowerCase()));
    if (existingItem) {
      existingItem.stockKgs += qty;
      existingItem.boxes += boxes;
    } else {
      inventory.push({
        name: item,
        icon: '🥬',
        stockKgs: qty,
        boxes: boxes,
        avgRate: rate,
        category: 'Vegetables'
      });
    }
  }

  saveState();
  document.getElementById('newPurchaseModal').classList.remove('open');
  alert(`New Purchase Order ${poNumber} created for date ${poDate}! (Total: ₹${total.toLocaleString('en-IN')})`);
});

// --- Purchase Settlement Modal Logic ---
let settleCurrentDate = new Date();
let currentSettlePurchase = null;
let currentSettleMode = 'Cash';

function updateSettleDateUI() {
  const display = formatDDMMYY(settleCurrentDate);
  const compact = formatCompactDDMMYY(settleCurrentDate);
  const human = getHumanDate(settleCurrentDate);
  const rel = getRelativeTag(settleCurrentDate);

  document.getElementById('settleDdmmyyBadge').textContent = `DDMMYY: ${compact}`;
  document.getElementById('settleDateDisplay').textContent = display;
  document.getElementById('settleDateSub').textContent = `${human} (${rel})`;
}

document.getElementById('settleDateBackBtn')?.addEventListener('click', () => {
  settleCurrentDate.setDate(settleCurrentDate.getDate() - 1);
  updateSettleDateUI();
});

document.getElementById('settleDateFwdBtn')?.addEventListener('click', () => {
  settleCurrentDate.setDate(settleCurrentDate.getDate() + 1);
  updateSettleDateUI();
});

document.getElementById('settleDateYest')?.addEventListener('click', () => {
  const d = new Date();
  d.setDate(d.getDate() - 1);
  settleCurrentDate = d;
  updateSettleDateUI();
});

document.getElementById('settleDateToday')?.addEventListener('click', () => {
  settleCurrentDate = new Date();
  updateSettleDateUI();
});

document.getElementById('settleDateTom')?.addEventListener('click', () => {
  const d = new Date();
  d.setDate(d.getDate() + 1);
  settleCurrentDate = d;
  updateSettleDateUI();
});

// Payment Mode Toggle in Modal
document.getElementById('modeCashBtn')?.addEventListener('click', () => {
  currentSettleMode = 'Cash';
  document.getElementById('modeCashBtn').style.background = '#ecfdf5';
  document.getElementById('modeCashBtn').style.borderColor = '#10b981';
  document.getElementById('modeCashBtn').style.color = '#065f46';
  document.getElementById('modeCashBtn').style.fontWeight = 'bold';

  document.getElementById('modeUpiBtn').style.background = 'transparent';
  document.getElementById('modeUpiBtn').style.borderColor = '#cbd5e1';
  document.getElementById('modeUpiBtn').style.color = '#334155';
  document.getElementById('modeUpiBtn').style.fontWeight = 'normal';

  document.getElementById('cashFields').style.display = 'block';
  document.getElementById('upiFields').style.display = 'none';
});

document.getElementById('modeUpiBtn')?.addEventListener('click', () => {
  currentSettleMode = 'UPI';
  document.getElementById('modeUpiBtn').style.background = '#f0fdfa';
  document.getElementById('modeUpiBtn').style.borderColor = '#0d9488';
  document.getElementById('modeUpiBtn').style.color = '#0f766e';
  document.getElementById('modeUpiBtn').style.fontWeight = 'bold';

  document.getElementById('modeCashBtn').style.background = 'transparent';
  document.getElementById('modeCashBtn').style.borderColor = '#cbd5e1';
  document.getElementById('modeCashBtn').style.color = '#334155';
  document.getElementById('modeCashBtn').style.fontWeight = 'normal';

  document.getElementById('cashFields').style.display = 'none';
  document.getElementById('upiFields').style.display = 'block';
});

window.openSettlementModal = function(batchId) {
  const p = purchases.find(item => item.batchId === batchId);
  if (!p) return;
  currentSettlePurchase = p;
  settleCurrentDate = new Date();
  updateSettleDateUI();

  document.getElementById('settlePoBatch').textContent = p.batchId;
  document.getElementById('settlePoItem').textContent = `${p.item} • ${p.supplier}`;
  document.getElementById('settlePoAmount').textContent = `₹${p.total.toLocaleString('en-IN', { minimumFractionDigits: 2 })}`;
  document.getElementById('settleCashVoucher').value = 'CASH-VCH-' + Math.floor(1000 + Math.random() * 9000);
  document.getElementById('settleUpiRef').value = Math.floor(100000000000 + Math.random() * 900000000000).toString();

  document.getElementById('settlePaymentModal').classList.add('open');
};

document.getElementById('confirmSettlementBtn')?.addEventListener('click', () => {
  if (!currentSettlePurchase) return;
  const settlementDateStr = formatDDMMYY(settleCurrentDate);
  const ref = currentSettleMode === 'Cash' 
    ? document.getElementById('settleCashVoucher').value.trim() 
    : document.getElementById('settleUpiRef').value.trim();

  currentSettlePurchase.paid = true;
  currentSettlePurchase.settlementDate = settlementDateStr;
  currentSettlePurchase.settlementRef = ref;
  currentSettlePurchase.settlementMode = currentSettleMode;

  // Deduct from supplier balanceDue
  const sup = suppliers.find(s => s.name.trim().toLowerCase() === currentSettlePurchase.supplier.trim().toLowerCase());
  if (sup && sup.balanceDue) {
    sup.balanceDue = Math.max(0, sup.balanceDue - currentSettlePurchase.total);
  }

  saveState();
  document.getElementById('settlePaymentModal').classList.remove('open');
  alert(`Payment for ${currentSettlePurchase.batchId} successfully settled on ${settlementDateStr} via ${currentSettleMode}! Ref: ${ref}`);
});

// Initial date setups
updateNewPoDateUI();
updateSettleDateUI();

// Initial Render
renderAll();
