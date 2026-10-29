package com.drautomation.intelpark.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.drautomation.intelpark.AppContainer
import com.drautomation.intelpark.R
import com.drautomation.intelpark.data.Role
import com.drautomation.intelpark.ui.admin.AdminAlarmScreen
import com.drautomation.intelpark.ui.admin.AdminLiveScreen
import com.drautomation.intelpark.ui.admin.AdminMapScreen
import com.drautomation.intelpark.ui.admin.AdminPhotoScreen
import com.drautomation.intelpark.ui.admin.AdminViewModel
import com.drautomation.intelpark.ui.auth.AuthViewModel
import com.drautomation.intelpark.ui.auth.LoginScreen
import com.drautomation.intelpark.ui.auth.SignupAccountScreen
import com.drautomation.intelpark.ui.auth.SignupAddressScreen
import com.drautomation.intelpark.ui.auth.SignupPhoneScreen
import com.drautomation.intelpark.ui.auth.SignupPlateScreen
import com.drautomation.intelpark.ui.auth.WelcomeScreen
import com.drautomation.intelpark.ui.driver.BookingScreen
import com.drautomation.intelpark.ui.driver.DriverViewModel
import com.drautomation.intelpark.ui.driver.HomeScreen
import com.drautomation.intelpark.ui.driver.MapScreen
import com.drautomation.intelpark.ui.driver.PayScreen
import com.drautomation.intelpark.ui.driver.ProfileScreen
import com.drautomation.intelpark.ui.driver.ScanScreen
import com.drautomation.intelpark.ui.theme.IP

private object Routes {
    const val WELCOME = "welcome"
    const val LOGIN = "login?staff={staff}"
    const val SIGNUP_ACCOUNT = "signup/account"
    const val SIGNUP_PLATE = "signup/plate"
    const val SIGNUP_ADDRESS = "signup/address"
    const val SIGNUP_PHONE = "signup/phone"
    const val HOME = "home"
    const val MAP = "map"
    const val BOOKING = "booking?site={site}"
    const val PROFILE = "profile"
    const val SCAN = "scan"
    const val PAY = "pay/{bay}"
    const val ADMIN_LIVE = "admin/live"
    const val ADMIN_MAP = "admin/map"
    const val ADMIN_ALARM = "admin/alarm/{id}"
    const val ADMIN_PHOTO = "admin/photo/{id}"

    fun login(staff: Boolean) = "login?staff=$staff"
    fun booking(site: String? = null) = if (site == null) "booking" else "booking?site=$site"
    fun pay(bay: String) = "pay/$bay"
    fun alarm(id: String) = "admin/alarm/$id"
    fun photo(id: String) = "admin/photo/$id"
}

private data class Tab(val route: String, val pattern: String, val label: Int, val icon: ImageVector)

private val driverTabs = listOf(
    Tab(Routes.HOME, Routes.HOME, R.string.tab_home, Icons.Filled.Home),
    Tab(Routes.MAP, Routes.MAP, R.string.tab_map, Icons.Filled.Map),
    Tab(Routes.booking(), Routes.BOOKING, R.string.tab_booking, Icons.Filled.CalendarMonth),
    Tab(Routes.PROFILE, Routes.PROFILE, R.string.tab_profile, Icons.Filled.Person),
)

private val adminTabs = listOf(
    Tab(Routes.ADMIN_LIVE, Routes.ADMIN_LIVE, R.string.tab_live, Icons.Filled.MonitorHeart),
    Tab(Routes.ADMIN_MAP, Routes.ADMIN_MAP, R.string.tab_map, Icons.Filled.Map),
)

@Composable
fun AppNav(container: AppContainer) {
    val nav = rememberNavController()
    val auth: AuthViewModel = viewModel { AuthViewModel(container) }
    val driver: DriverViewModel = viewModel { DriverViewModel(container) }
    val admin: AdminViewModel = viewModel { AdminViewModel(container) }
    val start = when (container.store.role) {
        Role.DRIVER -> Routes.HOME
        Role.STAFF -> Routes.ADMIN_LIVE
        null -> Routes.WELCOME
    }
    val entry by nav.currentBackStackEntryAsState()
    val current = entry?.destination?.route
    val tabs = when {
        driverTabs.any { it.pattern == current } -> driverTabs
        adminTabs.any { it.pattern == current } -> adminTabs
        else -> emptyList()
    }
    Box(Modifier.fillMaxSize().background(IP.Ground).windowInsetsPadding(WindowInsets.safeDrawing)) {
        Scaffold(
            containerColor = IP.Ground,
            contentWindowInsets = WindowInsets(0),
            bottomBar = { if (tabs.isNotEmpty()) BottomBar(nav, tabs, current) },
        ) { padding ->
            NavHost(nav, startDestination = start, modifier = Modifier.padding(padding)) {
                // --- Welcome, sign-in, sign-up (4 steps) ---------------------------------------
                composable(Routes.WELCOME) {
                    WelcomeScreen(auth, onCreate = { nav.navigate(Routes.SIGNUP_ACCOUNT) }, onLogin = { nav.navigate(Routes.login(it)) })
                }
                composable(Routes.LOGIN, arguments = listOf(navArgument("staff") { type = NavType.BoolType; defaultValue = false })) {
                    LoginScreen(
                        auth, staffFirst = it.arguments?.getBoolean("staff") ?: false,
                        onBack = { nav.popBackStack() },
                        onSignup = { nav.navigate(Routes.SIGNUP_ACCOUNT) },
                        onDone = { staff, next -> nav.enter(if (staff) Routes.ADMIN_LIVE else next ?: Routes.HOME) },
                    )
                }
                composable(Routes.SIGNUP_ACCOUNT) {
                    SignupAccountScreen(auth, onBack = { nav.popBackStack() }, onLogin = { nav.navigate(Routes.login(false)) },
                        onDone = { nav.enter(Routes.SIGNUP_PLATE) })
                }
                composable(Routes.SIGNUP_PLATE) {
                    // the account exists now: no way back to step 1
                    SignupPlateScreen(auth, onBack = null, onDone = { nav.navigate(Routes.SIGNUP_ADDRESS) })
                }
                composable(Routes.SIGNUP_ADDRESS) {
                    SignupAddressScreen(auth, onBack = { nav.popBackStack() }, onDone = { nav.navigate(Routes.SIGNUP_PHONE) })
                }
                composable(Routes.SIGNUP_PHONE) {
                    SignupPhoneScreen(auth, onBack = { nav.popBackStack() }, onDone = { nav.enter(Routes.HOME) })
                }

                // --- Driver ----------------------------------------------------------------------
                composable(Routes.HOME) {
                    HomeScreen(driver, onScan = { nav.navigate(Routes.SCAN) }, onBook = { nav.tab(Routes.booking()) },
                        onMap = { nav.tab(Routes.MAP) })
                }
                composable(Routes.MAP) { MapScreen(driver, onBook = { nav.tab(Routes.booking(it)) }) }
                composable(Routes.BOOKING, arguments = listOf(navArgument("site") { type = NavType.StringType; nullable = true })) {
                    BookingScreen(driver, it.arguments?.getString("site"))
                }
                composable(Routes.PROFILE) {
                    ProfileScreen(driver, container.store.serverUrl, onLogout = { nav.enter(Routes.WELCOME) })
                }
                composable(Routes.SCAN) {
                    ScanScreen(driver, onBack = { nav.popBackStack() }, onBay = { bay ->
                        nav.navigate(Routes.pay(bay)) { popUpTo(Routes.SCAN) { inclusive = true } }
                    })
                }
                composable(Routes.PAY, arguments = listOf(navArgument("bay") { type = NavType.StringType })) {
                    PayScreen(driver, it.arguments?.getString("bay").orEmpty(), onBack = { nav.popBackStack() },
                        onHome = { nav.tab(Routes.HOME) })
                }

                // --- Agents and administrators ------------------------------------------------------
                composable(Routes.ADMIN_LIVE) {
                    AdminLiveScreen(admin, onAlarm = { nav.navigate(Routes.alarm(it)) }, onMap = { nav.tab(Routes.ADMIN_MAP) },
                        onLogout = { nav.enter(Routes.WELCOME) })
                }
                composable(Routes.ADMIN_MAP) { AdminMapScreen(admin, onAlarm = { nav.navigate(Routes.alarm(it)) }) }
                composable(Routes.ADMIN_ALARM, arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                    val id = it.arguments?.getString("id").orEmpty()
                    AdminAlarmScreen(admin, id, onBack = { nav.popBackStack() }, onFine = { nav.navigate(Routes.photo(id)) })
                }
                composable(Routes.ADMIN_PHOTO, arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                    AdminPhotoScreen(admin, it.arguments?.getString("id").orEmpty(), onBack = { nav.popBackStack() },
                        onDone = { nav.tab(Routes.ADMIN_LIVE) })
                }
            }
        }
    }
}

/** Go to a top-level screen and forget everything before (after sign-in, sign-up or sign-out). */
private fun NavHostController.enter(route: String) = navigate(route) {
    popUpTo(graph.id) { inclusive = true }
    launchSingleTop = true
}

/** Switch between the tabs of the bottom bar. */
private fun NavHostController.tab(route: String) = navigate(route) {
    popUpTo(if (route.startsWith("admin")) Routes.ADMIN_LIVE else Routes.HOME) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@Composable
private fun BottomBar(nav: NavHostController, tabs: List<Tab>, current: String?) {
    NavigationBar(containerColor = Color.White) {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = current == tab.pattern,
                onClick = { nav.tab(tab.route) },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(stringResource(tab.label)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = IP.Ink, selectedTextColor = IP.Ink, indicatorColor = IP.Yellow,
                    unselectedIconColor = IP.Muted, unselectedTextColor = IP.Muted,
                ),
            )
        }
    }
}
