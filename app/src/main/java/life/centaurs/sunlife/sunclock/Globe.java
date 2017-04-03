package life.centaurs.sunlife.sunclock;


import org.joda.time.LocalDate;

public abstract class Globe {
    protected LocalDate requiredDate;
    protected double latitudeInSeconds;
    protected double longitudeInSeconds;//is positive for East and negative for West

    public Globe (LocalDate requiredDate, double latitudeInSeconds, double longitudeInSeconds){
        this.requiredDate = requiredDate;
        this.latitudeInSeconds = latitudeInSeconds;
        this.longitudeInSeconds = longitudeInSeconds;
    }
}
