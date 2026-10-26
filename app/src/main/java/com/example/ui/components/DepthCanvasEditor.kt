package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.model.DepthStroke
import kotlin.math.min
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DepthCanvasEditor(
    strokes: List<DepthStroke>,
    brushDepth: Float,
    brushSizeRatio: Float,
    aspectRatio: Float,
    onBrushDepthChange: (Float) -> Unit,
    onBrushSizeChange: (Float) -> Unit,
    onStrokeAdded: (DepthStroke) -> Unit,
    onUndoStroke: () -> Unit,
    onClearStrokes: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activePoints = remember { mutableStateListOf<Offset>() }
    var canvasSize by remember { mutableStateOf(IntSize(1, 1)) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Brush,
                    contentDescription = "بوم نقاشی عمق",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "بوم نقاشی نقشهٔ عمق (سفید = نزدیک‌تر)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "${strokes.size} خط",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Interactive Drawing Surface
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio.coerceIn(0.8f, 2.0f))
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black)
                .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                .onSizeChanged { canvasSize = it }
                .testTag("depth_drawing_canvas")
                .pointerInput(brushDepth, brushSizeRatio) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            activePoints.clear()
                            val norm = Offset(
                                (offset.x / canvasSize.width.coerceAtLeast(1)).coerceIn(0f, 1f),
                                (offset.y / canvasSize.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                            )
                            activePoints.add(norm)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val pos = change.position
                            val norm = Offset(
                                (pos.x / canvasSize.width.coerceAtLeast(1)).coerceIn(0f, 1f),
                                (pos.y / canvasSize.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                            )
                            activePoints.add(norm)
                        },
                        onDragEnd = {
                            if (activePoints.isNotEmpty()) {
                                onStrokeAdded(
                                    DepthStroke(
                                        points = activePoints.toList(),
                                        depthValue = brushDepth,
                                        strokeWidthRatio = brushSizeRatio
                                    )
                                )
                                activePoints.clear()
                            }
                        },
                        onDragCancel = {
                            activePoints.clear()
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val minDim = min(size.width, size.height)

                fun drawSingleStroke(stroke: DepthStroke) {
                    if (stroke.points.isEmpty()) return
                    val gray = stroke.depthValue.coerceIn(0f, 1f)
                    val color = Color(gray, gray, gray, 1f)
                    val pxWidth = (stroke.strokeWidthRatio * minDim).coerceAtLeast(4f)

                    if (stroke.points.size == 1) {
                        val p = stroke.points.first()
                        drawCircle(
                            color = color,
                            radius = pxWidth / 2f,
                            center = Offset(p.x * size.width, p.y * size.height)
                        )
                    } else {
                        val path = Path()
                        stroke.points.forEachIndexed { index, pt ->
                            val x = pt.x * size.width
                            val y = pt.y * size.height
                            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(
                            path = path,
                            color = color,
                            style = Stroke(
                                width = pxWidth,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }

                strokes.forEach { drawSingleStroke(it) }

                if (activePoints.isNotEmpty()) {
                    drawSingleStroke(
                        DepthStroke(
                            points = activePoints,
                            depthValue = brushDepth,
                            strokeWidthRatio = brushSizeRatio
                        )
                    )
                }
            }

            if (strokes.isEmpty() && activePoints.isEmpty()) {
                Text(
                    text = "با انگشت خود روی این صفحهٔ سیاه نقاشی بکشید",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.55f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Preset depth level chips
        Text(
            text = "سطح برجستگی قلم (عمق):",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val presets = listOf(
                1.0f to "نزدیک‌ترین (سفید)",
                0.7f to "برجسته متوسط",
                0.4f to "لایهٔ میانی",
                0.0f to "پاک‌کن (سیاه)"
            )
            presets.forEach { (depthVal, label) ->
                val selected = kotlin.math.abs(brushDepth - depthVal) < 0.08f
                FilterChip(
                    selected = selected,
                    onClick = { onBrushDepthChange(depthVal) },
                    label = { Text(label) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(Color(depthVal, depthVal, depthVal))
                                .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        )
                    },
                    modifier = Modifier.testTag("depth_preset_${(depthVal * 100).roundToInt()}")
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Brush size slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "ضخامت قلم",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${(brushSizeRatio * 100).roundToInt()}٪",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = brushSizeRatio,
            onValueChange = onBrushSizeChange,
            valueRange = 0.02f..0.16f,
            modifier = Modifier.testTag("slider_brush_size")
        )

        // Undo & Clear buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onUndoStroke,
                enabled = strokes.isNotEmpty(),
                modifier = Modifier
                    .weight(1f)
                    .testTag("btn_undo_stroke")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "واگرد آخرین خط"
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("واگرد")
            }
            AssistChip(
                onClick = onClearStrokes,
                enabled = strokes.isNotEmpty(),
                label = { Text("پاک‌کردن بوم") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "پاک‌کردن بوم"
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    leadingIconContentColor = MaterialTheme.colorScheme.secondary
                ),
                modifier = Modifier.testTag("btn_clear_canvas")
            )
        }
    }
}
