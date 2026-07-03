package layout

import model.LayoutResult
import model.LayoutStats
import model.PlankPiece
import model.RoomParams
import kotlin.math.floor
import kotlin.random.Random

class LayoutEngine {

    fun calculate(params: RoomParams): LayoutResult {
        val error = params.validate()
        require(error == null) { error!! }

        val layAlongLength = params.roomLength >= params.roomWidth
        val layLength = if (layAlongLength) params.roomLength else params.roomWidth
        val layDepth = if (layAlongLength) params.roomWidth else params.roomLength

        val usableLength = layLength - 2 * params.wallGap
        val usableDepth = layDepth - 2 * params.wallGap
        require(usableLength > 0 && usableDepth > 0) {
            "Рабочая зона комнаты слишком мала"
        }

        val plankLen = params.panelLength
        val plankWidth = params.panelWidth
        require(plankLen > 0 && plankWidth > 0)

        val pool = CutoffPool(params.minTrimLength)
        val pieces = mutableListOf<PlankPiece>()
        var nextNumber = 1
        var planksConsumed = 0
        var cutoffCount = 0
        var totalUsedArea = 0.0

        val fullRows = floor(usableDepth / plankWidth).toInt()
        val lastRowDepth = usableDepth - fullRows * plankWidth
        val rowCount = fullRows + if (lastRowDepth > 0.001) 1 else 0

        val random = params.randomSeed?.let { Random(it) }

        for (row in 0 until rowCount) {
            val rowDepth = if (row == rowCount - 1 && lastRowDepth > 0.001) lastRowDepth else plankWidth
            val rowY = params.wallGap + row * plankWidth
            val offset = rowOffset(params.offset, row, plankLen, pool, random)

            val rowPieces = layoutRow(
                rowIndex = row,
                rowY = rowY,
                rowDepth = rowDepth,
                offset = offset,
                usableLength = usableLength,
                plankLen = plankLen,
                wallGap = params.wallGap,
                pool = pool,
                startNumber = nextNumber,
            )

            for (piece in rowPieces.pieces) {
                pieces.add(piece)
                totalUsedArea += piece.length * piece.width
            }
            nextNumber = rowPieces.nextNumber
            planksConsumed += rowPieces.planksConsumed
            cutoffCount += rowPieces.cutoffUses
        }

        val totalPlankArea = planksConsumed * plankLen * plankWidth
        val wastePercent = if (totalPlankArea > 0) {
            ((totalPlankArea - totalUsedArea) / totalPlankArea) * 100.0
        } else {
            0.0
        }

        val fullPanels = pieces.count { piece ->
            !piece.isWallCut &&
                kotlin.math.abs(piece.length - plankLen) < 0.001 &&
                kotlin.math.abs(piece.width - plankWidth) < 0.001 &&
                !piece.isFromCutoff
        }

        return LayoutResult(
            pieces = pieces,
            stats = LayoutStats(
                totalPieces = pieces.size,
                fullPanels = fullPanels,
                cutoffs = cutoffCount,
                wastePercent = wastePercent,
            ),
            roomLength = params.roomLength,
            roomWidth = params.roomWidth,
            layLength = layLength,
            layDepth = layDepth,
            usableLength = usableLength,
            usableDepth = usableDepth,
            wallGap = params.wallGap,
            panelLength = plankLen,
            panelWidth = plankWidth,
        )
    }

    private fun rowOffset(
        pattern: OffsetPattern,
        rowIndex: Int,
        plankLen: Double,
        pool: CutoffPool,
        random: Random?,
    ): Double = when (pattern) {
        OffsetPattern.HALF -> if (rowIndex % 2 == 1) plankLen / 2.0 else 0.0
        OffsetPattern.THIRD -> when (rowIndex % 3) {
            1 -> plankLen / 3.0
            2 -> 2.0 * plankLen / 3.0
            else -> 0.0
        }
        OffsetPattern.RANDOM -> {
            val candidate = pool.takeSmallest()
            if (candidate != null) {
                candidate.coerceAtMost(plankLen - 1.0)
            } else {
                val maxOffset = plankLen - 1.0
                if (maxOffset <= 0) 0.0 else (random ?: Random.Default).nextDouble(0.0, maxOffset)
            }
        }
    }

    private data class RowLayoutResult(
        val pieces: List<PlankPiece>,
        val nextNumber: Int,
        val fullPanels: Int,
        val cutoffUses: Int,
        val planksConsumed: Int,
    )

    private fun layoutRow(
        rowIndex: Int,
        rowY: Double,
        rowDepth: Double,
        offset: Double,
        usableLength: Double,
        plankLen: Double,
        wallGap: Double,
        pool: CutoffPool,
        startNumber: Int,
    ): RowLayoutResult {
        val pieces = mutableListOf<PlankPiece>()
        var number = startNumber
        var pos = 0.0
        var fullPanels = 0
        var cutoffUses = 0
        var planksConsumed = 0
        var isFirst = true

        if (offset > 0.001) {
            val result = placePiece(
                neededLength = offset,
                x = wallGap,
                y = rowY,
                width = rowDepth,
                plankLen = plankLen,
                pool = pool,
                isWallCut = true,
                isFirstInRow = true,
                number = number,
            )
            pieces.add(result.piece)
            number++
            pos += offset
            fullPanels += result.fullPanels
            cutoffUses += result.cutoffUses
            planksConsumed += result.planksConsumed
            isFirst = false
        }

        while (pos + plankLen < usableLength - 0.001) {
            val result = placePiece(
                neededLength = plankLen,
                x = wallGap + pos,
                y = rowY,
                width = rowDepth,
                plankLen = plankLen,
                pool = pool,
                isWallCut = isFirst,
                isFirstInRow = isFirst,
                number = number,
            )
            pieces.add(result.piece)
            number++
            pos += plankLen
            fullPanels += result.fullPanels
            cutoffUses += result.cutoffUses
            planksConsumed += result.planksConsumed
            isFirst = false
        }

        val lastLen = usableLength - pos
        if (lastLen > 0.001) {
            val result = placePiece(
                neededLength = lastLen,
                x = wallGap + pos,
                y = rowY,
                width = rowDepth,
                plankLen = plankLen,
                pool = pool,
                isWallCut = true,
                isFirstInRow = isFirst,
                number = number,
            )
            pieces.add(result.piece)
            number++
            fullPanels += result.fullPanels
            cutoffUses += result.cutoffUses
            planksConsumed += result.planksConsumed
        }

        return RowLayoutResult(pieces, number, fullPanels, cutoffUses, planksConsumed)
    }

    private data class PlaceResult(
        val piece: PlankPiece,
        val fullPanels: Int,
        val cutoffUses: Int,
        val planksConsumed: Int,
    )

    private fun placePiece(
        neededLength: Double,
        x: Double,
        y: Double,
        width: Double,
        plankLen: Double,
        pool: CutoffPool,
        isWallCut: Boolean,
        isFirstInRow: Boolean,
        number: Int,
    ): PlaceResult {
        val cutoff = pool.takeSmallestAtLeast(neededLength)
        if (cutoff != null) {
            val remainder = cutoff - neededLength
            pool.add(remainder)
            return PlaceResult(
                piece = PlankPiece(
                    number = number,
                    x = x,
                    y = y,
                    length = neededLength,
                    width = width,
                    isWallCut = isWallCut,
                    isFromCutoff = true,
                ),
                fullPanels = 0,
                cutoffUses = 1,
                planksConsumed = 0,
            )
        }

        val remainder = plankLen - neededLength
        pool.add(remainder)
        val isFull = kotlin.math.abs(neededLength - plankLen) < 0.001 && !isWallCut
        return PlaceResult(
            piece = PlankPiece(
                number = number,
                x = x,
                y = y,
                length = neededLength,
                width = width,
                isWallCut = isWallCut,
                isFromCutoff = false,
            ),
            fullPanels = if (isFull) 1 else 0,
            cutoffUses = 0,
            planksConsumed = 1,
        )
    }
}
