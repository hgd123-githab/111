package com.example.shareplatform.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.shareplatform.R;
import com.example.shareplatform.model.response.ShareResponse;
import com.example.shareplatform.util.Resource;
import com.example.shareplatform.viewmodel.AuthViewModel;
import com.example.shareplatform.viewmodel.ShareViewModel;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class ShareActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {
    private EditText contentEt;
    private LinearLayout imageContainer;
    private ImageButton addImageBtn;
    private Button publishBtn;
    private ShareViewModel shareViewModel;
    private AuthViewModel authViewModel;

    private List<Uri> imageUris = new ArrayList<>();
    private static final int PICK_IMAGE_REQUEST = 100;
    private static final int PERMISSION_REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        if (!authViewModel.isLoggedIn()) {
            navigateToLoginActivity();
            return;
        }

        initViews();
        shareViewModel = new ViewModelProvider(this).get(ShareViewModel.class);
        setupClickListeners();
        setupObservers();
    }

    private void initViews() {
        contentEt = findViewById(R.id.et_content);
        imageContainer = findViewById(R.id.image_container);
        addImageBtn = findViewById(R.id.btn_add_image);
        publishBtn = findViewById(R.id.btn_publish);
    }

    private void setupClickListeners() {
        addImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestImagePermissionAndPick();
            }
        });

        publishBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String content = contentEt.getText().toString().trim();
                if (imageUris.isEmpty()) {
                    Toast.makeText(ShareActivity.this, "请至少选择一张图片", Toast.LENGTH_SHORT).show();
                    return;
                }

                shareViewModel.share(content, imageUris);
            }
        });
    }

    private void setupObservers() {
        shareViewModel.getShareLiveData().observe(this, new Observer<Resource<ShareResponse>>() {
            @Override
            public void onChanged(Resource<ShareResponse> resource) {
                if (resource instanceof Resource.Loading) {
                    showLoading();
                } else if (resource instanceof Resource.Success) {
                    hideLoading();
                    Toast.makeText(ShareActivity.this, "分享成功", Toast.LENGTH_SHORT).show();
                    finish();
                } else if (resource instanceof Resource.Error) {
                    hideLoading();
                    Toast.makeText(ShareActivity.this, ((Resource.Error<ShareResponse>) resource).getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void requestImagePermissionAndPick() {
        if (EasyPermissions.hasPermissions(
                this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )) {
            pickImage();
        } else {
            EasyPermissions.requestPermissions(
                    this,
                    "需要访问存储权限来选择图片",
                    PERMISSION_REQUEST_CODE,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            pickImage();
        }
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this).build().show();
        }
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                imageUris.add(imageUri);
                addImageToContainer(imageUri);
            }
        }
    }

    private void addImageToContainer(Uri uri) {
        ImageView imageView = new ImageView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                dpToPx(100),
                dpToPx(100)
        );
        params.setMargins(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
        imageView.setLayoutParams(params);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

        Glide.with(this)
                .load(uri)
                .into(imageView);

        imageContainer.addView(imageView);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void showLoading() {
        Snackbar.make(findViewById(android.R.id.content), "上传中...", Snackbar.LENGTH_INDEFINITE).show();
    }

    private void hideLoading() {
        Snackbar currentSnackbar = Snackbar.make(findViewById(android.R.id.content), "", Snackbar.LENGTH_SHORT);
        currentSnackbar.dismiss();
    }

    private void navigateToLoginActivity() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}