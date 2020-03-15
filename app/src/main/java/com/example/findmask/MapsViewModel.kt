package com.example.findmask

import android.util.Log
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.findmask.network.Features
import com.example.findmask.network.MaskApi
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MapsViewModel(private val mMap: GoogleMap): ViewModel() {

    private val job = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + job)
    var features = MutableLiveData<List<Features>>()
    private val _nowFeature = MutableLiveData<Features>()
    val nowFeature: LiveData<Features>
        get() = _nowFeature

    init {
        getData()
    }

    fun getData() {
        coroutineScope.launch {
            try {
                val response = MaskApi.retrofitService.getProperties()
                val data = response.await()
                features.value = data.features
            } catch (e: Exception) {
                Log.i("Mask", "error: ${e.message}")
            }
        }
    }

    private fun getMarkerColor(adult: Int, child: Int): Float {
        return if(adult < 20 || child < 20) BitmapDescriptorFactory.HUE_RED
        else if(adult < 50 || child < 50) BitmapDescriptorFactory.HUE_YELLOW
        else BitmapDescriptorFactory.HUE_GREEN
    }

    fun addMarker(features: List<Features>) {
        for((tag, feature) in features.withIndex()) {
            val latitude = feature.geometry.coordinates[1]
            val longitude = feature.geometry.coordinates[0]
            val title = feature.properties.name
            val snippet = "成人: ${feature.properties.maskAdult} / 小孩: ${feature.properties.maskChild}"
            val markerColor = getMarkerColor(feature.properties.maskAdult,feature.properties.maskChild)
            mMap.addMarker(
                MarkerOptions()
                    .position(LatLng(latitude,longitude))
                    .anchor(0.5f,0.5f)
                    .title(title)
                    .snippet(snippet)
                    .icon(BitmapDescriptorFactory.defaultMarker(markerColor))).tag = tag
        }
    }

    fun showMarkerInfo(tag: Int) {
        _nowFeature.value = features.value?.get(tag)
    }
}