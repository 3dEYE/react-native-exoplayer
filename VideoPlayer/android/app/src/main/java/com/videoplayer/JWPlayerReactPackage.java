package com.videoplayer;

import android.util.Log;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.JavaScriptModule;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ViewManager;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created by ivan on 13.10.16.
 */

public class JWPlayerReactPackage implements ReactPackage {
    private static final String TAG = "JWPlayerReactPackage";
    @Override
    public List<NativeModule> createNativeModules(ReactApplicationContext reactContext) {
        Log.d(TAG, "createNativeModules: ");
        return Collections.emptyList();
    }

    @Override
    public List<Class<? extends JavaScriptModule>> createJSModules() {
        Log.d(TAG, "createJSModules: ");
       return Collections.emptyList();
    }

    @Override
    public List<ViewManager> createViewManagers(ReactApplicationContext reactContext) {
        Log.d(TAG, "createViewManagers: ");
        return Arrays.<ViewManager>asList(
                new ReactJWPlayerManager()
        );
    }
}
