package com.example.intrestnearby.interfaces

import com.example.intrestnearby.model.googlePlaceModel.GooglePlaceModel

interface NearLocationInterface {

    fun onSaveClick(googlePlaceModel: GooglePlaceModel)

    fun onDirectionClick(googlePlaceModel: GooglePlaceModel)
}