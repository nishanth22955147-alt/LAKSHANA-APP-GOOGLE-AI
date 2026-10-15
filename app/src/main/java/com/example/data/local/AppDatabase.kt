package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.ItemEntity
import com.example.data.model.PurchaseEntryEntity
import com.example.data.model.StockMovementEntity
import com.example.data.model.SupplierEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserEntity::class,
        ItemEntity::class,
        SupplierEntity::class,
        PurchaseEntryEntity::class,
        TransactionEntity::class,
        StockMovementEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun itemDao(): ItemDao
    abstract fun supplierDao(): SupplierDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun transactionDao(): TransactionDao
    abstract fun stockMovementDao(): StockMovementDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "procure_flow_db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database)
                }
            }
        }

        private suspend fun populateInitialData(db: AppDatabase) {
            // 1. Seed Admin User with Admin Mobile 8608414322 & PIN 5147
            val adminUser = UserEntity(
                mobileNumber = "8608414322",
                fullName = "Lakshana Admin",
                role = UserRole.ADMIN,
                pinHash = "5147",
                isApproved = true,
                department = "Executive Admin"
            )
            db.userDao().insertUser(adminUser)

            // 2. Seed Initial Mandi Inventory Items
            val initialItems = listOf(
                ItemEntity(name = "Fresh Country Tomatoes (நாட்டு தக்காளி)", code = "TOM-01", category = "Vegetables", currentStockKgs = 450.0, totalBoxes = 18, defaultRatePerKg = 32.0, unit = "kgs"),
                ItemEntity(name = "Nashik Red Onions (பெரிய வெங்காயம்)", code = "ONI-01", category = "Vegetables", currentStockKgs = 800.0, totalBoxes = 32, defaultRatePerKg = 28.0, unit = "kgs"),
                ItemEntity(name = "Ooty Fresh Potatoes (உருளைக்கிழங்கு)", code = "POT-01", category = "Vegetables", currentStockKgs = 600.0, totalBoxes = 20, defaultRatePerKg = 30.0, unit = "kgs"),
                ItemEntity(name = "Mysore Hybrid Carrots (கேரட்)", code = "CAR-01", category = "Vegetables", currentStockKgs = 300.0, totalBoxes = 12, defaultRatePerKg = 42.0, unit = "kgs"),
                ItemEntity(name = "Guntur Green Chillies (பச்சை மிளகாய்)", code = "CHI-01", category = "Vegetables", currentStockKgs = 120.0, totalBoxes = 8, defaultRatePerKg = 55.0, unit = "kgs"),
                ItemEntity(name = "Coimbatore Cabbage (முட்டைக்கோஸ்)", code = "CAB-01", category = "Vegetables", currentStockKgs = 250.0, totalBoxes = 10, defaultRatePerKg = 22.0, unit = "kgs"),
                ItemEntity(name = "Malur Cauliflower (காலிஃபிளவர்)", code = "CAU-01", category = "Vegetables", currentStockKgs = 180.0, totalBoxes = 15, defaultRatePerKg = 35.0, unit = "kgs"),
                ItemEntity(name = "Small Onion / Shallots (சின்ன வெங்காயம்)", code = "SHL-01", category = "Vegetables", currentStockKgs = 380.0, totalBoxes = 15, defaultRatePerKg = 65.0, unit = "kgs"),
                ItemEntity(name = "Dindigul Drumstick (முருங்கைக்காய்)", code = "DRM-01", category = "Vegetables", currentStockKgs = 85.0, totalBoxes = 5, defaultRatePerKg = 60.0, unit = "kgs")
            )
            initialItems.forEach { db.itemDao().insertItem(it) }

            // 3. Seed Initial Mandi Suppliers
            val initialSuppliers = listOf(
                SupplierEntity(name = "Hosur Direct Farm Producers", contactPerson = "Ramanathan", phone = "9443210987", email = "hosur@lakshanaveggie.trade", address = "Hosur Valley, Tamil Nadu", outstandingPayable = 28000.0, vegetableCategories = "Country Tomatoes, Chillies, Greens"),
                SupplierEntity(name = "Nashik Onion Logistics Hub", contactPerson = "Senthil Kumar", phone = "9123456789", email = "nashik@lakshanaveggie.trade", address = "Nashik APMC, Maharashtra", outstandingPayable = 65000.0, vegetableCategories = "Red Onions, Shallots, Garlic"),
                SupplierEntity(name = "Ooty Hill Vegetable Cooperative", contactPerson = "Murugan", phone = "9789012345", email = "ooty@lakshanaveggie.trade", address = "Nilgiris / Ooty Market", outstandingPayable = 15400.0, vegetableCategories = "Carrots, Potatoes, Cabbage"),
                SupplierEntity(name = "Koyambedu APMC Merchant 14", contactPerson = "Velmurugan", phone = "9840123456", email = "koyambedu@lakshanaveggie.trade", address = "Koyambedu Market, Chennai", outstandingPayable = 42500.0, vegetableCategories = "Drumstick, Cauliflower, Vegetables")
            )
            initialSuppliers.forEach { db.supplierDao().insertSupplier(it) }

            // 4. Seed Initial Purchase Ledger with Today, Yesterday, and Past Dates
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val nowCal = java.util.Calendar.getInstance()
            val todayStr = sdf.format(nowCal.time)

            val yestCal = (nowCal.clone() as java.util.Calendar).apply { add(java.util.Calendar.DAY_OF_YEAR, -1) }
            val yestStr = sdf.format(yestCal.time)

            val fourDaysCal = (nowCal.clone() as java.util.Calendar).apply { add(java.util.Calendar.DAY_OF_YEAR, -4) }
            val fourDaysStr = sdf.format(fourDaysCal.time)

            val initialPurchases = listOf(
                PurchaseEntryEntity(batchId = "PO-TODAY-01", date = todayStr, itemName = "Country Tomato (நாட்டு தக்காளி)", boxes = 18, qtyKgs = 450.0, rate = 28.0, totalAmount = 12600.0, supplierName = "Hosur Direct Farm Producers", status = com.example.data.model.PurchaseStatus.PENDING_PAYMENT, paymentMethod = "Credit / Unpaid"),
                PurchaseEntryEntity(batchId = "PO-TODAY-02", date = todayStr, itemName = "Bellary Big Onion (பெரிய வெங்காயம்)", boxes = 50, qtyKgs = 1250.0, rate = 35.0, totalAmount = 43750.0, supplierName = "Nashik Onion Logistics Hub", status = com.example.data.model.PurchaseStatus.PAID_UPI, paymentMethod = "UPI (Google Pay)"),
                PurchaseEntryEntity(batchId = "PO-YEST-01", date = yestStr, itemName = "Ooty Fresh Potato (உருளைக்கிழங்கு)", boxes = 20, qtyKgs = 600.0, rate = 29.0, totalAmount = 17400.0, supplierName = "Ooty Hill Vegetable Cooperative", status = com.example.data.model.PurchaseStatus.PAID_CASH, paymentMethod = "Cash"),
                PurchaseEntryEntity(batchId = "PO-YEST-02", date = yestStr, itemName = "Mysore Hybrid Carrot (கேரட்)", boxes = 12, qtyKgs = 290.0, rate = 45.0, totalAmount = 13050.0, supplierName = "Ooty Hill Vegetable Cooperative", status = com.example.data.model.PurchaseStatus.PENDING_PAYMENT, paymentMethod = "Credit / Unpaid"),
                PurchaseEntryEntity(batchId = "PO-WEEK-01", date = fourDaysStr, itemName = "Small Onion / Shallots (சின்ன வெங்காயம்)", boxes = 15, qtyKgs = 380.0, rate = 65.0, totalAmount = 24700.0, supplierName = "Koyambedu APMC Merchant 14", status = com.example.data.model.PurchaseStatus.PAID, paymentMethod = "Bank NEFT")
            )
            initialPurchases.forEach { db.purchaseDao().insertPurchase(it) }
        }
    }
}
