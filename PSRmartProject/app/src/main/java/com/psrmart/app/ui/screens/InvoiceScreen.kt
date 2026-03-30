@file:OptIn(ExperimentalMaterial3Api::class)
package com.psrmart.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import android.media.RingtoneManager
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.content.Intent
import android.net.Uri
import com.psrmart.app.data.model.*
import com.psrmart.app.ui.components.*
import com.psrmart.app.ui.theme.PSRColors
import com.psrmart.app.ui.theme.PlayfairFamily
import com.psrmart.app.ui.theme.InterFamily
import com.psrmart.app.ui.theme.MontserratFamily
import com.psrmart.app.ui.theme.PSRTextStyles
import com.psrmart.app.util.InvoicePrinter
import com.psrmart.app.viewmodel.RoundOffMode
import com.psrmart.app.viewmodel.PSRViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import java.text.SimpleDateFormat
import java.util.*

// ─────────────────────────────────────────────
// INVOICE SCREEN — tab container
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceScreen(viewModel: PSRViewModel, onViewInvoice: (Long) -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }
    val navTarget   by viewModel.navigateToInvoiceId.collectAsState()
    val editingId   by viewModel.editingInvoiceId.collectAsState()

    // When an invoice is loaded for edit, switch to builder tab
    LaunchedEffect(editingId) { if (editingId != null) selectedTab = 0 }
    LaunchedEffect(navTarget)  { if (navTarget != null) selectedTab = 1 }

    Column(modifier = Modifier.fillMaxSize().background(PSRColors.Surface)) {
        Box(modifier = Modifier.fillMaxWidth().background(PSRColors.Navy600).padding(bottom = 12.dp, start = 16.dp, end = 16.dp, top = 4.dp)) {
            Row(modifier = Modifier.fillMaxWidth().background(PSRColors.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp)).padding(4.dp)) {
                listOf("New Invoice", "History").forEachIndexed { idx, label ->
                    val isSelected = selectedTab == idx
                    Box(
                        modifier = Modifier.weight(1f)
                            .background(if (isSelected) PSRColors.White else Color.Transparent, RoundedCornerShape(10.dp))
                            .clickable { selectedTab = idx; if (idx == 0) viewModel.clearNavigation() }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, style = MaterialTheme.typography.labelLarge, color = if (isSelected) PSRColors.Navy600 else PSRColors.White.copy(alpha = 0.7f))
                    }
                }
            }
        }
        when (selectedTab) {
            0 -> InvoiceBuilderTab(viewModel, onViewInvoice)
            1 -> InvoiceHistoryTab(viewModel, onEdit = { invoice -> viewModel.loadInvoiceForEdit(invoice); selectedTab = 0 })
        }
    }
}

// ─────────────────────────────────────────────
// BUILDER TAB
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceBuilderTab(viewModel: PSRViewModel, onInvoiceSaved: (Long) -> Unit) {
    val context         = LocalContext.current
    val customers       by viewModel.customers.collectAsState()
    val allStock        by viewModel.allStockItems.collectAsState()
    val categories      by viewModel.categories.collectAsState()
    val settings        by viewModel.businessSettings.collectAsState()
    val draftCustomer   by viewModel.draftCustomer.collectAsState()
    val draftCustomerId by viewModel.draftCustomerId.collectAsState()
    val draftItems      by viewModel.draftItems.collectAsState()
    val draftSubtotal   by viewModel.draftSubtotal.collectAsState()
    val draftTotal      by viewModel.draftTotal.collectAsState()
    val draftProfit     by viewModel.draftProfit.collectAsState()
    val draftNotes      by viewModel.draftNotes.collectAsState()
    val draftDiscount   by viewModel.draftDiscount.collectAsState()
    val draftRoundOff   by viewModel.draftRoundOff.collectAsState()
    val draftInvNo      by viewModel.draftInvoiceNumber.collectAsState()
    val draftDate     by viewModel.draftDate.collectAsState()
    val editingId     by viewModel.editingInvoiceId.collectAsState()
    val invoices      by viewModel.invoices.collectAsState()

    var showCustomerPicker  by remember { mutableStateOf(false) }
    var showAddItem         by remember { mutableStateOf(false) }
    var editingItemIndex    by remember { mutableStateOf<Int?>(null) }
    var discountInput       by remember { mutableStateOf("") }
    var showSaveConfirm     by remember { mutableStateOf(false) }
    var showPreview         by remember { mutableStateOf(false) }
    var showDatePicker      by remember { mutableStateOf(false) }
    var showClearConfirm    by remember { mutableStateOf(false) }

    // Sync discount input when editing
    LaunchedEffect(draftDiscount) { if (discountInput.toDoubleOrNull() != draftDiscount) discountInput = if (draftDiscount == 0.0) "" else "%.2f".format(draftDiscount) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // Edit mode banner
        if (editingId != null) {
            Row(
                modifier = Modifier.fillMaxWidth().background(PSRColors.Warning.copy(0.15f)).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Edit, null, tint = PSRColors.Warning, modifier = Modifier.size(14.dp))
                    Text("Editing invoice $draftInvNo", style = MaterialTheme.typography.labelMedium, color = PSRColors.Warning)
                }
                TextButton(onClick = { showClearConfirm = true }, contentPadding = PaddingValues(4.dp)) {
                    Text("Cancel", style = MaterialTheme.typography.labelSmall, color = PSRColors.Error)
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Bill To + Company
        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PSRColors.Card), elevation = CardDefaults.cardElevation(1.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Bill To:", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic), color = PSRColors.Navy900)
                    Spacer(Modifier.height(4.dp))
                    if (draftCustomer != null) {
                        Text(draftCustomer!!.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = PSRColors.Navy900)
                        if (draftCustomer!!.company.isNotBlank()) Text(draftCustomer!!.company, style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey600)
                        if (draftCustomer!!.phone.isNotBlank()) Text(draftCustomer!!.phone, style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey600)

                        // ── Outstanding balance + last invoice for this customer ──
                        val currentCustomerId = draftCustomerId
                        if (currentCustomerId != null && currentCustomerId > 0) {
                            val custInvoices = remember(invoices, currentCustomerId) {
                                invoices.filter { it.customerId == currentCustomerId || it.customerSnapshot.name == draftCustomer!!.name }
                            }
                            val outstanding = remember(custInvoices) {
                                custInvoices.filter { it.status != InvoiceStatus.PAID && it.status != InvoiceStatus.CANCELLED }
                                    .sumOf { it.totalAmount - it.amountPaid }
                            }
                            val lastInvoice = remember(custInvoices) {
                                custInvoices.maxByOrNull { it.issuedAt }
                            }

                            Spacer(Modifier.height(8.dp))
                            HorizontalDivider(color = PSRColors.Divider)
                            Spacer(Modifier.height(6.dp))

                            // Outstanding balance pill
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (outstanding > 0.01) {
                                    Box(
                                        modifier = Modifier
                                            .background(PSRColors.Error.copy(0.1f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Icon(Icons.Default.Warning, null, tint = PSRColors.Error, modifier = Modifier.size(12.dp))
                                            Text(
                                                "Owes RM ${"%.2f".format(outstanding)}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = PSRColors.Error
                                            )
                                        }
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .background(PSRColors.Success.copy(0.1f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Icon(Icons.Default.CheckCircle, null, tint = PSRColors.Success, modifier = Modifier.size(12.dp))
                                            Text(
                                                "All cleared",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = PSRColors.Success
                                            )
                                        }
                                    }
                                }
                            }

                            // Last invoice number
                            if (lastInvoice != null) {
                                Spacer(Modifier.height(4.dp))
                                val sdf = remember { java.text.SimpleDateFormat("dd/MM/yy", java.util.Locale.getDefault()) }
                                Text(
                                    "Last: ${lastInvoice.invoiceNumber} · ${sdf.format(java.util.Date(lastInvoice.issuedAt))} · RM ${"%.2f".format(lastInvoice.totalAmount)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PSRColors.Grey500
                                )
                            }
                        }
                    } else {
                        TextButton(onClick = { showCustomerPicker = true }, contentPadding = PaddingValues(0.dp)) {
                            Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)); Text("Select Customer")
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                    Text(settings.companyName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = PSRColors.Navy900, textAlign = TextAlign.End)
                    if (settings.phone.isNotBlank()) Text(settings.phone, style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey600, textAlign = TextAlign.End)
                    if (settings.email.isNotBlank()) Text(settings.email, style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey600, textAlign = TextAlign.End)
                }
            }
            if (draftCustomer != null) {
                TextButton(onClick = { showCustomerPicker = true }, modifier = Modifier.padding(start = 8.dp, bottom = 4.dp), contentPadding = PaddingValues(8.dp, 4.dp)) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(12.dp)); Spacer(Modifier.width(4.dp)); Text("Change", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Invoice No + Date
        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = PSRColors.Card), elevation = CardDefaults.cardElevation(1.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                // ── Invoice number: locked "INV-" prefix + editable 4-digit number ──
                val numPart = remember(draftInvNo) {
                    draftInvNo.filter { it.isDigit() }.padStart(4, '0').takeLast(4)
                }
                OutlinedTextField(
                    value = numPart,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }.take(4)
                        viewModel.setDraftInvoiceNumber("INV-${digits.padStart(4, '0')}")
                    },
                    label = { Text("Invoice No") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    prefix = {
                        Text(
                            "INV-",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = PSRColors.Navy600
                            )
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PSRColors.Accent)
                )
                Spacer(Modifier.height(12.dp))
                val dateStr = remember(draftDate) { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(draftDate)) }
                OutlinedCard(onClick = { showDatePicker = true }, shape = RoundedCornerShape(10.dp), colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(18.dp), tint = PSRColors.Navy600)
                        Spacer(Modifier.width(12.dp))
                        Column { Text("Invoice Date", style = MaterialTheme.typography.labelSmall, color = PSRColors.Grey600); Text(dateStr, style = MaterialTheme.typography.bodyMedium) }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Invoice Table
        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PSRColors.Card), elevation = CardDefaults.cardElevation(1.dp)) {
            Column {
                // Header row
                Row(modifier = Modifier.fillMaxWidth().background(PSRColors.Grey50).padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("#", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.width(28.dp), textAlign = TextAlign.Center, color = PSRColors.Navy900)
                    VerticalDivider(modifier = Modifier.height(20.dp), color = PSRColors.Grey200)
                    Text("Description", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f).padding(horizontal = 6.dp), color = PSRColors.Navy900)
                    VerticalDivider(modifier = Modifier.height(20.dp), color = PSRColors.Grey200)
                    Text("Qty", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.width(48.dp), textAlign = TextAlign.End, color = PSRColors.Navy900)
                    VerticalDivider(modifier = Modifier.height(20.dp), color = PSRColors.Grey200)
                    Text("Unit", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.width(34.dp), textAlign = TextAlign.Center, color = PSRColors.Navy900)
                    VerticalDivider(modifier = Modifier.height(20.dp), color = PSRColors.Grey200)
                    Text("Rate", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.width(48.dp), textAlign = TextAlign.End, color = PSRColors.Navy900)
                    VerticalDivider(modifier = Modifier.height(32.dp), color = PSRColors.Grey200)
                    Text("Amount", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.width(56.dp), textAlign = TextAlign.End, color = PSRColors.Navy900)
                }
                HorizontalDivider(color = PSRColors.Navy900, thickness = 1.dp)

                if (draftItems.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                        Text("No items — tap Add Item below", style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey400, textAlign = TextAlign.Center)
                    }
                } else {
                    draftItems.forEachIndexed { idx, item ->
                        Column {
                            Row(modifier = Modifier.fillMaxWidth().clickable { editingItemIndex = idx }.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("${idx+1}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center, color = PSRColors.Navy900)
                                VerticalDivider(modifier = Modifier.height(20.dp), color = PSRColors.Grey200)
                                Text(item.description, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f).padding(horizontal = 6.dp), color = PSRColors.Navy900)
                                VerticalDivider(modifier = Modifier.height(20.dp), color = PSRColors.Grey200)
                                Text("%.2f".format(item.qty), style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(48.dp), textAlign = TextAlign.End, color = PSRColors.Navy900)
                                VerticalDivider(modifier = Modifier.height(20.dp), color = PSRColors.Grey200)
                                Text(item.unit, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(34.dp), textAlign = TextAlign.Center, color = PSRColors.Navy900)
                                VerticalDivider(modifier = Modifier.height(20.dp), color = PSRColors.Grey200)
                                Text("%.2f".format(item.sellPrice), style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(48.dp), textAlign = TextAlign.End, color = PSRColors.Navy900)
                                VerticalDivider(modifier = Modifier.height(20.dp), color = PSRColors.Grey200)
                                Row(modifier = Modifier.width(56.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                                    Text("%.2f".format(item.netTotal), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End, color = PSRColors.Navy900, modifier = Modifier.weight(1f))
                                    Box(modifier = Modifier.size(16.dp).clickable { viewModel.removeDraftItem(idx) }, contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Close, null, tint = PSRColors.Error, modifier = Modifier.size(11.dp))
                                    }
                                }
                            }
                            // Return sub-line — italic, indented
                            if (item.hasReturn) {
                                Row(modifier = Modifier.fillMaxWidth().padding(start = 36.dp, end = 8.dp, bottom = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "↩ ${item.returnNote.ifBlank { "Return ${item.returnQty} ${item.unit}" }}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                                        color = PSRColors.Warning,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text("−%.2f".format(item.returnAmount),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = PSRColors.Warning)
                                }
                            }
                        }
                        HorizontalDivider(color = PSRColors.Grey100)
                    }

                    HorizontalDivider(color = PSRColors.Grey300)

                    // Sub Total
                    Row(modifier = Modifier.fillMaxWidth().padding(end = 8.dp, top = 4.dp, bottom = 2.dp)) {
                        Spacer(Modifier.weight(1f))
                        Text("Sub Total", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.width(100.dp), textAlign = TextAlign.End, color = PSRColors.Navy900)
                        Text("%.2f".format(draftSubtotal), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.width(72.dp), textAlign = TextAlign.End, color = PSRColors.Navy900)
                    }

                    // Discount
                    Row(modifier = Modifier.fillMaxWidth().padding(end = 8.dp, top = 2.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Spacer(Modifier.weight(1f))
                        Text("Discount On Sale", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.width(100.dp), textAlign = TextAlign.End, color = PSRColors.Navy900, lineHeight = 14.sp)
                        OutlinedTextField(
                            value = discountInput,
                            onValueChange = { v -> val filtered = v.filter { it.isDigit() || it == '.' }; discountInput = filtered; viewModel.setDraftDiscount(filtered.toDoubleOrNull() ?: 0.0) },
                            prefix = { Text("(", style = MaterialTheme.typography.bodySmall) },
                            suffix = { Text(")", style = MaterialTheme.typography.bodySmall) },
                            modifier = Modifier.width(72.dp), singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.End),
                            shape = RoundedCornerShape(6.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PSRColors.Accent, unfocusedBorderColor = PSRColors.Grey200)
                        )
                    }

                    // Round-off row
                    Row(modifier = Modifier.fillMaxWidth().padding(end = 8.dp, top = 2.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Spacer(Modifier.weight(1f))
                        Text("Round-off", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.width(100.dp), textAlign = TextAlign.End, color = PSRColors.Navy900)
                        Text(
                            if (draftRoundOff == 0.0) "0.00" else "%+.2f".format(draftRoundOff),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.width(72.dp), textAlign = TextAlign.End,
                            color = if (draftRoundOff >= 0) PSRColors.Navy900 else PSRColors.Error
                        )
                    }

                    // Round-off quick buttons
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Spacer(Modifier.weight(1f))
                        listOf(
                            "None" to RoundOffMode.NONE,
                            "±0.50" to RoundOffMode.NEAREST_50,
                            "↑RM" to RoundOffMode.ROUND_UP,
                            "↓RM" to RoundOffMode.ROUND_DOWN
                        ).forEach { (label, mode) ->
                            val active = when (mode) {
                                RoundOffMode.NONE -> draftRoundOff == 0.0
                                else -> false
                            }
                            OutlinedButton(
                                onClick = { viewModel.applyRoundOff(mode) },
                                modifier = Modifier.height(28.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, if (active) PSRColors.Navy600 else PSRColors.Grey200)
                            ) {
                                Text(label, style = MaterialTheme.typography.labelSmall, color = if (active) PSRColors.Navy600 else PSRColors.Grey600)
                            }
                        }
                    }

                    // Divider + Amount
                    Row(modifier = Modifier.fillMaxWidth().padding(end = 8.dp)) { Spacer(Modifier.weight(1f)); HorizontalDivider(modifier = Modifier.width(172.dp), color = PSRColors.Navy900) }
                    Spacer(Modifier.height(2.dp))
                    Row(modifier = Modifier.fillMaxWidth().padding(end = 8.dp, bottom = 4.dp)) {
                        Spacer(Modifier.weight(1f))
                        Text("Amount", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.width(100.dp), textAlign = TextAlign.End, color = PSRColors.Navy900)
                        Text("%.2f".format(draftTotal), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.width(72.dp), textAlign = TextAlign.End, color = PSRColors.Navy900)
                    }

                    // Total footer
                    HorizontalDivider(color = PSRColors.Navy900, thickness = 1.5.dp)
                    Row(modifier = Modifier.fillMaxWidth().background(PSRColors.Grey50).padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Total", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.width(40.dp), color = PSRColors.Navy900)
                        Text("%.2f".format(draftItems.sumOf { it.qty }), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.width(60.dp), textAlign = TextAlign.End, color = PSRColors.Navy900)
                        Spacer(Modifier.weight(1f))
                        Text("RM%.2f".format(draftTotal), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), color = PSRColors.Navy900)
                    }
                    HorizontalDivider(color = PSRColors.Navy900, thickness = 1.5.dp)
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        OutlinedButton(onClick = { showAddItem = true }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, PSRColors.Accent)) {
            Icon(Icons.Default.Add, null, tint = PSRColors.Accent); Spacer(Modifier.width(6.dp)); Text("Add Item", color = PSRColors.Accent)
        }

        Spacer(Modifier.height(10.dp))

        if (draftItems.isNotEmpty()) {
            // Profit card
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = if (draftProfit >= 0) PSRColors.Success.copy(0.08f) else PSRColors.Error.copy(0.08f))) {
                Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Lock, null, tint = PSRColors.Grey400, modifier = Modifier.size(14.dp))
                        Text("NET PROFIT (not on invoice)", style = MaterialTheme.typography.labelSmall, color = PSRColors.Grey600)
                    }
                    Text("RM %.2f".format(draftProfit), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = if (draftProfit >= 0) PSRColors.Success else PSRColors.Error)
                }
            }

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(value = draftNotes, onValueChange = viewModel::setDraftNotes,
                label = { Text("Payment notes / footer") },
                placeholder = { Text("e.g. Cash or Maybank AC: ${settings.accountNumber}") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(14.dp), maxLines = 3)

            Spacer(Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { showPreview = true }, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, PSRColors.Navy600)) {
                    Icon(Icons.Default.Visibility, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Preview")
                }
                Button(onClick = { showSaveConfirm = true }, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PSRColors.Navy600)) {
                    Icon(if (editingId != null) Icons.Default.Save else Icons.Default.NoteAdd, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (editingId != null) "Update" else "Generate")
                }
            }
        }
        Spacer(Modifier.height(100.dp))
    }

    // ── Dialogs ───────────────────────────────
    if (showDatePicker) {
        val dps = rememberDatePickerState(initialSelectedDateMillis = draftDate)
        DatePickerDialog(onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { dps.selectedDateMillis?.let { viewModel.setDraftDate(it) }; showDatePicker = false }) { Text("OK") } }
        ) { DatePicker(state = dps) }
    }

    if (showCustomerPicker) {
        val invList by viewModel.invoices.collectAsState()
        CustomerPickerSheet(customers = customers, invoices = invList,
            onSelect = { c -> viewModel.setDraftCustomerWithId(CustomerSnapshot(c.name, c.company, c.phone, c.email, c.address, c.customFields), c.id); showCustomerPicker = false },
            onWalkIn = { viewModel.setDraftCustomer(CustomerSnapshot("Walk-in Customer","","","","")); showCustomerPicker = false },
            onDismiss = { showCustomerPicker = false },
            onAddNew  = { nc -> viewModel.insertCustomer(nc); viewModel.setDraftCustomer(CustomerSnapshot(nc.name,nc.company,nc.phone,nc.email,nc.address)); showCustomerPicker = false },
            onDelete  = { c -> viewModel.deleteCustomer(c) },
            onPayInvoice = { inv, amount, date, account -> viewModel.processInvoicePayment(inv, amount, date, account) })
    }

    if (showAddItem || editingItemIndex != null) {
        Dialog(onDismissRequest = { showAddItem = false; editingItemIndex = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            AddInvoiceItemContent(
                existingItem = editingItemIndex?.let { draftItems[it] },
                stockItems = allStock, categories = categories,
                units = settings.customUnits,
                customerId = draftCustomerId,
                // Open catalogue picker immediately for new items, skip for edits
                startWithPicker = editingItemIndex == null,
                viewModel = viewModel,
                onDismiss = { showAddItem = false; editingItemIndex = null },
                onSaveNewItem = { item -> viewModel.insertStock(item) },
                onSave = { item ->
                    if (editingItemIndex != null) { viewModel.updateDraftItem(editingItemIndex!!, item); editingItemIndex = null }
                    else { viewModel.addDraftItem(item); showAddItem = false }
                }
            )
        }
    }

    if (showPreview) {
        InvoicePreviewDialog(settings=settings, customer=draftCustomer, items=draftItems,
            invNo=draftInvNo, date=draftDate, subtotal=draftSubtotal, discount=draftDiscount,
            roundOff=draftRoundOff, total=draftTotal, notes=draftNotes, onDismiss={showPreview=false})
    }

    if (showSaveConfirm) {
        val isEdit = editingId != null
        AlertDialog(onDismissRequest = { showSaveConfirm = false },
            title = { Text(if (isEdit) "Update Invoice?" else "Generate Invoice?") },
            text = { Column {
                Text("Total: RM %.2f".format(draftTotal), style = MaterialTheme.typography.titleMedium)
                Text("Profit: RM %.2f".format(draftProfit), color = if (draftProfit>=0) PSRColors.Success else PSRColors.Error)
                if (draftRoundOff != 0.0) Text("Round-off: %+.2f".format(draftRoundOff), style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey600)
            } },
            confirmButton = { Button(onClick = { viewModel.saveInvoice { id -> onInvoiceSaved(id) }; showSaveConfirm=false }, colors = ButtonDefaults.buttonColors(containerColor = PSRColors.Navy600)) { Text(if (isEdit) "Update" else "Generate") } },
            dismissButton = { TextButton(onClick={showSaveConfirm=false}){Text("Cancel")} })
    }

    if (showClearConfirm) {
        AlertDialog(onDismissRequest = { showClearConfirm = false },
            title = { Text("Cancel Editing?") },
            text = { Text("This will discard your changes and start a new invoice.") },
            confirmButton = { TextButton(onClick = { viewModel.clearDraft(); showClearConfirm = false }) { Text("Discard", color = PSRColors.Error) } },
            dismissButton = { TextButton(onClick = { showClearConfirm = false }) { Text("Keep Editing") } })
    }
}

// ─────────────────────────────────────────────
// ADD INVOICE ITEM — picker-first flow
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddInvoiceItemContent(
    existingItem: InvoiceItem?,
    stockItems: List<StockItem>,
    categories: List<Category>,
    units: List<String>,
    customerId: Long? = null,
    startWithPicker: Boolean = true,
    viewModel: PSRViewModel? = null,
    onDismiss: () -> Unit,
    onSave: (InvoiceItem) -> Unit,
    onSaveNewItem: (StockItem) -> Unit = {}
) {
    // Pre-loaded price map for this customer — available instantly, no async needed
    val customerPriceMap by (viewModel?.customerPriceMap ?: MutableStateFlow(emptyMap())).collectAsState()

    var description     by remember { mutableStateOf(existingItem?.description ?: "") }
    var unit            by remember { mutableStateOf(existingItem?.unit ?: units.firstOrNull() ?: "unit") }
    var qty             by remember { mutableStateOf(existingItem?.qty?.toString() ?: "") }
    var buyPrice        by remember { mutableStateOf(existingItem?.buyPrice?.let { "%.2f".format(it) } ?: "") }
    var sellPrice       by remember { mutableStateOf(existingItem?.sellPrice?.let { "%.2f".format(it) } ?: "") }
    var rememberedLabel   by remember { mutableStateOf("") }
    var rememberedDate    by remember { mutableStateOf("") }
    var cataloguePrice    by remember { mutableStateOf(0.0) }
    var hasCustomerPrice  by remember { mutableStateOf(false) }
    var showUnitMenu    by remember { mutableStateOf(false) }
    var selectedStockItemId by remember { mutableStateOf(existingItem?.stockItemId) }
    // Return / refund sub-line
    var showReturnSection by remember { mutableStateOf(existingItem?.hasReturn == true) }
    var returnQtyStr    by remember { mutableStateOf(if ((existingItem?.returnQty ?: 0.0) > 0) "%.2f".format(existingItem!!.returnQty) else "") }
    var returnPriceStr  by remember { mutableStateOf(if ((existingItem?.returnPrice ?: 0.0) > 0) "%.2f".format(existingItem!!.returnPrice) else "") }
    var returnNote      by remember { mutableStateOf(existingItem?.returnNote ?: "") }

    var showPicker      by remember { mutableStateOf(startWithPicker && existingItem == null) }
    var showOneOffForm  by remember { mutableStateOf(false) }
    var catalogueSearch by remember { mutableStateOf("") }

    var newName      by remember { mutableStateOf("") }
    var newUnit      by remember { mutableStateOf(units.firstOrNull() ?: "kg") }
    var newBuy       by remember { mutableStateOf("") }
    var newSell      by remember { mutableStateOf("") }
    var saveToDb     by remember { mutableStateOf(false) }
    var showNewUnit  by remember { mutableStateOf(false) }

    val buy  = buyPrice.toDoubleOrNull() ?: 0.0
    val sell = sellPrice.toDoubleOrNull() ?: 0.0
    val qtyVal    = qty.toDoubleOrNull() ?: 0.0
    val lineTotal = qtyVal * sell
    val lineCost  = qtyVal * buy
    val profit    = lineTotal - lineCost
    val profitPerUnit = sell - buy
    val marginPct = if (buy > 0) (profitPerUnit / buy) * 100 else 0.0

    // ── CATALOGUE PICKER ─────────────────────────────────────────────────
    if (showPicker) {
        val filtered = remember(stockItems, catalogueSearch) {
            if (catalogueSearch.isBlank()) stockItems
            else stockItems.filter { it.name.contains(catalogueSearch, ignoreCase = true) }
        }
        val grouped = remember(filtered, categories) { filtered.groupBy { it.categoryId } }

        Surface(modifier = Modifier.fillMaxSize(), color = PSRColors.Surface) {
            Column {
                Column(modifier = Modifier.fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(PSRColors.Navy800, PSRColors.Navy600)))
                    .padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Close, null, tint = PSRColors.White)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("Pick an Item", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = PSRColors.White, modifier = Modifier.weight(1f))
                        if (customerId != null && customerId > 0) {
                            Box(modifier = Modifier.background(PSRColors.Gold.copy(0.3f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                Text("Custom prices", style = MaterialTheme.typography.labelSmall, color = PSRColors.White)
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = catalogueSearch, onValueChange = { catalogueSearch = it },
                        placeholder = { Text("Search products\u2026", color = PSRColors.White.copy(0.5f), style = MaterialTheme.typography.bodyMedium) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = PSRColors.White.copy(0.7f), modifier = Modifier.size(18.dp)) },
                        trailingIcon = { if (catalogueSearch.isNotBlank()) IconButton(onClick = { catalogueSearch = "" }) { Icon(Icons.Default.Clear, null, tint = PSRColors.White.copy(0.7f), modifier = Modifier.size(16.dp)) } },
                        modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp), singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PSRColors.Accent, unfocusedBorderColor = PSRColors.White.copy(0.25f), cursorColor = PSRColors.Accent)
                    )
                }
                LazyColumn(modifier = Modifier.weight(1f)) {
                    if (filtered.isEmpty()) {
                        item {
                            Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("No products found", color = PSRColors.Grey500, style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.height(12.dp))
                                OutlinedButton(onClick = { newName = catalogueSearch; showOneOffForm = true; showPicker = false }, shape = RoundedCornerShape(10.dp)) {
                                    Icon(Icons.Default.AddCircleOutline, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Add \"$catalogueSearch\" as one-off")
                                }
                            }
                        }
                    } else {
                        categories.forEach { cat ->
                            val catItems = grouped[cat.id] ?: return@forEach
                            item(key = "cat_${cat.id}") {
                                Text("${cat.emoji} ${cat.name}", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = PSRColors.Grey600, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                            }
                            items(catItems, key = { it.id }) { stock ->
                                Row(modifier = Modifier.fillMaxWidth()
                                    .clickable {
                                        description = stock.name; unit = stock.unit
                                        buyPrice = "%.2f".format(stock.defaultBuyPrice)
                                        selectedStockItemId = stock.id
                                        val defaultSell = stock.currentSellPrice
                                        cataloguePrice = defaultSell
                                        if (customerId != null && customerId > 0 && viewModel != null) {
                                            viewModel.getCustomerItemPriceWithDate(customerId, stock.id) { price, date ->
                                                if (price != null) {
                                                    sellPrice        = "%.2f".format(price)
                                                    hasCustomerPrice = true
                                                    rememberedLabel  = "RM ${"%.2f".format(price)}"
                                                    rememberedDate   = date ?: ""
                                                } else {
                                                    sellPrice        = "%.2f".format(defaultSell)
                                                    hasCustomerPrice = false
                                                    rememberedLabel  = ""
                                                    rememberedDate   = ""
                                                }
                                            }
                                        } else {
                                            sellPrice        = "%.2f".format(defaultSell)
                                            hasCustomerPrice = false
                                            rememberedLabel  = ""
                                            rememberedDate   = ""
                                        }
                                        showPicker = false
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(stock.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = PSRColors.Navy900)
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("Buy: RM ${"%.2f".format(stock.defaultBuyPrice)}", style = MaterialTheme.typography.labelSmall, color = PSRColors.Grey500)
                                            if (stock.stockQty < 2) Text("\u26a0 Low", style = MaterialTheme.typography.labelSmall, color = PSRColors.Warning)
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        val customerRecord = customerPriceMap[stock.id]
                                        if (customerRecord != null) {
                                            // Show their remembered price prominently
                                            Text("RM ${"%.2f".format(customerRecord.lastSellPrice)}",
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                                                color = PSRColors.Accent)
                                            Text("↩ last price",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                color = PSRColors.Accent.copy(0.7f))
                                            if (Math.abs(customerRecord.lastSellPrice - stock.currentSellPrice) > 0.001)
                                                Text("cat: ${"%.2f".format(stock.currentSellPrice)}",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                    color = PSRColors.Grey400)
                                        } else {
                                            Text("RM ${"%.2f".format(stock.currentSellPrice)}",
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                color = PSRColors.Navy600)
                                            if (customerId != null && customerId > 0)
                                                Text("new item",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                    color = PSRColors.Warning)
                                        }
                                        Text("${"%.1f".format(stock.stockQty)} ${stock.unit}", style = MaterialTheme.typography.labelSmall, color = PSRColors.Grey400)
                                    }
                                    Icon(Icons.Default.ChevronRight, null, tint = PSRColors.Grey300, modifier = Modifier.size(16.dp))
                                }
                                HorizontalDivider(color = PSRColors.Divider, modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                    item {
                        Row(modifier = Modifier.fillMaxWidth()
                            .clickable { showOneOffForm = true; showPicker = false }
                            .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(40.dp).background(PSRColors.Grey100, CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.AddCircleOutline, null, tint = PSRColors.Accent, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text("Add one-off item", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = PSRColors.Accent)
                                Text("Not in catalogue", style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey500)
                            }
                        }
                        Spacer(Modifier.height(80.dp))
                    }
                }
            }
        }
        return
    }

    // ── ONE-OFF FORM ─────────────────────────────────────────────────────
    if (showOneOffForm) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showOneOffForm = false; showPicker = true }) { Icon(Icons.Default.ArrowBack, null, tint = PSRColors.Navy600) }
                    Spacer(Modifier.width(8.dp))
                    Column { Text("One-off Item", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                        Text("Not saved to catalogue", style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey500) }
                }
                Spacer(Modifier.height(20.dp))
                OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Item Name *") },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
                Spacer(Modifier.height(10.dp))
                ExposedDropdownMenuBox(expanded = showNewUnit, onExpandedChange = { showNewUnit = it }) {
                    OutlinedTextField(value = newUnit, onValueChange = { newUnit = it }, label = { Text("Unit") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(showNewUnit) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
                    ExposedDropdownMenu(expanded = showNewUnit, onDismissRequest = { showNewUnit = false }) {
                        units.forEach { u -> DropdownMenuItem(text = { Text(u) }, onClick = { newUnit = u; showNewUnit = false }) }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RMInputField("Buy / $newUnit", newBuy, { newBuy = it }, Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    RMInputField("Sell / $newUnit", newSell, { newSell = it }, Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(value = qty, onValueChange = { qty = it }, label = { Text("Quantity") },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    suffix = { Text(newUnit, color = PSRColors.Navy600, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)) })
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth().background(if (saveToDb) PSRColors.Navy600.copy(0.06f) else PSRColors.Grey50, RoundedCornerShape(10.dp))
                    .clickable { saveToDb = !saveToDb }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = saveToDb, onCheckedChange = { saveToDb = it }, colors = CheckboxDefaults.colors(checkedColor = PSRColors.Navy600))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Also save to catalogue", style = MaterialTheme.typography.labelLarge, color = if (saveToDb) PSRColors.Navy600 else PSRColors.Grey700)
                        Text(if (saveToDb) "Will be added as a product" else "One-off only", style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey500)
                    }
                }
                Spacer(Modifier.weight(1f))
                val b2 = newBuy.toDoubleOrNull() ?: 0.0
                val s2 = newSell.toDoubleOrNull() ?: (b2 * 1.2)
                val q2 = qty.toDoubleOrNull() ?: 0.0
                Button(onClick = {
                    if (newName.isBlank() || q2 <= 0) return@Button
                    if (saveToDb) {
                        val si = StockItem(name = newName, categoryId = 0L, unit = newUnit, sellPrice = s2,
                            supplierPrices = if (b2 > 0) listOf(SupplierPrice("Default", b2, true)) else emptyList())
                        onSaveNewItem(si); viewModel?.quickAddStockItem(si, true) {}
                    }
                    onSave(InvoiceItem(description = newName, unit = newUnit, qty = q2, buyPrice = b2, sellPrice = s2, lineTotal = q2 * s2, lineCost = q2 * b2, returnQty = 0.0, returnPrice = 0.0, returnNote = ""))
                }, enabled = newName.isNotBlank() && q2 > 0,
                    modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PSRColors.Navy600)
                ) { Text("Add to Invoice", style = MaterialTheme.typography.titleMedium) }
            }
        }
        return
    }

    // ── ITEM DETAIL FORM ─────────────────────────────────────────────────
    Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (existingItem == null) {
                    IconButton(onClick = { showPicker = true }) { Icon(Icons.Default.ArrowBack, null, tint = PSRColors.Navy600) }
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (existingItem == null) description.ifBlank { "Item Details" } else "Edit Item",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
            Spacer(Modifier.height(10.dp))
            ExposedDropdownMenuBox(expanded = showUnitMenu, onExpandedChange = { showUnitMenu = it }) {
                OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("Unit") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(showUnitMenu) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true,
                    leadingIcon = { Box(modifier = Modifier.background(PSRColors.Navy600.copy(0.1f), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) { Text(unit.take(6), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = PSRColors.Navy600) } },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PSRColors.Accent))
                ExposedDropdownMenu(expanded = showUnitMenu, onDismissRequest = { showUnitMenu = false }) {
                    units.forEach { u -> DropdownMenuItem(text = { Text(u) }, onClick = { unit = u; showUnitMenu = false }) }
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = qty, onValueChange = { qty = it }, label = { Text("Quantity") },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                suffix = { Text(unit, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = PSRColors.Navy600) })
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RMInputField("Buy / $unit", buyPrice, { buyPrice = it }, Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                Column(modifier = Modifier.weight(1f)) {
                    RMInputField("Sell / $unit", sellPrice, { sellPrice = it }, Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    // ── Customer price memory hint ─────────────────
                    if (description.isNotBlank() && customerId != null && customerId > 0) {
                        Spacer(Modifier.height(4.dp))
                        if (hasCustomerPrice) {
                            // Has a previous price for this customer
                            Surface(
                                color = PSRColors.Accent.copy(0.08f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.History, null,
                                            tint = PSRColors.Accent, modifier = Modifier.size(12.dp))
                                        Text("Last charged: $rememberedLabel",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = PSRColors.Accent)
                                        if (rememberedDate.isNotBlank())
                                            Text("· $rememberedDate",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = PSRColors.Grey500)
                                    }
                                    if (cataloguePrice > 0 && Math.abs(cataloguePrice - (sellPrice.toDoubleOrNull() ?: 0.0)) > 0.001) {
                                        Row(verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text("Catalogue: RM ${"%.2f".format(cataloguePrice)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = PSRColors.Grey400)
                                            // Quick swap chip
                                            Surface(
                                                onClick = { sellPrice = "%.2f".format(cataloguePrice) },
                                                color = PSRColors.Grey100,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text("Use catalogue",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = PSRColors.Grey600,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (description.isNotBlank()) {
                            // New item for this customer
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                modifier = Modifier.padding(top = 1.dp)) {
                                Icon(Icons.Default.NewReleases, null,
                                    tint = PSRColors.Warning, modifier = Modifier.size(12.dp))
                                Text("New item for this customer — using catalogue price",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PSRColors.Warning)
                            }
                        }
                    }
                }
            }
            if (buy > 0 || sell > 0) {
                Spacer(Modifier.height(10.dp))
                Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = if (profitPerUnit >= 0) PSRColors.Success.copy(0.06f) else PSRColors.Error.copy(0.06f))) {
                    Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Profit Breakdown", style = MaterialTheme.typography.labelSmall, color = PSRColors.Grey600)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            PriceChip("Per $unit", "RM ${"%.2f".format(profitPerUnit)}", if (profitPerUnit >= 0) PSRColors.Success else PSRColors.Error)
                            PriceChip("Margin", "${"%.1f".format(marginPct)}%", PSRColors.Accent)
                            if (qtyVal > 0 && sell > 0) { PriceChip("Total", "RM ${"%.2f".format(lineTotal)}", PSRColors.Navy900); PriceChip("Profit", "RM ${"%.2f".format(profit)}", if (profit >= 0) PSRColors.Success else PSRColors.Error) }
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))

            // ── Return / Refund section ──────────────────────────────
            HorizontalDivider(color = PSRColors.Divider, modifier = Modifier.padding(bottom = 8.dp))
            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { showReturnSection = !showReturnSection }
                    .padding(vertical = 6.dp)) {
                Icon(if (showReturnSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null, tint = PSRColors.Warning, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Return / Refund",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = PSRColors.Warning)
                Spacer(Modifier.weight(1f))
                if (!showReturnSection && (returnQtyStr.isNotBlank()))
                    Text("−RM ${"%.2f".format((returnQtyStr.toDoubleOrNull() ?: 0.0) * (returnPriceStr.toDoubleOrNull() ?: sell))}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = PSRColors.Warning)
            }
            AnimatedVisibility(visible = showReturnSection) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = returnQtyStr,
                            onValueChange = { returnQtyStr = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Return Qty") },
                            modifier = Modifier.weight(1f), singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                            suffix = { Text(unit, style = MaterialTheme.typography.labelSmall) }
                        )
                        OutlinedTextField(
                            value = returnPriceStr,
                            onValueChange = { returnPriceStr = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Price/unit") },
                            placeholder = { Text("%.2f".format(sell)) },
                            modifier = Modifier.weight(1f), singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                            prefix = { Text("RM ", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                    OutlinedTextField(
                        value = returnNote,
                        onValueChange = { returnNote = it },
                        label = { Text("Return description (shown italic on invoice)") },
                        placeholder = { Text("e.g. Return — damaged goods") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                    val retAmt = (returnQtyStr.toDoubleOrNull() ?: 0.0) * (returnPriceStr.toDoubleOrNull() ?: sell)
                    if (retAmt > 0) {
                        Text("−RM ${"%.2f".format(retAmt)} will be deducted from this line",
                            style = MaterialTheme.typography.labelSmall, color = PSRColors.Warning)
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text("Cancel") }
                Button(onClick = {
                    if (description.isBlank() || qtyVal <= 0) return@Button
                    val stockId = selectedStockItemId
                    if (customerId != null && customerId > 0 && stockId != null && sell > 0 && viewModel != null) viewModel.setCustomerItemPrice(customerId, stockId, sell)
                    val retQty   = returnQtyStr.toDoubleOrNull() ?: 0.0
                    val retPrice = returnPriceStr.toDoubleOrNull() ?: 0.0
                    onSave(InvoiceItem(
                        stockItemId = selectedStockItemId,
                        description = description, unit = unit, qty = qtyVal,
                        buyPrice = buy, sellPrice = sell, lineTotal = lineTotal, lineCost = lineCost,
                        returnQty   = retQty,
                        returnPrice = retPrice,
                        returnNote  = if (retQty > 0) returnNote.ifBlank { "Return ${retQty} $unit" } else ""
                    ))
                }, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PSRColors.Navy600)) {
                    Text(if (existingItem == null) "Add" else "Update", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// INVOICE PREVIEW
// ─────────────────────────────────────────────

@Composable
fun InvoicePreviewDialog(settings: BusinessSettings, customer: CustomerSnapshot?, items: List<InvoiceItem>,
    invNo: String, date: Long, subtotal: Double, discount: Double, roundOff: Double, total: Double, notes: String, onDismiss: () -> Unit) {
    val dateStr = remember(date) { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(date)) }
    val config = LocalConfiguration.current

    Dialog(onDismissRequest=onDismiss, properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(modifier=Modifier.width(config.screenWidthDp.dp).padding(vertical=24.dp), shape=RoundedCornerShape(8.dp), color=Color.White) {
            Column(modifier=Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                Text("Invoice", style=MaterialTheme.typography.headlineLarge.copy(fontWeight=FontWeight.ExtraBold), modifier=Modifier.fillMaxWidth(), textAlign=TextAlign.Center, color=Color.Black)
                Spacer(Modifier.height(20.dp))
                Row(modifier=Modifier.fillMaxWidth()) {
                    Column(modifier=Modifier.weight(1f)) {
                        Text("Bill To:",style=MaterialTheme.typography.bodySmall.copy(fontWeight=FontWeight.Bold,fontStyle=FontStyle.Italic))
                        Text(customer?.name?:"Walk-in",style=MaterialTheme.typography.bodyMedium.copy(fontWeight=FontWeight.Bold))
                        if(customer?.company?.isNotBlank()==true)Text(customer.company,style=MaterialTheme.typography.bodySmall,color=Color.Gray)
                    }
                    Column(horizontalAlignment=Alignment.End,modifier=Modifier.weight(1f)) {
                        Text(settings.companyName,style=MaterialTheme.typography.bodyMedium.copy(fontWeight=FontWeight.Bold),textAlign=TextAlign.End)
                        if(settings.email.isNotBlank())Text(settings.email,style=MaterialTheme.typography.bodySmall,color=Color.Gray,textAlign=TextAlign.End)
                        if(settings.phone.isNotBlank())Text(settings.phone,style=MaterialTheme.typography.bodySmall,color=Color.Gray,textAlign=TextAlign.End)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Invoice No: $invNo",style=MaterialTheme.typography.bodySmall)
                Text("Dated: $dateStr",style=MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(12.dp))
                Box(modifier=Modifier.fillMaxWidth().border(1.dp,Color.Black)) {
                    Column {
                        Row(modifier=Modifier.fillMaxWidth().background(Color(0xFFF5F5F5)).padding(vertical=6.dp), verticalAlignment=Alignment.CenterVertically) {
                            Text("S.\nNo.",modifier=Modifier.width(30.dp),textAlign=TextAlign.Center,fontSize=9.sp,fontWeight=FontWeight.Bold)
                            VerticalDivider(modifier=Modifier.height(24.dp),color=Color.Black)
                            Text("Description",modifier=Modifier.weight(1f).padding(horizontal=4.dp),fontSize=9.sp,fontWeight=FontWeight.Bold)
                            VerticalDivider(modifier=Modifier.height(24.dp),color=Color.Black)
                            Text("Quantity",modifier=Modifier.width(45.dp),textAlign=TextAlign.End,fontSize=9.sp,fontWeight=FontWeight.Bold)
                            VerticalDivider(modifier=Modifier.height(24.dp),color=Color.Black)
                            Text("Unit",modifier=Modifier.width(30.dp),textAlign=TextAlign.Center,fontSize=9.sp,fontWeight=FontWeight.Bold)
                            VerticalDivider(modifier=Modifier.height(24.dp),color=Color.Black)
                            Text("Rate/\nUnit",modifier=Modifier.width(45.dp),textAlign=TextAlign.End,fontSize=9.sp,fontWeight=FontWeight.Bold)
                            VerticalDivider(modifier=Modifier.height(24.dp),color=Color.Black)
                            Box(modifier=Modifier.width(50.dp).padding(end=4.dp),contentAlignment=Alignment.CenterEnd){Text("Amount",fontSize=9.sp,fontWeight=FontWeight.Bold)}
                        }
                        HorizontalDivider(color=Color.Black)
                        items.forEachIndexed{idx,item->
                            Row(modifier=Modifier.fillMaxWidth().padding(vertical=4.dp),verticalAlignment=Alignment.CenterVertically){
                                Text("${idx+1}",modifier=Modifier.width(30.dp),textAlign=TextAlign.Center,fontSize=9.sp)
                                VerticalDivider(modifier=Modifier.height(16.dp),color=Color.LightGray)
                                Text(item.description,modifier=Modifier.weight(1f).padding(horizontal=4.dp),fontSize=9.sp)
                                VerticalDivider(modifier=Modifier.height(16.dp),color=Color.LightGray)
                                Text("%.2f".format(item.qty),modifier=Modifier.width(45.dp),textAlign=TextAlign.End,fontSize=9.sp)
                                VerticalDivider(modifier=Modifier.height(16.dp),color=Color.LightGray)
                                Text(item.unit,modifier=Modifier.width(30.dp),textAlign=TextAlign.Center,fontSize=9.sp)
                                VerticalDivider(modifier=Modifier.height(16.dp),color=Color.LightGray)
                                Text("%.2f".format(item.sellPrice),modifier=Modifier.width(45.dp),textAlign=TextAlign.End,fontSize=9.sp)
                                VerticalDivider(modifier=Modifier.height(16.dp),color=Color.LightGray)
                                Box(modifier=Modifier.width(50.dp).padding(end=4.dp),contentAlignment=Alignment.CenterEnd){Text("%.2f".format(item.lineTotal),fontSize=9.sp)}
                            }
                            HorizontalDivider(color=Color(0xFFEEEEEE))
                        }
                        Row(modifier=Modifier.fillMaxWidth()) {
                            Spacer(Modifier.weight(1f))
                            Column(modifier=Modifier.width(130.dp).drawBehind{drawLine(Color.Black,Offset(0f,0f),Offset(0f,size.height),1.dp.toPx())}) {
                                Row(modifier=Modifier.fillMaxWidth().padding(4.dp)){Text("Sub Total",modifier=Modifier.weight(1f),fontSize=9.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.End);Text("%.2f".format(subtotal),modifier=Modifier.width(50.dp),fontSize=9.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.End)}
                                if(discount>0){Row(modifier=Modifier.fillMaxWidth().padding(4.dp)){Text("Discount On Sale",modifier=Modifier.weight(1f),fontSize=9.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.End);Text("(%.2f)".format(discount),modifier=Modifier.width(50.dp),fontSize=9.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.End)}}
                                if(roundOff!=0.0){Row(modifier=Modifier.fillMaxWidth().padding(4.dp)){Text("Round-off",modifier=Modifier.weight(1f),fontSize=9.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.End);Text("%+.2f".format(roundOff),modifier=Modifier.width(50.dp),fontSize=9.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.End)}}
                                HorizontalDivider(color=Color.Black,modifier=Modifier.padding(horizontal=4.dp))
                                Row(modifier=Modifier.fillMaxWidth().padding(4.dp)){Text("Amount",modifier=Modifier.weight(1f),fontSize=10.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.End);Text("%.2f".format(total),modifier=Modifier.width(50.dp),fontSize=10.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.End)}
                            }
                        }
                        HorizontalDivider(color=Color.Black,thickness=1.5.dp)
                        Row(modifier=Modifier.fillMaxWidth().padding(horizontal=4.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically){
                            Text("Total",fontSize=10.sp,fontWeight=FontWeight.Bold)
                            Text("%.2f".format(items.sumOf{it.qty}),fontSize=10.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f).padding(start=8.dp))
                            Text("RM%.2f".format(total),fontSize=11.sp,fontWeight=FontWeight.ExtraBold)
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                Row(modifier=Modifier.fillMaxWidth()) {
                    Column(modifier=Modifier.weight(1f)){
                        val ft=notes.ifBlank{"All payments can be made via cash or bank transfer via ${settings.bankName} AC: ${settings.accountNumber}\nName: ${settings.accountName}"}
                        Text(ft,fontSize=9.sp,color=Color.DarkGray);Spacer(Modifier.height(8.dp));Text("Thank you.",fontSize=9.sp,color=Color.DarkGray)
                    }
                    Column(horizontalAlignment=Alignment.End){Text("Authorized",fontSize=9.sp,fontWeight=FontWeight.Bold);Text("Signatory",fontSize=9.sp,fontWeight=FontWeight.Bold)}
                }
                Spacer(Modifier.height(24.dp))
                // ── Export row ────────────────────────────────────────────
                val context = LocalContext.current
                val printer = remember { com.psrmart.app.util.InvoicePrinter(context) }
                val waPhone = customer?.phone?.filter { it.isDigit() } ?: ""
                val invoiceText = buildWhatsAppInvoiceText(
                    companyName = settings.companyName,
                    customerName = customer?.name ?: "Customer",
                    invNo = invNo, date = dateStr,
                    items = items, subtotal = subtotal,
                    discount = discount, roundOff = roundOff, total = total,
                    bankName = settings.bankName, accountNumber = settings.accountNumber,
                    notes = notes
                )
                val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
                var copied by remember { mutableStateOf(false) }

                // Row 1: PDF + PNG
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Need a fake Invoice to pass to printer — build from preview params
                    Button(
                        onClick = {
                            val fakeInv = Invoice(
                                invoiceNumber = invNo,
                                customerSnapshot = customer ?: CustomerSnapshot("Customer","","","",""),
                                items = items, subtotal = subtotal,
                                discountAmount = discount, roundOff = roundOff,
                                totalAmount = total, totalCost = 0.0, netProfit = 0.0,
                                issuedAt = date, notes = notes
                            )
                            printer.printInvoice(fakeInv, settings)
                        },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PSRColors.Navy700)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("PDF", style = MaterialTheme.typography.labelLarge)
                    }
                    OutlinedButton(
                        onClick = {
                            val fakeInv = Invoice(
                                invoiceNumber = invNo,
                                customerSnapshot = customer ?: CustomerSnapshot("Customer","","","",""),
                                items = items, subtotal = subtotal,
                                discountAmount = discount, roundOff = roundOff,
                                totalAmount = total, totalCost = 0.0, netProfit = 0.0,
                                issuedAt = date, notes = notes
                            )
                            printer.exportAsPng(fakeInv, settings)
                        },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, PSRColors.Navy600)
                    ) {
                        Icon(Icons.Default.Image, null, modifier = Modifier.size(16.dp), tint = PSRColors.Navy600)
                        Spacer(Modifier.width(6.dp))
                        Text("PNG", color = PSRColors.Navy600, style = MaterialTheme.typography.labelLarge)
                    }
                }
                Spacer(Modifier.height(8.dp))
                // Row 2: Copy Text + WhatsApp
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            clipboard.setText(androidx.compose.ui.text.AnnotatedString(invoiceText))
                            copied = true
                        },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(if (copied) Icons.Default.Check else Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp), tint = if (copied) PSRColors.Success else PSRColors.Grey600)
                        Spacer(Modifier.width(6.dp))
                        Text(if (copied) "Copied!" else "Copy Text", style = MaterialTheme.typography.labelLarge, color = if (copied) PSRColors.Success else PSRColors.Grey700)
                    }
                    Button(
                        onClick = {
                            if (waPhone.isNotBlank()) {
                                val phone = if (waPhone.startsWith("6")) waPhone else "6$waPhone"
                                val uri = Uri.parse("https://api.whatsapp.com/send?phone=$phone&text=${Uri.encode(invoiceText)}")
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                            } else {
                                val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, invoiceText) }
                                context.startActivity(Intent.createChooser(intent, "Share Invoice").apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                            }
                        },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                    ) {
                        Icon(Icons.Default.Chat, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (waPhone.isNotBlank()) "WhatsApp" else "Share", style = MaterialTheme.typography.labelLarge)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = PSRColors.Navy600)) { Text("Done") }
            }
        }
    }
}

// ─────────────────────────────────────────────
// CUSTOMER PICKER SHEET
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerPickerSheet(customers: List<Customer>, invoices: List<Invoice>, onSelect: (Customer) -> Unit, onWalkIn: () -> Unit, onDismiss: () -> Unit, onAddNew: (Customer) -> Unit,
    onDelete: (Customer) -> Unit = {},
    onPayInvoice: (com.psrmart.app.data.model.Invoice, Double, Long, FinancialAccount) -> Unit = { _, _, _, _ -> }) {
    var searchQuery by remember { mutableStateOf("") }
    var showAdd     by remember { mutableStateOf(false) }
    var viewLedgerCustomer by remember { mutableStateOf<Customer?>(null) }
    var deleteTarget by remember { mutableStateOf<Customer?>(null) }
    val filtered = remember(customers, searchQuery) {
        customers.filter { it.name.contains(searchQuery, true) || it.company.contains(searchQuery, true) }
    }

    // Pre-compute per-customer stats
    val customerStats = remember(invoices, customers) {
        customers.associate { c ->
            val cInvoices = invoices.filter { it.customerId == c.id || it.customerSnapshot.name == c.name }
            val outstanding = cInvoices.filter { it.status != InvoiceStatus.PAID && it.status != InvoiceStatus.CANCELLED }
                .sumOf { it.totalAmount - it.amountPaid }
            val lastInvoice = cInvoices.maxByOrNull { it.issuedAt }
            c.id to Pair(outstanding, lastInvoice)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp)) {

            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Select Customer", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                    Text("${customers.size} customers", style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey500)
                }
                OutlinedButton(onClick = { showAdd = true }, shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                    Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("New", style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Search
            OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it },
                placeholder = { Text("Search name or company…") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = PSRColors.Grey400) },
                trailingIcon = { if (searchQuery.isNotBlank()) IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, null, modifier = Modifier.size(16.dp)) } },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PSRColors.Navy600))

            Spacer(Modifier.height(8.dp))

            // Walk-in option
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PSRColors.Grey50)
                    .clickable(onClick = onWalkIn)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.size(42.dp).background(PSRColors.Grey200, CircleShape), contentAlignment = Alignment.Center) {
                    Text("🚶", style = MaterialTheme.typography.titleLarge)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Walk-in Customer", style = MaterialTheme.typography.titleSmall)
                    Text("No account record", style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey500)
                }
                Icon(Icons.Default.ChevronRight, null, tint = PSRColors.Grey300)
            }

            HorizontalDivider(color = PSRColors.Divider, modifier = Modifier.padding(vertical = 8.dp))

            // Customer list
            LazyColumn(modifier = Modifier.heightIn(max = 380.dp)) {
                if (filtered.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No customers found", color = PSRColors.Grey400)
                        }
                    }
                } else {
                    items(filtered, key = { it.id }) { c ->
                        val (outstanding, lastInv) = customerStats[c.id] ?: Pair(0.0, null)
                        val hasDebt = outstanding > 0.01
                        val lastInvStr = lastInv?.let {
                            val dateStr = remember(it.issuedAt) { SimpleDateFormat("d MMM yy", Locale.getDefault()).format(Date(it.issuedAt)) }
                            "${it.invoiceNumber}  ·  RM %.2f  ·  $dateStr".format(it.totalAmount)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelect(c) }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Avatar initial
                            Box(
                                modifier = Modifier.size(44.dp)
                                    .background(if (hasDebt) PSRColors.Error.copy(0.12f) else PSRColors.Navy600.copy(0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(c.name.firstOrNull()?.uppercase() ?: "?",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontFamily = InterFamily),
                                    color = if (hasDebt) PSRColors.Error else PSRColors.Navy600)
                            }

                            // Name + details
                            Column(modifier = Modifier.weight(1f)) {
                                Text(c.name, style = MaterialTheme.typography.titleSmall)
                                if (c.company.isNotBlank()) {
                                    Text(c.company, style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey500, maxLines = 1)
                                }
                                if (lastInvStr != null) {
                                    Text("Last: $lastInvStr", style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp), color = PSRColors.Grey400)
                                }
                            }

                            // Outstanding balance pill
                            Column(horizontalAlignment = Alignment.End) {
                                if (hasDebt) {
                                    Column(
                                        modifier = Modifier
                                            .background(PSRColors.Error.copy(0.1f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text("OWES", style = PSRTextStyles.Caption, color = PSRColors.Error)
                                        Text("RM %.2f".format(outstanding),
                                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = MontserratFamily, fontWeight = FontWeight.Bold),
                                            color = PSRColors.Error)
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .background(PSRColors.Success.copy(0.1f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("✓  Clear", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = PSRColors.Success)
                                    }
                                    if (lastInv == null) Text("no invoices", style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp), color = PSRColors.Grey400)
                                }
                            }
                            // Ledger icon — tap to view customer history without selecting
                            IconButton(
                                onClick = { viewLedgerCustomer = c },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Outlined.History, "View ledger", tint = PSRColors.Grey400, modifier = Modifier.size(18.dp))
                            }
                            // Delete icon
                            IconButton(
                                onClick = { deleteTarget = c },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, "Delete customer", tint = PSRColors.Error.copy(0.6f), modifier = Modifier.size(18.dp))
                            }
                        }
                        HorizontalDivider(color = PSRColors.Divider, modifier = Modifier.padding(start = 56.dp))
                    }
                }
            }
        }
    }
    if (showAdd) AddCustomerDialog(onDismiss = { showAdd = false }, onSave = onAddNew)

    // Delete confirmation
    deleteTarget?.let { c ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete Customer?") },
            text = {
                Column {
                    Text("Remove ${c.name} from your customer list?")
                    Spacer(Modifier.height(4.dp))
                    Text("Their past invoices will remain but the customer profile will be gone.",
                        style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey500)
                }
            },
            confirmButton = {
                Button(
                    onClick = { onDelete(c); deleteTarget = null },
                    colors = ButtonDefaults.buttonColors(containerColor = PSRColors.Error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }

    // Customer ledger sheet
    viewLedgerCustomer?.let { c ->
        var ledgerPayTarget by remember { mutableStateOf<com.psrmart.app.data.model.Invoice?>(null) }
        val ledgerSettings = com.psrmart.app.data.model.BusinessSettings() // settings not needed for display here
        CustomerLedgerSheet(
            customer  = c,
            invoices  = invoices,
            settings  = ledgerSettings,
            onDismiss = { viewLedgerCustomer = null },
            onPayInvoice = { inv -> ledgerPayTarget = inv }
        )
        ledgerPayTarget?.let { inv ->
            PaymentEntryDialog(
                invoice  = inv,
                onDismiss = { ledgerPayTarget = null },
                onSave    = { amount, date, account ->
                    onPayInvoice(inv, amount, date, account)
                    playCashSound(context)
                    ledgerPayTarget = null
                }
            )
        }
    }
}

// ─────────────────────────────────────────────
// ADD CUSTOMER
// ─────────────────────────────────────────────

@Composable
fun AddCustomerDialog(onDismiss: () -> Unit, onSave: (Customer) -> Unit) {
    var name by remember{mutableStateOf("")};var company by remember{mutableStateOf("")}
    var phone by remember{mutableStateOf("")};var email by remember{mutableStateOf("")}
    var address by remember{mutableStateOf("")};var customFields by remember{mutableStateOf(listOf<CustomField>())}
    var newFieldLabel by remember{mutableStateOf("")}

    AlertDialog(onDismissRequest=onDismiss,title={Text("New Customer")},text={
        Column(modifier=Modifier.verticalScroll(rememberScrollState())){
            listOf(Triple(name,"Name *"){v:String->name=v},Triple(company,"Company"){v:String->company=v},Triple(phone,"Phone"){v:String->phone=v},Triple(email,"Email"){v:String->email=v},Triple(address,"Address"){v:String->address=v}).forEach{(value,label,onChange)->
                OutlinedTextField(value=value,onValueChange=onChange,label={Text(label)},modifier=Modifier.fillMaxWidth().padding(vertical=3.dp),shape=RoundedCornerShape(10.dp),singleLine=true)}
            Spacer(Modifier.height(10.dp))
            Text("Custom Fields",style=MaterialTheme.typography.labelMedium,color=PSRColors.Grey600)
            customFields.forEachIndexed{idx,field->Row(verticalAlignment=Alignment.CenterVertically){Text(field.label,modifier=Modifier.weight(1f),style=MaterialTheme.typography.bodySmall);IconButton(onClick={customFields=customFields.toMutableList().also{it.removeAt(idx)}}){Icon(Icons.Default.Close,null,modifier=Modifier.size(16.dp))}}}
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){
                OutlinedTextField(value=newFieldLabel,onValueChange={newFieldLabel=it},label={Text("Field Name")},modifier=Modifier.weight(1f),shape=RoundedCornerShape(10.dp),singleLine=true)
                IconButton(onClick={if(newFieldLabel.isNotBlank()){customFields=customFields+CustomField(newFieldLabel,"");newFieldLabel=""}}){Icon(Icons.Default.AddCircle,null,tint=PSRColors.Accent)}
            }
        }
    },
    confirmButton={Button(onClick={if(name.isBlank())return@Button;onSave(Customer(name=name,company=company,phone=phone,email=email,address=address,customFields=customFields))},colors=ButtonDefaults.buttonColors(containerColor=PSRColors.Navy600)){Text("Save")}},
    dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}})
}

// ─────────────────────────────────────────────
// HISTORY TAB — with edit/delete
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceHistoryTab(viewModel: PSRViewModel, onEdit: (Invoice) -> Unit) {
    val invoices  by viewModel.invoices.collectAsState()
    val settings  by viewModel.businessSettings.collectAsState()
    val navTarget by viewModel.navigateToInvoiceId.collectAsState()
    val context   = LocalContext.current
    val printer   = remember { InvoicePrinter(context) }

    var invoiceToPay       by remember { mutableStateOf<Invoice?>(null) }
    var invoiceToPreview   by remember { mutableStateOf<Invoice?>(null) }
    var invoiceToDelete    by remember { mutableStateOf<Invoice?>(null) }
    var expandedInvoiceId  by remember { mutableStateOf<Long?>(null) }
    var showUnpaidOnly     by remember { mutableStateOf(false) }
    var sortMode           by remember { mutableStateOf(0) }
    var filterDate         by remember { mutableStateOf<Long?>(null) }
    var showDatePicker     by remember { mutableStateOf(false) }

    // ── Bulk-select state ──────────────────────────────────────────────
    var selectMode         by remember { mutableStateOf(false) }
    var selectedIds        by remember { mutableStateOf(setOf<Long>()) }
    var showBulkConfirm    by remember { mutableStateOf(false) }
    var showDeleteRangeSheet by remember { mutableStateOf(false) }

    LaunchedEffect(navTarget) { if (navTarget != null) expandedInvoiceId = navTarget }

    // When leaving select mode, clear selection
    LaunchedEffect(selectMode) { if (!selectMode) selectedIds = emptySet() }

    val filteredInvoices = remember(invoices, showUnpaidOnly, sortMode, filterDate) {
        var list = invoices
        if (showUnpaidOnly) list = list.filter { it.status != InvoiceStatus.PAID }
        if (filterDate != null) {
            val cal = Calendar.getInstance().apply { timeInMillis = filterDate!! }
            val year = cal.get(Calendar.YEAR); val day = cal.get(Calendar.DAY_OF_YEAR)
            list = list.filter { val ic = Calendar.getInstance().apply { timeInMillis = it.issuedAt }; ic.get(Calendar.YEAR) == year && ic.get(Calendar.DAY_OF_YEAR) == day }
        }
        if (sortMode == 1) list.sortedWith(compareBy({ it.customerSnapshot.name }, { -it.issuedAt }))
        else list.sortedByDescending { it.issuedAt }
    }

    val allVisibleSelected = filteredInvoices.isNotEmpty() && filteredInvoices.all { it.id in selectedIds }
    val selectedInvoices   = filteredInvoices.filter { it.id in selectedIds }
    val paidSelected       = selectedInvoices.count { it.status == InvoiceStatus.PAID }
    val unpaidSelected     = selectedInvoices.count { it.status != InvoiceStatus.PAID }

    Column(modifier = Modifier.fillMaxSize()) {

        // ── Toolbar ─────────────────────────────────────────────────────
        Row(modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!selectMode) {
                FilterChip(selected = showUnpaidOnly, onClick = { showUnpaidOnly = !showUnpaidOnly },
                    label = { Text("Unpaid Only", fontSize = 10.sp) })
                AssistChip(onClick = { sortMode = if (sortMode == 0) 1 else 0 },
                    label = { Text(if (sortMode == 0) "Sort: Date" else "Sort: Customer", fontSize = 10.sp) },
                    leadingIcon = { Icon(Icons.Default.Sort, null, modifier = Modifier.size(14.dp)) })
                AssistChip(onClick = { showDatePicker = true },
                    label = { Text(if (filterDate == null) "All Dates" else remember(filterDate) { SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(filterDate!!)) }, fontSize = 10.sp) },
                    leadingIcon = { Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(14.dp)) },
                    trailingIcon = { if (filterDate != null) Icon(Icons.Default.Close, null, modifier = Modifier.size(12.dp).clickable { filterDate = null }) })
                Spacer(Modifier.weight(1f))
                // Delete range button
                IconButton(onClick = { showDeleteRangeSheet = true }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.DeleteSweep, "Delete range", tint = PSRColors.Grey500, modifier = Modifier.size(20.dp))
                }
                // Select mode toggle
                IconButton(onClick = { selectMode = true }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.PlaylistAddCheck, "Select", tint = PSRColors.Grey500, modifier = Modifier.size(20.dp))
                }
            } else {
                // Select mode toolbar
                Checkbox(checked = allVisibleSelected, onCheckedChange = {
                    selectedIds = if (allVisibleSelected) emptySet() else filteredInvoices.map { it.id }.toSet()
                }, modifier = Modifier.size(36.dp))
                Text(
                    if (selectedIds.isEmpty()) "Tap to select" else "${selectedIds.size} selected",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selectedIds.isEmpty()) PSRColors.Grey500 else PSRColors.Navy700,
                    modifier = Modifier.weight(1f)
                )
                if (selectedIds.isNotEmpty()) {
                    IconButton(onClick = { showBulkConfirm = true }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.DeleteSweep, null, tint = PSRColors.Error, modifier = Modifier.size(22.dp))
                    }
                }
                TextButton(onClick = { selectMode = false }) { Text("Cancel") }
            }
        }

        if (filteredInvoices.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(icon = Icons.Outlined.ReceiptLong, title = "No results", subtitle = "Try changing filters")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                var lastHeader = ""
                items(filteredInvoices, key = { it.id }) { invoice ->
                    if (sortMode == 1 && invoice.customerSnapshot.name != lastHeader) {
                        lastHeader = invoice.customerSnapshot.name
                        Text(lastHeader, style = MaterialTheme.typography.labelLarge, color = PSRColors.Navy600,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                    }
                    val dateStr = remember(invoice.issuedAt) { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(invoice.issuedAt)) }
                    val isExpanded = !selectMode && expandedInvoiceId == invoice.id
                    val isSelected = invoice.id in selectedIds

                    Card(
                        onClick = {
                            if (selectMode) {
                                selectedIds = if (isSelected) selectedIds - invoice.id else selectedIds + invoice.id
                            } else {
                                expandedInvoiceId = if (isExpanded) null else invoice.id
                                if (!isExpanded && navTarget == invoice.id) viewModel.clearNavigation()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isSelected) PSRColors.Navy600.copy(0.06f) else PSRColors.Card),
                        elevation = CardDefaults.cardElevation(if (isExpanded) 4.dp else 1.dp),
                        border = when {
                            isSelected   -> BorderStroke(2.dp, PSRColors.Navy600)
                            navTarget == invoice.id -> BorderStroke(2.dp, PSRColors.Accent)
                            else         -> null
                        }
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            // Checkbox in select mode
                            if (selectMode) {
                                Checkbox(checked = isSelected, onCheckedChange = {
                                    selectedIds = if (isSelected) selectedIds - invoice.id else selectedIds + invoice.id
                                }, modifier = Modifier.size(36.dp))
                                Spacer(Modifier.width(4.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(invoice.invoiceNumber, style = MaterialTheme.typography.labelLarge, color = PSRColors.Accent)
                                        Text(invoice.customerSnapshot.name, style = MaterialTheme.typography.titleMedium, color = PSRColors.Navy900)
                                        Text("Dated: $dateStr", style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey600)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("RM %.2f".format(invoice.totalAmount), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = PSRColors.Navy900)
                                        if (invoice.amountPaid > 0 && invoice.status != InvoiceStatus.PAID) {
                                            Text("Paid: RM %.2f".format(invoice.amountPaid), style = MaterialTheme.typography.labelSmall, color = PSRColors.Success)
                                            Text("Due: RM %.2f".format(invoice.totalAmount - invoice.amountPaid), style = MaterialTheme.typography.labelSmall, color = PSRColors.Error)
                                        } else {
                                            Text("+RM %.2f profit".format(invoice.netProfit), style = MaterialTheme.typography.labelSmall,
                                                color = if (invoice.netProfit >= 0) PSRColors.Success else PSRColors.Error)
                                        }
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) { StatusBadge(invoice.status.name); Spacer(Modifier.weight(1f)) }

                                if (isExpanded) {
                                    Spacer(Modifier.height(16.dp))
                                    HorizontalDivider(color = PSRColors.Grey100)
                                    Spacer(Modifier.height(12.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(onClick = { invoiceToPreview = invoice }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) { Icon(Icons.Default.Visibility, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("View") }
                                        OutlinedButton(onClick = { onEdit(invoice) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) { Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Edit") }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (invoice.status != InvoiceStatus.PAID) {
                                            Button(onClick = { invoiceToPay = invoice }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = PSRColors.Success)) {
                                                Icon(Icons.Default.Payment, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Paid")
                                            }
                                        } else {
                                            OutlinedButton(onClick = { viewModel.markInvoiceUnpaid(invoice) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PSRColors.Error)) {
                                                Icon(Icons.Default.Undo, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Unpaid")
                                            }
                                        }
                                        OutlinedButton(onClick = { invoiceToDelete = invoice }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PSRColors.Error)) {
                                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Delete")
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        TextButton(onClick = { printer.printInvoice(invoice, settings) }, modifier = Modifier.weight(1f)) {
                                            Icon(Icons.Default.PictureAsPdf, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("PDF", style = MaterialTheme.typography.labelMedium)
                                        }
                                        val ctx = LocalContext.current
                                        val histPhone = invoice.customerSnapshot.phone.filter { it.isDigit() }
                                        TextButton(onClick = {
                                            val invoiceText = buildWhatsAppInvoiceText(companyName = settings.companyName, customerName = invoice.customerSnapshot.name,
                                                invNo = invoice.invoiceNumber, date = remember(invoice.issuedAt) { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(invoice.issuedAt)) },
                                                items = invoice.items, subtotal = invoice.subtotal, discount = invoice.discountAmount, roundOff = invoice.roundOff,
                                                total = invoice.totalAmount, bankName = settings.bankName, accountNumber = settings.accountNumber, notes = invoice.notes)
                                            if (histPhone.isNotBlank()) {
                                                val phone = if (histPhone.startsWith("6")) histPhone else "6$histPhone"
                                                val uri = Uri.parse("https://api.whatsapp.com/send?phone=$phone&text=${Uri.encode(invoiceText)}")
                                                ctx.startActivity(Intent(Intent.ACTION_VIEW, uri).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                                            } else {
                                                val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, invoiceText) }
                                                ctx.startActivity(Intent.createChooser(intent, "Share Invoice").apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                                            }
                                        }, modifier = Modifier.weight(1f)) {
                                            Icon(Icons.Default.Chat, null, modifier = Modifier.size(16.dp), tint = Color(0xFF25D366))
                                            Spacer(Modifier.width(6.dp))
                                            Text(if (histPhone.isNotBlank()) "WhatsApp" else "Share", style = MaterialTheme.typography.labelMedium, color = Color(0xFF25D366))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Date picker ──────────────────────────────────────────────────────
    if (showDatePicker) {
        val dps = rememberDatePickerState()
        DatePickerDialog(onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { filterDate = dps.selectedDateMillis; showDatePicker = false }) { Text("OK") } }
        ) { DatePicker(state = dps) }
    }

    // ── Single invoice delete ─────────────────────────────────────────────
    invoiceToDelete?.let { inv ->
        AlertDialog(
            onDismissRequest = { invoiceToDelete = null },
            title = { Text("Delete Invoice?") },
            text = {
                Column {
                    Text("${inv.invoiceNumber} — ${inv.customerSnapshot.name}", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(6.dp))
                    if (inv.status == InvoiceStatus.PAID) {
                        Text("This invoice is paid. Your balance sheet will NOT be affected — the money you received stays in your ledger.",
                            style = MaterialTheme.typography.bodySmall, color = PSRColors.Success)
                    } else {
                        Text("Any partial payments linked to this invoice will be reversed from your balance.",
                            style = MaterialTheme.typography.bodySmall, color = PSRColors.Warning)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (inv.status == InvoiceStatus.PAID) viewModel.deleteInvoiceKeepPayments(inv)
                    else viewModel.deleteInvoice(inv)
                    invoiceToDelete = null
                }, colors = ButtonDefaults.buttonColors(containerColor = PSRColors.Error)) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { invoiceToDelete = null }) { Text("Cancel") } }
        )
    }

    // ── Bulk delete confirm ───────────────────────────────────────────────
    if (showBulkConfirm && selectedInvoices.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showBulkConfirm = false },
            title = { Text("Delete ${selectedInvoices.size} invoices?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (paidSelected > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.CheckCircle, null, tint = PSRColors.Success, modifier = Modifier.size(16.dp))
                            Text("$paidSelected paid — balance unaffected (money already received)",
                                style = MaterialTheme.typography.bodySmall, color = PSRColors.Success)
                        }
                    }
                    if (unpaidSelected > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Warning, null, tint = PSRColors.Warning, modifier = Modifier.size(16.dp))
                            Text("$unpaidSelected unpaid — partial payments will be reversed",
                                style = MaterialTheme.typography.bodySmall, color = PSRColors.Warning)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("This cannot be undone.", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.bulkDeleteInvoices(selectedInvoices)
                    showBulkConfirm = false
                    selectMode = false
                }, colors = ButtonDefaults.buttonColors(containerColor = PSRColors.Error)) {
                    Text("Delete ${selectedInvoices.size}")
                }
            },
            dismissButton = { TextButton(onClick = { showBulkConfirm = false }) { Text("Cancel") } }
        )
    }

    // ── Invoice dialogs ──────────────────────────────────────────────────
    invoiceToPreview?.let { inv ->
        InvoicePreviewDialog(settings = settings, customer = inv.customerSnapshot, items = inv.items, invNo = inv.invoiceNumber,
            date = inv.issuedAt, subtotal = inv.subtotal, discount = inv.discountAmount, roundOff = inv.roundOff,
            total = inv.totalAmount, notes = inv.notes, onDismiss = { invoiceToPreview = null })
    }

    val ctx = LocalContext.current
    invoiceToPay?.let { inv ->
        PaymentEntryDialog(invoice = inv, onDismiss = { invoiceToPay = null },
            onSave = { amount, date, account ->
                viewModel.processInvoicePayment(inv, amount, date, account)
                playCashSound(ctx)
                invoiceToPay = null
            },
            settings = settings)
    }

    if (showDeleteRangeSheet) {
        DeleteRangeSheet(
            allInvoices = invoices,
            onDismiss   = { showDeleteRangeSheet = false },
            onDelete    = { toDelete ->
                viewModel.bulkDeleteInvoices(toDelete)
                showDeleteRangeSheet = false
            }
        )
    }
}

// ─────────────────────────────────────────────
// DELETE RANGE SHEET
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteRangeSheet(
    allInvoices: List<Invoice>,
    onDismiss: () -> Unit,
    onDelete: (List<Invoice>) -> Unit
) {
    // ── Period options ───────────────────────────────────────────────
    // 0 = This week, 1 = Last week, 2 = These 2 weeks, 3 = This month
    var selectedPeriod by remember { mutableStateOf(0) }
    // 0 = All, 1 = Paid only, 2 = Unpaid only
    var selectedStatus by remember { mutableStateOf(0) }
    var showConfirm    by remember { mutableStateOf(false) }

    fun startOfWeek(weeksAgo: Int = 0): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0);      cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.WEEK_OF_YEAR, -weeksAgo)
        return cal.timeInMillis
    }

    val now = System.currentTimeMillis()
    val periodRange: Pair<Long, Long> = remember(selectedPeriod) {
        when (selectedPeriod) {
            0 -> startOfWeek(0) to now                    // This week
            1 -> startOfWeek(1) to startOfWeek(0) - 1    // Last week
            2 -> startOfWeek(1) to now                    // These 2 weeks
            3 -> {                                         // This month
                val cal = Calendar.getInstance()
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0);      cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis to now
            }
            else -> startOfWeek(0) to now
        }
    }

    val (rangeStart, rangeEnd) = periodRange

    val matchingInvoices = remember(allInvoices, selectedPeriod, selectedStatus, periodRange) {
        allInvoices
            .filter { it.issuedAt in rangeStart..rangeEnd }
            .filter { inv ->
                when (selectedStatus) {
                    1 -> inv.status == InvoiceStatus.PAID
                    2 -> inv.status != InvoiceStatus.PAID
                    else -> true
                }
            }
    }

    val paidCount   = matchingInvoices.count { it.status == InvoiceStatus.PAID }
    val unpaidCount = matchingInvoices.count { it.status != InvoiceStatus.PAID }
    val totalValue  = matchingInvoices.sumOf { it.totalAmount }

    val sdf = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }
    val rangeLabel = "${sdf.format(Date(rangeStart))} – ${sdf.format(Date(rangeEnd))}"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = Color.White,
        modifier         = Modifier.fillMaxHeight(0.75f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp)) {

            Text("Delete Invoice Range",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
            Text("Remove invoices from a specific period. Paid invoices won't affect your balance.",
                style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey500,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))

            // ── Period selector ──────────────────────────────────────
            Text("Period", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = PSRColors.Grey600)
            Spacer(Modifier.height(8.dp))
            val periods = listOf("This week", "Last week", "These 2 weeks", "This month")
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                periods.chunked(2).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { label ->
                            val idx = periods.indexOf(label)
                            val selected = selectedPeriod == idx
                            Surface(
                                onClick = { selectedPeriod = idx },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) PSRColors.Navy600 else PSRColors.Grey50,
                                border = if (selected) null else BorderStroke(1.dp, PSRColors.Grey200)
                            ) {
                                Text(label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) Color.White else PSRColors.Navy700,
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Status selector ──────────────────────────────────────
            Text("Which invoices", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = PSRColors.Grey600)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth().background(PSRColors.Grey100, RoundedCornerShape(10.dp)).padding(4.dp)) {
                listOf("All", "Paid only", "Unpaid only").forEachIndexed { idx, label ->
                    val selected = selectedStatus == idx
                    Box(
                        modifier = Modifier.weight(1f)
                            .background(if (selected) Color.White else Color.Transparent, RoundedCornerShape(8.dp))
                            .clickable { selectedStatus = idx }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label,
                            style = MaterialTheme.typography.labelMedium,
                            color = when {
                                !selected -> PSRColors.Grey500
                                idx == 1  -> PSRColors.Success
                                idx == 2  -> PSRColors.Warning
                                else      -> PSRColors.Navy600
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Preview card ──────────────────────────────────────────
            Surface(
                color = if (matchingInvoices.isEmpty()) PSRColors.Grey50
                        else PSRColors.Error.copy(0.05f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp,
                    if (matchingInvoices.isEmpty()) PSRColors.Grey200
                    else PSRColors.Error.copy(0.2f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(rangeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = PSRColors.Grey500)
                    if (matchingInvoices.isEmpty()) {
                        Text("No invoices in this range",
                            style = MaterialTheme.typography.titleSmall,
                            color = PSRColors.Grey400)
                    } else {
                        Text("${matchingInvoices.size} invoice${if (matchingInvoices.size != 1) "s" else ""} — RM ${"%.2f".format(totalValue)} total",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = PSRColors.Error)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 2.dp)) {
                            if (paidCount > 0) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.CheckCircle, null, tint = PSRColors.Success, modifier = Modifier.size(12.dp))
                                    Text("$paidCount paid — balance safe",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PSRColors.Success)
                                }
                            }
                            if (unpaidCount > 0) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.Warning, null, tint = PSRColors.Warning, modifier = Modifier.size(12.dp))
                                    Text("$unpaidCount unpaid — payments reversed",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PSRColors.Warning)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = { if (matchingInvoices.isNotEmpty()) showConfirm = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (matchingInvoices.isEmpty()) PSRColors.Grey300 else PSRColors.Error),
                enabled = matchingInvoices.isNotEmpty()
            ) {
                Icon(Icons.Default.DeleteSweep, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (matchingInvoices.isEmpty()) "Nothing to delete"
                    else "Delete ${matchingInvoices.size} invoice${if (matchingInvoices.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Confirm Delete") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Delete ${matchingInvoices.size} invoices from $rangeLabel?",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    if (paidCount > 0) {
                        Text("✓ $paidCount paid — your balance sheet is unaffected",
                            style = MaterialTheme.typography.bodySmall, color = PSRColors.Success)
                    }
                    if (unpaidCount > 0) {
                        Text("⚠ $unpaidCount unpaid — partial payments will be reversed",
                            style = MaterialTheme.typography.bodySmall, color = PSRColors.Warning)
                    }
                    Text("Cannot be undone.",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = PSRColors.Error, modifier = Modifier.padding(top = 4.dp))
                }
            },
            confirmButton = {
                Button(onClick = { onDelete(matchingInvoices); showConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PSRColors.Error)) {
                    Text("Delete ${matchingInvoices.size}")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Cancel") }
            }
        )
    }
}
@Composable
fun PaymentEntryDialog(invoice: Invoice, onDismiss: () -> Unit, onSave: (Double, Long, FinancialAccount) -> Unit, settings: com.psrmart.app.data.model.BusinessSettings = com.psrmart.app.data.model.BusinessSettings()) {
    val remainingBalance = invoice.totalAmount - invoice.amountPaid
    var paymentType      by remember { mutableStateOf("Full") }
    var amountInput      by remember { mutableStateOf("%.2f".format(remainingBalance)) }
    var paymentDate      by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var selectedAccount  by remember { mutableStateOf(FinancialAccount.MAYBANK) }
    var showDatePicker   by remember { mutableStateOf(false) }
    val dateStr = remember(paymentDate) { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(paymentDate)) }

    AlertDialog(onDismissRequest=onDismiss, title={Text("Record Payment")}, text={
        Column {
            Text("Invoice: ${invoice.invoiceNumber}",style=MaterialTheme.typography.labelLarge,color=PSRColors.Accent)
            Text("Customer: ${invoice.customerSnapshot.name}",style=MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(16.dp))
            Text("Account Received Into",style=MaterialTheme.typography.labelMedium,color=PSRColors.Grey600)
            Spacer(Modifier.height(8.dp))
            Row(modifier=Modifier.fillMaxWidth().background(PSRColors.Grey100,RoundedCornerShape(12.dp)).padding(4.dp)){
                FinancialAccount.entries.forEach{acc->val sel=selectedAccount==acc;Box(modifier=Modifier.weight(1f).background(if(sel)Color.White else Color.Transparent,RoundedCornerShape(10.dp)).clickable{selectedAccount=acc}.padding(vertical=8.dp),contentAlignment=Alignment.Center){Text(settings.labelFor(acc),style=MaterialTheme.typography.labelLarge,color=if(sel)PSRColors.Navy600 else PSRColors.Grey600)}}
            }
            Spacer(Modifier.height(16.dp))
            Text("Payment Type",style=MaterialTheme.typography.labelMedium,color=PSRColors.Grey600)
            Spacer(Modifier.height(8.dp))
            Row(modifier=Modifier.fillMaxWidth().background(PSRColors.Grey100,RoundedCornerShape(12.dp)).padding(4.dp)){
                listOf("Full","Partial").forEach{type->val sel=paymentType==type;Box(modifier=Modifier.weight(1f).background(if(sel)Color.White else Color.Transparent,RoundedCornerShape(10.dp)).clickable{paymentType=type;if(type=="Full")amountInput="%.2f".format(remainingBalance)}.padding(vertical=8.dp),contentAlignment=Alignment.Center){Text(type,style=MaterialTheme.typography.labelLarge,color=if(sel)PSRColors.Navy600 else PSRColors.Grey600)}}
            }
            Spacer(Modifier.height(16.dp))
            if(paymentType=="Partial"){
                RMInputField(label="Amount Received",value=amountInput,onValueChange={amountInput=it},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal))
                Text("Remaining balance: RM %.2f".format(remainingBalance),style=MaterialTheme.typography.labelSmall,color=PSRColors.Grey600,modifier=Modifier.padding(start=4.dp,top=4.dp))
            } else {
                Card(modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(12.dp),colors=CardDefaults.cardColors(containerColor=PSRColors.Navy600.copy(alpha=0.05f))){
                    Column(modifier=Modifier.padding(12.dp)){Text("Total to Pay",style=MaterialTheme.typography.labelSmall,color=PSRColors.Grey600);Text("RM %.2f".format(remainingBalance),style=MaterialTheme.typography.titleLarge.copy(fontWeight=FontWeight.Bold),color=PSRColors.Navy900)}
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Received On",style=MaterialTheme.typography.labelMedium,color=PSRColors.Grey600)
            Spacer(Modifier.height(8.dp))
            OutlinedCard(onClick={showDatePicker=true},shape=RoundedCornerShape(12.dp),colors=CardDefaults.outlinedCardColors(containerColor=Color.Transparent)){
                Row(modifier=Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.CalendarToday,null,modifier=Modifier.size(18.dp),tint=PSRColors.Navy600);Spacer(Modifier.width(12.dp));Text(dateStr,style=MaterialTheme.typography.bodyMedium)}
            }
        }
    },
    confirmButton={Button(onClick={val amt=if(paymentType=="Full")remainingBalance else(amountInput.toDoubleOrNull()?:0.0);if(amt>0)onSave(amt,paymentDate,selectedAccount)},colors=ButtonDefaults.buttonColors(containerColor=PSRColors.Navy600)){Text("Save Payment")}},
    dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}})

    if(showDatePicker){val dps=rememberDatePickerState(initialSelectedDateMillis=paymentDate);DatePickerDialog(onDismissRequest={showDatePicker=false},confirmButton={TextButton(onClick={dps.selectedDateMillis?.let{paymentDate=it};showDatePicker=false}){Text("OK")}}){DatePicker(state=dps)}}
}

// ─────────────────────────────────────────────
// WHATSAPP INVOICE TEXT BUILDER
// ─────────────────────────────────────────────

fun buildWhatsAppInvoiceText(
    companyName: String,
    customerName: String,
    invNo: String,
    date: String,
    items: List<com.psrmart.app.data.model.InvoiceItem>,
    subtotal: Double,
    discount: Double,
    roundOff: Double,
    total: Double,
    bankName: String,
    accountNumber: String,
    notes: String
): String = buildString {
    appendLine("*$companyName*")
    appendLine("─────────────────")
    appendLine("Invoice: *$invNo*")
    appendLine("Date: $date")
    appendLine("To: *$customerName*")
    appendLine()
    appendLine("*Items:*")
    items.forEach { item ->
        val line = "• ${item.description}  ${item.qty} ${item.unit} × RM${"%.2f".format(item.sellPrice)} = *RM${"%.2f".format(item.lineTotal)}*"
        appendLine(line)
    }
    appendLine("─────────────────")
    if (discount > 0 || roundOff != 0.0) {
        appendLine("Subtotal: RM${"%.2f".format(subtotal)}")
        if (discount > 0) appendLine("Discount: -RM${"%.2f".format(discount)}")
        if (roundOff != 0.0) appendLine("Round-off: %+.2f".format(roundOff))
    }
    appendLine("*TOTAL: RM${"%.2f".format(total)}*")
    appendLine()
    val payLine = notes.ifBlank { "Payment via $bankName · AC: $accountNumber" }
    appendLine(payLine)
    append("_Thank you!_ 🙏")
}

// ─────────────────────────────────────────────
// CUSTOMER LEDGER SHEET
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerLedgerSheet(
    customer: com.psrmart.app.data.model.Customer,
    invoices: List<com.psrmart.app.data.model.Invoice>,
    settings: com.psrmart.app.data.model.BusinessSettings,
    onDismiss: () -> Unit,
    onPayInvoice: (com.psrmart.app.data.model.Invoice) -> Unit
) {
    val context = LocalContext.current
    val sdf = remember { java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault()) }

    // Only this customer's invoices, newest first, last 60 days
    val cutoff = System.currentTimeMillis() - 60L * 24 * 60 * 60 * 1000
    val custInvoices = remember(invoices, customer.id) {
        invoices
            .filter { it.customerId == customer.id || it.customerSnapshot.name == customer.name }
            .sortedByDescending { it.issuedAt }
    }
    val recentInvoices = custInvoices.filter { it.issuedAt >= cutoff }
    val displayList = recentInvoices.ifEmpty { custInvoices.take(10) }

    val totalOwed = custInvoices
        .filter { it.status != com.psrmart.app.data.model.InvoiceStatus.PAID && it.status != com.psrmart.app.data.model.InvoiceStatus.CANCELLED }
        .sumOf { it.totalAmount - it.amountPaid }
    val totalBusiness = custInvoices.sumOf { it.totalAmount }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = androidx.compose.ui.graphics.Color.White,
        modifier = Modifier.fillMaxHeight(0.88f)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(PSRColors.Navy600, PSRColors.Navy700)
                    ))
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(PSRColors.White.copy(0.15f), androidx.compose.foundation.shape.CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            customer.name.firstOrNull()?.uppercase() ?: "?",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = PSRColors.White
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(customer.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = PSRColors.White)
                        if (customer.company.isNotBlank())
                            Text(customer.company, style = MaterialTheme.typography.bodySmall, color = PSRColors.White.copy(0.7f))
                        if (customer.phone.isNotBlank())
                            Text(customer.phone, style = MaterialTheme.typography.bodySmall, color = PSRColors.White.copy(0.7f))
                    }
                    // WhatsApp button if phone present
                    if (customer.phone.isNotBlank()) {
                        IconButton(
                            onClick = {
                                val digits = customer.phone.filter { it.isDigit() }
                                val phone = if (digits.startsWith("6")) digits else "6$digits"
                                val uri = Uri.parse("https://api.whatsapp.com/send?phone=$phone")
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                            },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF25D366).copy(0.2f))
                        ) {
                            Icon(Icons.Default.Chat, null, tint = PSRColors.White)
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Summary chips
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LedgerStatChip(
                        label = "Outstanding",
                        value = "RM ${"%.2f".format(totalOwed)}",
                        color = if (totalOwed > 0.01) PSRColors.Error else PSRColors.Success
                    )
                    LedgerStatChip(
                        label = "Total Business",
                        value = "RM ${"%.2f".format(totalBusiness)}",
                        color = PSRColors.White
                    )
                    LedgerStatChip(
                        label = "Invoices",
                        value = "${custInvoices.size}",
                        color = PSRColors.White
                    )
                }
            }

            // ── Invoice list ──────────────────────────────────────────────
            if (displayList.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Text("No invoices yet", style = MaterialTheme.typography.bodyMedium, color = PSRColors.Grey400)
                }
            } else {
                Text(
                    if (recentInvoices.isEmpty()) "Last ${displayList.size} invoices" else "Last 60 days",
                    style = MaterialTheme.typography.labelMedium,
                    color = PSRColors.Grey500,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                )
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(displayList, key = { it.id }) { inv ->
                        val isPaid = inv.status == com.psrmart.app.data.model.InvoiceStatus.PAID
                        val isPartial = inv.status == com.psrmart.app.data.model.InvoiceStatus.PARTIAL
                        val owing = inv.totalAmount - inv.amountPaid

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Status dot
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(
                                        when {
                                            isPaid    -> PSRColors.Success
                                            isPartial -> PSRColors.Warning
                                            else      -> PSRColors.Error
                                        },
                                        androidx.compose.foundation.shape.CircleShape
                                    )
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(inv.invoiceNumber, style = MaterialTheme.typography.labelLarge, color = PSRColors.Accent)
                                Text(sdf.format(java.util.Date(inv.issuedAt)), style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey500)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "RM ${"%.2f".format(inv.totalAmount)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = PSRColors.Navy900
                                )
                                if (!isPaid) {
                                    Text(
                                        if (isPartial) "owes RM ${"%.2f".format(owing)}" else "UNPAID",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isPartial) PSRColors.Warning else PSRColors.Error
                                    )
                                } else {
                                    Text("PAID ✓", style = MaterialTheme.typography.labelSmall, color = PSRColors.Success)
                                }
                            }
                            // Pay button for unpaid
                            if (!isPaid) {
                                TextButton(
                                    onClick = { onPayInvoice(inv) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("Pay", style = MaterialTheme.typography.labelMedium, color = PSRColors.Success)
                                }
                            }
                        }
                        HorizontalDivider(color = PSRColors.Divider, modifier = Modifier.padding(horizontal = 20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun LedgerStatChip(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(
        modifier = Modifier
            .background(PSRColors.White.copy(0.1f), androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = PSRColors.White.copy(0.65f))
        Text(value, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = color)
    }
}
