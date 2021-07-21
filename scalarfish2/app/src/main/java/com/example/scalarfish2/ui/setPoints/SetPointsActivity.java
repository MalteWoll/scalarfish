package com.example.scalarfish2.ui.setPoints;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.scalarfish2.R;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;

import java.util.Vector;

public class SetPointsActivity extends AppCompatActivity implements View.OnClickListener{
    static ImageView imageView;
    View view;
    int maxPoints;
    BitmapDrawable drawable;
    Bitmap bitmap;
    Bitmap mutableBitmap;
    static Canvas canvas;
    Button btnCalibrateAngle;
    static TextView txtCalculatedAngle;
    static int drawCase;
    Bitmap currentImg;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_points);

        /*
        Intent intent = getIntent();
        byte[] tmp = intent.getByteArrayExtra("currentImg");
        Log.i("Img:", tmp.toString());
        currentImg = BitmapFactory.decodeByteArray(tmp, 0, tmp.length);
        */
        imageView = findViewById(R.id.setPointsImage);
        //imageView.setImageBitmap(currentImg);

        SharedPreferences prefs = getSharedPreferences("lastImage", Context.MODE_PRIVATE);
        String lastImagePath = prefs.getString("lastFilePath", "");
        Log.i("lastImage", "Last image path: " + lastImagePath);
        imageView.setImageURI(Uri.parse(lastImagePath));
        maxPoints = 0;

        //drawable = (BitmapDrawable) imageView.getDrawable();
        //mutableBitmap = currentImg.copy(Bitmap.Config.ARGB_8888, true);
        //canvas = new Canvas(mutableBitmap);

        btnCalibrateAngle = findViewById(R.id.btnCalculateAngle);
        btnCalibrateAngle.setOnClickListener(this);

        txtCalculatedAngle = findViewById(R.id.txtCalculatedAngle);

        drawCase = 0;

        /*imageView.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View view, MotionEvent event) {

                    // Get the coordinates of the touch point x, y
                    float x = event.getX();
                    float y = event.getY();
                    // The coordinates of the target point
                    float dst[] = new float[2];
                    // Get the matrix of ImageView
                    Matrix imageMatrix = imageView.getImageMatrix();
                    // Create an inverse matrix
                    Matrix inverseMatrix = new Matrix();
                    // Inverse, the inverse matrix is ​​assigned
                    imageMatrix.invert(inverseMatrix);

                    // Get the value of the target point dst through the inverse matrix mapping
                    inverseMatrix.mapPoints(dst, new float[]{x, y});
                    float dstX = dst[0];
                    float dstY = dst[1];
                    Log.i("coords", "x: " + String.valueOf(dstX) + ", y: " + String.valueOf(dstY));
                    canvas.drawColor(Color.RED);
                    canvas.drawCircle(dstX, dstY, 100f, paint);
                    maxPoints++;
                    Log.i("maxPoints", String.valueOf(maxPoints));
                    // Determine the position of dstX, dstY on the Bitmap
                    return true;
            }*/
        //});

    }

    @Override
    public void onClick(View view){
        switch(view.getId()){
            case R.id.btnCalculateAngle:
                Log.i("btn", "Calculate Angle");
                float angle = DrawingImageView.calculateAngle();
                Log.i("Angle:", String.valueOf(angle));
        }
    }


    public static class DrawingImageView extends androidx.appcompat.widget.AppCompatImageView {
        public static PointF point1;
        public static PointF point2;
        private static Paint paint = new Paint();

        public DrawingImageView(Context context) {
            super(context);
        }

        public DrawingImageView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public DrawingImageView(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
        }

        @Override
        public boolean onTouchEvent(@NonNull MotionEvent event) {
            float x = event.getX();
            float y = event.getY();
            Log.i("coords", "x: " + String.valueOf(x) + ", y: " + String.valueOf(y));

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if(point1 == null){
                        point1 = new PointF(x, y);
                        invalidate();
                        break;
                    }if(point2 == null){
                        point2 = new PointF(x, y);
                        invalidate();
                        break;
                    }else {
                        Log.i("Max Points reached", "2");
                        break;
                    }
                }

                /*case MotionEvent.ACTION_MOVE:
                    point.set(x, y);
                    invalidate();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    point = null;
                    invalidate();
                    break;*/
            return true;
        }

        @Override
        protected void onDraw(@NonNull Canvas canvas) {
            super.onDraw(canvas);
            if (point1 != null) {
                canvas.drawCircle(point1.x, point1.y, 20, paint);
            }
            if (point2 != null) {
                canvas.drawCircle(point2.x, point2.y, 20, paint);
                canvas.drawCircle(imageView.getWidth()/2, imageView.getHeight(), 20, paint);
            }
        }

        public static float calculateAngle(){
            PointF eyePoint = new PointF(imageView.getWidth()/2, imageView.getHeight());
            Log.i("eyePoint", String.valueOf(eyePoint));
            PointF a = new PointF(point1.x - eyePoint.x, point1.y - eyePoint.y);
            PointF b = new PointF(point2.x - eyePoint.x, point2.y - eyePoint.y);
            Log.i("a:", String.valueOf(a));
            Log.i("b:", String.valueOf(b));
            float angle = (float) Math.toDegrees(Math.acos(((a.x* b.x) + (a.y * b.y))/ (a.length() * b.length())));
            Log.i("Angle:", String.valueOf(angle));
            txtCalculatedAngle.setText(String.valueOf(angle));

            return angle;
        }
    }
}