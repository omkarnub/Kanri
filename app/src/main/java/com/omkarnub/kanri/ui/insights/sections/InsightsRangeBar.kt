package com.omkarnub.kanri.ui.insights.sections

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.ui.insights.InsightsMode
import com.omkarnub.kanri.ui.insights.InsightsRange
import com.omkarnub.kanri.util.rememberKanriHaptics
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private val ExpenseRed = Color(0xFFE54D2E)
private val IncomeSage = Color(0xFF30A46C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsRangeBar(
    currentRange: InsightsRange,
    currentMode: InsightsMode,
    onRangeSelected: (InsightsRange) -> Unit,
    onModeSelected: (InsightsMode) -> Unit,
    modifier: Modifier = Modifier,
    earliestTxMillis: Long? = null
) {
    var showCustomDatePicker by remember { mutableStateOf(false) }
    val haptics = rememberKanriHaptics()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1: Range Selector Pills (Month · 30 Days · 3 Months · Year · Custom)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val ranges = listOf(
                    "Month" to (currentRange is InsightsRange.Month),
                    "30D" to (currentRange is InsightsRange.Days30),
                    "3M" to (currentRange is InsightsRange.Months3),
                    "Year" to (currentRange is InsightsRange.Year),
                    "Custom" to (currentRange is InsightsRange.Custom)
                )

                ranges.forEach { (label, isSelected) ->
                    val bgColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                        label = "rangeBg"
                    )
                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
                        label = "rangeText"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(bgColor)
                            .clickable(role = Role.Tab) {
                                haptics.tick()
                                when (label) {
                                    "Month" -> onRangeSelected(InsightsRange.Month())
                                    "30D" -> onRangeSelected(InsightsRange.Days30)
                                    "3M" -> onRangeSelected(InsightsRange.Months3)
                                    "Year" -> onRangeSelected(InsightsRange.Year)
                                    "Custom" -> showCustomDatePicker = true
                                }
                            }
                            .semantics {
                                this.role = Role.Tab
                                this.contentDescription = "$label range, ${if (isSelected) "selected" else "not selected"}"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = textColor,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            // Row 2: Expense | Income Mode Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Expense option
                val isExpense = currentMode == InsightsMode.EXPENSE
                val expenseBg by animateColorAsState(
                    targetValue = if (isExpense) ExpenseRed.copy(alpha = 0.15f) else Color.Transparent,
                    label = "expBg"
                )
                val expenseText = if (isExpense) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(expenseBg)
                        .clickable(role = Role.RadioButton) {
                            haptics.tick()
                            onModeSelected(InsightsMode.EXPENSE)
                        }
                        .semantics {
                            this.role = Role.RadioButton
                            this.contentDescription = "Expense mode, ${if (isExpense) "active" else "inactive"}"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Spent",
                        color = expenseText,
                        fontSize = 13.sp,
                        fontWeight = if (isExpense) FontWeight.Bold else FontWeight.Medium
                    )
                }

                // Income option
                val isIncome = currentMode == InsightsMode.INCOME
                val incomeBg by animateColorAsState(
                    targetValue = if (isIncome) IncomeSage.copy(alpha = 0.15f) else Color.Transparent,
                    label = "incBg"
                )
                val incomeText = if (isIncome) IncomeSage else MaterialTheme.colorScheme.onSurfaceVariant

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(incomeBg)
                        .clickable(role = Role.RadioButton) {
                            haptics.tick()
                            onModeSelected(InsightsMode.INCOME)
                        }
                        .semantics {
                            this.role = Role.RadioButton
                            this.contentDescription = "Income mode, ${if (isIncome) "active" else "inactive"}"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Received",
                        color = incomeText,
                        fontSize = 13.sp,
                        fontWeight = if (isIncome) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }

    if (showCustomDatePicker) {
        val todayEpochDay = LocalDate.now().toEpochDay()
        val todayUtcMillis = todayEpochDay * 86_400_000L
        val minUtcMillis = earliestTxMillis ?: 0L

        val dateRangePickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = if (currentRange is InsightsRange.Custom) {
                currentRange.from.toEpochDay() * 86_400_000L
            } else null,
            initialSelectedEndDateMillis = if (currentRange is InsightsRange.Custom) {
                currentRange.to.toEpochDay() * 86_400_000L
            } else null,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis in minUtcMillis..todayUtcMillis
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = { showCustomDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val startMillis = dateRangePickerState.selectedStartDateMillis
                        val endMillis = dateRangePickerState.selectedEndDateMillis
                        if (startMillis != null && endMillis != null) {
                            val from = Instant.ofEpochMilli(startMillis).atZone(ZoneId.of("UTC")).toLocalDate()
                            val to = Instant.ofEpochMilli(endMillis).atZone(ZoneId.of("UTC")).toLocalDate()
                            onRangeSelected(InsightsRange.Custom(from, to))
                        }
                        showCustomDatePicker = false
                    },
                    enabled = dateRangePickerState.selectedStartDateMillis != null &&
                            dateRangePickerState.selectedEndDateMillis != null
                ) {
                    Text("Apply", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDatePicker = false }) {
                    Text("Cancel")
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                title = {
                    Text(
                        text = "Select Date Range",
                        modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
