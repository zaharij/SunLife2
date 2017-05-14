package life.centaurs.sunlife.video.render.encoder;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.opengl.EGLContext;
import android.view.Surface;

import java.io.IOException;

import life.centaurs.sunlife.video.render.glutils.RenderHandler;

public class MediaVideoEncoder extends MediaEncoder {
	private final static String TAG = "MVE";
	private static final String MIME_TYPE = "video/avc";
    public static final int FRAME_RATE = 25;
    private static final float BITS_PER_PIXEL = 0.25f;

    private final int width;
    private final int height;
    private RenderHandler renderHandler;
    private Surface surface;

	public MediaVideoEncoder(final MediaMuxerWrapper muxer, final MediaEncoderListener listener, final int width, final int height) {
		super(muxer, listener);
		this.width = width;
		this.height = height;
		renderHandler = RenderHandler.createHandler(TAG);
	}

    public static int getFrameRate() {
        return FRAME_RATE;
    }

    public boolean frameAvailableSoon(final float[] tex_matrix) {
		boolean result;
		if (result = super.frameAvailableSoon())
			renderHandler.draw(tex_matrix);
		return result;
	}

	public boolean frameAvailableSoon(final float[] tex_matrix, final float[] mvp_matrix) {
		boolean result;
		if (result = super.frameAvailableSoon())
			renderHandler.draw(tex_matrix, mvp_matrix);
		return result;
	}

	@Override
	public boolean frameAvailableSoon() {
		boolean result;
		if (result = super.frameAvailableSoon())
			renderHandler.draw(null);
		return result;
	}

	@Override
	protected void prepare() throws IOException {
        trackIndex = -1;
        muxerIsRunning = encoderReceivedEndOfStream = false;

        final MediaCodecInfo videoCodecInfo = selectVideoCodec(MIME_TYPE);
        if (videoCodecInfo == null) {
            return;
        }
        final MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, width, height);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, calcBitRate());
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10);
        mediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
        mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        surface = mediaCodec.createInputSurface();
        mediaCodec.start();
        if (MEDIA_ENCODER_LISTENER != null) {
        	try {
        		MEDIA_ENCODER_LISTENER.onPrepared(this);
        	} catch (final Exception e) {
        	}
        }
	}

	public void setEglContext(final EGLContext shared_context, final int tex_id) {
		renderHandler.setEglContext(shared_context, tex_id, surface, true);
	}

	@Override
    protected void release() {
		if (surface != null) {
			surface.release();
			surface = null;
		}
		if (renderHandler != null) {
			renderHandler.release();
			renderHandler = null;
		}
		super.release();
	}

	private int calcBitRate() {
		final int bitrate = (int)(BITS_PER_PIXEL * FRAME_RATE * width * height);
		return bitrate;
	}

    /**
     * select the first codec that match a specific MIME type
     * @param mimeType
     * @return null if no codec matched
     */
    protected static final MediaCodecInfo selectVideoCodec(final String mimeType) {
        final int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
        	final MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {
                continue;
            }

            final String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
            		final int format = selectColorFormat(codecInfo, mimeType);
                	if (format > 0) {
                		return codecInfo;
                	}
                }
            }
        }
        return null;
    }

    /**
     * select color format available on specific codec and we can use.
     * @return 0 if no colorFormat is matched
     */
    protected static final int selectColorFormat(final MediaCodecInfo codecInfo, final String mimeType) {
    	int result = 0;
    	final MediaCodecInfo.CodecCapabilities caps;
    	try {
    		Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
    		caps = codecInfo.getCapabilitiesForType(mimeType);
    	} finally {
    		Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
    	}
        int colorFormat;
        for (int i = 0; i < caps.colorFormats.length; i++) {
        	colorFormat = caps.colorFormats[i];
            if (isRecognizedViewoFormat(colorFormat)) {
            	if (result == 0)
            		result = colorFormat;
                break;
            }
        }
        return result;
    }

		/**
		 * color formats that we can use in this class
		 */
		protected static int[] recognizedFormats;
		static {
			recognizedFormats = new int[] {
				MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface,
			};
		}

    private static final boolean isRecognizedViewoFormat(final int colorFormat) {
    	final int n = recognizedFormats != null ? recognizedFormats.length : 0;
    	for (int i = 0; i < n; i++) {
    		if (recognizedFormats[i] == colorFormat) {
    			return true;
    		}
    	}
    	return false;
    }

    @Override
    protected void signalEndOfInputStream() {
		mediaCodec.signalEndOfInputStream();
		encoderReceivedEndOfStream = true;
	}

}
