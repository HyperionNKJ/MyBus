package util;

import java.util.Arrays;
import java.util.List;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.libraries.places.api.model.Place;


public class Config {
    public static final boolean PRECHECK_ENABLED = false;
    public static final List<Place.Field> PLACE_FIELDS = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG);
    public static final int DEFAULT_MAP_TYPE = GoogleMap.MAP_TYPE_HYBRID;
}
