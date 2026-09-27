package com.omkarnub.kanri.ui.lending

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable

enum class SplitRole {
    I_PAID,         // User paid total -> collects from friends (Money I Lent)
    SOMEONE_ELSE    // Friend paid total -> user owes their share (Money I Borrowed)
}

/**
 * SplitExpenseSheet delegates directly to the full-page SplitExpenseScreen.
 * Ensures backward-compatibility with all existing callers while delivering
 * the full-page monochrome levitating pie experience.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitExpenseSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSaveLenderSplit: (friendNames: List<String>, perPersonShare: Double, eventDescription: String) -> Unit,
    onSaveBorrowerSplit: (payerName: String, userShare: Double, eventDescription: String) -> Unit,
    existingPeople: List<String> = emptyList()
) {
    SplitExpenseScreen(
        onDismiss = onDismiss,
        onSaveLenderSplit = onSaveLenderSplit,
        onSaveBorrowerSplit = onSaveBorrowerSplit,
        existingPeople = existingPeople
    )
}
