package com.omkarnub.kanri.ui.health

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HealthCheckBanner(
    healthStatus: HealthStatus,
    onOpenDiagnostics: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!healthStatus.hasAnyIssue || healthStatus.warningSummary == null) return

    val isCritical = healthStatus.hasCriticalIssue
    val isLight = MaterialTheme.colorScheme.background == com.omkarnub.kanri.ui.theme.CreamSplashBackground
    val primaryColor = if (isCritical) Color(0xFFD62828) else if (isLight) Color(0xFFB45309) else Color(0xFFFFD166)
    val bgColor = if (isLight) (if (isCritical) Color(0xFFFBEBEB) else Color(0xFFFDF6E2)) else (if (isCritical) Color(0xFF261216) else Color(0xFF262012))
    val borderColor = if (isLight) (if (isCritical) Color(0xFFF5C6CB) else Color(0xFFE8DBB8)) else primaryColor.copy(alpha = 0.5f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenDiagnostics),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.WarningAmber,
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isCritical) "Background Detection Alert" else "Optimization Advisory",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
                Text(
                    text = healthStatus.warningSummary,
                    fontSize = 11.sp,
                    color = if (isLight) androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFFDCDFE4),
                    lineHeight = 15.sp
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = primaryColor.copy(alpha = if (isLight) 0.15f else 0.2f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Fix",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = if (isLight) androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF8B949E),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
