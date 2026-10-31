package com.omkarnub.kanri.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.ui.common.GlassLevel
import com.omkarnub.kanri.ui.common.KanriGlassCard
import com.omkarnub.kanri.ui.theme.LocalKanriGlass
import com.omkarnub.kanri.util.CurrencyUtils

@Composable
fun LendingQuickPulse(
    totalLentPending: Double,
    totalBorrowedPending: Double,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalKanriGlass.current

    KanriGlassCard(
        modifier = modifier.fillMaxWidth(),
        level = GlassLevel.Blur,
        shape = RoundedCornerShape(24.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = "Lend and Borrow",
                    tint = tokens?.accent ?: MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "LEND & BORROW",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            fontSize = 10.sp
                        ),
                        color = tokens?.textSecondary ?: MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (totalLentPending > 0 || totalBorrowedPending > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (totalLentPending > 0) {
                                Text(
                                    text = "${CurrencyUtils.formatCurrency(totalLentPending)} to collect",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.5.sp
                                    ),
                                    color = tokens?.credit ?: Color(0xFF10B981)
                                )
                            }

                            if (totalLentPending > 0 && totalBorrowedPending > 0) {
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.sp
                                    ),
                                    color = (tokens?.textTertiary ?: MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.6f)
                                )
                            }

                            if (totalBorrowedPending > 0) {
                                Text(
                                    text = "${CurrencyUtils.formatCurrency(totalBorrowedPending)} you owe",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.5.sp
                                    ),
                                    color = tokens?.debit ?: Color(0xFFF59E0B)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "All settled • No pending dues",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.5.sp
                            ),
                            color = tokens?.textTertiary ?: MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = "Go to Lend and Borrow",
                tint = (tokens?.textTertiary ?: MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.6f),
                modifier = Modifier.size(13.dp)
            )
        }
    }
}
