package life.centaurs.sunlife.video.render.glutils;

import android.annotation.TargetApi;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.os.Build;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import static life.centaurs.sunlife.video.render.constants.DisplayConstants.CHOOSE_CONFIG_FAILED_MESS;
import static life.centaurs.sunlife.video.render.constants.DisplayConstants.EGL_ALREADY_SET_UP_MESSAGE;
import static life.centaurs.sunlife.video.render.constants.DisplayConstants.EGL_CREATE_CONTEXT_STR;
import static life.centaurs.sunlife.video.render.constants.DisplayConstants.EGL_CREATE_PBUFFER_SURFACE_STR;
import static life.centaurs.sunlife.video.render.constants.DisplayConstants.EGL_GET_DISPLAY_FAILED_MESSAGE;
import static life.centaurs.sunlife.video.render.constants.DisplayConstants.EGL_INIT_FAILE_MESS;
import static life.centaurs.sunlife.video.render.constants.DisplayConstants.NULL_SURFACE_MESS;
import static life.centaurs.sunlife.video.render.constants.DisplayConstants.UNSUPPORTED_SURFACE_MESSAGE;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class EGLBase {
	private static final int EGL_RECORDABLE_ANDROID = 0x3142;

	private EGLConfig mEglConfig = null;
	private EGLContext mEglContext = EGL14.EGL_NO_CONTEXT;
	private EGLDisplay mEglDisplay = EGL14.EGL_NO_DISPLAY;
	private EGLContext mDefaultContext = EGL14.EGL_NO_CONTEXT;

	public static class EglSurface {
		private final EGLBase mEgl;
		private EGLSurface mEglSurface = EGL14.EGL_NO_SURFACE;
		private final int mWidth, mHeight;

		EglSurface(final EGLBase egl, final Object surface) {
			if (!(surface instanceof SurfaceView)
					&& !(surface instanceof Surface)
					&& !(surface instanceof SurfaceHolder)
					&& !(surface instanceof SurfaceTexture))
				throw new IllegalArgumentException(UNSUPPORTED_SURFACE_MESSAGE);
			mEgl = egl;
			mEglSurface = mEgl.createWindowSurface(surface);
			mWidth = mEgl.querySurface(mEglSurface, EGL14.EGL_WIDTH);
			mHeight = mEgl.querySurface(mEglSurface, EGL14.EGL_HEIGHT);
		}
		EglSurface(final EGLBase egl, final int width, final int height) {
			mEgl = egl;
			mEglSurface = mEgl.createOffscreenSurface(width, height);
			mWidth = width;
			mHeight = height;
		}

		public void makeCurrent() {
			mEgl.makeCurrent(mEglSurface);
		}

		public void swap() {
			mEgl.swap(mEglSurface);
		}

		public EGLContext getContext() {
			return mEgl.getContext();
		}

		public void release() {
			mEgl.makeDefault();
			mEgl.destroyWindowSurface(mEglSurface);
			mEglSurface = EGL14.EGL_NO_SURFACE;
		}

		public int getWidth() {
			return mWidth;
		}

		public int getHeight() {
			return mHeight;
		}
	}

	public EGLBase(final EGLContext shared_context, final boolean with_depth_buffer, final boolean isRecordable) {
		init(shared_context, with_depth_buffer, isRecordable);
	}

	public void release() {
		if (mEglDisplay != EGL14.EGL_NO_DISPLAY) {
			destroyContext();
			EGL14.eglTerminate(mEglDisplay);
			EGL14.eglReleaseThread();
		}
		mEglDisplay = EGL14.EGL_NO_DISPLAY;
		mEglContext = EGL14.EGL_NO_CONTEXT;
	}

	public EglSurface createFromSurface(final Object surface) {
		final EglSurface eglSurface = new EglSurface(this, surface);
		eglSurface.makeCurrent();
		return eglSurface;
	}

	public EglSurface createOffscreen(final int width, final int height) {
		final EglSurface eglSurface = new EglSurface(this, width, height);
		eglSurface.makeCurrent();
		return eglSurface;
	}

	public EGLContext getContext() {
		return mEglContext;
	}

	public int querySurface(final EGLSurface eglSurface, final int what) {
		final int[] value = new int[1];
		EGL14.eglQuerySurface(mEglDisplay, eglSurface, what, value, 0);
		return value[0];
	}

	private void init(EGLContext shared_context, final boolean with_depth_buffer, final boolean isRecordable) {
		if (mEglDisplay != EGL14.EGL_NO_DISPLAY) {
			throw new RuntimeException(EGL_ALREADY_SET_UP_MESSAGE);
		}
		mEglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
		if (mEglDisplay == EGL14.EGL_NO_DISPLAY) {
			throw new RuntimeException(EGL_GET_DISPLAY_FAILED_MESSAGE);
		}
		final int[] version = new int[2];
		if (!EGL14.eglInitialize(mEglDisplay, version, 0, version, 1)) {
			mEglDisplay = null;
			throw new RuntimeException(EGL_INIT_FAILE_MESS);
		}
		shared_context = shared_context != null ? shared_context : EGL14.EGL_NO_CONTEXT;
		if (mEglContext == EGL14.EGL_NO_CONTEXT) {
			mEglConfig = getConfig(with_depth_buffer, isRecordable);
			if (mEglConfig == null) {
				throw new RuntimeException(CHOOSE_CONFIG_FAILED_MESS);
			}
			mEglContext = createContext(shared_context);
		}
		final int[] values = new int[1];
		EGL14.eglQueryContext(mEglDisplay, mEglContext, EGL14.EGL_CONTEXT_CLIENT_VERSION, values, 0);
		makeDefault();
	}

	/**
	 * change context to draw this window surface
	 * @return
	 */
	private boolean makeCurrent(final EGLSurface surface) {
		if (surface == null || surface == EGL14.EGL_NO_SURFACE) {
			final int error = EGL14.eglGetError();
			if (error == EGL14.EGL_BAD_NATIVE_WINDOW) {
			}
			return false;
		}
		if (!EGL14.eglMakeCurrent(mEglDisplay, surface, surface, mEglContext)) {
			return false;
		}
		return true;
	}

	private void makeDefault() {
		EGL14.eglMakeCurrent(mEglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
	}

	private int swap(final EGLSurface surface) {
		if (!EGL14.eglSwapBuffers(mEglDisplay, surface)) {
			final int err = EGL14.eglGetError();
			return err;
		}
		return EGL14.EGL_SUCCESS;
	}

	private EGLContext createContext(final EGLContext shared_context) {
		final int[] attrib_list = {
				EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
				EGL14.EGL_NONE
		};
		final EGLContext context = EGL14.eglCreateContext(mEglDisplay, mEglConfig, shared_context, attrib_list, 0);
		checkEglError(EGL_CREATE_CONTEXT_STR);
		return context;
	}

	private void destroyContext() {
		EGL14.eglDestroyContext(mEglDisplay, mEglContext);
		mEglContext = EGL14.EGL_NO_CONTEXT;
		if (mDefaultContext != EGL14.EGL_NO_CONTEXT) {
			EGL14.eglDestroyContext(mEglDisplay, mDefaultContext);
			mDefaultContext = EGL14.EGL_NO_CONTEXT;
		}
	}

	private EGLSurface createWindowSurface(final Object nativeWindow) {
		final int[] surfaceAttribs = {
				EGL14.EGL_NONE
		};
		EGLSurface result = null;
		try {
			result = EGL14.eglCreateWindowSurface(mEglDisplay, mEglConfig, nativeWindow, surfaceAttribs, 0);
		} catch (final IllegalArgumentException e) {
		}
		return result;
	}

	/**
	 * Creates an EGL surface associated with an offscreen buffer.
	 */
	private EGLSurface createOffscreenSurface(final int width, final int height) {
		final int[] surfaceAttribs = {
				EGL14.EGL_WIDTH, width,
				EGL14.EGL_HEIGHT, height,
				EGL14.EGL_NONE
		};
		EGLSurface result = null;
		try {
			result = EGL14.eglCreatePbufferSurface(mEglDisplay, mEglConfig, surfaceAttribs, 0);
			checkEglError(EGL_CREATE_PBUFFER_SURFACE_STR);
			if (result == null) {
				throw new RuntimeException(NULL_SURFACE_MESS);
			}
		} catch (final IllegalArgumentException e) {
		} catch (final RuntimeException e) {
		}
		return result;
	}

	private void destroyWindowSurface(EGLSurface surface) {
		if (surface != EGL14.EGL_NO_SURFACE) {
			EGL14.eglMakeCurrent(mEglDisplay,
					EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
			EGL14.eglDestroySurface(mEglDisplay, surface);
		}
		surface = EGL14.EGL_NO_SURFACE;
	}

	private void checkEglError(final String msg) {
		int error;
		if ((error = EGL14.eglGetError()) != EGL14.EGL_SUCCESS) {
			throw new RuntimeException(msg + ": EGL error: 0x" + Integer.toHexString(error));
		}
	}

	private EGLConfig getConfig(final boolean with_depth_buffer, final boolean isRecordable) {
		final int[] attribList = {
				EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
				EGL14.EGL_RED_SIZE, 8,
				EGL14.EGL_GREEN_SIZE, 8,
				EGL14.EGL_BLUE_SIZE, 8,
				EGL14.EGL_ALPHA_SIZE, 8,
				EGL14.EGL_NONE, EGL14.EGL_NONE,
				EGL14.EGL_NONE, EGL14.EGL_NONE,
				EGL14.EGL_NONE,	EGL14.EGL_NONE,

				EGL14.EGL_NONE
		};
		int offset = 10;
		if (false) {
			attribList[offset++] = EGL14.EGL_STENCIL_SIZE;
			attribList[offset++] = 8;
		}
		if (with_depth_buffer) {
			attribList[offset++] = EGL14.EGL_DEPTH_SIZE;
			attribList[offset++] = 16;
		}
		if (isRecordable && (Build.VERSION.SDK_INT >= 18)) {
			attribList[offset++] = EGL_RECORDABLE_ANDROID;
			attribList[offset++] = 1;
		}
		for (int i = attribList.length - 1; i >= offset; i--) {
			attribList[i] = EGL14.EGL_NONE;
		}
		final EGLConfig[] configs = new EGLConfig[1];
		final int[] numConfigs = new int[1];
		if (!EGL14.eglChooseConfig(mEglDisplay, attribList, 0, configs, 0, configs.length, numConfigs, 0)) {
			return null;
		}
		return configs[0];
	}
}
