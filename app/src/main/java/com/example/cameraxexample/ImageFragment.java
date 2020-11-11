package com.example.cameraxexample;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class ImageFragment extends Fragment {

    TextView testTxt;
    ImageView imageView;
    public ImageFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_image, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        testTxt = view.findViewById(R.id.testTxt);
        testTxt.setText( ImageFragmentArgs.fromBundle(getArguments()).getMyName());

        imageView = view.findViewById(R.id.imageView);
        imageView.setImageBitmap(ImageFragmentArgs.fromBundle(getArguments()).getMyImage());
    }
}