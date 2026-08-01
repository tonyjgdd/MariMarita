package com.app.tmarita.model

/**
 * Representa la "visita" a un departamento: lo que ustedes vivieron ahí.
 * Esto vive aparte de PeruRegion (que es solo geometría) porque va a crecer:
 * fecha, nota, fotos, etc. mientras que PeruRegion no cambia.
 */
data class VisitedPlace(
    val regionId: String,      // ej: "PE-CUS", debe matchear PeruRegion.id
    val visitedAt: Long,       // epoch millis
    val note: String? = null,
    val photoUri: String? = null
)
