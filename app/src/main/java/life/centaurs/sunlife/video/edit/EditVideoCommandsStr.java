package life.centaurs.sunlife.video.edit;


import java.io.File;

import life.centaurs.sunlife.video.render.display.CameraActivity;
import life.centaurs.sunlife.video.render.enums.OrientationEnum;

import static life.centaurs.sunlife.video.render.constants.DisplayConstants.SCREENSHOTS_NUMBER;
import static life.centaurs.sunlife.video.render.constants.DisplayConstants.SCREENSHOT_HEIGHT;
import static life.centaurs.sunlife.video.render.constants.DisplayConstants.SCREENSHOT_WIDTH;
import static life.centaurs.sunlife.video.render.constants.DisplayConstants.TIME_PHOTO_PROGRESS;
import static life.centaurs.sunlife.video.render.constants.DisplayConstants.VIDEO_SIZE;
import static life.centaurs.sunlife.video.render.display.ChunksManager.TRANSIT_CHUNK_FILE_EXTENSION;
import static life.centaurs.sunlife.video.render.encoder.MediaVideoEncoder.FRAME_RATE;

public final class EditVideoCommandsStr {

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

    public final static String[] getMakePhotoScreenshotsCommand(File photoFile
            , StringBuilder absoluteScreenshotFilePathStringBuilder){
        StringBuilder commandStringBuilder = new StringBuilder();
        commandStringBuilder.append("-i ").append(photoFile.getAbsolutePath()).append(" -vf scale=")
                .append(SCREENSHOT_WIDTH).append(":").append(SCREENSHOT_HEIGHT).append(" ")
                .append(absoluteScreenshotFilePathStringBuilder);
        String[] command = commandStringBuilder.toString().split(" ");
        return command;
    }

    public final static String[] getConvertPhotoToVideoCommand(File photoFile){
        int width, height;
        if (CameraActivity.getOrientationEnum().equals(OrientationEnum.PORTRAIT)
                || CameraActivity.getOrientationEnum().equals(OrientationEnum.PORTRAIT_REVERSE)){
            width = VIDEO_SIZE.getHEIGHT();
            height = VIDEO_SIZE.getWIDTH();
        } else {
            width = VIDEO_SIZE.getWIDTH();
            height = VIDEO_SIZE.getHEIGHT();
        }

        StringBuilder commandStringBuilder = new StringBuilder();
        commandStringBuilder.append("-loop 1 -i ").append(photoFile.getAbsolutePath())
                .append(" -c:v libx264 -t ").append(TIME_PHOTO_PROGRESS).append(" -pix_fmt yuv420p -vf scale=")
                .append(width).append(":").append(height).append(" -r ").append(FRAME_RATE).append(" ")
                .append(photoFile.getAbsolutePath().concat(TRANSIT_CHUNK_FILE_EXTENSION));
        String[] command = commandStringBuilder.toString().split(" ");
        return command;
    }

    public final static String[] getConvertVideoFileToTransitExtensionCommand(File videoFile){
        StringBuilder commandStringBuilder = new StringBuilder();
        commandStringBuilder.append("-i ").append(videoFile.getAbsolutePath())
                .append(" -c copy -bsf h264_mp4toannexb ").append(videoFile.getAbsolutePath()
                .concat(TRANSIT_CHUNK_FILE_EXTENSION));
        String[] command = commandStringBuilder.toString().split(" ");
        return command;
    }

    public final static String[] getRotateVideoFileByItsMetadata(File videoFile, File rotatedVideoFile){
        StringBuilder commandStringBuilder = new StringBuilder();
        commandStringBuilder.append("-i ").append(videoFile.getAbsolutePath())
                .append(" -c:a copy ").append(rotatedVideoFile.getAbsolutePath());
        String[] command = commandStringBuilder.toString().split(" ");
        return command;
    }

    public final static String[] getMakeFullVideoCommand(String chunkListString, File outputFullVideoFile){
        StringBuilder commandStringBuilder = new StringBuilder();
        commandStringBuilder.append("-i concat:").append(chunkListString)
                .append(" -vcodec copy -acodec copy -absf aac_adtstoasc ").append(outputFullVideoFile.getAbsolutePath());
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

    public final static String[] getRotatePhotoCommand(File photoFile, int transposeNumber, File outputPhotoFile){
        StringBuilder commandStringBuilder = new StringBuilder();
        commandStringBuilder.append("-i ").append(photoFile.getAbsolutePath())
                .append(" -filter_complex transpose=").append(transposeNumber)
                .append(" ").append(outputPhotoFile.getAbsolutePath());
        String[] command = commandStringBuilder.toString().split(" ");
        return command;
    }

    String setMetaRot = "-i /storage/sdcard0/Movies/SunLifeMedia/vqr.mp4 -c copy -metadata:s:v:0 rotate=90 /storage/sdcard0/Movies/SunLifeMedia/vqroutput.mp4";
    public final static String[] getSetRotationMetaComand(File inputVideoFile, int rotationDegrees, File outputVideoFile){
        StringBuilder commandStringBuilder = new StringBuilder();
        commandStringBuilder.append("-i ").append(inputVideoFile.getAbsolutePath())
                .append(" -c copy -metadata:s:v:0 rotate=").append(rotationDegrees)
                .append(" ").append(outputVideoFile.getAbsolutePath());
        String[] command = commandStringBuilder.toString().split(" ");
        return command;
    }
}