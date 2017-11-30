package pl.kotliners.locationservice

import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Toast
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.iid.FirebaseInstanceId
import kotlinx.android.synthetic.main.activity_main.*
import pl.kotliners.locationservice.Model.LocationModel
import pl.kotliners.locationservice.Service.LocationService
import pl.kotliners.locationservice.Service.StartService


class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private var onAccessLocation = false
    private var databaseReference: DatabaseReference? = null
    private var isMapReady = false

    private lateinit var mAuth: FirebaseAuth
    private lateinit var pref: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var tokenSaved: String
    private lateinit var myIntent: Intent
    private lateinit var pendingIntent:PendingIntent
    private lateinit var alarmService:StartService
    private lateinit var mMap: GoogleMap

    private val points = ArrayList<LatLng>()
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
            startLocation(progressChanged)
        }
    }

    private fun initApp() {
        tvInterval.text = "$progressChanged"
        FirebaseApp.initializeApp(applicationContext)

        if (databaseReference == null) {
            FirebaseDatabase.getInstance()
            databaseReference = FirebaseDatabase.getInstance().reference
            mAuth = FirebaseAuth.getInstance()
        }
        pref = PreferenceManager.getDefaultSharedPreferences(this)
        editor = pref.edit()
        mAuth = FirebaseAuth.getInstance()
        createFirebaseListener()
        myIntent = Intent(this, LocationService::class.java)
        alarmService = StartService()

        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        switchLocation.setOnClickListener {
            if (switchLocation.isChecked) {
                if (!LocationService.isServiceRunning) {
                    startLocation(progressChanged)
                } else {
                    Toast.makeText(this, "Service already running", Toast.LENGTH_LONG).show()
                }
            } else {
                stopLocation()
            }
        }

        timeInterval.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar?, value: Int, p2: Boolean) {
                progressChanged = value
                tvInterval.text = "$progressChanged"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                Log.i("SeekBar", "Started tracing")
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                Toast.makeText(this@MainActivity, "Location tracking time set to: $progressChanged", Toast.LENGTH_LONG).show()
                restartServiceWithInterval(progressChanged)
            }
        })
    }

    private fun createFirebaseListener() {
        val locationListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {

                val toReturn: ArrayList<LocationModel> = ArrayList()

                dataSnapshot.children
                        .map { it.getValue<LocationModel>(LocationModel::class.java) }
                        .mapNotNullTo(toReturn) { messageData -> messageData?.let { it } }

                setupLocationAdapter(toReturn)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.d("Firebase error", databaseError.message)
            }
        }
        databaseReference?.child("${mAuth.currentUser?.uid}")?.addValueEventListener(locationListener)
    }

    private fun setupLocationAdapter(listOfLocation: ArrayList<LocationModel>) {
        points.clear()
        (0 until listOfLocation.size).mapTo(points) { LatLng(listOfLocation[it].latitude.toDouble(), listOfLocation[it].longitude.toDouble()) }
        Log.i("POINTS", points.isNotEmpty().toString())
        if (isMapReady  && points.isNotEmpty()) {
            mMap.clear()

            mMap.addPolyline(PolylineOptions()
                    .addAll(points)
                    .width(6F)
                    .color(Color.BLUE))

            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(points.last().latitude, points.last().longitude), 12F))
        }
    }



    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        isMapReady = true
    }

    private fun restartServiceWithInterval(progressChanged: Int) {
        alarmService.stop(applicationContext)
        alarmService.start(applicationContext, progressChanged)
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

    private fun startLocation(interval: Int) {
        LocationService.isServiceRunning = true
        alarmService.start(applicationContext, interval)
    }

    private fun stopLocation() {
        LocationService.isServiceRunning = false
        alarmService.stop(applicationContext)
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

    companion object {
        var progressChanged = 1
    }
}