package com.example.data.remote

enum class SyncState {
    IDLE,
    SYNCING,
    SUCCESS,
    OFFLINE,
    ERROR
}

data class SyncSummary(
    val syncState: SyncState = SyncState.IDLE,
    val lastSyncTimestamp: Long = 0L,
    val itemsCount: Int = 0,
    val purchasesCount: Int = 0,
    val suppliersCount: Int = 0,
    val transactionsCount: Int = 0,
    val usersCount: Int = 0,
    val stockMovementsCount: Int = 0,
    val isOnline: Boolean = true,
    val statusMessage: String = "Ready to synchronize with Lakshana Cloud",
    val cloudEndpoint: String = "https://lakshanaveggie.trade/api/v1/sync",
    val autoSyncEnabled: Boolean = true
)
