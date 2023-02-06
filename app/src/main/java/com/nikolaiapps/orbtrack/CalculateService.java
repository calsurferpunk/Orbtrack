package com.nikolaiapps.orbtrack;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.lifecycle.MutableLiveData;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.TimeZone;


public class CalculateService extends NotifyService
{
    //Param Types
    @SuppressWarnings("SpellCheckingInspection")
    public static abstract class ParamTypes extends NotifyService.ParamTypes
    {
        static final String CalculateType = "calculateType";
        static final String PassItems = "passItems";
        static final String Observer = "observer";
        static final String MinEl = "minEl";
        static final String StartJulian = "startJulian";
        static final String EndJulian = "endJulian";
        static final String ApplyRefraction = "applyRefraction";
        static final String Id = "id";
        static final String Satellite = "satellite";
        static final String Illumination = "illum";
        static final String PhaseName = "pn";
        static final String AzimuthStart = "azStart";
        static final String AzimuthEnd = "azEnd";
        static final String AzimuthTravel = "azTravel";
        static final String ElevationMax = "elMax";
        static final String ClosestAzimuth = "closestAz";
        static final String ClosestElevation = "closestEl";
        static final String Calculating = "calculating";
        static final String FoundPass = "foundPass";
        static final String FinishedCalculating = "finishedCalc";
        static final String FoundPassStart = "foundPassStart";
        static final String PassProgress = "passProgress";
        static final String ZoneStart = "zoneStart";
        static final String TimeStart = "timeStart";
        static final String ZoneEnd = "zoneEnd";
        static final String TimeEnd = "timeEnd";
        static final String Duration = "duration";
        static final String Views = "views";
        static final String Views2 = "views2";
        static final String PassData = "passData";
        static final String NotifyType = "notifyType";
        static final String Latitude = "latitude";
        static final String Longitude = "longitude";
        static final String Altitude = "altitude";
        static final String ZoneId = "zoneId";
    }

    //Calculate types
    public static abstract class CalculateType
    {
        static final byte OrbitalsPasses = 0;
        static final byte OrbitalPasses = 1;
        static final byte OrbitalPass = 2;
        static final byte OrbitalIntersections = 3;
        static final byte FullMoon = 4;
        static final byte CalculateCount = 5;
    }

    //Alarm notify settings
    public static class AlarmNotifySettings
    {
        boolean nextOnly;
        public int noradId;
        public long timeMs;
        public final Calculations.ObserverType location;

        public AlarmNotifySettings()
        {
            nextOnly = false;
            noradId = Integer.MIN_VALUE;
            timeMs = 0;
            location = new Calculations.ObserverType();
        }

        public boolean isEnabled()
        {
            return(timeMs != 0);
        }
    }

    //Item
    public static class PassData extends Selectable.ListItem implements Parcelable
    {
        public int id2;
        public Calculations.SatelliteObjectType satellite;
        public Calculations.SatelliteObjectType satellite2;
        public boolean passStartFound;
        public boolean passCalculated;
        public boolean passCalculating;
        public boolean passCalculateFinished;
        public final boolean showPathProgress;
        public double passAzStart;
        public double passAzEnd;
        public double passAzTravel;
        public double passElMax;
        public double passClosestAz;
        public double passClosestEl;
        double illumination;
        public Calendar passTimeStart;
        public Calendar passTimeEnd;
        public String passDuration;
        public String name;
        public String ownerCode;
        public String owner2Code;
        public String phaseName;
        public byte orbitalType;
        public byte orbital2Type;
        public CalculateViewsTask.OrbitalView[] passViews;
        public CalculateViewsTask.OrbitalView[] passViews2;
        public static final Creator<PassData> CREATOR =  new Parcelable.Creator<PassData>()
        {
            @Override
            public PassData createFromParcel(Parcel source)
            {
                Bundle bundle = source.readBundle(getClass().getClassLoader());
                if(bundle == null)
                {
                    bundle = new Bundle();
                }

                return(new PassData(bundle.getInt(ParamTypes.Id), bundle.getDouble(ParamTypes.AzimuthStart), bundle.getDouble(ParamTypes.AzimuthEnd), bundle.getDouble(ParamTypes.AzimuthTravel), bundle.getDouble(ParamTypes.ElevationMax), bundle.getDouble(ParamTypes.ClosestAzimuth), bundle.getDouble(ParamTypes.ClosestElevation), bundle.getBoolean(ParamTypes.Calculating), bundle.getBoolean(ParamTypes.FoundPass), bundle.getBoolean(ParamTypes.FinishedCalculating), bundle.getBoolean(ParamTypes.FoundPassStart), bundle.getBoolean(ParamTypes.PassProgress), bundle.getString(ParamTypes.ZoneStart), bundle.getLong(ParamTypes.TimeStart), bundle.getString(ParamTypes.ZoneEnd), bundle.getLong(ParamTypes.TimeEnd), bundle.getString(ParamTypes.Duration), bundle.getParcelableArray(ParamTypes.Views), bundle.getParcelableArray(ParamTypes.Views2), bundle.getParcelable(ParamTypes.Satellite), bundle.getDouble(ParamTypes.Illumination), bundle.getString(ParamTypes.PhaseName)));
            }

            @Override
            public PassData[] newArray(int size)
            {
                return(new PassData[size]);
            }
        };

        protected PassData(int index, double azStart, double azEnd, double azTravel, double elMax, double closestAz, double closestEl, boolean calculating, boolean foundPass, boolean finishedCalculating, boolean foundPassStart, boolean usePathProgress, Calendar startTime, Calendar endTime, String duration, Parcelable[] views, Parcelable[] views2, Calculations.SatelliteObjectType sat, Calculations.SatelliteObjectType sat2, double illumination, String phaseName)
        {
            super((sat != null ? sat.getSatelliteNum() : Universe.IDs.None), index, false, false, false, false);
            this.satellite = (sat != null ? new Calculations.SatelliteObjectType(sat) : null);
            this.satellite2 = (sat2 != null ? new Calculations.SatelliteObjectType(sat2) : null);
            this.id2 = (sat2 != null ? sat2.getSatelliteNum() : Universe.IDs.Invalid);
            this.passAzStart = azStart;
            this.passAzEnd = azEnd;
            this.passAzTravel = azTravel;
            this.passElMax = elMax;
            this.passClosestAz = closestAz;
            this.passClosestEl = closestEl;
            this.passStartFound = foundPassStart;
            this.passCalculating = calculating;
            this.passCalculated = foundPass;
            this.passCalculateFinished = finishedCalculating;
            this.showPathProgress = usePathProgress;
            this.passTimeStart = startTime;
            this.passTimeEnd = endTime;
            this.passDuration = duration;
            this.illumination = illumination;
            this.name = "";
            this.ownerCode = null;
            this.owner2Code = null;
            this.orbitalType = this.orbital2Type = Database.OrbitalType.Satellite;
            this.phaseName = phaseName;
            this.passViews = copyViewArray(views);
            this.passViews2 = copyViewArray(views2);
        }
        public PassData(int index, double azStart, double azEnd, double azTravel, double elMax, double closestAz, double closestEl, boolean calculating, boolean foundPass, boolean finishedCalculating, boolean foundPassStart, boolean usePathProgress, Calendar startTime, Calendar endTime, String duration, Parcelable[] views, Parcelable[] views2, Calculations.SatelliteObjectType sat, double illumination, String phaseName)
        {
            this(index, azStart, azEnd, azTravel, elMax, closestAz, closestEl, calculating, foundPass, finishedCalculating, foundPassStart, usePathProgress, startTime, endTime, duration, views, views2, sat, null, illumination, phaseName);
        }
        public PassData(int index, double azStart, double azEnd, double azTravel, double elMax, double closestAz, double closestEl, boolean calculating, boolean foundPass, boolean finishedCalculating, boolean foundPassStart, boolean useListPathProgress, String zoneStart, long timeStart, String zoneEnd, long timeEnd, String duration, Parcelable[] views, Parcelable[] views2, Calculations.SatelliteObjectType sat, double illumination, String phaseName)
        {
            this(index, azStart, azEnd, azTravel, elMax, closestAz, closestEl, calculating, foundPass, finishedCalculating, foundPassStart, useListPathProgress, Globals.getCalendar(zoneStart, timeStart), Globals.getCalendar(zoneEnd, timeEnd), duration, views, views2, sat, illumination, phaseName);
        }
        public PassData(int index, Database.SatelliteData currentSatellite)
        {
            this(index, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, false, false, false, false, false, null, null, "", null, null, null, 0, null);

            if(currentSatellite != null && currentSatellite.satellite != null)
            {
                id = currentSatellite.getSatelliteNum();
                name = currentSatellite.getName();
                ownerCode = currentSatellite.getOwnerCode();
                orbitalType = orbital2Type = currentSatellite.getOrbitalType();
                satellite = currentSatellite.satellite;
            }
        }
        public PassData(int index, Database.SatelliteData satellite1, Database.SatelliteData satellite2)
        {
            this(index, satellite1);

            boolean haveSatellite2 = (satellite2 != null);
            this.satellite2 = (haveSatellite2 ? new Calculations.SatelliteObjectType(satellite2.satellite) : null);
            if(haveSatellite2)
            {
                id2 = satellite2.getSatelliteNum();
                owner2Code = satellite2.getOwnerCode();
            }
        }
        public PassData(PassData copyFrom)
        {
            this(copyFrom.listIndex, copyFrom.passAzStart, copyFrom.passAzEnd, copyFrom.passAzTravel, copyFrom.passElMax, copyFrom.passClosestAz, copyFrom.passClosestEl, copyFrom.passCalculating, copyFrom.passCalculated, copyFrom.passCalculateFinished, copyFrom.passStartFound, copyFrom.showPathProgress, copyFrom.passTimeStart, copyFrom.passTimeEnd, copyFrom.passDuration, copyFrom.passViews, copyFrom.passViews2, copyFrom.satellite, copyFrom.satellite2, copyFrom.illumination, copyFrom.phaseName);
            id = copyFrom.id;
            id2 = copyFrom.id2;
            name = copyFrom.name;
            ownerCode = copyFrom.ownerCode;
            owner2Code = copyFrom.owner2Code;
            orbitalType = copyFrom.orbitalType;
            orbital2Type = copyFrom.orbital2Type;
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

            bundle.putInt(ParamTypes.Id, id);
            bundle.putParcelable(ParamTypes.Satellite, satellite);
            bundle.putDouble(ParamTypes.AzimuthStart, passAzStart);
            bundle.putDouble(ParamTypes.AzimuthEnd, passAzEnd);
            bundle.putDouble(ParamTypes.AzimuthTravel, passAzTravel);
            bundle.putDouble(ParamTypes.ElevationMax, passElMax);
            bundle.putDouble(ParamTypes.ClosestAzimuth, passClosestAz);
            bundle.putDouble(ParamTypes.ClosestElevation, passClosestEl);
            bundle.putBoolean(ParamTypes.Calculating, passCalculating);
            bundle.putBoolean(ParamTypes.FoundPass, passCalculated);
            bundle.putBoolean(ParamTypes.FinishedCalculating, passCalculateFinished);
            bundle.putBoolean(ParamTypes.FoundPassStart, passStartFound);
            bundle.putBoolean(ParamTypes.PassProgress, showPathProgress);
            bundle.putString(ParamTypes.ZoneStart, passTimeStart != null ? passTimeStart.getTimeZone().getID() : "");
            bundle.putLong(ParamTypes.TimeStart, passTimeStart != null ? passTimeStart.getTimeInMillis() : 0);
            bundle.putString(ParamTypes.ZoneEnd, passTimeEnd != null ? passTimeEnd.getTimeZone().getID() : "");
            bundle.putLong(ParamTypes.TimeEnd, passTimeEnd != null ? passTimeEnd.getTimeInMillis() : 0);
            bundle.putString(ParamTypes.Duration, passDuration);
            bundle.putDouble(ParamTypes.Illumination, illumination);
            bundle.putString(ParamTypes.PhaseName, phaseName);
            bundle.putParcelableArray(ParamTypes.Views, passViews);
            bundle.putParcelableArray(ParamTypes.Views2, passViews2);
            bundle.putParcelable(ParamTypes.Satellite, satellite);

            dest.writeBundle(bundle);
        }

        public boolean equals(PassData otherItem)
        {
            return(otherItem != null && id == otherItem.id && ((satellite2 == null && otherItem.satellite2 == null) || (satellite2 != null && otherItem.satellite2 != null && satellite2.getSatelliteNum() == otherItem.satellite2.getSatelliteNum())));
        }

        private CalculateViewsTask.OrbitalView[] copyViewArray(CalculateViewsTask.OrbitalView[] copyFrom)
        {
            int index;
            int length;
            CalculateViewsTask.OrbitalView[] newArray = null;

            if(copyFrom != null)
            {
                length = copyFrom.length;
                newArray = new CalculateViewsTask.OrbitalView[length];
                for(index = 0; index < length; index++)
                {
                    newArray[index] = new CalculateViewsTask.OrbitalView(copyFrom[index]);
                }
            }

            return(newArray);
        }
        private CalculateViewsTask.OrbitalView[] copyViewArray(Parcelable[] copyFrom)
        {
            int index;
            int length;
            CalculateViewsTask.OrbitalView[] newArray = null;

            if(copyFrom != null)
            {
                length = copyFrom.length;
                newArray = new CalculateViewsTask.OrbitalView[length];
                for(index = 0; index < length; index++)
                {
                    newArray[index] = (CalculateViewsTask.OrbitalView)copyFrom[index];
                }
            }

            return(newArray);
        }

        public void copyPass(PassData otherItem)
        {
            passAzStart = otherItem.passAzStart;
            passAzEnd = otherItem.passAzEnd;
            passAzTravel = otherItem.passAzTravel;
            passElMax = otherItem.passElMax;
            passClosestAz = otherItem.passClosestAz;
            passClosestEl = otherItem.passClosestEl;
            passStartFound = otherItem.passStartFound;
            passCalculated = otherItem.passCalculated;
            passCalculating = otherItem.passCalculating;
            passCalculateFinished = otherItem.passCalculateFinished;
            passTimeStart = otherItem.passTimeStart;
            passTimeEnd = otherItem.passTimeEnd;
            passDuration = otherItem.passDuration;
            illumination = otherItem.illumination;
            phaseName = otherItem.phaseName;
            passViews = copyViewArray(otherItem.passViews);
            passViews2 = copyViewArray(otherItem.passViews2);
        }

        public void clearPass()
        {
            passCalculated = passCalculating = passCalculateFinished = passStartFound = false;
            passTimeStart = null;
            passTimeEnd = null;
            passDuration = "";
            illumination = 0;
            phaseName = null;
        }

        public boolean inUnknownPassTimeStartNow()
        {
            Calendar timeNow = Globals.getGMTTime();
            return(!passStartFound && passTimeStart != null && timeNow.getTimeInMillis() >= passTimeStart.getTimeInMillis() && (passTimeEnd == null || timeNow.before(passTimeEnd)));
        }

        public boolean isKnownPassElevationMax()
        {
            return(passElMax != Double.MAX_VALUE);
        }

        public long getMidPass()
        {
            return(passTimeStart != null && passTimeEnd != null ? ((passTimeStart.getTimeInMillis() + passTimeEnd.getTimeInMillis()) / 2) : System.currentTimeMillis());
        }

        public long getPassDurationMs()
        {
            return(passTimeStart != null && passTimeEnd != null ? (passTimeEnd.getTimeInMillis() - passTimeStart.getTimeInMillis()) : 0);
        }

        public float getPassProgressPercent(Calendar timeNow)
        {
            long elapsedMs;
            long passDurationMs = getPassDurationMs();
            float elapsedPercent = Float.MAX_VALUE;

            //if start time is set, progress displays exist, and duration set
            if(passTimeStart != null && timeNow.after(passTimeStart) && passDurationMs > 0)
            {
                //if before end
                if(timeNow.before(passTimeEnd))
                {
                    elapsedMs = passTimeEnd.getTimeInMillis() - timeNow.getTimeInMillis();
                    elapsedPercent = (100 - ((elapsedMs / (float)passDurationMs) * 100));
                }
                else
                {
                    elapsedPercent = 100;
                }
            }

            return(elapsedPercent);
        }
    }

    //Notify receiver
    public static class NotifyReceiver extends NotifyService.NotifyReceiver
    {
        private static final int CalculateMultiOrbitalPassesID = 1;
        private static final int CalculateSinglePassesID = 2;
        private static final int RetryPassesID = 3;
        private static final int DismissPassesID = 4;
        private static final int PassStartID = 5;
        private static final int PassStartDismissID = 6;
        private static final int PassEndID = 7;
        private static final int PassEndDismissID = 8;
        private static final int FullMoonStartID = 9;
        private static final int FullMoonStartDismissID = 10;
        private static final int FullMoonEndID = 11;
        private static final int FullMoonEndDismissID = 12;
        private static final int PassSettingsID = 13;

        private static boolean firstUpdate = false;
        private static LocationReceiver locationReceiver;

        @Override
        protected void onBootCompleted(Context context, long timeNowMs, AlarmManager manager)
        {
            Database.DatabaseLocation location = Database.getLocation(context);

            //update with current or existing locations
            firstUpdate = true;
            updateNotifyLocations(context, null, (location.locationType == Database.LocationType.Current), true, true);
        }

        @Override
        protected void onSettings(Context context, Intent intent)
        {
            int noradId = intent.getIntExtra(ParamTypes.Id, Integer.MIN_VALUE);
            double latitude = intent.getDoubleExtra(ParamTypes.Latitude, Double.MAX_VALUE);
            double longitude = intent.getDoubleExtra(ParamTypes.Longitude, Double.MAX_VALUE);
            double altitude = intent.getDoubleExtra(ParamTypes.Altitude, Double.MAX_VALUE);
            String zoneId = intent.getStringExtra(ParamTypes.ZoneId);

            //if a valid norad ID
            if(noradId != Integer.MIN_VALUE)
            {
                //start settings activity
                NotifySettingsActivity.show(context, null, noradId, new Calculations.ObserverType(zoneId, new Calculations.GeodeticDataType(latitude, longitude, altitude, 0, 0)));
            }
        }

        @Override
        protected void onRunService(final Context context, Intent intent)
        {
            final byte notifyType = intent.getByteExtra(ParamTypes.NotifyType, Byte.MAX_VALUE);
            boolean isStart = (notifyType == Globals.NotifyType.PassStart || notifyType == Globals.NotifyType.FullMoonStart);
            boolean isFullMoon = (notifyType == Globals.NotifyType.FullMoonStart || notifyType == Globals.NotifyType.FullMoonEnd);
            int noradId = intent.getIntExtra(ParamTypes.Id, Integer.MIN_VALUE);
            long startTimeMs = intent.getLongExtra(ParamTypes.TimeStart, 0);
            long endTimeMs = intent.getLongExtra(ParamTypes.TimeEnd, 0);
            double azStart = intent.getDoubleExtra(ParamTypes.AzimuthStart, Double.MAX_VALUE);
            double azEnd = intent.getDoubleExtra(ParamTypes.AzimuthEnd, Double.MAX_VALUE);
            double elMax = intent.getDoubleExtra(ParamTypes.ElevationMax, Double.MAX_VALUE);
            TimeZone startZone = (TimeZone)intent.getSerializableExtra(ParamTypes.ZoneStart);
            TimeZone endZone = (TimeZone)intent.getSerializableExtra(ParamTypes.ZoneEnd);
            String duration;
            String notifyTitle = null;
            String notifyMessage = null;
            String notifyDetails = null;
            String notifyChannelId;
            Calendar startTime = (startTimeMs != 0 ? Globals.getCalendar((startZone != null ? startZone.getID() : TimeZone.getDefault().getID()), startTimeMs) : null);
            Calendar endTime = (endTimeMs != 0 ? Globals.getCalendar((endZone != null ? endZone.getID() : TimeZone.getDefault().getID()), endTimeMs) : null);
            Resources res = context.getResources();
            Intent dismissIntent;
            Intent settingsIntent;
            NotificationManagerCompat notifyManager;
            NotificationCompat.Builder notifyBuilder;
            AlarmNotifySettings notifySettings;
            Calculations.ObserverType notifyLocation;
            Database.DatabaseSatellite orbital = Database.getOrbital(context, noradId);

            //handle based on notify type
            switch(notifyType)
            {
                case Globals.NotifyType.PassStart:
                case Globals.NotifyType.PassEnd:
                case Globals.NotifyType.FullMoonStart:
                case Globals.NotifyType.FullMoonEnd:
                    //if found orbital
                    if(orbital != null)
                    {
                        //set title and message
                        notifyTitle = (isFullMoon ? res.getString(isStart ? R.string.title_full_moon_start : R.string.title_full_moon_end) : (orbital.getName() + " " + res.getString(isStart ? R.string.title_pass_start : R.string.title_pass_end)));
                        duration = Globals.getTimeBetween(context, startTime, endTime);
                        notifyMessage = Globals.getDateString(context, (isStart ? startTime : endTime), (isStart ? startZone : endZone), false, true, false);
                        notifyDetails = notifyMessage + "\r\n" + Globals.Symbols.Time + duration + "\r\n" + Globals.getAzimuthDirectionString(res, (isStart ? azStart : azEnd)) + ", " + Globals.getDegreeString(elMax) + " " + res.getString(R.string.abbrev_elevation) + " " + res.getString(R.string.title_max);
                    }
                    break;
            }

            //if notify title is set
            if(notifyTitle != null)
            {
                //get manager and settings
                notifyManager = NotificationManagerCompat.from(context);
                notifySettings = Settings.getNotifyPassSettings(context, noradId, notifyType);
                notifyLocation = notifySettings.location;

                //create dismiss intent
                dismissIntent = new Intent(context, NotifyReceiver.class);
                dismissIntent.setAction(NotifyReceiver.DismissAction);
                dismissIntent.putExtra(ParamTypes.Id, noradId);
                dismissIntent.putExtra(ParamTypes.NotifyType, notifyType);

                //create settings intent
                settingsIntent = new Intent(context, NotifyReceiver.class);
                settingsIntent.setAction(NotifyReceiver.SettingsAction);
                settingsIntent.putExtra(ParamTypes.Id, noradId);
                settingsIntent.putExtra(ParamTypes.NotifyType, notifyType);
                settingsIntent.putExtra(ParamTypes.Latitude, notifyLocation.geo.latitude);
                settingsIntent.putExtra(ParamTypes.Longitude, notifyLocation.geo.longitude);
                settingsIntent.putExtra(ParamTypes.Altitude, notifyLocation.geo.altitudeKm);
                settingsIntent.putExtra(ParamTypes.ZoneId, notifyLocation.timeZone.getID());

                //remove any old notification
                notifyManager.cancel(getNotifyID(noradId, (isFullMoon ? (notifyType == Globals.NotifyType.FullMoonStart ? Globals.NotifyType.FullMoonEnd : Globals.NotifyType.FullMoonStart) : (notifyType == Globals.NotifyType.PassStart ? Globals.NotifyType.PassEnd : Globals.NotifyType.PassStart))));

                //show notification
                notifyChannelId = Globals.getChannelId(Globals.ChannelIds.Pass);
                Globals.createNotifyChannel(context, notifyChannelId);
                notifyBuilder = Globals.createNotifyBuilder(context, notifyChannelId, Globals.getOrbitalIconID(context, noradId, orbital.orbitalType));
                notifyBuilder.setContentTitle(notifyTitle).setContentText(notifyMessage).setStyle(new NotificationCompat.BigTextStyle().bigText(notifyDetails));
                notifyBuilder.addAction(new NotificationCompat.Action(0, res.getString(R.string.title_settings), Globals.getPendingBroadcastIntent(context, getSettingsID(noradId), settingsIntent, PendingIntent.FLAG_UPDATE_CURRENT)));
                notifyBuilder.addAction(new NotificationCompat.Action(0, res.getString(R.string.title_dismiss), Globals.getPendingBroadcastIntent(context, getDismissID(noradId, notifyType), dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT)));
                Globals.showNotification(context, getNotifyID(noradId, notifyType), notifyBuilder.build());

                //if for this pass only
                if(notifySettings.nextOnly)
                {
                    //remove
                    Settings.setNotifyPassNext(context, noradId, null, false, 0, notifyType);
                }
                //else if a pass ending time found
                else if(endTimeMs > startTimeMs)
                {
                    //set next starting time
                    setNextPassNotify(context, notifySettings, notifyType, orbital, endTimeMs);
                }
            }
        }

        @Override
        protected void onCloseNotification(Context context, Intent intent, NotificationManagerCompat manager)
        {
            int noradId = intent.getIntExtra(ParamTypes.Id, Integer.MIN_VALUE);
            byte notifyType = intent.getByteExtra(ParamTypes.NotifyType, Byte.MAX_VALUE);
            manager.cancel(notifyType != Byte.MAX_VALUE ? getNotifyID(noradId, notifyType) : getCalculateID(getCalculateType(intent)));
        }

        //Gets the calculate type from the given intent
        private byte getCalculateType(Intent intent)
        {
            return(intent.getByteExtra(ParamTypes.CalculateType, Byte.MAX_VALUE));
        }

        //Gets update ID for given update type
        private static int getCalculateID(byte calculateType)
        {
            return(calculateType == CalculateType.OrbitalsPasses ? CalculateMultiOrbitalPassesID : CalculateSinglePassesID);
        }

        //Gets the notify ID for the given norad ID and notify type
        private static int getNotifyID(int noradId, byte notifyType)
        {
            int id = 0;

            switch(notifyType)
            {
                case Globals.NotifyType.PassStart:
                    id = PassStartID;
                    break;

                case Globals.NotifyType.PassEnd:
                    id = PassEndID;
                    break;

                case Globals.NotifyType.FullMoonStart:
                    id = FullMoonStartID;
                    break;

                case Globals.NotifyType.FullMoonEnd:
                    id = FullMoonEndID;
                    break;
            }

            return(id + (noradId * 100));
        }

        //Gets the dismiss ID for the given norad ID and notify type
        private static int getDismissID(int noradId, byte notifyType)
        {
            int id = 0;

            switch(notifyType)
            {
                case Globals.NotifyType.PassStart:
                    id = PassStartDismissID;
                    break;

                case Globals.NotifyType.PassEnd:
                    id = PassEndDismissID;
                    break;

                case Globals.NotifyType.FullMoonStart:
                    id = FullMoonStartDismissID;
                    break;

                case Globals.NotifyType.FullMoonEnd:
                    id = FullMoonEndDismissID;
                    break;
            }

            return(id + (noradId * 100));
        }

        //Gets the settings ID for the given norad ID
        private static int getSettingsID(int noradId)
        {
            return(PassSettingsID + (noradId * 100));
        }

        //Gets a notify pending intent
        private static PendingIntent getNotifyPendingIntent(Context context, int noradId, TimeZone zone, PassData pass, byte notifyType)
        {
            Intent notifyIntent = new Intent(context, NotifyReceiver.class);
            PendingIntent notifyPendingIntent;

            //create and return intent
            notifyIntent.putExtra(ParamTypes.Id, noradId);
            notifyIntent.putExtra(ParamTypes.NotifyType, notifyType);
            if(pass != null)
            {
                notifyIntent.putExtra(ParamTypes.AzimuthStart, pass.passAzStart);
                notifyIntent.putExtra(ParamTypes.AzimuthEnd, pass.passAzEnd);
                if(pass.passTimeStart != null)
                {
                    notifyIntent.putExtra(ParamTypes.TimeStart, pass.passTimeStart.getTimeInMillis());
                    notifyIntent.putExtra(ParamTypes.ZoneStart, zone);
                }
                if(pass.passTimeEnd != null)
                {
                    notifyIntent.putExtra(ParamTypes.TimeEnd, pass.passTimeEnd.getTimeInMillis());
                    notifyIntent.putExtra(ParamTypes.ZoneEnd, zone);
                }
                notifyIntent.putExtra(ParamTypes.ElevationMax, pass.passElMax);
            }
            notifyPendingIntent = Globals.getPendingBroadcastIntent(context, getNotifyID(noradId, notifyType), notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            return(notifyPendingIntent);
        }

        //Set next pass notify
        private static void setNextPassNotify(final Context context, final AlarmNotifySettings notifySettings, final byte notifyType, Database.DatabaseSatellite orbital, long endTimeMs)
        {
            byte calculateType = CalculateType.OrbitalPass;
            double nextStartJulianDate;
            Calendar nextStartTime;

            //get next starting time
            nextStartTime = Globals.getGMTTime(endTimeMs + 10000);      //10 seconds after last pass ending time
            nextStartJulianDate = Calculations.julianDateCalendar(nextStartTime);

            //possibly get calculate type
            switch(notifyType)
            {
                case Globals.NotifyType.FullMoonStart:
                case Globals.NotifyType.FullMoonEnd:
                    calculateType = CalculateType.FullMoon;
                    break;
            }

            //set again for next
            calculateOrbitalPass(context, calculateType, Integer.MAX_VALUE, new Database.SatelliteData(orbital), notifySettings.location, 0, nextStartJulianDate, true, new CalculateListener()
            {
                @Override
                public void onCalculated(int progressType, PassData pass)
                {
                    //set to notify
                    setNotifyState(context, notifySettings.location, pass, notifyType, notifySettings, false, true);
                }
            });
        }

        //Sets notify states
        public static void setNotifyStates(final Context context, final Calculations.ObserverType location, final AlarmNotifySettings[] notifySettings, final boolean[] notifyUsing, final boolean showPast)
        {
            byte index;
            int noradId = Integer.MAX_VALUE;
            boolean usingAny = false;
            boolean usingPass = false;
            boolean usingFullMoon = false;
            double julianStart;

            //go through each notify type
            for(index = 0; index < Globals.NotifyType.NotifyCount; index++)
            {
                //update status
                if(notifyUsing[index])
                {
                    //if norad ID not set yet
                    if(noradId == Integer.MAX_VALUE)
                    {
                        //use current
                        noradId = notifySettings[index].noradId;
                    }

                    //update using
                    usingAny = true;
                    switch(index)
                    {
                        case Globals.NotifyType.PassStart:
                        case Globals.NotifyType.PassEnd:
                            usingPass = true;
                            break;

                        case Globals.NotifyType.FullMoonStart:
                        case Globals.NotifyType.FullMoonEnd:
                            usingFullMoon = true;
                            break;
                    }
                }

                //stop any existing
                setNotifyState(context, null, null, index, notifySettings[index], false, false);
            }

            //if using any
            if(usingAny)
            {
                //get current orbital
                Database.SatelliteData currentOrbital = new Database.SatelliteData(context, noradId);

                //get parameters
                julianStart = Globals.getJulianDate();

                //if using pass
                if(usingPass)
                {
                    //update pass notify
                    calculateOrbitalPass(context, CalculateType.OrbitalPass, Integer.MAX_VALUE, currentOrbital, location, 0, julianStart, true, createNotifyCalculateReceiver(context, location, notifyUsing, notifySettings, showPast, Globals.NotifyType.PassStart, Globals.NotifyType.PassEnd));
                }

                //if using full moon
                if(usingFullMoon)
                {
                    //update full moon notify
                    calculateOrbitalPass(context, CalculateType.FullMoon, Integer.MAX_VALUE, currentOrbital, location, 0, julianStart, true, createNotifyCalculateReceiver(context, location, notifyUsing, notifySettings, showPast, Globals.NotifyType.FullMoonStart, Globals.NotifyType.FullMoonEnd));
                }
            }
        }

        //Sets notify state
        private static void setNotifyState(Context context, Calculations.ObserverType location, PassData pass, byte notifyType, AlarmNotifySettings settings, boolean showPast, boolean using)
        {
            int noradId = settings.noradId;
            long passTimeMs;
            boolean passInFuture;
            Calendar passTime;
            PendingIntent notifyPendingIntent;
            AlarmManager manager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

            //if using notify
            if(using)
            {
                //get pass time
                passTime = (pass == null ? null : (notifyType == Globals.NotifyType.PassStart || notifyType == Globals.NotifyType.FullMoonStart) ? pass.passTimeStart : pass.passTimeEnd);
                passTimeMs = (passTime != null ? passTime.getTimeInMillis() : Long.MAX_VALUE);
                passInFuture = (passTimeMs >= Globals.getGMTTime().getTimeInMillis());

                //update settings
                Settings.setNotifyPassNext(context, noradId, location, settings.nextOnly, passTimeMs, notifyType);

                //if manager still exists and valid pass time
                if(manager != null && passTimeMs != Long.MAX_VALUE)
                {
                    //pass is in future or showing past
                    if(passInFuture || showPast)
                    {
                        //set notify alarm
                        notifyPendingIntent = getNotifyPendingIntent(context, noradId, location.timeZone, pass, notifyType);
                        Globals.setAlarm(context, manager, passTimeMs, notifyPendingIntent, true);
                    }
                    //else if pass end time is known
                    else if(pass.passTimeEnd != null)
                    {
                        //set for next pass
                        setNextPassNotify(context, Settings.getNotifyPassSettings(context, noradId, notifyType), notifyType, Database.getOrbital(context, noradId), pass.passTimeEnd.getTimeInMillis());
                    }
                }
            }
            //else if manager exists
            else if(manager != null)
            {
                //cancel notify
                Settings.setNotifyPassNext(context, noradId, null, false, 0, notifyType);
                manager.cancel(getNotifyPendingIntent(context, noradId, null, null, notifyType));
            }
        }

        //Update notifications
        private static void updateNotify(Context context, Calculations.ObserverType location, boolean allowPast)
        {
            byte index;
            int noradId;
            boolean usingLocation = (location != null);
            boolean[] notifyUsing = new boolean[Globals.NotifyType.NotifyCount];
            AlarmNotifySettings[] notifySettings = new AlarmNotifySettings[Globals.NotifyType.NotifyCount];
            AlarmNotifySettings[][] allNotifySettings = Settings.getNotifyPassSettings(context);

            //go through each settings group
            for(AlarmNotifySettings[] currentGroupSettings : allNotifySettings)
            {
                //get norad ID
                noradId = (currentGroupSettings.length == Globals.NotifyType.NotifyCount ? currentGroupSettings[0].noradId : Universe.IDs.Invalid);

                //if a valid norad ID
                if(noradId != Universe.IDs.Invalid)
                {
                    boolean setNotify = false;
                    Calculations.ObserverType setLocation = null;

                    //go through each notify type
                    for(index = 0; index < Globals.NotifyType.NotifyCount; index++)
                    {
                        //remember current settings and location
                        AlarmNotifySettings currentSettings = currentGroupSettings[index];
                        Calculations.ObserverType currentLocation = (usingLocation ? location : currentSettings.location);

                        //update status
                        notifySettings[index] = currentSettings;
                        notifyUsing[index] = currentSettings.isEnabled();

                        //if -using- and --on first update- or -location changed enough---
                        if(notifyUsing[index] && (firstUpdate || Math.abs(Globals.latitudeDistance(currentSettings.location.geo.latitude, currentLocation.geo.latitude)) >= 0.2 || Math.abs(Globals.longitudeDistance(currentSettings.location.geo.longitude, currentLocation.geo.longitude)) >= 0.2))
                        {
                            //if using location, save now since not updated until notify time
                            if(usingLocation)
                            {
                                //update location
                                Settings.setNotifyPassNext(context, noradId, location, currentSettings.nextOnly, currentSettings.timeMs, index);
                            }

                            //need to set states
                            if(setLocation == null)
                            {
                                setLocation = currentLocation;
                            }
                            setNotify = true;
                        }
                    }

                    //if need to set notify
                    if(setNotify)
                    {
                        //set notify
                        setNotifyStates(context, setLocation, notifySettings, notifyUsing, firstUpdate || allowPast);
                    }
                }
            }

            //reset
            firstUpdate = false;
        }

        //Create notify calculate receiver
        private static CalculateListener createNotifyCalculateReceiver(final Context context, final Calculations.ObserverType location, final boolean[] notifyUsing, final AlarmNotifySettings[] notifySettings, final boolean showPast, final byte... notifyTypes)
        {
            return(new CalculateListener()
            {
                @Override
                public void onCalculated(int progressType, PassData pass)
                {
                    //go through each notify
                    for(byte currentNotify : notifyTypes)
                    {
                        //set to notify if using
                        if(notifyUsing[currentNotify])
                        {
                            setNotifyState(context, location, pass, currentNotify, notifySettings[currentNotify], showPast, true);
                        }
                    }
                }
            });
        }

        //Create location receiver
        private static void createLocationReceiver(Context context, boolean use, boolean runForeground)
        {
            //if using and not set
            if(use && locationReceiver == null)
            {
                //listen for location updates
                locationReceiver = new LocationReceiver(LocationService.FLAG_START_GET_LOCATION | (runForeground ? LocationService.FLAG_START_RUN_FOREGROUND : LocationService.FLAG_START_NONE))
                {
                    private boolean firstLocation = true;
                    private Calculations.ObserverType observer = null;

                    @Override
                    protected void onGotLocation(Context context, Calculations.ObserverType updatedObserver)
                    {
                        boolean updated = (updatedObserver != null && (firstLocation || observer == null || observer.notEqual(updatedObserver)));

                        //if updated location
                        if(updated)
                        {
                            //update observer
                            observer = updatedObserver;

                            //update notifications
                            updateNotify(context, updatedObserver, false);
                        }

                        //update status
                        firstLocation = false;
                    }
                };
                locationReceiver.register(context);
            }
            //else if not using and is set
            else if(!use && locationReceiver != null)
            {
                //stop listening
                locationReceiver.unregister();
            }
        }

        //Update notify locations
        public static void updateNotifyLocations(Context context, Calculations.ObserverType location, boolean useCurrentLocation, boolean usePastNotify, boolean runForeground)
        {
            //start/stop receiver
            createLocationReceiver(context, useCurrentLocation, runForeground);

            //update with known location
            updateNotify(context, location, usePastNotify);
        }
    }

    //Calculate listener
    public interface CalculateListener
    {
        void onCalculated(int progressType, PassData pass);
    }

    //Calculates passes as a task
    public static class CalculatePathsTask extends ThreadTask<Object, Void, Void>
    {
        private final byte calculateType;
        private final CalculateListener calculateListener;

        public CalculatePathsTask(byte calcType, CalculateListener listener)
        {
            calculateType = calcType;
            calculateListener = listener;
        }

        @Override
        protected Void doInBackground(Object... params)
        {
            Context context = (Context)params[0];
            CalculatePathsTask task = (CalculatePathsTask)params[1];
            PassData[] passItems = (PassData[])params[2];
            Current.Passes.Item[] savedPassItems = (Current.Passes.Item[])params[3];
            Calculations.ObserverType observer = (Calculations.ObserverType)params[4];
            double minEl = (double)params[5];
            double intersection = (double)params[6];
            double julianStart = (double)params[7];
            double julianEnd = (double)params[8];
            boolean applyRefraction = (boolean)params[9];
            boolean getAll = (calculateType == CalculateType.OrbitalPasses || calculateType == CalculateType.OrbitalIntersections);
            boolean hideSlow = (calculateType == CalculateType.OrbitalsPasses);
            ArrayList<PassData> items = new ArrayList<>();

            //if pass items are set
            if(passItems != null)
            {
                //add all items
                Collections.addAll(items, passItems);
            }

            //calculate paths
            calculatePaths(null, task, context, calculateType, null, items, savedPassItems, observer, minEl, intersection, julianStart, julianEnd, applyRefraction, getAll, getAll, hideSlow, calculateListener);

            //done
            return(null);
        }
    }

    private static boolean firstRun = true;
    private static MutableLiveData<Intent> localBroadcast;
    private static final boolean[] cancelIntent = new boolean[CalculateType.CalculateCount];
    private static final boolean[] showNotification = new boolean[CalculateType.CalculateCount];
    public static final String CALCULATE_FILTER = "calculateServiceFilter";

    public CalculateService()
    {
        super("calculateService", Globals.getChannelId(Globals.ChannelIds.Calculate));

        int index;

        //if the first run
        if(firstRun)
        {
            //go through each status
            for(index = 0; index < CalculateType.CalculateCount; index++)
            {
                //don't cancel or show
                cancelIntent[index] = showNotification[index] = false;
            }

            //update status
            firstRun = false;
        }
    }

    @Override
    protected void onClearIntent(byte index) {}

    @Override
    protected void onRunIntent(Intent intent)
    {
        byte calculateType = intent.getByteExtra(ParamTypes.CalculateType, Byte.MAX_VALUE);
        boolean runForeground = intent.getBooleanExtra(ParamTypes.RunForeground, false);
        boolean applyRefraction = intent.getBooleanExtra(ParamTypes.ApplyRefraction, true);
        double minEl = intent.getDoubleExtra(ParamTypes.MinEl, Double.MAX_VALUE);
        double julianStart = intent.getDoubleExtra(ParamTypes.StartJulian, Double.MAX_VALUE);
        double julianEnd = intent.getDoubleExtra(ParamTypes.EndJulian, Double.MAX_VALUE);
        String notifyTitle = intent.getStringExtra(ParamTypes.NotifyTitle);
        Calculations.ObserverType observer = intent.getParcelableExtra(ParamTypes.Observer);
        NotificationCompat.Builder notifyBuilder = Globals.createNotifyBuilder(this, notifyChannelId);
        ArrayList<PassData> passItems = intent.getParcelableArrayListExtra(ParamTypes.PassItems);

        //handle if need to start in foreground
        Globals.startForeground(this, Globals.ChannelIds.Calculate, notifyBuilder, runForeground);

        //update intent and status
        showNotification[calculateType] = runForeground;
        cancelIntent[calculateType] = false;

        //update notification
        currentNotify.title = notifyTitle;
        updateNotification(notifyBuilder, calculateType);

        //update progress
        sendGeneralMessage(calculateType, Globals.ProgressType.Started);

        //handle based on calculate type
        switch(calculateType)
        {
            case CalculateType.OrbitalsPasses:
            case CalculateType.OrbitalPasses:
            case CalculateType.OrbitalPass:
            case CalculateType.FullMoon:
                //calculate paths
                calculatePaths(this, null, this, calculateType, null, passItems, null, observer, minEl, -Double.MAX_VALUE, julianStart, julianEnd, applyRefraction, false, false, (calculateType == CalculateType.OrbitalsPasses), null);
                break;

            default:
                //nothing to calculate
                sendGeneralMessage(calculateType, Globals.ProgressType.Failed);
                break;
        }

        //reset
        //currentIntent[calculateType] = null;
        cancelIntent[calculateType] = showNotification[calculateType] = false;
    }

    @NonNull
    private MutableLiveData<Intent> getLocalBroadcast()
    {
        if(localBroadcast == null)
        {
            localBroadcast = new MutableLiveData<>();
            Globals.setBroadcastValue(localBroadcast, new Intent());
        }

        return(localBroadcast);
    }

    //Updates notification and it's visibility
    private void updateNotification(NotificationCompat.Builder notifyBuilder, byte calculateType)
    {
        updateNotification(notifyBuilder, NotifyReceiver.getCalculateID(calculateType), 0, showNotification[calculateType]);
    }

    //Sends a message
    private void sendMessage(byte messageType, byte calculateType, int id, String section, long subIndex, long subCount, long index, long count, int progressType, PassData pass)
    {
        int updateID = NotifyReceiver.getCalculateID(calculateType);
        String titleDesc = this.getResources().getString(R.string.title_calculating_passes);
        Bundle extraData = null;

        if(pass != null)
        {
            extraData = new Bundle();
            extraData.putParcelable(ParamTypes.PassData, pass);
        }

        sendMessage(getLocalBroadcast(), messageType, calculateType, ParamTypes.CalculateType, id, titleDesc, section, CALCULATE_FILTER, NotifyReceiver.class, subIndex, subCount, index, count, progressType, updateID, NotifyReceiver.DismissPassesID, NotifyReceiver.RetryPassesID, showNotification[calculateType], extraData);
    }

    //Sends a load message
    private void sendLoadFinishedMessage(byte calculateType, int id, String section, long index, long count, PassData pass)
    {
        sendMessage(MessageTypes.Load, calculateType, id, section, 0, 0, index, count, Globals.ProgressType.Finished, pass);
    }
    private void sendLoadRunningMessage(byte calculateType, String section, long subIndex, long index, long count)
    {
        sendMessage(MessageTypes.Load, calculateType, Integer.MAX_VALUE, section, subIndex, 100, index, count, Globals.ProgressType.Running, null);
    }

    //Sends a general message
    private void sendGeneralMessage(byte calculateType, int progressType)
    {
        sendMessage(MessageTypes.General, calculateType, Integer.MAX_VALUE, null, 0, 0, 0, 0, progressType, null);
    }

    /*//Sets cancel
    public static void cancel(byte calculateType)
    {
        //if a valid calculate type
        if(calculateType < cancelIntent.length)
        {
            //cancel
            cancelIntent[calculateType] = true;
        }
    }*/

    //Gets position for given satellite
    private static Calculations.TopographicDataType getPosition(Calculations.SatelliteObjectType currentSatellite, Calculations.ObserverType observer, Calendar gmtTime, boolean applyRefraction)
    {
        //get position for the current time
        //note: not updating GEO location
        Calculations.updateOrbitalPosition(currentSatellite, observer, gmtTime);
        return(Calculations.getLookAngles(observer, currentSatellite, applyRefraction));
    }

    //Returns true if angles are within intersecting range
    private static boolean anglesIntersect(double angle1, double angle2, double range)
    {
        return(Math.abs(Globals.degreeDistance(angle1, angle2)) <= range);
    }

    //Returns true if points are within intersecting range
    private static boolean pointsIntersect(Calculations.TopographicDataType pointData1, Calculations.TopographicDataType pointData2, double intersection)
    {
        return(anglesIntersect(pointData1.azimuth, pointData2.azimuth, intersection) && anglesIntersect(pointData1.elevation, pointData2.elevation, intersection));
    }

    //Updates time in pass to within 1 second of max seconds and returns success
    private static boolean updateTimeInPath(byte calculateType, Calculations.SatelliteObjectType satellite1, Calculations.SatelliteObjectType satellite2, Calculations.ObserverType observer, double intersection, int secsInc, long maxSeconds, Calendar gmtTime, boolean forEndTime, boolean applyRefraction)
    {
        boolean inPath;
        boolean isFullMoon;
        boolean forFullMoon = (satellite1.getSatelliteNum() == Universe.IDs.Moon && calculateType == CalculateType.FullMoon);
        boolean forIntersection = (calculateType == CalculateType.OrbitalIntersections);
        boolean moving = true;
        boolean foundTime = false;
        boolean foundOutsidePath = false;
        boolean calculatingTime = true;
        boolean extendedSearch = false;
        double azLast = Double.MAX_VALUE;
        double azLast2 = Double.MAX_VALUE;
        double azTravel = 0;
        double azTravel2 = 0;
        double calculateSeconds = 0;
        Calculations.TopographicDataType pointData1;
        Calculations.TopographicDataType pointData2;

        //if not for ending time
        if(!forEndTime)
        {
            //going backwards in time
            secsInc = -secsInc;
        }

        //while still calculating time and moving
        while(calculatingTime)
        {
            //get new position, if full moon, and if in pass
            pointData1 = getPosition(satellite1, observer, gmtTime, applyRefraction);
            pointData2 = (forIntersection ? getPosition(satellite2, observer, gmtTime, applyRefraction) : null);
            isFullMoon = (forFullMoon && Universe.Moon.isFull(Universe.Moon.getPhase(gmtTime.getTimeInMillis())));
            inPath = ((pointData1.elevation >= 0.0 && (!forIntersection || pointData2.elevation >= 0.0)) && (!forFullMoon || isFullMoon) && (!forIntersection || pointsIntersect(pointData1, pointData2, intersection)));

            //if haven't found outside path
            if(!foundOutsidePath)
            {
                //update movement
                if(azLast == Double.MAX_VALUE)
                {
                    //initialize
                    azLast = pointData1.azimuth;
                }
                azTravel += Math.abs(Globals.degreeDistance(azLast, pointData1.azimuth));
                azLast = pointData1.azimuth;
                if(forIntersection)
                {
                    if(azLast2 == Double.MAX_VALUE)
                    {
                        azLast2 = pointData2.azimuth;
                    }
                    azTravel2 += Math.abs(Globals.degreeDistance(azLast2, pointData2.azimuth));
                    azLast2 = pointData2.azimuth;
                }
                else
                {
                    azTravel2 = azTravel;
                }

                //if not moving after 1 hour
                if(calculateSeconds > Calculations.SecondsPerHour && (azTravel < 0.75 && azTravel2 < 0.75))
                {
                    //not moving, so stop checking
                    moving = false;
                }
                //else if finding start time, not for intersection, not extended search, and been looking for at least 1 day
                else if(!forEndTime && !forIntersection && !extendedSearch && calculateSeconds > Calculations.SecondsPerDay)
                {
                    //change from seconds to minutes
                    secsInc *= 60;
                    extendedSearch = true;
                }

                //if no longer in path
                if(!inPath)
                {
                    //update status, change increment to 1 second, and reset calculated time
                    foundOutsidePath = true;
                    secsInc = (!forEndTime ? 1 : -1);
                    calculateSeconds = 0;
                }
            }
            //else if found inside pass again
            else if(inPath)
            {
                //found time
                foundTime = true;
            }

            //continue calculating time if moving, have time left, and haven't found time
            calculatingTime = moving && (calculateSeconds < maxSeconds) && !foundTime;

            //if still calculating time
            if(calculatingTime)
            {
                //update time
                gmtTime.add(Calendar.SECOND, secsInc);
                calculateSeconds += Math.abs(secsInc);
            }
        }

        //return success
        return(foundTime);
    }

    //Calculates pass for given item
    private static void calculatePath(CalculateService service, CalculatePathsTask task, Context context, byte calculateType, int listIndex, int listCount, PassData currentItem, Calculations.ObserverType observer, Calendar startGMT, Calendar endGMT, double intersection, boolean applyRefraction, boolean allowPastEnd, boolean hideSlow)
    {
        boolean isSun;
        boolean isMoon;
        boolean isSlow = false;
        boolean usingTask = (task != null);
        boolean forFullMoon;
        boolean doneWithPath;
        boolean onFirstOrLast;
        boolean extendedSearch = false;
        boolean tle1IsAccurate;
        boolean tle2IsAccurate;
        boolean neededTLEsAccurate;
        boolean adjustLargeTime;
        boolean forIntersection = (calculateType == CalculateType.OrbitalIntersections);
        double azStart;
        double azStart2;
        double azEnd;
        double azEnd2;
        double azLast;
        double azLast2;
        double elLast;
        double elLast2;
        double azTravel;
        double azTravel2;
        double elTravel;
        double elTravel2;
        double highestEl;
        double azDistance;
        double elDistance;
        double azIntersectClosest = Double.MAX_VALUE;
        double elIntersectClosest = Double.MAX_VALUE;
        double pathJulianDate;
        double pathJulianDateStart;
        double pathJulianDateEnd;
        double pathDayIncrement;
        double phase;
        long spanMs;
        long firstTimeMs;
        long startTimeMs = startGMT.getTimeInMillis();
        String section;
        Calendar currentGMT = Globals.getGMTTime(startTimeMs);
        Calendar currentEndGMT = Globals.getGMTTime();
        Calendar hourAfterStartGMT = Globals.getGMTTime(startTimeMs);
        Calendar pathStartGMT = null;
        Calendar pathHourAfterStartGMT = Globals.getGMTTime();
        Calendar pathEndGMT = null;
        Calendar maxEndGMT = Globals.getGMTTime(endGMT);
        Calendar extendedSearchGMT = Globals.getGMTTime(startTimeMs);
        Calculations.TopographicDataType pointData1;
        Calculations.TopographicDataType pointData2;
        ArrayList<CalculateViewsTask.OrbitalView> pathViewsList = new ArrayList<>(0);
        ArrayList<CalculateViewsTask.OrbitalView> pathViewsList2 = new ArrayList<>(0);

        //if observer not set
        if(observer == null)
        {
            //set to default
            observer = new Calculations.ObserverType();
        }

        //remember current satellite(s)
        Calculations.SatelliteObjectType satellite1 = new Calculations.SatelliteObjectType(currentItem.satellite);
        Calculations.SatelliteObjectType satellite2 = (forIntersection ? new Calculations.SatelliteObjectType(currentItem.satellite2) : null);

        //update status
        section = currentItem.name;
        isSun = (currentItem.id == Universe.IDs.Sun || (forIntersection && satellite2.getSatelliteNum() == Universe.IDs.Sun));
        isMoon = (currentItem.id == Universe.IDs.Moon || (forIntersection && satellite2.getSatelliteNum() == Universe.IDs.Moon));
        forFullMoon = (isMoon && calculateType == CalculateType.FullMoon);
        tle1IsAccurate = Globals.getTLEIsAccurate(satellite1.tle);
        tle2IsAccurate = (satellite2 != null && Globals.getTLEIsAccurate(satellite2.tle));
        neededTLEsAccurate = (tle1IsAccurate && (!forIntersection || tle2IsAccurate));

        //set hour after start
        hourAfterStartGMT.add(Calendar.HOUR, 1);

        //set maximum ending time to 180 days after ending time
        maxEndGMT.add(Calendar.DAY_OF_YEAR, 180);

        //set extended search time to 1 day after start
        extendedSearchGMT.add(Calendar.DAY_OF_YEAR, 1);

        //update progress
        if(service != null)
        {
            service.sendLoadRunningMessage(calculateType, section, 10, listIndex, listCount);
        }

        //calculate pass if -needed TLEs are accurate- and --not already calculating- or -not calculated yet- or -pass start time is after end time- or -pass end time is before start time--
        doneWithPath = (neededTLEsAccurate && (currentItem.passCalculating || currentItem.passCalculated || (currentItem.passTimeStart != null && !currentItem.passTimeStart.after(endGMT)) || (currentItem.passTimeEnd != null && !currentItem.passTimeEnd.before(startGMT))));
        if(!doneWithPath)
        {
            //initialize
            azStart = azStart2 = azEnd = azEnd2 = azTravel = azTravel2 = elTravel = elTravel2 = 0;
            azLast = azLast2 = elLast = elLast2 = Double.MAX_VALUE;
            highestEl = -180;

            //update status
            currentItem.clearPass();
            currentItem.passCalculating = true;

            //if satellite was set and needed TLEs are accurate
            if(currentItem.satellite != null && neededTLEsAccurate)
            {
                //while not done and not cancelled
                while(!doneWithPath && !(usingTask ? task.isCancelled() : cancelIntent[calculateType]))
                {
                    //get position and phase for the current time
                    pointData1 = getPosition(satellite1, observer, currentGMT, applyRefraction);
                    pointData2 = (forIntersection ? getPosition(satellite2, observer, currentGMT, applyRefraction) : null);
                    if(azLast == Double.MAX_VALUE)
                    {
                        azLast = pointData1.azimuth;
                    }
                    if(forIntersection && azLast2 == Double.MAX_VALUE)
                    {
                        azLast2 = pointData2.azimuth;
                    }
                    phase = (isMoon ? Universe.Moon.getPhase(currentGMT.getTimeInMillis()) : 0);

                    //if elevation is within range and requirements met
                    if((pointData1.elevation >= 0.0 && (!forIntersection || pointData2.elevation >= 0.0)) && (!forFullMoon || Universe.Moon.isFull(phase)) && (!forIntersection || pointsIntersect(pointData1, pointData2, intersection)))
                    {
                        //if pass start has not been set yet
                        if(pathStartGMT == null)
                        {
                            //update progress
                            if(service != null)
                            {
                                service.sendLoadRunningMessage(calculateType, section, 50, listIndex, listCount);
                            }

                            // remember first time and try to go back up to 180 days in 1 or 15 second increments to find starting second
                            firstTimeMs = currentGMT.getTimeInMillis();
                            currentItem.passStartFound = updateTimeInPath(calculateType, satellite1, satellite2, observer, intersection, (forIntersection ? 1 : 15), (long)(Calculations.SecondsPerDay * 180), currentGMT, false, applyRefraction);

                            //set pass start to now
                            pathStartGMT = Globals.getGMTTime(currentItem.passStartFound ? currentGMT : startGMT);

                            //if pass start was not found
                            if(!currentItem.passStartFound)
                            {
                                //go back first known pass start time
                                currentGMT.setTimeInMillis(firstTimeMs);
                            }

                            //remember 1 hour after pass start time
                            pathHourAfterStartGMT.setTimeInMillis(pathStartGMT.getTimeInMillis());
                            pathHourAfterStartGMT.add(Calendar.HOUR, 1);

                            //update az start
                            pointData1 = getPosition(satellite1, observer, pathStartGMT, applyRefraction);
                            if(forIntersection)
                            {
                                pointData2 = getPosition(satellite2, observer, pathStartGMT, applyRefraction);
                            }
                            azStart = pointData1.azimuth;
                            azStart2 = (forIntersection ? pointData2.azimuth : azStart);
                        }
                        else
                        {
                            //update az travel
                            azTravel += Math.abs(Globals.degreeDistance(azLast, pointData1.azimuth));
                            if(forIntersection)
                            {
                                azTravel2 += Math.abs(Globals.degreeDistance(azLast2, pointData2.azimuth));
                            }
                            else
                            {
                                azTravel2 = azTravel;
                            }
                        }

                        //if the highest elevation so far
                        if(pointData1.elevation > highestEl)
                        {
                            //update maximum elevation
                            highestEl = pointData1.elevation;
                        }
                        if(forIntersection && pointData2.elevation > highestEl)
                        {
                            //update maximum elevation
                            highestEl = pointData2.elevation;
                        }

                        //if for intersection
                        if(forIntersection)
                        {
                            //get current distances
                            azDistance = Math.abs(Globals.degreeDistance(pointData1.azimuth, pointData2.azimuth));
                            elDistance = Math.abs(Globals.degreeDistance(pointData1.elevation, pointData2.elevation));

                            //if the overall closest so far
                            if((azDistance + elDistance) < ((azIntersectClosest == Double.MAX_VALUE || elIntersectClosest == Double.MAX_VALUE) ? Double.MAX_VALUE : (azIntersectClosest + elIntersectClosest)))
                            {
                                //update closest
                                azIntersectClosest = azDistance;
                                elIntersectClosest = elDistance;
                            }
                        }
                    }
                    //else if elevation was within range
                    else if(pathStartGMT != null)
                    {
                        //update pass and az end
                        pathEndGMT = Globals.getGMTTime(currentGMT);
                        azEnd = pointData1.azimuth;
                        azEnd2 = (forIntersection ? pointData2.azimuth : azEnd);

                        //done with pass
                        doneWithPath = true;
                    }

                    //if elevation -has a last point- and -less than 90 degrees travel-
                    if(elLast != Double.MAX_VALUE && elTravel < 90.0)
                    {
                        //update el travel
                        elTravel += Math.abs(Globals.degreeDistance(elLast, pointData1.elevation));
                    }
                    if(forIntersection)
                    {
                        if(elLast2 != Double.MAX_VALUE && elTravel2 < 90.0)
                        {
                            //update el travel 2
                            elTravel2 += Math.abs(Globals.degreeDistance(elLast2, pointData2.elevation));
                        }
                    }
                    else
                    {
                        //same as el travel
                        elTravel2 = elTravel;
                    }

                    //if not done with pass
                    if(!doneWithPath)
                    {
                        //if not in extended search yet
                        if(!extendedSearch)
                        {
                            //check if now on extended search
                            extendedSearch = currentGMT.after(extendedSearchGMT);
                        }

                        //if in extended search now
                        if(extendedSearch)
                        {
                            //look ahead 15 minutes for next calculation
                            currentGMT.add(Calendar.MINUTE, 15);
                        }
                        else
                        {
                            //look ahead 15 seconds for next calculation
                            currentGMT.add(Calendar.SECOND, 15);
                        }
                    }

                    //if --not allowing past search time- and -past search time-- or --allowing past search time- and -after maximum search time-- or -found start and target is not moving enough (0.75 degrees) after 1 hour in pass- or -not found start and target is not moving enough (0.1 degrees) after 1 hour-
                    if((!allowPastEnd && currentGMT.after(endGMT)) || (allowPastEnd && currentGMT.after(maxEndGMT)) || (pathStartGMT != null && currentGMT.after(pathHourAfterStartGMT) && azTravel < 0.75 && azTravel2 < 0.75) || (pathStartGMT == null && currentGMT.after(hourAfterStartGMT) && elTravel < 0.1 && elTravel2 < 0.1))
                    {
                        //if weren't done with pass
                        if(!doneWithPath)
                        {
                            //unknown pass end
                            pathEndGMT = null;
                        }

                        //done with pass
                        doneWithPath = true;
                    }

                    //remember last az and el
                    azLast = pointData1.azimuth;
                    elLast = pointData1.elevation;
                    if(forIntersection)
                    {
                        azLast2 = pointData2.azimuth;
                        elLast2 = pointData2.elevation;
                    }
                }
            }

            //update progress
            if(service != null)
            {
                service.sendLoadRunningMessage(calculateType, section, 90, listIndex, listCount);
            }

            //if done with pass, not cancelled, and pass end found
            if(doneWithPath && !(usingTask ? task.isCancelled() : cancelIntent[calculateType]) && pathEndGMT != null)
            {
                //go forward up to 15 minutes/seconds to find ending second
                updateTimeInPath(calculateType, satellite1, satellite2, observer, intersection, 1, (extendedSearch ? (15 * 60) : 15), pathEndGMT, true, applyRefraction);
            }

            //if done with pass or cancelled
            if(doneWithPath || (usingTask ? task.isCancelled() : cancelIntent[calculateType]))
            {
                //if pass start is set and not cancelled
                if(pathStartGMT != null && !(usingTask ? task.isCancelled() : cancelIntent[calculateType]))
                {
                    //update status
                    currentItem.passAzStart = (azTravel2 > azTravel ? azStart2 : azStart);
                    currentItem.passAzEnd = (azTravel2 > azTravel ? azEnd2 : azEnd);
                    currentItem.passAzTravel = Math.max(azTravel, azTravel2);
                    currentItem.passElMax = highestEl;
                    currentItem.passClosestAz = azIntersectClosest;
                    currentItem.passClosestEl = elIntersectClosest;
                    currentItem.passTimeStart = Globals.getGMTTime(pathStartGMT);
                    if(pathEndGMT != null)
                    {
                        currentItem.passTimeEnd = Globals.getGMTTime(pathEndGMT);
                    }
                    currentItem.passDuration = Globals.getTimeBetween(context, pathStartGMT, pathEndGMT);
                    phase = (isMoon ? Universe.Moon.getPhase(currentItem.getMidPass()) : 0);
                    currentItem.illumination = (isMoon ? Universe.Moon.getIllumination(phase) : 0);
                    currentItem.phaseName = (isMoon ? Universe.Moon.getPhaseName(context, phase) : isSun ? Universe.Sun.getPhaseName(context, highestEl, true) : null);

                    //if end time found
                    if(currentItem.passTimeEnd != null)
                    {
                        //set to pass end time
                        currentEndGMT.setTimeInMillis(currentItem.passTimeEnd.getTimeInMillis());
                    }
                    else
                    {
                        //set to 0.1 seconds after start
                        currentEndGMT.setTimeInMillis(currentItem.passTimeStart.getTimeInMillis() + 100);
                        isSlow = true;
                    }

                    //if not slow or not hiding slow
                    if(!isSlow || !hideSlow)
                    {
                        //remember zone ID
                        String zoneId = observer.timeZone.getID();

                        //get end julian date and increment
                        pathJulianDateEnd = Calculations.julianDateCalendar(currentEndGMT);
                        spanMs = (currentEndGMT.getTimeInMillis() - currentItem.passTimeStart.getTimeInMillis());
                        pathDayIncrement = (spanMs / Calculations.MsPerDay) / (spanMs < 900000 ? 80.0 : 40.0);      //more increments if less than 15 minutes
                        if(pathDayIncrement < 0.000025)
                        {
                            pathDayIncrement = 0.000025;
                        }

                        //want to adjust time if over 1 minute increment
                        adjustLargeTime = (pathDayIncrement > 0.000694444);

                        //get views for path
                        pathJulianDateStart = Calculations.julianDateCalendar(currentItem.passTimeStart);
                        for(pathJulianDate = pathJulianDateStart; pathJulianDate <= pathJulianDateEnd; pathJulianDate += pathDayIncrement)
                        {
                            //remember if on first or last
                            onFirstOrLast = !(pathJulianDate > pathJulianDateStart && pathJulianDate < pathJulianDateEnd);

                            //adjust time if 1 more than 1 minute increment and not on first or last
                            if(adjustLargeTime && !onFirstOrLast)
                            {
                                //set julian date to 0 seconds at that time
                                pathJulianDate = Globals.julianDateNoSeconds(pathJulianDate);
                            }

                            //update view(s) and add to list
                            Calculations.updateOrbitalPosition(satellite1, observer, pathJulianDate, false);
                            pathViewsList.add(new CalculateViewsTask.OrbitalView(context, Calculations.getLookAngles(observer, satellite1, true), pathJulianDate, zoneId, onFirstOrLast));
                            if(forIntersection)
                            {
                                Calculations.updateOrbitalPosition(satellite2, observer, pathJulianDate, false);
                                pathViewsList2.add(new CalculateViewsTask.OrbitalView(context, Calculations.getLookAngles(observer, satellite2, true), pathJulianDate, zoneId, onFirstOrLast));
                            }

                            //if date is before end date and next date would be after
                            if(pathJulianDate < pathJulianDateEnd && (pathJulianDate + pathDayIncrement) > pathJulianDateEnd)
                            {
                                //set to increment before end date
                                pathJulianDate = pathJulianDateEnd - pathDayIncrement;
                            }
                        }
                    }
                    currentItem.passViews = pathViewsList.toArray(new CalculateViewsTask.OrbitalView[0]);
                    currentItem.passViews2 = pathViewsList2.toArray(new CalculateViewsTask.OrbitalView[0]);

                    //done calculating
                    currentItem.passCalculated = true;
                }

                //if done with path
                if(doneWithPath)
                {
                    //finished with calculation
                    currentItem.passCalculateFinished = true;
                }
            }

            //done calculating
            currentItem.passCalculating = false;

            //update progress
            if(service != null)
            {
                service.sendLoadRunningMessage(calculateType, section, 100, listIndex, listCount);
            }
        }
    }

    //Calculates paths for given items
    private static void calculatePaths(@Nullable CalculateService service, CalculatePathsTask task, Context context, byte calculateType, @Nullable int[] ids, ArrayList<PassData> pathItems, Current.Passes.Item[] savedPassItems, Calculations.ObserverType observer, double minEl, double intersection, double julianStart, double julianEnd, boolean applyRefraction, boolean getAll, boolean allowPastEnd, boolean hideSlow, @Nullable CalculateListener listener)
    {
        boolean usingTask = (task != null);
        boolean usingSavedItems = (savedPassItems != null && savedPassItems.length > 0 && !savedPassItems[0].isLoading);
        int index;
        int listIndex;
        int pathCount = 0;
        int pathItemCount = (usingSavedItems ? savedPassItems.length : pathItems != null ? pathItems.size() : 0);
        String section;
        Calendar endGMT = Globals.julianDateToCalendar(julianEnd);
        Calendar startGMT = Globals.julianDateToCalendar(julianStart);
        Calendar currentGMT = Globals.getGMTTime();
        Resources res = context.getResources();

        //update progress
        if(listener != null)
        {
            listener.onCalculated(Globals.ProgressType.Started, null);
        }

        //go through each item while not cancelled
        for(index = 0; index < pathItemCount && !(usingTask ? task.isCancelled() : cancelIntent[calculateType]); index++)
        {
            //remember current item
            PassData currentItem = (usingSavedItems ? new PassData(savedPassItems[index]) : pathItems.get(index));

            //if current item exists
            if(currentItem != null)
            {
                //remember current orbital(s) and set defaults
                boolean firstPath = true;
                boolean reachedMinEl;
                boolean haveSatellite2 = (currentItem.satellite2 != null);
                Database.SatelliteData currentSatellite1 = new Database.SatelliteData(context, currentItem.id);
                Database.SatelliteData currentSatellite2 = (haveSatellite2 ? new Database.SatelliteData(context, currentItem.satellite2.getSatelliteNum()) : null);
                currentGMT.setTimeInMillis(startGMT.getTimeInMillis());
                if(!usingSavedItems)
                {
                    pathCount = 0;
                }
                listIndex = index;

                do
                {
                    PassData newPass = null;

                    //if using saved items and passed calculate
                    if(usingSavedItems && currentItem.passCalculated)
                    {
                        //update current time
                        currentGMT.setTimeInMillis((currentItem.passTimeEnd != null ? currentItem.passTimeEnd : endGMT).getTimeInMillis());
                    }
                    else
                    {
                        //if not on the first pass
                        if(!firstPath)
                        {
                            //reset
                            currentItem.passCalculated = false;
                            currentItem.passTimeStart = null;
                            currentItem.passTimeEnd = null;
                        }

                        //calculate path and update status
                        calculatePath(service, task, context, calculateType, listIndex, pathItemCount, currentItem, observer, currentGMT, endGMT, intersection, applyRefraction, allowPastEnd, hideSlow);
                    }

                    //check if reached elevation
                    reachedMinEl = (currentItem.passCalculated && currentItem.passElMax >= minEl);

                    //if reached elevation
                    if(reachedMinEl)
                    {
                        //set section
                        section = currentSatellite1.getName();
                        if(haveSatellite2)
                        {
                            section += (", " + currentSatellite2.getName());
                        }
                    }
                    else
                    {
                        //unused
                        section = "";
                    }

                    //if path calculated
                    if(currentItem.passCalculated)
                    {
                        //if getting all passes
                        //note: current item max elevation not updated if not copied
                        if(reachedMinEl && getAll)
                        {
                            //create new item and add to list
                            newPass = new PassData(0, currentSatellite1, currentSatellite2);
                            section += (" " + res.getString(haveSatellite2 ? R.string.title_intersection : R.string.title_pass) + " " + (pathCount + 1));
                            newPass.id = currentItem.id;
                            newPass.name = section;
                            newPass.ownerCode = currentItem.ownerCode;
                            newPass.orbitalType = currentSatellite1.getOrbitalType();
                            newPass.orbital2Type = (haveSatellite2 ? currentSatellite2.getOrbitalType() : newPass.orbitalType);
                            newPass.listIndex = pathCount;
                            newPass.copyPass(currentItem);
                            pathCount++;
                        }

                        //if pass time end found
                        if(currentItem.passTimeEnd != null)
                        {
                            //update current time
                            currentGMT.setTimeInMillis(Math.max(currentGMT.getTimeInMillis(), currentItem.passTimeEnd.getTimeInMillis()) + 60000);  //jump ahead 1 minute after latest time
                        }
                    }

                    //if -reached elevation- and -not getting all or path calculated-
                    if(reachedMinEl && (!getAll || currentItem.passCalculated))
                    {
                        //update progress
                        if(service != null)
                        {
                            service.sendLoadFinishedMessage(calculateType, (ids != null && index < ids.length ? ids[index] : Integer.MAX_VALUE), section, listIndex, pathItemCount, (getAll ? newPass : currentItem));
                        }
                        if(listener != null)
                        {
                            listener.onCalculated(Globals.ProgressType.Success, (getAll ? newPass : currentItem));
                        }
                        listIndex++;
                    }

                    //update status
                    firstPath = false;

                  //continue if calculating all paths, not canceling, another is found, and before end time
                } while(getAll && !usingSavedItems && !(usingTask ? task.isCancelled() : cancelIntent[calculateType]) && currentItem.passCalculated && currentItem.passTimeEnd != null && !currentGMT.after(endGMT));
            }
        }

        //update progress
        if(service != null)
        {
            service.sendGeneralMessage(calculateType, (cancelIntent[calculateType] ? Globals.ProgressType.Cancelled : Globals.ProgressType.Finished));
        }
        if(listener != null)
        {
            listener.onCalculated((task == null || task.isCancelled() ? Globals.ProgressType.Cancelled : Globals.ProgressType.Finished), null);
        }
    }

    //Calculate paths for orbital(s)
    public static CalculatePathsTask calculateOrbitalPaths(Context context, byte calculateType, PassData passItem, Current.Passes.Item[] savedPassItems, Calculations.ObserverType observer, double minEl, double intersection, double julianStart, double julianEnd, boolean applyRefraction, CalculateListener listener)
    {
        CalculatePathsTask task = new CalculatePathsTask(calculateType, listener);

        //start and return task
        task.execute(context, task, new PassData[]{passItem}, savedPassItems, observer, minEl, intersection, julianStart, julianEnd, applyRefraction);
        return(task);
    }

    //Calculate passes for orbitals
    public static CalculatePathsTask calculateOrbitalsPasses(Context context, PassData[] passItems, Calculations.ObserverType observer, double minEl, Calendar gmtStart, CalculateListener listener)
    {
        double julianStart = Calculations.julianDateCalendar(gmtStart);
        CalculatePathsTask task = new CalculatePathsTask(CalculateType.OrbitalsPasses, listener);

        //start and return task
        task.execute(context, task, passItems, null, observer, minEl, -Double.MAX_VALUE, julianStart, julianStart + 180, true);
        return(task);
    }

    //Calculate pass for orbital
    private static void calculateOrbitalPass(Context context, byte calculateType, int id, Database.SatelliteData satellite, Calculations.ObserverType observer, double minEl, double julianStart, boolean applyRefraction, CalculateListener listener)
    {
        calculatePaths(null, null, context, calculateType, new int[]{id}, new ArrayList<>(java.util.Collections.singletonList(new PassData(0, satellite))), null, observer, minEl, -Double.MAX_VALUE, julianStart, julianStart + 180, applyRefraction, false, true, false, listener);
    }
    public static void calculateOrbitalPass(Context context, int id, Database.SatelliteData satellite, Calculations.ObserverType observer, double minEl, double julianStart, boolean applyRefraction, CalculateListener listener)
    {
        calculateOrbitalPass(context, CalculateType.OrbitalPass, id, satellite, observer, minEl, julianStart, applyRefraction, listener);
    }
}
