package life.centaurs.sunlife.video.render.display;

import android.content.Context;
import android.os.Handler;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import life.centaurs.sunlife.video.edit.VideoEditor;
import life.centaurs.sunlife.video.render.enums.MediaExtensionEnum;
import life.centaurs.sunlife.video.render.enums.OrientationEnum;

import static life.centaurs.sunlife.video.edit.EditVideoCommandsStr.getAddAudioCommand;
import static life.centaurs.sunlife.video.edit.EditVideoCommandsStr.getConvertPhotoToVideoCommand;
import static life.centaurs.sunlife.video.edit.EditVideoCommandsStr.getConvertVideoFileToTransitExtensionCommand;
import static life.centaurs.sunlife.video.edit.EditVideoCommandsStr.getMakeFullVideoCommand;
import static life.centaurs.sunlife.video.edit.EditVideoCommandsStr.getMakePhotoScreenshotsCommand;
import static life.centaurs.sunlife.video.edit.EditVideoCommandsStr.getMakeVideoScreenshotsCommand;
import static life.centaurs.sunlife.video.edit.EditVideoCommandsStr.getRotatePhotoCommand;
import static life.centaurs.sunlife.video.edit.EditVideoCommandsStr.getSetRotationMetaComand;
import static life.centaurs.sunlife.video.render.constants.DisplayConstants.DOT_STRING;
import static life.centaurs.sunlife.video.render.constants.DisplayConstants.EMPTY_STRING;
import static life.centaurs.sunlife.video.render.constants.DisplayConstants.FFMPEG_COUNTER;
import static life.centaurs.sunlife.video.render.constants.DisplayConstants.NAME_SEPARATOR;
import static life.centaurs.sunlife.video.render.constants.DisplayConstants.SCREENSHOTS_FRAMES_PER_SECOND;
import static life.centaurs.sunlife.video.render.constants.DisplayConstants.SCREENSHOT_HEIGHT;
import static life.centaurs.sunlife.video.render.constants.DisplayConstants.SCREENSHOT_NAME_START_COUNTER;
import static life.centaurs.sunlife.video.render.constants.DisplayConstants.SCREENSHOT_WIDTH;
import static life.centaurs.sunlife.video.render.constants.DisplayConstants.TRANSPOSE_CLOCKWISE_90;
import static life.centaurs.sunlife.video.render.constants.DisplayConstants.TRANSPOSE_COUNTER_CLOCKWISE_90;
import static life.centaurs.sunlife.video.render.constants.DisplayConstants.getCaptureFile;
import static life.centaurs.sunlife.video.render.constants.DisplayConstants.getMediaDir;

public class ChunksManager {
    private final String FULL_VIDEO_NAME_PREFIX = "SL_FULL_VIDEO_";
    private final String FULL_VIDEO_AUDIO_FILE_NAME_PREFIX = "SL_VIDEO_";
    private final static String OUTPUT_VIDEO_CLIP = "VIDEO_SUNLIFE_";
    public final static String TRANSIT_CHUNK_FILE_EXTENSION = ".ts";
    public final static String CHUNKS_STRING_BUFER_SEPARATOR = "|";
    public static boolean screenshotNumberNameIsTaken = false;
    private final static int ADDITIONAL_OPERATIONS_NUMBER = 3;
    private Set<File> chunksFiles;
    private Map<String, Integer> chunksDuration = new HashMap<>();
    private Map<String, ArrayList<File>> chunksScreenshots;//file name as a key
    private Handler handler;
    private OnScreenshotEndCreationListener onScreenshotEndCreationListener;
    private VideoSuccessListenerInterface videoSuccessListener;
    private String currentTime;
    private String folderPath;
    private File chunkFile;
    private int photoCounter = 0;
    private int videoCounter = 0;
    private File fullVideoFile;
    private File fullVideoAudioFile;
    private File currentSetChunk;
    private Context context;
    private VideoEditor videoEditor;
    private String chunkListStr;
    private File outputVideoFile;
    private boolean isCancelled = false;

    public interface OnScreenshotEndCreationListener{
        void onEndScreenshotCreation();
    }

    public ChunksManager(Context context, OnScreenshotEndCreationListener onScreenshotEndCreationListener
            , VideoSuccessListenerInterface videoSuccessListener){
        this.context = context;
        videoEditor = new VideoEditor(context);
        this.onScreenshotEndCreationListener = onScreenshotEndCreationListener;
        this.videoSuccessListener = videoSuccessListener;
        chunksFiles = new LinkedHashSet<>();
        chunksScreenshots = new HashMap<>();
        handler = new Handler();
    }

    public Map<String, Integer> getChunksDurationProgress() {
        return chunksDuration;
    }

    public void setIsCancelled(boolean isCancelled) {
        this.isCancelled = isCancelled;
    }

    public boolean isCancelled() {
        return isCancelled;
    }

    public static int getAdditionalOperationsNumber() {
        return ADDITIONAL_OPERATIONS_NUMBER;
    }

    public String getChunkListStr() {
        return chunkListStr;
    }

    public File getFullVideoFile() {
        return fullVideoFile;
    }

    public Set<File> getChunksFiles() {
        return chunksFiles;
    }

    public File getFullVideoAudioFile() {
        return fullVideoAudioFile;
    }

    public File getOutputVideoFile() {
        return outputVideoFile;
    }

    public void setChunkFile(final File chunkFile) {
        setChunksScreenshots(chunkFile);
        this.chunkFile = chunkFile;
        if (this.chunksFiles.add(chunkFile)){
            chunksDuration.put(chunkFile.getAbsolutePath(), ProgressBarManager.getLastChunkDurationProgress());
        }
    }

    public Map<String, ArrayList<File>> getChunkScreenshots() {
        return chunksScreenshots;
    }

    public ArrayList<File> getChunkScreenshots(File chunkFile) {
        return chunksScreenshots.get(chunkFile.getAbsolutePath());
    }

    public void makeFullVideo(){
        StringBuilder chunkListStrBuilder = new StringBuilder();
        for(File currentFile: chunksFiles){
            int i = currentFile.getName().lastIndexOf(DOT_STRING);
            String currentFileExtension = currentFile.getName().substring(i);
            if (currentFileExtension.equalsIgnoreCase(CameraActivity.getPhotoExtension().getExtensionStr())){
                photoCounter++;
                videoEditor.execFFmpegBinary(getConvertPhotoToVideoCommand(currentFile)
                        , isCancelled ? onFfmpegSuccessListenerEndProcessing : onFfmpegSuccessListenerPhotoToVideo);
            } else {
                videoCounter++;
                videoEditor.execFFmpegBinary(getConvertVideoFileToTransitExtensionCommand(currentFile)
                        , isCancelled ? onFfmpegSuccessListenerEndProcessing : onFfmpegSuccessListenerVideoChunkToTransitFile);
            }
            chunkListStrBuilder.append(currentFile.getAbsolutePath())
                    .append(TRANSIT_CHUNK_FILE_EXTENSION).append(CHUNKS_STRING_BUFER_SEPARATOR);
        }

        fullVideoFile = getCaptureFile(CameraActivity.getVideoExtension().getExtensionStr(), FULL_VIDEO_NAME_PREFIX);
        chunkListStrBuilder.deleteCharAt(chunkListStrBuilder.lastIndexOf(CHUNKS_STRING_BUFER_SEPARATOR));
        chunkListStr = chunkListStrBuilder.toString();
    }

    private void setChunksScreenshots(final File chunkFile){
        Thread setChunksScreenshotsThread = new Thread(new Runnable() {
            @Override
            public void run() {
                currentSetChunk = chunkFile;
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

                StringBuilder absoluteScreenshotFilePathStringBuilder = new StringBuilder();;

                if (currentFileExtension.equalsIgnoreCase(CameraActivity.getVideoExtension().getExtensionStr())){
                    absoluteScreenshotFilePathStringBuilder.append(folderPath).append(currentTime)
                            .append(NAME_SEPARATOR.concat(FFMPEG_COUNTER)).append(MediaExtensionEnum.JPG.getExtensionStr());
                    videoEditor.execFFmpegBinary(getMakeVideoScreenshotsCommand(chunkFile
                            , absoluteScreenshotFilePathStringBuilder, SCREENSHOTS_FRAMES_PER_SECOND, width, height)
                            , isCancelled ? onFfmpegSuccessListenerEndProcessing : onFfmpegSuccessListenerScreenshots);
                } else if (currentFileExtension.equalsIgnoreCase(CameraActivity.getPhotoExtension().getExtensionStr())){
                    if (CameraFragment.getVideoOrientationEnum().equals(OrientationEnum.LANDSCAPE)
                            || CameraFragment.getVideoOrientationEnum().equals(OrientationEnum.LANDSCAPE_REVERSE)
                            || CameraFragment.getVideoOrientationEnum().equals(OrientationEnum.PORTRAIT_REVERSE)) {
                        absoluteScreenshotFilePathStringBuilder.append(folderPath).append(rotationCount)
                                .append(NAME_SEPARATOR).append(currentTime).append(NAME_SEPARATOR)
                                .append(rotationCount).append(MediaExtensionEnum.JPG.getExtensionStr());
                        videoEditor.execFFmpegBinary(getMakePhotoScreenshotsCommand(chunkFile
                                , absoluteScreenshotFilePathStringBuilder)
                                , isCancelled ? onFfmpegSuccessListenerEndProcessing : onFfmpegSuccessListenerCreateScreenshotPhoto);
                    } else {
                        absoluteScreenshotFilePathStringBuilder.append(folderPath).append(currentTime)
                                .append(NAME_SEPARATOR.concat(FFMPEG_COUNTER)).append(MediaExtensionEnum.JPG.getExtensionStr());
                        videoEditor.execFFmpegBinary(getMakePhotoScreenshotsCommand(chunkFile
                                , absoluteScreenshotFilePathStringBuilder)
                                , isCancelled ? onFfmpegSuccessListenerEndProcessing : onFfmpegSuccessListenerScreenshots);
                    }
                }
            }
        });
        setChunksScreenshotsThread.setPriority(Thread.MAX_PRIORITY);
        setChunksScreenshotsThread.start();
    }

    private final int ROTATION_START_COUNT = 1;
    public int getROTATION_START_COUNT() {
        return ROTATION_START_COUNT;
    }
    private int rotationCount = ROTATION_START_COUNT;
    private VideoEditor.OnFfmpegSuccessListener onFfmpegSuccessListenerCreateScreenshotPhoto = new VideoEditor.OnFfmpegSuccessListener() {
        @Override
        public void onSuccess() {
            File screenshotFile = new File(folderPath.concat("" + ROTATION_START_COUNT).concat(NAME_SEPARATOR)
                    .concat(currentTime).concat(NAME_SEPARATOR).concat("" + ROTATION_START_COUNT).concat(MediaExtensionEnum.JPG.getExtensionStr()));
            File outputScreenshotFile = new File(folderPath.concat(currentTime).concat(NAME_SEPARATOR)
                    + SCREENSHOT_NAME_START_COUNTER + MediaExtensionEnum.JPG.getExtensionStr());
            int transposeNumber = TRANSPOSE_CLOCKWISE_90;
            if (CameraFragment.getVideoOrientationEnum().equals(OrientationEnum.LANDSCAPE)
                    || CameraFragment.getVideoOrientationEnum().equals(OrientationEnum.LANDSCAPE_REVERSE)){
                switch(CameraFragment.getVideoOrientationEnum()){
                    case LANDSCAPE:
                        transposeNumber = TRANSPOSE_COUNTER_CLOCKWISE_90;
                        break;
                    default:
                        transposeNumber = TRANSPOSE_CLOCKWISE_90;
                        break;
                }
                videoEditor.execFFmpegBinary(getRotatePhotoCommand(screenshotFile, transposeNumber
                        , outputScreenshotFile), isCancelled ? onFfmpegSuccessListenerEndProcessing : onFfmpegSuccessListenerScreenshots);
            } else {
                File tempFile = new File(folderPath.concat("" + (ROTATION_START_COUNT + 1)).concat(NAME_SEPARATOR)
                        .concat(currentTime).concat(NAME_SEPARATOR).concat("" + ROTATION_START_COUNT)
                        .concat(MediaExtensionEnum.JPG.getExtensionStr()));
                if (rotationCount < 2){
                    videoEditor.execFFmpegBinary(getRotatePhotoCommand(screenshotFile, transposeNumber
                            , tempFile), isCancelled ? onFfmpegSuccessListenerEndProcessing : onFfmpegSuccessListenerCreateScreenshotPhoto);
                } else {
                    videoEditor.execFFmpegBinary(getRotatePhotoCommand(tempFile, transposeNumber
                            , outputScreenshotFile), isCancelled ? onFfmpegSuccessListenerEndProcessing : onFfmpegSuccessListenerScreenshots);
                }
                rotationCount++;
            }
        }
    };

    private VideoEditor.OnFfmpegSuccessListener onFfmpegSuccessListenerScreenshots = new VideoEditor.OnFfmpegSuccessListener() {
        @Override
        public void onSuccess() {
            int fileCounter = SCREENSHOT_NAME_START_COUNTER;
            File screenshotFile = new File(folderPath.concat(currentTime).concat(NAME_SEPARATOR) + fileCounter
                    + MediaExtensionEnum.JPG.getExtensionStr());
            while(screenshotFile.exists()){
                if (!chunksScreenshots.containsKey(chunkFile.getAbsolutePath())){
                    chunksScreenshots.put(chunkFile.getAbsolutePath(), new ArrayList<File>());
                }
                chunksScreenshots.get(chunkFile.getAbsolutePath()).add(screenshotFile);
                fileCounter++;
                screenshotFile = new File(folderPath + currentTime + NAME_SEPARATOR + fileCounter + MediaExtensionEnum.JPG.getExtensionStr());
            }
            rotationCount = ROTATION_START_COUNT;
            onScreenshotEndCreationListener.onEndScreenshotCreation();
        }
    };

    private VideoEditor.OnFfmpegSuccessListener onFfmpegSuccessListenerPhotoToVideo = new VideoEditor.OnFfmpegSuccessListener() {
        @Override
        public void onSuccess() {
            if (isCancelled) {
                onFfmpegSuccessListenerEndProcessing.onSuccess();
            }
            CameraNavigationFragment.getProgressBarVideoEditingDialog().updateProgress();
            photoCounter--;
            if (videoCounter <= 0 && photoCounter <= 0) {
                videoEditor.execFFmpegBinary(getMakeFullVideoCommand(chunkListStr, fullVideoFile)
                        , isCancelled ? onFfmpegSuccessListenerEndProcessing : onFfmpegSuccessListenerFullVideo);
            }
        }
    };

    private VideoEditor.OnFfmpegSuccessListener onFfmpegSuccessListenerVideoChunkToTransitFile = new VideoEditor.OnFfmpegSuccessListener() {
        @Override
        public void onSuccess() {
            if (isCancelled) {
                onFfmpegSuccessListenerEndProcessing.onSuccess();
            }
            CameraNavigationFragment.getProgressBarVideoEditingDialog().updateProgress();
            videoCounter--;
            if (videoCounter <= 0 && photoCounter <= 0) {
                videoEditor.execFFmpegBinary(getMakeFullVideoCommand(chunkListStr, fullVideoFile)
                        , isCancelled ? onFfmpegSuccessListenerEndProcessing : onFfmpegSuccessListenerFullVideo);
            }
        }
    };

    private VideoEditor.OnFfmpegSuccessListener onFfmpegSuccessListenerFullVideo = new VideoEditor.OnFfmpegSuccessListener() {
        @Override
        public void onSuccess() {
            CameraNavigationFragment.getProgressBarVideoEditingDialog().updateProgress();
            fullVideoAudioFile = getCaptureFile(CameraActivity.getVideoExtension().getExtensionStr(), FULL_VIDEO_AUDIO_FILE_NAME_PREFIX);
            videoEditor.execFFmpegBinary(getAddAudioCommand(fullVideoFile, new File (getMediaDir()
                    .getAbsolutePath().concat("/").concat(ChoseSound.getSound())), fullVideoAudioFile)
                    , isCancelled ? onFfmpegSuccessListenerEndProcessing : onFfmpegSuccessListenerAudioVideoFile);
        }
    };

    private VideoEditor.OnFfmpegSuccessListener onFfmpegSuccessListenerAudioVideoFile = new VideoEditor.OnFfmpegSuccessListener() {
        @Override
        public void onSuccess() {
            CameraNavigationFragment.getProgressBarVideoEditingDialog().updateProgress();
            outputVideoFile = getCaptureFile(CameraActivity.getVideoExtension().getExtensionStr(), OUTPUT_VIDEO_CLIP);
            videoEditor.execFFmpegBinary(getSetRotationMetaComand(fullVideoAudioFile
                    , CameraFragment.getRealVideoOrientationEnum().getDegrees(), outputVideoFile)
                    , onFfmpegSuccessListenerEndProcessing);
        }
    };

    private VideoEditor.OnFfmpegSuccessListener onFfmpegSuccessListenerEndProcessing = new VideoEditor.OnFfmpegSuccessListener() {
        @Override
        public void onSuccess() {
            if (!isCancelled){
                CameraNavigationFragment.getProgressBarVideoEditingDialog().updateProgress();
            }
            videoSuccessListener.onVideoSuccessListener();
        }
    };
}