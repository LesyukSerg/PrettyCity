package com.raziel.prettycity.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.raziel.prettycity.R;
import com.raziel.prettycity.data.AppDatabase;
import com.raziel.prettycity.data.Task;
import com.raziel.prettycity.data.TaskDao;
import com.raziel.prettycity.sync.FirebaseSyncManager;
import com.raziel.prettycity.utils.Utils;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;

public class TaskListFragment extends Fragment {

    private TaskAdapter adapter;
    private TaskDao taskDao;
    private List<Task> currentTasks;
    private String currentSort = "name_asc";


    public TaskListFragment() {
        super(R.layout.fragment_task_list);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(true);

        FloatingActionButton fab = view.findViewById(R.id.fab_add2);
        fab.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_mapsFragment_to_addTaskFragment);
        });

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
        taskDao.getAll().observe(getViewLifecycleOwner(), tasks -> {
            currentTasks = tasks;
            applySort(currentSort);
        });

        adapter.setOnTaskClickListener(task -> {
            Bundle bundle = new Bundle();
            bundle.putInt("taskId", task.id);
            Navigation.findNavController(view).navigate(R.id.taskDetailsFragment, bundle);
        });

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
                            Executors.newSingleThreadExecutor().execute(() -> {
                                FirebaseSyncManager syncManager = new FirebaseSyncManager(taskDao);
                                syncManager.deleteTaskPhoto(task.id, task.photoBeforePath, "before.jpg");
                                syncManager.deleteTaskPhoto(task.id, task.photoAfterPath, "after.jpg");

                                Utils.deleteTaskPhoto(task.photoBeforePath);
                                Utils.deleteTaskPhoto(task.photoAfterPath);

                                taskDao.markAsDeleted(task.id);
                            });
                        })
                        .setNegativeButton("Скасувати", (dialog, which) -> {
                            adapter.notifyItemChanged(viewHolder.getAdapterPosition());
                        })
                        .show();
            }

            @Override
            public void onChildDraw(@NonNull Canvas c,
                                    @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

                View itemView = viewHolder.itemView;
                Paint paint = new Paint();
                paint.setColor(Color.RED);
                RectF background = new RectF(
                        itemView.getRight() + dX,
                        itemView.getTop(),
                        itemView.getRight(),
                        itemView.getBottom()
                );
                c.drawRect(background, paint);

                Drawable icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_delete_white);
                if (icon != null) {
                    int iconMargin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
                    int iconTop = itemView.getTop() + iconMargin;
                    int iconLeft = itemView.getRight() - iconMargin - icon.getIntrinsicWidth();
                    int iconRight = itemView.getRight() - iconMargin;
                    int iconBottom = iconTop + icon.getIntrinsicHeight();

                    icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                    icon.draw(c);
                }
            }
        }).attachToRecyclerView(recyclerView);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_task_list, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.sort_name_asc) {
            currentSort = "name_asc";
        } else if (itemId == R.id.sort_name_desc) {
            currentSort = "name_desc";
        } else if (itemId == R.id.sort_priority_asc) {
            currentSort = "priority_asc";
        } else if (itemId == R.id.sort_priority_desc) {
            currentSort = "priority_desc";
        } else if (itemId == R.id.sort_distance_asc) {
            currentSort = "distance_asc";
        } else if (itemId == R.id.sort_distance_desc) {
            currentSort = "distance_desc";
        } else {
            return super.onOptionsItemSelected(item);
        }

        applySort(currentSort);

        return true;
    }

    private void applySort(String sortType) {
        if (currentTasks == null) return;

        List<Task> sorted = new ArrayList<>(currentTasks);

        switch (sortType) {
            case "name_asc":
                sorted.sort(Comparator.comparing(t -> t.title != null ? t.title : ""));
                break;
            case "name_desc":
                sorted.sort((t1, t2) -> (t2.title != null ? t2.title : "").compareToIgnoreCase(t1.title != null ? t1.title : ""));
                break;
            case "priority_asc":
                sorted.sort(Comparator.comparingInt(t -> t.priority)); // додай поле priority в Task
                break;
            case "priority_desc":
                sorted.sort((t1, t2) -> Integer.compare(t2.priority, t1.priority));
                break;
            case "distance_asc":
                sorted.sort(Comparator.comparingDouble(this::calculateDistance));
                break;
            case "distance_desc":
                sorted.sort((t1, t2) -> Double.compare(calculateDistance(t2), calculateDistance(t1)));
                break;
        }

        adapter.submitList(sorted);
    }

    private double calculateDistance(Task task) {
        if (TaskAdapter.currentLat != null && TaskAdapter.currentLon != null && task.latitude != 0 && task.longitude != 0) {
            float[] result = new float[1];
            Location.distanceBetween(TaskAdapter.currentLat, TaskAdapter.currentLon, task.latitude, task.longitude, result);
            return result[0];
        }

        return Double.MAX_VALUE;
    }


}
