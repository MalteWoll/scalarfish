package com.example.scalarfish2.ui.setPoints;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Magnifier;
import android.widget.TextView;

import com.example.scalarfish2.R;

public class SetPointsActivity extends AppCompatActivity implements View.OnClickListener {
    static ImageView imageView;
    int maxPoints;
    Button btnCalibrateAngle;
    Button btnResetPoints;
    static TextView txtCalculatedAngle;
    static int drawCase;
    static Magnifier magnifier;
    static Magnifier.Builder test;
    static String version;

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_points);

        imageView = findViewById(R.id.setPointsImage);

        magnifier = new Magnifier(imageView);
        //magnifier.setZoom(3.0f);
        version = android.os.Build.VERSION.SDK ;
        Log.i("Version:", version);
        if(Integer.parseInt(version) >= 29) {
            test=new Magnifier.Builder(imageView);
            test.setInitialZoom(2.0f);
            test.setSize(200,200);
            test.setCornerRadius(50);

            magnifier=test.build();
        }



        SharedPreferences prefs = getSharedPreferences("lastImage", Context.MODE_PRIVATE);
        String lastImagePath = prefs.getString("lastFilePath", "");
        Log.i("lastImage", "Last image path: " + lastImagePath);
        imageView.setImageURI(Uri.parse(lastImagePath));
        maxPoints = 0;

        btnCalibrateAngle = findViewById(R.id.btnCalculateAngle);
        btnResetPoints = findViewById(R.id.btnResetPoints);
        btnCalibrateAngle.setOnClickListener(this);
        btnResetPoints.setOnClickListener(this);

        txtCalculatedAngle = findViewById(R.id.txtCalculatedAngle);

        drawCase = 0;
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnCalculateAngle:
                Log.i("btn", "Calculate Angle");
                float angle = DrawingImageView.calculateAngle();
                Log.i("Angle:", String.valueOf(angle));
            case R.id.btnResetPoints:
                DrawingImageView.removePoints();
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.P)
    public static class DrawingImageView extends androidx.appcompat.widget.AppCompatImageView {

        /*
        public Builder(DrawingImageView drawingImageView){
            super();

        }

         */


        public static PointF eyePoint;
        public static PointF point1;
        public static PointF point2;
        private static Paint paint = new Paint();

        private static PointF tmpPoint;




        public DrawingImageView(Context context) {
            super(context);
        }

        public DrawingImageView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public DrawingImageView(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
        }

        @RequiresApi(api = Build.VERSION_CODES.Q)
        @Override
        public boolean onTouchEvent(@NonNull MotionEvent event) {
            float x = event.getX();
            float y = event.getY();
            Log.i("coords", "x: " + String.valueOf(x) + ", y: " + String.valueOf(y));




            if(Integer.parseInt(version) >= 29) {
                //magnifier.setZoom(2.0f);
                magnifier.show(event.getX(), event.getY(), event.getX(), event.getY() - 150);
            }





            //y-100 als offset für lupe

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    //tmpPoint=new PointF(x,y);
                    break;
                case MotionEvent.ACTION_MOVE:
                    //tmpPoint.set(x,y);
                    tmpPoint=new PointF(x,y);
                    break;
                case MotionEvent.ACTION_UP:
                    if (point1 == null) {
                        eyePoint = new PointF(imageView.getWidth() / 2, imageView.getHeight());
                        point1 = new PointF(x, y);
                        invalidate();
                        if(Integer.parseInt(version) >= 29) {
                            magnifier.dismiss();
                        }
                        break;
                    }
                    if (point2 == null) {
                        point2 = new PointF(x, y);
                        invalidate();
                        if(Integer.parseInt(version) >= 29) {
                            magnifier.dismiss();
                        }
                        break;
                    } else {
                        Log.i("Max Points reached", "2");
                        if(Integer.parseInt(version) >= 29) {
                            magnifier.dismiss();
                        }
                        Log.i("action up", "action up");
                        break;
                    }




            }
            return true;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            paint.setColor(Color.rgb(190, 70, 70));
            paint.setStrokeWidth(5);
            if (point1 != null) {
                canvas.drawLine(point1.x, point1.y, eyePoint.x, eyePoint.y, paint);
                canvas.drawCircle(point1.x, point1.y, 20, paint);
                canvas.drawCircle(imageView.getWidth() / 2, imageView.getHeight(), 20, paint);
            }
            if (point2 != null) {
                canvas.drawLine(point2.x, point2.y, eyePoint.x, eyePoint.y, paint);
                canvas.drawCircle(point2.x, point2.y, 20, paint);
            }
            if (tmpPoint != null) {
                canvas.drawCircle(tmpPoint.x, tmpPoint.y, 10, paint);
            }
        }

        public static float calculateAngle() {
            Log.i("eyePoint", String.valueOf(eyePoint));
            PointF a = new PointF(point1.x - eyePoint.x, point1.y - eyePoint.y);
            PointF b = new PointF(point2.x - eyePoint.x, point2.y - eyePoint.y);
            Log.i("a:", String.valueOf(a));
            Log.i("b:", String.valueOf(b));
            float angle = (float) Math.toDegrees(Math.acos(((a.x * b.x) + (a.y * b.y)) / (a.length() * b.length())));
            Log.i("Angle:", String.valueOf(angle));
            txtCalculatedAngle.setText("Calculated Angle: " + String.valueOf(angle));

            return angle;
        }

        public static void removePoints() {
            point1 = null;
            point2 = null;
            paint = new Paint();
        }
    }
}