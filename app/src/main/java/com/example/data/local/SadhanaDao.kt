package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SadhanaDao {

    @Query("SELECT * FROM sankalps ORDER BY isActive DESC, createdAtMillis DESC")
    fun observeAllSankalps(): Flow<List<SankalpEntity>>

    @Query("SELECT * FROM japa_sessions ORDER BY startTimeMillis DESC, id DESC")
    fun observeAllSessions(): Flow<List<JapaSessionEntity>>

    @Query("SELECT * FROM japa_sessions WHERE sankalpId = :sankalpId ORDER BY startTimeMillis DESC, id DESC")
    fun observeSessionsForSankalp(sankalpId: Long): Flow<List<JapaSessionEntity>>

    @Query("SELECT * FROM sankalps ORDER BY id ASC")
    suspend fun getAllSankalpsOnce(): List<SankalpEntity>

    @Query("SELECT * FROM japa_sessions ORDER BY id ASC")
    suspend fun getAllSessionsOnce(): List<JapaSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSankalp(sankalp: SankalpEntity): Long

    @Update
    suspend fun updateSankalp(sankalp: SankalpEntity)

    @Query("DELETE FROM sankalps WHERE id = :sankalpId")
    suspend fun deleteSankalpById(sankalpId: Long)

    @Query("UPDATE sankalps SET isActive = 0")
    suspend fun clearAllActiveFlags()

    @Query("UPDATE sankalps SET isActive = 1 WHERE id = :sankalpId")
    suspend fun markSankalpActive(sankalpId: Long)

    @Transaction
    suspend fun setActiveSankalp(sankalpId: Long) {
        clearAllActiveFlags()
        markSankalpActive(sankalpId)
    }

    @Transaction
    suspend fun createAndActivateSankalp(sankalp: SankalpEntity): Long {
        clearAllActiveFlags()
        return insertSankalp(sankalp.copy(isActive = true))
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: JapaSessionEntity): Long

    @Update
    suspend fun updateSession(session: JapaSessionEntity)

    @Query("DELETE FROM japa_sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: Long)

    @Query("DELETE FROM japa_sessions")
    suspend fun deleteAllSessions()

    @Query("DELETE FROM sankalps")
    suspend fun deleteAllSankalps()

    @Transaction
    suspend fun restoreBackup(
        sankalpsWithSessions: List<Pair<SankalpEntity, List<JapaSessionEntity>>>,
        replaceExisting: Boolean
    ) {
        if (replaceExisting) {
            deleteAllSessions()
            deleteAllSankalps()
        } else {
            clearAllActiveFlags()
        }
        sankalpsWithSessions.forEachIndexed { index, (sankalp, sessions) ->
            val newSankalpId = insertSankalp(
                sankalp.copy(
                    id = 0L,
                    isActive = if (index == 0) true else sankalp.isActive
                )
            )
            sessions.forEach { session ->
                insertSession(session.copy(id = 0L, sankalpId = newSankalpId))
            }
        }
    }
}
