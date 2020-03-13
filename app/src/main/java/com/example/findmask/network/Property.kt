package com.example.findmask.network

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Property(
    val name: String,
    val phone: String,
    val address: String,
    @SerializedName("mask_adult")
    val maskAdult: Int,
    @SerializedName("mask_child")
    val maskChild: Int,
    val updated: String
): Parcelable