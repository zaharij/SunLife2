package life.centaurs.sunlife.video.edit;


import android.content.Context;
import android.os.Handler;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

/**
 * VideoEditor
 * The purpose of this class is video editing
 */
public final class VideoEditor {
    private FFmpeg ffmpeg;
    private Handler handler = new Handler();
    private OnFfmpegSuccessListener onFfmpegSuccessListener;

    public interface OnFfmpegSuccessListener{
        void onSuccess();
    }

    public VideoEditor (Context context, OnFfmpegSuccessListener onFfmpegSuccessListener){
        this.onFfmpegSuccessListener = onFfmpegSuccessListener;
        this.ffmpeg = FFmpeg.getInstance(context);
        loadFFmpegBinary();
    }

    private void loadFFmpegBinary() {
        try {
            ffmpeg.loadBinary(new LoadBinaryResponseHandler() {

                @Override
                public void onStart() {}

                @Override
                public void onFailure() {}

                @Override
                public void onSuccess() {
                }

                @Override
                public void onFinish() {}
            });
        } catch (FFmpegNotSupportedException e) {
            // Handle if FFmpeg is not supported by device
        }
    }

    public void execFFmpegBinary(final String[] command) {
        try {
            ffmpeg.execute(command, new ExecuteBinaryResponseHandler() {
                @Override
                public void onFailure(String s) {
                }
                @Override
                public void onSuccess(String s) {
                    onFfmpegSuccessListener.onSuccess();
                }
                @Override
                public void onProgress(String s) {
                }
                @Override
                public void onStart() {
                }
                @Override
                public void onFinish() {
                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
        }
    }
}
