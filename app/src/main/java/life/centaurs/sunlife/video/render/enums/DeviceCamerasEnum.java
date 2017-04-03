package life.centaurs.sunlife.video.render.enums;


public enum DeviceCamerasEnum {
    BACK_CAMERA(0), FRONT_CAMERA(1);

    private final int CAMERA_ID;

    DeviceCamerasEnum(int id){
        this.CAMERA_ID = id;
    }

    public int getCAMERA_ID() {
        return CAMERA_ID;
    }
}
