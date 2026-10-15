package com.thaiclockfix.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thaiclockfix.R
import com.thaiclockfix.model.PrimaryAction
import com.thaiclockfix.ui.components.AdbCommandsDialog
import com.thaiclockfix.ui.components.FaqAndTipsDialog
import com.thaiclockfix.ui.components.FormatToggleGroup
import com.thaiclockfix.ui.components.HeroClock
import com.thaiclockfix.ui.components.PrimaryActionBar
import com.thaiclockfix.ui.components.RestoreConfirmDialog
import com.thaiclockfix.ui.components.ShizukuBottomSheet
import com.thaiclockfix.viewmodel.ClockFixViewModel

/**
 * Material 3 Expressive Single-Screen Experience:
 * - Zero-Scroll Core Flow: Hero + Connected Toggle + State-Aware Bottom Action
 * - Emphasized typography & visual storytelling in the Hero Clock
 * - State-aware primary action replaces scattered status badges
 * - Secondary tools (Restore, ADB, FAQ) organized cleanly in the top app bar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClockFixMainScreen(
    viewModel: ClockFixViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showOverflowMenu by remember { mutableStateOf(false) }
    val errorPrefix = stringResource(R.string.error_snackbar_prefix)

    LaunchedEffect(uiState.userNotice) {
        uiState.userNotice?.let { notice ->
            snackbarHostState.showSnackbar(notice)
            viewModel.dismissNotice()
        }
    }

    LaunchedEffect(uiState.lastError) {
        uiState.lastError?.let { err ->
            snackbarHostState.showSnackbar(errorPrefix.format(err))
            viewModel.dismissError()
        }
    }

    val primaryAction = remember(
        uiState.shizukuStatus,
        uiState.selectedMode,
        uiState.actualSystemUiLocale,
        uiState.actualIs24Hour,
        uiState.isApplying,
        uiState.isCheckingSystem,
        uiState.lastError,
    ) {
        PrimaryAction.resolve(
            shizukuStatus = uiState.shizukuStatus,
            selectedMode = uiState.selectedMode,
            actualSystemUiLocale = uiState.actualSystemUiLocale,
            isApplying = uiState.isApplying,
            hasError = uiState.lastError != null,
            is24Hour = uiState.actualIs24Hour,
            isChecking = uiState.isCheckingSystem,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.showFaqDialog(true) },
                        modifier = Modifier.testTag("action_faq_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = stringResource(R.string.faq_icon_cd),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showOverflowMenu = true },
                            modifier = Modifier.testTag("action_overflow_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.overflow_menu_cd),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_restore)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Restore,
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.showRestoreDialog(true)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_adb_commands)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Code,
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.showAdbDialog(true)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_shizuku_connection)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.showShizukuSheet(true)
                                },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
                modifier = Modifier.testTag("top_app_bar"),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.TopCenter,
        ) {
            // Zero-Scroll Core Flow: flex the hero into the viewport when there is room;
            // fall back to scrolling on short viewports (landscape, small screens).
            val flexHero = maxHeight >= 560.dp
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp)
                    .then(if (!flexHero) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    // Horizontal safe-drawing keeps content clear of display cutouts in
                    // landscape; bottom keeps the gesture/navigation bar distance.
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // 1. Hero Clock (takes remaining height when there is room)
                HeroClock(
                    mode = uiState.selectedMode,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (flexHero) Modifier.weight(1f) else Modifier),
                )

                // 2. Format Selection: Connected M3 Toggle Group (subtitle lives inside each button)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.format_section_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    FormatToggleGroup(
                        selectedMode = uiState.selectedMode,
                        onModeSelect = { viewModel.selectMode(it) },
                    )
                }

                // 3. Single State-Aware Primary Action Button
                PrimaryActionBar(
                    action = primaryAction,
                    onActionClick = { act ->
                        when (act) {
                            PrimaryAction.Apply -> viewModel.applySelectedLocale()
                            PrimaryAction.Retry -> viewModel.applySelectedLocale()
                            PrimaryAction.GrantPermission -> viewModel.requestShizukuPermission()
                            PrimaryAction.StartShizuku -> viewModel.showShizukuSheet(true)
                            PrimaryAction.InstallShizuku -> viewModel.showShizukuSheet(true)
                            else -> {}
                        }
                    },
                    lastError = uiState.lastError,
                )
            }
        }
    }

    // Unified Shizuku Bottom Sheet (Zero-clutter, single-stop hub)
    if (uiState.showShizukuSheet) {
        ShizukuBottomSheet(
            status = uiState.shizukuStatus,
            onOpenShizuku = { viewModel.openShizukuApp() },
            onRequestPermission = { viewModel.requestShizukuPermission() },
            onRefreshStatus = { viewModel.refreshShizukuStatus() },
            onDismiss = { viewModel.showShizukuSheet(false) },
        )
    }

    if (uiState.showRestoreConfirmDialog) {
        RestoreConfirmDialog(
            onConfirm = { viewModel.restoreDefaultLocale() },
            onDismiss = { viewModel.showRestoreDialog(false) },
        )
    }

    if (uiState.showAdbCommandsDialog) {
        AdbCommandsDialog(
            mode = uiState.selectedMode,
            commands = viewModel.getAdbCommands(),
            restoreCommands = viewModel.getRestoreAdbCommands(),
            onDismiss = { viewModel.showAdbDialog(false) },
        )
    }

    if (uiState.showFaqDialog) {
        FaqAndTipsDialog(
            onDismiss = { viewModel.showFaqDialog(false) },
        )
    }
}
