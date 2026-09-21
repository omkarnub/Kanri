package com.omkarnub.kanri.ui.export

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.data.export.ExportFormat
import com.omkarnub.kanri.data.export.ExportRange
import com.omkarnub.kanri.data.export.ExportSummary
import com.omkarnub.kanri.data.export.StatementExportRepository
import com.omkarnub.kanri.data.security.SecurityManager
import com.omkarnub.kanri.ui.home.formatCurrency
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportStatementSheet(
    initialRange: ExportRange = ExportRange.CurrentMonth,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val repository = remember { StatementExportRepository(context) }
    val coroutineScope = rememberCoroutineScope()

    var selectedFormat by remember { mutableStateOf(ExportFormat.PDF) }
    var selectedRange by remember { mutableStateOf<ExportRange>(initialRange) }

    var isProcessing by remember { mutableStateOf(false) }
    var previewSummary by remember { mutableStateOf<ExportSummary?>(null) }
    var exportSuccessSummary by remember { mutableStateOf<ExportSummary?>(null) }
    var exportedUri by remember { mutableStateOf<Uri?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Load preview when range changes
    LaunchedEffect(selectedRange) {
        previewSummary = repository.getPreview(selectedRange)
    }

    // Storage Access Framework Launcher
    val createDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(selectedFormat.mimeType)
    ) { uri ->
        if (uri != null) {
            isProcessing = true
            errorMessage = null
            coroutineScope.launch {
                val result = repository.exportStatementToUri(uri, selectedFormat, selectedRange)
                isProcessing = false
                result.onSuccess { summary ->
                    exportSuccessSummary = summary
                    exportedUri = uri
                    Toast.makeText(context, "Statement exported successfully!", Toast.LENGTH_SHORT).show()
                }.onFailure { err ->
                    errorMessage = "Export failed: ${err.localizedMessage}"
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(2.dp))
            )
        }
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Assessment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Text(
                        text = "Export Statement",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Download PDF report or Excel/CSV spreadsheet",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Success Notification Card
            if (exportSuccessSummary != null && exportedUri != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0D2818)),
                    border = BorderStroke(1.dp, Color(0xFF238636))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF3FB950),
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Statement Exported!",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF3FB950)
                                )
                                Text(
                                    text = "${exportSuccessSummary?.transactionCount} records saved as ${selectedFormat.name}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF8B949E)
                                )
                            }
                        }

                        // Share / Open Button
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    try {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = selectedFormat.mimeType
                                            putExtra(Intent.EXTRA_STREAM, exportedUri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Statement"))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Cannot share: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF238636),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share", fontSize = 13.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    try {
                                        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(exportedUri, selectedFormat.mimeType)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(viewIntent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "No app found to open file", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFF3FB950).copy(alpha = 0.5f))
                            ) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF3FB950))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open", fontSize = 13.sp, color = Color(0xFF3FB950))
                            }
                        }
                    }
                }
            }

            // Error Card
            if (errorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2B1015)),
                    border = BorderStroke(1.dp, Color(0xFFF85149))
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = Color(0xFFF85149),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            // Section 1: Choose Format
            Text(
                text = "EXPORT FORMAT",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // PDF Card
                FormatCard(
                    title = "PDF Statement",
                    subtitle = "Printable statement with summary & tables",
                    icon = Icons.Default.PictureAsPdf,
                    accentColor = Color(0xFFEF476F),
                    isSelected = selectedFormat == ExportFormat.PDF,
                    onClick = { selectedFormat = ExportFormat.PDF },
                    modifier = Modifier.weight(1f)
                )

                // CSV Card
                FormatCard(
                    title = "Excel / CSV",
                    subtitle = "Raw data for Excel, Sheets & accounting",
                    icon = Icons.Default.TableChart,
                    accentColor = Color(0xFF06D6A0),
                    isSelected = selectedFormat == ExportFormat.CSV,
                    onClick = { selectedFormat = ExportFormat.CSV },
                    modifier = Modifier.weight(1f)
                )
            }

            // Section 2: Choose Period Range
            Text(
                text = "DATE RANGE",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                val ranges = buildList {
                    if (initialRange is ExportRange.SpecificMonth) {
                        add(initialRange)
                    }
                    add(ExportRange.CurrentMonth)
                    add(ExportRange.LastMonth)
                    add(ExportRange.YearToDate)
                    add(ExportRange.AllTime)
                }.distinct()

                ranges.forEach { range ->
                    val isSelected = selectedRange == range
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedRange = range },
                        label = {
                            Text(
                                text = range.label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = MaterialTheme.colorScheme.outlineVariant,
                            selectedBorderColor = MaterialTheme.colorScheme.primary,
                            enabled = true,
                            selected = isSelected
                        )
                    )
                }
            }

            // Preview Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Statement Scope: ${repository.getPeriodTitle(selectedRange)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    val preview = previewSummary
                    if (preview != null) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${preview.transactionCount} transactions",
                                fontSize = 12.sp,
                                color = Color(0xFF8B949E)
                            )
                            Text(
                                text = "Spent: ${formatCurrency(preview.totalDebit)}",
                                fontSize = 12.sp,
                                color = Color(0xFFF85149),
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Received: ${formatCurrency(preview.totalCredit)}",
                                fontSize = 12.sp,
                                color = Color(0xFF3FB950),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        Text(
                            text = "Calculating summary...",
                            fontSize = 12.sp,
                            color = Color(0xFF8B949E)
                        )
                    }
                }
            }

            // Export Action Button
            Button(
                onClick = {
                    SecurityManager.isTemporarilyExempt = true
                    val suggestedFilename = repository.getSuggestedFilename(selectedFormat, selectedRange)
                    createDocLauncher.launch(suggestedFilename)
                },
                enabled = !isProcessing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                } else {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Export ${selectedFormat.label}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun FormatCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else accentColor,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 15.sp
            )
        }
    }
}
