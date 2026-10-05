package com.kikepb.squadfy.domain.club

enum class PlayerPosition {
    GOALKEEPER,
    DEFENDER,
    MIDFIELDER,
    FORWARD;

    companion object {
        /** Tolerant parser for legacy free-text positions stored before the enum existed. */
        fun fromRaw(raw: String?): PlayerPosition? {
            val normalized = raw?.trim()?.uppercase() ?: return null
            entries.firstOrNull { it.name == normalized }?.let { return it }
            return when (normalized) {
                "GK", "POR", "PORTERO", "GOALIE" -> GOALKEEPER
                "DF", "DEF", "DEFENSA", "CENTRAL", "LATERAL" -> DEFENDER
                "MF", "MED", "MEDIO", "CENTROCAMPISTA", "MEDIOCENTRO" -> MIDFIELDER
                "FW", "DEL", "DELANTERO", "STRIKER", "EXTREMO" -> FORWARD
                else -> null
            }
        }
    }
}
