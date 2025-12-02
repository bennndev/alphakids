package com.example.alphakids.ui.screens.teacher.students

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alphakids.data.firebase.models.Estudiante
import com.example.alphakids.data.mappers.StudentMapper
import com.example.alphakids.domain.usecases.GetCurrentUserUseCase
import com.example.alphakids.domain.usecases.GetStudentsForDocenteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class TeacherStudentsViewModel @Inject constructor(
    private val getStudentsForDocenteUseCase: GetStudentsForDocenteUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeacherStudentsUiState())
    val uiState: StateFlow<TeacherStudentsUiState> = _uiState.asStateFlow()

    // 🔍 Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // 🎓 Filter
    private val _selectedFilter = MutableStateFlow<String?>(null)
    val selectedFilter = _selectedFilter.asStateFlow()

    // 🔥 Load students
    @OptIn(ExperimentalCoroutinesApi::class)
    val students: StateFlow<List<Estudiante>> = getCurrentUserUseCase()
        .map { it?.uid }
        .flatMapLatest { docenteId ->
            docenteId?.let { getStudentsForDocenteUseCase(it) } ?: flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Map to domain Student
    private val domainStudents = students.map { list ->
        list.map { StudentMapper.toDomain(it) }
    }

    // 🎯 Filtering Logic
    val filteredStudents = combine(
        domainStudents,
        _searchQuery,
        _selectedFilter
    ) { students, query, filter ->

        var result = students

        // 🔍 Search by full name
        if (query.isNotBlank()) {
            result = result.filter {
                "${it.nombre} ${it.apellido}".contains(query, ignoreCase = true)
            }
        }

        // 🎓 Filter by Grade
        if (filter != null) {
            result = result.filter { it.grado == filter }
        }

        result
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // 🧮 Metrics (Tarjetas)
    val metrics = domainStudents.map { students ->
        TeacherStudentsMetrics(
            totalStudents = students.size,
            totalGrades = students.map { it.grado }.distinct().count { it.isNotBlank() },
            totalSections = students.map { it.seccion }.distinct().count { it.isNotBlank() },
            totalInstitutions = students.map { it.idInstitucion }.distinct().count { it.isNotBlank() }
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        TeacherStudentsMetrics()
    )

    // 🎓 Available grade filters: 3 años, 4 años, 5 años + Firestore
    val availableFilters = domainStudents.map { students ->
        val firestoreGrades = students.map { it.grado }.distinct().sorted()

        listOf("3 años", "4 años", "5 años") +
                firestoreGrades.filterNot { it in listOf("3 años", "4 años", "5 años") }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSelectedFilter(filter: String?) {
        _selectedFilter.value = filter
    }

    fun selectStudent(studentId: String) {
        _uiState.update { it.copy(selectedStudentId = studentId) }
    }
}

data class TeacherStudentsUiState(
    val isLoading: Boolean = false,
    val selectedStudentId: String? = null,
    val error: String? = null
)

data class TeacherStudentsMetrics(
    val totalStudents: Int = 0,
    val totalGrades: Int = 0,
    val totalSections: Int = 0,
    val totalInstitutions: Int = 0
)
