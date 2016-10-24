package com.threedeye.reactvideo;

import android.media.MediaDrm;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.MediaController;

import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.uimanager.ThemedReactContext;
import com.google.android.exoplayer.AspectRatioFrameLayout;
import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.drm.MediaDrmCallback;
import com.google.android.exoplayer.util.PlayerControl;
import com.google.android.exoplayer.util.Util;
import com.threedeye.reactvideo.trackrenderer.DashRendererBuilder;
import com.threedeye.reactvideo.trackrenderer.ExtractorRendererBuilder;
import com.threedeye.reactvideo.trackrenderer.HlsRendererBuilder;
import com.threedeye.reactvideo.trackrenderer.SmoothStreamingRendererBuilder;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.Arguments;

import java.util.UUID;

public class ExoPlayerView extends FrameLayout implements ExoPlayer.Listener,
        LifecycleEventListener, SurfaceHolder.Callback {

    public enum Events {
                EVENT_ERROR("onError"),
                EVENT_PROGRESS("onProgress"),
                EVENT_END("onEnd");

        private final String mName;

        Events(final String name) {
            mName = name;
        }

        @Override
        public String toString() {
            return mName;
        }
    }

    public static final String EVENT_PROP_DURATION = "duration";
    public static final String EVENT_PROP_CURRENT_TIME = "currentTime";
    public static final String EVENT_PROP_ERROR = "error";
    private static final int RENDERER_COUNT = 2;

    private MediaController mMediaController = null;
    private Uri mUri;
    private ExoPlayer mPlayer;
    private float mRate;
    private long mPlayerPosition;
    private MediaCodecVideoTrackRenderer mVideoRenderer;
    private MediaCodecAudioTrackRenderer mAudioRenderer;
    private RendererBuilder mBuilder;
    private final Handler mHandler = new Handler();
    private ThemedReactContext mContext;
    private final AspectRatioFrameLayout mAspectRatioFrameLayout;
    private SurfaceView mSurfaceView;
    private float mVolume;
    private RCTEventEmitter mEventEmitter;
    private boolean mIsPlaying = true;

    public ExoPlayerView(ThemedReactContext context) {
        super(context.getCurrentActivity());
        mContext = context;
        context.addLifecycleEventListener(this);
        mEventEmitter = context.getJSModule(RCTEventEmitter.class);
        mAspectRatioFrameLayout = new AspectRatioFrameLayout(context.getCurrentActivity());
        mSurfaceView = new SurfaceView(context.getCurrentActivity());
        mSurfaceView.getHolder().addCallback(this);
        mMediaController = new MediaController(mContext.getCurrentActivity());
        mMediaController.setAnchorView(mSurfaceView);
        initializePlayerIfNeeded();
        mAspectRatioFrameLayout.addView(mSurfaceView,
                new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
        this.addView(mAspectRatioFrameLayout, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER));
        this.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    toggleControlsVisibility();
                }
                return true;
            }
        });
    }

    public void setUri(Uri uri) {
        if (uri == null){
            onError("URL is incorrect");
        } else {
            mUri = uri;
            mPlayerPosition = 0;
            initializePlayerIfNeeded();
            mPlayer.seekTo(mPlayerPosition);
            preparePlayer();
        }
    }

    public void setVolume(float volume) {
        mVolume = volume;
        if (mAudioRenderer == null) {
            return;
        }
        mPlayer.sendMessage(mAudioRenderer,  MediaCodecAudioTrackRenderer.MSG_SET_VOLUME, mVolume);
    }

    public void setRate(float rate) {

    }

    public void setMuted(boolean isMuted) {
        if (isMuted) {
            setVolume(0.0f);
        } else {
            setVolume(1.0f);
        }
    }

    public void setPaused(boolean isPaused) {
        mIsPlaying = !isPaused;
        if (mPlayer != null) {
            mPlayer.setPlayWhenReady(mIsPlaying);
        }
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        WritableMap event;
        switch (playbackState) {
            case ExoPlayer.STATE_ENDED:
                event = Arguments.createMap();
                mEventEmitter.receiveEvent(getId(), Events.EVENT_END.toString(), event);
                break;
            case ExoPlayer.STATE_READY:
                if (playWhenReady) {
                    event = Arguments.createMap();
                    event.putDouble(EVENT_PROP_CURRENT_TIME, mPlayer.getCurrentPosition() / 1000.0);
                    event.putDouble(EVENT_PROP_DURATION, mPlayer.getDuration() / 1000.0);
                    mEventEmitter.receiveEvent(getId(), Events.EVENT_PROGRESS.toString(), event);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onPlayWhenReadyCommitted() {

    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        onError(error.getMessage());
    }

    public void onError(String errorMessage){
        WritableMap event = Arguments.createMap();
        event.putString(EVENT_PROP_ERROR, errorMessage);
        mEventEmitter.receiveEvent(getId(), Events.EVENT_ERROR.toString(), event);
    }

    private void toggleControlsVisibility() {
        if (mMediaController.isShowing()) {
            mMediaController.hide();
        } else {
            mMediaController.show(0);
        }
    }

    private void initializePlayerIfNeeded() {
        if (mPlayer != null) {
            return;
        }
        mPlayer = ExoPlayer.Factory.newInstance(RENDERER_COUNT, 1000, 5000);
        mPlayer.addListener(this);
        mPlayer.seekTo(mPlayerPosition);
        if (mMediaController != null) {
            mMediaController.setMediaPlayer(new PlayerControl(mPlayer));
            mMediaController.setEnabled(true);
        }
    }

    private void preparePlayer() {
        if (mUri == null){
            return;
        }
        if (mBuilder == null) {
            mBuilder = getRendererBuilder();
        }
        mBuilder.buildRender(new RendererBuilderCallback() {
            @Override
            public void onRender(MediaCodecVideoTrackRenderer videoRenderer,
                                 MediaCodecAudioTrackRenderer audioRenderer) {
                mVideoRenderer = videoRenderer;
                mAudioRenderer = audioRenderer;
                mPlayer.prepare(videoRenderer, audioRenderer);
                maybeStartPlayback();
            }

            @Override
            public void onRenderFailure(Exception e) {
                onError(e.getMessage());
            }
        });
    }

    private RendererBuilder getRendererBuilder() {
        final int contentType = Util.inferContentType(mUri.toString());
        String userAgent = Util.getUserAgent(mContext, mContext.getPackageName());
        switch (contentType) {
            case Util.TYPE_OTHER:
                return new ExtractorRendererBuilder(mContext, mUri, userAgent);
            case Util.TYPE_HLS:
                return new HlsRendererBuilder(mContext, mUri, mHandler, userAgent);
            case Util.TYPE_DASH:
                return new DashRendererBuilder(mContext, mUri, mHandler, userAgent,
                        mPlayer.getPlaybackLooper(), mDrmCallback);
            case Util.TYPE_SS:
                return new SmoothStreamingRendererBuilder(mContext, mUri, mHandler, userAgent,
                        mPlayer.getPlaybackLooper(), mDrmCallback);
            default:
                throw new IllegalStateException();
        }
    }

    private void maybeStartPlayback() {
        Surface surface = mSurfaceView.getHolder().getSurface();
        if (mVideoRenderer == null || surface == null || mAudioRenderer == null) {
            return;
        }
        mPlayer.sendMessage(mVideoRenderer, MediaCodecVideoTrackRenderer.MSG_SET_SURFACE, surface);
        mPlayer.sendMessage(mAudioRenderer,  MediaCodecAudioTrackRenderer.MSG_SET_VOLUME, mVolume);
        mPlayer.setPlayWhenReady(mIsPlaying);
    }

    @Override
    public void onHostPause() {
        mPlayerPosition = mPlayer.getCurrentPosition();
        releasePlayer();
    }

    @Override
    public void onHostResume() {
        initializePlayerIfNeeded();
        preparePlayer();
    }

    @Override
    public void onHostDestroy() {
        releasePlayer();
    }

    private void releasePlayer() {
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer.removeListener(this);
            mPlayer = null;
            mBuilder.cancel();
            mVideoRenderer = null;
            mAudioRenderer = null;
        }
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

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        maybeStartPlayback();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    private final MediaDrmCallback mDrmCallback = new MediaDrmCallback() {
        @Override
        public byte[] executeProvisionRequest(UUID uuid, MediaDrm.ProvisionRequest request)
                throws Exception {
            return new byte[0];
        }

        @Override
        public byte[] executeKeyRequest(UUID uuid, MediaDrm.KeyRequest request) throws Exception {
            return new byte[0];
        }
    };


}