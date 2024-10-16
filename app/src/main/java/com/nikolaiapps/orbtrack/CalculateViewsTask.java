package com.nikolaiapps.orbtrack;


import android.content.Context;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;


//Task to calculate view path information
public class CalculateViewsTask extends ThreadTask<Object, Integer, Integer[]>
{
    private final OnProgressChangedListener progressChangedListener;
    public boolean needViews;

    //Progress listener
    public interface OnProgressChangedListener
    {
        void onProgressChanged(int progressType, int satelliteIndex, CalculateService.ViewListItem item, ArrayList<OrbitalView> pathPoints);
    }

    //Viewing information at time
    public static class OrbitalView extends Calculations.TopographicDataType implements Parcelable
    {
        private final String timeZoneId;

        public final double julianDate;
        public final double illumination;
        public final Calendar gmtTime;
        public final String name;
        public final String phaseName;
        public final String timeString;
        public final Rect timeArea;

        public static final Creator<OrbitalView> CREATOR = new Parcelable.Creator<>()
        {
            @Override
            public OrbitalView createFromParcel(Parcel source)
            {
                double azimuth = source.readDouble();
                double elevation = source.readDouble();
                double rangeKm = source.readDouble();
                double julianDate = source.readDouble();
                long gmtTimeMs = source.readLong();

                return(new OrbitalView(null, azimuth, elevation, rangeKm, julianDate, (gmtTimeMs != 0 ? Globals.getGMTTime(gmtTimeMs) : null), source.readString(), source.readDouble(), source.readString(), source.readString(), source.readString()));
            }

            @Override
            public OrbitalView[] newArray(int size)
            {
                return(new OrbitalView[size]);
            }
        };

        private OrbitalView(Context context, double az, double el, double rangeKm, double julianDate, Calendar gmtTime, String zoneId, double illumination, String name, String phaseName, String timeString, boolean showSeconds)
        {
            if(zoneId == null)
            {
                zoneId = TimeZone.getDefault().getID();
            }

            this.azimuth = az;
            this.elevation = el;
            this.rangeKm = rangeKm;
            this.julianDate = julianDate;
            this.gmtTime = (gmtTime != null ? gmtTime : Globals.julianDateToCalendar(this.julianDate));
            this.timeZoneId = zoneId;
            if(timeString != null && !timeString.isEmpty())
            {
                this.timeString = timeString;
            }
            else
            {
                this.timeString = Globals.getDateTimeString(context, Globals.getCalendar(zoneId, this.gmtTime.getTimeInMillis()), showSeconds, false, true);
            }
            this.timeArea = new Rect(0, 0, 0, 0);
            this.timeArea.setEmpty();
            this.illumination = illumination;
            this.name = name;
            this.phaseName = phaseName;
        }
        public OrbitalView(Context context, double az, double el, double rangeKm, double julianDate, Calendar gmtTime, String zoneId, double illumination, String name, String phaseName, String timeString)
        {
            this(context, az, el, rangeKm, julianDate, gmtTime, zoneId, illumination, name, phaseName, timeString, false);
        }
        public OrbitalView(Context context, Calculations.TopographicDataType topographicData, double julianDate, String zoneId, double illumination, String name, String pn, boolean getTime)
        {
            this(context, topographicData.azimuth, topographicData.elevation, topographicData.rangeKm, julianDate, null, zoneId, illumination, name, pn, (getTime ? "" : "-"), false);
        }
        public OrbitalView(Context context, Calculations.TopographicDataType topographicData, double julianDate, String zoneId, String name, boolean showSeconds)
        {
            this(context, topographicData.azimuth, topographicData.elevation, topographicData.rangeKm, julianDate, null, zoneId, 0, name, null, null, showSeconds);
        }
        public OrbitalView(OrbitalView copyFrom)
        {
            this(null, copyFrom.azimuth, copyFrom.elevation, copyFrom.rangeKm, copyFrom.julianDate, copyFrom.gmtTime, copyFrom.timeZoneId, 0, copyFrom.name, null, copyFrom.timeString, false);
        }

        @Override
        public int describeContents()
        {
            return(0);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags)
        {
            dest.writeDouble(azimuth);
            dest.writeDouble(elevation);
            dest.writeDouble(rangeKm);
            dest.writeDouble(julianDate);
            dest.writeLong((gmtTime != null ? gmtTime.getTimeInMillis() : 0));
            dest.writeString(timeZoneId);
            dest.writeDouble(illumination);
            dest.writeString(name);
            dest.writeString(phaseName);
            dest.writeString(timeString);
        }

        public String getNameTimeString()
        {
            return((name != null ? (name + ", ") : "") + timeString);
        }
    }

    public static class ViewData extends Calculate.CalculateDataBase
    {
        public final float azimuth;
        public final float elevation;
        public final float rangeKm;

        public ViewData()
        {
            this.azimuth = Float.MAX_VALUE;
            this.elevation = Float.MAX_VALUE;
            this.rangeKm = Float.MAX_VALUE;
        }
        public ViewData(int noradId, OrbitalView data)
        {
            this.noradId = noradId;
            this.azimuth = (float)data.azimuth;
            this.elevation = (float)data.elevation;
            this.rangeKm = (float)data.rangeKm;
            this.illumination = data.illumination;
            this.julianDate = data.julianDate;
            this.phaseName =  data.phaseName;
        }
    }

    //Constructor
    public CalculateViewsTask(OnProgressChangedListener listener)
    {
        progressChangedListener = listener;
        needViews = true;
    }

    private ArrayList<OrbitalView> getViews(Context context, CalculateService.ViewListItem currentItem, int satelliteIndex, Calculate.ViewAngles.Item[] savedViewItems, Calculations.ObserverType observer, double pathJulianDateStart, double pathJulianDateEnd, double dayIncrement, boolean limitTravel, boolean adjustTime, boolean hideSlow, boolean getTimeString)
    {
        int index = 0;
        Calculations.TopographicDataType topographicData;
        Calculations.TopographicDataType oldTopographicData;
        Calculations.SatelliteObjectType viewSatellite = currentItem.satellite;
        double phase;
        double azTravel = 0;
        double viewJulianDate;
        double illumination = 0;
        double lastAz = Double.MAX_VALUE;
        double period = (viewSatellite != null ? viewSatellite.orbit.periodMinutes : 0);
        double beforeDayIncrement = (10.0 / Calculations.SecondsPerDay);                          //10 seconds
        double periodJulianEnd = (period > 0 ? (pathJulianDateStart + (period / Calculations.MinutesPerDay)) : Double.MAX_VALUE);
        final boolean isSun = (currentItem.getSatelliteNum() == Universe.IDs.Sun);
        final boolean isMoon = (currentItem.getSatelliteNum() == Universe.IDs.Moon);
        String phaseName = null;
        ArrayList<OrbitalView> pathViews = new ArrayList<>(0);

        //if want to adjust time and over 1 minute increment
        adjustTime = (adjustTime && dayIncrement > (1.0 / Calculations.MinutesPerDay));

        //if adjusting time
        if(adjustTime)
        {
            //set julian dates to 0 seconds at that time
            pathJulianDateStart = Globals.julianDateNoSeconds(pathJulianDateStart);
            pathJulianDateEnd = Globals.julianDateNoSeconds(pathJulianDateEnd);
        }

        //calculate points in path unless cancelled
        for(viewJulianDate = pathJulianDateStart; viewJulianDate <= pathJulianDateEnd && (!limitTravel || viewJulianDate <= periodJulianEnd || Math.abs(azTravel) <= 360) && !this.isCancelled(); viewJulianDate += dayIncrement)
        {
            //if adjusting time and not on first or last
            if(adjustTime && viewJulianDate > pathJulianDateStart && viewJulianDate < pathJulianDateEnd)
            {
                //set julian date to 0 seconds at that time
                viewJulianDate = Globals.julianDateNoSeconds(viewJulianDate);
            }

            //if there are saved items and julian date matches current
            if(savedViewItems != null && index < savedViewItems.length && satelliteIndex < savedViewItems[index].views.length && viewJulianDate == savedViewItems[index].julianDate)
            {
                //remember current item
                Calculate.ViewAngles.Item currentSavedItem = savedViewItems[index];

                //set position
                topographicData = new Calculations.TopographicDataType(currentSavedItem.views[satelliteIndex].azimuth, currentSavedItem.views[satelliteIndex].elevation, currentSavedItem.views[satelliteIndex].rangeKm);
                illumination = currentSavedItem.views[satelliteIndex].illumination;
                phaseName = currentSavedItem.views[satelliteIndex].phaseName;
            }
            else if(viewSatellite != null)
            {
                //calculate position
                Calculations.updateOrbitalPosition(viewSatellite, observer, viewJulianDate, false);
                topographicData = Calculations.getLookAngles(observer, viewSatellite, true);

                //if moon
                if(isMoon)
                {
                    //get illumination and phase name
                    phase = Universe.Moon.getPhase(Globals.julianDateToCalendar(viewJulianDate).getTimeInMillis());
                    illumination = Universe.Moon.getIllumination(phase);
                    phaseName = Universe.Moon.getPhaseName(context, phase);
                }
                //else if sun
                else if(isSun)
                {
                    //get old angles
                    Calculations.updateOrbitalPosition(viewSatellite, observer, viewJulianDate - beforeDayIncrement, false);
                    oldTopographicData = Calculations.getLookAngles(observer, viewSatellite, true);

                    //get phase name
                    phaseName = Universe.Sun.getPhaseName(context, topographicData.elevation, (topographicData.elevation > oldTopographicData.elevation));
                }
            }
            else
            {
                topographicData = new Calculations.TopographicDataType();
                phaseName = null;
                lastAz = Double.MAX_VALUE;
            }
            index++;

            //add view to list
            pathViews.add(new OrbitalView(context, topographicData, viewJulianDate, observer.timeZone.getID(), illumination, currentItem.getName(), phaseName, getTimeString));

            //update azimuth travel
            if(lastAz != Double.MAX_VALUE)
            {
                azTravel += Math.abs(Globals.degreeDistance(lastAz, topographicData.azimuth));
            }
            lastAz = topographicData.azimuth;

            //if date is before end date and next date would be after
            if(viewJulianDate < pathJulianDateEnd && (viewJulianDate + dayIncrement) > pathJulianDateEnd)
            {
                //set to increment before end date
                viewJulianDate = pathJulianDateEnd - dayIncrement;
            }
        }

        //if hide slow and is slow, and have points
        if(hideSlow && azTravel < 1)
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
        boolean limitTravel = (Boolean)params[9];
        boolean autoSize = (Boolean)params[10];
        boolean usingAllItems = (Boolean)params[3];
        double julianDateStart = (Double)params[5];
        double julianDateEnd = (Double)params[6];
        double dayIncrement = (Double)params[7];
        double largeDayIncrement = (Double)params[8];
        boolean adjustTime = (Boolean)params[11];
        boolean hideSlow = (Boolean)params[12];
        boolean getTimeString = (Boolean)params[13];
        Calculations.ObserverType observer = (Calculations.ObserverType)params[4];
        Calculate.ViewAngles.Item[] savedViewItems = (Calculate.ViewAngles.Item[])params[2];
        CalculateService.ViewListItem[] viewItems = (CalculateService.ViewListItem[])params[1];
        Context context = (Context)params[0];

        //update progress
        onProgressChanged(Globals.ProgressType.Started, Integer.MAX_VALUE, null, null);

        //go through each item while not cancelled
        for(index = 0; index < viewItems.length && !this.isCancelled(); index++)
        {
            //get current item and path
            CalculateService.ViewListItem currentItem = viewItems[index];
            boolean currentInFilter = (usingAllItems || currentItem.inFilter);
            double currentDayIncrement = (currentItem.getSatelliteNum() <= Universe.IDs.Polaris ? largeDayIncrement : dayIncrement);
            boolean skipAutoSize = (currentDayIncrement != dayIncrement);
            ArrayList<OrbitalView> pathViews = null;

            //update progress
            publishProgress(index, Globals.ProgressType.Started);

            //if current item in filter
            if(currentInFilter)
            {
                //get views
                pathViews = getViews(context, currentItem, index, savedViewItems, observer, julianDateStart, julianDateEnd, currentDayIncrement, limitTravel, adjustTime, hideSlow, getTimeString);
                length = pathViews.size();
                if(autoSize && !skipAutoSize && (!hideSlow || length > 1) && length < 100)
                {
                    //get a more accurate path
                    pathViews = getViews(context, currentItem, index, savedViewItems, observer, julianDateStart, julianDateEnd, currentDayIncrement / 5, limitTravel, adjustTime, false, getTimeString);
                }
            }

            //update status
            currentItem.viewCalculateFinished = (!this.isCancelled() && currentInFilter);

            //update progress
            onProgressChanged((currentItem.viewCalculateFinished ? Globals.ProgressType.Success : Globals.ProgressType.Failed), index, currentItem, pathViews);
        }

        //update status
        if(!this.isCancelled())
        {
            needViews = false;
        }

        //return status
        onProgressChanged((!this.isCancelled() ? Globals.ProgressType.Finished : Globals.ProgressType.Cancelled), Integer.MAX_VALUE, null, null);
        return(new Integer[]{!this.isCancelled() ? Globals.ProgressType.Finished : Globals.ProgressType.Cancelled});
    }

    //Calls on progress changed listener
    private void onProgressChanged(int progressType, int satelliteIndex, CalculateService.ViewListItem item, ArrayList<OrbitalView> pathPoints)
    {
        //if there is a listener
        if(progressChangedListener != null)
        {
            progressChangedListener.onProgressChanged(progressType, satelliteIndex, item, pathPoints);
        }
    }

    @Override
    protected  void onPostExecute(Integer[] result)
    {
        //finished
        onProgressChanged(result[0],0, null, null);
    }

    @Override
    protected void onCancelled(Integer[] result)
    {
        //cancelled
        onProgressChanged((result != null ? result[0] : Globals.ProgressType.Cancelled), 0, null, null);
    }
}
