package life.centaurs.sunlife.video.render.encoder;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;
import android.text.TextUtils;

import org.joda.time.DateTime;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import life.centaurs.sunlife.video.render.display.CameraActivity;
import life.centaurs.sunlife.video.render.display.CameraFragment;

import static life.centaurs.sunlife.video.render.constants.DisplayConstants.MUXER_ALREADY_STARTED_MESSAGE;
import static life.centaurs.sunlife.video.render.constants.DisplayConstants.NO_PERMISSION_TO_WRITE_EXTERNAL_STORAGE;
import static life.centaurs.sunlife.video.render.constants.DisplayConstants.UNSUPPORTED_ENCODER;
import static life.centaurs.sunlife.video.render.constants.DisplayConstants.VIDEO_ENCODER_ALREADY_ADDED;
import static life.centaurs.sunlife.video.render.display.CameraFragment.currentFile;

public class MediaMuxerWrapper {
	private final static String DATE_FORMAT_STR = "yyyy-MM-dd_HH-mm-ss.SSS";
	private final static String FOLDER_NAME = "SunLifeMedia";
	private final static String VIDEO_NAME_PREFIX = "SL_Video_";

	public static volatile String outputPath;
	private final MediaMuxer mediaMuxer;
	private int encoderCount, startedCount;
	private boolean isStarted;
	private MediaEncoder videoEncoder, audioEncoder;

	/**
	 * Constructor
	 * @param ext extension of output file
	 * @throws IOException
	 */
	public MediaMuxerWrapper(String ext) throws IOException {
		if (TextUtils.isEmpty(ext)) ext = CameraActivity.getVideoExtension().getExtensionStr();
		try {
			currentFile = getCaptureFile(Environment.DIRECTORY_MOVIES, ext, VIDEO_NAME_PREFIX);
			outputPath = currentFile.toString();
		} catch (final NullPointerException e) {
			throw new RuntimeException(NO_PERMISSION_TO_WRITE_EXTERNAL_STORAGE);
		}
		mediaMuxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
		encoderCount = startedCount = 0;
		isStarted = false;
	}

	public static String getOutputPath() {
		return outputPath;
	}

	public void prepareVideo() throws IOException {
		if (videoEncoder != null)
			videoEncoder.prepare();
	}

	public void prepareAudio() throws IOException {
		if (audioEncoder != null)
			audioEncoder.prepare();
	}

	public void startRecording() {
		if (videoEncoder != null)
			videoEncoder.startRecording();
		if (audioEncoder != null)
			audioEncoder.startRecording();
	}

	public void stopRecording() {
		if (videoEncoder != null)
			videoEncoder.stopRecording();
		videoEncoder = null;
		if (audioEncoder != null)
			audioEncoder.stopRecording();
		audioEncoder = null;
	}

	public synchronized boolean isStarted() {
		return isStarted;
	}

	/**
	 * assign encoder to this calss. this is called from encoder.
	 * @param encoder instance of MediaVideoEncoder or MediaAudioEncoder
	 */
	void addEncoder(final MediaEncoder encoder) {
		if (encoder instanceof MediaVideoEncoder) {
			if (videoEncoder != null)
				throw new IllegalArgumentException(VIDEO_ENCODER_ALREADY_ADDED);
			videoEncoder = encoder;
		} else if (encoder instanceof MediaAudioEncoder) {
			if (audioEncoder != null)
				throw new IllegalArgumentException(VIDEO_ENCODER_ALREADY_ADDED);
			audioEncoder = encoder;
		} else
			throw new IllegalArgumentException(UNSUPPORTED_ENCODER);
		encoderCount = (videoEncoder != null ? 1 : 0) + (audioEncoder != null ? 1 : 0);
	}

	/**
	 * request start recording from encoder
	 * @return true when muxer is ready to write
	 */
	synchronized boolean start() {
		startedCount++;
		if ((encoderCount > 0) && (startedCount == encoderCount)) {
			mediaMuxer.setOrientationHint(CameraFragment.getVideoOrientationEnum().getDegrees());
			mediaMuxer.start();
			isStarted = true;
			notifyAll();
		}
		return isStarted;
	}

	/**
	 * request stop recording from encoder when encoder received EOS
	*/
	synchronized void stop() {
		startedCount--;
		if ((encoderCount > 0) && (startedCount <= 0)) {
			mediaMuxer.stop();
			mediaMuxer.release();
			isStarted = false;
		}
	}

	/**
	 * assign encoder to muxer
	 * @param format
	 * @return minus value indicate error
	 */
	/*package*/ synchronized int addTrack(final MediaFormat format) {
		if (isStarted)
			throw new IllegalStateException(MUXER_ALREADY_STARTED_MESSAGE);
		final int trackIx = mediaMuxer.addTrack(format);
		return trackIx;
	}

	/**
	 * write encoded data to muxer
	 * @param trackIndex
	 * @param byteBuf
	 * @param bufferInfo
	 */
	synchronized void writeSampleData(final int trackIndex, final ByteBuffer byteBuf, final MediaCodec.BufferInfo bufferInfo) {
		if (startedCount > 0)
			mediaMuxer.writeSampleData(trackIndex, byteBuf, bufferInfo);
	}

    /**
     * generate output file
     * @param type Environment.DIRECTORY_MOVIES / Environment.DIRECTORY_DCIM etc.
     * @param ext .mp4(.m4a for audio) or .png
     * @return return null when this app has no writing permission to external storage.
     */
    public static final File getCaptureFile(final String type, final String ext, String namePrefix) {
		final File dir = new File(Environment.getExternalStoragePublicDirectory(type), FOLDER_NAME);
		dir.mkdirs();
        if (dir.canWrite()) {
        	return new File(dir, namePrefix.concat(getDateTimeString()).concat(ext));
        }
    	return null;
    }

	/**
	 * returns current DateTime (String)
	 * @return
	 */
	private static String getDateTimeString(){
		return new DateTime(DateTime.now()).toString(DATE_FORMAT_STR);
	}
}
