package render

import model.LayoutResult
import model.PlankPiece
import dom.document
import dom.window
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import kotlin.math.max
import kotlin.math.min

class FloorRenderer(private val canvas: HTMLCanvasElement) {

    fun render(result: LayoutResult, pixelRatio: Double? = null, forExport: Boolean = false) {
        val (logicalW, logicalH) = if (forExport) {
            Pair(LOGICAL_WIDTH, LOGICAL_HEIGHT)
        } else {
            computeDisplaySize()
        }
        val ratio = pixelRatio ?: if (forExport) EXPORT_PIXEL_RATIO else displayPixelRatio()
        resizeCanvas(logicalW, logicalH, ratio)

        val ctx = canvas.getContext("2d") as CanvasRenderingContext2D
        ctx.setTransform(ratio, 0.0, 0.0, ratio, 0.0, 0.0)
        ctx.imageSmoothingEnabled = true
        ctx.asDynamic().imageSmoothingQuality = "high"

        val margin = 72.0
        val statsHeight = 80.0
        val roomW = result.roomLength
        val roomH = result.roomWidth

        val availW = logicalW - 2 * margin
        val availH = logicalH - 2 * margin - statsHeight
        val scale = min(availW / roomW, availH / roomH)

        val drawW = roomW * scale
        val drawH = roomH * scale
        val offsetX = margin + (availW - drawW) / 2
        val offsetY = margin + (availH - drawH) / 2

        ctx.clearRect(0.0, 0.0, logicalW, logicalH)

        drawRoomOutline(ctx, offsetX, offsetY, drawW, drawH)
        drawWallGap(ctx, offsetX, offsetY, scale, result)
        drawPlanks(ctx, offsetX, offsetY, scale, result)
        drawRoomDimensions(ctx, offsetX, offsetY, drawW, drawH, result)
        drawStats(ctx, offsetX, offsetY + drawH + 20, result)
    }

    private fun computeDisplaySize(): Pair<Double, Double> {
        val wrapper = canvas.parentElement
        if (wrapper == null) {
            return fitAspect(LOGICAL_WIDTH, LOGICAL_HEIGHT)
        }

        val padding = 16.0
        var maxW = wrapper.clientWidth.toDouble() - padding
        var maxH = wrapper.clientHeight.toDouble() - padding

        if (maxH < 120) {
            val top = wrapper.getBoundingClientRect().top
            maxH = window.innerHeight - top - padding
        }
        if (maxW < 120) {
            maxW = wrapper.getBoundingClientRect().width - padding
        }

        return fitAspect(
            maxW.coerceAtLeast(200.0),
            maxH.coerceAtLeast(160.0),
        )
    }

    private fun fitAspect(maxW: Double, maxH: Double): Pair<Double, Double> {
        val aspect = LOGICAL_WIDTH / LOGICAL_HEIGHT
        var w = maxW
        var h = w / aspect
        if (h > maxH) {
            h = maxH
            w = h * aspect
        }
        return Pair(w, h)
    }

    private fun displayPixelRatio(): Double {
        val dpr = window.devicePixelRatio
        return min(max(dpr, 1.0), 3.0)
    }

    private fun resizeCanvas(logicalW: Double, logicalH: Double, pixelRatio: Double) {
        canvas.style.width = "${logicalW.toInt()}px"
        canvas.style.height = "${logicalH.toInt()}px"
        canvas.width = (logicalW * pixelRatio).toInt()
        canvas.height = (logicalH * pixelRatio).toInt()
    }

    private fun drawRoomOutline(
        ctx: CanvasRenderingContext2D,
        x: Double,
        y: Double,
        w: Double,
        h: Double,
    ) {
        ctx.strokeStyle = "#1a1a1a"
        ctx.lineWidth = 3.0
        ctx.strokeRect(x, y, w, h)
    }

    private fun drawWallGap(
        ctx: CanvasRenderingContext2D,
        offsetX: Double,
        offsetY: Double,
        scale: Double,
        result: LayoutResult,
    ) {
        val gap = result.wallGap * scale
        val drawW = result.roomLength * scale
        val drawH = result.roomWidth * scale

        ctx.save()
        ctx.fillStyle = createHatchPattern(ctx, "#e8e8e8", "#d0d0d0")
        ctx.fillRect(offsetX, offsetY, drawW, gap)
        ctx.fillRect(offsetX, offsetY + drawH - gap, drawW, gap)
        ctx.fillRect(offsetX, offsetY, gap, drawH)
        ctx.fillRect(offsetX + drawW - gap, offsetY, gap, drawH)
        ctx.restore()
    }

    private fun createHatchPattern(
        ctx: CanvasRenderingContext2D,
        color1: String,
        color2: String,
    ): dynamic {
        val patternCanvas = document.createElement("canvas") as HTMLCanvasElement
        patternCanvas.width = 8
        patternCanvas.height = 8
        val pCtx = patternCanvas.getContext("2d") as CanvasRenderingContext2D
        pCtx.fillStyle = color1
        pCtx.fillRect(0.0, 0.0, 8.0, 8.0)
        pCtx.strokeStyle = color2
        pCtx.lineWidth = 1.0
        pCtx.beginPath()
        pCtx.moveTo(0.0, 8.0)
        pCtx.lineTo(8.0, 0.0)
        pCtx.stroke()
        return ctx.createPattern(patternCanvas, "repeat")
    }

    private fun drawPlanks(
        ctx: CanvasRenderingContext2D,
        offsetX: Double,
        offsetY: Double,
        scale: Double,
        result: LayoutResult,
    ) {
        val layAlongLength = result.layLength == result.roomLength

        for (piece in result.pieces) {
            val px: Double
            val py: Double
            val pw: Double
            val ph: Double

            if (layAlongLength) {
                px = offsetX + piece.x * scale
                py = offsetY + piece.y * scale
                pw = piece.length * scale
                ph = piece.width * scale
            } else {
                px = offsetX + piece.y * scale
                py = offsetY + piece.x * scale
                pw = piece.width * scale
                ph = piece.length * scale
            }

            ctx.fillStyle = PLANK_FILL
            ctx.fillRect(px, py, pw, ph)
            ctx.strokeStyle = PLANK_STROKE
            ctx.lineWidth = 1.5
            ctx.strokeRect(px, py, pw, ph)

            drawPieceLabel(ctx, piece, px, py, pw, ph, result)
        }
    }

    private fun drawPieceLabel(
        ctx: CanvasRenderingContext2D,
        piece: PlankPiece,
        px: Double,
        py: Double,
        pw: Double,
        ph: Double,
        result: LayoutResult,
    ) {
        if (min(pw, ph) < 12) return

        val cx = px + pw / 2
        val cy = py + ph / 2
        val fontSize = max(7.0, min(pw, ph) * 0.22)
        val numberLine = "№${piece.number}"
        val lengthLabel = displayLengthLabel(piece, result)

        ctx.fillStyle = "#222"
        ctx.font = "${fontSize}px sans-serif"
        ctx.asDynamic().textAlign = "center"
        ctx.asDynamic().textBaseline = "middle"

        if (lengthLabel == null) {
            ctx.fillText(numberLine, cx, cy)
            return
        }

        val inline = "$numberLine / $lengthLabel"
        val inlineWidth = ctx.measureText(inline).width
        val lineHeight = fontSize * 1.15
        val padding = 4.0

        if (inlineWidth <= pw - padding && lineHeight <= ph - padding) {
            ctx.fillText(inline, cx, cy)
        } else if (lineHeight * 2 <= ph - padding) {
            val halfGap = lineHeight * 0.45
            ctx.fillText(numberLine, cx, cy - halfGap)
            ctx.fillText(lengthLabel, cx, cy + halfGap)
        } else {
            ctx.fillText(numberLine, cx, cy)
        }
    }

    private fun displayLengthLabel(piece: PlankPiece, result: LayoutResult): String? {
        val mm = displayLengthMm(piece, result) ?: return null
        return "$mm мм"
    }

    private fun displayLengthMm(piece: PlankPiece, result: LayoutResult): Int? {
        if (!isCutPiece(piece, result)) return null
        val lengthCut = piece.length < result.panelLength - 0.001
        val widthCut = piece.width < result.panelWidth - 0.001
        return when {
            lengthCut -> piece.length.toInt()
            widthCut -> piece.width.toInt()
            else -> null
        }
    }

    private fun isCutPiece(piece: PlankPiece, result: LayoutResult): Boolean =
        piece.length < result.panelLength - 0.001 || piece.width < result.panelWidth - 0.001

    private fun drawRoomDimensions(
        ctx: CanvasRenderingContext2D,
        x: Double,
        y: Double,
        w: Double,
        h: Double,
        result: LayoutResult,
    ) {
        val gap = 14.0
        val tick = 6.0
        ctx.strokeStyle = "#333"
        ctx.fillStyle = "#333"
        ctx.lineWidth = 1.0
        ctx.font = "13px sans-serif"

        val topY = y - gap
        ctx.beginPath()
        ctx.moveTo(x, topY)
        ctx.lineTo(x + w, topY)
        ctx.moveTo(x, topY - tick)
        ctx.lineTo(x, topY + tick)
        ctx.moveTo(x + w, topY - tick)
        ctx.lineTo(x + w, topY + tick)
        ctx.stroke()
        ctx.asDynamic().textAlign = "center"
        ctx.asDynamic().textBaseline = "bottom"
        ctx.fillText("${result.roomLength.toInt()} мм", x + w / 2, topY - 4)

        val leftX = x - gap
        ctx.beginPath()
        ctx.moveTo(leftX, y)
        ctx.lineTo(leftX, y + h)
        ctx.moveTo(leftX - tick, y)
        ctx.lineTo(leftX + tick, y)
        ctx.moveTo(leftX - tick, y + h)
        ctx.lineTo(leftX + tick, y + h)
        ctx.stroke()
        ctx.save()
        ctx.translate(leftX - 6, y + h / 2)
        ctx.rotate(-kotlin.math.PI / 2)
        ctx.asDynamic().textAlign = "center"
        ctx.asDynamic().textBaseline = "bottom"
        ctx.fillText("${result.roomWidth.toInt()} мм", 0.0, 0.0)
        ctx.restore()
    }

    private fun drawStats(
        ctx: CanvasRenderingContext2D,
        x: Double,
        y: Double,
        result: LayoutResult,
    ) {
        val stats = result.stats
        ctx.fillStyle = "#333"
        ctx.font = "13px sans-serif"
        ctx.asDynamic().textAlign = "left"
        ctx.asDynamic().textBaseline = "top"
        val text = "Всего кусков: ${stats.totalPieces}  |  " +
            "Полных панелей: ${stats.fullPanels}  |  " +
            "Из обрезков: ${stats.cutoffs}  |  " +
            "Отходы: ${formatPercent(stats.wastePercent)}%"
        ctx.fillText(text, x, y)
    }

    fun toPngDataUrl(): String = canvas.toDataURL("image/png")

    companion object {
        private const val PLANK_FILL = "#FFF8B8"
        private const val PLANK_STROKE = "#333333"
        private const val LOGICAL_WIDTH = 1200.0
        private const val LOGICAL_HEIGHT = 900.0
        const val EXPORT_PIXEL_RATIO = 4.0
    }

    private fun formatPercent(value: Double): String {
        val rounded = kotlin.math.round(value * 10.0) / 10.0
        return rounded.toString()
    }
}
