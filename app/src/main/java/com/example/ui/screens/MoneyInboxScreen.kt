package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.NorthEast
import androidx.compose.material.icons.outlined.SouthWest
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ShylockWordmark
import com.example.TypeFilterPill
import com.example.data.model.DetectedPayment
import com.example.formatInRupee
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.GlassCard
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.accentGlow
import com.example.ui.theme.liquidGlass
import com.example.ui.theme.selectedPillBrush
import com.example.ui.theme.selectedPillRim
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class InboxFilter(val label: String) { ALL("All"), SENT("Sent"), RECEIVED("Received") }

/**
 * Money Inbox tab: payments spotted in notifications, parked here until the user records or
 * dismisses them. Recording hands off to the existing add-record dialog via [onRecord].
 */
@Composable
fun MoneyInboxScreen(
    payments: List<DetectedPayment>,
    currencySymbol: String,
    detectionEnabled: Boolean,
    hasNotificationAccess: Boolean,
    onRecord: (DetectedPayment) -> Unit,
    onDismiss: (DetectedPayment) -> Unit,
    onEnableDetection: () -> Unit,
    onGrantAccess: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var filter by remember { mutableStateOf(InboxFilter.ALL) }
    val sentCount = remember(payments) { payments.count { it.isSent } }
    val receivedCount = payments.size - sentCount
    val visible = remember(payments, filter) {
        when (filter) {
            InboxFilter.ALL -> payments
            InboxFilter.SENT -> payments.filter { it.isSent }
            InboxFilter.RECEIVED -> payments.filter { !it.isSent }
        }
    }
    val grouped = remember(visible) { groupByDay(visible) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("money_inbox_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Wordmark + settings shortcut, scrolls away with the content like the other tabs
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("inbox_brand_header"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShylockWordmark()
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .liquidGlass(shape = CircleShape, strength = 0.9f)
                        .clickable { onOpenSettings() }
                        .testTag("inbox_settings_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Money Inbox settings",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        item { MoneyInboxHeaderCard(pendingCount = payments.size) }

        // Detection is on but the OS hasn't granted us notification access yet — nothing can
        // arrive until it does, so say so instead of showing an empty inbox
        if (detectionEnabled && !hasNotificationAccess) {
            item { NotificationAccessBanner(onGrantAccess = onGrantAccess) }
        }

        if (payments.isEmpty()) {
            item {
                if (detectionEnabled) {
                    InboxCaughtUpState()
                } else {
                    InboxDetectionOffState(onEnableDetection = onEnableDetection)
                }
            }
            return@LazyColumn
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val accent = MaterialTheme.colorScheme.primary
                TypeFilterPill(
                    label = "All (${payments.size})",
                    selected = filter == InboxFilter.ALL,
                    accent = accent,
                    onClick = { filter = InboxFilter.ALL },
                    modifier = Modifier.weight(1f).testTag("inbox_filter_all")
                )
                TypeFilterPill(
                    label = "Sent ($sentCount)",
                    selected = filter == InboxFilter.SENT,
                    accent = accent,
                    onClick = { filter = InboxFilter.SENT },
                    modifier = Modifier.weight(1f).testTag("inbox_filter_sent")
                )
                TypeFilterPill(
                    label = "Received ($receivedCount)",
                    selected = filter == InboxFilter.RECEIVED,
                    accent = accent,
                    onClick = { filter = InboxFilter.RECEIVED },
                    modifier = Modifier.weight(1f).testTag("inbox_filter_received")
                )
            }
        }

        if (visible.isEmpty()) {
            item {
                Text(
                    text = "Nothing ${filter.label.lowercase(Locale.getDefault())} is waiting right now.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp)
                )
            }
        }

        grouped.forEach { (dayLabel, dayPayments) ->
            item(key = "header_$dayLabel") {
                Text(
                    text = dayLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 6.dp)
                )
            }
            items(dayPayments, key = { it.id }) { payment ->
                DetectedPaymentCard(
                    payment = payment,
                    currencySymbol = currencySymbol,
                    onRecord = { onRecord(payment) },
                    onDismiss = { onDismiss(payment) }
                )
            }
        }
    }
}

// Hero card: tinted mail tile, title with the pending badge, and the one-line explanation
@Composable
private fun MoneyInboxHeaderCard(pendingCount: Int) {
    val accent = MaterialTheme.colorScheme.primary
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("inbox_header_card"),
        shape = RoundedCornerShape(22.dp),
        elevation = 6.dp,
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(listOf(accent.copy(alpha = 0.26f), accent.copy(alpha = 0.10f)))
                    )
                    .border(1.dp, accent.copy(alpha = 0.30f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Email,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Money Inbox",
                        fontSize = 20.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.3).sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (pendingCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        PendingBadge(count = pendingCount, modifier = Modifier.testTag("inbox_pending_badge"))
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (pendingCount > 0) {
                        "Payments detected from your notifications waiting for your review."
                    } else {
                        "Payments detected from your notifications appear here for review."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Small red counter used in the header and (via [InboxNavBadge]) the bottom navigation. */
@Composable
fun PendingBadge(count: Int, modifier: Modifier = Modifier, compact: Boolean = false) {
    val label = if (count > 99) "99+" else count.toString()
    Box(
        modifier = modifier
            .height(if (compact) 16.dp else 20.dp)
            .clip(CircleShape)
            .background(ExpenseRed)
            .padding(horizontal = if (compact) 5.dp else 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = if (compact) 9.sp else 11.sp,
            lineHeight = if (compact) 9.sp else 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun NotificationAccessBanner(onGrantAccess: () -> Unit) {
    val accent = MaterialTheme.colorScheme.tertiary
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("inbox_access_banner"),
        shape = RoundedCornerShape(18.dp),
        tint = accent,
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.NotificationsOff,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Notification access needed",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Allow SHYLOCK to read bank and UPI alerts so payments can be detected.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            InboxPillButton(label = "Allow", filled = true, accent = accent, onClick = onGrantAccess)
        }
    }
}

// One detected payment: direction glyph, amount, who/where, source + time, and the two actions
@Composable
private fun DetectedPaymentCard(
    payment: DetectedPayment,
    currencySymbol: String,
    onRecord: () -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val directionColor = if (payment.isSent) ExpenseRed else IncomeGreen
    val directionIcon: ImageVector = if (payment.isSent) Icons.Outlined.NorthEast else Icons.Outlined.SouthWest
    val headline = when {
        payment.counterparty != null && payment.isSent -> "Sent to ${payment.counterparty}"
        payment.counterparty != null -> "Received from ${payment.counterparty}"
        payment.isSent -> "Money sent"
        else -> "Money received"
    }
    val time = remember(payment.timestamp) {
        Instant.ofEpochMilli(payment.timestamp).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
            .lowercase(Locale.getDefault())
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("detected_payment_${payment.id}"),
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { onRecord() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(directionColor.copy(alpha = 0.16f))
                    .border(1.dp, directionColor.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = directionIcon,
                    contentDescription = if (payment.isSent) "Sent" else "Received",
                    tint = directionColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatInRupee(payment.amount, currencySymbol),
                    fontSize = 18.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = headline,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Detected from ${payment.source} • $time",
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Review",
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            InboxPillButton(
                label = if (payment.isSent) "Record" else "Add to Income",
                filled = true,
                accent = MaterialTheme.colorScheme.primary,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onRecord()
                },
                modifier = Modifier.testTag("inbox_record_${payment.id}")
            )
            InboxPillButton(
                label = "Dismiss",
                filled = false,
                accent = MaterialTheme.colorScheme.primary,
                onClick = onDismiss,
                modifier = Modifier.testTag("inbox_dismiss_${payment.id}")
            )
        }
    }
}

// Compact pill button: filled reads as the primary action, glass as the quiet alternative
@Composable
private fun InboxPillButton(
    label: String,
    filled: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = CircleShape
    val base = modifier
        .height(34.dp)
        .then(if (filled) Modifier.accentGlow(accent, shape, 5.dp) else Modifier)
    Box(
        modifier = if (filled) {
            base
                .clip(shape)
                .background(selectedPillBrush(accent))
                .border(0.8.dp, selectedPillRim(accent), shape)
                .clickable { onClick() }
        } else {
            base
                .liquidGlass(shape = shape, strength = 0.7f)
                .clickable { onClick() }
        }
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (filled) FontWeight.Bold else FontWeight.SemiBold,
            color = if (filled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

// Empty state when detection is on and nothing is waiting
@Composable
private fun InboxCaughtUpState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 36.dp)
            .testTag("inbox_empty_state"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        InboxGlyph()
        Spacer(modifier = Modifier.height(22.dp))
        Text(
            text = "You're all caught up!",
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "No payments are waiting for review.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(28.dp))
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            strength = 0.8f,
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Text(
                text = "SHYLOCK will notify you when detected payments appear here.",
                style = MaterialTheme.typography.bodySmall,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// Empty state when detection is off: explain the feature and offer to switch it on
@Composable
private fun InboxDetectionOffState(onEnableDetection: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 36.dp)
            .testTag("inbox_detection_off_state"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        InboxGlyph(dimmed = true)
        Spacer(modifier = Modifier.height(22.dp))
        Text(
            text = "Payment detection is off",
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Turn on payment detection so SHYLOCK can spot bank and UPI payments from your notifications and park them here for review.",
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = 20.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        InboxPillButton(
            label = "Enable payment detection",
            filled = true,
            accent = MaterialTheme.colorScheme.primary,
            onClick = onEnableDetection,
            modifier = Modifier.testTag("inbox_enable_detection_btn")
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Nothing is recorded automatically — you confirm every payment.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )
    }
}

// Inbox tray inside a glass tile with three short "ping" strokes above it
@Composable
private fun InboxGlyph(dimmed: Boolean = false) {
    val accent = MaterialTheme.colorScheme.primary
    val alpha = if (dimmed) 0.55f else 1f
    Box(
        modifier = Modifier
            .size(112.dp)
            .drawBehind {
                if (dimmed) return@drawBehind
                val cx = size.width / 2f
                val top = size.height * 0.06f
                val stroke = 3.dp.toPx()
                val c = accent.copy(alpha = 0.85f)
                drawLine(c, Offset(cx, top), Offset(cx, top + 12.dp.toPx()), stroke, StrokeCap.Round)
                drawLine(c, Offset(cx - 16.dp.toPx(), top + 4.dp.toPx()), Offset(cx - 10.dp.toPx(), top + 14.dp.toPx()), stroke, StrokeCap.Round)
                drawLine(c, Offset(cx + 16.dp.toPx(), top + 4.dp.toPx()), Offset(cx + 10.dp.toPx(), top + 14.dp.toPx()), stroke, StrokeCap.Round)
            },
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .liquidGlass(shape = RoundedCornerShape(24.dp), strength = 0.9f, elevation = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Inbox,
                contentDescription = null,
                tint = lerp(accent, MaterialTheme.colorScheme.onSurfaceVariant, if (dimmed) 0.5f else 0f).copy(alpha = alpha),
                modifier = Modifier.size(44.dp)
            )
        }
    }
}

/** Groups newest-first payments under Today / Yesterday / "12 Sep" labels, preserving order. */
private fun groupByDay(payments: List<DetectedPayment>): List<Pair<String, List<DetectedPayment>>> {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now()
    val formatter = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    val labelled = payments.map { p ->
        val date = Instant.ofEpochMilli(p.timestamp).atZone(zone).toLocalDate()
        val label = when (date) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            else -> date.format(formatter)
        }
        label to p
    }
    val order = LinkedHashMap<String, MutableList<DetectedPayment>>()
    labelled.forEach { (label, p) -> order.getOrPut(label) { mutableListOf() }.add(p) }
    return order.map { (label, list) -> label to list.toList() }
}
