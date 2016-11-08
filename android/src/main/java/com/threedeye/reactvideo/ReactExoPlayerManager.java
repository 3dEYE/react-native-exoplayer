package com.threedeye.reactvideo;

import android.net.Uri;
import android.support.annotation.Nullable;

import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.threedeye.reactvideo.ExoPlayerView.Events;

import java.util.Map;

public class ReactExoPlayerManager extends SimpleViewManager<ExoPlayerView> {

    private static final String REACT_CLASS = "RNExoPlayer";
    private static final String PROP_SRC = "source";
    private static final String PROP_PAUSED = "paused";
    private static final String PROP_MUTED = "muted";
    private static final String PROP_VOLUME = "volume";
    private static final String PROP_RATE = "rate";
    private static final String PROP_CONTROLS = "controls";
    private static final String PROP_SEEK_TO = "seekTo";

    @Override
    public String getName() {
        return REACT_CLASS;
    }


    @Override
    public ExoPlayerView createViewInstance(ThemedReactContext reactContext) {
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
    public void setUri(ExoPlayerView view, String uri) {
        view.setUri(Uri.parse(uri));
    }

    @ReactProp(name = PROP_RATE)
    public void setSpeed(ExoPlayerView view, final float rate) {
        view.setSpeed(rate);
    }

    @ReactProp(name = PROP_SEEK_TO)
    public void setSeek(ExoPlayerView view, final int position) {
        view.seekTo(position);
    }

    @ReactProp(name = PROP_VOLUME, defaultFloat = 1.0f)
    public void setVolume(ExoPlayerView view, float volume) {
        view.setVolume(volume);
    }

    @ReactProp(name = PROP_MUTED, defaultBoolean = false)
    public void setMuted(ExoPlayerView view, boolean isMuted) {
        view.setMuted(isMuted);
    }

    @ReactProp(name = PROP_PAUSED, defaultBoolean = false)
    public void setPaused(ExoPlayerView view, boolean isPaused) {
        view.setPaused(isPaused);
    }

    @ReactProp(name = PROP_CONTROLS, defaultBoolean = true)
    public void setControls(ExoPlayerView view, boolean isControlVisibile) {
        view.setControls(isControlVisibile);
    }
}