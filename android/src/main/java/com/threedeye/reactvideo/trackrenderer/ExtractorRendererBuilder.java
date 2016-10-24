package com.threedeye.reactvideo.trackrenderer;

import android.content.Context;
import android.media.MediaCodec;
import android.net.Uri;

import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecSelector;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.extractor.ExtractorSampleSource;
import com.google.android.exoplayer.upstream.Allocator;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.upstream.DefaultUriDataSource;
import com.threedeye.reactvideo.RendererBuilder;
import com.threedeye.reactvideo.RendererBuilderCallback;

public class ExtractorRendererBuilder implements RendererBuilder {

    private static final int BUFFER_SEGMENT_SIZE = 64 * 1024;
    private static final int BUFFER_SEGMENT_COUNT = 256;

    private Context mContext;
    private Uri mUri;
    private String mUserAgent;

    public ExtractorRendererBuilder(Context context, Uri url, String userAgent) {
        mContext = context;
        mUri = url;
        mUserAgent = userAgent;
    }

    @Override
    public void cancel(){

    }

    @Override
    public void buildRender(RendererBuilderCallback callback) {
        Allocator allocator = new DefaultAllocator(BUFFER_SEGMENT_SIZE);
        DataSource dataSource = new DefaultUriDataSource(mContext, null, mUserAgent);
        ExtractorSampleSource sampleSource = new ExtractorSampleSource(
                mUri, dataSource, allocator, BUFFER_SEGMENT_COUNT * BUFFER_SEGMENT_SIZE);
        MediaCodecVideoTrackRenderer videoRenderer = new MediaCodecVideoTrackRenderer(
                mContext, sampleSource, MediaCodecSelector.DEFAULT,
                MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT);
        MediaCodecAudioTrackRenderer audioRenderer = new MediaCodecAudioTrackRenderer(
                sampleSource, MediaCodecSelector.DEFAULT);
        callback.onRender(videoRenderer, audioRenderer);
    }
}
