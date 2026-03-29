package com.example.smartfinance.ui.home

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.smartfinance.ui.screens.auth.AuthViewModel
import com.example.smartfinance.ui.screens.auth.LoginScreen
import com.example.smartfinance.ui.screens.auth.RegisterScreen
import com.example.smartfinance.ui.screens.home.HomeScreen
import com.example.smartfinance.ui.screens.home.HomeViewModel
import com.example.smartfinance.ui.screens.profile.ProfileScreen

sealed class AppScreens(val route: String) {
    data object Login : AppScreens("login_screen")
    data object Register : AppScreens("register_screen")
    data object Home : AppScreens("home_screen")
    data object Profile : AppScreens("profile_screen")
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = AppScreens.Login.route) {

        composable(route = AppScreens.Login.route) {
            val authViewModel: AuthViewModel = hiltViewModel()
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToRegister = {
                    navController.navigate(AppScreens.Register.route)
                },
                onLoginSuccess = {
                    navController.navigate(AppScreens.Home.route) {
                        popUpTo(AppScreens.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(route = AppScreens.Register.route) {
            val authViewModel: AuthViewModel = hiltViewModel()
            RegisterScreen(
                viewModel = authViewModel,
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onRegisterSuccess = {
                    navController.navigate(AppScreens.Home.route) {
                        popUpTo(AppScreens.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(route = AppScreens.Home.route) {
            val homeViewModel: HomeViewModel = hiltViewModel()
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToProfile = {
                    navController.navigate(AppScreens.Profile.route)
                }
            )
        }

        composable(route = AppScreens.Profile.route) {
            ProfileScreen(
                onLogout = {
                    navController.navigate(AppScreens.Login.route) {
                        popUpTo(AppScreens.Home.route) { inclusive = true }
                    }
                }
            )
        }
    }
}