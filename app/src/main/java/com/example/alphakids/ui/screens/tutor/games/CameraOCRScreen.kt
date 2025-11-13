package com.example.alphakids.ui.screens.tutor.games

import android.Manifest
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.alphakids.ui.theme.dmSansFamily
import com.example.alphakids.ui.utils.MusicManager // Clase centralizada para control de audio
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay
import java.util.*
import kotlin.Exception

// 🔊 URLs de audio desde Firebase Storage
private const val AUDIO_EXITO_URL =
    "https://firebasestorage.googleapis.com/v0/b/alphakids-tecsup.firebasestorage.app/o/audio_exito.mp3?alt=media&token=d484c88c-253e-4f41-a638-04da263d476a"
private const val AUDIO_FALLO_URL =
    "https://firebasestorage.googleapis.com/v0/b/alphakids-tecsup.firebasestorage.app/o/audio_fallo.mp3?alt=media&token=bd92cf80-ac33-494f-aac3-bb252369cfb9"

// 🔊 Control de efectos
private var sfxPlayer: android.media.MediaPlayer? = null

fun playSfxAudioFromUrl(url: String) {
    try {
        sfxPlayer?.release()
        sfxPlayer = android.media.MediaPlayer().apply {
            setDataSource(url)
            setOnPreparedListener { it.start() }
            setOnCompletionListener {
                it.release()
                sfxPlayer = null
            }
            prepareAsync()
        }
    } catch (e: Exception) {
        Log.e("AudioPlayer", "Error al reproducir SFX: ${e.message}")
    }
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CameraOCRScreen(
    assignmentId: String,
    targetWord: String,
    onBackClick: () -> Unit,
    onWordCompleted: () -> Unit,
    viewModel: CameraOCRViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var detectedText by remember { mutableStateOf("") }
    var showSuccessAnimation by remember { mutableStateOf(false) }
    var isWordCompleted by remember { mutableStateOf(false) }
    var isFailurePlayed by remember { mutableStateOf(false) }

    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    // 🎶 Al entrar al juego: pausa la música global y arranca la del juego
    LaunchedEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("es", "ES")
                tts?.setSpeechRate(0.9f)
            }
        }
        // Pausa la música de la APP y empieza la del juego.
        MusicManager.pauseMusicaApp()
        MusicManager.startMusicaJuego(context)
    }

    // 🚪 Al salir del juego: limpia y reanuda música global
    DisposableEffect(Unit) {
        onDispose {
            tts?.shutdown()
            sfxPlayer?.release()

            // Detiene la música del juego (por si el usuario presiona atrás)
            MusicManager.stopMusicaJuego()

            // Reanuda la música de la APP al salir de esta pantalla.
            MusicManager.resumeMusicaApp()
        }
    }

    // 🔤 Detección y control de audios
    LaunchedEffect(detectedText, targetWord) {
        val detected = detectedText.trim().uppercase()
        val target = targetWord.trim().uppercase()

        // 1. Lógica de Éxito
        if (!isWordCompleted && detected == target) {
            isWordCompleted = true
            showSuccessAnimation = true
            isFailurePlayed = false

            // 🛑 1. DETENER la música del juego INMEDIATAMENTE
            MusicManager.stopMusicaJuego()

            // 🔊 2. Reproducir audio de éxito
            playSfxAudioFromUrl(AUDIO_EXITO_URL)

            // ⏳ 3. Esperar a que termine la animación/SFX (3 segundos)
            delay(3000)

            // ✅ 4. REANUDAR la música de la App solo después del delay, ¡evitando la mezcla!
            MusicManager.resumeMusicaApp()

            // 🚪 5. Salir de la pantalla
            onWordCompleted()

        } else if (!isWordCompleted && detected.length >= 3 && detected != target) {
            // Lógica de Fallo (se mantiene igual)
            if (!isFailurePlayed) {
                isFailurePlayed = true
                playSfxAudioFromUrl(AUDIO_FALLO_URL)
                delay(2000)
                isFailurePlayed = false
            }
        }

        if (detected.isEmpty()) isFailurePlayed = false
    }

    if (cameraPermissionState.status.isGranted) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        previewView = this
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // 📸 Overlay ROI (se mantiene igual)
            val density = LocalDensity.current
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val rw = w * 0.8f
                val rh = h * 0.3f
                val left = (w - rw) / 2
                val top = (h - rh) / 2

                drawRect(Color.Black.copy(alpha = 0.5f), Offset.Zero, size)
                drawRect(Color.Transparent, Offset(left, top), Size(rw, rh), blendMode = androidx.compose.ui.graphics.BlendMode.Clear)
                drawRect(Color.Green, Offset(left, top), Size(rw, rh), style = Stroke(width = with(density) { 4.dp.toPx() }))
            }

            // 🔙 Top Bar (se mantiene igual)
            TopAppBar(
                title = {
                    Text(
                        text = "Busca: $targetWord",
                        fontFamily = dmSansFamily,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.7f)
                )
            )

            // 📝 Texto detectado (se mantiene igual)
            if (detectedText.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Black.copy(alpha = 0.8f)
                    )
                ) {
                    Text(
                        text = "Texto detectado: $detectedText",
                        color = Color.White,
                        fontFamily = dmSansFamily,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // 🎉 Animación de éxito (se mantiene igual)
            AnimatedVisibility(
                visible = showSuccessAnimation,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Card(
                    modifier = Modifier
                        .padding(32.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Green.copy(alpha = 0.9f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🎉", fontSize = 48.sp)
                        Text("¡Palabra Completada!", color = Color.White, fontFamily = dmSansFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(targetWord, color = Color.White, fontFamily = dmSansFamily, fontSize = 24.sp)
                    }
                }
            }
        }

        // 📸 Configurar cámara (se mantiene igual)
        LaunchedEffect(previewView) {
            previewView?.let { preview ->
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProvider = cameraProviderFuture.get()
                setupCamera(cameraProvider!!, preview, lifecycleOwner, targetWord) { text ->
                    detectedText = text
                }
            }
        }
    } else {
        // 🚫 Solicitud de permiso (se mantiene igual)
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Se necesita permiso de cámara para usar este juego")
            Spacer(Modifier.height(8.dp))
            Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                Text("Conceder permiso")
            }
        }
    }
}

// ---------------------------------------------------------------------------------

private fun setupCamera(
    cameraProvider: ProcessCameraProvider,
    previewView: PreviewView,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    targetWord: String,
    onTextDetected: (String) -> Unit
) {
    val preview = Preview.Builder().build().also {
        it.setSurfaceProvider(previewView.surfaceProvider)
    }

    val imageAnalyzer = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()
        .also {
            it.setAnalyzer(
                ContextCompat.getMainExecutor(previewView.context),
                TextAnalyzer(targetWord, onTextDetected)
            )
        }

    val selector = CameraSelector.DEFAULT_BACK_CAMERA

    try {
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, imageAnalyzer)
    } catch (e: Exception) {
        Log.e("CameraOCR", "Error al iniciar cámara", e)
    }
}