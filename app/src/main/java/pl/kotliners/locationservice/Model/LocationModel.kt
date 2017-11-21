package pl.kotliners.locationservice.Model

import android.location.Location

/**
 * Created by ejdrian on 18.11.17.
 */

class LocationModel{

    constructor() //for a firebase

    lateinit var latitude:String
    lateinit var longitude:String
    lateinit var speed:String
    lateinit var time:String
    lateinit var altitude:String
    lateinit var accuracy:String
    lateinit var bearing:String
    lateinit var provider:String

    constructor(location: Location) {
        latitude = location.latitude.toString()
        longitude = location.longitude.toString()
        speed = location.speed.toString()
        time = location.time.toString()
        altitude = location.altitude.toString()
        accuracy = location.accuracy.toString()
        bearing = location.bearing.toString()
        provider = location.provider.toString()
    }
}