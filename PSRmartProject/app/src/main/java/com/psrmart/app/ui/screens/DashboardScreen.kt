@file:OptIn(ExperimentalMaterial3Api::class)
package com.psrmart.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.psrmart.app.data.model.*
import com.psrmart.app.ui.components.*
import com.psrmart.app.ui.theme.PSRColors
import com.psrmart.app.ui.theme.MontserratFamily
import com.psrmart.app.viewmodel.PSRViewModel
import com.psrmart.app.viewmodel.startOfToday
import com.psrmart.app.viewmodel.startOfWeek
import com.psrmart.app.viewmodel.startOfMonth
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: PSRViewModel) {
    val maybankBalance by viewModel.maybankBalance.collectAsState()
    val cashBalance    by viewModel.cashBalance.collectAsState()
    val savingsBalance by viewModel.savingsBalance.collectAsState()
    val totalProfit    by viewModel.totalProfit.collectAsState()
    val todayProfit    by viewModel.todayProfit.collectAsState()
    val weekProfit     by viewModel.weekProfit.collectAsState()
    val monthProfit    by viewModel.monthProfit.collectAsState()
    val unpaidsCount   by viewModel.unpaidsCount.collectAsState()
    val unpaidAmount   by viewModel.unpaidAmount.collectAsState()
    val totalDebtOwed  by viewModel.totalDebtOwed.collectAsState()
    val activeDebtCount by viewModel.activeDebtCount.collectAsState()
    val bankEntries    by viewModel.bankEntries.collectAsState()
    val invoices       by viewModel.invoices.collectAsState()
    val customers      by viewModel.customers.collectAsState()
    val settings       by viewModel.businessSettings.collectAsState()
    val suppliers      by viewModel.suppliers.collectAsState()
    val allStock       by viewModel.allStockItems.collectAsState()

    val totalLiquidity = maybankBalance + cashBalance

    var showAddExpenseSheet   by remember { mutableStateOf(false) }
    var showDebtManager       by remember { mutableStateOf(false) }
    var showAdjustmentSheet   by remember { mutableStateOf(false) }
    var adjustAccountTarget   by remember { mutableStateOf<FinancialAccount?>(null) }
    var selectedAccountFilter by remember { mutableStateOf<FinancialAccount?>(null) }
    var showProfitLog         by remember { mutableStateOf(false) }
    var showMoneyTimeline     by remember { mutableStateOf(false) }
    var showDailySummary      by remember { mutableStateOf(false) }
    var showTransferSheet     by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(PSRColors.Surface)) {
        Box(
            modifier = Modifier.fillMaxWidth()
                .background(Brush.verticalGradient(listOf(PSRColors.Navy600, PSRColors.Navy800)))
                .padding(20.dp)
        ) {
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Finance", style = MaterialTheme.typography.labelLarge, color = PSRColors.White.copy(alpha = 0.65f))
                    Row {
                        IconButton(onClick = { showDebtManager = true },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = PSRColors.White.copy(alpha = 0.1f))) {
                            Icon(Icons.Outlined.AccountBalance, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = { showTransferSheet = true },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = PSRColors.White.copy(alpha = 0.1f))) {
                            Icon(Icons.Default.SwapHoriz, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = { showAddExpenseSheet = true },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = PSRColors.White.copy(alpha = 0.1f))) {
                            Icon(Icons.Default.Add, null, tint = Color.White)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Column(modifier = Modifier.clickable { showMoneyTimeline = true }) {
                    AnimatedBalance(amount = totalLiquidity,
                        style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Black),
                        color = PSRColors.White)
                    Text("Tap for money timeline", style = MaterialTheme.typography.labelSmall, color = PSRColors.White.copy(alpha = 0.45f))
                }
                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AccountBalanceCard(settings.labelFor(FinancialAccount.MAYBANK), maybankBalance, Icons.Outlined.AccountBalance,
                        selectedAccountFilter == FinancialAccount.MAYBANK, Modifier.weight(1f),
                        onClick = { selectedAccountFilter = if (selectedAccountFilter == FinancialAccount.MAYBANK) null else FinancialAccount.MAYBANK },
                        onLongClick = { adjustAccountTarget = FinancialAccount.MAYBANK; showAdjustmentSheet = true })
                    AccountBalanceCard(settings.labelFor(FinancialAccount.CASH), cashBalance, Icons.Outlined.Payments,
                        selectedAccountFilter == FinancialAccount.CASH, Modifier.weight(1f),
                        onClick = { selectedAccountFilter = if (selectedAccountFilter == FinancialAccount.CASH) null else FinancialAccount.CASH },
                        onLongClick = { adjustAccountTarget = FinancialAccount.CASH; showAdjustmentSheet = true })
                    AccountBalanceCard(settings.labelFor(FinancialAccount.SAVINGS), savingsBalance, Icons.Outlined.Savings,
                        selectedAccountFilter == FinancialAccount.SAVINGS, Modifier.weight(1f),
                        onClick = { selectedAccountFilter = if (selectedAccountFilter == FinancialAccount.SAVINGS) null else FinancialAccount.SAVINGS },
                        onLongClick = { adjustAccountTarget = FinancialAccount.SAVINGS; showAdjustmentSheet = true })
                }
                // Adjustments hint
                Text("Long-press any account to adjust balance",
                    style = MaterialTheme.typography.labelSmall, color = PSRColors.White.copy(0.4f),
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp))
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 100.dp)) {
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(
                        title = "Today's Profit",
                        value = formatRM(todayProfit),
                        subtitle = "All-time: ${formatRM(totalProfit)}",
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        iconTint = if (todayProfit >= 0) PSRColors.Success else PSRColors.Error,
                        valueColor = if (todayProfit >= 0) PSRColors.Success else PSRColors.Error,
                        valueStyle = MaterialTheme.typography.titleLarge.copy(fontFamily = MontserratFamily, fontWeight = FontWeight.Bold, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                        modifier = Modifier.weight(1f),
                        onClick = { showProfitLog = true }
                    )
                    StatCard(title = "Unpaid Invoices", value = unpaidsCount.toString(),
                        subtitle = formatRM(unpaidAmount),
                        icon = Icons.Outlined.ReceiptLong, iconTint = PSRColors.Warning,
                        valueStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.triggerNavigateToInvoicesPane() })
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(title = "Total Debt", value = formatRM(totalDebtOwed),
                        icon = Icons.Outlined.AccountBalance, iconTint = if (totalDebtOwed > 0) PSRColors.Error else PSRColors.Success,
                        valueColor = if (totalDebtOwed > 0) PSRColors.Error else PSRColors.Success,
                        valueStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        subtitle = if (activeDebtCount > 0) "$activeDebtCount active debts • RM ${"%.2f".format(totalDebtOwed)} owed" else "All clear",
                        modifier = Modifier.weight(1f),
                        onClick = { showDebtManager = true })
                    // ── Daily Summary card — bottom right ────────────
                    DailySummaryCard(
                        invoices   = invoices,
                        bankEntries = bankEntries,
                        customers  = customers,
                        modifier   = Modifier.weight(1f),
                        onClick    = { showDailySummary = true }
                    )
                }
            }

            item {
                SectionHeader(title = if (selectedAccountFilter == null) "Ledger (last 300 entries)" else "${selectedAccountFilter?.name} Entries")
            }

            val filteredEntries = if (selectedAccountFilter == null) bankEntries
                else bankEntries.filter { it.account == selectedAccountFilter }

            if (filteredEntries.isEmpty()) {
                item {
                    EmptyState(icon = Icons.Outlined.AccountBalanceWallet, title = "No entries yet",
                        subtitle = "Paid invoices and manual expenses appear here")
                }
            } else {
                items(filteredEntries, key = { "entry_${it.id}" }) { entry ->
                    LedgerEntryRow(
                        entry = entry,
                        onDelete = { viewModel.deleteBankEntry(entry) },
                        onClick = { if (entry.invoiceId != null) viewModel.navigateToInvoice(entry.invoiceId) },
                        settings = settings
                    )
                }
            }
        }
    }

    if (showAddExpenseSheet) {
        AddExpenseSheet(
            onDismiss  = { showAddExpenseSheet = false },
            onSave     = { entry: BankEntry ->
                viewModel.addBankEntry(entry)
                showAddExpenseSheet = false
            },
            settings   = settings,
            suppliers  = suppliers,
            stockItems = allStock,
            onAddDebt  = { debt ->
                viewModel.addDebt(debt)
                showAddExpenseSheet = false
            }
        )
    }

    if (showDebtManager) {
        DebtManagerSheet(viewModel = viewModel, onDismiss = { showDebtManager = false })
    }

    if (showProfitLog) {
        ProfitLogSheet(
            invoices    = invoices,
            todayProfit = todayProfit,
            weekProfit  = weekProfit,
            monthProfit = monthProfit,
            totalProfit = totalProfit,
            onDismiss   = { showProfitLog = false }
        )
    }
    if (showMoneyTimeline) {
        MoneyTimelineSheet(entries = bankEntries, onDismiss = { showMoneyTimeline = false })
    }
    if (showDailySummary) {
        DailySummarySheet(
            invoices    = invoices,
            bankEntries = bankEntries,
            customers   = customers,
            onDismiss   = { showDailySummary = false }
        )
    }
    if (showTransferSheet) {
        InterTransferSheet(
            settings  = settings,
            onDismiss = { showTransferSheet = false },
            onTransfer = { from, to, amount ->
                viewModel.addBankEntry(BankEntry(
                    description = "Transfer to ${settings.labelFor(to)}",
                    amount = -amount, account = from, category = "Transfer", isManual = true))
                viewModel.addBankEntry(BankEntry(
                    description = "Transfer from ${settings.labelFor(from)}",
                    amount = amount, account = to, category = "Transfer", isManual = true))
                showTransferSheet = false
            }
        )
    }
    if (showAdjustmentSheet && adjustAccountTarget != null) {
        val currentBalance = when (adjustAccountTarget) {
            FinancialAccount.MAYBANK  -> maybankBalance
            FinancialAccount.CASH     -> cashBalance
            FinancialAccount.SAVINGS  -> savingsBalance
            else -> 0.0
        }
        AdjustmentSheet(
            account        = adjustAccountTarget!!,
            currentBalance = currentBalance,
            accountLabel   = settings.labelFor(adjustAccountTarget!!),
            onDismiss      = { showAdjustmentSheet = false; adjustAccountTarget = null },
            onSave         = { entry ->
                viewModel.addBankEntry(entry)
                showAdjustmentSheet = false
                adjustAccountTarget = null
            }
        )
    }
}

// ─────────────────────────────────────────────
// DATA CLASSES FOR TIMELINE
// ─────────────────────────────────────────────

private data class TLEntry(val entry: BankEntry, val runningBalance: Double)
private data class TLPinGroup(
    val entries: List<TLEntry>,
    val isIncome: Boolean,
    val totalAmount: Double,
    val earliestTime: Long,
    val latestTime: Long
)
private data class TLCheckpoint(val balance: Double, val label: String)
private data class TLTimelineItem(
    val isCheckpoint: Boolean = false,
    val checkpoint: TLCheckpoint? = null,
    val pin: TLPinGroup? = null
)

// ─────────────────────────────────────────────
// ADD EXPENSE SHEET
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseSheet(
    onDismiss: () -> Unit,
    onSave: (BankEntry) -> Unit,
    settings: BusinessSettings = BusinessSettings(),
    suppliers: List<Supplier> = emptyList(),
    stockItems: List<StockItem> = emptyList(),
    onAddDebt: ((SupplierDebt) -> Unit)? = null
) {
    // 0 = Purchase (COG), 1 = Expense, 2 = Income
    var logType by remember { mutableIntStateOf(1) }

    // ── Shared fields ──────────────────────────────────────────────
    var description     by remember { mutableStateOf("") }
    var amount          by remember { mutableStateOf("") }
    var selectedAccount by remember { mutableStateOf(FinancialAccount.MAYBANK) }
    var selectedSub     by remember { mutableStateOf("") }
    var selectedCat     by remember { mutableStateOf("Other") }

    // ── Purchase-specific fields ────────────────────────────────────
    var supplierQuery   by remember { mutableStateOf("") }
    var selectedSupplier by remember { mutableStateOf<Supplier?>(null) }
    var showSupplierDrop by remember { mutableStateOf(false) }
    var stockQuery      by remember { mutableStateOf("") }
    var selectedStock   by remember { mutableStateOf<StockItem?>(null) }
    var showStockDrop   by remember { mutableStateOf(false) }
    // Purchase account: MAYBANK, CASH, or DEBT
    var purchaseOnDebt  by remember { mutableStateOf(false) }

    val expenseCategories = listOf("Parking", "Toll", "Petrol", "Market Fee", "Labour", "Packaging", "Utilities", "Other")

    val filteredSuppliers = remember(suppliers, supplierQuery) {
        if (supplierQuery.isBlank()) suppliers
        else suppliers.filter { it.name.contains(supplierQuery, ignoreCase = true) }
    }
    val filteredStock = remember(stockItems, stockQuery) {
        if (stockQuery.isBlank()) stockItems.take(10)
        else stockItems.filter { it.name.contains(stockQuery, ignoreCase = true) }.take(10)
    }

    LaunchedEffect(selectedAccount) { selectedSub = "" }
    LaunchedEffect(logType) {
        description = ""; amount = ""; selectedCat = "Other"
        supplierQuery = ""; selectedSupplier = null; stockQuery = ""; selectedStock = null
    }

    val subOptions = settings.subAccountsFor(selectedAccount)

    ModalBottomSheet(onDismissRequest = onDismiss, modifier = Modifier.fillMaxHeight(0.90f)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Type tabs ──────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth()
                .background(PSRColors.Grey100).padding(4.dp)) {
                listOf("📦 Purchase", "💸 Expense", "💰 Income").forEachIndexed { idx, label ->
                    val sel = logType == idx
                    Box(modifier = Modifier.weight(1f)
                        .background(if (sel) Color.White else Color.Transparent, RoundedCornerShape(10.dp))
                        .clickable { logType = idx }.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center) {
                        Text(label, style = MaterialTheme.typography.labelLarge.copy(fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal),
                            color = when {
                                !sel -> PSRColors.Grey500
                                idx == 0 -> PSRColors.Navy600
                                idx == 1 -> PSRColors.Error
                                else -> PSRColors.Success
                            }, fontSize = 12.sp)
                    }
                }
            }

            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(bottom = 32.dp, top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // ── PURCHASE tab ───────────────────────────────────
                if (logType == 0) {
                    Text("Log Purchase (Cost of Goods)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

                    // Supplier picker
                    Text("Supplier", style = MaterialTheme.typography.labelMedium, color = PSRColors.Grey600)
                    ExposedDropdownMenuBox(expanded = showSupplierDrop, onExpandedChange = { showSupplierDrop = it }) {
                        OutlinedTextField(
                            value = selectedSupplier?.name ?: supplierQuery,
                            onValueChange = { supplierQuery = it; selectedSupplier = null; showSupplierDrop = true },
                            label = { Text("Search supplier…") },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp), singleLine = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(showSupplierDrop) }
                        )
                        ExposedDropdownMenu(expanded = showSupplierDrop && filteredSuppliers.isNotEmpty(),
                            onDismissRequest = { showSupplierDrop = false }) {
                            filteredSuppliers.forEach { sup ->
                                DropdownMenuItem(text = { Text(sup.name) }, onClick = {
                                    selectedSupplier = sup; supplierQuery = sup.name; showSupplierDrop = false
                                })
                            }
                        }
                    }

                    // Item picker
                    Text("Item bought", style = MaterialTheme.typography.labelMedium, color = PSRColors.Grey600)
                    ExposedDropdownMenuBox(expanded = showStockDrop, onExpandedChange = { showStockDrop = it }) {
                        OutlinedTextField(
                            value = selectedStock?.name ?: stockQuery,
                            onValueChange = { stockQuery = it; selectedStock = null; showStockDrop = true },
                            label = { Text("Search item from catalogue…") },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp), singleLine = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(showStockDrop) }
                        )
                        ExposedDropdownMenu(expanded = showStockDrop && filteredStock.isNotEmpty(),
                            onDismissRequest = { showStockDrop = false }) {
                            filteredStock.forEach { item ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(item.name, style = MaterialTheme.typography.bodyMedium)
                                            Text("Buy: RM ${"%.2f".format(item.defaultBuyPrice)} / ${item.unit}",
                                                style = MaterialTheme.typography.labelSmall, color = PSRColors.Grey500)
                                        }
                                    },
                                    onClick = {
                                        selectedStock = item
                                        stockQuery = item.name
                                        if (description.isBlank()) description = item.name
                                        if (amount.isBlank() && item.defaultBuyPrice > 0)
                                            amount = "%.2f".format(item.defaultBuyPrice)
                                        showStockDrop = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(value = description, onValueChange = { description = it },
                        label = { Text("Description / note") }, modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp), singleLine = true)

                    RMInputField("Total amount paid", amount, { amount = it }, Modifier.fillMaxWidth())

                    // Payment method: Bank, Cash, or Debt
                    Text("Paid from", style = MaterialTheme.typography.labelMedium, color = PSRColors.Grey600)
                    Row(modifier = Modifier.fillMaxWidth().background(PSRColors.Grey100, RoundedCornerShape(12.dp)).padding(4.dp)) {
                        listOf(false to settings.labelFor(FinancialAccount.MAYBANK),
                               null to settings.labelFor(FinancialAccount.CASH),
                               true to "Debt").forEach { (isDebt, label) ->
                            val sel = when (isDebt) {
                                true  -> purchaseOnDebt
                                false -> !purchaseOnDebt && selectedAccount == FinancialAccount.MAYBANK
                                else  -> !purchaseOnDebt && selectedAccount == FinancialAccount.CASH
                            }
                            Box(modifier = Modifier.weight(1f)
                                .background(if (sel) Color.White else Color.Transparent, RoundedCornerShape(10.dp))
                                .clickable {
                                    if (isDebt == true) { purchaseOnDebt = true }
                                    else { purchaseOnDebt = false; selectedAccount = if (isDebt == false) FinancialAccount.MAYBANK else FinancialAccount.CASH }
                                }.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center) {
                                Text(label, style = MaterialTheme.typography.labelLarge,
                                    color = if (sel) (if (isDebt == true) PSRColors.Warning else PSRColors.Navy600) else PSRColors.Grey500)
                            }
                        }
                    }
                    if (purchaseOnDebt) {
                        Surface(color = PSRColors.Warning.copy(0.08f), shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, PSRColors.Warning.copy(0.3f))) {
                            Text("This will be added as a debt in the Debt Manager",
                                style = MaterialTheme.typography.labelSmall, color = PSRColors.Warning,
                                modifier = Modifier.padding(10.dp))
                        }
                    }

                    Button(
                        onClick = {
                            val amt = amount.toDoubleOrNull() ?: return@Button
                            val desc = buildString {
                                if (selectedSupplier != null) append("[${selectedSupplier!!.name}] ")
                                else if (supplierQuery.isNotBlank()) append("[$supplierQuery] ")
                                append(if (description.isNotBlank()) description else selectedStock?.name ?: "Purchase")
                            }
                            if (purchaseOnDebt) {
                                onAddDebt?.invoke(SupplierDebt(
                                    supplierName = selectedSupplier?.name ?: supplierQuery.ifBlank { "Unknown" },
                                    description  = desc,
                                    totalAmount  = amt,
                                    amountPaid   = 0.0
                                ))
                            } else {
                                onSave(BankEntry(description = desc, amount = -amt,
                                    account = selectedAccount, category = "Stock Purchase", isManual = true))
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PSRColors.Navy600),
                        enabled = amount.toDoubleOrNull() != null && amount.toDoubleOrNull()!! > 0
                    ) {
                        Icon(Icons.Default.ShoppingCart, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (purchaseOnDebt) "Add to Debt Manager" else "Log Purchase", style = MaterialTheme.typography.titleSmall)
                    }
                }

                // ── EXPENSE tab ────────────────────────────────────
                if (logType == 1) {
                    Text("Log Expense", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

                    // Account + sub
                    Text("Account", style = MaterialTheme.typography.labelMedium, color = PSRColors.Grey600)
                    Row(modifier = Modifier.fillMaxWidth().background(PSRColors.Grey100, RoundedCornerShape(12.dp)).padding(4.dp)) {
                        FinancialAccount.entries.forEach { acc ->
                            val sel = selectedAccount == acc
                            Box(modifier = Modifier.weight(1f)
                                .background(if (sel) Color.White else Color.Transparent, RoundedCornerShape(10.dp))
                                .clickable { selectedAccount = acc }.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center) {
                                Text(settings.labelFor(acc), style = MaterialTheme.typography.labelLarge,
                                    color = if (sel) PSRColors.Navy600 else PSRColors.Grey600)
                            }
                        }
                    }
                    if (subOptions.isNotEmpty()) {
                        Text("Sub-account", style = MaterialTheme.typography.labelSmall, color = PSRColors.Grey500)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            subOptions.forEach { sub ->
                                val sel = selectedSub == sub
                                FilterChip(selected = sel, onClick = { selectedSub = if (sel) "" else sub },
                                    label = { Text(sub, style = MaterialTheme.typography.labelSmall) }, shape = RoundedCornerShape(8.dp),
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PSRColors.Navy600, selectedLabelColor = PSRColors.White))
                            }
                        }
                    }
                    OutlinedTextField(value = description, onValueChange = { description = it },
                        label = { Text("Description") }, modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp), singleLine = true)
                    RMInputField("Amount", amount, { amount = it }, Modifier.fillMaxWidth())
                    Text("Category", style = MaterialTheme.typography.labelMedium, color = PSRColors.Grey600)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(expenseCategories) { cat ->
                            FilterChip(selected = selectedCat == cat, onClick = { selectedCat = cat }, label = { Text(cat) }, shape = RoundedCornerShape(10.dp),
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PSRColors.Navy600, selectedLabelColor = PSRColors.White))
                        }
                    }
                    Button(
                        onClick = {
                            val amt = amount.toDoubleOrNull() ?: return@Button
                            val desc = buildString { if (selectedSub.isNotBlank()) append("[$selectedSub] "); append(description.ifBlank { selectedCat }) }
                            onSave(BankEntry(description = desc, amount = -amt, account = selectedAccount, category = selectedCat, isManual = true))
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PSRColors.Error),
                        enabled = amount.toDoubleOrNull() != null && amount.toDoubleOrNull()!! > 0
                    ) { Text("Log Expense", style = MaterialTheme.typography.titleSmall) }
                }

                // ── INCOME tab ─────────────────────────────────────
                if (logType == 2) {
                    Text("Log Income", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text("Account", style = MaterialTheme.typography.labelMedium, color = PSRColors.Grey600)
                    Row(modifier = Modifier.fillMaxWidth().background(PSRColors.Grey100, RoundedCornerShape(12.dp)).padding(4.dp)) {
                        FinancialAccount.entries.forEach { acc ->
                            val sel = selectedAccount == acc
                            Box(modifier = Modifier.weight(1f)
                                .background(if (sel) Color.White else Color.Transparent, RoundedCornerShape(10.dp))
                                .clickable { selectedAccount = acc }.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center) {
                                Text(settings.labelFor(acc), style = MaterialTheme.typography.labelLarge,
                                    color = if (sel) PSRColors.Success else PSRColors.Grey600)
                            }
                        }
                    }
                    OutlinedTextField(value = description, onValueChange = { description = it },
                        label = { Text("Description") }, modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp), singleLine = true)
                    RMInputField("Amount received", amount, { amount = it }, Modifier.fillMaxWidth())
                    Button(
                        onClick = {
                            val amt = amount.toDoubleOrNull() ?: return@Button
                            onSave(BankEntry(description = description.ifBlank { "Income" }, amount = amt,
                                account = selectedAccount, category = "Income", isManual = true))
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PSRColors.Success),
                        enabled = amount.toDoubleOrNull() != null && amount.toDoubleOrNull()!! > 0
                    ) { Text("Log Income", style = MaterialTheme.typography.titleSmall) }
                }
            }
        }
    }
}


// ─────────────────────────────────────────────
// PROFIT LOG SHEET
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfitLogSheet(
    invoices: List<Invoice>,
    todayProfit: Double,
    weekProfit: Double,
    monthProfit: Double,
    totalProfit: Double,
    onDismiss: () -> Unit
) {
    val sdfDay  = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val sdfTime = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    // Period tab: 0=Today, 1=Week, 2=Month, 3=All
    var selectedTab by remember { mutableStateOf(0) }

    val tabStartMs = remember(selectedTab) {
        when (selectedTab) {
            0 -> startOfToday()
            1 -> startOfWeek()
            2 -> startOfMonth()
            else -> 0L
        }
    }

    val tabLabel = when (selectedTab) { 0 -> "Today"; 1 -> "This Week"; 2 -> "This Month"; else -> "All Time" }
    val tabProfit = when (selectedTab) { 0 -> todayProfit; 1 -> weekProfit; 2 -> monthProfit; else -> totalProfit }

    val filteredInvoices = remember(invoices, selectedTab, tabStartMs) {
        invoices
            .filter { (it.status == InvoiceStatus.PAID || it.netProfit > 0) && it.issuedAt >= tabStartMs }
            .sortedByDescending { it.issuedAt }
    }

    val grouped = remember(filteredInvoices) {
        filteredInvoices.groupBy { sdfDay.format(Date(it.issuedAt)) }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White, modifier = Modifier.fillMaxHeight(0.92f)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Column(modifier = Modifier.fillMaxWidth()
                .background(Brush.verticalGradient(listOf(PSRColors.Navy600, PSRColors.Navy700)))
                .padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text("Profit Summary", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = PSRColors.White)
                Spacer(Modifier.height(12.dp))
                // Period summary cards
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Triple("Today",      todayProfit,  0),
                        Triple("This Week",  weekProfit,   1),
                        Triple("This Month", monthProfit,  2),
                        Triple("All Time",   totalProfit,  3)
                    ).forEach { (label, profit, idx) ->
                        val isSelected = selectedTab == idx
                        Surface(
                            onClick = { selectedTab = idx },
                            modifier = Modifier.weight(1f),
                            color = if (isSelected) PSRColors.White.copy(0.2f) else PSRColors.White.copy(0.08f),
                            shape = RoundedCornerShape(10.dp),
                            border = if (isSelected) BorderStroke(1.dp, PSRColors.White.copy(0.5f)) else null
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = PSRColors.White.copy(0.65f))
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "RM ${"%.0f".format(profit)}",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black, fontSize = 11.sp),
                                    color = if (profit >= 0) PSRColors.Success else PSRColors.Error,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // Current period detail header
            Row(modifier = Modifier.fillMaxWidth().background(PSRColors.Grey50).padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(tabLabel, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = PSRColors.Navy700)
                Column(horizontalAlignment = Alignment.End) {
                    Text("RM ${"%.2f".format(tabProfit)}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = if (tabProfit >= 0) PSRColors.Success else PSRColors.Error)
                    Text("${filteredInvoices.size} invoice${if (filteredInvoices.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.labelSmall, color = PSRColors.Grey500)
                }
            }

            if (filteredInvoices.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No paid invoices", style = MaterialTheme.typography.bodyMedium, color = PSRColors.Grey400)
                        if (selectedTab == 0) Text("Profit resets at midnight", style = MaterialTheme.typography.labelSmall, color = PSRColors.Grey300)
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
                    grouped.forEach { (dayLabel, dayInvoices) ->
                        val dayProfit = dayInvoices.sumOf { it.netProfit }
                        item(key = "h_$dayLabel") {
                            Row(modifier = Modifier.fillMaxWidth().background(PSRColors.Grey50).padding(horizontal = 20.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(dayLabel, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = PSRColors.Navy700)
                                Text("RM ${"%.2f".format(dayProfit)}",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = if (dayProfit >= 0) PSRColors.Success else PSRColors.Error)
                            }
                        }
                        items(dayInvoices, key = { it.id }) { inv ->
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(modifier = Modifier.size(8.dp).background(when (inv.status) {
                                    InvoiceStatus.PAID      -> PSRColors.Success
                                    InvoiceStatus.PARTIAL   -> PSRColors.Warning
                                    InvoiceStatus.UNPAID    -> PSRColors.Error
                                    InvoiceStatus.CANCELLED -> PSRColors.Grey300
                                }, CircleShape))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(inv.customerSnapshot.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = PSRColors.Navy900)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(inv.invoiceNumber, style = MaterialTheme.typography.labelSmall, color = PSRColors.Accent)
                                        Text(sdfTime.format(Date(inv.issuedAt)), style = MaterialTheme.typography.labelSmall, color = PSRColors.Grey400)
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("RM ${"%.2f".format(inv.totalAmount)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = PSRColors.Navy900)
                                    Text("profit RM ${"%.2f".format(inv.netProfit)}", style = MaterialTheme.typography.labelSmall,
                                        color = if (inv.netProfit >= 0) PSRColors.Success else PSRColors.Error)
                                }
                            }
                            HorizontalDivider(color = PSRColors.Divider, modifier = Modifier.padding(horizontal = 20.dp))
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// DEBT MANAGER SHEET
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtManagerSheet(viewModel: PSRViewModel, onDismiss: () -> Unit) {
    val activeDebts     by viewModel.activeDebts.collectAsState()
    val totalDebtOwed   by viewModel.totalDebtOwed.collectAsState()
    val activeDebtCount by viewModel.activeDebtCount.collectAsState()
    val suppliers       by viewModel.suppliers.collectAsState()
    var showAddDebtSheet by remember { mutableStateOf(false) }
    var payingDebt       by remember { mutableStateOf<SupplierDebt?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = PSRColors.Surface, modifier = Modifier.fillMaxHeight(0.9f)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxWidth()
                .background(Brush.verticalGradient(listOf(PSRColors.Navy600, PSRColors.Navy700)))
                .padding(horizontal = 24.dp, vertical = 20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Debt Manager", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                        Text("Money owed to suppliers", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.7f))
                    }
                    IconButton(onClick = { showAddDebtSheet = true },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = PSRColors.White.copy(0.15f))) {
                        Icon(Icons.Default.Add, null, tint = Color.White)
                    }
                }
                Spacer(Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Surface(modifier = Modifier.weight(1f), color = Color.White.copy(0.1f), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("TOTAL OWED", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.6f))
                            Text("RM ${"%.2f".format(totalDebtOwed)}", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black), color = PSRColors.Error)
                        }
                    }
                    Surface(modifier = Modifier.weight(1f), color = Color.White.copy(0.1f), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("ACTIVE DEBTS", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.6f))
                            Text("$activeDebtCount", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black), color = Color.White)
                        }
                    }
                }
            }
            if (activeDebts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.CheckCircle, null, modifier = Modifier.size(48.dp), tint = PSRColors.Success.copy(0.5f))
                        Spacer(Modifier.height(8.dp))
                        Text("No outstanding debts 🎉", color = PSRColors.Grey500)
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
                    items(activeDebts, key = { "debt_${it.id}" }) { debt ->
                        DebtRow(debt = debt, onPay = { payingDebt = debt }, onDelete = { viewModel.deleteDebt(debt) })
                    }
                }
            }
        }
    }
    if (showAddDebtSheet) {
        AddDebtSheet(suppliers = suppliers, onDismiss = { showAddDebtSheet = false },
            onSave = { debt, isNew ->
                if (isNew) viewModel.findOrCreateSupplier(debt.supplierName) {}
                viewModel.addDebt(debt); showAddDebtSheet = false
            })
    }
    payingDebt?.let { debt ->
        val debtSettings by viewModel.businessSettings.collectAsState()
        PayDebtSheet(debt = debt, onDismiss = { payingDebt = null },
            onPay = { amount, account -> viewModel.payDebt(debt, amount, account); payingDebt = null },
            settings = debtSettings)
    }
}

// ─────────────────────────────────────────────
// MONEY TIMELINE SHEET
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyTimelineSheet(entries: List<BankEntry>, onDismiss: () -> Unit) {
    val sdfDay  = remember { SimpleDateFormat("EEE, dd MMM", Locale.getDefault()) }
    val sdfTime = remember { SimpleDateFormat("h:mma", Locale.getDefault()) }
    val sdfKey  = remember { SimpleDateFormat("yyyyMMdd", Locale.getDefault()) }

    val processed = remember(entries) {
        var bal = 0.0
        entries.sortedBy { it.entryDate }.map { e -> bal += e.amount; TLEntry(e, bal) }
    }

    val dayBuckets = remember(processed) {
        processed.groupBy { sdfKey.format(Date(it.entry.entryDate)) }
            .entries.toList().sortedByDescending { it.key }
    }

    val timelineItems = remember(dayBuckets) {
        val items = mutableListOf<TLTimelineItem>()
        dayBuckets.forEach { entry ->
            val dayEntries = entry.value
            val dayLabel   = sdfDay.format(Date(dayEntries.first().entry.entryDate))
            val finalBal   = dayEntries.last().runningBalance
            items.add(TLTimelineItem(isCheckpoint = true, checkpoint = TLCheckpoint(finalBal, dayLabel)))

            val dayPins = mutableListOf<TLPinGroup>()
            fun groupByType(isIncome: Boolean) {
                val list = dayEntries.filter { if (isIncome) it.entry.amount >= 0 else it.entry.amount < 0 }
                    .sortedBy { it.entry.entryDate }
                var i = 0
                while (i < list.size) {
                    val group = mutableListOf(list[i])
                    var j = i + 1
                    while (j < list.size && list[j].entry.entryDate - list[i].entry.entryDate < 45 * 60 * 1000L) {
                        group.add(list[j]); j++
                    }
                    dayPins.add(TLPinGroup(group, isIncome,
                        group.sumOf { kotlin.math.abs(it.entry.amount) },
                        group.first().entry.entryDate, group.last().entry.entryDate))
                    i = j
                }
            }
            groupByType(true); groupByType(false)
            dayPins.sortedByDescending { it.earliestTime }.forEach { pin ->
                items.add(TLTimelineItem(pin = pin))
            }
        }
        items
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = PSRColors.Navy900,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(0.3f)) },
        modifier = Modifier.fillMaxHeight(0.96f)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text("Money Timeline",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp),
                color = Color.White,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp))

            if (timelineItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No transactions yet", color = Color.White.copy(0.5f))
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) {
                    itemsIndexed(timelineItems) { _, item ->
                        if (item.isCheckpoint) {
                            item.checkpoint?.let { TimelineCheckpointCircle(it) }
                        } else {
                            item.pin?.let { TimelineTransactionPin(it, sdfTime) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineCheckpointCircle(checkpoint: TLCheckpoint) {
    Box(modifier = Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
        SpineLine(modifier = Modifier.fillMaxHeight())
        Surface(
            shape = CircleShape,
            color = Color.White,
            modifier = Modifier.size(115.dp),
            shadowElevation = 12.dp,
            border = BorderStroke(2.dp, PSRColors.Accent.copy(0.3f))
        ) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(checkpoint.label.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 9.sp),
                    color = PSRColors.Grey500)
                Spacer(Modifier.height(2.dp))
                Text("NET BALANCE",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold),
                    color = PSRColors.Accent)
                Text("%.2f".format(checkpoint.balance),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = 16.sp),
                    color = if (checkpoint.balance >= 0) PSRColors.Navy900 else PSRColors.Error)
                Text("RM",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                    color = PSRColors.Navy900.copy(0.4f))
            }
        }
    }
}

@Composable
private fun SpineLine(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "spinePulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes { durationMillis = 5000; 0f at 0; 0f at 4000; 1f at 4500; 0f at 5000 },
            repeatMode = RepeatMode.Restart
        ), label = "alpha"
    )
    Canvas(modifier = modifier.width(2.dp)) {
        val dash = 6.dp.toPx(); val gap = 4.dp.toPx()
        var y = 0f
        while (y < size.height) {
            val end = minOf(y + dash, size.height)
            drawLine(Color.White.copy(0.15f), Offset(size.width / 2, y), Offset(size.width / 2, end), strokeWidth = 2.dp.toPx())
            drawLine(PSRColors.Accent.copy(alpha = pulseAlpha * 0.6f), Offset(size.width / 2, y), Offset(size.width / 2, end), strokeWidth = 4.dp.toPx())
            y += dash + gap
        }
    }
}

@Composable
private fun TimelineTransactionPin(pin: TLPinGroup, sdfTime: SimpleDateFormat) {
    val isIncome  = pin.isIncome
    val color     = if (isIncome) PSRColors.Success else PSRColors.Error
    val amountStr = (if (isIncome) "+" else "-") + "RM ${"%.2f".format(pin.totalAmount)}"
    val timeStr   = sdfTime.format(Date(pin.earliestTime)).lowercase()
    val acctStr   = pin.entries.first().entry.account.name
    var expanded  by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth().animateContentSize().padding(vertical = 4.dp)) {
        SpineLine(modifier = Modifier.matchParentSize().align(Alignment.Center))
        Row(
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side — expense
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                if (!isIncome) PinContent(amountStr, timeStr, acctStr, pin, color, Alignment.End, expanded)
            }
            // Centre marker + arrows
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!isIncome) PinArrow(color, pointRight = true) else Spacer(Modifier.width(36.dp))
                Box(modifier = Modifier.size(18.dp).background(color, RoundedCornerShape(4.dp)).border(2.dp, Color.White.copy(0.5f), RoundedCornerShape(4.dp)))
                if (isIncome) PinArrow(color, pointRight = false) else Spacer(Modifier.width(36.dp))
            }
            // Right side — income
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (isIncome) PinContent(amountStr, timeStr, acctStr, pin, color, Alignment.Start, expanded)
            }
        }
    }
}

@Composable
private fun PinArrow(color: Color, pointRight: Boolean) {
    Canvas(modifier = Modifier.size(36.dp, 12.dp)) {
        val midY = size.height / 2
        drawLine(color.copy(0.4f), Offset(0f, midY), Offset(size.width, midY), strokeWidth = 2.dp.toPx())
        val h = 6.dp.toPx()
        val path = Path().apply {
            if (pointRight) { moveTo(size.width, midY); lineTo(size.width - h, midY - h / 1.5f); lineTo(size.width - h, midY + h / 1.5f) }
            else            { moveTo(0f, midY);          lineTo(h, midY - h / 1.5f);              lineTo(h, midY + h / 1.5f) }
            close()
        }
        drawPath(path, color.copy(0.4f))
    }
}

@Composable
private fun PinContent(amount: String, time: String, account: String, pin: TLPinGroup, color: Color, alignment: Alignment.Horizontal, expanded: Boolean) {
    val sdfExact = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    // Derive a human label from category
    fun transactionLabel(entry: BankEntry): String = when {
        entry.category == "Stock Purchase"       -> "Purchase"
        entry.category == "Adjustment"           -> "Adjustment"
        entry.category == "Online Sale"          -> "Online Sale"
        entry.invoiceId != null                  -> "Invoice"
        entry.amount >= 0                        -> "Income"
        else                                     -> "Expense"
    }

    Column(modifier = Modifier.padding(horizontal = 12.dp), horizontalAlignment = alignment) {
        // Header: total amount + time + account
        Text(
            text = "$amount  $time  ${account.uppercase()}",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
            color = color
        )
        // Sub-label when collapsed: show count
        if (!expanded && pin.entries.size > 1) {
            Text("${pin.entries.size} transactions",
                style = MaterialTheme.typography.labelSmall, color = color.copy(0.7f))
        }
        AnimatedVisibility(visible = expanded) {
            Column(horizontalAlignment = alignment, modifier = Modifier.padding(top = 6.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)) {
                pin.entries.toList().forEach { te: TLEntry ->
                    val t     = sdfExact.format(Date(te.entry.entryDate))
                    val label = transactionLabel(te.entry)
                    val desc  = te.entry.description
                    val amt   = "${if (te.entry.amount >= 0) "+" else ""}RM ${"%.2f".format(te.entry.amount)}"
                    Surface(color = Color.White.copy(0.08f), shape = RoundedCornerShape(6.dp)) {
                        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            horizontalAlignment = alignment) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                // Type badge
                                Box(modifier = Modifier
                                    .background(Color.White.copy(0.15f), RoundedCornerShape(3.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)) {
                                    Text(label, style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 8.sp, fontWeight = FontWeight.Bold),
                                        color = Color.White.copy(0.9f))
                                }
                                Text(amt, style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                    color = Color.White)
                                Text(t, style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                    color = Color.White.copy(0.6f))
                            }
                            if (desc.isNotBlank()) {
                                Text(desc, style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                    color = Color.White.copy(0.75f), maxLines = 1,
                                    overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// INTER-TRANSFER SHEET
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterTransferSheet(
    settings: BusinessSettings,
    onDismiss: () -> Unit,
    onTransfer: (from: FinancialAccount, to: FinancialAccount, amount: Double) -> Unit
) {
    var fromAccount  by remember { mutableStateOf(FinancialAccount.MAYBANK) }
    var toAccount    by remember { mutableStateOf(FinancialAccount.CASH) }
    var amountStr    by remember { mutableStateOf("") }
    val amount = amountStr.toDoubleOrNull() ?: 0.0

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White,
        modifier = Modifier.fillMaxHeight(0.55f)) {
        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Inter-Account Transfer",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
            Text("Move money between your accounts. Net total stays the same.",
                style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey500)

            // From / To row
            Row(modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("From", style = MaterialTheme.typography.labelSmall, color = PSRColors.Grey500)
                    Spacer(Modifier.height(4.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        FinancialAccount.entries.forEach { acc ->
                            val sel = fromAccount == acc
                            Surface(onClick = { fromAccount = acc; if (toAccount == acc) toAccount = FinancialAccount.entries.first { it != acc } },
                                shape = RoundedCornerShape(10.dp),
                                color = if (sel) PSRColors.Error.copy(0.1f) else PSRColors.Grey50,
                                border = BorderStroke(if (sel) 2.dp else 1.dp, if (sel) PSRColors.Error.copy(0.5f) else PSRColors.Grey200),
                                modifier = Modifier.fillMaxWidth()) {
                                Text(settings.labelFor(acc), style = MaterialTheme.typography.labelLarge,
                                    color = if (sel) PSRColors.Error else PSRColors.Grey600,
                                    modifier = Modifier.padding(10.dp))
                            }
                        }
                    }
                }
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = PSRColors.Navy600, modifier = Modifier.size(24.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("To", style = MaterialTheme.typography.labelSmall, color = PSRColors.Grey500)
                    Spacer(Modifier.height(4.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        FinancialAccount.entries.forEach { acc ->
                            val sel = toAccount == acc
                            val disabled = acc == fromAccount
                            Surface(onClick = { if (!disabled) toAccount = acc },
                                shape = RoundedCornerShape(10.dp),
                                color = when { disabled -> PSRColors.Grey50; sel -> PSRColors.Success.copy(0.1f); else -> PSRColors.Grey50 },
                                border = BorderStroke(if (sel) 2.dp else 1.dp, when { disabled -> PSRColors.Grey100; sel -> PSRColors.Success.copy(0.5f); else -> PSRColors.Grey200 }),
                                modifier = Modifier.fillMaxWidth()) {
                                Text(settings.labelFor(acc), style = MaterialTheme.typography.labelLarge,
                                    color = when { disabled -> PSRColors.Grey300; sel -> PSRColors.Success; else -> PSRColors.Grey600 },
                                    modifier = Modifier.padding(10.dp))
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = amountStr,
                onValueChange = { amountStr = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Amount (RM)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp), singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                prefix = { Text("RM ") }
            )

            Button(
                onClick = { onTransfer(fromAccount, toAccount, amount) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = amount > 0 && fromAccount != toAccount,
                colors = ButtonDefaults.buttonColors(containerColor = PSRColors.Navy600)
            ) {
                Icon(Icons.Default.SwapHoriz, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Transfer RM ${"%.2f".format(amount)}", style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

// ─────────────────────────────────────────────
// ADJUSTMENT SHEET
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdjustmentSheet(
    account: FinancialAccount,
    currentBalance: Double,
    accountLabel: String,
    onDismiss: () -> Unit,
    onSave: (BankEntry) -> Unit
) {
    var targetBalanceStr by remember { mutableStateOf("%.2f".format(currentBalance)) }
    var reason           by remember { mutableStateOf("") }
    val target = targetBalanceStr.toDoubleOrNull() ?: currentBalance
    val diff   = target - currentBalance
    val isGain = diff >= 0

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White,
        modifier = Modifier.fillMaxHeight(0.6f)) {
        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Adjust $accountLabel Balance",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
            Text("Use this to correct your balance if you notice a discrepancy. The difference is recorded as an Adjustment entry — not counted as profit.",
                style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey500)

            // Current balance display
            Surface(color = PSRColors.Grey50, shape = RoundedCornerShape(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("Current balance", style = MaterialTheme.typography.bodyMedium, color = PSRColors.Grey600)
                    Text("RM ${"%.2f".format(currentBalance)}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = PSRColors.Navy900)
                }
            }

            // Target balance input
            OutlinedTextField(
                value = targetBalanceStr,
                onValueChange = { targetBalanceStr = it },
                label = { Text("Set correct balance (RM)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp), singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                prefix = { Text("RM ") }
            )

            // Diff preview
            if (targetBalanceStr.isNotBlank() && diff != 0.0) {
                Surface(
                    color = if (isGain) PSRColors.Success.copy(0.08f) else PSRColors.Error.copy(0.08f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (isGain) PSRColors.Success.copy(0.3f) else PSRColors.Error.copy(0.3f))
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("Adjustment", style = MaterialTheme.typography.labelLarge, color = PSRColors.Grey600)
                        Text("${if (isGain) "+" else ""}RM ${"%.2f".format(diff)}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isGain) PSRColors.Success else PSRColors.Error)
                    }
                }
            }

            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text("Reason (optional)") },
                placeholder = { Text("e.g. Cash count, found missing entry") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp), singleLine = true
            )

            Button(
                onClick = {
                    val t = targetBalanceStr.toDoubleOrNull() ?: return@Button
                    val d = t - currentBalance
                    if (d == 0.0) { onDismiss(); return@Button }
                    onSave(BankEntry(
                        description = "Balance Adjustment${if (reason.isNotBlank()) ": $reason" else ""}",
                        amount      = d,
                        account     = account,
                        category    = "Adjustment",
                        isManual    = true
                    ))
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = targetBalanceStr.toDoubleOrNull() != null && targetBalanceStr.toDoubleOrNull() != currentBalance,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (diff >= 0) PSRColors.Success else PSRColors.Warning)
            ) {
                Icon(Icons.Default.Tune, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Apply Adjustment", style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}


// ─────────────────────────────────────────────
// HELPER COMPONENTS
// ─────────────────────────────────────────────

// ─────────────────────────────────────────────
// DAILY SUMMARY CARD + SHEET
// ─────────────────────────────────────────────

@Composable
fun DailySummaryCard(
    invoices: List<Invoice>,
    bankEntries: List<BankEntry>,
    customers: List<Customer>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val sdfDay = remember { SimpleDateFormat("yyyyMMdd", Locale.getDefault()) }
    val yesterday = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        sdfDay.format(cal.time)
    }
    val todayKey = remember { sdfDay.format(Date()) }

    // Yesterday's stats
    val ydInvoices = remember(invoices) {
        invoices.filter { sdfDay.format(Date(it.issuedAt)) == yesterday }
    }
    val ydSales   = remember(ydInvoices) { ydInvoices.sumOf { it.totalAmount } }
    val ydProfit  = remember(ydInvoices) { ydInvoices.filter { it.status == InvoiceStatus.PAID }.sumOf { it.netProfit } }
    val ydCount   = ydInvoices.size

    // Today's so far
    val todayInvoices = remember(invoices) {
        invoices.filter { sdfDay.format(Date(it.issuedAt)) == todayKey }
    }
    val todaySales = remember(todayInvoices) { todayInvoices.sumOf { it.totalAmount } }

    // Outstanding customers count
    val outstandingCount = remember(invoices) {
        invoices.filter { it.status != InvoiceStatus.PAID && it.status != InvoiceStatus.CANCELLED }
            .map { it.customerSnapshot.name }.distinct().size
    }

    val hasYesterdayData = ydCount > 0
    val label = if (hasYesterdayData) "Yesterday: RM ${"%.0f".format(ydSales)}" else "No sales yesterday"
    val todayOnlineSalesCard = remember(bankEntries, todayKey) {
        bankEntries.filter { sdfDay.format(Date(it.entryDate)) == todayKey && it.amount > 0 &&
            it.category == "Online Sale" }.sumOf { it.amount }
    }
    val sublabel = when {
        todaySales > 0 && todayOnlineSalesCard > 0 -> "Today: RM ${"%.0f".format(todaySales + todayOnlineSalesCard)} (incl. online)"
        todaySales > 0 -> "Today so far: RM ${"%.0f".format(todaySales)}"
        todayOnlineSalesCard > 0 -> "Today online: RM ${"%.0f".format(todayOnlineSalesCard)}"
        else -> "Today: nothing yet"
    }

    // Reuse StatCard visual but with custom content feel
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val scale by animateFloatAsState(targetValue = if (visible) 1f else 0.85f,
        animationSpec = androidx.compose.animation.core.tween(400), label = "ds_scale")
    val alpha by animateFloatAsState(targetValue = if (visible) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(500), label = "ds_alpha")

    Card(
        onClick = onClick,
        modifier = modifier.scale(scale).alpha(alpha),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PSRColors.Card),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).background(PSRColors.Accent.copy(0.12f), CircleShape),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Assessment, null, tint = PSRColors.Accent, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.weight(1f))
                // "tap for more" hint bottom-right
                Icon(Icons.Default.OpenInNew, null, tint = PSRColors.Grey300, modifier = Modifier.size(13.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                label,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = if (hasYesterdayData) PSRColors.Navy900 else PSRColors.Grey400,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text("Daily Summary", style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey600)
            Text(sublabel, style = MaterialTheme.typography.labelSmall, color = PSRColors.Grey400)
            if (outstandingCount > 0) {
                Text("$outstandingCount customer${if (outstandingCount != 1) "s" else ""} owe",
                    style = MaterialTheme.typography.labelSmall, color = PSRColors.Warning)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailySummarySheet(
    invoices: List<Invoice>,
    bankEntries: List<BankEntry>,
    customers: List<Customer>,
    onDismiss: () -> Unit
) {
    val sdfDay  = remember { SimpleDateFormat("yyyyMMdd", Locale.getDefault()) }
    val sdfDisp = remember { SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()) }
    val sdfTime = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    // ── Date keys ──────────────────────────────────────────────────────
    val todayCal = remember { Calendar.getInstance() }
    val ydCal    = remember { Calendar.getInstance().also { it.add(Calendar.DAY_OF_YEAR, -1) } }
    val todayKey = remember { sdfDay.format(todayCal.time) }
    val ydKey    = remember { sdfDay.format(ydCal.time) }

    // ── Yesterday ──────────────────────────────────────────────────────
    val ydInvoices = remember(invoices) {
        invoices.filter { sdfDay.format(Date(it.issuedAt)) == ydKey }.sortedByDescending { it.issuedAt }
    }
    val ydSales     = remember(ydInvoices) { ydInvoices.sumOf { it.totalAmount } }
    val ydCollected = remember(ydInvoices) { ydInvoices.filter { it.status == InvoiceStatus.PAID }.sumOf { it.amountPaid } }
    val ydProfit    = remember(ydInvoices) { ydInvoices.filter { it.status == InvoiceStatus.PAID }.sumOf { it.netProfit } }
    val ydOnlineSales = remember(bankEntries) {
        bankEntries.filter { sdfDay.format(Date(it.entryDate)) == ydKey && it.amount > 0 &&
            it.category == "Online Sale" }.sumOf { it.amount }
    }
    val ydStockCost = remember(bankEntries) {
        bankEntries.filter { sdfDay.format(Date(it.entryDate)) == ydKey && it.amount < 0 &&
            it.category.contains("Stock", ignoreCase = true) }.sumOf { kotlin.math.abs(it.amount) }
    }
    val ydExpenses  = remember(bankEntries) {
        bankEntries.filter { sdfDay.format(Date(it.entryDate)) == ydKey && it.amount < 0 &&
            !it.category.contains("Stock", ignoreCase = true) }.sumOf { kotlin.math.abs(it.amount) }
    }

    // ── Today so far ───────────────────────────────────────────────────
    val todayInvoices = remember(invoices) {
        invoices.filter { sdfDay.format(Date(it.issuedAt)) == todayKey }.sortedByDescending { it.issuedAt }
    }
    val todayOnlineSales = remember(bankEntries) {
        bankEntries.filter { sdfDay.format(Date(it.entryDate)) == todayKey && it.amount > 0 &&
            it.category == "Online Sale" }.sumOf { it.amount }
    }
    val todaySales     = remember(todayInvoices) { todayInvoices.sumOf { it.totalAmount } }
    val todayCollected = remember(todayInvoices) { todayInvoices.filter { it.status == InvoiceStatus.PAID }.sumOf { it.amountPaid } }
    val todayProfit    = remember(todayInvoices) { todayInvoices.filter { it.status == InvoiceStatus.PAID }.sumOf { it.netProfit } }

    // ── Outstanding customers ──────────────────────────────────────────
    val outstanding = remember(invoices, customers) {
        invoices
            .filter { it.status != InvoiceStatus.PAID && it.status != InvoiceStatus.CANCELLED }
            .groupBy { it.customerSnapshot.name }
            .map { (name, invList) -> name to invList.sumOf { it.totalAmount - it.amountPaid } }
            .sortedByDescending { it.second }
    }

    // ── WhatsApp summary text ─────────────────────────────────────────
    val summaryText = remember(ydInvoices, ydSales, ydOnlineSales, ydCollected, ydProfit, outstanding) {
        buildString {
            appendLine("📊 *Daily Summary — ${sdfDisp.format(ydCal.time)}*")
            appendLine("─────────────────────")
            appendLine("🧾 Invoice sales: RM ${"%.2f".format(ydSales)} (${ydInvoices.size} invoices)")
            if (ydOnlineSales > 0) appendLine("🛒 Online sales: RM ${"%.2f".format(ydOnlineSales)}")
            appendLine("💰 Collected: RM ${"%.2f".format(ydCollected + ydOnlineSales)}")
            if (ydStockCost > 0) appendLine("📦 Stock cost: RM ${"%.2f".format(ydStockCost)}")
            if (ydExpenses > 0)  appendLine("🔧 Expenses: RM ${"%.2f".format(ydExpenses)}")
            appendLine("📈 Net profit: RM ${"%.2f".format(ydProfit)}")
            if (outstanding.isNotEmpty()) {
                appendLine("─────────────────────")
                appendLine("⚠️ Outstanding:")
                outstanding.forEach { (name, amt) -> appendLine("  • $name: RM ${"%.2f".format(amt)}") }
            }
            appendLine("─────────────────────")
            append("_Sent from PSRmart_")
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = PSRColors.Surface,
        modifier         = Modifier.fillMaxHeight(0.92f)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Header ────────────────────────────────────────────────
            Column(modifier = Modifier.fillMaxWidth()
                .background(Brush.verticalGradient(listOf(PSRColors.Navy600, PSRColors.Navy800)))
                .padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text("Daily Summary", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = PSRColors.White)
                Text(sdfDisp.format(ydCal.time), style = MaterialTheme.typography.bodySmall, color = PSRColors.White.copy(0.6f))
            }

            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // ── Today so far banner ───────────────────────────────
                if (todaySales > 0 || todayInvoices.isNotEmpty() || todayOnlineSales > 0) {
                    item {
                        Surface(color = PSRColors.Accent.copy(0.08f), shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, PSRColors.Accent.copy(0.2f))) {
                            Row(modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Default.WbSunny, null, tint = PSRColors.Accent, modifier = Modifier.size(20.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Today so far", style = MaterialTheme.typography.labelMedium, color = PSRColors.Accent)
                                    if (todaySales > 0)
                                        Text("RM ${"%.2f".format(todaySales)} · ${todayInvoices.size} invoice${if (todayInvoices.size != 1) "s" else ""}",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = PSRColors.Navy900)
                                    if (todayOnlineSales > 0)
                                        Text("🛒 Online: RM ${"%.2f".format(todayOnlineSales)}",
                                            style = MaterialTheme.typography.bodySmall, color = PSRColors.Navy600)
                                }
                                if (todayProfit > 0) {
                                    Text("+RM ${"%.0f".format(todayProfit)} profit",
                                        style = MaterialTheme.typography.labelSmall, color = PSRColors.Success)
                                }
                            }
                        }
                    }
                }

                // ── Yesterday numbers ─────────────────────────────────
                item {
                    Text("Yesterday", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = PSRColors.Navy700)
                }
                item {
                    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = PSRColors.Card)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SummaryRow("Invoice Sales", "RM ${"%.2f".format(ydSales)}", "${ydInvoices.size} invoices", PSRColors.Navy900)
                            if (ydOnlineSales > 0)
                                SummaryRow("🛒 Online Sales", "RM ${"%.2f".format(ydOnlineSales)}", "Shopee / TikTok settled", PSRColors.Navy600)
                            SummaryRow("Total Collected", "RM ${"%.2f".format(ydCollected + ydOnlineSales)}", null, PSRColors.Success)
                            if (ydStockCost > 0) SummaryRow("Stock Cost", "−RM ${"%.2f".format(ydStockCost)}", null, PSRColors.Error)
                            if (ydExpenses > 0)  SummaryRow("Other Expenses", "−RM ${"%.2f".format(ydExpenses)}", null, PSRColors.Error)
                            HorizontalDivider(color = PSRColors.Divider)
                            SummaryRow("Net Profit", "RM ${"%.2f".format(ydProfit)}", null,
                                if (ydProfit >= 0) PSRColors.Success else PSRColors.Error, isBold = true)
                        }
                    }
                }

                // ── Yesterday invoices ────────────────────────────────
                if (ydInvoices.isNotEmpty()) {
                    item {
                        Text("Yesterday's invoices", style = MaterialTheme.typography.labelLarge, color = PSRColors.Grey600,
                            modifier = Modifier.padding(top = 4.dp))
                    }
                    items(ydInvoices, key = { "yd_${it.id}" }) { inv ->
                        Row(modifier = Modifier.fillMaxWidth().background(PSRColors.Card, RoundedCornerShape(10.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(modifier = Modifier.size(8.dp).background(
                                when (inv.status) {
                                    InvoiceStatus.PAID -> PSRColors.Success
                                    InvoiceStatus.PARTIAL -> PSRColors.Warning
                                    else -> PSRColors.Error
                                }, CircleShape))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(inv.customerSnapshot.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = PSRColors.Navy900)
                                Text("${inv.invoiceNumber} · ${sdfTime.format(Date(inv.issuedAt))}",
                                    style = MaterialTheme.typography.labelSmall, color = PSRColors.Grey400)
                            }
                            Text("RM ${"%.2f".format(inv.totalAmount)}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = PSRColors.Navy900)
                        }
                    }
                }

                // ── Outstanding customers ─────────────────────────────
                if (outstanding.isNotEmpty()) {
                    item {
                        Text("Outstanding", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = PSRColors.Navy700, modifier = Modifier.padding(top = 4.dp))
                    }
                    items(outstanding, key = { "out_${it.first}" }) { (name, amt) ->
                        Row(modifier = Modifier.fillMaxWidth().background(PSRColors.Warning.copy(0.06f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.AccountCircle, null, tint = PSRColors.Warning, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(name, modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = PSRColors.Navy900)
                            Text("owes RM ${"%.2f".format(amt)}",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = PSRColors.Warning)
                        }
                    }
                }

                // ── WhatsApp share ────────────────────────────────────
                item {
                    val ctx = LocalContext.current
                    val clipboard = LocalClipboardManager.current
                    var copied by remember { mutableStateOf(false) }

                    Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = {
                                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(android.content.Intent.EXTRA_TEXT, summaryText)
                                }
                                ctx.startActivity(android.content.Intent.createChooser(intent, "Share Summary")
                                    .apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) })
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF25D366))
                        ) {
                            Icon(Icons.Default.Chat, null, tint = Color(0xFF25D366), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Share", color = Color(0xFF25D366), style = MaterialTheme.typography.labelLarge)
                        }

                        OutlinedButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(summaryText))
                                copied = true
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(if (copied) Icons.Default.Check else Icons.Default.ContentCopy, null,
                                tint = if (copied) PSRColors.Success else PSRColors.Grey500,
                                modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (copied) "Copied!" else "Copy text",
                                color = if (copied) PSRColors.Success else PSRColors.Grey600,
                                style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, sub: String?, valueColor: Color, isBold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey600)
            if (sub != null) Text(sub, style = MaterialTheme.typography.labelSmall, color = PSRColors.Grey400)
        }
        Text(value,
            style = if (isBold) MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black)
                    else MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = valueColor)
    }
}

@Composable
fun DebtRow(debt: SupplierDebt, onPay: () -> Unit, onDelete: () -> Unit) {
    val dateStr    = remember(debt.incurredAt) { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(debt.incurredAt)) }
    val dueDateStr = remember(debt.dueDate) { debt.dueDate?.let { SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(it)) } }
    var showDelete by remember { mutableStateOf(false) }
    val accentColor = when (debt.status) {
        DebtStatus.PAID    -> PSRColors.Success
        DebtStatus.PARTIAL -> PSRColors.Warning
        DebtStatus.UNPAID  -> if (debt.isOverdue) PSRColors.Error else PSRColors.Navy600
    }
    Row(modifier = Modifier.fillMaxWidth().background(PSRColors.Card).clickable { showDelete = !showDelete }.padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(modifier = Modifier.width(4.dp).height(44.dp).background(accentColor, RoundedCornerShape(2.dp)))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(debt.supplierName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = PSRColors.Navy900)
                if (debt.isOverdue) Badge("OVERDUE", PSRColors.Error)
                if (debt.status == DebtStatus.PARTIAL) Badge("PARTIAL", PSRColors.Warning)
            }
            if (debt.description.isNotBlank())
                Text(debt.description, style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey600, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(dateStr, style = MaterialTheme.typography.labelSmall, color = PSRColors.Grey500)
                if (dueDateStr != null) Text("Due: $dueDateStr", style = MaterialTheme.typography.labelSmall, color = if (debt.isOverdue) PSRColors.Error else PSRColors.Grey500)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("RM ${"%.2f".format(debt.outstanding)}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = accentColor)
            if (debt.amountPaid > 0 && debt.status != DebtStatus.PAID)
                Text("of RM ${"%.2f".format(debt.totalAmount)}", style = MaterialTheme.typography.labelSmall, color = PSRColors.Grey500)
            if (debt.status == DebtStatus.PAID)
                Text("Paid RM ${"%.2f".format(debt.totalAmount)}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = PSRColors.Success)
        }
        AnimatedVisibility(visible = showDelete) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onPay,    modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Payment,      null, tint = PSRColors.Success, modifier = Modifier.size(18.dp)) }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.DeleteOutline, null, tint = PSRColors.Error,   modifier = Modifier.size(18.dp)) }
            }
        }
        if (!showDelete) {
            IconButton(onClick = onPay, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Payment, null, tint = PSRColors.Success, modifier = Modifier.size(20.dp)) }
        }
    }
    HorizontalDivider(color = PSRColors.Divider, modifier = Modifier.padding(horizontal = 20.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDebtSheet(suppliers: List<Supplier>, onDismiss: () -> Unit, onSave: (SupplierDebt, Boolean) -> Unit) {
    var supplierName   by remember { mutableStateOf("") }
    var description    by remember { mutableStateOf("") }
    var amount         by remember { mutableStateOf("") }
    var notes          by remember { mutableStateOf("") }
    var hasDueDate     by remember { mutableStateOf(false) }
    var dueDate        by remember { mutableLongStateOf(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDropdown   by remember { mutableStateOf(false) }
    val filtered = remember(suppliers, supplierName) {
        if (supplierName.isBlank()) suppliers else suppliers.filter { it.name.contains(supplierName, ignoreCase = true) }
    }
    val isNew = supplierName.isNotBlank() && suppliers.none { it.name.equals(supplierName.trim(), ignoreCase = true) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp).verticalScroll(rememberScrollState())) {
            Text("Record Debt", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
            Spacer(Modifier.height(20.dp))
            ExposedDropdownMenuBox(expanded = showDropdown && (filtered.isNotEmpty() || isNew), onExpandedChange = { showDropdown = it }) {
                OutlinedTextField(value = supplierName, onValueChange = { supplierName = it; showDropdown = true },
                    label = { Text("Supplier") }, modifier = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                ExposedDropdownMenu(expanded = showDropdown && (filtered.isNotEmpty() || isNew), onDismissRequest = { showDropdown = false }) {
                    filtered.forEach { s -> DropdownMenuItem(text = { Text(s.name) }, onClick = { supplierName = s.name; showDropdown = false }) }
                    if (isNew) DropdownMenuItem(text = { Text("Add \"$supplierName\" as new", color = PSRColors.Accent) }, onClick = { showDropdown = false })
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Items taken") },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), minLines = 2, maxLines = 4,
                placeholder = { Text("e.g. Ayam 30pc 12/2\nIkan 15kg") })
            Spacer(Modifier.height(10.dp))
            RMInputField("Total Amount Owed", amount, { amount = it }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth().background(PSRColors.Grey50, RoundedCornerShape(12.dp)).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Due date", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                    if (hasDueDate) Text(remember(dueDate) { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(dueDate)) }, style = MaterialTheme.typography.bodySmall, color = PSRColors.Navy600)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (hasDueDate) OutlinedButton(onClick = { showDatePicker = true }, shape = RoundedCornerShape(8.dp)) { Text("Change", style = MaterialTheme.typography.labelSmall) }
                    Switch(checked = hasDueDate, onCheckedChange = { hasDueDate = it }, colors = SwitchDefaults.colors(checkedThumbColor = PSRColors.Accent, checkedTrackColor = PSRColors.AccentDim))
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(onClick = {
                val amt = amount.toDoubleOrNull() ?: return@Button
                if (supplierName.isNotBlank()) onSave(SupplierDebt(supplierName = supplierName.trim(), description = description.trim(), totalAmount = amt, dueDate = if (hasDueDate) dueDate else null, notes = notes.trim()), isNew)
            }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PSRColors.Navy600)) {
                Text("Record Debt", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
    if (showDatePicker) {
        val dps = rememberDatePickerState(initialSelectedDateMillis = dueDate)
        DatePickerDialog(onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { dps.selectedDateMillis?.let { dueDate = it }; showDatePicker = false }) { Text("OK") } }
        ) { DatePicker(state = dps) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayDebtSheet(debt: SupplierDebt, onDismiss: () -> Unit, onPay: (Double, FinancialAccount) -> Unit, settings: BusinessSettings = BusinessSettings()) {
    var amountInput     by remember { mutableStateOf("%.2f".format(debt.outstanding)) }
    var selectedAccount by remember { mutableStateOf(FinancialAccount.MAYBANK) }
    var selectedSub     by remember { mutableStateOf("") }
    LaunchedEffect(selectedAccount) { selectedSub = "" }
    val subOptions = settings.subAccountsFor(selectedAccount)

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Text("Pay Debt", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
            Text(debt.supplierName, style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey600)
            Spacer(Modifier.height(20.dp))
            RMInputField("Amount", amountInput, { amountInput = it })
            Spacer(Modifier.height(14.dp))
            Text("Pay from", style = MaterialTheme.typography.labelMedium, color = PSRColors.Grey600)
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth().background(PSRColors.Grey100, RoundedCornerShape(12.dp)).padding(4.dp)) {
                FinancialAccount.entries.forEach { acc ->
                    val sel = selectedAccount == acc
                    Box(modifier = Modifier.weight(1f).background(if (sel) Color.White else Color.Transparent, RoundedCornerShape(10.dp))
                        .clickable { selectedAccount = acc }.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                        Text(settings.labelFor(acc), color = if (sel) PSRColors.Navy600 else PSRColors.Grey600,
                            style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            if (subOptions.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("Sub-account", style = MaterialTheme.typography.labelSmall, color = PSRColors.Grey500)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    subOptions.forEach { sub ->
                        val sel = selectedSub == sub
                        FilterChip(selected = sel, onClick = { selectedSub = if (sel) "" else sub },
                            label = { Text(sub, style = MaterialTheme.typography.labelSmall) },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PSRColors.Navy600, selectedLabelColor = PSRColors.White))
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(onClick = {
                val amt = amountInput.toDoubleOrNull()?.coerceIn(0.01, debt.outstanding) ?: return@Button
                onPay(amt, selectedAccount)
            }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PSRColors.Success)) {
                Text("Confirm Payment")
            }
        }
    }
}

@Composable
fun Badge(label: String, color: Color) {
    Box(modifier = Modifier.background(color.copy(0.12f), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 1.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = color)
    }
}

@Composable
fun AccountBalanceCard(name: String, amount: Double, icon: androidx.compose.ui.graphics.vector.ImageVector, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) PSRColors.Accent else PSRColors.White.copy(alpha = 0.12f),
        border = if (isSelected) null else BorderStroke(1.dp, PSRColors.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(icon, null, tint = if (isSelected) PSRColors.White else PSRColors.White.copy(0.7f), modifier = Modifier.size(12.dp))
                Text(name, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = if (isSelected) PSRColors.White else PSRColors.White.copy(0.7f), maxLines = 1, softWrap = false)
            }
            Spacer(Modifier.height(4.dp))
            Text("RM %.2f".format(amount), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp), color = PSRColors.White, maxLines = 1, overflow = TextOverflow.Visible, softWrap = false)
        }
    }
}

@Composable
fun LedgerEntryRow(entry: BankEntry, onDelete: () -> Unit, onClick: () -> Unit = {}, settings: BusinessSettings = BusinessSettings()) {
    val dateStr = remember(entry.entryDate) { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(entry.entryDate)) }
    Row(modifier = Modifier.fillMaxWidth().background(PSRColors.Card)
        .clickable { if (entry.invoiceId != null) onClick() else onDelete() }
        .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        val isAdjustment = entry.category == "Adjustment"
        Box(modifier = Modifier.size(8.dp).background(
            when {
                isAdjustment    -> PSRColors.Warning
                entry.amount >= 0 -> PSRColors.Success
                else            -> PSRColors.Error
            }, CircleShape))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(entry.description, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = PSRColors.Navy900, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (isAdjustment) {
                    Surface(color = PSRColors.Warning.copy(0.12f), shape = RoundedCornerShape(4.dp)) {
                        Text("ADJ", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Black),
                            color = PSRColors.Warning, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("$dateStr • ${entry.category}", style = MaterialTheme.typography.labelSmall, color = PSRColors.Grey500, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Surface(color = PSRColors.Grey100, shape = RoundedCornerShape(4.dp)) {
                    Text(settings.labelFor(entry.account),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                        color = PSRColors.Grey600, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
        }
        Text("${if (entry.amount >= 0) "+" else ""}RM ${"%.2f".format(entry.amount)}",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
            color = when {
                isAdjustment    -> PSRColors.Warning
                entry.amount >= 0 -> PSRColors.Success
                else            -> PSRColors.Error
            })
    }
    HorizontalDivider(color = PSRColors.Divider, modifier = Modifier.padding(horizontal = 20.dp))
}
