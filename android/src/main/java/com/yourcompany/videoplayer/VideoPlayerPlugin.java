package com.yourcompany.videoplayer;

import android.content.pm.ActivityInfo;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/**
 * VideoPlayerPlugin
 */
public class VideoPlayerPlugin implements MethodCallHandler {

    private Registrar registrar;
    LinearLayout linearLayout;
    private KsyPlayerView ksyPlayerView;
    boolean haveVideo;
    private LinearLayout.LayoutParams layoutParams;

    public VideoPlayerPlugin(Registrar registrar) {
        this.registrar = registrar;
    }

    /**
     * Plugin registration.
     */
    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "flutter.io/videoPlayer");
        channel.setMethodCallHandler(new VideoPlayerPlugin(registrar));
    }

    @Override
    public void onMethodCall(MethodCall methodCall, Result result) {
        if (methodCall.method.equals("Init")) {
            if(!haveVideo){
                layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
                linearLayout = new LinearLayout(registrar.context());
                String url = methodCall.argument("url").toString();
                Log.d("onMethodCall: ", url);
                ksyPlayerView = new KsyPlayerView(registrar.context(), registrar, layoutParams, url);
                linearLayout.addView(ksyPlayerView);
                ksyPlayerView.vod_display_back_portrait.setOnClickListener(listener);
                registrar.activity().addContentView(linearLayout, layoutParams);
                registrar.activity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                haveVideo = true;
            }


        } else if (methodCall.method.equals("setPort")) {
            if (ksyPlayerView != null) {
                if (haveVideo) {
                    destroyVideo();
                    result.success(false);

                } else {
                    result.success(true);
                }
            }else {
                result.success(true);
            }
//            if (ksyPlayerView!=null){
//                if (ksyPlayerView.isFull){
////                    ksyPlayerView.setPorirait();
//                    result.success(false);
//                }else {
//                    if (haveVideo){
//                        destroyVideo();
//                        result.success(false);
//                    }else {
//                        result.success(true);
//                    }
//                }
//            }else {
//                result.success(true);
//
//            }
        } else if (methodCall.method.equals("androidDispose")) {
            destroyVideo();


        } else {
            result.notImplemented();
        }
    }

    private void destroyVideo() {
        ksyPlayerView.stop();
        linearLayout.removeAllViews();
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(0, 0);
        linearLayout.setLayoutParams(layoutParams);
        registrar.activity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        haveVideo = false;
    }

    View.OnClickListener listener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            destroyVideo();

        }
    };


}