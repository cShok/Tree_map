# Fruit Tree Community Map

We built an Android app that lets users find and map fruit trees around them.

This the final assignment in the course [Advanced Java Programing Workshop](https://www.openu.ac.il/courses/20503.htm) at The Open University of Israel and Guided by Tamar Bnaya.

## The App

The main functionality of the app is adding and editing fruit trees.

The user can add a new tree by clicking on the map, and then edit the tree's details. The user can also search for trees by their name, and filter them by their type.

Only signed in users can add and edit trees. The app uses Firebase Authentication to sign in users and you need a Google account to sign in.

Users can add trees only if they are in the radius of 50 meters from the tree's location.

Our thanks to [Guy Griv](https://www.linkedin.com/in/guy-griv-a1076289/) for his advice.



### Tech Stack

Although Android apps tend to be written in Kotlin in our days, we wrote it in **Java** due to the subject of the course.

We used **Google Sign-in** as our login system, and used the **FireStore** database to store the data.

**Google Maps API** is obviously used to display the map, and **Android Studio** was used as the IDE.

We used various tutorials and documentation to help us with the project, mainly from [Firebase](https://firebase.google.com/docs/android/setup) and [Google Maps API](https://developers.google.com/maps/documentation/android-sdk/start).


#### To run locally:

- add `MAPS_API_KEY` to `local.properties`
- add your SHA to firebase ([explanation](https://stackoverflow.com/questions/67460387/how-to-get-sha1-code-in-new-version-of-android-studio-4-2))
