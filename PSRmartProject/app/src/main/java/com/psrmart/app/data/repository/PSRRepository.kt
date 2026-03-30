package com.psrmart.app.data.repository

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.psrmart.app.data.database.PSRDatabase
import com.psrmart.app.data.model.*
import kotlinx.coroutines.flow.*
import java.io.File

private val Application.dataStore: DataStore<Preferences> by preferencesDataStore("psrmart_settings")

class PSRRepository(private val app: Application) {
    private val db   = PSRDatabase.getDatabase(app)
    private val gson = Gson()
    private val SETTINGS_KEY = stringPreferencesKey("business_settings")

    val categories    = db.categoryDao().getAllFlow()
    val allStock      = db.stockItemDao().getAllFlow()
    val customers     = db.customerDao().getAllFlow()
    val invoices      = db.invoiceDao().getAllFlow()
    val bankEntries   = db.bankEntryDao().getAllFlow()
    val activeOrders  = db.customerOrderDao().getActiveFlow()
    val allOrders     = db.customerOrderDao().getAllFlow()
    val suppliers     = db.supplierDao().getAllFlow()
    val allMovements  = db.stockMovementDao().getAllFlow()

    val totalProfit    = db.invoiceDao().getTotalProfitFlow()

    // ── Online Orders ─────────────────────────────────────────────────
    val onlineProducts     = db.onlineProductDao().getAllActiveFlow()
    val allOnlineProducts  = db.onlineProductDao().getAllFlow()
    val activeOnlineOrders = db.onlineOrderDao().getActiveFlow()
    val allOnlineOrders    = db.onlineOrderDao().getAllFlow()

    suspend fun insertOnlineProduct(p: OnlineProduct) = db.onlineProductDao().insert(p)
    suspend fun updateOnlineProduct(p: OnlineProduct) = db.onlineProductDao().update(p)
    suspend fun deleteOnlineProduct(p: OnlineProduct) = db.onlineProductDao().delete(p)

    suspend fun insertOnlineOrder(o: OnlineOrder) = db.onlineOrderDao().insert(o)
    suspend fun updateOnlineOrder(o: OnlineOrder) = db.onlineOrderDao().update(o)
    suspend fun deleteOnlineOrder(o: OnlineOrder) = db.onlineOrderDao().delete(o)

    /** Mark order as settled and write income to bank ledger */
    suspend fun settleOnlineOrder(
        order: OnlineOrder,
        platformFeeRm: Double,
        shippingSubsidy: Double,
        extraCosts: Double,
        extraCostNote: String,
        account: FinancialAccount
    ) {
        val settled = order.copy(
            status         = OnlineOrderStatus.SETTLED,
            settledAt      = System.currentTimeMillis(),
            shippedAt      = order.shippedAt ?: System.currentTimeMillis(),
            platformFeeRm  = platformFeeRm,
            shippingSubsidy = shippingSubsidy,
            extraCosts     = extraCosts,
            extraCostNote  = extraCostNote,
            settledToAccount = account.name
        )
        db.onlineOrderDao().update(settled)

        val netReceived = settled.subtotal - platformFeeRm - extraCosts + shippingSubsidy
        val platformLabel = when (order.platform) {
            OnlinePlatform.SHOPEE -> "Shopee"
            OnlinePlatform.TIKTOK -> "TikTok"
            OnlinePlatform.OTHER  -> "Online"
        }
        val desc = "$platformLabel — ${order.customerName} (${order.items.size} item${if (order.items.size != 1) "s" else ""})"
        db.bankEntryDao().insert(BankEntry(
            description = desc,
            amount      = netReceived,
            account     = account,
            category    = "Online Sale",
            isManual    = false
        ))
    }

    // Profit for time periods — recalculated from start-of-period timestamps
    // These are Flows so they update live as invoices are paid
    fun profitSince(startMs: Long) = db.invoiceDao().getProfitSinceFlow(startMs).map { it ?: 0.0 }
    val unpaidsCount   = db.invoiceDao().getUnpaidCountFlow()
    val unpaidAmount   = db.invoiceDao().getUnpaidAmountFlow()
    val maybankBalance = db.invoiceDao().getMaybankBalanceFlow()
    val cashBalance    = db.invoiceDao().getCashBalanceFlow()
    val savingsBalance = db.invoiceDao().getSavingsBalanceFlow()
    val inventoryValue = db.stockItemDao().getTotalStockUnitsFlow()

    fun searchStock(q: String) = db.stockItemDao().searchFlow(q)
    fun movementsForItem(id: Long) = db.stockMovementDao().getForItemFlow(id)

    // Categories
    suspend fun insertCategory(c: Category) = db.categoryDao().insert(c)
    suspend fun deleteCategory(c: Category) = db.categoryDao().delete(c)

    // Suppliers
    suspend fun insertSupplier(s: Supplier): Long = db.supplierDao().insert(s)
    suspend fun updateSupplier(s: Supplier) = db.supplierDao().update(s)
    suspend fun deleteSupplier(s: Supplier) = db.supplierDao().delete(s)
    /** Find supplier by name, or create one if it doesn't exist. Returns the supplier id. */
    suspend fun findOrCreateSupplier(name: String): Long {
        val existing = db.supplierDao().findByName(name.trim())
        return existing?.id ?: db.supplierDao().insert(Supplier(name = name.trim()))
    }

    // Stock
    suspend fun insertStock(item: StockItem): Long {
        val id = db.stockItemDao().insert(item)
        if (item.defaultBuyPrice > 0) {
            db.priceHistoryDao().insert(PriceHistory(stockItemId = id, buyPrice = item.defaultBuyPrice, sellPrice = item.currentSellPrice))
        }
        autoBackupCatalogue()
        return id
    }
    suspend fun updateStock(item: StockItem) {
        db.stockItemDao().update(item)
        if (item.defaultBuyPrice > 0) {
            db.priceHistoryDao().insert(PriceHistory(stockItemId = item.id, buyPrice = item.defaultBuyPrice, sellPrice = item.currentSellPrice))
        }
        autoBackupCatalogue()
    }
    suspend fun deleteStock(item: StockItem) {
        db.stockItemDao().delete(item)
        autoBackupCatalogue()
    }

    // Customer-specific item prices
    suspend fun getCustomerItemPrice(customerId: Long, stockItemId: Long): Double? =
        db.customerItemPriceDao().get(customerId, stockItemId)?.lastSellPrice

    suspend fun getCustomerItemPriceWithDate(customerId: Long, stockItemId: Long): Pair<Double?, String?> {
        val record = db.customerItemPriceDao().get(customerId, stockItemId) ?: return Pair(null, null)
        val dateStr = java.text.SimpleDateFormat("EEE, dd MMM", java.util.Locale.getDefault())
            .format(java.util.Date(record.updatedAt))
        return Pair(record.lastSellPrice, dateStr)
    }

    suspend fun getAllCustomerItemPrices(customerId: Long): Map<Long, CustomerItemPrice> =
        db.customerItemPriceDao().getAllForCustomer(customerId).associateBy { it.stockItemId }
    suspend fun setCustomerItemPrice(customerId: Long, stockItemId: Long, price: Double) =
        db.customerItemPriceDao().upsert(CustomerItemPrice(customerId, stockItemId, price))

    // Stock Movements
    suspend fun recordStockIn(item: StockItem, qty: Double, unitCost: Double, supplierName: String, account: FinancialAccount, notes: String = "") {
        db.stockMovementDao().insert(StockMovement(stockItemId = item.id, type = StockMovementType.STOCK_IN, qty = qty, unitCost = unitCost, supplierName = supplierName, account = account, notes = notes))
        val newQty = item.stockQty + qty
        db.stockItemDao().update(item.copy(stockQty = newQty, updatedAt = System.currentTimeMillis()))
        val total = qty * unitCost
        db.bankEntryDao().insert(BankEntry(description = "Stock In: ${item.name} (${qty}${item.unit} × RM${unitCost})", amount = -total, account = account, category = "Stock Purchase", isManual = false))
        val updatedPrices = item.supplierPrices.toMutableList()
        val existing = updatedPrices.indexOfFirst { it.supplierName.equals(supplierName, true) }
        if (existing >= 0) {
            val old = updatedPrices[existing]
            updatedPrices[existing] = old.copy(price = unitCost, lastUpdated = System.currentTimeMillis(), buyCount = old.buyCount + 1)
        } else {
            updatedPrices.add(SupplierPrice(supplierName = supplierName, price = unitCost, isDefault = updatedPrices.isEmpty(), lastUpdated = System.currentTimeMillis(), buyCount = 1))
        }
        db.stockItemDao().update(item.copy(supplierPrices = updatedPrices, stockQty = newQty, updatedAt = System.currentTimeMillis()))
    }

    suspend fun recordSellStock(item: StockItem, qty: Double, unitSell: Double, unitCost: Double, account: FinancialAccount, notes: String = "") {
        db.stockMovementDao().insert(StockMovement(stockItemId = item.id, type = StockMovementType.SELL, qty = qty, unitCost = unitCost, unitSell = unitSell, account = account, notes = notes))
        val newQty = (item.stockQty - qty).coerceAtLeast(0.0)
        db.stockItemDao().update(item.copy(stockQty = newQty, updatedAt = System.currentTimeMillis()))
        val revenue = qty * unitSell
        db.bankEntryDao().insert(BankEntry(description = "Direct Sale: ${item.name} (${qty}${item.unit} × RM${unitSell})", amount = revenue, account = account, category = "Direct Sale", isManual = false))
    }

    suspend fun recordAdjustment(item: StockItem, newQty: Double, notes: String = "") {
        val delta = newQty - item.stockQty
        db.stockMovementDao().insert(StockMovement(stockItemId = item.id, type = StockMovementType.ADJUSTMENT, qty = kotlin.math.abs(delta), notes = notes.ifBlank { "Manual adjustment" }))
        db.stockItemDao().update(item.copy(stockQty = newQty, updatedAt = System.currentTimeMillis()))
    }

    // Customers
    suspend fun insertCustomer(c: Customer) = db.customerDao().insert(c)
    suspend fun updateCustomer(c: Customer) = db.customerDao().update(c)
    suspend fun deleteCustomer(c: Customer) = db.customerDao().delete(c)

    // Invoices
    suspend fun insertInvoice(inv: Invoice): Long = db.invoiceDao().insert(inv)
    suspend fun updateInvoice(inv: Invoice) = db.invoiceDao().update(inv)
    suspend fun deleteInvoice(inv: Invoice) = db.invoiceDao().delete(inv)
    suspend fun getLastInvoiceForCustomer(customerId: Long): Invoice? = db.invoiceDao().getLastForCustomer(customerId)

    // Bank Entries
    suspend fun insertBankEntry(e: BankEntry) = db.bankEntryDao().insert(e)
    suspend fun deleteBankEntry(e: BankEntry) = db.bankEntryDao().delete(e)

    // Orders
    suspend fun insertOrder(o: CustomerOrder): Long = db.customerOrderDao().insert(o)
    suspend fun updateOrder(o: CustomerOrder) = db.customerOrderDao().update(o)
    suspend fun deleteOrder(o: CustomerOrder) = db.customerOrderDao().delete(o)
    suspend fun archiveOldOrders() = db.customerOrderDao().archiveOldOrders()

    // Tasks
    val activeTasks = db.businessTaskDao().getActiveFlow()
    val doneTasks   = db.businessTaskDao().getDoneFlow()
    val allTasks    = db.businessTaskDao().getAllFlow()
    suspend fun insertTask(t: BusinessTask): Long = db.businessTaskDao().insert(t)
    suspend fun updateTask(t: BusinessTask) = db.businessTaskDao().update(t)
    suspend fun deleteTask(t: BusinessTask) = db.businessTaskDao().delete(t)

    // Supplier Debts
    val activeDebts      = db.supplierDebtDao().getActiveFlow()
    val allDebts         = db.supplierDebtDao().getAllFlow()
    val totalDebtOwed    = db.supplierDebtDao().getTotalOutstandingFlow()
    val activeDebtCount  = db.supplierDebtDao().getActiveCountFlow()
    suspend fun insertDebt(d: SupplierDebt): Long = db.supplierDebtDao().insert(d)
    suspend fun updateDebt(d: SupplierDebt) = db.supplierDebtDao().update(d)
    suspend fun deleteDebt(d: SupplierDebt) = db.supplierDebtDao().delete(d)

    // Settings
    val businessSettings: Flow<BusinessSettings> = app.dataStore.data.map { prefs ->
        prefs[SETTINGS_KEY]?.let { runCatching { gson.fromJson(it, BusinessSettings::class.java) }.getOrNull() } ?: BusinessSettings()
    }
    suspend fun saveBusinessSettings(s: BusinessSettings) { app.dataStore.edit { it[SETTINGS_KEY] = gson.toJson(s) } }

    // Flush
    suspend fun buildFlushReport(beforeMs: Long) = FlushReport(
        totalInvoices    = db.invoiceDao().countAll(),
        totalBankEntries = db.bankEntryDao().countAll(),
        maybankBalance   = db.bankEntryDao().sumByAccount("MAYBANK") ?: 0.0,
        cashBalance      = db.bankEntryDao().sumByAccount("CASH") ?: 0.0,
        savingsBalance   = db.bankEntryDao().sumByAccount("SAVINGS") ?: 0.0,
        cutoffMs         = beforeMs
    )

    suspend fun performFlush(beforeMs: Long) {
        val maybankBal = db.bankEntryDao().sumByAccount("MAYBANK") ?: 0.0
        val cashBal    = db.bankEntryDao().sumByAccount("CASH") ?: 0.0
        val savingsBal = db.bankEntryDao().sumByAccount("SAVINGS") ?: 0.0
        db.invoiceDao().deletePaidBefore(beforeMs)
        db.bankEntryDao().deleteManualBefore(beforeMs)
        db.bankEntryDao().deletePaymentsBefore(beforeMs)
        db.customerOrderDao().deleteAllBefore(beforeMs)
        db.priceHistoryDao().deleteOlderThan(beforeMs)
        db.stockMovementDao().deleteBefore(beforeMs)
        val newMaybank = db.bankEntryDao().sumByAccount("MAYBANK") ?: 0.0
        val newCash    = db.bankEntryDao().sumByAccount("CASH") ?: 0.0
        val newSavings = db.bankEntryDao().sumByAccount("SAVINGS") ?: 0.0
        val label = "Carry-forward (Flush ${java.text.SimpleDateFormat("MMM yyyy").format(java.util.Date())})"
        if (maybankBal != newMaybank) db.bankEntryDao().insert(BankEntry(description = label, amount = maybankBal - newMaybank, account = FinancialAccount.MAYBANK, category = "System", isManual = false, entryDate = beforeMs + 1))
        if (cashBal != newCash)       db.bankEntryDao().insert(BankEntry(description = label, amount = cashBal - newCash, account = FinancialAccount.CASH, category = "System", isManual = false, entryDate = beforeMs + 1))
        if (savingsBal != newSavings) db.bankEntryDao().insert(BankEntry(description = label, amount = savingsBal - newSavings, account = FinancialAccount.SAVINGS, category = "System", isManual = false, entryDate = beforeMs + 1))
        // Run VACUUM on IO thread to avoid main-thread crash
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            db.openHelper.writableDatabase.execSQL("VACUUM")
        }
    }

    /** Nuclear flush — wipes ALL transactions then seeds fresh opening balances */
    suspend fun nuclearFlush(maybankOpening: Double, cashOpening: Double, savingsOpening: Double) {
        db.invoiceDao().deleteAll()
        db.bankEntryDao().deleteAll()
        db.customerOrderDao().deleteAllBefore(Long.MAX_VALUE)
        db.priceHistoryDao().deleteOlderThan(Long.MAX_VALUE)
        db.stockMovementDao().deleteBefore(Long.MAX_VALUE)
        val label = "Opening Balance — ${java.text.SimpleDateFormat("dd MMM yyyy").format(java.util.Date())}"
        if (maybankOpening != 0.0) db.bankEntryDao().insert(BankEntry(description = label, amount = maybankOpening, account = FinancialAccount.MAYBANK, category = "System", isManual = false))
        if (cashOpening    != 0.0) db.bankEntryDao().insert(BankEntry(description = label, amount = cashOpening,    account = FinancialAccount.CASH,    category = "System", isManual = false))
        if (savingsOpening != 0.0) db.bankEntryDao().insert(BankEntry(description = label, amount = savingsOpening, account = FinancialAccount.SAVINGS, category = "System", isManual = false))
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            db.openHelper.writableDatabase.execSQL("VACUUM")
        }
    }

    /** Export all bank entries + invoices as Excel-friendly CSV */
    suspend fun exportTransactionsAsCsv(): File? {
        return try {
            val entries   = db.bankEntryDao().getAllUnlimited()
            val invoices  = db.invoiceDao().getAllUnlimited()
            val sdf       = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
            val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.getDefault()).format(java.util.Date())

            val csv = buildString {
                // Sheet 1: Bank Ledger
                appendLine("=== BANK LEDGER ===")
                appendLine("Date,Time,Account,Type,Category,Description,Amount (RM)")
                entries.sortedBy { it.entryDate }.forEach { e ->
                    val dt   = sdf.format(java.util.Date(e.entryDate)).split(" ")
                    val date = dt.getOrElse(0) { "" }
                    val time = dt.getOrElse(1) { "" }
                    val type = if (e.amount >= 0) "Income" else "Expense"
                    appendLine("$date,$time,${e.account.name},$type,\"${e.category}\",\"${e.description.replace("\"","\"\"")}\",${"%+.2f".format(e.amount)}")
                }
                appendLine()
                appendLine("=== INVOICES ===")
                appendLine("Date,Invoice No,Customer,Status,Subtotal,Discount,Total,Paid,Profit")
                invoices.sortedBy { it.issuedAt }.forEach { inv ->
                    val date = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date(inv.issuedAt))
                    appendLine("$date,${inv.invoiceNumber},\"${inv.customerSnapshot.name.replace("\"","\"\"")}\",${inv.status.name},${"%.2f".format(inv.subtotal)},${"%.2f".format(inv.discountAmount)},${"%.2f".format(inv.totalAmount)},${"%.2f".format(inv.amountPaid)},${"%.2f".format(inv.netProfit)}")
                }
            }

            val file = File(app.filesDir, "psrmart_transactions_$timestamp.csv")
            file.writeText(csv)
            file
        } catch (_: Exception) { null }
    }

    // Catalogue backup
    private suspend fun autoBackupCatalogue() {
        try {
            val items = db.stockItemDao().getAll()
            val catMap = db.categoryDao().getAll().associate { it.id to it.name }
            val csv = buildString {
                appendLine("id,name,category,unit,stockQty,buyPrice,sellPrice,notes,suppliers")
                items.forEach { item ->
                    val suppliersStr = item.supplierPrices.joinToString("|") { "${it.supplierName}:${it.price}${if (it.isDefault) "(default)" else ""}" }
                    appendLine("${item.id},\"${item.name.replace("\"","\"\"")}\",\"${catMap[item.categoryId] ?: ""}\",${item.unit},${item.stockQty},${item.defaultBuyPrice},${item.sellPrice},\"${item.notes.replace("\"","\"\"")}\",\"$suppliersStr\"")
                }
            }
            File(app.filesDir, "psrmart_catalogue_backup.csv").writeText(csv)
        } catch (_: Exception) {}
    }

    suspend fun exportCatalogueAsFile(): File? {
        return try {
            val items      = db.stockItemDao().getAll()
            val categories = db.categoryDao().getAll()
            val suppliers  = db.supplierDao().getAll()
            val catMap     = categories.associate { it.id to it.name }
            val timestamp  = java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.getDefault()).format(java.util.Date())
            val json = buildString {
                appendLine("{")
                appendLine("  \"version\": 2,")
                appendLine("  \"exportedAt\": \"$timestamp\",")
                appendLine("  \"appId\": \"psrmart\",")
                appendLine("  \"categories\": [")
                categories.forEachIndexed { i, cat ->
                    val comma = if (i < categories.lastIndex) "," else ""
                    appendLine("    {\"id\":${cat.id},\"name\":${gson.toJson(cat.name)},\"emoji\":${gson.toJson(cat.emoji)},\"colorHex\":${gson.toJson(cat.colorHex)},\"sortOrder\":${cat.sortOrder}}$comma")
                }
                appendLine("  ],")
                appendLine("  \"suppliers\": [")
                suppliers.forEachIndexed { i, sup ->
                    val comma = if (i < suppliers.lastIndex) "," else ""
                    appendLine("    {\"id\":${sup.id},\"name\":${gson.toJson(sup.name)},\"phone\":${gson.toJson(sup.phone)},\"notes\":${gson.toJson(sup.notes)}}$comma")
                }
                appendLine("  ],")
                appendLine("  \"items\": [")
                items.forEachIndexed { i, item ->
                    val comma = if (i < items.lastIndex) "," else ""
                    appendLine("    {")
                    appendLine("      \"name\":${gson.toJson(item.name)},")
                    appendLine("      \"category\":${gson.toJson(catMap[item.categoryId] ?: "")},")
                    appendLine("      \"unit\":${gson.toJson(item.unit)},")
                    appendLine("      \"sellPrice\":${item.sellPrice},")
                    appendLine("      \"stockQty\":${item.stockQty},")
                    appendLine("      \"customerSpec\":${gson.toJson(item.customerSpec)},")
                    appendLine("      \"notes\":${gson.toJson(item.notes)},")
                    appendLine("      \"supplierPrices\":${gson.toJson(item.supplierPrices)}")
                    appendLine("    }$comma")
                }
                appendLine("  ]")
                append("}")
            }
            val file = File(app.filesDir, "psrmart_backup_$timestamp.psrmart")
            file.writeText(json)
            file
        } catch (_: Exception) { null }
    }

    suspend fun importCatalogueFromJson(jsonText: String): ImportResult {
        return try {
            val root    = gson.fromJson(jsonText, com.google.gson.JsonObject::class.java)
            val appId   = root.get("appId")?.asString ?: ""
            if (appId.isNotEmpty() && appId != "psrmart") return ImportResult(false, "Not a PSRmart backup file.")
            var catsAdded = 0; var suppAdded = 0; var itemsNew = 0; var itemsUpdated = 0

            val catNameToId = mutableMapOf<String, Long>()
            db.categoryDao().getAll().forEach { catNameToId[it.name] = it.id }
            root.getAsJsonArray("categories")?.forEach { el ->
                val obj = el.asJsonObject
                val name = obj.get("name")?.asString ?: return@forEach
                if (!catNameToId.containsKey(name)) {
                    val id = db.categoryDao().insert(Category(name = name,
                        emoji    = obj.get("emoji")?.asString ?: "📦",
                        colorHex = obj.get("colorHex")?.asString ?: "#1A4D2E",
                        sortOrder = obj.get("sortOrder")?.asInt ?: 0))
                    catNameToId[name] = id; catsAdded++
                }
            }

            val supNameSet = db.supplierDao().getAll().map { it.name }.toMutableSet()
            root.getAsJsonArray("suppliers")?.forEach { el ->
                val obj = el.asJsonObject
                val name = obj.get("name")?.asString ?: return@forEach
                if (!supNameSet.contains(name)) {
                    db.supplierDao().insert(Supplier(name = name,
                        phone = obj.get("phone")?.asString ?: "",
                        notes = obj.get("notes")?.asString ?: ""))
                    supNameSet.add(name); suppAdded++
                }
            }

            val existingByName = db.stockItemDao().getAll().associateBy { it.name.trim().lowercase() }
            root.getAsJsonArray("items")?.forEach { el ->
                val obj      = el.asJsonObject
                val name     = obj.get("name")?.asString?.trim() ?: return@forEach
                val catName  = obj.get("category")?.asString ?: ""
                val unit     = obj.get("unit")?.asString ?: "kg"
                val sell     = obj.get("sellPrice")?.asDouble ?: 0.0
                val qty      = obj.get("stockQty")?.asDouble ?: 0.0
                val spec     = obj.get("customerSpec")?.asString ?: ""
                val notes    = obj.get("notes")?.asString ?: ""
                val catId    = catNameToId[catName] ?: 0L
                val prices   = try {
                    val type = object : com.google.gson.reflect.TypeToken<List<SupplierPrice>>() {}.type
                    gson.fromJson<List<SupplierPrice>>(obj.get("supplierPrices"), type) ?: emptyList()
                } catch (_: Exception) { emptyList() }
                val existing = existingByName[name.lowercase()]
                if (existing != null) {
                    itemsUpdated++ // skip — already in catalogue
                } else {
                    db.stockItemDao().insert(StockItem(categoryId = catId, name = name,
                        unit = unit, sellPrice = sell, stockQty = qty,
                        customerSpec = spec, notes = notes, supplierPrices = prices))
                    itemsNew++
                }
            }
            ImportResult(true, "$itemsNew new items added, $itemsUpdated already existed (skipped), $catsAdded categories, $suppAdded suppliers added.")
        } catch (e: Exception) {
            ImportResult(false, "Could not read backup: ${e.message}")
        }
    }
}

data class ImportResult(val success: Boolean, val message: String)
data class FlushReport(val totalInvoices: Int, val totalBankEntries: Int, val maybankBalance: Double, val cashBalance: Double, val savingsBalance: Double, val cutoffMs: Long)
