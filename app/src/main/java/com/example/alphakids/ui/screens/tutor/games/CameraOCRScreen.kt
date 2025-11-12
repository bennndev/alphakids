package com.example.alphakids.ui.screens.tutor.games

import ScannerOverlay
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
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
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import java.util.concurrent.Executors
import com.example.alphakids.ui.screens.tutor.games.WordStorage


@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CameraOCRScreen(
    assignmentId: String,
    targetWord: String,
    studentId: String, // 🚨 ¡PARÁMETRO AÑADIDO! Necesario para la navegación de resultado.
    targetImageUrl: String?,
    onBackClick: () -> Unit,
    // 🚨 ¡CAMBIO DE FIRMA! Ahora necesitamos el studentId para la navegación de resultado
    onWordCompleted: (word: String, imageUrl: String?, studentId: String) -> Unit,
    onTimeExpired: (imageUrl: String?, studentId: String) -> Unit
) {
    Log.d("DebugImagen", "PASO 3 (CAMARA): ¿URL recibida? URL = $targetImageUrl")
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // --- ESTADO DE LÓGICA ---
    var detectedText by remember { mutableStateOf("") }
    var isWordCompleted by remember { mutableStateOf(false) } // Clave para detener el timer/OCR
    var isNavigating by remember { mutableStateOf(false) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    // --- ESTADO DE UI ---
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var torchOn by remember { mutableStateOf(false) }
    var lensFacingBack by remember { mutableStateOf(true) }
    val previewViewRef = remember { mutableStateOf<PreviewView?>(null) }
    var roiRect by remember { mutableStateOf<FloatArray?>(null) }
    var showNotification by remember { mutableStateOf(true) }

    val totalMillis = 60_000L
    var remainingMillis by remember { mutableStateOf(totalMillis) }
    var progress by remember { mutableStateOf(0f) }
    var isWarning by remember { mutableStateOf(false) }


    // --- CONTROLADOR DE CÁMARA ---
    val cameraController = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        }
    }
    val executor = remember { Executors.newSingleThreadExecutor() }

    suspend fun safeReleaseCamera(delayMs: Long = 300L) {
        try {
            cameraController.unbind()
        } catch (e: Exception) {
            Log.e("CameraOCR", "Error al liberar cámara: ${e.message}")
        }
        try {
            executor.shutdownNow()
        } catch (_: Exception) { }
        delay(delayMs)
    }
    // --- LÓGICA DE INICIALIZACIÓN Y LIBERACIÓN ---

    // Inicializar TTS
    LaunchedEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("es", "ES")
                tts?.setSpeechRate(0.9f)
            }
        }
    }

    // Liberar TTS, Cámara y Executor
    DisposableEffect(Unit) {
        onDispose {
            try {
                // 🚨 ¡IMPORTANTE! Desvincular cámara para liberarla
                cameraController.unbind()
                executor.shutdownNow()
                tts?.stop()
                tts?.shutdown()
            } catch (e: Exception) {
                Log.e("CameraOCR", "Error al liberar recursos en onDispose: ${e.message}")
            }
        }
    }

    // Vinculación de la cámara al ciclo de vida
    LaunchedEffect(lifecycleOwner) {
        cameraController.bindToLifecycle(lifecycleOwner)
    }

    // Configuración del analizador de texto
    LaunchedEffect(cameraController, targetWord) {
        val textAnalyzer = TextAnalyzer(
            targetWord = targetWord,
            onTextDetected = { text ->
                // El analizador llama desde el hilo de fondo.
                // Usamos el scope para saltar al hilo principal y actualizar el estado
                scope.launch(Dispatchers.Main) {
                    detectedText = text
                }
            }
        )
        cameraController.setImageAnalysisAnalyzer(executor, textAnalyzer)
    }


    // --- LÓGICA DE "GANAR" (WORD COMPLETED) ---
    LaunchedEffect(detectedText, targetWord) {
        // 🚨 ¡Arreglo! targetWord viene decodificada en la navegación,
        // pero la limpiamos por si acaso.
        val cleanDetectedText = detectedText.trim().uppercase()
        val cleanTargetWord = targetWord.trim().uppercase()

        if (!isWordCompleted &&
            !isNavigating &&
            cleanTargetWord.isNotEmpty() &&
            cleanDetectedText.contains(cleanTargetWord)
        ) {
            isWordCompleted = true
            isNavigating = true

            WordStorage.saveCompletedWord(context, targetWord, assignmentId)

            tts?.speak("¡Bien hecho! La palabra es $targetWord",
                TextToSpeech.QUEUE_FLUSH, null, null)

            scope.launch {
                withContext(Dispatchers.IO) { safeReleaseCamera() }
                withContext(Dispatchers.Main) {
                    try {
                        onWordCompleted(targetWord, targetImageUrl, studentId) // ✅ añadí studentId
                    } catch (e: Exception) {
                        Log.e("CameraOCR", "Error onWordCompleted: ${e.message}")
                        isNavigating = false
                    }
                }
            }
        }

    }


    // --- LÓGICA DE "PERDER" (TEMPORIZADOR) ---
    LaunchedEffect(isWordCompleted) {
        // Bucle de temporizador
        while (remainingMillis > 0 && !isWordCompleted) {
            delay(1000)
            if (!isWordCompleted) {
                remainingMillis -= 1000
                progress = 1f - (remainingMillis.toFloat() / totalMillis.toFloat())
                isWarning = remainingMillis <= 10_000L
            }
        }

        // LÓGICA DE TIEMPO AGOTADO
        if (!isWordCompleted && remainingMillis <= 0) {
            isWordCompleted = true // Marca como completado para evitar doble navegación
            onTimeExpired(targetImageUrl, studentId)

            // Inicia corutina para apagar cámara Y LUEGO navegar
            scope.launch {
                try {
                    // 🚨 Desvinculamos la cámara explícitamente antes de navegar
                    cameraController.unbind()
                } catch (e: Exception) {
                    Log.e("CameraOCR", "Error al desvincular cámara (perder): ${e.message}")
                }
                delay(200) // Pequeña pausa
                withContext(Dispatchers.Main) { // Navega en el hilo principal
                    // 🚨 Incluimos studentId en la navegación
                    onTimeExpired(targetImageUrl, studentId)
                }
            }
        }
    }

    // Función de ayuda
    fun formatTime(ms: Long): String {
        val totalSec = (ms / 1000).toInt()
        val m = totalSec / 60
        val s = totalSec % 60
        return String.format("%d:%02d", m, s)
    }

    // --- RENDERIZADO DE UI (Sin Cambios significativos) ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ... (Contenido de la UI es similar, no lo repito para brevedad) ...

        // Vista de Cámara
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
            // Pantalla de solicitud de permiso
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
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { cameraPermissionState.launchPermissionRequest() }
                ) {
                    Text("Conceder Permiso")
                }
            }
        }

        // Lógica de Enfoque
        LaunchedEffect(roiRect, previewViewRef.value) {
            val pv = previewViewRef.value
            val rect = roiRect
            if (pv != null && rect != null && pv.width > 0 && pv.height > 0) {
                val cx = ((rect[0] + rect[2]) / 2f) * pv.width
                val cy = ((rect[1] + rect[3]) / 2f) * pv.height
                val point: MeteringPoint = pv.meteringPointFactory.createPoint(cx, cy)
                val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                    .addPoint(point, FocusMeteringAction.FLAG_AE)
                    .build()
                try {
                    cameraController.cameraControl?.startFocusAndMetering(action)
                } catch (_: Exception) {
                }
            }
        }

        // Overlay del Escáner
        ScannerOverlay(
            modifier = Modifier.fillMaxSize(),
            boxWidthPercent = 0.8f,
            boxAspectRatio = 1.6f,
            onBoxRectChange = { l, t, r, b ->
                roiRect = floatArrayOf(l, t, r, b)
            }
        )

        // Barra Superior: Botón Atrás + Timer + Notificación
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Regresar",
                        modifier = Modifier.size(24.dp),
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.size(16.dp))
                Box(modifier = Modifier.weight(1f)) {
                    TimerBar(
                        modifier = Modifier.fillMaxWidth(),
                        progress = progress,
                        timeText = formatTime(remainingMillis),
                        isWarning = isWarning
                    )
                }
            }

            if (showNotification) {
                NotificationCard(
                    modifier = Modifier.padding(top = 12.dp),
                    title = "Busca la palabra:",
                    content = targetWord,
                    imageUrl = targetImageUrl,
                    icon = Icons.Rounded.Checkroom,
                    onCloseClick = {
                        showNotification = false
                    }
                )
            }
        }

        // Barra de Acción
        CameraActionBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            onFlashClick = {
                torchOn = !torchOn
                cameraController.enableTorch(torchOn)
            },
            onShutterClick = null,
            onFlipCameraClick = {
                lensFacingBack = !lensFacingBack
                cameraController.cameraSelector = if (lensFacingBack) {
                    CameraSelector.DEFAULT_BACK_CAMERA
                } else {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                }
            }
        )

        // Muestra de Texto Detectado
        if (detectedText.isNotEmpty() && !isWordCompleted) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 120.dp)
                    .padding(horizontal = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.7f)
                )
            ) {
                Text(
                    text = "Detectado: $detectedText",
                    color = Color.White,
                    fontFamily = dmSansFamily,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}