package com.rncoding.testvineshield.core.presentation.disease_catalogue

import androidx.compose.foundation.layout.*
import com.rncoding.testvineshield.resources.*
import org.jetbrains.compose.resources.stringResource
import com.rncoding.testvineshield.core.presentation.disease_observations.builtInDiseaseName
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

    Scaffold(topBar = { RecordTopBar(if (diseaseId == null) stringResource(Res.string.catalogue_title) else stringResource(Res.string.catalogue_details), onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxHeight().widthIn(max = 720.dp).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (val current = state) {
                DiseaseCatalogueState.Loading -> item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                DiseaseCatalogueState.Unavailable -> item {
                    Text(stringResource(Res.string.catalogue_load_error))
                    TextButton(onClick = userAction(viewModel::reload)) { Text(stringResource(Res.string.retry)) }
                }
                is DiseaseCatalogueState.Loaded -> if (diseaseId == null) {
                    if (current.diseases.isEmpty()) item { Text(stringResource(Res.string.observation_no_diseases)) }
                    items(current.diseases, key = { it.diseaseId }) { disease ->
                        OutlinedCard(
                            onClick = userAction { onOpenDisease(disease.diseaseId) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(builtInDiseaseName(disease.code, disease.name), style = MaterialTheme.typography.titleMedium)
                                Text(catalogueParagraph(disease, CatalogueParagraph.DESCRIPTION))
                            }
                        }
                    }
                } else {
                    val disease = current.diseases.firstOrNull { it.diseaseId == diseaseId }
                    if (disease == null) item { Text(stringResource(Res.string.catalogue_unavailable)) }
                    else {
                        item { Text(builtInDiseaseName(disease.code, disease.name), style = MaterialTheme.typography.headlineSmall) }
                        item { Text(catalogueParagraph(disease, CatalogueParagraph.DESCRIPTION)) }
                        val symptoms = builtInSymptomReference(disease.code)
                        if (symptoms.isNotEmpty()) {
                            item {
                                Text(stringResource(Res.string.catalogue_signs), style = MaterialTheme.typography.titleLarge)
                                Text(stringResource(Res.string.catalogue_signs_help))
                            }
                            items(symptoms, key = { it.key }) { symptom ->
                                CatalogueDetail(stringResource(symptom.plantPart), stringResource(symptom.description))
                            }
                            item {
                                Text(stringResource(Res.string.catalogue_source), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (disease.vectors.isNotBlank()) item { CatalogueDetail(stringResource(Res.string.catalogue_spread), catalogueParagraph(disease, CatalogueParagraph.SPREAD)) }
                        if (disease.factors.isNotBlank()) item { CatalogueDetail(stringResource(Res.string.catalogue_conditions), catalogueParagraph(disease, CatalogueParagraph.CONDITIONS)) }
                        if (disease.treatmentSuggestion.isNotBlank()) item {
                            CatalogueDetail(stringResource(Res.string.catalogue_management), catalogueParagraph(disease, CatalogueParagraph.MANAGEMENT))
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
