package com.example.scalarfish2.ui.home;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.scalarfish2.R;
import com.example.scalarfish2.databinding.FragmentHomeBinding;
import com.example.scalarfish2.ui.calibrate.Calibrate;
import com.example.scalarfish2.ui.camera.Camera;
import com.example.scalarfish2.ui.setPoints.SetPointsActivity;
import com.example.scalarfish2.ui.verify.Verify;

public class HomeFragment extends Fragment implements View.OnClickListener {

    private HomeViewModel homeViewModel;
    private FragmentHomeBinding binding;

    View view;
    Button btnCalibrate;
    Button btnStart;
    Button btnVerify;

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
        btnVerify = (Button) root.findViewById(R.id.btnVerifyMain);
        btnVerify.setOnClickListener(this);
        return root;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnCalibrate:
                Log.i("Button", "Calibrate button pressed.");
                Fragment fragmentCalib = new Calibrate();
                FragmentTransaction transactionCalib = getFragmentManager().beginTransaction();
                transactionCalib.replace(R.id.nav_host_fragment_content_main, fragmentCalib);
                transactionCalib.addToBackStack(null);
                transactionCalib.commit();
                break;
            case R.id.btnCamera:
                Log.i("Button", "Camera button pressed.");
                Fragment fragmentCamera = new Camera();
                FragmentTransaction transactionCamera = getFragmentManager().beginTransaction();
                transactionCamera.replace(R.id.nav_host_fragment_content_main, fragmentCamera);
                transactionCamera.addToBackStack(null);
                transactionCamera.commit();
                break;
            case R.id.btnVerifyMain:
                Log.i("Button", "Verify button pressed.");
                // If the device has not been calibrated yet, don't open the verify fragment
                if(checkForPreviousCalib()) {
                    Fragment fragmentVerify = new Verify();
                    FragmentTransaction transactionVerify = getFragmentManager().beginTransaction();
                    transactionVerify.replace(R.id.nav_host_fragment_content_main, fragmentVerify);
                    transactionVerify.addToBackStack(null);
                    transactionVerify.commit();
                } else {
                    Log.i("Verify Call", "No calibration yet.");
                }
                break;
        }
    }

    // Use this method to check for previous calibration results
    private boolean checkForPreviousCalib() {
        SharedPreferences prefs = getActivity().getPreferences(Context.MODE_PRIVATE);
        String data = prefs.getString("distCoeffs0", "");
        Log.i("Data", data);
        if(data != "") {
            return true;
        } else {
            return false;
        }
    }

    private void openSetPointsActivity() {
        Intent intent = new Intent(getActivity(), SetPointsActivity.class);
        startActivity(intent);
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}