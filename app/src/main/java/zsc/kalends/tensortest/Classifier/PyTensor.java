package zsc.kalends.tensortest.Classifier;

import android.app.Activity;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.File;

public class PyTensor {
    public float[] GetProb(Activity activity, Bitmap bitmap) {
        if (bitmap.getWidth() != 224 || bitmap.getHeight() != 224) {
            // rescale the bitmap if needed
            bitmap = ThumbnailUtils.extractThumbnail(bitmap, 224, 224);
        }
        final String moduleFileAbsoluteFilePath = new File(
                PyUtils.assetFilePath(activity, "my_test_cpu.pt")).getAbsolutePath();
        Module module = Module.load(moduleFileAbsoluteFilePath);
        Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(bitmap, TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB);
        Tensor outputTensor = module.forward(IValue.from(inputTensor)).toTensor();
        float[] scores = outputTensor.getDataAsFloatArray();
        return scores;
    }
}
