package life.centaurs.sunlife.video.render.enums;


public enum MediaExtensionEnum {
    MP4(".mp4"), JPG(".jpg");

    private final String extensionStr;

    MediaExtensionEnum(String extensionStr){
        this.extensionStr = extensionStr;
    }

    public String getExtensionStr() {
        return extensionStr;
    }
}
