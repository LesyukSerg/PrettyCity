package com.raziel.prettycity.ui;

import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.raziel.prettycity.R;

import java.io.File;

public class FullscreenImageFragment extends Fragment {

    public FullscreenImageFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_fullscreen_image, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        ImageView imageView = view.findViewById(R.id.fullscreen_image_view);
        String imagePath = getArguments() != null ? getArguments().getString("image_path") : null;

        if (imagePath != null) {
            Glide.with(requireContext())
                .load(Uri.fromFile(new File(imagePath)))
                .into(imageView);
        }
//        imageView.setImageBitmap(BitmapFactory.decodeFile(imagePath));

        imageView.setOnClickListener(v -> requireActivity().onBackPressed());
    }
}
