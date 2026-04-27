package com.example.calyx

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(viewModel: MainViewModel) {
    val state   = viewModel.state.collectAsState().value
    val context = LocalContext.current
    val alerts  = state.alerts

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Alerts",
                            fontWeight = FontWeight.Bold,
                            fontSize   = 24.sp,
                            color      = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "${alerts.size} notification${if (alerts.size != 1) "s" else ""}",
                            fontSize = 12.sp,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // ── Always show share button ──────────────────
                    IconButton(onClick = {
                        val csv  = "ALERT LOG\n${viewModel.buildAlertsCsv()}"
                        val file = java.io.File(context.cacheDir, "calyx_alerts.csv")
                        file.writeText(csv)

                        val uri = androidx.core.content.FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.provider",
                            file
                        )
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/csv"
                            putExtra(Intent.EXTRA_SUBJECT, "Calyx Alert Report")
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share alerts via"))
                    }) {
                        Icon(
                            Icons.Filled.Share,
                            "Share",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background  // ← fixes dark mode
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background  // ← fixes dark mode
    ) { padding ->

        if (alerts.isEmpty()) {
            // ── Empty state ───────────────────────────────────────
            Box(
                modifier         = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint     = MaterialTheme.colorScheme.primary,  // ← theme color
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "All Clear!",
                        fontSize   = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.primary  // ← theme color
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "No threshold breaches detected.\nYour crops are healthy.",
                        fontSize   = 14.sp,
                        color      = MaterialTheme.colorScheme.onSurfaceVariant,  // ← theme
                        textAlign  = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }
            }

        } else {
            // ── Alert list ────────────────────────────────────────
            LazyColumn(
                modifier            = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(alerts, key = { it.id }) { alert ->
                    SwipeToDeleteAlertCard(
                        alert    = alert,
                        onDelete = { viewModel.dismissAlert(alert.id) }
                    )
                }
            }
        }
    }
}

// ── Swipe to delete wrapper ───────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteAlertCard(alert: AlertEntry, onDelete: () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete(); true
            } else false
        }
    )

    SwipeToDismissBox(
        state                       = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier         = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFE53935), RoundedCornerShape(20.dp))
                    .padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.White)
            }
        }
    ) {
        AlertCard(alert)
    }
}

// ── Single alert card ─────────────────────────────────────────────
@Composable
fun AlertCard(alert: AlertEntry) {
    val (icon, iconBg, accent) = when (alert.type) {
        AlertType.DRY       -> Triple(Icons.Filled.Warning,      Color(0xFFFFCDD2), Color(0xFFE53935))
        AlertType.HIGH_TEMP -> Triple(Icons.Filled.Info,         Color(0xFFFFE0B2), Color(0xFFE65100))
        AlertType.FROST     -> Triple(Icons.Filled.Warning,      Color(0xFFBBDEFB), Color(0xFF1565C0))
        AlertType.OK        -> Triple(Icons.Filled.CheckCircle,  Color(0xFFC8E6C9), Color(0xFF2E7D32))
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface  // ← theme color
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (alert.type == AlertType.DRY ||
                alert.type == AlertType.FROST) 4.dp else 1.dp
        )
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Left accent bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(60.dp)
                    .background(accent, RoundedCornerShape(4.dp))
            )

            Spacer(Modifier.width(12.dp))

            // Icon bubble
            Surface(
                modifier = Modifier.size(44.dp),
                shape    = CircleShape,
                color    = iconBg
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = accent, modifier = Modifier.size(22.dp))
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        alert.title,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 15.sp,
                        color      = MaterialTheme.colorScheme.onSurface  // ← theme
                    )
                    Text(
                        alert.timestamp,
                        fontSize = 11.sp,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant  // ← theme
                    )
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    alert.description,
                    fontSize   = 13.sp,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant,  // ← theme
                    lineHeight = 18.sp
                )

                Spacer(Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = iconBg
                ) {
                    Text(
                        alert.badge,
                        modifier   = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = accent
                    )
                }
            }
        }
    }
}

// ── Section header ────────────────────────────────────────────────
@Composable
fun SectionHeader(text: String) {
    Text(
        text          = text,
        fontSize      = 12.sp,
        fontWeight    = FontWeight.Bold,
        color         = MaterialTheme.colorScheme.onSurfaceVariant,  // ← theme
        letterSpacing = 1.5.sp,
        modifier      = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
    )
}