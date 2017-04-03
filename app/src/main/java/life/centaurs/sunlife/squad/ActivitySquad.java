package life.centaurs.sunlife.squad;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageSwitcher;

import java.util.Timer;
import java.util.TimerTask;

/**
 * ActivitySquad
 * This class describes activity commands, such as - change activity, show something, etc..
 * , all methods are static, you don't need to create a new object;
 */
public class ActivitySquad {

    /**
     * changes activity from current to given
     * @param currentActivity
     * @param newActivityClass
     */
    public static void goFromCurrentActivityToNewActivity(AppCompatActivity currentActivity
            , Class<?> newActivityClass){
        Intent homeIntent = new Intent(currentActivity, newActivityClass);
        currentActivity.startActivity(homeIntent);
        currentActivity.finish();
    }

    /**
     * changes activity from current to given
     * , after given time is out
     * @param currentActivity
     * @param newActivityClass
     * @param activityTimeOut
     */
    public static void goFromCurrentActivityToNewActivity(final AppCompatActivity currentActivity
            , final Class<?> newActivityClass, long activityTimeOut){
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                ActivitySquad.goFromCurrentActivityToNewActivity(currentActivity, newActivityClass);
            }
        }, activityTimeOut);
    }

    /**
     * changes image, when duration time is out
     * @param currentActivity
     * @param imageSwitcher
     * @param timeToWait
     */
    public static void changeImageTimerStart(final AppCompatActivity currentActivity
            , final ImageSwitcher imageSwitcher, long timeToWait) {
        Timer timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                currentActivity.runOnUiThread(new Runnable() {
                    public void run() {
                        imageSwitcher.showNext();
                    }
                });
            }
        };
        timer.schedule(timerTask, timeToWait);
    }

    public static void clnScrActivity(Activity activity, boolean hasFocus) {
        View decorView = activity.getWindow().getDecorView();
        if(hasFocus){
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    }
}
