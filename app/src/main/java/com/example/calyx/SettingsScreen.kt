package com.example.calyx

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val state           = viewModel.state.collectAsState().value
    val context         = LocalContext.current
    var showClearDialog by remember { mutableStateOf(false) }
    var showSaveDialog  by remember { mutableStateOf(false) }
    var channelIdInput  by remember { mutableStateOf(state.channelId) }

    LaunchedEffect(state.channelId) {
        channelIdInput = state.channelId
    }

    // ── Clear logs dialog ─────────────────────────────────────────
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            icon    = { Icon(Icons.Filled.Delete, null, tint = Color(0xFFE53935)) },
            title   = { Text("Clear All Logs?") },
            text    = {
                Text(
                    "This will permanently delete all alerts and recorded events. " +
                            "This action cannot be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllLogs()
                    showClearDialog = false
                }) {
                    Text("Clear", color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ── Save channel dialog ───────────────────────────────────────
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            icon    = { Icon(Icons.Filled.Settings, null, tint = Green700) },
            title   = { Text("Switch Channel?") },
            text    = {
                Text("App will reconnect to Channel ID: ${channelIdInput.trim()}")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.saveChannelConfig(channelIdInput.trim())
                    showSaveDialog = false
                }) {
                    Text("Save", color = Green700, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {

        Text(
            "Settings",
            fontSize   = 28.sp,
            fontWeight = FontWeight.Bold,
            color      = Green900
        )

        Spacer(Modifier.height(24.dp))

        // ── Appearance ────────────────────────────────────────────
        SettingsSectionTitle(icon = Icons.Filled.Edit, title = "Appearance")
        SettingsCard {
            SettingsToggleRow(
                icon            = Icons.Filled.Star,
                title           = "Dark Mode",
                subtitle        = "Adjust interface to low-light environments",
                checked         = state.isDarkMode,
                onCheckedChange = { viewModel.toggleDarkMode(it) }
            )
        }

        Spacer(Modifier.height(20.dp))

        // Channel Configuration
        SettingsSectionTitle(icon = Icons.Filled.Info, title = "Channel Configuration")
        SettingsCard {
            Column(modifier = Modifier.padding(16.dp)) {

                OutlinedTextField(
                    value         = channelIdInput,
                    onValueChange = { channelIdInput = it },
                    label         = { Text("Channel ID") },
                    placeholder   = { Text("e.g. 3246420") },
                    leadingIcon   = {
                        Icon(Icons.Filled.Info, null, tint = Green700)
                    },
                    supportingText = {
                        Text(
                            "Only public ThingSpeak channels supported",
                            fontSize = 11.sp,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    singleLine = true,
                    modifier   = Modifier.fillMaxWidth(),
                    colors     = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = Green700,
                        focusedLabelColor    = Green700,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick  = { showSaveDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Green700),
                    enabled  = channelIdInput.isNotBlank()
                            && channelIdInput.trim() != state.channelId
                ) {
                    Icon(Icons.Filled.Check, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Save & Reconnect",
                        color      = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // ── Humidity Field Selector ───────────────────────────────
        // Only shown once channel has loaded fields
        if (state.sensorFields.isNotEmpty()) {

            Spacer(Modifier.height(20.dp))

            SettingsSectionTitle(
                icon  = Icons.Filled.Star,
                title = "Humidity / Moisture Field"
            )

            SettingsCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Select which field represents soil humidity for AI prediction:",
                        fontSize   = 13.sp,
                        color      = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )

                    Spacer(Modifier.height(12.dp))

                    state.sensorFields.forEach { field ->
                        val isSelected = when {
                            // User has explicitly selected this field
                            state.humidityFieldKey.isNotBlank() ->
                                field.fieldKey == state.humidityFieldKey
                            // No selection yet — highlight auto-detected one
                            else -> field.fieldKey ==
                                    (state.sensorFields.firstOrNull {
                                        it.fieldName.contains("humid",   ignoreCase = true) ||
                                                it.fieldName.contains("moisture", ignoreCase = true) ||
                                                it.fieldName.contains("soil",    ignoreCase = true)
                                    }?.fieldKey ?: "")
                        }

                        FilterChip(
                            selected = isSelected,
                            onClick  = { viewModel.setHumidityField(field.fieldKey) },
                            label    = {
                                Text(
                                    "${field.fieldKey.uppercase()}: ${field.fieldName}",
                                    fontSize = 13.sp
                                )
                            },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        Icons.Filled.Check,
                                        null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor   = Green200,
                                selectedLabelColor       = Green900,
                                selectedLeadingIconColor = Green900,
                                containerColor           = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor               = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    // Show which field is currently being used for prediction
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Prediction uses: ${state.humidityFieldName}",
                        fontSize   = 11.sp,
                        color      = Green700,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Data Management ───────────────────────────────────────
        SettingsSectionTitle(icon = Icons.Filled.Build, title = "Data Management")

        SettingsCard {
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Delete,
                    null,
                    tint     = Color(0xFFE53935),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    "Clear All Logs",
                    color      = Color(0xFFE53935),
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.weight(1f)
                )
                IconButton(onClick = { showClearDialog = true }) {
                    Icon(Icons.Filled.ArrowForward, null, tint = Color.Gray)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                val sensorCsv = viewModel.buildSensorCsv()
                val alertsCsv = viewModel.buildAlertsCsv()
                val fullCsv   = "SENSOR DATA\n$sensorCsv\nALERT LOG\n$alertsCsv"

                val file = java.io.File(context.cacheDir, "calyx_export.csv")
                file.writeText(fullCsv)

                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    file
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_SUBJECT, "Calyx Sensor Export — ${state.lastSync}")
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Export logs via"))
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape  = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Green900)
        ) {
            Icon(Icons.Filled.Share, null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text(
                "Export Logs",
                color      = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize   = 16.sp
            )
        }

        Spacer(Modifier.height(20.dp))

        // ── Technical Info ────────────────────────────────────────
        SettingsSectionTitle(icon = Icons.Filled.Settings, title = "Technical Info")

        SettingsCard {
            TechInfoRow("Last Sync",  state.lastSync)
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color    = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
            TechInfoRow("Channel ID", state.channelId)
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color    = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
            TechInfoRow(
                "Humidity Field",
                if (state.humidityFieldKey.isNotBlank())
                    "${state.humidityFieldKey.uppercase()} (manual)"
                else
                    "${state.humidityFieldName} (auto)"
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "PRECISION ORGANICISM V2.4.1",
            modifier      = Modifier.fillMaxWidth(),
            fontSize      = 11.sp,
            color         = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp,
            textAlign     = TextAlign.Center
        )
    }
}

// ── Reusable components ───────────────────────────────────────────

@Composable
fun SettingsSectionTitle(icon: ImageVector, title: String) {
    Row(
        modifier          = Modifier.padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Green700, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            title,
            fontWeight = FontWeight.Bold,
            fontSize   = 16.sp,
            color      = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        content   = { Column(content = content) }
    )
}

@Composable
fun SettingsToggleRow(
    icon:            ImageVector,
    title:           String,
    subtitle:        String,
    checked:         Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Green700, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontWeight = FontWeight.SemiBold,
                fontSize   = 15.sp,
                color      = MaterialTheme.colorScheme.onSurface
            )
            Text(
                subtitle,
                fontSize = 12.sp,
                color    = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked         = checked,
            onCheckedChange = onCheckedChange,
            colors          = SwitchDefaults.colors(
                checkedThumbColor   = Color.White,
                checkedTrackColor   = Green700,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}

@Composable
fun TechInfoRow(label: String, value: String) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(
            label,
            fontSize = 14.sp,
            color    = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            fontSize   = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.onSurface
        )
    }
}