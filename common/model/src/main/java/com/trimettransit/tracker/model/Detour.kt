package com.trimettransit.tracker.model

import android.os.Parcel
import android.os.Parcelable

data class Detour(
    val id: Int = 0,
    val desc: String = "",
    val routes: List<Int>? = null
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.createIntArray()?.toList()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(desc)
        parcel.writeIntArray(routes?.toIntArray() ?: intArrayOf())
    }

    override fun describeContents(): Int = 0

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<Detour> {
            override fun createFromParcel(parcel: Parcel): Detour = Detour(parcel)
            override fun newArray(size: Int): Array<Detour?> = arrayOfNulls(size)
        }
    }
}
