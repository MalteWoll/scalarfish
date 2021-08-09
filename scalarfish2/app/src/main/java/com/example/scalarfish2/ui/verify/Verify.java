// Author: Malte Wollermann

package com.example.scalarfish2.ui.verify;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.scalarfish2.R;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class Verify extends Fragment implements View.OnClickListener, CameraBridgeViewBase.CvCameraViewListener2 {
    View view;
    Button btnTakeImg;

    JavaCameraView javaCameraView;
    private final int PERMISSIONS_READ_CAMERA=1;
    private Mat mRGBA; /* a matrix for copying the values of the current frame of the camera to */

    Mat distCoeffs = new Mat(1, 5, CvType.CV_64FC1);
    Mat intrinsic = new Mat(3, 3, CvType.CV_64FC1); /* Intrinsic camera values */

    Size boardSize = new Size(9,6); /* The size of the chessboard */
    MatOfPoint2f imageCorners = new MatOfPoint2f(); /* A matrix for detecting the corners */
    MatOfPoint2f imageCornerCopy = new MatOfPoint2f(); /* A matrix for detecting the corners */
    Mat savedImage = new Mat(); /* saving a captured image from the camera for setting the image dimensions when calibrating */

    Mat verificationImg = new MatOfPoint2f();
    Mat undistorted = new Mat();

    boolean found = false;
    boolean debug = true;

    public Verify() {
        // Required empty public constructor
    }

    // For enabling the camera view
    BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(getContext()) {
        @Override
        public void onManagerConnected(int status) {
            Log.d("Callback", "callbacksuccess");
            switch (status)
            {
                case BaseLoaderCallback.SUCCESS:
                {
                    Log.d("Callback", "case success");
                    javaCameraView.enableView();
                    break;
                }
                default:
                {
                    Log.d("Callback", "case default");
                    super.onManagerConnected(status);
                    break;
                }

            }
        }
    };

    public static Verify newInstance(String param1, String param2) {
        Verify fragment = new Verify();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_verify, container, false);

        // When loading the fragment, no matter from where, change the title
        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle("Verify Calibration");

        // Get the OpenCV camera view in the fragment's layout
        javaCameraView = (JavaCameraView) view.findViewById(R.id.openCvCameraView3);
        javaCameraView.setCvCameraViewListener(this);
        // Set the front camera to the one that will be used
        javaCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_BACK);

        btnTakeImg = (Button) view.findViewById(R.id.btnTakeImage);
        btnTakeImg.setOnClickListener(this::onClick);

        // Permission check for camera
        if (ContextCompat.checkSelfPermission(getContext(),
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                    Manifest.permission.CAMERA)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{Manifest.permission.CAMERA},
                        PERMISSIONS_READ_CAMERA);
                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            Log.d("Permissions", "Permissions granted");
            javaCameraView.setCameraPermissionGranted();
            // Permission has already been granted
        }

        // Read out the values for the distortion matrix
        // TODO: Some catch if the matrix is empty
        SharedPreferences prefs = getActivity().getPreferences(Context.MODE_PRIVATE);
        for(int i = 0; i < 5; i++) {
            String data = prefs.getString("distCoeffs"+i, "");
            distCoeffs.put(0, i, Double.valueOf(data));
        }

        Log.i("distCoeffs", distCoeffs.dump());
        Log.i("distCoeffs", distCoeffs.toString());

        // Intrinsic camera parameters
        for(int i = 0; i < 3; i++) {
            for(int j = 0; j < 3; j++) {
                String data = prefs.getString("intrinsic"+i+j, "");
                intrinsic.put(i, j, Double.valueOf(data));
            }
        }

        return view;
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        // Ensure that this result is for the camera permission request
        if (requestCode == PERMISSIONS_READ_CAMERA) {
            // Check if the request was granted or denied
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // The request was granted -> tell the camera view
                javaCameraView.setCameraPermissionGranted();
            } else {
                // The request was denied -> tell the user and exit the application
                Log.d("Permission", "Permission was denied");
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.btnTakeImage:
                distanceBetweenPoints();
                javaCameraView.disableView();
                break;
        }
    }

    // Detecting the chessboard pattern in a Mat variable, corners are saved to the imageCorners variable, returns true if chessboard is detected
    public boolean chessboardDetection(Mat img_result) {
        // TODO: Everything in this method on another thread (maybe? Is that a good idea?)
        boolean found = Calib3d.findChessboardCorners(img_result, boardSize, imageCorners, Calib3d.CALIB_CB_ADAPTIVE_THRESH + Calib3d.CALIB_CB_NORMALIZE_IMAGE + Calib3d.CALIB_CB_FAST_CHECK);

        if(found) {
            // When a chessboard has been detected, save the imageCorners by adding it to the list of corners
            imageCornerCopy = imageCorners;
            imageCorners = new MatOfPoint2f();

            img_result.copyTo(savedImage);
        }
        img_result.release(); /* Release the matrix manually, since Java doesn't detect the size behind it */
        return found;
    }

    // To verify the calibration, we measure the distance between points for a taken image of the chessboard. Equal distances between the points of a row mean good calibration results
    public void distanceBetweenPoints() {
        boolean chessboardFound = chessboardDetection(savedImage);
        Log.i("inMethod", "distBetweenPoints");
        if(chessboardFound) {
            Log.i("inMethod", "distBetweenPoints - chessboard found");

            // Calculate the distance between points of the chessboard. If the distances are equal, the camera should be calculated correctly.
            for(int i = 0; i < (boardSize.width * boardSize.height - 1); i++) {
                double pow1 = Math.pow((imageCornerCopy.get(i+1,0)[0] - imageCornerCopy.get(i,0)[0]), 2);
                double pow2 = Math.pow((imageCornerCopy.get(i+1,0)[1] - imageCornerCopy.get(i,0)[1]), 2);
                double dist = Math.sqrt(pow1 + pow2);
                Log.i("Distance", "Distance " + i + ": " + dist);
            }
        } else {
            Log.i("inMethod", "distBetweenPoints - chessboard not found");
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        Log.d("Camera", "onCameraViewStarted");
        mRGBA = new Mat(height, width, CvType.CV_8UC4);
        //Log.i("mRGBA size", "Size on camera start: Height: " + height + ", Width: " + width); /* 720x960 */
    }

    @Override
    public void onCameraViewStopped() {
        Log.d("Camera", "onCameraViewStopped");
        mRGBA.release();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("onDestroy", "onDestroy");
        if (javaCameraView != null)
        {
            javaCameraView.disableView();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d("onPause", "onPause");
        if (javaCameraView != null)
        {
            javaCameraView.disableView();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("onResume", "onResume");
        if (OpenCVLoader.initDebug())
        {
            Log.d("OpenCV", "OpenCV is initialised again");
            baseLoaderCallback.onManagerConnected((BaseLoaderCallback.SUCCESS));
        }
        else
        {
            Log.d("OpenCV", "OpenCV is not working");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, getContext(), baseLoaderCallback);
        }
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        // Grab the frame shown in the camera and assign it to the mRGBA variable
        mRGBA = inputFrame.rgba();
        Mat grayImage = new Mat();

        Calib3d.undistort(mRGBA, undistorted, intrinsic, distCoeffs);

        Imgproc.cvtColor(undistorted, grayImage, Imgproc.COLOR_BGR2GRAY);

        if(debug) {
            Log.i("intrinsic", intrinsic.toString());
            Log.i("intrinsic", intrinsic.dump());
            debug = false;
        }

        found = chessboardDetection(grayImage);

        if (found) {
            Log.d("Chessboard", "Chessboard true");
            Calib3d.drawChessboardCorners(undistorted, boardSize, imageCornerCopy, found);
        } else {
            Log.d("Chessboard", "Chessboard false");
        }

        return undistorted;
    }
}