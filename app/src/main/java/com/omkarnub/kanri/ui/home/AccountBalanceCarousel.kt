package com.omkarnub.kanri.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.util.CurrencyUtils

@Composable
fun AccountBalanceCarousel(
    accounts: List<BankAccountBalance>,
    totalLiquidBalance: Double,
    modifier: Modifier = Modifier
) {
    if (accounts.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ACCOUNTS & BALANCES",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.3.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (totalLiquidBalance > 0) {
                Text(
                    text = "TOTAL: ${CurrencyUtils.formatCurrency(totalLiquidBalance)}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Horizontal Carousel
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 0.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(accounts, key = { "${it.bankName}-${it.accountMask}" }) { account ->
                AccountCard(account = account)
            }
        }
    }
}

@Composable
private fun AccountCard(
    account: BankAccountBalance,
    modifier: Modifier = Modifier
) {
    val shortBank = when {
        account.bankName.contains("State Bank", ignoreCase = true) || account.bankName.equals("SBI", ignoreCase = true) -> "SBI"
        account.bankName.contains("HDFC", ignoreCase = true) -> "HDFC"
        account.bankName.contains("ICICI", ignoreCase = true) -> "ICICI"
        account.bankName.contains("Axis", ignoreCase = true) -> "AXIS"
        account.bankName.contains("Kotak", ignoreCase = true) -> "KOTAK"
        account.bankName.contains("Punjab", ignoreCase = true) || account.bankName.contains("PNB", ignoreCase = true) -> "PNB"
        account.bankName.contains("Bank of Baroda", ignoreCase = true) || account.bankName.contains("BOB", ignoreCase = true) -> "BOB"
        account.bankName.contains("Paytm", ignoreCase = true) -> "PAYTM"
        account.bankName.contains("Airtel", ignoreCase = true) -> "AIRTEL"
        else -> account.bankName.take(5).uppercase()
    }

    Card(
        modifier = modifier
            .width(168.dp)
            .height(82.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 13.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: Bank monogram & account mask
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(
                            0.8.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            RoundedCornerShape(6.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = shortBank.take(3),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "$shortBank ${account.accountMask}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.5.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Bottom: Available balance
            Column {
                Text(
                    text = if (account.balance != null) CurrencyUtils.formatCurrency(account.balance) else "Active",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
