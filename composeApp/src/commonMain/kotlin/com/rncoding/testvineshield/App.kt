package com.rncoding.testvineshield

import androidx.compose.runtime.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.rncoding.testvineshield.core.presentation.vineyard_list.VineyardListScreenRoot
import com.rncoding.testvineshield.core.presentation.vineyard_list.VineyardListViewModel

@Composable
@Preview
fun App() {
    VineyardListScreenRoot(
        viewModel = remember { VineyardListViewModel() },
        onVineyardClick = {

        }
    )

}