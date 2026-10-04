package com.example.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.model.*
import com.example.data.repository.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CloudSyncManager(
    private val context: Context,
    private val repository: AppRepository
) {
    private val prefs = context.getSharedPreferences("lakshana_sync_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LAST_SYNC = "last_sync_time"
        private const val KEY_AUTO_SYNC = "auto_sync_enabled"
        private const val KEY_ENDPOINT = "cloudflare_worker_endpoint"
        const val DEFAULT_ENDPOINT = ""

        fun normalizeEndpoint(raw: String): String {
            var trimmed = raw.trim()
            if (trimmed.isEmpty()) return ""
            if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
                trimmed = "https://$trimmed"
            }
            trimmed = trimmed.trimEnd('/')
            return if (trimmed.endsWith("/api/v1/sync") || trimmed.endsWith("/sync")) {
                trimmed
            } else {
                "$trimmed/api/v1/sync"
            }
        }
    }

    fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun getLastSyncTimestamp(): Long {
        return prefs.getLong(KEY_LAST_SYNC, 0L)
    }

    fun setLastSyncTimestamp(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_SYNC, timestamp).apply()
    }

    fun isAutoSyncEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_SYNC, true)
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SYNC, enabled).apply()
    }

    fun getCloudEndpoint(): String {
        return prefs.getString(KEY_ENDPOINT, DEFAULT_ENDPOINT) ?: DEFAULT_ENDPOINT
    }

    fun setCloudEndpoint(endpoint: String) {
        prefs.edit().putString(KEY_ENDPOINT, endpoint).apply()
    }

    suspend fun testConnection(endpoint: String): Result<String> = withContext(Dispatchers.IO) {
        val trimmed = normalizeEndpoint(endpoint)
        if (trimmed.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Cloudflare Worker URL cannot be empty. Enter your default domain (e.g. https://your-worker.workers.dev)"))
        }
        if (!isNetworkAvailable()) {
            return@withContext Result.failure(IllegalStateException("No active internet connection detected on this device."))
        }

        try {
            val url = java.net.URL(trimmed)
            val host = url.host ?: "host"
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.instanceFollowRedirects = true
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "Lakshana-Android-Sync/2.0")
            conn.setRequestProperty("Accept", "application/json, text/html, */*")

            val responseCode = conn.responseCode
            conn.disconnect()

            if (responseCode in 200..299) {
                Result.success("Connection Successful (HTTP $responseCode)! Cloudflare Worker at '$host' is online and responding.")
            } else if (responseCode in 300..399) {
                Result.success("Worker Active (HTTP $responseCode Redirect). Server at '$host' is online.")
            } else if (responseCode == 404) {
                Result.failure(Exception("HTTP 404 Not Found at '$host'. The worker is reachable, but the endpoint route was not matched."))
            } else {
                Result.failure(Exception("HTTP $responseCode received from '$host'."))
            }
        } catch (e: java.net.UnknownHostException) {
            Result.failure(Exception("Worker Domain Not Found: Could not resolve '${e.message}'. Check your Cloudflare Worker name."))
        } catch (e: java.net.ConnectException) {
            Result.failure(Exception("Connection Refused: Server at ${e.message} is offline."))
        } catch (e: java.net.SocketTimeoutException) {
            Result.failure(Exception("Connection Timed Out: Server took too long to respond (>6s)."))
        } catch (e: javax.net.ssl.SSLException) {
            Result.failure(Exception("SSL Certificate Error: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun performOnlineSync(): Result<SyncSummary> = withContext(Dispatchers.IO) {
        val isOnline = isNetworkAvailable()
        val rawEndpoint = getCloudEndpoint()
        val endpoint = normalizeEndpoint(rawEndpoint)
        val autoSync = isAutoSyncEnabled()

        val items = repository.allItems.first()
        val purchases = repository.allPurchases.first()
        val suppliers = repository.allSuppliers.first()
        val transactions = repository.allTransactions.first()
        val users = repository.allUsers.first()
        val movements = repository.allStockMovements.first()

        if (endpoint.isEmpty()) {
            return@withContext Result.failure(IllegalStateException("No Cloudflare Worker URL configured. Open Online Sync Hub and enter your Cloudflare Worker URL (e.g. https://your-worker.workers.dev)."))
        }

        if (!isOnline) {
            val lastTime = getLastSyncTimestamp()
            return@withContext Result.success(
                SyncSummary(
                    syncState = SyncState.OFFLINE,
                    lastSyncTimestamp = lastTime,
                    itemsCount = items.size,
                    purchasesCount = purchases.size,
                    suppliersCount = suppliers.size,
                    transactionsCount = transactions.size,
                    usersCount = users.size,
                    stockMovementsCount = movements.size,
                    isOnline = false,
                    statusMessage = "Offline Mode: Device has no active internet connection. All local changes are safely queued for next sync.",
                    cloudEndpoint = endpoint,
                    autoSyncEnabled = autoSync
                )
            )
        }

        try {
            // Build full export payload
            val fullPayloadJson = exportFullBackupJson()

            // Perform real HTTP POST to endpoint
            val url = java.net.URL(endpoint)
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 7000
            conn.readTimeout = 7000
            conn.doOutput = true
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("User-Agent", "Lakshana-Android-Sync/2.0")

            conn.outputStream.use { os ->
                os.write(fullPayloadJson.toByteArray(Charsets.UTF_8))
                os.flush()
            }

            val responseCode = conn.responseCode
            val responseText = try {
                if (responseCode in 200..299) {
                    conn.inputStream.bufferedReader().use { it.readText() }
                } else {
                    conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                }
            } catch (e: Exception) {
                ""
            }
            conn.disconnect()

            val now = System.currentTimeMillis()
            setLastSyncTimestamp(now)
            val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(now))

            if (responseCode in 200..299) {
                // If server returned updated remote data (bidirectional sync), merge it
                if (responseText.isNotBlank() && (responseText.contains("\"items\"") || responseText.contains("\"purchases\""))) {
                    try {
                        restoreFromBackupJson(responseText)
                    } catch (_: Exception) {}
                }

                val summary = SyncSummary(
                    syncState = SyncState.SUCCESS,
                    lastSyncTimestamp = now,
                    itemsCount = items.size,
                    purchasesCount = purchases.size,
                    suppliersCount = suppliers.size,
                    transactionsCount = transactions.size,
                    usersCount = users.size,
                    stockMovementsCount = movements.size,
                    isOnline = true,
                    statusMessage = "Online Synced at $timeStr (HTTP $responseCode). Uploaded ${items.size} items, ${purchases.size} purchases & ${suppliers.size} suppliers to Cloud.",
                    cloudEndpoint = endpoint,
                    autoSyncEnabled = autoSync
                )
                Result.success(summary)
            } else if (responseCode == 404) {
                // Server reachable but path not found
                val summary = SyncSummary(
                    syncState = SyncState.ERROR,
                    lastSyncTimestamp = getLastSyncTimestamp(),
                    itemsCount = items.size,
                    purchasesCount = purchases.size,
                    suppliersCount = suppliers.size,
                    transactionsCount = transactions.size,
                    usersCount = users.size,
                    stockMovementsCount = movements.size,
                    isOnline = true,
                    statusMessage = "HTTP 404 at ${url.host}: The web host is online, but /api/v1/sync endpoint is not active. Use 'Export Mobile App JSON' in Website tab for instant manual transfer.",
                    cloudEndpoint = endpoint,
                    autoSyncEnabled = autoSync
                )
                Result.success(summary)
            } else {
                val summary = SyncSummary(
                    syncState = SyncState.ERROR,
                    lastSyncTimestamp = getLastSyncTimestamp(),
                    itemsCount = items.size,
                    purchasesCount = purchases.size,
                    suppliersCount = suppliers.size,
                    transactionsCount = transactions.size,
                    usersCount = users.size,
                    stockMovementsCount = movements.size,
                    isOnline = true,
                    statusMessage = "Server returned HTTP $responseCode. Web host responded with: ${responseText.take(80)}",
                    cloudEndpoint = endpoint,
                    autoSyncEnabled = autoSync
                )
                Result.success(summary)
            }
        } catch (e: java.net.UnknownHostException) {
            val summary = SyncSummary(
                syncState = SyncState.ERROR,
                lastSyncTimestamp = getLastSyncTimestamp(),
                itemsCount = items.size,
                purchasesCount = purchases.size,
                suppliersCount = suppliers.size,
                transactionsCount = transactions.size,
                usersCount = users.size,
                stockMovementsCount = movements.size,
                isOnline = true,
                statusMessage = "DNS Error: Cannot resolve domain '${e.message}'. The domain DNS records (A/CNAME) have not propagated or are not configured at registrar.",
                cloudEndpoint = endpoint,
                autoSyncEnabled = autoSync
            )
            Result.success(summary)
        } catch (e: java.net.ConnectException) {
            val summary = SyncSummary(
                syncState = SyncState.ERROR,
                lastSyncTimestamp = getLastSyncTimestamp(),
                itemsCount = items.size,
                purchasesCount = purchases.size,
                suppliersCount = suppliers.size,
                transactionsCount = transactions.size,
                usersCount = users.size,
                stockMovementsCount = movements.size,
                isOnline = true,
                statusMessage = "Connection Refused: Web server is not running or port is blocked. Local database remains safe.",
                cloudEndpoint = endpoint,
                autoSyncEnabled = autoSync
            )
            Result.success(summary)
        } catch (e: Exception) {
            val summary = SyncSummary(
                syncState = SyncState.ERROR,
                lastSyncTimestamp = getLastSyncTimestamp(),
                itemsCount = items.size,
                purchasesCount = purchases.size,
                suppliersCount = suppliers.size,
                transactionsCount = transactions.size,
                usersCount = users.size,
                stockMovementsCount = movements.size,
                isOnline = true,
                statusMessage = "Sync failed (${e.javaClass.simpleName}): ${e.message ?: "Unknown error"}. Use 1-tap JSON Import/Export in Website tab.",
                cloudEndpoint = endpoint,
                autoSyncEnabled = autoSync
            )
            Result.success(summary)
        }
    }

    suspend fun exportFullBackupJson(): String = withContext(Dispatchers.IO) {
        val items = repository.allItems.first()
        val purchases = repository.allPurchases.first()
        val suppliers = repository.allSuppliers.first()
        val transactions = repository.allTransactions.first()
        val users = repository.allUsers.first()

        val root = JSONObject()
        root.put("app", "Lakshana Veggie")
        root.put("version", "2.0")
        root.put("domain", getCloudEndpoint())
        root.put("exportedAt", System.currentTimeMillis())
        root.put("exportDate", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))

        // Items Array (Mobile native schema)
        val itemsArr = JSONArray()
        val inventoryArr = JSONArray()
        for (item in items) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("code", item.code)
                put("category", item.category)
                put("unit", item.unit)
                put("currentStockKgs", item.currentStockKgs)
                put("totalBoxes", item.totalBoxes)
                put("defaultRatePerKg", item.defaultRatePerKg)
                put("minStockThresholdKgs", item.minStockThresholdKgs)
                put("description", item.description)
                // Also provide web aliases directly
                put("stockKgs", item.currentStockKgs)
                put("boxes", item.totalBoxes)
                put("avgRate", item.defaultRatePerKg)
            }
            itemsArr.put(obj)

            // Web-friendly inventory array
            val invObj = JSONObject().apply {
                put("name", item.name)
                put("category", item.category)
                put("stockKgs", item.currentStockKgs)
                put("boxes", item.totalBoxes)
                put("avgRate", item.defaultRatePerKg)
                put("icon", when {
                    item.name.contains("Tomato", ignoreCase = true) -> "🍅"
                    item.name.contains("Onion", ignoreCase = true) -> "🧅"
                    item.name.contains("Carrot", ignoreCase = true) -> "🥕"
                    item.name.contains("Potato", ignoreCase = true) -> "🥔"
                    item.name.contains("Chilli", ignoreCase = true) || item.name.contains("Chili", ignoreCase = true) -> "🌶️"
                    else -> "🥬"
                })
            }
            inventoryArr.put(invObj)
        }
        root.put("items", itemsArr)
        root.put("inventory", inventoryArr)

        // Purchases Array
        val purchasesArr = JSONArray()
        for (p in purchases) {
            val obj = JSONObject().apply {
                put("id", p.id)
                put("batchId", p.batchId)
                put("date", p.date)
                put("itemName", p.itemName)
                put("item", p.itemName) // web alias
                put("boxes", p.boxes)
                put("qtyKgs", p.qtyKgs)
                put("rate", p.rate)
                put("totalAmount", p.totalAmount)
                put("total", p.totalAmount) // web alias
                put("supplierName", p.supplierName)
                put("supplier", p.supplierName) // web alias
                put("status", p.status.name)
                put("paid", p.status == PurchaseStatus.PAID)
                put("paymentMethod", p.paymentMethod)
                put("stripePaymentIntentId", p.stripePaymentIntentId)
            }
            purchasesArr.put(obj)
        }
        root.put("purchases", purchasesArr)

        // Suppliers Array
        val suppliersArr = JSONArray()
        for (s in suppliers) {
            val obj = JSONObject().apply {
                put("id", s.id)
                put("name", s.name)
                put("contactPerson", s.contactPerson)
                put("phone", s.phone)
                put("contact", s.phone) // web alias
                put("email", s.email)
                put("address", s.address)
                put("location", s.address) // web alias
                put("outstandingPayable", s.outstandingPayable)
                put("balanceDue", s.outstandingPayable) // web alias
            }
            suppliersArr.put(obj)
        }
        root.put("suppliers", suppliersArr)

        // Transactions Array
        val transactionsArr = JSONArray()
        for (t in transactions) {
            val obj = JSONObject().apply {
                put("id", t.id)
                put("stripePaymentIntentId", t.stripePaymentIntentId)
                put("purchaseBatchId", t.purchaseBatchId)
                put("supplierName", t.supplierName)
                put("amount", t.amount)
                put("currency", t.currency)
                put("paymentMethod", t.paymentMethod)
                put("status", t.status.name)
                put("timestamp", t.timestamp)
            }
            transactionsArr.put(obj)
        }
        root.put("transactions", transactionsArr)

        // Users Array
        val usersArr = JSONArray()
        for (u in users) {
            val obj = JSONObject().apply {
                put("id", u.id)
                put("mobileNumber", u.mobileNumber)
                put("fullName", u.fullName)
                put("role", u.role.name)
                put("department", u.department)
                put("isActive", u.isActive)
                put("isApproved", u.isApproved)
            }
            usersArr.put(obj)
        }
        root.put("users", usersArr)

        root.toString(2)
    }

    suspend fun restoreFromBackupJson(jsonString: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            var restoredItems = 0
            var restoredPurchases = 0
            var restoredSuppliers = 0
            var restoredTransactions = 0
            var restoredUsers = 0

            // 1. Restore items (supports both "items" and "inventory")
            val itemsArr = when {
                root.has("items") -> root.getJSONArray("items")
                root.has("inventory") -> root.getJSONArray("inventory")
                else -> null
            }

            if (itemsArr != null) {
                for (i in 0 until itemsArr.length()) {
                    val obj = itemsArr.getJSONObject(i)
                    val name = obj.optString("name", "").trim()
                    if (name.isNotBlank()) {
                        val stock = if (obj.has("currentStockKgs")) obj.optDouble("currentStockKgs", 0.0) else obj.optDouble("stockKgs", 0.0)
                        val boxes = if (obj.has("totalBoxes")) obj.optInt("totalBoxes", 0) else obj.optInt("boxes", 0)
                        val rate = if (obj.has("defaultRatePerKg")) obj.optDouble("defaultRatePerKg", 0.0) else obj.optDouble("avgRate", 0.0)
                        val item = ItemEntity(
                            name = name,
                            code = obj.optString("code", "VEG-" + (1000..9999).random()),
                            category = obj.optString("category", "Vegetables"),
                            unit = obj.optString("unit", "kgs"),
                            currentStockKgs = stock,
                            totalBoxes = boxes,
                            defaultRatePerKg = rate,
                            minStockThresholdKgs = obj.optDouble("minStockThresholdKgs", 50.0),
                            description = obj.optString("description", "")
                        )
                        repository.addOrUpdateItem(item)
                        restoredItems++
                    }
                }
            }

            // 2. Restore suppliers
            if (root.has("suppliers")) {
                val suppliersArr = root.getJSONArray("suppliers")
                for (i in 0 until suppliersArr.length()) {
                    val obj = suppliersArr.getJSONObject(i)
                    val name = obj.optString("name", "").trim()
                    if (name.isNotBlank()) {
                        val phone = obj.optString("phone", obj.optString("contact", "8608414322"))
                        val address = obj.optString("address", obj.optString("location", "Mandi Yard"))
                        val balance = if (obj.has("outstandingPayable")) obj.optDouble("outstandingPayable", 0.0) else obj.optDouble("balanceDue", 0.0)
                        val supplier = SupplierEntity(
                            name = name,
                            contactPerson = obj.optString("contactPerson", name),
                            phone = phone,
                            email = obj.optString("email", ""),
                            address = address,
                            outstandingPayable = balance
                        )
                        repository.addOrUpdateSupplier(supplier)
                        restoredSuppliers++
                    }
                }
            }

            // 3. Restore purchases
            if (root.has("purchases")) {
                val purchasesArr = root.getJSONArray("purchases")
                for (i in 0 until purchasesArr.length()) {
                    val obj = purchasesArr.getJSONObject(i)
                    val itemName = obj.optString("itemName", obj.optString("item", "")).trim()
                    if (itemName.isNotBlank()) {
                        val isPaid = obj.optBoolean("paid", false)
                        val statusStr = obj.optString("status", if (isPaid) "PAID" else "PENDING_PAYMENT")
                        val pStatus = when (statusStr.uppercase()) {
                            "PAID", "PAID_CASH", "PAID_UPI" -> PurchaseStatus.PAID
                            else -> if (isPaid) PurchaseStatus.PAID else PurchaseStatus.PENDING_PAYMENT
                        }

                        val total = if (obj.has("totalAmount")) obj.optDouble("totalAmount", 0.0) else obj.optDouble("total", 0.0)
                        val supplierName = obj.optString("supplierName", obj.optString("supplier", "Wholesale Supplier"))
                        val purchase = PurchaseEntryEntity(
                            batchId = obj.optString("batchId", "PO-" + System.currentTimeMillis().toString().takeLast(6)),
                            date = obj.optString("date", SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())),
                            itemName = itemName,
                            boxes = obj.optInt("boxes", 0),
                            qtyKgs = obj.optDouble("qtyKgs", 0.0),
                            rate = obj.optDouble("rate", 0.0),
                            totalAmount = total,
                            supplierName = supplierName,
                            status = pStatus,
                            paymentMethod = obj.optString("paymentMethod", obj.optString("settlementMode", "Cash")),
                            stripePaymentIntentId = obj.optString("stripePaymentIntentId", "").takeIf { it.isNotBlank() && it != "null" },
                            notes = obj.optString("notes", "")
                        )
                        repository.addOrUpdatePurchase(purchase)
                        restoredPurchases++
                    }
                }
            }

            // 4. Restore transactions
            if (root.has("transactions")) {
                val transactionsArr = root.getJSONArray("transactions")
                for (i in 0 until transactionsArr.length()) {
                    val obj = transactionsArr.getJSONObject(i)
                    val statusStr = obj.optString("status", "SUCCEEDED")
                    val tStatus = when (statusStr.uppercase()) {
                        "SUCCEEDED", "SUCCESS", "PAID" -> TransactionStatus.SUCCEEDED
                        "PROCESSING" -> TransactionStatus.PROCESSING
                        "FAILED" -> TransactionStatus.FAILED
                        "REFUNDED" -> TransactionStatus.REFUNDED
                        else -> TransactionStatus.SUCCEEDED
                    }
                    val trans = TransactionEntity(
                        stripePaymentIntentId = obj.optString("stripePaymentIntentId", "TXN-" + (100000..999999).random()),
                        purchaseBatchId = obj.optString("purchaseBatchId", ""),
                        supplierName = obj.optString("supplierName", "Mandi Vendor"),
                        amount = obj.optDouble("amount", 0.0),
                        currency = obj.optString("currency", "INR"),
                        paymentMethod = obj.optString("paymentMethod", "Cash"),
                        status = tStatus,
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        processedBy = "Sync Manager"
                    )
                    repository.addOrUpdateTransaction(trans)
                    restoredTransactions++
                }
            }

            // 5. Restore users
            if (root.has("users")) {
                val usersArr = root.getJSONArray("users")
                for (i in 0 until usersArr.length()) {
                    val obj = usersArr.getJSONObject(i)
                    val mobile = obj.optString("mobileNumber", "").trim()
                    if (mobile.isNotBlank()) {
                        val roleStr = obj.optString("role", "PURCHASER")
                        val uRole = try {
                            UserRole.valueOf(roleStr)
                        } catch (e: Exception) {
                            UserRole.PURCHASER
                        }
                        val user = UserEntity(
                            mobileNumber = mobile,
                            fullName = obj.optString("fullName", "Staff User"),
                            role = uRole,
                            pinHash = obj.optString("pinHash", "5147"),
                            department = obj.optString("department", "Procurement"),
                            isActive = obj.optBoolean("isActive", true),
                            isApproved = obj.optBoolean("isApproved", true)
                        )
                        repository.addOrUpdateUserFromBackup(user)
                        restoredUsers++
                    }
                }
            }

            val totalRestored = restoredItems + restoredPurchases + restoredSuppliers + restoredTransactions + restoredUsers
            setLastSyncTimestamp(System.currentTimeMillis())

            val summaryMsg = "Restored: $restoredItems items, $restoredPurchases purchases, $restoredSuppliers vendors, $restoredTransactions transactions, $restoredUsers users ($totalRestored total records)"
            Result.success(summaryMsg)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun saveBackupToCacheFile(jsonString: String): java.io.File {
        val fileName = "lakshana_backup_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.json"
        val cacheFile = java.io.File(context.cacheDir, fileName)
        cacheFile.writeText(jsonString)
        return cacheFile
    }
}
