package com.omkarnub.kanri.ui.common.kokonut

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.omkarnub.kanri.ui.theme.GoogleSansFlex
import kotlin.math.max

/**
 * High-performance, tactile 1:1 Aspect Ratio Avatar Cropper Dialog.
 * Allows interactive pinch-to-zoom and drag-to-pan with 1:1 square guide
 * and circular avatar mask preview.
 */
@Composable
fun AvatarCropDialog(
    sourceBitmap: Bitmap,
    onDismiss: () -> Unit,
    onCropSaved: (Bitmap) -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var viewportSizePx by remember { mutableFloatStateOf(0f) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xE60A0A0A))
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = Color.White
                        )
                    }

                    Text(
                        text = "Crop Avatar (1:1)",
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        color = Color.White
                    )

                    IconButton(
                        onClick = {
                            scale = 1f
                            panOffset = Offset.Zero
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset",
                            tint = Color.White
                        )
                    }
                }

                // 1:1 Viewport Crop Stage
                val cropBoxDp = 280.dp
                Box(
                    modifier = Modifier
                        .size(cropBoxDp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.5.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
                        .onSizeChanged { size: IntSize ->
                            viewportSizePx = size.width.toFloat()
                        }
                        .pointerInput(sourceBitmap) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(0.5f, 4.0f)
                                panOffset += pan
                            }
                        }
                        .clipToBounds(),
                    contentAlignment = Alignment.Center
                ) {
                    val composeBitmap = remember(sourceBitmap) { sourceBitmap.asImageBitmap() }

                    ComposeCanvas(modifier = Modifier.fillMaxSize()) {
                        val canvasW = size.width
                        val canvasH = size.height

                        // Calculate fit-center base scale
                        val bmpW = sourceBitmap.width.toFloat()
                        val bmpH = sourceBitmap.height.toFloat()
                        val baseScale = max(canvasW / bmpW, canvasH / bmpH)

                        val finalScale = baseScale * scale
                        val scaledW = bmpW * finalScale
                        val scaledH = bmpH * finalScale

                        val centerX = (canvasW - scaledW) / 2f + panOffset.x
                        val centerY = (canvasH - scaledH) / 2f + panOffset.y

                        drawImage(
                            image = composeBitmap,
                            dstOffset = androidx.compose.ui.unit.IntOffset(centerX.toInt(), centerY.toInt()),
                            dstSize = androidx.compose.ui.unit.IntSize(scaledW.toInt(), scaledH.toInt())
                        )

                        // Circular Avatar Mask overlay guideline
                        drawCircle(
                            color = Color.White.copy(alpha = 0.40f),
                            radius = canvasW / 2f - 4f,
                            center = Offset(canvasW / 2f, canvasH / 2f),
                            style = Stroke(width = 2.dp.toPx())
                        )

                        // Subtle grid lines (rule of thirds)
                        val oneThirdX = canvasW / 3f
                        val twoThirdX = canvasW * 2f / 3f
                        val oneThirdY = canvasH / 3f
                        val twoThirdY = canvasH * 2f / 3f

                        val gridColor = Color.White.copy(alpha = 0.18f)
                        drawLine(gridColor, Offset(oneThirdX, 0f), Offset(oneThirdX, canvasH), strokeWidth = 1f)
                        drawLine(gridColor, Offset(twoThirdX, 0f), Offset(twoThirdX, canvasH), strokeWidth = 1f)
                        drawLine(gridColor, Offset(0f, oneThirdY), Offset(canvasW, oneThirdY), strokeWidth = 1f)
                        drawLine(gridColor, Offset(0f, twoThirdY), Offset(canvasW, twoThirdY), strokeWidth = 1f)
                    }
                }

                // Help caption
                Text(
                    text = "Pinch to zoom and drag to position inside the 1:1 frame.",
                    fontFamily = GoogleSansFlex,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.65f)
                )

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(50),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = "Cancel",
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }

                    Button(
                        onClick = {
                            if (viewportSizePx > 0f) {
                                val cropped = cropBitmap(
                                    source = sourceBitmap,
                                    viewportSizePx = viewportSizePx,
                                    scale = scale,
                                    panOffset = panOffset,
                                    targetSize = 512
                                )
                                onCropSaved(cropped)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        )
                    ) {
                        Text(
                            text = "Save Avatar",
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}

/**
 * Extracts a high-resolution square cropped Bitmap from user pan & zoom parameters.
 */
private fun cropBitmap(
    source: Bitmap,
    viewportSizePx: Float,
    scale: Float,
    panOffset: Offset,
    targetSize: Int = 512
): Bitmap {
    val bmpW = source.width.toFloat()
    val bmpH = source.height.toFloat()
    val baseScale = max(viewportSizePx / bmpW, viewportSizePx / bmpH)
    val finalScale = baseScale * scale

    val scaledW = bmpW * finalScale
    val scaledH = bmpH * finalScale

    val leftInViewport = (viewportSizePx - scaledW) / 2f + panOffset.x
    val topInViewport = (viewportSizePx - scaledH) / 2f + panOffset.y

    // Map viewport (0, 0, viewportSizePx, viewportSizePx) to source image coordinates
    val srcX0 = ((0f - leftInViewport) / finalScale).coerceIn(0f, bmpW)
    val srcY0 = ((0f - topInViewport) / finalScale).coerceIn(0f, bmpH)
    val srcX1 = ((viewportSizePx - leftInViewport) / finalScale).coerceIn(0f, bmpW)
    val srcY1 = ((viewportSizePx - topInViewport) / finalScale).coerceIn(0f, bmpH)

    val srcCropWidth = max(1f, srcX1 - srcX0)
    val srcCropHeight = max(1f, srcY1 - srcY0)

    val out = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    val srcRect = android.graphics.Rect(
        srcX0.toInt().coerceAtLeast(0),
        srcY0.toInt().coerceAtLeast(0),
        srcX1.toInt().coerceAtMost(source.width),
        srcY1.toInt().coerceAtMost(source.height)
    )
    val dstRect = RectF(0f, 0f, targetSize.toFloat(), targetSize.toFloat())

    canvas.drawBitmap(source, srcRect, dstRect, paint)
    return out
}

