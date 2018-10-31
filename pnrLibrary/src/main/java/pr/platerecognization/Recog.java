package pr.platerecognization;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.TextureView;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by pchsun on 2018/10/30.
 */

public class Recog {

    /**
     *
     * @param bmp 图片
     * @param dp 设置与图片原分辨率的比例，0-1
     * @return  map
     */
    public static HashMap<String, String> recog(Bitmap bmp, int dp) {
        HashMap<String, String> map = new HashMap<>();
        float dp_asp = dp / 10.f;
        Mat mat_src = new Mat(bmp.getWidth(), bmp.getHeight(), CvType.CV_8UC4);
        float new_w = bmp.getWidth() * dp_asp;
        float new_h = bmp.getHeight() * dp_asp;
        Size sz = new Size(new_w, new_h);
        Utils.bitmapToMat(bmp, mat_src);
        Imgproc.resize(mat_src, mat_src, sz);
        long currentTime1 = System.currentTimeMillis();
        String res = PlateRecognition.SimpleRecognization(mat_src.getNativeObjAddr(), IdentificationInit.getHandle());
        long diff = System.currentTimeMillis() - currentTime1;
        map.put("number", res);
        map.put("time", String.valueOf(diff));
        return map;
    }

    /**
     *
     * @param bmp 图片
     * @param dp 设置与图片原分辨率的比例，0-1
     * @param frameNumber 采集结果总数
     * @param list 存放采集结果
     * @return map
     */
    public static void recogRealTime(Bitmap bmp, int dp, int frameNumber, List<String> list, Handler handler){
        HashMap<String, String> map = new HashMap<>();
        float dp_asp = dp / 10.f;
        Mat mat_src = new Mat(bmp.getWidth(), bmp.getHeight(), CvType.CV_8UC4);
        float new_w = bmp.getWidth() * dp_asp;
        float new_h = bmp.getHeight() * dp_asp;
        Size sz = new Size(new_w, new_h);
        Utils.bitmapToMat(bmp, mat_src);
        Imgproc.resize(mat_src, mat_src, sz);
        long currentTime1 = System.currentTimeMillis();
        String res = PlateRecognition.SimpleRecognization(mat_src.getNativeObjAddr(), IdentificationInit.getHandle());
        if (!TextUtils.isEmpty(res))
            list.add(res);

        if (list.size() >= frameNumber){
            Message message = new Message();
            message.what = RecogResultHandler.REAL_TIME_RECOG_FINISH;
            map.put("time", String.valueOf(System.currentTimeMillis() - currentTime1));
            map.put("result", getMax_str(list));
            message.obj = map;
            handler.sendMessage(message);
        }else {
            Message message = new Message();
            message.what = RecogResultHandler.REAL_TIME_RECOG_WAITTING;
            message.obj = System.currentTimeMillis() - currentTime1;
            handler.sendMessage(message);
        }
    }

    private static String getMax_str(List<String> list) {
        String regex;
        Pattern p;
        Matcher m;
        String tot_str = list.toString();
        int max_cnt = 0;
        String tmp = "";
        String max_str = "";
        for (String str : list) {
            if (tmp.equals(str)) continue;
            tmp = str;
            regex = str;
            p = Pattern.compile(regex);
            m = p.matcher(tot_str);
            int cnt = 0;
            while (m.find()) {
                cnt++;
            }
            if (cnt > max_cnt) {
                max_cnt = cnt;
                max_str = str;
            }
        }
        Log.d("PlatNumberRecog", " 出现的最大次数的字符串是 " + max_str);
        return max_str;
    }
}
