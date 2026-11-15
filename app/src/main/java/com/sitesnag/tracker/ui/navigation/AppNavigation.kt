package com.sitesnag.tracker.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.sitesnag.tracker.data.local.EngineerProfileManager
import com.sitesnag.tracker.ui.screens.capture.CaptureScreen
import com.sitesnag.tracker.ui.screens.capture.CaptureViewModel
import com.sitesnag.tracker.ui.screens.dashboard.DashboardScreen
import com.sitesnag.tracker.ui.screens.dashboard.DashboardViewModel
import com.sitesnag.tracker.ui.screens.ncr.NcrListScreen
import com.sitesnag.tracker.ui.screens.ncr.NcrListViewModel
import com.sitesnag.tracker.ui.screens.observations.ObservationListScreen
import com.sitesnag.tracker.ui.screens.observations.ObservationListViewModel
import com.sitesnag.tracker.ui.screens.settings.*
import com.sitesnag.tracker.ui.theme.NavyPrimary
import com.sitesnag.tracker.ui.theme.NcrRed
import com.sitesnag.tracker.ui.theme.RectifiedGreen
import com.sitesnag.tracker.ui.theme.SurfaceBorder

data class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val profileManager = remember { EngineerProfileManager.getInstance(context) }
    val engineerProfile by profileManager.profileFlow.collectAsState()

    var showProfileSetupDialog by remember { mutableStateOf(!profileManager.isConfigured()) }
    var setupName by remember { mutableStateOf(engineerProfile.fullName) }
    var setupInitials by remember { mutableStateOf(engineerProfile.initials) }

    val captureViewModel: CaptureViewModel = viewModel()
    val observationListViewModel: ObservationListViewModel = viewModel()
    val ncrListViewModel: NcrListViewModel = viewModel()
    val dashboardViewModel: DashboardViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()

    val dashboardUiState by dashboardViewModel.uiState.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        BottomNavItem(Screen.Capture.route, "Capture", Icons.Default.CameraAlt),
        BottomNavItem(Screen.Observations.route, "Snags", Icons.Default.Checklist),
        BottomNavItem(Screen.Ncrs.route, "NCRs", Icons.Default.Warning),
        BottomNavItem(Screen.Dashboard.route, "Dashboard", Icons.Default.BarChart),
        BottomNavItem(Screen.Settings.route, "Settings", Icons.Default.Settings)
    )

    val showBottomBar = currentRoute in bottomNavItems.map { it.route }
    var projectMenuExpanded by remember { mutableStateOf(false) }

    // First-Launch Engineer Profile Setup Dialog
    if (showProfileSetupDialog) {
        AlertDialog(
            onDismissRequest = { /* Force setup */ },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Badge, contentDescription = null, tint = NavyPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text("Welcome to SnagOps", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Set up your team engineer profile. Your initials will prefix your site logs (e.g. OBS-RK-001) for zero offline collisions across team members.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.DarkGray
                    )
                    OutlinedTextField(
                        value = setupName,
                        onValueChange = { setupName = it },
                        label = { Text("Your Full Name *") },
                        placeholder = { Text("e.g. Rahul Kumar") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = setupInitials,
                        onValueChange = { setupInitials = it.uppercase().take(4) },
                        label = { Text("Engineer Initials (2-4 Letters) *") },
                        placeholder = { Text("e.g. RK, AS, JDM") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (setupName.isNotBlank() && setupInitials.isNotBlank()) {
                            profileManager.saveProfile(setupName, setupInitials)
                            showProfileSetupDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                ) {
                    Text("Start Using SnagOps")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (showBottomBar) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = when (currentRoute) {
                                    Screen.Capture.route -> "SnagOps Quick Capture"
                                    Screen.Observations.route -> "Site Snag Register"
                                    Screen.Ncrs.route -> "Non-Conformance Register"
                                    Screen.Dashboard.route -> "Management Dashboard"
                                    else -> "SnagOps Setup & Settings"
                                },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    actions = {
                        // Active Project Selector Dropdown Pill
                        Box {
                            Surface(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .clickable { projectMenuExpanded = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Apartment,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = dashboardUiState.project.projectName.ifBlank { "Select Project" },
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.widthIn(max = 110.dp)
                                    )
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        contentDescription = "Switch Project",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = projectMenuExpanded,
                                onDismissRequest = { projectMenuExpanded = false }
                            ) {
                                Text(
                                    text = "Active Construction Project",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )

                                dashboardUiState.allProjects.forEach { proj ->
                                    val isCurrent = proj.id == dashboardUiState.project.id
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (isCurrent) {
                                                    Icon(
                                                        Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = RectifiedGreen,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(Modifier.width(8.dp))
                                                } else {
                                                    Spacer(Modifier.width(24.dp))
                                                }
                                                Column {
                                                    Text(
                                                        proj.projectName,
                                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                        fontSize = 14.sp
                                                    )
                                                    Text(
                                                        "Code: ${proj.projectCode}",
                                                        fontSize = 11.sp,
                                                        color = Color.Gray
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            dashboardViewModel.switchActiveProject(proj.id)
                                            projectMenuExpanded = false
                                        }
                                    )
                                }

                                HorizontalDivider(color = SurfaceBorder)

                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.FolderSpecial,
                                                contentDescription = null,
                                                tint = NavyPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                "Manage Projects...",
                                                fontWeight = FontWeight.SemiBold,
                                                color = NavyPrimary
                                            )
                                        }
                                    },
                                    onClick = {
                                        projectMenuExpanded = false
                                        navController.navigate(Screen.ProjectsManagement.route)
                                    }
                                )
                            }
                        }

                        // Engineer Initials Badge (Tap to Edit Profile)
                        Surface(
                            color = Color.White.copy(alpha = 0.25f),
                            shape = CircleShape,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable {
                                    setupName = engineerProfile.fullName
                                    setupInitials = engineerProfile.initials
                                    showProfileSetupDialog = true
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = engineerProfile.initials,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = when (currentRoute) {
                            Screen.Ncrs.route -> NcrRed
                            else -> NavyPrimary
                        },
                        titleContentColor = Color.White
                    )
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val isSelected = currentRoute == item.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title) }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Capture.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Capture.route) {
                CaptureScreen(
                    viewModel = captureViewModel,
                    snackbarHostState = snackbarHostState
                )
            }
            composable(Screen.Observations.route) {
                ObservationListScreen(
                    viewModel = observationListViewModel
                )
            }
            composable(Screen.Ncrs.route) {
                NcrListScreen(
                    viewModel = ncrListViewModel
                )
            }
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    snackbarHostState = snackbarHostState
                )
            }
            composable(Screen.Settings.route) {
                ProjectSettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateToProjects = { navController.navigate(Screen.ProjectsManagement.route) },
                    onNavigateToLocations = { navController.navigate(Screen.LocationSetup.route) },
                    onNavigateToVendors = { navController.navigate(Screen.VendorSetup.route) },
                    onNavigateToCustomFields = { navController.navigate(Screen.CustomFieldsSetup.route) },
                    snackbarHostState = snackbarHostState
                )
            }
            composable(Screen.ProjectsManagement.route) {
                ProjectsManagementScreen(
                    viewModel = settingsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.LocationSetup.route) {
                LocationManagementScreen(
                    viewModel = settingsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.VendorSetup.route) {
                VendorManagementScreen(
                    viewModel = settingsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.CustomFieldsSetup.route) {
                CustomFieldsScreen(
                    viewModel = settingsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
