package com.sitesnag.tracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sitesnag.tracker.data.local.converter.Converters
import com.sitesnag.tracker.data.local.dao.*
import com.sitesnag.tracker.data.local.entity.*
import com.sitesnag.tracker.domain.model.CustomFieldType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ProjectEntity::class,
        BlockEntity::class,
        FloorEntity::class,
        SubLocationEntity::class,
        VendorEntity::class,
        CustomFieldDefinitionEntity::class,
        ObservationEntity::class,
        SyncOutboxEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SiteSnagDatabase : RoomDatabase() {

    abstract fun projectDao(): ProjectDao
    abstract fun locationDao(): LocationDao
    abstract fun vendorDao(): VendorDao
    abstract fun customFieldDao(): CustomFieldDao
    abstract fun observationDao(): ObservationDao
    abstract fun syncOutboxDao(): SyncOutboxDao

    companion object {
        @Volatile
        private var INSTANCE: SiteSnagDatabase? = null

        fun getInstance(context: Context): SiteSnagDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SiteSnagDatabase::class.java,
                    "sitesnag_tracker.db"
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        seedInitialData(database)
                    }
                }
            }
        }

        suspend fun seedInitialData(database: SiteSnagDatabase) {
            // ==========================================
            // PROJECT 1: Metro Residences - Tower 1 & 2
            // ==========================================
            val p1Id = database.projectDao().insertProject(
                ProjectEntity(
                    id = 1L,
                    projectName = "Metro Residences - Tower 1 & 2",
                    projectCode = "MR-2026",
                    clientContractorName = "Skyline Construction Corp / Apex Dev",
                    spreadsheetId = "1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms",
                    defaultCcEmails = "qaqc.lead@skyline.com, pm@skyline.com",
                    emailFrequency = "Daily",
                    isActive = true
                )
            )

            // Blocks for Project 1
            val p1TowerA = database.locationDao().insertBlock(BlockEntity(projectId = p1Id, name = "Tower A"))
            val p1TowerB = database.locationDao().insertBlock(BlockEntity(projectId = p1Id, name = "Tower B"))
            val p1Podium = database.locationDao().insertBlock(BlockEntity(projectId = p1Id, name = "Podium & Amenities"))
            val p1External = database.locationDao().insertBlock(BlockEntity(projectId = p1Id, name = "External / Civil Works"))

            // Floors under Tower A
            val fBasement = database.locationDao().insertFloor(FloorEntity(projectId = p1Id, blockId = p1TowerA, name = "Basement 1 - Parking"))
            val fGround = database.locationDao().insertFloor(FloorEntity(projectId = p1Id, blockId = p1TowerA, name = "Ground Floor Lobby"))
            val fLevel1 = database.locationDao().insertFloor(FloorEntity(projectId = p1Id, blockId = p1TowerA, name = "1st Floor Residential"))
            val fLevel2 = database.locationDao().insertFloor(FloorEntity(projectId = p1Id, blockId = p1TowerA, name = "2nd Floor Residential"))
            val fTerrace = database.locationDao().insertFloor(FloorEntity(projectId = p1Id, blockId = p1TowerA, name = "Terrace & Parapet"))

            // Floors under Tower B & Podium
            database.locationDao().insertFloor(FloorEntity(projectId = p1Id, blockId = p1TowerB, name = "Ground Floor"))
            database.locationDao().insertFloor(FloorEntity(projectId = p1Id, blockId = p1TowerB, name = "1st Floor"))
            database.locationDao().insertFloor(FloorEntity(projectId = p1Id, blockId = p1Podium, name = "Swimming Pool Deck"))
            database.locationDao().insertFloor(FloorEntity(projectId = p1Id, blockId = p1Podium, name = "Clubhouse"))

            // Sub-locations under Level 1 Tower A
            database.locationDao().insertSubLocation(SubLocationEntity(projectId = p1Id, floorId = fLevel1, name = "Flat 101 - Master Bedroom"))
            database.locationDao().insertSubLocation(SubLocationEntity(projectId = p1Id, floorId = fLevel1, name = "Flat 101 - Master Bath"))
            database.locationDao().insertSubLocation(SubLocationEntity(projectId = p1Id, floorId = fLevel1, name = "Flat 102 - Living Area"))
            database.locationDao().insertSubLocation(SubLocationEntity(projectId = p1Id, floorId = fLevel1, name = "Common Corridor - Grid B2"))
            database.locationDao().insertSubLocation(SubLocationEntity(projectId = p1Id, floorId = fLevel1, name = "Electrical Shaft #1"))

            // Vendors for Project 1
            database.vendorDao().insertVendor(
                VendorEntity(
                    projectId = p1Id,
                    name = "Apex Waterproofing Ltd",
                    trade = "Waterproofing",
                    primaryEmail = "site.manager@apexwaterproofing.com",
                    ccEmails = "operations@apexwaterproofing.com"
                )
            )
            database.vendorDao().insertVendor(
                VendorEntity(
                    projectId = p1Id,
                    name = "Prime MEP Engineers",
                    trade = "MEP / Plumbing & Electrical",
                    primaryEmail = "project.lead@primemep.com",
                    ccEmails = "qa@primemep.com"
                )
            )
            database.vendorDao().insertVendor(
                VendorEntity(
                    projectId = p1Id,
                    name = "Global Finishing & Cladding",
                    trade = "Finishing & Tiling",
                    primaryEmail = "site@globalfinishing.com",
                    ccEmails = "supervisor@globalfinishing.com"
                )
            )
            database.vendorDao().insertVendor(
                VendorEntity(
                    projectId = p1Id,
                    name = "BuildStrong Structural Works",
                    trade = "Structural / Concrete",
                    primaryEmail = "engineering@buildstrong.com",
                    ccEmails = "rebar@buildstrong.com"
                )
            )

            // Dynamic Custom Fields for Project 1
            database.customFieldDao().insertField(
                CustomFieldDefinitionEntity(
                    id = "p1_drawing_ref",
                    projectId = p1Id,
                    name = "Drawing / Sheet Reference",
                    type = CustomFieldType.TEXT,
                    isRequired = false,
                    orderIndex = 1
                )
            )
            database.customFieldDao().insertField(
                CustomFieldDefinitionEntity(
                    id = "p1_contract_pkg",
                    projectId = p1Id,
                    name = "Contract Package",
                    type = CustomFieldType.DROPDOWN,
                    optionsJson = "[\"Package A - Core & Shell\",\"Package B - MEP Works\",\"Package C - Architectural Finishes\"]",
                    isRequired = false,
                    orderIndex = 2
                )
            )
            database.customFieldDao().insertField(
                CustomFieldDefinitionEntity(
                    id = "p1_inspection_stage",
                    projectId = p1Id,
                    name = "Inspection Stage",
                    type = CustomFieldType.DROPDOWN,
                    optionsJson = "[\"Pre-Pour Inspection\",\"Post-Demolding\",\"First Fix\",\"Final Handover Punch\"]",
                    isRequired = false,
                    orderIndex = 3
                )
            )

            // ==========================================
            // PROJECT 2: Grand Bayfront Complex
            // ==========================================
            val p2Id = database.projectDao().insertProject(
                ProjectEntity(
                    id = 2L,
                    projectName = "Grand Bayfront Complex",
                    projectCode = "GBC-2026",
                    clientContractorName = "Bayfront Waterfront Properties",
                    spreadsheetId = "1GBC_Waterfront_Master_Register_2026",
                    defaultCcEmails = "site.qa@bayfront.com",
                    emailFrequency = "Weekly",
                    isActive = false
                )
            )

            // Blocks for Project 2
            val p2North = database.locationDao().insertBlock(BlockEntity(projectId = p2Id, name = "North Commercial Tower"))
            val p2South = database.locationDao().insertBlock(BlockEntity(projectId = p2Id, name = "South Residential Tower"))
            val p2Marina = database.locationDao().insertBlock(BlockEntity(projectId = p2Id, name = "Marina Promenade"))

            // Floors & Sublocations for Project 2
            val p2F1 = database.locationDao().insertFloor(FloorEntity(projectId = p2Id, blockId = p2North, name = "Ground Marina Plaza"))
            database.locationDao().insertFloor(FloorEntity(projectId = p2Id, blockId = p2North, name = "Level 10 - Office Suites"))
            database.locationDao().insertSubLocation(SubLocationEntity(projectId = p2Id, floorId = p2F1, name = "Retail Unit 104"))

            // Vendors for Project 2
            database.vendorDao().insertVendor(
                VendorEntity(
                    projectId = p2Id,
                    name = "Marine Civil Works LLC",
                    trade = "Marine & Foundations",
                    primaryEmail = "marine@coastalworks.com",
                    ccEmails = "director@coastalworks.com"
                )
            )
            database.vendorDao().insertVendor(
                VendorEntity(
                    projectId = p2Id,
                    name = "Coastal Glass & Façade",
                    trade = "Glazing & Curtain Wall",
                    primaryEmail = "qa@coastalglass.com",
                    ccEmails = "operations@coastalglass.com"
                )
            )

            // Custom fields for Project 2
            database.customFieldDao().insertField(
                CustomFieldDefinitionEntity(
                    id = "p2_marine_zone",
                    projectId = p2Id,
                    name = "Marine Exposure Category",
                    type = CustomFieldType.DROPDOWN,
                    optionsJson = "[\"Zone 1 - Tidal Splash\",\"Zone 2 - Atmospheric Marine\",\"Zone 3 - Submerged\"]",
                    isRequired = true,
                    orderIndex = 1
                )
            )
        }
    }
}
