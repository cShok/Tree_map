package com.example.maptur;


import android.annotation.SuppressLint;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;



import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentActivity;

import com.example.maptur.databinding.ActivityMapsBinding;
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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentReference;


import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private FirebaseAuth mAuth;

    FirebaseFirestore db;
    private boolean signedIn;
    private GoogleSignInClient googleSignInClient;
    private SignInButton btSignIn;
    private Button btLogout;

    private TextView notSignedText;
    private Button yes;
    private Button no;
    private Button moreDetails;


    private LinearLayout addTreeLinear, updateLinear, addExtraLinear;
    private TextView addTreeText, addDesText, existTreeDescription, treeDetailsUpdate;
    private EditText addTreeEdit, addDesEdit, addExtraEdit;
    private Button exitTreeForm, confirmTreeForm, exitDetails, nextDescription, updateTreeCond, existAddTreeDes, conExtraDes;
    private RadioGroup treeCond;
    Editable addTreeName, treeDes;
    private RatingBar rateTree;
    int treeCondSts, treeRating;
    private Map<String, Object> markersMap = new HashMap<>();
    private Map<String, Object> gTreeData = new HashMap<>();
    Map<String, Object> desList = new HashMap<>();

    ArrayList<Object> treeD =new ArrayList<>();
    private String mSnippet, desExtra = " ";
    DocumentReference docRef;
    private int sameM, nums;
    private int iDes = 0;
    private View locationButton;

    @Override
    public void onStart() {
        super.onStart();
        Log.i("onStart", "onStart called");
        updateUI();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("onCreate", "onCreate called");
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
        btSignIn = findViewById(R.id.bt_sign_in);
        btLogout = findViewById(R.id.bt_logout);


        // Initialize sign in options
        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(
                GoogleSignInOptions.DEFAULT_SIGN_IN
        ).requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        // Initialize sign in client
        googleSignInClient = GoogleSignIn.getClient(MapsActivity.this
                , googleSignInOptions);

        btSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Initialize sign in intent
                Intent intent = googleSignInClient.getSignInIntent();
                // Start activity for result
                // todo: replace with https://stackoverflow.com/questions/62671106/onactivityresult-method-is-deprecated-what-is-the-alternative
                startActivityForResult(intent, 100);
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
                        if (task.isSuccessful()) {
                            // When task is successful
                            // Sign out from firebase
                            mAuth.signOut();
                            signedIn = false;
                            // Display Toast
                            Toast.makeText(getApplicationContext(), "Logout successful", Toast.LENGTH_SHORT).show();
                            // Update UI
                            updateUI();
                        }
                    }
                });
            }
        });

        // navigation drawer
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.navigation_view);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull @org.jetbrains.annotations.NotNull MenuItem item) {
                EditText input = null;


                int id = item.getItemId();
                drawerLayout.closeDrawer(GravityCompat.START);
                switch (id) {
                    case R.id.nav_all_trees:
                        Toast.makeText(getApplicationContext(), "All trees", Toast.LENGTH_SHORT).show();
                        presentMarkers();
                        break;
                    case R.id.nav_my_trees:
                        Toast.makeText(MapsActivity.this, "Showing your trees", Toast.LENGTH_SHORT).show();
                        removeMarkers();
                        myMarkers();
                        break;
                    case R.id.fruit_now:


                        Toast.makeText(MapsActivity.this, "Tree that have fruit on them", Toast.LENGTH_SHORT).show();

                        break;
                    //present markers based on users input
                    case R.id.tree_type:
                        input = new EditText(MapsActivity.this);
                        input.setHint("Apple, Pear, Cherry, etc.");
                        EditText finalInput = input;
                        new AlertDialog.Builder(MapsActivity.this)
                                .setTitle("Enter tree type")
                                .setView(input)
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        String treeType = finalInput.getText().toString();
                                        removeMarkers();
                                        searchMarkers(treeType);
                                    }
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                        Toast.makeText(MapsActivity.this, "Showing the trees you chose", Toast.LENGTH_SHORT).show();
                        break;

                    default:
                        return true;
                }
                return true;
            }
        });
        updateUI();
        presentMarkers();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        Log.i("onMapReady", "onMapReady: ");
        //location permission - if not granted, ask for it
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        LatLng Jerusalem = new LatLng(31.7683, 35.2137);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(Jerusalem));
        mMap.moveCamera(CameraUpdateFactory.zoomTo(50000));
        mMap.setOnMarkerClickListener(this::onMarkerClick);//marker pushed
        mMap.setOnMapLongClickListener(this::onMapLongClick);//long push

        // get your maps fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        // Extract My Location View from maps fragment
        locationButton = mapFragment.getView().findViewById(0x2);

        // Change the visibility of my location button
        if (locationButton != null)
            locationButton.setVisibility(View.GONE);

        findViewById(R.id.ic_location).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mMap != null) {
                    if (locationButton != null)
                        locationButton.callOnClick();
                }
            }
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i("onActivityResult", "onActivityResult called");
        // Check condition
        if (requestCode == 100) {
            // When request code is equal to 100
            // Initialize task
            Task<GoogleSignInAccount> signInAccountTask = GoogleSignIn
                    .getSignedInAccountFromIntent(data);

            // check condition
            if (signInAccountTask.isSuccessful()) {
                // When google sign in successful
                // Initialize string
                //String s="Google sign in successful";
                // Display Toast
                //displayToast(s);
                // Initialize sign in account
                try {
                    // Initialize sign in account
                    GoogleSignInAccount googleSignInAccount = signInAccountTask
                            .getResult(ApiException.class);
                    // Check condition
                    if (googleSignInAccount != null) {
                        // When sign in account is not equal to null
                        // Initialize auth credential
                        AuthCredential authCredential = GoogleAuthProvider
                                .getCredential(googleSignInAccount.getIdToken()
                                        , null);
                        // Check credential
                        mAuth.signInWithCredential(authCredential)
                                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                                    @Override
                                    public void onComplete(@NonNull Task<AuthResult> task) {
                                        // Check condition

                                        if (task.isSuccessful()) {
                                            displayToast("Firebase authentication successful");
                                            signedIn = true;
                                            onStart();
                                        } else {
                                            // When task is unsuccessful
                                            // Display Toast
                                            displayToast("Authentication Failed :" + task.getException()
                                                    .getMessage());
                                        }
                                    }
                                });

                    }
                } catch (ApiException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //// GUI functions  ////

    // util helper function
    private void removeMarkers() {
        //remove all markers
        mMap.clear();
    }
    // big helper function
    private void updateUI() {
        // Initialize google sign in account
        GoogleSignInAccount googleSignInAccount = GoogleSignIn.getLastSignedInAccount(MapsActivity.this);
        presentMarkers();
        addTreeLinear = findViewById(R.id.form);
        addTreeLinear.setVisibility(View.INVISIBLE);
        // Check condition
        if (googleSignInAccount != null) {
            signedIn = true;

            // When google sign in account is not null
            // Set visibility
            btLogout.setVisibility(View.VISIBLE);
            btSignIn.setVisibility(View.GONE);


            // change text in nav bar to user's name
            NavigationView navigationView = findViewById(R.id.navigation_view);
            View headerView = navigationView.getHeaderView(0);
            TextView navUsername = (TextView) headerView.findViewById(R.id.name);
            navUsername.setText(googleSignInAccount.getDisplayName());
            // change text in nav bar to user's email
            TextView navEmail = (TextView) headerView.findViewById(R.id.username);
            navEmail.setText(googleSignInAccount.getEmail());

        } else {
            // When google sign in account is null
            // Set visibility
            btLogout.setVisibility(View.GONE);
            btSignIn.setVisibility(View.VISIBLE);
        }
    }
    // small helper function
    private void presentMarkers() {
        // pull all items in collection "markers" from firestore db
        db.collection("markers")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            LatLng marker = new LatLng(document.getDouble("position.latitude"), document.getDouble("position.longitude"));
                            mMap.addMarker(new MarkerOptions().position(marker).title(document.getString("title")));
                        }
                    } else {
                        Log.w("TAG", "Error getting documents.", task.getException());
                    }
                });

    }
    // small helper function
    private void myMarkers() {

        db.collection("markers")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
//                             my trees
//                            for (QueryDocumentSnapshot document : task.getResult()) {
//                            if (document.getString("user").equals(mAuth.getCurrentUser().getUid())) {
//                                LatLng marker = new LatLng(document.getDouble("position.latitude"), document.getDouble("position.longitude"));
//                                mMap.addMarker(new MarkerOptions().position(marker).title(document.getString("title")));
//                            }

                            // if lat is greater than 35.45
                            if (document.getDouble("position.latitude") > 31.5) {
                                LatLng marker = new LatLng(document.getDouble("position.latitude"), document.getDouble("position.longitude"));
                                mMap.addMarker(new MarkerOptions().position(marker).title(document.getString("title")));


                            }
                        }
                    } else {
                        Log.w("TAG", "Error getting documents.", task.getException());
                    }
                });
    }
    // medium helper function
    // a function that will get a string from the user and present markers on map based on that string
    private void searchMarkers(String search) {
        db.collection("markers")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            if (document.getString("title").equals(search)) {
                                LatLng marker = new LatLng(document.getDouble("position.latitude"), document.getDouble("position.longitude"));
                                mMap.addMarker(new MarkerOptions().position(marker).title(document.getString("title")));
                            }

                        }
                    } else {
                        Log.w("TAG", "Error getting documents.", task.getException());
                    }
                });
    }

    // utils
    private void displayToast(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
    }
    // end of GUI functions  ////






    public void onMapLongClick(LatLng latLng) {
        updateUI();

        if (!signedIn) {
            notSignedText = (TextView) findViewById(R.id.NotSignedText);
            notSignedText.setVisibility(View.VISIBLE);
            new Handler().postDelayed(() -> notSignedText.setVisibility(View.INVISIBLE), 3000);
            return;
        }

        if (isWithinRadius(latLng)) {
            Toast.makeText(getApplicationContext(), "You can add a tree only in a radius of 50 meters from your current location", Toast.LENGTH_SHORT).show();
            return;
        }

        //TODO any other touch close them both?

        yes = (Button) findViewById(R.id.wantToAddTreeY);
        no = (Button) findViewById(R.id.wantToAddTreeN);

        yes.setVisibility(View.VISIBLE);
        no.setVisibility(View.VISIBLE);

        yes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                yes.setVisibility(View.INVISIBLE);
                no.setVisibility(View.INVISIBLE);
                addTreeForm(latLng);
            }
        });
        no.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                yes.setVisibility(View.INVISIBLE);
                no.setVisibility(View.INVISIBLE);


            }

        });
        new Handler().postDelayed(() -> yes.setVisibility(View.INVISIBLE), 3000);
        new Handler().postDelayed(() -> no.setVisibility(View.INVISIBLE), 3000);
    }



    // this function gets the marker and returns the tree document in the collections 'trees' the matches the markers key value 'mSnipet'
    private Object getTreeData(Marker marker) {
        db.collection("trees").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        if (document.getId().equals(mSnippet)) {
                            docRef = document.getReference();

                            Log.i("docRef", docRef.toString());
                            break;
                        }
                    }
                } else {
                    Log.d("TAG", "Error getting documents: ", task.getException());
                }
            }
        });
        return docRef;
    }

    private Task<QuerySnapshot> getMarkerSnippet(final Marker marker) {

        return db.collection("markers").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot document : task.getResult()) {

                    if (marker.getPosition().latitude == document.getDouble("position.latitude") &&
                            marker.getPosition().longitude == document.getDouble("position.longitude")) {
                        String cur = (String) document.get("snippet");
                        Log.i("treeid", "cur " + cur + "  " + document.getId());

                        mSnippet = cur;
                        break;

                    }
                }
            } else {
                Log.w("TAG", "Error getting documents.", task.getException());
            }
        });
    }

    public boolean onMarkerClick(final Marker marker) {

//         Retrieve the data from the marker.

        moreDetails = findViewById(R.id.more_details); // The button for more details

        exitDetails = findViewById(R.id.exitDetails);// exit button

        treeDetailsUpdate = findViewById(R.id.treeDetailsUpdate);//textview
        updateLinear = findViewById(R.id.details);// The layout
        //update
        nextDescription = findViewById(R.id.nextDes);//button
        existTreeDescription = findViewById(R.id.treeDescriptions);//textview for descriptions
        existAddTreeDes = findViewById(R.id.addDesExist);//button to add description
        updateTreeCond = findViewById(R.id.addCurrentCond);//button to update condition

        addExtraLinear = findViewById(R.id.addExtraDes);//linear layout to add description
        addExtraEdit = findViewById(R.id.addExtraDesText);//EditView to get the des
        conExtraDes = findViewById(R.id.addExtraDesButton); //button to confirm

        rateTree = findViewById(R.id.treeRate);// rating bar

        getMarkerSnippet(marker); // query to get the tree id
        getTreeData(marker);

        moreDetails = findViewById(R.id.more_details);
        moreDetails.setVisibility(View.VISIBLE);
        //set listener on the more details

        moreDetails.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                moreDetails.setVisibility(View.INVISIBLE);

                //get the tree data
                docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {

                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                Log.d("TAG", "DocumentSnapshot data: " + document.getData());
                                //set the data
                                treeDetailsUpdate.setText(document.getString("name") + "\n" +
                                        document.get("condition"));
                                db.collection("description").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                        if (task.isSuccessful()) {
                                            for (QueryDocumentSnapshot document : task.getResult()) {
                                                if (mSnippet.equals(document.getData().keySet().toArray()[0].toString())) {
                                                    String cur = (String) (document.getData().values().toArray()[0].toString());
                                                    existTreeDescription.setText(cur);
                                                    break;
                                                    // TODO set a global variable to the current description
                                                    // TODO use a counter instead of int to get the next description
                                                }

                                            }
                                        } else {
                                            Log.d("TAG", "Error getting documents: ", task.getException());
                                        }
                                    }
                                });


                                //set the rating using 'rating' field
                                rateTree.setRating(document.getDouble("rating").floatValue());

                                //set the listener on the exit button
                                exitDetails.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        updateLinear.setVisibility(View.INVISIBLE);
                                        exitDetails.setVisibility(View.INVISIBLE);
                                    }
                                });
                                //set the listener on the next description button

                            } else {
                                Log.d("TAG", "No such document");
                            }
                        } else {
                            Log.d("TAG", "get failed with ", task.getException());
                        }
                    }
                });
                updateLinear.setVisibility(View.VISIBLE);
                exitDetails.setVisibility(View.VISIBLE);
            }
        });
        nextDescription.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //existTreeDescription.setText(docRef.getString("description2"));
                nextDescription.setVisibility(View.INVISIBLE);
            }
        });
        //set the listener on the add description button
        existAddTreeDes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addExtraLinear.setVisibility(View.VISIBLE);
                existAddTreeDes.setVisibility(View.INVISIBLE);
                //set the listener on the confirm button
                conExtraDes.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //update the tree
                        docRef.update("description2", addExtraEdit.getText().toString());
                        addExtraLinear.setVisibility(View.INVISIBLE);
                        existAddTreeDes.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
        //set the listener on the update condition button
        updateTreeCond.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //update the tree
                docRef.update("rate", rateTree.getRating());
            }
        });
        rateTree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                treeRating = (int) rateTree.getRating();
                // update the 4th value of the documentReference using the treeUpdate function
                updateTreeComp(docRef, 3, treeRating);
            }
        });
        exitDetails.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateLinear.setVisibility(View.INVISIBLE);
            }
        });//exit more details
        existAddTreeDes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                updateLinear.setVisibility(View.INVISIBLE);
                addExtraLinear.setVisibility(View.VISIBLE);
                addExtraEdit.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void afterTextChanged(Editable editable) {
                        desExtra = editable.toString();
                    }
                });
//                conExtraDes.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View view) {
//
//                        if (!desExtra.toString().equals(" ")) {
//
//                            updateTreeComp(curTree, 2);
//                            addExtraLinear.setVisibility(View.INVISIBLE);
//                        } else {
//                            addExtraEdit.setText("please add description");
//                        }
//                    }
//                });
            }
        });// the layout to add description

        new Handler().postDelayed(() -> moreDetails.setVisibility(View.INVISIBLE), 3000);

        return false;
    }

    public void addTree(LatLng latLng) {
            Map<String, Object> tree = new HashMap<>();
            tree.put("name", addTreeName.toString());
            tree.put("condition", treeCondSts);
            tree.put("rating", 0);
            db.collection("trees").add(tree).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                @Override
                public void onSuccess(DocumentReference documentReference) {
                    Log.d("TAG", "DocumentSnapshot added with ID: " + documentReference.getId());
                    // new marker
                    MarkerOptions marker = new MarkerOptions();
                    marker.position(latLng);
                    marker.snippet(documentReference.getId());
                    marker.title(addTreeName.toString());
                    db.collection("markers").add(marker).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(DocumentReference documentReference) {
                            Log.d("TAG", "DocumentSnapshot added with ID: " + documentReference.getId());
                            addTreeLinear.setVisibility(View.INVISIBLE);
                            updateUI();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w("TAG", "Error adding document", e);
                        }
                    });

                    Map<String, Object> description = new HashMap<>();

                    // concatenate the description with the author name from the user sign in
                    GoogleSignInAccount googleSignInAccount = GoogleSignIn.getLastSignedInAccount(MapsActivity.this);

                    // turn treeDes to string
                    String treeDesString;

                    treeDesString = treeDes.toString() + "\n by: " + googleSignInAccount.getDisplayName();

                    // add an array of descriptions
                    description.put( documentReference.getId(), Arrays.asList(treeDesString));

                    db.collection("description").add(description).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(DocumentReference documentReference) {
                            Log.d("TAG", "DocumentSnapshot added with ID: " + documentReference.getId());
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w("TAG", "Error adding document", e);
                        }
                    });

                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.w("TAG", "Error adding document", e);
                }
            });
        }

    public void addTreeForm(LatLng latLng) {

        //TODO add verify name, description and condition
        addTreeLinear = findViewById(R.id.form);
        addTreeText = findViewById(R.id.treeNameText);
        addTreeEdit = findViewById(R.id.treeNameEdit);
        addDesText = findViewById(R.id.treeDescribeText);
        addDesEdit = findViewById(R.id.treeDescribeEdit);
        treeCond = findViewById(R.id.treeCond);
        exitTreeForm = findViewById(R.id.exitForm);
        confirmTreeForm = findViewById(R.id.confirmTree);
        addTreeLinear.setVisibility(View.VISIBLE);
        addTreeEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {

                addTreeName = addTreeEdit.getText();

            }
        });

        addDesEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                treeDes = addDesEdit.getText();

            }
        });
        treeCond.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                switch (i) {
                    case R.id.winter:
                        treeCondSts = 0;
                        break;
                    case R.id.spring:
                        treeCondSts = 1;
                        break;

                    case R.id.summer:
                        treeCondSts = 2;
                        break;
                    case R.id.autumn:
                        treeCondSts = 3;
                        break;
                }
            }

        });


        exitTreeForm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateUI();
            }
        });
        confirmTreeForm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addTree(latLng);
            }
        });
    }

    private void updateTreeComp(DocumentReference docsi, int field, Object newValue) {
        //this function will update the values of the map in documentReference depending on the field number with the content of newValue

        docsi.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Map<String, Object> tree = document.getData();
                        ArrayList<Object> treeData = (ArrayList<Object>) tree.get("treeData");
                        treeData.set(field, newValue);
                        tree.put("treeData", treeData);
                        docsi.set(tree);
                    }
                }
            }
        });

    }





    // check if given lat lng is within 50m radius of the users current location
    public boolean isWithinRadius(LatLng latLng) {
        Location locationA = new Location("point A");
        locationA.setLatitude(latLng.latitude);
        locationA.setLongitude(latLng.longitude);
        LatLng usersLocation = new LatLng(mMap.getMyLocation().getLatitude(), mMap.getMyLocation().getLongitude());
        Location locationB = new Location("point B");
        locationB.setLatitude(usersLocation.latitude);
        locationB.setLongitude(usersLocation.longitude);

        float distance = locationA.distanceTo(locationB);
        if (distance < 50) {
            return true;
        } else {
            return false;
        }
    }


}




