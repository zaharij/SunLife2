package life.centaurs.sunlife.video.render.enums;


public enum VideoSizeEnum {
    MEDIUM_SIZE(1280, 720);

    private final int WIDTH;
    private final int HEIGHT;

    VideoSizeEnum(int width, int height){
        this.WIDTH = width;
        this.HEIGHT = height;
    }

    public int getWIDTH() {
        return WIDTH;
    }

    public int getHEIGHT() {
        return HEIGHT;
    }
}
