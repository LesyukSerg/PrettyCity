package com.raziel.prettycity.ui;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.exifinterface.media.ExifInterface;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.raziel.prettycity.R;
import com.raziel.prettycity.data.AppDatabase;
import com.raziel.prettycity.data.Task;
import com.raziel.prettycity.data.TaskDao;
import com.raziel.prettycity.utils.Utils;

import java.io.File;
import java.util.concurrent.Executors;

public class TaskDetailsFragment extends Fragment {

    private TextView textTitle, textStatus, textDescription, textCoordinates, textCompletedAt;
    private ImageView imageBefore, imageAfter;
    private Button buttonUpdateStatus, buttonEditCoordinates, buttonBuildRoute;

    private TaskDao taskDao;
    private Task currentTask;
    private Uri photoUri;
    private int taskId;

    public TaskDetailsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_task_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        textTitle = view.findViewById(R.id.textTitle);
        textStatus = view.findViewById(R.id.textStatus);
        textCompletedAt = view.findViewById(R.id.textCompletedAt);
        textDescription = view.findViewById(R.id.textDescription);
        textCoordinates = view.findViewById(R.id.textCoordinates);

        imageBefore = view.findViewById(R.id.imageBefore);
        imageAfter = view.findViewById(R.id.imageAfter);

        buttonUpdateStatus = view.findViewById(R.id.buttonUpdateStatus);
        buttonEditCoordinates = view.findViewById(R.id.buttonEditCoordinates);
        buttonBuildRoute = view.findViewById(R.id.buttonBuildRoute);

        buttonBuildRoute.setOnClickListener(v -> {
            if (currentTask != null) {
                Bundle bundle = new Bundle();
                bundle.putString("target_lat", String.valueOf(currentTask.latitude));
                bundle.putString("target_lon", String.valueOf(currentTask.longitude));
                Navigation.findNavController(v).navigate(R.id.action_taskDetailsFragment_to_mapsFragment, bundle);
            }
        });

        taskDao = AppDatabase.getDatabase(requireContext()).taskDao();

        if (getArguments() != null) {
            taskId = getArguments().getInt("taskId", -1);
            if (taskId != -1) {
                taskDao.getById(taskId).observe(getViewLifecycleOwner(), task -> {
                    if (task != null) {
                        currentTask = task;

                        requireActivity().setTitle(task.title);

                        textTitle.setText(task.title);
                        textStatus.setText("Status: " + task.status);
                        textDescription.setText(task.description);
                        textCoordinates.setText("Lat: " + task.latitude + ", Lng: " + task.longitude);

                        if (task.photoBeforePath != null) {
                            File originalFile = new File(task.photoBeforePath);
                            String prettyCityDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/PrettyCity";

                            if (!task.photoBeforePath.contains("PrettyCity")) {
                                File targetDir = new File(prettyCityDir);
                                if (!targetDir.exists()) {
                                    targetDir.mkdirs(); // Створити папку, якщо її нема
                                }

                                String newFileName = "IMG-before-" + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(new Date()) + ".jpg";
                                File newFile = new File(targetDir, newFileName);

                                boolean success = Utils.copyFile(originalFile, newFile);
                                if (success) {
                                    task.photoBeforePath = newFile.getAbsolutePath();

                                    Executors.newSingleThreadExecutor().execute(() -> {
                                        taskDao.update(task);
                                    });

                                    originalFile.delete(); // (опційно) видалити старий
                                }
                            }

                            Glide.with(requireContext())
                                    .load(Uri.fromFile(new File(task.photoBeforePath)))
                                    .into(imageBefore);
                            imageBefore.setVisibility(View.VISIBLE);

                            imageBefore.setOnClickListener(v -> {
                                Bundle bundle = new Bundle();
                                bundle.putString("image_path", task.photoBeforePath);
                                Navigation.findNavController(v).navigate(R.id.action_taskDetailsFragment_to_fullscreenImageFragment, bundle);
                            });
                        }

                        if ("done".equalsIgnoreCase(task.status) && task.photoAfterPath != null) {
                            textCompletedAt.setText("Дата виконання: " + task.completedAt);
                            textCompletedAt.setVisibility(View.VISIBLE);

                            File originalAfterFile = new File(task.photoAfterPath);
                            String prettyCityDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/PrettyCity";

                            if (!task.photoAfterPath.contains("PrettyCity")) {
                                File targetDir = new File(prettyCityDir);
                                if (!targetDir.exists()) {
                                    targetDir.mkdirs(); // Створити папку, якщо її ще нема
                                }

                                String newFileName = "IMG-after-" + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(new Date()) + ".jpg";
                                File newFile = new File(targetDir, newFileName);

                                boolean success = Utils.copyFile(originalAfterFile, newFile);
                                if (success) {
                                    task.photoAfterPath = newFile.getAbsolutePath();

                                    Executors.newSingleThreadExecutor().execute(() -> {
                                        taskDao.update(task);
                                    });

                                    originalAfterFile.delete(); // (опційно) видалити старий
                                }
                            }

                            Glide.with(requireContext())
                                    .load(Uri.fromFile(new File(task.photoAfterPath)))
                                    .into(imageAfter);
                            imageAfter.setVisibility(View.VISIBLE);

                            imageAfter.setOnClickListener(v -> {
                                Bundle bundle = new Bundle();
                                bundle.putString("image_path", task.photoAfterPath);
                                Navigation.findNavController(v).navigate(R.id.action_taskDetailsFragment_to_fullscreenImageFragment, bundle);
                            });

                        } else {
                            imageAfter.setVisibility(View.GONE);
                        }

                        buttonUpdateStatus.setOnClickListener(v -> {
                            String[] options = {"Зробити фото", "Вибрати з галереї"};
                            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                                    .setTitle("Додати фото")
                                    .setItems(options, (dialog, which) -> {
                                        if (which == 0) {
                                            takePhoto();
                                        } else {
                                            Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                                            afterPhotoPickerLauncher.launch(pickIntent);
                                        }
                                    })
                                    .show();
                        });
                    }
                });

                // Слухаємо результат від фрагмента зміни координат
                getParentFragmentManager().setFragmentResultListener("location_result", this, (key, bundle) -> {
                    String newLat = bundle.getString("latitude");
                    String newLng = bundle.getString("longitude");

                    textCoordinates.setText("Lat: " + newLat + ", Lng: " + newLng);
                    currentTask.latitude = Double.parseDouble(newLat);
                    currentTask.longitude = Double.parseDouble(newLng);

                    Executors.newSingleThreadExecutor().execute(() -> {
                        taskDao.update(currentTask);
                    });
                });

                buttonEditCoordinates.setOnClickListener(v -> {
                    Bundle args = new Bundle();
                    args.putString("latitude", String.valueOf(currentTask.latitude));
                    args.putString("longitude", String.valueOf(currentTask.longitude));
                    Navigation.findNavController(v).navigate(R.id.action_taskDetailsFragment_to_editLocationFragment, args);
                });
            }
        }
    }

    private void takePhoto() {
        String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault())
                .format(new Date());
        ContentValues values = new ContentValues();
        String filename = "IMG-after-" + timestamp + ".jpg";
        values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/PrettyCity");

        ContentResolver resolver = requireContext().getContentResolver();
        photoUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
        cameraLauncher.launch(takePictureIntent);
    }

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && photoUri != null) {
                    String realPath = Utils.getPath(requireContext(), photoUri);

                    if (realPath != null && currentTask != null) {
                        currentTask.photoAfterPath = realPath;
                        currentTask.status = "done";

                        try {
                            InputStream inputStream = requireContext().getContentResolver().openInputStream(photoUri);
                            ExifInterface exif = new ExifInterface(inputStream);

                            String dateDone = exif.getAttribute(ExifInterface.TAG_DATETIME);
                            if (dateDone == null) {
                                dateDone = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                            }

                            textCompletedAt.setText("Дата виконання: " + dateDone);
                            textCompletedAt.setVisibility(View.VISIBLE);
                            currentTask.completedAt = dateDone.trim();

                        } catch (Exception e) {
                            Toast.makeText(requireContext(), "Не вдалося прочитати EXIF", Toast.LENGTH_SHORT).show();
                        }

                        Executors.newSingleThreadExecutor().execute(() -> {
                            taskDao.update(currentTask);
                        });

                        Glide.with(requireContext())
                                .load(Uri.fromFile(new File(realPath)))
                                .into(imageAfter);

                        buttonUpdateStatus.setText(R.string.complete);
                        buttonUpdateStatus.setVisibility(View.GONE);
                        buttonEditCoordinates.setVisibility(View.GONE);
                        imageAfter.setVisibility(View.VISIBLE);
                        textStatus.setText("Status: " + currentTask.status);
                    }
                }
            }
    );

    private final ActivityResultLauncher<Intent> afterPhotoPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == requireActivity().RESULT_OK && result.getData() != null) {
                    Uri photoUri = result.getData().getData();
                    String realPath = Utils.getPath(requireContext(), photoUri);

                    if (realPath != null && currentTask != null) {
                        currentTask.photoAfterPath = realPath;
                        currentTask.status = "done";

                        try {
                            InputStream inputStream = requireContext().getContentResolver().openInputStream(photoUri);
                            ExifInterface exif = new ExifInterface(inputStream);

                            String dateDone = exif.getAttribute(ExifInterface.TAG_DATETIME);
                            if (dateDone == null) {
                                dateDone = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                            }

                            textCompletedAt.setText("Дата виконання: " + dateDone);
                            textCompletedAt.setVisibility(View.VISIBLE);
                            currentTask.completedAt = dateDone.trim();

                        } catch (Exception e) {
                            Toast.makeText(requireContext(), "Не вдалося прочитати EXIF", Toast.LENGTH_SHORT).show();
                        }

                        Executors.newSingleThreadExecutor().execute(() -> {
                            taskDao.update(currentTask);
                        });

                        Glide.with(requireContext())
                                .load(Uri.fromFile(new File(realPath)))
                                .into(imageAfter);

                        buttonUpdateStatus.setText(R.string.complete);
                        buttonUpdateStatus.setVisibility(View.GONE);
                        buttonEditCoordinates.setVisibility(View.GONE);
                        imageAfter.setVisibility(View.VISIBLE);
                        textStatus.setText("Status: " + currentTask.status);
                    }
                }
            }
    );
}
