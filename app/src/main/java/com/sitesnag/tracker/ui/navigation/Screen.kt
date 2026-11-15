package com.sitesnag.tracker.ui.navigation

sealed class Screen(val route: String, val title: String) {
    object Capture : Screen("capture", "Quick Capture")
    object Observations : Screen("observations", "Snag Register")
    object Ncrs : Screen("ncrs", "NCR Register")
    object Dashboard : Screen("dashboard", "Dashboard & Export")
    object Settings : Screen("settings", "Project Settings")
    object ProjectsManagement : Screen("projects_management", "Manage Projects")
    object LocationSetup : Screen("location_setup", "Location Hierarchy")
    object VendorSetup : Screen("vendor_setup", "Vendor Directory")
    object CustomFieldsSetup : Screen("custom_fields_setup", "Custom Fields")
}
