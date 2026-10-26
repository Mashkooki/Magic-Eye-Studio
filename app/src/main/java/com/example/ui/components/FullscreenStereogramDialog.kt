package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Rotate90DegreesCcw
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomInMap
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

private fun Bitmap.rotateBitmap90(): Bitmap {
    val matrix = Matrix().apply { postRotate(90f) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

@Composable
fun FullscreenStereogramDialog(
    title: String,
    stereogramBitmap: Bitmap,
    depthBitmap: Bitmap?,
    startWithDepth: Boolean = false,
    onDismiss: () -> Unit,
    onSaveToGallery: () -> Unit,
    onDownloadFile: (() -> Unit)? = null,
    onShare: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var showDepthOverlay by remember { mutableStateOf(startWithDepth) }
    var isRotated90 by remember { mutableStateOf(false) }
    var isImmersiveFull by remember { mutableStateOf(false) }
    var savedBannerVisible by remember { mutableStateOf(false) }

    val rotatedStereogram = remember(stereogramBitmap) { stereogramBitmap.rotateBitmap90() }
    val rotatedDepth = remember(depthBitmap) { depthBitmap?.rotateBitmap90() }

    DisposableEffect(activity) {
        val prevOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        onDispose {
            activity?.requestedOrientation = prevOrientation
        }
    }

    LaunchedEffect(savedBannerVisible) {
        if (savedBannerVisible) {
            delay(2500)
            savedBannerVisible = false
        }
    }

    BackHandler {
        if (isImmersiveFull) {
            isImmersiveFull = false
            isRotated90 = false
            scale = 1f
            offset = Offset.Zero
        } else {
            onDismiss()
        }
    }

    val baseBitmap = if (showDepthOverlay && depthBitmap != null) {
        if (isRotated90) rotatedDepth ?: depthBitmap else depthBitmap
    } else {
        if (isRotated90) rotatedStereogram else stereogramBitmap
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("fullscreen_viewer_scaffold"),
        containerColor = Color(0xFF05070B),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            if (!isImmersiveFull) {
                Surface(
                    color = Color(0xFF0F131F),
                    tonalElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 90-degree in-place rotation button (fills vertical screen without needing auto-rotate)
                            FilledTonalIconButton(
                                onClick = {
                                    isRotated90 = !isRotated90
                                    scale = 1f
                                    offset = Offset.Zero
                                },
                                modifier = Modifier.testTag("btn_rotate_image_90")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Rotate90DegreesCcw,
                                    contentDescription = "چرخش ۹۰ درجه تصویر"
                                )
                            }

                            // 4-arrows Fullscreen button: rotates & fills the entire screen cleanly!
                            FilledTonalIconButton(
                                onClick = {
                                    isImmersiveFull = true
                                    if (stereogramBitmap.width > stereogramBitmap.height) {
                                        isRotated90 = true
                                    }
                                    scale = 1f
                                    offset = Offset.Zero
                                },
                                modifier = Modifier.testTag("btn_reset_zoom")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ZoomOutMap,
                                    contentDescription = "تمام‌صفحه کامل"
                                )
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.testTag("btn_close_fullscreen")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "بستن نمای تصویر",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (!isImmersiveFull) {
                Surface(
                    color = Color(0xFF0F131F),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Row 1: Zoom & Orientation quick controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilledTonalIconButton(
                                    onClick = {
                                        scale = (scale + 0.35f).coerceAtMost(4.5f)
                                    },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .testTag("btn_zoom_in")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ZoomIn,
                                        contentDescription = "بزرگ‌نمایی"
                                    )
                                }

                                FilledTonalIconButton(
                                    onClick = {
                                        val next = (scale - 0.35f).coerceAtLeast(1f)
                                        scale = next
                                        if (next <= 1.05f) offset = Offset.Zero
                                    },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .testTag("btn_zoom_out")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ZoomOut,
                                        contentDescription = "کوچک‌نمایی"
                                    )
                                }

                                if (scale > 1.05f || offset != Offset.Zero) {
                                    OutlinedButton(
                                        onClick = {
                                            scale = 1f
                                            offset = Offset.Zero
                                        },
                                        modifier = Modifier.height(38.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("${(scale * 100).roundToInt()}٪")
                                    }
                                } else {
                                    Text(
                                        text = "بزرگ‌نمایی: ۱۰۰٪",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    isRotated90 = !isRotated90
                                    scale = 1f
                                    offset = Offset.Zero
                                },
                                modifier = Modifier
                                    .height(38.dp)
                                    .testTag("btn_bottom_rotate")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Rotate90DegreesCcw,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isRotated90) "حالت افقی" else "چرخش ۹۰°")
                            }
                        }

                        // Row 2: Primary Action Buttons (Discover Hidden Shape, Save, Download, Share)
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
                                        },
                                        contentColor = if (showDepthOverlay) {
                                            MaterialTheme.colorScheme.onSecondary
                                        } else {
                                            MaterialTheme.colorScheme.onPrimary
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
                                        contentDescription = null,
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
                                    savedBannerVisible = true
                                },
                                modifier = Modifier.testTag("btn_fullscreen_save")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SaveAlt,
                                    contentDescription = "ذخیره در گالری",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ذخیره", fontWeight = FontWeight.Bold)
                            }

                            if (onDownloadFile != null) {
                                FilledTonalIconButton(
                                    onClick = onDownloadFile,
                                    modifier = Modifier.testTag("btn_fullscreen_download")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = "دانلود PNG"
                                    )
                                }
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

                        // Row 3: Concise viewing instruction
                        Text(
                            text = "روش دیدن: به دو نقطهٔ سفید بالای تصویر نگاه کنید و نگاهتان را به پشت صفحه معطوف کنید تا دو نقطه به سه نقطه تبدیل شده و شکل سه‌بعدی ظاهر شود.",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.78f)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF05070B)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = baseBitmap.asImageBitmap(),
                contentDescription = title,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isImmersiveFull) {
                        detectTapGestures(
                            onTap = {
                                if (isImmersiveFull) {
                                    isImmersiveFull = false
                                }
                            },
                            onDoubleTap = {
                                if (scale > 1.05f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = 2.0f
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(1f, 5f)
                            scale = newScale
                            offset = if (newScale > 1.02f) {
                                Offset(
                                    x = (offset.x + pan.x).coerceIn(-1200f, 1200f),
                                    y = (offset.y + pan.y).coerceIn(-1200f, 1200f)
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

            // Floating exit button when in true full-bleed immersive mode
            AnimatedVisibility(
                visible = isImmersiveFull,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.75f),
                    shape = CircleShape,
                    modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        IconButton(
                            onClick = {
                                isRotated90 = !isRotated90
                                scale = 1f
                                offset = Offset.Zero
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Rotate90DegreesCcw,
                                contentDescription = "چرخش تصویر",
                                tint = Color.White
                            )
                        }
                        IconButton(
                            onClick = {
                                isImmersiveFull = false
                                isRotated90 = false
                                scale = 1f
                                offset = Offset.Zero
                            },
                            modifier = Modifier.testTag("btn_exit_immersive")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ZoomInMap,
                                contentDescription = "خروج از حالت تمام‌صفحه",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Saved confirmation banner
            AnimatedVisibility(
                visible = savedBannerVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
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
        }
    }
}
