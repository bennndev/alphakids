package com.example.alphakids.ui.screens.teacher.students

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Spellcheck
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Spellcheck
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.alphakids.ui.components.*

@Composable
fun TeacherStudentsScreen(
    viewModel: TeacherStudentsViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onStudentClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onBottomNavClick: (String) -> Unit,
    currentRoute: String = "students"
) {

    val teacherBottomNavItems = listOf(
        BottomNavItem("home", "Inicio", Icons.Rounded.Home, Icons.Outlined.Home),
        BottomNavItem("students", "Alumnos", Icons.Rounded.Groups, Icons.Outlined.Groups),
        BottomNavItem("words", "Palabras", Icons.Rounded.Spellcheck, Icons.Outlined.Spellcheck)
    )

    // ViewModel States
    val uiState by viewModel.uiState.collectAsState()
    val students by viewModel.filteredStudents.collectAsState()
    val metrics by viewModel.metrics.collectAsState()
    val availableFilters by viewModel.availableFilters.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()

    Scaffold(
        topBar = {
            AppHeader(
                title = "Mis alumnos",
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Regresar", Modifier.size(24.dp))
                    }
                },
                actionIcon = {
                    IconButton(onClick = onLogoutClick) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, "Cerrar sesión", Modifier.size(24.dp))
                    }
                }
            )
        },
        bottomBar = {
            MainBottomBar(
                items = teacherBottomNavItems,
                currentRoute = currentRoute,
                onNavigate = onBottomNavClick
            )
        },
        floatingActionButton = {
            CustomFAB(
                icon = Icons.Rounded.Settings,
                contentDescription = "Configuración",
                onClick = onSettingsClick
            )
        }
    ) { paddingValues ->

        Column(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            // Métricas
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                InfoCard(Modifier.weight(1f), "Total Alumnos", metrics.totalStudents.toString())
                InfoCard(Modifier.weight(1f), "Grados", metrics.totalGrades.toString())
            }

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                InfoCard(Modifier.weight(1f), "Secciones", metrics.totalSections.toString())
                InfoCard(Modifier.weight(1f), "Instituciones", metrics.totalInstitutions.toString())
            }

            Spacer(Modifier.height(24.dp))

            // Buscador
            SearchBar(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholderText = "Buscar por nombre"
            )

            Spacer(Modifier.height(16.dp))

            // Filtros de grado
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                InfoChip(
                    text = "Todos",
                    isSelected = selectedFilter == null,
                    onClick = { viewModel.updateSelectedFilter(null) }
                )

                availableFilters.forEach { grade ->
                    InfoChip(
                        text = grade,
                        isSelected = selectedFilter == grade,
                        onClick = { viewModel.updateSelectedFilter(grade) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Lista de estudiantes
            if (students.isEmpty()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (searchQuery.isEmpty() && selectedFilter == null)
                            "No hay estudiantes disponibles"
                        else
                            "No se encontraron estudiantes con los filtros aplicados",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(students, key = { it.id }) { student ->

                        // SIN CONTEO REAL ⬇⬇⬇
                        StudentListItem(
                            fullname = "${student.nombre} ${student.apellido}",
                            age = "${student.edad} años",
                            numWords = "0 palabras", // <- SIN CONTEO
                            icon = Icons.Rounded.Face,
                            chipText = student.grado,
                            isSelected = (uiState.selectedStudentId == student.id),
                            onClickNavigation = {
                                viewModel.selectStudent(student.id)
                                onStudentClick(student.id)
                            }
                        )
                    }
                }
            }
        }
    }
}
