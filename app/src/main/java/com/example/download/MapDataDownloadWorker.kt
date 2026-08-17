package com.example.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.BuildConfig
import com.example.R
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

/**
 * Descarga cuba.mbtiles (mapa 3D real) y graph-cache.zip (rutas reales de GraphHopper) desde
 * las URLs configuradas en download.properties, y descomprime el segundo. Corre como trabajo
 * en primer plano (con notificación) porque son archivos grandes (cientos de MB) y el sistema
 * podría matar procesos en segundo plano antes de terminar.
 *
 * Reanuda descargas parciales con "Range" si la app se cierra o se pierde la conexión a
 * mitad de camino, en vez de volver a empezar desde cero.
 */
class MapDataDownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val mapsDir = File(applicationContext.getExternalFilesDir(null), "maps")

        override suspend fun doWork(): Result {
        setForeground(buildForegroundInfo("Preparando descarga del paquete completo de Cuba…", 0))
        mapsDir.mkdirs()

        // Cambiamos a una sola URL para el archivo ZIP unificado
        val fullPackageUrl = BuildConfig.CUBA_FULL_PACKAGE_URL // Asegúrate de definir esto en tu build.gradle.kts / download.properties
        if (fullPackageUrl.isBlank()) {
            return Result.failure(workDataOf(KEY_ERROR to "No hay URL configurada para el paquete único (download.properties vacío)"))
        }

        try {
            // 1. Descargar el ZIP único (con soporte de reanudación si se interrumpe)
            val zipFile = File(mapsDir, "cuba_full_package.zip.part")
            downloadWithResume(
                url = fullPackageUrl,
                destFile = zipFile,
                phase = PHASE_DOWNLOAD_ZIP
            )

            // 2. Descomprimir el paquete completo directamente en la carpeta 'maps'
            // El ZIP debe contener adentro: cuba.mbtiles, pois.sqlite y la carpeta graph-cache/
            unzip(zipFile, mapsDir, phase = PHASE_UNZIP)

            // 3. Limpiar el archivo ZIP temporal descargado
            zipFile.delete()

        } catch (e: Exception) {
            return Result.retry() // WorkManager reintentará cuando vuelva la red
        }

        return Result.success()
    }

    private suspend fun downloadWithResume(url: String, destFile: File, phase: String) {
        val partFile = if (destFile.name.endsWith(".part")) destFile else File(destFile.parentFile, "${destFile.name}.part")
        val alreadyDownloaded = if (partFile.exists()) partFile.length() else 0L

        val requestBuilder = Request.Builder().url(url)
        if (alreadyDownloaded > 0) {
            requestBuilder.header("Range", "bytes=$alreadyDownloaded-")
        }

        client.newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) throw java.io.IOException("HTTP ${response.code} descargando $url")

            val isResuming = response.code == 206 // 206 Partial Content
            val body = response.body ?: throw java.io.IOException("Respuesta vacía de $url")
            val contentLength = body.contentLength()
            val totalSize = if (isResuming) alreadyDownloaded + contentLength else contentLength

            RandomAccessFile(partFile, "rw").use { raf ->
                if (isResuming) raf.seek(alreadyDownloaded) else raf.setLength(0)
                body.byteStream().use { input ->
                    val buffer = ByteArray(64 * 1024)
                    var totalRead = if (isResuming) alreadyDownloaded else 0L
                    var lastReportedPercent = -1
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        raf.write(buffer, 0, read)
                        totalRead += read
                        val percent = if (totalSize > 0) ((totalRead * 100) / totalSize).toInt() else 0
                        if (percent != lastReportedPercent) {
                            lastReportedPercent = percent
                            reportProgress(phase, percent, totalRead, totalSize)
                        }
                    }
                }
            }
        }

        if (destFile != partFile) {
            partFile.renameTo(destFile)
        }
    }

    private suspend fun unzip(zipFile: File, outputDir: File, phase: String) {
        reportProgress(phase, 0, 0, 0)
        outputDir.mkdirs()
        ZipInputStream(zipFile.inputStream().buffered()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val outFile = File(outputDir, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    outFile.outputStream().use { out -> zis.copyTo(out) }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        reportProgress(phase, 100, 1, 1)
    }

    private suspend fun reportProgress(phase: String, percent: Int, bytesDone: Long, bytesTotal: Long) {
        setProgress(
            workDataOf(
                KEY_PHASE to phase,
                KEY_PERCENT to percent,
                KEY_BYTES_DONE to bytesDone,
                KEY_BYTES_TOTAL to bytesTotal
            )
        )
        val label = when (phase) {
            PHASE_MBTILES -> "Descargando mapa 3D de Cuba…"
            PHASE_GRAPH -> "Descargando datos de rutas…"
            PHASE_UNZIP -> "Preparando rutas offline…"
            PHASE_POIS -> "Descargando lugares de interés…"
            else -> "Descargando…"
        }
        setForeground(buildForegroundInfo("$label $percent%", percent))
    }

    private fun buildForegroundInfo(text: String, progressPercent: Int): ForegroundInfo {
        val channelId = "map_data_download"
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Descarga de mapas", NotificationManager.IMPORTANCE_LOW)
            manager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Cuba GPS Offline")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setProgress(100, progressPercent, false)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        const val WORK_NAME = "cuba_map_data_download"
        const val NOTIFICATION_ID = 4821

        const val KEY_PHASE = "phase"
        const val KEY_PERCENT = "percent"
        const val KEY_BYTES_DONE = "bytes_done"
        const val KEY_BYTES_TOTAL = "bytes_total"
        const val KEY_ERROR = "error"

        const val PHASE_DOWNLOAD_ZIP = "download_zip"
        const val PHASE_UNZIP = "unzip"
