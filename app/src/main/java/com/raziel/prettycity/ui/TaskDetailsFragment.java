package com.raziel.prettycity.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.raziel.prettycity.R;
import com.raziel.prettycity.data.AppDatabase;
import com.raziel.prettycity.data.Task;
import com.raziel.prettycity.data.TaskDao;
import com.raziel.prettycity.utils.Utils;

import java.io.File;

public class TaskDetailsFragment extends Fragment {

    private EditText editLatitude, editLongitude, editTitle, editDescription, editStatus;
    private Button buttonSave;
    private TaskDao taskDao;
    private int taskId;
    private Task currentTask;

    private TextView textTitle, textStatus, textDescription, textCoordinates;
    private ImageView imageBefore, imageAfter;
    private Button buttonChangeStatus, buttonAddAfterImage;


    public TaskDetailsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_task_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        TextView textTitle = view.findViewById(R.id.textTitle);
        TextView textStatus = view.findViewById(R.id.textStatus);
        TextView textDescription = view.findViewById(R.id.textDescription);
        TextView textCoordinates = view.findViewById(R.id.textCoordinates);
        ImageView imageBefore = view.findViewById(R.id.imageBefore);
        ImageView imageAfter = view.findViewById(R.id.imageAfter);
        Button buttonUpdateStatus = view.findViewById(R.id.buttonUpdateStatus);
        Button buttonAddAfterPhoto = view.findViewById(R.id.buttonAddAfterPhoto);

        taskDao = AppDatabase.getDatabase(requireContext()).taskDao();

        if (getArguments() != null) {
            taskId = getArguments().getInt("taskId", -1);
            if (taskId != -1) {
                taskDao.getById(taskId).observe(getViewLifecycleOwner(), task -> {
                    if (task != null) {
                        currentTask = task;

                        // Встановлення заголовка Fragment-а
                        requireActivity().setTitle(task.title);

                        textTitle.setText(task.title);
                        textStatus.setText("Status: " + task.status);
                        textDescription.setText(task.description);
                        textCoordinates.setText("Lat: " + task.latitude + ", Lng: " + task.longitude);

                        // Вивести зображення (якщо є)
                        if (currentTask.photoBeforePath != null) {
                            Glide.with(requireContext())
                                    .load(new File(task.photoBeforePath))
//                                    .placeholder(R.drawable.placeholder)
//                                    .error(R.drawable.error)
                                    .into(imageBefore);

                            imageBefore.setVisibility(View.VISIBLE);
//                            imageBefore.setImageURI(Uri.parse(currentTask.photoBeforePath));

                            imageBefore.setOnClickListener(v -> {
                                Bundle bundle = new Bundle();
                                bundle.putString("image_path", currentTask.photoBeforePath);

                                Navigation.findNavController(v).navigate(R.id.action_taskDetailsFragment_to_fullscreenImageFragment, bundle);
                            });
                        }

                        if ("completed".equalsIgnoreCase(task.status) && task.photoAfterPath != null) {
//                            imageAfter.setImageURI(Uri.parse(task.photoAfterPath));
                            Glide.with(requireContext())
                                    .load(new File(currentTask.photoAfterPath))
//                                    .placeholder(R.drawable.placeholder)
//                                    .error(R.drawable.error)
                                    .into(imageAfter);

                            imageAfter.setVisibility(View.VISIBLE);
                        } else {
                            imageAfter.setVisibility(View.GONE);
                        }

                        // Кнопка оновлення статусу
                        buttonUpdateStatus.setOnClickListener(v -> {
                            currentTask.status = "completed";
                            taskDao.update(currentTask);
                            textStatus.setText("Status: " + currentTask.status);
                        });

                        // Кнопка додавання зображення після
                        buttonAddAfterPhoto.setOnClickListener(v -> {
                            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                            afterPhotoPickerLauncher.launch(intent);
                        });
                    }
                });
            }
        }
    }

    private final ActivityResultLauncher<Intent> afterPhotoPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == requireActivity().RESULT_OK && result.getData() != null) {
                    Uri selectedImage = result.getData().getData();
                    String path = Utils.getPath(requireContext(), selectedImage);
                    if (path != null && currentTask != null) {
                        currentTask.photoAfterPath = path;
                        // зразу оновлюємо статус, якщо потрібно:
                        currentTask.status = "completed";
                        taskDao.update(currentTask);

                        imageAfter.setImageURI(Uri.parse(path));
                        imageAfter.setVisibility(View.VISIBLE);
                        textStatus.setText("Status: " + currentTask.status);
                    }
                }
            }
    );

}
