package com.nikolaiapps.orbtrack;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Map;
import java.util.TimeZone;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;


public abstract class Settings
{
    //Constants
    public static final int IconScaleMin = 20;
    public static final int IconScaleMax = 200;
    public static final int SpeedScaleMin = 0;
    public static final int SpeedScaleMax = 150;
    public static final int SensitivityScaleMin = 10;
    public static final int SensitivityScaleMax = 150;
    public static final int StarMagnitudeScaleMin = -20;
    public static final int StarMagnitudeScaleMax = 70;

    //Page types
    public static abstract class PageType
    {
        static final int Accounts = 0;
        static final int Locations = 1;
        static final int Notifications = 2;
        static final int Updates = 3;
        static final int Widgets = 4;
        static final int PageCount = 5;
    }

    //Preference names
    public static abstract class PreferenceName
    {
        static final String FirstRun = "FirstRun";
        static final String AcceptedPrivacy = "AcceptedPrivacy";
        static final String MaterialDesignNotice = "MaterialDesignNotice";
        static final String DarkTheme = "DarkTheme";
        static final String MaterialTheme = "MaterialTheme";
        static final String ColorTheme = "ColorTheme";
        static final String MetricUnits = "MetricUnits";
        static final String AllowNumberCommas = "AllowNumberCommas";
        static final String SatelliteIcon = "SatelliteIcon";
        static final String OrbitalIcons = "OrbitalIcons";
        static final String MapLayerType = "MapLayerType";
        static final String MapShow3dPaths = "MapShow3dPaths";
        static final String ShowSatelliteClouds = "ShowSatelliteClouds";
        static final String MapDisplayType = "MapDisplayType";
        static final String MapShowZoom = "MapShowZoom";
        static final String MapShowLabelsAlways = "MapShowLabelsAlways";
        static final String MapShowStars = "MapShowStars";
        static final String MapSpeedScale = "MapSpeedScale";
        static final String MapSensitivityScale = "MapSensitivityScale";
        static final String MapRotateAllowed = "MapRotateAllowed";
        static final String MapShowGrid = "MapShowGrid";
        static final String MapGridColor = "MapGridColor";
        static final String MapMarkerScale = "MapMarkerScale";
        static final String MapMarkerLocationIcon = "MapMarkerLocationIcon";
        static final String MapMarkerLocationIconUseTint = "MapMarkerLocationIconUseTint";
        static final String MapMarkerLocationIconTintColor = "MapMarkerLocationIconTintColor";
        static final String MapMarkerInfoLocation = "MapMarkerInfoLocation";
        static final String MapMarkerShowBackground = "MapMarkerShowBackground";
        static final String MapMarkerShowShadow = "MapMarkerShowShadow";
        static final String MapUpdateDelay = "MapUpdateDelay";
        static final String MapFrameRate = "MapFrameRate";
        static final String MapShowSunlight = "MapShowSunlight";
        static final String MapShowFootprint = "MapShowFootprint";
        static final String MapFootprintAlpha = "MapFootprintAlpha";
        static final String MapFootprintType = "MapFootprintType";
        static final String MapShowSelectedFootprint = "MapShowSelectedFootprint";
        static final String MapSelectedFootprintColor = "MapSelectedFootprintColor";
        static final String MapShowOrbitalDirection = "MapShowOrbitalDirection";
        static final String MapShowOrbitalDirectionLimit = "MapShowOrbitalDirectionLimit";
        static final String MapShowOrbitalDirectionUseLimit = "MapShowOrbitalDirectionUseLimit";
        static final String MapShowToolbars = "MapShowToolbars";
        static final String MapOrbitalTypeFilter = "MapOrbitalTypeFilter";
        static final String CurrentCombinedSortBy = "CurrentCombinedSortBy";
        static final String ListUpdateDelay = "ListUpdateDelay";
        static final String ListShowPassProgress = "ListShowPassProgress";
        static final String ListShowPassQuality = "ListShowPassQuality";
        static final String ListOrbitalTypeFilter = "ListOrbitalTypeFilter";
        static final String LensFirstRun = "LensFirstRun";
        static final String LensFirstCalibrate = "LensFirstCalibrate";
        static final String LensUpdateDelay = "LensUpdateDelay";
        static final String LensAverageCount = "LensAverageCount";
        static final String LensDirectionCentered = "LensDirectionCentered";
        static final String LensIndicator = "LensIndicator";
        static final String LensIndicatorIconShowDirection = "LensIndicatorIconShowDirection";
        static final String LensIndicatorAlpha = "LensIndicatorAlpha";
        static final String LensTextAlpha = "LensTextAlpha";
        static final String LensConstellationAlpha = "LensConstellationAlpha";
        static final String LensHorizonColor = "LensHorizonColor";
        static final String LensUseHorizon = "LensUseHorizon";
        static final String LensStarMagnitude = "LensStarMagnitude";
        static final String LensUseCamera = "LensUseCamera";
        static final String LensRotate = "LensRotate";
        static final String LensUseAutoWidth = "LensUseAutoWidth";
        static final String LensUseAutoHeight = "LensUseAutoHeight";
        static final String LensAzimuthUserOffset = "LensAzimuthUserOffset";
        static final String LensWidth = "LensWidth";
        static final String LensHeight = "LensHeight";
        static final String LensShowToolbars = "LensShowToolbars";
        static final String LensPathLabelType = "LensPathLabelType";
        static final String LensHideConstellationStarPaths = "LensHideConstellationStarPaths";
        static final String LensShowPathDirection = "LensShowPathDirection";
        static final String LensShowPathTimeNames = "LensShowPathTimeNames";
        static final String LensHideDistantPathTimes = "LensHideDistantPathTimes";
        static final String LensShowOutsideArea = "LensShowOutsideArea";
        static final String LensOrbitalTypeFilter = "LensOrbitalTypeFilter";
        static final String LocationLastLatitude = "LocationLatitude";
        static final String LocationLastLongitude = "LocationLastLongitude";
        static final String LocationLastAltitude = "LocationLastAltitude";
        static final String AltitudeSource = "AltitudeSource";
        static final String TimeZoneSource = "TimeZoneSource";
        static final String SatelliteSource = "SatelliteSource";
        static final String SatelliteSourceUseGP = "SatelliteSourceUseGP";
        static final String SatelliteDataSource = "SatelliteDataSource";
        static final String SatelliteSourceShared = "SatelliteSourceShared";
        static final String SatelliteNextDefaultColor = "SatelliteNextDefaultColor";
        static final String SatelliteUseNextDefaultColor = "SatelliteUseNextDefaultColor";
        static final String CatalogDebris = "CatalogDebris";
        static final String CatalogRocketBodies = "CatalogRocketBodies";
        static final String CatalogAutoUpdate = "CatalogAutoUpdate";
        static final String CatalogAutoUpdateRate = "CatalogAutoUpdateRate";
        static final String CatalogAutoUpdateHour = "CatalogAutoUpdateHour";
        static final String CatalogAutoUpdateMinute = "CatalogAutoUpdateMinute";
        static final String CatalogAutoUpdateNextMs = "CatalogAutoUpdateNextMs";
        static final String TLEAutoUpdate = "TLEAutoUpdate";
        static final String TLEAutoUpdateRate = "TLEAutoUpdateRate";
        static final String TLEAutoUpdateHour = "TLEAutoUpdateHour";
        static final String TLEAutoUpdateMinute = "TLEAutoUpdateMinute";
        static final String TLEAutoUpdateNextMs = "TLEAutoUpdateNextMs";
        static final String NotifyPassStartNextOnly = "NotifyPassStartNextOnly";
        static final String NotifyPassStartNextMs = "NotifyPassStartNextMs";
        static final String NotifyPassStartLatitude = "NotifyPassStartLatitude";
        static final String NotifyPassStartLongitude = "NotifyPassStartLongitude";
        static final String NotifyPassStartAltitude = "NotifyPassStartAltitude";
        static final String NotifyPassStartZoneId = "NotifyPassStartZoneId";
        static final String NotifyPassEndNextOnly = "NotifyPassEndNextOnly";
        static final String NotifyPassEndNextMs = "NotifyPassEndNextMs";
        static final String NotifyPassEndLatitude = "NotifyPassEndLatitude";
        static final String NotifyPassEndLongitude = "NotifyPassEndLongitude";
        static final String NotifyPassEndAltitude = "NotifyPassEndAltitude";
        static final String NotifyPassEndZoneId = "NotifyPassEndZoneId";
        static final String NotifyFullMoonStartNextOnly = "NotifyFullMoonStartNextOnly";
        static final String NotifyFullMoonStartNextMs = "NotifyFullMoonStartNextMs";
        static final String NotifyFullMoonStartLatitude = "NotifyFullMoonStartLatitude";
        static final String NotifyFullMoonStartLongitude = "NotifyFullMoonStartLongitude";
        static final String NotifyFullMoonStartAltitude = "NotifyFullMoonStartAltitude";
        static final String NotifyFullMoonStartZoneId = "NotifyFullMoonStartZoneId";
        static final String NotifyFullMoonEndNextOnly = "NotifyFullMoonEndNextOnly";
        static final String NotifyFullMoonEndNextMs = "NotifyFullMoonEndNextMs";
        static final String NotifyFullMoonEndLatitude = "NotifyFullMoonEndLatitude";
        static final String NotifyFullMoonEndLongitude = "NotifyFullMoonEndLongitude";
        static final String NotifyFullMoonEndAltitude = "NotifyFullMoonEndAltitude";
        static final String NotifyFullMoonEndZoneId = "NotifyFullMoonEndZoneId";
        static final String InformationSource = "InformationSource";
        static final String TranslateInformation = "TranslateInformation";
        static final String ShareTranslations = "ShareTranslations";
        static final String SpaceTrackUser = "SpaceTrackUser";
        static final String SpaceTrackPassword = "SpaceTrackPwd";
        static final String AskInternet = "askInternet";
        static final String UseGlobeCompatibility = "UseGlobeCompatibility";
    }

    //Sub preference name
    public static abstract class SubPreferenceName
    {
        static final String Map = "Map";
        static final String Globe = "Globe";
    }

    //Options
    public static abstract class Options
    {
        //Lens view
        public static abstract class LensView
        {
            //Indicator types
            public static abstract class IndicatorType
            {
                static final int Circle = 0;
                static final int Square = 1;
                static final int Triangle = 2;
                static final int Icon = 3;
            }

            //Path types
            public static abstract class PathLabelType
            {
                static final int FilledBox = 0;
                static final int ColorText = 1;
            }

            //Indicator items
            public static IconSpinner.Item[] indicatorItems;

            //Path type items
            public static IconSpinner.Item[] pathLabelTypeItems;

            //Sensor smoothing items
            public static IconSpinner.Item[] sensorSmoothingItems;

            //Initializes values
            public static void initValues(Context context)
            {
                Resources res = context.getResources();

                //if values are not set
                if(indicatorItems == null || indicatorItems.length == 0)
                {
                    //init indicator items
                    indicatorItems = new IconSpinner.Item[]
                    {
                        new IconSpinner.Item(Settings.getSatelliteIconImageId(context), Settings.getSatelliteIconImageIsThemeable(context), res.getString(R.string.title_icon), IndicatorType.Icon),
                        new IconSpinner.Item(R.drawable.shape_circle_black, res.getString(R.string.title_circle), IndicatorType.Circle),
                        new IconSpinner.Item(R.drawable.shape_square_black, res.getString(R.string.title_square), IndicatorType.Square),
                        new IconSpinner.Item(R.drawable.shape_triangle_black, res.getString(R.string.title_triangle), IndicatorType.Triangle)
                    };
                }
                if(pathLabelTypeItems == null || pathLabelTypeItems.length == 0)
                {
                    //set path type items
                    pathLabelTypeItems = new IconSpinner.Item[]
                    {
                        new IconSpinner.Item(res.getString(R.string.title_filled_box), PathLabelType.FilledBox),
                        new IconSpinner.Item(res.getString(R.string.title_color_text), PathLabelType.ColorText)
                    };
                }
                if(sensorSmoothingItems == null || sensorSmoothingItems.length == 0)
                {
                    //init sensor smoothing items
                    sensorSmoothingItems = new IconSpinner.Item[]
                    {
                        new IconSpinner.Item(res.getString(R.string.title_high), 80),
                        new IconSpinner.Item(res.getString(R.string.title_medium), 40),
                        new IconSpinner.Item(res.getString(R.string.title_low), 10)
                    };
                }
            }
        }

        //Display
        public static abstract class Display
        {
            //Location icon types
            public static abstract class LocationIcon
            {
                static final int Marker = 0;
                static final int Person = 1;
                static final int Home = 2;
                static final int City = 3;
                static final int Tower = 4;
                static final int Radar = 5;
                static final int Telescope1 = 6;
                static final int Telescope2 = 7;
                static final int Observatory1 = 8;
                static final int Observatory2 = 9;
                static final int Dish1 = 10;
                static final int Dish2 = 11;
                static final int Dish3 = 12;
                static final int Dish4 = 13;
            }
            public static IconSpinner.Item[] locationIconItems;

            //Satellite icon types
            public static abstract class SatelliteIcon
            {
                static final int Black = 0;
                static final int GrayBlue = 1;
                static final int Sputnik1 = 2;
                static final int Sputnik2 = 3;
                static final int Emoji = 4;
                static final int GrayOrange = 5;
                static final int Signal = 6;
            }
            public static IconSpinner.Item[] satelliteIconItems;

            //Orbital icons types
            public static abstract class OrbitalIcons
            {
                static final int Moozarov = 0;
                static final int Freepik = 1;
            }
            public static IconSpinner.Item[] orbitalIconsItems;

            //Theme colors
            private static abstract class ThemeIndex
            {
                static final int Blue = 0;
                static final int Brown = 1;
                static final int Cyan = 2;
                static final int Green = 3;
                static final int Grey = 4;
                static final int Orange = 5;
                static final int Purple = 6;
                static final int Pink = 7;
                static final int Red = 8;
                static final int Yellow = 9;
            }
            public static IconSpinner.Item[] colorAdvancedItems;

            //Initializes values
            public static void initValues(Context context)
            {
                int color;
                Resources res = context.getResources();

                //if values are not set
                if(locationIconItems == null || locationIconItems.length == 0)
                {
                    //init location icon items
                    color = getMapMarkerLocationIconUsedTintColor(context, LocationIcon.Person);
                    locationIconItems = new IconSpinner.Item[]
                    {
                        new IconSpinner.Item(R.drawable.map_location_marker_red, Color.TRANSPARENT, LocationIcon.Marker),
                        new IconSpinner.Item(R.drawable.map_location_person_red, color, LocationIcon.Person),
                        new IconSpinner.Item(R.drawable.map_location_home_red, color, LocationIcon.Home),
                        new IconSpinner.Item(R.drawable.map_location_city_red, color, LocationIcon.City),
                        new IconSpinner.Item(R.drawable.map_location_tower_red, color, LocationIcon.Tower),
                        new IconSpinner.Item(R.drawable.map_location_radar_red, color, LocationIcon.Radar),
                        new IconSpinner.Item(R.drawable.map_location_telescope1, color, LocationIcon.Telescope1),
                        new IconSpinner.Item(R.drawable.map_location_telescope2, color, LocationIcon.Telescope2),
                        new IconSpinner.Item(R.drawable.map_location_observatory1, color, LocationIcon.Observatory1),
                        new IconSpinner.Item(R.drawable.map_location_observatory2, color, LocationIcon.Observatory2),
                        new IconSpinner.Item(R.drawable.map_location_dish1, color, LocationIcon.Dish1),
                        new IconSpinner.Item(R.drawable.map_location_dish2, Color.TRANSPARENT, LocationIcon.Dish2),
                        new IconSpinner.Item(R.drawable.map_location_dish3, Color.TRANSPARENT, LocationIcon.Dish3),
                        new IconSpinner.Item(R.drawable.map_location_dish4, Color.TRANSPARENT, LocationIcon.Dish4)
                    };
                }
                if(satelliteIconItems == null || satelliteIconItems.length == 0)
                {
                    //init satellite icon items
                    satelliteIconItems = new IconSpinner.Item[]
                    {
                        new IconSpinner.Item(R.drawable.orbital_satellite_black, true, SatelliteIcon.Black),
                        new IconSpinner.Item(R.drawable.orbital_satellite_gray_blue, false, SatelliteIcon.GrayBlue),
                        new IconSpinner.Item(R.drawable.orbital_satellite_sputnik1, true, SatelliteIcon.Sputnik1),
                        new IconSpinner.Item(R.drawable.orbital_satellite_sputnik2, true, SatelliteIcon.Sputnik2),
                        new IconSpinner.Item(R.drawable.orbital_satellite_emoji, false, SatelliteIcon.Emoji),
                        new IconSpinner.Item(R.drawable.orbital_satellite_gray_orange, false, SatelliteIcon.GrayOrange),
                        new IconSpinner.Item(R.drawable.orbital_satellite_signal, false, SatelliteIcon.Signal)
                    };
                }
                if(orbitalIconsItems == null || orbitalIconsItems.length == 0)
                {
                    //init orbital icon items
                    orbitalIconsItems = new IconSpinner.Item[]
                    {
                        new IconSpinner.Item(Globals.getDrawableCombined(context, new int[]{R.drawable.orbital_sun_moozarov, R.drawable.orbital_mercury_moozarov, R.drawable.orbital_venus_moozarov, R.drawable.orbital_moon_moozarov, R.drawable.orbital_mars_moozarov, R.drawable.orbital_jupiter_moozarov, R.drawable.orbital_saturn_moozarov, R.drawable.orbital_uranus_moozarov, R.drawable.orbital_neptune_moozarov, R.drawable.orbital_pluto_moozarov}), null, OrbitalIcons.Moozarov),
                        new IconSpinner.Item(Globals.getDrawableCombined(context, new int[]{R.drawable.orbital_sun_freepik, R.drawable.orbital_mercury_freepik, R.drawable.orbital_venus_freepik, R.drawable.orbital_moon_freepik, R.drawable.orbital_mars_freepik, R.drawable.orbital_jupiter_freepik, R.drawable.orbital_saturn_freepik, R.drawable.orbital_uranus_freepik, R.drawable.orbital_neptune_freepik, R.drawable.orbital_pluto_freepik}), null, OrbitalIcons.Freepik),
                    };
                }
                if(colorAdvancedItems == null || colorAdvancedItems.length == 0)
                {
                    //init color advanced items
                    colorAdvancedItems = new IconSpinner.Item[]
                    {
                        new IconSpinner.Item(context, R.color.pink, res.getString(R.string.title_pink), Display.ThemeIndex.Pink),
                        new IconSpinner.Item(context, R.color.red, res.getString(R.string.title_red), Display.ThemeIndex.Red),
                        new IconSpinner.Item(context, R.color.orange, res.getString(R.string.title_orange), Display.ThemeIndex.Orange),
                        new IconSpinner.Item(context, R.color.yellow, res.getString(R.string.title_yellow), Display.ThemeIndex.Yellow),
                        new IconSpinner.Item(context, R.color.green, res.getString(R.string.title_green), Display.ThemeIndex.Green),
                        new IconSpinner.Item(context, R.color.cyan, res.getString(R.string.title_cyan), Display.ThemeIndex.Cyan),
                        new IconSpinner.Item(context, R.color.blue, res.getString(R.string.title_blue), Display.ThemeIndex.Blue),
                        new IconSpinner.Item(context, R.color.purple, res.getString(R.string.title_purple), Display.ThemeIndex.Purple),
                        new IconSpinner.Item(context, R.color.brown, res.getString(R.string.title_brown), Display.ThemeIndex.Brown),
                        new IconSpinner.Item(context, R.color.grey, res.getString(R.string.title_grey), Display.ThemeIndex.Grey)
                    };
                }
            }

            //Sets the theme
            public static void setTheme(Context context)
            {
                boolean darkTheme = getDarkTheme(context);
                int colorTheme = getColorTheme(context);
                int themeID;

                //get theme ID
                switch(colorTheme)
                {
                    case ThemeIndex.Blue:
                        themeID = (darkTheme ? R.style.DarkBlue : R.style.LightBlue);
                        break;

                    case ThemeIndex.Brown:
                        themeID = (darkTheme ? R.style.DarkBrown : R.style.LightBrown);
                        break;

                    case ThemeIndex.Green:
                        themeID = (darkTheme ? R.style.DarkGreen : R.style.LightGreen);
                        break;

                    case ThemeIndex.Grey:
                        themeID = (darkTheme ? R.style.DarkGrey : R.style.LightGrey);
                        break;

                    case ThemeIndex.Orange:
                        themeID = (darkTheme ? R.style.DarkOrange : R.style.LightOrange);
                        break;

                    case ThemeIndex.Purple:
                        themeID = (darkTheme ? R.style.DarkPurple : R.style.LightPurple);
                        break;

                    case ThemeIndex.Pink:
                        themeID = (darkTheme ? R.style.DarkPink : R.style.LightPink);
                        break;

                    case ThemeIndex.Red:
                        themeID = (darkTheme ? R.style.DarkRed : R.style.LightRed);
                        break;

                    case ThemeIndex.Yellow:
                        themeID = (darkTheme ? R.style.DarkYellow : R.style.LightYellow);
                        break;

                    default:
                    case ThemeIndex.Cyan:
                        themeID = (darkTheme ? R.style.DarkCyan : R.style.LightCyan);
                        break;
                }

                context.setTheme(themeID);
            }
        }

        //Accounts
        public static abstract class Accounts
        {
            //Item
            public static class Item extends Selectable.ListDisplayItem
            {
                final String loginName;

                public Item(int accType, String name)
                {
                    super(accType, -1, (accType != Globals.AccountType.None), false, false, false);

                    loginName = name;
                }

                public int getIconId()
                {
                    switch(id)
                    {
                        case Globals.AccountType.GoogleDrive:
                            return(R.drawable.org_google_drive);

                        case Globals.AccountType.Dropbox:
                            return(R.drawable.org_dropbox);

                        case Globals.AccountType.SpaceTrack:
                            return(R.drawable.org_space_track);

                        default:
                        case Globals.AccountType.None:
                            return(-1);
                    }
                }
            }

            //Item holder
            public static class ItemHolder extends Selectable.ListDisplayItemHolder
            {
                final AppCompatImageView accountImage;
                final TextView nameText;

                public ItemHolder(View itemView)
                {
                    super(itemView, -1);

                    //get displays
                    accountImage = itemView.findViewById(R.id.Accounts_Item_Image);
                    nameText = itemView.findViewById(R.id.Accounts_Item_Name_Text);
                }
            }

            //Item list adapter
            public static class ItemListAdapter extends Selectable.ListBaseAdapter
            {
                private Item[] items;
                private Activity activity;
                private Selectable.ListFragment currentPage;

                public ItemListAdapter(Selectable.ListFragment page, String title)
                {
                    super(page.getContext(), title);

                    //if activity exists
                    if(currentContext instanceof Activity)
                    {
                        //remember page and activity
                        currentPage = page;
                        activity = (Activity)currentContext;

                        //setup items
                        initItems();
                        this.itemsRefID = R.layout.settings_accounts_item;
                    }
                }

                private void initItems()
                {
                    String accountName;
                    Resources res = currentContext.getResources();
                    GoogleSignInAccount googleDriveAccount = GoogleSignIn.getLastSignedInAccount(currentContext);
                    ArrayList<Item> accountList = new ArrayList<>(0);
                    String[] loginData;

                    //get google drive
                    if(googleDriveAccount != null)
                    {
                        //if can get email
                        accountName = googleDriveAccount.getDisplayName();
                        if(accountName != null)
                        {
                            //add email
                            accountList.add(new Item(Globals.AccountType.GoogleDrive, accountName));
                        }
                    }

                    //get dropbox
                    accountName = DropboxAccess.getUserEmail(currentContext);
                    if(accountName != null)
                    {
                        //add name
                        accountList.add(new Item(Globals.AccountType.Dropbox, accountName));
                    }

                    //get space-track
                    loginData = getLogin(currentContext, Globals.AccountType.SpaceTrack);
                    if(loginData[0] != null && loginData[0].trim().length() > 0 && loginData[1] != null && loginData[1].trim().length() > 0)
                    {
                        //add email
                        accountList.add(new Item(Globals.AccountType.SpaceTrack, loginData[0]));
                    }

                    //if no accounts
                    if(accountList.size() == 0)
                    {
                        //add empty
                        accountList.add(new Item(Globals.AccountType.None, res.getString(R.string.title_none)));
                    }

                    //set items
                    items = accountList.toArray(new Item[0]);
                }

                //Reloads items
                @SuppressLint("NotifyDataSetChanged")
                public void reload()
                {
                    initItems();
                    notifyDataSetChanged();
                    currentPage.cancelEditMode();
                }

                @Override
                protected boolean showColumnTitles(int page)
                {
                    return(categoryTitle == null);
                }

                @Override
                protected void setColumnTitles(ViewGroup listColumns, TextView categoryText, int page)
                {
                    //if have category
                    if(categoryText != null && categoryTitle != null)
                    {
                        categoryText.setText(categoryTitle);
                        ((View)categoryText.getParent()).setVisibility(View.VISIBLE);
                    }
                    else
                    {
                        AppCompatImageView imageColumn = listColumns.findViewById(R.id.Accounts_Item_Image);

                        imageColumn.setVisibility(View.INVISIBLE);
                        imageColumn.setImageResource(R.drawable.org_google_drive);
                        ((TextView)listColumns.findViewById(R.id.Accounts_Item_Name_Text)).setText(R.string.title_account);
                    }

                    super.setColumnTitles(listColumns, categoryText, page);
                }

                @Override
                protected void onItemNonEditClick(Selectable.ListDisplayItem item, int pageNum)
                {
                    final Item currentItem = (Item)item;
                    final int accountType = currentItem.id;
                    final int iconId = currentItem.getIconId();
                    final Resources res = currentContext.getResources();
                    final String loginName = currentItem.loginName;

                    //if a known account
                    if(accountType != Globals.AccountType.None)
                    {
                        //get action
                        Globals.showSelectDialog(currentContext, iconId, loginName, AddSelectListAdapter.SelectType.EditAccount, accountType, new AddSelectListAdapter.OnItemClickListener()
                        {
                            @Override
                            public void onItemClick(int which)
                            {
                                //handle based on selection
                                switch(which)
                                {
                                    case AddSelectListAdapter.EditAccountType.Edit:
                                        //edit account
                                        editAccount(accountType);
                                        break;

                                    case AddSelectListAdapter.EditAccountType.Remove:
                                        //confirm removal
                                        Globals.showConfirmDialog(currentContext, Globals.getDrawable(currentContext, iconId), res.getString(R.string.title_account_remove_question), loginName, res.getString(R.string.title_yes), res.getString(R.string.title_no), true, new DialogInterface.OnClickListener()
                                        {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which)
                                            {
                                                //remove account
                                                removeAccount(accountType);
                                            }
                                        }, null, null);
                                        break;
                                }
                            }
                        });
                    }
                }

                @Override
                public @NonNull RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
                {
                    View itemView = LayoutInflater.from(parent.getContext()).inflate(this.itemsRefID, parent, false);
                    ItemHolder itemHolder = new ItemHolder(itemView);

                    setItemSelector(itemView);
                    setViewClickListeners(itemView, itemHolder);
                    return(itemHolder);
                }

                @Override
                public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position)
                {
                    Item currentItem = items[position];
                    ItemHolder itemHolder = (ItemHolder)holder;
                    int imageId = currentItem.getIconId();

                    //set displays
                    itemHolder.nameText.setText(currentItem.loginName);
                    if(imageId != -1)
                    {
                        itemHolder.accountImage.setImageResource(imageId);
                    }
                    else
                    {
                        itemHolder.accountImage.setImageDrawable(null);
                    }

                    //set background
                    setItemBackground(itemHolder.itemView, false);
                }

                @Override
                public int getItemCount()
                {
                    return(items.length);
                }

                @Override
                public Item getItem(int position)
                {
                    return(items[position]);
                }

                //Edits given account
                public void editAccount(int accountType)
                {
                    //handle based on account
                    if(accountType == Globals.AccountType.SpaceTrack)
                    {
                        Globals.showAccountLogin(activity, accountType, new Globals.WebPageListener()
                        {
                            @Override
                            public void onResult(Globals.WebPageData pageData, boolean success)
                            {
                                //update list
                                activity.runOnUiThread(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        //reload items
                                        reload();
                                    }
                                });
                            }
                        });
                    }
                }

                //Returns if there are any non edit items selected
                public boolean haveNonEditItemsSelected()
                {
                    //go through each item
                    for(Item currentItem : items)
                    {
                        //if something other than space-track
                        if(currentItem.isSelected && currentItem.id != Globals.AccountType.SpaceTrack)
                        {
                            //found non edit
                            return(true);
                        }
                    }

                    //none found
                    return(false);
                }

                //Returns all used accounts
                public ArrayList<Integer> getUsedAccounts()
                {
                    int accountType;
                    ArrayList<Integer> used = new ArrayList<>(0);

                    //go through each item
                    for(Item currentItem : items)
                    {
                        //get account type and validity
                        accountType = currentItem.id;
                        if(accountType < Globals.AccountType.Count)
                        {
                            //add used
                            used.add(accountType);
                        }
                    }

                    //return status
                    return(used);
                }

                //Removes given account
                public void removeAccount(int accountType)
                {
                    //handle based on account
                    switch(accountType)
                    {
                        case Globals.AccountType.GoogleDrive:
                            Globals.getGoogleDriveSignInClient(activity).signOut();
                            break;

                        case Globals.AccountType.Dropbox:
                            DropboxAccess.removeAccount(currentContext);
                            break;

                        case Globals.AccountType.SpaceTrack:
                            removeSpaceTrackLogin(currentContext);
                            break;
                    }

                    //reload items
                    reload();
                }
            }
        }

        //Map view
        public static abstract class MapView
        {
            //Footprint types
            public static abstract class FootprintType
            {
                static final int Outline = 0;
                static final int Filled = 1;
                static final int OutlineFilled = 2;
            }

            //Map types
            public static String[] mapTypeItems;
            public static final Integer[] MapTypeValues = new Integer[]{CoordinatesFragment.MapLayerType.Normal, CoordinatesFragment.MapLayerType.Satellite, /*CoordinatesFragment.MapLayerType.Terrain,*/ CoordinatesFragment.MapLayerType.Hybrid};

            //Footprint types
            public static String[] footprintTypeItems;
            public static final Integer[] FootprintTypeValues = new Integer[]{FootprintType.Outline, FootprintType.Filled, FootprintType.OutlineFilled};

            //Information location types
            public static String[] infoLocationItems;
            public static final Integer[] InfoLocationValues = new Integer[]{CoordinatesFragment.MapMarkerInfoLocation.None, CoordinatesFragment.MapMarkerInfoLocation.UnderTitle, CoordinatesFragment.MapMarkerInfoLocation.ScreenBottom};

            //Initializes values
            public static void initValues(Context context)
            {
                Resources res = context.getResources();

                //if values are not set
                if(mapTypeItems == null || mapTypeItems.length == 0)
                {
                    //init map type items
                    mapTypeItems = new String[]
                    {
                        res.getString(R.string.title_normal),
                        res.getQuantityString(R.plurals.title_satellites, 1),
                        /*res.getString(R.string.title_terrain),*/
                        res.getString(R.string.title_hybrid)
                    };
                }
                if(footprintTypeItems == null || footprintTypeItems.length == 0)
                {
                    //init footprint type items
                    footprintTypeItems = new String[]
                    {
                        res.getString(R.string.title_outline),
                        res.getString(R.string.title_filled),
                        res.getString(R.string.title_both)
                    };
                }
                if(infoLocationItems == null || infoLocationItems.length == 0)
                {
                    //init info location items
                    infoLocationItems = new String[]
                    {
                        res.getString(R.string.title_none),
                        res.getString(R.string.title_under_name),
                        res.getString(R.string.title_screen_bottom)
                    };
                }
            }
        }

        //Sources
        public static abstract class Sources
        {
            static final String MapQuest = "MapQuest";
            static final String Google = "Google";
            static final String GeoNames = "GeoNames";
            static final String Celestrak = Globals.Strings.Celestrak;
            static final String N2YO = Globals.Strings.N2YO;
            static final String SpaceTrack = Globals.Strings.SpaceTrack;
            static final String HeavensAbove = Globals.Strings.HeavensAbove;
            static final String NASA = "NASA";
        }

        //Updates
        public static abstract class Updates
        {
            //Altitude sources
            public static final String[] AltitudeSourceItems = new String[]{Sources.MapQuest, Sources.Google};
            public static final Byte[] AltitudeSourceValues = new Byte[]{LocationService.OnlineSource.MapQuest, LocationService.OnlineSource.Google};
            public static final int[] AltitudeSourceImageIds = new int[]{R.drawable.org_mapquest, R.drawable.org_google};

            //Time zone sources
            public static final String[] TimeZoneSourceItems = new String[]{Sources.GeoNames, Sources.Google};
            public static final Byte[] TimeZoneSourceValues = new Byte[]{LocationService.OnlineSource.GeoNames, LocationService.OnlineSource.Google};
            public static final int[] TimeZoneSourceImageIds = new int[]{R.drawable.org_geonames, R.drawable.org_google};

            //Satellite sources
            public static final String[] SatelliteSourceItems = new String[]{Sources.Celestrak, Sources.N2YO, Sources.SpaceTrack};
            public static final Byte[] SatelliteSourceValues = new Byte[]{Database.UpdateSource.Celestrak, Database.UpdateSource.N2YO, Database.UpdateSource.SpaceTrack};
            public static final int[] SatelliteSourceImageIds = new int[]{R.drawable.org_celestrak, R.drawable.org_n2yo, R.drawable.org_space_track};
            public static String[] SatelliteSourceSubTexts;

            //Satellite data sources
            public static final String[] SatelliteDataSourceItems = new String[]{Sources.Celestrak, Sources.HeavensAbove, Sources.N2YO, Sources.SpaceTrack};
            public static final Byte[] SatelliteDataSourceValues = new Byte[]{Database.UpdateSource.Celestrak, Database.UpdateSource.HeavensAbove, Database.UpdateSource.N2YO, Database.UpdateSource.SpaceTrack};
            public static final int[] SatelliteDataSourceImageIds = new int[]{R.drawable.org_celestrak, R.drawable.org_heavens_above, R.drawable.org_n2yo, R.drawable.org_space_track};
            public static String[] SatelliteDataSourceSubTexts;

            //Information sources
            public static final String[] InformationSourceItems = new String[]{Sources.NASA, Sources.Celestrak, Sources.N2YO};
            public static final Byte[] InformationSourceValues = new Byte[]{Database.UpdateSource.NASA, Database.UpdateSource.Celestrak, Database.UpdateSource.N2YO};
            public static final int[] InformationSourceImageIds = new int[]{R.drawable.org_nasa, R.drawable.org_celestrak, R.drawable.org_n2yo};

            //Update frequencies
            private static final long MsPerDay = (long)Calculations.MsPerDay;
            private static final long MsPer3Days = MsPerDay * 3;
            private static final long MsPer4Weeks = MsPerDay * 28;

            //Initializes values
            public static void initValues(Context context)
            {
                Resources res = context.getResources();
                String qualityString = res.getString(R.string.title_quality) + ":";
                String speedString = "  " + res.getString(R.string.title_speed) + ":";

                //if values are not set
                if(SatelliteSourceSubTexts == null || SatelliteSourceSubTexts.length == 0)
                {
                    //init satellite source sub texts
                    SatelliteSourceSubTexts = new String[]
                    {
                        qualityString + Globals.getStars(2) + speedString + Globals.getStars(2),
                        qualityString + Globals.getStars(1) + speedString + Globals.getStars(2),
                        qualityString + Globals.getStars(3) + speedString + Globals.getStars(3) + " " + Globals.Symbols.Lock
                    };
                }
                if(SatelliteDataSourceSubTexts == null || SatelliteDataSourceSubTexts.length == 0)
                {
                    //init satellite data source sub texts
                    SatelliteDataSourceSubTexts = new String[]
                    {
                        qualityString + Globals.getStars(2) + speedString + Globals.getStars(2),
                        qualityString + Globals.getStars(2) + speedString + Globals.getStars(2),
                        qualityString + Globals.getStars(1) + speedString + Globals.getStars(2),
                        qualityString + Globals.getStars(3) + speedString + Globals.getStars(3) + " " + Globals.Symbols.Lock
                    };
                }
            }
        }

        //Rates
        public static abstract class Rates
        {
            //Update rates
            public static IconSpinner.Item[] frameRateItems;
            public static IconSpinner.Item[] updateRateItems;

            //Initializes values
            public static void initValues(Context context)
            {
                Resources res = context.getResources();

                //if values are not set
                if(frameRateItems == null || frameRateItems.length == 0)
                {
                    //init frame rate items
                    frameRateItems = new IconSpinner.Item[]
                    {
                        new IconSpinner.Item(res.getString(R.string.title_very_slow), res.getQuantityString(R.plurals.title_fps, 10, 10), 10),
                        new IconSpinner.Item(res.getString(R.string.title_slow), res.getQuantityString(R.plurals.title_fps, 24, 24), 24),
                        new IconSpinner.Item(res.getString(R.string.title_average), res.getQuantityString(R.plurals.title_fps, 30, 30), 30),
                        new IconSpinner.Item(res.getString(R.string.title_fast), res.getQuantityString(R.plurals.title_fps, 45, 45), 45),
                        new IconSpinner.Item(res.getString(R.string.title_very_fast), res.getQuantityString(R.plurals.title_fps, 60, 60), 60)
                    };
                }
                if(updateRateItems == null || updateRateItems.length == 0)
                {
                    //init rate items
                    updateRateItems = new IconSpinner.Item[]
                    {
                        new IconSpinner.Item(res.getString(R.string.title_very_slow), res.getString(R.string.title_5_seconds), 5000),
                        new IconSpinner.Item(res.getString(R.string.title_slow), res.getString(R.string.title_2_seconds), 2000),
                        new IconSpinner.Item(res.getString(R.string.title_average), res.getString(R.string.title_1_second), 1000),
                        new IconSpinner.Item(res.getString(R.string.title_fast), res.getString(R.string.title_500_ms), 500),
                        new IconSpinner.Item(res.getString(R.string.title_very_fast), res.getString(R.string.title_100_ms), 100),
                        new IconSpinner.Item(res.getString(R.string.title_immediate), res.getString(R.string.title_20_ms), 20)
                    };
                }
            }
        }
    }

    //Locations
    public static abstract class Locations
    {
        //Item
        public static class Item extends Selectable.ListDisplayItem
        {
            public final String name;
            public double latitude;
            public double longitude;
            public double altitudeM;
            public final byte locationType;
            public final TimeZone zone;

            public Item(int id, int index, String nm, double lat, double lon, double altM, String znId, byte lType, boolean canEdit, boolean selected, boolean checked)
            {
                super(id, index, canEdit, selected, true, checked);
                name = nm;
                latitude = lat;
                longitude = lon;
                altitudeM = altM;
                zone = (znId != null ? TimeZone.getTimeZone(znId) : TimeZone.getDefault());
                locationType = lType;
            }
        }

        //Item holder
        public static class ItemHolder extends Selectable.ListDisplayItemHolder
        {
            final AppCompatImageView locationImage;
            final TextView locationText;

            public ItemHolder(View itemView, int checkBoxID, int locationImageID, int locationTextID)
            {
                super(itemView, checkBoxID);
                locationImage = itemView.findViewById(locationImageID);
                locationText = itemView.findViewById(locationTextID);
            }
        }

        //Item list adapter
        public static class ItemListAdapter extends Selectable.ListBaseAdapter
        {
            private double knownLatitude;
            private double knownLongitude;
            private double knownAltitude;
            private final int columnTitleStringId;
            private Item[] locations;

            public ItemListAdapter(View parentView, int titleStringId, String categoryTitle)
            {
                super(parentView, categoryTitle);

                //set pending data and title
                knownLatitude = Double.MAX_VALUE;
                knownLongitude = Double.MAX_VALUE;
                knownAltitude = Double.MAX_VALUE;
                columnTitleStringId = titleStringId;

                this.itemsRefID = R.layout.settings_location_item;
                reload();
            }
            public ItemListAdapter(View parentView, String title)
            {
                this(parentView, -1, title);
            }

            @Override
            public int getItemCount()
            {
                return(locations != null ? locations.length : 0);
            }

            @Override
            public Item getItem(int position)
            {
                return(locations != null ? locations[position] : null);
            }

            @Override
            public long getItemId(int position)
            {
                return(-1);
            }

            @Override
            protected boolean showColumnTitles(int page)
            {
                return(categoryTitle == null);
            }

            @Override
            protected void setColumnTitles(ViewGroup listColumns, TextView categoryText, int page)
            {
                //if have category
                if(categoryText != null && categoryTitle != null)
                {
                    categoryText.setText(categoryTitle);
                    ((View)categoryText.getParent()).setVisibility(View.VISIBLE);
                }
                else
                {
                    AppCompatImageView imageColumn = listColumns.findViewById(R.id.Location_Item_Image);

                    imageColumn.setVisibility(View.INVISIBLE);
                    imageColumn.setImageResource(R.drawable.ic_my_location_black);
                    ((TextView)listColumns.findViewById(R.id.Location_Item_Text)).setText(columnTitleStringId > 0 ? columnTitleStringId : R.string.title_name);
                    listColumns.findViewById(R.id.Location_Item_CheckBox).setVisibility(View.INVISIBLE);
                }

                super.setColumnTitles(listColumns, categoryText, page);
            }

            private void setText(TextView view, String text)
            {
                if(view != null)
                {
                    view.setText(text);
                }
            }

            @Override
            protected void onItemNonEditClick(Selectable.ListDisplayItem item, int pageNum)
            {
                final Item currentItem = (Item)item;
                final boolean usingContext = haveContext();
                final boolean isCurrent = (currentItem.locationType == Database.LocationType.Current);
                double latitude = currentItem.latitude;
                double longitude = currentItem.longitude;
                double altitudeM = currentItem.altitudeM;
                final Resources res = (usingContext ? currentContext.getResources() : null);
                final TextView[] texts;

                //if on current location and not set yet
                if(isCurrent && (latitude == Double.MAX_VALUE || longitude == Double.MAX_VALUE || altitudeM == Double.MAX_VALUE))
                {
                    //try to use MainActivity location
                    Calculations.ObserverType currentLocation = MainActivity.getObserver();
                    if(currentLocation != null && currentLocation.geo.isSet())
                    {
                        Calculations.GeodeticDataType currentGeo = currentLocation.geo;
                        latitude = currentGeo.latitude;
                        longitude = currentGeo.longitude;
                        altitudeM = currentGeo.altitudeKm / 1000;
                    }
                }

                //if have context and -a valid latitude, longitude, or altitude-
                if(usingContext && ((latitude != 0 && latitude >= -90 && latitude <= 90) || (longitude != 0 && longitude >= -180 && longitude <= 180) || (altitudeM != 0 && altitudeM != Double.MAX_VALUE)))
                {
                    //create dialog
                    final ItemDetailDialog detailDialog = new ItemDetailDialog(currentContext, listInflater, Universe.IDs.None, currentItem.name, null, Globals.getDrawable(currentContext, Globals.getLocationIcon(currentItem.locationType), true), itemDetailButtonClickListener);

                    //add titles
                    detailDialog.addGroup(res.getString(R.string.title_name), null,
                                                     res.getString(R.string.title_latitude), res.getString(R.string.title_altitude),
                                                     res.getString(R.string.title_longitude), null,
                                                     res.getString(R.string.title_time_zone));

                    //get displays
                    texts = detailDialog.getDetailTexts();
                    if(texts != null && texts.length > 6)
                    {
                        //update displays
                        setText(texts[2], Globals.getLatitudeDirectionString(res, latitude, 4));
                        setText(texts[3], Globals.getNumberString(Globals.getMetersUnitValue(altitudeM)) + " " + Globals.getMetersLabel(res));
                        setText(texts[4], Globals.getLongitudeDirectionString(res, longitude, 4));

                        //get location name
                        if(isCurrent && (currentContext instanceof Activity))
                        {
                            //resolve location
                            AddressUpdateService.getResolvedLocation(currentContext, latitude, longitude, new AddressUpdateService.OnLocationResolvedListener()
                            {
                                @Override
                                public void onLocationResolved(final String locationString, final int resultCode)
                                {
                                    ((Activity)currentContext).runOnUiThread(new Runnable()
                                    {
                                        @Override
                                        public void run()
                                        {
                                            boolean success = (resultCode == AddressUpdateService.RESULT_SUCCESS) ;
                                            final String unknown = Globals.getUnknownString(currentContext);

                                            //set name and timezone
                                            setText(texts[0], (success ? locationString : unknown));
                                            setText(texts[6], (success ? currentItem.zone.getDisplayName() : unknown));
                                        }
                                    });
                                }
                            }, false);
                        }
                        else
                        {
                            //use existing name and timezone
                            setText(texts[0], currentItem.name);
                            setText(texts[6], currentItem.zone.getDisplayName());
                        }
                    }

                    //update display visibilities
                    detailDialog.show();
                }
            }

            @Override
            public @NonNull ItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
            {
                View itemView = LayoutInflater.from(parent.getContext()).inflate(this.itemsRefID, parent, false);
                ItemHolder itemHolder = new ItemHolder(itemView, R.id.Location_Item_CheckBox, R.id.Location_Item_Image, R.id.Location_Item_Text);

                setItemSelector(itemView);
                setViewClickListeners(itemView, itemHolder);
                return(itemHolder);
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position)
            {
                Item currentItem = locations[position];
                ItemHolder itemHolder = (ItemHolder)holder;

                //get displays
                currentItem.checkBoxView = itemHolder.checkBoxView;
                if(currentItem.locationType == Database.LocationType.Current)
                {
                    //update current item
                    currentItem.latitude = knownLatitude;
                    currentItem.longitude = knownLongitude;
                    currentItem.altitudeM = knownAltitude;
                }

                //set displays
                itemHolder.locationImage.setImageResource(Globals.getLocationIcon(currentItem.locationType));
                itemHolder.locationText.setText(currentItem.name);
                currentItem.setChecked(currentItem.isChecked || locations.length == 1);

                //set background
                setItemBackground(itemHolder.itemView, currentItem.isSelected);
            }

            //Reloads items
            @SuppressLint("NotifyDataSetChanged")
            public void reload()
            {
                int index;
                Database.DatabaseLocation[] dbLocations = Database.getLocations(currentContext);

                //setup items
                locations = new Item[dbLocations.length];
                for(index = 0; index < dbLocations.length; index++)
                {
                    Database.DatabaseLocation currentLoc = dbLocations[index];
                    locations[index] = new Item(currentLoc.id, index, currentLoc.name, currentLoc.latitude, currentLoc.longitude, currentLoc.altitudeKM * 1000, currentLoc.zoneId, currentLoc.locationType, (columnTitleStringId < 0 && currentLoc.locationType != Database.LocationType.Current), false, currentLoc.isChecked);
                }

                //update display
                notifyDataSetChanged();
            }

            //Sets current location displays
            public void setCurrentLocation(double lat, double lon, double altM)
            {
                //remember known data
                knownLatitude = lat;
                knownLongitude = lon;
                knownAltitude = altM;
            }
        }
    }

    //Notifications
    public static abstract class Notifications
    {
        //Item
        public static class Item extends Selectable.ListDisplayItem
        {
            public final String name;
            public final CalculateService.AlarmNotifySettings passStartSettings;
            public final CalculateService.AlarmNotifySettings passEndSettings;
            public final CalculateService.AlarmNotifySettings fullStartSettings;
            public final CalculateService.AlarmNotifySettings fullEndSettings;

            public Item(int id, String nm, CalculateService.AlarmNotifySettings passStartSettings, CalculateService.AlarmNotifySettings passEndSettings, CalculateService.AlarmNotifySettings fullStartSettings, CalculateService.AlarmNotifySettings fullEndSettings)
            {
                super(id, -1, true, false, true, false);
                this.name = nm;
                this.passStartSettings = passStartSettings;
                this.passEndSettings = passEndSettings;
                this.fullStartSettings = fullStartSettings;
                this.fullEndSettings = fullEndSettings;
            }
        }

        //Item holder
        public static class ItemHolder extends Selectable.ListDisplayItemHolder
        {
            final AppCompatImageView notifyImage;
            final TextView nameText;
            final TextView passText;
            final TextView passStartText;
            final TextView passEndText;
            final TextView fullText;
            final TextView fullStartText;
            final TextView fullEndText;

            public ItemHolder(View itemView, int notifyImageId, int notifyTextId, int passTextId, int passStartTextId, int passEndTextId, int fullTextId, int fullStartTextId, int fullEndTextId)
            {
                super(itemView, -1);
                notifyImage = itemView.findViewById(notifyImageId);
                nameText = itemView.findViewById(notifyTextId);
                passText = itemView.findViewById(passTextId);
                passStartText = itemView.findViewById(passStartTextId);
                passEndText = itemView.findViewById(passEndTextId);
                fullText = itemView.findViewById(fullTextId);
                fullStartText = itemView.findViewById(fullStartTextId);
                fullEndText = itemView.findViewById(fullEndTextId);
            }
        }

        //Item list adapter
        public static class ItemListAdapter extends Selectable.ListBaseAdapter
        {
            private Item[] notifications;

            public ItemListAdapter(View parentView, String title)
            {
                super(parentView, title);

                this.itemsRefID = R.layout.settings_notify_item;
            }

            @Override
            public int getItemCount()
            {
                return(notifications != null ? notifications.length : 0);
            }

            @Override
            public Item getItem(int position)
            {
                return(notifications != null ? notifications[position] : null);
            }

            @Override
            public long getItemId(int position)
            {
                return(-1);
            }

            @Override
            protected boolean showColumnTitles(int page)
            {
                return(categoryTitle == null);
            }

            @Override
            protected void setColumnTitles(ViewGroup listColumns, TextView categoryText, int page)
            {
                //if have category
                if(categoryText != null && categoryTitle != null)
                {
                    categoryText.setText(categoryTitle);
                    ((View)categoryText.getParent()).setVisibility(View.VISIBLE);
                }
                else
                {
                    AppCompatImageView imageColumn = listColumns.findViewById(R.id.Notify_Item_Image);

                    imageColumn.setVisibility(View.INVISIBLE);
                    ((TextView)listColumns.findViewById(R.id.Notify_Item_Name_Text)).setText(R.string.title_name);
                }

                super.setColumnTitles(listColumns, categoryText, page);
            }

            @Override
            protected void onItemNonEditClick(Selectable.ListDisplayItem item, int pageNum)
            {
                Item currentItem = (Item)item;
                int noradId = currentItem.id;

                //if a valid ID
                if(noradId != Integer.MIN_VALUE)
                {
                    //start edit activity
                    NotifySettingsActivity.show(currentContext, (currentContext instanceof SettingsActivity ? ((SettingsActivity) currentContext).getResultLauncher() : null), noradId, MainActivity.getObserver());
                }
            }

            @Override
            public @NonNull ItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
            {
                View itemView = LayoutInflater.from(parent.getContext()).inflate(this.itemsRefID, parent, false);
                ItemHolder itemHolder = new ItemHolder(itemView, R.id.Notify_Item_Image, R.id.Notify_Item_Name_Text, R.id.Notify_Item_Pass_Text, R.id.Notify_Item_Pass_Start_Text, R.id.Notify_Item_Pass_End_Text, R.id.Notify_Item_Full_Text, R.id.Notify_Item_Full_Start_Text, R.id.Notify_Item_Full_End_Text);

                setItemSelector(itemView);
                setViewClickListeners(itemView, itemHolder);
                return(itemHolder);
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position)
            {
                Item currentItem = notifications[position];
                ItemHolder itemHolder = (ItemHolder)holder;
                int noradId = currentItem.id;
                boolean usePassStart = (currentItem.passStartSettings.isEnabled());
                boolean usePassEnd = (currentItem.passEndSettings.isEnabled());
                boolean useFullStart = (currentItem.fullStartSettings.isEnabled());
                boolean useFullEnd = (currentItem.fullEndSettings.isEnabled());
                Database.DatabaseSatellite currentOrbital = Database.getOrbital(currentContext, noradId);

                //set displays
                if(currentOrbital != null)
                {
                    itemHolder.notifyImage.setImageDrawable(noradId != Integer.MIN_VALUE ? Globals.getOrbitalIcon(currentContext, MainActivity.getObserver(), noradId, currentOrbital.orbitalType) : Globals.getDrawable(currentContext, R.drawable.ic_notifications_white, true));
                    if(noradId < 0 && noradId != Universe.IDs.Invalid)
                    {
                        itemHolder.notifyImage.setColorFilter(Color.TRANSPARENT);
                    }
                    else
                    {
                        itemHolder.notifyImage.clearColorFilter();
                    }
                    itemHolder.notifyImage.setVisibility(View.VISIBLE);
                }
                else
                {
                    itemHolder.notifyImage.setVisibility(View.INVISIBLE);
                }
                itemHolder.nameText.setText(currentItem.name);
                if(usePassStart || usePassEnd)
                {
                    if(usePassStart)
                    {
                        setNotifyText(itemHolder.passStartText, true, currentItem.passStartSettings.nextOnly);
                    }
                    if(usePassEnd)
                    {
                        setNotifyText(itemHolder.passEndText, false, currentItem.passEndSettings.nextOnly);
                    }

                    itemHolder.passText.setVisibility(View.VISIBLE);
                }
                else
                {
                    itemHolder.passText.setVisibility(View.INVISIBLE);
                }
                itemHolder.passStartText.setVisibility(usePassStart ? View.VISIBLE : View.INVISIBLE);
                itemHolder.passEndText.setVisibility(usePassEnd ? View.VISIBLE : View.INVISIBLE);
                if(useFullStart || useFullEnd)
                {
                    if(useFullStart)
                    {
                        setNotifyText(itemHolder.fullStartText, true, currentItem.fullStartSettings.nextOnly);
                    }
                    if(useFullEnd)
                    {
                        setNotifyText(itemHolder.fullEndText, false, currentItem.fullEndSettings.nextOnly);
                    }

                    itemHolder.fullText.setVisibility(View.VISIBLE);
                }
                else
                {
                    itemHolder.fullText.setVisibility(View.INVISIBLE);
                }
                itemHolder.fullStartText.setVisibility(useFullStart ? View.VISIBLE : View.INVISIBLE);
                itemHolder.fullEndText.setVisibility(useFullEnd ? View.VISIBLE : View.INVISIBLE);

                //set background
                setItemBackground(itemHolder.itemView, false);
            }

            //Reloads items
            @SuppressLint("NotifyDataSetChanged")
            public void reload()
            {
                boolean usingContext = haveContext();
                Resources res = (usingContext ? currentContext.getResources() : null);
                Database.DatabaseSatellite[] orbitals = Database.getOrbitals(currentContext);
                ArrayList<Item> notifyList = new ArrayList<>(0);

                //if context is set
                if(usingContext)
                {
                    //go through each orbital
                    for(Database.DatabaseSatellite currentOrbital : orbitals)
                    {
                        CalculateService.AlarmNotifySettings passStartSettings = Settings.getNotifyPassSettings(currentContext, currentOrbital.noradId, Globals.NotifyType.PassStart);
                        CalculateService.AlarmNotifySettings passEndSettings = Settings.getNotifyPassSettings(currentContext, currentOrbital.noradId, Globals.NotifyType.PassEnd);
                        CalculateService.AlarmNotifySettings fullStartSettings = Settings.getNotifyPassSettings(currentContext, currentOrbital.noradId, Globals.NotifyType.FullMoonStart);
                        CalculateService.AlarmNotifySettings fullEndSettings = Settings.getNotifyPassSettings(currentContext, currentOrbital.noradId, Globals.NotifyType.FullMoonEnd);

                        //if using start or ending notification for any
                        if(passStartSettings.isEnabled() || passEndSettings.isEnabled() || fullStartSettings.isEnabled() || fullEndSettings.isEnabled())
                        {
                            //add orbital
                            notifyList.add(new Item(currentOrbital.noradId, currentOrbital.getName(), passStartSettings, passEndSettings, fullStartSettings, fullEndSettings));
                        }
                    }

                    //set items and layout ID
                    if(notifyList.size() == 0)
                    {
                        notifyList.add(new Item(Integer.MIN_VALUE, (res != null ? res.getString(R.string.title_none) : ""), new CalculateService.AlarmNotifySettings(), new CalculateService.AlarmNotifySettings(), new CalculateService.AlarmNotifySettings(), new CalculateService.AlarmNotifySettings()));
                    }
                }

                //set items
                notifications = notifyList.toArray(new Item[0]);

                //update display
                notifyDataSetChanged();
            }

            //Sets notify text
            private void setNotifyText(TextView notifyText, boolean up, boolean nextOnly)
            {
                notifyText.setVisibility(View.VISIBLE);
                notifyText.setCompoundDrawablesWithIntrinsicBounds(Globals.getDrawableYesNo(currentContext, R.drawable.ic_clock_black, 18, true, !up), null, Globals.getDrawableSized(currentContext, (nextOnly ? R.drawable.ic_repeat_one_white : R.drawable.ic_repeat_white), 18, 18, true, true), null);
            }
        }
    }

    //Widgets
    public static abstract class Widgets
    {
        //Item
        public static class Item extends Selectable.ListDisplayItem
        {
            public final String name;
            public final String location;
            public final Class<?> widgetClass;

            public Item(int id, int index, String nm, String loc, Class<?> wClass)
            {
                super(id, index);
                name = nm;
                location = loc;
                widgetClass = wClass;
            }
        }

        //Item holder
        public static class ItemHolder extends Selectable.ListDisplayItemHolder
        {
            final AppCompatImageView widgetImage;
            final TextView nameText;
            final TextView locationText;

            public ItemHolder(View itemView, int widgetImageId, int widgetTextId, int widgetLocationTextId)
            {
                super(itemView, -1);
                widgetImage = itemView.findViewById(widgetImageId);
                nameText = itemView.findViewById(widgetTextId);
                locationText = itemView.findViewById(widgetLocationTextId);
            }
        }

        //Item list adapter
        public static class ItemListAdapter extends Selectable.ListBaseAdapter
        {
            private Item[] widgets;

            public ItemListAdapter(View parentView, String title)
            {
                super(parentView, title);

                this.itemsRefID = R.layout.settings_widgets_item;
            }

            @Override
            public int getItemCount()
            {
                return(widgets != null ? widgets.length : 0);
            }

            @Override
            public Item getItem(int position)
            {
                return(widgets != null ? widgets[position] : null);
            }

            @Override
            public long getItemId(int position)
            {
                return(-1);
            }

            @Override
            protected boolean showColumnTitles(int page)
            {
                return(categoryTitle == null);
            }

            @Override
            protected void setColumnTitles(ViewGroup listColumns, TextView categoryText, int page)
            {
                //if have category
                if(categoryText != null && categoryTitle != null)
                {
                    categoryText.setText(categoryTitle);
                    ((View)categoryText.getParent()).setVisibility(View.VISIBLE);
                }
                else
                {
                    AppCompatImageView imageColumn = listColumns.findViewById(R.id.Widget_Item_Image);

                    imageColumn.setVisibility(View.INVISIBLE);
                    ((TextView)listColumns.findViewById(R.id.Widget_Item_Name_Text)).setText(R.string.title_name);
                    ((TextView)listColumns.findViewById(R.id.Widget_Item_Location_Text)).setText(R.string.title_location);
                }

                super.setColumnTitles(listColumns, categoryText, page);
            }

            @Override
            protected void onItemNonEditClick(Selectable.ListDisplayItem item, int pageNum)
            {
                Item currentItem = (Item)item;
                int widgetId = currentItem.id;
                Intent editIntent;
                Bundle extras = new Bundle();
                Class<?> widgetClass = currentItem.widgetClass;

                //if a valid ID and class
                if(widgetId != 0 && widgetClass != null)
                {
                    //start edit activity
                    editIntent = new Intent(currentContext, (widgetClass.equals(WidgetPassMediumProvider.class) ? WidgetPassMediumProvider.SetupActivity.class : widgetClass.equals(WidgetPassSmallProvider.class) ? WidgetPassSmallProvider.SetupActivity.class : WidgetPassTinyProvider.SetupActivity.class));
                    extras.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
                    editIntent.putExtras(extras);
                    if(currentContext instanceof SettingsActivity)
                    {
                        Globals.startActivityForResult(((SettingsActivity)currentContext).getResultLauncher(), editIntent, BaseInputActivity.RequestCode.EditWidget);
                    }
                }
            }

            @Override
            public @NonNull ItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
            {
                View itemView = LayoutInflater.from(parent.getContext()).inflate(this.itemsRefID, parent, false);
                ItemHolder itemHolder = new ItemHolder(itemView, R.id.Widget_Item_Image, R.id.Widget_Item_Name_Text, R.id.Widget_Item_Location_Text);

                setItemSelector(itemView);
                setViewClickListeners(itemView, itemHolder);
                return(itemHolder);
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position)
            {
                Item currentItem = widgets[position];
                ItemHolder itemHolder = (ItemHolder)holder;
                int widgetId = currentItem.id;
                int noradId = WidgetBaseSetupActivity.getNoradID(currentContext, currentItem.id);

                //set displays
                itemHolder.widgetImage.setImageDrawable(widgetId != 0 ? Globals.getOrbitalIcon(currentContext, MainActivity.getObserver(), noradId, WidgetBaseSetupActivity.getOrbitalType(currentContext, currentItem.id)) : Globals.getDrawable(currentContext, R.drawable.ic_widgets_black, true));
                if(noradId < 0 && noradId != Universe.IDs.Invalid && widgetId != 0)
                {
                    itemHolder.widgetImage.setColorFilter(Color.TRANSPARENT);
                }
                else
                {
                    itemHolder.widgetImage.clearColorFilter();
                }
                itemHolder.nameText.setText(currentItem.name);
                itemHolder.locationText.setText(currentItem.location);

                //set background
                setItemBackground(itemHolder.itemView, false);
            }

            //Reloads items
            @SuppressLint("NotifyDataSetChanged")
            public void reload()
            {
                int index;
                int widgetId;
                Resources res = currentContext.getResources();
                ArrayList<WidgetPassBaseProvider.WidgetData> widgetsData = WidgetPassBaseProvider.getWidgetData(currentContext);

                //setup items
                widgets = new Item[widgetsData.size()];

                //add all widgets
                for(index = 0; index < widgets.length; index++)
                {
                    //remember current data and ID
                    WidgetPassBaseProvider.WidgetData currentData = widgetsData.get(index);
                    widgetId = currentData.widgetId;

                    //add widget
                    widgets[index] = new Item(widgetId, index, WidgetBaseSetupActivity.getName(currentContext, widgetId), WidgetBaseSetupActivity.getLocationName(currentContext, widgetId), currentData.widgetClass);
                }

                //if no items
                if(widgets.length == 0)
                {
                    //add blank widget
                    widgets = new Item[]{new Item(0, 0, null,  res.getString(R.string.desc_widgets_add), null)};
                }

                //update display
                notifyDataSetChanged();
            }
        }
    }

    //Page view
    public static class Page extends Selectable.ListFragment
    {
        private final String categoryTitle;

        public Page(String title)
        {
            super();
            categoryTitle = title;
        }
        public Page()
        {
            super();
            categoryTitle = null;
        }

        @Override
        public View createView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
        {
            int group = this.getGroupParam();
            int page = this.getPageParam();
            Selectable.ListBaseAdapter listAdapter = null;

            //set list adapter based on page
            switch(page)
            {
                case PageType.Accounts:
                    listAdapter = new Options.Accounts.ItemListAdapter(this, categoryTitle);
                    break;

                case PageType.Locations:
                    listAdapter = new Locations.ItemListAdapter(this.listParentView, categoryTitle);
                    break;

                case PageType.Notifications:
                    listAdapter = new Notifications.ItemListAdapter(this.listParentView, categoryTitle);
                    break;

                case PageType.Widgets:
                    listAdapter = new Widgets.ItemListAdapter(this.listParentView, categoryTitle);
                    break;
            }

            //create view
            return(this.onCreateView(inflater, container, listAdapter, group, page));
        }

        @Override
        protected boolean setupActionModeItems(MenuItem all, MenuItem none, MenuItem edit, MenuItem delete, MenuItem save, MenuItem sync)
        {
            boolean onLocations = (pageNum == PageType.Locations);
            boolean onNotifications = (pageNum == PageType.Notifications);
            boolean onAccounts = isOnAccounts();

            //set visibility
            all.setVisible(false);
            none.setVisible(false);
            edit.setVisible(false);
            delete.setVisible(inEditMode && (onLocations || onNotifications || onAccounts));
            save.setVisible(false);
            sync.setVisible(false);

            //handled
            return(true);
        }

        @Override
        protected void onActionModeSelect(boolean all) {}

        @Override
        protected void onActionModeDelete()
        {
            //show deletion dialog
            showConfirmDeleteDialog(pageNum == PageType.Notifications ? R.plurals.title_notifications : isOnAccounts() ? R.plurals.title_accounts : R.plurals.title_locations);
        }

        @Override
        protected int onActionModeConfirmDelete()
        {
            int deleteCount = 0;
            boolean needLocation = false;
            Context context = (listParentView != null ? listParentView.getContext() : null);
            Selectable.ListDisplayItem[] items = selectedItems.toArray(new Selectable.ListDisplayItem[0]);

            //go through selected items
            for(Selectable.ListDisplayItem currentItem : items)
            {
                //handle based on page
                switch(pageNum)
                {
                    case PageType.Locations:
                        //get current location item
                        Locations.Item currentLocation = (Locations.Item)currentItem;

                        //delete location from database
                        if(context != null && Database.deleteLocation(context, currentLocation.name, currentLocation.locationType))
                        {
                            //if this was the selected location
                            if(currentLocation.isChecked)
                            {
                                //need to select new location
                                needLocation = true;
                            }

                            //update count
                            deleteCount++;
                        }
                        break;

                    case PageType.Notifications:
                        //get current notification item
                        Notifications.Item currentNotification = (Notifications.Item)currentItem;

                        //remove notification for each used type
                        if(currentNotification.passStartSettings.timeMs != 0)
                        {
                            //remove type and update count
                            Settings.setNotifyPassNext(context, currentNotification.id, null, false, 0, Globals.NotifyType.PassStart);
                            deleteCount++;
                        }
                        if(currentNotification.passEndSettings.timeMs != 0)
                        {
                            //remove type and update count
                            Settings.setNotifyPassNext(context, currentNotification.id, null, false, 0, Globals.NotifyType.PassEnd);
                            deleteCount++;
                        }
                        if(currentNotification.fullStartSettings.timeMs != 0)
                        {
                            //remove type and update count
                            Settings.setNotifyPassNext(context, currentNotification.id, null, false, 0, Globals.NotifyType.FullMoonStart);
                            deleteCount++;
                        }
                        if(currentNotification.fullEndSettings.timeMs != 0)
                        {
                            //remove type and update count
                            Settings.setNotifyPassNext(context, currentNotification.id, null, false, 0, Globals.NotifyType.FullMoonEnd);
                            deleteCount++;
                        }

                        //normalize count
                        if(deleteCount > 0)
                        {
                            //only show 1 to user
                            deleteCount = 1;
                        }
                        break;

                    case PageType.Accounts:
                        //remove account
                        ((Options.Accounts.ItemListAdapter)getAdapter()).removeAccount(currentItem.id);
                        deleteCount++;
                        break;
                }
            }

            //if need a location
            if(needLocation)
            {
                //use first (current) location by default
                setItemChecked(0);
            }

            //return delete count
            return(deleteCount);
        }

        @Override
        protected void onActionModeSave() {}

        @Override
        protected void onActionModeSync() {}

        @Override
        protected void onUpdateStarted() {}

        @Override
        protected void onUpdateFinished(boolean success) {}

        //Returns if on accounts page
        private boolean isOnAccounts()
        {
            return(pageNum == PageType.Accounts);
        }

        //Tries to reload the page
        public void reload()
        {
            //get adapter
            Selectable.ListBaseAdapter listAdapter = getAdapter();

            //if adapter is the locations list
            if(listAdapter instanceof Settings.Locations.ItemListAdapter)
            {
                //reload locations list
                ((Settings.Locations.ItemListAdapter)listAdapter).reload();
            }
            //else if adapter is the notifications list
            else if(listAdapter instanceof Settings.Notifications.ItemListAdapter)
            {
                //reload notification list
                ((Settings.Notifications.ItemListAdapter)listAdapter).reload();
            }
            //else if adapter is the widget list
            else if(listAdapter instanceof Settings.Widgets.ItemListAdapter)
            {
                //reload widget list
                ((Settings.Widgets.ItemListAdapter)listAdapter).reload();
            }
        }
    }

    //Page adapter
    public static class PageAdapter extends Selectable.ListFragmentAdapter
    {
        public PageAdapter(FragmentManager fm, View parentView, Selectable.ListFragment.OnItemSelectedListener selectedListener, Selectable.ListFragment.OnItemCheckChangedListener checkChangedListener, Selectable.ListFragment.OnAdapterSetListener adapterListener, Selectable.ListFragment.OnUpdateNeededListener updateListener, Selectable.ListFragment.OnPageResumeListener resumeListener, int[] subPg)
        {
            super(fm, parentView, selectedListener, updateListener, checkChangedListener, null, adapterListener, null, resumeListener, MainActivity.Groups.Settings, subPg);
        }

        @Override
        public @NonNull Fragment getItem(int position)
        {
            return(this.getItem(group, position, subPage[position], new Page()));
        }

        @Override
        public int getCount()
        {
            return(PageType.PageCount);
        }

        @Override
        public CharSequence getPageTitle(int position)
        {
            Resources res = currentContext.getResources();

            switch(position)
            {
                case PageType.Accounts:
                    return(res.getString(R.string.title_accounts));

                case PageType.Locations:
                    return(res.getString(R.string.title_locations));

                case PageType.Notifications:
                    return(res.getString(R.string.title_notifications));

                case PageType.Updates:
                    return(res.getString(R.string.title_updates));

                case PageType.Widgets:
                    return(res.getString(R.string.title_widgets));

                default:
                    return(res.getString(R.string.title_invalid));
            }
        }
    }

    //Status of using metric units, grid, map marker bottom info, footprint, default colors, and sort by
    private static boolean usingMetric = true;
    private static boolean allowNumberCommas = true;
    private static boolean mapMarkerInfoTitle = false;
    private static boolean mapMarkerInfoBottom = true;
    private static boolean mapShowFootprint = false;
    private static boolean mapSelectedShowFootprint = false;
    private static boolean satelliteUseNextDefaultColor = true;
    private static int currentCombinedSortBy = Current.Items.SortBy.Name;

    //Gets read settings
    public static SharedPreferences getReadSettings(Context context)
    {
        return(context.getSharedPreferences("Settings", Context.MODE_PRIVATE));
    }

    //Gets write settings
    public static SharedPreferences.Editor getWriteSettings(Context context)
    {
        return(getReadSettings(context).edit());
    }

    //Gets default boolean value for given preference
    public static boolean getDefaultBooleanValue(String preferenceName, Object dependency)
    {
        if(preferenceName.startsWith(PreferenceName.SatelliteSourceUseGP) && dependency instanceof Integer)
        {
            return((int)dependency != Database.UpdateSource.N2YO && (int)dependency != Database.UpdateSource.HeavensAbove);
        }
        else
        {
            return(getDefaultBooleanValue(preferenceName));
        }
    }
    public static boolean getDefaultBooleanValue(String preferenceName)
    {
        switch(preferenceName)
        {
            case PreferenceName.AskInternet:
            case PreferenceName.FirstRun:
            case PreferenceName.LensFirstRun:
            case PreferenceName.LensFirstCalibrate:
            case PreferenceName.LensUseCamera:
            case PreferenceName.LensIndicatorIconShowDirection:
            case PreferenceName.LensShowToolbars:
            case PreferenceName.LensHideConstellationStarPaths:
            case PreferenceName.LensShowPathDirection:
            case PreferenceName.LensShowPathTimeNames:
            case PreferenceName.LensHideDistantPathTimes:
            case PreferenceName.ListShowPassProgress:
            case PreferenceName.ListShowPassQuality:
            case PreferenceName.MapMarkerShowShadow:
            case PreferenceName.MapMarkerLocationIconUseTint:
            case PreferenceName.MapRotateAllowed:
            case PreferenceName.MapShow3dPaths:
            case PreferenceName.MapShowLabelsAlways:
            case PreferenceName.MapShowOrbitalDirection:
            case PreferenceName.MapShowSelectedFootprint:
            case PreferenceName.MapShowStars:
            case PreferenceName.MapShowSunlight:
            case PreferenceName.MapShowToolbars:
            case PreferenceName.MaterialTheme:
            case PreferenceName.MetricUnits:
            case PreferenceName.AllowNumberCommas:
            case PreferenceName.ShareTranslations:
            case PreferenceName.ShowSatelliteClouds + SubPreferenceName.Map:
            case PreferenceName.ShowSatelliteClouds + SubPreferenceName.Globe:
            case PreferenceName.TranslateInformation:
            case PreferenceName.SatelliteSourceShared:
            case PreferenceName.SatelliteUseNextDefaultColor:
                return(true);
        }

        return(Globals.startsWith(preferenceName, PreferenceName.NotifyFullMoonStartNextOnly, PreferenceName.NotifyFullMoonEndNextOnly, PreferenceName.NotifyPassStartNextOnly, PreferenceName.NotifyPassEndNextOnly));
    }

    //Gets given boolean preference
    public static boolean getPreferenceBoolean(Context context, String preferenceName, Object dependency)
    {
        boolean defaultValue = getDefaultBooleanValue(preferenceName, dependency);
        return(context != null ? getReadSettings(context).getBoolean(preferenceName, defaultValue) : defaultValue);
    }
    public static boolean getPreferenceBoolean(Context context, String preferenceName)
    {
        boolean defaultValue = getDefaultBooleanValue(preferenceName);
        return(context != null ? getReadSettings(context).getBoolean(preferenceName, defaultValue) : defaultValue);
    }

    //Sets given boolean preference
    private static void setPreferenceBoolean(Context context, String preferenceName, boolean value)
    {
        if(context != null)
        {
            getWriteSettings(context).putBoolean(preferenceName, value).apply();
        }
    }

    //Get default int value for the given preference
    public static int getDefaultIntValue(String preferenceName)
    {
        switch(preferenceName)
        {
            case PreferenceName.AltitudeSource:
                return(LocationService.OnlineSource.MapQuest);

            case PreferenceName.CatalogAutoUpdateHour:
            case PreferenceName.TLEAutoUpdateHour:
                return(12);

            case PreferenceName.CurrentCombinedSortBy:
                return(currentCombinedSortBy);

            case PreferenceName.ColorTheme:
                return(Options.Display.ThemeIndex.Cyan);

            case PreferenceName.MapMarkerLocationIcon:
                return(Options.Display.LocationIcon.Marker);

            case PreferenceName.MapMarkerLocationIconTintColor:
                return(Color.rgb(255, 0, 0));

            case PreferenceName.InformationSource:
                return(Database.UpdateSource.NASA);

            case PreferenceName.SatelliteIcon:
                return(Options.Display.SatelliteIcon.Black);

            case PreferenceName.OrbitalIcons:
                return(Options.Display.OrbitalIcons.Freepik);

            case PreferenceName.LensIndicator:
                return(Options.LensView.IndicatorType.Icon);

            case PreferenceName.LensPathLabelType:
                return(Options.LensView.PathLabelType.ColorText);

            case PreferenceName.LensAverageCount:
                return(40);

            case PreferenceName.LensHorizonColor:
                return(Color.rgb(150, 150, 150));

            case PreferenceName.ListUpdateDelay:
            case PreferenceName.LensUpdateDelay:
                return(1000);

            case PreferenceName.MapFrameRate:
                return(60);

            case PreferenceName.MapUpdateDelay:
            case PreferenceName.MapShowOrbitalDirectionLimit:
                return(100);

            case PreferenceName.MapFootprintType:
                return(Options.MapView.FootprintType.OutlineFilled);

            case PreferenceName.MapSelectedFootprintColor:
                return(Color.argb(102, 200, 200, 220));

            case PreferenceName.MapLayerType + SubPreferenceName.Globe:
                return(CoordinatesFragment.MapLayerType.Hybrid);

            case PreferenceName.MapLayerType + SubPreferenceName.Map:
                return(CoordinatesFragment.MapLayerType.Normal);

            case PreferenceName.MapDisplayType:
                return(CoordinatesFragment.MapDisplayType.Globe);

            case PreferenceName.MapGridColor:
                return(Color.argb(180, 0, 0, 255));

            case PreferenceName.MapMarkerInfoLocation:
                return(CoordinatesFragment.MapMarkerInfoLocation.ScreenBottom);

            case PreferenceName.SatelliteSource:
            case PreferenceName.SatelliteDataSource:
                return(Database.UpdateSource.SpaceTrack);

            case PreferenceName.SatelliteNextDefaultColor:
                return(Color.rgb(68, 68, 68));

            case PreferenceName.TimeZoneSource:
                return(LocationService.OnlineSource.Google);
        }

        return(0);
    }

    //Gets given int preference
    public static int getPreferenceInt(Context context, String preferenceName)
    {
        int defaultValue = getDefaultIntValue(preferenceName);
        return(context != null ? getReadSettings(context).getInt(preferenceName, defaultValue) : defaultValue);
    }

    //Sets given int preference
    private static void setPreferenceInt(Context context, String preferenceName, int value)
    {
        if(context != null)
        {
            getWriteSettings(context).putInt(preferenceName, value).apply();
        }
    }

    //Get default long value for the given preference
    public static long getDefaultLongValue(String preferenceName)
    {
        switch(preferenceName)
        {
            case PreferenceName.CatalogAutoUpdateRate:
                return(Options.Updates.MsPer4Weeks);

            case PreferenceName.TLEAutoUpdateRate:
                return(Options.Updates.MsPer3Days);
        }

        return(0);
    }

    //Gets given long preference
    public static long getPreferenceLong(Context context, String preferenceName)
    {
        long defaultValue = getDefaultLongValue(preferenceName);
        return(context != null ? getReadSettings(context).getLong(preferenceName, defaultValue) : defaultValue);
    }

    //Sets given long preference
    private static void setPreferenceLong(Context context, String preferenceName, long value)
    {
        if(context != null)
        {
            getWriteSettings(context).putLong(preferenceName, value).apply();
        }
    }

    //Get default float value for the given preference
    public static float getDefaultFloatValue(String preferenceName)
    {
        switch(preferenceName)
        {
            case PreferenceName.LensIndicatorAlpha:
                return(0.32f);

            case PreferenceName.LensTextAlpha:
                return(0.68f);

            case PreferenceName.LensConstellationAlpha:
                return(0.10f);

            case PreferenceName.LensWidth:
                return(39.279f);

            case PreferenceName.LensHeight:
                return(65.789f);

            case PreferenceName.MapSpeedScale + SubPreferenceName.Globe:
            case PreferenceName.MapSpeedScale + SubPreferenceName.Map:
                return(0.5f);

            case PreferenceName.MapSensitivityScale + SubPreferenceName.Globe:
            case PreferenceName.MapSensitivityScale + SubPreferenceName.Map:
                return(0.8f);

            case PreferenceName.MapMarkerScale:
                return(0.65f);

            case PreferenceName.LensStarMagnitude:
                return(2.2f);

            case PreferenceName.MapFootprintAlpha:
                return(0.35f);
        }

        if(Globals.startsWith(preferenceName,
            PreferenceName.NotifyFullMoonStartLatitude, PreferenceName.NotifyFullMoonStartLongitude, PreferenceName.NotifyFullMoonStartAltitude,
            PreferenceName.NotifyFullMoonEndLatitude, PreferenceName.NotifyFullMoonEndLongitude, PreferenceName.NotifyFullMoonEndAltitude,
            PreferenceName.NotifyPassStartLatitude, PreferenceName.NotifyPassStartLongitude, PreferenceName.NotifyPassStartAltitude,
            PreferenceName.NotifyPassEndLatitude, PreferenceName.NotifyPassEndLongitude, PreferenceName.NotifyPassEndAltitude))
        {
            return(Float.MIN_VALUE);
        }

        return(0);
    }

    //Gets given float preference
    public static float getPreferenceFloat(Context context, String preferenceName)
    {
        float defaultValue = getDefaultFloatValue(preferenceName);
        return(context != null ? getReadSettings(context).getFloat(preferenceName, defaultValue) : defaultValue);
    }

    //Sets given float preference
    private static void setPreferenceFloat(Context context, String preferenceName, float value)
    {
        if(context != null)
        {
            getWriteSettings(context).putFloat(preferenceName, value).apply();
        }
    }

    //Gets default string value for the given preference
    public static String getDefaultStringValue(String preferenceName)
    {
        if(preferenceName.equals(PreferenceName.ListOrbitalTypeFilter) || preferenceName.equals(PreferenceName.MapOrbitalTypeFilter))
        {
            return(getOrbitalTypeFilterString(Database.OrbitalType.Satellite, Database.OrbitalType.RocketBody, Database.OrbitalType.Debris, Database.OrbitalType.Sun, Database.OrbitalType.Planet));
        }
        else if(preferenceName.equals(PreferenceName.LensOrbitalTypeFilter))
        {
            return(getOrbitalTypeFilterString(Database.OrbitalType.Satellite, Database.OrbitalType.RocketBody, Database.OrbitalType.Debris, Database.OrbitalType.Sun, Database.OrbitalType.Planet, Database.OrbitalType.Constellation));
        }
        else if(Globals.startsWith(preferenceName, PreferenceName.NotifyFullMoonStartZoneId, PreferenceName.NotifyFullMoonEndZoneId, PreferenceName.NotifyPassStartZoneId, PreferenceName.NotifyPassEndZoneId))
        {
            return(TimeZone.getDefault().getID());
        }

        return(null);
    }

    //Gets given string preference
    public static String getPreferenceString(Context context, String preferenceName)
    {
        String defaultValue = getDefaultStringValue(preferenceName);
        return(context != null ? getReadSettings(context).getString(preferenceName, defaultValue) : defaultValue);
    }

    //Sets given string preference
    private static void setPreferenceString(Context context, String preferenceName, String value)
    {
        if(context != null)
        {
            getWriteSettings(context).putString(preferenceName, value).apply();
        }
    }

    //Gets orbital type filter for the given preference
    private static ArrayList<Byte> getOrbitalTypeFilter(Context context, String preferenceName)
    {
        byte byteValue;
        String listValue = getPreferenceString(context, preferenceName);
        String[] values = (listValue != null ? listValue.split(",") : null);
        ArrayList<Byte> orbitalTypes = new ArrayList<>(values != null ? values.length : 0);

        //if values are set
        if(values != null)
        {
            //go through each value
            for(String currentValue : values)
            {
                //if able to get a valid byte value
                byteValue = Globals.tryParseByte(currentValue);
                if(byteValue > 0 && byteValue <= Database.OrbitalType.TypeCount)
                {
                    //add orbital type
                    orbitalTypes.add(Byte.parseByte(currentValue));
                }
            }
        }

        //return orbital types
        return(orbitalTypes);
    }

    //Gets a formatted orbital type filter string
    public static String getOrbitalTypeFilterString(Byte... orbitalTypes)
    {
        int index;
        StringBuilder filterString = new StringBuilder();

        //if types are set
        if(orbitalTypes != null)
        {
            //go through each type
            for(index = 0; index < orbitalTypes.length; index++)
            {
                //add type
                filterString.append(orbitalTypes[index]);

                //if there are more types after current
                if(index + 1 < orbitalTypes.length)
                {
                    //add separator
                    filterString.append(",");
                }
            }
        }

        //return filter string
        return(filterString.toString());
    }

    //Sets orbital type filter for the given preference
    public static void setOrbitalTypeFilter(Context context, String preferenceName, Byte... orbitalTypes)
    {
        setPreferenceString(context, preferenceName, getOrbitalTypeFilterString(orbitalTypes));
    }

    //Gets if first run
    public static boolean getFirstRun(Context context)
    {
        return(getPreferenceBoolean(context, PreferenceName.FirstRun));
    }

    //Sets if first run
    public static void setFirstRun(Context context, boolean firstRun)
    {
        setPreferenceBoolean(context, PreferenceName.FirstRun, firstRun);
    }

    //Gets if accepted privacy policy
    public static boolean getAcceptedPrivacy(Context context)
    {
        return(getPreferenceBoolean(context, PreferenceName.AcceptedPrivacy));
    }

    //Sets if accepted privacy policy
    public static void setAcceptedPrivacy(Context context, boolean accepted)
    {
        setPreferenceBoolean(context, PreferenceName.AcceptedPrivacy, accepted);
    }

    //Gets if material design notice shown
    public static boolean getMaterialDesignShown(Context context)
    {
        return(getPreferenceBoolean(context, PreferenceName.MaterialDesignNotice));
    }

    //Sets if material design notice shown
    public static void setMaterialDesignShown(Context context, boolean shown)
    {
        setPreferenceBoolean(context, PreferenceName.MaterialDesignNotice, shown);
    }

    //Gets satellite icon type
    public static int getSatelliteIconType(Context context)
    {
        return(getPreferenceInt(context, PreferenceName.SatelliteIcon));
    }

    //Returns image ID for satellite icon type
    public static int getSatelliteIconImageId(Context context)
    {
        if(context != null)
        {
            switch(getSatelliteIconType(context))
            {
                case Options.Display.SatelliteIcon.GrayBlue:
                    return(R.drawable.orbital_satellite_gray_blue);

                case Options.Display.SatelliteIcon.Sputnik1:
                    return(R.drawable.orbital_satellite_sputnik1);

                case Options.Display.SatelliteIcon.Sputnik2:
                    return(R.drawable.orbital_satellite_sputnik2);

                case Options.Display.SatelliteIcon.Emoji:
                    return(R.drawable.orbital_satellite_emoji);

                case Options.Display.SatelliteIcon.GrayOrange:
                    return(R.drawable.orbital_satellite_gray_orange);

                case Options.Display.SatelliteIcon.Signal:
                    return(R.drawable.orbital_satellite_signal);
            }
        }

        return(R.drawable.orbital_satellite_black);
    }

    //Returns if satellite icon type is themeable
    public static boolean getSatelliteIconImageIsThemeable(Context context)
    {
        if(context != null)
        {
            switch(getSatelliteIconType(context))
            {
                case Options.Display.SatelliteIcon.Black:
                case Options.Display.SatelliteIcon.Sputnik1:
                case Options.Display.SatelliteIcon.Sputnik2:
                    return(true);
            }
        }

        return(false);
    }

    //Gets orbital icons type
    public static int getOrbitalIconsType(Context context)
    {
        return(getPreferenceInt(context, PreferenceName.OrbitalIcons));
    }

    //Returns if lens direction centered
    public static boolean getLensDirectionCentered(Context context)
    {
        return(getPreferenceBoolean(context, PreferenceName.LensDirectionCentered));
    }

    //Gets indicator type
    public static int getIndicator(Context context)
    {
        return(getPreferenceInt(context, PreferenceName.LensIndicator));
    }

    //Sets indicator type
    public static void setIndicator(Context context, int indicatorType)
    {
        setPreferenceInt(context, PreferenceName.LensIndicator, indicatorType);
    }

    //Returns lens icon indicator showing direction
    public static boolean getIndicatorIconShowDirection(Context context)
    {
        return(getPreferenceBoolean(context, PreferenceName.LensIndicatorIconShowDirection));
    }

    //Gets lens average count
    public static int getLensAverageCount(Context context)
    {
        return(getPreferenceInt(context, PreferenceName.LensAverageCount));
    }

    //Gets dark theme value
    public static boolean getDarkTheme(Context context)
    {
        return(getPreferenceBoolean(context, PreferenceName.DarkTheme));
    }

    //Gets material theme value
    public static boolean getMaterialTheme(Context context)
    {
        return(getPreferenceBoolean(context, PreferenceName.MaterialTheme));
    }

    //Gets color theme value
    public static int getColorTheme(Context context)
    {
        return(getPreferenceInt(context, PreferenceName.ColorTheme));
    }

    //Gets metric units value
    public static boolean getMetricUnits(Context context)
    {
        return(getPreferenceBoolean(context, PreferenceName.MetricUnits));
    }

    //Sets metric units value
    public static void setMetricUnits(Context context, boolean use)
    {
        setPreferenceBoolean(context, PreferenceName.MetricUnits, use);
        usingMetric = use;
    }

    //Returns if using metric units
    //note: faster than getting through preferences since called a lot
    public static boolean getUsingMetric()
    {
        return(usingMetric);
    }

    //Gets allowing number commas
    public static boolean getAllowNumberCommas(Context context)
    {
        return(getPreferenceBoolean(context, PreferenceName.AllowNumberCommas));
    }

    //Sets allowing number commas
    public static void setAllowNumberCommas(Context context, boolean allow)
    {
        setPreferenceBoolean(context, PreferenceName.AllowNumberCommas, allow);
        allowNumberCommas = allow;
    }

    //Returns if allowing/using number commas
    //note: faster than getting through preferences since called a lot
    public static boolean getUsingNumberCommas()
    {
        return(allowNumberCommas);
    }

    //Get lens first run
    public static boolean getLensFirstRun(Context context)
    {
        return(getPreferenceBoolean(context, PreferenceName.LensFirstRun));
    }

    //Sets lens first run
    public static void setLensFirstRun(Context context, boolean first)
    {
        setPreferenceBoolean(context, PreferenceName.LensFirstRun, first);
    }

    //Get lens first calibrate
    public static boolean getLensFirstCalibrate(Context context)
    {
        return(getPreferenceBoolean(context, PreferenceName.LensFirstCalibrate));
    }

    //Sets lens first calibrate
    public static void setLensFirstCalibrate(Context context, boolean first)
    {
        setPreferenceBoolean(context, PreferenceName.LensFirstCalibrate, first);
    }

    //Returns lens indicator alpha
    //note: converts from 0 - 1.0 to alpha 0 - 255
    public static int getLensIndicatorAlpha(Context context)
    {
        return((int)(getPreferenceFloat(context, PreferenceName.LensIndicatorAlpha) * 255));
    }

    //Returns lens text alpha
    //note: converts from 0 - 1.0 to alpha 0 - 255
    public static int getLensTextAlpha(Context context)
    {
        return((int)(getPreferenceFloat(context, PreferenceName.LensTextAlpha) * 255));
    }

    //Returns lens constellation alpha
    //note: converts from 0 - 1.0 to alpha 0 - 255
    public static int getLensConstellationAlpha(Context context)
    {
        return((int)(getPreferenceFloat(context, PreferenceName.LensConstellationAlpha) * 255));
    }

    //Returns lens showing toolbars
    public static boolean getLensShowToolbars(Context context)
    {
        return(getPreferenceBoolean(context, PreferenceName.LensShowToolbars));
    }

    //Sets lens showing search list
    public static void setLensShowToolbars(Context context, boolean show)
    {
        setPreferenceBoolean(context, PreferenceName.LensShowToolbars, show);
    }

    //Gets lens path label type
    public static int getLensPathLabelType(Context context)
    {
        return(getPreferenceInt(context, PreferenceName.LensPathLabelType));
    }

    //Returns hide constellation star paths
    public static boolean getLensHideConstellationStarPaths(Context context)
    {
        return(getPreferenceBoolean(context, PreferenceName.LensHideConstellationStarPaths));
    }

    //Returns show path direction
    public static boolean getLensShowPathDirection(Context context)
    {
        return(getPreferenceBoolean(context, PreferenceName.LensShowPathDirection));
    }

    //Returns show path time names
    public static boolean getLensShowPathTimeNames(Context context)
    {
        return(getPreferenceBoolean(context, PreferenceName.LensShowPathTimeNames));
    }

    //Returns lens hide distant path times
    public static boolean getLensHideDistantPathTimes(Context context)
    {
        return(getPreferenceBoolean(context, PreferenceName.LensHideDistantPathTimes));
    }

    //Returns lens showing outside visible area
    public static boolean getLensShowOutsideArea(Context context)
    {
        return(getPreferenceBoolean(context, PreferenceName.LensShowOutsideArea));
    }

    //Get lens horizon color
    public static int getLensHorizonColor(Context context)
    {
        return(getPreferenceInt(context, PreferenceName.LensHorizonColor));
    }

    //Get showing lens horizon
    public static boolean getLensShowHorizon(Context context)
    {
        return(getPreferenceBoolean(context, PreferenceName.LensUseHorizon));
    }

    //Sets showing lens horizon
    public static void setLensShowHorizon(Context context, boolean show)
    {
        setPreferenceBoolean(context, PreferenceName.LensUseHorizon, show);
    }

    //Returns lens star magnitude
    public static float getLensStarMagnitude(Context context)
    {
        //get magnitude
        return(getPreferenceFloat(context, PreferenceName.LensStarMagnitude));
    }

    //Sets lens star magnitude
    public static void setLensStarMagnitude(Context context, float magnitude)
    {
        //if a valid magnitude
        if(magnitude >= (StarMagnitudeScaleMin / 10.0f) && magnitude <= (StarMagnitudeScaleMax / 10.0f))
        {
            //set magnitude
            setPreferenceFloat(context, PreferenceName.LensStarMagnitude, magnitude);
        }
    }

    //Gets lens using camera
    public static boolean getLensUseCamera(Context context)
    {
        return(getPreferenceBoolean(context, PreferenceName.LensUseCamera));
    }

    //Gets lens azimuth user offset
    public static float getLensAzimuthUserOffset(Context context)
    {
        return(getPreferenceFloat(context, PreferenceName.LensAzimuthUserOffset));
    }

    //Sets lens azimuth user offset
    public static void setLensAzimuthUserOffset(Context context, float offset)
    {
        setPreferenceFloat(context, PreferenceName.LensAzimuthUserOffset, offset);
    }

    //Gets if lens needs rotating
    public static boolean getLensRotate(Context context)
    {
        return(getPreferenceBoolean(context, PreferenceName.LensRotate));
    }

    //Gets lens using auto width
    public static boolean getLensAutoWidth(Context context)
    {
        return(getPreferenceBoolean(context, PreferenceName.LensUseAutoWidth));
    }

    //Sets lens using auto width
    public static void setLensAutoWidth(Context context, boolean auto)
    {
        setPreferenceBoolean(context, PreferenceName.LensUseAutoWidth, auto);
    }

    //Gets lens width
    public static float getLensWidth(Context context)
    {
        return(getPreferenceFloat(context, PreferenceName.LensWidth));
    }

    //Set lens width
    public static void setLensWidth(Context context, float width)
    {
        setPreferenceFloat(context, PreferenceName.LensWidth, width);
    }

    //Gets lens using auto height
    public static boolean getLensAutoHeight(Context context)
    {
        return(getPreferenceBoolean(context, PreferenceName.LensUseAutoHeight));
    }

    //Sets lens using auto height
    public static void setLensAutoHeight(Context context, boolean auto)
    {
        setPreferenceBoolean(context, PreferenceName.LensUseAutoHeight, auto);
    }

    //Gets lens height
    public static float getLensHeight(Context context)
    {
        return(getPreferenceFloat(context, PreferenceName.LensHeight));
    }

    //Set lens height
    public static void setLensHeight(Context context, float height)
    {
        setPreferenceFloat(context, PreferenceName.LensHeight, height);
    }

    //Gets lens orbital type filter
    public static ArrayList<Byte> getLensOrbitalTypeFilter(Context context)
    {
        return(getOrbitalTypeFilter(context, PreferenceName.LensOrbitalTypeFilter));
    }

    //Sets lens orbital type filter
    public static void setLensOrbitalTypeFilter(Context context, Byte... orbitalTypes)
    {
        setOrbitalTypeFilter(context, PreferenceName.LensOrbitalTypeFilter, orbitalTypes);
    }

    //Gets list path progress being shown
    public static boolean getListPathProgress(Context context)
    {
        return(getPreferenceBoolean(context, PreferenceName.ListShowPassProgress));
    }

    //Gets list pass quality being shown
    public static boolean getListPassQuality(Context context)
    {
        return(getPreferenceBoolean(context, PreferenceName.ListShowPassQuality));
    }

    //Gets list update delay
    public static int getListUpdateDelay(Context context)
    {
        return(getPreferenceInt(context, PreferenceName.ListUpdateDelay));
    }

    //Gets list orbital type filter
    public static ArrayList<Byte> getListOrbitalTypeFilter(Context context)
    {
        return(getOrbitalTypeFilter(context, PreferenceName.ListOrbitalTypeFilter));
    }

    //Sets list orbital type filter
    public static void setListOrbitalTypeFilter(Context context, Byte... orbitalTypes)
    {
        setOrbitalTypeFilter(context, PreferenceName.ListOrbitalTypeFilter, orbitalTypes);
    }

    //Gets map frame rate
    public static int getMapFrameRate(Context context)
    {
        return(getPreferenceInt(context, PreferenceName.MapFrameRate));
    }

    //Gets map update delay
    public static int getMapUpdateDelay(Context context)
    {
        return(getPreferenceInt(context, PreferenceName.MapUpdateDelay));
    }

    //Gets lens update delay
    public static int getLensUpdateDelay(Context context)
    {
        return(getPreferenceInt(context, PreferenceName.LensUpdateDelay));
    }

    //Returns map layer type
    public static int getMapLayerType(Context context, boolean forGlobe)
    {
        return(getPreferenceInt(context, PreferenceName.MapLayerType + (forGlobe ? SubPreferenceName.Globe : SubPreferenceName.Map)));
    }

    //Returns if using 3d path on globe
    public static boolean getMapShow3dPaths(Context context)
    {
        return(getPreferenceBoolean(context, PreferenceName.MapShow3dPaths));
    }

    //Returns if using clouds on satellite layer for map/globe
    public static boolean getSatelliteClouds(Context context, boolean forMap)
    {
        return(getPreferenceBoolean(context, PreferenceName.ShowSatelliteClouds + (forMap ? SubPreferenceName.Map : SubPreferenceName.Globe)));
    }

    //Sets map display type
    public static void setMapDisplayType(Context context, int mapDisplayType)
    {
        setPreferenceInt(context, PreferenceName.MapDisplayType, mapDisplayType);
    }

    //Returns map display type
    public static int getMapDisplayType(Context context)
    {
        return(getPreferenceInt(context, PreferenceName.MapDisplayType));
    }

    //Returns map showing zoom
    public static boolean getMapShowZoom(Context context)
    {
        return(getPreferenceBoolean(context, PreferenceName.MapShowZoom));
    }

    //Sets map showing zoom
    public static void setMapShowZoom(Context context, boolean show)
    {
        setPreferenceBoolean(context, PreferenceName.MapShowZoom, show);
    }

    //Returns map showing stars
    public static boolean getMapShowStars(Context context)
    {
        return(getPreferenceBoolean(context, PreferenceName.MapShowStars));
    }

    //Gets map speed scale
    public static float getMapSpeedScale(Context context, boolean forGlobe)
    {
        return(getPreferenceFloat(context, PreferenceName.MapSpeedScale + (forGlobe ? SubPreferenceName.Globe : SubPreferenceName.Map)));
    }

    //Gets map sensitivity scale
    public static float getMapSensitivityScale(Context context, boolean forGlobe)
    {
        return(getPreferenceFloat(context, PreferenceName.MapSensitivityScale + (forGlobe ? SubPreferenceName.Globe : SubPreferenceName.Map)));
    }

    //Returns map rotate allowed
    public static boolean getMapRotateAllowed(Context context)
    {
        return(getPreferenceBoolean(context, PreferenceName.MapRotateAllowed));
    }

    //Returns map showing grid
    public static boolean getMapShowGrid(Context context)
    {
        return(getPreferenceBoolean(context, PreferenceName.MapShowGrid));
    }

    //Sets map showing grid
    public static void setMapShowGrid(Context context, boolean show)
    {
        setPreferenceBoolean(context, PreferenceName.MapShowGrid, show);
    }

    //Returns map view grid color
    public static int getMapGridColor(Context context)
    {
        return(getPreferenceInt(context, PreferenceName.MapGridColor));
    }

    //Returns map marker scale
    public static float getMapMarkerScale(Context context)
    {
        //get scale
        return(getPreferenceFloat(context, PreferenceName.MapMarkerScale));
    }

    //Sets map marker scale
    public static void setMapMarkerScale(Context context, float scale)
    {
        //if a valid scale
        if(scale >= (IconScaleMin / 100.0f) && scale <= (IconScaleMax / 100.0f))
        {
            //set scale
            setPreferenceFloat(context, PreferenceName.MapMarkerScale, scale);
        }
    }

    //Gets if location icon type can use tint
    public static boolean getLocationIconTypeCanTint(int locationIconType)
    {
        switch(locationIconType)
        {
            case Settings.Options.Display.LocationIcon.Person:
            case Settings.Options.Display.LocationIcon.Home:
            case Settings.Options.Display.LocationIcon.City:
            case Settings.Options.Display.LocationIcon.Tower:
            case Settings.Options.Display.LocationIcon.Radar:
            case Settings.Options.Display.LocationIcon.Telescope1:
            case Settings.Options.Display.LocationIcon.Telescope2:
            case Settings.Options.Display.LocationIcon.Observatory1:
            case Settings.Options.Display.LocationIcon.Observatory2:
            case Settings.Options.Display.LocationIcon.Dish1:
                return(true);

            default:
                return(false);
        }
    }

    //Returns map marker location icon
    public static int getMapMarkerLocationIcon(Context context)
    {
        //get location icon type
        return(getPreferenceInt(context, PreferenceName.MapMarkerLocationIcon));
    }

    //Returns map marker location icon tint color
    public static int getMapMarkerLocationIconTintColor(Context context)
    {
        return(getPreferenceInt(context, PreferenceName.MapMarkerLocationIconTintColor));
    }

    //Returns map marker location icon used tint color
    public static int getMapMarkerLocationIconUsedTintColor(Context context, int locationIconType)
    {
        boolean canUseTint = getLocationIconTypeCanTint(locationIconType);
        return(canUseTint && getPreferenceBoolean(context, PreferenceName.MapMarkerLocationIconUseTint) ? getMapMarkerLocationIconTintColor(context) : canUseTint ? Color.RED : Color.TRANSPARENT);
    }

    //Returns map showing toolbars
    public static boolean getMapShowToolbars(Context context)
    {
        return(getPreferenceBoolean(context, PreferenceName.MapShowToolbars));
    }

    //Sets map showing search list
    public static void setMapShowToolbars(Context context, boolean show)
    {
        setPreferenceBoolean(context, PreferenceName.MapShowToolbars, show);
    }

    //Returns map showing sunlight
    public static boolean getMapShowSunlight(Context context)
    {
        return(getPreferenceBoolean(context, PreferenceName.MapShowSunlight));
    }

    //Returns map showing orbital footprints
    public static boolean getMapShowFootprint(Context context)
    {
        return(getPreferenceBoolean(context, PreferenceName.MapShowFootprint));
    }

    //Sets map showing orbital footprints
    public static void setMapShowFootprint(Context context, boolean show)
    {
        setPreferenceBoolean(context, PreferenceName.MapShowFootprint, show);
        mapShowFootprint = show;
    }

    //Returns map showing orbital footprint
    //note: faster than getting through preferences since called a lot
    public static boolean usingMapShowFootprint()
    {
        return(mapShowFootprint);
    }

    //Returns map orbital footprint type
    public static int getMapFootprintType(Context context)
    {
        return(getPreferenceInt(context, PreferenceName.MapFootprintType));
    }

    //Returns map footprint alpha
    //note: converts from 0 - 1.0 to alpha 0 - 255
    public static int getMapFootprintAlpha(Context context)
    {
        return((int)(getPreferenceFloat(context, PreferenceName.MapFootprintAlpha) * 255));
    }

    //Get map selected footprint color
    public static int getMapSelectedFootprintColor(Context context)
    {
        return(getPreferenceInt(context, PreferenceName.MapSelectedFootprintColor));
    }

    //Returns if showing selected orbital footprint
    public static boolean getMapShowSelectedFootprint(Context context)
    {
        return(getPreferenceBoolean(context, PreferenceName.MapShowSelectedFootprint));
    }

    //Sets map showing selected orbital footprint
    public static void setMapShowSelectedFootprint(Context context, boolean show)
    {
        setPreferenceBoolean(context, PreferenceName.MapShowSelectedFootprint, show);
        mapSelectedShowFootprint = show;
    }

    //Returns map showing footprint, normal and selected
    //note: faster than getting through both preferences since called a lot
    public static boolean usingMapFootprintAndSelected()
    {
        return(mapShowFootprint && mapSelectedShowFootprint);
    }

    //Returns map showing orbital direction
    public static boolean getMapShowOrbitalDirection(Context context)
    {
        return(getPreferenceBoolean(context, PreferenceName.MapShowOrbitalDirection));
    }

    //Returns map showing orbital direction limit
    public static int getMapShowOrbitalDirectionLimit(Context context)
    {
        return(getPreferenceInt(context, PreferenceName.MapShowOrbitalDirectionLimit));
    }

    //Returns if map showing orbital direction using limit
    public static boolean getMapShowOrbitalDirectionUseLimit(Context context)
    {
        return(getPreferenceBoolean(context, PreferenceName.MapShowOrbitalDirectionUseLimit));
    }

    //Sets if map showing orbital direction using limit
    public static void setMapShowOrbitalDirectionUseLimit(Context context, boolean use)
    {
        setPreferenceBoolean(context, PreferenceName.MapShowOrbitalDirectionUseLimit, use);
    }

    //Returns map showing labels always
    public static boolean getMapShowLabelAlways(Context context)
    {
        return(getPreferenceBoolean(context, PreferenceName.MapShowLabelsAlways));
    }

    //Returns map marker info location
    public static int getMapMarkerInfoLocation(Context context)
    {
        return(getPreferenceInt(context, PreferenceName.MapMarkerInfoLocation));
    }

    //Sets map marker info location
    public static void setMapMarkerInfoLocation(Context context, int location)
    {
        setPreferenceInt(context, PreferenceName.MapMarkerInfoLocation, location);
        mapMarkerInfoTitle = (location == CoordinatesFragment.MapMarkerInfoLocation.UnderTitle);
        mapMarkerInfoBottom = (location == CoordinatesFragment.MapMarkerInfoLocation.ScreenBottom);
    }

    //Returns if using map marker under title info
    //note: faster than getting through preferences since called a lot
    public static boolean usingMapMarkerInfoTitle()
    {
        return(mapMarkerInfoTitle);
    }

    //Returns if using map marker bottom info
    //note: faster than getting through preferences since called a lot
    public static boolean usingMapMarkerInfoBottom()
    {
        return(mapMarkerInfoBottom);
    }

    //Returns map marker showing background
    public static boolean getMapMarkerShowBackground(Context context)
    {
        return(getPreferenceBoolean(context, PreferenceName.MapMarkerShowBackground));
    }

    //Returns map marker showing shadow
    public static boolean getMapMarkerShowShadow(Context context)
    {
        return(getPreferenceBoolean(context, PreferenceName.MapMarkerShowShadow));
    }

    //Sets map marker showing shadow
    public static void setMapMarkerShowShadow(Context context, boolean show)
    {
        setPreferenceBoolean(context, PreferenceName.MapMarkerShowShadow, show);
    }

    //Gets map orbital type filter
    public static ArrayList<Byte> getMapOrbitalTypeFilter(Context context)
    {
        return(getOrbitalTypeFilter(context, PreferenceName.MapOrbitalTypeFilter));
    }

    //Sets map orbital type filter
    public static void setMapOrbitalTypeFilter(Context context, Byte... orbitalTypes)
    {
        setOrbitalTypeFilter(context, PreferenceName.MapOrbitalTypeFilter, orbitalTypes);
    }

    //Gets sort by string id for given sort by value
    public static int getSortByStringId(int sortBy)
    {
        switch(sortBy)
        {
            case Current.Items.SortBy.Azimuth:
                return(R.string.title_azimuth);

            case Current.Items.SortBy.Elevation:
                return(R.string.title_elevation);

            case Current.Items.SortBy.Range:
                return(R.string.title_range);

            case Current.Items.SortBy.PassStartTime:
                return(R.string.title_pass_start);

            case Current.Items.SortBy.PassDuration:
                return(R.string.title_pass_duration);

            case Current.Items.SortBy.MaxElevation:
                return(R.string.title_pass_elevation);

            case Current.Items.SortBy.Latitude:
                return(R.string.title_latitude);

            case Current.Items.SortBy.Longitude:
                return(R.string.title_longitude);

            case Current.Items.SortBy.Altitude:
                return(R.string.title_altitude);

            default:
            case Current.Items.SortBy.Name:
                return(R.string.title_name);
        }
    }

    //Gets string for given sort by
    public static int getSortBy(Context context, String value)
    {
        int sortBy;
        Resources res = context.getResources();

        //go through each sort by
        for(sortBy = 0; sortBy < Current.Items.SortBy.Count; sortBy++)
        {
            //if string is a match
            if(res.getString(getSortByStringId(sortBy)).equals(value))
            {
                //return sort by
                return(sortBy);
            }
        }

        //not found
        return(Current.Items.SortBy.Name);
    }

    //Gets current sort by for given page
    public static int getCurrentSortBy(Context context)
    {
        return(getPreferenceInt(context, PreferenceName.CurrentCombinedSortBy));
    }
    public static int getCurrentSortBy()
    {
        return(currentCombinedSortBy);
    }
    public static String getCurrentSortByString(Context context)
    {
        return(context.getString(getSortByStringId(getCurrentSortBy())));
    }

    //Sets current sort by for given page
    public static void setCurrentSortBy(Context context, int sortBy)
    {
        setPreferenceInt(context, PreferenceName.CurrentCombinedSortBy, sortBy);
        currentCombinedSortBy = sortBy;
    }
    public static void setCurrentSortBy(Context context, String sortByString)
    {
        setCurrentSortBy(context, getSortBy(context, sortByString));
    }

    //Gets last location
    public static Location getLastLocation(Context context)
    {
        Location lastLocation = new Location("");

        //set location
        lastLocation.setLatitude(getPreferenceFloat(context, PreferenceName.LocationLastLatitude));
        lastLocation.setLongitude(getPreferenceFloat(context, PreferenceName.LocationLastLongitude));
        lastLocation.setAltitude(getPreferenceFloat(context, PreferenceName.LocationLastAltitude));

        //return
        return(lastLocation);
    }

    //Sets last location
    public static void setLastLocation(Context context, Location lastLocation)
    {
        SharedPreferences.Editor writeSettings = getWriteSettings(context);

        if(lastLocation != null)
        {
            writeSettings.putFloat(PreferenceName.LocationLastLatitude, (float)lastLocation.getLatitude());
            writeSettings.putFloat(PreferenceName.LocationLastLongitude, (float)lastLocation.getLongitude());
            writeSettings.putFloat(PreferenceName.LocationLastAltitude, (float)lastLocation.getAltitude());
        }
        writeSettings.apply();
    }

    //Gets altitude source
    public static int getAltitudeSource(Context context)
    {
        return(getPreferenceInt(context, PreferenceName.AltitudeSource));
    }

    //Gets time zone source
    public static int getTimeZoneSource(Context context)
    {
        return(getPreferenceInt(context, PreferenceName.TimeZoneSource));
    }

    //Sets satellite catalog source
    public static void setSatelliteCatalogSource(Context context, int source)
    {
        //if source is changing
        if(source != getSatelliteCatalogSource(context))
        {
            //clear master satellites
            Database.clearMasterSatelliteTable(context);
        }

        //save setting
        setPreferenceInt(context, PreferenceName.SatelliteSource, source);
    }

    //Gets satellites catalog source
    public static int getSatelliteCatalogSource(Context context)
    {
        return(getPreferenceInt(context, PreferenceName.SatelliteSource));
    }

    //Gets satellites data source
    public static int getSatelliteDataSource(Context context)
    {
        return(getPreferenceInt(context, (getSatelliteSourceShared(context) ? PreferenceName.SatelliteSource : PreferenceName.SatelliteDataSource)));
    }

    //Gets satellite source using GP
    public static boolean getSatelliteSourceUseGP(Context context, int source)
    {
        return(getPreferenceBoolean(context, PreferenceName.SatelliteSourceUseGP + source, source));
    }

    //Gets satellite source being shared
    public static boolean getSatelliteSourceShared(Context context)
    {
        return(getPreferenceBoolean(context, PreferenceName.SatelliteSourceShared));
    }

    //Returns if using satellite next default color
    public static boolean getSatelliteUseNextDefaultColor(Context context)
    {
        return(getPreferenceBoolean(context, PreferenceName.SatelliteUseNextDefaultColor));
    }

    //Sets if using satellite next default color
    public static void setSatelliteUseNextDefaultColor(Context context, boolean use)
    {
        setPreferenceBoolean(context, PreferenceName.SatelliteUseNextDefaultColor, use);
        satelliteUseNextDefaultColor = use;
    }

    //Returns using satellite next default color
    //note: faster than getting through preferences since called a lot
    public static boolean usingSatelliteNextDefaultColor()
    {
        return(satelliteUseNextDefaultColor);
    }

    //Sets satellite next default color
    public static void setSatelliteNextDefaultColor(Context context, int color)
    {
        setPreferenceInt(context, PreferenceName.SatelliteNextDefaultColor, color);
    }

    //Generates a new default color
    public static void generateNextDefaultColor(Context context, int color)
    {
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        int start = 60;
        int afterStart = start + 5;
        int end = 230;

        if(red > start)
        {
            green = blue = start;
            red += 10;
            if(red > end)
            {
                red = start;
                green = afterStart;
            }
        }
        else if(green > start)
        {
            red = blue = start;
            green += 10;
            if(green > end)
            {
                green = start;
                blue = afterStart;
            }
        }
        else
        {
            red = green = start;
            blue += 10;
            if(blue > end)
            {
                blue = start;
                red = afterStart;
            }
        }

        //set next default color
        setSatelliteNextDefaultColor(context, Color.rgb(red, green, blue));
    }

    //Gets satellite next default color
    public static int getSatelliteNextDefaultColor(Context context)
    {
        return(getPreferenceInt(context, PreferenceName.SatelliteNextDefaultColor));
    }

    //Gets catalog debris usage
    public static boolean getCatalogDebris(Context context)
    {
        return(getPreferenceBoolean(context, PreferenceName.CatalogDebris));
    }

    //Gets catalog rocket bodies usage
    public static boolean getCatalogRocketBodies(Context context)
    {
        return(getPreferenceBoolean(context, PreferenceName.CatalogRocketBodies));
    }

    //Gets information source
    public static int getInformationSource(Context context)
    {
        return(getPreferenceInt(context, PreferenceName.InformationSource));
    }

    //Gets if translating information
    public static boolean getTranslateInformation(Context context)
    {
        return(getPreferenceBoolean(context, PreferenceName.TranslateInformation));
    }

    //Gets if sharing translations
    public static boolean getShareTranslations(Context context)
    {
        return(getPreferenceBoolean(context, PreferenceName.ShareTranslations));
    }

    //Gets encrypted Space-Track password
    public static String getEncryptedSpaceTrackPassword(Context context)
    {
        return(getPreferenceString(context, PreferenceName.SpaceTrackPassword));
    }

    //Gets login name and password
    public static String[] getLogin(Context context, int accountType)
    {
        return(accountType == Globals.AccountType.SpaceTrack ? new String[]{getPreferenceString(context, PreferenceName.SpaceTrackUser), Encryptor.decrypt(context, getEncryptedSpaceTrackPassword(context))} : new String[]{null, null});
    }

    //Sets space-track login name and password
    public static void setSpaceTrackLogin(Context context, String user, String password)
    {
        setPreferenceString(context, PreferenceName.SpaceTrackUser, user);
        setPreferenceString(context, PreferenceName.SpaceTrackPassword, Encryptor.encrypt(context, password));
    }

    //Removes space-track login name and password
    public static void removeSpaceTrackLogin(Context context)
    {
        SharedPreferences.Editor writeSettings = getWriteSettings(context);

        writeSettings.remove(PreferenceName.SpaceTrackUser);
        writeSettings.remove(PreferenceName.SpaceTrackPassword);
        writeSettings.apply();
    }

    //Gets notify pass next only key
    private static String getNotifyPassNextOnlyKey(int noradId, byte notifyType)
    {
        String key;

        switch(notifyType)
        {
            case Globals.NotifyType.FullMoonStart:
                key = PreferenceName.NotifyFullMoonStartNextOnly;
                break;

            case Globals.NotifyType.FullMoonEnd:
                key = PreferenceName.NotifyFullMoonEndNextOnly;
                break;

            case Globals.NotifyType.PassStart:
                key = PreferenceName.NotifyPassStartNextOnly;
                break;

            default:
            case Globals.NotifyType.PassEnd:
                key = PreferenceName.NotifyPassEndNextOnly;
                break;

        }
        return(key + noradId);
    }

    //Gets notify pass next time key
    private static String getNotifyPassNextMsKey(int noradId, byte notifyType)
    {
        String key;

        switch(notifyType)
        {
            case Globals.NotifyType.FullMoonStart:
                key = PreferenceName.NotifyFullMoonStartNextMs;
                break;

            case Globals.NotifyType.FullMoonEnd:
                key = PreferenceName.NotifyFullMoonEndNextMs;
                break;

            case Globals.NotifyType.PassStart:
                key = PreferenceName.NotifyPassStartNextMs;
                break;

            default:
            case Globals.NotifyType.PassEnd:
                key = PreferenceName.NotifyPassEndNextMs;
                break;

        }
        return(key + noradId);
    }

    //Gets notify pass latitude key
    private static String getNotifyPassLatitudeKey(int noradId, byte notifyType)
    {
        String key;

        switch(notifyType)
        {
            case Globals.NotifyType.FullMoonStart:
                key = PreferenceName.NotifyFullMoonStartLatitude;
                break;

            case Globals.NotifyType.FullMoonEnd:
                key = PreferenceName.NotifyFullMoonEndLatitude;
                break;

            case Globals.NotifyType.PassStart:
                key = PreferenceName.NotifyPassStartLatitude;
                break;

            default:
            case Globals.NotifyType.PassEnd:
                key = PreferenceName.NotifyPassEndLatitude;
                break;

        }
        return(key + noradId);
    }

    //Gets notify pass longitude key
    private static String getNotifyPassLongitudeKey(int noradId, byte notifyType)
    {
        String key;

        switch(notifyType)
        {
            case Globals.NotifyType.FullMoonStart:
                key = PreferenceName.NotifyFullMoonStartLongitude;
                break;

            case Globals.NotifyType.FullMoonEnd:
                key = PreferenceName.NotifyFullMoonEndLongitude;
                break;

            case Globals.NotifyType.PassStart:
                key = PreferenceName.NotifyPassStartLongitude;
                break;

            default:
            case Globals.NotifyType.PassEnd:
                key = PreferenceName.NotifyPassEndLongitude;
                break;

        }
        return(key + noradId);
    }

    //Gets notify pass altitude key
    private static String getNotifyPassAltitudeKey(int noradId, byte notifyType)
    {
        String key;

        switch(notifyType)
        {
            case Globals.NotifyType.FullMoonStart:
                key = PreferenceName.NotifyFullMoonStartAltitude;
                break;

            case Globals.NotifyType.FullMoonEnd:
                key = PreferenceName.NotifyFullMoonEndAltitude;
                break;

            case Globals.NotifyType.PassStart:
                key = PreferenceName.NotifyPassStartAltitude;
                break;

            default:
            case Globals.NotifyType.PassEnd:
                key = PreferenceName.NotifyPassEndAltitude;
                break;

        }
        return(key + noradId);
    }

    //Gets notify pass zone ID key
    private static String getNotifyPassZoneIdKey(int noradId, byte notifyType)
    {
        String key;

        switch(notifyType)
        {
            case Globals.NotifyType.FullMoonStart:
                key = PreferenceName.NotifyFullMoonStartZoneId;
                break;

            case Globals.NotifyType.FullMoonEnd:
                key = PreferenceName.NotifyFullMoonEndZoneId;
                break;

            case Globals.NotifyType.PassStart:
                key = PreferenceName.NotifyPassStartZoneId;
                break;

            default:
            case Globals.NotifyType.PassEnd:
                key = PreferenceName.NotifyPassEndZoneId;
                break;

        }
        return(key + noradId);
    }

    //Sets notify pass next
    public static void setNotifyPassNext(Context context, int noradId, Calculations.ObserverType location, boolean nextOnly, long timeMs, byte notifyType)
    {
        String nextOnlyKey = getNotifyPassNextOnlyKey(noradId, notifyType);
        String nextMsKey = getNotifyPassNextMsKey(noradId, notifyType);
        String nextLatKey = getNotifyPassLatitudeKey(noradId, notifyType);
        String nextLonKey = getNotifyPassLongitudeKey(noradId, notifyType);
        String altKey = getNotifyPassAltitudeKey(noradId, notifyType);
        String nextZoneIdKey = getNotifyPassZoneIdKey(noradId, notifyType);
        SharedPreferences.Editor writeSettings = getWriteSettings(context);

        if(timeMs == 0)
        {
            writeSettings.remove(nextOnlyKey);
            writeSettings.remove(nextMsKey);
            writeSettings.remove(nextLatKey);
            writeSettings.remove(nextLonKey);
            writeSettings.remove(altKey);
            writeSettings.remove(nextZoneIdKey);
        }
        else
        {
            writeSettings.putBoolean(nextOnlyKey, nextOnly);
            writeSettings.putLong(nextMsKey, timeMs);
            if(location != null)
            {
                writeSettings.putFloat(nextLatKey, (float)location.geo.latitude);
                writeSettings.putFloat(nextLonKey, (float)location.geo.longitude);
                writeSettings.putFloat(altKey, (float)location.geo.altitudeKm);
                writeSettings.putString(nextZoneIdKey, location.timeZone.getID());
            }
        }
        writeSettings.apply();
    }

    //Gets notify pass settings
    public static CalculateService.AlarmNotifySettings getNotifyPassSettings(Context context, int noradId, byte notifyType)
    {
        CalculateService.AlarmNotifySettings settings = new CalculateService.AlarmNotifySettings();

        settings.nextOnly = Settings.getPreferenceBoolean(context, getNotifyPassNextOnlyKey(noradId, notifyType));
        settings.noradId = noradId;
        settings.timeMs = Settings.getPreferenceLong(context, getNotifyPassNextMsKey(noradId, notifyType));
        settings.location.timeZone = TimeZone.getTimeZone(Settings.getPreferenceString(context, getNotifyPassZoneIdKey(noradId, notifyType)));
        settings.location.geo.latitude = Settings.getPreferenceFloat(context, getNotifyPassLatitudeKey(noradId, notifyType));
        settings.location.geo.longitude = Settings.getPreferenceFloat(context, getNotifyPassLongitudeKey(noradId, notifyType));
        settings.location.geo.altitudeKm = Settings.getPreferenceFloat(context, getNotifyPassAltitudeKey(noradId, notifyType));

        return(settings);
    }
    public static CalculateService.AlarmNotifySettings[][] getNotifyPassSettings(Context context)
    {
        int noradId;
        byte index;
        Map<String, ?> allSettings;
        ArrayList<Integer> usedIds = new ArrayList<>(0);
        ArrayList<CalculateService.AlarmNotifySettings[]> notifySettings = new ArrayList<>(0);

        //if context is set
        if(context != null)
        {
            //get all settings
            allSettings = getReadSettings(context).getAll();
            if(allSettings != null)
            {
                //go through each key
                for(Map.Entry<String, ?> currentSetting : allSettings.entrySet())
                {
                    //get current key
                    String currentKey = currentSetting.getKey();

                    //if a notify key
                    if(currentKey.startsWith("Notify"))
                    {
                        ArrayList<CalculateService.AlarmNotifySettings> settings = new ArrayList<>(0);

                        //get norad ID
                        noradId = Globals.tryParseInt(currentKey.replaceAll("[a-zA-Z]", ""));

                        //if valid norad ID and not in list
                        if(noradId != Integer.MAX_VALUE && !usedIds.contains(noradId))
                        {
                            //go through each notify type
                            for(index = 0; index < Globals.NotifyType.NotifyCount; index++)
                            {
                                //add notify setting to list
                                settings.add(getNotifyPassSettings(context, noradId, index));
                            }

                            //add to notify settings and used IDs
                            notifySettings.add(settings.toArray(new CalculateService.AlarmNotifySettings[0]));
                            usedIds.add(noradId);
                        }
                    }
                }
            }
        }

        //return settings
        return(notifySettings.toArray(new CalculateService.AlarmNotifySettings[0][]));
    }

    //Sets notify
    public static void setNotify(Context context, int noradId, Calculations.ObserverType location, boolean[] notifyUsing, boolean[] notifyNextOnly)
    {
        byte index;
        CalculateService.AlarmNotifySettings[] notifySettings = new CalculateService.AlarmNotifySettings[Globals.NotifyType.NotifyCount];

        //go through each notify type
        for(index = 0; index < Globals.NotifyType.NotifyCount; index++)
        {
            //update settings
            notifySettings[index] = getNotifyPassSettings(context, noradId, index);
            notifySettings[index].nextOnly = notifyNextOnly[index];
        }

        //start/stop updates
        CalculateService.NotifyReceiver.setNotifyStates(context, location, notifySettings, notifyUsing, true);
    }

    //Gets auto update next update time in ms
    public static long getAutoUpdateNextMs(Context context, byte updateType)
    {
        switch(updateType)
        {
            case UpdateService.UpdateType.GetMasterList:
                return(Settings.getPreferenceLong(context, PreferenceName.CatalogAutoUpdateNextMs));

            case UpdateService.UpdateType.UpdateSatellites:
                return(Settings.getPreferenceLong(context, PreferenceName.TLEAutoUpdateNextMs));
        }

        return(0);
    }

    //Sets auto update next time in ms
    public static void setAutoUpdateNextMs(Context context, byte updateType, long timeMs)
    {
        switch(updateType)
        {
            case UpdateService.UpdateType.GetMasterList:
                setPreferenceLong(context, PreferenceName.CatalogAutoUpdateNextMs, timeMs);
                break;

            case UpdateService.UpdateType.UpdateSatellites:
                setPreferenceLong(context, PreferenceName.TLEAutoUpdateNextMs, timeMs);
                break;
        }
    }

    //Gets auto update alarm rate
    public static long getAutoUpdateRateMs(Context context, byte updateType)
    {
        switch(updateType)
        {
            case UpdateService.UpdateType.GetMasterList:
                return(Settings.getPreferenceLong(context, PreferenceName.CatalogAutoUpdateRate));

            default:
            case UpdateService.UpdateType.UpdateSatellites:
                return(Settings.getPreferenceLong(context, PreferenceName.TLEAutoUpdateRate));
        }
    }

    //Gets auto update alarm settings
    public static UpdateService.AlarmUpdateSettings getAutoUpdateSettings(Context context, byte updateType)
    {
        UpdateService.AlarmUpdateSettings settings = new UpdateService.AlarmUpdateSettings();

        switch(updateType)
        {
            case UpdateService.UpdateType.GetMasterList:
                settings.enabled = getPreferenceBoolean(context, PreferenceName.CatalogAutoUpdate);
                settings.hour = getPreferenceInt(context, PreferenceName.CatalogAutoUpdateHour);
                settings.minute = getPreferenceInt(context, PreferenceName.CatalogAutoUpdateMinute);
                break;

            default:
            case UpdateService.UpdateType.UpdateSatellites:
                settings.enabled = getPreferenceBoolean(context, PreferenceName.TLEAutoUpdate);
                settings.hour = getPreferenceInt(context, PreferenceName.TLEAutoUpdateHour);
                settings.minute = getPreferenceInt(context, PreferenceName.TLEAutoUpdateMinute);
                break;
        }
        settings.rate = getAutoUpdateRateMs(context, updateType);

        return(settings);
    }
    public static UpdateService.AlarmUpdateSettings getAutoUpdateSettings(Context context, String preferenceKey)
    {
        return(getAutoUpdateSettings(context, getUpdateType(preferenceKey)));
    }

    //Gets the update type from the preference name
    private static byte getUpdateType(String preferenceKey)
    {
        //handle specific cases
        switch(preferenceKey)
        {
            case PreferenceName.CatalogAutoUpdate:
            case PreferenceName.CatalogAutoUpdateHour:
            case PreferenceName.CatalogAutoUpdateMinute:
            case PreferenceName.CatalogAutoUpdateRate:
                return(UpdateService.UpdateType.GetMasterList);

            case PreferenceName.TLEAutoUpdate:
            case PreferenceName.TLEAutoUpdateHour:
            case PreferenceName.TLEAutoUpdateMinute:
            case PreferenceName.TLEAutoUpdateRate:
                return(UpdateService.UpdateType.UpdateSatellites);
        }

        //unknown
        return(Byte.MAX_VALUE);
    }

    //Sets auto update
    public static void setAutoUpdate(Context context, String preferenceKey)
    {
        byte updateType = getUpdateType(preferenceKey);
        UpdateService.AlarmUpdateSettings settings = getAutoUpdateSettings(context, updateType);

        //handle specific cases
        switch(updateType)
        {
            case UpdateService.UpdateType.GetMasterList:
            case UpdateService.UpdateType.UpdateSatellites:
                //start/stop updates
                UpdateService.NotifyReceiver.setUpdateTime(context, updateType, settings);
                break;
        }
    }

    //Gets asking of internet connection
    public static boolean getAskInternet(Context context)
    {
        return(getPreferenceBoolean(context, PreferenceName.AskInternet));
    }

    //Sets asking of internet connection
    public static void setAskInternet(Context context, boolean ask)
    {
        setPreferenceBoolean(context, PreferenceName.AskInternet, ask);
    }

    //Gets if using globe compatibility mode
    public static boolean getUseGlobeCompatibility(Context context)
    {
        return(getPreferenceBoolean(context, PreferenceName.UseGlobeCompatibility));
    }

    //Sets if using globe compatibility mode
    public static void setUseGlobeCompatibility(Context context, boolean use)
    {
        setPreferenceBoolean(context, PreferenceName.UseGlobeCompatibility, use);
    }
}
