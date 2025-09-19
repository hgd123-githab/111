package com.example.shareplatform.activity;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.shareplatform.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ImagePreviewActivity extends AppCompatActivity {

    private ImageView imageView;
    private Button saveButton;
    private String imageUrl;
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 101;
    private Bitmap currentBitmap; // 缓存图片，避免重复获取

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_image_preview);

        // 初始化视图
        imageView = findViewById(R.id.imageView_preview);
        saveButton = findViewById(R.id.button_save);

        // 获取图片URL并加载图片
        imageUrl = getIntent().getStringExtra("IMAGE_URL");
        if (imageUrl != null) {
            loadImageWithGlide();
        } else {
            Toast.makeText(this, "图片URL不存在", Toast.LENGTH_SHORT).show();
            finish();
        }

        // 设置保存按钮点击事件
        saveButton.setOnClickListener(v -> checkPermissionAndSaveImage());
    }

    /**
     * 使用Glide加载图片并缓存Bitmap
     */
    private void loadImageWithGlide() {
        Glide.with(this)
                .asBitmap()
                .load(imageUrl)
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_launcher_background)
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, Transition<? super Bitmap> transition) {
                        imageView.setImageBitmap(resource);
                        currentBitmap = resource; // 缓存Bitmap
                    }
                });
    }

    /**
     * 检查权限并保存图片
     */
    private void checkPermissionAndSaveImage() {
        // 检查是否有可用的图片
        if (currentBitmap == null) {
            Toast.makeText(this, "图片加载中，请稍候", Toast.LENGTH_SHORT).show();
            return;
        }

        // 检查权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13及以上不需要WRITE_EXTERNAL_STORAGE权限
            saveImageToGallery(currentBitmap);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        STORAGE_PERMISSION_REQUEST_CODE);
                return;
            }
            saveImageToGallery(currentBitmap);
        } else {
            // Android 6.0以下不需要动态申请权限
            saveImageToGallery(currentBitmap);
        }
    }

    /**
     * 保存图片到相册
     */
    private void saveImageToGallery(Bitmap bitmap) {
        // 创建图片文件名
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "SharePlatform_" + timeStamp + ".jpg";

        OutputStream outputStream = null;
        Uri imageUri = null;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10及以上使用MediaStore
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, imageFileName);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES
                        + File.separator + "SharePlatform"); // 保存到自定义目录
                values.put(MediaStore.Images.Media.IS_PENDING, 1); // 标记为待处理

                // 获取ContentResolver并插入记录
                imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (imageUri == null) {
                    showSaveError();
                    return;
                }

                // 打开输出流写入图片
                outputStream = getContentResolver().openOutputStream(imageUri);
                if (outputStream == null) {
                    showSaveError();
                    return;
                }

                // 压缩保存图片，避免过大
                boolean compressSuccess = bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
                if (!compressSuccess) {
                    showSaveError();
                    return;
                }

                // 标记为已处理
                values.clear();
                values.put(MediaStore.Images.Media.IS_PENDING, 0);
                getContentResolver().update(imageUri, values, null, null);

            } else {
                // Android 9及以下版本处理方式
                File picturesDir;

                // 优先使用公共目录
                if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                    picturesDir = new File(Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_PICTURES), "SharePlatform");
                } else {
                    // 外部存储不可用时使用应用私有目录
                    picturesDir = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "SharePlatform");
                }

                // 创建目录
                if (!picturesDir.exists() && !picturesDir.mkdirs()) {
                    Toast.makeText(this, "无法创建保存目录", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 创建文件
                File imageFile = new File(picturesDir, imageFileName);
                outputStream = new FileOutputStream(imageFile);

                // 压缩保存
                boolean compressSuccess = bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
                if (!compressSuccess) {
                    showSaveError();
                    return;
                }

                // 获取文件Uri（适配7.0及以上）
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    imageUri = FileProvider.getUriForFile(this,
                            getPackageName() + ".fileprovider", imageFile);
                } else {
                    imageUri = Uri.fromFile(imageFile);
                }

                // 通知相册更新
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                        Uri.fromFile(imageFile)));
            }

            // 保存成功处理
            if (imageUri != null) {
                Toast.makeText(this, "图片已保存到相册", Toast.LENGTH_SHORT).show();
                // 可选：显示保存位置
                // Toast.makeText(this, "保存路径: " + imageUri.getPath(), Toast.LENGTH_LONG).show();
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            showSaveError();
        }  finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 显示保存错误提示
     */
    private void showSaveError() {
        Toast.makeText(this, "保存失败，请重试", Toast.LENGTH_SHORT).show();
    }

    /**
     * 权限请求结果处理
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限被授予，执行保存操作
                if (currentBitmap != null) {
                    saveImageToGallery(currentBitmap);
                }
            } else {
                // 权限被拒绝，引导用户去设置
                Toast.makeText(this, "需要存储权限才能保存图片", Toast.LENGTH_SHORT).show();
                // 可选：引导用户手动开启权限
                // Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                // intent.setData(Uri.fromParts("package", getPackageName(), null));
                // startActivity(intent);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 释放Bitmap资源，避免内存泄漏
        if (currentBitmap != null && !currentBitmap.isRecycled()) {
            currentBitmap.recycle();
            currentBitmap = null;
        }
    }
}
