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

import com.akexorcist.googledirection.DirectionCallback;
import com.akexorcist.googledirection.GoogleDirection;
import com.akexorcist.googledirection.constant.TransportMode;
import com.akexorcist.googledirection.model.Direction;
import com.akexorcist.googledirection.model.Route; // <-- ВАЖЛИВО: цей імпорт
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
import com.google.android.gms.maps.model.PolylineOptions;
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

    private enum MarkerFilter {
        ALL,
        PLANNED_ONLY,
        CURRENT_ROUTE
    }

    private MarkerFilter currentFilter = MarkerFilter.ALL;
    private LatLng currentRouteTarget = null;
    private List<Task> allTasks;

    public MapsFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_maps, container, false);

        ImageButton btnShowAll = view.findViewById(R.id.btn_show_all);
        ImageButton btnShowPlanned = view.findViewById(R.id.btn_show_planned);
        ImageButton btnShowCurrentRoute = view.findViewById(R.id.btn_show_current_route);

        btnShowAll.setOnClickListener(v -> {
            currentFilter = MarkerFilter.ALL;
            reloadMarkers();
        });

        btnShowPlanned.setOnClickListener(v -> {
            currentFilter = MarkerFilter.PLANNED_ONLY;
            reloadMarkers();
        });

        btnShowCurrentRoute.setOnClickListener(v -> {
            currentFilter = MarkerFilter.CURRENT_ROUTE;
            reloadMarkers();

            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    LatLng start = new LatLng(location.getLatitude(), location.getLongitude());
                    drawRoute(start, currentRouteTarget);
                }
            });
        });

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

        if (getArguments() != null && getArguments().containsKey("target_lat") && getArguments().containsKey("target_lon")) {
            double targetLat = Double.parseDouble(getArguments().getString("target_lat"));
            double targetLng = Double.parseDouble(getArguments().getString("target_lon"));

            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    LatLng start = new LatLng(location.getLatitude(), location.getLongitude());
                    LatLng end = new LatLng(targetLat, targetLng);

                    currentFilter = MarkerFilter.CURRENT_ROUTE;
                    reloadMarkers();
                    drawRoute(start, end);
                }
            });
        }

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

        taskDao.getAll().observe(getViewLifecycleOwner(), tasks -> {
            this.allTasks = tasks;
            reloadMarkers();
        });
    }

    private void drawRoute(LatLng start, LatLng end) {
        currentRouteTarget = end;

        GoogleDirection.withServerKey(getString(R.string.map_key))
                .from(start)
                .to(end)
                .transportMode(TransportMode.WALKING)
                .execute(new DirectionCallback() {
                    @Override
                    public void onDirectionSuccess(Direction direction) {
                        if (direction.isOK()) {
                            List<Route> routeList = direction.getRouteList();
                            if (!routeList.isEmpty()) {
                                List<LatLng> directionPositionList = routeList.get(0)
                                        .getLegList().get(0)
                                        .getDirectionPoint();

                                mMap.addPolyline(new PolylineOptions()
                                        .addAll(directionPositionList)
                                        .width(10)
                                        .color(ContextCompat.getColor(requireContext(), R.color.teal_700)));

//                                mMap.addMarker(new MarkerOptions().position(start).title("Ваша позиція"));
//                                mMap.addMarker(new MarkerOptions().position(end).title("Задача"));

                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(start, 14f));
                            } else {
                                Toast.makeText(requireContext(), "Маршрут не знайдено", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(requireContext(), "Маршрут не знайдено (відповідь від Google не OK)", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onDirectionFailure(Throwable t) {
                        Toast.makeText(requireContext(), "Помилка побудови маршруту", Toast.LENGTH_SHORT).show();
                        t.printStackTrace();
                    }
                });
    }

    private void reloadMarkers() {
        if (mMap == null || allTasks == null) return;

        mMap.clear();

        for (Task task : allTasks) {
            LatLng position = new LatLng(task.getLatitude(), task.getLongitude());

            boolean shouldShow = false;
            switch (currentFilter) {
                case ALL:
                    shouldShow = true;
                    break;
                case PLANNED_ONLY:
                    shouldShow = "planned".equalsIgnoreCase(task.status);
                    break;
                case CURRENT_ROUTE:
                    if (currentRouteTarget != null) {
                        shouldShow = position.equals(currentRouteTarget);
                    }
                    break;
            }

            if (!shouldShow) continue;

            BitmapDescriptor icon = "done".equalsIgnoreCase(task.status)
                    ? BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                    : BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN);

            mMap.addMarker(new MarkerOptions()
                    .position(position)
                    .title(task.id + ". " + task.title)
                    .snippet(task.completedAt)
                    .icon(icon));
        }

        mMap.setOnInfoWindowClickListener(marker -> {
            String title = marker.getTitle();
            if (title != null && title.contains(".")) {
                try {
                    int taskId = Integer.parseInt(title.split("\\.")[0].trim());
                    Bundle bundle = new Bundle();
                    bundle.putInt("taskId", taskId);
                    Navigation.findNavController(requireView()).navigate(R.id.action_mapsFragment_to_taskDetailsFragment, bundle);
                } catch (NumberFormatException e) {
                    Toast.makeText(requireContext(), "Невірний ID задачі", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);

//            fusedLocationClient.getLastLocation()
//                    .addOnSuccessListener(location -> {
//                        if (location != null) {
//                            LatLng myLatLng = new LatLng(location.getLatitude(), location.getLongitude());
//                            mMap.addMarker(new MarkerOptions()
//                                    .position(myLatLng)
//                                    .title("Ви тут"));
//
//                            saveLastKnownLocation(location.getLatitude(), location.getLongitude());
//                        }
//                    });

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
