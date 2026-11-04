package com.omkarnub.kanri.ui.lending

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import dev.chrisbanes.haze.HazeState

/**
 * SplitExpenseSheet delegates directly to SplitExpenseEntryDialog for the unified
 * Kokonut pop-up workflow (Amount Pad -> Fill Details -> Summary Drawer + Slide To Confirm).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitExpenseSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSaveLenderSplit: (friendNames: List<String>, perPersonShare: Double, eventDescription: String) -> Unit,
    onSaveBorrowerSplit: (payerName: String, userShare: Double, eventDescription: String) -> Unit,
    existingPeople: List<String> = emptyList(),
    initialAmount: Double? = null,
    initialDescription: String? = null,
    hazeState: HazeState? = null
) {
    SplitExpenseEntryDialog(
        onDismiss = onDismiss,
        onSaveLenderSplit = onSaveLenderSplit,
        onSaveBorrowerSplit = onSaveBorrowerSplit,
        existingPeople = existingPeople,
        initialAmount = initialAmount,
        initialDescription = initialDescription,
        hazeState = hazeState
    )
}
