package life.centaurs.sunlife.video.render.display;

import android.os.Handler;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import life.centaurs.sunlife.video.edit.VideoEditor;
import life.centaurs.sunlife.video.render.enums.MediaExtensionEnum;

import static android.content.ContentValues.TAG;
import static life.centaurs.sunlife.video.edit.EditVideoCommandsStr.getAddAudioCommand;
import static life.centaurs.sunlife.video.edit.EditVideoCommandsStr.getConvertPhotoToVideoCommand;
import static life.centaurs.sunlife.video.edit.EditVideoCommandsStr.getFadeInCommand;
import static life.centaurs.sunlife.video.edit.EditVideoCommandsStr.getFadeOutCommand;
import static life.centaurs.sunlife.video.edit.EditVideoCommandsStr.getMakeFullVideoCommand;
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
import static life.centaurs.sunlife.video.render.constants.DisplayConstants.TIME_PHOTO_PROGRESS;
import static life.centaurs.sunlife.video.render.constants.DisplayConstants.getCaptureFile;
import static life.centaurs.sunlife.video.render.constants.DisplayConstants.getMediaDir;
import static life.centaurs.sunlife.video.render.display.CameraFragment.videoEditor;
import static life.centaurs.sunlife.video.render.display.ProgressBarManager.getLastChunkDuration;

public class ChunksManager {
    private final String FULL_VIDEO_NAME_PREFIX = "SL_FULL_VIDEO_";
    private final String FULL_VIDEO_AUDIO_FILE_NAME_PREFIX = "SL_VIDEO_";
    private Set<File> chunksFiles;
    private Map<String, ArrayList<File>> chunksScreenshots;//file name as a key
    private Handler handler;
    private OnScreenshotEndCreationListener onScreenshotEndCreationListener;
    private String currentTime;
    private String folderPath;
    private File chunkFile;
    private int photoCounter = 0;
    private File chunkListFile;
    public static boolean screenshotNumberNameIsTaken = false;
    private File fullVideoFile;
    private File fullVideoAudioFile;

    public interface OnScreenshotEndCreationListener{
        void onEndScreenshotCreation();
    }

    public ChunksManager(OnScreenshotEndCreationListener onScreenshotEndCreationListener){
        this.onScreenshotEndCreationListener = onScreenshotEndCreationListener;
        chunksFiles = new LinkedHashSet<>();
        chunksScreenshots = new HashMap<>();
        handler = new Handler();
    }

    public File getChunkListFile() {
        return chunkListFile;
    }

    public File getFullVideoFile() {
        return fullVideoFile;
    }

    public Set<File> getChunksFiles() {
        return chunksFiles;
    }

    public void setChunkFile(File chunkFile) {
        setChunksScreenshots(chunkFile);
        this.chunkFile = chunkFile;
        this.chunksFiles.add(chunkFile);
        //getAnimatedVideoChunk(chunkFile);
    }

    private void getAnimatedVideoChunk(File chunkFile){
        folderPath = chunkFile.getAbsolutePath().replace(chunkFile.getName(), EMPTY_STRING);
        final File tempFadeInChunk = new File(folderPath.concat("tempFadeIn.mp4"));
        final File tempFadeOutChunk = new File(folderPath.concat("tempFadeOut.mp4"));
        VideoEditor.OnFfmpegSuccessListener onFfmpegSuccessListenerFadeIn = new VideoEditor.OnFfmpegSuccessListener() {
            @Override
            public void onSuccess() {
                VideoEditor.OnFfmpegSuccessListener onFfmpegSuccessListenerFadeOut = new VideoEditor.OnFfmpegSuccessListener() {
                    @Override
                    public void onSuccess() {
                        tempFadeInChunk.delete();
                        getChunksFiles().add(tempFadeOutChunk);
                        tempFadeOutChunk.delete();
                    }
                };
                videoEditor.execFFmpegBinary(getFadeOutCommand(tempFadeInChunk, tempFadeOutChunk, getLastChunkDuration()), onFfmpegSuccessListenerFadeOut);
            }
        };
        videoEditor.execFFmpegBinary(getFadeInCommand(chunkFile, tempFadeInChunk, getLastChunkDuration()), onFfmpegSuccessListenerFadeIn);
    }

    public Map<String, ArrayList<File>> getChunkScreenshots() {
        return chunksScreenshots;
    }

    public ArrayList<File> getChunkScreenshots(File chunkFile) {
        return chunksScreenshots.get(chunkFile.getName());
    }

    public void makeFullVideo(){
        StringBuilder chunkListStrBuilder = new StringBuilder();
        for(File currentFile: chunksFiles){
            int i = currentFile.getName().lastIndexOf(DOT_STRING);
            String currentFileExtension = currentFile.getName().substring(i);
            if (currentFileExtension.equalsIgnoreCase(CameraActivity.getPhotoExtension().getExtensionStr())){
                photoCounter++;
                videoEditor.execFFmpegBinary(getConvertPhotoToVideoCommand(currentFile), onFfmpegSuccessListenerPhotoToVideo);
                chunkListStrBuilder.append("file '").append(currentFile.getAbsolutePath())
                        .append(CameraActivity.getVideoExtension().getExtensionStr())
                        .append("'\n").append("duration ").append(TIME_PHOTO_PROGRESS).append("\n");
            } else {
                chunkListStrBuilder.append("file '").append(currentFile.getAbsolutePath()).append("'\n");
            }
        }

        chunkListFile = new File(folderPath.concat("chunkList.txt"));
        try {
            chunkListFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        saveToFile(chunkListFile, chunkListStrBuilder.toString());
    }

    private boolean saveToFile(File file, String data){
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream fileOutputStream = new FileOutputStream(file,true);
            fileOutputStream.write((data + System.getProperty("line.separator")).getBytes());

            return true;
        }  catch(FileNotFoundException ex) {
            Log.d(TAG, ex.getMessage());
        }  catch(IOException ex) {
            Log.d(TAG, ex.getMessage());
        }
        return  false;


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

                if (currentFileExtension.equalsIgnoreCase(CameraActivity.getVideoExtension().getExtensionStr())){
                        videoEditor.execFFmpegBinary(getMakeVideoScreenshotsCommand(chunkFile
                                , absoluteScreenshotFilePathStringBuilder, SCREENSHOTS_FRAMES_PER_SECOND, width, height), onFfmpegSuccessListenerScreenshots);
                } else if (currentFileExtension.equalsIgnoreCase(CameraActivity.getPhotoExtension().getExtensionStr())){
                    videoEditor.execFFmpegBinary(getMakePhotoScreenshotsCommand(chunkFile
                            , absoluteScreenshotFilePathStringBuilder, width, height), onFfmpegSuccessListenerScreenshots);
                }
            }

        }).start();

    }

    private VideoEditor.OnFfmpegSuccessListener onFfmpegSuccessListenerScreenshots = new VideoEditor.OnFfmpegSuccessListener() {
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
            onScreenshotEndCreationListener.onEndScreenshotCreation();
        }
    };

    private VideoEditor.OnFfmpegSuccessListener onFfmpegSuccessListenerPhotoToVideo = new VideoEditor.OnFfmpegSuccessListener() {
        @Override
        public void onSuccess() {
            photoCounter--;
            if (photoCounter <= 0){
                fullVideoFile = getCaptureFile(CameraActivity.getVideoExtension().getExtensionStr(), FULL_VIDEO_NAME_PREFIX);
                videoEditor.execFFmpegBinary(getMakeFullVideoCommand(chunkListFile
                        , fullVideoFile), onFfmpegSuccessListenerFullVideo);
            }
        }
    };

    private VideoEditor.OnFfmpegSuccessListener onFfmpegSuccessListenerFullVideo = new VideoEditor.OnFfmpegSuccessListener() {
        @Override
        public void onSuccess() {
            fullVideoAudioFile = getCaptureFile(CameraActivity.getVideoExtension().getExtensionStr(), FULL_VIDEO_AUDIO_FILE_NAME_PREFIX);
            videoEditor.execFFmpegBinary(getAddAudioCommand(fullVideoFile, new File (getMediaDir()
                    .getAbsolutePath().concat("/").concat(ChoseSound.getSound())), fullVideoAudioFile), onFfmpegSuccessListenerAudioVideoFile);
        }
    };

    private VideoEditor.OnFfmpegSuccessListener onFfmpegSuccessListenerAudioVideoFile = new VideoEditor.OnFfmpegSuccessListener() {
        @Override
        public void onSuccess() {
            System.out.println("*********************************************************** end **********************************************************");
        }
    };
}