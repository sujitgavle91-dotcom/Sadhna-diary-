package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.CumulativeChartPoint
import com.example.domain.DailyChartPoint
import com.example.domain.SadhanaDateUtils
import kotlin.math.max

@Composable
fun SankalpProgressRing(
    progressFraction: Float,
    completedJapa: Long,
    targetJapa: Long,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800),
        label = "sankalp_ring"
    )
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Box(
        modifier = modifier.size(164.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            val inset = strokeWidth / 2f
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)

            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            if (animatedProgress > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(primaryColor, secondaryColor, primaryColor)
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val pct = (animatedProgress * 100f).toInt()
            Text(
                text = "$pct%",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${SadhanaDateUtils.formatIndianNumber(completedJapa)} / ${SadhanaDateUtils.formatIndianNumber(targetJapa)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DailyJapaBarChart(
    points: List<DailyChartPoint>,
    dailyTarget: Long,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember(points) { mutableIntStateOf(points.lastIndex.coerceAtLeast(0)) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    val guideColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)

    val maxJapa = remember(points, dailyTarget) {
        max(
            dailyTarget.coerceAtLeast(1L),
            points.maxOfOrNull { it.japaCount } ?: 1L
        ).coerceAtLeast(108L)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        val selected = points.getOrNull(selectedIndex)
        if (selected != null) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = selected.fullDateLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${SadhanaDateUtils.formatIndianNumber(selected.japaCount)} जप (${selected.malaCount} माळा)",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = if (selected.durationMinutes > 0) {
                            SadhanaDateUtils.formatDurationMarathi(selected.durationMinutes)
                        } else {
                            "नोंद नाही"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(points) {
                        detectTapGestures { offset ->
                            if (points.isNotEmpty() && size.width > 0) {
                                val slotWidth = size.width.toFloat() / points.size.toFloat()
                                val tapped = (offset.x / slotWidth).toInt().coerceIn(0, points.lastIndex)
                                selectedIndex = tapped
                            }
                        }
                    }
            ) {
                if (points.isEmpty()) return@Canvas
                val chartHeight = size.height - 14.dp.toPx()
                val slotWidth = size.width / points.size.toFloat()
                val barWidth = (slotWidth * 0.56f).coerceIn(8.dp.toPx(), 28.dp.toPx())

                // Target reference dashed line
                if (dailyTarget > 0L) {
                    val targetRatio = (dailyTarget.toFloat() / maxJapa.toFloat()).coerceIn(0f, 1f)
                    val targetY = chartHeight - (chartHeight * targetRatio)
                    drawLine(
                        color = secondaryColor,
                        start = Offset(0f, targetY),
                        end = Offset(size.width, targetY),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                    )
                }

                // Baseline
                drawLine(
                    color = guideColor,
                    start = Offset(0f, chartHeight),
                    end = Offset(size.width, chartHeight),
                    strokeWidth = 1.dp.toPx()
                )

                points.forEachIndexed { index, point ->
                    val centerX = slotWidth * index + slotWidth / 2f
                    val left = centerX - barWidth / 2f

                    // Subtle background slot
                    drawRoundRect(
                        color = trackColor,
                        topLeft = Offset(left, 0f),
                        size = Size(barWidth, chartHeight),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )

                    val ratio = (point.japaCount.toFloat() / maxJapa.toFloat()).coerceIn(0f, 1f)
                    val barHeight = (chartHeight * ratio).coerceAtLeast(
                        if (point.japaCount > 0L) 6.dp.toPx() else 0f
                    )
                    if (barHeight > 0f) {
                        val barColor = when {
                            point.japaCount >= dailyTarget && dailyTarget > 0L -> tertiaryColor
                            index == selectedIndex -> primaryColor
                            else -> primaryColor.copy(alpha = 0.78f)
                        }
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(left, chartHeight - barHeight),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                    }

                    if (index == selectedIndex) {
                        drawCircle(
                            color = primaryColor,
                            radius = 3.5.dp.toPx(),
                            center = Offset(centerX, chartHeight + 8.dp.toPx())
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Sparse date axis labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val step = (points.size / 4).coerceAtLeast(1)
            points.forEachIndexed { index, point ->
                if (index == 0 || index == points.lastIndex || index % step == 0) {
                    Text(
                        text = point.shortLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendDot(color = tertiaryColor, label = "लक्ष्य पूर्ण")
            LegendDot(color = primaryColor, label = "झालेला जप")
            LegendDot(color = secondaryColor, label = "दररोज लक्ष्य (${SadhanaDateUtils.formatIndianNumber(dailyTarget)})")
        }
    }
}

@Composable
fun CumulativeTrendChart(
    points: List<CumulativeChartPoint>,
    targetTotal: Long,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val brassColor = MaterialTheme.colorScheme.secondary
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(175.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (points.size < 2) return@Canvas
                val maxVal = max(
                    targetTotal.coerceAtLeast(1L),
                    points.maxOf { max(it.actualCumulative, it.idealCumulative) }
                ).coerceAtLeast(1L)

                val padTop = 8.dp.toPx()
                val padBottom = 10.dp.toPx()
                val usableHeight = (size.height - padTop - padBottom).coerceAtLeast(1f)
                val stepX = size.width / (points.size - 1).coerceAtLeast(1)

                // Horizontal grid lines (25%, 50%, 75%, 100%)
                for (g in 1..4) {
                    val y = padTop + usableHeight * (1f - g / 4f)
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                val idealPath = Path()
                val actualPath = Path()
                val fillPath = Path()

                points.forEachIndexed { idx, pt ->
                    val x = idx * stepX
                    val idealY = padTop + usableHeight * (1f - (pt.idealCumulative.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f))
                    val actualY = padTop + usableHeight * (1f - (pt.actualCumulative.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f))
                    if (idx == 0) {
                        idealPath.moveTo(x, idealY)
                        actualPath.moveTo(x, actualY)
                        fillPath.moveTo(x, padTop + usableHeight)
                        fillPath.lineTo(x, actualY)
                    } else {
                        idealPath.lineTo(x, idealY)
                        actualPath.lineTo(x, actualY)
                        fillPath.lineTo(x, actualY)
                    }
                    if (idx == points.lastIndex) {
                        fillPath.lineTo(x, padTop + usableHeight)
                        fillPath.close()
                    }
                }

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(primaryColor.copy(alpha = 0.28f), primaryColor.copy(alpha = 0.02f))
                    )
                )

                drawPath(
                    path = idealPath,
                    color = brassColor,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                    )
                )

                drawPath(
                    path = actualPath,
                    color = primaryColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                points.forEachIndexed { idx, pt ->
                    val x = idx * stepX
                    val actualY = padTop + usableHeight * (1f - (pt.actualCumulative.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f))
                    drawCircle(
                        color = primaryColor,
                        radius = 3.5.dp.toPx(),
                        center = Offset(x, actualY)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (points.isNotEmpty()) {
                Text(
                    text = points.first().label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = points[points.size / 2].label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = points.last().label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendDot(color = primaryColor, label = "प्रत्यक्ष संचित जप")
            LegendDot(color = brassColor, label = "अपेक्षित गती (लक्ष्य)")
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .background(color = color, shape = CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
