package com.example.sp500auth.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.Column
import com.example.sp500auth.util.Resource
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.sp500auth.data.ops.SessionEvents
import com.example.sp500auth.data.push.*
import com.example.sp500auth.ui.auth.AuthViewModel
import com.example.sp500auth.ui.auth.ForgotPasswordScreen
import com.example.sp500auth.ui.auth.LoginScreen
import com.example.sp500auth.ui.auth.ProfileScreen
import com.example.sp500auth.ui.auth.RegisterScreen
import com.example.sp500auth.ui.navigation.NavRoutes
import com.example.sp500auth.ui.theme.SP500AuthTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    @Inject lateinit var sessionEvents: SessionEvents
    @Inject lateinit var pushRegistrar: PushRegistrar
    private var pushTarget by mutableStateOf<PushTarget?>(null)

    private fun readTarget(intent: Intent?) {
        pushTarget(intent?.getStringExtra(EXTRA_TICKER), intent?.getStringExtra(EXTRA_OWNER), intent?.getStringExtra(EXTRA_NOTICE))?.let { pushTarget = it }
        // Consumed once: a rotation must not reopen the company.
        listOf(EXTRA_TICKER, EXTRA_OWNER, EXTRA_NOTICE).forEach { intent?.removeExtra(it) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readTarget(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        readTarget(intent)
        // The app is always dark: light system icons keep the status bar readable.
        enableEdgeToEdge(SystemBarStyle.dark(android.graphics.Color.TRANSPARENT), SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        setContent {
            SP500AuthTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(sessionEvents, pushRegistrar, pushTarget) { pushTarget = null }
                }
            }
        }
    }
}

@Composable
fun AppNavigation(sessionEvents: SessionEvents, push: PushRegistrar, target: PushTarget?, onTargetUsed: () -> Unit) {
    val navController = rememberNavController()
    val notices: com.example.sp500auth.ui.ops.NotificationsViewModel = hiltViewModel()
    val account: com.example.sp500auth.ui.ops.AccountViewModel = hiltViewModel()
    val authViewModel: AuthViewModel = hiltViewModel()
    val favoritesViewModel: com.example.sp500auth.ui.favorites.FavoritesViewModel = hiltViewModel()
    val reportsViewModel: com.example.sp500auth.ui.reports.ReportsViewModel = hiltViewModel()
    val recentViewModel:com.example.sp500auth.ui.sprint3.RecentViewModel=hiltViewModel()
    LaunchedEffect(Unit) { authViewModel.restoreSession() }
    LaunchedEffect(authViewModel.logoutState) {
        if (authViewModel.logoutState is Resource.Success || authViewModel.logoutState is Resource.Error) {
            favoritesViewModel.clear()
            reportsViewModel.reset()
            recentViewModel.clear()
            notices.clear()
            account.clear()
            navController.navigate(NavRoutes.Login.route) { popUpTo(0) { inclusive = true } }
        }
    }
    fun endSession() {
        authViewModel.sessionEnded()
        favoritesViewModel.clear(); reportsViewModel.reset(); recentViewModel.clear(); notices.clear(); account.clear()
        navController.navigate(NavRoutes.Login.route) { popUpTo(0) { inclusive = true } }
    }
    // HU13_03: a blocked account or a role change closes the session on the next request.
    val revoked by sessionEvents.revoked.collectAsState()
    var endMessage by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(revoked) { revoked?.let { endMessage = it; endSession(); sessionEvents.consumed() } }
    // HU13_05: only after the server confirmed the deletion.
    LaunchedEffect(account.deleted) { if (account.deleted) { endMessage = "Tu cuenta y tus datos se eliminaron."; endSession() } }
    endMessage?.let { text ->
        AlertDialog(onDismissRequest = { endMessage = null }, title = { Text("Sesión terminada") }, text = { Text(text) },
            confirmButton = { TextButton(onClick = { endMessage = null }) { Text("Entendido") } })
    }
    val entry by navController.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val inside = route != null && route !in setOf(NavRoutes.Splash.route, NavRoutes.Login.route, NavRoutes.Register.route, NavRoutes.ForgotPassword.route)
    LaunchedEffect(inside) { if (inside) { notices.refresh(); account.load(); push.resync() } }
    // HU12_02: opens the company once there is a session, and only for the account that received the notice.
    LaunchedEffect(inside, target) {
        if (inside && target != null) {
            if (push.owner() == target.owner) {
                navController.navigate("company/${android.net.Uri.encode(target.ticker)}") { launchSingleTop = true }
                notices.refresh(); target.noticeId?.let(notices::markRead)
            }
            onTargetUsed()
        }
    }

    NavHost(
        navController = navController,
        startDestination = NavRoutes.Splash.route
    ) {
        composable(NavRoutes.Splash.route) {
            val state = authViewModel.sessionState
            LaunchedEffect(state) {
                when (state) {
                    is Resource.Success -> navController.navigate(NavRoutes.Dashboard.route) { popUpTo(NavRoutes.Splash.route) { inclusive = true } }
                    is Resource.Empty -> navController.navigate(NavRoutes.Login.route) { popUpTo(NavRoutes.Splash.route) { inclusive = true } }
                    else -> Unit
                }
            }
            Column {
                if (state is Resource.Loading) CircularProgressIndicator()
                if (state is Resource.Error) {
                    Text(state.message ?: "No se pudo verificar la sesión")
                    Button(onClick = { authViewModel.restoreSession() }) { Text("Reintentar") }
                    Button(onClick = { authViewModel.logout() }) { Text("Volver al acceso") }
                }
            }
        }
        composable(NavRoutes.Login.route) {
            LoginScreen(navController = navController, viewModel = authViewModel)
        }
        composable(NavRoutes.Register.route) {
            RegisterScreen(navController = navController, viewModel = authViewModel)
        }
        composable(NavRoutes.Dashboard.route) {
            com.example.sp500auth.ui.finance.CompanySearchScreen(navController, authViewModel, favorites = favoritesViewModel,recent=recentViewModel, notices = notices, account = account)
        }
        composable(NavRoutes.ForgotPassword.route) {
            ForgotPasswordScreen(navController = navController, viewModel = authViewModel)
        }
        composable(NavRoutes.Profile.route) {
            ProfileScreen(navController = navController, viewModel = authViewModel, account = account)
        }
        composable("profile/edit") { com.example.sp500auth.ui.ops.ProfileEditScreen(navController, account) }
        composable("profile/delete") { com.example.sp500auth.ui.ops.DeleteAccountScreen(navController, account) }
        composable("notifications") { com.example.sp500auth.ui.ops.NotificationsScreen(navController, notices) }
        composable("notifications/preferences") { com.example.sp500auth.ui.ops.PreferencesScreen(navController) }
        composable("admin") { com.example.sp500auth.ui.ops.AdminHubScreen(navController, account) }
        composable("admin/users") { com.example.sp500auth.ui.ops.UsersScreen(navController) }
        composable("admin/catalog") { com.example.sp500auth.ui.ops.CatalogScreen(navController) }
        composable("admin/catalog/add") { com.example.sp500auth.ui.ops.AddCompanyScreen(navController) }
        composable("admin/monitoring") { com.example.sp500auth.ui.ops.MonitoringScreen(navController) }
        composable("compare?ticker={ticker}", arguments = listOf(androidx.navigation.navArgument("ticker") { defaultValue = "" })) { entry ->
            com.example.sp500auth.ui.sprint3.ComparisonAccessGate(navController) {
                com.example.sp500auth.ui.compare.ComparisonScreen(navController, entry.arguments?.getString("ticker").orEmpty())
            }
        }
        composable("plans") { com.example.sp500auth.ui.billing.PlansScreen(navController) }
        composable("checkout") { com.example.sp500auth.ui.billing.CheckoutScreen(navController) }
        composable("invoices") { com.example.sp500auth.ui.billing.InvoicesScreen(navController) }
        composable("receipt/{number}?celebrate={celebrate}", arguments = listOf(
            androidx.navigation.navArgument("celebrate") { defaultValue = "false" })) { entry ->
            com.example.sp500auth.ui.billing.ReceiptScreen(navController, entry.arguments?.getString("number").orEmpty(),
                entry.arguments?.getString("celebrate") == "true")
        }
        composable("report?a={a}&b={b}&year={year}&range={range}", arguments = listOf(
            androidx.navigation.navArgument("a") { defaultValue="" }, androidx.navigation.navArgument("b") {defaultValue=""}, androidx.navigation.navArgument("year") { defaultValue="" }, androidx.navigation.navArgument("range") { defaultValue="1Y" })) { entry ->
            val b=entry.arguments?.getString("b")?.takeIf {it.isNotBlank()}
            val screen: @Composable ()->Unit = { com.example.sp500auth.ui.reports.ReportScreen(navController,entry.arguments?.getString("a").orEmpty(),b,entry.arguments?.getString("year").orEmpty(),entry.arguments?.getString("range") ?: "1Y",reportsViewModel) }
            if(b!=null) com.example.sp500auth.ui.sprint3.ComparisonAccessGate(navController) {screen()} else screen()
        }
        composable("favorites") { val tags:com.example.sp500auth.ui.favorites.TagsViewModel=hiltViewModel();com.example.sp500auth.ui.favorites.FavoritesScreen(navController, favoritesViewModel,tags) }
        composable("tags?favorite={favorite}",arguments=listOf(androidx.navigation.navArgument("favorite"){defaultValue=""})){entry->com.example.sp500auth.ui.favorites.TagsScreen(navController,entry.arguments?.getString("favorite")?.toLongOrNull(),favoritesViewModel)}
        composable("pillars/liquidity") { com.example.sp500auth.ui.pillars.PillarScreen(navController, "liquidity") }
        composable("pillars/profitability") { com.example.sp500auth.ui.pillars.PillarScreen(navController, "profitability") }
        composable("analysis/{ticker}/{year}") { entry ->
            com.example.sp500auth.ui.analysis.AnalysisScreen(navController,entry.arguments?.getString("ticker") ?: "MSFT",
                entry.arguments?.getString("year")?.toIntOrNull() ?: 2024)
        }
        composable("scenarios/{ticker}") { entry ->
            com.example.sp500auth.ui.scenarios.ScenariosScreen(navController,entry.arguments?.getString("ticker") ?: "MSFT")
        }
        composable("company/{ticker}") { entry ->
            com.example.sp500auth.ui.finance.CompanyDetailScreen(navController, entry.arguments?.getString("ticker") ?: "MSFT", favorites = favoritesViewModel,recent=recentViewModel)
        }
        composable("finance/{ticker}/{pillar}") { entry ->
            com.example.sp500auth.ui.pillars.PillarScreen(navController, entry.arguments?.getString("pillar") ?: "all",
                initialTicker=entry.arguments?.getString("ticker") ?: "MSFT")
        }
    }
}
