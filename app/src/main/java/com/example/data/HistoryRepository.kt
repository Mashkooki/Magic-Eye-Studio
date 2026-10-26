package com.example.data

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.model.StereogramConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class HistoryRepository(
    private val context: Context,
    private val historyDao: StereogramHistoryDao
) {
    val allHistory: Flow<List<StereogramHistoryEntity>> = historyDao.getAllHistory()

    suspend fun saveToHistory(
        config: StereogramConfig,
        stereogramBitmap: Bitmap,
        depthBitmap: Bitmap
    ): StereogramHistoryEntity = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "stereograms").apply { mkdirs() }
        val ts = System.currentTimeMillis()
        val stereoFile = File(dir, "stereo_$ts.png")
        val depthFile = File(dir, "depth_$ts.png")

        FileOutputStream(stereoFile).use { out ->
            stereogramBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        FileOutputStream(depthFile).use { out ->
            depthBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        val displayTitle = if (config.shapeType.id == "text" && config.text.isNotBlank()) {
            "متن: ${config.text}"
        } else {
            "${config.shapeType.labelFa} (${config.patternType.labelFa})"
        }

        val entity = StereogramHistoryEntity(
            title = displayTitle,
            shapeId = config.shapeType.id,
            textValue = config.text,
            patternId = config.patternType.id,
            invertDepth = config.invertDepth,
            width = config.width,
            height = config.height,
            stripWidth = config.stripWidth,
            depthMultiplier = config.depthMultiplier,
            blurRadius = config.blurRadius,
            stereogramFilePath = stereoFile.absolutePath,
            depthFilePath = depthFile.absolutePath,
            createdAt = ts
        )
        val id = historyDao.insertHistory(entity)
        entity.copy(id = id)
    }

    suspend fun deleteHistoryItem(item: StereogramHistoryEntity) = withContext(Dispatchers.IO) {
        runCatching { File(item.stereogramFilePath).delete() }
        runCatching { File(item.depthFilePath).delete() }
        historyDao.deleteById(item.id)
    }

    suspend fun saveBitmapToGallery(bitmap: Bitmap, filenamePrefix: String = "MagicEye"): Result<Uri> =
        withContext(Dispatchers.IO) {
            runCatching {
                val filename = "${filenamePrefix}_${System.currentTimeMillis()}.png"
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(
                            MediaStore.MediaColumns.RELATIVE_PATH,
                            Environment.DIRECTORY_PICTURES + "/MagicEye"
                        )
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                }
                val imageUri = resolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                ) ?: error("امکان ایجاد فایل در گالری وجود نداشت")

                resolver.openOutputStream(imageUri)?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                } ?: error("خطا در نوشتن فایل تصویر")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(imageUri, contentValues, null, null)
                }
                imageUri
            }
        }

    suspend fun writeBitmapToUri(bitmap: Bitmap, targetUri: Uri): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openOutputStream(targetUri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                } ?: error("امکان نوشتن در مسیر انتخابی وجود ندارد")
                Unit
            }
        }

    suspend fun createShareIntent(bitmap: Bitmap, title: String): Result<Intent> =
        withContext(Dispatchers.IO) {
            runCatching {
                val shareDir = File(context.cacheDir, "shared_images").apply { mkdirs() }
                val file = File(shareDir, "stereogram_share.png")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, "تصویر سه‌بعدی (Magic Eye) — $title")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
        }

    suspend fun decodeBitmapFromUri(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        }.getOrNull()
    }
}
