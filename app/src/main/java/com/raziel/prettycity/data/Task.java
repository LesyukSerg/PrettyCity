package com.raziel.prettycity.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tasks")
public class Task {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public double latitude;
    public double longitude;

    public String photoBeforePath;
    public String photoAfterPath;

    public String createdAt;
    public String completedAt;

    public String status; // e.g., "pending", "done", "in_progress"
    public int priority;  // 1 - 5
    public String description;
    public String type;   // кущі / дерево / інше

    public Task(double latitude, double longitude, String photoBeforePath, String status, String createdAt) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.photoBeforePath = photoBeforePath;
        this.status = status;
        this.createdAt = createdAt;
    }

    @NonNull
    @Override
    public String toString() {
        return "Task{" +
                "id=" + id +
                ", lat=" + latitude +
                ", lon=" + longitude +
                ", status=" + status +
                ", date=" + createdAt +
                '}';
    }
}
