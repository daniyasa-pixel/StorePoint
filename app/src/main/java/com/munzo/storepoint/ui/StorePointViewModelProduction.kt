package com.munzo.storepoint.ui

import androidx.lifecycle.viewModelScope
import com.munzo.storepoint.data.BillOfMaterials
import com.munzo.storepoint.data.ProductionLogEntity
import com.munzo.storepoint.data.StorePointRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun StorePointViewModel.executeRepackagingProduction(
    rawMaterialProductId: Int,
    rawMaterialQtyUsed: Double,
    targetRetailProductId: Int,
    targetRetailQtyProduced: Double,
    wasteLossQty: Double,
    retailSellingPrice: Double,
    saveAsBom: Boolean = false,
    notes: String = "",
    onComplete: (Boolean, String, ProductionLogEntity?) -> Unit
) {
    viewModelScope.launch {
        try {
            val allProds = repository.allProducts.firstOrNull() ?: emptyList()
            val rawProd = allProds.firstOrNull { it.id == rawMaterialProductId }
            val targetProd = allProds.firstOrNull { it.id == targetRetailProductId }

            if (rawProd == null) {
                onComplete(false, "Source raw material product not found.", null)
                return@launch
            }
            if (targetProd == null) {
                onComplete(false, "Target retail SKU product not found.", null)
                return@launch
            }
            if (rawMaterialQtyUsed <= 0.0) {
                onComplete(false, "Raw material usage must be greater than zero.", null)
                return@launch
            }
            if (targetRetailQtyProduced <= 0.0) {
                onComplete(false, "Produced target yield must be greater than zero.", null)
                return@launch
            }
            if (rawProd.stockCount < rawMaterialQtyUsed.toInt()) {
                onComplete(false, "Insufficient stock of ${rawProd.name}. In stock: ${rawProd.stockCount}, required: ${rawMaterialQtyUsed.toInt()}.", null)
                return@launch
            }

            val totalRawCost = StorePointRepository.roundMoney(rawMaterialQtyUsed * rawProd.cost)
            val netYield = targetRetailQtyProduced // actual yield ready for inventory
            val derivedUnitCost = if (netYield > 0.0) {
                StorePointRepository.roundMoney(totalRawCost / netYield)
            } else 0.0

            val codeDate = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
            val productionCode = "PRD-$codeDate"
            val operator = activeUser.value?.username ?: "supervisor"

            val productionRun = ProductionLogEntity(
                productionCode = productionCode,
                rawMaterialProductId = rawProd.id,
                rawMaterialName = rawProd.name,
                rawMaterialQtyUsed = rawMaterialQtyUsed,
                rawMaterialUnitCost = rawProd.cost,
                targetRetailProductId = targetProd.id,
                targetRetailProductName = targetProd.name,
                targetRetailQtyProduced = targetRetailQtyProduced,
                derivedRetailUnitCost = derivedUnitCost,
                retailSellingPrice = if (retailSellingPrice > 0.0) retailSellingPrice else targetProd.price,
                wasteLossQty = wasteLossQty,
                operatorUsername = operator,
                timestamp = System.currentTimeMillis(),
                notes = notes
            )

            val executedRun = repository.executeProductionRunAtomic(productionRun, saveAsBom)
            onComplete(true, "Successfully transformed ${rawMaterialQtyUsed.toInt()} ${rawProd.baseUom} into ${targetRetailQtyProduced.toInt()} ${targetProd.baseUom} (Derived cost: $derivedUnitCost each).", executedRun)
        } catch (e: Exception) {
            onComplete(false, "Repackaging production failed: ${e.localizedMessage}", null)
        }
    }
}

fun StorePointViewModel.saveBomRecipe(
    bom: BillOfMaterials,
    onComplete: (Boolean, String) -> Unit
) {
    viewModelScope.launch {
        try {
            repository.saveBOM(bom)
            onComplete(true, "Standard BOM Recipe saved successfully.")
        } catch (e: Exception) {
            onComplete(false, "Failed to save BOM Recipe: ${e.localizedMessage}")
        }
    }
}

fun StorePointViewModel.deleteBomRecipe(
    id: Int,
    onComplete: (Boolean) -> Unit
) {
    viewModelScope.launch {
        try {
            repository.deleteBOM(id)
            onComplete(true)
        } catch (_: Exception) {
            onComplete(false)
        }
    }
}
