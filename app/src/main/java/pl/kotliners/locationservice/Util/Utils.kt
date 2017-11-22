package pl.kotliners.locationservice.Util

/**
 * Created by ejdrian on 22.11.17.
 */
object Utils {
    fun minutesToMiliSeconds(minutes: Int) = (minutes * 1000 * 60).toLong()
    fun minutesToMiliSeconds(minutes: Long) = (minutes * 1000 * 60)
    fun minutesToMiliSeconds(minutes: Double) = (minutes * 1000 * 60).toLong()
}