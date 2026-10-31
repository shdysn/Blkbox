package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.entity.CloneInstanceEntity
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CardElevated
import com.example.ui.theme.CardSurface
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.util.AppIconHelper

@Composable
fun CloneCard(
    clone: CloneInstanceEntity,
    onLaunch: () -> Unit,
    onCloneAgain: () -> Unit,
    onRename: () -> Unit,
    onStorageDetails: () -> Unit,
    onClearCache: () -> Unit,
    onClearData: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    val appIcon = remember(clone.packageName) {
        AppIconHelper.getAppIcon(context, clone.packageName)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardSurface)
            .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
            .testTag("clone_card_${clone.packageName}_${clone.userId}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // Header Row: App Icon + Badge + Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App Icon with glow container
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardElevated)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (appIcon != null) {
                        Image(
                            bitmap = appIcon,
                            contentDescription = clone.appName,
                            modifier = Modifier.size(34.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(CyanPrimary.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = clone.appName.take(1).uppercase(),
                                color = CyanPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }

                // Instance Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyanPrimary.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "#${clone.cloneIndex}",
                        color = CyanPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                // More Options Menu Anchor
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("clone_card_menu_${clone.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(CardElevated)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Launch Sandbox", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.PlayArrow, null, tint = CyanPrimary) },
                            onClick = {
                                showMenu = false
                                onLaunch()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Create Clone (+1)", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.Add, null, tint = StatusGreen) },
                            onClick = {
                                showMenu = false
                                onCloneAgain()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Storage & Data", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.Info, null, tint = TextSecondary) },
                            onClick = {
                                showMenu = false
                                onStorageDetails()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Rename Clone", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.Edit, null, tint = TextSecondary) },
                            onClick = {
                                showMenu = false
                                onRename()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Clear Cache", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.Refresh, null, tint = StatusAmber) },
                            onClick = {
                                showMenu = false
                                onClearCache()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Wipe Data", color = StatusRed) },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = StatusRed) },
                            onClick = {
                                showMenu = false
                                onClearData()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Remove Clone", color = StatusRed) },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = StatusRed) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // App Label & Custom Name
            Text(
                text = clone.customName,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = clone.packageName,
                fontSize = 11.sp,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )

            // Storage / Status metadata
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sandbox #${clone.userId}",
                    fontSize = 10.sp,
                    color = TextMuted
                )
                Text(
                    text = Formatters.formatBytes(clone.dataSizeBytes + clone.cacheSizeBytes),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Launch Button
            Button(
                onClick = onLaunch,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanPrimary,
                    contentColor = Color(0xFF041E26)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .testTag("launch_button_${clone.id}")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "LAUNCH",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}
