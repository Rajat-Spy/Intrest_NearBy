package com.example.intrestnearby.model.googlePlaceModel

import com.squareup.moshi.Json

data class LocationModel(
    @field:Json(name = "lat")
    val lat: Double?,

    @field:Json(name = "lng")
    val lng: Double?
)