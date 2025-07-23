package com.raziel.prettycity.ui;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.navigation.Navigation;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.raziel.prettycity.R;
import com.raziel.prettycity.data.AppDatabase;
import com.raziel.prettycity.data.Task;
import com.raziel.prettycity.data.TaskDao;

import java.util.List;

public class MapsFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private TaskDao taskDao;
    private FusedLocationProviderClient fusedLocationClient;

    public MapsFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_maps, container, false);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        taskDao = AppDatabase.getDatabase(requireContext()).taskDao();

        FloatingActionButton fab = view.findViewById(R.id.fab_add);
        fab.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_mapsFragment_to_addTaskFragment);
        });

        centerMapOnMyLocation();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        SharedPreferences prefs = requireContext().getSharedPreferences("map_prefs", Context.MODE_PRIVATE);
        float lastLat = prefs.getFloat("last_lat", 0f);
        float lastLng = prefs.getFloat("last_lng", 0f);

        if (lastLat != 0f && lastLng != 0f) {
            LatLng lastLocation = new LatLng(lastLat, lastLng);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastLocation, 15f));
        }

        enableMyLocation();

        taskDao.getAll().observe(getViewLifecycleOwner(), new Observer<List<Task>>() {
            @Override
            public void onChanged(List<Task> tasks) {
                mMap.clear();
                for (Task task : tasks) {
                    LatLng position = new LatLng(task.getLatitude(), task.getLongitude());

                    BitmapDescriptor icon;
                    if ("done".equalsIgnoreCase(task.status)) {
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED);
                    } else {
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN);
                    }

                    mMap.addMarker(new MarkerOptions()
                            .position(position)
                            .title(task.id + ". " + task.title)
                            .snippet(task.description)
                            .icon(icon));
                }

                mMap.setOnInfoWindowClickListener(marker -> {
                    String title = marker.getTitle(); // Наприклад: "3. Обрізка кущів"
                    if (title != null && title.contains(".")) {
                        try {
                            int taskId = Integer.parseInt(title.split("\\.")[0].trim());

                            Bundle bundle = new Bundle();
                            bundle.putInt("taskId", taskId); // передаємо taskId

                            Navigation.findNavController(requireView()).navigate(R.id.action_mapsFragment_to_taskDetailsFragment, bundle);
                        } catch (NumberFormatException e) {
                            Toast.makeText(requireContext(), "Невірний ID задачі", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }

    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            LatLng myLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.addMarker(new MarkerOptions()
                                    .position(myLatLng)
                                    .title("Ви тут"));

                            saveLastKnownLocation(location.getLatitude(), location.getLongitude());
                        }
                    });

        } else {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
        }
    }

    private void centerMapOnMyLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            LatLng myLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, 15f));
                        } else {
                            Toast.makeText(requireContext(), "Не вдалося отримати локацію", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void saveLastKnownLocation(double lat, double lng) {
        requireContext().getSharedPreferences("map_prefs", Context.MODE_PRIVATE)
                .edit()
                .putFloat("last_lat", (float) lat)
                .putFloat("last_lng", (float) lng)
                .apply();
    }
}
