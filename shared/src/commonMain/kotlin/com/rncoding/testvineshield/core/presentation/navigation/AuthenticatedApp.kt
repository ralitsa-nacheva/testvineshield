package com.rncoding.testvineshield.core.presentation.navigation

import com.rncoding.testvineshield.core.presentation.disease_observations.BlockDiseaseObservationsScreen
import com.rncoding.testvineshield.core.presentation.disease_catalogue.DiseaseCatalogueScreenRoot
import com.rncoding.testvineshield.core.domain.auth.AuthenticatedUserProvider
import androidx.compose.material3.SnackbarHostState
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.repository.VineyardRepository
import com.rncoding.testvineshield.core.domain.repository.BlockRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.koin.compose.koinInject
import androidx.compose.runtime.*
import com.rncoding.testvineshield.core.presentation.harvest.*
import com.rncoding.testvineshield.core.domain.usecases.harvest.HarvestScope
import com.rncoding.testvineshield.core.presentation.activities.*
import com.rncoding.testvineshield.core.presentation.spray.SprayScreenRoot
import com.rncoding.testvineshield.core.presentation.spray.SprayExportScreenRoot
import com.rncoding.testvineshield.core.domain.usecases.activity.SprayKey
import com.rncoding.testvineshield.core.domain.usecases.activity.ActivityScope
import com.rncoding.testvineshield.core.presentation.blocks.*
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.rncoding.testvineshield.core.presentation.account.AccountScreenRoot
import com.rncoding.testvineshield.core.presentation.security.SecuritySettingsScreenRoot
import com.rncoding.testvineshield.core.presentation.vineyard_list.VineyardListScreenRoot
import com.rncoding.testvineshield.core.presentation.vineyard_list.VineyardSelectionPane
import com.rncoding.testvineshield.core.presentation.vineyard_editor.VineyardEditorMode
import com.rncoding.testvineshield.core.presentation.vineyard_editor.VineyardEditorScreenRoot
import com.rncoding.testvineshield.core.presentation.vineyard_details.VineyardDetailsScreenRoot
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator

@Composable
fun AuthenticatedApp(
    userId: Long,
    alertBadgeCount: Int = 0,
    activityBadgeCount: Int = 0,
    vineyardLinks: VineyardLinkInbox? = null
) {

    var busyRecordRoute by remember { mutableStateOf<AppRoute?>(null) }
    val navigationState =
        rememberVineShieldNavigationState()

    val availableDestinations = listOf(TopLevelDestination.VINEYARDS, TopLevelDestination.ACTIVITIES, TopLevelDestination.ACCOUNT)
    LaunchedEffect(navigationState.selectedDestination) {
        if (navigationState.selectedDestination !in availableDestinations)
            navigationState.selectDestination(TopLevelDestination.VINEYARDS)
    }

    val vineyards = koinInject<VineyardRepository>()
    val blocks = koinInject<BlockRepository>()
    val linkMessages = remember { SnackbarHostState() }
    var linkUnavailable by remember { mutableStateOf(false) }
    LaunchedEffect(linkUnavailable) {
        if (linkUnavailable) {
            linkMessages.showSnackbar("This record is unavailable for the current account.")
            linkUnavailable = false
        }
    }
    val users = koinInject<AuthenticatedUserProvider>()
    val pendingRequest = vineyardLinks?.pending
    LaunchedEffect(userId, pendingRequest, busyRecordRoute) {
        val inbox = vineyardLinks ?: return@LaunchedEffect
        val request = pendingRequest ?: return@LaunchedEffect
        if (busyRecordRoute != null || users.currentUserOrNull()?.userId != userId) return@LaunchedEffect
        val id = request.vineyardId

        val canOpen = try {
            val ownsVineyard = vineyards.getVineyard(userId, id) is Result.Success
            val blockId = request.blockId
            ownsVineyard && (blockId == null || blocks.getBlock(id, blockId) != null)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            false
        }
        currentCoroutineContext().ensureActive()
        if (busyRecordRoute != null || users.currentUserOrNull()?.userId != userId) return@LaunchedEffect
        if (!inbox.consume(request)) return@LaunchedEffect
        if (canOpen) {
            navigationState.selectDestination(TopLevelDestination.VINEYARDS)
            val route: AppRoute = request.blockId?.let { BlockDetailsRoute(id, it) }
                ?: VineyardDetailsRoute(id)
            if (navigationState.currentBackStack.lastOrNull() != route) {
                navigationState.navigate(route)
            }
        } else {
            linkUnavailable = true
        }
    }

    val selectDestination: (TopLevelDestination) -> Unit = { destination ->
        if (busyRecordRoute == null) navigationState.selectDestination(destination)
    }

    val topRoute = navigationState.currentBackStack.lastOrNull()
    val currentBlock = topRoute as? BlockDetailsRoute
    val selectedVineyardId = when (topRoute) {
        is VineyardDetailsRoute -> topRoute.vineyardId
        is BlockDetailsRoute -> topRoute.vineyardId
        else -> null
    }
    val vineyardListPane: (@Composable () -> Unit)? =
        if (navigationState.selectedDestination == TopLevelDestination.VINEYARDS && selectedVineyardId != null) {
            {
                VineyardSelectionPane(
                    userId = userId,
                    selectedVineyardId = selectedVineyardId,
                    enabled = busyRecordRoute == null,
                    onSelect = { vineyardId ->
                        val stack = navigationState.vineyardBackStack
                        if (busyRecordRoute == null && stack.lastOrNull() == topRoute &&
                            vineyardId != selectedVineyardId
                        ) {
                            val route = VineyardDetailsRoute(vineyardId)
                            val existingIndex = stack.lastIndexOf(route)
                            if (existingIndex >= 0) {
                                stack.subList(existingIndex + 1, stack.size).clear()
                            } else if (currentBlock != null) {
                                stack.add(route)
                            } else {
                                stack[stack.lastIndex] = route
                            }
                        }
                    }
                )
            }
        } else null
    val blockListPane: (@Composable () -> Unit)? =
        if (navigationState.selectedDestination == TopLevelDestination.VINEYARDS && currentBlock != null) {
            {
                BlockSelectionPane(
                    ownerId = userId,
                    vineyardId = currentBlock.vineyardId,
                    selectedBlockId = currentBlock.blockId,
                    enabled = busyRecordRoute == null,
                    onSelect = { blockId ->
                        val stack = navigationState.vineyardBackStack
                        if (busyRecordRoute == null && stack.lastOrNull() == currentBlock &&
                            blockId != currentBlock.blockId
                        ) {
                            val route = BlockDetailsRoute(currentBlock.vineyardId, blockId)
                            val existingIndex = stack.lastIndexOf(route)
                            if (existingIndex >= 0) stack.subList(existingIndex + 1, stack.size).clear()
                            else stack[stack.lastIndex] = route
                        }
                    }
                )
            }
        } else null

    AdaptiveVineShieldScaffold(
        navigationState.selectedDestination,
        alertBadgeCount,
        activityBadgeCount,
        selectDestination,
        availableDestinations,
        linkMessages,
        leadingPane = vineyardListPane,
        middlePane = blockListPane
    ) { contentModifier ->
        NavDisplay(
            backStack =
                navigationState.currentBackStack,

            onBack = {
                if(busyRecordRoute == null) navigationState.goBack()
            },

            modifier = contentModifier,

            entryDecorators =
                listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator()
                ),

            entryProvider =
                entryProvider {

                    entry<VineyardsRoute> {

                        VineyardListScreenRoot(
                            onOpenDiseaseCatalogue = { navigationState.navigate(DiseaseCatalogueRoute) },
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

                    entry<DiseaseCatalogueRoute> {
                        DiseaseCatalogueScreenRoot(
                            ownerId = userId,
                            onBack = { navigationState.goBack() },
                            onOpenDisease = { navigationState.navigate(BuiltInDiseaseDetailsRoute(it)) }
                        )
                    }

                    entry<BuiltInDiseaseDetailsRoute> { route ->
                        DiseaseCatalogueScreenRoot(
                            ownerId = userId,
                            diseaseId = route.diseaseId,
                            onBack = { navigationState.goBack() }
                        )
                    }

                    entry<VineyardWeatherRoute> { route ->
                        com.rncoding.testvineshield.core.presentation.weather.WeatherScreenRoot(userId, route.vineyardId,
                            onBack = { navigationState.goBack() })
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
                            onBlocks = { navigationState.navigate(BlocksRoute(route.vineyardId)) },
                            onActivities = { navigationState.navigate(ActivityListRoute(route.vineyardId)) },
                            onWeather = { navigationState.navigate(VineyardWeatherRoute(route.vineyardId)) }
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
                            onActivities={navigationState.navigate(ActivityListRoute(route.vineyardId,route.blockId))},
                            onHarvests={navigationState.navigate(HarvestListRoute(route.vineyardId,route.blockId))},
                            onDiseaseObservations={navigationState.navigate(BlockDiseaseObservationsRoute(route.vineyardId, route.blockId))},
                            onBusyChanged={ if(it) busyRecordRoute=route else if(busyRecordRoute==route) busyRecordRoute=null })
                    }
                    entry<BlockDiseaseObservationsRoute> { route ->
                        BlockDiseaseObservationsScreen(
                            userId, route.vineyardId, route.blockId,
                            onBack = { navigationState.goBack() },
                            onBusyChanged = {
                                if (it) busyRecordRoute = route
                                else if (busyRecordRoute == route) busyRecordRoute = null
                            }
                        )
                    }
                    entry<BlockEditorRoute> { route ->
                        BlockEditorScreenRoot(route.vineyardId,route.blockId,userId,onBack={navigationState.goBack()},
                            onSaved={navigationState.goBack()},onBusyChanged={ if(it) busyRecordRoute=route else if(busyRecordRoute==route) busyRecordRoute=null })
                    }

                    entry<HarvestListRoute> { route ->
                        HarvestListScreenRoot(userId, HarvestScope(route.vineyardId, route.blockId),
                            onBack = { navigationState.goBack() },
                            onCreate = { navigationState.navigate(HarvestEditorRoute(route.vineyardId, route.blockId)) },
                            onOpen = { navigationState.navigate(HarvestDetailsRoute(route.vineyardId, route.blockId, it)) },
                            onExport = { navigationState.navigate(HarvestExportRoute(route.vineyardId, route.blockId, it)) })
                    }
                    entry<HarvestExportRoute> { route ->
                        HarvestExportScreenRoot(userId, HarvestScope(route.vineyardId, route.blockId), route.season,
                            onBack = { navigationState.goBack() },
                            onBusyChanged = { if (it) busyRecordRoute = route else if (busyRecordRoute == route) busyRecordRoute = null })
                    }
                    entry<HarvestDetailsRoute> { route ->
                        HarvestDetailsScreenRoot(userId, HarvestKey(HarvestScope(route.vineyardId, route.blockId), route.harvestId),
                            onBack = { navigationState.goBack() },
                            onEdit = { navigationState.navigate(HarvestEditorRoute(route.vineyardId, route.blockId, route.harvestId)) },
                            onDeleted = { navigationState.goBack() },
                            onBusyChanged = { if (it) busyRecordRoute = route else if (busyRecordRoute == route) busyRecordRoute = null })
                    }
                    entry<HarvestEditorRoute> { route ->
                        HarvestEditorScreenRoot(userId, HarvestKey(HarvestScope(route.vineyardId, route.blockId), route.harvestId),
                            onBack = { navigationState.goBack() }, onSaved = { navigationState.goBack() },
                            onBusyChanged = { if (it) busyRecordRoute = route else if (busyRecordRoute == route) busyRecordRoute = null })
                    }

                    entry<ActivitiesRoute> {
                        ActivityListScreenRoot(userId,onVineyard={navigationState.navigate(ActivityListRoute(it))})
                    }
                    entry<ActivityListRoute> { route ->
                        ActivityListScreenRoot(userId,ActivityScope(route.vineyardId,route.blockId),onBack={navigationState.goBack()},
                            onCreate={navigationState.navigate(ActivityEditorRoute(route.vineyardId,route.blockId))},
                            onOpen={navigationState.navigate(ActivityDetailsRoute(route.vineyardId,it))})
                    }
                    entry<ActivityDetailsRoute> { route ->
                        ActivityDetailsScreenRoot(userId,route.vineyardId,route.activityId,onBack={navigationState.goBack()},
                            onEdit={navigationState.navigate(ActivityEditorRoute(route.vineyardId,activityId=route.activityId))},
                            onComplete={navigationState.navigate(ActivityEditorRoute(route.vineyardId,activityId=route.activityId,complete=true))},
                            onSpray={navigationState.navigate(SprayRoute(route.vineyardId,route.activityId))},
                            onDeleted={navigationState.goBack()},
                            onBusyChanged={if(it) busyRecordRoute=route else if(busyRecordRoute==route) busyRecordRoute=null})
                    }
                    entry<SprayRoute> { route ->
                        SprayScreenRoot(userId, SprayKey(route.vineyardId, route.activityId),
                            onBack = { navigationState.goBack() }, onFinished = { navigationState.goBack() },
                            onExport = { navigationState.navigate(SprayExportRoute(route.vineyardId, route.activityId)) },
                            onBusyChanged = { if (it) busyRecordRoute = route else if (busyRecordRoute == route) busyRecordRoute = null })
                    }
                    entry<SprayExportRoute> { route ->
                        SprayExportScreenRoot(userId, SprayKey(route.vineyardId, route.activityId),
                            onBack = { navigationState.goBack() },
                            onBusyChanged = { if (it) busyRecordRoute = route else if (busyRecordRoute == route) busyRecordRoute = null })
                    }
                    entry<ActivityEditorRoute> { route ->
                        ActivityEditorScreenRoot(userId,ActivityScope(route.vineyardId,route.blockId),route.activityId,route.complete,
                            onBack={navigationState.goBack()},onSaved={navigationState.goBack()},
                            onBusyChanged={if(it) busyRecordRoute=route else if(busyRecordRoute==route) busyRecordRoute=null})
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
