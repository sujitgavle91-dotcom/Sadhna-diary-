package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "sankalps")
data class SankalpEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val mantraName: String,
    val beadsPerMala: Int = 108,
    val targetCount: Long,
    val startDateEpochDay: Long,
    val endDateEpochDay: Long,
    val dailyTargetCount: Long,
    val isActive: Boolean = false,
    val isCompleted: Boolean = false,
    val notes: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "japa_sessions",
    foreignKeys = [
        ForeignKey(
            entity = SankalpEntity::class,
            parentColumns = ["id"],
            childColumns = ["sankalpId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sankalpId"), Index("sessionDateEpochDay")]
)
data class JapaSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sankalpId: Long,
    val sessionDateEpochDay: Long,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val japaCount: Long,
    val malaCount: Int,
    val extraBeads: Int = 0,
    val durationMinutes: Int,
    val note: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
)
