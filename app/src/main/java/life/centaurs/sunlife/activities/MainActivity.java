package life.centaurs.sunlife.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import life.centaurs.sunlife.R;
import life.centaurs.sunlife.squad.ActivitySquad;
import life.centaurs.sunlife.video.render.display.CameraActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View viewBackground = this.getWindow().getDecorView();
        viewBackground.setBackgroundResource(R.color.colorBackground);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        ActivitySquad.clnScrActivity(this, hasFocus);
    }

    public void video(View view){
        ActivitySquad.goFromCurrentActivityToNewActivity(this, CameraActivity.class);
    }
}
