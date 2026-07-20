package uz.ownsms.sender.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import uz.ownsms.sender.R
import uz.ownsms.sender.ui.components.AppDrawer
import uz.ownsms.sender.ui.screens.AboutScreen
import uz.ownsms.sender.ui.screens.ActivityScreen
import uz.ownsms.sender.ui.screens.BulkScreen
import uz.ownsms.sender.ui.screens.GuideScreen
import uz.ownsms.sender.ui.screens.HomeScreen
import uz.ownsms.sender.ui.screens.OnboardingScreen
import uz.ownsms.sender.ui.screens.SettingsScreen
import uz.ownsms.sender.ui.screens.SimsScreen

private enum class Tab(val route: String, val labelRes: Int, val titleRes: Int, val icon: ImageVector) {
    HOME("home", R.string.tab_home_label, R.string.app_name, Icons.Filled.Home),
    ACTIVITY("activity", R.string.tab_activity, R.string.tab_activity, Icons.AutoMirrored.Filled.List),
    BULK("bulk", R.string.tab_bulk_label, R.string.tab_bulk_title, Icons.Filled.Campaign),
    SIMS("sims", R.string.tab_sims_label, R.string.tab_sims_title, Icons.Filled.Call),
    SETTINGS("settings", R.string.tab_settings_label, R.string.tab_settings_title, Icons.Filled.Settings),
}

private const val ROUTE_ABOUT = "about"
private const val ROUTE_GUIDE = "guide"

// Below this width we're on a phone (bottom nav); at/above, a tablet/foldable (side rail).
private val EXPANDED_WIDTH = 600.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(
    vm: MainViewModel,
    bulkVm: BulkViewModel,
    accountVm: AccountViewModel,
    onRequestPermissions: () -> Unit,
    onIgnoreBattery: () -> Unit,
    onRequestDefaultSms: () -> Unit,
) {
    val savedToken by vm.token.collectAsState()
    if (savedToken.isBlank()) {
        OnboardingScreen(vm, onRequestPermissions)
        return
    }

    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    val currentTab = Tab.entries.firstOrNull { it.route == current }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val version = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "—"
    }

    val titleRes = currentTab?.titleRes ?: when (current) {
        ROUTE_ABOUT -> R.string.drawer_about
        ROUTE_GUIDE -> R.string.drawer_guide
        else -> R.string.app_name
    }

    // Single navigation path for both bottom-nav and drawer: pop back to the start
    // destination so the back stack never grows past [home, X] and Back always reaches home.
    fun go(route: String) {
        nav.navigate(route) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun openFromDrawer(route: String) {
        scope.launch { drawerState.close() }
        go(route)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                version = version,
                current = current,
                onAbout = { openFromDrawer(ROUTE_ABOUT) },
                onGuide = { openFromDrawer(ROUTE_GUIDE) },
                onApiDocs = {
                    scope.launch { drawerState.close() }
                    context.openUrl("https://sms.omadli.uz/api/v1/docs")
                },
            )
        },
    ) {
        BoxWithConstraints {
            val expanded = maxWidth >= EXPANDED_WIDTH
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text(stringResource(titleRes)) },
                        navigationIcon = {
                            // Drawer routes (about/guide) show a back arrow; tab roots show the menu.
                            if (currentTab == null) {
                                IconButton(onClick = { nav.popBackStack() }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.action_back),
                                    )
                                }
                            } else {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.action_menu))
                                }
                            }
                        },
                    )
                },
                bottomBar = {
                    if (!expanded) {
                        NavigationBar {
                            Tab.entries.forEach { tab ->
                                val label = stringResource(tab.labelRes)
                                NavigationBarItem(
                                    selected = current == tab.route,
                                    onClick = { go(tab.route) },
                                    icon = { Icon(tab.icon, contentDescription = label) },
                                    label = { Text(label) },
                                )
                            }
                        }
                    }
                },
            ) { padding ->
                Row(modifier = Modifier.padding(padding).fillMaxSize()) {
                    if (expanded) {
                        NavigationRail {
                            Tab.entries.forEach { tab ->
                                val label = stringResource(tab.labelRes)
                                NavigationRailItem(
                                    selected = current == tab.route,
                                    onClick = { go(tab.route) },
                                    icon = { Icon(tab.icon, contentDescription = label) },
                                    label = { Text(label) },
                                )
                            }
                        }
                    }
                    Box(modifier = Modifier.fillMaxSize()) {
                        NavHost(
                            navController = nav,
                            startDestination = Tab.HOME.route,
                            // Cap content width so cards don't stretch on tablets; center it.
                            modifier = Modifier.widthIn(max = 640.dp).fillMaxSize().align(Alignment.TopCenter),
                        ) {
                            composable(Tab.HOME.route) {
                                HomeScreen(vm, onOpenSettings = { go(Tab.SETTINGS.route) })
                            }
                            composable(Tab.ACTIVITY.route) { ActivityScreen(vm) }
                            composable(Tab.BULK.route) { BulkScreen(bulkVm) }
                            composable(Tab.SIMS.route) { SimsScreen(vm) }
                            composable(Tab.SETTINGS.route) {
                                SettingsScreen(vm, accountVm, onRequestPermissions, onIgnoreBattery, onRequestDefaultSms)
                            }
                            composable(ROUTE_ABOUT) { AboutScreen() }
                            composable(ROUTE_GUIDE) { GuideScreen(vm) }
                        }
                    }
                }
            }
        }
    }
}
