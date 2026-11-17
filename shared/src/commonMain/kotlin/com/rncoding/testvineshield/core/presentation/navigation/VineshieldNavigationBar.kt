package com.rncoding.testvineshield.core.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.VerticalDivider
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.rncoding.testvineshield.core.presentation.root.userAction

@Composable
fun VineShieldNavigationBar(
    selectedDestination: TopLevelDestination,
    alertBadgeCount: Int,
    activityBadgeCount: Int,
    onDestinationSelected: (TopLevelDestination) -> Unit,
    destinations: List<TopLevelDestination> = TopLevelDestination.entries
) {
    NavigationBar {
        destinations.forEach { destination ->
            NavigationBarItem(
                selected = selectedDestination == destination,
                onClick = userAction { onDestinationSelected(destination) },
                icon = { DestinationBadge(destination, alertBadgeCount, activityBadgeCount) },
                label = { Text(destination.label) }
            )
        }
    }
}

@Composable
internal fun VineShieldNavigationRail(
    selectedDestination: TopLevelDestination,
    alertBadgeCount: Int,
    activityBadgeCount: Int,
    onDestinationSelected: (TopLevelDestination) -> Unit,
    destinations: List<TopLevelDestination>
) {
    NavigationRail {
        destinations.forEach { destination ->
            NavigationRailItem(
                selected = selectedDestination == destination,
                onClick = userAction { onDestinationSelected(destination) },
                icon = { DestinationBadge(destination, alertBadgeCount, activityBadgeCount) },
                label = { Text(destination.label) }
            )
        }
    }
}

@Composable
private fun DestinationBadge(
    destination: TopLevelDestination,
    alertBadgeCount: Int,
    activityBadgeCount: Int
) {
    val count = when (destination) {
        TopLevelDestination.ALERTS -> alertBadgeCount
        TopLevelDestination.ACTIVITIES -> activityBadgeCount
        else -> 0
    }
    BadgedBox(badge = {
        if (count > 0) Badge { Text(if (count > 99) "99+" else count.toString()) }
    }) {
        Text(destination.label.take(1))
    }
}

@Composable
internal fun AdaptiveVineShieldScaffold(
    selectedDestination: TopLevelDestination,
    alertBadgeCount: Int,
    activityBadgeCount: Int,
    onDestinationSelected: (TopLevelDestination) -> Unit,
    destinations: List<TopLevelDestination>,
    snackbarHostState: SnackbarHostState,
    leadingPane: (@Composable () -> Unit)? = null,
    content: @Composable (Modifier) -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp && maxHeight >= 480.dp
        val showVineyardPane = useRail && maxWidth >= 900.dp
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (!useRail) {
                    VineShieldNavigationBar(
                        selectedDestination, alertBadgeCount, activityBadgeCount,
                        onDestinationSelected, destinations
                    )
                }
            }
        ) { padding ->
            Row(Modifier.fillMaxSize().padding(padding)) {
                if (useRail) {
                    VineShieldNavigationRail(
                        selectedDestination, alertBadgeCount, activityBadgeCount,
                        onDestinationSelected, destinations
                    )
                }
                if (showVineyardPane && leadingPane != null) {
                    Box(Modifier.width(320.dp).fillMaxHeight()) { leadingPane() }
                    VerticalDivider()
                }
                content(Modifier.weight(1f).fillMaxSize())
            }
        }
    }
}
