package com.example.ui.screens

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.entity.CloneInstanceEntity
import com.example.ui.components.CloneCard
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.RenameCloneDialog
import com.example.ui.components.StorageSheet
import com.example.ui.components.TelemetryHud
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CardElevated
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AppClonerViewModel
import java.io.File
import java.io.FileOutputStream

@Composable
fun DashboardScreen(
    viewModel: AppClonerViewModel,
    onNavigateToDiscovery: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clones by viewModel.clones.collectAsStateWithLifecycle()
    val stats by viewModel.engineStats.collectAsStateWithLifecycle()

    var selectedForStorage by remember { mutableStateOf<CloneInstanceEntity?>(null) }
    var selectedForRename by remember { mutableStateOf<CloneInstanceEntity?>(null) }
    var selectedForDelete by remember { mutableStateOf<CloneInstanceEntity?>(null) }

    // SAF Document Picker for external APKs
    val apkPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { selectedUri ->
            try {
                val inputStream = context.contentResolver.openInputStream(selectedUri)
                val tempApk = File(context.cacheDir, "imported_${System.currentTimeMillis()}.apk")
                FileOutputStream(tempApk).use { out ->
                    inputStream?.copyTo(out)
                }
                viewModel.importApk(tempApk)
            } catch (e: Exception) {
                // Handled in ViewModel toast
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg),
        containerColor = ObsidianBg,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToDiscovery,
                containerColor = CyanPrimary,
                contentColor = Color(0xFF041E26),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("fab_add_clone")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Clone Application",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Dashboard Cyber Header
            DashboardTopBar(
                onRefresh = { viewModel.refreshStats() },
                onSettings = onNavigateToSettings
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Telemetry Metrics HUD
            TelemetryHud(
                stats = stats,
                cloneCount = clones.size
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Section Header & SAF Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "VIRTUAL INSTANCES",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary,
                        letterSpacing = 1.sp
                    )
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(CardElevated)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${clones.size}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanPrimary
                        )
                    }
                }

                Text(
                    text = "Import .APK",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanPrimary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .border(1.dp, CyanPrimary.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("action_import_apk")
                        .clickable {
                            apkPickerLauncher.launch(arrayOf("application/vnd.android.package-archive", "*/*"))
                        }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Clones Grid or Empty State
            if (clones.isEmpty()) {
                EmptyClonesZeroState(
                    onDeployClick = onNavigateToDiscovery,
                    onImportApkClick = {
                        apkPickerLauncher.launch(arrayOf("application/vnd.android.package-archive", "*/*"))
                    }
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("clones_grid")
                ) {
                    items(clones, key = { it.id }) { clone ->
                        CloneCard(
                            clone = clone,
                            onLaunch = { viewModel.launchClone(context, clone) },
                            onCloneAgain = { viewModel.cloneAgain(clone) },
                            onRename = { selectedForRename = clone },
                            onStorageDetails = { selectedForStorage = clone },
                            onClearCache = { viewModel.clearCache(clone) },
                            onClearData = { viewModel.clearData(clone) },
                            onDelete = { selectedForDelete = clone }
                        )
                    }
                }
            }
        }
    }

    // Storage Details Bottom Sheet
    selectedForStorage?.let { clone ->
        StorageSheet(
            clone = clone,
            onDismiss = { selectedForStorage = null },
            onClearCache = {
                viewModel.clearCache(clone)
                selectedForStorage = null
            },
            onClearData = {
                viewModel.clearData(clone)
                selectedForStorage = null
            },
            onDeleteClone = {
                viewModel.deleteClone(clone)
                selectedForStorage = null
            }
        )
    }

    // Rename Dialog
    selectedForRename?.let { clone ->
        RenameCloneDialog(
            clone = clone,
            onDismiss = { selectedForRename = null },
            onConfirm = { newName ->
                viewModel.renameClone(clone.id, newName)
                selectedForRename = null
            }
        )
    }

    // Confirm Delete Dialog
    selectedForDelete?.let { clone ->
        ConfirmDeleteDialog(
            clone = clone,
            onDismiss = { selectedForDelete = null },
            onConfirm = {
                viewModel.deleteClone(clone)
                selectedForDelete = null
            }
        )
    }
}

@Composable
private fun DashboardTopBar(
    onRefresh: () -> Unit,
    onSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "APP CLONER",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary,
                    letterSpacing = 1.sp
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(CyanPrimary.copy(alpha = 0.2f))
                        .border(1.dp, CyanPrimary.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "BLACKBOX",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = CyanPrimary,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(StatusGreen)
                )
                Text(
                    text = "Virtual Sandboxes Ready",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(
                onClick = onRefresh,
                modifier = Modifier.testTag("topbar_refresh_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = TextSecondary
                )
            }

            IconButton(
                onClick = onSettings,
                modifier = Modifier.testTag("topbar_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun EmptyClonesZeroState(
    onDeployClick: () -> Unit,
    onImportApkClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 40.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(CardElevated)
                .border(2.dp, CyanPrimary.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = CyanPrimary,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "No Virtual Clones Deployed",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Text(
            text = "Import applications installed on your phone into an isolated virtual sandbox. Run independent accounts for WhatsApp, Telegram, Discord, and more with zero shared data.",
            fontSize = 13.sp,
            color = TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )

        Button(
            onClick = onDeployClick,
            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = Color(0xFF041E26)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(48.dp)
                .testTag("empty_state_deploy_button")
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(8.dp))
            Text("Clone Installed App", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onImportApkClick,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanPrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(44.dp)
                .testTag("empty_state_import_apk_button")
        ) {
            Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.size(8.dp))
            Text("Import .APK From Storage", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
