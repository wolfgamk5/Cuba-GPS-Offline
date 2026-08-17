package com.example.download

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Estado expuesto a la UI del progreso de descarga del mapa/rutas reales de Cuba. */
sealed class MapDownloadStatus {
    data object NotStarted : MapDownloadStatus()
    data class Downloading(val phase: String, val percent: Int, val bytesDone: Long, val bytesTotal: Long) : MapDownloadStatus()
    data object Completed : MapDownloadStatus()
    data class Failed(val message: String) : MapDownloadStatus()
}

class MapDataRepository(private val context: Context) {

    private val workManager = WorkManager.getInstance(context)
    private val mapsDir = File(context.getExternalFilesDir(null), "maps")

    fun isMbtilesReady(): Boolean = File(mapsDir, "cuba.mbtiles").let { it.exists() && it.length() > 0 }

    fun isPoisReady(): Boolean = File(mapsDir, "pois.sqlite").let { it.exists() && it.length() > 0 }

    fun isGraphCacheReady(): Boolean {
        val dir = File(mapsDir, "graph-cache")
        return dir.exists() && dir.isDirectory && (dir.listFiles()?.isNotEmpty() == true)
    }

    /**
     * Inicia (o reanuda, si ya había una descarga a medias) la descarga del mapa 3D y de las
     * rutas reales. Por defecto solo corre con WiFi; [allowMeteredData] la permite también con
     * datos móviles (el usuario debe elegirlo explícitamente, ya que son cientos de MB).
     */
    fun startDownload(allowMeteredData: Boolean = false) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (allowMeteredData) NetworkType.CONNECTED else NetworkType.UNMETERED)
            .build()

        val request = OneTimeWorkRequestBuilder<MapDataDownloadWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        workManager.enqueueUniqueWork(MapDataDownloadWorker.WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }

    fun cancelDownload() {
        workManager.cancelUniqueWork(MapDataDownloadWorker.WORK_NAME)
    }

    /** Progreso combinado en tiempo real, listo para collectAsState() en Compose. */
    fun observeDownloadStatus(): Flow<MapDownloadStatus> =
        workManager.getWorkInfosForUniqueWorkFlow(MapDataDownloadWorker.WORK_NAME).map { infos ->
            val info = infos.firstOrNull() ?: return@map MapDownloadStatus.NotStarted
            when (info.state) {
                WorkInfo.State.RUNNING -> {
                    val data = info.progress
                    MapDownloadStatus.Downloading(
                        phase = data.getString(MapDataDownloadWorker.KEY_PHASE) ?: MapDataDownloadWorker.PHASE_MBTILES,
                        percent = data.getInt(MapDataDownloadWorker.KEY_PERCENT, 0),
                        bytesDone = data.getLong(MapDataDownloadWorker.KEY_BYTES_DONE, 0),
                        bytesTotal = data.getLong(MapDataDownloadWorker.KEY_BYTES_TOTAL, 0)
                    )
                }
                WorkInfo.State.SUCCEEDED -> MapDownloadStatus.Completed
                WorkInfo.State.FAILED -> MapDownloadStatus.Failed(
                    info.outputData.getString(MapDataDownloadWorker.KEY_ERROR) ?: "Error desconocido descargando el mapa"
                )
                WorkInfo.State.CANCELLED -> MapDownloadStatus.NotStarted
                WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED ->
                    MapDownloadStatus.Downloading(phase = "esperando_wifi", percent = 0, bytesDone = 0, bytesTotal = 0)
            }
        }
}
