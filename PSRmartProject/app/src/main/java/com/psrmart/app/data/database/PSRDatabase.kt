package com.psrmart.app.data.database

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import com.psrmart.app.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC") fun getAllFlow(): Flow<List<Category>>
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC") suspend fun getAll(): List<Category>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(c: Category): Long
    @Update suspend fun update(c: Category)
    @Delete suspend fun delete(c: Category)
}

@Dao interface SupplierDao {
    @Query("SELECT * FROM suppliers ORDER BY name ASC") fun getAllFlow(): Flow<List<Supplier>>
    @Query("SELECT * FROM suppliers ORDER BY name ASC") suspend fun getAll(): List<Supplier>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(s: Supplier): Long
    @Update suspend fun update(s: Supplier)
    @Delete suspend fun delete(s: Supplier)
    @Query("SELECT * FROM suppliers WHERE name = :name LIMIT 1") suspend fun findByName(name: String): Supplier?
}

@Dao interface StockItemDao {
    @Query("SELECT * FROM stock_items ORDER BY name ASC") fun getAllFlow(): Flow<List<StockItem>>
    @Query("SELECT * FROM stock_items ORDER BY name ASC") suspend fun getAll(): List<StockItem>
    @Query("SELECT * FROM stock_items WHERE categoryId = :cid ORDER BY name ASC") fun getByCategoryFlow(cid: Long): Flow<List<StockItem>>
    @Query("SELECT * FROM stock_items WHERE id = :id") suspend fun getById(id: Long): StockItem?
    @Query("SELECT * FROM stock_items WHERE name LIKE '%' || :q || '%' ORDER BY name ASC") fun searchFlow(q: String): Flow<List<StockItem>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(item: StockItem): Long
    @Update suspend fun update(item: StockItem)
    @Delete suspend fun delete(item: StockItem)
    @Query("DELETE FROM stock_items") suspend fun deleteAll()
    @Query("SELECT SUM(stockQty) FROM stock_items") fun getTotalStockUnitsFlow(): Flow<Double?>
}

@Dao interface CustomerItemPriceDao {
    @Query("SELECT * FROM customer_item_prices WHERE customerId = :cid AND stockItemId = :iid LIMIT 1")
    suspend fun get(cid: Long, iid: Long): CustomerItemPrice?
    @Query("SELECT * FROM customer_item_prices WHERE customerId = :cid")
    suspend fun getAllForCustomer(cid: Long): List<CustomerItemPrice>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(p: CustomerItemPrice)
    @Query("DELETE FROM customer_item_prices WHERE customerId = :cid") suspend fun deleteForCustomer(cid: Long)
}

@Dao interface PriceHistoryDao {
    @Query("SELECT * FROM price_history WHERE stockItemId = :id ORDER BY recordedAt DESC LIMIT 50") fun getHistoryFlow(id: Long): Flow<List<PriceHistory>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(h: PriceHistory)
    @Query("DELETE FROM price_history WHERE recordedAt < :beforeMs") suspend fun deleteOlderThan(beforeMs: Long)
    @Query("DELETE FROM price_history") suspend fun deleteAll()
}

@Dao interface StockMovementDao {
    @Query("SELECT * FROM stock_movements WHERE stockItemId = :id ORDER BY movedAt DESC LIMIT 100") fun getForItemFlow(id: Long): Flow<List<StockMovement>>
    @Query("SELECT * FROM stock_movements ORDER BY movedAt DESC LIMIT 200") fun getAllFlow(): Flow<List<StockMovement>>
    @Query("SELECT SUM(CASE WHEN type = 'STOCK_IN' THEN qty WHEN type IN ('SELL','WASTAGE') THEN -qty ELSE 0 END) FROM stock_movements WHERE stockItemId = :id") suspend fun netQtyForItem(id: Long): Double?
    @Query("SELECT SUM((unitSell - unitCost) * qty) FROM stock_movements WHERE type = 'SELL'") fun getTotalSellProfitFlow(): Flow<Double?>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(m: StockMovement): Long
    @Delete suspend fun delete(m: StockMovement)
    @Query("DELETE FROM stock_movements WHERE stockItemId = :id") suspend fun deleteForItem(id: Long)
    @Query("DELETE FROM stock_movements WHERE movedAt < :beforeMs") suspend fun deleteBefore(beforeMs: Long)
}

@Dao interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name ASC") fun getAllFlow(): Flow<List<Customer>>
    @Query("SELECT * FROM customers WHERE id = :id") suspend fun getById(id: Long): Customer?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(c: Customer): Long
    @Update suspend fun update(c: Customer)
    @Delete suspend fun delete(c: Customer)
}

@Dao interface InvoiceDao {
    @Query("SELECT * FROM invoices ORDER BY issuedAt DESC LIMIT 200") fun getAllFlow(): Flow<List<Invoice>>
    @Query("SELECT * FROM invoices WHERE customerId = :cid ORDER BY issuedAt DESC LIMIT 1") suspend fun getLastForCustomer(cid: Long): Invoice?
    @Query("SELECT SUM(netProfit) FROM invoices WHERE status = 'PAID'") fun getTotalProfitFlow(): Flow<Double?>
    @Query("SELECT SUM(netProfit) FROM invoices WHERE status = 'PAID' AND issuedAt >= :startMs") fun getProfitSinceFlow(startMs: Long): Flow<Double?>
    @Query("SELECT COUNT(*) FROM invoices WHERE status = 'UNPAID'") fun getUnpaidCountFlow(): Flow<Int>
    @Query("SELECT SUM(totalAmount - amountPaid) FROM invoices WHERE status != 'PAID'") fun getUnpaidAmountFlow(): Flow<Double?>
    @Query("SELECT SUM(amount) FROM bank_entries WHERE account = 'MAYBANK'") fun getMaybankBalanceFlow(): Flow<Double?>
    @Query("SELECT SUM(amount) FROM bank_entries WHERE account = 'CASH'") fun getCashBalanceFlow(): Flow<Double?>
    @Query("SELECT SUM(amount) FROM bank_entries WHERE account = 'SAVINGS'") fun getSavingsBalanceFlow(): Flow<Double?>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(inv: Invoice): Long
    @Update suspend fun update(inv: Invoice)
    @Delete suspend fun delete(inv: Invoice)
    @Query("DELETE FROM invoices WHERE issuedAt < :beforeMs AND status = 'PAID'") suspend fun deletePaidBefore(beforeMs: Long)
    @Query("DELETE FROM invoices") suspend fun deleteAll()
    @Query("SELECT COUNT(*) FROM invoices") suspend fun countAll(): Int
    @Query("SELECT * FROM invoices ORDER BY issuedAt ASC") suspend fun getAllUnlimited(): List<Invoice>
}

@Dao interface BankEntryDao {
    @Query("SELECT * FROM bank_entries ORDER BY entryDate DESC LIMIT 300") fun getAllFlow(): Flow<List<BankEntry>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(e: BankEntry): Long
    @Update suspend fun update(e: BankEntry)
    @Delete suspend fun delete(e: BankEntry)
    @Query("DELETE FROM bank_entries WHERE entryDate < :beforeMs AND isManual = 1") suspend fun deleteManualBefore(beforeMs: Long)
    @Query("DELETE FROM bank_entries WHERE entryDate < :beforeMs AND invoiceId IS NOT NULL") suspend fun deletePaymentsBefore(beforeMs: Long)
    @Query("DELETE FROM bank_entries") suspend fun deleteAll()
    @Query("SELECT SUM(amount) FROM bank_entries WHERE account = :acc") suspend fun sumByAccount(acc: String): Double?
    @Query("SELECT COUNT(*) FROM bank_entries") suspend fun countAll(): Int
    @Query("SELECT * FROM bank_entries ORDER BY entryDate ASC") suspend fun getAllUnlimited(): List<BankEntry>
}

@Dao interface BusinessTaskDao {
    @Query("SELECT * FROM business_tasks ORDER BY CASE WHEN isPinned=1 THEN 0 ELSE 1 END, CASE priority WHEN 'URGENT' THEN 0 WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 2 ELSE 3 END, createdAt DESC")
    fun getAllFlow(): Flow<List<BusinessTask>>
    @Query("SELECT * FROM business_tasks WHERE status != 'DONE' ORDER BY CASE WHEN isPinned=1 THEN 0 ELSE 1 END, CASE priority WHEN 'URGENT' THEN 0 WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 2 ELSE 3 END, createdAt DESC")
    fun getActiveFlow(): Flow<List<BusinessTask>>
    @Query("SELECT * FROM business_tasks WHERE status = 'DONE' ORDER BY completedAt DESC LIMIT 50")
    fun getDoneFlow(): Flow<List<BusinessTask>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(t: BusinessTask): Long
    @Update suspend fun update(t: BusinessTask)
    @Delete suspend fun delete(t: BusinessTask)
    @Query("DELETE FROM business_tasks WHERE status = 'DONE' AND completedAt < :beforeMs") suspend fun purgeOldDone(beforeMs: Long)
}

@Dao interface CustomerOrderDao {
    @Query("SELECT * FROM orders ORDER BY createdAt DESC") fun getAllFlow(): Flow<List<CustomerOrder>>
    @Query("SELECT * FROM orders WHERE isArchived = 0 ORDER BY createdAt DESC") fun getActiveFlow(): Flow<List<CustomerOrder>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(o: CustomerOrder): Long
    @Update suspend fun update(o: CustomerOrder)
    @Delete suspend fun delete(o: CustomerOrder)
    @Query("UPDATE orders SET isArchived = 1 WHERE isArchived = 0 AND date(createdAt/1000,'unixepoch','localtime') < date('now','localtime')") suspend fun archiveOldOrders()
    @Query("DELETE FROM orders WHERE isArchived = 1 AND createdAt < :beforeMs") suspend fun deleteArchivedBefore(beforeMs: Long)
    @Query("DELETE FROM orders WHERE createdAt < :beforeMs") suspend fun deleteAllBefore(beforeMs: Long)
}

@Dao interface SupplierDebtDao {
    @Query("SELECT * FROM supplier_debts ORDER BY CASE WHEN status != 'PAID' THEN 0 ELSE 1 END, incurredAt DESC")
    fun getAllFlow(): Flow<List<SupplierDebt>>
    @Query("SELECT * FROM supplier_debts WHERE status != 'PAID' ORDER BY CASE WHEN dueDate IS NOT NULL AND dueDate < :now THEN 0 ELSE 1 END, incurredAt DESC")
    fun getActiveFlow(now: Long = System.currentTimeMillis()): Flow<List<SupplierDebt>>
    @Query("SELECT SUM(totalAmount - amountPaid) FROM supplier_debts WHERE status != 'PAID'") fun getTotalOutstandingFlow(): Flow<Double?>
    @Query("SELECT COUNT(*) FROM supplier_debts WHERE status != 'PAID'") fun getActiveCountFlow(): Flow<Int>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(d: SupplierDebt): Long
    @Update suspend fun update(d: SupplierDebt)
    @Delete suspend fun delete(d: SupplierDebt)
}

@Dao interface OnlineProductDao {
    @Query("SELECT * FROM online_products WHERE isActive = 1 ORDER BY name ASC") fun getAllActiveFlow(): Flow<List<OnlineProduct>>
    @Query("SELECT * FROM online_products ORDER BY name ASC") fun getAllFlow(): Flow<List<OnlineProduct>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(p: OnlineProduct): Long
    @Update suspend fun update(p: OnlineProduct)
    @Delete suspend fun delete(p: OnlineProduct)
}

@Dao interface OnlineOrderDao {
    @Query("SELECT * FROM online_orders ORDER BY createdAt DESC") fun getAllFlow(): Flow<List<OnlineOrder>>
    @Query("SELECT * FROM online_orders WHERE status NOT IN ('SETTLED','CANCELLED') ORDER BY CASE WHEN shipBefore IS NOT NULL THEN shipBefore ELSE createdAt END ASC") fun getActiveFlow(): Flow<List<OnlineOrder>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(o: OnlineOrder): Long
    @Update suspend fun update(o: OnlineOrder)
    @Delete suspend fun delete(o: OnlineOrder)
}

@Database(
    entities = [Category::class, Supplier::class, StockItem::class, PriceHistory::class,
                StockMovement::class, Customer::class, Invoice::class, BankEntry::class,
                CustomerOrder::class, BusinessTask::class, SupplierDebt::class,
                CustomerItemPrice::class, OnlineProduct::class, OnlineOrder::class],
    version = 10, exportSchema = false
)
@TypeConverters(PSRTypeConverters::class)
abstract class PSRDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun supplierDao(): SupplierDao
    abstract fun stockItemDao(): StockItemDao
    abstract fun customerItemPriceDao(): CustomerItemPriceDao
    abstract fun priceHistoryDao(): PriceHistoryDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun customerDao(): CustomerDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun bankEntryDao(): BankEntryDao
    abstract fun customerOrderDao(): CustomerOrderDao
    abstract fun supplierDebtDao(): SupplierDebtDao
    abstract fun businessTaskDao(): BusinessTaskDao
    abstract fun onlineProductDao(): OnlineProductDao
    abstract fun onlineOrderDao(): OnlineOrderDao

    companion object {
        @Volatile private var INSTANCE: PSRDatabase? = null
        fun getDatabase(context: Context): PSRDatabase = INSTANCE ?: synchronized(this) {
            Room.databaseBuilder(context.applicationContext, PSRDatabase::class.java, "psrmart_db")
                .fallbackToDestructiveMigration()
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        db.execSQL("INSERT INTO categories (name, emoji, colorHex, sortOrder) VALUES ('Ayam','🐔','#1A4D2E',0),('Ikan','🐟','#0D4C8B',1),('Sayur','🥬','#1A5C3A',2),('Buah','🍎','#8B1A2E',3),('Lain-lain','📦','#5A4A1A',4)")
                        applyIndexes(db)
                    }
                }).build().also { INSTANCE = it }
        }

        fun applyIndexes(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_bank_account ON bank_entries(account)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_bank_date ON bank_entries(entryDate)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_inv_status ON invoices(status)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_inv_issued ON invoices(issuedAt)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_inv_customer ON invoices(customerId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_movement_item ON stock_movements(stockItemId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_orders_arch ON orders(isArchived)")
        }
    }
}
