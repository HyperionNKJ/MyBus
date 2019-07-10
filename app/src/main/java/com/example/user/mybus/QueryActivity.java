package com.example.user.mybus;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.api.Status;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;


import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.util.Arrays;

import static util.Config.PLACE_FIELDS;

public class QueryActivity extends AppCompatActivity {

    private static final String TAG = "QueryActivity";

    private PlacesClient placesClient;
    private Place departurePlace;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_query);

        initialize();
        setupAutocompleteSupportFragment(); // from Google Places API
    }

    private void initialize() {
        Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));
        placesClient = Places.createClient(this);
    }

    private void setupAutocompleteSupportFragment() {
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.departure_location);

        autocompleteFragment.setPlaceFields(PLACE_FIELDS);
        autocompleteFragment.setHint(getString(R.string.departure_location_hint));
        autocompleteFragment.setCountry("SG");
        autocompleteFragment.setOnPlaceSelectedListener(getPlaceSelectionListener());
    }

    private PlaceSelectionListener getPlaceSelectionListener() {
        return new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                Log.d(TAG, "Selected departure place:\nName:" + place.getName() + "\nID: " + place.getId() + "\nCo-ord: " + place.getLatLng() + "\n");
                departurePlace = place;
            }

            @Override
            public void onError(@NonNull Status status) {
                Log.d(TAG, "An error has occurred. Status code: " + status);
                Toast.makeText(getApplicationContext(), "An error has occurred: Status code: " + status, Toast.LENGTH_LONG).show();
            }
        };
    }
}




