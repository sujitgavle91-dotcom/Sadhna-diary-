package com.example

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.JapaSessionEntity
import com.example.data.local.SadhanaDatabase
import com.example.data.local.SankalpEntity
import com.example.data.repository.SadhanaRepository
import com.example.domain.SadhanaCalculator
import com.example.domain.SadhanaDateUtils
import com.example.ui.SadhanaViewModel
import com.example.ui.components.SankalpDialog
import com.example.ui.components.SessionDialog
import com.example.ui.screens.LiveJapaScreen
import com.example.ui.screens.ReportsBackupScreen
import com.example.ui.screens.SankalpHomeScreen
import com.example.ui.screens.SessionsHistoryScreen
import com.example.ui.theme.SadhanaSankalpTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class MainTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    HOME("संकल्प", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
    LIVE_JAPA("जप मोजणी", Icons.Filled.SelfImprovement, Icons.Outlined.SelfImprovement, "nav_live"),
    HISTORY("नोंदी", Icons.AutoMirrored.Filled.ListAlt, Icons.AutoMirrored.Outlined.ListAlt, "nav_history"),
    REPORTS("अहवाल", Icons.Filled.Assessment, Icons.Outlined.Assessment, "nav_reports")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SadhanaSankalpTheme {
                SadhanaApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SadhanaApp() {
    val context = LocalContext.current.applicationContext
    val repository = remember(context) {
        SadhanaRepository(SadhanaDatabase.getInstance(context).sadhanaDao())
    }
    val viewModel: SadhanaViewModel = viewModel(factory = SadhanaViewModel.factory(repository))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableStateOf(MainTab.HOME) }
    var showSankalpDialog by remember { mutableStateOf(false) }
    var editingSankalp by remember { mutableStateOf<SankalpEntity?>(null) }

    var sessionDialogSankalp by remember { mutableStateOf<SankalpEntity?>(null) }
    var editingSession by remember { mutableStateOf<JapaSessionEntity?>(null) }
    var pendingRestoreJson by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    BackHandler(enabled = currentTab != MainTab.HOME) {
        currentTab = MainTab.HOME
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val json = viewModel.exportBackupJson()
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(json.toByteArray(Charsets.UTF_8))
                        }
                    }
                    viewModel.showMessage("बॅकअप फाईल यशस्वीपणे सेव्ह झाली!")
                } catch (e: Exception) {
                    viewModel.showMessage("बॅकअप सेव्ह करताना त्रुटी: ${e.localizedMessage ?: ""}")
                }
            }
        }
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val csv = viewModel.exportSessionsCsv()
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(csv.toByteArray(Charsets.UTF_8))
                        }
                    }
                    viewModel.showMessage("CSV अहवाल यशस्वीपणे सेव्ह झाला!")
                } catch (e: Exception) {
                    viewModel.showMessage("CSV सेव्ह करताना त्रुटी: ${e.localizedMessage ?: ""}")
                }
            }
        }
    }

    val restoreJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val text = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    }
                    if (!text.isNullOrBlank()) {
                        pendingRestoreJson = text
                    } else {
                        viewModel.showMessage("निवडलेली फाईल रिकामी आहे.")
                    }
                } catch (e: Exception) {
                    viewModel.showMessage("फाईल वाचता आली नाही: ${e.localizedMessage ?: ""}")
                }
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.primary
                    )
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar {
                        MainTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label, maxLines = 1) },
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (currentTab == MainTab.HOME && uiState.activeMetrics != null) {
                    FloatingActionButton(
                        onClick = {
                            editingSankalp = null
                            showSankalpDialog = true
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("fab_new_sankalp")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.cd_add_sankalp)
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        MainTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (currentTab) {
                        MainTab.HOME -> SankalpHomeScreen(
                            uiState = uiState,
                            onCreateNewSankalp = {
                                editingSankalp = null
                                showSankalpDialog = true
                            },
                            onQuickCreatePreset = { mantra, target, days ->
                                val today = SadhanaDateUtils.todayEpochDay()
                                val end = today + (days - 1L)
                                val (_, daily) = SadhanaCalculator.calculateDailyTargetFromDates(
                                    targetCount = target,
                                    startEpochDay = today,
                                    endEpochDay = end
                                )
                                viewModel.createOrUpdateSankalp(
                                    existingSankalp = null,
                                    mantraName = mantra,
                                    beadsPerMala = 108,
                                    targetCount = target,
                                    startEpochDay = today,
                                    endEpochDay = end,
                                    dailyTargetCount = daily,
                                    notes = ""
                                )
                            },
                            onEditSankalp = { sankalp ->
                                editingSankalp = sankalp
                                showSankalpDialog = true
                            },
                            onSelectSankalp = { id -> viewModel.setActiveSankalp(id) },
                            onDeleteSankalp = { id -> viewModel.deleteSankalp(id) },
                            onStartLiveJapa = { sankalp ->
                                if (!uiState.liveSession.isSitting) {
                                    viewModel.startLiveSitting(sankalp)
                                }
                                currentTab = MainTab.LIVE_JAPA
                            },
                            onOpenManualSession = { sankalp ->
                                editingSession = null
                                sessionDialogSankalp = sankalp
                            }
                        )

                        MainTab.LIVE_JAPA -> LiveJapaScreen(
                            uiState = uiState,
                            onStartSitting = { sankalp -> viewModel.startLiveSitting(sankalp) },
                            onTapBead = { viewModel.tapBead() },
                            onUndoBead = { viewModel.undoBead() },
                            onAddWholeMala = { viewModel.addWholeMala() },
                            onSaveSitting = { note -> viewModel.finishAndSaveLiveSitting(note) },
                            onCancelSitting = { viewModel.cancelLiveSitting() },
                            onCreateSankalp = {
                                editingSankalp = null
                                showSankalpDialog = true
                            }
                        )

                        MainTab.HISTORY -> SessionsHistoryScreen(
                            uiState = uiState,
                            onAddManualSession = { sankalp ->
                                editingSession = null
                                sessionDialogSankalp = sankalp
                            },
                            onEditSession = { sankalp, session ->
                                editingSession = session
                                sessionDialogSankalp = sankalp
                            },
                            onDeleteSession = { id -> viewModel.deleteSession(id) }
                        )

                        MainTab.REPORTS -> ReportsBackupScreen(
                            uiState = uiState,
                            onExportJsonBackup = {
                                exportJsonLauncher.launch("sadhana_sankalp_backup.json")
                            },
                            onRestoreJsonBackup = {
                                restoreJsonLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                            },
                            onExportCsvReport = {
                                exportCsvLauncher.launch("sadhana_japa_report.csv")
                            }
                        )
                    }
                }
            }
        }
    }

    if (showSankalpDialog) {
        SankalpDialog(
            existingSankalp = editingSankalp,
            onDismiss = { showSankalpDialog = false },
            onSave = { mantra, beads, target, startDay, endDay, daily, notes ->
                viewModel.createOrUpdateSankalp(
                    existingSankalp = editingSankalp,
                    mantraName = mantra,
                    beadsPerMala = beads,
                    targetCount = target,
                    startEpochDay = startDay,
                    endEpochDay = endDay,
                    dailyTargetCount = daily,
                    notes = notes
                )
                showSankalpDialog = false
            }
        )
    }

    sessionDialogSankalp?.let { targetSankalp ->
        SessionDialog(
            sankalp = targetSankalp,
            existingSession = editingSession,
            onDismiss = {
                sessionDialogSankalp = null
                editingSession = null
            },
            onSave = { dateDay, startMillis, endMillis, japaCount, note ->
                viewModel.saveManualSession(
                    existingSession = editingSession,
                    sankalpId = targetSankalp.id,
                    beadsPerMala = targetSankalp.beadsPerMala,
                    sessionDateEpochDay = dateDay,
                    startTimeMillis = startMillis,
                    endTimeMillis = endMillis,
                    japaCount = japaCount,
                    note = note
                )
                sessionDialogSankalp = null
                editingSession = null
            }
        )
    }

    pendingRestoreJson?.let { jsonText ->
        AlertDialog(
            onDismissRequest = { pendingRestoreJson = null },
            title = { Text("बॅकअप रिस्टोअर कसा करायचा?") },
            text = {
                Text("सध्याचा डेटा कायम ठेवून बॅकअपमधील नोंदी जोडायच्या (Merge) की सध्याचा डेटा बदलून नवीन बॅकअप आणायचा?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.restoreBackupJson(jsonText, replaceExisting = false)
                        pendingRestoreJson = null
                    }
                ) {
                    Text("डेटा जोडा (Merge)")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.restoreBackupJson(jsonText, replaceExisting = true)
                        pendingRestoreJson = null
                    }
                ) {
                    Text("सर्व बदला (Replace)")
                }
            }
        )
    }
}
