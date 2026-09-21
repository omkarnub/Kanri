package com.omkarnub.kanri.ui.insights.sections

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.ui.common.AnimatedNumberText
import com.omkarnub.kanri.ui.home.formatCurrency
import com.omkarnub.kanri.ui.insights.PersonalRecordsData
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val IncomeSage = Color(0xFF30A46C)

private data class RecordCardModel(
    val title: String,
    val heroValue: String,
    val detailLine: String,
    val isNewRecord: Boolean,
    val associatedMonth: YearMonth? = null,
    val associatedDay: LocalDate? = null
)

@Composable
fun PersonalRecordsSection(
    data: PersonalRecordsData,
    onSelectMonth: (YearMonth) -> Unit,
    onSelectDay: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val ymDtf = DateTimeFormatter.ofPattern("MMMM yyyy")
    val dayDtf = DateTimeFormatter.ofPattern("d MMM yyyy")

    val cards = mutableListOf<RecordCardModel>()

    data.highestSpendMonth?.let { (ym, amount) ->
        cards.add(
            RecordCardModel(
                title = "HIGHEST-SPEND MONTH",
                heroValue = formatCurrency(amount),
                detailLine = ym.format(ymDtf),
                isNewRecord = data.newRecordsBroken.contains("HIGHEST_SPEND_MONTH"),
                associatedMonth = ym
            )
        )
    }

    data.lowestSpendMonth?.let { (ym, amount) ->
        cards.add(
            RecordCardModel(
                title = "LOWEST-SPEND MONTH",
                heroValue = formatCurrency(amount),
                detailLine = ym.format(ymDtf),
                isNewRecord = data.newRecordsBroken.contains("LOWEST_SPEND_MONTH"),
                associatedMonth = ym
            )
        )
    }

    data.highestIncomeMonth?.let { (ym, amount) ->
        cards.add(
            RecordCardModel(
                title = "HIGHEST INCOME MONTH",
                heroValue = formatCurrency(amount),
                detailLine = ym.format(ymDtf),
                isNewRecord = data.newRecordsBroken.contains("HIGHEST_INCOME_MONTH"),
                associatedMonth = ym
            )
        )
    }

    data.bestSavingsMonth?.let { (ym, net) ->
        cards.add(
            RecordCardModel(
                title = "BEST SAVINGS MONTH",
                heroValue = formatCurrency(net),
                detailLine = "Net in ${ym.format(ymDtf)}",
                isNewRecord = data.newRecordsBroken.contains("BEST_SAVINGS_MONTH"),
                associatedMonth = ym
            )
        )
    }

    if (data.longestNoSpendStreakDays > 0) {
        cards.add(
            RecordCardModel(
                title = "LONGEST NO-SPEND",
                heroValue = "${data.longestNoSpendStreakDays} days",
                detailLine = data.longestStreakDateRange,
                isNewRecord = data.newRecordsBroken.contains("LONGEST_NO_SPEND_STREAK")
            )
        )
    }

    cards.add(
        RecordCardModel(
            title = "CURRENT NO-SPEND",
            heroValue = "${data.currentNoSpendStreakDays} days",
            detailLine = if (data.currentNoSpendStreakDays > 0) "Ongoing streak" else "Spent today",
            isNewRecord = false
        )
    )

    data.biggestSingleExpense?.let { (amount, payee, date) ->
        cards.add(
            RecordCardModel(
                title = "BIGGEST EXPENSE",
                heroValue = formatCurrency(amount),
                detailLine = "$payee · ${date.format(dayDtf)}",
                isNewRecord = data.newRecordsBroken.contains("BIGGEST_EXPENSE"),
                associatedDay = date
            )
        )
    }

    data.highestSpendDay?.let { (date, amount) ->
        cards.add(
            RecordCardModel(
                title = "HIGHEST-SPEND DAY",
                heroValue = formatCurrency(amount),
                detailLine = date.format(dayDtf),
                isNewRecord = data.newRecordsBroken.contains("HIGHEST_SPEND_DAY"),
                associatedDay = date
            )
        )
    }

    data.mostTransactionsInOneDay?.let { (date, count) ->
        cards.add(
            RecordCardModel(
                title = "MOST TRANSACTIONS",
                heroValue = "$count txns",
                detailLine = date.format(dayDtf),
                isNewRecord = data.newRecordsBroken.contains("MOST_TXNS_DAY"),
                associatedDay = date
            )
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Personal records: all-time financial records"
            },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Text(
                text = "PERSONAL RECORDS",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            val chunked = cards.chunked(2)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                chunked.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEach { item ->
                            RecordTileView(
                                item = item,
                                onSelectMonth = onSelectMonth,
                                onSelectDay = onSelectDay,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordTileView(
    item: RecordCardModel,
    onSelectMonth: (YearMonth) -> Unit,
    onSelectDay: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val isClickable = item.associatedMonth != null || item.associatedDay != null

    Surface(
        modifier = modifier
            .then(
                if (isClickable) {
                    Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            item.associatedMonth?.let { onSelectMonth(it) }
                            item.associatedDay?.let { onSelectDay(it) }
                        }
                } else Modifier
            ),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )

                if (item.isNewRecord) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = IncomeSage.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "New",
                            color = IncomeSage,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            AnimatedNumberText(
                text = item.heroValue,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp
                )
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = item.detailLine,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
