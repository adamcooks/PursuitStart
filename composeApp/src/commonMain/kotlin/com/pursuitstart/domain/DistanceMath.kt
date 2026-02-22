package com.pursuitstart.domain

import kotlin.math.*

object DistanceMath {

    private const val EARTH_RADIUS = 6_371_000.0 // meters

    private fun Double.toRadians(): Double = this * PI / 180.0

    fun distanceMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {

        val dLat = (lat2 - lat1).toRadians()
        val dLon = (lon2 - lon1).toRadians()

        val rLat1 = lat1.toRadians()
        val rLat2 = lat2.toRadians()

        val a = sin(dLat / 2).pow(2) +
                cos(rLat1) * cos(rLat2) *
                sin(dLon / 2).pow(2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return EARTH_RADIUS * c
    }
}