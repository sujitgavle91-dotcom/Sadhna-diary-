package com.example

import com.example.data.local.JapaSessionEntity
import com.example.data.local.SankalpEntity
import com.example.domain.PaceStatus
import com.example.domain.SadhanaCalculator
import com.example.domain.SadhanaDateUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class SadhanaCalculatorTest {

    @Test
    fun `calculateDailyTargetFromDates computes exact inclusive days and ceil daily count`() {
        val start = 20_000L
        val end = 20_040L // 41 days inclusive
        val (days, daily) = SadhanaCalculator.calculateDailyTargetFromDates(
            targetCount = 125_000L,
            startEpochDay = start,
            endEpochDay = end
        )
        assertEquals(41, days)
        assertEquals(3049L, daily)
    }

    @Test
    fun `calculateEndDateFromDailyTarget computes exact end date`() {
        val start = 20_000L
        // 11 malas/day * 108 = 1188 japa/day -> ceil(125000 / 1188) = 106 days
        val (days, end) = SadhanaCalculator.calculateEndDateFromDailyTarget(
            targetCount = 125_000L,
            dailyTargetCount = 1188L,
            startEpochDay = start
        )
        assertEquals(106, days)
        assertEquals(20_105L, end)
    }

    @Test
    fun `calculateDurationMinutes handles same day and overnight sittings`() {
        val startSame = 1_700_000_000_000L
        val endSame = startSame + 45L * 60_000L
        assertEquals(45, SadhanaCalculator.calculateDurationMinutes(startSame, endSame))

        // Overnight crossing: end appears earlier than start
        val startNight = 1_700_000_000_000L
        val endMorning = startNight - 23L * 3_600_000L // +1 hr after adding 24h
        assertEquals(60, SadhanaCalculator.calculateDurationMinutes(startNight, endMorning))
    }

    @Test
    fun `calculateSankalpMetrics dynamically adjusts required daily japa`() {
        val start = 20_000L
        val end = 20_009L // 10 days
        val sankalp = SankalpEntity(
            id = 1L,
            mantraName = "ॐ नमः शिवाय",
            beadsPerMala = 108,
            targetCount = 10_800L,
            startDateEpochDay = start,
            endDateEpochDay = end,
            dailyTargetCount = 1_080L
        )
        val sessions = listOf(
            JapaSessionEntity(
                id = 1L,
                sankalpId = 1L,
                sessionDateEpochDay = start,
                startTimeMillis = 1000L,
                endTimeMillis = 2000L,
                japaCount = 2_160L,
                malaCount = 20,
                durationMinutes = 30
            )
        )
        // On day 2 (start + 1), 9 days remaining (including today), 8640 japa left -> 960/day
        val metrics = SadhanaCalculator.calculateSankalpMetrics(
            sankalp = sankalp,
            sessions = sessions,
            todayEpochDay = start + 1L
        )
        assertEquals(2_160L, metrics.completedJapa)
        assertEquals(8_640L, metrics.remainingJapa)
        assertEquals(9, metrics.remainingDays)
        assertEquals(960L, metrics.requiredDailyJapaNow)
        assertEquals(PaceStatus.ON_TRACK, metrics.paceStatus)
    }

    @Test
    fun `formatIndianNumber formats lakhs and thousands properly`() {
        assertEquals("1,25,000", SadhanaDateUtils.formatIndianNumber(125_000L))
        assertEquals("1,080", SadhanaDateUtils.formatIndianNumber(1_080L))
        assertEquals("108", SadhanaDateUtils.formatIndianNumber(108L))
    }
}
