package com.yeasinx.oshudhkhan.navigation

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

private val BAR_HEIGHT = 64.dp
private val BAR_MARGIN = 16.dp

// Vertical space the floating bar occupies above the system navigation inset.
val NAV_BAR_SPACE = BAR_HEIGHT + BAR_MARGIN * 2

sealed class BottomNavItem(val title: String, val icon: ImageVector, val route: String) {
    object Home : BottomNavItem("Home", Icons.Default.Home, Screen.Home.route)
    object Medicines : BottomNavItem("Medicines", Icons.AutoMirrored.Filled.List, Screen.MedicineList.route)
    object Profile : BottomNavItem("Profile", Icons.Default.Person, Screen.Profile.route)
}

@Composable
fun FloatingBottomNavBar(
    navController: NavController,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Medicines,
        BottomNavItem.Profile
    )
    val backStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry.value?.destination?.route

    // Hide bottom nav on the add/edit form
    if (currentRoute == Screen.AddMedicine.route) {
        return
    }

    Row(
        modifier = modifier
            .navigationBarsPadding()
            .padding(BAR_MARGIN)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .height(BAR_HEIGHT)
                .glass(hazeState, RoundedCornerShape(BAR_HEIGHT / 2))
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                NavTab(
                    item = item,
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

        Box(
            modifier = Modifier
                .size(BAR_HEIGHT)
                .glass(hazeState, CircleShape)
                .clickable(role = Role.Button) { navController.navigate(Screen.AddMedicine.ADD) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add medicine",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

// Icon-only when idle; the selected tab grows into a tinted pill with its label.
@Composable
private fun RowScope.NavTab(item: BottomNavItem, selected: Boolean, onClick: () -> Unit) {
    val weight by animateFloatAsState(if (selected) 1.7f else 1f, label = "tabWeight")
    val tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .weight(weight)
            .fillMaxHeight()
            // Whole column is the touch target; the pill animation is the feedback
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = null,
                indication = null
            )
            .semantics { contentDescription = item.title },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent)
                .animateContentSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = item.icon, contentDescription = null, tint = tint)
            if (selected) {
                Spacer(Modifier.width(6.dp))
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = tint,
                    maxLines = 1
                )
            }
        }
    }
}

// Frosted glass: blurs the content behind (Android 12+; translucent tint on older versions).
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
private fun Modifier.glass(hazeState: HazeState, shape: Shape): Modifier = this
    .clip(shape)
    .hazeEffect(state = hazeState, style = HazeMaterials.thin())
    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), shape)
