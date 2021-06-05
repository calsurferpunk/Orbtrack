package com.nikolaiapps.orbtrack;


import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.text.Spanned;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;


public class SettingsActivity extends AppCompatActivity implements PreferenceFragmentCompat.OnPreferenceStartFragmentCallback
{
    static
    {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    private static abstract class SetupPageType
    {
        static final int Welcome = 0;
        static final int Display = 1;
        static final int Location = 2;
        static final int Updates = 3;
        static final int Satellites = 4;
        static final int Finished = 5;
        static final int PageCount = 6;
    }

    public static abstract class ScreenKey
    {
        public static final String Accounts = "accounts";
        public static final String Display = "display";
        public static final String LensView = "lensView";
        public static final String ListView = "listView";
        public static final String Locations = "locations";
        public static final String MapView = "mapView";
        public static final String Notifications = "notifications";
        public static final String Updates = "updates";
        public static final String Widgets = "widgets";
    }

    public static class SettingsSubFragment extends PreferenceFragmentCompat
    {
        private static boolean showSetup;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey)
        {
            final FragmentActivity context = getActivity();
            Bundle args = this.getArguments();
            String screenKey;

            //if args do not exist
            if(args == null)
            {
                //create them
                args = new Bundle();
            }

            //get if showing setup and screen key
            showSetup = args.getBoolean(EXTRA_SHOW_SETUP, false);
            screenKey = args.getString(RootKey);

            //set display
            setPreferencesFromResource(R.xml.settings_main, screenKey);

            //if context and screen key exist
            if(context != null && screenKey != null)
            {
                switch(screenKey)
                {
                    case ScreenKey.Display:
                        SwitchPreference darkThemeSwitch = this.findPreference(Settings.PreferenceName.DarkTheme);
                        IconListPreference colorThemeList = this.findPreference(Settings.PreferenceName.ColorTheme);

                        //initialize values
                        Settings.Options.Display.initValues(context);

                        //setup displays
                        setupSwitch(darkThemeSwitch, null);
                        setupList(colorThemeList, Settings.Options.Display.colorAdvancedItems, null, null, null, null);
                        break;

                    case ScreenKey.LensView:
                        SwitchPreference useCameraSwitch = this.findPreference(Settings.PreferenceName.LensUseCamera);
                        SwitchPreference rotateSwitch = this.findPreference(Settings.PreferenceName.LensRotate);
                        SwitchTextPreference lensWidthSwitch = this.findPreference(Settings.PreferenceName.LensWidth);
                        SwitchTextPreference lensHeightSwitch = this.findPreference(Settings.PreferenceName.LensHeight);
                        SwitchTextPreference lensAzimuthOffsetSwitch = this.findPreference(Settings.PreferenceName.LensAzimuthUserOffset);
                        SwitchButtonPreference lensUseHorizonSwitch = this.findPreference(Settings.PreferenceName.LensUseHorizon);
                        IconListPreference lensOrbitalIconList = this.findPreference(Settings.PreferenceName.LensIndicator);
                        IconListPreference lensUpdateRateList = this.findPreference(Settings.PreferenceName.LensUpdateDelay);
                        IconListPreference lensSensorSmoothingList = this.findPreference(Settings.PreferenceName.LensAverageCount);

                        //initialize values
                        Settings.Options.LensView.initValues(context);
                        Settings.Options.Rates.initValues(context);

                        //setup displays
                        setupSwitch(useCameraSwitch, null);
                        setupSwitch(rotateSwitch, null);
                        setupSwitchButton(lensUseHorizonSwitch);
                        setupSwitchText(lensWidthSwitch);
                        setupSwitchText(lensHeightSwitch);
                        setupSwitchText(lensAzimuthOffsetSwitch);
                        setupList(lensOrbitalIconList, Settings.Options.LensView.indicatorItems, null, null, null, null);
                        setupList(lensUpdateRateList, Settings.Options.Rates.updateRateItems, null, null, null, null);
                        setupList(lensSensorSmoothingList, Settings.Options.LensView.sensorSmoothingItems, null, null, null, null);
                        break;

                    case ScreenKey.ListView:
                        SwitchPreference combinedSwitch = this.findPreference(Settings.PreferenceName.UseCombinedCurrentLayout);
                        SwitchPreference pathProgressSwitch = this.findPreference(Settings.PreferenceName.ListShowPassProgress);
                        IconListPreference listUpdateRateList = this.findPreference(Settings.PreferenceName.ListUpdateDelay);

                        //initialize values
                        Settings.Options.Rates.initValues(context);

                        //setup displays
                        setupSwitch(combinedSwitch, null);
                        setupSwitch(pathProgressSwitch, null);
                        setupList(listUpdateRateList, Settings.Options.Rates.updateRateItems, null, null, null, null);
                        break;

                    case ScreenKey.MapView:
                        SwitchPreference showCloudsGlobeSwitch = this.findPreference(Settings.PreferenceName.ShowSatelliteClouds + Settings.SubPreferenceName.Globe);
                        SwitchPreference showCloudsMapSwitch = this.findPreference(Settings.PreferenceName.ShowSatelliteClouds + Settings.SubPreferenceName.Map);
                        SwitchPreference show3dPathsSwitch = this.findPreference(Settings.PreferenceName.MapShow3dPaths);
                        SwitchPreference allowRotationSwitch = this.findPreference(Settings.PreferenceName.MapRotateAllowed);
                        SwitchPreference showInformationBackgroundSwitch = this.findPreference(Settings.PreferenceName.MapMarkerShowBackground);
                        SwitchPreference showOrbitalDirection = this.findPreference(Settings.PreferenceName.MapShowOrbitalDirection);
                        SwitchPreference showSearchSwitch = this.findPreference(Settings.PreferenceName.MapShowSearchList);
                        SwitchPreference showZoomSwitch = this.findPreference(Settings.PreferenceName.MapShowZoom);
                        SwitchPreference showLabelsAlwaysSwitch = this.findPreference(Settings.PreferenceName.MapShowLabelsAlways);
                        SwitchPreference showShadowsSwitch = this.findPreference(Settings.PreferenceName.MapMarkerShowShadow);
                        SwitchPreference showStarsSwitch = this.findPreference(Settings.PreferenceName.MapShowStars);
                        SwitchButtonPreference showGridSwitch = this.findPreference(Settings.PreferenceName.MapShowGrid);
                        SliderPreference globeSensitivitySlider = this.findPreference(Settings.PreferenceName.MapSensitivityScale + Settings.SubPreferenceName.Globe);
                        SliderPreference globeSpeedScaleSlider = this.findPreference(Settings.PreferenceName.MapSpeedScale + Settings.SubPreferenceName.Globe);
                        SliderPreference mapSensitivitySlider = this.findPreference(Settings.PreferenceName.MapSensitivityScale + Settings.SubPreferenceName.Map);
                        SliderPreference mapSpeedScaleSlider = this.findPreference(Settings.PreferenceName.MapSpeedScale + Settings.SubPreferenceName.Map);
                        SliderPreference iconScaleSlider = this.findPreference(Settings.PreferenceName.MapMarkerScale);
                        IconListPreference globeTypeList = this.findPreference(Settings.PreferenceName.MapLayerType + Settings.SubPreferenceName.Globe);
                        IconListPreference mapTypeList = this.findPreference(Settings.PreferenceName.MapLayerType + Settings.SubPreferenceName.Map);
                        IconListPreference informationLocationList = this.findPreference(Settings.PreferenceName.MapMarkerInfoLocation);

                        //initialize values
                        Settings.Options.MapView.initValues(context);

                        //setup displays
                        setupSwitch(showCloudsGlobeSwitch, null);
                        setupSwitch(showCloudsMapSwitch, null);
                        setupSwitch(show3dPathsSwitch, null);
                        setupSwitch(allowRotationSwitch, null);
                        setupSwitch(showOrbitalDirection, null);
                        setupSwitch(showInformationBackgroundSwitch, null);
                        setupSwitch(showSearchSwitch, null);
                        setupSwitch(showZoomSwitch, null);
                        setupSwitch(showLabelsAlwaysSwitch, null);
                        setupSwitch(showShadowsSwitch, null);
                        setupSwitch(showStarsSwitch, null);
                        setupSwitchButton(showGridSwitch);
                        setupSlider(globeSensitivitySlider);
                        setupSlider(globeSpeedScaleSlider);
                        setupSlider(mapSensitivitySlider);
                        setupSlider(mapSpeedScaleSlider);
                        setupSlider(iconScaleSlider);
                        setupList(globeTypeList, Settings.Options.MapView.mapTypeItems, Settings.Options.MapView.MapTypeValues, null, null, showCloudsGlobeSwitch);
                        setupList(mapTypeList, Settings.Options.MapView.mapTypeItems, Settings.Options.MapView.MapTypeValues, null, null, showCloudsMapSwitch);
                        setupList(informationLocationList, Settings.Options.MapView.infoLocationItems, Settings.Options.MapView.InfoLocationValues, null, null, null);
                        break;

                    case ScreenKey.Updates:
                        //get displays
                        SwitchPreference tleAutoSwitch = this.findPreference(Settings.PreferenceName.TLEAutoUpdate);
                        SwitchPreference catalogAutoSwitch = this.findPreference(Settings.PreferenceName.CatalogAutoUpdate);
                        SwitchPreference rocketBodySwitch = this.findPreference(Settings.PreferenceName.CatalogRocketBodies);
                        SwitchPreference debrisSwitch = this.findPreference(Settings.PreferenceName.CatalogDebris);
                        SwitchPreference legacyDataSwitch = this.findPreference(Settings.PreferenceName.SatelliteSourceUseGP);
                        SwitchPreference translateInformationSwitch = this.findPreference(Settings.PreferenceName.TranslateInformation);
                        SwitchPreference shareTranslateSwitch = this.findPreference(Settings.PreferenceName.ShareTranslations);
                        IconListPreference satellitesList = this.findPreference(Settings.PreferenceName.SatelliteSource);
                        IconListPreference altitudeList = this.findPreference(Settings.PreferenceName.AltitudeSource);
                        IconListPreference timeZoneList = this.findPreference(Settings.PreferenceName.TimeZoneSource);
                        IconListPreference informationList = this.findPreference(Settings.PreferenceName.InformationSource);
                        TimeIntervalPreference tleAutoTime = this.findPreference(Settings.PreferenceName.TLEAutoUpdateRate);
                        TimeIntervalPreference catalogAutoTime = this.findPreference(Settings.PreferenceName.CatalogAutoUpdateRate);
                        PreferenceCategory altitudeCategory = this.findPreference("AltitudeCategory");
                        PreferenceCategory timeZoneCategory = this.findPreference("TimeZoneCategory");
                        PreferenceCategory informationCategory = this.findPreference("InformationCategory");

                        //initialize values
                        Settings.Options.Updates.initValues(context);

                        //setup displays
                        setupSwitch(tleAutoSwitch, tleAutoTime);
                        setupSwitch(catalogAutoSwitch, catalogAutoTime);
                        setupSwitch(rocketBodySwitch, null);
                        setupSwitch(debrisSwitch, null);
                        setupSwitch(legacyDataSwitch, null);
                        setupSwitch(translateInformationSwitch, null);
                        setupSwitch(shareTranslateSwitch, null);
                        setupList(satellitesList, Settings.Options.Updates.SatelliteSourceItems, Settings.Options.Updates.SatelliteSourceValues, Settings.Options.Updates.SatelliteSourceImageIds, Settings.Options.Updates.SatelliteSourceSubTexts, legacyDataSwitch);
                        setupList(altitudeList, Settings.Options.Updates.AltitudeSourceItems, Settings.Options.Updates.AltitudeSourceValues, Settings.Options.Updates.AltitudeSourceImageIds, null, null);
                        setupList(timeZoneList, Settings.Options.Updates.TimeZoneSourceItems, Settings.Options.Updates.TimeZoneSourceValues, Settings.Options.Updates.TimeZoneSourceImageIds, null, null);
                        setupList(informationList, Settings.Options.Updates.InformationSourceItems, Settings.Options.Updates.InformationSourceValues, Settings.Options.Updates.InformationSourceImageIds, null, null);
                        setupTimeInterval(tleAutoTime);
                        setupTimeInterval(catalogAutoTime);
                        setupCategory(altitudeCategory);
                        setupCategory(timeZoneCategory);
                        setupCategory(informationCategory);

                        //if showing setup
                        if(showSetup)
                        {
                            //if TLE auto switch exists
                            if(tleAutoSwitch != null)
                            {
                                //set to checked and call change listener
                                tleAutoSwitch.setChecked(true);
                                tleAutoSwitch.getOnPreferenceChangeListener().onPreferenceChange(tleAutoSwitch, true);
                            }
                        }
                        break;
                }
            }
        }

        //Gets if preference should be visible
        private static boolean preferenceVisible(String preferenceKey)
        {
            switch(preferenceKey)
            {
                case Settings.PreferenceName.SatelliteSourceUseGP:
                case Settings.PreferenceName.AltitudeSource:
                case Settings.PreferenceName.TimeZoneSource:
                case Settings.PreferenceName.InformationSource:
                case Settings.PreferenceName.TranslateInformation:
                case Settings.PreferenceName.ShareTranslations:
                    return(!showSetup);

                default:
                    return(true);
            }
        }

        //Sets up a list
        private static void setupList(IconListPreference preference, Object[] items, Object[] values, int[] itemImageIds, String[] subTexts, Preference childPreference)
        {
            //if preference exists
            if(preference != null)
            {
                final Context context = preference.getContext();
                final String preferenceKey = preference.getKey();
                boolean forGlobe = false;
                int valueIndex = -1;
                Object currentValue = null;

                //if preference should be visible
                if(preferenceVisible(preferenceKey))
                {
                    //get current value based on key
                    switch(preferenceKey)
                    {
                        case Settings.PreferenceName.ColorTheme:
                            currentValue = Settings.getColorTheme(context);

                            preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
                            {
                                @Override
                                public boolean onPreferenceChange(Preference preference, Object newValue)
                                {
                                    //allow if changing
                                    return((int)newValue != Settings.getColorTheme(context));
                                }
                            });
                            break;

                        case Settings.PreferenceName.LensIndicator:
                            currentValue = Settings.getIndicator(context);
                            break;

                        case Settings.PreferenceName.LensUpdateDelay:
                            currentValue = Settings.getLensUpdateDelay(context);
                            break;

                        case Settings.PreferenceName.ListUpdateDelay:
                            currentValue = Settings.getListUpdateDelay(context);
                            break;

                        case Settings.PreferenceName.MapLayerType + Settings.SubPreferenceName.Globe:
                            forGlobe = true;
                            //fall through

                        case Settings.PreferenceName.MapLayerType + Settings.SubPreferenceName.Map:
                            currentValue = Settings.getMapLayerType(context, forGlobe);

                            preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
                            {
                                @Override
                                public boolean onPreferenceChange(Preference preference, Object newValue)
                                {
                                    int layerType = (int)newValue;

                                    //if child preference exists
                                    if(childPreference != null)
                                    {
                                        //update child visibility
                                        childPreference.setVisible(layerType == CoordinatesFragment.MapLayerType.Satellite || layerType == CoordinatesFragment.MapLayerType.Hybrid);
                                    }

                                    //allow change
                                    return(true);
                                }
                            });
                            break;

                        case Settings.PreferenceName.MapMarkerInfoLocation:
                            currentValue = Settings.getMapMarkerInfoLocation(context);
                            break;

                        case Settings.PreferenceName.SatelliteSource:
                            valueIndex = Settings.getSatelliteSource(context);

                            preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
                            {
                                @Override
                                public boolean onPreferenceChange(Preference preference, Object newValue)
                                {
                                    byte source = (byte)newValue;
                                    SwitchPreference childSwitch = (SwitchPreference)childPreference;
                                    boolean sourceUseGP = !Settings.getSatelliteSourceUseGP(context, source);

                                    //update child switch
                                    childSwitch.setChecked(sourceUseGP);
                                    childSwitch.setEnabled(source != Database.UpdateSource.N2YO);

                                    //if changing source
                                    if(source != Settings.getSatelliteSource(context))
                                    {
                                        //clear master satellites
                                        Database.clearMasterSatelliteTable(context);
                                    }

                                    //allow change
                                    return(true);
                                }
                            });
                            break;

                        case Settings.PreferenceName.AltitudeSource:
                            valueIndex = (Settings.getAltitudeSource(context) == LocationService.OnlineSource.MapQuest ? 0 : 1);
                            break;

                        case Settings.PreferenceName.TimeZoneSource:
                            valueIndex = (Settings.getTimeZoneSource(context) == LocationService.OnlineSource.GeoNames ? 0 : 1);
                            break;

                        case Settings.PreferenceName.InformationSource:
                            valueIndex = Settings.getInformationSource(context);
                            valueIndex = (valueIndex == Database.UpdateSource.NASA ? 0 : valueIndex == Database.UpdateSource.Celestrak ? 1 : 2);
                            break;
                    }
                    if(valueIndex >= 0 && valueIndex < values.length)
                    {
                        currentValue = values[valueIndex];
                    }

                    //set adapter and state
                    if(items instanceof IconSpinner.Item[])
                    {
                        preference.setAdapter(context, (IconSpinner.Item[])items);
                    }
                    else
                    {
                        preference.setAdapter(context, items, values, itemImageIds, subTexts);
                    }
                    preference.setSelectedValue(currentValue);
                }
                else
                {
                    //hide preference
                    preference.setVisible(false);
                }
            }
        }

        //Sets up up a preference switch
        private static void setupSwitch(SwitchPreference preference, TimeIntervalPreference timePreference)
        {
            //if preference exists
            if(preference != null)
            {
                final Context context = preference.getContext();
                final SharedPreferences.Editor writeSettings = Settings.getWriteSettings(context);
                final String preferenceKey = preference.getKey();
                final boolean isGPDataUsage = preferenceKey.equals(Settings.PreferenceName.SatelliteSourceUseGP);
                final Object dependency = Settings.getSatelliteSource(context);
                final boolean checked = Settings.getPreferenceBoolean(context, preferenceKey + (isGPDataUsage ? dependency : ""), (isGPDataUsage ? dependency : null));

                //if preference should be visible
                if(preferenceVisible(preferenceKey))
                {
                    //set state and listener
                    preference.setIconSpaceReserved(false);
                    preference.setChecked(checked);
                    preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
                    {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue)
                        {
                            boolean checked = (Boolean)newValue;
                            boolean isAutoUpdate = (timePreference != null);
                            String subKey = "";
                            UpdateService.AlarmUpdateSettings settings = (isAutoUpdate ? Settings.getAutoUpdateSettings(context, preferenceKey) : null);

                            //if -not for auto update- or -settings are changing-
                            if(!isAutoUpdate || settings.enabled != checked)
                            {
                                //if for GP data usage
                                if(isGPDataUsage)
                                {
                                    //get sub key
                                    subKey = String.valueOf(Settings.getSatelliteSource(context));

                                    //invert value
                                    checked = !checked;
                                }

                                //save preference
                                writeSettings.putBoolean(preferenceKey + subKey, checked).apply();

                                //if auto update
                                if(isAutoUpdate)
                                {
                                    //apply changes
                                    Settings.setAutoUpdate(context, preferenceKey);
                                }
                            }

                            //if time preference is set
                            if(timePreference != null)
                            {
                                //update visibility
                                timePreference.setVisible(checked);
                            }

                            //allow change
                            return(true);
                        }
                    });

                    //if might need changes
                    if(timePreference != null)
                    {
                        //call once to update changes
                        preference.getOnPreferenceChangeListener().onPreferenceChange(preference, checked);
                    }
                }
                else
                {
                    //hide preference
                    preference.setVisible(false);
                }
            }
        }

        //Sets up a slider preference
        private static void setupSlider(SliderPreference preference)
        {
            //if preference exists
            if(preference != null)
            {
                final String preferenceKey = preference.getKey();
                int min = -1;
                int max = -1;

                switch(preferenceKey)
                {
                    case Settings.PreferenceName.MapSensitivityScale + Settings.SubPreferenceName.Globe:
                    case Settings.PreferenceName.MapSensitivityScale + Settings.SubPreferenceName.Map:
                        min = Settings.SensitivityScaleMin;
                        max = Settings.SensitivityScaleMax;
                        break;

                    case Settings.PreferenceName.MapSpeedScale + Settings.SubPreferenceName.Globe:
                    case Settings.PreferenceName.MapSpeedScale + Settings.SubPreferenceName.Map:
                        min = Settings.SpeedScaleMin;
                        max = Settings.SpeedScaleMax;
                        break;

                    case Settings.PreferenceName.MapMarkerScale:
                        min = Settings.IconScaleMin;
                        max = Settings.IconScaleMax;
                        break;
                }

                //if range is valid
                if(min >= 0)
                {
                    //set range
                    preference.setRange(min, max);
                }
            }
        }

        //Sets up a switch button preference
        private static void setupSwitchButton(SwitchButtonPreference preference)
        {
            //if preference exists
            if(preference != null)
            {
                final Context context = preference.getContext();
                final int titleId;
                final boolean allowOpacity;
                final SharedPreferences.Editor writeSettings = Settings.getWriteSettings(context);
                final String preferenceKey = preference.getKey();
                final String buttonPreferenceKey;

                //if lens horizon or show grid toggle
                switch(preferenceKey)
                {
                    case Settings.PreferenceName.LensUseHorizon:
                    case Settings.PreferenceName.MapShowGrid:
                        BorderButton switchButton = new BorderButton(new ContextThemeWrapper(context, R.style.ColorButton), null);
                        float[] size = Globals.dpsToPixels(context, 60, 40);
                        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams((int)size[0], (int)size[1]);

                        //get specific settings
                        if(preferenceKey.equals(Settings.PreferenceName.LensUseHorizon))
                        {
                            titleId = R.string.title_horizon_color;
                            buttonPreferenceKey = Settings.PreferenceName.LensHorizonColor;
                            allowOpacity = false;
                        }
                        else
                        {
                            titleId = R.string.title_grid_color;
                            buttonPreferenceKey = Settings.PreferenceName.MapGridColor;
                            allowOpacity = true;
                        }

                        //setup button
                        switchButton.setBackgroundColor(Settings.getPreferenceInt(context, buttonPreferenceKey));
                        switchButton.setLayoutParams(params);
                        preference.setButton(switchButton);
                        preference.setButtonOnClickListener(createOnColorButtonClickListener(context, buttonPreferenceKey, titleId, allowOpacity, writeSettings));
                        break;
                }
            }
        }

        //Sets up a switch text preference
        private static void setupSwitchText(SwitchTextPreference preference)
        {
            //if preference exists
            if(preference != null)
            {
                final Context context = preference.getContext();
                final String preferenceKey = preference.getKey();
                float enabledValue;
                float disabledValue;

                switch(preferenceKey)
                {
                    case Settings.PreferenceName.LensWidth:
                    case Settings.PreferenceName.LensHeight:
                        boolean isWidth = preferenceKey.equals(Settings.PreferenceName.LensWidth);

                        //get lens width and height
                        try
                        {
                            Camera.Parameters cameraParams;
                            cameraParams = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK).getParameters();
                            enabledValue = (isWidth ? cameraParams.getHorizontalViewAngle() : cameraParams.getVerticalViewAngle());
                        }
                        catch(Exception ex)
                        {
                            enabledValue = 45;
                        }
                        disabledValue = (isWidth ? Settings.getLensWidth(context) : Settings.getLensHeight(context));

                        //set values
                        preference.setValueText(String.valueOf(enabledValue), String.valueOf(disabledValue));
                        break;

                    case Settings.PreferenceName.LensAzimuthUserOffset:
                        //set value
                        preference.setValueText(String.valueOf(Settings.getLensAzimuthUserOffset(context)));
                        break;
                }
            }
        }

        //Sets up a time interval
        private static void setupTimeInterval(TimeIntervalPreference preference)
        {
            //if preference exists
            if(preference != null)
            {
                final Context context = preference.getContext();
                final String preferenceKey = preference.getKey();

                //set listener
                preference.setOnPreferenceChangedListener(new TimeIntervalPreference.OnPreferenceChangedListener()
                {
                    @Override
                    public void onPreferenceChanged(TimeIntervalPreference preference, Object newValue)
                    {
                        TimeIntervalPreference.IntervalValues values = (TimeIntervalPreference.IntervalValues)newValue;
                        UpdateService.AlarmUpdateSettings settings = Settings.getAutoUpdateSettings(context, preferenceKey);

                        //if settings changed
                        if(values.hour != settings.hour || values.minute != settings.minute || values.intervalMs != settings.rate)
                        {
                            //apply changes
                            Settings.setAutoUpdate(context, preferenceKey);
                        }
                    }
                });
            }
        }

        //Setups a category
        private static void setupCategory(PreferenceCategory category)
        {
            //if category exists
            if(category != null)
            {
                //set visibility
                category.setVisible(!showSetup);
            }
        }
    }

    public static class SetupPage extends Selectable.ListFragment
    {
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
        {
            int page = this.getPageParam();
            boolean onWelcome = (page == SetupPageType.Welcome);
            ViewGroup rootView = (ViewGroup)inflater.inflate(R.layout.setup_view, container, false);

            //handle display based on page
            switch(page)
            {
                case SetupPageType.Welcome:
                case SetupPageType.Finished:
                    rootView.findViewById(R.id.Setup_Text_Layout).setVisibility(View.VISIBLE);
                    ((TextView)rootView.findViewById(R.id.Setup_Text_Title)).setText(onWelcome ? R.string.title_welcome : R.string.title_finished);
                    ((TextView)rootView.findViewById(R.id.Setup_Text)).setText(onWelcome ? R.string.desc_quick_setup : R.string.desc_finished_setup);
                    break;
            }

            //return view
            return(rootView);
        }

        @Override
        protected boolean setupActionModeItems(MenuItem edit, MenuItem delete, MenuItem save, MenuItem sync)
        {
            return(false);
        }

        @Override
        protected void onActionModeEdit() {}

        @Override
        protected void onActionModeDelete() {}

        @Override
        protected int onActionModeConfirmDelete()
        {
            return(0);
        }

        @Override
        protected void onActionModeSave() {}

        @Override
        protected void onActionModeSync() {}

        @Override
        protected void onUpdateStarted() {}

        @Override
        protected void onUpdateFinished(boolean success) {}
    }

    private class SetupPageAdapter extends Selectable.ListFragmentAdapter
    {
        public SetupPageAdapter(FragmentManager fm, View parentView)
        {
            super(fm, parentView, null, null, null, null, null, null, null, -1, null);
        }

        @Override
        public @NonNull Fragment getItem(int position)
        {
            Bundle params;
            Context context = SettingsActivity.this;
            Fragment page;

            //get page for position
            switch(position)
            {
                case SetupPageType.Location:
                    page = getSettingsFragment(ScreenKey.Locations, context.getString(R.string.title_locations));
                    params = page.getArguments();
                    break;

                case SetupPageType.Display:
                case SetupPageType.Updates:
                    page = new SettingsSubFragment();
                    params = new Bundle();
                    params.putBoolean(EXTRA_SHOW_SETUP, true);
                    params.putString(RootKey, (position == SetupPageType.Updates ? ScreenKey.Updates : ScreenKey.Display));
                    break;

                case SetupPageType.Satellites:
                    page = getOrbitalsFragment();
                    params = page.getArguments();
                    break;

                default:
                case SetupPageType.Welcome:
                case SetupPageType.Finished:
                    page = this.getItem(-1, position, -1, new SetupPage());
                    params = page.getArguments();
                    break;
            }

            //add SetupPageType to params
            if(params == null)
            {
                params = new Bundle();
            }
            params.putInt(SetupPageParam, position);
            page.setArguments(params);

            //return page
            return(page);
        }

        @Override
        public int getCount()
        {
            return(SetupPageType.PageCount);
        }
    }

    public static final String EXTRA_RECREATE = "recreate";
    public static final String EXTRA_RECREATE_LENS = "recreateLens";
    public static final String EXTRA_RECREATE_MAP = "recreateMap";
    public static final String EXTRA_RELOAD_LOCATION = "reloadLocation";
    public static final String EXTRA_UPDATE_COMBINED_LAYOUT = "updateCombinedLayout";
    public static final String EXTRA_START_SCREEN = "startScreen";
    public static final String EXTRA_SHOW_SETUP = "showSetup";
    private static final String RootKey = "rootKey";
    private static final String SetupPageParam = "setupPageParam";

    private static boolean inEditMode = false;
    private static Intent resultIntent = null;

    private byte locationSource;
    private int currentPage;
    private boolean isLoading;
    private boolean showSetup;
    private boolean updateNow;
    private boolean needSaveCurrentLocation;
    private String currentPageKey;
    private TextView infoText;
    private CheckBox inputCheckBox;
    private ViewGroup settingsLayout;
    private LinearLayout progressLayout;
    private FragmentManager manager;
    private SwipeStateViewPager setupPager;
    private MaterialButton backButton;
    private MaterialButton nextButton;
    private FloatingActionButton floatingButton;
    private CircularProgressIndicator loadingBar;
    private AlertDialog addCurrentLocationDialog;
    private LocationReceiver locationReceiver;
    private UpdateReceiver localUpdateReceiver;
    private Orbitals.PageAdapter orbitalsPageAdapter;
    private Settings.PageAdapter settingsPageAdapter;
    private Settings.Locations.ItemListAdapter settingsLocationsListAdapter;
    private Settings.Options.Accounts.ItemListAdapter accountsListAdapter;
    private SharedPreferences preferences;
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;
    private Selectable.ListFragment.OnInformationChangedListener informationChangedListener;

    public static class SettingsMainFragment extends PreferenceFragmentCompat
    {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey)
        {
            setPreferencesFromResource(R.xml.settings_main, null);
            setIconTint(this.getPreferenceScreen(), Globals.resolveColorID(this.getContext(), R.attr.defaultTextColor));
        }

        //Set icon tint for given preference
        private void setIconTint(Preference preference, int tintColor)
        {
            int index;

            //if a preference category
            if(preference instanceof PreferenceCategory)
            {
                //remember current category
                PreferenceCategory currentCategory = (PreferenceCategory)preference;

                //go through each preference in category
                for(index = 0; index < currentCategory.getPreferenceCount(); index++)
                {
                    //set preference icon tint
                    setIconTint(currentCategory.getPreference(index), tintColor);
                }
            }
            else
            {
                //if able to get current icon
                Drawable currentIcon = preference.getIcon();
                if(currentIcon != null)
                {
                    //set icon tint
                    DrawableCompat.setTint(currentIcon, tintColor);
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        Settings.Options.Display.setTheme(this);
        super.onCreate(savedInstanceState);

        int titleId;
        String startScreenKey;
        Bundle args = new Bundle();
        Intent startIntent = this.getIntent();
        ActionBar mainActionBar;
        Fragment startFragment;
        List<Fragment> previousPages;

        //get start intent and values
        if(startIntent == null)
        {
            startIntent = new Intent();
        }
        showSetup = startIntent.getBooleanExtra(EXTRA_SHOW_SETUP, false);
        startScreenKey = startIntent.getStringExtra(EXTRA_START_SCREEN);

        //if result intent does not exist yet
        if(resultIntent == null)
        {
            //create intent
            resultIntent = new Intent();
        }

        //set defaults
        currentPage = -1;
        currentPageKey = startScreenKey;
        isLoading = updateNow = false;
        locationSource = Database.LocationType.Current;
        orbitalsPageAdapter = null;
        settingsPageAdapter = null;
        settingsLocationsListAdapter = null;
        accountsListAdapter = null;
        informationChangedListener = null;

        //set view
        this.setContentView(showSetup ? R.layout.setup_layout : R.layout.settings_layout);

        //setup displays
        settingsLayout = this.findViewById(showSetup ? R.id.Setup_Layout : R.id.Settings_Layout);
        progressLayout = (showSetup ? this.findViewById(R.id.Setup_Progress_Layout) : null);
        floatingButton = this.findViewById(showSetup ? R.id.Setup_Floating_Button : R.id.Settings_Layout_Floating_Button);
        floatingButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                handleFloatingButtonClick(false);
            }
        });
        floatingButton.setOnLongClickListener(new View.OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View view)
            {
                handleFloatingButtonClick(true);
                return(true);
            }
        });
        backButton = this.findViewById(showSetup ? R.id.Setup_Back_Button : R.id.Settings_Layout_Back_Button);
        backButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //if showing setup
                if(showSetup)
                {
                    //go back a page
                    moveSetupPage(false);
                }
                else
                {
                    //if on a sub page
                    if(manager.getBackStackEntryCount() > 0)
                    {
                        //go back a page
                        manager.popBackStack();
                    }
                    else
                    {
                        //done
                        SettingsActivity.this.finish();
                    }
                }
            }
        });
        nextButton = (showSetup ? this.findViewById(R.id.Setup_Next_Button) : null);
        if(nextButton != null)
        {
            nextButton.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    //go forward a page
                    moveSetupPage(true);
                }
            });
        }
        infoText = (showSetup ? this.findViewById(R.id.Setup_Info_Text) : null);
        loadingBar = (showSetup ? this.findViewById(R.id.Setup_Loading_Bar) : null);
        inputCheckBox = (showSetup ? this.findViewById(R.id.Setup_Input_CheckBox) : null);
        if(inputCheckBox != null)
        {
            inputCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
            {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
                {
                    int page = setupPager.getCurrentItem();

                    //if on satellites
                    if(page == SetupPageType.Satellites)
                    {
                        //update if updating and allow swiping if not
                        updateNow = isChecked;
                        setupPager.setSwipeEnabled(!updateNow);
                    }
                }
            });
        }
        setupPager = (showSetup ? this.findViewById(R.id.Setup_Pager) : null);

        //get fragment manager
        manager = this.getSupportFragmentManager();

        //if there were previous pages
        previousPages = manager.getFragments();
        for(Fragment currentFragment : previousPages)
        {
            //if a Selectable.ListFragment
            if(currentFragment instanceof Selectable.ListFragment)
            {
                //update with current parent view
                ((Selectable.ListFragment)currentFragment).setParentView(settingsLayout);
            }
        }

        //if not showing setup
        if(!showSetup)
        {
            //setup starting fragment
            switch(startScreenKey)
            {
                case ScreenKey.Accounts:
                case ScreenKey.Locations:
                case ScreenKey.Notifications:
                case ScreenKey.Widgets:
                    switch(startScreenKey)
                    {
                        case ScreenKey.Accounts:
                            titleId = R.string.title_accounts;
                            break;

                        case ScreenKey.Locations:
                            titleId = R.string.title_locations;
                            break;

                        case ScreenKey.Notifications:
                            titleId = R.string.title_notifications;
                            break;

                        default:
                        case ScreenKey.Widgets:
                            titleId = R.string.title_widgets;
                            break;
                    }

                    startFragment = getSettingsFragment(startScreenKey, this.getString(titleId));
                    break;

                case ScreenKey.Display:
                case ScreenKey.LensView:
                case ScreenKey.ListView:
                case ScreenKey.MapView:
                case ScreenKey.Updates:
                    startFragment = new SettingsSubFragment();

                    args.putString(RootKey, startScreenKey);
                    startFragment.setArguments(args);
                    break;

                default:
                    startFragment = new SettingsMainFragment();
                    currentPageKey = null;
                    break;
            }

            //setup fragment manager
            manager.beginTransaction().replace(R.id.Settings_Layout_Fragment, startFragment, startScreenKey).commit();
            manager.addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener()
            {
                @Override
                public void onBackStackChanged()
                {
                    if(manager.getBackStackEntryCount() == 0)
                    {
                        currentPage = -1;
                    }
                    updateFloatingButton();
                    updateBackButton();
                }
            });
        }

        //setup dialog
        addCurrentLocationDialog = Globals.createAddCurrentLocationDialog(this, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                //cancel
                needSaveCurrentLocation = false;
                lockScreenOrientation(false);
            }
        }, new DialogInterface.OnCancelListener()
        {
            @Override
            public void onCancel(DialogInterface dialog)
            {
                //cancel
                needSaveCurrentLocation = false;
                lockScreenOrientation(false);
            }
        });

        //if using setup pager
        if(setupPager != null)
        {
            //setup pager
            setupPager.setAdapter(new SetupPageAdapter(manager, settingsLayout));
            setupPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener()
            {
                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

                @Override
                public void onPageSelected(int position)
                {
                    //update status
                    switch(position)
                    {
                        case SetupPageType.Location:
                            currentPage = Settings.PageType.Locations;
                            break;

                        case SetupPageType.Updates:
                            currentPage = Settings.PageType.Updates;
                            break;

                        default:
                            currentPage = -1;
                            break;
                    }
                    updateProgress();
                    updateFloatingButton();
                }

                @Override
                public void onPageScrollStateChanged(int state) {}
            });
        }

        //set receivers/listeners
        locationReceiver = createLocationReceiver();
        locationReceiver.register(this);
        localUpdateReceiver = createUpdateReceiver();
        localUpdateReceiver.register(this);
        preferences = Settings.getReadSettings(this);
        preferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener()
        {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
            {
                boolean recreateThis = false;

                //if key ends with sub preference Globe or Map
                if(key.endsWith(Settings.SubPreferenceName.Globe) || key.endsWith(Settings.SubPreferenceName.Map))
                {
                    //map needs recreate
                    setMapRecreateNeeded();
                }
                else
                {
                    switch(key)
                    {
                        case Settings.PreferenceName.ColorTheme:
                        case Settings.PreferenceName.DarkTheme:
                            recreateThis = true;
                            //fall through

                        case Settings.PreferenceName.ListShowPassProgress:
                        case Settings.PreferenceName.MetricUnits:
                            //recreate activity
                            setRecreateNeeded();

                            //if need to recreate this activity
                            if(recreateThis)
                            {
                                //update theme and recreate
                                Settings.Options.Display.setTheme(SettingsActivity.this);
                                SettingsActivity.this.recreate();
                            }
                            break;

                        case Settings.PreferenceName.LensIndicator:
                        case Settings.PreferenceName.LensHorizonColor:
                        case Settings.PreferenceName.LensUseHorizon:
                        case Settings.PreferenceName.LensUseCamera:
                        case Settings.PreferenceName.LensRotate:
                        case Settings.PreferenceName.LensUseAutoWidth:
                        case Settings.PreferenceName.LensUseAutoHeight:
                        case Settings.PreferenceName.LensAzimuthUserOffset:
                        case Settings.PreferenceName.LensWidth:
                        case Settings.PreferenceName.LensHeight:
                            //lens needs recreate
                            setLensRecreateNeed();
                            break;

                        case Settings.PreferenceName.MapShow3dPaths:
                        case Settings.PreferenceName.MapRotateAllowed:
                        case Settings.PreferenceName.MapMarkerShowBackground:
                        case Settings.PreferenceName.MapShowSearchList:
                        case Settings.PreferenceName.MapShowZoom:
                        case Settings.PreferenceName.MapShowLabelsAlways:
                        case Settings.PreferenceName.MapShowOrbitalDirection:
                        case Settings.PreferenceName.MapMarkerShowShadow:
                        case Settings.PreferenceName.MapShowStars:
                        case Settings.PreferenceName.MapShowGrid:
                        case Settings.PreferenceName.MapGridColor:
                        case Settings.PreferenceName.MapMarkerScale:
                        case Settings.PreferenceName.MapMarkerInfoLocation:
                            //map needs recreate
                            setMapRecreateNeeded();
                            break;

                        case Settings.PreferenceName.UseCombinedCurrentLayout:
                            //combined layout needed update
                            setUpdateCombinedLayoutNeeded();
                            break;
                    }
                }
            }
        };
        preferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener);

        //if able to get action bar
        mainActionBar = getSupportActionBar();
        if(mainActionBar != null)
        {
            //if showing setup
            if(showSetup)
            {
                //set background
                mainActionBar.setBackgroundDrawable(new ColorDrawable(Globals.resolveColorID(this, R.attr.actionBarBackground)));
            }
            else
            {
                //hide it
                mainActionBar.hide();
            }
        }

        //update display
        updateBackButton();
        updateFloatingButton();

        //if showing setup
        if(showSetup)
        {
            //set page
            setupPager.setCurrentItem(SetupPageType.Welcome);
            updateProgress();
        }
    }

    @Override
    public void onBackPressed()
    {
        int page;

        //if showing setup
        if(showSetup)
        {
            //if not loading
            if(!isLoading)
            {
                //if after first page
                page = (setupPager != null ? setupPager.getCurrentItem() : -1);
                if(page >= 1)
                {
                    //go back 1 page
                    setupPager.setCurrentItem(page - 1);
                }
                else
                {
                    //call base
                    super.onBackPressed();
                }
            }
        }
        else
        {
            //call base
            super.onBackPressed();
        }
    }

    @Override
    public void finish()
    {
        //set result and reset
        setResult(RESULT_OK, resultIntent);
        resultIntent = null;

        super.finish();
    }

    public void cancel()
    {
        //set result and reset
        setResult(RESULT_CANCELED, resultIntent);
        resultIntent = null;

        super.finish();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        //remove receivers
        locationReceiver.unregister(this);
        localUpdateReceiver.unregister(this);

        //stop listener
        if(preferences != null && preferenceChangeListener != null)
        {
            preferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        boolean isOkay = (resultCode == RESULT_OK);
        int id;
        int progressType = Globals.ProgressType.Unknown;
        Resources res = this.getResources();

        //if no data
        if(data == null)
        {
            //set to empty
            data = new Intent();
        }

        //handle response
        switch(requestCode)
        {
            case BaseInputActivity.RequestCode.MasterAddList:
                //if able to get progress type
                progressType = BaseInputActivity.handleActivityMasterAddListResult(res, settingsLayout, data);
                if(progressType == Globals.ProgressType.Unknown)
                {
                    //stop
                    break;
                }
                //else fall through

            case BaseInputActivity.RequestCode.ManualOrbitalInput:
                //if set and weren't denied
                if(isOkay && progressType != Globals.ProgressType.Denied)
                {
                    //update list
                    updateList();
                }
                break;

            case BaseInputActivity.RequestCode.SDCardOpenItem:
                //if set
                if(isOkay)
                {
                    //handle SD card open files request
                    BaseInputActivity.handleActivitySDCardOpenFilesRequest(this, data);
                }
                break;

            case BaseInputActivity.RequestCode.MapLocationInput:
            case BaseInputActivity.RequestCode.EditNotify:
                //if set
                if(isOkay)
                {
                    //update list
                    updateList();
                }
                break;

            case BaseInputActivity.RequestCode.EditWidget:
                //if set
                if(isOkay)
                {
                    //if valid ID
                    id = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 0);
                    if(id != 0)
                    {
                        //update list
                        updateList();
                    }
                }
                break;

            case BaseInputActivity.RequestCode.GoogleDriveAddAccount:
            case BaseInputActivity.RequestCode.DropboxAddAccount:
                //if adapter is set
                if(accountsListAdapter != null)
                {
                    //reload items
                    accountsListAdapter.reloadItems();
                }
                break;

            case BaseInputActivity.RequestCode.GoogleDriveSignIn:
                //handle Google Drive open file browser request
                BaseInputActivity.handleActivityGoogleDriveOpenFileBrowserRequest(this, settingsLayout, data, isOkay);
                break;

            case BaseInputActivity.RequestCode.GoogleDriveOpenFile:
            case BaseInputActivity.RequestCode.DropboxOpenFile:
            case BaseInputActivity.RequestCode.OthersOpenItem:
                //if selected item
                if(isOkay)
                {
                    //handle open file request
                    BaseInputActivity.handleActivityOpenFileRequest(this, settingsLayout, data, requestCode);
                }
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        boolean granted = (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED);
        boolean retrying = false;

        //check if retrying
        switch(requestCode)
        {
            case Globals.PermissionType.LocationRetry:
            case Globals.PermissionType.ReadExternalStorageRetry:
                retrying = true;
                break;
        }

        //handle response
        switch(requestCode)
        {
            case Globals.PermissionType.Location:
            case Globals.PermissionType.LocationRetry:
                //if granted
                if(granted)
                {
                    //try to start again
                    LocationService.start(this, LocationService.FLAG_START_GET_LOCATION);
                }
                //else if not retrying
                else if(!retrying)
                {
                    //ask permission again
                    Globals.askLocationPermission(this, true);
                }
                break;

            case Globals.PermissionType.ReadExternalStorage:
            case Globals.PermissionType.ReadExternalStorageRetry:
                //if granted
                if(granted)
                {
                    Orbitals.showSDCardFileBrowser(this, settingsLayout);
                }
                //else if not retrying
                else if(!retrying)
                {
                    //ask permission again
                    Globals.askReadPermission(this, true);
                }
                break;
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public boolean isFinishing()
    {
        this.setResult(BaseInputActivity.RequestCode.Settings, resultIntent);
        return(super.isFinishing());
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref)
    {
        Bundle args = pref.getExtras();
        String fragmentClass = pref.getFragment();
        boolean isPage = fragmentClass.equals(Settings.Page.class.getName());
        Fragment fragment;

        //update current page key
        currentPageKey = pref.getKey();

        //if a page
        if(isPage)
        {
            //create settings fragment
            fragment = getSettingsFragment(currentPageKey, pref.getTitle().toString());
        }
        else
        {
            //update current page
            currentPage = -1;

            //create preference fragment
            fragment = manager.getFragmentFactory().instantiate(getClassLoader(), fragmentClass);

            //set arguments
            args.putString(RootKey, currentPageKey);
            fragment.setArguments(args);
        }

        //add fragment
        fragment.setTargetFragment(caller, 0);
        manager.beginTransaction().replace(R.id.Settings_Layout_Fragment, fragment, (isPage ? currentPageKey : null)).addToBackStack(null).commit();

        return(true);
    }

    //Gets orbital fragment with given title
    private Fragment getOrbitalsFragment()
    {
        //if adapter not set yet
        if(orbitalsPageAdapter == null)
        {
            //create adapter
            orbitalsPageAdapter = new Orbitals.PageAdapter(manager, settingsLayout, new Selectable.ListFragment.OnItemDetailButtonClickListener()
            {
                @Override
                public void onClick(int group, int pageType, int itemID, Selectable.ListItem item, int buttonNum)
                {
                    //if info button
                    if(buttonNum == Selectable.ListBaseAdapter.DetailButtonType.Info)
                    {
                        //get information
                        UpdateService.getInformation(SettingsActivity.this, group, new Database.SatelliteData(Database.getOrbital(SettingsActivity.this, itemID)).satellite.tle);
                    }
                }
            }, createOnSetAdapterListener(), null, null);
        }

        //return page fragment
        return(orbitalsPageAdapter.getItem(MainActivity.Groups.Orbitals, Orbitals.PageType.Satellites, -1, new Orbitals.Page(this.getString(R.string.title_satellites), true)));
    }

    //Gets fragment for settings page with given screen key and title
    private Fragment getSettingsFragment(String screenKey, String title)
    {
        //update current page
        switch(screenKey)
        {
            case ScreenKey.Accounts:
                currentPage = Settings.PageType.Accounts;
                break;

            case ScreenKey.Locations:
                currentPage = Settings.PageType.Locations;
                break;

            case ScreenKey.Notifications:
                currentPage = Settings.PageType.Notifications;
                break;

            case ScreenKey.Widgets:
                currentPage = Settings.PageType.Widgets;
                break;
        }

        //if adapter not set yet
        if(settingsPageAdapter == null)
        {
            //create adapter
            settingsPageAdapter = new Settings.PageAdapter(manager, settingsLayout, null, createOnItemCheckChangedListener(), createOnSetAdapterListener(), createOnUpdateNeededListener(), null, createOnPageResumeListener(), new int[Settings.PageType.PageCount]);
        }

        //return page fragment
        return(settingsPageAdapter.getItem(MainActivity.Groups.Settings, currentPage, 0, new Settings.Page(title)));
    }

    //Handles main floating button click
    private void handleFloatingButtonClick(boolean isLong)
    {
        //if showing setup and on SetupPageType.Satellites
        if(showSetup && setupPager.getCurrentItem() == SetupPageType.Satellites)
        {
            //show add satellite dialog
            Orbitals.showAddDialog(this, settingsLayout, isLong);
            return;
        }

        switch(currentPage)
        {
            case Settings.PageType.Accounts:
                if(inEditMode)
                {
                    //get selected items
                    ArrayList<Selectable.ListItem> selectedItems = getSettingsPage(currentPageKey).getSelectedItems();

                    //go through selected items
                    for(Selectable.ListItem currentItem : selectedItems)
                    {
                        //edit account
                        accountsListAdapter.editAccount(currentItem.id);
                    }
                }
                else
                {
                    //show add account dialog
                    showAddAccountDialog(accountsListAdapter.getUsedAccounts());
                }
                break;

            case Settings.PageType.Locations:
                if(inEditMode)
                {
                    showEditLocationDialog();
                }
                else
                {
                    showAddLocationDialog();
                }
                break;

            case Settings.PageType.Notifications:
                Database.DatabaseLocation currentLocation = Database.getSelectedLocation(this);
                NotifySettingsActivity.show(this, new Calculations.ObserverType(currentLocation.zoneId, new Calculations.GeodeticDataType(currentLocation.latitude, currentLocation.longitude, currentLocation.altitudeKM, 0, 0)));
                break;
        }
    }

    //Creates a location receiver
    private LocationReceiver createLocationReceiver()
    {
        return(new LocationReceiver(LocationService.FLAG_START_GET_LOCATION)
        {
            @Override
            protected Activity getActivity()
            {
                return(SettingsActivity.this);
            }

            @Override
            protected View getParentView()
            {
                return(settingsLayout);
            }

            @Override
            protected void onDenied(Context context)
            {
                //dismiss dialog
                showAddCurrentLocationDialog(false);
            }

            @Override
            protected void onRestart(Context context, boolean close, boolean checkClose)
            {
                //if using current location
                if(locationSource == Database.LocationType.Current)
                {
                    //continue with restart
                    super.onRestart(context, close, checkClose);
                }
            }

            @Override
            protected void onGotLocation(Context context, Calculations.ObserverType observer)
            {
                //if need to save current location
                if(needSaveCurrentLocation)
                {
                    //save location after resolving it
                    saveResolvedLocation(context, observer);
                }
                //else if still using current location
                else if(locationSource == Database.LocationType.Current)
                {
                    //show status
                    Globals.showSnackBar(settingsLayout, context.getString(R.string.text_location_success));
                }

                //if settings location list adapter exists
                if(settingsLocationsListAdapter != null)
                {
                    //update current location
                    settingsLocationsListAdapter.setCurrentLocation(observer.geo.latitude, observer.geo.longitude, observer.geo.altitudeKm / 1000);
                    settingsLocationsListAdapter.notifyDataSetChanged();
                }
            }

            @Override
            protected void onSaveLocation(boolean success)
            {
                //dismiss dialog
                showAddCurrentLocationDialog(false);

                //if success
                if(success)
                {
                    //update display
                    updateList();
                }
            }
        });
    }

    //Creates an update receiver
    private UpdateReceiver createUpdateReceiver()
    {
        Activity activity = SettingsActivity.this;

        //create receiver
        return(new UpdateReceiver()
        {
            @Override
            protected View getParentView()
            {
                return(settingsLayout);
            }

            @Override
            protected void onLoadPending(ArrayList<Database.DatabaseSatellite> pendingLoadSatellites)
            {
                //show dialog to load pending
                Orbitals.showLoadDialog(activity, setupPager, (Selectable.ListFragmentAdapter) setupPager.getAdapter(), pendingLoadSatellites, new EditValuesDialog.OnDismissListener()
                {
                    @Override
                    public void onDismiss(EditValuesDialog dialog, int saveCount)
                    {
                        //if some were saved
                        if(saveCount > 0)
                        {
                            //update list
                            updateList();
                        }
                    }
                });
            }

            @Override
            protected void onGotInformation(Spanned infoText, int index)
            {
                //if listener is set
                if(informationChangedListener != null)
                {
                    //update display
                    informationChangedListener.informationChanged(infoText);
                }
            }

            @Override
            protected void onGeneralUpdate(int progressType, byte updateType, boolean ended)
            {
                //if button and pager exist and on satellites
                if(floatingButton != null && setupPager != null && setupPager.getCurrentItem() == SetupPageType.Satellites)
                {
                    //update visibility
                    Globals.setVisible(floatingButton, ended);
                }

                //if ended
                if(ended)
                {
                    updateList();
                }

                //handle based on update type
                switch(updateType)
                {
                    case UpdateService.UpdateType.UpdateSatellites:
                        //handle based on progress type
                        switch(progressType)
                        {
                            case Globals.ProgressType.Denied:
                                //show login
                                Globals.showAccountLogin(activity, Globals.AccountType.SpaceTrack, updateType, new Globals.WebPageListener()
                                {
                                    @Override
                                    public void onResult(Globals.WebPageData pageData, boolean success)
                                    {
                                        //if success or attempted to login
                                        if(success || pageData != null)
                                        {
                                            //try again
                                            updateSatellites(false);
                                        }
                                        else
                                        {
                                            //allow inputs
                                            setLoading(false);
                                        }
                                    }
                                });
                                break;

                            case Globals.ProgressType.Finished:
                                //if pager exists
                                if(setupPager != null)
                                {
                                    //done with update
                                    updateNow = false;

                                    //go to next page
                                    setupPager.setCurrentItem(setupPager.getCurrentItem() + 1);
                                }
                                //fall through

                            default:
                                //allow inputs if ended
                                setLoading(!ended);
                                break;

                        }
                        break;
                }
            }
        });
    }

    //Creates an on item check changed listener
    private Selectable.ListFragment.OnItemCheckChangedListener createOnItemCheckChangedListener()
    {
        return(new Selectable.ListFragment.OnItemCheckChangedListener()
        {
            @Override
            public void itemCheckedChanged(int page, Selectable.ListItem item)
            {
                Context context = SettingsActivity.this;

                //if on location
                if(page == Settings.PageType.Locations)
                {
                    //remember current item and if current location
                    Settings.Locations.Item currentItem = (Settings.Locations.Item)item;

                    //if now checked
                    if(item.isChecked)
                    {
                        //update source
                        locationSource = currentItem.locationType;
                        Database.saveLocation(context, currentItem.name, currentItem.locationType, true);
                        setReloadLocationNeeded();

                        //if current location and not on setup
                        if(locationSource == Database.LocationType.Current && !showSetup)
                        {
                            //show status and update location
                            Globals.showSnackBar(settingsLayout, context.getResources().getString(R.string.title_location_getting));
                            LocationService.getCurrentLocation(context, false, true, LocationService.PowerTypes.HighPowerThenBalanced);
                        }
                    }
                }
            }
        });
    }

    //Creates an on set adapter listener
    private Selectable.ListFragment.OnAdapterSetListener createOnSetAdapterListener()
    {
        return(new Selectable.ListFragment.OnAdapterSetListener()
        {
            @Override
            public void setAdapter(Selectable.ListFragment fragment, final int group, final int position, Selectable.ListBaseAdapter adapter)
            {
                switch(group)
                {
                    case MainActivity.Groups.Settings:
                        switch(position)
                        {
                            case Settings.PageType.Accounts:
                                //set adapter
                                accountsListAdapter = (Settings.Options.Accounts.ItemListAdapter)adapter;
                                break;

                            case Settings.PageType.Locations:
                                //set adapter
                                settingsLocationsListAdapter = (Settings.Locations.ItemListAdapter)adapter;
                                break;
                        }
                        break;

                    case MainActivity.Groups.Orbitals:
                        //set listener
                        informationChangedListener = fragment.createOnInformationChangedListener(adapter);
                        break;
                }
            }
        });
    }

    //Creates an on update needed listener
    private Selectable.ListFragment.OnUpdateNeededListener createOnUpdateNeededListener()
    {
        return(new Selectable.ListFragment.OnUpdateNeededListener()
        {
            @Override
            public void updateNeeded()
            {
                updateList();
            }
        });
    }

    //Creates an on page resume listener
    private Selectable.ListFragment.OnPageResumeListener createOnPageResumeListener()
    {
        return(new Selectable.ListFragment.OnPageResumeListener()
        {
            @Override
            public void resumed(Selectable.ListFragment page)
            {
                //update listener
                page.setOnEditModeChangedListener(new Selectable.ListFragment.OnEditModeChangedListener()
                {
                    @Override
                    public void editModeChanged(boolean editMode)
                    {
                        inEditMode = editMode;
                        updateFloatingButton();
                    }
                });

                //if a settings page
                if(page instanceof Settings.Page)
                {
                    //get adapter
                    Selectable.ListBaseAdapter listAdapter = page.getAdapter();

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
        });
    }

    //Creates an on color button click listener
    private static View.OnClickListener createOnColorButtonClickListener(final Context context, final String preferenceKey, final int titleId, final boolean allowOpacity, SharedPreferences.Editor writeSettings)
    {
        return(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                ChooseColorDialog colorDialog = new ChooseColorDialog(context, Settings.getPreferenceInt(context, preferenceKey));
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

    //Gets the desired settings page
    private Settings.Page getSettingsPage(String fragmentName)
    {
        Fragment page = manager.findFragmentByTag(fragmentName);
        return(page != null ? (Settings.Page)page : new Settings.Page(null));
    }

    //Gets the desired fragment
    private Fragment getFragment(int setupPageType)
    {
        List<Fragment> pages = manager.getFragments();

        //go through all pages
        for(Fragment currentPage : pages)
        {
            Bundle args = currentPage.getArguments();

            //make sure arguments exist
            if(args == null)
            {
                args = new Bundle();
            }

            //if a match
            if(args.getInt(SetupPageParam, -1) == setupPageType)
            {
                //return page
                return(currentPage);
            }
        }

        //not found
        //note: Settings.Page chosen for convenience, but should be any Selectable.ListFragment
        return(new Settings.Page(null));
    }

    //Moves setup page forward/back
    private void moveSetupPage(boolean forward)
    {
        int page;

        //if adapter exists
        if(setupPager != null)
        {
            //get page
            page = setupPager.getCurrentItem();

            //handle based on page
            switch(page)
            {
                case SetupPageType.Welcome:
                    //if forward
                    if(forward)
                    {
                        //go forward
                        setupPager.setCurrentItem(page + 1);
                    }
                    else
                    {
                        //skip
                        cancel();
                    }
                    break;

                case SetupPageType.Finished:
                    //if forward
                    if(forward)
                    {
                        //finished
                        finish();
                    }
                    //else fall through

                default:
                    //if on satellites, forward, and need to update now
                    if(page == SetupPageType.Satellites && forward && updateNow)
                    {
                        updateSatellites(true);
                        break;
                    }
                    //else fall through

                    //go forward or back
                    setupPager.setCurrentItem(page + (forward ? 1 : -1));
                    break;
            }
        }
    }

    //Update loading status
    private void setLoading(boolean loading)
    {
        int page = (setupPager != null ? setupPager.getCurrentItem() : -1);
        boolean onSatellites = (page == SetupPageType.Satellites);

        //update status
        isLoading = loading;

        //update displays
        if(setupPager != null)
        {
            setupPager.setEnabled(!isLoading);
        }
        if(loadingBar != null)
        {
            loadingBar.setVisibility(isLoading && onSatellites ? View.VISIBLE : View.GONE);
        }
        if(inputCheckBox != null)
        {
            inputCheckBox.setVisibility(isLoading || !onSatellites ? View.GONE : View.VISIBLE);
        }
        if(backButton != null)
        {
            backButton.setEnabled(!isLoading);
        }
        if(nextButton != null)
        {
            nextButton.setEnabled(!isLoading);
        }
    }

    //Update satellites
    private void updateSatellites(boolean confirmInternet)
    {
        int index;
        boolean askInternet = (confirmInternet && Globals.shouldAskInternetConnection(this));
        Resources res = this.getResources();
        Database.DatabaseSatellite[] satellites;
        ArrayList<Database.DatabaseSatellite> satelliteList = new ArrayList<>(0);

        //if asking about internet
        if(askInternet)
        {
            //get confirmation
            Globals.showConfirmInternetDialog(this, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    //don't ask this time
                    updateSatellites(false);
                }
            });
        }
        else
        {
            //if not already updating satellites
            if(!UpdateService.updatingSatellites())
            {
                //get satellites
                satellites = Database.getOrbitals(this, Database.getSatelliteConditions());
                for(index = 0; index < satellites.length; index++)
                {
                    //add satellite to list
                    satelliteList.add(satellites[index]);
                }

                //update satellites
                UpdateService.updateSatellites(this, res.getQuantityString(R.plurals.title_satellites_updating, satelliteList.size()), satelliteList, false);
            }
            else
            {
                //hide notification
                UpdateService.setNotificationVisible(UpdateService.UpdateType.UpdateSatellites, false);
            }
        }
    }

    //Updates progress
    private void updateProgress()
    {
        int currentPage;
        int page = setupPager.getCurrentItem();
        boolean onUpdates = (page == SetupPageType.Updates);
        boolean onSatellites = (page == SetupPageType.Satellites);
        Resources res = this.getResources();

        //update visibility
        progressLayout.setVisibility(page == SetupPageType.Welcome || page == SetupPageType.Finished ? View.INVISIBLE : View.VISIBLE);

        //go through each page
        for(currentPage = 1; currentPage < SetupPageType.PageCount - 1; currentPage++)
        {
            //get progress box
            View progressBox = progressLayout.findViewWithTag(String.valueOf(currentPage - 1));
            if(progressBox != null)
            {
                //set progress box color
                progressBox.setBackgroundResource(currentPage <= page ? Globals.resolveAttributeID(this, currentPage < page ? R.attr.actionBarBackground : R.attr.colorAccent) : R.color.light_gray);
            }
        }

        //allow swiping if not SetupPage.Satellites or not updating
        setupPager.setSwipeEnabled(page != SetupPageType.Satellites || !updateNow);

        //update info text
        infoText.setText(onUpdates ? res.getString(R.string.text_spacetrack_create_account) : "");
        infoText.setVisibility(onUpdates ? View.VISIBLE : View.GONE);

        //update checkbox
        inputCheckBox.setChecked(onSatellites && updateNow);
        inputCheckBox.setText(onSatellites ? res.getString(R.string.desc_update_now) : "");
        inputCheckBox.setVisibility(onSatellites ? View.VISIBLE : View.GONE);

        //update buttons
        backButton.setText(page == SetupPageType.Welcome ? R.string.title_skip : R.string.title_back);
        nextButton.setText(page == SetupPageType.Finished ? R.string.title_finish : R.string.title_next);
    }

    //Update list
    private void updateList()
    {
        Fragment currentFragment = manager.findFragmentByTag(currentPageKey);
        FragmentTransaction currentTransaction;
        PagerAdapter currentAdapter = (setupPager != null ? setupPager.getAdapter() : null);

        //if found current fragment
        if(currentFragment != null)
        {
            try
            {
                //detach then attach to refresh
                currentTransaction = manager.beginTransaction();
                if(Build.VERSION.SDK_INT >= 26)
                {
                    currentTransaction.setReorderingAllowed(false);
                }
                currentTransaction.detach(currentFragment).attach(currentFragment).commit();
            }
            catch(Exception ex)
            {
                //do nothing
            }
        }

        //if found current adapter
        if(currentAdapter != null)
        {
            //update list
            currentAdapter.notifyDataSetChanged();
        }
    }

    //Update floating button
    private void updateFloatingButton()
    {
        int imageID = R.drawable.ic_add_white;
        boolean show = false;

        switch(currentPage)
        {
            case Settings.PageType.Accounts:
                if(inEditMode)
                {
                    show = (accountsListAdapter != null && !accountsListAdapter.haveNonEditItemsSelected());
                    imageID = R.drawable.ic_mode_edit_white;
                }
                else
                {
                    show = (accountsListAdapter != null && accountsListAdapter.getUsedAccounts().size() < Globals.AccountType.Count);
                }
                break;

            case Settings.PageType.Locations:
                show = true;
                if(inEditMode)
                {
                    imageID = R.drawable.ic_mode_edit_white;
                }
                break;

            case Settings.PageType.Notifications:
                show = !inEditMode;
                break;

            default:
                if(showSetup && setupPager != null)
                {
                    show = (setupPager.getCurrentItem() == SetupPageType.Satellites && !UpdateService.modifyingSatellites());
                }
                break;
        }

        //update display
        if(floatingButton != null)
        {
            floatingButton.setImageResource(imageID);
        }
        Globals.setVisible(floatingButton, show);
    }

    //Update back button
    private void updateBackButton()
    {
        backButton.setText(manager.getBackStackEntryCount() > 0 ? R.string.title_back : R.string.title_close);
    }

    //Sets recreate needed
    private void setRecreateNeeded()
    {
        resultIntent.putExtra(EXTRA_RECREATE, true);
    }

    //Sets lens recreate needed
    private void setLensRecreateNeed()
    {
        resultIntent.putExtra(EXTRA_RECREATE_LENS, true);
    }

    //Sets map recreate needed
    private void setMapRecreateNeeded()
    {
        resultIntent.putExtra(EXTRA_RECREATE_MAP, true);
    }

    //Sets reload location needed
    private void setReloadLocationNeeded()
    {
        resultIntent.putExtra(EXTRA_RELOAD_LOCATION, true);
    }

    //Sets update combined layout needed
    private void setUpdateCombinedLayoutNeeded()
    {
        resultIntent.putExtra(EXTRA_UPDATE_COMBINED_LAYOUT, true);
    }

    //Locks screen orientation
    private void lockScreenOrientation(boolean lock)
    {
        Globals.lockScreenOrientation(this, lock);
    }

    //Shows an add account dialog
    private void showAddAccountDialog(ArrayList<Integer> usedAccounts)
    {
        Resources res = this.getResources();
        ArrayList<Integer> unused = new ArrayList<>(0);
        int[] all = new int[]{Globals.AccountType.GoogleDrive, Globals.AccountType.Dropbox, Globals.AccountType.SpaceTrack};

        //get all unused
        for(int currentAccount : all)
        {
            //if not used
            if(!usedAccounts.contains(currentAccount))
            {
                //add to unused
                unused.add(currentAccount);
            }
        }

        //if there are unused
        if(unused.size() > 0)
        {
            //show dialog
            Globals.showSelectDialog(this, res.getString(R.string.title_account_add), AddSelectListAdapter.SelectType.AddAccount, unused.toArray(new Integer[0]), new AddSelectListAdapter.OnItemClickListener()
            {
                @Override
                public void onItemClick(final int which)
                {
                    //handle based on account type
                    switch(which)
                    {
                        case Globals.AccountType.GoogleDrive:
                            Globals.askGoogleDriveAccount(SettingsActivity.this, BaseInputActivity.RequestCode.GoogleDriveAddAccount);
                            break;

                        case Globals.AccountType.Dropbox:
                            DropboxAccess.start(SettingsActivity.this);
                            break;

                        case Globals.AccountType.SpaceTrack:
                            Globals.showAccountLogin(SettingsActivity.this, which, new Globals.WebPageListener()
                            {
                                @Override
                                public void onResult(Globals.WebPageData pageData, boolean success)
                                {
                                    //if success
                                    if(success)
                                    {
                                        //if adapter is set
                                        if(accountsListAdapter != null)
                                        {
                                            SettingsActivity.this.runOnUiThread(new Runnable()
                                            {
                                                @Override
                                                public void run()
                                                {
                                                    //reload items
                                                    accountsListAdapter.reloadItems();
                                                }
                                            });
                                        }
                                    }
                                    else
                                    {
                                        //remove account attempt
                                        Settings.removeSpaceTrackLogin(SettingsActivity.this);
                                    }
                                }
                            }, true);
                            break;
                    }
                }
            });
        }
    }

    //Shows an edit location dialog
    private void showEditLocationDialog()
    {
        int index;
        final Resources res = this.getResources();
        final Fragment setupFragment = (showSetup ? getFragment(SetupPageType.Location) : null);
        ArrayList<Selectable.ListItem> selectedItems = (showSetup && setupFragment instanceof Selectable.ListFragment ? (Selectable.ListFragment)setupFragment : getSettingsPage(currentPageKey)).getSelectedItems();
        int itemCount = selectedItems.size();
        final int[] ids = new int[itemCount];
        final double[] numberValues = new double[itemCount];
        final double[] number2Values = new double[itemCount];
        final double[] number3Values = new double[itemCount];
        final String[] names = new String[itemCount];
        final String[] defaultZones = new String[itemCount];
        final boolean[] idChecked = new boolean[itemCount];
        final ArrayList<TimeZone> zones = Globals.getTimeZoneList();
        final String[] zoneNames = new String[zones.size()];

        //get ids and values
        for(index = 0; index < ids.length; index++)
        {
            //remember current item
            Settings.Locations.Item currentItem = (Settings.Locations.Item)selectedItems.get(index);

            //get current ID and values
            ids[index] = currentItem.id;
            numberValues[index] = currentItem.latitude;
            number2Values[index] = currentItem.longitude;
            number3Values[index] = Globals.getMetersUnitValue(currentItem.altitudeM);
            names[index] = currentItem.name;
            defaultZones[index] = Globals.getGMTOffsetString(currentItem.zone);
            idChecked[index] = currentItem.isChecked;
        }
        for(index = 0; index < zoneNames.length; index++)
        {
            zoneNames[index] = Globals.getGMTOffsetString(zones.get(index));
        }

        //show dialog
        new EditValuesDialog(this, new EditValuesDialog.OnSaveListener()
        {
            @Override
            public void onSave(EditValuesDialog dialog, int itemIndex, int id, String textValue, String text2Value, double number1, double number2, double number3, String listValue, String list2Value, long dateValue)
            {
                String zoneId = zones.get(Arrays.asList(zoneNames).indexOf(list2Value)).getID();
                double altitudeM = (Settings.getUsingMetric() ? number3 : (number3 / Globals.FEET_PER_METER));

                //save location
                Database.saveLocation(SettingsActivity.this, id, textValue, number1, number2, altitudeM, zoneId, Database.LocationType.Online);

                //if the selected location
                if(idChecked[itemIndex])
                {
                    //updated observer
                    setReloadLocationNeeded();
                }
            }
        }, new EditValuesDialog.OnDismissListener()
        {
            @Override
            public void onDismiss(EditValuesDialog dialog, int saveCount)
            {
                //if any were saved
                if(saveCount > 0)
                {
                    //need update
                    settingsLocationsListAdapter.notifyDataSetChanged();
                }

                //end edit mode
                inEditMode = false;
                if(showSetup)
                {
                    ((Selectable.ListFragment)getFragment(SetupPageType.Location)).cancelEditMode();
                }
                else
                {
                    getSettingsPage(currentPageKey).cancelEditMode();
                }
                updateList();
                updateFloatingButton();
            }
        }).getLocation(res.getString(R.string.title_edit), ids, res.getString(R.string.title_name), names, new String[]{res.getString(R.string.title_latitude), res.getString(R.string.title_longitude), res.getString(R.string.title_altitude) + " (" + Globals.getMetersLabel(res) + ")"}, numberValues, number2Values, number3Values, res.getString(R.string.title_time_zone), zoneNames, defaultZones);
    }

    //Shows/dismisses add current location dialog
    private void showAddCurrentLocationDialog(boolean show)
    {
        //update status
        needSaveCurrentLocation = show;
        lockScreenOrientation(show);

        //if showing
        if(show)
        {
            //show
            addCurrentLocationDialog.show();
        }
        //else if dialog exists and is shown
        else if(addCurrentLocationDialog != null && addCurrentLocationDialog.isShowing())
        {
            //dismiss
            addCurrentLocationDialog.dismiss();
        }
    }

    //Shows an add location dialog
    private void showAddLocationDialog()
    {
        Resources res = this.getResources();

        Globals.showSelectDialog(this, res.getString(R.string.title_location_add), AddSelectListAdapter.SelectType.Location, new AddSelectListAdapter.OnItemClickListener()
        {
            @Override
            public void onItemClick(int which)
            {
                //handle based on source
                switch(which)
                {
                    case AddSelectListAdapter.LocationSourceType.Current:
                        //show dialog and wait for update
                        showAddCurrentLocationDialog(true);
                        LocationService.getCurrentLocation(SettingsActivity.this, false, true, LocationService.PowerTypes.HighPowerThenBalanced);
                        break;

                    case AddSelectListAdapter.LocationSourceType.Custom:
                    case AddSelectListAdapter.LocationSourceType.Search:
                        //show map and wait for update
                        MapLocationInputActivity.show(SettingsActivity.this, which == AddSelectListAdapter.LocationSourceType.Search);
                        break;
                }
            }
        });
    }
}
