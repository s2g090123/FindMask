package com.example.findmask


import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.GnssStatus
import android.location.GpsStatus
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import com.example.findmask.databinding.FragmentMapsBinding
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

/**
 * A simple [Fragment] subclass.
 */
class MapsFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var binding: FragmentMapsBinding
    private var mMap: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private var locationCallback: LocationCallback? = null


    private lateinit var viewModel: MapsViewModel
    private var locationPermissionGranted = false
    private var locationUpdatedGranted = false

    companion object {
        private const val PERMISSION_REQUEST_CODE = 0
        private const val GPS_REQUEST_CODE = 1
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_maps,container,false)
        binding.lifecycleOwner = this
        setHasOptionsMenu(true)
        (activity as AppCompatActivity).setSupportActionBar(binding.toolbar)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context!!)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
        return binding.root
    }

    override fun onStop() {
        super.onStop()
        if(locationCallback != null)
            fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onStart() {
        super.onStart()
        if(locationPermissionGranted)
            createLocationRequest()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        if(mMap == null) {
            mMap = googleMap
            mMap!!.setMaxZoomPreference(18.0f)
            mMap!!.setMinZoomPreference(15.0f)
            mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(22.644684, 120.306005),16.0f))
            mMap!!.uiSettings.isZoomControlsEnabled = true
            mMap!!.setOnMarkerClickListener(this)
            val viewModelFactory = MapsViewModelFactory(mMap!!)
            viewModel = ViewModelProvider(this,viewModelFactory).get(MapsViewModel::class.java)
            viewModel.features.observe(this, Observer {
                viewModel.addMarker(it)
                binding.transparentView.visibility = View.GONE
            })
            createLocationRequest()
        }
        else
            binding.transparentView.visibility = View.GONE
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu,menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.refresh -> {
                binding.transparentView.visibility = View.VISIBLE
                mMap!!.clear()
                viewModel.getData()
            }
            R.id.about -> {
                NavHostFragment.findNavController(this).navigate(MapsFragmentDirections.actionMapsFragmentToInfoFragment())
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun getLocationPermission() {
        val permission = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if(ActivityCompat.checkSelfPermission(context!!,permission[0]) == PackageManager.PERMISSION_GRANTED) {
            updateLocation()
            locationPermissionGranted = true
        }
        else
            requestPermissions(permission,PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        locationPermissionGranted = false
        when(requestCode) {
            PERMISSION_REQUEST_CODE ->
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    getLocationPermission()
        }
    }

    private fun updateLocation() {
        locationCallback = object: LocationCallback() {
            override fun onLocationResult(location: LocationResult?) {
                if(location == null)
                    Log.i("Test","Location is null")
                else {
                    if(!locationUpdatedGranted) {
                        mMap!!.isMyLocationEnabled = true
                        val position = location.lastLocation
                        val currentLatLng = LatLng(position.latitude,position.longitude)
                        mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng,16.0f))
                        locationUpdatedGranted = true
                    }
                }
            }
        }
        fusedLocationClient.requestLocationUpdates(locationRequest,locationCallback,null)
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest()
        locationRequest.interval = 10000
        locationRequest.fastestInterval = 5000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
        val client = LocationServices.getSettingsClient(activity!!)
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            getLocationPermission()
        }
        task.addOnFailureListener { e ->
            if (e is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    locationUpdatedGranted = false
                    e.startResolutionForResult(activity,GPS_REQUEST_CODE)
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GPS_REQUEST_CODE) {
            getLocationPermission()
        }
    }

    override fun onMarkerClick(marker: Marker?): Boolean {
        marker?.let {
            val tag = marker.tag
            Log.i("Test",tag.toString())
        }
        return false
    }
}
