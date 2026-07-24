package com.trimettransit.tracker.data.model

class Route {
    var desc: String = ""
    var routeId: Int = 0
    var isBus: Boolean = false
    var isMax: Boolean = false
    var isStreetcar: Boolean = false
    var isWes: Boolean = false

    val typeLetter: String
        get() = when {
            isWes -> "W"
            isMax -> "M"
            isBus -> "B"
            isStreetcar -> "S"
            else -> "Z"
        }

    fun setType(type: String, desc: String) {
        if (type == "R" && desc.contains("WES")) {
            isWes = true
            return
        }
        if (type == "R" && desc.contains("MAX")) {
            isMax = true
        } else if (type == "B") {
            isBus = true
        }
    }

    fun setStreetcarType(desc: String, type: String) {
        if (desc.contains("Portland Streetcar") && type == "R") {
            isStreetcar = true
        }
    }
}
