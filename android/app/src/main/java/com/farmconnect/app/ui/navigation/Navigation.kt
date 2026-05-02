package com.farmconnect.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.farmconnect.app.ui.screens.SplashScreen
import com.farmconnect.app.ui.screens.auth.LoginScreen
import com.farmconnect.app.ui.screens.auth.RegisterScreen
import com.farmconnect.app.ui.screens.contracts.ContractDetailScreen
import com.farmconnect.app.ui.screens.contracts.ContractsScreen
import com.farmconnect.app.ui.screens.dashboard.DashboardScreen
import com.farmconnect.app.ui.screens.disputes.DisputesScreen
import com.farmconnect.app.ui.screens.disputes.RaiseDisputeScreen
import com.farmconnect.app.ui.screens.listings.CreateListingScreen
import com.farmconnect.app.ui.screens.listings.ListingDetailScreen
import com.farmconnect.app.ui.screens.listings.ListingsScreen
import com.farmconnect.app.ui.screens.negotiations.NegotiationChatScreen
import com.farmconnect.app.ui.screens.negotiations.NegotiationsScreen
import com.farmconnect.app.ui.screens.profile.ProfileScreen
import com.farmconnect.app.ui.screens.profile.EditProfileScreen
import com.farmconnect.app.ui.screens.profile.ChangePasswordScreen
import com.farmconnect.app.ui.screens.profile.PrivacyPolicyScreen
import com.farmconnect.app.ui.screens.notifications.NotificationsScreen
import com.farmconnect.app.ui.viewmodels.AuthViewModel

sealed class Screen(val route: String) {
    // Splash
    object Splash : Screen("splash")
    
    // Auth
    object Login : Screen("login")
    object Register : Screen("register")
    
    // Main
    object Dashboard : Screen("dashboard")
    object Profile : Screen("profile")
    object EditProfile : Screen("edit-profile")
    object ChangePassword : Screen("change-password")
    object PrivacyPolicy : Screen("privacy-policy")
    object Notifications : Screen("notifications")
    
    // Listings
    object Listings : Screen("listings")
    object ListingDetail : Screen("listing/{listingId}") {
        fun createRoute(listingId: Int) = "listing/$listingId"
    }
    object CreateListing : Screen("create-listing")
    
    // Negotiations
    object Negotiations : Screen("negotiations")
    object NegotiationChat : Screen("negotiation/{negotiationId}") {
        fun createRoute(negotiationId: Int) = "negotiation/$negotiationId"
    }
    
    // Contracts
    object Contracts : Screen("contracts")
    object ContractDetail : Screen("contract/{contractId}") {
        fun createRoute(contractId: Int) = "contract/$contractId"
    }
    
    // Disputes
    object Disputes : Screen("disputes")
    object RaiseDispute : Screen("raise-dispute/{contractId}") {
        fun createRoute(contractId: Int) = "raise-dispute/$contractId"
    }
}

@Composable
fun FarmConnectNavigation(
    navController: NavHostController = rememberNavController()
) {
    // Start at Splash to check auth state without flicker
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        // Splash Screen
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToDashboard = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        
        // Auth Screens
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = { 
                    navController.navigate(Screen.Register.route) 
                },
                onLoginSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        // Dashboard
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToListings = { navController.navigate(Screen.Listings.route) },
                onNavigateToNegotiations = { navController.navigate(Screen.Negotiations.route) },
                onNavigateToContracts = { navController.navigate(Screen.Contracts.route) },
                onNavigateToDisputes = { navController.navigate(Screen.Disputes.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateToCreateListing = { navController.navigate(Screen.CreateListing.route) },
                onNavigateToListingDetail = { id -> navController.navigate(Screen.ListingDetail.createRoute(id)) },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToNotifications = {
                    navController.navigate(Screen.Notifications.route)
                }
            )
        }
        
        // Listings
        composable(Screen.Listings.route) {
            ListingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { id -> navController.navigate(Screen.ListingDetail.createRoute(id)) },
                onNavigateToCreate = { navController.navigate(Screen.CreateListing.route) }
            )
        }
        
        composable(
            route = Screen.ListingDetail.route,
            arguments = listOf(navArgument("listingId") { type = NavType.IntType })
        ) { backStackEntry ->
            val listingId = backStackEntry.arguments?.getInt("listingId") ?: 0
            ListingDetailScreen(
                listingId = listingId,
                onNavigateBack = { navController.popBackStack() },
                onStartNegotiation = { negotiationId ->
                    navController.navigate(Screen.NegotiationChat.createRoute(negotiationId))
                }
            )
        }
        
        composable(Screen.CreateListing.route) {
            CreateListingScreen(
                onNavigateBack = { navController.popBackStack() },
                onListingCreated = { navController.popBackStack() }
            )
        }
        
        // Negotiations
        composable(Screen.Negotiations.route) {
            NegotiationsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToChat = { id -> navController.navigate(Screen.NegotiationChat.createRoute(id)) }
            )
        }
        
        composable(
            route = Screen.NegotiationChat.route,
            arguments = listOf(navArgument("negotiationId") { type = NavType.IntType })
        ) { backStackEntry ->
            val negotiationId = backStackEntry.arguments?.getInt("negotiationId") ?: 0
            NegotiationChatScreen(
                negotiationId = negotiationId,
                onNavigateBack = { navController.popBackStack() },
                onContractCreated = { contractId ->
                    navController.navigate(Screen.ContractDetail.createRoute(contractId)) {
                        popUpTo(Screen.Negotiations.route) { inclusive = true }
                    }
                }
            )
        }
        
        // Contracts
        composable(Screen.Contracts.route) {
            ContractsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { id -> navController.navigate(Screen.ContractDetail.createRoute(id)) }
            )
        }
        
        composable(
            route = Screen.ContractDetail.route,
            arguments = listOf(navArgument("contractId") { type = NavType.IntType })
        ) { backStackEntry ->
            val contractId = backStackEntry.arguments?.getInt("contractId") ?: 0
            ContractDetailScreen(
                contractId = contractId,
                onNavigateBack = { navController.popBackStack() },
                onRaiseDispute = { navController.navigate(Screen.RaiseDispute.createRoute(contractId)) }
            )
        }
        
        // Disputes
        composable(Screen.Disputes.route) {
            DisputesScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Screen.RaiseDispute.route,
            arguments = listOf(navArgument("contractId") { type = NavType.IntType })
        ) { backStackEntry ->
            val contractId = backStackEntry.arguments?.getInt("contractId") ?: 0
            RaiseDisputeScreen(
                contractId = contractId,
                onNavigateBack = { navController.popBackStack() },
                onDisputeRaised = {
                    navController.navigate(Screen.Disputes.route) {
                        popUpTo(Screen.ContractDetail.route) { inclusive = true }
                    }
                }
            )
        }
        
        // Profile
        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToEditProfile = { navController.navigate(Screen.EditProfile.route) },
                onNavigateToChangePassword = { navController.navigate(Screen.ChangePassword.route) },
                onNavigateToPrivacyPolicy = { navController.navigate(Screen.PrivacyPolicy.route) }
            )
        }
        
        composable(Screen.EditProfile.route) {
            EditProfileScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.ChangePassword.route) {
            ChangePasswordScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.PrivacyPolicy.route) {
            PrivacyPolicyScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Notifications
        composable(Screen.Notifications.route) {
            NotificationsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToNegotiation = { negotiationId ->
                    navController.navigate(Screen.NegotiationChat.createRoute(negotiationId))
                },
                onNavigateToContract = { contractId ->
                    navController.navigate(Screen.ContractDetail.createRoute(contractId))
                },
                onNavigateToListing = { listingId ->
                    navController.navigate(Screen.ListingDetail.createRoute(listingId))
                }
            )
        }
    }
}
