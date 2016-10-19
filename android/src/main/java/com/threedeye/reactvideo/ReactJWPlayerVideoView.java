package com.threedeye.reactvideo;

import android.os.Build;

import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.uimanager.ThemedReactContext;
import com.longtailvideo.jwplayer.JWPlayerView;
import com.longtailvideo.jwplayer.configuration.PlayerConfig;

public class ReactJWPlayerVideoView extends JWPlayerView  {

    ReactJWPlayerVideoView(ThemedReactContext themedReactContext, PlayerConfig playerConfig) {
        super(themedReactContext.getCurrentActivity(), playerConfig);
    }

    @Override
    protected void onAttachedToWindow() {

        super.onAttachedToWindow();
    }

    private final Runnable mLayoutRunnable = new Runnable() {
    @Override
    public void run() {
      measure(
          MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.EXACTLY),
          MeasureSpec.makeMeasureSpec(getHeight(), MeasureSpec.EXACTLY));
      layout(getLeft(), getTop(), getRight(), getBottom());
    }
    };

    @Override
    public void requestLayout() {
        super.requestLayout();
        post(mLayoutRunnable);
  }

}
