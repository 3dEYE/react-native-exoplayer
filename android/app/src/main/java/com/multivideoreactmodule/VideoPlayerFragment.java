package com.multivideoreactmodule;


import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveVideoTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.VideoRendererEventListener;


public class VideoPlayerFragment extends Fragment implements ExoPlayer.EventListener,
        VideoRendererEventListener {

    private static final String ARGS_URI = "uri";
    private static final String ARGS_INDEX = "index";
    private static final int COLUMN_COUNT = 3;
    private static final String LOG = "VideoFragment";
    private SimpleExoPlayerView mVideoView;
    private SimpleExoPlayer mPlayer;
    private TrackSelector mTrackSelector;
    private int mPlayerPosition;
    private int mRowStart = 0;
    private int mColumnStart = 0;
    private Uri mUri;
    private Handler mHandler;


    public VideoPlayerFragment() {
    }

    public static VideoPlayerFragment newInstance(Uri uri, int index) {
        VideoPlayerFragment fragment = new VideoPlayerFragment();
        Bundle bundle = new Bundle();
        bundle.putString(ARGS_URI, uri.toString());
        bundle.putInt(ARGS_INDEX, index);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        if (bundle != null) {
            mUri = Uri.parse(bundle.getString(ARGS_URI));
            int index = bundle.getInt(ARGS_INDEX);
            mRowStart = index / COLUMN_COUNT;
            mColumnStart = index % COLUMN_COUNT;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_video_player, container, false);
        mVideoView = (SimpleExoPlayerView) view.findViewById(R.id.video_view);
        mHandler = new Handler();
        GridLayout.Spec rowSpec = GridLayout.spec(mRowStart);
        GridLayout.Spec columnSpec = GridLayout.spec(mColumnStart);
        GridLayout.LayoutParams childParams = new GridLayout.LayoutParams(rowSpec, columnSpec);
        view.setLayoutParams(childParams);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public void onStart() {
        super.onStart();
        initializePlayer();
    }


    @Override
    public void onPause() {
        super.onPause();

    }

    @Override
    public void onStop() {
        super.onStop();
        releasePlayer();
    }

    private void releasePlayer() {
        if (mPlayer != null) {
            mPlayer.removeListener(this);
            mPlayer.setVideoDebugListener(null);
            mPlayer.release();
            mPlayer = null;
            mTrackSelector = null;
        }
    }

    private void initializePlayer() {
        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory =
                new AdaptiveVideoTrackSelection.Factory(bandwidthMeter);
        mTrackSelector =
                new DefaultTrackSelector(mHandler, videoTrackSelectionFactory);
        LoadControl loadControl = new DefaultLoadControl();
        mPlayer = ExoPlayerFactory.newSimpleInstance(getContext(), mTrackSelector, loadControl);
        mPlayer.addListener(this);
        mVideoView.setPlayer(mPlayer);
        mPlayer.seekTo(mPlayerPosition);
        mPlayer.setPlayWhenReady(true);
        mPlayer.setVideoDebugListener(this);
        preparePlayer();
    }

    private void preparePlayer() {
        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(getContext(),
                Util.getUserAgent(getContext(), "RNVideoManager"), bandwidthMeter);
        ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
        MediaSource videoSource = new ExtractorMediaSource(mUri,
                dataSourceFactory, extractorsFactory, null, null);
        mPlayer.prepare(videoSource);
    }

    @Nullable
    protected VideoPlayerCallbacks getVideoPlayerCallbacks() {
        final FragmentActivity activity = getActivity();
        if (activity instanceof VideoPlayerCallbacks) {
            return (VideoPlayerCallbacks) activity;
        }
        return null;
    }


    protected void onSuccessLoad() {
        final VideoPlayerCallbacks videoPlayerCallbacks = getVideoPlayerCallbacks();
        if (videoPlayerCallbacks != null) {
            videoPlayerCallbacks.onSuccessLoad();
        }
    }


    protected void onFailedLoad() {
        final VideoPlayerCallbacks videoPlayerCallbacks = getVideoPlayerCallbacks();
        if (videoPlayerCallbacks != null) {
            videoPlayerCallbacks.onFailedLoad();
        }
    }

    @Override
    public void onLoadingChanged(boolean isLoading) {

    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {

    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest) {

    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        onFailedLoad();
    }


    @Override
    public void onPositionDiscontinuity() {

    }

    @Override
    public void onVideoEnabled(DecoderCounters counters) {

    }

    @Override
    public void onVideoDecoderInitialized(String decoderName, long initializedTimestampMs,
                                          long initializationDurationMs) {
        onSuccessLoad();
    }

    @Override
    public void onVideoInputFormatChanged(Format format) {

    }

    @Override
    public void onDroppedFrames(int count, long elapsedMs) {

    }

    @Override
    public void onVideoSizeChanged(int width, int height, int unAppliedRotationDegrees,
                                   float pixelWidthHeightRatio) {

    }

    @Override
    public void onRenderedFirstFrame(Surface surface) {

    }

    @Override
    public void onVideoDisabled(DecoderCounters counters) {

    }


}