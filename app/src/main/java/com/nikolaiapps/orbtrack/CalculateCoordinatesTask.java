package com.nikolaiapps.orbtrack;


import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Calendar;


//Task to calculate coordinate information
public class CalculateCoordinatesTask extends ThreadTask<Object, Integer, Integer[]>
{
    @SuppressWarnings("SpellCheckingInspection")
    private static abstract class ParamTypes
    {
        static final String Index = "index";
        static final String Latitude = "lat";
        static final String Longitude = "lon";
        static final String Altitude = "alt";
        static final String Illumination = "illum";
        static final String SpeedKms = "speedKms";
        static final String PhaseName = "pn";
    }

    public static class CoordinateBase
    {
        public final Calculations.SatelliteObjectType satellite;
        public double pathJulianDateStart;
        public double pathJulianDateEnd;

        public CoordinateBase(Database.SatelliteData newSat)
        {
            satellite = newSat.satellite;
            pathJulianDateStart = pathJulianDateEnd = 0;
        }
    }

    public static class CoordinateItemBase extends Selectable.ListItem implements Parcelable
    {
        public boolean isLoading;
        public float latitude;
        public float longitude;
        public float altitudeKm;
        public double illumination;
        public double speedKms;
        public String phaseName;
        public TextView latitudeText;
        public TextView longitudeText;
        public TextView altitudeText;
        public LinearLayout progressGroup;
        public LinearLayout dataGroup;
        public static final Creator<CoordinateItemBase> CREATOR =  new Parcelable.Creator<CoordinateItemBase>()
        {
            @Override
            public CoordinateItemBase createFromParcel(Parcel source)
            {
                Bundle bundle = source.readBundle(getClass().getClassLoader());
                if(bundle == null)
                {
                    bundle = new Bundle();
                }

                return(new CoordinateItemBase(bundle.getInt(ParamTypes.Index), bundle.getFloat(ParamTypes.Latitude), bundle.getFloat(ParamTypes.Longitude), bundle.getFloat(ParamTypes.Altitude), bundle.getDouble(ParamTypes.Illumination), bundle.getDouble(ParamTypes.SpeedKms), bundle.getString(ParamTypes.PhaseName)));
            }

            @Override
            public CoordinateItemBase[] newArray(int size)
            {
                return(new CoordinateItemBase[size]);
            }
        };

        private CoordinateItemBase(int index, float lat, float lon, float altKm, double illum, double spdKms, String pn)
        {
            super(Integer.MAX_VALUE, index, false, false, false, false);
            isLoading = false;
            latitude = lat;
            longitude = lon;
            altitudeKm = altKm;
            illumination = illum;
            speedKms = spdKms;
            phaseName = pn;
            latitudeText = null;
            longitudeText = null;
            altitudeText = null;
            progressGroup = null;
            dataGroup = null;
        }
        public CoordinateItemBase(int index, float lat, float lon, float alt)
        {
            this(index, lat, lon, alt, 0, Double.MAX_VALUE, null);
        }
        public CoordinateItemBase(int index)
        {
            this(index, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, 0, Double.MAX_VALUE, null);
        }

        @Override
        public int describeContents()
        {
            return(0);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags)
        {
            Bundle bundle = new Bundle();

            bundle.putInt(ParamTypes.Index, listIndex);
            bundle.putFloat(ParamTypes.Latitude, latitude);
            bundle.putFloat(ParamTypes.Longitude, longitude);
            bundle.putFloat(ParamTypes.Altitude, altitudeKm);
            bundle.putDouble(ParamTypes.Illumination, illumination);
            bundle.putDouble(ParamTypes.SpeedKms, speedKms);
            bundle.putString(ParamTypes.PhaseName, phaseName);

            dest.writeBundle(bundle);
        }

        public boolean equals(CoordinateItemBase otherItem)
        {
            return(id == otherItem.id);
        }

        public void setLoading(boolean loading, boolean tleIsAccurate)
        {
            isLoading = loading && tleIsAccurate;

            if(progressGroup != null)
            {
                progressGroup.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
            if(dataGroup != null)
            {
                dataGroup.setVisibility(loading || !tleIsAccurate ? View.GONE : View.VISIBLE);
            }
        }

        public void updateDisplays()
        {
            Context context = (latitudeText != null ? latitudeText.getContext() : longitudeText != null ? longitudeText.getContext() : null);
            Resources res = (context != null ? context.getResources() : null);

            if(latitudeText != null)
            {
                latitudeText.setText(latitude != Float.MAX_VALUE ? Globals.getLatitudeDirectionString(res, latitude, 2) : "-");
            }
            if(longitudeText != null)
            {
                longitudeText.setText(longitude != Float.MAX_VALUE ? Globals.getLongitudeDirectionString(res, longitude, 2) : "-");
            }
            if(altitudeText != null)
            {
                altitudeText.setText(altitudeKm != Float.MAX_VALUE ? Globals.getNumberString(Globals.getKmUnitValue(altitudeKm), 0) : "-");
            }
        }
    }

    private final OnProgressChangedListener progressChangedListener;
    public boolean calculatingCoordinates;

    //Progress listener
    public interface OnProgressChangedListener
    {
        void onProgressChanged(int index, int progressType, ArrayList<OrbitalCoordinate> coordinates);
    }

    //Coordinate information at time
    public static class OrbitalCoordinate extends Calculations.GeodeticDataType
    {
        public final double julianDate;
        public final double illumination;
        public final Calendar time;
        public final String phaseName;

        public OrbitalCoordinate(Calculations.GeodeticDataType geoData, double jd, double illum, String pn)
        {
            latitude = geoData.latitude;
            longitude = geoData.longitude;
            altitudeKm = geoData.altitudeKm;
            julianDate = jd;
            illumination = illum;
            phaseName = pn;
            time = Globals.julianDateToCalendar(julianDate);
        }
    }

    //Constructor
    public CalculateCoordinatesTask(OnProgressChangedListener listener)
    {
        progressChangedListener = listener;
        calculatingCoordinates = false;
    }

    private ArrayList<OrbitalCoordinate> getCoordinates(Context context, CoordinateBase currentPath, Current.Coordinates.Item[] savedCoordinateItems, Calculations.ObserverType observer, double dayIncrement)
    {
        int index = 0;
        double phase;
        double illumination = 0;
        double coordinateJulianDate;
        double beforeDayIncrement = ((10.0 / Calculations.SecondsPerDay));     //10 seconds
        final boolean isSun = (currentPath.satellite.getSatelliteNum() == Universe.IDs.Sun);
        final boolean isMoon = (currentPath.satellite.getSatelliteNum() == Universe.IDs.Moon);
        String phaseName = null;
        Calculations.GeodeticDataType geoData;
        Calculations.TopographicDataType oldTopoData;
        Calculations.TopographicDataType currentTopoData;
        Calculations.SatelliteObjectType coordinateSatellite = new Calculations.SatelliteObjectType(currentPath.satellite);
        ArrayList<OrbitalCoordinate> coordinates = new ArrayList<>(0);

        //calculate coordinates in path unless cancelled
        for(coordinateJulianDate = currentPath.pathJulianDateStart; coordinateJulianDate <= currentPath.pathJulianDateEnd && !this.isCancelled(); coordinateJulianDate += dayIncrement)
        {
            //create new geographic data
            geoData = new Calculations.GeodeticDataType();

            //if there are saved items and julian date matches current
            if(savedCoordinateItems != null && index < savedCoordinateItems.length && coordinateJulianDate == savedCoordinateItems[index].julianDate)
            {
                //remember current item
                Current.Coordinates.Item currentItem = savedCoordinateItems[index];

                //set next coordinate
                geoData.latitude = currentItem.latitude;
                geoData.longitude = currentItem.longitude;
                geoData.altitudeKm = currentItem.altitudeKm;
                illumination = currentItem.illumination;
                phaseName = currentItem.phaseName;
            }
            else
            {
                //calculate next position
                Calculations.updateOrbitalPosition(coordinateSatellite, observer, coordinateJulianDate, true);
                geoData.latitude = coordinateSatellite.geo.latitude;
                geoData.longitude = coordinateSatellite.geo.longitude;
                geoData.altitudeKm = coordinateSatellite.geo.altitudeKm;

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
                    currentTopoData = Calculations.getLookAngles(observer, coordinateSatellite, true);

                    //get old angles
                    Calculations.updateOrbitalPosition(coordinateSatellite, observer, coordinateJulianDate - beforeDayIncrement, false);
                    oldTopoData = Calculations.getLookAngles(observer, coordinateSatellite, true);

                    //get phase name
                    phaseName = Universe.Sun.getPhaseName(context, currentTopoData.elevation, (currentTopoData.elevation > oldTopoData.elevation));
                }

            }
            index++;

            //add coordinate to list
            coordinates.add(new OrbitalCoordinate(geoData, coordinateJulianDate, illumination, phaseName));

            //if date is before end date and next date would be after
            if(coordinateJulianDate < currentPath.pathJulianDateEnd && (coordinateJulianDate + dayIncrement) > currentPath.pathJulianDateEnd)
            {
                //set to increment before end date
                coordinateJulianDate = currentPath.pathJulianDateEnd - dayIncrement;
            }
        }

        //return views
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
        CoordinateBase[] orbitalCoordinates = (CoordinateBase[])params[1];
        Current.Coordinates.Item[] savedCoordinateItems = (Current.Coordinates.Item[])params[2];

        //update progress
        onProgressChanged(0, Globals.ProgressType.Started, null);

        //go through each orbital while not cancelled
        for(index = 0; index < orbitalCoordinates.length && !this.isCancelled(); index++)
        {
            //get current path and satellite
            CoordinateBase currentPath = orbitalCoordinates[index];
            ArrayList<OrbitalCoordinate> coordinates;

            //update progress
            publishProgress(index, Globals.ProgressType.Started);

            //set given dates
            currentPath.pathJulianDateStart = julianDateStart;
            currentPath.pathJulianDateEnd = julianDateEnd;

            //get coordinates
            coordinates = getCoordinates(context, currentPath, savedCoordinateItems, observer, dayIncrement);

            //update progress
            onProgressChanged(index, (!this.isCancelled() ? Globals.ProgressType.Success : Globals.ProgressType.Failed), coordinates);
        }

        //update status
        calculatingCoordinates = false;

        //return status
        return(new Integer[]{!this.isCancelled() ? Globals.ProgressType.Finished : Globals.ProgressType.Cancelled});
    }

    //Calls on progress changed listener
    private void onProgressChanged(int index, int progressType, ArrayList<OrbitalCoordinate> coordinates)
    {
        //if there is a listener
        if(progressChangedListener != null)
        {
            progressChangedListener.onProgressChanged(index, progressType, coordinates);
        }
    }

    @Override
    protected  void onPostExecute(Integer[] result)
    {
        //finished
        onProgressChanged(0, result[0], null);
    }

    @Override
    protected void onCancelled(Integer[] result)
    {
        //cancelled
        onProgressChanged(0, (result != null ? result[0] : Globals.ProgressType.Cancelled), null);
    }
}
