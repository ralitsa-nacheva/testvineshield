package com.rncoding.testvineshield.core.presentation.disease_catalogue

import com.rncoding.testvineshield.core.presentation.components.InfoLabel
import com.rncoding.testvineshield.core.presentation.components.InfoHelp
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
                DiseaseCatalogueState.Loading -> item(key = "fixed:DiseaseCatalogueScreen:1", contentType = "fixed:DiseaseCatalogueScreen:1") { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                DiseaseCatalogueState.Unavailable -> item(key = "fixed:DiseaseCatalogueScreen:2", contentType = "fixed:DiseaseCatalogueScreen:2") {
                    Text(stringResource(Res.string.catalogue_load_error))
                    TextButton(onClick = userAction(viewModel::reload)) { Text(stringResource(Res.string.retry)) }
                }
                is DiseaseCatalogueState.Loaded -> if (diseaseId == null) {
                    if (current.diseases.isEmpty()) item(key = "fixed:DiseaseCatalogueScreen:3", contentType = "fixed:DiseaseCatalogueScreen:3") { Text(stringResource(Res.string.observation_no_diseases)) }
                    items(current.diseases, key = { "catalogue-disease:${it.diseaseId}" }, contentType = { "catalogue-disease" }) { disease ->
                        OutlinedCard(
                            onClick = userAction { onOpenDisease(disease.diseaseId) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                InfoLabel(title = builtInDiseaseName(disease.code, disease.name), text = catalogueParagraph(disease, CatalogueParagraph.DESCRIPTION), style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                } else {
                    val disease = current.diseases.firstOrNull { it.diseaseId == diseaseId }
                    if (disease == null) item(key = "fixed:DiseaseCatalogueScreen:4", contentType = "fixed:DiseaseCatalogueScreen:4") { Text(stringResource(Res.string.catalogue_unavailable)) }
                    else {
                        item(key = "fixed:DiseaseCatalogueScreen:5", contentType = "fixed:DiseaseCatalogueScreen:5") { Text(builtInDiseaseName(disease.code, disease.name), style = MaterialTheme.typography.headlineSmall) }
                        item(key = "fixed:DiseaseCatalogueScreen:6", contentType = "fixed:DiseaseCatalogueScreen:6") { InfoHelp(title = stringResource(Res.string.info_reference), text = catalogueParagraph(disease, CatalogueParagraph.DESCRIPTION)) }
                        val symptoms = builtInSymptomReference(disease.code)
                        if (symptoms.isNotEmpty()) {
                            item(key = "fixed:DiseaseCatalogueScreen:7", contentType = "fixed:DiseaseCatalogueScreen:7") {
                                InfoLabel(title = stringResource(Res.string.catalogue_signs), text = stringResource(Res.string.catalogue_signs_help), style = MaterialTheme.typography.titleLarge)

                            }
                            items(symptoms, key = { "catalogue-symptom:${it.key}" }, contentType = { "catalogue-symptom" }) { symptom ->
                                CatalogueDetail(stringResource(symptom.plantPart), stringResource(symptom.description))
                            }
                            item(key = "fixed:DiseaseCatalogueScreen:8", contentType = "fixed:DiseaseCatalogueScreen:8") {
                                Text(stringResource(Res.string.catalogue_source), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (disease.vectors.isNotBlank()) item(key = "fixed:DiseaseCatalogueScreen:9", contentType = "fixed:DiseaseCatalogueScreen:9") { CatalogueDetail(stringResource(Res.string.catalogue_spread), catalogueParagraph(disease, CatalogueParagraph.SPREAD)) }
                        if (disease.factors.isNotBlank()) item(key = "fixed:DiseaseCatalogueScreen:10", contentType = "fixed:DiseaseCatalogueScreen:10") { CatalogueDetail(stringResource(Res.string.catalogue_conditions), catalogueParagraph(disease, CatalogueParagraph.CONDITIONS)) }
                        if (disease.treatmentSuggestion.isNotBlank()) item(key = "fixed:DiseaseCatalogueScreen:11", contentType = "fixed:DiseaseCatalogueScreen:11") {
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
    InfoLabel(title = label, text = description, style = MaterialTheme.typography.titleMedium)
}
