package pl.kotliners.locationservice

import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.iid.FirebaseInstanceId
import kotlinx.android.synthetic.main.activity_main.*
import pl.kotliners.locationservice.Service.LocationService
import pl.kotliners.locationservice.Service.StartService

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private var onAccessLocation = false
    private lateinit var mAuth: FirebaseAuth
    private lateinit var pref: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var tokenSaved: String
    private lateinit var myIntent: Intent
    private lateinit var pendingIntent:PendingIntent
    private lateinit var alarmService:StartService

    private val accessLocation = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        FirebaseApp.initializeApp(applicationContext)
        initApp()
        checkUserLogIn()
        checkPermission()
        alarmSet()
    }

    override fun onStart() {
        super.onStart()
        if (onAccessLocation) {
            startLocation()
        }
    }

    fun buChangeSheduledTime(view: View) {
        alarmService.stop(applicationContext)
        var interval = getMinutes()
        alarmService.start(applicationContext, interval)
    }

    fun buStartLocation(view: View) {
        if (!LocationService.isServiceRunning) {
            startLocation()
        } else {
            Toast.makeText(this, "Service already running", Toast.LENGTH_LONG).show()
        }
    }

    fun buStopLocation(view: View) {
        LocationService.isServiceRunning = false
        alarmService.stop(applicationContext)
    }

    private fun initApp() {
        pref = PreferenceManager.getDefaultSharedPreferences(this)
        editor = pref.edit()
        mAuth = FirebaseAuth.getInstance()
        myIntent = Intent(this, LocationService::class.java)
        alarmService = StartService()
    }

    private fun checkUserLogIn() {
        Log.i("TOKEN", "${pref.all["TOKEN"]}")
        Log.i("mAuthUID", "${mAuth.currentUser?.uid}")

        tokenSaved = pref.all["TOKEN"].toString()
        if (tokenSaved == "null") {
            mAuth.signInAnonymously().addOnCompleteListener(this) { task ->
                when {
                    task.isSuccessful -> {
                        val token = FirebaseInstanceId.getInstance().token
                        tokenSaved = token!!
                        editor.putString("TOKEN", token)
                        editor.commit()
                        Toast.makeText(this@MainActivity, "Log to firebase",
                                Toast.LENGTH_SHORT).show()
                    }
                    else -> Toast.makeText(this@MainActivity, "Authentication failed.",
                            Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            mAuth.signInWithCustomToken(tokenSaved)
        }
    }

    private fun startLocation() {
        LocationService.isServiceRunning = true
        alarmService.start(applicationContext, ten_minutes)
    }

    private fun alarmSet() {
        pendingIntent = PendingIntent.getBroadcast(this, 0, myIntent, 0)
    }

    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (ActivityCompat.
                    checkSelfPermission(this,
                            android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), accessLocation)
                return
            }
        }
        onAccessLocation = true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            accessLocation -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onAccessLocation = true
                } else {
                    onAccessLocation = false
                    Toast.makeText(this, "We cannot access to your location", Toast.LENGTH_LONG).show()
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}