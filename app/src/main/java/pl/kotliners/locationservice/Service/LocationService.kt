package pl.kotliners.locationservice.Service

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import android.preference.PreferenceManager
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import pl.kotliners.locationservice.Model.LocationModel

/**
 * Created by ejdrian on 17.11.17.
 */

class LocationService : IntentService("LocationBackground") {

    private var locationManager: LocationManager? = null
    private var lastLocation: Location? = null
    private val handler: android.os.Handler = android.os.Handler()
    private lateinit var database: FirebaseDatabase
    private lateinit var myRef: DatabaseReference
    private lateinit var pref: SharedPreferences
    private lateinit var tokenSaved: String

    override fun onCreate() {
        super.onCreate()
        pref = PreferenceManager.getDefaultSharedPreferences(this)
        tokenSaved = pref.all["TOKEN"].toString()
        isServiceRunning = true
        FirebaseApp.initializeApp(baseContext)
        Log.i(TAG, "Service created $tokenSaved")
    }

    override fun onHandleIntent(intent: Intent?) {
        var user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            FirebaseAuth.getInstance().signInWithCustomToken(tokenSaved).addOnCompleteListener {
                user = FirebaseAuth.getInstance().currentUser
            }
        }
        Log.i(TAG, "Service started ${user?.uid}")

        getFirebaseReference(user?.uid)
        runLocationThread()
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        Log.i(TAG, "Service Destroyed: ${!isServiceRunning}")
        when {
            locationManager != null -> for (i in 0 until locationListeners.size) {
                try {
                    locationManager?.removeUpdates(locationListeners[i])
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to remove location listeners")
                }
            }
        }
    }

    private fun runLocationThread() {
        when (locationManager) {
            null -> locationManager = applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        }

        var count = 0
        while (count < 10) {
            handler.post {
                try {
                    Thread.sleep(INTERVAL)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }

                if (locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    try {
                        locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, INTERVAL, DISTANCE, locationListeners[0])
                    } catch (e: SecurityException) {
                        Log.e(TAG, "Fail to request location update", e)
                    } catch (e: IllegalArgumentException) {
                        Log.e(TAG, "GPS provider does not exist", e)
                    } finally {
                        lastLocation = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    }
                } else {
                    try {
                        locationManager?.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, INTERVAL, DISTANCE, locationListeners[1])
                    } catch (e: SecurityException) {
                        Log.e(TAG, "Fail to request location update", e)
                    } catch (e: IllegalArgumentException) {
                        Log.e(TAG, "Network provider does not exist", e)
                    } finally {
                        lastLocation = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    }
                }
                if (lastLocation != null) {
                    myRef.child(lastLocation!!.time.toString()).setValue(LocationModel(lastLocation!!))
                }
            }
            count++
        }
    }

    private fun getFirebaseReference(uid: String?) {
        database = FirebaseDatabase.getInstance()
        myRef = database.getReference("$uid")
    }

    companion object {
        val TAG = "LocationTrackingService"

        val INTERVAL = 5000.toLong() // In milliseconds
        val DISTANCE = 1F // In meters
        var isServiceRunning = false
        val locationListeners = arrayOf(
                LocationTracking(LocationManager.GPS_PROVIDER),
                LocationTracking(LocationManager.NETWORK_PROVIDER)
        )
    }
}