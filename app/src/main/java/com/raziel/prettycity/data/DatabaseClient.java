package com.raziel.prettycity.data;

import android.content.Context;

import androidx.room.Room;

public class DatabaseClient {

    private static DatabaseClient instance;
    private final AppDatabase appDatabase;

    private DatabaseClient(Context context) {
        appDatabase = Room.databaseBuilder(
                context.getApplicationContext(),
                AppDatabase.class,
                "TaskDB"
        ).build();
    }

    public static synchronized DatabaseClient getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseClient(context);
        }
        return instance;
    }

    public AppDatabase getAppDatabase() {
        return appDatabase;
    }

    public void insertTestTasks() {
        TaskDao taskDao = getAppDatabase().taskDao();

        for (int i = 1; i <= 10; i++) {
//            Task task = new Task();
//            task.setId(i);
//            task.setPhotoBeforePath("path/to/photo_before_" + i + ".jpg");
//            task.setPhotoAfterPath(null);
//            task.setCreatedAt(System.currentTimeMillis());
//            task.setCompletedAt(0);
//            task.setLatitude(50.4501 + i * 0.001); // приклад координат
//            task.setLongitude(30.5234 + i * 0.001);
//            task.setStatus("Заплановано");
//            task.setPriority((i % 5) + 1);
//            task.setDescription("Тестова задача #" + i);
//            task.setWorkType("Дерева");
//            task.setAddress("Адреса #" + i);
//            task.setExecutor("Виконавець #" + i);

//            taskDao.insert(task);
        }
    }
}