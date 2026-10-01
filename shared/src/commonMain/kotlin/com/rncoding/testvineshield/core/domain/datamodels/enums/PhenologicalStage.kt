package com.rncoding.testvineshield.core.domain.datamodels.enums

enum class PhenologicalStage(
    val dbValue: String
) {
    DORMANCY("dormancy"),
    BUD_SWELL("bud_swell"),
    BUD_BURST("bud_burst"),
    LEAF_DEVELOPMENT("leaf_development"),
    INFLORESCENCE_DEVELOPMENT("inflorescence_development"),
    FLOWERING("flowering"),
    FRUIT_SET("fruit_set"),
    BERRY_DEVELOPMENT("berry_development"),
    VERAISON("veraison"),
    RIPENING("ripening"),
    HARVEST("harvest"),
    POST_HARVEST("post_harvest"),
    LEAF_FALL("leaf_fall");

    companion object {
        fun fromDbValue(
            value: String
        ): PhenologicalStage =
            entries.firstOrNull {
                it.dbValue == value
            } ?: throw IllegalArgumentException(
                "Unknown PhenologicalStage database value: $value"
            )
    }
}