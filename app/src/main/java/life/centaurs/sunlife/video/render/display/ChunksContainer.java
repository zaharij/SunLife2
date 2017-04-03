package life.centaurs.sunlife.video.render.display;

import android.content.Context;
import android.os.Handler;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import life.centaurs.sunlife.video.edit.VideoEditor;

import static life.centaurs.sunlife.video.edit.EditVideoCommandsStr.getMakePhotoScreenshotsCommand;
import static life.centaurs.sunlife.video.edit.EditVideoCommandsStr.getMakeVideoScreenshotsCommand;
import static life.centaurs.sunlife.video.render.constants.DisplayConstants.SCREENSHOT_HEIGHT;
import static life.centaurs.sunlife.video.render.constants.DisplayConstants.SCREENSHOT_WIDTH;

public class ChunksContainer {
    private ArrayList<File> chunksFiles;
    private Map<String, ArrayList<File>> chunksScreenshots;//file name as a key
    private Handler handler;
    private VideoEditor videoEditor;
    private Context context;

    public ChunksContainer(Context context){
        this.context = context;
        videoEditor = new VideoEditor(context);
        chunksFiles = new ArrayList<>();
        chunksScreenshots = new HashMap<>();
        handler = new Handler();
    }

    public ArrayList<File> getChunksFiles() {
        return chunksFiles;
    }

    public void setChunkFile(File chunkFile) {
        this.chunksFiles.add(chunkFile);
        setChunksScreenshots(chunkFile);
    }

    public ArrayList<File> getChunkScreenshots(File chunkFile) {
        return chunksScreenshots.get(chunkFile.getName());
    }

    private void setChunksScreenshots(final File chunkFile){
        new Thread(new Runnable() {
            @Override
            public void run() {
                int i = chunkFile.getName().lastIndexOf(".");
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

                long currentTime = System.currentTimeMillis();

                String folderPath = chunkFile.getAbsolutePath().replace(chunkFile.getName(), "");

                StringBuilder absoluteScreenshotFilePathStringBuilder = new StringBuilder();
                absoluteScreenshotFilePathStringBuilder.append(folderPath).append(currentTime).append("_%d").append(".jpg");

                if (currentFileExtension.equalsIgnoreCase(".mp4")){
                    videoEditor.execFFmpegBinary(getMakeVideoScreenshotsCommand(chunkFile
                            , absoluteScreenshotFilePathStringBuilder, 2, width, height));
                } else if (currentFileExtension.equalsIgnoreCase(".jpg")){
                    videoEditor.execFFmpegBinary(getMakePhotoScreenshotsCommand(chunkFile
                            , absoluteScreenshotFilePathStringBuilder, width, height));
                }

                int fileCounter = 1;
                File screenshotFile = new File(folderPath + currentTime + "_" + fileCounter);

                while(screenshotFile.exists()){

                    if (!chunksScreenshots.containsKey(chunkFile.getName())){
                        chunksScreenshots.put(chunkFile.getName(), new ArrayList<File>());
                    }
                    chunksScreenshots.get(chunkFile.getName()).add(screenshotFile);

                    fileCounter++;
                    screenshotFile = new File(folderPath + currentTime + "_" + fileCounter);
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {

                    }
                });
            }
        }).start();
    }
}