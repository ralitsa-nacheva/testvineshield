package com.rncoding.testvineshield.core.presentation.vineyard_list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import com.rncoding.testvineshield.core.presentation.DarkPurple
import com.rncoding.testvineshield.core.presentation.components.SearchBar
import com.rncoding.testvineshield.vineyard_details.domain.Vineyard

@Composable
fun VineyardListScreenRoot(
    viewModel: VineyardListViewModel = koinViewModel(),
    onVineyardClick: (Vineyard) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    VineyardListScreen(
        state = state,
        onAction = { action ->
            when(action) {
                is VineyardListAction.onVineyardClick -> onVineyardClick(action.vineyard)
                else -> Unit
            }
            viewModel.onAction(action)
        }
    )
}

@Composable
private fun VineyardListScreen(
    state: VineyardListState,
    onAction: (VineyardListAction) -> Unit,

) {
    val keyboardController = LocalSoftwareKeyboardController.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkPurple)
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SearchBar(
            searchQuery = state.searchQuery,
            onSearchQueryChange = {
               onAction(VineyardListAction.onSearchQueryChange(it))
            },
            onImeSearch = {
                keyboardController?.hide()
            },
            modifier = Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}

