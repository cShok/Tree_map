package com.example.maptur;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class TreeServer {
    static boolean found = false;
 public DocumentReference gDoc;
//    1. create
//    2. update
//    3. read
//    4. delete

    //read
    // we need to write a generic function that will receive map, db, user, and filtering type
    // and return a list of trees depending on the filtering type
    // this function will replace getAllMarkers + presentMyMarkers + searchMarkers

    public static void getTreeData(GoogleMap mMap, FirebaseFirestore db, FirebaseAuth userName, LatLng latLng, ArrayList<Object> docr){
        db.collection("markers").get()
                .addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()) {
                        for (QueryDocumentSnapshot document1 : task1.getResult()) {
                            if(Objects.equals(latLng.latitude, document1.getDouble("position.latitude")) &&
                                    Objects.equals(latLng.longitude, document1.getDouble("position.longitude"))){

                                db.collection("trees")
                                        .get()
                                        .addOnCompleteListener(task2 -> {
                                            if (task2.isSuccessful()) {
                                                for (QueryDocumentSnapshot document2 : task2.getResult()) {
                                                    if (Objects.equals(document2.getId(), document1.getString("snippet"))) {
                                                        docr.add(0, document2.getReference());
                                                        db.collection("description")
                                                                .get()
                                                                .addOnCompleteListener(task3 -> {
                                                                    if (task2.isSuccessful()) {
                                                                        for (QueryDocumentSnapshot document3 : task3.getResult()) {
                                                                            if (document3.getString("TreeId").equals(document2.getId())) {
                                                                                docr.add(1, document3.getReference());
                                                                                Log.i("docr5", docr.toString());
                                                                                return;
                                                                            }
                                                                        }

                                                                    } else {
                                                                        Log.d("TAG", "Error getting documents: ", task2.getException());
                                                                    }

                                                                });
                                                        break;

                                                    }
                                                }

                                            } else {
                                                Log.d("TAG", "Error getting documents: ", task2.getException());
                                            }
                                        });
                            }

                        }
                    }
                }
                );
    }
    public static void getMarkers (GoogleMap mMap, FirebaseFirestore db, FirebaseAuth userName, ArrayList<Object> filter) {
        db.collection("markers")
                .get()
                .addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()) {
                        for (QueryDocumentSnapshot document1 : task1.getResult()) {
                            switch  ((int) filter.get(0)) {
                                case 0: // all markers
                                    LatLng marker = new LatLng(document1.getDouble("position.latitude"), document1.getDouble("position.longitude"));
                                    mMap.addMarker(new MarkerOptions().position(marker).title(document1.getString("title")));
                                    break;
                                case 1: // my markers
                                    // get from 'trees' where document id equals to the doucumnet1.snippet
                                    db.collection("trees")
                                            .get()
                                            .addOnCompleteListener(task2 -> {
                                                if (task2.isSuccessful()) {
                                                    for (QueryDocumentSnapshot document2 : task2.getResult()) {
                                                        if (Objects.equals(document2.getId(), document1.getString("snippet")) && Objects.equals(document2.getString("creator"), userName.getCurrentUser().getEmail())) {
                                                                LatLng marker1 = new LatLng(document1.getDouble("position.latitude"), document1.getDouble("position.longitude"));
                                                                mMap.addMarker(new MarkerOptions().position(marker1).title(document1.getString("title")));

                                                        }
                                                    }

                                                } else {
                                                    Log.d("TAG", "Error getting documents: ", task2.getException());
                                                }
                                            });
                                    break;
                                case 2: // search markers by title
                                    if (document1.getString("title").contains((String) filter.get(1))) {
                                        LatLng marker2 = new LatLng(document1.getDouble("position.latitude"), document1.getDouble("position.longitude"));
                                        mMap.addMarker(new MarkerOptions().position(marker2).title(document1.getString("title")));
                                    }
                                    break;
                                case 3: // search markers where the filed condition is "Ripe"
                                    db.collection("trees")
                                            .get()
                                            .addOnCompleteListener(task2 -> {
                                                if (task2.isSuccessful()) {
                                                    for (QueryDocumentSnapshot document2 : task2.getResult()) {
                                                        if (Objects.equals(document2.getId(), document1.getString("snippet")) && Objects.equals(document2.getString("condition"), "Ripe")) {
                                                            LatLng marker1 = new LatLng(document1.getDouble("position.latitude"), document1.getDouble("position.longitude"));
                                                            mMap.addMarker(new MarkerOptions().position(marker1).title(document1.getString("title")));

                                                        }
                                                    }

                                                } else {
                                                    Log.d("TAG", "Error getting documents: ", task2.getException());
                                                }
                                            });
                                    break;

                            }
                        }
                    } else {
                        Log.w("TAG", "Error getting documents.", task1.getException());
                    }
                });

        }

//    public static void getMarkerDetails () {
//             return specific tree?


    //create
    // The function gets tree details, the marker details, db, and maybe the userName ->
    // createTree(latlng, details, db, auth?)


    // create a function that get map, db and optional to get a string
    // and return a list of trees that are in the map
    // this function will replace getAllMarkers + presentMyMarkers + searchMarkers

    //update
    // The function gets tree details, the marker details, db, and maybe the userName ->
    // createTree(latlng, details, db, auth?)
    public static void createTree(LatLng latLng, Map<String, Object> tree, String treeDes, FirebaseFirestore db, FirebaseAuth userName) {
        db.collection("trees").add(tree).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
            @Override
            public void onSuccess(DocumentReference documentReference) {
                Log.d("TAG", "DocumentSnapshot added with ID: " + documentReference.getId());
                // new marker
                MarkerOptions marker = new MarkerOptions();
                marker.position(latLng);
                marker.snippet(documentReference.getId());
                marker.title((String) tree.get("name"));

                db.collection("markers").add(marker).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
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

                Map<String, Object> description = new HashMap<>();

                // add an array of descriptions

                description.put( "TreeId", documentReference.getId());
                Map<String,String> desCol = new HashMap<>();
                desCol.put(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()), treeDes);
                description.put( "des",desCol);

                db.collection("description").add(description).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        addLog(db, userName, "Added a new Tree: " + tree.get("name"));
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

    public static void updateTree(FirebaseFirestore db ,DocumentReference docR, int filter, Object obj){
        switch (filter){
            case 0: // update the rating in the 'tree' collection
                int rating = (int) ((ArrayList<Object>)obj).get(0);
                int numOfRatings = (int) ((ArrayList<Object>)obj).get(1);;
                int totalRating = (int) ((ArrayList<Object>)obj).get(2);
                //calculate the new rating
                Log.i("TAG", "updateTree: " + rating + " " + numOfRatings + " " + totalRating);
                int newRating = (int)(totalRating + rating *numOfRatings) / (numOfRatings + 1) ;
                docR.update("rating",  newRating);
                numOfRatings++;
                docR.update("numOfRates", numOfRatings);
                Log.i("TAG", "updateTreeAfter: " + rating + " " + numOfRatings + " " + totalRating +" " + newRating);

                break;
            case 1: // update the Condition in the 'tree' collection
                docR.update("condition", (String)obj);

                break;
            case 2: // update the description
                docR.update("des", obj);
                break;
        }
    }
    public static void addLog(FirebaseFirestore db, FirebaseAuth userName, String log){
        Map<Object,String> dLog = new HashMap<>();
        dLog.put(userName.getCurrentUser().getEmail(), new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()) + " | " + log);
        db.collection("Logs").add(dLog);
    }

    //delete tree marker and description based on LatLng
    public static void deleteTree(FirebaseFirestore db, LatLng latLng){
        db.collection("markers")
                .whereEqualTo("position.latitude", latLng.latitude)
                .whereEqualTo("position.longitude", latLng.longitude)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            db.collection("markers").document(document.getId()).delete();
                            db.collection("trees").document(document.getString("snippet")).delete();
                            db.collection("description").whereEqualTo("TreeId", document.getString("snippet")).get().addOnCompleteListener(task1 -> {
                                if (task1.isSuccessful()) {
                                    for (QueryDocumentSnapshot document1 : task1.getResult()) {
                                        db.collection("description").document(document1.getId()).delete();
                                    }
                                } else {
                                    Log.w("TAG", "Error getting documents.", task1.getException());
                                }
                            });
                        }
                    } else {
                        Log.w("TAG", "Error getting documents.", task.getException());
                    }
                });
    }
}