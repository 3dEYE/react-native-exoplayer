package com.threedeye.reactvideo;

public interface RendererBuilder {

    void buildRender(RendererBuilderCallback callback);

    void cancel();
}
