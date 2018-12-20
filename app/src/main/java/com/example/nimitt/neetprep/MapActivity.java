package com.example.nimitt.neetprep;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.ImageFormat;
import android.graphics.drawable.BitmapDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener {

    public static final String TAG = "MapActivity";
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private Boolean mLocationPermissionsGranted = false;
    private static final int LOCATION_PERMISSION_REQUEST = 123;
    private static final LatLngBounds mLatLngBounds = new LatLngBounds(new LatLng(-40, -168), new LatLng(71, 136));
    private final String CHANNEL_ID = "50";
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private static final float DEFAULT_ZOOM = 10F;
    private PlaceAutocompleteAdapter mPlaceAutocompleteAdapter;
    private GoogleApiClient mGoogleApiClient;
    private AutoCompleteTextView mSearch;
    private ImageView myLocation;
    private TextView dateId;
    private ImageView nextDate;
    private ImageView presentDate;
    private ImageView previousDate;
    private Calendar c,y,x;
    private TextView sunRisingTime;
    private TextView sunSettingTime;
    private ImageView mSaveLocation;
    private ImageView mBookmark;
    private LatLng lastLocation;
    private ImageView addNotification;

    DatabaseHelper myDb;
    String placeName;
    ArrayList<LatLng> locationList;


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // Toast.makeText(this, "Map is ready", Toast.LENGTH_SHORT).show();

        if (mLocationPermissionsGranted) {
            getCurrentLocation();
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
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        getLocationPermission();

        myDb = new DatabaseHelper(this);
        mSearch = (AutoCompleteTextView) findViewById(R.id.input_search);
        myLocation = (ImageView) findViewById(R.id.myLocation);
        nextDate = (ImageView) findViewById(R.id.nextDate);
        previousDate = (ImageView) findViewById(R.id.previousDate);
        presentDate = (ImageView) findViewById(R.id.presentDate);
        sunRisingTime = (TextView) findViewById(R.id.sunRisingTime);
        sunSettingTime = (TextView) findViewById(R.id.sunSetTime);
        mSaveLocation = (ImageView) findViewById(R.id.saveLocation);
        mBookmark = (findViewById(R.id.bookmarks));
        addNotification = (ImageView) findViewById(R.id.addNotification);
        c = Calendar.getInstance();
        getPresentDate();
        init();

        previousDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getPreviousDate();
            }
        });
        presentDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getPresentDate();
            }
        });
        nextDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getNextDate();
            }
        });
        mBookmark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                viewAllSavedPlaces();
            }
        });
        addNotification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createNotificationChannel();

                scheduleNotification(MapActivity.this,50);
            }
        });
    }


    private void init() {
        Log.d(TAG, "init: initializing");

        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this, this)
                .build();

        mPlaceAutocompleteAdapter = new PlaceAutocompleteAdapter(this, mGoogleApiClient, mLatLngBounds, null);
        mSearch.setAdapter(mPlaceAutocompleteAdapter);
        placeName = mSearch.getText().toString();

        mSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_SEARCH
                        || i == EditorInfo.IME_ACTION_DONE
                        || keyEvent.getAction() == KeyEvent.ACTION_DOWN
                        || keyEvent.getAction() == KeyEvent.KEYCODE_ENTER) {

                    //execute our method for searching
                    geoLocate();
                }

                return false;
            }
        });
        myLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getCurrentLocation();
            }
        });
        mSearch.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                view = getCurrentFocus();
                if (view != null) {
                    InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                    geoLocate();
                }
            }
        });
    }

    private void geoLocate() {
        Log.d(TAG, "geoLocate: geolocating");

        String searchString = mSearch.getText().toString();

        Geocoder geocoder = new Geocoder(MapActivity.this);
        List<Address> list = new ArrayList<>();
        try {
            list = geocoder.getFromLocationName(searchString, 1);
        } catch (IOException e) {
            Log.e(TAG, "geoLocate: IOException: " + e.getMessage());
        }

        if (list.size() > 0) {
            Address address = list.get(0);

            Log.d(TAG, "geoLocate: found a location: " + address.toString());
            moveCamera(new LatLng(address.getLatitude(), address.getLongitude()), DEFAULT_ZOOM, address.getAddressLine(0));
        }

    }

    private void getCurrentLocation() {
        Log.d(TAG, "getCurrentLocation: Your current Location");
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        try {
            if (mLocationPermissionsGranted) {
                Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        //if task successful
                        if (task.isSuccessful()) {
                            Log.d(TAG, "onComplete: Found Current Location and " + task.getResult());
                            Location currentLocation = (Location) task.getResult();

                            //mMap.moveCamera();
                            moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), DEFAULT_ZOOM, "Selected Location");
                        } else {
                            Log.d(TAG, "onComplete: Can't find current location");
                            Toast.makeText(MapActivity.this, "Can't get current location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

        } catch (SecurityException se) {
            Log.e(TAG, "getCurrentLocation: SecurityException " + se.getMessage());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1)
            if (resultCode == Activity.RESULT_OK) {
                int position = data.getIntExtra("result", 0);
                lastLocation = locationList.get(position);
                calculation();
                moveCamera(lastLocation, DEFAULT_ZOOM, "Selected Location");
            }
    }

    private void moveCamera(final LatLng latLng, float zoom, String title) {
        Log.d(TAG, "moveCamera: move camera to current location");
        mMap.clear();
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));


        SimpleDateFormat sdf0 = new SimpleDateFormat("dd:MM:YYYY");
        final String dateMonth = sdf0.format(c.getTime());

        lastLocation = latLng;

        calculation();
        if (!title.equals("My Location")) {
            MarkerOptions options = new MarkerOptions()
                    .position(latLng)
                    .title(title);
            mMap.addMarker(options);
        }


        mSaveLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveCurrentLocation(latLng, dateMonth);
            }
        });
    }

    private void calculation() {

        TimeZone zone = TimeZone.getTimeZone("Asia/Kolkata");
        x = getSunset(lastLocation.latitude, lastLocation.longitude, zone, c, 6);
        //Toast.makeText(this, x.getTime().toString(),Toast.LENGTH_SHORT).show();
        y = getSunrise(lastLocation.latitude, lastLocation.longitude, zone, c, 6);
        //Toast.makeText(this, y.getTime().toString(),Toast.LENGTH_SHORT).show();


        SimpleDateFormat sdf0 = new SimpleDateFormat("dd:MM:YYYY");
        final String dateMonth = sdf0.format(y.getTime());

        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm");
        String sunRiseTime = sdf.format(y.getTime());

        sunRisingTime.setText(sunRiseTime);

        SimpleDateFormat sdf1 = new SimpleDateFormat("hh:mm");
        String sunSetTime = sdf1.format(x.getTime());
        sunSettingTime.setText(sunSetTime);

    }

    private void initMap() {
        Log.d(TAG, "init Map:Initializing Map");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(MapActivity.this);
    }

    private void getLocationPermission() {

        Log.d(TAG, "getLocationPermissions");
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(), COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionsGranted = true;
                initMap();
            } else {
                ActivityCompat.requestPermissions(this,
                        permissions, LOCATION_PERMISSION_REQUEST);
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    permissions, LOCATION_PERMISSION_REQUEST);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionCalled");
        mLocationPermissionsGranted = false;

        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST: {
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            mLocationPermissionsGranted = false;
                            return;
                        }
                    }
                    mLocationPermissionsGranted = true;

                }
            }
            initMap();
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private void getPresentDate() {
        c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
        String formattedDate = df.format(c.getTime());

        //Toast.makeText(this,formattedDate,Toast.LENGTH_SHORT).show();
        dateId = (findViewById(R.id.dateId));
        dateId.setText(formattedDate);

        if (lastLocation != null)
            calculation();

    }

    private void getPreviousDate() {
        //Calendar c = Calendar.getInstance();

        c.add(Calendar.DATE, -1);
        SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
        String formattedDate = df.format(c.getTime());

        Log.v("PREVIOUS DATE : ", formattedDate);
        dateId.setText(formattedDate);
        calculation();
    }

    private void getNextDate() {

        c.add(Calendar.DATE, 1);
        SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
        String formattedDate = df.format(c.getTime());

        Log.v("PREVIOUS DATE : ", formattedDate);
        dateId.setText(formattedDate);
        calculation();
    }

    public void saveCurrentLocation(LatLng latLng, String c) {

        String selectedLocation = mSearch.getText().toString();
        boolean isInserted;
        if (selectedLocation.length() > 0) {
            isInserted = myDb.insertData(mSearch.getText().toString(), latLng.latitude, latLng.longitude, c.toString());
        } else {
            isInserted = myDb.insertData("Your Selected Location", latLng.latitude, latLng.longitude, c.toString());
        }
        if (isInserted == true)
            Toast.makeText(MapActivity.this, "Data Inserted", Toast.LENGTH_LONG).show();
        else
            Toast.makeText(MapActivity.this, "Data not Inserted", Toast.LENGTH_LONG).show();

    }

    private void viewAllSavedPlaces() {
        Cursor res = myDb.getAllData();
        if (res.getCount() == 0) {
            // show message
            showMessage("Error", "Nothing found");
            return;
        }

        locationList = new ArrayList<LatLng>();
        ArrayList<String> placeList = new ArrayList<String>();
        while (res.moveToNext()) {
            placeList.add(res.getString(0));
            locationList.add(new LatLng(Double.valueOf(res.getString(1)), Double.valueOf(res.getString(2))));
        }

        // Show all data
        Intent intent = new Intent(MapActivity.this, ListActivity.class);
        intent.putExtra("key", placeList);
        startActivityForResult(intent, 1);
        //showMessage("Data",buffer.toString());

    }

    public void showMessage(String title, String Message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setTitle(title);
        builder.setMessage(Message);
        builder.show();
    }

    public static Calendar getSunrise(double latitude, double longitude, TimeZone timeZone, Calendar date, double degrees) {
        Location location = new Location("");
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        SolarEventCalculator solarEventCalculator = new SolarEventCalculator(new Location(location), timeZone);
        return solarEventCalculator.computeSunriseCalendar(new Zenith(90 - degrees), date);
    }

    public static Calendar getSunset(double latitude, double longitude, TimeZone timeZone, Calendar date, double degrees) {
        Location location = new Location("");
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        SolarEventCalculator solarEventCalculator = new SolarEventCalculator(new Location(location), timeZone);
        return solarEventCalculator.computeSunsetCalendar(new Zenith(90 - degrees), date);
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Channel Name";
            String description = "Channel Description";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void scheduleNotification(Context context, int notificationId) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Photography")
                .setContentText("Golden Hour")
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_sun_rising)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));

        String diff=dateId.getText().toString();
        diff=diff+sunRisingTime.getText().toString();
        SimpleDateFormat diffFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        //Date date = format.parse(string);
        String formattedDate=diffFormat.format(c.getTime());


        Calendar c1=Calendar.getInstance();
        long miliSec=x.getTimeInMillis()-c1.getTimeInMillis();
        if(miliSec<3600000)
            Toast.makeText(this, "Please select a future date", Toast.LENGTH_SHORT).show();
        else {
            Toast.makeText(this, "You will get a notification an hour before sunset", Toast.LENGTH_LONG).show();

            Notification notification = builder.build();

            Intent notificationIntent = new Intent(context, NotificationPublisher.class);
            notificationIntent.putExtra(NotificationPublisher.NOTIFICATION_ID, notificationId);
            notificationIntent.putExtra(NotificationPublisher.NOTIFICATION, notification);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, notificationId, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            long futureInMillis = SystemClock.elapsedRealtime() + (miliSec - 3600000);
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, futureInMillis, pendingIntent);

        }
    }
}
