package com.example.calyx

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class AlertEntry(
    val id:          Int,
    val title:       String,
    val description: String,
    val badge:       String,
    val timestamp:   String,
    val type:        AlertType
)

enum class AlertType { DRY, HIGH_TEMP, FROST, OK }

data class AppState(
    val sensorFields:       List<NetworkUtils.FieldData> = emptyList(),
    val currentHumidity:    Float                        = 0f,
    val currentTemperature: Float                        = 0f,
    val humidityFieldName:  String                       = "Humidity",
    val tempFieldName:      String                       = "Temperature",
    val humidityFieldKey:   String                       = "", // user selected
    val prediction:         String                       = "Syncing...",
    val lastSync:           String                       = "Never",
    val alerts:             List<AlertEntry>             = emptyList(),
    val isLoading:          Boolean                      = true,
    val hasError:           Boolean                      = false,
    val isDarkMode:         Boolean                      = false,
    val channelId:          String                       = Constants.CHANNEL_ID
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext

    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state

    private var alertIdCounter = 0
    private var lastAlertType: AlertType = AlertType.OK
    private var pollingJob: kotlinx.coroutines.Job? = null

    init {
        viewModelScope.launch {
            // Load both channelId and humidityFieldKey together
            combine(
                PreferencesManager.getChannelId(context),
                PreferencesManager.getHumidityFieldKey(context)
            ) { channelId, humidityKey ->
                Pair(channelId, humidityKey)
            }.collectLatest { (channelId, humidityKey) ->
                _state.value = _state.value.copy(
                    channelId        = channelId,
                    humidityFieldKey = humidityKey
                )
                pollingJob?.cancel()
                pollingJob = launch(Dispatchers.IO) {
                    startPolling(channelId, humidityKey)
                }
            }
        }
    }

    private suspend fun startPolling(channelId: String, humidityKey: String) {
        while (true) {
            _state.value = _state.value.copy(isLoading = true, hasError = false)

            val data = NetworkUtils.fetchSensorHistory(channelId)

            if (data.fields.isNotEmpty()) {
                // Use user-selected key or auto-detect
                val resolvedHumidityReadings = data.humidityReadings(humidityKey)
                val resolvedHumidity         = data.currentHumidity(humidityKey)
                val resolvedHumidityName     = data.humidityFieldName(humidityKey)
                val prediction               = computePrediction(resolvedHumidityReadings)
                val newAlerts                = checkThresholds(
                    humidity    = resolvedHumidity,
                    temperature = data.currentTemperature,
                    existing    = _state.value.alerts
                )

                _state.value = _state.value.copy(
                    sensorFields       = data.fields,
                    currentHumidity    = resolvedHumidity,
                    currentTemperature = data.currentTemperature,
                    humidityFieldName  = resolvedHumidityName,
                    tempFieldName      = data.tempFieldName,
                    prediction         = prediction,
                    lastSync           = formatTime(data.lastSyncTime),
                    alerts             = newAlerts,
                    isLoading          = false,
                    hasError           = false
                )
            } else {
                _state.value = _state.value.copy(
                    isLoading  = false,
                    hasError   = true,
                    prediction = "Unable to sync"
                )
            }

            delay(Constants.POLLING_INTERVAL_MS)
        }
    }

    // ── Set user-selected humidity field ──────────────────────────
    fun setHumidityField(fieldKey: String) {
        viewModelScope.launch {
            PreferencesManager.saveHumidityFieldKey(context, fieldKey)
            // collectLatest above auto-restarts polling with new key
        }
    }

    // ── Save channel ID ───────────────────────────────────────────
    fun saveChannelConfig(channelId: String) {
        viewModelScope.launch {
            PreferencesManager.saveChannelId(context, channelId)
        }
    }

    // ── Dark mode ─────────────────────────────────────────────────
    fun toggleDarkMode(enabled: Boolean) {
        _state.value = _state.value.copy(isDarkMode = enabled)
    }

    // ── Clear all alerts ──────────────────────────────────────────
    fun clearAllLogs() {
        _state.value = _state.value.copy(alerts = emptyList())
        lastAlertType = AlertType.OK
    }

    // Dismiss single alert
    fun dismissAlert(id: Int) {
        _state.value = _state.value.copy(
            alerts = _state.value.alerts.filter { it.id != id }
        )
    }

    // ── Get predicted minutes for calendar ───────────────────────
    fun getPredictedMinutes(): Int {
        val prediction = _state.value.prediction
        return when {
            prediction.contains("mins") ->
                Regex("~(\\d+) mins").find(prediction)
                    ?.groupValues?.get(1)?.toIntOrNull() ?: 30
            prediction.contains("hrs")  -> {
                val hours = Regex("~(\\d+) hrs").find(prediction)
                    ?.groupValues?.get(1)?.toIntOrNull() ?: 1
                hours * 60
            }
            prediction.contains("now")  -> 0
            else                        -> 30
        }
    }

    // ── Build sensor CSV ──────────────────────────────────────────
    fun buildSensorCsv(): String {
        val sb     = StringBuilder()
        val chId   = _state.value.channelId
        val fields = _state.value.sensorFields

        if (fields.isEmpty()) return "No data available"

        val headers = fields.joinToString(",") { "\"${it.fieldName}\"" }
        sb.appendLine("Channel ID,Reading Index,$headers")

        val maxReadings = fields.maxOf { it.readings.size }
        for (i in 0 until maxReadings) {
            val values = fields.joinToString(",") { field ->
                field.readings.getOrNull(i)?.toString() ?: "N/A"
            }
            sb.appendLine("$chId,${i + 1},$values")
        }
        return sb.toString()
    }

    //Build alerts CSV
    fun buildAlertsCsv(): String {
        val sb   = StringBuilder()
        val chId = _state.value.channelId

        sb.appendLine("Channel ID,Alert Title,Description,Badge,Timestamp,Type")
        _state.value.alerts.forEach { alert ->
            sb.appendLine(
                "$chId," +
                        "\"${alert.title}\"," +
                        "\"${alert.description}\"," +
                        "\"${alert.badge}\"," +
                        "\"${alert.timestamp}\"," +
                        "\"${alert.type.name}\""
            )
        }
        return sb.toString()
    }

    //Linear regression prediction
    private fun computePrediction(data: List<Float>): String {
        val n = data.size
        if (n < 3) return "Gathering data..."

        val xValues = (0 until n).map { it.toDouble() }
        val yValues = data.map { it.toDouble() }

        val xSum   = xValues.sum()
        val ySum   = yValues.sum()
        val xySum  = xValues.zip(yValues).sumOf { (x, y) -> x * y }
        val xSqSum = xValues.sumOf { it * it }
        val denom  = n * xSqSum - xSum * xSum

        if (denom == 0.0) return "Soil is stable"

        val slope = (n * xySum - xSum * ySum) / denom

        return if (slope < 0) {
            val current           = data.last().toDouble()
            val pointsToThreshold = (Constants.DRY_THRESHOLD - current) / slope
            val minutesToDry      = (pointsToThreshold * Constants.LOGGING_INTERVAL_MINS).toInt()
            when {
                minutesToDry <= 0 -> "Water needed now!"
                minutesToDry < 60 -> "Water needed in ~$minutesToDry mins"
                else              -> "Water needed in ~${minutesToDry / 60} hrs"
            }
        } else {
            "Soil is stable 🌱"
        }
    }

    //Threshold checker
    private fun checkThresholds(
        humidity:    Float,
        temperature: Float,
        existing:    List<AlertEntry>
    ): List<AlertEntry> {
        val now       = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        val newAlerts = existing.toMutableList()

        val currentType = when {
            humidity    < Constants.DRY_THRESHOLD       -> AlertType.DRY
            temperature > Constants.HIGH_TEMP_THRESHOLD -> AlertType.HIGH_TEMP
            temperature < Constants.FROST_THRESHOLD     -> AlertType.FROST
            else                                        -> AlertType.OK
        }

        if (currentType != AlertType.OK && currentType != lastAlertType) {
            val newAlert = when (currentType) {
                AlertType.DRY -> AlertEntry(
                    id          = ++alertIdCounter,
                    title       = "Soil is Dry!",
                    description = "Humidity dropped to ${humidity.toInt()}%. Immediate irrigation required.",
                    badge       = "${humidity.toInt()}% HUMIDITY",
                    timestamp   = now,
                    type        = AlertType.DRY
                )
                AlertType.HIGH_TEMP -> AlertEntry(
                    id          = ++alertIdCounter,
                    title       = "High Temperature!",
                    description = "Ambient temperature reached ${temperature.toInt()}°C. Check ventilation.",
                    badge       = "${temperature.toInt()}°C TEMP",
                    timestamp   = now,
                    type        = AlertType.HIGH_TEMP
                )
                AlertType.FROST -> AlertEntry(
                    id          = ++alertIdCounter,
                    title       = "Frost Alert!",
                    description = "Temperature dropped to ${temperature.toInt()}°C. Crops at risk.",
                    badge       = "${temperature.toInt()}°C TEMP",
                    timestamp   = now,
                    type        = AlertType.FROST
                )
                AlertType.OK -> null
            }
            newAlert?.let { newAlerts.add(0, it) }
        }

        lastAlertType = currentType
        return newAlerts
    }

    //Format ISO timestamp
    private fun formatTime(iso: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            parser.timeZone = TimeZone.getTimeZone("UTC")
            val date      = parser.parse(iso) ?: return iso
            val formatter = SimpleDateFormat("MMM dd · HH:mm", Locale.getDefault())
            formatter.format(date)
        } catch (e: Exception) {
            iso
        }
    }
}