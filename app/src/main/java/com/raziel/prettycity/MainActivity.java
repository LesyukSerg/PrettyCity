package com.raziel.prettycity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.firebase.FirebaseApp;
import com.raziel.prettycity.data.AppDatabase;
import com.raziel.prettycity.data.Task;
import com.raziel.prettycity.data.TaskDao;
import com.raziel.prettycity.databinding.ActivityMainBinding;
import com.raziel.prettycity.sync.FirebaseSyncManager;
import com.raziel.prettycity.sync.SyncProgressListener;
import com.raziel.prettycity.utils.SetPermissions;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;

    private TaskDao taskDao; // ✅ Додано

    private SetPermissions setPermissions;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setPermissions = new SetPermissions(this);
        setPermissions.requestPermissionsInSequence();

        FirebaseApp.initializeApp(this); // ✅ Ініціалізація Firebase (на всяк випадок)

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.mainMap, R.id.taskList, R.id.doneTaskList)
                .setOpenableLayout(drawer)
                .build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // ✅ Отримуємо DAO з бази даних
        taskDao = AppDatabase.getDatabase(this).taskDao();

        FirebaseSyncManager syncManager = new FirebaseSyncManager(taskDao);

        FrameLayout syncOverlay = findViewById(R.id.sync_overlay);
        TextView syncText = findViewById(R.id.sync_text);

        syncManager.setSyncProgressListener(new SyncProgressListener() {
            @Override
            public void onSyncStarted(String direction) {
                runOnUiThread(() -> {
                    syncOverlay.setVisibility(View.VISIBLE);
                    syncText.setText(direction.equals("up") ? "Вивантаження..." : "Завантаження...");
                });
            }

            @Override
            public void onProgress(int current, int total) {
                runOnUiThread(() -> {
                    syncText.setText(String.format("Прогрес: %d / %d", current, total));
                });
            }

            @Override
            public void onSyncCompleted() {
                runOnUiThread(() -> syncOverlay.setVisibility(View.GONE));
            }
        });

        // Синхронізація при старті
        syncManager.syncFromCloud();

        // Зворотна синхронізація: локальні → Firebase
        // ❗ getViewLifecycleOwner() → this (у Activity)
        taskDao.getAll().observe(this, new Observer<List<Task>>() {
            @Override
            public void onChanged(List<Task> tasks) {
                syncManager.syncToCloud(tasks);
            }
        });

        // Синхронізація кожні 5 хвилин
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable periodicSync = new Runnable() {
            @Override
            public void run() {
                syncManager.syncFromCloud();
                handler.postDelayed(this, 5 * 60 * 1000); // 5 хв
            }
        };
        handler.post(periodicSync);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        setPermissions.handlePermissionsResult(requestCode, permissions, grantResults);
    }

}
