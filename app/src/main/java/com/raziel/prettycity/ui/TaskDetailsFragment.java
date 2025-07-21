package com.raziel.prettycity.ui;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.raziel.prettycity.R;
import com.raziel.prettycity.data.AppDatabase;
import com.raziel.prettycity.data.Task;
import com.raziel.prettycity.data.TaskDao;

public class TaskDetailsFragment extends Fragment {

    private EditText editLatitude, editLongitude, editTitle, editDescription, editStatus;
    private Button buttonSave;
    private TaskDao taskDao;
    private int taskId;
    private Task currentTask;

    public TaskDetailsFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_task_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        editLatitude = view.findViewById(R.id.editLatitude);
        editLongitude = view.findViewById(R.id.editLongitude);
        editTitle = view.findViewById(R.id.editTitle);
        editDescription = view.findViewById(R.id.editDescription);
        editStatus = view.findViewById(R.id.editStatus);
        buttonSave = view.findViewById(R.id.buttonSave);

        taskDao = AppDatabase.getDatabase(requireContext()).taskDao();

        if (getArguments() != null) {
            taskId = getArguments().getInt("taskId", -1);
            if (taskId != -1) {
                taskDao.getById(taskId).observe(getViewLifecycleOwner(), new Observer<Task>() {
                    @Override
                    public void onChanged(Task task) {
                        if (task != null) {
                            currentTask = task;
                            editLatitude.setText(String.valueOf(task.latitude));
                            editLongitude.setText(String.valueOf(task.longitude));
                            editTitle.setText(task.title);
                            editDescription.setText(task.description);
                            editStatus.setText(task.status);
                        }
                    }
                });
            }
        }

        buttonSave.setOnClickListener(v -> {
            if (currentTask != null) {
//                currentTask.setLatitude(Double.parseDouble(editLatitude.getText().toString()));
//                currentTask.setLongitude(Double.parseDouble(editLongitude.getText().toString()));
//                currentTask.setTitle(editTitle.getText().toString());
//                currentTask.setDescription(editDescription.getText().toString());
//                currentTask.setStatus(editStatus.getText().toString());
                taskDao.update(currentTask);
            }
        });
    }
}
