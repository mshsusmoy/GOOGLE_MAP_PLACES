package practice_app_map_places.susmoy.com.practiceapp_google_map_places;

import android.*;
import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.gcm.Task;
import com.google.android.gms.identity.intents.Address;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;

import java.io.IOException;
import java.security.Permission;
import java.util.ArrayList;
import java.util.List;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener {

    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private boolean location_permission_granted = false;
    private static final int REQUEST_CODE = 1234;
    private static final int PLACE_PICKER_REQUEST = 1;
    private GoogleMap googleMap;
    private Marker marker;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private static float MAP_ZOOM = 15f;
    private GoogleApiClient googleApiClient;
    private PlaceAutocompleteAdapter placeAutocompleteAdapter;
    private static final LatLngBounds latLngBounds = new LatLngBounds(new LatLng(-40,-168), new LatLng(71,136));
    private static Location current_location;

    private AutoCompleteTextView editText_search;
    private ImageView imageView_gps, imageView_info, imageView_place_picker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        get_Permission_For_Location();

        editText_search = (AutoCompleteTextView) findViewById(R.id.editText_search);
        editText_search.setSingleLine();

        imageView_gps = (ImageView) findViewById(R.id.imageview_gps);
        imageView_gps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Gps_Imageview_clicked();
            }
        });

        imageView_info = (ImageView) findViewById(R.id.imageview_info);
        imageView_info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Info_imageview_clicked();
            }
        });

        imageView_place_picker = (ImageView) findViewById(R.id.imageview_place_picker);
        imageView_place_picker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Place_picker_clicked();
            }
        });
    }

    public void Place_picker_clicked(){

        PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
        try {
            startActivityForResult(builder.build(MapActivity.this), PLACE_PICKER_REQUEST);
        } catch (GooglePlayServicesRepairableException e) {
            e.printStackTrace();
        } catch (GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK) {
                Place place = PlacePicker.getPlace(this, data);

                PendingResult<PlaceBuffer> place_result = Places.GeoDataApi
                        .getPlaceById(googleApiClient, place.getId());
                place_result.setResultCallback(resultCallback);
            }
        }
    }

    public void Info_imageview_clicked(){
        if(marker.isInfoWindowShown()){
            marker.hideInfoWindow();
        }
        else{
            marker.showInfoWindow();
            //googleMap.setInfoWindowAdapter(new InfoWindow(this));
        }
    }

    public void Gps_Imageview_clicked(){
        Get_current_location();

    }

    public void Search_bar_enable(){

        googleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this, this)
                .build();

        editText_search.setOnItemClickListener(onItemClickListener);

        placeAutocompleteAdapter = new PlaceAutocompleteAdapter(this, googleApiClient, latLngBounds, null);
        editText_search.setAdapter(placeAutocompleteAdapter);

        editText_search.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                    Log.e("Called onEditorAction: ", " onEditorAction");
                    if(i == EditorInfo.IME_ACTION_SEARCH
                            || i == EditorInfo.IME_ACTION_DONE
                            || keyEvent.getAction() == KeyEvent.ACTION_DOWN
                            || keyEvent.getAction() == KeyEvent.KEYCODE_ENTER){

                        Geolocate();
                    }

                return false;
            }
        });

    }

    public void Geolocate(){
        Log.e("Calling Geolocate: ", "Geolocating");
        String input_text = editText_search.getText().toString();
        Geocoder geocoder = new Geocoder(MapActivity.this);
        List<android.location.Address> list = new ArrayList<>();
        try{
            list = geocoder.getFromLocationName(input_text, 1);
        }
        catch(IOException e){
        }

        if(list.size() > 0){
            android.location.Address address = list.get(0);
            Log.e("Geolocate: ", address.toString());

            Move_camera(new LatLng(address.getLatitude(), address.getLongitude()),MAP_ZOOM, address.getAddressLine(0));


        }
    }

    public void Init_map() {
        SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        supportMapFragment.getMapAsync(MapActivity.this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;

        if(location_permission_granted){
            Log.e("onMapReady", "calling Get_current_location");
            Get_current_location();
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            googleMap.setMyLocationEnabled(true);
            googleMap.getUiSettings().setMyLocationButtonEnabled(false);
            Log.e("Calling Search_bar: ", " Search_bar_enable");
            Search_bar_enable();
        }

    }

    public void Get_current_location() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        try {
            if (location_permission_granted) {

                final com.google.android.gms.tasks.Task<Location> location = fusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull com.google.android.gms.tasks.Task<Location> task) {
                        if(task.isSuccessful()){
                            current_location = (Location) task.getResult();
                            Log.e("Get_current_location", ""+ current_location.getLatitude()+ " / "+ current_location.getLongitude());
                            Move_camera(new LatLng(current_location.getLatitude(), current_location.getLongitude()),MAP_ZOOM, "My Location");
                        }
                        else{
                            Log.e("Get_current_location", "---------ERROR!!!!!!!");
                        }
                    }
                });
            }
        }
        catch (SecurityException e){}

    }

    public void Move_camera(LatLng latLng, float zoom, String title){
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
        googleMap.clear();

        googleMap.setInfoWindowAdapter(new InfoWindow(this));
        if(!(title == "My Location")){
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(latLng)
                    .title(title);
            marker = googleMap.addMarker(markerOptions);
        }

    }

    public void get_Permission_For_Location(){
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
        if(ContextCompat.checkSelfPermission(this.getApplicationContext(), FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            if(ContextCompat.checkSelfPermission(this.getApplicationContext(), COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                location_permission_granted = true;
                Log.e("get_Permission_Location", "Init_map called here");
                Init_map();

            }
            else{
                ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE);
            }
        }
        else{
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
       location_permission_granted = false;

        if(requestCode == REQUEST_CODE){
            if(grantResults.length >0){
                for(int i = 0; i< grantResults.length; i++){
                    if(grantResults[i] != PackageManager.PERMISSION_GRANTED){
                        location_permission_granted = false;
                        return;
                    }
                }
                location_permission_granted = true;
                Log.e("onRequestPermisioResult", "Init_map called here");
                //Init_map();
            }
        }

    }

    private AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            final AutocompletePrediction autocompletePrediction_place = placeAutocompleteAdapter.getItem(i);
            String placeId = autocompletePrediction_place.getPlaceId();

            PendingResult<PlaceBuffer> place_result = Places.GeoDataApi
                    .getPlaceById(googleApiClient, placeId);
            place_result.setResultCallback(resultCallback);

        }
    };

    private ResultCallback<PlaceBuffer> resultCallback = new ResultCallback<PlaceBuffer>() {
        @Override
        public void onResult(@NonNull PlaceBuffer places) {
            if(!places.getStatus().isSuccess()){
                Log.e("ResultCallBack:", "Error on ResultCallBack");
                places.release();
                return;
            }
            Log.e("Place Result",places.get(0).getAddress().toString());
            Place place = places.get(0);

            Location a = new Location("A");
            Location b = new Location("B");

            a.setLatitude(current_location.getLatitude());
            a.setLongitude(current_location.getLongitude());
            b.setLatitude(place.getLatLng().latitude);
            b.setLongitude(place.getLatLng().longitude);

            float dis = a.distanceTo(b);
            Log.e("Distance:   ",""+dis/1000.0);
            Toast.makeText(MapActivity.this, "Distance:  "+dis/1000.0, Toast.LENGTH_SHORT).show();

            Move_camera(place.getLatLng(),MAP_ZOOM,place.getAddress().toString());
            places.release();
        }
    };

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
