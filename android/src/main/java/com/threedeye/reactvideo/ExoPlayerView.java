package com.threedeye.reactvideo;

import android.media.PlaybackParams;
import android.net.Uri;
import android.os.Handler;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.ExoMediaDrm;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.drm.MediaDrmCallback;
import com.google.android.exoplayer2.drm.StreamingDrmSessionManager;
import com.google.android.exoplayer2.drm.UnsupportedDrmException;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.trackselection.AdaptiveVideoTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector.MappedTrackInfo;
import com.google.android.exoplayer2.trackselection.TrackSelections;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.PlaybackControlView;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;

import java.util.UUID;

public class ExoPlayerView extends FrameLayout implements ExoPlayer.EventListener,
        TrackSelector.EventListener<MappedTrackInfo>, LifecycleEventListener {

    public enum Events {
        EVENT_ERROR("sendErrorEvent"),
        EVENT_PROGRESS("onProgress"),
        EVENT_WARNING("onWarning"),
        EVENT_END("onEnd"),
        EVENT_SEEK("onSeek");

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
    private static final String EVENT_PROP_SEEK_TIME = "seekTime";

    private Uri mUri;
    private String mUserAgent;
    private SimpleExoPlayer mPlayer;
    private float mSpeed = 1.0f;
    private boolean mIsMuted = false;
    private long mPlayerPosition;
    private final Handler mHandler = new Handler();
    private ThemedReactContext mContext;
    private SimpleExoPlayerView mSimpleExoPlayerView;
    private float mVolume = 1.0f;
    private RCTEventEmitter mEventEmitter;
    private boolean mIsPlaying = true;
    private Runnable mProgressUpdateRunnable = null;
    private Handler mProgressUpdateHandler = new Handler();
    private boolean mIsDetached = false;
    private EventLogger mEventLogger;
    private MappingTrackSelector mTrackSelector;
    private DefaultBandwidthMeter mBandwidthMeter = new DefaultBandwidthMeter();
    private DataSource.Factory mMediaDataSourceFactory;

    public ExoPlayerView(ThemedReactContext context) {

        super(context.getCurrentActivity());
        mContext = context;
        mUserAgent = Util.getUserAgent(mContext, mContext.getPackageName());
        context.addLifecycleEventListener(this);
        mEventEmitter = context.getJSModule(RCTEventEmitter.class);
        mSimpleExoPlayerView = new SimpleExoPlayerView(mContext);
        this.addView(mSimpleExoPlayerView, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER));
        mMediaDataSourceFactory = buildDataSourceFactory();
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
        initializePlayer();
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
        if (mSimpleExoPlayerView != null) {
            mSimpleExoPlayerView.setUseController(isControlVisibile);
        }
    }

    private void sendProgressEvent(int currentTime, int duration) {
        WritableMap event = Arguments.createMap();
        event.putInt(EVENT_PROP_CURRENT_TIME, currentTime);
        event.putInt(EVENT_PROP_DURATION, duration);
        mEventEmitter.receiveEvent(getId(), Events.EVENT_PROGRESS.toString(), event);
    }

    private void sendSeekEvent() {
        //TODO
    }

    private void sendEndEvent() {
        WritableMap event = Arguments.createMap();
        mEventEmitter.receiveEvent(getId(), Events.EVENT_END.toString(), event);
    }

    private void sendErrorEvent(String errorMessage) {
        WritableMap event = Arguments.createMap();
        event.putString(EVENT_PROP_ERROR, errorMessage);
        mEventEmitter.receiveEvent(getId(), Events.EVENT_ERROR.toString(), event);
    }

    private void releasePlayer() {
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer.removeListener(this);
            mPlayer = null;
        }
        mTrackSelector = null;
        mEventLogger = null;
    }

    private void initializePlayer() {
        if (mPlayer != null) {
            return;
        }
        mEventLogger = new EventLogger();
        TrackSelection.Factory videoTrackSelectionFactory =
                new AdaptiveVideoTrackSelection.Factory(mBandwidthMeter);
        mTrackSelector = new DefaultTrackSelector(mHandler, videoTrackSelectionFactory);
        mTrackSelector.addListener(this);
        mTrackSelector.addListener(mEventLogger);
        mPlayer = ExoPlayerFactory.newSimpleInstance(mContext, mTrackSelector,
                new DefaultLoadControl(), buildDrmSessionManager());
        mPlayer.addListener(this);
        mPlayer.addListener(mEventLogger);
        mSimpleExoPlayerView.setPlayer(mPlayer);
    }

    private void preparePlayer() {
        if (mUri == null) {
            return;
        }
        changeSpeed();
        changeVolume();
        mPlayer.setPlayWhenReady(mIsPlaying);
        mPlayer.prepare(buildMediaSource(mUri));
    }

    private void changeVolume() {
        if (mPlayer == null) {
            return;
        }
        if (mIsMuted) {
            mPlayer.setVolume(0.0f);
        } else {
            mPlayer.setVolume(mVolume);
        }
    }

    private void changeSpeed() {
        try {
            if (mPlayer == null || mSpeed == 1.0f) {
                return;
            }
            if (RNExoPlayerModule.isRateSupported) {
                PlaybackParams playbackParams = new PlaybackParams();
                playbackParams.setSpeed(mSpeed);
                mPlayer.setPlaybackParams(playbackParams);
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

    private MediaSource buildMediaSource(Uri uri) {
        int type = Util.inferContentType(uri.getLastPathSegment());
        switch (type) {
            case C.TYPE_SS:
                return new SsMediaSource(uri, buildDataSourceFactory(),
                        new DefaultSsChunkSource.Factory(mMediaDataSourceFactory), mHandler,
                        mEventLogger);
            case C.TYPE_DASH:
                return new DashMediaSource(uri, buildDataSourceFactory(),
                        new DefaultDashChunkSource.Factory(mMediaDataSourceFactory), mHandler,
                        mEventLogger);
            case C.TYPE_HLS:
                return new HlsMediaSource(uri, mMediaDataSourceFactory, mHandler, mEventLogger);
            case C.TYPE_OTHER:
                return new ExtractorMediaSource(uri, mMediaDataSourceFactory,
                        new DefaultExtractorsFactory(), mHandler, mEventLogger);
            default: {
                throw new IllegalStateException("Unsupported type: " + type);
            }
        }
    }

    private DataSource.Factory buildDataSourceFactory() {
        return new DefaultDataSourceFactory(mContext, mBandwidthMeter,
                buildHttpDataSourceFactory());
    }

    private HttpDataSource.Factory buildHttpDataSourceFactory() {
        return new DefaultHttpDataSourceFactory(mUserAgent, mBandwidthMeter);
    }

    private DrmSessionManager<FrameworkMediaCrypto> buildDrmSessionManager() {
        if (Util.SDK_INT < 18) {
            return null;
        }
        StreamingDrmSessionManager drmSessionManager = null;
        try {
            drmSessionManager = StreamingDrmSessionManager.newWidevineInstance(mDrmCallback, null,
                    mHandler, mEventLogger);
        } catch (UnsupportedDrmException e) {
            e.printStackTrace();
        }
        return drmSessionManager;
    }

    @Override
    public void onLoadingChanged(boolean isLoading) {

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
                } else {
                    mProgressUpdateHandler.removeCallbacks(mProgressUpdateRunnable);
                }
                break;
            default:
                break;
        }
    }
    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest) {

    }

    @Override
    public void onTrackSelectionsChanged(TrackSelections<? extends MappedTrackInfo> trackSelections) {

    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        sendErrorEvent(error.getMessage());
    }

    @Override
    public void onPositionDiscontinuity() {

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
            initializePlayer();
            preparePlayer();
        }
    }

    @Override
    public void onHostDestroy() {
        releasePlayer();
    }

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

    private final MediaDrmCallback mDrmCallback = new MediaDrmCallback() {
        @Override
        public byte[] executeProvisionRequest(UUID uuid, ExoMediaDrm.ProvisionRequest request)
                throws Exception {
            return new byte[0];
        }

        @Override
        public byte[] executeKeyRequest(UUID uuid, ExoMediaDrm.KeyRequest request)
                throws Exception {
            return new byte[0];
        }
    };
}