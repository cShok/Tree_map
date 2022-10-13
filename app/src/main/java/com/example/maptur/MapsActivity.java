package com.example.maptur;


import android.Manifest;
import android.annotation.SuppressLint;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
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
import com.google.common.net.InternetDomainName;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.auth.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private FirebaseAuth mAuth;

    FirebaseFirestore db;
    private boolean signedIn;
    private GoogleSignInClient googleSignInClient;
    private SignInButton btSignIn;
    private Button btLogout;

    private Button moreDetails;

    private LinearLayout addTreeLinear, updateLinear, addExtraLinear, updateTreeConditionLinear;
    private TextView addTreeText, addDesText, existTreeDescription, treeDetailsUpdate;
    private EditText addTreeEdit, addDesEdit, addExtraEdit;
    private Button exitTreeForm, confirmTreeForm, exitDetails, nextDescription, updateTreeCond, existAddTreeDes, conExtraDes, confirmCondButton;
    private RadioGroup treeCond, treeCondUpdate;
    Editable addTreeName, treeDes;
    private RatingBar rateTree;
    String treeCondSts = "";
    private Map<String, Object> markersMap = new HashMap<>();
    private Map<String, Object> gTreeData = new HashMap<>();

    private String mSnippet, desExtra = " ", treeType = "";
    DocumentReference docRef, desID;
    private int iDes = 0;
    private View locationButton;
    private String [] sts = {"Fall", "Blooming", "Flowers", "Fruits"};

    private Collection<Object> chosenTreeDes = new ArrayList<>();

    //general functions
    @Override
    public void onStart() {
        super.onStart();
        Log.i("onStart", "onStart called");
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
        DrawerLayout drawerLayout = findViewById(R.id.autumnUpdate);
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



                int id = item.getItemId();
                drawerLayout.closeDrawer(GravityCompat.START);
                removeMarkers();
                ArrayList <Object> filters = new ArrayList<>();
                switch (id) {
                    case R.id.nav_all_trees:
                        filters.add(0, 0);
                        Toast.makeText(getApplicationContext(), "All trees", Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.nav_my_trees:
                        Toast.makeText(MapsActivity.this, "Showing your trees", Toast.LENGTH_SHORT).show();
                        filters.add(0, 1);
                        break;
                    case R.id.fruit_now:
                        Toast.makeText(MapsActivity.this, "Tree that have fruit on them", Toast.LENGTH_SHORT).show();
                        filters.add(0, 3);
                        break;
                    //present markers based on users input
                    case R.id.tree_type:
                        getFilterType();
                        filters.add(0, 2);
                        filters.add(1, treeType);
                        break;
                    default:
                        return true;
                }
                Log.i("filters", filters.toString());
                TreeServer.getMarkers(mMap, db, mAuth, filters);
                return true;
            }
        });
    }

    private void getFilterType() {

        EditText input = new EditText(MapsActivity.this);
        input.setHint("Apple, Pear, Cherry, etc.");
        EditText finalInput = input;
        new AlertDialog.Builder(MapsActivity.this)
                .setTitle("Enter tree type")
                .setView(input)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        treeType = finalInput.getText().toString();
                        return;
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
        Toast.makeText(MapsActivity.this, "Showing the trees you chose", Toast.LENGTH_SHORT).show();
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
        updateUI();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i("onActivityResult", "onActivityResult called");
        // Check condition
        if (requestCode == 100) {
            Task<GoogleSignInAccount> signInAccountTask = GoogleSignIn
                    .getSignedInAccountFromIntent(data);

            if (signInAccountTask.isSuccessful()) {

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
                                            displayToast("You have successfully signed in");
                                            addUserToDatabase();
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

    private void addUserToDatabase() {
        // if user doesn't exist in database in the collection "users" then add him
        DocumentReference docRef = db.collection("users").document(mAuth.getCurrentUser().getUid());
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                       //add timestamp to value 'lastLogin'
                        Map<String, Object> lastLogin = new HashMap<>();
                        lastLogin.put("last Login", FieldValue.serverTimestamp());
                        db.collection("users").document(mAuth.getCurrentUser().getUid()).update(lastLogin);
                    } else {
                        Log.d("TAG", "No such document");
                        Map<String, Object> user = new HashMap<>();
                        user.put("name", mAuth.getCurrentUser().getDisplayName());
                        user.put("email", mAuth.getCurrentUser().getEmail());
                        user.put("last Login", FieldValue.serverTimestamp());
                        db.collection("users").document(mAuth.getCurrentUser().getUid())
                                .set(user)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Log.d("TAG", "DocumentSnapshot successfully written!");
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.w("TAG", "Error writing document", e);
                                    }
                                });
                    }
                } else {
                    Log.d("TAG", "get failed with ", task.getException());
                }
            }
        });

    }

    //map and marker functions
    public void onMapLongClick(LatLng latLng) {
        updateUI();

        if (!signedIn) {
            Toast.makeText(getApplicationContext(), "Please sign in to add a tree", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isWithinRadius(latLng)) {
            Toast.makeText(getApplicationContext(), "You need to be closer to the tree you want to add", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(MapsActivity.this)
                .setTitle("Would you like to add a tree here?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        addTreeForm(latLng);
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }


    public boolean onMarkerClick(final Marker marker) {

        moreDetails = findViewById(R.id.more_details); // The button for more details

        exitDetails = findViewById(R.id.exitDetails);// exit button

        treeDetailsUpdate = findViewById(R.id.treeDetailsUpdate);//textview
        updateLinear = findViewById(R.id.details);// The layout
        nextDescription = findViewById(R.id.nextDes);//button
        existTreeDescription = findViewById(R.id.treeDescriptions);//textview for descriptions
        existAddTreeDes = findViewById(R.id.addDesExist);//button to add description
        updateTreeCond = findViewById(R.id.addCurrentCond);//button to update condition
        addExtraLinear = findViewById(R.id.addExtraDes);//linear layout to add description
        addExtraEdit = findViewById(R.id.addExtraDesText);//EditView to get the des
        conExtraDes = findViewById(R.id.addExtraDesButton); //button to confirm

        treeCondUpdate = findViewById(R.id.conditionButtons);//radioGroup for updating condition
        updateTreeConditionLinear = findViewById(R.id.updateTreeConditionLayout);//linear layout for updating condition
        confirmCondButton = findViewById(R.id.confirmConditionButton);//button to confirm condition

        rateTree = findViewById(R.id.treeRate);// rating bar

        getMarkerSnippet(marker); // query to get the tree id
        getTreeData(marker);

        moreDetails = findViewById(R.id.more_details);
        moreDetails.setVisibility(View.VISIBLE);

        //set listener on the more details

        moreDetails.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Handler().postDelayed(() ->   docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {

                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                //set the data

                                treeDetailsUpdate.setText(document.getString("name") + "\n" +
                                         document.getString("condition") + "\n" );
                                db.collection("description").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                        if (task.isSuccessful()) {
                                            for (QueryDocumentSnapshot document : task.getResult()) {
                                                if (mSnippet.equals(document.getData().keySet().toArray()[0].toString())) {
                                                    desID = document.getReference();
                                                    // save to value cur only the first value in the array
                                                    Collection<Object> cur = (document.getData().values());
                                                    // cast cur[0] to array
                                                    ArrayList<String> curArr = (ArrayList<String>) cur.toArray()[0];
                                                    // set desToPresent to the first value in the curArr array
                                                    String desToPresent = curArr.get(0).toString();
                                                    existTreeDescription.setText(desToPresent);
                                                    break;
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

                            } else {
                                Log.d("TAG", "No such document");
                            }
                        } else {
                            Log.d("TAG", "get failed with ", task.getException());
                        }
                    }
                }), 3000);


                //get the tree data

                updateLinear.setVisibility(View.VISIBLE);
                exitDetails.setVisibility(View.VISIBLE);
            }
        });
        nextDescription.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // loop through all the values in the array, when get to the last value, set the text to the first value
                db.collection("description").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                if (mSnippet.equals(document.getData().keySet().toArray()[0].toString())) {
                                    desID = document.getReference();
                                    // save to value cur only the first value in the array
                                    Collection<Object> cur = (document.getData().values());
                                    // cast cur[0] to array
                                    ArrayList<String> curArr = (ArrayList<String>) cur.toArray()[0];
                                    // set desToPresent to the first value in the curArr array
                                    String desToPresent = curArr.get(iDes);
                                    if(iDes == cur.size()) iDes =0;
                                    else iDes++;
                                    existTreeDescription.setText(desToPresent);
                                    break;
                                }
                            }
                        } else {
                            Log.d("TAG", "Error getting documents: ", task.getException());
                        }
                    }
                });
            }
        });
        existAddTreeDes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addExtraLinear.setVisibility(View.VISIBLE);
                updateLinear.setVisibility(View.INVISIBLE);
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
                        chosenTreeDes.add(desExtra);
                        db.collection("description").document(desID.getId()).update(mSnippet, chosenTreeDes);
                        addExtraLinear.setVisibility(View.INVISIBLE);
                        existAddTreeDes.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
        updateTreeCond.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // if user isn't signed in - show a toast 'you must be signed in to update the tree' and return
                if (signedIn == false) {
                    Toast.makeText(MapsActivity.this, "You must be signed in to update the tree", Toast.LENGTH_SHORT).show();
                    return;
                }
                updateTreeConditionLinear.setVisibility(View.VISIBLE);
                updateLinear.setVisibility(View.INVISIBLE);
                treeCondUpdate.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(RadioGroup radioGroup, int i) {
                        switch (i) {
                            case R.id.autumnButton:
                                treeCondSts = "Autumn";
                                break;
                            case R.id.bloomButton:
                                treeCondSts = "Bloom";
                                break;
                            case R.id.flowersButton:
                                treeCondSts = "Flowers";
                                break;
                            case R.id.ripeButton:
                                treeCondSts = "Ripe";
                                break;
                        }
                    }
                });
                confirmCondButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        //update the condition field of the docRef
                        db.collection("trees").document(mSnippet).update("condition", treeCondSts);
                        updateTreeConditionLinear.setVisibility(View.INVISIBLE);
                    }
                });
            }
        });
        //set the listener on the update the rating value based on the rating bar
        rateTree.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float v, boolean b) {
                if (signedIn == false) {
                    Toast.makeText(MapsActivity.this, "You must be signed in to update the tree rating", Toast.LENGTH_SHORT).show();
                    return;
                }
                else {
                    //update the rating field of the docRef
                    db.collection("trees").document(mSnippet).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()) {
                                DocumentSnapshot document = task.getResult();
                                if (document.exists()) {
                                    int ourNumOfRates = document.getLong("numOfRates").intValue();
                                    // update the numOfRates field
                                    db.collection("trees").document(mSnippet).update("numOfRates", ourNumOfRates + 1);
                                    // update the rating field
                                    Log.i("TAG", "full: " + ((Math.round(v) + document.getLong("rating")) / (ourNumOfRates + 1)));
                                    Log.i("TAG", "math roundV: " + Math.round(v));
                                    Log.i("TAG", "ourNumOfRates: " + ourNumOfRates);
                                    if (Math.round(v) == 0) {
                                        db.collection("trees").document(mSnippet).update("rating", (1 + document.getLong("rating")) / (ourNumOfRates + 1));
                                    } else {
                                        db.collection("trees").document(mSnippet).update("rating", (Math.round(v) + document.getLong("rating")) / (ourNumOfRates + 1));
                                    }

                                    //TODO - fix calculation
                                } else {
                                    Log.d("TAG", "No such document");
                                }
                            } else {
                                Log.d("TAG", "get failed with ", task.getException());
                            }
                        }
                    });
                }
            }
        });
        exitDetails.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateLinear.setVisibility(View.INVISIBLE);
            }
        });

        new Handler().postDelayed(() -> moreDetails.setVisibility(View.INVISIBLE), 3000);

        return false;

    }
    private void presentMarkers() {
        TreeServer.getAllMarkers(mMap, db, mAuth);
    }

    private void myMarkers() {
        TreeServer.presentMyMarkers(mMap, db, mAuth);
    }
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

    // tree functions
    public void addTree(LatLng latLng) {
        Map<String, Object> tree = new HashMap<>();
        tree.put("name", addTreeName.toString());
        tree.put("condition", treeCondSts);
        tree.put("rating", 3);
        tree.put("numOfRates", 0);
        tree.put("creator", FirebaseAuth.getInstance().getCurrentUser().getEmail());

        TreeServer.createTree(latLng, tree, treeDes.toString(), db, mAuth);

        addTreeLinear.setVisibility(View.INVISIBLE);
        Toast.makeText(MapsActivity.this, "Adding your tree...", Toast.LENGTH_SHORT).show();
        new Handler().postDelayed(() -> updateUI(), 3000);

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
                        treeCondSts = "Autumn";
                        break;
                    case R.id.spring:
                        treeCondSts = "Flowers";
                        break;

                    case R.id.summer:
                        treeCondSts =  "Ripe";
                        break;
                    case R.id.autumn:
                        treeCondSts = "Bloom";
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

    // these functions will be moved to TreeServer
    private Object getTreeData(Marker marker) {
        db.collection("trees").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        if (document.getId().equals(mSnippet)) {
                            if(document.exists()) {
                                docRef = document.getReference();

                                Log.i("docRef", docRef.toString());
                                break;
                            }
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

    // utils //
    // update the UI to the default state
    private void updateUI() {
        // Initialize google sign in account
        GoogleSignInAccount googleSignInAccount = GoogleSignIn.getLastSignedInAccount(MapsActivity.this);
        //
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
        // initialize array list object with 0,1
        ArrayList<Object> treeData = new ArrayList<>();
        treeData.add(0);

        TreeServer.getMarkers(mMap, db, mAuth, treeData);

    }
    // check if user is within the radius of the tree
    public boolean isWithinRadius(LatLng treePos) {
        Location locationA = new Location("point A");
        locationA.setLatitude(treePos.latitude);
        locationA.setLongitude(treePos.longitude);
        LatLng usersLocation = new LatLng(mMap.getMyLocation().getLatitude(), mMap.getMyLocation().getLongitude());
        Location locationB = new Location("point B");
        locationB.setLatitude(usersLocation.latitude);
        locationB.setLongitude(usersLocation.longitude);

        float distance = locationA.distanceTo(locationB);
        if (distance > 50) {
            return true;
        } else {
            return false;
        }
    }
    // remove all markers from the map
    private void removeMarkers() {
        //remove all markers
        mMap.clear();
    }
    // display toast message
    private void displayToast(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
    }


}
