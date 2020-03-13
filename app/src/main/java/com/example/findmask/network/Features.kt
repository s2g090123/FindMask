package com.example.findmask.network

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Features (
    val properties: Property,
    val geometry: Geometry
):Parcelable