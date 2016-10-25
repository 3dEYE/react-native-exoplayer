package com.threedeye.reactvideo;


import android.support.annotation.Nullable;
import android.view.ViewGroup;

import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;
import android.net.Uri;
import com.threedeye.reactvideo.ExoPlayerView.Events;

import java.util.Map;

public class ReactExoPlayerManager extends SimpleViewManager<ExoPlayerView > {

    public static final String REACT_CLASS = "RNExoPlayer";
    public static final String PROP_SRC = "source";
    public static final String PROP_PAUSED = "paused";
    public static final String PROP_MUTED = "muted";
    public static final String PROP_VOLUME = "volume";
    public static final String PROP_RATE = "rate";

    @Override
    public String getName() {
        return REACT_CLASS;
    }


    @Override
    public ExoPlayerView  createViewInstance(ThemedReactContext reactContext) {
        ExoPlayerView playerView = new ExoPlayerView(reactContext);
        return playerView;
    }

    @Override
    @Nullable
    public Map getExportedCustomDirectEventTypeConstants() {
        MapBuilder.Builder builder = MapBuilder.builder();
        for (Events event : Events.values()) {
            builder.put(event.toString(), MapBuilder.of("registrationName", event.toString()));
        }
        return builder.build();
    }

    @ReactProp(name = PROP_SRC)
    public void setUri(ExoPlayerView  view, String uri) {
        view.setUri(Uri.parse(uri));
    }

    @ReactProp(name = PROP_RATE, defaultFloat = 1.0f)
    public void setRate(ExoPlayerView  view, final float rate) {
        view.setRate(rate);
    }

    @ReactProp(name = PROP_VOLUME, defaultFloat = 1.0f)
    public void setVolume(ExoPlayerView  view, float volume) {
        view.setVolume(volume);
    }

    @ReactProp(name = PROP_MUTED, defaultBoolean = false)
    public void setMuted(ExoPlayerView  view, boolean isMuted) {
        view.setMuted(isMuted);
    }

    @ReactProp(name = PROP_PAUSED, defaultBoolean = false)
    public void setPaused(ExoPlayerView  view, boolean isPaused) {
        view.setPaused(isPaused);
    }
}
