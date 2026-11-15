package com.sitesnag.tracker

import android.app.Application
import com.sitesnag.tracker.data.local.SiteSnagDatabase
import com.sitesnag.tracker.data.repository.ObservationRepository
import com.sitesnag.tracker.data.repository.ProjectRepository
import com.sitesnag.tracker.data.repository.SyncRepository
import com.sitesnag.tracker.sync.SyncManager

class SiteSnagApplication : Application() {

    val database by lazy { SiteSnagDatabase.getInstance(this) }
    val projectRepository by lazy {
        ProjectRepository(
            database.projectDao(),
            database.locationDao(),
            database.vendorDao(),
            database.customFieldDao()
        )
    }
    val observationRepository by lazy {
        ObservationRepository(
            this,
            database.observationDao(),
            database.projectDao(),
            database.syncOutboxDao()
        )
    }
    val syncRepository by lazy {
        SyncRepository(database.syncOutboxDao())
    }

    override fun onCreate() {
        super.onCreate()
        // Initialize WorkManager background sync schedule with network connectivity constraint
        SyncManager.schedulePeriodicSync(this)
    }
}
