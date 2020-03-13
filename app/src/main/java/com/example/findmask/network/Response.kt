package com.example.findmask.network

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Response(
    val features: List<Features>
): Parcelable