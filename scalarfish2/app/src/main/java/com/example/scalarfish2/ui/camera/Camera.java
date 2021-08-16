// Author: Malte Wollermann

package com.example.scalarfish2.ui.camera;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
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
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;

import com.example.scalarfish2.R;
import com.example.scalarfish2.ui.setPoints.SetPointsActivity;
import com.example.scalarfish2.util.CustomCameraView;



import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

public class Camera extends Fragment implements View.OnClickListener, CameraBridgeViewBase.CvCameraViewListener2 {

    // Interface
    View view; /* The view everything is in */

    // Camera view and settings
    CustomCameraView javaCameraView;
    private final int PERMISSIONS_READ_CAMERA=1;
    private Mat mRGBA; /* a matrix for copying the values of the current frame of the camera to */
    private Mat mRGBAcopy;

    // Matrices for distortion and images
    Mat distCoeffs = new Mat(1, 5, CvType.CV_64FC1);
    Mat intrinsic = new Mat(3, 3, CvType.CV_64FC1); /* Intrinsic camera values */
    Mat undistorted = new Mat();
    Mat currentImg = new Mat();

    // UI elements
    ImageButton btnCaptureImg;
    ImageButton btnConfirmImg;
    ImageButton btnCancelImg;
    Switch switchUseCalibrated;
    ImageView imagePreview;




    CameraDevice cameraDevice;
    CameraManager cameraManager;
    String [] cameras;

    CameraCharacteristics cameraCharacteristics;
    float [] focalLength;


    boolean useCalibration = false; /* Value of the switch on top to decide if the calibrated image should be used */

    // Image path and bitmap file
    String lastSavedImgPath;
    Bitmap imgBitmap;

    public Camera() {
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

    public static Camera newInstance(String param1, String param2) {
        Camera fragment = new Camera();
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
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_camera, container, false);

        // When loading the fragment, no matter from where, change the title
        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle("Camera");

        // Get the OpenCV camera view in the fragment's layout
        javaCameraView = (CustomCameraView) view.findViewById(R.id.openCvCameraView3);
        javaCameraView.setCvCameraViewListener(this);
        // Set the front camera to the one that will be used
        javaCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_BACK);

        // Get the buttons and switch from the xml file
        btnCaptureImg = (ImageButton) view.findViewById(R.id.btnCaptureImgCamera);
        btnCaptureImg.setOnClickListener(this);

        btnConfirmImg = (ImageButton) view.findViewById(R.id.btnConfirmImg);
        btnConfirmImg.setOnClickListener(this);
        btnConfirmImg.setVisibility(View.INVISIBLE);

        btnCancelImg = (ImageButton) view.findViewById(R.id.btnCancelImg);
        btnCancelImg.setOnClickListener(this);
        btnCancelImg.setVisibility(View.INVISIBLE);

        imagePreview = (ImageView) view.findViewById(R.id.imageViewCamera);

        /*
        if (cameraManager!=null){
            try {
                cameras= cameraManager.getCameraIdList();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        String cameraId;
        if (cameras!=null){
            cameraId=cameras[0];

            try {
                cameraCharacteristics=cameraManager.getCameraCharacteristics(cameraId);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }


        CameraCharacteristics.Key key=CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS;

        if (key!=null&&cameraCharacteristics!=null){

            focalLength= (float[]) cameraCharacteristics.get(key);
        }

        Log.e("focalLengthArray: ", String.valueOf(focalLength[0]));

         */

        switchUseCalibrated = (Switch) view.findViewById(R.id.switchCalibration);
        // Set the listener for the switch, since it needs its own
        switchUseCalibrated.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // The status of the switch decides if we want to use the calibrated image or not
                if(isChecked) {
                    useCalibration = true;
                } else {
                    useCalibration = false;
                }
            }
        });

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

        // Calibration should not be done everytime the user starts the app, so the distortion matrix is saved in the shared preferences
        // Since the matrix is a C++ object and in shared preferences only integers, floats and strings can be saved, the entries must be read out one by one
        SharedPreferences prefs = getActivity().getPreferences(Context.MODE_PRIVATE);
        for(int i = 0; i < 5; i++) {
            // Reading out the single matrix values
            String data = prefs.getString("distCoeffs"+i, "");
            // Putting the values into the matrix object of the distortion matrix
            distCoeffs.put(0, i, Double.valueOf(data));
        }

        // Debug
        Log.i("distCoeffs", distCoeffs.dump());
        Log.i("distCoeffs", distCoeffs.toString());

        // Intrinsic camera parameters
        // Same idea as above, read out the previously set intrinsic camera values and apply them
        for(int i = 0; i < 3; i++) {
            for(int j = 0; j < 3; j++) {
                String data = prefs.getString("intrinsic"+i+j, "");
                intrinsic.put(i, j, Double.valueOf(data));
            }
        }
        return view;
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.btnCaptureImgCamera:
                // This randomly throws an error on occasion when trying to create the bitmap: Width and Height must be > 0, not sure why at the moment.
                // For now, a try-catch block solves this
                try {
                    // Copy the current matrix of the camera to a new object, create a bitmap file from it, change button visibilities
                    mRGBAcopy.copyTo(currentImg);
                    imgBitmap = createBitmap(currentImg);
                    imagePreview.setImageBitmap(imgBitmap);
                    javaCameraView.setVisibility(View.INVISIBLE);
                    btnConfirmImg.setVisibility(View.VISIBLE);
                    btnCancelImg.setVisibility(View.VISIBLE);
                    btnCaptureImg.setVisibility(View.INVISIBLE);
                } catch(Exception e) {
                    Log.e("Error", e.toString());
                }
                break;
            case R.id.btnConfirmImg:
                // If confirmed, save the bitmap file to the device and start the activity to calculate the angles
                saveBitmap(imgBitmap);
                Intent intent = new Intent(getActivity(), SetPointsActivity.class);
                startActivity(intent);
                break;
            case R.id.btnCancelImg:
                // If canceled, change button visibilities back and reset the matrix object
                currentImg = new Mat();
                btnCancelImg.setVisibility(View.INVISIBLE);
                btnConfirmImg.setVisibility(View.INVISIBLE);
                btnCaptureImg.setVisibility(View.VISIBLE);
                javaCameraView.setVisibility(View.VISIBLE);
                break;
        }
    }

    private Bitmap createBitmap(Mat source) {
        Log.i("Source", source.toString());
        Bitmap bmp = null;
        Mat rgb = new Mat();

        //Imgproc.cvtColor(source, rgb, Imgproc.COLOR_BGR2RGB);
        rgb = source;

        // Rotate the image by 90 degrees, as it is always rotated - no idea why. This works without loss of data anyways.
        Mat rotated = new Mat();
        Core.rotate(rgb, rotated, Core.ROTATE_90_CLOCKWISE);

        try {
            // Creation of the actual bitmap file: First, set the size, based on the matrix size
            bmp = Bitmap.createBitmap(rotated.cols(), rotated.rows(), Bitmap.Config.ARGB_8888);
            // Then create the image from the matrix
            Utils.matToBitmap(rotated, bmp);
        }
        catch(CvException e) {
            Log.d("Exception", e.getMessage());
        }
        return bmp;
    }

    private void saveBitmap(Bitmap bmp) {
        // Create a file name for the image
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = timeStamp + ".jpg";

        try {
            // Save the file to the device
            FileOutputStream fileOutputStream = getContext().openFileOutput(imageFileName, Context.MODE_PRIVATE);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
            fileOutputStream.close();
            Log.i("ImgSaved", "Image file saved");
        } catch(Exception e) {
            e.printStackTrace();
        }

        // Get the path to the image file last saved. There might be an easier way, but this works for now
        lastSavedImgPath = getContext().getFilesDir().listFiles()[getContext().getFilesDir().listFiles().length-1].toString();
        Log.i("LastSavedImgPath", lastSavedImgPath);

        // Save the path to the image in shared preferences, to be read out when calculating the angle
        SharedPreferences prefs = getContext().getSharedPreferences("lastImage",Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("lastFilePath", lastSavedImgPath);
        editor.commit();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        Log.d("Camera", "onCameraViewStarted");
        mRGBA = new Mat(height, width, CvType.CV_8UC4);
        // Debug
        Log.i("mRGBA size", "Size on camera start: Height: " + height + ", Width: " + width); /* 720x960 */
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
        ((AppCompatActivity)getActivity()).getSupportActionBar().hide();
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
    public void onStop() {
        super.onStop();
        ((AppCompatActivity)getActivity()).getSupportActionBar().show();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRGBA = inputFrame.rgba();
        mRGBAcopy = new Mat();

        // Check if we want to return the undistorted image or the raw camera output
        // This is only for our prototype: The final app would only show the undistorted image
        if(useCalibration) {
            Calib3d.undistort(mRGBA, undistorted, intrinsic, distCoeffs);
            // To save the current frame in one object, no matter if distorted or not, we copy the value to that matrix
            mRGBAcopy = undistorted;
            Log.i("cameraFrameUndistorted", mRGBAcopy.toString());
            return mRGBAcopy;
        } else {
            mRGBA.copyTo(mRGBAcopy);
            return mRGBAcopy;
        }
    }
}