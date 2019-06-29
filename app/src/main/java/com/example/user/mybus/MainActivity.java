package com.example.user.mybus;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import static util.Constants.ERROR_DIALOG_REQUEST;
import static util.Constants.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION;
import static util.Constants.PERMISSIONS_REQUEST_ENABLE_GPS;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private boolean mLocationPermissionGranted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();

        /*
          Three checks before app starts.
          1) Google Play Services is installed
          2) GPS is enabled
          3) Location Permission is granted
         */
        if (isServicesOK() && isMapsEnabled()) {
            if (mLocationPermissionGranted) {
                startMyBus();
            } else {
                getLocationPermission();
            }
        }
    }

    // Checks for Google Play Services
    public boolean isServicesOK() {
        Log.d(TAG, "isServicesOK: Checking google services ...");
        GoogleApiAvailability instance = GoogleApiAvailability.getInstance();

        int available = instance.isGooglePlayServicesAvailable(MainActivity.this);
        switch (available) {
            case ConnectionResult.SUCCESS:
                Log.d(TAG, "isServicesOK: Google Play Service is up-to-date and can connect");
                return true;
            case ConnectionResult.SERVICE_MISSING: case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED: case ConnectionResult.SERVICE_DISABLED:
                Log.d(TAG, "isServicesOK: Google Play Service outdated. Need to update");
                Dialog dialog = instance.getErrorDialog(MainActivity.this, available, ERROR_DIALOG_REQUEST);
                dialog.show();
                break;
            default:
                Log.d(TAG, "isServicesOK: Google Play Services failed");
                Toast.makeText(this, "Google Play Services failed", Toast.LENGTH_SHORT).show();
                break;
        }
        return false;
    }

    // Check if GPS enabled
    public boolean isMapsEnabled(){
        Log.d(TAG, "isMapsEnabled: Checking GPS availability ...");
        final LocationManager manager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );

        if ( !manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
            Log.d(TAG, "isMapsEnabled: GPS is disabled");
            buildAlertMessageNoGps(); // if GPS disabled, prompt user to enable it
            return false;
        }
        Log.d(TAG, "isMapsEnabled: GPS is enabled");
        return true;
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("This application requires GPS to work properly, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    // opens up setting screen to enable GPS
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        Intent enableGpsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        // SettingActivity will send result back here. Callback "onActivityResult (below)" automatically called.
                        startActivityForResult(enableGpsIntent, PERMISSIONS_REQUEST_ENABLE_GPS); // 2nd arg is requestCode
                    }
                });
        final AlertDialog alert = builder.create();
        Log.d(TAG, "buildAlertMessageNoGps: Request dialog to enable GPS ...");
        alert.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ENABLE_GPS: {
                // Check if location permission granted
                if(mLocationPermissionGranted){
                    Log.d(TAG, "onActivityResult: Location permission granted. Starting app.");
                    startMyBus();
                }
                else{
                    Log.d(TAG, "onActivityResult: Location permission not granted.");
                    getLocationPermission(); // if not granted, request for it
                }
            }
        }
    }

    private void getLocationPermission() {
        // Request location permission, so that we can get the location of the device.

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
            startMyBus();
        } else {
            Log.d(TAG, "getLocationPermission: Getting permission ...");
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            // The result of the permission request is handled by a callback, onRequestPermissionsResult (below).
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
        }
    }

    private void startMyBus() {
        Intent intent = new Intent(MainActivity.this, MapsActivity.class);
        startActivity(intent);
    }
}
