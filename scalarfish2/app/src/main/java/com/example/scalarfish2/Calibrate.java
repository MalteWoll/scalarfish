package com.example.scalarfish2;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.core.MatOfPoint2f;
import org.opencv.imgproc.Imgproc;
import org.opencv.calib3d.*;

public class Calibrate extends Fragment implements View.OnClickListener, CameraBridgeViewBase.CvCameraViewListener2 {
    private static final int MY_CAMERA_REQUEST_CODE = 100;

    // For image capturing
    static final int REQUEST_IMAGE_CAPTURE = 1;

    // For accessing the elements in the fragment
    ImageView imgView;
    View view;
    Button btnCaptureImg;

    // For creating a path to save the image to
    String currentPhotoPath;

    Uri photoURI;
    File photoFile;

    Calib3d calib = new Calib3d();

    // OpenCV camera
    private JavaCameraView javaCameraView;
    private Mat mRGBA, mRGBAT;
    private final int PERMISSIONS_READ_CAMERA=1;
    int cameraCounter = 0; /* for counting and reducing fps */

    // Calibration values
    Size boardSize = new Size(9,6);
    MatOfPoint2f imageCorners = new MatOfPoint2f();

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

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

    public Calibrate() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment Calibrate.
     */
    // TODO: Rename and change types and number of parameters
    public static Calibrate newInstance(String param1, String param2) {
        Calibrate fragment = new Calibrate();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.fragment_calibrate, container, false);

        // Get the button for capturing a calibration image and set the listener
        btnCaptureImg = (Button) view.findViewById(R.id.btnCaptureImg);
        btnCaptureImg.setOnClickListener(this);


        // Get the OpenCV camera view in the fragment's layout
        javaCameraView = (JavaCameraView) view.findViewById(R.id.openCvCameraView);
        javaCameraView.setCvCameraViewListener(this);
        // Set the front camera to the one that will be used
        javaCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_BACK);

        // Get the image view to display the captured image on
        imgView = (ImageView) view.findViewById(R.id.imgViewCalibration);

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

    // Creates an unique image file name
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        // getActivity() is required for calling the function, since this is a fragment
        File storageDir = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",   /* suffix */
                storageDir      /* directory */
        );

        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    // Open the camera, take a picture and save the picture
    // This uses intents to open the android camera, ideally this will not be necessary and we will use the opencv camera
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            photoFile = null;
            try {
                photoFile = createImageFile();
                Log.i("File created", "Image file created: " + photoFile.toString());
            } catch (IOException ex) {
                // Error while creating file
                Log.e("File creation error", ex.toString());
            }

            if (photoFile != null) {
                photoURI = FileProvider.getUriForFile(getActivity(), /* 'getActivity' was 'this' in the example, but this is a fragment, not an activity */
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.btnCaptureImg:
                // Make the OpenCV camera view visible when the button is pressed
                javaCameraView.setVisibility(SurfaceView.VISIBLE);
                // Disable the button
                btnCaptureImg.setVisibility(SurfaceView.INVISIBLE);
                break;
        }
    }


    // Testing openCV with a filter
    public Bitmap openCvCannyFilter(File imgFile) {
        Mat img = new Mat();

        // Get the path of the image file and covert the file to a bitmap
        Bitmap srcBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
        Bitmap bmp32 = srcBitmap.copy(Bitmap.Config.ARGB_8888, true);

        // Convert the bitmap to a Mat
        Utils.bitmapToMat(bmp32, img);

        // OpenCV stuff
        Imgproc.cvtColor(img, img, Imgproc.COLOR_RGB2BGRA);
        Mat img_result = img.clone();
        Imgproc.Canny(img, img_result, 80, 90);
        Bitmap img_bitmap = Bitmap.createBitmap(img_result.cols(), img_result.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(img_result, img_bitmap);

        return img_bitmap;
    }

    // Detecting the chessboard pattern in a Mat variable, corners are saved to the imageCorners variable, returns true if chessboard is detected
    public boolean chessboardDetection(Mat img_result) {

        boolean found = calib.findChessboardCorners(img_result, boardSize, imageCorners, Calib3d.CALIB_CB_ADAPTIVE_THRESH + Calib3d.CALIB_CB_NORMALIZE_IMAGE + Calib3d.CALIB_CB_FAST_CHECK);

        //imageCorners.release();
        img_result.release();

        return found;
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        Log.d("Camera", "onCameraViewStarted");
        mRGBA = new Mat(height, width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        Log.d("Camera", "onCameraViewStopped");
        mRGBA.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Log.d("Camera", "onCameraFrame");

        Mat frame = inputFrame.rgba();

        cameraCounter++;
        // Limit the fps, increase the number for less fps -> should help with processing
        if(cameraCounter < 6) {
            return null;
        } else {
            // Find the chessboard in the live view

            if (chessboardDetection(frame)) {
                Log.d("Chessboard", "Chessboard true");
            } else {
                Log.d("Chessboard", "Chessboard false");
            }
            //Calib3d.drawChessboardCorners(frame, boardSize, imageCorners, chessboardDetection(frame)); /* This replaces the above lines */
            return frame;
        }
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
}