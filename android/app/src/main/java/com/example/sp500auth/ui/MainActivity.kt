package com.example.sp500auth.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.Column
import com.example.sp500auth.util.Resource
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.sp500auth.ui.auth.AssetDetailScreen
import com.example.sp500auth.ui.auth.AuthViewModel
import com.example.sp500auth.ui.auth.DashboardScreen
import com.example.sp500auth.ui.auth.ForgotPasswordScreen
import com.example.sp500auth.ui.auth.LoginScreen
import com.example.sp500auth.ui.auth.ProfileScreen
import com.example.sp500auth.ui.auth.RegisterScreen
import com.example.sp500auth.ui.navigation.NavRoutes
import com.example.sp500auth.ui.theme.SP500AuthTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SP500AuthTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    LaunchedEffect(Unit) { authViewModel.restoreSession() }
    LaunchedEffect(authViewModel.logoutState) {
        if (authViewModel.logoutState is Resource.Success || authViewModel.logoutState is Resource.Error) {
            navController.navigate(NavRoutes.Login.route) { popUpTo(0) { inclusive = true } }
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
            com.example.sp500auth.ui.finance.CompanySearchScreen(navController, authViewModel)
        }
        composable(NavRoutes.ForgotPassword.route) {
            ForgotPasswordScreen(navController = navController, viewModel = authViewModel)
        }
        composable(NavRoutes.Profile.route) {
            ProfileScreen(navController = navController, viewModel = authViewModel)
        }
        composable("pillars/liquidity") { com.example.sp500auth.ui.pillars.PillarScreen(navController, "liquidity") }
        composable("pillars/profitability") { com.example.sp500auth.ui.pillars.PillarScreen(navController, "profitability") }
        composable("scenarios/{ticker}") { entry ->
            com.example.sp500auth.ui.scenarios.ScenariosScreen(navController,entry.arguments?.getString("ticker") ?: "MSFT")
        }
        composable("company/{ticker}") { entry ->
            com.example.sp500auth.ui.finance.CompanyDetailScreen(navController, entry.arguments?.getString("ticker") ?: "MSFT")
        }
        composable("finance/{ticker}/{pillar}") { entry ->
            com.example.sp500auth.ui.pillars.PillarScreen(navController, entry.arguments?.getString("pillar") ?: "all",
                initialTicker=entry.arguments?.getString("ticker") ?: "MSFT")
        }
        composable(NavRoutes.AssetDetail.route) {
            AssetDetailScreen(navController = navController)
        }
    }
}