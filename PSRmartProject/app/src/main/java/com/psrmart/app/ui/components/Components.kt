package com.psrmart.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.psrmart.app.ui.theme.PSRColors
import com.psrmart.app.ui.theme.PSRMotion
import java.text.NumberFormat
import java.util.Locale

fun formatRM(amount: Double): String {
    val f = NumberFormat.getNumberInstance(Locale.getDefault())
    f.minimumFractionDigits = 2; f.maximumFractionDigits = 2
    return "RM ${f.format(amount)}"
}
fun formatRMCompact(amount: Double) = "RM %.2f".format(amount)

@Composable
fun StatCard(
    title: String, value: String, subtitle: String = "",
    icon: ImageVector, iconTint: Color = PSRColors.Accent,
    valueColor: Color = PSRColors.Navy900, modifier: Modifier = Modifier,
    valueStyle: TextStyle? = null,
    onClick: (() -> Unit)? = null
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val scale by animateFloatAsState(targetValue = if (visible) 1f else 0.85f, animationSpec = PSRMotion.Emphasized, label = "card_scale")
    val alpha by animateFloatAsState(targetValue = if (visible) 1f else 0f, animationSpec = tween(400), label = "card_alpha")
    val cardMod = modifier.scale(scale).alpha(alpha).let { if (onClick != null) it.clickable(onClick = onClick) else it }
    Card(modifier = cardMod, shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PSRColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).background(iconTint.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Text(value, style = valueStyle ?: MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = valueColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(title, style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey600)
            if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.labelSmall, color = PSRColors.Grey400)
        }
    }
}

@Composable
fun PulseDot(color: Color, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(initialValue = 1f, targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(900, easing = EaseInOutSine), RepeatMode.Reverse), label = "pulse_alpha")
    val pulseScale by infiniteTransition.animateFloat(initialValue = 1f, targetValue = 1.6f,
        animationSpec = infiniteRepeatable(tween(900, easing = EaseInOutSine), RepeatMode.Reverse), label = "pulse_scale")
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.size(12.dp).scale(pulseScale).alpha(pulseAlpha).background(color.copy(alpha = 0.3f), CircleShape))
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
    }
}

@Composable
fun ShimmerBox(modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(12.dp)) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerX by infiniteTransition.animateFloat(initialValue = -1000f, targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "shimmer_x")
    Box(modifier = modifier.clip(shape).background(
        Brush.horizontalGradient(colors = listOf(PSRColors.Grey100, PSRColors.Grey50, PSRColors.Grey100),
            startX = shimmerX, endX = shimmerX + 600f)))
}

@Composable
fun SectionHeader(title: String, action: String = "", onAction: () -> Unit = {}) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = PSRColors.Navy900)
        if (action.isNotBlank()) TextButton(onClick = onAction, contentPadding = PaddingValues(0.dp)) {
            Text(action, style = MaterialTheme.typography.labelLarge, color = PSRColors.Accent)
        }
    }
}

@Composable
fun AnimatedBalance(amount: Double, style: androidx.compose.ui.text.TextStyle, color: Color) {
    val animatedAmount by animateFloatAsState(targetValue = amount.toFloat(), animationSpec = PSRMotion.CounterSpec, label = "balance_counter")
    Text(text = "RM %.2f".format(animatedAmount), style = style, color = color)
}

@Composable
fun PriceChip(label: String, value: String, color: Color = PSRColors.Navy600) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = PSRColors.Grey600)
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = color)
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, subtitle: String, actionLabel: String = "", onAction: () -> Unit = {}) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    AnimatedVisibility(visible = visible, enter = slideInVertically(initialOffsetY = { it / 3 }) + fadeIn(tween(400))) {
        Column(modifier = Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(80.dp).background(PSRColors.Grey100, CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = PSRColors.Grey400, modifier = Modifier.size(36.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = PSRColors.Navy900, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = PSRColors.Grey600, textAlign = TextAlign.Center)
            if (actionLabel.isNotBlank()) {
                Spacer(Modifier.height(24.dp))
                Button(onClick = onAction, shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PSRColors.Navy600)) { Text(actionLabel) }
            }
        }
    }
}

@Composable
fun RMInputField(
    label: String, value: String, onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
) {
    OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(label) },
        prefix = { Text("RM ", color = PSRColors.Grey600) }, modifier = modifier,
        shape = RoundedCornerShape(12.dp), singleLine = true, keyboardOptions = keyboardOptions,
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PSRColors.Accent,
            unfocusedBorderColor = PSRColors.Grey200, focusedLabelColor = PSRColors.Accent))
}

@Composable
fun StatusBadge(status: String) {
    val (color, bg) = when (status.uppercase()) {
        "PAID"      -> Pair(PSRColors.Success, PSRColors.Success.copy(alpha = 0.12f))
        "UNPAID"    -> Pair(PSRColors.Warning, PSRColors.Warning.copy(alpha = 0.12f))
        "PARTIAL"   -> Pair(PSRColors.Accent,  PSRColors.Accent.copy(alpha = 0.12f))
        "CANCELLED" -> Pair(PSRColors.Grey400, PSRColors.Grey100)
        else        -> Pair(PSRColors.Grey600, PSRColors.Grey100)
    }
    val animatedBg by animateColorAsState(bg, tween(300), label = "badge_bg")
    val animatedColor by animateColorAsState(color, tween(300), label = "badge_color")
    Box(modifier = Modifier.background(animatedBg, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
        Text(status, style = MaterialTheme.typography.labelSmall, color = animatedColor, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PaneIndicator(currentPage: Int, pageCount: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(pageCount) { index ->
            val isSelected = index == currentPage
            val width by animateDpAsState(if (isSelected) 22.dp else 5.dp, PSRMotion.SnappyDp, label = "dot_w")
            val alpha by animateFloatAsState(if (isSelected) 1f else 0.4f, tween(200), label = "dot_a")
            Box(modifier = Modifier.height(5.dp).width(width).alpha(alpha).background(PSRColors.White, RoundedCornerShape(3.dp)))
        }
    }
}

@Composable
fun ProfitBadge(profit: Double) {
    val isPositive = profit >= 0
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(profit) { visible = false; visible = true }
    val scale by animateFloatAsState(targetValue = if (visible) 1f else 0.5f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium), label = "profit_scale")
    Box(modifier = Modifier.scale(scale)
        .background(if (isPositive) PSRColors.Success.copy(0.12f) else PSRColors.Error.copy(0.12f), RoundedCornerShape(8.dp))
        .padding(horizontal = 10.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown, null,
                tint = if (isPositive) PSRColors.Success else PSRColors.Error, modifier = Modifier.size(12.dp))
            Text("RM %.2f".format(profit), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = if (isPositive) PSRColors.Success else PSRColors.Error)
        }
    }
}

@Composable
fun HorizontalDivider(modifier: Modifier = Modifier, thickness: Dp = 1.dp, color: Color = PSRColors.Divider) {
    Box(modifier.fillMaxWidth().height(thickness).background(color))
}

@Composable
fun VerticalDivider(modifier: Modifier = Modifier, thickness: Dp = 1.dp, color: Color = PSRColors.Divider) {
    Box(modifier.fillMaxHeight().width(thickness).background(color))
}
