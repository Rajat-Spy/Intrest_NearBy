package com.example.intrestnearby.viewModels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.intrestnearby.model.googlePlaceModel.GooglePlaceModel
import com.example.intrestnearby.repo.AppRepo
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

class LocationViewModel : ViewModel() {

    private val repo = AppRepo()

    fun getNearByPlace(url: String) = repo.getPlaces(url)


}