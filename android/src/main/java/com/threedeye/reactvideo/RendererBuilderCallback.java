package com.threedeye.reactvideo;

import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;

public interface RendererBuilderCallback {

    void onRender(MediaCodecVideoTrackRenderer videoRenderer,
                  MediaCodecAudioTrackRenderer audioRenderer);

    void onRenderFailure(Exception e);
}
