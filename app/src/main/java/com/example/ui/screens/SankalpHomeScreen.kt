package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.SankalpEntity
import com.example.domain.PaceStatus
import com.example.domain.SadhanaDateUtils
import com.example.domain.SankalpMetrics
import com.example.ui.SadhanaUiState

@Composable
fun SankalpHomeScreen(
    uiState: SadhanaUiState,
    onCreateNewSankalp: () -> Unit,
    onQuickCreatePreset: (mantra: String, target: Long, days: Int) -> Unit,
    onEditSankalp: (SankalpEntity) -> Unit,
    onSelectSankalp: (Long) -> Unit,
    onDeleteSankalp: (Long) -> Unit,
    onStartLiveJapa: (SankalpEntity) -> Unit,
    onOpenManualSession: (SankalpEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var sankalpToDelete by remember { mutableStateOf<SankalpEntity?>(null) }

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
            if (uiState.activeMetrics == null) {
                item {
                    EmptySadhanaHeroCard(
                        onCreateNewSankalp = onCreateNewSankalp,
                        onQuickCreatePreset = onQuickCreatePreset
                    )
                }
            } else {
                val active = uiState.activeMetrics
                item {
                    ActiveSankalpHeroCard(
                        metrics = active,
                        onEditSankalp = { onEditSankalp(active.sankalp) },
                        onStartLiveJapa = { onStartLiveJapa(active.sankalp) },
                        onOpenManualSession = { onOpenManualSession(active.sankalp) }
                    )
                }

                item {
                    CalculationBreakdownCard(metrics = active)
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "माझे सर्व साधना संकल्प (${uiState.sankalps.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        FilledTonalButton(
                            onClick = onCreateNewSankalp,
                            modifier = Modifier.testTag("add_sankalp_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("नवीन संकल्प")
                        }
                    }
                }

                items(uiState.sankalps, key = { it.sankalp.id }) { metrics ->
                    SankalpListItemCard(
                        metrics = metrics,
                        isSelected = metrics.sankalp.id == active.sankalp.id,
                        onSelect = { onSelectSankalp(metrics.sankalp.id) },
                        onEdit = { onEditSankalp(metrics.sankalp) },
                        onDelete = { sankalpToDelete = metrics.sankalp }
                    )
                }
            }
        }
    }

    sankalpToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { sankalpToDelete = null },
            title = { Text("संकल्प हटवायचा का?") },
            text = {
                Text("\"${target.mantraName}\" हा संकल्प आणि त्यातील सर्व जप नोंदी कायमच्या हटवल्या जातील.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteSankalp(target.id)
                        sankalpToDelete = null
                    }
                ) {
                    Text("हटवा", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { sankalpToDelete = null }) {
                    Text("रद्द करा")
                }
            }
        )
    }
}

@Composable
private fun EmptySadhanaHeroCard(
    onCreateNewSankalp: () -> Unit,
    onQuickCreatePreset: (String, Long, Int) -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_sadhana_empty),
                    contentDescription = stringResource(R.string.cd_empty_sadhana),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "|| जय माँ महाकाली ||",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFFFD580)
                    )
                    Text(
                        text = "महाकाली साधना संकल्प सुरू करा",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "माँ महाकाली मंत्राची एकूण संख्या, सुरुवात व समाप्ती तारीख निवडल्यास दररोज किती जप व माळा कराव्या लागतील याची स्वयंचलित गणना होईल.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = onCreateNewSankalp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("empty_create_sankalp_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("नवीन संकल्प तयार करा", style = MaterialTheme.typography.titleMedium)
                }

                Text(
                    text = "झटपट महाकाली संकल्प नमुने (Quick Start):",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                val presets = listOf(
                    Triple("ॐ क्रीं कालिकायै नमः", 125_000L, 41),
                    Triple("ॐ ऐं ह्रीं क्लीं चामुण्डायै विच्चे", 108_000L, 108),
                    Triple("जय माँ महाकाली", 11_000L, 11)
                )
                presets.forEach { (mantra, count, days) ->
                    OutlinedButton(
                        onClick = { onQuickCreatePreset(mantra, count, days) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "$mantra • ${SadhanaDateUtils.formatIndianNumber(count)} जप ($days दिवस)",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveSankalpHeroCard(
    metrics: SankalpMetrics,
    onEditSankalp: () -> Unit,
    onStartLiveJapa: () -> Unit,
    onOpenManualSession: () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = metrics.progressFraction,
        label = "hero_progress"
    )
    val paceLabel = when (metrics.paceStatus) {
        PaceStatus.COMPLETED -> "संकल्प पूर्ण!"
        PaceStatus.AHEAD -> "वेळेच्या पुढे आहात"
        PaceStatus.ON_TRACK -> "नियमित गतीने सुरू"
        PaceStatus.BEHIND -> "थोडा वेग वाढवा"
        PaceStatus.UPCOMING -> "संकल्प लवकरच सुरू"
    }
    val paceBg = when (metrics.paceStatus) {
        PaceStatus.COMPLETED, PaceStatus.AHEAD -> MaterialTheme.colorScheme.tertiaryContainer
        PaceStatus.ON_TRACK -> MaterialTheme.colorScheme.secondaryContainer
        PaceStatus.BEHIND -> MaterialTheme.colorScheme.errorContainer
        PaceStatus.UPCOMING -> MaterialTheme.colorScheme.surfaceVariant
    }
    val paceFg = when (metrics.paceStatus) {
        PaceStatus.COMPLETED, PaceStatus.AHEAD -> MaterialTheme.colorScheme.onTertiaryContainer
        PaceStatus.ON_TRACK -> MaterialTheme.colorScheme.onSecondaryContainer
        PaceStatus.BEHIND -> MaterialTheme.colorScheme.onErrorContainer
        PaceStatus.UPCOMING -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    ElevatedCard(
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(155.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_sadhana_hero),
                    contentDescription = stringResource(R.string.cd_hero_banner),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.2f),
                                    Color.Black.copy(alpha = 0.78f)
                                )
                            )
                        )
                )

                Surface(
                    color = paceBg,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(14.dp)
                ) {
                    Text(
                        text = paceLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = paceFg,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }

                IconButton(
                    onClick = onEditSankalp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .testTag("edit_active_sankalp_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.cd_edit_sankalp),
                        tint = Color.White
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = metrics.sankalp.mantraName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${SadhanaDateUtils.formatMarathiDate(metrics.sankalp.startDateEpochDay)}  ते  ${SadhanaDateUtils.formatMarathiDate(metrics.sankalp.endDateEpochDay)} (${metrics.totalDays} दिवस)",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFFFE8C8)
                    )
                }
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "झालेला जप",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = SadhanaDateUtils.formatIndianNumber(metrics.completedJapa),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${metrics.completedMalas} माळा पूर्ण",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "एकूण संकल्प",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = SadhanaDateUtils.formatIndianNumber(metrics.sankalp.targetCount),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "शिल्लक: ${SadhanaDateUtils.formatIndianNumber(metrics.remainingJapa)} जप",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    strokeCap = StrokeCap.Round
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${(metrics.progressFraction * 100f).toInt()}% पूर्ण",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "सलग ${metrics.currentStreakDays} दिवस साधना",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onStartLiveJapa,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("home_start_live_japa")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("थेट जपाला बसा")
                    }
                    FilledTonalButton(
                        onClick = onOpenManualSession,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("home_add_manual_session")
                    ) {
                        Icon(Icons.Default.EditCalendar, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("वेळेची नोंद करा")
                    }
                }
            }
        }
    }
}

@Composable
private fun CalculationBreakdownCard(metrics: SankalpMetrics) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.SelfImprovement,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "स्वयंचलित संकल्प हिशोब",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatTile(
                    label = "नियोजित दररोज जप",
                    value = "${SadhanaDateUtils.formatIndianNumber(metrics.plannedDailyJapa)} जप",
                    subValue = "${metrics.plannedDailyMalas} माळा / दिवस",
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "आजपासून आवश्यक",
                    value = "${SadhanaDateUtils.formatIndianNumber(metrics.requiredDailyJapaNow)} जप",
                    subValue = "${metrics.requiredDailyMalasNow} माळा • ${metrics.remainingDays} दिवस बाकी",
                    highlight = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatTile(
                    label = "सुरुवात तारीख",
                    value = SadhanaDateUtils.formatMarathiDate(metrics.sankalp.startDateEpochDay),
                    subValue = "दिवस ${metrics.elapsedDays} / ${metrics.totalDays}",
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "समाप्ती तारीख",
                    value = SadhanaDateUtils.formatMarathiDate(metrics.sankalp.endDateEpochDay),
                    subValue = "एकूण वेळ: ${SadhanaDateUtils.formatDurationMarathi(metrics.totalMinutesSpent)}",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    subValue: String,
    highlight: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (highlight) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subValue,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun SankalpListItemCard(
    metrics: SankalpMetrics,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        ),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .testTag("sankalp_card_${metrics.sankalp.id}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "सक्रिय संकल्प",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(10.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = metrics.sankalp.mantraName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${SadhanaDateUtils.formatIndianNumber(metrics.completedJapa)} / ${SadhanaDateUtils.formatIndianNumber(metrics.sankalp.targetCount)} जप • ${(metrics.progressFraction * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${SadhanaDateUtils.formatMarathiDate(metrics.sankalp.startDateEpochDay)} - ${SadhanaDateUtils.formatMarathiDate(metrics.sankalp.endDateEpochDay)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.cd_edit_item)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = stringResource(R.string.cd_delete_item),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
