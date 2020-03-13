package com.example.findmask.network

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Geometry(
    val type: String,
    val coordinates: List<Double>
) : Parcelable