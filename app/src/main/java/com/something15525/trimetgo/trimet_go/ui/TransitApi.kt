package com.something15525.trimetgo.trimet_go.ui

import android.content.Context
import android.util.Log
import com.something15525.trimetgo.trimet_go.data.model.Direction
import com.something15525.trimetgo.trimet_go.data.model.Route
import com.something15525.trimetgo.trimet_go.data.model.Stop
import com.something15525.trimetgo.trimet_go.network.JSONParser
import com.something15525.trimetgo.trimet_go.util.ConnectionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object TransitApi {
    private const val TAG = "TransitApi"
    private val parser = JSONParser()

    suspend fun fetchRoutes(context: Context, url: String): List<Route>? = withContext(Dispatchers.IO) {
        if (!ConnectionUtils.isOnline(context)) return@withContext null
        try {
            val json = parser.fetch(url) ?: return@withContext null
            val routes = mutableListOf<Route>()
            val arr = json.getJSONObject("resultSet").getJSONArray("route")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val route = Route().apply {
                    desc = obj.getString("desc")
                    routeId = obj.getInt("route")
                    setType(obj.getString("type"), obj.getString("desc"))
                    setStreetcarType(desc, obj.getString("type"))
                }
                if (route.desc != "Portland Aerial Tram") {
                    routes.add(route)
                }
            }
            routes
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch routes", e)
            null
        }
    }

    suspend fun fetchDirections(context: Context, url: String): List<Direction>? = withContext(Dispatchers.IO) {
        if (!ConnectionUtils.isOnline(context)) return@withContext null
        try {
            val json = parser.fetch(url) ?: return@withContext null
            val dirs = mutableListOf<Direction>()
            val routeObj = json.getJSONObject("resultSet").getJSONArray("route").getJSONObject(0)
            val routeDesc = routeObj.optString("desc", "")
            val routeId = routeObj.optInt("route", 0)
            val routeType = routeObj.optString("type", "")
            val route = Route().apply {
                desc = routeDesc
                this.routeId = routeId
                setType(routeType, routeDesc)
                setStreetcarType(routeDesc, routeType)
            }
            val arr = routeObj.getJSONArray("dir")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val dir = Direction().apply {
                    this.dir = obj.getInt("dir")
                    desc = obj.optString("desc", "")
                    this.route = route
                }
                dirs.add(dir)
            }
            dirs
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch directions", e)
            null
        }
    }

    suspend fun fetchStops(context: Context, url: String): List<Stop>? = withContext(Dispatchers.IO) {
        if (!ConnectionUtils.isOnline(context)) return@withContext null
        try {
            val json = parser.fetch(url) ?: return@withContext null
            val resultSet = json.optJSONObject("resultSet") ?: return@withContext null
            val routeArr = resultSet.optJSONArray("route")
            if (routeArr == null || routeArr.length() == 0) return@withContext null

            val route0 = routeArr.getJSONObject(0)
            val dirArr = route0.optJSONArray("dir")
            if (dirArr == null || dirArr.length() == 0) return@withContext null

            val dir0 = dirArr.getJSONObject(0)
            val stopArr = dir0.optJSONArray("stop")
            if (stopArr == null || stopArr.length() == 0) return@withContext emptyList()

            val routeDesc = route0.optString("desc", "")
            val routeId = route0.optInt("route", 0)
            val routeType = route0.optString("type", "")
            val route = Route().apply {
                desc = routeDesc
                this.routeId = routeId
                setType(routeType, routeDesc)
                setStreetcarType(routeDesc, routeType)
            }
            val stops = mutableListOf<Stop>()
            for (i in 0 until stopArr.length()) {
                val obj = stopArr.getJSONObject(i)
                val stop = Stop().apply {
                    desc = obj.optString("desc", "")
                    val dirField = obj.optString("dir", "")
                    if (dirField == "") {
                        dirDesc = context.getString(com.something15525.trimetgo.trimet_go.R.string.stop_bidirectional_text)
                    } else {
                        dirDesc = dirField
                    }
                    locId = obj.optInt("locid", 0)
                    latitude = obj.optDouble("lat", 0.0)
                    longitude = obj.optDouble("lng", 0.0)
                    addRoute(route)
                    computeTransitType()
                }
                stops.add(stop)
            }
            stops
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch stops", e)
            null
        }
    }
}
