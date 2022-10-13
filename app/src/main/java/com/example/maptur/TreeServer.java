package com.example.maptur;

import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class TreeServer {

//    1. create
//    2. update
//    3. read
//    4. delete

    //read
    // we need to write a generic function that will receive map, db, user, and filtering type
    // and return a list of trees depending on the filtering type
    // this function will replace getAllMarkers + presentMyMarkers + searchMarkers

    public static void getAllMarkers(GoogleMap mMap, FirebaseFirestore db, FirebaseAuth userName) {

        db.collection("markers")
                .get()
                .addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()) {
                        for (QueryDocumentSnapshot document1 : task1.getResult()) {
                            LatLng marker = new LatLng(document1.getDouble("position.latitude"), document1.getDouble("position.longitude"));
                            mMap.addMarker(new MarkerOptions().position(marker).title(document1.getString("title")));
                        }
                    } else {
                        Log.w("TAG", "Error getting documents.", task1.getException());
                    }
                });



    }

    public static void presentMyMarkers(GoogleMap mMap, FirebaseFirestore db, FirebaseAuth userName) {
        // return all the trees where the 'creator' field is equal to the userName
        db.collection("trees")
                .whereEqualTo("creator", userName.getCurrentUser().getEmail())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            // find the marker where snippet is equal to the tree id and add it to the map
                            db.collection("markers")
                                    .whereEqualTo("snippet", document.getId())
                                    .get()
                                    .addOnCompleteListener(task1 -> {
                                        if (task1.isSuccessful()) {
                                            for (QueryDocumentSnapshot document1 : task1.getResult()) {
                                                LatLng marker = new LatLng(document1.getDouble("position.latitude"), document1.getDouble("position.longitude"));
                                                mMap.addMarker(new MarkerOptions().position(marker).title(document1.getString("title")));
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


    //create
    // The function gets tree details, the marker details, db, and maybe the userName ->
    // createTree(latlng, details, db, auth?)
    public static void addLog(FirebaseFirestore db, FirebaseAuth userName, String log){
        Map<Object,String> dLog = new HashMap<>();
        dLog.put(Objects.requireNonNull(userName.getCurrentUser()).getEmail(),log);
        db.collection("Logs").add(dLog);
    }
    //update
    // The function gets tree details, the marker details, db, and maybe the userName ->
    // createTree(latlng, details, db, auth?)

    //delete
    // The function gets tree details, the marker details, db, and maybe the userName ->
    // deleteTree(latlng, details, db, auth?)

    


}