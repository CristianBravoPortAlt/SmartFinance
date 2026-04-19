package com.example.smartfinance.ui.home


import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.smartfinance.ui.screens.auth.AuthViewModel
import com.example.smartfinance.ui.screens.auth.LoginScreen
import com.example.smartfinance.ui.screens.auth.RegisterScreen
import com.example.smartfinance.ui.screens.home.HomeScreen
import com.example.smartfinance.ui.screens.home.HomeViewModel
import com.example.smartfinance.ui.screens.profile.ProfileScreen
import com.example.smartfinance.ui.screens.transaction.AddEditTransactionScreen
import com.example.smartfinance.ui.screens.transaction.TransactionViewModel
import com.example.smartfinance.ui.screens.stats.StatsScreen

import com.example.smartfinance.ui.screens.goals.GoalScreen
import com.example.smartfinance.ui.screens.goals.GoalViewModel

sealed class AppScreens(val route: String) {
    data object Login : AppScreens("login_screen")
    data object Register : AppScreens("register_screen")
    data object Home : AppScreens("home_screen")
    data object Profile : AppScreens("profile_screen")
    data object AddEditTransaction : AppScreens("add_edit_transaction_screen/{transactionId}") {
        fun createRoute(transactionId: Int?) = "add_edit_transaction_screen/${transactionId ?: -1}"
    }
    data object Stats : AppScreens("stats_screen")
    data object Goals : AppScreens("goals_screen")
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
            val transactionViewModel: TransactionViewModel = hiltViewModel()
            HomeScreen(
                homeViewModel = homeViewModel,
                transactionViewModel = transactionViewModel,
                onNavigateToProfile = {
                    navController.navigate(AppScreens.Profile.route)
                },
                onNavigateToAddTransaction = {
                    navController.navigate(AppScreens.AddEditTransaction.createRoute(null))
                },
                onNavigateToEditTransaction = { id ->
                    navController.navigate(AppScreens.AddEditTransaction.createRoute(id))
                },
                onNavigateToStats = {
                    navController.navigate(AppScreens.Stats.route)
                },
                onNavigateToGoals = {
                    navController.navigate(AppScreens.Goals.route)
                }
            )
        }

        composable(route = AppScreens.Profile.route) {
            val profileViewModel: com.example.smartfinance.ui.screens.profile.ProfileViewModel = hiltViewModel()
            ProfileScreen(
                viewModel = profileViewModel,
                onLogout = {
                    navController.navigate(AppScreens.Login.route) {
                        popUpTo(AppScreens.Home.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(
            route = AppScreens.AddEditTransaction.route,
            arguments = listOf(navArgument("transactionId") { type = NavType.IntType })
        ) { backStackEntry ->
            val transactionViewModel: TransactionViewModel = hiltViewModel()
            val transactionId = backStackEntry.arguments?.getInt("transactionId")
            AddEditTransactionScreen(
                viewModel = transactionViewModel,
                transactionId = transactionId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = AppScreens.Stats.route) {
            val transactionViewModel: TransactionViewModel = hiltViewModel()
            StatsScreen(
                viewModel = transactionViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = AppScreens.Goals.route) {
            val goalViewModel: GoalViewModel = hiltViewModel()
            GoalScreen(
                viewModel = goalViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}