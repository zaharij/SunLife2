package life.centaurs.sunlife.video.render.display;


import android.os.AsyncTask;
import android.os.Handler;
import android.widget.ProgressBar;

public final class ProgressBarManager extends AsyncTask{
    private ProgressBar progressBar;
    private int progressStatus = 0;
    private volatile boolean resumed = false;
    private Handler handler = new Handler();
    private int timeVideoProgress;
    private OnProgressListener onProgressListener;
    private final static int PROGRESS_STATUS_MAX = 100;

    public interface OnProgressListener{
        public void onEndProgress();
    }

    ProgressBarManager(ProgressBar progressBar, int timeVideoProgress, OnProgressListener onProgressListener){
        this.progressBar = progressBar;
        this.timeVideoProgress = timeVideoProgress;
        this.onProgressListener = onProgressListener;
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
    }

    public void progress(){
        resumed = true;
    }

    @Override
    protected Object doInBackground(Object[] objects) {
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
        return null;
    }

    @Override
    protected void onPostExecute(Object o) {
        onProgressListener.onEndProgress();
        super.onPostExecute(o);
    }
}
