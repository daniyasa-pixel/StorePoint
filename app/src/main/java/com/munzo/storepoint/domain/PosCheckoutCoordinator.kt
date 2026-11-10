package com.munzo.storepoint.domain

import com.munzo.storepoint.data.CartItemDetails
import com.munzo.storepoint.data.StorePointRepository

/**
 * Domain Coordinator for POS ringing, pricing precision, VAT, and split-tender payment calculations.
 *
 * Encapsulates core business rules for:
 * 1. Line item pricing with multi-UOM unit multipliers
 * 2. Order discounts (Percentage, Fixed Amount, Senior/PWD with statutory VAT exemption)
 * 3. Philippine 12% Value Added Tax (VAT) precision breakdown
 * 4. Split-tender payment settlement and change calculation
 */
object PosCheckoutCoordinator {

    enum class DiscountType {
        NONE,
        PERCENTAGE,
        FIXED,
        SENIOR_PWD;

        companion object {
            fun fromString(value: String): DiscountType {
                return when (value.trim().uppercase()) {
                    "PERCENTAGE" -> PERCENTAGE
                    "FIXED" -> FIXED
                    "SENIOR_PWD", "SENIOR", "PWD" -> SENIOR_PWD
                    else -> NONE
                }
            }
        }
    }

    data class CheckoutTotals(
        val subtotal: Double,
        val discountAmount: Double,
        val netTotal: Double,
        val vatableSales: Double,
        val vatAmount: Double,
        val vatExemptSales: Double = 0.0
    )

    data class SplitPaymentResult(
        val nonCashAmount: Double,
        val cashRequired: Double,
        val cashPaid: Double,
        val changeAmount: Double,
        val isFullySettled: Boolean
    )

    /**
     * Computes the line item total for a given cart item taking into account its unit quantity.
     */
    fun computeLineTotal(item: CartItemDetails): Double {
        return StorePointRepository.roundMoney(item.quantity * item.selectedPrice)
    }

    /**
     * Computes the effective inventory base units deducted for a multi-UOM cart item.
     */
    fun computeBaseUnitsNeeded(item: CartItemDetails): Int {
        val multiplier = if (item.multiplier > 0) item.multiplier else 1
        return item.quantity * multiplier
    }

    /**
     * Computes complete checkout totals including subtotal, discount, net amount,
     * and statutory 12% VAT breakdown.
     */
    fun computeCheckoutTotals(
        items: List<CartItemDetails>,
        discountType: String = "NONE",
        discountValue: Double = 0.0,
        isVatInclusive: Boolean = true,
        vatRate: Double = 12.0
    ): CheckoutTotals {
        return computeCheckoutTotals(
            items = items,
            type = DiscountType.fromString(discountType),
            discountValue = discountValue,
            isVatInclusive = isVatInclusive,
            vatRate = vatRate
        )
    }

    fun computeCheckoutTotals(
        items: List<CartItemDetails>,
        type: DiscountType = DiscountType.NONE,
        discountValue: Double = 0.0,
        isVatInclusive: Boolean = true,
        vatRate: Double = 12.0
    ): CheckoutTotals {
        val subtotal = StorePointRepository.roundMoney(items.sumOf { computeLineTotal(it) })
        if (subtotal <= 0.0) {
            return CheckoutTotals(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        }

        val discountCalc = calculateDiscount(subtotal, type, discountValue, isVatInclusive, vatRate)
        return calculateVatBreakdown(subtotal, discountCalc, type, isVatInclusive, vatRate)
    }

    /**
     * Calculates discount amount adhering to Philippine statutory requirements (RA 9994 / RA 10754).
     * For Senior/PWD with VAT-inclusive pricing, the 12% VAT must be stripped first to establish
     * the VAT-exempt base, upon which the 20% statutory discount is applied.
     */
    private fun calculateDiscount(
        subtotal: Double,
        type: DiscountType,
        value: Double,
        isVatInclusive: Boolean,
        vatRate: Double
    ): Pair<Double, Double> { // Pair(discountAmount, netPayable)
        return when (type) {
            DiscountType.SENIOR_PWD -> {
                val vatExemptBase = if (isVatInclusive && vatRate > 0.0) {
                    StorePointRepository.calculateVatableSalesFromInclusive(subtotal, vatRate)
                } else subtotal
                val discount = StorePointRepository.roundMoney(vatExemptBase * 0.20)
                val net = StorePointRepository.roundMoney(vatExemptBase - discount)
                Pair(discount, net)
            }
            DiscountType.PERCENTAGE -> {
                val clampedPercent = value.coerceIn(0.0, 100.0)
                val discount = StorePointRepository.roundMoney(subtotal * (clampedPercent / 100.0))
                val net = StorePointRepository.roundMoney(subtotal - discount)
                Pair(discount, net)
            }
            DiscountType.FIXED -> {
                val clampedAmount = value.coerceIn(0.0, subtotal)
                val discount = StorePointRepository.roundMoney(clampedAmount)
                val net = StorePointRepository.roundMoney(subtotal - discount)
                Pair(discount, net)
            }
            DiscountType.NONE -> {
                Pair(0.0, subtotal)
            }
        }
    }

    /**
     * Computes the VAT breakdown delegating directly to StorePointRepository's verified single source of truth.
     */
    private fun calculateVatBreakdown(
        subtotal: Double,
        discountCalc: Pair<Double, Double>,
        type: DiscountType,
        isVatInclusive: Boolean,
        vatRate: Double
    ): CheckoutTotals {
        val (discountAmount, netTotal) = discountCalc

        return if (type == DiscountType.SENIOR_PWD) {
            CheckoutTotals(
                subtotal = subtotal,
                discountAmount = discountAmount,
                netTotal = netTotal,
                vatableSales = 0.0,
                vatAmount = 0.0,
                vatExemptSales = netTotal
            )
        } else if (isVatInclusive && vatRate > 0.0) {
            val vat = StorePointRepository.calculateVatFromInclusive(netTotal, vatRate)
            val vatable = StorePointRepository.calculateVatableSalesFromInclusive(netTotal, vatRate)
            CheckoutTotals(
                subtotal = subtotal,
                discountAmount = discountAmount,
                netTotal = netTotal,
                vatableSales = vatable,
                vatAmount = vat,
                vatExemptSales = 0.0
            )
        } else {
            CheckoutTotals(
                subtotal = subtotal,
                discountAmount = discountAmount,
                netTotal = netTotal,
                vatableSales = netTotal,
                vatAmount = 0.0,
                vatExemptSales = 0.0
            )
        }
    }

    /**
     * Resolves split-tender settlements where part of the balance is settled via electronic
     * payments (GCash, Maya, Card) and the remainder in physical cash.
     */
    fun computeSplitPayment(
        netTotal: Double,
        nonCashTendered: Double,
        cashTendered: Double
    ): SplitPaymentResult {
        val roundedNet = StorePointRepository.roundMoney(netTotal.coerceAtLeast(0.0))
        val nonCash = StorePointRepository.roundMoney(nonCashTendered.coerceIn(0.0, roundedNet))
        val cashRequired = StorePointRepository.roundMoney(roundedNet - nonCash)
        val cashPaid = StorePointRepository.roundMoney(cashTendered.coerceAtLeast(0.0))
        val change = if (cashPaid >= cashRequired) {
            StorePointRepository.roundMoney(cashPaid - cashRequired)
        } else 0.0
        val isFullySettled = cashPaid >= cashRequired

        return SplitPaymentResult(
            nonCashAmount = nonCash,
            cashRequired = cashRequired,
            cashPaid = cashPaid,
            changeAmount = change,
            isFullySettled = isFullySettled
        )
    }
}
