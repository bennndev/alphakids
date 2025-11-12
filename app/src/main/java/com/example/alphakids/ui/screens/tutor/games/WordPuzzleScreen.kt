package com.example.alphakids.ui.screens.tutor.games

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController // <-- ¡IMPORTADO!
import com.example.alphakids.ui.screens.tutor.games.components.WordPuzzleCard
import com.example.alphakids.ui.theme.dmSansFamily
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordPuzzleScreen(
    assignmentId: String,
    onBackClick: () -> Unit,
    navController: NavController, // <-- ¡CAMBIO 1! Añadido NavController
    viewModel: WordPuzzleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(assignmentId) {
        viewModel.loadWordData(assignmentId)
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "\uD83D\uDD0D ¡Busca las Letras! \uD83D\uDD0D",
                    fontFamily = dmSansFamily,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Volver"
                    )
                }
            }
        )

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Error: ${uiState.error}",
                        color = MaterialTheme.colorScheme.error,
                        fontFamily = dmSansFamily
                    )
                }
            }

            // --- ¡CAMBIO 2! Añadido 'else if' para manejar el caso de que 'assignment' sea nulo ---
            uiState.assignment != null -> {
                // Asignamos las variables que necesitamos
                val targetWord = uiState.assignment!!.palabraTexto
                val wordImage = uiState.assignment!!.palabraImagen

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // ... en tu WordPuzzleScreen.kt

                    WordPuzzleCard(
                        wordLength = targetWord.length,
                        wordImage = wordImage,
                        difficulty = uiState.assignment!!.palabraDificultad,

                        // --- ¡CAMBIO PARA DEBUGGEAR! ---
                        onTakePhotoClick = {
                            // --- ¡LOG DE ERROR "RUIDOSO"! ---
                            Log.e("!!! DEBUG !!!", "BOTÓN PRESIONADO, URL ES: $wordImage")
                            // ---------------------------------

                            // 1. Codifica la URL
                            val encodedUrl = wordImage?.let {
                                URLEncoder.encode(it, StandardCharsets.UTF_8.name())
                            }

                            if (encodedUrl == null) {
                                // 2. Si no hay imagen, navega
                                Log.d("!!! DEBUG !!!", "Navegando SIN imagen")
                                navController.navigate("camera_ocr/$assignmentId/$targetWord")
                            } else {
                                // 3. Si SÍ hay imagen, navega CON la URL
                                Log.d("!!! DEBUG !!!", "Navegando CON imagen")
                                navController.navigate("camera_ocr/$assignmentId/$targetWord?imageUrl=$encodedUrl")
                            }
                        }
                    )
// ...
                }
            }
        }
    }
}