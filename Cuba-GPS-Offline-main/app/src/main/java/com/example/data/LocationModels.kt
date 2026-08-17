package com.example.data

enum class PoiCategory(val displayName: String, val iconName: String) {
    GASOLINERA("Servicentro / CUPET", "local_gas_station"),
    HOSPITAL("Hospital / Policlínico", "local_hospital"),
    HOTEL_CAMPISMO("Hotel / Campismo", "hotel"),
    TALLER_PONCHERA("Taller / Ponchera", "build"),
    FARMACIA("Farmacia", "local_pharmacy"),
    TRANSPORTE("Terminal / Viazul / Tren", "directions_bus"),
    TURISMO("Punto de Interés / Turismo", "attractions"),
    BANCO_CAJERO("Banco / Cadeca / Cajero", "account_balance"),
    GASTRONOMIA("Restaurante / Paladar", "restaurant")
}

enum class TurnType {
    START,
    STRAIGHT,
    SLIGHT_RIGHT,
    RIGHT,
    SHARP_RIGHT,
    SLIGHT_LEFT,
    LEFT,
    SHARP_LEFT,
    UTURN,
    ROUNDABOUT,
    DESTINATION
}

data class GeoPoint(
    val lat: Double,
    val lon: Double,
    val name: String = "",
    val altitude: Double = 0.0
) {
    /**
     * Calculates distance to another point in meters using the Haversine formula
     */
    fun distanceTo(other: GeoPoint): Double {
        val earthRadius = 6371000.0 // meters
        val dLat = Math.toRadians(other.lat - this.lat)
        val dLon = Math.toRadians(other.lon - this.lon)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(this.lat)) * Math.cos(Math.toRadians(other.lat)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return earthRadius * c
    }

    /**
     * Calculates bearing in degrees from this point to destination
     */
    fun bearingTo(other: GeoPoint): Float {
        val lat1 = Math.toRadians(this.lat)
        val lon1 = Math.toRadians(this.lon)
        val lat2 = Math.toRadians(other.lat)
        val lon2 = Math.toRadians(other.lon)

        val dLon = lon2 - lon1
        val y = Math.sin(dLon) * Math.cos(lat2)
        val x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon)
        val radians = Math.atan2(y, x)
        val degrees = Math.toDegrees(radians).toFloat()
        return (degrees + 360) % 360
    }
}

data class PointOfInterest(
    val id: String,
    val name: String,
    val category: PoiCategory,
    val province: String,
    val lat: Double,
    val lon: Double,
    val address: String,
    val phone: String = "",
    val description: String = ""
) {
    fun toGeoPoint(): GeoPoint = GeoPoint(lat, lon, name)
}

data class RoadSegment(
    val id: String,
    val name: String,
    val highwayType: String, // "motorway", "primary", "secondary", "tertiary", "residential"
    val speedLimit: Int, // km/h
    val points: List<GeoPoint>
)

data class RouteStep(
    val id: String,
    val instruction: String,
    val roadName: String,
    val distanceMeters: Double,
    val turnType: TurnType,
    val startPoint: GeoPoint,
    val endPoint: GeoPoint
)

data class RouteResult(
    val origin: GeoPoint,
    val destination: GeoPoint,
    val totalDistanceKm: Double,
    val estimatedTimeMinutes: Int,
    val polyline: List<GeoPoint>,
    val steps: List<RouteStep>,
    val routeName: String,
    val primaryRoads: List<String> = emptyList()
)

data class SavedPlace(
    val id: String,
    val title: String,
    val subtitle: String,
    val lat: Double,
    val lon: Double,
    val category: PoiCategory,
    val isFavorite: Boolean = true
)

data class OfflineMapPackage(
    val id: String,
    val name: String,
    val fileName: String,
    val sizeMb: Double,
    val versionDate: String,
    val isDownloaded: Boolean,
    val isLoadedInEngine: Boolean,
    val format: String, // ".osm.pbf", ".mbtiles", ".map"
    val description: String
)
