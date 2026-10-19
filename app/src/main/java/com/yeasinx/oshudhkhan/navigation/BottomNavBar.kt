package com.yeasinx.oshudhkhan.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

sealed class BottomNavItem(val title: String, val icon: ImageVector, val route: String) {
    object Home : BottomNavItem("Home", Icons.Default.Home, Screen.Home.route)
    object Medicines : BottomNavItem("Medicines", Icons.AutoMirrored.Filled.List, Screen.MedicineList.route)
    object Profile : BottomNavItem("Profile", Icons.Default.Person, Screen.Profile.route)
}

@Composable
fun FloatingBottomNavBar(navController: NavController) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Medicines,
        BottomNavItem.Profile
    )
    val backStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry.value?.destination?.route

    // Hide bottom nav on specific screens (like AddMedicine)
    if (currentRoute == Screen.AddMedicine.route) {
        return
    }

    val shape = RoundedCornerShape(46.dp)

    NavigationBar(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp))
            .clip(shape)
            .height(72.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        windowInsets = WindowInsets(0, 0, 0, 0)
    ) {
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(imageVector = item.icon, contentDescription = item.title) },
                label = { Text(text = item.title) },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        navController.graph.startDestinationRoute?.let { route ->
                            popUpTo(route) {
                                saveState = true
                            }
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
