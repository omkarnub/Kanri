package com.omkarnub.kanri.ui.lending

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.ui.common.GlassLevel
import com.omkarnub.kanri.ui.common.KanriGlassCard
import com.omkarnub.kanri.ui.common.LocalHazeState
import com.omkarnub.kanri.ui.theme.LocalKanriGlass

/**
 * A sleek, frosted glass card featured prominently in the Lend & Borrow menu
 * with fixed 50dp blur and 10% dark tint.
 */
@Composable
fun LendingSplitGroupCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    KanriGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        level = GlassLevel.EntrySheet,
        hazeState = LocalHazeState.current,
        onClick = onClick
    ) {
        LendingSplitGroupContent()
    }
}

@Composable
private fun LendingSplitGroupContent() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.CallSplit,
                contentDescription = "Split a Group Expense",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "GROUP EXPENSE",
                    fontFamily = com.omkarnub.kanri.ui.theme.GoogleSansFlex,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.3.sp,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Split a Group Expense",
                    fontFamily = com.omkarnub.kanri.ui.theme.GoogleSansFlex,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Open Split Expense",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
