package com.psrmart.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// ─────────────────────────────────────────────
// ENUMS
// ─────────────────────────────────────────────

enum class FinancialAccount { MAYBANK, CASH, SAVINGS }
enum class InvoiceStatus    { UNPAID, PAID, PARTIAL, CANCELLED }
enum class StockMovementType { STOCK_IN, SELL, ADJUSTMENT, WASTAGE }

// ─────────────────────────────────────────────
// SUPPORTING DATA CLASSES
// ─────────────────────────────────────────────

data class CustomField(val label: String, val value: String)

/**
 * A single purchase record from one supplier on one date.
 * Replaces the old flat SupplierPrice — now fully date-sensitive.
 * buyCount tracks how many times we've bought from this supplier (for ranking).
 */
data class SupplierPrice(
    val supplierName: String,
    val price: Double,
    val isDefault: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis(), // date-sensitive
    val buyCount: Int = 0                               // frequency ranking
)

data class CustomerSnapshot(
    val name: String,
    val company: String,
    val phone: String,
    val email: String,
    val address: String,
    val customFields: List<CustomField> = emptyList()
)

data class InvoiceItem(
    val stockItemId: Long? = null,
    val description: String,
    val unit: String,
    val qty: Double,
    val buyPrice: Double,
    val sellPrice: Double,
    val lineTotal: Double,
    val lineCost: Double,
    // Optional return/refund sub-line
    val returnQty: Double = 0.0,
    val returnPrice: Double = 0.0,       // price per unit for return (defaults to sellPrice)
    val returnNote: String = ""           // italic description shown on invoice
) {
    val returnAmount: Double get() = if (returnQty > 0) returnQty * (if (returnPrice > 0) returnPrice else sellPrice) else 0.0
    val netTotal: Double     get() = lineTotal - returnAmount
    val hasReturn: Boolean   get() = returnQty > 0
}

// ─────────────────────────────────────────────
// ENTITIES
// ─────────────────────────────────────────────

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String = "📦",
    val colorHex: String = "#1A2E5A",
    val sortOrder: Int = 0
)

@Entity(tableName = "suppliers")
data class Supplier(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "stock_items")
@TypeConverters(PSRTypeConverters::class)
data class StockItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val name: String,
    val unit: String = "kg",
    val imagePath: String? = null,
    val supplierPrices: List<SupplierPrice> = emptyList(),
    val sellPrice: Double = 0.0,
    val customerSpec: String = "",   // dimensions/packaging info shown only in customer mode
    val stockQty: Double = 0.0,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val defaultBuyPrice: Double
        get() = supplierPrices.firstOrNull { it.isDefault }?.price
            ?: supplierPrices.minByOrNull { it.price }?.price ?: 0.0
    val averageBuyPrice: Double
        get() = if (supplierPrices.isEmpty()) 0.0 else supplierPrices.sumOf { it.price } / supplierPrices.size
    val mostFrequentSupplier: SupplierPrice? get() = supplierPrices.maxByOrNull { it.buyCount }
    val cheapestSupplier: SupplierPrice?     get() = supplierPrices.minByOrNull { it.price }
    val rankedSuppliers: List<SupplierPrice>
        get() = supplierPrices.sortedWith(compareByDescending<SupplierPrice> { it.buyCount }.thenBy { it.price })
    val currentBuyPrice: Double get() = defaultBuyPrice
    /** Effective sell price: explicit sellPrice if set, else 20% markup on buy */
    val currentSellPrice: Double get() = if (sellPrice > 0) sellPrice else defaultBuyPrice * 1.2
}

// ─────────────────────────────────────────────
// CUSTOMER-SPECIFIC ITEM PRICES
// Remembers the last sell price used for a specific item with a specific customer
// ─────────────────────────────────────────────

@Entity(tableName = "customer_item_prices", primaryKeys = ["customerId", "stockItemId"])
data class CustomerItemPrice(
    val customerId: Long,
    val stockItemId: Long,
    val lastSellPrice: Double,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "price_history")
data class PriceHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val stockItemId: Long,
    val supplierName: String = "",
    val buyPrice: Double,
    val sellPrice: Double,
    val recordedAt: Long = System.currentTimeMillis()
)

/**
 * Every stock-in or sell-out event.
 * stockQty in StockItem is always derived from summing these movements.
 */
@Entity(tableName = "stock_movements")
data class StockMovement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val stockItemId: Long,
    val type: StockMovementType,
    val qty: Double,                     // always positive; sign derived from type
    val unitCost: Double = 0.0,          // cost per unit (for STOCK_IN)
    val unitSell: Double = 0.0,          // sell price per unit (for SELL)
    val supplierName: String = "",       // which supplier (for STOCK_IN)
    val account: FinancialAccount = FinancialAccount.CASH,
    val notes: String = "",
    val movedAt: Long = System.currentTimeMillis()
) {
    val totalValue: Double get() = when (type) {
        StockMovementType.STOCK_IN   ->  qty * unitCost
        StockMovementType.SELL       ->  qty * unitSell
        StockMovementType.WASTAGE    -> -qty * unitCost
        StockMovementType.ADJUSTMENT ->  0.0
    }
    val profit: Double get() = when (type) {
        StockMovementType.SELL -> qty * (unitSell - unitCost)
        else -> 0.0
    }
}

@Entity(tableName = "customers")
@TypeConverters(PSRTypeConverters::class)
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val company: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val customFields: List<CustomField> = emptyList(),
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "invoices")
@TypeConverters(PSRTypeConverters::class)
data class Invoice(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceNumber: String,
    val customerId: Long? = null,
    val customerSnapshot: CustomerSnapshot,
    val items: List<InvoiceItem>,
    val subtotal: Double,
    val discountAmount: Double = 0.0,
    val roundOff: Double = 0.0,
    val taxPercent: Double = 0.0,
    val totalAmount: Double,
    val totalCost: Double,
    val netProfit: Double,
    val amountPaid: Double = 0.0,
    val status: InvoiceStatus = InvoiceStatus.UNPAID,
    val notes: String = "",
    val issuedAt: Long = System.currentTimeMillis(),
    val paidAt: Long? = null,
    val dueDate: Long? = null
)

@Entity(tableName = "bank_entries")
data class BankEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val description: String,
    val amount: Double,
    val account: FinancialAccount = FinancialAccount.MAYBANK,
    val category: String = "Expense",
    val invoiceId: Long? = null,
    val entryDate: Long = System.currentTimeMillis(),
    val isManual: Boolean = true
)

data class BusinessSettings(
    val companyName: String = "PSRmart",
    val tagline: String = "Fresh • Quality • Reliable",
    val address: String = "",
    val phone: String = "",
    val email: String = "",
    val website: String = "",
    val bankName: String = "Maybank",
    val accountName: String = "",
    val accountNumber: String = "",
    val logoPath: String? = null,
    val invoicePrefix: String = "INV",
    val invoiceNextNumber: Int = 1001,
    val defaultPaymentTerms: String = "Due on receipt",
    val defaultNotes: String = "Thank you for your business.",
    val taxLabel: String = "GST",
    val defaultTaxPercent: Double = 0.0,
    val showEmail: Boolean = true,
    val showWebsite: Boolean = false,
    val showTax: Boolean = false,
    val showDiscount: Boolean = true,
    val customInvoiceFields: List<CustomField> = emptyList(),
    val customUnits: List<String> = listOf("kg", "g", "pcs", "box", "carton", "pack", "dozen", "litre", "ml", "unit"),
    val priceListHeader: String = "📋 *Price List — {date}*\n─────────────────────",
    val priceListFooter: String = "─────────────────────\n_Prices subject to change. Contact us to confirm._",
    // ── Account display names (user-facing labels) ──────────────────────
    val maybankLabel: String  = "Maybank",   // e.g. rename to "Bank"
    val cashLabel: String     = "Cash",      // e.g. rename to "Hand"
    val savingsLabel: String  = "Savings",   // e.g. rename to "Reserve"
    // ── Sub-accounts (optional split within each main account) ──────────
    // Leave blank to disable. When set, transaction entry shows a picker
    val maybankSub1: String   = "",   // e.g. "Personal"
    val maybankSub2: String   = "",   // e.g. "Business"
    val cashSub1: String      = "",   // e.g. "Petty Cash"
    val cashSub2: String      = "",   // e.g. "Market Float"
    val savingsSub1: String   = "",
    val savingsSub2: String   = ""
)

// ─────────────────────────────────────────────
// ORDER MODEL
// ─────────────────────────────────────────────

data class OrderItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isPurchased: Boolean = false,  // first tick: item bought/picked
    val isChecked: Boolean = false     // second tick: properly packed/sent → order complete
)

@Entity(tableName = "orders")
@TypeConverters(PSRTypeConverters::class)
data class CustomerOrder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long? = null,
    val customerName: String,
    val items: List<OrderItem>,
    val createdAt: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false
) {
    val isComplete: Boolean    get() = items.isNotEmpty() && items.all { it.isChecked }
    val purchasedCount: Int    get() = items.count { it.isPurchased }
    val checkedCount: Int      get() = items.count { it.isChecked }
}

// ─────────────────────────────────────────────
// BUSINESS TASK
// ─────────────────────────────────────────────

enum class TaskPriority { LOW, MEDIUM, HIGH, URGENT }
enum class TaskStatus   { TODO, IN_PROGRESS, DONE }
enum class TaskCategory { MARKETING, SOCIAL_MEDIA, OPERATIONS, FINANCE, GROWTH, OTHER }

@Entity(tableName = "business_tasks")
@TypeConverters(PSRTypeConverters::class)
data class BusinessTask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val notes: String = "",
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val category: TaskCategory = TaskCategory.OTHER,
    val status: TaskStatus = TaskStatus.TODO,
    val dueDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val isPinned: Boolean = false
) {
    val isOverdue: Boolean get() = dueDate != null && dueDate < System.currentTimeMillis() && status != TaskStatus.DONE
    val isDueToday: Boolean get() {
        if (dueDate == null) return false
        val cal = java.util.Calendar.getInstance()
        val todayStr = "${cal.get(java.util.Calendar.YEAR)}-${cal.get(java.util.Calendar.DAY_OF_YEAR)}"
        cal.timeInMillis = dueDate
        val dueStr = "${cal.get(java.util.Calendar.YEAR)}-${cal.get(java.util.Calendar.DAY_OF_YEAR)}"
        return todayStr == dueStr
    }
}

// ─────────────────────────────────────────────
// SUPPLIER DEBT
// ─────────────────────────────────────────────

enum class DebtStatus { UNPAID, PARTIAL, PAID }

@Entity(tableName = "supplier_debts")
data class SupplierDebt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val supplierName: String,
    val description: String = "",         // what was purchased
    val totalAmount: Double,
    val amountPaid: Double = 0.0,
    val status: DebtStatus = DebtStatus.UNPAID,
    val incurredAt: Long = System.currentTimeMillis(),
    val dueDate: Long? = null,
    val notes: String = ""
) {
    val outstanding: Double get() = totalAmount - amountPaid
    val isOverdue: Boolean get() = dueDate != null && dueDate < System.currentTimeMillis() && status != DebtStatus.PAID
}

// ─────────────────────────────────────────────
// SINGLE UNIFIED TYPE CONVERTER
// ─────────────────────────────────────────────

class PSRTypeConverters {
    private val gson = Gson()

    @TypeConverter fun customFieldListToString(v: List<CustomField>?): String = gson.toJson(v ?: emptyList<CustomField>())
    @TypeConverter fun stringToCustomFieldList(v: String): List<CustomField> { val t = object : TypeToken<List<CustomField>>() {}.type; return gson.fromJson(v, t) ?: emptyList() }

    @TypeConverter fun supplierPriceListToString(v: List<SupplierPrice>?): String = gson.toJson(v ?: emptyList<SupplierPrice>())
    @TypeConverter fun stringToSupplierPriceList(v: String): List<SupplierPrice> { val t = object : TypeToken<List<SupplierPrice>>() {}.type; return gson.fromJson(v, t) ?: emptyList() }

    @TypeConverter fun invoiceItemListToString(v: List<InvoiceItem>?): String = gson.toJson(v ?: emptyList<InvoiceItem>())
    @TypeConverter fun stringToInvoiceItemList(v: String): List<InvoiceItem> { val t = object : TypeToken<List<InvoiceItem>>() {}.type; return gson.fromJson(v, t) ?: emptyList() }

    @TypeConverter fun orderItemListToString(v: List<OrderItem>?): String = gson.toJson(v ?: emptyList<OrderItem>())
    @TypeConverter fun stringToOrderItemList(v: String): List<OrderItem> { val t = object : TypeToken<List<OrderItem>>() {}.type; return gson.fromJson(v, t) ?: emptyList() }

    @TypeConverter fun customerSnapshotToString(v: CustomerSnapshot?): String = gson.toJson(v)
    @TypeConverter fun stringToCustomerSnapshot(v: String): CustomerSnapshot = gson.fromJson(v, CustomerSnapshot::class.java) ?: CustomerSnapshot("", "", "", "", "")

    @TypeConverter fun invoiceStatusToString(v: InvoiceStatus): String = v.name
    @TypeConverter fun stringToInvoiceStatus(v: String): InvoiceStatus = runCatching { InvoiceStatus.valueOf(v) }.getOrDefault(InvoiceStatus.UNPAID)

    @TypeConverter fun financialAccountToString(v: FinancialAccount): String = v.name
    @TypeConverter fun stringToFinancialAccount(v: String): FinancialAccount = runCatching { FinancialAccount.valueOf(v) }.getOrDefault(FinancialAccount.MAYBANK)

    @TypeConverter fun movementTypeToString(v: StockMovementType): String = v.name
    @TypeConverter fun stringToMovementType(v: String): StockMovementType = runCatching { StockMovementType.valueOf(v) }.getOrDefault(StockMovementType.STOCK_IN)

    @TypeConverter fun taskPriorityToString(v: TaskPriority): String = v.name
    @TypeConverter fun stringToTaskPriority(v: String): TaskPriority = runCatching { TaskPriority.valueOf(v) }.getOrDefault(TaskPriority.MEDIUM)

    @TypeConverter fun taskStatusToString(v: TaskStatus): String = v.name
    @TypeConverter fun stringToTaskStatus(v: String): TaskStatus = runCatching { TaskStatus.valueOf(v) }.getOrDefault(TaskStatus.TODO)

    @TypeConverter fun debtStatusToString(v: DebtStatus): String = v.name
    @TypeConverter fun stringToDebtStatus(v: String): DebtStatus = runCatching { DebtStatus.valueOf(v) }.getOrDefault(DebtStatus.UNPAID)

    @TypeConverter fun onlineOrderItemListToString(v: List<OnlineOrderItem>?): String = gson.toJson(v ?: emptyList<OnlineOrderItem>())
    @TypeConverter fun stringToOnlineOrderItemList(v: String): List<OnlineOrderItem> { val t = object : TypeToken<List<OnlineOrderItem>>() {}.type; return gson.fromJson(v, t) ?: emptyList() }

    @TypeConverter fun onlinePlatformToString(v: OnlinePlatform): String = v.name
    @TypeConverter fun stringToOnlinePlatform(v: String): OnlinePlatform = runCatching { OnlinePlatform.valueOf(v) }.getOrDefault(OnlinePlatform.SHOPEE)

    @TypeConverter fun onlineOrderStatusToString(v: OnlineOrderStatus): String = v.name
    @TypeConverter fun stringToOnlineOrderStatus(v: String): OnlineOrderStatus = runCatching { OnlineOrderStatus.valueOf(v) }.getOrDefault(OnlineOrderStatus.PENDING)
}

// Extension helpers so any composable can get the user-facing label for an account
fun BusinessSettings.labelFor(account: FinancialAccount): String = when (account) {
    FinancialAccount.MAYBANK  -> maybankLabel.ifBlank { "Maybank" }
    FinancialAccount.CASH     -> cashLabel.ifBlank { "Cash" }
    FinancialAccount.SAVINGS  -> savingsLabel.ifBlank { "Savings" }
}

/** Returns non-blank sub-account options for a given main account, or empty if none configured */
fun BusinessSettings.subAccountsFor(account: FinancialAccount): List<String> = when (account) {
    FinancialAccount.MAYBANK -> listOfNotNull(maybankSub1.ifBlank { null }, maybankSub2.ifBlank { null })
    FinancialAccount.CASH    -> listOfNotNull(cashSub1.ifBlank { null }, cashSub2.ifBlank { null })
    FinancialAccount.SAVINGS -> listOfNotNull(savingsSub1.ifBlank { null }, savingsSub2.ifBlank { null })
}

// ─────────────────────────────────────────────
// ONLINE ORDERS (Shopee / TikTok)
// ─────────────────────────────────────────────

enum class OnlinePlatform { SHOPEE, TIKTOK, OTHER }
enum class OnlineOrderStatus { PENDING, PACKED, SHIPPED, SETTLED, CANCELLED }

/** A product in your online shop — separate from physical stock catalogue.
 *  Name is taken from stock catalogue but all financials are overridable. */
@Entity(tableName = "online_products")
@TypeConverters(PSRTypeConverters::class)
data class OnlineProduct(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val unit: String = "pcs",
    val weightGrams: Double = 0.0,          // for reference only
    val sizeLabel: String = "",             // e.g. "Small / Medium / Large" — display only
    val avgCostPrice: Double = 0.0,         // your cost of goods for this product
    val defaultSellPrice: Double = 0.0,     // default listing price
    // Platform fee stored as fixed RM + optional % — both can be 0
    // User sets once per product and overrides at settlement if needed
    val shopeeFeeRm: Double = 0.0,          // Shopee fixed fee per order (postage subsidy etc)
    val shopeeFeePercent: Double = 0.0,     // Shopee commission %
    val tiktokFeeRm: Double = 0.0,
    val tiktokFeePercent: Double = 0.0,
    val otherFeeRm: Double = 0.0,
    val otherFeePercent: Double = 0.0,
    val isActive: Boolean = true,
    val stockItemId: Long? = null,          // optional link back to catalogue item
    val createdAt: Long = System.currentTimeMillis()
)

data class OnlineOrderItem(
    val productId: Long,
    val productName: String,
    val qty: Double,
    val unit: String,
    val sellPriceEach: Double,
    val costPriceEach: Double,
    val weightGrams: Double = 0.0
) {
    val lineTotal: Double get() = qty * sellPriceEach
    val lineCost: Double  get() = qty * costPriceEach
}

@Entity(tableName = "online_orders")
@TypeConverters(PSRTypeConverters::class)
data class OnlineOrder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val platform: OnlinePlatform = OnlinePlatform.SHOPEE,
    val customerName: String,
    val orderRef: String = "",              // platform order ID (optional)
    val items: List<OnlineOrderItem>,
    val shipBefore: Long? = null,           // deadline
    val shippedAt: Long? = null,
    val settledAt: Long? = null,
    val status: OnlineOrderStatus = OnlineOrderStatus.PENDING,
    // Settlement fields — filled in at delivery confirmation
    val platformFeeRm: Double = 0.0,        // actual fee charged (RM)
    val shippingSubsidy: Double = 0.0,      // any shipping subsidy received
    val extraCosts: Double = 0.0,           // torn packaging, penalty, etc
    val extraCostNote: String = "",
    val settledToAccount: String = "MAYBANK", // which account received the money
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    val subtotal: Double    get() = items.sumOf { it.lineTotal }
    val totalCost: Double   get() = items.sumOf { it.lineCost }
    val netProfit: Double   get() = subtotal - totalCost - platformFeeRm - extraCosts + shippingSubsidy
    val itemCount: Int      get() = items.size
    val isOverdue: Boolean  get() = shipBefore != null && System.currentTimeMillis() > shipBefore && status == OnlineOrderStatus.PENDING
}
