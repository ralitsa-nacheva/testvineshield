package com.rncoding.testvineshield.core.presentation.navigation

import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun VineShieldNavigationBar(
    selectedDestination: TopLevelDestination,
    alertBadgeCount: Int,
    activityBadgeCount: Int,
    onDestinationSelected: (TopLevelDestination) -> Unit
) {
    NavigationBar {

        TopLevelDestination.entries.forEach { destination ->

            val badgeCount =
                when (destination) {
                    TopLevelDestination.ALERTS ->
                        alertBadgeCount

                    TopLevelDestination.ACTIVITIES ->
                        activityBadgeCount

                    else -> 0
                }

            NavigationBarItem(
                selected =
                    selectedDestination == destination,

                onClick = {
                    onDestinationSelected(destination)
                },

                icon = {
                    BadgedBox(
                        badge = {
                            if (badgeCount > 0) {
                                Badge {
                                    Text(
                                        if (badgeCount > 99) {
                                            "99+"
                                        } else {
                                            badgeCount.toString()
                                        }
                                    )
                                }
                            }
                        }
                    ) {
                        Text(
                            text = destination.label
                                .take(1)
                        )
                    }
                },

                label = {
                    Text(destination.label)
                }
            )
        }
    }
}