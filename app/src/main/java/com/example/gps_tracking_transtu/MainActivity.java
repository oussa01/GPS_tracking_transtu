package com.example.gps_tracking_transtu;

import static com.example.gps_tracking_transtu.ConfigActivity.sharedprefrences;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.provider.Settings;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Granularity;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.Task;

import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements TaskListener{
    private static final int PERMISSIONS_FINE_LOCATION = 99;
    private static final int REQUEST_ENABLE_GPS = 101;

    private static final int port = 5600;
    String data="";
    private long runningtime = 0;
    private String baseURL = "";
    private String urlport = "";
    private String urlhost = "";
    private Switch switchbutton;
    private TextView tv_lat, tv_lng, tv_speed, tv_acc, tv_alt,tv_addr;
    Button btnsetting;
    String vecID ="";
    long time;
    Float dist;
    // google location apî service
    FusedLocationProviderClient fusedLocationProviderClient;
    LocationRequest locationRequest;
    LocationCallback locationCallBack;
    private Timer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        switchbutton = findViewById(R.id.switch1);
        setTitle("Transtu GPS tracker");
        switchbutton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    Startgetlocation();
                    Toast.makeText(MainActivity.this, "Gps en Marche", Toast.LENGTH_SHORT).show();

                } else {
                    Stopgetlocation();

                }
            }
        });
        tv_lat = findViewById(R.id.tv_lat);
        tv_lng = findViewById(R.id.tv_lng);
        tv_alt = findViewById(R.id.tv_alt);
        tv_speed = findViewById(R.id.tv_spd);
        tv_acc = findViewById(R.id.tv_acc);
        tv_addr = findViewById(R.id.tv_addr);
        btnsetting = findViewById(R.id.btnparam);
        getsharedinfo();
        if (dist == null && time == 0L){
            Toast.makeText(MainActivity.this, "Aucune parametres pour la configuration de gps ", Toast.LENGTH_SHORT).show();
            Stopgetlocation();
        }else{
        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, time *1000).setWaitForAccurateLocation(false)
                .setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
                .setWaitForAccurateLocation(false).setMinUpdateIntervalMillis(50).setMaxUpdateDelayMillis(100).setMinUpdateDistanceMeters(dist).build();
        }
        locationCallBack = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                if (locationResult != null) {
                    Location location = locationResult.getLastLocation();
                    if (location != null) {
                        UpdateUiValues(location);
                    }
                }
            }
        };
        updateGps();
        checkGpsEnabled();

        btnsetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openActivity();
            }
        });

    }

    private void getsharedinfo(){
        SharedPreferences sp = getApplicationContext().getSharedPreferences(sharedprefrences,Context.MODE_PRIVATE);
        vecID = sp.getString("Vid","");
        dist = sp.getFloat("distance",0.0f);
        time = sp.getLong("time", 10L);
        urlhost = sp.getString("url", "");
        if (urlhost.isEmpty()) {
            // Set a default URL when it's empty
            urlhost = "http://localhost";
            // Optionally, save the default URL back to SharedPreferences
            SharedPreferences.Editor editor = sp.edit();
            editor.putString("url", urlhost);
            editor.apply();
        }
        urlport = sp.getString("port","");
        if (urlport.isEmpty()) {
            // Set a default URL when it's empty
            urlport = "5600";
            // Optionally, save the default URL back to SharedPreferences
            SharedPreferences.Editor editor = sp.edit();
            editor.putString("port", urlport);
            editor.apply();
        }

        baseURL = urlhost+":"+urlport;
        Log.i("uri",baseURL);
        Log.i("baseuri",urlhost);

    }





    private void Stopgetlocation() {
        switchbutton.setText("Switch Gps status to enable tracking");
        tv_lat.setText("Latitude: 0");
        tv_lng.setText("Longitude: 0");
        tv_alt.setText("Altitude: 0");
        tv_acc.setText("Accuracy : 0");
        tv_speed.setText("Speed: 0");
        tv_addr.setText("EMPTY");
        fusedLocationProviderClient.removeLocationUpdates(locationCallBack);
        onStop();

    }
    @Override
    protected void onStop() {
        super.onStop();

        // Cancel the timer if it's running
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
    private boolean onSuccessThreadHasRun = false;
    private boolean onErrorThreadHasRun = false;
    @Override
    public void onSuccess(String message) {
        if (!onSuccessThreadHasRun) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                }
            });
            onSuccessThreadHasRun =true;
        }

    }
    @Override
    public void onError(String error) {
        if (!onErrorThreadHasRun) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show();
                }
            });
            onErrorThreadHasRun =true;
            if (switchbutton.isChecked()){
                onErrorThreadHasRun =false;
            }
        }
    }
    private void Startgetlocation() {

        switchbutton.setText("Switch Gps status to disable tracking");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallBack, null);
            timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    updateGps();
                }
            }, 0, time *1000);

        } else {
            // Request permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_FINE_LOCATION);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case PERMISSIONS_FINE_LOCATION:
                if (grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    updateGps();
                }
                Toast.makeText(this, "App requires Permission to work properly", Toast.LENGTH_SHORT).show();
                finish();
        }
    }
    private void checkGpsEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null && !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showEnableGpsDialog();
        }
    }

    private void updateGps(){

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, location -> UpdateUiValues(location));
        }else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},PERMISSIONS_FINE_LOCATION);
            }
        }
    }
    public void openActivity(){
        Intent intent = new Intent(this,ConfigActivity.class);
        startActivity(intent);

    }


    private void showEnableGpsDialog() {
        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000).setWaitForAccurateLocation(false)
                .setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
                .setWaitForAccurateLocation(false).setMinUpdateIntervalMillis(50).setMaxUpdateDelayMillis(100).setMinUpdateDistanceMeters(2.0f).build();
            LocationSettingsRequest.Builder settingsBuilder = new LocationSettingsRequest.Builder()
                    .addLocationRequest(locationRequest);

            SettingsClient settingsClient = LocationServices.getSettingsClient(this);
            Task<LocationSettingsResponse> task = settingsClient.checkLocationSettings(settingsBuilder.build());

            task.addOnCompleteListener(task1 -> {
                try {
                    LocationSettingsResponse response = task1.getResult(ApiException.class);
                    updateGps();
                } catch (ApiException exception) {
                    if (exception.getStatusCode() == LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
                        try {
                            ResolvableApiException resolvable = (ResolvableApiException) exception;
                            resolvable.startResolutionForResult(MainActivity.this, REQUEST_ENABLE_GPS);
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error
                        }
                    }
                }
            });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_GPS) {
            if (resultCode == RESULT_OK) {
                // User enabled GPS
                Startgetlocation();
                Log.i("Dist Value", String.valueOf(dist));
                Log.i("Time Value", String.valueOf(time));
                switchbutton.setChecked(true);
                Toast.makeText(MainActivity.this, "GPS en marche.", Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                // User declined to enable GPS
                switchbutton.setChecked(false);
                Toast.makeText(MainActivity.this, "GPS n\'est pas en ecoute.", Toast.LENGTH_SHORT).show();
            }
        }
    }



    private void UpdateUiValues(Location location) {
        if (location != null) {

                tv_lat.setText(String.valueOf(location.getLatitude()));
                tv_lng.setText(String.valueOf(location.getLongitude()));

                if (location.hasAltitude()) {
                    tv_alt.setText(String.valueOf(location.getAltitude()));
                } else {
                    tv_alt.setText("Non disponible");
                }

                // Display speed in km/h
                if (location.hasSpeed()) {
                    float speedKmh = location.getSpeed() * 3.6f; // Convert m/s to km/h
                    tv_speed.setText(String.valueOf(speedKmh));
                } else {
                    tv_speed.setText("Non disponible");
                }

                if (location.hasAccuracy()) {
                    tv_acc.setText(String.valueOf(location.getAccuracy()));
                    Log.i("accuracy:", String.valueOf(location.getAccuracy()));
                } else {
                    tv_acc.setText("Non disponible");

                }
            runningtime = android.os.SystemClock.uptimeMillis();
            DobackgroundTask performBackgroundTask = new DobackgroundTask(vecID,location.getLatitude(), location.getLongitude(), location.getAltitude(),
                    location.getSpeed(),location.getBearing(), location.getAccuracy(),
                    getAddressFromLocation(location.getLatitude(),location.getLongitude()),new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(location.getTime())
                    ,Build.VERSION.RELEASE,baseURL,this);
            performBackgroundTask.execute();
            getAddressFromLocation(location.getLatitude(), location.getLongitude());
        } else {

                // Handle the case where location is null
                tv_lat.setText("Latitude: N/A");
                tv_lng.setText("Longitude: N/A");
                tv_alt.setText("Altitude: N/A");
                tv_acc.setText("Accuracy: N/A");
                tv_speed.setText("Speed: N/A");

        }
    }
    private String getAddressFromLocation(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this);
        String addressText = "";

        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);

            if (!addresses.isEmpty()) {
                Address address = addresses.get(0);
                addressText = address.getAddressLine(0); // Get full address including street, city, etc.
                tv_addr.setText(addressText);
            } else {
                tv_addr.setText("Address not available");
            }
        } catch (IOException e) {
            e.printStackTrace();
            tv_addr.setText("Address retrieval error");
        }
        return addressText;
    }


}