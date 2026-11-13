package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.SadhanaCalculator
import com.example.domain.SadhanaDateUtils
import com.example.ui.SadhanaUiState
import com.example.ui.components.CumulativeTrendChart
import com.example.ui.components.DailyJapaBarChart
import com.example.ui.components.SankalpProgressRing

@Composable
fun ReportsBackupScreen(
    uiState: SadhanaUiState,
    onExportJsonBackup: () -> Unit,
    onRestoreJsonBackup: () -> Unit,
    onExportCsvReport: () -> Unit,
    modifier: Modifier = Modifier
) {
    var chartDaysWindow by remember { mutableIntStateOf(7) }
    val active = uiState.activeMetrics

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Assessment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "साधना प्रगती अहवाल व बॅकअप",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }

            if (active != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = active.sankalp.mantraName,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )

                            SankalpProgressRing(
                                progressFraction = active.progressFraction,
                                completedJapa = active.completedJapa,
                                targetJapa = active.sankalp.targetCount
                            )

                            val avgDuration = if (active.sessionsCount > 0) {
                                active.totalMinutesSpent / active.sessionsCount
                            } else {
                                0
                            }
                            val preferredWindow = remember(uiState.activeSessions) {
                                SadhanaCalculator.preferredSittingWindowLabel(uiState.activeSessions)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                InsightBox(
                                    label = "सलग साधना",
                                    value = "${active.currentStreakDays} दिवस",
                                    modifier = Modifier.weight(1f)
                                )
                                InsightBox(
                                    label = "एकूण वेळ",
                                    value = SadhanaDateUtils.formatDurationMarathi(active.totalMinutesSpent),
                                    modifier = Modifier.weight(1f)
                                )
                                InsightBox(
                                    label = "सरासरी सत्र",
                                    value = SadhanaDateUtils.formatDurationMarathi(avgDuration),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "नियमित साधना वेळ: $preferredWindow",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    val dailyPoints = remember(uiState.activeSessions, chartDaysWindow, active.sankalp.beadsPerMala) {
                        SadhanaCalculator.computeDailySeries(
                            sessions = uiState.activeSessions,
                            beadsPerMala = active.sankalp.beadsPerMala,
                            daysCount = chartDaysWindow
                        )
                    }

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "दैनंदिन जप आलेख",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(7, 14, 30).forEach { days ->
                                        FilterChip(
                                            selected = chartDaysWindow == days,
                                            onClick = { chartDaysWindow = days },
                                            label = { Text("${days} दि.") }
                                        )
                                    }
                                }
                            }

                            DailyJapaBarChart(
                                points = dailyPoints,
                                dailyTarget = active.plannedDailyJapa
                            )
                        }
                    }
                }

                item {
                    val cumulativePoints = remember(active.sankalp, uiState.activeSessions) {
                        SadhanaCalculator.computeCumulativeSeries(
                            sankalp = active.sankalp,
                            sessions = uiState.activeSessions
                        )
                    }
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "संचित प्रगती वक्र (प्रत्यक्ष विरुद्ध लक्ष्य)",
                                style = MaterialTheme.typography.titleMedium
                            )
                            CumulativeTrendChart(
                                points = cumulativePoints,
                                targetTotal = active.sankalp.targetCount
                            )
                        }
                    }
                }
            }

            // Backup & Restore Section
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "डेटा सुरक्षा व बॅकअप (Backup & Restore)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "तुमच्या पसंतीनुसार क्लाउड सिंक (Firebase) बंद आहे. सर्व संकल्प व नोंदी डिव्हाइसवर सुरक्षित आहेत. सुरक्षित बॅकअपसाठी तुम्ही खालील बटणावरून JSON फाईल Google Drive किंवा फोनवर सेव्ह करू शकता.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = onExportJsonBackup,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("export_json_backup_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("संपूर्ण बॅकअप सेव्ह करा (.json)")
                        }

                        OutlinedButton(
                            onClick = onRestoreJsonBackup,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("restore_json_backup_button")
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("बॅकअप फाईल परत आणा (.json)")
                        }

                        OutlinedButton(
                            onClick = onExportCsvReport,
                            enabled = uiState.allSessions.isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("export_csv_button")
                        ) {
                            Icon(Icons.Default.TableChart, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("सत्र अहवाल सेव्ह करा (.csv / Excel)")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
