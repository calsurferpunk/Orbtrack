package com.nikolaiapps.orbtrack;


import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.TimeZone;


public class Database extends SQLiteOpenHelper
{
    private static abstract class ParamTypes
    {
        static final String Name = "name";
        static final String UserName = "userName";
        static final String Norad = "norad";
        static final String OwnerCode = "ownerCode";
        static final String OwnerName = "ownerName";
        static final String LaunchDate = "launchDate";
        static final String TLELine1 = "tleLine1";
        static final String TLELine2 = "tleLine2";
        static final String TLEDate = "tleDate";
        static final String GP = "gp";
        static final String UpdateDate = "updateDate";
        static final String PathColor = "pathColor";
        static final String OrbitalType = "orbitalType";
        static final String IsSelected = "isSelected";
    }

    static abstract class Tables
    {
        static final String Orbital = "[Orbital]";
        static final String Location = "[Location]";
        static final String LocationName = "[LocationName]";
        static final String TimeZone = "[TimeZone]";
        static final String Altitude = "[Altitude]";
        static final String MasterSatellite = "[MasterSatellite]";
		static final String Owner = "[Owner]";
        static final String Category = "[Category]";
        static final String SatelliteCategory = "[SatelliteCategory]";
        static final String Information = "[Information]";
    }

    public static abstract class OrbitalType
    {
        static final byte Star = 1;
        static final byte Planet = 2;
        static final byte Satellite = 3;
        static final byte RocketBody = 4;
        static final byte Debris = 5;
    }

    static abstract class UpdateSource
    {
        static final byte Celestrak = 0;
        static final byte N2YO = 1;
        static final byte SpaceTrack = 2;
        static final byte NASA = 3;
        static final byte SpaceDotCom = 127;
    }

    private static abstract class SQLBindType
    {
        static final byte String = 0;
        static final byte Double = 1;
    }

    private static abstract class LanguageIndex
    {
        static final byte English = 0;
        static final byte Spanish = 1;
        static final byte LanguageCount = 2;
    }

    @SuppressWarnings("SpellCheckingInspection")
    private static abstract class TLELines
    {
        static final String ISSZarya1 = "1 25544U 98067A   21188.62787601  .00000229  00000-0  12373-4 0  9990";
        static final String ISSZarya2 = "2 25544  51.6436 241.6415 0001986 141.2144 354.6717 15.48768433291768";
        static final long ISSZaryaDate = 1625689224996L;
    }

    private static final int ISS_ZARYA_NORAD_ID = 25544;

    private static final int INFO_FILE_COLUMNS = 2;
    private static final int INFO_FILE_ROWS = 11;
    private static final String INFO_FILE_SEPARATOR = "[|]";

    private static abstract class LocaleInformation
    {
        private static final ArrayList<Integer> noradId = new ArrayList<>(0);
        private static final ArrayList<ArrayList<String>> languageInfo = new ArrayList<>(LanguageIndex.LanguageCount);

        //Initializes data
        public static void initData(Context context)
        {
            int fileId;
            byte languageIndex;
            String line;
            BufferedReader file;
            Resources res = context.getResources();

            //clear any existing
            noradId.clear();
            languageInfo.clear();

            //go through each information file
            for(languageIndex = 0; languageIndex < LanguageIndex.LanguageCount; languageIndex++)
            {
                ArrayList<String> infoList = new ArrayList<>(0);

                //open information file
                switch(languageIndex)
                {
                    case LanguageIndex.Spanish:
                        fileId = R.raw.information_es;
                        break;

                    default:
                    case LanguageIndex.English:
                        fileId = R.raw.information_en;
                        break;
                }
                file = new BufferedReader(new InputStreamReader(res.openRawResource(fileId)));
                try
                {
                    //while there are lines to read
                    while((line = file.readLine()) != null)
                    {
                        //split columns
                        String[] columns = line.split(INFO_FILE_SEPARATOR);

                        //if valid number of columns
                        if(columns.length == INFO_FILE_COLUMNS)
                        {
                            //add current ID and information
                            if(languageIndex == 0)
                            {
                                noradId.add(Integer.valueOf(columns[0]));
                            }
                            infoList.add(columns[1]);
                        }
                    }
                    languageInfo.add(infoList);

                    //close file
                    file.close();
                }
                catch(IOException ex)
                {
                    //do nothing
                }
            }
        }

        //Gets the information for the given locale
        public static String getInformation(Context context, int id)
        {
            int index;
            byte languageIndex;
            String info = null;
            String language;

            //if ID is in list
            index = noradId.indexOf(id);
            if(index >= 0)
            {
                //get language
                language = Globals.getLanguage(context);

                //if Spanish
                if(language.equals(Globals.Languages.Spanish))
                {
                    languageIndex = LanguageIndex.Spanish;
                }
                //else default to English
                else
                {
                    languageIndex = LanguageIndex.English;
                }

                //get info for ID and language
                info = languageInfo.get(languageIndex).get(index);
            }

            //return information
            return(info);
        }

        //Returns if have data
        public static boolean haveData()
        {
            return(noradId.size() > 0);
        }
    }

    private static final int OWNERS_FILE_COLUMNS = 2;
    private static final int OWNERS_FILE_ROWS = 106;
    private static final String OWNERS_FILE_SEPARATOR = "[|]";

    public static abstract class LocaleOwner
    {
        private static final ArrayList<String> codeName = new ArrayList<>(0);
        private static final ArrayList<ArrayList<String>> languageName = new ArrayList<>(LanguageIndex.LanguageCount);

        //Initializes data
        public static void initData(Context context)
        {
            int fileId;
            byte languageIndex;
            String line;
            BufferedReader file;
            Resources res = context.getResources();

            //clear any existing
            codeName.clear();
            languageName.clear();

            //go through each owner file
            for(languageIndex = 0; languageIndex < LanguageIndex.LanguageCount; languageIndex++)
            {
                ArrayList<String> nameList = new ArrayList<>(0);

                //open information file
                switch(languageIndex)
                {
                    case LanguageIndex.Spanish:
                        fileId = R.raw.owners_es;
                        break;

                    default:
                    case LanguageIndex.English:
                        fileId = R.raw.owners_en;
                        break;
                }
                file = new BufferedReader(new InputStreamReader(res.openRawResource(fileId)));
                try
                {
                    //while there are lines to read
                    while((line = file.readLine()) != null)
                    {
                        //split columns
                        String[] columns = line.split(OWNERS_FILE_SEPARATOR);

                        //if valid number of columns
                        if(columns.length == OWNERS_FILE_COLUMNS)
                        {
                            //add current code and name
                            if(languageIndex == 0)
                            {
                                codeName.add(columns[0].toUpperCase());
                            }
                            nameList.add(columns[1].toUpperCase());
                        }
                    }
                    languageName.add(nameList);

                    //close file
                    file.close();
                }
                catch(IOException ex)
                {
                    //do nothing
                }
            }
        }

        //Gets the name for the given locale
        public static String getName(Context context, String code)
        {
            int index;
            byte languageIndex;
            String name = null;
            String language;

            //if no code set
            if(code == null)
            {
                //stop
                return(null);
            }

            //if code is in list
            index = codeName.indexOf(code.toUpperCase());
            if(index >= 0)
            {
                //get language
                language = Globals.getLanguage(context);

                //if Spanish
                if(language.equals(Globals.Languages.Spanish))
                {
                    languageIndex = LanguageIndex.Spanish;
                }
                //else default to English
                else
                {
                    languageIndex = LanguageIndex.English;
                }

                //get info for code and language
                name = languageName.get(languageIndex).get(index);

                //if unknown and not English
                if((name == null || name.equals("?")) && languageIndex != LanguageIndex.English)
                {
                    //default to English
                    name = languageName.get(LanguageIndex.English).get(index);
                }
            }

            /*if(name == null)
            {
                int i = 0;
                i++;
            }*/

            //if name set but unknown
            if(name != null && name.equals("?"))
            {
                //clear
                name = null;
            }

            //return name
            return(name);
        }

        //Returns if have data
        public static boolean haveData()
        {
            return(codeName.size() > 0);
        }
    }

    @SuppressWarnings("SpellCheckingInspection")
    public static abstract class LocaleCategory
    {
        //Gets category locale
        public static String getCategory(Resources res, String name)
        {
            switch(name.toLowerCase())
            {
                case "100 (or so) brightest":
                    name = res.getString(R.string.category_100_brightest);
                    break;

                case "amateur":
                    name = res.getString(R.string.category_amateur);
                    break;

                case "amateur radio":
                    name = res.getString(R.string.category_amateur_radio);
                    break;

                case "analyst":
                    name = res.getString(R.string.category_analyst);
                    break;

                case "argos data collection system":
                    name = res.getString(R.string.category_argos_data_collection_system);
                    break;

                case "beidou navigation system":
                    name = res.getString(R.string.category_beidou_navigation_system);
                    break;

                case "breeze-m r/b breakup":
                    name = res.getString(R.string.category_breeze_m_rb_breakup);
                    break;

                case "brightest":
                    name = res.getString(R.string.category_brightest);
                    break;

                case "bright geo":
                    name = res.getString(R.string.category_bright_geo);
                    break;

                case "cosmos 2251 debris":
                    name = res.getString(R.string.category_cosmos_2251_debris);
                    break;

                case "disaster monitoring":
                    name = res.getString(R.string.category_disaster_monitoring);
                    break;

                case "earth resources":
                    name = res.getString(R.string.category_earth_resources);
                    break;

                case "education":
                    name = res.getString(R.string.category_education);
                    break;

                case "engineering":
                    name = res.getString(R.string.category_engineering);
                    break;

                case "experimental":
                    name = res.getString(R.string.category_experimental);
                    break;

                case "fengyun 1c debris":
                    name = res.getString(R.string.category_fengyun_1c_debris);
                    break;

                case "geodetic":
                    name = res.getString(R.string.category_geodetic);
                    break;

                case "geostationary":
                    name = res.getString(R.string.category_geostationary);
                    break;

                case "geosynchronous":
                    name = res.getString(R.string.category_geosynchronous);
                    break;

                case "globalstar":
                    name = res.getString(R.string.category_globalstar);
                    break;

                case "global positioning system (gps)":
                    name = res.getString(R.string.category_global_positioning_system);
                    break;

                case "glonass operational":
                    name = res.getString(R.string.category_glonass_operational);
                    break;

                case "gps operational":
                    name = res.getString(R.string.category_gps_operational);
                    break;

                case "human spaceflight":
                    name = res.getString(R.string.category_human_spaceflight);
                    break;

                case "inmarsat":
                    name = res.getString(R.string.category_inmarsat);
                    break;

                case "intelsat":
                    name = res.getString(R.string.category_intelsat);
                    break;

                case "iridium":
                    name = res.getString(R.string.category_iridium);
                    break;

                case "iridium 33 debris":
                    name = res.getString(R.string.category_iridium_33_debris);
                    break;

                case "iridium next":
                    name = res.getString(R.string.category_iridium_next);
                    break;

                case "military":
                case "miscellaneous military":
                    name = res.getString(R.string.category_military);
                    break;

                case "navigation":
                    name = res.getString(R.string.category_navigation);
                    break;

                case "navy navigation satellite system":
                case "navy navigation satellite system (nnss)":
                    name = res.getString(R.string.category_navy_navigation_satellite_system);
                    break;

                case "o3b networks":
                    name = res.getString(R.string.category_o3b_networks);
                    break;

                case "orbcomm":
                    name = res.getString(R.string.category_orbcomm);
                    break;

                case "other":
                case "other comm":
                    name = res.getString(R.string.category_other);
                    break;

                case "planet":
                    name = res.getString(R.string.category_planet);
                    break;

                case "radar calibration":
                    name = res.getString(R.string.category_radar_calibration);
                    break;

                case "russian leo navigation":
                    name = res.getString(R.string.category_russian_leo_navigation);
                    break;

                case "satellite-based augmentation system":
                case "satellite-based augmentation system (waas/egnos/msas)":
                    name = res.getString(R.string.category_satellite_based_augmentation_system);
                    break;

                case "search & rescue":
                case "search & rescue (sarsat)":
                    name = res.getString(R.string.category_search_and_rescue);
                    break;

                case "space & earth science":
                    name = res.getString(R.string.category_space_and_earth_science);
                    break;

                case "space stations":
                    name = res.getString(R.string.category_space_stations);
                    break;

                case "special interest":
                    name = res.getString(R.string.category_special_interest);
                    break;

                case "tracking and data relay satellite system":
                    name = res.getString(R.string.category_tracking_and_data_relay_satellite_system);
                    break;

                case "tv":
                    name = res.getString(R.string.category_tv);
                    break;

                case "visible":
                    name = res.getString(R.string.category_visible);
                    break;

                case "weather":
                    name = res.getString(R.string.category_weather);
                    break;

                case "xm and sirius":
                    name = res.getString(R.string.category_xm_and_sirius);
                    break;
            }

            //return name in upper case
            return(name.toUpperCase());
        }
    }

    private static final int TIME_ZONE_COLUMNS = 3;
    private static final int TIME_ZONE_ROWS = 23427;
    private static final String TIME_ZONE_FILE_SEPARATOR = "\t";

    public static class DatabaseSatellite implements Parcelable
    {
        public final String name;
        private final String userName;
        public final int noradId;
        public String ownerCode;
        public final String ownerName;
        public final String tleLine1;
        public final String tleLine2;
        public final String gp;
        public Calculations.TLEDataType tle;
        public long tleDateMs;
        public final long updateDateMs;
        public final long launchDateMs;
        public int pathColor;
        public final byte orbitalType;
        public final boolean tleIsAccurate;
        public final boolean isSelected;
        public static final Creator<DatabaseSatellite> CREATOR =  new Parcelable.Creator<DatabaseSatellite>()
        {
            @Override
            public DatabaseSatellite createFromParcel(Parcel source)
            {
                Bundle bundle = source.readBundle(getClass().getClassLoader());
                if(bundle == null)
                {
                    bundle = new Bundle();
                }

                return(new DatabaseSatellite(bundle.getString(ParamTypes.Name), bundle.getString(ParamTypes.UserName), bundle.getInt(ParamTypes.Norad), bundle.getString(ParamTypes.OwnerCode), bundle.getString(ParamTypes.OwnerName), bundle.getLong(ParamTypes.LaunchDate), bundle.getString(ParamTypes.TLELine1), bundle.getString(ParamTypes.TLELine2), bundle.getLong(ParamTypes.TLEDate), bundle.getString(ParamTypes.GP), bundle.getLong(ParamTypes.UpdateDate), bundle.getInt(ParamTypes.PathColor), bundle.getByte(ParamTypes.OrbitalType), bundle.getBoolean(ParamTypes.IsSelected)));
            }

            @Override
            public DatabaseSatellite[] newArray(int size)
            {
                return(new DatabaseSatellite[size]);
            }
        };

        public DatabaseSatellite(String nm, String uNm, int nrd, String ownrCd, String ownrNm, long launchDate, String line1, String line2, long tleDtMs, String gpData, long updateDtMs, int pthClr, byte orbType, boolean selected)
        {
            name = nm;
            userName = (uNm != null ? uNm : "");
            noradId = nrd;
            ownerCode = ownrCd;
            ownerName = ownrNm;
            tleLine1 = line1;
            tleLine2 = line2;
            gp = gpData;
            tle = null;
            if(gp != null && !gp.equals(""))
            {
                tle = Calculations.loadTLE(gp);
            }
            if(tle == null)
            {
                tle = Calculations.loadTLE(line1, line2);
            }
            tleDateMs = tleDtMs;
            updateDateMs = updateDtMs;
            pathColor = pthClr;
            orbitalType = orbType;
            isSelected = selected;
            launchDateMs = launchDate;

            if((tleDateMs == Globals.INVALID_DATE_MS || tleDateMs == Globals.UNKNOWN_DATE_MS) && tle.satelliteNum != Universe.IDs.Invalid)
            {
                tleDateMs = Globals.julianDateToCalendar(tle.epochJulian).getTimeInMillis();
            }

            tleIsAccurate = Globals.getTLEIsAccurate(tleDateMs);
        }
        public DatabaseSatellite(String nm, int nrd, String ownCd, long launchDate, byte orbType)
        {
            this(nm, null, nrd, ownCd, null, launchDate, null, null, Globals.UNKNOWN_DATE_MS, null, Globals.UNKNOWN_DATE_MS, Color.DKGRAY, orbType, true);
        }

        public String getName()
        {
            return(!userName.equals("") ? userName : name);
        }

        public String[] getTLELines()
        {
            String line1;
            String line2;

            //if TLE lines exist
            if(tleLine1 != null && tleLine1.length() > 0 && tleLine2 != null && tleLine2.length() > 0)
            {
                //return lines
                return(new String[]{tleLine1, tleLine2});
            }
            //else if TLE data exists and a valid TLE satellite number
            else if(tle != null && tle.satelliteNum <= 99999)
            {
                //create lines
                line1 = String.format(Locale.US, "1 %5d%c %02d%03d%-3s %02d%012.8f %10s %8s %8s %1d %4d", tle.satelliteNum, tle.classification, tle.launchYear % 100, tle.launchNum, tle.launchPiece, tle.epochYear % 100, tle.epochDay, String.format(Locale.US, "%10.8f", tle.meanMotionDeriv1).replace("0.", "."), Calculations.getTLEDecimalExponentString(tle.meanMotionDeriv2), Calculations.getTLEDecimalExponentString(tle.drag), tle.ephemeris, tle.elementNum);
                line2 = String.format(Locale.US, "2 %5d %8.4f %8.4f %07d %8.4f %8.4f %11.8f%5d", tle.satelliteNum, tle.inclinationDeg, tle.rightAscnAscNodeDeg, (int)(tle.eccentricity * Math.pow(10, 7)), tle.argPerigreeDeg, tle.meanAnomalyDeg, tle.revsPerDay, tle.revAtEpoch);

                //add checksums
                line1 += Calculations.getTLEChecksum(line1);
                line2 += Calculations.getTLEChecksum(line2);

                //return created lines
                return(new String[]{line1, line2});
            }
            else
            {
                //none
                return(null);
            }
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

            bundle.putString(ParamTypes.Name, name);
            bundle.putString(ParamTypes.UserName, userName);
            bundle.putInt(ParamTypes.Norad, noradId);
            bundle.putString(ParamTypes.OwnerCode, ownerCode);
            bundle.putString(ParamTypes.OwnerName, ownerName);
            bundle.putLong(ParamTypes.LaunchDate, launchDateMs);
            bundle.putString(ParamTypes.TLELine1, tleLine1);
            bundle.putString(ParamTypes.TLELine2, tleLine2);
            bundle.putLong(ParamTypes.TLEDate, tleDateMs);
            bundle.putString(ParamTypes.GP, gp);
            bundle.putLong(ParamTypes.UpdateDate, updateDateMs);
            bundle.putInt(ParamTypes.PathColor, pathColor);
            bundle.putByte(ParamTypes.OrbitalType, orbitalType);
            bundle.putBoolean(ParamTypes.IsSelected, isSelected);

            dest.writeBundle(bundle);
        }
    }

    public static class SatelliteData
    {
        final Calculations.SatelliteObjectType satellite;
        final DatabaseSatellite database;

        public SatelliteData(DatabaseSatellite db)
        {
            satellite = Calculations.loadOrbital(db);
            database = db;
        }
        public SatelliteData(Context context, int noradId)
        {
            this(getOrbital(context, noradId));
        }

        public int getSatelliteNum()
        {
            return(satellite != null ? satellite.getSatelliteNum() : Integer.MAX_VALUE);
        }

        public String getName(String defaultVal)
        {
            return(database != null ? database.getName() : defaultVal);
        }
        public String getName()
        {
            return(getName("?"));
        }

        public byte getOrbitalType()
        {
            return(database != null ? database.orbitalType : OrbitalType.Satellite);
        }

        public boolean equals(SatelliteData other)
        {
            boolean satNull = (satellite == null);
            boolean otherSatNull = (other == null || other.satellite == null);

            return((satNull && otherSatNull) || (!satNull && !otherSatNull && satellite.getSatelliteNum() == other.satellite.getSatelliteNum()));
        }
    }

    public static abstract class LocationType
    {
        public static final byte Current = 0;
        public static final byte Saved = 1;
        public static final byte Online = 2;
        public static final byte New = 3;
    }

    public static class DatabaseLocation
    {
        public final int id;
        public final String name;
        public String zoneId;
        public double latitude;
        public double longitude;
        public double altitudeKM;
        public final byte locationType;
        public final boolean isChecked;

        public DatabaseLocation(int locID, String nm, double lat, double lon, double altKm, String znId, byte ltype, boolean checked)
        {
            id = locID;
            name = nm;
            latitude = lat;
            longitude = lon;
            altitudeKM = altKm;
            zoneId = znId;
            locationType = ltype;
            isChecked = checked;
        }

        public boolean isValid()
        {
            return(latitude != 0 || longitude != 0 || altitudeKM != 0);
        }
    }

    private static class UpdateStatusType
    {
        public boolean needUpdate;
        public int previousVersion;
        public int currentVersion;

        public UpdateStatusType()
        {
            needUpdate = false;
            previousVersion = DB_VERSION;
            currentVersion = DB_VERSION;
        }
    }

    public static final byte MAX_ORBITAL_NAME_LENGTH = 24;
    private static final byte MAX_LOCATION_NAME_LENGTH = 50;
    private static final byte MAX_OWNER_CODE_LENGTH = 8;
    private static final byte MAX_OWNER_NAME_LENGTH = 40;
    private static final byte MAX_CATEGORY_NAME_LENGTH = 40;
    private static final byte MAX_ZONE_ID_LENGTH = 40;
    private static final int MAX_INFO_LENGTH = 5000;
    private static final int MAX_LANGUAGE_LENGTH = 10;

    private static final int DB_VERSION = 13;
    private static final String DB_NAME = "OrbTrack.DB";
    private static UpdateStatusType updateStatus = null;

    private Globals.OnProgressChangedListener progressListener;

    public Database(Context context)
    {
        super(context, DB_NAME, null, DB_VERSION);

        //init data
        if(!LocaleOwner.haveData())
        {
            LocaleOwner.initData(context);
        }
        if(!LocaleInformation.haveData())
        {
            LocaleInformation.initData(context);
        }
    }

    public void setProgressListener(Globals.OnProgressChangedListener listener)
    {
        progressListener = listener;
    }

    //Sets up table in database with initial data
    private void initTable(SQLiteDatabase db, Resources res, String tableName, String tableValues, Object[] sqlBindConstants,  @NonNull byte[] sqlBindTypes, int fileId, String fileSeparator, int rowCount, int columnCount, int progressTitleId)
    {
        int index = 0;
        int bindIndex;
        int currentColumn;
        int pendingRows = 0;
        boolean usingConstant;
        String line;
        SQLiteStatement sqlStatement = db.compileStatement("REPLACE INTO " + tableName + " " + tableValues);
        String[] columns;

        //open file
        BufferedReader file = new BufferedReader(new InputStreamReader(res.openRawResource(fileId)));
        try
        {
            //while there are lines to read
            while((line = file.readLine()) != null)
            {
                //split columns
                columns = line.split(fileSeparator);

                //if valid number of columns
                if(columns.length == columnCount)
                {
                    //if starting over
                    if(pendingRows == 0)
                    {
                        //begin
                        db.beginTransaction();
                        sqlStatement.clearBindings();
                    }

                    //add bind values
                    currentColumn = 0;
                    for(bindIndex = 0; bindIndex < sqlBindTypes.length; bindIndex++)
                    {
                        //check if using constant
                        usingConstant = (sqlBindConstants != null && sqlBindConstants[bindIndex] != null);

                        //handle based on type
                        switch(sqlBindTypes[bindIndex])
                        {
                            case SQLBindType.Double:
                                sqlStatement.bindDouble(bindIndex + 1, (usingConstant ? (Double)sqlBindConstants[bindIndex] : Double.valueOf(columns[currentColumn])));
                                break;

                            case SQLBindType.String:
                                sqlStatement.bindString(bindIndex + 1, (usingConstant ? (String)sqlBindConstants[bindIndex] : columns[currentColumn]));
                                break;
                        }

                        //if didn't use constant
                        if(!usingConstant)
                        {
                            //go to next column
                            currentColumn++;
                        }
                    }
                    sqlStatement.execute();

                    //update progress
                    sendProgress(Globals.ProgressType.Running, res.getString(progressTitleId), index, rowCount);
                    index++;

                    //update pending rows
                    pendingRows++;
                    if(pendingRows >= 500)
                    {
                        //finish
                        db.setTransactionSuccessful();
                        db.endTransaction();
                        pendingRows = 0;
                    }
                }
            }

            //if there are still pending rows
            if(pendingRows > 0)
            {
                //finish
                db.setTransactionSuccessful();
                db.endTransaction();
            }
        }
        catch(IOException ex)
        {
            //do nothing
        }
    }

    //Sets up database with initial data
    private void initData(Context context, SQLiteDatabase db)
    {
        Resources res = (context != null ? context.getResources() : null);

        //if no context
        if(context == null)
        {
            //stop
            return;
        }

        //update progress
        sendProgress(Globals.ProgressType.Started);

        //make sure tables exist
        db.execSQL("CREATE TABLE IF NOT EXISTS " + Tables.Orbital + " ([ID] INTEGER PRIMARY KEY, [Name] TEXT(" + MAX_ORBITAL_NAME_LENGTH + "), [User_Name] TEXT(" + MAX_ORBITAL_NAME_LENGTH + "), [Norad] INTEGER UNIQUE, [Owner_Code] TEXT(" + MAX_OWNER_CODE_LENGTH + "), [Launch_Date] INTEGER, [TLE_Line1] TEXT(69) NOT NULL, [TLE_Line2] TEXT(69) NOT NULL, [TLE_Date] INTEGER, [GP] TEXT, [Update_Date] INTEGER, [Path_Color] INTEGER, [Type] INTEGER, [Selected] INTEGER)");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + Tables.Location + " ([ID] INTEGER PRIMARY KEY, [Name] TEXT("+ MAX_LOCATION_NAME_LENGTH + "), [Latitude] DOUBLE, [Longitude] DOUBLE, [Altitude] DOUBLE, [ZoneId] TEXT(" + MAX_ZONE_ID_LENGTH + "), [Type] INTEGER, [Selected] INTEGER)");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + Tables.LocationName + "([ID] INTEGER PRIMARY KEY, [Latitude] DOUBLE, [Longitude] DOUBLE, [Name] TEXT(" + MAX_LOCATION_NAME_LENGTH + "))");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + Tables.TimeZone + "([ID] INTEGER PRIMARY KEY, [Latitude] DOUBLE, [Longitude] DOUBLE, [ZoneId] TEXT(" + MAX_ZONE_ID_LENGTH + "))");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + Tables.Altitude + "([ID] INTEGER PRIMARY KEY, [Latitude] DOUBLE, [Longitude] DOUBLE, [Altitude] DOUBLE)");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + Tables.MasterSatellite + " ([ID] INTEGER PRIMARY KEY, [Norad] INTEGER UNIQUE, [Name] TEXT(" + MAX_ORBITAL_NAME_LENGTH + "), [Owner_Code] TEXT(" + MAX_OWNER_CODE_LENGTH + "), [Launch_Date] INTEGER)");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + Tables.Owner + " ([ID] INTEGER PRIMARY KEY, [Code] TEXT(" + MAX_OWNER_CODE_LENGTH + ") UNIQUE, [Name] TEXT(" + MAX_OWNER_NAME_LENGTH + "))");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + Tables.Category + " ([ID] INTEGER PRIMARY KEY, [Name] TEXT(" + MAX_CATEGORY_NAME_LENGTH + ") UNIQUE, [Indx] INTEGER)");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + Tables.SatelliteCategory + " ([ID] INTEGER PRIMARY KEY, [Norad] INTEGER, [Category_Index] INTEGER)");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + Tables.Information + "([ID] INTEGER PRIMARY KEY, [Norad] INTEGER, [Source] INTEGER, [Language] TEXT(" + MAX_LANGUAGE_LENGTH + "), [Info] TEXT(" + MAX_INFO_LENGTH + "))");

        //if there are no orbitals
        if(runQuery(context, "SELECT [Name] FROM " + Tables.Orbital + " WHERE [Type]=" + OrbitalType.Star + " LIMIT 1", null).length == 0)
        {
            //add stars
            addOrbital(context, Universe.IDs.Sun, Color.YELLOW, OrbitalType.Star);
            addOrbital(context, Universe.IDs.Polaris, ResourcesCompat.getColor(res, R.color.very_light_gray, null), OrbitalType.Star);

            //add planets
            addOrbital(context, Universe.IDs.Moon, Color.GRAY, OrbitalType.Planet);
            addOrbital(context, Universe.IDs.Mars, Color.RED, OrbitalType.Planet);
            addOrbital(context, Universe.IDs.Mercury, ResourcesCompat.getColor(res, R.color.dark_red, null), OrbitalType.Planet);
            addOrbital(context, Universe.IDs.Venus, ResourcesCompat.getColor(res, R.color.dark_yellow, null), OrbitalType.Planet);
            addOrbital(context, Universe.IDs.Jupiter, ResourcesCompat.getColor(res, R.color.orange, null), OrbitalType.Planet);
            addOrbital(context, Universe.IDs.Saturn, ResourcesCompat.getColor(res, R.color.cinnamon, null), OrbitalType.Planet);
            addOrbital(context, Universe.IDs.Uranus, Color.CYAN, OrbitalType.Planet);
            addOrbital(context, Universe.IDs.Neptune, ResourcesCompat.getColor(res, R.color.dark_blue, null), OrbitalType.Planet);
            addOrbital(context, Universe.IDs.Pluto, ResourcesCompat.getColor(res, R.color.tan, null), OrbitalType.Planet);
        }

        //if there are no locations
        if(runQuery(context, "SELECT [Name] FROM " + Tables.Location + " WHERE [Type]=" + LocationType.Current + " LIMIT 1", null).length == 0)
        {
            String zoneId = TimeZone.getDefault().getID();

            //add current location
            runInsert(context, Tables.Location, getLocationValues("Current", 0, 0, 0, zoneId, LocationType.Current, true, true));
        }

        //if there are no groups
        if(runQuery(context, "SELECT [Name] FROM " + Tables.Category + " LIMIT 1", null).length == 0)
        {
            //add none group
            runInsert(context, Tables.Category, getCategoryValues("None", 0));
        }

        //if there is no information
        if(runQuery(context, "SELECT [Info] FROM " + Tables.Information + " LIMIT 1", null).length == 0)
        {
            //load information
            initTable(db, res, Tables.Information, "([Norad], [Source], [Language], [Info]) VALUES(?, ?, ?, ?)", new Object[]{null, String.valueOf(UpdateSource.SpaceDotCom), "en", null}, new byte[]{SQLBindType.String, SQLBindType.String, SQLBindType.String, SQLBindType.String}, R.raw.information_en, INFO_FILE_SEPARATOR, INFO_FILE_ROWS, INFO_FILE_COLUMNS, R.string.title_information);

            //update locale information
            LocaleInformation.initData(context);
        }

        //if there are no owners
        if(runQuery(context, "SELECT [Code] FROM " + Tables.Owner + " LIMIT 1", null).length == 0)
        {
            //load owners
            initTable(db, res, Tables.Owner, "([Code], [Name]) VALUES(?, ?)", null, new byte[]{SQLBindType.String, SQLBindType.String}, R.raw.owners_en, OWNERS_FILE_SEPARATOR, OWNERS_FILE_ROWS, OWNERS_FILE_COLUMNS, R.string.title_owners);

            //update locale owners
            LocaleOwner.initData(context);
        }

        //if there are no satellites
        if(runQuery(context, "SELECT [Name] FROM " + Tables.Orbital + " WHERE [Type]='" + OrbitalType.Satellite + "' LIMIT 1", null).length == 0)
        {
            //load default satellites
            saveSatellite(context, "ISS (ZARYA)", ISS_ZARYA_NORAD_ID, "ISS", 911548800000L, TLELines.ISSZarya1, TLELines.ISSZarya2, TLELines.ISSZaryaDate, null, TLELines.ISSZaryaDate, OrbitalType.Satellite);
        }

        //if there are no time zones
        if(runQuery(context, "SELECT [ZoneId] FROM " + Tables.TimeZone + " LIMIT 1", null).length == 0)
        {
            //load time zones
            initTable(db, res, Tables.TimeZone, "([Latitude], [Longitude], [ZoneId]) VALUES(?, ?, ?)", null, new byte[]{SQLBindType.Double, SQLBindType.Double, SQLBindType.String}, R.raw.timezones, TIME_ZONE_FILE_SEPARATOR, TIME_ZONE_ROWS, TIME_ZONE_COLUMNS, R.string.title_time_zone_locations);
        }

        //done with first load
        Settings.setFirstRun(context, false);

        //update progress
        sendProgress(Globals.ProgressType.Finished);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {}

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        //if update status not set yet
        if(updateStatus == null)
        {
            //create it
            updateStatus = new UpdateStatusType();
        }

        //update with current status
        updateStatus.needUpdate = true;
        updateStatus.previousVersion = oldVersion;
        updateStatus.currentVersion = newVersion;
    }

    //Handles any needed updates
    public static void handleUpdates(Context context)
    {
        SQLiteDatabase db;
        DatabaseSatellite issZarya;

        //if update status is set
        if(updateStatus != null)
        {
            //if need to run updates
            if(updateStatus.needUpdate)
            {
                //if coming from version before adding GP data
                if(updateStatus.previousVersion < 6)
                {
                    //add GP data to orbitals
                    db = DatabaseManager.get(context, true);
                    db.execSQL("ALTER TABLE " + Tables.Orbital + " ADD COLUMN [GP] TEXT");
                }

                //if ISS Zarya exists and is older than hard coded TLE
                issZarya = getOrbital(context, ISS_ZARYA_NORAD_ID);
                if(issZarya != null && issZarya.updateDateMs < TLELines.ISSZaryaDate)
                {
                    //save updated TLE
                    saveSatellite(context, issZarya.getName(), ISS_ZARYA_NORAD_ID, issZarya.ownerCode, issZarya.launchDateMs, TLELines.ISSZarya1, TLELines.ISSZarya2, TLELines.ISSZaryaDate, null, TLELines.ISSZaryaDate, OrbitalType.Satellite);
                }

                //if coming from version before master list parsing improved
                if(updateStatus.previousVersion < 4 && updateStatus.currentVersion >= 4)
                {
                    //clear master satellites
                    clearMasterSatelliteTable(context);
                }

                //if previous is before lens icon indicators
                if(updateStatus.previousVersion < 30)
                {
                    //default to lens icon indicators
                    Settings.setIndicator(context, Settings.Options.LensView.IndicatorType.Icon);
                }

                //show any notice
                showNoticeDialog(context, updateStatus.previousVersion);
            }

            //reset
            updateStatus.needUpdate = false;
        }
    }

    //Builds database
    public static void build(Context context, Globals.OnProgressChangedListener listener)
    {
        DatabaseManager.getDb(context, listener).initData(context, DatabaseManager.get(context, true));
    }

    //Sends progress
    private void sendProgress(int progressType, String section, long index, long count)
    {
        //if using progress listener
        if(progressListener != null)
        {
            //send progress
            progressListener.onProgressChanged(progressType, section, index, count);
        }
    }
    private void sendProgress(int progressType)
    {
        sendProgress(progressType, null, 0 , 0);
    }

    //Show notice dialog
    private static void showNoticeDialog(final Context context, int previousVersion)
    {
        Resources res = context.getResources();

        //if changed to newer version and combined notice not shown
        if(previousVersion <= 2 && DB_VERSION > 2 && !Settings.getCombinedShown(context))
        {
            //show notice
            Globals.showConfirmDialog(context, res.getQuantityString(R.plurals.title_updates, 1), res.getString(R.string.desc_combine_lists_notice), res.getString(R.string.title_ok), null, true, null, null, new DialogInterface.OnDismissListener()
            {
                @Override
                public void onDismiss(DialogInterface dialog)
                {
                    //remember shown
                    Settings.setCombinedShown(context, true);
                }
            });
        }
    }

    //Shows the given error
    private static void showError(final Context context, final Exception ex)
    {
        try
        {
            ((Activity)context).runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    //show error
                    Toast.makeText(context, ex.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }
        catch(Exception ex2)
        {
            //do nothing
        }
    }

    //Runs a query
    private static String[][] runQuery(Context context, String sql, String[] replacements)
    {
        int index;
        int index2;
        int column_count;
        Cursor queryResult;
        String[][] queryResults = new String[0][0];

        try
        {
            SQLiteDatabase db = DatabaseManager.get(context, false);

            //run query
            queryResult = db.rawQuery(sql, replacements);
            if(queryResult != null)
            {
                //get query results
                column_count = queryResult.getColumnCount();
                queryResults = new String[queryResult.getCount()][column_count];
                for(index = 0; index < queryResults.length && queryResult.moveToNext(); index++)
                {
                    //go through each column
                    for(index2 = 0; index2 < column_count; index2++)
                    {
                        //get column value
                        queryResults[index][index2] = queryResult.getString(index2);
                    }
                }
                queryResult.close();
            }
        }
        catch(Exception ex)
        {
            //show error
            showError(context, ex);
        }

        return(queryResults);
    }

    //Runs an insert and returns ID
    private static long runInsert(Context context, String table, ContentValues values)
    {
        long id = -1;
        SQLiteDatabase db = DatabaseManager.get(context, true);

        try
        {
            id = db.insert(table, null, values);
        }
        catch(Exception ex)
        {
            //show error
            showError(context, ex);
        }

        return(id);
    }

    //Runs an update and returns success
    private static boolean runUpdate(Context context, String table, ContentValues values, String selection)
    {
        boolean success = false;
        SQLiteDatabase db = DatabaseManager.get(context, true);

        try
        {
            success = (db.update(table, values, selection, null) > 0);
        }
        catch(Exception ex)
        {
            //show error
            showError(context, ex);
        }

        return(success);
    }

    //Runs a save (updates or inserts) and returns ID
    private static long runSave(Context context, long id, String table, ContentValues values)
    {
        long saveID;

        if(id > 0)
        {
            saveID = (runUpdate(context, table, values, "[ID]='" + id + "'") ? id : -1);
        }
        else
        {
            saveID = runInsert(context, table, values);
        }

        return(saveID);
    }

    //Runs a delete and returns success
    private static boolean runDelete(Context context, String table, String selection)
    {
        boolean success = false;
        SQLiteDatabase db = DatabaseManager.get(context, true);

        try
        {
            success = (db.delete(table, selection, null) > 0);
        }
        catch(Exception ex)
        {
            //show error
            showError(context, ex);
        }

        return(success);
    }

    //Adds orbital to database
    private static void addOrbital(Context context, int orbitalID, int pathColor, byte orbitalType)
    {
        long gmtMs = Globals.getGMTTime().getTimeInMillis();
        runInsert(context, Tables.Orbital, getSatelliteValues(Universe.getName(null, orbitalID), "", orbitalID, "", 0, "", "", gmtMs, "", gmtMs, pathColor, orbitalType, true));
    }

    //Gets all satellite data for the given norad ID
    public static String[][] getSatelliteData(Context context, int noradId)
    {
        return(runQuery(context, "SELECT [Name], [User_Name], [Norad], [Owner_Code], [Launch_Date], [TLE_Line1], [TLE_Line2], [TLE_Date], [GP], [Update_Date], [Path_Color], [Type], [Selected] FROM " + Tables.Orbital + " WHERE [Norad]=" + noradId, null));
    }

    //Gets satellite values
    private static ContentValues getSatelliteValues(String name, String userName, int noradId, String ownerCode, long launchDate, String tleLine1, String tleLine2, long tleDate, String gp, long updateDateMs, int pathColor, byte orbitalType, boolean selected)
    {
        ContentValues satelliteValues = new ContentValues(0);
        if(name != null)
        {
            if(name.length() > MAX_ORBITAL_NAME_LENGTH)
            {
                name = name.substring(0, MAX_ORBITAL_NAME_LENGTH - 1);
            }
            satelliteValues.put("[Name]", name.replace("[", "").replace("]", ""));
        }
        if(userName != null)
        {
            if(userName.length() > MAX_ORBITAL_NAME_LENGTH)
            {
                userName = userName.substring(0, MAX_ORBITAL_NAME_LENGTH - 1);
            }
            satelliteValues.put("[User_Name]", userName);
        }
        satelliteValues.put("[Norad]", noradId);
        if(ownerCode != null)
        {
            satelliteValues.put("[Owner_Code]", ownerCode);
        }
        if(launchDate != Globals.INVALID_DATE_MS)
        {
            satelliteValues.put("[Launch_Date]", launchDate);
        }
		if(tleLine1 != null)
        {
            satelliteValues.put("[TLE_Line1]", tleLine1);
        }
        if(tleLine2 != null)
        {
            satelliteValues.put("[TLE_Line2]", tleLine2);
        }
        if(tleDate != Globals.INVALID_DATE_MS)
        {
            satelliteValues.put("[TLE_Date]", tleDate);
        }
        if(gp != null)
        {
            satelliteValues.put("[GP]", gp);
        }
        if(updateDateMs != Globals.INVALID_DATE_MS)
        {
            satelliteValues.put("[Update_Date]", updateDateMs);
        }
        if(pathColor != Integer.MAX_VALUE)
        {
            satelliteValues.put("[Path_Color]", pathColor);
        }
        if(orbitalType != Byte.MAX_VALUE)
        {
            satelliteValues.put("[Type]", orbitalType);
        }
        satelliteValues.put("[Selected]", (selected ? 1 : 0));
        return(satelliteValues);
    }

    //Gets desired orbitals from the database
    private static DatabaseSatellite[] getOrbitals(Context context, String sqlConditions, boolean addLocation, boolean addNone)
    {
        int index;
        int noradId;
        String name;
        String ownerCode;
        String localOwnerName;
        String[][] queryResult = runQuery(context, "SELECT " + Tables.Orbital + ".[Name], [User_Name], [Norad], [Code], " + Tables.Owner + ".[Name], [Launch_Date], [TLE_Line1], [TLE_Line2], [TLE_Date], [GP], [Update_Date], [Path_Color], [Type], [Selected] FROM " + Tables.Orbital + " LEFT JOIN " + Tables.Owner + " ON [Owner_Code]=[Code]" + (sqlConditions != null ? (" WHERE " + sqlConditions) : "") + " ORDER BY CASE WHEN [User_Name] IS NULL OR [User_Name]='' THEN " + Tables.Orbital + ".[Name] ELSE [User_Name] END ASC", null);
        ArrayList<DatabaseSatellite> list = new ArrayList<>(0);

        //if adding none
        if(addNone)
        {
            //add none to list
            list.add(new DatabaseSatellite(context.getResources().getString(R.string.title_none), Universe.IDs.None, null, Globals.UNKNOWN_DATE_MS, OrbitalType.Planet));
        }

        //if adding location
        if(addLocation)
        {
            //add location to list
            list.add(new DatabaseSatellite(context.getResources().getString(R.string.title_location_current), Universe.IDs.CurrentLocation, null, Globals.UNKNOWN_DATE_MS, OrbitalType.Planet));
        }

        //go through each satellite
        for(index = 0; index < queryResult.length; index++)
        {
            //get ID and name
            noradId = Integer.parseInt(queryResult[index][2]);
            name = (noradId < 0 ? Universe.getName(context, noradId) : queryResult[index][0]);

            //get owner code and name
            ownerCode = queryResult[index][3];
            localOwnerName = LocaleOwner.getName(context, ownerCode);

            //add to list
            list.add(new DatabaseSatellite(name, queryResult[index][1], noradId, ownerCode, (localOwnerName != null ? localOwnerName : queryResult[index][4]), Long.parseLong(queryResult[index][5]), queryResult[index][6], queryResult[index][7], Long.parseLong(queryResult[index][8]), queryResult[index][9], Long.parseLong(queryResult[index][10]), Integer.parseInt(queryResult[index][11]), Byte.parseByte(queryResult[index][12]), queryResult[index][13].equals("1")));
        }

        //return list as array
        return(list.toArray(new DatabaseSatellite[0]));
    }
    public static DatabaseSatellite[] getOrbitals(Context context, String sqlConditions)
    {
        return(getOrbitals(context, sqlConditions, false, false));
    }
    public static DatabaseSatellite[] getOrbitals(Context context, int[] noradIds)
    {
        int index;
        StringBuilder idStrings = new StringBuilder();

        //go through each ID
        for(index = 0; index < noradIds.length; index++)
        {
            //if after the first
            if(index > 0)
            {
                //add separator
                idStrings.append(",");
            }

            //add ID
            idStrings.append(noradIds[index]);
        }

        //return satellites
        return(getOrbitals(context, "[Norad] IN(" + idStrings.toString() + ")", false, false));
    }
    public static DatabaseSatellite[] getOrbitals(Context context)
    {
        return(getOrbitals(context, null, false, false));
    }

    //Returns sql conditions to get satellites from orbitals
    public static String getSatelliteConditions()
    {
        return("[Type] IN(" + OrbitalType.Satellite + ", " + OrbitalType.RocketBody + ", " + OrbitalType.Debris + ")");
    }

    //Gets desired orbital with norad ID
    public static DatabaseSatellite getOrbital(Context context, int noradId)
    {
        DatabaseSatellite[] satellites = getOrbitals(context, "[Norad]=" + noradId);
        return(satellites.length > 0 ? satellites[0] : null);
    }

    //Gets all selected orbitals
    public static DatabaseSatellite[] getSelectedOrbitals(Context context, boolean addLocation, boolean addNone)
    {
        return(getOrbitals(context, "[Selected]=1", addLocation, addNone));
    }

    //Gets a satellite ID by norad
    private static long getSatelliteID(Context context, int noradId)
    {
        //run query
        String[][] queryResult = runQuery(context,"SELECT [ID] FROM " + Tables.Orbital + " WHERE [Norad]=" + noradId + " LIMIT 1", null);
        return(queryResult.length > 0 ? Long.parseLong(queryResult[0][0]) : -1);
    }

    /*//Gets a satellite TLE by norad
    public static String[][] getSatelliteTLE(Context context, int noradId)
    {
        return(runQuery(context, "SELECT [TLE_Line1], [TLE_Line2] FROM " + Tables.Orbital + " WHERE [Norad]=" + noradId + " LIMIT 1", null));
    }*/

    //Gets whether a satellite is selected
    private static boolean getSatelliteSelected(Context context, long id)
    {
        DatabaseSatellite[] satellites;

        //if trying to update an existing satellite
        if(id > 0)
        {
            //if able to find satellite
            satellites = getOrbitals(context, Tables.Orbital + ".[ID]='" + id + "'");
            if(satellites.length > 0)
            {
                //get current selected state
                return(satellites[0].isSelected);
            }
        }

        //unknown
        return(false);
    }

    //Modifies satellite data and returns ID
    public static long saveSatellite(Context context, String name, String userName, int noradId, String ownerCode, long launchDate, String tleLine1, String tleLine2, long tleDateMs, String gp, long updateDateMs, int pathColor, byte orbitalType, boolean selected)
    {
        long id = getSatelliteID(context, noradId);
        long saveId;
        ContentValues satelliteValues = getSatelliteValues(name, userName, noradId, ownerCode, launchDate, tleLine1, tleLine2, tleDateMs, gp, updateDateMs, pathColor, orbitalType, selected);
        saveId = runSave(context, id, Tables.Orbital, satelliteValues);

        //update any applicable widget
        WidgetPassBaseProvider.updateWidget(context, noradId);

        //return save ID
        return(saveId);
    }
    public static long saveSatellite(Context context, String name, int noradId, String ownerCode, long launchDate, String tleLine1, String tleLine2, long tleDateMs, String gp, long updateDateMs, byte orbitalType)
    {
        long id = getSatelliteID(context, noradId);
        return(saveSatellite(context, name, null, noradId, ownerCode, launchDate, tleLine1, tleLine2, tleDateMs, gp, updateDateMs, Color.DKGRAY, orbitalType, (id < 0 || getSatelliteSelected(context, id))));
    }
    public static void saveSatellite(Context context, int noradId, String userName, String ownerCode, long launchDate)
    {
        long id = getSatelliteID(context, noradId);
        saveSatellite(context, null, userName, noradId, ownerCode, launchDate, null, null, Globals.INVALID_DATE_MS, null, Globals.INVALID_DATE_MS, Integer.MAX_VALUE, Byte.MAX_VALUE, getSatelliteSelected(context, id));
    }
    public static void saveSatellite(Context context, int noradId, boolean selected)
    {
        saveSatellite(context, null, null, noradId, null, Globals.INVALID_DATE_MS, null, null, Globals.INVALID_DATE_MS, null, Globals.INVALID_DATE_MS, Integer.MAX_VALUE, Byte.MAX_VALUE, selected);
    }
    public static void saveSatellite(Context context, int noradId, int pathColor)
    {
        saveSatellite(context, null, null, noradId, null, Globals.INVALID_DATE_MS, null, null, Globals.INVALID_DATE_MS, null, Globals.INVALID_DATE_MS, pathColor, Byte.MAX_VALUE, true);
    }

    //Deletes a satellite
    public static boolean deleteSatellite(Context context, int noradId)
    {
        return(runDelete(context, Tables.Orbital, "[ID]='" + getSatelliteID(context, noradId) + "'"));
    }

    //Gets location name values
    private static ContentValues getLocationNameValues(double latitude, double longitude, String name)
    {
        ContentValues locationNameValues = new ContentValues(0);

        locationNameValues.put("[Latitude]", latitude);
        locationNameValues.put("[Longitude]", longitude);
        locationNameValues.put("[Name]", name);

        return(locationNameValues);
    }

    //Adds location name to database
    public static void addLocationName(Context context, double latitude, double longitude, String name)
    {
        runInsert(context, Tables.LocationName, getLocationNameValues(latitude, longitude, name));
    }

    //Gets time zone values
    private static ContentValues getTimeZoneValues(double latitude, double longitude, String zoneId)
    {
        ContentValues timezoneValues = new ContentValues(0);

        timezoneValues.put("[Latitude]", latitude);
        timezoneValues.put("[Longitude]", longitude);
        timezoneValues.put("[ZoneId]", zoneId);

        return(timezoneValues);
    }

    //Adds time zone to database
    public static void addTimeZone(Context context, double latitude, double longitude, String zoneId)
    {
        runInsert(context, Tables.TimeZone, getTimeZoneValues(latitude, longitude, zoneId));
    }

    //Gets altitude values
    private static ContentValues getAltitudeValues(double latitude, double longitude, double altitudeM)
    {
        ContentValues altitudeValues = new ContentValues(0);

        altitudeValues.put("[Latitude]", latitude);
        altitudeValues.put("[Longitude]", longitude);
        altitudeValues.put("[Altitude]", altitudeM);

        return(altitudeValues);
    }

    //Adds altitude to database
    public static void addAltitude(Context context, double latitude, double longitude, double altitudeM)
    {
        runInsert(context, Tables.Altitude, getAltitudeValues(latitude, longitude, altitudeM));
    }

    //Gets closest data within given area delta
    private static String getClosestData(Context context, String table, String column, double latitude, double longitude, double delta)
    {
        int index;
        double absDelta = Math.abs(delta);
        double currentLat;
        double currentLon;
        double currentDelta;
        double closestDelta = Double.MAX_VALUE;
        String closestData = null;
        String currentData;
        String minLat = Globals.getNumberString(latitude - absDelta, 5);
        String maxLat = Globals.getNumberString(latitude + absDelta, 5);
        String minLon = Globals.getNumberString(longitude - delta, 5);
        String maxLon = Globals.getNumberString(longitude + delta, 5);
        String[][] queryResult = runQuery(context, "SELECT [Latitude], [Longitude], " + column + " FROM " + table + " WHERE([Latitude] >= " + minLat + " AND [Latitude] <= " + maxLat + " AND [Longitude] >= " + minLon + " AND [Longitude] <= " + maxLon + ") ORDER BY [Latitude], [Longitude] ASC", null);

        //if there are results
        if(queryResult.length > 0)
        {
            //go through each location
            for(index = 0; index < queryResult.length; index++)
            {
                //get current lat, lon, and data
                currentLat = Double.parseDouble(queryResult[index][0]);
                currentLon = Double.parseDouble(queryResult[index][1]);
                currentData = queryResult[index][2];

                //get current combined delta
                currentDelta = Math.abs(latitude - currentLat) + Math.abs(longitude - currentLon);

                //if the smallest so far
                if(currentDelta < closestDelta)
                {
                    //update
                    closestDelta = currentDelta;
                    closestData = currentData;
                }
            }

            //return closest found
            return(closestData);
        }
        else
        {
            //not found
            return(null);
        }
    }

    //Gets closest known location name within given delta
    public static String getClosestLocationName(Context context, double latitude, double longitude, double delta)
    {
        return(getClosestData(context, Tables.LocationName, "[Name]", latitude, longitude, delta));
    }

    //Gets closest known time zone within given delta
    public static String getClosestTimeZone(Context context, double latitude, double longitude, double delta)
    {
        return(getClosestData(context, Tables.TimeZone, "[ZoneId]", latitude, longitude, delta));
    }

    //Gets closest known altitude within given delta
    public static double getClosestAltitude(Context context, double latitude, double longitude, double delta)
    {
        String dataString = getClosestData(context, Tables.Altitude, "[Altitude]", latitude, longitude, delta);
        return(dataString != null ? Double.parseDouble(dataString) : Double.MAX_VALUE);
    }

    //Gets location values
    private static ContentValues getLocationValues(String name, double latitude, double longitude, double altitudeKM, String zoneId, byte locationType, boolean checked, boolean useChecked)
    {
        ContentValues locationValues = new ContentValues(0);
        if(name != null)
        {
            locationValues.put("[Name]", name);
        }
        if(latitude != Double.MAX_VALUE)
        {
            locationValues.put("[Latitude]", latitude);
        }
        if(longitude != Double.MAX_VALUE)
        {
            locationValues.put("[Longitude]", longitude);
        }
        if(altitudeKM != Double.MAX_VALUE)
        {
            locationValues.put("[Altitude]", altitudeKM);
        }
        if(zoneId != null)
        {
            locationValues.put("[ZoneId]", zoneId);
        }
        if(locationType != Byte.MAX_VALUE)
        {
            locationValues.put("[Type]", locationType);
        }
        if(useChecked)
        {
            locationValues.put("[Selected]", (checked ? 1 : 0));
        }

        return(locationValues);
    }

    //Gets desired locations from the database
    public static DatabaseLocation[] getLocations(Context context, String sqlConditions)
    {
        int index;
        int index2;
        byte locationType;
        Resources res = (context != null ? context.getResources() : null);
        String name;
        String[][] queryResult = runQuery(context, "SELECT [ID], [Name], [Latitude], [Longitude], [Altitude], [ZoneId], [Type], [Selected] FROM " + Tables.Location + (sqlConditions != null ? (" WHERE " + sqlConditions) : "") + " ORDER BY [Type], [Name] ASC", null);
        ArrayList<DatabaseLocation> list = new ArrayList<>(0);

        //go through each location
        for(index = 0; index < queryResult.length; index++)
        {
            //if enough values
            if(queryResult[index].length >= 8)
            {
                try
                {
                    //get location type and name
                    locationType = Byte.parseByte(queryResult[index][6]);
                    name = (locationType == LocationType.Current && res != null ? res.getString(R.string.title_current) : queryResult[index][1]);

                    //normalize empty values
                    for(index2 = 0; index2 < queryResult[index].length; index2++)
                    {
                        //if empty
                        if(queryResult[index][index2] == null)
                        {
                            //handle based on column
                            switch(index2)
                            {
                                case 2:
                                case 3:
                                case 4:
                                    queryResult[index][index2] = "0";
                                    break;
                            }
                        }
                    }

                    //if first result is set
                    if(queryResult[index][0] != null)
                    {
                        //add to list
                        list.add(new DatabaseLocation(Integer.parseInt(queryResult[index][0]), name, Double.parseDouble(queryResult[index][2]), Double.parseDouble(queryResult[index][3]), Double.parseDouble(queryResult[index][4]), queryResult[index][5], locationType, queryResult[index][7].equals("1")));
                    }
                }
                catch(Exception ex)
                {
                    //do nothing
                }
            }
        }

        //return list as array
        return(list.toArray(new DatabaseLocation[0]));
    }
    public static DatabaseLocation[] getLocations(Context context)
    {
        return(getLocations(context, null));
    }

    //Gets the selected location form the database
    public static DatabaseLocation getSelectedLocation(Context context)
    {
        DatabaseLocation[] locations = getLocations(context,"[Selected]=1");

        if(locations.length > 0)
        {
            return(locations[0]);
        }
        else
        {
            return(new DatabaseLocation(0, context.getResources().getString(R.string.title_current), 0, 0 , 0, TimeZone.getDefault().getID(), LocationType.Current, true));
        }
    }

    //Gets a location ID by name
    private static long getLocationID(Context context, String name, byte locationType)
    {
        //run query
        String[][] queryResult = runQuery(context, "SELECT [ID] FROM " + Tables.Location + " WHERE [Name]=? " + (locationType == LocationType.Current ? (" OR [Type]=" + LocationType.Current + " ") : "") + "LIMIT 1", new String[]{name});
        return(queryResult.length > 0 ? Long.parseLong(queryResult[0][0]) : -1);
    }

    //Modifies location data
    private static void saveLocation(Context context, long id, String name, double latitude, double longitude, double altitudeM, String zoneId, byte locationType, boolean checked, boolean usedChecked)
    {
        ContentValues locationValues = getLocationValues(name, latitude, longitude, (altitudeM != Double.MAX_VALUE ? altitudeM / 1000 : Double.MAX_VALUE), zoneId, locationType, checked, usedChecked);
        ContentValues selectValue = new ContentValues();

        //if no ID
        if(id == Long.MAX_VALUE)
        {
            //get it
            id = getLocationID(context, name, locationType);
        }

        //if checking this location
        if(checked)
        {
            //deselect all locations
            selectValue.put("[Selected]", 0);
            runUpdate(context, Tables.Location, selectValue, null);
        }

        //save location
        runSave(context, id, Tables.Location, locationValues);
    }
    public static void saveLocation(Context context, long id, String name, double latitude, double longitude, double altitudeM, String zoneId, byte locationType)
    {
        saveLocation(context, id, name, latitude, longitude, altitudeM, zoneId, locationType, false, false);
    }
    public static void saveLocation(Context context, String name, double latitude, double longitude, double altitudeM, String zoneId, byte locationType, boolean checked)
    {
        saveLocation(context, Long.MAX_VALUE, name, latitude, longitude, altitudeM, zoneId, locationType, checked, true);
    }
    public static void saveLocation(Context context, String name, byte locationType, boolean checked)
    {
        saveLocation(context, Long.MAX_VALUE, name, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, null, locationType, checked, true);
    }

    //Deletes a location
    public static boolean deleteLocation(Context context, String name, byte locationType)
    {
        return(runDelete(context, Tables.Location, "[ID]='" + getLocationID(context, name, locationType) + "'"));
    }

	//Gets owner values
    private static ContentValues getOwnerValues(String code, String name)
    {
        ContentValues ownerValues = new ContentValues(0);
        if(code != null)
        {
            ownerValues.put("[Code]", code);
        }
        if(name != null)
        {
            ownerValues.put("[Name]", name);
        }

        return(ownerValues);
    }

    //Gets desired owners in English from the database for the given norad ID
    public static String[][] getOwnersEnglish(Context context, int noradId)
    {
        return(runQuery(context, "SELECT [Code], " + Tables.Owner + ".[Name] FROM " + Tables.Owner + " JOIN " + Tables.Orbital + " ON " + Tables.Owner + ".[Code]=" + Tables.Orbital + ".[Owner_Code] WHERE [Norad]=" + noradId + " ORDER BY " + Tables.Owner + ".[Name]", null));
    }

    //Gets owners from the database in the current locale
    public static ArrayList<UpdateService.MasterOwner> getOwners(Context context)
    {
        int index;
        String code;
        String localeName;
        //String[][] queryResult = runQuery(context, "SELECT [Code], [Name] FROM " + Tables.Owner + " ORDER BY [Name]", null);
        String[][] queryResult = runQuery(context, "SELECT [Code], [Name] FROM " + Tables.Owner + " ORDER BY [Code]", null);
        ArrayList<UpdateService.MasterOwner> list = new ArrayList<>(0);

        //go through each owner
        for(index = 0; index < queryResult.length; index++)
        {
            //try to get code and locale name
            code = queryResult[index][0];
            localeName = LocaleOwner.getName(context, code);

            //add to list
            list.add(new UpdateService.MasterOwner(code, (localeName != null ? localeName : queryResult[index][1])));
        }

        //sort and return list
        Collections.sort(list);
        return(list);
    }

    //Saves given owners
    public static void saveOwners(Context context, ArrayList<UpdateService.MasterOwner> owners, Globals.OnProgressChangedListener listener)
    {
        int index;
        int count = owners.size();
        SQLiteDatabase db = DatabaseManager.get(context, true);
        SQLiteStatement sqlStatement;
        UpdateService.MasterOwner currentOwner;

        //start transaction
        db.beginTransaction();
        sqlStatement = db.compileStatement("REPLACE INTO " + Tables.Owner + " ([Code], [Name]) VALUES(?, ?)");

        //go through each owner
        for(index = 0; index < count; index++)
        {
            //get current owner values
            currentOwner = owners.get(index);
            ContentValues satelliteValues = getOwnerValues(currentOwner.code, currentOwner.name);

            try
            {
                //save satellite
                sqlStatement.bindString(1, satelliteValues.getAsString("[Code]"));
                sqlStatement.bindString(2, satelliteValues.getAsString("[Name]"));
                sqlStatement.execute();

                //if listener exists
                if(listener != null)
                {
                    //update progress
                    listener.onProgressChanged(Globals.ProgressType.Success, null, index, count);
                }
            }
            catch(Exception ex)
            {
                //do nothing
            }
        }

        //finish transaction
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    //Gets category values
    private static ContentValues getCategoryValues(String name, int index)
    {
        ContentValues ownerValues = new ContentValues(0);
        if(name != null)
        {
            ownerValues.put("[Name]", name);
        }
        if(index != Integer.MAX_VALUE)
        {
            ownerValues.put("[Indx]", index);
        }

        return(ownerValues);
    }

    //Gets desired categories from the database
    public static ArrayList<UpdateService.MasterCategory> getCategories(Context context)
    {
        int index;
        Resources res = context.getResources();
        String[][] queryResult = runQuery(context, "SELECT [Name], [Indx] FROM " + Tables.Category + "ORDER BY [Name], [Indx]", null);
        ArrayList<UpdateService.MasterCategory> list = new ArrayList<>(0);

        //go through each category
        for(index = 0; index < queryResult.length; index++)
        {
            //get category and add if not in list
            UpdateService.MasterCategory category = new UpdateService.MasterCategory(LocaleCategory.getCategory(res, queryResult[index][0]), Integer.parseInt(queryResult[index][1]));
            if(!list.contains(category))
            {
                //add to list
                list.add(category);
            }
        }

        //sort and return list
        Collections.sort(list);
        return(list);
    }

    //Saves given categories
    public static void saveCategories(Context context, ArrayList<UpdateService.MasterCategory> categories, Globals.OnProgressChangedListener listener)
    {
        int index;
        int count = categories.size();
        SQLiteDatabase db = DatabaseManager.get(context, true);
        SQLiteStatement sqlStatement;

        //start transaction
        db.beginTransaction();
        sqlStatement = db.compileStatement("REPLACE INTO " + Tables.Category + " ([Name], [Indx]) VALUES(?, ?)");

        //go through each category
        for(index = 0; index < count; index++)
        {
            //get current satellite values
            UpdateService.MasterCategory currentCategory = categories.get(index);
            ContentValues categoryValues = getCategoryValues(currentCategory.name, currentCategory.index);

            try
            {
                //save satellite
                sqlStatement.bindString(1, categoryValues.getAsString("[Name]"));
                sqlStatement.bindLong(2, categoryValues.getAsLong("[Indx]"));
                sqlStatement.execute();

                //if listener exists
                if(listener != null)
                {
                    //update progress
                    listener.onProgressChanged(Globals.ProgressType.Success, null, index, count);
                }
            }
            catch(Exception ex)
            {
                //do nothing
            }
        }

        //finish transaction
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    //Gets satellite category values
    private static ContentValues getSatelliteCategoryValues(int noradId, int categoryIndex)
    {
        ContentValues satCatValues = new ContentValues(0);
        if(noradId != Integer.MAX_VALUE)
        {
            satCatValues.put("[Norad]", noradId);
        }
        if(categoryIndex != Integer.MAX_VALUE)
        {
            satCatValues.put("[Category_Index]", categoryIndex);
        }

        return(satCatValues);
    }

    //Gets master satellite categories from the database in English
    public static String[][] getSatelliteCategoriesEnglish(Context context, int noradId, boolean getIndex)
    {
        return(runQuery(context, "SELECT DISTINCT '" + noradId + "', [Name]" + (getIndex ? ", [Indx]" : "") + " FROM " + Tables.Category + " JOIN " + Tables.SatelliteCategory + " ON " + Tables.Category + ".[Indx]=" + Tables.SatelliteCategory + ".[Category_Index] WHERE " + Tables.SatelliteCategory + ".[Norad]=" + noradId, null));
    }
    public static String[][] getSatelliteCategoriesEnglish(Context context, int noradId)
    {
        return(getSatelliteCategoriesEnglish(context, noradId, false));
    }
    public static ArrayList<UpdateService.MasterSatelliteCategory> getSatelliteCategoriesEnglish(Context context)
    {
        int index;
        String[][] queryResult = runQuery(context, "SELECT [Norad], [Category_Index] FROM " + Tables.SatelliteCategory + " ORDER BY [Norad], [Category_Index] ASC", null);
        ArrayList<UpdateService.MasterSatelliteCategory> list = new ArrayList<>(0);

        //go through each item
        for(index = 0; index < queryResult.length; index++)
        {
            //add to list
            list.add(new UpdateService.MasterSatelliteCategory(Integer.parseInt(queryResult[index][0]), Integer.parseInt(queryResult[index][1])));
        }

        //return list
        return(list);
    }

    //Saves given satellite categories
    public static void saveSatelliteCategories(Context context, ArrayList<UpdateService.MasterSatelliteCategory> satelliteCategories, Globals.OnProgressChangedListener listener)
    {
        int index;
        int count = satelliteCategories.size();
        SQLiteDatabase db = DatabaseManager.get(context, true);
        SQLiteStatement sqlStatement;

        //start transaction
        db.beginTransaction();
        sqlStatement = db.compileStatement("REPLACE INTO " + Tables.SatelliteCategory + " ([Norad], [Category_Index]) VALUES(?, ?)");

        //go through each owner
        for(index = 0; index < count; index++)
        {
            //get current item norad and index values
            UpdateService.MasterSatelliteCategory currentItem = satelliteCategories.get(index);
            ContentValues satelliteValues = getSatelliteCategoryValues(currentItem.noradId, currentItem.categoryIndex);

            try
            {
                //save satellite
                sqlStatement.bindLong(1, satelliteValues.getAsInteger("[Norad]"));
                sqlStatement.bindLong(2, satelliteValues.getAsInteger("[Category_Index]"));
                sqlStatement.execute();

                //if listener exists
                if(listener != null)
                {
                    //update progress
                    listener.onProgressChanged(Globals.ProgressType.Success, null, index, count);
                }
            }
            catch(Exception ex)
            {
                //do nothing
            }
        }

        //finish transaction
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    //Gets information values
    private static ContentValues getInformationValues(int noradId, int updateSource, String language, String info)
    {
        ContentValues infoValues = new ContentValues(0);
        if(noradId != Integer.MAX_VALUE)
        {
            infoValues.put("[Norad]", noradId);
        }
        if(updateSource != Integer.MAX_VALUE)
        {
            infoValues.put("[Source]", updateSource);
        }
        if(language != null)
        {
            infoValues.put("[Language]", language);
        }
        if(info != null)
        {
            infoValues.put("[Info]", info);
        }

        return(infoValues);
    }

    //Saves given information
    public static void saveInformation(Context context, int noradId, int updateSource, String language, String info)
    {
        SQLiteDatabase db = DatabaseManager.get(context, true);
        SQLiteStatement sqlStatement;
        ContentValues infoValues = getInformationValues(noradId, updateSource, language, info);

        //start transaction
        db.beginTransaction();
        sqlStatement = db.compileStatement("REPLACE INTO " + Tables.Information + " ([Norad], [Source], [Language], [Info]) VALUES(?, ?, ?, ?)");

        try
        {
            //save information
            sqlStatement.bindLong(1, infoValues.getAsInteger("[Norad]"));
            sqlStatement.bindLong(2, infoValues.getAsInteger("[Source]"));
            sqlStatement.bindString(3, infoValues.getAsString("[Language]"));
            sqlStatement.bindString(4, infoValues.getAsString("[Info]"));
            sqlStatement.execute();
        }
        catch(Exception ex)
        {
            //do nothing
        }

        //finish transaction
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    //Gets information from the database
    public static String getInformation(Context context, int noradId, int updateSource, String language)
    {
        String info;
        String localeInfo;
        String[][] queryResult = runQuery(context, "SELECT DISTINCT [Info] FROM " + Tables.Information + " WHERE " + Tables.Information + ".[Norad]=" + noradId + " AND " + Tables.Information + ".[Language]='" + language + "' AND (" + Tables.Information + ".[Source]='" + updateSource + "' OR " + Tables.Information + ".[Source]='" + UpdateSource.SpaceDotCom + "')", null);

        //return any information
        info = (queryResult.length > 0 ? queryResult[0][0] : null);
        localeInfo = LocaleInformation.getInformation(context, noradId);
        return(localeInfo != null ? localeInfo : info);
    }
    public static String[][] getInformation(Context context, int noradId)
    {
        return(runQuery(context, "SELECT DISTINCT [Norad], [Info], [Language], [Source] FROM " + Tables.Information + " WHERE " + Tables.Information + ".[Norad]=" + noradId, null));
    }

    //Gets master satellite values
    private static ContentValues getMasterSatelliteValues(UpdateService.MasterSatellite satellite)
    {
        ContentValues masterValues = new ContentValues(0);

        masterValues.put("[Norad]", satellite.noradId);
        if(satellite.name != null)
        {
            masterValues.put("[Name]", satellite.name);
        }
        masterValues.put("[Owner_Code]", satellite.ownerCode);
        masterValues.put("[Launch_Date]", satellite.launchDateMs);

        return(masterValues);
    }

    //Gets master satellites from the database
    public static ArrayList<UpdateService.MasterSatellite> getMasterSatellites(Context context)
    {
        int index;
        int currentID;
        String[][] queryResult = runQuery(context, "SELECT " + Tables.MasterSatellite + ".[Norad], " + Tables.MasterSatellite + ".[Name], [Owner_Code], " + Tables.Owner + ".[Name], " + Tables.MasterSatellite + ".[Launch_Date], " + Tables.SatelliteCategory + ".[Category_Index], " + Tables.Category + ".[Name] FROM " + Tables.MasterSatellite + " LEFT JOIN " + Tables.Owner + " ON " + Tables.MasterSatellite + ".[Owner_Code]=" + Tables.Owner + ".[Code] LEFT JOIN " + Tables.SatelliteCategory + " ON " + Tables.MasterSatellite + ".[Norad]=" + Tables.SatelliteCategory + ".[Norad] LEFT JOIN " + Tables.Category + " ON [Category_Index]=[Indx] ORDER BY " + Tables.MasterSatellite + ".[Norad], [Category_Index]", null);
        ArrayList<UpdateService.MasterSatellite> list = new ArrayList<>(0);

        //go through each satellite
        for(index = 0; index < queryResult.length; index++)
        {
            //remember current norad ID and satellite type
            currentID = Integer.parseInt(queryResult[index][0]);

            //create new satellite
            UpdateService.MasterSatellite newSatellite = new UpdateService.MasterSatellite(currentID, queryResult[index][1], queryResult[index][2], queryResult[index][3], Long.parseLong(queryResult[index][4]));

            //add all categories
            do
            {
                //if there is a category
                if(queryResult[index][5] != null)
                {
                    newSatellite.categoryIndexes.add(Integer.valueOf(queryResult[index][5]));
                    newSatellite.categories.add(queryResult[index][6]);
                }

                //while there is a next and it has the same norad ID
                index++;
            } while(index < queryResult.length && currentID == Integer.parseInt(queryResult[index][0]));
            index--;

            //add to list
            list.add(newSatellite);
        }

        //return list
        return(list);
    }

    //Saves given master satellites
    public static void saveMasterSatellites(Context context, ArrayList<UpdateService.MasterSatellite> satellites, Globals.OnProgressChangedListener listener)
    {
        int index;
        int count = satellites.size();
        SQLiteDatabase db = DatabaseManager.get(context, true);
        SQLiteStatement sqlStatement;

        //start transaction
        db.beginTransaction();
        sqlStatement = db.compileStatement("REPLACE INTO " + Tables.MasterSatellite + " ([Norad], [Name], [Owner_Code], [Launch_Date]) VALUES(?, ?, ?, ?)");

        //go through each satellite
        for(index = 0; index < count; index++)
        {
            //get current satellite values
            ContentValues satelliteValues = getMasterSatelliteValues(satellites.get(index));

            try
            {
                //save satellite
                sqlStatement.bindLong(1, satelliteValues.getAsInteger("[Norad]"));
                sqlStatement.bindString(2, satelliteValues.getAsString("[Name]"));
                sqlStatement.bindString(3, satelliteValues.getAsString("[Owner_Code]"));
                sqlStatement.bindString(4, satelliteValues.getAsString("[Launch_Date]"));
                sqlStatement.execute();

                //if listener exists
                if(listener != null)
                {
                    //update progress
                    listener.onProgressChanged(Globals.ProgressType.Success, null, index, count);
                }
            }
            catch(Exception ex)
            {
                //do nothing
            }
        }

        //finish transaction
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    //Clears master satellite table
    public static void clearMasterSatelliteTable(Context context)
    {
        UpdateService.setMasterListTime(context, "", 0);
        runDelete(context, Tables.MasterSatellite, null);
    }
}