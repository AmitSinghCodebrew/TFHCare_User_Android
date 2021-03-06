package com.consultantapp.ui.dashboard

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import com.consultantapp.R
import com.consultantapp.data.models.requests.SaveAddress
import com.consultantapp.data.repos.UserRepository
import com.consultantapp.databinding.ActivityMainBinding
import com.consultantapp.ui.LoginViewModel
import com.consultantapp.ui.adapter.CommonFragmentPagerAdapter
import com.consultantapp.ui.dashboard.appointment.AppointmentFragment
import com.consultantapp.ui.dashboard.chat.ChatFragment
import com.consultantapp.ui.drawermenu.DrawerActivity
import com.consultantapp.utils.*
import com.consultantapp.utils.dialogs.ProgressDialog
import com.google.android.gms.location.*
import com.google.android.libraries.places.widget.Autocomplete
import dagger.android.support.DaggerAppCompatActivity
import java.util.*
import javax.inject.Inject


class MainActivity : DaggerAppCompatActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var prefsManager: PrefsManager

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var appSocket: AppSocket

    lateinit var binding: ActivityMainBinding

    private lateinit var progressDialog: ProgressDialog

    private lateinit var viewModel: LoginViewModel

    private lateinit var viewModelDoctor: DoctorViewModel

    private lateinit var adapter: CommonFragmentPagerAdapter


    lateinit var mFusedLocationClient: FusedLocationProviderClient

    private lateinit var geoCoder: Geocoder

    private var currentNavController: LiveData<NavController>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        initialise()
        setNavigation()
        listeners()
    }

    private fun setNavigation() {
        val navGraphIds = listOf(
            R.navigation.navigation_home,
            R.navigation.navigation_appointment,
            R.navigation.navigation_chat,
            R.navigation.navigation_profile)

        // Setup the bottom navigation view with a list of navigation graphs
        val controller = binding.bottomNav.setupWithNavController(
            navGraphIds = navGraphIds,
            fragmentManager = supportFragmentManager,
            containerId = R.id.nav_host_fragment,
            intent = intent,
            activity = this,
            userRepository = userRepository
        )

        currentNavController = controller

        if (intent.hasExtra(EXTRA_TAB)) {
            if (intent.getStringExtra(EXTRA_TAB) == "1") {
                binding.bottomNav.selectedItemId = R.id.navigation_appointment
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return currentNavController?.value?.navigateUp() ?: false
    }

    private fun initialise() {
        appSocket.init()
        viewModel = ViewModelProvider(this, viewModelFactory)[LoginViewModel::class.java]
        viewModelDoctor = ViewModelProvider(this, viewModelFactory)[DoctorViewModel::class.java]
        progressDialog = ProgressDialog(this)

        /*Ask for location*/
        geoCoder = Geocoder(this, Locale.getDefault())
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        getLastLocation()

        /*Update Token*/
        userRepository.pushTokenUpdate()
    }

    private fun listeners() {

    }


    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {

                mFusedLocationClient.lastLocation.addOnCompleteListener(this) { task ->
                    var location: Location? = task.result
                    if (location == null) {
                        requestNewLocationData()
                    } else {
                        getLocationName(location.latitude, location.longitude)
                         /*findViewById<TextView>(R.id.latTextView).text = location.latitude.toString()
                         findViewById<TextView>(R.id.lonTextView).text = location.longitude.toString()*/
                    }
                }
            } else {
                Toast.makeText(this, "Turn on location", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            requestPermissions()
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {
        var mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = 0
        mLocationRequest.fastestInterval = 0
        mLocationRequest.numUpdates = 1

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
            Looper.myLooper()
        )
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            var mLastLocation: Location = locationResult.lastLocation
            getLocationName(mLastLocation.latitude, mLastLocation.longitude)
            /*findViewById<TextView>(R.id.latTextView).text = mLastLocation.latitude.toString()
            findViewById<TextView>(R.id.lonTextView).text = mLastLocation.longitude.toString()*/
        }
    }

    private fun isLocationEnabled(): Boolean {
        var locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private fun checkPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }

    private fun requestPermissions() {
        startActivityForResult(
            Intent(this, DrawerActivity::class.java)
                .putExtra(PAGE_TO_OPEN, DrawerActivity.LOCATION), AppRequestCode.ASK_FOR_LOCATION
        )

    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == AppRequestCode.LOCATION_PERMISSION_ID) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                getLastLocation()
            }
        }
    }

    private fun getLocationName(lat: Double, lng: Double) {
        runOnUiThread {
            try {
                var locationName = ""

                val addresses = geoCoder.getFromLocation(
                    lat, lng, 1
                ) // Here 1 represent max location result to returned, by documents it recommended 1 to 5

                if (addresses.isNotEmpty()) {
                    locationName = when {
                        addresses[0].getAddressLine(1) != null -> addresses[0].getAddressLine(
                            1
                        )
                        addresses[0].featureName == null -> addresses[0].adminArea
                        else -> String.format(
                            "%s, %s",
                            addresses[0].featureName,
                            addresses[0].locality
                        )
                    }
                }

                /*Save Address*/
                val address = SaveAddress()
                address.locationName = locationName
                address.location = ArrayList()
                address.location?.add(lng)
                address.location?.add(lat)

                prefsManager.save(USER_ADDRESS, address)

                // binding.itemMain.tvLocation.text = name
            } catch (e: Exception) {
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        try {
            if (resultCode == Activity.RESULT_OK) {
                when (requestCode) {
                    AppRequestCode.AUTOCOMPLETE_REQUEST_CODE -> {
                        data?.let {
                            val place = Autocomplete.getPlaceFromIntent(it)

                            //binding.itemMain.tvLocation.text = getAddress(place)

                            Log.i("Place===", "Place: " + place.name + ", " + place.id)

                            /*  val address = SaveAddress()
                              address.locationName = getAddress(place)
                              address.location = ArrayList()
                              address.location?.add(place.latLng?.longitude ?: 0.0)
                              address.location?.add(place.latLng?.latitude ?: 0.0)

                              LocaleHelper.setLocale(this, userRepository.getUserLanguage(), prefsManager)*/

                            //performAddressSelectAction(false, address)
                        }
                    }
                    AppRequestCode.ASK_FOR_LOCATION -> {
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
                            AppRequestCode.LOCATION_PERMISSION_ID)
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshAllPages()
    }

    private fun refreshAllPages() {
        try {
            /*Refresh pages*/
            (adapter.fragments[1] as AppointmentFragment).hitApiRefresh()
            (adapter.fragments[2] as ChatFragment).hitApiRefresh()
        } catch (e: Exception) {
        }
    }
}
