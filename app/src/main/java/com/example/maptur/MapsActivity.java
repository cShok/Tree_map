package com.example.maptur;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.maptur.databinding.ActivityMapsBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private FirebaseAuth mAuth;
    FirebaseFirestore db;
    private boolean signedIn = true;
    private GoogleSignInClient googleSignInClient;
    private SignInButton btSignIn;
    private Button btLogout;

    private Button notSignedButton;
    private TextView notSignedText;

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

        // Assign variable
        btSignIn=findViewById(R.id.bt_sign_in);
        btLogout=findViewById(R.id.bt_logout);


        // Initialize sign in options
        GoogleSignInOptions googleSignInOptions=new GoogleSignInOptions.Builder(
                GoogleSignInOptions.DEFAULT_SIGN_IN
        ).requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        // Initialize sign in client
        googleSignInClient= GoogleSignIn.getClient(MapsActivity.this
                ,googleSignInOptions);

        btSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Initialize sign in intent
                Intent intent=googleSignInClient.getSignInIntent();
                // Start activity for result
                // todo: replace with https://stackoverflow.com/questions/62671106/onactivityresult-method-is-deprecated-what-is-the-alternative
                startActivityForResult(intent,100);
            }
        });

        btLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Sign out from google
                googleSignInClient.signOut().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        // Check condition
                        if(task.isSuccessful())
                        {
                            // When task is successful
                            // Sign out from firebase
                            mAuth.signOut();
                            // Display Toast
                            Toast.makeText(getApplicationContext(), "Logout successful", Toast.LENGTH_SHORT).show();
                            // Update UI
                            updateUI();
                        }
                    }
                });
            }
        });

    }

    private void updateUI() {
        // Initialize google sign in account
        GoogleSignInAccount googleSignInAccount=GoogleSignIn.getLastSignedInAccount(MapsActivity.this);
        // Check condition
        if(googleSignInAccount!=null)
        {
            // When google sign in account is not null
            // Set visibility
            btLogout.setVisibility(View.VISIBLE);
            btSignIn.setVisibility(View.GONE);
        }
        else
        {
            // When google sign in account is null
            // Set visibility
            btLogout.setVisibility(View.GONE);
            btSignIn.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Check condition
        if(requestCode==100)
        {
            // When request code is equal to 100
            // Initialize task
            Task<GoogleSignInAccount> signInAccountTask= GoogleSignIn
                    .getSignedInAccountFromIntent(data);

            // check condition
            if(signInAccountTask.isSuccessful())
            {
                // When google sign in successful
                // Initialize string
                String s="Google sign in successful";
                // Display Toast
                displayToast(s);
                // Initialize sign in account
                try {
                    // Initialize sign in account
                    GoogleSignInAccount googleSignInAccount=signInAccountTask
                            .getResult(ApiException.class);
                    // Check condition
                    if(googleSignInAccount!=null)
                    {
                        // When sign in account is not equal to null
                        // Initialize auth credential
                        AuthCredential authCredential= GoogleAuthProvider
                                .getCredential(googleSignInAccount.getIdToken()
                                        ,null);
                        // Check credential
                        mAuth.signInWithCredential(authCredential)
                                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                                    @Override
                                    public void onComplete(@NonNull Task<AuthResult> task) {
                                        // Check condition

                                        if(task.isSuccessful())
                                        {
                                            displayToast("Firebase authentication successful");
                                            onStart();
                                        }
                                        else
                                        {
                                            // When task is unsuccessful
                                            // Display Toast
                                            displayToast("Authentication Failed :"+task.getException()
                                                    .getMessage());
                                        }
                                    }
                                });

                    }
                }
                catch (ApiException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    private void displayToast(String s) {
        Toast.makeText(getApplicationContext(),s,Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            btSignIn.setVisibility(View.INVISIBLE);
            btLogout.setVisibility(View.VISIBLE);
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
        mMap.setOnMarkerClickListener(this::onMarkerClick);
        mMap.setOnMapLongClickListener(this::onMapLongClick);


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
//        db.collection("markers").add(newMarker);

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
}
