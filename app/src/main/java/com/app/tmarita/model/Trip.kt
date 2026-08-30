package com.app.tmarita.model

data class Trip(
    val id: Long = 0,
    val regionId: String,
    val place: String?,
    val visitDateMillis: Long?,
    val driveLink: String?,
    val notes: String?,
    val photoPath: String?

)