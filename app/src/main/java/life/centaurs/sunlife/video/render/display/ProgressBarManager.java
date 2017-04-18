package life.centaurs.sunlife.video.render.display;


import android.os.Handler;
import android.widget.ProgressBar;

import static life.centaurs.sunlife.video.render.constants.DisplayConstants.PROGRESS_VIDEO_TIME_KOEF;

public final class ProgressBarManager{
    private ProgressBar progressBar;
    private int progressStatus = 0;
    private volatile boolean resumed = false;
    private Handler handler = new Handler();
    private int timeVideoProgress;
    private OnProgressListener onProgressListener;
    private final static int PROGRESS_STATUS_MAX = 100;
    private static int lastChunkProgressStatus = 0;
    private static int lastChunkDuration = 0;//in seconds

    public interface OnProgressListener{
        public void onEndProgress();
    }

    ProgressBarManager(ProgressBar progressBar, int timeVideoProgress, OnProgressListener onProgressListener){
        this.progressBar = progressBar;
        this.timeVideoProgress = timeVideoProgress;
        this.onProgressListener = onProgressListener;
    }

    public static int getLastChunkDuration() {
        return lastChunkDuration;
    }

    public int getProgressStatus() {
        return progressStatus;
    }

    public static int getProgressStatusMax() {
        return PROGRESS_STATUS_MAX;
    }

    public void setProgressStatus(int progressStatus) {
        if (progressStatus > 100){
            this.progressStatus = 100;
        } else {
            this.progressStatus = progressStatus;
        }
        progressBar.setProgress(this.progressStatus);
    }

    public void pauseProgress(){
        resumed = false;
        lastChunkDuration = ((timeVideoProgress / PROGRESS_VIDEO_TIME_KOEF) *(progressStatus - lastChunkProgressStatus)) / PROGRESS_STATUS_MAX;
    }

    public void startProgress(){
        lastChunkProgressStatus = progressStatus;
        resumed = true;
    }

    public void nullProgressBarStatus(){
        progressStatus = 0;
        lastChunkProgressStatus = 0;
        lastChunkDuration = 0;
    }

    public void startProgressBar(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (progressStatus < PROGRESS_STATUS_MAX) {
                    if (resumed) {
                        progressStatus += 1;
                    }
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setProgress(progressStatus);
                        }
                    });
                    try {
                        Thread.sleep(timeVideoProgress);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        onProgressListener.onEndProgress();
                    }
                });
            }
        }).start();
    }
}
