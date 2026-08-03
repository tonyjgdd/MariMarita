package com.app.tmarita.model

data class VisitedPlace(
    val regionId: String,
    val visited: Boolean = true,
    val visitedAt: Long? = null,
    val place: String? = null,
    val driveLink: String? = null,
    val note: String? = null,
    val photoUri: String? = null
)