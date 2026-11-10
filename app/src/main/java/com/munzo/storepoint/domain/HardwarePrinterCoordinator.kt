package com.munzo.storepoint.domain

import com.munzo.storepoint.data.StoreConfig
import com.munzo.storepoint.data.Transaction
import com.munzo.storepoint.data.TransactionItem
import com.munzo.storepoint.util.EscPosHelper

/**
 * Domain Coordinator for Hardware Thermal Printers and ESC/POS Receipt Formatting.
 *
 * Provides high-level dispatch coordination between raw ESC/POS byte generation,
 * width configuration (58mm vs 80mm), and automatic cash drawer kick commands.
 */
object HardwarePrinterCoordinator {

    enum class PaperWidth(val columns: Int, val is80mm: Boolean) {
        WIDTH_58MM(32, false),
        WIDTH_80MM(48, true)
    }

    /**
     * Prepares standard receipt bytes configured for the target printer profile.
     */
    fun prepareReceiptPayload(
        storeConfig: StoreConfig,
        transaction: Transaction,
        items: List<TransactionItem>,
        paperWidth: PaperWidth = PaperWidth.WIDTH_58MM,
        kickDrawer: Boolean = true,
        hasAutoCutter: Boolean = false,
        customHeader: String? = null,
        customFooter: String? = null,
        storeTin: String? = null,
        storeAddress: String? = null,
        printBarcode: Boolean = true
    ): ByteArray {
        return EscPosHelper.buildReceiptEscPos(
            storeConfig = storeConfig,
            transaction = transaction,
            items = items,
            is80mm = paperWidth.is80mm,
            kickDrawerOnPrint = kickDrawer,
            hasAutoCutter = hasAutoCutter,
            customHeader = customHeader,
            customFooter = customFooter,
            storeTin = storeTin,
            storeAddress = storeAddress,
            printBarcode = printBarcode
        )
    }

    /**
     * Returns raw ESC/POS command sequence for manual drawer open / cash drop.
     */
    fun prepareCashDrawerKickSignal(): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        out.write(EscPosHelper.CMD_INIT)
        out.write(EscPosHelper.CMD_DRAWER_KICK_PIN2)
        out.write(EscPosHelper.CMD_DRAWER_KICK_PIN5)
        return out.toByteArray()
    }
}
