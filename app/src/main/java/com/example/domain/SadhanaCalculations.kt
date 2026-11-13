package com.example.domain

import com.example.data.local.JapaSessionEntity
import com.example.data.local.SankalpEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

private const val MILLIS_PER_DAY = 86_400_000L

enum class PaceStatus {
    AHEAD,
    ON_TRACK,
    BEHIND,
    COMPLETED,
    UPCOMING
}

data class SankalpMetrics(
    val sankalp: SankalpEntity,
    val totalDays: Int,
    val elapsedDays: Int,
    val remainingDays: Int,
    val completedJapa: Long,
    val remainingJapa: Long,
    val targetMalas: Long,
    val completedMalas: Long,
    val remainingMalas: Long,
    val plannedDailyJapa: Long,
    val plannedDailyMalas: Int,
    val requiredDailyJapaNow: Long,
    val requiredDailyMalasNow: Int,
    val progressFraction: Float,
    val expectedJapaByToday: Long,
    val totalMinutesSpent: Int,
    val sessionsCount: Int,
    val currentStreakDays: Int,
    val paceStatus: PaceStatus
)

data class DailyChartPoint(
    val epochDay: Long,
    val shortLabel: String,
    val fullDateLabel: String,
    val japaCount: Long,
    val malaCount: Int,
    val durationMinutes: Int
)

data class CumulativeChartPoint(
    val epochDay: Long,
    val label: String,
    val actualCumulative: Long,
    val idealCumulative: Long
)

object SadhanaDateUtils {

    fun todayEpochDay(): Long {
        val local = Calendar.getInstance()
        return ymdToEpochDay(
            local.get(Calendar.YEAR),
            local.get(Calendar.MONTH),
            local.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun ymdToEpochDay(year: Int, monthZeroBased: Int, dayOfMonth: Int): Long {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, monthZeroBased, dayOfMonth, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return utc.timeInMillis / MILLIS_PER_DAY
    }

    fun utcMillisToEpochDay(utcMillis: Long): Long {
        return utcMillis / MILLIS_PER_DAY
    }

    fun epochDayToUtcMillis(epochDay: Long): Long {
        return epochDay * MILLIS_PER_DAY
    }

    fun localMillisToEpochDay(localMillis: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = localMillis }
        return ymdToEpochDay(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun combineEpochDayAndHourMinute(epochDay: Long, hour24: Int, minute: Int): Long {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = epochDay * MILLIS_PER_DAY
        }
        val local = Calendar.getInstance().apply {
            set(
                utc.get(Calendar.YEAR),
                utc.get(Calendar.MONTH),
                utc.get(Calendar.DAY_OF_MONTH),
                hour24,
                minute,
                0
            )
            set(Calendar.MILLISECOND, 0)
        }
        return local.timeInMillis
    }

    fun extractHourMinute(localMillis: Long): Pair<Int, Int> {
        val cal = Calendar.getInstance().apply { timeInMillis = localMillis }
        return cal.get(Calendar.HOUR_OF_DAY) to cal.get(Calendar.MINUTE)
    }

    private val marathiMonthsShort = arrayOf(
        "जाने", "फेब्रु", "मार्च", "एप्रिल", "मे", "जून",
        "जुलै", "ऑग", "सप्टें", "ऑक्टो", "नोव्हें", "डिसें"
    )

    fun formatMarathiDate(epochDay: Long): String {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = epochDay * MILLIS_PER_DAY
        }
        val day = utc.get(Calendar.DAY_OF_MONTH)
        val month = marathiMonthsShort[utc.get(Calendar.MONTH)]
        val year = utc.get(Calendar.YEAR)
        return "$day $month $year"
    }

    fun formatShortDayMonth(epochDay: Long): String {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = epochDay * MILLIS_PER_DAY
        }
        val day = utc.get(Calendar.DAY_OF_MONTH)
        val month = utc.get(Calendar.MONTH) + 1
        return "$day/$month"
    }

    fun formatTime12Hr(millis: Long): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
        return sdf.format(Date(millis))
    }

    fun formatDurationMarathi(totalMinutes: Int): String {
        val safe = totalMinutes.coerceAtLeast(0)
        val hours = safe / 60
        val mins = safe % 60
        return when {
            hours > 0 && mins > 0 -> "$hours तास $mins मि."
            hours > 0 -> "$hours तास"
            else -> "$mins मिनिटे"
        }
    }

    fun formatElapsedTimer(elapsedMillis: Long): String {
        val totalSeconds = (elapsedMillis / 1000L).coerceAtLeast(0L)
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0L) {
            String.format(Locale.ENGLISH, "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.ENGLISH, "%02d:%02d", minutes, seconds)
        }
    }

    fun formatIndianNumber(value: Long): String {
        val negative = value < 0L
        val digits = kotlin.math.abs(value).toString()
        if (digits.length <= 3) return if (negative) "-$digits" else digits
        val lastThree = digits.takeLast(3)
        val rest = digits.dropLast(3)
        val groupedRest = rest.reversed().chunked(2).joinToString(",").reversed()
        val formatted = "$groupedRest,$lastThree"
        return if (negative) "-$formatted" else formatted
    }
}

object SadhanaCalculator {

    fun ceilDiv(numerator: Long, denominator: Long): Long {
        if (denominator <= 0L) return numerator
        if (numerator <= 0L) return 0L
        return (numerator + denominator - 1L) / denominator
    }

    fun calculateDurationMinutes(startMillis: Long, endMillis: Long): Int {
        val adjustedEnd = if (endMillis >= startMillis) {
            endMillis
        } else {
            endMillis + MILLIS_PER_DAY
        }
        val diffMillis = adjustedEnd - startMillis
        return ceil(diffMillis / 60_000.0).toInt().coerceAtLeast(1)
    }

    fun calculateDailyTargetFromDates(
        targetCount: Long,
        startEpochDay: Long,
        endEpochDay: Long
    ): Pair<Int, Long> {
        val totalDays = (endEpochDay - startEpochDay + 1L).coerceAtLeast(1L).toInt()
        val daily = ceilDiv(targetCount.coerceAtLeast(1L), totalDays.toLong())
        return totalDays to daily
    }

    fun calculateEndDateFromDailyTarget(
        targetCount: Long,
        dailyTargetCount: Long,
        startEpochDay: Long
    ): Pair<Int, Long> {
        val safeDaily = dailyTargetCount.coerceAtLeast(1L)
        val totalDays = ceilDiv(targetCount.coerceAtLeast(1L), safeDaily).coerceAtLeast(1L).toInt()
        val endEpochDay = startEpochDay + (totalDays - 1L)
        return totalDays to endEpochDay
    }

    fun malasToJapa(malas: Int, beadsPerMala: Int): Long {
        return malas.coerceAtLeast(0).toLong() * beadsPerMala.coerceAtLeast(1).toLong()
    }

    fun japaToCeilMalas(japa: Long, beadsPerMala: Int): Int {
        return ceilDiv(japa.coerceAtLeast(0L), beadsPerMala.coerceAtLeast(1).toLong()).toInt()
    }

    fun calculateSankalpMetrics(
        sankalp: SankalpEntity,
        sessions: List<JapaSessionEntity>,
        todayEpochDay: Long = SadhanaDateUtils.todayEpochDay()
    ): SankalpMetrics {
        val beads = sankalp.beadsPerMala.coerceAtLeast(1).toLong()
        val totalDays = (sankalp.endDateEpochDay - sankalp.startDateEpochDay + 1L)
            .coerceAtLeast(1L)
            .toInt()

        val elapsedDays = when {
            todayEpochDay < sankalp.startDateEpochDay -> 0
            todayEpochDay > sankalp.endDateEpochDay -> totalDays
            else -> (todayEpochDay - sankalp.startDateEpochDay + 1L).toInt()
        }

        val remainingDays = when {
            todayEpochDay < sankalp.startDateEpochDay -> totalDays
            todayEpochDay > sankalp.endDateEpochDay -> 1
            else -> (sankalp.endDateEpochDay - todayEpochDay + 1L).coerceAtLeast(1L).toInt()
        }

        val completedJapa = sessions.sumOf { it.japaCount }.coerceAtLeast(0L)
        val remainingJapa = (sankalp.targetCount - completedJapa).coerceAtLeast(0L)

        val targetMalas = ceilDiv(sankalp.targetCount, beads)
        val completedMalas = completedJapa / beads
        val remainingMalas = ceilDiv(remainingJapa, beads)

        val plannedDailyJapa = if (sankalp.dailyTargetCount > 0L) {
            sankalp.dailyTargetCount
        } else {
            ceilDiv(sankalp.targetCount, totalDays.toLong())
        }
        val plannedDailyMalas = ceilDiv(plannedDailyJapa, beads).toInt()

        val requiredDailyJapaNow = if (remainingJapa == 0L) 0L else ceilDiv(remainingJapa, remainingDays.toLong())
        val requiredDailyMalasNow = ceilDiv(requiredDailyJapaNow, beads).toInt()

        val progressFraction = if (sankalp.targetCount > 0L) {
            (completedJapa.toFloat() / sankalp.targetCount.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

        val expectedJapaByToday = if (elapsedDays == 0) {
            0L
        } else {
            ((sankalp.targetCount.toDouble() * elapsedDays.toDouble()) / totalDays.toDouble())
                .roundToInt()
                .toLong()
                .coerceAtMost(sankalp.targetCount)
        }

        val totalMinutesSpent = sessions.sumOf { it.durationMinutes }
        val streak = computeStreakDays(sessions, todayEpochDay)

        val paceStatus = when {
            completedJapa >= sankalp.targetCount || sankalp.isCompleted -> PaceStatus.COMPLETED
            todayEpochDay < sankalp.startDateEpochDay -> PaceStatus.UPCOMING
            completedJapa >= expectedJapaByToday + max(plannedDailyJapa / 2L, 1L) -> PaceStatus.AHEAD
            completedJapa + plannedDailyJapa >= expectedJapaByToday -> PaceStatus.ON_TRACK
            else -> PaceStatus.BEHIND
        }

        return SankalpMetrics(
            sankalp = sankalp,
            totalDays = totalDays,
            elapsedDays = elapsedDays,
            remainingDays = remainingDays,
            completedJapa = completedJapa,
            remainingJapa = remainingJapa,
            targetMalas = targetMalas,
            completedMalas = completedMalas,
            remainingMalas = remainingMalas,
            plannedDailyJapa = plannedDailyJapa,
            plannedDailyMalas = plannedDailyMalas,
            requiredDailyJapaNow = requiredDailyJapaNow,
            requiredDailyMalasNow = requiredDailyMalasNow,
            progressFraction = progressFraction,
            expectedJapaByToday = expectedJapaByToday,
            totalMinutesSpent = totalMinutesSpent,
            sessionsCount = sessions.size,
            currentStreakDays = streak,
            paceStatus = paceStatus
        )
    }

    fun computeStreakDays(
        sessions: List<JapaSessionEntity>,
        todayEpochDay: Long = SadhanaDateUtils.todayEpochDay()
    ): Int {
        if (sessions.isEmpty()) return 0
        val activeDays = sessions
            .filter { it.japaCount > 0L }
            .map { it.sessionDateEpochDay }
            .toHashSet()
        var cursor = when {
            activeDays.contains(todayEpochDay) -> todayEpochDay
            activeDays.contains(todayEpochDay - 1L) -> todayEpochDay - 1L
            else -> return 0
        }
        var streak = 0
        while (activeDays.contains(cursor)) {
            streak++
            cursor--
        }
        return streak
    }

    fun computeDailySeries(
        sessions: List<JapaSessionEntity>,
        beadsPerMala: Int,
        daysCount: Int,
        todayEpochDay: Long = SadhanaDateUtils.todayEpochDay()
    ): List<DailyChartPoint> {
        val grouped = sessions.groupBy { it.sessionDateEpochDay }
        val startDay = todayEpochDay - (daysCount - 1L)
        return (0 until daysCount).map { offset ->
            val day = startDay + offset
            val daySessions = grouped[day].orEmpty()
            val totalJapa = daySessions.sumOf { it.japaCount }
            val totalDuration = daySessions.sumOf { it.durationMinutes }
            val malas = (totalJapa / beadsPerMala.coerceAtLeast(1)).toInt()
            DailyChartPoint(
                epochDay = day,
                shortLabel = SadhanaDateUtils.formatShortDayMonth(day),
                fullDateLabel = SadhanaDateUtils.formatMarathiDate(day),
                japaCount = totalJapa,
                malaCount = malas,
                durationMinutes = totalDuration
            )
        }
    }

    fun computeCumulativeSeries(
        sankalp: SankalpEntity,
        sessions: List<JapaSessionEntity>,
        todayEpochDay: Long = SadhanaDateUtils.todayEpochDay()
    ): List<CumulativeChartPoint> {
        val start = sankalp.startDateEpochDay
        val end = max(sankalp.endDateEpochDay, start)
        val totalSpan = (end - start).coerceAtLeast(1L)
        val byDay = sessions.groupBy { it.sessionDateEpochDay }
            .mapValues { entry -> entry.value.sumOf { it.japaCount } }

        val maxDisplayDay = minOf(max(todayEpochDay, start), end)
        val spanToShow = (maxDisplayDay - start + 1L).coerceAtLeast(1L)
        val sampleCount = minOf(spanToShow.toInt(), 12).coerceAtLeast(2)

        var runningActual = 0L
        val sortedDays = byDay.keys.sorted()
        var dayIdx = 0

        val points = mutableListOf<CumulativeChartPoint>()
        for (i in 0 until sampleCount) {
            val sampleDay = if (sampleCount == 1) {
                start
            } else {
                start + ((maxDisplayDay - start) * i) / (sampleCount - 1)
            }
            while (dayIdx < sortedDays.size && sortedDays[dayIdx] <= sampleDay) {
                runningActual += byDay[sortedDays[dayIdx]] ?: 0L
                dayIdx++
            }
            val elapsedRatio = ((sampleDay - start + 1L).toDouble() / (totalSpan + 1L).toDouble())
                .coerceIn(0.0, 1.0)
            val ideal = (sankalp.targetCount.toDouble() * elapsedRatio).roundToInt().toLong()
            points.add(
                CumulativeChartPoint(
                    epochDay = sampleDay,
                    label = SadhanaDateUtils.formatShortDayMonth(sampleDay),
                    actualCumulative = runningActual,
                    idealCumulative = ideal
                )
            )
        }
        return points
    }

    fun preferredSittingWindowLabel(sessions: List<JapaSessionEntity>): String {
        if (sessions.isEmpty()) return "—"
        var brahma = 0
        var morning = 0
        var evening = 0
        var nishith = 0
        sessions.forEach { session ->
            val (hour, _) = SadhanaDateUtils.extractHourMinute(session.startTimeMillis)
            when (hour) {
                in 4..5 -> brahma++
                in 6..16 -> morning++
                in 17..21 -> evening++
                else -> nishith++
            }
        }
        return listOf(
            "ब्रह्ममुहूर्त (4-6 AM)" to brahma,
            "दिवस साधना (6 AM-5 PM)" to morning,
            "संध्याकाळ (5-10 PM)" to evening,
            "निशीथ / रात्र (10 PM-4 AM)" to nishith
        ).maxByOrNull { it.second }?.first ?: "—"
    }
}
