package com.raziel.prettycity.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.*;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.exifinterface.media.ExifInterface;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.raziel.prettycity.R;
import com.raziel.prettycity.data.DatabaseClient;
import com.raziel.prettycity.data.Task;
import com.raziel.prettycity.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AddTaskFragment extends Fragment {
    private FusedLocationProviderClient fusedLocationClient;

    private ImageView imageBefore;
    private Button buttonSelectPhoto, buttonSaveTask;
    private EditText editTextTitle, editTextDescription, editTextCreatedAt;
    private TextView textCoordinates;
    private Spinner spinnerStatus, spinnerPriority;

    private String photoPath;
    private double latitude;
    private double longitude;
    private String dateTaken = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_task, container, false);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        imageBefore = view.findViewById(R.id.imageBefore);
        buttonSelectPhoto = view.findViewById(R.id.buttonSelectPhoto);
        buttonSaveTask = view.findViewById(R.id.buttonSaveTask);
        textCoordinates = view.findViewById(R.id.textCoordinates);
        editTextTitle = view.findViewById(R.id.editTextTitle);
        editTextDescription = view.findViewById(R.id.editTextDescription);
        editTextCreatedAt = view.findViewById(R.id.editTextCreatedAt);
        spinnerStatus = view.findViewById(R.id.spinnerStatus);
        spinnerPriority = view.findViewById(R.id.spinnerPriority);

        buttonSelectPhoto.setOnClickListener(v -> selectPhoto());
        buttonSaveTask.setOnClickListener(v -> saveTask());

        return view;
    }

    private void getCoordinatesFromExifOrLocation(Uri imageUri, LocationCallback callback) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri);
            ExifInterface exif = new ExifInterface(inputStream);

            String latStr = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
            String latRef = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
            String lonStr = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
            String lonRef = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF);

            double lat = Utils.convertToDegree(latStr);
            if (latRef != null && latRef.equals("S")) lat *= -1;

            double lon = Utils.convertToDegree(lonStr);
            if (lonRef != null && lonRef.equals("W")) lon *= -1;

            Log.d("COORD", "EXIF coords: " + lat + ", " + lon);
            callback.onCoordinatesReady(lat, lon);

            if (lat == 0.0 && lon == 0.0) {
                fetchCurrentLocation(callback);
            }

        } catch (IOException e) {
            e.printStackTrace();
            fetchCurrentLocation(callback);
        }
    }

    private void fetchCurrentLocation(LocationCallback callback) {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                Log.d("COORD", "GPS coords: " + location.getLatitude() + ", " + location.getLongitude());
                callback.onCoordinatesReady(location.getLatitude(), location.getLongitude());
            } else {
                Log.d("COORD", "GPS unavailable");
            }
        });
    }

    public interface LocationCallback {
        void onCoordinatesReady(double latitude, double longitude);
    }

    private final ActivityResultLauncher<Intent> photoPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                    Uri selectedImage = result.getData().getData();
                    photoPath = Utils.getPath(requireContext(), selectedImage);

                    getCoordinatesFromExifOrLocation(selectedImage, new LocationCallback() {
                        @Override
                        public void onCoordinatesReady(double lat, double lon) {
                            latitude = lat;
                            longitude = lon;
                            textCoordinates.setText("Lat: " + latitude + ", Lon: " + longitude);
                        }
                    });

                    try {
                        InputStream inputStream = requireContext().getContentResolver().openInputStream(selectedImage);
                        ExifInterface exif = new ExifInterface(inputStream);
                        dateTaken = exif.getAttribute(ExifInterface.TAG_DATETIME);
                        if (dateTaken == null) {
                            dateTaken = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                        }
                        editTextCreatedAt.setText(dateTaken);
//                        imageBefore.setImageBitmap(BitmapFactory.decodeFile(photoPath));

                        Glide.with(requireContext())
                            .load(Uri.fromFile(new File(photoPath)))
//                                    .placeholder(R.drawable.placeholder)
//                                    .error(R.drawable.error)
                            .into(imageBefore);

                        buttonSaveTask.setEnabled(true);

                    } catch (Exception e) {
                        Toast.makeText(requireContext(), "Не вдалося прочитати EXIF", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private void selectPhoto() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        photoPickerLauncher.launch(intent);
    }

    private void saveTask() {
        String title = editTextTitle.getText().toString().trim();
        String description = editTextDescription.getText().toString().trim();
        String status = spinnerStatus.getSelectedItem().toString();
        String priorityStr = spinnerPriority.getSelectedItem().toString();
        String createdAt = editTextCreatedAt.getText().toString().trim();

        int priority;
        try {
            priority = Integer.parseInt(priorityStr);
        } catch (NumberFormatException e) {
            priority = 3;
        }

        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "Введіть назву задачі", Toast.LENGTH_SHORT).show();
            return;
        }

        Task task = new Task(latitude, longitude, photoPath, status, createdAt);
        task.title = title;
        task.description = description;
        task.priority = priority;
        task.type = "";

        new Thread(() -> {
            DatabaseClient.getInstance(requireContext())
                    .getAppDatabase()
                    .taskDao()
                    .insert(task);
            Log.d("AddTask", "Inserted task with title: " + task.title + " at " + task.latitude + ", " + task.longitude);

            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), "Задачу додано №" + task.id, Toast.LENGTH_SHORT).show();
                Navigation.findNavController(requireView()).navigate(R.id.nav_map);
            });
        }).start();
    }
}
