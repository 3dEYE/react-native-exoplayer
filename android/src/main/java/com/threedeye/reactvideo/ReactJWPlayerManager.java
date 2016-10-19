package com.threedeye.reactvideo;


import android.support.annotation.Nullable;
import android.view.ViewGroup;

import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.longtailvideo.jwplayer.configuration.PlayerConfig;
import com.longtailvideo.jwplayer.media.playlists.PlaylistItem;


public class ReactJWPlayerManager extends SimpleViewManager<ReactJWPlayerVideoView> {

    public static final String REACT_CLASS = "RCTJWPlayer";

    @Override
    public String getName() {
        return REACT_CLASS;
    }


    @Override
    public ReactJWPlayerVideoView createViewInstance(ThemedReactContext reactContext) {
        PlayerConfig playerConfig = new PlayerConfig.Builder()
                .file("https://content.jwplatform.com/manifests/vM7nH0Kl.m3u8")
                .autostart(true)
                .build();

        ReactJWPlayerVideoView playerView = new ReactJWPlayerVideoView(reactContext, playerConfig);

        return playerView;
    }

    @ReactProp(name = "file")
    public void setSource(ReactJWPlayerVideoView view, String src) {
        //PlaylistItem item = new PlaylistItem(src);
        //view.load(item);
    }
}
