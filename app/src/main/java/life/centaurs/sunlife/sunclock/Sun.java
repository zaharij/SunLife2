package life.centaurs.sunlife.sunclock;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import life.centaurs.sunlife.sunclock.enums.SunTimeEnum;

/**
 * Sun
 * This class describes all sunrise and sunset time calculations
 */
public final class Sun extends Globe {
    private static final int OFFICIAL_ZENITH_IN_SECONDS = 327000;
    private static final int CIVIL_ZENITH_IN_SECONDS = 345600;
    private static final int NAUTICAL_ZENITH_IN_SECONDS = 367200;
    private static final int ASTRONOMICAL_ZENITH_IN_SECONDS = 388800;
    private static final double D2R = Math.PI / 180;
    private static final double R2D = 180 / Math.PI;

    public Sun(LocalDate requiredDate, double latitudeInSeconds, double longitudeInSeconds) {
        super(requiredDate, latitudeInSeconds, longitudeInSeconds);
    }

    /**
     * returns the longitude of the day
     * @return the longitude of the day (int)
     */
    public int getDayLongitude(){
        int dayTime = getOfficialSunsetTime().getMillisOfDay() - getOfficialSunriseTime().getMillisOfDay();
        return dayTime;
    }

    /**
     * returns the sun zenith time
     * @return the sun zenith time (LocalTime - joda-time)
     */
    public LocalTime getSunZenithTime(){
        int zenith =  getDayLongitude() / 2;
        LocalTime localTime = getOfficialSunriseTime().plusMillis(zenith);
        return localTime;
    }

    /**
     * returns local sunrise (astronomical) time
     * @return local sunrise (astronomical) time (localTime - joda-time)
     */
    public LocalTime getAstronomicalSunriseTime(){
        return getSunTime(SunTimeEnum.SUNRISE, ASTRONOMICAL_ZENITH_IN_SECONDS);
    }

    /**
     * returns local sunset (astronomical) time
     * @return local sunset (astronomical) time (localTime - joda-time)
     */
    public LocalTime getAstronomicalSunsetTime(){
        return getSunTime(SunTimeEnum.SUNSET, ASTRONOMICAL_ZENITH_IN_SECONDS);
    }

    /**
     * returns local sunrise (nautical) time
     * @return local sunrise (nautical) time (localTime - joda-time)
     */
    public LocalTime getNauticalSunriseTime(){
        return getSunTime(SunTimeEnum.SUNRISE, NAUTICAL_ZENITH_IN_SECONDS);
    }

    /**
     * returns local sunset (nautical) time
     * @return local sunset (nautical) time (localTime - joda-time)
     */
    public LocalTime getNauticalSunsetTime(){
        return getSunTime(SunTimeEnum.SUNSET, NAUTICAL_ZENITH_IN_SECONDS);
    }

    /**
     * returns local sunrise (civil) time
     * @return local sunrise (civil) time (localTime - joda-time)
     */
    public LocalTime getCivilSunriseTime(){
        return getSunTime(SunTimeEnum.SUNRISE, CIVIL_ZENITH_IN_SECONDS);
    }

    /**
     * returns local sunset (civil) time
     * @return local sunset (civil) time (localTime - joda-time)
     */
    public LocalTime getCivilSunsetTime(){
        return getSunTime(SunTimeEnum.SUNSET, CIVIL_ZENITH_IN_SECONDS);
    }

    /**
     * returns local sunrise time
     * @return local sunrise time (localTime - joda-time)
     */
    public LocalTime getOfficialSunriseTime(){
        return getSunTime(SunTimeEnum.SUNRISE, OFFICIAL_ZENITH_IN_SECONDS);
    }

    /**
     * returns local sunset time
     * @return local sunset time (localTime - joda-time)
     */
    public LocalTime getOfficialSunsetTime(){
        return getSunTime(SunTimeEnum.SUNSET, OFFICIAL_ZENITH_IN_SECONDS);
    }

    /**
     * calculates and returns the time of sunrise or sunset
     * , according to given parameters
     * @param sunTimeEnum sunrise or sunset param
     * @param zenith sun zenith degrees
     * @return the time of sunrise or sunset
     */
    private LocalTime getSunTime(SunTimeEnum sunTimeEnum, int zenith){
        double approximateTime = getApproximateTime(sunTimeEnum);
        double sunMeanAnomaly = getSunMeanAnomaly(approximateTime);
        double trueLongitude = getSunTrueLongitude(sunMeanAnomaly);
        double rightAscension = getSunRightAscension(trueLongitude);
        double rightAscensionInSameQuadrantAsTrueLongitude = getSunRightAscensionInSameQuadrantAsTrueLongitude(rightAscension, trueLongitude);
        double rightAscensionInHours = getSunRightAscensionInHours(rightAscensionInSameQuadrantAsTrueLongitude);
        double sinDec = getSunDeclinationSin(trueLongitude);
        double cosDec = getSunDeclinationCos(sinDec);
        double localHourAngle = getSunLocalHourAngle(sinDec, cosDec, zenith, sunTimeEnum);
        double localMeanTime = getLocalMeanTimeOfRisingSetting(localHourAngle, rightAscensionInHours, approximateTime);
        long utc = getUtcTimeMilliseconds(localMeanTime);

        LocalTime localTimeSunrise = new LocalTime(utc);

        return localTimeSunrise;
    }

    /**
     * converts the longitude to hour value
     * @return the longitude in hours
     */
    private double getLongitudeHour(){
        double longitudeHour = (longitudeInSeconds / 60 / 60) / 15;
        return longitudeHour;
    }

    /**
     * calculates an approximate time
     * @param sunTimeEnum - if rising time is desired, or if setting time is desired (enum)
     * @return an approximate time (double)
     */
    private double getApproximateTime(SunTimeEnum sunTimeEnum){
        double approximateTime = 0;
        double longitudeHour = getLongitudeHour();
        switch (sunTimeEnum){
            case SUNRISE:
                approximateTime = requiredDate.getDayOfYear() + ((6 - longitudeHour) / 24);
                break;
            case SUNSET:
                approximateTime = requiredDate.getDayOfYear() + ((18 - longitudeHour) / 24);
                break;
        }
        return approximateTime;
    }

    /**
     * calculates the Sun's mean anomaly
     * @param approximateTime - an approximate time
     * @return the Sun's mean anomaly (double)
     */
    private double getSunMeanAnomaly(double approximateTime){
        double sunMeanAnomaly = (0.9856 * approximateTime) - 3.289;
        return sunMeanAnomaly;
    }

    /**
     * calculates the Sun's true longitude
     * @param sunMeanAnomaly - the Sun's mean anomaly
     * @return the Sun's true longitude (double)
     */
    private double getSunTrueLongitude(double sunMeanAnomaly){
        double trueLongitude = sunMeanAnomaly + (1.916 * Math.sin(sunMeanAnomaly * D2R))
                + (0.020 * Math.sin(2 * sunMeanAnomaly * D2R)) + 282.634;
        if (trueLongitude > 360){
            trueLongitude -= 360;
        } else if (trueLongitude < 0){
            trueLongitude += 360;
        }
        return trueLongitude;
    }

    /**
     * calculates the Sun's right ascension
     * @param trueLongitude - the Sun's true longitude
     * @return he Sun's right ascension (double)
     */
    private double getSunRightAscension(double trueLongitude){
        double rightAscension = R2D * Math.atan(0.91764 * Math.tan(trueLongitude * D2R));
        if (rightAscension > 360) {
            rightAscension -= 360;
        } else if (rightAscension < 0) {
            rightAscension += 360;
        }
        return rightAscension;
    }

    /**
     * returns the right ascension value
     * , which needs to be in the same quadrant as Sun's true longitude
     * @param rightAscension - the Sun's right ascension (double)
     * @param trueLongitude - the Sun's true longitude
     * @return the right ascension value (double)
     */
    private double getSunRightAscensionInSameQuadrantAsTrueLongitude(double rightAscension, double trueLongitude){
        double lquadrant = (Math.floor(trueLongitude / (90))) * 90;
        double rAquadrant = (Math.floor(rightAscension / 90)) * 90;
        double rightAscensionInSameQuadrantAsTrueLongitude = rightAscension + (lquadrant - rAquadrant);
        return rightAscensionInSameQuadrantAsTrueLongitude;
    }

    /**
     * converts into hours and returns right ascension value
     * @param rightAscensionInSameQuadrantAsTrueLongitude returns the right ascension value in the same quadrant as Sun's true longitude
     * @return right ascension value in hours (double)
     */
    private double getSunRightAscensionInHours(double rightAscensionInSameQuadrantAsTrueLongitude){
        double rightAscensionInHours = rightAscensionInSameQuadrantAsTrueLongitude / 15;
        return rightAscensionInHours;
    }

    /**
     * calculates the Sun's declination (sin)
     * @param trueLongitude the Sun's true longitude
     * @return the Sun's declination (sin) - double
     */
    private double getSunDeclinationSin(double trueLongitude){
        double sinDec = 0.39782 * Math.sin(trueLongitude * D2R);
        return sinDec;
    }

    /**
     * calculates the Sun's declination (cos)
     * @param sinDec the Sun's declination (sin)
     * @return the Sun's declination (cos) - double
     */
    private double getSunDeclinationCos(double sinDec){
        double cosDec = Math.cos(Math.asin(sinDec));
        return cosDec;
    }

    /**
     * calculates the Sun's local hour angle
     * @param sinDec the Sun's declination (sin)
     * @param cosDec the Sun's declination (cos)
     * @param zenith - zenith in seconds
     * @param sunTimeEnum sunset or sunrise
     * @return the Sun's local hour angle
     */
    private double getSunLocalHourAngle(double sinDec, double cosDec, int zenith, SunTimeEnum sunTimeEnum){
        double cosH = (Math.cos((zenith / 60 / 60) * D2R) - (sinDec * Math.sin((latitudeInSeconds / 60 / 60) * D2R)))
                / (cosDec * Math.cos((latitudeInSeconds / 60 / 60) * D2R));
        double localHourAngle = 0;
        switch (sunTimeEnum){
            case SUNRISE:
                localHourAngle = 360 - R2D * Math.acos(cosH);
                break;
            case SUNSET:
                localHourAngle = R2D * Math.acos(cosH);
        }
        localHourAngle /= 15;
        return localHourAngle;
    }

    /**
     * calculates local mean time of rising/setting
     * @param localHourAngle the Sun's local hour angle
     * @param rightAscensionInHours right ascension value in hours
     * @param approximateTime an approximate time
     * @return the local mean time of rising/setting
     */
    private double getLocalMeanTimeOfRisingSetting(double localHourAngle, double rightAscensionInHours, double approximateTime){
        double localMeanTime = localHourAngle + rightAscensionInHours - (0.06571 * approximateTime) - 6.622;
        return localMeanTime;
    }

    /**
     * adjusts back to UTC
     * @param localMeanTime local mean time of rising/setting
     * @return utcMilli (long)
     */
    private long getUtcTimeMilliseconds(double localMeanTime){
        double utc = localMeanTime - getLongitudeHour();
        if (utc > 24) {
            utc = utc - 24;
        } else if (utc < 0) {
            utc = utc + 24;
        }
        long utcMilli = (long) (utc * 1000 * 60 * 60);
        return utcMilli;
    }
}
