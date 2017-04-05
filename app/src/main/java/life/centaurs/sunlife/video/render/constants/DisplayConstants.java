package life.centaurs.sunlife.video.render.constants;

import life.centaurs.sunlife.video.render.display.ProgressBarManager;

public final class DisplayConstants {
    public final static String DEPRECATION_ANNOTATION_MESs = "deprecation";

    public final static String NO_PERMISSION_TO_WRITE_EXTERNAL_STORAGE = "This app has no permission of writing external storage";
    public final static String OES_EGL_IMAGE_EXTERNAL_STR = "OES_EGL_image_external";
    public final static String NOT_SUPPORT_OES_EGL_MESSAGE = "This system does not support OES_EGL_image_external.";
    public final static String UNCNOWN_MESSAGE_MES = "unknown message:";
    public final static String CAMERA_THREAD_NAME =  "Camera thread";
    public final static String IS_NULL_MESSAGE = " is null";
    public final static String FORMAT_CHANGED_TWICE_MESSAGE = "format changed twice";
    public final static String MUXER_HAS_NOT_STARTED_MESSAGE = "drain:muxer hasn't started";
    public final static String VIDEO_ENCODER_ALREADY_ADDED =  "Video encoder already added.";
    public final static String UNSUPPORTED_ENCODER = "unsupported encoder";
    public final static String MUXER_ALREADY_STARTED_MESSAGE = "muxer already started";
    public final static String UNSUPPORTED_SURFACE_MESSAGE = "unsupported surface";
    public final static String EGL_ALREADY_SET_UP_MESSAGE = "EGL already set up";
    public final static String EGL_GET_DISPLAY_FAILED_MESSAGE = "eglGetDisplay failed";
    public final static String EGL_INIT_FAILE_MESS = "eglInitialize failed";
    public final static String CHOOSE_CONFIG_FAILED_MESS = "chooseConfig failed";
    public final static String EGL_CREATE_CONTEXT_STR = "eglCreateContext";
    public final static String EGL_CREATE_PBUFFER_SURFACE_STR = "eglCreatePbufferSurface";
    public final static String NULL_SURFACE_MESS = "surface was null";
    public final static String UNSUPPORTED_WINDOW_TYPE_MESS = "unsupported window type:";
    public final static String MUST_IMPL_ON_CAMERA_BUTTON_LISTENER_MESS = " must implements OnCameraButtonListener";

    public final static String A_POSITION_STR = "aPosition";
    public final static String A_TEXTURE_COORD_STR = "aTextureCoord";
    public final static String U_MVP_MATRIX_STR = "uMVPMatrix";
    public final static String U_TEX_MATRIX_STR = "uTexMatrix";
    public final static String NAME_SEPARATOR = "_";
    public final static String FFMPEG_COUNTER = "%d";
    public final static String EMPTY_STRING = "";
    public final static String DOT_STRING = ".";

    public final static int TOUGH_LENGTH_TO_SWITCH_CAMERA = 200;

    public final static int SCREENSHOT_NAME_START_COUNTER = 1;
    public final static int SCREENSHOTS_FRAMES_PER_SECOND = 2;
    public final static int SCREENSHOT_FRAME_CHANGE_DURATION = 1000 / SCREENSHOTS_FRAMES_PER_SECOND;

    private static int timeVideoProgressInSeconds = 45; //set hear time for VIDEO_PROGRESS_TIME in seconds
    private static int setProgressVideoTime(int timeInSeconds){
        return timeInSeconds * 10;
    }
    public final static int VIDEO_PROGRESS_TIME = setProgressVideoTime(timeVideoProgressInSeconds);

    private static int timePhotoProgress = 3;// set time for photo in seconds
    private static int setProgressPhoto(){
        return (ProgressBarManager.getProgressStatusMax() * timePhotoProgress) / timeVideoProgressInSeconds;
    }
    public final static int PHOTO_PROGRESS_STATUS = setProgressPhoto();

    public final static int TIME_PHOTO_BUTTON_ACTIVE_IN_MILLIS = 200;
    public final static int MIN_VIDEO_TIME_IN_MILLIS = 1000;

    public final static int SCREENSHOT_WIDTH = 160;
    public final static int SCREENSHOT_HEIGHT = 284;
}
