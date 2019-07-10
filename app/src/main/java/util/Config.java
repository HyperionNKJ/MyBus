package util;

import java.util.Arrays;
import java.util.List;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.model.Place;


public class Config {
    public static final boolean PRECHECK_ENABLED = false;
    public static final List<Place.Field> PLACE_FIELDS = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG);
    public static final int DEFAULT_MAP_TYPE = GoogleMap.MAP_TYPE_HYBRID;
    public static final LatLng SINGAPORE = new LatLng(1.3485, 103.8195); // Perfect bird eye's view of Singapore
    public static final CameraPosition DEFAULT_CAMERA_VIEW = new CameraPosition(SINGAPORE, 11.35F, 0.0F, 0.0F);  // CameraPosition(LatLng target, float zoom, float tilt, float bearing)
    public static final float MAP_ZOOM_VALUE = 15.2F;
}
