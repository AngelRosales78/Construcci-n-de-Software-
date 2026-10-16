package com.example.sp500auth.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
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
    val isLoggedIn by remember { mutableStateOf(authViewModel.isLoggedIn()) }

    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) NavRoutes.Dashboard.route else NavRoutes.Login.route
    ) {
        composable(NavRoutes.Login.route) {
            LoginScreen(navController = navController, viewModel = authViewModel)
        }
        composable(NavRoutes.Register.route) {
            RegisterScreen(navController = navController, viewModel = authViewModel)
        }
        composable(NavRoutes.Dashboard.route) {
            DashboardScreen(navController = navController, viewModel = authViewModel)
        }
        composable(NavRoutes.ForgotPassword.route) {
            ForgotPasswordScreen(navController = navController, viewModel = authViewModel)
        }
        composable(NavRoutes.Profile.route) {
            ProfileScreen(navController = navController, viewModel = authViewModel)
        }
        composable(NavRoutes.AssetDetail.route) {
            AssetDetailScreen(navController = navController)
        }
    }
}