package pl.kotliners.locationservice

import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import pl.kotliners.locationservice.Model.LocationModel

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private val points = ArrayList<LatLng>()
    private lateinit var mAuth: FirebaseAuth
    private var databaseReference: DatabaseReference? = null
    private var isMapReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        initFirebase()
        createFirebaseListener()
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        isMapReady = true
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

    private fun initFirebase() {
        FirebaseApp.initializeApp(applicationContext)

        if (databaseReference == null) {
            FirebaseDatabase.getInstance()
            databaseReference = FirebaseDatabase.getInstance().reference
            mAuth = FirebaseAuth.getInstance()
        }
    }
}
