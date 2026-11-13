package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.local.SankalpEntity
import com.example.domain.SadhanaCalculator
import com.example.domain.SadhanaDateUtils
import com.example.ui.LiveJapaState
import com.example.ui.SadhanaUiState
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LiveJapaScreen(
    uiState: SadhanaUiState,
    onStartSitting: (SankalpEntity) -> Unit,
    onTapBead: () -> Unit,
    onUndoBead: () -> Unit,
    onAddWholeMala: () -> Unit,
    onSaveSitting: (note: String) -> Unit,
    onCancelSitting: () -> Unit,
    onCreateSankalp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val active = uiState.activeMetrics
    val live = uiState.liveSession
    val haptic = LocalHapticFeedback.current

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showFinishDialog by remember { mutableStateOf(false) }
    var sessionNote by remember { mutableStateOf("") }

    LaunchedEffect(live.isSitting) {
        while (live.isSitting) {
            nowMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (active == null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SelfImprovement,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "सक्रिय संकल्प नाही",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "थेट जप मोजणी सुरू करण्यासाठी प्रथम एक साधना संकल्प तयार करा.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = onCreateSankalp) {
                            Text("संकल्प तयार करा")
                        }
                    }
                }
            } else if (!live.isSitting) {
                PreSittingCard(
                    mantraName = active.sankalp.mantraName,
                    beadsPerMala = active.sankalp.beadsPerMala,
                    requiredDailyJapa = active.requiredDailyJapaNow,
                    requiredDailyMalas = active.requiredDailyMalasNow,
                    onStartSitting = { onStartSitting(active.sankalp) }
                )
            } else {
                // Sitting Active View
                val elapsed = (nowMillis - live.startTimeMillis).coerceAtLeast(0L)

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = active.sankalp.mantraName,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "बसण्याची वेळ: ${SadhanaDateUtils.formatTime12Hr(live.startTimeMillis)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "वेळ ${SadhanaDateUtils.formatElapsedTimer(elapsed)}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                MalaBeadTapCounter(
                    live = live,
                    onTap = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onTapBead()
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("पूर्ण माळा", style = MaterialTheme.typography.labelMedium)
                            Text(
                                text = "${live.completedMalas}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("या सत्रातील जप", style = MaterialTheme.typography.labelMedium)
                            Text(
                                text = SadhanaDateUtils.formatIndianNumber(live.totalSessionJapa),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onUndoBead,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("live_undo_bead_button")
                    ) {
                        Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("1 मणी मागे")
                    }

                    FilledTonalButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onAddWholeMala()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("live_add_mala_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("+1 माळ (${live.beadsPerMala})")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancelSitting,
                        modifier = Modifier
                            .weight(0.42f)
                            .height(52.dp)
                            .testTag("live_cancel_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("रद्द")
                    }

                    Button(
                        onClick = {
                            sessionNote = ""
                            showFinishDialog = true
                        },
                        enabled = live.totalSessionJapa > 0L,
                        modifier = Modifier
                            .weight(0.58f)
                            .height(52.dp)
                            .testTag("live_finish_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("जप पूर्ण (उठलो)")
                    }
                }
            }
        }
    }

    if (showFinishDialog && live.isSitting) {
        val endNow = System.currentTimeMillis()
        val duration = SadhanaCalculator.calculateDurationMinutes(live.startTimeMillis, endNow)
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text("जप सत्र जतन करा") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• बसण्याची वेळ: ${SadhanaDateUtils.formatTime12Hr(live.startTimeMillis)}")
                    Text("• उठण्याची वेळ: ${SadhanaDateUtils.formatTime12Hr(endNow)}")
                    Text("• कालावधी: ${SadhanaDateUtils.formatDurationMarathi(duration)}")
                    Text(
                        text = "• झालेला जप: ${SadhanaDateUtils.formatIndianNumber(live.totalSessionJapa)} (${live.completedMalas} माळा + ${live.currentBeads} मणी)",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedTextField(
                        value = sessionNote,
                        onValueChange = { sessionNote = it },
                        label = { Text("अनुभव / टीप (ऐच्छिक)") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showFinishDialog = false
                        onSaveSitting(sessionNote)
                    },
                    modifier = Modifier.testTag("confirm_finish_live_button")
                ) {
                    Text("नोंद सेव्ह करा")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) {
                    Text("सुरू ठेवा")
                }
            }
        )
    }
}

@Composable
private fun PreSittingCard(
    mantraName: String,
    beadsPerMala: Int,
    requiredDailyJapa: Long,
    requiredDailyMalas: Int,
    onStartSitting: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.SelfImprovement,
                        contentDescription = null,
                        modifier = Modifier.size(38.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = mantraName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "1 माळ = $beadsPerMala मणी  •  आजचे आवश्यक लक्ष्य: ${SadhanaDateUtils.formatIndianNumber(requiredDailyJapa)} जप ($requiredDailyMalas माळा)",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "खालील बटणावर क्लिक करताच तुमची 'जपाला बसण्याची वेळ' आपोआप नोंदवली जाईल.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.secondary
            )

            Button(
                onClick = onStartSitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("start_sitting_button")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("जपाला बसा (सुरुवात करा)", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun MalaBeadTapCounter(
    live: LiveJapaState,
    onTap: () -> Unit
) {
    var tapScaleTrigger by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (tapScaleTrigger) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.55f),
        finishedListener = { tapScaleTrigger = false },
        label = "mala_tap_scale"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val inactiveBeadColor = MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = Modifier.size(270.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val visualBeads = 36
            val radius = size.minDimension / 2f - 16.dp.toPx()
            val center = Offset(size.width / 2f, size.height / 2f)
            val filledFraction = if (live.beadsPerMala > 0) {
                live.currentBeads.toFloat() / live.beadsPerMala.toFloat()
            } else {
                0f
            }
            val activeBeadsCount = (filledFraction * visualBeads).toInt()

            for (i in 0 until visualBeads) {
                val angleRad = Math.toRadians((-90.0) + (360.0 / visualBeads) * i)
                val bx = center.x + (radius * cos(angleRad)).toFloat()
                val by = center.y + (radius * sin(angleRad)).toFloat()
                val isLit = i < activeBeadsCount
                drawCircle(
                    color = if (isLit) primaryColor else inactiveBeadColor,
                    radius = if (i == 0) 7.5.dp.toPx() else if (isLit) 5.5.dp.toPx() else 4.5.dp.toPx(),
                    center = Offset(bx, by)
                )
            }
        }

        Box(
            modifier = Modifier
                .size(190.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            primaryColor,
                            secondaryColor
                        )
                    )
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = Color.White),
                    onClick = {
                        tapScaleTrigger = true
                        onTap()
                    }
                )
                .testTag("tap_bead_circle"),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${live.currentBeads}",
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "/ ${live.beadsPerMala} मणी",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "स्पर्श करा (+1)",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFFFFF3D6)
                )
            }
        }
    }
}
