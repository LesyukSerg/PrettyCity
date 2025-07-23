package com.raziel.prettycity.ui;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

import com.raziel.prettycity.R;

public class EditLocationFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap map;
    private LatLng currentLatLng = new LatLng(48.45, 34.983); // default

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_location, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        SupportMapFragment mapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.map_edit);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        view.findViewById(R.id.buttonSaveLocation).setOnClickListener(v -> {
            Bundle result = new Bundle();
            result.putString("latitude", String.valueOf(currentLatLng.latitude));
            result.putString("longitude", String.valueOf(currentLatLng.longitude));

            Navigation.findNavController(v).navigateUp();
            getParentFragmentManager().setFragmentResult("location_result", result);
        });

        if (getArguments() != null) {
            double lat = Double.parseDouble(getArguments().getString("latitude"));
            double lng = Double.parseDouble(getArguments().getString("longitude"));
            currentLatLng = new LatLng(lat, lng);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
        map.addMarker(new MarkerOptions().position(currentLatLng).draggable(true));

        map.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(@NonNull Marker marker) { }

            @Override
            public void onMarkerDrag(@NonNull Marker marker) { }

            @Override
            public void onMarkerDragEnd(@NonNull Marker marker) {
                currentLatLng = marker.getPosition();
            }
        });
    }
}
