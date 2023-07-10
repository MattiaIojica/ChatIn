package com.licenta.chatin.activities;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatImageView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.licenta.chatin.R;
import com.licenta.chatin.databinding.ActivityUserInfoBinding;
import com.licenta.chatin.models.User;
import com.licenta.chatin.utilities.Constants;
import com.licenta.chatin.utilities.PreferenceManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class UserInfoActivity extends BaseActivity {

    private ActivityUserInfoBinding binding;
    private FirebaseFirestore database;
    private User receiverUser;
    private Boolean isReceiverAvailable = false;
    private PreferenceManager preferenceManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferenceManager = new PreferenceManager(getApplicationContext());
        binding = ActivityUserInfoBinding.inflate(getLayoutInflater());
        database = FirebaseFirestore.getInstance();
        setContentView(binding.getRoot());
        setListeners();
        setButton();
        loadReceiverDetails();
    }

    @Override
    protected void onResume() {
        super.onResume();
        listenAvailabilityOfReceiver();
    }


    private void loadReceiverDetails() {
        receiverUser = (User) getIntent().getSerializableExtra(Constants.KEY_USER);
        binding.textName.setText(receiverUser.getName());
        binding.textEmail.setText(receiverUser.getEmail());
        Bitmap bitmap = getBitmapFromEncodedString(receiverUser.getImage());
        binding.imageProfile.setImageBitmap(bitmap);
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
        binding.buttonBlockUser.setOnClickListener(v -> blockUser(receiverUser.getId()));
        binding.buttonCopyMail.setOnClickListener(v -> copyToClipboard());


        binding.imageProfile.setOnClickListener(v -> {
            updatePreferences(receiverUser.getImage());
            Intent intent = new Intent(getApplicationContext(), FullscreenImageViewerActivity.class);
            startActivity(intent);
        });
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    @SuppressLint("SetTextI18n")
    private void blockUser(String userIdToBlock) {
        String currentUserID = preferenceManager.getString(Constants.KEY_USER_ID);

        // Retrieve the current user document
        DocumentReference userRef = database.collection(Constants.KEY_COLLECTIONS_USERS).document(currentUserID);

        userRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    User currentUser = documentSnapshot.toObject(User.class);

                    assert currentUser != null;
                    List<String> blockedUsers = currentUser.getBlockedUsers();

                    if (blockedUsers != null && blockedUsers.contains(userIdToBlock)) {
                        blockedUsers.remove(userIdToBlock);
                    } else {
                        if (blockedUsers == null) {
                            blockedUsers = new ArrayList<>();
                        }
                        blockedUsers.add(userIdToBlock);
                    }

                    currentUser.setBlockedUsers(blockedUsers);

                    List<String> finalBlockedUsers = blockedUsers;
                    MaterialButton buttonBlockUser = findViewById(R.id.buttonBlockUser);
                    userRef.update(Constants.KEY_BLOCKED_USERS, finalBlockedUsers)
                            .addOnSuccessListener(aVoid -> {
                                if (finalBlockedUsers.contains(userIdToBlock)) {
                                    showToast("User blocked");
                                    buttonBlockUser.setText("Unblock");
                                } else {
                                    showToast("User unblocked");
                                    buttonBlockUser.setText("Block");
                                }
                            })
                            .addOnFailureListener(exception -> {
                                showToast(exception.getMessage());
                            });
                })
                .addOnFailureListener(exception -> {
                    showToast(exception.getMessage());
                });
    }

    private void listenAvailabilityOfReceiver() {
        database.collection(Constants.KEY_COLLECTIONS_USERS).document(
                receiverUser.getId()
        ).addSnapshotListener(UserInfoActivity.this, (value, error) -> {
            if (error != null) {
                return;
            }
            if (value != null) {
                if (value.getLong(Constants.KEY_AVAILABILITY) != null) {
                    int availability = Objects.requireNonNull(
                            value.getLong(Constants.KEY_AVAILABILITY)
                    ).intValue();

                    isReceiverAvailable = availability == 1;
                }
            }
            if (isReceiverAvailable  && value.getBoolean(Constants.KEY_ACTIVE_STATUS)) {
                binding.textAvailability.setVisibility(View.VISIBLE);
            } else {
                binding.textAvailability.setVisibility(View.GONE);
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void setButton() {
        String currentUserID = preferenceManager.getString(Constants.KEY_USER_ID);

        // Retrieve the current user document
        DocumentReference userRef = database.collection(Constants.KEY_COLLECTIONS_USERS).document(currentUserID);

        userRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    User currentUser = documentSnapshot.toObject(User.class);

                    assert currentUser != null;
                    List<String> blockedUsers = currentUser.getBlockedUsers();

                    MaterialButton buttonBlockUser = findViewById(R.id.buttonBlockUser);

                    if (blockedUsers != null && blockedUsers.contains(receiverUser.getId())) {
                        buttonBlockUser.setText("Unblock");
                    } else if (blockedUsers == null || !blockedUsers.contains(receiverUser.getId())) {
                        buttonBlockUser.setText("Block");
                    }
                });
    }

    private void copyToClipboard() {
        String email = receiverUser.getEmail();

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Email", email);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(getApplicationContext(), "Email copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    private void updatePreferences(String encodedImage){
        preferenceManager.putString(Constants.KEY_FULLSCREEN_IMAGE, encodedImage);
    }

}
