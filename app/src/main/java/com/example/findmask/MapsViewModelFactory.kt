package com.example.findmask

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.GoogleMap

class MapsViewModelFactory(private val mMap: GoogleMap): ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(MapsViewModel::class.java))
            return MapsViewModel(mMap) as T
        throw Exception()
    }
}