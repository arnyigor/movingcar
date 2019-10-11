package com.arny.movingcar

import android.content.Context
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.roundToInt

 fun Context.getSizeDP(size: Int): Int {
    return (size * this.resources.displayMetrics.density).roundToInt()
}

fun getAngle(from: IntArray, target: IntArray): Double {
    val y2 = target[1]
    val y1 = from[1]
    val x2 = target[0]
    val x1 = from[0]
    val y = (y2 - y1).toDouble()
    val x = (x2 - x1).toDouble()
    val atan2 = atan2(y, x)
    var angle = Math.toDegrees(atan2)
//    if (angle < 0) {
//        angle += 360f
//    }
    angle += ceil(-angle / 360) * 360;
    return angle
}

fun getTimeToRotate(angle: Double, baseValue: Int): Double {
    return (1 / angle) * baseValue
}

fun normalizeAngle(angle: Double): Double {
    var ang = angle % 360
    if (ang < 0)
        ang += 360.0
    return ang
}

fun distance(alpha: Double, beta: Double): Double {
    val a = normalAngle(alpha)
    val b = normalAngle(beta)
    val d = abs(a - b) % 360
    return if (d > 180) 360 - d else d
}

fun minus360(a: Double): Double {
    if (a > 360.0) {
        return minus360(a - 360)
    }
    return a
}

fun plus360(a: Double): Double {
    if (a < -360.0) {
        return plus360(a + 360)
    }
    return a
}



fun getMirrorAngle(angle: Double): Double {
    var a = angle
    a = normalAngle(a)
    if (a < 0) {
        return 360 + a
    }
    return -1 * (360 - a)
}

 fun normalAngle(a: Double): Double {
    var a1 = a
    a1 = minus360(a1)
    a1 = plus360(a1)
    return a1
}