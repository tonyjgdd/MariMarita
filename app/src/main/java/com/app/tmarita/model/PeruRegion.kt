package com.app.tmarita.model

/**
 * Un departamento (o territorio especial) del mapa. Geometría pura, no cambia.
 * id sigue ISO 3166-2, ej: "PE-LIM" = Lima, "PE-CUS" = Cusco.
 */
data class PeruRegion(
    val id: String,
    val title: String,
    val pathData: String
)
