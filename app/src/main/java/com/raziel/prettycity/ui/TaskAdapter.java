package com.raziel.prettycity.ui;

import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.raziel.prettycity.R;
import com.raziel.prettycity.data.Task;

public class TaskAdapter extends ListAdapter<Task, TaskAdapter.TaskViewHolder> {

    public static Double currentLat = null;
    public static Double currentLon = null;

    public TaskAdapter() {
        super(DIFF_CALLBACK);
    }

    static final DiffUtil.ItemCallback<Task> DIFF_CALLBACK = new DiffUtil.ItemCallback<Task>() {
        @Override public boolean areItemsTheSame(@NonNull Task oldItem, @NonNull Task newItem) {
            return oldItem.id == newItem.id;
        }

        @Override public boolean areContentsTheSame(@NonNull Task oldItem, @NonNull Task newItem) {
            return oldItem.equals(newItem);
        }
    };

    @NonNull
    @Override public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new TaskViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    public Task getTaskAt(int position) {
        return getItem(position);
    }

    class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView titleView, descriptionView, distanceView;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            titleView = itemView.findViewById(R.id.taskTitleTextView);
            descriptionView = itemView.findViewById(R.id.taskDescriptionTextView);
            distanceView = itemView.findViewById(R.id.taskDistanceTextView);
        }

        void bind(Task task) {
            titleView.setText(task.title != null ? task.title : "Без назви");
            descriptionView.setText(task.description != null ? task.description : "");

            if (TaskAdapter.currentLat != null && TaskAdapter.currentLon != null && task.latitude != 0 && task.longitude != 0) {
                float[] result = new float[1];
                Location.distanceBetween(TaskAdapter.currentLat, TaskAdapter.currentLon, task.latitude, task.longitude, result);
                int distance = Math.round(result[0]);
                distanceView.setText(distance + " м");
            } else {
                distanceView.setText("—");
            }
        }
    }
}
