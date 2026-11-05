package com.munzo.storepoint.ui

import android.widget.Toast
import androidx.lifecycle.viewModelScope
import com.munzo.storepoint.data.ShiftReconciliation
import com.munzo.storepoint.data.StorePointRepository
import com.munzo.storepoint.util.EscPosHelper
import kotlinx.coroutines.launch

fun StorePointViewModel.reconcileShiftAndClose(
    count1000: Int,
    count500: Int,
    count200: Int,
    count100: Int,
    count50: Int,
    count20: Int,
    countCoins: Double,
    totalCountedCash: Double,
    expectedCash: Double,
    countedGCash: Double,
    expectedGCash: Double,
    countedSmartLoad: Double,
    expectedSmartLoad: Double,
    countedGlobeLoad: Double,
    expectedGlobeLoad: Double,
    managerOverrideApproved: Boolean,
    managerUsername: String,
    discrepancyReason: String,
    onComplete: (Boolean, String, ShiftReconciliation?) -> Unit
) {
    viewModelScope.launch {
        try {
            val session = repository.getActiveSessionSync()
            if (session == null) {
                onComplete(false, "No active shift session to reconcile.", null)
                return@launch
            }

            val cashVariance = StorePointRepository.roundMoney(totalCountedCash - expectedCash)
            val gcashVariance = StorePointRepository.roundMoney(countedGCash - expectedGCash)
            val smartLoadVariance = StorePointRepository.roundMoney(countedSmartLoad - expectedSmartLoad)
            val globeLoadVariance = StorePointRepository.roundMoney(countedGlobeLoad - expectedGlobeLoad)

            val hasDiscrepancy = Math.abs(cashVariance) >= 0.01 ||
                    Math.abs(gcashVariance) >= 0.01 ||
                    Math.abs(smartLoadVariance) >= 0.01 ||
                    Math.abs(globeLoadVariance) >= 0.01

            if (hasDiscrepancy && !managerOverrideApproved) {
                onComplete(false, "Variance exceeds discrepancy threshold. Manager override PIN and explanation required.", null)
                return@launch
            }

            val now = System.currentTimeMillis()
            val reconciliation = ShiftReconciliation(
                sessionId = session.id,
                cashierUsername = session.cashierUsername,
                timestamp = now,
                count1000 = count1000,
                count500 = count500,
                count200 = count200,
                count100 = count100,
                count50 = count50,
                count20 = count20,
                countCoins = countCoins,
                totalCountedCash = totalCountedCash,
                expectedCash = expectedCash,
                cashVariance = cashVariance,
                countedGCash = countedGCash,
                expectedGCash = expectedGCash,
                gcashVariance = gcashVariance,
                countedSmartLoad = countedSmartLoad,
                expectedSmartLoad = expectedSmartLoad,
                smartLoadVariance = smartLoadVariance,
                countedGlobeLoad = countedGlobeLoad,
                expectedGlobeLoad = expectedGlobeLoad,
                globeLoadVariance = globeLoadVariance,
                hasDiscrepancy = hasDiscrepancy,
                managerOverrideApproved = managerOverrideApproved,
                managerUsername = managerUsername,
                discrepancyReason = discrepancyReason,
                receiptPrinted = false
            )

            repository.reconcileAndCloseShift(
                reconciliation = reconciliation,
                endingCash = totalCountedCash,
                endingGCash = countedGCash,
                endingSmartLoad = countedSmartLoad,
                endingGlobeLoad = countedGlobeLoad
            )

            clearCart()
            onComplete(true, "Shift closed and reconciled successfully.", reconciliation)
        } catch (e: Exception) {
            onComplete(false, "Reconciliation error: ${e.localizedMessage}", null)
        }
    }
}

fun StorePointViewModel.printShiftReconciliationReceipt(
    reconciliation: ShiftReconciliation,
    startTime: Long,
    formulaSummary: EscPosHelper.DrawerBalancingSummary? = null,
    onResult: (Boolean, String) -> Unit
) {
    val config = storeConfig.value ?: return
    val is80 = is80mmThermal.value
    val hasCutter = printerHasAutoCutter.value

    try {
        val ticketBytes = EscPosHelper.buildShiftReconciliationReceipt(
            storeConfig = config,
            reconciliation = reconciliation,
            startTime = startTime,
            formulaSummary = formulaSummary,
            is80mm = is80,
            hasAutoCutter = hasCutter
        )
        printShiftZReading(ticketBytes, onResult)
    } catch (e: Exception) {
        onResult(false, "Printing error: ${e.localizedMessage}")
    }
}
