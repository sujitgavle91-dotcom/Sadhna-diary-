package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.SankalpEntity
import com.example.domain.SadhanaCalculator
import com.example.domain.SadhanaDateUtils

private enum class CalcMode { BY_DATES, BY_DAILY_PACE }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SankalpDialog(
    existingSankalp: SankalpEntity?,
    onDismiss: () -> Unit,
    onSave: (
        mantraName: String,
        beadsPerMala: Int,
        targetCount: Long,
        startEpochDay: Long,
        endEpochDay: Long,
        dailyTargetCount: Long,
        notes: String
    ) -> Unit
) {
    BackHandler { onDismiss() }

    val today = remember { SadhanaDateUtils.todayEpochDay() }
    var mantraName by remember(existingSankalp) {
        mutableStateOf(existingSankalp?.mantraName ?: "ॐ क्रीं कालिकायै नमः")
    }
    var beadsPerMalaText by remember(existingSankalp) {
        mutableStateOf((existingSankalp?.beadsPerMala ?: 108).toString())
    }
    var targetCountText by remember(existingSankalp) {
        mutableStateOf((existingSankalp?.targetCount ?: 125000L).toString())
    }
    var calcMode by remember { mutableStateOf(CalcMode.BY_DATES) }
    var startEpochDay by remember(existingSankalp) {
        mutableLongStateOf(existingSankalp?.startDateEpochDay ?: today)
    }
    var endEpochDay by remember(existingSankalp) {
        mutableLongStateOf(existingSankalp?.endDateEpochDay ?: (today + 40L))
    }
    var dailyMalasText by remember(existingSankalp) {
        val beads = existingSankalp?.beadsPerMala ?: 108
        val dailyJapa = existingSankalp?.dailyTargetCount ?: 3024L
        mutableStateOf(SadhanaCalculator.japaToCeilMalas(dailyJapa, beads).coerceAtLeast(1).toString())
    }
    var notes by remember(existingSankalp) { mutableStateOf(existingSankalp?.notes ?: "") }

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    val beadsPerMala = beadsPerMalaText.toIntOrNull()?.coerceIn(1, 1008) ?: 108
    val targetCount = targetCountText.toLongOrNull()?.coerceAtLeast(1L) ?: 0L

    val (computedDays, computedEndEpochDay, computedDailyJapa) = remember(
        calcMode,
        targetCount,
        startEpochDay,
        endEpochDay,
        dailyMalasText,
        beadsPerMala
    ) {
        if (calcMode == CalcMode.BY_DATES) {
            val safeEnd = maxOf(endEpochDay, startEpochDay)
            val (days, daily) = SadhanaCalculator.calculateDailyTargetFromDates(
                targetCount = targetCount.coerceAtLeast(1L),
                startEpochDay = startEpochDay,
                endEpochDay = safeEnd
            )
            Triple(days, safeEnd, daily)
        } else {
            val dailyMalas = dailyMalasText.toIntOrNull()?.coerceAtLeast(1) ?: 1
            val dailyJapa = SadhanaCalculator.malasToJapa(dailyMalas, beadsPerMala)
            val (days, end) = SadhanaCalculator.calculateEndDateFromDailyTarget(
                targetCount = targetCount.coerceAtLeast(1L),
                dailyTargetCount = dailyJapa,
                startEpochDay = startEpochDay
            )
            Triple(days, end, dailyJapa)
        }
    }

    val computedTotalMalas = SadhanaCalculator.japaToCeilMalas(targetCount, beadsPerMala)
    val computedDailyMalas = SadhanaCalculator.japaToCeilMalas(computedDailyJapa, beadsPerMala)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (existingSankalp == null) "नवीन महाकाली संकल्प" else "संकल्प संपादित करा",
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("sankalp_dialog_close")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "बंद करा")
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                if (mantraName.isNotBlank() && targetCount > 0L) {
                                    onSave(
                                        mantraName,
                                        beadsPerMala,
                                        targetCount,
                                        startEpochDay,
                                        computedEndEpochDay,
                                        computedDailyJapa,
                                        notes
                                    )
                                }
                            },
                            enabled = mantraName.isNotBlank() && targetCount > 0L,
                            modifier = Modifier.testTag("sankalp_dialog_save")
                        ) {
                            Text("जतन करा", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = mantraName,
                    onValueChange = { mantraName = it },
                    label = { Text("मंत्राचे नाव") },
                    placeholder = { Text("उदा. ॐ क्रीं कालिकायै नमः") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("mantra_name_input")
                )

                val mantraPresets = listOf(
                    "ॐ क्रीं कालिकायै नमः",
                    "ॐ ऐं ह्रीं क्लीं चामुण्डायै विच्चे",
                    "जय माँ महाकाली",
                    "ॐ ह्रीं श्रीं क्लीं कालिकायै नमः",
                    "ॐ क्रीं काल्यै नमः"
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    mantraPresets.forEach { preset ->
                        FilterChip(
                            selected = mantraName == preset,
                            onClick = { mantraName = preset },
                            label = { Text(preset, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                HorizontalDivider()

                Text(
                    text = "एकूण संकल्प जप संख्या",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = targetCountText,
                        onValueChange = { targetCountText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("एकूण जप संख्या") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1.4f)
                            .testTag("target_count_input")
                    )
                    OutlinedTextField(
                        value = beadsPerMalaText,
                        onValueChange = { beadsPerMalaText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("मणी / माळ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(0.8f)
                            .testTag("beads_per_mala_input")
                    )
                }

                val targetPresets = listOf(
                    11_000L to "11,000",
                    21_000L to "21,000",
                    51_000L to "51,000",
                    108_000L to "1,08,000",
                    125_000L to "1,25,000 (सव्वा लाख)"
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    targetPresets.forEach { (count, label) ->
                        FilterChip(
                            selected = targetCount == count,
                            onClick = { targetCountText = count.toString() },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                HorizontalDivider()

                Text(
                    text = "स्वयंचलित गणना पद्धत (Auto Calculation)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = calcMode == CalcMode.BY_DATES,
                        onClick = { calcMode = CalcMode.BY_DATES },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        modifier = Modifier.testTag("mode_by_dates")
                    ) {
                        Text("तारखेवरून दररोज जप", style = MaterialTheme.typography.labelMedium)
                    }
                    SegmentedButton(
                        selected = calcMode == CalcMode.BY_DAILY_PACE,
                        onClick = { calcMode = CalcMode.BY_DAILY_PACE },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        modifier = Modifier.testTag("mode_by_daily")
                    ) {
                        Text("दररोज माळेवरून तारीख", style = MaterialTheme.typography.labelMedium)
                    }
                }

                // Start Date button
                OutlinedButton(
                    onClick = { showStartPicker = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("start_date_button")
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("सुरुवात तारीख: ${SadhanaDateUtils.formatMarathiDate(startEpochDay)}")
                }

                if (calcMode == CalcMode.BY_DATES) {
                    OutlinedButton(
                        onClick = { showEndPicker = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("end_date_button")
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("समाप्ती तारीख: ${SadhanaDateUtils.formatMarathiDate(computedEndEpochDay)}")
                    }

                    val dayPresets = listOf(
                        7 to "7 दिवस",
                        11 to "11 दिवस",
                        21 to "21 दिवस",
                        41 to "41 दिवस (मंडळ)",
                        108 to "108 दिवस"
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        dayPresets.forEach { (days, label) ->
                            FilterChip(
                                selected = computedDays == days,
                                onClick = { endEpochDay = startEpochDay + (days - 1L) },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = dailyMalasText,
                        onValueChange = { dailyMalasText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("दररोज किती माळा जप करणार? (1 माळ = $beadsPerMala जप)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("daily_malas_input")
                    )
                }

                // Live Automatic Calculation Summary Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "स्वयंचलित संकल्प गणना",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "• एकूण जप: ${SadhanaDateUtils.formatIndianNumber(targetCount)} (अंदाजे ${SadhanaDateUtils.formatIndianNumber(computedTotalMalas.toLong())} माळा)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "• कालावधी: $computedDays दिवस (${SadhanaDateUtils.formatMarathiDate(startEpochDay)} ते ${SadhanaDateUtils.formatMarathiDate(computedEndEpochDay)})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "• दररोज आवश्यक जप: ${SadhanaDateUtils.formatIndianNumber(computedDailyJapa)} जप ($computedDailyMalas माळा / दिवस)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("संकल्प टीप / नियम (ऐच्छिक)") },
                    placeholder = { Text("उदा. संध्याकाळी किंवा निशीथ काळात जास्वंदाचे फूल अर्पण करून जप करणे") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sankalp_notes_input")
                )

                Button(
                    onClick = {
                        if (mantraName.isNotBlank() && targetCount > 0L) {
                            onSave(
                                mantraName,
                                beadsPerMala,
                                targetCount,
                                startEpochDay,
                                computedEndEpochDay,
                                computedDailyJapa,
                                notes
                            )
                        }
                    },
                    enabled = mantraName.isNotBlank() && targetCount > 0L,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("confirm_save_sankalp_button")
                ) {
                    Text(
                        text = if (existingSankalp == null) "संकल्प सुरू करा" else "बदल जतन करा",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showStartPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = SadhanaDateUtils.epochDayToUtcMillis(startEpochDay)
        )
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { millis ->
                            startEpochDay = SadhanaDateUtils.utcMillisToEpochDay(millis)
                            if (endEpochDay < startEpochDay) {
                                endEpochDay = startEpochDay
                            }
                        }
                        showStartPicker = false
                    }
                ) { Text("निवडा") }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker = false }) { Text("रद्द करा") }
            }
        ) {
            DatePicker(state = state)
        }
    }

    if (showEndPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = SadhanaDateUtils.epochDayToUtcMillis(endEpochDay)
        )
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { millis ->
                            val picked = SadhanaDateUtils.utcMillisToEpochDay(millis)
                            endEpochDay = maxOf(picked, startEpochDay)
                        }
                        showEndPicker = false
                    }
                ) { Text("निवडा") }
            },
            dismissButton = {
                TextButton(onClick = { showEndPicker = false }) { Text("रद्द करा") }
            }
        ) {
            DatePicker(state = state)
        }
    }
}
