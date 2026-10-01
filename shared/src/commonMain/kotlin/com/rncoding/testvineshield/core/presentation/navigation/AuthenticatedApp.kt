package com.rncoding.testvineshield.core.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.rncoding.testvineshield.core.presentation.account.AccountScreenRoot
import com.rncoding.testvineshield.core.presentation.security.SecuritySettingsScreenRoot
import com.rncoding.testvineshield.core.presentation.vineyard_list.VineyardListScreenRoot
import com.rncoding.testvineshield.core.presentation.vineyard_editor.VineyardEditorMode
import com.rncoding.testvineshield.core.presentation.vineyard_editor.VineyardEditorScreenRoot
import com.rncoding.testvineshield.core.presentation.vineyard_details.VineyardDetailsScreenRoot
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator

@Composable
fun AuthenticatedApp(
    userId: Long,
    alertBadgeCount: Int = 0,
    activityBadgeCount: Int = 0
) {

    val navigationState =
        rememberVineShieldNavigationState()

    Scaffold(
        bottomBar = {

            VineShieldNavigationBar(
                selectedDestination =
                    navigationState.selectedDestination,
                alertBadgeCount =
                    alertBadgeCount,
                activityBadgeCount =
                    activityBadgeCount,
                onDestinationSelected =
                    navigationState::selectDestination
            )
        }
    ) { innerPadding ->

        NavDisplay(
            backStack =
                navigationState.currentBackStack,

            onBack = {
                navigationState.goBack()
            },

            modifier =
                Modifier.padding(
                    innerPadding
                ),

            entryDecorators =
                listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator()
                ),

            entryProvider =
                entryProvider {

                    entry<VineyardsRoute> {

                        VineyardListScreenRoot(
                            onVineyardClick = { vineyardId ->

                                navigationState.navigate(
                                    VineyardDetailsRoute(
                                        vineyardId =
                                            vineyardId
                                    )
                                )
                            },
                            onCreateVineyard = {

                                navigationState.navigate(
                                    CreateVineyardRoute
                                )
                            }
                        )
                    }

                    entry<VineyardDetailsRoute> { route ->
                        VineyardDetailsScreenRoot(
                            vineyardId =
                                route.vineyardId,

                            onBack = {
                                navigationState.goBack()
                            },

                            onEdit = {
                                navigationState.navigate(
                                    EditVineyardRoute(
                                        vineyardId =
                                            route.vineyardId
                                    )
                                )
                            },

                            onDeleted = {
                                navigationState.goBack()
                            }
                        )
                    }

                    entry<CreateVineyardRoute> {
                        VineyardEditorScreenRoot(
                            mode = VineyardEditorMode.Create,
                            onBack = {
                                navigationState.goBack()
                            },
                            onSaved = {
                                navigationState.goBack()
                            }
                        )
                    }

                    entry<EditVineyardRoute> { route ->
                        VineyardEditorScreenRoot(
                            mode =
                                VineyardEditorMode.Edit(
                                    vineyardId = route.vineyardId
                                ),
                            onBack = {
                                navigationState.goBack()
                            },
                            onSaved = {
                                navigationState.goBack()
                            }
                        )
                    }

                    entry<ActivitiesRoute> {

                        FeaturePlaceholderScreen(
                            title = "Activities"
                        )
                    }

                    entry<ActivityDetailsRoute> { route ->

                        FeaturePlaceholderScreen(
                            title =
                                "Activity ${route.activityId}"
                        )
                    }

                    entry<AlertsRoute> {

                        FeaturePlaceholderScreen(
                            title = "Alerts"
                        )
                    }

                    entry<DiseaseAlertDetailsRoute> { route ->

                        FeaturePlaceholderScreen(
                            title =
                                "Disease alert ${route.alertId}"
                        )
                    }

                    entry<WeatherAlertDetailsRoute> { route ->

                        FeaturePlaceholderScreen(
                            title =
                                "Weather alert ${route.alertId}"
                        )
                    }

                    entry<AccountRoute> {

                        AccountScreenRoot(
                            onBack = {
                                // Account is a top-level screen.
                            },
                            onSecuritySettings = {

                                navigationState.navigate(
                                    SecuritySettingsRoute
                                )
                            }
                        )
                    }

                    entry<SecuritySettingsRoute> {

                        SecuritySettingsScreenRoot(
                            userId = userId,
                            onBack = {
                                navigationState.goBack()
                            }
                        )
                    }
                }
        )
    }
}