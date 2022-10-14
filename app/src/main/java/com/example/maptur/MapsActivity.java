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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private FirebaseAuth mAuth;

    private FirebaseFirestore db;

    private int numOfRatings = 1, totalRating = 3, gDes = 0;
    private boolean signedIn;
    private String desExtra = " ", treeType = "", treeCondSts = "";
    private GoogleSignInClient googleSignInClient;
    private SignInButton btSignIn;
    private LinearLayout addTreeLinear, updateLinear, addExtraLinear, updateTreeConditionLinear;
    private TextView addTreeText, addDesText, existTreeDescription, treeDetailsUpdate;
    private EditText addTreeEdit, addDesEdit, addExtraEdit;
    private Button exitTreeForm, confirmTreeForm, exitDetails, nextDescription, updateTreeCond,
            existAddTreeDes, conExtraDes, confirmCondButton, delTreeButton, btLogout, moreDetails;
    private RadioGroup treeCond, treeCondUpdate;
    private Editable addTreeName, treeDes;
    private RatingBar rateTree;
    private View locationButton;
    private Map<String, String> chosenTreeDes = new HashMap<>();


    //Boot and activities management functions//


    @Override
    public void onStart() {
        super.onStart();
    }

    // Initializes FireBase Client, Google Sign In Client, and Google Map Activity
    // Also, sets up the navigation drawer and top-bar, and the sign in button
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

        // Initialize Firebase Database
        db = FirebaseFirestore.getInstance();


        // Initialize sign in options
        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(
                GoogleSignInOptions.DEFAULT_SIGN_IN
        ).requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        btSignIn = findViewById(R.id.bt_sign_in);
        btLogout = findViewById(R.id.bt_logout);

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
                    // all trees
                    case R.id.nav_all_trees:
                        filters.add(0, 0);
                        Toast.makeText(getApplicationContext(), "All trees", Toast.LENGTH_SHORT).show();
                        break;
                    // my trees
                    case R.id.nav_my_trees:
                        Toast.makeText(MapsActivity.this, "Showing your trees", Toast.LENGTH_SHORT).show();
                        filters.add(0, 1);
                        break;
                    // ripe trees
                    case R.id.fruit_now:
                        Toast.makeText(MapsActivity.this, "Tree that have fruit on them", Toast.LENGTH_SHORT).show();
                        filters.add(0, 3);
                        break;
                    // type of tree
                    case R.id.tree_type:
                        getFilterType();
                        filters.add(0, 2);
                        filters.add(1, treeType);
                        break;
                    default:
                        return true;
                }
                // get the trees from the database by calling the generic function
                TreeServer.getMarkers(mMap, db, mAuth, filters);
                return true;
            }
        });
    }

    // get the map when ready and ask for location permissions,
    @Override
    public void onMapReady(GoogleMap googleMap) {

        // initialize the map in the activity
        mMap = googleMap;

        //location permission - if not granted, ask for it
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        LatLng Jerusalem = new LatLng(31.7807688, 35.21472);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(Jerusalem));
        mMap.moveCamera(CameraUpdateFactory.zoomTo(50000));

        // set listener for when the user clicks on the marker, to extract and update the tree data.
        mMap.setOnMarkerClickListener(this::onMarkerClick);

        // set listener for when the user clicks on the map, to add a new tree
        mMap.setOnMapLongClickListener(this::onMapLongClick);//long push

        // get your maps fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        // Extract My Location View from maps fragment
        locationButton = mapFragment.getView().findViewById(0x2);

        findViewById(R.id.ic_location).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mMap != null) {
                    if (locationButton != null)
                        locationButton.callOnClick();
                }
            }
        });
        // get all the markers from the database and refresh the map
        updateUI();
    }


    // implementing Google sign in using google authentication with Firebase
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
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
                                            //add the user to the database or update the user login
                                            TreeServer.createUser(db, mAuth);
                                            // global var to control the user activity who demands authentication
                                            signedIn = true;
                                            onStart();
                                            updateUI();
                                        } else {
                                            // When task is unsuccessful - display Toast
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


    //map and marker functions//

    // when user long clicks on the map, add a new tree to the database, using user input and location as
    // parameter for the tree marker and description
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

    // when user clicks on a marker, show the tree data and offer to update the tree data
    public boolean onMarkerClick(final Marker marker) {

        moreDetails = findViewById(R.id.more_details); // The button for more details

        exitDetails = findViewById(R.id.exitDetails);// exit button
        delTreeButton = findViewById(R.id.deleteTree);// delete button
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

//        getMarkerSnippet(marker); // query to get the tree id
//        getTreeData(marker);
        ArrayList<Object> docRefList = new ArrayList<>();
        LatLng treeLocation = new LatLng(marker.getPosition().latitude, marker.getPosition().longitude);
        TreeServer.getTreeData(db, treeLocation, docRefList, mAuth);

        moreDetails = findViewById(R.id.more_details);
        rateTree.setIsIndicator(true);
        moreDetails.setVisibility(View.VISIBLE);
        exitDetails.setVisibility(View.VISIBLE);
        //set listener on the more details

        delTreeButton.setVisibility(View.VISIBLE);
        delTreeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(MapsActivity.this)
                        .setTitle("Are you sure you want to delete this tree?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                delTreeButton.setVisibility(View.INVISIBLE);
                                TreeServer.deleteTree(db, marker.getPosition());
                                moreDetails.setVisibility(View.INVISIBLE);
                                //hold the function for a 3000 ms till updateUI is ready
                                //delete the marker from the map
                                marker.remove();
                                updateUI();

                            }
                        })
                        .setNegativeButton("No", null)
                        .show();
            }
        });
        moreDetails.setOnClickListener(new View.OnClickListener() {


            @Override
            public void onClick(View view) {
                //unlock rateTree  rating bar

                new Handler().postDelayed(() -> docRefList.get(0), 3000);
                new Handler().postDelayed(() -> docRefList.get(1), 3000);
                DocumentReference docRefTree = (DocumentReference) docRefList.get(0);
                DocumentReference docRefDes = (DocumentReference) docRefList.get(1);
                Toast.makeText(getApplicationContext(), "Loading Tree Data...", Toast.LENGTH_SHORT).show();
                new Handler().postDelayed(() ->   docRefTree.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {

                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                //set the data

                                treeDetailsUpdate.setText(document.getString("name") + "\n" +
                                        document.getString("condition") + "\n");
                                //set the rating using 'rating' field
                                rateTree.setRating(document.getDouble("rating").floatValue());
                                rateTree.setIsIndicator(false);
                                numOfRatings = document.getDouble("numOfRates").intValue();
                                totalRating = document.getDouble("rating").intValue();
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
                new Handler().postDelayed(() ->   docRefDes.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {

                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                //set the data
                               chosenTreeDes =  (HashMap<String,String>)(document.get("des"));
                                for (Object str : chosenTreeDes.entrySet().toArray()) {
                                    existTreeDescription.setText(str.toString());
                                    break;
                                }
                            } else {
                                Log.d("TAG", "No such document");
                            }
                        } else {
                            Log.d("TAG", "get failed with ", task.getException());
                        }
                    }
                }), 3000);
                new Handler().postDelayed(() -> updateLinear.setVisibility(View.VISIBLE), 3000);
            }
        });
        nextDescription.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // loop through all the values in the array, when get to the last value, set the text to the first value
                Object [] desArray = chosenTreeDes.entrySet().toArray();
                int maxDes = chosenTreeDes.entrySet().toArray().length;
                if(gDes >= maxDes) gDes =0;
                existTreeDescription.setText(desArray[gDes].toString());
                gDes++;
        }});

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

                        chosenTreeDes.put(new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date()) ,desExtra);
                        TreeServer.updateTree(db , (DocumentReference) docRefList.get(1), 2, chosenTreeDes, mAuth);

//                            chosenTreeDes.add(desExtra);
//                        db.collection("description").document(desID.getId()).update(mSnippet, chosenTreeDes);
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
                        //use upadate method in TresServer to update the tree condition
                        TreeServer.updateTree(db, (DocumentReference) docRefList.get(0), 1, treeCondSts, mAuth);
                        //update the condition field of the docRef
//                        db.collection("trees").document(mSnippet).update("condition", treeCondSts);
                        updateTreeConditionLinear.setVisibility(View.INVISIBLE);
                    }
                });
            }
        });
//        set the listener on the update the rating value based on the rating bar
        rateTree.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float v, boolean b) {

                //Update only available for signed in users
                if (signedIn == false) {
                    Toast.makeText(MapsActivity.this, "You must be signed in to update the tree rating", Toast.LENGTH_SHORT).show();
                    return;
                }
                else {
                    ArrayList<Object>  ratingAtrr= new ArrayList<>();
                    ratingAtrr.add((int)v); // user's rating
                    ratingAtrr.add(1, numOfRatings);// number of ratings by all users
                    ratingAtrr.add(2, totalRating);// total rating score
                    // Update the rating in the server
                    TreeServer.updateTree(db, (DocumentReference) docRefList.get(0), 0, ratingAtrr, mAuth);
                    //lock the rating bar
                    rateTree.setIsIndicator(true);
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
        new Handler().postDelayed(() -> delTreeButton.setVisibility(View.INVISIBLE), 3000);
        return false;

    }


    // tree's functions //

    // gets the full information to add, arrange in JSON like format and add to the server
    public void addTree(LatLng latLng) {
        Map<String, Object> tree = new HashMap<>();
        tree.put("name", addTreeName.toString());
        tree.put("condition", treeCondSts);
        tree.put("rating", 3);
        tree.put("numOfRates", 1);
        tree.put("creator", FirebaseAuth.getInstance().getCurrentUser().getEmail());

        TreeServer.createTree(latLng, tree, treeDes.toString(), db, mAuth);

        addTreeLinear.setVisibility(View.INVISIBLE);
        Toast.makeText(MapsActivity.this, "Adding your tree...", Toast.LENGTH_SHORT).show();
        new Handler().postDelayed(() -> updateUI(), 3000);

    }

    //get the tree information from user and call addTree function
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

    //get the user input of the type of tree and call server with only trees of that type.
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


    // utils //

    // update the UI to the default state (all markers)
    private void updateUI() {

        // Initialize google sign in account
        GoogleSignInAccount googleSignInAccount = GoogleSignIn.getLastSignedInAccount(MapsActivity.this);

        // GUI to add trees (with long-click)
        addTreeLinear = findViewById(R.id.form);
        addTreeLinear.setVisibility(View.INVISIBLE);

        // Check condition
        if (googleSignInAccount != null) {

            // global variables initialization
            signedIn = true;

            // When google sign in account is not null
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
            btLogout.setVisibility(View.GONE);
            btSignIn.setVisibility(View.VISIBLE);
        }
        // initialize array list object with 0,1
        ArrayList<Object> treeData = new ArrayList<>();
        treeData.add(0);
        //gel all trees from server
        TreeServer.getMarkers(mMap, db, mAuth, treeData);

    }

    // check if user is within the radius of the tree, to add and update trees.
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
