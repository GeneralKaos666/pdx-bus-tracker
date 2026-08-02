package com.trimettransit.tracker.model

import android.os.Parcel
import android.os.Parcelable

data class Detour(
    var id: Int = 0,
    var desc: String = "",
    var routes: List<Int> = emptyList()
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.createIntArray()?.toList() ?: emptyList()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(desc)
        parcel.writeIntArray(routes.toIntArray())
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
