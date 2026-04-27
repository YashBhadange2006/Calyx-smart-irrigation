package com.example.calyx

object Constants {
    const val CHANNEL_ID            = "3246420"
    const val POLLING_INTERVAL_MS   = 20000L
    const val LOGGING_INTERVAL_MINS = 0.3f   // ~18 seconds between readings
    const val DRY_THRESHOLD         = 40f    // humidity % below this = dry
    const val HIGH_TEMP_THRESHOLD   = 35f    // °C
    const val FROST_THRESHOLD       = 5f     // °C
}