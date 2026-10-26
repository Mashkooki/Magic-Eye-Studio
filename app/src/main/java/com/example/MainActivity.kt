package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.HistoryRepository
import com.example.ui.GuideScreen
import com.example.ui.HistoryScreen
import com.example.ui.StereogramViewModel
import com.example.ui.StudioScreen
import com.example.ui.components.FullscreenStereogramDialog
import com.example.ui.theme.MyApplicationTheme

enum class AppSection(val route: String) {
    STUDIO("studio"),
    HISTORY("history"),
    GUIDE("guide")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getInstance(applicationContext)
        val repository = HistoryRepository(applicationContext, database.historyDao())

        setContent {
            val viewModel: StereogramViewModel = viewModel(
                factory = StereogramViewModel.Factory(applicationContext, repository)
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val historyItems by viewModel.historyList.collectAsStateWithLifecycle()

            MyApplicationTheme(darkTheme = uiState.isDarkTheme) {
                MagicEyeApp(
                    viewModel = viewModel,
                    uiState = uiState,
                    historyCount = historyItems.size,
                    historyContent = { onSwitchToStudio ->
                        val context = LocalContext.current
                        HistoryScreen(
                            historyItems = historyItems,
                            onBackToStudio = onSwitchToStudio,
                            onOpenInFullscreen = { title, stereoBmp, depthBmp ->
                                viewModel.openCustomInFullscreen(title, stereoBmp, depthBmp)
                            },
                            onLoadIntoStudio = { item ->
                                viewModel.loadFromHistory(item, onLoaded = onSwitchToStudio)
                            },
                            onSaveToGallery = { bmp -> viewModel.saveBitmapToGallery(bmp) },
                            onShareBitmap = { bmp, title ->
                                viewModel.shareBitmap(bmp, title) { shareIntent ->
                                    context.startActivity(
                                        Intent.createChooser(shareIntent, "اشتراک‌گذاری تصویر سه‌بعدی")
                                    )
                                }
                            },
                            onDeleteItem = { item -> viewModel.deleteHistoryItem(item) }
                        )
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MagicEyeApp(
    viewModel: StereogramViewModel,
    uiState: com.example.ui.StereogramUiState,
    historyCount: Int,
    historyContent: @Composable (onSwitchToStudio: () -> Unit) -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var currentSection by rememberSaveable { mutableStateOf(AppSection.STUDIO) }

    val depthPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.onDepthImagePicked(uri)
        }
    }

    val patternPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.onPatternImagePicked(uri)
        }
    }

    val downloadPngLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
        if (uri != null) {
            viewModel.exportCurrentPngToUri(uri)
        }
    }

    LaunchedEffect(uiState.statusMessage) {
        val msg = uiState.statusMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    // If a Fullscreen Viewer item is active, render it directly in the Activity window (no broken Dialog window!)
    val fullscreenData = uiState.fullscreenData
    if (fullscreenData != null) {
        FullscreenStereogramDialog(
            title = fullscreenData.title,
            stereogramBitmap = fullscreenData.stereogramBitmap,
            depthBitmap = fullscreenData.depthBitmap,
            startWithDepth = fullscreenData.startWithDepth,
            onDismiss = viewModel::closeFullscreen,
            onSaveToGallery = { viewModel.saveBitmapToGallery(fullscreenData.stereogramBitmap) },
            onDownloadFile = {
                downloadPngLauncher.launch("stereogram_${System.currentTimeMillis()}.png")
            },
            onShare = {
                viewModel.shareBitmap(fullscreenData.stereogramBitmap, fullscreenData.title) { shareIntent ->
                    context.startActivity(
                        Intent.createChooser(shareIntent, "اشتراک‌گذاری تصویر سه‌بعدی")
                    )
                }
            }
        )
        return
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 680.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_launcher_custom_art),
                                contentDescription = stringResource(R.string.app_name),
                                modifier = Modifier
                                    .padding(end = 10.dp)
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Text(
                                text = stringResource(R.string.header_title),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.toggleTheme() },
                            modifier = Modifier.testTag("btn_toggle_theme")
                        ) {
                            Icon(
                                imageVector = if (uiState.isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "تغییر تم روشن و تیره"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            bottomBar = {
                if (!isExpandedScreen) {
                    NavigationBar {
                        NavigationBarItem(
                            selected = currentSection == AppSection.STUDIO,
                            onClick = { currentSection = AppSection.STUDIO },
                            icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                            label = { Text(stringResource(R.string.tab_studio)) },
                            modifier = Modifier.testTag("nav_studio")
                        )
                        NavigationBarItem(
                            selected = currentSection == AppSection.HISTORY,
                            onClick = { currentSection = AppSection.HISTORY },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (historyCount > 0) {
                                            Badge { Text(historyCount.toString()) }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Collections, contentDescription = null)
                                }
                            },
                            label = { Text(stringResource(R.string.tab_history)) },
                            modifier = Modifier.testTag("nav_history")
                        )
                        NavigationBarItem(
                            selected = currentSection == AppSection.GUIDE,
                            onClick = { currentSection = AppSection.GUIDE },
                            icon = { Icon(Icons.Default.Visibility, contentDescription = null) },
                            label = { Text(stringResource(R.string.tab_guide)) },
                            modifier = Modifier.testTag("nav_guide")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isExpandedScreen) {
                    NavigationRail {
                        NavigationRailItem(
                            selected = currentSection == AppSection.STUDIO,
                            onClick = { currentSection = AppSection.STUDIO },
                            icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                            label = { Text(stringResource(R.string.tab_studio)) },
                            modifier = Modifier.testTag("rail_studio")
                        )
                        NavigationRailItem(
                            selected = currentSection == AppSection.HISTORY,
                            onClick = { currentSection = AppSection.HISTORY },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (historyCount > 0) {
                                            Badge { Text(historyCount.toString()) }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Collections, contentDescription = null)
                                }
                            },
                            label = { Text(stringResource(R.string.tab_history)) },
                            modifier = Modifier.testTag("rail_history")
                        )
                        NavigationRailItem(
                            selected = currentSection == AppSection.GUIDE,
                            onClick = { currentSection = AppSection.GUIDE },
                            icon = { Icon(Icons.Default.Visibility, contentDescription = null) },
                            label = { Text(stringResource(R.string.tab_guide)) },
                            modifier = Modifier.testTag("rail_guide")
                        )
                    }
                }

                when (currentSection) {
                    AppSection.STUDIO -> {
                        StudioScreen(
                            uiState = uiState,
                            onShapeSelected = viewModel::updateShapeType,
                            onTextChanged = viewModel::updateText,
                            onPatternSelected = viewModel::updatePatternType,
                            onInvertDepthChanged = viewModel::updateInvertDepth,
                            onShowGuideDotsChanged = viewModel::updateShowGuideDots,
                            onWidthChanged = viewModel::updateWidth,
                            onHeightChanged = viewModel::updateHeight,
                            onStripWidthChanged = viewModel::updateStripWidth,
                            onDepthMultiplierChanged = viewModel::updateDepthMultiplier,
                            onBlurRadiusChanged = viewModel::updateBlurRadius,
                            onBrushDepthChanged = viewModel::updateBrushDepth,
                            onBrushSizeChanged = viewModel::updateBrushSizeRatio,
                            onStrokeAdded = viewModel::addStroke,
                            onUndoStroke = viewModel::undoLastStroke,
                            onClearStrokes = viewModel::clearStrokes,
                            onPickDepthImage = {
                                depthPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onPickPatternImage = {
                                patternPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onGenerateClick = { viewModel.generateStereogram(saveToHistoryAutomatically = true) },
                            onSaveToGalleryClick = viewModel::saveCurrentToGallery,
                            onDownloadFileClick = {
                                downloadPngLauncher.launch("stereogram_${System.currentTimeMillis()}.png")
                            },
                            onShareClick = {
                                viewModel.shareCurrentStereogram { shareIntent ->
                                    context.startActivity(
                                        Intent.createChooser(shareIntent, "اشتراک‌گذاری تصویر سه‌بعدی")
                                    )
                                }
                            },
                            onSaveHistoryClick = viewModel::saveCurrentToHistory,
                            onPreviewModeChanged = viewModel::setPreviewMode,
                            onOpenFullscreenClick = viewModel::openCurrentInFullscreen,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    AppSection.HISTORY -> {
                        historyContent { currentSection = AppSection.STUDIO }
                    }

                    AppSection.GUIDE -> {
                        GuideScreen(
                            onBackToStudio = { currentSection = AppSection.STUDIO },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
