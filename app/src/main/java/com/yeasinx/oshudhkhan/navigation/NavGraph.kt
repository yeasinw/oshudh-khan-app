package com.yeasinx.oshudhkhan.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavHost
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.yeasinx.oshudhkhan.ui.home.HomeViewModel
import com.yeasinx.oshudhkhan.ui.medicine.MedicineViewModel
import com.yeasinx.oshudhkhan.ui.profile.ProfileViewModel
import com.yeasinx.oshudhkhan.ui.screens.AddMedicineScreen
import com.yeasinx.oshudhkhan.ui.screens.MedicineListScreen

import com.yeasinx.oshudhkhan.ui.screens.HomeScreen
import com.yeasinx.oshudhkhan.ui.screens.ProfileScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object MedicineList : Screen("medicine_list")
    object AddMedicine : Screen("add_medicine")
    object Profile : Screen("profile")
}

@Composable
fun MedicineNavGraph(
    navController: NavHostController,
    viewModel: MedicineViewModel,
    homeViewModel: HomeViewModel,
    profileViewModel: ProfileViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = homeViewModel,
                onSetUpProfile = {
                    navController.navigate(Screen.Profile.route) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
        composable(Screen.MedicineList.route) {
            MedicineListScreen(
                viewModel = viewModel,
                onAddMedicine = { navController.navigate(Screen.AddMedicine.route) }
            )
        }
        composable(Screen.AddMedicine.route) {
            AddMedicineScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Profile.route) {
            ProfileScreen(viewModel = profileViewModel)
        }
    }
}