package com.example.finalfinal.StreamingService;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.example.finalfinal.R;
import com.google.common.util.concurrent.ListenableFuture;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraService {

    private RtmpSender rtmpSender;

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ExecutorService cameraExecutor;
    private PreviewView previewView;
    private Context context;

    private ImageAnalysis unencryptedImageAnalysis;
    private byte[] currentFrame;

    public CameraService(PreviewView view, Context context){
        previewView = view;
        this.context = context;
    }

    public void startCamera(){
        initCamera();
        startProcessCamera();
    }

    private void initCamera(){
        cameraProviderFuture = ProcessCameraProvider.getInstance(context);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(context));

        // Create an executor service for camera operations
        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    private void startProcessCamera(){
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindCamera(cameraProvider);
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(context));
    }

    private void bindCamera(ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder()
                .build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        unencryptedImageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        unencryptedImageAnalysis.setAnalyzer(cameraExecutor, unencryptedImageAnalyzer);

        try {
            Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, preview, unencryptedImageAnalysis);
            CameraControl cameraControl = camera.getCameraControl();
            // Additional camera control operations can be performed here if needed.
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final ImageAnalysis.Analyzer unencryptedImageAnalyzer = new ImageAnalysis.Analyzer() {

        @Override
        public void analyze(@NonNull ImageProxy image) {
            // Convert the ImageProxy to byte array
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] data = new byte[buffer.capacity()];
            buffer.get(data);

            currentFrame = data;

            image.close();
        }
    };

    public byte[] getCurrentFrame() {
        return currentFrame;
    }
}
