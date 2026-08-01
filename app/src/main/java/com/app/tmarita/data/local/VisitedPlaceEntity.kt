package com.app.tmarita.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "visited_places")
data class VisitedPlaceEntity(
    @PrimaryKey val regionId: String,
    val visitedAt: Long,
    val note: String? = null,
    val photoUri: String? = null
)
