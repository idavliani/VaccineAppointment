package unipi.protal.vaccineappointment;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import unipi.protal.vaccineappointment.databinding.ActivityMapsBinding;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static unipi.protal.vaccineappointment.FirebaseUIActivity.REQUEST_LOCATION;
import static unipi.protal.vaccineappointment.FirebaseUIActivity.START_MAPS_ACTIVITY;
import static unipi.protal.vaccineappointment.FirebaseUIActivity.VACCINE_POINTS;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        HospitalAdapter.ListItemClickListener, LocationListener {
    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private boolean mapReady = false;
    private HospitalAdapter hospitalAdapter;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;
    private LocationManager manager;
    private Location currentLocation;
    private LatLng position;
    private Marker marker;
    private ArrayList<Hospital> hospitalList;
    private List<Hospital> hospitalsByDistance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        /*
        default lat long to Syntagma square
         */
        position = new LatLng(37.9750952, 23.7328519);
        manager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        } else {
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
            currentLocation = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference().child(VACCINE_POINTS);
        hospitalList = new ArrayList<>();
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Hospital hospital = snapshot.getValue(Hospital.class);
                    hospitalList.add(hospital);
                }
                if (currentLocation != null) {
                    calculateDistance(currentLocation);
                    binding.firebaseProgressBar.setVisibility(View.GONE);
                    createAdapter();
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting hospitals failed, log a message

            }
        });
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        binding.btnMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mapReady)
                    mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            }
        });

        binding.btnSatellite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mapReady)
                    mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
            }
        });

        binding.btnHybrid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mapReady)
                    mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
            }
        });
    }

    private void createAdapter() {
        hospitalsByDistance = hospitalList.stream()
                .sorted(Comparator.comparing(Hospital::getDistance))
                .collect(Collectors.toList());
        hospitalAdapter = new HospitalAdapter(hospitalsByDistance, this);
        binding.hospitalRecyclerView.setAdapter(hospitalAdapter);
    }

    private void calculateDistance(Location location) {
        for (Hospital hospital : hospitalList) {
            Location hospitalLocation = new Location(hospital.getTitle());
            hospitalLocation.setLatitude(hospital.getLatitude());
            hospitalLocation.setLongitude(hospital.getLongitute());
            Float distance = hospitalLocation.distanceTo(location);
            hospital.setDistance(distance);
        }

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mapReady = true;
        try {
            position = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
        } catch (NullPointerException ne) {
            ne.printStackTrace();
        }
        marker = mMap.addMarker(new MarkerOptions().position(position));
        CameraPosition target = CameraPosition.builder().target(position).zoom(14).build();
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(target));

    }

    @Override
    public void onListItemClick(int clickedItemIndex) {
        Intent i = new Intent(this, AppointmentActivity.class);
        i.putExtra("hospital", hospitalsByDistance.get(clickedItemIndex));
        startActivity(i);

    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        marker.remove();
        currentLocation = location;
        position = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
        marker = mMap.addMarker(new MarkerOptions().position(position));
        CameraPosition target = CameraPosition.builder().target(position).zoom(14).build();
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(target));
        calculateDistance(currentLocation);
        binding.firebaseProgressBar.setVisibility(View.GONE);
        createAdapter();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);

            }
        }
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {

    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {

    }


}