package com.example.maptur;

import java.lang.Thread.*;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.maptur.databinding.ActivityMapsBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db; // private??

    private boolean signedIn = true;
    int ansClick = 0;


    private Button notSignedButton;
    private TextView notSignedText;
    private Button yes;
    private Button no;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // set db
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            currentUser.reload();
        }
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
//      mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));

//       mMap.moveCamera(CameraUpdateFactory.newLatLng();
        mMap.setOnMarkerClickListener(this::onMarkerClick); //marker pushed
        mMap.setOnMapLongClickListener(this::onMapLongClick);//long push


    }


    public void onMapLongClick(LatLng latLng) {
        /* need to check if sign in, if not, do nothing @eKurer*/
        // check if in a public space
        // pop the quiz qustion, first, do you wanna add tree...
        // add a listner or once retrive the data

        if (!signedIn) { //pop-up/ sign in please?
            notSignedButton = (Button) findViewById(R.id.NotSigned);
            notSignedText = (TextView) findViewById(R.id.NotSignedText);

            notSignedText.setVisibility(View.VISIBLE);
            new Handler().postDelayed(() ->  notSignedText.setVisibility(View.INVISIBLE), 3000);
            return;
        }


        //buttun do you want to add tree?

        yes = (Button) findViewById(R.id.wantToAddTreeY);
        no = (Button) findViewById(R.id.wantToAddTreeN);

        yes.setVisibility(View.VISIBLE);
        no.setVisibility(View.VISIBLE);


            yes.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    yes.setVisibility(View.INVISIBLE);
                    no.setVisibility(View.INVISIBLE);

                    addTree(latLng);

                }
            });
            no.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    yes.setVisibility(View.INVISIBLE);
                    no.setVisibility(View.INVISIBLE);


                }

            });


    }
    public boolean onMarkerClick(final Marker marker) {

        // Retrieve the data from the marker.
        Integer clickCount = (Integer) marker.getTag();

        // Check if a click count was set, then display the click count.
        if (clickCount != null) {
            clickCount =(Integer) ((int)clickCount + 1);
            marker.setTag(clickCount);
            Toast.makeText(this,
                    marker.getTitle() +
                            " has been clicked " + clickCount + " times.",
                    Toast.LENGTH_SHORT).show();
        }
        Log.i("makrer count **" , "**********" + clickCount);
        // Return false to indicate that we have not consumed the event and that we wish
        // for the default behavior to occur (which is for the camera to move such that the
        // marker is centered and for the marker's info window to open, if it has one).
        return false;
    }

    public void addTree(LatLng latLng){
        MarkerOptions newMarker = new MarkerOptions();
        // Setting the position for the marker
        newMarker.position(latLng);
        // Setting the title for the marker.
        // This will be displayed on taping the marker
        newMarker.title(latLng.latitude + " : " + latLng.longitude);

        // Placing a marker on the touched position
        mMap.addMarker(newMarker);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        // after validation, add marker to the db

        // need to implemnet clusters
//        db.collection("markers").add(newMarker);
    }
}
