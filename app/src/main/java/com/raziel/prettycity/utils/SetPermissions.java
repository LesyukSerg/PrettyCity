package com.raziel.prettycity.utils;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class SetPermissions {

    public static final int REQUEST_MEDIA_IMAGES = 101;
    public static final int REQUEST_FINE_LOCATION = 102;

    private Activity activity;

    public SetPermissions(Activity activity) {
        this.activity = activity;
    }

    public void requestPermissionsInSequence() {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.READ_MEDIA_IMAGES}, REQUEST_MEDIA_IMAGES);
        } else if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_FINE_LOCATION);
        } else {
            onAllPermissionsGranted();
        }
    }

    public void handlePermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_MEDIA_IMAGES) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestPermissionsInSequence(); // Запит наступного
            } else {
                Toast.makeText(activity, "Permission to read media is required", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_FINE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onAllPermissionsGranted();
            } else {
                Toast.makeText(activity, "Permission to access location is required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void onAllPermissionsGranted() {
        Toast.makeText(activity, "All permissions granted", Toast.LENGTH_SHORT).show();
    }

}
