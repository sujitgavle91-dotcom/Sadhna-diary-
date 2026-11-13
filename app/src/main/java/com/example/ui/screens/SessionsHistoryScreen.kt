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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.JapaSessionEntity
import com.example.data.local.SankalpEntity
import com.example.domain.SadhanaDateUtils
import com.example.ui.SadhanaUiState

@Composable
fun SessionsHistoryScreen(
    uiState: SadhanaUiState,
    onAddManualSession: (SankalpEntity) -> Unit,
    onEditSession: (SankalpEntity, JapaSessionEntity) -> Unit,
    onDeleteSession: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showOnlyActive by remember { mutableStateOf(true) }
    var sessionToDelete by remember { mutableStateOf<JapaSessionEntity?>(null) }

    val sankalpMap = remember(uiState.sankalps) {
        uiState.sankalps.associate { it.sankalp.id to it.sankalp }
    }
    val activeSankalp = uiState.activeMetrics?.sankalp

    val displayedSessions = remember(uiState.allSessions, uiState.activeSessions, showOnlyActive) {
        if (showOnlyActive && activeSankalp != null) {
            uiState.activeSessions
        } else {
            uiState.allSessions
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "जप नोंदी व वेळेचा तपशील",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "किती वाजता बसलो व उठलो याचा संपूर्ण इतिहास",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (activeSankalp != null) {
                        Button(
                            onClick = { onAddManualSession(activeSankalp) },
                            modifier = Modifier.testTag("history_add_session_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("नोंद जोडा")
                        }
                    }
                }
            }

            if (activeSankalp != null && uiState.sankalps.size > 1) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = showOnlyActive,
                            onClick = { showOnlyActive = true },
                            label = { Text("सक्रिय संकल्प (${uiState.activeSessions.size})") }
                        )
                        FilterChip(
                            selected = !showOnlyActive,
                            onClick = { showOnlyActive = false },
                            label = { Text("सर्व संकल्प (${uiState.allSessions.size})") }
                        )
                    }
                }
            }

            if (displayedSessions.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "अद्याप एकही जप नोंद नाही",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "जप मोजणी सुरू करा किंवा वरच्या बटणावरून मागील जप वेळेची नोंद जोडा.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                item {
                    val totalJapa = displayedSessions.sumOf { it.japaCount }
                    val totalMins = displayedSessions.sumOf { it.durationMinutes }
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "सत्रे: ${displayedSessions.size}",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "एकूण जप: ${SadhanaDateUtils.formatIndianNumber(totalJapa)}",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = SadhanaDateUtils.formatDurationMarathi(totalMins),
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }
                }

                items(displayedSessions, key = { it.id }) { session ->
                    val parentSankalp = sankalpMap[session.sankalpId]
                    SessionRecordCard(
                        session = session,
                        mantraName = parentSankalp?.mantraName ?: "मंत्र जप",
                        onEdit = {
                            if (parentSankalp != null) {
                                onEditSession(parentSankalp, session)
                            }
                        },
                        onDelete = { sessionToDelete = session }
                    )
                }
            }
        }
    }

    sessionToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text("जप नोंद हटवायची का?") },
            text = {
                Text("${SadhanaDateUtils.formatMarathiDate(target.sessionDateEpochDay)} रोजीची ${SadhanaDateUtils.formatIndianNumber(target.japaCount)} जप नोंद हटवली जाईल.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteSession(target.id)
                        sessionToDelete = null
                    }
                ) {
                    Text("हटवा", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text("रद्द करा")
                }
            }
        )
    }
}

@Composable
private fun SessionRecordCard(
    session: JapaSessionEntity,
    mantraName: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("session_card_${session.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = SadhanaDateUtils.formatMarathiDate(session.sessionDateEpochDay),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = mantraName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(50)
                ) {
                    val malaDetail = if (session.extraBeads > 0) {
                        "${session.malaCount} माळा + ${session.extraBeads}"
                    } else {
                        "${session.malaCount} माळा"
                    }
                    Text(
                        text = "${SadhanaDateUtils.formatIndianNumber(session.japaCount)} जप ($malaDetail)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "बसलो: ${SadhanaDateUtils.formatTime12Hr(session.startTimeMillis)}  →  उठलो: ${SadhanaDateUtils.formatTime12Hr(session.endTimeMillis)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = SadhanaDateUtils.formatDurationMarathi(session.durationMinutes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (session.note.isNotBlank()) {
                Text(
                    text = "टीप: ${session.note}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.cd_edit_item),
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = stringResource(R.string.cd_delete_item),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
