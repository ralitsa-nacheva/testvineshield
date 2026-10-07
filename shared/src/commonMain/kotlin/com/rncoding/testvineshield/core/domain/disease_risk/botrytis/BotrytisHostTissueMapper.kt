package com.rncoding.testvineshield.core.domain.disease_risk.botrytis

import com.rncoding.testvineshield.core.domain.datamodels.enums.PhenologicalStage

object BotrytisHostTissueMapper {

    fun map(
        phenologicalStage: PhenologicalStage?
    ): BotrytisHostTissue {

        return when (phenologicalStage) {

            PhenologicalStage.FLOWERING ->
                BotrytisHostTissue.FLOWERS

            PhenologicalStage.VERAISON,
            PhenologicalStage.RIPENING,
            PhenologicalStage.HARVEST ->
                BotrytisHostTissue.MATURE_BERRIES

            else ->
                BotrytisHostTissue.UNSUPPORTED
        }
    }
}