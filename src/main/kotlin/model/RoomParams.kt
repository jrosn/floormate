package model

import layout.OffsetPattern

data class RoomParams(
    val roomLength: Double,
    val roomWidth: Double,
    val panelLength: Double,
    val panelWidth: Double,
    val minTrimLength: Double,
    val wallGap: Double = 10.0,
    val offset: OffsetPattern = OffsetPattern.HALF,
    val randomSeed: Long? = null,
) {
    fun validate(): String? {
        if (roomLength <= 0) return "Длина комнаты должна быть больше 0"
        if (roomWidth <= 0) return "Ширина комнаты должна быть больше 0"
        if (panelLength <= 0) return "Длина панели должна быть больше 0"
        if (panelWidth <= 0) return "Ширина панели должна быть больше 0"
        if (minTrimLength <= 0) return "Минимальная длина обреза должна быть больше 0"
        if (wallGap < 0) return "Отступ от стены не может быть отрицательным"
        if (roomLength <= 2 * wallGap) return "Длина комнаты слишком мала для выбранного отступа от стены"
        if (roomWidth <= 2 * wallGap) return "Ширина комнаты слишком мала для выбранного отступа от стены"
        if (panelLength > roomLength && panelLength > roomWidth) {
            return "Длина панели не должна превышать размеры комнаты"
        }
        return null
    }
}
