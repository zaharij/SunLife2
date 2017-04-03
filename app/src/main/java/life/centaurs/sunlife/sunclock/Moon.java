package life.centaurs.sunlife.sunclock;

import org.joda.time.LocalDate;

public final class Moon extends Globe {

    public Moon(LocalDate requiredDate, double latitudeInSeconds, double longitudeInSeconds) {
        super(requiredDate, latitudeInSeconds, longitudeInSeconds);
    }
}
