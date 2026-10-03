package com.rakshax.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.rakshax.app.data.ble.BleRepository
import com.rakshax.app.data.location.LocationRepository
import com.rakshax.app.data.location.SosLocationPayload
import com.rakshax.app.data.network.NetworkStatusMonitor
import com.rakshax.app.data.remote.AuroraSafeRepository
import com.rakshax.app.data.repository.MockDataRepository
import com.rakshax.app.notification.FcmRegistration
import com.rakshax.app.service.SosEscalationService
import com.rakshax.app.ui.components.RakshaXBottomNav
import com.rakshax.app.ui.components.RakshaXTopBar
import com.rakshax.app.ui.screens.*
import com.rakshax.app.ui.theme.BackgroundDark
import kotlinx.coroutines.launch

@Composable
fun RakshaXNavGraph(
    navController: NavHostController = rememberNavController(),
    locationRepository: LocationRepository,
    bleRepository: BleRepository,
    auroraSafeRepository: AuroraSafeRepository,
    networkStatusMonitor: NetworkStatusMonitor
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Splash.route
    val deviceState by MockDataRepository.deviceState.collectAsState()
    val scanResults by bleRepository.scanResults.collectAsState()
    val bleError by bleRepository.errorMessage.collectAsState()
    val scope = rememberCoroutineScope()
    val showBars = currentRoute != Screen.Splash.route &&
                   currentRoute != Screen.Onboarding.route &&
                   currentRoute != Screen.Auth.route

    val screenTitle = when (currentRoute) {
        Screen.Home.route -> "Emergency Dashboard"
        Screen.Device.route -> "ESP32 Device Telemetry"
        Screen.SafetyMap.route -> "Safety Map & Intelligence"
        Screen.Contacts.route -> "Trusted Circle"
        Screen.SosStatus.route -> "Live SOS Tracker"
        Screen.Profile.route -> "Settings & Safety"
        else -> "Personal Safety"
    }

    Scaffold(
        topBar = {
            if (showBars) {
                RakshaXTopBar(
                    title = screenTitle,
                    deviceStatus = deviceState.status,
                    onDeviceClick = {
                        navController.navigate(Screen.Device.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (showBars) {
                RakshaXBottomNav(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        },
        containerColor = BackgroundDark
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BackgroundDark)
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(
                    onTimeout = {
                        navController.navigate(Screen.Onboarding.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onFinished = {
                        navController.navigate(Screen.Auth.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Auth.route) {
                AuthScreen(
                    onAuthSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Auth.route) { inclusive = true }
                        }
                    },
                    onAuthSubmit = { isRegister, name, email, phone, password ->
                        val result = if (isRegister) {
                            auroraSafeRepository.register(name, email, password, phone)
                        } else {
                            auroraSafeRepository.login(email, password)
                        }
                        result.value?.let {
                            MockDataRepository.setCurrentUser(it)
                            FcmRegistration.registerCurrentDevice(navController.context)
                        }
                        if (result.isSuccess) null else result.error ?: "Unable to contact AuroraSafe"
                    }
                )
            }

            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToMap = { navController.navigate(Screen.SafetyMap.route) },
                    onNavigateToContacts = { navController.navigate(Screen.Contacts.route) },
                    onNavigateToDevice = { navController.navigate(Screen.Device.route) },
                    onNavigateToSosLive = { navController.navigate(Screen.SosStatus.route) },
                    locationRepository = locationRepository,
                    networkStatusMonitor = networkStatusMonitor,
                    onTriggerSos = { location: SosLocationPayload? ->
                        scope.launch {
                            val contacts = MockDataRepository.contacts.value
                                .filter { it.isEnabled }
                                .map { it.name to it.phone }
                            val result = auroraSafeRepository.triggerSos(
                                location = location,
                                locationText = location?.let { "${it.latitude}, ${it.longitude}" }
                                    ?: "Location unavailable",
                                contacts = contacts,
                                userId = MockDataRepository.currentUser.value.id
                            )
                            MockDataRepository.triggerSos(
                                source = "In-App Emergency Button",
                                location = location,
                                backendStatus = if (result.isSuccess) "SENT" else "QUEUED",
                                backendEventId = result.value?.eventId,
                                backendError = result.error
                            )
                            SosEscalationService.start(navController.context)
                            navController.navigate(Screen.SosStatus.route)
                        }
                    }
                )
            }

            composable(Screen.Device.route) {
                DeviceScreen(
                    scanResults = scanResults,
                    errorMessage = bleError,
                    onScan = bleRepository::startScan,
                    onConnect = bleRepository::connect,
                    onDisconnect = bleRepository::disconnect
                )
            }

            composable(Screen.SafetyMap.route) {
                SafetyMapScreen(
                    locationRepository = locationRepository,
                    auroraSafeRepository = auroraSafeRepository
                )
            }

            composable(Screen.Contacts.route) {
                ContactsScreen()
            }

            composable(Screen.SosStatus.route) {
                SosStatusScreen(auroraSafeRepository = auroraSafeRepository)
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    onNavigateToContacts = { navController.navigate(Screen.Contacts.route) },
                    onNavigateToDevice = { navController.navigate(Screen.Device.route) },
                    onLogout = {
                        navController.navigate(Screen.Auth.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
