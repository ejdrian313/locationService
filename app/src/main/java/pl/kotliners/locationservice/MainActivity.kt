package pl.kotliners.locationservice

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
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.iid.FirebaseInstanceId
import pl.kotliners.locationservice.Service.LocationService

class MainActivity : AppCompatActivity() {

    private var ACCESSLOCATION = 1
    private var onAccessLocation = false
    lateinit var mAuth: FirebaseAuth
    lateinit var pref: SharedPreferences
    lateinit var editor: SharedPreferences.Editor
    lateinit var tokenSaved: String
    lateinit var myIntent: Intent


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        FirebaseApp.initializeApp(applicationContext)
        pref = PreferenceManager.getDefaultSharedPreferences(this)
        editor = pref.edit()
        mAuth = FirebaseAuth.getInstance()
        myIntent = Intent(baseContext, LocationService::class.java)
        checkUserLogIn()
        checkPermission()
    }

    override fun onStart() {
        super.onStart()
        if (onAccessLocation) {
            startLocationService()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            ACCESSLOCATION -> {
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

    fun startLocationService(view: View) {
        if (!LocationService.isServiceRunning) {
            myIntent.putExtra("TOKEN", tokenSaved)
            LocationService.isServiceRunning = true
            startService(myIntent)
        } else {
            Toast.makeText(this, "Service already running", Toast.LENGTH_LONG).show()
        }
    }

    fun stopLocationService(view: View) {
        LocationService.isServiceRunning = false
        stopService(myIntent)
    }

    fun getPattern(view: View) {
        val intent = Intent(this, MapsActivity::class.java)
        startActivity(intent)
    }

    private fun checkUserLogIn() {
        Log.i("TOKEN", "${pref.all["TOKEN"]}")
        Log.i("mAuth", "${mAuth.currentUser}")
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

    private fun startLocationService() {
        LocationService.isServiceRunning = true
        startService(myIntent)
    }

    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (ActivityCompat.
                    checkSelfPermission(this,
                            android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), ACCESSLOCATION)
                return
            }
        }
        onAccessLocation = true
    }
}