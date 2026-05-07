package com.tailgunnerx.frameextractor

import android.content.ContentValues
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
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
    var currentPositionMs by remember { mutableStateOf(0L) }
    var videoDurationMs by remember { mutableStateOf(0L) }
    var isExtracting by remember { mutableStateOf(false) }

    var videoResolution by remember { mutableStateOf("") }
    var videoFps by remember { mutableFloatStateOf(30f) }
    var msPerFrame by remember { mutableLongStateOf(33L) }

    var isPlaying by remember { mutableStateOf(false) }
    var playSpeedFps by remember { mutableFloatStateOf(5f) }

    val scope = rememberCoroutineScope()
    val retriever = remember { MediaMetadataRetriever() }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Storage permission required to save frames", Toast.LENGTH_LONG).show()
        }
    }

    val pickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            videoUri = uri
            currentPositionMs = 0L
            isPlaying = false
            retriever.setDataSource(context, uri)
            videoDurationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            
            val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH) ?: ""
            val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT) ?: ""
            videoResolution = if (w.isNotEmpty() && h.isNotEmpty()) "${w}x${h}" else "Unknown"

            val frameCount = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT)?.toLongOrNull()
            if (frameCount != null && videoDurationMs > 0) {
                videoFps = frameCount.toFloat() / (videoDurationMs.toFloat() / 1000f)
            } else {
                videoFps = 30f 
            }
            msPerFrame = (1000f / videoFps).toLong().coerceAtLeast(1L)
        }
    }

    LaunchedEffect(videoUri) {
        if (videoUri == null) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            var currentlyDecodedMs = -1L
            while (true) {
                val targetMs = currentPositionMs
                if (targetMs != currentlyDecodedMs) {
                    val frame = retriever.getFrameAtTime(targetMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST)
                    withContext(Dispatchers.Main) {
                        currentBitmap = frame
                    }
                    currentlyDecodedMs = targetMs
                }
                delay(16)
            }
        }
    }

    LaunchedEffect(isPlaying, playSpeedFps) {
        if (isPlaying) {
            while (currentPositionMs < videoDurationMs) {
                val delayMs = (1000f / playSpeedFps).toLong()
                delay(delayMs)
                currentPositionMs = (currentPositionMs + msPerFrame).coerceAtMost(videoDurationMs)
                if (currentPositionMs >= videoDurationMs) {
                    isPlaying = false
                }
            }
        }
    }

    val prevInteractionSource = remember { MutableInteractionSource() }
    val isPrevPressed by prevInteractionSource.collectIsPressedAsState()
    
    val nextInteractionSource = remember { MutableInteractionSource() }
    val isNextPressed by nextInteractionSource.collectIsPressedAsState()

    LaunchedEffect(isPrevPressed) {
        if (isPrevPressed && !isPlaying) {
            delay(400)
            while (true) {
                currentPositionMs = (currentPositionMs - msPerFrame).coerceAtLeast(0)
                delay((1000f / playSpeedFps).toLong())
            }
        }
    }

    LaunchedEffect(isNextPressed) {
        if (isNextPressed && !isPlaying) {
            delay(400)
            while (true) {
                currentPositionMs = (currentPositionMs + msPerFrame).coerceAtMost(videoDurationMs)
                delay((1000f / playSpeedFps).toLong())
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- TOP BAR ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Frame Extractor", color = Color.White, style = MaterialTheme.typography.titleLarge)
                if (videoUri != null) {
                    val formattedFps = "%.1f".format(videoFps)
                    Text("$videoResolution • $formattedFps FPS", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                }
            }
            
            // Clean Icon Eject Button
            if (videoUri != null) {
                IconButton(
                    onClick = {
                        videoUri = null
                        currentBitmap = null
                        isPlaying = false
                        currentPositionMs = 0L
                        videoDurationMs = 0L
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
        var scale by remember { mutableFloatStateOf(1f) }
        var offsetX by remember { mutableFloatStateOf(0f) }
        var offsetY by remember { mutableFloatStateOf(0f) }

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
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val currentFrame = (currentPositionMs / msPerFrame).toInt() + 1
                        val totalFrames = (videoDurationMs / msPerFrame).toInt() + 1
                        Text("Frame $currentFrame / $totalFrames", color = Color.White, style = MaterialTheme.typography.labelLarge)
                        Text("${playSpeedFps.roundToInt()} FPS Speed", color = Color.Gray, style = MaterialTheme.typography.labelLarge)
                    }

                    // Main Timeline Slider
                    Slider(
                        value = currentPositionMs.toFloat(),
                        onValueChange = { 
                            currentPositionMs = it.toLong() 
                            isPlaying = false
                        },
                        valueRange = 0f..(if (videoDurationMs > 0) videoDurationMs.toFloat() else 100f),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Player Control Wheel
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Extract (Save) Button
                        FloatingActionButton(
                            onClick = {
                                if (isExtracting || currentBitmap == null) return@FloatingActionButton
                                
                                // Check for legacy permission on older devices (API 28 and below)
                                if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.P) {
                                    val permission = android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                                    if (context.checkSelfPermission(permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                        permissionLauncher.launch(permission)
                                        return@FloatingActionButton
                                    }
                                }

                                isExtracting = true
                                isPlaying = false
                                scope.launch {
                                    try {
                                        withContext(Dispatchers.IO) {
                                            saveBitmapToPictures(context, currentBitmap!!, currentPositionMs)
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
                                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                            } else {
                                Icon(SaveIcon, contentDescription = "Extract Frame", tint = Color.Black, modifier = Modifier.size(24.dp))
                            }
                        }

                        // Slow Down
                        IconButton(onClick = { playSpeedFps = (playSpeedFps - 1f).coerceAtLeast(1f) }) {
                            Icon(RemoveIcon, contentDescription = "Slower", tint = Color.Gray)
                        }

                        // Prev Frame
                        IconButton(
                            onClick = { 
                                currentPositionMs = (currentPositionMs - msPerFrame).coerceAtLeast(0) 
                                isPlaying = false
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
                                currentPositionMs = (currentPositionMs + msPerFrame).coerceAtMost(videoDurationMs) 
                                isPlaying = false
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

private fun saveBitmapToPictures(context: android.content.Context, bitmap: Bitmap, timestampMs: Long) {
    val fileName = "ExtractedFrame_${timestampMs}ms.png"
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
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
            arrayOf(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES).absolutePath + "/FrameExtractor/" + fileName),
            arrayOf("image/png"),
            null
        )
    } catch (e: Exception) {
        // Clean up the empty MediaStore entry if writing failed
        resolver.delete(uri, null, null)
        throw e
    }
}
