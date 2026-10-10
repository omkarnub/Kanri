package com.omkarnub.kanri.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.ui.theme.Panchang

@Composable
fun PrivacyPolicyDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "PRIVACY POLICY",
                    fontFamily = Panchang,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "100% Offline-First • Zero Telemetry Guarantee",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Feature Pillars Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PolicyBadgeRow(icon = Icons.Default.VisibilityOff, text = "Zero Trackers & Zero Analytics SDKs")
                        PolicyBadgeRow(icon = Icons.Default.Lock, text = "100% On-Device Financial Storage")
                        PolicyBadgeRow(icon = Icons.Default.CloudDone, text = "drive.file Scope Only (No General Access)")
                        PolicyBadgeRow(icon = Icons.Default.CheckCircle, text = "Client-Side Encrypted Google Backup")
                    }
                }

                // Section 1: Core Philosophy
                PolicySection(
                    title = "1. Data Sovereignty & Offline Architecture",
                    body = "Kanri is architected with strict data sovereignty. We do not operate proprietary user servers. All transactions, accounts, budgets, and categories are stored solely inside your device's private sandbox database. Balances are entered by the user and calculated locally; no balance data leaves the device. Your financial records are never harvested, monitored, sold, or shared."
                )

                // Section 2: Permissions Transparency
                Text(
                    text = "2. Device Permissions Breakdown",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                PermissionCard(
                    permissionName = "BIND_NOTIFICATION_LISTENER_SERVICE",
                    purpose = "Local Payment & Transaction Detection",
                    detail = "Parsed strictly on-device using local regex engines to auto-detect spend alerts and inbound payments from banking and UPI notifications. Never uploaded or saved on external servers."
                )

                PermissionCard(
                    permissionName = "SYSTEM_ALERT_WINDOW",
                    purpose = "Instant Category Popups",
                    detail = "Displays floating 1-tap categorization chips right after spending. Purely a local window manager display."
                )

                PermissionCard(
                    permissionName = "USE_BIOMETRIC",
                    purpose = "Ledger Security Lock",
                    detail = "Authenticates you via Android BiometricPrompt hardware security (TEE/Keystore). Kanri never reads biometric raw data."
                )

                PermissionCard(
                    permissionName = "INTERNET",
                    purpose = "Optional Google Drive Backup",
                    detail = "Only active when Cloud Backup is chosen. Connects directly to Google Drive API with zero intermediary proxy servers."
                )

                // Section 3: Google Drive & drive.file Scope
                PolicySection(
                    title = "3. Google Drive 'drive.file' Scope",
                    body = "When Cloud Backup is enabled, Kanri requests strictly the restricted 'drive.file' scope. This ensures Kanri can ONLY read and write its own encrypted backup file (kanri_backup.enc). Kanri CANNOT view, list, or access your photos, documents, or any other files on your Google Drive."
                )

                // Section 4: Telemetry & Third Parties
                PolicySection(
                    title = "4. Zero Third-Party Telemetry",
                    body = "Kanri contains ZERO advertising SDKs (no AdMob) and ZERO analytics platforms (no Firebase Analytics, Google Analytics, or Facebook SDK). Local crash diagnostics remain exclusively on your device."
                )

                // Section 5: Data Deletion & Exports
                PolicySection(
                    title = "5. User Deletion & Export Rights",
                    body = "You have full ownership of your data. You can export complete records anytime as JSON, PDF, or CSV. You can also permanently wipe all databases and profile data with 1 tap via Settings > Danger Zone > Reset All Data."
                )

                // Contact
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Developer Support",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "support@kanri.app • https://github.com/omkarnub/Kanri",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Got It")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = {
                    val browserIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://omkarnub.github.io/Kanri/")
                    )
                    try {
                        context.startActivity(browserIntent)
                    } catch (e: Exception) {
                        // Ignore
                    }
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInBrowser,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Web Policy")
            }
        }
    )
}

@Composable
private fun PolicyBadgeRow(
    icon: ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun PolicySection(
    title: String,
    body: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = body,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PermissionCard(
    permissionName: String,
    purpose: String,
    detail: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = purpose,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = permissionName,
                    fontSize = 10.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = detail,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
