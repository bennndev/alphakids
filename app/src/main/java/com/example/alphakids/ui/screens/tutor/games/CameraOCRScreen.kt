package com.example.alphakids.ui.screens.tutor.games

import android.Manifest
import android.content.Context
import android.media.MediaPlayer
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
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import kotlin.Exception

// 🔊 URLs de audio desde Firebase Storage (¡REEMPLAZA EL TOKEN DE FALLO!)
private const val AUDIO_EXITO_URL = "https://firebasestorage.googleapis.com/v0/b/alphakids-tecsup.firebasestorage.app/o/audio_exito.mp3?alt=media&token=d484c88c-255e-4f41-a638-04da263d476a"
private const val AUDIO_FALLO_URL = "https://firebasestorage.googleapis.com/v0/b/alphakids-tecsup.firebasestorage.app/o/audio_fallo.mp3?alt=media&token=EL_TOKEN_DE_FALLO" // ⚠️ ¡REEMPLAZAR ESTE TOKEN!

// 🔊 Variable Singleton para controlar el audio
private var audioPlayer: MediaPlayer? = null

/**
 * Función para reproducir el audio desde una URL, controlando el ciclo de vida del MediaPlayer.
 */
fun playAudioFromUrl(url: String) {
    // 1. Detener y liberar el reproductor anterior si existe
    audioPlayer?.release()
    audioPlayer = null

    // 2. Crear nueva instancia
    audioPlayer = MediaPlayer().apply {
        try {
            setDataSource(url)

            setOnPreparedListener {
                it.start()
            }

            setOnCompletionListener {
                it.release() // Libera los recursos al terminar
                audioPlayer = null // Restablece la variable
            }

            prepareAsync() // Esencial para URLs
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Error al configurar o reproducir audio: ${e.message}")
            audioPlayer = null
        }
    }
}

// ---------------------------------------------------------------------------------

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
    val coroutineScope = rememberCoroutineScope() // Para las coroutines de fallo/éxito

    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var detectedText by remember { mutableStateOf("") }
    var showSuccessAnimation by remember { mutableStateOf(false) }
    var isWordCompleted by remember { mutableStateOf(false) }

    // Bandera para limitar la reproducción del audio de fallo (EVITA REPETICIÓN CONTINUA)
    var isFailurePlayed by remember { mutableStateOf(false) }

    // TTS Setup (se mantiene por si necesitas la voz en el futuro)
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    LaunchedEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("es", "ES")
                tts?.setSpeechRate(0.9f)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            tts?.shutdown()
            audioPlayer?.release() // 🔊 Asegura liberar el MediaPlayer al salir
        }
    }

    // Lógica principal: Check de palabra completada y manejo de audios
    LaunchedEffect(detectedText, targetWord) {
        val detected = detectedText.trim().uppercase()
        val target = targetWord.trim().uppercase()

        // 1. Lógica de Éxito
        if (!isWordCompleted && detected == target) {
            isWordCompleted = true
            showSuccessAnimation = true
            isFailurePlayed = false // Reinicia la bandera de fallo

            // 🔊 Reproducir audio de éxito
            playAudioFromUrl(AUDIO_EXITO_URL)

            // Play TTS (opcional)


            // Save to storage
            // WordHistoryStorage.saveCompletedWord(context, targetWord) // Asegúrate de tener esta clase

            // Ocultar animación y completar
            delay(3000)
            onWordCompleted()

            // 2. Lógica de Fallo (detecta texto, pero no es el correcto)
        } else if (!isWordCompleted && detected.length >= 3 && detected != target) {

            if (!isFailurePlayed) {
                isFailurePlayed = true // Activa la bandera para evitar repetición

                // 🔊 Reproducir audio de fallo
                playAudioFromUrl(AUDIO_FALLO_URL)

                // Después de 2 segundos, permite que el audio de fallo se reproduzca de nuevo
                delay(2000)
                isFailurePlayed = false
            }
        }

        // 3. Reiniciar bandera de fallo si no hay texto detectado
        if (detected.isEmpty()) {
            isFailurePlayed = false
        }
    }

    if (cameraPermissionState.status.isGranted) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Camera Preview
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        previewView = this
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // ROI Overlay
            val density = LocalDensity.current
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val roiWidth = canvasWidth * 0.8f
                val roiHeight = canvasHeight * 0.3f
                val roiLeft = (canvasWidth - roiWidth) / 2
                val roiTop = (canvasHeight - roiHeight) / 2

                drawRect(
                    color = Color.Black.copy(alpha = 0.5f),
                    topLeft = Offset.Zero,
                    size = size
                )

                drawRect(
                    color = Color.Transparent,
                    topLeft = Offset(roiLeft, roiTop),
                    size = Size(roiWidth, roiHeight),
                    blendMode = androidx.compose.ui.graphics.BlendMode.Clear
                )

                drawRect(
                    color = Color.Green,
                    topLeft = Offset(roiLeft, roiTop),
                    size = Size(roiWidth, roiHeight),
                    style = Stroke(width = with(density) { 4.dp.toPx() })
                )
            }

            // Top Bar
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
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.7f)
                )
            )

            // Detected Text Display
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

            // Success Animation
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
                        Text(
                            text = "🎉",
                            fontSize = 48.sp
                        )
                        Text(
                            text = "¡Palabra Completada!",
                            color = Color.White,
                            fontFamily = dmSansFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = targetWord,
                            color = Color.White,
                            fontFamily = dmSansFamily,
                            fontSize = 24.sp
                        )
                    }
                }
            }
        }

        // Setup Camera
        LaunchedEffect(previewView) {
            previewView?.let { preview ->
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProvider = cameraProviderFuture.get()

                setupCamera(
                    cameraProvider = cameraProvider!!,
                    previewView = preview,
                    lifecycleOwner = lifecycleOwner,
                    targetWord = targetWord,
                    onTextDetected = { text ->
                        detectedText = text
                    }
                )
            }
        }
    } else {
        // Permission Request
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Se necesita permiso de cámara para usar esta función",
                fontFamily = dmSansFamily,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { cameraPermissionState.launchPermissionRequest() }
            ) {
                Text("Conceder Permiso")
            }
        }
    }
}


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

    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    try {
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner, cameraSelector, preview, imageAnalyzer
        )
    } catch (exc: Exception) {
        Log.e("CameraOCR", "Use case binding failed", exc)
    }
}