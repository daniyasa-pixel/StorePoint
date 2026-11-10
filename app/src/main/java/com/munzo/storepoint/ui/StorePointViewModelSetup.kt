package com.munzo.storepoint.ui

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.viewModelScope
import com.munzo.storepoint.data.AppDatabase
import com.munzo.storepoint.data.StoreConfig
import com.munzo.storepoint.data.StorePointRepository
import com.munzo.storepoint.data.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// --- Setup & Diagnostics Extensions ---

internal fun StorePointViewModel.setupCheckDatabaseHealthImpl() {
    viewModelScope.launch(Dispatchers.IO) {
        val report = AppDatabase.checkConnectionHealth(context)
        databaseHealth.value = report
        android.util.Log.i("StorePointViewModel", "Database Health Report: ${report.message}")
    }
}

internal fun StorePointViewModel.setupCompleteInitialSetupImpl(
    storeName: String,
    currency: String,
    taxPercent: Double,
    adminUser: String,
    adminPass: String,
    adminName: String = "",
    adminPhone: String = "",
    hasGCash: Boolean,
    hasMaya: Boolean,
    hasLoad: Boolean,
    smartLoadBalance: Double = 0.0,
    globeLoadBalance: Double = 0.0,
    loadServiceFee: Double = 2.0,
    gcashServiceFee: Double = 10.0,
    mayaServiceFee: Double = 10.0,
    mayaBankFee: Double = 15.0
) {
    viewModelScope.launch {
        val hashedPass = hashPin(adminPass)
        val user = User(
            username = adminUser,
            pinHash = hashedPass,
            role = "ADMIN",
            biometricEnrolled = false,
            pinResetRequired = false
        )
        repository.saveUser(user)

        // Save admin contact details in preferences
        val adminPrefs = context.getSharedPreferences("storepoint_admin_prefs", Context.MODE_PRIVATE)
        adminPrefs.edit()
            .putString("admin_phone_number", adminPhone.trim())
            .putString("admin_name", adminName.trim())
            .apply()

        // Save store config
        val config = StoreConfig(
            storeName = storeName,
            currencySymbol = currency,
            taxPercentage = taxPercent,
            setupCompleted = true,
            hasGCash = hasGCash,
            hasMaya = hasMaya,
            hasLoad = hasLoad,
            smartLoadBalance = StorePointRepository.roundMoney(smartLoadBalance),
            globeLoadBalance = StorePointRepository.roundMoney(globeLoadBalance),
            loadServiceFee = StorePointRepository.roundMoney(loadServiceFee),
            gcashServiceFee = StorePointRepository.roundMoney(gcashServiceFee),
            mayaServiceFee = StorePointRepository.roundMoney(mayaServiceFee),
            mayaBankFee = StorePointRepository.roundMoney(mayaBankFee)
        )
        repository.saveStoreConfig(config)
        
        // Automatically log in active administrator
        activeUser.value = user
        Toast.makeText(context, "Initial Store Setup Completed Successfully!", Toast.LENGTH_SHORT).show()
    }
}
