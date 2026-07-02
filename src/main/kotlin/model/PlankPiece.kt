package model

data class PlankPiece(
    val number: Int,
    val x: Double,
    val y: Double,
    val length: Double,
    val width: Double,
    val isWallCut: Boolean = false,
    val isFromCutoff: Boolean = false,
)
