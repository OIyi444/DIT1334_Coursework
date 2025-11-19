package com.name.ccf.Data.Repository;
import android.net.Uri; // ⬅️ (Add this import)
import android.util.Log;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
// --- ⬇️ GEMINI FIX: Add Firebase Storage imports ---
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
// --- ⬆️ END OF FIX ---
import com.name.ccf.Data.Entity.Feedback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID; // ⬅️ (Add this import)

public class FeedbackRepository {

    private final FirebaseFirestore db;
    private final CollectionReference feedbackRef;
    // --- ⬇️ GEMINI FIX: Add Storage reference ---
    private final StorageReference storageRef;
    // --- ⬆️ END OF FIX ---
    private static final String TAG = "FeedbackRepository";

    public FeedbackRepository() {
        db = FirebaseFirestore.getInstance();
        feedbackRef = db.collection("feedback"); // Firebase collection
        // --- ⬇️ GEMINI FIX: Initialize Storage ---
        // (This points to the root of your Firebase Storage bucket)
        storageRef = FirebaseStorage.getInstance().getReference();
        // --- ⬆️ END OF FIX ---
    }

    // --- ⬇️ GEMINI FIX: Add new interface for callback ---
    /**
     * Callback interface for when the image upload is complete
     * and the download URL is ready.
     */
    public interface OnImageUrlReadyListener {
        void onUrlReady(String url);
        void onUploadFailed(Exception e);
    }
    // --- ⬆️ END OF FIX ---

    // --- ⬇️ GEMINI FIX: Add new method to upload image ---
    /**
     * Uploads an image file to Firebase Storage and returns the public URL.
     * @param imageUri The local 'content://' URI of the file to upload.
     * @param listener The callback to return the new HTTPS URL.
     */
    public void uploadImageAndGetUrl(Uri imageUri, OnImageUrlReadyListener listener) {
        // Create a unique file name (e.g., feedback_images/some-random-uuid.jpg)
        String uniqueFileName = UUID.randomUUID().toString() + ".jpg";
        StorageReference fileRef = storageRef.child("feedback_images/" + uniqueFileName);

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
    // --- ⬆️ END OF FIX ---


    /**
     * Uploads a single Feedback object to Firestore.
     * (This method remains unchanged from your provided code)
     */
    public void uploadFeedback(Feedback feedback) {
        Map<String, Object> data = new HashMap<>();
        data.put("username", feedback.username);
        data.put("dishname", feedback.dishname);
        data.put("rating", feedback.rating);
        data.put("category", feedback.category);
        data.put("tag", feedback.tag);
        data.put("imageUri", feedback.imageUri); // This will now be the HTTPS URL
        data.put("feedbackText", feedback.feedbackText);
        data.put("timestamp", feedback.timestamp);

        feedbackRef.add(data)
                .addOnSuccessListener(doc -> Log.d(TAG, "Feedback (database entry) uploaded: " + doc.getId()))
                .addOnFailureListener(e -> Log.e(TAG, "Feedback (database entry) upload failed", e));
    }

    /**
     * Fetches ALL feedback documents from Firestore, ordered by most recent.
     * (This method remains unchanged from your provided code)
     */
    public void fetchAllFeedback(OnFeedbackFetched callback) {
        feedbackRef.orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Feedback> feedbackList = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Feedback feedback = createFeedbackFromDoc(doc);
                        if (feedback != null) {
                            feedbackList.add(feedback);
                        }
                    }
                    callback.onFetched(feedbackList);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Fetch all feedback failed", e);
                    callback.onFetched(new ArrayList<>()); // Return empty list on failure
                });
    }

    /**
     * [NEW] Efficiently syncs only NEW feedback from other devices.
     * (This method remains unchanged from your provided code)
     */
    public void syncFeedback(long latestLocalTimestamp, OnFeedbackFetched callback) {
        feedbackRef
                .whereGreaterThan("timestamp", latestLocalTimestamp)
                .orderBy("timestamp", Query.Direction.DESCENDING) // Order is still good practice
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Feedback> newFeedbackList = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Feedback feedback = createFeedbackFromDoc(doc);
                        if (feedback != null) {
                            newFeedbackList.add(feedback);
                        }
                    }
                    Log.d(TAG, "Sync successful, found " + newFeedbackList.size() + " new items.");
                    callback.onFetched(newFeedbackList);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Sync feedback failed", e);
                    callback.onFetched(new ArrayList<>()); // Return empty list on failure
                });
    }

    /**
     * Helper function to safely parse a Firestore document into a Feedback object.
     * (This method remains unchanged from your provided code)
     */
    private Feedback createFeedbackFromDoc(QueryDocumentSnapshot doc) {
        try {
            // Use .get() and check for nulls to prevent crashes
            Double ratingDouble = doc.getDouble("rating");
            Long timestampLong = doc.getLong("timestamp");

            // Set defaults if fields are missing
            float rating = (ratingDouble != null) ? ratingDouble.floatValue() : 0.0f;
            long timestamp = (timestampLong != null) ? timestampLong.longValue() : 0L;

            return new Feedback(
                    doc.getString("username"),
                    doc.getString("dishname"),
                    rating,
                    doc.getString("category"),
                    doc.getString("tag"),
                    doc.getString("imageUri"), // This will now be the HTTPS URL
                    doc.getString("feedbackText"),
                    timestamp
            );
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse Feedback document: " + doc.getId(), e);
            return null; // Return null if parsing fails
        }
    }

    // Callback interface
    // (This interface remains unchanged from your provided code)
    public interface OnFeedbackFetched {
        void onFetched(List<Feedback> feedbackList);
    }
}