package com.nikolaiapps.orbtrack;


import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
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
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TimeZone;


public class SettingsActivity extends AppCompatActivity implements PreferenceFragmentCompat.OnPreferenceStartFragmentCallback
{
    static
    {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    public static class SettingsSubFragment extends PreferenceFragmentCompat
    {
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

            screenKey = args.getString("rootKey");
            setPreferencesFromResource(R.xml.settings_main, screenKey);

            if(context != null && screenKey != null)
            {
                switch(screenKey)
                {
                    case "display":
                        SwitchPreference darkThemeSwitch = this.findPreference(Settings.PreferenceName.DarkTheme);
                        IconListPreference colorThemeList = this.findPreference(Settings.PreferenceName.ColorTheme);

                        //initialize values
                        Settings.Options.Display.initValues(context);

                        //setup displays
                        setupSwitch(darkThemeSwitch, null);
                        setupList(colorThemeList, Settings.Options.Display.colorAdvancedItems, null, null, null, null);
                        break;

                    case "lensView":
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

                    case "listView":
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

                    case "mapView":
                        SwitchPreference showCloudsGlobeSwitch = this.findPreference(Settings.PreferenceName.ShowSatelliteClouds + Settings.SubPreferenceName.Globe);
                        SliderPreference globeSensitivitySlider = this.findPreference(Settings.PreferenceName.MapSensitivityScale + Settings.SubPreferenceName.Globe);
                        IconListPreference globeTypeList = this.findPreference(Settings.PreferenceName.MapLayerType + Settings.SubPreferenceName.Globe);

                        //initialize values
                        Settings.Options.MapView.initValues(context);

                        //setup displays
                        setupSwitch(showCloudsGlobeSwitch, null);
                        setupSlider(globeSensitivitySlider);
                        setupList(globeTypeList, Settings.Options.MapView.mapTypeItems, Settings.Options.MapView.MapTypeValues, null, null, showCloudsGlobeSwitch);
                        break;

                    case "updates":
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
                        break;
                }
            }
        }
    }

    public static final String EXTRA_RECREATE = "recreate";

    private static boolean inEditMode = false;
    private static Intent resultIntent = null;

    private byte locationSource;
    private int currentPage;
    private boolean pendingUpdate;
    private boolean needSaveCurrentLocation;
    private String currentPageKey;
    private FrameLayout settingsLayout;
    private FragmentManager manager;
    private MaterialButton backButton;
    private FloatingActionButton floatingButton;
    private AlertDialog addCurrentLocationDialog;
    private LocationReceiver locationReceiver;
    private Settings.PageAdapter settingsPageAdapter;
    private Settings.Locations.ItemListAdapter settingsLocationsListAdapter;
    private Settings.Options.Accounts.ItemListAdapter accountsListAdapter;
    private SharedPreferences preferences;
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;

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

        ActionBar mainActionBar;

        setContentView(R.layout.settings_layout);

        //if result intent does not exist yet
        if(resultIntent == null)
        {
            //create intent
            resultIntent = new Intent();
        }

        //set defaults
        currentPage = -1;
        currentPageKey = null;
        pendingUpdate = false;
        locationSource = Database.LocationType.Current;
        settingsPageAdapter = null;
        settingsLocationsListAdapter = null;
        accountsListAdapter = null;

        //setup fragment manager
        manager = this.getSupportFragmentManager();
        manager.beginTransaction().replace(R.id.Settings_Layout_Fragment, new SettingsMainFragment()).commit();
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

        //setup displays
        settingsLayout = this.findViewById(R.id.Settings_Layout);
        floatingButton = this.findViewById(R.id.Settings_Layout_Floating_Button);
        floatingButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                handleFloatingButtonClick();
            }
        });
        floatingButton.setOnLongClickListener(new View.OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View view)
            {
                handleFloatingButtonClick();
                return(true);
            }
        });
        backButton = this.findViewById(R.id.Settings_Layout_Back_Button);
        backButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if(manager.getBackStackEntryCount() > 0)
                {
                    manager.popBackStack();
                }
                else
                {
                    SettingsActivity.this.finish();
                }
            }
        });

        //set receivers/listeners
        locationReceiver = createLocationReceiver();
        locationReceiver.register(this);
        preferences = Settings.getPreferences(this);
        preferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener()
        {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
            {
                boolean recreateThis = false;

                switch(key)
                {
                    case Settings.PreferenceName.ColorTheme:
                    case Settings.PreferenceName.DarkTheme:
                        recreateThis = true;
                        //fall through

                    case Settings.PreferenceName.MetricUnits:
                        //recreate activity
                        setRecreateNeeded();

                        //if need to recreate this activity
                        if(recreateThis)
                        {
                            SettingsActivity.this.recreate();
                        }
                        break;
                }
            }
        };
        preferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener);

        //if able to get action bar
        mainActionBar = getSupportActionBar();
        if(mainActionBar != null)
        {
            //hide it
            mainActionBar.hide();
        }

        //update display
        updateBackButton();
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        if(pendingUpdate)
        {
            updateList();
            pendingUpdate = false;
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

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        //remove receiver
        locationReceiver.unregister(this);

        //stop listener
        if(preferences != null && preferenceChangeListener != null)
        {
            preferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        boolean isError = true;
        boolean isOkay = (resultCode == RESULT_OK);
        Resources res = this.getResources();

        //handle response
        switch(requestCode)
        {
            case BaseInputActivity.RequestCode.MapLocationInput:
            case BaseInputActivity.RequestCode.EditNotify:
                //if set
                if(isOkay)
                {
                    //update list later
                    pendingUpdate = true;
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
                //if signed in
                if(isOkay)
                {
                    //try to get account
                    Task<GoogleSignInAccount> getAccountTask = GoogleSignIn.getSignedInAccountFromIntent(data);

                    //if got account
                    if(getAccountTask.isSuccessful())
                    {
                        //note: don't confirm internet since would have done already to get past sign in

                        //show google drive file browser
                        Orbitals.showGoogleDriveFileBrowser(this, false);

                        //no error
                        isError = false;
                    }
                }

                //if there was an error
                if(isError)
                {
                    //show error message
                    Globals.showSnackBar(settingsLayout, res.getString(R.string.text_login_failed), true);
                }
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        boolean granted = (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED);
        boolean retrying = (requestCode == Globals.PermissionType.LocationRetry);

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
        }
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
            //update current page
            switch(currentPageKey)
            {
                case "accounts":
                    currentPage = Settings.PageType.Accounts;
                    break;

                case "locations":
                    currentPage = Settings.PageType.Locations;
                    break;

                case "notifications":
                    currentPage = Settings.PageType.Notifications;
                    break;
            }

            //if adapter not set yet
            if(settingsPageAdapter == null)
            {
                //create adapter
                settingsPageAdapter = new Settings.PageAdapter(manager, settingsLayout, null, createOnItemCheckChangedListener(), createOnSetAdapterListener(), createOnUpdateNeededListener(), null, createOnPageResumeListener(), new int[Settings.PageType.PageCount]);
            }

            //create page fragment
            fragment = settingsPageAdapter.getItem(MainActivity.Groups.Settings, currentPage, 0, new Settings.Page(pref.getTitle().toString()));
        }
        else
        {
            //update current page
            currentPage = -1;

            //create preference fragment
            fragment = manager.getFragmentFactory().instantiate(getClassLoader(), fragmentClass);

            //set arguments
            args.putString("rootKey", currentPageKey);
            fragment.setArguments(args);
        }

        //add fragment
        fragment.setTargetFragment(caller, 0);
        manager.beginTransaction().replace(R.id.Settings_Layout_Fragment, fragment, (isPage ? currentPageKey : null)).addToBackStack(null).commit();

        return(true);
    }

    //Handles main floating button click
    private void handleFloatingButtonClick()
    {
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

                        //if current location
                        if(locationSource == Database.LocationType.Current)
                        {
                            //show status and update location
                            Globals.showSnackBar(settingsLayout, context.getResources().getString(R.string.title_location_getting));
                            LocationService.getCurrentLocation(SettingsActivity.this, false, true, LocationService.PowerTypes.HighPowerThenBalanced);
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
            public void setAdapter(final int group, final int position, RecyclerView.Adapter<RecyclerView.ViewHolder> adapter)
            {
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

                    case Settings.PageType.Other:
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

                    //if adapter is set and is the widget list
                    if(listAdapter instanceof Settings.Widgets.ItemListAdapter)
                    {
                        //reload widget list
                        ((Settings.Widgets.ItemListAdapter)listAdapter).reload();
                    }
                }
            }
        });
    }

    //Creates an on color button click listener
    private static View.OnClickListener createOnColorButtonClickListener(final Context context, final String preferenceKey, final int titleId, final int startColor, boolean allowOpacity, SharedPreferences readSettings, SharedPreferences.Editor writeSettings)
    {
        return(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
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

    //Sets up a list
    private static void setupList(IconListPreference preference, Object[] items, Object[] values, int[] itemImageIds, String[] subTexts, Preference childPreference)
    {
        //if preference exists
        if(preference != null)
        {
            final Context context = preference.getContext();
            final String preferenceKey = preference.getKey();
            int valueIndex = -1;
            Object currentValue = null;

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
                    currentValue = Settings.getMapLayerType(context, true);

                    preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
                    {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue)
                        {
                            int layerType = (int)newValue;

                            //update child visibility
                            childPreference.setVisible(layerType == CoordinatesFragment.MapLayerType.Satellite || layerType == CoordinatesFragment.MapLayerType.Hybrid);

                            //allow change
                            return(true);
                        }
                    });
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
    }

    //Sets up up a preference switch
    private static void setupSwitch(SwitchPreference preference, TimeIntervalPreference timePreference)
    {
        //if preference exists
        if(preference != null)
        {
            final Context context = preference.getContext();
            final SharedPreferences readSettings = Settings.getPreferences(context);
            final SharedPreferences.Editor writeSettings = Settings.getWriteSettings(context);
            final String preferenceKey = preference.getKey();
            final boolean checked = readSettings.getBoolean(preferenceKey, false);

            //set state and listener
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
                        if(preferenceKey.equals(Settings.PreferenceName.SatelliteSourceUseGP))
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
    }

    //Sets up a slider preference
    private static void setupSlider(SliderPreference preference)
    {
        //if preference exists
        if(preference != null)
        {
            final Context context = preference.getContext();
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
            final SharedPreferences readSettings = Settings.getPreferences(context);
            final SharedPreferences.Editor writeSettings = Settings.getWriteSettings(context);
            final String preferenceKey = preference.getKey();

            //if lens horizon toggle
            if(preferenceKey.equals(Settings.PreferenceName.LensUseHorizon))
            {
                BorderButton switchButton = new BorderButton(new ContextThemeWrapper(context, R.style.ColorButton), null);
                float[] size = Globals.dpsToPixels(context, 60, 40);
                ViewGroup.LayoutParams params = new ViewGroup.LayoutParams((int)size[0], (int)size[1]);

                switchButton.setBackgroundColor(Settings.getLensHorizonColor(context));
                switchButton.setLayoutParams(params);
                preference.setButton(switchButton);
                preference.setButtonOnClickListener(createOnColorButtonClickListener(context, Settings.PreferenceName.LensHorizonColor, R.string.title_horizon_color, Settings.getLensHorizonColor(context), false, readSettings, writeSettings));
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

    //Gets the desired settings page
    private Settings.Page getSettingsPage(String fragmentName)
    {
        Fragment page = manager.findFragmentByTag(fragmentName);
        return(page != null ? (Settings.Page)page : new Settings.Page(null));
    }

    //Update list
    private void updateList()
    {
        Fragment currentFragment = manager.findFragmentByTag(currentPageKey);
        FragmentTransaction currentTransaction;

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

            case Settings.PageType.Other:
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
        //backButton.setText(currentPage == -1 ? R.string.title_close : R.string.title_back);
        backButton.setText(manager.getBackStackEntryCount() > 0 ? R.string.title_back : R.string.title_close);
    }

    //Sets recreate needed
    private void setRecreateNeeded()
    {
        resultIntent.putExtra(EXTRA_RECREATE, true);
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
                                        Settings.removeLogin(SettingsActivity.this, which);
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
        ArrayList<Selectable.ListItem> selectedItems = getSettingsPage(currentPageKey).getSelectedItems();
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
                    setRecreateNeeded();
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
                getSettingsPage(currentPageKey).cancelEditMode();
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
