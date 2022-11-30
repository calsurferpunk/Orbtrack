package com.nikolaiapps.orbtrack;


import android.content.Context;
import java.util.ArrayList;
import java.util.Calendar;


//Task to calculate coordinate information
public class CalculateCoordinatesTask extends ThreadTask<Object, Integer, Integer[]>
{
    public static class CoordinateData extends Calculate.CalculateDataBase
    {
        public float latitude;
        public float longitude;
        public float altitudeKm;
        public double speedKms;

        public CoordinateData()
        {
            this.latitude = Float.MAX_VALUE;
            this.longitude = Float.MAX_VALUE;
            this.altitudeKm = Float.MAX_VALUE;
            this.speedKms = Double.MAX_VALUE;
        }
    }

    private final OnProgressChangedListener progressChangedListener;
    public boolean calculatingCoordinates;

    //Progress listener
    public interface OnProgressChangedListener
    {
        void onProgressChanged(int progressType, int satelliteIndex, ArrayList<OrbitalCoordinate> coordinates);
    }

    //Coordinate information at time
    public static class OrbitalCoordinate extends Calculations.GeodeticDataType
    {
        public final double julianDate;
        public final double illumination;
        public final Calendar time;
        public final String phaseName;

        public OrbitalCoordinate(Calculations.GeodeticDataType geoData, double julianDate, double illumination, String phaseName)
        {
            this.latitude = geoData.latitude;
            this.longitude = geoData.longitude;
            this.altitudeKm = geoData.altitudeKm;
            this.speedKmS = geoData.speedKmS;
            this.julianDate = julianDate;
            this.illumination = illumination;
            this.phaseName = phaseName;
            this.time = Globals.julianDateToCalendar(this.julianDate);
        }
    }

    //Constructor
    public CalculateCoordinatesTask(OnProgressChangedListener listener)
    {
        progressChangedListener = listener;
        calculatingCoordinates = false;
    }

    private ArrayList<OrbitalCoordinate> getCoordinates(Context context, Calculations.SatelliteObjectType currentSatellite, int satelliteIndex, Current.Coordinates.Item[] savedCoordinateItems, Calculations.ObserverType observer, double pathJulianDateStart, double pathJulianDateEnd, double dayIncrement)
    {
        int index = 0;
        double phase;
        double illumination = 0;
        double coordinateJulianDate;
        double beforeDayIncrement = ((10.0 / Calculations.SecondsPerDay));     //10 seconds
        final boolean isSun = (currentSatellite.getSatelliteNum() == Universe.IDs.Sun);
        final boolean isMoon = (currentSatellite.getSatelliteNum() == Universe.IDs.Moon);
        String phaseName = null;
        Calculations.GeodeticDataType geoData;
        Calculations.TopographicDataType oldTopographicData;
        Calculations.TopographicDataType currentTopographicData;
        Calculations.SatelliteObjectType coordinateSatellite = new Calculations.SatelliteObjectType(currentSatellite);
        ArrayList<OrbitalCoordinate> coordinates = new ArrayList<>(0);

        //calculate coordinates in path unless cancelled
        for(coordinateJulianDate = pathJulianDateStart; coordinateJulianDate <= pathJulianDateEnd && !this.isCancelled(); coordinateJulianDate += dayIncrement)
        {
            //create new geographic data
            geoData = new Calculations.GeodeticDataType();

            //if there are saved items, satellite index is valid, and julian date matches current
            if(savedCoordinateItems != null && index < savedCoordinateItems.length && satelliteIndex < savedCoordinateItems[index].coordinates.length && coordinateJulianDate == savedCoordinateItems[index].julianDate)
            {
                //remember current item
                Current.Coordinates.Item currentItem = savedCoordinateItems[index];

                //set next coordinate
                geoData.latitude = currentItem.coordinates[satelliteIndex].latitude;
                geoData.longitude = currentItem.coordinates[satelliteIndex].longitude;
                geoData.altitudeKm = currentItem.coordinates[satelliteIndex].altitudeKm;
                geoData.speedKmS = currentItem.coordinates[satelliteIndex].speedKms;
                illumination = currentItem.coordinates[satelliteIndex].illumination;
                phaseName = currentItem.coordinates[satelliteIndex].phaseName;
            }
            else
            {
                //calculate next position
                Calculations.updateOrbitalPosition(coordinateSatellite, observer, coordinateJulianDate, true);
                geoData.latitude = coordinateSatellite.geo.latitude;
                geoData.longitude = coordinateSatellite.geo.longitude;
                geoData.altitudeKm = coordinateSatellite.geo.altitudeKm;
                if(coordinateSatellite.getSatelliteNum() < 0)
                {
                    geoData.speedKmS = (index > 0 ? Calculations.getDistanceKm(coordinates.get(index - 1), coordinateSatellite.geo) / (dayIncrement * Calculations.SecondsPerDay) : 0);
                }
                else
                {
                    geoData.speedKmS = coordinateSatellite.geo.speedKmS;
                }

                //if moon
                if(isMoon)
                {
                    //get illumination and phase name
                    phase = Universe.Moon.getPhase(Globals.julianDateToCalendar(coordinateJulianDate).getTimeInMillis());
                    illumination = Universe.Moon.getIllumination(phase);
                    phaseName = Universe.Moon.getPhaseName(context, phase);
                }
                //else if sun
                else if(isSun)
                {
                    //get current angles
                    currentTopographicData = Calculations.getLookAngles(observer, coordinateSatellite, true);

                    //get old angles
                    Calculations.updateOrbitalPosition(coordinateSatellite, observer, coordinateJulianDate - beforeDayIncrement, false);
                    oldTopographicData = Calculations.getLookAngles(observer, coordinateSatellite, true);

                    //get phase name
                    phaseName = Universe.Sun.getPhaseName(context, currentTopographicData.elevation, (currentTopographicData.elevation > oldTopographicData.elevation));
                }

            }
            index++;

            //add coordinate to list
            coordinates.add(new OrbitalCoordinate(geoData, coordinateJulianDate, illumination, phaseName));

            //if date is before end date and next date would be after
            if(coordinateJulianDate < pathJulianDateEnd && (coordinateJulianDate + dayIncrement) > pathJulianDateEnd)
            {
                //set to increment before end date
                coordinateJulianDate = pathJulianDateEnd - dayIncrement;
            }
        }

        //return coordinates
        return(coordinates);
    }

    @Override
    protected Integer[] doInBackground(Object... params)
    {
        //update status
        calculatingCoordinates = true;

        int index;
        double julianDateStart = (Double)params[4];
        double julianDateEnd = (Double)params[5];
        double dayIncrement = (Double)params[6];
        Context context = (Context)params[0];
        Calculations.ObserverType observer = (Calculations.ObserverType)params[3];
        Calculations.SatelliteObjectType[] satelliteObjects = (Calculations.SatelliteObjectType[])params[1];
        Current.Coordinates.Item[] savedCoordinateItems = (Current.Coordinates.Item[])params[2];

        //update progress
        onProgressChanged(Globals.ProgressType.Started, Integer.MAX_VALUE, null);

        //go through each orbital while not cancelled
        for(index = 0; index < satelliteObjects.length && !this.isCancelled(); index++)
        {
            //get current path and satellite
            Calculations.SatelliteObjectType currentSatellite = satelliteObjects[index];
            ArrayList<OrbitalCoordinate> coordinates;

            //update progress
            publishProgress(index, Globals.ProgressType.Started);

            //get coordinates
            coordinates = getCoordinates(context, currentSatellite, index, savedCoordinateItems, observer, julianDateStart, julianDateEnd, dayIncrement);

            //update progress
            onProgressChanged((!this.isCancelled() ? Globals.ProgressType.Success : Globals.ProgressType.Failed), index, coordinates);
        }

        //update status
        calculatingCoordinates = false;

        //update progress and return status
        onProgressChanged((!this.isCancelled() ? Globals.ProgressType.Finished : Globals.ProgressType.Cancelled), Integer.MAX_VALUE, null);
        return(new Integer[]{!this.isCancelled() ? Globals.ProgressType.Finished : Globals.ProgressType.Cancelled, Integer.MAX_VALUE});
    }

    //Calls on progress changed listener
    private void onProgressChanged(int progressType, int satelliteIndex, ArrayList<OrbitalCoordinate> coordinates)
    {
        //if there is a listener
        if(progressChangedListener != null)
        {
            progressChangedListener.onProgressChanged(progressType, satelliteIndex, coordinates);
        }
    }

    @Override
    protected  void onPostExecute(Integer[] result)
    {
        //finished
        onProgressChanged(result[0], result[1], null);
    }

    @Override
    protected void onCancelled(Integer[] result)
    {
        boolean haveResult = (result != null && result.length >= 2);

        //cancelled
        onProgressChanged((haveResult ? result[0] : Globals.ProgressType.Cancelled), (haveResult ? result[1] : Integer.MAX_VALUE), null);
    }
}
