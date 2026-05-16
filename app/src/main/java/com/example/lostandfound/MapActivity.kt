package com.example.lostandfound
import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.lostandfound.databinding.ActivityMapBinding
import com.example.lostandfound.db.DatabaseHelper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
class MapActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityMapBinding
    private lateinit var googleMap: GoogleMap
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: Location? = null
    private var radiusKm: Int = 10
    private var isRadiusFilterActive = false
    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 3001
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.title = getString(R.string.title_map)
        dbHelper = DatabaseHelper(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        setupRadiusFilter()
        requestLocationPermission()
    }
    private fun setupRadiusFilter() {
        binding.seekRadius.max = 49
        binding.seekRadius.progress = 9
        updateRadiusLabel(10)
        binding.seekRadius.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                radiusKm = progress + 1
                updateRadiusLabel(radiusKm)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
        binding.btnApplyRadius.setOnClickListener {
            val loc = currentLocation
            if (loc == null) {
                fetchCurrentLocationThenFilter()
            } else {
                isRadiusFilterActive = true
                if (::googleMap.isInitialized) refreshMarkers()
            }
        }
        binding.btnClearRadius.setOnClickListener {
            isRadiusFilterActive = false
            if (::googleMap.isInitialized) refreshMarkers()
        }
    }
    private fun updateRadiusLabel(km: Int) {
        binding.txtRadiusLabel.text = getString(R.string.radius_label, km)
    }
    private fun requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                REQUEST_LOCATION_PERMISSION
            )
        } else {
            fetchCurrentLocation()
        }
    }
    private fun fetchCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                currentLocation = location
                if (::googleMap.isInitialized) {
                    googleMap.isMyLocationEnabled = true
                }
            }
        }
    }
    private fun fetchCurrentLocationThenFilter() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, getString(R.string.location_permission_needed), Toast.LENGTH_SHORT).show()
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                currentLocation = location
                isRadiusFilterActive = true
                refreshMarkers()
            } else {
                Toast.makeText(this, getString(R.string.location_unavailable), Toast.LENGTH_SHORT).show()
            }
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION &&
            grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            fetchCurrentLocation()
        }
    }
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.isZoomControlsEnabled = true
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap.isMyLocationEnabled = true
            googleMap.uiSettings.isMyLocationButtonEnabled = true
        }
        refreshMarkers()
    }
    private fun refreshMarkers() {
        googleMap.clear()
        val items = dbHelper.getAllItems()
        val boundsBuilder = LatLngBounds.Builder()
        var markerCount = 0
        var skippedCount = 0
        for (item in items) {
            val lat = item.latitude
            val lng = item.longitude
            if (lat == null || lng == null) {
                skippedCount++
                continue
            }
            if (isRadiusFilterActive) {
                val loc = currentLocation ?: continue
                val results = FloatArray(1)
                Location.distanceBetween(loc.latitude, loc.longitude, lat!!, lng!!, results)
                val distanceKm = results[0] / 1000
                if (distanceKm > radiusKm) continue
            }
            val position = LatLng(lat, lng)
            val hue = if (item.postType == "Lost")
                BitmapDescriptorFactory.HUE_RED
            else
                BitmapDescriptorFactory.HUE_AZURE
            googleMap.addMarker(
                MarkerOptions()
                    .position(position)
                    .title("[${item.postType}] ${item.name}")
                    .snippet("${item.location}  •  ${item.date}")
                    .icon(BitmapDescriptorFactory.defaultMarker(hue))
            )
            boundsBuilder.include(position)
            markerCount++
        }
        when {
            markerCount == 0 && isRadiusFilterActive ->
                Toast.makeText(this, getString(R.string.no_items_in_radius, radiusKm), Toast.LENGTH_SHORT).show()
            markerCount == 0 ->
                Toast.makeText(this, getString(R.string.no_items_on_map), Toast.LENGTH_SHORT).show()
            else -> {
                try {
                    val bounds = boundsBuilder.build()
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150))
                } catch (_: Exception) { }
            }
        }
    }
}
