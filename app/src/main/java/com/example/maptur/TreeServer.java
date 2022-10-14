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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

enum MarkerChoice {ALL_TREES, MINE, RIPE, SPECIFIC}


public class TreeServer {

    // CRUD operations //


    // create //

    // this function creates a new marker in the database, and tree+description corresponding to it
    public static void createTree(LatLng latLng, Map<String, Object> tree, String treeDes, FirebaseFirestore db, FirebaseAuth userName) {

        // add the tree to the database using the tree map data
        db.collection("trees").add(tree).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
            @Override
            public void onSuccess(DocumentReference documentReference) {
                Log.d("TAG", "DocumentSnapshot added with ID: " + documentReference.getId());
                // new marker initialization with the lat, lng and the tree id in the snippet
                MarkerOptions marker = new MarkerOptions();
                marker.position(latLng);
                marker.snippet(documentReference.getId());
                marker.title((String) tree.get("name"));
                // add the marker to the database
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
                // new description map with the treeDes and the tree id as key
                Map<String, Object> description = new HashMap<>();
                description.put("TreeId", documentReference.getId());
                Map<String, String> desCol = new HashMap<>();
                // description in the format Timestamp, user name and the string description
                desCol.put(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()), treeDes);
                description.put("des", desCol);
                // add the description map to the database
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

    // read //

    //method to retrieve tree data from the DB using lat,lng as the key
    //first, retrieves the marker, then uses the foreign key of the tree to retrieve the tree and the description from their considerable tables
    //docRef stores the document reference of the tree and the description

    public static void getTreeData(FirebaseFirestore db, LatLng latLng, ArrayList<Object> docRef, FirebaseAuth mAuth) {

        db.collection("markers").get()
                .addOnCompleteListener(task1 -> {
                            if (task1.isSuccessful()) {
                                for (QueryDocumentSnapshot document1 : task1.getResult()) {
                                    //get the specific Marker by the Lat,Long, in Document1
                                    if (Objects.equals(latLng.latitude, document1.getDouble("position.latitude")) &&
                                            Objects.equals(latLng.longitude, document1.getDouble("position.longitude"))) {
                                        db.collection("trees")
                                                .get()
                .addOnCompleteListener(task2 -> {
                            if (task2.isSuccessful()) {
                                for (QueryDocumentSnapshot document2 : task2.getResult()) {
                                    //get the specific Tree using the snippet filed,  In document2. Saving in docRef[0].
                                    if (Objects.equals(document2.getId(), document1.getString("snippet"))) {
                                        docRef.add(0, document2.getReference());
                                        db.collection("description")
                                                .get()
                .addOnCompleteListener(task3 -> {
                            if (task2.isSuccessful()) {
                                for (QueryDocumentSnapshot document3 : task3.getResult()) {
                                    //get the description entry using the tree ID, in Document 3. saving in docRef[1]
                                    if (document3.getString("TreeId").equals(document2.getId())) {
                                        docRef.add(1, document3.getReference());
                                        addLog(db, mAuth, "pulled tree data" + document1.getId());
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
                        }//end task1
                );//end of 1 q
    }

    // method to retrieve markers based on the given filter
    public static void getMarkers(GoogleMap mMap, FirebaseFirestore db, FirebaseAuth userName, ArrayList<Object> filter) {

        db.collection("markers")
                .get()
                .addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()) {
                        for (QueryDocumentSnapshot document1 : task1.getResult()) {
                            switch ((int) filter.get(0)) {

                                case 0: // all markers
                                    LatLng marker = new LatLng(document1.getDouble("position.latitude"), document1.getDouble("position.longitude"));
                                    mMap.addMarker(new MarkerOptions().position(marker).title(document1.getString("title")));
                                    addLog(db, userName, "Got all markers");
                                    break;

                                case 1: // my markers
                                    // get from 'trees' where document id equals to the document1.snippet
                                    db.collection("trees")
                                            .get()
                                            .addOnCompleteListener(task2 -> {
                                                if (task2.isSuccessful()) {
                                                    for (QueryDocumentSnapshot document2 : task2.getResult()) {
                                                        //Get all the markers where the username corresponds to the creator
                                                        if (Objects.equals(document2.getId(), document1.getString("snippet")) &&
                                                                Objects.equals(document2.getString("creator"), userName.getCurrentUser().getEmail())) {
                                                            LatLng marker1 = new LatLng(document1.getDouble("position.latitude"), document1.getDouble("position.longitude"));
                                                            mMap.addMarker(new MarkerOptions().position(marker1).title(document1.getString("title")));
                                                        }
                                                    }
                                                } else {
                                                    Log.d("TAG", "Error getting documents: ", task2.getException());
                                                }
                                            });
                                    addLog(db, userName, "Got his markers");
                                    break;

                                case 2: // search markers by title
                                    if (document1.getString("title").contains((String) filter.get(1))) {
                                        LatLng marker2 = new LatLng(document1.getDouble("position.latitude"), document1.getDouble("position.longitude"));
                                        mMap.addMarker(new MarkerOptions().position(marker2).title(document1.getString("title")));
                                    }
                                    addLog(db, userName, "Got markers with the title: " + filter.get(1));
                                    break;

                                case 3: // search markers where the filed condition is "Ripe"
                                    db.collection("trees")
                                            .get()
                                            .addOnCompleteListener(task2 -> {
                                                if (task2.isSuccessful()) {
                                                    for (QueryDocumentSnapshot document2 : task2.getResult()) {
                                                        //Get all markers where the tree condition is ripe
                                                        if (Objects.equals(document2.getId(), document1.getString("snippet")) &&
                                                                Objects.equals(document2.getString("condition"), "Ripe")) {
                                                            LatLng marker1 = new LatLng(document1.getDouble("position.latitude"), document1.getDouble("position.longitude"));
                                                            mMap.addMarker(new MarkerOptions().position(marker1).title(document1.getString("title")));
                                                        }
                                                    }
                                                } else {
                                                    Log.d("TAG", "Error getting documents: ", task2.getException());
                                                }
                                            });
                                    addLog(db, userName, "Got markers with the condition: Ripe");
                                    break;
                            }
                        }
                    } else {
                        Log.w("TAG", "Error getting documents.", task1.getException());
                    }
                });

    }

    // update //

    // this function updates the tree and the description in the database
    public static void updateTree(FirebaseFirestore db, DocumentReference docR, int filter, Object obj, FirebaseAuth mAuth) {
        switch (filter) {
            // ALL_TREES
            case 0: // update the rating in the 'tree' collection
                int rating = (int) ((ArrayList<Object>) obj).get(0);
                int numOfRatings = (int) ((ArrayList<Object>) obj).get(1);
                int totalRating = (int) ((ArrayList<Object>) obj).get(2);
                //calculate the new rating
                Log.i("TAG", "updateTree: " + rating + " " + numOfRatings + " " + totalRating);
                int newRating = (totalRating + rating * numOfRatings) / (numOfRatings + 1);
                docR.update("rating", newRating);
                numOfRatings++;
                docR.update("numOfRates", numOfRatings);
                addLog(db, mAuth, "updated tree " + docR.getId() + "rating " + rating +  " .Now its rating is " + newRating);

                break;
            case 1: // update the Condition in the 'tree' collection
                docR.update("condition", obj);
                addLog(db, mAuth, "updated tree " + docR.getId() + "condition " + obj.toString());

                break;
            case 2: // update the description
                docR.update("des", obj);
                addLog(db, mAuth, "updated tree " + docR.getId() + "description " + obj.toString());
                break;
        }
    }

    // delete //

    //delete tree marker and description based on LatLng
    // start by deleting the marker from the 'markers' collection
    // then delete the description from the 'trees' collection
    // and finally delete the description from the 'description' collection
    public static void deleteTree(FirebaseFirestore db, LatLng latLng) {
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


    // logging //
    // this function is a generic utility that adds a log to the 'logs' collection with a timestamp
    public static void addLog(FirebaseFirestore db, FirebaseAuth userName, String log) {


        // if user is null - username = "guest"
        String username = "guest";
        if ( userName != null && userName.getCurrentUser() != null) {
            username = userName.getCurrentUser().getEmail();
        }
        Map<Object, String> dLog = new HashMap<>();
        dLog.put(username, new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()) + " | " + log);
        db.collection("Logs").add(dLog);
    }


}