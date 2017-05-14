package life.centaurs.sunlife.video.render.display;


import android.os.Handler;
import android.widget.ProgressBar;

public final class ProgressBarManager{
    private ProgressBar progressBar;
    private int progressStatus = 0;
    private volatile boolean resumed = false;
    private Handler handler = new Handler();
    private int timeVideoProgress;
    private OnProgressListener onProgressListener;
    private final static int PROGRESS_STATUS_MAX = 100;
    private static int lastChunkProgressStatus = 0;
    private static int lastChunkDurationProgress = 0;//in seconds
    private static boolean timeIsOff = false;

    public interface OnProgressListener{
        public void onEndProgress();
    }

    ProgressBarManager(ProgressBar progressBar, int timeVideoProgress, OnProgressListener onProgressListener){
        this.progressBar = progressBar;
        this.timeVideoProgress = timeVideoProgress;
        this.onProgressListener = onProgressListener;
    }

    public static int getLastChunkDurationProgress() {
        return lastChunkDurationProgress;
    }

    public int getProgressStatus() {
        return progressStatus;
    }

    public static int getProgressStatusMax() {
        return PROGRESS_STATUS_MAX;
    }

    public static boolean isTimeIsOff() {
        return timeIsOff;
    }

    public static void setTimeIsOff(boolean currentTimeIsOff) {
        timeIsOff = currentTimeIsOff;
    }

    public void setProgressStatus(int progressStatus) {
        if (progressStatus > 100){
            this.progressStatus = 100;
        } else {
            this.progressStatus = progressStatus;
        }
        lastChunkDurationProgress = this.progressStatus - lastChunkProgressStatus;
        lastChunkProgressStatus = this.progressStatus;
        progressBar.setProgress(this.progressStatus);
    }

    public void increaseStatusProgressStatuc(int progress){
        int tempProgress = progress + this.progressStatus;
        if (tempProgress > 100){
            this.progressStatus = 100;
        } else {
            this.progressStatus = tempProgress;
        }
        lastChunkDurationProgress = this.progressStatus - lastChunkProgressStatus;
        lastChunkProgressStatus = this.progressStatus;
        progressBar.setProgress(this.progressStatus);
    }

    public void decreaseProgressStatus(int progress){
        int tempProgress = this.progressStatus - progress;
        if (tempProgress < 0){
            this.progressStatus = 0;
        } else {
            this.progressStatus = tempProgress;
        }
        progressBar.setProgress(this.progressStatus);
    }

    public void pauseProgress(){
        resumed = false;
        lastChunkDurationProgress = progressStatus - lastChunkProgressStatus;
        lastChunkProgressStatus = progressStatus;
    }

    public void startProgress(){
        lastChunkProgressStatus = progressStatus;
        resumed = true;
    }

    public void nullProgressBarStatus(){
        progressStatus = 0;
        lastChunkProgressStatus = 0;
        lastChunkDurationProgress = 0;
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
