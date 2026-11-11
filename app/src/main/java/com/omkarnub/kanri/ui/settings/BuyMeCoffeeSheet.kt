package com.omkarnub.kanri.ui.settings

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.omkarnub.kanri.R
import com.omkarnub.kanri.ui.common.GlassLevel
import com.omkarnub.kanri.ui.common.KanriGlassCard
import com.omkarnub.kanri.ui.common.KanriModalBottomSheet
import com.omkarnub.kanri.ui.common.kanriPrimaryButtonColors
import com.omkarnub.kanri.ui.theme.GoogleSansFlex
import com.omkarnub.kanri.ui.theme.Panchang
import com.omkarnub.kanri.util.QrCodeHelper
import com.omkarnub.kanri.util.QrCodeHelper.toBitmap
import com.omkarnub.kanri.util.QrMatrix
import com.omkarnub.kanri.util.SupportDonationConfig
import com.omkarnub.kanri.util.rememberKanriHaptics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private enum class SupportMethod {
    UPI,
    BINANCE_PAY,
    CRYPTO
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyMeCoffeeSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val haptics = rememberKanriHaptics()

    var selectedMethod by rememberSaveable { mutableStateOf(SupportMethod.UPI) }
    var selectedUpiAmount by rememberSaveable { mutableStateOf<Int?>(null) }

    val upiUri = QrCodeHelper.buildUpiPaymentUri(amountRupees = selectedUpiAmount)
    val copiedUpiText = stringResource(R.string.buy_me_coffee_copied_upi)
    val copiedAddressText = stringResource(R.string.buy_me_coffee_copied_address)
    val upiChooserTitle = stringResource(R.string.buy_me_coffee_upi_chooser)
    val noUpiAppText = stringResource(R.string.buy_me_coffee_no_upi_app)
    val shareChooserTitle = stringResource(R.string.buy_me_coffee_share_qr_chooser)
    val shareErrorText = stringResource(R.string.buy_me_coffee_share_qr_error)

    KanriModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
                .navigationBarsPadding(),
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
                    imageVector = Icons.Default.LocalCafe,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.buy_me_coffee_title),
                fontFamily = Panchang,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(R.string.buy_me_coffee_tagline),
                fontFamily = GoogleSansFlex,
                fontSize = 12.5.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3-way payment selector
            KanriGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                level = GlassLevel.Flat
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SupportTabChip(
                        label = stringResource(R.string.buy_me_coffee_tab_upi),
                        selected = selectedMethod == SupportMethod.UPI,
                        onClick = {
                            haptics.tick()
                            selectedMethod = SupportMethod.UPI
                        },
                        modifier = Modifier.weight(1f)
                    )
                    SupportTabChip(
                        label = stringResource(R.string.buy_me_coffee_tab_binance_pay),
                        selected = selectedMethod == SupportMethod.BINANCE_PAY,
                        onClick = {
                            haptics.tick()
                            selectedMethod = SupportMethod.BINANCE_PAY
                        },
                        modifier = Modifier.weight(1f)
                    )
                    SupportTabChip(
                        label = stringResource(R.string.buy_me_coffee_tab_crypto),
                        selected = selectedMethod == SupportMethod.CRYPTO,
                        onClick = {
                            haptics.tick()
                            selectedMethod = SupportMethod.CRYPTO
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (selectedMethod) {
                SupportMethod.UPI -> {
                    Text(
                        text = stringResource(R.string.buy_me_coffee_upi_scan_hint),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SupportDonationConfig.UPI_PRESET_AMOUNTS.forEach { preset ->
                            val isSelected = selectedUpiAmount == preset
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    haptics.tick()
                                    selectedUpiAmount = preset
                                },
                                label = {
                                    Text(
                                        text = if (preset == null) {
                                            stringResource(R.string.buy_me_coffee_upi_amount_any)
                                        } else {
                                            "₹$preset"
                                        },
                                        fontFamily = GoogleSansFlex,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    QrWhiteFrame(
                        contentDescription = stringResource(R.string.buy_me_coffee_upi_qr_desc)
                    ) {
                        VectorQrCode(
                            content = upiUri,
                            modifier = Modifier.size(196.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    CopyableInfoRow(
                        label = "${stringResource(R.string.buy_me_coffee_payee_label)}: ${SupportDonationConfig.UPI_PAYEE_NAME}",
                        value = SupportDonationConfig.UPI_ID,
                        copyDescription = stringResource(R.string.buy_me_coffee_copy_upi),
                        onCopy = {
                            haptics.click()
                            scope.launch {
                                clipboard.setClipEntry(
                                    ClipData.newPlainText("UPI ID", SupportDonationConfig.UPI_ID).toClipEntry()
                                )
                            }
                            Toast.makeText(context, copiedUpiText, Toast.LENGTH_SHORT).show()
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            haptics.primaryAction()
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, upiUri.toUri())
                                val chooser = Intent.createChooser(intent, upiChooserTitle)
                                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(chooser)
                            } catch (_: Exception) {
                                Toast.makeText(context, noUpiAppText, Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = kanriPrimaryButtonColors()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.buy_me_coffee_open_upi_app),
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                SupportMethod.BINANCE_PAY -> {
                    Text(
                        text = stringResource(R.string.buy_me_coffee_binance_heading),
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.buy_me_coffee_binance_scan_hint),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.qr_binance_pay),
                            contentDescription = stringResource(R.string.buy_me_coffee_binance_qr_desc),
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .padding(10.dp)
                                .size(204.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    KanriGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        level = GlassLevel.Flat
                    ) {
                        Text(
                            text = stringResource(R.string.buy_me_coffee_binance_single_phone_tip),
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            haptics.click()
                            scope.launch {
                                val shared = shareDrawableQr(
                                    context = context,
                                    drawableRes = R.drawable.qr_binance_pay,
                                    fileName = "kanri_binance_pay_qr.png",
                                    chooserTitle = shareChooserTitle
                                )
                                if (!shared) {
                                    Toast.makeText(context, shareErrorText, Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.buy_me_coffee_share_qr),
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                SupportMethod.CRYPTO -> {
                    Text(
                        text = stringResource(R.string.buy_me_coffee_crypto_heading),
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.buy_me_coffee_crypto_sub),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    QrWhiteFrame(
                        contentDescription = stringResource(R.string.buy_me_coffee_crypto_qr_desc)
                    ) {
                        VectorQrCode(
                            content = SupportDonationConfig.USDT_EVM_ADDRESS,
                            modifier = Modifier.size(196.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    CopyableInfoRow(
                        label = stringResource(R.string.buy_me_coffee_crypto_address_label),
                        value = SupportDonationConfig.USDT_EVM_ADDRESS,
                        monospace = true,
                        copyDescription = stringResource(R.string.buy_me_coffee_copy_address),
                        onCopy = {
                            haptics.click()
                            scope.launch {
                                clipboard.setClipEntry(
                                    ClipData.newPlainText(
                                        "USDT BEP20 / Base Address",
                                        SupportDonationConfig.USDT_EVM_ADDRESS
                                    ).toClipEntry()
                                )
                            }
                            Toast.makeText(context, copiedAddressText, Toast.LENGTH_SHORT).show()
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.buy_me_coffee_crypto_warning),
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            haptics.click()
                            scope.launch {
                                val shared = shareDynamicQr(
                                    context = context,
                                    content = SupportDonationConfig.USDT_EVM_ADDRESS,
                                    fileName = "kanri_usdt_bep20_base_qr.png",
                                    chooserTitle = shareChooserTitle
                                )
                                if (!shared) {
                                    Toast.makeText(context, shareErrorText, Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.buy_me_coffee_share_qr),
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = {
                    haptics.click()
                    onDismiss()
                },
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
            ) {
                Text(
                    text = stringResource(R.string.buy_me_coffee_close),
                    fontFamily = GoogleSansFlex,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun SupportTabChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        Color.Transparent
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = 42.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        color = containerColor,
        shape = RoundedCornerShape(10.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontFamily = GoogleSansFlex,
                fontSize = 12.5.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun QrWhiteFrame(
    contentDescription: String,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.semantics { this.contentDescription = contentDescription }
    ) {
        Box(
            modifier = Modifier.padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

@Composable
private fun VectorQrCode(
    content: String,
    modifier: Modifier = Modifier
) {
    val qrMatrix by produceState<QrMatrix?>(initialValue = null, key1 = content) {
        value = withContext(Dispatchers.Default) {
            QrCodeHelper.encodeQrMatrix(content)
        }
    }

    val matrix = qrMatrix
    if (matrix != null) {
        Canvas(modifier = modifier.background(Color.White)) {
            val cellSize = size.width / matrix.size.toFloat()
            for (y in 0 until matrix.size) {
                for (x in 0 until matrix.size) {
                    if (matrix.get(x, y)) {
                        drawRect(
                            color = Color.Black,
                            topLeft = Offset(x * cellSize, y * cellSize),
                            size = Size(cellSize + 0.5f, cellSize + 0.5f)
                        )
                    }
                }
            }
        }
    } else {
        Box(modifier = modifier.background(Color.White))
    }
}

@Composable
private fun CopyableInfoRow(
    label: String,
    value: String,
    copyDescription: String,
    onCopy: () -> Unit,
    monospace: Boolean = false
) {
    KanriGlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        level = GlassLevel.Flat
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    fontFamily = if (monospace) FontFamily.Monospace else GoogleSansFlex,
                    fontSize = if (monospace) 12.sp else 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = onCopy) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = copyDescription,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private suspend fun shareDrawableQr(
    context: Context,
    drawableRes: Int,
    fileName: String,
    chooserTitle: String
): Boolean {
    val file = withContext(Dispatchers.IO) {
        try {
            val bitmap = BitmapFactory.decodeResource(context.resources, drawableRes) ?: return@withContext null
            val outFile = File(context.cacheDir, fileName)
            FileOutputStream(outFile).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            outFile
        } catch (_: Exception) {
            null
        }
    } ?: return false
    return launchShareImage(context, file, chooserTitle)
}

private suspend fun shareDynamicQr(
    context: Context,
    content: String,
    fileName: String,
    chooserTitle: String
): Boolean {
    val file = withContext(Dispatchers.IO) {
        try {
            val bitmap = QrCodeHelper.encodeQrMatrix(content).toBitmap(pixelsPerModule = 16)
            val outFile = File(context.cacheDir, fileName)
            FileOutputStream(outFile).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            outFile
        } catch (_: Exception) {
            null
        }
    } ?: return false
    return launchShareImage(context, file, chooserTitle)
}

private fun launchShareImage(context: Context, file: File, chooserTitle: String): Boolean {
    return try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
        true
    } catch (_: Exception) {
        false
    }
}

