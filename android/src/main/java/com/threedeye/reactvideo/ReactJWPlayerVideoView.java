package com.threedeye.reactvideo;

import android.os.Build;

import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.uimanager.ThemedReactContext;
import com.longtailvideo.jwplayer.JWPlayerView;
import com.longtailvideo.jwplayer.configuration.PlayerConfig;

public class ReactJWPlayerVideoView extends JWPlayerView implements LifecycleEventListener {

    ReactJWPlayerVideoView(ThemedReactContext themedReactContext, PlayerConfig playerConfig) {
        super(themedReactContext.getCurrentActivity(), playerConfig);
        themedReactContext.addLifecycleEventListener(this);
    }

    @Override
    public void onHostPause() {
        this.onPause();
    }

    @Override
    public void onHostResume() {
        this.onResume();
    }

    @Override
    public void onHostDestroy() {
        this.onDestroy();
    }

}
