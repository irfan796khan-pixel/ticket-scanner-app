package com.ticketscanner.app.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Price Initialization Matrix: determines ticket price based on the initial
 * ticket number (quantity when batch is first scanned).
 *
 * Default matrix:
 *   150 -> $1.00 (100 cents)
 *   120 -> $2.00 (200 cents)
 *   100 -> $5.00 (500 cents)
 *    75 -> $10.00 (1000 cents)
 *    50 -> $20.00 (2000 cents)
 *    40 -> $30.00 (3000 cents)
 *    25 -> $50.00 (5000 cents)
 *
 * All prices are stored in cents to avoid floating-point issues.
 */
object PriceMatrix {

    private const val PREFS_NAME = "price_matrix_prefs"
    private const val KEY_USE_NEAREST_MATCH = "use_nearest_match"
    private const val KEY_CUSTOM_PRICES = "custom_prices"

    // Default price matrix: ticketNumber -> priceInCents
    private val DEFAULT_MATRIX: Map<Int, Int> = linkedMapOf(
        150 to 100,
        120 to 200,
        100 to 500,
        75 to 1000,
        50 to 2000,
        40 to 3000,
        25 to 5000
    )

    private var customMatrix: MutableMap<Int, Int> = mutableMapOf()
    private var useNearestMatch: Boolean = true

    /**
     * Initialize price matrix from SharedPreferences.
     */
    fun initialize(context: Context) {
        val prefs = getPrefs(context)
        useNearestMatch = prefs.getBoolean(KEY_USE_NEAREST_MATCH, true)
        loadCustomPrices(prefs)
    }

    /**
     * Get the current price matrix (custom overrides + defaults).
     */
    fun getMatrix(): Map<Int, Int> {
        val merged = DEFAULT_MATRIX.toMutableMap()
        merged.putAll(customMatrix)
        return merged.toSortedMap(compareByDescending { it })
    }

    /**
     * Look up the price for a given ticket number.
     *
     * @param ticketNumber The initial ticket number (quantity in batch)
     * @return The price in cents, or null if no match found and nearest match is disabled
     */
    fun getPrice(ticketNumber: Int): Int? {
        val matrix = getMatrix()

        // Exact match
        matrix[ticketNumber]?.let { return it }

        if (!useNearestMatch) {
            return null // Will prompt user for manual entry
        }

        // Nearest match: find the closest ticket number in the matrix
        return findNearestPrice(ticketNumber, matrix)
    }

    /**
     * Find the price from the nearest matching ticket number in the matrix.
     */
    private fun findNearestPrice(ticketNumber: Int, matrix: Map<Int, Int>): Int? {
        if (matrix.isEmpty()) return null

        var closestKey: Int? = null
        var closestDistance = Int.MAX_VALUE

        for (key in matrix.keys) {
            val distance = kotlin.math.abs(key - ticketNumber)
            if (distance < closestDistance) {
                closestDistance = distance
                closestKey = key
            }
        }

        return closestKey?.let { matrix[it] }
    }

    /**
     * Check if nearest match mode is enabled.
     */
    fun isNearestMatchEnabled(): Boolean = useNearestMatch

    /**
     * Set nearest match mode.
     */
    fun setNearestMatchEnabled(context: Context, enabled: Boolean) {
        useNearestMatch = enabled
        getPrefs(context).edit().putBoolean(KEY_USE_NEAREST_MATCH, enabled).apply()
    }

    /**
     * Add or update a custom price rule.
     */
    fun setCustomPrice(context: Context, ticketNumber: Int, priceInCents: Int) {
        customMatrix[ticketNumber] = priceInCents
        saveCustomPrices(context)
    }

    /**
     * Remove a custom price rule.
     */
    fun removeCustomPrice(context: Context, ticketNumber: Int) {
        customMatrix.remove(ticketNumber)
        saveCustomPrices(context)
    }

    /**
     * Reset to default matrix.
     */
    fun resetToDefaults(context: Context) {
        customMatrix.clear()
        useNearestMatch = true
        val editor = getPrefs(context).edit()
        editor.remove(KEY_CUSTOM_PRICES)
        editor.putBoolean(KEY_USE_NEAREST_MATCH, true)
        editor.apply()
    }

    /**
     * Format price from cents to display string.
     */
    fun formatPrice(cents: Int): String {
        val dollars = cents / 100
        val remainingCents = cents % 100
        return "$${dollars}.${"%02d".format(remainingCents)}"
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun saveCustomPrices(context: Context) {
        val serialized = customMatrix.entries.joinToString(";") { "${it.key}:${it.value}" }
        getPrefs(context).edit().putString(KEY_CUSTOM_PRICES, serialized).apply()
    }

    private fun loadCustomPrices(prefs: SharedPreferences) {
        val serialized = prefs.getString(KEY_CUSTOM_PRICES, "") ?: ""
        customMatrix.clear()
        if (serialized.isNotEmpty()) {
            serialized.split(";").forEach { entry ->
                val parts = entry.split(":")
                if (parts.size == 2) {
                    val key = parts[0].toIntOrNull()
                    val value = parts[1].toIntOrNull()
                    if (key != null && value != null) {
                        customMatrix[key] = value
                    }
                }
            }
        }
    }
}
