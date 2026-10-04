// Lakshana Veggie Express - Web Portal Application Script
// Real-time Mandi Procurement & Inventory Management
// Fully synchronized with Android Mobile App Architecture (lakshanaveggie.trade)

// ==========================================
// 1. DATA STATE MANAGEMENT (ZERO SAMPLE DATA)
// ==========================================

// Auto-purge all stale mock/demo data from browser localStorage
(function purgeLegacySampleData() {
  try {
    ['lakshana_mock_seeded', 'lakshana_demo_data', 'lakshana_sample_initialized', 'lakshana_web_current_user'].forEach(k => {
      localStorage.removeItem(k);
    });

    ['lakshana_web_purchases', 'lakshana_web_inventory', 'lakshana_web_suppliers'].forEach(key => {
      const raw = localStorage.getItem(key);
      if (raw && (raw.includes('BATCH-2026') || raw.includes('Nashik') || raw.includes('Koyambedu APMC') || raw.includes('dummy') || raw.includes('sample'))) {
        localStorage.removeItem(key);
      }
    });
  } catch (e) {
    console.error('Error during cleanup:', e);
  }
})();

// Real storage arrays - strictly empty until user enters real data or syncs from mobile app
let purchases = JSON.parse(localStorage.getItem('lakshana_web_purchases')) || [];
let inventory = JSON.parse(localStorage.getItem('lakshana_web_inventory')) || [];
let suppliers = JSON.parse(localStorage.getItem('lakshana_web_suppliers')) || [];

function saveState(triggerAutoSync = true) {
  localStorage.setItem('lakshana_web_purchases', JSON.stringify(purchases));
  localStorage.setItem('lakshana_web_inventory', JSON.stringify(inventory));
  localStorage.setItem('lakshana_web_suppliers', JSON.stringify(suppliers));
  if (currentUser) {
    renderAll();
  }
  if (triggerAutoSync && currentUser) {
    // Automatically push to cloud without requiring manual actions or JSON uploads
    triggerRealSync(false);
  }
}

// ==========================================
// 2. USER AUTHENTICATION & ROLE MANAGEMENT
// ==========================================

// Pre-seeded Default Admin user (matching Android App: AppRepository.kt)
const DEFAULT_ADMIN = {
  id: 1,
  mobileNumber: '8608414322',
  fullName: 'Lakshana Admin',
  role: 'ADMIN',
  pinHash: '5147',
  department: 'Executive Admin',
  isActive: true,
  isApproved: true,
  createdAt: Date.now(),
  lastLoginAt: Date.now()
};

let users = JSON.parse(localStorage.getItem('lakshana_web_users')) || [DEFAULT_ADMIN];
// Ensure admin exists in user list
if (!users.some(u => u.mobileNumber === '8608414322')) {
  users.unshift(DEFAULT_ADMIN);
  localStorage.setItem('lakshana_web_users', JSON.stringify(users));
}

// Session-based current user: null when opening in fresh browser session
let currentUser = JSON.parse(sessionStorage.getItem('lakshana_web_current_user')) || null;

function saveUsers() {
  localStorage.setItem('lakshana_web_users', JSON.stringify(users));
}

function updateAuthVisibility() {
  const authScreen = document.getElementById('authScreen');
  const mainContent = document.getElementById('authenticatedMainContent');
  const headerActions = document.getElementById('authenticatedHeaderActions');

  if (currentUser) {
    if (authScreen) authScreen.style.display = 'none';
    if (mainContent) mainContent.style.display = 'block';
    if (headerActions) headerActions.style.display = 'flex';
  } else {
    if (authScreen) authScreen.style.display = 'flex';
    if (mainContent) mainContent.style.display = 'none';
    if (headerActions) headerActions.style.display = 'none';
  }
}

function setCurrentUser(user) {
  currentUser = user;
  if (user) {
    sessionStorage.setItem('lakshana_web_current_user', JSON.stringify(user));
  } else {
    sessionStorage.removeItem('lakshana_web_current_user');
  }
  updateAuthVisibility();
  renderHeaderAuth();
  if (currentUser) {
    renderAll();
    // Auto-fetch latest phone updates seamlessly
    setTimeout(() => { triggerRealSync(false); }, 600);
  }
}

function renderHeaderAuth() {
  const container = document.getElementById('headerAuthContainer');
  if (!container) return;

  if (currentUser) {
    const roleClass = `role-${currentUser.role.toLowerCase()}`;
    const initials = currentUser.fullName.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2) || 'LV';
    const adminMgmtBtn = currentUser.role === 'ADMIN'
      ? `<button class="btn btn-outline btn-sm" id="headerAdminUsersBtn" title="Manage Staff & Approvals" style="padding: 0.3rem 0.6rem; font-size: 0.75rem;">👥 Staff</button>`
      : '';

    container.innerHTML = `
      <div style="display: flex; align-items: center; gap: 0.5rem;">
        <div class="user-profile-badge">
          <div class="user-avatar-circle">${initials}</div>
          <div class="user-info-text">
            <span class="user-name-title">${currentUser.fullName}</span>
            <span class="user-role-tag ${roleClass}">${currentUser.role}</span>
          </div>
        </div>
        ${adminMgmtBtn}
        <button class="btn btn-outline btn-sm" id="headerSignOutBtn" title="Sign Out / Lock Portal" style="padding: 0.3rem 0.6rem; font-size: 0.75rem; color: #dc2626; border-color: #fca5a5;">🚪 Exit</button>
      </div>
    `;

    document.getElementById('headerSignOutBtn')?.addEventListener('click', () => {
      if (confirm('Lock management portal and sign out?')) {
        setCurrentUser(null);
      }
    });

    document.getElementById('headerAdminUsersBtn')?.addEventListener('click', () => {
      openAdminUsersModal();
    });
  } else {
    container.innerHTML = `
      <span class="badge" style="background: #fef3c7; color: #92400e; font-weight: 700; padding: 0.35rem 0.75rem; border-radius: 9999px; font-size: 0.75rem;">
        🔒 Restricted Internal System
      </span>
    `;
  }
}

// Portal Authentication Form Handlers
function initPortalAuth() {
  const tabLogin = document.getElementById('portalTabLogin');
  const tabRegister = document.getElementById('portalTabRegister');
  const loginContainer = document.getElementById('loginFormContainer');
  const regContainer = document.getElementById('registerFormContainer');
  const authTopSub = document.getElementById('authTopSub');
  const quickAdminBtn = document.getElementById('portalQuickAdminBtn');
  const clearMobileBtn = document.getElementById('clearMobileBtn');
  const togglePinBtn = document.getElementById('togglePinVisibilityBtn');
  const pinInput = document.getElementById('portalLoginPin');
  const mobileInput = document.getElementById('portalLoginMobile');
  const forgotPinLink = document.getElementById('forgotPinLink');

  tabLogin?.addEventListener('click', () => {
    tabLogin.classList.add('active');
    tabRegister?.classList.remove('active');
    if (loginContainer) loginContainer.style.display = 'block';
    if (regContainer) regContainer.style.display = 'none';
    if (authTopSub) authTopSub.textContent = 'Secure Staff Sign In';
    hidePortalAlert();
  });

  tabRegister?.addEventListener('click', () => {
    tabRegister.classList.add('active');
    tabLogin?.classList.remove('active');
    if (loginContainer) loginContainer.style.display = 'none';
    if (regContainer) regContainer.style.display = 'block';
    if (authTopSub) authTopSub.textContent = 'User Registration';
    hidePortalAlert();
  });

  quickAdminBtn?.addEventListener('click', () => {
    if (mobileInput) mobileInput.value = '8608414322';
    if (pinInput) pinInput.value = '5147';
    handlePortalLoginSubmit();
  });

  clearMobileBtn?.addEventListener('click', () => {
    if (mobileInput) {
      mobileInput.value = '';
      mobileInput.focus();
    }
  });

  togglePinBtn?.addEventListener('click', () => {
    if (pinInput) {
      if (pinInput.type === 'password') {
        pinInput.type = 'text';
        togglePinBtn.textContent = '🔒';
      } else {
        pinInput.type = 'password';
        togglePinBtn.textContent = '👁️';
      }
    }
  });

  forgotPinLink?.addEventListener('click', () => {
    document.getElementById('forgotPinModal')?.classList.add('open');
  });
}

function showPortalAlert(msg, isError = true) {
  const el = document.getElementById('portalAuthAlert');
  if (!el) return;
  el.className = `auth-alert-box ${isError ? 'auth-alert-error' : 'auth-alert-info'}`;
  el.textContent = msg;
  el.style.display = 'block';
}

function hidePortalAlert() {
  const el = document.getElementById('portalAuthAlert');
  if (el) el.style.display = 'none';
}

window.handlePortalLoginSubmit = function() {
  const mobile = document.getElementById('portalLoginMobile')?.value.trim();
  const pin = document.getElementById('portalLoginPin')?.value.trim();

  if (!mobile) {
    showPortalAlert('Please enter your 10-digit registered mobile number.', true);
    return;
  }
  if (!pin) {
    showPortalAlert('Please enter your 4-digit security PIN.', true);
    return;
  }

  // Check default admin
  if (mobile === '8608414322' && pin === '5147') {
    let admin = users.find(u => u.mobileNumber === '8608414322');
    if (!admin) {
      admin = { ...DEFAULT_ADMIN };
      users.unshift(admin);
      saveUsers();
    }
    admin.lastLoginAt = Date.now();
    saveUsers();
    setCurrentUser(admin);
    return;
  }

  const user = users.find(u => u.mobileNumber === mobile);
  if (!user) {
    showPortalAlert('Mobile number not found. Please click "Request Access" to register.', true);
    return;
  }

  if (user.pinHash !== pin) {
    showPortalAlert('Incorrect PIN. Please try again or contact Admin (8608414322).', true);
    return;
  }

  if (!user.isApproved) {
    showPortalAlert('Account Pending Approval: Awaiting Admin approval.', true);
    return;
  }

  if (!user.isActive) {
    showPortalAlert('Account Deactivated. Please contact Administrator.', true);
    return;
  }

  user.lastLoginAt = Date.now();
  saveUsers();
  setCurrentUser(user);
};

window.handlePortalRegisterSubmit = function() {
  const name = document.getElementById('portalRegName')?.value.trim();
  const mobile = document.getElementById('portalRegMobile')?.value.trim();
  const pin = document.getElementById('portalRegPin')?.value.trim();
  const dept = document.getElementById('portalRegDept')?.value || 'Procurement';

  if (!name || mobile.length < 8 || pin.length < 4) {
    showPortalAlert('Please enter valid name, 10-digit mobile, and 4-digit PIN.', true);
    return;
  }

  if (users.some(u => u.mobileNumber === mobile)) {
    showPortalAlert(`Mobile ${mobile} is already registered. Please Sign In.`, true);
    return;
  }

  const isAdmin = mobile === '8608414322';
  const newUser = {
    id: Date.now(),
    mobileNumber: mobile,
    fullName: name,
    role: isAdmin ? 'ADMIN' : (dept === 'Warehouse' ? 'INVENTORY_CLERK' : (dept === 'Operations' ? 'MANAGER' : 'PURCHASER')),
    pinHash: pin,
    department: dept,
    isActive: true,
    isApproved: isAdmin,
    createdAt: Date.now(),
    lastLoginAt: isAdmin ? Date.now() : 0
  };

  users.push(newUser);
  saveUsers();

  if (isAdmin) {
    setCurrentUser(newUser);
  } else {
    showPortalAlert(`Registration submitted for ${name}! Please ask Admin (8608414322) to approve your account.`, false);
  }
};

function openAuthModal(initialTab = 'login') {
  switchAuthTab(initialTab);
  document.getElementById('authAlert').style.display = 'none';
  document.getElementById('authModal').classList.add('open');
}

function closeAuthModal() {
  document.getElementById('authModal').classList.remove('open');
}

function switchAuthTab(tab) {
  const isLogin = tab === 'login';
  document.getElementById('authTabLogin').classList.toggle('active', isLogin);
  document.getElementById('authTabRegister').classList.toggle('active', !isLogin);
  document.getElementById('loginFormSection').style.display = isLogin ? 'block' : 'none';
  document.getElementById('registerFormSection').style.display = isLogin ? 'none' : 'block';
  document.getElementById('authModalTitle').textContent = isLogin ? '🔑 Sign In to Lakshana Veggie' : '📝 Register New Staff Account';
  document.getElementById('authAlert').style.display = 'none';
}

function showAuthAlert(msg, isError = true) {
  const el = document.getElementById('authAlert');
  el.className = `auth-alert-box ${isError ? 'auth-alert-error' : 'auth-alert-info'}`;
  el.textContent = msg;
  el.style.display = 'block';
}

// 1-Click Fast Admin Sign In
document.getElementById('quickAdminLoginBtn')?.addEventListener('click', () => {
  document.getElementById('loginMobile').value = '8608414322';
  document.getElementById('loginPin').value = '5147';
  handleLogin();
});

// Login Form Submit
document.getElementById('submitLoginBtn')?.addEventListener('click', handleLogin);

function handleLogin() {
  const mobile = document.getElementById('loginMobile').value.trim();
  const pin = document.getElementById('loginPin').value.trim();

  if (!mobile) {
    showAuthAlert('Please enter your 10-digit registered mobile number.', true);
    return;
  }
  if (!pin) {
    showAuthAlert('Please enter your 4-digit security PIN.', true);
    return;
  }

  // Check against registered users
  let user = users.find(u => u.mobileNumber === mobile);

  // Default admin login or creation on-the-fly
  if ((!user && mobile === '8608414322' && pin === '5147') || (user && user.mobileNumber === '8608414322' && pin === '5147')) {
    if (!user) {
      user = { ...DEFAULT_ADMIN };
      users.unshift(user);
      saveUsers();
    }
    user.lastLoginAt = Date.now();
    saveUsers();
    setCurrentUser(user);
    closeAuthModal();
    return;
  }

  if (!user) {
    showAuthAlert('Mobile number not found. Please click "Register Account" to create your credentials.', true);
    return;
  }

  if (user.pinHash !== pin) {
    showAuthAlert('Incorrect Security PIN. If forgotten, contact Administrator (8608414322).', true);
    return;
  }

  if (!user.isApproved) {
    showAuthAlert('Account Pending Approval: Your registration has been submitted and is awaiting Admin approval.', true);
    return;
  }

  if (!user.isActive) {
    showAuthAlert('This account is currently deactivated. Please contact Administrator.', true);
    return;
  }

  user.lastLoginAt = Date.now();
  saveUsers();
  setCurrentUser(user);
  closeAuthModal();
}

// Register Form Submit
document.getElementById('submitRegisterBtn')?.addEventListener('click', () => {
  const name = document.getElementById('regFullName').value.trim();
  const mobile = document.getElementById('regMobile').value.trim();
  const pin = document.getElementById('regPin').value.trim();
  const dept = document.getElementById('regDepartment').value;

  if (!name) {
    showAuthAlert('Please enter your full name.', true);
    return;
  }
  if (mobile.length < 8) {
    showAuthAlert('Please enter a valid 10-digit mobile number.', true);
    return;
  }
  if (pin.length < 4) {
    showAuthAlert('Security PIN must be at least 4 digits.', true);
    return;
  }

  const existing = users.find(u => u.mobileNumber === mobile);
  if (existing) {
    showAuthAlert(`Mobile ${mobile} is already registered. Please sign in.`, true);
    return;
  }

  const isAdminReg = (mobile === '8608414322' && pin === '5147');
  const newUser = {
    id: Date.now(),
    mobileNumber: mobile,
    fullName: isAdminReg ? 'Lakshana Admin' : name,
    role: isAdminReg ? 'ADMIN' : (dept === 'Warehouse' ? 'INVENTORY_CLERK' : (dept === 'Operations' ? 'MANAGER' : (dept === 'Auditor' ? 'VIEWER' : 'PURCHASER'))),
    pinHash: pin,
    department: dept,
    isActive: true,
    isApproved: isAdminReg, // Admin is immediately approved; others require admin approval
    createdAt: Date.now(),
    lastLoginAt: Date.now()
  };

  users.push(newUser);
  saveUsers();

  if (isAdminReg) {
    setCurrentUser(newUser);
    closeAuthModal();
  } else {
    showAuthAlert('Registration submitted successfully! Your account is awaiting Administrator approval.', false);
    document.getElementById('regFullName').value = '';
    document.getElementById('regMobile').value = '';
    document.getElementById('regPin').value = '';
    setTimeout(() => {
      switchAuthTab('login');
      document.getElementById('loginMobile').value = mobile;
    }, 1500);
  }
});

document.getElementById('authTabLogin')?.addEventListener('click', () => switchAuthTab('login'));
document.getElementById('authTabRegister')?.addEventListener('click', () => switchAuthTab('register'));

// ==========================================
// 3. ADMIN USER MANAGEMENT & APPROVALS
// ==========================================

function openAdminUsersModal() {
  if (!currentUser || currentUser.role !== 'ADMIN') {
    alert('Access restricted: Only Administrators can manage staff accounts.');
    return;
  }
  renderAdminUsersTable();
  document.getElementById('adminUsersModal').classList.add('open');
}

function renderAdminUsersTable() {
  const tbody = document.getElementById('adminUsersTbody');
  if (!tbody) return;
  tbody.innerHTML = '';

  users.forEach(u => {
    const tr = document.createElement('tr');
    const isMainAdmin = u.mobileNumber === '8608414322';

    const statusBadge = u.isApproved 
      ? (u.isActive ? `<span class="badge badge-success">Active</span>` : `<span class="badge" style="background:#fee2e2; color:#991b1b;">Inactive</span>`)
      : `<span class="badge badge-warning">Pending Approval</span>`;

    const approveBtn = !u.isApproved 
      ? `<button class="btn btn-sm btn-primary" onclick="approveUserById(${u.id})">✓ Approve</button>`
      : '';

    const roleSelect = isMainAdmin
      ? `<strong>ADMIN</strong>`
      : `<select class="form-input" style="padding: 0.2rem 0.4rem; font-size: 0.75rem;" onchange="changeUserRoleById(${u.id}, this.value)">
          <option value="PURCHASER" ${u.role === 'PURCHASER' ? 'selected' : ''}>PURCHASER</option>
          <option value="MANAGER" ${u.role === 'MANAGER' ? 'selected' : ''}>MANAGER</option>
          <option value="INVENTORY_CLERK" ${u.role === 'INVENTORY_CLERK' ? 'selected' : ''}>INVENTORY_CLERK</option>
          <option value="VIEWER" ${u.role === 'VIEWER' ? 'selected' : ''}>VIEWER</option>
          <option value="ADMIN" ${u.role === 'ADMIN' ? 'selected' : ''}>ADMIN</option>
        </select>`;

    const toggleStatusBtn = isMainAdmin 
      ? `<span style="font-size: 0.75rem; color: #94a3b8;">Main Admin</span>`
      : `<button class="btn btn-sm btn-outline" onclick="toggleUserActiveById(${u.id})" style="padding: 0.2rem 0.4rem; font-size: 0.75rem;">${u.isActive ? 'Deactivate' : 'Activate'}</button>`;

    tr.innerHTML = `
      <td><strong>${u.fullName}</strong></td>
      <td><code>${u.mobileNumber}</code></td>
      <td>${roleSelect}</td>
      <td>${u.department || 'Procurement'}</td>
      <td>${statusBadge}</td>
      <td>
        <div style="display: flex; gap: 0.35rem; align-items: center;">
          ${approveBtn}
          ${toggleStatusBtn}
        </div>
      </td>
    `;
    tbody.appendChild(tr);
  });
}

window.approveUserById = function(userId) {
  const u = users.find(x => x.id === userId);
  if (u) {
    u.isApproved = true;
    u.isActive = true;
    saveUsers();
    renderAdminUsersTable();
    alert(`Account for ${u.fullName} (${u.mobileNumber}) approved successfully!`);
  }
};

window.changeUserRoleById = function(userId, newRole) {
  const u = users.find(x => x.id === userId);
  if (u) {
    u.role = newRole;
    saveUsers();
    alert(`Updated ${u.fullName}'s role to ${newRole}`);
  }
};

window.toggleUserActiveById = function(userId) {
  const u = users.find(x => x.id === userId);
  if (u) {
    u.isActive = !u.isActive;
    saveUsers();
    renderAdminUsersTable();
  }
};

// Admin Add Staff Directly
document.getElementById('adminCreateUserBtn')?.addEventListener('click', () => {
  const name = document.getElementById('adminNewUserName').value.trim();
  const mobile = document.getElementById('adminNewUserMobile').value.trim();
  const role = document.getElementById('adminNewUserRole').value;
  const pin = document.getElementById('adminNewUserPin').value.trim();

  if (!name || mobile.length < 8 || pin.length < 4) {
    alert('Please enter valid staff name, 10-digit mobile, and 4-digit PIN');
    return;
  }

  if (users.some(u => u.mobileNumber === mobile)) {
    alert(`Mobile ${mobile} is already registered.`);
    return;
  }

  users.push({
    id: Date.now(),
    mobileNumber: mobile,
    fullName: name,
    role: role,
    pinHash: pin,
    department: role === 'ADMIN' ? 'Executive Admin' : (role === 'INVENTORY_CLERK' ? 'Warehouse' : 'Procurement'),
    isActive: true,
    isApproved: true,
    createdAt: Date.now(),
    lastLoginAt: 0
  });

  saveUsers();
  document.getElementById('adminNewUserName').value = '';
  document.getElementById('adminNewUserMobile').value = '';
  renderAdminUsersTable();
  alert(`Staff member ${name} created and approved successfully!`);
});

// ==========================================
// 4. RENDERING & STATS (CALCULATED FROM REAL DATA)
// ==========================================

function renderAll() {
  if (!currentUser) {
    updateAuthVisibility();
    return;
  }
  updateAuthVisibility();
  renderStats();
  renderPurchasesTable();
  renderInventoryGrid();
  renderSuppliersGrid();
}

function renderStats() {
  const totalKgs = inventory.reduce((sum, item) => sum + (item.stockKgs || 0), 0);
  const totalBoxes = inventory.reduce((sum, item) => sum + (item.boxes || 0), 0);
  const todayTotal = purchases.reduce((sum, p) => sum + (p.total || 0), 0);

  document.getElementById('totalStockKgs').innerHTML = `${totalKgs.toLocaleString('en-IN', { maximumFractionDigits: 1 })} <small>kgs</small>`;
  document.getElementById('totalBoxesCount').textContent = `${totalBoxes} boxes / crates`;
  document.getElementById('todayPurchasesTotal').textContent = `₹${todayTotal.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  document.getElementById('todayIntakeBatchesSub').textContent = `${purchases.length} recorded purchases`;
  document.getElementById('activeSuppliersCount').textContent = suppliers.filter(s => s.active !== false).length;
}

// ==========================================
// 5. PURCHASES LEDGER & SETTLEMENT
// ==========================================

function renderPurchasesTable(filterText = '') {
  const tbody = document.getElementById('purchasesTbody');
  if (!tbody) return;
  tbody.innerHTML = '';

  const filtered = purchases.filter(p => 
    (p.item || '').toLowerCase().includes(filterText.toLowerCase()) ||
    (p.supplier || '').toLowerCase().includes(filterText.toLowerCase()) ||
    (p.batchId || '').toLowerCase().includes(filterText.toLowerCase())
  );

  if (filtered.length === 0) {
    tbody.innerHTML = `
      <tr>
        <td colspan="9" style="padding: 0;">
          <div class="empty-state">
            <div class="empty-state-icon">📑</div>
            <h3>No Purchases Recorded Yet</h3>
            <p>Start recording your daily Mandi intake by clicking "+ New Purchase" or uploading your procurement sheet.</p>
            <button class="btn btn-primary" onclick="openNewPurchaseModal()">+ Add First Purchase</button>
          </div>
        </td>
      </tr>
    `;
    return;
  }

  filtered.forEach(p => {
    const tr = document.createElement('tr');
    let paymentCol;
    if (p.paid) {
      paymentCol = `<span class="badge badge-success">✓ Settled (${p.settlementDate || p.date})</span>`;
    } else {
      paymentCol = `<button class="btn btn-sm btn-primary" style="padding: 0.25rem 0.5rem; font-size: 0.75rem;" onclick="openSettlementModal('${p.batchId}')">⚡ Settle Payment</button>`;
    }

    tr.innerHTML = `
      <td><strong>${p.date}</strong></td>
      <td><code>${p.batchId}</code></td>
      <td><strong>${p.item}</strong></td>
      <td>${p.boxes || 0}</td>
      <td>${(p.qtyKgs || 0).toFixed(1)} kgs</td>
      <td>₹${(p.rate || 0).toFixed(2)}</td>
      <td><strong style="color: #15803d;">₹${(p.total || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}</strong></td>
      <td>${p.supplier}</td>
      <td>${paymentCol}</td>
    `;
    tbody.appendChild(tr);
  });
}

// ==========================================
// 6. INVENTORY & STOCK LEVELS
// ==========================================

function renderInventoryGrid(filterText = '') {
  const grid = document.getElementById('inventoryCardsGrid');
  if (!grid) return;
  grid.innerHTML = '';

  const filtered = inventory.filter(i => (i.name || '').toLowerCase().includes(filterText.toLowerCase()));

  if (filtered.length === 0) {
    grid.innerHTML = `
      <div style="grid-column: 1 / -1;">
        <div class="empty-state">
          <div class="empty-state-icon">🥕</div>
          <h3>Warehouse Inventory is Empty</h3>
          <p>Stock balances will automatically update as you create purchases or upload daily Mandi arrival sheets.</p>
          <button class="btn btn-primary" onclick="openNewPurchaseModal()">+ Add Vegetable Stock</button>
        </div>
      </div>
    `;
    return;
  }

  filtered.forEach(item => {
    const card = document.createElement('div');
    card.className = 'veg-card';
    card.innerHTML = `
      <div>
        <div class="veg-card-header">
          <span style="font-size: 1.5rem;">${item.icon || '🥬'}</span>
          <span class="badge badge-success">${item.category || 'Vegetables'}</span>
        </div>
        <h4 class="veg-name">${item.name}</h4>
        <div class="veg-stock">${(item.stockKgs || 0).toLocaleString('en-IN')} <small style="font-size: 0.85rem; font-weight: 500;">kgs</small></div>
      </div>
      <div class="veg-details">
        <div>📦 Available Crates: <strong>${item.boxes || 0} boxes</strong></div>
        <div>💰 Avg Mandi Rate: <strong>₹${(item.avgRate || 0).toFixed(2)} / kg</strong></div>
      </div>
    `;
    grid.appendChild(card);
  });
}

// ==========================================
// 7. MANDI SUPPLIERS & BALANCES
// ==========================================

function renderSuppliersGrid() {
  const grid = document.getElementById('suppliersGrid');
  if (!grid) return;
  grid.innerHTML = '';

  if (suppliers.length === 0) {
    grid.innerHTML = `
      <div style="grid-column: 1 / -1;">
        <div class="empty-state">
          <div class="empty-state-icon">🏢</div>
          <h3>No Mandi Vendors Registered</h3>
          <p>Vendors will appear automatically when purchases are entered, or you can register your procurement partners directly.</p>
          <button class="btn btn-primary" onclick="document.getElementById('newSupplierModal').classList.add('open')">+ Register Mandi Vendor</button>
        </div>
      </div>
    `;
    return;
  }

  suppliers.forEach(s => {
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

      <div style="background:#f8fafc; border:1px solid ${displayBalance > 0 ? '#fde68a' : '#bbf7d0'}; border-radius:8px; padding:0.75rem; margin:0.75rem 0;">
        <div style="font-size:0.75rem; text-transform:uppercase; font-weight:700; color:${displayBalance > 0 ? '#b45309' : '#15803d'}; letter-spacing:0.05em;">
          Outstanding Balance
        </div>
        <div style="font-size:1.35rem; font-weight:800; color:${displayBalance > 0 ? '#b45309' : '#15803d'};">
          ₹${displayBalance.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
        </div>
        <div style="font-size:0.75rem; color:#64748b; margin-top:0.25rem;">
          ${unpaidPurchases.length > 0 ? `⚡ Pulled from daily ledger: ${unpaidPurchases.length} unpaid bills` : '✓ All daily ledger purchases settled'}
        </div>
      </div>

      <div style="display: flex; justify-content: space-between; font-size: 0.8125rem; color:#475569; padding-top: 0.25rem;">
        <div>Total Sourced: <strong style="color:#0f172a;">₹${totalPurchased.toLocaleString('en-IN', { maximumFractionDigits: 0 })}</strong></div>
        <div>Settled: <strong style="color:#15803d;">₹${totalSettled.toLocaleString('en-IN', { maximumFractionDigits: 0 })}</strong></div>
      </div>
    `;
    grid.appendChild(card);
  });
}

// Add Supplier Action
document.getElementById('openNewSupplierModalBtn')?.addEventListener('click', () => {
  document.getElementById('newSupplierModal').classList.add('open');
});

document.getElementById('saveSupplierBtn')?.addEventListener('click', () => {
  const name = document.getElementById('supplierNameInput').value.trim();
  const contact = document.getElementById('supplierContactInput').value.trim() || '8608414322';
  const location = document.getElementById('supplierLocationInput').value.trim() || 'Mandi Yard';
  const balance = parseFloat(document.getElementById('supplierOpeningBalance').value) || 0;

  if (!name) {
    alert('Please enter supplier / vendor business name');
    return;
  }

  const existing = suppliers.find(s => s.name.toLowerCase() === name.toLowerCase());
  if (existing) {
    alert('A supplier with this name already exists.');
    return;
  }

  suppliers.push({
    name,
    contact,
    location,
    balanceDue: balance,
    active: true
  });

  saveState();
  document.getElementById('newSupplierModal').classList.remove('open');
  document.getElementById('supplierNameInput').value = '';
  document.getElementById('supplierContactInput').value = '';
  alert(`Mandi vendor "${name}" registered successfully!`);
});

// ==========================================
// 8. TABS & MODALS NAVIGATION
// ==========================================

document.querySelectorAll('.portal-tabs .tab-btn').forEach(btn => {
  btn.addEventListener('click', () => {
    document.querySelectorAll('.portal-tabs .tab-btn').forEach(b => b.classList.remove('active'));
    document.querySelectorAll('.tab-panel').forEach(p => p.classList.remove('active'));
    btn.classList.add('active');
    document.getElementById(`tab-${btn.dataset.tab}`).classList.add('active');
  });
});

document.querySelectorAll('.upload-tab-btn').forEach(btn => {
  btn.addEventListener('click', () => {
    document.querySelectorAll('.upload-tab-btn').forEach(b => b.classList.remove('active'));
    document.querySelectorAll('.upload-tab-content').forEach(c => c.classList.remove('active'));
    btn.classList.add('active');
    document.getElementById(`utab-${btn.dataset.utab}`).classList.add('active');
  });
});

window.openNewPurchaseModal = function() {
  if (!currentUser) {
    openAuthModal();
    return;
  }
  document.getElementById('newPurchaseModal').classList.add('open');
};

document.getElementById('openNewPurchaseBtn')?.addEventListener('click', openNewPurchaseModal);

document.getElementById('openUploadModalBtn')?.addEventListener('click', () => {
  if (!currentUser) {
    openAuthModal();
    return;
  }
  document.getElementById('uploadModal').classList.add('open');
});

// Search filters
document.getElementById('purchaseSearch')?.addEventListener('input', (e) => {
  renderPurchasesTable(e.target.value);
});

document.getElementById('inventorySearch')?.addEventListener('input', (e) => {
  renderInventoryGrid(e.target.value);
});

// Print
document.getElementById('printReportBtn')?.addEventListener('click', () => {
  window.print();
});

// ==========================================
// 9. DATE UTILITIES (DDMMYY FORWARD & BACKWARD)
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

// New PO Date Stepper State
let newPoCurrentDate = new Date();

function updateNewPoDateUI() {
  const display = formatDDMMYY(newPoCurrentDate);
  const compact = formatCompactDDMMYY(newPoCurrentDate);
  const human = getHumanDate(newPoCurrentDate);
  const rel = getRelativeTag(newPoCurrentDate);

  const badge = document.getElementById('newPoDdmmyyBadge');
  const disp = document.getElementById('newPoDateDisplay');
  const sub = document.getElementById('newPoDateSub');
  if (badge) badge.textContent = `DDMMYY: ${compact}`;
  if (disp) disp.textContent = display;
  if (sub) sub.textContent = `${human} (${rel})`;
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

// Auto-Calculate New PO Total
function updateNewPoCalculatedTotal() {
  const qty = parseFloat(document.getElementById('newPoQty').value) || 0;
  const rate = parseFloat(document.getElementById('newPoRate').value) || 0;
  const total = qty * rate;
  document.getElementById('newPoTotalDisplay').textContent = `₹${total.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

document.getElementById('newPoQty')?.addEventListener('input', updateNewPoCalculatedTotal);
document.getElementById('newPoRate')?.addEventListener('input', updateNewPoCalculatedTotal);

// Save New PO
document.getElementById('saveNewPoBtn')?.addEventListener('click', () => {
  if (!currentUser) {
    openAuthModal();
    return;
  }

  const item = document.getElementById('newPoItem').value.trim();
  const supplier = document.getElementById('newPoSupplier').value.trim() || 'Direct Mandi Farmer';
  const boxes = parseInt(document.getElementById('newPoBoxes').value) || 1;
  const qty = parseFloat(document.getElementById('newPoQty').value) || 0;
  const rate = parseFloat(document.getElementById('newPoRate').value) || 0;
  const autoStock = document.getElementById('newPoUpdateStock').checked;

  if (!item || qty <= 0 || rate <= 0) {
    alert('Please enter valid item name, quantity and rate per kg.');
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
    paid: false,
    recordedBy: currentUser.fullName
  });

  // Automatically register supplier if not present
  const existingSup = suppliers.find(s => s.name.trim().toLowerCase() === supplier.toLowerCase());
  if (existingSup) {
    existingSup.balanceDue = (existingSup.balanceDue || 0) + total;
  } else {
    suppliers.push({
      name: supplier,
      contact: '8608414322',
      location: 'Direct Mandi / Farm Gate',
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
      existingItem.avgRate = Math.round(((existingItem.avgRate + rate) / 2) * 100) / 100;
    } else {
      let icon = '🥬';
      const lower = item.toLowerCase();
      if (lower.includes('tomato')) icon = '🍅';
      else if (lower.includes('onion')) icon = '🧅';
      else if (lower.includes('carrot')) icon = '🥕';
      else if (lower.includes('potato')) icon = '🥔';
      else if (lower.includes('chilli') || lower.includes('chili')) icon = '🌶️';
      else if (lower.includes('cauliflower') || lower.includes('cabbage')) icon = '🥦';
      else if (lower.includes('capsicum')) icon = '🫑';

      inventory.push({
        name: item,
        icon: icon,
        stockKgs: qty,
        boxes: boxes,
        avgRate: rate,
        category: 'Vegetables'
      });
    }
  }

  saveState();
  document.getElementById('newPurchaseModal').classList.remove('open');
  alert(`Purchase Order ${poNumber} for ${qty} kgs ${item} created successfully!`);
});

// Process Daily Sheet Upload
document.getElementById('processUploadBtn')?.addEventListener('click', () => {
  if (!currentUser) {
    openAuthModal();
    return;
  }

  const pasteText = document.getElementById('excelPasteArea').value.trim();
  const autoUpdate = document.getElementById('autoUpdateInventory').checked;

  if (!pasteText) {
    alert('Please enter or paste purchase sheet rows in CSV / Tabular format (Date, Supplier, Items, Boxes, Qty, Rate, Total)');
    return;
  }

  const lines = pasteText.split('\n').filter(l => l.trim().length > 0);
  const batchId = 'BATCH-' + Date.now().toString().slice(-6);
  let importedCount = 0;

  lines.forEach(line => {
    const parts = line.split(',').map(s => s.trim());
    if (parts.length >= 6 && !parts[0].toLowerCase().includes('date')) {
      let date, supplier, item, boxes, qtyKgs, rate, total;
      if (parts.length >= 7) {
        date = parts[0];
        supplier = parts[1] || 'Direct Mandi Farmer';
        item = parts[2];
        boxes = parseInt(parts[3]) || 0;
        qtyKgs = parseFloat(parts[4]) || 0;
        rate = parseFloat(parts[5]) || 0;
        total = qtyKgs * rate;
      } else {
        date = parts[0];
        supplier = 'Direct Mandi Farmer';
        item = parts[1];
        boxes = parseInt(parts[2]) || 0;
        qtyKgs = parseFloat(parts[3]) || 0;
        rate = parseFloat(parts[4]) || 0;
        total = qtyKgs * rate;
      }

      if (item && qtyKgs > 0) {
        purchases.unshift({
          date,
          batchId,
          item,
          boxes,
          qtyKgs,
          rate,
          total,
          supplier,
          paid: false,
          recordedBy: currentUser.fullName
        });

        // Supplier balance sync
        const cleanSup = supplier ? supplier.trim() : '';
        if (cleanSup) {
          const existingSup = suppliers.find(s => s.name.trim().toLowerCase() === cleanSup.toLowerCase());
          if (!existingSup) {
            suppliers.push({
              name: cleanSup,
              contact: '8608414322',
              location: 'Direct Mandi / Farm Gate',
              balanceDue: total,
              active: true
            });
          } else {
            existingSup.balanceDue = (existingSup.balanceDue || 0) + total;
          }
        }

        // Inventory sync
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
        importedCount++;
      }
    }
  });

  if (importedCount > 0) {
    saveState();
    document.getElementById('uploadModal').classList.remove('open');
    document.getElementById('excelPasteArea').value = '';
    alert(`Successfully processed ${importedCount} purchase items into batch ${batchId}!`);
  } else {
    alert('Could not parse rows. Please ensure lines are comma separated with: Date, Supplier, Items, Boxes, Qty, Rate, Total');
  }
});

// ==========================================
// 10. PAYMENT SETTLEMENT MODAL (CASH / UPI)
// ==========================================

let currentSettlePurchase = null;
let currentSettleMode = 'Cash';
let settleCurrentDate = new Date();

function updateSettleDateUI() {
  const display = formatDDMMYY(settleCurrentDate);
  const compact = formatCompactDDMMYY(settleCurrentDate);
  const human = getHumanDate(settleCurrentDate);
  const rel = getRelativeTag(settleCurrentDate);

  const badge = document.getElementById('settleDdmmyyBadge');
  const disp = document.getElementById('settleDateDisplay');
  const sub = document.getElementById('settleDateSub');
  if (badge) badge.textContent = `DDMMYY: ${compact}`;
  if (disp) disp.textContent = display;
  if (sub) sub.textContent = `${human} (${rel})`;
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
  if (!currentUser) {
    openAuthModal();
    return;
  }

  // Role check: Only ADMIN and MANAGER can settle payments
  if (currentUser.role !== 'ADMIN' && currentUser.role !== 'MANAGER') {
    alert(`Access Denied: Payment settlement is restricted to Managers and Administrators (Your role: ${currentUser.role})`);
    return;
  }

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
  currentSettlePurchase.settledBy = currentUser ? currentUser.fullName : 'Admin';

  // Deduct from supplier balanceDue
  const sup = suppliers.find(s => s.name.trim().toLowerCase() === currentSettlePurchase.supplier.trim().toLowerCase());
  if (sup && sup.balanceDue) {
    sup.balanceDue = Math.max(0, sup.balanceDue - currentSettlePurchase.total);
  }

  saveState();
  document.getElementById('settlePaymentModal').classList.remove('open');
  alert(`Payment for ${currentSettlePurchase.batchId} successfully settled on ${settlementDateStr} via ${currentSettleMode}! Ref: ${ref}`);
});

// ==========================================
// 11. IMPORT MOBILE APP BACKUP & CLOUD SYNC
// ==========================================

// Open Import Modal
document.getElementById('openImportBackupModalBtn')?.addEventListener('click', () => {
  document.getElementById('importBackupModal').classList.add('open');
});

// Live preview function for backup JSON
function updateBackupPreview(rawJson) {
  const statusEl = document.getElementById('backupPreviewStatus');
  if (!statusEl) return;
  const trimmed = (rawJson || '').trim();
  if (!trimmed || !trimmed.startsWith('{')) {
    statusEl.style.display = 'none';
    return;
  }
  try {
    const data = JSON.parse(trimmed);
    const iCount = Array.isArray(data.items) ? data.items.length : (Array.isArray(data.inventory) ? data.inventory.length : 0);
    const pCount = Array.isArray(data.purchases) ? data.purchases.length : 0;
    const sCount = Array.isArray(data.suppliers) ? data.suppliers.length : 0;
    const uCount = Array.isArray(data.users) ? data.users.length : 0;
    statusEl.style.display = 'block';
    statusEl.style.background = '#ecfdf5';
    statusEl.style.color = '#065f46';
    statusEl.style.borderColor = '#a7f3d0';
    statusEl.innerHTML = `✓ <strong>Valid Backup JSON:</strong> ${iCount} Items • ${pCount} Purchases • ${sCount} Suppliers • ${uCount} Users detected`;
  } catch (err) {
    statusEl.style.display = 'block';
    statusEl.style.background = '#fef2f2';
    statusEl.style.color = '#991b1b';
    statusEl.style.borderColor = '#fecaca';
    statusEl.innerHTML = `✕ <strong>Invalid JSON:</strong> Please check text syntax (${err.message})`;
  }
}

document.getElementById('backupJsonPasteArea')?.addEventListener('input', (e) => {
  updateBackupPreview(e.target.value);
});

// Paste from Clipboard in Web Browser
document.getElementById('pasteFromClipboardBtn')?.addEventListener('click', async () => {
  try {
    const text = await navigator.clipboard.readText();
    if (text) {
      document.getElementById('backupJsonPasteArea').value = text;
      updateBackupPreview(text);
    } else {
      alert('Clipboard appears to be empty.');
    }
  } catch (err) {
    alert('Clipboard read permission was not granted. Please paste manually into the text box (Ctrl+V / Cmd+V).');
  }
});

// File upload handler for backup JSON
document.getElementById('backupFileInput')?.addEventListener('change', (e) => {
  const file = e.target.files[0];
  if (!file) return;
  const reader = new FileReader();
  reader.onload = (event) => {
    const content = event.target.result;
    document.getElementById('backupJsonPasteArea').value = content;
    updateBackupPreview(content);
  };
  reader.readAsText(file);
});

// Confirm Import Mobile App JSON
document.getElementById('confirmImportBackupBtn')?.addEventListener('click', () => {
  const raw = document.getElementById('backupJsonPasteArea').value.trim();
  if (!raw) {
    alert('Please paste or choose a valid JSON backup exported from the mobile app');
    return;
  }

  try {
    const data = JSON.parse(raw);
    let importedPurchases = 0;
    let importedItems = 0;
    let importedSuppliers = 0;
    let importedUsers = 0;

    // 1. Import Items -> Inventory (supports both "items" and "inventory")
    const rawItems = Array.isArray(data.items) ? data.items : (Array.isArray(data.inventory) ? data.inventory : null);
    if (rawItems) {
      inventory = rawItems.map(it => {
        const stock = parseFloat(it.currentStockKgs !== undefined ? it.currentStockKgs : it.stockKgs) || 0;
        const boxes = parseInt(it.totalBoxes !== undefined ? it.totalBoxes : it.boxes) || 0;
        const rate = parseFloat(it.defaultRatePerKg !== undefined ? it.defaultRatePerKg : it.avgRate) || 0;
        let icon = it.icon;
        if (!icon) {
          const lower = (it.name || '').toLowerCase();
          if (lower.includes('tomato')) icon = '🍅';
          else if (lower.includes('onion')) icon = '🧅';
          else if (lower.includes('carrot')) icon = '🥕';
          else if (lower.includes('potato')) icon = '🥔';
          else if (lower.includes('chilli') || lower.includes('chili')) icon = '🌶️';
          else if (lower.includes('cauliflower') || lower.includes('cabbage')) icon = '🥦';
          else if (lower.includes('capsicum')) icon = '🫑';
          else icon = '🥬';
        }
        return {
          name: it.name,
          icon: icon,
          stockKgs: stock,
          boxes: boxes,
          avgRate: rate,
          category: it.category || 'Vegetables'
        };
      });
      importedItems = inventory.length;
    }

    // 2. Import Purchases
    if (Array.isArray(data.purchases)) {
      purchases = data.purchases.map(p => {
        const item = p.itemName || p.item || 'Vegetable';
        const supplier = p.supplierName || p.supplier || 'Direct Mandi Farmer';
        const total = parseFloat(p.totalAmount !== undefined ? p.totalAmount : p.total) || 0;
        const isPaid = p.status === 'PAID' || p.paid === true;
        return {
          date: p.date,
          batchId: p.batchId || ('PO-' + (p.id || Date.now())),
          item: item,
          boxes: parseInt(p.boxes) || 0,
          qtyKgs: parseFloat(p.qtyKgs) || 0,
          rate: parseFloat(p.rate) || 0,
          total: total,
          supplier: supplier,
          paid: isPaid,
          settlementDate: p.settlementDate || (isPaid ? p.date : ''),
          settlementMode: p.paymentMethod || p.settlementMode || 'Cash',
          recordedBy: p.recordedBy || 'Mobile App Sync'
        };
      });
      importedPurchases = purchases.length;
    }

    // 3. Import Suppliers
    if (Array.isArray(data.suppliers)) {
      suppliers = data.suppliers.map(s => ({
        name: s.name,
        contact: s.phone || s.contact || '8608414322',
        location: s.address || s.location || 'Mandi Yard',
        balanceDue: parseFloat(s.outstandingPayable !== undefined ? s.outstandingPayable : s.balanceDue) || 0,
        active: true
      }));
      importedSuppliers = suppliers.length;
    }

    // 4. Import Users
    if (Array.isArray(data.users)) {
      data.users.forEach(u => {
        const existing = users.find(x => x.mobileNumber === u.mobileNumber);
        if (!existing) {
          users.push({
            id: u.id || Date.now(),
            mobileNumber: u.mobileNumber,
            fullName: u.fullName,
            role: u.role || 'PURCHASER',
            pinHash: '5147',
            department: u.department || 'Procurement',
            isActive: u.isActive !== false,
            isApproved: u.isApproved !== false,
            createdAt: Date.now(),
            lastLoginAt: 0
          });
          importedUsers++;
        }
      });
      saveUsers();
    }

    saveState();
    document.getElementById('importBackupModal').classList.remove('open');
    document.getElementById('backupJsonPasteArea').value = '';
    const statusEl = document.getElementById('backupPreviewStatus');
    if (statusEl) statusEl.style.display = 'none';

    alert(`Direct Mobile Sync Import Successful!\n\n✓ ${importedPurchases} Purchases\n✓ ${importedItems} Warehouse Inventory Items\n✓ ${importedSuppliers} Mandi Suppliers\n✓ ${importedUsers} Staff Accounts`);
  } catch (err) {
    alert('Failed to parse JSON backup. Error: ' + err.message);
  }
});

// Clear / Reset All Data
document.getElementById('clearWebDataBtn')?.addEventListener('click', () => {
  if (!currentUser || currentUser.role !== 'ADMIN') {
    alert('Access restricted: Only Administrators can reset database records.');
    return;
  }

  if (confirm('Are you sure you want to clear all purchases, inventory, and supplier records? This will reset all tables to empty.')) {
    purchases = [];
    inventory = [];
    suppliers = [];
    saveState();
    alert('All procurement and stock records have been reset.');
  }
});

// Helper function to build 100% Cross-Platform Compatible Backup JSON
function generateFullSyncPayload() {
  const currentDomain = localStorage.getItem('lakshana_cloudflare_worker_url') || '';
  
  // Format items for Android Room ItemEntity
  const items = inventory.map((it, idx) => ({
    id: idx + 1,
    name: it.name,
    code: 'VEG-' + (1000 + idx),
    category: it.category || 'Vegetables',
    unit: 'kgs',
    currentStockKgs: it.stockKgs || 0,
    totalBoxes: it.boxes || 0,
    defaultRatePerKg: it.avgRate || 0,
    minStockThresholdKgs: 50,
    description: ''
  }));

  // Format purchases for Android Room PurchaseEntryEntity
  const formattedPurchases = purchases.map((p, idx) => ({
    id: idx + 1,
    batchId: p.batchId || ('PO-' + (1000 + idx)),
    date: p.date,
    itemName: p.item,
    item: p.item,
    boxes: p.boxes || 0,
    qtyKgs: p.qtyKgs || 0,
    rate: p.rate || 0,
    totalAmount: p.total || 0,
    total: p.total || 0,
    supplierName: p.supplier || 'Wholesale Supplier',
    supplier: p.supplier || 'Wholesale Supplier',
    status: p.paid ? 'PAID' : 'PENDING',
    paid: !!p.paid,
    paymentMethod: p.settlementMode || 'Cash',
    stripePaymentIntentId: null,
    notes: p.settlementRef || ''
  }));

  // Format suppliers for Android Room SupplierEntity
  const formattedSuppliers = suppliers.map((s, idx) => ({
    id: idx + 1,
    name: s.name,
    contactPerson: s.name,
    phone: s.contact || '8608414322',
    contact: s.contact || '8608414322',
    email: '',
    address: s.location || 'Mandi Yard',
    location: s.location || 'Mandi Yard',
    outstandingPayable: s.balanceDue || 0,
    balanceDue: s.balanceDue || 0
  }));

  // Format transactions for settled purchases
  const transactions = purchases.filter(p => p.paid).map((p, idx) => ({
    id: idx + 1,
    stripePaymentIntentId: p.settlementRef || ('TXN-' + (100000 + idx)),
    purchaseBatchId: p.batchId,
    supplierName: p.supplier,
    amount: p.total,
    currency: 'INR',
    paymentMethod: p.settlementMode || 'Cash',
    status: 'SUCCESS',
    timestamp: Date.now()
  }));

  return {
    app: 'Lakshana Veggie',
    version: '2.0',
    domain: currentDomain,
    exportedAt: Date.now(),
    exportDate: new Date().toISOString(),
    items: items,
    inventory: inventory, // web alias
    purchases: formattedPurchases,
    suppliers: formattedSuppliers,
    transactions: transactions,
    users: users
  };
}

// Export JSON Backup
document.getElementById('exportBackupBtn')?.addEventListener('click', () => {
  const data = generateFullSyncPayload();
  const jsonStr = "data:text/json;charset=utf-8," + encodeURIComponent(JSON.stringify(data, null, 2));
  const dlAnchor = document.createElement('a');
  dlAnchor.setAttribute("href", jsonStr);
  dlAnchor.setAttribute("download", `lakshana_backup_${new Date().toISOString().slice(0,10)}.json`);
  dlAnchor.click();
});

// Copy Web Sync Payload directly for Mobile App
document.getElementById('copySyncPayloadBtn')?.addEventListener('click', () => {
  const data = generateFullSyncPayload();
  const jsonText = JSON.stringify(data, null, 2);
  navigator.clipboard.writeText(jsonText).then(() => {
    alert(`Web Sync Payload copied to clipboard!\n\nPayload contains:\n- ${data.items.length} Items\n- ${data.purchases.length} Purchases\n- ${data.suppliers.length} Suppliers\n- ${data.users.length} Users\n\nIn your Android app, tap Online Sync Hub ➔ "Paste JSON" or Website tab ➔ "Paste JSON Text" to import instantly!`);
  }).catch(() => {
    prompt('Copy this sync JSON and paste into mobile app:', jsonText);
  });
});

// Trigger Real Sync with Custom Domain / Backend
// Two-Way Sync Merger: Merges incoming records from Mobile App into Web Portal
function mergeRemoteDataIntoLocal(data) {
  let changed = false;
  const remoteItems = Array.isArray(data.items) ? data.items : (Array.isArray(data.inventory) ? data.inventory : null);
  if (remoteItems && remoteItems.length > 0) {
    remoteItems.forEach(it => {
      const existing = inventory.find(i => (i.name || '').toLowerCase() === (it.name || '').toLowerCase());
      const stock = parseFloat(it.currentStockKgs !== undefined ? it.currentStockKgs : it.stockKgs) || 0;
      const boxes = parseInt(it.totalBoxes !== undefined ? it.totalBoxes : it.boxes) || 0;
      const rate = parseFloat(it.defaultRatePerKg !== undefined ? it.defaultRatePerKg : it.avgRate) || 0;
      if (!existing) {
        inventory.push({
          name: it.name,
          icon: it.icon || '🥬',
          stockKgs: stock,
          boxes: boxes,
          avgRate: rate,
          category: it.category || 'Vegetables'
        });
        changed = true;
      }
    });
  }

  if (Array.isArray(data.purchases) && data.purchases.length > 0) {
    data.purchases.forEach(rp => {
      const bId = rp.batchId || ('PO-' + (rp.id || Date.now()));
      if (!purchases.some(p => p.batchId === bId)) {
        purchases.unshift({
          date: rp.date,
          batchId: bId,
          item: rp.itemName || rp.item || 'Vegetable',
          boxes: parseInt(rp.boxes) || 0,
          qtyKgs: parseFloat(rp.qtyKgs) || 0,
          rate: parseFloat(rp.rate) || 0,
          total: parseFloat(rp.totalAmount !== undefined ? rp.totalAmount : rp.total) || 0,
          supplier: rp.supplierName || rp.supplier || 'Direct Mandi Farmer',
          paid: rp.status === 'PAID' || rp.paid === true,
          settlementDate: rp.settlementDate || '',
          settlementMode: rp.paymentMethod || rp.settlementMode || 'Cash',
          recordedBy: rp.recordedBy || 'Mobile App Sync'
        });
        changed = true;
      }
    });
  }

  if (Array.isArray(data.suppliers) && data.suppliers.length > 0) {
    data.suppliers.forEach(rs => {
      if (!suppliers.some(s => (s.name || '').toLowerCase() === (rs.name || '').toLowerCase())) {
        suppliers.push({
          name: rs.name,
          contact: rs.phone || rs.contact || '8608414322',
          location: rs.address || rs.location || 'Mandi Yard',
          balanceDue: parseFloat(rs.outstandingPayable !== undefined ? rs.outstandingPayable : rs.balanceDue) || 0,
          active: true
        });
        changed = true;
      }
    });
  }

  if (changed) {
    localStorage.setItem('lakshana_web_purchases', JSON.stringify(purchases));
    localStorage.setItem('lakshana_web_inventory', JSON.stringify(inventory));
    localStorage.setItem('lakshana_web_suppliers', JSON.stringify(suppliers));
    renderAll();
  }
}

// Trigger Real Sync with Custom Domain / Backend
async function triggerRealSync(showFeedback = true) {
  const statusEl = document.getElementById('cloudSyncStatus');
  const btn = document.getElementById('syncNowBtn');
  const triggerBtn = document.getElementById('triggerSyncNow');
  
  if (statusEl) {
    statusEl.querySelector('.status-text').textContent = 'Syncing...';
    statusEl.querySelector('.status-dot').style.background = '#f59e0b';
  }
  if (btn) btn.disabled = true;
  if (triggerBtn) triggerBtn.disabled = true;

  const DEFAULT_DOMAIN = 'https://lakshanaveggie.trade/api/v1/sync';
  const workerUrl = localStorage.getItem('lakshana_cloudflare_worker_url') || DEFAULT_DOMAIN;

  let endpoint = workerUrl.trim().replace(/\/+$/, '');
  if (!endpoint.endsWith('/api/v1/sync') && !endpoint.endsWith('/sync')) {
    endpoint = `${endpoint}/api/v1/sync`;
  }

  try {
    const payload = generateFullSyncPayload();
    const res = await fetch(endpoint, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    if (res.ok) {
      const data = await res.json();
      const now = new Date();
      const timeStr = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
      if (statusEl) {
        statusEl.querySelector('.status-text').textContent = 'Cloud Synchronized';
        statusEl.querySelector('.status-dot').style.background = '#10b981';
      }
      const timeLabel = document.getElementById('lastSyncTimeLabel');
      if (timeLabel) timeLabel.textContent = `Synced at ${timeStr}`;

      // Automatically merge any records that were added from the Android Mobile App
      if (data && (Array.isArray(data.items) || Array.isArray(data.purchases) || Array.isArray(data.inventory))) {
        mergeRemoteDataIntoLocal(data);
      }

      if (showFeedback) {
        alert(`Online Sync Successful (HTTP ${res.status})!\n\nSynchronized with ${endpoint} at ${timeStr}\n• ${inventory.length} Stock Items\n• ${purchases.length} Purchases\n• ${suppliers.length} Suppliers`);
      }
    } else {
      throw new Error(`Sync Endpoint returned HTTP ${res.status}`);
    }
  } catch (err) {
    if (statusEl) {
      statusEl.querySelector('.status-text').textContent = 'Sync Ready (Local)';
      statusEl.querySelector('.status-dot').style.background = '#10b981';
    }
    if (showFeedback) {
      alert(`Cloud Sync Endpoint Info:\n${err.message}\n\nSync is configured for ${endpoint}. Local changes are queued and will automatically sync when online.`);
    }
  } finally {
    if (btn) btn.disabled = false;
    if (triggerBtn) triggerBtn.disabled = false;
  }
}

// Automatic 15-second background sync loop to pull records entered on mobile app
setInterval(() => {
  if (currentUser) {
    triggerRealSync(false);
  }
}, 15000);

document.getElementById('syncNowBtn')?.addEventListener('click', () => triggerRealSync(true));
document.getElementById('triggerSyncNow')?.addEventListener('click', () => triggerRealSync(true));

// Cloudflare Worker URL Config
const savedWorkerUrl = localStorage.getItem('lakshana_cloudflare_worker_url') || 'https://lakshanaveggie.trade/api/v1/sync';
const domainInput = document.getElementById('customDomainInput');
if (domainInput) domainInput.value = savedWorkerUrl;

document.getElementById('saveDomainBtn')?.addEventListener('click', () => {
  let url = domainInput.value.trim();
  if (!url) {
    alert('Please enter your Cloudflare Worker URL (e.g. https://your-worker.workers.dev)');
    return;
  }
  if (!url.startsWith('http://') && !url.startsWith('https://')) {
    url = 'https://' + url;
  }
  localStorage.setItem('lakshana_cloudflare_worker_url', url);
  domainInput.value = url;
  alert(`Cloudflare Worker URL saved!\nURL: ${url}\n\nTesting sync now...`);
  triggerRealSync(true);
});

document.getElementById('testDomainPingBtn')?.addEventListener('click', async () => {
  let url = domainInput.value.trim();
  const feedback = document.getElementById('domainTestFeedback');
  if (!url) {
    alert('Please enter your Cloudflare Worker URL first');
    return;
  }
  if (!url.startsWith('http://') && !url.startsWith('https://')) {
    url = 'https://' + url;
  }
  feedback.style.display = 'block';
  feedback.style.background = '#eff6ff';
  feedback.style.color = '#1d4ed8';
  feedback.textContent = '⏳ Testing connection to ' + url + '...';

  try {
    const pingEndpoint = (url.endsWith('/api/v1/sync') || url.endsWith('/sync')) ? url : url.replace(/\/$/, '') + '/api/v1/sync';
    const res = await fetch(pingEndpoint, { method: 'GET' });
    
    if (res.ok) {
      feedback.style.background = '#ecfdf5';
      feedback.style.color = '#065f46';
      feedback.innerHTML = `✓ <strong>Connected!</strong> Cloudflare Worker is online and responding (HTTP ${res.status}).`;
    } else {
      feedback.style.background = '#fffbeb';
      feedback.style.color = '#b45309';
      feedback.innerHTML = `⚠️ Worker reached, but status code is HTTP ${res.status}. Check endpoint route.`;
    }
  } catch (e) {
    feedback.style.background = '#fef2f2';
    feedback.style.color = '#991b1b';
    feedback.innerHTML = `✕ <strong>Connection Failed:</strong> ${e.message}. Verify your Cloudflare Worker URL.`;
  }
});

// ==========================================
// 12. EXPORT CENTER: WEB APP ZIP & CSV EXPORTS
// ==========================================

// Open Export Hub Modal
document.getElementById('openExportHubBtn')?.addEventListener('click', () => {
  document.getElementById('exportHubModal')?.classList.add('open');
});

// CSV Downloader Helper
function downloadCsvFile(csvContent, fileName) {
  const blob = new Blob(["\uFEFF" + csvContent], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.setAttribute('href', url);
  link.setAttribute('download', fileName);
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}

// 1. Export Purchases to CSV / Excel
function exportPurchasesToCsv() {
  if (!purchases || purchases.length === 0) {
    alert('No purchases recorded yet to export.');
    return;
  }
  const headers = ['Date', 'Batch ID', 'Item Name', 'Boxes/Crates', 'Quantity (Kgs)', 'Rate per Kg (Rs)', 'Total Amount (Rs)', 'Supplier Name', 'Payment Status', 'Payment Method'];
  const rows = purchases.map(p => [
    `"${p.date || ''}"`,
    `"${p.batchId || ''}"`,
    `"${(p.item || '').replace(/"/g, '""')}"`,
    p.boxes || 0,
    p.qtyKgs || 0,
    p.rate || 0,
    p.total || 0,
    `"${(p.supplier || '').replace(/"/g, '""')}"`,
    p.paid ? 'PAID' : 'PENDING',
    `"${p.settlementMode || 'Cash'}"`
  ]);
  const csvContent = [headers.join(','), ...rows.map(r => r.join(','))].join('\r\n');
  const dateStr = new Date().toISOString().slice(0, 10);
  downloadCsvFile(csvContent, `lakshana_purchases_${dateStr}.csv`);
}

// 2. Export Inventory Stock to CSV
function exportInventoryToCsv() {
  if (!inventory || inventory.length === 0) {
    alert('No inventory commodities found to export.');
    return;
  }
  const headers = ['Vegetable Item', 'Category', 'Stock Available (Kgs)', 'Total Boxes / Crates', 'Avg Rate per Kg (Rs)', 'Status'];
  const rows = inventory.map(item => [
    `"${(item.name || '').replace(/"/g, '""')}"`,
    `"${(item.category || 'Vegetables').replace(/"/g, '""')}"`,
    item.stockKgs || 0,
    item.boxes || 0,
    item.avgRate || 0,
    (item.stockKgs < 50 ? 'LOW STOCK' : 'IN STOCK')
  ]);
  const csvContent = [headers.join(','), ...rows.map(r => r.join(','))].join('\r\n');
  const dateStr = new Date().toISOString().slice(0, 10);
  downloadCsvFile(csvContent, `lakshana_inventory_${dateStr}.csv`);
}

// 3. Export Suppliers Directory to CSV
function exportSuppliersToCsv() {
  if (!suppliers || suppliers.length === 0) {
    alert('No suppliers recorded yet to export.');
    return;
  }
  const headers = ['Supplier Name', 'Contact Phone', 'Mandi Location / Yard', 'Outstanding Balance Due (Rs)', 'Active Status'];
  const rows = suppliers.map(s => [
    `"${(s.name || '').replace(/"/g, '""')}"`,
    `"${s.contact || ''}"`,
    `"${(s.location || '').replace(/"/g, '""')}"`,
    s.balanceDue || 0,
    s.active !== false ? 'ACTIVE' : 'INACTIVE'
  ]);
  const csvContent = [headers.join(','), ...rows.map(r => r.join(','))].join('\r\n');
  const dateStr = new Date().toISOString().slice(0, 10);
  downloadCsvFile(csvContent, `lakshana_suppliers_${dateStr}.csv`);
}

// 4. Download Complete Web App (.ZIP)
async function downloadWebAppZip() {
  const btn = document.getElementById('downloadWebAppZipBtn');
  const originalText = btn ? btn.innerHTML : '📦 Download Web App ZIP';
  if (btn) {
    btn.disabled = true;
    btn.innerHTML = '⏳ Packaging Web App...';
  }

  try {
    if (typeof JSZip === 'undefined') {
      throw new Error('JSZip library is still loading. Please check your internet connection and try again.');
    }

    const zip = new JSZip();

    // Helper to fetch file or provide fallback
    async function getFileContent(filePath) {
      try {
        const response = await fetch(filePath);
        if (response.ok) {
          return await response.text();
        }
      } catch (e) {
        console.warn('Direct fetch failed for', filePath, e);
      }
      return null;
    }

    // Try fetching live files, otherwise synthesize from document
    let indexHtml = await getFileContent('index.html');
    if (!indexHtml) {
      indexHtml = '<!DOCTYPE html>\n' + document.documentElement.outerHTML;
    }

    let stylesCss = await getFileContent('styles.css');
    if (!stylesCss) {
      stylesCss = '/* Lakshana Veggie Stylesheet */\n';
      for (const sheet of document.styleSheets) {
        try {
          if (sheet.cssRules) {
            for (const rule of sheet.cssRules) {
              stylesCss += rule.cssText + '\n';
            }
          }
        } catch (e) { /* ignore cross-origin */ }
      }
    }

    let appJs = await getFileContent('app.js');
    if (!appJs) {
      appJs = '// Lakshana Veggie App Script\n';
    }

    let workerJs = await getFileContent('cloudflare-worker.js');
    if (!workerJs) {
      workerJs = `// Cloudflare Worker for lakshanaveggie.trade
let inMemorySyncCache = null;

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);

    if (request.method === 'OPTIONS') {
      return new Response(null, {
        status: 204,
        headers: {
          'Access-Control-Allow-Origin': '*',
          'Access-Control-Allow-Methods': 'GET, POST, OPTIONS, PUT, DELETE',
          'Access-Control-Allow-Headers': 'Content-Type, Authorization, X-Requested-With',
          'Access-Control-Max-Age': '86400'
        }
      });
    }

    const cleanPath = url.pathname.replace(/\\/+$/, '');
    if (cleanPath === '/api/v1/sync' || cleanPath === '/sync' || cleanPath.startsWith('/api/v1/sync')) {
      if (request.method === 'POST') {
        try {
          const payload = await request.json();
          inMemorySyncCache = {
            ...payload,
            syncedAt: Date.now(),
            serverTime: new Date().toISOString()
          };
          if (env && env.SYNC_KV) {
            await env.SYNC_KV.put('latest_sync', JSON.stringify(inMemorySyncCache));
          }
          return new Response(JSON.stringify({
            status: 'success',
            syncedAt: Date.now(),
            items: inMemorySyncCache.items || inMemorySyncCache.inventory || [],
            purchases: inMemorySyncCache.purchases || [],
            suppliers: inMemorySyncCache.suppliers || [],
            message: 'Real-time sync successful with Cloudflare Worker'
          }), {
            status: 200,
            headers: { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' }
          });
        } catch (err) {
          return new Response(JSON.stringify({ status: 'error', message: err.message }), {
            status: 400,
            headers: { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' }
          });
        }
      }

      let data = inMemorySyncCache;
      if (env && env.SYNC_KV) {
        const raw = await env.SYNC_KV.get('latest_sync');
        if (raw) data = JSON.parse(raw);
      }
      return new Response(JSON.stringify(data || { status: 'online', domain: 'cloudflare-worker' }), {
        status: 200,
        headers: { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' }
      });
    }

    return fetch(request);
  }
};`;
    }

    let packageJson = await getFileContent('package.json');
    if (!packageJson) {
      packageJson = JSON.stringify({
        name: "lakshana-veggie-web-portal",
        version: "2.0.0",
        scripts: { start: "npx serve ." }
      }, null, 2);
    }

    let readmeMd = await getFileContent('README.md');
    if (!readmeMd) {
      readmeMd = '# Lakshana Veggie Web Portal\n\nDirect Mandi Procurement & Inventory Management Web Portal.\nPowered by Cloudflare Worker for online synchronization.';
    }

    // Add files to zip
    zip.file('index.html', indexHtml);
    zip.file('styles.css', stylesCss);
    zip.file('app.js', appJs);
    zip.file('cloudflare-worker.js', workerJs);
    zip.file('package.json', packageJson);
    zip.file('README.md', readmeMd);

    // Also include a pre-populated backup payload in the zip
    const currentData = generateFullSyncPayload();
    zip.file('initial_data_backup.json', JSON.stringify(currentData, null, 2));

    const content = await zip.generateAsync({ type: 'blob' });
    const dateStr = new Date().toISOString().slice(0, 10);
    const fileName = `lakshana-veggie-web-app_${dateStr}.zip`;

    const url = URL.createObjectURL(content);
    const a = document.createElement('a');
    a.href = url;
    a.download = fileName;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);

    alert(`Web App successfully packaged and downloaded!\nFile: ${fileName}\n\nIncludes website files and cloudflare-worker.js for instant online synchronization.`);
  } catch (err) {
    alert('Failed to package web app: ' + err.message);
  } finally {
    if (btn) {
      btn.disabled = false;
      btn.innerHTML = originalText;
    }
  }
}

// Bind Export Event Listeners
document.getElementById('exportPurchasesCsvBtn')?.addEventListener('click', exportPurchasesToCsv);
document.getElementById('exportPurchasesHubBtn')?.addEventListener('click', exportPurchasesToCsv);
document.getElementById('exportInventoryCsvBtn')?.addEventListener('click', exportInventoryToCsv);
document.getElementById('exportInventoryHubBtn')?.addEventListener('click', exportInventoryToCsv);
document.getElementById('exportSuppliersCsvBtn')?.addEventListener('click', exportSuppliersToCsv);
document.getElementById('exportSuppliersHubBtn')?.addEventListener('click', exportSuppliersToCsv);
document.getElementById('downloadWebAppZipBtn')?.addEventListener('click', downloadWebAppZip);

document.getElementById('exportBackupHubBtn')?.addEventListener('click', () => {
  document.getElementById('exportBackupBtn')?.click();
});

document.getElementById('copySyncPayloadHubBtn')?.addEventListener('click', () => {
  document.getElementById('copySyncPayloadBtn')?.click();
});

// ==========================================
// 13. INITIALIZATION
// ==========================================

initPortalAuth();
updateNewPoDateUI();
updateSettleDateUI();
updateAuthVisibility();
renderHeaderAuth();
if (currentUser) {
  renderAll();
}
