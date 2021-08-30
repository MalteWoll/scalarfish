package com.example.scalarfish2.ui.calibrate;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;

import org.jetbrains.annotations.NotNull;

public class CalibrateDialog extends AppCompatDialogFragment {

    @NotNull
    @Override
    public Dialog onCreateDialog(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("How to calibrate")
                .setMessage("To achieve the best calibration we recommend to take 50 pictures of the" +
                        " chessboard. Make sure to cover different perspective views and rotate your" +
                        " phone 90 degrees. Everytime the counter in the upper right corner goes up " +
                        "the app was able to detect the calibration chart")
                .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        return builder.create();
    };
}
