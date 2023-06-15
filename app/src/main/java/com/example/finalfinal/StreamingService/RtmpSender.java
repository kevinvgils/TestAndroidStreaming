package com.example.finalfinal.StreamingService;

import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.NonNull;

import com.octiplex.android.rtmp.H264VideoFrame;
import com.octiplex.android.rtmp.RtmpConnectionListener;
import com.octiplex.android.rtmp.RtmpMuxer;
import com.octiplex.android.rtmp.Time;

import java.io.IOException;

public class RtmpSender implements RtmpConnectionListener {
    private RtmpMuxer muxer;
    private RtmpSender sender = this;
    CameraService cam;
    H264Encoder encoder;

    public RtmpSender(CameraService cam) {
        this.cam = cam;
    }

    public void startMuxerConnection(){
        initMuxer();
    }

    private void initMuxer()
    {
        muxer = new RtmpMuxer("145.49.8.154", 1935, new Time()
        {
            @Override
            public long getCurrentTimestamp()
            {
                return System.currentTimeMillis();
            }
        });

        // Always call start method from a background thread.


        new AsyncTask<Void, Void, Void>()
        {
            @Override
            protected Void doInBackground(Void... params)
            {
                muxer.start( sender, "live", null, null);
                return null;
            }
        }.execute();
    }

    @Override
    public void onConnected()
    {
        // Muxer is connected to the RTMP server, you can create a stream to publish data
        new AsyncTask<Void, Void, Void>()
        {
            @Override
            protected Void doInBackground(Void... params)
            {
                try {
                    muxer.createStream("goldo");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return null;
            }
        }.execute();
    }

    @Override
    public void onReadyToPublish()
    {
        // Muxer is connected to the server and ready to receive data
        cam.startStream();
        encoder.encodeFrame(cam.getCurrentFrame());


        encoder.setH264EncodedDataListener(new H264EncodedDataListener() {
            @Override
            public void onH264EncodedData(byte[] data, boolean isHeader, boolean isKeyFrame, long timeStamp) {
                // Process the encoded H.264 data here
                // You can send it over the network, save to a file, or handle it as needed

                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        try {
                            muxer.postVideo(new H264VideoFrame() {
                                @Override
                                public boolean isHeader() {
                                    return isHeader;
                                }

                                @Override
                                public long getTimestamp() {
                                    return timeStamp;
                                }

                                @NonNull
                                @Override
                                public byte[] getData() {
                                    return data;
                                }

                                @Override
                                public boolean isKeyframe() {
                                    return isKeyFrame;
                                }
                            });
                        } catch (IOException e) {
                            // An error occured while sending the video frame to the server
                        }

                        return null;
                    }
                }.execute();
            }
        });
    }

    @Override
    public void onConnectionError(@NonNull IOException e) {
        Log.e("ERROR", "onConnectionError: ", e);
    }
}
