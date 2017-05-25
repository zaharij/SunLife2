package life.centaurs.sunlife.activities;

import android.Manifest;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import life.centaurs.sunlife.R;
import life.centaurs.sunlife.squad.ActivitySquad;
import life.centaurs.sunlife.video.render.display.CameraFragment;
import life.centaurs.sunlife.video.render.display.CameraNavigationFragment;
import life.centaurs.sunlife.video.render.display.ChoseSound;
import life.centaurs.sunlife.video.render.display.FragmentsCommunicationListener;
import life.centaurs.sunlife.video.render.enums.CommandEnum;
import life.centaurs.sunlife.video.render.enums.DeviceCamerasEnum;
import life.centaurs.sunlife.video.render.enums.MediaExtensionEnum;
import life.centaurs.sunlife.video.render.enums.OrientationEnum;

import static life.centaurs.sunlife.constants.ActivitiesConstants.SPLASH_SCREEN_BACKGROUND_COLOR;
import static life.centaurs.sunlife.video.render.display.ProgressBarManager.isTimeIsOff;
import static life.centaurs.sunlife.video.render.enums.OrientationEnum.LANDSCAPE;
import static life.centaurs.sunlife.video.render.enums.OrientationEnum.LANDSCAPE_REVERSE;
import static life.centaurs.sunlife.video.render.enums.OrientationEnum.PORTRAIT;
import static life.centaurs.sunlife.video.render.enums.OrientationEnum.PORTRAIT_REVERSE;

public class CameraActivity extends AppCompatActivity implements SensorEventListener, FragmentsCommunicationListener {
    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private CameraFragment cameraFragment;
    private CameraNavigationFragment cameraNavigationFragment;
    private int cameraId = DeviceCamerasEnum.BACK_CAMERA.getCAMERA_ID();
    private FragmentTransaction transaction;
    private static OrientationEnum orientationEnum = PORTRAIT;
    private static MediaExtensionEnum videoExtension;
    private static MediaExtensionEnum photoExtension;
    private ChoseSound choseSound;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        View viewBackground = this.getWindow().getDecorView();
        viewBackground.setBackgroundColor(SPLASH_SCREEN_BACKGROUND_COLOR);
        videoExtension = MediaExtensionEnum.MP4;
        photoExtension = MediaExtensionEnum.JPG;

        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        choseSound = new ChoseSound(this);
        choseSound.chooseAndCopyAssetsSoundToSd();

        if (!checkWriteExternalPermission()){
            ActivityCompat.requestPermissions(CameraActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE
                    , Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA
                    , Manifest.permission.RECORD_AUDIO}, 1);
        }

        cameraFragment = new CameraFragment(cameraId);
        transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.container, cameraFragment);
        transaction.addToBackStack(null);
        transaction.commit();

        cameraNavigationFragment = new CameraNavigationFragment();
        transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.container1, cameraNavigationFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private boolean checkWriteExternalPermission() {
        int readResultPermission = this.checkCallingOrSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
        int cameraResultPermission = this.checkCallingOrSelfPermission(Manifest.permission.CAMERA);
        int writeResultPermission = this.checkCallingOrSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int recordResultPermission = this.checkCallingOrSelfPermission(Manifest.permission.RECORD_AUDIO);
        return (readResultPermission == PackageManager.PERMISSION_GRANTED
                && cameraResultPermission == PackageManager.PERMISSION_GRANTED
                && writeResultPermission == PackageManager.PERMISSION_GRANTED
                && recordResultPermission == PackageManager.PERMISSION_GRANTED);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(CameraActivity.this, "Permission denied to read your External storage", Toast.LENGTH_SHORT).show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    public static MediaExtensionEnum getVideoExtension() {
        return videoExtension;
    }

    public static MediaExtensionEnum getPhotoExtension() {
        return photoExtension;
    }

    protected void onPause() {
        super.onPause();
        senSensorManager.unregisterListener(this);
    }

    protected void onResume() {
        super.onResume();
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor mySensor = sensorEvent.sensor;
        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = sensorEvent.values[0];
            float y = sensorEvent.values[1];
            float z = sensorEvent.values[2];
            setOrientationEnum(x, y, z);
            if (!CameraNavigationFragment.isProcessing() && (CameraFragment.getRealVideoOrientationEnum() != null
                    && getOrientationEnum() != CameraFragment.getRealVideoOrientationEnum())){
                switch(CameraFragment.getRealVideoOrientationEnum()){
                    case PORTRAIT:
                        cameraNavigationFragment.setMessageText(getResources().getString(R.string.portrait_orientation_message));
                        break;
                    case PORTRAIT_REVERSE:
                        cameraNavigationFragment.setMessageText(getResources().getString(R.string.reverse_portrait_orientation_message));
                        break;
                    case LANDSCAPE:
                        cameraNavigationFragment.setMessageText(getResources().getString(R.string.landscape_orientation_message));
                        break;
                    case LANDSCAPE_REVERSE:
                        cameraNavigationFragment.setMessageText(getResources().getString(R.string.reverse_landscape_orientation_message));
                        break;
                }
            } else {
                cameraNavigationFragment.setMessageText("");
            }
        }
    }

    private void setOrientationEnum(float x, float y, float z){
        if (z < 0) z *= (-1);
        boolean isLandscapeReverse = false;
        boolean isPortraitReverse = false;
        if (x < 0){
            x *= (-1);
            isLandscapeReverse = true;
        }
        if (y < 0){
            y *= (-1);
            isPortraitReverse = true;
        }
        if (z < x || z < y){
            if (x < y){
                orientationEnum = isPortraitReverse ? PORTRAIT_REVERSE : PORTRAIT;
            } else {
                orientationEnum = isLandscapeReverse ? LANDSCAPE_REVERSE : LANDSCAPE;
            }
        }
    }

    public static OrientationEnum getOrientationEnum() {
        return orientationEnum;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        View decorView = this.getWindow().getDecorView();
        if(hasFocus){
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    }

    @Override
    public void onClickButton(CommandEnum commandEnum) {
        switch(commandEnum){
            case BACK_TO_MAIN:
                ActivitySquad.goFromCurrentActivityToNewActivity(this, CameraActivity.class);
                break;
            case SWITCH_CAMERA:
                switchCamera();
                break;
            case START_RECORDING:
            case TAKE_PICTURE:
                if(isTimeIsOff())break;
            default:
                cameraFragment.onClickButton(commandEnum);
                break;
        }
    }

    private void switchCamera() {
        cameraFragment = null;
        transaction = getFragmentManager().beginTransaction();
        if(cameraId == DeviceCamerasEnum.BACK_CAMERA.getCAMERA_ID()){
            cameraId = DeviceCamerasEnum.FRONT_CAMERA.getCAMERA_ID();
            cameraFragment = new CameraFragment(cameraId);
            transaction.replace(R.id.container, cameraFragment);
        }else{
            cameraId = DeviceCamerasEnum.BACK_CAMERA.getCAMERA_ID();
            cameraFragment = new CameraFragment(cameraId);
            transaction.replace(R.id.container, cameraFragment);
        }
        transaction.commit();
    }
}