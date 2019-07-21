package com.example.user.mybus;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

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


import android.annotation.SuppressLint;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
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
    private Button mapTypeButton;
    private Marker mapMarker;

    // Direct input
    private AutocompleteSupportFragment mDepartureSearch;
    private Spinner mDepartureRadius;
    private AutocompleteSupportFragment mArrivalSearch;
    private Spinner mArrivalRadius;

    // Set-as dialog
    private RadioGroup setAsRadioGroup;
    private SeekBar radiusSeekbar;
    private TextView radiusProgress;

    // Query data
    private LatLng departureCoord;
    private int departureRadius;
    private LatLng arrivalCoord;
    private int arrivalRadius;

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
        googleMap.setOnInfoWindowClickListener(getOnInfoWindowClickListener());

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

    private GoogleMap.OnInfoWindowClickListener getOnInfoWindowClickListener() {
        return new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                showSetAsDialog(marker.getTitle(), marker.getPosition());
            }
        };
    }

    private void showSetAsDialog(String placeName, LatLng placeCoord) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(QueryActivity.this);
        final View dialogView = getLayoutInflater().inflate(R.layout.dialog_set_as, null);
        dialogBuilder.setView(dialogView);
        dialogBuilder.setCancelable(true);

        initializeDialog(dialogView, placeName, placeCoord);
        final AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.show();
    }

    @SuppressLint("SetTextI18n")
    private void initializeDialog(View dialogView, String placeName, LatLng placeCoord) {
        ((TextView) dialogView.findViewById(R.id.tv_place_name)).setText(placeName);

        setAsRadioGroup = dialogView.findViewById(R.id.rg_set_as);

        radiusSeekbar = dialogView.findViewById(R.id.sb_radius);
        radiusProgress = dialogView.findViewById(R.id.sb_radius_progress);
        radiusProgress.setText(radiusSeekbar.getProgress() + "m"); // show default progress
        radiusSeekbar.setOnSeekBarChangeListener(getOnSeekBarChangeListener());

        dialogView.findViewById(R.id.b_enter).setOnClickListener(getEnterButtonOnClickListener(placeName, placeCoord));
    }

    @SuppressLint("SetTextI18n")
    private SeekBar.OnSeekBarChangeListener getOnSeekBarChangeListener() {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                radiusProgress.setText(progress + "m");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        };
    }

    private View.OnClickListener getEnterButtonOnClickListener(final String placeName, final LatLng placeCoord) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (setAsRadioGroup.getCheckedRadioButtonId() == R.id.rb_departure) {
                    mDepartureSearch.setText(placeName);

                    // TODO Logic for when enter button is pressed
                    departureCoord = placeCoord;
                    departureRadius = radiusSeekbar.getProgress();
//                    mDepartureRadius.set radiusSeekbar.getProgress();
                } else {
                    arrivalCoord = placeCoord;
                    mArrivalSearch.setText(placeName);
                    arrivalRadius = radiusSeekbar.getProgress();
                }
            }
        };
    }

    private void setupAutocompleteSearch(int type) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        AutocompleteSupportFragment autocompleteFragment;

        if (type == DEPARTURE) {
            autocompleteFragment = (AutocompleteSupportFragment) fragmentManager.findFragmentById(R.id.frag_departure_location);
            autocompleteFragment.setHint(getString(R.string.departure_location_hint));
            mDepartureSearch = autocompleteFragment;
        } else {
            autocompleteFragment = (AutocompleteSupportFragment) fragmentManager.findFragmentById(R.id.frag_arrival_location);
            autocompleteFragment.setHint(getString(R.string.arrival_location_hint));
            mArrivalSearch = autocompleteFragment;
        }

        autocompleteFragment.setPlaceFields(PLACE_FIELDS);
        autocompleteFragment.setCountry("SG");
        autocompleteFragment.setOnPlaceSelectedListener(getPlaceSelectionListener(type));
    }

    private PlaceSelectionListener getPlaceSelectionListener(final int type) {
        return new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                Log.d(TAG, printPlaceSelected(place, type));

                LatLng placeLatLng = place.getLatLng();
                if (type == DEPARTURE) {
                    departureCoord = placeLatLng;
                    Log.d(TAG, printQueryData("Departure Co-ord"));
                } else {
                    arrivalCoord = placeLatLng;
                    Log.d(TAG, printQueryData("Arrival Co-ord"));
                }
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(placeLatLng, MAP_ZOOM_VALUE));
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

        Spinner spinner;

        if (type == DEPARTURE) {
            spinner = findViewById(R.id.spinner_departure_radius);
            mDepartureRadius = spinner;
        } else {
            spinner = findViewById(R.id.spinner_arrival_radius);
            mArrivalRadius = spinner;
        }
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(getOnItemSelectedListener(type));
    }

    private Spinner.OnItemSelectedListener getOnItemSelectedListener(final int type) {
        return new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
                String selectedRadiusWithMetre = (String) adapterView.getItemAtPosition(pos); // e,g. "50m"
                int selectedRadius = parseRadiusWithMetre(selectedRadiusWithMetre);
                if (type == DEPARTURE) {
                    departureRadius = selectedRadius;
                    Log.d(TAG, printQueryData("Departure Radius"));
                } else {
                    arrivalRadius = selectedRadius;
                    Log.d(TAG, printQueryData("Arrival Radius"));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        };
    }

    private int parseRadiusWithMetre(String radiusWithMetre) {
        String radiusWithoutMetre =  radiusWithMetre.substring(0, radiusWithMetre.length() - 1);
        return Integer.parseInt(radiusWithoutMetre);
    }

    // ================================== FOR DEBUGGING PURPOSE ==================================  //
    private String printPlaceSelected(Place place, int type) {
        return ".\n==================================================================\n" +
                "Selected " + ((type == DEPARTURE) ? "departure" : "arrival") + " place:" +
                "\nName: " + place.getName() +
                "\nID: " + place.getId() +
                "\nCo-ord: " + place.getLatLng() +
                "\n==================================================================\n";
    }

    private String printQueryData(String updatedData) {
        return  ".\n==================================================================\n" +
                "Change has been made to: " + updatedData +
                "\nDeparture Co-ord: " + departureCoord +
                "\nDeparture Radius: " + departureRadius +
                "\nArrival Co-ord: " + arrivalCoord +
                "\nArrival Radius: " + arrivalRadius +
                "\n==================================================================\n";
    }
}