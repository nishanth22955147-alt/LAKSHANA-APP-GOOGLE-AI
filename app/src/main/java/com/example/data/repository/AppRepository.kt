package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.ItemEntity
import com.example.data.model.MovementType
import com.example.data.model.PurchaseEntryEntity
import com.example.data.model.PurchaseStatus
import com.example.data.model.StockMovementEntity
import com.example.data.model.SupplierEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionStatus
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.data.remote.StripePaymentClient
import com.example.data.remote.StripePaymentResult
import com.example.data.util.DailyPurchaseRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class AppRepository(
    private val database: AppDatabase,
    val stripeClient: StripePaymentClient = StripePaymentClient()
) {
    private val userDao = database.userDao()
    private val itemDao = database.itemDao()
    private val supplierDao = database.supplierDao()
    private val purchaseDao = database.purchaseDao()
    private val transactionDao = database.transactionDao()
    private val stockMovementDao = database.stockMovementDao()

    // Current Session State
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    // Public reactive flows
    val allItems: Flow<List<ItemEntity>> = itemDao.getAllItems()
    val allSuppliers: Flow<List<SupplierEntity>> = supplierDao.getAllSuppliers()
    val allPurchases: Flow<List<PurchaseEntryEntity>> = purchaseDao.getAllPurchases()
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val allStockMovements: Flow<List<StockMovementEntity>> = stockMovementDao.getAllMovements()
    val allUsers: Flow<List<UserEntity>> = userDao.getAllUsers()
    val pendingUsers: Flow<List<UserEntity>> = userDao.getPendingUsers()

    suspend fun getUserByMobile(mobile: String): UserEntity? = withContext(Dispatchers.IO) {
        userDao.getUserByMobile(mobile)
    }

    suspend fun ensureDefaultAdminExists(): UserEntity = withContext(Dispatchers.IO) {
        val allUsersList = userDao.getAllUsersList()
        val existingAdmin = allUsersList.firstOrNull { it.role == UserRole.ADMIN }
            ?: userDao.getUserByMobile("8608414322")
        if (existingAdmin != null) {
            existingAdmin
        } else {
            val newAdmin = UserEntity(
                mobileNumber = "8608414322",
                fullName = "Lakshana Admin",
                role = UserRole.ADMIN,
                pinHash = "5147",
                isApproved = true,
                department = "Executive Admin"
            )
            val id = userDao.insertUser(newAdmin)
            newAdmin.copy(id = id)
        }
    }

    suspend fun loginWithMobileAndPin(mobile: String, pin: String): Result<UserEntity> = withContext(Dispatchers.IO) {
        val cleanMobile = mobile.trim()
        val cleanPin = pin.trim()

        if (cleanMobile.isBlank()) {
            return@withContext Result.failure(Exception("Please enter your registered mobile number."))
        }
        if (cleanPin.isBlank()) {
            return@withContext Result.failure(Exception("Please enter your security PIN."))
        }

        var user = userDao.getUserByMobile(cleanMobile)

        if (user == null) {
            val allUsersList = userDao.getAllUsersList()
            val hasAdmin = allUsersList.any { it.role == UserRole.ADMIN }

            // Default Admin setup or login with admin mobile 8608414322 and PIN 5147
            if ((!hasAdmin && cleanPin == "5147") || (cleanMobile == "8608414322" && cleanPin == "5147")) {
                val newAdmin = UserEntity(
                    mobileNumber = "8608414322",
                    fullName = "Lakshana Admin",
                    role = UserRole.ADMIN,
                    pinHash = "5147",
                    isApproved = true,
                    department = "Executive Admin"
                )
                val id = userDao.insertUser(newAdmin)
                val created = newAdmin.copy(id = id)
                _currentUser.value = created
                return@withContext Result.success(created)
            } else {
                return@withContext Result.failure(Exception("Mobile number not registered. Please switch to the 'Register' tab to create an account."))
            }
        }

        // Validate user PIN
        if (user.pinHash != cleanPin) {
            return@withContext Result.failure(Exception("Incorrect Security PIN. If forgotten, please contact Administrator."))
        }

        // Check if user is approved
        if (!user.isApproved) {
            return@withContext Result.failure(Exception("Account Pending Approval: Your registration has been submitted and is awaiting Administrator approval in App Settings."))
        }

        if (!user.isActive) {
            return@withContext Result.failure(Exception("This account is currently deactivated. Please contact Administrator."))
        }

        userDao.updateLastLogin(user.id, System.currentTimeMillis())
        val loggedIn = user.copy(lastLoginAt = System.currentTimeMillis())
        _currentUser.value = loggedIn
        Result.success(loggedIn)
    }

    suspend fun registerUser(
        fullName: String,
        mobileNumber: String,
        pin: String,
        department: String = "Pending Admin Assignment"
    ): Result<UserEntity> = withContext(Dispatchers.IO) {
        val cleanMobile = mobileNumber.trim()
        val cleanPin = pin.trim()
        val cleanName = fullName.trim()

        if (cleanName.isBlank()) {
            return@withContext Result.failure(Exception("Please enter your full name."))
        }
        if (cleanMobile.length < 8) {
            return@withContext Result.failure(Exception("Please enter a valid 10-digit mobile number."))
        }
        if (cleanPin.length < 4) {
            return@withContext Result.failure(Exception("Security PIN must be 4 digits."))
        }

        val existingUser = userDao.getUserByMobile(cleanMobile)
        if (existingUser != null) {
            return@withContext Result.failure(Exception("An account with mobile $cleanMobile is already registered. Please sign in."))
        }

        // First time Admin registration with mobile 8608414322 and PIN 5147
        val isAdminRegistration = (cleanMobile == "8608414322" && cleanPin == "5147")
        val newUser = UserEntity(
            mobileNumber = cleanMobile,
            fullName = if (isAdminRegistration && (cleanName.isBlank() || cleanName.equals("User", true))) "Lakshana Admin" else cleanName,
            role = if (isAdminRegistration) UserRole.ADMIN else UserRole.PURCHASER,
            pinHash = cleanPin,
            isActive = true,
            isApproved = isAdminRegistration, // Admin is approved immediately; regular users need approval in App Settings
            department = if (isAdminRegistration) "Executive Admin" else "Pending Admin Assignment"
        )
        val id = userDao.insertUser(newUser)
        val saved = newUser.copy(id = id)

        if (isAdminRegistration) {
            _currentUser.value = saved
        }
        Result.success(saved)
    }

    suspend fun approveUserRegistration(userId: Long, assignedRole: UserRole, assignedDepartment: String = "Procurement"): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            userDao.approveUser(userId, assignedRole, assignedDepartment)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rejectUserRegistration(userId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val user = userDao.getUserById(userId)
            if (user != null) {
                userDao.deleteUser(user)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun setLoggedInUser(user: UserEntity?) {
        _currentUser.value = user
    }

    suspend fun registerOrUpdatePin(
        mobile: String,
        fullName: String,
        pin: String,
        role: UserRole = UserRole.PURCHASER
    ): UserEntity = withContext(Dispatchers.IO) {
        val existing = userDao.getUserByMobile(mobile)
        val user = if (existing != null) {
            existing.copy(fullName = fullName, pinHash = pin, lastLoginAt = System.currentTimeMillis())
        } else {
            UserEntity(
                mobileNumber = mobile,
                fullName = fullName,
                pinHash = pin,
                role = role
            )
        }
        val id = userDao.insertUser(user)
        val saved = user.copy(id = if (existing != null) existing.id else id)
        _currentUser.value = saved
        saved
    }

    fun logout() {
        _currentUser.value = null
    }

    suspend fun changeUserPin(userId: Long, oldPin: String, newPin: String): Boolean = withContext(Dispatchers.IO) {
        val user = userDao.getUserById(userId) ?: return@withContext false
        if (user.pinHash != oldPin) {
            return@withContext false
        }
        val updated = user.copy(pinHash = newPin)
        userDao.updateUser(updated)
        if (_currentUser.value?.id == userId) {
            _currentUser.value = updated
        }
        true
    }

    suspend fun adminResetUserPin(userId: Long, newPin: String): Boolean = withContext(Dispatchers.IO) {
        val user = userDao.getUserById(userId) ?: return@withContext false
        val updated = user.copy(pinHash = newPin)
        userDao.updateUser(updated)
        if (_currentUser.value?.id == userId) {
            _currentUser.value = updated
        }
        true
    }

    // Admin User & Role Management
    suspend fun updateUserRole(userId: Long, newRole: UserRole) = withContext(Dispatchers.IO) {
        userDao.updateUserRole(userId, newRole)
        if (_currentUser.value?.id == userId) {
            _currentUser.value = _currentUser.value?.copy(role = newRole)
        }
    }

    suspend fun toggleUserActive(userId: Long, isActive: Boolean) = withContext(Dispatchers.IO) {
        userDao.updateUserStatus(userId, isActive)
    }

    suspend fun createNewUser(
        fullName: String,
        mobile: String,
        role: UserRole,
        pin: String,
        department: String
    ): Long = withContext(Dispatchers.IO) {
        val user = UserEntity(
            mobileNumber = mobile,
            fullName = fullName,
            role = role,
            pinHash = pin,
            department = department
        )
        userDao.insertUser(user)
    }

    // Items & Inventory Management
    suspend fun addOrUpdateItem(item: ItemEntity): Long = withContext(Dispatchers.IO) {
        if (item.id == 0L) {
            itemDao.insertItem(item)
        } else {
            itemDao.updateItem(item)
            item.id
        }
    }

    suspend fun deleteItem(item: ItemEntity) = withContext(Dispatchers.IO) {
        itemDao.deleteItem(item)
    }

    suspend fun manualStockAdjustment(
        item: ItemEntity,
        newStockKgs: Double,
        newBoxes: Int,
        notes: String
    ) = withContext(Dispatchers.IO) {
        val changeKgs = newStockKgs - item.currentStockKgs
        val changeBoxes = newBoxes - item.totalBoxes
        itemDao.setStock(item.id, newStockKgs, newBoxes)

        val movement = StockMovementEntity(
            itemName = item.name,
            movementType = MovementType.ADJUSTMENT,
            qtyKgsChange = changeKgs,
            boxesChange = changeBoxes,
            resultingStockKgs = newStockKgs,
            reasonOrNotes = notes,
            recordedBy = _currentUser.value?.fullName ?: "Staff"
        )
        stockMovementDao.insertMovement(movement)
    }

    // Supplier Management
    suspend fun addOrUpdateSupplier(supplier: SupplierEntity): Long = withContext(Dispatchers.IO) {
        if (supplier.id == 0L) {
            supplierDao.insertSupplier(supplier)
        } else {
            supplierDao.updateSupplier(supplier)
            supplier.id
        }
    }

    suspend fun deleteSupplier(supplier: SupplierEntity) = withContext(Dispatchers.IO) {
        supplierDao.deleteSupplier(supplier)
    }

    // Purchase Management & Daily Excel Sheet Import
    suspend fun updatePurchase(purchase: PurchaseEntryEntity) = withContext(Dispatchers.IO) {
        purchaseDao.updatePurchase(purchase)
    }

    suspend fun deletePurchase(purchase: PurchaseEntryEntity) = withContext(Dispatchers.IO) {
        purchaseDao.deletePurchase(purchase)
    }

    // Clear all example and demo data from all tables
    suspend fun clearAllExampleData() = withContext(Dispatchers.IO) {
        supplierDao.deleteAllSuppliers()
        purchaseDao.deleteAllPurchases()
        itemDao.deleteAllItems()
        transactionDao.deleteAllTransactions()
        stockMovementDao.deleteAllMovements()
    }
    suspend fun createSinglePurchase(
        entry: PurchaseEntryEntity,
        autoUpdateStock: Boolean
    ): Long = withContext(Dispatchers.IO) {
        val cleanItemName = entry.itemName.trim()
        val cleanSupplierName = entry.supplierName.trim()

        // 1. Automatically sync item to catalog if not existing
        val existingItem = itemDao.getItemByName(cleanItemName)
        var updatedStock: Double = entry.qtyKgs
        if (existingItem != null) {
            if (autoUpdateStock) {
                itemDao.addStock(existingItem.id, entry.qtyKgs, entry.boxes)
                updatedStock = existingItem.currentStockKgs + entry.qtyKgs
            } else {
                updatedStock = existingItem.currentStockKgs
            }
        } else {
            val newItem = ItemEntity(
                name = cleanItemName,
                code = "SKU-" + cleanItemName.take(3).uppercase().replace(" ", "") + "-" + (100..999).random(),
                category = "Vegetables",
                currentStockKgs = if (autoUpdateStock) entry.qtyKgs else 0.0,
                totalBoxes = if (autoUpdateStock) entry.boxes else 0,
                defaultRatePerKg = entry.rate
            )
            itemDao.insertItem(newItem)
            updatedStock = if (autoUpdateStock) entry.qtyKgs else 0.0
        }

        // 2. Automatically sync supplier name to supplier list if not existing
        var finalSupplierId = entry.supplierId
        if (cleanSupplierName.isNotBlank() && !cleanSupplierName.equals("Wholesale Supplier", ignoreCase = true)) {
            val existingSupplier = if (finalSupplierId != null) {
                supplierDao.getSupplierById(finalSupplierId) ?: supplierDao.getSupplierByName(cleanSupplierName)
            } else {
                supplierDao.getSupplierByName(cleanSupplierName)
            }

            if (existingSupplier != null) {
                finalSupplierId = existingSupplier.id
                supplierDao.addPayable(existingSupplier.id, entry.totalAmount)
            } else {
                val newSupplier = SupplierEntity(
                    name = cleanSupplierName,
                    contactPerson = cleanSupplierName,
                    phone = "",
                    email = "",
                    address = "Mandi / Farm Gate Procurement",
                    outstandingPayable = entry.totalAmount,
                    totalPurchasesAmount = entry.totalAmount,
                    totalPaidAmount = 0.0,
                    vegetableCategories = cleanItemName
                )
                finalSupplierId = supplierDao.insertSupplier(newSupplier)
            }
        } else if (finalSupplierId != null) {
            supplierDao.addPayable(finalSupplierId, entry.totalAmount)
        }

        val entryToSave = if (entry.supplierId != finalSupplierId) {
            entry.copy(supplierId = finalSupplierId)
        } else {
            entry
        }

        val purchaseId = purchaseDao.insertPurchase(entryToSave)

        if (autoUpdateStock) {
            stockMovementDao.insertMovement(
                StockMovementEntity(
                    itemName = cleanItemName,
                    movementType = MovementType.MANUAL_PURCHASE,
                    qtyKgsChange = entry.qtyKgs,
                    boxesChange = entry.boxes,
                    resultingStockKgs = updatedStock,
                    referenceBatchOrPo = "PO-$purchaseId",
                    reasonOrNotes = "Manual Purchase Inward (${cleanSupplierName.ifBlank { "Supplier" }})",
                    recordedBy = _currentUser.value?.fullName ?: "Purchaser"
                )
            )
        }

        purchaseId
    }

    suspend fun importDailyExcelSheet(
        batchId: String,
        rows: List<DailyPurchaseRow>,
        autoUpdateStock: Boolean = true
    ): Int = withContext(Dispatchers.IO) {
        var count = 0
        val movements = mutableListOf<StockMovementEntity>()

        for (row in rows) {
            val cleanSupplierName = row.supplierName.trim()
            val cleanItemName = row.itemName.trim()
            var supplierId: Long? = null

            // Automatically sync supplier to supplier list if not existing
            if (cleanSupplierName.isNotBlank() && !cleanSupplierName.equals("Wholesale Supplier", ignoreCase = true)) {
                val existingSupplier = supplierDao.getSupplierByName(cleanSupplierName)
                if (existingSupplier != null) {
                    supplierId = existingSupplier.id
                    supplierDao.addPayable(existingSupplier.id, row.calculatedTotal)
                } else {
                    val newSupplier = SupplierEntity(
                        name = cleanSupplierName,
                        contactPerson = cleanSupplierName,
                        phone = "",
                        email = "",
                        address = "Mandi / Farm Gate Procurement",
                        outstandingPayable = row.calculatedTotal,
                        totalPurchasesAmount = row.calculatedTotal,
                        totalPaidAmount = 0.0,
                        vegetableCategories = cleanItemName
                    )
                    supplierId = supplierDao.insertSupplier(newSupplier)
                }
            }

            val purchase = PurchaseEntryEntity(
                batchId = batchId,
                date = row.date,
                itemName = cleanItemName,
                boxes = row.boxes,
                qtyKgs = row.qtyKgs,
                rate = row.rate,
                totalAmount = row.calculatedTotal,
                supplierId = supplierId,
                supplierName = if (cleanSupplierName.isNotBlank()) cleanSupplierName else "Wholesale Supplier",
                status = PurchaseStatus.PENDING_PAYMENT,
                isExcelImport = true,
                notes = "Daily Excel Sheet Import"
            )
            purchaseDao.insertPurchase(purchase)
            count++

            if (autoUpdateStock) {
                val existingItem = itemDao.getItemByName(cleanItemName)
                val newStockKgs: Double
                if (existingItem != null) {
                    itemDao.addStock(existingItem.id, row.qtyKgs, row.boxes)
                    newStockKgs = existingItem.currentStockKgs + row.qtyKgs
                } else {
                    val code = "SKU-" + cleanItemName.take(3).uppercase().replace(" ", "") + "-" + (10..99).random()
                    val newItem = ItemEntity(
                        name = cleanItemName,
                        code = code,
                        category = "Daily Import",
                        currentStockKgs = row.qtyKgs,
                        totalBoxes = row.boxes,
                        defaultRatePerKg = row.rate
                    )
                    itemDao.insertItem(newItem)
                    newStockKgs = row.qtyKgs
                }

                movements.add(
                    StockMovementEntity(
                        itemName = cleanItemName,
                        movementType = MovementType.EXCEL_BULK_IMPORT,
                        qtyKgsChange = row.qtyKgs,
                        boxesChange = row.boxes,
                        resultingStockKgs = newStockKgs,
                        referenceBatchOrPo = batchId,
                        reasonOrNotes = "Daily Excel sheet bulk upload: ${row.boxes} boxes / ${row.qtyKgs} kgs (${cleanSupplierName.ifBlank { "Supplier" }})",
                        recordedBy = _currentUser.value?.fullName ?: "Bulk Importer"
                    )
                )
            }
        }

        if (movements.isNotEmpty()) {
            stockMovementDao.insertMovements(movements)
        }
        count
    }

    // --- Cash Payment Workflow ---
    suspend fun processPurchasePaymentCash(
        purchase: PurchaseEntryEntity,
        cashTendered: Double,
        voucherOrReceiptNo: String,
        notes: String = "",
        settlementDate: String = SimpleDateFormat("dd-MM-yy", Locale.getDefault()).format(Date())
    ): String = withContext(Dispatchers.IO) {
        val receiptId = voucherOrReceiptNo.ifBlank { "CASH-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date()) }
        val settlementMillis = try {
            com.example.ui.components.DateFormatterUtil.parseToCalendar(settlementDate).timeInMillis
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
        val transaction = TransactionEntity(
            stripePaymentIntentId = receiptId,
            purchaseId = purchase.id,
            purchaseBatchId = purchase.batchId,
            supplierName = purchase.supplierName,
            amount = purchase.totalAmount,
            currency = "INR",
            paymentMethod = "Cash Payment • Settled $settlementDate",
            status = TransactionStatus.SUCCEEDED,
            processedBy = _currentUser.value?.fullName ?: "Cashier",
            receiptUrl = null,
            timestamp = settlementMillis
        )
        transactionDao.insertTransaction(transaction)

        val updatedNotes = if (purchase.notes.isBlank()) "Settled via Cash on $settlementDate (Voucher: $receiptId)" else "${purchase.notes} | Settled on $settlementDate (Cash $receiptId)"
        purchaseDao.updateStatusAndNotes(purchase.id, PurchaseStatus.PAID_CASH, receiptId, updatedNotes)

        purchase.supplierId?.let { sId ->
            supplierDao.recordPayment(sId, purchase.totalAmount)
        }
        receiptId
    }

    suspend fun processBatchPaymentCash(
        batchId: String,
        amount: Double,
        supplierName: String,
        voucherOrReceiptNo: String,
        settlementDate: String = SimpleDateFormat("dd-MM-yy", Locale.getDefault()).format(Date())
    ): String = withContext(Dispatchers.IO) {
        val receiptId = voucherOrReceiptNo.ifBlank { "CASH-BATCH-" + System.currentTimeMillis().toString().takeLast(6) }
        val settlementMillis = try {
            com.example.ui.components.DateFormatterUtil.parseToCalendar(settlementDate).timeInMillis
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
        val transaction = TransactionEntity(
            stripePaymentIntentId = receiptId,
            purchaseBatchId = batchId,
            supplierName = supplierName,
            amount = amount,
            currency = "INR",
            paymentMethod = "Cash Payment (Bulk Settlement) • $settlementDate",
            status = TransactionStatus.SUCCEEDED,
            processedBy = _currentUser.value?.fullName ?: "Cashier",
            timestamp = settlementMillis
        )
        transactionDao.insertTransaction(transaction)
        purchaseDao.updateBatchPaymentStatus(batchId, PurchaseStatus.PAID_CASH, receiptId)
        receiptId
    }

    // --- UPI Payment Workflow ---
    suspend fun processPurchasePaymentUpi(
        purchase: PurchaseEntryEntity,
        upiId: String,
        utrNumber: String,
        upiApp: String,
        settlementDate: String = SimpleDateFormat("dd-MM-yy", Locale.getDefault()).format(Date())
    ): String = withContext(Dispatchers.IO) {
        val utrRef = if (utrNumber.isNotBlank()) "UPI-UTR-$utrNumber" else "UPI-" + System.currentTimeMillis().toString().takeLast(10)
        val settlementMillis = try {
            com.example.ui.components.DateFormatterUtil.parseToCalendar(settlementDate).timeInMillis
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
        val transaction = TransactionEntity(
            stripePaymentIntentId = utrRef,
            purchaseId = purchase.id,
            purchaseBatchId = purchase.batchId,
            supplierName = purchase.supplierName,
            amount = purchase.totalAmount,
            currency = "INR",
            paymentMethod = "UPI ($upiApp - $upiId) • Settled $settlementDate",
            status = TransactionStatus.SUCCEEDED,
            processedBy = _currentUser.value?.fullName ?: "Staff",
            receiptUrl = "upi://pay?pa=$upiId&pn=${purchase.supplierName}&am=${purchase.totalAmount}&tr=$utrRef",
            timestamp = settlementMillis
        )
        transactionDao.insertTransaction(transaction)

        val updatedNotes = if (purchase.notes.isBlank()) "Settled via UPI on $settlementDate (Ref: $utrRef)" else "${purchase.notes} | Settled on $settlementDate (UPI $utrRef)"
        purchaseDao.updateStatusAndNotes(purchase.id, PurchaseStatus.PAID_UPI, utrRef, updatedNotes)

        purchase.supplierId?.let { sId ->
            supplierDao.recordPayment(sId, purchase.totalAmount)
        }
        utrRef
    }

    suspend fun processBatchPaymentUpi(
        batchId: String,
        amount: Double,
        supplierName: String,
        upiId: String,
        utrNumber: String,
        upiApp: String,
        settlementDate: String = SimpleDateFormat("dd-MM-yy", Locale.getDefault()).format(Date())
    ): String = withContext(Dispatchers.IO) {
        val utrRef = if (utrNumber.isNotBlank()) "UPI-UTR-$utrNumber" else "UPI-BATCH-" + System.currentTimeMillis().toString().takeLast(10)
        val settlementMillis = try {
            com.example.ui.components.DateFormatterUtil.parseToCalendar(settlementDate).timeInMillis
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
        val transaction = TransactionEntity(
            stripePaymentIntentId = utrRef,
            purchaseBatchId = batchId,
            supplierName = supplierName,
            amount = amount,
            currency = "INR",
            paymentMethod = "UPI ($upiApp - $upiId) • $settlementDate",
            status = TransactionStatus.SUCCEEDED,
            processedBy = _currentUser.value?.fullName ?: "Staff",
            timestamp = settlementMillis
        )
        transactionDao.insertTransaction(transaction)
        purchaseDao.updateBatchPaymentStatus(batchId, PurchaseStatus.PAID_UPI, utrRef)
        utrRef
    }

    // Stripe Payment Processing Workflows
    suspend fun processPurchasePaymentWithStripe(
        purchase: PurchaseEntryEntity,
        currency: String = "INR",
        cardLast4: String = "4242"
    ): StripePaymentResult = withContext(Dispatchers.IO) {
        val result = stripeClient.processPurchasePayment(
            amount = purchase.totalAmount,
            currency = currency,
            description = "Purchase of ${purchase.itemName} (${purchase.qtyKgs} kgs) - Ref #${purchase.id}",
            supplierName = purchase.supplierName,
            purchaseBatchOrId = "PUR-${purchase.id}",
            cardLast4 = cardLast4
        )

        if (result.isSuccess) {
            // Record successful transaction
            val transaction = TransactionEntity(
                stripePaymentIntentId = result.paymentIntentId,
                purchaseId = purchase.id,
                purchaseBatchId = purchase.batchId,
                supplierName = purchase.supplierName,
                amount = purchase.totalAmount,
                currency = currency.uppercase(),
                paymentMethod = "Stripe Card (Visa •••• $cardLast4)",
                status = TransactionStatus.SUCCEEDED,
                receiptUrl = result.receiptUrl,
                processedBy = _currentUser.value?.fullName ?: "Staff"
            )
            transactionDao.insertTransaction(transaction)

            // Update purchase entry status
            purchaseDao.updateStatus(purchase.id, PurchaseStatus.PAID, result.paymentIntentId)

            // Update supplier balance if mapped
            purchase.supplierId?.let { sId ->
                supplierDao.recordPayment(sId, purchase.totalAmount)
            }
        }
        result
    }

    suspend fun processBatchPaymentWithStripe(
        batchId: String,
        amount: Double,
        supplierName: String,
        currency: String = "INR",
        cardLast4: String = "4242"
    ): StripePaymentResult = withContext(Dispatchers.IO) {
        val result = stripeClient.processPurchasePayment(
            amount = amount,
            currency = currency,
            description = "Bulk Excel Purchase Settlement - Batch $batchId",
            supplierName = supplierName,
            purchaseBatchOrId = batchId,
            cardLast4 = cardLast4
        )

        if (result.isSuccess) {
            val transaction = TransactionEntity(
                stripePaymentIntentId = result.paymentIntentId,
                purchaseBatchId = batchId,
                supplierName = supplierName,
                amount = amount,
                currency = currency.uppercase(),
                paymentMethod = "Stripe Card (Visa •••• $cardLast4)",
                status = TransactionStatus.SUCCEEDED,
                receiptUrl = result.receiptUrl,
                processedBy = _currentUser.value?.fullName ?: "Staff"
            )
            transactionDao.insertTransaction(transaction)

            purchaseDao.updateBatchPaymentStatus(batchId, PurchaseStatus.PAID, result.paymentIntentId)
        }
        result
    }
}
