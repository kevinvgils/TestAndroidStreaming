package com.example.finalfinal.StreamingService;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

public class H264Encoder {
    private static final String TAG = "H264Encoder";

    private static final String MIME_TYPE = "video/avc";
    private static final int TIMEOUT_US = 10000;

    private int width;
    private int height;
    private int frameRate;
    private String outputPath;

    private MediaCodec mediaCodec;
    private MediaCodec.BufferInfo bufferInfo;
    private H264EncodedDataListener encodedDataListener;

    public void setH264EncodedDataListener(H264EncodedDataListener listener) {
        this.encodedDataListener = listener;
    }

    public void startEncoder(int width, int height, int frameRate) {
        this.width = width;
        this.height = height;
        this.frameRate = frameRate;

        try {
            MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, width, height);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
            format.setInteger(MediaFormat.KEY_BIT_RATE, calculateBitRate(width, height, frameRate));
            format.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);

            mediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
            mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mediaCodec.start();

            bufferInfo = new MediaCodec.BufferInfo();


        } catch (IOException e) {
            Log.e(TAG, "Failed to start H.264 encoder: " + e.getMessage());
        }
    }

    public void encodeFrame(byte[] input) {
        if (mediaCodec == null) {
            Log.e(TAG, "Encoder is not initialized");
            return;
        }

        int inputBufferIndex = mediaCodec.dequeueInputBuffer(TIMEOUT_US);
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = getInputBuffer(inputBufferIndex);
            inputBuffer.clear();
            inputBuffer.put(input);

            mediaCodec.queueInputBuffer(inputBufferIndex, 0, input.length, 0, 0);
        }

        final boolean isHeader = (bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0;

        final boolean isKeyframe = (bufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0 && !isHeader;

        final long timestamp = bufferInfo.presentationTimeUs;

        int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US);
        while (outputBufferIndex >= 0) {
            ByteBuffer outputBuffer = getOutputBuffer(outputBufferIndex);
            byte[] encodedData = new byte[bufferInfo.size];
            outputBuffer.get(encodedData);

            if (encodedDataListener != null) {
                encodedDataListener.onH264EncodedData(encodedData, isHeader, isKeyframe, timestamp);
            }

            mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
            outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US);
        }
    }

    public void stopEncoder() {
        if (mediaCodec != null) {
            mediaCodec.stop();
            mediaCodec.release();
            mediaCodec = null;
        }
    }

    private ByteBuffer getInputBuffer(int index) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return mediaCodec.getInputBuffer(index);
        } else {
            return mediaCodec.getInputBuffers()[index];
        }
    }

    private ByteBuffer getOutputBuffer(int index) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return mediaCodec.getOutputBuffer(index);
        } else {
            return mediaCodec.getOutputBuffers()[index];
        }
    }

    private int calculateBitRate(int width, int height, int frameRate) {
        // Modify this method according to your desired bit rate calculation logic
        int bitRate = width * height * frameRate * 2; // Modify this calculation as needed
        return bitRate;
    }
}