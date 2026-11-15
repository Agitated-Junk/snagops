package com.sitesnag.tracker.sync

import android.content.Context
import androidx.work.*
import com.sitesnag.tracker.data.local.SiteSnagDatabase
import com.sitesnag.tracker.data.repository.SyncRepository
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val database = SiteSnagDatabase.getInstance(applicationContext)
        val syncRepo = SyncRepository(database.syncOutboxDao())
        val syncService = GoogleSyncService(applicationContext)

        val pending = syncRepo.getPendingQueue()
        if (pending.isEmpty()) {
            return Result.success()
        }

        var anyFailed = false
        for (item in pending) {
            when (val res = syncService.processOutboxItem(item)) {
                is SyncResult.Success -> {
                    syncRepo.markSynced(res.outboxId)
                }
                is SyncResult.Failure -> {
                    syncRepo.markFailed(res.outboxId)
                    anyFailed = true
                }
            }
        }

        // Clean up old synced records
        syncRepo.purgeSynced()

        // Two-way sync: Pull remote master data and observations for active project
        val activeProj = database.projectDao().getActiveProject()
        if (activeProj != null) {
            syncService.performTwoWaySync(activeProj.id)
        }

        return if (anyFailed) Result.retry() else Result.success()
    }
}

object SyncManager {

    private const val UNIQUE_PERIODIC_SYNC = "SnagOps_Periodic_Sync"
    private const val UNIQUE_MANUAL_SYNC = "SnagOps_Manual_Sync"

    fun schedulePeriodicSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            15, TimeUnit.MINUTES,
            5, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_PERIODIC_SYNC,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )
    }

    fun triggerManualSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val oneTimeRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_MANUAL_SYNC,
            ExistingWorkPolicy.REPLACE,
            oneTimeRequest
        )
    }

    fun getSyncWorkInfoFlow(context: Context): Flow<List<WorkInfo>> {
        return WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(UNIQUE_MANUAL_SYNC)
    }
}
