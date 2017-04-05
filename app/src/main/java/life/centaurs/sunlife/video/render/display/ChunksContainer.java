package life.centaurs.sunlife.video.render.display;

import android.content.Context;
import android.os.Handler;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import life.centaurs.sunlife.video.edit.VideoEditor;
import life.centaurs.sunlife.video.render.enums.MediaExtensionEnum;

import static life.centaurs.sunlife.video.edit.EditVideoCommandsStr.getMakePhotoScreenshotsCommand;
import static life.centaurs.sunlife.video.edit.EditVideoCommandsStr.getMakeVideoScreenshotsCommand;
import static life.centaurs.sunlife.video.render.constants.DisplayConstants.DOT_STRING;
import static life.centaurs.sunlife.video.render.constants.DisplayConstants.EMPTY_STRING;
import static life.centaurs.sunlife.video.render.constants.DisplayConstants.FFMPEG_COUNTER;
import static life.centaurs.sunlife.video.render.constants.DisplayConstants.NAME_SEPARATOR;
import static life.centaurs.sunlife.video.render.constants.DisplayConstants.SCREENSHOTS_FRAMES_PER_SECOND;
import static life.centaurs.sunlife.video.render.constants.DisplayConstants.SCREENSHOT_HEIGHT;
import static life.centaurs.sunlife.video.render.constants.DisplayConstants.SCREENSHOT_NAME_START_COUNTER;
import static life.centaurs.sunlife.video.render.constants.DisplayConstants.SCREENSHOT_WIDTH;

public class ChunksContainer {
    private ArrayList<File> chunksFiles;
    private Map<String, ArrayList<File>> chunksScreenshots;//file name as a key
    private Handler handler;
    private VideoEditor videoEditor;
    private Context context;
    private OnScreenshotEndCreationListener onScreenshotEndCreationListener;
    private String currentTime;
    private String folderPath;
    private File chunkFile;
    public static boolean screenshotNumberNameIsTaken = false;

    public interface OnScreenshotEndCreationListener{
        void onEndScreenshotCreation();
    }

    public ChunksContainer(Context context, OnScreenshotEndCreationListener onScreenshotEndCreationListener){
        this.onScreenshotEndCreationListener = onScreenshotEndCreationListener;
        this.context = context;
        videoEditor = new VideoEditor(context, onFfmpegSuccessListener);
        chunksFiles = new ArrayList<>();
        chunksScreenshots = new HashMap<>();
        handler = new Handler();
    }

    public ArrayList<File> getChunksFiles() {
        return chunksFiles;
    }

    public void setChunkFile(File chunkFile) {
        this.chunkFile = chunkFile;
        this.chunksFiles.add(chunkFile);
        setChunksScreenshots(chunkFile);
    }

    public Map<String, ArrayList<File>> getChunkScreenshots() {
        return chunksScreenshots;
    }

    public ArrayList<File> getChunkScreenshots(File chunkFile) {
        return chunksScreenshots.get(chunkFile.getName());
    }

    private void setChunksScreenshots(final File chunkFile){
        new Thread(new Runnable() {
            @Override
            public void run() {

                int i = chunkFile.getName().lastIndexOf(DOT_STRING);
                String currentFileExtension = chunkFile.getName().substring(i);

                int width;
                int height;
                switch (CameraFragment.getVideoOrientationEnum()){
                    case LANDSCAPE:
                    case LANDSCAPE_REVERSE:
                        width = SCREENSHOT_HEIGHT;
                        height = SCREENSHOT_WIDTH;
                        break;
                    default:
                        width = SCREENSHOT_WIDTH;
                        height = SCREENSHOT_HEIGHT;
                }

                if(!screenshotNumberNameIsTaken) {
                    currentTime = "" + System.currentTimeMillis();
                    screenshotNumberNameIsTaken = true;
                }

                folderPath = chunkFile.getAbsolutePath().replace(chunkFile.getName(), EMPTY_STRING);

                StringBuilder absoluteScreenshotFilePathStringBuilder = new StringBuilder();
                absoluteScreenshotFilePathStringBuilder.append(folderPath).append(currentTime)
                        .append(NAME_SEPARATOR.concat(FFMPEG_COUNTER)).append(MediaExtensionEnum.JPG.getExtensionStr());

                if (currentFileExtension.equalsIgnoreCase(MediaExtensionEnum.MP4.getExtensionStr())){
                    videoEditor.execFFmpegBinary(getMakeVideoScreenshotsCommand(chunkFile
                            , absoluteScreenshotFilePathStringBuilder, SCREENSHOTS_FRAMES_PER_SECOND, width, height));
                } else if (currentFileExtension.equalsIgnoreCase(MediaExtensionEnum.JPG.getExtensionStr())){
                    videoEditor.execFFmpegBinary(getMakePhotoScreenshotsCommand(chunkFile
                            , absoluteScreenshotFilePathStringBuilder, width, height));
                }
            }

        }).start();

    }

    private VideoEditor.OnFfmpegSuccessListener onFfmpegSuccessListener = new VideoEditor.OnFfmpegSuccessListener() {
        @Override
        public void onSuccess() {
            int fileCounter = SCREENSHOT_NAME_START_COUNTER;
            File screenshotFile = new File(folderPath.concat(currentTime).concat(NAME_SEPARATOR) + fileCounter
                    + MediaExtensionEnum.JPG.getExtensionStr());
            while(screenshotFile.exists()){
                if (!chunksScreenshots.containsKey(chunkFile.getName())){
                    chunksScreenshots.put(chunkFile.getName(), new ArrayList<File>());
                }
                chunksScreenshots.get(chunkFile.getName()).add(screenshotFile);
                fileCounter++;
                screenshotFile = new File(folderPath + currentTime + NAME_SEPARATOR + fileCounter
                        + MediaExtensionEnum.JPG.getExtensionStr());
            }
            if (chunksScreenshots.get(chunkFile.getName())== null){
                System.out.println("" + fileCounter + "***************************************************************************" + folderPath);
                System.out.println("" + chunksScreenshots.size() + "************************************* null folder path im chunk screenshots container ****************************************" + screenshotFile.getAbsolutePath());
                System.out.println("*******************************************************************************" + chunkFile);
            }
            onScreenshotEndCreationListener.onEndScreenshotCreation();
        }
    };
}