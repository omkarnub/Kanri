package com.omkarnub.kanri.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import com.omkarnub.kanri.ui.common.GlassLevel
import com.omkarnub.kanri.ui.common.KanriGlassCard
import com.omkarnub.kanri.ui.theme.LocalKanriGlass
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.data.wallet.WalletBalances
import com.omkarnub.kanri.data.wallet.WalletPreferences
import android.os.Build
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.IconButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.blur
import com.omkarnub.kanri.ui.common.AnimatedNumberText
import com.omkarnub.kanri.ui.theme.ExpenseRed
import com.omkarnub.kanri.ui.theme.IncomeGreen
import com.omkarnub.kanri.util.CurrencyUtils
import com.omkarnub.kanri.util.rememberKanriHaptics
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * "Manage money Balance" Home Card.
 * Positioned below Expense Overview.
 * Displays Cash Balance and Online Balance with clean typography matching the app.
 * Tapping the card or arrow opens the full WalletSheet containing all tools & menus.
 */
@Composable
fun ManageMoneyBalanceCard(
    balances: WalletBalances,
    onClick: () -> Unit,
    onSetupClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val walletPrefs = remember { WalletPreferences.getInstance(context) }
    val isSpoilerEnabled by walletPrefs.isBalanceSpoilerEnabledFlow.collectAsState()
    val tokens = LocalKanriGlass.current
    val haptics = rememberKanriHaptics()

    // Local reveal state: when spoiler is enabled, default to hidden (!isSpoilerEnabled)
    var isRevealed by remember(isSpoilerEnabled) { mutableStateOf(!isSpoilerEnabled) }

    val secondaryLabelColor = tokens?.textPrimary?.copy(alpha = 0.80f)
        ?: MaterialTheme.colorScheme.onSurface.copy(alpha = 0.80f)

    if (!balances.isConfigured) {
        // Unconfigured minimal setup card
        KanriGlassCard(
            modifier = modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "Wallets, tap to set up Cash and Online balances"
                },
            level = GlassLevel.Blur,
            shape = RoundedCornerShape(24.dp),
            onClick = onSetupClick
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Wallets",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp
                        ),
                        color = secondaryLabelColor
                    )

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Set Up Balances",
                        tint = secondaryLabelColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = "Tap to set up Cash & Online balances",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        return
    }

    val zeroTodayText = "${CurrencyUtils.formatCurrency(0.0)} today"
    val cashTodayNet = balances.cashTodayNet
    val cashFlowText = when {
        cashTodayNet > 0.0001 -> "+${CurrencyUtils.formatCurrency(cashTodayNet)} today"
        cashTodayNet < -0.0001 -> "${CurrencyUtils.formatCurrency(cashTodayNet)} today"
        else -> zeroTodayText
    }
    val cashFlowColor = when {
        cashTodayNet > 0.0001 -> IncomeGreen
        cashTodayNet < -0.0001 -> ExpenseRed
        else -> secondaryLabelColor
    }

    val onlineTodayNet = balances.onlineTodayNet
    val onlineFlowText = when {
        onlineTodayNet > 0.0001 -> "+${CurrencyUtils.formatCurrency(onlineTodayNet)} today"
        onlineTodayNet < -0.0001 -> "${CurrencyUtils.formatCurrency(onlineTodayNet)} today"
        else -> zeroTodayText
    }
    val onlineFlowColor = when {
        onlineTodayNet > 0.0001 -> IncomeGreen
        onlineTodayNet < -0.0001 -> ExpenseRed
        else -> secondaryLabelColor
    }

    // Configured Minimalist Card
    KanriGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = if (isRevealed) {
                    "Wallets. Cash: ${CurrencyUtils.formatCurrency(balances.cash)}, today: $cashFlowText. Online: ${CurrencyUtils.formatCurrency(balances.online)}, today: $onlineFlowText"
                } else {
                    "Wallets. Balances hidden. Tap eye icon to reveal."
                }
            },
        level = GlassLevel.Blur,
        shape = RoundedCornerShape(24.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            // Header Row: Title & Action Icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Wallets",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp
                    ),
                    color = secondaryLabelColor
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = {
                            haptics.click()
                            isRevealed = !isRevealed
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isRevealed) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (isRevealed) "Hide balances" else "Reveal balances",
                            tint = secondaryLabelColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open Full Wallet Window",
                        tint = secondaryLabelColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Minimalist Balances Row: Cash Balance & Online Balance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cash Balance
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Text(
                            text = "Cash Balance",
                            style = MaterialTheme.typography.labelSmall,
                            color = secondaryLabelColor
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    BlurredBalanceAmount(
                        amount = balances.cash,
                        isBlurred = !isRevealed,
                        onReveal = { isRevealed = true },
                        horizontalAlignment = Alignment.Start
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = cashFlowText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.5.sp
                        ),
                        color = cashFlowColor
                    )
                }

                // Online Balance
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.outline)
                        )
                        Text(
                            text = "Online Balance",
                            style = MaterialTheme.typography.labelSmall,
                            color = secondaryLabelColor
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    BlurredBalanceAmount(
                        amount = balances.online,
                        isBlurred = !isRevealed,
                        onReveal = { isRevealed = true },
                        horizontalAlignment = Alignment.End
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = onlineFlowText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.5.sp
                        ),
                        color = onlineFlowColor
                    )
                }
            }
        }
    }
}

/** Backward compatibility alias */
@Composable
fun WheresMyMoneyCard(
    balances: WalletBalances,
    onClick: () -> Unit,
    onSetupClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ManageMoneyBalanceCard(
        balances = balances,
        onClick = onClick,
        onSetupClick = onSetupClick,
        modifier = modifier
    )
}

@Composable
private fun BlurredBalanceAmount(
    amount: Double,
    isBlurred: Boolean,
    onReveal: () -> Unit,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start
) {
    val haptics = rememberKanriHaptics()
    val animatedBlur by animateDpAsState(
        targetValue = if (isBlurred) 12.dp else 0.dp,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "balanceBlurAnimation"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(
                enabled = isBlurred,
                onClick = {
                    haptics.click()
                    onReveal()
                }
            )
            .then(
                if (animatedBlur > 0.dp) {
                    Modifier.blur(animatedBlur)
                } else {
                    Modifier
                }
            ),
        contentAlignment = if (horizontalAlignment == Alignment.End) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && isBlurred) {
            Text(
                text = "••••••••",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        } else {
            AnimatedNumberText(
                text = CurrencyUtils.formatCurrency(amount),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = if (amount < 0) ExpenseRed else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
