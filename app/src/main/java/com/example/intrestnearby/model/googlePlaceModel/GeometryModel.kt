package com.example.intrestnearby.model.googlePlaceModel

import com.squareup.moshi.Json

data class GeometryModel(
    @field:Json(name = "location")
    val location: LocationModel?
)