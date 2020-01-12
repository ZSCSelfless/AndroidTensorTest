package zsc.kalends.tensortest.Classifier;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.ThumbnailUtils;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

public class FrontPoint {

    private TensorFlowInferenceInterface inferenceInterface;
    private static final int IMAGE_WIDTH = 512;
    private static final int IMAGE_HEIGHT = 512;
    private static final int POINTS_NUM = 16;
    private static final int CHANNELS_NUM = 3;

    public FrontPoint(TensorFlowInferenceInterface infer) {
        this.inferenceInterface = infer;
    }

    public int[] getResult(Mat image) {
        Bitmap bitmap = Bitmap.createBitmap(image.cols(), image.rows(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(image, bitmap);
        int[] values = new int[IMAGE_WIDTH * IMAGE_HEIGHT];
        int[] batch_size = {1};
        float[] input_data = new float[IMAGE_HEIGHT * IMAGE_WIDTH * CHANNELS_NUM];
        if (bitmap.getWidth() != IMAGE_WIDTH || bitmap.getHeight() !=IMAGE_HEIGHT) {
            bitmap = ThumbnailUtils.extractThumbnail(bitmap, IMAGE_WIDTH, IMAGE_HEIGHT);
        }

        bitmap.getPixels(values, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        for (int i = 0; i < values.length; i++) {
            int val = values[i];

            input_data[i * 3] = (Color.red(val) / 255.0f) * 2.0f - 1.0f;
            input_data[i * 3 + 1] = (Color.green(val) / 255.0f) * 2.0f - 1.0f;
            input_data[i * 3 + 2] = (Color.blue(val) / 255.0f) * 2.0f - 1.0f;
        }

        inferenceInterface.feed("input/input_images", input_data, 1, IMAGE_WIDTH, IMAGE_HEIGHT, CHANNELS_NUM);
        inferenceInterface.feed("input/batch_size", batch_size);
        inferenceInterface.run(new String[]{"inference/output"}, false);
        float[] labels = new float[IMAGE_WIDTH * IMAGE_HEIGHT * POINTS_NUM];
        inferenceInterface.fetch("inference/output", labels);

        int[] results = new int[POINTS_NUM * 2];
        int index = 0;

        for (int i = 0; i < POINTS_NUM; i++) {
            float maxn = labels[index];
            int maxwIndex = 0, maxhIndex = 0;
            for (int w = 0; w < IMAGE_WIDTH; w++) {
                for (int h = 0; h < IMAGE_HEIGHT; h++) {
                    if (labels[index] > maxn) {
                        maxwIndex = w;
                        maxhIndex = h;
                        maxn = labels[index];
                    }
                    index++;
                }
            }

            if (maxn <= 0.85f) {
                maxwIndex = -1;
                maxhIndex = -1;
            }

            results[i * 2] = (int) (maxwIndex * image.size().width) / IMAGE_WIDTH;
            results[i * 2 + 1] = (int) (maxhIndex * image.size().height) / IMAGE_HEIGHT;
        }
        return results;
    }
}
