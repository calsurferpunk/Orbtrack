package com.nikolaiapps.orbtrack;


import android.annotation.SuppressLint;
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
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
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
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;


public class SettingsActivity extends AppCompatActivity implements PreferenceFragmentCompat.OnPreferenceStartFragmentCallback, ActivityResultCallback<ActivityResult>
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
        public static final String GlobeMapView = "globeMapView";
        public static final String MapViewGlobe = "mapViewGlobe";
        public static final String MapViewMap = "mapViewMap";
        public static final String MapViewFootprint = "mapViewFootprint";
        public static final String MapViewLabelData = "mapViewLabelData";
        public static final String MapViewDisplay = "mapViewDisplay";
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
            SwitchPreference allowRotationSwitch;

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
                        SwitchPreference materialThemeSwitch = this.findPreference(Settings.PreferenceName.MaterialTheme);
                        SwitchPreference allowNumberCommasSwitch = this.findPreference(Settings.PreferenceName.AllowNumberCommas);
                        SwitchButtonPreference useLocationTintSwitch = this.findPreference(Settings.PreferenceName.MapMarkerLocationIconUseTint);
                        IconListPreference colorThemeList = this.findPreference(Settings.PreferenceName.ColorTheme);
                        IconListPreference locationIconList = this.findPreference(Settings.PreferenceName.MapMarkerLocationIcon);
                        IconListPreference satelliteIconList = this.findPreference(Settings.PreferenceName.SatelliteIcon);
                        IconListPreference orbitalIconsList = this.findPreference(Settings.PreferenceName.OrbitalIcons);
                        PreferenceCategory iconsCategory = this.findPreference("IconsCategory");

                        //always reset location icon items to update tint color
                        Settings.Options.Display.locationIconItems = null;

                        //initialize values
                        Settings.Options.Display.initValues(context);

                        //setup displays
                        setupSwitch(darkThemeSwitch);
                        setupSwitch(materialThemeSwitch);
                        setupSwitch(allowNumberCommasSwitch);
                        setupSwitchButton(useLocationTintSwitch);
                        setupList(colorThemeList, Settings.Options.Display.colorAdvancedItems, null, null, null, null);
                        setupList(locationIconList, Settings.Options.Display.locationIconItems, null, null, null, useLocationTintSwitch);
                        setupList(satelliteIconList, Settings.Options.Display.satelliteIconItems, null, null, null, null);
                        setupList(orbitalIconsList, Settings.Options.Display.orbitalIconsItems, null, null, null, null);
                        setupCategory(iconsCategory);
                        break;

                    case ScreenKey.LensView:
                        SwitchPreference useCameraSwitch = this.findPreference(Settings.PreferenceName.LensUseCamera);
                        SwitchPreference rotateSwitch = this.findPreference(Settings.PreferenceName.LensRotate);
                        SwitchPreference lensShowIconDirection = this.findPreference(Settings.PreferenceName.LensIndicatorIconShowDirection);
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
                        setupSwitch(useCameraSwitch);
                        setupSwitch(rotateSwitch);
                        setupSwitch(lensShowIconDirection);
                        setupSwitchButton(lensUseHorizonSwitch);
                        setupSwitchText(lensWidthSwitch);
                        setupSwitchText(lensHeightSwitch);
                        setupSwitchText(lensAzimuthOffsetSwitch);
                        setupList(lensOrbitalIconList, Settings.Options.LensView.indicatorItems, null, null, null, lensShowIconDirection);
                        setupList(lensUpdateRateList, Settings.Options.Rates.updateRateItems, null, null, null, null);
                        setupList(lensSensorSmoothingList, Settings.Options.LensView.sensorSmoothingItems, null, null, null, null);
                        break;

                    case ScreenKey.ListView:
                        SwitchPreference pathProgressSwitch = this.findPreference(Settings.PreferenceName.ListShowPassProgress);
                        SwitchPreference passQualitySwitch = this.findPreference(Settings.PreferenceName.ListShowPassQuality);
                        IconListPreference listUpdateRateList = this.findPreference(Settings.PreferenceName.ListUpdateDelay);

                        //initialize values
                        Settings.Options.Rates.initValues(context);

                        //setup displays
                        setupSwitch(pathProgressSwitch);
                        setupSwitch(passQualitySwitch);
                        setupList(listUpdateRateList, Settings.Options.Rates.updateRateItems, null, null, null, null);
                        break;

                    case ScreenKey.MapViewGlobe:
                        SwitchPreference showCloudsGlobeSwitch = this.findPreference(Settings.PreferenceName.ShowSatelliteClouds + Settings.SubPreferenceName.Globe);
                        SwitchPreference showSunlightSwitch = this.findPreference(Settings.PreferenceName.MapShowSunlight);
                        SwitchPreference show3dPathsSwitch = this.findPreference(Settings.PreferenceName.MapShow3dPaths);
                        allowRotationSwitch = this.findPreference(Settings.PreferenceName.MapRotateAllowed);
                        SliderPreference globeSensitivitySlider = this.findPreference(Settings.PreferenceName.MapSensitivityScale + Settings.SubPreferenceName.Globe);
                        SliderPreference globeSpeedScaleSlider = this.findPreference(Settings.PreferenceName.MapSpeedScale + Settings.SubPreferenceName.Globe);
                        IconListPreference globeTypeList = this.findPreference(Settings.PreferenceName.MapLayerType + Settings.SubPreferenceName.Globe);
                        IconListPreference globeUpdateRateList = this.findPreference(Settings.PreferenceName.MapUpdateDelay);

                        //initialize values
                        Settings.Options.MapView.initValues(context);
                        Settings.Options.Rates.initValues(context);

                        //setup displays
                        setupSwitch(showCloudsGlobeSwitch);
                        setupSwitch(showSunlightSwitch);
                        setupSwitch(show3dPathsSwitch);
                        setupSwitch(allowRotationSwitch);
                        setupSlider(globeSensitivitySlider);
                        setupSlider(globeSpeedScaleSlider);
                        setupList(globeTypeList, Settings.Options.MapView.mapTypeItems, Settings.Options.MapView.MapTypeValues, null, null, showCloudsGlobeSwitch);
                        setupList(globeUpdateRateList, Settings.Options.Rates.updateRateItems, null, null, null, null);
                        break;

                    case ScreenKey.MapViewMap:
                        SwitchPreference showCloudsMapSwitch = this.findPreference(Settings.PreferenceName.ShowSatelliteClouds + Settings.SubPreferenceName.Map);
                        allowRotationSwitch = this.findPreference(Settings.PreferenceName.MapRotateAllowed);
                        SliderPreference mapSensitivitySlider = this.findPreference(Settings.PreferenceName.MapSensitivityScale + Settings.SubPreferenceName.Map);
                        SliderPreference mapSpeedScaleSlider = this.findPreference(Settings.PreferenceName.MapSpeedScale + Settings.SubPreferenceName.Map);
                        IconListPreference mapTypeList = this.findPreference(Settings.PreferenceName.MapLayerType + Settings.SubPreferenceName.Map);
                        IconListPreference mapUpdateRateList = this.findPreference(Settings.PreferenceName.MapUpdateDelay);

                        //initialize values
                        Settings.Options.MapView.initValues(context);
                        Settings.Options.Rates.initValues(context);

                        //setup displays
                        setupSwitch(showCloudsMapSwitch);
                        setupSwitch(allowRotationSwitch);
                        setupSlider(mapSensitivitySlider);
                        setupSlider(mapSpeedScaleSlider);
                        setupList(mapTypeList, Settings.Options.MapView.mapTypeItems, Settings.Options.MapView.MapTypeValues, null, null, showCloudsMapSwitch);
                        setupList(mapUpdateRateList, Settings.Options.Rates.updateRateItems, null, null, null, null);
                        break;

                    case ScreenKey.MapViewFootprint:
                        SwitchPreference showFootprint = this.findPreference(Settings.PreferenceName.MapShowFootprint);
                        SwitchButtonPreference showSelectedFootprint = this.findPreference(Settings.PreferenceName.MapShowSelectedFootprint);
                        SliderPreference footprintAlphaSlider = this.findPreference(Settings.PreferenceName.MapFootprintAlpha);
                        IconListPreference footprintTypeList = this.findPreference(Settings.PreferenceName.MapFootprintType);

                        //initialize values
                        Settings.Options.MapView.initValues(context);

                        //setup displays
                        setupSwitch(showFootprint);
                        setupSwitchButton(showSelectedFootprint);
                        setupSlider(footprintAlphaSlider);
                        setupList(footprintTypeList, Settings.Options.MapView.footprintTypeItems, Settings.Options.MapView.FootprintTypeValues, null, null, null);
                        break;

                    case ScreenKey.MapViewLabelData:
                        SwitchPreference showInformationBackgroundSwitch = this.findPreference(Settings.PreferenceName.MapMarkerShowBackground);
                        IconListPreference informationLocationList = this.findPreference(Settings.PreferenceName.MapMarkerInfoLocation);

                        //initialize values
                        Settings.Options.MapView.initValues(context);

                        //setup displays
                        setupSwitch(showInformationBackgroundSwitch);
                        setupList(informationLocationList, Settings.Options.MapView.infoLocationItems, Settings.Options.MapView.InfoLocationValues, null, null, null);
                        break;

                    case ScreenKey.MapViewDisplay:
                        SwitchPreference showOrbitalDirection = this.findPreference(Settings.PreferenceName.MapShowOrbitalDirection);
                        SwitchPreference showToolbarsSwitch = this.findPreference(Settings.PreferenceName.MapShowToolbars);
                        SwitchPreference showZoomSwitch = this.findPreference(Settings.PreferenceName.MapShowZoom);
                        SwitchPreference showLabelsAlwaysSwitch = this.findPreference(Settings.PreferenceName.MapShowLabelsAlways);
                        SwitchPreference showShadowsSwitch = this.findPreference(Settings.PreferenceName.MapMarkerShowShadow);
                        SwitchPreference showStarsSwitch = this.findPreference(Settings.PreferenceName.MapShowStars);
                        SwitchTextPreference showOrbitalDirectionLimit = this.findPreference(Settings.PreferenceName.MapShowOrbitalDirectionLimit);
                        SwitchButtonPreference showGridSwitch = this.findPreference(Settings.PreferenceName.MapShowGrid);
                        SliderPreference iconScaleSlider = this.findPreference(Settings.PreferenceName.MapMarkerScale);
                        IconListPreference mapFrameRateList = this.findPreference(Settings.PreferenceName.MapFrameRate);

                        //initialize values
                        Settings.Options.MapView.initValues(context);
                        Settings.Options.Rates.initValues(context);

                        //setup displays
                        setupSwitch(showOrbitalDirection, showOrbitalDirectionLimit);
                        setupSwitch(showToolbarsSwitch);
                        setupSwitch(showZoomSwitch);
                        setupSwitch(showLabelsAlwaysSwitch);
                        setupSwitch(showShadowsSwitch);
                        setupSwitch(showStarsSwitch);
                        setupSwitchText(showOrbitalDirectionLimit);
                        setupSwitchButton(showGridSwitch);
                        setupSlider(iconScaleSlider);
                        setupList(mapFrameRateList, Settings.Options.Rates.frameRateItems, null, null, null, null);
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
                        SwitchPreference sharedSourceSwitch = this.findPreference(Settings.PreferenceName.SatelliteSourceShared);
                        SwitchPreference useDefaultNextColorSwitch = this.findPreference(Settings.PreferenceName.SatelliteUseNextDefaultColor);
                        IconListPreference satelliteSourceList = this.findPreference(Settings.PreferenceName.SatelliteSource);
                        IconListPreference satelliteDataList = this.findPreference(Settings.PreferenceName.SatelliteDataSource);
                        IconListPreference altitudeList = this.findPreference(Settings.PreferenceName.AltitudeSource);
                        IconListPreference timeZoneList = this.findPreference(Settings.PreferenceName.TimeZoneSource);
                        IconListPreference informationList = this.findPreference(Settings.PreferenceName.InformationSource);
                        TimeIntervalPreference tleAutoTime = this.findPreference(Settings.PreferenceName.TLEAutoUpdateRate);
                        TimeIntervalPreference catalogAutoTime = this.findPreference(Settings.PreferenceName.CatalogAutoUpdateRate);
                        PreferenceCategory altitudeCategory = this.findPreference("AltitudeCategory");
                        PreferenceCategory timeZoneCategory = this.findPreference("TimeZoneCategory");
                        PreferenceCategory informationCategory = this.findPreference("InformationCategory");
                        PreferenceCategory colorsCategory = this.findPreference("ColorsCategory");

                        //initialize values
                        Settings.Options.Updates.initValues(context);

                        //setup displays
                        setupSwitch(tleAutoSwitch, tleAutoTime);
                        setupSwitch(catalogAutoSwitch, catalogAutoTime);
                        setupSwitch(rocketBodySwitch);
                        setupSwitch(debrisSwitch);
                        setupSwitch(legacyDataSwitch);
                        setupSwitch(translateInformationSwitch);
                        setupSwitch(shareTranslateSwitch);
                        setupSwitch(sharedSourceSwitch, satelliteDataList, satelliteSourceList);
                        setupSwitch(useDefaultNextColorSwitch);
                        setupList(satelliteSourceList, Settings.Options.Updates.SatelliteSourceItems, Settings.Options.Updates.SatelliteSourceValues, Settings.Options.Updates.SatelliteSourceImageIds, Settings.Options.Updates.SatelliteSourceSubTexts, legacyDataSwitch);
                        setupList(satelliteDataList, Settings.Options.Updates.SatelliteDataSourceItems, Settings.Options.Updates.SatelliteDataSourceValues, Settings.Options.Updates.SatelliteDataSourceImageIds, Settings.Options.Updates.SatelliteDataSourceSubTexts, legacyDataSwitch);
                        setupList(altitudeList, Settings.Options.Updates.AltitudeSourceItems, Settings.Options.Updates.AltitudeSourceValues, Settings.Options.Updates.AltitudeSourceImageIds, null, null);
                        setupList(timeZoneList, Settings.Options.Updates.TimeZoneSourceItems, Settings.Options.Updates.TimeZoneSourceValues, Settings.Options.Updates.TimeZoneSourceImageIds, null, null);
                        setupList(informationList, Settings.Options.Updates.InformationSourceItems, Settings.Options.Updates.InformationSourceValues, Settings.Options.Updates.InformationSourceImageIds, null, null);
                        setupTimeInterval(tleAutoTime);
                        setupTimeInterval(catalogAutoTime);
                        setupCategory(altitudeCategory);
                        setupCategory(timeZoneCategory);
                        setupCategory(informationCategory);
                        setupCategory(colorsCategory);

                        //if showing setup
                        if(showSetup)
                        {
                            //if TLE auto switch exists
                            if(tleAutoSwitch != null)
                            {
                                Preference.OnPreferenceChangeListener listener = tleAutoSwitch.getOnPreferenceChangeListener();

                                //set to checked and call change listener
                                tleAutoSwitch.setChecked(true);
                                if(listener != null)
                                {
                                    listener.onPreferenceChange(tleAutoSwitch, true);
                                }
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
                case Settings.PreferenceName.SatelliteIcon:
                case Settings.PreferenceName.OrbitalIcons:
                case Settings.PreferenceName.MapMarkerLocationIcon:
                case Settings.PreferenceName.MapMarkerLocationIconUseTint:
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
                int index;
                int value = -1;
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
                                public boolean onPreferenceChange(@NonNull Preference preference, Object newValue)
                                {
                                    //allow if changing
                                    return((int)newValue != Settings.getColorTheme(context));
                                }
                            });
                            break;

                        case Settings.PreferenceName.SatelliteIcon:
                            currentValue = Settings.getSatelliteIconType(context);
                            break;

                        case Settings.PreferenceName.OrbitalIcons:
                            currentValue = Settings.getOrbitalIconsType(context);
                            break;

                        case Settings.PreferenceName.LensIndicator:
                            currentValue = Settings.getIndicator(context);

                            preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
                            {
                                @Override
                                public boolean onPreferenceChange(@NonNull Preference preference, Object newValue)
                                {
                                    int indicatorType = (int)newValue;

                                    //if child preference exists
                                    if(childPreference != null)
                                    {
                                        childPreference.setVisible(indicatorType == Settings.Options.LensView.IndicatorType.Icon);
                                    }

                                    //allow change
                                    return(true);
                                }
                            });
                            break;

                        case Settings.PreferenceName.LensUpdateDelay:
                            currentValue = Settings.getLensUpdateDelay(context);
                            break;

                        case Settings.PreferenceName.ListUpdateDelay:
                            currentValue = Settings.getListUpdateDelay(context);
                            break;

                        case Settings.PreferenceName.MapFrameRate:
                            currentValue = Settings.getMapFrameRate(context);
                            break;

                        case Settings.PreferenceName.MapUpdateDelay:
                            currentValue = Settings.getMapUpdateDelay(context);
                            break;

                        case Settings.PreferenceName.MapLayerType + Settings.SubPreferenceName.Globe:
                            forGlobe = true;
                            //fall through

                        case Settings.PreferenceName.MapLayerType + Settings.SubPreferenceName.Map:
                            currentValue = Settings.getMapLayerType(context, forGlobe);

                            preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
                            {
                                @Override
                                public boolean onPreferenceChange(@NonNull Preference preference, Object newValue)
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

                        case Settings.PreferenceName.MapFootprintType:
                            currentValue = Settings.getMapFootprintType(context);
                            break;

                        case Settings.PreferenceName.MapMarkerLocationIcon:
                            currentValue = Settings.getMapMarkerLocationIcon(context);

                            preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
                            {
                                @Override
                                public boolean onPreferenceChange(@NonNull Preference preference, Object newValue)
                                {
                                    //if child preference exists
                                    if(childPreference != null)
                                    {
                                        //update child visibility
                                        childPreference.setVisible(Settings.getLocationIconTypeCanTint((int)newValue));
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
                        case Settings.PreferenceName.SatelliteDataSource:
                            boolean isCatalog = preferenceKey.equals(Settings.PreferenceName.SatelliteSource);
                            value = (isCatalog ? Settings.getSatelliteCatalogSource(context) : Settings.getSatelliteDataSource(context));

                            preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
                            {
                                @Override
                                public boolean onPreferenceChange(@NonNull Preference preference, Object newValue)
                                {
                                    boolean isSharedSource = Settings.getSatelliteSourceShared(context);
                                    byte dataSource = (!isCatalog || isSharedSource ? (byte)newValue : Integer.valueOf(Settings.getSatelliteDataSource(context)).byteValue());
                                    byte catalogSource = (isCatalog ? (byte)newValue : Integer.valueOf(Settings.getSatelliteCatalogSource(context)).byteValue());
                                    SwitchPreference childSwitch = (SwitchPreference)childPreference;
                                    boolean sourceUseGP = Settings.getSatelliteSourceUseGP(context, dataSource);

                                    //update child switch
                                    childSwitch.setSubKey(String.valueOf(dataSource));
                                    childSwitch.setChecked(!sourceUseGP);
                                    childSwitch.setEnabled(dataSource != Database.UpdateSource.N2YO && dataSource != Database.UpdateSource.HeavensAbove);

                                    //if changing catalog source
                                    if(catalogSource != Settings.getSatelliteCatalogSource(context))
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

                    //if value is set
                    if(value >= 0)
                    {
                        //go through values
                        for(index = 0; index < values.length && valueIndex == -1; index++)
                        {
                            //if value and index matches value
                            if(values[index] instanceof Byte && (byte)values[index] == value)
                            {
                                //set value index
                                valueIndex = index;
                            }
                        }
                    }

                    //if value index set and valid
                    if(valueIndex >= 0 && valueIndex < values.length)
                    {
                        //remember current value
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

        //Sets up a preference switch
        private static void setupSwitch(SwitchPreference preference, Preference childPreference, Preference childPreference2)
        {
            //if preference exists
            if(preference != null)
            {
                final Context context = preference.getContext();
                final SharedPreferences.Editor writeSettings = Settings.getWriteSettings(context);
                final String preferenceKey = preference.getKey();
                final boolean isGPDataUsage = preferenceKey.equals(Settings.PreferenceName.SatelliteSourceUseGP);
                final boolean isSourceShared = preferenceKey.equals(Settings.PreferenceName.SatelliteSourceShared);
                final boolean useCompatibility = Settings.getUseGlobeCompatibility(context);
                final Object dependency = Settings.getSatelliteDataSource(context);
                final boolean checked = Settings.getPreferenceBoolean(context, preferenceKey + (isGPDataUsage ? dependency : ""), (isGPDataUsage ? dependency : null));

                //if preference should be visible
                if(preferenceVisible(preferenceKey))
                {
                    //handle compatibility
                    if(preferenceKey.equals(Settings.PreferenceName.MapMarkerShowShadow))
                    {
                        //enabled if not using compatibility
                        preference.setEnabled(!useCompatibility);
                    }

                    //set state and listener
                    preference.setIconSpaceReserved(false);
                    preference.setChecked(checked);
                    preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
                    {
                        @Override
                        public boolean onPreferenceChange(@NonNull Preference preference, Object newValue)
                        {
                            boolean checked = (Boolean)newValue;
                            boolean visible = checked;
                            boolean isAutoUpdate = (childPreference instanceof TimeIntervalPreference);
                            String subKey = ((SwitchPreference)preference).getSubKey();
                            UpdateService.AlarmUpdateSettings settings = (isAutoUpdate ? Settings.getAutoUpdateSettings(context, preferenceKey) : null);

                            //if -not for auto update- or -settings are changing-
                            if(!isAutoUpdate || settings.enabled != checked)
                            {
                                //if for GP data usage
                                if(isGPDataUsage)
                                {
                                    //invert checked
                                    checked = visible = !checked;
                                }

                                //if for shared source
                                if(isSourceShared)
                                {
                                    //invert visible
                                    visible = !visible;
                                }

                                //save preference
                                writeSettings.putBoolean(preferenceKey + subKey, checked).apply();

                                //if auto update
                                if(isAutoUpdate)
                                {
                                    //apply changes
                                    Settings.setAutoUpdate(context, preferenceKey);
                                }
                                //else if satellite using next default color
                                else if(preferenceKey.equals(Settings.PreferenceName.SatelliteUseNextDefaultColor))
                                {
                                    //apply changes
                                    Settings.setSatelliteUseNextDefaultColor(context, checked);
                                }
                            }

                            //if child preference is set
                            if(childPreference != null)
                            {
                                //update visibility
                                childPreference.setVisible(visible);
                            }
                            if(childPreference2 instanceof IconListPreference && isSourceShared)
                            {
                                IconListPreference satelliteSourceList = (IconListPreference)childPreference2;

                                //if shared source
                                if(checked)
                                {
                                    //update selected catalog and data source
                                    satelliteSourceList.setSelectedValue(Settings.getSatelliteCatalogSource(context));
                                }

                                //update title
                                satelliteSourceList.setTitle(checked ? R.string.title_source : R.string.title_catalog);
                            }

                            //allow change
                            return(true);
                        }
                    });

                    //if might need changes
                    if(childPreference != null)
                    {
                        Preference.OnPreferenceChangeListener listener = preference.getOnPreferenceChangeListener();

                        if(listener != null)
                        {
                            //call once to update changes
                            listener.onPreferenceChange(preference, checked);
                        }
                    }
                }
                else
                {
                    //hide preference
                    preference.setVisible(false);
                }
            }
        }
        private static void setupSwitch(SwitchPreference preference, Preference childPreference)
        {
            setupSwitch(preference, childPreference, null);
        }
        private static void setupSwitch(SwitchPreference preference)
        {
            setupSwitch(preference, null);
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

                    case Settings.PreferenceName.MapFootprintAlpha:
                        min = 0;
                        max = 100;
                        break;
                }

                //if range has been set
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
                final SharedPreferences.Editor writeSettings = Settings.getWriteSettings(context);
                final String preferenceKey = preference.getKey();
                final String buttonPreferenceKey;
                final boolean allowOpacity;
                final boolean isSelectedFootprint = (preferenceKey.equals(Settings.PreferenceName.MapShowSelectedFootprint));

                //if preference should be visible
                if(preferenceVisible(preferenceKey))
                {
                    //if lens horizon or show grid toggle
                    switch(preferenceKey)
                    {
                        case Settings.PreferenceName.LensUseHorizon:
                        case Settings.PreferenceName.MapShowGrid:
                        case Settings.PreferenceName.MapShowSelectedFootprint:
                        case Settings.PreferenceName.MapMarkerLocationIconUseTint:
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
                            else if(isSelectedFootprint)
                            {
                                titleId = R.string.title_footprint_color;
                                buttonPreferenceKey = Settings.PreferenceName.MapSelectedFootprintColor;
                                allowOpacity = true;
                            }
                            else if(preferenceKey.equals(Settings.PreferenceName.MapMarkerLocationIconUseTint))
                            {
                                titleId = R.string.title_tint_color;
                                buttonPreferenceKey = Settings.PreferenceName.MapMarkerLocationIconTintColor;
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
                            if(isSelectedFootprint)
                            {
                                preference.setCheckedChangedListener(new SwitchButtonPreference.OnCheckedChangedListener()
                                {
                                    @Override
                                    public void onCheckedChanged(String preferenceName, boolean isChecked)
                                    {
                                        Settings.setMapShowSelectedFootprint(context, isChecked);
                                    }
                                });
                            }
                            break;
                    }
                }
                else
                {
                    //hide preference
                    preference.setVisible(false);
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
                final boolean useCompatibility = Settings.getUseGlobeCompatibility(context);
                final String preferenceKey = preference.getKey();
                String value = null;
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
                        value = String.valueOf(Settings.getLensAzimuthUserOffset(context));
                        break;

                    case Settings.PreferenceName.MapShowOrbitalDirectionLimit:
                        //switch enabled if not using compatibility and checked if using
                        preference.setSwitchEnabled(!useCompatibility);
                        if(useCompatibility)
                        {
                            preference.setSwitchChecked(true);
                        }

                        //set value
                        value = String.valueOf(Settings.getMapShowOrbitalDirectionLimit(context));
                        break;
                }

                //if using single value
                if(value != null)
                {
                    //set value text
                    preference.setValueText(value);
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
        protected boolean setupActionModeItems(MenuItem all, MenuItem none, MenuItem edit, MenuItem delete, MenuItem save, MenuItem sync)
        {
            return(false);
        }

        @Override
        protected void onActionModeSelect(boolean all) {}

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
    private byte lastUpdateType;
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
    private MaterialButton loadingCancelButton;
    private FloatingActionButton floatingButton;
    private CircularProgressIndicator loadingBar;
    private AlertDialog addCurrentLocationDialog;
    private LocationReceiver locationReceiver;
    private UpdateReceiver localUpdateReceiver;
    private ActivityResultLauncher<Intent> resultLauncher;
    private ActivityResultLauncher<Intent> otherOpenLauncher;
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

        //set defaults
        currentPage = -1;
        currentPageKey = startScreenKey;
        isLoading = updateNow = false;
        locationSource = Database.LocationType.Current;
        lastUpdateType = UpdateService.UpdateType.UpdateSatellites;
        orbitalsPageAdapter = null;
        settingsPageAdapter = null;
        settingsLocationsListAdapter = null;
        accountsListAdapter = null;
        informationChangedListener = null;
        createResultIntent();
        BaseInputActivity.setRequestCode(resultIntent, BaseInputActivity.getRequestCode(startIntent));

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
        loadingCancelButton = (showSetup ? this.findViewById(R.id.Setup_Loading_Cancel_Button) : null);
        if(loadingCancelButton != null)
        {
            loadingCancelButton.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    //cancel last update
                    UpdateService.cancel(lastUpdateType);
                    setLoading(false);
                }
            });
        }
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
                case ScreenKey.GlobeMapView:
                case ScreenKey.LensView:
                case ScreenKey.ListView:
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
            }
        }, new DialogInterface.OnCancelListener()
        {
            @Override
            public void onCancel(DialogInterface dialog)
            {
                //cancel
                needSaveCurrentLocation = false;
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
        resultLauncher = Globals.createActivityLauncher(this, this);
        otherOpenLauncher = Globals.createActivityLauncher(this, this, BaseInputActivity.RequestCode.OthersOpenItem);
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
                        case Settings.PreferenceName.MaterialTheme:
                        case Settings.PreferenceName.SatelliteIcon:
                        case Settings.PreferenceName.OrbitalIcons:
                            recreateThis = true;
                            //fall through

                        case Settings.PreferenceName.ListShowPassProgress:
                        case Settings.PreferenceName.ListShowPassQuality:
                        case Settings.PreferenceName.MetricUnits:
                        case Settings.PreferenceName.AllowNumberCommas:
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
                        case Settings.PreferenceName.LensIndicatorIconShowDirection:
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

                        case Settings.PreferenceName.MapMarkerLocationIconUseTint:
                        case Settings.PreferenceName.MapMarkerLocationIconTintColor:
                            //update location icon list
                            List<Fragment> fragments = manager.getFragments();
                            for(Fragment currentFragment : fragments)
                            {
                                //if a SettingsSubFragment
                                if(currentFragment instanceof SettingsSubFragment)
                                {
                                    //get location icon preference
                                    Preference locationIconPreference = ((SettingsSubFragment)currentFragment).findPreference(Settings.PreferenceName.MapMarkerLocationIcon);
                                    if(locationIconPreference instanceof IconListPreference)
                                    {
                                        IconListPreference locationIconList = (IconListPreference)locationIconPreference;

                                        //reset location icon items to update tint color
                                        Settings.Options.Display.locationIconItems = null;
                                        Settings.Options.Display.initValues(SettingsActivity.this);

                                        //update adapter
                                        locationIconList.setAdapter(SettingsActivity.this, Settings.Options.Display.locationIconItems, true);
                                    }
                                }
                            }
                            //fall through

                        case Settings.PreferenceName.MapMarkerInfoLocation:
                            if(key.equals(Settings.PreferenceName.MapMarkerInfoLocation))
                            {
                                //update global setting
                                Settings.setMapMarkerInfoLocation(SettingsActivity.this, Settings.getMapMarkerInfoLocation(SettingsActivity.this));
                            }
                            //fall through

                        case Settings.PreferenceName.ListUpdateDelay:
                        case Settings.PreferenceName.MapShow3dPaths:
                        case Settings.PreferenceName.MapRotateAllowed:
                        case Settings.PreferenceName.MapMarkerShowBackground:
                        case Settings.PreferenceName.MapShowToolbars:
                        case Settings.PreferenceName.MapShowZoom:
                        case Settings.PreferenceName.MapShowLabelsAlways:
                        case Settings.PreferenceName.MapShowOrbitalDirection:
                        case Settings.PreferenceName.MapShowOrbitalDirectionLimit:
                        case Settings.PreferenceName.MapFootprintType:
                        case Settings.PreferenceName.MapFootprintAlpha:
                        case Settings.PreferenceName.MapSelectedFootprintColor:
                        case Settings.PreferenceName.MapMarkerShowShadow:
                        case Settings.PreferenceName.MapFrameRate:
                        case Settings.PreferenceName.MapUpdateDelay:
                        case Settings.PreferenceName.MapShowStars:
                        case Settings.PreferenceName.MapShowSunlight:
                        case Settings.PreferenceName.MapShowGrid:
                        case Settings.PreferenceName.MapGridColor:
                        case Settings.PreferenceName.MapMarkerScale:
                        case Settings.PreferenceName.MapMarkerLocationIcon:
                            //map needs recreate
                            setMapRecreateNeeded();
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
        locationReceiver.unregister();
        localUpdateReceiver.unregister();

        //stop listener
        if(preferences != null && preferenceChangeListener != null)
        {
            preferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
        }
    }

    @Override
    public void onActivityResult(ActivityResult result)
    {
        int resultCode = result.getResultCode();
        boolean isOkay = (resultCode == RESULT_OK);
        int id;
        int progressType = Globals.ProgressType.Unknown;
        byte requestCode;
        Intent data = result.getData();
        Resources res = this.getResources();

        //if no data
        if(data == null)
        {
            //set to empty
            data = new Intent();
        }

        //get status
        requestCode = BaseInputActivity.getRequestCode(data);

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
                    setLoading(true);
                    lastUpdateType = UpdateService.UpdateType.LoadFile;
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

                    //if location
                    if(requestCode == BaseInputActivity.RequestCode.MapLocationInput)
                    {
                        //update observer
                        setReloadLocationNeeded();
                    }
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
                BaseInputActivity.handleActivityGoogleDriveOpenFileBrowserRequest(this, resultLauncher, settingsLayout, data, isOkay);
                break;

            case BaseInputActivity.RequestCode.GoogleDriveOpenFile:
            case BaseInputActivity.RequestCode.DropboxOpenFile:
            case BaseInputActivity.RequestCode.OthersOpenItem:
                //if selected item
                if(isOkay)
                {
                    //handle open file request
                    setLoading(true);
                    lastUpdateType = UpdateService.UpdateType.LoadFile;
                    BaseInputActivity.handleActivityOpenFileRequest(this, data, requestCode);
                }
                break;
        }
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
                    Orbitals.showSDCardFileBrowser(this, resultLauncher, settingsLayout);
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
    public boolean onPreferenceStartFragment(@NonNull PreferenceFragmentCompat caller, Preference pref)
    {
        Bundle args = pref.getExtras();
        String fragmentClass = pref.getFragment();
        boolean haveFragment;
        boolean isPage = Settings.Page.class.getName().equals(fragmentClass);
        Fragment fragment = null;

        //update current page key
        currentPageKey = pref.getKey();

        //if a page
        if(isPage)
        {
            CharSequence title = pref.getTitle();

            if(title != null)
            {
                //create settings fragment
                fragment = getSettingsFragment(currentPageKey, title.toString());
            }
        }
        else if(fragmentClass != null)
        {
            //update current page
            currentPage = -1;

            //create preference fragment
            fragment = manager.getFragmentFactory().instantiate(getClassLoader(), fragmentClass);

            //set arguments
            args.putString(RootKey, currentPageKey);
            fragment.setArguments(args);
        }

        //if fragment exists
        haveFragment = (fragment != null);
        if(haveFragment)
        {
            //add fragment
            manager.beginTransaction().replace(R.id.Settings_Layout_Fragment, fragment, (isPage ? currentPageKey : null)).addToBackStack(null).commit();
        }

        //handled if have fragment
        return(haveFragment);
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
            settingsPageAdapter = new Settings.PageAdapter(manager, settingsLayout, null, createOnItemCheckChangedListener(), createOnSetAdapterListener(), createOnUpdateNeededListener(), createOnPageResumeListener(), new int[Settings.PageType.PageCount]);
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
            Orbitals.showAddDialog(this, resultLauncher, otherOpenLauncher, settingsLayout, isLong);
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
                    showAddLocationDialog(resultLauncher);
                }
                break;

            case Settings.PageType.Notifications:
                Database.DatabaseLocation currentLocation = Database.getLocation(this);
                NotifySettingsActivity.show(this, resultLauncher, new Calculations.ObserverType(currentLocation.zoneId, new Calculations.GeodeticDataType(currentLocation.latitude, currentLocation.longitude, currentLocation.altitudeKM, 0, 0)));
                break;
        }
    }

    //Creates result intent if it does not exist yet
    private void createResultIntent()
    {
        //if result intent does not exist yet
        if(resultIntent == null)
        {
            //create intent
            resultIntent = new Intent();
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
            protected void onRestart(Context context)
            {
                //if using current location
                if(locationSource == Database.LocationType.Current)
                {
                    //continue with restart
                    super.onRestart(context);
                }
            }

            @Override @SuppressLint("NotifyDataSetChanged")
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
            protected void onGeneralUpdate(int progressType, byte updateType, boolean ended, String section, int index, int count, File usedFile)
            {
                boolean loadingFile = (updateType == UpdateService.UpdateType.LoadFile);
                boolean updatingSatellites = (updateType == UpdateService.UpdateType.UpdateSatellites);

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

                //if updating satellites or loading a file
                if(updatingSatellites || loadingFile)
                {
                    //handle based on progress type
                    switch(progressType)
                    {
                        case Globals.ProgressType.Denied:
                            //if updating satellites
                            if(updatingSatellites)
                            {
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
                            }
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
                            LocationService.getCurrentLocation(context, false, LocationService.PowerTypes.HighPowerThenBalanced);
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
                    //reload list
                    ((Settings.Page)page).reload();
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
                colorDialog.setTitle(context.getString(titleId));
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
        return(page != null ? (Settings.Page)page : new Settings.Page());
    }

    //Gets the desired fragment
    @SuppressWarnings("SameParameterValue")
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
        return(new Settings.Page());
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

    //Returns result launcher
    public ActivityResultLauncher<Intent> getResultLauncher(byte requestCode)
    {
        return(requestCode == BaseInputActivity.RequestCode.OthersOpenItem ? otherOpenLauncher : resultLauncher);
    }
    public ActivityResultLauncher<Intent> getResultLauncher()
    {
        return(getResultLauncher(BaseInputActivity.RequestCode.None));
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
        if(loadingCancelButton != null)
        {
            loadingCancelButton.setVisibility(isLoading && onSatellites ? View.VISIBLE : View.GONE);
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

        //update last update type
        lastUpdateType = UpdateService.UpdateType.UpdateSatellites;

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

                //if a settings page
                if(currentFragment instanceof Settings.Page)
                {
                    //reload list
                    ((Settings.Page)currentFragment).reload();
                }
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
        createResultIntent();
        resultIntent.putExtra(EXTRA_RECREATE, true);
    }

    //Sets lens recreate needed
    private void setLensRecreateNeed()
    {
        createResultIntent();
        resultIntent.putExtra(EXTRA_RECREATE_LENS, true);
    }

    //Sets map recreate needed
    private void setMapRecreateNeeded()
    {
        createResultIntent();
        resultIntent.putExtra(EXTRA_RECREATE_MAP, true);
    }

    //Sets reload location needed
    private void setReloadLocationNeeded()
    {
        createResultIntent();
        resultIntent.putExtra(EXTRA_RELOAD_LOCATION, true);
    }

    //Sets update combined layout needed
    private void setUpdateCombinedLayoutNeeded()
    {
        createResultIntent();
        resultIntent.putExtra(EXTRA_UPDATE_COMBINED_LAYOUT, true);
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
                            Globals.askGoogleDriveAccount(SettingsActivity.this, resultLauncher, BaseInputActivity.RequestCode.GoogleDriveAddAccount);
                            break;

                        case Globals.AccountType.Dropbox:
                            DropboxAccess.start(SettingsActivity.this, resultLauncher);
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
            public void onSave(EditValuesDialog dialog, int itemIndex, int id, String textValue, String text2Value, double number1, double number2, double number3, String listValue, String list2Value, long dateValue, int color1, int color2, boolean visible)
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
            @Override @SuppressLint("NotifyDataSetChanged")
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
    private void showAddLocationDialog(ActivityResultLauncher<Intent> launcher)
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
                        LocationService.getCurrentLocation(SettingsActivity.this, false, LocationService.PowerTypes.HighPowerThenBalanced);
                        break;

                    case AddSelectListAdapter.LocationSourceType.Custom:
                    case AddSelectListAdapter.LocationSourceType.Search:
                        //show map and wait for update
                        MapLocationInputActivity.show(SettingsActivity.this, launcher, which == AddSelectListAdapter.LocationSourceType.Search);
                        break;
                }
            }
        });
    }
}
