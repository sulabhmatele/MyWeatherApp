package com.laksystems.sulabhmatele.myweatherapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import com.google.android.gms.location.LocationListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;

    private static Context mContext = null;

    private Location mLastLocation;
    private String mIconCode = "";

    private GoogleApiClient mGoogleApiClient;

    private LocationRequest mLocationRequest = null;
    private Bitmap mIconBmp = null;

    private static int UPDATE_INTERVAL = 10000;
    private static int FASTEST_INTERVAL = 5000;
    private static int DISPLACEMENT = 10;

    private TextView locationTextView, conditionTextView, temperatureTextView;
    private ImageView iconImageView;

    double mLatitude = 0;
    double mLongitude = 0;

    RequestQueue mRequestQueue;

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        locationTextView = (TextView) findViewById(R.id.textView);
        conditionTextView = (TextView) findViewById(R.id.textView2);
        temperatureTextView = (TextView) findViewById(R.id.textView3);
        iconImageView = (ImageView) findViewById(R.id.imageView);

        // First we need to check availability of play services
        if (checkPlayServices()) {

            // Building the GoogleApi client
            buildGoogleApiClient();

            if (mGoogleApiClient.isConnected()) {
                Log.d("TEST_Weather"," OnCreate Google Api client connected :: ");
                startLocationUpdates();
                getUpdatedLocationParams();
                makeJsonObjectRequest();
            }
        }
        else
        {
            Toast.makeText(getApplicationContext(),
                    "OnCreate: No Play service", Toast.LENGTH_LONG)
                    .show();
            finish();
        }
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    private void makeJsonObjectRequest() {
        String url = "http://api.openweathermap.org/data/2.5/weather?lat=";

        String newLat = Double.toString(mLatitude);
        String newLon = Double.toString(mLongitude);

        String api_key = "PasteYourAPIKeyHERE";
        String mergedUrl = url+newLat+"&lon="+newLon+"&appid=";

        final String jsonObjUrl = mergedUrl+api_key;
        System.out.println("URL" +jsonObjUrl);

        Log.d("TEST_Weather","JsonUrlReq: " + jsonObjUrl);

        mRequestQueue = Volley.newRequestQueue(this);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest((Request.Method.GET),jsonObjUrl,
                                               null,
                new Response.Listener<JSONObject>()
                {
                    @Override
                    public void onResponse(JSONObject response)
                    {
                        Log.d("TEST_Weather"," Json Response received: " + response.toString());

                     //   Toast.makeText(getApplicationContext(), response.toString(), Toast.LENGTH_SHORT).show();

                        try {
                            String locationName = response.getString("name");
                            locationTextView.setText(locationName);

                            JSONArray ja = response.getJSONArray("weather");

                            JSONObject jsonObject = ja.getJSONObject(0);

                            String description = jsonObject.getString("description");
                            String main = jsonObject.getString("main");

                            conditionTextView.setText(main + " , " + description);
                            mIconCode = jsonObject.getString("icon");

                            new IconDownloadandShow().execute("");
                            Thread.sleep(500);

                            iconImageView.setImageBitmap(mIconBmp);

                            JSONObject temperatureJsonObj = response.getJSONObject("main");
                            String temperature = temperatureJsonObj.getString("temp");
                            Log.d("TEST_WEATHER" , "temperature received as -----------: " + temperature );
                            float kelvin = Float.parseFloat(temperature);
                            float celcius = kelvin - 273.15f;
                            String weatherTemp = Float.toString(celcius);

                            temperatureTextView.setText(weatherTemp + "\u00b0c");


                        } catch (JSONException e) {

                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error)
                    {
                        Toast.makeText(getApplicationContext(), error.toString(), Toast.LENGTH_SHORT).show();
                    }
                });

        mRequestQueue.add(jsonObjectRequest);
    }

    private class IconDownloadandShow extends AsyncTask<String, Void, String> {
         Context outerContext = mContext;
        @Override
        protected String doInBackground(String... strings) {

            URL url = null;

            try {
                url = new URL("http://openweathermap.org/img/w/"+mIconCode+".png");
                Log.d("TEST_WEATHER", "mIconCode: " + mIconCode + " and url is :" + url);
                mIconBmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }
    }

    private void getUpdatedLocationParams() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Toast.makeText(getApplicationContext(),
                    "Display Location: No permission.", Toast.LENGTH_LONG)
                    .show();
            finish();
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi
                .getLastLocation(mGoogleApiClient);

        if (mLastLocation != null) {
            mLatitude = mLastLocation.getLatitude();
            mLongitude = mLastLocation.getLongitude();

            Log.d("TEST_Weather"," Location Param: mLatitude: "+ mLatitude + " ,mLongitute: "+ mLongitude);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();// ATTENTION: This was auto-generated to implement the App Indexing API.
// See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    protected void onResume() {
        super.onResume();

        checkPlayServices();

        if (mGoogleApiClient.isConnected()) {
            startLocationUpdates();
        }
    }

    private void startLocationUpdates() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.

            Toast.makeText(getApplicationContext(),
                    "Start LocationUpdate: No Permission.", Toast.LENGTH_LONG)
                    .show();
            finish();
            return;
        }
        createLocationRequest();
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, (com.google.android.gms.location.LocationListener) this);
    }

    protected void onPause() {
        super.onPause();
        stopLocationUpdates();

    }

    private void stopLocationUpdates() {

        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, (com.google.android.gms.location.LocationListener) this);
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
    }

    private boolean checkPlayServices() {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(mContext);

        if (resultCode != ConnectionResult.SUCCESS) {
        if (googleApiAvailability.isUserResolvableError(resultCode)) {
            googleApiAvailability.getErrorDialog(this, resultCode,
                    PLAY_SERVICES_RESOLUTION_REQUEST).show();
        } else {
            Toast.makeText(getApplicationContext(),
                    "This device is not supported.", Toast.LENGTH_LONG)
                    .show();
            finish();
        }
        return false;
    }

    return true;
}


    @Override
    public void onConnected(Bundle bundle) {
        Log.d("TEST_Weather"," onConnected:: ");

        startLocationUpdates();
        getUpdatedLocationParams();
        makeJsonObjectRequest();
    }

    @Override
    public void onConnectionSuspended(int i) {

        mGoogleApiClient.connect();

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {


        Log.i(TAG, "Connection Failed" + connectionResult.getErrorCode());

    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d("TEST_Weather"," OnLocation Changed:: ");

        mLastLocation = location;
        getUpdatedLocationParams();
        makeJsonObjectRequest();
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Main Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }
}