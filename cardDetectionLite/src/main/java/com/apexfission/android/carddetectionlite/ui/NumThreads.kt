package com.apexfission.android.carddetectionlite.ui

sealed class NumThreads {
    object Default : NumThreads()
    object Quarter : NumThreads()
    object Half : NumThreads()
    object ThreeQuarters : NumThreads()
    data class CustomPercentage(val percentage: Float) : NumThreads()
    data class CustomCount(val count: Int) : NumThreads()

    fun toInt(): Int {
        val availableProcessors = Runtime.getRuntime().availableProcessors()
        return when (this) {
            Default -> 3.coerceAtMost(availableProcessors)
            Quarter -> (availableProcessors * 0.25f).toInt().coerceAtLeast(1)
            Half -> (availableProcessors * 0.5f).toInt().coerceAtLeast(1)
            ThreeQuarters -> (availableProcessors * 0.75f).toInt().coerceAtLeast(1)
            is CustomPercentage -> (availableProcessors * percentage).toInt().coerceAtLeast(1)
            is CustomCount -> count.coerceAtLeast(1)
        }
    }

    override fun toString(): String {
        return when (this) {
            Default -> "Default"
            Quarter -> "25%"
            Half -> "50%"
            ThreeQuarters -> "75%"
            is CustomPercentage -> "Custom (${(percentage * 100).toInt()}%)"
            is CustomCount -> "Custom (${count})"
        }
    }
}
