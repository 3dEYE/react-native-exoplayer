package com.threedeye.reactvideo.trackrenderer;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaCodec;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Handler;

import com.google.android.exoplayer.DefaultLoadControl;
import com.google.android.exoplayer.LoadControl;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecSelector;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.chunk.ChunkSampleSource;
import com.google.android.exoplayer.chunk.ChunkSource;
import com.google.android.exoplayer.chunk.FormatEvaluator.AdaptiveEvaluator;
import com.google.android.exoplayer.drm.MediaDrmCallback;
import com.google.android.exoplayer.drm.StreamingDrmSessionManager;
import com.google.android.exoplayer.drm.UnsupportedDrmException;
import com.google.android.exoplayer.drm.DrmSessionManager;
import com.google.android.exoplayer.smoothstreaming.DefaultSmoothStreamingTrackSelector;
import com.google.android.exoplayer.smoothstreaming.SmoothStreamingChunkSource;
import com.google.android.exoplayer.smoothstreaming.SmoothStreamingManifest;
import com.google.android.exoplayer.smoothstreaming.SmoothStreamingManifestParser;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer.upstream.DefaultUriDataSource;
import com.google.android.exoplayer.upstream.UriDataSource;
import com.google.android.exoplayer.util.ManifestFetcher;
import com.threedeye.reactvideo.RendererBuilder;
import com.threedeye.reactvideo.RendererBuilderCallback;
import com.google.android.exoplayer.util.Util;

import java.io.IOException;

public class SmoothStreamingRendererBuilder implements RendererBuilder,
        ManifestFetcher.ManifestCallback<SmoothStreamingManifest> {

    private static final int BUFFER_SEGMENT_SIZE = 64 * 1024;
    private static final int VIDEO_BUFFER_SEGMENTS = 200;
    private static final int AUDIO_BUFFER_SEGMENTS = 54;
    private static final int LIVE_EDGE_LATENCY_MS = 30000;

    private Context mContext;
    private String mUserAgent;
    private String mUrl;
    private RendererBuilderCallback mCallback;
    private Handler mHandler;
    private Looper mPlaybackLooper;
    private boolean mIsCanceled;
    private UriDataSource mUriDataSource;
    private MediaDrmCallback mDrmCallback;
    private ManifestFetcher<SmoothStreamingManifest> mManifestFetcher;

    public SmoothStreamingRendererBuilder(Context context, Uri url, Handler handler, String userAgent,
                                          Looper playbackLooper, MediaDrmCallback drmCallback) {
        mContext = context;
        mUserAgent = userAgent;
        mUrl = url.toString();
        mUrl = Util.toLowerInvariant(mUrl).endsWith("/manifest") ? mUrl : mUrl + "/Manifest";
        mHandler = handler;
        mPlaybackLooper = playbackLooper;
        mDrmCallback = drmCallback;
    }


    @Override
    public void buildRender(RendererBuilderCallback callback) {
        mCallback = callback;
        mIsCanceled = false;
        SmoothStreamingManifestParser parser = new SmoothStreamingManifestParser();
        mManifestFetcher =
                new ManifestFetcher<>(mUrl, new DefaultHttpDataSource(mUserAgent, null), parser);
        mManifestFetcher.singleLoad(mHandler.getLooper(), this);
    }

    @Override
    public void cancel(){
        mIsCanceled = true;
    }

    @Override
    public void onSingleManifest(SmoothStreamingManifest manifest) {
        if (mIsCanceled) {
            return;
        }
        build(manifest);
    }

    @Override
    public void onSingleManifestError(final IOException e) {
        if (mIsCanceled) {
            return;
        }
        e.printStackTrace();
        mCallback.onRenderFailure(e);
    }

    private void build(SmoothStreamingManifest manifest) {
        LoadControl loadControl = new DefaultLoadControl(new DefaultAllocator(BUFFER_SEGMENT_SIZE));
        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();

        DrmSessionManager drmSessionManager = null;
        if (manifest.protectionElement != null) {
            if (Util.SDK_INT < 18) {
                mCallback.onRenderFailure(
                        new UnsupportedDrmException(
                                UnsupportedDrmException.REASON_UNSUPPORTED_SCHEME));
                return;
            }
            try {
                drmSessionManager = new StreamingDrmSessionManager(manifest.protectionElement.uuid,
                        mPlaybackLooper, mDrmCallback, null, mHandler, null);
            } catch (UnsupportedDrmException e) {
                mCallback.onRenderFailure(e);
                return;
            }
        }

        DataSource videoDataSource = new DefaultUriDataSource(mContext, bandwidthMeter, mUserAgent);
        ChunkSource videoChunkSource = new SmoothStreamingChunkSource(mManifestFetcher,
                DefaultSmoothStreamingTrackSelector.newVideoInstance(mContext, true, false),
                videoDataSource, new AdaptiveEvaluator(bandwidthMeter), LIVE_EDGE_LATENCY_MS);
        ChunkSampleSource videoSampleSource = new ChunkSampleSource(videoChunkSource, loadControl,
                VIDEO_BUFFER_SEGMENTS * BUFFER_SEGMENT_SIZE, mHandler, null, 0);
        MediaCodecVideoTrackRenderer videoRenderer = new MediaCodecVideoTrackRenderer(mContext,
                videoSampleSource, MediaCodecSelector.DEFAULT,
                MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT, 5000, drmSessionManager, true,
                mHandler, null, 50);

        DataSource audioDataSource = new DefaultUriDataSource(mContext, bandwidthMeter, mUserAgent);
        ChunkSource audioChunkSource = new SmoothStreamingChunkSource(mManifestFetcher,
                DefaultSmoothStreamingTrackSelector.newAudioInstance(),
                audioDataSource, null, LIVE_EDGE_LATENCY_MS);
        ChunkSampleSource audioSampleSource = new ChunkSampleSource(audioChunkSource, loadControl,
                AUDIO_BUFFER_SEGMENTS * BUFFER_SEGMENT_SIZE, mHandler, null, 1);
        MediaCodecAudioTrackRenderer audioRenderer = new MediaCodecAudioTrackRenderer(audioSampleSource,
                MediaCodecSelector.DEFAULT, drmSessionManager, true, mHandler, null,
                AudioCapabilities.getCapabilities(mContext), AudioManager.STREAM_MUSIC);

        mCallback.onRender(videoRenderer, audioRenderer);
    }
}