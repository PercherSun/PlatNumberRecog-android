package com.casic.plate.platenumberidentification;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.util.HashMap;

import pr.platerecognization.Recog;
import pr.platerecognization.TextureViewActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private final String TAG = this.getClass().toString();

    private Button button1, button2, button3;

    private static final int REQUEST_CODE_IMAGE_CAMERA = 1;
    private static final int REQUEST_CODE_IMAGE_OP = 2;
    private static final int REQUEST_CODE_VIDEO_CAMERA = 3;
    private Uri mPath;

    Bitmap latestBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        button1 = findViewById(R.id.button1);
        button2 = findViewById(R.id.button2);
        button3 = findViewById(R.id.button3);
        button1.setOnClickListener(this);
        button2.setOnClickListener(this);
        button3.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.button1:
                Intent getImageByalbum = new Intent(Intent.ACTION_GET_CONTENT);
                getImageByalbum.addCategory(Intent.CATEGORY_OPENABLE);
                getImageByalbum.setType("image/jpeg");
                startActivityForResult(getImageByalbum, REQUEST_CODE_IMAGE_OP);
                break;
            case R.id.button2:
                Intent getImageByCamera = new Intent(
                        "android.media.action.IMAGE_CAPTURE");
                ContentValues values = new ContentValues(1);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                mPath = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                getImageByCamera.putExtra(MediaStore.EXTRA_OUTPUT, mPath);
                startActivityForResult(getImageByCamera, REQUEST_CODE_IMAGE_CAMERA);
                break;
            case R.id.button3:
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, TextureViewActivity.class);
                intent.putExtra(TextureViewActivity.CODE_RESOLUTION_RATIO, 1);//dp 设置与图片原分辨率的比例，0-1;
                intent.putExtra(TextureViewActivity.CODE_FRAME_NUMBER, 10);//frameNumber 采集结果总数
                startActivityForResult(intent, REQUEST_CODE_VIDEO_CAMERA);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case REQUEST_CODE_IMAGE_OP:
                if (resultCode == RESULT_OK){
                    mPath = data.getData();
                    String file = getPath(mPath);
                    Bitmap bmp = decodeImage(file);
                    if (bmp == null || bmp.getWidth() <= 0 || bmp.getHeight() <= 0 ) {
                        Log.e(TAG, "error");
                    } else {
                        Log.i(TAG, "bmp [" + bmp.getWidth() + "," + bmp.getHeight());
                    }
                    latestBitmap = bmp;

                    HashMap<String, String> map = Recog.recog(bmp, 1);
                    String number = map.get("number");
                    String time = map.get("time");
                    button1.setText("车牌号=" + number + "==时间=" + time);
                }
                break;
            case REQUEST_CODE_IMAGE_CAMERA:
                if (resultCode == RESULT_OK){
                    String file = getPath(mPath);
                    Bitmap bmp = decodeImage(file);
                    latestBitmap = bmp;
                    HashMap<String, String> map = Recog.recog(bmp, 1);
                    String number = map.get("number");
                    String time = map.get("time");
                    button2.setText("车牌号=" + number + "==时间=" + time);
                }
                break;
            case REQUEST_CODE_VIDEO_CAMERA:
                if (resultCode == RESULT_OK){
                    Bundle bundle = data.getExtras();
                    String number = bundle.getString("result");
                    String time = bundle.getString("time");
                    button3.setText("车牌号=" + number + "==时间=" + time);
                }
                break;
        }
    }

    public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * @param uri
     * @return
     */
    private String getPath(Uri uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (DocumentsContract.isDocumentUri(this, uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                    return null;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    return null;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(this, contentUri, selection, selectionArgs);
            }
        }
        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor actualimagecursor = managedQuery(uri, proj,null,null,null);
        int actual_image_column_index = actualimagecursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        actualimagecursor.moveToFirst();
        String img_path = actualimagecursor.getString(actual_image_column_index);
        String end = img_path.substring(img_path.length() - 4);
        if (0 != end.compareToIgnoreCase(".jpg") && 0 != end.compareToIgnoreCase(".png")) {
            return null;
        }
        return img_path;
    }

    public static Bitmap decodeImage(String path) {
        Bitmap res;
        try {
            ExifInterface exif = new ExifInterface(path);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            BitmapFactory.Options op = new BitmapFactory.Options();
            op.inSampleSize = 1;
            op.inJustDecodeBounds = false;
            //op.inMutable = true;
            res = BitmapFactory.decodeFile(path, op);
            //rotate and scale.
            Matrix matrix = new Matrix();

            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                matrix.postRotate(90);
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
                matrix.postRotate(180);
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                matrix.postRotate(270);
            }

            Bitmap temp = Bitmap.createBitmap(res, 0, 0, res.getWidth(), res.getHeight(), matrix, true);
            Log.d("com.arcsoft", "check target Image:" + temp.getWidth() + "X" + temp.getHeight());

            if (!temp.equals(res)) {
                res.recycle();
            }
            return temp;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
