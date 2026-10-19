// Lakshana Web Portal Script (App Linked)
let appData = {
  items: [],
  purchases: [],
  suppliers: [],
  transactions: []
};

function switchTab(tabId) {
  document.querySelectorAll('.tab-content').forEach(el => el.classList.remove('active'));
  document.querySelectorAll('.nav-item').forEach(el => el.classList.remove('active'));
  
  document.getElementById('tab-' + tabId).classList.add('active');
  event.currentTarget.classList.add('active');

  const titles = {
    dashboard: "Dashboard",
    items: "Item Master Catalog",
    purchase: "Purchase Orders Ledger",
    supplier: "Mandi Suppliers & Farms",
    transaction: "Settlements & UPI",
    inventory: "Inventory Stock"
  };
  document.getElementById('appCurrentScreenTitle').innerText = titles[tabId] || "Dashboard";
}

function openNewPurchaseModal() {
  document.getElementById('poDate').valueAsDate = new Date();
  document.getElementById('purchaseModal').classList.add('active');
}

function openNewItemModal() {
  document.getElementById('itemModal').classList.add('active');
}

function openNewSupplierModal() {
  document.getElementById('supplierModal').classList.add('active');
}

function closeModals() {
  document.querySelectorAll('.modal-overlay').forEach(m => m.classList.remove('active'));
}

function handleNewPurchase(e) {
  e.preventDefault();
  const purchase = {
    date: document.getElementById('poDate').value,
    batchId: 'PO-' + Math.floor(100000 + Math.random() * 900000),
    itemName: document.getElementById('poItemName').value,
    boxes: parseInt(document.getElementById('poBoxes').value) || 0,
    qtyKgs: parseFloat(document.getElementById('poQty').value) || 0,
    rate: parseFloat(document.getElementById('poRate').value) || 0,
    totalAmount: (parseFloat(document.getElementById('poQty').value) || 0) * (parseFloat(document.getElementById('poRate').value) || 0),
    supplierName: document.getElementById('poSupplier').value,
    status: 'Pending'
  };

  appData.purchases.unshift(purchase);
  
  let existingItem = appData.items.find(i => i.name.toLowerCase() === purchase.itemName.toLowerCase());
  if (existingItem) {
    existingItem.currentStockKgs += purchase.qtyKgs;
    existingItem.totalBoxes += purchase.boxes;
  } else {
    appData.items.push({
      name: purchase.itemName,
      code: 'SKU-' + purchase.itemName.substring(0, 3).toUpperCase() + '-' + Math.floor(100 + Math.random() * 900),
      category: 'Vegetables',
      currentStockKgs: purchase.qtyKgs,
      totalBoxes: purchase.boxes,
      defaultRatePerKg: purchase.rate
    });
  }

  let existingSup = appData.suppliers.find(s => s.name.toLowerCase() === purchase.supplierName.toLowerCase());
  if (!existingSup) {
    appData.suppliers.push({
      name: purchase.supplierName,
      contactPerson: purchase.supplierName,
      phone: '9876543210',
      vegetableCategories: 'Vegetables',
      outstandingPayable: purchase.totalAmount
    });
  } else {
    existingSup.outstandingPayable += purchase.totalAmount;
  }

  saveAndRender();
  closeModals();
  document.getElementById('purchaseForm').reset();
}

function handleNewItem(e) {
  e.preventDefault();
  const newItem = {
    name: document.getElementById('newItemName').value,
    code: document.getElementById('newItemCode').value,
    category: document.getElementById('newItemCategory').value,
    currentStockKgs: 0,
    totalBoxes: 0,
    defaultRatePerKg: parseFloat(document.getElementById('newItemRate').value) || 0
  };
  appData.items.push(newItem);
  saveAndRender();
  closeModals();
  e.target.reset();
}

function handleNewSupplier(e) {
  e.preventDefault();
  const newSup = {
    name: document.getElementById('newSupName').value,
    contactPerson: document.getElementById('newSupContact').value,
    phone: document.getElementById('newSupPhone').value,
    vegetableCategories: document.getElementById('newSupCat').value,
    outstandingPayable: 0
  };
  appData.suppliers.push(newSup);
  saveAndRender();
  closeModals();
  e.target.reset();
}

function saveAndRender() {
  localStorage.setItem('lakshana_portal_data', JSON.stringify(appData));
  renderAll();
}

function loadData() {
  const saved = localStorage.getItem('lakshana_portal_data');
  if (saved) {
    try {
      appData = JSON.parse(saved);
    } catch(e) {}
  }
}

function renderAll() {
  let totalKgs = appData.items.reduce((acc, i) => acc + (i.currentStockKgs || 0), 0);
  let totalBoxes = appData.items.reduce((acc, i) => acc + (i.totalBoxes || 0), 0);
  let totalPurchases = appData.purchases.reduce((acc, p) => acc + (p.totalAmount || 0), 0);

  document.getElementById('dashTotalStock').innerText = totalKgs.toFixed(1) + ' kgs';
  document.getElementById('dashTotalBoxes').innerText = totalBoxes + ' boxes / crates';
  document.getElementById('dashTotalPurchases').innerText = '₹' + totalPurchases.toLocaleString('en-IN', {minimumFractionDigits: 2});
  document.getElementById('dashPurchasesCount').innerText = appData.purchases.length + ' recorded purchases';
  document.getElementById('dashSupplierCount').innerText = appData.suppliers.length;

  renderDashboardPurchases();
  renderItems();
  renderPurchases();
  renderSuppliers();
  renderInventory();
}

function renderDashboardPurchases() {
  renderTable('dashboardPurchaseTbody', appData.purchases, p => `
    <td>${p.date}</td>
    <td><b>${p.batchId}</b></td>
    <td>${p.itemName}</td>
    <td>${p.boxes}</td>
    <td>${p.qtyKgs} kgs</td>
    <td>₹${p.rate.toFixed(2)}</td>
    <td><b>₹${p.totalAmount.toFixed(2)}</b></td>
    <td>${p.supplierName}</td>
    <td><span style="color:var(--success); font-weight:600;">${p.status}</span></td>
  `, 'No purchases recorded yet.');
}

function renderItems() {
  const query = (document.getElementById('searchItemsInput')?.value || '').toLowerCase();
  const filtered = appData.items.filter(i => 
    i.name.toLowerCase().includes(query) || i.code.toLowerCase().includes(query)
  );
  renderTable('itemsTbody', filtered, i => `
    <td><b>${i.name}</b></td>
    <td>${i.code}</td>
    <td>${i.category}</td>
    <td>${i.currentStockKgs} kgs</td>
    <td>${i.totalBoxes}</td>
    <td>₹${i.defaultRatePerKg.toFixed(2)}</td>
  `, 'No items match your search.');
}

function renderPurchases() {
  const query = (document.getElementById('searchPurchasesInput')?.value || '').toLowerCase();
  const filtered = appData.purchases.filter(p => 
    p.itemName.toLowerCase().includes(query) || 
    p.batchId.toLowerCase().includes(query) || 
    p.supplierName.toLowerCase().includes(query)
  );
  renderTable('purchaseTbody', filtered, p => `
    <td>${p.date}</td>
    <td><b>${p.batchId}</b></td>
    <td>${p.itemName}</td>
    <td>${p.boxes}</td>
    <td>${p.qtyKgs} kgs</td>
    <td>₹${p.rate.toFixed(2)}</td>
    <td><b>₹${p.totalAmount.toFixed(2)}</b></td>
    <td>${p.supplierName}</td>
    <td>${p.status}</td>
  `, 'No purchase orders match your search.');
}

function renderSuppliers() {
  const query = (document.getElementById('searchSuppliersInput')?.value || '').toLowerCase();
  const filtered = appData.suppliers.filter(s => 
    s.name.toLowerCase().includes(query) || s.contactPerson.toLowerCase().includes(query)
  );
  renderTable('supplierTbody', filtered, s => `
    <td><b>${s.name}</b></td>
    <td>${s.contactPerson}</td>
    <td>${s.phone}</td>
    <td>${s.vegetableCategories}</td>
    <td>₹${s.outstandingPayable.toFixed(2)}</td>
  `, 'No suppliers match your search.');
}

function renderInventory() {
  renderTable('inventoryTbody', appData.items, i => `
    <td><b>${i.name}</b></td>
    <td>${i.code}</td>
    <td>${i.currentStockKgs} kgs</td>
    <td>${i.totalBoxes} boxes</td>
    <td>₹${i.defaultRatePerKg.toFixed(2)} / kg</td>
  `, 'Inventory is currently empty.');
}

function renderTable(tbodyId, dataList, rowMapper, emptyMsg) {
  const tbody = document.getElementById(tbodyId);
  if (!tbody) return;
  if (!dataList || dataList.length === 0) {
    tbody.innerHTML = `<tr><td colspan="10" class="empty-row">${emptyMsg}</td></tr>`;
    return;
  }
  tbody.innerHTML = dataList.map(item => `<tr>${rowMapper(item)}</tr>`).join('');
}

loadData();
renderAll();
