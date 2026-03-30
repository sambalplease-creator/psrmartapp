package com.psrmart.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.core.view.WindowCompat
import com.psrmart.app.ui.screens.*
import com.psrmart.app.ui.components.*
import com.psrmart.app.ui.theme.*
import com.psrmart.app.viewmodel.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: PSRViewModel by viewModels { PSRViewModelFactory(application) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            PSRTheme {
                var showSplash by remember { mutableStateOf(true) }
                if (showSplash) {
                    SplashScreen(onFinish = { showSplash = false })
                } else {
                    PSRApp(viewModel)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// SPLASH SCREEN
// ─────────────────────────────────────────────

@Composable
fun SplashScreen(onFinish: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    val alpha   by animateFloatAsState(targetValue = if (visible) 1f else 0f, animationSpec = tween(700), label = "fade")
    val slide   by animateFloatAsState(targetValue = if (visible) 0f else 40f, animationSpec = tween(700, easing = EaseOutCubic), label = "slide")

    LaunchedEffect(Unit) { visible = true; delay(2000); onFinish() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PSRColors.Navy600),   // solid forest green
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .alpha(alpha)
                .offset(y = slide.dp)
        ) {
            // Chicken mascot
            Text("🐔", style = MaterialTheme.typography.displayLarge.copy(fontSize = 72.sp))

            Spacer(Modifier.height(24.dp))

            // PSRmart wordmark — big bold white
            Text(
                "PSRmart",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                ),
                color = PSRColors.White
            )

            Spacer(Modifier.height(8.dp))

            // Tagline
            Text(
                "Fresh • Quality • Reliable",
                style = MaterialTheme.typography.bodyMedium,
                color = PSRColors.White.copy(alpha = 0.6f)
            )

            Spacer(Modifier.height(60.dp))

            // Subtle loading dots
            AnimatedLoadingDots()
        }

        // Version at bottom
        Text(
            "PSRmart Manager",
            style = MaterialTheme.typography.labelSmall,
            color = PSRColors.White.copy(alpha = 0.3f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .alpha(alpha)
        )
    }
}

@Composable
fun AnimatedLoadingDots() {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        (0..2).forEach { i ->
            val yOffset by infiniteTransition.animateFloat(
                initialValue = 0f, targetValue = -8f,
                animationSpec = infiniteRepeatable(tween(400, delayMillis = i * 120, easing = EaseInOutSine), RepeatMode.Reverse),
                label = "dot_$i"
            )
            Box(modifier = Modifier.size(6.dp).offset(y = yOffset.dp).background(PSRColors.Accent.copy(alpha = 0.7f), CircleShape))
        }
    }
}

// ─────────────────────────────────────────────
// PANE DEFINITIONS
// ─────────────────────────────────────────────

data class PaneDef(val title: String, val subtitle: String, val icon: ImageVector, val iconSelected: ImageVector, val fabIcon: ImageVector? = null, val fabLabel: String = "")

val PANES = listOf(
    PaneDef("Orders",   "Today's Picking List",   Icons.Outlined.AssignmentTurnedIn, Icons.Filled.AssignmentTurnedIn, Icons.Default.Add, "Add Order"),
    PaneDef("Stock",    "Visual Stock Library",   Icons.Outlined.Inventory2,         Icons.Filled.Inventory2,         Icons.Default.Add, "Add Item"),
    PaneDef("Finance",  "Financial Summary",      Icons.Outlined.AccountBalance,     Icons.Filled.AccountBalance,     Icons.Default.Add, "Log Entry"),
    PaneDef("Invoices", "Invoice Builder",        Icons.Outlined.ReceiptLong,        Icons.Filled.ReceiptLong,        Icons.Default.NoteAdd, "New Invoice"),
    PaneDef("Settings", "Business Configuration", Icons.Outlined.Settings,           Icons.Filled.Settings)
)

// ─────────────────────────────────────────────
// MAIN APP
// ─────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PSRApp(viewModel: PSRViewModel) {
    var savedPage by rememberSaveable { mutableIntStateOf(2) }
    val pagerState  = rememberPagerState(initialPage = savedPage) { PANES.size }
    val scope       = rememberCoroutineScope()
    val currentPage = pagerState.currentPage
    val haptic      = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(currentPage) { savedPage = currentPage }

    // Navigate to Invoices pane (index 3) when triggered from Dashboard
    val goToInvoices by viewModel.navigateToInvoicesPane.collectAsState()
    LaunchedEffect(goToInvoices) {
        if (goToInvoices) {
            pagerState.animateScrollToPage(3)
            viewModel.clearNavigateToInvoicesPane()
        }
    }

    // Snackbar messages
    val snackMsg by viewModel.snackbarMessage.collectAsState()
    LaunchedEffect(snackMsg) {
        if (snackMsg != null) {
            snackbarHostState.showSnackbar(snackMsg!!, duration = SnackbarDuration.Short)
            viewModel.clearMessage()
        }
    }

    var ordersFabTrigger    by remember { mutableStateOf(0) }
    var catalogueFabTrigger by remember { mutableStateOf(0) }
    var dashboardFabTrigger  by remember { mutableStateOf(0) }
    var showCalculator       by remember { mutableStateOf(false) }

    val topBarTransition = updateTransition(targetState = currentPage, label = "topbar")

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = PSRColors.Navy800,
                    contentColor = PSRColors.White,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    topBarTransition.AnimatedContent(
                        transitionSpec = {
                            val dir = if (targetState > initialState) 1 else -1
                            (slideInHorizontally { it * dir / 3 } + fadeIn()) togetherWith (slideOutHorizontally { -it * dir / 3 } + fadeOut())
                        }
                    ) { page ->
                        Column {
                            Text(PANES[page].title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = PSRColors.White)
                            Text(PANES[page].subtitle, style = MaterialTheme.typography.bodySmall, color = PSRColors.White.copy(alpha = 0.65f))
                        }
                    }
                },
                actions = {
                    // Calculator toggle button
                    IconButton(onClick = { showCalculator = !showCalculator }) {
                        Icon(Icons.Default.Calculate, contentDescription = "Calculator", tint = if (showCalculator) PSRColors.Accent else PSRColors.White.copy(alpha = 0.8f))
                    }
                    PaneIndicator(currentPage = currentPage, pageCount = PANES.size, modifier = Modifier.padding(end = 16.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PSRColors.Navy600),
                windowInsets = WindowInsets.statusBars
            )
        },
        bottomBar = {
            NavigationBar(containerColor = PSRColors.Card, tonalElevation = 0.dp, windowInsets = WindowInsets.navigationBars) {
                PANES.forEachIndexed { idx, pane ->
                    val selected = currentPage == idx
                    val iconScale by animateFloatAsState(
                        targetValue = if (selected) 1.15f else 1f,
                        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
                        label = "nav_$idx"
                    )
                    NavigationBarItem(
                        selected = selected,
                        onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); scope.launch { pagerState.animateScrollToPage(idx) } },
                        icon = { Icon(if (selected) pane.iconSelected else pane.icon, pane.title, modifier = Modifier.scale(iconScale)) },
                        label = { Text(pane.title.split(" ").first(), style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PSRColors.Navy600, selectedTextColor = PSRColors.Navy600,
                            indicatorColor = PSRColors.Navy600.copy(alpha = 0.1f),
                            unselectedIconColor = PSRColors.Grey400, unselectedTextColor = PSRColors.Grey400
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            val pane = PANES[currentPage]
            AnimatedVisibility(visible = pane.fabIcon != null, enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(), exit = scaleOut() + fadeOut()) {
                if (pane.fabIcon != null) {
                    ExtendedFloatingActionButton(
                        onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); when (currentPage) { 0 -> ordersFabTrigger++; 1 -> catalogueFabTrigger++; 2 -> dashboardFabTrigger++ } },
                        icon = { Icon(pane.fabIcon, null) },
                        text = { Text(pane.fabLabel) },
                        containerColor = PSRColors.Navy600,
                        contentColor = PSRColors.White,
                        expanded = !pagerState.isScrollInProgress
                    )
                }
            }
        },
        containerColor = PSRColors.Surface
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                beyondBoundsPageCount = 1,
            ) { page ->
                val pageAlpha by animateFloatAsState(if (pagerState.currentPage == page) 1f else 0.6f, tween(300), label = "page_alpha_$page")
                Box(modifier = Modifier.alpha(pageAlpha)) {
                    when (page) {
                        0 -> OrderScreenWithFab(viewModel, ordersFabTrigger)
                        1 -> CatalogueScreenWithFab(viewModel, catalogueFabTrigger)
                        2 -> DashboardScreenWithFab(viewModel, dashboardFabTrigger)
                        3 -> InvoiceScreen(viewModel) {}
                        4 -> SettingsScreen(viewModel)
                    }
                }
            }

            // Floating Calculator overlay
            if (showCalculator) {
                FloatingCalculator(
                    onDismiss = { showCalculator = false },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = paddingValues.calculateBottomPadding() + 80.dp, end = 16.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// FLOATING CALCULATOR
// ─────────────────────────────────────────────

@Composable
fun FloatingCalculator(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    var display    by remember { mutableStateOf("0") }
    var operand1   by remember { mutableStateOf(0.0) }
    var operator   by remember { mutableStateOf<String?>(null) }
    var waitNext   by remember { mutableStateOf(false) }
    var offsetX    by remember { mutableStateOf(0f) }
    var offsetY    by remember { mutableStateOf(0f) }

    fun input(value: String) {
        when (value) {
            "C"  -> { display = "0"; operand1 = 0.0; operator = null; waitNext = false }
            "⌫"  -> { display = if (display.length > 1) display.dropLast(1) else "0" }
            "+", "-", "×", "÷" -> {
                operand1 = display.toDoubleOrNull() ?: 0.0
                operator = value
                waitNext = true
            }
            "="  -> {
                val op2 = display.toDoubleOrNull() ?: 0.0
                val result = when (operator) {
                    "+"  -> operand1 + op2
                    "-"  -> operand1 - op2
                    "×"  -> operand1 * op2
                    "÷"  -> if (op2 != 0.0) operand1 / op2 else 0.0
                    else -> op2
                }
                display = if (result == result.toLong().toDouble()) result.toLong().toString() else "%.4f".format(result).trimEnd('0').trimEnd('.')
                operator = null; waitNext = false
            }
            "."  -> { if (!display.contains('.')) display = "$display." }
            else -> {
                display = if (waitNext || display == "0") { waitNext = false; value } else "$display$value"
                if (display.length > 12) display = display.dropLast(1)
            }
        }
    }

    val buttons = listOf(
        listOf("C", "⌫", "÷", "×"),
        listOf("7", "8", "9", "-"),
        listOf("4", "5", "6", "+"),
        listOf("1", "2", "3", "="),
        listOf("0", ".", "", "")
    )

    Card(
        modifier = modifier
            .offset { IntOffset(offsetX.toInt(), offsetY.toInt()) }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            }
            .width(240.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PSRColors.Navy900),
        elevation = CardDefaults.cardElevation(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Title bar
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Calculate, null, tint = PSRColors.Accent, modifier = Modifier.size(14.dp))
                    Text("Calculator", style = MaterialTheme.typography.labelMedium, color = PSRColors.White.copy(alpha = 0.6f))
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, null, tint = PSRColors.White.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                }
            }

            Spacer(Modifier.height(8.dp))

            // Display
            Box(
                modifier = Modifier.fillMaxWidth().background(PSRColors.Navy800, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    display,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = PSRColors.White,
                    maxLines = 1,
                    textAlign = TextAlign.End
                )
            }

            if (operator != null) {
                Text("$operand1 $operator", style = MaterialTheme.typography.labelSmall, color = PSRColors.Accent, modifier = Modifier.align(Alignment.End).padding(end = 4.dp, top = 2.dp))
            }

            Spacer(Modifier.height(8.dp))

            // Buttons
            buttons.forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { btn ->
                        if (btn.isEmpty()) {
                            Spacer(modifier = Modifier.weight(1f))
                        } else {
                            val isOperator = btn in listOf("+", "-", "×", "÷", "=")
                            val isAction   = btn in listOf("C", "⌫")
                            val bgColor    = when {
                                btn == "="    -> PSRColors.Accent
                                isOperator    -> PSRColors.Navy600
                                isAction      -> PSRColors.Navy700
                                else          -> PSRColors.Navy800
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .background(bgColor, RoundedCornerShape(10.dp))
                                    .clickable { input(btn) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(btn, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = PSRColors.White)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────
// FAB-TRIGGERED WRAPPERS
// ─────────────────────────────────────────────

@Composable
fun CatalogueScreenWithFab(viewModel: PSRViewModel, fabTrigger: Int) {
    var showAddDialog by remember { mutableStateOf(false) }
    val categories by viewModel.categories.collectAsState()
    val suppliers  by viewModel.suppliers.collectAsState()
    val settings   by viewModel.businessSettings.collectAsState()
    LaunchedEffect(fabTrigger) { if (fabTrigger > 0) showAddDialog = true }
    CatalogueScreen(viewModel)
    if (showAddDialog) {
        AddEditStockDialog(
            stock = null, categories = categories, units = settings.customUnits,
            suppliers = suppliers,
            onDismiss = { showAddDialog = false },
            onSave = { item -> viewModel.insertStock(item); showAddDialog = false },
            onDelete = {}, onAddCategory = {}
        )
    }
}

@Composable
fun DashboardScreenWithFab(viewModel: PSRViewModel, fabTrigger: Int) {
    var showExpenseSheet by remember { mutableStateOf(false) }
    LaunchedEffect(fabTrigger) { if (fabTrigger > 0) showExpenseSheet = true }
    DashboardScreen(viewModel)
    if (showExpenseSheet) {
        AddExpenseSheet(onDismiss = { showExpenseSheet = false },
            onSave = { entry -> viewModel.addBankEntry(entry); showExpenseSheet = false })
    }
}

@Composable
private fun rememberCoroutines() = rememberCoroutineScope()
