package life.centaurs.sunlife.video.render.display;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import life.centaurs.sunlife.R;

import static life.centaurs.sunlife.video.render.constants.DisplayConstants.*;
import static life.centaurs.sunlife.video.render.enums.CommandEnum.*;


public class CameraNavigationFragment extends Fragment implements ProgressBarManager.OnProgressListener {
    private boolean isRecording = false;
    private ProgressBarManager progressBarManager;
    private ProgressBar progressBarHorizontal;
    private FragmentsCommunicationListener fragmentsCommunicationListener;
    private Handler handler = new Handler();
    private Handler handlerMinVideo = new Handler();
    private boolean recordVideo = false;
    private ImageView imageViewRecordButton;
    private AnimationDrawable animationRecordButton;
    private long touchDownTime = 0;
    private long resultTime;

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            try {
                fragmentsCommunicationListener = (FragmentsCommunicationListener) activity;
            } catch (ClassCastException e) {
                throw new ClassCastException(activity.toString() + " must implements OnCameraButtonListener");
            }
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Activity activity = context instanceof Activity ? (Activity) context : null;
        try {
            fragmentsCommunicationListener = (FragmentsCommunicationListener) activity;
        } catch (ClassCastException e){
            throw new ClassCastException(context.toString() + " must implements OnCameraButtonListener");
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_camera_navigation, container, false);

        progressBarHorizontal = (ProgressBar) rootView.findViewById(R.id.progressBarHorizontal);
        progressBarManager = new ProgressBarManager(progressBarHorizontal, VIDEO_PROGRESS_TIME, this);

        imageViewRecordButton = (ImageView) rootView.findViewById(R.id.imageViewRecordButton);
        imageViewRecordButton.setOnTouchListener(onRecordButtonTouchListener);
        imageViewRecordButton.setBackgroundResource(R.mipmap.record_btn_0);
        progressBarManager.execute();
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        isRecording = false;
        startRecordingVisualisation(isRecording);
        super.onPause();
    }

    @Override
    public void startActivity(Intent intent) {
        super.startActivity(intent);
    }

    private final View.OnTouchListener onRecordButtonTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            recordVideo = true;
            if (!CameraActivity.isTimeIsOff()) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        touchDownTime = System.currentTimeMillis();
                        startVideoCountDown();
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        resultTime = System.currentTimeMillis() - touchDownTime;
                        recordVideo = false;
                        if (resultTime >= TIME_PHOTO_BUTTON_ACTIVE_IN_MILLIS) {
                            if (resultTime >= MIN_VIDEO_TIME_IN_MILLIS){
                                isRecording = false;
                                recordButtonClick(R.drawable.record_off_button);
                                startRecordingVisualisation(isRecording);
                                fragmentsCommunicationListener.onClickButton(STOP_RECORDING);
                            } else {
                                minVideoCountDown();
                            }

                        } else {
                            recordButtonClick(R.drawable.anim_photo);
                            progressBarManager.setProgressStatus(progressBarManager.getProgressStatus() + PHOTO_PROGRESS_STATUS);
                            fragmentsCommunicationListener.onClickButton(TAKE_PICTURE);
                        }
                        break;
                }
            }
            return true;
        }
    };

    private void startVideoCountDown(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(TIME_PHOTO_BUTTON_ACTIVE_IN_MILLIS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (recordVideo) {
                            isRecording = true;
                            recordButtonClick(R.drawable.record_on_button);
                            startRecordingVisualisation(isRecording);
                            fragmentsCommunicationListener.onClickButton(START_RECORDING);
                        }
                    }
                });
            }
        }).start();
    }

    private void minVideoCountDown(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(MIN_VIDEO_TIME_IN_MILLIS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        isRecording = false;
                        recordButtonClick(R.drawable.record_off_button);
                        startRecordingVisualisation(isRecording);
                        fragmentsCommunicationListener.onClickButton(STOP_RECORDING);
                    }
                });
            }
        }).start();
    }

    private void recordButtonClick(int resourceBackground){
        if (!CameraActivity.isTimeIsOff()) {
            imageViewRecordButton.setBackgroundResource(resourceBackground);
            animationRecordButton = (AnimationDrawable) imageViewRecordButton.getBackground();
            animationRecordButton.setOneShot(true);
            animationRecordButton.stop();
            animationRecordButton.start();
        }
    }

    protected void startRecordingVisualisation(boolean isRecording) {
        if (isRecording) {
            progressBarManager.progress();
        } else {
            progressBarManager.pauseProgress();
        }
    }

    @Override
    public void onEndProgress() {
        isRecording = false;
        imageViewRecordButton.setBackgroundResource(R.mipmap.record_btn_0);
        startRecordingVisualisation(isRecording);
        fragmentsCommunicationListener.onClickButton(STOP_RECORDING);
        CameraActivity.setTimeIsOff(true);
    }
}
