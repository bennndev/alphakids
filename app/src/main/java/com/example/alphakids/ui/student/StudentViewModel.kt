package com.example.alphakids.ui.student

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alphakids.data.firebase.models.Estudiante
import com.example.alphakids.data.firebase.models.Usuario
import com.example.alphakids.domain.usecases.CreateStudentUseCase
import com.example.alphakids.domain.usecases.GetCurrentUserUseCase
import com.example.alphakids.domain.usecases.GetStudentsUseCase
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class StudentViewModel @Inject constructor(
    private val createStudentUseCase: CreateStudentUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getStudentsUseCase: GetStudentsUseCase,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _createUiState = MutableStateFlow<StudentUiState>(StudentUiState.Idle)
    val createUiState: StateFlow<StudentUiState> = _createUiState.asStateFlow()

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

    private val _filteredDocentes = MutableStateFlow<List<Usuario>>(emptyList())
    val filteredDocentes: StateFlow<List<Usuario>> = _filteredDocentes.asStateFlow()

    init {
        loadInstitutions()
    }

    private fun loadInstitutions() {
        viewModelScope.launch {
            try {
                Log.d("StudentViewModel", "Loading institutions...")
                val snapshot = firestore.collection("instituciones")
                    .get()
                    .await()
                
                Log.d("StudentViewModel", "Found ${snapshot.size()} institutions")

                val institutionsList = snapshot.documents.mapNotNull { doc ->
                    try {
                        val id = doc.id
                        val nombre = doc.getString("nombre") ?: ""
                        val direccion = doc.getString("direccion") ?: ""
                        val telefono = doc.getString("telefono") ?: ""
                        val correo = doc.getString("correo") ?: ""
                        val fechaCreacion = doc.getTimestamp("fecha_creacion")?.toDate()?.time

                        Log.d("StudentViewModel", "Institution found: $nombre ($id)")

                        com.example.alphakids.domain.models.Institution(
                            id = id,
                            nombre = nombre,
                            direccion = direccion,
                            telefono = telefono,
                            correo = correo,
                            fechaCreacionMillis = fechaCreacion
                        )
                    } catch (e: Exception) {
                        Log.e("StudentViewModel", "Error parsing institution: ${doc.id}", e)
                        null
                    }
                }
                _institutions.value = institutionsList
            } catch (e: Exception) {
                Log.e("StudentViewModel", "Error loading institutions", e)
                _institutions.value = emptyList()
            }
        }
    }

    private fun loadGrades(institutionId: String) {
        Log.d("StudentViewModel", "Loading grades for institution: $institutionId")
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("instituciones")
                    .document(institutionId)
                    .collection("grado") // Verified: collection name is "grado"
                    //.orderBy("orden")
                    .get()
                    .await()

                Log.d("StudentViewModel", "Found ${snapshot.size()} grades")

                val gradesList = snapshot.documents.mapNotNull { doc ->
                    try {
                        val id = doc.id
                        val nombre = doc.getString("nombre") ?: ""
                        val nivel = doc.getString("nivel") ?: ""
                        val orden = doc.getLong("orden")?.toInt() ?: 0

                        Log.d("StudentViewModel", "Grade found: $nombre ($id), Order: $orden")

                        com.example.alphakids.domain.models.Grade(
                            id = id,
                            name = nombre,
                            level = nivel,
                            order = orden
                        )
                    } catch (e: Exception) {
                        Log.e("StudentViewModel", "Error parsing grade: ${doc.id}", e)
                        null
                    }
                }
                _grades.value = gradesList.sortedBy { it.order }
            } catch (e: Exception) {
                Log.e("StudentViewModel", "Error loading grades", e)
                _grades.value = emptyList()
            }
        }
    }

    private fun loadSections(gradeId: String) {
        val institutionId = _selectedInstitution.value?.id ?: return
        Log.d("StudentViewModel", "Loading sections for grade: $gradeId in institution: $institutionId")
        
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("instituciones")
                    .document(institutionId)
                    .collection("grado")
                    .document(gradeId)
                    .collection("secciones")
                    .get()
                    .await()

                Log.d("StudentViewModel", "Found ${snapshot.size()} sections")

                val sectionsList = snapshot.documents.mapNotNull { doc ->
                    try {
                        val id = doc.id
                        val codigo = doc.getString("codigo") ?: ""

                        Log.d("StudentViewModel", "Section found: $codigo ($id)")

                        com.example.alphakids.domain.models.Section(
                            id = id,
                            code = codigo
                        )
                    } catch (e: Exception) {
                        Log.e("StudentViewModel", "Error parsing section: ${doc.id}", e)
                        null
                    }
                }
                _sections.value = sectionsList
            } catch (e: Exception) {
                Log.e("StudentViewModel", "Error loading sections", e)
                _sections.value = emptyList()
            }
        }
    }

    fun selectInstitution(institution: com.example.alphakids.domain.models.Institution) {
        Log.d("StudentViewModel", "Selected institution: ${institution.nombre} (${institution.id})")
        _selectedInstitution.value = institution
        _selectedGrade.value = null
        _selectedSection.value = null
        _grades.value = emptyList()
        _sections.value = emptyList()
        _filteredDocentes.value = emptyList()
        loadGrades(institution.id)
    }

    fun selectGrade(grade: com.example.alphakids.domain.models.Grade) {
        Log.d("StudentViewModel", "Selected grade: ${grade.name} (${grade.id})")
        _selectedGrade.value = grade
        _selectedSection.value = null
        _sections.value = emptyList()
        _filteredDocentes.value = emptyList()
        loadSections(grade.id)
    }

    fun selectSection(section: com.example.alphakids.domain.models.Section) {
        Log.d("StudentViewModel", "Selected section: ${section.code} (${section.id})")
        _selectedSection.value = section
        loadDocentesForSection()
    }

    private fun loadDocentesForSection() {
        val institutionId = _selectedInstitution.value?.id ?: return
        val gradeName = _selectedGrade.value?.name ?: return
        val sectionCode = _selectedSection.value?.code ?: return

        viewModelScope.launch {
            try {

                val docentesQuery = firestore.collection("docentes")
                    .whereEqualTo("idInstitucion", institutionId)
                    .whereEqualTo("grado", gradeName)
                    .whereEqualTo("seccion", sectionCode)
                    .get()
                    .await()

                val docenteIds = docentesQuery.documents.map { it.id }

                if (docenteIds.isEmpty()) {
                    _filteredDocentes.value = emptyList()
                    return@launch
                }

                val usersQuery = firestore.collection("usuarios")
                    .whereIn(FieldPath.documentId(), docenteIds)
                    .get()
                    .await()

                val users = usersQuery.toObjects(Usuario::class.java)
                _filteredDocentes.value = users

            } catch (e: Exception) {
                Log.e("StudentViewModel", "Error loading filtered docentes", e)
                _filteredDocentes.value = emptyList()
            }
        }
    }


    private val tutorIdFlow: Flow<String?> = getCurrentUserUseCase()
        .map { it?.uid }

    @OptIn(ExperimentalCoroutinesApi::class)
    val students: StateFlow<List<Estudiante>> = tutorIdFlow
        .flatMapLatest { tutorId ->
            if (tutorId != null) {
                getStudentsUseCase(tutorId)
                    .catch { e ->
                        Log.e("StudentViewModel", "Error fetching students", e)
                        emit(emptyList())
                    }
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun createStudent(
        nombre: String,
        apellido: String,
        edad: Int,
        grado: String,
        seccion: String,
        idInstitucion: String,
        idDocente: String?
    ) {
        viewModelScope.launch {
            _createUiState.value = StudentUiState.Loading

            val currentUser = getCurrentUserUseCase().firstOrNull()
            if (currentUser == null) {
                _createUiState.value = StudentUiState.Error("No se pudo obtener el usuario actual.")
                return@launch
            }
            val tutorId = currentUser.uid

            val nuevoEstudiante = Estudiante(
                nombre = nombre,
                apellido = apellido,
                edad = edad,
                grado = grado,
                seccion = seccion,
                idTutor = tutorId,
                idInstitucion = idInstitucion,
                idDocente = idDocente ?: "",
                fotoPerfil = null
            )

            Log.d("StudentViewModel", "Intentando crear estudiante: ${nuevoEstudiante.nombre} con idTutor: $tutorId")

            val result = createStudentUseCase(nuevoEstudiante)

            if (result.isSuccess) {
                _createUiState.value = StudentUiState.Success(result.getOrNull() ?: "unknown_id")
            } else {
                _createUiState.value = StudentUiState.Error(
                    result.exceptionOrNull()?.message ?: "Error desconocido al crear perfil."
                )
            }
        }
    }

    fun resetCreateState() {
        _createUiState.value = StudentUiState.Idle
    }
}
