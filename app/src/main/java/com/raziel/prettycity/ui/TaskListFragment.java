package com.raziel.prettycity.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.raziel.prettycity.R;
import com.raziel.prettycity.data.AppDatabase;
import com.raziel.prettycity.data.Task;
import com.raziel.prettycity.data.TaskDao;
import android.view.View;
import java.util.List;

public class TaskListFragment extends Fragment {

    private TaskAdapter adapter;
    private TaskDao taskDao;

    public TaskListFragment() {
        super(R.layout.fragment_task_list);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FusedLocationProviderClient locationClient = LocationServices.getFusedLocationProviderClient(requireContext());
        locationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                TaskAdapter.currentLat = location.getLatitude();
                TaskAdapter.currentLon = location.getLongitude();
                adapter.notifyDataSetChanged(); // оновити відстань
            }
        });

        RecyclerView recyclerView = view.findViewById(R.id.taskRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TaskAdapter();
        recyclerView.setAdapter(adapter);

        taskDao = AppDatabase.getDatabase(getContext()).taskDao();
        taskDao.getAll().observe(getViewLifecycleOwner(), tasks -> adapter.submitList(tasks));

        // Swipe to delete
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                Task task = adapter.getTaskAt(viewHolder.getAdapterPosition());

                new AlertDialog.Builder(getContext())
                        .setTitle("Видалити задачу?")
                        .setMessage("Цю дію неможливо скасувати.")
                        .setPositiveButton("Так", (dialog, which) -> {
                            taskDao.delete(task);
                        })
                        .setNegativeButton("Скасувати", (dialog, which) -> {
                            adapter.notifyItemChanged(viewHolder.getAdapterPosition());
                        })
                        .show();
            }
        }).attachToRecyclerView(recyclerView);
    }
}
