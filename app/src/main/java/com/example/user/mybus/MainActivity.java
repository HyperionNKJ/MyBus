package com.example.user.mybus;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int ERROR_DIALOG_REQUEST = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (isServicesOK()) {
            startMyBus();
        }
    }

    private void startMyBus() {
        Intent intent = new Intent(MainActivity.this, MapsActivity.class);
        startActivity(intent);
    }

    public boolean isServicesOK() {
        Log.d(TAG, "isServicesOK: Checking google services version...");
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
}
