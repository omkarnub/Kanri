package com.omkarnub.kanri.ui.popup

import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.omkarnub.kanri.data.db.KanriDatabase
import com.omkarnub.kanri.data.parser.DeduplicationResult
import com.omkarnub.kanri.data.parser.NotificationDeduplicationHelper
import com.omkarnub.kanri.data.parser.NotificationParser
import com.omkarnub.kanri.ui.notification.NotificationAccessHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstantPopupSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasOverlayPermission by remember {
        mutableStateOf(OverlayPermissionHelper.canDrawOverlays(context))
    }
    var hasNotificationAccess by remember {
        mutableStateOf(NotificationAccessHelper.isNotificationAccessGranted(context))
    }

    // Refresh permission statuses when returning to app
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            hasOverlayPermission = OverlayPermissionHelper.canDrawOverlays(context)
            hasNotificationAccess = NotificationAccessHelper.isNotificationAccessGranted(context)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF58A6FF).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = Color(0xFF58A6FF),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Instant Detection & Popups",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE6EDF3)
                        )
                        Text(
                            text = "Overlay cards & UPI receive detection",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF8B949E)
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF8B949E)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 1. Overlay Permission Status Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (hasOverlayPermission) Color(0xFF238636).copy(alpha = 0.5f) else Color(0xFFD29922).copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (hasOverlayPermission) Icons.Default.CheckCircle else Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = if (hasOverlayPermission) Color(0xFF3FB950) else Color(0xFFD29922),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (hasOverlayPermission) "Overlay Permission Active" else "Overlay Permission Required",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = if (hasOverlayPermission) Color(0xFF3FB950) else Color(0xFFD29922)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (hasOverlayPermission) Color(0xFF238636).copy(alpha = 0.2f) else Color(0xFFD29922).copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = if (hasOverlayPermission) "ACTIVE" else "DISABLED",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (hasOverlayPermission) Color(0xFF3FB950) else Color(0xFFD29922),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Allows Kanri to display a floating card directly over GPay, PhonePe, Paytm, or any app right when an SMS is received so you can categorize it with 1 tap.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8B949E),
                        lineHeight = 18.sp
                    )

                    if (!hasOverlayPermission) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                OverlayPermissionHelper.requestOverlayPermission(context)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD29922),
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Grant 'Display over other apps'", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. UPI Notification Access Card (Receive-Side Detection)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (hasNotificationAccess) Color(0xFF238636).copy(alpha = 0.5f) else Color(0xFF58A6FF).copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (hasNotificationAccess) Icons.Default.CheckCircle else Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = if (hasNotificationAccess) Color(0xFF3FB950) else Color(0xFF58A6FF),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (hasNotificationAccess) "UPI Notification Access Active" else "UPI Notification Access",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = if (hasNotificationAccess) Color(0xFF3FB950) else Color(0xFF58A6FF)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (hasNotificationAccess) Color(0xFF238636).copy(alpha = 0.2f) else Color(0xFF58A6FF).copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = if (hasNotificationAccess) "ACTIVE" else "ENABLE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (hasNotificationAccess) Color(0xFF3FB950) else Color(0xFF58A6FF),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Intercepts receive-side payment alerts from Google Pay, PhonePe, Paytm, and Amazon Pay to record credits and rewards instantly with 5-minute SMS deduplication.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8B949E),
                        lineHeight = 18.sp
                    )

                    if (!hasNotificationAccess) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                NotificationAccessHelper.openNotificationAccessSettings(context)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF58A6FF),
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Grant 'Device & App Notifications' Access", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Test Floating Overlay Button (Debit spend)
            Button(
                onClick = {
                    if (hasOverlayPermission) {
                        triggerTestPopup(context)
                        Toast.makeText(context, "Showing floating popup overlay...", Toast.LENGTH_SHORT).show()
                    } else {
                        triggerTestNotification(context)
                        Toast.makeText(context, "Overlay disabled — triggered heads-up notification fallback!", Toast.LENGTH_LONG).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF58A6FF),
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (hasOverlayPermission) "Test Instant Popup Overlay (₹380)" else "Test Fallback Alert",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Test GPay Money Received Button (Credit)
            OutlinedButton(
                onClick = {
                    triggerTestGPayReceived(context)
                    Toast.makeText(context, "Simulating GPay received payment: +₹1,250 from Rahul Sharma", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF3FB950)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF238636)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color(0xFF3FB950)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Test GPay Received Payment (+₹1,250)", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Test Notification Fallback Directly
            OutlinedButton(
                onClick = {
                    triggerTestNotification(context)
                    Toast.makeText(context, "Triggered heads-up notification with action chips!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF8B949E)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color(0xFF8B949E)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Test Notification Fallback Chips", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(18.dp))

            // How It Works Details
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Detection & Overlay Behaviors",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    FeatureBullet(
                        title = "Receive-Side Capture (GPay / Paytm / PhonePe)",
                        desc = "Intercepts incoming money notifications and reward scratch cards with smart 5-minute deduplication against bank SMS."
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    FeatureBullet(
                        title = "1-Tap Quick Categorization",
                        desc = "Floating card appears immediately over any active app with emoji category chips, auto-mapping the counterparty for future spends."
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    FeatureBullet(
                        title = "8-Second Auto-Dismiss",
                        desc = "Smooth linear countdown timer closes the card if ignored, keeping your phone screen clean."
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FeatureBullet(title: String, desc: String) {
    Column {
        Text(
            text = "• $title",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF58A6FF)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = desc,
            fontSize = 12.sp,
            color = Color(0xFF8B949E),
            lineHeight = 16.sp,
            modifier = Modifier.padding(start = 10.dp)
        )
    }
}

private fun triggerTestPopup(context: Context) {
    val intent = Intent(context, InstantPopupService::class.java).apply {
        putExtra(InstantPopupService.EXTRA_TRANSACTION_ID, 9999L)
        putExtra(InstantPopupService.EXTRA_AMOUNT, 380.00)
        putExtra(InstantPopupService.EXTRA_IS_DEBIT, true)
        putExtra(InstantPopupService.EXTRA_COUNTERPARTY, "Swiggy")
        putExtra(InstantPopupService.EXTRA_BANK, "HDFC Bank")
        putExtra(InstantPopupService.EXTRA_SOURCE_TYPE, "UPI")
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}

private fun triggerTestNotification(context: Context) {
    InstantPopupNotificationHelper.showInstantTransactionNotification(
        context = context,
        txId = 9999L,
        amount = 380.00,
        isDebit = true,
        counterparty = "Swiggy",
        bank = "HDFC Bank",
        sourceType = "UPI"
    )
}

private fun triggerTestGPayReceived(context: Context) {
    CoroutineScope(Dispatchers.IO).launch {
        val parsed = NotificationParser.parse(
            packageName = NotificationParser.PKG_GPAY,
            title = "Rahul Sharma sent you ₹1,250",
            text = "Google Pay • Received ₹1,250 from Rahul Sharma via UPI"
        )
        if (parsed != null) {
            val db = KanriDatabase.getDatabase(context)
            val atmMode = com.omkarnub.kanri.data.wallet.WalletPreferences.getInstance(context).atmWithdrawalMode
            val result = NotificationDeduplicationHelper.processIncomingTransaction(
                dao = db.transactionDao(),
                categoryDao = db.categoryDao(),
                parsed = parsed,
                atmMode = atmMode
            )
            val txId = when (result) {
                is DeduplicationResult.Inserted -> result.id
                is DeduplicationResult.Enriched -> result.id
                is DeduplicationResult.SkippedDuplicate -> result.existingId
            }

            withContext(Dispatchers.Main) {
                if (OverlayPermissionHelper.canDrawOverlays(context)) {
                    val intent = Intent(context, InstantPopupService::class.java).apply {
                        putExtra(InstantPopupService.EXTRA_TRANSACTION_ID, txId)
                        putExtra(InstantPopupService.EXTRA_AMOUNT, 1250.00)
                        putExtra(InstantPopupService.EXTRA_IS_DEBIT, false) // Received
                        putExtra(InstantPopupService.EXTRA_COUNTERPARTY, "Rahul Sharma")
                        putExtra(InstantPopupService.EXTRA_BANK, "Google Pay")
                        putExtra(InstantPopupService.EXTRA_SOURCE_TYPE, "UPI")
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    }
                } else {
                    InstantPopupNotificationHelper.showInstantTransactionNotification(
                        context = context,
                        txId = txId,
                        amount = 1250.00,
                        isDebit = false,
                        counterparty = "Rahul Sharma",
                        bank = "Google Pay",
                        sourceType = "UPI"
                    )
                }
            }
        }
    }
}
