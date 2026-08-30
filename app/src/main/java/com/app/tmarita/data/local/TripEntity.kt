package com.app.tmarita.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val regionId: String,
    val place: String?,
    val visitDateMillis: Long?,
    val driveLink: String?,
    val notes: String?,
    val photoPath: String? = null
)