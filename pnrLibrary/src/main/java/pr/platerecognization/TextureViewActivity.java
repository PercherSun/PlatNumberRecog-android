package pr.platerecognization;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TextureViewActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener, Camera.PreviewCallback {

    public static final String CODE_RESOLUTION_RATIO = "dp";
    public static final String CODE_FRAME_NUMBER = "frameNumber";

    private TextureView textureView;

    private Camera mCamera;
    private List<String> list;
    private RecogResultHandler handler;

    private ExecutorService cachedThreadPool;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_texture_view);
        textureView = findViewById(R.id.texture_view);
        list = new ArrayList<>();
        initCamera();
        initListener();
        cachedThreadPool = Executors.newCachedThreadPool();
        handler = new RecogResultHandler(TextureViewActivity.this);
    }

    private void initCamera() {
        int numberOfCameras = Camera.getNumberOfCameras();// 获取摄像头个数
        if (numberOfCameras < 1) {
            Toast.makeText(this, "没有相机", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }

    private void initListener() {
        textureView.setSurfaceTextureListener(this);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        // 打开相机 0后置 1前置
        mCamera = Camera.open(0);
        if (mCamera != null) {
            // 设置相机预览宽高，此处设置为TextureView宽高
            Camera.Parameters params = mCamera.getParameters();
            params.setPreviewSize(width, height);
            // 设置自动对焦模式
            List<String> focusModes = params.getSupportedFocusModes();
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                try {
                    mCamera.setParameters(params);
                } catch (Exception e) {
                    e.printStackTrace();
                    try {
                        params.setPreviewSize(1920, 1080);
                        mCamera.setParameters(params);
                    } catch (Exception e1) {
                        e.printStackTrace();
                    }
                }
            }
            try {
                mCamera.setDisplayOrientation(getCameraDisplayOrientation(0));// 设置预览角度，并不改变获取到的原始数据方向
                // 绑定相机和预览的View
                mCamera.setPreviewTexture(surface);
                // 开始预览
                mCamera.startPreview();
                mCamera.setPreviewCallback(this);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        mCamera.setPreviewCallback(null);
        mCamera.stopPreview();
        mCamera.release();
        return false;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (camera == null)
            return;
        try {
            Camera.Size size = camera.getParameters().getPreviewSize();
            YuvImage image = new YuvImage(data, ImageFormat.NV21, size.width, size.height, null);
            if (image != null) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                image.compressToJpeg(new Rect(0, 0, size.width, size.height), 80, stream);
                Bitmap bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
                final Bitmap newBitmap = rotateBitmap(bmp, getCameraDisplayOrientation(0));

                cachedThreadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        Recog.recogRealTime(newBitmap, getIntent().getIntExtra("dp", 1), getIntent().getIntExtra("frameNumber", 10), list, handler);
                    }
                });
                stream.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 摄像头预览旋转角度
     *
     * @param cameraId
     * @return
     */
    private int getCameraDisplayOrientation(int cameraId) {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, cameraInfo);
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int degree = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degree = 0;
                break;
            case Surface.ROTATION_90:
                degree = 90;
                break;
            case Surface.ROTATION_180:
                degree = 180;
                break;
            case Surface.ROTATION_270:
                degree = 270;
                break;
        }

        int result;

        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (cameraInfo.orientation + degree) % 360;
            result = (360 - result) % 360;//防止镜像
        } else {
            result = (cameraInfo.orientation - degree + 360) % 360;
        }

        return result;
    }


    /**
     * 选择变换
     *
     * @param origin 原图
     * @param degree 旋转角度，可正可负
     * @return 旋转后的图片
     */
    private Bitmap rotateBitmap(Bitmap origin, float degree) {
        if (origin == null) {
            return null;
        }
        int width = origin.getWidth();
        int height = origin.getHeight();
        Matrix matrix = new Matrix();
        matrix.setRotate(degree);
        // 围绕原地进行旋转
        Bitmap newBM = Bitmap.createBitmap(origin, 0, 0, width, height, matrix, false);
        if (newBM.equals(origin)) {
            return newBM;
        }
        origin.recycle();
        return newBM;
    }
}
