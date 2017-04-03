package life.centaurs.sunlife.video.render.display;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import life.centaurs.sunlife.R;
import life.centaurs.sunlife.video.render.encoder.MediaAudioEncoder;
import life.centaurs.sunlife.video.render.encoder.MediaEncoder;
import life.centaurs.sunlife.video.render.encoder.MediaMuxerWrapper;
import life.centaurs.sunlife.video.render.encoder.MediaVideoEncoder;
import life.centaurs.sunlife.video.render.enums.CommandEnum;
import life.centaurs.sunlife.video.render.enums.DeviceCamerasEnum;
import life.centaurs.sunlife.video.render.enums.OrientationEnum;

import static life.centaurs.sunlife.video.render.enums.CommandEnum.SWITCH_CAMERA;
import static life.centaurs.sunlife.video.render.enums.OrientationEnum.*;
import static life.centaurs.sunlife.video.render.enums.VideoSizeEnum.MEDIUM_SIZE;


public class CameraFragment extends Fragment {
	private CameraGLView cameraPreviewDisplay;
	private MediaMuxerWrapper mediaMuxerWrapper;
	private boolean isRecording = false;
	private int cameraId;
	private Handler handlerVideo = new Handler();;
	private Handler handlerAudio = new Handler();;
	private Handler handlerMuxer = new Handler();;
	private boolean videoEncoderIsReady = false;
	private boolean audioEncoderIsReady = false;
	private static OrientationEnum videoOrientationEnum = null;// get this value uses only getVideoOrientationEnum() method
	private float touchDownX;
	private float touchDownY;
	private float moveX;
	private float moveY;
	private ChunksContainer chunksContainer;

	private FragmentsCommunicationListener fragmentsCommunicationListener;



	public CameraFragment(int cameraId){
		this.cameraId = cameraId;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		chunksContainer = new ChunksContainer(activity);
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
		chunksContainer = new ChunksContainer(context);
		Activity activity = context instanceof Activity ? (Activity) context : null;
		try {
			fragmentsCommunicationListener = (FragmentsCommunicationListener) activity;
		} catch (ClassCastException e){
			throw new ClassCastException(context.toString() + " must implements OnCameraButtonListener");
		}
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		final View rootView = inflater.inflate(R.layout.fragment_main, container, false);
		cameraPreviewDisplay = (CameraGLView)rootView.findViewById(R.id.cameraView);
		cameraPreviewDisplay.setVideoSize(MEDIUM_SIZE);
		cameraPreviewDisplay.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent motionEvent) {
				switch (motionEvent.getAction()) {
					case MotionEvent.ACTION_DOWN:
						touchDownX= motionEvent.getX();
						touchDownY = motionEvent.getY();
						break;
					case MotionEvent.ACTION_UP:
					case MotionEvent.ACTION_CANCEL:
						moveX = (motionEvent.getX() - touchDownX) > 0
								? (motionEvent.getX() - touchDownX) : (motionEvent.getX() - touchDownX) * (-1);
						moveY = (motionEvent.getY() - touchDownY) > 0
								? (motionEvent.getY() - touchDownY) : (motionEvent.getY() - touchDownY) * (-1);
						if ((moveX) > 200){
							fragmentsCommunicationListener.onClickButton(SWITCH_CAMERA);
						}
						break;
				}
				return true;
			}
		});
		CameraGLView.setCameraId(cameraId);
		return rootView;
	}

	@Override
	public void onResume() {
		super.onResume();
		cameraPreviewDisplay.onResume();
	}

	@Override
	public void onPause() {
		stopRecording();
		cameraPreviewDisplay.onPause();
		super.onPause();
	}

	@Override
	public void startActivity(Intent intent) {
		super.startActivity(intent);
	}

	public void onClickButton(CommandEnum commandEnum) {
		switch (commandEnum) {
			case TAKE_PICTURE:
				takePhoto();
				break;
			case START_RECORDING:
				startRecording();
				break;
			case STOP_RECORDING:
				stopRecording();
				break;
		}
	}

	public static OrientationEnum getVideoOrientationEnum(){
		setVideoOrientationEnum();
		return videoOrientationEnum;
	}

	public void takePhoto() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				Camera camera = cameraPreviewDisplay.getCamera();
				Camera.Parameters parameters = camera.getParameters();
				if(cameraId == DeviceCamerasEnum.BACK_CAMERA.getCAMERA_ID()){
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
						if (cameraId == DeviceCamerasEnum.FRONT_CAMERA.getCAMERA_ID()) {
							Bitmap newImage = null;
							Bitmap cameraBitmap = null;
							if (data != null) {
								cameraBitmap = BitmapFactory.decodeByteArray(data, 0, (data != null) ? data.length : 0);
								if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
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
							File file = mediaMuxerWrapper.getCaptureFile(Environment.DIRECTORY_MOVIES, ".jpg", "SL_Photo_");
							curFile[0] = file;
							FileOutputStream fos = new FileOutputStream(file);
							fos.write(imageBytes);
							fos.close();
						} catch (Exception e) {
							e.printStackTrace();
						}
						cameraPreviewDisplay.restartPreview();
						chunksContainer.setChunkFile(curFile[0]);
					}
				});
			}
		}).start();
	}

	private synchronized void startRecordVideo(){
		mediaMuxerWrapper.startRecording();
	}

	public void startRecording(){
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					mediaMuxerWrapper = new MediaMuxerWrapper(CameraActivity.getVideoExtension().getExtensionStr());
				} catch (IOException e) {
					e.printStackTrace();
				}
				handlerMuxer.post(new Runnable() {
					@Override
					public void run() {
						prepareVideoEncoder();
						prepareAudioEncoder();
					}
				});
			}
		}).start();
	}

	private void prepareVideoEncoder(){
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					new MediaVideoEncoder(mediaMuxerWrapper, mediaEncoderListener, cameraPreviewDisplay.getVideoWidth(), cameraPreviewDisplay.getVideoHeight());
					mediaMuxerWrapper.prepareVideo();
				} catch (IOException e) {
					e.printStackTrace();
				}
				handlerVideo.post(new Runnable() {
					@Override
					public void run() {
						videoEncoderIsReady = true;
						if (audioEncoderIsReady && videoEncoderIsReady);
							startRecordVideo();
					}
				});
			}
		}).start();
	}

	private void prepareAudioEncoder(){
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					new MediaAudioEncoder(mediaMuxerWrapper, mediaEncoderListener);
					mediaMuxerWrapper.prepareAudio();
				} catch (IOException e) {
					e.printStackTrace();
				}
				handlerAudio.post(new Runnable() {
					@Override
					public void run() {
						audioEncoderIsReady = true;
						if (videoEncoderIsReady && audioEncoderIsReady);
							startRecordVideo();
					}
				});
			}
		}).start();
	}

	/**
	 * request stop recording
	 */
	protected void stopRecording() {
		if (mediaMuxerWrapper != null) {
			mediaMuxerWrapper.stopRecording();
			chunksContainer.setChunkFile(new File(mediaMuxerWrapper.getOutputPath()));
			mediaMuxerWrapper = null;
		}
	}

	/**
	 * callback methods from encoder
	 */
	private final MediaEncoder.MediaEncoderListener mediaEncoderListener = new MediaEncoder.MediaEncoderListener() {
		@Override
		public void onPrepared(final MediaEncoder encoder) {
			if (encoder instanceof MediaVideoEncoder)
				cameraPreviewDisplay.setVideoEncoder((MediaVideoEncoder)encoder);
		}

		@Override
		public void onStopped(final MediaEncoder encoder) {
			if (encoder instanceof MediaVideoEncoder)
				cameraPreviewDisplay.setVideoEncoder(null);
		}
	};

	private static void setVideoOrientationEnum(){
		if (videoOrientationEnum == null){
			videoOrientationEnum = CameraActivity.getOrientationEnum();
		} else {
			OrientationEnum currentOrientationDeviceEnum = CameraActivity.getOrientationEnum();
			if (videoOrientationEnum.equals(PORTRAIT) && currentOrientationDeviceEnum.equals(PORTRAIT_REVERSE)){
				videoOrientationEnum = PORTRAIT_REVERSE;
			} else if (videoOrientationEnum.equals(PORTRAIT_REVERSE) && currentOrientationDeviceEnum.equals(PORTRAIT)){
				videoOrientationEnum = PORTRAIT;
			} else if (videoOrientationEnum.equals(LANDSCAPE) && currentOrientationDeviceEnum.equals(LANDSCAPE_REVERSE)){
				videoOrientationEnum = LANDSCAPE_REVERSE;
			} else if (videoOrientationEnum.equals(LANDSCAPE_REVERSE) && currentOrientationDeviceEnum.equals(LANDSCAPE)){
				videoOrientationEnum = LANDSCAPE;
			}
		}
	}
}
