package com.threedeye.reactvideo;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.AsyncTask;
import android.os.Build;
import android.widget.Toast;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import android.os.Debug;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RNExoPlayerModule extends ReactContextBaseJavaModule {

    private static final String CLASS_NAME = "RNEPManager";
    private static final int MAX_INSTANCES_V19 = 4;
    private static final int MAX_INSTANCES = 16;
    private static final String PROMISE_MAX_SUPPORTED = "maxSupported";
    private static final String PROMISE_HEAP_SIZE = "heapSize";
    public static boolean isRateSupported = android.os.Build.VERSION.SDK_INT
            >= Build.VERSION_CODES.M;
    private Promise mPromise;
    private int mMaxSupported;
    private int mHeapSize;

    public RNExoPlayerModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return CLASS_NAME;
    }

    @ReactMethod
    public static void isRateSupported(Promise promise) {
        promise.resolve(isRateSupported);
    }

    @ReactMethod
    public void getMaxSupportedVideoPlayersCount(String message, Promise promise) {
        if (promise != null) {
            mPromise = promise;
            Toast.makeText(getReactApplicationContext(), message, Toast.LENGTH_LONG).show();
            mHeapSize = getHeapSize();
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
                mMaxSupported = MAX_INSTANCES_V19;
                sendReault();
            }
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                MediaCodecInfo.CodecCapabilities codecCapabilities =
                        new MediaCodecInfo.CodecCapabilities();
                mMaxSupported = codecCapabilities.getMaxSupportedInstances();
                sendReault();
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                MaxSupportedTask task = new MaxSupportedTask();
                task.execute();
            } 
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private int getMaxSupportedInstancesV21() {
        int count = 0;
        MediaCodecList codecs = new MediaCodecList(MediaCodecList.REGULAR_CODECS);
        for (MediaCodecInfo info : codecs.getCodecInfos()) {
            if (info.isEncoder()) continue;
            if (isSupportedTypeVideoAvc(info.getSupportedTypes())) {
                String type = MediaFormat.MIMETYPE_VIDEO_AVC;
                MediaCodecInfo.CodecCapabilities caps = info.getCapabilitiesForType(type);
                count = getActualMax(
                        info.isEncoder(), info.getName(), type, caps, MAX_INSTANCES);
                break;
            }
        }
        return count;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static int getActualMax(
            boolean isEncoder, String name, String mime, MediaCodecInfo.CodecCapabilities caps,
            int max) {
        int flag = isEncoder ? MediaCodec.CONFIGURE_FLAG_ENCODE : 0;
        MediaFormat format = createMinFormat(mime, caps);
        List<MediaCodec> codecs = new ArrayList();
        int actualMax = 0;
        for (int i = 0; i < max; ++i) {
            try {
                MediaCodec codec = MediaCodec.createByCodecName(name);
                codec.configure(format, null, null, flag);
                codec.start();
                codecs.add(codec);
            } catch (IllegalArgumentException | IOException | MediaCodec.CodecException e) {
                e.printStackTrace();
                break;
            }
        }

        actualMax = codecs.size();
        for (MediaCodec codec : codecs) {
            if (codec != null) {
                codec.release();
                codec = null;
            }
        }
        codecs.clear();
        return actualMax;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static MediaFormat createMinFormat(String mime, MediaCodecInfo.CodecCapabilities caps) {
        MediaFormat format;
        if (caps.getVideoCapabilities() != null) {
            MediaCodecInfo.VideoCapabilities vcaps = caps.getVideoCapabilities();
            int minWidth = vcaps.getSupportedWidths().getLower();
            int minHeight = vcaps.getSupportedHeightsFor(minWidth).getLower();
            int minBitrate = vcaps.getBitrateRange().getLower();
            format = MediaFormat.createVideoFormat(mime, minWidth, minHeight);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, caps.colorFormats[0]);
            format.setInteger(MediaFormat.KEY_BIT_RATE, minBitrate);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, 10);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10);
        } else {
            MediaCodecInfo.AudioCapabilities acaps = caps.getAudioCapabilities();
            int minSampleRate = acaps.getSupportedSampleRateRanges()[0].getLower();
            int minChannelCount = 1;
            int minBitrate = acaps.getBitrateRange().getLower();
            format = MediaFormat.createAudioFormat(mime, minSampleRate, minChannelCount);
            format.setInteger(MediaFormat.KEY_BIT_RATE, minBitrate);
        }
        return format;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private boolean isSupportedTypeVideoAvc(String[] types) {
        boolean isSupported = false;
        if (types == null || types.length == 0) {
            return isSupported;
        }
        for (int j = 0; j < types.length; ++j) {
            if (types[j].equalsIgnoreCase(MediaFormat.MIMETYPE_VIDEO_AVC)) {
                isSupported = true;
                break;
            }
        }
        return isSupported;
    }

    public int getHeapSize() {
        return (int) ((int) Debug.getNativeHeapSize() / 1048576L);
    }

    class MaxSupportedTask extends AsyncTask<Void, Integer, Integer> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Integer doInBackground(Void... params) {
            return getMaxSupportedInstancesV21();
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            if (result == null) {
                mMaxSupported = 0;
            } else {
                mMaxSupported = result;
            }
            sendReault();
        }
    }

    private void sendReault() {
        WritableMap map = Arguments.createMap();
        map.putInt(PROMISE_MAX_SUPPORTED, mMaxSupported);
        map.putInt(PROMISE_HEAP_SIZE, mHeapSize);
        mPromise.resolve(map);
    }
}
