package zsc.kalends.tensortest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import zsc.kalends.tensortest.Classifier.PyUtils;

public class TestActivity extends AppCompatActivity {

    private TensorFlowInferenceInterface inferenceInterface;
    private ImageView imageView;
    private CascadeClassifier cascadeClassifier;

    public BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void surfaceCreate(SurfaceHolder holder) {

        }

        @Override
        public void onManagerConnected(int status) {
            if (status == LoaderCallbackInterface.SUCCESS) {
                Log.i("Test", "Loading OpenCV Success");
               // mOpenCVCameraView.enableView();
            } else {
                super.onManagerConnected(status);
                Log.i("Test", "Loading OpenCV Fail");
            }
        }
    };

    static {
        System.loadLibrary("tensorflow_inference");
        Log.e("TensorFlow", "LibTensorFlow_inference.so库加载成功");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test);
        imageView = findViewById(R.id.iv_test);

        //inferenceInterface = new TensorFlowInferenceInterface(getAssets(), "file:///android_asset/front_frozen_cloth_model.pb");
        Log.e("TensorLite", "TensorFlow 模型加载成功");
    }

    private void Press() {
        Bitmap bitmap = ((BitmapDrawable)imageView.getDrawable()).getBitmap();
        Mat image = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC4);
        Utils.bitmapToMat(bitmap, image);

        Imgproc.cvtColor(image, image, Imgproc.COLOR_BGR2RGB);
        Imgproc.resize(image, image, new Size(256, 256));
        int[] values = new int[256 * 256];
        int[] batch_size = new int[1];
        batch_size[0] = 1;
        float[] input_data = new float[256 * 256 * 3];
        if (bitmap.getWidth() != 256 || bitmap.getHeight() != 256) {
            // rescale the bitmap if needed
            bitmap = ThumbnailUtils.extractThumbnail(bitmap, 256, 256);
        }

        bitmap.getPixels(values,0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        for (int i = 0; i < values.length; i++) {
            int val = values[i];
            input_data[i * 3] = (Color.red(val) / 255.0f) * 2.0f - 1.0f;
            //Log.d("Test1", Float.toString(input_data[i * 3]));
            input_data[i * 3 + 1] = (Color.green(val) / 255.0f) * 2.0f - 1.0f;
            //Log.d("Test2", Float.toString(input_data[i * 3 + 1]));
            input_data[i * 3 + 2] = (Color.blue(val) / 255.0f) * 2.0f - 1.0f;
            //Log.d("Test3", Float.toString(input_data[i * 3 + 2]));
        }

        inferenceInterface.feed("input/input_images", input_data, 1,256,256,3);
        inferenceInterface.feed("input/batch_size", batch_size);
        inferenceInterface.run(new String[]{"inference/output"}, false);
        float[] labels = new float[256 * 256 * 12];
        inferenceInterface.fetch("inference/output", labels);
        Log.e("Label:", Integer.toString(labels.length));
        int index = 0;
        for (int i = 0; i < 12; i++) {
            float maxn = labels[index];
            int maxwIndex = 0, maxhIndex = 0;
            for (int w = 0; w < 256; w++) {
                for (int h = 0; h < 256; h++) {
                    if (labels[index] > maxn) {
                        maxwIndex = w;
                        maxhIndex = h;
                        maxn = labels[index];
                    }
                    index++;
                }
            }

            if (maxn <= 0.85) {
                maxwIndex = -1;
                maxhIndex = -1;
            }

            int x = (int) (maxwIndex * image.size().width / 256.0f);
            int y = (int) (maxhIndex * image.size().height / 256.0f);
            Log.e("(x, y):", x + "," + y);
        }
    }

    private void Py_Press() {
        Bitmap bitmap = ((BitmapDrawable)imageView.getDrawable()).getBitmap();
        if (bitmap.getWidth() != 224 || bitmap.getHeight() != 224) {
            // rescale the bitmap if needed
            bitmap = ThumbnailUtils.extractThumbnail(bitmap, 224, 224);
        }
        final String moduleFileAbsoluteFilePath = new File(
                PyUtils.assetFilePath(this, "my_test_cpu.pt")).getAbsolutePath();
        Module module = Module.load(moduleFileAbsoluteFilePath);
        Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(bitmap, TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB);
        Tensor outputTensor = module.forward(IValue.from(inputTensor)).toTensor();
        float[] scores = outputTensor.getDataAsFloatArray();

        for (float score : scores) {
            Log.e("Score:", Float.toString(score));
        }
    }

    private void initalizeOpenDependencies() {
        try {
            InputStream is = getResources().openRawResource(R.raw.haarcascade_frontalface_default);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            File mCascadeFile = new File(cascadeDir, "haarcascade_frontalface_default.xml");
            FileOutputStream os = new FileOutputStream(mCascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();
            cascadeClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void FacePress() {
        Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
        Mat image = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC4);
        Utils.bitmapToMat(bitmap, image);

        Mat grayscaleImage = new Mat();
        Imgproc.cvtColor(image, grayscaleImage, Imgproc.COLOR_RGBA2RGB);
        MatOfRect faces = new MatOfRect();
        if (cascadeClassifier != null) {
            cascadeClassifier.detectMultiScale(grayscaleImage, faces, 1.1, 2, 2, new Size((int) image.size().height * 0.2, (int) image.size().height * 0.2), new Size());
        }
        Rect[] facesArray = faces.toArray();
        for (int i = 0; i < facesArray.length; i++) {
            Imgproc.rectangle(image, facesArray[i].tl(), facesArray[i].br(), new Scalar(0, 255, 0, 255), 3);
        }
        Bitmap result = Bitmap.createBitmap(image.cols(), image.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(image, result);
        imageView.setImageBitmap(result);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d("Test", "Internal OpenCV library not found. Using OpenCV Manager for initialization.");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, baseLoaderCallback);
        } else {
            Log.d("Test", "OpenCV library found inside package. Using it!");
            baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        //Press();
       // Py_Press();
        initalizeOpenDependencies();
        FacePress();
    }
}
