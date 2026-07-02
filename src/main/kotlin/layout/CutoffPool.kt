package layout

class CutoffPool(private val minTrimLength: Double) {
    private val pool = mutableListOf<Double>()

    fun add(length: Double) {
        if (length >= minTrimLength) {
            pool.add(length)
        }
    }

    fun takeSmallestAtLeast(needed: Double): Double? {
        val idx = pool.indices
            .filter { pool[it] >= needed }
            .minByOrNull { pool[it] }
            ?: return null
        return pool.removeAt(idx)
    }

    fun takeSmallest(): Double? {
        if (pool.isEmpty()) return null
        val idx = pool.indices.minByOrNull { pool[it] }!!
        return pool.removeAt(idx)
    }

    fun size(): Int = pool.size
}
