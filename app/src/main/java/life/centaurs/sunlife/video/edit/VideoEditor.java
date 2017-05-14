package life.centaurs.sunlife.video.edit;


import android.content.Context;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

/**
 * VideoEditor
 * The purpose of this class is video editing
 */
public final class VideoEditor{
    private FFmpeg ffmpeg;

    public interface OnFfmpegSuccessListener{
        void onSuccess();
    }

    public VideoEditor(Context context){
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
        }
    }

    public void execFFmpegBinary(final String[] command, final OnFfmpegSuccessListener onFfmpegSuccessListener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ffmpeg.execute(command, new ExecuteBinaryResponseHandler() {
                        @Override
                        public void onFailure(String s) {
                        }
                        @Override
                        public void onSuccess(String s) {
                            if (onFfmpegSuccessListener != null){
                                onFfmpegSuccessListener.onSuccess();
                            }
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
        }).start();
    }
}
