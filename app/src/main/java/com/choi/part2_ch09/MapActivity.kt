package com.choi.part2_ch09

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.choi.part2_ch09.databinding.ActivityMapBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.play.integrity.internal.m
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMapBinding
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val markerMap = hashMapOf<String, Marker>()

    @RequiresApi(Build.VERSION_CODES.N)
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                // fine Location 권한이 있음
                getCurrentLocation()
            }

            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                // Coarse Location 권한이 있음
                getCurrentLocation()
            }

            else -> {
                // TODO 교육용 팝업 띄워 권한 다시 받기
            }

        }

    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            // 새로 요청된 위치 정보
            for (location in locationResult.locations) {
                Log.e(
                    "MapActivity",
                    "latitude : ${location.latitude}, longitude : ${location.longitude}"
                )

                // Firebase에 내 위치 업로드
                val uid = Firebase.auth.currentUser?.uid.orEmpty()

                val locationMap = mutableMapOf<String, Any>()
                locationMap["latitude"] = location.latitude
                locationMap["longitude"] = location.longitude


                Firebase.database.reference
                    .child("Person")
                    .child(uid)
                    .updateChildren(locationMap)


                // 지도에 마커 찍어 움직이기
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        requestLocationPermission()
        setUpFirebaseDatabase()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onResume() {
        super.onResume()
        getCurrentLocation()
    }

    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun getCurrentLocation() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5 * 1000)
            .build()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationPermission()
            return
        }

        // 권한이 있는 상태
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        fusedLocationClient.lastLocation.addOnSuccessListener {
            mMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(it.latitude, it.longitude), 16.0f
                )
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun requestLocationPermission() {
        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun setUpFirebaseDatabase() {
        Firebase.database.reference.child("Person")
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val person = snapshot.getValue(Person::class.java) ?: return
                    val uid = person.uid ?: return

                    if (markerMap[uid] == null) {
                        markerMap[uid] = makeNewMarker(person,uid) ?: return
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    val person = snapshot.getValue(Person::class.java) ?: return
                    val uid = person.uid ?: return



                    if (markerMap[uid] == null) {
                        markerMap[uid] = makeNewMarker(person,uid) ?: return
                    } else {
                        markerMap[uid]?.position =
                            LatLng(person.latitude ?: 0.0, person.latitude ?: 0.0)
                    }
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {

                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {}

            })
    }

    private fun makeNewMarker(person: Person, uid:String) : Marker? {
        val marker = mMap.addMarker(
            MarkerOptions().position(
                LatLng(
                    person.latitude ?: 0.0,
                    person.longitude ?: 0.0
                )
            ).title(person.name.orEmpty())
        ) ?: return null

        return marker
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mMap.setMaxZoomPreference(20.0f)
        mMap.setMinZoomPreference(10.0f)

    }
}