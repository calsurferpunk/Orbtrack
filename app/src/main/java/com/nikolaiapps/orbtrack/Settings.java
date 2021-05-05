package com.nikolaiapps.orbtrack;


import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.location.Location;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SwitchCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TableRow;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
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

    //Page types
    public static abstract class PageType
    {
        static final int Accounts = 0;
        static final int Locations = 1;
        static final int Notifications = 2;
        static final int Updates = 3;
        static final int Other = 4;
        static final int PageCount = 5;
    }

    //Sub page types
    public static abstract class SubPageType
    {
        static final int None = 0;
        static final int Accounts = 1;
        static final int Display = 2;
        static final int ListView = 3;
        static final int LensView = 4;
        static final int MapView = 5;
        static final int Widgets = 6;
    }

    //Preference names
    public static abstract class PreferenceName
    {
        static final String FirstRun = "FirstRun";
        static final String AcceptedPrivacy = "AcceptedPrivacy";
        static final String CombinedNotice = "CombinedNotice";
        static final String DarkTheme = "DarkTheme";
        static final String ColorTheme = "ColorTheme";
        static final String MetricUnits = "MetricUnits";
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
        static final String MapMarkerInfoLocation = "MapMarkerInfoLocation";
        static final String MapMarkerShowBackground = "MapMarkerShowBackground";
        static final String MapMarkerShowShadow = "MapMarkerShowShadow";
        static final String MapShowSearchList = "MapShowSearchList";
        static final String UseCombinedCurrentLayout = "UseCombinedCurrentLayout";
        static final String UseCurrentGridLayout = "UseCurrentGridLayout";
        static final String CurrentCombinedSortBy = "CurrentCombinedSortBy";
        static final String CurrentViewSortBy = "CurrentViewSortBy";
        static final String CurrentPassesSortBy = "CurrentPassesSortBy";
        static final String CurrentCoordinatesSortBy = "CurrentCoordinatesSortBy";
        static final String LensFirstRun = "LensFirstRun";
        static final String ListUpdateDelay = "ListUpdateDelay";
        static final String ListShowPassProgress = "ListShowPassProgress";
        static final String LensUpdateDelay = "LensUpdateDelay";
        static final String LensAverageCount = "LensAverageCount";
        static final String LensIndicator = "LensIndicator";
        static final String LensHorizonColor = "LensHorizonColor";
        static final String LensUseHorizon = "LensUseHorizon";
        static final String LensUseCamera = "LensUseCamera";
        static final String LensRotate = "LensRotate";
        static final String LensUseAutoWidth = "LensUseAutoWidth";
        static final String LensUseAutoHeight = "LensUseAutoHeight";
        static final String LensAzimuthUserOffset = "LensAzimuthUserOffset";
        static final String LensWidth = "LensWidth";
        static final String LensHeight = "LensHeight";
        static final String LocationLastLatitude = "LocationLatitude";
        static final String LocationLastLongitude = "LocationLastLongitude";
        static final String LocationLastAltitude = "LocationLastAltitude";
        static final String AltitudeSource = "AltitudeSource";
        static final String TimeZoneSource = "TimeZoneSource";
        static final String SatelliteSource = "SatelliteSource";
        static final String SatelliteSourceUseGP = "SatelliteSourceUseGP";
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
        //Indicator types
        public static abstract class IndicatorType
        {
            static final int Circle = 0;
            static final int Square = 1;
            static final int Triangle = 2;
            static final int Icon = 3;
        }

        //Display
        public static abstract class Display
        {
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
                Resources res = context.getResources();

                //if values are not set
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

            //Creates an on dark theme check changed listener
            private static CompoundButton.OnCheckedChangeListener createOnDarkThemeCheckedChangedListener(final Selectable.ListFragment page, final Activity context, final SharedPreferences.Editor writeSettings)
            {
                return(new CompoundButton.OnCheckedChangeListener()
                {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked)
                    {
                        //if view exists and user changed
                        if(compoundButton != null && compoundButton.isPressed())
                        {
                            //update value
                            writeSettings.putBoolean(PreferenceName.DarkTheme, isChecked).apply();

                            //set theme and apply
                            setTheme(context);
                            context.recreate();
                            page.onUpdateNeeded();
                        }
                    }
                });
            }

            //Creates an on color item selected listener
            private static AdapterView.OnItemSelectedListener createOnColorItemSelectedListener(final Selectable.ListFragment page, final Activity context, final SharedPreferences.Editor writeSettings)
            {
                return(new AdapterView.OnItemSelectedListener()
                {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id)
                    {
                        int desiredThemeValue = (int) colorAdvancedItems[position].value;
                        int currentThemeValue = getColorTheme(context);

                        //if view exists
                        if(adapterView != null && desiredThemeValue != currentThemeValue)
                        {
                            //update value
                            writeSettings.putInt(PreferenceName.ColorTheme, desiredThemeValue).apply();

                            //set theme and apply
                            setTheme(context);
                            context.recreate();
                            page.onUpdateNeeded();
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) { }
                });
            }

            //Sets the theme
            public static void setTheme(Context context)
            {
                SharedPreferences readSettings = getPreferences(context);
                boolean darkTheme = readSettings.getBoolean(PreferenceName.DarkTheme, false);
                int colorTheme = readSettings.getInt(PreferenceName.ColorTheme, ThemeIndex.Cyan);
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
            public static class Item extends Selectable.ListItem
            {
                String loginName;

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
                            return(R.drawable.org_gdrive);

                        case Globals.AccountType.Dropbox:
                            return(R.drawable.org_dbox);

                        case Globals.AccountType.SpaceTrack:
                            return(R.drawable.org_space_track);

                        default:
                        case Globals.AccountType.None:
                            return(-1);
                    }
                }
            }

            //Item holder
            public static class ItemHolder extends Selectable.ListItemHolder
            {
                AppCompatImageView accountImage;
                TextView nameText;

                public ItemHolder(View itemView)
                {
                    super(itemView, -1);

                    //get displays
                    accountImage = itemView.findViewById(R.id.Settings_Accounts_Item_Image);
                    nameText = itemView.findViewById(R.id.Settings_Accounts_Item_Name_Text);
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
                        this.itemsRefID = R.layout.settings_other_accounts_item;
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
                public void reloadItems()
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
                        AppCompatImageView imageColumn = listColumns.findViewById(R.id.Settings_Accounts_Item_Image);

                        imageColumn.setVisibility(View.INVISIBLE);
                        imageColumn.setImageResource(R.drawable.org_gdrive);
                        ((TextView)listColumns.findViewById(R.id.Settings_Accounts_Item_Name_Text)).setText(R.string.title_account);
                    }

                    super.setColumnTitles(listColumns, categoryText, page);
                }

                @Override
                protected void onItemNonEditClick(Selectable.ListItem item, int pageNum)
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
                    switch(accountType)
                    {
                        case Globals.AccountType.SpaceTrack:
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
                                            reloadItems();
                                        }
                                    });
                                }
                            }, true);
                            break;
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
                            removeLogin(currentContext, accountType);
                            break;
                    }

                    //reload items
                    reloadItems();
                }
            }
        }

        //Map view
        private static abstract class MapView
        {
            //Map types
            private static String[] mapTypeItems;
            private static final Integer[] MapTypeValues = new Integer[]{CoordinatesFragment.MapLayerType.Normal, CoordinatesFragment.MapLayerType.Satellite, /*CoordinatesFragment.MapLayerType.Terrain,*/ CoordinatesFragment.MapLayerType.Hybrid};

            //Information location types
            private static String[] infoLocationItems;
            private static final Integer[] InfoLocationValues = new Integer[]{CoordinatesFragment.MapMarkerInfoLocation.None, CoordinatesFragment.MapMarkerInfoLocation.UnderTitle, CoordinatesFragment.MapMarkerInfoLocation.ScreenBottom};
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

            //Information sources
            public static final String[] InformationSourceItems = new String[]{Sources.NASA, Sources.Celestrak, Sources.N2YO};
            public static final Byte[] InformationSourceValues = new Byte[]{Database.UpdateSource.NASA, Database.UpdateSource.Celestrak, Database.UpdateSource.N2YO};
            public static final int[] InformationSourceImageIds = new int[]{R.drawable.org_nasa, R.drawable.org_celestrak, R.drawable.org_n2yo};

            //Update frequencies
            private static final long MsPerDay = (long)Calculations.MsPerDay;
            private static final long MsPer3Days = MsPerDay * 3;
            private static final long MsPer4Weeks = MsPerDay * 28;
            private static String[] updateFrequencyItems;
            private static final Long[] UpdateFrequencyValues = new Long[]{MsPerDay, MsPerDay * 2, MsPer3Days, MsPerDay * 5, MsPerDay * 7, MsPerDay * 14, MsPer4Weeks};

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
            }
        }

        //Rates
        public static abstract class Rates
        {
            //Update rates
            public static IconSpinner.Item[] updateRateItems;

            //Initializes values
            public static void initValues(Context context)
            {
                Resources res = context.getResources();

                //if values are not set
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

        //Creates an on play bar changed listener
        private static PlayBar.OnPlayBarChangedListener createOnPlayBarChangedListener(final Context context, boolean forGlobe, boolean forSensitivity)
        {
            return(new PlayBar.OnPlayBarChangedListener()
            {
                @Override
                public void onProgressChanged(PlayBar seekBar, int progressValue, double subProgressPercent, boolean fromUser)
                {
                    float scaleValue = progressValue / 100f;

                    if(forSensitivity)
                    {
                        //set sensitivity
                        Settings.setMapSensitivityScale(context, scaleValue, forGlobe);
                    }
                    else
                    {
                        //set speed
                        Settings.setMapSpeedScale(context, scaleValue, forGlobe);
                    }

                    //update text
                    seekBar.setScaleText(String.format(Locale.US, "%3d%%", progressValue));
                }
            });
        }

        //Creates an on item selected listener
        private static AdapterView.OnItemSelectedListener createOnItemSelectedListener(final Context context, final View childView, final String preferenceKey, final Integer[] itemValues, final SharedPreferences.Editor writeSettings)
        {
            return(new AdapterView.OnItemSelectedListener()
            {
                @Override
                public void onItemSelected(final AdapterView<?> parent, View view, final int position, long id)
                {
                    boolean resetData = false;
                    boolean updateSwitch = false;
                    boolean saveSetting = true;
                    boolean switchValue = false;
                    boolean switchEnabled = true;
                    Integer selectedValue;

                    //if there are items and position is valid
                    if(itemValues != null && position < itemValues.length)
                    {
                        selectedValue = itemValues[position];

                        //if for map layer
                        if(preferenceKey.startsWith(PreferenceName.MapLayerType))
                        {
                            //if child view is set
                            if(childView != null)
                            {
                                //show clouds selection if on a satellite layer
                                childView.setVisibility(selectedValue == CoordinatesFragment.MapLayerType.Satellite || selectedValue == CoordinatesFragment.MapLayerType.Hybrid ? View.VISIBLE : View.GONE);
                            }
                        }
                        //else if for marker information location
                        else if(preferenceKey.equals(PreferenceName.MapMarkerInfoLocation))
                        {
                            //save setting here
                            Settings.setMapMarkerInfoLocation(context, selectedValue);
                            saveSetting = false;
                        }
                        else
                        {
                            switch(preferenceKey)
                            {
                                case PreferenceName.SatelliteSource:
                                    //if context is set and child view is a switch
                                    if(context != null && childView instanceof SwitchCompat)
                                    {
                                        //update switch
                                        updateSwitch = true;
                                        switchValue = !Settings.getSatelliteSourceUseGP(context, selectedValue);
                                        switchEnabled = (selectedValue != Database.UpdateSource.N2YO);
                                    }

                                    //reset if not current source
                                    resetData = (context != null && selectedValue != getSatelliteSource(context));
                                    break;

                                default:
                                    //reset if context is set
                                    resetData = (context != null);
                                    break;
                            }
                        }

                        //if saving setting
                        if(saveSetting)
                        {
                            //save setting
                            writeSettings.putInt(preferenceKey, selectedValue).apply();
                        }

                        //if updating switch
                        if(updateSwitch)
                        {
                            //remember switch
                            SwitchCompat currentSwitch = (SwitchCompat)childView;

                            //set switch
                            currentSwitch.setChecked(switchValue);
                            currentSwitch.setEnabled(switchEnabled);
                        }

                        //if resetting data
                        if(resetData)
                        {
                            //clear master satellites
                            Database.clearMasterSatelliteTable(context);
                        }
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) { }
            });
        }
        private static AdapterView.OnItemSelectedListener createOnItemSelectedListener(View childView, final String preferenceKey, final Integer[] itemValues, final SharedPreferences.Editor writeSettings)
        {
            return(createOnItemSelectedListener(null, childView, preferenceKey, itemValues, writeSettings));
        }
        private static AdapterView.OnItemSelectedListener createOnItemSelectedListener(final Context context, final View childView, final String preferenceKey, final Byte[] itemValues, final SharedPreferences.Editor writeSettings)
        {
            int index;
            Integer[] intItemValues = new Integer[itemValues.length];

            for(index = 0; index < intItemValues.length; index++)
            {
                intItemValues[index] = (int)itemValues[index];
            }

            return(createOnItemSelectedListener(context, childView, preferenceKey, intItemValues, writeSettings));
        }
        private static AdapterView.OnItemSelectedListener createOnItemSelectedListener(final String preferenceKey, final Byte[] itemValues, final SharedPreferences.Editor writeSettings)
        {
            return(createOnItemSelectedListener(null, null, preferenceKey, itemValues, writeSettings));
        }
        private static AdapterView.OnItemSelectedListener createOnItemSelectedListener(final IconSpinner.CustomAdapter adapter, final String preferenceKey, final SharedPreferences.Editor writeSettings)
        {
            return(new AdapterView.OnItemSelectedListener()
            {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
                {
                    //save setting
                    writeSettings.putInt(preferenceKey, (Integer)adapter.getItemValue(position)).apply();
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) { }
            });
        }

        //Creates an on frequency item selected listener
        private static AdapterView.OnItemSelectedListener createOnFrequencyItemSelectedListener(final String preferenceKey, final SharedPreferences.Editor writeSettings)
        {
            return(new AdapterView.OnItemSelectedListener()
            {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
                {
                    Context context = parent.getContext();
                    UpdateService.AlarmUpdateSettings settings = getAutoUpdateSettings(context, preferenceKey);

                    //if values are set, valid index, and settings are changing
                    if(position < Updates.UpdateFrequencyValues.length && settings.rate != Updates.UpdateFrequencyValues[position])
                    {
                        //save setting
                        writeSettings.putLong(preferenceKey, Updates.UpdateFrequencyValues[position]).apply();

                        //apply changes
                        setAutoUpdate(context, preferenceKey);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) { }
            });
        }

        private static View.OnClickListener createColorButtonClickedListener(final Context context, final int startColor, final int titleId, final String preferenceKey, final boolean allowOpacity, final SharedPreferences readSettings, final SharedPreferences.Editor writeSettings)
        {
            return(new View.OnClickListener()
            {
                @Override
                public void onClick(final View v)
                {
                    ChooseColorDialog colorDialog = new ChooseColorDialog(context, readSettings.getInt(preferenceKey, startColor));
                    colorDialog.setAllowOpacity(allowOpacity);
                    colorDialog.setAllowTransparent(allowOpacity);
                    colorDialog.setTitle(context.getResources().getString(titleId));
                    colorDialog.setOnColorSelectedListener(new ChooseColorDialog.OnColorSelectedListener()
                    {
                        @Override
                        public void onColorSelected(int color)
                        {
                            writeSettings.putInt(preferenceKey, color).apply();
                            v.setBackgroundColor(color);
                        }
                    });
                    colorDialog.show(context);
                }
            });
        }

        //Creates an on checked changed listener
        private static CompoundButton.OnCheckedChangeListener createAutoCheckedChangedListener(final String preferenceKey, final float textValue, final float userValue, final EditText textView, final SharedPreferences.Editor writeSettings)
        {
            return(new CompoundButton.OnCheckedChangeListener()
            {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked)
                {
                    //update display
                    if(isChecked)
                    {
                        //reset to hardware value
                        textView.setEnabled(true);
                        textView.setText(Globals.getNumberString(textValue, 3));
                    }
                    else
                    {
                        //show user value
                        textView.setText(Globals.getNumberString(userValue, 3));
                    }

                    //update state
                    textView.setEnabled(!isChecked);

                    //save setting
                    writeSettings.putBoolean(preferenceKey, isChecked).apply();
                }
            });
        }
        private static CompoundButton.OnCheckedChangeListener createStateCheckedChangedListener(final String preferenceKey, final SharedPreferences.Editor writeSettings)
        {
            return(new CompoundButton.OnCheckedChangeListener()
            {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked)
                {
                    //save setting
                    writeSettings.putBoolean(preferenceKey, isChecked).apply();
                }
            });
        }
        private static CompoundButton.OnCheckedChangeListener createCheckedChangedListener(final String preferenceKey, final SharedPreferences.Editor writeSettings, final View autoRow)
        {
            return(new CompoundButton.OnCheckedChangeListener()
            {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked)
                {
                    boolean forUpdate = (autoRow != null);
                    Context context = compoundButton.getContext();
                    String subKey = "";
                    UpdateService.AlarmUpdateSettings settings = getAutoUpdateSettings(context, preferenceKey);

                    //if -not for auto- or -auto settings are changing-
                    if(!forUpdate || settings.enabled != isChecked)
                    {
                        //if for GP data usage
                        if(preferenceKey.equals(PreferenceName.SatelliteSourceUseGP))
                        {
                            //get sub key
                            subKey = String.valueOf(Settings.getSatelliteSource(context));

                            //invert value
                            isChecked = !isChecked;
                        }

                        //save setting
                        writeSettings.putBoolean(preferenceKey + subKey, isChecked).apply();

                        //if for update
                        if(forUpdate)
                        {
                            //apply changes
                            setAutoUpdate(context, preferenceKey);
                        }
                    }

                    //if for auto
                    if(forUpdate)
                    {
                        //update display
                        autoRow.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                    }
                }
            });
        }

        //Creates an on time set listener
        private static TimeInputView.OnTimeSetListener createOnTimeSetListener(final String preferenceHourKey, final String preferenceMinuteKey, final SharedPreferences.Editor writeSettings)
        {
            return(new TimeInputView.OnTimeSetListener()
            {
                @Override
                public void onTimeSet(TimeInputView timeView, int hour, int minute)
                {
                    Context context = timeView.getContext();
                    UpdateService.AlarmUpdateSettings settings = getAutoUpdateSettings(context, preferenceHourKey);

                    //if settings are changing
                    if(settings.hour != hour || settings.minute != minute)
                    {
                        //save setting
                        writeSettings.putInt(preferenceHourKey, hour);
                        writeSettings.putInt(preferenceMinuteKey, minute);
                        writeSettings.apply();

                        //apply changes
                        setAutoUpdate(context, preferenceHourKey);
                    }
                }
            });
        }

        //Creates an on text changed listener
        private static TextWatcher createOnTextChangedListener(final EditText view, final String preferenceKey, final SharedPreferences.Editor writeSettings)
        {
            return(new TextWatcher()
            {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

                @Override
                public void afterTextChanged(Editable editable)
                {
                    boolean save = false;
                    float value = Globals.tryParseFloat(editable.toString());

                    //if valid
                    if(value != Float.MAX_VALUE)
                    {
                        switch(preferenceKey)
                        {
                            case PreferenceName.LensWidth:
                            case PreferenceName.LensHeight:
                                //save if valid range
                                save = (value >= 0 && value <= 360);
                                break;

                            case PreferenceName.LensAzimuthUserOffset:
                                //save if valid range
                                save = (value >= -180 && value <= 360);
                                break;
                        }
                    }

                    //if saving
                    if(save)
                    {
                        //save setting
                        writeSettings.putFloat(preferenceKey, value).apply();

                        //clear any error
                        view.setError(null);
                    }
                    else
                    {
                        //show error
                        view.setError(view.getContext().getResources().getString(R.string.title_invalid));
                    }
                }
            });
        }

        //Creates an on settings item clicked listener
        private static View.OnClickListener createOnSettingsItemClickListener(final Selectable.ListFragment page, final int subPage)
        {
            return(new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    //update page
                    page.onUpdatePage(PageType.Other, subPage);
                }
            });
        }

        //Creates the view
        public static View onCreateView(Selectable.ListFragment page, int pageNum, int subPage, LayoutInflater inflater, ViewGroup container, boolean simple)
        {
            float cameraWidth;
            float cameraHeight;
            float userCameraWidth;
            float userCameraHeight;
            final Resources res;
            final SharedPreferences readSettings;
            final SharedPreferences.Editor writeSettings;
            final IconSpinner.CustomAdapter rateListAdapter;
            final IconSpinner.CustomAdapter sensorSmoothingAdapter;
            final Activity context = page.getActivity();
            final ViewGroup rootView = (ViewGroup)inflater.inflate(pageNum == PageType.Updates     ? R.layout.settings_updates :
                                                                   subPage == SubPageType.Accounts ? R.layout.settings_other_accounts :
                                                                   subPage == SubPageType.Display  ? R.layout.settings_other_display :
                                                                   subPage == SubPageType.ListView ? R.layout.settings_other_list_view :
                                                                   subPage == SubPageType.LensView ? R.layout.settings_other_lens_view :
                                                                   subPage == SubPageType.MapView  ? R.layout.settings_other_map_view :
                                                                                                     R.layout.settings_other, container, false);
            //if context exists
            if(context != null)
            {
                //get resources and settings
                res = context.getResources();
                readSettings = getPreferences(context);
                writeSettings = readSettings.edit();

                //initialize values
                Rates.initValues(context);
                IconSpinner.Item[] sensorSmoothingItems = new IconSpinner.Item[]
                {
                    new IconSpinner.Item(res.getString(R.string.title_high), 80),
                    new IconSpinner.Item(res.getString(R.string.title_medium), 40),
                    new IconSpinner.Item(res.getString(R.string.title_low), 10)
                };
                Updates.updateFrequencyItems = new String[]
                {
                    res.getQuantityString(R.plurals.text_days, 1, 1),
                    res.getQuantityString(R.plurals.text_days, 2, 2),
                    res.getQuantityString(R.plurals.text_days, 3, 3),
                    res.getQuantityString(R.plurals.text_days, 5, 5),
                    res.getQuantityString(R.plurals.text_weeks, 1, 1),
                    res.getQuantityString(R.plurals.text_weeks, 2, 2),
                    res.getQuantityString(R.plurals.text_weeks, 4, 4)
                };
                MapView.mapTypeItems = new String[]
                {
                    res.getString(R.string.title_normal),
                    res.getQuantityString(R.plurals.title_satellites, 1),
                    /*res.getString(R.string.title_terrain),*/
                    res.getString(R.string.title_hybrid)
                };
                MapView.infoLocationItems = new String[]
                {
                    res.getString(R.string.title_none),
                    res.getString(R.string.title_under_name),
                    res.getString(R.string.title_screen_bottom)
                };
                Settings.Options.Display.initValues(context);

                //create adapters
                rateListAdapter = new IconSpinner.CustomAdapter(context, Rates.updateRateItems);
                sensorSmoothingAdapter = new IconSpinner.CustomAdapter(context, sensorSmoothingItems);

                switch(pageNum)
                {
                    case PageType.Updates:
                        final IconSpinner satellitesList = rootView.findViewById(R.id.Settings_Updates_Satellites_List);
                        final IconSpinner catalogAutoList = rootView.findViewById(R.id.Settings_Updates_Catalog_Auto_List);
                        final IconSpinner tleAutoList = rootView.findViewById(R.id.Settings_Updates_TLE_Auto_List);
                        final IconSpinner altitudeList = rootView.findViewById(R.id.Settings_Updates_Altitude_List);
                        final IconSpinner timeZoneList = rootView.findViewById(R.id.Settings_Updates_Time_Zone_List);
                        final IconSpinner informationList = rootView.findViewById(R.id.Settings_Updates_Information_List);
                        final SwitchCompat rocketBodySwitch = rootView.findViewById(R.id.Settings_Updates_Catalog_Rocket_Body_Switch);
                        final SwitchCompat debrisSwitch = rootView.findViewById(R.id.Settings_Updates_Catalog_Debris_Switch);
                        final SwitchCompat legacyDataSwitch = rootView.findViewById(R.id.Settings_Updates_Use_Legacy_Data_Switch);
                        final SwitchCompat catalogAutoSwitch = rootView.findViewById(R.id.Settings_Updates_Catalog_Auto_Switch);
                        final SwitchCompat tleAutoSwitch = rootView.findViewById(R.id.Settings_Updates_TLE_Auto_Switch);
                        final SwitchCompat translateInformationSwitch = rootView.findViewById(R.id.Settings_Updates_Information_Translate_Switch);
                        final SwitchCompat shareTranslateSwitch = rootView.findViewById(R.id.Settings_Updates_Information_Translate_Share_Switch);
                        final TimeInputView catalogAutoTimeText = rootView.findViewById(R.id.Settings_Updates_Catalog_Auto_Time_Text);
                        final TimeInputView tleAutoTimeText = rootView.findViewById(R.id.Settings_Updates_TLE_Auto_Time_Text);
                        final View catalogAutoRow = rootView.findViewById(R.id.Settings_Updates_Catalog_Auto_Row);
                        final View tleAutoRow = rootView.findViewById(R.id.Settings_Updates_TLE_Auto_Row);
                        final UpdateService.AlarmUpdateSettings catalogAutoSettings = getAutoUpdateSettings(context, UpdateService.UpdateType.GetMasterList);
                        final UpdateService.AlarmUpdateSettings tleAutoSettings = getAutoUpdateSettings(context, UpdateService.UpdateType.UpdateSatellites);
                        int altitudeSource = getAltitudeSource(context);
                        int timeZoneSource = getTimeZoneSource(context);
                        int informationSource = getInformationSource(context);
                        int satelliteSource = getSatelliteSource(context);

                        //initialize values
                        Updates.initValues(context);

                        //set events
                        satellitesList.setOnItemSelectedListener(createOnItemSelectedListener((simple ? null : context), (simple ? null : legacyDataSwitch), PreferenceName.SatelliteSource, Updates.SatelliteSourceValues, writeSettings));
                        debrisSwitch.setOnCheckedChangeListener(createCheckedChangedListener(PreferenceName.CatalogDebris, writeSettings, null));
                        rocketBodySwitch.setOnCheckedChangeListener(createCheckedChangedListener(PreferenceName.CatalogRocketBodies, writeSettings, null));
                        if(!simple)
                        {
                            legacyDataSwitch.setOnCheckedChangeListener(createCheckedChangedListener(PreferenceName.SatelliteSourceUseGP, writeSettings, null));
                            altitudeList.setOnItemSelectedListener(createOnItemSelectedListener(PreferenceName.AltitudeSource, Updates.AltitudeSourceValues, writeSettings));
                            timeZoneList.setOnItemSelectedListener(createOnItemSelectedListener(PreferenceName.TimeZoneSource, Updates.TimeZoneSourceValues, writeSettings));
                            informationList.setOnItemSelectedListener(createOnItemSelectedListener(PreferenceName.InformationSource, Updates.InformationSourceValues, writeSettings));
                            translateInformationSwitch.setOnCheckedChangeListener(createCheckedChangedListener(PreferenceName.TranslateInformation, writeSettings, null));
                            shareTranslateSwitch.setOnCheckedChangeListener(createCheckedChangedListener(PreferenceName.ShareTranslations, writeSettings, null));
                        }

                        //set auto events
                        catalogAutoSwitch.setOnCheckedChangeListener(createCheckedChangedListener(PreferenceName.CatalogAutoUpdate, writeSettings, catalogAutoRow));
                        catalogAutoList.setOnItemSelectedListener(createOnFrequencyItemSelectedListener(PreferenceName.CatalogAutoUpdateRate, writeSettings));
                        catalogAutoTimeText.setOnTimeSetListener(createOnTimeSetListener(PreferenceName.CatalogAutoUpdateHour, PreferenceName.CatalogAutoUpdateMinute, writeSettings));
                        tleAutoSwitch.setOnCheckedChangeListener(createCheckedChangedListener(PreferenceName.TLEAutoUpdate, writeSettings, tleAutoRow));
                        tleAutoList.setOnItemSelectedListener(createOnFrequencyItemSelectedListener(PreferenceName.TLEAutoUpdateRate, writeSettings));
                        tleAutoTimeText.setOnTimeSetListener(createOnTimeSetListener(PreferenceName.TLEAutoUpdateHour, PreferenceName.TLEAutoUpdateMinute, writeSettings));

                        //set list items
                        satellitesList.setAdapter(new IconSpinner.CustomAdapter(context, Updates.SatelliteSourceItems, Updates.SatelliteSourceImageIds, Updates.SatelliteSourceSubTexts));
                        catalogAutoList.setAdapter(new IconSpinner.CustomAdapter(context, Updates.updateFrequencyItems));
                        tleAutoList.setAdapter(new IconSpinner.CustomAdapter(context, Updates.updateFrequencyItems));
                        if(!simple)
                        {
                            altitudeList.setAdapter(new IconSpinner.CustomAdapter(context, Updates.AltitudeSourceItems, Updates.AltitudeSourceImageIds));
                            timeZoneList.setAdapter(new IconSpinner.CustomAdapter(context, Updates.TimeZoneSourceItems, Updates.TimeZoneSourceImageIds));
                            informationList.setAdapter(new IconSpinner.CustomAdapter(context, Updates.InformationSourceItems, Updates.InformationSourceImageIds));
                        }

                        //update displays
                        satellitesList.setSelectedValue(Updates.SatelliteSourceItems[satelliteSource]);
                        catalogAutoSwitch.setChecked(catalogAutoSettings.enabled);
                        catalogAutoList.setSelection(Arrays.asList(Updates.UpdateFrequencyValues).indexOf(catalogAutoSettings.rate));
                        catalogAutoTimeText.setTime(catalogAutoSettings.hour, catalogAutoSettings.minute);
                        debrisSwitch.setChecked(getCatalogDebris(context));
                        rocketBodySwitch.setChecked(getCatalogRocketBodies(context));
                        if(!simple)
                        {
                            legacyDataSwitch.setChecked(!Settings.getSatelliteSourceUseGP(context, satelliteSource));
                            legacyDataSwitch.setEnabled(satelliteSource != Database.UpdateSource.N2YO);
                        }
                        if(simple && !tleAutoSettings.enabled)
                        {
                            //set opposite to force change
                            tleAutoSwitch.setChecked(!tleAutoSettings.enabled);
                            tleAutoSettings.enabled = !tleAutoSettings.enabled;
                        }
                        else
                        {
                            //set to current
                            tleAutoSwitch.setChecked(tleAutoSettings.enabled);
                        }
                        tleAutoList.setSelection(Arrays.asList(Updates.UpdateFrequencyValues).indexOf(tleAutoSettings.rate));
                        tleAutoTimeText.setTime(tleAutoSettings.hour, tleAutoSettings.minute);
                        if(!simple)
                        {
                            altitudeList.setSelectedValue(Updates.AltitudeSourceItems[altitudeSource == LocationService.OnlineSource.MapQuest ? 0 : 1]);
                            timeZoneList.setSelectedValue(Updates.TimeZoneSourceItems[timeZoneSource == LocationService.OnlineSource.GeoNames ? 0 : 1]);
                            informationList.setSelectedValue(Updates.InformationSourceItems[informationSource == Database.UpdateSource.NASA ? 0 : 1]);
                            translateInformationSwitch.setChecked(getTranslateInformation(context));
                            shareTranslateSwitch.setChecked(getShareTranslations(context));
                        }

                        //if simple
                        if(simple)
                        {
                            //hide displays
                            legacyDataSwitch.setVisibility(View.GONE);
                            rootView.findViewById(R.id.Settings_Updates_Altitude_Row).setVisibility(View.GONE);
                            rootView.findViewById(R.id.Settings_Updates_Altitude_Text).setVisibility(View.GONE);
                            rootView.findViewById(R.id.Settings_Updates_Altitude_Divider).setVisibility(View.GONE);
                            rootView.findViewById(R.id.Settings_Updates_Time_Zone_Row).setVisibility(View.GONE);
                            rootView.findViewById(R.id.Settings_Updates_Time_Zone_Text).setVisibility(View.GONE);
                            rootView.findViewById(R.id.Settings_Updates_Time_Zone_Divider).setVisibility(View.GONE);
                            rootView.findViewById(R.id.Settings_Updates_Information_Row).setVisibility(View.GONE);
                            rootView.findViewById(R.id.Settings_Updates_Information_Text).setVisibility(View.GONE);
                            rootView.findViewById(R.id.Settings_Updates_Information_Divider).setVisibility(View.GONE);
                            translateInformationSwitch.setVisibility(View.GONE);
                            shareTranslateSwitch.setVisibility(View.GONE);
                        }
                        break;

                    default:
                        //handle based on sub page
                        switch(subPage)
                        {
                            case SubPageType.Display:
                                final boolean isMetric = getMetricUnits(context);
                                final SwitchCompat darkThemeSwitch = rootView.findViewById(R.id.Settings_Display_Dark_Theme_Switch);
                                final IconSpinner colorThemeList = rootView.findViewById(R.id.Settings_Display_Color_Theme_List);
                                final AppCompatRadioButton metricRadio = rootView.findViewById(R.id.Settings_Display_Metric_Radio);
                                final AppCompatRadioButton imperialRadio = rootView.findViewById(R.id.Settings_Display_Imperial_Radio);

                                //set events
                                darkThemeSwitch.setOnCheckedChangeListener(Display.createOnDarkThemeCheckedChangedListener(page, context, writeSettings));
                                colorThemeList.setOnItemSelectedListener(Display.createOnColorItemSelectedListener(page, context, writeSettings));
                                metricRadio.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
                                {
                                    @Override
                                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
                                    {
                                        imperialRadio.setChecked(!isChecked);
                                        setMetricUnits(context, isChecked);
                                    }
                                });
                                imperialRadio.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
                                {
                                    @Override
                                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
                                    {
                                        metricRadio.setChecked(!isChecked);
                                    }
                                });

                                //set list items
                                colorThemeList.setAdapter(new IconSpinner.CustomAdapter(context, Display.colorAdvancedItems));

                                //update displays
                                darkThemeSwitch.setChecked(readSettings.getBoolean(PreferenceName.DarkTheme, false));
                                colorThemeList.setSelectedValue(readSettings.getInt(PreferenceName.ColorTheme, Display.ThemeIndex.Cyan));       //default to Display.ThemeIndex.Cyan
                                metricRadio.setChecked(isMetric);
                                imperialRadio.setChecked(!isMetric);
                                break;

                            case SubPageType.ListView:
                                final SwitchCompat combinedSwitch = rootView.findViewById(R.id.Settings_List_View_Combined_Switch);
                                final SwitchCompat pathProgressSwitch = rootView.findViewById(R.id.Settings_List_View_Pass_Progress_Switch);
                                final IconSpinner listUpdateRateList = rootView.findViewById(R.id.Settings_List_View_Update_Rate_List);

                                //set events
                                combinedSwitch.setOnCheckedChangeListener(createStateCheckedChangedListener(PreferenceName.UseCombinedCurrentLayout, writeSettings));
                                pathProgressSwitch.setOnCheckedChangeListener(createStateCheckedChangedListener(PreferenceName.ListShowPassProgress, writeSettings));
                                listUpdateRateList.setOnItemSelectedListener(createOnItemSelectedListener(rateListAdapter, PreferenceName.ListUpdateDelay, writeSettings));

                                //set list items
                                listUpdateRateList.setAdapter(rateListAdapter);

                                //update displays
                                combinedSwitch.setChecked(Settings.getCombinedCurrentLayout(context));
                                pathProgressSwitch.setChecked(Settings.getListPathProgress(context));
                                listUpdateRateList.setSelectedValue(readSettings.getInt(PreferenceName.ListUpdateDelay, 1000));                      //default to average
                                break;

                            case SubPageType.LensView:
                                final IconSpinner lensOrbitalIconList = rootView.findViewById(R.id.Settings_Lens_View_Orbital_Icon_List);
                                final IconSpinner lensUpdateRateList = rootView.findViewById(R.id.Settings_Lens_View_Update_Rate_List);
                                final IconSpinner lensSensorSmoothingList = rootView.findViewById(R.id.Settings_Lens_View_Sensor_Smoothing_List);
                                final BorderButton horizonColorButton = rootView.findViewById(R.id.Settings_Lens_View_Horizon_Color_Button);
                                final SwitchCompat useHorizonSwitch = rootView.findViewById(R.id.Settings_Lens_View_Use_Horizon_Switch);
                                final SwitchCompat useCameraSwitch = rootView.findViewById(R.id.Settings_Lens_View_Use_Camera_Switch);
                                final SwitchCompat rotateSwitch = rootView.findViewById(R.id.Settings_Lens_Rotate_Camera_Switch);
                                final SwitchCompat autoWidthSwitch = rootView.findViewById(R.id.Settings_Lens_View_Lens_Auto_Width_Switch);
                                final SwitchCompat autoHeightSwitch = rootView.findViewById(R.id.Settings_Lens_View_Lens_Auto_Height_Switch);
                                final EditText widthText = rootView.findViewById(R.id.Settings_Lens_View_Lens_Width_Text);
                                final EditText heightText = rootView.findViewById(R.id.Settings_Lens_View_Lens_Height_Text);
                                final EditText azimuthOffsetText = rootView.findViewById(R.id.Settings_Lens_View_Azimuth_Offset_Text);
                                final boolean useRotate = Settings.getLensRotate(context);
                                final boolean useAutoWidth = Settings.getLensAutoWidth(context);
                                final boolean useAutoHeight = Settings.getLensAutoHeight(context);

                                IconSpinner.Item[] indicatorItems = new IconSpinner.Item[]
                                {
                                    new IconSpinner.Item(Globals.getDrawable(context, R.drawable.orbital_satellite, true), res.getString(R.string.title_icon), IndicatorType.Icon),
                                    new IconSpinner.Item(Globals.getDrawable(context, R.drawable.shape_circle_black, true), res.getString(R.string.title_circle), IndicatorType.Circle),
                                    new IconSpinner.Item(Globals.getDrawable(context, R.drawable.shape_square_black, true), res.getString(R.string.title_square), IndicatorType.Square),
                                    new IconSpinner.Item(Globals.getDrawable(context, R.drawable.shape_triangle_black, true), res.getString(R.string.title_triangle), IndicatorType.Triangle)
                                };
                                IconSpinner.CustomAdapter indicatorListAdapter = new IconSpinner.CustomAdapter(context, indicatorItems);

                                //get lens width and height
                                try
                                {
                                    Camera.Parameters cameraParams;
                                    cameraParams = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK).getParameters();
                                    cameraWidth = cameraParams.getHorizontalViewAngle();
                                    cameraHeight = cameraParams.getVerticalViewAngle();
                                }
                                catch(Exception ex)
                                {
                                    cameraWidth = cameraHeight = 45;
                                }
                                userCameraWidth = Settings.getLensWidth(context);
                                userCameraHeight = Settings.getLensHeight(context);

                                //set events
                                lensOrbitalIconList.setOnItemSelectedListener(createOnItemSelectedListener(indicatorListAdapter, PreferenceName.LensIndicator, writeSettings));
                                lensUpdateRateList.setOnItemSelectedListener(createOnItemSelectedListener(rateListAdapter, PreferenceName.LensUpdateDelay, writeSettings));
                                lensSensorSmoothingList.setOnItemSelectedListener(createOnItemSelectedListener(sensorSmoothingAdapter, PreferenceName.LensAverageCount, writeSettings));
                                horizonColorButton.setOnClickListener(createColorButtonClickedListener(context, Settings.getLensHorizonColor(context), R.string.title_horizon_color, PreferenceName.LensHorizonColor, false, readSettings, writeSettings));
                                useHorizonSwitch.setOnCheckedChangeListener(createStateCheckedChangedListener(PreferenceName.LensUseHorizon, writeSettings));
                                useCameraSwitch.setOnCheckedChangeListener(createStateCheckedChangedListener(PreferenceName.LensUseCamera, writeSettings));
                                rotateSwitch.setOnCheckedChangeListener(createStateCheckedChangedListener(PreferenceName.LensRotate, writeSettings));
                                autoWidthSwitch.setOnCheckedChangeListener(createAutoCheckedChangedListener(PreferenceName.LensUseAutoWidth, cameraWidth, userCameraWidth, widthText, writeSettings));
                                autoHeightSwitch.setOnCheckedChangeListener(createAutoCheckedChangedListener(PreferenceName.LensUseAutoHeight, cameraHeight, userCameraHeight, heightText, writeSettings));
                                widthText.addTextChangedListener(createOnTextChangedListener(widthText, PreferenceName.LensWidth, writeSettings));
                                heightText.addTextChangedListener(createOnTextChangedListener(heightText, PreferenceName.LensHeight, writeSettings));
                                azimuthOffsetText.addTextChangedListener(createOnTextChangedListener(azimuthOffsetText, PreferenceName.LensAzimuthUserOffset, writeSettings));

                                //set list items
                                lensOrbitalIconList.setAdapter(indicatorListAdapter);
                                lensUpdateRateList.setAdapter(rateListAdapter);
                                lensSensorSmoothingList.setAdapter(sensorSmoothingAdapter);

                                //update displays
                                lensOrbitalIconList.setSelectedValue(readSettings.getInt(PreferenceName.LensIndicator, IndicatorType.Icon));
                                lensUpdateRateList.setSelectedValue(readSettings.getInt(PreferenceName.LensUpdateDelay, 1000));                    //default to average
                                lensSensorSmoothingList.setSelectedValue(readSettings.getInt(PreferenceName.LensAverageCount, 40));
                                horizonColorButton.setBackgroundColor(Settings.getLensHorizonColor(context));
                                useHorizonSwitch.setChecked(readSettings.getBoolean(PreferenceName.LensUseHorizon, false));
                                useCameraSwitch.setChecked(readSettings.getBoolean(PreferenceName.LensUseCamera, true));
                                rotateSwitch.setChecked(useRotate);
                                autoWidthSwitch.setChecked(useAutoWidth);
                                autoHeightSwitch.setChecked(useAutoHeight);
                                widthText.setText(Globals.getNumberString(useAutoWidth ? cameraWidth : Settings.getLensWidth(context), 3));
                                heightText.setText(Globals.getNumberString(useAutoHeight ? cameraHeight : Settings.getLensHeight(context), 3));
                                azimuthOffsetText.setText(Globals.getNumberString(Settings.getLensAzimuthUserOffset(context), 3));
                                break;

                            case SubPageType.MapView:
                                final IconSpinner globeTypeList = rootView.findViewById(R.id.Settings_Map_View_Globe_Type_List);
                                final IconSpinner mapTypeList = rootView.findViewById(R.id.Settings_Map_View_Map_Type_List);
                                final IconSpinner informationLocationList = rootView.findViewById(R.id.Settings_Map_View_Information_Location_List);
                                final BorderButton gridColorButton = rootView.findViewById(R.id.Settings_Map_View_Grid_Color_Button);
                                final PlayBar globeSensitivityScaleBar = rootView.findViewById(R.id.Settings_Map_View_Globe_Sensitivity_Scale_Bar);
                                final PlayBar globeSpeedScaleBar = rootView.findViewById(R.id.Settings_Map_View_Globe_Speed_Scale_Bar);
                                final PlayBar mapSensitivityScaleBar = rootView.findViewById(R.id.Settings_Map_View_Map_Sensitivity_Scale_Bar);
                                final PlayBar mapSpeedScaleBar = rootView.findViewById(R.id.Settings_Map_View_Map_Speed_Scale_Bar);
                                final PlayBar iconScaleBar = rootView.findViewById(R.id.Settings_Map_View_Icon_Scale_Bar);
                                final SwitchCompat show3dPathsSwitch = rootView.findViewById(R.id.Settings_Map_View_Show_3d_Paths_Switch);
                                final SwitchCompat showGlobeCloudsSwitch = rootView.findViewById(R.id.Settings_Map_View_Show_Globe_Clouds_Switch);
                                final SwitchCompat showMapCloudsSwitch = rootView.findViewById(R.id.Settings_Map_View_Show_Map_Clouds_Switch);
                                final SwitchCompat showInformationBackgroundSwitch = rootView.findViewById(R.id.Settings_Map_View_Show_Information_Background_Switch);
                                final SwitchCompat useSearchSwitch = rootView.findViewById(R.id.Settings_Map_View_Show_Search_Switch);
                                final SwitchCompat useZoomSwitch = rootView.findViewById(R.id.Settings_Map_View_Show_Zoom_Switch);
                                final SwitchCompat useLabelsAlwaysSwitch = rootView.findViewById(R.id.Settings_Map_View_Show_Labels_Always_Switch);
                                final SwitchCompat useShadowsSwitch = rootView.findViewById(R.id.Settings_Map_View_Show_Shadows_Switch);
                                final SwitchCompat useStarsSwitch = rootView.findViewById(R.id.Settings_Map_View_Show_Stars_Switch);
                                final SwitchCompat useGridSwitch = rootView.findViewById(R.id.Settings_Map_View_Use_Grid_Switch);
                                final SwitchCompat allowRotationSwitch = rootView.findViewById(R.id.Settings_Map_View_Allow_Rotation_Switch);

                                //set events
                                globeTypeList.setOnItemSelectedListener(createOnItemSelectedListener(showGlobeCloudsSwitch, PreferenceName.MapLayerType + SubPreferenceName.Globe, MapView.MapTypeValues, writeSettings));
                                mapTypeList.setOnItemSelectedListener(createOnItemSelectedListener(showMapCloudsSwitch, PreferenceName.MapLayerType + SubPreferenceName.Map, MapView.MapTypeValues, writeSettings));
                                informationLocationList.setOnItemSelectedListener(createOnItemSelectedListener(context, null, PreferenceName.MapMarkerInfoLocation, MapView.InfoLocationValues, writeSettings));
                                showInformationBackgroundSwitch.setOnCheckedChangeListener(createStateCheckedChangedListener(PreferenceName.MapMarkerShowBackground, writeSettings));
                                gridColorButton.setOnClickListener(createColorButtonClickedListener(context, Settings.getMapGridColor(context), R.string.title_grid_color, PreferenceName.MapGridColor, true, readSettings, writeSettings));
                                show3dPathsSwitch.setOnCheckedChangeListener(createStateCheckedChangedListener(PreferenceName.MapShow3dPaths, writeSettings));
                                showGlobeCloudsSwitch.setOnCheckedChangeListener(createStateCheckedChangedListener(PreferenceName.ShowSatelliteClouds + SubPreferenceName.Globe, writeSettings));
                                showMapCloudsSwitch.setOnCheckedChangeListener(createStateCheckedChangedListener(PreferenceName.ShowSatelliteClouds + SubPreferenceName.Map, writeSettings));
                                useSearchSwitch.setOnCheckedChangeListener(createStateCheckedChangedListener(PreferenceName.MapShowSearchList, writeSettings));
                                useZoomSwitch.setOnCheckedChangeListener(createStateCheckedChangedListener(PreferenceName.MapShowZoom, writeSettings));
                                useLabelsAlwaysSwitch.setOnCheckedChangeListener(createStateCheckedChangedListener(PreferenceName.MapShowLabelsAlways, writeSettings));
                                useShadowsSwitch.setOnCheckedChangeListener(createStateCheckedChangedListener(PreferenceName.MapMarkerShowShadow, writeSettings));
                                useStarsSwitch.setOnCheckedChangeListener(createStateCheckedChangedListener(PreferenceName.MapShowStars, writeSettings));
                                useGridSwitch.setOnCheckedChangeListener(createStateCheckedChangedListener(PreferenceName.MapShowGrid, writeSettings));
                                allowRotationSwitch.setOnCheckedChangeListener(createStateCheckedChangedListener(PreferenceName.MapRotateAllowed, writeSettings));

                                //set list items
                                globeTypeList.setAdapter(new IconSpinner.CustomAdapter(context, MapView.mapTypeItems));
                                mapTypeList.setAdapter(new IconSpinner.CustomAdapter(context, MapView.mapTypeItems));
                                informationLocationList.setAdapter(new IconSpinner.CustomAdapter(context, MapView.infoLocationItems));

                                //setup sensitivities
                                globeSensitivityScaleBar.setMin(Settings.SensitivityScaleMin);
                                globeSensitivityScaleBar.setMax(Settings.SensitivityScaleMax);
                                globeSensitivityScaleBar.setPlayIndexIncrementUnits(1);
                                globeSensitivityScaleBar.setPlayActivity(null);
                                globeSensitivityScaleBar.setOnSeekChangedListener(createOnPlayBarChangedListener(context, true, true));
                                mapSensitivityScaleBar.setMin(Settings.SensitivityScaleMin);
                                mapSensitivityScaleBar.setMax(Settings.SensitivityScaleMax);
                                mapSensitivityScaleBar.setPlayIndexIncrementUnits(1);
                                mapSensitivityScaleBar.setPlayActivity(null);
                                mapSensitivityScaleBar.setOnSeekChangedListener(createOnPlayBarChangedListener(context, false, true));

                                //setup speeds
                                globeSpeedScaleBar.setMin(Settings.SpeedScaleMin);
                                globeSpeedScaleBar.setMax(Settings.SpeedScaleMax);
                                globeSpeedScaleBar.setPlayIndexIncrementUnits(1);
                                globeSpeedScaleBar.setPlayActivity(null);
                                globeSpeedScaleBar.setOnSeekChangedListener(createOnPlayBarChangedListener(context, true, false));
                                mapSpeedScaleBar.setMin(Settings.SpeedScaleMin);
                                mapSpeedScaleBar.setMax(Settings.SpeedScaleMax);
                                mapSpeedScaleBar.setPlayIndexIncrementUnits(1);
                                mapSpeedScaleBar.setPlayActivity(null);
                                mapSpeedScaleBar.setOnSeekChangedListener(createOnPlayBarChangedListener(context, false, false));

                                //setup scale
                                iconScaleBar.setMin(Settings.IconScaleMin);
                                iconScaleBar.setMax(Settings.IconScaleMax);
                                iconScaleBar.setPlayIndexIncrementUnits(1);
                                iconScaleBar.setPlayActivity(null);
                                iconScaleBar.setOnSeekChangedListener(new PlayBar.OnPlayBarChangedListener()
                                {
                                    @Override
                                    public void onProgressChanged(PlayBar seekBar, int progressValue, double subProgressPercent, boolean fromUser)
                                    {
                                        //set scale
                                        Settings.setMapMarkerScale(context, progressValue / 100f);

                                        //update text
                                        seekBar.setScaleText(String.format(Locale.US, "%3d%%", progressValue));
                                    }
                                });

                                //update displays
                                globeTypeList.setSelection(Arrays.asList(MapView.MapTypeValues).indexOf(getMapLayerType(context, true)));
                                mapTypeList.setSelection(Arrays.asList(MapView.MapTypeValues).indexOf(getMapLayerType(context, false)));
                                informationLocationList.setSelection(Arrays.asList(MapView.InfoLocationValues).indexOf(getMapMarkerInfoLocation(context)));
                                gridColorButton.setBackgroundColor(Settings.getMapGridColor(context));
                                globeSensitivityScaleBar.setValue((int)Math.floor(Settings.getMapSensitivityScale(context, true) * 100));
                                globeSpeedScaleBar.setValue((int)Math.floor(Settings.getMapSpeedScale(context, true) * 100));
                                mapSensitivityScaleBar.setValue((int)Math.floor(Settings.getMapSensitivityScale(context, false) * 100));
                                mapSpeedScaleBar.setValue((int)Math.floor(Settings.getMapSpeedScale(context, false) * 100));
                                iconScaleBar.setValue((int)Math.floor(Settings.getMapMarkerScale(context) * 100));
                                showInformationBackgroundSwitch.setChecked(Settings.getMapMarkerShowBackground(context));
                                show3dPathsSwitch.setChecked(Settings.getMapShow3dPaths(context));
                                showGlobeCloudsSwitch.setChecked(Settings.getSatelliteClouds(context, false));
                                showMapCloudsSwitch.setChecked(Settings.getSatelliteClouds(context, true));
                                useSearchSwitch.setChecked(Settings.getMapShowSearchList(context));
                                useZoomSwitch.setChecked(Settings.getMapShowZoom(context));
                                useLabelsAlwaysSwitch.setChecked(Settings.getMapShowLabelsAlways(context));
                                useShadowsSwitch.setChecked(Settings.getMapMarkerShowShadow(context));
                                useStarsSwitch.setChecked(Settings.getMapShowStars(context));
                                useGridSwitch.setChecked(Settings.getMapShowGrid(context));
                                allowRotationSwitch.setChecked(Settings.getMapRotateAllowed(context));
                                break;

                            default:
                            case SubPageType.None:
                                final boolean havePositionSensors = SensorUpdate.havePositionSensors(context);
                                final TextView accountsText = rootView.findViewById(R.id.Settings_Accounts_Text);
                                final TextView displayText = rootView.findViewById(R.id.Settings_Display_Text);
                                final TextView listViewText = rootView.findViewById(R.id.Settings_List_View_Text);
                                final TextView lensViewText = rootView.findViewById(R.id.Settings_Lens_View_Text);
                                final TextView mapViewText = rootView.findViewById(R.id.Settings_Map_View_Text);
                                final TextView widgetsText = rootView.findViewById(R.id.Settings_Widgets_Text);

                                //update displays
                                accountsText.setCompoundDrawablesWithIntrinsicBounds(Globals.getDrawable(context, R.drawable.ic_account_circle_white, true), null, null, null);
                                displayText.setCompoundDrawablesWithIntrinsicBounds(Globals.getDrawable(context, R.drawable.ic_tablet_white, true), null, null, null);
                                listViewText.setCompoundDrawablesWithIntrinsicBounds(Globals.getDrawable(context, R.drawable.ic_list_white, true), null, null, null);
                                if(havePositionSensors)
                                {
                                    lensViewText.setCompoundDrawablesWithIntrinsicBounds(Globals.getDrawable(context, R.drawable.ic_photo_camera_white, true), null, null, null);
                                }
                                lensViewText.setVisibility(havePositionSensors ? View.VISIBLE : View.GONE);
                                mapViewText.setCompoundDrawablesWithIntrinsicBounds(Globals.getDrawable(context, R.drawable.ic_map_white, true), null, null, null);
                                widgetsText.setCompoundDrawablesWithIntrinsicBounds(Globals.getDrawable(context, R.drawable.ic_widgets_black, true), null, null, null);

                                //set events
                                accountsText.setOnClickListener(createOnSettingsItemClickListener(page, SubPageType.Accounts));
                                displayText.setOnClickListener(createOnSettingsItemClickListener(page, SubPageType.Display));
                                listViewText.setOnClickListener(createOnSettingsItemClickListener(page, SubPageType.ListView));
                                if(havePositionSensors)
                                {
                                    lensViewText.setOnClickListener(createOnSettingsItemClickListener(page, SubPageType.LensView));
                                }
                                mapViewText.setOnClickListener(createOnSettingsItemClickListener(page, SubPageType.MapView));
                                widgetsText.setOnClickListener(createOnSettingsItemClickListener(page, SubPageType.Widgets));
                                break;
                        }
                        break;
                }
            }

            //return view
            return(rootView);
        }
    }

    //Locations
    public static abstract class Locations
    {
        //Item
        public static class Item extends Selectable.ListItem
        {
            public String name;
            public double latitude;
            public double longitude;
            public double altitudeM;
            public byte locationType;
            public TimeZone zone;

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
        public static class ItemHolder extends Selectable.ListItemHolder
        {
            AppCompatImageView locationImage;
            TextView locationText;

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
            private final Item[] locations;

            public ItemListAdapter(View parentView, int titleStringId, String categoryTitle)
            {
                super(parentView, categoryTitle);

                int index;
                Database.DatabaseLocation[] dbLocations = Database.getLocations(currentContext);

                //set displays and pending data
                knownLatitude = Double.MAX_VALUE;
                knownLongitude = Double.MAX_VALUE;
                knownAltitude = Double.MAX_VALUE;
                columnTitleStringId = titleStringId;

                //setup items
                locations = new Item[dbLocations.length];
                for(index = 0; index < dbLocations.length; index++)
                {
                    Database.DatabaseLocation currentLoc = dbLocations[index];
                    locations[index] = new Item(currentLoc.id, index, currentLoc.name, currentLoc.latitude, currentLoc.longitude, currentLoc.altitudeKM * 1000, currentLoc.zoneId, currentLoc.locationType, (titleStringId < 0 && currentLoc.locationType != Database.LocationType.Current), false, currentLoc.isChecked);
                }
                this.itemsRefID = R.layout.settings_locations_item;
            }
            public ItemListAdapter(View parentView, String title)
            {
                this(parentView, -1, title);
            }

            @Override
            public int getItemCount()
            {
                return(locations.length);
            }

            @Override
            public Item getItem(int position)
            {
                return(locations[position]);
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

            @Override
            protected void onItemNonEditClick(Selectable.ListItem item, int pageNum)
            {
                int offset = 0;
                String text;
                final Item currentItem = (Item)item;
                final boolean haveContext = (currentContext != null);
                final boolean isCurrent = (currentItem.locationType == Database.LocationType.Current);
                final double latitude = currentItem.latitude;
                final double longitude = currentItem.longitude;
                final double altitudeM = currentItem.altitudeM;
                final Resources res = (haveContext ? currentContext.getResources() : null);
                final TextView[] titles;
                final TextView[] texts;

                //if have context and -a valid latitude, longitude, or altitude-
                if(haveContext && ((latitude != 0 && latitude >= -90 && latitude <= 90) || (longitude != 0 && longitude >= -180 && longitude <= 180) || (altitudeM != 0 && altitudeM != Double.MAX_VALUE)))
                {
                    //create dialog
                    final ItemDetailDialog detailDialog = new ItemDetailDialog(currentContext, listInflater, Universe.IDs.None, currentItem.name, Globals.getDrawable(currentContext, Globals.getLocationIcon(currentItem.locationType), true), itemDetailButtonClickListener);

                    //get displays
                    titles = detailDialog.getItemDetailTitles();
                    texts = detailDialog.getItemDetailTexts();

                    //
                    //update displays
                    //
                    text = res.getString(R.string.title_name) + ":";
                    titles[offset].setText(text);
                    TableRow.LayoutParams viewParams = (TableRow.LayoutParams)texts[offset].getLayoutParams();
                    viewParams.span = 4;
                    detailDialog.hideVerticalItemDetailDivider(offset);
                    texts[offset++].setLayoutParams(viewParams);
                    offset++;

                    text = res.getString(R.string.title_latitude) + ":";
                    titles[offset].setText(text);
                    texts[offset++].setText(Globals.getLatitudeDirectionString(res, latitude, 4));

                    text = res.getString(R.string.title_altitude) + ":";
                    titles[offset].setText(text);
                    text = Globals.getNumberString(Globals.getMetersUnitValue(altitudeM)) + " " + Globals.getMetersLabel(res);
                    texts[offset++].setText(text);

                    text = res.getString(R.string.title_longitude) + ":";
                    titles[offset].setText(text);
                    texts[offset++].setText(Globals.getLongitudeDirectionString(res, longitude, 4));
                    offset++;

                    text = res.getString(R.string.title_time_zone) + ":";
                    titles[offset].setText(text);
                    viewParams = (TableRow.LayoutParams)texts[offset].getLayoutParams();
                    viewParams.span = 4;
                    detailDialog.hideVerticalItemDetailDivider(offset);
                    texts[offset++].setLayoutParams(viewParams);

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

                                        texts[0].setText(success ? locationString : unknown);
                                        texts[6].setText(success ? currentItem.zone.getDisplayName() : unknown);
                                    }
                                });
                            }
                        }, false);
                    }
                    else
                    {
                        //use existing name
                        texts[0].setText(currentItem.name);
                        texts[6].setText(currentItem.zone.getDisplayName());
                    }

                    //update display visibilities
                    detailDialog.showItemDetailRows(offset, 0, isCurrent);
                    detailDialog.show();
                }
            }

            @Override
            public @NonNull ItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
            {
                View itemView = LayoutInflater.from(parent.getContext()).inflate(this.itemsRefID, parent, false);
                ItemHolder itemHolder = new ItemHolder(itemView, R.id.Location_Item_CheckBox, R.id.Location_Item_Image, R.id.Location_Item_Text);

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
        public static class Item extends Selectable.ListItem
        {
            public String name;
            public CalculateService.AlarmNotifySettings passStartSettings;
            public CalculateService.AlarmNotifySettings passEndSettings;
            public CalculateService.AlarmNotifySettings fullStartSettings;
            public CalculateService.AlarmNotifySettings fullEndSettings;

            public Item(int id, String nm, CalculateService.AlarmNotifySettings pssStrtSttngs, CalculateService.AlarmNotifySettings pssEndSttngs, CalculateService.AlarmNotifySettings fllStrtSttngs, CalculateService.AlarmNotifySettings fllEndSttngs)
            {
                super(id, -1, true, false, true, false);
                name = nm;
                passStartSettings = pssStrtSttngs;
                passEndSettings = pssEndSttngs;
                fullStartSettings = fllStrtSttngs;
                fullEndSettings = fllEndSttngs;
            }
        }

        //Item holder
        public static class ItemHolder extends Selectable.ListItemHolder
        {
            AppCompatImageView notifyImage;
            TextView nameText;
            TextView passText;
            TextView passStartText;
            TextView passEndText;
            TextView fullText;
            TextView fullStartText;
            TextView fullEndText;

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
            private final Item[] notifications;

            public ItemListAdapter(View parentView, String title)
            {
                super(parentView, title);

                Resources res = currentContext.getResources();
                Database.DatabaseSatellite[] orbitals = Database.getOrbitals(currentContext);
                ArrayList<Item> notifyList = new ArrayList<>(0);

                //go through each orbital
                for(Database.DatabaseSatellite currentOrbital : orbitals)
                {
                    CalculateService.AlarmNotifySettings passStartSettings = Settings.getNotifyPassSettings(currentContext, currentOrbital.norad, Globals.NotifyType.PassStart);
                    CalculateService.AlarmNotifySettings passEndSettings = Settings.getNotifyPassSettings(currentContext, currentOrbital.norad, Globals.NotifyType.PassEnd);
                    CalculateService.AlarmNotifySettings fullStartSettings = Settings.getNotifyPassSettings(currentContext, currentOrbital.norad, Globals.NotifyType.FullMoonStart);
                    CalculateService.AlarmNotifySettings fullEndSettings = Settings.getNotifyPassSettings(currentContext, currentOrbital.norad, Globals.NotifyType.FullMoonEnd);

                    //if using start or ending notification for any
                    if(passStartSettings.isEnabled() || passEndSettings.isEnabled() || fullStartSettings.isEnabled() || fullEndSettings.isEnabled())
                    {
                        //add orbital
                        notifyList.add(new Item(currentOrbital.norad, currentOrbital.getName(), passStartSettings, passEndSettings, fullStartSettings, fullEndSettings));
                    }
                }

                //set items and layout ID
                if(notifyList.size() == 0)
                {
                    notifyList.add(new Item(Integer.MIN_VALUE, res.getString(R.string.title_none), new CalculateService.AlarmNotifySettings(), new CalculateService.AlarmNotifySettings(), new CalculateService.AlarmNotifySettings(), new CalculateService.AlarmNotifySettings()));
                }
                notifications = notifyList.toArray(new Item[0]);
                this.itemsRefID = R.layout.settings_notify_layout;
            }

            @Override
            public int getItemCount()
            {
                return(notifications.length);
            }

            @Override
            public Item getItem(int position)
            {
                return(notifications[position]);
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
                    AppCompatImageView imageColumn = listColumns.findViewById(R.id.Settings_Notify_Item_Image);

                    imageColumn.setVisibility(View.INVISIBLE);
                    ((TextView)listColumns.findViewById(R.id.Settings_Notify_Item_Name_Text)).setText(R.string.title_name);
                }

                super.setColumnTitles(listColumns, categoryText, page);
            }

            @Override
            protected void onItemNonEditClick(Selectable.ListItem item, int pageNum)
            {
                Item currentItem = (Item)item;
                int noradId = currentItem.id;

                //if a valid ID
                if(noradId != Integer.MIN_VALUE)
                {
                    //start edit activity
                    NotifySettingsActivity.show(currentContext, noradId, MainActivity.getObserver());
                }
            }

            @Override
            public @NonNull ItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
            {
                View itemView = LayoutInflater.from(parent.getContext()).inflate(this.itemsRefID, parent, false);
                ItemHolder itemHolder = new ItemHolder(itemView, R.id.Settings_Notify_Item_Image, R.id.Settings_Notify_Item_Name_Text, R.id.Settings_Notify_Item_Pass_Text, R.id.Settings_Notify_Item_Pass_Start_Text, R.id.Settings_Notify_Item_Pass_End_Text, R.id.Settings_Notify_Item_Full_Text, R.id.Settings_Notify_Item_Full_Start_Text, R.id.Settings_Notify_Item_Full_End_Text);

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
                }
                itemHolder.nameText.setText(currentItem.name);
                if(usePassStart || usePassEnd)
                {
                    itemHolder.passText.setVisibility(View.VISIBLE);

                    if(usePassStart)
                    {
                        setNotifyText(itemHolder.passStartText, true, currentItem.passStartSettings.nextOnly);
                    }
                    if(usePassEnd)
                    {
                        setNotifyText(itemHolder.passEndText, false, currentItem.passEndSettings.nextOnly);
                    }
                }
                if(useFullStart || useFullEnd)
                {
                    itemHolder.fullText.setVisibility(View.VISIBLE);

                    if(useFullStart)
                    {
                        setNotifyText(itemHolder.fullStartText, true, currentItem.fullStartSettings.nextOnly);
                    }
                    if(useFullEnd)
                    {
                        setNotifyText(itemHolder.fullEndText, false, currentItem.fullEndSettings.nextOnly);
                    }
                }

                //set background
                setItemBackground(itemHolder.itemView, false);
            }

            //Sets notify text
            private void setNotifyText(TextView notifyText, boolean up, boolean nextOnly)
            {
                notifyText.setText(up ? Globals.Symbols.Up : Globals.Symbols.Down);
                notifyText.setVisibility(View.VISIBLE);
                notifyText.setCompoundDrawablesWithIntrinsicBounds(null, null, Globals.getDrawable(currentContext, nextOnly ? R.drawable.ic_repeat_one_white : R.drawable.ic_repeat_white, true), null);
            }
        }
    }

    //Widgets
    public static abstract class Widgets
    {
        //Item
        public static class Item extends Selectable.ListItem
        {
            public String name;
            public String location;
            public Class<?> widgetClass;

            public Item(int id, int index, String nm, String loc, Class<?> wClass)
            {
                super(id, index, false, false, false, false);
                name = nm;
                location = loc;
                widgetClass = wClass;
            }
        }

        //Item holder
        public static class ItemHolder extends Selectable.ListItemHolder
        {
            AppCompatImageView widgetImage;
            TextView nameText;
            TextView locationText;

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

            public ItemListAdapter(View parentView)
            {
                super(parentView, null);

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
            protected void setColumnTitles(ViewGroup listColumns, TextView categoryText, int page)
            {
                AppCompatImageView imageColumn = listColumns.findViewById(R.id.Settings_Widget_Item_Image);

                imageColumn.setVisibility(View.INVISIBLE);
                ((TextView)listColumns.findViewById(R.id.Settings_Widget_Item_Name_Text)).setText(R.string.title_name);
                ((TextView)listColumns.findViewById(R.id.Settings_Widget_Item_Location_Text)).setText(R.string.title_location);

                super.setColumnTitles(listColumns, categoryText, page);
            }

            @Override
            protected void onItemNonEditClick(Selectable.ListItem item, int pageNum)
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
                    ((Activity)currentContext).startActivityForResult(editIntent, BaseInputActivity.RequestCode.EditWidget);
                }
            }

            @Override
            public @NonNull ItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
            {
                View itemView = LayoutInflater.from(parent.getContext()).inflate(this.itemsRefID, parent, false);
                ItemHolder itemHolder = new ItemHolder(itemView, R.id.Settings_Widget_Item_Image, R.id.Settings_Widget_Item_Name_Text, R.id.Settings_Widget_Item_Location_Text);

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
                itemHolder.widgetImage.setImageDrawable(widgetId != 0 ? Globals.getOrbitalIcon(currentContext, MainActivity.getObserver(), noradId, WidgetBaseSetupActivity.getOrbitalType(currentContext, currentItem.id)) : Globals.getDrawable(currentContext, R.drawable.ic_widgets_black, (noradId > 0)));
                itemHolder.nameText.setText(currentItem.name);
                itemHolder.locationText.setText(currentItem.location);

                //set background
                setItemBackground(itemHolder.itemView, false);
            }

            public void reload()
            {
                int index;
                int widgetId;
                Resources res = currentContext.getResources();
                int[] passTinyWidgetIds = WidgetPassBaseProvider.getWidgetIds(currentContext, WidgetPassTinyProvider.class);
                int[] passSmallWidgetIds = WidgetPassBaseProvider.getWidgetIds(currentContext, WidgetPassSmallProvider.class);
                int[] passMediumWidgetIds = WidgetPassBaseProvider.getWidgetIds(currentContext, WidgetPassMediumProvider.class);
                int tinyOffset = passTinyWidgetIds.length;
                int smallOffset = passSmallWidgetIds.length;

                //setup items
                widgets = new Item[passTinyWidgetIds.length + passSmallWidgetIds.length + passMediumWidgetIds.length];

                //add all tiny widgets
                for(index = 0; index < passTinyWidgetIds.length; index++)
                {
                    widgetId = passTinyWidgetIds[index];
                    widgets[index] = new Item(widgetId, index, WidgetBaseSetupActivity.getName(currentContext, widgetId), WidgetBaseSetupActivity.getLocationName(currentContext, widgetId), WidgetPassTinyProvider.class);
                }

                //add all small widgets
                for(index = 0; index < passSmallWidgetIds.length; index++)
                {
                    widgetId = passSmallWidgetIds[index];
                    widgets[index + tinyOffset] = new Item(widgetId, index + tinyOffset, WidgetBaseSetupActivity.getName(currentContext, widgetId), WidgetBaseSetupActivity.getLocationName(currentContext, widgetId), WidgetPassSmallProvider.class);
                }

                //add all medium widgets
                for(index = 0; index < passMediumWidgetIds.length; index++)
                {
                    widgetId = passMediumWidgetIds[index];
                    widgets[index + tinyOffset + smallOffset] = new Item(widgetId, index + tinyOffset + smallOffset, WidgetBaseSetupActivity.getName(currentContext, widgetId), WidgetBaseSetupActivity.getLocationName(currentContext, widgetId), WidgetPassMediumProvider.class);
                }

                //if no items
                if(widgets.length == 0)
                {
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

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
        {
            int group = this.getGroupParam();
            int page = this.getPageParam();
            int subPage = this.getSubPageParam();
            Selectable.ListBaseAdapter listAdapter;

            //set list adapter based on page
            switch(page)
            {
                case PageType.Accounts:
                    listAdapter = new Options.Accounts.ItemListAdapter(this, categoryTitle);
                    break;

                case PageType.Locations:
                    listAdapter = new Locations.ItemListAdapter(Page.this.listParentView, categoryTitle);
                    break;

                case PageType.Notifications:
                    listAdapter = new Notifications.ItemListAdapter(this.listParentView, categoryTitle);
                    break;

                case PageType.Updates:
                case PageType.Other:
                    switch(subPage)
                    {
                        case SubPageType.Accounts:
                            listAdapter = new Options.Accounts.ItemListAdapter(this, null);
                            break;

                        case SubPageType.Widgets:
                            listAdapter = new Widgets.ItemListAdapter(this.listParentView);
                            break;

                        default:
                            return(Options.onCreateView(this, page, subPage, inflater, container, false));
                    }
                    break;

                default:
                    listAdapter = null;
                    break;
            }

            //create view
            return(this.onCreateView(inflater, container, listAdapter, group, page));
        }

        @Override
        protected boolean setupActionModeItems(MenuItem edit, MenuItem delete, MenuItem save, MenuItem sync)
        {
            boolean onLocations = (pageNum == PageType.Locations);
            boolean onNotifications = (pageNum == PageType.Notifications);
            boolean onAccounts = isOnAccounts();

            //set visibility
            edit.setVisible(false);
            delete.setVisible(inEditMode && (onLocations || onNotifications || onAccounts));
            save.setVisible(false);
            sync.setVisible(false);

            //handled
            return(true);
        }

        @Override
        protected void onActionModeEdit() {}

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
            Selectable.ListItem[] items = selectedItems.toArray(new Selectable.ListItem[0]);

            //go through selected items
            for(Selectable.ListItem currentItem : items)
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

                    case PageType.Other:
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
            return(pageNum == PageType.Accounts || (pageNum == PageType.Other && this.getSubPageParam() == SubPageType.Accounts));
        }
    }

    //Page adapter
    public static class PageAdapter extends Selectable.ListFragmentAdapter
    {
        public PageAdapter(FragmentManager fm, View parentView, Selectable.ListFragment.OnItemSelectedListener selectedListener, Selectable.ListFragment.OnItemCheckChangedListener checkChangedListener, Selectable.ListFragment.OnAdapterSetListener adapterListener, Selectable.ListFragment.OnUpdateNeededListener updateListener, Selectable.ListFragment.OnUpdatePageListener updatePgListener, Selectable.ListFragment.OnPageResumeListener resumeListener, int[] subPg)
        {
            super(fm, parentView, selectedListener, updateListener, updatePgListener, checkChangedListener, null, adapterListener, resumeListener, MainActivity.Groups.Settings, subPg);
        }

        @Override
        public @NonNull Fragment getItem(int position)
        {
            return(this.getItem(group, position, subPage[position], new Page(null)));
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

                case PageType.Other:
                    return(res.getString(R.string.title_other));

                default:
                    return(res.getString(R.string.title_invalid));
            }
        }
    }

    //Status of using metric units, grid, and map marker bottom info
    private static boolean usingMetric = true;
    private static boolean usingCurrentGridLayout = false;
    private static boolean mapMarkerInfoBottom = true;

    //Gets settings preferences
    public static SharedPreferences getPreferences(Context context)
    {
        return(context.getSharedPreferences("Settings", Context.MODE_PRIVATE));
    }

    //Gets write settings
    public static SharedPreferences.Editor getWriteSettings(Context context)
    {
        return(getPreferences(context).edit());
    }

    //Sets given boolean preference
    private static void setPreferenceBoolean(Context context, String name, boolean value)
    {
        getWriteSettings(context).putBoolean(name, value).apply();
    }

    //Sets given int preference
    private static void setPreferenceInt(Context context, String name, int value)
    {
        getWriteSettings(context).putInt(name, value).apply();
    }

    //Gets if first run
    public static boolean getFirstRun(Context context)
    {
        return(getPreferences(context).getBoolean(PreferenceName.FirstRun, true));
    }

    //Sets if first run
    public static void setFirstRun(Context context, boolean firstRun)
    {
        setPreferenceBoolean(context, PreferenceName.FirstRun, firstRun);
    }

    //Gets if accepted privacy policy
    public static boolean getAcceptedPrivacy(Context context)
    {
        return(getPreferences(context).getBoolean(PreferenceName.AcceptedPrivacy, false));
    }

    //Sets if accepted privacy policy
    public static void setAcceptedPrivacy(Context context, boolean accepted)
    {
        setPreferenceBoolean(context, PreferenceName.AcceptedPrivacy, accepted);
    }

    //Gets if combined notice shown
    public static boolean getCombinedShown(Context context)
    {
        return(getPreferences(context).getBoolean(PreferenceName.CombinedNotice, false));
    }

    //Sets if combined noticed shown
    public static void setCombinedShown(Context context, boolean shown)
    {
        setPreferenceBoolean(context, PreferenceName.CombinedNotice, shown);
    }

    //Gets indicator type
    public static int getIndicator(Context context)
    {
        return(getPreferences(context).getInt(PreferenceName.LensIndicator, Options.IndicatorType.Icon));
    }

    //Sets indicator type
    public static void setIndicator(Context context, int indicatorType)
    {
        setPreferenceInt(context, PreferenceName.LensIndicator, indicatorType);
    }

    //Gets lens average count
    public static int getLensAverageCount(Context context)
    {
        return(getPreferences(context).getInt(PreferenceName.LensAverageCount, 40));
    }

    //Gets dark theme value
    public static boolean getDarkTheme(Context context)
    {
        return(getPreferences(context).getBoolean(PreferenceName.DarkTheme, false));
    }

    //Gets color theme value
    public static int getColorTheme(Context context)
    {
        return(getPreferences(context).getInt(PreferenceName.ColorTheme, Options.Display.ThemeIndex.Cyan));
    }

    //Gets metric units value
    public static boolean getMetricUnits(Context context)
    {
        return(getPreferences(context).getBoolean(PreferenceName.MetricUnits, true));
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

    //Get lens first run
    public static boolean getLensFirstRun(Context context)
    {
        return(getPreferences(context).getBoolean(PreferenceName.LensFirstRun, true));
    }

    //Sets lens first run
    public static void setLensFirstRun(Context context, boolean first)
    {
        setPreferenceBoolean(context, PreferenceName.LensFirstRun, first);
    }

    //Get lens horizon color
    public static int getLensHorizonColor(Context context)
    {
        return(getPreferences(context).getInt(PreferenceName.LensHorizonColor, Color.rgb(150, 150, 150)));
    }

    //Get showing lens horizon
    public static boolean getLensShowHorizon(Context context)
    {
        return(getPreferences(context).getBoolean(PreferenceName.LensUseHorizon, false));
    }

    //Sets showing lens horizon
    public static void setLensShowHorizon(Context context, boolean show)
    {
        setPreferenceBoolean(context, PreferenceName.LensUseHorizon, show);
    }

    //Gets lens azimuth user offset
    public static float getLensAzimuthUserOffset(Context context)
    {
        return(getPreferences(context).getFloat(PreferenceName.LensAzimuthUserOffset, 0));
    }

    //Sets lens azimuth user offset
    public static void setLensAzimuthUserOffset(Context context, float offset)
    {
        SharedPreferences.Editor writeSettings = getWriteSettings(context);
        writeSettings.putFloat(PreferenceName.LensAzimuthUserOffset, offset);
        writeSettings.apply();
    }

    //Gets if lens needs rotating
    public static boolean getLensRotate(Context context)
    {
        return(getPreferences(context).getBoolean(PreferenceName.LensRotate, false));
    }

    /*//Sets if lens needs rotating
    public static void setLensRotate(Context context, boolean rotate)
    {
        setPreferenceBoolean(context, PreferenceName.LensRotate, rotate);
    }*/

    //Gets lens using auto width
    public static boolean getLensAutoWidth(Context context)
    {
        return(getPreferences(context).getBoolean(PreferenceName.LensUseAutoWidth, false));
    }

    //Sets lens using auto width
    public static void setLensAutoWidth(Context context, boolean auto)
    {
        setPreferenceBoolean(context, PreferenceName.LensUseAutoWidth, auto);
    }

    //Gets lens width
    public static float getLensWidth(Context context)
    {
        return(getPreferences(context).getFloat(PreferenceName.LensWidth, 39.279f));
    }

    //Set lens width
    public static void setLensWidth(Context context, float width)
    {
        SharedPreferences.Editor writeSettings = getWriteSettings(context);
        writeSettings.putFloat(PreferenceName.LensWidth, width);
        writeSettings.apply();
    }

    //Gets lens using auto height
    public static boolean getLensAutoHeight(Context context)
    {
        return(getPreferences(context).getBoolean(PreferenceName.LensUseAutoHeight, false));
    }

    //Sets lens using auto height
    public static void setLensAutoHeight(Context context, boolean auto)
    {
        setPreferenceBoolean(context, PreferenceName.LensUseAutoHeight, auto);
    }

    //Gets lens height
    public static float getLensHeight(Context context)
    {
        return(getPreferences(context).getFloat(PreferenceName.LensHeight, 65.789f));
    }

    //Set lens height
    public static void setLensHeight(Context context, float height)
    {
        SharedPreferences.Editor writeSettings = getWriteSettings(context);
        writeSettings.putFloat(PreferenceName.LensHeight, height).apply();
    }

    //Gets list path progress being shown
    public static boolean getListPathProgress(Context context)
    {
        return(getPreferences(context).getBoolean(PreferenceName.ListShowPassProgress, true));
    }

    //Gets list update delay
    public static int getListUpdateDelay(Context context)
    {
        return(getPreferences(context).getInt(PreferenceName.ListUpdateDelay, 1000));
    }

    //Returns map layer type
    public static int getMapLayerType(Context context, boolean forGlobe)
    {
        return(getPreferences(context).getInt(PreferenceName.MapLayerType + (forGlobe ? SubPreferenceName.Globe : SubPreferenceName.Map), (forGlobe ? CoordinatesFragment.MapLayerType.Hybrid : CoordinatesFragment.MapLayerType.Normal)));
    }

    //Returns if using 3d path on globe
    public static boolean getMapShow3dPaths(Context context)
    {
        return(getPreferences(context).getBoolean(PreferenceName.MapShow3dPaths, true));
    }

    //Returns if using clouds on satellite layer for map/globe
    public static boolean getSatelliteClouds(Context context, boolean forMap)
    {
        return(getPreferences(context).getBoolean(PreferenceName.ShowSatelliteClouds + (forMap ? SubPreferenceName.Map : SubPreferenceName.Globe), true));
    }

    /*//Returns google map type
    public static int getGoogleMapType(Context context)
    {
        int mapLayerType = getMapLayerType(context, false);

        switch(mapLayerType)
        {
            case CoordinatesFragment.MapLayerType.Satellite:
                return(GoogleMap.MAP_TYPE_SATELLITE);

            case CoordinatesFragment.MapLayerType.Terrain:
                return(GoogleMap.MAP_TYPE_TERRAIN);

            case CoordinatesFragment.MapLayerType.Hybrid:
                return(GoogleMap.MAP_TYPE_HYBRID);

            default:
            case CoordinatesFragment.MapLayerType.Normal:
                return(GoogleMap.MAP_TYPE_NORMAL);
        }
    }*/

    //Sets map display type
    public static void setMapDisplayType(Context context, int mapDisplayType)
    {
        setPreferenceInt(context, PreferenceName.MapDisplayType, mapDisplayType);
    }

    //Returns map display type
    public static int getMapDisplayType(Context context)
    {
        return(getPreferences(context).getInt(PreferenceName.MapDisplayType, CoordinatesFragment.MapDisplayType.Globe));
    }

    //Returns map showing zoom
    public static boolean getMapShowZoom(Context context)
    {
        return(getPreferences(context).getBoolean(PreferenceName.MapShowZoom, false));
    }

    //Sets map showing zoom
    public static void setMapShowZoom(Context context, boolean show)
    {
        setPreferenceBoolean(context, PreferenceName.MapShowZoom, show);
    }

    //Returns map showing labels always
    public static boolean getMapShowLabelsAlways(Context context)
    {
        return(getPreferences(context).getBoolean(PreferenceName.MapShowLabelsAlways, true));
    }

    //Sets map showing labels always
    public static void setMapShowLabelsAlways(Context context, boolean show)
    {
        setPreferenceBoolean(context, PreferenceName.MapShowLabelsAlways, show);
    }

    //Returns map showing stars
    public static boolean getMapShowStars(Context context)
    {
        return(getPreferences(context).getBoolean(PreferenceName.MapShowStars, true));
    }

    //Gets map speed scale
    public static float getMapSpeedScale(Context context, boolean forGlobe)
    {
        return(getPreferences(context).getFloat(PreferenceName.MapSpeedScale + (forGlobe ? SubPreferenceName.Globe : SubPreferenceName.Map), 0.5f));
    }

    //Sets map speed scale
    public static void setMapSpeedScale(Context context, float scale, boolean forGlobe)
    {
        //if a valid scale
        if(scale >= (SpeedScaleMin / 100.0f) && scale <= (SpeedScaleMax / 100.0f))
        {
            //set scale
            getWriteSettings(context).putFloat(PreferenceName.MapSpeedScale + (forGlobe ? SubPreferenceName.Globe : SubPreferenceName.Map), scale).apply();
        }
    }

    //Gets map sensitivity scale
    public static float getMapSensitivityScale(Context context, boolean forGlobe)
    {
        return(getPreferences(context).getFloat(PreferenceName.MapSensitivityScale + (forGlobe ? SubPreferenceName.Globe : SubPreferenceName.Map), 0.8f));
    }

    //Sets map sensitivity scale
    public static void setMapSensitivityScale(Context context, float scale, boolean forGlobe)
    {
        //if a valid scale
        if(scale >= (SensitivityScaleMin / 100.0f) && scale <= (SensitivityScaleMax / 100.0f))
        {
            //set scale
            getWriteSettings(context).putFloat(PreferenceName.MapSensitivityScale + (forGlobe ? SubPreferenceName.Globe : SubPreferenceName.Map), scale).apply();
        }
    }

    //Returns map rotate allowed
    public static boolean getMapRotateAllowed(Context context)
    {
        return(getPreferences(context).getBoolean(PreferenceName.MapRotateAllowed, true));
    }

    //Returns map showing grid
    public static boolean getMapShowGrid(Context context)
    {
        return(getPreferences(context).getBoolean(PreferenceName.MapShowGrid, false));
    }

    //Sets map showing grid
    public static void setMapShowGrid(Context context, boolean show)
    {
        setPreferenceBoolean(context, PreferenceName.MapShowGrid, show);
    }

    //Returns map view grid color
    public static int getMapGridColor(Context context)
    {
        return(getPreferences(context).getInt(PreferenceName.MapGridColor, Color.argb(180, 0, 0, 255)));
    }

    //Returns map marker scale
    public static float getMapMarkerScale(Context context)
    {
        //get scale
        return(getPreferences(context).getFloat(PreferenceName.MapMarkerScale, 0.65f));
    }

    //Sets map marker scale
    public static void setMapMarkerScale(Context context, float scale)
    {
        //if a valid scale
        if(scale >= (IconScaleMin / 100.0f) && scale <= (IconScaleMax / 100.0f))
        {
            //set scale
            getWriteSettings(context).putFloat(PreferenceName.MapMarkerScale, scale).apply();
        }
    }

    //Returns map showing search list
    public static boolean getMapShowSearchList(Context context)
    {
        return(getPreferences(context).getBoolean(PreferenceName.MapShowSearchList, true));
    }

    //Sets map showing search list
    public static void setMapShowSearchList(Context context, boolean show)
    {
        setPreferenceBoolean(context, PreferenceName.MapShowSearchList, show);
    }

    //Returns map marker info location
    public static int getMapMarkerInfoLocation(Context context)
    {
        return(getPreferences(context).getInt(PreferenceName.MapMarkerInfoLocation, CoordinatesFragment.MapMarkerInfoLocation.ScreenBottom));
    }

    //Sets map marker info location
    public static void setMapMarkerInfoLocation(Context context, int location)
    {
        setPreferenceInt(context, PreferenceName.MapMarkerInfoLocation, location);
        mapMarkerInfoBottom = (location == CoordinatesFragment.MapMarkerInfoLocation.ScreenBottom);
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
        return(getPreferences(context).getBoolean(PreferenceName.MapMarkerShowBackground, false));
    }

    //Returns map marker showing shadow
    public static boolean getMapMarkerShowShadow(Context context)
    {
        return(getPreferences(context).getBoolean(PreferenceName.MapMarkerShowShadow, true));
    }

    //Gets if using combined current layout
    public static boolean getCombinedCurrentLayout(Context context)
    {
        return(getPreferences(context).getBoolean(PreferenceName.UseCombinedCurrentLayout, true));
    }

    /*//Sets using combined current layout
    public static void setUsingCombinedCurrentLayout(Context context, boolean use)
    {
        setPreferenceBoolean(context, PreferenceName.UseCombinedCurrentLayout, use);
    }*/

    //Gets current grid layout setting
    public static boolean getCurrentGridLayout(Context context)
    {
        return(getPreferences(context).getBoolean(PreferenceName.UseCurrentGridLayout, false));
    }

    //Sets using current grid layout
    public static void setUsingCurrentGridLayout(Context context, boolean use)
    {
        setPreferenceBoolean(context, PreferenceName.UseCurrentGridLayout, use);
        usingCurrentGridLayout = use;
    }

    //Returns if using current grid layout
    //note: faster than getting through preferences since called a lot
    public static boolean getUsingCurrentGridLayout(int page)
    {
        return(page < Current.PageType.PageCount && usingCurrentGridLayout);
    }

    //Gets sort by preference name for given page
    private static String getCurrentSortByName(int page)
    {
        switch(page)
        {
            case Current.PageType.View:
                return(PreferenceName.CurrentViewSortBy);

            case Current.PageType.Passes:
                return(PreferenceName.CurrentPassesSortBy);

            case Current.PageType.Coordinates:
                return(PreferenceName.CurrentCoordinatesSortBy);

            default:
            case Current.PageType.Combined:
                return(PreferenceName.CurrentCombinedSortBy);
        }
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
    public static int getCurrentSortBy(Context context, int page)
    {
        return(getPreferences(context).getInt(getCurrentSortByName(page), Current.Items.SortBy.Name));
    }
    public static String getCurrentSortByString(Context context, int page)
    {
        return(context.getString(getSortByStringId(getCurrentSortBy(context, page))));
    }

    //Sets current sort by for given page
    public static void setCurrentSortBy(Context context, int page, int sortBy)
    {
        setPreferenceInt(context, getCurrentSortByName(page), sortBy);
    }
    public static void setCurrentSortBy(Context context, int page, String sortByString)
    {
        setCurrentSortBy(context, page, getSortBy(context, sortByString));
    }

    //Gets last location
    public static Location getLastLocation(Context context)
    {
        Location lastLocation = new Location("");
        SharedPreferences readSettings = getPreferences(context);

        //set location
        lastLocation.setLatitude(readSettings.getFloat(PreferenceName.LocationLastLatitude, 0));
        lastLocation.setLongitude(readSettings.getFloat(PreferenceName.LocationLastLongitude, 0));
        lastLocation.setAltitude(readSettings.getFloat(PreferenceName.LocationLastAltitude, 0));

        //return
        return(lastLocation);
    }

    //Sets last location
    public static void setLastLocation(Context context, Location lastLocation)
    {
        SharedPreferences.Editor writeSettings = getWriteSettings(context);

        writeSettings.putFloat(PreferenceName.LocationLastLatitude, (float)lastLocation.getLatitude());
        writeSettings.putFloat(PreferenceName.LocationLastLongitude, (float)lastLocation.getLongitude());
        writeSettings.putFloat(PreferenceName.LocationLastAltitude, (float)lastLocation.getAltitude());
        writeSettings.apply();
    }

    //Gets altitude source
    public static int getAltitudeSource(Context context)
    {
        return(getPreferences(context).getInt(PreferenceName.AltitudeSource, LocationService.OnlineSource.MapQuest));
    }

    //Gets time zone source
    public static int getTimeZoneSource(Context context)
    {
        return(getPreferences(context).getInt(PreferenceName.TimeZoneSource, LocationService.OnlineSource.Google));
    }

    //Sets satellite source
    public static void setSatelliteSource(Context context, int source)
    {
        //if source is changing
        if(source != getSatelliteSource(context))
        {
            //clear master satellites
            Database.clearMasterSatelliteTable(context);
        }

        //save setting
        setPreferenceInt(context, PreferenceName.SatelliteSource, source);
    }

    //Gets satellites source
    public static int getSatelliteSource(Context context)
    {
        return(getPreferences(context).getInt(PreferenceName.SatelliteSource, Database.UpdateSource.SpaceTrack));
    }

    /*//Sets satellite source using GP
    public static void setSatelliteSourceUseGP(Context context, int source, boolean use)
    {
        setPreferenceBoolean(context, PreferenceName.SatelliteSourceUseGP + source, use);
    }*/

    //Gets satellite source using GP
    public static boolean getSatelliteSourceUseGP(Context context, int source)
    {
        return(getPreferences(context).getBoolean(PreferenceName.SatelliteSourceUseGP + source, (source != Database.UpdateSource.N2YO)));
    }

    //Gets owner icon(s)
    public static Drawable[] getOwnerIcons(Context context, int noradId, String ownerCode)
    {
        int index;
        int currentID;
        int[] iconIDs = Globals.getOwnerIconIDs(ownerCode);
        Drawable[] icons = new Drawable[iconIDs.length];

        //go through each icon ID
        for(index = 0; index < iconIDs.length; index++)
        {
            //remember ID and set icon
            currentID = iconIDs[index];
            icons[index] = (noradId <= 0 ? null : Globals.getDrawable(context, currentID));
        }

        //return icons
        return(icons);
    }

    //Gets catalog debris usage
    public static boolean getCatalogDebris(Context context)
    {
        return(getPreferences(context).getBoolean(PreferenceName.CatalogDebris, false));
    }

    //Gets catalog rocket bodies usage
    public static boolean getCatalogRocketBodies(Context context)
    {
        return(getPreferences(context).getBoolean(PreferenceName.CatalogRocketBodies, false));
    }

    //Gets information source
    public static int getInformationSource(Context context)
    {
        return(getPreferences(context).getInt(PreferenceName.InformationSource, Database.UpdateSource.NASA));
    }

    //Gets if translating information
    public static boolean getTranslateInformation(Context context)
    {
        return(getPreferences(context).getBoolean(PreferenceName.TranslateInformation, true));
    }

    //Gets if sharing translations
    public static boolean getShareTranslations(Context context)
    {
        return(getPreferences(context).getBoolean(PreferenceName.ShareTranslations, true));
    }

    //Gets login name and password
    public static String[] getLogin(Context context, int accountType)
    {
        SharedPreferences currentPreferences = getPreferences(context);

        switch(accountType)
        {
            case Globals.AccountType.SpaceTrack:
                return(new String[]{currentPreferences.getString(PreferenceName.SpaceTrackUser, null), Encryptor.decrypt(context, currentPreferences.getString(PreferenceName.SpaceTrackPassword, null))});

            default:
                return(new String[]{null, null});
        }
    }

    //Sets login name and password
    public static void setLogin(Context context, String user, String password, int accountType)
    {
        SharedPreferences.Editor writeSettings = getWriteSettings(context);

        switch(accountType)
        {
            case Globals.AccountType.SpaceTrack:
                writeSettings.putString(PreferenceName.SpaceTrackUser, user);
                writeSettings.putString(PreferenceName.SpaceTrackPassword, Encryptor.encrypt(context, password));
                writeSettings.apply();
                break;
        }
    }

    //Removes login name and password
    public static void removeLogin(Context context, int accountType)
    {
        SharedPreferences.Editor writeSettings = getWriteSettings(context);

        switch(accountType)
        {
            case Globals.AccountType.SpaceTrack:
                writeSettings.remove(PreferenceName.SpaceTrackUser);
                writeSettings.remove(PreferenceName.SpaceTrackPassword);
                writeSettings.apply();
                break;
        }
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
        SharedPreferences readSettings = getPreferences(context);

        settings.nextOnly = readSettings.getBoolean(getNotifyPassNextOnlyKey(noradId, notifyType), true);
        settings.noradId = noradId;
        settings.timeMs = readSettings.getLong(getNotifyPassNextMsKey(noradId, notifyType), 0);
        settings.location.timeZone = TimeZone.getTimeZone(readSettings.getString(getNotifyPassZoneIdKey(noradId, notifyType), TimeZone.getDefault().getID()));
        settings.location.geo.latitude = readSettings.getFloat(getNotifyPassLatitudeKey(noradId, notifyType), Float.MIN_VALUE);
        settings.location.geo.longitude = readSettings.getFloat(getNotifyPassLongitudeKey(noradId, notifyType), Float.MIN_VALUE);
        settings.location.geo.altitudeKm = readSettings.getFloat(getNotifyPassAltitudeKey(noradId, notifyType), Float.MIN_VALUE);

        return(settings);
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
        SharedPreferences readSettings = getPreferences(context);

        switch(updateType)
        {
            case UpdateService.UpdateType.GetMasterList:
                return(readSettings.getLong(PreferenceName.CatalogAutoUpdateNextMs, 0));

            case UpdateService.UpdateType.UpdateSatellites:
                return(readSettings.getLong(PreferenceName.TLEAutoUpdateNextMs, 0));
        }

        return(0);
    }

    //Sets auto update next time in ms
    public static void setAutoUpdateNextMs(Context context, byte updateType, long timeMs)
    {
        SharedPreferences.Editor writeSettings = getWriteSettings(context);

        switch(updateType)
        {
            case UpdateService.UpdateType.GetMasterList:
                writeSettings.putLong(PreferenceName.CatalogAutoUpdateNextMs, timeMs).apply();
                break;

            case UpdateService.UpdateType.UpdateSatellites:
                writeSettings.putLong(PreferenceName.TLEAutoUpdateNextMs, timeMs).apply();
                break;
        }
    }

    //Gets auto update alarm rate
    private static long getAutoUpdateRateMs(SharedPreferences readSettings, byte updateType)
    {
        switch(updateType)
        {
            case UpdateService.UpdateType.GetMasterList:
                return(readSettings.getLong(PreferenceName.CatalogAutoUpdateRate, Options.Updates.MsPer4Weeks));

            default:
            case UpdateService.UpdateType.UpdateSatellites:
                return(readSettings.getLong(PreferenceName.TLEAutoUpdateRate, Options.Updates.MsPer3Days));
        }
    }
    public static long getAutoUpdateRateMs(Context context, byte updateType)
    {
        return(getAutoUpdateRateMs(getPreferences(context), updateType));
    }

    //Gets auto update alarm settings
    public static UpdateService.AlarmUpdateSettings getAutoUpdateSettings(Context context, byte updateType)
    {
        UpdateService.AlarmUpdateSettings settings = new UpdateService.AlarmUpdateSettings();
        SharedPreferences readSettings = getPreferences(context);

        switch(updateType)
        {
            case UpdateService.UpdateType.GetMasterList:
                settings.enabled = readSettings.getBoolean(PreferenceName.CatalogAutoUpdate, false);
                settings.hour = readSettings.getInt(PreferenceName.CatalogAutoUpdateHour, 12);
                settings.minute = readSettings.getInt(PreferenceName.CatalogAutoUpdateMinute, 0);
                break;

            default:
            case UpdateService.UpdateType.UpdateSatellites:
                settings.enabled = readSettings.getBoolean(PreferenceName.TLEAutoUpdate, false);
                settings.hour = readSettings.getInt(PreferenceName.TLEAutoUpdateHour, 12);
                settings.minute = readSettings.getInt(PreferenceName.TLEAutoUpdateMinute, 0);
                break;
        }
        settings.rate = getAutoUpdateRateMs(readSettings, updateType);

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
        return(getPreferences(context).getBoolean(PreferenceName.AskInternet, true));
    }

    //Sets asking of internet connection
    public static void setAskInternet(Context context, boolean ask)
    {
        setPreferenceBoolean(context, PreferenceName.AskInternet, ask);
    }
}
