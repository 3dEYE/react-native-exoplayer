package com.threedeye.reactvideo.trackrenderer;

import android.content.Context;
import android.media.MediaCodec;
import android.net.Uri;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.google.android.exoplayer.DefaultLoadControl;
import com.google.android.exoplayer.LoadControl;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecSelector;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.chunk.ChunkSampleSource;
import com.google.android.exoplayer.chunk.ChunkSource;
import com.google.android.exoplayer.chunk.FormatEvaluator;
import com.google.android.exoplayer.dash.DashChunkSource;
import com.google.android.exoplayer.dash.DefaultDashTrackSelector;
import com.google.android.exoplayer.dash.mpd.AdaptationSet;
import com.google.android.exoplayer.dash.mpd.MediaPresentationDescription;
import com.google.android.exoplayer.dash.mpd.MediaPresentationDescriptionParser;
import com.google.android.exoplayer.dash.mpd.Period;
import com.google.android.exoplayer.dash.mpd.UtcTimingElement;
import com.google.android.exoplayer.dash.mpd.UtcTimingElementResolver;
import com.google.android.exoplayer.drm.MediaDrmCallback;
import com.google.android.exoplayer.drm.StreamingDrmSessionManager;
import com.google.android.exoplayer.drm.UnsupportedDrmException;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer.upstream.DefaultUriDataSource;
import com.google.android.exoplayer.upstream.UriDataSource;
import com.google.android.exoplayer.util.ManifestFetcher;
import com.threedeye.reactvideo.RendererBuilder;
import com.threedeye.reactvideo.RendererBuilderCallback;

import java.io.IOException;


public class DashRendererBuilder implements RendererBuilder,
        UtcTimingElementResolver.UtcTimingCallback,
        ManifestFetcher.ManifestCallback<MediaPresentationDescription> {

    private static final int BUFFER_SEGMENT_SIZE = 64 * 1024;
    private static final int VIDEO_BUFFER_SEGMENTS = 200;
    private static final int AUDIO_BUFFER_SEGMENTS = 54;
    private static final int LIVE_EDGE_LATENCY_MS = 30000;

    private static final int SECURITY_LEVEL_UNKNOWN = -1;
    private static final int SECURITY_LEVEL_1 = 1;
    private static final int SECURITY_LEVEL_3 = 3;

    private Context mContext;
    private String mUserAgent;
    private String mUrl;
    private Handler mHandler;
    private RendererBuilderCallback mCallback;
    private ManifestFetcher<MediaPresentationDescription> manifestFetcher;
    private Looper mPlaybackLooper;
    private boolean mIsCanceled;
    private UriDataSource mUriDataSource;
    private MediaPresentationDescription mMpd;
    private long mElapsedRealtimeOffset;
    private MediaDrmCallback mDrmCallback;

    public DashRendererBuilder(Context context, Uri url, Handler handler, String userAgent,
                               Looper playbackLooper, MediaDrmCallback drmCallback) {
        mContext = context;
        mUserAgent = userAgent;
        mUrl = url.toString();
        mHandler = handler;
        mPlaybackLooper = playbackLooper;
        mDrmCallback = drmCallback;
    }

    @Override
    public void cancel() {
        mIsCanceled = true;
    }

    @Override
    public void buildRender(RendererBuilderCallback callback) {
        mCallback = callback;
        mIsCanceled = false;
        mUriDataSource = new DefaultUriDataSource(mContext, mUserAgent);
        manifestFetcher = new ManifestFetcher<>(mUrl, mUriDataSource,
                new MediaPresentationDescriptionParser());
        manifestFetcher.singleLoad(mHandler.getLooper(), this);
    }


    @Override
    public void onTimestampResolved(UtcTimingElement utcTiming, long elapsedRealtimeOffset) {
        if (mIsCanceled) {
            return;
        }
        mElapsedRealtimeOffset = elapsedRealtimeOffset;
        build();
    }

    @Override
    public void onTimestampError(UtcTimingElement utcTiming, IOException e) {
        if (mIsCanceled) {
            return;
        }
        build();
    }


    @Override
    public void onSingleManifest(MediaPresentationDescription manifest) {
        if (mIsCanceled) {
            return;
        }
        mMpd = manifest;
        if (mMpd.dynamic && mMpd.utcTiming != null) {
            UtcTimingElementResolver.resolveTimingElement(mUriDataSource, mMpd.utcTiming,
                    manifestFetcher.getManifestLoadCompleteTimestamp(), this);
        } else {
            build();
        }
    }

    @Override
    public void onSingleManifestError(final IOException e) {
        if (mIsCanceled) {
            return;
        }
        mCallback.onRenderFailure(e);
    }

    private void build() {
        Period period = mMpd.getPeriod(0);
        LoadControl loadControl = new DefaultLoadControl(new DefaultAllocator(BUFFER_SEGMENT_SIZE));
        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();

        boolean hasContentProtection = false;
        for (int i = 0; i < period.adaptationSets.size(); i++) {
            AdaptationSet adaptationSet = period.adaptationSets.get(i);
            if (adaptationSet.type != AdaptationSet.TYPE_UNKNOWN) {
                hasContentProtection |= adaptationSet.hasContentProtection();
            }
        }

        boolean filterHdContent = false;
        StreamingDrmSessionManager drmSessionManager = null;
        if (hasContentProtection) {
            if (Build.VERSION.SDK_INT < 18) {
                mCallback.onRenderFailure(
                        new UnsupportedDrmException(UnsupportedDrmException.REASON_UNSUPPORTED_SCHEME));
                return;
            }
            try {
                drmSessionManager = StreamingDrmSessionManager.newWidevineInstance(mPlaybackLooper,
                        mDrmCallback, null, mHandler, new StreamingDrmSessionManager.EventListener() {
                            @Override
                            public void onDrmKeysLoaded() {

                            }

                            @Override
                            public void onDrmSessionManagerError(Exception e) {
                                if (mIsCanceled) {
                                    return;
                                }
                                mCallback.onRenderFailure(e);
                            }
                        });
                filterHdContent = getWidevineSecurityLevel(drmSessionManager) != SECURITY_LEVEL_1;
            } catch (final UnsupportedDrmException e) {
                if (!mIsCanceled) {
                    mCallback.onRenderFailure(e);
                }
                return;
            }
        }
        DataSource videoDataSource = new DefaultUriDataSource(mContext, bandwidthMeter, mUserAgent);
        ChunkSource videoChunkSource = new DashChunkSource(manifestFetcher,
                DefaultDashTrackSelector.newVideoInstance(mContext, true, filterHdContent),
                videoDataSource, new FormatEvaluator.AdaptiveEvaluator(bandwidthMeter),
                LIVE_EDGE_LATENCY_MS, mElapsedRealtimeOffset, mHandler, null, 0);
        ChunkSampleSource videoSampleSource = new ChunkSampleSource(videoChunkSource, loadControl,
                VIDEO_BUFFER_SEGMENTS * BUFFER_SEGMENT_SIZE, mHandler, null,
                0);
        MediaCodecVideoTrackRenderer videoRenderer = new MediaCodecVideoTrackRenderer(mContext,
                videoSampleSource, MediaCodecSelector.DEFAULT,
                MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT, 5000, drmSessionManager, true,
                mHandler, null, 50);

        DataSource audioDataSource = new DefaultUriDataSource(mContext, bandwidthMeter, mUserAgent);
        ChunkSource audioChunkSource = new DashChunkSource(manifestFetcher,
                DefaultDashTrackSelector.newAudioInstance(), audioDataSource, null,
                LIVE_EDGE_LATENCY_MS, mElapsedRealtimeOffset, mHandler, null, 1);
        ChunkSampleSource audioSampleSource = new ChunkSampleSource(audioChunkSource, loadControl,
                AUDIO_BUFFER_SEGMENTS * BUFFER_SEGMENT_SIZE, mHandler, null, 1);
        MediaCodecAudioTrackRenderer audioRenderer = new MediaCodecAudioTrackRenderer(
                audioSampleSource, MediaCodecSelector.DEFAULT, drmSessionManager, true, mHandler,
                null, AudioCapabilities.getCapabilities(mContext), AudioManager.STREAM_MUSIC);

        mCallback.onRender(videoRenderer, audioRenderer);
    }

    private static int getWidevineSecurityLevel(StreamingDrmSessionManager sessionManager) {
        String securityLevelProperty = sessionManager.getPropertyString("securityLevel");
        return securityLevelProperty.equals("L1") ? SECURITY_LEVEL_1 : securityLevelProperty
                .equals("L3") ? SECURITY_LEVEL_3 : SECURITY_LEVEL_UNKNOWN;
    }
}