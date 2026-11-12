package com.rncoding.testvineshield.core.presentation.disease_catalogue

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rncoding.testvineshield.core.presentation.components.RecordTopBar
import com.rncoding.testvineshield.core.presentation.root.userAction
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DiseaseCatalogueScreenRoot(
    ownerId: Long,
    onBack: () -> Unit,
    diseaseId: Long? = null,
    onOpenDisease: (Long) -> Unit = {}
) {
    val viewModel = koinViewModel<DiseaseCatalogueViewModel>(key = "disease-catalogue-$ownerId-$diseaseId")
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(topBar = { RecordTopBar(if (diseaseId == null) "Disease catalogue" else "Disease details", onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxHeight().widthIn(max = 720.dp).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (val current = state) {
                DiseaseCatalogueState.Loading -> item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                DiseaseCatalogueState.Unavailable -> item {
                    Text("The disease catalogue could not be loaded.")
                    TextButton(onClick = userAction(viewModel::reload)) { Text("Retry") }
                }
                is DiseaseCatalogueState.Loaded -> if (diseaseId == null) {
                    if (current.diseases.isEmpty()) item { Text("No built-in diseases are available.") }
                    items(current.diseases, key = { it.diseaseId }) { disease ->
                        OutlinedCard(
                            onClick = userAction { onOpenDisease(disease.diseaseId) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(disease.name, style = MaterialTheme.typography.titleMedium)
                                Text(disease.description)
                            }
                        }
                    }
                } else {
                    val disease = current.diseases.firstOrNull { it.diseaseId == diseaseId }
                    if (disease == null) item { Text("This built-in disease is unavailable.") }
                    else {
                        item { Text(disease.name, style = MaterialTheme.typography.headlineSmall) }
                        item { Text(disease.description) }
                        if (disease.vectors.isNotBlank()) item { CatalogueDetail("How it spreads", disease.vectors) }
                        if (disease.factors.isNotBlank()) item { CatalogueDetail("Conditions that favor it", disease.factors) }
                        if (disease.treatmentSuggestion.isNotBlank()) item {
                            CatalogueDetail("General management information", disease.treatmentSuggestion)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogueDetail(label: String, description: String) {
    Text(label, style = MaterialTheme.typography.titleMedium)
    Text(description)
}
