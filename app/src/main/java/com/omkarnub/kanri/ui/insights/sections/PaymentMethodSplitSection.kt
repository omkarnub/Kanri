package com.omkarnub.kanri.ui.insights.sections

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.ui.home.formatCurrency
import com.omkarnub.kanri.ui.insights.InsightsMode
import com.omkarnub.kanri.ui.insights.PaymentMethodItem
import com.omkarnub.kanri.ui.insights.PaymentMethodSplitData

@Composable
fun PaymentMethodSplitSection(
    data: PaymentMethodSplitData,
    mode: InsightsMode,
    onMethodClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val title = if (mode == InsightsMode.EXPENSE) "PAYMENT METHODS" else "HOW YOU WERE PAID"
    var sweepAnimTriggered by remember { mutableStateOf(false) }

    LaunchedEffect(data) {
        sweepAnimTriggered = true
    }

    val animatedSweep by animateFloatAsState(
        targetValue = if (sweepAnimTriggered) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "methodSweep"
    )

    val onSurface = MaterialTheme.colorScheme.onSurface

    // Tonal monochrome steps for methods
    fun tonalStepColor(index: Int): Color {
        val alphas = listOf(0.95f, 0.70f, 0.50f, 0.32f, 0.18f)
        val alpha = alphas.getOrElse(index) { 0.20f }
        return onSurface.copy(alpha = alpha)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "$title: ${data.methods.size} methods used"
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Horizontal stacked bar
            if (data.methods.isNotEmpty() && data.totalAmount > 0) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .pointerInput(data.methods) {
                            detectTapGestures { offset ->
                                var currentX = 0f
                                for (method in data.methods) {
                                    val segWidth = (size.width * (method.percentage / 100f).toFloat()) * animatedSweep
                                    if (offset.x in currentX..(currentX + segWidth)) {
                                        onMethodClick(method.sourceType)
                                        break
                                    }
                                    currentX += segWidth
                                }
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height
                    var currentX = 0f

                    for (i in data.methods.indices) {
                        val method = data.methods[i]
                        val segRatio = (method.percentage / 100.0).toFloat()
                        val segWidth = (w * segRatio) * animatedSweep
                        val color = tonalStepColor(i)

                        drawRect(
                            color = color,
                            topLeft = Offset(currentX, 0f),
                            size = Size(segWidth, h)
                        )
                        currentX += segWidth
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Method rows
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                data.methods.forEachIndexed { index, method ->
                    PaymentMethodRow(
                        method = method,
                        stepColor = tonalStepColor(index),
                        onClick = { onMethodClick(method.sourceType) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentMethodRow(
    method: PaymentMethodItem,
    stepColor: Color,
    onClick: () -> Unit
) {
    val icon = when (method.sourceType.uppercase()) {
        "UPI" -> Icons.Default.QrCode
        "CARD" -> Icons.Default.CreditCard
        "ATM" -> Icons.Default.LocalAtm
        "BANK_TRANSFER", "NETBANKING" -> Icons.Default.AccountBalance
        else -> Icons.Default.Payments
    }

    val displayName = when (method.sourceType.uppercase()) {
        "BANK_TRANSFER" -> "Bank Transfer"
        else -> method.sourceType
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(stepColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = stepColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Column {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    text = "${method.count} transaction${if (method.count == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatCurrency(method.totalAmount),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            val pct = String.format(java.util.Locale.US, "%.0f%%", method.percentage)
            Text(
                text = pct,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            )
        }
    }
}
