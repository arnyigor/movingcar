package com.arny.movingcar

import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        val angle = getAngle(intArrayOf(5, 5), intArrayOf(2, 2))
        println("angle:$angle")
        assert(angle == 90.0)
    }
    @Test
    fun less_time_from_more_angle() {
        val angle = 10.0
        val time = getTimeToRotate(angle, 2000)
        println("tmp:$time")
        assert(time==-100.0)
    }
    @Test
    fun normal_angle() {
        val angle = normalizeAngle(-360.0-45.0)
        println("tmp:$angle")
        assert(angle==-100.0)
    }
    @Test
    fun mirror_angle() {
        val angle = getMirrorAngle(135.0)
        println("mirror_angle:$angle")
        assert(angle==-100.0)
    }
    @Test
    fun less_angle() {
        val current = 5.0
        val angle = -5.0
        val distance = distance(current, angle)
        println("tmp:$distance")
        assert(distance==-100000.0)
    }
}
