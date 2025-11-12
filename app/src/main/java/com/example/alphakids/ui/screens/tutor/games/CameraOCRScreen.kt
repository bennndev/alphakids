package com.example.alphakids.ui.screens.tutor.games

import ScannerOverlay // Componente de UI (Archivo 2)
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
// import androidx.annotation.DrawableRes // <-- ¡CAMBIO 1! Ya no se usa
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.MeteringPoint
import androidx.camera.core.FocusMeteringAction
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.* // Lógica de UI (Archivo 1)
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack // Icono de UI (Archivo 2)
import androidx.compose.material.icons.rounded.Checkroom // Icono de fallback
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
import com.example.alphakids.ui.components.NotificationCard // Componente de UI (Archivo 2)
import com.example.alphakids.ui.components.TimerBar // Componente de UI (Archivo 2)
import com.example.alphakids.ui.screens.tutor.games.components.CameraActionBar // Componente de UI (Archivo 2)
import com.example.alphakids.ui.theme.dmSansFamily // Fuente (Archivo 1)
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.delay
import java.util.*
import java.util.concurrent.Executor

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CameraOCRScreen(
    // Parámetros de Lógica (del Archivo 1)
    assignmentId: String,
    targetWord: String,
    targetImageUrl: String?, // <-- ¡CAMBIO 2! Acepta URL (String)
    onBackClick: () -> Unit,
    onWordCompleted: () -> Unit
) {
    Log.d("DebugImagen", "PASO 3 (CAMARA): ¿URL recibida? URL = $targetImageUrl")
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // --- ESTADO DE LÓGICA (del Archivo 1) ---
    var detectedText by remember { mutableStateOf("") }
    var showSuccessAnimation by remember { mutableStateOf(false) }
    var isWordCompleted by remember { mutableStateOf(false) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    // --- ESTADO DE UI (del Archivo 2) ---
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var torchOn by remember { mutableStateOf(false) }
    var lensFacingBack by remember { mutableStateOf(true) }
    val previewViewRef = remember { mutableStateOf<PreviewView?>(null) }
    var roiRect by remember { mutableStateOf<FloatArray?>(null) }
    var showNotification by remember { mutableStateOf(true) }
    // Estado del temporizador
    val totalMillis = 60_000L // Puedes ajustar esto
    var remainingMillis by remember { mutableStateOf(totalMillis) }
    var progress by remember { mutableStateOf(0f) }
    var isWarning by remember { mutableStateOf(false) }


    // --- CONTROLADOR DE CÁMARA (del Archivo 2, ¡pero modificado!) ---
    val cameraController = remember {
        LifecycleCameraController(context).apply {
            // ¡CAMBIO CLAVE! Usamos IMAGE_ANALYSIS, no IMAGE_CAPTURE
            setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        }
    }
    val executor = ContextCompat.getMainExecutor(context)

    // --- LÓGICA DE INICIALIZACIÓN (del Archivo 1 y 2) ---

    // Manejo de permisos (Archivo 2)
    LaunchedEffect(Unit) {
        if (cameraPermissionState.status != PermissionStatus.Granted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    // Inicializar TTS (Archivo 1)
    LaunchedEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("es", "ES")
                tts?.setSpeechRate(0.9f)
            }
        }
    }

    // Liberar TTS (Archivo 1)
    DisposableEffect(Unit) {
        onDispose {
            tts?.shutdown()
            cameraController.unbind() // También desvincula el controlador
        }
    }

    // Lógica de finalización de palabra (Archivo 1)
    LaunchedEffect(detectedText, targetWord) {
        // Usamos uiState.targetWord en lugar de solo targetWord
        val cleanDetectedText = detectedText.trim().uppercase()
        val cleanTargetWord = targetWord.trim().uppercase()

        if (!isWordCompleted && cleanDetectedText.contains(cleanTargetWord) && cleanTargetWord.isNotEmpty()) {
            isWordCompleted = true
            showSuccessAnimation = true

            // Play TTS
            tts?.speak(
                "¡Bien hecho! La palabra es $targetWord",
                TextToSpeech.QUEUE_FLUSH,
                null,
                null
            )

            // (Opcional: Guardar en Storage, como en tu Archivo 1)
            // WordHistoryStorage.saveCompletedWord(context, targetWord)

            // Hide animation after 3 seconds and complete
            delay(3000)
            onWordCompleted()
        }
    }

    // Lógica del Temporizador (Archivo 2)
    LaunchedEffect(Unit) {
        while (remainingMillis > 0) {
            delay(1000)
            remainingMillis -= 1000
            progress = 1f - (remainingMillis.toFloat() / totalMillis.toFloat())
            isWarning = remainingMillis <= 10_000L
        }
        // Opcional: ¿Qué pasa cuando el tiempo se acaba?
        // onWordCompleted() // ¿Quizás falló?
    }

    fun formatTime(ms: Long): String {
        val totalSec = (ms / 1000).toInt()
        val m = totalSec / 60
        val s = totalSec % 60
        return String.format("%d:%02d", m, s)
    }

    // --- CONFIGURACIÓN DE CÁMARA (Fusión de Archivo 1 y 2) ---
    // Vincula al ciclo de vida
    LaunchedEffect(lifecycleOwner) {
        cameraController.bindToLifecycle(lifecycleOwner)
    }

    // Configura el analizador de imágenes
    LaunchedEffect(cameraController, targetWord) {
        val textAnalyzer = TextAnalyzer(
            // targetWord = targetWord, // El analizador no necesita la palabra
            targetWord = targetWord,
            onTextDetected = { text ->
                detectedText = text // Actualiza el estado del Archivo 1
            }
        )

        cameraController.setImageAnalysisAnalyzer(executor, textAnalyzer)
    }


    // --- RENDERIZADO DE UI ---

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Vista de Cámara (Archivo 2)
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
            // Pantalla de solicitud de permiso (Archivo 1)
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

        // Lógica de Enfoque (Archivo 2)
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
                } catch (_: Exception) { }
            }
        }

        // Overlay del Escáner (Archivo 2) - Reemplaza el Canvas del Archivo 1
        ScannerOverlay(
            modifier = Modifier.fillMaxSize(),
            boxWidthPercent = 0.8f,
            boxAspectRatio = 1.6f, // Más ancho para palabras
            onBoxRectChange = { l, t, r, b ->
                roiRect = floatArrayOf(l, t, r, b)
            }
        )

        // Barra Superior: Botón Atrás + Timer + Notificación (Archivo 2)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) { // Lógica del Archivo 1
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

            // Fusión de Lógica: Usamos la notificación para mostrar el targetWord
            if (showNotification) {
                NotificationCard(
                    modifier = Modifier.padding(top = 12.dp),
                    title = "Busca la palabra:", // Lógica del Archivo 1
                    content = targetWord,        // Lógica del Archivo 1
                    imageUrl = targetImageUrl, // <-- ¡CAMBIO 3! Pasa la URL
                    icon = Icons.Rounded.Checkroom, // <-- Pasa un icono de fallback
                    onCloseClick = {
                        showNotification = false
                    }
                )
            }
        }

        // Barra de Acción (Archivo 2)
        CameraActionBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            onFlashClick = {
                torchOn = !torchOn
                cameraController.enableTorch(torchOn)
            },
            onShutterClick = null, // <-- ¡CAMBIO 4! Pasa null
            onFlipCameraClick = {
                lensFacingBack = !lensFacingBack
                cameraController.cameraSelector = if (lensFacingBack) {
                    CameraSelector.DEFAULT_BACK_CAMERA
                } else {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                }
            }
        )

        // Muestra de Texto Detectado (Archivo 1)
        if (detectedText.isNotEmpty() && !showSuccessAnimation) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 120.dp) // Encima del Action Bar
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

        // Animación de Éxito (Archivo 1)
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
}