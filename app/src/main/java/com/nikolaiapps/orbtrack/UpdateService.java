package com.nikolaiapps.orbtrack;


import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;


public class UpdateService extends NotifyService
{
    //Param Types
    public static abstract class ParamTypes extends NotifyService.ParamTypes
    {
        static final String UpdateType = "updateType";
        static final String UpdateSource = "updateSource";
        static final String GetUpdate = "getUpdate";
        static final String CheckUpdate = "checkUpdate";
        static final String User = "user";
        static final String Password = "pwd";
        static final String Satellites = "satellites";
        static final String FileNames = "fileNames";
        static final String FileName = "fileName";
        static final String FilesData = "fileData";
        static final String FileType = "fileType";
        static final String FileExt = "fileExt";
        static final String FileSourceType = "fileSourceType";
        static final String FileUri = "fileUri";
        static final String TLEData = "tleData";
        static final String UsedFile = "usedFile";
        static final String Information = "information";
        static final String PendingSatellites = "pendingSatellites";
    }

    //Update types
    public static abstract class UpdateType
    {
        static final byte UpdateSatellites = 0;
        static final byte GetMasterList = 1;
        static final byte LoadFile = 2;
        static final byte SaveFile = 3;
        static final byte BuildDatabase = 4;
        static final byte GetInformation = 5;
        static final byte UpdateCount = 6;
    }

    //Update sub source
    private static abstract class UpdateSubSource
    {
        static final int Satellites = 0;
        static final int Category = 1;
        static final int Owners = 2;
        static final int SatelliteOwners = 3;
    }

    //Preference names
    public static abstract class PreferenceName
    {
        static final String MasterSource = "MasterSource";
        static final String MasterTz = "MasterTz";
        static final String MasterTime = "MasterTime";
    }

    //Master satellite
    public static class MasterSatellite
    {
        final int noradId;
        long launchDateMs;
        public final String name;
        String ownerCode;
        String ownerName;
        byte orbitalType;
        ArrayList<Integer> categoryIndexes;
        final ArrayList<String> categories;

        public MasterSatellite(int noradId, String name, String ownerCode, String ownerName, long launchDateMs)
        {
            String lowerName = name.toLowerCase();

            this.noradId = noradId;
            this.launchDateMs = launchDateMs;
            this.name = name;
            this.ownerCode = Globals.normalizeOwnerCode(ownerCode);
            this.ownerName = Globals.normalizeOwnerName(ownerName);
            if(lowerName.contains("r/b"))
            {
                this.orbitalType = Database.OrbitalType.RocketBody;
            }
            else if(isDebris(lowerName))
            {
                this.orbitalType = Database.OrbitalType.Debris;
            }
            else
            {
                this.orbitalType = Database.OrbitalType.Satellite;
            }
            this.categoryIndexes = new ArrayList<>(0);
            this.categories = new ArrayList<>(0);
        }

        public void addCategory(String catName, int catIndex)
        {
            int index;
            int size = categories.size();
            String lowerName = catName.toLowerCase();

            //go through each category
            for(index = 0; index < size; index++)
            {
                //if a match
                if(categories.get(index).toLowerCase().equals(lowerName))
                {
                    //already added
                    return;
                }
            }

            //add category and return index
            categories.add(catName);
            categoryIndexes.add(catIndex);
        }
    }

    //Master link
    public static class MasterLink
    {
        public final String group;
        public final String link;

        public MasterLink(String gp, String lnk)
        {
            group = gp.toUpperCase();
            link = lnk;
        }
    }

    //Master owner
    public static class MasterOwner implements Comparable<MasterOwner>
    {
        public final String code;
        public final String name;

        public static class Comparer implements Comparator<MasterOwner>
        {
            private final boolean codeOnly;

            public Comparer(boolean byCodeOnly)
            {
                codeOnly = byCodeOnly;
            }

            @Override
            public int compare(MasterOwner value1, MasterOwner value2)
            {
                String code1Lower = value1.code.toLowerCase();
                String code2Lower = value2.code.toLowerCase();

                if(!codeOnly && code1Lower.equals(code2Lower))
                {
                    return(value1.name.compareTo(value2.name));
                }
                else
                {
                    return(code1Lower.compareTo(code2Lower));
                }
            }
        }

        public MasterOwner(String cd, String nm)
        {
            code = Globals.normalizeOwnerCode(cd);
            name = Globals.normalizeOwnerName(nm);
        }

        @Override
        public int compareTo(@NonNull MasterOwner other)
        {
            boolean nameNull = (name == null);
            boolean otherNull = (other.name == null);

            //if either null
            if(nameNull || otherNull)
            {
                //if both null
                if(nameNull && otherNull)
                {
                    return(0);
                }
                //else only 1 null
                else
                {
                    return(otherNull ? 1 : -1);
                }
            }
            else
            {
                return(name.compareTo(other.name));
            }
        }
    }

    //Master category
    public static class MasterCategory implements Comparable<MasterCategory>
    {
        public final String name;
        public int index;

        public static class Comparer implements Comparator<MasterCategory>
        {
            private final boolean nameOnly;
            private final boolean indexOnly;

            public Comparer(boolean byNameOnly, boolean byIndexOnly)
            {
                nameOnly = byNameOnly;
                indexOnly = byIndexOnly;
            }

            @Override
            public int compare(MasterCategory value1, MasterCategory value2)
            {
                if(indexOnly || (!nameOnly && value1.name.equals(value2.name)))
                {
                    return(Globals.intCompare(value1.index, value2.index));
                }
                else
                {
                    return(value1.name.compareTo(value2.name));
                }
            }
        }

        public MasterCategory(String nm, int idx)
        {
            name = nm.toUpperCase();
            index = idx;
        }

        @Override
        public int compareTo(@NonNull MasterCategory other)
        {
            return(name.compareTo(other.name));
        }
    }

    //Master satellite category
    public static class MasterSatelliteCategory
    {
        public final int noradId;
        public final int categoryIndex;

        public static class Comparer implements Comparator<MasterSatelliteCategory>
        {
            @Override
            public int compare(MasterSatelliteCategory value1, MasterSatelliteCategory value2)
            {
                if(value1.noradId == value2.noradId)
                {
                    return(Globals.intCompare(value1.categoryIndex, value2.categoryIndex));
                }
                else
                {
                    return(Globals.intCompare(value1.noradId, value2.noradId));
                }
            }
        }

        public MasterSatelliteCategory(int nrId, int catIndex)
        {
            noradId = nrId;
            categoryIndex = catIndex;
        }
    }

    //Master list type
    public static class MasterListType
    {
        public boolean justUpdated;
        public ArrayList<MasterSatellite> satellites;
        private ArrayList<MasterCategory> categories;
        private final ArrayList<MasterCategory> categoriesByIndex;
        private final ArrayList<MasterCategory> newCategories;
        private ArrayList<MasterOwner> owners;
        private final ArrayList<MasterOwner> newOwners;
        public final ArrayList<MasterOwner> usedOwners;
        private ArrayList<MasterSatelliteCategory> satelliteCategories;
        private final ArrayList<MasterSatelliteCategory> newSatelliteCategories;
        public final ArrayList<String> usedCategories;

        public MasterListType()
        {
            justUpdated = false;
            satellites = new ArrayList<>(0);
            categories = new ArrayList<>(0);
            categoriesByIndex = new ArrayList<>(0);
            newCategories = new ArrayList<>(0);
            owners = new ArrayList<>(0);
            newOwners = new ArrayList<>(0);
            usedOwners = new ArrayList<>(0);
            satelliteCategories = new ArrayList<>(0);
            newSatelliteCategories = new ArrayList<>(0);
            usedCategories = new ArrayList<>(0);
        }

        public String getOwner(String ownerCode)
        {
            int index;

            //make case insensitive
            ownerCode = ownerCode.toLowerCase();

            //go through each owner
            for(index = 0; index < owners.size(); index++)
            {
                //if a match
                if(owners.get(index).code.toLowerCase().equals(ownerCode))
                {
                    //return index
                    return(owners.get(index).name);
                }
            }

            //not found
            return("");
        }

        public void addOwner(MasterOwner newOwner)
        {
            int[] indexes = Globals.divideFind(newOwner, owners, new MasterOwner.Comparer(true));

            if(indexes.length > 1)
            {
                owners.add(indexes[1], newOwner);
                newOwners.add(newOwner);
            }
        }

        public int categoryIndex(String name)
        {
            int index;
            MasterCategory newCategory = new MasterCategory(name, -1);
            index = Globals.divideFind(newCategory, categories, new MasterCategory.Comparer(true, false))[0];

            return(index >= 0 ? categories.get(index).index : -1);
        }

        public String categoryName(int index)
        {
            MasterCategory newCategory = new MasterCategory("", index);
            int index2 = Globals.divideFind(newCategory, categoriesByIndex, new MasterCategory.Comparer(false, true))[0];
            return(index2 >= 0 ? categoriesByIndex.get(index2).name : "");
        }

        public int addCategory(String name)
        {
            int index = -1;
            String nameLower = name.toLowerCase();

            //if name does not contain -"last" and "days"-
            if(!(nameLower.contains("last") && nameLower.contains("days")))
            {
                int[] indexes;
                MasterCategory newCategory = new MasterCategory(name, -1);

                //if not already in list
                indexes = Globals.divideFind(newCategory, categories, new MasterCategory.Comparer(true, false));
                if(indexes.length > 1)
                {
                    //add category
                    index = categories.size();
                    newCategory.index = index;
                    categories.add(indexes[1], newCategory);
                    newCategories.add(newCategory);

                    //add category sorted by index
                    indexes = Globals.divideFind(newCategory, categoriesByIndex, new MasterCategory.Comparer(false, true));
                    if(indexes.length > 1)
                    {
                        categoriesByIndex.add(indexes[1], newCategory);
                    }
                }
                else
                {
                    //set to existing index
                    index = categories.get(indexes[0]).index;
                }
            }

            //return index
            return(index);
        }

        public void addSatelliteCategory(int noradId, int categoryIndex)
        {
            int[] indexes;
            MasterSatelliteCategory satCat = new MasterSatelliteCategory(noradId, categoryIndex);

            indexes = Globals.divideFind(satCat, satelliteCategories, new MasterSatelliteCategory.Comparer());
            if(indexes.length > 1)
            {
                satelliteCategories.add(indexes[1], satCat);
                newSatelliteCategories.add(satCat);
            }
        }
    }

    //Celestrak catalog
    public static class CelestrakCatalog
    {
        static final int NoradIDStart = 12;
        static final int NoradIDEnd = 19;
        static final int OwnerStart = 48;
        static final int OwnerEnd = 55;
        static final int LaunchDateStart = 56;
        static final int LaunchDateEnd = 66;
    }

    //Space track country
    private static abstract class SpaceTrackCountry
    {
        @SuppressWarnings("SpellCheckingInspection")
        public static abstract class Constants
        {
            static final String Owner = "COUNTRY";
            static final String OwnerCode = "SPADOC_CD";
        }
    }

    //Space track satellite
    public static class SpaceTrackSatellite
    {
        @SuppressWarnings("SpellCheckingInspection")
        public static abstract class Constants
        {
            static final String InternationalCode = "INTLDES";
            static final String NoradID = "NORAD_CAT_ID";
            static final String Name = "SATNAME";
            static final String CountryCode = "COUNTRY";
            static final String ObjectType = "OBJECT_TYPE";
            static final String LaunchDate = "LAUNCH";
        }

        public int noradId;
        public String internationalCode;
        public String name;
        public String ownerCode;
        public byte orbitalType;
        public Calendar launchDate;
        public final ArrayList<String> categories;

        public SpaceTrackSatellite()
        {
            noradId = Integer.MAX_VALUE;
            internationalCode = name = ownerCode = null;
            orbitalType = Database.OrbitalType.Satellite;
            launchDate = null;
            categories = new ArrayList<>(10);
        }
    }

    //Space track satellite group
    public static class SpaceTrackSatelliteGroup
    {
        public int firstID;
        public int lastID;
        public SpaceTrackSatellite[] satellites;

        public SpaceTrackSatelliteGroup(SpaceTrackSatellite[] sats)
        {
            int index;

            firstID = Integer.MAX_VALUE;
            lastID = 0;
            satellites = new SpaceTrackSatellite[0];

            //if satellites are set
            if(sats != null)
            {
                //go through satellites
                for(index = 0; index < sats.length; index++)
                {
                    //remember current satellite
                    SpaceTrackSatellite currentSatellite = sats[index];

                    //if lowest ID so far
                    if(currentSatellite.noradId < firstID)
                    {
                        //set first
                        firstID = currentSatellite.noradId;
                    }

                    //if last satellite so far
                    if(currentSatellite.noradId > lastID)
                    {
                        //set last
                        lastID = currentSatellite.noradId;
                    }
                }

                //if a valid range
                if(lastID > firstID)
                {
                    //create satellites list
                    satellites = new SpaceTrackSatellite[(lastID - firstID) + 1];
                    for(index = 0; index < sats.length; index++)
                    {
                        //remember current satellite
                        SpaceTrackSatellite currentSatellite = sats[index];

                        //set satellite
                        satellites[currentSatellite.noradId - firstID] = currentSatellite;
                    }
                }
            }
        }
    }

    //Space track list
    private static class SpaceTrackList
    {
        public final boolean loginFailed;
        public final SpaceTrackSatellite[] satellites;
        public final MasterOwner[] owners;

        public SpaceTrackList(SpaceTrackSatellite[] satellites, MasterOwner[] owners, boolean loggedIn)
        {
            this.satellites = satellites;
            this.owners = owners;
            this.loginFailed = !loggedIn;
        }
    }

    //Owner item
    public static class OwnerItem
    {
        public final int noradId;
        public final String ownerCode;
        public final Calendar launchDate;

        public OwnerItem(int nrID, String ownerCode, Calendar launchDate)
        {
            this.noradId = nrID;
            this.ownerCode = Globals.normalizeOwnerCode(ownerCode);
            this.launchDate = launchDate;
        }
    }

    //Alarm update settings
    public static class AlarmUpdateSettings
    {
        public boolean enabled;
        public int hour;
        public int minute;
        public long rate;

        public AlarmUpdateSettings()
        {
            enabled = false;
            hour = 12;
            minute = 0;
            rate = (long)Calculations.MsPerDay;
        }

        public boolean equals(AlarmUpdateSettings other)
        {
            return(enabled == other.enabled && hour == other.hour && minute == other.minute && rate == other.rate);
        }
    }

    //Space track category
    @SuppressWarnings("SpellCheckingInspection")
    private static abstract class SpaceTrackCategory
    {
        private static final String[] urls = new String[]{"Amateur", "analyst_satellites", "brightgeo", "Geosynchronous", "Globalstar", "Human_Spaceflight", "Inmarsat", "Intelsat", "Iridium" ,"Navigation", "Orbcomm", "Special_Interest", "Visible", "Weather"};
        private static final String[] nameValues = new String[]{"Amateur", "Analyst", "Bright Geo", "Geosynchronous", "Globalstar", "Human Spaceflight", "Inmarsat", "Intelsat", "Iridium", "Navigation", "Orbcomm", "Special Interest", "Visible", "Weather"};
    }

    //Notify receiver
    public static class NotifyReceiver extends NotifyService.NotifyReceiver
    {
        private static final int UpdateSatelliteID = 1;
        private static final int UpdateListID = 2;
        private static final int RetrySatelliteID = 3;
        private static final int RetryListID = 4;
        private static final int DismissSatelliteID = 5;
        private static final int DismissListID = 6;

        @Override
        protected void onBootCompleted(Context context, long timeNowMs, AlarmManager manager)
        {
            long msHz;
            long timeMs;

            //go through each alarm update
            for(byte currentUpdate : new byte[]{UpdateType.GetMasterList, UpdateType.UpdateSatellites})
            {
                //get auto update time and rate in ms
                timeMs = Settings.getAutoUpdateNextMs(context, currentUpdate);
                msHz = Settings.getAutoUpdateRateMs(context, currentUpdate);

                //if times are valid
                if(timeMs  > 0 && msHz > 0)
                {
                    //while time is before now
                    while(timeMs < timeNowMs)
                    {
                        //go to next update interval
                        timeMs += msHz;
                    }

                    //set previously set alarm
                    manager.setInexactRepeating(AlarmManager.RTC_WAKEUP, timeMs, msHz, getAlarmPendingIntent(context, currentUpdate));
                }
            }
        }

        @Override
        protected void onRunService(Context context, Intent intent)
        {
            long timeMs;
            byte updateType = getUpdateType(intent);
            Calendar nextUpdateTIme = Calendar.getInstance();
            Resources res = context.getResources();
            Database.DatabaseSatellite[] satellites;

            //start update
            switch(updateType)
            {
                case UpdateType.GetMasterList:
                    //check for update
                    updateMasterList(context, Settings.getSatelliteSource(context), false, true, true);
                    break;

                case UpdateType.UpdateSatellites:
                    satellites = Database.getOrbitals(context, Database.getSatelliteConditions());
                    updateSatellites(context, res.getQuantityString(R.plurals.title_satellites_updating, satellites.length), new ArrayList<>(Arrays.asList(satellites)), true);
                    break;
            }

            //get current update time
            timeMs = Settings.getAutoUpdateNextMs(context, updateType);
            if(timeMs > 0)
            {
                //add next interval to time
                nextUpdateTIme.setTimeInMillis(timeMs + Settings.getAutoUpdateRateMs(context, updateType));

                //save next update time
                Settings.setAutoUpdateNextMs(context, updateType, nextUpdateTIme.getTimeInMillis());
            }
        }

        @Override
        protected void onCloseNotification(Context context, Intent intent, NotificationManagerCompat manager)
        {
            manager.cancel(getUpdateID(getUpdateType(intent)));
        }

        //Gets the update type from the given intent
        private byte getUpdateType(Intent intent)
        {
            return(intent.getByteExtra(ParamTypes.UpdateType, Byte.MAX_VALUE));
        }

        //Gets update ID for given update type
        private static int getUpdateID(byte updateType)
        {
            return(updateType == UpdateType.GetMasterList ? UpdateListID : UpdateSatelliteID);
        }

        //Gets an alarm pending intent
        private static PendingIntent getAlarmPendingIntent(Context context, byte updateType)
        {
            Intent alarmIntent = new Intent(context, NotifyReceiver.class);
            PendingIntent alarmPendingIntent;

            //create and return intent
            alarmIntent.putExtra(ParamTypes.UpdateType, updateType);
            alarmPendingIntent = Globals.getPendingBroadcastIntent(context, getUpdateID(updateType), alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            return(alarmPendingIntent);
        }

        //Set update state
        private static void setUpdateState(Context context, byte updateType, int startHour, int startMinute, long msHz, boolean use)
        {
            long timeMs = 0;
            PendingIntent alarmPendingIntent = getAlarmPendingIntent(context, updateType);
            AlarmManager manager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
            Calendar timeNow = Calendar.getInstance();
            Calendar startTime = Calendar.getInstance();

            //if using alarm
            if(use)
            {
                //set time to today with hour and minute
                startTime.set(Calendar.HOUR_OF_DAY, startHour);
                startTime.set(Calendar.MINUTE, startMinute);

                //if time is not after now
                if(!startTime.after(timeNow))
                {
                    //set to next interval
                    //note: msHz assumed to always be in days
                    startTime.add(Calendar.DAY_OF_YEAR, (int)Math.floor(msHz / Calculations.MsPerDay));
                }

                //update time in ms
                timeMs = startTime.getTimeInMillis();

                //if manager exists
                if(manager != null)
                {
                    //set alarm
                    manager.setInexactRepeating(AlarmManager.RTC_WAKEUP, timeMs, msHz, alarmPendingIntent);
                }
            }
            //else if manager exists
            else if(manager != null)
            {
                //cancel alarm
                manager.cancel(alarmPendingIntent);
            }

            //save next update time
            Settings.setAutoUpdateNextMs(context, updateType, timeMs);
        }

        //Set update time
        public static void setUpdateTime(Context context, byte updateType, AlarmUpdateSettings settings)
        {
            //stop any existing
            setUpdateState(context, updateType, 0, 0, 0, false);

            //if using
            if(settings.enabled)
            {
                //apply changes
                setUpdateState(context, updateType, settings.hour, settings.minute, settings.rate, true);
            }
        }
    }

    private static final byte TLE_LINE_LENGTH = 69;
    private static final long MIN_MASTER_LIST_UPDATE_TIME_MS = 86400000L;            //1 day
    private static final int CurrentYear = Calendar.getInstance().get(Calendar.YEAR);
    private static MasterListType masterList;
    private static final boolean[] cancelIntent = new boolean[UpdateType.UpdateCount];
    private static final boolean[] showNotification = new boolean[UpdateType.UpdateCount];
    private static final String[] currentError = new String[UpdateType.UpdateCount];
    private static final Intent[] currentIntent = new Intent[UpdateType.UpdateCount];
    public static final String UPDATE_FILTER = "updateServiceFilter";

    public UpdateService()
    {
        super("updateService", Globals.getChannelId(Globals.ChannelIds.Update));

        int index;

        //go through each status
        for(index = 0; index < UpdateType.UpdateCount; index++)
        {
            //don't cancel or show
            cancelIntent[index] = showNotification[index] = false;
        }
    }

    @Override
    protected void onClearIntent(byte index)
    {
        //if a valid index
        if(index < currentIntent.length)
        {
            //set intent
            currentIntent[index] = null;
        }
    }

    @Override
    protected void onRunIntent(Intent intent)
    {
        byte updateType = intent.getByteExtra(ParamTypes.UpdateType, Byte.MAX_VALUE);
        boolean getUpdate = intent.getBooleanExtra(ParamTypes.GetUpdate, false);
        boolean checkUpdate = intent.getBooleanExtra(ParamTypes.CheckUpdate, false);
        boolean runForeground = intent.getBooleanExtra(ParamTypes.RunForeground, false);
        int index = intent.getIntExtra(ParamTypes.Index, 0);
        int updateSource = intent.getIntExtra(ParamTypes.UpdateSource, Database.UpdateSource.SpaceTrack);
        int fileType = intent.getIntExtra(ParamTypes.FileType, Integer.MAX_VALUE);
        int fileSourceType = intent.getIntExtra(ParamTypes.FileSourceType, Integer.MAX_VALUE);
        Uri fileUri = intent.getParcelableExtra(ParamTypes.FileUri);
        String user = intent.getStringExtra(ParamTypes.User);
        String pwd = intent.getStringExtra(ParamTypes.Password);
        String section = intent.getStringExtra(ParamTypes.Section);
        String fileName = intent.getStringExtra(ParamTypes.FileName);
        String fileExt = intent.getStringExtra(ParamTypes.FileExt);
        String notifyTitle = intent.getStringExtra(ParamTypes.NotifyTitle);
        Calculations.TLEDataType tleData = intent.getParcelableExtra(ParamTypes.TLEData);
        NotificationCompat.Builder notifyBuilder = Globals.createNotifyBuilder(this, notifyChannelId);
        ArrayList<String> fileNames = intent.getStringArrayListExtra(ParamTypes.FileNames);
        ArrayList<String> filesData = intent.getStringArrayListExtra(ParamTypes.FilesData);
        ArrayList<Database.DatabaseSatellite> satelliteList = intent.getParcelableArrayListExtra(ParamTypes.Satellites);

        //handle if need to start in foreground
        Globals.startForeground(this, Globals.ChannelIds.Update, notifyBuilder, runForeground);

        //update intent and status
        currentIntent[updateType] = intent;
        currentError[updateType] = null;
        showNotification[updateType] = runForeground;
        cancelIntent[updateType] = false;

        //update notification
        currentNotify.title = notifyTitle;
        updateNotification(notifyBuilder, updateType);

        //set defaults
        if(section == null)
        {
            section = "";
        }
        if(fileName == null)
        {
            fileName = "";
        }
        if(satelliteList == null)
        {
            satelliteList = new ArrayList<>(0);
        }

        //update progress
        sendMessage(MessageTypes.General, updateType, section, Globals.ProgressType.Started);

        //handle based on update type
        switch(updateType)
        {
            case UpdateType.UpdateSatellites:
                //update satellites
                updateSatellites(updateSource, section, user, pwd, satelliteList);
                break;

            case UpdateType.GetMasterList:
                //get master list
                getMasterList(updateSource, user, pwd, getUpdate, checkUpdate);
                break;

            case UpdateType.LoadFile:
                //load files
                loadSatellite(fileNames, filesData);
                break;

            case UpdateType.SaveFile:
                //save file
                saveSatellitesFile(satelliteList.toArray(new Database.DatabaseSatellite[0]), fileUri, fileName, fileExt, fileType, fileSourceType);
                break;

            case UpdateType.BuildDatabase:
                //build database
                buildDatabase();
                break;

            case UpdateType.GetInformation:
                //get information
                getOrbitalInformation(index, updateSource, tleData);
                break;

            default:
                //nothing to update
                sendMessage(MessageTypes.General, updateType, section, Globals.ProgressType.Failed);
                break;
        }

        //reset
        currentIntent[updateType] = null;
        cancelIntent[updateType] = showNotification[updateType] = false;
    }

    //Updates notification and it's visibility
    private void updateNotification(NotificationCompat.Builder notifyBuilder, byte updateType)
    {
        updateNotification(notifyBuilder, NotifyReceiver.getUpdateID(updateType), 0, showNotification[updateType]);
    }

    //Sets notification visibility
    public static void setNotificationVisible(byte updateType, boolean show)
    {
        //if a valid update type, changing, and not cancelled
        if(updateType < showNotification.length && show != showNotification[updateType] && !cancelIntent[updateType])
        {
            //update visibility
            showNotification[updateType] = show;
        }
    }

    //Gets notification visibility
    public static boolean getNotificationVisible(byte updateType)
    {
        return(updateType < showNotification.length && showNotification[updateType]);
    }

    //Sets cancel
    public static void cancel(byte updateType)
    {
        //if a valid update type
        if(updateType < cancelIntent.length)
        {
            //cancel
            cancelIntent[updateType] = true;
        }
    }

    //Sends a message
    private void sendMessage(byte messageType, byte updateType, String section, long index, long count, long overall, int progressType, Object data)
    {
        int updateID = NotifyReceiver.getUpdateID(updateType);
        int dismissID = (updateType == UpdateType.GetMasterList ? NotifyReceiver.DismissListID : NotifyReceiver.DismissSatelliteID);
        int retryID = (updateType == UpdateType.GetMasterList ? NotifyReceiver.RetryListID : NotifyReceiver.RetrySatelliteID);
        Resources res = this.getResources();
        String titleDesc = res.getQuantityString(updateType == UpdateType.GetMasterList ? R.plurals.title_satellites_updating_list : R.plurals.title_satellites_updating_tles, (int)count);
        Bundle extraData = null;

        if(data != null)
        {
            extraData = new Bundle();

            //if a File
            if(data instanceof File)
            {
                //add File
                extraData.putSerializable(ParamTypes.UsedFile, (File)data);
            }
            //else if a String
            else if(data instanceof String)
            {
                //add String
                extraData.putSerializable(ParamTypes.Information, (String)data);
            }
            //else if an ArrayList
            else if(data instanceof ArrayList)
            {
                //remember list
                ArrayList<?> list = (ArrayList<?>)data;

                //if items in list
                if(!list.isEmpty())
                {
                    //if a list of Database.DatabaseSatellite
                    if(list.get(0) instanceof Database.DatabaseSatellite)
                    {
                        //create new list
                        ArrayList<Database.DatabaseSatellite> pendingSatellites = new ArrayList<>();
                        for(Object currentItem : list)
                        {
                            pendingSatellites.add((Database.DatabaseSatellite)currentItem);
                        }

                        //add new list
                        extraData.putParcelableArrayList(ParamTypes.PendingSatellites, pendingSatellites);
                    }
                }
            }
        }

        sendMessage(messageType, updateType, ParamTypes.UpdateType, Integer.MAX_VALUE, titleDesc, section, UPDATE_FILTER, NotifyReceiver.class, overall, 0, index, count, progressType, updateID, dismissID, retryID, showNotification[updateType], extraData);
    }
    private void sendMessage(byte messageType, byte updateType, String section, long index, long count, int progressType, Object data)
    {
        sendMessage(messageType, updateType, section, index, count, -1, progressType, data);
    }
    private void sendMessage(byte messageType, byte updateType, String section, long index, long count, long overall, int progressType)
    {
        sendMessage(messageType, updateType, section, index, count, overall, progressType, null);
    }
    private void sendMessage(byte messageType, byte updateType, String section, long index, long count, int progressType)
    {
        sendMessage(messageType, updateType, section, index, count, -1, progressType, null);
    }
    private void sendMessage(byte messageType, byte updateType, String section, int progressType)
    {
        sendMessage(messageType, updateType, section, 0, 0, -1, progressType, null);
    }

    //Gets current intent
    public static Intent getIntent(byte updateType)
    {
        //if a valid update type
        if(updateType < currentIntent.length)
        {
            //return current
            return(currentIntent[updateType]);
        }

        //invalid
        return(null);
    }

    //Gets update preferences
    private static SharedPreferences getPreferences(Context context)
    {
        return(context.getSharedPreferences("Update", Context.MODE_PRIVATE));
    }

    //Creates a new master list progress listener
    private Globals.OnProgressChangedListener createMasterListProgressListener(final byte messageType, final String displaySection, long overall)
    {
        return(new Globals.OnProgressChangedListener()
        {
            @Override
            public void onProgressChanged(int progressType, String section, final long updateIndex, final long updateCount)
            {
                UpdateService.this.sendMessage(messageType, UpdateType.GetMasterList, displaySection, updateIndex, updateCount, overall, progressType);
            }
        });
    }

    //Sends information message
    private void sendInformationMessage(int group, String infoString, String sourceString)
    {
        int status;
        boolean foundInfo = (infoString != null);
        Resources res = this.getResources();

        //if info found
        if(foundInfo)
        {
            //if source found
            if(sourceString != null)
            {
                //add source
                infoString += ("<br><br>" + sourceString);
            }
        }
        else
        {
            //not found
            infoString = "<p> " + res.getString(R.string.text_not_found) + "</p>";
        }

        //update status
        status = (foundInfo ? Globals.ProgressType.Finished : Globals.ProgressType.Failed);
        sendMessage(MessageTypes.Download, UpdateType.GetInformation, res.getString(R.string.title_information), group, 0, status, infoString);
    }

    //Tries to get orbital information
    private void getOrbitalInformation(final int group, int updateSource, Calculations.TLEDataType tleData)
    {
        int firstIndex;
        int startIndex;
        int endIndex;
        boolean haveTleData = (tleData != null);
        final int noradId = (haveTleData ? tleData.satelliteNum : Universe.IDs.None);
        boolean useInternet = true;
        boolean allowInformationTranslate = Settings.getTranslateInformation(this);
        String urlString = null;
        String infoString;
        String sourceString = null;
        String receivedPage;
        String receivedPageLower;
        String language = Globals.getLanguage(this);
        String launchYear = (haveTleData ? String.valueOf(tleData.launchYear) : "");
        String launchYearNum = (haveTleData ? (launchYear + "-" + String.format(Locale.US, "%03d", tleData.launchNum)) : "");

        //handle based on ID
        switch(noradId)
        {
            case Universe.IDs.Earth:
            case Universe.IDs.Sun:
            case Universe.IDs.Moon:
            case Universe.IDs.Mars:
            case Universe.IDs.Mercury:
            case Universe.IDs.Venus:
            case Universe.IDs.Jupiter:
            case Universe.IDs.Saturn:
            case Universe.IDs.Uranus:
            case Universe.IDs.Neptune:
            case Universe.IDs.Pluto:
            case Universe.IDs.Polaris:
                updateSource = Database.UpdateSource.SpaceDotCom;
                sourceString = "<p>Space.com</p><a>https://www.space.com</a>";
                useInternet = false;
                break;
        }

        //handle based on update source
        switch(updateSource)
        {
            case Database.UpdateSource.Celestrak:
                urlString = "https://www.celestrak.com/satcat/" + launchYear + "/" + launchYearNum + ".asp";
                sourceString = "<p>Celestrak</p><a>" + urlString + "</a>";
                break;

            case Database.UpdateSource.N2YO:
                urlString = "https://www.n2yo.com/satellite/?s=" + noradId;
                sourceString = "<p>N2YO</p><a>" + urlString + "</a>";
                break;

            case Database.UpdateSource.NASA:
                urlString = (haveTleData ? ("https://nssdc.gsfc.nasa.gov/nmc/spacecraft/display.action?id=" + launchYearNum + tleData.launchPiece) : "");
                sourceString = "<p>NASA</p><a>" + urlString + "</a>";
                break;
        }

        //try to get information from database
        infoString = Database.getInformation(this, noradId, updateSource, language);

        //if no database information yet, not allowed to translate, and not in English
        if(infoString == null && !allowInformationTranslate && !language.equals(Globals.Languages.English))
        {
            //try to get database information in English
            infoString = Database.getInformation(this, noradId, updateSource, Globals.Languages.English);
            if(infoString != null)
            {
                //use English instead
                language = Globals.Languages.English;
            }
        }

        //if no database information, can use internet, allowed to translate, and not English
        if(infoString == null && useInternet && allowInformationTranslate && !language.equals(Globals.Languages.English))
        {
            //try to get saved translation
            infoString = Globals.getSavedOnlineTranslation(this, "norad_" + noradId, updateSource, language);

            //if got information
            if(infoString != null)
            {
                //save information to local database
                Database.saveInformation(UpdateService.this, noradId, updateSource, language, infoString);
            }
        }

        //if still no information, can use internet, and url is set
        if(infoString == null && useInternet && urlString != null)
        {
            //if have TLE data
            if(haveTleData)
            {
                //get page
                receivedPage = Globals.getWebPage(urlString, true,null);
                receivedPageLower = (receivedPage != null ? receivedPage.toLowerCase() : null);
                if(receivedPageLower != null && !receivedPageLower.equals(""))
                {
                    switch(updateSource)
                    {
                        case Database.UpdateSource.Celestrak:
                            //get start
                            startIndex = receivedPageLower.indexOf("<blockquote>");
                            if(startIndex >= 0)
                            {
                                //set start
                                startIndex += 12;

                                //get end
                                endIndex = receivedPageLower.indexOf("</blockquote>");
                                if(endIndex >= 0)
                                {
                                    infoString = receivedPage.substring(startIndex, endIndex).trim();
                                }
                            }
                            break;

                        case Database.UpdateSource.N2YO:
                            //get first start
                            firstIndex = receivedPageLower.indexOf("<b>launch site</b>");
                            if(firstIndex >= 0)
                            {
                                //get start
                                firstIndex += 18;
                                startIndex = receivedPageLower.indexOf("<br/><br/>", firstIndex);
                                if(startIndex >= 0 && startIndex - firstIndex < 500)        //note: within 500 to make sure not near bottom of page
                                {
                                    //set start
                                    startIndex += 10;

                                    //get end
                                    endIndex = receivedPageLower.indexOf("</div>", startIndex);
                                    if(endIndex >= 0)
                                    {
                                        infoString = receivedPage.substring(startIndex, endIndex).trim();
                                    }
                                }
                            }
                            break;

                        default:
                        case Database.UpdateSource.NASA:
                            //get start
                            startIndex = receivedPageLower.indexOf("<h2>description</h2>");
                            if(startIndex >= 0)
                            {
                                //get end
                                startIndex += 20;
                                endIndex = receivedPageLower.indexOf("</div>", startIndex);
                                if(endIndex >= 0)
                                {
                                    infoString = receivedPage.substring(startIndex, endIndex).trim();
                                }
                            }
                            break;
                    }
                }
            }

            //if no length
            if(infoString != null && infoString.trim().length() == 0)
            {
                //null it
                infoString = null;
            }

            //if info found
            if(infoString != null)
            {
                final int source = updateSource;
                final String sourceDesc = sourceString;

                //remove any extra spaces and new lines
                infoString = infoString.replaceAll("\\s+", " ").replaceAll("\r", "").replace("\n", "").replace("<p> ", "<p>").replace(" </p>", "</p>").replace("</p> <p>", "</p><p>");

                //translate if needed
                Globals.translateText(this, infoString, new Globals.TranslateListener()
                {
                    @Override
                    public void onTranslate(String text, String toLanguage, boolean success)
                    {
                        boolean allowInformationShare = Settings.getShareTranslations(UpdateService.this);

                        //if translated
                        if(success)
                        {
                            //save information to local database
                            Database.saveInformation(UpdateService.this, noradId, source, toLanguage, text);

                            //if allowed to save information online and not English
                            if(allowInformationShare && !toLanguage.equals(Globals.Languages.English))
                            {
                                //save information to online database
                                Globals.saveOnlineTranslation(UpdateService.this,"norad_" + noradId, source, toLanguage, text);
                            }
                        }

                        //send information
                        sendInformationMessage(group, text, sourceDesc);
                    }
                });
            }
            else
            {
                //send no information
                sendInformationMessage(group, null, sourceString);
            }
        }
        else
        {
            //send information
            sendInformationMessage(group, infoString, sourceString);
        }
    }

    //Tries to get a space track web page
    private Globals.WebPageData[] getSpaceTrackWebPages(String[] urlStrings, String[] sections, String user, String pwd, Globals.OnProgressChangedListener[] listeners)
    {
        int index;
        boolean loginFailed;
        Globals.WebPageData loginData;
        Globals.WebPageData[] pages = new Globals.WebPageData[urlStrings.length];

        //login to space track
        loginData = Globals.loginSpaceTrack(user, pwd);
        loginFailed = (loginData.isDenied() || loginData.isLoginError());

        //go through each url while not cancelled
        for(index = 0; index < urlStrings.length && !loginFailed && !cancelIntent[UpdateType.GetMasterList]; index++)
        {
            //update status
            sendMessage(MessageTypes.Download, UpdateType.GetMasterList, sections[index], Globals.ProgressType.Started);

            //get page data
            pages[index] = Globals.getWebPage(urlStrings[index], null, listeners[index]);
            loginFailed = pages[index].isDenied();

            //update status
            sendMessage(MessageTypes.Download, UpdateType.GetMasterList, sections[index], Globals.ProgressType.Finished);
        }
        while(index < urlStrings.length)
        {
            //make sure page is still set
            pages[index] = new Globals.WebPageData(null, "", 401);
            index++;
        }

        //if connected
        if(loginData.connection != null)
        {
            //logout
            Globals.logoutSpaceTrack(null);
        }

        //return data for each page
        return(pages);
    }

    //Gets owner data from given page
    private static MasterOwner[] parseOwners(String htmlPage)
    {
        int index;
        String currentCode;
        JSONObject[] data = Globals.getJsonObjects(htmlPage);
        ArrayList<MasterOwner> owners = new ArrayList<>(0);

        //go through each object
        for(index = 0; index < data.length; index++)
        {
            JSONObject dataItem = data[index];

            try
            {
                currentCode = dataItem.getString(SpaceTrackCountry.Constants.OwnerCode);
                if(!currentCode.equalsIgnoreCase("all"))
                {
                    owners.add(new MasterOwner(currentCode, dataItem.getString(SpaceTrackCountry.Constants.Owner)));
                }
            }
            catch(Exception ex)
            {
                // do nothing
            }
        }

        return(owners.toArray(new MasterOwner[0]));
    }

    //Gets satellite data from given page
    private static SpaceTrackSatellite[] parseSatellites(String htmlPage)
    {
        int index;
        JSONObject[] data = Globals.getJsonObjects(htmlPage);
        ArrayList<SpaceTrackSatellite> satellites = new ArrayList<>(60000);

        //go through each object
        for(index = 0; index < data.length; index++)
        {
            String objectType;
            SpaceTrackSatellite currentSatellite = new SpaceTrackSatellite();
            JSONObject dataItem = data[index];

            try
            {
                currentSatellite.internationalCode = dataItem.getString(SpaceTrackSatellite.Constants.InternationalCode);
                currentSatellite.noradId = Globals.tryParseInt(dataItem.getString(SpaceTrackSatellite.Constants.NoradID));
                currentSatellite.ownerCode = Globals.normalizeOwnerCode(dataItem.getString(SpaceTrackSatellite.Constants.CountryCode));
                currentSatellite.name = dataItem.getString(SpaceTrackSatellite.Constants.Name);
                currentSatellite.launchDate = Globals.tryParseGMTDate(dataItem.getString(SpaceTrackSatellite.Constants.LaunchDate));

                objectType = dataItem.getString(SpaceTrackSatellite.Constants.ObjectType).toLowerCase();
                switch(objectType)
                {
                    case "rocket body":
                        currentSatellite.orbitalType = Database.OrbitalType.RocketBody;
                        break;

                    case "debris":
                        currentSatellite.orbitalType = Database.OrbitalType.Debris;
                        break;

                    default:
                        currentSatellite.orbitalType = Database.OrbitalType.Satellite;
                        break;

                }

                satellites.add(currentSatellite);
            }
            catch(Exception ex)
            {
                //do nothing
            }
        }

        return(satellites.toArray(new SpaceTrackSatellite[0]));
    }

    //Gets norad IDs from given page
    private static Integer[] parseNoradIDs(String htmlPage)
    {
        int currentID;
        int satNumIndex = 1;
        String[] lines = htmlPage.split("\n");
        ArrayList<Integer> ids = new ArrayList<>(0);

        //go through each line
        for(String line : lines)
        {
            //split columns
            String[] lineSplit = line.split(",");

            //if the column title line
            if(line.contains("NORAD_CAT_ID"))
            {
                //get satellite number column
                satNumIndex = Arrays.asList(lineSplit).indexOf("NORAD_CAT_ID");
            }
            //else if satellite number column is valid
            else if(satNumIndex >= 0 && satNumIndex < lineSplit.length)
            {
                //get ID
                currentID = Globals.tryParseInt(lineSplit[satNumIndex].replace("\"", ""));
                if(currentID != Integer.MAX_VALUE)
                {
                    //add ID
                    ids.add(currentID);
                }
            }
        }

        return(ids.toArray(new Integer[0]));
    }

    //Returns if string contains any of the given values
    private static boolean containsAny(String inputString, String[] values)
    {
        int index;

        //go through each value
        for(index = 0; index < values.length; index++)
        {
            //check for invalid characters in the line
            if(inputString.contains(values[index]))
            {
                //found value
                return(true);
            }
        }

        //not found
        return(false);
    }

    //Gets TLE data from given page
    private static String[] parseTLEData(String htmlPage, int satelliteNum)
    {
        int index = 0;
        boolean validTle = false;
        String tleLine1 = "";
        String tleLine2;
        String satelliteNumberString = String.format(Locale.US, "%05d", satelliteNum);
        String[] errorResult = new String[]{"", ""};
        String[] invalidChars = new String[]{"<", ">", "/", "\\", "!", "@", "#", "$", "%", "^", "&", "*", "(", ")", "{", "}", "[", "]", "\"", "?", "\"", "`", "~"};

        //search for the start of TLE line 1 while not found
        while(!validTle)
        {
            //find the next possible starting index
            index = htmlPage.indexOf("1 " + satelliteNumberString, index);

            //if the index was found and is long enough
            if(index >= 0 && (index + TLE_LINE_LENGTH) <= htmlPage.length())
            {
                //get TLE line 1
                tleLine1 = htmlPage.substring(index, index + TLE_LINE_LENGTH);

                //if no invalid chars are found in the line
                if(!containsAny(tleLine1, invalidChars))
                {
                    //done searching
                    validTle = true;
                }
                //else keep searching

                //jump ahead to next
                index++;
            }
            //else no index found or too short
            else
            {
                return(errorResult);
            }
        }

        //search for start of TLE line 2 while not found
        validTle = false;
        while(!validTle)
        {
            //find the next possible starting index
            index = htmlPage.indexOf("2 " + satelliteNumberString, index);

            //if the index was found and is long enough
            if(index >= 0 && (index + TLE_LINE_LENGTH) <= htmlPage.length())
            {
                //get TLE line 2
                tleLine2 = htmlPage.substring(index, index + TLE_LINE_LENGTH);

                //if no invalid chars are found in the line
                if(!containsAny(tleLine2, invalidChars))
                {
                    //both lines found, done searching
                    validTle = true;
                    return(new String[]{tleLine1, tleLine2});
                }
                //else keep searching

                //jump ahead to next
                index++;
            }
            //else no index found or too short
            else
            {
                return(errorResult);
            }
        }

        //no valid TLE data found
        return(errorResult);
    }

    //Removes HTML tags
    private static String removeHtmlTags(String htmlString)
    {
        int index;
        int index2;

        //if there is a tag start
        index = htmlString.indexOf('<');
        if(index >= 0 && index < htmlString.length() - 1)
        {
            //if there is a tag end
            index2 = htmlString.indexOf('>', index + 1);
            if(index2 > index)
            {
                //remove all same tags occurrences and continue
                return(removeHtmlTags(htmlString.replace(htmlString.substring(index, index + (index2 - index) + 1), "")));
            }
        }

        //no more tags
        return(htmlString);
    }

    //Tries to determine if satellite name is debris
    private static boolean isDebris(String satName)
    {
        int year = CurrentYear;
        int endYear = CurrentYear - 5;

        //if ends with deb suffix
        if(satName.toLowerCase().endsWith(" deb"))
        {
            //is debris
            return(true);
        }

        //check if name starts with past 5 years
        while(year >= endYear)
        {
            //if starts with year
            if(satName.startsWith(String.valueOf(year)))
            {
                //probably debris
                return(true);
            }

            //go to previous year
            year--;
        }

        //probably not debris
        return(false);
    }

    //Gets master list data from space-track
    @SuppressWarnings("SpellCheckingInspection")
    private SpaceTrackList getSpaceTrackList(String user, String password)
    {
        int index;
        int index2;
        int index3;
        boolean loginFailed;
        boolean loadDebris = Settings.getCatalogDebris(this);
        String section;
        Resources res = this.getResources();
        SpaceTrackSatelliteGroup satelliteGroup = new SpaceTrackSatelliteGroup(null);
        ArrayList<String> urls = new ArrayList<>(0);
        ArrayList<String> sections = new ArrayList<>(0);
        ArrayList<Globals.OnProgressChangedListener> listeners = new ArrayList<>(0);
        String[] localeNames = new String[SpaceTrackCategory.nameValues.length];
        Globals.WebPageData[] pages;
        MasterOwner[] owners = null;

        //add owner urls and listeners
        urls.add("https://www.space-track.org/basicspacedata/query/class/boxscore/orderby/COUNTRY%20asc/metadata/false");
        section = res.getString(R.string.title_owners);
        sections.add(section);
        listeners.add(createMasterListProgressListener(MessageTypes.Download, section, 10));        //10%

        //add catalog urls and listeners
        urls.add("https://www.space-track.org/basicspacedata/query/class/satcat/DECAY/null-val/" + (!loadDebris ? "OBJECT_TYPE/%3C%3EDEBRIS/" : "") + "orderby/NORAD_CAT_ID%20asc/metadata/false");
        section = res.getString(R.string.title_satellites);
        sections.add(section);
        listeners.add(createMasterListProgressListener(MessageTypes.Download, section, 20));        //20%

        //add category urls and listeners
        for(index = 0; index < SpaceTrackCategory.urls.length; index++)
        {
            urls.add("https://www.space-track.org/basicspacedata/query/class/satcat/CURRENT/Y/favorites/" + SpaceTrackCategory.urls[index] + "/orderby/NORAD_CAT_ID/format/csv/emptyresult/show");
            localeNames[index] = Database.LocaleCategory.getCategory(res, SpaceTrackCategory.nameValues[index]);
            sections.add(SpaceTrackCategory.nameValues[index]);
            listeners.add(createMasterListProgressListener(MessageTypes.Download, localeNames[index], 30 + (int)(((index + 1) / (float)SpaceTrackCategory.urls.length) * 35)));     //30 - 65%
        }

        //get pages
        pages = getSpaceTrackWebPages(urls.toArray(new String[0]), sections.toArray(new String[0]), user, password, listeners.toArray(new Globals.OnProgressChangedListener[0]));

        //if logged in
        loginFailed = pages[0].isDenied();
        if(!loginFailed)
        {
            //go through each page
            for(index = 0; index < pages.length; index++)
            {
                //update status
                sendMessage(MessageTypes.LoadPercent, UpdateType.GetMasterList, sections.get(index), index, pages.length, Globals.ProgressType.Started);

                //if got page data
                if(pages[index].gotData())
                {
                    switch(index)
                    {
                        case 0:
                            //get owners
                            owners = parseOwners(pages[0].pageData);
                            break;

                        case 1:
                            //get satellites
                            satelliteGroup = new SpaceTrackSatelliteGroup(parseSatellites(pages[1].pageData));
                            break;

                        default:
                            //get satellites in category
                            Integer[] currentIDs = parseNoradIDs(pages[index].pageData);

                            //go through each ID
                            for(index2 = 0; index2 < currentIDs.length; index2++)
                            {
                                SpaceTrackSatellite currentSatellite;

                                //if a valid index
                                index3 = currentIDs[index2] - satelliteGroup.firstID;
                                if(index3 >= 0 && index3 < satelliteGroup.satellites.length)
                                {
                                    //remember current satellite
                                    currentSatellite = satelliteGroup.satellites[index3];

                                    //if satellite exists
                                    if(currentSatellite != null)
                                    {
                                        //add category for satellite by name
                                        currentSatellite.categories.add(localeNames[index - 2]);
                                    }
                                }
                            }
                            break;
                    }
                }

                //update status (65 - 75%)
                sendMessage(MessageTypes.LoadPercent, UpdateType.GetMasterList, sections.get(index), index, pages.length, 65 + (int)((index + 1) / (float)pages.length * 10), Globals.ProgressType.Success);
            }
        }

        //return satellites
        return(new SpaceTrackList(satelliteGroup.satellites, owners, !loginFailed));
    }

    //Sets master list update time
    public static void setMasterListTime(Context context, String znId, long timeMs)
    {
        SharedPreferences.Editor writeSettings = getPreferences(context).edit();

        writeSettings.putString(PreferenceName.MasterTz, znId);
        writeSettings.putLong(PreferenceName.MasterTime, timeMs);
        writeSettings.apply();
    }

    //Gets last master list update time age in milliseconds
    public static long getMasterListAgeMs(Context context)
    {
        long timeNowMs;
        long masterListUpdateTimeMs;
        Calendar lastUpdateTime;
        SharedPreferences readSettings = getPreferences(context);

        timeNowMs = Calendar.getInstance().getTimeInMillis();
        lastUpdateTime = Globals.getCalendar(readSettings.getString(PreferenceName.MasterTz, ""), readSettings.getLong(PreferenceName.MasterTime, 0));
        masterListUpdateTimeMs = (lastUpdateTime != null ? lastUpdateTime.getTimeInMillis() : 0);

        //if no update, send maximum, else days between now and last
        return(masterListUpdateTimeMs == 0 ? Long.MAX_VALUE : (timeNowMs - masterListUpdateTimeMs));
    }

    //Returns true if master list needs an update
    public static boolean getMasterListNeedUpdate(Context context, int updateSource)
    {
        byte updateType = UpdateType.GetMasterList;
        int savedSourceType;
        long updateRateMs;
        SharedPreferences readSettings = getPreferences(context);

        //get saved update source and rate
        savedSourceType = readSettings.getInt(PreferenceName.MasterSource, Database.UpdateSource.SpaceTrack);
        updateRateMs = (Settings.getAutoUpdateNextMs(context, updateType) > 0 ? Settings.getAutoUpdateRateMs(context, updateType) : MIN_MASTER_LIST_UPDATE_TIME_MS);

        //need update if -source changed- or -has no update time- or -enough time has passed since last update-
        return(updateSource != savedSourceType || getMasterListAgeMs(context) >= updateRateMs);
    }

    //Tries to get the master list
    private void getMasterList(final int updateSource, final String user, final String pwd, boolean getUpdate, boolean checkUpdate)
    {
        boolean ranUpdate = false;
        int status = Globals.ProgressType.Finished;
        int[] indexes;
        String currentName;
        String currentCode;
        String unknown = Globals.getUnknownString(this).toUpperCase();

        //try to load list from database
        loadMasterList();

        //if -want to get update- or -checking for update and needed-
        if(getUpdate || (checkUpdate && getMasterListNeedUpdate(this, updateSource)))
        {
            //get updated list
            status = getMasterList(updateSource, UpdateSubSource.Satellites, 0, 0, user, pwd, null);
            ranUpdate = (status == Globals.ProgressType.Finished);
        }

        //if there are satellites
        if(masterList.satellites.size() > 0)
        {
            //if ran update and it was a success
            if(ranUpdate && !cancelIntent[UpdateType.GetMasterList])
            {
                //load categories
                loadCategories();

                //save updates
                saveMasterList(updateSource);

                //update status
                masterList.justUpdated = true;
            }

            //get used owners and categories
            for(MasterSatellite currentSatellite : masterList.satellites)
            {
                //remember current owner code and name
                currentCode = currentSatellite.ownerCode;
                currentName = Database.LocaleOwner.getName(this, currentCode);
                if(currentName == null)
                {
                    currentName = currentSatellite.ownerName;
                }

                //if UpdateSource.N2YO and unknown owner
                if(updateSource == Database.UpdateSource.N2YO && (currentCode == null || currentName == null))
                {
                    //set name to unknown
                    currentName = unknown;
                    currentCode = "";
                }

                //if code and name are set
                if(currentCode != null && currentName != null)
                {
                    //create and search for new owner
                    MasterOwner newOwner = new MasterOwner(currentCode, currentName);
                    indexes = Globals.divideFind(newOwner, masterList.usedOwners, new MasterOwner.Comparer(true));

                    //if owner is not used yet
                    if(indexes.length > 1)
                    {
                        //add owner
                        masterList.usedOwners.add(indexes[1], newOwner);
                    }
                }

                //go through current satellite categories
                for(String currentCategory : currentSatellite.categories)
                {
                    //if current category is set and not used yet
                    if(currentCategory != null && !masterList.usedCategories.contains(currentCategory))
                    {
                        //add category
                        masterList.usedCategories.add(currentCategory);
                    }
                }
            }
            Collections.sort(masterList.usedOwners);
            Collections.sort(masterList.usedCategories);
        }

        //update progress
        sendMessage(MessageTypes.General, UpdateType.GetMasterList, "", (status == Globals.ProgressType.Denied ? Globals.ProgressType.Denied : status == Globals.ProgressType.Finished && masterList.satellites.size() > 0 && !cancelIntent[UpdateType.GetMasterList] ? Globals.ProgressType.Finished : ranUpdate ? Globals.ProgressType.Failed : Globals.ProgressType.Cancelled));
    }

    //Gets master list and returns progress status
    @SuppressWarnings("SpellCheckingInspection")
    private int getMasterList(int updateSource, int updateSubSource, int linkIndex, int overall, String user, String pwd, MasterLink[] urls)
    {
        int index;
        int index2;
        int rowStart;
        int rowEnd;
        int colStart;
        int colEnd;
        int listEnd;
        int listOffset;
        int categoryStart;
        int categoryEnd;
        int linkStart;
        int linkEnd;
        int noradId = 0;
        int pageOffset = 0;
        int categoryIndex;
        int firstNoradID = Integer.MAX_VALUE;
        int lastNoradID = 0;
        int status = Globals.ProgressType.Finished;
        int receivedPageLength;
        byte updateType = UpdateType.GetMasterList;
        boolean needNoradId;
        boolean addLineEnds = false;
        boolean loadDebris = Settings.getCatalogDebris(this);
        boolean loadRocketBodies = Settings.getCatalogRocketBodies(this);
        Resources res = this.getResources();
        String urlBase;
        String urlMasterBase = "";
        String section = null;
        String receivedPage;
        String receivedPageLower;
        String rowStartText = null;
        String rowEndText = null;
        String currentRow;
        String currentRowLower;
        String currentValue;
        String currentValue2;
        String currentValue3;
        String currentName;
        String currentStatus;
        String currentCode = null;
        String parsingString = res.getString(R.string.title_parsing);
        SpaceTrackList spaceTrackData;
        ArrayList<MasterLink> urlList = new ArrayList<>(0);
        ArrayList<MasterSatellite> oldSatellites = new ArrayList<>(0);
        ArrayList<OwnerItem> ownerItemList = new ArrayList<>(0);

        //setup source specifics
        switch(updateSource)
        {
            case Database.UpdateSource.N2YO:
                urlBase = "https://www.n2yo.com/satellites/";

                switch(updateSubSource)
                {
                    case UpdateSubSource.Owners:
                        urlMasterBase = urlBase + "/?c=&t=country";
                        break;

                    case UpdateSubSource.SatelliteOwners:
                    case UpdateSubSource.Category:
                        //if urls are valid
                        if(urls != null && linkIndex < urls.length)
                        {
                            //use category url
                            urlMasterBase = urlBase + urls[linkIndex].link;

                            //remember section
                            section = urls[linkIndex].group;

                            //if UpdateSubSource.SatelliteOwners
                            if(updateSubSource == UpdateSubSource.SatelliteOwners)
                            {
                                //remember owner code
                                currentCode = section;
                            }
                            break;
                        }
                        //else fall through

                    default:
                    case UpdateSubSource.Satellites:
                        urlMasterBase = urlBase;
                        break;
                }

                rowStartText = "<tr";
                rowEndText = "</tr>";
                break;

            case Database.UpdateSource.SpaceTrack:
                //do nothing here
                break;

            default:
            case Database.UpdateSource.Celestrak:
                urlBase = "https://www.celestrak.com/";

                switch(updateSubSource)
                {
                    case UpdateSubSource.Owners:
                        urlMasterBase = urlBase + "satcat/sources.asp";
                        rowStartText = "<tr align=center";
                        rowEndText = "</tr>";
                        break;

                    case UpdateSubSource.SatelliteOwners:
                        urlMasterBase = urlBase + "pub/satcat.txt";
                        rowEndText = "\r\n";
                        addLineEnds = true;
                        break;

                    default:
                    case UpdateSubSource.Satellites:
                        urlMasterBase = urlBase + "NORAD/elements/master.asp";
                        rowStartText = "<th";
                        rowEndText = "</th>";
                        break;
                }
                break;
        }

        //if getting satellites
        if(updateSubSource == UpdateSubSource.Satellites)
        {
            //backup old and start new satellite list
            oldSatellites = masterList.satellites;
            masterList.satellites = new ArrayList<>(0);
        }

        //if updating from space track
        if(updateSource == Database.UpdateSource.SpaceTrack)
        {
            //get list
            spaceTrackData = getSpaceTrackList(user, pwd);

            //if logged in
            if(!spaceTrackData.loginFailed)
            {
                //if got satellites
                if(spaceTrackData.satellites != null)
                {
                    //set status
                    currentStatus = res.getString(R.string.title_satellites);

                    //go through each satellite while not cancelled
                    for(index = 0; index < spaceTrackData.satellites.length && !cancelIntent[updateType]; index++)
                    {
                        //remember current satellite
                        SpaceTrackSatellite currentSatellite = spaceTrackData.satellites[index];
                        MasterSatellite newSatellite;

                        //update status
                        sendMessage(MessageTypes.LoadPercent, UpdateType.GetMasterList, currentStatus, index, spaceTrackData.satellites.length, Globals.ProgressType.Started);

                        //if current satellite exists
                        if(currentSatellite != null)
                        {
                            //get owner
                            currentValue = currentValue2 = "";
                            currentCode = currentSatellite.ownerCode.toLowerCase();
                            if(spaceTrackData.owners != null)
                            {
                                //go through owners
                                for(index2 = 0; index2 < spaceTrackData.owners.length && currentValue.equals(""); index2++)
                                {
                                    MasterOwner currentOwner = spaceTrackData.owners[index2];

                                    //if code matches
                                    if(currentCode.equals(currentOwner.code.toLowerCase()))
                                    {
                                        //set owner
                                        currentValue = currentOwner.code;
                                        currentValue2 = currentOwner.name;
                                    }
                                }
                            }

                            //create new satellite
                            newSatellite = new MasterSatellite(currentSatellite.noradId, currentSatellite.name, currentValue, currentValue2, (currentSatellite.launchDate != null ? currentSatellite.launchDate.getTimeInMillis() : Globals.UNKNOWN_DATE_MS));
                            newSatellite.orbitalType = currentSatellite.orbitalType;

                            //go through each category
                            for(index2 = 0; index2 < currentSatellite.categories.size(); index2++)
                            {
                                //get current category
                                currentValue3 = currentSatellite.categories.get(index2);

                                //add category
                                categoryIndex = masterList.addCategory(currentValue3);
                                masterList.addSatelliteCategory(currentSatellite.noradId, categoryIndex);

                                //update new satellite categories
                                newSatellite.categories.add(currentValue3);
                                newSatellite.categoryIndexes.add(categoryIndex);
                            }

                            //if loading rocket bodies or not a rocket body
                            //note: debris already filtered out if needed
                            if(loadRocketBodies || !currentSatellite.name.toLowerCase().contains("r/b"))
                            {
                                //add satellite to list
                                masterList.satellites.add(newSatellite);
                            }
                        }

                        //update status
                        sendMessage(MessageTypes.LoadPercent, UpdateType.GetMasterList, currentStatus, index, spaceTrackData.satellites.length, Globals.ProgressType.Success);
                    }
                }
                else
                {
                    //no satellites
                    return(Globals.ProgressType.Cancelled);
                }

                //if got owners
                if(spaceTrackData.owners != null)
                {
                    //go through each owner while not cancelled
                    for(index = 0; index < spaceTrackData.owners.length && !cancelIntent[updateType]; index++)
                    {
                        //remember current owner
                        MasterOwner currentOwner = spaceTrackData.owners[index];

                        //add if a new owner
                        masterList.addOwner(new MasterOwner(currentOwner.code, currentOwner.name));
                    }

                    //got everything
                    return(Globals.ProgressType.Finished);
                }
                else
                {
                    //no owners
                    return(Globals.ProgressType.Cancelled);
                }
            }
            else
            {
                //login failed
                return(Globals.ProgressType.Denied);
            }
        }
        else
        {
            //update status
            if(section == null)
            {
                switch(updateSubSource)
                {
                    case UpdateSubSource.Category:
                        section = res.getString(R.string.title_categories);
                        break;

                    case UpdateSubSource.Satellites:
                        section = res.getString(R.string.title_satellites);
                        break;

                    case UpdateSubSource.Owners:
                    case UpdateSubSource.SatelliteOwners:
                        section = res.getString(R.string.title_owners);
                        break;
                }
            }
            sendMessage(MessageTypes.Download, UpdateType.GetMasterList, section, Globals.ProgressType.Started);

            //get page
            receivedPage = Globals.getWebPage(urlMasterBase, true, createMasterListProgressListener(MessageTypes.Download, section, overall));
            receivedPageLength = (receivedPage != null ? receivedPage.length() : 0);
            receivedPageLower = (receivedPage != null ? receivedPage.toLowerCase() : null);
            if(receivedPageLength > 0)
            {
                //go through page while not cancelled
                do
                {
                    //update celestrak progress message (overall - overall +25%)
                    sendMessage(MessageTypes.Parse, UpdateType.GetMasterList, section + " " + parsingString, pageOffset, receivedPageLength, (updateSource == Database.UpdateSource.Celestrak ? (overall + (int)((pageOffset / (float)receivedPageLength) * 25)) : 0), Globals.ProgressType.Success);

                    //find next txt file row start while not cancelled
                    rowStart = (rowStartText != null ? receivedPageLower.indexOf(rowStartText, pageOffset) : pageOffset);
                    if(rowStart >= 0 && rowStart < receivedPageLength - 1 && !cancelIntent[updateType])
                    {
                        //find next txt file row end
                        rowEnd = receivedPageLower.indexOf(rowEndText, rowStart);
                        if(rowEnd > rowStart && rowEnd < receivedPageLength)
                        {
                            //get entire row and reset status
                            currentRow = receivedPage.substring(rowStart, rowStart + (rowEnd - rowStart) + rowEndText.length());
                            currentRowLower = currentRow.toLowerCase();
                            listEnd = -1;

                            //handle based on source
                            switch(updateSource)
                            {
                                case Database.UpdateSource.N2YO:
                                    //handle based on sub source
                                    switch(updateSubSource)
                                    {
                                        case UpdateSubSource.Satellites:
                                            //if current row contains a column and link
                                            colStart = currentRowLower.indexOf("<td><a href=\"");
                                            if(colStart >= 0)
                                            {
                                                //get column end
                                                colEnd = currentRowLower.indexOf("</td>", colStart);
                                                if(colEnd >= 0)
                                                {
                                                    //get link
                                                    linkStart = colStart + 13;
                                                    linkEnd = currentRowLower.indexOf("\">", linkStart);
                                                    if(linkEnd >= 0)
                                                    {
                                                        //get category
                                                        categoryEnd = currentRowLower.indexOf("<", linkEnd + 2);
                                                        if(categoryEnd >= 0)
                                                        {
                                                            //set category and url
                                                            currentValue = currentRow.substring(linkEnd + 2, categoryEnd);

                                                            //add if a new category and updates urls
                                                            masterList.addCategory(currentValue);
                                                            urlList.add(new MasterLink(currentValue, currentRow.substring(linkStart, linkEnd)));
                                                        }
                                                    }
                                                }
                                            }
                                            break;

                                        case UpdateSubSource.Category:
                                            //if current row contains a column and link
                                            colStart = currentRowLower.indexOf("<td align=left><a href=\"");
                                            if(colStart >= 0)
                                            {
                                                //get column end
                                                colEnd = currentRowLower.indexOf("</td>", colStart);
                                                if(colEnd >= 0)
                                                {
                                                    //get link starting and ending
                                                    linkStart = colStart + 15;
                                                    linkEnd = currentRowLower.indexOf("</a>") + 4;
                                                    if(linkStart < linkEnd)
                                                    {
                                                        //get current name
                                                        currentName = removeHtmlTags(currentRow.substring(linkStart, linkEnd));

                                                        //get next column start
                                                        colStart = colEnd + 5;
                                                        if(colStart < currentRow.length())
                                                        {
                                                            //get column end
                                                            colEnd = currentRowLower.indexOf("</td>", colStart);
                                                            if(colEnd >= 0)
                                                            {
                                                                //get current value
                                                                currentValue = removeHtmlTags(currentRow.substring(colStart, colEnd));

                                                                //if able to get norad ID
                                                                noradId = Globals.tryParseInt(currentValue);
                                                                if(noradId != Integer.MAX_VALUE && urls != null && linkIndex < urls.length)
                                                                {
                                                                    //create satellite
                                                                    MasterSatellite newSatellite = new MasterSatellite(noradId, currentName, "", "", Globals.UNKNOWN_DATE_MS);

                                                                    //if -loading rocket bodies or not a rocket body- and -loading debris or not debris-
                                                                    if((loadRocketBodies || newSatellite.orbitalType != Database.OrbitalType.RocketBody) && (loadDebris || newSatellite.orbitalType != Database.OrbitalType.Debris))
                                                                    {
                                                                        //add satellite to the list
                                                                        masterList.satellites.add(newSatellite);
                                                                    }

                                                                    //add satellite category
                                                                    categoryIndex = masterList.categoryIndex(urls[linkIndex].group);
                                                                    if(categoryIndex >= 0)
                                                                    {
                                                                        masterList.addSatelliteCategory(noradId, categoryIndex);
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            break;

                                        case UpdateSubSource.Owners:
                                            //if current row contains column
                                            colStart = currentRowLower.indexOf("<td>");
                                            if(colStart >= 0)
                                            {
                                                //get column end
                                                colEnd = currentRowLower.indexOf("</a></td>", colStart);
                                                if(colEnd >= 0)
                                                {
                                                    //get current name
                                                    currentName = removeHtmlTags(currentRow.substring(colStart, colEnd));

                                                    //get next column start
                                                    colStart = currentRowLower.indexOf("</td><td", colEnd + 8);
                                                    if(colStart >= 0)
                                                    {
                                                        //get next column end
                                                        colEnd = currentRowLower.indexOf("</td>", colStart + 7);
                                                        if(colEnd >= 0)
                                                        {
                                                            //get current value
                                                            currentValue = removeHtmlTags(currentRow.substring(colStart, colEnd));

                                                            //add if a new owner and update urls
                                                            masterList.addOwner(new MasterOwner(currentValue, currentName));
                                                            urlList.add(new MasterLink(currentValue, "?c=" + currentValue + "&t=country"));
                                                        }
                                                    }
                                                }
                                            }
                                            break;

                                        case UpdateSubSource.SatelliteOwners:
                                            //if current row contains satellite link
                                            colStart = currentRowLower.indexOf("><a href=\"/satellite/?s=");
                                            if(colStart >= 0)
                                            {
                                                //get column end
                                                colEnd = currentRowLower.indexOf("\">", colStart + 22);
                                                if(colEnd >= 0)
                                                {
                                                    //get current value
                                                    currentValue = currentRow.substring(colStart + 24, colEnd);

                                                    //if able to get norad ID
                                                    noradId = Globals.tryParseInt(currentValue);
                                                    if(noradId != Integer.MAX_VALUE && currentCode != null)
                                                    {
                                                        //add owner item
                                                        ownerItemList.add(new OwnerItem(noradId, currentCode, null));

                                                        //check if first/last norad ID
                                                        if(noradId < firstNoradID)
                                                        {
                                                            //update first
                                                            firstNoradID = noradId;
                                                        }
                                                        if(noradId > lastNoradID)
                                                        {
                                                            //update last
                                                            lastNoradID = noradId;
                                                        }
                                                    }
                                                }
                                            }
                                            break;
                                    }
                                    break;

                                default:
                                case Database.UpdateSource.Celestrak:
                                    //handle based on sub source
                                    switch(updateSubSource)
                                    {
                                        case UpdateSubSource.Satellites:
                                            //if current row contains a txt file link
                                            linkEnd = currentRowLower.indexOf(".txt\"");
                                            if(linkEnd >= 0)
                                            {
                                                //get link start
                                                linkStart = currentRowLower.indexOf("<a href=\"");
                                                if(linkStart >= 0 && linkStart < linkEnd)
                                                {
                                                    //reset
                                                    categoryIndex = -1;

                                                    //get category start
                                                    categoryStart = currentRowLower.indexOf(">") + 1;
                                                    if(categoryStart > 0)
                                                    {
                                                        //end should be right before .txt link
                                                        categoryEnd = linkStart - 2;
                                                        if(categoryEnd > categoryStart)
                                                        {
                                                            //get category
                                                            currentValue = currentRow.substring(categoryStart, categoryEnd).trim();

                                                            //add if a new category
                                                            categoryIndex = masterList.addCategory(currentValue);
                                                        }
                                                    }

                                                    //get end of satellites list
                                                    listEnd = receivedPageLower.indexOf("</table>", rowEnd + 1);
                                                    if(listEnd > rowEnd)
                                                    {
                                                        //reset status
                                                        needNoradId = true;

                                                        //go through list
                                                        listOffset = rowEnd;
                                                        do
                                                        {
                                                            //get column end
                                                            colEnd = receivedPageLower.indexOf("</td>", listOffset);
                                                            if(colEnd > listOffset && colEnd < listEnd)
                                                            {
                                                                //get column start
                                                                colStart = receivedPageLower.indexOf("<td", listOffset);
                                                                if(colStart >= listOffset && colStart < colEnd)
                                                                {
                                                                    //get current value
                                                                    //note: uses entire <TD>..</TD> block as input
                                                                    currentValue = removeHtmlTags(receivedPage.substring(colStart, colStart + (colEnd - colStart) + 5));

                                                                    //update satellite data
                                                                    if(needNoradId)
                                                                    {
                                                                        //if unable to get norad ID
                                                                        noradId = Globals.tryParseInt(currentValue);
                                                                        if(noradId == Integer.MAX_VALUE)
                                                                        {
                                                                            //default to 0
                                                                            noradId = 0;
                                                                        }
                                                                        needNoradId = false;
                                                                    }
                                                                    else
                                                                    {
                                                                        //if a valid name
                                                                        if(!currentValue.equals("") && !currentValue.equalsIgnoreCase("UNKNOWN"))
                                                                        {
                                                                            //create satellite
                                                                            MasterSatellite newSatellite = new MasterSatellite(noradId, currentValue, "", "", Globals.UNKNOWN_DATE_MS);

                                                                            //if -loading rocket bodies or not a rocket body- and -loading debris or not debris-
                                                                            if((loadRocketBodies || newSatellite.orbitalType != Database.OrbitalType.RocketBody) && (loadDebris || newSatellite.orbitalType != Database.OrbitalType.Debris))
                                                                            {
                                                                                //add satellite to the list
                                                                                masterList.satellites.add(newSatellite);
                                                                            }

                                                                            //if a valid category
                                                                            if(categoryIndex >= 0)
                                                                            {
                                                                                //add satellite category
                                                                                masterList.addSatelliteCategory(noradId, categoryIndex);
                                                                            }
                                                                        }

                                                                        //update status
                                                                        needNoradId = true;
                                                                    }

                                                                    //update status
                                                                    listOffset = colEnd + 1;
                                                                }
                                                                else
                                                                {
                                                                    //done
                                                                    listOffset = Integer.MAX_VALUE - 1;
                                                                }
                                                            }
                                                            else
                                                            {
                                                                //done
                                                                listOffset = Integer.MAX_VALUE - 1;
                                                            }

                                                        } while(listOffset < listEnd);
                                                    }
                                                }
                                            }
                                            break;

                                        case UpdateSubSource.Owners:
                                            //if current row contains a column
                                            colStart = currentRowLower.indexOf("<td>");
                                            if(colStart >= 0)
                                            {
                                                //get column end
                                                colEnd = currentRowLower.indexOf("</td>", colStart);
                                                if(colEnd >= 0)
                                                {
                                                    //get country code (without possible formatting)
                                                    currentValue = removeHtmlTags(currentRow.substring(colStart + 4, colEnd).trim());

                                                    //get next column
                                                    colStart = currentRowLower.indexOf("<td>", colEnd + 4);
                                                    if(colStart >= 0)
                                                    {
                                                        //get next column end
                                                        colEnd = currentRowLower.indexOf("</td>", colStart);
                                                        if(colEnd >= 0)
                                                        {
                                                            //get country name (without possible link)
                                                            currentValue2 = removeHtmlTags(currentRow.substring(colStart + 4, colEnd).trim());

                                                            //add if a new owner
                                                            masterList.addOwner(new MasterOwner(currentValue, currentValue2));
                                                        }
                                                    }
                                                }
                                            }
                                            break;

                                        case UpdateSubSource.SatelliteOwners:
                                            if(currentRow.length() > CelestrakCatalog.LaunchDateEnd + 1)
                                            {
                                                //get norad ID and owner
                                                noradId = Globals.tryParseInt(currentRow.substring(CelestrakCatalog.NoradIDStart, CelestrakCatalog.NoradIDEnd).trim());
                                                currentCode = currentRow.substring(CelestrakCatalog.OwnerStart, CelestrakCatalog.OwnerEnd).trim();
                                                Calendar launchDate = Globals.tryParseGMTDate(currentRow.substring(CelestrakCatalog.LaunchDateStart, CelestrakCatalog.LaunchDateEnd).trim());

                                                //if a valid norad ID and got owner code
                                                if(noradId >= 0 && noradId < Integer.MAX_VALUE && currentCode.length() > 0)
                                                {
                                                    //add owner and launch date items
                                                    ownerItemList.add(new OwnerItem(noradId, currentCode, launchDate));

                                                    //check if last norad ID
                                                    if(noradId > lastNoradID)
                                                    {
                                                        //update last
                                                        lastNoradID = noradId;
                                                    }
                                                }
                                            }
                                            break;
                                    }
                                    break;
                            }

                            //start after current set/row
                            pageOffset = (listEnd > -1 ? listEnd : rowEnd) + (addLineEnds ? 2 : 1);
                        }
                        else
                        {
                            //done
                            pageOffset = Integer.MAX_VALUE - 1;
                        }
                    }
                    else
                    {
                        //done
                        pageOffset = Integer.MAX_VALUE - 1;
                    }

                } while(pageOffset < receivedPageLength && !cancelIntent[updateType]);

                //handle any needed extra sub sources
                switch(updateSubSource)
                {
                    //getting satellites
                    case UpdateSubSource.Satellites:
                        switch(updateSource)
                        {
                            case Database.UpdateSource.N2YO:
                                //remember urls and file count
                                urls = urlList.toArray(new MasterLink[0]);

                                //get categories while not cancelled
                                for(index = 0; index < urls.length && !cancelIntent[updateType] && status == Globals.ProgressType.Finished; index++)
                                {
                                    //get satellites from categories (0 - 35%)
                                    status = getMasterList(Database.UpdateSource.N2YO, UpdateSubSource.Category, index, (int)(((index + 1) / (float)urls.length) * 35), null, null, urls);
                                }

                                //if not cancelled and got categories
                                if(!cancelIntent[updateType] && status == Globals.ProgressType.Finished)
                                {
                                    //get owners (40%)
                                    status = getMasterList(Database.UpdateSource.N2YO, UpdateSubSource.Owners, 0, 40, null, null, null);
                                }
                                break;

                            case Database.UpdateSource.Celestrak:
                                //if not cancelled
                                if(!cancelIntent[updateType])
                                {
                                    //get owners (25 - 50%)
                                    status = getMasterList(Database.UpdateSource.Celestrak, UpdateSubSource.Owners, 0, 25, null, null, null);
                                }

                                //if not cancelled and got owners
                                if(!cancelIntent[updateType] && status == Globals.ProgressType.Finished)
                                {
                                    //get satellite owners (50 - 75%)
                                    status = getMasterList(Database.UpdateSource.Celestrak, UpdateSubSource.SatelliteOwners, 0, 50, null, null, null);
                                }
                                break;
                        }
                        break;

                    //getting owners
                    case UpdateSubSource.Owners:
                        switch(updateSource)
                        {
                            case Database.UpdateSource.N2YO:
                                //remember urls
                                urls = urlList.toArray(new MasterLink[0]);

                                //get satellite owners while not cancelled (40 - 75%)
                                for(index = 0; index < urls.length && !cancelIntent[updateType] && status == Globals.ProgressType.Finished; index++)
                                {
                                    status = getMasterList(Database.UpdateSource.N2YO, UpdateSubSource.SatelliteOwners, index, overall + (int)(((index + 1) / (float)urls.length) * 35), null, null, urls);
                                }
                                break;
                        }
                        break;

                    //getting satellite owners
                    case UpdateSubSource.SatelliteOwners:
                        switch(updateSource)
                        {
                            case Database.UpdateSource.N2YO:
                            case Database.UpdateSource.Celestrak:
                                //if got owners
                                if(ownerItemList.size() > 0)
                                {
                                    //create offset and array to hold catalog items, indexed by norad ID
                                    int offset = (firstNoradID != Integer.MAX_VALUE ? firstNoradID : 0);
                                    OwnerItem[] ownerItems = new OwnerItem[(lastNoradID - offset) + 1];
                                    ArrayList<ArrayList<Integer>> satCatItems = new ArrayList<>(ownerItems.length);

                                    //go through owner and launch items while not cancelled
                                    for(index = 0; index < ownerItemList.size() && !cancelIntent[updateType]; index++)
                                    {
                                        //remember current item
                                        OwnerItem currentItem = ownerItemList.get(index);

                                        //set item
                                        ownerItems[currentItem.noradId - offset] = currentItem;
                                    }

                                    //go through satellite owner and category items while not cancelled
                                    for(index = 0; index < ownerItems.length && !cancelIntent[updateType]; index++)
                                    {
                                        //add empty list
                                        satCatItems.add(new ArrayList<>(0));
                                    }
                                    for(index = 0; index < masterList.satelliteCategories.size() && !cancelIntent[updateType]; index++)
                                    {
                                        //remember current item
                                        MasterSatelliteCategory currentItem = masterList.satelliteCategories.get(index);

                                        //if norad ID is within range
                                        noradId = currentItem.noradId - offset;
                                        if(noradId >= 0 && noradId < satCatItems.size())
                                        {
                                            //add item
                                            satCatItems.get(noradId).add(currentItem.categoryIndex);
                                        }
                                    }

                                    //go through satellites while not cancelled
                                    for(index = 0; index < masterList.satellites.size() && !cancelIntent[updateType]; index++)
                                    {
                                        //get current satellite and norad ID
                                        MasterSatellite currentSatellite = masterList.satellites.get(index);
                                        noradId = currentSatellite.noradId - offset;

                                        //if in items
                                        if(currentSatellite.noradId >= offset && noradId < ownerItems.length)
                                        {
                                            //remember current items
                                            OwnerItem currentOwnerItem = ownerItems[noradId];
                                            ArrayList<Integer> categoryIndexes = satCatItems.get(noradId);

                                            //if current item exists
                                            if(currentOwnerItem != null)
                                            {
                                                //update owner
                                                currentSatellite.ownerCode = currentOwnerItem.ownerCode;
                                                currentSatellite.ownerName = masterList.getOwner(currentOwnerItem.ownerCode);
                                                if(currentOwnerItem.launchDate != null)
                                                {
                                                    currentSatellite.launchDateMs = currentOwnerItem.launchDate.getTimeInMillis();
                                                }
                                            }
                                            if(categoryIndexes != null)
                                            {
                                                //update categories
                                                currentSatellite.categoryIndexes = categoryIndexes;
                                                for(index2 = 0; index2 < categoryIndexes.size(); index2++)
                                                {
                                                    //add category for category index
                                                    currentSatellite.categories.add(masterList.categoryName(categoryIndexes.get(index2)));
                                                }
                                            }
                                        }
                                    }
                                }
                                else
                                {
                                    //failed to get owners
                                    status = Globals.ProgressType.Cancelled;
                                }
                                break;
                        }
                        break;
                }
            }
            else
            {
                //failed to get page
                status = Globals.ProgressType.Cancelled;
            }

            //if cancelled
            if(cancelIntent[updateType])
            {
                //update status
                status = Globals.ProgressType.Cancelled;
            }

            //if did not get satellites
            if(updateSubSource == UpdateSubSource.Satellites && status != Globals.ProgressType.Finished)
            {
                //use old satellite list
                masterList.satellites = oldSatellites;
            }

            //update status
            sendMessage(MessageTypes.Download, UpdateType.GetMasterList, section, status);
            return(status);
        }
    }

    //Saves satellite(s) based on input and returns number saved
    //note: satelliteNum < 0 searches input for name(s) and number(s)
    private int saveSatellite(String inputString, String satelliteName, int satelliteNum, String ownerCode, long launchDate, byte orbitalType, ArrayList<Database.DatabaseSatellite> pendingSaveSatellites)
    {
        int index;
        int index2;
        int inputOffset;
        int nameStartIndex;
        int nameEndIndex;
        int line1Index;
        int line2Index;
        int currentNum;
        int saveCount = 0;
        int inputLength = inputString.length();
        boolean unknownSatellite = (satelliteNum < 0);
        boolean isGpData = inputString.contains(Calculations.GPParams.Name);
        String currentNumString;
        Calendar defaultLaunchDate = Calendar.getInstance();
        ArrayList<Integer> satelliteNumbers = new ArrayList<>(0);
        ArrayList<String> satelliteNames = new ArrayList<>(0);
        ArrayList<String> satelliteOwnerCodes = new ArrayList<>(0);
        ArrayList<Long> satelliteLaunchDates = new ArrayList<>(0);
        String[] tleLines;
        JSONObject[] gpData = null;
        Calculations.SatelliteObjectType[] newSatellites = null;

        //if gp data
        if(isGpData)
        {
            //try to get all new satellites
            gpData = Globals.getJsonObjects(inputString);
            newSatellites = Calculations.loadSatellites(gpData);
        }

        //if no satellite number specified
        if(unknownSatellite)
        {
            //if gp data
            if(isGpData)
            {
                //go through each gp
                for(index = 0; index < gpData.length; index++)
                {
                    JSONObject dataItem = gpData[index];
                    Calendar dataDate;

                    if(dataItem == null)
                    {
                        dataItem = new JSONObject();
                    }

                    try
                    {
                        //try to get number
                        satelliteNumbers.add(dataItem.getInt(Calculations.GPParams.SatNum));
                    }
                    catch(Exception ex)
                    {
                        //add as unknown
                        satelliteNumbers.add(Universe.IDs.Invalid);
                    }

                    try
                    {
                        //try to get name
                        satelliteNames.add(dataItem.getString(Calculations.GPParams.Name));
                    }
                    catch(Exception ex)
                    {
                        //add as unknown
                        satelliteNames.add(Globals.getUnknownString(this));
                    }

                    try
                    {
                        //try to get launch date
                        dataDate = (dataItem.has(Calculations.GPParams.LaunchDate) ? Globals.tryParseGMTDate(dataItem.getString(Calculations.GPParams.LaunchDate)) : dataItem.has(Calculations.GPParams.ObjectId) ? Globals.tryParseGMTDate(dataItem.getString(Calculations.GPParams.ObjectId).substring(0, 4) + "-01-01") : null);
                        satelliteLaunchDates.add(dataDate != null ? dataDate.getTimeInMillis() : Globals.INVALID_DATE_MS);
                    }
                    catch(Exception ex)
                    {
                        //add as unknown
                        satelliteLaunchDates.add(Globals.INVALID_DATE_MS);
                    }

                    try
                    {
                        //try to get owner code
                        satelliteOwnerCodes.add(dataItem.getString(Globals.normalizeOwnerCode(Calculations.GPParams.Owner)));
                    }
                    catch(Exception ex)
                    {
                        //add as unknown
                        satelliteOwnerCodes.add(null);
                    }
                }
            }
            else
            {
                //if starts with "0 "
                if(inputString.startsWith("0 "))
                {
                    //remove it
                    inputString = inputString.substring(2);
                    inputLength = inputString.length();
                }

                //while there are more input lines
                inputOffset = 0;
                while(inputOffset < inputLength)
                {
                    //get starting line 1 index
                    line1Index = inputString.indexOf("1 ", inputOffset);
                    if(line1Index >= 0 && line1Index + TLE_LINE_LENGTH < inputLength)
                    {
                        //if get starting line 2 index
                        line2Index = inputString.indexOf("2 ", line1Index + TLE_LINE_LENGTH);
                        if(line2Index > line1Index && line2Index + TLE_LINE_LENGTH <= inputLength)
                        {
                            //get current number string
                            currentNumString = inputString.substring(line1Index + 2, line1Index + 7).trim();

                            //if line 1 and 2 indexes are followed by the same number
                            if(inputString.substring(line1Index + 2, line1Index + 2 + currentNumString.length()).equals(inputString.substring(line2Index + 2, line2Index + 2 + currentNumString.length())))
                            {
                                //if satellite number is valid
                                currentNum = Globals.tryParseInt(currentNumString);
                                if(currentNum < Integer.MAX_VALUE)
                                {
                                    //add satellite number to list
                                    satelliteNumbers.add(currentNum);

                                    //try to get name
                                    satelliteName = Globals.getUnknownString(this);
                                    nameEndIndex = line1Index - 2;
                                    if(nameEndIndex > 0)
                                    {
                                        //keep going back until previous is not a new line
                                        nameStartIndex = nameEndIndex - 1;
                                        while(nameStartIndex > 0 && inputString.charAt(nameStartIndex - 1) != '\r' && inputString.charAt(nameStartIndex - 1) != '\n')
                                        {
                                            nameStartIndex--;
                                        }

                                        //if room for name and it is less than TLE_LINE_LENGTH
                                        if(nameStartIndex >= 0 && nameStartIndex < nameEndIndex && nameEndIndex - nameStartIndex < TLE_LINE_LENGTH)
                                        {
                                            //get name
                                            satelliteName = inputString.substring(nameStartIndex, nameEndIndex).trim();
                                        }
                                    }
                                    satelliteNames.add(satelliteName);

                                    //continue
                                    inputOffset = line2Index + TLE_LINE_LENGTH;
                                }
                                else
                                {
                                    //done
                                    inputOffset = inputString.length();
                                }
                            }
                            else
                            {
                                //line 1 index was not really line 1, so skip
                                inputOffset = line1Index + 2;
                            }
                        }
                        else
                        {
                            //done
                            inputOffset = inputString.length();
                        }
                    }
                    else
                    {
                        //done
                        inputOffset = inputString.length();
                    }
                }
            }
        }
        else
        {
            //add satellite number and name
            satelliteNumbers.add(satelliteNum);
            satelliteNames.add(satelliteName);
        }

        //go through each satellite number
        for(index = 0; index < satelliteNumbers.size(); index++)
        {
            String gpString = null;
            Calculations.SatelliteObjectType currentSatellite = null;

            //remember current satellite number, name, and get data
            satelliteNum = satelliteNumbers.get(index);
            satelliteName = satelliteNames.get(index);

            //if started with unknown satellite
            if(unknownSatellite)
            {
                //reset owner and launch date
                ownerCode = (index < satelliteOwnerCodes.size() ? satelliteOwnerCodes.get(index) : null);
                launchDate = (index < satelliteLaunchDates.size() ? satelliteLaunchDates.get(index) : Globals.INVALID_DATE_MS);
            }

            //if new satellites are set
            if(gpData != null && newSatellites != null && gpData.length > 0 && gpData.length == newSatellites.length)
            {
                //go through each new satellite object while current not found
                for(index2 = 0; index2 < newSatellites.length && currentSatellite == null; index2++)
                {
                    //remember current satellite object
                    Calculations.SatelliteObjectType currentSatObj = newSatellites[index2];

                    //if satellite is a match
                    if(currentSatObj != null && currentSatObj.tle.satelliteNum == satelliteNum)
                    {
                        //get current gp and satellite
                        gpString = gpData[index2].toString();
                        currentSatellite = currentSatObj;
                    }
                }

                //set blank TLE lines
                tleLines = new String[]{"", ""};
            }
            else
            {
                //if data was read for current satellite number
                tleLines = parseTLEData(inputString, satelliteNum);
                if(!tleLines[0].equals("") && !tleLines[1].equals(""))
                {
                    //get current satellite
                    currentSatellite = Calculations.loadSatellite(tleLines[0], tleLines[1]);
                }
            }

            //if satellite is set
            if(currentSatellite != null)
            {
                //if name or owner code is unknown
                if(satelliteName.equals(Globals.getUnknownString(this)) || ownerCode == null || launchDate == Globals.INVALID_DATE_MS)
                {
                    //try to get existing satellite
                    Database.DatabaseSatellite existingSatellite = Database.getOrbital(this, satelliteNum);
                    if(existingSatellite != null)
                    {
                        //get existing name owner code
                        satelliteName = existingSatellite.getName();
                        ownerCode = existingSatellite.ownerCode;
                        launchDate = existingSatellite.launchDateMs;
                    }

                    //if launch date is unknown
                    if(launchDate == Globals.INVALID_DATE_MS)
                    {
                        //default to today with known launch year
                        defaultLaunchDate.set(Calendar.YEAR, currentSatellite.tle.launchYear);
                    }
                }

                //if owner and launch date is known
                if(ownerCode != null && launchDate != Globals.INVALID_DATE_MS)
                {
                    //if able to save satellite
                    if(Database.saveSatellite(this, satelliteName, satelliteNum, ownerCode, launchDate, tleLines[0], tleLines[1], Calculations.epochToGMTCalendar(currentSatellite.tle.epochYear, currentSatellite.tle.epochDay).getTimeInMillis(), gpString, Globals.getGMTTime().getTimeInMillis(), orbitalType) >= 0)
                    {
                        //update save count
                        saveCount++;
                    }
                }
                //else if can have pending satellites
                else if(pendingSaveSatellites != null)
                {
                    //add to pending satellites
                    pendingSaveSatellites.add(new Database.DatabaseSatellite(satelliteName, null, satelliteNum, null, null, defaultLaunchDate.getTimeInMillis(), tleLines[0], tleLines[1], Globals.UNKNOWN_DATE_MS, gpString, Globals.UNKNOWN_DATE_MS, Color.DKGRAY, orbitalType, true));
                }
            }
        }

        //return save count
        return(saveCount);
    }

    //Updates satellites
    @SuppressWarnings("SpellCheckingInspection")
    private void updateSatellites(int updateSource, String section, String user, String pwd, ArrayList<Database.DatabaseSatellite> satelliteList)
    {
        int index;
        int index2;
        int index3;
        int startIndex;
        int endIndex;
        int count;
        int currentNumber;
        boolean saved;
        boolean isLast;
        boolean loginFailed = false;
        boolean downloadError = false;
        boolean usingGP = Settings.getSatelliteSourceUseGP(this, updateSource);
        String receivedPage;
        String receivedPageLower;
        Database.DatabaseSatellite[] satellites = satelliteList.toArray(new Database.DatabaseSatellite[0]);
        Globals.WebPageData loginData = null;
        Globals.WebPageData tleData;
        StringBuilder url = new StringBuilder();

        //if updating from space track
        if(updateSource == Database.UpdateSource.SpaceTrack)
        {
            //login to space track
            loginData = Globals.loginSpaceTrack(user, pwd);
            loginFailed = loginData.isLoginError();
        }

        //go through each satellite while not cancelled
        count = satellites.length;
        for(index = 0; index < count && !loginFailed && !downloadError && !cancelIntent[UpdateType.UpdateSatellites]; index++)
        {
            //reset
            receivedPage = "";
            index2 = index;

            //update progress
            sendMessage(MessageTypes.Download, UpdateType.UpdateSatellites, section, index, count, Globals.ProgressType.Started);

            //if updating from space track
            if(updateSource == Database.UpdateSource.SpaceTrack)
            {
                //if logged in
                if(loginData.connection != null)
                {
                    //reset url
                    url.setLength(0);

                    //build url, adding the next 20 norad IDs
                    url.append("https://www.space-track.org/basicspacedata/query/class/");
                    if(usingGP)
                    {
                        url.append("gp");
                    }
                    else
                    {
                        url.append("tle_latest/ORDINAL/1");
                    }
                    url.append("/NORAD_CAT_ID/");
                    for(index2 = index; index2 < count && index2 < index + 20; index2++)
                    {
                        //if after first
                        if(index2 > index)
                        {
                            //add separator
                            url.append(",");
                        }

                        //add current norad ID
                        url.append(satellites[index2].noradId);
                    }
                    if(index2 > index)
                    {
                        //go back to last used
                        index2--;
                    }
                    url.append("/orderby/");
                    if(usingGP)
                    {
                        url.append("NORAD_CAT_ID%20asc");
                    }
                    else
                    {
                        url.append("TLE_LINE1%20ASC/format/tle");
                    }

                    //try to get html page
                    tleData = Globals.getWebPage(url.toString());
                    downloadError = !tleData.isOkay();
                    loginFailed = tleData.isDenied();
                    receivedPage = tleData.pageData;
                }
            }
            else
            {
                //remember current number and if last
                currentNumber = satellites[index].noradId;
                isLast = ((index + 1) >= count);

                switch(updateSource)
                {
                    case Database.UpdateSource.Celestrak:
                        if(usingGP)
                        {
                            //try to get GP data
                            receivedPage = Globals.getWebPage("https://celestrak.com/NORAD/elements/gp.php?CATNR=" + currentNumber + "&FORMAT=json", isLast, null);
                        }
                        else
                        {
                            //try to get TLE data
                            receivedPage = Globals.getWebPage("https://celestrak.com/satcat/tle.php?CATNR=" + currentNumber, isLast, null);
                        }
                        break;

                    case Database.UpdateSource.N2YO:
                        //try to get html page
                        receivedPage = Globals.getWebPage("https://www.n2yo.com/satellite/?s=" + currentNumber, isLast, null);
                        break;
                }
            }

            //update status
            downloadError = (downloadError || receivedPage == null || receivedPage.equals(""));

            //if page was received
            if(!downloadError)
            {
                //remember lower case received page
                receivedPageLower = receivedPage.toLowerCase();

                //go through each downloaded TLE
                for(index3 = index; index3 <= index2; index3++)
                {
                    //remember current satellite
                    Database.DatabaseSatellite currentSatellite = satellites[index3];

                    //update progress
                    sendMessage(MessageTypes.Download, UpdateType.UpdateSatellites, section, index3, count, Globals.ProgressType.Started);

                    //if updated from UpdateSource.N2YO and owner is missing (not on countries page on website), try to find
                    if(updateSource == Database.UpdateSource.N2YO && (currentSatellite.ownerCode == null || currentSatellite.ownerCode.length() == 0))
                    {
                        //if found source line
                        startIndex = receivedPageLower.indexOf("<b>source</b>: ");
                        if(startIndex >= 0)
                        {
                            //find end of line
                            endIndex = receivedPageLower.indexOf("<br/>", startIndex);
                            if(endIndex >= 1 && endIndex > startIndex)
                            {
                                //find start "("
                                startIndex = receivedPageLower.indexOf("(", startIndex);
                                if(startIndex >= 0)
                                {
                                    //find end ")"
                                    endIndex = receivedPageLower.indexOf(")", startIndex);
                                    if(endIndex >= 1 && endIndex > startIndex + 2)
                                    {
                                        //get owner code
                                        currentSatellite.ownerCode = receivedPage.substring(startIndex + 1, endIndex);
                                    }
                                }
                            }
                        }
                    }

                    //try to save satellite based on input
                    saved = (saveSatellite(receivedPage, currentSatellite.getName(), currentSatellite.noradId, currentSatellite.ownerCode, currentSatellite.launchDateMs, currentSatellite.orbitalType, null) > 0);

                    //update progress
                    sendMessage(MessageTypes.Download, UpdateType.UpdateSatellites, section, index3, count, (saved ? Globals.ProgressType.Success : Globals.ProgressType.Failed));
                }

                //prevent overloading website
                try
                {
                    //noinspection BusyWait
                    Thread.sleep(50);
                }
                catch (InterruptedException ex)
                {
                    //do nothing
                }
            }
            else
            {
                //update progress
                sendMessage(MessageTypes.Download, UpdateType.UpdateSatellites, section, index, count, Globals.ProgressType.Failed);
            }

            index = index2;
        }

        //if updated from space track
        if(updateSource == Database.UpdateSource.SpaceTrack && loginData != null && loginData.connection != null)
        {
            //logout
            Globals.logoutSpaceTrack(null);
        }

        //update progress
        sendMessage(MessageTypes.General, UpdateType.UpdateSatellites, section, index, count, (loginFailed ? Globals.ProgressType.Denied : downloadError ? Globals.ProgressType.Cancelled : Globals.ProgressType.Finished));
    }

    //Loads a database backup file and returns saved satellite count
    private int loadBackup(String inputString) throws Exception
    {
        int tableIndex;
        int rowIndex;
        int saveCount = 0;
        JSONObject rootNode;
        String[] tables = new String[]{Database.Tables.SatelliteCategory, Database.Tables.Owner, Database.Tables.Orbital, Database.Tables.Information};

        //get root and master list
        rootNode = new JSONObject(inputString);
        loadMasterList();

        //go through each table
        for(tableIndex = 0; tableIndex < tables.length; tableIndex++)
        {
            //get current table data
            String gp;
            String currentTable = tables[tableIndex];
            JSONObject currentTableNode = (rootNode.has(currentTable) || !currentTable.equals(Database.Tables.Information) ? rootNode.getJSONObject(currentTable) : null);
            JSONArray rows = (currentTableNode != null ? currentTableNode.getJSONArray("Rows") : new JSONArray());

            //go through each row
            for(rowIndex = 0; rowIndex < rows.length(); rowIndex++)
            {
                //get row node
                JSONObject rowNode = rows.getJSONObject(rowIndex);

                //handle based on table
                switch(currentTable)
                {
                    case Database.Tables.SatelliteCategory:
                        masterList.addSatelliteCategory(rowNode.getInt("Norad"), masterList.addCategory(rowNode.getString("Name")));
                        break;

                    case Database.Tables.Owner:
                        masterList.addOwner(new UpdateService.MasterOwner(rowNode.getString("Code"), rowNode.getString("Name")));
                        break;

                    case Database.Tables.Orbital:
                        try
                        {
                            gp = rowNode.getString("GP");
                        }
                        catch(Exception ex)
                        {
                            gp = null;
                        }
                        if(Database.saveSatellite(this, rowNode.getString("Name"), (rowNode.has("User_Name") ? rowNode.getString("User_Name") : null), rowNode.getInt("Norad"), rowNode.getString("Owner_Code"), rowNode.getLong("Launch_Date"), rowNode.getString("TLE_Line1"), rowNode.getString("TLE_Line2"), rowNode.getLong("TLE_Date"), gp, rowNode.getLong("Update_Date"), rowNode.getInt("Path_Color"), (byte)rowNode.getInt("Type"), (rowNode.getInt("Selected") >= 1)) >= 0)
                        {
                            saveCount++;
                        }
                        break;

                    case Database.Tables.Information:
                        Database.saveInformation(this, rowNode.getInt("Norad"), rowNode.getInt("Source"), rowNode.getString("Language"), rowNode.getString("Info"));
                        break;
                }
            }
        }

        //save
        Database.saveSatelliteCategories(this, masterList.newSatelliteCategories, null);
        Database.saveOwners(this, masterList.newOwners, null);

        //return saved count
        return(saveCount);
    }

    //Loads satellite(s) from the given file
    private void loadSatellite(ArrayList<String> fileNames, ArrayList<String> filesData)
    {
        boolean error = false;
        boolean useFileData = (filesData != null);
        int index;
        int fileCount = (useFileData ? filesData.size() : (fileNames != null ? fileNames.size() : 0));
        int updatedSatellites = 0;
        FileInputStream fileStream;
        Resources res = this.getResources();
        String readString;
        String section = res.getString(R.string.title_file);
        ArrayList<Database.DatabaseSatellite> pendingSaveSatellites = new ArrayList<>(0);

        //go through each file
        for(index = 0; index < fileCount && !error && !cancelIntent[UpdateType.LoadFile]; index++)
        {
            //update status
            sendMessage(MessageTypes.Load, UpdateType.LoadFile, section, index, fileCount, Globals.ProgressType.Started);

            try
            {
                //read file
                if(useFileData)
                {
                    //use file data
                    readString = filesData.get(index);
                }
                else
                {
                    //read file data
                    fileStream = new FileInputStream(fileNames.get(index));
                    readString = Globals.readTextFile(this, fileStream);
                    fileStream.close();
                }

                //try to save all satellites
                if(readString.contains(Database.Tables.SatelliteCategory) && readString.contains(Database.Tables.Owner) && readString.contains(Database.Tables.Orbital))
                {
                    //load backup file
                    updatedSatellites += loadBackup(readString);
                }
                else
                {
                    //load TLE
                    updatedSatellites += saveSatellite(readString, Globals.getUnknownString(this), -1, null, Globals.INVALID_DATE_MS, Database.OrbitalType.Satellite, pendingSaveSatellites);
                }
            }
            catch(Exception ex)
            {
                //set error
                currentError[UpdateType.LoadFile] = ex.getMessage();
                error = true;
            }

            //update status
            sendMessage(MessageTypes.Load, UpdateType.LoadFile, section, index, fileCount, Globals.ProgressType.Success, pendingSaveSatellites);
        }

        //update progress
        sendMessage(MessageTypes.General, UpdateType.LoadFile, section, updatedSatellites, updatedSatellites, (cancelIntent[UpdateType.LoadFile] ? Globals.ProgressType.Cancelled : (error || updatedSatellites == 0) ? Globals.ProgressType.Failed : Globals.ProgressType.Finished), pendingSaveSatellites);
    }

    //Return if running given update type
    public static boolean isRunning(byte updateType)
    {
        //if a valid update type
        if(updateType < currentIntent.length)
        {
            //running if intent is set
            return(currentIntent[updateType] != null);
        }

        //invalid
        return(false);
    }

    //Return any error for update type
    public static String getError(byte updateType)
    {
        //if a valid update type
        if(updateType < currentError.length)
        {
            //return error
            return(currentError[updateType]);
        }

        //invalid
        return(null);
    }

    //Return if running satellite update
    public static boolean updatingSatellites()
    {
        return(isRunning(UpdateType.UpdateSatellites));
    }

    //Return if running master list update
    public static boolean updatingMasterList()
    {
        return(isRunning(UpdateType.GetMasterList));
    }

    //Return if running save file
    public static boolean savingFile()
    {
        return(isRunning(UpdateType.SaveFile));
    }

    //Returns if modifying any satellites
    public static boolean modifyingSatellites()
    {
        return(isRunning(UpdateType.GetMasterList) || isRunning(UpdateType.UpdateSatellites) || isRunning(UpdateType.LoadFile));
    }

    //Returns if building database
    public static boolean buildingDatabase()
    {
        return(isRunning(UpdateType.BuildDatabase));
    }

    //Get master list
    public static MasterListType getMasterList()
    {
        return(masterList);
    }

    private void loadCategories()
    {
        int index;
        int index2;
        int satelliteCount = masterList.satellites.size();
        Resources res = this.getResources();
        String section = res.getString(R.string.title_categories);

        //update status
        sendMessage(MessageTypes.Load, UpdateType.GetMasterList, section, Globals.ProgressType.Started);

        //go through each satellite
        for(index = 0; index < satelliteCount; index++)
        {
            //possibly add any missing existing categories
            MasterSatellite currentSatellite = masterList.satellites.get(index);
            String[][] currentCategories = Database.getSatelliteCategoriesEnglish(this, currentSatellite.noradId, true);
            for(index2 = 0; index2 < currentCategories.length; index2++)
            {
                //add category if missing
                currentSatellite.addCategory(currentCategories[index2][1], Integer.parseInt(currentCategories[index2][2]));
            }

            //update status (75 - 85%)
            sendMessage(MessageTypes.Load, UpdateType.GetMasterList, section, index + 1, satelliteCount, 75 + (int)(((index +1) / (float)satelliteCount) * 10), Globals.ProgressType.Success);
        }

        //update status
        sendMessage(MessageTypes.Load, UpdateType.GetMasterList, section, Globals.ProgressType.Finished);
    }


    //Loads master list from the database
    private void loadMasterList()
    {
        masterList = new MasterListType();
        masterList.satellites = Database.getMasterSatellites(this);
        masterList.owners = Database.getOwners(this);
        masterList.categories = Database.getCategories(this);
        masterList.categoriesByIndex.addAll(masterList.categories);
        Collections.sort(masterList.categoriesByIndex, new MasterCategory.Comparer(false, true));
        masterList.satelliteCategories = Database.getSatelliteCategoriesEnglish(this);
    }

    //Saves master list to the database
    private void saveMasterList(int updateSource)
    {
        int index;
        int satelliteCount;
        int fileCount;
        Calendar timeNow;
        Resources res = this.getResources();
        SharedPreferences.Editor writeSettings;
        Globals.OnProgressChangedListener saveListener;

        //if list exists
        if(masterList != null)
        {
            //get counts
            satelliteCount = masterList.satellites.size();
            fileCount = masterList.categories.size();

            //if satellites and files exist
            if(satelliteCount > 0 && fileCount > 0)
            {
                //get writeable settings
                writeSettings = getPreferences(this).edit();

                //clear master satellites
                Database.clearMasterSatelliteTable(this);

                //go through each list to save
                for(index = 0; index < 4; index++)
                {
                    String section = null;

                    //get section and create listener (85 - 90%)
                    switch(index)
                    {
                        case 0:
                            section = res.getString(R.string.title_owners);
                            break;

                        case 1:
                            section = res.getString(R.string.title_categories);
                            break;

                        case 2:
                            section = res.getString(R.string.title_satellites);
                            break;

                        case 3:
                            section = res.getString(R.string.abbrev_satellite) + " " + res.getString(R.string.title_categories);
                            break;
                    }
                    saveListener = createMasterListProgressListener(MessageTypes.Save, section, 85 + (int)(((index + 1) / 4f) * 5));

                    //update status
                    sendMessage(MessageTypes.Save, UpdateType.GetMasterList, section, Globals.ProgressType.Started);

                    //save owners, categories, and satellites
                    switch(index)
                    {
                        case 0:
                            Database.saveOwners(this, masterList.newOwners, saveListener);
                            break;

                        case 1:
                            Database.saveCategories(this, masterList.newCategories, saveListener);
                            break;

                        case 2:
                            Database.saveMasterSatellites(this, masterList.satellites, saveListener);
                            break;

                        case 3:
                            Database.saveSatelliteCategories(this, masterList.newSatelliteCategories, saveListener);
                            break;
                    }

                    //update status
                    sendMessage(MessageTypes.Save, UpdateType.GetMasterList, section, Globals.ProgressType.Finished);
                }

                //set update time
                timeNow = Calendar.getInstance();
                writeSettings.putInt(PreferenceName.MasterSource, updateSource).apply();
                setMasterListTime(this, timeNow.getTimeZone().getID(), timeNow.getTimeInMillis());
            }
        }
    }

    //Saves satellites file
    public static int saveSatellitesFile(UpdateService service, Context context, Database.DatabaseSatellite[] satellites, int fileType, OutputStream outStream) throws Exception
    {
        int rowIndex;
        int subRowIndex;
        int colIndex;
        int tableIndex;
        int savedSatellites = 0;
        int count = satellites.length;
        String line;
        String section;
        Resources res = context.getResources();
        JSONObject rootNode = new JSONObject();
        JSONObject[] tableNodes;
        String[] tables;
        String[] rowData;
        String[] columns;
        String[][] dataRows;

        //set needed data
        switch(fileType)
        {
            case Globals.FileType.Backup:
                tables = new String[]{Database.Tables.SatelliteCategory, Database.Tables.Owner, Database.Tables.Orbital, Database.Tables.Information};
                tableNodes = new JSONObject[tables.length];
                break;

            default:
            case Globals.FileType.TLEs:
                tables = new String[]{Database.Tables.Orbital};
                tableNodes = null;
                break;
        }

        //go through each table
        for(tableIndex = 0; tableIndex < tables.length; tableIndex++)
        {
            //remember current table
            String currentTable = tables[tableIndex];
            JSONArray rowNodes = new JSONArray();

            //go through each row
            for(rowIndex = 0; rowIndex < count; rowIndex++)
            {
                //remember current satellite
                Database.DatabaseSatellite currentSatellite = satellites[rowIndex];

                //update progress
                section = (rowIndex + 1) + res.getString(R.string.text_space_of_space) + count;
                if(service != null)
                {
                    service.sendMessage(MessageTypes.Save, UpdateType.SaveFile, section, rowIndex, count, Globals.ProgressType.Started);
                }

                //handle based on file type
                switch(fileType)
                {
                    case Globals.FileType.TLEs:
                        //get TLE lines
                        rowData = currentSatellite.getTLELines();

                        //if(dataRows.length > 0)
                        if(rowData != null)
                        {
                            //save name, TLE line 1, and TLE line 2
                            line = currentSatellite.getName() + "\r\n" + rowData[0] + "\r\n" + rowData[1] + "\r\n";
                            //noinspection CharsetObjectCanBeUsed
                            outStream.write(line.getBytes(Globals.Encoding.UTF16));
                        }
                        break;

                    case Globals.FileType.Backup:
                        //if on the first row
                        if(rowIndex == 0)
                        {
                            //create table node
                            tableNodes[tableIndex] = new JSONObject();
                        }

                        //get row data
                        switch(currentTable)
                        {
                            case Database.Tables.SatelliteCategory:
                                dataRows = Database.getSatelliteCategoriesEnglish(context, currentSatellite.noradId);
                                columns = new String[]{"Norad", "Name"};
                                break;

                            case Database.Tables.Owner:
                                dataRows = Database.getOwnersEnglish(context, currentSatellite.noradId);
                                columns = new String[]{"Code", "Name"};
                                break;

                            case Database.Tables.Information:
                                dataRows = Database.getInformation(context, currentSatellite.noradId);
                                columns = new String[]{"Norad", "Info", "Language", "Source"};
                                break;

                            default:
                            case Database.Tables.Orbital:
                                dataRows = Database.getSatelliteData(context, currentSatellite.noradId);
                                columns = new String[]{"Name", "User_Name", "Norad", "Owner_Code", "Launch_Date", "TLE_Line1", "TLE_Line2", "TLE_Date", "GP", "Update_Date", "Path_Color", "Type", "Selected"};
                                break;
                        }

                        //go through each sub row
                        for(subRowIndex = 0; subRowIndex < dataRows.length; subRowIndex++)
                        {
                            //get row
                            JSONObject rowNode = new JSONObject();
                            rowData = dataRows[subRowIndex];

                            //go through each column
                            for(colIndex = 0; colIndex < rowData.length; colIndex++)
                            {
                                //add column
                                rowNode.put(columns[colIndex], rowData[colIndex]);
                            }

                            //add row
                            rowNodes.put(rowNode);
                        }
                        break;
                }

                //update progress
                if(service != null)
                {
                    service.sendMessage(MessageTypes.Save, UpdateType.SaveFile, section, rowIndex, count, Globals.ProgressType.Success);
                }
                if(currentTable.equals(Database.Tables.Orbital))
                {
                    savedSatellites++;
                }
            }

            //if table nodes exist and current is set
            if(tableNodes != null && tableNodes[tableIndex] != null)
            {
                //add row to table node
                tableNodes[tableIndex].put("Rows", rowNodes);
            }
        }

        //if doing a backup
        if(fileType == Globals.FileType.Backup)
        {
            //go through each table
            for(tableIndex = 0; tableIndex < tableNodes.length; tableIndex++)
            {
                //add table to root node
                rootNode.put(tables[tableIndex], tableNodes[tableIndex]);
            }

            //write root node to file
            //noinspection CharsetObjectCanBeUsed
            outStream.write(rootNode.toString().getBytes(Globals.Encoding.UTF16));
        }

        //return saved satellite count
        return(savedSatellites);
    }
    private void saveSatellitesFile(Database.DatabaseSatellite[] satellites, Uri fileUri, String fileName, String fileExtension, int fileType, int fileSourceType)
    {
        int count = satellites.length;
        int savedSatellites = 0;
        boolean error = false;
        boolean isOthersSource = (fileSourceType == Globals.FileSource.Others);
        boolean isDropboxSource = (fileSourceType == Globals.FileSource.Dropbox);
        boolean isGoogleDriveSource = (fileSourceType == Globals.FileSource.GoogleDrive);
        String source = "";
        File saveFile = null;
        OutputStream outStream;
        Resources res = this.getResources();

        //if file does not end with extension
        if(!fileName.endsWith(fileExtension))
        {
            //add extension
            fileName += fileExtension;
        }

        //handle based on file source
        switch(fileSourceType)
        {
            case Globals.FileSource.SDCard:
            case Globals.FileSource.GoogleDrive:
            case Globals.FileSource.Dropbox:
            case Globals.FileSource.Others:
                //try to create file
                try
                {
                    //if for others
                    if(isOthersSource)
                    {
                        //set source and create output stream
                        source = res.getString(R.string.title_other);
                        outStream = this.getContentResolver().openOutputStream(Globals.createFileUri(this, fileUri, fileName, fileExtension));
                    }
                    else
                    {
                        //if for Dropbox or GoogleDrive
                        if(isDropboxSource || isGoogleDriveSource)
                        {
                            //set save file and source
                            saveFile = new File(this.getCacheDir() + "/" + fileName);
                            source = (isDropboxSource ? Globals.FileLocationType.Dropbox : Globals.FileLocationType.GoogleDrive);
                        }
                        else
                        {
                            //set save file and source
                            saveFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
                            source = res.getString(R.string.title_downloads);
                        }

                        //create output stream
                        outStream = new FileOutputStream(saveFile);
                    }

                    //save satellites
                    savedSatellites = saveSatellitesFile(this, this, satellites, fileType, outStream);

                    //close stream
                    outStream.close();
                }
                catch(Exception ex)
                {
                    //set error
                    currentError[UpdateType.SaveFile] = ex.getMessage();
                    error = true;
                }

                //return status
                sendMessage(MessageTypes.General, UpdateType.SaveFile, source, savedSatellites, count, cancelIntent[UpdateType.SaveFile] ? Globals.ProgressType.Cancelled : (error || savedSatellites == 0) ? Globals.ProgressType.Failed : Globals.ProgressType.Finished, saveFile);
                break;
        }
    }

    //Builds database
    private void buildDatabase()
    {
        Database.build(this, new Globals.OnProgressChangedListener()
        {
            @Override
            public void onProgressChanged(int progressType, String section, long updateIndex, long updateCount)
            {
                switch(progressType)
                {
                    case Globals.ProgressType.Started:
                    case Globals.ProgressType.Finished:
                        UpdateService.this.sendMessage(MessageTypes.General, UpdateType.BuildDatabase, section, progressType);
                        break;

                    case Globals.ProgressType.Running:
                        UpdateService.this.sendMessage(MessageTypes.General, UpdateType.BuildDatabase, section, updateIndex, updateCount, progressType);
                        break;
                }
            }
        });
    }

    //Creates a base intent
    private static Intent createIntent(Context context, byte updateType, String notifyTitle)
    {
        Intent baseIntent = new Intent(context, UpdateService.class);

        baseIntent.putExtra(ParamTypes.UpdateType, updateType);
        baseIntent.putExtra(ParamTypes.NotifyTitle, notifyTitle);

        return(baseIntent);
    }

    //Returns account type for given update source
    private static int getAccountType(int updateSource)
    {
        return(updateSource == Database.UpdateSource.SpaceTrack ? Globals.AccountType.SpaceTrack : Globals.AccountType.None);
    }

    //Update master list
    public static void updateMasterList(Context context, int updateSource, boolean getUpdate, boolean checkUpdate, boolean runForeground)
    {
        Resources res = context.getResources();
        Intent updateIntent = createIntent(context, UpdateType.GetMasterList,res.getString(R.string.title_list_getting));
        String[] loginData = Settings.getLogin(context, getAccountType(updateSource));

        updateIntent.putExtra(ParamTypes.UpdateSource, updateSource);
        updateIntent.putExtra(ParamTypes.GetUpdate, getUpdate);
        updateIntent.putExtra(ParamTypes.CheckUpdate, checkUpdate);
        updateIntent.putExtra(ParamTypes.RunForeground, runForeground);
        updateIntent.putExtra(ParamTypes.User, loginData[0]);
        updateIntent.putExtra(ParamTypes.Password, loginData[1]);

        Globals.startService(context, updateIntent, runForeground);
    }

    //Update given satellites
    public static void updateSatellites(Context context, String notifyTitle, ArrayList<Database.DatabaseSatellite> satellites, boolean oldSatellites, boolean runForeground)
    {
        int updateSource = Settings.getSatelliteSource(context);
        Intent updateIntent = createIntent(context, UpdateType.UpdateSatellites, notifyTitle);
        String[] loginData = Settings.getLogin(context, getAccountType(updateSource));

        updateIntent.putExtra(ParamTypes.UpdateSource, updateSource);
        updateIntent.putParcelableArrayListExtra(ParamTypes.Satellites, satellites);
        updateIntent.putExtra(ParamTypes.RunForeground, runForeground);
        updateIntent.putExtra(ParamTypes.User, loginData[0]);
        updateIntent.putExtra(ParamTypes.Password, loginData[1]);
        if(oldSatellites)
        {
            updateIntent.putExtra(ParamTypes.Section, "Old");
        }

        Globals.startService(context, updateIntent, runForeground);
    }
    public static void updateSatellites(Context context, String notifyTitle, ArrayList<Database.DatabaseSatellite> satellites, boolean runForeground)
    {
        updateSatellites(context, notifyTitle, satellites, false, runForeground);
    }

    //Loads a file
    public static void loadFile(Activity activity, ArrayList<String> fileNames)
    {
        boolean haveFileNames = (fileNames != null);
        Resources res = activity.getResources();
        Intent loadIntent = createIntent(activity, UpdateType.LoadFile, res.getQuantityString(R.plurals.title_files_loading, (haveFileNames ? fileNames.size() : 0)));

        if(haveFileNames)
        {
            loadIntent.putExtra(ParamTypes.FileNames, fileNames);
        }

        Globals.startService(activity, loadIntent, false);
    }

    //Loads file data
    public static void loadFileData(Activity activity, ArrayList<String> filesData)
    {
        Resources res = activity.getResources();
        Intent loadIntent = createIntent(activity, UpdateType.LoadFile, res.getString(R.string.title_file_loading));

        loadIntent.putExtra(ParamTypes.FilesData, filesData);

        Globals.startService(activity, loadIntent, false);
    }

    //Saves satellites to a file
    public static void saveFile(Activity activity, ArrayList<Database.DatabaseSatellite> satellites, Globals.PendingFile pendingFile)
    {
        Resources res = activity.getResources();
        Intent saveIntent = createIntent(activity, UpdateType.SaveFile, res.getString(R.string.title_saving));

        saveIntent.putParcelableArrayListExtra(ParamTypes.Satellites, satellites);
        saveIntent.putExtra(ParamTypes.FileName, pendingFile.name);
        saveIntent.putExtra(ParamTypes.FileExt, pendingFile.extension);
        saveIntent.putExtra(ParamTypes.FileType, pendingFile.type);
        saveIntent.putExtra(ParamTypes.FileSourceType, pendingFile.fileSourceType);
        saveIntent.putExtra(ParamTypes.FileUri, pendingFile.outUri);

        Globals.startService(activity, saveIntent, false);
    }

    //Builds database
    public static void buildDatabase(Activity activity)
    {
        Resources res = activity.getResources();
        Intent buildIntent = createIntent(activity, UpdateType.BuildDatabase, res.getString(R.string.title_database_building));
        Globals.startService(activity, buildIntent, false);
    }

    //Gets orbital information
    public static void getInformation(Activity activity, int group, Calculations.TLEDataType tleData)
    {
        int updateSource = Settings.getInformationSource(activity);
        Resources res = activity.getResources();
        Intent infoIntent = createIntent(activity, UpdateType.GetInformation, res.getString(R.string.title_information_getting));

        infoIntent.putExtra(ParamTypes.Index, group);
        infoIntent.putExtra(ParamTypes.UpdateSource, updateSource);
        infoIntent.putExtra(ParamTypes.TLEData, tleData);

        Globals.startService(activity, infoIntent, false);
    }
}