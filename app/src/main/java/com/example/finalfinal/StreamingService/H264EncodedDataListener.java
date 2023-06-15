package com.example.finalfinal.StreamingService;

public interface H264EncodedDataListener {
    void onH264EncodedData(byte[] data, boolean isHeader, boolean isKeyFrame, long timeStamp);
}