package com.nikolaiapps.orbtrack;


import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.os.SystemClock;
import androidx.annotation.NonNull;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.fragment.app.FragmentManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.core.view.GravityCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.viewpager.widget.PagerAdapter;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerTitleStrip;
import androidx.viewpager.widget.ViewPager;
import android.text.Spanned;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.Task;
import com.nikolaiapps.orbtrack.SideMenuListAdapter.*;
import com.nikolaiapps.orbtrack.Calculations.*;


public class MainActivity extends AppCompatActivity
{
    static
    {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    public static abstract class Groups
    {
        static final int Current = 0;
        static final int Calculate = 1;
        static final int Orbitals = 2;
        static final int Settings = 3;
    }

    public static abstract class ParamTypes
    {
        static final String MainGroup = "mainGroup";
        static final String CurrentSubPage = "currentSubPage";
        static final String CalculatePageInputs = "calcPageInputs";
        static final String CalculatePageSubInputs = "calcPageSubInputs";
        static final String CalculateSubPage = "calcSubPage";
        static final String SettingsSubPage = "settingsSubPage";
        static final String MainPagerItem = "mainPagerItem";
        static final String PassIndex = "passIndex";
        static final String GetPassItems = "getPassItems";
        static final String CoordinateNoradId = "coordinateNoradId";
        static final String ForceShowPaths = "forceShowPaths";
        static final String PathDivisions = "pathDivisions";
        static final String SavedState = "savedState";
    }

    //Status
    private static boolean inEditMode;
    private static boolean wasPaused = false;
    private static boolean runningUserSetup = false;
    private boolean savedState;
    private boolean finishedSetup;
    private Bundle calculateBundle;
    private Bundle savedStateBundle;
    //
    //Displays
    private Menu optionsMenu;
    private View sideActionDivider;
    private RecyclerView sideActionMenu;
    private SwipeStateViewPager mainPager;
    private PagerTitleStrip mainPagerTitles;
    private DrawerLayout mainDrawerLayout;
    private ActionBarDrawerToggle mainDrawerToggle;
    private MultiProgressDialog databaseProgress;
    private FloatingActionButton mainFloatingButton;
    //
    //Display adapters
    private Current.PageAdapter currentPageAdapter;
    private Calculate.PageAdapter calculatePageAdapter;
    private Orbitals.PageAdapter orbitalPageAdapter;
    private Settings.Locations.ItemListAdapter settingsLocationsListAdapter;
    private Settings.Options.Accounts.ItemListAdapter accountsListAdapter;
    private Settings.PageAdapter settingsPagerAdapter;
    //
    //Current
    private int mainGroup;
    private int previousGroup;
    private int previousPage;
    private int previousSubPage;
    private static int viewLensNoradID = Integer.MAX_VALUE;
    private static int passesLensNoradID = Integer.MAX_VALUE;
    public static int mapViewNoradID = Integer.MAX_VALUE;
    private static int passesPassIndex = Integer.MAX_VALUE;
    private static int intersectionPassIndex = Integer.MAX_VALUE;
    private static int lastSideMenuPosition = -1;
    private int[] currentSubPage;
    private int[] calculateSubPage;
    private int[] settingsSubPage;
    private long timerDelay;
    private long listTimerDelay;
    private long lensTimerDelay;
    private boolean wasDraggingPage = false;
    private ThreadTask<Void, Void, Void> timerTask;
    private Runnable timerRunnable;
    private Calendar backPressTime;
    private Globals.PendingFile pendingSaveFile;
    private CalculateViewsTask currentViewAnglesTask;
    private CalculateViewsTask calculateViewAnglesTask;
    private CalculateCoordinatesTask calculateCoordinatesTask;
    private CalculateService.CalculatePathsTask currentPassesTask;
    private CalculateService.CalculatePathsTask calculatePassesTask;
    private CalculateService.CalculatePathsTask calculateIntersectionsTask;
    private Current.Coordinates.CalculatePathTask currentCoordinatesTask;
    private static Database.SatelliteData[] currentSatellites = new Database.SatelliteData[0];
    //
    //Location
    private byte locationSource;
    private boolean pendingLocationUpdate;
    private boolean needSaveCurrentLocation;
    private boolean userChangedLocation;
    private static ObserverType observer;
    private AlertDialog addCurrentLocationDialog;
    //
    //Listeners
    private static Calculate.OnStartCalculationListener startCalculationListener;
    private static Selectable.ListFragment.OnEditModeChangedListener editModeChangedListener;
    private SharedPreferences preferences;
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;
    //
    //Receivers
    private LocationReceiver locationReceiver;
    private UpdateReceiver localUpdateReceiver;
    //

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        int index;
        Resources res = this.getResources();
        final Bundle stateCopy;

        Settings.Options.Display.setTheme(this);
        this.setContentView(R.layout.main_layout);

        //look for web protocol updates
        Globals.updateWebProtocols(this);

        //set defaults
        if(savedInstanceState == null)
        {
            savedInstanceState = new Bundle();
        }
        savedState = savedInstanceState.getBoolean(ParamTypes.SavedState, false);
        savedStateBundle = savedInstanceState;
        mainGroup = savedInstanceState.getInt(ParamTypes.MainGroup, Groups.Current);
        previousGroup = previousPage = previousSubPage = -1;
        inEditMode = pendingLocationUpdate = needSaveCurrentLocation = userChangedLocation = false;
        observer = null;
        Settings.setMetricUnits(this, Settings.getMetricUnits(this));
        Settings.setMapMarkerInfoLocation(this, Settings.getMapMarkerInfoLocation(this));
        Settings.setUsingCurrentGridLayout(this, Settings.getCurrentGridLayout(this));
        pendingSaveFile = new Globals.PendingFile(-1, "", "", -1, -1, "");
        backPressTime = Globals.getGMTTime();
        backPressTime.set(Calendar.YEAR, 0);

        //set default sub pages
        currentSubPage = new int[Current.PageType.PageCount];
        for(index = 0; index < currentSubPage.length; index++)
        {
            currentSubPage[index] = savedInstanceState.getInt(ParamTypes.CurrentSubPage + index, Current.SubPageType.List);
        }
        calculateSubPage = new int[Calculate.PageType.PageCount];
        for(index = 0; index < calculateSubPage.length; index++)
        {
            calculateSubPage[index] = savedInstanceState.getInt(ParamTypes.CalculateSubPage + index, Calculate.SubPageType.Input);
        }
        settingsSubPage = new int[Settings.PageType.PageCount];
        settingsSubPage[Settings.PageType.Other] = savedInstanceState.getInt(ParamTypes.SettingsSubPage + Settings.PageType.Other, Settings.SubPageType.None);

        //setup bundles
        calculateBundle = new Bundle();

        //setup displays
        mainDrawerLayout = this.findViewById(R.id.Main_Drawer_Layout);
        mainDrawerToggle = createActionBarDrawerToggle();
        mainDrawerLayout.addDrawerListener(mainDrawerToggle);
        mainPager = this.findViewById(R.id.Main_Pager);
        mainPagerTitles = this.findViewById(R.id.Main_Pager_Titles);

        //create receivers
        startCalculationListener = createOnStartCalculationListener();
        localUpdateReceiver = createUpdateReceiver(localUpdateReceiver);
        preferences = Settings.getPreferences(this);
        preferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener()
        {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
            {
                switch(key)
                {
                    case Settings.PreferenceName.DarkTheme:
                    case Settings.PreferenceName.ColorTheme:
                        updateTheme();
                        break;

                    case Settings.PreferenceName.UseCombinedCurrentLayout:
                        updateSideMenu();
                        break;
                }
            }
        };
        preferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener);

        //if privacy policy has been accepted
        stateCopy = savedInstanceState;
        if(Settings.getAcceptedPrivacy(this))
        {
            //handle any first run
            handleFirstRun(stateCopy);
        }
        else
        {
            //ask for acceptance
            Globals.showConfirmDialog(this, res.getString(R.string.title_privacy_policy), res.getText(R.string.desc_privacy_policy), res.getString(R.string.title_accept), res.getString(R.string.title_deny), false, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    //set privacy policy as accepted
                    Settings.setAcceptedPrivacy(MainActivity.this, true);

                    //handle any first run
                    handleFirstRun(stateCopy);
                }
            }, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    //denied privacy policy
                    MainActivity.this.finish();
                }
            });
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        int index;

        //save status
        outState.putBoolean(ParamTypes.SavedState, true);
        outState.putInt(ParamTypes.MainGroup, mainGroup);
        for(index = 0; index < Current.PageType.PageCount; index++)
        {
            outState.putInt(ParamTypes.CurrentSubPage + index, currentSubPage[index]);
        }
        outState.putInt(ParamTypes.SettingsSubPage + Settings.PageType.Other, settingsSubPage[Settings.PageType.Other]);
        outState.putInt(ParamTypes.MainPagerItem, getMainPage());

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        //stop tasks
        wasPaused = true;
        updateRunningTasks(false);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        //if were paused
        if(wasPaused)
        {
            //restore tasks
            updateRunningTasks(true);
        }

        //reset
        wasPaused = false;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState)
    {
        super.onPostCreate(savedInstanceState);
        mainDrawerToggle.syncState();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);

        if(locationReceiver != null)
        {
            manager.unregisterReceiver(locationReceiver);
        }
        if(localUpdateReceiver != null)
        {
            manager.unregisterReceiver(localUpdateReceiver);
        }

        //stop listener
        if(preferences != null && preferenceChangeListener != null)
        {
            preferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
        }

        //if progress display is visible
        if(databaseProgress != null && databaseProgress.isShowing())
        {
            //close it
            databaseProgress.dismiss();
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);

        boolean restartCamera = false;
        int index;
        int page = getMainPage();

        //update drawer
        mainDrawerToggle.onConfigurationChanged(newConfig);

        //handle based on group
        switch(mainGroup)
        {
            case Groups.Current:
                switch(page)
                {
                    case Current.PageType.View:
                    case Current.PageType.Passes:
                        restartCamera = (currentSubPage[page] == Current.SubPageType.Lens);
                        break;
                }

                //update columns on all pages
                for(index = 0; index < Current.PageType.PageCount; index++)
                {
                    Current.PageAdapter.notifyOrientationChangedListener(index);
                }
                break;

            case Groups.Calculate:
                switch(page)
                {
                    case Calculate.PageType.View:
                    case Calculate.PageType.Passes:
                    case Calculate.PageType.Intersection:
                        restartCamera = (calculateSubPage[page] == Calculate.SubPageType.Lens);
                        break;
                }

                //update columns on all pages
                for(index = 0; index < Current.PageType.PageCount; index++)
                {
                    Calculate.PageAdapter.notifyOrientationChangedListener(index);
                }
                break;
        }

        //if need to restart camera and it is set
        if(restartCamera && Current.cameraView != null)
        {
            //update orientation and restart
            Current.cameraView.updateOrientation();
            Current.cameraView.startCamera();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        int id;
        int progressType = Globals.ProgressType.Finished;
        long count;
        boolean isError = true;
        boolean isOkay = (resultCode == RESULT_OK);
        boolean handle = (requestCode == BaseInputActivity.RequestCode.Setup || finishedSetup);
        String message;
        Resources res = this.getResources();
        ArrayList<String> fileNames;
        ArrayList<String> filesData;

        //if not handling
        if(!handle)
        {
            //stop
            return;
        }

        //if no data
        if(data == null)
        {
            //set to empty
            data = new Intent();
        }

        //handle based on request code
        switch(requestCode)
        {
            case BaseInputActivity.RequestCode.Setup:
                //update status
                runningUserSetup = false;
                finishedSetup = !Settings.getFirstRun(this);

                //if finished updating database
                if(finishedSetup)
                {
                    //finish loading
                    loadStartData(savedStateBundle);
                }
                else
                {
                    //show progress dialog
                    showDatabaseProgressDialog(true);
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
                        updateMainPager(false);
                    }
                }
                break;

            case BaseInputActivity.RequestCode.EditNotify:
                //if set
                if(isOkay)
                {
                    //update list
                    updateMainPager(false);
                }
                break;

            case BaseInputActivity.RequestCode.MasterAddList:
                //get progress type
                progressType = data.getIntExtra(MasterAddListActivity.ParamTypes.ProgressType, Globals.ProgressType.Unknown);

                //if unknown
                if(progressType == Globals.ProgressType.Unknown)
                {
                    //stop
                    break;
                }

                //handle based on progress type
                switch(progressType)
                {
                    case Globals.ProgressType.Denied:
                        //show login failed
                        message = res.getString(R.string.text_login_failed);
                        break;

                    case Globals.ProgressType.Cancelled:
                        //show cancelled
                        message = res.getString(R.string.text_update_cancelled);
                        break;

                    case Globals.ProgressType.Failed:
                        //show failed
                        message = res.getString(R.string.text_download_failed);
                        break;

                    default:
                        //get and show count
                        count = data.getLongExtra(MasterAddListActivity.ParamTypes.SuccessCount, 0);
                        message = res.getQuantityString(R.plurals.title_satellites_added, (int)count, (int)count);
                        isError = (count < 1);
                        break;
                }

                //show message
                Globals.showSnackBar(mainDrawerLayout, message, isError);
                //fall through

            case BaseInputActivity.RequestCode.OrbitalViewList:
            case BaseInputActivity.RequestCode.ManualOrbitalInput:
                //if set and weren't denied
                if(isOkay && progressType != Globals.ProgressType.Denied)
                {
                    //update list without saved items
                    updateMainPager(true, false);
                }
                break;

            case BaseInputActivity.RequestCode.MapLocationInput:
                //if set
                if(isOkay)
                {
                    //update list and observer
                    updateMainPager(false);
                    loadObserver(true);
                }
                break;

            case BaseInputActivity.RequestCode.SDCardOpenItem:
                //if set
                if(isOkay)
                {
                    //load from file if not already
                    fileNames = data.getStringArrayListExtra(FileBrowserBaseActivity.ParamTypes.FileNames);
                    if(!UpdateService.isRunning(UpdateService.UpdateType.LoadFile))
                    {
                        UpdateService.loadFile(this, fileNames);
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
                    Globals.showSnackBar(mainDrawerLayout, res.getString(R.string.text_login_failed), true);
                }
                break;

            case BaseInputActivity.RequestCode.GoogleDriveOpenFolder:
            case BaseInputActivity.RequestCode.DropboxOpenFolder:
                //if selected folder
                if(isOkay)
                {
                    savePendingFile(true);
                }
                break;

            case BaseInputActivity.RequestCode.GoogleDriveOpenFile:
            case BaseInputActivity.RequestCode.DropboxOpenFile:
                //if selected item
                if(isOkay)
                {
                    //load from file if not already
                    filesData = data.getStringArrayListExtra(FileBrowserBaseActivity.ParamTypes.FilesData);
                    if(!UpdateService.isRunning(UpdateService.UpdateType.LoadFile))
                    {
                        UpdateService.loadFileData(this, filesData);
                    }
                }
                break;

            case BaseInputActivity.RequestCode.GoogleDriveSave:
            case BaseInputActivity.RequestCode.DropboxSave:
                //if saved
                if(isOkay)
                {
                    //if for calculation
                    if(mainGroup == Groups.Calculate)
                    {
                        //show success
                        Globals.showSnackBar(mainDrawerLayout, res.getString(R.string.text_file_saved), false);
                    }
                    else
                    {
                        //get item count and show success
                        count = data.getIntExtra(FileBrowserBaseActivity.ParamTypes.ItemCount, 0);
                        Globals.showSnackBar(mainDrawerLayout, res.getQuantityString(R.plurals.title_satellites_saved, (int)count, (int)count), (count < 1));
                    }
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
            case Globals.PermissionType.CameraRetry:
            case Globals.PermissionType.ReadExternalStorageRetry:
            case Globals.PermissionType.WriteExternalStorageRetry:
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

            case Globals.PermissionType.Camera:
            case Globals.PermissionType.CameraRetry:
                switch(mainGroup)
                {
                    case Groups.Current:
                    case Groups.Calculate:
                        //if view exists
                        if(Current.cameraView != null)
                        {
                            //if granted
                            if(granted)
                            {
                                //try to start camera
                                Current.cameraView.startCamera();
                            }
                            //else if not retrying
                            else if(!retrying)
                            {
                                //ask permission again
                                Globals.askCameraPermission(this, true, new Globals.OnDenyListener()
                                {
                                    @Override
                                    public void OnDeny(byte resultCode)
                                    {
                                        //try to start camera without all permissions
                                        Current.cameraView.startCamera();
                                    }
                                });
                            }
                        }
                        break;
                }
                break;

            case Globals.PermissionType.ReadExternalStorage:
            case Globals.PermissionType.ReadExternalStorageRetry:
                //if granted
                if(granted)
                {
                    Orbitals.showSDCardFileBrowser(this, mainDrawerLayout);
                }
                //else if not retrying
                else if(!retrying)
                {
                    //ask permission again
                    Globals.askReadPermission(this, true);
                }
                break;

            case Globals.PermissionType.WriteExternalStorage:
            case Globals.PermissionType.WriteExternalStorageRetry:
                //if granted
                if(granted)
                {
                    //save pending file
                    savePendingFile();
                }
                //else if not retrying
                else if(!retrying)
                {
                    //ask permission again
                    Globals.askWritePermission(this, true);
                }
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        //create options menu
        getMenuInflater().inflate(R.menu.menu_main_layout, menu);
        optionsMenu = menu;
        return(super.onCreateOptionsMenu(menu));
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        int page = getMainPage();
        boolean onCurrent = (mainGroup == Groups.Current);
        boolean onCalculate = (mainGroup == Groups.Calculate);
        boolean onOrbitals = (mainGroup == Groups.Orbitals);
        boolean onSettings = (mainGroup == Groups.Settings);
        int subPage = getSubPage();
        int mapDisplayType = Settings.getMapDisplayType(this);
        boolean usingCurrentViewGrid = Settings.getUsingCurrentGridLayout(Current.PageType.View);
        boolean usingCurrentPassesGrid = Settings.getUsingCurrentGridLayout(Current.PageType.Passes);
        boolean usingCurrentCoordinatesGrid = Settings.getUsingCurrentGridLayout(Current.PageType.Coordinates);
        boolean usingMapDisplay = (mapDisplayType == CoordinatesFragment.MapDisplayType.Map);
        boolean usingGlobeDisplay = (mapDisplayType == CoordinatesFragment.MapDisplayType.Globe);
        boolean onCurrentList = (subPage == Current.SubPageType.List);
        boolean onCurrentLens = (subPage == Current.SubPageType.Lens);
        boolean onCurrentMap = (subPage == Current.SubPageType.Map);
        boolean onCurrentGlobe = (subPage == Current.SubPageType.Globe);
        boolean onCurrentView = (onCurrent && page == Current.PageType.View);
        boolean onCurrentViewList = (onCurrentView && onCurrentList);
        boolean onCurrentPasses = (onCurrent && page == Current.PageType.Passes);
        boolean onCurrentPassesList = (onCurrentPasses && onCurrentList);
        boolean onCurrentCoordinates = (onCurrent && page == Current.PageType.Coordinates);
        boolean onCurrentCoordinatesList = (onCurrentCoordinates && onCurrentList);
        boolean onCurrentCoordinateMap = (onCurrentCoordinates && onCurrentMap);
        boolean onCurrentCoordinateGlobe = (onCurrentCoordinates && onCurrentGlobe);
        boolean onCurrentCombined = (onCurrent && page == Current.PageType.Combined);
        boolean onCurrentCombinedList = (onCurrentCombined && onCurrentList);
        boolean onCurrentCombinedMap = (onCurrentCombined && onCurrentMap);
        boolean onCurrentCombinedGlobe = (onCurrentCombined && onCurrentGlobe);
        boolean onCalculateList = (subPage == Calculate.SubPageType.List);
        boolean onCalculateLens = (subPage == Calculate.SubPageType.Lens);
        boolean onCalculateView = (onCalculate && page == Calculate.PageType.View);
        boolean onCalculatePasses = (onCalculate && page == Calculate.PageType.Passes);
        boolean onCalculateCoordinates = (onCalculate && page == Calculate.PageType.Coordinates);
        boolean onCalculateIntersection = (onCalculate && page == Calculate.PageType.Intersection);
        boolean onCalculateViewList = (onCalculateView && onCalculateList);
        boolean onCalculateViewLens = (onCalculateView && onCalculateLens);
        boolean onCalculatePassesList = (onCalculatePasses && onCalculateList);
        boolean onCalculatePassesLens = (onCalculatePasses && onCalculateLens);
        boolean onCalculateCoordinatesList = (onCalculateCoordinates && onCalculateList);
        boolean onCalculateCoordinatesMap = (onCalculateCoordinates && subPage == Calculate.SubPageType.Map);
        boolean onCalculateCoordinatesGlobe = (onCalculateCoordinates && subPage == Calculate.SubPageType.Globe);
        boolean onCalculateIntersectionList = (onCalculateIntersection && onCalculateList);
        boolean onCalculateIntersectionLens = (onCalculateIntersection && onCalculateLens);
        boolean onOrbitalSatellites = (onOrbitals && page == Orbitals.PageType.Satellites);
        boolean onSettingsOtherSub = (onSettings && page == Settings.PageType.Other && settingsSubPage[Settings.PageType.Other] != Settings.SubPageType.None);
        boolean haveSatellites = (onOrbitalSatellites && orbitalPageAdapter != null && orbitalPageAdapter.getPageItemCount(mainPager, Orbitals.PageType.Satellites) > 0);
        boolean onOrbitalSatellitesExistNoModify = (onOrbitalSatellites && haveSatellites && !UpdateService.modifyingSatellites());
        boolean calculatingViews = (calculateViewAnglesTask != null && calculateViewAnglesTask.isRunning());
        boolean calculatingPasses = (calculatePassesTask != null && calculatePassesTask.isRunning());
        boolean calculatingCoordinates = (calculateCoordinatesTask != null && calculateCoordinatesTask.calculatingCoordinates);
        boolean calculatingIntersection = (calculateIntersectionsTask != null && calculateIntersectionsTask.isRunning());
        boolean showLens = onCurrentViewList || onCurrentPassesList || onCurrentCombinedList || (onCalculateViewList && !calculatingViews);
        boolean showList = ((onCurrentView && onCurrentViewList == usingCurrentViewGrid) || (onCurrentPasses && onCurrentPassesList == usingCurrentPassesGrid) || (onCurrentCoordinates && onCurrentCoordinatesList == usingCurrentCoordinatesGrid)) || (onCurrentCombined && !onCurrentList) || onCalculateViewLens || onCalculatePassesLens || onCalculateCoordinatesMap || onCalculateIntersectionLens || onCalculateCoordinatesGlobe;
        boolean showGrid = ((onCurrentView && onCurrentViewList != usingCurrentViewGrid) || (onCurrentPasses && onCurrentPassesList != usingCurrentPassesGrid) || (onCurrentCoordinates && onCurrentCoordinatesList != usingCurrentCoordinatesGrid));
        boolean showMap = (onCurrentCoordinates && ((usingMapDisplay && onCurrentCoordinatesList) || onCurrentCoordinateGlobe)) || (onCurrentCombined && ((usingMapDisplay && onCurrentCombinedList) || onCurrentCombinedGlobe)) || (!calculatingCoordinates && ((onCalculateCoordinatesList && usingMapDisplay) || onCalculateCoordinatesGlobe));
        boolean showGlobe = (onCurrentCoordinates && ((usingGlobeDisplay && onCurrentCoordinatesList) || onCurrentCoordinateMap)) || (onCurrentCombined && ((usingGlobeDisplay && onCurrentCombinedList) || onCurrentCombinedMap)) || (!calculatingCoordinates && ((onCalculateCoordinatesList && usingGlobeDisplay) || onCalculateCoordinatesMap));
        boolean onCurrentNoSelected = ((onCurrentView && viewLensNoradID == Integer.MAX_VALUE) || (onCurrentPasses && passesLensNoradID == Integer.MAX_VALUE) || (onCurrentCoordinates && mapViewNoradID == Integer.MAX_VALUE) || (onCurrentCombined && viewLensNoradID == Integer.MAX_VALUE && passesLensNoradID == Integer.MAX_VALUE && mapViewNoradID == Integer.MAX_VALUE));
        boolean showSave = ((onCalculateViewList && !calculatingViews) || (onCalculatePassesList && !calculatingPasses) || (onCalculateCoordinatesList && !calculatingCoordinates) || (onCalculateIntersectionList && !calculatingIntersection) || onOrbitalSatellitesExistNoModify);

        menu.findItem(R.id.menu_back).setVisible(onSettingsOtherSub);
        menu.findItem(R.id.menu_list).setVisible(showList);
        menu.findItem(R.id.menu_grid).setVisible(showGrid);
        menu.findItem(R.id.menu_map).setVisible(showMap);
        menu.findItem(R.id.menu_globe).setVisible(showGlobe);
        menu.findItem(R.id.menu_lens).setVisible(showLens && SensorUpdate.havePositionSensors(this));
        menu.findItem(R.id.menu_filter).setVisible(onCurrentNoSelected && onCurrentList);
        menu.findItem(R.id.menu_settings).setVisible(onCurrentNoSelected && (onCurrentLens || onCurrentMap || onCurrentGlobe));
        menu.findItem(R.id.menu_edit).setVisible((showSave && !onOrbitalSatellites) || onCalculateViewLens || onCalculatePassesLens || onCalculateCoordinatesMap || onCalculateIntersectionLens || onCalculateCoordinatesGlobe);
        menu.findItem(R.id.menu_save).setVisible(showSave);
        menu.findItem(R.id.menu_update).setVisible(onOrbitalSatellitesExistNoModify);

        return(super.onPrepareOptionsMenu(menu));
    }

    //Updates the options menu
    private void updateOptionsMenu(boolean updateFloatingButton)
    {
        //if menu exists
        if(optionsMenu != null)
        {
            //refresh menu
            onPrepareOptionsMenu(optionsMenu);
        }

        //if updating floating button
        if(updateFloatingButton)
        {
            updateMainFloatingButton();
        }
    }
    private void updateOptionsMenu()
    {
        updateOptionsMenu(true);
    }

    //Updates the main floating button
    private void updateMainFloatingButton()
    {
        int imageID = R.drawable.ic_add_white;
        int page = getMainPage();
        boolean show = false;

        switch(mainGroup)
        {
            case Groups.Orbitals:
                switch(page)
                {
                    case Orbitals.PageType.Satellites:
                        show = !UpdateService.modifyingSatellites();
                        if(inEditMode)
                        {
                            imageID = R.drawable.ic_mode_edit_white;
                        }
                        break;
                }
                break;

            case Groups.Settings:
                switch(page)
                {
                    case Settings.PageType.Locations:
                    case Settings.PageType.Notifications:
                        show = true;
                        if(inEditMode)
                        {
                            imageID = R.drawable.ic_mode_edit_white;
                        }
                        break;

                    case Settings.PageType.Other:
                        switch(settingsSubPage[page])
                        {
                            case Settings.SubPageType.Accounts:
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
                        }
                        break;
                }
                break;
        }

        //update visibility
        if(mainFloatingButton != null)
        {
            mainFloatingButton.setImageResource(imageID);
        }
        Globals.setVisible(mainFloatingButton, show);
    }

    private void updateTheme()
    {
        Settings.Options.Display.setTheme(this);
        this.recreate();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item)
    {
        int id = item.getItemId();
        int previousId;
        int previousSubPage;
        int selectedSubPage;
        int page = getMainPage();
        boolean setDisplayGroup = false;
        boolean usingGrid = false;
        boolean usingGlobe;

        //if drawer handles
        if(mainDrawerToggle.onOptionsItemSelected(item))
        {
            return(true);
        }

        //get status based on group
        switch(mainGroup)
        {
            case Groups.Current:
                //if not combined
                if(page != Current.PageType.Combined)
                {
                    usingGrid = Settings.getUsingCurrentGridLayout(page);
                }
                break;
        }

        //handle item
        switch(id)
        {
            case R.id.menu_back:
                switch(mainGroup)
                {
                    case Groups.Settings:
                        //if went back to previous page
                        if(showPreviousPage())
                        {
                            //done
                            return(true);
                        }

                        setSubPage(Groups.Settings, page, Settings.SubPageType.None);
                        setDisplayGroup = true;
                        break;
                }
                break;

            case R.id.menu_list:
            case R.id.menu_grid:
                switch(mainGroup)
                {
                    case Groups.Current:
                        switch(page)
                        {
                            case Current.PageType.View:
                            case Current.PageType.Passes:
                            case Current.PageType.Coordinates:
                            case Current.PageType.Combined:
                                //if --using grid and list menu or- -not using grid and grid menu-- and on list
                                if(((usingGrid && id == R.id.menu_list) || (!usingGrid && id == R.id.menu_grid)) && currentSubPage[page] == Current.SubPageType.List)
                                {
                                    //stop using grid
                                    Settings.setUsingCurrentGridLayout(this, (id == R.id.menu_grid));
                                }
                                else
                                {
                                    //set sub page
                                    setSubPage(Groups.Current, page, Current.SubPageType.List);
                                }

                                //update display
                                setDisplayGroup = true;
                                break;
                        }
                        break;

                    case Groups.Calculate:
                        switch(page)
                        {
                            case Calculate.PageType.View:
                            case Calculate.PageType.Passes:
                            case Calculate.PageType.Coordinates:
                            case Calculate.PageType.Intersection:
                                setSubPage(Groups.Calculate, page, Calculate.SubPageType.List);
                                setDisplayGroup = true;
                                break;
                        }
                        break;
                }
                break;

            case R.id.menu_map:
            case R.id.menu_globe:
                //if not waiting on a location update
                if(!pendingLocationUpdate)
                {
                    usingGlobe = (id == R.id.menu_globe);

                    switch(mainGroup)
                    {
                        case Groups.Current:
                            switch(page)
                            {
                                case Current.PageType.Coordinates:
                                case Current.PageType.Combined:
                                    //remember previous ID and page
                                    previousId = mapViewNoradID;
                                    previousSubPage = currentSubPage[page];

                                    //update sub page
                                    setSubPage(mainGroup, page, (usingGlobe ? Current.SubPageType.Globe : Current.SubPageType.Map));
                                    selectedSubPage = currentSubPage[page];

                                    //if switching between globe/map
                                    if((previousSubPage == Current.SubPageType.Map && selectedSubPage == Current.SubPageType.Globe) || (previousSubPage == Current.SubPageType.Globe && selectedSubPage == Current.SubPageType.Map))
                                    {
                                        //restore ID
                                        mapViewNoradID = previousId;
                                    }

                                    //update default display
                                    Settings.setMapDisplayType(this, (usingGlobe ? CoordinatesFragment.MapDisplayType.Globe : CoordinatesFragment.MapDisplayType.Map));

                                    //continue updating
                                    setDisplayGroup = true;
                                    break;
                            }
                            break;

                        case Groups.Calculate:
                            switch(page)
                            {
                                case Current.PageType.Coordinates:
                                    //update sub page
                                    setSubPage(mainGroup, page, (usingGlobe ? Calculate.SubPageType.Globe : Calculate.SubPageType.Map));

                                    //update default display
                                    Settings.setMapDisplayType(this, (usingGlobe ? CoordinatesFragment.MapDisplayType.Globe : CoordinatesFragment.MapDisplayType.Map));

                                    //continue updating
                                    setDisplayGroup = true;
                                    break;
                            }
                            break;
                    }
                }
                else
                {
                    //show pending location display
                    showLocationGettingDisplay();
                }
                break;

            case R.id.menu_lens:
                //if not waiting on a location update
                if(!pendingLocationUpdate)
                {
                    switch(mainGroup)
                    {
                        case Groups.Current:
                            switch(page)
                            {
                                case Current.PageType.View:
                                case Current.PageType.Passes:
                                case Current.PageType.Combined:
                                    setSubPage(mainGroup, page, Current.SubPageType.Lens);
                                    setDisplayGroup = true;
                                    break;
                            }
                            break;

                        case Groups.Calculate:
                            switch(page)
                            {
                                case Calculate.PageType.View:
                                case Calculate.PageType.Passes:
                                case Calculate.PageType.Intersection:
                                    setSubPage(mainGroup, page, Calculate.SubPageType.Lens);
                                    setDisplayGroup = true;
                                    break;
                            }
                            break;
                    }
                }
                else
                {
                    //show pending location display
                    showLocationGettingDisplay();
                }
                break;

            case R.id.menu_filter:
                switch(mainGroup)
                {
                    case Groups.Current:
                        showFilterDialog();
                        break;
                }
                break;

            case R.id.menu_settings:
                switch(mainGroup)
                {
                    case Groups.Current:
                        showSettingsDialog();
                        break;
                }

            case R.id.menu_update:
                switch(mainGroup)
                {
                    case Groups.Orbitals:
                        switch(page)
                        {
                            case Orbitals.PageType.Satellites:
                                showConfirmUpdateDialog();
                                break;
                        }
                        break;
                }
                break;

            case R.id.menu_edit:
                switch(mainGroup)
                {
                    case Groups.Calculate:
                        switch(page)
                        {
                            case Calculate.PageType.View:
                            case Calculate.PageType.Passes:
                            case Calculate.PageType.Coordinates:
                            case Calculate.PageType.Intersection:
                                //stop any running task on page
                                stopPageTask();

                                //update sub page
                                setSubPage(Groups.Calculate, page, Calculate.SubPageType.Input);

                                //update display
                                updateMainPager(false);
                                updateOptionsMenu();
                                break;
                        }
                        break;
                }
                break;

            case R.id.menu_save:
                switch(mainGroup)
                {
                    case Groups.Calculate:
                        switch(page)
                        {
                            case Calculate.PageType.View:
                            case Calculate.PageType.Passes:
                            case Calculate.PageType.Coordinates:
                            case Calculate.PageType.Intersection:
                                showSaveCalculatePageFileDialog(page);
                                break;
                        }
                        break;

                    case Groups.Orbitals:
                        switch(page)
                        {
                            case Orbitals.PageType.Satellites:
                                showSaveSatellitesFileDialog();
                                break;
                        }
                        break;
                }
                break;
        }

        //if setting display group
        if(setDisplayGroup)
        {
            //update display group
            setMainGroup(mainGroup, true);
        }

        return(super.onOptionsItemSelected(item));
    }

    @Override
    public void onBackPressed()
    {
        boolean updateDisplay = false;
        boolean cancelCalibration = false;
        boolean closedSettings = false;
        int page = getMainPage();
        int desiredSubPage;
        Calendar currentTime = Globals.getGMTTime();
        Resources res = this.getResources();

        //if drawer is open
        if(mainDrawerLayout.isDrawerOpen(GravityCompat.START))
        {
            //close it
            mainDrawerLayout.closeDrawer(GravityCompat.START);
            return;
        }

        //handle based on group
        switch(mainGroup)
        {
            case Groups.Current:
                switch(currentSubPage[page])
                {
                    case Current.SubPageType.Lens:
                        //if camera view exists
                        if(Current.cameraView != null)
                        {
                            //if calibrating
                            if(Current.showCalibration)
                            {
                                cancelCalibration = true;
                                break;
                            }
                            //else if able to close settings menu
                            else if(Current.cameraView.closeSettingsMenu())
                            {
                                closedSettings = true;
                                break;
                            }
                        }
                        //else fall through

                    //go back to list
                    case Current.SubPageType.Map:
                    case Current.SubPageType.Globe:
                        //if not on lens and able to close settings menu
                        if(currentSubPage[page] != Current.SubPageType.Lens && Current.Coordinates.closeSettingsMenu())
                        {
                            closedSettings = true;
                        }
                        else
                        {
                            setSubPage(Groups.Current, page, Current.SubPageType.List);
                            updateDisplay = true;
                        }
                        break;
                }
                break;

            case Groups.Calculate:
                desiredSubPage = Calculate.SubPageType.Input;
                switch(page)
                {
                    case Calculate.PageType.View:
                    case Calculate.PageType.Passes:
                    case Calculate.PageType.Coordinates:
                    case Calculate.PageType.Intersection:
                        //if on view lens or map
                        switch(calculateSubPage[page])
                        {
                            //go back to list
                            case Calculate.SubPageType.Lens:
                            case Calculate.SubPageType.Map:
                            case Calculate.SubPageType.Globe:
                                desiredSubPage = Calculate.SubPageType.List;
                                break;
                        }
                        //fall through

                    default:
                        //stop any running task on page
                        stopPageTask();

                        //if not on desired sub page
                        if(calculateSubPage[page] != desiredSubPage)
                        {
                            //go back to desired sub page
                            setSubPage(Groups.Calculate, page, desiredSubPage);
                            updateDisplay = true;
                        }
                        break;
                }
                break;

            case Groups.Settings:
                switch(page)
                {
                    case Settings.PageType.Other:
                        //if not on the main display page
                        if(settingsSubPage[page] != Settings.SubPageType.None)
                        {
                            //if went back to previous page
                            if(showPreviousPage())
                            {
                                //stop
                                return;
                            }

                            //go back to main display page
                            setSubPage(Groups.Settings, page, Settings.SubPageType.None);
                            updateDisplay = true;
                        }
                        break;
                }
                break;
        }

        //if updating display
        if(updateDisplay)
        {
            //update displays
            updateMainPager(false);
            updateOptionsMenu();
        }
        //else if cancelling calibration
        else if(cancelCalibration)
        {
            //stop calibration
            Current.cameraView.stopCalibration();
        }
        //else if more than 3 seconds between presses
        else if(!closedSettings && currentTime.getTimeInMillis() - backPressTime.getTimeInMillis() > 3000)
        {
            //update time pressed
            backPressTime = currentTime;

            //show message
            Toast.makeText(this, res.getString(R.string.text_press_back_again_exit), Toast.LENGTH_SHORT).show();
        }
        else if(!closedSettings)
        {
            //call super
            super.onBackPressed();
        }
    }

    //Returns true if returned to previous page
    public boolean showPreviousPage()
    {
        boolean setPage = false;
        boolean previousMap;
        boolean previousLens;

        switch(previousGroup)
        {
            case Groups.Current:
                previousLens = (previousSubPage == Current.SubPageType.Lens);
                previousMap = (previousSubPage == Current.SubPageType.Map || previousSubPage == Current.SubPageType.Globe);

                switch(previousPage)
                {
                    case Current.PageType.View:
                    case Current.PageType.Passes:
                        setPage = previousLens;
                        break;

                    case Current.PageType.Coordinates:
                        setPage = previousMap;
                        break;

                    case Current.PageType.Combined:
                        setPage = (previousLens || previousMap);
                        break;
                }
                break;
        }

        //if can set page
        if(setPage)
        {
            //set to previous page
            setMainGroup(previousGroup, true);
            setMainPage(previousPage);
            setSubPage(previousGroup, previousPage, previousSubPage);
        }

        //return if set page
        return(setPage);
    }

    //Returns observer
    public static ObserverType getObserver()
    {
        return(observer);
    }

    //Returns observer time zone
    public static TimeZone getTimeZone()
    {
        return(observer == null || observer.timeZone == null ? TimeZone.getDefault() : observer.timeZone);
    }

    //Returns current satellites
    public static Database.SatelliteData[] getSatellites()
    {
        return(currentSatellites);
    }

    //Handles any needed first run
    private void handleFirstRun(Bundle savedInstanceState)
    {
        //if not running user setup and not on the first run
        boolean onFirstRun = Settings.getFirstRun(this);
        finishedSetup = (!runningUserSetup && !onFirstRun);
        if(finishedSetup)
        {
            //loading starting data
            loadStartData(savedInstanceState);
        }
        else
        {
            //if not done with first run and not already building database
            if(onFirstRun && !UpdateService.buildingDatabase())
            {
                //build database
                UpdateService.buildDatabase(this);
            }

            //if not already running user setup
            if(!runningUserSetup)
            {
                //show setup
                showSetupDialog();
            }
        }
    }

    //Loads start data and updates display
    private void loadStartData(Bundle savedInstanceState)
    {
        //get displays
        optionsMenu = null;
        sideActionDivider = this.findViewById(R.id.Side_Action_Divider);
        sideActionMenu = this.findViewById(R.id.Side_Action_Menu);
        mainFloatingButton = this.findViewById(R.id.Main_Floating_Button);
        mainFloatingButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                handleMainFloatingButtonClick(false);
            }
        });
        mainFloatingButton.setOnLongClickListener(new View.OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View view)
            {
                handleMainFloatingButtonClick(true);
                return(true);
            }
        });

        //set mode changed listener
        editModeChangedListener = createOnEditModeChangedListener();

        //setup drawer menu
        BaseInputActivity.setupActionBar(this, this.getSupportActionBar(), true);
        updateSideMenu();

        //setup location retrieving
        locationReceiver = createLocationReceiver(locationReceiver);

        //get observer
        loadObserver(!savedState);

        //handle any updates
        DatabaseManager.handleUpdates(this);

        //create add current location dialog
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

        //load orbitals
        loadOrbitals(this);

        //update timer delays
        updateTimerDelays();

        //set tasks
        timerTask = null;
        timerRunnable = null;
        timerDelay = listTimerDelay;
        calculateCoordinatesTask = null;

        //setup pager
        mainPager.addOnPageChangeListener(createOnPageChangedListener());

        //update display
        setMainGroup(mainGroup, true);

        //force refresh first time
        setMainPage(savedInstanceState.getInt(ParamTypes.MainPagerItem, 0));
    }

    //Loads currently selected orbitals
    public static void loadOrbitals(Context context)
    {
        int index;
        Database.DatabaseSatellite[] dbSatellites;

        //load selected orbitals
        dbSatellites = Database.getSelectedOrbitals(context, false, false);
        currentSatellites = new Database.SatelliteData[dbSatellites.length];
        for(index = 0; index < dbSatellites.length; index++)
        {
            currentSatellites[index] = new Database.SatelliteData(dbSatellites[index]);
        }
    }

    //Updates the side menu
    private void updateSideMenu()
    {
        Resources res = this.getResources();
        ExpandableListView sideMenu = this.findViewById(R.id.Side_Menu);
        Drawable satelliteDrawable = Globals.getDrawable(this, R.drawable.orbital_satellite, true);
        Drawable viewDrawable = Globals.getDrawable(this, R.drawable.ic_remove_red_eye_white, true);
        Drawable passDrawable = Globals.getDrawable(this, R.drawable.orbit, true);
        Drawable coordinateDrawable = Globals.getDrawable(this, R.drawable.ic_language_black, true);
        Drawable intersectionDrawable = Globals.getDrawable(this, R.drawable.ic_intersect, true);
        Drawable starDrawable = Globals.getDrawable(this, R.drawable.orbital_sun);
        Drawable planetDrawable = Globals.getDrawable(this, R.drawable.orbital_moon);
        Drawable locationDrawable = Globals.getDrawable(this, R.drawable.ic_my_location_black, true);
        Drawable notificationsDrawable = Globals.getDrawable(this, R.drawable.ic_notifications_white, true);
        Drawable updatesDrawable = Globals.getDrawable(this, R.drawable.ic_sync_white, true);
        Drawable otherDrawable = Globals.getDrawable(this, R.drawable.ic_settings_black, true);
        String statusString = res.getString(R.string.title_status);
        String viewString = res.getString(R.string.title_view);
        String passesString = res.getString(R.string.title_passes);
        String coordinatesString = res.getString(R.string.title_coordinates);
        String intersectionString = res.getString(R.string.title_intersection);
        ArrayList<SideMenuListAdapter.Group> groups = new ArrayList<>(0);

        groups.add(new Group(this, res.getString(R.string.title_current), R.drawable.ic_access_time_black, Settings.getCombinedCurrentLayout(this) ? (new Item[]{new Item(statusString, viewDrawable)}) : (new Item[]{new Item(viewString, viewDrawable), new Item(passesString, passDrawable), new Item(coordinatesString, coordinateDrawable)})));
        groups.add(new Group(this, res.getString(R.string.title_calculate), R.drawable.ic_calculator_black, new Item[]{new Item(viewString, viewDrawable), new Item(passesString, passDrawable), new Item(coordinatesString, coordinateDrawable), new Item(intersectionString, intersectionDrawable)}));
        groups.add(new Group(this, res.getString(R.string.title_orbitals), R.drawable.orbit, new Item[]{new Item(res.getString(R.string.title_satellites), satelliteDrawable), new Item(res.getString(R.string.title_stars), starDrawable), new Item(res.getString(R.string.title_moon_and_planets), planetDrawable)}));
        groups.add(new Group(this, res.getString(R.string.title_settings), R.drawable.ic_settings_black, new Item[]{new Item(res.getString(R.string.title_locations), locationDrawable), new Item(res.getString(R.string.title_notifications), notificationsDrawable), new Item(res.getString(R.string.title_updates), updatesDrawable), new Item(res.getString(R.string.title_other), otherDrawable)}));

        if(sideMenu != null)
        {
            sideMenu.setAdapter(new SideMenuListAdapter(this, groups));
            sideMenu.setOnChildClickListener(createOnSideMenuChildClickListener());
            sideMenu.setOnGroupExpandListener(createOnSideMenuGroupExpandListener(sideMenu));
        }
    }

    //Updates the main pager list and returns to page before update
    private void updateMainPager(boolean reloadOrbitals, boolean saveItems)
    {
        int page = getMainPage();

        //if need to reload orbitals
        if(reloadOrbitals)
        {
            loadOrbitals(this);
        }

        //update pager
        updateMainPagerAdapter(false, saveItems);
        setMainPage(page);

        //if reloaded orbitals and showing status
        if(reloadOrbitals && mainGroup == Groups.Current)
        {
            updateCurrentCalculations();
        }
    }
    private void updateMainPager(boolean reloadOrbitals)
    {
        updateMainPager(reloadOrbitals, true);
    }

    //Updates subtitle
    private void updateSubtitle()
    {
        String subTitle = null;
        Resources res = this.getResources();
        ActionBar mainActionBar = this.getSupportActionBar();

        switch(mainGroup)
        {
            case Groups.Current:
                subTitle = res.getString(R.string.title_current);
                break;

            case Groups.Calculate:
                subTitle = res.getString(R.string.title_calculate);
                break;

            case Groups.Orbitals:
                subTitle = res.getString(R.string.title_orbitals);
                break;

            case Groups.Settings:
                subTitle = res.getString(R.string.title_settings);
                break;
        }

        //if action bar exists
        if(mainActionBar != null)
        {
            //set subtitle
            mainActionBar.setSubtitle(subTitle);
        }
    }

    //Updates current timer delays
    private void updateTimerDelays()
    {
        SharedPreferences readSettings = Settings.getPreferences(this);

        listTimerDelay = readSettings.getInt(Settings.PreferenceName.ListUpdateDelay, 1000);
        lensTimerDelay = readSettings.getInt(Settings.PreferenceName.LensUpdateDelay, 1000);
    }

    //Updates current calculations
    private void updateCurrentCalculations()
    {
        int page = getMainPage();

        switch(page)
        {
            case Current.PageType.View:
            case Current.PageType.Combined:
                if(currentViewAnglesTask != null && currentSubPage[page] == Current.SubPageType.Lens)
                {
                    currentViewAnglesTask.needViews = true;
                }

                //if not combined
                if(page != Current.PageType.Combined)
                {
                    //stop
                    break;
                }
                //else fall through

            case Current.PageType.Coordinates:
                if(currentCoordinatesTask != null)
                {
                    currentCoordinatesTask.needCoordinates = true;
                }
                break;
        }
    }

    //Updates the current location display
    private void updateCurrentLocationDisplay()
    {
        if(settingsLocationsListAdapter != null)
        {
            settingsLocationsListAdapter.setCurrentLocation(observer.geo.latitude, observer.geo.longitude, observer.geo.altitudeKm * 1000);
        }

        Current.Coordinates.setCurrentLocation(this, observer);
    }

    //Loads the observer
    private void loadObserver(boolean showStatus)
    {
        Database.DatabaseLocation selectedLocation = Database.getSelectedLocation(this);

        //if observer is set, using current location, and nothing is set
        if(observer != null && selectedLocation.locationType == Database.LocationType.Current && selectedLocation.latitude == 0 && selectedLocation.longitude == 0 && selectedLocation.altitudeKM == 0)
        {
            //use observer instead
            selectedLocation.latitude = observer.geo.latitude;
            selectedLocation.longitude = observer.geo.longitude;
            selectedLocation.altitudeKM = observer.geo.altitudeKm;
            selectedLocation.zoneId = observer.timeZone.getID();
        }

        updateObserver(selectedLocation.latitude, selectedLocation.longitude, selectedLocation.altitudeKM, selectedLocation.zoneId, selectedLocation.locationType, showStatus);
    }

    //Updates the observer
    private void updateObserver(double latitude, double longitude, double altitudeKM, String zoneId, byte locationType, boolean showStatus)
    {
        //update location source and observer
        locationSource = locationType;
        observer = Calculations.loadObserver(latitude, longitude, altitudeKM, zoneId);
        switch(locationSource)
        {
            case Database.LocationType.Current:
                pendingLocationUpdate = true;
                if(showStatus)
                {
                    Globals.showSnackBar(mainDrawerLayout, this.getResources().getString(R.string.title_location_getting));
                }
                LocationService.getCurrentLocation(this, false, true, LocationService.PowerTypes.HighPowerThenBalanced);
                break;

            default:
                pendingLocationUpdate = false;
                Current.Coordinates.setCurrentLocation(this, observer);
                if(Current.cameraView != null)
                {
                    Current.cameraView.updateAzDeclination(observer);
                }
                CalculateService.NotifyReceiver.updateNotifyLocations(this, observer, false, userChangedLocation);
                userChangedLocation = false;
                break;
        }
    }

    //Saves a calculated angles file
    private void saveCalculateAnglesFile(OutputStream outStream, String separator) throws Exception
    {
        int index;
        boolean isSun;
        boolean isMoon;
        String line;
        Resources res = this.getResources();
        Current.ViewAngles.Item[] items;

        try
        {
            //get items
            items = Calculate.PageAdapter.getViewAngleItems();
            if(items.length <= 0)
            {
                //show error
                throw(new Exception(res.getString(R.string.text_no_items)));
            }
            else
            {
                //remember if sun or moon
                isSun = (items[0].id == Universe.IDs.Sun);
                isMoon = (items[0].id == Universe.IDs.Moon);
            }

            //write column headers
            line = res.getString(R.string.title_time) + separator + res.getString(R.string.title_azimuth) + separator + res.getString(R.string.title_elevation) + separator + res.getString(R.string.title_range) + " (" + Globals.getKmLabel(res) + ")" + (isMoon ? (separator + res.getString(R.string.title_illumination)) : "") + (isSun || isMoon ? (separator + res.getString(R.string.title_phase)) : "") + "\r\n";
            outStream.write(line.getBytes(Globals.Encoding.UTF16));

            //go through each item
            for(index = 0; index < items.length; index++)
            {
                //get current item
                Current.ViewAngles.Item currentItem = items[index];

                //save item to file
                line = Globals.getDateTimeString(currentItem.time) + separator + Globals.getNumberString(currentItem.azimuth) + separator + Globals.getNumberString(currentItem.elevation) + separator + String.format(Locale.US, "%.2f", Globals.getKmUnitValue(currentItem.rangeKm)) + (isMoon ? (separator + Globals.getNumberString(currentItem.illumination, 1) + "%") : "") + (isSun || isMoon ? (separator + currentItem.phaseName) : "") + "\r\n";
                outStream.write(line.getBytes(Globals.Encoding.UTF16));
            }
        }
        catch(Exception ex)
        {
            throw(new Exception(ex));
        }
    }

    //Saves a calculated passes file
    private void saveCalculatePassesFile(OutputStream outStream, String separator) throws Exception
    {
        int index;
        boolean isSun;
        boolean isMoon;
        String line;
        Resources res = this.getResources();
        String azString = res.getString(R.string.abbrev_azimuth);
        String startString = res.getString(R.string.title_start);
        String endString = res.getString(R.string.title_end);
        String timeString = res.getString(R.string.title_time);
        Current.Passes.Item[] items;

        try
        {
            //get items
            items = Calculate.PageAdapter.getPassItems();
            if(items.length <= 0)
            {
                //show error
                throw(new Exception(res.getString(R.string.text_no_items)));
            }
            else
            {
                //remember if sun or moon
                isSun = (items[0].id == Universe.IDs.Sun);
                isMoon = (items[0].id == Universe.IDs.Moon);
            }

            //write column headers
            line = timeString + " " + startString + separator + timeString + " " + endString + separator + res.getString(R.string.title_duration) + separator + azString + " " + startString + separator + azString + " " + endString + separator + azString + " " + res.getString(R.string.title_travel) + separator + res.getString(R.string.abbrev_elevation) + " " + res.getString(R.string.title_max) + (isMoon ? (separator + res.getString(R.string.title_illumination)) : "") + (isSun || isMoon ? (separator + res.getString(R.string.title_phase)) : "") + "\r\n";
            outStream.write(line.getBytes(Globals.Encoding.UTF16));

            //go through each item
            for(index = 0; index < items.length; index++)
            {
                //get current item
                Current.Passes.Item currentItem = items[index];

                //save item to file
                line = Globals.getDateTimeString(currentItem.passTimeStart) + separator + Globals.getDateTimeString(currentItem.passTimeEnd) + separator + currentItem.passDuration + separator + Globals.getNumberString(currentItem.passAzStart) + separator + Globals.getNumberString(currentItem.passAzEnd) + separator + Globals.getNumberString(currentItem.passAzTravel) + separator + Globals.getNumberString(currentItem.passElMax) + (isMoon ? (separator + Globals.getNumberString(currentItem.illumination, 1) + "%") : "") + (isSun || isMoon ? (separator + currentItem.phaseName) : "") + "\r\n";
                outStream.write(line.getBytes(Globals.Encoding.UTF16));
            }
        }
        catch(Exception ex)
        {
            throw(new Exception(ex));
        }
    }

    //Saves a calculated coordinates file
    private void saveCalculateCoordinatesFile(OutputStream outStream, String separator) throws Exception
    {
        int index;
        boolean isSun;
        boolean isMoon;
        String line;
        Resources res = this.getResources();
        Current.Coordinates.Item[] items;

        try
        {
            //get items
            items = Calculate.PageAdapter.getCoordinatesItems();
            if(items.length <= 0)
            {
                //show error
                throw(new Exception(res.getString(R.string.text_no_items)));
            }
            else
            {
                //remember if sun and moon
                isSun = (items[0].id == Universe.IDs.Sun);
                isMoon = (items[0].id == Universe.IDs.Moon);
            }

            //write column headers
            line = res.getString(R.string.title_time) + separator + res.getString(R.string.title_latitude) + separator + res.getString(R.string.title_longitude) + separator + res.getString(R.string.abbrev_altitude) + " (" + Globals.getKmLabel(res) + ")" + (isMoon ? (separator + res.getString(R.string.title_illumination)) : "") + (isSun || isMoon ? (separator + res.getString(R.string.title_phase)) : "") + "\r\n";
            outStream.write(line.getBytes(Globals.Encoding.UTF16));

            //go through each item
            for(index = 0; index < items.length; index++)
            {
                //get current item
                Current.Coordinates.Item currentItem = items[index];

                //save item to file
                line = Globals.getDateTimeString(currentItem.time) + separator + Globals.getNumberString(currentItem.latitude) + separator + Globals.getNumberString(currentItem.longitude) + separator + (currentItem.altitudeKm > 0 && currentItem.altitudeKm != Float.MAX_VALUE ? String.format(Locale.US, "%.2f", Globals.getKmUnitValue(currentItem.altitudeKm)) : "-") + (isMoon ? (separator + Globals.getNumberString(currentItem.illumination, 1) + "%") : "") + (isSun || isMoon ? (separator + currentItem.phaseName) : "") + "\r\n";
                outStream.write(line.getBytes(Globals.Encoding.UTF16));
            }
        }
        catch(Exception ex)
        {
            throw(new Exception(ex));
        }
    }

    //Saves a calculated intersections file
    private void saveCalculateIntersectionsFile(OutputStream outStream, String separator) throws Exception
    {
        int index;
        boolean isSun;
        boolean isMoon;
        String line;
        Resources res = this.getResources();
        String azString = res.getString(R.string.abbrev_azimuth);
        String elString = res.getString(R.string.abbrev_elevation);
        String startString = res.getString(R.string.title_start);
        String endString = res.getString(R.string.title_end);
        String timeString = res.getString(R.string.title_time);
        String closestIntersectionString = res.getString(R.string.title_closest_intersection_delta);
        Current.Passes.Item firstItem;
        Current.Passes.Item[] items;

        try
        {
            //get items
            items = Calculate.PageAdapter.getIntersectionItems();
            firstItem = (items.length > 0 ? items[0] : null);
            if(firstItem == null || firstItem.satellite2 == null)
            {
                //show error
                throw(new Exception(res.getString(R.string.text_no_items)));
            }
            else
            {
                //remember if sun or moon
                isSun = (firstItem.id == Universe.IDs.Sun || (firstItem.satellite2.getSatelliteNum() == Universe.IDs.Sun));
                isMoon = (firstItem.id == Universe.IDs.Moon || (firstItem.satellite2.getSatelliteNum() == Universe.IDs.Moon));
            }

            //write column headers
            line = timeString + " " + startString + separator + timeString + " " + endString + separator + res.getString(R.string.title_duration) + separator + azString + " " + startString + separator + azString + " " + endString + separator + azString + " " + res.getString(R.string.title_travel) + separator + elString + " " + res.getString(R.string.title_max) + separator + azString + " " + closestIntersectionString + separator + elString + " " + closestIntersectionString + (isMoon ? (separator + res.getString(R.string.title_illumination)) : "") + (isSun || isMoon ? (separator + res.getString(R.string.title_phase)) : "") + "\r\n";
            outStream.write(line.getBytes(Globals.Encoding.UTF16));

            //go through each item
            for(index = 0; index < items.length; index++)
            {
                //get current item
                Current.Passes.Item currentItem = items[index];

                //save item to file
                line = Globals.getDateTimeString(currentItem.passTimeStart) + separator + Globals.getDateTimeString(currentItem.passTimeEnd) + separator + currentItem.passDuration + separator + Globals.getNumberString(currentItem.passAzStart) + separator + Globals.getNumberString(currentItem.passAzEnd) + separator + Globals.getNumberString(currentItem.passAzTravel) + separator + Globals.getNumberString(currentItem.passElMax) + separator + Globals.getNumberString(currentItem.passClosestAz) + separator + Globals.getNumberString(currentItem.passClosestEl) + (isMoon ? (separator + Globals.getNumberString(currentItem.illumination, 1) + "%") : "") + (isSun || isMoon ? (separator + currentItem.phaseName) : "") + "\r\n";
                outStream.write(line.getBytes(Globals.Encoding.UTF16));
            }
        }
        catch(Exception ex)
        {
            throw(new Exception(ex));
        }
    }

    //Saves a calculated data file
    private void saveCalculateFile(int page, OutputStream outStream, String separator, int fileSource) throws Exception
    {
        Resources res = this.getResources();

        //save based on page
        switch(page)
        {
            case Calculate.PageType.View:
                saveCalculateAnglesFile(outStream, separator);
                break;

            case Calculate.PageType.Passes:
                saveCalculatePassesFile(outStream, separator);
                break;

            case Calculate.PageType.Coordinates:
                saveCalculateCoordinatesFile(outStream, separator);
                break;

            case Calculate.PageType.Intersection:
                saveCalculateIntersectionsFile(outStream, separator);
                break;
        }

        //if not for Dropbox or Google Drive
        if(fileSource != Globals.FileSource.Dropbox && fileSource != Globals.FileSource.GoogleDrive)
        {
            //show success message
            Globals.showSnackBar(mainDrawerLayout, res.getString(R.string.text_file_saved));
        }
    }
    private void saveCalculateFile(final int page, final String fileName, final String extension, final int fileSourceType, boolean confirmInternet)
    {
        String separator = (extension.equals(Globals.FileExtensionType.TXT) ? "\t" : ",");
        Resources res = this.getResources();

        //remember pending save file
        pendingSaveFile = new Globals.PendingFile(page, fileName, extension, -1, fileSourceType, separator);

        //handle based on file source
        switch(fileSourceType)
        {
            case Globals.FileSource.SDCard:
                //if have permission to write to external storage
                if(Globals.haveWritePermission(this))
                {
                    try
                    {
                        //try to create file and output stream
                        File saveFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), pendingSaveFile.name);
                        OutputStream outStream = new FileOutputStream(saveFile);

                        //save file
                        saveCalculateFile(page, outStream, separator, Globals.FileSource.SDCard);
                        outStream.close();
                    }
                    catch(Exception ex)
                    {
                        //show error message
                        Globals.showSnackBar(mainDrawerLayout, res.getString(R.string.text_file_error_saving), ex.getMessage(), true, true);
                    }
                }
                //else if don't have permission but can ask
                else if(Globals.askWritePermission)
                {
                    Globals.askWritePermission(this, false);
                }
                else
                {
                    //show denied and don't ask again
                    Globals.showDenied(mainDrawerLayout, res.getString(R.string.desc_permission_write_external_storage_deny));
                    Globals.askWritePermission = false;
                }
                break;

            case Globals.FileSource.GoogleDrive:
            case Globals.FileSource.Dropbox:
                //if confirm internet and should ask
                if(confirmInternet && Globals.shouldAskInternetConnection(this))
                {
                    //get confirmation
                    Globals.showConfirmInternetDialog(this, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            //don't ask this time
                            saveCalculateFile(page, fileName, extension, fileSourceType, false);
                        }
                    });
                }
                else
                {
                    try
                    {
                        //try to create file and output stream
                        File saveFile = new File(this.getCacheDir() + "/" + pendingSaveFile.name);
                        OutputStream outStream = new FileOutputStream(saveFile);

                        //save file
                        saveCalculateFile(page, outStream, separator, Globals.FileSource.GoogleDrive);
                        outStream.close();
                        if(fileSourceType == Globals.FileSource.GoogleDrive)
                        {
                            GoogleDriveAccess.start(this, saveFile, 1, true);
                        }
                        else
                        {
                            DropboxAccess.start(this, saveFile, 1, true);
                        }
                    }
                    catch(Exception ex)
                    {
                        //show error message
                        Globals.showSnackBar(mainDrawerLayout, res.getString(R.string.text_file_error_saving), ex.getMessage(), true, true);
                    }
                }
                break;
        }
    }
    private void saveCalculateFile(int page, String fileName, String extension, int fileSourceType)
    {
        saveCalculateFile(page, fileName, extension, fileSourceType, true);
    }

    //Saves pending file
    private void savePendingFile(boolean confirmInternet)
    {
        int page = getMainPage();

        switch(mainGroup)
        {
            case Groups.Calculate:
                if(calculateSubPage[page] == Calculate.SubPageType.List)
                {
                    //try to save file
                    saveCalculateFile(page, pendingSaveFile.name, pendingSaveFile.extension, pendingSaveFile.fileSourceType, confirmInternet);
                }
                break;

            case Groups.Orbitals:
                orbitalPageAdapter.saveFileSelectedItems(mainPager, page);
                break;
        }
    }
    private void savePendingFile()
    {
        savePendingFile(false);
    }

    //Updates main pager adapter
    private void updateMainPagerAdapter(boolean groupChanged, boolean saveItems)
    {
        int index;
        int page = getMainPage();
        boolean showCurrent = (mainGroup == Groups.Current);
        boolean showCalculate = (mainGroup == Groups.Calculate);
        boolean showSettings = (mainGroup == Groups.Settings);
        PagerAdapter currentAdapter = mainPager.getAdapter();
        PagerAdapter desiredAdapter = null;
        FragmentManager currentManger = this.getSupportFragmentManager();

        //if showing current
        if(showCurrent)
        {
            //update saved items
            if(currentPageAdapter != null)
            {
                //save items if not viewing list
                currentPageAdapter.setSavedItems(Current.PageType.Passes, (saveItems && currentSubPage[Current.PageType.Passes] != Current.SubPageType.List ? Current.PageAdapter.getPassItems() : null));
                currentPageAdapter.setSavedItems(Current.PageType.Combined, (saveItems && currentSubPage[Current.PageType.Combined] != Current.SubPageType.List ? Current.PageAdapter.getCombinedItems() : null));
            }
        }
        //else if showing calculate
        else if(showCalculate)
        {
            //save needed items
            if(calculatePageAdapter != null)
            {
                //save items if viewing input
                Calculate.PageAdapter.setSavedItems(Calculate.PageType.View, (calculateSubPage[Calculate.PageType.View] == Calculate.SubPageType.Input ? null : Calculate.PageAdapter.getViewAngleItems()));
                if(calculateSubPage[Calculate.PageType.View] == Calculate.SubPageType.Input)
                {
                    //remove items when starting again
                    Calculate.PageAdapter.setViewItems(new Current.ViewAngles.Item[0]);
                }

                //save inputs
                calculatePageAdapter.setSavedSubInput(Calculate.PageType.Passes, ParamTypes.PassIndex, passesPassIndex);
                if(calculateSubPage[Calculate.PageType.Passes] == Calculate.SubPageType.Input)
                {
                    //remove items when starting again
                    Calculate.PageAdapter.setSavedItems(Calculate.PageType.Passes, null);
                    Calculate.PageAdapter.setPassItems(new Current.Passes.Item[0]);
                }

                //save items if viewing input
                Calculate.PageAdapter.setSavedItems(Calculate.PageType.Coordinates, (calculateSubPage[Calculate.PageType.Coordinates] == Calculate.SubPageType.Input ? null : Calculate.PageAdapter.getCoordinatesItems()));
                if(calculateSubPage[Calculate.PageType.Coordinates] == Calculate.SubPageType.Input)
                {
                    //remove items when starting again
                    Calculate.PageAdapter.setCoordinateItems(new Current.Coordinates.Item[0]);
                }

                //save inputs
                calculatePageAdapter.setSavedSubInput(Calculate.PageType.Intersection, ParamTypes.PassIndex, intersectionPassIndex);
                if(calculateSubPage[Calculate.PageType.Intersection] == Calculate.SubPageType.Input)
                {
                    //remove items when starting again
                    Calculate.PageAdapter.setSavedItems(Calculate.PageType.Intersection, null);
                    Calculate.PageAdapter.setIntersectionItems(new Current.Passes.Item[0]);
                }
            }
        }

        //if adapter not set or group changed
        if(currentAdapter == null || groupChanged)
        {
            switch(mainGroup)
            {
                case Groups.Current:
                    desiredAdapter = currentPageAdapter = new Current.PageAdapter(currentManger, mainDrawerLayout, createOnItemDetailButtonClickListener(), createOnSetAdapterListener(), currentSubPage);
                    break;

                case Groups.Calculate:
                    desiredAdapter = calculatePageAdapter = new Calculate.PageAdapter(currentManger, mainDrawerLayout, createOnItemDetailButtonClickListener(), createOnSetAdapterListener(), createOnPageSetListener(), calculateSubPage, calculateBundle);
                    break;

                case Groups.Orbitals:
                    desiredAdapter = orbitalPageAdapter = new Orbitals.PageAdapter(currentManger, mainDrawerLayout, createOnItemDetailButtonClickListener(), createOnUpdateNeededListener(), createOnPageResumeListener());
                    break;

                case Groups.Settings:
                    desiredAdapter = settingsPagerAdapter = new Settings.PageAdapter(currentManger, mainDrawerLayout, createOnItemSelectedListener(), createOnSettingsItemCheckChangedListener(), createOnSetAdapterListener(), createOnUpdateNeededListener(), createOnUpdatePageListener(), createOnPageResumeListener(), settingsSubPage);
                    break;
            }

            //if the group changed
            if(groupChanged)
            {
                //if now showing calculate
                if(showCalculate)
                {
                    //go through each calculate sub page
                    for(index = 0; index < Calculate.PageType.PageCount; index++)
                    {
                        //reset each sub page to input
                        setSubPage(Groups.Calculate, index, Calculate.SubPageType.Input, false);
                    }
                }

                //if now showing settings
                if(showSettings)
                {
                    //go through each settings sub page
                    for(index = 0; index < Settings.PageType.PageCount; index++)
                    {
                        //reset each sub page to main page
                        setSubPage(Groups.Settings, index, Settings.SubPageType.None, false);
                    }
                }
            }

            //set adapter
            mainPager.setAdapter(desiredAdapter);
        }
        else
        {
            try
            {
                //update display
                currentAdapter.notifyDataSetChanged();
            }
            catch(Exception ex)
            {
                //do nothing
            }
        }

        //if side action menu exists
        if(sideActionMenu != null)
        {
            boolean hasItems;
            Resources res = getResources();
            ItemAdapter menuAdapter;
            ArrayList<SideMenuListAdapter.Item> items = new ArrayList<>(0);

            //menu items
            switch(mainGroup)
            {
                case Groups.Current:
                case Groups.Orbitals:
                    items.add(new Item(res.getString(R.string.title_add) + " " + res.getQuantityString(R.plurals.title_satellites, 1), Globals.getDrawable(this, R.drawable.ic_add_white, true)));
                    break;

            }

            //if there are items
            hasItems = (items.size() > 0);
            if(hasItems)
            {
                //setup menu adapter
                menuAdapter = new SideMenuListAdapter.ItemAdapter(this, items);
                menuAdapter.setOnItemClickedListener(new Selectable.ListBaseAdapter.OnItemClickListener()
                {
                    @Override
                    public void onItemClicked(View view, int position)
                    {
                        //handle based on group
                        switch(mainGroup)
                        {
                            case Groups.Current:
                            case Groups.Orbitals:
                                switch(position)
                                {
                                    //add satellite
                                    case 0:
                                        //show dialog and close menu
                                        Orbitals.showAddDialog(MainActivity.this, mainDrawerLayout, false);
                                        mainDrawerLayout.closeDrawers();
                                        break;
                                }
                                break;
                        }
                    }
                });

                //setup menu
                sideActionMenu.setHasFixedSize(true);
                sideActionMenu.addItemDecoration(new DividerItemDecoration(this.getBaseContext(), LinearLayoutManager.VERTICAL));
                sideActionMenu.setLayoutManager(new LinearLayoutManager(this.getBaseContext()));
                sideActionMenu.setAdapter(menuAdapter);
            }

            //update visibility
            sideActionDivider.setVisibility(hasItems ? View.VISIBLE : View.GONE);
            sideActionMenu.setVisibility(hasItems ? View.VISIBLE : View.GONE);
        }

        //update titles and swiping status to allow if not on current/calculate with map/globe
        mainPager.setSwipeEnabled(!((showCurrent && (page == Current.PageType.Coordinates || page == Current.PageType.Combined) && (currentSubPage[page] == Current.SubPageType.Map || currentSubPage[page] == Current.SubPageType.Globe)) || (showCalculate && page == Calculate.PageType.Coordinates && (calculateSubPage[page] == Calculate.SubPageType.Map || calculateSubPage[page] == Calculate.SubPageType.Globe))));
        mainPagerTitles.setVisibility(showCurrent && Settings.getCombinedCurrentLayout(this.getBaseContext()) ? View.GONE : View.VISIBLE);
    }

    //Sets main group
    private void setMainGroup(int group, boolean force)
    {
        boolean groupChanging = (mainGroup != group);

        //if forced or group is changing
        if(force || groupChanging)
        {
            //update current group and page
            if(previousGroup == mainGroup)
            {
                previousPage = getMainPage();
            }
            previousGroup = mainGroup;
            mainGroup = group;

            //reset
            wasDraggingPage = false;

            //update pager adapter
            updateMainPagerAdapter(groupChanging, true);

            //update running tasks
            updateRunningTasks(true);

            //update display
            updateOptionsMenu();
            updateSubtitle();
        }
    }

    //Gets main page
    private int getMainPage()
    {
        return(mainGroup == Groups.Current && Settings.getCombinedCurrentLayout(this) ? Current.PageType.Combined : mainPager.getCurrentItem());
    }

    //Updates main page
    private void setMainPage(int page)
    {
        //if previous group is the same group
        if(previousGroup == mainGroup)
        {
            //update previous page
            previousPage = getMainPage();
        }

        //set page
        mainPager.setCurrentItem(page);

        //reset
        wasDraggingPage = false;
    }

    //Updates running tasks
    private void updateRunningTasks(boolean run)
    {
        boolean onCurrent = (mainGroup == Groups.Current);
        boolean onCalculate = (mainGroup == Groups.Calculate);
        boolean onCurrentCombined = (onCurrent && getMainPage() == Current.PageType.Combined);
        boolean onCurrentList = ((!onCurrentCombined && currentSubPage[Current.PageType.Passes] == Current.SubPageType.List) || (onCurrentCombined && currentSubPage[Current.PageType.Combined] == Current.SubPageType.List));
        boolean onCalculateViewNonInput = (onCalculate && calculateSubPage[Calculate.PageType.View] != Calculate.SubPageType.Input);
        boolean onCalculateViewLens = (onCalculate && calculateSubPage[Calculate.PageType.View] == Calculate.SubPageType.Lens);
        boolean onCalculatePassesNonInput = (onCalculate && calculateSubPage[Calculate.PageType.Passes] != Calculate.SubPageType.Input);
        boolean onCalculatePassesLens = (onCalculate && calculateSubPage[Calculate.PageType.Passes] == Calculate.SubPageType.Lens);
        boolean onCalculateCoordinatesNonInput = (onCalculate && calculateSubPage[Calculate.PageType.Coordinates] != Calculate.SubPageType.Input);
        boolean onCalculateIntersectionNonInput = (onCalculate && calculateSubPage[Calculate.PageType.Intersection] != Calculate.SubPageType.Input);
        boolean onCalculateIntersectionLens = (onCalculate && calculateSubPage[Calculate.PageType.Intersection] == Calculate.SubPageType.Lens);
        Bundle params;

        //update running tasks
        setTimer(run && (onCurrent || onCalculateViewLens || onCalculatePassesLens || onCalculateIntersectionLens));
        if(run)
        {
            if(onCurrent)
            {
                updateCurrentCalculations();
            }
            setCurrentPassCalculations(onCurrent && onCurrentList);
            if(onCalculateViewNonInput)
            {
                params = Calculate.PageAdapter.getParams(Calculate.PageType.View);
                if(params != null)
                {
                    setCalculateViewCalculations(true, new Database.SatelliteData(this, params.getInt(Calculate.ParamTypes.NoradId)), Calculations.julianDateCalendar(params.getLong(Calculate.ParamTypes.StartDateMs), observer), Calculations.julianDateCalendar(params.getLong(Calculate.ParamTypes.EndDateMs), observer), Calculate.getDayIncrement(params));
                }
            }
            if(onCalculatePassesNonInput)
            {
                params = Calculate.PageAdapter.getParams(Calculate.PageType.Passes);
                if(params != null)
                {
                    setCalculatePassCalculations(true, new Database.SatelliteData(this, params.getInt(Calculate.ParamTypes.NoradId)), Calculations.julianDateCalendar(params.getLong(Calculate.ParamTypes.StartDateMs), observer), Calculations.julianDateCalendar(params.getLong(Calculate.ParamTypes.EndDateMs), observer), params.getDouble(Calculate.ParamTypes.ElevationMinDegs, 0.0));
                }
            }
            if(onCalculateCoordinatesNonInput)
            {
                params = Calculate.PageAdapter.getParams(Calculate.PageType.Coordinates);
                if(params != null)
                {
                    setCalculateCoordinateCalculations(true, new Database.SatelliteData(this, params.getInt(Calculate.ParamTypes.NoradId)), Calculations.julianDateCalendar(params.getLong(Calculate.ParamTypes.StartDateMs), observer), Calculations.julianDateCalendar(params.getLong(Calculate.ParamTypes.EndDateMs), observer), Calculate.getDayIncrement(params));
                }
            }
            if(onCalculateIntersectionNonInput)
            {
                params = Calculate.PageAdapter.getParams(Calculate.PageType.Intersection);
                if(params != null)
                {
                    setCalculateIntersectionCalculations(true, new Database.SatelliteData(this, params.getInt(Calculate.ParamTypes.NoradId)), new Database.SatelliteData(this, params.getInt(Calculate.ParamTypes.NoradId2, Universe.IDs.Invalid)), Calculations.julianDateCalendar(params.getLong(Calculate.ParamTypes.StartDateMs), observer), Calculations.julianDateCalendar(params.getLong(Calculate.ParamTypes.EndDateMs), observer), params.getDouble(Calculate.ParamTypes.IntersectionDegs, 0.2), params.getDouble(Calculate.ParamTypes.ElevationMinDegs, 0.0));
                }
            }
        }
        if(!run)
        {
            setCurrentPassCalculations(false);
            setCurrentViewCalculations(false, 0);
            setCurrentCoordinateCalculations(false, 0);
            setCalculateViewCalculations(false, null, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
            setCalculatePassCalculations(false, null, Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE);
            setCalculateCoordinateCalculations(false, null, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
            setCalculateIntersectionCalculations(false, null, null, Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE);
            if(calculatePageAdapter != null)
            {
                //stop any running timer
                calculatePageAdapter.stopPlayTimer(mainPager, Calculate.PageType.Coordinates);
            }
        }
    }

    //Stops any running task on current page
    private void stopPageTask()
    {
        int page = getMainPage();

        //handle based on page
        switch(mainGroup)
        {
            case Groups.Calculate:
                switch(page)
                {
                    case Calculate.PageType.View:
                        setCalculateViewCalculations(false, null, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
                        break;

                    case Calculate.PageType.Passes:
                        setCalculatePassCalculations(false, null, Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE);
                        break;

                    case Calculate.PageType.Coordinates:
                        setCalculateCoordinateCalculations(false, null, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
                        break;

                    case Calculate.PageType.Intersection:
                        setCalculateIntersectionCalculations(false, null, null, Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE);
                        break;
                }
                break;
        }
    }

    //Creates drawer toggle
    private ActionBarDrawerToggle createActionBarDrawerToggle()
    {
        return(new ActionBarDrawerToggle(this, mainDrawerLayout, R.string.app_name, R.string.app_name)
        {
            public void onDrawerClosed(View drawerView)
            {
                super.onDrawerClosed(drawerView);
            }

            public void onDrawerOpened(View drawerView)
            {
                super.onDrawerOpened(drawerView);
            }
        });
    }

    //Creates a location receiver
    private LocationReceiver createLocationReceiver(LocationReceiver oldReceiver)
    {
        final Resources res = this.getResources();

        //if old receiver is set
        if(oldReceiver != null)
        {
            //remove it
            oldReceiver.unregister(this);
        }

        //create receiver
        LocationReceiver receiver = new LocationReceiver(LocationService.FLAG_START_GET_LOCATION | LocationService.FLAG_START_HIGH_POWER_ONCE)
        {
            @Override
            protected Activity getActivity()
            {
                return(MainActivity.this);
            }

            @Override
            protected View getParentView()
            {
                return(mainDrawerLayout);
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
            protected void onGotLocation(Context context, ObserverType updatedObserver)
            {
                boolean updated = (updatedObserver != null && (observer == null || observer.notEqual(updatedObserver)));
                boolean showStatus = (needSaveCurrentLocation || pendingLocationUpdate) && updated;
                boolean usingCurrent = (locationSource == Database.LocationType.Current);

                //if need to save current location
                if(needSaveCurrentLocation)
                {
                    //save location after resolving it
                    saveResolvedLocation(context, updatedObserver);
                }

                //if pending location update or using current location
                if(pendingLocationUpdate || usingCurrent)
                {
                    //update observer
                    observer = updatedObserver;
                    pendingLocationUpdate = false;

                    //if updated
                    if(updated)
                    {
                        //update displays
                        updateCurrentLocationDisplay();
                        if(Current.cameraView != null)
                        {
                            Current.cameraView.updateAzDeclination(observer);
                        }
                        switch(mainGroup)
                        {
                            case Groups.Current:
                                setCurrentPassCalculations(true);
                                break;

                            case Groups.Settings:
                                if(settingsPagerAdapter != null)
                                {
                                    runOnUiThread(new Runnable()
                                    {
                                        @Override
                                        public void run()
                                        {
                                            settingsPagerAdapter.notifyDataSetChanged();
                                        }
                                    });
                                }
                                break;
                        }
                        Current.Coordinates.setCurrentLocation(MainActivity.this, observer);
                        if(usingCurrent)
                        {
                            CalculateService.NotifyReceiver.updateNotifyLocations(MainActivity.this, observer, true, userChangedLocation);
                            userChangedLocation = false;
                        }
                    }
                }

                //if showing status
                if(showStatus)
                {
                    Globals.showSnackBar(mainDrawerLayout, res.getString(R.string.text_location_success));
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
                    updateMainPager(false);
                }
            }
        };

        //register and return receiver
        receiver.register(this);
        return(receiver);
    }

    //Creates an update receiver
    private UpdateReceiver createUpdateReceiver(UpdateReceiver oldReceiver)
    {
        //if old receiver is set
        if(oldReceiver != null)
        {
            //remove it
            oldReceiver.unregister(this);
        }

        //create receiver
        UpdateReceiver receiver = new UpdateReceiver()
        {
            @Override
            protected View getParentView()
            {
                return(mainDrawerLayout);
            }

            @Override
            protected MultiProgressDialog getDatabaseProgressDialog()
            {
                return(databaseProgress);
            }

            @Override
            protected void onLoadPending(ArrayList<Database.DatabaseSatellite> pendingLoadSatellites)
            {
                //if finished setup
                if(finishedSetup)
                {
                    //show dialog to load pending
                    Orbitals.showLoadDialog(MainActivity.this, mainPager, orbitalPageAdapter, pendingLoadSatellites, new EditValuesDialog.OnDismissListener()
                    {
                        @Override
                        public void onDismiss(int saveCount)
                        {
                            //if some were saved
                            if(saveCount > 0)
                            {
                                //update list
                                MainActivity.this.updateMainPager(true);
                            }
                        }
                    });
                }
            }

            @Override
            protected void onDatabaseUpdated()
            {
                //hide progress
                showDatabaseProgressDialog(false);

                //if finished user setup
                if(!runningUserSetup)
                {
                    //finish loading
                    loadStartData(savedStateBundle);
                }
            }

            @Override
            protected void onGotInformation(Spanned infoText, int index)
            {
                int page = getMainPage();

                //if finished setup
                if(finishedSetup)
                {
                    //update display
                    switch(index)
                    {
                        case Groups.Current:
                            Current.PageAdapter.notifyInformationChanged(page, infoText);
                            break;

                        case Groups.Calculate:
                            Calculate.PageAdapter.notifyInformationChanged(page, infoText);
                            break;

                        case Groups.Orbitals:
                            Orbitals.PageAdapter.notifyInformationChanged(page, infoText);
                            break;
                    }
                }
            }

            @Override
            protected void onGeneralUpdate(int progressType, byte updateType, boolean ended)
            {
                int page = getMainPage();

                //if button exists, on orbitals, and on satellites
                if(mainFloatingButton != null && mainGroup == Groups.Orbitals && page == Orbitals.PageType.Satellites)
                {
                    //update visibility
                    Globals.setVisible(mainFloatingButton, ended);

                    //if done and -loaded file or running in background-
                    if(ended && (updateType == UpdateService.UpdateType.LoadFile || UpdateService.getNotificationVisible(updateType)))
                    {
                        //update list
                        updateMainPager(true);
                    }
                }

                //update menu
                updateOptionsMenu(false);
            }
        };

        //register and return receiver
        receiver.register(this);
        return(receiver);
    }

    //Locks screen orientation
    private void lockScreenOrientation(boolean lock)
    {
        Globals.lockScreenOrientation(this, lock);
    }

    //Shows setup dialog
    private void showSetupDialog()
    {
        Intent setupIntent = new Intent(this, SetupActivity.class);

        runningUserSetup = true;
        finishedSetup = false;
        this.startActivityForResult(setupIntent, BaseInputActivity.RequestCode.Setup);
    }

    //Shows/dismisses database progress dialog
    private void showDatabaseProgressDialog(boolean show)
    {
        //if showing
        if(show)
        {
            databaseProgress = Globals.createProgressDialog(this);
            databaseProgress.setCancelable(false);
            databaseProgress.setTitle(R.string.title_database_building);
            databaseProgress.show();
        }
        else if(databaseProgress != null && databaseProgress.isShowing())
        {
            //hide it
            databaseProgress.dismiss();
        }
    }

    //Shows orbital view list
    private void showOrbitalViewList()
    {
        Intent intent = new Intent(this, MasterAddListActivity.class);
        intent.putExtra(MasterAddListActivity.ParamTypes.ListType, MasterAddListActivity.ListType.VisibleList);
        this.startActivityForResult(intent, BaseInputActivity.RequestCode.OrbitalViewList);
    }

    //Shows a filter dialog
    public void showFilterDialog()
    {
        final int page = getMainPage();
        final Resources res = this.getResources();

        Globals.showSelectDialog(this, res.getString(R.string.title_select_filter), AddSelectListAdapter.SelectType.Filter, new AddSelectListAdapter.OnItemClickListener()
        {
            @Override
            public void onItemClick(int which)
            {
                //handle based on source
                switch(which)
                {
                    case AddSelectListAdapter.FilterType.Sorting:
                        //show dialog
                        new EditValuesDialog(MainActivity.this, new EditValuesDialog.OnSaveListener()
                        {
                            @Override
                            public void onSave(int itemIndex, int id, String textValue, String text2Value, double number1, double number2, double number3, String listValue, String list2Value, long dateValue)
                            {
                                //update current sort by for page
                                Settings.setCurrentSortBy(MainActivity.this, page, list2Value);

                                //sort page and notify of change
                                Current.PageAdapter.sortItems(MainActivity.this, page, true);
                                Current.PageAdapter.notifyItemsChanged(page);
                            }
                        }).getSortBy(res.getString(R.string.title_sort_by), page);
                        break;

                    case AddSelectListAdapter.FilterType.Visibility:
                        //show view list
                        showOrbitalViewList();
                        break;
                }
            }
        });
    }

    //Shows a settings dialog
    public void showSettingsDialog()
    {
        final int page = getMainPage();
        final int subPage = getSubPage();
        Resources res = this.getResources();

        Globals.showSelectDialog(this, res.getString(R.string.title_settings), AddSelectListAdapter.SelectType.Settings, page, subPage, new AddSelectListAdapter.OnItemClickListener()
        {
            @Override
            public void onItemClick(int which)
            {
                //handle based on source
                switch(which)
                {
                    case AddSelectListAdapter.SettingsType.Settings:
                        //update display and page selection
                        setMainGroup(Groups.Settings, false);
                        setMainPage(Settings.PageType.Other);

                        //set sub page
                        switch(page)
                        {
                            case Current.PageType.View:
                            case Current.PageType.Passes:
                            case Current.PageType.Coordinates:
                            case Current.PageType.Combined:
                                setSubPage(Groups.Settings, Settings.PageType.Other, (subPage == Current.SubPageType.Lens ? Settings.SubPageType.LensView : Settings.SubPageType.MapView));
                                break;
                        }
                        break;

                    case AddSelectListAdapter.SettingsType.Visibility:
                        //show view list
                        showOrbitalViewList();
                        break;
                }
            }
        });
    }

    //Shows an edit location dialog
    private void showEditLocationDialog()
    {
        int index;
        final Resources res = this.getResources();
        ArrayList<Selectable.ListItem> selectedItems = settingsPagerAdapter.getSelectedItems(mainPager, Settings.PageType.Locations);
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
            public void onSave(int itemIndex, int id, String textValue, String text2Value, double number1, double number2, double number3, String listValue, String list2Value, long dateValue)
            {
                String zoneId = zones.get(Arrays.asList(zoneNames).indexOf(list2Value)).getID();
                double altitudeM = (Settings.getUsingMetric() ? number3 : (number3 / Globals.FEET_PER_METER));

                //save location
                Database.saveLocation(MainActivity.this, id, textValue, number1, number2, altitudeM, zoneId, Database.LocationType.Online);

                //if the selected location
                if(idChecked[itemIndex])
                {
                    //update observer
                    updateObserver(number1, number2, altitudeM / 1000, zoneId, Database.LocationType.Online, false);
                }
            }
        }, new EditValuesDialog.OnDismissListener()
        {
            @Override
            public void onDismiss(int saveCount)
            {
                //if any were saved
                if(saveCount > 0)
                {
                    //need update
                    settingsPagerAdapter.notifyDataSetChanged();
                }

                //end edit mode
                cancelEditMode(mainPager);
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
                        LocationService.getCurrentLocation(MainActivity.this, false, true, LocationService.PowerTypes.HighPowerThenBalanced);
                        break;

                    case AddSelectListAdapter.LocationSourceType.Custom:
                    case AddSelectListAdapter.LocationSourceType.Search:
                        MapLocationInputActivity.show(MainActivity.this, which == AddSelectListAdapter.LocationSourceType.Search);
                        break;
                }
            }
        });
    }

    //Shows getting location display
    private void showLocationGettingDisplay()
    {
        Toast.makeText(this, this.getResources().getString(R.string.title_location_getting), Toast.LENGTH_SHORT).show();
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
                            Globals.askGoogleDriveAccount(MainActivity.this, BaseInputActivity.RequestCode.GoogleDriveAddAccount);
                            break;

                        case Globals.AccountType.Dropbox:
                            DropboxAccess.start(MainActivity.this);
                            break;

                        case Globals.AccountType.SpaceTrack:
                            Globals.showAccountLogin(MainActivity.this, which, new Globals.WebPageListener()
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
                                            MainActivity.this.runOnUiThread(new Runnable()
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
                                        Settings.removeLogin(MainActivity.this, which);
                                    }
                                }
                            }, true);
                            break;
                    }
                }
            });
        }
    }

    //Shows a confirm update dialog
    private void showConfirmUpdateDialog()
    {
        Resources res = this.getResources();
        final int page = Orbitals.PageType.Satellites;

        Globals.showConfirmDialog(this, res.getString(R.string.title_satellites_update_all) + " (" + orbitalPageAdapter.getPageItemCount(mainPager, page) + ")?", null, res.getString(R.string.title_ok), res.getString(R.string.title_cancel), true, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int which)
            {
                //select and update all satellites
                orbitalPageAdapter.updateSelectedItems(mainPager, page);
            }
        },
        null,
        new DialogInterface.OnDismissListener()
        {
            @Override
            public void onDismiss(DialogInterface dialogInterface)
            {
                //make sure no items are selected
                orbitalPageAdapter.selectItems(mainPager, page, false);
            }
        });
    }

    //Shows a save satellites to file dialog
    private void showSaveSatellitesFileDialog()
    {
        Resources res = this.getResources();

        Globals.showSelectDialog(this, res.getString(R.string.title_save_format), AddSelectListAdapter.SelectType.SaveAs, new AddSelectListAdapter.OnItemClickListener()
        {
            @Override
            public void onItemClick(final int which)
            {
                boolean isBackup = which == Globals.FileType.Backup;
                final int page = Orbitals.PageType.Satellites;
                String fileType = (isBackup ? Globals.FileExtensionType.JSON : Globals.FileExtensionType.TLE);
                Resources res = MainActivity.this.getResources();

                new EditValuesDialog(MainActivity.this, new EditValuesDialog.OnSaveListener()
                {
                    @Override
                    public void onSave(int itemIndex, int id, String textValue, String text2Value, double number1, double number2, double number3, String listValue, String list2Value, long dateValue)
                    {
                        //save all satellites
                        orbitalPageAdapter.saveFileSelectedItems(mainPager, page, textValue, listValue, which, Globals.getFileSource(MainActivity.this, list2Value));
                    }
                }, new EditValuesDialog.OnDismissListener()
                {
                    @Override
                    public void onDismiss(int saveCount)
                    {
                        //make sure no items are selected
                        orbitalPageAdapter.selectItems(mainPager, page, false);
                    }
                }).getFileLocation(res.getString(R.string.title_satellites_save_file), new int[]{0}, new String[]{res.getString(R.string.title_satellites) + (isBackup ? (" " + res.getString(R.string.title_backup)) : "")}, new String[]{fileType}, new String[]{fileType}, new String[]{res.getString(R.string.title_downloads)});
            }
        });
    }

    //Shows a save calculate page to file dialog
    private void showSaveCalculatePageFileDialog(final int calculatePageType)
    {
        //if adapter exists and has items
        if(Calculate.PageAdapter.hasItems(calculatePageType))
        {
            //if there are params
            Bundle params = Calculate.PageAdapter.getParams(calculatePageType);
            if(params != null)
            {
                String fileName = null;
                Resources res = this.getResources();
                Database.SatelliteData satellite = new Database.SatelliteData(this, params.getInt(Calculate.ParamTypes.NoradId));
                Database.SatelliteData satellite2 = new Database.SatelliteData(this, params.getInt(Calculate.ParamTypes.NoradId2, Universe.IDs.Invalid));
                String satelliteName = satellite.getName();

                switch(calculatePageType)
                {
                    case Calculate.PageType.View:
                        fileName = satelliteName + " " + res.getString(R.string.title_views);
                        break;

                    case Calculate.PageType.Passes:
                        fileName = satelliteName + " " + res.getString(R.string.title_passes);
                        break;

                    case Calculate.PageType.Coordinates:
                        fileName = satelliteName + " " + res.getString(R.string.abbrev_coordinates);
                        break;

                    case Calculate.PageType.Intersection:
                        fileName = satelliteName + " " + satellite2.getName() + " " + res.getString(R.string.title_intersection);
                        break;
                }
                if(fileName == null)
                {
                    fileName = "save.txt";
                }

                new EditValuesDialog(this, new EditValuesDialog.OnSaveListener()
                {
                    @Override
                    public void onSave(int itemIndex, int id, String textValue, String text2Value, double number1, double number2, double number3, String listValue, String list2Value, long dateValue)
                    {
                        saveCalculateFile(calculatePageType, textValue, listValue, Globals.getFileSource(MainActivity.this, list2Value));
                    }
                }).getFileLocation(res.getString(R.string.title_save_file), new int[]{0}, new String[]{fileName}, new String[]{Globals.FileExtensionType.TXT, Globals.FileExtensionType.CSV}, new String[]{Globals.FileExtensionType.TXT}, new String[]{res.getString(R.string.title_downloads)});
            }
        }
    }

    //Handles main floating button click
    private void handleMainFloatingButtonClick(boolean isLong)
    {
        int page = getMainPage();

        //handle based on group
        switch(mainGroup)
        {
            case Groups.Orbitals:
                if(inEditMode)
                {
                    Orbitals.showEditDialog(this, mainPager, orbitalPageAdapter);
                }
                else
                {
                    Orbitals.showAddDialog(this, mainDrawerLayout, isLong);
                }
                break;

            case Groups.Settings:
                switch(page)
                {
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
                        NotifySettingsActivity.show(this, observer);
                        break;

                    case Settings.PageType.Other:
                        switch(settingsSubPage[page])
                        {
                            case Settings.SubPageType.Accounts:
                                //if adapter exists
                                if(accountsListAdapter != null)
                                {
                                    if(inEditMode)
                                    {
                                        //get selected items
                                        ArrayList<Selectable.ListItem> selectedItems = settingsPagerAdapter.getSelectedItems(mainPager, page);

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
                                }
                                break;
                        }
                        break;
                }
                break;
        }
    }

    //Creates an on item detail button click listener
    private Selectable.ListFragment.OnItemDetailButtonClickListener createOnItemDetailButtonClickListener()
    {
        return(new Selectable.ListFragment.OnItemDetailButtonClickListener()
        {
            @Override
            public void onClick(final int group, final int pageType, int itemID, Selectable.ListItem item, int buttonNum)
            {
                final Activity activity = MainActivity.this;

                //handle based on button
                switch(buttonNum)
                {
                    case Selectable.ListBaseAdapter.DetailButtonType.LensView:
                        switch(group)
                        {
                            case Groups.Current:
                                switch(pageType)
                                {
                                    case Current.PageType.View:
                                    case Current.PageType.Combined:
                                        //update sub page and norad ID
                                        setSubPage(group, pageType, Current.SubPageType.Lens);
                                        viewLensNoradID = itemID;
                                        setMainGroup(mainGroup, true);
                                        break;

                                    case Current.PageType.Passes:
                                        //update sub page and norad ID
                                        setSubPage(group, pageType, Current.SubPageType.Lens);
                                        passesLensNoradID = itemID;
                                        setMainGroup(mainGroup, true);
                                        break;
                                }
                                break;

                            case Groups.Calculate:
                                switch(pageType)
                                {
                                    case Calculate.PageType.Passes:
                                    case Calculate.PageType.Intersection:
                                        //update sub page and norad ID
                                        setSubPage(group, pageType, Calculate.SubPageType.Lens);
                                        if(pageType == Calculate.PageType.Intersection)
                                        {
                                            intersectionPassIndex = item.listIndex;
                                        }
                                        else
                                        {
                                            passesPassIndex = item.listIndex;
                                        }
                                        setMainGroup(mainGroup, true);
                                        break;
                                }
                                break;
                        }
                        break;

                    case Selectable.ListBaseAdapter.DetailButtonType.MapView:
                    case Selectable.ListBaseAdapter.DetailButtonType.GlobeView:
                        switch(group)
                        {
                            case Groups.Current:
                                switch(pageType)
                                {
                                    case Current.PageType.Coordinates:
                                    case Current.PageType.Combined:
                                        //update sub page and norad ID
                                        setSubPage(group, pageType, (buttonNum == Selectable.ListBaseAdapter.DetailButtonType.GlobeView ? Current.SubPageType.Globe : Current.SubPageType.Map));
                                        mapViewNoradID = itemID;
                                        setMainGroup(mainGroup, true);
                                        break;
                                }
                                break;
                        }
                        break;

                    case Selectable.ListBaseAdapter.DetailButtonType.Notify:
                        //show pass notification settings
                        NotifySettingsActivity.show(activity, itemID, observer);
                        break;

                    case Selectable.ListBaseAdapter.DetailButtonType.Graph:
                    case Selectable.ListBaseAdapter.DetailButtonType.Preview3d:
                    case Selectable.ListBaseAdapter.DetailButtonType.Info:
                        //get orbital
                        final Database.SatelliteData currentOrbital = new Database.SatelliteData(activity, itemID);

                        //handle based on button again
                        switch(buttonNum)
                        {
                            case Selectable.ListBaseAdapter.DetailButtonType.Info:
                                //get information
                                UpdateService.getInformation(activity, group, currentOrbital.satellite.tle);
                                break;

                            case Selectable.ListBaseAdapter.DetailButtonType.Preview3d:
                                switch(group)
                                {
                                    case Groups.Current:
                                        Current.PageAdapter.notifyPreview3dChanged(pageType, itemID);
                                        break;

                                    case Groups.Orbitals:
                                        Orbitals.PageAdapter.notifyPreview3dChanged(pageType, itemID);
                                        break;
                                }
                                break;

                            case Selectable.ListBaseAdapter.DetailButtonType.Graph:
                                //remember current item and dates
                                CalculateService.PassData currentItem = (CalculateService.PassData)item;
                                final double julianStartDate = Calculations.julianDateCalendar(currentItem.passTimeStart);
                                final double julianEndDate = Calculations.julianDateCalendar(currentItem.passTimeEnd);
                                final double dayIncrement = (currentItem.passTimeEnd.getTimeInMillis() - currentItem.passTimeStart.getTimeInMillis()) / Calculations.MsPerDay / 100;
                                final Database.SatelliteData currentOrbital2 = (currentItem.satellite2 != null ? new Database.SatelliteData(activity, currentItem.satellite2.getSatelliteNum()) : null);

                                //calculate views
                                Calculate.calculateViews(activity, currentOrbital, null, observer, julianStartDate, julianEndDate, dayIncrement, new CalculateViewsTask.OnProgressChangedListener()
                                {
                                    @Override
                                    public void onProgressChanged(int index, int progressType, final ArrayList<CalculateViewsTask.OrbitalView> pathPoints)
                                    {
                                        switch(progressType)
                                        {
                                            case Globals.ProgressType.Success:
                                            case Globals.ProgressType.Failed:
                                                switch(group)
                                                {
                                                    case Groups.Current:
                                                        Current.PageAdapter.notifyGraphChanged(pageType, currentOrbital, pathPoints);
                                                        break;

                                                    case Groups.Calculate:
                                                        if(progressType == Globals.ProgressType.Success && pageType == Calculate.PageType.Intersection && currentOrbital2 != null)
                                                        {
                                                            //calculate remaining views
                                                            Calculate.calculateViews(activity, currentOrbital2, null, observer, julianStartDate, julianEndDate, dayIncrement, new CalculateViewsTask.OnProgressChangedListener()
                                                            {
                                                                @Override
                                                                public void onProgressChanged(int index, int progressType, ArrayList<CalculateViewsTask.OrbitalView> path2Points)
                                                                {
                                                                    switch(progressType)
                                                                    {
                                                                        case Globals.ProgressType.Success:
                                                                        case Globals.ProgressType.Failed:
                                                                            Calculate.PageAdapter.notifyGraphChanged(pageType, currentOrbital, pathPoints, currentOrbital2, path2Points);
                                                                            break;
                                                                    }
                                                                }
                                                            });
                                                        }
                                                        else
                                                        {
                                                            Calculate.PageAdapter.notifyGraphChanged(pageType, currentOrbital, pathPoints, null, null);
                                                        }
                                                        break;
                                                }
                                                break;
                                        }
                                    }
                                });
                                break;
                        }
                        break;
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
            public void setAdapter(final int group, final int position, RecyclerView.Adapter adapter)
            {
                int orbitalId;
                Bundle params;

                switch(group)
                {
                    case Groups.Current:
                        switch(position)
                        {
                            case Current.PageType.Passes:
                            case Current.PageType.Combined:
                                //get passes
                                setCurrentPassCalculations(true);
                                break;
                        }
                        break;

                    case Groups.Calculate:
                        switch(position)
                        {
                            case Calculate.PageType.View:
                                Current.ViewAngles.Item[] viewItems = null;
                                params = Calculate.PageAdapter.getParams(Calculate.PageType.View);

                                //if adapter already exists
                                if(Calculate.PageAdapter.hasItems(Calculate.PageType.View))
                                {
                                    //remember items
                                    viewItems = Calculate.PageAdapter.getViewAngleItems();
                                }

                                //set adapter
                                if(viewItems != null && viewItems.length > 0)
                                {
                                    Calculate.PageAdapter.setViewItems(viewItems);
                                    Calculate.PageAdapter.notifyItemsChanged(Calculate.PageType.View);
                                }
                                if(params != null)
                                {
                                    orbitalId = params.getInt(Calculate.ParamTypes.NoradId);
                                    Calculate.PageAdapter.notifyHeaderChanged(Calculate.PageType.View, orbitalId, Globals.getHeaderText(MainActivity.this, new Database.SatelliteData(MainActivity.this, orbitalId).getName(), Calculations.julianDateCalendar(params.getLong(Calculate.ParamTypes.StartDateMs), observer), Calculations.julianDateCalendar(params.getLong(Calculate.ParamTypes.EndDateMs), observer)));
                                }

                                //recreate tasks
                                updateRunningTasks(true);
                                break;

                            case Calculate.PageType.Passes:
                            case Calculate.PageType.Intersection:
                                boolean forIntersection = (position == Calculate.PageType.Intersection);
                                Current.Passes.Item[] passItems = null;
                                params = Calculate.PageAdapter.getParams(position);

                                //if adapter already exists
                                if(Calculate.PageAdapter.hasItems(position))
                                {
                                    //remember items
                                    passItems = (forIntersection ? Calculate.PageAdapter.getIntersectionItems() : Calculate.PageAdapter.getPassItems());
                                }

                                //set adapter
                                if(passItems != null && passItems.length > 0)
                                {
                                    if(forIntersection)
                                    {
                                        Calculate.PageAdapter.setIntersectionItems(new Current.Passes.Item[0]);
                                        Calculate.PageAdapter.addIntersectionItems(passItems, 0);
                                    }
                                    else
                                    {
                                        Calculate.PageAdapter.setPassItems(new Current.Passes.Item[0]);
                                        Calculate.PageAdapter.addPassItems(passItems, 0);
                                    }
                                }
                                if(params != null)
                                {
                                    orbitalId = params.getInt(Calculate.ParamTypes.NoradId);
                                    Calculate.PageAdapter.notifyHeaderChanged(position, orbitalId, Globals.getHeaderText(MainActivity.this, new Database.SatelliteData(MainActivity.this, orbitalId).getName(), Calculations.julianDateCalendar(params.getLong(Calculate.ParamTypes.StartDateMs), observer), Calculations.julianDateCalendar(params.getLong(Calculate.ParamTypes.EndDateMs), observer), (forIntersection ? params.getDouble(Calculate.ParamTypes.IntersectionDegs, -Double.MAX_VALUE) : -Double.MAX_VALUE)));
                                }

                                //recreate tasks
                                updateRunningTasks(true);
                                break;

                            case Calculate.PageType.Coordinates:
                                Current.Coordinates.Item[] coordinateItems = null;
                                params = Calculate.PageAdapter.getParams(Calculate.PageType.Coordinates);

                                //if adapter already exists
                                if(Calculate.PageAdapter.hasItems(Calculate.PageType.Coordinates))
                                {
                                    //remember items
                                    coordinateItems = Calculate.PageAdapter.getCoordinatesItems();
                                }

                                //set adapter
                                if(coordinateItems != null && coordinateItems.length > 0)
                                {
                                    Calculate.PageAdapter.setCoordinateItems(coordinateItems);
                                    Calculate.PageAdapter.notifyItemsChanged(Calculate.PageType.Coordinates);
                                }
                                if(params != null)
                                {
                                    orbitalId = params.getInt(Calculate.ParamTypes.NoradId);
                                    Calculate.PageAdapter.notifyHeaderChanged(Calculate.PageType.Coordinates, orbitalId, Globals.getHeaderText(MainActivity.this, new Database.SatelliteData(MainActivity.this, orbitalId).getName(), Calculations.julianDateCalendar(params.getLong(Calculate.ParamTypes.StartDateMs), observer), Calculations.julianDateCalendar(params.getLong(Calculate.ParamTypes.EndDateMs), observer)));
                                }

                                //recreate tasks
                                updateRunningTasks(true);
                                break;
                        }
                        break;

                    case Groups.Settings:
                        switch(position)
                        {
                            case Settings.PageType.Locations:
                                //set adapter
                                settingsLocationsListAdapter = (Settings.Locations.ItemListAdapter)adapter;

                                //create receiver
                                locationReceiver = createLocationReceiver(locationReceiver);

                                //if using current location
                                if(locationSource == Database.LocationType.Current)
                                {
                                    updateCurrentLocationDisplay();
                                }
                                break;

                            case Settings.PageType.Other:
                                switch(settingsSubPage[Settings.PageType.Other])
                                {
                                    case Settings.SubPageType.Accounts:
                                        //set adapter
                                        accountsListAdapter = (Settings.Options.Accounts.ItemListAdapter)adapter;
                                        break;
                                }
                                break;
                        }
                        break;
                }
            }
        });
    }

    private Calculate.OnStartCalculationListener createOnStartCalculationListener()
    {
        return(new Calculate.OnStartCalculationListener()
        {
            @Override
            public void onStartCalculation(Bundle params)
            {
                //get params
                int page = params.getInt(Selectable.ParamTypes.PageNumber);
                int subPage = params.getInt(Selectable.ParamTypes.SubPageNumber);
                int objectId = params.getInt(Calculate.ParamTypes.NoradId);
                int object2Id = params.getInt(Calculate.ParamTypes.NoradId2, Universe.IDs.Invalid);
                long startDateMs = params.getLong(Calculate.ParamTypes.StartDateMs);
                long endDateMs = params.getLong(Calculate.ParamTypes.EndDateMs);
                double dayIncrement = 1;
                double intersection = params.getDouble(Calculate.ParamTypes.IntersectionDegs, 0.2);
                double elevationMin = params.getDouble(Calculate.ParamTypes.ElevationMinDegs, 0.0);
                final double julianDateStart = Calculations.julianDateCalendar(startDateMs, observer);
                final double julianDateEnd = Calculations.julianDateCalendar(endDateMs, observer);
                final Database.SatelliteData satellite = new Database.SatelliteData(MainActivity.this, objectId);
                final Database.SatelliteData satellite2 = new Database.SatelliteData(MainActivity.this, object2Id);

                //if not waiting on a location update
                if(!pendingLocationUpdate)
                {
                    //params based on page
                    switch(page)
                    {
                        case Calculate.PageType.View:
                        case Calculate.PageType.Coordinates:
                            //get day increment
                            dayIncrement = Calculate.getDayIncrement(params);
                            break;
                    }

                    //update sub page and params
                    setSubPage(Groups.Calculate, page, subPage);
                    Calculate.PageAdapter.setParams(page, params);

                    //update display
                    updateMainPager(false);
                    updateOptionsMenu();

                    //calculation based on page
                    switch(page)
                    {
                        case Calculate.PageType.View:
                            switch(subPage)
                            {
                                case Calculate.SubPageType.List:
                                    //start calculating
                                    setCalculateViewCalculations(true, satellite, julianDateStart, julianDateEnd, dayIncrement);
                                    break;
                            }
                            break;

                        case Calculate.PageType.Passes:
                            switch(subPage)
                            {
                                case Calculate.SubPageType.List:
                                    //reset last inputs and start calculating
                                    setCalculatePassCalculations(true, satellite, julianDateStart, julianDateEnd, elevationMin);
                                    break;
                            }
                            break;

                        case Calculate.PageType.Coordinates:
                            switch(subPage)
                            {
                                case Calculate.SubPageType.List:
                                    //start calculating
                                    setCalculateCoordinateCalculations(true, satellite, julianDateStart, julianDateEnd, dayIncrement);
                                    break;
                            }
                            break;

                        case Calculate.PageType.Intersection:
                            switch(subPage)
                            {
                                case Calculate.SubPageType.List:
                                    //reset last inputs and start calculating
                                    setCalculateIntersectionCalculations(true, satellite, satellite2, julianDateStart, julianDateEnd, intersection, elevationMin);
                                    break;
                            }
                            break;
                    }
                }
                else
                {
                    //show pending location display
                    showLocationGettingDisplay();
                }
            }
        });
    }

    //Creates an on page set listener
    private Calculate.OnPageSetListener createOnPageSetListener()
    {
        return(new Calculate.OnPageSetListener()
        {
            @Override
            public void onPageSet(Calculate.Page page, int pageNum, int subPageNum)
            {
                switch(pageNum)
                {
                    case Calculate.PageType.View:
                    case Calculate.PageType.Passes:
                    case Calculate.PageType.Coordinates:
                    case Calculate.PageType.Intersection:
                        //if getting input
                        if(subPageNum == Calculate.SubPageType.Input)
                        {
                            page.setOnStartCalculationListener(startCalculationListener);
                        }
                        break;
                }
            }
        });
    }

    //Creates an on paged changed listener
    private SwipeStateViewPager.OnPageChangeListener createOnPageChangedListener()
    {
        return(new SwipeStateViewPager.OnPageChangeListener()
        {
            private int lastPosition = 0;

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            @Override
            public void onPageSelected(int position)
            {
                boolean stopCamera = false;
                boolean startCamera = false;
                boolean allowSwipe = true;

                cancelEditMode(mainPager);
                updateOptionsMenu();
                switch(mainGroup)
                {
                    case Groups.Current:
                        switch(lastPosition)
                        {
                            case Current.PageType.View:
                            case Current.PageType.Passes:
                                //if were using camera
                                if(currentSubPage[lastPosition] == Current.SubPageType.Lens)
                                {
                                    //make sure camera stops
                                    stopCamera = true;
                                }
                                break;
                        }

                        switch(position)
                        {
                            case Current.PageType.View:
                            case Current.PageType.Passes:
                            case Current.PageType.Combined:
                                //if now using camera
                                if(currentSubPage[position] == Current.SubPageType.Lens)
                                {
                                    //start camera
                                    startCamera = true;
                                }

                                //if not combined
                                if(position != Current.PageType.Combined)
                                {
                                    //stop
                                    break;
                                }
                                //else fall through

                            case Current.PageType.Coordinates:
                                //don't allow swiping if on map/globe
                                allowSwipe = (currentSubPage[position] != Current.SubPageType.Map && currentSubPage[position] != Current.SubPageType.Globe);
                                break;
                        }

                        //update current calculation status
                        updateCurrentCalculations();
                        break;

                    case Groups.Calculate:
                        switch(lastPosition)
                        {
                            case Calculate.PageType.View:
                            case Calculate.PageType.Passes:
                            case Calculate.PageType.Intersection:
                                //if were using camera
                                if(calculateSubPage[lastPosition] == Calculate.SubPageType.Lens)
                                {
                                    //make sure camera stops
                                    stopCamera = true;
                                }
                                break;

                            case Calculate.PageType.Coordinates:
                                //if adapter exists
                                if(calculatePageAdapter != null)
                                {
                                    //stop any running timer
                                    calculatePageAdapter.stopPlayTimer(mainPager, Calculate.PageType.Coordinates);
                                }

                                //don't allow swiping if on map/globe
                                allowSwipe = (calculateSubPage[position] != Calculate.SubPageType.Map && calculateSubPage[position] != Calculate.SubPageType.Globe);
                                break;
                        }

                        switch(position)
                        {
                            case Calculate.PageType.View:
                            case Calculate.PageType.Passes:
                            case Calculate.PageType.Intersection:
                                //if now using camera
                                if(calculateSubPage[position] == Calculate.SubPageType.Lens)
                                {
                                    //start camera
                                    startCamera = true;
                                }
                                break;
                        }
                        break;
                }

                //if need to stop or start camera and camera exists
                if((stopCamera || startCamera) && Current.cameraView != null)
                {
                    //if need to stop camera
                    if(stopCamera)
                    {
                        //stop camera
                        Current.cameraView.stopCamera(true);
                    }

                    //if need to start camera
                    if(startCamera)
                    {
                        //start camera
                        Current.cameraView.startCamera();
                    }
                }

                //update last position
                lastPosition = position;

                //update pager swipe
                mainPager.setSwipeEnabled(allowSwipe);

                //if user was dragging
                if(wasDraggingPage)
                {
                    //update previous sub page
                    previousPage = getMainPage();
                    previousGroup = mainGroup;
                    setPreviousSubPage();

                    //reset
                    wasDraggingPage = false;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state)
            {
                //if user is dragging
                if(state == ViewPager.SCROLL_STATE_DRAGGING)
                {
                    //were dragging
                    wasDraggingPage = true;
                }
            }
        });
    }

    //Creates an on edit mode changed listener
    private Selectable.ListFragment.OnEditModeChangedListener createOnEditModeChangedListener()
    {
        return(new Selectable.ListFragment.OnEditModeChangedListener()
        {
            @Override
            public void editModeChanged(boolean editMode)
            {
                inEditMode = editMode;
                updateMainFloatingButton();
            }
        });
    }

    private Selectable.ListFragment.OnUpdateNeededListener createOnUpdateNeededListener()
    {
        return(new Selectable.ListFragment.OnUpdateNeededListener()
        {
            @Override
            public void updateNeeded()
            {
                updateMainPager(true);
            }
        });
    }

    private Selectable.ListFragment.OnUpdatePageListener createOnUpdatePageListener()
    {
        return(new Selectable.ListFragment.OnUpdatePageListener()
        {
            @Override
            public void updatePage(int page, int subPage)
            {
                switch(mainGroup)
                {
                    case Groups.Settings:
                        switch(page)
                        {
                            case Settings.PageType.Other:
                                setSubPage(Groups.Settings, page, subPage);
                                setMainGroup(mainGroup, true);
                                break;
                        }
                        break;
                }
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
                page.setOnEditModeChangedListener(editModeChangedListener);

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

    //Creates an on item selected listener
    private Selectable.ListFragment.OnItemSelectedListener createOnItemSelectedListener()
    {
        return(new Selectable.ListFragment.OnItemSelectedListener()
        {
            @Override
            public void itemSelected(int group, int page, int subPage, int position, boolean selected)
            {
                switch(group)
                {
                    case Groups.Settings:
                        switch(page)
                        {
                            case Settings.PageType.Other:
                                switch(subPage)
                                {
                                    case Settings.SubPageType.Accounts:
                                        //update floating button
                                        updateMainFloatingButton();
                                        break;
                                }
                                break;
                        }
                        break;
                }
            }
        });
    }

    //Creates an on settings item check changed listener
    private Selectable.ListFragment.OnItemCheckChangedListener createOnSettingsItemCheckChangedListener()
    {
        return(new Selectable.ListFragment.OnItemCheckChangedListener()
        {
            @Override
            public void itemCheckedChanged(int page, Selectable.ListItem item)
            {
                switch(page)
                {
                    case Settings.PageType.Locations:
                        //remember current item and if current location
                        Settings.Locations.Item currentItem = (Settings.Locations.Item)item;
                        boolean isCurrentLocation = (currentItem.locationType == Database.LocationType.Current);

                        //if now checked
                        if(item.isChecked)
                        {
                            //update status
                            userChangedLocation = true;

                            //update observer and checked in database
                            Database.saveLocation(MainActivity.this, currentItem.name, currentItem.locationType, true);
                            if(isCurrentLocation)
                            {
                                //reset item
                                currentItem.latitude = currentItem.longitude = currentItem.altitudeM = 0;
                            }
                            updateObserver(currentItem.latitude, currentItem.longitude, currentItem.altitudeM / 1000, currentItem.zone.getID(), currentItem.locationType, true);
                        }
                        //else if now unchecked and the current location
                        else if(isCurrentLocation)
                        {
                            //stop location updates
                            pendingLocationUpdate = false;
                        }
                        break;
                }
            }
        });
    }

    //Creates an on child click listener
    private ExpandableListView.OnChildClickListener createOnSideMenuChildClickListener()
    {
        return(new ExpandableListView.OnChildClickListener()
        {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id)
            {
                //update display, page selection, and close menu
                setMainGroup(groupPosition, false);
                setMainPage(childPosition);
                mainDrawerLayout.closeDrawers();

                //click handled
                return(true);
            }
        });
    }

    //Creates an on group expand listener
    private ExpandableListView.OnGroupExpandListener createOnSideMenuGroupExpandListener(final ExpandableListView sideMenu)
    {
        return(new ExpandableListView.OnGroupExpandListener()
        {
            @Override
            public void onGroupExpand(int groupPosition)
            {
                //if there was a last position and it changed
                if(lastSideMenuPosition >= 0 && groupPosition != lastSideMenuPosition)
                {
                    //close old
                    sideMenu.collapseGroup(lastSideMenuPosition);
                }

                //update last position
                lastSideMenuPosition = groupPosition;
            }
        });
    }

    //Cancels edit mode
    private static void cancelEditMode(SwipeStateViewPager pager)
    {
        if(inEditMode)
        {
            PagerAdapter currentAdapter = pager.getAdapter();

            if(currentAdapter instanceof Selectable.ListFragmentAdapter)
            {
                ((Selectable.ListFragmentAdapter)currentAdapter).cancelEditMode(pager);
            }
        }
    }

    //Creates timer runnable for current
    private Runnable createCurrentRunnable()
    {
        final Globals.BoolObject firstRun = new Globals.BoolObject(true);
        final Globals.DoubleObject sunLastEl = new Globals.DoubleObject(Double.MAX_VALUE);
        final Globals.LongObject lastSystemElapsedSeconds = new Globals.LongObject(SystemClock.elapsedRealtime() / 1000);

        return(new Runnable()
        {
            private int lastNoradId = Universe.IDs.None;

            @Override
            public void run()
            {
                int index;
                int currentNoradId;
                int page = getMainPage();
                int subPage = currentSubPage[page];
                boolean updateElapsed = false;
                boolean onView = (page == Current.PageType.View);
                boolean onPasses = (page == Current.PageType.Passes);
                boolean onCoordinates = (page == Current.PageType.Coordinates);
                boolean onCombined = (page == Current.PageType.Combined);
                boolean allViewOrbitals = (viewLensNoradID == Integer.MAX_VALUE);
                boolean allPassesOrbitals = (passesLensNoradID == Integer.MAX_VALUE);
                boolean allMapOrbitals = (mapViewNoradID == Integer.MAX_VALUE);
                boolean onMap = (subPage == Current.SubPageType.Map || subPage == Current.SubPageType.Globe);
                boolean onList = (subPage == Current.SubPageType.List);
                boolean onLens = (subPage == Current.SubPageType.Lens);
                boolean[] updateList = new boolean[Current.PageType.PageCount];
                long currentSystemElapsedSeconds = (SystemClock.elapsedRealtime() / 1000);
                long travelSeconds = (currentSystemElapsedSeconds - lastSystemElapsedSeconds.value);
                double phase;
                double julianDate = Calculations.julianDateCalendar(Globals.getGMTTime());
                String coordinateString = null;
                TopographicDataType topoData;
                TopographicDataType[] lookAngles = new TopographicDataType[currentSatellites.length];
                TopographicDataType[] selectedLookAngles = null;
                Database.SatelliteData[] selectedSatellites = null;

                //go through each page
                for(index = 0; index < Current.PageType.PageCount; index++)
                {
                    //setup
                    updateList[index] = false;
                }

                //if not waiting on location update
                if(!pendingLocationUpdate)
                {
                    //if enough time has passed
                    if(travelSeconds >= 2)
                    {
                        //update later
                        updateElapsed = true;
                    }

                    //if showing current location on map
                    if(onMap && Current.Coordinates.currentLocationMarker != null && Current.Coordinates.currentLocationMarker.getInfoVisible())
                    {
                        //update info display
                        coordinateString = Globals.getCoordinateString(MainActivity.this, observer.geo.latitude, observer.geo.longitude, observer.geo.altitudeKm);
                        Current.Coordinates.currentLocationMarker.setInfoLocation();
                        Current.Coordinates.currentLocationMarker.setInfoVisible(true);
                    }

                    //go through each satellite
                    for(index = 0; index < currentSatellites.length; index++)
                    {
                        //get current satellite, location, and look angle
                        boolean changedEnough = false;
                        double illumination = 0;
                        String phaseName = null;
                        SatelliteObjectType currentOrbital = currentSatellites[index].satellite;
                        currentNoradId = currentOrbital.getSatelliteNum();
                        currentOrbital = Calculations.updateOrbitalPosition(currentOrbital, observer, julianDate, true);
                        topoData = Calculations.getLookAngles(observer, currentOrbital, true);

                        //if the moon
                        if(currentNoradId == Universe.IDs.Moon)
                        {
                            //get phase and name
                            phase = Universe.Moon.getPhase(System.currentTimeMillis());
                            illumination = Universe.Moon.getIllumination(phase);
                            phaseName = Universe.Moon.getPhaseName(MainActivity.this, phase);
                        }
                        //else if the sun
                        else if(currentNoradId == Universe.IDs.Sun)
                        {
                            //if have last elevation
                            if(sunLastEl.value != Double.MAX_VALUE)
                            {
                                //if changed enough
                                changedEnough = (Math.abs(topoData.elevation - sunLastEl.value) >= Universe.Sun.MinElevationPhaseChange);
                                if(changedEnough)
                                {
                                    //get phase name
                                    phaseName = Universe.Sun.getPhaseName(MainActivity.this, topoData.elevation, (topoData.elevation > sunLastEl.value));
                                }
                            }

                            //if -had no last elevation- or -changed enough-
                            if(sunLastEl.value == Double.MAX_VALUE || changedEnough)
                            {
                                //update last elevation
                                sunLastEl.value = topoData.elevation;
                            }
                        }

                        //if not a satellite
                        if(currentNoradId <= 0)
                        {
                            //if first run
                            if(firstRun.value)
                            {
                                //set saved geo
                                currentOrbital.savedGeo = new GeodeticDataType(currentOrbital.geo);
                            }

                            //if enough time has passed
                            if(updateElapsed)
                            {
                                //update speed and saved geo
                                currentOrbital.geo.speedKmS = Calculations.getDistanceKm(currentOrbital.savedGeo, currentOrbital.geo) / travelSeconds;
                                currentOrbital.savedGeo = new GeodeticDataType(currentOrbital.geo);
                            }
                        }

                        //if not using all satellites and satellite number matches
                        if(((onView || onCombined) && !allViewOrbitals && currentNoradId == viewLensNoradID) || (onPasses && !allPassesOrbitals && currentNoradId == passesLensNoradID))
                        {
                            //set array with current satellite
                            selectedSatellites = new Database.SatelliteData[currentSatellites.length];
                            selectedSatellites[index] = currentSatellites[index];
                            selectedLookAngles = new TopographicDataType[currentSatellites.length];
                            selectedLookAngles[index] = topoData;
                        }

                        //if not viewing a specific orbital or norad ID matches
                        if(allViewOrbitals || currentNoradId == viewLensNoradID)
                        {
                            //remember look angle
                            lookAngles[index] = topoData;

                            //if status angles list exists and showing list
                            if(onView && onList && Current.PageAdapter.hasItems(Current.PageType.View))
                            {
                                //get current item
                                Current.ViewAngles.Item currentItem = Current.PageAdapter.getViewAngleItemByNorad(currentNoradId);
                                if(currentItem != null)
                                {
                                    //update values
                                    currentItem.azimuth = (float)topoData.azimuth;
                                    currentItem.elevation = (float)topoData.elevation;
                                    currentItem.rangeKm = (float)topoData.rangeKm;
                                    currentItem.illumination = illumination;
                                    if(phaseName != null)
                                    {
                                        currentItem.phaseName = phaseName;
                                    }

                                    //update list later
                                    updateList[Current.PageType.View] = true;
                                }
                            }

                            //if combined list exists and showing list
                            if(onCombined && onList && Current.PageAdapter.hasItems(Current.PageType.Combined))
                            {
                                //get current item
                                Current.Combined.Item currentItem = Current.PageAdapter.getCombinedItemByNorad(currentNoradId);
                                if(currentItem != null)
                                {
                                    //update values
                                    currentItem.azimuth = (float)topoData.azimuth;
                                    currentItem.elevation = (float)topoData.elevation;
                                    currentItem.rangeKm = (float)topoData.rangeKm;
                                    currentItem.speedKms = (float)currentOrbital.geo.speedKmS;
                                    currentItem.latitude = (float)currentOrbital.geo.latitude;
                                    currentItem.longitude = (float)currentOrbital.geo.longitude;
                                    currentItem.altitudeKm = (float)currentOrbital.geo.altitudeKm;

                                    //update list later
                                    updateList[Current.PageType.Combined] = true;
                                }
                            }
                        }

                        //if on passes list
                        if(onPasses && Current.PageAdapter.hasItems(Current.PageType.Passes))
                        {
                            //get current item
                            Current.Passes.Item currentItem = Current.PageAdapter.getPassItemByNorad(currentNoradId);
                            if(currentItem != null)
                            {
                                //if lens exists, showing lens, and -not viewing a specific pass or on that pass-
                                if(onLens && Current.cameraView != null && (allPassesOrbitals || currentNoradId == passesLensNoradID))
                                {
                                    //set look angles
                                    Current.cameraView.setTravel(index, currentItem.passViews);
                                }
                                //else if showing list and pass calculated
                                else if(onList && currentItem.passCalculated)
                                {
                                    //update list later
                                    updateList[Current.PageType.Passes] = true;
                                }
                            }
                        }

                        //if the orbital markers exist and showing map/globe
                        if(onMap && Current.Coordinates.mapView != null && Current.Coordinates.mapView.getOrbitalCount() > 0)
                        {
                            //get current orbital
                            final CoordinatesFragment.OrbitalBase currentMarker = Current.Coordinates.mapView.getOrbital(index);
                            if(currentMarker != null)
                            {
                                //if using all orbitals or norad ID matches
                                if(allMapOrbitals || mapViewNoradID == currentNoradId)
                                {
                                    //remember latitude, longitude, and if visible
                                    double currentLatitude = currentOrbital.geo.latitude;
                                    double currentLongitude = currentOrbital.geo.longitude;
                                    double currentAltitudeKm = currentOrbital.geo.altitudeKm;

                                    //if there is a selection and it has not been selected yet
                                    if(mapViewNoradID == currentNoradId && currentNoradId != lastNoradId)
                                    {
                                        //make sure info is displayed
                                        Current.Coordinates.mapView.selectOrbital(currentNoradId);
                                        currentMarker.setInfoVisible(true);
                                    }

                                    //update coordinates display
                                    if(currentMarker.getInfoVisible())
                                    {
                                        //refresh window and follow
                                        coordinateString = Globals.getCoordinateString(MainActivity.this, currentLatitude, currentLongitude, currentAltitudeKm);
                                        currentMarker.setText(coordinateString);
                                        Current.Coordinates.mapView.moveCamera(currentLatitude, currentLongitude, (lastNoradId != currentNoradId && !Current.Coordinates.mapView.isMap() ? CoordinatesFragment.Utils.getZoom(currentAltitudeKm)  : Current.Coordinates.mapView.getCameraZoom()));

                                        //update last
                                        lastNoradId = currentNoradId;
                                    }
                                    currentMarker.moveLocation(currentLatitude, currentLongitude, currentAltitudeKm);
                                    currentMarker.setVisible(true);
                                }
                                else
                                {
                                    currentMarker.setVisible(false);
                                }
                            }
                        }

                        //if coordinates list exists and showing list
                        if(onCoordinates && onList && Current.PageAdapter.hasItems(Current.PageType.Coordinates))
                        {
                            //get current item
                            Current.Coordinates.Item currentItem = Current.PageAdapter.getCoordinatesItemByNorad(currentNoradId);
                            if(currentItem != null)
                            {
                                //update values
                                currentItem.latitude = (float)currentOrbital.geo.latitude;
                                currentItem.longitude = (float)currentOrbital.geo.longitude;
                                currentItem.altitudeKm = (float)currentOrbital.geo.altitudeKm;
                                currentItem.speedKms = currentOrbital.geo.speedKmS;
                                currentItem.illumination = illumination;
                                if(phaseName != null)
                                {
                                    currentItem.phaseName = phaseName;
                                }

                                //update list later
                                updateList[Current.PageType.Coordinates] = true;
                            }
                        }
                    }

                    //if on map, map info text exists, and using marker bottom info
                    if(onMap && Current.Coordinates.mapInfoText != null && Settings.usingMapMarkerInfoBottom())
                    {
                        final String infoString = coordinateString;
                        MainActivity.this.runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                //if information string is set
                                if(infoString != null)
                                {
                                    //update and show coordinates
                                    Current.Coordinates.mapInfoText.setText(infoString.replace("\n", Globals.COORDINATE_SEPARATOR));
                                    Current.Coordinates.mapInfoText.setVisibility(View.VISIBLE);
                                }
                                else
                                {
                                    //hide coordinates
                                    Current.Coordinates.mapInfoText.setVisibility(View.GONE);
                                }
                            }
                        });
                    }

                    //go through any needed updates
                    for(index = 0; index < Current.PageType.PageCount; index++)
                    {
                        //if there is a pending sort
                        if(Current.PageAdapter.hasPendingSort(index))
                        {
                            //sort page and notify of change
                            Current.PageAdapter.sortItems(MainActivity.this, index, true);
                            Current.PageAdapter.setPendingSort(index, false);
                            updateList[index] = true;
                        }

                        //if update needed
                        if(updateList[index])
                        {
                            //update list
                            Current.PageAdapter.notifyItemsChanged(index);
                        }
                    }

                    //update camera view
                    if(onLens && Current.cameraView != null && (onView || onPasses || onCombined))
                    {
                        //update positions
                        Current.cameraView.updatePositions((selectedSatellites != null ? selectedSatellites : currentSatellites), (selectedLookAngles != null ? selectedLookAngles : lookAngles), true);

                        //set to lens delay
                        timerDelay = lensTimerDelay;
                    }
                    else
                    {
                        //set to list delay
                        timerDelay = listTimerDelay;
                    }

                    //if trying to show view path and not calculated yet
                    if(Current.cameraView != null && Current.cameraView.showPaths && (currentViewAnglesTask == null || (currentViewAnglesTask.needViews && !currentViewAnglesTask.isRunning())))
                    {
                        //start calculating views
                        setCurrentViewCalculations(true, julianDate);
                    }

                    //if trying to show coordinate path and not calculated yet
                    if(Current.Coordinates.showPaths && (currentCoordinatesTask == null || (currentCoordinatesTask.needCoordinates && !currentCoordinatesTask.isRunning())))
                    {
                        //start calculating locations
                        setCurrentCoordinateCalculations(true, julianDate);
                    }

                    //if updating elapsed
                    if(updateElapsed)
                    {
                        //update last time
                        lastSystemElapsedSeconds.value = currentSystemElapsedSeconds;
                    }

                    //update status
                    firstRun.value = false;
                }
            }
        });
    }

    //Creates timer runnable for calculate
    private Runnable createCalculateRunnable()
    {
        return(new Runnable()
        {
            @Override
            public void run()
            {
                int index;
                int playIndex;
                double subProgressPercent;
                Bundle params;
                Context context = MainActivity.this;
                Database.SatelliteData currentSatellite1 = null;
                Database.SatelliteData currentSatellite2 = null;
                Database.SatelliteData[] currentSatellites = null;
                TopographicDataType[] currentPlayTopoDatas;
                CalculateViewsTask.OrbitalView currentView;
                CalculateViewsTask.OrbitalView nextView;
                CalculateViewsTask.OrbitalView[] views = null;
                CalculateViewsTask.OrbitalView[] views2 = null;
                CalculateViewsTask.OrbitalView[][] currentViews = null;
                Current.Passes.Item[] passesItems;
                Current.ViewAngles.Item[] angleItems;

                //if lens exists
                if(Current.cameraView != null)
                {
                    //if using lens and angles list exists
                    if(calculateSubPage[Calculate.PageType.View] == Calculate.SubPageType.Lens && Calculate.PageAdapter.hasItems(Calculate.PageType.View))
                    {
                        //get items
                        angleItems = Calculate.PageAdapter.getViewAngleItems();
                        views = new CalculateViewsTask.OrbitalView[angleItems.length];

                        //go through each item
                        for(index = 0; index < angleItems.length; index++)
                        {
                            //remember current item
                            Current.ViewAngles.Item currentAngleItem = angleItems[index];

                            //set view
                            views[index] = new CalculateViewsTask.OrbitalView(context, currentAngleItem.azimuth, currentAngleItem.elevation, currentAngleItem.rangeKm, currentAngleItem.julianDate, observer.timeZone.getID(), currentAngleItem.illumination, currentAngleItem.phaseName);
                        }

                        //set params and current satellite
                        params = Calculate.PageAdapter.getParams(Calculate.PageType.View);
                        if(params != null)
                        {
                            currentSatellite1 = new Database.SatelliteData(context, params.getInt(Calculate.ParamTypes.NoradId));
                        }
                    }

                    //if using lens and passes list exists
                    if(calculateSubPage[Calculate.PageType.Passes] == Calculate.SubPageType.Lens && Calculate.PageAdapter.hasItems(Calculate.PageType.Passes))
                    {
                        //get items
                        passesItems = Calculate.PageAdapter.getPassItems();

                        //if pass index is valid
                        if(passesPassIndex < passesItems.length)
                        {
                            //get views
                            views = passesItems[passesPassIndex].passViews;
                        }

                        //update
                        params = Calculate.PageAdapter.getParams(Calculate.PageType.Passes);
                        if(params != null)
                        {
                            currentSatellite1 = new Database.SatelliteData(context, params.getInt(Calculate.ParamTypes.NoradId));
                        }
                    }

                    //if passes list exists
                    if(calculateSubPage[Calculate.PageType.Intersection] == Calculate.SubPageType.Lens && Calculate.PageAdapter.hasItems(Calculate.PageType.Intersection))
                    {
                        //get items
                        passesItems = Calculate.PageAdapter.getIntersectionItems();

                        //if pass index is valid
                        if(intersectionPassIndex < passesItems.length)
                        {
                            //get views
                            views = passesItems[intersectionPassIndex].passViews;
                            views2 = passesItems[intersectionPassIndex].passViews2;
                        }

                        //update
                        params = Calculate.PageAdapter.getParams(Calculate.PageType.Intersection);
                        if(params != null)
                        {
                            currentSatellite1 = new Database.SatelliteData(context, params.getInt(Calculate.ParamTypes.NoradId));
                            currentSatellite2 = new Database.SatelliteData(context, params.getInt(Calculate.ParamTypes.NoradId2, Universe.IDs.Invalid));
                        }
                    }
                    if(currentSatellite1 != null && views != null)
                    {
                        if(currentSatellite2 != null && views2 != null)
                        {
                            currentSatellites = new Database.SatelliteData[]{currentSatellite1, currentSatellite2};
                            currentViews = new CalculateViewsTask.OrbitalView[][]{views, views2};
                        }
                        else
                        {
                            currentSatellites = new Database.SatelliteData[]{currentSatellite1};
                            currentViews = new CalculateViewsTask.OrbitalView[][]{views};
                        }
                    }

                    //if satellites, views, and bar are set
                    if(currentSatellites != null && Current.cameraView.playBar != null)
                    {
                        //get play index and progress and set topographic data
                        playIndex = Current.cameraView.playBar.getValue();
                        subProgressPercent = Current.cameraView.playBar.getSubProgressPercent();
                        currentPlayTopoDatas = new TopographicDataType[currentViews.length];

                        //go through each view set
                        for(index = 0; index < currentViews.length; index++)
                        {
                            //remember current satellite views
                            CalculateViewsTask.OrbitalView[] currentSatelliteViews = currentViews[index];
                            TopographicDataType currentTopoData = null;

                            //if a valid play index
                            if(playIndex < currentSatelliteViews.length)
                            {
                                //remember current view
                                currentView = currentSatelliteViews[playIndex];

                                //if more indexes after current
                                if(playIndex + 1 < currentSatelliteViews.length)
                                {
                                    //get add distance index percentage to current view
                                    nextView = currentSatelliteViews[playIndex + 1];
                                    currentTopoData = new TopographicDataType();
                                    currentTopoData.azimuth = Globals.normalizeAngle(currentView.azimuth + (Globals.degreeDistance(currentView.azimuth, nextView.azimuth) * subProgressPercent));
                                    currentTopoData.elevation = Globals.normalizeAngle(currentView.elevation + (Globals.degreeDistance(currentView.elevation, nextView.elevation) * subProgressPercent));
                                    currentTopoData.rangeKm = (currentView.rangeKm + nextView.rangeKm) / 2;
                                }
                                else
                                {
                                    //set look angles
                                    currentTopoData = new TopographicDataType(currentView.azimuth, currentView.elevation, currentView.rangeKm);
                                }
                            }
                            currentPlayTopoDatas[index] = currentTopoData;
                        }

                        //go through topographic data
                        for(index = 0; index < currentPlayTopoDatas.length; index++)
                        {
                            //if not set
                            if(currentPlayTopoDatas[index] == null)
                            {
                                //clear all data
                                currentPlayTopoDatas = new TopographicDataType[0];
                            }
                        }

                        //show paths and update positions/travel
                        Current.cameraView.showPaths = true;
                        Current.cameraView.updatePositions(currentSatellites, currentPlayTopoDatas, false);
                        for(index = 0; index < currentViews.length; index++)
                        {
                            Current.cameraView.setTravel(index, currentViews[index]);
                        }

                        //update delay
                        timerDelay = lensTimerDelay;
                    }
                }
            }
        });
    }

    //Sets previous subpage
    private void setPreviousSubPage()
    {
        //if a valid page
        if(previousPage >= 0)
        {
            //handle based on given group
            switch(previousGroup)
            {
                case Groups.Current:
                    if(previousPage < currentSubPage.length)
                    {
                        previousSubPage = currentSubPage[previousPage];
                    }
                    break;

                case Groups.Calculate:
                    if(previousPage < calculateSubPage.length)
                    {
                        previousSubPage = calculateSubPage[previousPage];
                    }
                    break;

                case Groups.Settings:
                    if(previousPage < settingsSubPage.length)
                    {
                        previousSubPage = settingsSubPage[previousPage];
                    }
                    break;
            }
        }
    }

    //Returns the current sub page
    private int getSubPage()
    {
        int page = getMainPage();
        return(mainGroup == Groups.Current ? currentSubPage[page] : mainGroup == Groups.Calculate ? calculateSubPage[page] : mainGroup == Groups.Settings ? settingsSubPage[page] : 0);
    }

    //Sets the sub page for the given group
    private void setSubPage(int group, int page, int subPage, boolean setPrevious)
    {
        int index;

        //if setting previous and a valid page
        if(setPrevious)
        {
            //set previous sub page
            setPreviousSubPage();
        }

        //handle based on group
        switch(group)
        {
            case Groups.Current:
                if(page < currentSubPage.length && currentPageAdapter != null)
                {
                    currentSubPage[page] = subPage;
                    currentPageAdapter.setSubPage(page, subPage);

                    //clear any selected lens
                    viewLensNoradID = Integer.MAX_VALUE;
                    passesLensNoradID = Integer.MAX_VALUE;

                    switch(page)
                    {
                        case Current.PageType.View:
                        case Current.PageType.Passes:
                        case Current.PageType.Combined:
                            //if changing to lens view
                            if(subPage == Current.SubPageType.Lens)
                            {
                                //go through each sub page
                                for(index = 0; index < Current.PageType.PageCount; index++)
                                {
                                    //if on other page
                                    if(index != page)
                                    {
                                        //make sure other page is not using lens view
                                        currentSubPage[index] = Current.SubPageType.List;
                                    }
                                }
                            }

                            //if not on combined
                            if(page != Current.PageType.Combined)
                            {
                                //stop
                                break;
                            }
                            //else fall through

                        case Current.PageType.Coordinates:
                            //clear any selected map
                            mapViewNoradID = Integer.MAX_VALUE;

                            //if changing to map/globe view
                            if(subPage == Current.SubPageType.Map || subPage == Current.SubPageType.Globe)
                            {
                                //go through each sub page
                                for(index = 0; index < Current.PageType.PageCount; index++)
                                {
                                    //if on other page
                                    if(index != page)
                                    {
                                        //make sure other page is not using map/globe view
                                        currentSubPage[index] = Current.SubPageType.List;
                                    }
                                }
                            }
                            break;
                    }
                    return;
                }
                break;

            case Groups.Calculate:
                if(page < calculateSubPage.length && calculatePageAdapter != null)
                {
                    calculateSubPage[page] = subPage;
                    calculatePageAdapter.setSubPage(page, subPage);

                    switch(page)
                    {
                        case Calculate.PageType.View:
                        case Calculate.PageType.Passes:
                        case Calculate.PageType.Intersection:
                            switch(page)
                            {
                                case Calculate.PageType.Passes:
                                    passesPassIndex = Integer.MAX_VALUE;
                                    break;

                                case Calculate.PageType.Intersection:
                                    intersectionPassIndex = Integer.MAX_VALUE;
                                    break;
                            }

                            //if changing to lens view
                            if(subPage == Calculate.SubPageType.Lens)
                            {
                                //go through other pages that use lens
                                for(int currentOtherPage : new int[]{Calculate.PageType.View, Calculate.PageType.Passes, Calculate.PageType.Intersection})
                                {
                                    //if not the current page and page is using lens
                                    if(currentOtherPage != page && calculateSubPage[currentOtherPage] == Calculate.SubPageType.Lens)
                                    {
                                        //set to input
                                        calculateSubPage[currentOtherPage] = Calculate.SubPageType.Input;
                                    }
                                }
                            }
                            break;
                    }
                    return;
                }
                break;

            case Groups.Settings:
                if(page < settingsSubPage.length && settingsPagerAdapter != null)
                {
                    settingsSubPage[page] = subPage;
                    settingsPagerAdapter.setSubPage(page, subPage);
                    return;
                }
                break;
        }
    }
    private void setSubPage(int group, int page, int subPage)
    {
        setSubPage(group, page, subPage, true);
    }

    //Starts/stops current timer updates
    private void setTimer(boolean run)
    {
        //update timer delays
        updateTimerDelays();

        //if timer was running
        if(timerTask != null)
        {
            //cancel and clear it
            timerTask.cancel(true);
            timerTask = null;

            //reset delay
            timerDelay = listTimerDelay;
        }

        //if want to run
        if(run)
        {
            final Runnable currentRunnable = createCurrentRunnable();
            final Runnable calculateRunnable = createCalculateRunnable();

            //create and start task
            timerRunnable = new Runnable()
            {
                @Override
                public void run()
                {
                    int index;
                    int page = getMainPage();
                    boolean pendingSort = false;
                    boolean onCurrent = (mainGroup == Groups.Current);
                    boolean onCalculate = (mainGroup == Groups.Calculate);
                    boolean onCalculateLens = (onCalculate && page < Calculate.PageType.PageCount && calculateSubPage[page] == Calculate.SubPageType.Lens);

                    //if task still exists
                    if(timerTask != null)
                    {
                        //run appropriate runnable
                        Runnable runTask = (onCurrent ? currentRunnable : onCalculate ? calculateRunnable : null);
                        if(runTask != null)
                        {
                            //if on current
                            if(onCurrent)
                            {
                                //go through each page
                                for(index = 0; index < Current.PageType.PageCount; index++)
                                {
                                    //update if pending sort
                                    pendingSort = pendingSort || Current.PageAdapter.hasPendingSort(index);
                                }
                            }

                            //if -no pending sort- and -on calculate but not showing any lens--
                            if(!pendingSort && onCalculate && !onCalculateLens)
                            {
                                //pause/sleep for 2 seconds
                                timerTask.sleep(2000);
                            }
                            else
                            {
                                //run task
                                runTask.run();
                            }
                        }
                    }
                }
            };
            timerTask = new ThreadTask<>(timerRunnable, timerDelay);
            timerTask.execute();
        }
    }

    //Gets current page action button
    private FloatingActionStateButton getCurrentActionButton(int pageType)
    {
        //if page adapter is set
        if(currentPageAdapter != null)
        {
            //if page is set and a Current.Page
            Selectable.ListFragment page = currentPageAdapter.getPage(mainPager, pageType);
            if(page instanceof Current.Page)
            {
                //get button
                return(((Current.Page)page).actionButton);
            }
        }

        //not found
        return(null);
    }

    //Starts/stops current view calculations
    private void setCurrentViewCalculations(boolean run, double julianDate)
    {
        final FloatingActionStateButton actionButton;

        //if task was running
        if(currentViewAnglesTask != null)
        {
            //cancel and clear it
            currentViewAnglesTask.cancel(true);
            currentViewAnglesTask = null;
        }

        //get action button
        actionButton = getCurrentActionButton(Current.PageType.View);

        //if want to run and have items
        if(run && Current.orbitalViews != null && Current.orbitalViews.length > 0)
        {
            currentViewAnglesTask = Current.calculateViews(this, observer, julianDate, julianDate + 1, 0.2 / 24, new CalculateViewsTask.OnProgressChangedListener()
            {
                @Override
                public void onProgressChanged(final int index, final int progressType, final ArrayList<CalculateViewsTask.OrbitalView> pathPoints)
                {
                    Runnable progressRunnable = null;

                    switch(progressType)
                    {
                        case Globals.ProgressType.Started:
                        case Globals.ProgressType.Finished:
                        case Globals.ProgressType.Cancelled:
                            //if button exists
                            if(actionButton != null)
                            {
                                //set runnable
                                progressRunnable = new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        actionButton.setRotating(progressType == Globals.ProgressType.Started);
                                    }
                                };
                            }
                            break;

                        case Globals.ProgressType.Success:
                            //set runnable
                            progressRunnable = new Runnable()
                            {
                                @Override
                                public synchronized void run()
                                {
                                    CalculateViewsTask.OrbitalView[] views = pathPoints.toArray(new CalculateViewsTask.OrbitalView[0]);

                                    //if camera view still exists
                                    if(Current.cameraView != null)
                                    {
                                        //set camera travel points
                                        Current.cameraView.setTravel(index, views);
                                    }
                                }
                            };
                            break;
                    }

                    //if runnable is set
                    if(progressRunnable != null)
                    {
                        //run it
                        MainActivity.this.runOnUiThread(progressRunnable);
                    }
                }
            });
        }
    }

    //Starts/stops current pass calculations
    private void setCurrentPassCalculations(boolean run)
    {
        int index;
        boolean havePasses;
        boolean haveCombined;
        boolean onCurrent = (mainGroup == Groups.Current);
        boolean usingCombined = (Settings.getCombinedCurrentLayout(this));
        CalculateService.PassData[] selectedItems = null;
        CalculateService.PassData[] passItems;

        //if task was running
        if(currentPassesTask != null)
        {
            //cancel and clear it
            currentPassesTask.cancel(true);
            currentPassesTask = null;
        }

        ///if want to run, not waiting for location update, and have items
        havePasses = (onCurrent && !usingCombined && Current.PageAdapter.hasItems(Current.PageType.Passes));
        haveCombined = (onCurrent && usingCombined && Current.PageAdapter.hasItems(Current.PageType.Combined));
        if(run && (!pendingLocationUpdate || locationSource != Database.LocationType.Current) && (havePasses || haveCombined))
        {
            //get items
            passItems = (havePasses ? Current.PageAdapter.getPassItems() : Current.PageAdapter.getCombinedItems());

            //if not for all orbitals
            if(passesLensNoradID != Integer.MAX_VALUE)
            {
                //go through each item while no match
                for(index = 0; index < passItems.length && selectedItems == null; index++)
                {
                    //if a match
                    if(passItems[index].satellite.getSatelliteNum() == passesLensNoradID)
                    {
                        //remember selected items
                        selectedItems = new CalculateService.PassData[passItems.length];
                        selectedItems[index] = passItems[index];
                    }
                }
            }

            //start task
            currentPassesTask = CalculateService.calculateOrbitalsPasses(this, (selectedItems != null ? selectedItems : passItems), observer, 0, Globals.getGMTTime(), new CalculateService.CalculateListener()
            {
                @Override
                public void onCalculated(int progressType, final CalculateService.PassData pass)
                {
                    //if pass it set and not none
                    if(pass != null)
                    {
                        //if there are pass items
                        if(Current.PageAdapter.hasItems(Current.PageType.Passes))
                        {
                            //remember current item
                            final Current.Passes.Item currentItem = (pass instanceof Current.Passes.Item ? (Current.Passes.Item)pass : null);
                            if(currentItem != null && observer != null)
                            {
                                MainActivity.this.runOnUiThread(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        //update displays
                                        currentItem.setLoading(false);
                                        currentItem.updateDisplays(MainActivity.this, observer.timeZone);

                                        //set pending sort
                                        Current.PageAdapter.setPendingSort(Current.PageType.Passes, true);
                                    }
                                });
                            }
                        }

                        //if there are combined items
                        if(Current.PageAdapter.hasItems(Current.PageType.Combined))
                        {
                            //remember current item
                            final Current.Combined.Item currentItem = (pass instanceof Current.Combined.Item ? (Current.Combined.Item)pass : null);
                            if(currentItem != null && observer != null)
                            {
                                MainActivity.this.runOnUiThread(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        //update displays
                                        currentItem.setLoading(false);
                                        currentItem.updateDisplays(MainActivity.this, observer.timeZone);

                                        //set pending sort
                                        Current.PageAdapter.setPendingSort(Current.PageType.Combined, true);
                                    }
                                });
                            }
                        }
                    }
                }
            });
        }
    }

    //Starts/stops current coordinate calculations
    private void setCurrentCoordinateCalculations(boolean run, double julianDate)
    {
        final FloatingActionStateButton actionButton;

        //if task was running
        if(currentCoordinatesTask != null)
        {
            //cancel and clear it
            currentCoordinatesTask.cancel(true);
            currentCoordinatesTask = null;
        }

        //get action button
        actionButton = getCurrentActionButton(Current.PageType.Coordinates);

        //if want to run and have items
        if(run && Current.Coordinates.mapView != null && Current.Coordinates.mapView.getOrbitalCount() > 0)
        {
            currentCoordinatesTask = Current.Coordinates.calculateCoordinates(observer, julianDate, new Current.Coordinates.CalculatePathTask.OnProgressChangedListener()
            {
                @Override
                public void onProgressChanged(final int index, final int progressType, final ArrayList<CoordinatesFragment.Coordinate> pathPoints)
                {
                    Runnable progressRunnable = null;

                    switch(progressType)
                    {
                        case Globals.ProgressType.Started:
                        case Globals.ProgressType.Finished:
                        case Globals.ProgressType.Cancelled:
                            //if button exists
                            if(actionButton != null)
                            {
                                //set runnable
                                progressRunnable = new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        actionButton.setRotating(progressType == Globals.ProgressType.Started);
                                    }
                                };
                            }
                            break;

                        case Globals.ProgressType.Success:
                            //set runnable
                            progressRunnable = new Runnable()
                            {
                                @Override
                                public synchronized void run()
                                {
                                    //if map exists
                                    if(Current.Coordinates.mapView != null)
                                    {
                                        //update location points
                                        Current.Coordinates.mapView.setPath(index, pathPoints);
                                        Current.Coordinates.mapView.setPathVisible(index, (mapViewNoradID == Integer.MAX_VALUE || mapViewNoradID == Current.Coordinates.mapView.getOrbitalNoradId(index)));
                                    }
                                }
                            };
                            break;
                    }

                    //if runnable it set
                    if(progressRunnable != null)
                    {
                        //run it
                        MainActivity.this.runOnUiThread(progressRunnable);
                    }
                }
            });
        }
    }

    //Starts/stops calculate view calculations
    private void setCalculateViewCalculations(boolean run, final Database.SatelliteData satellite, final double julianDateStart, final double julianDateEnd, double dayIncrement)
    {
        Bundle params;
        Database.SatelliteData listSatellite;
        Current.ViewAngles.Item[] savedViewItems = null;

        //if task was running
        if(calculateViewAnglesTask != null)
        {
            //cancel and clear it
            calculateViewAnglesTask.cancel(true);
            calculateViewAnglesTask = null;
        }

        //if want to run and have an item
        if(run && satellite != null && satellite.database != null)
        {
            //get any saved items and params
            savedViewItems = (Current.ViewAngles.Item[])Calculate.PageAdapter.getSavedItems(Calculate.PageType.View);
            params = Calculate.PageAdapter.getParams(Calculate.PageType.View);

            //check if params changed
            if(params != null && savedViewItems != null)
            {
                //if satellite, start, end, or increment changed
                listSatellite = new Database.SatelliteData(this, params.getInt(Calculate.ParamTypes.NoradId));
                if(listSatellite.database != null && (listSatellite.database.norad != satellite.database.norad || Calculations.julianDateCalendar(params.getLong(Calculate.ParamTypes.StartDateMs), observer) != julianDateStart || Calculations.julianDateCalendar(params.getLong(Calculate.ParamTypes.EndDateMs), observer) != julianDateEnd || Calculate.getDayIncrement(params) != dayIncrement))
                {
                    //ignore saved
                    savedViewItems = null;
                }
            }

            //start task
            calculateViewAnglesTask = Calculate.calculateViews(this, satellite, savedViewItems, observer, julianDateStart, julianDateEnd, dayIncrement, new CalculateViewsTask.OnProgressChangedListener()
            {
                @Override
                public void onProgressChanged(int index, int progressType, final ArrayList<CalculateViewsTask.OrbitalView> pathPoints)
                {
                    Runnable passAction = null;

                    //handle based on progress
                    switch(progressType)
                    {
                        case Globals.ProgressType.Started:
                            passAction = new Runnable()
                            {
                                @Override
                                public synchronized void run()
                                {
                                    //update header and menu
                                    Calculate.PageAdapter.notifyHeaderChanged(Calculate.PageType.View, satellite.getSatelliteNum(), Globals.getHeaderText(MainActivity.this, satellite.getName(), julianDateStart, julianDateEnd));
                                    updateOptionsMenu();
                                }
                            };
                            break;

                        case Globals.ProgressType.Failed:
                        case Globals.ProgressType.Success:
                            passAction = new Runnable()
                            {
                                @Override
                                public synchronized void run()
                                {
                                    int index;
                                    Current.ViewAngles.Item[] items = new Current.ViewAngles.Item[pathPoints != null ? pathPoints.size() : 0];

                                    //go through each item/view
                                    for(index = 0; index < items.length; index++)
                                    {
                                        //remember current view, item, time, and if sun or moon
                                        CalculateViewsTask.OrbitalView currentView = pathPoints.get(index);
                                        Current.ViewAngles.Item currentItem = new Current.ViewAngles.Item(index, false);
                                        Calendar time = Globals.getLocalTime(currentView.gmtTime, observer.timeZone);

                                        currentItem.id = satellite.getSatelliteNum();
                                        currentItem.julianDate = currentView.julianDate;
                                        currentItem.time = time;
                                        currentItem.azimuth = (float)currentView.azimuth;
                                        currentItem.elevation = (float)currentView.elevation;
                                        currentItem.rangeKm = (float)currentView.rangeKm;
                                        currentItem.illumination = currentView.illumination;
                                        currentItem.phaseName =  currentView.phaseName;
                                        items[index] = currentItem;
                                    }

                                    //update list and menu
                                    Calculate.PageAdapter.setViewItems(items);
                                    Calculate.PageAdapter.notifyItemsChanged(Calculate.PageType.View);
                                    updateOptionsMenu();
                                }
                            };
                            break;
                    }

                    //if pass action is set
                    if(passAction != null)
                    {
                        //run action
                        MainActivity.this.runOnUiThread(passAction);
                    }
                }
            });
        }
    }

    //Starts/stops given task path calculations and returns updated task
    private CalculateService.CalculatePathsTask setCalculatePathCalculations(boolean run, CalculateService.CalculatePathsTask task, byte calculateType, final Database.SatelliteData satellite1, final Database.SatelliteData satellite2, double julianDateStart, double julianDateEnd, double intersection, double minEl)
    {
        Bundle params;
        Current.Passes.Item currentItem = (satellite1 != null ? new Current.Passes.Item(MainActivity.this, 0, satellite1, satellite2, true) : null);
        final boolean forIntersections = (calculateType == CalculateService.CalculateType.OrbitalIntersections);
        final int pageType = (forIntersections ? Calculate.PageType.Intersection : Calculate.PageType.Passes);
        Current.Passes.Item[] savedPassItems;

        //if task was running
        if(task != null)
        {
            //cancel and clear it
            task.cancel(true);
            task = null;
        }

        ///if want to run and have item
        if(run && currentItem != null)
        {
            //get any saved items and params
            savedPassItems = (Current.Passes.Item[])Calculate.PageAdapter.getSavedItems(pageType);
            params = Calculate.PageAdapter.getParams(pageType);

            //check if params changed
            if(params != null && savedPassItems != null)
            {
                //if IDs, start date, end date, intersection, or minimum elevation changed
                if(params.getInt(Calculate.ParamTypes.NoradId) != currentItem.id || params.getInt(Calculate.ParamTypes.NoradId2, Universe.IDs.Invalid) != (forIntersections ? currentItem.satellite2.getSatelliteNum() : Universe.IDs.Invalid) || Calculations.julianDateCalendar(params.getLong(Calculate.ParamTypes.StartDateMs), observer) != julianDateStart || Calculations.julianDateCalendar(params.getLong(Calculate.ParamTypes.EndDateMs), observer) != julianDateEnd || params.getDouble(Calculate.ParamTypes.IntersectionDegs, 0.2) != intersection || params.getDouble(Calculate.ParamTypes.ElevationMinDegs) != minEl)
                {
                    //ignore saved
                    savedPassItems = null;
                }
            }

            //start task
            task = CalculateService.calculateOrbitalPaths(this, calculateType, currentItem, savedPassItems, observer, minEl, (forIntersections ? intersection : -Double.MAX_VALUE), julianDateStart, julianDateEnd, true, new CalculateService.CalculateListener()
            {
                @Override
                public void onCalculated(int progressType, final CalculateService.PassData pass)
                {
                    Runnable passAction = null;

                    //if no pass data
                    if(pass == null)
                    {
                        switch(progressType)
                        {
                            case Globals.ProgressType.Started:
                                //set action to set title
                                passAction = new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        //int noradId;
                                        Bundle params = Calculate.PageAdapter.getParams(pageType);

                                        //if params are set
                                        if(params != null)
                                        {
                                            //set title and add loading item
                                            Calculate.PageAdapter.notifyHeaderChanged(pageType, satellite1.getSatelliteNum(), Globals.getHeaderText(MainActivity.this, satellite1.getName() + (satellite2 != null ? (", " + satellite2.getName()) : ""), Calculations.julianDateCalendar(params.getLong(Calculate.ParamTypes.StartDateMs), observer), Calculations.julianDateCalendar(params.getLong(Calculate.ParamTypes.EndDateMs), observer), (forIntersections ? params.getDouble(Calculate.ParamTypes.IntersectionDegs, -Double.MAX_VALUE) : -Double.MAX_VALUE)));
                                            if(forIntersections)
                                            {
                                                Calculate.PageAdapter.setIntersectionItems(new Current.Passes.Item[0]);
                                                Calculate.PageAdapter.addIntersectionItem(new Current.Passes.Item(null, 0, null, true));
                                            }
                                            else
                                            {
                                                Calculate.PageAdapter.setPassItems(new Current.Passes.Item[0]);
                                                Calculate.PageAdapter.addPassItem(new Current.Passes.Item(null, 0, null, true));
                                            }
                                        }
                                        updateOptionsMenu();
                                    }
                                };
                                break;

                            case Globals.ProgressType.Finished:
                            case Globals.ProgressType.Cancelled:
                                //set action to remove loading item
                                passAction = new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        int count = Calculate.PageAdapter.getCount(pageType);
                                        Current.Passes.Item lastItem;

                                        //remove loading item and update menu
                                        if(forIntersections)
                                        {
                                            lastItem = Calculate.PageAdapter.getIntersectionItem(count - 1);
                                            if(lastItem != null && lastItem.isLoading)
                                            {
                                                Calculate.PageAdapter.removeInterSectionItem(count - 1);
                                            }
                                        }
                                        else
                                        {
                                            lastItem = Calculate.PageAdapter.getPassItem(count - 1);
                                            if(lastItem != null && lastItem.isLoading)
                                            {
                                                Calculate.PageAdapter.removePassItem(count - 1);
                                            }
                                        }
                                        updateOptionsMenu();

                                        //save items
                                        Calculate.PageAdapter.setSavedItems(pageType, (forIntersections ? Calculate.PageAdapter.getIntersectionItems() : Calculate.PageAdapter.getPassItems()));
                                    }
                                };
                                break;
                        }
                    }
                    else
                    {
                        //set action to add pass
                        passAction = new Runnable()
                        {
                            @Override
                            public synchronized void run()
                            {
                                //add current pass
                                if(forIntersections)
                                {
                                    Calculate.PageAdapter.addIntersectionItem(new Current.Passes.Item(pass));
                                }
                                else
                                {
                                    Calculate.PageAdapter.addPassItem(new Current.Passes.Item(pass));
                                }
                            }
                        };
                    }

                    //if pass action is set
                    if(passAction != null)
                    {
                        //run action
                        MainActivity.this.runOnUiThread(passAction);
                    }
                }
            });
        }

        return(task);
    }

    //Starts/stops calculate pass calculations
    private void setCalculatePassCalculations(boolean run, Database.SatelliteData satellite, double julianDateStart, double julianDateEnd, double minEl)
    {
        calculatePassesTask = setCalculatePathCalculations(run, calculatePassesTask, CalculateService.CalculateType.OrbitalPasses, satellite, null, julianDateStart, julianDateEnd, 0.2, minEl);
    }

    //Starts/stops calculate intersection calculations
    private void setCalculateIntersectionCalculations(boolean run, Database.SatelliteData satellite1, Database.SatelliteData satellite2, double julianDateStart, double julianDateEnd, double intersection, double minEl)
    {
        calculateIntersectionsTask = setCalculatePathCalculations(run, calculateIntersectionsTask, CalculateService.CalculateType.OrbitalIntersections, satellite1, satellite2, julianDateStart, julianDateEnd, intersection, minEl);
    }

    //Starts/stops calculate coordinate calculations
    private void setCalculateCoordinateCalculations(boolean run, final Database.SatelliteData satellite, final double julianDateStart, final double julianDateEnd, double dayIncrement)
    {
        Bundle params;
        Database.SatelliteData listSatellite;
        Current.Coordinates.Item[] savedCoordinateItems = null;

        //if task was running
        if(calculateCoordinatesTask != null)
        {
            //cancel and clear it
            calculateCoordinatesTask.cancel(true);
            calculateCoordinatesTask = null;
        }

        //if want to run and have an item
        if(run && satellite != null && satellite.database != null)
        {
            //get any saved items and params
            savedCoordinateItems = (Current.Coordinates.Item[])Calculate.PageAdapter.getSavedItems(Calculate.PageType.Coordinates);
            params = Calculate.PageAdapter.getParams(Calculate.PageType.Coordinates);

            //check if params changed
            if(params != null && savedCoordinateItems != null)
            {
                //if satellite, start, end, or increment changed
                listSatellite = new Database.SatelliteData(this, params.getInt(Calculate.ParamTypes.NoradId));
                if(listSatellite.database != null && (listSatellite.database.norad != satellite.database.norad || Calculations.julianDateCalendar(params.getLong(Calculate.ParamTypes.StartDateMs), observer) != julianDateStart || Calculations.julianDateCalendar(params.getLong(Calculate.ParamTypes.EndDateMs), observer) != julianDateEnd || Calculate.getDayIncrement(params) != dayIncrement))
                {
                    //ignore saved
                    savedCoordinateItems = null;
                }
            }

            //start task
            calculateCoordinatesTask = Calculate.calculateCoordinates(this, satellite, savedCoordinateItems, observer, julianDateStart, julianDateEnd, dayIncrement, new CalculateCoordinatesTask.OnProgressChangedListener()
            {
                @Override
                public void onProgressChanged(int index, int progressType, final ArrayList<CalculateCoordinatesTask.OrbitalCoordinate> pathPoints)
                {
                    Runnable passAction = null;

                    //handle based on progress
                    switch(progressType)
                    {
                        case Globals.ProgressType.Started:
                            passAction = new Runnable()
                            {
                                @Override
                                public synchronized void run()
                                {
                                    //update header and menu
                                    Calculate.PageAdapter.notifyHeaderChanged(Calculate.PageType.Coordinates, satellite.getSatelliteNum(), Globals.getHeaderText(MainActivity.this, satellite.getName(), julianDateStart, julianDateEnd));
                                    updateOptionsMenu();
                                }
                            };
                            break;

                        case Globals.ProgressType.Success:
                            passAction = new Runnable()
                            {
                                @Override
                                public synchronized void run()
                                {
                                    int index;
                                    Current.Coordinates.Item[] items = new Current.Coordinates.Item[pathPoints.size()];

                                    //go through each item/view
                                    for(index = 0; index < items.length; index++)
                                    {
                                        //remember current view, item, time, and if moon
                                        CalculateCoordinatesTask.OrbitalCoordinate currentCoordinate = pathPoints.get(index);
                                        Current.Coordinates.Item currentItem = new Current.Coordinates.Item(index);
                                        Calendar time = Globals.getLocalTime(currentCoordinate.time, observer.timeZone);

                                        currentItem.id = satellite.getSatelliteNum();
                                        currentItem.julianDate = currentCoordinate.julianDate;
                                        currentItem.time = time;
                                        currentItem.latitude = (float)currentCoordinate.latitude;
                                        currentItem.longitude = (float)currentCoordinate.longitude;
                                        currentItem.altitudeKm = (float)currentCoordinate.altitudeKm;
                                        currentItem.illumination = currentCoordinate.illumination;
                                        currentItem.phaseName = currentCoordinate.phaseName;
                                        items[index] = currentItem;
                                    }

                                    //update list and menu
                                    Calculate.PageAdapter.setCoordinateItems(items);
                                    Calculate.PageAdapter.notifyItemsChanged(Calculate.PageType.Coordinates);
                                    updateOptionsMenu();
                                }
                            };
                            break;
                    }

                    //if pass action is set
                    if(passAction != null)
                    {
                        //run action
                        MainActivity.this.runOnUiThread(passAction);
                    }
                }
            });
        }
    }
}
