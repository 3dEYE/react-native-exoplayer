package com.threedeye.reactvideo;

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

import java.util.ArrayList;
import java.util.List;

public class RNExoPlayerModule extends ReactContextBaseJavaModule {

    private static final String CLASS_NAME = "RNEPManager";
    private static final String PROMISE_MAX_SUPPORTED = "maxSupported";
    private static final String PROMISE_HEAP_SIZE = "heapSize";
    private static final long MB_SIZE = 1048576L;
    private static final int MAX_INSTANCES_V19 = 4;
    private static final int MAX_INSTANCES = 16;

    public static boolean isRateSupported = android.os.Build.VERSION.SDK_INT
            >= Build.VERSION_CODES.M;
    private Promise mPromise;
    private int mMaxSupported = 1;
    private int mHeapSize;
    private int mVersionSdk = android.os.Build.VERSION.SDK_INT;


    public RNExoPlayerModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return CLASS_NAME;
    }

    @ReactMethod
    public void isRateSupported(Promise promise) {
        promise.resolve(isRateSupported);
    }

    @ReactMethod
    public void getMaxSupportedVideoPlayersCount(String message, Promise promise) {
        if (promise != null) {
            mPromise = promise;
            Toast.makeText(getReactApplicationContext(), message, Toast.LENGTH_LONG).show();
            mHeapSize = getHeapSize();
            if (mVersionSdk < Build.VERSION_CODES.LOLLIPOP) {
                mMaxSupported = MAX_INSTANCES_V19;
                sendResult();
            }
            if (mVersionSdk >= Build.VERSION_CODES.LOLLIPOP) {
                MaxSupportedTask task = new MaxSupportedTask();
                task.execute();
            }
        }
    }


    private int getMaxSupportedInstancesV21() {
        int count = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            MediaCodecList codecs = new MediaCodecList(MediaCodecList.REGULAR_CODECS);
            for (MediaCodecInfo info : codecs.getCodecInfos()) {
                if (info.isEncoder()) continue;
                if (isSupportedTypeVideoAvc(info.getSupportedTypes())) {
                    String type = MediaFormat.MIMETYPE_VIDEO_AVC;
                    MediaCodecInfo.CodecCapabilities caps = info.getCapabilitiesForType(type);
                    if (mVersionSdk >= android.os.Build.VERSION_CODES.M) {
                        count = caps.getMaxSupportedInstances();
                    } else {
                        count = getActualMax(
                                info.isEncoder(), info.getName(), type, caps, MAX_INSTANCES);
                    }
                    break;
                }
            }
        }
        return count;
    }


    private int getActualMax(boolean isEncoder, String name, String mime,
                                    MediaCodecInfo.CodecCapabilities caps, int max) {
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
            } catch (Exception e) {
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

    private MediaFormat createMinFormat(String mime, MediaCodecInfo.CodecCapabilities caps) {
        MediaFormat format = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
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
        }
        return format;
    }


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
        return (int) ((int) Runtime.getRuntime().maxMemory() / MB_SIZE);
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
            if (result != null) {
                mMaxSupported = result;
            }
            sendResult();
        }
    }

    private void sendResult() {
        WritableMap map = Arguments.createMap();
        map.putInt(PROMISE_MAX_SUPPORTED, mMaxSupported);
        map.putInt(PROMISE_HEAP_SIZE, mHeapSize);
        mPromise.resolve(map);
    }
}