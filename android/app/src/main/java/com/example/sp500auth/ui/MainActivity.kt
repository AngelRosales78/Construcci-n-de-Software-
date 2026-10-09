package com.example.sp500auth.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.sp500auth.ui.auth.LoginScreen
import com.example.sp500auth.ui.auth.RegisterScreen
import com.example.sp500auth.ui.auth.DashboardScreen
import com.example.sp500auth.ui.navigation.NavRoutes
import com.example.sp500auth.ui.theme.SP500AuthTheme
import com.example.sp500auth.ui.auth.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
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

    NavHost(
        navController = navController,
        startDestination = NavRoutes.Login.route
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
    }
}
