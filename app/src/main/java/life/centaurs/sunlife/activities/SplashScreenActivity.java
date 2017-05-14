package life.centaurs.sunlife.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageSwitcher;
import android.widget.TextView;

import life.centaurs.sunlife.R;
import life.centaurs.sunlife.animation.AnimationSunLife;
import life.centaurs.sunlife.squad.ActivitySquad;
import life.centaurs.sunlife.video.render.display.CameraActivity;

import static life.centaurs.sunlife.constants.ActivitiesConstants.CHANGE_IMAGE_TIME;
import static life.centaurs.sunlife.constants.ActivitiesConstants.SPLASH_SCREEN_BACKGROUND_COLOR;
import static life.centaurs.sunlife.constants.ActivitiesConstants.SPLASH_SCREEN_TIME_OUT;
import static life.centaurs.sunlife.constants.ActivitiesConstants.VERSION_MESSAGE;
import static life.centaurs.sunlife.constants.ActivitiesConstants.VERSION_MESSAGE_COLOR;
import static life.centaurs.sunlife.constants.ActivitiesConstants.VERSION_MESSAGE_TEXT_SCALE_X;
import static life.centaurs.sunlife.constants.ActivitiesConstants.VERSION_MESSAGE_TEXT_SIZE;

/**
 * SplashScreenActivity
 * This class describes splash screen activity
 */
public class SplashScreenActivity extends AppCompatActivity {
    private ImageSwitcher imageSwitcher;
    private TextView textViewVersion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        View viewBackground = this.getWindow().getDecorView();
        viewBackground.setBackgroundColor(SPLASH_SCREEN_BACKGROUND_COLOR);
        textViewVersion = (TextView) findViewById(R.id.textViewVersion);
        textViewVersion.setTextColor(VERSION_MESSAGE_COLOR);
        textViewVersion.setTextScaleX(VERSION_MESSAGE_TEXT_SCALE_X);
        textViewVersion.setTextSize(VERSION_MESSAGE_TEXT_SIZE);
        textViewVersion.setText(VERSION_MESSAGE);

        imageSwitcher = (ImageSwitcher)findViewById(R.id.imageSwitcher);

        AnimationSunLife.imageEmergenceAlphaAnimation(imageSwitcher);

        ActivitySquad.changeImageTimerStart(SplashScreenActivity.this, imageSwitcher, CHANGE_IMAGE_TIME);

        ActivitySquad.goFromCurrentActivityToNewActivity(SplashScreenActivity.this
                , CameraActivity.class, SPLASH_SCREEN_TIME_OUT);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        ActivitySquad.clnScrActivity(this, hasFocus);
    }
}
