package zsc.kalends.tensortest;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Date;

import zsc.kalends.tensortest.Classifier.FrontPoint;


public class MainActivity extends AppCompatActivity implements View.OnClickListener, CameraBridgeViewBase.CvCameraViewListener2 {

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static final String TAG = "MainActivity";
    private Mat mRgba;
    private TensorFlowInferenceInterface front_inferenceInterface;
    private static final int POINTS_NUM = 16;

    private ImageView iv_thumb;
    private JavaCameraView mOpenCVCameraView;
    private float angley;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private String filePath;
    private String info_infer;

    private SensorEventListener mSensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            if (sensorEvent.sensor == null) {
                Log.e(TAG, "Sensor Error.");
                return;
            }
            angley = sensorEvent.values[SensorManager.DATA_Y];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    };

    public BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void surfaceCreate(SurfaceHolder holder) {

        }

        @Override
        public void onManagerConnected(int status) {
            if (status == LoaderCallbackInterface.SUCCESS) {
                Log.i(TAG, "Loading OpenCV Success");
                mOpenCVCameraView.enableView();
            } else {
                super.onManagerConnected(status);
                Log.i(TAG, "Loading OpenCV Fail");
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);//隐藏标题栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);//隐藏状态栏
        setContentView(R.layout.activity_camera);

        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        initView();

        /*=================手机重力感应器注册=====================*/
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager != null) {
            mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mSensorManager.registerListener(mSensorEventListener, mSensor, SensorManager.SENSOR_DELAY_GAME);
        }
        /*=====================================================*/

        /*=======================模型加载============================*/
        front_inferenceInterface = new TensorFlowInferenceInterface(getAssets(), "file:///android_asset/front_frozen_model.pb");

        /*===========================================================*/
        mOpenCVCameraView = findViewById(R.id.view_camera);
        mOpenCVCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        mOpenCVCameraView.setCvCameraViewListener(this);

    }


    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCVCameraView != null) {
            mOpenCVCameraView.disableView();
        }
        mSensorManager.unregisterListener(mSensorEventListener);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization.");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, baseLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        mSensorManager.registerListener(mSensorEventListener, mSensor, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1) {
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "对不起，没有权限，无法正常使用相机", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void initView() {
        iv_thumb = findViewById(R.id.iv_thumb);

        /* =========================== Android权限 ============================= */
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            findViewById(R.id.btn_control).setClickable(false);
        }

        findViewById(R.id.btn_control).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //通过合规检测才可以进行拍照
                if (angley >= 9.0f && angley <= 10.9 && Iseligible()) {
                    MainActivity.this.takePhoto();
                }
            }
        });

//        //查看照片
//        iv_thumb.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                try {
//                    File file = new File(filePath);
//                    Intent it = new Intent(Intent.ACTION_VIEW);
//                    Uri mUri = Uri.parse("file://" + file.getPath());
//                    it.setDataAndType(mUri, "image/*");
//                    MainActivity.this.startActivity(it);
//                } catch (Exception e) {
//                    Log.e(TAG, e.toString());
//                    Toast.makeText(MainActivity.this, "图片打开失败", Toast.LENGTH_SHORT).show();
//                }
//            }
//        });
    }

    //TODO:拍照
    @SuppressLint({"NewApi", "SimpleDateFormat"})
    private void takePhoto() {
        if (mRgba != null) {
            Mat inter = new Mat(mRgba.width(), mRgba.height(), CvType.CV_8UC4);
            Imgproc.cvtColor(mRgba, inter, Imgproc.COLOR_RGBA2BGR);
            File sdDir = null;
            boolean sdCardExist = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
            if (sdCardExist) {
                sdDir = Environment.getExternalStorageDirectory();
            }

            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            String filename = sdf.format(new Date());
            String appname = getString(R.string.app_name);
            String savepath = sdDir + appname;
            File f = new File(savepath);
            if (!f.exists()) {
                try {
                    f.mkdirs();
                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                }
            }

            filePath = savepath + filename + ".png";
            Imgcodecs.imwrite(filePath, inter);
            Toast.makeText(MainActivity.this, "图片保存到：" + filePath, Toast.LENGTH_SHORT).show();
            try {
                FileInputStream fis = new FileInputStream(filePath);
                iv_thumb.setImageBitmap(BitmapFactory.decodeStream(fis));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish(); // back button
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {

    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Log.e(TAG, "onCameraFrame");
        mRgba = inputFrame.rgba();
        Bitmap cameraView = Bitmap.createBitmap(inputFrame.rgba().cols(), inputFrame.rgba().rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(inputFrame.rgba(), cameraView);
        Mat result = new Mat();
        try {
            Bitmap mark = BitmapFactory.decodeResource(getResources(), R.drawable.front1);
            mark = ThumbnailUtils.extractThumbnail(mark, 400, 400);
            Matrix matrix = new Matrix();
            matrix.setRotate(-90.0f, (float)mark.getWidth() / 2, (float)mark.getHeight() / 2);
            mark = Bitmap.createBitmap(mark, 0, 0, mark.getWidth(), mark.getHeight(), matrix, true);
            Bitmap temp = createResultBitmap(cameraView, mark);
            Utils.bitmapToMat(temp, result);
        } catch (Exception e) {
            Log.e(TAG, "OpenCV Read Image is Error" + e);
            return inputFrame.rgba();
        }
        return result;
    }

    private Bitmap createResultBitmap(Bitmap background, Bitmap mark) {
        Log.e(TAG, "function createResultBitmap");

        int bg_width = background.getWidth();
        int bg_height = background.getHeight();
        int m_width = mark.getWidth();
        int m_height = mark.getHeight();

        Bitmap result = Bitmap.createBitmap(bg_width, bg_height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(background, 0, 0, null);
        canvas.drawBitmap(mark, bg_width - m_width , bg_height- m_height - 20, null);
        canvas.save();
        canvas.restore();
        return result;
    }

    //TODO:对照片进行合规判定
    private boolean Iseligible() {
      FrontPoint frontPoint = new FrontPoint(front_inferenceInterface);
      int[] front_points = frontPoint.getResult(mRgba);

      for (int i = 0; i < front_points.length / 2; i++) {
          if (front_points[i * 2] < front_points[i * 2 + 1]) {
              return false;
          }
      }

      return true;
    }

    private void FaceBlur() {
        if (info_infer.equals("front")) {
            FrontFaceBlur();
        } else {
            SideFaceBlur();
        }
    }

    private void FrontFaceBlur() {

    }

    private void SideFaceBlur() {

    }
}