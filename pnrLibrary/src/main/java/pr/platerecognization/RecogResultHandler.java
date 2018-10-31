package pr.platerecognization;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by pchsun on 2018/10/31.
 */

public class RecogResultHandler extends Handler {

    public static final int REAL_TIME_RECOG_FINISH = 0;
    public static final int REAL_TIME_RECOG_WAITTING = 1;

    WeakReference<TextureViewActivity> mTextureViewActivityWeakReference;
    private List<Long> timeList;

    public RecogResultHandler(TextureViewActivity textureViewActivity){
        mTextureViewActivityWeakReference = new WeakReference<>(textureViewActivity);
        timeList = new ArrayList<>();
    }
    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        TextureViewActivity textureViewActivity = mTextureViewActivityWeakReference.get();
        switch (msg.what) {
            case REAL_TIME_RECOG_WAITTING:
                long time1 = (Long) msg.obj;
                timeList.add(time1);
                break;
            case REAL_TIME_RECOG_FINISH:
                long allTime = 0;
                for (long time : timeList){
                    allTime = allTime + time;
                }
                allTime = allTime + Long.parseLong((String) (((HashMap)msg.obj).get("time")));
                Intent intent = new Intent();
                intent.putExtra("time", String.valueOf(allTime));//时间单位ms
                intent.putExtra("result", (String) (((HashMap)msg.obj).get("result")));
                textureViewActivity.setResult(Activity.RESULT_OK, intent);
                textureViewActivity.finish();
                break;
        }
    }
}
