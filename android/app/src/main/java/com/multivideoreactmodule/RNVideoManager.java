package com.multivideoreactmodule;


import android.app.Activity;
import android.content.Intent;
import android.media.MediaCodecInfo;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

public class RNVideoManager extends ReactContextBaseJavaModule implements ActivityEventListener {

    public static final int MAX_SUPPORTED_DECODER_COUNT_REQUEST = 1;
    public static final String EXTRA_MAX_SUPPORTED_VIDEO_DECODERS = "max_supported_video_decoders";
    public static final String ERROR_CODE = "error";
    public static final String ERROR_MESSAGE = "request error";
    private Promise mPromise = null;

    public RNVideoManager(ReactApplicationContext reactContext) {
        super(reactContext);
        reactContext.addActivityEventListener(this);
    }

    @Override
    public String getName() {
        return "RNVideoManager";
    }

    @ReactMethod
    public void getMaxSupportedVideoPlayersCount(String loadingMessage, Promise promise) {
        if (promise != null) {
            mPromise = promise;
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                MediaCodecInfo.CodecCapabilities codecCapabilities =
                        new MediaCodecInfo.CodecCapabilities();
                mPromise.resolve(codecCapabilities.getMaxSupportedInstances());
            } else {
                Activity activity = getCurrentActivity();
                if (activity != null) {
                    Intent intent = new Intent(getReactApplicationContext(),
                            MultiPlayerActivity.class);
                    activity.startActivityForResult(intent, MAX_SUPPORTED_DECODER_COUNT_REQUEST);
                    if (loadingMessage != null) {
                        Toast.makeText(getReactApplicationContext(), loadingMessage,
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        if (requestCode == MAX_SUPPORTED_DECODER_COUNT_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                int count = data.getIntExtra(EXTRA_MAX_SUPPORTED_VIDEO_DECODERS, -1);
                if (count > 0) {
                    mPromise.resolve(count);
                } else {
                    mPromise.reject(ERROR_CODE, ERROR_MESSAGE);
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                mPromise.reject(ERROR_CODE, ERROR_MESSAGE);
            }
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
    }
}
