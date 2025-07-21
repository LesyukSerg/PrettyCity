package com.raziel.prettycity.ui;

import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.raziel.prettycity.R;
import com.raziel.prettycity.data.Task;

import java.util.ArrayList;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> taskList = new ArrayList<>();

    public void submitList(List<Task> tasks) {
        this.taskList = tasks;
        notifyDataSetChanged();
    }

    public Task getTaskAt(int position) {
        return taskList.get(position);
    }

    @NonNull @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(itemView);
    }

    @Override public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        holder.textDate.setText(task.createdAt);
        holder.textLocation.setText(task.latitude + ", " + task.longitude);
        if (task.photoBeforePath != null) {
            holder.imageView.setImageBitmap(BitmapFactory.decodeFile(task.photoBeforePath));
        }
    }

    @Override public int getItemCount() {
        return taskList.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView textDate, textLocation;
        ImageView imageView;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            textDate = itemView.findViewById(R.id.textDate);
            textLocation = itemView.findViewById(R.id.textLocation);
            imageView = itemView.findViewById(R.id.imageViewPreview);
        }
    }
}
