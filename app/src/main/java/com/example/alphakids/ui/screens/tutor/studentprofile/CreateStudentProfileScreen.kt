package com.example.alphakids.ui.screens.tutor.studentprofile

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.alphakids.ui.student.StudentUiState
import com.example.alphakids.ui.student.StudentViewModel
import com.example.alphakids.ui.components.AppHeader
import com.example.alphakids.ui.components.IconContainer
import com.example.alphakids.ui.components.LabeledDropdownField
import com.example.alphakids.ui.components.LabeledTextField
import com.example.alphakids.ui.components.PrimaryButton
import com.example.alphakids.ui.theme.AlphakidsTheme
import com.example.alphakids.ui.theme.dmSansFamily
import kotlinx.coroutines.flow.collectLatest

@Composable
fun CreateStudentProfileScreen(
    onBackClick: () -> Unit,
    onCloseClick: () -> Unit,
    onCreateSuccess: () -> Unit,
    viewModel: StudentViewModel = hiltViewModel()
) {
    var nombre by remember { mutableStateOf("") }
    var apellido by remember { mutableStateOf("") }
    var edadString by remember { mutableStateOf("") }

    val institutions by viewModel.institutions.collectAsState()
    val grades by viewModel.grades.collectAsState()
    val sections by viewModel.sections.collectAsState()
    val filteredDocentes by viewModel.filteredDocentes.collectAsState()

    val selectedInstitucion by viewModel.selectedInstitution.collectAsState()
    val selectedGrado by viewModel.selectedGrade.collectAsState()
    val selectedSeccion by viewModel.selectedSection.collectAsState()

    var selectedDocenteName by remember { mutableStateOf("") }
    var selectedDocenteId by remember { mutableStateOf<String?>(null) }

    var institutionExpanded by remember { mutableStateOf(false) }
    var gradeExpanded by remember { mutableStateOf(false) }
    var sectionExpanded by remember { mutableStateOf(false) }
    var docenteExpanded by remember { mutableStateOf(false) }

    val uiState by viewModel.createUiState.collectAsState()
    val isLoading = uiState is StudentUiState.Loading
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.createUiState.collectLatest { state ->
            when (state) {
                is StudentUiState.Success -> {
                    Toast.makeText(context, "Perfil creado", Toast.LENGTH_SHORT).show()
                    onCreateSuccess()
                    viewModel.resetCreateState()
                }
                is StudentUiState.Error -> {
                    Toast.makeText(context, "Error: ${state.message}", Toast.LENGTH_LONG).show()
                    viewModel.resetCreateState()
                }
                else -> {}
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppHeader(
                title = "Crear Perfil",
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                actionIcon = {
                    IconButton(onClick = onCloseClick) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                IconContainer(
                    icon = Icons.Rounded.ChildCare,
                    contentDescription = "Icono Niño"
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Nuevo Perfil",
                    fontFamily = dmSansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    "Ingresa los datos de tu hijo",
                    fontFamily = dmSansFamily,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(32.dp))

                // Nombre
                LabeledTextField(
                    label = "Nombre",
                    value = nombre,
                    onValueChange = { nombre = it },
                    placeholderText = "Nombre del niño"
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Apellido
                LabeledTextField(
                    label = "Apellido",
                    value = apellido,
                    onValueChange = { apellido = it },
                    placeholderText = "Apellido del niño"
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Edad
                LabeledTextField(
                    label = "Edad",
                    value = edadString,
                    onValueChange = { edadString = it.filter { char -> char.isDigit() } },
                    placeholderText = "Edad del niño (ej. 4)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 1. Institución
                Box(modifier = Modifier.fillMaxWidth()) {
                    LabeledDropdownField(
                        label = "Institución",
                        selectedOption = selectedInstitucion?.nombre ?: "",
                        placeholderText = "Selecciona institución",
                        onClick = {
                            institutionExpanded = true
                        }
                    )
                    DropdownMenu(
                        expanded = institutionExpanded,
                        onDismissRequest = { institutionExpanded = false }
                    ) {
                        institutions.forEach { institution ->
                            DropdownMenuItem(
                                text = { Text(text = institution.nombre) },
                                onClick = {
                                    viewModel.selectInstitution(institution)
                                    // Reset dependientes
                                    selectedDocenteName = ""
                                    selectedDocenteId = null
                                    institutionExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Grado
                Box(modifier = Modifier.fillMaxWidth()) {
                    LabeledDropdownField(
                        label = "Grado",
                        selectedOption = selectedGrado?.name ?: "",
                        placeholderText = "Selecciona grado",
                        onClick = {
                            if (selectedInstitucion != null) {
                                gradeExpanded = true
                            } else {
                                Toast.makeText(
                                    context,
                                    "Primero selecciona una institución",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = gradeExpanded,
                        onDismissRequest = { gradeExpanded = false }
                    ) {
                        grades.forEach { grade ->
                            DropdownMenuItem(
                                text = { Text(text = grade.name) },
                                onClick = {
                                    viewModel.selectGrade(grade)
                                    // Reset dependientes
                                    selectedDocenteName = ""
                                    selectedDocenteId = null
                                    gradeExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Sección
                Box(modifier = Modifier.fillMaxWidth()) {
                    LabeledDropdownField(
                        label = "Sección",
                        selectedOption = selectedSeccion?.code ?: "",
                        placeholderText = "Selecciona sección",
                        onClick = {
                            if (selectedGrado != null) {
                                sectionExpanded = true
                            } else {
                                Toast.makeText(
                                    context,
                                    "Primero selecciona un grado",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = sectionExpanded,
                        onDismissRequest = { sectionExpanded = false }
                    ) {
                        sections.forEach { section ->
                            DropdownMenuItem(
                                text = { Text(text = section.code) },
                                onClick = {
                                    viewModel.selectSection(section)
                                    selectedDocenteName = ""
                                    selectedDocenteId = null
                                    sectionExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4. Docente
                Box(modifier = Modifier.fillMaxWidth()) {
                    LabeledDropdownField(
                        label = "Docente",
                        selectedOption = selectedDocenteName,
                        placeholderText = "Selecciona docente (Opcional)",
                        onClick = {
                            if (selectedSeccion != null) {
                                docenteExpanded = true
                            } else {
                                Toast.makeText(
                                    context,
                                    "Primero selecciona una sección",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = docenteExpanded,
                        onDismissRequest = { docenteExpanded = false }
                    ) {
                        if (filteredDocentes.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No hay docentes asignados") },
                                onClick = { docenteExpanded = false },
                                enabled = false
                            )
                        } else {
                            filteredDocentes.forEach { usuario ->
                                DropdownMenuItem(
                                    text = { Text(text = "${usuario.nombre} ${usuario.apellido}") },
                                    onClick = {
                                        selectedDocenteName = "${usuario.nombre} ${usuario.apellido}"
                                        selectedDocenteId = usuario.uid
                                        docenteExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                if (uiState is StudentUiState.Error) {
                    // Aquí podrías mostrar un Text con el error si quieres
                }

                PrimaryButton(
                    text = "Crear Perfil",
                    onClick = {
                        val edadInt = edadString.toIntOrNull()
                        if (nombre.isBlank()) {
                            Toast.makeText(context, "Ingresa el nombre", Toast.LENGTH_SHORT).show()
                            return@PrimaryButton
                        }
                        if (apellido.isBlank()) {
                            Toast.makeText(context, "Ingresa el apellido", Toast.LENGTH_SHORT).show()
                            return@PrimaryButton
                        }
                        if (edadInt == null || edadInt <= 0) {
                            Toast.makeText(context, "Ingresa una edad válida", Toast.LENGTH_SHORT).show()
                            return@PrimaryButton
                        }

                        viewModel.createStudent(
                            nombre = nombre,
                            apellido = apellido,
                            edad = edadInt,
                            grado = selectedGrado?.name ?: "",
                            seccion = selectedSeccion?.code ?: "",
                            idInstitucion = selectedInstitucion?.id ?: "",
                            idDocente = selectedDocenteId
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CreateStudentProfileScreenPreview() {
    AlphakidsTheme {
        CreateStudentProfileScreen({}, {}, {})
    }
}
