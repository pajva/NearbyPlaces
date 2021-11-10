package com.example.nearbyplaces;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity{

    //Initialize variable
    String coor1,coor2,coor3,searchName,phNumber,website,rating,address;
    EditText editText;
    Spinner spType;
    Button btFind,myLocation;
    SupportMapFragment supportMapFragment;
    GoogleMap map;
    FusedLocationProviderClient fusedLocationProviderClient;
    double currentLat =0,currentLong =0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Assign Variable
        spType = findViewById(R.id.sp_type);
        btFind = findViewById(R.id.bt_find);
        supportMapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.google_map);
//        supportMapFragment.getMapAsync(this);

        //Initialize array of place type
        String[] placeTypeLIst = {"atm","bank","hospital","movie_theater","restaurant","church","school"};
        //Initialize array of place name
        String[] placeNameList = {"ATM", "Bank", "Hospital", "Movie Theater", "Restaurant","Church","School"};

        //Set adapter on spinner
        spType.setAdapter(new ArrayAdapter<>(MainActivity.this,
                android.R.layout.simple_spinner_dropdown_item, placeNameList));

        //Initialize fused Location provider client
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        
        myLocation = findViewById(R.id.myLocation);
        myLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Check permission
                if (ActivityCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    //When permission granted
                    //call method
                    getCurrentLocation();
                } else {
                    //when permission denied
                    //request permission
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            44);
                }
            }
        });

        

        btFind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //get selected positon of spinner
                int i = spType.getSelectedItemPosition();
//                Toast.makeText(MainActivity.this, currentLat+"/"+currentLong+placeTypeLIst[i], Toast.LENGTH_SHORT).show();
                //initialize url
                String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" + //url
                        "?location=" + currentLat + "," + currentLong +//location latitude and longitude
                        "&radius=5000" + //nearby redius
                        "&types=" + placeTypeLIst[i] + //place type
                        "&sensor=true" +//sensor
                        "&key=" + getResources().getString(R.string.google_maps_key);//google map key

                //execute place task method to download json data
                new PlaceTask().execute(url);
            }
        });
        //Initialize places
        Places.initialize(getApplicationContext(), getResources().getString(R.string.google_maps_key));
        //set edittext on focusable text
        editText = findViewById(R.id.auto_address);
        editText.setFocusable(false);
        editText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //initialize place field list
                List<Place.Field> fieldList = Arrays
                        .asList(Place.Field.ADDRESS,Place.Field.LAT_LNG,Place.Field.NAME,Place.Field.TYPES,
                                Place.Field.OPENING_HOURS,Place.Field.PHONE_NUMBER,Place.Field.WEBSITE_URI,
                                Place.Field.RATING,Place.Field.USER_RATINGS_TOTAL);
                //create intent
                Intent intent = new Autocomplete.IntentBuilder
                        (AutocompleteActivityMode.OVERLAY,fieldList)
                        .build(MainActivity.this);
                //start activity result
                startActivityForResult(intent, 100);
            }
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK){
            //when success
            //initialize place
            Place place = Autocomplete.getPlaceFromIntent(data);
            coor1 = String.valueOf(place.getLatLng());
            Matcher m = Pattern.compile("\\(([^)]+)\\)").matcher(coor1);
            while(m.find()) {
                coor2 = (m.group(1));
                String[] latLong = coor2.split(",");
                currentLat = Double.parseDouble(latLong[0]);
                currentLong = Double.parseDouble(latLong[1]);
//                Toast.makeText(this, coor2, Toast.LENGTH_LONG).show();
            }
            String types = String.valueOf(place.getTypes());
//            Toast.makeText(this, types, Toast.LENGTH_SHORT).show();
            String[]types1 = types.replaceAll("^\\s*\\[|\\]\\s*$", "").split("\\s*,\\s*");
            coor3=types1[0].toLowerCase();
//            Toast.makeText(this, coor3, Toast.LENGTH_SHORT).show();
            //set address on edittext
            editText.setText(place.getName());
            phNumber = String.format("Phone number : "+place.getPhoneNumber());
            website = String.format("Website : "+place.getWebsiteUri());
            rating = String.format("Rating : "+place.getRating());
            address = String.format("Address : "+place.getAddress());
//            getSearchedLocation();
            //set locality name
            searchName = String.format(place.getName());
            supportMapFragment.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap googleMap) {
                    //when map is ready
                    map = googleMap;
                    LatLng myLoca = new LatLng(currentLat,currentLong );
                    map.addMarker(new MarkerOptions().position(myLoca).title(searchName).snippet(address));
//                                    map.moveCamera(CameraUpdateFactory.newLatLng(myLoca));
                    //zoom current location on map
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(currentLat, currentLong),10
                    ));
                }
            });

        }else if(resultCode == AutocompleteActivity.RESULT_ERROR){
            //when error
            //initialize status
            Status status = Autocomplete.getStatusFromIntent(data);
            //display toast
            Toast.makeText(getApplicationContext(),status.getStatusMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

//

    private void getCurrentLocation() {
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();
        }
        //initialize task location
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Task<Location> task = fusedLocationProviderClient.getLastLocation();
                task.addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        //when success
                        if (location !=null){
                            //when location is not equal to null
                            //get current latitude
                            currentLat = location.getLatitude();
                            //get current longitude
                            currentLong = location.getLongitude();
                            //sync map
                            supportMapFragment.getMapAsync(new OnMapReadyCallback() {
                                @Override
                                public void onMapReady(GoogleMap googleMap) {
                                    //when map is ready
                                    map = googleMap;
                                    LatLng myLoca = new LatLng(currentLat,currentLong );
                                    map.addMarker(new MarkerOptions().position(myLoca).title("I am here"));
//                                    map.moveCamera(CameraUpdateFactory.newLatLng(myLoca));
                                    //zoom current location on map
                                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                            new LatLng(currentLat, currentLong),10
                                    ));
                                }
                            });
                        }

                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 44) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //when permission granted
                // call method
                getCurrentLocation();
            }
        }
    }

    private class PlaceTask extends AsyncTask<String,Integer,String> {
        @Override
        protected String doInBackground(String... strings) {
            String data = null;
            try {
                //initialize data
                data = downloadUrl(strings[0]);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return data;
        }

        @Override
        protected void onPostExecute(String s) {
            //execute parser task
            new ParserTask().execute(s);
        }
    }

    private String downloadUrl(String string) throws IOException {
        //initialize url
        URL url = new URL(string);
        //initialize connection
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        //connect connection
        connection.connect();
        //initialize input stream
        InputStream stream = connection.getInputStream();
        //initialize buffer reader
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        //initialize string builder
        StringBuilder builder = new StringBuilder();
        //initialize string variable
        String line = "";
        //use while loop
        while((line=reader.readLine())!=null){
            //append line
            builder.append(line);
        }
        //get append data
        String data = builder.toString();
        //close reader
        reader.close();
        //return data
        return data;
    }

    private class ParserTask extends AsyncTask<String,Integer, List<HashMap<String, String>>> {
        @Override
        protected List<HashMap<String, String>> doInBackground(String... strings) {
            //create json parser class
            JsonParser jsonParser = new JsonParser();
            //initialize hash map list
            List<HashMap<String, String>> mapList = null;
            JSONObject object = null;
            try {
                //initialize json object
                object = new JSONObject(strings[0]);
                //parse json objject
                mapList = jsonParser.parseResult(object);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            //return map list
            return mapList;
        }

        @Override
        protected void onPostExecute(List<HashMap<String, String>> hashMaps) {
            //clear map
            map.clear();
            for (int i = 0; i<hashMaps.size();i++){
                //initialize hash map
                HashMap<String, String> hashMapList = hashMaps.get(i);
                //get lalitude
                double lat = Double.parseDouble(hashMapList.get("lat"));
                //get longitude
                double lng = Double.parseDouble(hashMapList.get("lng"));
                //get name
                String name = hashMapList.get("name");
                //concat latitude and longitude
                LatLng latLng = new LatLng(lat, lng);
                //initialize marker options
                MarkerOptions options = new MarkerOptions();
                //set position option
                options.position(latLng);
                //set title
                options.title(name);
                //add marker on map
                map.addMarker(options);
            }
        }
    }
    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }
}