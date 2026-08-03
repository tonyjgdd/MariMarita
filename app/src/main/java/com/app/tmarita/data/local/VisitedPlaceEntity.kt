package com.app.tmarita.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "visited_places")
data class VisitedPlaceEntity(
    @PrimaryKey val regionId: String,
    val visited: Boolean = true,
    val visitedAt: Long? = null,   // fecha del viaje (antes era "cuándo se marcó"; ahora la elige el usuario)
    val place: String? = null,     // lugar / provincia dentro del departamento
    val driveLink: String? = null, // enlace de Drive con las fotos
    val note: String? = null,
    val photoUri: String? = null
)