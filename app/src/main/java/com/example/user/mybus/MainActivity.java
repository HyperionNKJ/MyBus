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

import static util.Config.PRECHECK_ENABLED;
import static util.Constants.ERROR_DIALOG_REQUEST;
import static util.Constants.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION;
import static util.Constants.PERMISSIONS_REQUEST_ENABLE_GPS;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private boolean mLocationPermissionGranted = false;
    private boolean firstPermissionCallback = true; // for logD usage

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (PRECHECK_ENABLED) {
            /*
              Three checks before app starts.
              1) Google Play Services is installed
              2) GPS is enabled
              3) Location Permission is granted
            */
            if (isPlayServicesOK() && isMapsEnabled()) {
                if (mLocationPermissionGranted) {
                    Log.d(TAG, "onResume: Location permission granted. Starting app...");
                    startMyBus();
                } else {
                    checkLocationPermission();
                }
            }
            /*
              if isPlayServicesOK / isMapsEnabled is false, no further execution will occur on main thread,
              allowing UI thread (corresponding dialogs) to "block" and wait for user's input.
            */
        } else {
            Log.d(TAG, "onResume: Precheck DISABLED. Starting app straight...");
            startMyBus(); // if config disables precheck, start MyBus straight away.
        }
    }

    // Check if Google Play Services is installed
    public boolean isPlayServicesOK() {
        Log.d(TAG, "isPlayServicesOK: Checking google services...");
        GoogleApiAvailability instance = GoogleApiAvailability.getInstance();
        int available = instance.isGooglePlayServicesAvailable(MainActivity.this);

        switch (available) {
            case ConnectionResult.SUCCESS:
                Log.d(TAG, "isPlayServicesOK: Google Play Service is up-to-date and can connect");
                return true;
            case ConnectionResult.SERVICE_MISSING:
            case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
            case ConnectionResult.SERVICE_DISABLED:
                Log.d(TAG, "isPlayServicesOK: Google Play Service outdated or not installed. Prompting dialog request...");
                Dialog dialog = instance.getErrorDialog(MainActivity.this, available, ERROR_DIALOG_REQUEST);
                dialog.show();
                break;
            default:
                Log.d(TAG, "isPlayServicesOK: Google Play Services failed");
                Toast.makeText(this, "Google Play Services failed", Toast.LENGTH_SHORT).show();
                break;
        }
        return false; // no more code in main thread, allowing UI thread carrying dialog to block
    }

    // Check if GPS is enabled
    public boolean isMapsEnabled(){
        Log.d(TAG, "isMapsEnabled: Checking GPS availability ...");
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Log.d(TAG, "isMapsEnabled: GPS is enabled");
            return true;
        } else {
            Log.d(TAG, "isMapsEnabled: GPS is disabled");
            buildAlertMessageNoGps(); // if GPS disabled, prompt user to enable it
            return false; // no more code in main thread, allowing UI thread carrying dialog to block
        }
    }

    // If GPS is disabled, prompt dialog to enable GPS in settings
    private void buildAlertMessageNoGps() {
        Log.d(TAG, "buildAlertMessageNoGps: Prompting dialog request to enable GPS...");
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("This application requires GPS to work properly, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        Intent enableGpsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(enableGpsIntent, PERMISSIONS_REQUEST_ENABLE_GPS); // 2nd arg is requestCode
                        // Direct user to settings to enable GPS. Callback "onActivityResult (below)" automatically called upon returning.
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        buildAlertMessageNoGps();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    // Callback when user returns from GPS setting page
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: User has returned from GPS setting page");

        // Check if location permission granted
        if (requestCode == PERMISSIONS_REQUEST_ENABLE_GPS) {
            if (mLocationPermissionGranted) {
                Log.d(TAG, "onActivityResult: Location permission granted. Starting app...");
                startMyBus();
            } else {
                checkLocationPermission();
            }
        }
    }

    // Checks if location permission is granted. If not granted, request for it via permission dialog.
    private void checkLocationPermission() {
        Log.d(TAG, "getLocationPermission: Checking if location permission is granted...");

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "getLocationPermission: Location permission granted. Starting app...");
            mLocationPermissionGranted = true;
            startMyBus();
        } else {
            Log.d(TAG, "getLocationPermission: Location permission not granted. Prompting dialog request to obtain location permission...");
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            // Callback "onRequestPermissionsResult (below)" automatically called upon user response.
        }
    }

    // Callback when user responds to permission dialog
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (firstPermissionCallback) {
            // First callback is false alarm, don't show log
            firstPermissionCallback = false;
        } else {
            Log.d(TAG, "onRequestPermissionsResult: User has responded to permission dialog");
        }

        mLocationPermissionGranted = false;
        if (requestCode == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionGranted = true;
            }
        }
    }

    // Start MyBus
    private void startMyBus() {
        Intent intent = new Intent(MainActivity.this, MapsActivity.class);
        startActivity(intent);
    }
}
