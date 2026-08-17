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
        setForeground(buildForegroundInfo("Preparando descarga de componentes offline...", 0))
        mapsDir.mkdirs()

        val mbtilesUrl = BuildConfig.CUBA_MBTILES_URL
        val graphUrl = BuildConfig.CUBA_GRAPH_CACHE_ZIP_URL
        val poisUrl = BuildConfig.CUBA_POIS_SQLITE_URL

        try {
            // 1. Descargar MBTiles (Mapa 3D)
            if (mbtilesUrl.isNotBlank()) {
                val mbtilesFile = File(mapsDir, "cuba.mbtiles.part")
                val finalMbtiles = File(mapsDir, "cuba.mbtiles")
                downloadWithResume(mbtilesUrl, mbtilesFile, finalMbtiles, PHASE_MBTILES)
            }

            // 2. Descargar Graph Cache (Rutas) y descomprimirlo
            if (graphUrl.isNotBlank()) {
                val zipFile = File(mapsDir, "graph-cache.zip.part")
                val finalZip = File(mapsDir, "graph-cache.zip")
                downloadWithResume(graphUrl, zipFile, finalZip, PHASE_GRAPH)
                
                // Descomprimir el caché de rutas en la carpeta correspondiente
                unzip(finalZip, mapsDir, PHASE_GRAPH)
                finalZip.delete()
            }

            // 3. Descargar POIs si estuviera configurado
            if (poisUrl.isNotBlank()) {
                val poisFile = File(mapsDir, "pois.sqlite.part")
                val finalPois = File(mapsDir, "pois.sqlite")
                downloadWithResume(poisUrl, poisFile, finalPois, PHASE_POIS)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }

        return Result.success()
    }

    private suspend fun downloadWithResume(url: String, partFile: File, destFile: File, phase: String) {
        val alreadyDownloaded = if (partFile.exists()) partFile.length() else 0L

        val requestBuilder = Request.Builder().url(url)
        if (alreadyDownloaded > 0) {
            requestBuilder.header("Range", "bytes=$alreadyDownloaded-")
        }

        client.newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful && response.code != 206) {
                throw java.io.IOException("HTTP ${response.code} descargando $url")
            }

            val isResuming = response.code == 206
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

        if (partFile.exists()) {
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
            PHASE_MBTILES -> "Descargando mapa 3D de Cuba..."
            PHASE_GRAPH -> "Descargando datos de rutas..."
            PHASE_POIS -> "Descargando lugares de interés..."
            else -> "Descargando..."
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

        const val PHASE_MBTILES = "mbtiles"
        const val PHASE_GRAPH = "graph"
        const val PHASE_POIS = "pois"
    }
}
