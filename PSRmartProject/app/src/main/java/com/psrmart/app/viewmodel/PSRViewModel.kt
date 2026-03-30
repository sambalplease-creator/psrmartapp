package com.psrmart.app.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.psrmart.app.data.model.*
import com.psrmart.app.data.repository.PSRRepository
import com.psrmart.app.data.repository.ImportResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

class PSRViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = PSRRepository(app)

    val categories      = repo.categories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val allStockItems   = repo.allStock.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val customers       = repo.customers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val invoices        = repo.invoices.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val bankEntries     = repo.bankEntries.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val businessSettings= repo.businessSettings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BusinessSettings())

    // Fast SQL aggregates — no full-table loads in UI
    val totalProfit = repo.totalProfit.map { it ?: 0.0 }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    // Today's profit — from midnight today. Resets automatically each new day.
    val todayProfit = repo.profitSince(startOfToday())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)
    val weekProfit  = repo.profitSince(startOfWeek())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)
    val monthProfit = repo.profitSince(startOfMonth())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)
    val unpaidsCount   = repo.unpaidsCount.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val unpaidAmount   = repo.unpaidAmount.map { it ?: 0.0 }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)
    val maybankBalance = repo.maybankBalance.map { it ?: 0.0 }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)
    val cashBalance    = repo.cashBalance.map { it ?: 0.0 }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)
    val savingsBalance = repo.savingsBalance.map { it ?: 0.0 }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    // Flush state
    private val _flushState = MutableStateFlow<FlushState>(FlushState.Idle)
    val flushState: StateFlow<FlushState> = _flushState

    // Snackbar messages
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage
    fun showMessage(msg: String) { _snackbarMessage.value = msg }
    fun clearMessage() { _snackbarMessage.value = null }

    // Stock Search
    private val _stockSearchQuery = MutableStateFlow("")
    val stockSearchQuery: StateFlow<String> = _stockSearchQuery
    val filteredStock = combine(allStockItems, _stockSearchQuery) { items, q ->
        if (q.isBlank()) items else items.filter { it.name.contains(q, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    fun setStockQuery(q: String) { _stockSearchQuery.value = q }

    // Navigation
    private val _navigateToInvoiceId = MutableStateFlow<Long?>(null)
    val navigateToInvoiceId: StateFlow<Long?> = _navigateToInvoiceId
    fun navigateToInvoice(id: Long) { _navigateToInvoiceId.value = id }
    fun clearNavigation() { _navigateToInvoiceId.value = null }

    // Signal to navigate to the Invoices pane (pane index 3) from anywhere
    private val _navigateToInvoicesPane = MutableStateFlow(false)
    val navigateToInvoicesPane: StateFlow<Boolean> = _navigateToInvoicesPane
    fun triggerNavigateToInvoicesPane() { _navigateToInvoicesPane.value = true }
    fun clearNavigateToInvoicesPane() { _navigateToInvoicesPane.value = false }

    // Customer-specific item pricing
    fun getCustomerItemPrice(customerId: Long, stockItemId: Long, onResult: (Double?) -> Unit) =
        viewModelScope.launch { onResult(repo.getCustomerItemPrice(customerId, stockItemId)) }

    fun getCustomerItemPriceWithDate(customerId: Long, stockItemId: Long, onResult: (Double?, String?) -> Unit) =
        viewModelScope.launch {
            val (price, date) = repo.getCustomerItemPriceWithDate(customerId, stockItemId)
            onResult(price, date)
        }

    // Map of stockItemId → CustomerItemPrice for the currently selected customer
    // Loaded whenever draftCustomerId changes so picker shows correct prices instantly
    private val _customerPriceMap = MutableStateFlow<Map<Long, CustomerItemPrice>>(emptyMap())
    val customerPriceMap: StateFlow<Map<Long, CustomerItemPrice>> = _customerPriceMap
    fun setCustomerItemPrice(customerId: Long, stockItemId: Long, price: Double) =
        viewModelScope.launch { repo.setCustomerItemPrice(customerId, stockItemId, price) }

    // Auto-create supplier by name if not existing
    fun findOrCreateSupplier(name: String, onResult: (Long) -> Unit) =
        viewModelScope.launch { onResult(repo.findOrCreateSupplier(name)) }

    // Draft Invoice State
    private val _draftCustomer      = MutableStateFlow<CustomerSnapshot?>(null)
    private val _draftCustomerId    = MutableStateFlow<Long?>(null)   // DB id of selected customer
    val draftCustomerId: StateFlow<Long?> = _draftCustomerId
    private val _draftItems         = MutableStateFlow<List<InvoiceItem>>(emptyList())
    private val _draftDiscount      = MutableStateFlow(0.0)
    private val _draftRoundOff      = MutableStateFlow(0.0)
    private val _draftNotes         = MutableStateFlow("")
    private val _draftInvoiceNumber = MutableStateFlow("")
    private val _draftDate          = MutableStateFlow(System.currentTimeMillis())
    private val _editingInvoiceId   = MutableStateFlow<Long?>(null)

    val draftCustomer: StateFlow<CustomerSnapshot?> = _draftCustomer
    val draftItems: StateFlow<List<InvoiceItem>> = _draftItems
    val draftDiscount: StateFlow<Double> = _draftDiscount
    val draftRoundOff: StateFlow<Double> = _draftRoundOff
    val draftNotes: StateFlow<String> = _draftNotes
    val draftInvoiceNumber: StateFlow<String> = _draftInvoiceNumber
    val draftDate: StateFlow<Long> = _draftDate
    val editingInvoiceId: StateFlow<Long?> = _editingInvoiceId

    val draftSubtotal = _draftItems.map { it.sumOf { i -> i.netTotal } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)
    val draftTotal = combine(_draftItems, _draftDiscount, _draftRoundOff) { items, disc, round ->
        items.sumOf { it.netTotal } - disc + round
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)
    val draftProfit = combine(_draftItems, _draftDiscount, _draftRoundOff) { items, disc, round ->
        items.sumOf { it.netTotal - it.lineCost } - disc + round
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    init {
        viewModelScope.launch {
            businessSettings.collect { s ->
                if (_draftInvoiceNumber.value.isBlank() && _editingInvoiceId.value == null) {
                    _draftInvoiceNumber.value = "INV-${s.invoiceNextNumber.toString().padStart(4, '0')}"
                }
            }
        }
        // Archive yesterday's completed orders on startup
        viewModelScope.launch { repo.archiveOldOrders() }
    }

    fun setDraftCustomer(c: CustomerSnapshot?) {
        _draftCustomer.value    = c
        _draftCustomerId.value  = null
        _customerPriceMap.value = emptyMap()
        if (_editingInvoiceId.value != null) return
        viewModelScope.launch { _autoSetInvoiceNumber(null) }
    }

    /**
     * Wire this when selecting a saved Customer (has a DB id).
     * Looks up their last invoice number and pre-loads all remembered prices.
     */
    fun setDraftCustomerWithId(c: CustomerSnapshot?, customerId: Long?) {
        _draftCustomer.value   = c
        _draftCustomerId.value = customerId
        if (_editingInvoiceId.value != null) return
        viewModelScope.launch {
            _autoSetInvoiceNumber(customerId)
            _customerPriceMap.value = if (customerId != null && customerId > 0)
                repo.getAllCustomerItemPrices(customerId)
            else emptyMap()
        }
    }

    private suspend fun _autoSetInvoiceNumber(customerId: Long?) {
        val settings = businessSettings.value
        val nextNum: Int = if (customerId != null && customerId > 0) {
            val lastInv = repo.getLastInvoiceForCustomer(customerId)
            if (lastInv != null) {
                // Extract ONLY digits from invoice number, parse as Int, +1
                val digits = lastInv.invoiceNumber.filter { it.isDigit() }
                digits.toIntOrNull()?.plus(1) ?: settings.invoiceNextNumber
            } else settings.invoiceNextNumber
        } else settings.invoiceNextNumber
        _draftInvoiceNumber.value = "INV-${nextNum.toString().padStart(4, '0')}"
    }

    // ── Supplier Debts ─────────────────────────────────────────────────────
    val activeDebts     = repo.activeDebts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val allDebts        = repo.allDebts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val totalDebtOwed   = repo.totalDebtOwed.map { it ?: 0.0 }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)
    val activeDebtCount = repo.activeDebtCount.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun addDebt(d: SupplierDebt) = viewModelScope.launch {
        repo.insertDebt(d)
        showMessage("📋 Debt to ${d.supplierName} recorded — RM ${"%.2f".format(d.totalAmount)}")
    }
    fun payDebt(debt: SupplierDebt, amount: Double, account: FinancialAccount) = viewModelScope.launch {
        val newPaid   = (debt.amountPaid + amount).coerceAtMost(debt.totalAmount)
        val newStatus = when {
            newPaid >= debt.totalAmount -> DebtStatus.PAID
            newPaid > 0                -> DebtStatus.PARTIAL
            else                       -> DebtStatus.UNPAID
        }
        repo.updateDebt(debt.copy(amountPaid = newPaid, status = newStatus))
        // Debit from the selected account
        repo.insertBankEntry(BankEntry(
            description = "Debt Payment: ${debt.supplierName}${if (debt.description.isNotBlank()) " — ${debt.description}" else ""}",
            amount      = -amount,
            account     = account,
            category    = "Debt Repayment",
            isManual    = false
        ))
        if (newStatus == DebtStatus.PAID)
            showMessage("✅ Debt to ${debt.supplierName} fully settled!")
        else
            showMessage("💵 Paid RM ${"%.2f".format(amount)} — RM ${"%.2f".format(debt.totalAmount - newPaid)} remaining")
    }
    fun deleteDebt(d: SupplierDebt) = viewModelScope.launch {
        repo.deleteDebt(d)
        showMessage("🗑️ Debt record removed")
    }
    fun setDraftDiscount(d: Double) { _draftDiscount.value = d }
    fun setDraftRoundOff(r: Double) { _draftRoundOff.value = r }
    fun setDraftNotes(n: String) { _draftNotes.value = n }
    fun setDraftInvoiceNumber(n: String) {
        // Accept either a raw 4-digit string (from the UI field) or a full "INV-XXXX"
        val digits = n.filter { it.isDigit() }.takeLast(4).padStart(4, '0')
        _draftInvoiceNumber.value = "INV-$digits"
    }
    fun setDraftDate(d: Long) { _draftDate.value = d }
    fun addDraftItem(item: InvoiceItem) { _draftItems.value = _draftItems.value + item }
    fun removeDraftItem(index: Int) { _draftItems.value = _draftItems.value.toMutableList().also { it.removeAt(index) } }
    fun updateDraftItem(index: Int, item: InvoiceItem) { _draftItems.value = _draftItems.value.toMutableList().also { it[index] = item } }

    fun applyRoundOff(mode: RoundOffMode) {
        val base = draftSubtotal.value - _draftDiscount.value
        val rounded = when (mode) {
            RoundOffMode.NONE       -> base
            RoundOffMode.NEAREST_50 -> (base / 0.5).roundToInt() * 0.5
            RoundOffMode.ROUND_UP   -> ceil(base)
            RoundOffMode.ROUND_DOWN -> floor(base)
        }
        _draftRoundOff.value = rounded - base
    }

    fun loadInvoiceForEdit(invoice: Invoice) {
        _editingInvoiceId.value   = invoice.id
        _draftInvoiceNumber.value = invoice.invoiceNumber
        _draftCustomer.value      = invoice.customerSnapshot
        _draftItems.value         = invoice.items
        _draftDiscount.value      = invoice.discountAmount
        _draftRoundOff.value      = invoice.roundOff
        _draftNotes.value         = invoice.notes
        _draftDate.value          = invoice.issuedAt
        // Pre-load customer price map for editing — uses customerId if available
        val cid = invoice.customerId
        if (cid != null && cid > 0) {
            _draftCustomerId.value = cid
            viewModelScope.launch {
                _customerPriceMap.value = repo.getAllCustomerItemPrices(cid)
            }
        }
    }

    fun clearDraft() {
        _editingInvoiceId.value   = null
        _draftCustomer.value      = null
        _draftCustomerId.value    = null
        _customerPriceMap.value   = emptyMap()
        _draftItems.value         = emptyList()
        _draftDiscount.value      = 0.0
        _draftRoundOff.value      = 0.0
        _draftNotes.value         = ""
        _draftInvoiceNumber.value = ""
        _draftDate.value          = System.currentTimeMillis()
    }

    // Stock CRUD
    fun insertCategory(c: Category) = viewModelScope.launch { repo.insertCategory(c) }
    fun insertStock(item: StockItem) = viewModelScope.launch {
        repo.insertStock(item)
        showMessage("✅ New product added — ${item.name}")
    }
    fun updateStock(item: StockItem) = viewModelScope.launch {
        repo.updateStock(item)
        showMessage("💰 New price added for ${item.name}")
    }
    fun deleteStock(item: StockItem) = viewModelScope.launch {
        repo.deleteStock(item)
        showMessage("🗑️ ${item.name} removed from catalogue")
    }

    // Customer CRUD
    fun insertCustomer(c: Customer) = viewModelScope.launch { repo.insertCustomer(c) }
    fun updateCustomer(c: Customer) = viewModelScope.launch { repo.updateCustomer(c) }
    fun deleteCustomer(c: Customer) = viewModelScope.launch { repo.deleteCustomer(c) }

    // Bank Entries
    fun addBankEntry(e: BankEntry) = viewModelScope.launch { repo.insertBankEntry(e) }
    fun deleteBankEntry(e: BankEntry) = viewModelScope.launch { repo.deleteBankEntry(e) }

    // Save invoice (handles both new and edit)
    fun saveInvoice(onSaved: (Long) -> Unit = {}) = viewModelScope.launch {
        val editId = _editingInvoiceId.value
        if (editId != null) {
            val old = invoices.value.find { it.id == editId } ?: return@launch
            updateInvoiceInternal(old, onSaved)
        } else {
            insertInvoiceInternal(onSaved)
        }
    }

    private suspend fun insertInvoiceInternal(onSaved: (Long) -> Unit) {
        val customer = _draftCustomer.value ?: CustomerSnapshot("Walk-in Customer","","","","")
        val items    = _draftItems.value
        val subtotal = items.sumOf { it.netTotal }
        val discount = _draftDiscount.value
        val roundOff = _draftRoundOff.value
        val total    = subtotal - discount + roundOff
        val cost     = items.sumOf { it.lineCost }
        val settings = businessSettings.value
        val invNo    = _draftInvoiceNumber.value.ifBlank {
            "INV-${settings.invoiceNextNumber.toString().padStart(4, '0')}"
        }
        val invoice = Invoice(
            invoiceNumber    = invNo,
            customerId       = _draftCustomerId.value,
            customerSnapshot = customer,
            items            = items,
            subtotal         = subtotal,
            discountAmount   = discount,
            roundOff         = roundOff,
            totalAmount      = total,
            totalCost        = cost,
            netProfit        = total - cost,
            notes            = _draftNotes.value,
            issuedAt         = _draftDate.value
        )
        val id = repo.insertInvoice(invoice)
        repo.saveBusinessSettings(settings.copy(invoiceNextNumber = settings.invoiceNextNumber + 1))
        showMessage("🎉 Congrats on the sale! Invoice $invNo generated.")
        clearDraft()
        onSaved(id)
    }

    private suspend fun updateInvoiceInternal(old: Invoice, onSaved: (Long) -> Unit) {
        val customer = _draftCustomer.value ?: old.customerSnapshot
        val items    = _draftItems.value
        val subtotal = items.sumOf { it.netTotal }
        val discount = _draftDiscount.value
        val roundOff = _draftRoundOff.value
        val total    = subtotal - discount + roundOff
        val cost     = items.sumOf { it.lineCost }

        // Rebalance bank entries if total changed and there were payments
        if (old.amountPaid > 0 && old.totalAmount != total) {
            val oldPayments = bankEntries.value.filter { it.invoiceId == old.id && !it.isManual }
            oldPayments.forEach { repo.deleteBankEntry(it) }
            val scale = if (old.totalAmount > 0) total / old.totalAmount else 1.0
            oldPayments.forEach { entry ->
                repo.insertBankEntry(entry.copy(id = 0, amount = entry.amount * scale))
            }
        }

        repo.updateInvoice(old.copy(
            invoiceNumber    = _draftInvoiceNumber.value,
            customerSnapshot = customer,
            items            = items,
            subtotal         = subtotal,
            discountAmount   = discount,
            roundOff         = roundOff,
            totalAmount      = total,
            totalCost        = cost,
            netProfit        = total - cost,
            notes            = _draftNotes.value,
            issuedAt         = _draftDate.value
        ))
        showMessage("✏️ Invoice ${old.invoiceNumber} updated")
        clearDraft()
        onSaved(old.id)
    }

    fun deleteInvoice(invoice: Invoice) = viewModelScope.launch {
        // Reverse all payment bank entries
        bankEntries.value.filter { it.invoiceId == invoice.id && !it.isManual }
            .forEach { repo.deleteBankEntry(it) }
        repo.deleteInvoice(invoice)
        showMessage("😬 Oh no — invoice ${invoice.invoiceNumber} deleted!")
    }

    /** Delete invoice but keep the bank payment entries (money already received) */
    fun deleteInvoiceKeepPayments(invoice: Invoice) = viewModelScope.launch {
        repo.deleteInvoice(invoice)
        showMessage("Invoice ${invoice.invoiceNumber} deleted. Balance unchanged.")
    }

    /** Bulk delete invoices — keeps bank entries for paid ones */
    fun bulkDeleteInvoices(invoices: List<Invoice>) = viewModelScope.launch {
        invoices.forEach { inv ->
            if (inv.status != InvoiceStatus.PAID) {
                // Unpaid — reverse any partial payment entries
                bankEntries.value.filter { it.invoiceId == inv.id && !it.isManual }
                    .forEach { repo.deleteBankEntry(it) }
            }
            // Paid — just delete the invoice, bank entries stay
            repo.deleteInvoice(inv)
        }
        showMessage("🗑 ${invoices.size} invoice${if (invoices.size != 1) "s" else ""} deleted.")
    }

    fun processInvoicePayment(invoice: Invoice, amount: Double, date: Long, account: FinancialAccount) = viewModelScope.launch {
        val newAmountPaid = invoice.amountPaid + amount
        val newStatus = when {
            newAmountPaid >= invoice.totalAmount -> InvoiceStatus.PAID
            newAmountPaid > 0 -> InvoiceStatus.PARTIAL
            else -> InvoiceStatus.UNPAID
        }
        repo.updateInvoice(invoice.copy(
            amountPaid = newAmountPaid,
            status = newStatus,
            paidAt = if (newStatus == InvoiceStatus.PAID) date else null
        ))
        repo.insertBankEntry(BankEntry(
            description = "Payment: ${invoice.invoiceNumber} - ${invoice.customerSnapshot.name}",
            amount = amount,
            account = account,
            category = "Invoice Payment",
            invoiceId = invoice.id,
            entryDate = date,
            isManual = false
        ))
        if (newStatus == InvoiceStatus.PAID)
            showMessage("🎉 Congrats! ${invoice.customerSnapshot.name} fully paid!")
        else
            showMessage("💵 Partial payment of RM %.2f recorded".format(amount))
    }

    fun markInvoiceUnpaid(invoice: Invoice) = viewModelScope.launch {
        bankEntries.value.filter { it.invoiceId == invoice.id && !it.isManual }
            .forEach { repo.deleteBankEntry(it) }
        repo.updateInvoice(invoice.copy(status = InvoiceStatus.UNPAID, amountPaid = 0.0, paidAt = null))
        showMessage("↩️ Invoice ${invoice.invoiceNumber} marked as unpaid")
    }

    val activeOrders = repo.activeOrders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val allOrders    = repo.allOrders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Business Tasks ─────────────────────────────────────────────────────
    val activeTasks = repo.activeTasks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val doneTasks   = repo.doneTasks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun insertTask(t: BusinessTask) = viewModelScope.launch {
        repo.insertTask(t)
        showMessage("📌 Task added: ${t.title}")
    }
    fun updateTask(t: BusinessTask) = viewModelScope.launch { repo.updateTask(t) }
    fun deleteTask(t: BusinessTask) = viewModelScope.launch {
        repo.deleteTask(t)
        showMessage("🗑️ Task removed")
    }
    fun completeTask(t: BusinessTask) = viewModelScope.launch {
        repo.updateTask(t.copy(status = TaskStatus.DONE, completedAt = System.currentTimeMillis()))
        showMessage("✅ Done! \"${t.title}\"")
    }
    fun uncompleteTask(t: BusinessTask) = viewModelScope.launch {
        repo.updateTask(t.copy(status = TaskStatus.TODO, completedAt = null))
    }
    fun toggleTaskPin(t: BusinessTask) = viewModelScope.launch {
        repo.updateTask(t.copy(isPinned = !t.isPinned))
    }

    fun saveOrder(order: CustomerOrder) = viewModelScope.launch {
        repo.insertOrder(order)
        showMessage("📋 Order for ${order.customerName} saved!")
    }

    fun updateOrder(order: CustomerOrder) = viewModelScope.launch {
        repo.updateOrder(order)
    }

    fun deleteOrder(order: CustomerOrder) = viewModelScope.launch {
        repo.deleteOrder(order)
        showMessage("🗑️ Order for ${order.customerName} removed")
    }

    fun toggleOrderItem(order: CustomerOrder, itemId: String) = viewModelScope.launch {
        val updated = order.copy(items = order.items.map { if (it.id == itemId) it.copy(isChecked = !it.isChecked, isPurchased = if (!it.isChecked) true else it.isPurchased) else it })
        repo.updateOrder(updated)
        if (updated.isComplete) showMessage("✅ All done for ${order.customerName}!")
    }

    fun toggleOrderItemPurchased(order: CustomerOrder, itemId: String) = viewModelScope.launch {
        val updated = order.copy(items = order.items.map {
            if (it.id == itemId) it.copy(isPurchased = !it.isPurchased, isChecked = if (it.isPurchased) false else it.isChecked)
            else it
        })
        repo.updateOrder(updated)
    }

    fun editOrderItemText(order: CustomerOrder, itemId: String, newText: String) = viewModelScope.launch {
        val updated = order.copy(items = order.items.map { if (it.id == itemId) it.copy(text = newText) else it })
        repo.updateOrder(updated)
    }

    fun previewFlush(beforeMs: Long) = viewModelScope.launch {
        _flushState.value = FlushState.Previewing
        val report = repo.buildFlushReport(beforeMs)
        _flushState.value = FlushState.Ready(report)
    }

    fun confirmFlush(beforeMs: Long) = viewModelScope.launch {
        _flushState.value = FlushState.Running
        try {
            repo.performFlush(beforeMs)
            _flushState.value = FlushState.Done
            showMessage("🧹 Flush complete! Data archived and space reclaimed.")
        } catch (e: Exception) {
            _flushState.value = FlushState.Idle
            showMessage("❌ Flush failed — please try again")
        }
    }

    fun confirmNuclearFlush(maybankOpening: Double, cashOpening: Double, savingsOpening: Double) = viewModelScope.launch {
        _flushState.value = FlushState.Running
        try {
            repo.nuclearFlush(maybankOpening, cashOpening, savingsOpening)
            _flushState.value = FlushState.Done
            showMessage("🧹 Full reset complete. Opening balances saved.")
        } catch (e: Exception) {
            _flushState.value = FlushState.Idle
            showMessage("❌ Reset failed: ${e.message}")
        }
    }

    fun resetFlushState() { _flushState.value = FlushState.Idle }

    private val _transactionExportFile = MutableStateFlow<java.io.File?>(null)
    val transactionExportFile: StateFlow<java.io.File?> = _transactionExportFile

    fun exportTransactions() = viewModelScope.launch {
        val file = repo.exportTransactionsAsCsv()
        if (file != null) _transactionExportFile.value = file
        else showMessage("❌ Export failed")
    }

    fun clearTransactionExport() { _transactionExportFile.value = null }

    fun saveSettings(s: BusinessSettings) = viewModelScope.launch {
        repo.saveBusinessSettings(s)
        showMessage("⚙️ Settings saved!")
    }

    // ── Catalogue Backup ───────────────────────────────────────────────────
    private val _catalogueExportFile = MutableStateFlow<java.io.File?>(null)
    val catalogueExportFile: StateFlow<java.io.File?> = _catalogueExportFile

    fun exportCatalogue() = viewModelScope.launch {
        val file = repo.exportCatalogueAsFile()
        if (file != null) {
            _catalogueExportFile.value = file
        } else {
            showMessage("❌ Export failed — please try again")
        }
    }

    fun clearCatalogueExport() { _catalogueExportFile.value = null }

    private val _importResult = MutableStateFlow<ImportResult?>(null)
    val importResult: StateFlow<ImportResult?> = _importResult

    fun importCatalogue(jsonText: String) = viewModelScope.launch {
        _importResult.value = null
        val result = repo.importCatalogueFromJson(jsonText)
        _importResult.value = result
        if (result.success) showMessage("✅ ${result.message}")
        else showMessage("❌ ${result.message}")
    }

    fun clearImportResult() { _importResult.value = null }

    // ── Suppliers ─────────────────────────────────────────────────────────
    val suppliers = repo.suppliers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Online Orders ──────────────────────────────────────────────────────
    val onlineProducts     = repo.onlineProducts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val allOnlineProducts  = repo.allOnlineProducts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val activeOnlineOrders = repo.activeOnlineOrders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val allOnlineOrders    = repo.allOnlineOrders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveOnlineProduct(p: OnlineProduct) = viewModelScope.launch {
        if (p.id == 0L) repo.insertOnlineProduct(p) else repo.updateOnlineProduct(p)
    }
    fun deleteOnlineProduct(p: OnlineProduct) = viewModelScope.launch { repo.deleteOnlineProduct(p) }

    fun placeOnlineOrder(o: OnlineOrder) = viewModelScope.launch {
        repo.insertOnlineOrder(o)
        showMessage("📦 Order for ${o.customerName} created")
    }
    fun updateOnlineOrder(o: OnlineOrder) = viewModelScope.launch { repo.updateOnlineOrder(o) }
    fun deleteOnlineOrder(o: OnlineOrder) = viewModelScope.launch { repo.deleteOnlineOrder(o) }

    fun settleOnlineOrder(
        order: OnlineOrder,
        platformFeeRm: Double,
        shippingSubsidy: Double,
        extraCosts: Double,
        extraCostNote: String,
        account: FinancialAccount
    ) = viewModelScope.launch {
        repo.settleOnlineOrder(order, platformFeeRm, shippingSubsidy, extraCosts, extraCostNote, account)
        showMessage("✅ Order settled — RM ${"%.2f".format(order.subtotal - platformFeeRm - extraCosts + shippingSubsidy)} added to ledger")
    }

    fun insertSupplier(s: Supplier) = viewModelScope.launch {
        repo.insertSupplier(s)
        showMessage("🏭 Supplier ${s.name} added")
    }
    fun updateSupplier(s: Supplier) = viewModelScope.launch { repo.updateSupplier(s) }
    fun deleteSupplier(s: Supplier) = viewModelScope.launch {
        repo.deleteSupplier(s)
        showMessage("🗑️ ${s.name} removed")
    }

    // ── Stock Movements ────────────────────────────────────────────────────
    val stockMovements = repo.allMovements.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun recordStockIn(item: StockItem, qty: Double, unitCost: Double, supplierName: String, account: FinancialAccount, notes: String = "") =
        viewModelScope.launch {
            repo.recordStockIn(item, qty, unitCost, supplierName, account, notes)
            showMessage("📦 ${item.name} — %.1f${item.unit} received".format(qty))
        }

    fun recordSellStock(item: StockItem, qty: Double, unitSell: Double, unitCost: Double, account: FinancialAccount, notes: String = "") =
        viewModelScope.launch {
            repo.recordSellStock(item, qty, unitSell, unitCost, account, notes)
            showMessage("🎉 Sold %.1f${item.unit} of ${item.name}".format(qty))
        }

    fun recordAdjustment(item: StockItem, newQty: Double, notes: String = "") =
        viewModelScope.launch {
            repo.recordAdjustment(item, newQty, notes)
            showMessage("🔄 ${item.name} adjusted to %.1f${item.unit}".format(newQty))
        }

    fun quickAddStockItem(item: StockItem, saveToDb: Boolean, onDone: (StockItem) -> Unit) =
        viewModelScope.launch {
            if (saveToDb) {
                val id = repo.insertStock(item)
                onDone(item.copy(id = id))
                showMessage("✅ ${item.name} added to catalogue")
            } else {
                onDone(item)
            }
        }
}

sealed class FlushState {
    object Idle      : FlushState()
    object Previewing: FlushState()
    data class Ready(val report: com.psrmart.app.data.repository.FlushReport) : FlushState()
    object Running   : FlushState()
    object Done      : FlushState()
}

enum class RoundOffMode { NONE, NEAREST_50, ROUND_UP, ROUND_DOWN }

class PSRViewModelFactory(private val app: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PSRViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PSRViewModel(app) as T
        }
        throw IllegalArgumentException("Unknown ViewModel")
    }
}

// ── Time helpers — start-of-period timestamps in local time ──────────────

fun startOfToday(): Long {
    val cal = java.util.Calendar.getInstance()
    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
    cal.set(java.util.Calendar.MINUTE, 0)
    cal.set(java.util.Calendar.SECOND, 0)
    cal.set(java.util.Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

fun startOfWeek(): Long {
    val cal = java.util.Calendar.getInstance()
    cal.set(java.util.Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
    cal.set(java.util.Calendar.MINUTE, 0)
    cal.set(java.util.Calendar.SECOND, 0)
    cal.set(java.util.Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

fun startOfMonth(): Long {
    val cal = java.util.Calendar.getInstance()
    cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
    cal.set(java.util.Calendar.MINUTE, 0)
    cal.set(java.util.Calendar.SECOND, 0)
    cal.set(java.util.Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}
