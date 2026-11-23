package com.rncoding.testvineshield.core.presentation.vineyards

import androidx.compose.runtime.Composable
import com.rncoding.testvineshield.resources.*
import org.jetbrains.compose.resources.stringResource

/** Observation failures have no typed domain result yet; never expose a Throwable message. */
enum class VineyardLoadIssue { Failed, NotFound }

@Composable
internal fun localizedVineyardLoadIssue(issue: VineyardLoadIssue): String = stringResource(when (issue) {
    VineyardLoadIssue.Failed -> Res.string.vineyard_load_failed
    VineyardLoadIssue.NotFound -> Res.string.vineyard_load_not_found
})
