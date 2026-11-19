package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.local.BlockMatchResult
import com.example.data.model.PlatformCategory
import com.example.ui.BetShieldUiState
import com.example.ui.BetShieldViewModel
import com.example.ui.i18n.LocalizedCopy
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.EmergencySupportScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ProtectionCenterScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StatisticsScreen

object BetShieldRoutes {
  const val ROUTE_DASHBOARD = "dashboard"
  const val ROUTE_PROTECTION = "protection"
  const val ROUTE_STATISTICS = "statistics"
  const val ROUTE_SETTINGS = "settings"
  const val ROUTE_EMERGENCY = "emergency"
}

private data class TopLevelNavItem(
  val route: String,
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
)

@Composable
fun BetShieldAppShell(
  state: BetShieldUiState,
  copy: LocalizedCopy,
  viewModel: BetShieldViewModel,
  domainTestResult: Pair<String, BlockMatchResult?>?,
  feedbackMessage: String?,
  openEmergencyInitially: Boolean,
  onToggleVpn: (Boolean) -> Unit,
  onOpenAccessibilitySettings: () -> Unit,
) {
  var forceOnboarding by rememberSaveable { mutableStateOf(false) }

  if (!state.settings.onboardingCompleted || forceOnboarding) {
    if (forceOnboarding) {
      BackHandler { forceOnboarding = false }
    }
    OnboardingScreen(
      copy = copy,
      onFinish = {
        forceOnboarding = false
        viewModel.completeOnboarding()
      },
    )
    return
  }

  val navController = rememberNavController()
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(feedbackMessage) {
    if (!feedbackMessage.isNullOrBlank()) {
      snackbarHostState.showSnackbar(feedbackMessage)
      viewModel.clearFeedback()
    }
  }

  LaunchedEffect(openEmergencyInitially) {
    if (openEmergencyInitially) {
      navController.navigate(BetShieldRoutes.ROUTE_EMERGENCY) { launchSingleTop = true }
    }
  }

  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route ?: BetShieldRoutes.ROUTE_DASHBOARD
  val showChrome = currentRoute != BetShieldRoutes.ROUTE_EMERGENCY

  val topItems =
    listOf(
      TopLevelNavItem(
        route = BetShieldRoutes.ROUTE_DASHBOARD,
        label = copy.navDashboard,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
      ),
      TopLevelNavItem(
        route = BetShieldRoutes.ROUTE_PROTECTION,
        label = copy.navProtection,
        selectedIcon = Icons.Filled.Shield,
        unselectedIcon = Icons.Outlined.Shield,
      ),
      TopLevelNavItem(
        route = BetShieldRoutes.ROUTE_STATISTICS,
        label = copy.navStats,
        selectedIcon = Icons.Filled.BarChart,
        unselectedIcon = Icons.Outlined.BarChart,
      ),
      TopLevelNavItem(
        route = BetShieldRoutes.ROUTE_SETTINGS,
        label = copy.navSettings,
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
      ),
    )

  fun navigateTopLevel(route: String) {
    navController.navigate(route) {
      popUpTo(navController.graph.findStartDestination().id) { saveState = true }
      launchSingleTop = true
      restoreState = true
    }
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useNavRail = maxWidth >= 600.dp

    Scaffold(
      contentWindowInsets = WindowInsets.safeDrawing,
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      bottomBar = {
        if (showChrome && !useNavRail) {
          NavigationBar {
            topItems.forEach { item ->
              val selected = currentRoute == item.route
              NavigationBarItem(
                selected = selected,
                onClick = { navigateTopLevel(item.route) },
                icon = {
                  Icon(
                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                    contentDescription = item.label,
                  )
                },
                label = { Text(item.label) },
                modifier = Modifier.testTag("nav_${item.route}"),
              )
            }
          }
        }
      },
    ) { innerPadding ->
      Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        if (showChrome && useNavRail) {
          NavigationRail(modifier = Modifier.fillMaxHeight()) {
            topItems.forEach { item ->
              val selected = currentRoute == item.route
              NavigationRailItem(
                selected = selected,
                onClick = { navigateTopLevel(item.route) },
                icon = {
                  Icon(
                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                    contentDescription = item.label,
                  )
                },
                label = { Text(item.label) },
                modifier = Modifier.testTag("nav_${item.route}"),
              )
            }
          }
        }

        NavHost(
          navController = navController,
          startDestination = BetShieldRoutes.ROUTE_DASHBOARD,
          modifier = Modifier.weight(1f).fillMaxSize(),
        ) {
          composable(BetShieldRoutes.ROUTE_DASHBOARD) {
            DashboardScreen(
              state = state,
              copy = copy,
              onToggleVpn = onToggleVpn,
              onOpenEmergency = { navController.navigate(BetShieldRoutes.ROUTE_EMERGENCY) },
              onOpenProtection = { navigateTopLevel(BetShieldRoutes.ROUTE_PROTECTION) },
              onOpenSettings = { navigateTopLevel(BetShieldRoutes.ROUTE_SETTINGS) },
            )
          }

          composable(BetShieldRoutes.ROUTE_PROTECTION) {
            ProtectionCenterScreen(
              state = state,
              copy = copy,
              domainTestResult = domainTestResult,
              onToggleVpn = onToggleVpn,
              onOpenAccessibilitySettings = onOpenAccessibilitySettings,
              onToggleCategory = { category: PlatformCategory, enabled: Boolean ->
                viewModel.toggleCategory(category, enabled)
              },
              onSetPlatformEnabled = { id, enabled -> viewModel.setPlatformEnabled(id, enabled) },
              onAddCustomDomain = { viewModel.addCustomDomain(it) },
              onTestDomain = { viewModel.testDomainFilter(it) },
              onToggleCustomRule = { id, enabled -> viewModel.toggleCustomRule(id, enabled) },
              onDeleteCustomRule = { id -> viewModel.deleteCustomRule(id) },
              onToggleInstalledApp = { app, block -> viewModel.toggleInstalledAppBlock(app, block) },
              onAddManualPackage = { pkg, label -> viewModel.addManualBlockedPackage(pkg, label) },
              onRefreshApps = { viewModel.refreshInstalledApps() },
            )
          }

          composable(BetShieldRoutes.ROUTE_STATISTICS) {
            StatisticsScreen(
              state = state,
              copy = copy,
              computeBuckets = viewModel::computeStatBuckets,
              onClearHistory = { viewModel.clearHistory() },
            )
          }

          composable(BetShieldRoutes.ROUTE_SETTINGS) {
            SettingsScreen(
              state = state,
              copy = copy,
              onThemeChange = viewModel::setThemeMode,
              onLanguageChange = viewModel::setLanguage,
              onNotificationsChange = viewModel::setNotificationsEnabled,
              onCoolingOffChange = viewModel::setCoolingOffSeconds,
              onUpdateDatabase = viewModel::triggerRemoteDatabaseUpdate,
              onSaveRemoteUrl = viewModel::setRemoteDatabaseUrl,
              onResetBundledDatabase = viewModel::resetDatabaseToBundled,
              onRevisitOnboarding = { forceOnboarding = true },
            )
          }

          composable(BetShieldRoutes.ROUTE_EMERGENCY) {
            EmergencySupportScreen(
              coolingOffSeconds = state.settings.coolingOffSeconds,
              onBack = { navController.popBackStack() },
            )
          }
        }
      }
    }
  }
}
