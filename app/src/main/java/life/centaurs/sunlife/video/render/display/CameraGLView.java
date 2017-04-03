package life.centaurs.sunlife.video.render.display;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import life.centaurs.sunlife.video.render.encoder.MediaVideoEncoder;
import life.centaurs.sunlife.video.render.enums.DeviceCamerasEnum;
import life.centaurs.sunlife.video.render.enums.VideoSizeEnum;
import life.centaurs.sunlife.video.render.glutils.GLDrawer2D;

/**
 * Sub class of GLSurfaceView to display camera preview and write video frame to capturing surface
 */
public final class CameraGLView extends GLSurfaceView {
	private static int cameraId = DeviceCamerasEnum.BACK_CAMERA.getCAMERA_ID();
	private final CameraSurfaceRenderer cameraSurfaceRenderer;
	private boolean hasSurface;
	private CameraHandler cameraHandler = null;
	private static int videoWidth;
	private static int videoHeight;

	public CameraGLView(final Context context) {
		this(context, null, 0);
	}

	public CameraGLView(final Context context, final AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public CameraGLView(final Context context, final AttributeSet attrs, final int defStyle) {
		super(context, attrs);
		cameraSurfaceRenderer = new CameraSurfaceRenderer(this);
		setEGLContextClientVersion(2);
		setRenderer(cameraSurfaceRenderer);
	}

	public Camera getCamera() {
		return cameraHandler.getCameraThread().getmCamera();
	}

	public static void setCameraId(int cameraId) {
		CameraGLView.cameraId = cameraId;
	}

	public static int getCameraId() {
		return cameraId;
	}

	@Override
	public void onResume() {
		super.onResume();
		if (hasSurface) {
			if (cameraHandler == null) {
				startPreview();
			}
		}
	}

	@Override
	public void onPause() {
		if (cameraHandler != null) {
			cameraHandler.stopPreview(false);
		}
		super.onPause();
	}

	public void restartPreview(){
		cameraHandler.getCameraThread().restartPreviewCamera();
	}

	public void setVideoSize(int width, int height) {
		videoWidth = height;
		videoHeight = width;
		queueEvent(new Runnable() {
			@Override
			public void run() {
				cameraSurfaceRenderer.updateViewport();
			}
		});
	}

	public void setVideoSize(VideoSizeEnum videoSizeEnum) {
		setVideoSize(videoSizeEnum.getWIDTH(), videoSizeEnum.getHEIGHT());
	}

	public int getVideoWidth() {
		return videoWidth;
	}

	public int getVideoHeight() {
		return videoHeight;
	}

	public SurfaceTexture getSurfaceTexture() {
		return cameraSurfaceRenderer != null ? cameraSurfaceRenderer.surfaceTexture : null;
	}

	@Override
	public void surfaceDestroyed(final SurfaceHolder holder) {
		if (cameraHandler != null) {
			cameraHandler.stopPreview(true);
		}
		cameraHandler = null;
		hasSurface = false;
		cameraSurfaceRenderer.onSurfaceDestroyed();
		super.surfaceDestroyed(holder);
	}

	public void setVideoEncoder(final MediaVideoEncoder encoder) {
		queueEvent(new Runnable() {
			@Override
			public void run() {
				synchronized (cameraSurfaceRenderer) {
					if (encoder != null) {
						encoder.setEglContext(EGL14.eglGetCurrentContext(), cameraSurfaceRenderer.hTex);
					}
					cameraSurfaceRenderer.mediaVideoEncoder = encoder;
				}
			}
		});
	}

	private synchronized void startPreview() {
		if (cameraHandler == null) {
			final CameraThread thread = new CameraThread(this);
			thread.start();
			cameraHandler = thread.getHandler();
		}
		cameraHandler.startPreview(videoWidth, videoHeight);
	}

	/**
	 * GLSurfaceViewRenderer
	 */
	private static final class CameraSurfaceRenderer implements Renderer, SurfaceTexture.OnFrameAvailableListener {
		private final WeakReference<CameraGLView> CAMERA_GLVIEW_WEAK_REFERENCE;
		private SurfaceTexture surfaceTexture;
		private int hTex;
		private GLDrawer2D glDrawer2D;
		private final float[] ST_MATRIX = new float[16];
		private final float[] MVP_MATRIX = new float[16];
		private MediaVideoEncoder mediaVideoEncoder;
		private volatile boolean requestUpdateTex = false;
		private boolean flip = true;

		public CameraSurfaceRenderer(final CameraGLView parent) {
			CAMERA_GLVIEW_WEAK_REFERENCE = new WeakReference<CameraGLView>(parent);
			Matrix.setIdentityM(MVP_MATRIX, 0);
		}

		@Override
		public void onSurfaceCreated(final GL10 unused, final EGLConfig config) {
			final String extensions = GLES20.glGetString(GLES20.GL_EXTENSIONS);
			if (!extensions.contains("OES_EGL_image_external")) throw new RuntimeException("This system does not support OES_EGL_image_external.");
			hTex = GLDrawer2D.initTex();
			surfaceTexture = new SurfaceTexture(hTex);
			surfaceTexture.setOnFrameAvailableListener(this);
			GLES20.glClearColor(0f, 0f, 0f, 0f);
			final CameraGLView parent = CAMERA_GLVIEW_WEAK_REFERENCE.get();
			if (parent != null) {
				parent.hasSurface = true;
			}
			glDrawer2D = new GLDrawer2D();
			glDrawer2D.setMatrix(MVP_MATRIX, 0);
		}

		@Override
		public void onSurfaceChanged(final GL10 unused, final int width, final int height) {
			if ((width == 0) || (height == 0)) return;
			updateViewport();
			final CameraGLView parent = CAMERA_GLVIEW_WEAK_REFERENCE.get();
			if (parent != null) {
				parent.startPreview();
			}
		}

		/**
		 * when GLSurface context is soon destroyed
		 */
		public void onSurfaceDestroyed() {
			if (glDrawer2D != null) {
				glDrawer2D.release();
				glDrawer2D = null;
			}
			if (surfaceTexture != null) {
				surfaceTexture.release();
				surfaceTexture = null;
			}
			GLDrawer2D.deleteTex(hTex);
		}

		private final void updateViewport() {
			final CameraGLView parent = CAMERA_GLVIEW_WEAK_REFERENCE.get();
			if (parent != null) {
				final int view_width = parent.getWidth();
				final int view_height = parent.getHeight();
				GLES20.glViewport(0, 0, view_width, view_height);
				GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
				final double video_width = parent.videoWidth;
				final double video_height = parent.videoHeight;
				if (video_width == 0 || video_height == 0) return;
				Matrix.setIdentityM(MVP_MATRIX, 0);
				final double view_aspect = view_width / (double)view_height;
				final double req = video_width / video_height;
				int x, y;
				int width, height;
				if (view_aspect > req) {
					y = 0;
					height = view_height;
					width = (int)(req * view_height);
					x = (view_width - width) / 2;
				} else {
					x = 0;
					width = view_width;
					height = (int)(view_width / req);
					y = (view_height - height) / 2;
				}
				GLES20.glViewport(x, y, width, height);
				if (glDrawer2D != null)
					glDrawer2D.setMatrix(MVP_MATRIX, 0);
			}
		}

		/**
		 * drawing to GLSurface
		 * we set renderMode to GLSurfaceView.RENDERMODE_WHEN_DIRTY,
		 * this method is only called when #requestRender is called(= when texture is required to update)
		 * if you don't set RENDERMODE_WHEN_DIRTY, this method is called at maximum 60fps
		 */
		@Override
		public void onDrawFrame(final GL10 unused) {
			GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

			if (requestUpdateTex) {
				requestUpdateTex = false;
				surfaceTexture.updateTexImage();
				surfaceTexture.getTransformMatrix(ST_MATRIX);
			}
			glDrawer2D.draw(hTex, ST_MATRIX);
			flip = !flip;
			if (flip) {
				synchronized (this) {
					if (mediaVideoEncoder != null) {
						mediaVideoEncoder.frameAvailableSoon(ST_MATRIX, MVP_MATRIX);
					}
				}
			}
		}

		@Override
		public void onFrameAvailable(final SurfaceTexture st) {
			requestUpdateTex = true;
		}
	}

	/**
	 * Handler class for asynchronous camera operation
	 */
	private static final class CameraHandler extends Handler {
		private static final int MSG_PREVIEW_START = 1;
		private static final int MSG_PREVIEW_STOP = 2;
		private CameraThread cameraThread;

		public CameraHandler(final CameraThread thread) {
			cameraThread = thread;
		}

		public CameraThread getCameraThread() {
			return cameraThread;
		}

		public void startPreview(final int width, final int height) {
			sendMessage(obtainMessage(MSG_PREVIEW_START, width, height));
		}

		/**
		 * request to stop camera preview
		 * @param needWait need to wait for stopping camera preview
		 */
		public void stopPreview(final boolean needWait) {
			synchronized (this) {
				sendEmptyMessage(MSG_PREVIEW_STOP);
				if (needWait && cameraThread.mIsRunning) {
					try {
						wait();
					} catch (final InterruptedException e) {
					}
				}
			}
		}

		/**
		 * message handler for camera thread
		 */
		@Override
		public void handleMessage(final Message msg) {
			switch (msg.what) {
			case MSG_PREVIEW_START:
				cameraThread.startPreview();
				break;
			case MSG_PREVIEW_STOP:
				cameraThread.stopPreview();
				synchronized (this) {
					notifyAll();
				}
				Looper.myLooper().quit();
				cameraThread = null;
				break;
			default:
				throw new RuntimeException("unknown message:what=" + msg.what);
			}
		}
	}

	/**
	 * Thread for asynchronous operation of camera preview
	 */
	private static final class CameraThread extends Thread {
    	private final Object mReadyFence = new Object();
    	private final WeakReference<CameraGLView>mWeakParent;
    	private CameraHandler mHandler;
    	private volatile boolean mIsRunning = false;
		private Camera mCamera;
		private boolean mIsFrontFace;

		public Camera getmCamera() {
			return mCamera;
		}

		public CameraThread(final CameraGLView parent) {
			super("Camera thread");
    		mWeakParent = new WeakReference<CameraGLView>(parent);
    	}

    	public CameraHandler getHandler() {
            synchronized (mReadyFence) {
            	try {
            		mReadyFence.wait();
            	} catch (final InterruptedException e) {
                }
            }
            return mHandler;
    	}

    	/**
    	 * message loop
    	 * prepare Looper and create Handler for this thread
    	 */
		@Override
		public void run() {
            Looper.prepare();
            synchronized (mReadyFence) {
                mHandler = new CameraHandler(this);
                mIsRunning = true;
                mReadyFence.notify();
            }
            Looper.loop();
            synchronized (mReadyFence) {
                mHandler = null;
                mIsRunning = false;
            }
		}

		/**
		 * start camera preview
		 */
		private final void startPreview() {
			final CameraGLView parent = mWeakParent.get();
			if ((parent != null) && (mCamera == null)) {
				try {
					mCamera = Camera.open(cameraId);
					final Camera.Parameters params = mCamera.getParameters();
					final List<String> focusModes = params.getSupportedFocusModes();
					if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
						params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
					} else if(focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
						params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
					} else {
						//TODO hand focus
					}
					final List<int[]> supportedFpsRange = params.getSupportedPreviewFpsRange();
					final int[] max_fps = supportedFpsRange.get(supportedFpsRange.size() - 1);
					params.setPreviewFpsRange(max_fps[0], max_fps[1]);
					params.setRecordingHint(true);
					params.setPreviewSize(videoHeight, videoWidth);
					params.setPictureSize(videoHeight, videoWidth);
					setRotation();
					//mCamera.setDisplayOrientation(90);
					mCamera.setParameters(params);
					final Camera.Size previewSize = mCamera.getParameters().getPreviewSize();
					parent.post(new Runnable() {
						@Override
						public void run() {
							parent.setVideoSize(previewSize.width, previewSize.height);
						}
					});
					final SurfaceTexture st = parent.getSurfaceTexture();
					st.setDefaultBufferSize(previewSize.width, previewSize.height);
					mCamera.setPreviewTexture(st);
				} catch (final IOException e) {
					if (mCamera != null) {
						mCamera.release();
						mCamera = null;
					}
				} catch (final RuntimeException e) {
					if (mCamera != null) {
						mCamera.release();
						mCamera = null;
					}
				}
				if (mCamera != null) {
					mCamera.startPreview();
				}
			}
		}

		private static Camera.Size getClosestSupportedSize(List<Camera.Size> supportedSizes, final int requestedWidth, final int requestedHeight) {
			return (Camera.Size)Collections.min(supportedSizes, new Comparator<Camera.Size>() {

				private int diff(final Camera.Size size) {
					return Math.abs(requestedWidth - size.width) + Math.abs(requestedHeight - size.height);
				}

				@Override
				public int compare(final Camera.Size lhs, final Camera.Size rhs) {
					return diff(lhs) - diff(rhs);
				}
			});

		}

		/**
		 * stop camera preview
		 */
		private void stopPreview() {
			if (mCamera != null) {
				mCamera.stopPreview();
		        mCamera.release();
		        mCamera = null;
			}
			final CameraGLView parent = mWeakParent.get();
			if (parent == null) return;
			parent.cameraHandler = null;
		}

		/**
		 * rotate preview screen according to the device orientation
		 */
		private final void setRotation() {
			final CameraGLView parent = mWeakParent.get();
			if (parent == null) return;
			int degrees = 0;
			final Camera.CameraInfo info = new Camera.CameraInfo();
			Camera.getCameraInfo(cameraId, info);
			mIsFrontFace = (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT);
			if (mIsFrontFace) {
				degrees = (info.orientation + degrees) % 360;
				degrees = (360 - degrees) % 360;
			} else {
				degrees = (info.orientation - degrees + 360) % 360;
			}
			mCamera.setDisplayOrientation(degrees);
		}

		public void restartPreviewCamera(){
			mCamera.stopPreview();
			mCamera.startPreview();
		}
	}
}
