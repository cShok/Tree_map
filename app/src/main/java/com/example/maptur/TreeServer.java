package com.example.maptur;

import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class TreeServer {

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
}