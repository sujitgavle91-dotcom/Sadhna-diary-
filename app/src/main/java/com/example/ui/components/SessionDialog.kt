package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.JapaSessionEntity
import com.example.data.local.SankalpEntity
import com.example.domain.SadhanaCalculator
import com.example.domain.SadhanaDateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDialog(
    sankalp: SankalpEntity,
    existingSession: JapaSessionEntity?,
    onDismiss: () -> Unit,
    onSave: (
        sessionDateEpochDay: Long,
        startTimeMillis: Long,
        endTimeMillis: Long,
        japaCount: Long,
        note: String
    ) -> Unit
) {
    BackHandler { onDismiss() }

    val beadsPerMala = sankalp.beadsPerMala.coerceAtLeast(1)
    val initialDate = existingSession?.sessionDateEpochDay ?: SadhanaDateUtils.todayEpochDay()
    val (initStartHr, initStartMin) = remember(existingSession) {
        if (existingSession != null) {
            SadhanaDateUtils.extractHourMinute(existingSession.startTimeMillis)
        } else {
            6 to 0
        }
    }
    val (initEndHr, initEndMin) = remember(existingSession) {
        if (existingSession != null) {
            SadhanaDateUtils.extractHourMinute(existingSession.endTimeMillis)
        } else {
            6 to 30
        }
    }

    var sessionEpochDay by remember(existingSession) { mutableLongStateOf(initialDate) }
    var startHour by remember(existingSession) { mutableIntStateOf(initStartHr) }
    var startMinute by remember(existingSession) { mutableIntStateOf(initStartMin) }
    var endHour by remember(existingSession) { mutableIntStateOf(initEndHr) }
    var endMinute by remember(existingSession) { mutableIntStateOf(initEndMin) }

    var malasText by remember(existingSession) {
        mutableStateOf(
            if (existingSession != null) existingSession.malaCount.toString() else "11"
        )
    }
    var japaCountText by remember(existingSession) {
        mutableStateOf(
            if (existingSession != null) existingSession.japaCount.toString() else (11 * beadsPerMala).toString()
        )
    }
    var note by remember(existingSession) { mutableStateOf(existingSession?.note ?: "") }

    var showDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val startMillis = remember(sessionEpochDay, startHour, startMinute) {
        SadhanaDateUtils.combineEpochDayAndHourMinute(sessionEpochDay, startHour, startMinute)
    }
    val endMillis = remember(sessionEpochDay, endHour, endMinute) {
        val candidate = SadhanaDateUtils.combineEpochDayAndHourMinute(sessionEpochDay, endHour, endMinute)
        if (candidate >= startMillis) candidate else candidate + 86_400_000L
    }
    val durationMinutes = SadhanaCalculator.calculateDurationMinutes(startMillis, endMillis)
    val japaCount = japaCountText.toLongOrNull() ?: 0L

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (existingSession == null) "जप सत्राची नोंद करा" else "जप नोंद संपादित करा",
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("session_dialog_close")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "बंद करा")
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                if (japaCount > 0L) {
                                    onSave(sessionEpochDay, startMillis, endMillis, japaCount, note)
                                }
                            },
                            enabled = japaCount > 0L,
                            modifier = Modifier.testTag("session_dialog_save")
                        ) {
                            Text("सेव्ह करा", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "मंत्र: ${sankalp.mantraName}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("session_date_button")
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("तारीख: ${SadhanaDateUtils.formatMarathiDate(sessionEpochDay)}")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showStartTimePicker = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sitting_time_button")
                    ) {
                        Icon(Icons.Default.AccessTime, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text("बसण्याची वेळ", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = SadhanaDateUtils.formatTime12Hr(startMillis),
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { showEndTimePicker = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("rising_time_button")
                    ) {
                        Icon(Icons.Default.AccessTime, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text("उठण्याची वेळ", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = SadhanaDateUtils.formatTime12Hr(endMillis),
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "एकूण साधना वेळ: ${SadhanaDateUtils.formatDurationMarathi(durationMinutes)}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = malasText,
                        onValueChange = { raw ->
                            val clean = raw.filter { it.isDigit() }
                            malasText = clean
                            val m = clean.toIntOrNull() ?: 0
                            japaCountText = SadhanaCalculator.malasToJapa(m, beadsPerMala).toString()
                        },
                        label = { Text("माळा ($beadsPerMala मणी)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("session_malas_input")
                    )

                    OutlinedTextField(
                        value = japaCountText,
                        onValueChange = { raw ->
                            val clean = raw.filter { it.isDigit() }
                            japaCountText = clean
                            val j = clean.toLongOrNull() ?: 0L
                            malasText = (j / beadsPerMala).toString()
                        },
                        label = { Text("एकूण जप संख्या") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("session_japa_input")
                    )
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("साधना अनुभव / टीप (ऐच्छिक)") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("session_note_input")
                )

                Button(
                    onClick = {
                        if (japaCount > 0L) {
                            onSave(sessionEpochDay, startMillis, endMillis, japaCount, note)
                        }
                    },
                    enabled = japaCount > 0L,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("confirm_save_session_button")
                ) {
                    Text("नोंद सेव्ह करा", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = SadhanaDateUtils.epochDayToUtcMillis(sessionEpochDay)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let {
                            sessionEpochDay = SadhanaDateUtils.utcMillisToEpochDay(it)
                        }
                        showDatePicker = false
                    }
                ) { Text("निवडा") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("रद्द करा") }
            }
        ) {
            DatePicker(state = state)
        }
    }

    if (showStartTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = startHour,
            initialMinute = startMinute,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showStartTimePicker = false },
            title = { Text("जपाला बसण्याची वेळ निवडा") },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        startHour = timeState.hour
                        startMinute = timeState.minute
                        showStartTimePicker = false
                    }
                ) { Text("निवडा") }
            },
            dismissButton = {
                TextButton(onClick = { showStartTimePicker = false }) { Text("रद्द करा") }
            }
        )
    }

    if (showEndTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = endHour,
            initialMinute = endMinute,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showEndTimePicker = false },
            title = { Text("जपावरून उठण्याची वेळ निवडा") },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        endHour = timeState.hour
                        endMinute = timeState.minute
                        showEndTimePicker = false
                    }
                ) { Text("निवडा") }
            },
            dismissButton = {
                TextButton(onClick = { showEndTimePicker = false }) { Text("रद्द करा") }
            }
        )
    }
}
