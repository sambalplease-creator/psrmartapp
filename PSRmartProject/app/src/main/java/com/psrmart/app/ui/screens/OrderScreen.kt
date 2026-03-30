@file:OptIn(ExperimentalMaterial3Api::class)
package com.psrmart.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import com.psrmart.app.data.model.*
import com.psrmart.app.ui.components.*
import com.psrmart.app.ui.theme.*
import com.psrmart.app.viewmodel.PSRViewModel
import java.text.SimpleDateFormat
import java.util.*

// ─────────────────────────────────────────────
// ROOT — Tab host for Orders + Tasks
// ─────────────────────────────────────────────

@Composable
fun OrderScreen(viewModel: PSRViewModel, onAddOrderClick: () -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize().background(PSRColors.Surface)) {
        Box(modifier = Modifier.fillMaxWidth().background(PSRColors.Navy700)) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("📦  Orders", "🛒  Online", "📋  Tasks").forEachIndexed { idx, label ->
                    val active = selectedTab == idx
                    Box(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                            .background(if (active) PSRColors.White.copy(0.15f) else Color.Transparent)
                            .clickable { selectedTab = idx }.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = InterFamily,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal),
                            color = if (active) PSRColors.White else PSRColors.White.copy(0.5f))
                    }
                }
            }
        }

        AnimatedContent(targetState = selectedTab,
            transitionSpec = {
                if (targetState > initialState)
                    (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it } + fadeOut())
                else
                    (slideInHorizontally { -it } + fadeIn()) togetherWith (slideOutHorizontally { it } + fadeOut())
            }, label = "tab") { tab ->
            when (tab) {
                0 -> OrdersTab(viewModel = viewModel, onAddOrderClick = onAddOrderClick)
                1 -> OnlineOrdersTab(viewModel = viewModel)
                2 -> TasksTab(viewModel = viewModel)
                else -> OrdersTab(viewModel = viewModel, onAddOrderClick = onAddOrderClick)
            }
        }
    }
}

// ═════════════════════════════════════════════
// TAB 0 — ORDERS
// ═════════════════════════════════════════════

@Composable
fun OrdersTab(viewModel: PSRViewModel, onAddOrderClick: () -> Unit) {
    val orders     by viewModel.activeOrders.collectAsState()
    val allOrders  by viewModel.allOrders.collectAsState()
    val customers  by viewModel.customers.collectAsState()
    var showPast   by remember { mutableStateOf(false) }

    val sorted    = remember(orders) { orders.sortedWith(compareBy({ it.isComplete }, { -it.createdAt })) }
    val pastOrders = remember(allOrders) {
        allOrders.filter { it.isArchived }.sortedByDescending { it.createdAt }.take(15)
    }
    val total     = orders.size
    val completed = orders.count { it.isComplete }
    val pending   = orders.filter { !it.isComplete }.sumOf { it.items.count { i -> !i.isChecked } }
    val purchased = orders.filter { !it.isComplete }.sumOf { it.items.count { i -> i.isPurchased && !i.isChecked } }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Sub-header with Today / Past toggle ──────────────────
        Row(modifier = Modifier.fillMaxWidth().background(PSRColors.Navy600.copy(0.06f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            if (!showPast) {
                OrderStatChip("Orders", "$completed/$total", Icons.Outlined.CheckCircle, PSRColors.Success)
                OrderStatChip("Items left", "$pending", Icons.Outlined.Pending, PSRColors.Warning)
                if (purchased > 0)
                    OrderStatChip("Bought", "$purchased", Icons.Default.ShoppingCart, PSRColors.Accent)
                if (total > 0 && completed == total)
                    Text("All done!", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontFamily = InterFamily), color = PSRColors.Success)
            } else {
                Text("Last 15 past orders", style = MaterialTheme.typography.labelMedium, color = PSRColors.Grey600)
            }
            Spacer(Modifier.weight(1f))
            // Toggle button
            Surface(
                onClick = { showPast = !showPast },
                shape = RoundedCornerShape(8.dp),
                color = if (showPast) PSRColors.Navy600 else PSRColors.Grey100,
                modifier = Modifier
            ) {
                Text(
                    if (showPast) "← Today" else "Past Orders",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (showPast) Color.White else PSRColors.Grey600,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        if (!showPast) {
            // ── Active orders ──────────────────────────────────────
            if (sorted.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(icon = Icons.Outlined.AssignmentTurnedIn, title = "No orders today",
                        subtitle = "Tap + to add your first order", actionLabel = "Add Order", onAction = onAddOrderClick)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(sorted, key = { it.id }) { order ->
                        OrderTile(order = order, viewModel = viewModel, customers = customers)
                    }
                    item { Spacer(Modifier.height(100.dp)) }
                }
            }
        } else {
            // ── Past orders ────────────────────────────────────────
            if (pastOrders.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(icon = Icons.Outlined.History, title = "No past orders yet",
                        subtitle = "Completed orders from previous days appear here")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(pastOrders, key = { "past_${it.id}" }) { order ->
                        PastOrderRow(order = order, onDelete = { viewModel.deleteOrder(order) })
                    }
                    item { Spacer(Modifier.height(100.dp)) }
                }
            }
        }
    }
}

@Composable
fun PastOrderRow(order: CustomerOrder, onDelete: () -> Unit) {
    val sdf      = remember { SimpleDateFormat("dd MMM, EEE", Locale.getDefault()) }
    val dateStr  = remember(order.createdAt) { sdf.format(Date(order.createdAt)) }
    var expanded by remember { mutableStateOf(false) }
    var showDeleteConf by remember { mutableStateOf(false) }

    Card(
        onClick  = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(14.dp),
        colors   = CardDefaults.cardColors(containerColor = PSRColors.Card),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(order.customerName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = PSRColors.Navy900)
                    Text("$dateStr · ${order.items.size} item${if (order.items.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey500)
                }
                Icon(Icons.Default.CheckCircle, null, tint = PSRColors.Success.copy(0.5f),
                    modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null, tint = PSRColors.Grey400, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = { showDeleteConf = true }, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.DeleteOutline, null, tint = PSRColors.Error.copy(0.5f),
                        modifier = Modifier.size(18.dp))
                }
            }

            // Collapsed: show item summary
            if (!expanded) {
                Text(
                    order.items.joinToString(" · ") { it.text },
                    style = MaterialTheme.typography.labelSmall, color = PSRColors.Grey400,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Expanded: show full item list with tick states
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    HorizontalDivider(color = PSRColors.Divider, modifier = Modifier.padding(bottom = 6.dp))
                    order.items.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Purchased tick
                            Icon(
                                if (item.isPurchased) Icons.Default.ShoppingCart
                                else Icons.Outlined.RadioButtonUnchecked,
                                null,
                                tint = if (item.isPurchased) PSRColors.Accent else PSRColors.Grey200,
                                modifier = Modifier.size(14.dp)
                            )
                            // Sent tick
                            Icon(
                                Icons.Default.LocalShipping,
                                null,
                                tint = if (item.isChecked) PSRColors.Success else PSRColors.Grey200,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                item.text,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    textDecoration = if (item.isChecked) TextDecoration.LineThrough
                                                     else TextDecoration.None),
                                color = if (item.isChecked) PSRColors.Grey400 else PSRColors.Navy800,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConf) {
        AlertDialog(
            onDismissRequest = { showDeleteConf = false },
            title = { Text("Remove past order?") },
            text = { Text("Remove ${order.customerName}'s order from history?") },
            confirmButton = {
                Button(onClick = { onDelete(); showDeleteConf = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PSRColors.Error)) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConf = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun OrderStatChip(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(14.dp))
        Text(value, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontFamily = InterFamily), color = PSRColors.Navy900)
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontFamily = InterFamily), color = PSRColors.Grey500)
    }
}

// ─────────────────────────────────────────────
// ORDER TILE
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderTile(order: CustomerOrder, viewModel: PSRViewModel, customers: List<Customer>) {
    var expanded       by remember { mutableStateOf(!order.isComplete) }
    var showEditSheet  by remember { mutableStateOf(false) }
    var showDeleteConf by remember { mutableStateOf(false) }

    val done         = order.isComplete
    val progress     = if (order.items.isEmpty()) 0f else order.checkedCount.toFloat() / order.items.size
    val progAnim     by animateFloatAsState(progress, tween(500), label = "prog")
    val alphaAnim    by animateFloatAsState(if (done) 0.6f else 1f, tween(400), label = "alpha")
    val timeStr      = remember(order.createdAt) { SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(order.createdAt)) }

    Card(
        modifier = Modifier.fillMaxWidth().alpha(alphaAnim),
        onClick = { if (!done) expanded = !expanded else if (!expanded) expanded = true else showDeleteConf = true },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = if (done) PSRColors.Grey100 else Color.White),
        elevation = CardDefaults.cardElevation(if (done) 0.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.size(46.dp).background(
                    if (done) PSRColors.Success.copy(0.12f) else PSRColors.Navy600.copy(0.08f), CircleShape),
                    contentAlignment = Alignment.Center) {
                    if (done) Icon(Icons.Default.CheckCircle, null, tint = PSRColors.Success, modifier = Modifier.size(26.dp))
                    else Text(order.customerName.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black, fontFamily = PlayfairFamily),
                        color = PSRColors.Navy600)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(order.customerName, style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold, fontFamily = InterFamily,
                        textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None),
                        color = if (done) PSRColors.Grey500 else PSRColors.Navy900, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${order.checkedCount}/${order.items.size} items",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = InterFamily),
                            color = if (done) PSRColors.Success else PSRColors.Grey600)
                        Text("·", color = PSRColors.Grey400, fontSize = 10.sp)
                        Text(timeStr, style = MaterialTheme.typography.bodySmall.copy(fontFamily = InterFamily), color = PSRColors.Grey400)
                    }
                }

                if (done) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.DeleteOutline, null, tint = PSRColors.Error.copy(0.5f), modifier = Modifier.size(18.dp)
                            .clickable { showDeleteConf = true })
                        Text("tap to delete", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = PSRColors.Grey400)
                    }
                } else {
                    IconButton(onClick = { showEditSheet = true }, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.EditNote, null, tint = PSRColors.Grey400, modifier = Modifier.size(18.dp))
                    }
                    Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        null, tint = PSRColors.Grey300, modifier = Modifier.size(18.dp))
                }
            }

            // Progress bar
            Spacer(Modifier.height(10.dp))
            Box(modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)).background(PSRColors.Grey100)) {
                Box(modifier = Modifier.fillMaxWidth(progAnim).fillMaxHeight().clip(RoundedCornerShape(3.dp))
                    .background(if (done) PSRColors.Success else PSRColors.Accent))
            }

            // Checklist
            AnimatedVisibility(visible = expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Column(modifier = Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    HorizontalDivider(color = PSRColors.Grey100, modifier = Modifier.padding(bottom = 6.dp))
                    order.items.forEach { item ->
                        OrderCheckRow(
                            item              = item,
                            isDone            = done,
                            onToggle          = { viewModel.toggleOrderItem(order, item.id) },
                            onEdit            = if (!done) { newText -> viewModel.editOrderItemText(order, item.id, newText) } else null,
                            onTogglePurchased = if (!done) { -> viewModel.toggleOrderItemPurchased(order, item.id) } else null
                        )
                    }
                }
            }
        }
    }

    if (showEditSheet) OrderEditSheet(order = order, customers = customers, onDismiss = { showEditSheet = false },
        onSave = { updated -> viewModel.updateOrder(updated); showEditSheet = false })
    if (showDeleteConf) AlertDialog(onDismissRequest = { showDeleteConf = false },
        title = { Text("Remove order?") },
        text = { Text("Remove ${order.customerName}'s order?") },
        confirmButton = { Button(onClick = { viewModel.deleteOrder(order); showDeleteConf = false },
            colors = ButtonDefaults.buttonColors(containerColor = PSRColors.Error)) { Text("Remove") } },
        dismissButton = { TextButton(onClick = { showDeleteConf = false }) { Text("Cancel") } })
}

@Composable
fun OrderCheckRow(item: OrderItem, isDone: Boolean, onToggle: () -> Unit, onEdit: ((String) -> Unit)? = null, onTogglePurchased: (() -> Unit)? = null) {
    var editing        by remember { mutableStateOf(false) }
    var editText       by remember(item.text) { mutableStateOf(item.text) }
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }

    LaunchedEffect(editing) { if (editing) focusRequester.requestFocus() }

    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)) {

        // ── Tick 1: Purchased/picked ──────────────────────────────────
        Box(
            modifier = Modifier.size(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    when {
                        item.isChecked   -> PSRColors.Success.copy(0.15f)
                        item.isPurchased -> PSRColors.Accent.copy(0.15f)
                        else             -> PSRColors.Grey100
                    }
                )
                .clickable {
                    if (onTogglePurchased != null) onTogglePurchased()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                when {
                    item.isChecked   -> Icons.Default.CheckCircle
                    item.isPurchased -> Icons.Default.ShoppingCart
                    else             -> Icons.Outlined.RadioButtonUnchecked
                },
                contentDescription = "Purchased",
                tint = when {
                    item.isChecked   -> PSRColors.Success
                    item.isPurchased -> PSRColors.Accent
                    else             -> PSRColors.Grey300
                },
                modifier = Modifier.size(18.dp)
            )
        }

        // ── Tick 2: Sent/complete ─────────────────────────────────────
        Box(
            modifier = Modifier.size(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    if (item.isChecked) PSRColors.Success.copy(0.15f)
                    else if (item.isPurchased) PSRColors.Navy600.copy(0.08f)
                    else PSRColors.Grey50
                )
                .clickable(enabled = item.isPurchased || item.isChecked) { onToggle() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.LocalShipping,
                contentDescription = "Sent",
                tint = when {
                    item.isChecked   -> PSRColors.Success
                    item.isPurchased -> PSRColors.Navy600
                    else             -> PSRColors.Grey200
                },
                modifier = Modifier.size(18.dp)
            )
        }

        // ── Item text / edit field ────────────────────────────────────
        if (editing && !item.isChecked && onEdit != null) {
            BasicTextField(
                value = editText,
                onValueChange = { editText = it },
                modifier = Modifier.weight(1f).focusRequester(focusRequester),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = InterFamily, color = PSRColors.Navy800),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (editText.isNotBlank()) onEdit(editText.trim())
                    editing = false
                }),
                decorationBox = { inner ->
                    Box(modifier = Modifier.fillMaxWidth()
                        .background(PSRColors.Navy600.copy(0.06f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp)) { inner() }
                }
            )
            IconButton(onClick = { if (editText.isNotBlank()) onEdit(editText.trim()); editing = false },
                modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Check, null, tint = PSRColors.Success, modifier = Modifier.size(18.dp))
            }
        } else {
            Text(
                item.text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = InterFamily,
                    textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (item.isChecked) PSRColors.Grey400 else PSRColors.Navy800),
                modifier = Modifier.weight(1f)
                    .clickable(enabled = !item.isChecked && onEdit != null) {
                        editing = true; editText = item.text
                    }
            )
            if (!item.isChecked && onEdit != null) {
                Icon(Icons.Default.EditNote, null, tint = PSRColors.Grey300, modifier = Modifier.size(14.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────
// ADD ORDER SHEET
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOrderSheet(customers: List<Customer>, existingOrders: List<CustomerOrder>, onDismiss: () -> Unit, onSave: (CustomerOrder) -> Unit) {
    var rawText          by remember { mutableStateOf("") }
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var showCustMenu     by remember { mutableStateOf(false) }
    var customName       by remember { mutableStateOf("") }
    var useCustomName    by remember { mutableStateOf(false) }

    val sdfDay = remember { SimpleDateFormat("yyyyMMdd", Locale.getDefault()) }
    val today = remember { sdfDay.format(Date()) }
    val available = remember(customers, existingOrders, today) {
        customers.filter { c ->
            val d = existingOrders.find { it.customerName == c.name }?.createdAt
            d == null || sdfDay.format(Date(d)) != today
        }
    }
    val parsed  = rawText.trim().split("\n").filter { it.isNotBlank() }
    val effName = if (useCustomName) customName else selectedCustomer?.name ?: ""

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        windowInsets = WindowInsets.ime, containerColor = Color.White) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("New Order", style = MaterialTheme.typography.headlineMedium.copy(fontFamily = PlayfairFamily))
                    Text("Each line becomes a checkbox", style = MaterialTheme.typography.bodySmall.copy(fontFamily = InterFamily), color = PSRColors.Grey500)
                }
                AnimatedVisibility(visible = parsed.isNotEmpty() && effName.isNotBlank()) {
                    Box(modifier = Modifier.background(PSRColors.Navy600, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Text("${parsed.size} items", style = MaterialTheme.typography.labelMedium.copy(fontFamily = InterFamily), color = Color.White)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))

            // Customer / type name toggle
            Row(modifier = Modifier.fillMaxWidth().background(PSRColors.Grey100, RoundedCornerShape(12.dp)).padding(4.dp)) {
                listOf(false to "Saved Customer", true to "Type Name").forEach { (isCustom, label) ->
                    val sel = useCustomName == isCustom
                    Box(modifier = Modifier.weight(1f).background(if (sel) Color.White else Color.Transparent, RoundedCornerShape(10.dp))
                        .clickable { useCustomName = isCustom }.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                        Text(label, style = MaterialTheme.typography.labelLarge.copy(fontFamily = InterFamily), color = if (sel) PSRColors.Navy600 else PSRColors.Grey500)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            if (useCustomName) {
                OutlinedTextField(value = customName, onValueChange = { customName = it }, label = { Text("Name / Label") },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true,
                    placeholder = { Text("e.g. Uncle Hassan, Stall 3…") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PSRColors.Navy600))
            } else {
                ExposedDropdownMenuBox(expanded = showCustMenu, onExpandedChange = { showCustMenu = it }) {
                    OutlinedTextField(value = selectedCustomer?.name ?: "", onValueChange = {}, readOnly = true,
                        label = { Text("Customer") }, placeholder = { Text("Select…") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(showCustMenu) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PSRColors.Navy600))
                    ExposedDropdownMenu(expanded = showCustMenu, onDismissRequest = { showCustMenu = false }) {
                        if (available.isEmpty()) DropdownMenuItem(text = { Text("All customers have orders", color = PSRColors.Grey500) }, onClick = {})
                        else available.forEach { c ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(c.name)
                                        if (c.company.isNotBlank()) Text(c.company, style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey500)
                                    }
                                },
                                onClick = { selectedCustomer = c; showCustMenu = false })
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(value = rawText, onValueChange = { rawText = it }, label = { Text("Order items") },
                placeholder = { Text("5kg ayam\n3kg ikan merah\n2 dozen telur") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp, max = 260.dp),
                shape = RoundedCornerShape(14.dp), maxLines = 25,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PSRColors.Accent))

            AnimatedVisibility(visible = parsed.isNotEmpty()) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    parsed.take(5).forEach { line ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                            Icon(Icons.Outlined.RadioButtonUnchecked, null, tint = PSRColors.Grey300, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(line.trim(), style = MaterialTheme.typography.bodySmall.copy(fontFamily = InterFamily), color = PSRColors.Grey700)
                        }
                    }
                    if (parsed.size > 5) Text("…and ${parsed.size - 5} more",
                        style = MaterialTheme.typography.labelSmall, color = PSRColors.Grey500, modifier = Modifier.padding(start = 21.dp))
                }
            }
            Spacer(Modifier.height(20.dp))

            Button(onClick = {
                if (effName.isBlank() || parsed.isEmpty()) return@Button
                onSave(CustomerOrder(customerName = effName.trim(), customerId = if (useCustomName) null else selectedCustomer?.id,
                    items = parsed.map { OrderItem(text = it.trim()) }))
            }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PSRColors.Navy600),
                enabled = effName.isNotBlank() && parsed.isNotEmpty()) {
                Icon(Icons.Default.Save, null); Spacer(Modifier.width(8.dp))
                Text("Save Order", style = MaterialTheme.typography.titleMedium.copy(fontFamily = InterFamily))
            }
        }
    }
}

// ─────────────────────────────────────────────
// ORDER EDIT SHEET
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderEditSheet(order: CustomerOrder, customers: List<Customer>, onDismiss: () -> Unit, onSave: (CustomerOrder) -> Unit) {
    var items       by remember { mutableStateOf(order.items) }
    var newItemText by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        windowInsets = WindowInsets.ime, containerColor = Color.White) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Edit Order", style = MaterialTheme.typography.headlineMedium.copy(fontFamily = PlayfairFamily))
                    Text(order.customerName, style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic, fontFamily = InterFamily), color = PSRColors.Accent)
                }
                Button(onClick = { onSave(order.copy(items = items)) }, shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PSRColors.Navy600)) { Text("Done") }
            }
            Spacer(Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items.forEachIndexed { idx, item ->
                    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(if (item.isChecked) PSRColors.Grey50 else PSRColors.Surface)
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(if (item.isChecked) Icons.Default.CheckCircle else Icons.Outlined.RadioButtonUnchecked, null,
                            tint = if (item.isChecked) PSRColors.Success else PSRColors.Grey300, modifier = Modifier.size(18.dp))
                        Text(item.text, modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = InterFamily),
                            color = if (item.isChecked) PSRColors.Grey400 else PSRColors.Navy900)
                        IconButton(onClick = { items = items.toMutableList().also { it.removeAt(idx) } }, modifier = Modifier.size(26.dp)) {
                            Icon(Icons.Default.Close, null, tint = PSRColors.Error.copy(0.7f), modifier = Modifier.size(15.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = PSRColors.Divider)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = newItemText, onValueChange = { newItemText = it },
                    placeholder = { Text("Add item…") }, modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp), singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PSRColors.Accent))
                IconButton(onClick = {
                    if (newItemText.isNotBlank()) { items = items + OrderItem(text = newItemText.trim()); newItemText = "" }
                }, modifier = Modifier.size(48.dp).background(PSRColors.Navy600, RoundedCornerShape(12.dp))) {
                    Icon(Icons.Default.Add, null, tint = Color.White)
                }
            }
        }
    }
}

// ═════════════════════════════════════════════
// TAB 1 — TASKS
// ═════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksTab(viewModel: PSRViewModel) {
    val activeTasks  by viewModel.activeTasks.collectAsState()
    val doneTasks    by viewModel.doneTasks.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }
    var editingTask  by remember { mutableStateOf<BusinessTask?>(null) }
    var showDone     by remember { mutableStateOf(false) }
    var filterCat    by remember { mutableStateOf<TaskCategory?>(null) }

    val urgentCount  = activeTasks.count { it.priority == TaskPriority.URGENT }
    val overdueCount = activeTasks.count { it.isOverdue }
    val dueTodayCount= activeTasks.count { it.isDueToday && !it.isOverdue }

    val filtered = remember(activeTasks, filterCat) {
        if (filterCat == null) activeTasks else activeTasks.filter { it.category == filterCat }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Alert banner
            AnimatedVisibility(visible = overdueCount > 0 || urgentCount > 0) {
                Row(modifier = Modifier.fillMaxWidth().background(PSRColors.Error.copy(0.09f))
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Warning, null, tint = PSRColors.Error, modifier = Modifier.size(14.dp))
                    Text(buildString {
                        if (overdueCount > 0) append("$overdueCount overdue")
                        if (overdueCount > 0 && urgentCount > 0) append(" · ")
                        if (urgentCount > 0) append("$urgentCount urgent")
                        if (dueTodayCount > 0) append(" · $dueTodayCount due today")
                    }, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = PSRColors.Error)
                }
            }

            // Stats + done toggle
            Row(modifier = Modifier.fillMaxWidth().background(PSRColors.Navy600.copy(0.04f)).padding(horizontal = 18.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(PSRColors.Navy600, CircleShape))
                Text("${activeTasks.size} active", style = MaterialTheme.typography.labelMedium.copy(fontFamily = InterFamily), color = PSRColors.Grey600)
                Box(modifier = Modifier.size(8.dp).background(PSRColors.Success, CircleShape))
                Text("${doneTasks.size} done", style = MaterialTheme.typography.labelMedium.copy(fontFamily = InterFamily), color = PSRColors.Grey600)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { showDone = !showDone }, contentPadding = PaddingValues(4.dp)) {
                    Text(if (showDone) "Hide Done" else "Show Done",
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = InterFamily), color = PSRColors.Grey400)
                }
            }

            // Category filters
            LazyRow(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                contentPadding = PaddingValues(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    FilterChip(selected = filterCat == null, onClick = { filterCat = null },
                        label = { Text("All", style = MaterialTheme.typography.labelSmall) }, shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PSRColors.Navy600, selectedLabelColor = Color.White))
                }
                items(TaskCategory.entries) { cat ->
                    FilterChip(selected = filterCat == cat, onClick = { filterCat = if (filterCat == cat) null else cat },
                        label = { Text("${cat.emoji} ${cat.displayName}", style = MaterialTheme.typography.labelSmall) },
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = cat.tcvColor, selectedLabelColor = Color.White,
                            containerColor = cat.tcvColor.copy(0.08f), labelColor = cat.tcvColor))
                }
            }

            // List
            if (filtered.isEmpty() && !showDone) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(icon = Icons.Outlined.TaskAlt, title = "No tasks yet",
                        subtitle = "Add tasks like \"Post TikTok\", \"Boost ad\", \"Restock Ayam\"",
                        actionLabel = "Add Task", onAction = { showAddSheet = true })
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 110.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {

                    if (filtered.isNotEmpty()) {
                        item { Text("Active (${filtered.size})", style = MaterialTheme.typography.labelMedium.copy(fontFamily = InterFamily), color = PSRColors.Grey400, modifier = Modifier.padding(horizontal = 4.dp)) }
                        items(filtered, key = { it.id }) { task ->
                            TaskCard(task = task, viewModel = viewModel, onEdit = { editingTask = task })
                        }
                    }
                    if (showDone && doneTasks.isNotEmpty()) {
                        item { Spacer(Modifier.height(4.dp)); Text("Completed (${doneTasks.size})", style = MaterialTheme.typography.labelMedium.copy(fontFamily = InterFamily), color = PSRColors.Grey400, modifier = Modifier.padding(horizontal = 4.dp)) }
                        items(doneTasks, key = { "d_${it.id}" }) { task ->
                            DoneTaskRow(task = task, viewModel = viewModel)
                        }
                    }
                }
            }
        }

        // FAB
        ExtendedFloatingActionButton(onClick = { showAddSheet = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 16.dp),
            containerColor = PSRColors.Accent, contentColor = Color.White,
            icon = { Icon(Icons.Default.Add, null) },
            text = { Text("Add Task", style = MaterialTheme.typography.labelLarge.copy(fontFamily = InterFamily)) })
    }

    if (showAddSheet) AddEditTaskSheet(task = null, onDismiss = { showAddSheet = false }, onSave = { viewModel.insertTask(it); showAddSheet = false })
    if (editingTask != null) AddEditTaskSheet(task = editingTask, onDismiss = { editingTask = null }, onSave = { viewModel.updateTask(it); editingTask = null })
}

// ─────────────────────────────────────────────
// TASK CARD
// ─────────────────────────────────────────────

@Composable
fun TaskCard(task: BusinessTask, viewModel: PSRViewModel, onEdit: () -> Unit) {
    val dueDateStr = task.dueDate?.let { d ->
        when { task.isOverdue -> "Overdue · ${SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(d))}"; task.isDueToday -> "Due today"; else -> SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(d)) }
    }
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = task.priority.tcvCardBg),
        elevation = CardDefaults.cardElevation(if (task.isPinned) 3.dp else 1.dp)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Left accent dot
                Box(modifier = Modifier.padding(top = 4.dp).width(4.dp).height(40.dp)
                    .background(task.priority.tcvColor, RoundedCornerShape(2.dp)))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (task.isPinned) Icon(Icons.Default.PushPin, null, tint = PSRColors.Accent, modifier = Modifier.size(11.dp))
                        Text(task.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontFamily = InterFamily), color = PSRColors.Navy900)
                    }
                    if (task.notes.isNotBlank()) {
                        Text(task.notes, style = MaterialTheme.typography.bodySmall.copy(fontFamily = InterFamily), color = PSRColors.Grey600, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                        TaskBadge(task.category.emoji + " " + task.category.displayName, task.category.tcvColor)
                        TaskBadge(task.priority.label, task.priority.tcvColor)
                        if (dueDateStr != null) Text(dueDateStr,
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = InterFamily, fontSize = 9.sp),
                            color = if (task.isOverdue) PSRColors.Error else if (task.isDueToday) PSRColors.Warning else PSRColors.Grey400)
                    }
                }

                // Actions column
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    IconButton(onClick = { viewModel.completeTask(task) }, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Outlined.CheckCircle, null, tint = PSRColors.Success, modifier = Modifier.size(24.dp))
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, null, tint = PSRColors.Grey400, modifier = Modifier.size(14.dp))
                    }
                    IconButton(onClick = { viewModel.toggleTaskPin(task) }, modifier = Modifier.size(28.dp)) {
                        Icon(if (task.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                            null, tint = if (task.isPinned) PSRColors.Accent else PSRColors.Grey300, modifier = Modifier.size(14.dp))
                    }
                    IconButton(onClick = { viewModel.deleteTask(task) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.DeleteOutline, null, tint = PSRColors.Error.copy(0.5f), modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun TaskBadge(label: String, color: Color) {
    Box(modifier = Modifier.background(color.copy(0.1f), RoundedCornerShape(6.dp)).padding(horizontal = 5.dp, vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontFamily = InterFamily, fontSize = 9.sp), color = color)
    }
}

// ─────────────────────────────────────────────
// DONE TASK ROW
// ─────────────────────────────────────────────

@Composable
fun DoneTaskRow(task: BusinessTask, viewModel: PSRViewModel) {
    val completedStr = remember(task.completedAt) { task.completedAt?.let { SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(it)) } } ?: ""
    Row(modifier = Modifier.fillMaxWidth().alpha(0.55f).clip(RoundedCornerShape(12.dp))
        .background(PSRColors.Grey50).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(Icons.Default.CheckCircle, null, tint = PSRColors.Success.copy(0.5f), modifier = Modifier.size(18.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(task.title, style = MaterialTheme.typography.bodySmall.copy(fontFamily = InterFamily, textDecoration = TextDecoration.LineThrough), color = PSRColors.Grey500, maxLines = 1)
            if (completedStr.isNotBlank()) Text("Done $completedStr", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = PSRColors.Grey400)
        }
        IconButton(onClick = { viewModel.uncompleteTask(task) }, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Undo, null, tint = PSRColors.Grey400, modifier = Modifier.size(15.dp))
        }
        IconButton(onClick = { viewModel.deleteTask(task) }, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Close, null, tint = PSRColors.Error.copy(0.4f), modifier = Modifier.size(14.dp))
        }
    }
}

// ─────────────────────────────────────────────
// ADD / EDIT TASK SHEET
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTaskSheet(task: BusinessTask?, onDismiss: () -> Unit, onSave: (BusinessTask) -> Unit) {
    var title    by remember { mutableStateOf(task?.title ?: "") }
    var notes    by remember { mutableStateOf(task?.notes ?: "") }
    var priority by remember { mutableStateOf(task?.priority ?: TaskPriority.MEDIUM) }
    var category by remember { mutableStateOf(task?.category ?: TaskCategory.OTHER) }
    var dueDate  by remember { mutableStateOf(task?.dueDate) }
    var showDatePicker by remember { mutableStateOf(false) }
    val isEdit = task != null

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        windowInsets = WindowInsets.ime, containerColor = Color.White) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 36.dp).verticalScroll(rememberScrollState())) {
            Text(if (isEdit) "Edit Task" else "New Task", style = MaterialTheme.typography.headlineMedium.copy(fontFamily = PlayfairFamily))
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Task *") },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true,
                placeholder = { Text("Post TikTok, Boost ad, Restock, Follow up…") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PSRColors.Navy600))
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PSRColors.Navy600))
            Spacer(Modifier.height(14.dp))

            // Priority picker
            Text("Priority", style = MaterialTheme.typography.labelMedium.copy(fontFamily = InterFamily), color = PSRColors.Grey600)
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TaskPriority.entries.forEach { p ->
                    val sel = priority == p
                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                        .background(if (sel) p.tcvColor else p.tcvColor.copy(0.08f))
                        .clickable { priority = p }.padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center) {
                        Text(p.label, style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = InterFamily, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal),
                            color = if (sel) Color.White else p.tcvColor)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))

            // Category picker
            Text("Category", style = MaterialTheme.typography.labelMedium.copy(fontFamily = InterFamily), color = PSRColors.Grey600)
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(TaskCategory.entries) { cat ->
                    val sel = category == cat
                    FilterChip(selected = sel, onClick = { category = cat },
                        label = { Text("${cat.emoji} ${cat.displayName}", style = MaterialTheme.typography.labelSmall.copy(fontFamily = InterFamily)) },
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = cat.tcvColor, selectedLabelColor = Color.White,
                            containerColor = cat.tcvColor.copy(0.08f), labelColor = cat.tcvColor))
                }
            }
            Spacer(Modifier.height(14.dp))

            // Due date
            Text("Due Date", style = MaterialTheme.typography.labelMedium.copy(fontFamily = InterFamily), color = PSRColors.Grey600)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { showDatePicker = true }, shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(remember(dueDate) { dueDate?.let { SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(it)) } ?: "No due date" },
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = InterFamily))
                }
                if (dueDate != null) IconButton(onClick = { dueDate = null }) { Icon(Icons.Default.Clear, null, tint = PSRColors.Grey400) }
            }
            Spacer(Modifier.height(22.dp))

            Button(onClick = {
                if (title.isBlank()) return@Button
                val t = (task ?: BusinessTask(title = "")).copy(title = title.trim(), notes = notes.trim(),
                    priority = priority, category = category, dueDate = dueDate, status = task?.status ?: TaskStatus.TODO)
                onSave(t)
            }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PSRColors.Navy600), enabled = title.isNotBlank()) {
                Icon(Icons.Default.Save, null); Spacer(Modifier.width(8.dp))
                Text(if (isEdit) "Save Changes" else "Add Task", style = MaterialTheme.typography.titleMedium.copy(fontFamily = InterFamily))
            }
        }
    }

    if (showDatePicker) {
        val dps = rememberDatePickerState(initialSelectedDateMillis = dueDate ?: System.currentTimeMillis())
        DatePickerDialog(onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { dueDate = dps.selectedDateMillis; showDatePicker = false }) { Text("OK") } }
        ) { DatePicker(state = dps) }
    }
}

// ─────────────────────────────────────────────
// WRAPPER
// ─────────────────────────────────────────────

@Composable
fun OrderScreenWithFab(viewModel: PSRViewModel, fabTrigger: Int) {
    val orders    by viewModel.activeOrders.collectAsState()
    val customers by viewModel.customers.collectAsState()
    var showSheet by remember { mutableStateOf(false) }
    LaunchedEffect(fabTrigger) { if (fabTrigger > 0) showSheet = true }
    OrderScreen(viewModel = viewModel, onAddOrderClick = { showSheet = true })
    if (showSheet) AddOrderSheet(customers = customers, existingOrders = orders,
        onDismiss = { showSheet = false }, onSave = { viewModel.saveOrder(it); showSheet = false })
}

// ─────────────────────────────────────────────
// EXTENSION HELPERS
// ─────────────────────────────────────────────

val TaskPriority.tcvColor get() = when (this) {
    TaskPriority.LOW    -> Color(0xFF78909C)
    TaskPriority.MEDIUM -> Color(0xFF1E88E5)
    TaskPriority.HIGH   -> Color(0xFFFF7043)
    TaskPriority.URGENT -> Color(0xFFE53935)
}
val TaskPriority.tcvCardBg get() = when (this) {
    TaskPriority.URGENT -> Color(0xFFFFF8F8)
    TaskPriority.HIGH   -> Color(0xFFFFFBF8)
    else                -> Color.White
}
val TaskPriority.label get() = when (this) {
    TaskPriority.LOW    -> "Low"
    TaskPriority.MEDIUM -> "Medium"
    TaskPriority.HIGH   -> "High"
    TaskPriority.URGENT -> "🔥 Urgent"
}
val TaskCategory.displayName get() = when (this) {
    TaskCategory.MARKETING    -> "Marketing"
    TaskCategory.SOCIAL_MEDIA -> "Social"
    TaskCategory.OPERATIONS   -> "Ops"
    TaskCategory.FINANCE      -> "Finance"
    TaskCategory.GROWTH       -> "Growth"
    TaskCategory.OTHER        -> "Other"
}
val TaskCategory.emoji get() = when (this) {
    TaskCategory.MARKETING    -> "📢"
    TaskCategory.SOCIAL_MEDIA -> "📱"
    TaskCategory.OPERATIONS   -> "⚙️"
    TaskCategory.FINANCE      -> "💰"
    TaskCategory.GROWTH       -> "🚀"
    TaskCategory.OTHER        -> "📌"
}
val TaskCategory.tcvColor get() = when (this) {
    TaskCategory.MARKETING    -> Color(0xFF7B1FA2)
    TaskCategory.SOCIAL_MEDIA -> Color(0xFF0288D1)
    TaskCategory.OPERATIONS   -> Color(0xFF00796B)
    TaskCategory.FINANCE      -> Color(0xFF2E7D32)
    TaskCategory.GROWTH       -> Color(0xFFE65100)
    TaskCategory.OTHER        -> Color(0xFF546E7A)
}

// ═════════════════════════════════════════════
// TAB — ONLINE ORDERS (Shopee / TikTok)
// ═════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineOrdersTab(viewModel: PSRViewModel) {
    val activeOrders      by viewModel.activeOnlineOrders.collectAsState()
    val allOrders         by viewModel.allOnlineOrders.collectAsState()
    val products          by viewModel.onlineProducts.collectAsState()
    val allOnlineProducts by viewModel.allOnlineProducts.collectAsState()
    val allStockItems     by viewModel.allStockItems.collectAsState()

    var showNewOrder       by remember { mutableStateOf(false) }
    var showProductManager by remember { mutableStateOf(false) }
    var settlingOrder      by remember { mutableStateOf<OnlineOrder?>(null) }
    var viewingOrder       by remember { mutableStateOf<OnlineOrder?>(null) }
    var showHistory        by remember { mutableStateOf(false) }

    val displayOrders = if (showHistory) allOrders else activeOrders
    val sdf = remember { SimpleDateFormat("dd/MM", Locale.getDefault()) }
    val sdfFull = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    Box(modifier = Modifier.fillMaxSize().background(PSRColors.Surface)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Sub-header ──────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth()
                .background(PSRColors.Navy800)
                .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !showHistory, onClick = { showHistory = false },
                    label = { Text("Active", fontSize = 11.sp) },
                    shape = RoundedCornerShape(8.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PSRColors.Accent,
                        selectedLabelColor = Color.White)
                )
                FilterChip(
                    selected = showHistory, onClick = { showHistory = true },
                    label = { Text("History", fontSize = 11.sp) },
                    shape = RoundedCornerShape(8.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PSRColors.Accent,
                        selectedLabelColor = Color.White)
                )
                Spacer(Modifier.weight(1f))
                // Products manager button
                IconButton(onClick = { showProductManager = true }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Inventory2, "Manage products",
                        tint = PSRColors.White.copy(0.7f), modifier = Modifier.size(20.dp))
                }
            }

            if (displayOrders.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (products.isEmpty()) "🛒" else "📦",
                            style = MaterialTheme.typography.displayMedium)
                        Text(
                            if (products.isEmpty()) "Set up your online products first"
                            else if (showHistory) "No completed orders yet"
                            else "No active orders",
                            style = MaterialTheme.typography.bodyMedium, color = PSRColors.Grey500)
                        if (products.isEmpty()) {
                            OutlinedButton(onClick = { showProductManager = true }, shape = RoundedCornerShape(10.dp)) {
                                Text("Set Up Products")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp, 10.dp, 12.dp, 90.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(displayOrders, key = { it.id }) { order ->
                        OnlineOrderCard(
                            order = order,
                            sdf = sdf,
                            onSettle = { settlingOrder = order },
                            onView   = { viewingOrder = order },
                            onMarkShipped = {
                                viewModel.updateOnlineOrder(order.copy(
                                    status    = OnlineOrderStatus.SHIPPED,
                                    shippedAt = System.currentTimeMillis()
                                ))
                            },
                            onDelete = { viewModel.deleteOnlineOrder(order) }
                        )
                    }
                }
            }
        }

        // ── FAB ─────────────────────────────────────────────────────
        if (!showHistory) {
            ExtendedFloatingActionButton(
                onClick = { showNewOrder = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 16.dp),
                containerColor = PSRColors.Accent, contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("New Order", style = MaterialTheme.typography.labelLarge) }
            )
        }
    }

    if (showNewOrder) {
        NewOnlineOrderSheet(
            products  = products,
            onDismiss = { showNewOrder = false },
            onSave    = { order -> viewModel.placeOnlineOrder(order); showNewOrder = false }
        )
    }
    if (showProductManager) {
        OnlineProductManagerSheet(
            products  = allOnlineProducts,
            allStock  = allStockItems,
            onDismiss = { showProductManager = false },
            onSave    = { p -> viewModel.saveOnlineProduct(p) },
            onDelete  = { p -> viewModel.deleteOnlineProduct(p) }
        )
    }
    settlingOrder?.let { order ->
        SettleOrderSheet(
            order     = order,
            onDismiss = { settlingOrder = null },
            onSettle  = { fee, subsidy, extra, note, account ->
                viewModel.settleOnlineOrder(order, fee, subsidy, extra, note, account)
                settlingOrder = null
            }
        )
    }
    viewingOrder?.let { order ->
        OnlineOrderDetailSheet(order = order, onDismiss = { viewingOrder = null })
    }
}

// ─────────────────────────────────────────────
// ORDER CARD
// ─────────────────────────────────────────────

@Composable
fun OnlineOrderCard(
    order: OnlineOrder,
    sdf: SimpleDateFormat,
    onSettle: () -> Unit,
    onView: () -> Unit,
    onMarkShipped: () -> Unit,
    onDelete: () -> Unit
) {
    val platformColor = when (order.platform) {
        OnlinePlatform.SHOPEE -> Color(0xFFEE4D2D)
        OnlinePlatform.TIKTOK -> Color(0xFF010101)
        OnlinePlatform.OTHER  -> PSRColors.Navy600
    }
    val platformLabel = when (order.platform) {
        OnlinePlatform.SHOPEE -> "Shopee"
        OnlinePlatform.TIKTOK -> "TikTok"
        OnlinePlatform.OTHER  -> "Online"
    }
    val statusColor = when (order.status) {
        OnlineOrderStatus.PENDING  -> PSRColors.Warning
        OnlineOrderStatus.PACKED   -> PSRColors.Accent
        OnlineOrderStatus.SHIPPED  -> PSRColors.Navy600
        OnlineOrderStatus.SETTLED  -> PSRColors.Success
        OnlineOrderStatus.CANCELLED -> PSRColors.Grey400
    }
    var expanded by remember { mutableStateOf(false) }
    val deadlineStr = remember(order.shipBefore) {
        order.shipBefore?.let { "Ship by ${sdf.format(Date(it))}" } ?: ""
    }

    Card(
        onClick = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PSRColors.Card),
        elevation = CardDefaults.cardElevation(1.dp),
        border = if (order.isOverdue) BorderStroke(2.dp, PSRColors.Error) else null
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Platform badge
                Box(modifier = Modifier
                    .background(platformColor.copy(0.12f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)) {
                    Text(platformLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = platformColor)
                }
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(order.customerName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = PSRColors.Navy900)
                    if (order.orderRef.isNotBlank())
                        Text(order.orderRef, style = MaterialTheme.typography.labelSmall, color = PSRColors.Grey400)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("RM ${"%.2f".format(order.subtotal)}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = PSRColors.Navy900)
                    Box(modifier = Modifier
                        .background(statusColor.copy(0.12f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text(order.status.name,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                            color = statusColor)
                    }
                }
            }

            // Items preview
            Spacer(Modifier.height(8.dp))
            Text(
                order.items.joinToString("  ·  ") { "${it.productName} ×${"%.0f".format(it.qty)}" },
                style = MaterialTheme.typography.bodySmall,
                color = PSRColors.Grey600,
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )

            // Deadline
            if (deadlineStr.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Schedule, null,
                        tint = if (order.isOverdue) PSRColors.Error else PSRColors.Grey400,
                        modifier = Modifier.size(12.dp))
                    Text(deadlineStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (order.isOverdue) PSRColors.Error else PSRColors.Grey400)
                    if (order.isOverdue) Text("OVERDUE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = PSRColors.Error)
                }
            }

            // Expanded actions
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = PSRColors.Divider)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onView, modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(0.dp)) {
                            Icon(Icons.Default.Visibility, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("View", style = MaterialTheme.typography.labelMedium)
                        }
                        if (order.status == OnlineOrderStatus.PENDING || order.status == OnlineOrderStatus.PACKED) {
                            OutlinedButton(onClick = onMarkShipped, modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(0.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PSRColors.Navy600)) {
                                Icon(Icons.Default.LocalShipping, null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Shipped", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        if (order.status == OnlineOrderStatus.SHIPPED || order.status == OnlineOrderStatus.PENDING) {
                            Button(onClick = onSettle, modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(0.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PSRColors.Success)) {
                                Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Settle", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.DeleteOutline, null, tint = PSRColors.Error.copy(0.6f), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// NEW ORDER SHEET
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewOnlineOrderSheet(
    products: List<OnlineProduct>,
    onDismiss: () -> Unit,
    onSave: (OnlineOrder) -> Unit
) {
    var platform       by remember { mutableStateOf(OnlinePlatform.SHOPEE) }
    var customerName   by remember { mutableStateOf("") }
    var orderRef       by remember { mutableStateOf("") }
    var hasDueDate     by remember { mutableStateOf(false) }
    var shipBefore     by remember { mutableLongStateOf(System.currentTimeMillis() + 3 * 24 * 60 * 60 * 1000L) }
    var showDatePicker by remember { mutableStateOf(false) }
    var notes          by remember { mutableStateOf("") }
    // Selected items: productId -> qty
    var selectedItems  by remember { mutableStateOf<Map<Long, Double>>(emptyMap()) }
    // Price overrides per product
    var priceOverrides by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }

    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val platformColor = when (platform) {
        OnlinePlatform.SHOPEE -> Color(0xFFEE4D2D)
        OnlinePlatform.TIKTOK -> Color(0xFF010101)
        OnlinePlatform.OTHER  -> PSRColors.Navy600
    }

    val orderItems = remember(selectedItems, priceOverrides, products) {
        selectedItems.mapNotNull { (pid, qty) ->
            val p = products.find { it.id == pid } ?: return@mapNotNull null
            val sellPrice = priceOverrides[pid]?.toDoubleOrNull() ?: p.defaultSellPrice
            OnlineOrderItem(productId = pid, productName = p.name, qty = qty,
                unit = p.unit, sellPriceEach = sellPrice, costPriceEach = p.avgCostPrice,
                weightGrams = p.weightGrams)
        }
    }
    val subtotal = orderItems.sumOf { it.lineTotal }

    ModalBottomSheet(onDismissRequest = onDismiss, modifier = Modifier.fillMaxHeight(0.94f)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(modifier = Modifier.fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(PSRColors.Navy800, PSRColors.Navy600)))
                .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("New Online Order",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White)
                    if (orderItems.isNotEmpty())
                        Text("RM ${"%.2f".format(subtotal)} · ${orderItems.size} item${if (orderItems.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.7f))
                }
                TextButton(onClick = {
                    if (customerName.isBlank() || orderItems.isEmpty()) return@TextButton
                    onSave(OnlineOrder(
                        platform = platform, customerName = customerName.trim(),
                        orderRef = orderRef.trim(), items = orderItems,
                        shipBefore = if (hasDueDate) shipBefore else null,
                        notes = notes.trim()
                    ))
                }, enabled = customerName.isNotBlank() && orderItems.isNotEmpty()) {
                    Text("Save", color = if (customerName.isNotBlank() && orderItems.isNotEmpty()) PSRColors.Gold else Color.White.copy(0.3f),
                        fontWeight = FontWeight.Bold)
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp, 14.dp, 16.dp, 40.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {

                // Platform selector
                item {
                    Text("Platform", style = MaterialTheme.typography.labelMedium, color = PSRColors.Grey600)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(OnlinePlatform.SHOPEE to "🛍 Shopee",
                               OnlinePlatform.TIKTOK to "🎵 TikTok",
                               OnlinePlatform.OTHER  to "🌐 Other").forEach { (p, label) ->
                            val sel = platform == p
                            Surface(onClick = { platform = p }, modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = if (sel) platformColor.copy(0.12f) else PSRColors.Grey50,
                                border = BorderStroke(if (sel) 2.dp else 1.dp, if (sel) platformColor else PSRColors.Grey200)) {
                                Text(label, style = MaterialTheme.typography.labelLarge.copy(fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal),
                                    color = if (sel) platformColor else PSRColors.Grey500,
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        }
                    }
                }

                // Customer name + order ref
                item {
                    OutlinedTextField(value = customerName, onValueChange = { customerName = it },
                        label = { Text("Customer Name") }, modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp), singleLine = true,
                        placeholder = { Text("e.g. Nurul / Sarah / Order #1234") })
                }
                item {
                    OutlinedTextField(value = orderRef, onValueChange = { orderRef = it },
                        label = { Text("Platform Order ID (optional)") }, modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp), singleLine = true,
                        placeholder = { Text("e.g. 240311ABCD1234") })
                }

                // Ship before toggle
                item {
                    Row(modifier = Modifier.fillMaxWidth()
                        .background(PSRColors.Grey50, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Must ship before", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                            if (hasDueDate)
                                Text(remember(shipBefore) { sdf.format(Date(shipBefore)) },
                                    style = MaterialTheme.typography.bodySmall, color = PSRColors.Navy600)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (hasDueDate)
                                OutlinedButton(onClick = { showDatePicker = true }, shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)) {
                                    Text("Change", style = MaterialTheme.typography.labelSmall)
                                }
                            Switch(checked = hasDueDate, onCheckedChange = { hasDueDate = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = PSRColors.Accent, checkedTrackColor = PSRColors.AccentDim))
                        }
                    }
                }

                // Product picker
                item {
                    Text("Items", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = PSRColors.Navy700)
                }
                if (products.isEmpty()) {
                    item {
                        Text("No products set up yet. Add products first using the inventory icon above.",
                            style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey500)
                    }
                } else {
                    items(products, key = { "prod_${it.id}" }) { product ->
                        val qty = selectedItems[product.id] ?: 0.0
                        val isSelected = qty > 0
                        val priceStr = priceOverrides[product.id] ?: "%.2f".format(product.defaultSellPrice)

                        Surface(
                            onClick = {
                                selectedItems = if (isSelected)
                                    selectedItems - product.id
                                else
                                    selectedItems + (product.id to 1.0)
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) PSRColors.Navy600.copy(0.06f) else PSRColors.Card,
                            border = BorderStroke(if (isSelected) 2.dp else 1.dp,
                                if (isSelected) PSRColors.Navy600.copy(0.3f) else PSRColors.Grey100)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(product.name,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = PSRColors.Navy900)
                                        val meta = buildString {
                                            if (product.sizeLabel.isNotBlank()) append(product.sizeLabel)
                                            if (product.weightGrams > 0) {
                                                if (isNotEmpty()) append(" · ")
                                                append("${product.weightGrams.toInt()}g")
                                            }
                                        }
                                        if (meta.isNotBlank())
                                            Text(meta, style = MaterialTheme.typography.labelSmall, color = PSRColors.Grey500)
                                    }
                                    Text("RM ${"%.2f".format(product.defaultSellPrice)}",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = PSRColors.Navy600)
                                }
                                if (isSelected) {
                                    Spacer(Modifier.height(10.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        // Qty stepper
                                        Text("Qty:", style = MaterialTheme.typography.labelMedium, color = PSRColors.Grey600)
                                        IconButton(onClick = {
                                            val newQty = qty - 1
                                            selectedItems = if (newQty <= 0) selectedItems - product.id
                                                           else selectedItems + (product.id to newQty)
                                        }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.Remove, null, modifier = Modifier.size(18.dp))
                                        }
                                        Text("${"%.0f".format(qty)} ${product.unit}",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            modifier = Modifier.widthIn(min = 48.dp),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                        IconButton(onClick = {
                                            selectedItems = selectedItems + (product.id to qty + 1)
                                        }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                                        }
                                        Spacer(Modifier.weight(1f))
                                        // Price override
                                        OutlinedTextField(
                                            value = priceStr,
                                            onValueChange = { priceOverrides = priceOverrides + (product.id to it) },
                                            label = { Text("RM each", style = MaterialTheme.typography.labelSmall) },
                                            modifier = Modifier.width(100.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                                            textStyle = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Notes
                item {
                    OutlinedTextField(value = notes, onValueChange = { notes = it },
                        label = { Text("Notes (optional)") }, modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp), minLines = 1, maxLines = 3)
                }

                // Summary
                if (orderItems.isNotEmpty()) {
                    item {
                        Surface(color = PSRColors.Navy600.copy(0.06f), shape = RoundedCornerShape(12.dp)) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Order Summary", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = PSRColors.Navy700)
                                orderItems.forEach { item ->
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        Text("${item.productName} ×${"%.0f".format(item.qty)}", modifier = Modifier.weight(1f),
                                            style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey600)
                                        Text("RM ${"%.2f".format(item.lineTotal)}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                            color = PSRColors.Navy900)
                                    }
                                }
                                HorizontalDivider(color = PSRColors.Divider, modifier = Modifier.padding(vertical = 4.dp))
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Text("Total", modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                                    Text("RM ${"%.2f".format(subtotal)}",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                                        color = PSRColors.Navy600)
                                }
                            }
                        }
                    }
                }

                // Save button
                item {
                    Button(
                        onClick = {
                            if (customerName.isBlank() || orderItems.isEmpty()) return@Button
                            onSave(OnlineOrder(
                                platform = platform, customerName = customerName.trim(),
                                orderRef = orderRef.trim(), items = orderItems,
                                shipBefore = if (hasDueDate) shipBefore else null,
                                notes = notes.trim()
                            ))
                        },
                        enabled = customerName.isNotBlank() && orderItems.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PSRColors.Accent)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Create Order", style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val dps = rememberDatePickerState(initialSelectedDateMillis = shipBefore)
        DatePickerDialog(onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { dps.selectedDateMillis?.let { shipBefore = it }; showDatePicker = false }) { Text("OK") } }
        ) { DatePicker(state = dps) }
    }
}

// ─────────────────────────────────────────────
// SETTLE ORDER SHEET
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettleOrderSheet(
    order: OnlineOrder,
    onDismiss: () -> Unit,
    onSettle: (fee: Double, subsidy: Double, extra: Double, note: String, account: FinancialAccount) -> Unit
) {
    // Pre-fill platform fee from first product's fee (if available)
    var platformFeeStr  by remember { mutableStateOf("0.00") }
    var subsidyStr      by remember { mutableStateOf("0.00") }
    var extraCostStr    by remember { mutableStateOf("0.00") }
    var extraNote       by remember { mutableStateOf("") }
    var account         by remember { mutableStateOf(FinancialAccount.MAYBANK) }

    val fee      = platformFeeStr.toDoubleOrNull() ?: 0.0
    val subsidy  = subsidyStr.toDoubleOrNull() ?: 0.0
    val extra    = extraCostStr.toDoubleOrNull() ?: 0.0
    val netReceived = order.subtotal - fee - extra + subsidy
    val netProfit   = order.subtotal - order.totalCost - fee - extra + subsidy

    val platformLabel = when (order.platform) {
        OnlinePlatform.SHOPEE -> "Shopee"
        OnlinePlatform.TIKTOK -> "TikTok"
        OnlinePlatform.OTHER  -> "Platform"
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White,
        modifier = Modifier.fillMaxHeight(0.92f)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Column(modifier = Modifier.fillMaxWidth()
                .background(Brush.verticalGradient(listOf(PSRColors.Navy600, PSRColors.Navy700)))
                .padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text("Settle Order", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                Text("${order.customerName} · RM ${"%.2f".format(order.subtotal)}",
                    style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.7f))
            }

            LazyColumn(modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 40.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // Items recap
                item {
                    Text("Items Sold", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = PSRColors.Navy700)
                }
                items(order.items, key = { "si_${it.productId}_${it.productName}" }) { item ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("${item.productName} ×${"%.0f".format(item.qty)} ${item.unit}",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey700)
                        Text("RM ${"%.2f".format(item.lineTotal)}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
                    }
                }

                // Platform fee section
                item {
                    Text("$platformLabel Fees & Adjustments",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = PSRColors.Navy700)
                    Spacer(Modifier.height(2.dp))
                    Text("Edit if the actual fee differs from expected. This is what platform deducted.",
                        style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey500)
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("$platformLabel Fee (RM)", style = MaterialTheme.typography.labelSmall, color = PSRColors.Grey600)
                            Spacer(Modifier.height(4.dp))
                            RMInputField("Fee", platformFeeStr, { platformFeeStr = it }, Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Shipping Subsidy (RM)", style = MaterialTheme.typography.labelSmall, color = PSRColors.Grey600)
                            Spacer(Modifier.height(4.dp))
                            RMInputField("Subsidy", subsidyStr, { subsidyStr = it }, Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal))
                        }
                    }
                }
                item {
                    RMInputField("Extra costs (torn pack, late penalty, etc)", extraCostStr, { extraCostStr = it }, Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal))
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(value = extraNote, onValueChange = { extraNote = it },
                        label = { Text("Extra cost note") }, modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp), singleLine = true,
                        placeholder = { Text("e.g. Torn packaging ×2") })
                }

                // Account selector
                item {
                    Text("Money received into", style = MaterialTheme.typography.labelMedium, color = PSRColors.Grey600)
                    Spacer(Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth()
                        .background(PSRColors.Grey100, RoundedCornerShape(12.dp)).padding(4.dp)) {
                        FinancialAccount.entries.forEach { acc ->
                            val sel = account == acc
                            Box(modifier = Modifier.weight(1f)
                                .background(if (sel) Color.White else Color.Transparent, RoundedCornerShape(10.dp))
                                .clickable { account = acc }.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center) {
                                Text(acc.name, style = MaterialTheme.typography.labelLarge,
                                    color = if (sel) PSRColors.Navy600 else PSRColors.Grey500)
                            }
                        }
                    }
                }

                // Net summary card
                item {
                    Surface(color = if (netProfit >= 0) PSRColors.Success.copy(0.06f) else PSRColors.Error.copy(0.06f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (netProfit >= 0) PSRColors.Success.copy(0.2f) else PSRColors.Error.copy(0.2f))) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text("Sales", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey600)
                                Text("RM ${"%.2f".format(order.subtotal)}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
                            }
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text("Cost of goods", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey600)
                                Text("−RM ${"%.2f".format(order.totalCost)}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), color = PSRColors.Error)
                            }
                            if (fee > 0) Row(modifier = Modifier.fillMaxWidth()) {
                                Text("$platformLabel fee", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey600)
                                Text("−RM ${"%.2f".format(fee)}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), color = PSRColors.Error)
                            }
                            if (subsidy > 0) Row(modifier = Modifier.fillMaxWidth()) {
                                Text("Shipping subsidy", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey600)
                                Text("+RM ${"%.2f".format(subsidy)}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), color = PSRColors.Success)
                            }
                            if (extra > 0) Row(modifier = Modifier.fillMaxWidth()) {
                                Text("Extra costs", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey600)
                                Text("−RM ${"%.2f".format(extra)}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), color = PSRColors.Error)
                            }
                            HorizontalDivider(color = PSRColors.Divider, modifier = Modifier.padding(vertical = 2.dp))
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text("You receive", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                                Text("RM ${"%.2f".format(netReceived)}", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = PSRColors.Navy900)
                            }
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text("Net profit", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = PSRColors.Grey600)
                                Text("RM ${"%.2f".format(netProfit)}", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (netProfit >= 0) PSRColors.Success else PSRColors.Error)
                            }
                        }
                    }
                }

                item {
                    Button(
                        onClick = { onSettle(fee, subsidy, extra, extraNote.trim(), account) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PSRColors.Success)
                    ) {
                        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Confirm & Add to Ledger", style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// ORDER DETAIL SHEET
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineOrderDetailSheet(order: OnlineOrder, onDismiss: () -> Unit) {
    val sdf = remember { SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault()) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = PSRColors.Surface,
        modifier = Modifier.fillMaxHeight(0.75f)) {
        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState())) {
            Text("Order Detail", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            Spacer(Modifier.height(4.dp))
            Text("${order.customerName} · ${when(order.platform){OnlinePlatform.SHOPEE->"Shopee";OnlinePlatform.TIKTOK->"TikTok";else->"Online"}}",
                style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey500)
            Spacer(Modifier.height(16.dp))
            order.items.forEach { item ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Text("${item.productName}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Text("×${"%.0f".format(item.qty)} ${item.unit}", style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey500, modifier = Modifier.padding(horizontal = 8.dp))
                    Text("RM ${"%.2f".format(item.lineTotal)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
            HorizontalDivider(color = PSRColors.Divider, modifier = Modifier.padding(vertical = 8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Total", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                Text("RM ${"%.2f".format(order.subtotal)}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            }
            if (order.status == OnlineOrderStatus.SETTLED) {
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("Platform fee", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey500)
                    Text("−RM ${"%.2f".format(order.platformFeeRm)}", style = MaterialTheme.typography.bodySmall, color = PSRColors.Error)
                }
                if (order.shippingSubsidy > 0) Row(modifier = Modifier.fillMaxWidth()) {
                    Text("Shipping subsidy", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey500)
                    Text("+RM ${"%.2f".format(order.shippingSubsidy)}", style = MaterialTheme.typography.bodySmall, color = PSRColors.Success)
                }
                if (order.extraCosts > 0) Row(modifier = Modifier.fillMaxWidth()) {
                    Text(order.extraCostNote.ifBlank { "Extra costs" }, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey500)
                    Text("−RM ${"%.2f".format(order.extraCosts)}", style = MaterialTheme.typography.bodySmall, color = PSRColors.Error)
                }
                HorizontalDivider(color = PSRColors.Divider, modifier = Modifier.padding(vertical = 4.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("Net profit", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    Text("RM ${"%.2f".format(order.netProfit)}", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (order.netProfit >= 0) PSRColors.Success else PSRColors.Error)
                }
            }
            if (order.notes.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text("Notes: ${order.notes}", style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey500)
            }
            if (order.settledAt != null) {
                Spacer(Modifier.height(4.dp))
                Text("Settled: ${sdf.format(Date(order.settledAt))}",
                    style = MaterialTheme.typography.labelSmall, color = PSRColors.Grey400)
            }
        }
    }
}

// ─────────────────────────────────────────────
// PRODUCT MANAGER SHEET
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineProductManagerSheet(
    products: List<OnlineProduct>,
    allStock: List<StockItem>,
    onDismiss: () -> Unit,
    onSave: (OnlineProduct) -> Unit,
    onDelete: (OnlineProduct) -> Unit
) {
    var editingProduct by remember { mutableStateOf<OnlineProduct?>(null) }
    var showForm       by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, modifier = Modifier.fillMaxHeight(0.92f),
        containerColor = PSRColors.Surface) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text("Online Products", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f))
                IconButton(onClick = { editingProduct = null; showForm = true }) {
                    Icon(Icons.Default.Add, null, tint = PSRColors.Accent)
                }
            }
            if (products.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No products yet", style = MaterialTheme.typography.bodyMedium, color = PSRColors.Grey400)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { editingProduct = null; showForm = true },
                            shape = RoundedCornerShape(12.dp)) { Text("Add First Product") }
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 40.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(products, key = { it.id }) { product ->
                        Card(shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = PSRColors.Card)) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(product.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                    val meta = buildList {
                                        if (product.sizeLabel.isNotBlank()) add(product.sizeLabel)
                                        if (product.weightGrams > 0) add("${product.weightGrams.toInt()}g")
                                        add("Cost: RM ${"%.2f".format(product.avgCostPrice)}")
                                    }.joinToString(" · ")
                                    Text(meta, style = MaterialTheme.typography.labelSmall, color = PSRColors.Grey500)
                                    Text("Sell: RM ${"%.2f".format(product.defaultSellPrice)}",
                                        style = MaterialTheme.typography.bodySmall, color = PSRColors.Navy600)
                                }
                                IconButton(onClick = { editingProduct = product; showForm = true },
                                    modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Edit, null, tint = PSRColors.Grey400, modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { onDelete(product) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.DeleteOutline, null, tint = PSRColors.Error.copy(0.6f), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showForm) {
        OnlineProductFormSheet(
            product   = editingProduct,
            allStock  = allStock,
            onDismiss = { showForm = false },
            onSave    = { p -> onSave(p); showForm = false }
        )
    }
}

// ─────────────────────────────────────────────
// PRODUCT FORM SHEET
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineProductFormSheet(
    product: OnlineProduct?,
    allStock: List<StockItem>,
    onDismiss: () -> Unit,
    onSave: (OnlineProduct) -> Unit
) {
    var name          by remember { mutableStateOf(product?.name ?: "") }
    var description   by remember { mutableStateOf(product?.description ?: "") }
    var unit          by remember { mutableStateOf(product?.unit ?: "pcs") }
    var weightStr     by remember { mutableStateOf(if ((product?.weightGrams ?: 0.0) > 0) "%.0f".format(product!!.weightGrams) else "") }
    var sizeLabel     by remember { mutableStateOf(product?.sizeLabel ?: "") }
    var costStr       by remember { mutableStateOf(if ((product?.avgCostPrice ?: 0.0) > 0) "%.2f".format(product!!.avgCostPrice) else "") }
    var sellStr       by remember { mutableStateOf(if ((product?.defaultSellPrice ?: 0.0) > 0) "%.2f".format(product!!.defaultSellPrice) else "") }
    // Platform fees
    var shopeeFeeRmStr  by remember { mutableStateOf(if ((product?.shopeeFeeRm ?: 0.0) > 0) "%.2f".format(product!!.shopeeFeeRm) else "") }
    var shopeeFeePhStr  by remember { mutableStateOf(if ((product?.shopeeFeePercent ?: 0.0) > 0) "%.1f".format(product!!.shopeeFeePercent) else "") }
    var tiktokFeeRmStr  by remember { mutableStateOf(if ((product?.tiktokFeeRm ?: 0.0) > 0) "%.2f".format(product!!.tiktokFeeRm) else "") }
    var tiktokFeePhStr  by remember { mutableStateOf(if ((product?.tiktokFeePercent ?: 0.0) > 0) "%.1f".format(product!!.tiktokFeePercent) else "") }

    // Stock catalogue lookup for quick fill
    var showStockPicker by remember { mutableStateOf(false) }
    var stockSearch     by remember { mutableStateOf("") }
    val filteredStock   = remember(allStock, stockSearch) {
        if (stockSearch.isBlank()) allStock else allStock.filter { it.name.contains(stockSearch, ignoreCase = true) }
    }

    val cost = costStr.toDoubleOrNull() ?: 0.0
    val sell = sellStr.toDoubleOrNull() ?: 0.0
    val margin = if (cost > 0 && sell > 0) ((sell - cost) / sell * 100) else 0.0

    ModalBottomSheet(onDismissRequest = onDismiss, modifier = Modifier.fillMaxHeight(0.95f)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(PSRColors.Navy800, PSRColors.Navy600)))
                .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(if (product == null) "New Online Product" else "Edit Product",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White, modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    if (name.isBlank()) return@TextButton
                    onSave((product ?: OnlineProduct(name = name)).copy(
                        name = name.trim(), description = description.trim(),
                        unit = unit, weightGrams = weightStr.toDoubleOrNull() ?: 0.0,
                        sizeLabel = sizeLabel.trim(),
                        avgCostPrice = cost, defaultSellPrice = sell,
                        shopeeFeeRm = shopeeFeeRmStr.toDoubleOrNull() ?: 0.0,
                        shopeeFeePercent = shopeeFeePhStr.toDoubleOrNull() ?: 0.0,
                        tiktokFeeRm = tiktokFeeRmStr.toDoubleOrNull() ?: 0.0,
                        tiktokFeePercent = tiktokFeePhStr.toDoubleOrNull() ?: 0.0
                    ))
                }, enabled = name.isNotBlank()) {
                    Text("Save", color = if (name.isNotBlank()) PSRColors.Gold else Color.White.copy(0.3f), fontWeight = FontWeight.Bold)
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp, 14.dp, 16.dp, 40.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {

                // Quick-fill from catalogue
                item {
                    OutlinedButton(onClick = { showStockPicker = !showStockPicker },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                        Icon(Icons.Default.Inventory2, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Fill name from stock catalogue", style = MaterialTheme.typography.labelLarge)
                    }
                    if (showStockPicker) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = stockSearch, onValueChange = { stockSearch = it },
                            placeholder = { Text("Search…") }, modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp), singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(16.dp)) })
                        Spacer(Modifier.height(6.dp))
                        filteredStock.take(6).forEach { stock ->
                            Surface(onClick = {
                                name = stock.name; unit = stock.unit
                                showStockPicker = false; stockSearch = ""
                            }, shape = RoundedCornerShape(8.dp), color = PSRColors.Grey50,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(stock.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                    Text(stock.unit, style = MaterialTheme.typography.labelSmall, color = PSRColors.Grey400)
                                }
                            }
                        }
                    }
                }

                // Name, unit, size, weight
                item {
                    OutlinedTextField(value = name, onValueChange = { name = it },
                        label = { Text("Product Name *") }, modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp), singleLine = true)
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(value = unit, onValueChange = { unit = it },
                            label = { Text("Unit") }, modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp), singleLine = true)
                        OutlinedTextField(value = weightStr, onValueChange = { weightStr = it },
                            label = { Text("Weight (g)") }, modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp), singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
                    }
                }
                item {
                    OutlinedTextField(value = sizeLabel, onValueChange = { sizeLabel = it },
                        label = { Text("Size / Variant (display only)") },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true,
                        placeholder = { Text("e.g. Small / 500g pack / Red") })
                }

                // Pricing
                item {
                    Text("Pricing", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = PSRColors.Navy700)
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Avg Cost (RM)", style = MaterialTheme.typography.labelSmall, color = PSRColors.Grey600)
                            Spacer(Modifier.height(4.dp))
                            RMInputField("Cost", costStr, { costStr = it }, Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Default Sell (RM)", style = MaterialTheme.typography.labelSmall, color = PSRColors.Grey600)
                            Spacer(Modifier.height(4.dp))
                            RMInputField("Sell", sellStr, { sellStr = it }, Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal))
                        }
                    }
                    if (margin > 0) {
                        Spacer(Modifier.height(4.dp))
                        Text("Margin: ${"%.1f".format(margin)}%  ·  Profit: RM ${"%.2f".format(sell - cost)} per unit",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (margin >= 20) PSRColors.Success else PSRColors.Warning)
                    }
                }

                // Platform fees — the smart section
                item {
                    Text("Platform Fees (default per order)",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = PSRColors.Navy700)
                    Spacer(Modifier.height(2.dp))
                    Text("Set your typical fee. You can always edit it at settlement if it changes.",
                        style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey500)
                }
                item {
                    // Shopee
                    Surface(color = Color(0xFFEE4D2D).copy(0.04f), shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFEE4D2D).copy(0.2f))) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🛍 Shopee", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFEE4D2D))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Fixed fee (RM)", style = MaterialTheme.typography.labelSmall, color = PSRColors.Grey600)
                                    Spacer(Modifier.height(4.dp))
                                    RMInputField("e.g. 0.50", shopeeFeeRmStr, { shopeeFeeRmStr = it }, Modifier.fillMaxWidth(),
                                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Commission (%)", style = MaterialTheme.typography.labelSmall, color = PSRColors.Grey600)
                                    Spacer(Modifier.height(4.dp))
                                    OutlinedTextField(value = shopeeFeePhStr, onValueChange = { shopeeFeePhStr = it },
                                        label = { Text("e.g. 2.0") }, modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp), singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                                        trailingIcon = { Text("%", modifier = Modifier.padding(end = 8.dp), style = MaterialTheme.typography.bodyMedium) })
                                }
                            }
                        }
                    }
                }
                item {
                    // TikTok
                    Surface(color = Color.Black.copy(0.03f), shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.Black.copy(0.1f))) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🎵 TikTok", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Fixed fee (RM)", style = MaterialTheme.typography.labelSmall, color = PSRColors.Grey600)
                                    Spacer(Modifier.height(4.dp))
                                    RMInputField("e.g. 0.50", tiktokFeeRmStr, { tiktokFeeRmStr = it }, Modifier.fillMaxWidth(),
                                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Commission (%)", style = MaterialTheme.typography.labelSmall, color = PSRColors.Grey600)
                                    Spacer(Modifier.height(4.dp))
                                    OutlinedTextField(value = tiktokFeePhStr, onValueChange = { tiktokFeePhStr = it },
                                        label = { Text("e.g. 1.8") }, modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp), singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                                        trailingIcon = { Text("%", modifier = Modifier.padding(end = 8.dp), style = MaterialTheme.typography.bodyMedium) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
