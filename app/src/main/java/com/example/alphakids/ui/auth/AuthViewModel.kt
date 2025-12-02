package com.example.alphakids.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alphakids.domain.models.User
import com.example.alphakids.domain.usecases.GetCurrentUserUseCase
import com.example.alphakids.domain.usecases.LoginUserUseCase
import com.example.alphakids.domain.usecases.LogoutUserUseCase
import com.example.alphakids.domain.usecases.RegisterUserUseCase
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val registerUserUseCase: RegisterUserUseCase,
    private val loginUserUseCase: LoginUserUseCase,
    private val logoutUserUseCase: LogoutUserUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    // Usuario actual
    val currentUser: StateFlow<User?> = getCurrentUserUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Estado de autenticación
    private val _authUiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val authUiState: StateFlow<AuthUiState> = _authUiState.asStateFlow()

    // ------------------------------
    // CASCADA FIRESTORE
    // ------------------------------

    private val _institutions =
        MutableStateFlow<List<com.example.alphakids.domain.models.Institution>>(emptyList())
    val institutions: StateFlow<List<com.example.alphakids.domain.models.Institution>> = _institutions

    private val _grades =
        MutableStateFlow<List<com.example.alphakids.domain.models.Grade>>(emptyList())
    val grades: StateFlow<List<com.example.alphakids.domain.models.Grade>> = _grades

    private val _sections =
        MutableStateFlow<List<com.example.alphakids.domain.models.Section>>(emptyList())
    val sections: StateFlow<List<com.example.alphakids.domain.models.Section>> = _sections

    private val _selectedInstitution =
        MutableStateFlow<com.example.alphakids.domain.models.Institution?>(null)
    val selectedInstitution: StateFlow<com.example.alphakids.domain.models.Institution?> =
        _selectedInstitution

    private val _selectedGrade =
        MutableStateFlow<com.example.alphakids.domain.models.Grade?>(null)
    val selectedGrade: StateFlow<com.example.alphakids.domain.models.Grade?> = _selectedGrade

    private val _selectedSection =
        MutableStateFlow<com.example.alphakids.domain.models.Section?>(null)
    val selectedSection: StateFlow<com.example.alphakids.domain.models.Section?> = _selectedSection

    init {
        loadInstitutions()
    }

    // ------------------------------
    // LOAD INSTITUTIONS
    // ------------------------------
    private fun loadInstitutions() {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("instituciones").get().await()

                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        com.example.alphakids.domain.models.Institution(
                            id = doc.id,
                            nombre = doc.getString("nombre") ?: "",
                            direccion = doc.getString("direccion") ?: "",
                            telefono = doc.getString("telefono") ?: "",
                            correo = doc.getString("correo") ?: "",
                            fechaCreacionMillis = doc.getTimestamp("fecha_creacion")
                                ?.toDate()?.time
                        )
                    } catch (e: Exception) {
                        Log.e("AuthViewModel", "Error parsing institution", e)
                        null
                    }
                }

                _institutions.value = list

            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error loading institutions", e)
            }
        }
    }

    fun selectInstitution(inst: com.example.alphakids.domain.models.Institution) {
        _selectedInstitution.value = inst
        _selectedGrade.value = null
        _selectedSection.value = null
        _grades.value = emptyList()
        _sections.value = emptyList()
        loadGrades(inst.id)
    }

    // ------------------------------
    // LOAD GRADES
    // ------------------------------
    private fun loadGrades(instId: String) {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("instituciones")
                    .document(instId)
                    .collection("grado")
                    .get()
                    .await()

                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        com.example.alphakids.domain.models.Grade(
                            id = doc.id,
                            name = doc.getString("nombre") ?: "",
                            level = doc.getString("nivel") ?: "",
                            order = doc.getLong("orden")?.toInt() ?: 0
                        )
                    } catch (e: Exception) {
                        Log.e("AuthViewModel", "Error parsing grade", e)
                        null
                    }
                }.sortedBy { it.order }

                _grades.value = list

            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error loading grades", e)
                _grades.value = emptyList()
            }
        }
    }

    fun selectGrade(grade: com.example.alphakids.domain.models.Grade) {
        _selectedGrade.value = grade
        _selectedSection.value = null
        _sections.value = emptyList()
        loadSections(grade.id)
    }

    // ------------------------------
    // LOAD SECTIONS
    // ------------------------------
    private fun loadSections(gradeId: String) {
        val instId = _selectedInstitution.value?.id ?: return

        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("instituciones")
                    .document(instId)
                    .collection("grado")
                    .document(gradeId)
                    .collection("secciones")
                    .get()
                    .await()

                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        com.example.alphakids.domain.models.Section(
                            id = doc.id,
                            code = doc.getString("codigo") ?: ""
                        )
                    } catch (e: Exception) {
                        Log.e("AuthViewModel", "Error parsing section", e)
                        null
                    }
                }

                _sections.value = list

            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error loading sections", e)
                _sections.value = emptyList()
            }
        }
    }

    fun selectSection(section: com.example.alphakids.domain.models.Section) {
        _selectedSection.value = section
    }

    // ------------------------------
    // REGISTER
    // ------------------------------
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
            registerUserUseCase(
                nombre,
                apellido,
                email,
                clave,
                telefono,
                rol,
                idInstitucion,
                grado,
                seccion
            ).collect { result ->
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

    // ------------------------------
    // LOGIN (RESTABLECIDO)
    // ------------------------------
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

    // ------------------------------
    // LOGOUT
    // ------------------------------
    fun logout() {
        viewModelScope.launch {
            logoutUserUseCase()
        }
    }

    fun resetAuthState() {
        _authUiState.value = AuthUiState.Idle
    }
}
