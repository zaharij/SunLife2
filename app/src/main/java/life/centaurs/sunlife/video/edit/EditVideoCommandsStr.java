package life.centaurs.sunlife.video.edit;


import java.io.File;

public class EditVideoCommandsStr {

    public final static String ADD_WATERMARK_FFMPEG_COMMAND = "-y -i /storage/sdcard0/t/output.mp4 -i /storage/sdcard0/t/water_log.png -filter_complex [0:v][1:v]overlay=5:(main_h-overlay_h)-5[out] -map [out] -map 0:a -vcodec h264 -pix_fmt yuv420p /storage/sdcard0/t/overlayvideo.mp4";

    public static String[] getMakeVideoScreenshotsCommand(File videoFile, StringBuilder absoluteScreenshotFilePathStringBuilder, int framesPerSecond, int videoWidth, int videoHeight){
        StringBuilder commandStringBuilder = new StringBuilder();
        commandStringBuilder.append("-i ").append(videoFile.getAbsolutePath()).append(" -vf fps=")
                .append(framesPerSecond).append(" -s ").append(videoWidth).append("x").append(videoHeight)
                .append(" ").append(absoluteScreenshotFilePathStringBuilder);
        String[] command = commandStringBuilder.toString().split(" ");
        return command;
    }

    public static String[] getMakePhotoScreenshotsCommand(File photoFile, StringBuilder absoluteScreenshotFilePathStringBuilder, int videoWidth, int videoHeight){
        StringBuilder commandStringBuilder = new StringBuilder();
        commandStringBuilder.append("-i ").append(photoFile.getAbsolutePath()).append(" -vf scale=")
                .append(videoWidth).append(":").append(videoHeight).append(" ")
                .append(absoluteScreenshotFilePathStringBuilder);
        String[] command = commandStringBuilder.toString().split(" ");
        return command;
    }
}
