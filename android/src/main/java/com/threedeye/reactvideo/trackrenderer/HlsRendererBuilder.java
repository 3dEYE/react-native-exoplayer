package com.threedeye.reactvideo.trackrenderer;

import android.content.Context;
import android.media.MediaCodec;
import android.net.Uri;
import android.os.Handler;
import com.google.android.exoplayer.DefaultLoadControl;
import com.google.android.exoplayer.LoadControl;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecSelector;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.hls.DefaultHlsTrackSelector;
import com.google.android.exoplayer.hls.HlsChunkSource;
import com.google.android.exoplayer.hls.HlsPlaylist;
import com.google.android.exoplayer.hls.HlsPlaylistParser;
import com.google.android.exoplayer.hls.HlsSampleSource;
import com.google.android.exoplayer.hls.PtsTimestampAdjusterProvider;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer.upstream.DefaultUriDataSource;
import com.google.android.exoplayer.util.ManifestFetcher;
import com.threedeye.reactvideo.RendererBuilder;
import com.threedeye.reactvideo.RendererBuilderCallback;

import java.io.IOException;


public class HlsRendererBuilder implements RendererBuilder,
        ManifestFetcher.ManifestCallback<HlsPlaylist> {

    private static final int BUFFER_SEGMENT_SIZE = 64 * 1024;
    private static final int MAIN_BUFFER_SEGMENTS = 254;

    private Context mContext;
    private String mUserAgent;
    private String mUrl;
    private ManifestFetcher<HlsPlaylist> mPlaylistFetcher;
    private RendererBuilderCallback mCallback;
    private Handler mHandler;
    private boolean mIsCanceled;

    @Override
    public void cancel(){
        mIsCanceled = true;
    }

    @Override
    public void onSingleManifest(HlsPlaylist manifest) {
        if (mIsCanceled){
            return;
        }
        LoadControl loadControl = new DefaultLoadControl(new DefaultAllocator(BUFFER_SEGMENT_SIZE));
        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        PtsTimestampAdjusterProvider timestampAdjusterProvider = new PtsTimestampAdjusterProvider();
        DataSource dataSource = new DefaultUriDataSource(mContext, bandwidthMeter, mUserAgent);
        HlsChunkSource chunkSource = new HlsChunkSource(true, dataSource, manifest,
                DefaultHlsTrackSelector.newDefaultInstance(mContext), bandwidthMeter,
                timestampAdjusterProvider, HlsChunkSource.ADAPTIVE_MODE_SPLICE);
        HlsSampleSource sampleSource = new HlsSampleSource(chunkSource, loadControl,
                MAIN_BUFFER_SEGMENTS * BUFFER_SEGMENT_SIZE);
        MediaCodecVideoTrackRenderer videoRenderer = new MediaCodecVideoTrackRenderer(mContext,
                sampleSource, MediaCodecSelector.DEFAULT, MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT);
        MediaCodecAudioTrackRenderer audioRenderer = new MediaCodecAudioTrackRenderer(sampleSource,
                MediaCodecSelector.DEFAULT);
        mCallback.onRender(videoRenderer, audioRenderer);

    }

    @Override
    public void onSingleManifestError(IOException e) {
        e.printStackTrace();
        if (!mIsCanceled) {
            mCallback.onRenderFailure(e);
        }
    }

    public HlsRendererBuilder(Context context, Uri url, Handler handler, String userAgent) {
        mContext = context;
        mUserAgent = userAgent;
        mUrl = url.toString();
        mHandler = handler;
    }

    @Override
    public void buildRender(RendererBuilderCallback callback) {
        mCallback = callback;
        mIsCanceled = false;
        HlsPlaylistParser parser = new HlsPlaylistParser();
        mPlaylistFetcher =
                new ManifestFetcher<>(mUrl, new DefaultUriDataSource(mContext, mUserAgent), parser);
        mPlaylistFetcher.singleLoad(mHandler.getLooper(), this);
    }
}
