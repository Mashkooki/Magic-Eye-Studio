package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.DepthStroke
import com.example.model.PatternType
import com.example.model.ShapeType
import com.example.ui.components.DepthCanvasEditor
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StudioScreen(
    uiState: StereogramUiState,
    onShapeSelected: (ShapeType) -> Unit,
    onTextChanged: (String) -> Unit,
    onPatternSelected: (PatternType) -> Unit,
    onInvertDepthChanged: (Boolean) -> Unit,
    onShowGuideDotsChanged: (Boolean) -> Unit,
    onWidthChanged: (Int) -> Unit,
    onHeightChanged: (Int) -> Unit,
    onStripWidthChanged: (Int) -> Unit,
    onDepthMultiplierChanged: (Int) -> Unit,
    onBlurRadiusChanged: (Int) -> Unit,
    onBrushDepthChanged: (Float) -> Unit,
    onBrushSizeChanged: (Float) -> Unit,
    onStrokeAdded: (DepthStroke) -> Unit,
    onUndoStroke: () -> Unit,
    onClearStrokes: () -> Unit,
    onPickDepthImage: () -> Unit,
    onPickPatternImage: () -> Unit,
    onGenerateClick: () -> Unit,
    onSaveToGalleryClick: () -> Unit,
    onDownloadFileClick: () -> Unit,
    onShareClick: () -> Unit,
    onSaveHistoryClick: () -> Unit,
    onPreviewModeChanged: (StudioPreviewMode) -> Unit,
    onOpenFullscreenClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val config = uiState.config
    var shapeDropdownExpanded by remember { mutableStateOf(false) }
    var patternDropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("studio_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = stringResource(R.string.header_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // 1. Clean, Uncluttered Output Image Card (#cv + #dprev)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.5.dp,
                    brush = Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                        )
                    ),
                    shape = RoundedCornerShape(18.dp)
                )
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header row with title and Fullscreen 4-arrows button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${config.width}×${config.height} • ${
                                if (config.shapeType == ShapeType.TEXT) "متن: ${config.text}" else config.shapeType.labelFa
                            }",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    FilledTonalIconButton(
                        onClick = onOpenFullscreenClick,
                        modifier = Modifier.testTag("btn_open_fullscreen")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ZoomOutMap,
                            contentDescription = stringResource(R.string.btn_fullscreen)
                        )
                    }
                }

                // Clear Segmented Mode Tabs: Stereogram vs Hidden Shape vs Compare
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = uiState.previewMode == StudioPreviewMode.STEREOGRAM,
                        onClick = { onPreviewModeChanged(StudioPreviewMode.STEREOGRAM) },
                        label = { Text("استریوگرام") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.testTag("tab_mode_stereogram")
                    )
                    FilterChip(
                        selected = uiState.previewMode == StudioPreviewMode.DEPTH_MAP,
                        onClick = { onPreviewModeChanged(StudioPreviewMode.DEPTH_MAP) },
                        label = { Text("کشف شکل پنهان") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.testTag("btn_peek_depth")
                    )
                    FilterChip(
                        selected = uiState.previewMode == StudioPreviewMode.SPLIT_COMPARE,
                        onClick = { onPreviewModeChanged(StudioPreviewMode.SPLIT_COMPARE) },
                        label = { Text("مقایسه") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Compare,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.testTag("tab_mode_compare")
                    )
                }

                val aspect = (config.width.toFloat() / config.height.toFloat()).coerceIn(0.85f, 2.2f)

                if (uiState.previewMode == StudioPreviewMode.SPLIT_COMPARE) {
                    // Clean Split Compare View: Stereogram on top, Depth Map right below it without any overlap
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.stereogramBitmap?.let { stereoBmp ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(aspect)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black)
                                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                                    .clickable { onOpenFullscreenClick() }
                            ) {
                                Image(
                                    bitmap = stereoBmp.asImageBitmap(),
                                    contentDescription = "تصویر استریوگرام",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        uiState.depthPreviewBitmap?.let { depthBmp ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(aspect)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black)
                                    .border(1.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(12.dp))
                                    .clickable { onOpenFullscreenClick() }
                            ) {
                                Image(
                                    bitmap = depthBmp.asImageBitmap(),
                                    contentDescription = "نقشهٔ عمق (شکل پنهان)",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                } else {
                    // Single clean image viewport (either Stereogram or Depth Map) with zero overlapping clutter
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(aspect)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                            .clickable { onOpenFullscreenClick() }
                            .testTag("stereogram_output_canvas"),
                        contentAlignment = Alignment.Center
                    ) {
                        val displayedBmp = if (uiState.previewMode == StudioPreviewMode.DEPTH_MAP) {
                            uiState.depthPreviewBitmap ?: uiState.stereogramBitmap
                        } else {
                            uiState.stereogramBitmap
                        }

                        if (displayedBmp != null) {
                            Image(
                                bitmap = displayedBmp.asImageBitmap(),
                                contentDescription = "خروجی تصویر",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        if (uiState.isGenerating) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        strokeWidth = 2.5.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("در حال ساخت تصویر سه‌بعدی...", color = Color.White)
                                }
                            }
                        }
                    }
                }

                // Dedicated Fullscreen & Zoom Button right below the image
                OutlinedButton(
                    onClick = onOpenFullscreenClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_open_fullscreen_bar")
                ) {
                    Icon(imageVector = Icons.Default.ZoomOutMap, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("نمایش تمام‌صفحه، بزرگ‌نمایی و چرخش ۹۰ درجه")
                }

                // Action Row 1: Generate & Save to Gallery
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onGenerateClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_generate_main")
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.btn_generate),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    FilledTonalButton(
                        onClick = onSaveToGalleryClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_save_gallery")
                    ) {
                        Icon(imageVector = Icons.Default.SaveAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.btn_save_gallery),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Action Row 2: Download PNG & Share
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDownloadFileClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_download_png")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.btn_download_file))
                    }

                    OutlinedButton(
                        onClick = onShareClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_share_stereogram")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.btn_share))
                    }
                }
            }
        }

        // 2. Hidden Shape (Depth Map) Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.section_depth_shape),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                ExposedDropdownMenuBox(
                    expanded = shapeDropdownExpanded,
                    onExpandedChange = { shapeDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = config.shapeType.labelFa,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("انتخاب شکل پنهان") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = shapeDropdownExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .testTag("dropdown_shape")
                    )
                    ExposedDropdownMenu(
                        expanded = shapeDropdownExpanded,
                        onDismissRequest = { shapeDropdownExpanded = false }
                    ) {
                        ShapeType.entries.forEach { shape ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(shape.labelFa, fontWeight = FontWeight.Bold)
                                        Text(
                                            shape.descriptionFa,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    shapeDropdownExpanded = false
                                    onShapeSelected(shape)
                                },
                                modifier = Modifier.testTag("shape_option_${shape.id}")
                            )
                        }
                    }
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ShapeType.entries.forEach { shape ->
                        FilterChip(
                            selected = config.shapeType == shape,
                            onClick = { onShapeSelected(shape) },
                            label = { Text(shape.labelFa) },
                            modifier = Modifier.testTag("chip_shape_${shape.id}")
                        )
                    }
                }

                AnimatedVisibility(visible = config.shapeType == ShapeType.TEXT) {
                    OutlinedTextField(
                        value = config.text,
                        onValueChange = onTextChanged,
                        label = { Text("متن پنهان سه‌بعدی (فارسی یا انگلیسی)") },
                        placeholder = { Text("مثلاً: داوود") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_hidden_text")
                    )
                }

                AnimatedVisibility(visible = config.shapeType == ShapeType.DRAW) {
                    DepthCanvasEditor(
                        strokes = uiState.strokes,
                        brushDepth = uiState.brushDepth,
                        brushSizeRatio = uiState.brushSizeRatio,
                        aspectRatio = config.width.toFloat() / config.height.toFloat(),
                        onBrushDepthChange = onBrushDepthChanged,
                        onBrushSizeChange = onBrushSizeChanged,
                        onStrokeAdded = onStrokeAdded,
                        onUndoStroke = onUndoStroke,
                        onClearStrokes = onClearStrokes
                    )
                }

                AnimatedVisibility(visible = config.shapeType == ShapeType.UPLOAD) {
                    OutlinedButton(
                        onClick = onPickDepthImage,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_upload_depth_image")
                    ) {
                        Icon(imageVector = Icons.Default.UploadFile, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (uiState.customDepthBitmap != null) {
                                "تغییر تصویر نقشهٔ عمق انتخاب‌شده"
                            } else {
                                stringResource(R.string.btn_upload_depth)
                            }
                        )
                    }
                }

                // Clean Depth Map Preview Row (#dprev) - Tapping switches preview or opens fullscreen
                uiState.depthPreviewBitmap?.let { depthBmp ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            .clickable {
                                onPreviewModeChanged(
                                    if (uiState.previewMode == StudioPreviewMode.DEPTH_MAP) {
                                        StudioPreviewMode.STEREOGRAM
                                    } else {
                                        StudioPreviewMode.DEPTH_MAP
                                    }
                                )
                            }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "پیش‌نمایش نقشهٔ عمق فعلی (برای بزرگ‌نمایی لمس کنید)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "نواحی سفید به چشم نزدیک‌تر و نواحی تیره دورتر دیده می‌شوند.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Image(
                            bitmap = depthBmp.asImageBitmap(),
                            contentDescription = "پیش‌نمایش نقشه عمق",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(width = 110.dp, height = 72.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black)
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                .testTag("depth_preview_canvas")
                        )
                    }
                }
            }
        }

        // 3. Background Pattern & Depth Options Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.section_pattern),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                ExposedDropdownMenuBox(
                    expanded = patternDropdownExpanded,
                    onExpandedChange = { patternDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = config.patternType.labelFa,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("انتخاب الگوی پس‌زمینه") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = patternDropdownExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .testTag("dropdown_pattern")
                    )
                    ExposedDropdownMenu(
                        expanded = patternDropdownExpanded,
                        onDismissRequest = { patternDropdownExpanded = false }
                    ) {
                        PatternType.entries.forEach { pat ->
                            DropdownMenuItem(
                                text = { Text(pat.labelFa) },
                                onClick = {
                                    patternDropdownExpanded = false
                                    onPatternSelected(pat)
                                },
                                modifier = Modifier.testTag("pattern_option_${pat.id}")
                            )
                        }
                    }
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PatternType.entries.forEach { pat ->
                        FilterChip(
                            selected = config.patternType == pat,
                            onClick = { onPatternSelected(pat) },
                            label = { Text(pat.labelFa) },
                            modifier = Modifier.testTag("chip_pattern_${pat.id}")
                        )
                    }
                }

                AnimatedVisibility(visible = config.patternType == PatternType.UPLOAD) {
                    OutlinedButton(
                        onClick = onPickPatternImage,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_upload_pattern_image")
                    ) {
                        Icon(imageVector = Icons.Default.UploadFile, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (uiState.customPatternBitmap != null) {
                                "تغییر تصویر الگوی انتخاب‌شده"
                            } else {
                                stringResource(R.string.btn_upload_pattern)
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onInvertDepthChanged(!config.invertDepth) }
                        .padding(vertical = 4.dp)
                        .testTag("checkbox_invert_depth"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = config.invertDepth,
                        onCheckedChange = onInvertDepthChanged
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.label_invert_depth),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onShowGuideDotsChanged(!config.showGuideDots) }
                        .padding(vertical = 4.dp)
                        .testTag("checkbox_guide_dots"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = config.showGuideDots,
                        onCheckedChange = onShowGuideDotsChanged
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.label_guide_dots),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // 4. Sliders Card (W, H, S, M, B)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.section_parameters),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                LabeledSlider(
                    label = stringResource(R.string.param_width),
                    value = config.width,
                    valueRange = 400f..1600f,
                    steps = 23,
                    onValueChange = { onWidthChanged(it.roundToInt()) },
                    testTag = "slider_width"
                )

                LabeledSlider(
                    label = stringResource(R.string.param_height),
                    value = config.height,
                    valueRange = 300f..1200f,
                    steps = 17,
                    onValueChange = { onHeightChanged(it.roundToInt()) },
                    testTag = "slider_height"
                )

                LabeledSlider(
                    label = stringResource(R.string.param_strip_width),
                    value = config.stripWidth,
                    valueRange = 60f..300f,
                    steps = 119,
                    onValueChange = { onStripWidthChanged(it.roundToInt()) },
                    testTag = "slider_strip_width"
                )

                LabeledSlider(
                    label = stringResource(R.string.param_depth_intensity),
                    value = config.depthMultiplier,
                    valueRange = 4f..70f,
                    steps = 65,
                    onValueChange = { onDepthMultiplierChanged(it.roundToInt()) },
                    testTag = "slider_depth_intensity"
                )

                LabeledSlider(
                    label = stringResource(R.string.param_edge_blur),
                    value = config.blurRadius,
                    valueRange = 0f..12f,
                    steps = 11,
                    onValueChange = { onBlurRadiusChanged(it.roundToInt()) },
                    testTag = "slider_edge_blur"
                )
            }
        }

        // 5. Bottom Actions & Tips Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onGenerateClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_generate_bottom")
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.btn_generate), fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onDownloadFileClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_download_png_bottom")
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.btn_download_file))
                    }
                }

                OutlinedButton(
                    onClick = onSaveHistoryClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_save_to_history")
                ) {
                    Icon(imageVector = Icons.Default.BookmarkAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.btn_save_history))
                }

                Text(
                    text = stringResource(R.string.tip_upload_help),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = stringResource(R.string.tip_viewing_method),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    testTag: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.testTag(testTag)
        )
    }
}
