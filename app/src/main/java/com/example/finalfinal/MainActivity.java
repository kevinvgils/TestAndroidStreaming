package com.example.finalfinal;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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

import com.google.common.util.concurrent.ListenableFuture;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ExecutorService cameraExecutor;
    private PreviewView previewView;
    private Button startButton;

    private boolean isStreaming = false;

    private ImageAnalysis encryptedImageAnalysis;
    private ImageAnalysis unencryptedImageAnalysis;
    private WebSocketClientEndpointTest clientEndPoint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize the start button
        startButton = findViewById(R.id.startButton);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                establishSocketConnection();
            }
        });
        previewView = findViewById(R.id.previewView);

        // Initialize CameraX
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));

        // Create an executor service for camera operations
        cameraExecutor = Executors.newSingleThreadExecutor();
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

        encryptedImageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        unencryptedImageAnalysis.setAnalyzer(cameraExecutor, unencryptedImageAnalyzer);
        encryptedImageAnalysis.setAnalyzer(cameraExecutor, encryptedImageAnalyzer);

        try {
            Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, preview, unencryptedImageAnalysis, encryptedImageAnalysis);
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
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);

            // Send the unencrypted frame to the server
            sendFrameToUnencryptedServer(data);

            image.close();
        }
    };

    private final ImageAnalysis.Analyzer encryptedImageAnalyzer = new ImageAnalysis.Analyzer() {
        @Override
        public void analyze(@NonNull ImageProxy image) {
            // Convert the ImageProxy to byte array
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);

            // Encrypt the frame data
//            byte[] encryptedData = encrypt(data);
            // Send the encrypted frame to the server
//            sendFrameToEncryptedServer(data);

            image.close();
        }
    };

    private void startStreaming() {
        // Update the UI
        startButton.setText("Stop Streaming");
        isStreaming = true;

        // Initialize the camera provider
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

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
        }, ContextCompat.getMainExecutor(this));
    }

    private void stopStreaming() {
        // Update the UI
        startButton.setText("Start Streaming");
        isStreaming = false;

        // Release the camera resources
        unencryptedImageAnalysis.clearAnalyzer();
        encryptedImageAnalysis.clearAnalyzer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }

    private void sendFrameToUnencryptedServer(byte[] frameData) {
        if (clientEndPoint != null) {
            clientEndPoint.sendMessage("{'streamId': 'test'," + Arrays.toString(frameData));
        } else {
            System.out.println("No UnencryptedServer");
            // Unencrypted WebSocket connection is not established
            // Handle the case accordingly
        }
    }

    private void sendFrameToEncryptedServer(byte[] frameData) {
        if (clientEndPoint != null) {
            clientEndPoint.sendFrame(frameData);
        } else {
            // Encrypted WebSocket connection is not established
            // Handle the case accordingly
        }
    }

    private void establishSocketConnection() {
        if (isStreaming) {
            stopStreaming();
        } else {
            try {
                URI url = new URI("ws://145.49.10.56:8080");
                clientEndPoint = new WebSocketClientEndpointTest(url);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
            // add listener
            clientEndPoint.addMessageHandler(new WebSocketClientEndpointTest.MessageHandler() {
                public void handleMessage(String message) {
                    System.out.println(message);
                }
            });
            startStreaming();
        }
    }
}
