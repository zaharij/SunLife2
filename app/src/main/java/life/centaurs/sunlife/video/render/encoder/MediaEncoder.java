package life.centaurs.sunlife.video.render.encoder;

import android.media.MediaCodec;
import android.media.MediaFormat;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

import static life.centaurs.sunlife.video.render.constants.DisplayConstants.FORMAT_CHANGED_TWICE_MESSAGE;
import static life.centaurs.sunlife.video.render.constants.DisplayConstants.IS_NULL_MESSAGE;
import static life.centaurs.sunlife.video.render.constants.DisplayConstants.MUXER_HAS_NOT_STARTED_MESSAGE;
import static life.centaurs.sunlife.video.render.display.CameraFragment.currentFile;
import static life.centaurs.sunlife.video.render.display.CameraNavigationFragment.chunksManager;

public abstract class MediaEncoder implements Runnable {
	protected static final int TIMEOUT_USEC = 10000;

	public interface MediaEncoderListener {
		public void onPrepared(MediaEncoder encoder);
		public void onStopped(MediaEncoder encoder);
	}
	private int frameDataWillBeAvaliableSoon;
	private MediaCodec.BufferInfo bufferInfo;
	private long previousPresentationTimeUsForWritting = 0;

	protected final Object mSync = new Object();
    protected volatile boolean encoderIsCapturingNow;
    protected volatile boolean requestStopCapturing;
    protected boolean encoderReceivedEndOfStream;
    protected boolean muxerIsRunning;
    protected int trackIndex;
    protected MediaCodec mediaCodec;
    protected final WeakReference<MediaMuxerWrapper> WEAK_REFERENCE_MEDIA_MUXER_WRAPPER;
    protected final MediaEncoderListener MEDIA_ENCODER_LISTENER;

    public MediaEncoder(final MediaMuxerWrapper muxer, final MediaEncoderListener listener) {
		WEAK_REFERENCE_MEDIA_MUXER_WRAPPER = new WeakReference<MediaMuxerWrapper>(muxer);
		muxer.addEncoder(this);
		MEDIA_ENCODER_LISTENER = listener;
        synchronized (mSync) {
            bufferInfo = new MediaCodec.BufferInfo();
            new Thread(this, getClass().getSimpleName()).start();
            try {
            	mSync.wait();
            } catch (final InterruptedException e) {
            }
        }
	}

    public String getOutputPath() {
    	final MediaMuxerWrapper muxer = WEAK_REFERENCE_MEDIA_MUXER_WRAPPER.get();
    	return muxer != null ? muxer.getOutputPath() : null;
    }

    /**
     * the method to indicate frame data is soon available or already available
     * @return return true if encoder is ready to encod.
     */
    public boolean frameAvailableSoon() {
        synchronized (mSync) {
            if (!encoderIsCapturingNow || requestStopCapturing) {
                return false;
            }
            frameDataWillBeAvaliableSoon++;
            mSync.notifyAll();
        }
        return true;
    }

    /**
     * encoding loop on private thread
     */
	@Override
	public void run() {
        synchronized (mSync) {
            requestStopCapturing = false;
    		frameDataWillBeAvaliableSoon = 0;
            mSync.notify();
        }
        final boolean isRunning = true;
        boolean localRequestStop;
        boolean localRequestDrain;
        while (isRunning) {
        	synchronized (mSync) {
        		localRequestStop = requestStopCapturing;
        		localRequestDrain = (frameDataWillBeAvaliableSoon > 0);
        		if (localRequestDrain)
        			frameDataWillBeAvaliableSoon--;
        	}
	        if (localRequestStop) {
	           	drain();
	           	signalEndOfInputStream();
	           	drain();
	           	release();
				chunksManager.setChunkFile(currentFile);
	           	break;
	        }
	        if (localRequestDrain) {
	        	drain();
	        } else {
	        	synchronized (mSync) {
		        	try {
						mSync.wait();
					} catch (final InterruptedException e) {
						break;
					}
	        	}
        	}
        }
        synchronized (mSync) {
        	requestStopCapturing = true;
            encoderIsCapturingNow = false;
        }
	}

	/*
    * prepareing method for each sub class
    * @throws IOException
    */
	abstract void prepare() throws IOException;

	void startRecording() {
		synchronized (mSync) {
			encoderIsCapturingNow = true;
			requestStopCapturing = false;
			mSync.notifyAll();
		}
	}

   /**
    * the method to request stop encoding
    */
	void stopRecording() {
		synchronized (mSync) {
			if (!encoderIsCapturingNow || requestStopCapturing) {
				return;
			}
			requestStopCapturing = true;
			mSync.notifyAll();
		}
	}

    /**
     * Release all releated objects
     */
    protected void release() {
		try {
			MEDIA_ENCODER_LISTENER.onStopped(this);
		} catch (final Exception e) {
		}
		encoderIsCapturingNow = false;
        if (mediaCodec != null) {
			try {
	            mediaCodec.stop();
	            mediaCodec.release();
	            mediaCodec = null;
			} catch (final Exception e) {
			}
        }
        if (muxerIsRunning) {
       		final MediaMuxerWrapper muxer = WEAK_REFERENCE_MEDIA_MUXER_WRAPPER != null ? WEAK_REFERENCE_MEDIA_MUXER_WRAPPER.get() : null;
       		if (muxer != null) {
       			try {
           			muxer.stop();
    			} catch (final Exception e) {
    			}
       		}
        }
        bufferInfo = null;
    }

    protected void signalEndOfInputStream() {
        encode(null, 0, getPTSUs());
	}

    /**
     * Method to set byte array to the MediaCodec encoder
     * @param buffer
     * @param length　length of byte array, zero means EOS.
     * @param presentationTimeUs
     */
    protected void encode(final ByteBuffer buffer, final int length, final long presentationTimeUs) {
    	if (!encoderIsCapturingNow) return;
        final ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
        while (encoderIsCapturingNow) {
	        final int inputBufferIndex = mediaCodec.dequeueInputBuffer(TIMEOUT_USEC);
	        if (inputBufferIndex >= 0) {
	            final ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
	            inputBuffer.clear();
	            if (buffer != null) {
	            	inputBuffer.put(buffer);
	            }
	            if (length <= 0) {
	            	encoderReceivedEndOfStream = true;
	            	mediaCodec.queueInputBuffer(inputBufferIndex, 0, 0,
	            		presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
		            break;
	            } else {
	            	mediaCodec.queueInputBuffer(inputBufferIndex, 0, length,
	            		presentationTimeUs, 0);
	            }
	            break;
	        } else if (inputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
	        }
        }
    }

    /**
     * drain encoded data and write them to muxer
     */
    protected void drain() {
    	if (mediaCodec == null) return;
        ByteBuffer[] encoderOutputBuffers = mediaCodec.getOutputBuffers();
        int encoderStatus, count = 0;
        final MediaMuxerWrapper muxer = WEAK_REFERENCE_MEDIA_MUXER_WRAPPER.get();
        if (muxer == null) {
        	return;
        }
LOOP:	while (encoderIsCapturingNow) {
            encoderStatus = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!encoderReceivedEndOfStream) {
                	if (++count > 5)
                		break LOOP;
                }
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                encoderOutputBuffers = mediaCodec.getOutputBuffers();
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {

                if (muxerIsRunning) {
                    throw new RuntimeException(FORMAT_CHANGED_TWICE_MESSAGE);
                }
                final MediaFormat format = mediaCodec.getOutputFormat();
               	trackIndex = muxer.addTrack(format);
               	muxerIsRunning = true;
               	if (!muxer.start()) {
               		synchronized (muxer) {
	               		while (!muxer.isStarted())
						try {
							muxer.wait(100);
						} catch (final InterruptedException e) {
							break LOOP;
						}
               		}
               	}
            } else if (encoderStatus < 0) {
            } else {
                final ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                if (encodedData == null) {
                    throw new RuntimeException(encoderStatus + IS_NULL_MESSAGE);
                }
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
					bufferInfo.size = 0;
                }
                if (bufferInfo.size != 0) {
            		count = 0;
                    if (!muxerIsRunning) {
                        throw new RuntimeException(MUXER_HAS_NOT_STARTED_MESSAGE);
                    }
                   	bufferInfo.presentationTimeUs = getPTSUs();
                   	muxer.writeSampleData(trackIndex, encodedData, bufferInfo);
					previousPresentationTimeUsForWritting = bufferInfo.presentationTimeUs;
                }
                mediaCodec.releaseOutputBuffer(encoderStatus, false);
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
               		encoderIsCapturingNow = false;
                    break;
                }
            }
        }
    }

	/**
	 * get next encoding presentationTimeUs
	 * @return
	 */
    protected long getPTSUs() {
		long result = System.nanoTime() / 1000L;
		if (result < previousPresentationTimeUsForWritting)
			result = (previousPresentationTimeUsForWritting - result) + result;
		return result;
    }

}
