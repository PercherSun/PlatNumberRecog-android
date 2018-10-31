package pr.platerecognization;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Created by pchsun on 2018/10/30.
 */

public class IdentificationInit {

    private static long handle;

    public static void initOpenCV() {
        if (OpenCVLoader.initDebug())
            Log.e("Opencv", "opencv load_success");
        else
            Log.e("Opencv", "opencv can't load opencv .");
    }

    public static void copyFilesFromAssets(Context context, String oldPath, String newPath) {
        try {
            String[] fileNames = context.getAssets().list(oldPath);
            if (fileNames.length > 0) {
                // directory
                File file = new File(newPath);
                if (!file.mkdir())
                {
                    Log.d("mkdir","can't make folder");

                }
//                    return false;                // copy recursively
                for (String fileName : fileNames) {
                    copyFilesFromAssets(context, oldPath + "/" + fileName,
                            newPath + "/" + fileName);
                }
            } else {
                // file
                InputStream is = context.getAssets().open(oldPath);
                FileOutputStream fos = new FileOutputStream(new File(newPath));
                byte[] buffer = new byte[1024];
                int byteCount;
                while ((byteCount = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, byteCount);
                }
                fos.flush();
                is.close();
                fos.close();
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void initRecognizer(Context context)
    {
        String assetPath = "pr";
        String sdcardPath = Environment.getExternalStorageDirectory()
                + File.separator + assetPath;
        copyFilesFromAssets(context, assetPath, sdcardPath);
        String cascade_filename  =  sdcardPath
                + File.separator+"cascade.xml";
        String finemapping_prototxt  =  sdcardPath
                + File.separator+"HorizonalFinemapping.prototxt";
        String finemapping_caffemodel  =  sdcardPath
                + File.separator+"HorizonalFinemapping.caffemodel";
        String segmentation_prototxt =  sdcardPath
                + File.separator+"Segmentation.prototxt";
        String segmentation_caffemodel =  sdcardPath
                + File.separator+"Segmentation.caffemodel";
        String character_prototxt =  sdcardPath
                + File.separator+"CharacterRecognization.prototxt";
        String character_caffemodel=  sdcardPath
                + File.separator+"CharacterRecognization.caffemodel";
        handle  =  PlateRecognition.InitPlateRecognizer(
                cascade_filename,
                finemapping_prototxt,finemapping_caffemodel,
                segmentation_prototxt,segmentation_caffemodel,
                character_prototxt,character_caffemodel
        );
    }

    public static long getHandle(){
        return handle;
    }
}
