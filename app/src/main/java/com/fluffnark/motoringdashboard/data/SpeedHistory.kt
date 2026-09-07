package com.fluffnark.motoringdashboard.data

data class SpeedSample(val elapsedMillis: Long, val mph: Float?)

/** Two minutes of actual observations. Null values create gaps, never invented zeroes. */
class SpeedHistory {
    private val samples = ArrayDeque<SpeedSample>()

    fun record(mph: Float?, elapsedMillis: Long): List<SpeedSample> {
        while (samples.isNotEmpty() && samples.first().elapsedMillis < elapsedMillis - 120_000) {
            samples.removeFirst()
        }
        val sample = SpeedSample(elapsedMillis, mph?.takeIf { it.isFinite() && it >= 0 })
        if (samples.isNotEmpty() && elapsedMillis - samples.last().elapsedMillis < 1_000) {
            // Keep at most one observation per second without shifting the bucket boundary.
            val previous = samples.removeLast()
            samples.addLast(sample.copy(elapsedMillis = previous.elapsedMillis))
        } else {
            samples.addLast(sample)
        }
        while (samples.size > 121) samples.removeFirst()
        return samples.toList()
    }

    fun clear() = samples.clear()
}
