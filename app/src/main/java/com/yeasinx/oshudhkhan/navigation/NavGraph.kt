package com.yeasinx.oshudhkhan.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavHost
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
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
    object AddMedicine : Screen("add_medicine?medicineId={medicineId}") {
        const val ADD = "add_medicine"
        fun editRoute(id: Int) = "add_medicine?medicineId=$id"
    }
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
                onAddMedicine = { navController.navigate(Screen.AddMedicine.ADD) },
                onEditMedicine = { navController.navigate(Screen.AddMedicine.editRoute(it.id)) }
            )
        }
        composable(
            Screen.AddMedicine.route,
            arguments = listOf(navArgument("medicineId") {
                type = NavType.IntType
                defaultValue = -1
            })
        ) { backStackEntry ->
            AddMedicineScreen(
                viewModel = viewModel,
                medicineId = backStackEntry.arguments?.getInt("medicineId")?.takeIf { it > 0 },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Profile.route) {
            ProfileScreen(viewModel = profileViewModel)
        }
    }
}