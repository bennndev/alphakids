package com.example.alphakids.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alphakids.domain.models.User
import com.example.alphakids.domain.usecases.GetCurrentUserUseCase
import com.example.alphakids.domain.usecases.LoginUserUseCase
import com.example.alphakids.domain.usecases.LogoutUserUseCase
import com.example.alphakids.domain.usecases.RegisterUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val registerUserUseCase: RegisterUserUseCase,
    private val loginUserUseCase: LoginUserUseCase,
    private val logoutUserUseCase: LogoutUserUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    val currentUser: StateFlow<User?> = getCurrentUserUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _authUiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val authUiState: StateFlow<AuthUiState> = _authUiState.asStateFlow()

    // --- Cascading Dropdown States ---
    private val _institutions = MutableStateFlow<List<com.example.alphakids.domain.models.Institution>>(emptyList())
    val institutions: StateFlow<List<com.example.alphakids.domain.models.Institution>> = _institutions.asStateFlow()

    private val _grades = MutableStateFlow<List<com.example.alphakids.domain.models.Grade>>(emptyList())
    val grades: StateFlow<List<com.example.alphakids.domain.models.Grade>> = _grades.asStateFlow()

    private val _sections = MutableStateFlow<List<com.example.alphakids.domain.models.Section>>(emptyList())
    val sections: StateFlow<List<com.example.alphakids.domain.models.Section>> = _sections.asStateFlow()

    private val _selectedInstitution = MutableStateFlow<com.example.alphakids.domain.models.Institution?>(null)
    val selectedInstitution: StateFlow<com.example.alphakids.domain.models.Institution?> = _selectedInstitution.asStateFlow()

    private val _selectedGrade = MutableStateFlow<com.example.alphakids.domain.models.Grade?>(null)
    val selectedGrade: StateFlow<com.example.alphakids.domain.models.Grade?> = _selectedGrade.asStateFlow()

    private val _selectedSection = MutableStateFlow<com.example.alphakids.domain.models.Section?>(null)
    val selectedSection: StateFlow<com.example.alphakids.domain.models.Section?> = _selectedSection.asStateFlow()

    init {
        loadInstitutions()
    }

    private fun loadInstitutions() {
        // Mock Data
        _institutions.value = listOf(
            com.example.alphakids.domain.models.Institution("1", "Colegio San José", "Av. Principal 123", "555-0101", "sanjose@edu.pe", null),
            com.example.alphakids.domain.models.Institution("2", "Institución Educativa 2025", "Calle Los Pinos 456", "555-0102", "ie2025@edu.pe", null),
            com.example.alphakids.domain.models.Institution("3", "Colegio Innova", "Jr. Las Flores 789", "555-0103", "innova@edu.pe", null)
        )
    }

    fun selectInstitution(institution: com.example.alphakids.domain.models.Institution) {
        _selectedInstitution.value = institution
        _selectedGrade.value = null
        _selectedSection.value = null
        _grades.value = emptyList()
        _sections.value = emptyList()
        loadGrades(institution.id)
    }

    private fun loadGrades(institutionId: String) {
        // Mock Data - In a real app, this would fetch from Firestore subcollection of the institution
        // Simulating different grades for different institutions
        if (institutionId == "1") {
            _grades.value = listOf(
                com.example.alphakids.domain.models.Grade("g1", "3 años", "inicial", 1),
                com.example.alphakids.domain.models.Grade("g2", "4 años", "inicial", 2),
                com.example.alphakids.domain.models.Grade("g3", "5 años", "inicial", 3)
            )
        } else {
            _grades.value = listOf(
                com.example.alphakids.domain.models.Grade("g4", "1er Grado", "primaria", 1),
                com.example.alphakids.domain.models.Grade("g5", "2do Grado", "primaria", 2)
            )
        }
    }

    fun selectGrade(grade: com.example.alphakids.domain.models.Grade) {
        _selectedGrade.value = grade
        _selectedSection.value = null
        _sections.value = emptyList()
        loadSections(grade.id)
    }

    private fun loadSections(gradeId: String) {
        // Mock Data - In a real app, this would fetch from Firestore subcollection of the grade
        _sections.value = listOf(
            com.example.alphakids.domain.models.Section("s1", "A"),
            com.example.alphakids.domain.models.Section("s2", "B"),
            com.example.alphakids.domain.models.Section("s3", "C")
        )
    }

    fun selectSection(section: com.example.alphakids.domain.models.Section) {
        _selectedSection.value = section
    }

    fun register(
        nombre: String,
        apellido: String,
        email: String,
        clave: String,
        telefono: String,
        rol: String,
        idInstitucion: String = "",
        grado: String = "",
        seccion: String = ""
    ) {
        viewModelScope.launch {
            _authUiState.value = AuthUiState.Loading
            registerUserUseCase(nombre, apellido, email, clave, telefono, rol, idInstitucion, grado, seccion)
                .collect { result ->
                    if (result.isSuccess) {
                        _authUiState.value = AuthUiState.Success(result.getOrNull()!!)
                    } else {
                        _authUiState.value = AuthUiState.Error(
                            result.exceptionOrNull()?.message ?: "Error desconocido"
                        )
                    }
                }
        }
    }

    fun login(email: String, clave: String) {
        viewModelScope.launch {
            _authUiState.value = AuthUiState.Loading
            loginUserUseCase(email, clave)
                .collect { result ->
                    if (result.isSuccess) {
                        _authUiState.value = AuthUiState.Success(result.getOrNull()!!)
                    } else {
                        _authUiState.value = AuthUiState.Error(
                            result.exceptionOrNull()?.message ?: "Correo o contraseña incorrectos"
                        )
                    }
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            logoutUserUseCase()
        }
    }

    fun resetAuthState() {
        _authUiState.value = AuthUiState.Idle
    }
}
