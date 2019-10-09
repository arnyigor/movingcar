package com.arny.movingcar

import kotlin.math.atan2

fun getAngle(from: IntArray, target: IntArray): Double {
    val y2 = target[1]
    val y1 = from[1]
    val x2 = target[0]
    val x1 = from[0]
    val y = (y2 - y1).toDouble()
    val x = (x2 - x1).toDouble()
    val atan2 = atan2(y, x)
    var angle = Math.toDegrees(atan2)
    if (angle < 0) {
        angle += 360f
    }
    return angle
}

fun getTimeToRotate(angle: Double, baseValue: Int): Double {
    return (1 / angle) * baseValue
}

fun normalizeAngle(angle: Double): Double {
    var tmp = angle % 180
    if (tmp < 0)
        tmp += 180.0
    return tmp
}