package com.psrmart.app.ui.screens

import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.psrmart.app.data.model.*
import com.psrmart.app.ui.components.*
import com.psrmart.app.ui.theme.*
import com.psrmart.app.viewmodel.PSRViewModel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogueScreen(viewModel: PSRViewModel) {
    val categories  by viewModel.categories.collectAsState()
    val allStock    by viewModel.filteredStock.collectAsState()
    val searchQuery by viewModel.stockSearchQuery.collectAsState()
    val settings    by viewModel.businessSettings.collectAsState()
    val suppliers   by viewModel.suppliers.collectAsState()

    var selectedCategoryId    by remember { mutableStateOf<Long?>(null) }
    var showAddStockDialog    by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showSupplierManager   by remember { mutableStateOf(false) }
    var editingStock          by remember { mutableStateOf<StockItem?>(null) }
    var stockInItem           by remember { mutableStateOf<StockItem?>(null) }
    var showAveragePrice      by remember { mutableStateOf(false) }
    var showPriceExport       by remember { mutableStateOf(false) }
    var showBuyPrice          by remember { mutableStateOf(false) }
    var customerMode          by remember { mutableStateOf(false) } // hides buy price on cards
    var listMode              by remember { mutableStateOf(false) } // list vs grid toggle

    val displayedStock  = remember(allStock, selectedCategoryId) {
        if (selectedCategoryId == null) allStock else allStock.filter { it.categoryId == selectedCategoryId }
    }
    val totalStockValue = remember(allStock) { allStock.sumOf { it.stockQty * it.defaultBuyPrice } }
    val categoryMap     = remember(categories) { categories.associateBy { it.id } }

    Column(modifier = Modifier.fillMaxSize().background(PSRColors.Surface)) {
        // ── Header ────────────────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(PSRColors.Navy800, PSRColors.Navy600)))
            .padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Stock", style = MaterialTheme.typography.headlineSmall, color = PSRColors.White)
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "Stock Value",
                            style = MaterialTheme.typography.labelSmall,
                            color = PSRColors.White.copy(0.55f),
                            modifier = Modifier.paddingFromBaseline(bottom = 2.dp)
                        )
                        Text(
                            formatRM(totalStockValue),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = PSRColors.Gold
                        )
                    }
                }
                // Supplier manager button
                IconButton(onClick = { showSupplierManager = true }) {
                    Icon(Icons.Outlined.Business, "Suppliers", tint = PSRColors.White.copy(0.8f), modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = { showPriceExport = true }) {
                    Icon(Icons.Default.Share, "Export price list", tint = PSRColors.White.copy(0.8f), modifier = Modifier.size(20.dp))
                }
                // Customer mode — hides buy price from cards
                IconButton(onClick = { customerMode = !customerMode }) {
                    Icon(
                        if (customerMode) Icons.Default.VisibilityOff else Icons.Default.Person,
                        "Customer mode",
                        tint = if (customerMode) PSRColors.Gold else PSRColors.White.copy(0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = { showAveragePrice = !showAveragePrice }) {
                    Icon(if (showAveragePrice) Icons.Default.Analytics else Icons.Default.PriceCheck, null, tint = PSRColors.White.copy(0.8f), modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = { listMode = !listMode }) {
                    Icon(
                        if (listMode) Icons.Default.GridView else Icons.Default.ViewList,
                        "Toggle view",
                        tint = if (listMode) PSRColors.Gold else PSRColors.White.copy(0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Search Bar
        Box(modifier = Modifier.fillMaxWidth().background(PSRColors.Navy600).padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
            OutlinedTextField(
                value = searchQuery, onValueChange = viewModel::setStockQuery,
                placeholder = { Text("Search stock...", color = PSRColors.White.copy(0.5f), style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = PSRColors.White.copy(0.7f), modifier = Modifier.size(18.dp)) },
                trailingIcon = { if (searchQuery.isNotBlank()) IconButton(onClick = { viewModel.setStockQuery("") }) { Icon(Icons.Default.Clear, null, tint = PSRColors.White.copy(0.7f), modifier = Modifier.size(16.dp)) } },
                modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp), singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PSRColors.Accent, unfocusedBorderColor = PSRColors.White.copy(0.2f), cursorColor = PSRColors.Accent)
            )
        }

        // Category chips
        LazyRow(modifier = Modifier.fillMaxWidth().background(PSRColors.White).padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
            item { CategoryChip("All", "🔍", selectedCategoryId == null) { selectedCategoryId = null } }
            items(categories) { cat -> CategoryChip(cat.name, cat.emoji, selectedCategoryId == cat.id) { selectedCategoryId = if (selectedCategoryId == cat.id) null else cat.id } }
            item { FilterChip(false, { showAddCategoryDialog = true }, label = { Text("+ Category", fontSize = 12.sp) }, shape = RoundedCornerShape(8.dp),
                colors = FilterChipDefaults.filterChipColors(containerColor = PSRColors.Grey50, labelColor = PSRColors.Accent)) }
        }

        HorizontalDivider(color = PSRColors.Divider)

        // Subtle customer mode indicator — just a thin gold bar, no text banner
        if (customerMode) {
            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(PSRColors.Gold.copy(0.6f)))
        }

        if (displayedStock.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                EmptyState(Icons.Outlined.Inventory2, "No items yet", "Tap + to add your first stock item", "Add Item") { showAddStockDialog = true }
            }
        } else if (listMode) {
            LazyColumn(modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(0.dp, 0.dp, 0.dp, 80.dp)) {
                // Group by category — same as grid
                val grouped = displayedStock.groupBy { it.categoryId }
                    .toSortedMap(compareBy { categories.indexOfFirst { c -> c.id == it } })
                grouped.forEach { (catId, catItems) ->
                    val cat = categoryMap[catId]
                    if (cat != null) {
                        item(key = "cat_$catId") {
                            Row(modifier = Modifier.fillMaxWidth()
                                .background(PSRColors.Grey50)
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(cat.emoji, style = MaterialTheme.typography.labelLarge)
                                Text(cat.name,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = PSRColors.Navy700)
                                Text("${catItems.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PSRColors.Grey400)
                            }
                        }
                    }
                    items(catItems, key = { it.id }) { item ->
                        StockListRow(
                            item        = item,
                            customerMode = customerMode,
                            useAverage  = showAveragePrice,
                            onClick     = { editingStock = item }
                        )
                        HorizontalDivider(color = PSRColors.Divider,
                            modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        } else {
            LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(displayedStock, key = { it.id }) { item ->
                    StockCard(
                        item = item,
                        category = categoryMap[item.categoryId],
                        useAverage = showAveragePrice,
                        customerMode = customerMode,
                        onClick = { editingStock = item },
                        onStockIn = { stockInItem = item },
                        onSell = { viewModel.triggerNavigateToInvoicesPane() }
                    )
                }
                item(span = { GridItemSpan(2) }) { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    // ── Dialogs / Sheets ──────────────────────────────────────────────────
    if (showAddStockDialog || editingStock != null) {
        AddEditStockDialog(
            stock = editingStock, categories = categories, units = settings.customUnits,
            suppliers = suppliers,
            onDismiss = { showAddStockDialog = false; editingStock = null },
            onSave = { item ->
                if (editingStock != null) viewModel.updateStock(item) else viewModel.insertStock(item)
                showAddStockDialog = false; editingStock = null
            },
            onDelete = { item -> viewModel.deleteStock(item); editingStock = null },
            onAddCategory = { showAddCategoryDialog = true }
        )
    }
    if (showAddCategoryDialog) {
        AddCategoryDialog(onDismiss = { showAddCategoryDialog = false }, onSave = { viewModel.insertCategory(it); showAddCategoryDialog = false })
    }
    if (showSupplierManager) {
        SupplierManagerSheet(suppliers = suppliers, onDismiss = { showSupplierManager = false },
            onAdd = { viewModel.insertSupplier(it) }, onDelete = { viewModel.deleteSupplier(it) })
    }
    stockInItem?.let { item ->
        StockInSheet(item = item, suppliers = suppliers, onDismiss = { stockInItem = null },
            onConfirm = { qty, cost, sup, acc, notes ->
                // Auto-create supplier if it's a new name
                val isNew = sup.isNotBlank() && suppliers.none { it.name.equals(sup.trim(), ignoreCase = true) }
                if (isNew) viewModel.findOrCreateSupplier(sup) {}
                viewModel.recordStockIn(item, qty, cost, sup, acc, notes)
                stockInItem = null
            })
    }
    if (showPriceExport) {
        PriceExportSheet(
            allStock   = allStock,
            categories = categories,
            settings   = settings,
            onDismiss  = { showPriceExport = false }
        )
    }
}

// ─────────────────────────────────────────────
// STOCK CARD — with stock-in / sell quick actions
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockListRow(
    item: StockItem,
    customerMode: Boolean,
    useAverage: Boolean,
    onClick: () -> Unit
) {
    val sellPrice = if (useAverage && item.supplierPrices.isNotEmpty())
        item.averageBuyPrice * 1.2 else item.currentSellPrice
    val buyPrice  = if (useAverage) item.averageBuyPrice else item.defaultBuyPrice
    val isLow     = item.stockQty < 2

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Name + meta
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(item.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = PSRColors.Navy900)
                if (isLow && !customerMode)
                    Text("LOW",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp, fontWeight = FontWeight.Bold),
                        color = PSRColors.Error,
                        modifier = Modifier
                            .background(PSRColors.Error.copy(0.1f), RoundedCornerShape(3.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp))
            }
            if (!customerMode) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Qty: ${"%.1f".format(item.stockQty)} ${item.unit}",
                        style = MaterialTheme.typography.labelSmall, color = PSRColors.Grey500)
                    if (item.supplierPrices.isNotEmpty())
                        Text("Buy: RM ${"%.2f".format(buyPrice)}",
                            style = MaterialTheme.typography.labelSmall, color = PSRColors.Grey400)
                }
            } else if (item.customerSpec.isNotBlank()) {
                Text(item.customerSpec,
                    style = MaterialTheme.typography.labelSmall,
                    color = PSRColors.Grey500)
            }
        }

        // Price column
        Column(horizontalAlignment = Alignment.End) {
            Text("RM ${"%.2f".format(sellPrice)}",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = PSRColors.Navy600)
            Text("/ ${item.unit}",
                style = MaterialTheme.typography.labelSmall, color = PSRColors.Grey400)
        }

        // Profit margin — only in owner mode
        if (!customerMode && buyPrice > 0 && sellPrice > 0) {
            val margin = ((sellPrice - buyPrice) / sellPrice * 100).toInt()
            val marginColor = when {
                margin >= 20 -> PSRColors.Success
                margin >= 10 -> PSRColors.Warning
                else         -> PSRColors.Error
            }
            Column(horizontalAlignment = Alignment.End,
                modifier = Modifier.widthIn(min = 38.dp)) {
                Text("+$margin%",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = marginColor)
                Text("margin",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    color = PSRColors.Grey300)
            }
        }

        Icon(Icons.Default.ChevronRight, null,
            tint = PSRColors.Grey300, modifier = Modifier.size(14.dp))
    }
}

@Composable
fun StockCard(item: StockItem, category: Category?, useAverage: Boolean, customerMode: Boolean = false, onClick: () -> Unit, onStockIn: () -> Unit, onSell: () -> Unit) {
    val buyPriceToShow = if (useAverage) item.averageBuyPrice else item.defaultBuyPrice
    val isLowStock     = item.stockQty in 0.01..5.0

    Card(
        onClick = if (customerMode) {{}} else onClick,
        enabled = !customerMode,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = PSRColors.Card),
        elevation = CardDefaults.cardElevation(if (customerMode) 0.dp else 1.dp)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(100.dp).background(PSRColors.Grey50), contentAlignment = Alignment.Center) {
                if (item.imagePath != null) {
                    SubcomposeAsyncImage(
                        model = item.imagePath,
                        contentDescription = item.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    ) {
                        val state = painter.state
                        // Show placeholder while loading
                        if (state is AsyncImagePainter.State.Loading || state is AsyncImagePainter.State.Empty) {
                            Box(modifier = Modifier.fillMaxSize().background(PSRColors.Grey100), contentAlignment = Alignment.Center) {
                                Text(category?.emoji ?: "📦", style = MaterialTheme.typography.displayMedium)
                            }
                        }
                        // Fade in only when image is ready
                        if (state is AsyncImagePainter.State.Success) {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = true,
                                enter = androidx.compose.animation.fadeIn(
                                    animationSpec = androidx.compose.animation.core.tween(300)
                                )
                            ) {
                                SubcomposeAsyncImageContent()
                            }
                        }
                    }
                } else {
                    Text(category?.emoji ?: "📦", style = MaterialTheme.typography.displayMedium)
                }
                category?.let {
                    Box(modifier = Modifier.align(Alignment.TopStart).padding(6.dp)
                        .background(Color(android.graphics.Color.parseColor(it.colorHex)).copy(0.85f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)) {
                        Text(it.name, style = MaterialTheme.typography.labelSmall, color = PSRColors.White)
                    }
                }
                // LOW badge only visible to owner
                if (!customerMode && isLowStock) {
                    Box(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
                        .background(PSRColors.Warning.copy(0.9f), RoundedCornerShape(6.dp)).padding(horizontal = 5.dp, vertical = 2.dp)) {
                        Text("LOW", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = PSRColors.White)
                    }
                }
            }
            Column(modifier = Modifier.padding(10.dp)) {
                Text(item.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = PSRColors.Navy900, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))

                if (customerMode) {
                    // ── Customer view: sell price only, spec if available ─────
                    Text(
                        "RM ${"%.2f".format(item.currentSellPrice)} / ${item.unit}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = PSRColors.Navy600
                    )
                    // Spec block — only shown if filled in
                    if (item.customerSpec.isNotBlank()) {
                        Spacer(Modifier.height(5.dp))
                        Box(
                            modifier = Modifier.fillMaxWidth()
                                .background(PSRColors.Grey50, RoundedCornerShape(6.dp))
                                .padding(horizontal = 7.dp, vertical = 4.dp)
                        ) {
                            Text(
                                item.customerSpec,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal),
                                color = PSRColors.Grey600,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onSell,
                        modifier = Modifier.fillMaxWidth().height(32.dp),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, PSRColors.Accent.copy(0.5f))
                    ) {
                        Icon(Icons.Default.ReceiptLong, null, tint = PSRColors.Accent, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("Invoice", style = MaterialTheme.typography.labelSmall, color = PSRColors.Accent)
                    }
                } else {
                    // ── Owner view: sell + buy, stock qty, action buttons ─────
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Sell", style = MaterialTheme.typography.labelSmall, color = PSRColors.Grey500)
                            Text("RM ${"%.2f".format(item.currentSellPrice)}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = PSRColors.Navy600)
                        }
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            Text(if (useAverage) "Avg Buy" else "Buy", style = MaterialTheme.typography.labelSmall, color = PSRColors.Grey500)
                            Text("RM ${"%.2f".format(buyPriceToShow)}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = PSRColors.Grey600)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Stock: ${"%.1f".format(item.stockQty)} ${item.unit}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (item.stockQty <= 0) PSRColors.Error else if (isLowStock) PSRColors.Warning else PSRColors.Grey500
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = onStockIn, modifier = Modifier.weight(1f).height(32.dp),
                            contentPadding = PaddingValues(0.dp), shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, PSRColors.Success.copy(0.5f))) {
                            Icon(Icons.Default.Add, null, tint = PSRColors.Success, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("Stock In", style = MaterialTheme.typography.labelSmall, color = PSRColors.Success)
                        }
                        OutlinedButton(onClick = onSell, modifier = Modifier.weight(1f).height(32.dp),
                            contentPadding = PaddingValues(0.dp), shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, PSRColors.Accent.copy(0.5f))) {
                            Icon(Icons.Default.ReceiptLong, null, tint = PSRColors.Accent, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("Invoice", style = MaterialTheme.typography.labelSmall, color = PSRColors.Accent)
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// STOCK IN SHEET
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockInSheet(item: StockItem, suppliers: List<Supplier>,
    onDismiss: () -> Unit, onConfirm: (qty: Double, cost: Double, supplier: String, account: FinancialAccount, notes: String) -> Unit) {

    var qty          by remember { mutableStateOf("") }
    var unitCost     by remember { mutableStateOf(item.defaultBuyPrice.let { if (it > 0) "%.2f".format(it) else "" }) }
    var supplierName by remember { mutableStateOf(item.mostFrequentSupplier?.supplierName ?: "") }
    var account      by remember { mutableStateOf(FinancialAccount.CASH) }
    var notes        by remember { mutableStateOf("") }
    var showSupMenu  by remember { mutableStateOf(false) }

    val qtyVal  = qty.toDoubleOrNull() ?: 0.0
    val costVal = unitCost.toDoubleOrNull() ?: 0.0
    val total   = qtyVal * costVal

    // Date-ranked supplier prices for this item
    val rankedPrices = item.rankedSuppliers

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), windowInsets = WindowInsets.ime) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Stock In", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                    Text(item.name, style = MaterialTheme.typography.titleSmall.copy(fontStyle = FontStyle.Italic), color = PSRColors.Accent)
                }
                Text("Curr: %.1f ${item.unit}".format(item.stockQty), style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey600)
            }
            Spacer(Modifier.height(16.dp))

            // Previous supplier prices ranked by frequency + recency
            if (rankedPrices.isNotEmpty()) {
                Text("Last prices from suppliers", style = MaterialTheme.typography.labelMedium, color = PSRColors.Grey600)
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(rankedPrices.take(4)) { sp ->
                        val daysAgo = ((System.currentTimeMillis() - sp.lastUpdated) / 86400000L).toInt()
                        val dateLabel = if (daysAgo == 0) "Today" else if (daysAgo == 1) "Yesterday" else "${daysAgo}d ago"
                        ElevatedCard(onClick = { supplierName = sp.supplierName; unitCost = "%.2f".format(sp.price) },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.elevatedCardColors(containerColor = if (supplierName == sp.supplierName) PSRColors.Navy600 else PSRColors.Card)) {
                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(sp.supplierName, style = MaterialTheme.typography.labelMedium,
                                    color = if (supplierName == sp.supplierName) PSRColors.White else PSRColors.Navy900,
                                    maxLines = 1)
                                Text("RM %.2f".format(sp.price),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic),
                                    color = if (supplierName == sp.supplierName) PSRColors.Gold else PSRColors.Accent)
                                Text(dateLabel, style = MaterialTheme.typography.labelSmall, color = if (supplierName == sp.supplierName) PSRColors.White.copy(0.6f) else PSRColors.Grey500)
                                if (sp.buyCount > 1) Text("×${sp.buyCount}", style = MaterialTheme.typography.labelSmall, color = PSRColors.Grey400)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Supplier name — searchable from saved list, auto-adds new ones
            val isNewSupInSheet = supplierName.isNotBlank() && suppliers.none { it.name.equals(supplierName.trim(), ignoreCase = true) }
            ExposedDropdownMenuBox(expanded = showSupMenu, onExpandedChange = { showSupMenu = it }) {
                OutlinedTextField(value = supplierName, onValueChange = { supplierName = it; showSupMenu = true },
                    label = { Text("Supplier") }, modifier = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(showSupMenu) },
                    leadingIcon = { Icon(Icons.Outlined.Business, null, modifier = Modifier.size(18.dp)) },
                    placeholder = { Text("Search or type supplier name") })
                ExposedDropdownMenu(expanded = showSupMenu, onDismissRequest = { showSupMenu = false }) {
                    val matchedSuppliers = suppliers.filter { it.name.contains(supplierName, true) || supplierName.isBlank() }
                    matchedSuppliers.forEach { s ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(s.name, style = MaterialTheme.typography.bodyMedium)
                                    if (s.phone.isNotBlank()) Text(s.phone, style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey500)
                                }
                            },
                            onClick = { supplierName = s.name; showSupMenu = false })
                    }
                    if (isNewSupInSheet) {
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.PersonAdd, null, tint = PSRColors.Accent, modifier = Modifier.size(16.dp))
                                    Text("Add \"${supplierName.trim()}\" as new supplier", style = MaterialTheme.typography.bodyMedium, color = PSRColors.Accent)
                                }
                            },
                            onClick = { showSupMenu = false }
                        )
                    }
                }
            }
            if (isNewSupInSheet) {
                Text("Will be added to your supplier list on save", style = MaterialTheme.typography.labelSmall, color = PSRColors.Accent,
                    modifier = Modifier.padding(top = 2.dp, start = 4.dp))
            }
            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = qty, onValueChange = { qty = it }, label = { Text("Qty received") },
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    suffix = { Text(item.unit, color = PSRColors.Grey600) })
                RMInputField("Cost / unit", unitCost, { unitCost = it }, Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            }
            Spacer(Modifier.height(8.dp))

            // Account selector
            Text("Pay from", style = MaterialTheme.typography.labelMedium, color = PSRColors.Grey600)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth().background(PSRColors.Grey100, RoundedCornerShape(12.dp)).padding(4.dp)) {
                FinancialAccount.entries.forEach { acc ->
                    val sel = account == acc
                    Box(Modifier.weight(1f).background(if (sel) PSRColors.Navy600 else Color.Transparent, RoundedCornerShape(10.dp)).clickable { account = acc }.padding(vertical = 10.dp), Alignment.Center) {
                        Text(acc.name, style = MaterialTheme.typography.labelLarge, color = if (sel) PSRColors.White else PSRColors.Grey600)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
            Spacer(Modifier.height(12.dp))

            if (total > 0) {
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = PSRColors.Navy600.copy(0.07f))) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Total cost", style = MaterialTheme.typography.bodyMedium, color = PSRColors.Grey700)
                        Text("RM %.2f".format(total), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic), color = PSRColors.Navy600)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            Button(onClick = {
                if (qtyVal > 0) onConfirm(qtyVal, costVal, supplierName.ifBlank { "Unknown" }, account, notes)
            }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PSRColors.Success),
                enabled = qtyVal > 0) {
                Icon(Icons.Default.MoveToInbox, null)
                Spacer(Modifier.width(8.dp))
                Text("Receive Stock", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

// ─────────────────────────────────────────────
// SELL STOCK SHEET
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellStockSheet(item: StockItem, onDismiss: () -> Unit,
    onConfirm: (qty: Double, sellPrice: Double, costPrice: Double, account: FinancialAccount, notes: String) -> Unit) {

    var qty      by remember { mutableStateOf("") }
    var sellPrice by remember { mutableStateOf("") }
    var costPrice by remember { mutableStateOf(item.defaultBuyPrice.let { if (it > 0) "%.2f".format(it) else "" }) }
    var account  by remember { mutableStateOf(FinancialAccount.CASH) }
    var notes    by remember { mutableStateOf("") }

    val qtyVal  = qty.toDoubleOrNull() ?: 0.0
    val sellVal = sellPrice.toDoubleOrNull() ?: 0.0
    val costVal = costPrice.toDoubleOrNull() ?: 0.0
    val revenue = qtyVal * sellVal
    val profit  = qtyVal * (sellVal - costVal)
    val margin  = if (costVal > 0) ((sellVal - costVal) / costVal) * 100 else 0.0

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), windowInsets = WindowInsets.ime) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Sell Stock", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                    Text(item.name, style = MaterialTheme.typography.titleSmall.copy(fontStyle = FontStyle.Italic), color = PSRColors.Accent)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Available", style = MaterialTheme.typography.labelSmall, color = PSRColors.Grey500)
                    Text("%.1f ${item.unit}".format(item.stockQty), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = PSRColors.Navy600)
                }
            }
            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = qty, onValueChange = { qty = it }, label = { Text("Qty to sell") },
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    suffix = { Text(item.unit, color = PSRColors.Grey600) },
                    isError = qtyVal > item.stockQty)
                RMInputField("Sell Price / unit", sellPrice, { sellPrice = it }, Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            }
            if (qtyVal > item.stockQty) {
                Text("⚠ Exceeds available stock (%.1f ${item.unit})".format(item.stockQty),
                    style = MaterialTheme.typography.bodySmall, color = PSRColors.Error, modifier = Modifier.padding(top = 4.dp))
            }
            Spacer(Modifier.height(10.dp))
            RMInputField("Cost / unit (for profit calc)", costPrice, { costPrice = it }, Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            Spacer(Modifier.height(10.dp))

            // Account
            Text("Receive payment to", style = MaterialTheme.typography.labelMedium, color = PSRColors.Grey600)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth().background(PSRColors.Grey100, RoundedCornerShape(12.dp)).padding(4.dp)) {
                FinancialAccount.entries.forEach { acc ->
                    val sel = account == acc
                    Box(Modifier.weight(1f).background(if (sel) PSRColors.Navy600 else Color.Transparent, RoundedCornerShape(10.dp)).clickable { account = acc }.padding(vertical = 10.dp), Alignment.Center) {
                        Text(acc.name, style = MaterialTheme.typography.labelLarge, color = if (sel) PSRColors.White else PSRColors.Grey600)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
            Spacer(Modifier.height(12.dp))

            // Profit preview
            if (revenue > 0 || profit != 0.0) {
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = (if (profit >= 0) PSRColors.Success else PSRColors.Error).copy(0.07f))) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Revenue", style = MaterialTheme.typography.labelSmall, color = PSRColors.Grey600)
                            Text("RM %.2f".format(revenue), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = PSRColors.Navy900)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Profit", style = MaterialTheme.typography.labelSmall, color = PSRColors.Grey600)
                            Text("RM %.2f".format(profit), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic),
                                color = if (profit >= 0) PSRColors.Success else PSRColors.Error)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Margin", style = MaterialTheme.typography.labelSmall, color = PSRColors.Grey600)
                            Text("%.1f%%".format(margin), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = PSRColors.Accent)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            Button(onClick = {
                if (qtyVal > 0 && qtyVal <= item.stockQty) onConfirm(qtyVal, sellVal, costVal, account, notes)
            }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PSRColors.Accent),
                enabled = qtyVal > 0 && qtyVal <= item.stockQty) {
                Icon(Icons.Default.ShoppingCartCheckout, null)
                Spacer(Modifier.width(8.dp))
                Text("Confirm Sale", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

// ─────────────────────────────────────────────
// SUPPLIER MANAGER SHEET
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierManagerSheet(suppliers: List<Supplier>, onDismiss: () -> Unit, onAdd: (Supplier) -> Unit, onDelete: (Supplier) -> Unit) {
    var newName  by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Text("Suppliers", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
            Text("Saved suppliers auto-fill in Stock In", style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey600)
            Spacer(Modifier.height(16.dp))

            // Add new supplier
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Name *") },
                    modifier = Modifier.weight(1.3f), shape = RoundedCornerShape(10.dp), singleLine = true)
                OutlinedTextField(value = newPhone, onValueChange = { newPhone = it }, label = { Text("Phone") },
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                IconButton(onClick = {
                    if (newName.isNotBlank()) { onAdd(Supplier(name = newName.trim(), phone = newPhone.trim())); newName = ""; newPhone = "" }
                }, modifier = Modifier.size(48.dp).background(PSRColors.Navy600, RoundedCornerShape(12.dp))) {
                    Icon(Icons.Default.Add, null, tint = Color.White)
                }
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = PSRColors.Divider)
            Spacer(Modifier.height(8.dp))

            if (suppliers.isEmpty()) {
                Text("No suppliers saved yet", style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey400, modifier = Modifier.padding(8.dp))
            } else {
                suppliers.forEach { s ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(38.dp).background(PSRColors.Navy600.copy(0.1f), CircleShape), Alignment.Center) {
                            Text(s.name.firstOrNull()?.uppercase() ?: "?", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = PSRColors.Navy600))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(s.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = PSRColors.Navy900)
                            if (s.phone.isNotBlank()) Text(s.phone, style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey600)
                        }
                        IconButton(onClick = { onDelete(s) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.DeleteOutline, null, tint = PSRColors.Error.copy(0.7f), modifier = Modifier.size(18.dp))
                        }
                    }
                    HorizontalDivider(color = PSRColors.Divider)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// CATEGORY CHIP
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryChip(label: String, emoji: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick,
        label = { Text("$emoji $label", fontSize = 12.sp) },
        shape = RoundedCornerShape(10.dp),
        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PSRColors.Navy600, selectedLabelColor = PSRColors.White, containerColor = PSRColors.Grey50, labelColor = PSRColors.Navy700))
}

// ─────────────────────────────────────────────
// ADD / EDIT STOCK DIALOG — with supplier dropdown
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditStockDialog(stock: StockItem?, categories: List<Category>, units: List<String>,
    suppliers: List<Supplier>, onDismiss: () -> Unit, onSave: (StockItem) -> Unit,
    onDelete: (StockItem) -> Unit, onAddCategory: () -> Unit) {

    val context = LocalContext.current
    var name           by remember { mutableStateOf(stock?.name ?: "") }
    var unit           by remember { mutableStateOf(stock?.unit ?: units.firstOrNull() ?: "kg") }
    var qty            by remember { mutableStateOf(stock?.stockQty?.let { "%.1f".format(it) } ?: "0.0") }
    var sellPriceStr   by remember { mutableStateOf(if ((stock?.sellPrice ?: 0.0) > 0) "%.2f".format(stock!!.sellPrice) else "") }
    var customerSpecStr by remember { mutableStateOf(stock?.customerSpec ?: "") }
    var notes          by remember { mutableStateOf(stock?.notes ?: "") }
    var categoryId     by remember { mutableStateOf(stock?.categoryId ?: categories.firstOrNull()?.id ?: 0L) }
    var imagePath      by remember { mutableStateOf(stock?.imagePath) }
    var supplierPrices by remember { mutableStateOf(stock?.supplierPrices ?: emptyList()) }
    var showCatMenu    by remember { mutableStateOf(false) }
    var showUnitMenu   by remember { mutableStateOf(false) }
    var showDeleteConf by remember { mutableStateOf(false) }
    var newSupName     by remember { mutableStateOf("") }
    var newSupPrice    by remember { mutableStateOf("") }
    var showSupMenu    by remember { mutableStateOf(false) }

    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80) }
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val file = File(context.filesDir, "stock_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(it)?.use { input -> FileOutputStream(file).use { out -> input.copyTo(out) } }
            imagePath = file.absolutePath
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), windowInsets = WindowInsets.ime) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp).verticalScroll(rememberScrollState())) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (stock == null) "Add Stock Item" else "Edit Item", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                if (stock != null) IconButton(onClick = { showDeleteConf = true }) { Icon(Icons.Default.DeleteOutline, null, tint = PSRColors.Error) }
            }
            Spacer(Modifier.height(16.dp))

            // Photo
            Box(modifier = Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(16.dp)).background(PSRColors.Grey100).clickable { photoLauncher.launch("image/*") }, contentAlignment = Alignment.Center) {
                if (imagePath != null) {
                    AsyncImage(model = imagePath, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    IconButton(onClick = { imagePath = null }, modifier = Modifier.align(Alignment.TopEnd)) { Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.background(Color.Black.copy(0.4f), CircleShape)) }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AddPhotoAlternate, null, tint = PSRColors.Grey400, modifier = Modifier.size(28.dp))
                        Text("Add Photo", style = MaterialTheme.typography.labelSmall, color = PSRColors.Grey600)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))

            // Category
            val selCat = categories.find { it.id == categoryId }
            ExposedDropdownMenuBox(expanded = showCatMenu, onExpandedChange = { showCatMenu = it }) {
                OutlinedTextField(value = "${selCat?.emoji ?: ""} ${selCat?.name ?: "Select Category"}", onValueChange = {}, readOnly = true,
                    label = { Text("Category") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(showCatMenu) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                ExposedDropdownMenu(expanded = showCatMenu, onDismissRequest = { showCatMenu = false }) {
                    categories.forEach { cat -> DropdownMenuItem(text = { Text("${cat.emoji} ${cat.name}") }, onClick = { categoryId = cat.id; showCatMenu = false }) }
                    Divider()
                    DropdownMenuItem(text = { Text("+ Add New Category", color = PSRColors.Accent) }, onClick = { showCatMenu = false; onAddCategory() })
                }
            }
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Product Name *") },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = qty, onValueChange = { qty = it }, label = { Text("Stock Qty") },
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                ExposedDropdownMenuBox(expanded = showUnitMenu, onExpandedChange = { showUnitMenu = it }, modifier = Modifier.weight(1f)) {
                    OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("Unit") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(showUnitMenu) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
                    ExposedDropdownMenu(expanded = showUnitMenu, onDismissRequest = { showUnitMenu = false }) {
                        units.forEach { u -> DropdownMenuItem(text = { Text(u) }, onClick = { unit = u; showUnitMenu = false }) }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))

            // Supplier prices section
            Text("Supplier Prices", style = MaterialTheme.typography.titleSmall.copy(color = PSRColors.Navy600))
            Spacer(Modifier.height(6.dp))

            if (supplierPrices.isNotEmpty()) {
                supplierPrices.forEachIndexed { idx, sp ->
                    val daysAgo = ((System.currentTimeMillis() - sp.lastUpdated) / 86400000L).toInt()
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).background(PSRColors.Grey50, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (sp.isDefault) Text("★ ", color = PSRColors.Gold, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        Column(Modifier.weight(1f)) {
                            Text(sp.supplierName, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = PSRColors.Navy900)
                            Text(if (daysAgo == 0) "Updated today" else "${daysAgo}d ago • ×${sp.buyCount} buys", style = MaterialTheme.typography.labelSmall, color = PSRColors.Grey500)
                        }
                        Text("RM %.2f".format(sp.price), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic), color = PSRColors.Navy600)
                        Spacer(Modifier.width(4.dp))
                        // Toggle default
                        IconButton(onClick = { supplierPrices = supplierPrices.mapIndexed { i, s -> s.copy(isDefault = i == idx) } }, modifier = Modifier.size(28.dp)) {
                            Icon(if (sp.isDefault) Icons.Default.Star else Icons.Outlined.StarOutline, null, tint = PSRColors.Gold, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = { supplierPrices = supplierPrices.toMutableList().also { it.removeAt(idx) } }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, null, tint = PSRColors.Error, modifier = Modifier.size(14.dp))
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }

            // Add supplier price row — with dropdown from saved suppliers
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                ExposedDropdownMenuBox(expanded = showSupMenu, onExpandedChange = { showSupMenu = it }, modifier = Modifier.weight(1.3f)) {
                    OutlinedTextField(value = newSupName, onValueChange = { newSupName = it; showSupMenu = it.isNotEmpty() },
                        label = { Text("Supplier") }, modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp), textStyle = MaterialTheme.typography.bodySmall, singleLine = true)
                    if (suppliers.isNotEmpty()) {
                        ExposedDropdownMenu(expanded = showSupMenu, onDismissRequest = { showSupMenu = false }) {
                            suppliers.filter { it.name.contains(newSupName, true) || newSupName.isBlank() }.take(5).forEach { s ->
                                DropdownMenuItem(text = { Text(s.name, style = MaterialTheme.typography.bodySmall) }, onClick = { newSupName = s.name; showSupMenu = false })
                            }
                        }
                    }
                }
                OutlinedTextField(value = newSupPrice, onValueChange = { newSupPrice = it }, label = { Text("Price") },
                    modifier = Modifier.weight(0.9f), shape = RoundedCornerShape(8.dp),
                    textStyle = MaterialTheme.typography.bodySmall, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix = { Text("RM ", style = MaterialTheme.typography.bodySmall) })
                IconButton(onClick = {
                    val p = newSupPrice.toDoubleOrNull() ?: return@IconButton
                    if (newSupName.isNotBlank()) {
                        supplierPrices = supplierPrices + SupplierPrice(newSupName, p, supplierPrices.isEmpty())
                        newSupName = ""; newSupPrice = ""
                    }
                }, modifier = Modifier.size(48.dp).background(PSRColors.Navy600, RoundedCornerShape(12.dp))) {
                    Icon(Icons.Default.AddCircle, null, tint = Color.White)
                }
            }
            Spacer(Modifier.height(12.dp))

            Spacer(Modifier.height(12.dp))

            // ── Pricing ───────────────────────────────────────────────────
            Text("Pricing", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = PSRColors.Grey600)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RMInputField(
                    label = "Selling Price",
                    value = sellPriceStr,
                    onValueChange = { sellPriceStr = it },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text("Default Buy", style = MaterialTheme.typography.labelSmall, color = PSRColors.Grey500)
                    Text(
                        if (supplierPrices.isEmpty()) "— add supplier" else "RM ${"%.2f".format(supplierPrices.firstOrNull { it.isDefault }?.price ?: supplierPrices.minByOrNull { it.price }?.price ?: 0.0)}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = if (supplierPrices.isEmpty()) PSRColors.Grey400 else PSRColors.Navy600
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes (internal)") },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
            Spacer(Modifier.height(10.dp))

            // Customer-facing spec — shown on card in customer mode only
            OutlinedTextField(
                value = customerSpecStr,
                onValueChange = { customerSpecStr = it },
                label = { Text("Spec / Dimensions (customer view)") },
                placeholder = { Text("e.g. 30×20×10cm, 500g pack, 12 per carton") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 1, maxLines = 3,
                trailingIcon = {
                    if (customerSpecStr.isNotBlank())
                        Icon(Icons.Default.Person, null, tint = PSRColors.Accent, modifier = Modifier.size(16.dp))
                }
            )
            Spacer(Modifier.height(20.dp))

            Button(onClick = {
                if (name.isBlank()) return@Button
                toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK)
                onSave((stock ?: StockItem(categoryId = categoryId, name = name)).copy(
                    categoryId = categoryId, name = name, unit = unit, imagePath = imagePath,
                    supplierPrices = supplierPrices, sellPrice = sellPriceStr.toDoubleOrNull() ?: 0.0,
                    customerSpec = customerSpecStr.trim(),
                    stockQty = qty.toDoubleOrNull() ?: 0.0,
                    notes = notes, updatedAt = System.currentTimeMillis()
                ))
            }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PSRColors.Navy600)) {
                Text(if (stock == null) "Add Product" else "Save Changes", style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    if (showDeleteConf && stock != null) {
        AlertDialog(onDismissRequest = { showDeleteConf = false },
            title = { Text("Delete Item?") }, text = { Text("This removes ${stock.name} from the catalogue.") },
            confirmButton = { TextButton(onClick = { onDelete(stock) }) { Text("Delete", color = PSRColors.Error) } },
            dismissButton = { TextButton(onClick = { showDeleteConf = false }) { Text("Cancel") } })
    }
}

// ─────────────────────────────────────────────
// ADD CATEGORY DIALOG
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCategoryDialog(onDismiss: () -> Unit, onSave: (Category) -> Unit) {
    val emojis = listOf("🐔","🐟","🥬","🍎","🥩","🥚","🌽","🍅","🧅","🧄","📦","🛒","🐄","🦐","🍗","🍖","🫘","🥕")
    var name          by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("📦") }

    AlertDialog(onDismissRequest = onDismiss, title = { Text("New Category") }, text = {
        Column {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Category Name") },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
            Spacer(Modifier.height(12.dp))
            Text("Choose Icon", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(8.dp))
            LazyVerticalGrid(columns = GridCells.Fixed(6), modifier = Modifier.height(120.dp)) {
                items(emojis) { emoji ->
                    Box(Modifier.padding(2.dp).size(40.dp).background(if (emoji == selectedEmoji) PSRColors.Navy600 else PSRColors.Grey50, RoundedCornerShape(8.dp)).clickable { selectedEmoji = emoji }, Alignment.Center) {
                        Text(emoji, style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        }
    },
    confirmButton = { Button(onClick = { if (name.isBlank()) return@Button; onSave(Category(name = name, emoji = selectedEmoji)) }, colors = ButtonDefaults.buttonColors(containerColor = PSRColors.Navy600)) { Text("Create") } },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

// ─────────────────────────────────────────────
// PRICE EXPORT SHEET
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceExportSheet(
    allStock: List<StockItem>,
    categories: List<Category>,
    settings: com.psrmart.app.data.model.BusinessSettings,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var searchQuery        by remember { mutableStateOf("") }
    var selectedCategories by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var selectedIds        by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var editableText       by remember { mutableStateOf("") }   // fully editable after generate
    var showResult         by remember { mutableStateOf(false) }
    val clipboard = remember { context.getSystemService(android.content.ClipboardManager::class.java) }
    val dateStr = remember { java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date()) }

    val filteredStock = remember(allStock, searchQuery, selectedCategories) {
        allStock.filter { item ->
            val matchesSearch = searchQuery.isBlank() || item.name.contains(searchQuery, true)
            val matchesCat = selectedCategories.isEmpty() || item.categoryId in selectedCategories
            matchesSearch && matchesCat
        }
    }

    fun buildPriceList(): String {
        val sb = StringBuilder()
        // Header from template — replace {date} placeholder
        val header = settings.priceListHeader.replace("{date}", dateStr)
        if (header.isNotBlank()) { sb.appendLine(header); sb.appendLine() }

        val selected = filteredStock.filter { it.id in selectedIds }
        val byCategory = selected.groupBy { it.categoryId }
        byCategory.forEach { (catId, items) ->
            val cat = categories.find { it.id == catId }
            if (cat != null) sb.appendLine("${cat.emoji} *${cat.name}*")
            items.forEach { item ->
                val price = item.currentSellPrice
                val priceStr = if (price > 0) "RM ${"%.2f".format(price)}/${item.unit}" else "Price TBD"
                sb.appendLine("• ${item.name} $priceStr")
            }
            sb.appendLine()
        }

        // Footer from template
        val footer = settings.priceListFooter
        if (footer.isNotBlank()) sb.append(footer)
        return sb.toString().trimEnd()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        modifier = Modifier.fillMaxHeight(0.92f)
    ) {
        if (showResult) {
            // ── Editable result screen ─────────────────────────────────────
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showResult = false }) { Icon(Icons.Default.ArrowBack, null) }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Price List", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                        Text("Tap anywhere to edit", style = MaterialTheme.typography.labelSmall, color = PSRColors.Accent)
                    }
                    // Regenerate button
                    IconButton(onClick = { editableText = buildPriceList() }) {
                        Icon(Icons.Default.Refresh, "Regenerate", tint = PSRColors.Grey500)
                    }
                }
                Spacer(Modifier.height(8.dp))

                // Tip banner
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .background(PSRColors.Accent.copy(0.08f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Edit, null, tint = PSRColors.Accent, modifier = Modifier.size(14.dp))
                    Text(
                        "Fully editable — change any price or text before sharing",
                        style = MaterialTheme.typography.labelSmall,
                        color = PSRColors.Accent
                    )
                }
                Spacer(Modifier.height(12.dp))

                // Editable text field — the entire list is editable
                OutlinedTextField(
                    value = editableText,
                    onValueChange = { editableText = it },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        lineHeight = 22.sp
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PSRColors.Accent,
                        unfocusedBorderColor = PSRColors.Grey200,
                        focusedContainerColor = PSRColors.Grey50,
                        unfocusedContainerColor = PSRColors.Grey50
                    )
                )
                Spacer(Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            val clip = android.content.ClipData.newPlainText("Price List", editableText)
                            clipboard.setPrimaryClip(clip)
                        },
                        modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Copy")
                    }
                    Button(
                        onClick = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, editableText)
                            }
                            context.startActivity(android.content.Intent.createChooser(intent, "Share Price List").apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) })
                        },
                        modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PSRColors.Navy600)
                    ) {
                        Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Share")
                    }
                }
            }
        } else {
            // ── Selection screen ───────────────────────────────────────────
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
                Text("Export Price List", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                Text("Select items to include", style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey600)
                Spacer(Modifier.height(12.dp))

                // Search
                OutlinedTextField(
                    value = searchQuery, onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name…") },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = PSRColors.Grey400) },
                    trailingIcon = { if (searchQuery.isNotBlank()) IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, null, modifier = Modifier.size(16.dp)) } },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PSRColors.Navy600)
                )
                Spacer(Modifier.height(8.dp))

                // Category filter chips
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
                    item {
                        FilterChip(
                            selected = selectedCategories.isEmpty(),
                            onClick = { selectedCategories = emptySet() },
                            label = { Text("All") },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PSRColors.Navy600, selectedLabelColor = Color.White)
                        )
                    }
                    items(categories) { cat ->
                        FilterChip(
                            selected = cat.id in selectedCategories,
                            onClick = {
                                selectedCategories = if (cat.id in selectedCategories)
                                    selectedCategories - cat.id else selectedCategories + cat.id
                            },
                            label = { Text("${cat.emoji} ${cat.name}") },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PSRColors.Navy600, selectedLabelColor = Color.White)
                        )
                    }
                }

                // Select all / none row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("${filteredStock.size} items", style = MaterialTheme.typography.labelMedium, color = PSRColors.Grey600)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { selectedIds = filteredStock.map { it.id }.toSet() }) { Text("Select All") }
                        TextButton(onClick = { selectedIds = emptySet() }) { Text("Clear") }
                    }
                }

                // Item list
                LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(vertical = 4.dp)) {
                    items(filteredStock, key = { it.id }) { item ->
                        val isSelected = item.id in selectedIds
                        val cat = remember(item.categoryId) { categories.find { it.id == item.categoryId } }
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) PSRColors.Navy600.copy(0.06f) else Color.Transparent)
                                .clickable {
                                    selectedIds = if (isSelected) selectedIds - item.id else selectedIds + item.id
                                }
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { selectedIds = if (isSelected) selectedIds - item.id else selectedIds + item.id },
                                colors = CheckboxDefaults.colors(checkedColor = PSRColors.Navy600)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                                if (cat != null) Text("${cat.emoji} ${cat.name}", style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey500)
                            }
                            val price = item.currentSellPrice
                            Text(
                                if (price > 0) "RM ${"%.2f".format(price)}/${item.unit}" else "—",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (price > 0) PSRColors.Navy600 else PSRColors.Grey400
                            )
                        }
                        HorizontalDivider(color = PSRColors.Divider)
                    }
                }

                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (selectedIds.isEmpty()) return@Button
                        editableText = buildPriceList()
                        showResult = true
                    },
                    enabled = selectedIds.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PSRColors.Navy600)
                ) {
                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Generate (${selectedIds.size} selected)", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
