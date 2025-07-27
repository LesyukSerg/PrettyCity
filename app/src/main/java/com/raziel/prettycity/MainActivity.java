package com.raziel.prettycity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;

import com.google.android.material.navigation.NavigationView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;

    private TaskDao taskDao; // ✅ Додано

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseApp.initializeApp(this); // ✅ Ініціалізація Firebase (на всяк випадок)

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.mainMap, R.id.taskList, R.id.nav_gallery, R.id.nav_slideshow)
                .setOpenableLayout(drawer)
                .build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // ✅ Отримуємо DAO з бази даних
        taskDao = AppDatabase.getDatabase(this).taskDao();

        FirebaseSyncManager syncManager = new FirebaseSyncManager(taskDao);

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
}
