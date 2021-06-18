package com.example.scalarfish2.ui.home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.scalarfish2.R;
import com.example.scalarfish2.databinding.FragmentHomeBinding;
import com.example.scalarfish2.ui.calibrate.Calibrate;

public class HomeFragment extends Fragment implements View.OnClickListener {

    private HomeViewModel homeViewModel;
    private FragmentHomeBinding binding;

    View view;
    Button btnCalibrate;
    Button btnStart;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textHome;
        homeViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                textView.setText(s);
            }
        });

        btnCalibrate = (Button) root.findViewById(R.id.btnCalibrate);
        btnCalibrate.setOnClickListener(this);
        btnStart = (Button) root.findViewById(R.id.btnCamera);
        btnStart.setOnClickListener(this);
        return root;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnCalibrate:
                Log.i("Button", "Calibrate button pressed.");
                Fragment fragment = new Calibrate();
                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                transaction.replace(R.id.nav_host_fragment_content_main, fragment);
                transaction.addToBackStack(null);
                transaction.commit();
                break;
            case R.id.btnCamera:
                Log.i("Button", "Camera button pressed.");
                getActivity().getActionBar().setTitle("Calibration");
                break;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}