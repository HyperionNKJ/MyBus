package com.example.user.mybus;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;


import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;

import static util.Config.DEFAULT_MAP_TYPE;
import static util.Config.MAP_ZOOM_VALUE;
import static util.Config.OVERVIEW_ZOOM_VALUE;
import static util.Config.PLACE_FIELDS;
import static util.Config.SINGAPORE;
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
    private Marker mapMarker;

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
        updateCamera(SINGAPORE, false, true, OVERVIEW_ZOOM_VALUE);
        googleMap.setOnMapClickListener(getOnMapClickListener());
        googleMap.setOnMarkerClickListener(getOnMarkerClickListener());
        googleMap.setOnMarkerDragListener(getOnMarkerDragListener());

        setupToggleButton();
        setupResetButton();
    }

    private GoogleMap.OnMapClickListener getOnMapClickListener() {
        return new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                if (googleMap.getCameraPosition().zoom <= OVERVIEW_ZOOM_VALUE) {
                    updateCamera(latLng, true, true, MAP_ZOOM_VALUE);
                } else {
                    if (mapMarker == null) {
                        placeNewMarker(latLng);
                    } else {
                        updateExistingMarker(latLng);
                    }
                    updateCamera(latLng, true, false, -1);
                }

            }
        };
    }

    // always centers camera at given LatLng
    private void updateCamera(LatLng newLatLng, boolean willAnimate, boolean willZoom, float zoomValue) {
        CameraUpdate cameraUpdate;
        if (willZoom) {
            cameraUpdate = CameraUpdateFactory.newLatLngZoom(newLatLng, zoomValue);
        } else {
            cameraUpdate = CameraUpdateFactory.newLatLng(newLatLng);
        }
        if (willAnimate) {
            googleMap.animateCamera(cameraUpdate);
        } else {
            googleMap.moveCamera(cameraUpdate);
        }
    }

    private void placeNewMarker(LatLng latLng) {
        mapMarker = googleMap.addMarker(new MarkerOptions()
                .position(latLng)
                .draggable(true)
                .title(getPositionAddress(latLng))
                .snippet(getString(R.string.info_window_set_as_snippet)));
        updateIsInfoWindowShown(false);
    }

    private String getPositionAddress(LatLng latLng) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            return addresses.get(0).getAddressLine(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return getString(R.string.geocoder_unknown_address);
    }

    private void updateExistingMarker(LatLng latLng) {
        boolean isInfoWindowShownInitially = isInfoWindowShown();
        mapMarker.setPosition(latLng); // teleport to new position, hiding the info window
        mapMarker.setTitle(getPositionAddress(latLng));
        if (isInfoWindowShownInitially) {
            mapMarker.showInfoWindow();
        }
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

    private void setupResetButton() {
        findViewById(R.id.b_map_reset).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                removeExistingMarker();
                updateCamera(SINGAPORE, true, true, OVERVIEW_ZOOM_VALUE);
            }
        });
    }

    private void removeExistingMarker() {
        if (mapMarker != null) {
            mapMarker.remove(); // remove from map
            mapMarker = null; // remove the object from reference
        }
    }

    private GoogleMap.OnMarkerClickListener getOnMarkerClickListener() {
        return new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                boolean isInfoWindowShown = isInfoWindowShown();
                if (isInfoWindowShown) {
                    marker.hideInfoWindow();
                } else {
                    marker.showInfoWindow();
                    updateCamera(marker.getPosition(), true, false, -1);
                }
                updateIsInfoWindowShown(!isInfoWindowShown);
                return true; // Prevent map's default behavior from occurring
            }
        };
    }

    private void updateIsInfoWindowShown(boolean bool) {
        mapMarker.setTag(bool); // tag = boolean isInfoWindowShown
    }

    private boolean isInfoWindowShown() {
        return (boolean) mapMarker.getTag();
    }

    private GoogleMap.OnMarkerDragListener getOnMarkerDragListener() {
        return new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {
                marker.hideInfoWindow();
            }

            @Override
            public void onMarkerDrag(Marker marker) {}

            @Override
            public void onMarkerDragEnd(Marker marker) {
                LatLng newPosition = marker.getPosition();
                updateCamera(newPosition, true, false, -1);
                marker.setTitle(getPositionAddress(newPosition));
                if (isInfoWindowShown()) {
                    marker.showInfoWindow();
                }
            }
        };
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