package life.centaurs.sunlife.video.render.encoder;


import android.app.Fragment;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Environment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import life.centaurs.sunlife.video.render.display.CameraFragment;
import life.centaurs.sunlife.video.render.display.CameraGLView;
import life.centaurs.sunlife.video.render.enums.DeviceCamerasEnum;
import life.centaurs.sunlife.video.render.enums.MediaExtensionEnum;

import static life.centaurs.sunlife.video.render.display.CameraFragment.*;
import static life.centaurs.sunlife.video.render.enums.OrientationEnum.*;

public class PhotoManager {
    private static final String PHOTO_NAME_PREFIX = "SL_Photo_";
    private Fragment fragment;
    private CameraGLView cameraPreviewDisplay;

    public PhotoManager(Fragment fragment, CameraGLView cameraPreviewDisplay) {
        this.fragment = fragment;
        this.cameraPreviewDisplay = cameraPreviewDisplay;
    }

    public void takePhoto(){
        //new Thread(new Runnable() {
        //    @Override
        //    public void run() {
                Camera camera = cameraPreviewDisplay.getCamera();
                Camera.Parameters parameters = camera.getParameters();
                if(CameraFragment.getCameraId() == DeviceCamerasEnum.BACK_CAMERA.getCAMERA_ID()){
                    parameters.setRotation(CameraFragment.getVideoOrientationEnum().getDegrees() <= PORTRAIT_REVERSE.getDegrees()
                            ? CameraFragment.getVideoOrientationEnum().getDegrees() + LANDSCAPE_REVERSE.getDegrees()
                            : PORTRAIT.getDegrees());
                } else {
                    parameters.setRotation(LANDSCAPE.getDegrees() - CameraFragment.getVideoOrientationEnum().getDegrees());
                }
                final File[] curFile = new File[1];
                camera.setParameters(parameters);
                camera.takePicture(null, null, new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {
                        byte[] imageBytes = data;
                        if (CameraFragment.getCameraId() == DeviceCamerasEnum.FRONT_CAMERA.getCAMERA_ID()) {
                            Bitmap newImage = null;
                            Bitmap cameraBitmap = null;
                            if (data != null) {
                                cameraBitmap = BitmapFactory.decodeByteArray(data, 0, (data != null) ? data.length : 0);
                                if (fragment.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                                    Matrix mtx = new Matrix();
                                    mtx.preScale(-1.0f, 1.0f);
                                    newImage = Bitmap.createBitmap(cameraBitmap, 0, 0, cameraBitmap.getWidth(), cameraBitmap.getHeight(), mtx, true);
                                }
                            }
                            ByteArrayOutputStream stream = new ByteArrayOutputStream();
                            newImage.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                            imageBytes = stream.toByteArray();
                        }
                        try {
                            File file = MediaMuxerWrapper.getCaptureFile(Environment.DIRECTORY_MOVIES
                                    , MediaExtensionEnum.JPG.getExtensionStr(), PHOTO_NAME_PREFIX);
                            curFile[0] = file;
                            FileOutputStream fos = new FileOutputStream(file);
                            fos.write(imageBytes);
                            fos.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        cameraPreviewDisplay.restartPreview();
                        currentFile = curFile[0];
                        chunksContainer.setChunkFile(currentFile);
                    }
                });
            //}
        //}).start();
    }
}
