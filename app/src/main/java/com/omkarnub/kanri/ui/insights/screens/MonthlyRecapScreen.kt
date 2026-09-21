package com.omkarnub.kanri.ui.insights.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.omkarnub.kanri.ui.insights.InsightsViewModel
import com.omkarnub.kanri.ui.insights.MonthlyRecapData
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyRecapScreen(
    targetMonth: YearMonth,
    viewModel: InsightsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val recapDataState: MonthlyRecapData? by viewModel.getMonthlyRecapFlow(targetMonth).collectAsState(initial = null)

    var isDarkStyle by remember { mutableStateOf(true) }
    var hideAmounts by remember { mutableStateOf(false) }

    val graphicsLayer = rememberGraphicsLayer()
    var pendingSaveBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // SAF Document Creator for saving PNG
    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/png")
    ) { uri: Uri? ->
        if (uri != null && pendingSaveBitmap != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    pendingSaveBitmap!!.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }
                Toast.makeText(context, "Recap card saved to device", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        pendingSaveBitmap = null
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Monthly Recap",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        val data = recapDataState

        if (data == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                com.omkarnub.kanri.ui.common.KanriWobbleLoader(color = MaterialTheme.colorScheme.primary)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Options: Dark / Cream style + Hide Amounts toggle
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Style pill: Dark | Cream
                        Row(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                                .padding(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isDarkStyle) MaterialTheme.colorScheme.onSurface else Color.Transparent)
                                    .clickable { isDarkStyle = true }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Dark",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkStyle) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (!isDarkStyle) MaterialTheme.colorScheme.onSurface else Color.Transparent)
                                    .clickable { isDarkStyle = false }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Cream",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!isDarkStyle) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Hide amounts switch
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Hide ₹",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Switch(
                                checked = hideAmounts,
                                onCheckedChange = { hideAmounts = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.surface,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }

            // The 4:5 Shareable Card with GraphicsLayer recording
            item {
                Box(
                    modifier = Modifier
                        .drawWithContent {
                            graphicsLayer.record {
                                this@drawWithContent.drawContent()
                            }
                            drawLayer(graphicsLayer)
                        }
                ) {
                    MonthlyRecapCard(
                        data = data,
                        isDarkStyle = isDarkStyle,
                        hideAmounts = hideAmounts
                    )
                }
            }

            // Action Buttons: Share & Save
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Share Button
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    val imageBitmap = graphicsLayer.toImageBitmap()
                                    val bitmap = imageBitmap.asAndroidBitmap()
                                    shareBitmap(context, bitmap, data.yearMonth)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Share failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Share", fontWeight = FontWeight.Bold)
                    }

                    // Save Button
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    val imageBitmap = graphicsLayer.toImageBitmap()
                                    val bitmap = imageBitmap.asAndroidBitmap()
                                    pendingSaveBitmap = bitmap
                                    val filename = "Kanri_Recap_${data.yearMonth}.png"
                                    saveLauncher.launch(filename)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Save failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = "Save",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

private fun shareBitmap(context: Context, bitmap: Bitmap, yearMonth: YearMonth) {
    try {
        val cachePath = File(context.cacheDir, "shared_recaps")
        cachePath.mkdirs()
        val file = File(cachePath, "Kanri_Recap_${yearMonth}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Kanri Monthly Recap - $yearMonth")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Monthly Recap"))
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to share image: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
