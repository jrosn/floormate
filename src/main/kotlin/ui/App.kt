package ui

import dom.document
import dom.window
import layout.LayoutEngine
import layout.OffsetPattern
import model.RoomParams
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLSelectElement
import render.FloorRenderer

class App {
    private val engine = LayoutEngine()
    private var renderer: FloorRenderer? = null
    private var lastResult: model.LayoutResult? = null

    private lateinit var errorDiv: HTMLDivElement
    private lateinit var canvas: HTMLCanvasElement
    private lateinit var roomLengthInput: HTMLInputElement
    private lateinit var roomWidthInput: HTMLInputElement
    private lateinit var panelLengthInput: HTMLInputElement
    private lateinit var panelWidthInput: HTMLInputElement
    private lateinit var minTrimInput: HTMLInputElement
    private lateinit var wallGapInput: HTMLInputElement
    private lateinit var offsetSelect: HTMLSelectElement

    fun mount() {
        errorDiv = document.getElementById("error") as HTMLDivElement
        canvas = document.getElementById("floor-canvas") as HTMLCanvasElement
        roomLengthInput = document.getElementById("room-length") as HTMLInputElement
        roomWidthInput = document.getElementById("room-width") as HTMLInputElement
        panelLengthInput = document.getElementById("panel-length") as HTMLInputElement
        panelWidthInput = document.getElementById("panel-width") as HTMLInputElement
        minTrimInput = document.getElementById("min-trim") as HTMLInputElement
        wallGapInput = document.getElementById("wall-gap") as HTMLInputElement
        offsetSelect = document.getElementById("offset") as HTMLSelectElement

        renderer = FloorRenderer(canvas)
        restoreSavedParams()
        bindAutoSave()

        val calcBtn = document.getElementById("btn-calc") as HTMLButtonElement
        calcBtn.addEventListener("click", { _ -> calculate() })

        val downloadBtn = document.getElementById("btn-download") as HTMLButtonElement
        downloadBtn.addEventListener("click", { _ -> downloadPng() })

        window.addEventListener("resize", { _ ->
            lastResult?.let { renderer?.render(it) }
        })

        calculate()
        window.setTimeout({ lastResult?.let { renderer?.render(it) } }, 0)
    }

    private fun calculate() {
        hideError()
        try {
            val params = readParams()
            val validationError = params.validate()
            if (validationError != null) {
                showError(validationError)
                return
            }
            val result = engine.calculate(params)
            lastResult = result
            ParamsStorage.save(params)
            renderer?.render(result)
        } catch (e: Exception) {
            showError(e.message ?: "Ошибка расчёта")
        }
    }

    private fun readParams(): RoomParams {
        return RoomParams(
            roomLength = roomLengthInput.value.toDoubleOrNull()
                ?: throw IllegalArgumentException("Введите корректную длину комнаты"),
            roomWidth = roomWidthInput.value.toDoubleOrNull()
                ?: throw IllegalArgumentException("Введите корректную ширину комнаты"),
            panelLength = panelLengthInput.value.toDoubleOrNull()
                ?: throw IllegalArgumentException("Введите корректную длину панели"),
            panelWidth = panelWidthInput.value.toDoubleOrNull()
                ?: throw IllegalArgumentException("Введите корректную ширину панели"),
            minTrimLength = minTrimInput.value.toDoubleOrNull()
                ?: throw IllegalArgumentException("Введите корректную минимальную длину обреза"),
            wallGap = wallGapInput.value.toDoubleOrNull()
                ?: throw IllegalArgumentException("Введите корректный отступ от стены"),
            offset = when (offsetSelect.value) {
                "HALF" -> OffsetPattern.HALF
                "THIRD" -> OffsetPattern.THIRD
                "RANDOM" -> OffsetPattern.RANDOM
                else -> OffsetPattern.HALF
            },
        )
    }

    private fun downloadPng() {
        if (lastResult == null) {
            calculate()
        }
        val result = lastResult ?: return
        val r = renderer ?: return
        r.render(result, forExport = true)
        val dataUrl = r.toPngDataUrl()
        r.render(result)
        val link = document.createElement("a") as HTMLAnchorElement
        link.href = dataUrl
        link.download = "floormate-layout.png"
        document.body?.appendChild(link)
        link.click()
        document.body?.removeChild(link)
    }

    private fun showError(message: String) {
        errorDiv.textContent = message
        errorDiv.className = "error visible"
    }

    private fun hideError() {
        errorDiv.textContent = ""
        errorDiv.className = "error"
    }

    private fun restoreSavedParams() {
        val saved = ParamsStorage.load() ?: return
        roomLengthInput.value = saved.roomLength
        roomWidthInput.value = saved.roomWidth
        panelLengthInput.value = saved.panelLength
        panelWidthInput.value = saved.panelWidth
        minTrimInput.value = saved.minTrim
        wallGapInput.value = saved.wallGap
        offsetSelect.value = saved.offset
    }

    private fun bindAutoSave() {
        val save = { _: dynamic -> saveCurrentForm() }
        roomLengthInput.addEventListener("input", save)
        roomWidthInput.addEventListener("input", save)
        panelLengthInput.addEventListener("input", save)
        panelWidthInput.addEventListener("input", save)
        minTrimInput.addEventListener("input", save)
        wallGapInput.addEventListener("input", save)
        offsetSelect.addEventListener("change", save)
    }

    private fun saveCurrentForm() {
        ParamsStorage.save(
            roomLength = roomLengthInput.value,
            roomWidth = roomWidthInput.value,
            panelLength = panelLengthInput.value,
            panelWidth = panelWidthInput.value,
            minTrim = minTrimInput.value,
            wallGap = wallGapInput.value,
            offset = offsetSelect.value,
        )
    }
}
