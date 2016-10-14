package com.videoplayer;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.uimanager.ThemedReactContext;
import com.longtailvideo.jwplayer.JWPlayerView;
import com.longtailvideo.jwplayer.configuration.PlayerConfig;

/**
 * Created by ivan on 13.10.16.
 */

public class ReactJWPlayerView extends JWPlayerView implements LifecycleEventListener{
    private static final String TAG = "ReactJWPlayerView";
    private static String sUrl;

    public ReactJWPlayerView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        Log.d(TAG, "ReactJWPlayerView: ");
    }

    public ReactJWPlayerView(ThemedReactContext context, PlayerConfig playerConfig) {
        super(context.getCurrentActivity(), playerConfig);
        Log.d(TAG, "ReactJWPlayerView: ");
    }

    @Override
    public void onHostResume() {
        Log.d(TAG, "onHostResume: ");
    }

    @Override
    public void onHostPause() {
        Log.d(TAG, "onHostPause: ");
    }

    @Override
    public void onHostDestroy() {
        Log.d(TAG, "onHostDestroy: ");
    }

    public void setUrl(String url) {
        sUrl = url;
    }

    public String getUrl() {
        return sUrl;
    }
}
