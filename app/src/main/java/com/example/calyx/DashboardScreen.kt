package com.example.calyx

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*

val Green900 = Color(0xFF1B5E20)
val Green700 = Color(0xFF388E3C)
val Green200 = Color(0xFFA5D6A7)
val BgColor  = Color(0xFFF0F4E8)

// ── Chart color pairs (barColor to labelColor) ────────────────────
private val chartColors = listOf(
    Pair(Color(0xFF1B5E20), Color(0xFF388E3C)), // green   — humidity/moisture
    Pair(Color(0xFF6D4C41), Color(0xFF8D6E63)), // brown   — temperature
    Pair(Color(0xFF1565C0), Color(0xFF1976D2)), // blue    — pressure/water
    Pair(Color(0xFF6A1B9A), Color(0xFF7B1FA2)), // purple  — CO2/gas
    Pair(Color(0xFF00695C), Color(0xFF00796B)), // teal    — pH
    Pair(Color(0xFFE65100), Color(0xFFF57C00)), // orange  — light/UV
    Pair(Color(0xFF37474F), Color(0xFF455A64)), // grey    — generic
    Pair(Color(0xFF880E4F), Color(0xFFAD1457)), // pink    — misc
)

// ── Detect unit from field name ───────────────────────────────────
private fun detectUnit(fieldName: String): String = when {
    fieldName.contains("temp",   ignoreCase = true) -> "°C"
    fieldName.contains("humid",  ignoreCase = true) -> "%"
    fieldName.contains("moist",  ignoreCase = true) -> "%"
    fieldName.contains("soil",   ignoreCase = true) -> "%"
    fieldName.contains("ph",     ignoreCase = true) -> "pH"
    fieldName.contains("light",  ignoreCase = true) -> "lx"
    fieldName.contains("press",  ignoreCase = true) -> "hPa"
    fieldName.contains("co2",    ignoreCase = true) -> "ppm"
    fieldName.contains("wind",   ignoreCase = true) -> "m/s"
    fieldName.contains("rain",   ignoreCase = true) -> "mm"
    else                                             -> ""
}

@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    val state        by viewModel.state.collectAsState()
    val scrollState   = rememberScrollState()

    val animatedMoisture by animateFloatAsState(
        targetValue   = state.currentHumidity / 100f,
        animationSpec = tween(1000, easing = EaseInOutCubic),
        label         = "moisture_gauge"
    )

    val bgColor          = MaterialTheme.colorScheme.background
    val surfaceColor     = MaterialTheme.colorScheme.surface
    val onSurfaceColor   = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val primaryColor     = MaterialTheme.colorScheme.primary

    // For calendar intent
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {

        // ── Header ────────────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                "Dashboard",
                fontSize   = 28.sp,
                fontWeight = FontWeight.Bold,
                color      = primaryColor
            )
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier    = Modifier.size(24.dp),
                    color       = primaryColor,
                    strokeWidth = 2.dp
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Moisture Gauge Card ───────────────────────────────────
        Card(
            modifier  = Modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(24.dp),
            colors    = CardDefaults.cardColors(containerColor = surfaceColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier         = Modifier
                    .fillMaxWidth()
                    .padding(32.dp)
            ) {
                CircularProgressIndicator(
                    progress    = { animatedMoisture },
                    modifier    = Modifier.size(180.dp),
                    strokeWidth = 14.dp,
                    color       = Green900,
                    trackColor  = Green200,
                    strokeCap   = StrokeCap.Round
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${state.currentHumidity.toInt()}%",
                        fontSize   = 52.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = onSurfaceColor
                    )
                    Text(
                        // Show actual field name or fallback
                        state.humidityFieldName.uppercase(),
                        fontSize      = 11.sp,
                        color         = onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── AI Forecast Card ──────────────────────────────────────
        Card(
            modifier  = Modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(24.dp),
            colors    = CardDefaults.cardColors(containerColor = Green900),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "AI FORECAST",
                    color         = Color.White.copy(alpha = 0.6f),
                    fontSize      = 11.sp,
                    letterSpacing = 1.5.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Predicted:\n${state.prediction}",
                    color      = Color.White,
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 30.sp
                )
                Spacer(Modifier.height(16.dp))
                FilledTonalButton(
                    onClick = {
                        val minutes   = viewModel.getPredictedMinutes()
                        val startTime = System.currentTimeMillis() + (minutes * 60 * 1000L)
                        val endTime   = startTime + (30 * 60 * 1000L)

                        val calendarIntent = android.content.Intent(
                            android.content.Intent.ACTION_INSERT
                        ).apply {
                            data = android.provider.CalendarContract.Events.CONTENT_URI
                            putExtra(
                                android.provider.CalendarContract.Events.TITLE,
                                "💧 Irrigate Crops — Calyx Alert"
                            )
                            putExtra(
                                android.provider.CalendarContract.Events.DESCRIPTION,
                                "Predicted by Calyx AI: ${state.prediction}"
                            )
                            putExtra(
                                android.provider.CalendarContract.Events.EVENT_LOCATION,
                                "Channel: ${Constants.CHANNEL_ID}"
                            )
                            putExtra(
                                android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME,
                                startTime
                            )
                            putExtra(
                                android.provider.CalendarContract.EXTRA_EVENT_END_TIME,
                                endTime
                            )
                            putExtra(
                                android.provider.CalendarContract.Events.HAS_ALARM,
                                1
                            )
                        }
                        try {
                            context.startActivity(calendarIntent)
                        } catch (e: android.content.ActivityNotFoundException) {
                            // No calendar app — do nothing
                        }
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Green200,
                        contentColor   = Green900
                    ),
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Text("Schedule Irrigation →", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Dynamic Sensor Charts ─────────────────────────────────
        // Renders a chart for every field found in the channel
        if (state.sensorFields.isEmpty()) {
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(24.dp),
                colors    = CardDefaults.cardColors(containerColor = surfaceColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier         = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Waiting for sensor data...",
                        color    = onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            state.sensorFields.forEachIndexed { index, field ->
                val (barColor, labelColor) = chartColors[index % chartColors.size]
                val unit                   = detectUnit(field.fieldName)

                SensorTrendCard(
                    title      = field.fieldName,
                    subtitle   = "Last ${field.readings.size} readings",
                    dataPoints = field.readings,
                    barColor   = barColor,
                    labelColor = labelColor,
                    icon       = Icons.Filled.Info,
                    unit       = unit
                )

                if (index < state.sensorFields.lastIndex) {
                    Spacer(Modifier.height(16.dp))
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

// ── Reusable Trend Card ───────────────────────────────────────────
@Composable
fun SensorTrendCard(
    title:      String,
    subtitle:   String,
    dataPoints: List<Float>,
    barColor:   Color,
    labelColor: Color,
    icon:       ImageVector,
    unit:       String
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(24.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint     = labelColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        title,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 16.sp,
                        color      = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        subtitle,
                        fontSize = 12.sp,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            if (dataPoints.isEmpty()) {
                Box(
                    modifier         = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Waiting for data...",
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            } else {
                AnimatedBarChart(
                    dataPoints = dataPoints,
                    barColor   = barColor,
                    unit       = unit
                )
            }
        }
    }
}

// ── Animated Bar Chart ────────────────────────────────────────────
@Composable
fun AnimatedBarChart(dataPoints: List<Float>, barColor: Color, unit: String) {
    if (dataPoints.isEmpty()) return

    val maxValue = dataPoints.maxOrNull() ?: 1f
    val minValue = dataPoints.minOrNull() ?: 0f
    val range    = (maxValue - minValue).coerceAtLeast(1f)

    val yMax = maxValue
    val yMid = minValue + range / 2f
    val yMin = minValue

    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    val animatedFractions = dataPoints.mapIndexed { index, point ->
        val fraction = ((point - minValue) / range).coerceIn(0.05f, 1f)
        val animated by animateFloatAsState(
            targetValue   = fraction,
            animationSpec = tween(700, delayMillis = index * 35, easing = EaseOutBack),
            label         = "bar_$index"
        )
        animated
    }

    Row(modifier = Modifier.fillMaxWidth()) {

        // ── Y-axis ────────────────────────────────────────────────
        Box(modifier = Modifier.width(38.dp).height(120.dp)) {
            Text(
                "${yMax.toInt()}$unit",
                modifier  = Modifier.align(Alignment.TopEnd),
                fontSize  = 9.sp,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End
            )
            Text(
                "${yMid.toInt()}$unit",
                modifier  = Modifier.align(Alignment.CenterEnd),
                fontSize  = 9.sp,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End
            )
            Text(
                "${yMin.toInt()}$unit",
                modifier  = Modifier.align(Alignment.BottomEnd),
                fontSize  = 9.sp,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End
            )
        }

        Spacer(Modifier.width(6.dp))

        Column(modifier = Modifier.weight(1f)) {

            // ── Tooltip ───────────────────────────────────────────
            Box(
                modifier         = Modifier
                    .fillMaxWidth()
                    .height(24.dp),
                contentAlignment = Alignment.Center
            ) {
                val tooltipAlpha by animateFloatAsState(
                    targetValue   = if (selectedIndex != null) 1f else 0f,
                    animationSpec = tween(150),
                    label         = "tooltip_alpha"
                )
                val tooltipScale by animateFloatAsState(
                    targetValue   = if (selectedIndex != null) 1f else 0.85f,
                    animationSpec = tween(150),
                    label         = "tooltip_scale"
                )

                Surface(
                    shape    = RoundedCornerShape(6.dp),
                    color    = barColor.copy(alpha = tooltipAlpha),
                    modifier = Modifier.graphicsLayer {
                        alpha  = tooltipAlpha
                        scaleX = tooltipScale
                        scaleY = tooltipScale
                    }
                ) {
                    Row(
                        modifier              = Modifier.padding(
                            horizontal = 8.dp,
                            vertical   = 3.dp
                        ),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "Reading ${(selectedIndex ?: 0) + 1}:",
                            fontSize = 10.sp,
                            color    = Color.White.copy(alpha = 0.75f * tooltipAlpha),
                            maxLines = 1
                        )
                        Text(
                            "${dataPoints.getOrNull(selectedIndex ?: 0)?.toInt() ?: 0}$unit",
                            fontSize   = 10.sp,
                            color      = Color.White.copy(alpha = tooltipAlpha),
                            fontWeight = FontWeight.Bold,
                            maxLines   = 1
                        )
                    }
                }
            }

            Spacer(Modifier.height(2.dp))

            // ── Bars ──────────────────────────────────────────────
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment     = Alignment.Bottom
            ) {
                animatedFractions.forEachIndexed { index, fraction ->
                    val isSelected = selectedIndex == index
                    val isLatest   = index == dataPoints.lastIndex

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(fraction)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(
                                when {
                                    isSelected -> barColor
                                    isLatest   -> barColor.copy(alpha = 0.6f)
                                    else       -> barColor.copy(alpha = 0.25f)
                                }
                            )
                            .clickable {
                                selectedIndex =
                                    if (selectedIndex == index) null else index
                            }
                    )
                }
            }

            HorizontalDivider(
                color     = MaterialTheme.colorScheme.outline,
                thickness = 0.5.dp
            )

            Spacer(Modifier.height(4.dp))

            // ── X-axis ────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                dataPoints.forEachIndexed { index, _ ->
                    val label = when (index) {
                        0                    -> "0"
                        dataPoints.size / 2  -> "${dataPoints.size / 2}"
                        dataPoints.lastIndex -> "${dataPoints.lastIndex}"
                        else                 -> ""
                    }
                    Text(
                        label,
                        modifier   = Modifier.weight(1f),
                        fontSize   = 9.sp,
                        color      = if (index == dataPoints.lastIndex)
                            barColor
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (index == dataPoints.lastIndex)
                            FontWeight.Bold
                        else
                            FontWeight.Normal,
                        textAlign  = TextAlign.Center,
                        maxLines   = 1
                    )
                }
            }
        }
    }
}