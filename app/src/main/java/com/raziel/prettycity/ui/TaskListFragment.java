package com.raziel.prettycity.ui;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.raziel.prettycity.R;
import com.raziel.prettycity.data.AppDatabase;
import com.raziel.prettycity.data.Task;
import com.raziel.prettycity.data.TaskDao;

import java.util.List;

public class TaskListFragment extends Fragment {

    private RecyclerView recyclerView;
    private TaskAdapter adapter;
    private TaskDao taskDao;

    public TaskListFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_task_list, container, false);
        recyclerView = view.findViewById(R.id.recyclerTasks);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TaskAdapter();
        recyclerView.setAdapter(adapter);

        taskDao = AppDatabase.getDatabase(getContext()).taskDao();
        taskDao.getAllTasks().observe(getViewLifecycleOwner(), new Observer<List<Task>>() {
            @Override
            public void onChanged(List<Task> tasks) {
                adapter.submitList(tasks);
            }
        });

        // Swipe to delete
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) { return false; }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                Task task = adapter.getTaskAt(viewHolder.getAdapterPosition());
                taskDao.delete(task);
            }
        }).attachToRecyclerView(recyclerView);

        return view;
    }
}
