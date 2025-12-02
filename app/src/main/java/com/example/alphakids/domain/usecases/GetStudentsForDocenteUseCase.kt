package com.example.alphakids.domain.usecases

import com.example.alphakids.domain.repository.StudentRepository
import javax.inject.Inject

class GetStudentsForDocenteUseCase @Inject constructor(
    private val repository: StudentRepository
) {
    operator fun invoke(docenteId: String) = repository.getStudentsForDocente(docenteId)
}
