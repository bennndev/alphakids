package com.example.alphakids.ui.screens.tutor.games

import ScannerOverlay
import android.Manifest
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.MeteringPoint
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.alphakids.ui.components.NotificationCard
import com.example.alphakids.ui.components.TimerBar
import com.example.alphakids.ui.screens.tutor.games.components.CameraActionBar
import com.example.alphakids.ui.theme.dmSansFamily
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import java.util.concurrent.Executors
import com.example.alphakids.ui.screens.tutor.games.WordStorage
import com.example.alphakids.ui.utils.MusicManager
import com.example.alphakids.ui.utils.AUDIO_TIMEOUT_URL // ✅ Import correcto aquí
import java.lang.Exception

// 🔊 URLs de audio desde Firebase Storage
const val AUDIO_EXITO_URL =
    "https://firebasestorage.googleapis.com/v0/b/alphakids-tecsup.firebasestorage.app/o/audio_exito.mp3?alt=media&token=8fd13d76-d100-4bff-9490-35a02138599d"

const val AUDIO_FALLO_URL =
    "https://firebasestorage.googleapis.com/v0/b/alphakids-tecsup.firebasestorage.app/o/audio_fallo.mp3?alt=media&token=EL_TOKEN_DE_FALLO"

// 🔊 MediaPlayer para efectos
var sfxPlayer: MediaPlayer? = null

// ----------------------------------------------------------------------
// 🔊 Implementación CORRECTA de MediaPlayer para SFX
// ----------------------------------------------------------------------
fun playSfxAudioFromUrl(context: Context, url: String, onCompletion: () -> Unit = {}) {
    try {
        // 🔇 Bajar volumen mientras suena el efecto
        MusicManager.setAppVolume(0.05f)
        MusicManager.setJuegoVolume(0.05f)

        // Liberar cualquier reproductor anterior antes de crear uno nuevo
        sfxPlayer?.release()
        sfxPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .build()
            )
            setDataSource(url)

            setOnPreparedListener { it.start() }

            setOnCompletionListener { mp ->
                mp.release()
                sfxPlayer = null
                // 🔊 Restaurar volumen normal
                MusicManager.setAppVolume(1f)
                MusicManager.setJuegoVolume(1f)
                onCompletion()
            }

            setOnErrorListener { mp, _, _ ->
                mp.release()
                sfxPlayer = null
                MusicManager.setAppVolume(1f)
                MusicManager.setJuegoVolume(1f)
                true
            }
            prepareAsync()
        }
    } catch (e: Exception) {
        Log.e("AudioPlayer", "Error al reproducir SFX: ${e.message}")
        MusicManager.setAppVolume(1f)
        MusicManager.setJuegoVolume(1f)
    }
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CameraOCRScreen(
    assignmentId: String,
    targetWord: String,
    studentId: String,
    targetImageUrl: String?,
    onBackClick: () -> Unit,
    onWordCompleted: (word: String, imageUrl: String?, studentId: String) -> Unit,
    onTimeExpired: (imageUrl: String?, studentId: String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var detectedText by remember { mutableStateOf("") }
    var isWordCompleted by remember { mutableStateOf(false) }
    var isNavigating by remember { mutableStateOf(false) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var torchOn by remember { mutableStateOf(false) }
    var lensFacingBack by remember { mutableStateOf(true) }
    val previewViewRef = remember { mutableStateOf<PreviewView?>(null) }
    var roiRect by remember { mutableStateOf<FloatArray?>(null) }
    var showNotification by remember { mutableStateOf(true) }

    var isCountdownRunning by remember { mutableStateOf(true) }
    var isCountdownFinished by remember { mutableStateOf(false) }
    var countdownIndex by remember { mutableStateOf(0) }
    val countdownSequence = listOf("3", "2", "1", "¡Empieza!")

    val totalMillis = 60_000L
    var remainingMillis by remember { mutableStateOf(totalMillis) }
    var progress by remember { mutableStateOf(0f) }
    var isWarning by remember { mutableStateOf(false) }
    var hasWarningSoundPlayed by remember { mutableStateOf(false) }

    val cameraController = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        }
    }
    val executor = remember { Executors.newSingleThreadExecutor() }

    suspend fun safeReleaseCamera(delayMs: Long = 300L) {
        try { cameraController.unbind() } catch (_: Exception) { }
        try { executor.shutdownNow() } catch (_: Exception) { }
        delay(delayMs)
    }

    LaunchedEffect(Unit) {
        MusicManager.pauseMusicaApp()
        MusicManager.startMusicaJuego(context)

        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("es", "ES")
                tts?.setSpeechRate(0.9f)
            }
        }
    }

    LaunchedEffect(Unit) {
        countdownSequence.forEachIndexed { i, _ ->
            countdownIndex = i
            delay(800)
        }
        isCountdownRunning = false
        isCountdownFinished = true
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                cameraController.unbind()
                executor.shutdownNow()
                tts?.stop()
                tts?.shutdown()
            } catch (_: Exception) {}

            sfxPlayer?.release()
            sfxPlayer = null
            MusicManager.stopMusicaJuego()
            MusicManager.resumeMusicaApp()
        }
    }

    LaunchedEffect(lifecycleOwner) {
        cameraController.bindToLifecycle(lifecycleOwner)
    }

    LaunchedEffect(cameraController, targetWord, isCountdownFinished) {
        if (isCountdownFinished) {
            val textAnalyzer = TextAnalyzer(
                targetWord = targetWord,
                onTextDetected = { text ->
                    scope.launch(Dispatchers.Main) {
                        detectedText = text
                    }
                }
            )
            cameraController.setImageAnalysisAnalyzer(executor, textAnalyzer)
        }
    }

    // 🏆 Lógica de éxito
    LaunchedEffect(detectedText, targetWord) {
        val cleanDetectedText = detectedText.trim().uppercase()
        val cleanTarget = targetWord.trim().uppercase()

        if (!isWordCompleted &&
            !isNavigating &&
            cleanDetectedText.contains(cleanTarget)
        ) {
            isWordCompleted = true
            isNavigating = true

            WordStorage.saveCompletedWord(context, targetWord, assignmentId)

            scope.launch {
                withContext(Dispatchers.IO) { safeReleaseCamera() }
                withContext(Dispatchers.Main) {
                    MusicManager.stopMusicaJuego()
                    onWordCompleted(targetWord, targetImageUrl, studentId)
                }
            }
        }
    }

    // ⏰ Lógica de perder (usa AUDIO_TIMEOUT_URL)
    LaunchedEffect(isCountdownFinished, isWordCompleted) {
        if (!isCountdownFinished) return@LaunchedEffect
        while (remainingMillis > 0 && !isWordCompleted) {
            delay(1000)
            if (!isWordCompleted) {
                remainingMillis -= 1000
                progress = 1f - (remainingMillis.toFloat() / totalMillis)
                isWarning = remainingMillis <= 10_000L

                if (isWarning && !hasWarningSoundPlayed) {
                    playSfxAudioFromUrl(context, AUDIO_TIMEOUT_URL)
                    hasWarningSoundPlayed = true
                }
            }
        }

        if (!isWordCompleted && remainingMillis <= 0) {
            isWordCompleted = true
            isNavigating = true

            scope.launch {
                withContext(Dispatchers.IO) { safeReleaseCamera() }
                withContext(Dispatchers.Main) {
                    MusicManager.stopMusicaJuego()
                    MusicManager.resumeMusicaApp()
                    onTimeExpired(targetImageUrl, studentId)
                }
            }
        }
    }

    fun formatTime(ms: Long): String {
        val s = (ms / 1000).toInt()
        return String.format("%d:%02d", s / 60, s % 60)
    }

    // --- UI ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (cameraPermissionState.status == PermissionStatus.Granted) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        controller = cameraController
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        previewViewRef.value = this
                    }
                }
            )
        } else {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Se necesita permiso de cámara", color = Color.White, fontFamily = dmSansFamily)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                    Text("Conceder Permiso")
                }
            }
        }

        ScannerOverlay(
            modifier = Modifier.fillMaxSize(),
            boxWidthPercent = 0.8f,
            boxAspectRatio = 1.6f,
            onBoxRectChange = { l, t, r, b ->
                roiRect = floatArrayOf(l, t, r, b)
            }
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = Color.White)
                }
                Spacer(Modifier.width(16.dp))
                TimerBar(
                    modifier = Modifier.weight(1f),
                    progress = progress,
                    timeText = formatTime(remainingMillis),
                    isWarning = isWarning
                )
            }

            if (showNotification) {
                NotificationCard(
                    modifier = Modifier.padding(top = 12.dp),
                    title = "Busca la palabra:",
                    content = targetWord,
                    imageUrl = targetImageUrl,
                    icon = Icons.Rounded.Checkroom,
                    onCloseClick = { showNotification = false }
                )
            }
        }

        if (isCountdownRunning) {
            AnimatedContent(
                modifier = Modifier.align(Alignment.Center),
                targetState = countdownIndex,
                transitionSpec = {
                    fadeIn() + scaleIn() togetherWith fadeOut() + scaleOut()
                }
            ) { idx ->
                Text(
                    text = countdownSequence[idx],
                    fontFamily = dmSansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 48.sp,
                    color = Color.White
                )
            }
        }

        CameraActionBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            onFlashClick = {
                torchOn = !torchOn
                cameraController.enableTorch(torchOn)
            },
            onShutterClick = null,
            onFlipCameraClick = {
                lensFacingBack = !lensFacingBack
                cameraController.cameraSelector =
                    if (lensFacingBack) CameraSelector.DEFAULT_BACK_CAMERA
                    else CameraSelector.DEFAULT_FRONT_CAMERA
            }
        )

        if (detectedText.isNotEmpty() && !isWordCompleted) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 120.dp)
                    .padding(horizontal = 24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.7f))
            ) {
                Text(
                    "Detectado: $detectedText",
                    color = Color.White,
                    fontFamily = dmSansFamily,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}
