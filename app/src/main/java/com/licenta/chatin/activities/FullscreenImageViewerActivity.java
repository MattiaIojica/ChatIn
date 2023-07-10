package com.licenta.chatin.activities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.licenta.chatin.R;
import com.licenta.chatin.databinding.ActivityFullscreenImageViewerBinding;
import com.licenta.chatin.utilities.Constants;
import com.licenta.chatin.utilities.PreferenceManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FullscreenImageViewerActivity extends AppCompatActivity {

    private ImageView imageView;
    private ActivityFullscreenImageViewerBinding binding;
    private PreferenceManager preferenceManager;
    private String encodedImage = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferenceManager = new PreferenceManager(getApplicationContext());
        binding = ActivityFullscreenImageViewerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setListeners();
        loadImage();
    }


    private void setListeners(){
        binding.imageBack.setOnClickListener(v -> {
            onBackPressed();
            overridePendingTransition(0, R.anim.fade_out);
        });

        binding.imageSave.setOnClickListener(v -> saveImageToGallery());
    }

    private void loadImage(){
        encodedImage = preferenceManager.getString(Constants.KEY_FULLSCREEN_IMAGE);
        Bitmap bitmap = getBitmapFromEncodedString(encodedImage);
        binding.imageViewFullscreen.setImageBitmap(bitmap);
    }

    private Bitmap getBitmapFromEncodedString(String encodedImage) {
        if (encodedImage != null) {
            byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } else {
            return null;
        }
    }

    private void saveImageToGallery() {
        Bitmap bitmap = getBitmapFromEncodedString(encodedImage);

        // Get the external storage directory
        File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        // Create a unique filename for the image
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String filename = "IMG_" + timestamp + ".jpg";

        // Create the file object
        File file = new File(directory, filename);

        try {
            // Create the output stream
            OutputStream outputStream = new FileOutputStream(file);

            // Compress the bitmap to JPEG format and write it to the output stream
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);

            // Flush and close the output stream
            outputStream.flush();
            outputStream.close();

            // Notify the MediaScanner about the new image
            MediaScannerConnection.scanFile(this, new String[]{file.getAbsolutePath()}, null, null);

            Toast.makeText(this, "Image Saved", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
        }
    }


}
