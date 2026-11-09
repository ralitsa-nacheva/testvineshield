package com.rncoding.testvineshield.core.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import com.rncoding.testvineshield.core.presentation.blocks.*
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

    var busyBlockRoute by remember { mutableStateOf<AppRoute?>(null) }
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
                    { if(busyBlockRoute == null) navigationState.selectDestination(it) }
            )
        }
    ) { innerPadding ->

        NavDisplay(
            backStack =
                navigationState.currentBackStack,

            onBack = {
                if(busyBlockRoute == null) navigationState.goBack()
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
                            },
                            onBlocks = { navigationState.navigate(BlocksRoute(route.vineyardId)) }
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

                    entry<BlocksRoute> { route ->
                        BlockListScreenRoot(route.vineyardId,userId,onBack={navigationState.goBack()},
                            onCreate={navigationState.navigate(BlockEditorRoute(route.vineyardId))},
                            onOpen={navigationState.navigate(BlockDetailsRoute(route.vineyardId,it))})
                    }
                    entry<BlockDetailsRoute> { route ->
                        BlockDetailsScreenRoot(route.vineyardId,route.blockId,userId,onBack={navigationState.goBack()},
                            onEdit={navigationState.navigate(BlockEditorRoute(route.vineyardId,route.blockId))},
                            onDeleted={navigationState.goBack()},
                            onBusyChanged={ if(it) busyBlockRoute=route else if(busyBlockRoute==route) busyBlockRoute=null })
                    }
                    entry<BlockEditorRoute> { route ->
                        BlockEditorScreenRoot(route.vineyardId,route.blockId,userId,onBack={navigationState.goBack()},
                            onSaved={navigationState.goBack()},onBusyChanged={ if(it) busyBlockRoute=route else if(busyBlockRoute==route) busyBlockRoute=null })
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