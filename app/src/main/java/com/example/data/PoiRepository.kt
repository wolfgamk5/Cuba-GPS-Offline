package com.example.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File

/**
 * Busca puntos de interés reales de Cuba (hospitales, gasolineras, hoteles, restaurantes,
 * farmacias, etc.) extraídos de OpenStreetMap, en vez de la lista curada de 38 lugares de
 * [CubaGeographyData]. La base `pois.sqlite` se genera en la nube (GitHub Actions, ver
 * SETUP_MAPA_OFFLINE.md) a partir del mismo `cuba-latest.osm.pbf`, y se descarga al
 * teléfono junto con `cuba.mbtiles` y `graph-cache` (Fase 3).
 *
 * Si el archivo no existe todavía, [isAvailable] es false y la pantalla que llama a esto
 * debe usar [CubaGeographyData.POI_DATABASE] como respaldo — mismo patrón que el resto de
 * la app (mapa y rutas).
 */
class PoiRepository(context: Context) {

    private val dbFile = File(context.getExternalFilesDir(null), "maps/pois.sqlite")
    private var db: SQLiteDatabase? = null

    val isAvailable: Boolean get() = dbFile.exists() && dbFile.length() > 0

    /** Abre la base en modo lectura. Operación de disco — llamar fuera del hilo principal. */
    fun open(): Boolean {
        if (!isAvailable) return false
        return try {
            db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            true
        } catch (e: Exception) {
            db = null
            false
        }
    }

    /**
     * Busca por nombre (contiene, sin distinguir mayúsculas/acentos exactos) y, opcionalmente,
     * por categoría. Llamar fuera del hilo principal (consulta de disco).
     */
    fun search(query: String, category: PoiCategory?, limit: Int = 150): List<PointOfInterest> {
        val database = db ?: return emptyList()
        val whereParts = mutableListOf<String>()
        val args = mutableListOf<String>()

        if (query.isNotBlank()) {
            whereParts += "name LIKE ? COLLATE NOCASE"
            args += "%$query%"
        }
        if (category != null) {
            whereParts += "category = ?"
            args += category.name
        }
        val where = if (whereParts.isEmpty()) "" else "WHERE " + whereParts.joinToString(" AND ")

        val sql = "SELECT id, name, category, lat, lon, address FROM poi $where ORDER BY name LIMIT $limit"
        val results = mutableListOf<PointOfInterest>()
        database.rawQuery(sql, args.toTypedArray()).use { cursor ->
            val idxId = cursor.getColumnIndexOrThrow("id")
            val idxName = cursor.getColumnIndexOrThrow("name")
            val idxCategory = cursor.getColumnIndexOrThrow("category")
            val idxLat = cursor.getColumnIndexOrThrow("lat")
            val idxLon = cursor.getColumnIndexOrThrow("lon")
            val idxAddress = cursor.getColumnIndexOrThrow("address")
            while (cursor.moveToNext()) {
                val categoryEnum = runCatching { PoiCategory.valueOf(cursor.getString(idxCategory)) }.getOrNull()
                    ?: continue
                results += PointOfInterest(
                    id = "osm_${cursor.getLong(idxId)}",
                    name = cursor.getString(idxName),
                    category = categoryEnum,
                    province = "",
                    lat = cursor.getDouble(idxLat),
                    lon = cursor.getDouble(idxLon),
                    address = cursor.getString(idxAddress) ?: ""
                )
            }
        }
        return results
    }

    fun close() {
        db?.close()
        db = null
    }
}
