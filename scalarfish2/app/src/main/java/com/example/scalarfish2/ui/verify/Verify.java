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
import android.widget.TextView;
import android.widget.Toast;

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

import java.util.ArrayList;
import java.util.List;

public class Verify extends Fragment implements View.OnClickListener, CameraBridgeViewBase.CvCameraViewListener2 {
    View view;
    Button btnTakeImg;
    TextView txtValues;

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

    List<Double> distances = new ArrayList<>();

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

        txtValues = (TextView) view.findViewById(R.id.textViewValues);

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
                //javaCameraView.disableView();
                break;
        }
    }

    // Detecting the chessboard pattern in a Mat variable, corners are saved to the imageCorners variable, returns true if chessboard is detected
    public boolean chessboardDetection(Mat img_result) {
        boolean found = false;
        try {
            found = Calib3d.findChessboardCorners(img_result, boardSize, imageCorners, Calib3d.CALIB_CB_ADAPTIVE_THRESH + Calib3d.CALIB_CB_NORMALIZE_IMAGE + Calib3d.CALIB_CB_FAST_CHECK);
        } catch(Exception e) {
            Log.e("Error", e.toString());
        }

        if(found) {
            // When a chessboard has been detected, save the imageCorners by adding it to the list of corners
            imageCornerCopy = imageCorners;
            imageCorners = new MatOfPoint2f();

            img_result.copyTo(savedImage);
        }
        img_result.release(); /* Release the matrix manually, since Java doesn't detect the size behind it */
        return found;
    }

    // To verify the calibration, we measure the distance between points for a taken image of the chessboard. Equal distances between the points mean good calibration results
    public void distanceBetweenPoints() {
        boolean chessboardFound = chessboardDetection(savedImage);
        Log.i("inMethod", "distBetweenPoints");
        if(chessboardFound && imageCornerCopy.size().height > 0 && imageCornerCopy.size().width > 0) {
            Log.i("inMethod", "distBetweenPoints - chessboard found");
            distances.clear();
            int counter = 0;
            // Calculate the distance between points of the chessboard. If the distances are equal, the camera should be calibrated correctly.
            for(int i = 0; i < (boardSize.width * boardSize.height - 1); i++) {
                // Horizontal distances between points: Take a point and the next point in the array, calculate the distance between them
                double pow1 = Math.pow((imageCornerCopy.get(i+1,0)[0] - imageCornerCopy.get(i,0)[0]), 2);
                double pow2 = Math.pow((imageCornerCopy.get(i+1,0)[1] - imageCornerCopy.get(i,0)[1]), 2);
                double dist = Math.sqrt(pow1 + pow2);

                // Vertical distances between points: Take a point and the point in the next row, in this case +9, calculate the distance. Skip the last row, since there is no next row with points
                if((i + 9) < (boardSize.width * boardSize.height - 1)){
                    double pow3 = Math.pow((imageCornerCopy.get(i + 9, 0)[0] - imageCornerCopy.get(i, 0)[0]), 2);
                    double pow4 = Math.pow((imageCornerCopy.get(i + 9, 0)[1] - imageCornerCopy.get(i, 0)[1]), 2);
                    double dist2 = Math.sqrt(pow3 + pow4);
                    Log.i("VerticalDistanceAdded", i + ": " + String.valueOf(dist2));
                    distances.add(dist2);
                }

                // This makes sure to not add the distances between the last corner of a row and the first corner of the next row
                counter++;
                if(counter % 9 == 0) {
                    Log.i("HorizontalDistanceAdded", i + ": Deleted, " + String.valueOf(dist));
                    counter = 0;
                } else {
                    distances.add(dist);
                    Log.i("HorizontalDistanceAdded", i + ": " + String.valueOf(dist));
                }
            }
            Log.i("Distance size", String.valueOf(distances.size()));

            // Calculating the average distance between two points
            double avg = 0;
            for(int i = 0; i < distances.size(); i++) {
                avg += distances.get(i);
            }
            avg = avg / ((int)distances.size());
            Log.i("Average distance", String.valueOf(avg));

            // Calculating the standard deviation. For now, we do not use this.
            double var = 0;
            for(int i = 0; i < distances.size(); i++) {
                var += Math.pow(distances.get(i)-avg, 2);
            }
            var = var / (distances.size()-1);
            double standardDeviation = Math.sqrt(var);
            Log.i("Standard Deviation", String.valueOf(standardDeviation));

            // Calculating the mean absolute deviationas measurement of error in the calibration result
            double meanAbsoluteDeviation = 0;
            for(int i = 0; i < distances.size(); i++) {
                meanAbsoluteDeviation += Math.abs(distances.get(i) - avg);
            }
            meanAbsoluteDeviation = meanAbsoluteDeviation / distances.size();
            Log.i("MeanAbsoluteDeviation", String.valueOf(meanAbsoluteDeviation));

            CharSequence text = "Average distance: " + String.valueOf(avg) + "; Mean absolute deviation: " + String.valueOf(meanAbsoluteDeviation);
            Toast toast = Toast.makeText(getContext(), text, Toast.LENGTH_LONG);
            toast.show();

            txtValues.setText(text);

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

        // Undistort the image with the calibration result
        Calib3d.undistort(mRGBA, undistorted, intrinsic, distCoeffs);

        // Convert the image to a grayscale for faster calculations
        Imgproc.cvtColor(undistorted, grayImage, Imgproc.COLOR_BGR2GRAY);

        // Debug
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