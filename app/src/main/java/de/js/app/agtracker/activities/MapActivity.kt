package de.js.app.agtracker.activities

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import de.js.app.agtracker.R
import de.js.app.agtracker.models.TrackedPlaceModel

class MapActivity : AppCompatActivity(), OnMapReadyCallback {
    private var mPlaceDetails: TrackedPlaceModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        if(intent.hasExtra(MainActivity.EXTRA_PLACE_DETAILS)){
            mPlaceDetails = intent.getSerializableExtra(MainActivity.EXTRA_PLACE_DETAILS) as TrackedPlaceModel
        }

        if(mPlaceDetails!=null){
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.title=mPlaceDetails!!.name
            //supportActionBar!!.setHomeButtonEnabled(true)
        }

        val supportMapFragment: SupportMapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        supportMapFragment.getMapAsync(this)


    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            android.R.id.home -> { onBackPressed(); return true }
            else -> return super.onOptionsItemSelected(item)
        }
    }
    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap?) {

        googleMap?.isMyLocationEnabled = true
        googleMap?.uiSettings?.isMyLocationButtonEnabled = true
        googleMap?.uiSettings?.isCompassEnabled = true

        // add postion
        val position = LatLng(mPlaceDetails!!.latitude, mPlaceDetails!!.longitude)
        googleMap!!.addMarker(MarkerOptions().position(position).title(mPlaceDetails!!.name))
        val newLatLngZoom = CameraUpdateFactory.newLatLngZoom(position, 18f)
        googleMap.animateCamera(newLatLngZoom)

    }

}