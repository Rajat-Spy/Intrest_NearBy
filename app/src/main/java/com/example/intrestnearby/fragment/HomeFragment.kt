package com.example.intrestnearby.fragment

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import com.example.intrestnearby.utility.State
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import com.example.intrestnearby.R
import com.example.intrestnearby.adapter.GooglePlaceAdapter
import com.example.intrestnearby.adapter.InfoWindowAdapter
import com.example.intrestnearby.constant.AppConstant
import com.example.intrestnearby.databinding.FragmentHomeBinding
import com.example.intrestnearby.interfaces.NearLocationInterface
import com.example.intrestnearby.model.googlePlaceModel.GooglePlaceModel
import com.example.intrestnearby.model.googlePlaceModel.GoogleResponseModel
import com.example.intrestnearby.viewModels.LocationViewModel
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import kotlin.jvm.internal.Intrinsics
import kotlin.math.ln

class HomeFragment : Fragment(), OnMapReadyCallback, NearLocationInterface, OnMarkerClickListener {

    private lateinit var binding: FragmentHomeBinding
    private var mGoogleMap: GoogleMap? = null
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private var permissionToRequest = mutableListOf<String>()
    private var isLocationPermissionOk = false
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private lateinit var currentLocation: Location
    private var currentMarker: Marker? =null
    private var isTrafficEnable: Boolean = false
    private var radius = 3000
    private val locationViewModel: LocationViewModel by viewModels<LocationViewModel>()
    private lateinit var googlePlaceList: ArrayList<GooglePlaceModel>
    private lateinit var googlePlaceAdapter: GooglePlaceAdapter
    private var infoWindowAdapter: InfoWindowAdapter? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        googlePlaceList = ArrayList()
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            isLocationPermissionOk = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true &&
                    permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true
            if(isLocationPermissionOk)
                setUpGoogleMap()
            else
                Snackbar.make(binding.root, "Location Permission denied", Snackbar.LENGTH_SHORT).show()
        }

        val mapFragment = (childFragmentManager.findFragmentById(R.id.homeMap) as SupportMapFragment?)
        mapFragment?.getMapAsync(this)

        for(placeModel in AppConstant.placesName){
            val chip = Chip(requireContext())
            chip.text = placeModel.name
            chip.id = placeModel.id
            chip.setPadding(8,8,8,8)
            chip.chipBackgroundColor = resources.getColorStateList(R.color.primaryColor, null)
            chip.setTextColor(resources.getColor(R.color.white, null))
            chip.chipIcon = ResourcesCompat.getDrawable(resources, placeModel.drawableId, null)
            chip.isCheckable = true
            chip.isCheckedIconVisible = false
            binding.placesGroup.addView(chip)
        }
        binding.enableTraffic.setOnClickListener {
            if(isTrafficEnable){
                mGoogleMap?.apply {
                    isTrafficEnable = false
                    isTrafficEnabled = false
                }
            }else{
                mGoogleMap?.apply {
                    isTrafficEnable = true
                    isTrafficEnabled = true
                }
            }
        }
        binding.currentLocation.setOnClickListener { getCurrentLocation() }
        binding.btnMapType.setOnClickListener {
            val popupMenu = PopupMenu(requireContext(), it)
            popupMenu.apply {
                menuInflater.inflate(R.menu.menu_type_menu, popupMenu.menu)
                setOnMenuItemClickListener { item ->
                    when(item.itemId) {
                        R.id.btnNormal-> mGoogleMap?.mapType = GoogleMap.MAP_TYPE_NORMAL
                        R.id.btnSatellite-> mGoogleMap?.mapType = GoogleMap.MAP_TYPE_SATELLITE
                        R.id.btnTerrain-> mGoogleMap?.mapType = GoogleMap.MAP_TYPE_TERRAIN
                    }
                    true
                }
                show()
            }
        }
        binding.placesGroup.setOnCheckedChangeListener {_, checkedId->
            if(checkedId != -1){
                val placeModel = AppConstant.placesName[checkedId-1]
                binding.edtPlaceName.setText(placeModel.name)
                getNearbyPlaces(placeModel.placeType)
            }
        }
        setUpRecyclerView()
    }

    private fun getNearbyPlaces(placeType: String) {
        val url = ("https://maps.googleapis.com/maps/api/place/nearbysearch/json?location="
                + currentLocation.latitude + "," + currentLocation.longitude
                + "&radius=" + radius + "&type=" + placeType + "&key=" +
                resources.getString(R.string.API_KEY))
        lifecycleScope.launchWhenCreated {
            locationViewModel.getNearByPlace(url).collect {
                when(it){
                    is State.Success -> {
                        val googleResponseModel: GoogleResponseModel =
                            it.data as GoogleResponseModel
                        if(googleResponseModel.googlePlaceModelList != null &&
                                googleResponseModel.googlePlaceModelList.isNotEmpty()){
                            googlePlaceList.clear()
                            mGoogleMap?.clear()
                            for(i in googleResponseModel.googlePlaceModelList.indices){
                                googlePlaceList.add(googleResponseModel.googlePlaceModelList[i])
                                addMarker(googleResponseModel.googlePlaceModelList[i], i)
                            }
                            googlePlaceAdapter.setGooglePlaces(googlePlaceList)
                        } else {
                            mGoogleMap?.clear()
                            googlePlaceList.clear()
                        }
                    }
                    is State.Failed -> {
                        Snackbar.make(binding.root, it.error, Snackbar.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun addMarker(googlePlaceModel: GooglePlaceModel, position: Int) {
        val markerOptions = MarkerOptions()
            .position(
                LatLng(
                    googlePlaceModel.geometry?.location?.lat!!,
                    googlePlaceModel.geometry.location.lng!!
                )
            )
            .title(googlePlaceModel.name)
            .snippet(googlePlaceModel.vicinity)

        markerOptions.icon(getCustomIcon())
        mGoogleMap?.addMarker(markerOptions)?.tag = position

    }
    private fun getCustomIcon(): BitmapDescriptor {

        val background = ContextCompat.getDrawable(requireContext(), R.drawable.ic_location)
        background?.setTint(resources.getColor(com.google.android.libraries.places.R.color.quantum_googred900, null))
        background?.setBounds(0, 0, background.intrinsicWidth, background.intrinsicHeight)
        val bitmap = Bitmap.createBitmap(
            background?.intrinsicWidth!!, background.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        background.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)

    }

    override fun onMapReady(googleMap: GoogleMap) {
        mGoogleMap = googleMap
        when {
            ActivityCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                isLocationPermissionOk = true
                setUpGoogleMap()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) -> {
                AlertDialog.Builder(requireContext())
                    .setTitle("Location Permission")
                    .setMessage("Near me required location permission to access your location")
                    .setPositiveButton("Ok") { _, _ ->
                        requestLocation()
                    }.create().show()
            }
            else -> {
                requestLocation()
            }
        }
    }
    private fun requestLocation() {
        permissionToRequest.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
        permissionToRequest.add(android.Manifest.permission.ACCESS_COARSE_LOCATION)

        permissionLauncher.launch(permissionToRequest.toTypedArray())
    }
    @SuppressLint("PotentialBehaviorOverride")
    private fun setUpGoogleMap() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            isLocationPermissionOk = false
            return
        }
        mGoogleMap?.isMyLocationEnabled = true
        mGoogleMap?.uiSettings?.isTiltGesturesEnabled = true
        mGoogleMap?.setOnMarkerClickListener(this)

        setUpLocationUpdate()
    }

    private fun setUpLocationUpdate() {

        locationRequest = LocationRequest.create()
        locationRequest.interval = 10000
        locationRequest.fastestInterval = 5000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)

                for (location in locationResult?.locations!!) {
                    Log.d("TAG", "onLocationResult: ${location.longitude} ${location.latitude}")
                }
            }
        }

        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireContext())

        startLocationUpdates()
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            isLocationPermissionOk = false
            return
        }
        fusedLocationProviderClient?.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(requireContext(), "Location update start", Toast.LENGTH_SHORT).show()
            }
        }

        getCurrentLocation()
    }

    private fun getCurrentLocation() {
        val fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireContext())

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            isLocationPermissionOk = false
            return
        }
        fusedLocationProviderClient.lastLocation.addOnSuccessListener {

            currentLocation = it
            infoWindowAdapter = null
            infoWindowAdapter = InfoWindowAdapter(currentLocation, requireContext())
            mGoogleMap?.setInfoWindowAdapter(infoWindowAdapter)
            moveCameraToLocation(currentLocation)
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "$it", Toast.LENGTH_SHORT).show()
        }
    }

    private fun moveCameraToLocation(location: Location) {

        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(
            LatLng(
                location.latitude,
                location.longitude
            ), 17f
        )

        val markerOption = MarkerOptions()
            .position(LatLng(location.latitude, location.longitude))
            .title("Current Location")
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))

        currentMarker?.remove()
        currentMarker = mGoogleMap?.addMarker(markerOption)
        currentMarker?.tag = 703
        mGoogleMap?.animateCamera(cameraUpdate)

    }
    private fun stopLocationUpdates() {
        fusedLocationProviderClient?.removeLocationUpdates(locationCallback)
        Log.d("TAG", "stopLocationUpdates: Location Update Stop")
    }
    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    override fun onResume() {
        super.onResume()
        if (fusedLocationProviderClient != null) {
            startLocationUpdates()
            currentMarker?.remove()
        }
    }
    private fun setUpRecyclerView() {
        val snapHelper: SnapHelper = PagerSnapHelper()
        googlePlaceAdapter = GooglePlaceAdapter(this)

        binding.placesRecyclerView.apply {
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

            setHasFixedSize(false)
            adapter = googlePlaceAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    val linearManager = recyclerView.layoutManager as LinearLayoutManager
                    val position = linearManager.findFirstCompletelyVisibleItemPosition()
                    if (position > -1) {
                        val googlePlaceModel: GooglePlaceModel = googlePlaceList[position]
                        mGoogleMap?.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(
                                    googlePlaceModel.geometry?.location?.lat!!,
                                    googlePlaceModel.geometry.location.lng!!
                                ), 20f
                            )
                        )
                    }
                }
            })
        }
        snapHelper.attachToRecyclerView(binding.placesRecyclerView)
    }

    override fun onSaveClick(googlePlaceModel: GooglePlaceModel) {
        TODO("Not yet implemented")
    }

    override fun onDirectionClick(googlePlaceModel: GooglePlaceModel) {
        val lat = googlePlaceModel.geometry?.location?.lat
        val lng = googlePlaceModel.geometry?.location?.lng
        val intent = Uri.parse("google.navigation:q=$lat,$lng")
        val mapIntent = Intent(Intent.ACTION_VIEW, intent)
        mapIntent.setPackage("com.google.android.apps.maps")
        startActivity(mapIntent)
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        val markerTag = marker.tag as Int
        Log.d("TAG", "onMarkerClick: $markerTag")
        binding.placesRecyclerView.scrollToPosition(markerTag)
        return false
    }
}