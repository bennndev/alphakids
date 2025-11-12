package com.example.alphakids.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.alphakids.domain.models.UserRole
import com.example.alphakids.ui.auth.AuthViewModel
import com.example.alphakids.ui.word.WordUiState
import com.example.alphakids.ui.word.WordViewModel
import com.example.alphakids.ui.components.ActionDialog

// Teacher Screens
import com.example.alphakids.ui.screens.teacher.home.TeacherHomeScreen
import com.example.alphakids.ui.screens.teacher.students.*
import com.example.alphakids.ui.screens.teacher.words.*

// Tutor General Screens
import com.example.alphakids.ui.screens.tutor.home.StudentHomeScreen
import com.example.alphakids.ui.screens.tutor.profile_selection.ProfileSelectionScreen
import com.example.alphakids.ui.screens.tutor.dictionary.StudentDictionaryScreen
import com.example.alphakids.ui.screens.tutor.achievements.StudentAchievementsScreen

// Tutor Profiles
import com.example.alphakids.ui.screens.tutor.studentprofile.CreateStudentProfileScreen
import com.example.alphakids.ui.screens.tutor.studentprofile.EditStudentProfileScreen

// Games
import com.example.alphakids.ui.screens.tutor.games.*
import com.example.alphakids.ui.screens.tutor.games.WordPuzzleScreen
import com.example.alphakids.ui.screens.tutor.games.WordPuzzleViewModel
import com.example.alphakids.ui.screens.tutor.games.CameraOCRScreen

// Store
import com.example.alphakids.ui.screens.tutor.store.StudentStoreScreen
import com.example.alphakids.ui.screens.tutor.store.StudentPetsStoreScreen
import com.example.alphakids.ui.screens.tutor.store.StudentAccessoriesStoreScreen

// Pets
import com.example.alphakids.ui.screens.tutor.pets.StudentPetsScreen
import com.example.alphakids.ui.screens.tutor.pets.StudentPetDetailScreen

// Profile General
import com.example.alphakids.ui.screens.profile.EditProfileScreen

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val currentUser by authViewModel.currentUser.collectAsState()

    val startDestination = when {
        currentUser == null -> Routes.ROLE_SELECTION
        currentUser?.rol == UserRole.TUTOR -> Routes.PROFILES
        currentUser?.rol == UserRole.DOCENTE -> Routes.TEACHER_HOME
        else -> Routes.ROLE_SELECTION
    }

    val onLogout = { authViewModel.logout() }

    // Navegación inferior estudiante
    val navigateToStudentBottomNav: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(Routes.HOME) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    // Navegación inferior docente
    val navigateToTeacherBottomNav: (String) -> Unit = { base ->
        val targetRoute = when (base) {
            "students" -> Routes.TEACHER_STUDENTS
            "words" -> Routes.WORDS
            else -> Routes.TEACHER_HOME
        }
        navController.navigate(targetRoute) {
            popUpTo(Routes.TEACHER_HOME) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {

        // ============================================================
        // ROL + LOGIN
        // ============================================================
        composable(Routes.ROLE_SELECTION) {
            com.example.alphakids.ui.screens.common.RoleSelectScreen(
                onTutorClick = { navController.navigate(Routes.loginRoute(Routes.ROLE_TUTOR)) },
                onTeacherClick = { navController.navigate(Routes.loginRoute(Routes.ROLE_TEACHER)) }
            )
        }

        composable(
            Routes.LOGIN,
            arguments = listOf(navArgument("role") { type = NavType.StringType })
        ) { entry ->
            val role = entry.arguments?.getString("role") ?: Routes.ROLE_TEACHER
            val isTutor = role == Routes.ROLE_TUTOR

            com.example.alphakids.ui.auth.LoginScreen(
                onBackClick = { navController.popBackStack() },
                onCloseClick = { navController.popBackStack() },
                onLoginSuccess = {
                    val next = if (isTutor) Routes.PROFILES else Routes.TEACHER_HOME
                    navController.navigate(next) {
                        popUpTo(Routes.ROLE_SELECTION) { inclusive = true }
                    }
                },
                onForgotPasswordClick = {},
                onRegisterClick = { navController.navigate(Routes.registerRoute(role)) },
                isTutorLogin = isTutor
            )
        }

        composable(
            Routes.REGISTER,
            arguments = listOf(navArgument("role") { type = NavType.StringType })
        ) { entry ->
            val role = entry.arguments?.getString("role") ?: Routes.ROLE_TEACHER
            val isTutor = role == Routes.ROLE_TUTOR

            com.example.alphakids.ui.auth.RegisterScreen(
                onBackClick = { navController.popBackStack() },
                onCloseClick = { navController.popBackStack() },
                onRegisterSuccess = {
                    val next = if (isTutor) Routes.PROFILES else Routes.TEACHER_HOME
                    navController.navigate(next) {
                        popUpTo(Routes.ROLE_SELECTION) { inclusive = true }
                    }
                },
                isTutorRegister = isTutor
            )
        }

        // ============================================================
        // PERFILES TUTOR
        // ============================================================
        composable(Routes.PROFILES) {
            ProfileSelectionScreen(
                onProfileClick = { navController.navigate(Routes.homeRoute(it)) },
                onAddProfileClick = { navController.navigate(Routes.STUDENT_PROFILE_CREATE) },
                onSettingsClick = { navController.navigate(Routes.editProfileRoute(Routes.ROLE_TUTOR)) },
                onLogoutClick = onLogout
            )
        }

        // ============================================================
        // HOME ESTUDIANTE
        // ============================================================
        composable(
            Routes.HOME,
            arguments = listOf(navArgument("studentId") { type = NavType.StringType })
        ) { entry ->

            val studentId = entry.arguments?.getString("studentId") ?: "default"

            StudentHomeScreen(
                studentName = "Estudiante",
                onLogoutClick = onLogout,
                onBackClick = { navController.popBackStack() },
                onPlayClick = { navController.navigate(Routes.assignedWordsRoute(studentId)) },
                onDictionaryClick = { navigateToStudentBottomNav(Routes.dictionaryRoute(studentId)) },
                onAchievementsClick = { navigateToStudentBottomNav(Routes.achievementsRoute(studentId)) },
                onSettingsClick = { navController.navigate(Routes.editStudentProfileRoute(studentId)) },
                onBottomNavClick = { route ->
                    val target = when (route) {
                        "store" -> Routes.storeRoute(studentId)
                        "pets" -> Routes.petsRoute(studentId)
                        else -> Routes.homeRoute(studentId)
                    }
                    navigateToStudentBottomNav(target)
                },
                currentRoute = "home"
            )
        }

        // ============================================================
        // JUEGOS
        // ============================================================
        composable(
            Routes.MY_GAMES,
            arguments = listOf(navArgument("studentId") { type = NavType.StringType })
        ) { entry ->
            val studentId = entry.arguments?.getString("studentId") ?: "default"
            MyGamesScreen(
                onBackClick = { navController.popBackStack() },
                onWordsGameClick = { navController.navigate(Routes.gameWordsRoute(studentId)) }
            )
        }

        composable(
            Routes.GAME_WORDS,
            arguments = listOf(navArgument("studentId") { type = NavType.StringType })
        ) {
            GameWordsScreen(
                onBackClick = { navController.popBackStack() },
                onWordClick = { }
            )
        }

        composable(
            Routes.ASSIGNED_WORDS,
            arguments = listOf(navArgument("studentId") { type = NavType.StringType })
        ) { entry ->
            val studentId = entry.arguments?.getString("studentId") ?: "default"
            AssignedWordsScreen(
                studentId = studentId,
                onBackClick = { navController.popBackStack() },
                onWordClick = { assignment ->
                    navController.navigate(Routes.wordPuzzleRoute(assignment.id ?: ""))
                }
            )
        }

        composable(
            Routes.WORD_PUZZLE,
            arguments = listOf(navArgument("assignmentId") { type = NavType.StringType })
        ) { entry ->
            val assignmentId = entry.arguments?.getString("assignmentId") ?: ""
            val viewModel: WordPuzzleViewModel = hiltViewModel(entry)
            val uiState by viewModel.uiState.collectAsState()

            LaunchedEffect(assignmentId) {
                viewModel.loadWordData(assignmentId)
            }

            WordPuzzleScreen(
                assignmentId = assignmentId,
                onBackClick = { navController.popBackStack() },
                onTakePhotoClick = {
                    val targetWord = uiState.assignment?.palabraTexto ?: ""
                    if (targetWord.isNotEmpty()) {
                        navController.navigate(
                            Routes.cameraOCRRoute(assignmentId, targetWord)
                        )
                    }
                }
            )
        }

        composable(
            Routes.CAMERA_OCR,
            arguments = listOf(
                navArgument("assignmentId") { type = NavType.StringType },
                navArgument("targetWord") { type = NavType.StringType }
            )
        ) { entry ->
            CameraOCRScreen(
                assignmentId = entry.arguments?.getString("assignmentId") ?: "",
                targetWord = entry.arguments?.getString("targetWord") ?: "",
                onBackClick = { navController.popBackStack() },
                onWordCompleted = { navController.popBackStack() }
            )
        }

        // ============================================================
        // DICCIONARIO, LOGROS, DOCENTE, PERFIL, TIENDA, MASCOTAS
        // ============================================================
        // 🟢 Mantener resto idéntico (sin conflictos) ...
        // (El contenido restante de tu versión ya estaba limpio, sin conflictos.)
        // Puedes conservar exactamente igual desde aquí.
    }
}
