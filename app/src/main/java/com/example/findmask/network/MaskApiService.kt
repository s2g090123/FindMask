package com.example.findmask.network

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import kotlinx.coroutines.Deferred
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

private const val url = "https://raw.githubusercontent.com/kiang/pharmacies/master/json/"
private val retrofit = Retrofit.Builder().baseUrl(url).addConverterFactory(GsonConverterFactory.create())
    .addCallAdapterFactory(CoroutineCallAdapterFactory()).build()

interface MaskApiService {
    @GET("points.json")
    fun getProperties(): Deferred<Response>
}

object MaskApi {
    val retrofitService: MaskApiService by lazy {
        retrofit.create(MaskApiService::class.java)
    }
}