
package com.rncoding.testvineshield.core.data.local.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.rncoding.testvineshield.core.data.local.database.entities.OccurrenceActivityCrossRef

@Dao
interface ActivityTestFixtureDao {
    @Insert
    suspend fun insertLink(link: OccurrenceActivityCrossRef)

    @Query(
        """
        SELECT COUNT(*)
        FROM OccurrenceActivityCrossRef
        WHERE occurrence_id = :occurrenceId
          AND activity_id = :activityId
        """
    )
    suspend fun countLinks(
        occurrenceId: Long,
        activityId: Long
    ): Int
}
