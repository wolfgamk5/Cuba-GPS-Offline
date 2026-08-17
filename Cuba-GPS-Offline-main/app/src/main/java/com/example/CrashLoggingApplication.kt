package com.example

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import org.maplibre.android.MapLibre
import org.maplibre.android.WellKnownTileServer
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Application personalizada TEMPORAL para diagnosticar el cierre inesperado al arrancar.
 * Captura cualquier excepción no controlada (crash) y la escribe como archivo .txt en la
 * carpeta pública de Descargas del teléfono, para poder verla con cualquier explorador de
 * archivos sin necesidad de ADB, logcat, ni conexión a una computadora.
 *
 * El manejador se instala en attachBaseContext (no en onCreate) porque Firebase y otras
 * librerías corren ContentProviders ANTES de Application.onCreate; si el crash ocurre ahí,
 * onCreate nunca llega a ejecutarse.
 *
 * Una vez resuelto el problema de arranque, esta clase y su registro en AndroidManifest.xml
 * (android:name=".CrashLoggingApplication") se pueden eliminar sin afectar el resto de la app.
 */
class CrashLoggingApplication : Application() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        instalarManejador(base)
    }

    override fun onCreate() {
        super.onCreate()
        // MapLibre EXIGE que esto se llame antes de crear cualquier MapView. Hacerlo aquí,
        // una sola vez al arrancar la app, evita el crash "Using MapView requires calling
        // MapLibre.getInstance(...) before inflating or creating the view" — llamarlo dentro
        // de la pantalla del mapa no es confiable porque Compose puede componer (y crear el
        // MapView) en una subcomposición antes de que esa línea llegue a ejecutarse.
        MapLibre.getInstance(this, null, WellKnownTileServer.MapLibre)
    }

    private fun instalarManejador(context: Context) {
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val texto = buildString {
                    appendLine("Cuba GPS Offline - reporte de cierre inesperado")
                    appendLine("Fecha: ${Date()}")
                    appendLine("Dispositivo: ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
                    appendLine("Hilo: ${thread.name}")
                    appendLine()
                    append(sw.toString())
                }
                guardarEnDescargas(context, texto)
            } catch (e: Exception) {
                // Si falla el propio guardado, no bloqueamos el cierre normal de la app.
            }
            previousHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun guardarEnDescargas(context: Context, contenido: String) {
        val nombre = "cuba_gps_crash_" +
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".txt"

        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, nombre)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/")
            }
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        uri?.let {
            resolver.openOutputStream(it)?.use { out ->
                out.write(contenido.toByteArray())
            }
        }
    }
}
