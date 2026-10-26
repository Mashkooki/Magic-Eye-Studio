package com.example.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.HistoryRepository
import com.example.data.StereogramHistoryEntity
import com.example.engine.StereogramEngine
import com.example.model.DepthStroke
import com.example.model.PatternType
import com.example.model.ShapeType
import com.example.model.StereogramConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class FullscreenViewerData(
    val title: String,
    val stereogramBitmap: Bitmap,
    val depthBitmap: Bitmap?,
    val startWithDepth: Boolean = false
)

enum class StudioPreviewMode {
    STEREOGRAM,
    DEPTH_MAP,
    SPLIT_COMPARE
}

data class StereogramUiState(
    val config: StereogramConfig = StereogramConfig(),
    val stereogramBitmap: Bitmap? = null,
    val depthPreviewBitmap: Bitmap? = null,
    val customDepthBitmap: Bitmap? = null,
    val customPatternBitmap: Bitmap? = null,
    val strokes: List<DepthStroke> = emptyList(),
    val brushDepth: Float = 1f,
    val brushSizeRatio: Float = 0.06f,
    val isGenerating: Boolean = false,
    val previewMode: StudioPreviewMode = StudioPreviewMode.STEREOGRAM,
    val fullscreenData: FullscreenViewerData? = null,
    val isDarkTheme: Boolean = true,
    val statusMessage: String? = null
)

class StereogramViewModel(
    private val repository: HistoryRepository,
    private val vazirmatnTypeface: Typeface?
) : ViewModel() {

    private val _uiState = MutableStateFlow(StereogramUiState())
    val uiState: StateFlow<StereogramUiState> = _uiState.asStateFlow()

    val historyList: StateFlow<List<StereogramHistoryEntity>> = repository.allHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private var previewJob: Job? = null
    private var generateJob: Job? = null

    init {
        generateStereogram(saveToHistoryAutomatically = false)
    }

    fun updateShapeType(shapeType: ShapeType) {
        _uiState.update { it.copy(config = it.config.copy(shapeType = shapeType)) }
        refreshDepthPreview()
    }

    fun updateText(text: String) {
        _uiState.update { it.copy(config = it.config.copy(text = text)) }
        refreshDepthPreview()
    }

    fun updatePatternType(patternType: PatternType) {
        _uiState.update { it.copy(config = it.config.copy(patternType = patternType)) }
    }

    fun updateInvertDepth(invert: Boolean) {
        _uiState.update { it.copy(config = it.config.copy(invertDepth = invert)) }
        refreshDepthPreview()
    }

    fun updateShowGuideDots(show: Boolean) {
        _uiState.update { it.copy(config = it.config.copy(showGuideDots = show)) }
        generateStereogram(saveToHistoryAutomatically = false)
    }

    fun updateWidth(width: Int) {
        val stepped = ((width + 25) / 50 * 50).coerceIn(400, 1600)
        _uiState.update { it.copy(config = it.config.copy(width = stepped)) }
        refreshDepthPreview()
    }

    fun updateHeight(height: Int) {
        val stepped = ((height + 25) / 50 * 50).coerceIn(300, 1200)
        _uiState.update { it.copy(config = it.config.copy(height = stepped)) }
        refreshDepthPreview()
    }

    fun updateStripWidth(stripWidth: Int) {
        val stepped = (stripWidth / 2 * 2).coerceIn(60, 300)
        _uiState.update { it.copy(config = it.config.copy(stripWidth = stepped)) }
    }

    fun updateDepthMultiplier(depthMultiplier: Int) {
        val clamped = depthMultiplier.coerceIn(4, 70)
        _uiState.update { it.copy(config = it.config.copy(depthMultiplier = clamped)) }
    }

    fun updateBlurRadius(blurRadius: Int) {
        val clamped = blurRadius.coerceIn(0, 12)
        _uiState.update { it.copy(config = it.config.copy(blurRadius = clamped)) }
        refreshDepthPreview()
    }

    fun updateBrushDepth(depth: Float) {
        _uiState.update { it.copy(brushDepth = depth.coerceIn(0f, 1f)) }
    }

    fun updateBrushSizeRatio(ratio: Float) {
        _uiState.update { it.copy(brushSizeRatio = ratio.coerceIn(0.02f, 0.16f)) }
    }

    fun addStroke(stroke: DepthStroke) {
        _uiState.update { it.copy(strokes = it.strokes + stroke) }
        refreshDepthPreview()
    }

    fun undoLastStroke() {
        _uiState.update { state ->
            if (state.strokes.isNotEmpty()) {
                state.copy(strokes = state.strokes.dropLast(1))
            } else {
                state
            }
        }
        refreshDepthPreview()
    }

    fun clearStrokes() {
        _uiState.update { it.copy(strokes = emptyList()) }
        refreshDepthPreview()
    }

    fun setPreviewMode(mode: StudioPreviewMode) {
        _uiState.update { it.copy(previewMode = mode) }
    }

    fun openCurrentInFullscreen() {
        val state = _uiState.value
        val stereo = state.stereogramBitmap ?: return
        val title = if (state.config.shapeType == ShapeType.TEXT) {
            "متن: ${state.config.text}"
        } else {
            state.config.shapeType.labelFa
        }
        _uiState.update {
            it.copy(
                fullscreenData = FullscreenViewerData(
                    title = title,
                    stereogramBitmap = stereo,
                    depthBitmap = state.depthPreviewBitmap,
                    startWithDepth = state.previewMode == StudioPreviewMode.DEPTH_MAP
                )
            )
        }
    }

    fun openCustomInFullscreen(title: String, stereogramBitmap: Bitmap, depthBitmap: Bitmap?) {
        _uiState.update {
            it.copy(
                fullscreenData = FullscreenViewerData(
                    title = title,
                    stereogramBitmap = stereogramBitmap,
                    depthBitmap = depthBitmap,
                    startWithDepth = false
                )
            )
        }
    }

    fun closeFullscreen() {
        _uiState.update { it.copy(fullscreenData = null) }
    }

    fun toggleTheme() {
        _uiState.update { it.copy(isDarkTheme = !it.isDarkTheme) }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    fun onDepthImagePicked(uri: Uri) {
        viewModelScope.launch {
            val bmp = repository.decodeBitmapFromUri(uri)
            if (bmp != null) {
                _uiState.update {
                    it.copy(
                        customDepthBitmap = bmp,
                        config = it.config.copy(shapeType = ShapeType.UPLOAD),
                        statusMessage = "تصویر نقشهٔ عمق با موفقیت بارگذاری شد"
                    )
                }
                refreshDepthPreview()
                generateStereogram(saveToHistoryAutomatically = false)
            } else {
                _uiState.update { it.copy(statusMessage = "خطا در خواندن تصویر نقشهٔ عمق") }
            }
        }
    }

    fun onPatternImagePicked(uri: Uri) {
        viewModelScope.launch {
            val bmp = repository.decodeBitmapFromUri(uri)
            if (bmp != null) {
                _uiState.update {
                    it.copy(
                        customPatternBitmap = bmp,
                        config = it.config.copy(patternType = PatternType.UPLOAD),
                        statusMessage = "تصویر الگوی پس‌زمینه با موفقیت بارگذاری شد"
                    )
                }
                generateStereogram(saveToHistoryAutomatically = false)
            } else {
                _uiState.update { it.copy(statusMessage = "خطا در خواندن تصویر الگو") }
            }
        }
    }

    private fun refreshDepthPreview() {
        previewJob?.cancel()
        val state = _uiState.value
        previewJob = viewModelScope.launch(Dispatchers.Default) {
            val preview = StereogramEngine.renderDepthPreview(
                config = state.config,
                customDepthBitmap = state.customDepthBitmap,
                strokes = state.strokes,
                typeface = vazirmatnTypeface
            )
            _uiState.update { it.copy(depthPreviewBitmap = preview) }
        }
    }

    fun generateStereogram(saveToHistoryAutomatically: Boolean = true) {
        generateJob?.cancel()
        val state = _uiState.value
        _uiState.update { it.copy(isGenerating = true, previewMode = StudioPreviewMode.STEREOGRAM) }

        generateJob = viewModelScope.launch(Dispatchers.Default) {
            val result = StereogramEngine.generate(
                config = state.config,
                customDepthBitmap = state.customDepthBitmap,
                customPatternBitmap = state.customPatternBitmap,
                strokes = state.strokes,
                typeface = vazirmatnTypeface
            )
            if (saveToHistoryAutomatically) {
                repository.saveToHistory(
                    config = state.config,
                    stereogramBitmap = result.stereogramBitmap,
                    depthBitmap = result.depthPreviewBitmap
                )
            }
            _uiState.update {
                it.copy(
                    stereogramBitmap = result.stereogramBitmap,
                    depthPreviewBitmap = result.depthPreviewBitmap,
                    isGenerating = false,
                    statusMessage = if (saveToHistoryAutomatically) {
                        "تصویر سه‌بعدی ساخته شد و در تاریخچه ذخیره گردید ✨"
                    } else {
                        it.statusMessage
                    }
                )
            }
        }
    }

    fun saveCurrentToHistory() {
        val state = _uiState.value
        val stereo = state.stereogramBitmap ?: return
        val depth = state.depthPreviewBitmap ?: return
        viewModelScope.launch {
            repository.saveToHistory(state.config, stereo, depth)
            _uiState.update { it.copy(statusMessage = "در تاریخچهٔ برنامه ذخیره شد") }
        }
    }

    fun saveCurrentToGallery() {
        val stereo = _uiState.value.stereogramBitmap ?: return
        saveBitmapToGallery(stereo)
    }

    fun saveBitmapToGallery(bitmap: Bitmap) {
        viewModelScope.launch {
            val result = repository.saveBitmapToGallery(bitmap)
            _uiState.update {
                it.copy(
                    statusMessage = if (result.isSuccess) {
                        "تصویر PNG با موفقیت در گالری (پوشهٔ Pictures/MagicEye) ذخیره شد"
                    } else {
                        "خطا در ذخیره تصویر: ${result.exceptionOrNull()?.message ?: ""}"
                    }
                )
            }
        }
    }

    fun exportCurrentPngToUri(uri: Uri) {
        val stereo = _uiState.value.fullscreenData?.stereogramBitmap
            ?: _uiState.value.stereogramBitmap
            ?: return
        viewModelScope.launch {
            val result = repository.writeBitmapToUri(stereo, uri)
            _uiState.update {
                it.copy(
                    statusMessage = if (result.isSuccess) {
                        "فایل PNG با موفقیت دانلود و ذخیره شد"
                    } else {
                        "ذخیره ممکن نشد: ${result.exceptionOrNull()?.message ?: ""}"
                    }
                )
            }
        }
    }

    fun shareCurrentStereogram(onIntentReady: (Intent) -> Unit) {
        val state = _uiState.value
        val stereo = state.stereogramBitmap ?: return
        val title = if (state.config.shapeType == ShapeType.TEXT) {
            state.config.text
        } else {
            state.config.shapeType.labelFa
        }
        shareBitmap(stereo, title, onIntentReady)
    }

    fun shareBitmap(bitmap: Bitmap, title: String, onIntentReady: (Intent) -> Unit) {
        viewModelScope.launch {
            val result = repository.createShareIntent(bitmap, title)
            result.onSuccess { intent ->
                onIntentReady(intent)
            }.onFailure { err ->
                _uiState.update { it.copy(statusMessage = "خطا در اشتراک‌گذاری: ${err.message}") }
            }
        }
    }

    fun loadFromHistory(item: StereogramHistoryEntity, onLoaded: () -> Unit) {
        viewModelScope.launch {
            val stereoBmp = withContext(Dispatchers.IO) {
                File(item.stereogramFilePath).takeIf { it.exists() }?.let {
                    BitmapFactory.decodeFile(it.absolutePath)
                }
            }
            val depthBmp = withContext(Dispatchers.IO) {
                File(item.depthFilePath).takeIf { it.exists() }?.let {
                    BitmapFactory.decodeFile(it.absolutePath)
                }
            }
            val shape = ShapeType.entries.find { it.id == item.shapeId } ?: ShapeType.TEXT
            val pattern = PatternType.entries.find { it.id == item.patternId } ?: PatternType.RANDOM_COLOR
            val restoredConfig = StereogramConfig(
                shapeType = shape,
                text = item.textValue,
                patternType = pattern,
                invertDepth = item.invertDepth,
                showGuideDots = _uiState.value.config.showGuideDots,
                width = item.width,
                height = item.height,
                stripWidth = item.stripWidth,
                depthMultiplier = item.depthMultiplier,
                blurRadius = item.blurRadius
            )
            _uiState.update {
                it.copy(
                    config = restoredConfig,
                    stereogramBitmap = stereoBmp ?: it.stereogramBitmap,
                    depthPreviewBitmap = depthBmp ?: it.depthPreviewBitmap,
                    fullscreenData = null,
                    statusMessage = "تنظیمات و تصویر «${item.title}» در استودیو بارگذاری شد"
                )
            }
            if (stereoBmp == null) {
                generateStereogram(saveToHistoryAutomatically = false)
            }
            onLoaded()
        }
    }

    fun deleteHistoryItem(item: StereogramHistoryEntity) {
        viewModelScope.launch {
            repository.deleteHistoryItem(item)
            _uiState.update { it.copy(statusMessage = "تصویر از تاریخچه حذف شد") }
        }
    }

    class Factory(
        private val context: Context,
        private val repository: HistoryRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val typeface = runCatching {
                ResourcesCompat.getFont(context, R.font.vazirmatn)
            }.getOrNull()
            return StereogramViewModel(repository, typeface) as T
        }
    }
}
