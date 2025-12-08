package com.stellarforge.composebooking.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.stellarforge.composebooking.ui.navigation.BottomNavItem

/**
 * A dynamic Bottom Navigation Bar that adapts to the User Role.
 *
 * **Features:**
 * - **Role-Based Menus:** Displays "Booking/History" for Customers, and "Schedule/Management" for Owners.
 * - **State Persistence:** Maintains the state of each tab (using `saveState` and `restoreState`) to prevent reloading data unnecessarily.
 * - **Localization:** Uses String Resources for tab titles.
 *
 * @param userRole Determines which menu set to display ("owner" or "customer").
 */
@Composable
fun AppBottomNavigationBar(
    navController: NavController,
    userRole: String
) {
    // Determine the menu items based on the logged-in role
    val items = if (userRole == "owner") {
        listOf(
            BottomNavItem.Schedule,
            BottomNavItem.ManageServices,
            BottomNavItem.BusinessProfile
        )
    } else {
        listOf(
            BottomNavItem.CustomerHome,
            BottomNavItem.MyBookings,
            BottomNavItem.CustomerProfile
        )
    }

    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        items.forEach { screen ->
            // Check if this item is currently selected
            val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true

            // Get localized title
            val title = stringResource(id = screen.titleResId)

            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = title) },
                label = { Text(title) },
                selected = isSelected,
                onClick = {
                    navController.navigate(screen.route) {
                        // Pop up to the start destination of the graph to
                        // avoid building up a large stack of destinations
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        // Avoid multiple copies of the same destination when
                        // re-selecting the same item
                        launchSingleTop = true
                        // Restore state when re-selecting a previously selected item
                        restoreState = true
                    }
                }
            )
        }
    }
}