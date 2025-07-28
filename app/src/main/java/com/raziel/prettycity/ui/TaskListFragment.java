package com.raziel.prettycity.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
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

import android.view.View;

import java.io.File;
import java.util.List;
import java.util.concurrent.Executors;

public class TaskListFragment extends Fragment {

    private TaskAdapter adapter;
    private TaskDao taskDao;

    public TaskListFragment() {
        super(R.layout.fragment_task_list);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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
        taskDao.getAll().observe(getViewLifecycleOwner(), tasks -> adapter.submitList(tasks));

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
}
