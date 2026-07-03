package ui

import dom.window
import layout.OffsetPattern
import model.RoomParams

object UrlParams {

    private const val RL = "rl"
    private const val RW = "rw"
    private const val PL = "pl"
    private const val PW = "pw"
    private const val TRIM = "trim"
    private const val GAP = "gap"
    private const val OFFSET = "offset"
    private const val SEED = "seed"

    fun load(): SavedForm? {
        val query = window.location.search.removePrefix("?")
        if (query.isBlank()) return null

        val map = parseQuery(query)
        return SavedForm(
            roomLength = map[RL] ?: return null,
            roomWidth = map[RW] ?: return null,
            panelLength = map[PL] ?: return null,
            panelWidth = map[PW] ?: return null,
            minTrim = map[TRIM] ?: return null,
            wallGap = map[GAP] ?: return null,
            offset = map[OFFSET] ?: OffsetPattern.HALF.name,
        )
    }

    fun loadSeed(): Long? =
        parseQuery(window.location.search.removePrefix("?"))[SEED]?.toLongOrNull()

    fun update(params: RoomParams) {
        val query = buildQuery(params)
        val newUrl = "${window.location.pathname}?$query"
        window.asDynamic().history.replaceState(null, "", newUrl)
    }

    private fun buildQuery(params: RoomParams): String {
        val parts = mutableListOf(
            "$RL=${format(params.roomLength)}",
            "$RW=${format(params.roomWidth)}",
            "$PL=${format(params.panelLength)}",
            "$PW=${format(params.panelWidth)}",
            "$TRIM=${format(params.minTrimLength)}",
            "$GAP=${format(params.wallGap)}",
            "$OFFSET=${params.offset.name}",
        )
        if (params.offset == OffsetPattern.RANDOM && params.randomSeed != null) {
            parts.add("$SEED=${params.randomSeed}")
        }
        return parts.joinToString("&")
    }

    private fun format(value: Double): String {
        val rounded = kotlin.math.round(value * 1000.0) / 1000.0
        return if (rounded == kotlin.math.floor(rounded)) {
            rounded.toLong().toString()
        } else {
            rounded.toString()
        }
    }

    private fun parseQuery(query: String): Map<String, String> =
        query.split("&")
            .mapNotNull { part ->
                if (part.isBlank()) return@mapNotNull null
                val eq = part.indexOf('=')
                if (eq < 0) return@mapNotNull null
                val key = decode(part.substring(0, eq))
                val value = decode(part.substring(eq + 1))
                key to value
            }
            .toMap()

    private fun decode(value: String): String =
        js("decodeURIComponent")(value.replace("+", " ")) as String
}
