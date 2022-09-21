package com.example.maptur;

import android.content.Intent;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
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

    private LinearLayout addTreeLinear;
    private TextView addTreeText, addDesText;
    private EditText addTreeEdit,addDesEdit ;
    private Button exitTreeForm, confirmTreeForm;
    private RadioGroup treeCond;
    Editable name, treeDes;
    int treeCondSts, formSts, snip;
    private Map<String,Object> markersMap = new HashMap<>();

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

                int id = item.getItemId();
                drawerLayout.closeDrawer(GravityCompat.START);
                switch (id)
                {
                    case R.id.synch:
                        Toast.makeText(MapsActivity.this, "Synch is Clicked",Toast.LENGTH_SHORT).show();break;
                    case R.id.nav_login:
                        Toast.makeText(MapsActivity.this, "Login is Clicked",Toast.LENGTH_SHORT).show();break;
                    default:
                        return true;
                }
                return true;
            }
        });
        updateUI();
        presentMarkers();
    }

    private void updateUI() {
        // Initialize google sign in account
        GoogleSignInAccount googleSignInAccount=GoogleSignIn.getLastSignedInAccount(MapsActivity.this);
        addTreeLinear = findViewById(R.id.form);
        addTreeLinear.setVisibility(View.INVISIBLE);
        // Check condition
        if(googleSignInAccount!=null)
        {
            signedIn = true;
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

    private void presentMarkers(){
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
                //String s="Google sign in successful";
                // Display Toast
                //displayToast(s);
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
                                            signedIn = true;
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
        updateUI();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        LatLng Jerusalem = new LatLng(31.7683, 35.2137);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(Jerusalem));
        mMap.moveCamera(CameraUpdateFactory.zoomTo(10));
        mMap.setOnMarkerClickListener(this::onMarkerClick); //marker pushed
        mMap.setOnMapLongClickListener(this::onMapLongClick);//long push
    }

    public void onMapLongClick(LatLng latLng) {
        /* need to check if sign in, if not, do nothing @eKurer*/
        // check if in a public space
        // pop the quiz question, first, do you wanna add tree...
        // add a listener or once retrieve the data
        updateUI();

        if (!signedIn) { //pop-up/ sign in please?
            notSignedText = (TextView) findViewById(R.id.NotSignedText);

            notSignedText.setVisibility(View.VISIBLE);
            new Handler().postDelayed(() ->  notSignedText.setVisibility(View.INVISIBLE), 3000);
            return;
        }

        //button do you want to add tree?
        //#TODO any other touch close them both?

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

    public boolean onMarkerClick(final Marker marker) {

//         Retrieve the data from the marker.

        Log.i("#######", marker.toString() );
        ArrayList<Object> treeData = new ArrayList<>();
        Task<QuerySnapshot> docRef = db.collection("trees")
        .get().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.i("@@@@@", marker.getSnippet()+ "  " + document.getId());
                                if (Objects.equals(marker.getTitle(), document.getId())) {
                                        ArrayList<Object> b = (ArrayList<Object>) document.getData().values().toArray()[0] ;

                                        Log.i("^^^^^^^^^",   b.get(0) + b.get(1).toString() + "\n");
//                                    Toast.makeText(this,
//                                            b.get(1).toString(),
//                                            Toast.LENGTH_SHORT).show();
                                    break;

                            } else{


                                Log.w("TAG", "Error getting documents." , task.getException());
                            }


                            }}});
        return false;
    }

 public void addTree(LatLng latLng)  {


        updateUI();

        MarkerOptions newMarker = new MarkerOptions();
        ArrayList<Object> treeData = new ArrayList<>();
        Map<String, String> descriptionMap = new HashMap<>();
        Map<String, Object> treeMap = new HashMap<>();

        newMarker.position(latLng);



        descriptionMap.put(googleSignInClient.toString(),  treeDes.toString());
        treeData.add(name.toString());//0
        treeData.add(treeCondSts);//1
        treeData.add(descriptionMap);//2
        treeData.add(0); //rating//3
        Double c1,c2;
        c1 = latLng.latitude; c2 = latLng.longitude;
        treeMap.put( c1.toString() + "+" + c2.toString() , treeData);
         Task<DocumentReference> h = db.collection("trees").add(treeMap);
         h.addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
             @Override
             public void onComplete(@NonNull Task<DocumentReference> task) {

                 newMarker.title(h.getResult().getId());
                 newMarker.snippet(h.getResult().getId());
                 db.collection("marker").add(newMarker);
                 presentMarkers();
             }
         });





    }

    public void addTreeForm(LatLng latLng)  {


        addTreeLinear = findViewById(R.id.form);
        addTreeText = findViewById(R.id.treeNameText);
        addTreeEdit = findViewById(R.id.treeNameEdit);
        addDesText = findViewById(R.id.treeDescribeText);
        addDesEdit = findViewById(R.id.treeDescribeEdit);
        treeCond = findViewById(R.id.treeCondition);
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
                    switch(i) {
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
}
