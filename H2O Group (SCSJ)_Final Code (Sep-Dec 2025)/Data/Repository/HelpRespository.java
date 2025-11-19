package com.name.ccf.Data.Repository;

// --- ⬇️ GEMINI FIX: Added imports for Storage, Uri, UUID ---
import android.net.Uri;
import android.util.Log;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.UUID;
// --- ⬆️ END OF FIX ---

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.name.ccf.Data.Entity.Help; // Make sure this import is correct

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository for handling 'Help' data operations with Firebase.
 * (This file uses the 'HelpRespository' spelling to match your existing file)
 */
public class HelpRespository {

    private final FirebaseFirestore db;
    private final CollectionReference helpRef;
    private final StorageReference storageRef;
    private static final String TAG = "HelpRepository";

    // --- ⬇️ GEMINI FIX: Constructor name MUST match the Class name ---
    public HelpRespository() { // (Was HelpRepository, now HelpRespository)
        db = FirebaseFirestore.getInstance();
        helpRef = db.collection("help_requests"); // Firebase collection
        storageRef = FirebaseStorage.getInstance().getReference();
    }
    // --- ⬆️ END OF FIX ---

    /**
     * Callback interface for when the image upload is complete
     * and the download URL is ready.
     */
    public interface OnImageUrlReadyListener {
        void onUrlReady(String url);
        void onUploadFailed(Exception e);
    }

    /**
     * Uploads an image file to Firebase Storage and returns the public URL.
     * @param imageUri The local 'content://' URI of the file to upload.
     * @param listener The callback to return the new HTTPS URL.
     */
    public void uploadImageAndGetUrl(Uri imageUri, OnImageUrlReadyListener listener) {
        // Create a unique file name (e.g., help_images/some-random-uuid.jpg)
        String uniqueFileName = UUID.randomUUID().toString() + ".jpg";
        StorageReference fileRef = storageRef.child("help_images/" + uniqueFileName);

        // 1. Start the file upload
        fileRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // 2. If upload is successful, get the download URL
                    fileRef.getDownloadUrl()
                            .addOnSuccessListener(url -> {
                                // 3. Return the URL string to the fragment
                                listener.onUrlReady(url.toString());
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to get download URL", e);
                                listener.onUploadFailed(e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Image upload failed", e);
                    listener.onUploadFailed(e);
                });
    }

    /**
     * Uploads a single Help request object to Firestore.
     * @param help The Help object to upload.
     */
    public void uploadHelpRequest(Help help) {
        Map<String, Object> data = new HashMap<>();
        data.put("username", help.username);
        data.put("userid", help.userid);
        data.put("helptext", help.helptext);
        data.put("imageUri", help.imageUri); // This will be the HTTPS URL
        data.put("timestamp", System.currentTimeMillis());

        helpRef.add(data)
                .addOnSuccessListener(doc -> Log.d(TAG, "Help request uploaded: " + doc.getId()))
                .addOnFailureListener(e -> Log.e(TAG, "Help request upload failed", e));
    }

    /**
     * Fetches ALL help requests from Firestore, ordered by most recent.
     * This is typically for an admin to view.
     * @param callback The callback to return the list of help requests.
     */
    public void fetchAllHelpRequests(OnHelpFetched callback) {
        helpRef.orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Help> helpList = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        try {
                            Help help = new Help(
                                    doc.getString("username"),
                                    doc.getString("userid"),
                                    doc.getString("helptext"),
                                    doc.getString("imageUri") // <-- Added this field
                            );
                            helpList.add(help);
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to parse help document: " + doc.getId(), e);
                        }
                    }
                    callback.onFetched(helpList);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Fetch all help requests failed", e);
                    callback.onFetched(new ArrayList<>()); // Return empty list on failure
                });
    }

    /**
     * Callback interface for asynchronous fetch operations.
     */
    public interface OnHelpFetched {
        void onFetched(List<Help> helpList);
    }
}