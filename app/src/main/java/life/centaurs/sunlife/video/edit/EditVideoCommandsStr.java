package life.centaurs.sunlife.video.edit;


import java.io.File;

import life.centaurs.sunlife.video.render.display.CameraActivity;

import static life.centaurs.sunlife.video.render.constants.DisplayConstants.SCREENSHOTS_NUMBER;
import static life.centaurs.sunlife.video.render.encoder.MediaVideoEncoder.getFrameRate;

public final class EditVideoCommandsStr {
    private final static int FADE_FRAMES_NUMBER = 5;
    //public final static String ADD_WATERMARK_FFMPEG_COMMAND = "-y -i /storage/sdcard0/t/output.mp4 -i /storage/sdcard0/t/water_log.png -filter_complex [0:v][1:v]overlay=5:(main_h-overlay_h)-5[out] -map [out] -map 0:a -vcodec h264 -pix_fmt yuv420p /storage/sdcard0/t/overlayvideo.mp4";

    //public final static String[] getMakeVideoScreenshotsCommand(File videoFile
    //        , StringBuilder absoluteScreenshotFilePathStringBuilder, int framesPerSecond, int videoWidth, int videoHeight){
    //    StringBuilder commandStringBuilder = new StringBuilder();
    //    commandStringBuilder.append("-i ").append(videoFile.getAbsolutePath()).append(" -filter:v fps=")
    //            .append(framesPerSecond).append(" -s ").append(videoWidth).append("x").append(videoHeight)
    //            .append(" ").append(absoluteScreenshotFilePathStringBuilder);
    //    String[] command = commandStringBuilder.toString().split(" ");
    //    return command;
    //}

    public final static String[] getMakeVideoScreenshotsCommand(File videoFile
            , StringBuilder absoluteScreenshotFilePathStringBuilder, int framesPerSecond, int videoWidth, int videoHeight){
        StringBuilder commandStringBuilder = new StringBuilder();
        commandStringBuilder.append("-i ").append(videoFile.getAbsolutePath()).append(" -ss ")
                .append("00:00:00.000").append(" -filter:v fps=")
                .append(framesPerSecond).append(" -s ").append(videoWidth).append("x").append(videoHeight)
                .append(" -vframes ").append(SCREENSHOTS_NUMBER).append(" ").append(absoluteScreenshotFilePathStringBuilder);
        String[] command = commandStringBuilder.toString().split(" ");
        return command;
    }

    public final static String[] getMakePhotoScreenshotsCommand(File photoFile, StringBuilder absoluteScreenshotFilePathStringBuilder, int videoWidth, int videoHeight){
        StringBuilder commandStringBuilder = new StringBuilder();
        commandStringBuilder.append("-i ").append(photoFile.getAbsolutePath()).append(" -vf scale=")
                .append(videoWidth).append(":").append(videoHeight).append(" ")
                .append(absoluteScreenshotFilePathStringBuilder);
        String[] command = commandStringBuilder.toString().split(" ");
        return command;
    }

    public final static String[] getConvertPhotoToVideoCommand(File photoFile){
        StringBuilder commandStringBuilder = new StringBuilder();
        commandStringBuilder.append("-framerate 25 -i ").append(photoFile.getAbsolutePath())
                .append(" -c:v libx264 -pix_fmt yuv420p -crf 23 ").append(photoFile.getAbsolutePath()
                .concat(CameraActivity.getVideoExtension().getExtensionStr()));
        String[] command = commandStringBuilder.toString().split(" ");
        return command;
    }

    public final static String[] getMakeFullVideoCommand(File chunkListFile, File outputFullVideoFile){
        StringBuilder commandStringBuilder = new StringBuilder();
        commandStringBuilder.append("-f concat -safe 0 -i ").append(chunkListFile.getAbsolutePath())
                .append(" -c:v copy -c:a copy ").append(outputFullVideoFile.getAbsolutePath());
        String[] command = commandStringBuilder.toString().split(" ");
        return command;
    }

    public final static String[] getAddAudioCommand(File fullVideoFile, File audioTrackFile, File outputFullVideoFile){
        StringBuilder commandStringBuilder = new StringBuilder();
        commandStringBuilder.append("-i ").append(fullVideoFile.getAbsolutePath())
                .append(" -i ").append(audioTrackFile.getAbsolutePath())
                .append(" -c:v copy -c:a aac -strict experimental -shortest ")
                .append(outputFullVideoFile.getAbsolutePath());
        String[] command = commandStringBuilder.toString().split(" ");
        return command;
    }

    public final static String[] getFadeOutCommand(File videoChunk, File outputVideoChunk, int chunkDuration){
        StringBuilder commandStringBuilder = new StringBuilder();
        int firstFrame = (chunkDuration * getFrameRate()) - FADE_FRAMES_NUMBER;
        commandStringBuilder.append("-i ").append(videoChunk.getAbsolutePath()).append(" -y -vf fade=out:")
                .append(firstFrame).append(FADE_FRAMES_NUMBER).append(" ").append(outputVideoChunk.getAbsolutePath());
        String[] command = commandStringBuilder.toString().split(" ");
        return command;
    }

    public final static String[] getFadeInCommand(File videoChunk, File outputVideoChunk, int chunkDuration){
        StringBuilder commandStringBuilder = new StringBuilder();
        commandStringBuilder.append("-i ").append(videoChunk.getAbsolutePath()).append(" -y -vf fade=in:")
                .append(0).append(FADE_FRAMES_NUMBER).append(" ").append(outputVideoChunk.getAbsolutePath());
        String[] command = commandStringBuilder.toString().split(" ");
        return command;
    }
}