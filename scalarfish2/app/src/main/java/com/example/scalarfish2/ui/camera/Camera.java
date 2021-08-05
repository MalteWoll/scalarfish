// https://github.com/opencv/opencv/tree/master/samples/android/tutorial-3-cameracontrol/src/org/opencv/samples/tutorial3
// https://github.com/opencv/opencv/tree/master/samples/android

package com.example.scalarfish2.ui.camera;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import com.example.scalarfish2.R;
import com.example.scalarfish2.ui.setPoints.SetPointsActivity;
import com.example.scalarfish2.ui.verify.Verify;
import com.example.scalarfish2.util.CustomCameraView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCamera2View;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class Camera extends Fragment implements View.OnClickListener, CameraBridgeViewBase.CvCameraViewListener2 {

    // Interface
    View view; /* The view everything is in */

    CustomCameraView javaCameraView;
    private final int PERMISSIONS_READ_CAMERA=1;
    private Mat mRGBA; /* a matrix for copying the values of the current frame of the camera to */
    private Mat mRGBAcopy;
    int cameraCounter = 0; /* for counting and reducing fps */

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

    boolean useCalibration = false; /* Value of the switch on top to decide if the calibrated image should be used */

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

        /* This shows that the preview does not "stretch" above the size of the display, but that it is created with false dimensions
        javaCameraView.setScaleX(0.5f);
        javaCameraView.setScaleY(0.5f);
         */

        // Get the buttons and switch
        btnCaptureImg = (ImageButton) view.findViewById(R.id.btnCaptureImgCamera);
        btnCaptureImg.setOnClickListener(this);

        btnConfirmImg = (ImageButton) view.findViewById(R.id.btnConfirmImg);
        btnConfirmImg.setOnClickListener(this);
        btnConfirmImg.setVisibility(View.INVISIBLE);

        btnCancelImg = (ImageButton) view.findViewById(R.id.btnCancelImg);
        btnCancelImg.setOnClickListener(this);
        btnCancelImg.setVisibility(View.INVISIBLE);

        imagePreview = (ImageView) view.findViewById(R.id.imageViewCamera);

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
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.btnCaptureImgCamera:
                mRGBAcopy.copyTo(currentImg);

                //javaCameraView.disableView(); /* Disabling the camera view deletes the values of the Mat objects. Why? How to circumvent and keep values? */

                imgBitmap = createBitmap(currentImg);

                //imgBitmap = createBitmapByTakingImage();
                imagePreview.setImageBitmap(imgBitmap);

                // Hide the camera view to display the taken image
                javaCameraView.setVisibility(View.INVISIBLE);

                btnConfirmImg.setVisibility(View.VISIBLE);
                btnCancelImg.setVisibility(View.VISIBLE);
                btnCaptureImg.setVisibility(View.INVISIBLE);
                break;
            case R.id.btnConfirmImg:
                //createAndSaveBitmap(currentImg);
                /*ByteArrayOutputStream bStream = new ByteArrayOutputStream();
                imgBitmap.compress(Bitmap.CompressFormat.PNG, 100, bStream);
                byte[] byteArray = bStream.toByteArray();
                Intent i = new Intent(getActivity(), SetPointsActivity.class);
                i.putExtra("currentImg", byteArray);
                startActivity(i);*/
                saveBitmap(imgBitmap);
                Intent intent = new Intent(getActivity(), SetPointsActivity.class);
                startActivity(intent);
                break;
            case R.id.btnCancelImg:
                currentImg = new Mat();
                btnCancelImg.setVisibility(View.INVISIBLE);
                btnConfirmImg.setVisibility(View.INVISIBLE);
                btnCaptureImg.setVisibility(View.VISIBLE);
                javaCameraView.setVisibility(View.VISIBLE);
                break;
        }
    }

    private Bitmap createBitmapByTakingImage() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        String currentDateandTime = sdf.format(new Date());
        String fileName = Environment.getStorageDirectory().getPath() + "/img_" + currentDateandTime + ".jpg";

        javaCameraView.takePicture(fileName);
        Log.i("ImageSaved", fileName + " saved");
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        Bitmap bmp = BitmapFactory.decodeFile(fileName, bmOptions);
        return bmp;
    }

    // TODO: Maybe move this to another thread
    private Bitmap createBitmap(Mat source) {
        Log.i("Source", source.toString());
        Bitmap bmp = null;
        Mat rgb = new Mat();

        //Imgproc.cvtColor(source, rgb, Imgproc.COLOR_BGR2RGB);
        rgb = source;

        // Rotate the image by 90 degrees
        Mat rotated = new Mat();
        Core.rotate(rgb, rotated, Core.ROTATE_90_CLOCKWISE);

        try {
            bmp = Bitmap.createBitmap(rotated.cols(), rotated.rows(), Bitmap.Config.ARGB_8888);
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

        SharedPreferences prefs = getContext().getSharedPreferences("lastImage",Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("lastFilePath", lastSavedImgPath);
        editor.commit();
    }

    public void onPictureTaken(byte[] data, Camera camera) {
        imgBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
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
        if(useCalibration) {
            Calib3d.undistort(mRGBA, undistorted, intrinsic, distCoeffs);
            // To save the current frame in one object, no matter if distorted or not, we copy the value to that matrix
            mRGBAcopy = undistorted;
            return mRGBAcopy;
        } else {
            mRGBA.copyTo(mRGBAcopy);
            return mRGBAcopy;
        }
    }
}