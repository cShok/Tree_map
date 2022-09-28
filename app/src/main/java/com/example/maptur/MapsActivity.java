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

import java.util.ArrayList;
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

    private TextView notSignedText, notInRadiusText;
    private Button yes;
    private Button no;
    private Button moreDetails;


    private LinearLayout addTreeLinear, updateLinear, addExtraLinear;
    private TextView addTreeText, addDesText, existTreeDescription, treeDetailsUpdate;
    private EditText addTreeEdit, addDesEdit, addExtraEdit;
    private Button exitTreeForm, confirmTreeForm, exitDetails, nextDescription, updateTreeCond, existAddTreeDes, conExtraDes;
    private RadioGroup treeCond;
    Editable name, treeDes;
    private RatingBar rateTree;
    int treeCondSts, treeRating;
    private Map<String, Object> markersMap = new HashMap<>();
    private Map<String, Object> gTreeData = new HashMap<>();
    Map<String, Object> desList = new HashMap<>();

    ArrayList<Object> treeD =new ArrayList<>();
    private String mSnippet, desExtra = " ";
    DocumentReference refi;
    private int sameM, nums;
    private int iDes = 0;
    private View locationButton;





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

    private void removeMarkers() {
        //remove all markers
        mMap.clear();
    }

    private void updateUI() {
        // Initialize google sign in account
        GoogleSignInAccount googleSignInAccount = GoogleSignIn.getLastSignedInAccount(MapsActivity.this);
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

    private void presentMarkers() {
        // pull all items in collection "markers" from firestore db
        db.collection("marker")
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
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

    private void displayToast(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStart() {
        super.onStart();
        updateUI();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

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

    }


    //foreign key of trees

    private Task<QuerySnapshot> getTreeData(final Marker marker) {
        return db.collection("trees")
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            if (Objects.equals(mSnippet, document.getId())) {

                                gTreeData = document.getData();

                            }

                        }
                    } else {
                        Log.w("TAG", "Error getting documents.", task.getException());
                    }
                });
    }

    private Task<QuerySnapshot> getMarkerSnippet(final Marker marker) {

        return db.collection("marker").get().addOnCompleteListener(task -> {
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

        moreDetails = findViewById(R.id.more_details);
        updateLinear = findViewById(R.id.details);
        exitDetails = findViewById(R.id.exitDetails);

        treeDetailsUpdate = findViewById(R.id.treeDetalisUpdate);//textview

        nextDescription = findViewById(R.id.nextDes);//button
        existTreeDescription = findViewById(R.id.treeDescriptions);//textview
        existAddTreeDes = findViewById(R.id.addDesExist);//button
        updateTreeCond = findViewById(R.id.addCurrentCond);//button

        addExtraLinear = findViewById(R.id.addExtraDes);//linear
        addExtraEdit = findViewById(R.id.addExtraDesText);//EditView
        conExtraDes = findViewById(R.id.addExtraDesButton); //button


        rateTree = findViewById(R.id.treeRate);

        Task<QuerySnapshot> curMarker = getMarkerSnippet(marker);
        Task<QuerySnapshot> curTree = getTreeData(marker);
        Object [] treeArray  = new Object[4];

        ArrayList<String> desc = new ArrayList<>();


        moreDetails = findViewById(R.id.more_details);
        moreDetails.setVisibility(View.VISIBLE);



        curMarker.addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                Log.i("marker listner", curMarker.getResult().toString());
            }
        });
        curTree.addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                moreDetails.setVisibility(View.VISIBLE);
//                for( DocumentSnapshot onm :  curTree.getResult().getDocuments()){
//                       onm.getReference().u;
//                }
//                Log.i("tree listner", ttt.getId());
                Log.i("tree listner", curTree.getResult().toString());
                Log.i("tree listner", curTree.getResult().getClass().toString());

            }
        });
//
//// ...
//        WriteResult result = future.get();
//        System.out.println("Write result: " + result);

        rateTree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Task<QuerySnapshot> newnew = curTree;
//                updateTreeComp(newnew, 3);
                treeRating = (int) rateTree.getRating();
            }
        });
        exitDetails.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateLinear.setVisibility(View.INVISIBLE);
            }
        });
        moreDetails.setOnClickListener(new View.OnClickListener() {


            @Override
            public void onClick(View view) {

                List<DocumentSnapshot> r = curTree.getResult().getDocuments();
                DocumentReference ref = r.get(0).getReference();
                        ref.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @SuppressLint("SetTextI18n")
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                DocumentSnapshot ds = task.getResult();
                                for (Object o : ds.getData().values()) {

                                    treeD = ((ArrayList<Object>) o);
//                                 extraDetails();
//                                   for(Map.Entry<String,Object> ent : ((Map<String,Object>)treeD.get(2)) )
//                                   desc.add(ent.toString() + "\n BY:~" + ent.getKey().toString());
                                    Log.i("Hihi", treeD.toString());
                                }
//                 existTreeDescription.setText(desc.get(iDes / desc.size()));
                                treeDetailsUpdate.setText(treeD.get().toString() +'\n' +
                                       + '\n' + treeD.get(1).toString() + '\n' + treeD.get(3).toString());
                                updateLinear.setVisibility(View.VISIBLE);


                            }

                            }
                        );

//
            }
        });
        nextDescription.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                iDes++;
                existTreeDescription.setText(desc.get(iDes / desc.size()));
            }
        });
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
                conExtraDes.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        if (!desExtra.toString().equals(" ")) {

//                            updateTreeComp(curTree, 2);
                            addExtraLinear.setVisibility(View.INVISIBLE);
                        } else {
                            addExtraEdit.setText("pleas add description pleas");
                        }
                    }
                });
            }
        });




        new Handler().postDelayed(() -> moreDetails.setVisibility(View.INVISIBLE), 3000);

        return false;
    }

    public void addTree(LatLng latLng) {


        updateUI();

        MarkerOptions newMarker = new MarkerOptions();
        ArrayList<Object> treeData = new ArrayList<>();
        Map<String, String> descriptionMap = new HashMap<>();
        Map<String, Object> treeMap = new HashMap<>();

        newMarker.position(latLng);

        descriptionMap.put(googleSignInClient.toString(), treeDes.toString());
        treeData.add(name.toString());//0
        treeData.add(treeCondSts);//1
        treeData.add(descriptionMap);//2
        treeData.add(0); //rating//3
        Double c1, c2;
        c1 = latLng.latitude;
        c2 = latLng.longitude;


        treeMap.put(c1 + "+" + c2, treeData);

        Task<DocumentReference> h = db.collection("trees").add(treeMap);
        h.addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
            @Override
            public void onComplete(@NonNull Task<DocumentReference> task) {

                newMarker.title(name.toString());
                newMarker.snippet(h.getResult().getId());
                db.collection("marker").add(newMarker);
                presentMarkers();
            }
        });
    }

    public void addTreeForm(LatLng latLng) {


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

                name = addTreeEdit.getText();

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
                    case R.id.atumn:
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

//    private void updateTreeComp(Task<QuerySnapshot> docsi, int filed) {
//
//        QuerySnapshot docs = docsi.getResult();
//        docs.getDocuments().
//        docsi.addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
//
//            @Override
//            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
//                if (task.isSuccessful()) {
//                    DocumentSnapshot refi = task.getResult();
//                    ArrayList<Object> nk = new ArrayList<>();
//                    nk = (ArrayList<Object>) refi.getData().values();
//                    switch (filed) {
//                        case (1): //condtion
//                            nk.set(1, 0);
//                            break;
//                        case (2): //description
//                            Map<String, Object> newdesList = (Map<String, Object>) (nk.get(2));
//                            newdesList.put(googleSignInClient.toString() + "+" + sameM++, desExtra.toString());
//                            nk.set(2, newdesList);
//                            break;
//                        case (3): //rating
//                            nk.set(3, treeRating);
//                            break;
//                    }
//                    refi.getReference().update(nk.get(0).toString(), nk);
//                }
//            }
//
//
//
//        });
//    }

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

    public void extraDetails() {
    }
}





