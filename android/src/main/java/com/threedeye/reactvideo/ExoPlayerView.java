package com.threedeye.reactvideo;

import android.media.MediaCodec;
import android.media.MediaDrm;
import android.media.PlaybackParams;
import android.net.Uri;
import android.os.Handler;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.MediaController;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.google.android.exoplayer.AspectRatioFrameLayout;
import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecTrackRenderer;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.drm.MediaDrmCallback;
import com.google.android.exoplayer.util.PlayerControl;
import com.google.android.exoplayer.util.Util;
import com.threedeye.reactvideo.UnsupportedRateException;
import com.threedeye.reactvideo.trackrenderer.DashRendererBuilder;
import com.threedeye.reactvideo.trackrenderer.ExtractorRendererBuilder;
import com.threedeye.reactvideo.trackrenderer.HlsRendererBuilder;
import com.threedeye.reactvideo.trackrenderer.SmoothStreamingRendererBuilder;

import java.util.UUID;

public class ExoPlayerView extends FrameLayout implements ExoPlayer.Listener,
        LifecycleEventListener, SurfaceHolder.Callback, MediaCodecVideoTrackRenderer.EventListener {

    public enum Events {
        EVENT_ERROR("onError"),
        EVENT_PROGRESS("onProgress"),
        EVENT_WARNING("onWarning"),
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

    private static final String EVENT_PROP_DURATION = "duration";
    private static final String EVENT_PROP_CURRENT_TIME = "currentTime";
    private static final String EVENT_PROP_WARNING_MESSAGE = "warningMessage";
    private static final String EVENT_PROP_ERROR = "error";
    private static final int RENDERER_COUNT = 2;

    private MediaController mMediaController = null;
    private Uri mUri;
    private ExoPlayer mPlayer;
    private float mSpeed = 1.0f;
    private boolean mIsMuted = false;
    private long mPlayerPosition;
    private MediaCodecVideoTrackRenderer mVideoRenderer;
    private MediaCodecAudioTrackRenderer mAudioRenderer;
    private RendererBuilder mBuilder;
    private final Handler mHandler = new Handler();
    private ThemedReactContext mContext;
    private final AspectRatioFrameLayout mAspectRatioFrameLayout;
    private SurfaceView mSurfaceView;
    private float mVolume = 1.0f;
    private RCTEventEmitter mEventEmitter;
    private boolean mIsPlaying = true;
    private Runnable mProgressUpdateRunnable = null;
    private Handler mProgressUpdateHandler = new Handler();
    private boolean mIsDetached = false;
    private boolean mIsControlVisibile = true;

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
        mAspectRatioFrameLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    toggleControlsVisibility();
                }
                return true;
            }
        });
        mProgressUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                if (mPlayer != null) {
                    sendProgressEvent((int) mPlayer.getCurrentPosition(), (int) mPlayer.getDuration());
                }
                mProgressUpdateHandler.postDelayed(mProgressUpdateRunnable, 250);
            }
        };
    }

    public void setUri(Uri uri) {
        mUri = uri;
        initializePlayerIfNeeded();
        preparePlayer();
    }

    public void setSpeed(float speed) {
        mSpeed = speed;
        changeSpeed();
    }

    public void setVolume(float volume) {
        mVolume = volume;
        changeVolume();
    }

    public void setMuted(boolean isMuted) {
        mIsMuted = isMuted;
        changeVolume();
    }

    public void setPaused(boolean isPaused) {
        mIsPlaying = !isPaused;
        if (mPlayer != null) {
            mPlayer.setPlayWhenReady(mIsPlaying);
        }
    }

    public void seekTo(int position) {
        mPlayerPosition = position;
        if (mPlayer != null) {
            mPlayer.seekTo(mPlayerPosition);
        }
    }

    public void setControls(boolean isControlVisibile) {
        mIsControlVisibile = isControlVisibile;
        if (mMediaController != null && mMediaController.isShowing()) {
            mMediaController.hide();
        }
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        switch (playbackState) {
            case ExoPlayer.STATE_ENDED:
                mProgressUpdateHandler.removeCallbacks(mProgressUpdateRunnable);
                sendProgressEvent((int) mPlayer.getDuration(), (int) mPlayer.getDuration());
                sendEndEvent();
                break;
            case ExoPlayer.STATE_READY:
                if (playWhenReady) {
                    mProgressUpdateHandler.post(mProgressUpdateRunnable);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onPlayWhenReadyCommitted() {

    }

    private void sendProgressEvent(int currentTime, int duration) {
        WritableMap event = Arguments.createMap();
        event.putInt(EVENT_PROP_CURRENT_TIME, currentTime);
        event.putInt(EVENT_PROP_DURATION, duration);
        mEventEmitter.receiveEvent(getId(), Events.EVENT_PROGRESS.toString(), event);
    }

    private void sendEndEvent() {
        WritableMap event = Arguments.createMap();
        mEventEmitter.receiveEvent(getId(), Events.EVENT_END.toString(), event);
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        onError(error.getMessage());
    }

    public void onError(String errorMessage) {
        WritableMap event = Arguments.createMap();
        event.putString(EVENT_PROP_ERROR, errorMessage);
        mEventEmitter.receiveEvent(getId(), Events.EVENT_ERROR.toString(), event);
    }

    private void toggleControlsVisibility() {
        if (mMediaController.isShowing()) {
            mMediaController.hide();
        } else if (mIsControlVisibile) {
            mMediaController.show(0);
        }
    }

    private void initializePlayerIfNeeded() {
        if (mPlayer != null) {
            return;
        }
        mPlayer = ExoPlayer.Factory.newInstance(RENDERER_COUNT, 1000, 5000);
        mPlayer.addListener(this);
        if (mMediaController != null) {
            mMediaController.setMediaPlayer(new PlayerControl(mPlayer));
            mMediaController.setEnabled(true);
        }
    }

    private void preparePlayer() {
        if (mUri == null) {
            return;
        }
        mBuilder = getRendererBuilder();
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
        changeSpeed();
        changeVolume();
        mPlayer.setPlayWhenReady(mIsPlaying);
        mPlayer.seekTo(mPlayerPosition);
    }

    private void changeVolume() {
        if (mPlayer == null || mAudioRenderer == null) {
            return;
        }
        if (mIsMuted) {
            mPlayer.sendMessage(mAudioRenderer, MediaCodecAudioTrackRenderer.MSG_SET_VOLUME, 0.0f);
        } else {
            mPlayer.sendMessage(mAudioRenderer, MediaCodecAudioTrackRenderer.MSG_SET_VOLUME,
                    mVolume);
        }
    }

    private void changeSpeed() {
        try {
            if (mPlayer == null || mAudioRenderer == null || mSpeed == 1.0f) {
                return;
            }
            if (RNExoPlayerModule.isRateSupported) {
                PlaybackParams playbackParams = new PlaybackParams();
                playbackParams.setSpeed(mSpeed);
                mPlayer.sendMessage(mAudioRenderer,
                        MediaCodecAudioTrackRenderer.MSG_SET_PLAYBACK_PARAMS, playbackParams);
            } else {
                throw new UnsupportedRateException("Change of speed is supported " +
                        "starting from API level 23.");
            }
        } catch (UnsupportedRateException e) {
            e.printStackTrace();
            WritableMap event = Arguments.createMap();
            String warningMessage = e.getMessage() + '\n';
            for (StackTraceElement element : e.getStackTrace()) {
                if (element != null) {
                    warningMessage += '\n' + element.toString();
                }
            }
            event.putString(EVENT_PROP_WARNING_MESSAGE, warningMessage);
            mEventEmitter.receiveEvent(getId(), Events.EVENT_WARNING.toString(), event);
        }
    }

    @Override
    public void onHostPause() {
        if (mPlayer != null) {
            mPlayerPosition = mPlayer.getCurrentPosition();
        } else {
            mPlayerPosition = 0;
        }
        releasePlayer();
        mProgressUpdateHandler.removeCallbacks(mProgressUpdateRunnable);
    }

    @Override
    public void onHostResume() {
        if (!mIsDetached) {
            initializePlayerIfNeeded();
            preparePlayer();
            mProgressUpdateHandler.post(mProgressUpdateRunnable);
        }
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
        }
        if (mBuilder != null) {
            mBuilder.cancel();
            mBuilder = null;
        }
        mVideoRenderer = null;
        mAudioRenderer = null;
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

    @Override
    public void onDrawnToSurface(Surface surface) {

    }

    @Override
    public void onDroppedFrames(int count, long elapsed) {

    }

    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees,
                                   float pixelWidthAspectRatio) {
        mAspectRatioFrameLayout.setAspectRatio(
                height == 0 ? 1 : (width * pixelWidthAspectRatio) / height);
    }

    @Override
    public void onDecoderInitializationError(
            MediaCodecTrackRenderer.DecoderInitializationException e) {
    }

    @Override
    public void onCryptoError(MediaCodec.CryptoException e) {
    }

    @Override
    public void onDecoderInitialized(String decoderName, long elapsedRealtimeMs,
                                     long initializationDurationMs) {

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

    @Override
    protected void onDetachedFromWindow() {
        releasePlayer();
        mIsDetached = true;
        super.onDetachedFromWindow();
    }

    @Override
    protected void onAttachedToWindow() {
        mIsDetached = false;
        super.onAttachedToWindow();
    }
}