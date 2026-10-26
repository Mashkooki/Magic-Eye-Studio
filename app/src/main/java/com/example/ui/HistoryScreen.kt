package com.example.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.StereogramHistoryEntity
import com.example.ui.components.FullscreenStereogramDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    historyItems: List<StereogramHistoryEntity>,
    onBackToStudio: () -> Unit,
    onLoadIntoStudio: (StereogramHistoryEntity) -> Unit,
    onSaveToGallery: (Bitmap) -> Unit,
    onShareBitmap: (Bitmap, String) -> Unit,
    onDeleteItem: (StereogramHistoryEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBackToStudio() }

    var selectedForFullscreen by remember {
        mutableStateOf<Triple<StereogramHistoryEntity, Bitmap, Bitmap?>?>(null)
    }

    if (historyItems.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Collections,
                        contentDescription = "تاریخچه خالی",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    )
                    Text(
                        text = "هنوز تصویری در تاریخچه ذخیره نشده است",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "در تب «استودیو ساخت»، دکمهٔ «بساز» یا «ذخیره در تاریخچه» را بزنید تا تصاویر سه‌بعدی شما همراه با نقشهٔ عمق و تنظیمات در اینجا نگهداری شوند.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = onBackToStudio,
                        modifier = Modifier.testTag("btn_empty_history_to_studio")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("رفتن به استودیو ساخت")
                    }
                }
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 280.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = modifier
                .fillMaxSize()
                .testTag("history_grid")
        ) {
            items(historyItems, key = { it.id }) { item ->
                HistoryItemCard(
                    item = item,
                    onOpenFullscreen = { stereoBmp, depthBmp ->
                        selectedForFullscreen = Triple(item, stereoBmp, depthBmp)
                    },
                    onLoadIntoStudio = { onLoadIntoStudio(item) },
                    onSaveToGallery = onSaveToGallery,
                    onShare = { bmp -> onShareBitmap(bmp, item.title) },
                    onDelete = { onDeleteItem(item) }
                )
            }
        }
    }

    selectedForFullscreen?.let { (item, stereoBmp, depthBmp) ->
        FullscreenStereogramDialog(
            title = item.title,
            stereogramBitmap = stereoBmp,
            depthBitmap = depthBmp,
            onDismiss = { selectedForFullscreen = null },
            onSaveToGallery = { onSaveToGallery(stereoBmp) },
            onShare = { onShareBitmap(stereoBmp, item.title) }
        )
    }
}

@Composable
private fun HistoryItemCard(
    item: StereogramHistoryEntity,
    onOpenFullscreen: (Bitmap, Bitmap?) -> Unit,
    onLoadIntoStudio: () -> Unit,
    onSaveToGallery: (Bitmap) -> Unit,
    onShare: (Bitmap) -> Unit,
    onDelete: () -> Unit
) {
    var stereoBitmap by remember(item.stereogramFilePath) { mutableStateOf<Bitmap?>(null) }
    var depthBitmap by remember(item.depthFilePath) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(item.stereogramFilePath, item.depthFilePath) {
        withContext(Dispatchers.IO) {
            stereoBitmap = File(item.stereogramFilePath).takeIf { it.exists() }?.let {
                BitmapFactory.decodeFile(it.absolutePath)
            }
            depthBitmap = File(item.depthFilePath).takeIf { it.exists() }?.let {
                BitmapFactory.decodeFile(it.absolutePath)
            }
        }
    }

    val dateStr = remember(item.createdAt) {
        SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale.getDefault()).format(Date(item.createdAt))
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .testTag("history_card_${item.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.5f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black)
                    .clickable {
                        stereoBitmap?.let { onOpenFullscreen(it, depthBitmap) }
                    }
            ) {
                stereoBitmap?.let { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Small depth thumbnail badge in bottom-left corner
                depthBitmap?.let { dBmp ->
                    Image(
                        bitmap = dBmp.asImageBitmap(),
                        contentDescription = "نقشهٔ عمق",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .size(width = 64.dp, height = 44.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                    )
                }

                FilledTonalIconButton(
                    onClick = { stereoBitmap?.let { onOpenFullscreen(it, depthBitmap) } },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .testTag("btn_history_fullscreen_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = "نمایش تمام‌صفحه"
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${item.width}×${item.height} • عمق ${item.depthMultiplier} • نوار ${item.stripWidth} • $dateStr",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("btn_history_delete_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "حذف از تاریخچه",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onLoadIntoStudio,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_history_load_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "بارگذاری در استودیو",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("بارگذاری")
                }

                FilledTonalIconButton(
                    onClick = { stereoBitmap?.let(onSaveToGallery) },
                    modifier = Modifier.testTag("btn_history_save_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.SaveAlt,
                        contentDescription = "ذخیره در گالری"
                    )
                }

                FilledTonalIconButton(
                    onClick = { stereoBitmap?.let(onShare) },
                    modifier = Modifier.testTag("btn_history_share_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "اشتراک‌گذاری"
                    )
                }
            }
        }
    }
}
