package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.JapaSessionEntity
import com.example.data.local.SankalpEntity
import com.example.data.repository.SadhanaRepository
import com.example.domain.SadhanaCalculator
import com.example.domain.SadhanaDateUtils
import com.example.domain.SankalpMetrics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LiveJapaState(
    val isSitting: Boolean = false,
    val sankalpId: Long = 0L,
    val startTimeMillis: Long = 0L,
    val beadsPerMala: Int = 108,
    val currentBeads: Int = 0,
    val completedMalas: Int = 0
) {
    val totalSessionJapa: Long
        get() = (completedMalas.toLong() * beadsPerMala.toLong()) + currentBeads.toLong()
}

data class SadhanaUiState(
    val isLoading: Boolean = true,
    val sankalps: List<SankalpMetrics> = emptyList(),
    val activeMetrics: SankalpMetrics? = null,
    val allSessions: List<JapaSessionEntity> = emptyList(),
    val activeSessions: List<JapaSessionEntity> = emptyList(),
    val liveSession: LiveJapaState = LiveJapaState(),
    val snackbarMessage: String? = null
)

class SadhanaViewModel(
    private val repository: SadhanaRepository
) : ViewModel() {

    private val _liveSession = MutableStateFlow(LiveJapaState())
    private val _snackbarMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SadhanaUiState> = combine(
        repository.allSankalps,
        repository.allSessions,
        _liveSession,
        _snackbarMessage
    ) { sankalps, sessions, live, snackbar ->
        val today = SadhanaDateUtils.todayEpochDay()
        val sessionsBySankalp = sessions.groupBy { it.sankalpId }
        val metricsList = sankalps.map { sankalp ->
            SadhanaCalculator.calculateSankalpMetrics(
                sankalp = sankalp,
                sessions = sessionsBySankalp[sankalp.id].orEmpty(),
                todayEpochDay = today
            )
        }
        val active = metricsList.firstOrNull { it.sankalp.isActive } ?: metricsList.firstOrNull()
        val activeSessions = if (active != null) {
            sessionsBySankalp[active.sankalp.id].orEmpty()
        } else {
            emptyList()
        }

        SadhanaUiState(
            isLoading = false,
            sankalps = metricsList,
            activeMetrics = active,
            allSessions = sessions,
            activeSessions = activeSessions,
            liveSession = live,
            snackbarMessage = snackbar
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = SadhanaUiState(isLoading = true)
    )

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(message: String) {
        _snackbarMessage.value = message
    }

    fun createOrUpdateSankalp(
        existingSankalp: SankalpEntity?,
        mantraName: String,
        beadsPerMala: Int,
        targetCount: Long,
        startEpochDay: Long,
        endEpochDay: Long,
        dailyTargetCount: Long,
        notes: String
    ) {
        viewModelScope.launch {
            if (existingSankalp == null) {
                val entity = SankalpEntity(
                    mantraName = mantraName.trim(),
                    beadsPerMala = beadsPerMala.coerceAtLeast(1),
                    targetCount = targetCount.coerceAtLeast(1L),
                    startDateEpochDay = startEpochDay,
                    endDateEpochDay = endEpochDay.coerceAtLeast(startEpochDay),
                    dailyTargetCount = dailyTargetCount.coerceAtLeast(1L),
                    isActive = true,
                    isCompleted = false,
                    notes = notes.trim()
                )
                repository.createAndActivateSankalp(entity)
                _snackbarMessage.value = "नवीन साधना संकल्प सुरू झाला!"
            } else {
                val updated = existingSankalp.copy(
                    mantraName = mantraName.trim(),
                    beadsPerMala = beadsPerMala.coerceAtLeast(1),
                    targetCount = targetCount.coerceAtLeast(1L),
                    startDateEpochDay = startEpochDay,
                    endDateEpochDay = endEpochDay.coerceAtLeast(startEpochDay),
                    dailyTargetCount = dailyTargetCount.coerceAtLeast(1L),
                    notes = notes.trim()
                )
                repository.updateSankalp(updated)
                _snackbarMessage.value = "संकल्प माहिती अद्ययावत केली."
            }
        }
    }

    fun setActiveSankalp(sankalpId: Long) {
        viewModelScope.launch {
            repository.setActiveSankalp(sankalpId)
        }
    }

    fun deleteSankalp(sankalpId: Long) {
        viewModelScope.launch {
            if (_liveSession.value.isSitting && _liveSession.value.sankalpId == sankalpId) {
                _liveSession.value = LiveJapaState()
            }
            repository.deleteSankalp(sankalpId)
            _snackbarMessage.value = "संकल्प हटवला गेला."
        }
    }

    fun startLiveSitting(sankalp: SankalpEntity) {
        _liveSession.value = LiveJapaState(
            isSitting = true,
            sankalpId = sankalp.id,
            startTimeMillis = System.currentTimeMillis(),
            beadsPerMala = sankalp.beadsPerMala.coerceAtLeast(1),
            currentBeads = 0,
            completedMalas = 0
        )
    }

    fun tapBead() {
        _liveSession.update { state ->
            if (!state.isSitting) return@update state
            val nextBead = state.currentBeads + 1
            if (nextBead >= state.beadsPerMala) {
                state.copy(
                    currentBeads = 0,
                    completedMalas = state.completedMalas + 1
                )
            } else {
                state.copy(currentBeads = nextBead)
            }
        }
    }

    fun undoBead() {
        _liveSession.update { state ->
            if (!state.isSitting) return@update state
            when {
                state.currentBeads > 0 -> state.copy(currentBeads = state.currentBeads - 1)
                state.completedMalas > 0 -> state.copy(
                    completedMalas = state.completedMalas - 1,
                    currentBeads = (state.beadsPerMala - 1).coerceAtLeast(0)
                )
                else -> state
            }
        }
    }

    fun addWholeMala() {
        _liveSession.update { state ->
            if (!state.isSitting) return@update state
            state.copy(completedMalas = state.completedMalas + 1)
        }
    }

    fun finishAndSaveLiveSitting(note: String) {
        val current = _liveSession.value
        if (!current.isSitting) return
        val totalJapa = current.totalSessionJapa
        if (totalJapa <= 0L) {
            _snackbarMessage.value = "किमान 1 जप किंवा 1 माळ मोजा."
            return
        }
        val endMillis = System.currentTimeMillis()
        val duration = SadhanaCalculator.calculateDurationMinutes(current.startTimeMillis, endMillis)
        val sessionDate = SadhanaDateUtils.localMillisToEpochDay(current.startTimeMillis)

        viewModelScope.launch {
            repository.insertSession(
                JapaSessionEntity(
                    sankalpId = current.sankalpId,
                    sessionDateEpochDay = sessionDate,
                    startTimeMillis = current.startTimeMillis,
                    endTimeMillis = endMillis,
                    japaCount = totalJapa,
                    malaCount = current.completedMalas,
                    extraBeads = current.currentBeads,
                    durationMinutes = duration,
                    note = note.trim()
                )
            )
            _liveSession.value = LiveJapaState()
            _snackbarMessage.value = "${SadhanaDateUtils.formatIndianNumber(totalJapa)} जप नोंद जतन झाली!"
        }
    }

    fun cancelLiveSitting() {
        _liveSession.value = LiveJapaState()
    }

    fun saveManualSession(
        existingSession: JapaSessionEntity?,
        sankalpId: Long,
        beadsPerMala: Int,
        sessionDateEpochDay: Long,
        startTimeMillis: Long,
        endTimeMillis: Long,
        japaCount: Long,
        note: String
    ) {
        val safeBeads = beadsPerMala.coerceAtLeast(1)
        val malas = (japaCount / safeBeads).toInt()
        val extra = (japaCount % safeBeads).toInt()
        val duration = SadhanaCalculator.calculateDurationMinutes(startTimeMillis, endTimeMillis)

        viewModelScope.launch {
            if (existingSession == null) {
                repository.insertSession(
                    JapaSessionEntity(
                        sankalpId = sankalpId,
                        sessionDateEpochDay = sessionDateEpochDay,
                        startTimeMillis = startTimeMillis,
                        endTimeMillis = endTimeMillis,
                        japaCount = japaCount,
                        malaCount = malas,
                        extraBeads = extra,
                        durationMinutes = duration,
                        note = note.trim()
                    )
                )
                _snackbarMessage.value = "जप सत्राची नोंद सेव्ह झाली!"
            } else {
                repository.updateSession(
                    existingSession.copy(
                        sankalpId = sankalpId,
                        sessionDateEpochDay = sessionDateEpochDay,
                        startTimeMillis = startTimeMillis,
                        endTimeMillis = endTimeMillis,
                        japaCount = japaCount,
                        malaCount = malas,
                        extraBeads = extra,
                        durationMinutes = duration,
                        note = note.trim()
                    )
                )
                _snackbarMessage.value = "जप नोंद अद्ययावत झाली."
            }
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            _snackbarMessage.value = "जप नोंद हटवली."
        }
    }

    suspend fun exportBackupJson(): String = repository.exportBackupJson()

    suspend fun exportSessionsCsv(): String = repository.exportSessionsCsv()

    fun restoreBackupJson(jsonText: String, replaceExisting: Boolean) {
        viewModelScope.launch {
            try {
                val summary = repository.importBackupJson(jsonText, replaceExisting)
                _snackbarMessage.value =
                    "${summary.sankalpsRestored} संकल्प आणि ${summary.sessionsRestored} नोंदी यशस्वीपणे परत आल्या!"
            } catch (e: Exception) {
                _snackbarMessage.value = "बॅकअप फाईल वाचताना त्रुटी आली: ${e.localizedMessage ?: "अवैध फाईल"}"
            }
        }
    }

    companion object {
        fun factory(repository: SadhanaRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SadhanaViewModel(repository) as T
                }
            }
        }
    }
}
