package com.eap.sdy61.ge4.eva_b.eapp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OneFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link OneFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class OneFragment extends Fragment {

    MapView mMapView;
    private GoogleMap googleMap;
    public LocationRequest mLocationRequest;
    FusedLocationProviderClient mFusedLocationClient;
    Location mLastLocation;

    LinearLayout mPosInfo;
    Button startBtn;
    TextView mLat;
    TextView mLon;
    TextView mAccuracy;


    // Hashmap only for last marker (it will contain only one entry)
    HashMap<String, Location> hashMapLocation = new HashMap<>();

    // Hashmap only for last marker's timestamp (it will contain only one entry)
    HashMap<String, Date> hashMapDate = new HashMap<>();



    public OneFragment() {
        // Required empty public constructor
    }

    public static OneFragment newInstance(String param1, String param2) {
        OneFragment fragment = new OneFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_one, container, false);

        mMapView = (MapView) rootView.findViewById(R.id.mapView);
        mPosInfo = (LinearLayout) rootView.findViewById(R.id.posInfoLayout);
        mLat = (TextView) rootView.findViewById(R.id.phonePosLat);
        mLon = (TextView) rootView.findViewById(R.id.phonePosLon);
        mAccuracy = (TextView) rootView.findViewById(R.id.posAccuracy);
        mMapView.onCreate(savedInstanceState);

        mMapView.onResume(); // needed to get the map to display immediately

        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }

        mMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap mMap) {
                googleMap = mMap;

                //Initialize Google Play Services
                mLocationRequest = new LocationRequest();
                mLocationRequest.setInterval(10000); // 10 seconds interval
                mLocationRequest.setFastestInterval(8000); // 8 seconds fast interval
                mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        //Location Permission already granted
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                        mMap.setMyLocationEnabled(true);
                    } else {
                        //Request Location Permission
                        checkLocationPermission();
                    }
                } else {
                    mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                    googleMap.setMyLocationEnabled(true);
                }
            }
        });
        mMapView.setVisibility(View.GONE);
        mPosInfo.setVisibility(View.GONE);

        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        startBtn = getView().findViewById(R.id.btnStart);
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startBtn.setVisibility(View.GONE);
                mMapView.setVisibility(View.VISIBLE);
                mPosInfo.setVisibility(View.VISIBLE);
                LatLng latLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                //move map camera
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));
            }
        });
    }

    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        // After getting the latest location. save it and re-zoom the map
        public void onLocationResult(LocationResult locationResult) {
            for (Location location : locationResult.getLocations()) {
//                Log.i("OneFragment", "Location: " + location.getLatitude() + " " + location.getLongitude());
                mLastLocation = location;
                //Place current location marker
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                mLat.setText("Γεωγρ. μήκος: " + mLastLocation.getLatitude());
                mLon.setText("Γεωγρ. πλάτος: " + mLastLocation.getLongitude());
                mAccuracy.setText("Ακρίβεια θέσης: " + mLastLocation.getAccuracy());

                putMarker(latLng, mLastLocation);
                //move map camera
              googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));
            }
        }
    };


    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
                alert.setTitle("Χρειάζεται άδεια για τον εντοπισμό θέσης");
                alert.setMessage("Η παρούσα εφαρμογή χρειάζεται την άδεια σας για να ανιχνεύσει την τοποθεσία σας, παρακαλούμε δεχτείτε τη για να αξιοποιήσετε τη λειτουργία εντοπισμού θέσης.");
                alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //Prompt the user once explanation has been shown
                        ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);
                    }
                }).create().show();
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);
            }

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                       googleMap.setMyLocationEnabled(true);
                    }
                } else {
                    // permission denied! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(getContext(), "Άρνηση παροχής αδείας.", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    void putMarker(LatLng point, Location loc) {
        // Get current date and time
        Date date = new Date();

        // Initialize duration and distance vars
        long duration;
        double distance;
        double speed;

        if (hashMapLocation.isEmpty()) {
            distance = 0;
        } else {
            // Retrieve the previous data (location)
            Location prevLoc = hashMapLocation.get("lastLocation");
            if (prevLoc != null) {
                // Get the distance
                distance = loc.distanceTo(prevLoc)/1000;
            } else {
                distance = 0;
            }

        }

        if (hashMapDate.isEmpty()) {
            duration = 0;
        } else {
            // Retrieve the previous data (date)
            Date prevDate = hashMapDate.get("lastDate");
            if (prevDate != null) {
                // Get the duration
                duration = (date.getTime()-prevDate.getTime())/1000;
            } else {
                duration = 0;
            }
        }

        if (duration!= 0) {
           speed = distance / duration;
        } else {
            speed = 0;
        }

        Log.i("OneFragment", "Speed distance date location: " + convertToDecimal(speed) + " " + distance + " " + date + " " + loc);

        // Create marker
        MarkerOptions markerOptions = new MarkerOptions();
        // Set the marker's position
        markerOptions.position(point);
        // Set the marker's colour
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE));
        // Set the marker's title (it is defined by location name)
        markerOptions.title(getLocationName(point.latitude, point.longitude));
        // Set the marker's details (speed in particular)
        markerOptions.snippet("Ταχύτητα: " + convertToDecimal(speed) + " m/s");

        // Finally, add the marker
        Marker mark = googleMap.addMarker(markerOptions);

        // Clear temporary data
        hashMapLocation.clear();
        hashMapDate.clear();

        // Add newest temporary data
        hashMapLocation.put("lastLocation", loc);
        hashMapDate.put("lastDate", date);
    }

    // Convert double to decimal with 4 digit points
    public String convertToDecimal(Double dbl) {
        DecimalFormat df = new DecimalFormat("#.####");
        df.setRoundingMode(RoundingMode.CEILING);
        return df.format(dbl);
    }


    // Get a points location name in order to have representative name for the entry
    public String getLocationName(double lat, double lng) {
        Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
        try {
            // Get the available addresses
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            // Check if there exist available addresses
            if (addresses != null && addresses.size() > 0) {
                // Get the first one
                Address obj = addresses.get(0);
                // In this case, we get featured name and locality
                String area = obj.getThoroughfare();
                // We return it as one name
                area = area + ", " + obj.getLocality();
                return area;
            } else {
                return "unknown area";
            }
            // Catch exception
        } catch (IOException e) {
            e.printStackTrace();
            // Inform about the error
            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
        if (mFusedLocationClient != null) {
            requestLocationListener();
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
        if (mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
        if (mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
    }

    public void requestLocationListener() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(8000);
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
