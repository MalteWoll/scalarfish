package com.example.scalarfish2.ui.setPoints;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.scalarfish2.R;

public class SetPointsActivity extends AppCompatActivity implements View.OnClickListener{
    static ImageView imageView;
    int maxPoints;
    Button btnCalibrateAngle;
    Button btnResetPoints;
    static TextView txtCalculatedAngle;
    static int drawCase;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_points);

        imageView = findViewById(R.id.setPointsImage);


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

    @Override
    public void onClick(View view){
        switch(view.getId()){
            case R.id.btnCalculateAngle:
                Log.i("btn", "Calculate Angle");
                float angle = DrawingImageView.calculateAngle();
                Log.i("Angle:", String.valueOf(angle));
            case R.id.btnResetPoints:
                DrawingImageView.removePoints();
        }
    }


    public static class DrawingImageView extends androidx.appcompat.widget.AppCompatImageView {
        public static PointF eyePoint;
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
                        eyePoint = new PointF(imageView.getWidth()/2, imageView.getHeight());
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
            return true;
        }

        @Override
        protected void onDraw( Canvas canvas) {
            super.onDraw(canvas);
            paint.setColor(Color.rgb(190, 70,70));
            paint.setStrokeWidth(5);
            if (point1 != null) {
                canvas.drawLine(point1.x, point1.y, eyePoint.x, eyePoint.y, paint);
                canvas.drawCircle(point1.x, point1.y, 20, paint);
                canvas.drawCircle(imageView.getWidth()/2, imageView.getHeight(), 20, paint);
            }
            if (point2 != null) {
                canvas.drawLine(point2.x, point2.y, eyePoint.x, eyePoint.y, paint);
                canvas.drawCircle(point2.x, point2.y, 20, paint);
            }
        }

        public static float calculateAngle(){
            Log.i("eyePoint", String.valueOf(eyePoint));
            PointF a = new PointF(point1.x - eyePoint.x, point1.y - eyePoint.y);
            PointF b = new PointF(point2.x - eyePoint.x, point2.y - eyePoint.y);
            Log.i("a:", String.valueOf(a));
            Log.i("b:", String.valueOf(b));
            float angle = (float) Math.toDegrees(Math.acos(((a.x * b.x) + (a.y * b.y)) / (a.length() * b.length())));
            Log.i("Angle:", String.valueOf(angle));
            txtCalculatedAngle.setText(String.valueOf(angle));

            return angle;
        }

        public static void removePoints() {
            point1 = null;
            point2 = null;
            paint = new Paint();
        }
    }
}