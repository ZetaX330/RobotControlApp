package com.example.rcapp;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.rcapp.databinding.ActivityCameraBinding;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.Manifest;

import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okio.ByteString;

public class CameraActivity extends AppCompatActivity {
    private ActivityCameraBinding binding;
    private static final String TAG = "CameraActivity";
    private static final String WEBSOCKET_URL = "ws://192.168.3.7:8766"; // WebSocket 服务器地址
    private PreviewView previewView;
    private ImageAnalysis imageAnalysis;
    private ProcessCameraProvider cameraProvider;
    private ExecutorService cameraExecutor;
    private WebSocket webSocket;
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{
            Manifest.permission.CAMERA
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityCameraBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());previewView = findViewById(R.id.viewFinder);
        //启动WebSocket，与Python后端连接，传输视频流
        setupWebSocket();
        if (allPermissionsGranted()) {
            //权限足够则开启相机
            startCamera();
        } else {
            //否则请求权限
            requestPermissions();
        }
        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    private void startCamera() {
        //加载相机，进行后续监听回调
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                //获取相机实例
                cameraProvider = cameraProviderFuture.get();
                // 预览用例
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
//                // 图像分析用例
                imageAnalysis = new ImageAnalysis.Builder()
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, new ImageAnalysis.Analyzer() {
                    @Override
                    @androidx.camera.core.ExperimentalGetImage
                    public void analyze(@NonNull ImageProxy imageProxy) {
                        Image image = imageProxy.getImage();
                        if (image != null) {
                            processImage(image);
                        }
                        imageProxy.close();
                    }
                });
//                // 选择后置摄像头
//                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
//                // 解绑所有用例
//                cameraProvider.unbindAll();
//                // 绑定用例到相机
//                Camera camera = cameraProvider.bindToLifecycle(
//                        this,
//                        cameraSelector,
//                        preview,
//                        imageAnalysis
//                );
                // 创建 ImageCapture 用例
                ImageCapture imageCapture = new ImageCapture.Builder().build();

                // 解绑所有用例
                cameraProvider.unbindAll();
                // 选择后置摄像头
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                // 绑定用例到相机
                Camera camera = cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview,
                        imageAnalysis,
                        imageCapture // 绑定 ImageCapture
                );

                // 设置拍照按钮的点击事件
                findViewById(R.id.camera_photo_btn).setOnClickListener(v -> {
                    // 创建文件以保存图片
                    File photoFile = new File(getExternalFilesDir(null), "photo.jpg");
                    ImageCapture.OutputFileOptions outputFileOptions =
                            new ImageCapture.OutputFileOptions.Builder(photoFile).build();

                    // 拍照
                    imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(this),
                            new ImageCapture.OnImageSavedCallback() {
                                @Override
                                public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                                    Toast.makeText(CameraActivity.this, "拍照成功: " + photoFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onError(@NonNull ImageCaptureException exception) {
                                    Log.e(TAG, "拍照失败: ", exception);
                                }
                            });
                });


            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera: ", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void processImage(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * image.getWidth();

        // 创建位图
        Bitmap bitmap = Bitmap.createBitmap(
                image.getWidth() + rowPadding / pixelStride,
                image.getHeight(),
                Bitmap.Config.ARGB_8888
        );
        bitmap.copyPixelsFromBuffer(buffer);
        sendImageToServer(bitmap);

        runOnUiThread(() -> {
            // 这里可以对位图进行处理或显示
            // 例如: imageView.setImageBitmap(bitmap);
        });

        // 可以选择保存图像
        // saveImage(bitmap);
    }
    private void sendImageToServer(Bitmap bitmap) {
        // 将位图转换为字节数组并发送到 WebSocket
        // 这里可以将 Bitmap 转换为 JPEG 格式并发送
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
        byte[] byteArray = stream.toByteArray();
        webSocket.send(ByteString.of(byteArray)); // 发送字节数组
    }
    private void saveImage(Bitmap bitmap) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME,
                "Image_" + System.currentTimeMillis() + ".jpg");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

        values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX");
        values.put(MediaStore.Images.Media.IS_PENDING, 1);

        ContentResolver resolver = getContentResolver();
        Uri collection = MediaStore.Images.Media.getContentUri(
                MediaStore.VOLUME_EXTERNAL_PRIMARY);
        Uri imageUri = resolver.insert(collection, values);

        try {
            if (imageUri != null) {
                try (OutputStream out = resolver.openOutputStream(imageUri)) {
                    if (out != null) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
                    }
                }

                values.clear();
                values.put(MediaStore.Images.Media.IS_PENDING, 0);
                resolver.update(imageUri, values, null, null);
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to save image", e);
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this,
                        "Permissions not granted by the user.",
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
    private void setupWebSocket() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(WEBSOCKET_URL).build();

        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                Log.d(TAG, "WebSocket connection opened");
            }

            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
                Log.d(TAG, "Message from server: " + text);
            }

            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull ByteString bytes) {
                Log.d(TAG, "Message from server: " + bytes.hex());
            }

            @Override
            public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                Log.d(TAG, "WebSocket connection closed: " + reason);
            }

            @Override
            public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, Response response) {
                Log.e(TAG, "WebSocket error: ", t);
            }
        });
    }

}