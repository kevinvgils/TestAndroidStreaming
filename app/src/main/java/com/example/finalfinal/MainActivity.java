package com.example.finalfinal;

import android.app.Activity;
import android.content.Context;
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

import com.example.finalfinal.StreamingService.CameraService;
import com.example.finalfinal.StreamingService.H264Encoder;
import com.example.finalfinal.StreamingService.RtmpSender;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.octiplex.android.rtmp.RtmpConnectionListener;

public class MainActivity extends AppCompatActivity {
    private Button startButton;

    private boolean isStreaming = false;

    private WebSocketClientEndpointTest clientEndPoint;

    private CameraService cam;
    private RtmpSender sender;
    private H264Encoder encoder;

    private Activity activity;

    private LifecycleOwner cycle = this;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        activity = this;

        // =====Initialize the start button=====
        startButton = findViewById(R.id.startButton);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                encoder = new H264Encoder();
                encoder.startEncoder(100, 100, 30);

                cam = new CameraService(findViewById(R.id.previewView), activity);
                cam.startCamera();

                sender = new RtmpSender(cam);
                sender.startMuxerConnection();
            }
        });

    }
}
