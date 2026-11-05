package com.munzo.storepoint

import com.munzo.storepoint.data.PaymentSchedule
import com.munzo.storepoint.data.PurchaseOrder
import com.munzo.storepoint.data.PurchaseOrderItem
import com.munzo.storepoint.data.Supplier
import com.munzo.storepoint.data.StorePointRepository
import org.junit.Assert.*
import org.junit.Test

class SuppliersPurchasingAndPayablesTest {

    @Test
    fun supplierDirectory_supportsDistributorsAgentsContactAndPaymentTerms() {
        val directDistributor = Supplier(
            id = 1,
            name = "Coca-Cola Beverages Philippines Inc.",
            contactPerson = "Juan Dela Cruz (Agent #402)",
            phone = "09171234567",
            email = "coke.orders@distributor.ph",
            address = "Canlubang Industrial Estate, Laguna",
            deliverySchedule = "Tuesdays & Fridays",
            paymentTerms = "15-Day",
            notes = "Direct Distributor • Minimum 10 crates per order"
        )

        assertEquals("Coca-Cola Beverages Philippines Inc.", directDistributor.name)
        assertEquals("Juan Dela Cruz (Agent #402)", directDistributor.contactPerson)
        assertEquals("09171234567", directDistributor.phone)
        assertEquals("Tuesdays & Fridays", directDistributor.deliverySchedule)
        assertEquals("15-Day", directDistributor.paymentTerms)
        assertTrue(directDistributor.notes.contains("Direct Distributor"))

        // Configurable payment terms verification
        val codVendor = Supplier(
            id = 2,
            name = "Aling Nena Fresh Produce",
            contactPerson = "Nena Santos",
            phone = "09289876543",
            deliverySchedule = "Daily early morning",
            paymentTerms = "COD"
        )
        assertEquals("COD", codVendor.paymentTerms)

        val creditDistributor = Supplier(
            id = 3,
            name = "Monde Nissin Mega Distributor",
            contactPerson = "Mark Bautista",
            phone = "09185551234",
            deliverySchedule = "Mondays & Thursdays",
            paymentTerms = "30-Day"
        )
        assertEquals("30-Day", creditDistributor.paymentTerms)

        val weeklyTermSupplier = Supplier(
            id = 4,
            name = "Universal Robina Corp (URC)",
            contactPerson = "Sarah Reyes",
            phone = "09194443210",
            deliverySchedule = "Wednesdays & Saturdays",
            paymentTerms = "7-Day"
        )
        assertEquals("7-Day", weeklyTermSupplier.paymentTerms)

        val directory = listOf(directDistributor, codVendor, creditDistributor, weeklyTermSupplier)
        assertEquals(4, directory.size)

        // Test filtering by payment terms
        val creditSuppliers = directory.filter { it.paymentTerms != "COD" }
        assertEquals(3, creditSuppliers.size)
        assertTrue(creditSuppliers.any { it.paymentTerms == "7-Day" })
        assertTrue(creditSuppliers.any { it.paymentTerms == "15-Day" })
        assertTrue(creditSuppliers.any { it.paymentTerms == "30-Day" })
    }

    @Test
    fun purchaseOrder_supportsCreationItemizedTrackingExpectedDeliveryAndStatusManagement() {
        val now = 1700000000000L
        val expectedDelivery = now + 86400000L * 3 // 3 days later

        val po = PurchaseOrder(
            id = 101,
            poNumber = "PO-20261102-001",
            supplierId = 1,
            supplierName = "Coca-Cola Beverages Philippines Inc.",
            orderDate = now,
            expectedDeliveryDate = expectedDelivery,
            receivedDate = null,
            totalCost = 3500.0,
            status = "PENDING",
            paymentStatus = "UNPAID",
            notes = "Rush delivery needed before weekend rush"
        )

        assertEquals("PO-20261102-001", po.poNumber)
        assertEquals("PENDING", po.status)
        assertEquals("UNPAID", po.paymentStatus)
        assertEquals(expectedDelivery, po.expectedDeliveryDate)

        // Itemized Order Tracking
        val items = listOf(
            PurchaseOrderItem(
                id = 1,
                purchaseOrderId = po.id,
                productId = 10,
                productName = "Coke 1.5L Case (12s)",
                quantityOrdered = 5,
                quantityReceived = 0,
                unitCost = 450.0,
                totalCost = 2250.0,
                uomName = "case"
            ),
            PurchaseOrderItem(
                id = 2,
                purchaseOrderId = po.id,
                productId = 11,
                productName = "Royal Tru-Orange 500ml Case (24s)",
                quantityOrdered = 2,
                quantityReceived = 0,
                unitCost = 625.0,
                totalCost = 1250.0,
                uomName = "case"
            )
        )

        val computedTotal = items.sumOf { it.quantityOrdered * it.unitCost }
        assertEquals(3500.0, computedTotal, 0.0)
        assertEquals(po.totalCost, computedTotal, 0.0)

        // Status Transition 1: Receipt of Shipment
        val receivedPo = po.copy(
            status = "RECEIVED",
            receivedDate = expectedDelivery,
            paymentStatus = "PAID"
        )
        assertEquals("RECEIVED", receivedPo.status)
        assertEquals("PAID", receivedPo.paymentStatus)
        assertNotNull(receivedPo.receivedDate)

        // Status Transition 2: Cancellation of Purchase Order
        val canceledPo = po.copy(status = "CANCELED")
        assertEquals("CANCELED", canceledPo.status)

        // Status filter logic
        val orders = listOf(po, receivedPo, canceledPo)
        val pendingOrders = orders.filter { it.status == "PENDING" || it.status == "ORDERED" }
        assertEquals(1, pendingOrders.size)
        val completedOrders = orders.filter { it.status == "RECEIVED" }
        assertEquals(1, completedOrders.size)
        val canceledOrders = orders.filter { it.status == "CANCELED" || it.status == "CANCELLED" }
        assertEquals(1, canceledOrders.size)
    }

    @Test
    fun paymentCalendarAndStoreBills_schedulesOverheadsWithPriorityAndPaidStatusTracking() {
        // Scheduled Store Overheads
        val rentBill = PaymentSchedule(
            id = 1,
            title = "Commercial Store Space Rental",
            amount = 15000.0,
            dueDate = "2026-11-05",
            category = "Rent",
            priority = "Critical",
            isPaid = false,
            paymentMethod = "CASH"
        )

        val electricBill = PaymentSchedule(
            id = 2,
            title = "Meralco Electric Bill (Commercial)",
            amount = 8750.50,
            dueDate = "2026-11-12",
            category = "Electricity / Meralco",
            priority = "Critical",
            isPaid = false,
            paymentMethod = "GCASH"
        )

        val waterBill = PaymentSchedule(
            id = 3,
            title = "Maynilad Water Services",
            amount = 1200.00,
            dueDate = "2026-11-18",
            category = "Water",
            priority = "Medium",
            isPaid = false,
            paymentMethod = "CASH"
        )

        val distributorCreditBill = PaymentSchedule(
            id = 4,
            title = "Coca-Cola Distributor 15-Day Net Term",
            amount = 3500.00,
            dueDate = "2026-11-17",
            category = "Distributor Credit",
            priority = "Medium",
            isPaid = false,
            paymentMethod = "CASH"
        )

        val routineLicense = PaymentSchedule(
            id = 5,
            title = "Barangay Clearance & Permit Sinking Fund",
            amount = 500.00,
            dueDate = "2026-11-30",
            category = "Taxes / Licenses",
            priority = "Low",
            isPaid = true,
            paymentMethod = "CASH"
        )

        val bills = listOf(rentBill, electricBill, waterBill, distributorCreditBill, routineLicense)

        // 1. Verify priority/urgency levels: Low, Medium, Critical
        val criticalBills = bills.filter { it.priority.equals("Critical", ignoreCase = true) }
        assertEquals(2, criticalBills.size)
        assertTrue(criticalBills.any { it.category == "Rent" })
        assertTrue(criticalBills.any { it.category == "Electricity / Meralco" })

        val mediumBills = bills.filter { it.priority.equals("Medium", ignoreCase = true) }
        assertEquals(2, mediumBills.size)
        assertTrue(mediumBills.any { it.category == "Water" })
        assertTrue(mediumBills.any { it.category == "Distributor Credit" })

        val lowBills = bills.filter { it.priority.equals("Low", ignoreCase = true) }
        assertEquals(1, lowBills.size)

        // 2. Paid / Unpaid tracking
        val unpaidBills = bills.filter { !it.isPaid }
        assertEquals(4, unpaidBills.size)
        val paidBills = bills.filter { it.isPaid }
        assertEquals(1, paidBills.size)

        // Total unpaid payables liability
        val totalAccountsPayable = StorePointRepository.roundMoney(unpaidBills.sumOf { it.amount })
        assertEquals(28450.50, totalAccountsPayable, 0.0)

        // Toggle paid status
        val settledRent = rentBill.copy(isPaid = true)
        assertTrue(settledRent.isPaid)
        val updatedAccountsPayable = StorePointRepository.roundMoney(totalAccountsPayable - rentBill.amount)
        assertEquals(13450.50, updatedAccountsPayable, 0.0)

        // 3. Overdue detection logic
        val currentDate = "2026-11-10"
        val overdueBills = bills.filter { !it.isPaid && it.dueDate < currentDate }
        assertEquals(1, overdueBills.size)
        assertEquals("Commercial Store Space Rental", overdueBills.first().title)
    }
}

