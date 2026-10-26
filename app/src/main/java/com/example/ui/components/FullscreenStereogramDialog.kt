package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ZoomInMap
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@Composable
fun FullscreenStereogramDialog(
    title: String,
    stereogramBitmap: Bitmap,
    depthBitmap: Bitmap?,
    onDismiss: () -> Unit,
    onSaveToGallery: () -> Unit,
    onShare: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var showDepthOverlay by remember { mutableStateOf(false) }
    var isFullBleedZoomed by remember { mutableStateOf(false) }
    var isLandscapeLocked by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    var savedFeedbackVisible by remember { mutableStateOf(false) }

    // Restore original screen orientation when closing the fullscreen viewer
    DisposableEffect(activity) {
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        onDispose {
            activity?.requestedOrientation = originalOrientation
        }
    }

    LaunchedEffect(savedFeedbackVisible) {
        if (savedFeedbackVisible) {
            delay(2500)
            savedFeedbackVisible = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        BackHandler {
            if (isFullBleedZoomed) {
                isFullBleedZoomed = false
                scale = 1f
                offset = Offset.Zero
                controlsVisible = true
            } else {
                onDismiss()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            val activeBitmap = if (showDepthOverlay && depthBitmap != null) {
                depthBitmap
            } else {
                stereogramBitmap
            }

            // Main Interactive Image Canvas
            Image(
                bitmap = activeBitmap.asImageBitmap(),
                contentDescription = title,
                contentScale = if (isFullBleedZoomed) ContentScale.Crop else ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                controlsVisible = !controlsVisible
                            },
                            onDoubleTap = {
                                isFullBleedZoomed = !isFullBleedZoomed
                                scale = if (isFullBleedZoomed) 1.35f else 1f
                                offset = Offset.Zero
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(0.8f, 4.5f)
                            scale = newScale
                            offset = if (newScale > 1.02f || isFullBleedZoomed) {
                                Offset(
                                    x = (offset.x + pan.x).coerceIn(-1000f, 1000f),
                                    y = (offset.y + pan.y).coerceIn(-1000f, 1000f)
                                )
                            } else {
                                Offset.Zero
                            }
                        }
                    }
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
            )

            // Floating mini button when controls are hidden in immersive mode
            AnimatedVisibility(
                visible = !controlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
            ) {
                FilledTonalIconButton(
                    onClick = {
                        controlsVisible = true
                        isFullBleedZoomed = false
                        scale = 1f
                        offset = Offset.Zero
                    },
                    modifier = Modifier.testTag("btn_exit_immersive")
                ) {
                    Icon(
                        imageVector = Icons.Default.FullscreenExit,
                        contentDescription = "نمایش منو و بازگشت به اندازه عادی"
                    )
                }
            }

            // Top Bar
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.82f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Rotate Screen (Landscape / Portrait) for comfortable full-width Magic Eye viewing
                            FilledTonalIconButton(
                                onClick = {
                                    isLandscapeLocked = !isLandscapeLocked
                                    activity?.requestedOrientation = if (isLandscapeLocked) {
                                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                    } else {
                                        ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                    }
                                    scale = 1f
                                    offset = Offset.Zero
                                },
                                modifier = Modifier.testTag("btn_rotate_screen")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ScreenRotation,
                                    contentDescription = "چرخش افقی/عمودی صفحه"
                                )
                            }

                            // 4-arrows icon: Toggles Full-Screen Fill / Immersive Zoom
                            FilledTonalIconButton(
                                onClick = {
                                    isFullBleedZoomed = !isFullBleedZoomed
                                    if (isFullBleedZoomed) {
                                        scale = 1.25f
                                        offset = Offset.Zero
                                    } else {
                                        scale = 1f
                                        offset = Offset.Zero
                                    }
                                },
                                modifier = Modifier.testTag("btn_reset_zoom")
                            ) {
                                Icon(
                                    imageVector = if (isFullBleedZoomed) {
                                        Icons.Default.ZoomInMap
                                    } else {
                                        Icons.Default.ZoomOutMap
                                    },
                                    contentDescription = if (isFullBleedZoomed) {
                                        "بازگشت به اندازهٔ استاندارد"
                                    } else {
                                        "پر کردن کل صفحه"
                                    }
                                )
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.testTag("btn_close_fullscreen")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "بستن نمای تمام‌صفحه",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Toast notification banner inside Fullscreen when Saved
            AnimatedVisibility(
                visible = savedFeedbackVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 70.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        text = "✓ تصویر با کیفیت اصلی در گالری ذخیره شد",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                    )
                }
            }

            // Bottom Action Bar + Compact Viewing Tip
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.88f),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Action Buttons placed FIRST so they are always prominent and never cut off
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (depthBitmap != null) {
                                Button(
                                    onClick = { showDepthOverlay = !showDepthOverlay },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (showDepthOverlay) {
                                            MaterialTheme.colorScheme.secondary
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        }
                                    ),
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .testTag("btn_toggle_fullscreen_depth")
                                ) {
                                    Icon(
                                        imageVector = if (showDepthOverlay) {
                                            Icons.Default.VisibilityOff
                                        } else {
                                            Icons.Default.Visibility
                                        },
                                        contentDescription = "تغییر نمای نقشه عمق",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (showDepthOverlay) "نمایش استریوگرام" else "کشف شکل پنهان",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            FilledTonalButton(
                                onClick = {
                                    onSaveToGallery()
                                    savedFeedbackVisible = true
                                },
                                modifier = Modifier.testTag("btn_fullscreen_save")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SaveAlt,
                                    contentDescription = "ذخیره در گالری",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ذخیره")
                            }

                            FilledTonalIconButton(
                                onClick = onShare,
                                modifier = Modifier.testTag("btn_fullscreen_share")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "اشتراک‌گذاری"
                                )
                            }
                        }

                        // Compact viewing guide below the buttons
                        Text(
                            text = "روش دیدن: به دو نقطهٔ سفید بالای تصویر نگاه کنید و نگاهتان را به پشت صفحه معطوف کنید تا دو نقطه به سه نقطه تبدیل شده و شکل سه‌بعدی ظاهر شود. (برای مخفی‌شدن این کادر، یک بار روی عکس بزنید)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.82f),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
