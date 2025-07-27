package com.raziel.prettycity.sync;

import android.net.Uri;
import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.raziel.prettycity.data.Task;
import com.raziel.prettycity.data.TaskDao;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class FirebaseSyncManager {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final TaskDao taskDao;

    public FirebaseSyncManager(TaskDao taskDao) {
        this.taskDao = taskDao;
    }

    // Upload unsynced tasks to Firebase
    public void syncToCloud(List<Task> tasks) {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Task> unsyncedTasks = taskDao.getUnsyncedTasks();

            for (Task task : unsyncedTasks) {
                db.collection("tasks")
                        .document(String.valueOf(task.id))
                        .set(task)
                        .addOnSuccessListener(aVoid -> {
                            task.synced = true;
                            taskDao.update(task); // ✅ позначаємо як синхронізовану
                        })
                        .addOnFailureListener(e -> Log.e("FirebaseSync", "Upload failed", e));

                uploadTaskPhotos(task);
            }
        });
    }

    // Download from Firebase and merge
    public void syncFromCloud() {
        db.collection("tasks").get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Task> remoteTasks = new ArrayList<>();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Task remoteTask = doc.toObject(Task.class);
                        remoteTask.synced = true; // позначаємо як синхронізовану
                        remoteTasks.add(remoteTask);
                    }

                    Executors.newSingleThreadExecutor().execute(() -> {
                        for (Task remoteTask : remoteTasks) {
                            Task local = taskDao.getById(remoteTask.id).getValue();
                            if (local == null) {
                                taskDao.insert(remoteTask);
                            } else {
                                taskDao.update(remoteTask);
                            }

                            downloadTaskPhotosIfMissing(remoteTask);
                        }
                    });
                })
                .addOnFailureListener(e -> Log.e("FirebaseSync", "Download failed", e));
    }

    public void uploadTaskPhotos(Task task) {
        uploadTaskPhoto(task.id, task.photoBeforePath, "before.jpg");
        uploadTaskPhoto(task.id, task.photoAfterPath, "after.jpg");
    }

    public void uploadTaskPhoto(Integer id, String localPath, String fileName) {
        if (localPath == null) return;

        Uri fileUri = Uri.fromFile(new File(localPath));
        StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                .child("PrettyCityPhotos/" + id + "/" + fileName);

        storageRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> {
                    Log.d("PhotoUpload", "Uploaded " + fileName + " for task " + id);
                })
                .addOnFailureListener(e -> {
                    Log.e("PhotoUpload", "Failed to upload photo: " + e.getMessage());
                });
    }

    public void downloadTaskPhotosIfMissing(Task task) {
        downloadTaskPhotoIfMissing(task.id, task.photoBeforePath, "before.jpg");
        downloadTaskPhotoIfMissing(task.id, task.photoAfterPath, "after.jpg");
    }

    public void downloadTaskPhotoIfMissing(Integer id, String localPath, String fileName) {
        if (localPath == null) return;
        File localFile = new File(localPath);
        if (localFile.exists()) return;

        StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                .child("PrettyCityPhotos/" + id + "/" + fileName);

        storageRef.getFile(localFile)
                .addOnSuccessListener(taskSnapshot -> {
                    Log.d("PhotoDownload", "Downloaded " + fileName + " for task " + id);
                })
                .addOnFailureListener(e -> {
                    Log.e("PhotoDownload", "Failed to download photo: " + e.getMessage());
                });
    }

}
