package com.esom.bank.common.utils.gps

import android.app.Activity
import android.content.Context
import android.content.IntentSender.SendIntentException
import android.location.LocationManager
import android.util.Log
import android.widget.Toast
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient


class GpsUtils(private val context: Context) {
    private val TAG = "GpsUtils"
    private val mSettingsClient: SettingsClient = LocationServices.getSettingsClient(context)
    private val mLocationSettingsRequest: LocationSettingsRequest
    private val locationManager: LocationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private var locationRequest: LocationRequest =
        LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10 * 1000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(2 * 1000)
            .setMaxUpdateDelayMillis(10 * 1000)
            .build()

    // method for turn on GPS
    fun turnGPSOn(onGpsListener: OnGpsListener? = null) {
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Log.d(TAG, "turnGPSOn: enabled")
            onGpsListener?.gpsStatus(true)
        } else {
            mSettingsClient
                .checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener((context as Activity)) { //  GPS is already enable, callback GPS status through listener
                    Log.d(TAG, "turnGPSOn: Success")
                    onGpsListener?.gpsStatus(true)
                }
                .addOnFailureListener(
                    context
                ) { e ->
                    when ((e as ApiException).statusCode) {
                        LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                            val rae = e as ResolvableApiException
                            rae.startResolutionForResult(
                                context,
                                GPS_REQUEST
                            )
                        } catch (sie: SendIntentException) {
                            Log.i(TAG, "PendingIntent unable to execute request.")
                        }

                        LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                            val errorMessage = "Enable GPS in settings"
                            Log.e(TAG, errorMessage)
                            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG)
                                .show()
                        }
                    }
                    e.printStackTrace()
                    Log.d(TAG, "turnGPSOn: ${e.message}")
                    onGpsListener?.gpsStatus(false)
                }
        }
    }

    interface OnGpsListener {
        fun gpsStatus(isGPSEnable: Boolean)
    }

    init {
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest).setAlwaysShow(true)

        mLocationSettingsRequest = builder.build()
    }

    companion object {
        const val GPS_REQUEST = 1001
    }
}