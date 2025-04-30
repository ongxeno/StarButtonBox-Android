package com.ongxeno.android.starbuttonbox.data

import kotlinx.serialization.Serializable

/**
 * Represents a game for which macros can be defined.
 *
 * @property id A unique identifier for the game (e.g., "star_citizen_4_1_1").
 * @property name The display name of the game (e.g., "Star Citizen 4.1.1").
 */
@Serializable
data class Game(
    val id: String,
    val name: String
) {
    companion object {
        // Define the specific Star Citizen game instance
        val STAR_CITIZEN_4_1_1 = Game(id = "star_citizen_4_1_1", name = "Star Citizen 4.1.1")

        // Add other games here if needed in the future
        // val ELITE_DANGEROUS = Game(id = "elite_dangerous", name = "Elite Dangerous")

        // Function to get a game by its ID (optional, but can be useful)
        fun getById(id: String): Game? {
            return when (id) {
                STAR_CITIZEN_4_1_1.id -> STAR_CITIZEN_4_1_1
                // Add cases for other games
                else -> null
            }
        }
    }
}
