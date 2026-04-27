package com.example.calyx

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object NetworkUtils {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    data class FieldData(
        val fieldKey:  String,
        val fieldName: String,
        val readings:  List<Float>
    )

    data class SensorData(
        val fields:       List<FieldData>,
        val lastSyncTime: String
    ) {
        //Auto-detect humidity by name keywords
        val humidityField: FieldData?
            get() = fields.firstOrNull {
                it.fieldName.contains("humid",   ignoreCase = true) ||
                        it.fieldName.contains("moisture", ignoreCase = true) ||
                        it.fieldName.contains("soil",    ignoreCase = true)
            }

        // ── Auto-detect temperature by name keywords
        val temperatureField: FieldData?
            get() = fields.firstOrNull {
                it.fieldName.contains("temp", ignoreCase = true)
            }

        fun getHumidityField(userSelectedKey: String): FieldData? {
            return if (userSelectedKey.isNotBlank()) {
                // User has explicitly picked a field
                fields.firstOrNull { it.fieldKey == userSelectedKey }
                    ?: humidityField  // fallback if key no longer exists
            } else {
                // No user selection yet — use auto-detect
                humidityField
            }
        }

        // Convenience shortcuts using the resolved field
        fun currentHumidity(userSelectedKey: String): Float =
            getHumidityField(userSelectedKey)?.readings?.lastOrNull() ?: 0f

        fun humidityReadings(userSelectedKey: String): List<Float> =
            getHumidityField(userSelectedKey)?.readings ?: emptyList()

        fun humidityFieldName(userSelectedKey: String): String =
            getHumidityField(userSelectedKey)?.fieldName ?: "Humidity"

        val currentTemperature: Float
            get() = temperatureField?.readings?.lastOrNull() ?: 0f

        val tempFieldName: String
            get() = temperatureField?.fieldName ?: "Temperature"
    }

    private fun isPercentLikeField(fieldName: String): Boolean {
        val lower = fieldName.lowercase()
        return lower.contains("humid") ||
                lower.contains("moist") ||
                lower.contains("soil")
    }

    // Normalize only percent-like fields to keep UI/prediction in 0..100 scale.
    private fun normalizePercentReadings(readings: List<Float>): List<Float> {
        if (readings.isEmpty()) return readings
        val maxReading = readings.maxOrNull() ?: return readings
        if (maxReading <= 100f) return readings

        var divisor = 1f
        var scaledMax = maxReading
        while (scaledMax > 100f) {
            divisor *= 10f
            scaledMax /= 10f
        }
        return readings.map { (it / divisor).coerceIn(0f, 100f) }
    }

    fun fetchSensorHistory(channelId: String): SensorData {
        val url = "https://api.thingspeak.com/channels/$channelId/feeds.json?results=15"

        return try {
            val request = Request.Builder().url(url).build()

            client.newCall(request).execute().use { response ->
                val jsonData = response.body?.string()
                    ?: return SensorData(emptyList(), "No response")

                Log.d("CALYX", "Raw JSON: $jsonData")

                val root        = JSONObject(jsonData)
                val channelMeta = root.getJSONObject("channel")
                val feeds       = root.getJSONArray("feeds")
                val lastSync    = feeds
                    .getJSONObject(feeds.length() - 1)
                    .optString("created_at", "N/A")

                // ── Read field names from channel metadata ─────────
                val fieldNames = mutableMapOf<String, String>()
                for (i in 1..8) {
                    val key  = "field$i"
                    val name = channelMeta.optString(key, "")
                    if (name.isNotBlank()) {
                        fieldNames[key] = name
                        Log.d("CALYX", "Found field: $key = $name")
                    }
                }

                // ── Read readings for each detected field ──────────
                val fieldReadings = mutableMapOf<String, MutableList<Float>>()
                fieldNames.keys.forEach { key ->
                    fieldReadings[key] = mutableListOf()
                }

                for (i in 0 until feeds.length()) {
                    val feed = feeds.getJSONObject(i)
                    fieldNames.keys.forEach { key ->
                        val value = feed.optString(key, "").toFloatOrNull() ?: 0f
                        fieldReadings[key]?.add(value)
                    }
                }

                //Build FieldData list
                val fields = fieldNames.map { (key, name) ->
                    val rawReadings = fieldReadings[key] ?: emptyList()
                    val normalizedReadings = if (isPercentLikeField(name)) {
                        normalizePercentReadings(rawReadings)
                    } else {
                        rawReadings
                    }
                    FieldData(
                        fieldKey  = key,
                        fieldName = name,
                        readings  = normalizedReadings
                    )
                }

                Log.d("CALYX", "Total fields found: ${fields.size}")
                fields.forEach {
                    Log.d("CALYX", "${it.fieldName}: ${it.readings}")
                }

                SensorData(fields, lastSync)
            }

        } catch (e: Exception) {
            Log.e("CALYX", "Fetch error: ${e.message}")
            SensorData(emptyList(), "Error")
        }
    }
}