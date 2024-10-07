package com.nikolaiapps.orbtrack;


import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Color;
import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.TimeZone;


public class Database extends SQLiteOpenHelper
{
    static abstract class Tables
    {
        static final String Orbital = "[Orbital]";
        static final String Star = "[Star]";
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

    static abstract class IndexTables
    {
        static final String Orbital = "[Orbital_Index]";
        static final String Star = "[Star_Index]";
        static final String Location = "[Location_Index]";
        static final String LocationName = "[LocationName_Index]";
        static final String TimeZone = "[TimeZone_Index]";
        static final String Altitude = "[Altitude_Index]";
        static final String MasterSatellite = "[MasterSatellite_Index]";
        static final String Owner = "[Owner_Index]";
        static final String Category = "[Category_Index]";
        static final String SatelliteCategory = "[SatelliteCategory_Index]";
        static final String Information = "[Information_Index]";
    }

    public static abstract class OrbitalType
    {
        static final byte Star = 1;
        static final byte Planet = 2;
        static final byte Satellite = 3;
        static final byte RocketBody = 4;
        static final byte Debris = 5;
        static final byte Constellation = 6;
        static final byte Sun = 7;
        static final byte TypeCount = 7;
    }

    static abstract class UpdateSource
    {
        static final byte Celestrak = 0;
        static final byte N2YO = 1;
        static final byte SpaceTrack = 2;
        static final byte NASA = 3;
        static final byte HeavensAbove = 4;
        static final byte TheSkyLive = 126;
        static final byte SpaceDotCom = 127;
    }

    private static abstract class SQLBindType
    {
        static final byte String = 0;
        static final byte Double = 1;
        static final byte None = Byte.MAX_VALUE;
    }

    public static abstract class LanguageIndex
    {
        static final byte English = 0;
        static final byte Spanish = 1;
        static final byte LanguageCount = 2;
    }

    static abstract class QueryId
    {
        static final int GetOrbitals = 0;
        static final int GetOrbitalSatelliteType = 1;
        static final int GetOwnersEnglish = 2;
        static final int GetSatelliteCategoriesEnglishNorad = 3;
        static final int GetSatelliteCategoriesEnglish = 4;
        static final int GetInformationNoradLanguageSource = 5;
        static final int GetInformationNorad = 6;
        static final int GetMasterSatellites = 7;
        static final int GetClosestLocationName = 8;
        static final int GetClosestTimeZone = 9;
        static final int GetClosestAltitude = 10;
        static final int GetLocations = 11;
        static final int GetLocationsSelected = 12;
        static final int GetLocationsId = 13;
        static final int GetLocationsType = 14;
        static final int GetOwners = 15;
        static final int GetCategories = 16;
        static final int GetSatelliteData = 17;
        static final int GetLocationIdName = 18;
        static final int GetLocationIdNameType = 19;
        static final int GetFirstOrbitalType = 20;
        static final int GetFirstOrbitalNorad = 21;
        static final int GetFirstStar = 22;
        static final int GetFirstLocation = 23;
        static final int GetFirstCategory = 24;
        static final int GetFirstInformation = 25;
        static final int GetFirstOwner = 26;
        static final int GetFirstTimeZone = 27;
    }

    private static abstract class TLELines
    {
        static final String ISSZarya1 = "1 25544U 98067A   24214.54091714  .00019661  00000-0  35748-3 0  9997";
        static final String ISSZarya2 = "2 25544  51.6390  90.6319 0006148 150.4340 280.6300 15.49514077465580";
        static final long ISSZaryaDate = 1722551885700L;
    }

    private static final int ISS_ZARYA_NORAD_ID = 25544;

    private static final int STARS_FILE_MIN_COLUMNS = 2;
    private static final int STARS_FILE_FULL_COLUMNS = 6;
    private static final int STARS_FILE_ROWS = 684;
    private static final String STARS_FILE_SEPARATOR = "[|]";

    public static abstract class LocaleStars
    {
        private static final ArrayList<Integer> noradId = new ArrayList<>(0);
        private static final ArrayList<ArrayList<String>> starNames = new ArrayList<>(LanguageIndex.LanguageCount);

        //Initializes data
        public static void initData(Context context)
        {
            int fileId;
            int languageIndex;
            String line;
            BufferedReader file;
            Resources res = context.getResources();

            //clear any existing
            noradId.clear();
            starNames.clear();

             //go through each stars file
            for(languageIndex = 0; languageIndex < LanguageIndex.LanguageCount; languageIndex++)
            {
                ArrayList<String> nameList = new ArrayList<>(0);

                //open stars file
                switch(languageIndex)
                {
                    case LanguageIndex.Spanish:
                        fileId = R.raw.stars_es;
                        break;

                    case LanguageIndex.English:
                    default:
                        fileId = R.raw.stars_en;
                        break;
                }
                file = new BufferedReader(new InputStreamReader(res.openRawResource(fileId)));
                try
                {
                    //while there are lines to read
                    while((line = file.readLine()) != null)
                    {
                        //split columns
                        String[] columns = line.split(STARS_FILE_SEPARATOR);

                        //if have at least minimum columns
                        if(columns.length >= STARS_FILE_MIN_COLUMNS)
                        {
                            //if on the first language
                            if(languageIndex == 0)
                            {
                                //add ID
                                noradId.add(Integer.valueOf(columns[0]));
                            }

                            //add name
                            nameList.add(columns[1]);
                        }
                    }
                    //close file
                    file.close();
                }
                catch(IOException ex)
                {
                    //do nothing
                }

                //add names
                starNames.add(nameList);
            }
        }

        //Gets the name for the given language
        private static String getName(int id, byte languageIndex)
        {
            int index;
            String name = null;

            //if ID is in list and valid indexes
            index = noradId.indexOf(id);
            if(index >= 0 && languageIndex < starNames.size() && index < starNames.get(languageIndex).size())
            {
                //get name for ID and language
                name = starNames.get(languageIndex).get(index);

                //if unknown, not English, and can get English name
                if(name.equals("?") && languageIndex != LanguageIndex.English && LanguageIndex.English < starNames.size() && index < starNames.get(LanguageIndex.English).size())
                {
                    //default to English
                    name = starNames.get(LanguageIndex.English).get(index);
                }
            }

            //return name
            return(name);
        }
        public static String getName(Context context, int id)
        {
            return(getName(id, Globals.getLanguageIndex(context)));
        }
        public static String getEnglishName(int id)
        {
            return(getName(id, LanguageIndex.English));
        }

        //Get norad IDs
        public static ArrayList<Integer> getNoradIds()
        {
            return(noradId);
        }

        //Returns if have data
        public static boolean haveData()
        {
            return(!noradId.isEmpty());
        }
    }

    private static final int CONSTELLATION_FILE_MIN_COLUMNS = 2;
    private static final int CONSTELLATION_FILE_FULL_COLUMNS = 5;
    private static final int CONSTELLATION_FILE_ROWS = 88;
    private static final String CONSTELLATION_FILE_SEPARATOR = "[|]";
    private static final String CONSTELLATION_FILE_PAIR_SEPARATOR = ":";
    private static final String CONSTELLATION_FILE_POINT_SEPARATOR = ",";

    public static abstract class LocaleConstellations
    {
        private static final ArrayList<Integer> noradId = new ArrayList<>(0);
        private static final ArrayList<ArrayList<String>> constellationNames = new ArrayList<>(LanguageIndex.LanguageCount);

        //Initializes data
        public static void initData(Context context)
        {
            int fileId;
            int languageIndex;
            String line;
            BufferedReader file;
            Resources res = context.getResources();

            //clear any existing
            noradId.clear();
            constellationNames.clear();

            //go through each constellation file
            for(languageIndex = 0; languageIndex < LanguageIndex.LanguageCount; languageIndex++)
            {
                ArrayList<String> nameList = new ArrayList<>(0);

                //open constellation file
                switch(languageIndex)
                {
                    case LanguageIndex.Spanish:
                        fileId = R.raw.constellation_es;
                        break;

                    case LanguageIndex.English:
                    default:
                        fileId = R.raw.constellation_en;
                        break;
                }
                file = new BufferedReader(new InputStreamReader(res.openRawResource(fileId)));
                try
                {
                    //while there are lines to read
                    while((line = file.readLine()) != null)
                    {
                        //split columns
                        String[] columns = line.split(CONSTELLATION_FILE_SEPARATOR);

                        //if have at least minimum columns
                        if(columns.length >= CONSTELLATION_FILE_MIN_COLUMNS)
                        {
                            //if on the first language
                            if(languageIndex == 0)
                            {
                                //add ID
                                noradId.add(Integer.valueOf(columns[0]));
                            }

                            //add name
                            nameList.add(columns[1]);
                        }
                    }
                    //close file
                    file.close();
                }
                catch(IOException ex)
                {
                    //do nothing
                }

                //add names
                constellationNames.add(nameList);
            }
        }

        //Gets the name for the given language
        private static String getName(int id, byte languageIndex)
        {
            int index;
            String name = null;

            //if ID is in list
            index = noradId.indexOf(id);
            if(index >= 0)
            {
                //get name for ID and language
                name = constellationNames.get(languageIndex).get(index);

                //if unknown and not English
                if(name.equals("?") && languageIndex != LanguageIndex.English)
                {
                    //default to English
                    name = constellationNames.get(LanguageIndex.English).get(index);
                }
            }

            //return name
            return(name);
        }
        public static String getName(Context context, int id)
        {
            return(getName(id, Globals.getLanguageIndex(context)));
        }
        public static String getEnglishName(int id)
        {
            return(getName(id, LanguageIndex.English));
        }

        //Get norad IDs
        public static ArrayList<Integer> getNoradIds()
        {
            return(noradId);
        }

        //Returns if have data
        public static boolean haveData()
        {
            return(!noradId.isEmpty());
        }
    }

    private static final int INFO_FILE_COLUMNS = 3;
    private static final int INFO_FILE_ROWS = 780;
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

                    case LanguageIndex.English:
                    default:
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
                            infoList.add(columns[2]);
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
            String info = null;

            //if ID is in list
            index = noradId.indexOf(id);
            if(index >= 0)
            {
                //get info for ID and language
                info = languageInfo.get(Globals.getLanguageIndex(context)).get(index);
            }

            //return information
            return(info);
        }

        //Returns if have data
        public static boolean haveData()
        {
            return(!noradId.isEmpty());
        }
    }

    private static final int OWNERS_FILE_COLUMNS = 2;
    private static final int OWNERS_FILE_ROWS = 133;
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

                    case LanguageIndex.English:
                    default:
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
                languageIndex = Globals.getLanguageIndex(context);

                //get info for code and language
                name = languageName.get(languageIndex).get(index);

                //if unknown and not English
                if((name == null || name.equals("?")) && languageIndex != LanguageIndex.English)
                {
                    //default to English
                    name = languageName.get(LanguageIndex.English).get(index);
                }
            }

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
            return(!codeName.isEmpty());
        }
    }

    @SuppressWarnings("SpellCheckingInspection")
    public static abstract class LocaleCategory
    {
        //Gets category locale
        public static String getCategory(Resources res, String name)
        {
            int stringId = -1;
            
            switch(name.toLowerCase())
            {
                case "100 (or so) brightest":
                    stringId = R.string.category_100_brightest;
                    break;

                case "active satellites":
                    stringId = R.string.category_active_satellites;
                    break;

                case "active geosynchronous":
                    stringId = R.string.category_active_geosynchronous;
                    break;

                case "amateur":
                    stringId = R.string.category_amateur;
                    break;

                case "amateur radio":
                    stringId = R.string.category_amateur_radio;
                    break;

                case "analyst":
                case "analyst satellites":
                    stringId = R.string.category_analyst;
                    break;

                case "argos data collection system":
                    stringId = R.string.category_argos_data_collection_system;
                    break;

                case "beidou":
                case "beidou navigation system":
                    stringId = R.string.category_beidou_navigation_system;
                    break;

                case "breeze-m r/b breakup":
                    stringId = R.string.category_breeze_m_rb_breakup;
                    break;

                case "brightest":
                    stringId = R.string.category_brightest;
                    break;

                case "bright geo":
                    stringId = R.string.category_bright_geo;
                    break;

                case "chinese asat test debris (fengyun 1c)":
                    stringId = R.string.category_chinese_asat_test_debris;
                    break;

                case "cosmos 2251 debris":
                    stringId = R.string.category_cosmos_2251_debris;
                    break;

                case "cubesats":
                    stringId = R.string.category_cubesats;
                    break;

                case "disaster monitoring":
                    stringId = R.string.category_disaster_monitoring;
                    break;

                case "earth resources":
                    stringId = R.string.category_earth_resources;
                    break;

                case "education":
                    stringId = R.string.category_education;
                    break;

                case "engineering":
                    stringId = R.string.category_engineering;
                    break;

                case "experimental":
                case "experimental comm":
                    stringId = R.string.category_experimental;
                    break;

                case "fengyun 1c debris":
                    stringId = R.string.category_fengyun_1c_debris;
                    break;

                case "galileo":
                    stringId = R.string.category_galileo;
                    break;

                case "geodetic":
                    stringId = R.string.category_geodetic;
                    break;

                case "geo protected zone":
                    stringId = R.string.category_geo_protected_zone;
                    break;

                case "geo protected zone plus":
                    stringId = R.string.category_geo_protected_zone_plus;
                    break;

                case "geostationary":
                    stringId = R.string.category_geostationary;
                    break;

                case "geosynchronous":
                    stringId = R.string.category_geosynchronous;
                    break;

                case "globalstar":
                    stringId = R.string.category_globalstar;
                    break;

                case "global positioning system (gps)":
                    stringId = R.string.category_global_positioning_system;
                    break;

                case "glonass operational":
                    stringId = R.string.category_glonass_operational;
                    break;

                case "goes":
                    stringId = R.string.category_goes;
                    break;

                case "gorizont":
                    stringId = R.string.category_gorizont;
                    break;

                case "gnss":
                    stringId = R.string.category_gnss;
                    break;

                case "gps operational":
                    stringId = R.string.category_gps_operational;
                    break;

                case "human spaceflight":
                    stringId = R.string.category_human_spaceflight;
                    break;

                case "indian asat test debris (microsat-r)":
                    stringId = R.string.category_indian_asat_test_debris;
                    break;

                case "inmarsat":
                    stringId = R.string.category_inmarsat;
                    break;

                case "intelsat":
                    stringId = R.string.category_intelsat;
                    break;

                case "iridium":
                    stringId = R.string.category_iridium;
                    break;

                case "iridium 33 debris":
                    stringId = R.string.category_iridium_33_debris;
                    break;

                case "iridium next":
                    stringId = R.string.category_iridium_next;
                    break;

                case "last 30 days' launches":
                    stringId = R.string.category_last_30_days;
                    break;

                case "military":
                case "miscellaneous military":
                    stringId = R.string.category_military;
                    break;

                case "molniya":
                    stringId = R.string.category_molniya;
                    break;

                case "navigation":
                    stringId = R.string.category_navigation;
                    break;

                case "navy navigation satellite system":
                case "navy navigation satellite system (nnss)":
                    stringId = R.string.category_navy_navigation_satellite_system;
                    break;

                case "noaa":
                    stringId = R.string.category_noaa;
                    break;

                case "o3b networks":
                    stringId = R.string.category_o3b_networks;
                    break;

                case "oneweb":
                    stringId = R.string.category_oneweb;
                    break;

                case "orbcomm":
                    stringId = R.string.category_orbcomm;
                    break;

                case "other":
                case "other comm":
                case "other satellites":
                    stringId = R.string.category_other;
                    break;

                case "planet":
                    stringId = R.string.category_planet;
                    break;

                case "radar calibration":
                    stringId = R.string.category_radar_calibration;
                    break;

                case "raduga":
                    stringId = R.string.category_raduga;
                    break;

                case "russian asat test debris (cosmos 1408)":
                    stringId = R.string.category_russian_asat_test_debris;
                    break;

                case "russian leo navigation":
                    stringId = R.string.category_russian_leo_navigation;
                    break;

                case "satellite-based augmentation system":
                case "satellite-based augmentation system (waas/egnos/msas)":
                    stringId = R.string.category_satellite_based_augmentation_system;
                    break;

                case "satnogs":
                    stringId = R.string.category_satnogs;
                    break;

                case "search & rescue":
                case "search & rescue (sarsat)":
                    stringId = R.string.category_search_and_rescue;
                    break;

                case "ses":
                    stringId = R.string.category_ses;
                    break;

                case "space & earth science":
                    stringId = R.string.category_space_and_earth_science;
                    break;

                case "space stations":
                    stringId = R.string.category_space_stations;
                    break;

                case "special interest":
                    stringId = R.string.category_special_interest;
                    break;

                case "spire":
                    stringId = R.string.category_spire;
                    break;

                case "starlink":
                    stringId = R.string.category_starlink;
                    break;

                case "swarm":
                    stringId = R.string.category_swarm;
                    break;

                case "tracking and data relay satellite system":
                case "tracking and data relay satellite system (tdrss)":
                    stringId = R.string.category_tracking_and_data_relay_satellite_system;
                    break;

                case "tv":
                    stringId = R.string.category_tv;
                    break;

                case "visible":
                    stringId = R.string.category_visible;
                    break;

                case "weather":
                    stringId = R.string.category_weather;
                    break;

                case "xm and sirius":
                    stringId = R.string.category_xm_and_sirius;
                    break;
            }
            
            //if string ID is set
            if(stringId > 0)
            {
                //get name
                name = res.getString(stringId);
            }

            //return name
            return(name.toUpperCase());
        }
    }

    private static abstract class OrbitalsBuffer
    {
        private static boolean needReload = false;
        private static int[] typeCount = null;
        private static DatabaseSatellite[] buffer = null;

        //Add parent ID to the orbital with child ID and return shortest child distance
        private static double addParentIdGetDistance(Context context, int parentId, int childId, ArrayList<Integer> usedIds)
        {
            double shortestLightYears = Double.MAX_VALUE;

            //if child ID not already used
            if(!usedIds.contains(childId))
            {
                //if child orbital exists
                DatabaseSatellite currentChild = getOrbital(context, childId, false);
                if(currentChild != null)
                {
                    //add parent ID to child orbital
                    if(currentChild.parentProperties == null)
                    {
                        //initialize list
                        currentChild.parentProperties = new ArrayList<>(0);
                    }
                    currentChild.parentProperties.add(new ParentProperties(parentId, -1));

                    //if the shortest distance so far
                    if(currentChild.distanceLightYears < shortestLightYears)
                    {
                        //update shortest
                        shortestLightYears = currentChild.distanceLightYears;
                    }
                }

                //add to used IDs
                usedIds.add(childId);
            }

            //return shortest child light years distance
            return(shortestLightYears);
        }

        //Load orbitals from database into buffers
        private static void load(Context context)
        {
            //get all orbitals and reset status
            buffer = Database.getOrbitals(context, false);
            needReload = false;
            typeCount = null;

            //go through each orbital
            for(DatabaseSatellite currentOrbital : buffer)
            {
                //remember current ID and lines
                int currentId = currentOrbital.noradId;
                IdLine[] currentLines = currentOrbital.lines;

                //if on a constellation and there are lines
                if(currentOrbital.orbitalType == OrbitalType.Constellation && currentLines != null)
                {
                    double shortestLightYears = Double.MAX_VALUE;
                    ArrayList<Integer> usedIds = new ArrayList<>(0);

                    //go through each line
                    for(IdLine currentLine : currentLines)
                    {
                        //add as parent to IDs and get distances
                        double startLightYears = addParentIdGetDistance(context, currentId, currentLine.startId, usedIds);
                        double endLightYears = addParentIdGetDistance(context, currentId, currentLine.endId, usedIds);
                        double shorterLightYears = Double.min(startLightYears, endLightYears);

                        //if the shortest distance so far
                        if(shorterLightYears < shortestLightYears)
                        {
                            //update shortest light years
                            shortestLightYears = shorterLightYears;
                        }
                    }

                    //set shortest light years
                    currentOrbital.distanceLightYears = shortestLightYears;
                }
            }
        }

        //Sets that orbitals need to be reloaded
        public static void setNeedReload()
        {
            //reload on next use
            needReload = true;
        }

        //Handles load if needed
        private static void handleLoad(Context context)
        {
            //if need to load orbitals
            if(needReload || buffer == null)
            {
                //load orbitals
                load(context);
            }
        }

        //Get all orbitals
        public static DatabaseSatellite[] getAll(Context context)
        {
            //load if needed
            handleLoad(context);

            //return all orbitals
            return(buffer);
        }

        //Get selected orbitals
        public static DatabaseSatellite[] getSelected(Context context)
        {
            ArrayList<DatabaseSatellite> selectedOrbitals;

            //load if needed
            handleLoad(context);

            //get selected orbitals
            selectedOrbitals = new ArrayList<>(buffer.length);
            for(DatabaseSatellite currentOrbital : buffer)
            {
                //if selected
                if(currentOrbital.isSelected)
                {
                    //add to list
                    selectedOrbitals.add(currentOrbital);
                }
            }

            //return list
            return(selectedOrbitals.toArray(new DatabaseSatellite[0]));
        }

        //Gets all satellites
        public static DatabaseSatellite[] getOrbitals(Context context, byte ...orbitalTypes)
        {
            ArrayList<DatabaseSatellite> orbitals = null;

            //if there are types to get
            if(orbitalTypes != null && orbitalTypes.length > 0)
            {
                //load if needed
                handleLoad(context);

                //setup list
                orbitals = new ArrayList<>(buffer.length);

                //go through each orbital
                for(DatabaseSatellite currentOrbital : buffer)
                {
                    //remember current orbital type
                    byte orbitalType = currentOrbital.orbitalType;

                    //go through each type
                    for(byte currentType : orbitalTypes)
                    {
                        //if a desired type
                        if(orbitalType == currentType)
                        {
                            //add to the list and stop searching types
                            orbitals.add(currentOrbital);
                            break;
                        }
                    }
                }
            }

            //return list
            return(orbitals != null ? orbitals.toArray(new DatabaseSatellite[0]) : new DatabaseSatellite[0]);
        }

        //Get orbital with given norad ID
        public static DatabaseSatellite getOrbital(Context context, int noradId, boolean allowLoad)
        {
            //if allow loading
            if(allowLoad)
            {
                //load if needed
                handleLoad(context);
            }

            //if buffered all exists
            if(buffer != null)
            {
                //go through each orbital
                for(DatabaseSatellite currentOrbital : buffer)
                {
                    //if norad ID matches
                    if(currentOrbital.noradId == noradId)
                    {
                        //return it
                        return(currentOrbital);
                    }
                }
            }

            //not found
            return(null);
        }
        public static DatabaseSatellite getOrbital(Context context, int noradId)
        {
            return(getOrbital(context, noradId, true));
        }

        //Gets if orbital is selected with option to allow loading
        //note: load not allowed when used during orbital updates
        public static boolean getOrbitalSelected(Context context, int noradId, boolean allowLoad)
        {
            //try to get orbital
            DatabaseSatellite orbital = getOrbital(context, noradId, allowLoad);

            //if orbital exists
            if(orbital != null)
            {
                //return if selected
                return(orbital.isSelected);
            }

            //unknown
            return(false);
        }

        //Gets the count of the given orbital type
        public static int getTypeCount(Context context, byte orbitalType)
        {
            int index;

            //load if needed
            handleLoad(context);

            //if type count needs to be calculated
            if(typeCount == null)
            {
                //initialize type count length
                //note: types start at 1
                typeCount = new int[OrbitalType.TypeCount + 1];
                for(index = 0; index < typeCount.length; index++)
                {
                    typeCount[index] = 0;
                }

                //go through each orbital
                for(DatabaseSatellite currentOrbital : buffer)
                {
                    //remember current type
                    byte currentType = currentOrbital.orbitalType;

                    //if a valid index
                    if(currentType >= 0 && currentType < typeCount.length)
                    {
                        //update count for type
                        typeCount[currentType]++;
                    }
                }
            }

            //return count
            return(orbitalType >= 0 && orbitalType < typeCount.length ? typeCount[orbitalType] : 0);
        }

        //Sets path color for orbital with given norad ID
        public static void setPathColor(Context context, int noradId, int pathColor)
        {
            //load if needed
            handleLoad(context);

            //go through each orbital
            for(DatabaseSatellite currentOrbital : buffer)
            {
                //if norad ID is a match
                if(currentOrbital.noradId == noradId)
                {
                    //update color and stop going through buffer
                    currentOrbital.pathColor = pathColor;
                    break;
                }
            }
        }

        //Sets if orbital with given norad ID is selected
        public static void setSelected(Context context, int noradId, boolean selected)
        {
            //load if needed
            handleLoad(context);

            //go through each orbital
            for(DatabaseSatellite currentOrbital : buffer)
            {
                //if norad ID is a match
                if(currentOrbital.noradId == noradId)
                {
                    //update selected status and stop going through buffer
                    currentOrbital.isSelected = selected;
                    break;
                }
            }
        }
    }

    private static final int TIME_ZONE_COLUMNS = 3;
    private static final int TIME_ZONE_ROWS = 23427;
    private static final String TIME_ZONE_FILE_SEPARATOR = "\t";

    public static class IdLine
    {
        public final int startId;
        public final int endId;

        public IdLine(int startId, int endId)
        {
            this.startId = startId;
            this.endId = endId;
        }

        //Gets ID lines from given string
        public static IdLine[] fromString(String pointString)
        {
            //if point string exists
            if(pointString != null)
            {
                int index;
                String[] pointPairs = pointString.split(CONSTELLATION_FILE_PAIR_SEPARATOR);
                IdLine[] lines = new IdLine[pointPairs.length];

                //if at least 1 line
                if(lines.length >= 1)
                {
                    //go through each pair
                    for(index = 0; index < pointPairs.length; index++)
                    {
                        String[] currentLine = pointPairs[index].split(CONSTELLATION_FILE_POINT_SEPARATOR);

                        //if a starting and ending ID
                        if(currentLine.length == 2)
                        {
                            //set current line
                            lines[index] = new IdLine(Globals.tryParseInt(currentLine[0]), Globals.tryParseInt(currentLine[1]));
                        }
                        else
                        {
                            //invalid
                            return(null);
                        }
                    }

                    //return lines
                    return(lines);
                }
            }

            //invalid
            return(null);
        }

        //Converts given ID lines to a string
        public static String toString(IdLine[] lines)
        {
            //if lines exist and at least 1
            if(lines != null && lines.length >= 1)
            {
                int index;
                StringBuilder pointString = new StringBuilder();

                //go through each line
                for(index = 0; index < lines.length; index++)
                {
                    //remember current ID line
                    IdLine currentLine = lines[index];

                    //add line points to string
                    pointString.append(currentLine.startId);
                    pointString.append(CONSTELLATION_FILE_POINT_SEPARATOR);
                    pointString.append(currentLine.endId);

                    //if there are more lines
                    if(index + 1 < lines.length)
                    {
                        //add separator
                        pointString.append(CONSTELLATION_FILE_PAIR_SEPARATOR);
                    }
                }

                //return point string
                return(pointString.toString());
            }

            //invalid
            return(null);
        }
    }

    public static class ParentProperties implements Parcelable
    {
        public final int id;
        public int index;
        public static final Creator<ParentProperties> CREATOR = new Creator<ParentProperties>()
        {
            @Override
            public ParentProperties createFromParcel(Parcel source)
            {
                return(new ParentProperties(source.readInt(), source.readInt()));
            }

            @Override
            public ParentProperties[] newArray(int size)
            {
                return(new ParentProperties[size]);
            }
        };

        public ParentProperties(int id, int index)
        {
            this.id = id;
            this.index = index;
        }

        @Override
        public int describeContents()
        {
            return(0);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags)
        {
            dest.writeInt(id);
            dest.writeInt(index);
        }
    }

    public static class DatabaseSatellite implements Parcelable, Serializable
    {
        private static abstract class ParentFilterState
        {
            static final byte Unknown = 0;
            static final byte Yes = 1;
            static final byte No = 2;
        }

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
        public final double rightAscensionHours;
        public final double declinationDegs;
        public final double magnitude;
        public double distanceLightYears;
        public final IdLine[] lines;
        public ArrayList<ParentProperties> parentProperties;
        public int pathColor;
        public final byte orbitalType;
        public final boolean tleIsAccurate;
        private boolean inFilter;
        private byte inParentFilterState;
        public boolean isSelected;
        public static final Creator<DatabaseSatellite> CREATOR =  new Parcelable.Creator<DatabaseSatellite>()
        {
            @Override
            public DatabaseSatellite createFromParcel(Parcel source)
            {
                return(new DatabaseSatellite(source.readString(), source.readString(), source.readInt(), source.readString(), source.readString(), source.readLong(), source.readString(), source.readString(), source.readLong(), source.readString(), source.readLong(), source.readDouble(), source.readDouble(), source.readDouble(), source.readDouble(), source.readString(), source.readParcelableArray(ParentProperties.class.getClassLoader()), source.readInt(), source.readByte(), (source.readByte() == 1), source.readByte(), (source.readByte() == 1)));
            }

            @Override
            public DatabaseSatellite[] newArray(int size)
            {
                return(new DatabaseSatellite[size]);
            }
        };

        public DatabaseSatellite(String name, String userName, int noradId, String ownerCode, String ownerName, long launchDate, String tleLine1, String tleLine2, long tleDateMs, String gp, long updateDateMs, double rightAscensionHours, double declinationDegs, double magnitude, double distanceLightYears, String pointsString, Parcelable[] parentProperties, int pathColor, byte orbitalType, boolean inFilter, byte inParentFilterState, boolean selected)
        {
            this.name = name;
            this.userName = (userName != null ? userName : "");
            this.noradId = noradId;
            this.ownerCode = ownerCode;
            this.ownerName = ownerName;
            this.tleLine1 = tleLine1;
            this.tleLine2 = tleLine2;
            this.gp = gp;
            this.tle = null;
            if(this.gp != null && !this.gp.isEmpty())
            {
                this.tle = Calculations.loadTLE(this.gp);
            }
            if(this.tle == null)
            {
                this.tle = Calculations.loadTLE(tleLine1, tleLine2);
            }
            this.tleDateMs = tleDateMs;
            this.updateDateMs = updateDateMs;
            this.rightAscensionHours = rightAscensionHours;
            this.declinationDegs = declinationDegs;
            this.magnitude = magnitude;
            this.distanceLightYears = distanceLightYears;
            this.lines = IdLine.fromString(pointsString);
            if(parentProperties instanceof ParentProperties[])
            {
                this.parentProperties = new ArrayList<>(parentProperties.length);
                for(Parcelable currentProperty : parentProperties)
                {
                    this.parentProperties.add((ParentProperties)currentProperty);
                }
            }
            else
            {
                this.parentProperties = null;
            }
            this.pathColor = pathColor;
            this.orbitalType = orbitalType;
            this.inFilter = inFilter;
            this.inParentFilterState = inParentFilterState;
            this.isSelected = selected;
            this.launchDateMs = launchDate;

            if((this.tleDateMs == Globals.INVALID_DATE_MS || this.tleDateMs == Globals.UNKNOWN_DATE_MS) && this.tle.satelliteNum != Universe.IDs.Invalid)
            {
                this.tleDateMs = Globals.julianDateToCalendar(this.tle.epochJulian).getTimeInMillis();
            }

            this.tleIsAccurate = (this.noradId != Universe.IDs.Invalid && this.noradId < 0) || Globals.getTLEIsAccurate(this.tleDateMs);
        }
        public DatabaseSatellite(String name, String userName, int noradId, String ownerCode, String ownerName, long launchDate, String tleLine1, String tleLine2, long tleDateMs, String gp, long updateDateMs, double rightAscensionHours, double declinationDegs, double magnitude, double distanceLightYears, String pathString, int pathColor, byte orbitalType, boolean selected)
        {
            this(name, userName, noradId, ownerCode, ownerName, launchDate, tleLine1, tleLine2, tleDateMs, gp, updateDateMs, rightAscensionHours, declinationDegs, magnitude, distanceLightYears, pathString, null, pathColor, orbitalType, true, ParentFilterState.Unknown, selected);
        }
        public DatabaseSatellite(String name, String userName, int noradId, String ownerCode, String ownerName, long launchDate, String tleLine1, String tleLine2, long tleDateMs, String gp, long updateDateMs, int pathColor, byte orbitalType, boolean selected)
        {
            this(name, userName, noradId, ownerCode, ownerName, launchDate, tleLine1, tleLine2, tleDateMs, gp, updateDateMs, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, null, pathColor, orbitalType, selected);
        }
        public DatabaseSatellite(String name, int noradId, String ownerCode, long launchDate, byte orbitalType)
        {
            this(name, null, noradId, ownerCode, null, launchDate, null, null, Globals.UNKNOWN_DATE_MS, null, Globals.UNKNOWN_DATE_MS, Color.DKGRAY, orbitalType, true);
        }

        public String getName()
        {
            return(!userName.isEmpty() ? userName : name);
        }

        public String[] getTLELines()
        {
            String line1;
            String line2;

            //if TLE lines exist
            if(tleLine1 != null && !tleLine1.isEmpty() && tleLine2 != null && !tleLine2.isEmpty())
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

        public ArrayList<Integer> getChildIds()
        {
            ArrayList<Integer> usedIds = new ArrayList<>(0);

            //if there are lines
            if(lines != null)
            {
                //go through each line
                for(IdLine currentLine : lines)
                {
                    //if start ID not in list
                    if(!usedIds.contains(currentLine.startId))
                    {
                        //add it
                        usedIds.add(currentLine.startId);
                    }

                    //if end ID not in list
                    if(!usedIds.contains(currentLine.endId))
                    {
                        //add it
                        usedIds.add(currentLine.endId);
                    }
                }
            }

            //return used child IDs
            return(usedIds);
        }

        public boolean getInFilter()
        {
            return(inFilter);
        }

        public void setInFilter(ArrayList<Byte> orbitalTypeFilterList)
        {
            int index;

            //if using filter
            if(orbitalTypeFilterList != null)
            {
                //reset in filter
                inFilter = false;

                //go through each filter type while not in filter
                for(index = 0; index < orbitalTypeFilterList.size() && !inFilter; index++)
                {
                    //update in filter
                    inFilter = orbitalTypeFilterList.contains(orbitalType);
                }
            }
            else
            {
                //always use
                inFilter = true;
            }
        }
        public void setInFilter(boolean inFilter)
        {
            this.inFilter = inFilter;
        }

        public boolean getInParentFilterSet()
        {
            return(inParentFilterState != ParentFilterState.Unknown);
        }

        public boolean getInParentFilter()
        {
            return(inParentFilterState != ParentFilterState.No);
        }

        public void setInParentFilter(boolean inFilter)
        {
            inParentFilterState = (inFilter ? ParentFilterState.Yes : ParentFilterState.No);
        }

        public void resetInParentFilter()
        {
            inParentFilterState = ParentFilterState.Unknown;
        }

        @Override
        public int describeContents()
        {
            return(0);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags)
        {
            dest.writeString(name);
            dest.writeString(userName);
            dest.writeInt(noradId);
            dest.writeString(ownerCode);
            dest.writeString(ownerName);
            dest.writeLong(launchDateMs);
            dest.writeString(tleLine1);
            dest.writeString(tleLine2);
            dest.writeLong(tleDateMs);
            dest.writeString(gp);
            dest.writeLong(updateDateMs);
            dest.writeDouble(rightAscensionHours);
            dest.writeDouble(declinationDegs);
            dest.writeDouble(magnitude);
            dest.writeDouble(distanceLightYears);
            dest.writeString(IdLine.toString(lines));
            if(parentProperties != null)
            {
                dest.writeParcelableArray(parentProperties.toArray(new Parcelable[0]), 0);
            }
            else
            {
                dest.writeParcelableArray(null, 0);
            }
            dest.writeInt(pathColor);
            dest.writeByte(orbitalType);
            dest.writeByte((byte)(inFilter ? 1 : 0));
            dest.writeByte(inParentFilterState);
            dest.writeByte((byte)(isSelected ? 1 : 0));
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
            return(database != null ? database.noradId : satellite != null ? satellite.getSatelliteNum() : Universe.IDs.None);
        }

        public String getName(String defaultVal)
        {
            return(database != null ? database.getName() : defaultVal);
        }
        public String getName()
        {
            return(getName("?"));
        }

        public String getOwnerCode()
        {
            return(database != null ? database.ownerCode : null);
        }

        public byte getOrbitalType()
        {
            return(database != null ? database.orbitalType : OrbitalType.Satellite);
        }

        public double getMagnitude()
        {
            return(database != null ? database.magnitude : Double.MAX_VALUE);
        }

        public int getPathColor()
        {
            return(database != null ? database.pathColor : Color.DKGRAY);
        }

        public void setPathColor(int color)
        {
            if(database != null)
            {
                database.pathColor = color;
            }
        }

        public boolean getTLEIsAccurate()
        {
            return(database != null && database.tleIsAccurate);
        }

        public boolean getInFilter()
        {
            return(database != null && database.getInFilter());
        }

        public void setInFilter(ArrayList<Byte> orbitalTypeFilterList)
        {
            if(database != null)
            {
                database.setInFilter(orbitalTypeFilterList);
            }
        }
        public void setInFilter(boolean inFilter)
        {
            if(database != null)
            {
                database.setInFilter(inFilter);
            }
        }

        public boolean getInParentFilterSet()
        {
            return(database != null && database.getInParentFilterSet());
        }

        public boolean getInParentFilter()
        {
            return(database != null && database.getInParentFilter());
        }

        public void setInParentFilter(boolean inFilter)
        {
            if(database != null)
            {
                database.setInParentFilter(inFilter);
            }
        }

        public void resetInParentFilter()
        {
            if(database != null)
            {
                database.resetInParentFilter();
            }
        }

        public ArrayList<ParentProperties> getParentProperties()
        {
            return(database != null ? database.parentProperties : null);
        }

        public IdLine[] getLines()
        {
            return(database != null ? database.lines : null);
        }

        public ArrayList<Integer> getChildIds()
        {
            return(database != null ? database.getChildIds() : new ArrayList<>(0));
        }

        @Override
        public boolean equals(Object other)
        {
            boolean isClass = (other instanceof SatelliteData);
            SatelliteData otherSat = (isClass ? (SatelliteData)other : null);
            boolean satNull = (satellite == null);
            boolean otherSatNull = (otherSat == null || otherSat.satellite == null);

            return((satNull && otherSatNull) || (!satNull && !otherSatNull && satellite.getSatelliteNum() == otherSat.satellite.getSatelliteNum()));
        }

        public static SatelliteData[] getSatellites(Context context, ArrayList<Integer> noraIds)
        {
            int index;
            SatelliteData[] satellites = null;

            if(noraIds != null)
            {
                satellites = new SatelliteData[noraIds.size()];
                for(index = 0; index < noraIds.size(); index++)
                {
                    satellites[index] = new SatelliteData(context, noraIds.get(index));
                }
            }

            return(satellites);
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

        public DatabaseLocation(int locationId, String name, double latitude, double longitude, double altitudeKm, String zoneId, byte locationType, boolean checked)
        {
            this.id = locationId;
            this.name = name;
            this.latitude = latitude;
            this.longitude = longitude;
            this.altitudeKM = altitudeKm;
            this.zoneId = zoneId;
            this.locationType = locationType;
            this.isChecked = checked;
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

    private static final int DB_VERSION = 41;
    private static final String DB_NAME = "OrbTrack.DB";
    private static UpdateStatusType updateStatus = null;

    private Globals.OnProgressChangedListener progressListener;

    public Database(Context context)
    {
        super(context, DB_NAME, null, DB_VERSION);

        //init data
        if(!LocaleStars.haveData())
        {
            LocaleStars.initData(context);
        }
        if(!LocaleConstellations.haveData())
        {
            LocaleConstellations.initData(context);
        }
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
    private static void initTable(Database instance, SQLiteDatabase db, Resources res, String tableName, String tableValues, Object[] sqlBindConstants,  @NonNull byte[] sqlBindTypes, int fileId, String fileSeparator, int rowTotalCount, int columnCount, int progressTitleId)
    {
        int index = 0;
        int bindIndex;
        int currentColumn;
        int pendingRows = 0;
        boolean usingConstant;
        String line;
        String progressTitle = res.getString(progressTitleId);
        SQLiteStatement sqlStatement = db.compileStatement("REPLACE INTO " + tableName + " " + tableValues);
        String[] columns;

        //open file
        BufferedReader file = new BufferedReader(new InputStreamReader(res.openRawResource(fileId)));
        try
        {
            //while there are lines to read
            while((line = file.readLine()) != null)
            {
                int bindIndexOffset = 0;

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
                    }

                    //add bind values
                    currentColumn = 0;
                    sqlStatement.clearBindings();
                    for(bindIndex = 0; bindIndex < sqlBindTypes.length; bindIndex++)
                    {
                        byte currentBindType = sqlBindTypes[bindIndex];

                        //check if using constant
                        usingConstant = (sqlBindConstants != null && sqlBindConstants[bindIndex] != null);

                        //if using current bind type
                        if(currentBindType != SQLBindType.None)
                        {
                            //handle based on current bind type
                            switch(currentBindType)
                            {
                                case SQLBindType.Double:
                                    sqlStatement.bindDouble(bindIndex + bindIndexOffset + 1, (usingConstant ? (Double)sqlBindConstants[bindIndex] : Double.valueOf(columns[currentColumn])));
                                    break;

                                case SQLBindType.String:
                                    sqlStatement.bindString(bindIndex + bindIndexOffset + 1, (usingConstant ? (String)sqlBindConstants[bindIndex] : columns[currentColumn]));
                                    break;
                            }
                        }
                        else
                        {
                            //don't count unused
                            bindIndexOffset--;
                        }

                        //if didn't use constant
                        if(!usingConstant)
                        {
                            //go to next column
                            currentColumn++;
                        }
                    }
                    sqlStatement.executeInsert();

                    //update progress
                    if(instance != null)
                    {
                        instance.sendProgress(Globals.ProgressType.Running, progressTitle, index, rowTotalCount);
                    }
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
    private void initTable(SQLiteDatabase db, Resources res, String tableName, String tableValues, @NonNull byte[] sqlBindTypes, int fileId, String fileSeparator, int rowCount, int columnCount, int progressTitleId)
    {
        initTable(this, db, res, tableName, tableValues, null, sqlBindTypes, fileId, fileSeparator, rowCount, columnCount, progressTitleId);
    }

    //Adds indexing to tables
    private static void initIndexing(SQLiteDatabase db, boolean starsOnly)
    {
        db.execSQL("CREATE INDEX IF NOT EXISTS " + IndexTables.Star + " ON " + Tables.Star + "([ID])");
        if(!starsOnly)
        {
            db.execSQL("CREATE INDEX IF NOT EXISTS " + IndexTables.Orbital + " ON " + Tables.Orbital + "([ID])");
            db.execSQL("CREATE INDEX IF NOT EXISTS " + IndexTables.Location + " ON " + Tables.Location + "([ID])");
            db.execSQL("CREATE INDEX IF NOT EXISTS " + IndexTables.LocationName + " ON " + Tables.LocationName + "([ID])");
            db.execSQL("CREATE INDEX IF NOT EXISTS " + IndexTables.TimeZone + " ON " + Tables.TimeZone + "([ID])");
            db.execSQL("CREATE INDEX IF NOT EXISTS " + IndexTables.Altitude + " ON " + Tables.Altitude + "([ID])");
            db.execSQL("CREATE INDEX IF NOT EXISTS " + IndexTables.MasterSatellite + " ON " + Tables.MasterSatellite + "([ID])");
            db.execSQL("CREATE INDEX IF NOT EXISTS " + IndexTables.Owner + " ON " + Tables.Owner + "([ID])");
            db.execSQL("CREATE INDEX IF NOT EXISTS " + IndexTables.Category + " ON " + Tables.Category + "([ID])");
            db.execSQL("CREATE INDEX IF NOT EXISTS " + IndexTables.SatelliteCategory + " ON " + Tables.SatelliteCategory + "([ID])");
            db.execSQL("CREATE INDEX IF NOT EXISTS " + IndexTables.Information + " ON " + Tables.Information + "([ID])");
        }
    }
    private static void initIndexing(SQLiteDatabase db)
    {
        initIndexing(db, false);
    }

    //Creates star table
    private static void createStars(SQLiteDatabase db)
    {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + Tables.Star + "([ID] INTEGER PRIMARY KEY, [Norad] INTEGER UNIQUE, [RA] DOUBLE, [DEC] DOUBLE, [Magnitude] DOUBLE, [Distance_LY] Double, [Points] TEXT)");
    }

    //Adds stars
    private static void addStars(Context context, Database instance, SQLiteDatabase db)
    {
        Resources res = (context != null ? context.getResources() : null);
        int pathColor = (res != null ? ResourcesCompat.getColor(res, R.color.light_gray, null) : Color.TRANSPARENT);
        ArrayList<Integer> ids;

        //if no context or no resources
        if(context == null || res == null)
        {
            //stop
            return;
        }

        //load information
        initTable(instance, db, res, Tables.Star, "([Norad], [RA], [DEC], [Magnitude], [Distance_LY]) VALUES(?, ?, ?, ?, ?)", null, new byte[]{SQLBindType.String, SQLBindType.None, SQLBindType.Double, SQLBindType.Double, SQLBindType.Double, SQLBindType.Double}, R.raw.stars_en, STARS_FILE_SEPARATOR, STARS_FILE_ROWS, STARS_FILE_FULL_COLUMNS, R.string.title_stars);

        //update local information and get IDs
        LocaleStars.initData(context);
        ids = LocaleStars.getNoradIds();

        //go through each ID
        for(Integer currentId : ids)
        {
            //add current star
            addOrbital(context, currentId, pathColor, OrbitalType.Star);
        }
    }

    //Add constellations
    private static void addConstellations(Context context, Database instance, SQLiteDatabase db)
    {
        Resources res = (context != null ? context.getResources() : null);
        int pathColor = (res != null ? ResourcesCompat.getColor(res, R.color.white, null) : Color.TRANSPARENT);
        ArrayList<Integer> ids;

        //if no context or no resources
        if(context == null || res == null)
        {
            //stop
            return;
        }

        //load information
        initTable(instance, db, res, Tables.Star, "([Norad], [RA], [DEC], [Points]) VALUES(?, ?, ?, ?)", null, new byte[]{SQLBindType.String, SQLBindType.None, SQLBindType.Double, SQLBindType.Double, SQLBindType.String}, R.raw.constellation_en, CONSTELLATION_FILE_SEPARATOR, CONSTELLATION_FILE_ROWS, CONSTELLATION_FILE_FULL_COLUMNS, R.string.title_constellations);

        //update local information and get IDs
        LocaleConstellations.initData(context);
        ids = LocaleConstellations.getNoradIds();

        //go through each ID
        for(Integer currentId : ids)
        {
            //add current constellation
            addOrbital(context, currentId, pathColor, OrbitalType.Constellation);
        }
    }

    //Add information
    private static void addInformation(Context context, Database instance, SQLiteDatabase db)
    {
        Resources res = (context != null ? context.getResources() : null);

        //if no context or no resources
        if(context == null || res == null)
        {
            //stop
            return;
        }

        //load information
        initTable(instance, db, res, Tables.Information, "([Norad], [Source], [Language], [Info]) VALUES(?, ?, ?, ?)", new Object[]{null, null, "en", null}, new byte[]{SQLBindType.String, SQLBindType.String, SQLBindType.String, SQLBindType.String}, R.raw.information_en, INFO_FILE_SEPARATOR, INFO_FILE_ROWS, INFO_FILE_COLUMNS, R.string.title_information);

        //update locale information
        LocaleInformation.initData(context);
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
        createStars(db);

        //add indexing
        initIndexing(db);

        //if there are no orbitals
        if(runQuery(context, QueryId.GetFirstOrbitalType, OrbitalType.Sun).length == 0)
        {
            //add sun
            addOrbital(context, Universe.IDs.Sun, Color.YELLOW, OrbitalType.Sun);

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
        if(runQuery(context, QueryId.GetFirstLocation, LocationType.Current).length == 0)
        {
            String zoneId = TimeZone.getDefault().getID();

            //add current location
            runInsert(context, Tables.Location, getLocationValues("Current", 0, 0, 0, zoneId, LocationType.Current, true, true));
        }

        //if there are no satellites
        if(runQuery(context, QueryId.GetFirstOrbitalType, OrbitalType.Satellite).length == 0)
        {
            //load default satellites
            saveSatellite(context, "ISS (ZARYA)", ISS_ZARYA_NORAD_ID, "ISS", 911548800000L, TLELines.ISSZarya1, TLELines.ISSZarya2, TLELines.ISSZaryaDate, null, TLELines.ISSZaryaDate, OrbitalType.Satellite);
        }

        //if there are no stars
        if(runQuery(context, QueryId.GetFirstStar).length == 0)
        {
            //add stars and constellations
            addStars(context, this, db);
            addConstellations(context, this, db);
        }

        //if there are no categories
        if(runQuery(context, QueryId.GetFirstCategory).length == 0)
        {
            //add none group
            runInsert(context, Tables.Category, getCategoryValues("None", 0));
        }

        //if there is no information
        if(runQuery(context, QueryId.GetFirstInformation).length == 0)
        {
            //add information
            addInformation(context, this, db);
        }

        //if there are no owners
        if(runQuery(context, QueryId.GetFirstOwner).length == 0)
        {
            //load owners
            initTable(db, res, Tables.Owner, "([Code], [Name]) VALUES(?, ?)", new byte[]{SQLBindType.String, SQLBindType.String}, R.raw.owners_en, OWNERS_FILE_SEPARATOR, OWNERS_FILE_ROWS, OWNERS_FILE_COLUMNS, R.string.title_owners);

            //update locale owners
            LocaleOwner.initData(context);
        }

        //if there are no time zones
        if(runQuery(context, QueryId.GetFirstTimeZone).length == 0)
        {
            //load time zones
            initTable(db, res, Tables.TimeZone, "([Latitude], [Longitude], [ZoneId]) VALUES(?, ?, ?)", new byte[]{SQLBindType.Double, SQLBindType.Double, SQLBindType.String}, R.raw.timezones, TIME_ZONE_FILE_SEPARATOR, TIME_ZONE_ROWS, TIME_ZONE_COLUMNS, R.string.title_time_zone_locations);
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
        String stringValue;
        SQLiteDatabase db = null;
        DatabaseSatellite issZarya;

        //make a call to possibly call onUpgrade/set updateStatus
        getSatelliteId(context, Universe.IDs.None);

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

                //if coming from version before master list parsing improved or celestrak changed
                if((updateStatus.previousVersion < 4 && updateStatus.currentVersion >= 4) || (updateStatus.previousVersion < 19 && updateStatus.currentVersion >= 19))
                {
                    //clear master satellites
                    clearMasterSatelliteTable(context);
                }

                //if previous version is before table indexing
                if(updateStatus.previousVersion < 19)
                {
                    //add indexing
                    if(db == null)
                    {
                        db = DatabaseManager.get(context, true);
                    }
                    initIndexing(db);
                }

                //if previous is before lens icon indicators
                if(updateStatus.previousVersion < 30)
                {
                    //default to lens icon indicators
                    Settings.setIndicator(context, Settings.Options.LensView.IndicatorType.Icon);
                }

                //if previous version before changing encryption and adding stars and constellation tables
                if(updateStatus.previousVersion < 33)
                {
                    //if have Space-Track password saved
                    stringValue = Settings.getEncryptedSpaceTrackPassword(context);
                    if(stringValue != null)
                    {
                        //decrypt with old key and encrypt with new key
                        Encryptor.encrypt(context, Encryptor.decryptOld(context, stringValue));
                    }

                    //make sure table exists and add indexing
                    if(db == null)
                    {
                        db = DatabaseManager.get(context, true);
                    }
                    createStars(db);
                    initIndexing(db, true);

                    //if sun is already in orbitals
                    if(Database.getOrbital(context, Universe.IDs.Sun) != null)
                    {
                        ContentValues values = new ContentValues();

                        //update sun orbital type
                        values.put("[Type]", OrbitalType.Sun);
                        runUpdate(context, Tables.Orbital, values, "[Norad]=?", new String[]{String.valueOf(Universe.IDs.Sun)});
                    }

                    //if polaris is already in orbitals
                    if(Database.getOrbital(context, Universe.IDs.Polaris) != null)
                    {
                        //remove it before adding again later
                        Database.deleteSatellite(context, Universe.IDs.Polaris);
                    }

                    //add all stars and constellations
                    addStars(context, null, db);
                    addConstellations(context, null, db);

                    //add/update information
                    addInformation(context, null, db);
                }

                //if before camera lens improvements and adding virtual lens
                if(updateStatus.previousVersion < 40)
                {
                    //use camera lens auto width/height by default
                    Settings.setLensAutoWidth(context, true);
                    Settings.setLensAutoHeight(context, true);
                }

                //if ISS Zarya exists and is older than hard coded TLE
                issZarya = getOrbital(context, ISS_ZARYA_NORAD_ID);
                if(issZarya != null && issZarya.updateDateMs < TLELines.ISSZaryaDate)
                {
                    //save updated TLE
                    saveSatellite(context, issZarya.getName(), ISS_ZARYA_NORAD_ID, issZarya.ownerCode, issZarya.launchDateMs, TLELines.ISSZarya1, TLELines.ISSZarya2, TLELines.ISSZaryaDate, null, TLELines.ISSZaryaDate, OrbitalType.Satellite);
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
        String updateString = res.getQuantityString(R.plurals.title_updates, 1);
        ArrayList<String> titles = new ArrayList<>(0);
        ArrayList<String> messages = new ArrayList<>(0);

        //if added stars/constellation
        if(previousVersion < 33)
        {
            titles.add(updateString);
            messages.add(res.getString(R.string.desc_stars_constellation_notice));
        }

        //if improved camera lens and added virtual lens
        if(previousVersion < 40)
        {
            titles.add(updateString);
            messages.add(res.getString(R.string.desc_lens_view_improvements));
        }

        //if any notices to show
        if(!titles.isEmpty())
        {
            //show all notices
            Globals.showNotificationDialog(context, Globals.getDrawable(context, R.drawable.ic_info_black, true), titles.toArray(new String[0]), messages.toArray(new String[0]), R.string.title_ok, -1, false, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    //do nothing
                }
            }, null, false);
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
    private static String[][] runQuery(Context context, int queryId, Object... argValues)
    {
        int index;
        int index2;
        int columnCount;
        Cursor queryResult = null;
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        String[][] queryResults = new String[0][0];

        try
        {
            SQLiteDatabase db = DatabaseManager.get(context, false);

            builder.setStrict(true);
            switch(queryId)
            {
                case QueryId.GetOrbitals:
                    builder.setTables(Tables.Orbital + " LEFT JOIN " + Tables.Owner + " ON [Owner_Code]=[Code] LEFT JOIN " + Tables.Star + " ON " + Tables.Orbital + ".[Norad]=" + Tables.Star + ".[Norad]");
                    queryResult = builder.query(db, new String[]{Tables.Orbital + ".[Name]", "[User_Name]", Tables.Orbital + ".[Norad]", "[Code]", Tables.Owner + ".[Name]", "[Launch_Date]", "[TLE_Line1]", "[TLE_Line2]", "[TLE_Date]", "[GP]", "[Update_Date]", "[RA]", "[DEC]", "[Magnitude]", "[Distance_LY]", "[Points]", "[Path_Color]", "[Type]", "[Selected]"}, null, null, null, null, "CASE WHEN [User_Name] IS NULL OR [User_Name]='' THEN " + Tables.Orbital + ".[Name] ELSE [User_Name] END ASC", null);
                    break;

                case QueryId.GetOrbitalSatelliteType:
                    builder.setTables(Tables.Orbital + " LEFT JOIN " + Tables.Owner + " ON [Owner_Code]=[Code] LEFT JOIN " + Tables.Star + " ON " + Tables.Orbital + ".[Norad]=" + Tables.Star + ".[Norad]");
                    queryResult = builder.query(db, new String[]{Tables.Orbital + ".[Name]", "[User_Name]", Tables.Orbital + ".[Norad]", "[Code]", Tables.Owner + ".[Name]", "[Launch_Date]", "[TLE_Line1]", "[TLE_Line2]", "[TLE_Date]", "[GP]", "[Update_Date]", "[RA]", "[DEC]", "[Magnitude]", "[Distance_LY]", "[Points]", "[Path_Color]", "[Type]", "[Selected]"}, "[Type] IN(?, ?, ?)", new String[]{String.valueOf(OrbitalType.Satellite), String.valueOf(OrbitalType.RocketBody), String.valueOf(OrbitalType.Debris)}, null, null, "CASE WHEN [User_Name] IS NULL OR [User_Name]='' THEN " + Tables.Orbital + ".[Name] ELSE [User_Name] END ASC", null);
                    break;

                case QueryId.GetOwnersEnglish:
                    builder.setTables(Tables.Owner + " JOIN " + Tables.Orbital + " ON " + Tables.Owner + ".[Code]=" + Tables.Orbital + ".[Owner_Code]");
                    queryResult = builder.query(db, new String[]{"[Code]", Tables.Owner + ".[Name]"}, "[Norad]=?", new String[]{String.valueOf(argValues[0])}, null, null, Tables.Owner + ".[Name]", null);
                    break;

                case QueryId.GetSatelliteCategoriesEnglishNorad:
                    builder.setTables(Tables.Category + " JOIN " + Tables.SatelliteCategory + " ON " + Tables.Category + ".[Indx]=" + Tables.SatelliteCategory + ".[Category_Index]");
                    queryResult = builder.query(db, new String[]{Tables.SatelliteCategory + ".[Norad]", "[Name]", "[Indx]"}, Tables.SatelliteCategory + ".[Norad]=?", new String[]{String.valueOf(argValues[0])}, null, null, null, null);
                    break;

                case QueryId.GetSatelliteCategoriesEnglish:
                    builder.setTables(Tables.SatelliteCategory);
                    queryResult = builder.query(db, new String[]{"[Norad]", "[Category_Index]"}, null, null, null, null, "[Norad], [Category_Index] ASC", null);
                    break;

                case QueryId.GetInformationNoradLanguageSource:
                    builder.setDistinct(true);
                    builder.setTables(Tables.Information);
                    queryResult = builder.query(db, new String[]{"[Info]"}, "[Norad]=? AND [Language]=? AND [Source] IN(?, ?, ?)", new String[]{String.valueOf(argValues[0]), String.valueOf(argValues[1]), String.valueOf(argValues[2]), String.valueOf(UpdateSource.TheSkyLive), String.valueOf(UpdateSource.SpaceDotCom)}, null, null, null, null);
                    break;

                case QueryId.GetInformationNorad:
                    builder.setDistinct(true);
                    builder.setTables(Tables.Information);
                    queryResult = builder.query(db, new String[]{"[Norad]", "[Info]", "[Language]", "[Source]"}, "[Norad]=?", new String[]{String.valueOf(argValues[0])}, null, null, null, null);
                    break;

                case QueryId.GetMasterSatellites:
                    builder.setTables(Tables.MasterSatellite + " LEFT JOIN " + Tables.Owner + " ON " + Tables.MasterSatellite + ".[Owner_Code]=" + Tables.Owner + ".[Code] LEFT JOIN " + Tables.SatelliteCategory + " ON " + Tables.MasterSatellite + ".[Norad]=" + Tables.SatelliteCategory + ".[Norad] LEFT JOIN " + Tables.Category + " ON [Category_Index]=[Indx]");
                    queryResult = builder.query(db, new String[]{Tables.MasterSatellite + ".[Norad]", Tables.MasterSatellite + ".[Name]", "[Owner_Code]", Tables.Owner + ".[Name]", Tables.MasterSatellite + ".[Launch_Date]", Tables.SatelliteCategory + ".[Category_Index]", Tables.Category + ".[Name]"}, null, null, null, null, Tables.MasterSatellite + ".[Norad], [Category_Index]", null);
                    break;

                case QueryId.GetClosestLocationName:
                    builder.setTables(Tables.LocationName);
                    queryResult = builder.query(db, new String[]{"[Latitude]", "[Longitude]", "[Name]"}, "[Latitude] >= ? AND [Latitude] <= ? AND [Longitude] >= ? AND [Longitude] <= ?", new String[]{String.valueOf(argValues[0]), String.valueOf(argValues[1]), String.valueOf(argValues[2]), String.valueOf(argValues[3])}, null, null, "[Latitude], [Longitude] ASC", null);
                    break;

                case QueryId.GetClosestTimeZone:
                    builder.setTables(Tables.TimeZone);
                    queryResult = builder.query(db, new String[]{"[Latitude]", "[Longitude]", "[ZoneId]"}, "[Latitude] >= ? AND [Latitude] <= ? AND [Longitude] >= ? AND [Longitude] <= ?", new String[]{String.valueOf(argValues[0]), String.valueOf(argValues[1]), String.valueOf(argValues[2]), String.valueOf(argValues[3])}, null, null, "[Latitude], [Longitude] ASC", null);
                    break;

                case QueryId.GetClosestAltitude:
                    builder.setTables(Tables.Altitude);
                    queryResult = builder.query(db, new String[]{"[Latitude]", "[Longitude]", "[Altitude]"}, "[Latitude] >= ? AND [Latitude] <= ? AND [Longitude] >= ? AND [Longitude] <= ?", new String[]{String.valueOf(argValues[0]), String.valueOf(argValues[1]), String.valueOf(argValues[2]), String.valueOf(argValues[3])}, null, null, "[Latitude], [Longitude] ASC", null);
                    break;

                case QueryId.GetLocations:
                    builder.setTables(Tables.Location);
                    queryResult = builder.query(db, new String[]{"[ID]", "[Name]", "[Latitude]", "[Longitude]", "[Altitude]", "[ZoneId]", "[Type]", "[Selected]"}, null, null, null, null, "[Type], [Name] ASC", null);
                    break;

                case QueryId.GetLocationsSelected:
                    builder.setTables(Tables.Location);
                    queryResult = builder.query(db, new String[]{"[ID]", "[Name]", "[Latitude]", "[Longitude]", "[Altitude]", "[ZoneId]", "[Type]", "[Selected]"}, "[Selected]=?", new String[]{"1"}, null, null, "[Type], [Name] ASC", null);
                    break;

                case QueryId.GetLocationsId:
                    builder.setTables(Tables.Location);
                    queryResult = builder.query(db, new String[]{"[ID]", "[Name]", "[Latitude]", "[Longitude]", "[Altitude]", "[ZoneId]", "[Type]", "[Selected]"}, "[ID]=?", new String[]{String.valueOf(argValues[0])}, null, null, "[Type], [Name] ASC", null);
                    break;

                case QueryId.GetLocationsType:
                    builder.setTables(Tables.Location);
                    queryResult = builder.query(db, new String[]{"[ID]", "[Name]", "[Latitude]", "[Longitude]", "[Altitude]", "[ZoneId]", "[Type]", "[Selected]"}, "[Type] <> ?", new String[]{String.valueOf(Database.LocationType.Current)}, null, null, "[Type], [Name] ASC", null);
                    break;

                case QueryId.GetOwners:
                    builder.setTables(Tables.Owner);
                    queryResult = builder.query(db, new String[]{"[Code]", "[Name]"}, null, null, null, null, "[Code]", null);
                    break;

                case QueryId.GetCategories:
                    builder.setTables(Tables.Category);
                    queryResult = builder.query(db, new String[]{"[Name]", "[Indx]"}, null, null, null, null, "[Name], [Indx]", null);
                    break;

                case QueryId.GetSatelliteData:
                    builder.setTables(Tables.Orbital);
                    queryResult = builder.query(db, new String[]{"[Name]", "[User_Name]", "[Norad]", "[Owner_Code]", "[Launch_Date]", "[TLE_Line1]", "[TLE_Line2]", "[TLE_Date]", "[GP]", "[Update_Date]", "[Path_Color]", "[Type]", "[Selected]"}, "[Norad]=?", new String[]{String.valueOf(argValues[0])}, null, null, null, null);
                    break;

                case QueryId.GetLocationIdName:
                    builder.setTables(Tables.Location);
                    queryResult = builder.query(db, new String[]{"[ID]"}, "[Name]=?", new String[]{String.valueOf(argValues[0])}, null, null, null, "1");
                    break;

                case QueryId.GetLocationIdNameType:
                    builder.setTables(Tables.Location);
                    queryResult = builder.query(db, new String[]{"[ID]"}, "[Name]=? OR [Type]=?", new String[]{String.valueOf(argValues[0]), String.valueOf(LocationType.Current)}, null, null, null, "1");
                    break;

                case QueryId.GetFirstOrbitalType:
                    builder.setTables(Tables.Orbital);
                    queryResult = builder.query(db, new String[]{"[Name]"}, "[Type]=?", new String[]{String.valueOf(argValues[0])}, null, null, null, "1");
                    break;

                case QueryId.GetFirstOrbitalNorad:
                    builder.setTables(Tables.Orbital);
                    queryResult = builder.query(db, new String[]{"[ID]"}, "[Norad]=?", new String[]{String.valueOf(argValues[0])}, null, null, null, "1");
                    break;

                case QueryId.GetFirstStar:
                    builder.setTables(Tables.Star);
                    queryResult = builder.query(db, new String[]{"[Norad]"}, null, null, null, null, null, "1");
                    break;

                case QueryId.GetFirstLocation:
                    builder.setTables(Tables.Location);
                    queryResult = builder.query(db, new String[]{"[Name]"}, "[Type]=?", new String[]{String.valueOf(argValues[0])}, null, null, null, "1");
                    break;

                case QueryId.GetFirstCategory:
                    builder.setTables(Tables.Category);
                    queryResult = builder.query(db, new String[]{"[Name]"}, null, null, null, null, null, "1");
                    break;

                case QueryId.GetFirstInformation:
                    builder.setTables(Tables.Information);
                    queryResult = builder.query(db, new String[]{"[Info]"}, null, null, null, null, null, "1");
                    break;

                case QueryId.GetFirstOwner:
                    builder.setTables(Tables.Owner);
                    queryResult = builder.query(db, new String[]{"[Code]"}, null, null, null, null, null, "1");
                    break;

                case QueryId.GetFirstTimeZone:
                    builder.setTables(Tables.TimeZone);
                    queryResult = builder.query(db, new String[]{"[ZoneId]"}, null, null, null, null, null, "1");
                    break;
            }

            //if result is set
            if(queryResult != null)
            {
                //get query results
                columnCount = queryResult.getColumnCount();
                queryResults = new String[queryResult.getCount()][columnCount];
                for(index = 0; index < queryResults.length && queryResult.moveToNext(); index++)
                {
                    //go through each column
                    for(index2 = 0; index2 < columnCount; index2++)
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

        //return results
        return(queryResults);
    }
    private static String[][] runQuery(Context context, int queryId)
    {
        return(runQuery(context, queryId, (Object)null));
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
    private static boolean runUpdate(Context context, String table, ContentValues values, String where, String[] whereArgs)
    {
        boolean success = false;
        SQLiteDatabase db = DatabaseManager.get(context, true);

        try
        {
            success = (db.update(table, values, where, whereArgs) > 0);
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
            saveID = (runUpdate(context, table, values, "[ID]=?", new String[]{String.valueOf(id)}) ? id : -1);
        }
        else
        {
            saveID = runInsert(context, table, values);
        }

        return(saveID);
    }

    //Runs a delete and returns success
    private static boolean runDelete(Context context, String table, String where, String[] whereArgs)
    {
        boolean success = false;
        SQLiteDatabase db = DatabaseManager.get(context, true);

        try
        {
            success = (db.delete(table, where, whereArgs) > 0);
        }
        catch(Exception ex)
        {
            //show error
            showError(context, ex);
        }

        return(success);
    }

    //Adds orbital to database
    private static void addOrbital(Context context, int orbitalId, int pathColor, byte orbitalType)
    {
        long gmtMs = Globals.getGMTTime().getTimeInMillis();
        runInsert(context, Tables.Orbital, getSatelliteValues(Universe.getName(null, orbitalId, orbitalType), "", orbitalId, "", 0, "", "", gmtMs, "", gmtMs, pathColor, orbitalType, true));
    }

    //Gets all satellite data for the given norad ID
    public static String[][] getSatelliteData(Context context, int noradId)
    {
        return(runQuery(context, QueryId.GetSatelliteData, noradId));
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

    //Creates satellite cache file
    public static void createSatelliteCacheFile(Context context, ArrayList<Database.DatabaseSatellite> satellites)
    {
        File satCache;
        FileOutputStream outputStream;
        ObjectOutputStream objectOutputStream;

        //if context is set
        if(context != null)
        {
            //set file
            satCache = new File(context.getCacheDir(), "satCache.txt");

            try
            {
                //if file exists
                if(satCache.exists())
                {
                    //if unable to delete file and can't write
                    if(!satCache.delete() && !satCache.canWrite())
                    {
                        //stop
                        return;
                    }
                }

                //try to create file
                if(satCache.createNewFile())
                {
                    //write each satellite
                    outputStream = new FileOutputStream(satCache);
                    objectOutputStream = new ObjectOutputStream(outputStream);
                    for(Database.DatabaseSatellite currentSatellite : satellites)
                    {
                        //write satellite
                        objectOutputStream.writeObject(currentSatellite);
                    }
                    objectOutputStream.close();
                    outputStream.close();
                }
            }
            catch(Exception ex)
            {
                //failed
            }
        }
    }

    //Reads satellite data from cache file
    public static ArrayList<Database.DatabaseSatellite> getSatelliteCacheData(Context context)
    {
        Object currentObject;
        File satCache;
        InputStream inputStream;
        ObjectInputStream objectInputStream;
        ArrayList<DatabaseSatellite> satellites = new ArrayList<>(0);

        //if context is set
        if(context != null)
        {
            //get file
            satCache = new File(context.getCacheDir(), "satCache.txt");

            try
            {
                //read each satellite from file
                inputStream = Globals.createFileInputStream(satCache);
                objectInputStream = new ObjectInputStream(inputStream);
                while((currentObject = objectInputStream.readObject()) != null)
                {
                    //add satellite to list
                    satellites.add((DatabaseSatellite)currentObject);
                }
                objectInputStream.close();
                inputStream.close();
            }
            catch(EOFException eofEx)
            {
                //do nothing
            }
            catch(Exception ex)
            {
                //clear list
                satellites.clear();
            }
        }

        //return list
        return(satellites);
    }

    //Gets desired orbitals from the database
    public static DatabaseSatellite[] getOrbitals(Context context, boolean satellitesOnly)
    {
        int index;
        int noradId;
        byte orbitalType;
        String name;
        String ownerCode;
        String localOwnerName;
        String[][] queryResult = runQuery(context, satellitesOnly ? QueryId.GetOrbitalSatelliteType : QueryId.GetOrbitals);
        ArrayList<DatabaseSatellite> list = new ArrayList<>(0);

        //go through each satellite
        for(index = 0; index < queryResult.length; index++)
        {
            //get ID, type, and name
            noradId = Integer.parseInt(queryResult[index][2]);
            orbitalType = Byte.parseByte(queryResult[index][17]);
            name = (noradId < 0 ? Universe.getName(context, noradId, orbitalType) : queryResult[index][0]);

            //get owner code and name
            ownerCode = queryResult[index][3];
            localOwnerName = LocaleOwner.getName(context, ownerCode);

            //add to list
            list.add(new DatabaseSatellite(name, queryResult[index][1], noradId, ownerCode, (localOwnerName != null ? localOwnerName : queryResult[index][4]), Long.parseLong(queryResult[index][5]), queryResult[index][6], queryResult[index][7], Long.parseLong(queryResult[index][8]), queryResult[index][9], Long.parseLong(queryResult[index][10]), Globals.tryParseDouble(queryResult[index][11]), Globals.tryParseDouble(queryResult[index][12]), Globals.tryParseDouble(queryResult[index][13]), Globals.tryParseDouble(queryResult[index][14]), queryResult[index][15], Integer.parseInt(queryResult[index][16]), orbitalType, queryResult[index][18].equals("1")));
        }

        //return list as array
        return(list.toArray(new DatabaseSatellite[0]));
    }
    public static DatabaseSatellite[] getOrbitals(Context context, int[] noradIds)
    {
        boolean haveNoradIds = (noradIds != null && noradIds.length > 0);
        ArrayList<DatabaseSatellite> orbitals = new ArrayList<>(haveNoradIds ? noradIds.length : 0);

        //if have norad IDs
        if(haveNoradIds)
        {
            //go through each ID
            for(int currentId : noradIds)
            {
                //try to get orbital
                DatabaseSatellite currentOrbital = OrbitalsBuffer.getOrbital(context, currentId, false);
                if(currentOrbital != null)
                {
                    //add orbital to list
                    orbitals.add(currentOrbital);
                }
            }
        }

        //return orbitals
        return(orbitals.toArray(new DatabaseSatellite[0]));
    }
    public static DatabaseSatellite[] getOrbitals(Context context, byte ...orbitalTypes)
    {
        return(OrbitalsBuffer.getOrbitals(context, orbitalTypes));
    }
    public static DatabaseSatellite[] getOrbitals(Context context)
    {
        return(OrbitalsBuffer.getAll(context));
    }

    //Gets desired orbital with norad ID
    public static DatabaseSatellite getOrbital(Context context, int noradId, boolean allowLoad)
    {
        return(OrbitalsBuffer.getOrbital(context, noradId, allowLoad));
    }
    public static DatabaseSatellite getOrbital(Context context, int noradId)
    {
        return(OrbitalsBuffer.getOrbital(context, noradId));
    }

    //Gets all selected orbitals
    public static DatabaseSatellite[] getSelectedOrbitals(Context context)
    {
        return(OrbitalsBuffer.getSelected(context));
    }

    //Gets a satellite ID by norad
    private static long getSatelliteId(Context context, int noradId)
    {
        //run query
        String[][] queryResult = runQuery(context, QueryId.GetFirstOrbitalNorad, noradId);
        return(queryResult.length > 0 ? Long.parseLong(queryResult[0][0]) : -1);
    }

    //Modifies satellite data and returns ID
    public static long saveSatellite(Context context, String name, String userName, int noradId, String ownerCode, long launchDate, String tleLine1, String tleLine2, long tleDateMs, String gp, long updateDateMs, int pathColor, byte orbitalType, boolean selected, boolean needReload)
    {
        long id = getSatelliteId(context, noradId);
        long saveId;
        ContentValues satelliteValues = getSatelliteValues(name, userName, noradId, ownerCode, launchDate, tleLine1, tleLine2, tleDateMs, gp, updateDateMs, pathColor, orbitalType, selected);
        saveId = runSave(context, id, Tables.Orbital, satelliteValues);

        //if need to reload
        if(needReload)
        {
            //update buffer
            OrbitalsBuffer.setNeedReload();
        }

        //update any applicable widget
        WidgetPassBaseProvider.updateWidget(context, noradId);

        //return save ID
        return(saveId);
    }
    public static long saveSatellite(Context context, String name, String userName, int noradId, String ownerCode, long launchDate, String tleLine1, String tleLine2, long tleDateMs, String gp, long updateDateMs, int pathColor, byte orbitalType, boolean selected)
    {
        return(saveSatellite(context, name, userName, noradId, ownerCode, launchDate, tleLine1, tleLine2, tleDateMs, gp, updateDateMs, pathColor, orbitalType, selected, true));
    }
    public static long saveSatellite(Context context, String name, int noradId, String ownerCode, long launchDate, String tleLine1, String tleLine2, long tleDateMs, String gp, long updateDateMs, byte orbitalType)
    {
        int nextDefaultColor = Color.DKGRAY;
        DatabaseSatellite orbital = OrbitalsBuffer.getOrbital(context, noradId, false);
        boolean haveOrbital = (orbital != null);

        //if a new orbital and using next default color
        if(!haveOrbital && Settings.usingSatelliteNextDefaultColor())
        {
            //get and generate next default color
            nextDefaultColor = Settings.getSatelliteNextDefaultColor(context);
            Settings.generateNextDefaultColor(context, nextDefaultColor);
        }

        return(saveSatellite(context, name, null, noradId, ownerCode, launchDate, tleLine1, tleLine2, tleDateMs, gp, updateDateMs, (haveOrbital ? orbital.pathColor : nextDefaultColor), orbitalType, (!haveOrbital || orbital.isSelected)));
    }
    public static void saveSatellite(Context context, int noradId, String userName, String ownerCode, long launchDate)
    {
        saveSatellite(context, null, userName, noradId, ownerCode, launchDate, null, null, Globals.INVALID_DATE_MS, null, Globals.INVALID_DATE_MS, Integer.MAX_VALUE, Byte.MAX_VALUE, OrbitalsBuffer.getOrbitalSelected(context, noradId, false));
    }

    //Saves satellite path color
    public static void saveSatellitePathColor(Context context, int noradId, int pathColor)
    {
        OrbitalsBuffer.setPathColor(context, noradId, pathColor);
        saveSatellite(context, null, null, noradId, null, Globals.INVALID_DATE_MS, null, null, Globals.INVALID_DATE_MS, null, Globals.INVALID_DATE_MS, pathColor, Byte.MAX_VALUE, true, false);
    }

    //Saves satellite visibility
    public static void saveSatelliteVisible(Context context, int noradId, boolean selected)
    {
        OrbitalsBuffer.setSelected(context, noradId, selected);
        saveSatellite(context, null, null, noradId, null, Globals.INVALID_DATE_MS, null, null, Globals.INVALID_DATE_MS, null, Globals.INVALID_DATE_MS, Integer.MAX_VALUE, Byte.MAX_VALUE, selected, false);
    }

    //Deletes a satellite
    public static boolean deleteSatellite(Context context, int noradId)
    {
        boolean success = runDelete(context, Tables.Orbital, "[ID]=?", new String[]{String.valueOf(getSatelliteId(context, noradId))});
        OrbitalsBuffer.setNeedReload();
        return(success);
    }

    //Gets the count of given orbital type
    public static int getOrbitalTypeCount(Context context, byte orbitalType)
    {
        return(OrbitalsBuffer.getTypeCount(context, orbitalType));
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
    private static String getClosestData(Context context, int queryId, double latitude, double longitude, double delta)
    {
        int index;
        double absDelta = Math.abs(delta);
        double currentLat;
        double currentLon;
        double currentDelta;
        double closestDelta = Double.MAX_VALUE;
        String closestData = null;
        String currentData;
        String minLat = Globals.getNumberString(latitude - absDelta, 5, false);
        String maxLat = Globals.getNumberString(latitude + absDelta, 5, false);
        String minLon = Globals.getNumberString(longitude - delta, 5, false);
        String maxLon = Globals.getNumberString(longitude + delta, 5, false);
        String[][] queryResult = runQuery(context, queryId, minLat, maxLat, minLon, maxLon);

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
        return(getClosestData(context, QueryId.GetClosestLocationName, latitude, longitude, delta));
    }

    //Gets closest known time zone within given delta
    public static String getClosestTimeZone(Context context, double latitude, double longitude, double delta)
    {
        return(getClosestData(context, QueryId.GetClosestTimeZone, latitude, longitude, delta));
    }

    //Gets closest known altitude within given delta
    public static double getClosestAltitude(Context context, double latitude, double longitude, double delta)
    {
        String dataString = getClosestData(context, QueryId.GetClosestAltitude, latitude, longitude, delta);
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
    public static DatabaseLocation[] getLocations(Context context, int queryId, Object ...argValues)
    {
        int index;
        int index2;
        byte locationType;
        Resources res = (context != null ? context.getResources() : null);
        String name;
        String[][] queryResult = runQuery(context, queryId, argValues);
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
        return(getLocations(context, QueryId.GetLocations));
    }

    //Gets the selected location from the database
    private static DatabaseLocation getSelectedLocation(Context context)
    {
        DatabaseLocation[] locations = getLocations(context, QueryId.GetLocationsSelected);

        if(locations.length > 0)
        {
            return(locations[0]);
        }
        else
        {
            return(new DatabaseLocation(0, context.getResources().getString(R.string.title_current), 0, 0 , 0, TimeZone.getDefault().getID(), LocationType.Current, true));
        }
    }

    //Gets the last known location
    public static DatabaseLocation getLocation(Context context)
    {
        Location lastLocation;
        DatabaseLocation currentLocation = getSelectedLocation(context);
        boolean isCurrent = (currentLocation.locationType == LocationType.Current);

        //if is current location
        if(isCurrent)
        {
            //update with last known location
            lastLocation = Settings.getLastLocation(context);
            currentLocation.latitude = lastLocation.getLatitude();
            currentLocation.longitude = lastLocation.getLongitude();
            currentLocation.altitudeKM = lastLocation.getAltitude() / 1000;
        }

        //return location
        return(currentLocation);
    }

    //Gets a location ID by name
    private static long getLocationID(Context context, String name, byte locationType)
    {
        //run query
        boolean usingCurrent = (locationType == LocationType.Current);
        String[][] queryResult = runQuery(context, (usingCurrent ? QueryId.GetLocationIdNameType : QueryId.GetLocationIdName), name);
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
            runUpdate(context, Tables.Location, selectValue, null, null);
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
        return(runDelete(context, Tables.Location, "[ID]=?", new String[]{String.valueOf(getLocationID(context, name, locationType))}));
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
        return(runQuery(context, QueryId.GetOwnersEnglish, noradId));
    }

    //Gets owners from the database in the current locale
    public static ArrayList<UpdateService.MasterOwner> getOwners(Context context)
    {
        int index;
        String code;
        String localeName;
        String[][] queryResult = runQuery(context, QueryId.GetOwners);
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
                sqlStatement.clearBindings();
                sqlStatement.bindString(1, satelliteValues.getAsString("[Code]"));
                sqlStatement.bindString(2, satelliteValues.getAsString("[Name]"));
                sqlStatement.executeInsert();

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
        String[][] queryResult = runQuery(context, QueryId.GetCategories);
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
                sqlStatement.clearBindings();
                sqlStatement.bindString(1, categoryValues.getAsString("[Name]"));
                sqlStatement.bindLong(2, categoryValues.getAsLong("[Indx]"));
                sqlStatement.executeInsert();

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
    public static String[][] getSatelliteCategoriesEnglish(Context context, int noradId)
    {
        return(runQuery(context, QueryId.GetSatelliteCategoriesEnglishNorad, noradId));
    }
    public static ArrayList<UpdateService.MasterSatelliteCategory> getSatelliteCategoriesEnglish(Context context)
    {
        int index;
        String[][] queryResult = runQuery(context, QueryId.GetSatelliteCategoriesEnglish);
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
                sqlStatement.clearBindings();
                sqlStatement.bindLong(1, satelliteValues.getAsInteger("[Norad]"));
                sqlStatement.bindLong(2, satelliteValues.getAsInteger("[Category_Index]"));
                sqlStatement.executeInsert();

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
            sqlStatement.clearBindings();
            sqlStatement.bindLong(1, infoValues.getAsInteger("[Norad]"));
            sqlStatement.bindLong(2, infoValues.getAsInteger("[Source]"));
            sqlStatement.bindString(3, infoValues.getAsString("[Language]"));
            sqlStatement.bindString(4, infoValues.getAsString("[Info]"));
            sqlStatement.executeInsert();
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
        String usedInfo;
        String[][] queryResult = runQuery(context, QueryId.GetInformationNoradLanguageSource, noradId, language, updateSource);

        //return any information
        info = (queryResult.length > 0 ? queryResult[0][0] : null);
        localeInfo = LocaleInformation.getInformation(context, noradId);
        usedInfo = (localeInfo != null ? localeInfo : info);
        return(usedInfo != null ? usedInfo.replace("</p><p>", "<p><br/></p>") : null);
    }
    public static String[][] getInformation(Context context, int noradId)
    {
        return(runQuery(context, QueryId.GetInformationNorad, noradId));
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
        String[][] queryResult = runQuery(context, QueryId.GetMasterSatellites);
        ArrayList<UpdateService.MasterSatellite> list = new ArrayList<>(0);

        //go through each satellite
        for(index = 0; index < queryResult.length; index++)
        {
            //remember current norad ID and satellite type
            currentID = Integer.parseInt(queryResult[index][0]);

            //create new satellite
            UpdateService.MasterSatellite newSatellite = new UpdateService.MasterSatellite(currentID, queryResult[index][1], queryResult[index][2], queryResult[index][3], (byte)-1, Long.parseLong(queryResult[index][4]));

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
                sqlStatement.clearBindings();
                sqlStatement.bindLong(1, satelliteValues.getAsInteger("[Norad]"));
                sqlStatement.bindString(2, satelliteValues.getAsString("[Name]"));
                sqlStatement.bindString(3, satelliteValues.getAsString("[Owner_Code]"));
                sqlStatement.bindString(4, satelliteValues.getAsString("[Launch_Date]"));
                sqlStatement.executeInsert();

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

    //Clears master satellite and associated tables
    public static void clearMasterSatelliteTable(Context context)
    {
        UpdateService.setMasterListTime(context, "", 0);
        runDelete(context, Tables.MasterSatellite, null, null);
        runDelete(context, Tables.Category, null, null);
        runDelete(context, Tables.SatelliteCategory, null, null);
    }
}