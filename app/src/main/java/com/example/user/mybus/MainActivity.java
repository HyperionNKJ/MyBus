package com.example.user.mybus;

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

import static util.Config.GPS_CHECK_ENABLED;
import static util.Config.LOCATION_PERMISSION_CHECK_ENABLED;
import static util.Config.PLAY_SERVICE_CHECK_ENABLED;
import static util.Constants.ERROR_DIALOG_REQUEST;
import static util.Constants.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();

        /*
          Three checks before app starts. Disable in util/Config
          1) Google Play Services is installed and working
          2) GPS is enabled
          3) Location Permission is granted
         */
        boolean[] checkResults = new boolean[3];
        while (!allCheckPasses(checkResults)) {
            requestForAction(checkResults);
        }
        startMyBus();
    }

    private boolean allCheckPasses(boolean[] checkResults) {
        boolean playServicePasses = !PLAY_SERVICE_CHECK_ENABLED || isPlayServicesOK();
        checkResults[0] = playServicePasses;

        boolean GPSPasses = !GPS_CHECK_ENABLED || isGPSEnabled();
        checkResults[1] = GPSPasses;

        boolean locationPermissionPasses = !LOCATION_PERMISSION_CHECK_ENABLED || isLocationPermitted();
        checkResults[2] = locationPermissionPasses;

        return playServicePasses && GPSPasses && locationPermissionPasses;
    }

    private void requestForAction(boolean[] checkResults) {
        if (!checkResults[0]) {
            requestForPlayService();
        }
        if (!checkResults[1]) {
            requestForGPS();
        }
        if (!checkResults[2]) {
            requestForLocation();
        }
    }

    // Checks for Google Play Services
    private boolean isPlayServicesOK() {
        Log.d(TAG, "isPlayServicesOK: Checking google services ...");
        return googleApiAvailability.isGooglePlayServicesAvailable(MainActivity.this)
                == ConnectionResult.SUCCESS;
    }

    // Request to update / install Google Play Services
    private void requestForPlayService() {
        Log.d(TAG, "requestForPlayService: Google Play Services failed. Checking why ...");
        int availability = googleApiAvailability.isGooglePlayServicesAvailable(MainActivity.this);
        switch (availability) {
            case ConnectionResult.SERVICE_MISSING: case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED: case ConnectionResult.SERVICE_DISABLED:
                Log.d(TAG, "requestForPlayService: Google Play Service outdated. Need to update");
                Dialog dialog = googleApiAvailability.getErrorDialog(MainActivity.this, availability, ERROR_DIALOG_REQUEST);
                dialog.show();
                break;
            default:
                Log.d(TAG, "requestForPlayService: Google Play Services failed for no reason.");
                Toast.makeText(this, "Google Play Services failed", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    // Check if GPS enabled
    private boolean isGPSEnabled(){
        Log.d(TAG, "isGPSEnabled: Checking GPS availability ...");
        final LocationManager manager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    // Request to enable GPS
    private void requestForGPS() {
        Log.d(TAG, "requestForGPS: GPS is disabled. Prompting user to enable ...");
        buildAlertMessageNoGps(); // if GPS disabled, prompt user to enable it
    }

    // Request to enable GPS - build dialog
    private void buildAlertMessageNoGps() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage("This application requires GPS to work properly, do you want to enable it?")
                .setCancelable(false)
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(getApplicationContext(), "Please enable GPS for full functionality of MyBus.", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                })
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    // opens up setting screen to enable GPS
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        Intent enableGpsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(enableGpsIntent); // 2nd arg is requestCode
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    // Check if location permission is granted
    private boolean isLocationPermitted() {
        Log.d(TAG, "isLocationPermitted: Checking location permission ...");
        return ContextCompat.checkSelfPermission(this.getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    // Request for location permission
    private void requestForLocation() {
        Log.d(TAG, "requestForLocation: Location permission not granted. Requesting for permission ...");
        // The result of the permission request is handled by a callback, onRequestPermissionsResult (below).
        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
    }

    private void startMyBus() {
        Log.d(TAG, "startMyBus: Starting MyBus ...");
        Intent intent = new Intent(MainActivity.this, MapsActivity.class);
        startActivity(intent);
    }
}
