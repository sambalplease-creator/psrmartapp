@file:OptIn(ExperimentalMaterial3Api::class)
package com.psrmart.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.psrmart.app.data.model.*
import com.psrmart.app.ui.components.*
import com.psrmart.app.ui.theme.PSRColors
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import com.psrmart.app.viewmodel.FlushState
import com.psrmart.app.viewmodel.PSRViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SettingsScreen(viewModel: PSRViewModel) {
    val settings by viewModel.businessSettings.collectAsState()
    var localSettings by remember(settings) { mutableStateOf(settings) }
    var hasChanges by remember { mutableStateOf(false) }
    var newCustomFieldLabel by remember { mutableStateOf("") }

    fun update(s: BusinessSettings) { localSettings = s; hasChanges = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PSRColors.Surface)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(8.dp))

        // ── Company Info ───────────────────────────────
        SettingsSection(title = "Company Information") {
            SettingsTextField("Company Name", localSettings.companyName) {
                update(localSettings.copy(companyName = it))
            }
            SettingsTextField("Tagline / Subtitle", localSettings.tagline) {
                update(localSettings.copy(tagline = it))
            }
            SettingsTextField("Address", localSettings.address, maxLines = 3) {
                update(localSettings.copy(address = it))
            }
            SettingsTextField("Phone", localSettings.phone) {
                update(localSettings.copy(phone = it))
            }
            SettingsTextField("Email", localSettings.email) {
                update(localSettings.copy(email = it))
            }
            SettingsTextField("Website", localSettings.website) {
                update(localSettings.copy(website = it))
            }
        }

        // ── Bank Details ───────────────────────────────
        SettingsSection(title = "Payment Information") {
            SettingsTextField("Bank Name", localSettings.bankName) { update(localSettings.copy(bankName = it)) }
            SettingsTextField("Account Name", localSettings.accountName) { update(localSettings.copy(accountName = it)) }
            SettingsTextField("Account Number", localSettings.accountNumber) { update(localSettings.copy(accountNumber = it)) }
        }

        // ── Account Labels & Sub-accounts ─────────────
        SettingsSection(title = "Account Names & Sub-Accounts") {
            Text("Rename your 3 accounts to whatever makes sense for you. Leave sub-accounts blank if you don't need them — they only appear when filled in.",
                style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey500,
                modifier = Modifier.padding(bottom = 8.dp))

            // Maybank
            SettingsTextField("Account 1 Label (default: Maybank)", localSettings.maybankLabel) { update(localSettings.copy(maybankLabel = it)) }
            SettingsTextField("  Sub-account A (optional)", localSettings.maybankSub1) { update(localSettings.copy(maybankSub1 = it)) }
            SettingsTextField("  Sub-account B (optional)", localSettings.maybankSub2) { update(localSettings.copy(maybankSub2 = it)) }
            Spacer(Modifier.height(6.dp))

            // Cash
            SettingsTextField("Account 2 Label (default: Cash)", localSettings.cashLabel) { update(localSettings.copy(cashLabel = it)) }
            SettingsTextField("  Sub-account A (optional)", localSettings.cashSub1) { update(localSettings.copy(cashSub1 = it)) }
            SettingsTextField("  Sub-account B (optional)", localSettings.cashSub2) { update(localSettings.copy(cashSub2 = it)) }
            Spacer(Modifier.height(6.dp))

            // Savings
            SettingsTextField("Account 3 Label (default: Savings)", localSettings.savingsLabel) { update(localSettings.copy(savingsLabel = it)) }
            SettingsTextField("  Sub-account A (optional)", localSettings.savingsSub1) { update(localSettings.copy(savingsSub1 = it)) }
            SettingsTextField("  Sub-account B (optional)", localSettings.savingsSub2) { update(localSettings.copy(savingsSub2 = it)) }
        }

        // ── Invoice Numbering ──────────────────────────
        SettingsSection(title = "Invoice Numbering") {
            SettingsTextField("Invoice Prefix", localSettings.invoicePrefix) {
                update(localSettings.copy(invoicePrefix = it))
            }
            SettingsTextField("Next Number", localSettings.invoiceNextNumber.toString()) {
                update(localSettings.copy(invoiceNextNumber = it.toIntOrNull() ?: localSettings.invoiceNextNumber))
            }
            // Preview
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = PSRColors.Navy600.copy(alpha = 0.06f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Receipt, contentDescription = null,
                        tint = PSRColors.Accent, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Next invoice will be: ", style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey600)
                    Text(
                        "${localSettings.invoicePrefix}-${localSettings.invoiceNextNumber.toString().padStart(4, '0')}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = PSRColors.Accent
                    )
                }
            }
        }

        // ── Invoice Defaults ────────────────────────────
        SettingsSection(title = "Invoice Defaults") {
            SettingsTextField("Payment Terms", localSettings.defaultPaymentTerms) {
                update(localSettings.copy(defaultPaymentTerms = it))
            }
            SettingsTextField("Default Footer Notes", localSettings.defaultNotes, maxLines = 3) {
                update(localSettings.copy(defaultNotes = it))
            }
            SettingsTextField("Tax Label (e.g. GST, SST)", localSettings.taxLabel) {
                update(localSettings.copy(taxLabel = it))
            }
            SettingsTextField("Default Tax % (0 = none)", localSettings.defaultTaxPercent.toString()) {
                update(localSettings.copy(defaultTaxPercent = it.toDoubleOrNull() ?: 0.0))
            }
        }

        // ── Invoice Field Visibility ─────────────────────
        SettingsSection(title = "Invoice Field Visibility") {
            SettingsToggle("Show Email on Invoice", localSettings.showEmail) {
                update(localSettings.copy(showEmail = it))
            }
            SettingsToggle("Show Website on Invoice", localSettings.showWebsite) {
                update(localSettings.copy(showWebsite = it))
            }
            SettingsToggle("Show Tax Row", localSettings.showTax) {
                update(localSettings.copy(showTax = it))
            }
            SettingsToggle("Show Discount Row", localSettings.showDiscount) {
                update(localSettings.copy(showDiscount = it))
            }
        }

        // ── Custom Invoice Fields ────────────────────────
        SettingsSection(title = "Custom Invoice Header Fields") {
            Text(
                "Add custom fields to your invoice header (e.g. SSM No., Vehicle No.)",
                style = MaterialTheme.typography.bodySmall,
                color = PSRColors.Grey600,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            localSettings.customInvoiceFields.forEachIndexed { idx, field ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(field.label, style = MaterialTheme.typography.labelMedium, color = PSRColors.Navy900)
                        Text(field.value, style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey600)
                    }
                    IconButton(
                        onClick = {
                            update(localSettings.copy(
                                customInvoiceFields = localSettings.customInvoiceFields.toMutableList().also { it.removeAt(idx) }
                            ))
                        }
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Remove",
                            tint = PSRColors.Error, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newCustomFieldLabel,
                    onValueChange = { newCustomFieldLabel = it },
                    label = { Text("Field Name") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    placeholder = { Text("e.g. SSM No.") }
                )
                IconButton(
                    onClick = {
                        if (newCustomFieldLabel.isNotBlank()) {
                            update(localSettings.copy(
                                customInvoiceFields = localSettings.customInvoiceFields + CustomField(newCustomFieldLabel, "")
                            ))
                            newCustomFieldLabel = ""
                        }
                    }
                ) {
                    Icon(Icons.Default.AddCircle, contentDescription = "Add", tint = PSRColors.Accent)
                }
            }
        }

        // ── Units of Measurement ───────────────────────
        SettingsSection(title = "Units of Measurement") {
            Text(
                "These units appear in dropdowns when adding stock items and invoice lines — reduces typos.",
                style = MaterialTheme.typography.bodySmall,
                color = PSRColors.Grey600,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            localSettings.customUnits.forEachIndexed { idx, unit ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.background(PSRColors.Navy600.copy(0.08f), RoundedCornerShape(6.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Text(unit, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = PSRColors.Navy700)
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { update(localSettings.copy(customUnits = localSettings.customUnits.toMutableList().also { it.removeAt(idx) })) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, null, tint = PSRColors.Error, modifier = Modifier.size(14.dp))
                    }
                }
            }
            var newUnit by remember { mutableStateOf("") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = newUnit, onValueChange = { newUnit = it }, label = { Text("New Unit") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), singleLine = true, placeholder = { Text("e.g. tray, bag") })
                IconButton(onClick = {
                    if (newUnit.isNotBlank() && !localSettings.customUnits.contains(newUnit.trim())) {
                        update(localSettings.copy(customUnits = localSettings.customUnits + newUnit.trim()))
                        newUnit = ""
                    }
                }) { Icon(Icons.Default.AddCircle, null, tint = PSRColors.Accent) }
            }
        }

        // ── Save Button ─────────────────────────────────
        if (hasChanges) {
            Button(
                onClick = {
                    viewModel.saveSettings(localSettings)
                    hasChanges = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PSRColors.Navy600)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Save Settings", style = MaterialTheme.typography.titleMedium)
            }
        }

        // ── Flush Day ──────────────────────────────────────────────────────
        // ── Price List Template ─────────────────────────────────────────
        SettingsSection(title = "Price List Template") {
            Text(
                "These appear at the top and bottom of every exported price list. Use {date} as a placeholder for today's date.",
                style = MaterialTheme.typography.bodySmall,
                color = PSRColors.Grey600,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            OutlinedTextField(
                value = localSettings.priceListHeader,
                onValueChange = { update(localSettings.copy(priceListHeader = it)) },
                label = { Text("Header") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 2, maxLines = 5,
                placeholder = { Text("e.g. 📋 *PSRmart Price List — {date}*\nFresh produce direct from farm 🌿") }
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = localSettings.priceListFooter,
                onValueChange = { update(localSettings.copy(priceListFooter = it)) },
                label = { Text("Footer") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 2, maxLines = 5,
                placeholder = { Text("e.g. _Prices subject to change._\nWhatsApp us to order!") }
            )
        }

        FlushDaySection(viewModel = viewModel)

        // ── Catalogue Backup ───────────────────────────────────────────
        CatalogueBackupSection(viewModel = viewModel)

        Spacer(Modifier.height(80.dp))
    }
}

// ─────────────────────────────────────────────
// SETTINGS HELPERS
// ─────────────────────────────────────────────

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        SectionHeader(title = title)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PSRColors.Card),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                content()
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun SettingsTextField(label: String, value: String, maxLines: Int = 1, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        maxLines = maxLines,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PSRColors.Accent,
            unfocusedBorderColor = PSRColors.Grey200,
            focusedLabelColor = PSRColors.Accent
        )
    )
}

@Composable
fun SettingsToggle(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f),
            color = PSRColors.Navy900)
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedThumbColor = PSRColors.White, checkedTrackColor = PSRColors.Navy600)
        )
    }
}

// ─────────────────────────────────────────────
// FLUSH DAY SECTION
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlushDaySection(viewModel: PSRViewModel) {
    val settings     by viewModel.businessSettings.collectAsState()
    val flushState   by viewModel.flushState.collectAsState()
    val exportFile   by viewModel.transactionExportFile.collectAsState()
    val context      = LocalContext.current

    var selectedTab       by remember { mutableStateOf(0) } // 0=partial, 1=nuclear
    var showDatePicker    by remember { mutableStateOf(false) }
    var selectedCutoffMs  by remember { mutableStateOf<Long?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showNuclearDialog by remember { mutableStateOf(false) }

    // Nuclear flush opening balances
    var maybankOpening  by remember { mutableStateOf("") }
    var cashOpening     by remember { mutableStateOf("") }
    var savingsOpening  by remember { mutableStateOf("") }

    val now = System.currentTimeMillis()
    val cal = Calendar.getInstance()

    // Share CSV when ready
    LaunchedEffect(exportFile) {
        exportFile?.let { file ->
            val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                putExtra(android.content.Intent.EXTRA_SUBJECT, "PSRmart Transaction Export")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(android.content.Intent.createChooser(intent, "Export Transactions").apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) })
            viewModel.clearTransactionExport()
        }
    }

    SectionHeader(title = "🧹 Flush Day")

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PSRColors.Card),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // ── Export first ─────────────────────────────────────────
            Text("Step 1 — Export your transactions", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = PSRColors.Navy700)
            Text("Save a full CSV of all bank entries and invoices before flushing. Open in Excel or Google Sheets.",
                style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey600)
            Button(
                onClick = { viewModel.exportTransactions() },
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PSRColors.Navy600)
            ) {
                Icon(Icons.Default.FileDownload, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Export All Transactions (.csv)", style = MaterialTheme.typography.titleSmall)
            }

            HorizontalDivider(color = PSRColors.Divider)

            // ── Flush type selector ──────────────────────────────────
            Text("Step 2 — Choose flush type", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = PSRColors.Navy700)
            Row(modifier = Modifier.fillMaxWidth().background(PSRColors.Grey100, RoundedCornerShape(10.dp)).padding(4.dp)) {
                listOf("Partial (by date)" to 0, "Full Reset (nuclear)" to 1).forEach { (label, idx) ->
                    val selected = selectedTab == idx
                    Box(
                        modifier = Modifier.weight(1f)
                            .background(if (selected) Color.White else Color.Transparent, RoundedCornerShape(8.dp))
                            .clickable { selectedTab = idx; viewModel.resetFlushState() }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal),
                            color = if (selected) (if (idx == 1) PSRColors.Error else PSRColors.Navy600) else PSRColors.Grey500)
                    }
                }
            }

            // ── Partial flush ────────────────────────────────────────
            if (selectedTab == 0) {
                Text("Remove paid invoices, bank entries and order history older than a chosen date. Your account balances are preserved as carry-forward entries.",
                    style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey600)

                Text("Flush data older than:", style = MaterialTheme.typography.labelMedium, color = PSRColors.Grey600)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1 to "1 Month", 2 to "2 Months", 3 to "3 Months").forEach { (months, label) ->
                        cal.timeInMillis = now
                        cal.add(Calendar.MONTH, -months)
                        val cutoff = cal.timeInMillis
                        FilterChip(selected = selectedCutoffMs == cutoff, onClick = { selectedCutoffMs = cutoff },
                            label = { Text(label, style = MaterialTheme.typography.labelMedium) }, shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PSRColors.Navy600, selectedLabelColor = PSRColors.White))
                    }
                }
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (selectedCutoffMs != null) "Before: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(selectedCutoffMs!!))}"
                         else "Choose custom date…")
                }

                if (selectedCutoffMs != null && flushState == FlushState.Idle) {
                    Button(onClick = { viewModel.previewFlush(selectedCutoffMs!!) },
                        modifier = Modifier.fillMaxWidth().height(46.dp), shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PSRColors.Warning)) {
                        Icon(Icons.Default.Preview, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Preview What Will Be Removed", style = MaterialTheme.typography.titleSmall)
                    }
                }
            }

            // ── Nuclear flush ────────────────────────────────────────
            if (selectedTab == 1) {
                Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = PSRColors.Error.copy(0.06f)),
                    border = BorderStroke(1.dp, PSRColors.Error.copy(0.3f))) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Warning, null, tint = PSRColors.Error, modifier = Modifier.size(16.dp))
                            Text("Full Reset — wipes everything", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = PSRColors.Error)
                        }
                        Text("Deletes ALL invoices, ALL bank entries, ALL logs. Cannot be undone. Specify your new opening balances below — these become your starting point.",
                            style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey700)
                    }
                }

                Text("Set opening balances after reset:", style = MaterialTheme.typography.labelMedium, color = PSRColors.Grey600)
                RMInputField(settings.labelFor(FinancialAccount.MAYBANK), maybankOpening, { maybankOpening = it }, Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal))
                RMInputField(settings.labelFor(FinancialAccount.CASH), cashOpening, { cashOpening = it }, Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal))
                RMInputField(settings.labelFor(FinancialAccount.SAVINGS), savingsOpening, { savingsOpening = it }, Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal))

                if (flushState == FlushState.Idle) {
                    Button(onClick = { showNuclearDialog = true },
                        modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PSRColors.Error)) {
                        Icon(Icons.Default.DeleteForever, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Full Reset & Set Opening Balances", style = MaterialTheme.typography.titleSmall)
                    }
                }
            }

            // ── State feedback (shared by both modes) ────────────────
            when (val state = flushState) {
                is FlushState.Previewing -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = PSRColors.Warning)
                    Text("Analysing data…", style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey500)
                }
                is FlushState.Ready -> {
                    val cutoffStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(state.report.cutoffMs))
                    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = PSRColors.Warning.copy(0.08f))) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Warning, null, tint = PSRColors.Warning, modifier = Modifier.size(18.dp))
                                Text("Flush Preview", style = MaterialTheme.typography.titleSmall, color = PSRColors.Warning)
                            }
                            Text("Data older than $cutoffStr:", style = MaterialTheme.typography.bodySmall)
                            Text("• ${state.report.totalInvoices} invoices in system", style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey700)
                            Text("• ${state.report.totalBankEntries} bank entries", style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey700)
                            HorizontalDivider(color = PSRColors.Grey200, modifier = Modifier.padding(vertical = 2.dp))
                            Text("Balances preserved:", style = MaterialTheme.typography.labelMedium, color = PSRColors.Navy600)
                            Text("${settings.labelFor(FinancialAccount.MAYBANK)}: RM %.2f  •  ${settings.labelFor(FinancialAccount.CASH)}: RM %.2f  •  ${settings.labelFor(FinancialAccount.SAVINGS)}: RM %.2f"
                                .format(state.report.maybankBalance, state.report.cashBalance, state.report.savingsBalance),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { viewModel.resetFlushState() }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) { Text("Cancel") }
                        Button(onClick = { showConfirmDialog = true }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PSRColors.Error)) {
                            Icon(Icons.Default.DeleteSweep, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Run Flush")
                        }
                    }
                }
                is FlushState.Running -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = PSRColors.Error)
                    Text("Running flush…", style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey500)
                }
                is FlushState.Done -> {
                    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = PSRColors.Success.copy(0.08f))) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.CheckCircle, null, tint = PSRColors.Success, modifier = Modifier.size(22.dp))
                            Column {
                                Text("Done!", style = MaterialTheme.typography.titleSmall, color = PSRColors.Success)
                                Text("All clear. App is fresh.", style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey600)
                            }
                        }
                    }
                    TextButton(onClick = { viewModel.resetFlushState(); selectedCutoffMs = null }, modifier = Modifier.fillMaxWidth()) { Text("Dismiss") }
                }
                else -> {}
            }
        }
    }

    if (showDatePicker) {
        val dps = rememberDatePickerState()
        DatePickerDialog(onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { dps.selectedDateMillis?.let { selectedCutoffMs = it }; showDatePicker = false }) { Text("OK") } }
        ) { DatePicker(state = dps) }
    }

    // Partial flush confirm
    if (showConfirmDialog) {
        AlertDialog(onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirm Partial Flush") },
            text = { Text("Permanently delete old invoices and bank entries before the chosen date. Your current balances will be preserved as carry-forward entries. Cannot be undone.") },
            confirmButton = {
                Button(onClick = { selectedCutoffMs?.let { viewModel.confirmFlush(it) }; showConfirmDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PSRColors.Error)) { Text("Yes, Flush") }
            },
            dismissButton = { TextButton(onClick = { showConfirmDialog = false }) { Text("Cancel") } }
        )
    }

    // Nuclear flush confirm
    if (showNuclearDialog) {
        AlertDialog(onDismissRequest = { showNuclearDialog = false },
            title = { Text("⚠️ Full Reset — Are you sure?") },
            text = {
                Column {
                    Text("This will permanently delete ALL invoices, ALL bank entries, and ALL logs.", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Opening balances you set will become your new starting point. This CANNOT be undone.",
                        style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey600)
                    Spacer(Modifier.height(8.dp))
                    Text("Did you export your transactions first?", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = PSRColors.Warning)
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.confirmNuclearFlush(
                        maybankOpening.toDoubleOrNull() ?: 0.0,
                        cashOpening.toDoubleOrNull() ?: 0.0,
                        savingsOpening.toDoubleOrNull() ?: 0.0
                    )
                    showNuclearDialog = false
                }, colors = ButtonDefaults.buttonColors(containerColor = PSRColors.Error)) { Text("Yes, Full Reset") }
            },
            dismissButton = { TextButton(onClick = { showNuclearDialog = false }) { Text("Cancel") } }
        )
    }
}

// ─────────────────────────────────────────────
// CATALOGUE BACKUP SECTION
// ─────────────────────────────────────────────

@Composable
fun CatalogueBackupSection(viewModel: PSRViewModel) {
    val context      = LocalContext.current
    val exportFile   by viewModel.catalogueExportFile.collectAsState()
    val importResult by viewModel.importResult.collectAsState()
    val allStock     by viewModel.allStockItems.collectAsState()
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var pendingJson        by remember { mutableStateOf<String?>(null) }

    // File picker for restore — accepts any file (user picks .psrmart)
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                val json = context.contentResolver.openInputStream(it)?.bufferedReader()?.readText()
                if (json != null) {
                    pendingJson = json
                    showRestoreConfirm = true
                }
            } catch (_: Exception) {
                viewModel.showMessage("❌ Could not read file")
            }
        }
    }

    // Trigger share sheet when export file is ready
    LaunchedEffect(exportFile) {
        exportFile?.let { file ->
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", file
            )
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                putExtra(android.content.Intent.EXTRA_SUBJECT, "PSRmart Catalogue Backup")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                android.content.Intent.createChooser(intent, "Save Catalogue Backup")
                    .apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) }
            )
            viewModel.clearCatalogueExport()
        }
    }

    SettingsSection(title = "Catalogue Backup & Restore") {
        Text(
            "Export your full catalogue as a .psrmart file and save it to Google Drive, email it to yourself, or send it anywhere. " +
            "If you ever lose your phone, open the app on a new device and restore from the file — all your products, categories, suppliers, and prices come back.",
            style = MaterialTheme.typography.bodySmall,
            color = PSRColors.Grey600,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Stats
        Row(modifier = Modifier.fillMaxWidth()
            .background(PSRColors.Grey50, androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
            .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Column {
                Text("Products", style = MaterialTheme.typography.labelSmall, color = PSRColors.Grey500)
                Text("${allStock.size}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = PSRColors.Navy600)
            }
            Column {
                Text("Format", style = MaterialTheme.typography.labelSmall, color = PSRColors.Grey500)
                Text(".psrmart", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = PSRColors.Navy600)
            }
            Column {
                Text("Auto-backup", style = MaterialTheme.typography.labelSmall, color = PSRColors.Grey500)
                Text("✓ Active", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = PSRColors.Success)
            }
        }

        Spacer(Modifier.height(14.dp))

        // Export button
        Button(
            onClick = { viewModel.exportCatalogue() },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PSRColors.Navy600),
            enabled = allStock.isNotEmpty()
        ) {
            Icon(Icons.Default.FileDownload, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                if (allStock.isEmpty()) "No products to export"
                else "Export Catalogue (${allStock.size} items)",
                style = MaterialTheme.typography.titleSmall
            )
        }

        Spacer(Modifier.height(8.dp))

        // Restore button
        OutlinedButton(
            onClick = { filePicker.launch(arrayOf("*/*")) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, PSRColors.Accent)
        ) {
            Icon(Icons.Default.FileUpload, null, modifier = Modifier.size(18.dp), tint = PSRColors.Accent)
            Spacer(Modifier.width(8.dp))
            Text("Restore from Backup File", style = MaterialTheme.typography.titleSmall, color = PSRColors.Accent)
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "Tip: save your .psrmart file to Google Drive. Restoring is safe — existing products are updated, nothing is deleted.",
            style = MaterialTheme.typography.labelSmall,
            color = PSRColors.Grey400
        )
    }

    // Restore confirmation dialog
    if (showRestoreConfirm && pendingJson != null) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false; pendingJson = null },
            title = { Text("Restore Catalogue?") },
            text = {
                Column {
                    Text("This will merge the backup into your current catalogue.")
                    Spacer(Modifier.height(4.dp))
                    Text("• New items will be added\n• Items already in your catalogue will be skipped\n• Nothing will be deleted",
                        style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey500)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.importCatalogue(pendingJson!!)
                        showRestoreConfirm = false
                        pendingJson = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PSRColors.Navy600)
                ) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false; pendingJson = null }) { Text("Cancel") }
            }
        )
    }
}
