package com.example.maps

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.util.zip.GZIPInputStream

/**
 * Servidor HTTP local (127.0.0.1) que expone un archivo .mbtiles (vector tiles, formato
 * estándar generado por Planetiler/Tilemaker a partir de datos OpenStreetMap) como un
 * endpoint tipo {z}/{x}/{y}.pbf que MapLibre puede consumir como fuente "vector".
 *
 * Esto permite que el mapa funcione 100% offline: el .mbtiles se genera UNA VEZ a partir del
 * extracto OSM de Cuba (ver /SETUP_MAPA_OFFLINE.md) y se guarda en el almacenamiento del
 * dispositivo. No requiere conexión a internet en tiempo de uso.
 *
 * Coloca el archivo en: context.getExternalFilesDir(null)/maps/cuba.mbtiles
 * (o descárgalo la primera vez que el usuario tenga internet, y cachéalo ahí).
 */
class MbtilesTileServer(
    private val context: Context,
    port: Int = 8085,
) : NanoHTTPD("127.0.0.1", port) {

    private var db: SQLiteDatabase? = null

    val mbtilesFile: File
        get() = File(context.getExternalFilesDir(null), "maps/cuba.mbtiles")

    fun isMapDataAvailable(): Boolean = mbtilesFile.exists() && mbtilesFile.length() > 0

    fun openDatabase() {
        if (!isMapDataAvailable()) {
            Log.w("MbtilesServer", "cuba.mbtiles no encontrado en ${mbtilesFile.absolutePath}")
            return
        }
        db = SQLiteDatabase.openDatabase(mbtilesFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
    }

    /** Metadata del mbtiles: nombre, bounds, minzoom/maxzoom, formato, json (capas). */
    fun readMetadata(): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val cursor = db?.rawQuery("SELECT name, value FROM metadata", null) ?: return result
        cursor.use {
            while (it.moveToNext()) {
                result[it.getString(0)] = it.getString(1)
            }
        }
        return result
    }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri.trim('/') // esperado: "{z}/{x}/{y}.pbf"
        val parts = uri.removeSuffix(".pbf").split("/")
        if (parts.size != 3) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Ruta inválida")
        }
        val z = parts[0].toIntOrNull()
        val x = parts[1].toIntOrNull()
        // MBTiles usa esquema TMS (Y invertido respecto a XYZ estándar que usa MapLibre/Google)
        val yXyz = parts[2].toIntOrNull()
        if (z == null || x == null || yXyz == null) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Coordenadas inválidas")
        }
        val yTms = (1 shl z) - 1 - yXyz

        val database = db ?: return newFixedLengthResponse(
            Response.Status.INTERNAL_ERROR, "text/plain", "Mapa offline no cargado"
        )

        val cursor = database.rawQuery(
            "SELECT tile_data FROM tiles WHERE zoom_level = ? AND tile_column = ? AND tile_row = ?",
            arrayOf(z.toString(), x.toString(), yTms.toString())
        )
        cursor.use {
            if (it.moveToFirst()) {
                var bytes = it.getBlob(0)
                // Los tiles en mbtiles suelen venir comprimidos con gzip
                if (bytes.size > 2 && bytes[0] == 0x1f.toByte() && bytes[1] == 0x8b.toByte()) {
                    bytes = GZIPInputStream(bytes.inputStream()).readBytes()
                }
                val response = newFixedLengthResponse(
                    Response.Status.OK, "application/x-protobuf", bytes.inputStream(), bytes.size.toLong()
                )
                response.addHeader("Content-Encoding", "identity")
                response.addHeader("Access-Control-Allow-Origin", "*")
                return response
            }
        }
        // Tile vacío fuera de los datos disponibles: devolver 204 en vez de error
        return newFixedLengthResponse(Response.Status.NO_CONTENT, "application/x-protobuf", "")
    }

    fun stopServer() {
        stop()
        db?.close()
        db = null
    }
}
