package com.yourcompany.videoplayer;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ksyun.media.player.IMediaPlayer;
import com.ksyun.media.player.KSYMediaPlayer;
import com.ksyun.media.player.KSYTextureView;

import java.io.IOException;

import io.flutter.plugin.common.PluginRegistry;

/**
 * Created by zqc on 2018/6/1.
 */

public class KsyPlayerView extends LinearLayout implements Handler.Callback {
    private static final int UPDATE_SEEK_BAR_STATUS = 101;
    private Boolean mTouching;
    public static final String TAG = "KsyPlayerView";
    private SeekBar mPlayerSeekBar;
    private SeekBar seekBar_landscape;
    private TextView current_time_landscape, total_time_landscape, video_name_landscape;
    private float lastMoveX = -1;
    private float lastMoveY = -1;
    private float startX, startY;
    private int leftPosition, rightPosition;
    private Boolean isPanelShowing_Portrait = true;
    private Boolean isPanelShowing_Landscape = true;
    private Handler mHandler;
    private int mVideoProgress = 0;
    boolean isFull = false;
    LayoutParams layoutParams;
    KSYTextureView mVideoView;
    RelativeLayout video;
    PluginRegistry.Registrar registrar;
    private ImageView back, pause, full_screen;
    private ImageView back_landscape, more_landscape, pause_landscape, next_landscape;
    private RelativeLayout landscape_content, portrait_content;
    private LinearLayout ll_content;
    private SharedPreferences.Editor editor;
    private RelativeLayout vod_content;
    //加载进度条
    private ImageView loadingProgress;
    private ImageView vod_display_back_landscape;
    public ImageView vod_display_back_portrait;
    private Boolean isPause = false;
    private TextView currentTime, totalTime;
    private RelativeLayout panel, content;

    String url;

    public KsyPlayerView(Context context, final PluginRegistry.Registrar registrar, LayoutParams layoutParams, String Url) {
        super(context);
        url = Url;
        LayoutInflater.from(context).inflate(R.layout.layout, this);
        video = (RelativeLayout) findViewById(R.id.vod_main_video);
        mVideoView = new KSYTextureView(context);
        RelativeLayout.LayoutParams reParams = (RelativeLayout.LayoutParams) video.getLayoutParams();
        reParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        mVideoView.setLayoutParams(reParams);
        mVideoView.setBufferTimeMax(2.0f);
        mVideoView.setTimeout(5, 30);
        this.registrar = registrar;
        this.layoutParams = layoutParams;
        mHandler = new Handler(registrar.context().getMainLooper(), this);
        initView();
        mVideoView.setOnPreparedListener(mOnPreparedListener);
        mVideoView.setOnTouchListener(mTouchListener);
        mVideoView.setOnErrorListener(mOnErrorListener);
        mVideoView.setBufferSize(1);
        mVideoView.setTimeout(50, 300);
        setFullScreen();
        try {
            //Url
            mVideoView.setDataSource(url);

        } catch (IOException e) {
            e.printStackTrace();
        }
        mVideoView.prepareAsync();
        Dialog.show();

    }

    //设置竖屏
//    void setPorirait() {
//        registrar.activity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//        landscape_content.setVisibility(View.GONE);
//        portrait_content.setVisibility(View.VISIBLE);
//        mVideoView.setVideoScalingMode(KSYMediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT);
//        vod_content.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
//        Toast.makeText(registrar.context(), "返回键Back键测试", Toast.LENGTH_SHORT).show();
//        isFull = false;
//    }

    void setFullScreen() {

        registrar.activity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        portrait_content.setVisibility(View.GONE);
        landscape_content.setVisibility(View.VISIBLE);
        mVideoView.setVideoScalingMode(KSYMediaPlayer.VIDEO_SCALING_MODE_NOSCALE_TO_FIT);
        vod_content.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        isFull = true;
    }

    IMediaPlayer.OnErrorListener mOnErrorListener = new IMediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(IMediaPlayer iMediaPlayer, int what, int extra) {
            switch (what) {
                case KSYMediaPlayer.MEDIA_ERROR_INVALID_DATA:

                    mVideoView.reload(url, false);
                    break;

                case KSYMediaPlayer.MEDIA_ERROR_UNKNOWN:
                    mVideoView.reload(url, false);


                    break;
            }
            return false;
        }
    };

    IMediaPlayer.OnPreparedListener mOnPreparedListener = new IMediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(IMediaPlayer iMediaPlayer) {
            if (mVideoView != null) {
                // 设置视频伸缩模式，此模式为裁剪模式
                mVideoView.setVideoScalingMode(KSYMediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT);
//                 开始播放视频
                setVideoProgress(0);
                mVideoView.start();
                Dialog.dismiss();
                video.addView(mVideoView);

            }
        }
    };

    private void initView() {
        mPlayerSeekBar = (SeekBar) findViewById(R.id.vod_display_seekbar);
        mPlayerSeekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);
        mPlayerSeekBar.setEnabled(true);
        current_time_landscape = (TextView) findViewById(R.id.current_time_landscape);
        totalTime = (TextView) findViewById(R.id.vod_display_total_time);
        seekBar_landscape = (SeekBar) findViewById(R.id.seekbar_landscape);
        seekBar_landscape.setOnSeekBarChangeListener(mSeekBarChangeListener);
        currentTime = (TextView) findViewById(R.id.vod_display_current_time);
        vod_display_back_landscape = findViewById(R.id.vod_display_back_landscape);
        total_time_landscape = (TextView) findViewById(R.id.total_time_landscape);
        vod_display_back_landscape.setOnClickListener(onclickListener);
        vod_display_back_portrait = findViewById(R.id.vod_display_back_portrait);
        full_screen = (ImageView) findViewById(R.id.vod_display_full_screen);
        pause = (ImageView) findViewById(R.id.vod_display_pause);
        full_screen.setOnClickListener(onclickListener);
        pause.setOnClickListener(onclickListener);
        pause_landscape = (ImageView) findViewById(R.id.pause_landscape);
        pause_landscape.setOnClickListener(onclickListener);
        portrait_content = findViewById(R.id.portrait_controller);
        landscape_content = findViewById(R.id.landscape_controller);
        ll_content = findViewById(R.id.ll_content);
        vod_content = findViewById(R.id.vod_content);
        panel = (RelativeLayout) findViewById(R.id.vod_controller_bar);
        panel.setOnClickListener(onclickListener);


        //进度条
        loadingProgress = (ImageView) findViewById(R.id.iv_loading);
        Dialog.init(loadingProgress);

    }

    OnClickListener onclickListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            int id = view.getId();
            if (id == R.id.vod_display_back_landscape) {
//                setPorirait();
//
            } else if (id == R.id.vod_display_pause || id == R.id.pause_landscape) {
                if (isPause) {
                    pause.setImageResource(R.mipmap.stop_full_screen);
                    pause_landscape.setImageResource(R.mipmap.stop_full_screen);
                    mVideoView.start();
//                    editor.putBoolean("isPlaying", true);
                } else {
                    pause.setImageResource(R.mipmap.start);
                    pause_landscape.setImageResource(R.mipmap.start);
                    mVideoView.pause();
//                    editor.putBoolean("isPlaying", false);
                    if (loadingProgress.getVisibility() == View.VISIBLE) {
                        Dialog.dismiss();
                    }
                }
//                editor.commit();
                isPause = !isPause;
            } else if (id == R.id.vod_display_full_screen) {
                registrar.activity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                portrait_content.setVisibility(View.GONE);
                landscape_content.setVisibility(View.VISIBLE);
                mVideoView.setVideoScalingMode(KSYMediaPlayer.VIDEO_SCALING_MODE_NOSCALE_TO_FIT);
                vod_content.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
                isFull = true;
            } else if (id == R.id.next_landscape) {
//                playNextVideo();
            } else if (id == R.id.more_landscape) {
//                hideLandscapePanel();
//                more_content.setVisibility(View.VISIBLE);
            } else if (id == R.id.clarity_landscape) {
//                hideLandscapePanel();
//                clarity_content.setVisibility(View.VISIBLE);
            } else if (id == R.id.screen_shot) {
//                saveBitmap();
            } else if (id == R.id.screen_cap) {
//                if (useHwDecoder) {
//                    Toast.makeText(this, "录制视频请切换至软解", Toast.LENGTH_LONG).show();
//                } else if(!mSettings.getString("clarity", Setting.CLARITY_HIGH).equals(Setting.CLARITY_NORMAL)){
//                    Toast.makeText(this, "当前视频清晰度过高，请切换至标清", Toast.LENGTH_LONG).show();
//                }
//                else{
//                    hideLandscapePanel();
//                    showStatusBar();
//                    screen_cap_content.setVisibility(View.VISIBLE);
//                    saveVideo();
//                    screen_cap_content.setOnClickListener(new View.OnClickListener() {
//                        @Override
//                        public void onClick(View view) {
//
//                        }
//                    });
//                }
            } else if (id == R.id.vod_controller_bar || id == R.id.landscape_bottom_panel || id == R.id.landscape_top_panel) {
                return;
            } else if (id == R.id.cap_pause) {
//                cap_stop = true;
            } else if (id == R.id.delete_cap) {
//                cap_stop = true;
//                landscape_content.setVisibility(View.VISIBLE);
//                deleteVideo();
//                screen_cap_content.setVisibility(View.GONE);
            } else if (id == R.id.save_cap) {
//                save_video.setVisibility(View.VISIBLE);
//                File file = new File(videoPath);
//                Uri uri = Uri.fromFile(file);
//                registrar.context().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
//                mHandler.postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        save_video.setVisibility(View.GONE);
//                        screen_cap_content.setVisibility(View.GONE);
//                        landscape_content.setVisibility(View.VISIBLE);
//                    }
//                }, 2000);
            }
        }
    };

    public int setVideoProgress(int currentProgress) {

        if (mVideoView == null)
            return -1;

        long time = currentProgress > 0 ? currentProgress : mVideoView.getCurrentPosition();
        long length = mVideoView.getDuration();

        // Update all view elements
        if (portrait_content.getVisibility() == View.VISIBLE) {
            mPlayerSeekBar.setMax((int) length);
            mPlayerSeekBar.setProgress((int) time);
        } else {
            seekBar_landscape.setMax((int) length);
            seekBar_landscape.setProgress((int) time);
        }

        if (time >= 0) {
            if (portrait_content.getVisibility() == View.VISIBLE) {
                currentTime.setText(Strings.millisToString(time));
                totalTime.setText(Strings.millisToString(length));
            } else {
                current_time_landscape.setText(Strings.millisToString(time));
                total_time_landscape.setText(Strings.millisToString(length));
            }
        }

        Message msg = new Message();
        msg.what = UPDATE_SEEK_BAR_STATUS;

        if (mHandler != null)
            mHandler.sendMessageDelayed(msg, 1000);
        return (int) time;
    }


    private SeekBar.OnSeekBarChangeListener mSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if (portrait_content.getVisibility() == View.VISIBLE) {
                mVideoProgress = seekBar.getProgress();
            } else {
                mVideoProgress = seekBar_landscape.getProgress();
            }
            if (mVideoView != null)
                mVideoView.seekTo(mVideoProgress);
        }
    };


    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case UPDATE_SEEK_BAR_STATUS:
                setVideoProgress(0);
                break;
//            case UPDATE_QOSMESS:
//                if (message.obj instanceof QosBean) {
//                    updateQosInfo((QosBean) message.obj);
//                }
//                break;
//            case UPADTE_QOSVIEW:
//                updateQosView();
//                break;
//            case REMOVE_TIPS:
//                cap_text.setVisibility(View.GONE);
//                break;
//            case CAP_FINISHED:
//                cap_save.setVisibility(View.VISIBLE);
//                break;
        }
        return false;
    }

    private OnTouchListener mTouchListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            KSYTextureView mVideoView = FloatingPlayer.getInstance().getKSYTextureView();
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    mTouching = false;
//                    startX = event.getX();
//                    startY = event.getY();
//                    leftPosition = full_screen_width / 3;
//                    rightPosition = leftPosition * 2;
//                    isControling = (startX < leftPosition || startX > rightPosition) && (full_screen_width > video_width);
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    mTouching = true;
//                    if (event.getPointerCount() == 2) {
//                        lastSpan = getCurrentSpan(event);
//                        centerPointX = getFocusX(event);
//                        centerPointY = getFocusY(event);
//                    }
                    break;
                case MotionEvent.ACTION_MOVE:
//                    if (event.getPointerCount() == 1) {
//                        float posX = event.getX();
//                        float posY = event.getY();
//                        if (lastMoveX == -1 && lastMoveX == -1) {
//                            lastMoveX = posX;
//                            lastMoveY = posY;
//                        }
//                        movedDeltaX = posX - lastMoveX;
//                        movedDeltaY = posY - lastMoveY;
//
//                        if (Math.abs(movedDeltaX) > 5 || Math.abs(movedDeltaY) > 5) {
//                            //判断调节音量和亮度 还是缩放画面
//                            if (isControling && (Math.abs(movedDeltaY) > 2 * Math.abs(movedDeltaX))) {
//
//                                if (startX < leftPosition) {
//                                    //调节亮度
//                                    float position = Display.changeLight(VodDisplayActivity.this, full_screen_height, (startY - posY), getWindow().getAttributes());
//                                    displaySeekbar_brightness.setMax(16);
//                                    displaySeekbar_brightness.setProgress((int) (position * 16));
//                                    displayDialog_brightness.setVisibility(View.VISIBLE);
//                                } else if (startX > rightPosition) {
//                                    //调节音量
//                                    currentVol = startVol + (startY - posY) / (full_screen_height / 8);
//                                    if (currentVol <= 0) {
//                                        currentVol = 0;
//                                        volumn_text.setText("静音");
//                                        volumn_image.setImageResource(R.mipmap.novolumn);
//                                    } else {
//                                        volumn_text.setText("音量");
//                                        volumn_image.setImageResource(R.mipmap.volumn1);
//                                    }
//                                    if (currentVol >= 2.0f) {
//                                        currentVol = 2.0f;
//                                    }
//                                    displaySeekbar_volumn.setMax(16);
//                                    displaySeekbar_volumn.setProgress((int) (currentVol * 8));
//                                    displayDialog_volumn.setVisibility(View.VISIBLE);
//                                    mVideoView.setVolume(currentVol, currentVol);
//                                }
//                            } else {
//                                if (mVideoView != null) {
//                                    mVideoView.moveVideo(movedDeltaX, movedDeltaY);
//                                }
//                            }
                    mTouching = true;
//                        }
//                        lastMoveX = posX;
//                        lastMoveY = posY;
//
//                    } else if (event.getPointerCount() == 2) {
//                        double spans = getCurrentSpan(event);
//                        if (spans > 5) {
//                            deltaRatio = (float) (spans / lastSpan);
//                            totalRatio = mVideoView.getVideoScaleRatio() * deltaRatio;
//                            if (mVideoView != null) {
//                                mVideoView.setVideoScaleRatio(totalRatio, centerPointX, centerPointY);
//                            }
//                            lastSpan = spans;
//                        }
//                    }
//                    break;
//                case MotionEvent.ACTION_POINTER_UP:
//                    if (event.getPointerCount() == 2) {
//                        lastMoveX = -1;
//                        lastMoveY = -1;
//                    }
                    break;
                case MotionEvent.ACTION_UP:
                    lastMoveX = -1;
                    lastMoveY = -1;
//                    Display.getCurrentLight();
//                    if (displayDialog_brightness.getVisibility() == View.VISIBLE) {
//                        displayDialog_brightness.setVisibility(View.GONE);
//                    }
//                    if (displayDialog_volumn.getVisibility() == View.VISIBLE) {
//                        displayDialog_volumn.setVisibility(View.GONE);
//                    }
//                    startVol = currentVol;
                    if (!mTouching) {
                        dealTouchEvent();
                    }
                    break;
                default:
                    break;
            }
            return true;
        }
    };

    private void dealTouchEvent() {
        if (portrait_content.getVisibility() == View.VISIBLE) {
            if (isPanelShowing_Portrait) {
                panel.setVisibility(View.GONE);
            } else {
                panel.setVisibility(View.VISIBLE);
            }
        } else {
//            hideShade();
//            showLandscapePanel();
            if (isPanelShowing_Landscape) {
                landscape_content.setVisibility(View.GONE);
//                hideStatusBar();
            } else {
                landscape_content.setVisibility(View.VISIBLE);
//                showStatusBar();
            }
        }
        isPanelShowing_Landscape = !isPanelShowing_Landscape;
        isPanelShowing_Portrait = !isPanelShowing_Portrait;
    }

    public void stop() {
        mVideoView.release();
    }
}
