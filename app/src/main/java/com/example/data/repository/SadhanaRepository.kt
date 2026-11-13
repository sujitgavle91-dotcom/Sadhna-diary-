package com.example.data.repository

import com.example.data.local.JapaSessionEntity
import com.example.data.local.SadhanaDao
import com.example.data.local.SankalpEntity
import com.example.domain.SadhanaDateUtils
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

data class BackupRestoreSummary(
    val sankalpsRestored: Int,
    val sessionsRestored: Int
)

class SadhanaRepository(private val dao: SadhanaDao) {

    val allSankalps: Flow<List<SankalpEntity>> = dao.observeAllSankalps()
    val allSessions: Flow<List<JapaSessionEntity>> = dao.observeAllSessions()

    suspend fun createAndActivateSankalp(sankalp: SankalpEntity): Long {
        return dao.createAndActivateSankalp(sankalp)
    }

    suspend fun updateSankalp(sankalp: SankalpEntity) {
        dao.updateSankalp(sankalp)
    }

    suspend fun setActiveSankalp(sankalpId: Long) {
        dao.setActiveSankalp(sankalpId)
    }

    suspend fun deleteSankalp(sankalpId: Long) {
        dao.deleteSankalpById(sankalpId)
    }

    suspend fun insertSession(session: JapaSessionEntity): Long {
        return dao.insertSession(session)
    }

    suspend fun updateSession(session: JapaSessionEntity) {
        dao.updateSession(session)
    }

    suspend fun deleteSession(sessionId: Long) {
        dao.deleteSessionById(sessionId)
    }

    suspend fun exportBackupJson(): String {
        val sankalps = dao.getAllSankalpsOnce()
        val sessionsBySankalp = dao.getAllSessionsOnce().groupBy { it.sankalpId }

        val root = JSONObject().apply {
            put("app", "SadhanaSankalp")
            put("schemaVersion", 1)
            put("exportedAtMillis", System.currentTimeMillis())
        }

        val sankalpArray = JSONArray()
        sankalps.forEach { s ->
            val sessionArray = JSONArray()
            sessionsBySankalp[s.id].orEmpty().forEach { sess ->
                sessionArray.put(
                    JSONObject().apply {
                        put("sessionDateEpochDay", sess.sessionDateEpochDay)
                        put("startTimeMillis", sess.startTimeMillis)
                        put("endTimeMillis", sess.endTimeMillis)
                        put("japaCount", sess.japaCount)
                        put("malaCount", sess.malaCount)
                        put("extraBeads", sess.extraBeads)
                        put("durationMinutes", sess.durationMinutes)
                        put("note", sess.note)
                        put("createdAtMillis", sess.createdAtMillis)
                    }
                )
            }
            sankalpArray.put(
                JSONObject().apply {
                    put("mantraName", s.mantraName)
                    put("beadsPerMala", s.beadsPerMala)
                    put("targetCount", s.targetCount)
                    put("startDateEpochDay", s.startDateEpochDay)
                    put("endDateEpochDay", s.endDateEpochDay)
                    put("dailyTargetCount", s.dailyTargetCount)
                    put("isActive", s.isActive)
                    put("isCompleted", s.isCompleted)
                    put("notes", s.notes)
                    put("createdAtMillis", s.createdAtMillis)
                    put("sessions", sessionArray)
                }
            )
        }
        root.put("sankalps", sankalpArray)
        return root.toString(2)
    }

    suspend fun importBackupJson(jsonText: String, replaceExisting: Boolean): BackupRestoreSummary {
        val root = JSONObject(jsonText)
        val sankalpArray = root.getJSONArray("sankalps")
        val parsed = mutableListOf<Pair<SankalpEntity, List<JapaSessionEntity>>>()
        var totalSessions = 0

        for (i in 0 until sankalpArray.length()) {
            val obj = sankalpArray.getJSONObject(i)
            val sankalp = SankalpEntity(
                mantraName = obj.getString("mantraName"),
                beadsPerMala = obj.optInt("beadsPerMala", 108).coerceAtLeast(1),
                targetCount = obj.getLong("targetCount").coerceAtLeast(1L),
                startDateEpochDay = obj.getLong("startDateEpochDay"),
                endDateEpochDay = obj.getLong("endDateEpochDay"),
                dailyTargetCount = obj.optLong("dailyTargetCount", 0L),
                isActive = obj.optBoolean("isActive", false),
                isCompleted = obj.optBoolean("isCompleted", false),
                notes = obj.optString("notes", ""),
                createdAtMillis = obj.optLong("createdAtMillis", System.currentTimeMillis())
            )
            val sessArray = obj.optJSONArray("sessions") ?: JSONArray()
            val sessions = mutableListOf<JapaSessionEntity>()
            for (j in 0 until sessArray.length()) {
                val sObj = sessArray.getJSONObject(j)
                sessions.add(
                    JapaSessionEntity(
                        sankalpId = 0L,
                        sessionDateEpochDay = sObj.getLong("sessionDateEpochDay"),
                        startTimeMillis = sObj.getLong("startTimeMillis"),
                        endTimeMillis = sObj.getLong("endTimeMillis"),
                        japaCount = sObj.getLong("japaCount"),
                        malaCount = sObj.optInt("malaCount", 0),
                        extraBeads = sObj.optInt("extraBeads", 0),
                        durationMinutes = sObj.optInt("durationMinutes", 1),
                        note = sObj.optString("note", ""),
                        createdAtMillis = sObj.optLong("createdAtMillis", System.currentTimeMillis())
                    )
                )
                totalSessions++
            }
            parsed.add(sankalp to sessions)
        }

        dao.restoreBackup(parsed, replaceExisting)
        return BackupRestoreSummary(sankalpsRestored = parsed.size, sessionsRestored = totalSessions)
    }

    suspend fun exportSessionsCsv(): String {
        val sankalps = dao.getAllSankalpsOnce().associateBy { it.id }
        val sessions = dao.getAllSessionsOnce()
        val sb = StringBuilder()
        sb.appendLine("Date,Mantra,SittingTime,RisingTime,DurationMinutes,Malas,ExtraBeads,TotalJapa,Note")
        sessions.forEach { s ->
            val mantra = (sankalps[s.sankalpId]?.mantraName ?: "").replace("\"", "\"\"")
            val note = s.note.replace("\"", "\"\"")
            val dateStr = SadhanaDateUtils.formatMarathiDate(s.sessionDateEpochDay)
            val startStr = SadhanaDateUtils.formatTime12Hr(s.startTimeMillis)
            val endStr = SadhanaDateUtils.formatTime12Hr(s.endTimeMillis)
            sb.appendLine("\"$dateStr\",\"$mantra\",\"$startStr\",\"$endStr\",${s.durationMinutes},${s.malaCount},${s.extraBeads},${s.japaCount},\"$note\"")
        }
        return sb.toString()
    }
}
