package com.example.smartfinance.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.smartfinance.data.local.UserEntity
import com.example.smartfinance.ui.screens.about.AboutScreen
import com.example.smartfinance.ui.screens.auth.AuthViewModel
import com.example.smartfinance.ui.screens.auth.LoginScreen
import com.example.smartfinance.ui.screens.auth.RegisterScreen
import com.example.smartfinance.ui.screens.friends.FriendsScreen
import com.example.smartfinance.ui.screens.goals.GoalDetailScreen
import com.example.smartfinance.ui.screens.goals.GoalScreen
import com.example.smartfinance.ui.screens.goals.GoalViewModel
import com.example.smartfinance.ui.screens.groups.AddGroupExpenseScreen
import com.example.smartfinance.ui.screens.groups.GroupDetailScreen
import com.example.smartfinance.ui.screens.groups.GroupScreen
import com.example.smartfinance.ui.screens.groups.GroupViewModel
import com.example.smartfinance.ui.screens.home.HomeScreen
import com.example.smartfinance.ui.screens.home.HomeViewModel
import com.example.smartfinance.ui.screens.profile.CategoryManagementScreen
import com.example.smartfinance.ui.screens.profile.EditProfileScreen
import com.example.smartfinance.ui.screens.profile.ProfileScreen
import com.example.smartfinance.ui.screens.profile.ProfileViewModel
import com.example.smartfinance.ui.screens.profile.RecurringExpensesScreen
import com.example.smartfinance.ui.screens.stats.MonthlySummaryScreen
import com.example.smartfinance.ui.screens.stats.StatsScreen
import com.example.smartfinance.ui.screens.transaction.AddEditTransactionScreen
import com.example.smartfinance.ui.screens.transaction.TransactionViewModel
import kotlinx.coroutines.launch

sealed class AppScreens(val route: String, val label: String = "", val icon: ImageVector = Icons.Default.Home) {
    data object Login : AppScreens("login_screen")
    data object Register : AppScreens("register_screen")
    data object Home : AppScreens("home_screen", "Inicio", Icons.Default.Home)
    data object MonthlySummary : AppScreens("monthly_summary_screen", "Resumen Mensual", Icons.Default.CalendarMonth)
    data object Goals : AppScreens("goals_screen", "Mis Objetivos", Icons.Default.Star)
    data object Profile : AppScreens("profile_screen", "Mi Perfil", Icons.Default.Person)
    data object ProfileEdit : AppScreens("profile_edit_screen")
    data object RecurringExpenses : AppScreens("recurring_expenses_screen")
    data object AddEditTransaction : AppScreens("add_edit_transaction_screen/{transactionId}") {
        fun createRoute(transactionId: String?) = "add_edit_transaction_screen/${transactionId ?: "new"}"
    }
    data object Stats : AppScreens("stats_screen")
    data object ManageCategories : AppScreens("manage_categories_screen", "Categorías", Icons.Default.Category)
    data object Groups : AppScreens("groups_screen", "Mis Grupos", Icons.Default.Group)
    data object Friends : AppScreens("friends_screen", "Social", Icons.Default.People)
    data object About : AppScreens("about_screen")
    data object GoalDetail : AppScreens("goal_detail_screen/{goalId}") {
        fun createRoute(goalId: String) = "goal_detail_screen/$goalId"
    }
    data object GroupDetail : AppScreens("group_detail_screen/{groupId}") {
        fun createRoute(groupId: String) = "group_detail_screen/$groupId"
    }
    data object AddGroupExpense : AppScreens("add_group_expense_screen/{groupId}") {
        fun createRoute(groupId: String) = "add_group_expense_screen/$groupId"
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    val profileViewModel: ProfileViewModel = hiltViewModel()
    val authViewModel: AuthViewModel = hiltViewModel()
    val user by profileViewModel.currentUser.collectAsState()
    val currentUser = user

    LaunchedEffect(currentDestination) {
        drawerState.close()
    }

    val drawerItems = listOf(
        AppScreens.Home,
        AppScreens.MonthlySummary,
        AppScreens.Goals,
        AppScreens.Groups,
        AppScreens.Friends,
        AppScreens.ManageCategories
    )

    val showDrawer = drawerItems.any { it.route == currentDestination?.route } || currentDestination?.route == AppScreens.Profile.route

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = showDrawer,
        drawerContent = {
            if (showDrawer) {
                ModalDrawerSheet(
                    modifier = Modifier.width(300.dp),
                    drawerContainerColor = MaterialTheme.colorScheme.background,
                    drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
                ) {
                    DrawerHeader(
                        user = currentUser,
                        onPhotoClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(AppScreens.Profile.route) {
                                launchSingleTop = true
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    drawerItems.forEach { item ->
                        NavigationDrawerItem(
                            label = { Text(item.label, fontWeight = FontWeight.Medium) },
                            icon = { Icon(item.icon, contentDescription = null) },
                            selected = currentDestination?.route == item.route,
                            onClick = {
                                scope.launch { drawerState.close() }
                                if (currentDestination?.route != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(AppScreens.Home.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    
                    NavigationDrawerItem(
                        label = { Text("Sobre Nosotros", fontWeight = FontWeight.Medium) },
                        icon = { Icon(Icons.Default.Info, contentDescription = null) },
                        selected = currentDestination?.route == AppScreens.About.route,
                        onClick = {
                            scope.launch { 
                                drawerState.close() 
                                navController.navigate(AppScreens.About.route)
                            }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )

                    NavigationDrawerItem(
                        label = { Text("Cerrar Sesión", color = MaterialTheme.colorScheme.error) },
                        icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        selected = false,
                        onClick = {
                            scope.launch { 
                                drawerState.close() 
                                authViewModel.logout()
                                navController.navigate(AppScreens.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        },
                        modifier = Modifier.padding(12.dp),
                        shape = RoundedCornerShape(16.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    ) {
        NavHost(
            navController = navController,
            startDestination = AppScreens.Login.route,
            modifier = Modifier.fillMaxSize()
        ) {
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
                val goalViewModel: GoalViewModel = hiltViewModel()
                HomeScreen(
                    homeViewModel = homeViewModel,
                    transactionViewModel = transactionViewModel,
                    goalViewModel = goalViewModel,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onNavigateToProfile = {
                        navController.navigate(AppScreens.Profile.route)
                    },
                    onNavigateToAddTransaction = {
                        navController.navigate(AppScreens.AddEditTransaction.createRoute(null))
                    },
                    onNavigateToEditTransaction = { id: String ->
                        navController.navigate(AppScreens.AddEditTransaction.createRoute(id))
                    },
                    onNavigateToStats = {
                        navController.navigate(AppScreens.MonthlySummary.route)
                    },
                    onNavigateToGoals = {
                        navController.navigate(AppScreens.Goals.route)
                    },
                    onNavigateToGoalDetail = { id: String ->
                        navController.navigate(AppScreens.GoalDetail.createRoute(id))
                    },
                    onNavigateToMonthlySummary = {
                        navController.navigate(AppScreens.MonthlySummary.route)
                    }
                )
            }

            composable(route = AppScreens.MonthlySummary.route) {
                val transactionViewModel: TransactionViewModel = hiltViewModel()
                MonthlySummaryScreen(
                    viewModel = transactionViewModel,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = { id: String ->
                        navController.navigate(AppScreens.AddEditTransaction.createRoute(id))
                    }
                )
            }

            composable(route = AppScreens.Profile.route) {
                val profileViewModel: ProfileViewModel = hiltViewModel()
                ProfileScreen(
                    viewModel = profileViewModel,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToEdit = {
                        navController.navigate(AppScreens.ProfileEdit.route)
                    },
                    onNavigateToManageCategories = {
                        navController.navigate(AppScreens.ManageCategories.route)
                    },
                    onNavigateToGroups = {
                        navController.navigate(AppScreens.Groups.route)
                    },
                    onNavigateToRecurringExpenses = {
                        navController.navigate(AppScreens.RecurringExpenses.route)
                    },
                    onLogout = {
                        navController.navigate(AppScreens.Login.route) {
                            popUpTo(AppScreens.Home.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(route = AppScreens.ProfileEdit.route) {
                val profileViewModel: ProfileViewModel = hiltViewModel()
                EditProfileScreen(
                    viewModel = profileViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(route = AppScreens.RecurringExpenses.route) {
                val transactionViewModel: TransactionViewModel = hiltViewModel()
                RecurringExpensesScreen(
                    viewModel = transactionViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = { id -> 
                        navController.navigate(AppScreens.AddEditTransaction.createRoute(id))
                    }
                )
            }

            composable(route = AppScreens.ManageCategories.route) {
                val transactionViewModel: TransactionViewModel = hiltViewModel()
                CategoryManagementScreen(
                    viewModel = transactionViewModel,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            
            composable(
                route = AppScreens.AddEditTransaction.route,
                arguments = listOf(navArgument("transactionId") { type = NavType.StringType })
            ) { backStackEntry ->
                val transactionViewModel: TransactionViewModel = hiltViewModel()
                val transactionId = backStackEntry.arguments?.getString("transactionId")
                AddEditTransactionScreen(
                    viewModel = transactionViewModel,
                    transactionId = if (transactionId == "new") null else transactionId,
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
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onNavigateToGoalDetail = { id -> 
                        navController.navigate(AppScreens.GoalDetail.createRoute(id))
                    }
                )
            }

            composable(
                route = AppScreens.GoalDetail.route,
                arguments = listOf(navArgument("goalId") { type = NavType.StringType })
            ) { backStackEntry ->
                val goalViewModel: GoalViewModel = hiltViewModel()
                val goalId = backStackEntry.arguments?.getString("goalId") ?: ""
                GoalDetailScreen(
                    goalId = goalId,
                    viewModel = goalViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(route = AppScreens.Groups.route) {
                val groupViewModel: GroupViewModel = hiltViewModel()
                val groups by groupViewModel.groups.collectAsState()
                GroupScreen(
                    groups = groups,
                    onNavigateBack = { navController.popBackStack() },
                    onCreateGroup = { name, desc -> groupViewModel.createGroup(name, desc) },
                    onJoinGroup = { code -> groupViewModel.joinGroup(code) },
                    onNavigateToGroupDetail = { id: String -> navController.navigate(AppScreens.GroupDetail.createRoute(id)) }
                )
            }

            composable(route = AppScreens.Friends.route) {
                FriendsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = AppScreens.GroupDetail.route,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
                GroupDetailScreen(
                    groupId = groupId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToAddExpense = { id -> navController.navigate(AppScreens.AddGroupExpense.createRoute(id)) }
                )
            }

            composable(
                route = AppScreens.AddGroupExpense.route,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
                AddGroupExpenseScreen(
                    groupId = groupId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(route = AppScreens.About.route) {
                AboutScreen(onNavigateBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
fun DrawerHeader(user: UserEntity?, onPhotoClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                )
            )
            .padding(24.dp)
            .padding(top = 24.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .clickable { onPhotoClick() },
                contentAlignment = Alignment.Center
            ) {
                if (user?.profilePictureUri != null) {
                    coil.compose.AsyncImage(
                        model = user.profilePictureUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = user?.name ?: "SmartFinance",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Text(
                text = user?.email ?: "Tu libertad financiera",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}
