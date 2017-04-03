package life.centaurs.sunlife.video.render.glutils;

import android.graphics.SurfaceTexture;
import android.opengl.EGLContext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.text.TextUtils;
import android.view.Surface;
import android.view.SurfaceHolder;

import static life.centaurs.sunlife.video.render.constants.DisplayConstants.UNSUPPORTED_WINDOW_TYPE_MESS;

/**
 * Helper class to draw texture to whole view on private thread
 */
public final class RenderHandler implements Runnable {
	private static final String THREAD_NAME = "RenderHandler";

	private final Object synchronizedObject = new Object();
	private EGLContext eglContext;
	private boolean isRecordable;
	private Object surfaceObject;
	private int textureId = -1;
	private float[] matrix = new float[32];

	private boolean requestSetEglContext;
	private boolean requestRelease;
	private int requestDraw;

	private EGLBase eglBase;
	private EGLBase.EglSurface eglSurface;
	private GLDrawer2D glDrawer2D;

	public static final RenderHandler createHandler(final String name) {
		final RenderHandler handler = new RenderHandler();
		synchronized (handler.synchronizedObject) {
			new Thread(handler, !TextUtils.isEmpty(name) ? name : THREAD_NAME).start();
			try {
				handler.synchronizedObject.wait();
			} catch (final InterruptedException e) {
			}
		}
		return handler;
	}

	public final void setEglContext(final EGLContext shared_context, final int tex_id, final Object surface, final boolean isRecordable) {
		if (!(surface instanceof Surface) && !(surface instanceof SurfaceTexture) && !(surface instanceof SurfaceHolder))
			throw new RuntimeException(UNSUPPORTED_WINDOW_TYPE_MESS + surface);
		synchronized (synchronizedObject) {
			if (requestRelease) return;
			eglContext = shared_context;
			textureId = tex_id;
			surfaceObject = surface;
			this.isRecordable = isRecordable;
			requestSetEglContext = true;
			Matrix.setIdentityM(matrix, 0);
			Matrix.setIdentityM(matrix, 16);
			synchronizedObject.notifyAll();
			try {
				synchronizedObject.wait();
			} catch (final InterruptedException e) {
			}
		}
	}

	public final void draw() {
		draw(textureId, matrix, null);
	}

	public final void draw(final int tex_id) {
		draw(tex_id, matrix, null);
	}

	public final void draw(final float[] tex_matrix) {
		draw(textureId, tex_matrix, null);
	}

	public final void draw(final float[] tex_matrix, final float[] mvp_matrix) {
		draw(textureId, tex_matrix, mvp_matrix);
	}

	public final void draw(final int tex_id, final float[] tex_matrix) {
		draw(tex_id, tex_matrix, null);
	}

	public final void draw(final int tex_id, final float[] tex_matrix, final float[] mvp_matrix) {
		synchronized (synchronizedObject) {
			if (requestRelease) return;
			textureId = tex_id;
			if ((tex_matrix != null) && (tex_matrix.length >= 16)) {
				System.arraycopy(tex_matrix, 0, matrix, 0, 16);
			} else {
				Matrix.setIdentityM(matrix, 0);
			}
			if ((mvp_matrix != null) && (mvp_matrix.length >= 16)) {
				System.arraycopy(mvp_matrix, 0, matrix, 16, 16);
			} else {
				Matrix.setIdentityM(matrix, 16);
			}
			requestDraw++;
			synchronizedObject.notifyAll();
		}
	}

	public boolean isValid() {
		synchronized (synchronizedObject) {
			return !(surfaceObject instanceof Surface) || ((Surface) surfaceObject).isValid();
		}
	}

	public final void release() {
		synchronized (synchronizedObject) {
			if (requestRelease) return;
			requestRelease = true;
			synchronizedObject.notifyAll();
			try {
				synchronizedObject.wait();
			} catch (final InterruptedException e) {
			}
		}
	}

	@Override
	public final void run() {
		synchronized (synchronizedObject) {
			requestSetEglContext = requestRelease = false;
			requestDraw = 0;
			synchronizedObject.notifyAll();
		}
		boolean localRequestDraw;
		for (;;) {
			synchronized (synchronizedObject) {
				if (requestRelease) break;
				if (requestSetEglContext) {
					requestSetEglContext = false;
					internalPrepare();
				}
				localRequestDraw = requestDraw > 0;
				if (localRequestDraw) {
					requestDraw--;
				}
			}
			if (localRequestDraw) {
				if ((eglBase != null) && textureId >= 0) {
					eglSurface.makeCurrent();
					GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
					GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
					glDrawer2D.setMatrix(matrix, 16);
					glDrawer2D.draw(textureId, matrix);
					eglSurface.swap();
				}
			} else {
				synchronized(synchronizedObject) {
					try {
						synchronizedObject.wait();
					} catch (final InterruptedException e) {
						break;
					}
				}
			}
		}
		synchronized (synchronizedObject) {
			requestRelease = true;
			internalRelease();
			synchronizedObject.notifyAll();
		}
	}

	private final void internalPrepare() {
		internalRelease();
		eglBase = new EGLBase(eglContext, false, isRecordable);
		eglSurface = eglBase.createFromSurface(surfaceObject);

		eglSurface.makeCurrent();
		glDrawer2D = new GLDrawer2D();
		surfaceObject = null;
		synchronizedObject.notifyAll();
	}

	private final void internalRelease() {
		if (eglSurface != null) {
			eglSurface.release();
			eglSurface = null;
		}
		if (glDrawer2D != null) {
			glDrawer2D.release();
			glDrawer2D = null;
		}
		if (eglBase != null) {
			eglBase.release();
			eglBase = null;
		}
	}
}
