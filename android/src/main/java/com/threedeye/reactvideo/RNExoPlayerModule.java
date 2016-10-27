package com.threedeye.reactvideo;

import android.os.Build;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

public class RNExoPlayerModule extends ReactContextBaseJavaModule {

    private static final String CLASS_NAME = "RNEPManager";

    public RNExoPlayerModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return CLASS_NAME;
    }

    @ReactMethod
    public void isRateSupported(Promise promise) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            promise.resolve(true);
        } else {
            promise.resolve(false);
        }
    }
}
