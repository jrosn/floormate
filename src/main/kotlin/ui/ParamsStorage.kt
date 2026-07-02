package ui

import dom.window
import layout.OffsetPattern
import model.RoomParams

private const val STORAGE_KEY = "floormate.params"

private external object JSON {
    fun stringify(value: dynamic): String
    fun parse(json: String): dynamic
}

object ParamsStorage {

    fun save(
        roomLength: String,
        roomWidth: String,
        panelLength: String,
        panelWidth: String,
        minTrim: String,
        wallGap: String,
        offset: String,
    ) {
        val data = js("({})")
        data.roomLength = roomLength
        data.roomWidth = roomWidth
        data.panelLength = panelLength
        data.panelWidth = panelWidth
        data.minTrim = minTrim
        data.wallGap = wallGap
        data.offset = offset
        window.localStorage.setItem(STORAGE_KEY, JSON.stringify(data))
    }

    fun save(params: RoomParams) {
        save(
            roomLength = params.roomLength.toString(),
            roomWidth = params.roomWidth.toString(),
            panelLength = params.panelLength.toString(),
            panelWidth = params.panelWidth.toString(),
            minTrim = params.minTrimLength.toString(),
            wallGap = params.wallGap.toString(),
            offset = params.offset.name,
        )
    }

    fun load(): SavedForm? {
        val raw = window.localStorage.getItem(STORAGE_KEY) ?: return null
        return try {
            val data = JSON.parse(raw)
            SavedForm(
                roomLength = data.roomLength?.toString() ?: return null,
                roomWidth = data.roomWidth?.toString() ?: return null,
                panelLength = data.panelLength?.toString() ?: return null,
                panelWidth = data.panelWidth?.toString() ?: return null,
                minTrim = data.minTrim?.toString() ?: return null,
                wallGap = data.wallGap?.toString() ?: return null,
                offset = data.offset?.toString() ?: OffsetPattern.HALF.name,
            )
        } catch (_: Throwable) {
            null
        }
    }
}

data class SavedForm(
    val roomLength: String,
    val roomWidth: String,
    val panelLength: String,
    val panelWidth: String,
    val minTrim: String,
    val wallGap: String,
    val offset: String,
)
