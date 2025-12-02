package com.example.alphakids.ui.screens.teacher.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alphakids.domain.models.User
import com.example.alphakids.domain.repository.AssignmentRepository
import com.example.alphakids.domain.repository.AuthRepository
import com.example.alphakids.domain.repository.WordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class TeacherHomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val assignmentRepository: AssignmentRepository,
    private val wordRepository: WordRepository
) : ViewModel() {

    // Obtener el usuario actual directamente del repositorio
    val currentUser: StateFlow<User?> = authRepository.getCurrentUser()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Obtener el nombre completo del profesor
    val teacherName: StateFlow<String> = currentUser
        .map { user ->
            if (user != null) {
                "${user.nombre} ${user.apellido}"
            } else {
                "Profesor"
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "Profesor"
        )

    // Obtener lista de estudiantes
    private val students = currentUser.flatMapLatest { user ->
        if (user != null) {
            assignmentRepository.getStudentsForDocente(user.uid)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Métricas
    val totalStudents: StateFlow<Int> = students
        .map { it.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val activeSections: StateFlow<Int> = students
        .map { list -> list.map { it.seccion }.distinct().count { it.isNotBlank() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val activeInstitutions: StateFlow<Int> = students
        .map { list -> list.map { it.idInstitucion }.distinct().count { it.isNotBlank() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val totalWords: StateFlow<Int> = currentUser.flatMapLatest { user ->
        if (user != null) {
            wordRepository.getWordsByDocente(user.uid).map { it.size }
        } else {
            flowOf(0)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )
}