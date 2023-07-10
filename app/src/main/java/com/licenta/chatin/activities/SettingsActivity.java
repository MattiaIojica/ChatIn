package com.licenta.chatin.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.licenta.chatin.R;
import com.licenta.chatin.databinding.ActivitySettingsBinding;
import com.licenta.chatin.fragment.ChangeNameDialogFragment;
import com.licenta.chatin.fragment.ChangePasswordDialogFragment;
import com.licenta.chatin.utilities.Constants;
import com.licenta.chatin.utilities.PreferenceManager;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;


public class SettingsActivity extends BaseActivity implements ChangePasswordDialogFragment.OnConfirmPasswordListener, ChangeNameDialogFragment.OnChangeNameListener {

    private ActivitySettingsBinding binding;
    private FirebaseFirestore database;
    private PreferenceManager preferenceManager;
    private String encodedImage;
    private DocumentReference documentReference;
    private Boolean deleteAcc = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferenceManager = new PreferenceManager(getApplicationContext());
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        database = FirebaseFirestore.getInstance();
        documentReference = database.collection(Constants.KEY_COLLECTIONS_USERS)
                .document(preferenceManager.getString(Constants.KEY_USER_ID));
        setContentView(binding.getRoot());
        setListeners();
        loadUserDetails();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void loadUserDetails() {
        binding.textName.setText(preferenceManager.getString(Constants.KEY_NAME));
        Bitmap bitmap = getBitmapFromEncodedString(preferenceManager.getString(Constants.KEY_IMAGE));
        binding.imageProfile.setImageBitmap(bitmap);
        binding.switchActiveStatus.setChecked(preferenceManager.getBoolean(Constants.KEY_ACTIVE_STATUS));
    }

    private static Bitmap getBitmapFromEncodedString(String encodedImage) {
        if (encodedImage != null) {
            byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } else {
            return null;
        }
    }

    private void setListeners() {
        binding.imageBack.setOnClickListener(v -> {
            onBackPressed();
            overridePendingTransition(0, R.anim.fade_out);
        });

        binding.imageSignOut.setOnClickListener(v -> signOut());

        binding.buttonChangeImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(intent);
        });

        binding.buttonChangeName.setOnClickListener(v -> {
            ChangeNameDialogFragment dialogFragment = new ChangeNameDialogFragment();
            dialogFragment.show(getSupportFragmentManager(), "ChangePasswordDialogFragment");
        });

        binding.buttonChangePassword.setOnClickListener(v -> {
            ChangePasswordDialogFragment dialogFragment = new ChangePasswordDialogFragment();
            dialogFragment.show(getSupportFragmentManager(), "ChangePasswordDialogFragment");
        });

        binding.switchActiveStatus.setOnCheckedChangeListener((buttonView, isChecked) -> {
            onActiveStatusChanged(isChecked);
        });

        binding.buttonDeleteAccount.setOnClickListener((v -> showDeleteAccountDialog()));
    }

    @Override
    public void onConfirmPassword(String currentPassword, String newPassword, String confirmPassword) {
        String userId = preferenceManager.getString(Constants.KEY_USER_ID);

        // Retrieve the user document from the database
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference userRef = database.collection(Constants.KEY_COLLECTIONS_USERS).document(userId);
        userRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    String storedPassword = documentSnapshot.getString(Constants.KEY_PASSWORD);

                    if (!encryptStringWithSHA256(currentPassword).equals(storedPassword)) {
                        showToast("Current password is incorrect");
                        return;
                    }

                    if (newPassword.equals(confirmPassword) && verifyPassword(newPassword)) {
                        // Passwords match, proceed with changing the password
                        userRef.update(Constants.KEY_PASSWORD, encryptStringWithSHA256(newPassword))
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        showToast("Password changed successfully");
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        showToast("Failed to change password. Please try again.");
                                    }
                                });
                    } else {
                        showToast("New password and confirm password do not match");
                    }
                } else {
                    showToast("User document not found");
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                showToast("Failed to retrieve user document. Please try again.");
            }
        });
    }

    @Override
    public void onChangeName(String newName, String currentPassword) {
        String userId = preferenceManager.getString(Constants.KEY_USER_ID);

        // Retrieve the user document from the database
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference userRef = database.collection(Constants.KEY_COLLECTIONS_USERS).document(userId);
        userRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    String storedPassword = documentSnapshot.getString(Constants.KEY_PASSWORD);

                    if (!encryptStringWithSHA256(currentPassword).equals(storedPassword)) {
                        showToast("Current password is incorrect");
                        return;
                    }

                        // Passwords match, proceed with changing the password
                        userRef.update(Constants.KEY_NAME, newName)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        preferenceManager.putString(Constants.KEY_NAME, newName);
                                        binding.textName.setText(preferenceManager.getString(Constants.KEY_NAME));
                                        updateConversations(newName);
                                        showToast("Name changed successfully");
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        showToast("Failed to change name. Please try again.");
                                    }
                                });
                    }
                }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                showToast("Failed to retrieve user document. Please try again.");
            }
        });
    }

    public void onActiveStatusChanged(boolean isChecked) {
        if(isChecked) {
            preferenceManager.putBoolean(Constants.KEY_ACTIVE_STATUS, true);
            documentReference.update(Constants.KEY_ACTIVE_STATUS, true);
        }
        else {
            preferenceManager.putBoolean(Constants.KEY_ACTIVE_STATUS, false);
            documentReference.update(Constants.KEY_ACTIVE_STATUS, false);
        }
    }


    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private String encodeImage(Bitmap bitmap) {
        int quality = 50; // Set the desired image quality here
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            binding.imageProfile.setImageBitmap(bitmap);
                            encodedImage = encodeImage(bitmap);
                            preferenceManager.putString(Constants.KEY_IMAGE, encodedImage);
                            updateDatabase(preferenceManager.getString(Constants.KEY_USER_ID));
                            showToast("Image Updated");
                        } catch (FileNotFoundException e) {
                            showToast("Action Failed");
                            e.printStackTrace();
                        }
                    }
                }
            }
    );

    private void updateDatabase(String currentUserID) {
        DocumentReference userRef = database.collection(Constants.KEY_COLLECTIONS_USERS).document(currentUserID);
        CollectionReference conversationsRef = database.collection(Constants.KEY_COLLECTION_CONVERSATION);

        // Update user image
        userRef.update(Constants.KEY_IMAGE, encodedImage)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // User image update successful
                        // Handle any additional logic here
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // User image update failed
                        // Handle any error or exception here
                    }
                });

        // Update sender image in conversations
        conversationsRef
                .whereEqualTo(Constants.KEY_SENDER_ID, currentUserID)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        String documentId = documentSnapshot.getId();
                        DocumentReference conversationRef = conversationsRef.document(documentId);
                        conversationRef.update(Constants.KEY_SENDER_IMAGE, encodedImage)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        // Sender image update successful for the specific conversation
                                        // Handle any additional logic here
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Sender image update failed for the specific conversation
                                        // Handle any error or exception here
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    // Error occurred while retrieving conversations for the sender
                    // Handle any error or exception here
                });

        // Update receiver image in conversations
        conversationsRef
                .whereEqualTo(Constants.KEY_RECEIVER_ID, currentUserID)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        String documentId = documentSnapshot.getId();
                        DocumentReference conversationRef = conversationsRef.document(documentId);
                        conversationRef.update(Constants.KEY_RECEIVER_IMAGE, encodedImage)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        // Receiver image update successful for the specific conversation
                                        // Handle any additional logic here
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Receiver image update failed for the specific conversation
                                        // Handle any error or exception here
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    // Error occurred while retrieving conversations for the receiver
                    // Handle any error or exception here
                });
    }

    private void signOut() {
        showToast("Signig Out...");
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference =
                database.collection(Constants.KEY_COLLECTIONS_USERS).document(
                        preferenceManager.getString(Constants.KEY_USER_ID)
                );
        HashMap<String, Object> updates = new HashMap<>();
        updates.put(Constants.KEY_FCM_TOKEN, FieldValue.delete());
        documentReference.update(updates)
                .addOnSuccessListener(unused -> {
                    preferenceManager.clear();
                    startActivity(new Intent(getApplicationContext(), SignInActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> showToast("Unable to Sign Out"));
    }

    public String encryptStringWithSHA256(String value) {
        try {
            // Create an instance of MessageDigest with SHA-256 algorithm
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

            // Convert the input value to bytes using UTF-8 encoding
            byte[] inputBytes = value.getBytes(StandardCharsets.UTF_8);

            // Compute the hash value of the input bytes
            byte[] hashedBytes = messageDigest.digest(inputBytes);

            // Convert the hashed bytes to a hexadecimal string representation
            StringBuilder hexString = new StringBuilder();
            for (byte hashedByte : hashedBytes) {
                String hex = Integer.toHexString(0xff & hashedByte);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            // Return the hexadecimal string representation of the hashed value
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void updateConversations(String newName){
        CollectionReference conversationsRef = database.collection(Constants.KEY_COLLECTION_CONVERSATION);

        conversationsRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    DocumentReference docRef = conversationsRef.document(document.getId());

                    if(document.getString(Constants.KEY_SENDER_ID).equals(preferenceManager.getString(Constants.KEY_USER_ID))){
                        docRef.update(Constants.KEY_SENDER_NAME, newName);
                    }
                    else
                        if (document.getString(Constants.KEY_RECEIVER_ID).equals(preferenceManager.getString(Constants.KEY_USER_ID))){
                            docRef.update(Constants.KEY_RECEIVER_NAME, newName);
                    }

                }
            }
        });
    }

    private void showDeleteAccountDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Account");
        builder.setMessage("Are you sure you want to delete your account?");
        builder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteAcc = true;
                deleteAccount();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void deleteAccount() {
        updateConversations("Account Deleted");

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.deleted_image);
        binding.imageProfile.setImageBitmap(bitmap);
        encodedImage = encodeImage(bitmap);
        preferenceManager.putString(Constants.KEY_IMAGE, encodedImage);
        updateDatabase(preferenceManager.getString(Constants.KEY_USER_ID));

        documentReference.update(Constants.KEY_NAME, "Deleted User");
        documentReference.update(Constants.KEY_EMAIL, "deletedUser");

        deleteAcc = false;

        signOut();
    }

    private boolean verifyPassword(String password) {
        // Check length
        if (password.length() < 8 || password.length() > 16) {
            return false;
        }

        // Check for at least one uppercase letter, lowercase letter, and digit
        boolean hasUppercase = false;
        boolean hasLowercase = false;
        boolean hasDigit = false;

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) {
                hasUppercase = true;
            } else if (Character.isLowerCase(c)) {
                hasLowercase = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            }
        }

        if (!hasUppercase || !hasLowercase || !hasDigit) {
            return false;
        }

        // Check for any special characters
        String specialChars = "~`!@#$%^&*()-_=+[{]}\\|;:'\",<.>/?";
        for (char c : password.toCharArray()) {
            if (specialChars.contains(String.valueOf(c))) {
                return true;
            }
        }

        return false;
    }

}
