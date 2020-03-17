package com.example.findmask


import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.*
import android.location.LocationListener
import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.TransitionManager
import android.util.Log
import android.view.*
import android.view.animation.AnticipateInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.app.ActivityCompat
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
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode

/**
 * A simple [Fragment] subclass.
 */
class MapsFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener, GoogleMap.OnMapClickListener, LocationListener {

    private lateinit var binding: FragmentMapsBinding
    private var mMap: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationManager: LocationManager

    private val permission = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    private lateinit var viewModel: MapsViewModel
    private var isFirstIn = true
    private var locationPermissionGranted = false
    private var isAskGpsGranted = false
    private var isAskLocationGranted = false
    private var isClickMarker = false

    private lateinit var constraintSet: ConstraintSet
    private lateinit var transition: ChangeBounds

    companion object {
        private const val PERMISSION_REQUEST_CODE = 0
        private const val GPS_REQUEST_CODE = 1
        private const val AUTOCOMPLETE_EREQUEST_CODE = 2
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
        binding.inputSearch.setOnClickListener { searchPlace() }
        locationManager = context!!.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context!!)
        if(!Places.isInitialized())
            Places.initialize(context!!,getString(R.string.google_maps_key))
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
        return binding.root
    }

    override fun onStop() {
        super.onStop()
        if(locationPermissionGranted && mMap != null) {
            mMap!!.isMyLocationEnabled = false
            locationManager.removeUpdates(this)
        }
    }

    override fun onStart() {
        super.onStart()
        if(mMap != null) {
            if(isAskGpsGranted)
                getLocationPermission()
            else
                getGpsRequest()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        if(mMap == null) {
            mMap = googleMap
            mMap!!.setMaxZoomPreference(18.0f)
            mMap!!.setMinZoomPreference(15.0f)
            mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(22.644684, 120.306005),16.0f))
            mMap!!.uiSettings.isZoomControlsEnabled = true
            mMap!!.setOnMarkerClickListener(this)
            mMap!!.setOnMapClickListener(this)
            val viewModelFactory = MapsViewModelFactory(mMap!!)
            viewModel = ViewModelProvider(this,viewModelFactory).get(MapsViewModel::class.java)
            viewModel.features.observe(this, Observer {
                viewModel.addMarker(it)
                binding.transparentView.visibility = View.GONE
            })
            viewModel.nowFeature.observe(this, Observer {
                it?.let {
                    binding.infoDetail.visibility = View.VISIBLE
                }
            })
            constraintSet = ConstraintSet()
            constraintSet.clone(binding.main)
            transition = ChangeBounds()
            transition.interpolator = AnticipateInterpolator(0.5f)
            transition.duration = 500
            getGpsRequest()
        }
        else {
            binding.transparentView.visibility = View.GONE
            if(isClickMarker) {
                isClickMarker = false
                transitionDetail(false)
            }
        }
        binding.viewModel = viewModel
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu,menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.refresh -> {
                if(isClickMarker)
                    transitionDetail(false)
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
        if(ActivityCompat.checkSelfPermission(context!!,permission[0]) == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true
            mMap!!.isMyLocationEnabled = true
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,5000,5.0f,this)
        }
        else
            if(!isAskLocationGranted) {
                requestPermissions(permission,PERMISSION_REQUEST_CODE)
                isAskLocationGranted = true
            }
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

    private fun getGpsRequest() {
        locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client = LocationServices.getSettingsClient(activity!!)
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            getLocationPermission()
        }
        task.addOnFailureListener { e ->
            if (e is ResolvableApiException) {
                try {
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
            isAskGpsGranted = true
            if(resultCode == Activity.RESULT_CANCELED)
                isFirstIn = false
            else if(resultCode == Activity.RESULT_OK)
                isAskLocationGranted = false
            getLocationPermission()
        }
        else if(requestCode == AUTOCOMPLETE_EREQUEST_CODE) {
            if(resultCode == AutocompleteActivity.RESULT_OK && data != null) {
                val place = Autocomplete.getPlaceFromIntent(data)
                binding.inputSearch.setText(place.name)
                mMap!!.moveCamera(CameraUpdateFactory.newLatLng(place.latLng))
            }
            else if(resultCode == AutocompleteActivity.RESULT_ERROR)
                Toast.makeText(context,"查詢發生了問題",Toast.LENGTH_SHORT).show()
        }
    }

    override fun onMarkerClick(marker: Marker?): Boolean {
        marker?.let {
            val tag = marker.tag
            viewModel.showMarkerInfo(tag as Int)
            if(!isClickMarker)
                transitionDetail(true)
        }
        return false
    }

    override fun onMapClick(p0: LatLng?) {
        if(isClickMarker)
            transitionDetail(true)
    }

    override fun onLocationChanged(location: Location?) {
        Log.i("Test","onLocationChanged (${location?.latitude},${location?.longitude})")
        if(isFirstIn && location != null && mMap != null) {
            val currentLatLng = LatLng(location.latitude,location.longitude)
            mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng,16.0f))
            isFirstIn = false
        }
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        Log.i("Test","onStatusChanged")
    }

    override fun onProviderEnabled(provider: String?) {
        if(locationPermissionGranted)
            mMap!!.isMyLocationEnabled = true
    }

    override fun onProviderDisabled(provider: String?) {
        if(mMap != null)
            mMap!!.isMyLocationEnabled = false
    }

    private fun transitionDetail(animation: Boolean) {
        if(isClickMarker) {
            constraintSet.clear(R.id.info_detail,ConstraintSet.START)
            constraintSet.connect(R.id.info_detail,ConstraintSet.END,ConstraintSet.PARENT_ID,ConstraintSet.START,0)
            constraintSet.setMargin(R.id.info_detail,ConstraintSet.END,1)
        }
        else {
            constraintSet.clear(R.id.info_detail,ConstraintSet.END)
            constraintSet.connect(R.id.info_detail,ConstraintSet.START,ConstraintSet.PARENT_ID,ConstraintSet.START,0)
        }
        if(animation) {
            constraintSet.setVisibility(R.id.transparent_view,View.GONE)
            TransitionManager.beginDelayedTransition(binding.main,transition)
        }
        constraintSet.applyTo(binding.main)
        isClickMarker = !isClickMarker
    }

    private fun searchPlace() {
        val fields = listOf(Place.Field.ADDRESS,Place.Field.LAT_LNG,Place.Field.NAME)
        val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY,fields).setCountry("TW").build(context!!)
        startActivityForResult(intent, AUTOCOMPLETE_EREQUEST_CODE)
    }
}
