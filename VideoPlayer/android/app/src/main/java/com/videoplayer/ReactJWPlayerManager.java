package com.videoplayer;

import android.support.annotation.Nullable;
import android.util.Log;

import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.longtailvideo.jwplayer.configuration.PlayerConfig;

/**
 * Created by ivan on 13.10.16.
 */

public class ReactJWPlayerManager extends SimpleViewManager<ReactJWPlayerView> {
    public static final String REACT_CLASS = "JWPlayerComponent";

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    protected ReactJWPlayerView createViewInstance(ThemedReactContext reactContext) {
        Log.d("ReactJWPlayerManager", "createViewInstance: ");
        PlayerConfig playerConfig = new PlayerConfig.Builder()
                .file("http://50.19.9.112/hls/c00007e00010p00124.m3u8")
                .autostart(true)
                .build();
        ReactJWPlayerView reactJWPlayerView = new ReactJWPlayerView(reactContext, playerConfig);
        return reactJWPlayerView;
    }

    @ReactProp(name = "url")
    public void setUrl(ReactJWPlayerView view, @Nullable String url) {
        view.setUrl(url);
    }

}
