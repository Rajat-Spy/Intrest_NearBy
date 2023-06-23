package com.example.intrestnearby.repo

import android.util.Log
import com.example.intrestnearby.network.RetrofitClient
import com.example.intrestnearby.utility.State
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class AppRepo {
    fun getPlaces(url: String): Flow<State<Any>> = flow<State<Any>> {
        emit(State.loading(true))
        val response = RetrofitClient.retrofitApi.getNearByPlaces(url)

        Log.d("TAG", "getPlaces:  $response")
        if (response.body()?.googlePlaceModelList?.size!! > 0) {
            Log.d(
                "TAG",
                "getPlaces:  Success called ${response.body()?.googlePlaceModelList?.size}"
            )

            emit(State.success(response.body()!!))
        } else {
            Log.d("TAG", "getPlaces:  failed called")
            emit(State.failed(response.body()!!.error!!))
        }


    }.catch {
        emit(State.failed(it.message.toString()))
    }.flowOn(Dispatchers.IO)

//    fun getDirection(url: String): Flow<State<Any>> = flow<State<Any>> {
//        emit(State.loading(true))
//
//        val response = RetrofitClient.retrofitApi.getDirection(url)
//
//        if (response.body()?.directionRouteModels?.size!! > 0) {
//            emit(State.success(response.body()!!))
//        } else {
//            emit(State.failed(response.body()?.error!!))
//        }
//    }.flowOn(Dispatchers.IO)
//        .catch {
//            if (it.message.isNullOrEmpty()) {
//                emit(State.failed("No route found"))
//            } else {
//                emit(State.failed(it.message.toString()))
//            }
//
//        }



}