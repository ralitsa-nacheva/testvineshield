package com.rncoding.testvineshield.core.data.local.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rncoding.testvineshield.core.data.local.database.entities.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: SessionEntity)

    @Query("SELECT * FROM session LIMIT 1")
    suspend fun observeSession(): Flow<SessionEntity?>

    @Query("SELECT * FROM session LIMIT 1")
    suspend fun getSession(): SessionEntity?

    @Query("DELETE FROM session")
    suspend fun clearSession()
}