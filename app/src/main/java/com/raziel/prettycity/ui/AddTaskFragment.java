package com.raziel.prettycity.ui;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.*;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.exifinterface.media.ExifInterface;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.raziel.prettycity.R;
import com.raziel.prettycity.data.DatabaseClient;
import com.raziel.prettycity.data.Task;
import com.raziel.prettycity.utils.FileUtils;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AddTaskFragment extends Fragment {

    private ImageView imageBefore;
    private Button buttonSelectPhoto, buttonSaveTask;
    private EditText editTextTitle, editTextDescription, editTextCreatedAt;
    private Spinner spinnerStatus, spinnerPriority;

    private String photoPath;
    private double latitude = 0;
    private double longitude = 0;
    private String dateTaken = "";

    private final ActivityResultLauncher<Intent> photoPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                    Uri selectedImage = result.getData().getData();
                    photoPath = FileUtils.getPath(requireContext(), selectedImage);

                    try {
                        InputStream inputStream = requireContext().getContentResolver().openInputStream(selectedImage);
                        ExifInterface exif = new ExifInterface(inputStream);

                        float[] latLong = new float[2];
                        if (exif.getLatLong(latLong)) {
                            latitude = latLong[0];
                            longitude = latLong[1];
                        }

                        dateTaken = exif.getAttribute(ExifInterface.TAG_DATETIME);
                        if (dateTaken == null) {
                            dateTaken = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                        }

                        imageBefore.setImageBitmap(BitmapFactory.decodeFile(photoPath));
                        buttonSaveTask.setEnabled(true);
                        editTextCreatedAt.setText(dateTaken);

                    } catch (Exception e) {
                        Toast.makeText(requireContext(), "Не вдалося прочитати EXIF", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_task, container, false);

        imageBefore = view.findViewById(R.id.imageBefore);
        buttonSelectPhoto = view.findViewById(R.id.buttonSelectPhoto);
        buttonSaveTask = view.findViewById(R.id.buttonSaveTask);
        editTextTitle = view.findViewById(R.id.editTextTitle);
        editTextDescription = view.findViewById(R.id.editTextDescription);
        editTextCreatedAt = view.findViewById(R.id.editTextCreatedAt);
        spinnerStatus = view.findViewById(R.id.spinnerStatus);
        spinnerPriority = view.findViewById(R.id.spinnerPriority);

        buttonSelectPhoto.setOnClickListener(v -> selectPhoto());
        buttonSaveTask.setOnClickListener(v -> saveTask());

        return view;
    }

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
            priority = 3; // Значення за замовчуванням
        }

        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "Введіть назву задачі", Toast.LENGTH_SHORT).show();
            return;
        }

        Task task = new Task(latitude, longitude, photoPath, status, createdAt);
        task.title = title;
        task.description = description;
        task.priority = priority;
        task.type = ""; // Якщо потрібно, можеш додати ще поле для типу

        new Thread(() -> {
            DatabaseClient.getInstance(requireContext())
                    .getAppDatabase()
                    .taskDao()
                    .insert(task);
            Log.d("AddTask", "Inserted task with title: " + task.title + " at " + task.latitude + ", " + task.longitude);


            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), "Задачу додано №"+ task.id, Toast.LENGTH_SHORT).show();
                Navigation.findNavController(requireView()).navigate(R.id.nav_map);
            });
        }).start();
    }
}
