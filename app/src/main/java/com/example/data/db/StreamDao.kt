package com.example.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StreamDao {
    @Query("SELECT * FROM streams ORDER BY isPreferred DESC, isHealthy DESC, rank ASC, failureCount ASC")
    fun getAllStreamsFlow(): Flow<List<StreamEntity>>

    @Query("SELECT * FROM streams ORDER BY isPreferred DESC, isHealthy DESC, rank ASC, failureCount ASC")
    suspend fun getAllStreams(): List<StreamEntity>

    @Query("SELECT * FROM streams WHERE isPreferred = 1 LIMIT 1")
    suspend fun getPreferredStream(): StreamEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreams(streams: List<StreamEntity>)

    @Update
    suspend fun updateStream(stream: StreamEntity)

    @Query("UPDATE streams SET failureCount = :failureCount, isHealthy = :isHealthy WHERE id = :id")
    suspend fun updateStreamStatus(id: Int, failureCount: Int, isHealthy: Boolean)

    @Query("UPDATE streams SET isPreferred = (url = :preferredUrl)")
    suspend fun setPreferredStreamUrl(preferredUrl: String)

    @Query("UPDATE streams SET failureCount = 0, isHealthy = 1")
    suspend fun resetAllStreamStatuses()
}
