package life.centaurs.sunlife.video.render.display;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;
import java.io.IOException;

import life.centaurs.sunlife.R;
import life.centaurs.sunlife.activities.CameraActivity;
import life.centaurs.sunlife.video.render.encoder.MediaAudioEncoder;
import life.centaurs.sunlife.video.render.encoder.MediaEncoder;
import life.centaurs.sunlife.video.render.encoder.MediaMuxerWrapper;
import life.centaurs.sunlife.video.render.encoder.MediaVideoEncoder;
import life.centaurs.sunlife.video.render.encoder.PhotoManager;
import life.centaurs.sunlife.video.render.enums.CommandEnum;
import life.centaurs.sunlife.video.render.enums.OrientationEnum;

import static life.centaurs.sunlife.video.render.constants.DisplayConstants.DEPRECATION_ANNOTATION_MESs;
import static life.centaurs.sunlife.video.render.constants.DisplayConstants.MUST_IMPL_ON_CAMERA_BUTTON_LISTENER_MESS;
import static life.centaurs.sunlife.video.render.constants.DisplayConstants.SCREEN_TOUCH_TIME;
import static life.centaurs.sunlife.video.render.constants.DisplayConstants.TOUGH_LENGTH_TO_SWITCH_CAMERA;
import static life.centaurs.sunlife.video.render.constants.DisplayConstants.VIDEO_SIZE;
import static life.centaurs.sunlife.video.render.enums.CommandEnum.SWITCH_CAMERA;


public class CameraFragment extends Fragment {
	private CameraGLView cameraPreviewDisplay;
	private MediaMuxerWrapper mediaMuxerWrapper;
	private boolean isRecording = false;
	private static int cameraId;
	private Handler handler = new Handler();
	private Handler handlerVideo = new Handler();;
	private Handler handlerAudio = new Handler();;
	private Handler handlerMuxer = new Handler();;
	private boolean videoEncoderIsReady = false;
	private boolean audioEncoderIsReady = false;
	private float touchDownX;
	private float touchDownY;
	private float moveX;
	private float moveY;
	private PhotoManager photoManager;
	private static OrientationEnum videoOrientationEnum = null;// use this field only by dint of getVideoOrientationEnum() method
	public static File currentFile;
	private static long lastScreenTouch = 0;

	private FragmentsCommunicationListener fragmentsCommunicationListener;

	public CameraFragment(int cameraId){
		this.cameraId = cameraId;
	}

	@SuppressWarnings(DEPRECATION_ANNOTATION_MESs)
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
			try {
				fragmentsCommunicationListener = (FragmentsCommunicationListener) activity;
			} catch (ClassCastException e) {
				throw new ClassCastException(activity.toString() + MUST_IMPL_ON_CAMERA_BUTTON_LISTENER_MESS);
			}
		}
	}

	public File getCurrentFile() {
		return currentFile;
	}

	public void setCurrentFile(File currentFile) {
		this.currentFile = currentFile;
	}

	public static int getCameraId() {
		return cameraId;
	}

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		Activity activity = context instanceof Activity ? (Activity) context : null;
		try {
			fragmentsCommunicationListener = (FragmentsCommunicationListener) activity;
		} catch (ClassCastException e){
			throw new ClassCastException(context.toString() + MUST_IMPL_ON_CAMERA_BUTTON_LISTENER_MESS);
		}
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		final View rootView = inflater.inflate(R.layout.fragment_main, container, false);
		cameraPreviewDisplay = (CameraGLView)rootView.findViewById(R.id.cameraView);
		cameraPreviewDisplay.setVideoSize(VIDEO_SIZE);
		cameraPreviewDisplay.setOnTouchListener(screenTouchListener);
		CameraGLView.setCameraId(cameraId);
		photoManager = new PhotoManager(this, cameraPreviewDisplay);
		return rootView;
	}

	View.OnTouchListener screenTouchListener = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View view, MotionEvent motionEvent) {
            if (!CameraNavigationFragment.isProcessing()) {
                long currentTouchTime = System.currentTimeMillis();
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        touchDownX = motionEvent.getX();
                        touchDownY = motionEvent.getY();
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if ((currentTouchTime - lastScreenTouch) > SCREEN_TOUCH_TIME) {
                            lastScreenTouch = currentTouchTime;
                            moveX = (motionEvent.getX() - touchDownX) > 0
                                    ? (motionEvent.getX() - touchDownX) : (motionEvent.getX() - touchDownX) * (-1);
                            moveY = (motionEvent.getY() - touchDownY) > 0
                                    ? (motionEvent.getY() - touchDownY) : (motionEvent.getY() - touchDownY) * (-1);
                            if ((moveX) > TOUGH_LENGTH_TO_SWITCH_CAMERA) {
                                fragmentsCommunicationListener.onClickButton(SWITCH_CAMERA);
                            }
                        }
                        break;
                }
            }
			return true;
		}
	};

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
				setVideoOrientationEnum();
				takePhoto();
				break;
			case START_RECORDING:
				setVideoOrientationEnum();
				startRecording();
				break;
			case STOP_RECORDING:
				stopRecording();
				break;
		}
	}

	public static OrientationEnum getRealVideoOrientationEnum(){
		return videoOrientationEnum;
	}

	public static OrientationEnum getVideoOrientationEnum(){
		setVideoOrientationEnum();
		return videoOrientationEnum;
	}

	public static void setNullVideoOrientationEnum(){
		videoOrientationEnum = null;
	}

	public void takePhoto() {
		photoManager.takePhoto();
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
					new MediaVideoEncoder(mediaMuxerWrapper, mediaEncoderListener
							, cameraPreviewDisplay.getVideoWidth(), cameraPreviewDisplay.getVideoHeight());
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
		}
	}
}
