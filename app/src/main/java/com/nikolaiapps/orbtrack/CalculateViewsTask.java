package com.nikolaiapps.orbtrack;


import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;


//Task to calculate view path information
public class CalculateViewsTask extends ThreadTask<Object, Integer, Integer[]>
{
    @SuppressWarnings("SpellCheckingInspection")
    private static abstract class ParamTypes
    {
        static final String Index = "index";
        static final String Azimuth = "az";
        static final String Elevation = "el";
        static final String Range = "range";
        static final String JulianDate = "jd";
        static final String TimeZoneId = "tzId";
        static final String Illumination = "illum";
        static final String PhaseName = "pn";
    }

    public static class OrbitalPathBase
    {
        public final Calculations.SatelliteObjectType satellite;

        public OrbitalPathBase(Database.SatelliteData newSat)
        {
            satellite = newSat.satellite;
        }
    }

    public static class ViewItemBase extends Selectable.ListItem implements Parcelable
    {
        public boolean isLoading;
        public float azimuth;
        public float elevation;
        public float rangeKm;
        public double illumination;
        public String phaseName;
        public TextView azText;
        public TextView elText;
        public TextView rangeText;
        public TextView phaseText;
        public TextView illuminationText;
        public LinearLayout progressGroup;
        public LinearLayout dataGroup;
        public static final Creator<ViewItemBase> CREATOR =  new Parcelable.Creator<ViewItemBase>()
        {
            @Override
            public ViewItemBase createFromParcel(Parcel source)
            {
                Bundle bundle = source.readBundle(getClass().getClassLoader());
                if(bundle == null)
                {
                    bundle = new Bundle();
                }

                return(new ViewItemBase(bundle.getInt(ParamTypes.Index), bundle.getFloat(ParamTypes.Azimuth), bundle.getFloat(ParamTypes.Elevation), bundle.getFloat(ParamTypes.Range), bundle.getDouble(ParamTypes.Illumination), bundle.getString(ParamTypes.PhaseName)));
            }

            @Override
            public ViewItemBase[] newArray(int size)
            {
                return(new ViewItemBase[size]);
            }
        };

        public ViewItemBase(int index, float az, float el, float range, double illum, String pn)
        {
            super(Integer.MAX_VALUE, index, false, false, false, false);
            isLoading = false;
            azimuth = az;
            elevation = el;
            rangeKm = range;
            azText = null;
            elText = null;
            rangeText = null;
            phaseText = null;
            illuminationText = null;
            progressGroup = null;
            dataGroup = null;
            illumination = illum;
            phaseName = pn;
        }
        public ViewItemBase(int index, float az, float el, float range)
        {
            this(index, az, el, range, 0, null);
        }
        public ViewItemBase(int index)
        {
            this(index, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, 0, null);
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
            bundle.putFloat(ParamTypes.Azimuth, azimuth);
            bundle.putFloat(ParamTypes.Elevation, elevation);
            bundle.putFloat(ParamTypes.Range, rangeKm);
            bundle.putDouble(ParamTypes.Illumination, illumination);
            bundle.putString(ParamTypes.PhaseName, phaseName);

            dest.writeBundle(bundle);
        }

        public boolean equals(ViewItemBase otherItem)
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
            if(azText != null)
            {
                azText.setText(azimuth != Float.MAX_VALUE ? Globals.getDegreeString(azimuth) : "-");
            }
            if(elText != null)
            {
                elText.setText(elevation != Float.MAX_VALUE ? Globals.getDegreeString(elevation) : "-");
            }
            if(rangeText != null)
            {
                rangeText.setText(rangeKm != Float.MAX_VALUE ? Globals.getNumberString(Globals.getKmUnitValue(rangeKm), 0) : "-");
            }
            if(phaseText != null)
            {
                phaseText.setText(phaseName == null ? "-" : phaseName);
            }
            if(illuminationText != null)
            {
                String text = Globals.getNumberString(illumination, 1) + "%";
                illuminationText.setText(text);
            }
        }
    }

    private final OnProgressChangedListener progressChangedListener;

    public boolean needViews;

    //Progress listener
    public interface OnProgressChangedListener
    {
        void onProgressChanged(int index, int progressType, ArrayList<OrbitalView> pathPoints);
    }

    //Viewing information at time
    public static class OrbitalView extends Calculations.TopographicDataType implements Parcelable
    {
        private final String timeZoneId;

        public final double julianDate;
        public final double illumination;
        public final Calendar gmtTime;
        public final String phaseName;
        public final String timeString;
        public final Rect timeArea;

        public static final Creator<OrbitalView> CREATOR =  new Parcelable.Creator<OrbitalView>()
        {
            @Override
            public OrbitalView createFromParcel(Parcel source)
            {
                Bundle bundle = source.readBundle(getClass().getClassLoader());
                if(bundle == null)
                {
                    bundle = new Bundle();
                }

                return(new OrbitalView(null, bundle.getDouble(ParamTypes.Azimuth), bundle.getDouble(ParamTypes.Elevation), bundle.getDouble(ParamTypes.Range), bundle.getDouble(ParamTypes.JulianDate), bundle.getString(ParamTypes.TimeZoneId), bundle.getDouble(ParamTypes.Illumination), bundle.getString(ParamTypes.PhaseName)));
            }

            @Override
            public OrbitalView[] newArray(int size)
            {
                return(new OrbitalView[size]);
            }
        };

        private OrbitalView(Context context, double az, double el, double rKm, double jd, String zoneId, double illum, String pn, String tmStrng, boolean showSeconds)
        {
            if(zoneId == null)
            {
                zoneId = TimeZone.getDefault().getID();
            }

            azimuth = az;
            elevation = el;
            rangeKm = rKm;
            julianDate = jd;
            gmtTime = Globals.julianDateToCalendar(julianDate);
            timeZoneId = zoneId;
            if(tmStrng != null)
            {
                timeString = tmStrng;
            }
            else
            {
                timeString = Globals.getDateTimeString(context, Globals.getCalendar(zoneId, gmtTime.getTimeInMillis()), showSeconds, false, true);
            }
            timeArea = new Rect(0, 0, 0, 0);
            timeArea.setEmpty();
            illumination = illum;
            phaseName = pn;
        }
        public OrbitalView(Context context, double az, double el, double rKm, double jd, String zoneId, double illum, String pn)
        {
            this(context, az, el, rKm, jd, zoneId, illum, pn, null, false);
        }
        public OrbitalView(Context context, Calculations.TopographicDataType topoData, double jd, String zoneId, double illum, String pn)
        {
            this(context, topoData.azimuth, topoData.elevation, topoData.rangeKm, jd, zoneId, illum, pn, null, false);
        }
        public OrbitalView(Context context, Calculations.TopographicDataType topoData, double jd, String zoneId, boolean showSeconds)
        {
            this(context, topoData.azimuth, topoData.elevation, topoData.rangeKm, jd, zoneId, 0, null, null, showSeconds);
        }
        @SuppressWarnings("CopyConstructorMissesField")
        public OrbitalView(OrbitalView copyFrom)
        {
            this(null, copyFrom.azimuth, copyFrom.elevation, copyFrom.rangeKm, copyFrom.julianDate, copyFrom.timeZoneId, 0, null, copyFrom.timeString, false);
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

            bundle.putDouble(ParamTypes.Azimuth, azimuth);
            bundle.putDouble(ParamTypes.Elevation, elevation);
            bundle.putDouble(ParamTypes.Range, rangeKm);
            bundle.putDouble(ParamTypes.JulianDate, julianDate);
            bundle.putString(ParamTypes.TimeZoneId, timeZoneId);
            bundle.putDouble(ParamTypes.Illumination, illumination);
            bundle.putString(ParamTypes.PhaseName, phaseName);

            dest.writeBundle(bundle);
        }
    }

    //Constructor
    public CalculateViewsTask(OnProgressChangedListener listener)
    {
        progressChangedListener = listener;
        needViews = true;
    }

    private ArrayList<OrbitalView> getViews(Context context, OrbitalPathBase currentPath, Current.ViewAngles.Item[] savedViewItems, Calculations.ObserverType observer, double pathJulianDateStart, double pathJulianDateEnd, double dayIncrement, boolean limitTravel, boolean adjustTime, boolean hideSlow)
    {
        int index = 0;
        double phase;
        double azTravel = 0;
        double pathJulianDate;
        double illumination = 0;
        double lastAz = Double.MAX_VALUE;
        double period = currentPath.satellite.orbit.periodMinutes;
        double beforeDayIncrement = (10.0 / Calculations.SecondsPerDay);                          //10 seconds
        double periodJulianEnd = (period > 0 ? (pathJulianDateStart + (period / Calculations.MinutesPerDay)) : Double.MAX_VALUE);
        final boolean isSun = (currentPath.satellite.getSatelliteNum() == Universe.IDs.Sun);
        final boolean isMoon = (currentPath.satellite.getSatelliteNum() == Universe.IDs.Moon);
        String phaseName = null;
        Calculations.TopographicDataType topoData;
        Calculations.TopographicDataType oldTopoData;
        Calculations.SatelliteObjectType pathSatellite = new Calculations.SatelliteObjectType(currentPath.satellite);
        ArrayList<OrbitalView> pathViews = new ArrayList<>(0);

        //if want to adjust time and over 1 minute increment
        adjustTime = (adjustTime && dayIncrement > 0.000694444);

        //if adjusting time
        if(adjustTime)
        {
            //set julian dates to 0 seconds at that time
            pathJulianDateStart = Globals.julianDateNoSeconds(pathJulianDateStart);
            pathJulianDateEnd = Globals.julianDateNoSeconds(pathJulianDateEnd);
        }

        //calculate points in path unless cancelled
        for(pathJulianDate = pathJulianDateStart; pathJulianDate <= pathJulianDateEnd && (!limitTravel || pathJulianDate <= periodJulianEnd || Math.abs(azTravel) <= 360) && !this.isCancelled(); pathJulianDate += dayIncrement)
        {
            //if there are saved items and julian date matches current
            if(savedViewItems != null && index < savedViewItems.length && pathJulianDate == savedViewItems[index].julianDate)
            {
                //remember current item
                Current.ViewAngles.Item currentItem = savedViewItems[index];

                //set position
                topoData = new Calculations.TopographicDataType(currentItem.azimuth, currentItem.elevation, currentItem.rangeKm);
                illumination = currentItem.illumination;
                phaseName = currentItem.phaseName;
            }
            else
            {
                //if adjusting time and not on first or last
                if(adjustTime && pathJulianDate > pathJulianDateStart && pathJulianDate < pathJulianDateEnd)
                {
                    //set julian date to 0 seconds at that time
                    pathJulianDate = Globals.julianDateNoSeconds(pathJulianDate);
                }

                //calculate position
                Calculations.updateOrbitalPosition(pathSatellite, observer, pathJulianDate, false);
                topoData = Calculations.getLookAngles(observer, pathSatellite, true);

                //if moon
                if(isMoon)
                {
                    //get illumination and phase name
                    phase = Universe.Moon.getPhase(Globals.julianDateToCalendar(pathJulianDate).getTimeInMillis());
                    illumination = Universe.Moon.getIllumination(phase);
                    phaseName = Universe.Moon.getPhaseName(context, phase);
                }
                //else if sun
                else if(isSun)
                {
                    //get old angles
                    Calculations.updateOrbitalPosition(pathSatellite, observer, pathJulianDate - beforeDayIncrement, false);
                    oldTopoData = Calculations.getLookAngles(observer, pathSatellite, true);

                    //get phase name
                    phaseName = Universe.Sun.getPhaseName(context, topoData.elevation, (topoData.elevation > oldTopoData.elevation));
                }
            }
            index++;

            //add view to list
            pathViews.add(new OrbitalView(context, topoData, pathJulianDate, observer.timeZone.getID(), illumination, phaseName));

            //update azimuth travel
            if(lastAz != Double.MAX_VALUE)
            {
                azTravel += Math.abs(Globals.degreeDistance(lastAz, topoData.azimuth));
            }
            lastAz = topoData.azimuth;

            //if date is before end date and next date would be after
            if(pathJulianDate < pathJulianDateEnd && (pathJulianDate + dayIncrement) > pathJulianDateEnd)
            {
                //set to increment before end date
                pathJulianDate = pathJulianDateEnd - dayIncrement;
            }
        }

        //if hide slow and is slow, and have points
        if(hideSlow && azTravel < 1 )
        {
            //clear views
            pathViews.clear();
        }

        //return views
        return(pathViews);
    }

    @Override
    protected Integer[] doInBackground(Object... params)
    {
        int index;
        int length;
        boolean limitTravel = (Boolean)params[7];
        boolean autoSize = (Boolean)params[8];
        double julianDateStart = (Double)params[4];
        double julianDateEnd = (Double)params[5];
        double dayIncrement = (Double)params[6];
        boolean adjustTime = (Boolean)params[9];
        boolean hideSlow = (Boolean)params[10];
        Calculations.ObserverType observer = (Calculations.ObserverType)params[3];
        Current.ViewAngles.Item[] savedViewItems = (Current.ViewAngles.Item[])params[2];
        OrbitalPathBase[] orbitalViews = (OrbitalPathBase[])params[1];
        Context context = (Context)params[0];

        //update progress
        onProgressChanged(0, Globals.ProgressType.Started, null);

        //go through each orbital while not cancelled
        for(index = 0; index < orbitalViews.length && !this.isCancelled(); index++)
        {
            //get current path and satellite
            OrbitalPathBase currentPath = orbitalViews[index];
            ArrayList<OrbitalView> pathViews;

            //update progress
            publishProgress(index, Globals.ProgressType.Started);

            //get views
            pathViews = getViews(context, currentPath, savedViewItems, observer, julianDateStart, julianDateEnd, dayIncrement, limitTravel, adjustTime, hideSlow);
            length = pathViews.size();
            if(autoSize && (!hideSlow || length > 1) && length < 100)
            {
                //get a more accurate path
                pathViews = getViews(context, currentPath, savedViewItems, observer, julianDateStart, julianDateEnd, dayIncrement / 5, limitTravel, adjustTime, false);
            }

            //update progress
            onProgressChanged(index, (!this.isCancelled() ? Globals.ProgressType.Success : Globals.ProgressType.Failed), pathViews);
        }

        //update status
        if(!this.isCancelled())
        {
            needViews = false;
        }

        //return status
        return(new Integer[]{!this.isCancelled() ? Globals.ProgressType.Finished : Globals.ProgressType.Cancelled});
    }

    //Calls on progress changed listener
    private void onProgressChanged(int index, int progressType, ArrayList<OrbitalView> pathPoints)
    {
        //if there is a listener
        if(progressChangedListener != null)
        {
            progressChangedListener.onProgressChanged(index, progressType, pathPoints);
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
