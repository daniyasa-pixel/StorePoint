package com.munzo.storepoint

import androidx.sqlite.db.SupportSQLiteDatabase
import com.munzo.storepoint.data.AppDatabase
import com.munzo.storepoint.data.Category
import com.munzo.storepoint.data.Product
import com.munzo.storepoint.data.PurchaseOrder
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Proxy

class RoomV19MigrationTest {

    @Test
    fun productAndCategory_defaultIsArchivedToFalse() {
        val category = Category(id = 1, name = "Beverages")
        assertFalse("Category isArchived must default to false", category.isArchived)

        val product = Product(
            id = 10,
            name = "Mineral Water 500ml",
            categoryId = 1,
            price = 20.0,
            stockCount = 50,
            barcode = "480001"
        )
        assertFalse("Product isArchived must default to false", product.isArchived)

        val archivedProduct = product.copy(isArchived = true)
        assertTrue(archivedProduct.isArchived)
    }

    @Test
    fun purchaseOrderModel_instantiationWithRestrictedForeignKey() {
        val po = PurchaseOrder(
            id = 1,
            poNumber = "PO-2026-001",
            supplierId = 5,
            supplierName = "San Miguel Corp",
            orderDate = 1700000000000L,
            totalCost = 5000.0,
            status = "ORDERED",
            paymentStatus = "UNPAID",
            notes = "Weekly restock"
        )
        assertEquals(5, po.supplierId)
        assertEquals("PO-2026-001", po.poNumber)
    }

    @Test
    fun migration18to19_definedInDatabaseMigrations() {
        assertNotNull(AppDatabase.MIGRATION_18_19)
        assertEquals(18, AppDatabase.MIGRATION_18_19.startVersion)
        assertEquals(19, AppDatabase.MIGRATION_18_19.endVersion)
    }

    @Test
    fun migration18to19_executesAllRequiredDdlAndDmlStatements() {
        val executedStatements = mutableListOf<String>()

        val dbProxy = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                val sql = args[0] as String
                executedStatements.add(sql.trim())
            }
            null
        } as SupportSQLiteDatabase

        // Execute MIGRATION_18_19
        AppDatabase.MIGRATION_18_19.migrate(dbProxy)

        assertTrue(executedStatements.isNotEmpty())
        assertTrue("Adds isArchived to categories", executedStatements.any { it.contains("ALTER TABLE `categories` ADD COLUMN `isArchived`") })
        assertTrue("Adds isArchived to products", executedStatements.any { it.contains("ALTER TABLE `products` ADD COLUMN `isArchived`") })
        assertTrue("Creates composite index on products(isArchived, name)", executedStatements.any { it.contains("`index_products_isArchived_name`") && it.contains("`isArchived`, `name`") })
        assertTrue("Creates purchase_orders_new with ON DELETE RESTRICT", executedStatements.any { it.contains("purchase_orders_new") && it.contains("ON DELETE RESTRICT") })
        assertTrue("Copies purchase orders data", executedStatements.any { it.contains("INSERT INTO `purchase_orders_new`") && it.contains("FROM `purchase_orders`") })
        assertTrue("Drops old purchase_orders table", executedStatements.any { it.contains("DROP TABLE `purchase_orders`") })
        assertTrue("Renames purchase_orders_new to purchase_orders", executedStatements.any { it.contains("ALTER TABLE `purchase_orders_new` RENAME TO `purchase_orders`") })
        assertTrue("Recreates supplierId index", executedStatements.any { it.contains("`index_purchase_orders_supplierId`") })
    }

    @Test
    fun configureConnectionPragmas_guardsForeignKeysDuringMigration() {
        // Case 1: When isMigrating = true, PRAGMA foreign_keys = ON must NOT be executed
        val migrationStatements = mutableListOf<String>()
        val migratingDbProxy = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                migrationStatements.add((args[0] as String).trim())
            }
            null
        } as SupportSQLiteDatabase

        AppDatabase.configureConnectionPragmas(migratingDbProxy, isMigrating = true)
        assertFalse("PRAGMA foreign_keys = ON must NOT run while migrating to prevent cascade drops",
            migrationStatements.any { it.contains("foreign_keys = ON", ignoreCase = true) }
        )
        assertTrue("Performance cache_size must still be configured",
            migrationStatements.any { it.contains("cache_size", ignoreCase = true) }
        )

        // Case 2: When isMigrating = false (normal runtime), PRAGMA foreign_keys = ON must be executed
        val runtimeStatements = mutableListOf<String>()
        val runtimeDbProxy = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                runtimeStatements.add((args[0] as String).trim())
            }
            null
        } as SupportSQLiteDatabase

        AppDatabase.configureConnectionPragmas(runtimeDbProxy, isMigrating = false)
        assertTrue("PRAGMA foreign_keys = ON must be executed at normal runtime",
            runtimeStatements.any { it.contains("foreign_keys = ON", ignoreCase = true) }
        )
    }
}
