package com.example.alphakids.data.firebase.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

data class Grado(
    @DocumentId
    val id: String = "",
    val nombre: String = "",
    val nivel: String = "",
    val estado: String = "",
    val orden: Int = 0,

    @PropertyName("creado_en")
    @ServerTimestamp
    val creadoEn: Timestamp? = null,

    @PropertyName("actualizado_en")
    @ServerTimestamp
    val actualizadoEn: Timestamp? = null
)
