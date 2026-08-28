package com.rncoding.testvineshield.core.data.local.database.models

import com.rncoding.testvineshield.core.data.local.database.relations.BlockWithActivities
// this class duplicates vineyardwithsummaryaggregate

data class VineyardDetailedSummary(val vineyardId: Long,
                                   val userId: Long,
                                   val name: String,
                                   val country: String,
                                   val city: String,
                                   val blockWithActivities: List<BlockWithActivities>, //create dao function
                                   // provide better data fields below
                                   val lastActivity: String, //create dao function
                                   val latestTemperature: Double, // create dao function
                                   val activeAlert: String, //create dao function
                                   val currentDiseases: String //create dao function
                                     )
