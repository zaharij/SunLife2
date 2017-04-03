package life.centaurs.sunlife.video.render.enums;


public enum OrientationEnum {
    PORTRAIT(0), PORTRAIT_REVERSE(180), LANDSCAPE(270), LANDSCAPE_REVERSE(90);

    private int degrees;

    OrientationEnum (int degrees){
        this.degrees = degrees;
    }

    public int getDegrees() {
        return degrees;
    }
}
