package life.centaurs.sunlife.video.render.encoder;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaRecorder;

import java.io.IOException;
import java.nio.ByteBuffer;

public class MediaAudioEncoder extends MediaEncoder {
	private static final String MIME_TYPE = "audio/mp4a-latm";
    private static final int SAMPLE_RATE = 44100;	// 44.1[KHz] is only setting guaranteed to be available on all devices.
    private static final int BIT_RATE = 64000;
	public static final int SAMPLES_PER_FRAME = 1024;	// AAC, bytes/frame/channel
	public static final int FRAMES_PER_BUFFER = 25; 	// AAC, frame/buffer/sec

    private AudioThread audioThread = null;

	public MediaAudioEncoder(final MediaMuxerWrapper muxer, final MediaEncoderListener listener) {
		super(muxer, listener);
	}

	@Override
	protected void prepare() throws IOException {
        trackIndex = -1;
        muxerIsRunning = encoderReceivedEndOfStream = false;
        final MediaCodecInfo audioCodecInfo = selectAudioCodec(MIME_TYPE);
        if (audioCodecInfo == null) {
            return;
        }
        final MediaFormat audioFormat = MediaFormat.createAudioFormat(MIME_TYPE, SAMPLE_RATE, 1);
		audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
		audioFormat.setInteger(MediaFormat.KEY_CHANNEL_MASK, AudioFormat.CHANNEL_IN_MONO);
		audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
		audioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
        mediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
        mediaCodec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mediaCodec.start();
        if (MEDIA_ENCODER_LISTENER != null) {
        	try {
        		MEDIA_ENCODER_LISTENER.onPrepared(this);
        	} catch (final Exception e) {
        	}
        }
	}

    @Override
	protected void startRecording() {
		super.startRecording();
		if (audioThread == null) {
	        audioThread = new AudioThread();
			audioThread.start();
		}
	}

	@Override
    protected void release() {
		audioThread = null;
		super.release();
    }

	private static final int[] AUDIO_SOURCES = new int[] {
		MediaRecorder.AudioSource.MIC,
		MediaRecorder.AudioSource.DEFAULT,
		MediaRecorder.AudioSource.CAMCORDER,
		MediaRecorder.AudioSource.VOICE_COMMUNICATION,
		MediaRecorder.AudioSource.VOICE_RECOGNITION,
	};

	/**
	 * Thread to capture audio data from internal mic as uncompressed 16bit PCM data
	 * and write them to the MediaCodec encoder
	 */
    private class AudioThread extends Thread {
    	@Override
    	public void run() {
    		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
    		try {
				final int min_buffer_size = AudioRecord.getMinBufferSize(
					SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
					AudioFormat.ENCODING_PCM_16BIT);
				int buffer_size = SAMPLES_PER_FRAME * FRAMES_PER_BUFFER;
				if (buffer_size < min_buffer_size)
					buffer_size = ((min_buffer_size / SAMPLES_PER_FRAME) + 1) * SAMPLES_PER_FRAME * 2;

				AudioRecord audioRecord = null;
				for (final int source : AUDIO_SOURCES) {
					try {
						audioRecord = new AudioRecord(
							source, SAMPLE_RATE,
							AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, buffer_size);
	    	            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED)
	    	            	audioRecord = null;
					} catch (final Exception e) {
						audioRecord = null;
					}
					if (audioRecord != null) break;
				}
				if (audioRecord != null) {
		            try {
						if (encoderIsCapturingNow) {
							final ByteBuffer buf = ByteBuffer.allocateDirect(SAMPLES_PER_FRAME);
			                int readBytes;
			                audioRecord.startRecording();
			                try {
					    		for (; encoderIsCapturingNow && !requestStopCapturing && !encoderReceivedEndOfStream;) {
									buf.clear();
					    			readBytes = audioRecord.read(buf, SAMPLES_PER_FRAME);
					    			if (readBytes > 0) {
										buf.position(readBytes);
										buf.flip();
					    				encode(buf, readBytes, getPTSUs());
					    				frameAvailableSoon();
					    			}
					    		}
			    				frameAvailableSoon();
			                } finally {
			                	audioRecord.stop();
			                }
		            	}
		            } finally {
		            	audioRecord.release();
		            }
				} else {
				}
    		} catch (final Exception e) {
    		}
    	}
    }

    /**
     * select the first codec that match a specific MIME type
     * @param mimeType
     * @return
     */
    private static final MediaCodecInfo selectAudioCodec(final String mimeType) {
    	MediaCodecInfo result = null;
        final int numCodecs = MediaCodecList.getCodecCount();
LOOP:	for (int i = 0; i < numCodecs; i++) {
        	final MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {
                continue;
            }
            final String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                	if (result == null) {
                		result = codecInfo;
               			break LOOP;
                	}
                }
            }
        }
   		return result;
    }
}
