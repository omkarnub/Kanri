package com.omkarnub.kanri.ui.security

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.omkarnub.kanri.R
import com.omkarnub.kanri.data.security.BiometricHelper
import com.omkarnub.kanri.ui.theme.Panchang
import com.omkarnub.kanri.ui.theme.kanriPressFeedback
import com.omkarnub.kanri.ui.theme.KanriMotion

@Composable
fun LockScreen(
    onUnlocked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    var statusMessage by remember { mutableStateOf<String?>(null) }

    BackHandler {
        activity?.finish()
    }

    fun triggerSystemLock() {
        if (activity != null) {
            BiometricHelper.showBiometricPrompt(
                activity = activity,
                title = "Unlock Kanri",
                subtitle = "Confirm your fingerprint, face, or phone lock to continue",
                onSuccess = {
                    statusMessage = null
                    onUnlocked()
                },
                onError = { _, errString ->
                    statusMessage = errString.toString()
                },
                onFailed = {
                    statusMessage = "Authentication failed. Try again."
                }
            )
        }
    }

    // Automatically invoke Android's native system lock prompt when LockScreen appears
    LaunchedEffect(Unit) {
        triggerSystemLock()
    }

    val tokens = com.omkarnub.kanri.ui.theme.LocalKanriGlass.current

    Surface(
        modifier = modifier.fillMaxSize(),
        color = if (tokens != null) Color.Transparent else MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val contentBlock = @Composable {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp, vertical = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // KANRI logo in Panchang font
                    Text(
                        text = "KANRI",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = Panchang,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 4.sp,
                            fontSize = 32.sp
                        ),
                        color = tokens?.textPrimary ?: MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Kanri is Locked",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = tokens?.textSecondary ?: MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(36.dp))

                    // Unlock Now Button with Lock SVG
                    Button(
                        onClick = { triggerSystemLock() },
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .height(54.dp)
                            .kanriPressFeedback(pressedScale = 0.96f),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = tokens?.accent ?: MaterialTheme.colorScheme.primary,
                            contentColor = tokens?.onAccent ?: MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Unlock",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Unlock Now",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Status or error message
                    if (statusMessage != null) {
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = statusMessage ?: "",
                            color = tokens?.debit ?: MaterialTheme.colorScheme.error,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }

            if (tokens != null) {
                com.omkarnub.kanri.ui.common.KanriGlassCard(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .padding(16.dp),
                    level = com.omkarnub.kanri.ui.common.GlassLevel.Blur,
                    shape = RoundedCornerShape(28.dp)
                ) {
                    contentBlock()
                }
            } else {
                contentBlock()
            }
        }
    }
}

