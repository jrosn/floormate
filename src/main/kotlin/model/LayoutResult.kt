package model

data class LayoutStats(
    val totalPieces: Int,
    val fullPanels: Int,
    val cutoffs: Int,
    val wastePercent: Double,
)

data class LayoutResult(
    val pieces: List<PlankPiece>,
    val stats: LayoutStats,
    val roomLength: Double,
    val roomWidth: Double,
    val layLength: Double,
    val layDepth: Double,
    val usableLength: Double,
    val usableDepth: Double,
    val wallGap: Double,
    val panelLength: Double,
    val panelWidth: Double,
)
