package life.centaurs.sunlife.video.render.display;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import life.centaurs.sunlife.R;
import life.centaurs.sunlife.activities.CameraActivity;

import static life.centaurs.sunlife.video.render.constants.DisplayConstants.CAMERA_MESSAGE_COLOR;
import static life.centaurs.sunlife.video.render.constants.DisplayConstants.CAMERA_MESSAGE_TEXT_SCALE_X;
import static life.centaurs.sunlife.video.render.constants.DisplayConstants.CAMERA_MESSAGE_TEXT_SIZE;
import static life.centaurs.sunlife.video.render.constants.DisplayConstants.DEPRECATION_ANNOTATION_MESs;
import static life.centaurs.sunlife.video.render.constants.DisplayConstants.MIN_VIDEO_TIME_IN_MILLIS;
import static life.centaurs.sunlife.video.render.constants.DisplayConstants.NAME_SEPARATOR;
import static life.centaurs.sunlife.video.render.constants.DisplayConstants.PHOTO_PROGRESS_STATUS;
import static life.centaurs.sunlife.video.render.constants.DisplayConstants.TIME_PHOTO_BUTTON_ACTIVE_IN_MILLIS;
import static life.centaurs.sunlife.video.render.constants.DisplayConstants.VIDEO_PROGRESS_TIME;
import static life.centaurs.sunlife.video.render.constants.DisplayConstants.getMediaDir;
import static life.centaurs.sunlife.video.render.display.CameraFragment.currentFile;
import static life.centaurs.sunlife.video.render.display.ProgressBarDialog.isProcessing;
import static life.centaurs.sunlife.video.render.encoder.PhotoManager.getPhotoNamePrefix;
import static life.centaurs.sunlife.video.render.enums.CommandEnum.BACK_TO_MAIN;
import static life.centaurs.sunlife.video.render.enums.CommandEnum.START_RECORDING;
import static life.centaurs.sunlife.video.render.enums.CommandEnum.STOP_RECORDING;
import static life.centaurs.sunlife.video.render.enums.CommandEnum.TAKE_PICTURE;


public class CameraNavigationFragment extends Fragment implements ProgressBarManager.OnProgressListener {
    private boolean isRecording = false;
    private ProgressBarManager progressBarManager;
    private ProgressBar progressBarHorizontal;
    private FragmentsCommunicationListener fragmentsCommunicationListener;
    private Handler handler = new Handler();
    private Handler handlerMinVideo = new Handler();
    private boolean recordVideo = false;
    private ImageView imageViewRecordButton;
    private AnimationDrawable animationRecordButton;
    private long touchDownTime = 0;
    private long resultTime;
    public static ScreenshotAnimator screenshotAnimator;
    private Context context;
    private LinearLayout mGallery;
    private ImageButton imageButtonRemoveVideo, imageButtonSaveVideo;
    private static boolean readyToRecord = true;
    private static boolean chunksIsRemoved = false;
    private boolean isReadyToSaveVideo = false;
    private boolean videoIsCreating = false;
    private static ProgressBarDialog progressBarVideoEditingDialog;
    private ImageView backgroundImageView;
    private ProgressBar progressBar;
    private TextView progressText, messageTextView;
    private Button cancelButton, cancelButton2, okButton2;

    public static ChunksManager chunksManager;

    @SuppressWarnings(DEPRECATION_ANNOTATION_MESs)
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
        chunksManager = new ChunksManager(context, onScreenshotEndCreationListener, videoSuccessListener);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            try {
                fragmentsCommunicationListener = (FragmentsCommunicationListener) activity;
            } catch (ClassCastException e) {
                throw new ClassCastException(activity.toString() + " must implements OnCameraButtonListener");
            }
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        chunksManager = new ChunksManager(context, onScreenshotEndCreationListener, videoSuccessListener);
        this.context = context;
        Activity activity = context instanceof Activity ? (Activity) context : null;
        try {
            fragmentsCommunicationListener = (FragmentsCommunicationListener) activity;
        } catch (ClassCastException e){
            throw new ClassCastException(context.toString() + " must implements OnCameraButtonListener");
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_camera_navigation, container, false);

        isProcessing = false;
        progressBarVideoEditingDialog.setIsCancelled(false);

        progressBarHorizontal = (ProgressBar) rootView.findViewById(R.id.progressBarHorizontal);
        progressBarManager = new ProgressBarManager(progressBarHorizontal, VIDEO_PROGRESS_TIME, this);

        imageButtonRemoveVideo = (ImageButton) rootView.findViewById(R.id.imageButtonRemoveVideo);
        imageButtonRemoveVideo.setOnClickListener(onClickListenerVideoChunks);

        imageButtonSaveVideo = (ImageButton) rootView.findViewById(R.id.imageButtonSaveVideo);
        imageButtonSaveVideo.setOnClickListener(onClickListenerVideoChunks);
        setIsReadyToSaveVideo(false);

        imageViewRecordButton = (ImageView) rootView.findViewById(R.id.imageViewRecordButton);
        imageViewRecordButton.setOnTouchListener(onRecordButtonTouchListener);
        imageViewRecordButton.setBackgroundResource(R.drawable.record_btn_0);

        messageTextView = (TextView) rootView.findViewById(R.id.textViewMessageCamera);
        messageTextView.setTextColor(CAMERA_MESSAGE_COLOR);
        messageTextView.setTextScaleX(CAMERA_MESSAGE_TEXT_SCALE_X);
        messageTextView.setTextSize(CAMERA_MESSAGE_TEXT_SIZE);

        mGallery = (LinearLayout) rootView.findViewById(R.id.id_gallery);
        screenshotAnimator = new ScreenshotAnimator(this, context, mGallery);

        initProgressBarDialogVideoEdit(rootView);

        progressBarManager.startProgressBar();
        return rootView;
    }

    private final int MENU_REMOVE_CHUNK = 0;
    private final int MENU_SAVE_CHUNK = 1;
    private final int MENU_REMOVE_AND_SAVE_CHUNK = 2;
    private ArrayList<String> chunksNamesToSaveArrayStr = new ArrayList<>();
    private View viewToGone;
    private String currentViewStringSelected;
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (!isProcessing) {
            switch (v.getId()) {
                default:
                    menu.add(0, MENU_REMOVE_CHUNK, 0, R.string.delete_chunk_menu);
                    menu.add(0, MENU_SAVE_CHUNK, 0, R.string.save_on_device_menu);
                    menu.add(0, MENU_REMOVE_AND_SAVE_CHUNK, 0, R.string.save_on_device_del_from_video_menu);
                    int firstIndex = v.toString().indexOf("{") + 1;
                    int lastIndex = v.toString().indexOf(" ");
                    currentViewStringSelected = v.toString().substring(firstIndex, lastIndex);
                    screenshotAnimator.getScreenshotHashMap().get(currentViewStringSelected);
                    viewToGone = v;
                    break;
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        Set chunkSkreenshotsKeys = chunksManager.getChunkScreenshots().keySet();
        Iterator iterator = chunkSkreenshotsKeys.iterator();
        String keyStrFile = null;
        File chunkFileToDelete = null;
        while (iterator.hasNext()){
            String currentKeyStr = (String) iterator.next();
            int currentScreenshotsListHash = chunksManager.getChunkScreenshots().get(currentKeyStr).hashCode();
            if(currentScreenshotsListHash == screenshotAnimator.getScreenshotHashMap().get(currentViewStringSelected)){
                keyStrFile = currentKeyStr;
                chunkFileToDelete = new File(keyStrFile);
                break;
            }
        }
        switch (item.getItemId()) {
            case MENU_REMOVE_CHUNK:
                chunkFileToDelete.delete();
            case MENU_REMOVE_AND_SAVE_CHUNK:
                for (File fileToDel: chunksManager.getChunkScreenshots().get(keyStrFile)){
                    fileToDel.delete();
                }
                chunksManager.getChunkScreenshots().remove(keyStrFile);
                screenshotAnimator.getScreenshotHashMap().remove(currentViewStringSelected);
                chunksManager.getChunksFiles().remove(chunkFileToDelete);
                viewToGone.setVisibility(View.GONE);
                if (chunksManager.getChunkScreenshots().keySet().size() <= 0){
                    progressBarManager.nullProgressBarStatus();
                }
                progressBarManager.decreaseProgressStatus(chunksManager.getChunksDurationProgress().get(keyStrFile));
                chunksNamesToSaveArrayStr.add(keyStrFile);
                chunksManager.getChunksDurationProgress().remove(keyStrFile);
                if (ProgressBarManager.isTimeIsOff()){
                    ProgressBarManager.setTimeIsOff(false);
                    progressBarManager.startProgressBar();
                }
                Toast.makeText(context, R.string.deleted_message, Toast.LENGTH_SHORT).show();
                break;
            case MENU_SAVE_CHUNK:
                chunksNamesToSaveArrayStr.add(keyStrFile);
                Toast.makeText(context, R.string.saved_message, Toast.LENGTH_SHORT).show();
                break;
        }
        return super.onContextItemSelected(item);
    }

    private void initProgressBarDialogVideoEdit(View rootView) {
        backgroundImageView = (ImageView) rootView.findViewById(R.id.imageViewProgressVideoEdit);
        progressBar = (ProgressBar) rootView.findViewById(R.id.progressBarVideoEdit);
        progressText = (TextView) rootView.findViewById(R.id.progressStatusTextView);
        cancelButton = (Button) rootView.findViewById(R.id.buttonCancelProgressBar);
        cancelButton.setOnClickListener(onClickListenerVideoChunks);
        cancelButton2 = (Button) rootView.findViewById(R.id.exitProgressButton);
        cancelButton2.setOnClickListener(onClickListenerVideoChunks);
        okButton2 = (Button) rootView.findViewById(R.id.buttonSaveProgressBar);
        okButton2.setOnClickListener(onClickListenerVideoChunks);
        progressBarVideoEditingDialog = new ProgressBarDialog(backgroundImageView, progressBar
                , progressText, cancelButton, null, cancelButton2, okButton2);
        progressBarVideoEditingDialog.setTextToCancelButton(getResources().getString(R.string.cancel_btn));
        progressBarVideoEditingDialog.setTextToCancelButton2(getResources().getString(R.string.cancel_btn_2));
        progressBarVideoEditingDialog.setTextToOkButton2(getResources().getString(R.string.save_btn_2));
        progressBarVideoEditingDialog.setVisibility(ProgressBarDialog.ProgressBarDialogVisibilityEnum.INVISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        isRecording = false;
        startRecordingVisualisation(isRecording);
        super.onPause();
    }

    @Override
    public void startActivity(Intent intent) {
        super.startActivity(intent);
    }

    public static boolean isReadyToRecord() {
        return readyToRecord;
    }

    public static void setReadyToRecord(boolean readyToRecord) {
        CameraNavigationFragment.readyToRecord = readyToRecord;
    }

    View.OnClickListener onClickListenerVideoChunks = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.imageButtonRemoveVideo:
                    removeChunks();
                    progressBarManager.nullProgressBarStatus();
                    screenshotAnimator = null;
                    CameraFragment.setNullVideoOrientationEnum();
                    ProgressBarManager.setTimeIsOff(false);
                    fragmentsCommunicationListener.onClickButton(BACK_TO_MAIN);
                    break;
                case R.id.imageButtonSaveVideo:
                    if (!videoIsCreating && isReadyToSaveVideo) {
                        imageButtonSaveVideo.setEnabled(false);
                        imageButtonRemoveVideo.setEnabled(false);
                        isProcessing = true;
                        int eachUpdateProgressNumber = (chunksManager.getChunksFiles().size()
                                + chunksManager.getAdditionalOperationsNumber());
                        progressBarVideoEditingDialog.setEachUpdateProgressStatus(eachUpdateProgressNumber);
                        videoIsCreating = true;
                        progressBarVideoEditingDialog.setVisibility(ProgressBarDialog.ProgressBarDialogVisibilityEnum.VISIBLE);
                        progressBarVideoEditingDialog.setProgressStatus(0);
                        chunksManager.makeFullVideo();
                    }
                    break;
                case R.id.buttonSaveProgressBar:
                    isProcessing = false;
                    ProgressBarManager.setTimeIsOff(false);
                    Toast.makeText(context, R.string.saved_message, Toast.LENGTH_SHORT).show();
                    fragmentsCommunicationListener.onClickButton(BACK_TO_MAIN);
                    break;
                case R.id.exitProgressButton:
                    isProcessing = false;
                    ProgressBarManager.setTimeIsOff(false);
                    File videoFile = chunksManager.getOutputVideoFile();
                    if (videoFile != null && videoFile.exists()) {
                        videoFile.delete();
                    }
                    Toast.makeText(context, R.string.deleted_message, Toast.LENGTH_SHORT).show();
                    fragmentsCommunicationListener.onClickButton(BACK_TO_MAIN);
                    break;
                case R.id.buttonCancelProgressBar:
                    cancelButton.setEnabled(false);
                    chunksManager.setIsCancelled(true);
                    progressBarVideoEditingDialog.setIsCancelled(true);
                    progressBarVideoEditingDialog.setProgressText(getResources().getString(R.string.process_cancelled_message));
                    Toast.makeText(context, R.string.canselled_message, Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    public void setMessageText(String text){
        messageTextView.setText(text);
    }

    public CharSequence getTextFromTextMessage(){
        CharSequence charSequence = messageTextView.getText();
        return messageTextView.getText();
    }

    public static boolean isProcessing() {
        return isProcessing;
    }

    public static void nullIsProcessing(){
        isProcessing = false;
    }

    public static ProgressBarDialog getProgressBarVideoEditingDialog() {
        return progressBarVideoEditingDialog;
    }

    private final View.OnTouchListener onRecordButtonTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (isReadyToRecord() && !isProcessing) {
                recordVideo = true;
                if (!ProgressBarManager.isTimeIsOff()) {
                    switch (motionEvent.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            setIsReadyToSaveVideo(false);
                            touchDownTime = System.currentTimeMillis();
                            startVideoCountDown();
                            break;
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            resultTime = System.currentTimeMillis() - touchDownTime;
                            recordVideo = false;
                            if (resultTime >= TIME_PHOTO_BUTTON_ACTIVE_IN_MILLIS) {
                                if (resultTime >= MIN_VIDEO_TIME_IN_MILLIS) {
                                    isRecording = false;
                                    recordButtonClick(R.drawable.record_off_button);
                                    startRecordingVisualisation(isRecording);
                                    fragmentsCommunicationListener.onClickButton(STOP_RECORDING);
                                } else {
                                    minVideoCountDown();
                                }
                            } else {
                                recordButtonClick(R.drawable.anim_photo);
                                progressBarManager.increaseStatusProgressStatuc(PHOTO_PROGRESS_STATUS);
                                fragmentsCommunicationListener.onClickButton(TAKE_PICTURE);
                            }
                            setReadyToRecord(false);
                            break;
                    }
                }
            }
            return true;
        }
    };

    private void startVideoCountDown(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(TIME_PHOTO_BUTTON_ACTIVE_IN_MILLIS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (recordVideo) {
                            isRecording = true;
                            recordButtonClick(R.drawable.record_on_button);
                            startRecordingVisualisation(isRecording);
                            fragmentsCommunicationListener.onClickButton(START_RECORDING);
                        }
                    }
                });
            }
        }).start();
    }

    private void minVideoCountDown(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(MIN_VIDEO_TIME_IN_MILLIS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        isRecording = false;
                        recordButtonClick(R.drawable.record_off_button);
                        startRecordingVisualisation(isRecording);
                        fragmentsCommunicationListener.onClickButton(STOP_RECORDING);
                    }
                });
            }
        }).start();
    }

    private void recordButtonClick(int resourceBackground){
        if (!ProgressBarManager.isTimeIsOff()) {
            imageViewRecordButton.setBackgroundResource(resourceBackground);
            animationRecordButton = (AnimationDrawable) imageViewRecordButton.getBackground();
            animationRecordButton.setOneShot(true);
            animationRecordButton.stop();
            animationRecordButton.start();
        }
    }

    protected void startRecordingVisualisation(boolean isRecording) {
        if (isRecording) {
            progressBarManager.startProgress();
        } else {
            progressBarManager.pauseProgress();
        }
    }

    @Override
    public void onEndProgress() {
        if (isRecording){
            isRecording = false;
            startRecordingVisualisation(isRecording);
            fragmentsCommunicationListener.onClickButton(STOP_RECORDING);
        }
        imageViewRecordButton.setBackgroundResource(R.drawable.record_btn_0);
        ProgressBarManager.setTimeIsOff(true);
    }

    @Override
    public void onDestroy() {
        if(!chunksIsRemoved && !isProcessing) {
            removeChunks();
        }
        CameraFragment.setNullVideoOrientationEnum();
        ProgressBarManager.setTimeIsOff(false);
        super.onDestroy();
    }

    /**
     * removes all video chunks from SD
     */
    public void removeChunks(){
        try {
            File soundFileToDelete = new File(getMediaDir().getAbsolutePath().concat("/").concat(ChoseSound.getSound()));
            if (soundFileToDelete.exists()) {
                soundFileToDelete.delete();
            }
        } catch (NullPointerException ex) {

        }
        ChoseSound.nullSound();
        for(File currentFile: chunksManager.getChunksFiles()) {
            if (currentFile.exists()) {
                if (chunksManager.getChunkScreenshots(currentFile) != null) {
                    for (File currentScreenshotChunk : chunksManager.getChunkScreenshots(currentFile)) {
                        int countScreenshots = chunksManager.getROTATION_START_COUNT();
                        String absolutePath = currentScreenshotChunk.getAbsolutePath().replace(currentScreenshotChunk.getName(), "");
                        File tempScreenshotFile = new File(absolutePath.concat("" + countScreenshots)
                                .concat(NAME_SEPARATOR).concat(currentScreenshotChunk.getName()));
                        while(tempScreenshotFile.exists()){
                            tempScreenshotFile.delete();
                            countScreenshots++;
                            tempScreenshotFile = new File(currentScreenshotChunk.getAbsolutePath()
                                .replace(currentScreenshotChunk.getName(), "" + (countScreenshots))
                                .concat(NAME_SEPARATOR).concat(currentScreenshotChunk.getName()));
                        }
                        currentScreenshotChunk.delete();
                    }
                }
                if (currentFile.getName().contains(getPhotoNamePrefix())) {
                    File fileToDelete = new File(currentFile.getAbsolutePath()
                            .concat(CameraActivity.getVideoExtension().getExtensionStr()));
                    if (fileToDelete.exists()) {
                        fileToDelete.delete();
                    }
                }
                if (!chunksNamesToSaveArrayStr.contains(currentFile.getAbsolutePath())){
                    currentFile.delete();
                }
            }
        }
        File fullVideoAudioFile = chunksManager.getFullVideoAudioFile();
        if (fullVideoAudioFile != null){
            fullVideoAudioFile.delete();
        }
        if (chunksManager.getFullVideoFile() != null && chunksManager.getFullVideoFile().exists()) {
            chunksManager.getFullVideoFile().delete();
        }

        try {
            String[] chunkListStrArr = chunksManager.getChunkListStr().split("\\|");
            File fileToDel;
            for (int i = 0; i < chunkListStrArr.length; i++){
                fileToDel = new File(chunkListStrArr[i]);
                if (fileToDel.exists()){
                    fileToDel.delete();
                }
            }
        } catch (NullPointerException e){
        }
        chunksIsRemoved = true;
    }

    public ChunksManager.OnScreenshotEndCreationListener onScreenshotEndCreationListener
            = new ChunksManager.OnScreenshotEndCreationListener() {
        @Override
        public void onEndScreenshotCreation() {
            screenshotAnimator.startScreenshotAnim(chunksManager.getChunkScreenshots(currentFile));
            readyToRecord = true;
            setIsReadyToSaveVideo(true);
        }
    };

    private void setIsReadyToSaveVideo(boolean isReady){
        if (isReady){
            isReadyToSaveVideo = true;
            imageButtonSaveVideo.setImageResource(R.drawable.btn_save_video);
        } else {
            isReadyToSaveVideo = false;
            imageButtonSaveVideo.setImageResource(R.drawable.save_frames_unactive);
        }
    }

    VideoSuccessListenerInterface videoSuccessListener = new VideoSuccessListenerInterface() {
        @Override
        public void onVideoSuccessListener() {
            removeChunks();
            isProcessing = false;
            if (!chunksManager.isCancelled()){
                progressBarVideoEditingDialog.showOnEndProgressChoiceDialog();
                try {
                    progressBarVideoEditingDialog.setProgressText(getResources().getString(R.string.process_complete_message));
                } catch (Exception ex){

                }
            } else {
                fragmentsCommunicationListener.onClickButton(BACK_TO_MAIN);
            }
            CameraFragment.setNullVideoOrientationEnum();
        }
    };
}
