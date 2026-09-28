package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.ItemEntity
import com.example.data.model.PurchaseEntryEntity
import com.example.data.model.StockMovementEntity
import com.example.data.model.SupplierEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.data.remote.CloudSyncManager
import com.example.data.remote.StripePaymentResult
import com.example.data.remote.SyncState
import com.example.data.remote.SyncSummary
import com.example.data.repository.AppRepository
import com.example.data.util.DailyPurchaseRow
import com.example.data.util.ExcelParseResult
import com.example.data.util.ExcelSheetParser
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

sealed interface NavigationDestination {
    data object Dashboard : NavigationDestination
    data object Items : NavigationDestination
    data object Purchase : NavigationDestination
    data object Supplier : NavigationDestination
    data object Transaction : NavigationDestination
    data object Reports : NavigationDestination
    data object Inventory : NavigationDestination
    data object AdminUsers : NavigationDestination
    data object Website : NavigationDestination
}

data class AuthUiState(
    val mobileInput: String = "",
    val enteredPin: String = "",
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isLoading: Boolean = false,
    val isRegisterMode: Boolean = false,
    val registerFullName: String = "",
    val registerMobile: String = "",
    val registerPin: String = "",
    val registerConfirmPin: String = "",
    val registerDepartment: String = "Procurement",
    val registrationStatusMessage: String? = null
)

enum class PaymentMode {
    CASH, UPI, STRIPE
}

data class PaymentModalState(
    val isOpen: Boolean = false,
    val purchase: PurchaseEntryEntity? = null,
    val batchId: String? = null,
    val amount: Double = 0.0,
    val supplierName: String = "",
    val activeTab: PaymentMode = PaymentMode.CASH,
    val isProcessing: Boolean = false,
    val settlementDate: String? = null,
    val successRefId: String? = null,
    val successMode: String? = null,
    val errorMessage: String? = null,
    val result: StripePaymentResult? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application, viewModelScope)
    val repository = AppRepository(database)

    // Current navigation tab
    private val _currentDestination = MutableStateFlow<NavigationDestination>(NavigationDestination.Dashboard)
    val currentDestination: StateFlow<NavigationDestination> = _currentDestination.asStateFlow()

    // Auth State
    val currentUser: StateFlow<UserEntity?> = repository.currentUser
    private val _authUiState = MutableStateFlow(AuthUiState())
    val authUiState: StateFlow<AuthUiState> = _authUiState.asStateFlow()

    // Snackbars / Toast events
    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    // Data streams
    val items: StateFlow<List<ItemEntity>> = repository.allItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val suppliers: StateFlow<List<SupplierEntity>> = repository.allSuppliers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val purchases: StateFlow<List<PurchaseEntryEntity>> = repository.allPurchases
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stockMovements: StateFlow<List<StockMovementEntity>> = repository.allStockMovements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val users: StateFlow<List<UserEntity>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingUsers: StateFlow<List<UserEntity>> = repository.pendingUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Excel Daily Sheet Bulk Upload State
    private val _excelParseResult = MutableStateFlow<ExcelParseResult?>(null)
    val excelParseResult: StateFlow<ExcelParseResult?> = _excelParseResult.asStateFlow()

    private val _isExcelProcessing = MutableStateFlow(false)
    val isExcelProcessing: StateFlow<Boolean> = _isExcelProcessing.asStateFlow()

    // Stripe Payment Workflow Modal State
    private val _paymentModalState = MutableStateFlow(PaymentModalState())
    val paymentModalState: StateFlow<PaymentModalState> = _paymentModalState.asStateFlow()

    // Online Cloud Synchronize Engine
    val cloudSyncManager = CloudSyncManager(application, repository)

    private val _syncSummary = MutableStateFlow(
        SyncSummary(
            lastSyncTimestamp = cloudSyncManager.getLastSyncTimestamp(),
            autoSyncEnabled = cloudSyncManager.isAutoSyncEnabled(),
            cloudEndpoint = cloudSyncManager.getCloudEndpoint()
        )
    )
    val syncSummary: StateFlow<SyncSummary> = _syncSummary.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    init {
        // Clear legacy example data once and ensure default Admin exists (no auto-login)
        viewModelScope.launch {
            val prefs = application.getSharedPreferences("lakshana_prefs", Context.MODE_PRIVATE)
            if (!prefs.getBoolean("cleared_example_data_v2", false)) {
                repository.clearAllExampleData()
                prefs.edit().putBoolean("cleared_example_data_v2", true).apply()
            }
            repository.ensureDefaultAdminExists()
            // Auto sync on startup if internet is available
            if (cloudSyncManager.isAutoSyncEnabled() && cloudSyncManager.isNetworkAvailable()) {
                syncNow()
            }
        }
    }

    fun navigateTo(dest: NavigationDestination) {
        _currentDestination.value = dest
    }

    // --- Authentication Actions (Direct Mobile & Security PIN Login) ---
    fun updateMobileInput(mobile: String) {
        _authUiState.value = _authUiState.value.copy(
            mobileInput = mobile.filter { it.isDigit() }.take(12),
            errorMessage = null
        )
    }

    fun updatePinInput(pin: String) {
        _authUiState.value = _authUiState.value.copy(
            enteredPin = pin.filter { it.isDigit() }.take(6),
            errorMessage = null
        )
    }

    fun toggleAuthMode(isRegister: Boolean) {
        _authUiState.value = _authUiState.value.copy(
            isRegisterMode = isRegister,
            errorMessage = null,
            registrationStatusMessage = null
        )
    }

    fun updateRegisterFullName(name: String) {
        _authUiState.value = _authUiState.value.copy(
            registerFullName = name,
            errorMessage = null
        )
    }

    fun updateRegisterMobile(mobile: String) {
        _authUiState.value = _authUiState.value.copy(
            registerMobile = mobile.filter { it.isDigit() }.take(12),
            errorMessage = null
        )
    }

    fun updateRegisterPin(pin: String) {
        _authUiState.value = _authUiState.value.copy(
            registerPin = pin.filter { it.isDigit() }.take(6),
            errorMessage = null
        )
    }

    fun updateRegisterConfirmPin(pin: String) {
        _authUiState.value = _authUiState.value.copy(
            registerConfirmPin = pin.filter { it.isDigit() }.take(6),
            errorMessage = null
        )
    }

    fun updateRegisterDepartment(dept: String) {
        _authUiState.value = _authUiState.value.copy(
            registerDepartment = dept,
            errorMessage = null
        )
    }

    fun clearRegistrationStatusMessage() {
        _authUiState.value = _authUiState.value.copy(registrationStatusMessage = null)
    }

    fun register() {
        val state = _authUiState.value
        val name = state.registerFullName.trim()
        val mobile = state.registerMobile.trim()
        val pin = state.registerPin.trim()
        val confirmPin = state.registerConfirmPin.trim()
        val dept = "Pending Admin Assignment"

        if (name.isBlank()) {
            _authUiState.value = state.copy(errorMessage = "Please enter your full name.")
            return
        }
        if (mobile.length < 8) {
            _authUiState.value = state.copy(errorMessage = "Please enter a valid 10-digit mobile number.")
            return
        }
        if (pin.length < 4) {
            _authUiState.value = state.copy(errorMessage = "Security PIN must be 4 digits.")
            return
        }
        if (pin != confirmPin) {
            _authUiState.value = state.copy(errorMessage = "PINs do not match. Please re-enter identical 4-digit PINs.")
            return
        }

        _authUiState.value = state.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val result = repository.registerUser(name, mobile, pin, dept)
            if (result.isSuccess) {
                val user = result.getOrThrow()
                if (user.role == UserRole.ADMIN) {
                    // Immediate Admin login
                    _authUiState.value = AuthUiState()
                    _toastEvent.emit("Welcome Administrator! Logged in successfully.")
                } else {
                    // Regular user pending approval
                    _authUiState.value = state.copy(
                        isLoading = false,
                        registerFullName = "",
                        registerMobile = "",
                        registerPin = "",
                        registerConfirmPin = "",
                        registrationStatusMessage = "Registration submitted successfully! Your account is awaiting Administrator approval in App Settings."
                    )
                    _toastEvent.emit("Registration submitted! Pending Administrator approval.")
                }
            } else {
                _authUiState.value = state.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Registration failed."
                )
            }
        }
    }

    fun approveUser(userId: Long, assignedRole: UserRole, assignedDepartment: String = "Procurement", onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.approveUserRegistration(userId, assignedRole, assignedDepartment)
            if (result.isSuccess) {
                _toastEvent.emit("User approved: ${assignedRole.displayName} in $assignedDepartment")
                onComplete()
            } else {
                _toastEvent.emit("Failed to approve user: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun rejectUser(userId: Long, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.rejectUserRegistration(userId)
            if (result.isSuccess) {
                _toastEvent.emit("Registration request rejected.")
                onComplete()
            } else {
                _toastEvent.emit("Failed to reject user.")
            }
        }
    }

    fun login() {
        val state = _authUiState.value
        val mobile = state.mobileInput.trim()
        val pin = state.enteredPin.trim()

        if (mobile.length < 8) {
            _authUiState.value = state.copy(errorMessage = "Please enter your registered mobile number.")
            return
        }
        if (pin.length < 4) {
            _authUiState.value = state.copy(errorMessage = "Please enter your 4-digit security PIN.")
            return
        }

        _authUiState.value = state.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val result = repository.loginWithMobileAndPin(mobile, pin)
            if (result.isSuccess) {
                val user = result.getOrThrow()
                _authUiState.value = AuthUiState()
                _toastEvent.emit("Logged in successfully as ${user.fullName} (${user.role.displayName})")
            } else {
                _authUiState.value = state.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Login failed. Check mobile and PIN."
                )
            }
        }
    }

    fun logout() {
        repository.logout()
        _currentDestination.value = NavigationDestination.Dashboard
        _authUiState.value = AuthUiState()
        viewModelScope.launch {
            _toastEvent.emit("Logged out securely.")
        }
    }

    // Admin reset or generate new PIN for a user
    fun adminResetUserPin(userId: Long, newPin: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            if (currentUser.value?.role != UserRole.ADMIN) {
                onResult(false, "Only Administrator can generate or reset user PINs.")
                return@launch
            }
            if (newPin.length < 4) {
                onResult(false, "Security PIN must be at least 4 digits.")
                return@launch
            }
            val success = repository.adminResetUserPin(userId, newPin)
            if (success) {
                _toastEvent.emit("User PIN updated to $newPin successfully")
                onResult(true, "PIN has been generated and updated.")
            } else {
                onResult(false, "Failed to update user PIN.")
            }
        }
    }

    fun changePin(oldPin: String, newPin: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val user = currentUser.value
            if (user == null) {
                onResult(false, "No active user logged in.")
                return@launch
            }
            if (newPin.length < 4) {
                onResult(false, "New PIN must be at least 4 digits.")
                return@launch
            }
            val success = repository.changeUserPin(user.id, oldPin, newPin)
            if (success) {
                _toastEvent.emit("PIN updated successfully!")
                onResult(true, "Security PIN has been updated.")
            } else {
                onResult(false, "Incorrect current PIN. Please check and try again.")
            }
        }
    }

    // --- Admin User Management ---
    fun updateUserRole(userId: Long, newRole: UserRole) {
        viewModelScope.launch {
            repository.updateUserRole(userId, newRole)
            _toastEvent.emit("User role updated to ${newRole.displayName}")
        }
    }

    fun toggleUserStatus(userId: Long, currentActive: Boolean) {
        viewModelScope.launch {
            repository.toggleUserActive(userId, !currentActive)
            _toastEvent.emit("User status set to ${if (!currentActive) "Active" else "Deactivated"}")
        }
    }

    fun createNewUserByAdmin(name: String, mobile: String, role: UserRole, pin: String, dept: String) {
        viewModelScope.launch {
            repository.createNewUser(name, mobile, role, pin, dept)
            _toastEvent.emit("New user $name added with role ${role.displayName}")
        }
    }

    // --- Items & Inventory Management ---
    fun saveItem(item: ItemEntity) {
        viewModelScope.launch {
            repository.addOrUpdateItem(item)
            _toastEvent.emit("Item '${item.name}' saved successfully")
        }
    }

    fun deleteItem(item: ItemEntity) {
        viewModelScope.launch {
            repository.deleteItem(item)
            _toastEvent.emit("Item '${item.name}' removed")
        }
    }

    fun adjustItemStock(item: ItemEntity, newKgs: Double, newBoxes: Int, notes: String) {
        viewModelScope.launch {
            repository.manualStockAdjustment(item, newKgs, newBoxes, notes)
            _toastEvent.emit("Stock for ${item.name} adjusted to $newKgs kgs ($newBoxes boxes)")
        }
    }

    // --- Supplier Management ---
    fun saveSupplier(supplier: SupplierEntity) {
        viewModelScope.launch {
            repository.addOrUpdateSupplier(supplier)
            _toastEvent.emit("Supplier '${supplier.name}' saved")
        }
    }

    fun deleteSupplier(supplier: SupplierEntity) {
        viewModelScope.launch {
            repository.deleteSupplier(supplier)
            _toastEvent.emit("Supplier '${supplier.name}' removed")
        }
    }

    // --- Purchase Management ---
    fun createPurchaseOrder(
        itemName: String,
        boxes: Int,
        qtyKgs: Double,
        rate: Double,
        supplierName: String,
        supplierId: Long?,
        autoUpdateStock: Boolean,
        date: String = com.example.ui.components.DateFormatterUtil.todayDDMMYY()
    ) {
        viewModelScope.launch {
            val total = Math.round(qtyKgs * rate * 100.0) / 100.0
            val poNumber = "PO-" + System.currentTimeMillis().toString().takeLast(6)

            val entry = PurchaseEntryEntity(
                batchId = poNumber,
                date = date,
                itemName = itemName,
                boxes = boxes,
                qtyKgs = qtyKgs,
                rate = rate,
                totalAmount = total,
                supplierId = supplierId,
                supplierName = supplierName,
                isExcelImport = false
            )
            repository.createSinglePurchase(entry, autoUpdateStock)
            _toastEvent.emit("Purchase $poNumber ($date) created (₹$total). Stock updated.")
        }
    }

    fun updatePurchaseOrder(purchase: PurchaseEntryEntity) {
        viewModelScope.launch {
            repository.updatePurchase(purchase)
            _toastEvent.emit("Purchase order #${purchase.id} (${purchase.itemName}) updated")
        }
    }

    fun deletePurchaseOrder(purchase: PurchaseEntryEntity) {
        viewModelScope.launch {
            repository.deletePurchase(purchase)
            _toastEvent.emit("Purchase order #${purchase.id} deleted")
        }
    }

    fun clearAllExampleData() {
        viewModelScope.launch {
            repository.clearAllExampleData()
            _toastEvent.emit("All example data cleared successfully")
        }
    }

    // --- Daily Excel Bulk Upload & Automatic Stock Updates ---
    fun parseDailyExcelText(text: String, defaultSupplier: String = "Farm Wholesale Supply") {
        val result = ExcelSheetParser.parse(text, defaultSupplier)
        _excelParseResult.value = result
    }

    fun loadSampleDailyExcel() {
        parseDailyExcelText(ExcelSheetParser.SAMPLE_DAILY_EXCEL_CSV, "Sunrise Agro Producers")
    }

    fun clearExcelParseResult() {
        _excelParseResult.value = null
    }

    fun commitDailyExcelImport(autoUpdateStock: Boolean = true, onComplete: (batchId: String, totalAmount: Double) -> Unit) {
        val result = _excelParseResult.value ?: return
        if (!result.success || result.rows.isEmpty()) return

        val batchId = "EXCEL-" + SimpleDateFormat("yyyyMMdd-HHmm", Locale.getDefault()).format(Date())

        viewModelScope.launch {
            _isExcelProcessing.value = true
            try {
                val importedCount = repository.importDailyExcelSheet(batchId, result.rows, autoUpdateStock)
                _toastEvent.emit("Imported $importedCount items! Automatic stock updates applied.")
                _excelParseResult.value = null
                onComplete(batchId, result.grandTotalAmount)
            } finally {
                _isExcelProcessing.value = false
            }
        }
    }

    // --- Payment Processing Workflows (Cash & UPI First) ---
    fun openPaymentModalForPurchase(purchase: PurchaseEntryEntity, defaultMode: PaymentMode = PaymentMode.CASH) {
        _paymentModalState.value = PaymentModalState(
            isOpen = true,
            purchase = purchase,
            batchId = purchase.batchId,
            amount = purchase.totalAmount,
            supplierName = purchase.supplierName,
            activeTab = defaultMode,
            isProcessing = false,
            successRefId = null,
            successMode = null,
            result = null
        )
    }

    fun openPaymentModalForBatch(batchId: String, amount: Double, supplierName: String, defaultMode: PaymentMode = PaymentMode.CASH) {
        _paymentModalState.value = PaymentModalState(
            isOpen = true,
            purchase = null,
            batchId = batchId,
            amount = amount,
            supplierName = supplierName,
            activeTab = defaultMode,
            isProcessing = false,
            successRefId = null,
            successMode = null,
            result = null
        )
    }

    fun openStripePaymentForPurchase(purchase: PurchaseEntryEntity) {
        openPaymentModalForPurchase(purchase, PaymentMode.CASH)
    }

    fun openStripePaymentForBatch(batchId: String, amount: Double, supplierName: String) {
        openPaymentModalForBatch(batchId, amount, supplierName, PaymentMode.CASH)
    }

    fun setPaymentActiveTab(mode: PaymentMode) {
        _paymentModalState.value = _paymentModalState.value.copy(activeTab = mode, errorMessage = null)
    }

    fun closePaymentModal() {
        _paymentModalState.value = PaymentModalState(isOpen = false)
    }

    fun closeStripePaymentModal() {
        closePaymentModal()
    }

    fun executeCashPayment(
        cashTendered: Double,
        voucherNo: String,
        notes: String,
        settlementDate: String = com.example.ui.components.DateFormatterUtil.todayDDMMYY()
    ) {
        val state = _paymentModalState.value
        if (!state.isOpen || state.amount <= 0.0) return

        viewModelScope.launch {
            _paymentModalState.value = state.copy(isProcessing = true, errorMessage = null, settlementDate = settlementDate)
            try {
                val receiptId = if (state.purchase != null) {
                    repository.processPurchasePaymentCash(state.purchase, cashTendered, voucherNo, notes, settlementDate)
                } else if (state.batchId != null) {
                    repository.processBatchPaymentCash(state.batchId, state.amount, state.supplierName, voucherNo, settlementDate)
                } else "CASH-PAID"

                _paymentModalState.value = _paymentModalState.value.copy(
                    isProcessing = false,
                    settlementDate = settlementDate,
                    successRefId = receiptId,
                    successMode = "Cash"
                )
                _toastEvent.emit("Cash payment of ₹${String.format(Locale.US, "%.2f", state.amount)} confirmed on $settlementDate! Receipt #$receiptId")
            } catch (e: Exception) {
                _paymentModalState.value = _paymentModalState.value.copy(
                    isProcessing = false,
                    errorMessage = e.localizedMessage ?: "Failed to record cash payment"
                )
            }
        }
    }

    fun executeUpiPayment(
        upiId: String,
        utrNumber: String,
        upiApp: String,
        settlementDate: String = com.example.ui.components.DateFormatterUtil.todayDDMMYY()
    ) {
        val state = _paymentModalState.value
        if (!state.isOpen || state.amount <= 0.0) return

        viewModelScope.launch {
            _paymentModalState.value = state.copy(isProcessing = true, errorMessage = null, settlementDate = settlementDate)
            try {
                val ref = if (state.purchase != null) {
                    repository.processPurchasePaymentUpi(state.purchase, upiId, utrNumber, upiApp, settlementDate)
                } else if (state.batchId != null) {
                    repository.processBatchPaymentUpi(state.batchId, state.amount, state.supplierName, upiId, utrNumber, upiApp, settlementDate)
                } else "UPI-PAID"

                _paymentModalState.value = _paymentModalState.value.copy(
                    isProcessing = false,
                    settlementDate = settlementDate,
                    successRefId = ref,
                    successMode = "UPI ($upiApp)"
                )
                _toastEvent.emit("UPI payment verified on $settlementDate! Ref #$ref")
            } catch (e: Exception) {
                _paymentModalState.value = _paymentModalState.value.copy(
                    isProcessing = false,
                    errorMessage = e.localizedMessage ?: "Failed to verify UPI payment"
                )
            }
        }
    }

    fun executeStripePayment(currency: String = "INR", cardLast4: String = "4242") {
        val state = _paymentModalState.value
        if (!state.isOpen || state.amount <= 0.0) return

        viewModelScope.launch {
            _paymentModalState.value = state.copy(isProcessing = true, errorMessage = null)

            val result: StripePaymentResult = if (state.purchase != null) {
                repository.processPurchasePaymentWithStripe(
                    purchase = state.purchase,
                    currency = currency,
                    cardLast4 = cardLast4
                )
            } else if (state.batchId != null) {
                repository.processBatchPaymentWithStripe(
                    batchId = state.batchId,
                    amount = state.amount,
                    supplierName = state.supplierName,
                    currency = currency,
                    cardLast4 = cardLast4
                )
            } else {
                StripePaymentResult(false, "", "failed", 0, currency, null, "No purchase reference")
            }

            _paymentModalState.value = _paymentModalState.value.copy(
                isProcessing = false,
                result = result,
                successRefId = if (result.isSuccess) result.paymentIntentId else null,
                successMode = if (result.isSuccess) "Stripe Card" else null
            )

            if (result.isSuccess) {
                _toastEvent.emit("Stripe payment of ₹${String.format(Locale.US, "%.2f", state.amount)} processed (${result.paymentIntentId})")
            } else {
                _toastEvent.emit("Payment failed: ${result.errorMessage}")
            }
        }
    }

    // --- Online Cloud Synchronization Actions ---
    fun syncNow() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            _syncSummary.value = _syncSummary.value.copy(syncState = SyncState.SYNCING)
            val result = cloudSyncManager.performOnlineSync()
            if (result.isSuccess) {
                val summary = result.getOrThrow()
                _syncSummary.value = summary
                _toastEvent.emit(summary.statusMessage)
            } else {
                _syncSummary.value = _syncSummary.value.copy(
                    syncState = SyncState.ERROR,
                    statusMessage = "Sync failed: ${result.exceptionOrNull()?.message}"
                )
                _toastEvent.emit("Cloud sync failed. Check internet connection.")
            }
            _isSyncing.value = false
        }
    }

    fun toggleAutoSync(enabled: Boolean) {
        cloudSyncManager.setAutoSyncEnabled(enabled)
        _syncSummary.value = _syncSummary.value.copy(autoSyncEnabled = enabled)
        viewModelScope.launch {
            _toastEvent.emit("Auto-Sync ${if (enabled) "Enabled" else "Disabled"}")
        }
    }

    fun exportDataBackup(onJsonReady: (String) -> Unit) {
        viewModelScope.launch {
            val json = cloudSyncManager.exportFullBackupJson()
            onJsonReady(json)
        }
    }

    fun updateCloudEndpoint(newEndpoint: String) {
        val trimmed = newEndpoint.trim()
        if (trimmed.isNotBlank()) {
            cloudSyncManager.setCloudEndpoint(trimmed)
            _syncSummary.value = _syncSummary.value.copy(cloudEndpoint = trimmed)
            viewModelScope.launch {
                _toastEvent.emit("Cloud Database Domain updated: $trimmed")
            }
        }
    }

    fun testCloudEndpoint(endpoint: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = cloudSyncManager.testConnection(endpoint)
            if (result.isSuccess) {
                val msg = result.getOrThrow()
                _toastEvent.emit(msg)
                onResult(true, msg)
            } else {
                val err = result.exceptionOrNull()?.message ?: "Connection failed"
                _toastEvent.emit("Domain test failed: $err")
                onResult(false, err)
            }
        }
    }

    fun restoreDataBackup(jsonString: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = cloudSyncManager.restoreFromBackupJson(jsonString)
            if (result.isSuccess) {
                val count = result.getOrThrow()
                _toastEvent.emit("Restored $count records from backup successfully!")
                onResult(true, "Successfully restored $count records.")
                syncNow()
            } else {
                onResult(false, "Restore failed: ${result.exceptionOrNull()?.message}")
            }
        }
    }
}
