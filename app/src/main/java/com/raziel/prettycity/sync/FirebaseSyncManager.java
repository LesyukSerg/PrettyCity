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

    private SyncProgressListener syncListener;

    public void setSyncProgressListener(SyncProgressListener listener) {
        this.syncListener = listener;
    }

    public FirebaseSyncManager(TaskDao taskDao) {
        this.taskDao = taskDao;
    }

    // Upload unsynced tasks to Firebase
    public void syncToCloud(List<Task> tasks) {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Task> unsyncedTasks = taskDao.getUnsyncedTasks();
            int total = unsyncedTasks.size();
            int[] progress = {0};

            if (syncListener != null) {
                syncListener.onSyncStarted("up");
            }

            for (Task task : unsyncedTasks) {
                db.collection("tasks")
                        .document(String.valueOf(task.id))
                        .set(task)
//                        .addOnSuccessListener(aVoid -> {
//                            task.synced = true;
//                            Executors.newSingleThreadExecutor().execute(() -> taskDao.update(task));
//                        })
                        .addOnFailureListener(e -> Log.e("FirebaseSync", "Upload failed", e));

                if (!task.deleted) {
                    uploadTaskPhotos(task, () -> {
                        progress[0]++;
                        if (syncListener != null) {
                            syncListener.onProgress(progress[0], total);
                            if (progress[0] == total) {
                                syncListener.onSyncCompleted();
                            }
                        }
                    });
                }
            }

            if (total == 0 && syncListener != null) {
                syncListener.onSyncCompleted();
            }
        });
    }

    // Download from Firebase and merge
    public void syncFromCloud() {
        db.collection("tasks").get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Task> remoteTasks = new ArrayList<>();
                    int[] progress = {0};

                    if (syncListener != null) {
                        syncListener.onSyncStarted("down");
                    }

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Task remoteTask = doc.toObject(Task.class);
                        remoteTask.synced = true; // позначаємо як синхронізовану
                        remoteTasks.add(remoteTask);
                    }

                    int total = remoteTasks.size();

                    for (Task remoteTask : remoteTasks) {
                        Task local = taskDao.getById(remoteTask.id).getValue();
                        if (local == null) {
                            Executors.newSingleThreadExecutor().execute(() -> taskDao.insert(remoteTask));
                        } else {
                            Executors.newSingleThreadExecutor().execute(() -> taskDao.update(remoteTask));
                        }

                        if (!remoteTask.deleted) {
                            downloadTaskPhotosIfMissing(remoteTask, () -> {
                                progress[0]++;
                                if (syncListener != null) {
                                    syncListener.onProgress(progress[0], total);
                                    if (progress[0] == total) {
                                        syncListener.onSyncCompleted();
                                    }
                                }
                            });
                        }
                    }

                    if (total == 0 && syncListener != null) {
                        syncListener.onSyncCompleted();
                    }
                })
                .addOnFailureListener(e -> Log.e("FirebaseSync", "Download failed", e));
    }

    public void uploadTaskPhotos(Task task, Runnable onComplete) {
        uploadTaskPhoto(task.id, task.photoBeforePath, "before.jpg", () ->
                uploadTaskPhoto(task.id, task.photoAfterPath, "after.jpg", onComplete));

        task.synced = true;
        Executors.newSingleThreadExecutor().execute(() -> taskDao.update(task));
    }

    public void uploadTaskPhoto(Integer id, String localPath, String fileName, Runnable onComplete) {
        if (localPath == null || !(new File(localPath).exists())) {
            onComplete.run();
            return;
        }

        Uri fileUri = Uri.fromFile(new File(localPath));
        StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                .child("PrettyCityPhotos/" + id + "/" + fileName);

        storageRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> {
                    Log.d("PhotoUpload", "Uploaded " + fileName + " for task " + id);
                    onComplete.run();
                })
                .addOnFailureListener(e -> {
                    Log.e("PhotoUpload", id + " Failed to upload " + fileName + " photo: " + e.getMessage());
                    onComplete.run();
                });
    }

    public void downloadTaskPhotosIfMissing(Task task, Runnable onComplete) {
        downloadTaskPhotoIfMissing(task.id, task.photoBeforePath, "before.jpg", () ->
                downloadTaskPhotoIfMissing(task.id, task.photoAfterPath, "after.jpg", onComplete));
    }

    public void downloadTaskPhotoIfMissing(Integer id, String localPath, String fileName, Runnable onComplete) {
        if (localPath == null) {
            onComplete.run();
            return;
        }

        File localFile = new File(localPath);
        File parentDir = localFile.getParentFile();

        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        if (localFile.exists()) {
            onComplete.run();
            return;
        }

        StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                .child("PrettyCityPhotos/" + id + "/" + fileName);

        storageRef.getFile(localFile)
                .addOnSuccessListener(taskSnapshot -> {
                    Log.d("PhotoDownload", "Downloaded " + fileName + " for task " + id);
                    onComplete.run();
                })
                .addOnFailureListener(e -> {
                    Log.e("PhotoDownload", id + " Failed to download " + fileName + " photo: " + e.getMessage());
                    onComplete.run();
                });
    }

    public void deleteTaskPhoto(Integer id, String localPath, String remoteFileName) {
        if (localPath == null || !(new File(localPath).exists())) {
            return;
        }

        StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                .child("PrettyCityPhotos/" + id + "/" + remoteFileName);

        storageRef.delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d("PhotoDelete", "Deleted " + remoteFileName + " for task " + id);
                })
                .addOnFailureListener(e -> {
                    Log.e("PhotoDelete", "Deleted " + remoteFileName + " for task " + id);
                });
    }

}
