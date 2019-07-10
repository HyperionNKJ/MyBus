package com.example.user.mybus;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;


import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import static util.Config.DEFAULT_CAMERA_VIEW;
import static util.Config.DEFAULT_MAP_TYPE;
import static util.Config.MAP_ZOOM_VALUE;
import static util.Config.PLACE_FIELDS;
import static util.Constants.ARRIVAL;
import static util.Constants.DEPARTURE;

public class QueryActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "QueryActivity";

    private GoogleMap googleMap;
    private PlacesClient placesClient;
    private Place departurePlace;
    private Place arrivalPlace;
    private Spinner departureRadius;
    private Spinner arrivalRadius;
    private Button mapTypeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_query);

        initialize();
    }

    private void initialize() {
        Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));
        placesClient = Places.createClient(this);

        setupGoogleMap();
        setupAutocompleteSearch(DEPARTURE);
        setupRadiusSpinner(DEPARTURE);
        setupAutocompleteSearch(ARRIVAL);
        setupRadiusSpinner(ARRIVAL);
    }

    private void setupGoogleMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.google_map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap gMap) {
        googleMap = gMap;
        googleMap.setMapType(DEFAULT_MAP_TYPE);
        googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(DEFAULT_CAMERA_VIEW));
        googleMap.setOnMapClickListener(getOnMapClickListener());

        setupToggleButton();
    }

    private GoogleMap.OnMapClickListener getOnMapClickListener() {
        return new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, MAP_ZOOM_VALUE));
            }
        };
    }

    private void setupToggleButton() {
        mapTypeButton = findViewById(R.id.b_map_type_toggle);
        updateButtonIcon();

        mapTypeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (googleMap.getMapType() == GoogleMap.MAP_TYPE_HYBRID) {
                    googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                } else {
                    googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                }
                updateButtonIcon();
            }
        });
    }

    private void updateButtonIcon() {
        int appropriateIconID = (googleMap.getMapType() == GoogleMap.MAP_TYPE_HYBRID) ? R.drawable.b_toggle_default : R.drawable.b_toggle_satellite;
        mapTypeButton.setBackground(getResources().getDrawable(appropriateIconID));
    }

    private void setupAutocompleteSearch(int type) {
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(
                        (type == DEPARTURE) ? R.id.frag_departure_location : R.id.frag_arrival_location);

        autocompleteFragment.setPlaceFields(PLACE_FIELDS);
        autocompleteFragment.setHint(getString((type == DEPARTURE) ? R.string.departure_location_hint : R.string.arrival_location_hint));
        autocompleteFragment.setCountry("SG");
        autocompleteFragment.setOnPlaceSelectedListener(getPlaceSelectionListener(type));
    }

    private PlaceSelectionListener getPlaceSelectionListener(final int type) {
        return new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                Log.d(TAG, ".\n==================================================================\n" +
                        "Selected " + ((type == DEPARTURE) ? "departure" : "arrival") + " place:" +
                        "\nName: " + place.getName() +
                        "\nID: " + place.getId() +
                        "\nCo-ord: " + place.getLatLng() +
                        "\n==================================================================\n");
                if (type == DEPARTURE) {
                    departurePlace = place;
                } else {
                    arrivalPlace = place;
                }
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(), MAP_ZOOM_VALUE));
            }

            @Override
            public void onError(@NonNull Status status) {
                Log.d(TAG, "An error has occurred. Status code: " + status);
                Toast.makeText(getApplicationContext(), "An error has occurred: Status code: " + status, Toast.LENGTH_LONG).show();
            }
        };
    }

    private void setupRadiusSpinner(int type) {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.spinner_radius, R.layout.spinner_radius);
        adapter.setDropDownViewResource(R.layout.spinner_radius);

        if (type == DEPARTURE) {
            departureRadius = findViewById(R.id.spinner_departure_radius);
            departureRadius.setAdapter(adapter);
        } else {
            arrivalRadius = findViewById(R.id.spinner_arrival_radius);
            arrivalRadius.setAdapter(adapter);
        }
    }
}
