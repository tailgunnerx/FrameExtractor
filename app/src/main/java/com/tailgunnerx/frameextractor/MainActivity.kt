package com.tailgunnerx.frameextractor

import android.content.ContentValues
import android.graphics.Bitmap
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tailgunnerx.frameextractor.ui.theme.FrameExtractorTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.time.DurationUnit
import kotlin.time.toDuration

// Custom UI Icons built directly so you don't have to download external Google ML/Icon packages!
val PauseIcon = ImageVector.Builder("Pause", 24.dp, 24.dp, 24f, 24f).apply {
    path(fill = SolidColor(Color.White)) {
        moveTo(6f, 19f); horizontalLineToRelative(4f); verticalLineTo(5f); horizontalLineTo(6f); verticalLineToRelative(14f); close()
        moveTo(14f, 5f); verticalLineToRelative(14f); horizontalLineToRelative(4f); verticalLineTo(5f); horizontalLineToRelative(-4f); close()
    }
}.build()

val SkipNextIcon = ImageVector.Builder("SkipNext", 24.dp, 24.dp, 24f, 24f).apply {
    path(fill = SolidColor(Color.White)) {
        moveTo(6f, 18f); lineToRelative(8.5f, -6f); lineTo(6f, 6f); verticalLineToRelative(12f); close()
        moveTo(16f, 6f); verticalLineToRelative(12f); horizontalLineToRelative(2f); verticalLineTo(6f); horizontalLineToRelative(-2f); close()
    }
}.build()

val SkipPreviousIcon = ImageVector.Builder("SkipPrevious", 24.dp, 24.dp, 24f, 24f).apply {
    path(fill = SolidColor(Color.White)) {
        moveTo(6f, 6f); horizontalLineToRelative(2f); verticalLineToRelative(12f); horizontalLineTo(6f); close()
        moveTo(8.5f, 12f); lineTo(17f, 18f); verticalLineTo(6f); lineToRelative(-8.5f, 6f); close()
    }
}.build()

val PlayIcon = ImageVector.Builder("Play", 24.dp, 24.dp, 24f, 24f).apply {
    path(fill = SolidColor(Color.White)) {
        moveTo(8f, 5f); verticalLineToRelative(14f); lineToRelative(11f, -7f); close()
    }
}.build()

val AddIcon = ImageVector.Builder("Add", 24.dp, 24.dp, 24f, 24f).apply {
    path(fill = SolidColor(Color.White)) {
        moveTo(19f, 13f); horizontalLineToRelative(-6f); verticalLineToRelative(6f); horizontalLineToRelative(-2f); verticalLineToRelative(-6f); horizontalLineTo(5f); verticalLineToRelative(-2f); horizontalLineToRelative(6f); verticalLineTo(5f); horizontalLineToRelative(2f); verticalLineToRelative(6f); horizontalLineToRelative(6f); verticalLineToRelative(2f); close()
    }
}.build()

val RemoveIcon = ImageVector.Builder("Remove", 24.dp, 24.dp, 24f, 24f).apply {
    path(fill = SolidColor(Color.White)) {
        moveTo(19f, 13f); horizontalLineTo(5f); verticalLineToRelative(-2f); horizontalLineToRelative(14f); verticalLineToRelative(2f); close()
    }
}.build()

val CloseIcon = ImageVector.Builder("Close", 24.dp, 24.dp, 24f, 24f).apply {
    path(fill = SolidColor(Color.White)) {
        moveTo(19f, 6.41f); lineTo(17.59f, 5f); lineTo(12f, 10.59f); lineTo(6.41f, 5f); lineTo(5f, 6.41f); lineTo(10.59f, 12f); lineTo(5f, 17.59f); lineTo(6.41f, 19f); lineTo(12f, 13.41f); lineTo(17.59f, 19f); lineTo(19f, 17.59f); lineTo(13.41f, 12f); lineTo(19f, 6.41f); close()
    }
}.build()

val SaveIcon = ImageVector.Builder("Save", 24.dp, 24.dp, 24f, 24f).apply {
    path(fill = SolidColor(Color.White)) {
        moveTo(12f, 16f); lineTo(7f, 11f); horizontalLineToRelative(4f); verticalLineTo(4f); horizontalLineToRelative(2f); verticalLineToRelative(7f); horizontalLineToRelative(4f); lineTo(12f, 16f); close()
        moveTo(5f, 18f); horizontalLineToRelative(14f); verticalLineToRelative(2f); horizontalLineTo(5f); close()
    }
}.build()

val SettingsIcon = ImageVector.Builder("Settings", 24.dp, 24.dp, 24f, 24f).apply {
    path(
        fill = SolidColor(Color.White),
        pathFillType = androidx.compose.ui.graphics.PathFillType.EvenOdd
    ) {
        moveTo(19.14f, 12.94f)
        curveToRelative(0.04f, -0.3f, 0.06f, -0.61f, 0.06f, -0.94f)
        curveToRelative(0f, -0.32f, -0.02f, -0.64f, -0.07f, -0.94f)
        lineToRelative(2.03f, -1.58f)
        curveToRelative(0.18f, -0.14f, 0.23f, -0.41f, 0.12f, -0.61f)
        lineToRelative(-1.92f, -3.32f)
        curveToRelative(-0.12f, -0.22f, -0.37f, -0.29f, -0.59f, -0.22f)
        lineToRelative(-2.39f, 0.96f)
        curveToRelative(-0.5f, -0.38f, -1.03f, -0.7f, -1.62f, -0.94f)
        lineToRelative(-0.36f, -2.54f)
        curveToRelative(-0.04f, -0.24f, -0.24f, -0.41f, -0.48f, -0.41f)
        horizontalLineToRelative(-3.84f)
        curveToRelative(-0.24f, 0f, -0.43f, 0.17f, -0.47f, 0.41f)
        lineToRelative(-0.36f, 2.54f)
        curveToRelative(-0.59f, 0.24f, -1.13f, 0.57f, -1.62f, 0.94f)
        lineToRelative(-2.39f, -0.96f)
        curveToRelative(-0.22f, -0.08f, -0.47f, 0f, -0.59f, 0.22f)
        lineToRelative(-1.92f, 3.32f)
        curveToRelative(-0.12f, 0.21f, -0.07f, 0.47f, 0.12f, 0.61f)
        lineToRelative(2.03f, 1.58f)
        curveToRelative(-0.05f, 0.3f, -0.09f, 0.63f, -0.09f, 0.94f)
        curveToRelative(0f, 0.31f, 0.02f, 0.64f, 0.07f, 0.94f)
        lineToRelative(-2.03f, 1.58f)
        curveToRelative(-0.18f, 0.14f, -0.23f, 0.41f, -0.12f, 0.61f)
        lineToRelative(1.92f, 3.32f)
        curveToRelative(0.12f, 0.22f, 0.37f, 0.29f, 0.59f, 0.22f)
        lineToRelative(2.39f, -0.96f)
        curveToRelative(0.5f, 0.38f, 1.03f, 0.7f, 1.62f, 0.94f)
        lineToRelative(0.36f, 2.54f)
        curveToRelative(0.05f, 0.24f, 0.24f, 0.41f, 0.48f, 0.41f)
        horizontalLineToRelative(3.84f)
        curveToRelative(0.24f, 0f, 0.44f, -0.17f, 0.47f, -0.41f)
        lineToRelative(0.36f, -2.54f)
        curveToRelative(0.59f, -0.24f, 1.13f, -0.56f, 1.62f, -0.94f)
        lineToRelative(2.39f, 0.96f)
        curveToRelative(0.22f, 0.08f, 0.47f, 0f, 0.59f, -0.22f)
        lineToRelative(1.92f, -3.32f)
        curveToRelative(0.12f, -0.22f, 0.07f, -0.47f, -0.12f, -0.61f)
        lineToRelative(-2.01f, -1.58f)
        close()
        moveTo(12f, 15.6f)
        curveToRelative(-1.98f, 0f, -3.6f, -1.62f, -3.6f, -3.6f)
        reflectiveCurveToRelative(1.62f, -3.6f, 3.6f, -3.6f)
        reflectiveCurveToRelative(3.6f, 1.62f, 3.6f, 3.6f)
        reflectiveCurveToRelative(-1.62f, 3.6f, -3.6f, 3.6f)
        close()
    }
}.build()

val OpenInNewIcon = ImageVector.Builder("OpenInNew", 24.dp, 24.dp, 24f, 24f).apply {
    path(fill = SolidColor(Color.White)) {
        moveTo(19f, 19f); horizontalLineTo(5f); verticalLineTo(5f); horizontalLineToRelative(7f); verticalLineTo(3f); horizontalLineTo(5f)
        curveToRelative(-1.11f, 0f, -2f, 0.9f, -2f, 2f); verticalLineToRelative(14f)
        curveToRelative(0f, 1.1f, 0.89f, 2f, 2f, 2f); horizontalLineToRelative(14f)
        curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f); verticalLineToRelative(-7f); horizontalLineToRelative(-2f); verticalLineToRelative(7f); close()
        moveTo(14f, 3f); verticalLineToRelative(2f); horizontalLineToRelative(3.59f); lineToRelative(-9.83f, 9.83f); lineToRelative(1.41f, 1.41f); lineTo(19f, 6.41f); verticalLineTo(10f); horizontalLineToRelative(2f); verticalLineTo(3f); horizontalLineToRelative(-7f); close()
    }
}.build()

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Issue #2: Edge-to-edge full screen with transparent status/navigation bars
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        setContent {
            // Issue #3: Material You Dynamic Color Theme (dark mode for video studio canvas)
            FrameExtractorTheme(darkTheme = true, dynamicColor = true) {
                FrameExtractorApp()
            }
        }
    }
}

@Composable
fun FrameExtractorApp() {
    val context = LocalContext.current
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var currentPositionUs by remember { mutableLongStateOf(0L) }
    val currentPositionMs = currentPositionUs / 1000L
    var fineTunePositionCenter by remember { mutableLongStateOf(0L) }
    var fineTunePositionOffset by remember { mutableLongStateOf(0L) }

    var videoDurationMs by remember { mutableStateOf(0L) }
    var videoWidth by remember { mutableIntStateOf(0) }
    var videoHeight by remember { mutableIntStateOf(0) }
    var totalFrameCount by remember { mutableIntStateOf(0) }
    var isExtracting by remember { mutableStateOf(false) }

    var videoResolution by remember { mutableStateOf("") }
    var videoFps by remember { mutableFloatStateOf(30f) }
    var msPerFrame by remember { mutableLongStateOf(33L) }

    var isPlaying by remember { mutableStateOf(false) }
    var isScrubbing by remember { mutableStateOf(false) }
    var playSpeedFps by remember { mutableFloatStateOf(5f) }

    val prefs = remember { context.getSharedPreferences("frame_extractor_prefs", android.content.Context.MODE_PRIVATE) }
    var showFineSelection by remember { mutableStateOf(prefs.getBoolean("show_fine_selection", false)) }
    var showSettingsMenu by remember { mutableStateOf(false) }
    var showChangelogDialog by remember { mutableStateOf(false) }

    // Exact frame presentation timestamps (in microseconds) indexed for instant frame stepping
    var frameTimestampsUs by remember { mutableStateOf<LongArray?>(null) }

    val scope = rememberCoroutineScope()
    val retriever = remember { MediaMetadataRetriever() }

    DisposableEffect(Unit) {
        onDispose {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }

    fun resetFineTuneSlider() {
        fineTunePositionCenter = currentPositionUs / 1000L
        fineTunePositionOffset = 0
    }

    // Snap or step to exact frame timestamp if indexed, falling back to msPerFrame math
    fun findClosestFrameIndex(tsList: LongArray, targetUs: Long): Int {
        val searchIdx = tsList.binarySearch(targetUs)
        if (searchIdx >= 0) return searchIdx
        val insertPos = (-searchIdx - 1).coerceIn(0, tsList.size)
        return when {
            insertPos == 0 -> 0
            insertPos >= tsList.size -> tsList.lastIndex
            else -> {
                val prevDiff = targetUs - tsList[insertPos - 1]
                val nextDiff = tsList[insertPos] - targetUs
                if (prevDiff <= nextDiff) insertPos - 1 else insertPos
            }
        }
    }

    fun stepFrame(deltaFrames: Int) {
        val tsList = frameTimestampsUs
        if (tsList != null && tsList.isNotEmpty()) {
            val idx = findClosestFrameIndex(tsList, currentPositionUs)
            val nextIdx = (idx + deltaFrames).coerceIn(0, tsList.lastIndex)
            currentPositionUs = tsList[nextIdx].coerceIn(0L, videoDurationMs * 1000L)
        } else {
            val nextMs = ((currentPositionUs / 1000L) + deltaFrames * msPerFrame).coerceIn(0L, videoDurationMs)
            currentPositionUs = nextMs * 1000L
        }
        resetFineTuneSlider()
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Storage permission required to save frames", Toast.LENGTH_LONG).show()
        }
    }

    val pickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                retriever.setDataSource(context, uri)
            } catch (e: Exception) {
                Toast.makeText(context, "Unable to open video: ${e.message}", Toast.LENGTH_LONG).show()
                return@rememberLauncherForActivityResult
            }
            videoUri = uri
            currentPositionUs = 0L
            resetFineTuneSlider()
            isPlaying = false
            frameTimestampsUs = null

            videoDurationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L

            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            val rawW = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val rawH = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            if (rotation == 90 || rotation == 270) {
                videoWidth = rawH
                videoHeight = rawW
            } else {
                videoWidth = rawW
                videoHeight = rawH
            }
            videoResolution = if (videoWidth > 0 && videoHeight > 0) "${videoWidth}x${videoHeight}" else "Unknown"

            val frameCount = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT)?.toIntOrNull() ?: 0
            totalFrameCount = frameCount
            if (frameCount > 0 && videoDurationMs > 0) {
                videoFps = frameCount.toFloat() / (videoDurationMs.toFloat() / 1000f)
            } else {
                videoFps = 30f
            }
            msPerFrame = (1000f / videoFps).roundToLong().coerceAtLeast(1L)

            // Index exact frame timestamps in background using MediaExtractor for fast & exact frame stepping
            scope.launch(Dispatchers.IO) {
                val timestamps = extractVideoFrameTimestampsUs(context, uri)
                if (timestamps != null && timestamps.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        if (videoUri == uri) {
                            frameTimestampsUs = timestamps
                            if (totalFrameCount <= 0) {
                                totalFrameCount = timestamps.size
                            }
                        }
                    }
                }
            }
        }
    }

    val prevInteractionSource = remember { MutableInteractionSource() }
    val isPrevPressed by prevInteractionSource.collectIsPressedAsState()

    val nextInteractionSource = remember { MutableInteractionSource() }
    val isNextPressed by nextInteractionSource.collectIsPressedAsState()

    // Reactive, lock-free frame decoder (Issue #5 fix: scaled preview during playback/scrubbing + full-res settle)
    LaunchedEffect(videoUri) {
        val activeUri = videoUri ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            val workerRetriever = MediaMetadataRetriever()
            try {
                workerRetriever.setDataSource(context, activeUri)
                var lastDecodedUs = -1L
                var lastDecodedFullRes = false

                while (isActive) {
                    val targetUs = currentPositionUs
                    val fastMode = isPlaying || isScrubbing || isPrevPressed || isNextPressed
                    val wantFullRes = !fastMode

                    if (targetUs != lastDecodedUs || (wantFullRes && !lastDecodedFullRes)) {
                        val frame = decodePreviewFrame(
                            retriever = workerRetriever,
                            timeUs = targetUs,
                            videoWidth = videoWidth,
                            videoHeight = videoHeight,
                            fastPreview = fastMode,
                            totalFrames = totalFrameCount,
                            frameTimestampsUs = frameTimestampsUs
                        )
                        if (frame != null) {
                            withContext(Dispatchers.Main) {
                                currentBitmap = frame
                            }
                            lastDecodedUs = targetUs
                            lastDecodedFullRes = wantFullRes
                        }
                    }
                    delay(4)
                }
            } finally {
                try {
                    workerRetriever.release()
                } catch (_: Exception) {}
            }
        }
    }

    // Playback loop synchronized with decoding so pause is instant and frames don't queue up
    LaunchedEffect(isPlaying, playSpeedFps) {
        if (isPlaying) {
            while (isActive && isPlaying) {
                val frameStartMs = SystemClock.elapsedRealtime()
                val targetDelayMs = (1000f / playSpeedFps).toLong().coerceAtLeast(16L)

                val beforeUs = currentPositionUs
                stepFrame(1)
                if (currentPositionUs == beforeUs || (currentPositionUs / 1000L) >= videoDurationMs) {
                    isPlaying = false
                    break
                }

                val elapsed = SystemClock.elapsedRealtime() - frameStartMs
                delay(max(4L, targetDelayMs - elapsed))
            }
        }
    }

    LaunchedEffect(isPrevPressed) {
        if (isPrevPressed && !isPlaying) {
            delay(350)
            while (isActive && isPrevPressed) {
                stepFrame(-1)
                delay((1000f / playSpeedFps).toLong().coerceAtLeast(16L))
            }
        }
    }

    LaunchedEffect(isNextPressed) {
        if (isNextPressed && !isPlaying) {
            delay(350)
            while (isActive && isNextPressed) {
                stepFrame(1)
                delay((1000f / playSpeedFps).toLong().coerceAtLeast(16L))
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- TOP BAR ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Frame Extractor", color = Color.White, style = MaterialTheme.typography.titleLarge)
                    IconButton(
                        onClick = { showSettingsMenu = true },
                        modifier = Modifier.padding(start = 6.dp).size(44.dp)
                    ) {
                        Icon(
                            imageVector = SettingsIcon,
                            contentDescription = "Settings",
                            tint = Color(0xFFE040FB),
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
                if (videoUri != null) {
                    val formattedFps = String.format(Locale.US, "%.1f", videoFps)
                    Text("$videoResolution • $formattedFps FPS", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                }
            }

            if (showSettingsMenu) {
                AlertDialog(
                    onDismissRequest = { showSettingsMenu = false },
                    title = {
                        Text("Settings", style = MaterialTheme.typography.titleLarge)
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showFineSelection = !showFineSelection
                                        prefs.edit().putBoolean("show_fine_selection", showFineSelection).apply()
                                        resetFineTuneSlider()
                                    }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                    Text(
                                        text = "Fine selection slider",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = "Show ±10s precision scrubbing bar and millisecond timestamps",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = showFineSelection,
                                    onCheckedChange = { checked ->
                                        showFineSelection = checked
                                        prefs.edit().putBoolean("show_fine_selection", checked).apply()
                                        resetFineTuneSlider()
                                    }
                                )
                            }

                            HorizontalDivider()

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showChangelogDialog = true
                                    }
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Changelog",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = "View release notes & version history (v1.2)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "View",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            HorizontalDivider()

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showSettingsMenu = false
                                        val intent = android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            Uri.parse("https://github.com/tailgunnerx/FrameExtractor")
                                        )
                                        context.startActivity(intent)
                                    }
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "GitHub Repository",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = "github.com/tailgunnerx/FrameExtractor",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Icon(
                                    imageVector = OpenInNewIcon,
                                    contentDescription = "Open GitHub Repository",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showSettingsMenu = false }) {
                            Text("Close")
                        }
                    }
                )
            }

            if (showChangelogDialog) {
                AlertDialog(
                    onDismissRequest = { showChangelogDialog = false },
                    title = {
                        Text("Changelog", style = MaterialTheme.typography.titleLarge)
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "v1.2",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "2026-11-23",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = "• Added Settings popup with optional ±10s Fine Selection slider & millisecond timestamps (PR #6)\n" +
                                        "• High-speed video scrubbing & playback engine (dynamic scaled previewing + instant full-res settle)\n" +
                                        "• Background MediaExtractor frame timestamp indexing for exact +1/-1 frame stepping & instant pause\n" +
                                        "• Full edge-to-edge display ignoring status bar / notch gap\n" +
                                        "• Material You dynamic theme support & Android 13+ monochrome themed app icon",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            HorizontalDivider()

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "v1.1",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "2026-05-07",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "• BlueStacks & legacy Android storage permission compatibility fix",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            HorizontalDivider()

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "v1.0",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "2026-05-05",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "• Initial release",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showChangelogDialog = false }) {
                            Text("Back")
                        }
                    }
                )
            }

            // Clean Icon Eject Button
            if (videoUri != null) {
                IconButton(
                    onClick = {
                        videoUri = null
                        currentBitmap = null
                        isPlaying = false
                        currentPositionUs = 0L
                        resetFineTuneSlider()
                        videoDurationMs = 0L
                        frameTimestampsUs = null
                    },
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(CloseIcon, contentDescription = "Eject Video", tint = MaterialTheme.colorScheme.error)
                }
            }

            Button(onClick = { pickerLauncher.launch("video/*") }) {
                Text(if (videoUri == null) "Open Video" else "Change")
            }
        }

        // --- IMAGE VIEWER ---
        var scale by remember(videoUri) { mutableFloatStateOf(1f) }
        var offsetX by remember(videoUri) { mutableFloatStateOf(0f) }
        var offsetY by remember(videoUri) { mutableFloatStateOf(0f) }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 5f)
                        val panLimit = (scale - 1f) * 800f
                        offsetX = (offsetX + pan.x).coerceIn(-panLimit, panLimit)
                        offsetY = (offsetY + pan.y).coerceIn(-panLimit, panLimit)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (currentBitmap != null) {
                Image(
                    bitmap = currentBitmap!!.asImageBitmap(),
                    contentDescription = "Current Frame",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offsetX
                            translationY = offsetY
                        }
                )
            } else {
                Text(if (videoUri == null) "No video selected" else "Loading frame...", color = Color.Gray)
            }
        }

        // --- TRADITIONAL MEDIA CONTROL BOARD ---
        if (videoUri != null) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Frame & Speed Info Array
                    val tsList = frameTimestampsUs
                    val currentFrame = if (tsList != null && tsList.isNotEmpty()) {
                        (findClosestFrameIndex(tsList, currentPositionUs) + 1).coerceIn(1, tsList.size)
                    } else {
                        (currentPositionMs / msPerFrame).toInt() + 1
                    }
                    val totalFrames = if (tsList != null && tsList.isNotEmpty()) {
                        tsList.size
                    } else if (totalFrameCount > 0) {
                        totalFrameCount
                    } else {
                        (videoDurationMs / msPerFrame).toInt() + 1
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Frame $currentFrame / $totalFrames", color = Color.White, style = MaterialTheme.typography.labelLarge)
                        Text("${playSpeedFps.roundToInt()} FPS Speed", color = Color.Gray, style = MaterialTheme.typography.labelLarge)
                    }
                    if (showFineSelection) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val timeFormatter = { hours: Long, minutes: Int, seconds: Int, nanoseconds: Int ->
                                String.format(Locale.US, "%02d:%02d:%02d.%03d", hours, minutes, seconds, nanoseconds / 1000000)
                            }
                            val pos = currentPositionMs.toDuration(DurationUnit.MILLISECONDS).toComponents(timeFormatter)
                            val total = videoDurationMs.toDuration(DurationUnit.MILLISECONDS).toComponents(timeFormatter)
                            Text("$pos / $total", color = Color.White, style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    // Main Timeline Slider
                    Slider(
                        value = currentPositionMs.toFloat().coerceIn(0f, if (videoDurationMs > 0) videoDurationMs.toFloat() else 100f),
                        onValueChange = {
                            isScrubbing = true
                            isPlaying = false
                            val nextMs = if (showFineSelection) {
                                ((it / msPerFrame).roundToLong() * msPerFrame).coerceIn(0L, videoDurationMs)
                            } else {
                                it.toLong().coerceIn(0L, videoDurationMs)
                            }
                            val ts = frameTimestampsUs
                            currentPositionUs = if (showFineSelection && ts != null && ts.isNotEmpty()) {
                                ts[findClosestFrameIndex(ts, nextMs * 1000L)]
                            } else {
                                nextMs * 1000L
                            }
                            resetFineTuneSlider()
                        },
                        onValueChangeFinished = {
                            isScrubbing = false
                        },
                        valueRange = 0f..(if (videoDurationMs > 0) videoDurationMs.toFloat() else 100f),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (showFineSelection) {
                        Text(
                            "Fine selection:",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.align(alignment = Alignment.Start).padding(horizontal = 8.dp)
                        )

                        // PR #6 Fine Selection Slider
                        Slider(
                            value = fineTunePositionOffset.toFloat(),
                            onValueChange = {
                                isScrubbing = true
                                isPlaying = false
                                fineTunePositionOffset = (it / msPerFrame).roundToLong() * msPerFrame
                                val nextMs = (fineTunePositionCenter + fineTunePositionOffset).coerceIn(0L, videoDurationMs)
                                val ts = frameTimestampsUs
                                currentPositionUs = if (ts != null && ts.isNotEmpty()) {
                                    ts[findClosestFrameIndex(ts, nextMs * 1000L)]
                                } else {
                                    nextMs * 1000L
                                }
                            },
                            onValueChangeFinished = {
                                isScrubbing = false
                                resetFineTuneSlider()
                            },
                            valueRange = -10000f..10000f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Player Control Wheel
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Extract (Save) Button - always extracts full-resolution original frame
                        FloatingActionButton(
                            onClick = {
                                if (isExtracting || currentBitmap == null) return@FloatingActionButton

                                // Check for legacy permission on older devices (API 28 and below)
                                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                                    val permission = android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                                    if (context.checkSelfPermission(permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                        permissionLauncher.launch(permission)
                                        return@FloatingActionButton
                                    }
                                }

                                isExtracting = true
                                isPlaying = false
                                val targetPositionUs = currentPositionUs
                                val targetPositionMs = currentPositionMs
                                scope.launch {
                                    try {
                                        withContext(Dispatchers.IO) {
                                            // Decode full-resolution frame for saving
                                            val fullResBitmap = decodePreviewFrame(
                                                retriever = retriever,
                                                timeUs = targetPositionUs,
                                                videoWidth = videoWidth,
                                                videoHeight = videoHeight,
                                                fastPreview = false,
                                                totalFrames = totalFrameCount,
                                                frameTimestampsUs = frameTimestampsUs
                                            ) ?: currentBitmap!!
                                            saveBitmapToPictures(context, fullResBitmap, targetPositionMs)
                                        }
                                        Toast.makeText(context, "Saved frame to Pictures!", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        Toast.makeText(context, "Error saving frame: ${e.message}", Toast.LENGTH_LONG).show()
                                    } finally {
                                        isExtracting = false
                                    }
                                }
                            },
                            containerColor = Color.White,
                            contentColor = Color.Black,
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape
                        ) {
                            if (isExtracting) {
                                CircularProgressIndicator(
                                    color = Color.Black,
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 3.dp
                                )
                            } else {
                                Icon(
                                    SaveIcon,
                                    contentDescription = "Extract Frame",
                                    tint = Color.Black,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        // Slow Down
                        IconButton(onClick = { playSpeedFps = (playSpeedFps - 1f).coerceAtLeast(1f) }) {
                            Icon(RemoveIcon, contentDescription = "Slower", tint = Color.Gray)
                        }

                        // Prev Frame
                        IconButton(
                            onClick = {
                                isPlaying = false
                                stepFrame(-1)
                            },
                            interactionSource = prevInteractionSource,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(SkipPreviousIcon, contentDescription = "Prev Frame", tint = Color.White, modifier = Modifier.size(36.dp))
                        }

                        // Play/Pause
                        FloatingActionButton(
                            onClick = { isPlaying = !isPlaying },
                            containerColor = if (isPlaying) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                            contentColor = if (isPlaying) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(64.dp),
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = if (isPlaying) PauseIcon else PlayIcon,
                                contentDescription = "Play/Pause",
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // Next Frame
                        IconButton(
                            onClick = {
                                isPlaying = false
                                stepFrame(1)
                            },
                            interactionSource = nextInteractionSource,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(SkipNextIcon, contentDescription = "Next Frame", tint = Color.White, modifier = Modifier.size(36.dp))
                        }

                        // Speed Up
                        IconButton(onClick = { playSpeedFps = (playSpeedFps + 1f).coerceAtMost(60f) }) {
                            Icon(AddIcon, contentDescription = "Faster", tint = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Decodes a frame optimized for smooth previewing on high-resolution videos (like 4K@60fps in Issue #5).
 * Uses getFrameAtIndex on API 28+ when exact frame indices are known, or scaled preview decoding during active playback/scrubbing.
 */
private fun decodePreviewFrame(
    retriever: MediaMetadataRetriever,
    timeUs: Long,
    videoWidth: Int,
    videoHeight: Int,
    fastPreview: Boolean,
    totalFrames: Int,
    frameTimestampsUs: LongArray?
): Bitmap? {
    try {
        // When paused on a High-Res video, or when video is <= 1080p, decode full quality
        val maxDim = max(videoWidth, videoHeight)
        if (fastPreview && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 && maxDim > 1280) {
            val scale = 1280f / maxDim.toFloat()
            val dstW = (videoWidth * scale).roundToInt().coerceAtLeast(1)
            val dstH = (videoHeight * scale).roundToInt().coerceAtLeast(1)
            return retriever.getScaledFrameAtTime(
                timeUs,
                MediaMetadataRetriever.OPTION_CLOSEST,
                dstW,
                dstH
            )
        }

        // Use exact hardware frame index lookup on API 28+ when paused if timestamp table is ready
        if (!fastPreview && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && frameTimestampsUs != null && frameTimestampsUs.isNotEmpty()) {
            val searchIdx = frameTimestampsUs.binarySearch(timeUs)
            val idx = if (searchIdx >= 0) {
                searchIdx
            } else {
                val insertPos = (-searchIdx - 1).coerceIn(0, frameTimestampsUs.size)
                when {
                    insertPos == 0 -> 0
                    insertPos >= frameTimestampsUs.size -> frameTimestampsUs.lastIndex
                    else -> {
                        val prevDiff = timeUs - frameTimestampsUs[insertPos - 1]
                        val nextDiff = frameTimestampsUs[insertPos] - timeUs
                        if (prevDiff <= nextDiff) insertPos - 1 else insertPos
                    }
                }
            }
            if (totalFrames <= 0 || idx < totalFrames) {
                val indexedBitmap = try {
                    retriever.getFrameAtIndex(idx)
                } catch (_: Exception) {
                    null
                }
                if (indexedBitmap != null) return indexedBitmap
            }
        }

        return retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
    } catch (_: Exception) {
        return null
    }
}

/**
 * Extracts all video sample presentation timestamps (in microseconds) in the background using MediaExtractor.
 * Very fast (~15-30ms for a 60s 60fps video) because it only reads the MP4 sample table without decoding video frames.
 */
private fun extractVideoFrameTimestampsUs(context: android.content.Context, uri: Uri): LongArray? {
    val extractor = MediaExtractor()
    try {
        extractor.setDataSource(context, uri, null)
        var videoTrackIndex = -1
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
            if (mime.startsWith("video/")) {
                videoTrackIndex = i
                break
            }
        }
        if (videoTrackIndex == -1) return null
        extractor.selectTrack(videoTrackIndex)

        val timestamps = ArrayList<Long>(4096)
        val maxFramesToIndex = 100_000 // Safety cap (~27 mins @ 60fps)
        while (timestamps.size < maxFramesToIndex) {
            val sampleTime = extractor.sampleTime
            if (sampleTime < 0) break
            timestamps.add(sampleTime)
            if (!extractor.advance()) break
        }
        val array = timestamps.toLongArray()
        array.sort() // Ensure PTS order (since B-frames can be stored in DTS order in container)
        return array
    } catch (_: Exception) {
        return null
    } finally {
        try {
            extractor.release()
        } catch (_: Exception) {}
    }
}

private fun saveBitmapToPictures(context: android.content.Context, bitmap: Bitmap, timestampMs: Long) {
    val fileName = "ExtractedFrame_${timestampMs}ms.png"
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/FrameExtractor")
        }
    }

    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        ?: throw Exception("Failed to create MediaStore entry")

    try {
        resolver.openOutputStream(uri)?.use { out ->
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                throw Exception("Failed to compress bitmap")
            }
        } ?: throw Exception("Failed to open output stream")

        // Force the gallery to see the file immediately
        android.media.MediaScannerConnection.scanFile(
            context,
            arrayOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath + "/FrameExtractor/" + fileName),
            arrayOf("image/png"),
            null
        )
    } catch (e: Exception) {
        // Clean up the empty MediaStore entry if writing failed
        resolver.delete(uri, null, null)
        throw e
    }
}
