package com.yeasinx.oshudhkhan

import android.app.Application
import android.os.Build
import android.util.Log
import com.yeasinx.oshudhkhan.data.remote.AuthRepository
import com.yeasinx.oshudhkhan.data.remote.CloudBackup
import com.yeasinx.oshudhkhan.data.sync.CloudSync
import com.yeasinx.oshudhkhan.data.local.MedicineDatabase
import com.yeasinx.oshudhkhan.data.MedicineRepository
import com.yeasinx.oshudhkhan.data.UserRepository
import com.yeasinx.oshudhkhan.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch

class MedicineApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database by lazy { MedicineDatabase.getDatabase(this) }
    private val backup by lazy { CloudBackup() }
    val repository by lazy {
        MedicineRepository(database.medicineDao(), database.doseLogDao(), database.tombstoneDao(), backup)
    }
    val userRepository by lazy { UserRepository(this, backup) }
    val authRepository by lazy { AuthRepository() }
    private val cloudSync by lazy {
        CloudSync(this, database.medicineDao(), database.doseLogDao(), database.tombstoneDao(), userRepository, backup)
    }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationHelper.createNotificationChannel(this)
        }
        // Merge with the cloud on sign-in and on each launch while signed in
        appScope.launch {
            authRepository.currentUser.distinctUntilChangedBy { it?.uid }.collectLatest { user ->
                if (user != null) {
                    runCatching { cloudSync.sync(user.uid, user.email) }
                        .onFailure { Log.w("CloudSync", "Sync failed", it) }
                }
            }
        }
    }
}
