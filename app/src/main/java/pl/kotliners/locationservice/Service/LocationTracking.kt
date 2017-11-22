package pl.kotliners.locationservice.Service

import android.location.Location
import android.os.Bundle

/**
 * Created by ejdrian on 17.11.17.
 */

class LocationTracking(provider: String) : android.location.LocationListener {

    private val lastLocation = Location(provider)

    override fun onLocationChanged(location: Location?) {
        lastLocation.set(location)
    }

    override fun onProviderDisabled(provider: String?) {
    }
    override fun onProviderEnabled(provider: String?) {
    }
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
    }

}