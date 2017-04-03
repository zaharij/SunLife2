package life.centaurs.sunlife.video.render.glutils;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static life.centaurs.sunlife.video.render.constants.DisplayConstants.A_POSITION_STR;
import static life.centaurs.sunlife.video.render.constants.DisplayConstants.A_TEXTURE_COORD_STR;
import static life.centaurs.sunlife.video.render.constants.DisplayConstants.U_MVP_MATRIX_STR;
import static life.centaurs.sunlife.video.render.constants.DisplayConstants.U_TEX_MATRIX_STR;

/**
 * Helper class to draw to whole view using specific texture and texture matrix
 */
public class GLDrawer2D {
	private final static String VSS = "uniform mat4 uMVPMatrix;\nuniform mat4 uTexMatrix;\nattribute highp vec4 aPosition;\nattribute highp vec4 aTextureCoord;\nvarying highp vec2 vTextureCoord;\n\nvoid main() {\ngl_Position = uMVPMatrix * aPosition;\nvTextureCoord = (uTexMatrix * aTextureCoord).xy;\n}\n";
	private static final String FSS = "#extension GL_OES_EGL_image_external : require\nprecision mediump float;\nuniform samplerExternalOES sTexture;\nvarying highp vec2 vTextureCoord;\nvoid main() {\ngl_FragColor = texture2D(sTexture, vTextureCoord);\n}";
	private static final float[] VERTICES = { 1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, -1.0f };
	private static final float[] TEXTCOORD = { 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f };

	private final FloatBuffer floatBufferVertex;
	private final FloatBuffer floatBufferTexCoord;
	private int program;
	int positionLoc;
	int textureCoordLoc;
	int mVPMatrixLoc;
	int texMatrixLoc;
	private final float[] MVP_MATRIX = new float[16];

	private static final int FLOAT_SZ = Float.SIZE / 8;
	private static final int VERTEX_NUM = 4;
	private static final int VERTEX_SZ = VERTEX_NUM * 2;

	/**
	 * Constructor
	 * this should be called in GL context
	 */
	public GLDrawer2D() {
		floatBufferVertex = ByteBuffer.allocateDirect(VERTEX_SZ * FLOAT_SZ).order(ByteOrder.nativeOrder()).asFloatBuffer();
		floatBufferVertex.put(VERTICES);
		floatBufferVertex.flip();
		floatBufferTexCoord = ByteBuffer.allocateDirect(VERTEX_SZ * FLOAT_SZ).order(ByteOrder.nativeOrder()).asFloatBuffer();
		floatBufferTexCoord.put(TEXTCOORD);
		floatBufferTexCoord.flip();

		program = loadShader(VSS, FSS);
		GLES20.glUseProgram(program);
		positionLoc = GLES20.glGetAttribLocation(program, A_POSITION_STR);
		textureCoordLoc = GLES20.glGetAttribLocation(program, A_TEXTURE_COORD_STR);
		mVPMatrixLoc = GLES20.glGetUniformLocation(program, U_MVP_MATRIX_STR);
		texMatrixLoc = GLES20.glGetUniformLocation(program, U_TEX_MATRIX_STR);

		Matrix.setIdentityM(MVP_MATRIX, 0);
		GLES20.glUniformMatrix4fv(mVPMatrixLoc, 1, false, MVP_MATRIX, 0);
		GLES20.glUniformMatrix4fv(texMatrixLoc, 1, false, MVP_MATRIX, 0);
		GLES20.glVertexAttribPointer(positionLoc, 2, GLES20.GL_FLOAT, false, VERTEX_SZ, floatBufferVertex);
		GLES20.glVertexAttribPointer(textureCoordLoc, 2, GLES20.GL_FLOAT, false, VERTEX_SZ, floatBufferTexCoord);
		GLES20.glEnableVertexAttribArray(positionLoc);
		GLES20.glEnableVertexAttribArray(textureCoordLoc);
	}

	/**
	 * terminatinng, this should be called in GL context
	 */
	public void release() {
		if (program >= 0)
			GLES20.glDeleteProgram(program);
		program = -1;
	}

	/**
	 * draw specific texture with specific texture matrix
	 * @param tex_id texture ID
	 * @param tex_matrix texture matrix、if this is null, the last one use(we don't check size of this array and needs at least 16 of float)
	 */
	public void draw(final int tex_id, final float[] tex_matrix) {
		GLES20.glUseProgram(program);
		if (tex_matrix != null) {
			GLES20.glUniformMatrix4fv(texMatrixLoc, 1, false, tex_matrix, 0);
		}
		GLES20.glUniformMatrix4fv(mVPMatrixLoc, 1, false, MVP_MATRIX, 0);
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, tex_id);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, VERTEX_NUM);
		GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
		GLES20.glUseProgram(0);
	}

	/**
	 * Sets model/view/projection transform matrix
	 * @param matrix
	 * @param offset
	 */
	public void setMatrix(final float[] matrix, final int offset) {
		if ((matrix != null) && (matrix.length >= offset + 16)) {
			System.arraycopy(matrix, offset, MVP_MATRIX, 0, 16);
		} else {
			Matrix.setIdentityM(MVP_MATRIX, 0);
		}
	}
	/**
	 * create external texture
	 * @return texture ID
	 */
	public static int initTex() {
		final int[] tex = new int[1];
		GLES20.glGenTextures(1, tex, 0);
		GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, tex[0]);
		GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
		GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
		return tex[0];
	}

	/**
	 * delete specific texture
	 */
	public static void deleteTex(final int hTex) {
		final int[] tex = new int[] {hTex};
		GLES20.glDeleteTextures(1, tex, 0);
	}

	/**
	 * load, compile and link shader
	 * @param vss source of vertex shader
	 * @param fss source of fragment shader
	 * @return
	 */
	public static int loadShader(final String vss, final String fss) {
		int vs = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
		GLES20.glShaderSource(vs, vss);
		GLES20.glCompileShader(vs);
		final int[] compiled = new int[1];
		GLES20.glGetShaderiv(vs, GLES20.GL_COMPILE_STATUS, compiled, 0);
		if (compiled[0] == 0) {
			GLES20.glDeleteShader(vs);
			vs = 0;
		}
		int fs = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
		GLES20.glShaderSource(fs, fss);
		GLES20.glCompileShader(fs);
		GLES20.glGetShaderiv(fs, GLES20.GL_COMPILE_STATUS, compiled, 0);
		if (compiled[0] == 0) {
			GLES20.glDeleteShader(fs);
			fs = 0;
		}
		final int program = GLES20.glCreateProgram();
		GLES20.glAttachShader(program, vs);
		GLES20.glAttachShader(program, fs);
		GLES20.glLinkProgram(program);
		return program;
	}
}
