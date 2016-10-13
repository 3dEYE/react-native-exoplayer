package com.multivideoreactmodule;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;


public class MultiPlayerActivity extends AppCompatActivity implements VideoPlayerCallbacks {

    private static final int MAX = 16;
    private int nextFragmentIndex = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_layout);
        addPlayer(nextFragmentIndex);

    }

    private void addPlayer(int number) {
        Uri uri = Uri.parse("file:///android_asset/sample.mp4");
        VideoPlayerFragment fragment = VideoPlayerFragment.newInstance(uri, number);
        getSupportFragmentManager().beginTransaction()
                .add(R.id.container, fragment).commit();
    }

    @Override
    public void onSuccessLoad() {
        nextFragmentIndex++;
        if (nextFragmentIndex <= MAX) {
            addPlayer(nextFragmentIndex);
        } else {
            sendResult();
        }
    }

    @Override
    public void onFailedLoad() {
        sendResult();
    }

    private void sendResult() {
        Intent result = new Intent();
        result.putExtra(RNVideoManager.EXTRA_MAX_SUPPORTED_VIDEO_DECODERS, nextFragmentIndex);
        setResult(Activity.RESULT_OK, result);
        finish();
    }


}



