package com.name.ccf.Data.Repository;

import android.util.Log;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.name.ccf.Data.Entity.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserRepository {

    private final FirebaseFirestore db;
    private final CollectionReference userRef;

    public UserRepository() {
        db = FirebaseFirestore.getInstance();
        userRef = db.collection("users"); // Firebase users collection
    }

    /**
     * Uploads or updates a single user's data to Firestore.
     * Note: The 'password' field in the passed User object MUST be the hashed password.
     * @param user The User entity containing the hashed password.
     */
    public void uploadUser(User user) {
        if (user == null) return;

        Map<String, Object> data = new HashMap<>();
        data.put("username", user.username);
        data.put("userid", user.userid);
        data.put("userType", user.usertype);
        // Upload the hashed password for device sync
        data.put("passwordHash", user.password);

        // Using 'userid' as the document ID is more stable as it's typically unique and constant
        userRef.document(user.userid)
                .set(data)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "User uploaded: " + user.username))
                .addOnFailureListener(e -> Log.e("Firestore", "Upload failed: " + user.username, e));
    }


    /**
     * [Recommended] Uploads a list of users using a WriteBatch for better performance.
     * @param users A list of User entities, each containing a hashed password.
     */
    public void uploadUserList(List<User> users) {
        if (users == null || users.isEmpty()) return;

        WriteBatch batch = db.batch();

        for (User user : users) {
            Map<String, Object> data = new HashMap<>();
            data.put("username", user.username);
            data.put("userid", user.userid);
            data.put("userType", user.usertype);
            // Upload the hashed password
            data.put("passwordHash", user.password);

            // Use 'userid' as the document ID
            DocumentReference docRef = userRef.document(user.userid);
            batch.set(docRef, data);
        }

        // Commit all operations at once
        batch.commit()
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Users batch upload successful."))
                .addOnFailureListener(e -> Log.e("Firestore", "Users batch upload failed", e));
    }

    /**
     * Fetches user data from Firebase (including the password hash).
     * @param callback The callback interface
     */
    public void fetchUsers(OnUsersFetched callback) {
        userRef.get()
                .addOnSuccessListener(querySnapshot -> {
                    List<User> userList = new ArrayList<>();
                    for (var doc : querySnapshot) {

                        // ✅ Bug Fix: Corrected the order of usertype and userid
                        User user = new User(
                                doc.getString("username"),
                                doc.getString("passwordHash"), // Download the hash from Firestore
                                doc.getString("userType"),     // Corrected
                                doc.getString("userid")        // Corrected
                        );
                        userList.add(user);
                    }
                    callback.onFetched(userList);
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Fetch failed", e));
    }

    // Callback interface
    public interface OnUsersFetched {
        void onFetched(List<User> userList);
    }
}