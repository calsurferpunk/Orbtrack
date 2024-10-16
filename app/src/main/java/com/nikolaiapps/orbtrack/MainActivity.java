package com.nikolaiapps.orbtrack;


import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.os.SystemClock;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.core.view.GravityCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.viewpager.widget.PagerAdapter;
import androidx.appcompat.app.ActionBar;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerTitleStrip;
import android.text.Spanned;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import com.nikolaiapps.orbtrack.SideMenuListAdapter.*;
import com.nikolaiapps.orbtrack.Calculations.*;


public class MainActivity extends BaseInputActivity implements ActivityResultCallback<ActivityResult>
{
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
        static final String MainPagerItem = "mainPagerItem";
        static final String PassIndex = "passIndex";
        static final String GetPassItems = "getPassItems";
        static final String ForceShowPaths = "forceShowPaths";
        static final String PathDivisions = "pathDivisions";
        static final String SavedState = "savedState";
    }

    //Status
    private static int calculatePendingPage = -1;
    private static boolean inEditMode;
    private static boolean wasPaused = false;
    private static boolean wasRecreated = false;
    private static boolean runningUserSetup = false;
    private static boolean recreateAfterSetup = false;
    private static boolean ranOldSatelliteUpdate = false;
    private static boolean pendingOldSatelliteMessage = false;
    private boolean savedState;
    private boolean finishedSetup;
    private boolean allowingPagerSwipe = false;
    private Bundle calculateBundle;
    private Bundle savedStateBundle;
    //
    //Displays
    private static WeakReference<PagerTitleStrip> mainPagerTitlesReference;
    private static WeakReference<SwipeStateViewPager> mainPagerReference;
    private Menu optionsMenu;
    private RecyclerView sideActionMenu;
    private ExpandableListView sideMenu;
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
    //
    //Current
    private int mainGroup;
    public static int lensViewNoradID = Integer.MAX_VALUE;
    public static int mapViewNoradID = Integer.MAX_VALUE;
    private static int passesPassIndex = Integer.MAX_VALUE;
    private static int intersectionPassIndex = Integer.MAX_VALUE;
    private static int lastSideMenuPosition = -1;
    private int[] currentSubPage;
    private int[] calculateSubPage;
    private long timerDelay;
    private long listTimerDelay;
    private long mapTimerDelay;
    private long lensTimerDelay;
    private ThreadTask<Void, Void, Void> timerTask;
    private Calendar backPressTime;
    private Globals.PendingFile pendingSaveFile;
    private CalculateViewsTask currentViewAnglesTask;
    private CalculateViewsTask calculateViewAnglesTask;
    private CalculateViewsTask currentTimelineAnglesTask;
    private CalculateCoordinatesTask calculateCoordinatesTask;
    private Current.CalculatePathTask currentCoordinatesTask;
    private CalculateService.CalculatePathsTask currentPassesTask;
    private CalculateService.CalculatePathsTask calculatePassesTask;
    private CalculateService.CalculatePathsTask calculateIntersectionsTask;
    private static final ArrayList<Integer> excludeOldNoradIds = new ArrayList<>(0);
    private static Database.SatelliteData[] currentSatellites = new Database.SatelliteData[0];
    //
    //Location
    private byte locationSource;
    private boolean pendingLocationUpdate;
    private static ObserverType observer;
    //
    //Listeners
    private static Calculate.OnStartCalculationListener startCalculationListener;
    private static Selectable.ListFragment.OnEditModeChangedListener editModeChangedListener;
    //
    //Receivers
    private LocationReceiver locationReceiver;
    private UpdateReceiver localUpdateReceiver;
    private ActivityResultLauncher<Intent> resultLauncher;
    private ActivityResultLauncher<Intent> otherOpenLauncher;
    private ActivityResultLauncher<Intent> otherSaveLauncher;
    //

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        int index;

        this.setContentView(R.layout.main_layout);
        setupActionBar(this, this.getSupportActionBar(), true);

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
        inEditMode = pendingLocationUpdate = false;
        observer = null;
        for(index = 0; index < Current.PageType.PageCount; index++)
        {
            Settings.setCurrentSortBy(this, index, Settings.getCurrentSortBy(this, index));
        }
        Settings.setMetricUnits(this, Settings.getMetricUnits(this));
        Settings.setAllowNumberCommas(this, Settings.getAllowNumberCommas(this));
        Settings.setMapMarkerInfoLocation(this, Settings.getMapMarkerInfoLocation(this));
        Settings.setMapShowFootprint(this, Settings.getMapShowFootprint(this));
        Settings.setMapShowSelectedFootprint(this, Settings.getMapShowSelectedFootprint(this));
        Settings.setSatelliteUseNextDefaultColor(this, Settings.getSatelliteUseNextDefaultColor(this));
        setSaveFileData(null, "", "", -1, -1);
        backPressTime = Globals.getGMTTime();
        backPressTime.set(Calendar.YEAR, 0);
        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true)
        {
            @Override
            public void handleOnBackPressed()
            {
                handleBackPressed();
            }
        });

        //setup bundles
        calculateBundle = new Bundle();

        //set default sub pages
        currentSubPage = new int[Current.PageType.PageCount];
        for(index = 0; index < currentSubPage.length; index++)
        {
            currentSubPage[index] = savedInstanceState.getInt(ParamTypes.CurrentSubPage + index, Globals.SubPageType.List);
        }
        calculateSubPage = new int[Calculate.PageType.PageCount];
        for(index = 0; index < calculateSubPage.length; index++)
        {
            calculateBundle.putBundle(ParamTypes.CalculatePageInputs + index, savedInstanceState.getBundle(ParamTypes.CalculatePageInputs + index));
            calculateSubPage[index] = savedInstanceState.getInt(ParamTypes.CalculateSubPage + index, Globals.SubPageType.Input);
        }

        //setup displays
        mainDrawerLayout = this.findViewById(R.id.Main_Drawer_Layout);
        setupViewEdges(mainDrawerLayout, EdgeDistance.TOP_AND_ACTION_AND_BOTTOM_BAR);
        mainDrawerToggle = createActionBarDrawerToggle();
        mainDrawerLayout.addDrawerListener(mainDrawerToggle);
        mainPager = this.findViewById(R.id.Main_Pager);
        mainPagerTitles = this.findViewById(R.id.Main_Pager_Titles);
        mainPagerTitlesReference = new WeakReference<>(mainPagerTitles);
        mainPagerReference = new WeakReference<>(mainPager);

        //create receivers
        startCalculationListener = createOnStartCalculationListener();
        localUpdateReceiver = createUpdateReceiver(localUpdateReceiver);
        resultLauncher = Globals.createActivityLauncher(this, this);
        otherOpenLauncher = Globals.createActivityLauncher(this, this, RequestCode.OthersOpenItem);
        otherSaveLauncher = Globals.createActivityLauncher(this, this, RequestCode.OthersSave);

        //handle any first run
        handleFirstRun(savedInstanceState);
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
        for(index = 0; index < Calculate.PageType.PageCount; index++)
        {
            outState.putInt(ParamTypes.CalculateSubPage + index, calculateSubPage[index]);

            if(calculatePageAdapter != null)
            {
                Bundle savedItems = calculatePageAdapter.getSavedInputs(index);
                outState.putBundle(ParamTypes.CalculatePageInputs + index, savedItems);
            }
        }
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
        try
        {
            super.onResume();
        }
        catch(Exception ex)
        {
            //do nothing
        }

        //if were paused and not recreating
        if(wasPaused && !wasRecreated)
        {
            //restore tasks
            updateRunningTasks(true);
        }

        //reset
        wasPaused = wasRecreated = false;
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

        //if receivers are set
        if(locationReceiver != null)
        {
            //remove it
            locationReceiver.unregister();
        }
        if(localUpdateReceiver != null)
        {
            //remove it
            localUpdateReceiver.unregister();
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
        int usedPageCount = 0;
        CameraLens cameraView = Current.getCameraView();
        Selectable.ListFragmentAdapter usedAdapter = null;
        int[] usedSubPage = null;

        //update drawer
        mainDrawerToggle.onConfigurationChanged(newConfig);

        //handle based on group
        switch(mainGroup)
        {
            case Groups.Current:
                //set used items
                usedAdapter = currentPageAdapter;
                usedSubPage = currentSubPage;
                usedPageCount = Current.PageType.PageCount;

                //update timeline calculations
                setCurrentTimelineCalculations(false);
                this.runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Current.PageAdapter.setTimelineItemsLoading(true);
                        updateCurrentTimelineCalculations();
                    }
                });
                break;

            case Groups.Calculate:
                //set used items
                usedAdapter = calculatePageAdapter;
                usedSubPage = calculateSubPage;
                usedPageCount = Calculate.PageType.PageCount;
                break;

            case Groups.Orbitals:
                //set used items
                usedAdapter = orbitalPageAdapter;
                usedPageCount = Orbitals.PageType.PageCount;
                break;
        }

        //go through every used page
        for(index = 0; index < usedPageCount; index++)
        {
            //if sub page is set
            if(usedSubPage != null)
            {
                //need camera restart if on lens
                restartCamera = restartCamera || (usedSubPage[index] == Globals.SubPageType.CameraLens) || (usedSubPage[index] == Globals.SubPageType.VirtualLens);
            }

            //if adapter is set
            if(usedAdapter != null)
            {
                //send event
                usedAdapter.notifyOrientationChangedListener(index);
            }
        }

        //if need to restart camera and it is set
        if(restartCamera && cameraView != null)
        {
            //update orientation and restart
            cameraView.updateOrientation();
            cameraView.startCamera();
        }
    }

    @Override
    public void onActivityResult(ActivityResult result)
    {
        int page = getMainPage();
        int subPage;
        int resultCode = result.getResultCode();
        int progressType = Globals.ProgressType.Finished;
        byte requestCode;
        long count;
        boolean isOkay = (resultCode == RESULT_OK);
        boolean handle;
        boolean needRecreate;
        boolean acceptedPrivacy = Settings.getAcceptedPrivacy(this);
        String message;
        Intent data = result.getData();
        Resources res = this.getResources();

        //if no data
        if(data == null)
        {
            //set to empty
            data = new Intent();
        }

        //get status
        requestCode = getRequestCode(data);
        handle = (requestCode == RequestCode.Setup || finishedSetup);
        needRecreate = data.getBooleanExtra(SettingsActivity.EXTRA_RECREATE, false);

        //if not handling
        if(!handle)
        {
            //stop
            return;
        }

        //handle based on request code
        switch(requestCode)
        {
            case RequestCode.Setup:
                //update status
                runningUserSetup = false;
                finishedSetup = !Settings.getFirstRun(this);
                recreateAfterSetup = needRecreate;

                //if cancelled and didn't accept privacy
                if(!isOkay && !acceptedPrivacy)
                {
                    //close app
                    MainActivity.this.finish();
                }
                //else if finished updating database
                else if(finishedSetup)
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

            case RequestCode.Settings:
                //if changed settings
                if(isOkay)
                {
                    //get sub page
                    subPage = getSubPage();

                    //if need to recreate everything
                    if(needRecreate)
                    {
                        //update theme
                        updateTheme();
                    }
                    //else if -need to recreate lens view and on a lens view- or -need to recreate map and on a map view-
                    else if((data.getBooleanExtra(SettingsActivity.EXTRA_RECREATE_LENS, false) && (subPage == Globals.SubPageType.CameraLens || subPage == Globals.SubPageType.VirtualLens)) ||
                            (data.getBooleanExtra(SettingsActivity.EXTRA_RECREATE_MAP, false) && (subPage == Globals.SubPageType.Globe || subPage == Globals.SubPageType.Map)))
                    {
                        //recreate
                        recreate();
                    }
                    else
                    {
                        //if need to reload location
                        if(data.getBooleanExtra(SettingsActivity.EXTRA_RELOAD_LOCATION, false))
                        {
                            //reload observer
                            loadObserver();
                        }
                    }
                }
                break;

            case RequestCode.MasterAddList:
                //if able to get progress type
                progressType = handleActivityMasterAddListResult(res, mainDrawerLayout, data);
                if(progressType == Globals.ProgressType.Unknown)
                {
                    //stop
                    break;
                }
                //else fall through

            case RequestCode.OrbitalViewList:
            case RequestCode.ManualOrbitalInput:
                //if set and weren't denied
                if(isOkay && progressType != Globals.ProgressType.Denied)
                {
                    //update list without saved items
                    updateMainPager(true, false);
                }
                break;

            case RequestCode.OrbitalSelectList:
                //if set
                if(isOkay)
                {
                    //get selected
                    int listNumber = data.getIntExtra(MasterAddListActivity.ParamTypes.ListNumber, 1);
                    ArrayList<Selectable.ListItem> selectedOrbitals = data.getParcelableArrayListExtra(MasterAddListActivity.ParamTypes.SelectedOrbitals);

                    //if calculate adapter exists
                    if(calculatePageAdapter != null)
                    {
                        //if able to get page
                        Selectable.ListFragment calculatePage = calculatePageAdapter.getPage(mainPager, page);
                        if(calculatePage instanceof Calculate.Page)
                        {
                            //set selected
                            //note: sets saved value to use when page resumes
                            calculatePageAdapter.setSavedInput(calculatePage.getPageParam(), (listNumber == 1 ? Calculate.ParamTypes.SelectedOrbitals : Calculate.ParamTypes.SelectedOrbitals2), selectedOrbitals.toArray(new Selectable.ListItem[0]));
                        }
                    }
                }
                break;

            case RequestCode.SDCardOpenItem:
                //if set
                if(isOkay)
                {
                    //handle SD card open files request
                    handleActivitySDCardOpenFilesRequest(this, data);
                }
                break;

            case RequestCode.GoogleDriveOpenFolder:
            case RequestCode.DropboxOpenFolder:
                //if selected folder
                if(isOkay)
                {
                    savePendingFile(true);
                }
                break;

            case RequestCode.GoogleDriveOpenFile:
            case RequestCode.DropboxOpenFile:
            case RequestCode.OthersOpenItem:
                //if selected item
                if(isOkay)
                {
                    //handle open file request
                    handleActivityOpenFileRequest(this, data, requestCode);
                }
                break;

            case RequestCode.GoogleDriveSave:
            case RequestCode.DropboxSave:
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

            case RequestCode.OthersSave:
                //if selected folder
                if(isOkay)
                {
                    try
                    {
                        //update pending file and save it
                        pendingSaveFile.outUri = data.getData();
                        savePendingFile();
                    }
                    catch(Exception ex)
                    {
                        //show error
                        Globals.showSnackBar(mainDrawerLayout, res.getString(R.string.text_file_error_saving), ex.getMessage(), true, true);
                    }
                }
                break;
        }

        //if not okay
        if(!isOkay)
        {
            //if there is a message
            message = data.getStringExtra(FileBrowserBaseActivity.ParamTypes.Message);
            if(message != null)
            {
                //show it
                Globals.showSnackBar(mainDrawerLayout, res.getString(R.string.title_error), message, true, true);
            }
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
            case Globals.PermissionType.CameraRetry:
            case Globals.PermissionType.ReadExternalStorageRetry:
            case Globals.PermissionType.WriteExternalStorageRetry:
            case Globals.PermissionType.ExactAlarmRetry:
            case Globals.PermissionType.PostNotificationsRetry:
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
                        CameraLens cameraView = Current.getCameraView();
                        if(cameraView != null)
                        {
                            //try to start the camera again
                            cameraView.startCamera(!granted);
                        }
                        break;
                }
                break;

            case Globals.PermissionType.ReadExternalStorage:
            case Globals.PermissionType.ReadExternalStorageRetry:
                //if granted
                if(granted)
                {
                    Orbitals.showSDCardFileBrowser(this, resultLauncher, mainDrawerLayout);
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

            case Globals.PermissionType.PostNotifications:
            case Globals.PermissionType.PostNotificationsRetry:
                //if granted
                if(granted)
                {
                    //if progress exists
                    MultiProgressDialog updateProgress = Orbitals.Page.getUpdateProgress();
                    if(updateProgress != null)
                    {
                        //set as going to background and close dialog
                        updateProgress.setTag(true);
                        updateProgress.dismiss();
                    }
                }
                else if(!retrying)
                {
                    //ask permission again
                    Globals.askPostNotificationsPermission(this, true);
                }
                break;
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
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
        if(mainGroup == Groups.Calculate)
        {
            calculatePageAdapter.notifyUpdateOptionsMenu(getMainPage());
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

        if(mainGroup == Groups.Orbitals && page == Orbitals.PageType.Satellites)
        {
            show = !UpdateService.modifyingSatellites();
            if(inEditMode)
            {
                imageID = R.drawable.ic_mode_edit_white;
            }
        }

        //update visibility
        if(mainFloatingButton != null)
        {
            mainFloatingButton.setImageResource(imageID);
        }
        Globals.setVisible(mainFloatingButton, show);
    }

    //Recreates the application
    @Override
    public void recreate()
    {
        wasRecreated = true;
        super.recreate();
    }
    public void recreate(boolean setRecreated)
    {
        wasRecreated = setRecreated;
        super.recreate();
    }

    //Updates the theme
    private void updateTheme()
    {
        Settings.Options.Display.setTheme(this);
        recreate();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item)
    {
        int id = item.getItemId();
        int previousId = Universe.IDs.Invalid;
        int previousSubPage = Globals.SubPageType.List;
        int selectedSubPage;
        int page = getMainPage();
        boolean usingGlobe = (id == R.id.menu_globe);
        boolean usingCameraLens = (id == R.id.menu_camera_lens);
        boolean setMapDisplay = false;
        boolean setLensDisplay = false;
        boolean setDisplayGroup = false;
        boolean onCalculate = (mainGroup == Groups.Calculate);

        //if drawer handles
        if(mainDrawerToggle.onOptionsItemSelected(item))
        {
            return(true);
        }

        //handle item
        if(id == R.id.menu_list)
        {
            switch(mainGroup)
            {
                case Groups.Current:
                    //set sub page
                    setSubPage(Groups.Current, page, Globals.SubPageType.List);

                    //update display
                    setDisplayGroup = true;
                    break;

                case Groups.Calculate:
                    switch(page)
                    {
                        case Calculate.PageType.View:
                        case Calculate.PageType.Passes:
                        case Calculate.PageType.Coordinates:
                        case Calculate.PageType.Intersection:
                            setSubPage(Groups.Calculate, page, Globals.SubPageType.List);
                            setDisplayGroup = true;
                            break;
                    }
                    break;
            }
        }
        else if(usingGlobe || id == R.id.menu_map)
        {
            //if not waiting on a location update
            if(!pendingLocationUpdate)
            {
                switch(mainGroup)
                {
                    case Groups.Current:
                        //continue updating
                        setMapDisplay = true;
                        break;

                    case Groups.Calculate:
                        if(page == Calculate.PageType.Coordinates)
                        {
                            //continue updating
                            setMapDisplay = true;
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
        else if(usingCameraLens || id == R.id.menu_virtual_lens)
        {
            //if not waiting on a location update
            if(!pendingLocationUpdate)
            {
                switch(mainGroup)
                {
                    case Groups.Current:
                        //continue updating
                        setLensDisplay = true;
                        break;

                    case Groups.Calculate:
                        switch(page)
                        {
                            case Calculate.PageType.View:
                            case Calculate.PageType.Passes:
                            case Calculate.PageType.Intersection:
                                //continue updating
                                setLensDisplay = true;
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
        else if(id == R.id.menu_sort_by)
        {
            showSortingDialog(page);
        }
        else if(id == R.id.menu_filter)
        {
            if(mainGroup == Groups.Current)
            {
                showFilterTypeDialog();
            }
        }
        else if(id == R.id.menu_visible)
        {
            showOrbitalViewList();
        }
        else if(id == R.id.menu_settings)
        {
            if(mainGroup == Groups.Current)
            {
                showSettingsDisplay(page);
            }
        }
        else if(id == R.id.menu_update)
        {
            if(mainGroup == Groups.Orbitals && page == Orbitals.PageType.Satellites)
            {
                showConfirmUpdateDialog();
            }
        }
        else if(id == R.id.menu_edit)
        {
            if(mainGroup == Groups.Calculate)
            {
                switch(page)
                {
                    case Calculate.PageType.View:
                    case Calculate.PageType.Passes:
                    case Calculate.PageType.Coordinates:
                    case Calculate.PageType.Intersection:
                        //stop any running task on page
                        stopPageTask();

                        //update sub page
                        setSubPage(Groups.Calculate, page, Globals.SubPageType.Input);

                        //update display
                        updateMainPager(false);
                        updateOptionsMenu();
                        break;
                }
            }
        }
        else if(id == R.id.menu_save)
        {
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
                    if(page == Orbitals.PageType.Satellites)
                    {
                        showSaveSatellitesFileDialog();
                    }
                    break;
            }
        }

        //if setting map display
        if(setMapDisplay)
        {
            //if not on calculate page
            if(!onCalculate)
            {
                //remember previous ID and sub page
                previousId = mapViewNoradID;
                previousSubPage = currentSubPage[page];
            }

            //update sub page
            setSubPage(mainGroup, page, (usingGlobe ? Globals.SubPageType.Globe : Globals.SubPageType.Map));

            //if not on calculate page
            if(!onCalculate)
            {
                //remember selected sub page
                selectedSubPage = currentSubPage[page];

                //if switching between globe/map
                if((previousSubPage == Globals.SubPageType.Map && selectedSubPage == Globals.SubPageType.Globe) || (previousSubPage == Globals.SubPageType.Globe && selectedSubPage == Globals.SubPageType.Map))
                {
                    //restore ID
                    mapViewNoradID = previousId;
                }
            }

            //update default display
            Settings.setMapDisplayType(this, (usingGlobe ? CoordinatesFragment.MapDisplayType.Globe : CoordinatesFragment.MapDisplayType.Map));
            setDisplayGroup = true;
        }

        //if setting lens display
        if(setLensDisplay)
        {
            //if not on a calculate page
            if(!onCalculate)
            {
                //remember previous ID and sub page
                previousId = lensViewNoradID;
                previousSubPage = currentSubPage[page];
            }

            //update sub page
            setSubPage(mainGroup, page, (usingCameraLens ? Globals.SubPageType.CameraLens : Globals.SubPageType.VirtualLens));

            //if not on calculate page
            if(!onCalculate)
            {
                //remember selected sub page
                selectedSubPage = currentSubPage[page];

                //if switching between camera/virtual
                if((previousSubPage == Globals.SubPageType.CameraLens && selectedSubPage == Globals.SubPageType.VirtualLens) || (previousSubPage == Globals.SubPageType.VirtualLens && selectedSubPage == Globals.SubPageType.CameraLens))
                {
                    //restore ID
                    lensViewNoradID = previousId;
                }
            }

            //update default display
            Settings.setLensDisplayType(this, (usingCameraLens ? Settings.Options.LensView.DisplayType.Camera : Settings.Options.LensView.DisplayType.Virtual));
            setDisplayGroup = true;
        }

        //if setting display group
        if(setDisplayGroup)
        {
            //update display group
            setMainGroup(mainGroup, true);
        }

        return(super.onOptionsItemSelected(item));
    }

    private void handleBackPressed()
    {
        boolean updateDisplay = false;
        boolean exitFullScreen = false;
        boolean cancelCalibration = false;
        boolean closedSettings = false;
        boolean closedSearchText = false;
        boolean onLens = false;
        int page = getMainPage();
        int desiredSubPage;
        Calendar currentTime = Globals.getGMTTime();
        CameraLens cameraView = Current.getCameraView();
        CustomSearchView searchText;
        FloatingActionButton fullscreenButton = null;
        ActionBar mainActionBar = this.getSupportActionBar();
        boolean inFullscreen = (mainActionBar != null && !mainActionBar.isShowing());
        Resources res = this.getResources();

        //if drawer is open
        if(mainDrawerLayout.isDrawerOpen(GravityCompat.START))
        {
            //close it
            mainDrawerLayout.closeDrawer(GravityCompat.START);
            return;
        }

        //look for common scenarios
        switch(mainGroup)
        {
            case Groups.Current:
            case Groups.Calculate:
                //get common properties
                searchText = Current.getSearchText();
                fullscreenButton = Current.getFullScreenButton();

                //if in fullscreen and button exists
                if(inFullscreen && fullscreenButton != null)
                {
                    //exit fullscreen
                    exitFullScreen = true;
                }
                //else if search text exists and was able to close
                else if(searchText != null && searchText.close())
                {
                    //closed search text
                    closedSearchText = true;
                }
                break;
        }

        //if not already handled
        if(!exitFullScreen && !closedSearchText)
        {
            //look for group specific scenarios
            switch(mainGroup)
            {
                case Groups.Current:
                    //handle based on sub page
                    switch(currentSubPage[page])
                    {
                        case Globals.SubPageType.CameraLens:
                        case Globals.SubPageType.VirtualLens:
                            //update status
                            onLens = true;

                            //if camera view exists
                            if(cameraView != null)
                            {
                                //if calibrating
                                if(Current.lensShowCalibration)
                                {
                                    cancelCalibration = true;
                                    break;
                                }
                                //else if able to close settings menu
                                else if(cameraView.closeSettingsMenu())
                                {
                                    closedSettings = true;
                                    break;
                                }
                            }
                            //else fall through

                        //go back to list
                        case Globals.SubPageType.Map:
                        case Globals.SubPageType.Globe:
                            //if not on lens and able to close settings menu
                            if(!onLens && Current.closeMapSettingsMenu())
                            {
                                closedSettings = true;
                            }
                            else
                            {
                                setSubPage(Groups.Current, page, Globals.SubPageType.List);
                                updateDisplay = true;
                            }
                            break;
                    }
                    break;

                case Groups.Calculate:
                    desiredSubPage = Globals.SubPageType.Input;

                    //handle based on page
                    switch(page)
                    {
                        case Calculate.PageType.View:
                        case Calculate.PageType.Passes:
                        case Calculate.PageType.Coordinates:
                        case Calculate.PageType.Intersection:
                            //if on view lens or map
                            switch(calculateSubPage[page])
                            {
                                case Globals.SubPageType.Map:
                                case Globals.SubPageType.Globe:
                                    //if able to close settings menu
                                    if(Current.closeMapSettingsMenu())
                                    {
                                        closedSettings = true;
                                        break;
                                    }
                                    //else fall through

                                    //go back to list
                                case Globals.SubPageType.CameraLens:
                                case Globals.SubPageType.VirtualLens:
                                    desiredSubPage = Globals.SubPageType.List;
                                    break;
                            }
                            //fall through

                        default:
                            //if closed settings
                            if(closedSettings)
                            {
                                //stop
                                break;
                            }

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
            }
        }

        //if updating display
        if(updateDisplay)
        {
            //update displays
            updateMainPager(false);
            updateOptionsMenu();
        }
        //else if exiting fullscreen
        else if(exitFullScreen)
        {
            //exit fullscreen
            fullscreenButton.callOnClick();
        }
        //else if cancelling calibration
        else if(cancelCalibration)
        {
            //stop calibration
            cameraView.stopCalibration();
        }
        //else if more than 3 seconds between presses
        else if(!closedSettings && !closedSearchText && currentTime.getTimeInMillis() - backPressTime.getTimeInMillis() > 3000)
        {
            //update time pressed
            backPressTime = currentTime;

            //show message
            Toast.makeText(this, res.getString(R.string.text_press_back_again_exit), Toast.LENGTH_SHORT).show();
        }
        else if(!closedSettings && !closedSearchText)
        {
            //call super
            finish();
        }
    }

    //Returns result launcher
    public ActivityResultLauncher<Intent> getResultLauncher(byte requestCode)
    {
        switch(requestCode)
        {
            case RequestCode.OthersOpenItem:
                return(otherOpenLauncher);

            case RequestCode.OthersSave:
                return(otherSaveLauncher);

            default:
                return(resultLauncher);
        }
    }
    public ActivityResultLauncher<Intent> getResultLauncher()
    {
        return(getResultLauncher(RequestCode.None));
    }

    //Returns pager
    public static SwipeStateViewPager getPager()
    {
        return(mainPagerReference != null ? mainPagerReference.get() : null);
    }

    //Returns pager titles
    public static PagerTitleStrip getPagerTitles()
    {
        return(mainPagerTitlesReference != null ? mainPagerTitlesReference.get() : null);
    }

    //Returns observer
    public static ObserverType getObserver()
    {
        return(observer != null ? observer : new ObserverType());
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

    //Returns if have a selected map view satellite
    public static boolean haveSelectedMapSatellite()
    {
        return(mapViewNoradID != Integer.MAX_VALUE);
    }

    //Returns if have a selected view lens satellite
    public static boolean haveSelectedLensSatellite()
    {
        return(lensViewNoradID != Integer.MAX_VALUE);
    }

    //Returns current lens satellites
    public static Database.SatelliteData[] getLensSatellites(Context context)
    {
        return(haveSelectedLensSatellite() ? new Database.SatelliteData[]{new Database.SatelliteData(context, MainActivity.lensViewNoradID)} : currentSatellites);
    }

    //Returns current old satellites
    private static ArrayList<Database.DatabaseSatellite> getOldSatellites()
    {
        ArrayList<Database.DatabaseSatellite> oldSatellites = new ArrayList<>(0);

        //go through current satellites
        for(Database.SatelliteData currentData : currentSatellites)
        {
            //if current data TLE is not accurate and not in exclude list
            if(currentData.database != null && !currentData.getTLEIsAccurate() && !excludeOldNoradIds.contains(currentData.getSatelliteNum()))
            {
                //add to list
                oldSatellites.add(currentData.database);
            }
        }

        //return list
        return(oldSatellites);
    }

    //Returns calculate view angles task
    public CalculateViewsTask getCalculateViewAnglesTask()
    {
        return(calculateViewAnglesTask);
    }

    //Returns calculate coordinates task
    public CalculateCoordinatesTask getCalculateCoordinatesTask()
    {
        return(calculateCoordinatesTask);
    }

    //Returns calculate passes task
    public CalculateService.CalculatePathsTask getCalculatePassesTask()
    {
        return(calculatePassesTask);
    }

    //Returns calculate intersections task
    public CalculateService.CalculatePathsTask getCalculateIntersectionsTask()
    {
        return(calculateIntersectionsTask);
    }

    //Handles any needed first run
    private void handleFirstRun(Bundle savedInstanceState)
    {
        //if not running user setup and not on the first run
        boolean onFirstRun = Settings.getFirstRun(this);
        boolean acceptedPrivacy = Settings.getAcceptedPrivacy(this);
        finishedSetup = (!runningUserSetup && !onFirstRun && acceptedPrivacy);
        if(finishedSetup)
        {
            //loading starting data
            loadStartData(savedInstanceState);
        }
        //else if not already running user setup
        else if(!runningUserSetup)
        {
            //show setup
            showSetupDialog();
        }
    }

    //Loads start data and updates display
    private void loadStartData(Bundle savedInstanceState)
    {
        int page;

        //get displays
        optionsMenu = null;
        sideActionMenu = this.findViewById(R.id.Side_Action_Menu);
        sideMenu = this.findViewById(R.id.Side_Menu);
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
        updateSideMenu();

        //setup location retrieving
        locationReceiver = createLocationReceiver(locationReceiver);

        //handle any updates
        Database.handleUpdates(this);

        //load orbitals
        loadOrbitals(this, mainDrawerLayout);

        //get observer
        loadObserver(!savedState);

        //update timer delays
        updateTimerDelays();

        //set tasks
        timerTask = null;
        timerDelay = listTimerDelay;
        calculateCoordinatesTask = null;

        //setup pager
        mainPager.addOnPageChangeListener(createOnPageChangedListener());

        //update display
        page = savedInstanceState.getInt(ParamTypes.MainPagerItem, 0);
        setMainGroup(mainGroup, page, true);

        //force refresh first time
        setMainPage(page);

        //if need to recreate after setup
        if(recreateAfterSetup)
        {
            //handle recreating
            recreateAfterSetup = false;
            updateTheme();
        }
    }

    //Updates excluded old norad IDs
    public static void updateExcludedOldNoradIds(int noradId, boolean add)
    {
        boolean inList = excludeOldNoradIds.contains(noradId);

        //if adding
        if(add)
        {
            //if not already in list
            if(!inList)
            {
                //add it
                excludeOldNoradIds.add(noradId);
            }
        }
        //else removing
        else
        {
            //if in the list
            if(inList)
            {
                //remove it
                excludeOldNoradIds.remove((Object)noradId);
            }
        }
    }

    //Loads currently selected orbitals
    public static void loadOrbitals(Context context, View parentView)
    {
        int index;
        int oldSatelliteCount;
        Resources res = (context != null ? context.getResources() : null);
        StringBuilder oldMessage;
        ArrayList<Database.DatabaseSatellite> oldSatellites;
        Database.DatabaseSatellite[] dbSatellites;

        //load selected orbitals
        dbSatellites = Database.getSelectedOrbitals(context);
        currentSatellites = new Database.SatelliteData[dbSatellites.length];
        for(index = 0; index < dbSatellites.length; index++)
        {
            Database.DatabaseSatellite currentSatellite = dbSatellites[index];
            currentSatellites[index] = new Database.SatelliteData(currentSatellite);
        }

        //get old satellites
        oldSatellites = getOldSatellites();
        oldSatelliteCount = oldSatellites.size();

        //if there are old satellites, parent view exists, and resources exist
        if(oldSatelliteCount > 0 && parentView != null && res != null)
        {
            //set message
            oldMessage = new StringBuilder();
            for(Database.DatabaseSatellite currentSatellite : oldSatellites)
            {
                oldMessage.append(currentSatellite.getName());
                oldMessage.append("\r\n");
            }
            oldMessage.append("\r\n");
            oldMessage.append(res.getString(R.string.desc_need_updating));

            //show old satellite count
            ranOldSatelliteUpdate = false;
            pendingOldSatelliteMessage = true;
            Globals.showSnackBar(parentView, res.getQuantityString(R.plurals.title_satellites_outdated, oldSatelliteCount, oldSatelliteCount), oldMessage.toString(), -1, true, true, R.string.title_update, R.string.title_dismiss, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    //try to update old satellites
                    ranOldSatelliteUpdate = true;
                    updateOldSatellites(context, oldSatellites);
                }
            }, new DialogInterface.OnDismissListener()
            {
                @Override
                public void onDismiss(DialogInterface dialog)
                {
                    ArrayList<Database.DatabaseSatellite> remainingOldSatellites;

                    //if didn't just run old satellite update
                    if(!ranOldSatelliteUpdate)
                    {
                        //add any remaining to the excluded list
                        remainingOldSatellites = getOldSatellites();
                        for(Database.DatabaseSatellite currentSatellite : remainingOldSatellites)
                        {
                            //add to list if not in already
                            updateExcludedOldNoradIds(currentSatellite.noradId, true);
                        }
                    }

                    //done with message
                    pendingOldSatelliteMessage = false;
                }
            });
        }
    }

    //Tries to update given old satellites
    private static void updateOldSatellites(Context context, ArrayList<Database.DatabaseSatellite> oldSatellites)
    {
        //if context is set
        if(context != null)
        {
            //update old satellites
            UpdateService.updateSatellites(context, context.getResources().getQuantityString(R.plurals.title_satellites_updating, oldSatellites.size()), oldSatellites, true, false);
        }
    }

    //Updates the side menu
    private void updateSideMenu()
    {
        Resources res = this.getResources();
        Drawable satelliteDrawable = Globals.getDrawable(this, Settings.getSatelliteIconImageId(this), Settings.getSatelliteIconImageIsThemeable(this));
        Drawable viewDrawable = Globals.getDrawable(this, R.drawable.ic_remove_red_eye_white, true);
        Drawable timelineDrawable = Globals.getDrawable(this, R.drawable.ic_timeline_black, true);
        Drawable passDrawable = Globals.getDrawable(this, R.drawable.orbit, true);
        Drawable coordinateDrawable = Globals.getDrawable(this, R.drawable.ic_language_black, true);
        Drawable intersectionDrawable = Globals.getDrawable(this, R.drawable.ic_intersect, true);
        Drawable sunDrawable = Globals.getDrawable(this, Globals.getOrbitalIconId(this, Universe.IDs.Sun));
        Drawable starDrawable = Globals.getDrawable(this, Globals.getOrbitalIconId(this, Universe.IDs.Polaris, Database.OrbitalType.Star));
        Drawable constellationDrawable = Globals.getDrawable(this, Globals.getOrbitalIconId(this, Universe.IDs.Polaris, Database.OrbitalType.Constellation));
        Drawable displayDrawable = Globals.getDrawable(this, R.drawable.ic_tablet_white, true);
        Drawable locationDrawable = Globals.getDrawable(this, R.drawable.ic_my_location_black, true);
        Drawable notificationsDrawable = Globals.getDrawable(this, R.drawable.ic_notifications_white, true);
        Drawable updatesDrawable = Globals.getDrawable(this, R.drawable.ic_sync_white, true);
        Drawable allDrawable = Globals.getDrawable(this, R.drawable.ic_settings_black, true);
        String statusString = res.getString(R.string.title_status);
        String timelineString = res.getString(R.string.title_timeline);
        String viewString = res.getString(R.string.title_view);
        String passesString = res.getString(R.string.title_passes);
        String coordinatesString = res.getString(R.string.title_coordinates);
        String intersectionString = res.getString(R.string.title_intersection);
        ArrayList<SideMenuListAdapter.Group> groups = new ArrayList<>(0);

        if(sideMenu != null)
        {
            groups.add(new Group(this, res.getString(R.string.title_current), R.drawable.ic_access_time_black, new Item[]{new Item(statusString, viewDrawable), new Item(timelineString, timelineDrawable)}));
            groups.add(new Group(this, res.getString(R.string.title_calculate), R.drawable.ic_calculator_black, new Item[]{new Item(viewString, viewDrawable), new Item(passesString, passDrawable), new Item(coordinatesString, coordinateDrawable), new Item(intersectionString, intersectionDrawable)}));
            groups.add(new Group(this, res.getString(R.string.title_orbitals), R.drawable.orbit, new Item[]{new Item(res.getString(R.string.title_satellites), satelliteDrawable), new Item(res.getString(R.string.title_solar_system), sunDrawable), new Item(res.getString(R.string.title_constellations), constellationDrawable), new Item(res.getString(R.string.title_stars), starDrawable)}));
            groups.add(new Group(this, res.getString(R.string.title_settings), R.drawable.ic_settings_black, new Item[]{new Item(res.getString(R.string.title_display), displayDrawable), new Item(res.getString(R.string.title_locations), locationDrawable), new Item(res.getString(R.string.title_notifications), notificationsDrawable), new Item(res.getString(R.string.title_updates), updatesDrawable), new Item(res.getString(R.string.title_all), allDrawable)}));

            sideMenu.setAdapter(new SideMenuListAdapter(this, groups));
            sideMenu.setOnChildClickListener(createOnSideMenuChildClickListener());
            sideMenu.setOnGroupExpandListener(createOnSideMenuGroupExpandListener());
            sideMenu.setGroupIndicator(null);
        }
    }

    //Updates the main pager list and returns to page before update
    private void updateMainPager(boolean reloadOrbitals, boolean saveItems)
    {
        int page = getMainPage();

        //if need to reload orbitals
        if(reloadOrbitals)
        {
            //stop tasks and reload
            updateRunningTasks(false);
            loadOrbitals(this, mainDrawerLayout);
        }

        //update pager
        updateMainPagerAdapter(saveItems);
        setMainPage(page);

        //if reloaded orbitals
        if(reloadOrbitals)
        {
            //if showing status
            if(mainGroup == Groups.Current)
            {
                updateCurrentCalculations();
            }

            //resume tasks
            updateRunningTasks(true);
        }
    }
    private void updateMainPager(boolean reloadOrbitals)
    {
        updateMainPager(reloadOrbitals, true);
    }

    //Updates current timer delays
    private void updateTimerDelays()
    {
        listTimerDelay = Settings.getListUpdateDelay(this);
        mapTimerDelay = Settings.getMapUpdateDelay(this);
        lensTimerDelay = Settings.getLensUpdateDelay(this);
    }

    //Updates current timeline calculations
    private void updateCurrentTimelineCalculations()
    {
        if(currentTimelineAnglesTask != null)
        {
            currentTimelineAnglesTask.needViews = true;
        }
    }

    //Updates current calculations
    private void updateCurrentCalculations()
    {
        int subPage = currentSubPage[Current.PageType.Combined];

        if(currentViewAnglesTask != null && (subPage == Globals.SubPageType.CameraLens || subPage == Globals.SubPageType.VirtualLens))
        {
            currentViewAnglesTask.needViews = true;
        }
        if(currentCoordinatesTask != null && (subPage == Globals.SubPageType.Map || subPage == Globals.SubPageType.Globe))
        {
            currentCoordinatesTask.needCoordinates = true;
        }
        updateCurrentTimelineCalculations();
    }

    //Updates the observer usage
    private void updateObserverUsage(boolean updateNotify)
    {
        boolean usingCurrent = (locationSource == Database.LocationType.Current);
        CameraLens cameraView = Current.getCameraView();

        //update displays and calculations
        Current.setCurrentLocation(this, observer);
        if(cameraView != null)
        {
            //update camera view
            cameraView.updateAzDeclination(observer);
        }
        if(mainGroup == Groups.Current)
        {
            //if there are pass items
            if(Current.PageAdapter.hasItems(Current.PageType.Combined))
            {
                //go through each pass item
                CalculateService.PassData[] passItems = (Current.PageAdapter.getCombinedItems());
                for(CalculateService.PassData currentItem : passItems)
                {
                    //clear pass
                    currentItem.clearPass();
                }
            }

            //recalculate
            setCurrentPassCalculations(true);
        }

        //if updating notifications
        if(updateNotify)
        {
            //update notifications
            CalculateService.NotifyReceiver.updateNotifyLocations(this, observer, usingCurrent, false, false);
        }
    }

    //Updates the observer
    private void updateObserver(Database.DatabaseLocation selectedLocation, boolean allowGetLocation, boolean showStatus)
    {
        //update location source and observer
        locationSource = selectedLocation.locationType;
        observer = new ObserverType(selectedLocation.zoneId, selectedLocation.latitude, selectedLocation.longitude, selectedLocation.altitudeKM);

        //if using current location
        if(locationSource == Database.LocationType.Current)
        {
            //if allowing location updates
            if(allowGetLocation)
            {
                //get location update
                pendingLocationUpdate = true;
                if(showStatus)
                {
                    Globals.showSnackBarTransparent(mainDrawerLayout, R.drawable.ic_location_searching_white, false);
                }
                LocationService.getCurrentLocation(this, false, LocationService.PowerTypes.HighPowerThenBalanced);
            }
            else
            {
                //use as is
                pendingLocationUpdate = false;
                updateObserverUsage(true);
            }
        }
        else
        {
            pendingLocationUpdate = false;
            updateObserverUsage(true);
        }
    }

    //Gets the most recently selected location
    private Database.DatabaseLocation getSelectedLocation()
    {
        Database.DatabaseLocation selectedLocation = Database.getLocation(this);
        boolean isCurrent = (selectedLocation.locationType == Database.LocationType.Current);

        //if observer is set, using current location, and selected is not valid
        if(observer != null && isCurrent && !selectedLocation.isValid())
        {
            //use current observer instead
            selectedLocation.latitude = observer.geo.latitude;
            selectedLocation.longitude = observer.geo.longitude;
            selectedLocation.altitudeKM = observer.geo.altitudeKm;
            selectedLocation.zoneId = observer.timeZone.getID();
        }

        //return selected location
        return(selectedLocation);
    }

    //Loads the observer
    private void loadObserver(boolean showStatus)
    {
        //load the most recent location and allow an update
        updateObserver(getSelectedLocation(), true, showStatus && !pendingOldSatelliteMessage);
    }
    private void loadObserver()
    {
        Database.DatabaseLocation selectedLocation = getSelectedLocation();
        boolean haveLocation = selectedLocation.isValid();

        //load most recent location and only update/show status if update is needed
        updateObserver(selectedLocation, !haveLocation, !haveLocation && !pendingOldSatelliteMessage);
    }

    //Sets save file data
    public void setSaveFileData(Uri outUri, String fileName, String fileExtension, int fileType, int fileSourceType)
    {
        pendingSaveFile = new Globals.PendingFile(outUri, fileName, fileExtension, fileType, fileSourceType);
    }

    //Saves a calculated angles file
    private void saveCalculateAnglesFile(OutputStream outStream, String separator) throws Exception
    {
        int index;
        int index2;
        int idCount;
        int currentId;
        boolean useMultiNorad;
        boolean isSun;
        boolean isMoon;
        String currentNameSeparator;
        StringBuilder lineString = new StringBuilder();
        Resources res = this.getResources();
        Calculate.ViewAngles.Item[] items;

        try
        {
            //get items
            items = Calculate.PageAdapter.getViewAngleItems();
            if(items.length < 1)
            {
                //show error
                throw(new Exception(res.getString(R.string.text_no_items)));
            }

            //get ID count and if using multiple IDs
            idCount = items[0].views.length;
            useMultiNorad = (idCount > 1);

            //build and write column headers
            lineString.append(res.getString(R.string.title_time));
            for(index = 0; index < idCount; index++)
            {
                //get current ID and name
                currentId = items[0].views[index].noradId;
                isSun = (currentId == Universe.IDs.Sun);
                isMoon = (currentId == Universe.IDs.Moon);
                currentNameSeparator = separator + (useMultiNorad ? ((new Database.SatelliteData(this, currentId)).getName() + " ") : "");

                //add columns for current ID
                lineString.append(currentNameSeparator).append(res.getString(R.string.title_azimuth)).append(currentNameSeparator).append(res.getString(R.string.title_elevation)).append(currentNameSeparator).append(res.getString(R.string.title_range)).append(" (").append(Globals.getKmLabel(res)).append(")");
                if(isMoon)
                {
                    lineString.append(currentNameSeparator).append(res.getString(R.string.title_illumination));
                }
                if(isSun || isMoon)
                {
                    lineString.append(currentNameSeparator).append(res.getString(R.string.title_phase));
                }
            }
            lineString.append("\r\n");
            //noinspection CharsetObjectCanBeUsed
            outStream.write(lineString.toString().getBytes(Globals.Encoding.UTF16));
            lineString.setLength(0);

            //go through each item
            for(index = 0; index < items.length; index++)
            {
                //get current item
                Calculate.ViewAngles.Item currentItem = items[index];

                //save item to file
                lineString.setLength(0);
                lineString.append(Globals.getDateTimeString(currentItem.time));
                for(index2 = 0; index2 < currentItem.views.length; index2++)
                {
                    //remember current view, ID, if moon, and if sun
                    CalculateViewsTask.ViewData currentData = currentItem.views[index2];
                    currentId = currentData.noradId;
                    isSun = (currentId == Universe.IDs.Sun);
                    isMoon = (currentId == Universe.IDs.Moon);

                    //add data to line
                    lineString.append(separator).append(Globals.getNumberString(currentData.azimuth, false)).append(separator).append(Globals.getNumberString(currentData.elevation, false)).append(separator).append(String.format(Locale.US, "%.2f", Globals.getKmUnitValue(currentData.rangeKm)));
                    if(isMoon)
                    {
                        lineString.append(separator).append(Globals.getIlluminationString(currentData.illumination));
                    }
                    if(isSun || isMoon)
                    {
                        lineString.append(separator).append(currentData.phaseName);
                    }
                }
                lineString.append("\r\n");
                //noinspection CharsetObjectCanBeUsed
                outStream.write(lineString.toString().getBytes(Globals.Encoding.UTF16));
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
        Calculate.Passes.Item[] items;

        try
        {
            //get items
            items = Calculate.PageAdapter.getPassItems();
            if(items.length < 1)
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
            //noinspection CharsetObjectCanBeUsed
            outStream.write(line.getBytes(Globals.Encoding.UTF16));

            //go through each item
            for(index = 0; index < items.length; index++)
            {
                //get current item
                Calculate.Passes.Item currentItem = items[index];

                //save item to file
                line = Globals.getDateTimeString(currentItem.passTimeStart) + separator + Globals.getDateTimeString(currentItem.passTimeEnd) + separator + currentItem.passDuration + separator + Globals.getNumberString(currentItem.passAzStart, false) + separator + Globals.getNumberString(currentItem.passAzEnd, false) + separator + Globals.getNumberString(currentItem.passAzTravel, false) + separator + Globals.getNumberString(currentItem.passElMax, false) + (isMoon ? (separator + Globals.getIlluminationString(currentItem.illumination)) : "") + (isSun || isMoon ? (separator + currentItem.phaseName) : "") + "\r\n";
                //noinspection CharsetObjectCanBeUsed
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
        int index2;
        int idCount;
        int currentId;
        boolean useMultiNorad;
        boolean isSun;
        boolean isMoon;
        String currentNameSeparator;
        StringBuilder lineString = new StringBuilder();
        Resources res = this.getResources();
        Calculate.Coordinates.Item[] items;

        try
        {
            //get items
            items = Calculate.PageAdapter.getCoordinatesItems();
            if(items.length < 1)
            {
                //show error
                throw(new Exception(res.getString(R.string.text_no_items)));
            }

            //get ID count and if using multiple IDs
            idCount = items[0].coordinates.length;
            useMultiNorad = (idCount > 1);

            //build and write column headers
            lineString.append(res.getString(R.string.title_time));
            for(index = 0; index < idCount; index++)
            {
                //get current ID and name
                currentId = items[0].coordinates[index].noradId;
                isSun = (currentId == Universe.IDs.Sun);
                isMoon = (currentId == Universe.IDs.Moon);
                currentNameSeparator = separator + (useMultiNorad ? ((new Database.SatelliteData(this, currentId)).getName() + " ") : "");

                //add columns for current ID
                lineString.append(currentNameSeparator).append(res.getString(R.string.title_latitude)).append(currentNameSeparator).append(res.getString(R.string.title_longitude)).append(currentNameSeparator).append(res.getString(R.string.abbrev_altitude)).append(" (").append(Globals.getKmLabel(res)).append(")");
                if(isMoon)
                {
                    lineString.append(currentNameSeparator).append(res.getString(R.string.title_illumination));
                }
                if(isSun || isMoon)
                {
                    lineString.append(currentNameSeparator).append(res.getString(R.string.title_phase));
                }
            }
            lineString.append("\r\n");
            //noinspection CharsetObjectCanBeUsed
            outStream.write(lineString.toString().getBytes(Globals.Encoding.UTF16));

            //go through each item
            for(index = 0; index < items.length; index++)
            {
                //get current item
                Calculate.Coordinates.Item currentItem = items[index];

                //save item to file
                lineString.setLength(0);
                lineString.append(Globals.getDateTimeString(currentItem.time));
                for(index2 = 0; index2 < currentItem.coordinates.length; index2++)
                {
                    //remember current coordinates, ID, if moon, and if sun
                    CalculateCoordinatesTask.CoordinateData currentData = currentItem.coordinates[index2];
                    currentId = currentData.noradId;
                    isSun = (currentId == Universe.IDs.Sun);
                    isMoon = (currentId == Universe.IDs.Moon);

                    //add data to line
                    lineString.append(separator).append(Globals.getNumberString(currentData.latitude, false)).append(separator).append(Globals.getNumberString(currentData.longitude, false)).append(separator).append(currentData.altitudeKm > 0 && currentData.altitudeKm != Float.MAX_VALUE ? String.format(Locale.US, "%.2f", Globals.getKmUnitValue(currentData.altitudeKm)) : "-");
                    if(isMoon)
                    {
                        lineString.append(separator).append(Globals.getIlluminationString(currentItem.coordinates[0].illumination));
                    }
                    if(isSun || isMoon)
                    {
                        lineString.append(separator).append(currentData.phaseName);
                    }
                }
                lineString.append("\r\n");
                //noinspection CharsetObjectCanBeUsed
                outStream.write(lineString.toString().getBytes(Globals.Encoding.UTF16));
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
        Calculate.Passes.Item firstItem;
        Calculate.Passes.Item[] items;

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
            //noinspection CharsetObjectCanBeUsed
            outStream.write(line.getBytes(Globals.Encoding.UTF16));

            //go through each item
            for(index = 0; index < items.length; index++)
            {
                //get current item
                Calculate.Passes.Item currentItem = items[index];

                //save item to file
                line = Globals.getDateTimeString(currentItem.passTimeStart) + separator + Globals.getDateTimeString(currentItem.passTimeEnd) + separator + currentItem.passDuration + separator + Globals.getNumberString(currentItem.passAzStart, false) + separator + Globals.getNumberString(currentItem.passAzEnd, false) + separator + Globals.getNumberString(currentItem.passAzTravel, false) + separator + Globals.getNumberString(currentItem.passElMax, false) + separator + Globals.getNumberString(currentItem.passClosestAz, false) + separator + Globals.getNumberString(currentItem.passClosestEl, false) + (isMoon ? (separator + Globals.getIlluminationString(currentItem.illumination)) : "") + (isSun || isMoon ? (separator + currentItem.phaseName) : "") + "\r\n";
                //noinspection CharsetObjectCanBeUsed
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
    private void saveCalculateFile(final int page, Uri outUri, final String fileName, final String extension, final int fileSourceType, boolean confirmInternet)
    {
        String separator = (extension.equals(Globals.FileExtensionType.TXT) ? "\t" : ",");
        OutputStream outStream;
        Resources res = this.getResources();

        //remember pending save file
        setSaveFileData(outUri, fileName, extension, -1, fileSourceType);

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
                        outStream = new FileOutputStream(saveFile);

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
                else if(Globals.canAskWritePermission)
                {
                    Globals.askWritePermission(this, false);
                }
                else
                {
                    //show denied and don't ask again
                    Globals.showDenied(mainDrawerLayout, res.getString(R.string.desc_permission_write_external_storage_deny));
                    Globals.canAskWritePermission = false;
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
                            saveCalculateFile(page, null, fileName, extension, fileSourceType, false);
                        }
                    });
                }
                else
                {
                    try
                    {
                        //try to create file and output stream
                        File saveFile = new File(this.getCacheDir() + "/" + pendingSaveFile.name);
                        outStream = new FileOutputStream(saveFile);

                        //save file
                        saveCalculateFile(page, outStream, separator, fileSourceType);
                        outStream.close();
                        switch(fileSourceType)
                        {
                            case Globals.FileSource.GoogleDrive:
                                GoogleDriveAccess.start(this, resultLauncher, saveFile, 1, true);
                                break;

                            case Globals.FileSource.Dropbox:
                                DropboxAccess.start(this, resultLauncher, saveFile, 1, true);
                                break;
                        }
                    }
                    catch(Exception ex)
                    {
                        //show error message
                        Globals.showSnackBar(mainDrawerLayout, res.getString(R.string.text_file_error_saving), ex.getMessage(), true, true);
                    }
                }
                break;

            case Globals.FileSource.Others:
                try
                {
                    //save file
                    outStream = this.getContentResolver().openOutputStream(Globals.createFileUri(this, outUri, fileName, extension));
                    saveCalculateFile(page, outStream, separator, Globals.FileSource.Others);
                    outStream.close();
                }
                catch(Exception ex)
                {
                    //show error
                    Globals.showSnackBar(mainDrawerLayout, res.getString(R.string.text_file_error_saving), ex.getMessage(), true, true);
                }
                break;
        }
    }
    private void saveCalculateFile(int page, String fileName, String extension, int fileSourceType)
    {
        saveCalculateFile(page, null, fileName, extension, fileSourceType, true);
    }

    //Saves pending file
    private void savePendingFile(boolean confirmInternet)
    {
        int page = getMainPage();

        switch(mainGroup)
        {
            case Groups.Calculate:
                //if on list
                if(calculateSubPage[page] == Globals.SubPageType.List)
                {
                    //save file
                    saveCalculateFile(page, pendingSaveFile.outUri, pendingSaveFile.name, pendingSaveFile.extension, pendingSaveFile.fileSourceType, confirmInternet);
                }
                break;

            case Groups.Orbitals:
                //save file
                orbitalPageAdapter.saveFileSelectedItems(mainPager, page, pendingSaveFile.outUri, pendingSaveFile.name, pendingSaveFile.extension, pendingSaveFile.type, pendingSaveFile.fileSourceType);
                break;
        }
    }
    private void savePendingFile()
    {
        savePendingFile(false);
    }

    //Updates main pager adapter
    private void updateMainPagerAdapter(int page, boolean groupChanged, boolean saveItems)
    {
        int index;
        int usedSubPage;
        boolean showCurrent = (mainGroup == Groups.Current);
        boolean showCalculate = (mainGroup == Groups.Calculate);
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
                boolean saveCurrentItems = (saveItems && currentSubPage[Current.PageType.Combined] != Globals.SubPageType.List);

                Current.PageAdapter.setSavedItems(Current.PageType.Combined, (saveCurrentItems ? Current.PageAdapter.getCombinedItems() : null));
            }
        }
        //else if showing calculate
        else if(showCalculate)
        {
            //save needed items
            if(calculatePageAdapter != null)
            {
                boolean onCalculateViewInput = (calculateSubPage[Calculate.PageType.View] == Globals.SubPageType.Input);
                boolean onCalculatePassesInput = (calculateSubPage[Calculate.PageType.Passes] == Globals.SubPageType.Input);
                boolean onCalculateCoordinatesInput = (calculateSubPage[Calculate.PageType.Coordinates] == Globals.SubPageType.Input);
                boolean onCalculateIntersectionInput = (calculateSubPage[Calculate.PageType.Intersection] == Globals.SubPageType.Input);

                //save items if viewing input
                Calculate.PageAdapter.setSavedItems(Calculate.PageType.View, (onCalculateViewInput ? null : Calculate.PageAdapter.getViewAngleItems()));
                if(onCalculateViewInput)
                {
                    //remove items when starting again
                    Calculate.PageAdapter.setViewItems(new Calculate.ViewAngles.Item[0]);
                }

                //save inputs
                calculatePageAdapter.setSavedSubInput(Calculate.PageType.Passes, ParamTypes.PassIndex, passesPassIndex);
                if(onCalculatePassesInput)
                {
                    //remove items when starting again
                    Calculate.PageAdapter.setSavedItems(Calculate.PageType.Passes, null);
                    Calculate.PageAdapter.setPassItems(new Calculate.Passes.Item[0]);
                }

                //save items if viewing input
                Calculate.PageAdapter.setSavedItems(Calculate.PageType.Coordinates, (onCalculateCoordinatesInput ? null : Calculate.PageAdapter.getCoordinatesItems()));
                if(onCalculateCoordinatesInput)
                {
                    //remove items when starting again
                    Calculate.PageAdapter.setCoordinateItems(new Calculate.Coordinates.Item[0]);
                }

                //save inputs
                calculatePageAdapter.setSavedSubInput(Calculate.PageType.Intersection, ParamTypes.PassIndex, intersectionPassIndex);
                if(onCalculateIntersectionInput)
                {
                    //remove items when starting again
                    Calculate.PageAdapter.setSavedItems(Calculate.PageType.Intersection, null);
                    Calculate.PageAdapter.setIntersectionItems(new Calculate.Passes.Item[0]);
                }
            }
        }

        //if adapter not set or group changed
        if(currentAdapter == null || groupChanged)
        {
            switch(mainGroup)
            {
                case Groups.Current:
                    desiredAdapter = currentPageAdapter = new Current.PageAdapter(currentManger, mainDrawerLayout, createOnItemDetailButtonClickListener(), createOnSetAdapterListener(), createOnPageSetListener(), currentSubPage);
                    break;

                case Groups.Calculate:
                    desiredAdapter = calculatePageAdapter = new Calculate.PageAdapter(currentManger, mainDrawerLayout, createOnItemDetailButtonClickListener(), createOnSetAdapterListener(), createOnPageSetListener(), calculateSubPage, calculateBundle);
                    break;

                case Groups.Orbitals:
                    desiredAdapter = orbitalPageAdapter = new Orbitals.PageAdapter(currentManger, mainDrawerLayout, createOnItemDetailButtonClickListener(), null, createOnUpdateNeededListener(), createOnPageResumeListener());
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
                        setSubPage(Groups.Calculate, index, Globals.SubPageType.Input);
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
                    items.add(new Item(res.getString(R.string.title_add) + " " + res.getQuantityString(R.plurals.title_satellites, 1), Globals.getDrawable(this, R.drawable.ic_add_circle_black, true)));
                    break;
            }

            //if there are items
            hasItems = !items.isEmpty();
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
                                //add satellite
                                if(position == 0)
                                {
                                    //show dialog and close menu
                                    Orbitals.showAddDialog(MainActivity.this, resultLauncher, otherOpenLauncher, mainDrawerLayout, false);
                                    mainDrawerLayout.closeDrawers();
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
            sideActionMenu.setVisibility(hasItems ? View.VISIBLE : View.GONE);
        }

        //update titles and swiping status to allow if not on current/calculate with -map/globe or virtual lens-
        usedSubPage = (showCurrent ? currentSubPage[Current.PageType.Combined] : showCalculate ? calculateSubPage[page] : 0);
        allowingPagerSwipe = (usedSubPage != Globals.SubPageType.Map && usedSubPage != Globals.SubPageType.Globe && usedSubPage != Globals.SubPageType.CameraLens && usedSubPage != Globals.SubPageType.VirtualLens);
        mainPager.setSwipeEnabled(allowingPagerSwipe);
        mainPagerTitles.setVisibility(View.VISIBLE);
    }
    private void updateMainPagerAdapter(boolean saveItems)
    {
        updateMainPagerAdapter(getMainPage(), false, saveItems);
    }

    //Sets main group
    private void setMainGroup(int group, int page, boolean force)
    {
        boolean groupChanging = (mainGroup != group);

        //if forced or group is changing
        if(force || groupChanging)
        {
            //update current group
            mainGroup = group;

            //update pager adapter
            updateMainPagerAdapter(page, groupChanging, true);

            //if not paused and not recreated
            if(!wasPaused && !wasRecreated)
            {
                //update running tasks
                updateRunningTasks(true);
            }

            //update display
            updateOptionsMenu();
        }
    }
    private void setMainGroup(int group, boolean force)
    {
        setMainGroup(group, getMainPage(), force);
    }

    //Gets main page
    private int getMainPage()
    {
        return(mainPager.getCurrentItem());
    }

    //Updates main page
    private void setMainPage(int page)
    {
        //if main pager exists
        if(mainPager != null)
        {
            //set page
            mainPager.setCurrentItem(page);
        }
    }

    //Updates running tasks
    private void updateRunningTasks(boolean run)
    {
        int viewSubPage = calculateSubPage[Calculate.PageType.View];
        int passesSubPage = calculateSubPage[Calculate.PageType.Passes];
        int intersectionSubPage = calculateSubPage[Calculate.PageType.Intersection];
        boolean onCurrent = (mainGroup == Groups.Current);
        boolean onCalculate = (mainGroup == Groups.Calculate);
        boolean onCurrentList = (onCurrent && currentSubPage[Current.PageType.Combined] == Globals.SubPageType.List);
        boolean onCalculateViewNonInput = (onCalculate && viewSubPage != Globals.SubPageType.Input);
        boolean onCalculateViewLens = (onCalculate && (viewSubPage == Globals.SubPageType.CameraLens || viewSubPage == Globals.SubPageType.VirtualLens));
        boolean onCalculatePassesNonInput = (onCalculate && passesSubPage != Globals.SubPageType.Input);
        boolean onCalculatePassesLens = (onCalculate && (passesSubPage == Globals.SubPageType.CameraLens || passesSubPage == Globals.SubPageType.VirtualLens));
        boolean onCalculateCoordinatesNonInput = (onCalculate && calculateSubPage[Calculate.PageType.Coordinates] != Globals.SubPageType.Input);
        boolean onCalculateIntersectionNonInput = (onCalculate && intersectionSubPage != Globals.SubPageType.Input);
        boolean onCalculateIntersectionLens = (onCalculate && (intersectionSubPage == Globals.SubPageType.CameraLens || intersectionSubPage == Globals.SubPageType.VirtualLens));
        Bundle params;
        ArrayList<Integer> multiNoradId;

        //update running tasks
        setTimer(run && (onCurrent || onCalculateViewLens || onCalculatePassesLens || onCalculateIntersectionLens));
        if(run)
        {
            if(onCurrent)
            {
                updateCurrentCalculations();
            }

            setCurrentPassCalculations(onCurrent && onCurrentList);

            if(onCalculateViewNonInput && calculatePendingPage != Calculate.PageType.View)
            {
                params = Calculate.PageAdapter.getParams(Calculate.PageType.View);
                if(params != null)
                {
                    multiNoradId = params.getIntegerArrayList(Calculate.ParamTypes.MultiNoradId);
                    setCalculateViewCalculations(true, (multiNoradId != null && !multiNoradId.isEmpty() ? CalculateService.ViewListItem.getSatellites(this, multiNoradId) : new CalculateService.ViewListItem[]{new CalculateService.ViewListItem(new Database.SatelliteData(this, params.getInt(Calculate.ParamTypes.NoradId)))}), Calculations.julianDateCalendar(params.getLong(Calculate.ParamTypes.StartDateMs), observer), Calculations.julianDateCalendar(params.getLong(Calculate.ParamTypes.EndDateMs), observer), Calculate.getDayIncrement(params));
                }
            }

            if(onCalculatePassesNonInput && calculatePendingPage != Calculate.PageType.Passes)
            {
                params = Calculate.PageAdapter.getParams(Calculate.PageType.Passes);
                if(params != null)
                {
                    setCalculatePassCalculations(true, new Database.SatelliteData(this, params.getInt(Calculate.ParamTypes.NoradId)), Calculations.julianDateCalendar(params.getLong(Calculate.ParamTypes.StartDateMs), observer), Calculations.julianDateCalendar(params.getLong(Calculate.ParamTypes.EndDateMs), observer), params.getDouble(Calculate.ParamTypes.ElevationMinDegs, 0.0));
                }
            }

            if(onCalculateCoordinatesNonInput && calculatePendingPage != Calculate.PageType.Coordinates)
            {
                params = Calculate.PageAdapter.getParams(Calculate.PageType.Coordinates);
                if(params != null)
                {
                    multiNoradId = params.getIntegerArrayList(Calculate.ParamTypes.MultiNoradId);
                    setCalculateCoordinateCalculations(true, (multiNoradId != null && !multiNoradId.isEmpty() ? Database.SatelliteData.getSatellites(this, multiNoradId) : new Database.SatelliteData[]{new Database.SatelliteData(this, params.getInt(Calculate.ParamTypes.NoradId))}), Calculations.julianDateCalendar(params.getLong(Calculate.ParamTypes.StartDateMs), observer), Calculations.julianDateCalendar(params.getLong(Calculate.ParamTypes.EndDateMs), observer), Calculate.getDayIncrement(params));
                }
            }

            if(onCalculateIntersectionNonInput && calculatePendingPage != Calculate.PageType.Intersection)
            {
                params = Calculate.PageAdapter.getParams(Calculate.PageType.Intersection);
                if(params != null)
                {
                    setCalculateIntersectionCalculations(true, new Database.SatelliteData(this, params.getInt(Calculate.ParamTypes.NoradId)), new Database.SatelliteData(this, params.getInt(Calculate.ParamTypes.NoradId2, Universe.IDs.Invalid)), Calculations.julianDateCalendar(params.getLong(Calculate.ParamTypes.StartDateMs), observer), Calculations.julianDateCalendar(params.getLong(Calculate.ParamTypes.EndDateMs), observer), params.getDouble(Calculate.ParamTypes.IntersectionDegs, 1.0), params.getDouble(Calculate.ParamTypes.ElevationMinDegs, 0.0));
                }
            }
        }
        else
        {
            setCurrentPassCalculations(false);
            setCurrentViewCalculations(false, 0);
            setCurrentCoordinateCalculations(false, 0);
            setCurrentTimelineCalculations(false);
            setCalculateViewCalculations(false, null, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
            setCalculatePassCalculations(false, null, Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE);
            setCalculateCoordinateCalculations(false, null, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
            setCalculateIntersectionCalculations(false, null, null, Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE);
            if(calculatePageAdapter != null)
            {
                //stop any running timer
                calculatePageAdapter.stopPlayTimer(mainPager, Calculate.PageType.Coordinates);
            }
            ThreadTask.purgeAll();
        }
    }

    //Stops any running task on current page
    private void stopPageTask()
    {
        int page = getMainPage();

        //handle based on page
        if(mainGroup == Groups.Calculate)
        {
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
        //if old receiver is set
        if(oldReceiver != null)
        {
            //remove it
            oldReceiver.unregister();
        }

        //create receiver
        LocationReceiver receiver = new LocationReceiver(LocationService.FLAG_START_GET_LOCATION | LocationService.FLAG_START_HIGH_POWER_ONCE)
        {
            private boolean firstLocation = true;

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
            protected void onRestart(Context context)
            {
                //if using current location
                if(locationSource == Database.LocationType.Current)
                {
                    //continue with restart
                    super.onRestart(context);
                }
            }

            @Override
            protected void onGotLocation(Context context, ObserverType updatedObserver)
            {
                boolean updated = (updatedObserver != null && (firstLocation || observer == null || observer.notEqual(updatedObserver)));
                boolean showStatus = (pendingLocationUpdate && updated && !pendingOldSatelliteMessage);
                boolean usingCurrent = (locationSource == Database.LocationType.Current);

                //if pending location update or using current location
                if(pendingLocationUpdate || usingCurrent)
                {
                    //update observer
                    observer = updatedObserver;
                    pendingLocationUpdate = false;

                    //if updated
                    if(updated)
                    {
                        //update displays and calculations
                        updateObserverUsage(usingCurrent);
                    }
                }

                //if showing status
                if(showStatus)
                {
                    Globals.showSnackBarTransparent(mainDrawerLayout, R.drawable.ic_my_location_black, false);
                }

                //update status
                firstLocation = false;
            }

            @Override
            protected void onSaveLocation(boolean success)
            {
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
        Resources res = this.getResources();
        MultiProgressDialog taskProgress = Globals.createProgressDialog(this);

        //if old receiver is set
        if(oldReceiver != null)
        {
            //remove it
            oldReceiver.unregister();
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
                        public void onDismiss(EditValuesDialog dialog, int saveCount)
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
            protected void onProgress(long updateValue, long updateCount, String section)
            {
                //if have resources and updating old satellites
                if(res != null && "Old".equals(section))
                {
                    //update progress
                    taskProgress.setMessage(res.getQuantityString(R.plurals.title_space_of_space, (int)updateCount, updateValue, updateCount));
                    taskProgress.setProgress(updateValue, updateCount);
                }
            }

            @Override
            protected void onLoadFileUpdate(int progressType, String section, int percent, int overallPercent)
            {
                //if section is set
                if(section != null)
                {
                    //update section
                    taskProgress.setMessage(section);
                }

                //update progress
                taskProgress.setProgress(percent);

                //if overall progress is set
                if(overallPercent > -1)
                {
                    //update overall progress
                    taskProgress.setProgress2(overallPercent);
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
                            //if adapter exists
                            if(currentPageAdapter != null)
                            {
                                //notify change
                                currentPageAdapter.notifyInformationChanged(page, infoText);
                            }
                            break;

                        case Groups.Calculate:
                            //if adapter exists
                            if(calculatePageAdapter != null)
                            {
                                //notify change
                                calculatePageAdapter.notifyInformationChanged(page, infoText);
                            }
                            break;

                        case Groups.Orbitals:
                            //if adapter exists
                            if(orbitalPageAdapter != null)
                            {
                                //notify change
                                orbitalPageAdapter.notifyInformationChanged(page, infoText);
                            }
                            break;
                    }
                }
            }

            @Override
            protected void onGeneralUpdate(int progressType, byte updateType, boolean ended, String section, int index, int count, File usedFile)
            {
                int page = getMainPage();
                boolean oldSatellites = "Old".equals(section);
                boolean loadingFile = (updateType == UpdateService.UpdateType.LoadFile);
                boolean updatingSatellites = (updateType == UpdateService.UpdateType.UpdateSatellites);
                boolean updatingOldSatellites = (updatingSatellites && oldSatellites);
                boolean onOrbitalsSatellites = (mainGroup == Groups.Orbitals && page == Orbitals.PageType.Satellites);

                //if -on current- or -on orbitals and satellite page-
                if((mainGroup == Groups.Current || onOrbitalsSatellites))
                {
                    //if button exists
                    if(mainFloatingButton != null)
                    {
                        //update visibility
                        Globals.setVisible(mainFloatingButton, ended && onOrbitalsSatellites);
                    }

                    //if done and -loaded file, -updated satellites and not on orbitals page or updating old satellites-, or running in background-
                    if(ended && (loadingFile || (updatingSatellites && (!onOrbitalsSatellites || oldSatellites)) || UpdateService.getNotificationVisible(updateType)))
                    {
                        //update list
                        updateMainPager(true);
                    }
                }

                //if updating old satellites or loading a file
                if(updatingOldSatellites || loadingFile)
                {
                    //if starting
                    if(progressType == Globals.ProgressType.Started)
                    {
                        try
                        {
                            //reset/start showing progress
                            Globals.setUpdateDialog(taskProgress, (loadingFile ? res.getString(R.string.title_file_loading) : (res.getString(R.string.title_updating) + " " + res.getQuantityString(R.plurals.title_satellites, 2))), (loadingFile ? UpdateService.UpdateType.LoadFile : UpdateService.UpdateType.UpdateSatellites));
                            taskProgress.show();
                        }
                        catch(Exception ex)
                        {
                            //do nothing
                        }
                    }
                    //else if ended
                    else if(ended)
                    {
                        try
                        {
                            //try to close progress
                            taskProgress.dismiss();
                        }
                        catch(Exception ex)
                        {
                            //do nothing
                        }
                    }

                    //if updating old satellites and denied
                    if(updatingOldSatellites && progressType == Globals.ProgressType.Denied)
                    {
                        //show login
                        Globals.showAccountLogin(MainActivity.this, Globals.AccountType.SpaceTrack, updateType, new Globals.WebPageListener()
                        {
                            @Override
                            public void onResult(Globals.WebPageData pageData, boolean success)
                            {
                                //if success or attempted to login
                                if(success || pageData != null)
                                {
                                    //try again
                                    updateOldSatellites(MainActivity.this, getOldSatellites());
                                }
                            }
                        });
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

    //Shows setup dialog
    private void showSetupDialog()
    {
        Intent setupIntent = new Intent(this, SettingsActivity.class);

        runningUserSetup = true;
        finishedSetup = false;
        setupIntent.putExtra(SettingsActivity.EXTRA_SHOW_SETUP, true);
        Globals.startActivityForResult(resultLauncher, setupIntent, RequestCode.Setup);
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
        MasterAddListActivity.showList(this, resultLauncher, MasterAddListActivity.ListType.VisibleList, RequestCode.OrbitalViewList, null, -1);
    }

    //Shows a sorting dialog
    public void showSortingDialog(int page)
    {
        //show sorting dialog
        new EditValuesDialog(this, new EditValuesDialog.OnSaveListener()
        {
            @Override
            public void onSave(EditValuesDialog dialog, int itemIndex, int id, String textValue, String text2Value, double number1, double number2, double number3, String listValue, String list2Value, long dateValue, int color1, int color2, boolean visible)
            {
                //update current sort by for page and set pending
                Settings.setCurrentSortBy(MainActivity.this, page, list2Value);
                Current.PageAdapter.setPendingSort(page, true);
            }
        }).getSortBy(this.getString(R.string.title_sort_by), page);
    }

    //Shows a filter type dialog
    public void showFilterTypeDialog()
    {
        int index;
        int page = getMainPage();
        int subPage = getSubPage();
        final int usedSubPage = (page == Current.PageType.Timeline ? currentSubPage[Current.PageType.Combined] : subPage);        //if timeline then use same subpage as combined, else current
        int titleStringId;
        AlertDialog.Builder filterTypeBuilder = new AlertDialog.Builder(this, Globals.getDialogThemeId(this));
        boolean[] checkedArray;
        int[] selections = new int[]{R.string.title_satellites, R.string.title_solar_system, R.string.title_rocket_bodies, R.string.title_debris, R.string.title_constellations, R.string.title_stars};
        ArrayList<Byte> filterList;
        ArrayList<String> titleList = new ArrayList<>(0);
        ArrayList<Boolean> checkedList = new ArrayList<>(0);
        ArrayList<List<Byte>> orbitalTypeList = new ArrayList<>(0);

        //get filter list
        switch(usedSubPage)
        {
            case Globals.SubPageType.List:
                titleStringId = R.string.title_list_filter;
                filterList = Settings.getListOrbitalTypeFilter(this);
                break;

            case Globals.SubPageType.CameraLens:
            case Globals.SubPageType.VirtualLens:
                titleStringId = R.string.title_lens_filter;
                filterList = Settings.getLensOrbitalTypeFilter(this);
                break;

            case Globals.SubPageType.Map:
            case Globals.SubPageType.Globe:
                titleStringId = R.string.title_globe_slash_map_filter;
                filterList = Settings.getMapOrbitalTypeFilter(this);
                break;

            default:
                titleStringId = R.string.title_unknown;
                filterList = new ArrayList<>(0);
                break;
        }

        //go through filter selections
        for(int stringId : selections)
        {
            int count = 0;
            boolean inFilter = false;
            List<Byte> currentOrbitalTypes;

            //get orbital types by title
            if(stringId == R.string.title_satellites)
            {
                currentOrbitalTypes = Collections.singletonList(Database.OrbitalType.Satellite);
            }
            else if(stringId == R.string.title_solar_system)
            {
                currentOrbitalTypes = Arrays.asList(Database.OrbitalType.Sun, Database.OrbitalType.Planet);
            }
            else if(stringId == R.string.title_rocket_bodies)
            {
                currentOrbitalTypes = Collections.singletonList(Database.OrbitalType.RocketBody);
            }
            else if(stringId == R.string.title_debris)
            {
                currentOrbitalTypes = Collections.singletonList(Database.OrbitalType.Debris);
            }
            else if(stringId == R.string.title_constellations)
            {
                currentOrbitalTypes = Collections.singletonList(Database.OrbitalType.Constellation);
            }
            else    //if(stringId == R.string.title_stars)
            {
                currentOrbitalTypes = Collections.singletonList(Database.OrbitalType.Star);
            }

            //go through each orbital type
            for(byte currentType : currentOrbitalTypes)
            {
                //update in filter status and count
                inFilter = (inFilter || filterList.contains(currentType));
                count += Database.getOrbitalTypeCount(this, currentType);
            }

            //if items exist in database
            if(count > 0)
            {
                //add selection
                titleList.add(this.getString(stringId));
                checkedList.add(inFilter);
                orbitalTypeList.add(currentOrbitalTypes);
            }
        }

        //copy selected into values
        checkedArray = new boolean[checkedList.size()];
        for(index = 0; index < checkedList.size(); index++)
        {
            //copy selected value
            checkedArray[index] = checkedList.get(index);
        }

        //setup and show dialog
        filterTypeBuilder.setTitle(titleStringId);
        filterTypeBuilder.setMultiChoiceItems(titleList.toArray(new String[0]), checkedArray, new DialogInterface.OnMultiChoiceClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked)
            {
                //update checked status
                checkedArray[which] = isChecked;
            }
        });
        filterTypeBuilder.setPositiveButton(R.string.title_ok, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int which)
            {
                int index;
                Byte[] selectedArray;
                ArrayList<Byte> selectedOrbitalTypes = new ArrayList<>(0);

                //go through each checked status
                for(index = 0; index < checkedArray.length; index++)
                {
                    //if checked
                    if(checkedArray[index])
                    {
                        //add orbital types for this selection
                        selectedOrbitalTypes.addAll(orbitalTypeList.get(index));
                    }
                }

                //update filter
                selectedArray = selectedOrbitalTypes.toArray(new Byte[0]);
                switch(usedSubPage)
                {
                    case Globals.SubPageType.List:
                        Settings.setListOrbitalTypeFilter(MainActivity.this, selectedArray);
                        updateMainPager(false);
                        break;

                    case Globals.SubPageType.CameraLens:
                    case Globals.SubPageType.VirtualLens:
                        Settings.setLensOrbitalTypeFilter(MainActivity.this, selectedArray);
                        recreate(false);
                        break;

                    case Globals.SubPageType.Map:
                    case Globals.SubPageType.Globe:
                        Settings.setMapOrbitalTypeFilter(MainActivity.this, selectedArray);
                        recreate(false);
                        break;
                }
            }
        });
        filterTypeBuilder.setNegativeButton(R.string.title_cancel, null);
        filterTypeBuilder.show();
    }

    //Shows settings display for given page and current sub page
    public void showSettingsDisplay(int page)
    {
        int subPage = getSubPage();
        Intent startIntent = new Intent(this, SettingsActivity.class);
        String startScreenValue = null;

        switch(page)
        {
            case Current.PageType.Combined:
                switch(subPage)
                {
                    case Globals.SubPageType.List:
                        startScreenValue = SettingsActivity.ScreenKey.ListView;
                        break;

                    case Globals.SubPageType.CameraLens:
                    case Globals.SubPageType.VirtualLens:
                        startScreenValue = SettingsActivity.ScreenKey.LensView;
                        break;

                    case Globals.SubPageType.Map:
                    case Globals.SubPageType.Globe:
                        startScreenValue = SettingsActivity.ScreenKey.GlobeMapView;
                        break;
                }
                break;

            case Current.PageType.Timeline:
                startScreenValue = SettingsActivity.ScreenKey.Timeline;
                break;
        }

        //start settings activity
        startIntent.putExtra(SettingsActivity.EXTRA_START_SCREEN, startScreenValue);
        Globals.startActivityForResult(resultLauncher, startIntent, RequestCode.Settings);
    }

    //Shows getting location display
    private void showLocationGettingDisplay()
    {
        Toast.makeText(this, this.getResources().getString(R.string.title_location_getting), Toast.LENGTH_SHORT).show();
    }

    //Shows a confirm update dialog
    private void showConfirmUpdateDialog()
    {
        Resources res = this.getResources();
        final int page = Orbitals.PageType.Satellites;

        //if orbital adapter exists and is not loading items
        if(orbitalPageAdapter != null && !orbitalPageAdapter.isPageLoadingItems(mainPager, page))
        {
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
    }

    //Shows a save satellites to file dialog
    private void showSaveSatellitesFileDialog()
    {
        Resources res = this.getResources();
        final int page = Orbitals.PageType.Satellites;

        //if orbital adapter exists and is not loading items
        if(orbitalPageAdapter != null && !orbitalPageAdapter.isPageLoadingItems(mainPager, page))
        {
            Globals.showSelectDialog(this, res.getString(R.string.title_save_format), AddSelectListAdapter.SelectType.SaveAs, new AddSelectListAdapter.OnItemClickListener()
            {
                @Override
                public void onItemClick(final int which)
                {
                    final boolean isBackup = which == Globals.FileType.Backup;
                    final int page = Orbitals.PageType.Satellites;
                    final String fileType = (isBackup ? Globals.FileExtensionType.JSON : Globals.FileExtensionType.TLE);
                    final Resources res = MainActivity.this.getResources();

                    new EditValuesDialog(MainActivity.this, new EditValuesDialog.OnSaveListener()
                    {
                        @Override
                        public void onSave(EditValuesDialog dialog, int itemIndex, int id, String textValue, String text2Value, double number1, double number2, double number3, String listValue, String list2Value, long dateValue, int color1, int color2, boolean visible)
                        {
                            //remember file source
                            int fileSourceType = Globals.getFileSource(MainActivity.this, list2Value);

                            //if for others
                            if(fileSourceType == AddSelectListAdapter.FileSourceType.Others)
                            {
                                //remember pending save file
                                setSaveFileData(null, textValue, listValue, which, fileSourceType);

                                //get folder
                                Globals.showOthersFolderSelect(otherSaveLauncher);
                            }
                            else
                            {
                                //save all satellites or prepare for later
                                orbitalPageAdapter.saveFileSelectedItems(mainPager, page, null, textValue, listValue, which, fileSourceType);
                            }
                        }
                    }, new EditValuesDialog.OnDismissListener()
                    {
                        @Override
                        public void onDismiss(EditValuesDialog dialog, int saveCount)
                        {
                            //make sure no items are selected
                            orbitalPageAdapter.selectItems(mainPager, page, false);
                        }
                    }).getFileLocation(res.getString(R.string.title_satellites_save_file), new int[]{0}, new String[]{res.getString(R.string.title_satellites) + (isBackup ? (" " + res.getString(R.string.title_backup)) : "")}, new String[]{fileType}, new String[]{fileType}, new String[]{res.getString(R.string.title_downloads)});
                }
            });
        }
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
                ArrayList<Integer> multiNoradId = params.getIntegerArrayList(Calculate.ParamTypes.MultiNoradId);
                int noradId = params.getInt(Calculate.ParamTypes.NoradId, Universe.IDs.Invalid);
                int multiCount = (multiNoradId != null ? multiNoradId.size() : 0);
                String satelliteName;
                Database.SatelliteData satellite;
                Database.SatelliteData satellite2 = new Database.SatelliteData(this, params.getInt(Calculate.ParamTypes.NoradId2, Universe.IDs.Invalid));

                //if ID is invalid but have 1 multi ID
                if(noradId == Universe.IDs.Invalid && multiCount == 1)
                {
                    //use multi ID
                    noradId = multiNoradId.get(0);
                }
                satellite = new Database.SatelliteData(this, noradId);
                satelliteName = (multiCount > 1 ? res.getString(R.string.title_multiple) : satellite.getName());

                //get file name
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

                //show dialog
                new EditValuesDialog(this, new EditValuesDialog.OnSaveListener()
                {
                    @Override
                    public void onSave(EditValuesDialog dialog, int itemIndex, int id, String textValue, String text2Value, double number1, double number2, double number3, String listValue, String list2Value, long dateValue, int color1, int color2, boolean visible)
                    {
                        //remember file source
                        int fileSourceType = Globals.getFileSource(MainActivity.this, list2Value);

                        //if for others
                        if(fileSourceType == AddSelectListAdapter.FileSourceType.Others)
                        {
                            //remember pending save file
                            setSaveFileData(null, textValue, listValue, -1, fileSourceType);

                            //get folder
                            Globals.showOthersFolderSelect(otherSaveLauncher);
                        }
                        else
                        {
                            saveCalculateFile(calculatePageType, textValue, listValue, fileSourceType);
                        }
                    }
                }).getFileLocation(res.getString(R.string.title_save_file), new int[]{0}, new String[]{fileName}, new String[]{Globals.FileExtensionType.TXT, Globals.FileExtensionType.CSV}, new String[]{Globals.FileExtensionType.TXT}, new String[]{res.getString(R.string.title_downloads)});
            }
        }
    }

    //Handles main floating button click
    private void handleMainFloatingButtonClick(boolean isLong)
    {
        //handle based on group
        if(mainGroup == Groups.Orbitals)
        {
            if(inEditMode)
            {
                Orbitals.showEditDialog(this, mainPager, orbitalPageAdapter);
            }
            else
            {
                Orbitals.showAddDialog(this, resultLauncher, otherOpenLauncher, mainDrawerLayout, isLong);
            }
        }
    }

    //Creates an on item detail button click listener
    private Selectable.ListFragment.OnItemDetailButtonClickListener createOnItemDetailButtonClickListener()
    {
        return(new Selectable.ListFragment.OnItemDetailButtonClickListener()
        {
            @Override
            public void onClick(final int group, final int pageNum, int itemID, Selectable.ListDisplayItem item, int buttonNum)
            {
                final Activity activity = MainActivity.this;
                boolean isCameraButton = false;

                //handle based on button
                switch(buttonNum)
                {
                    case Selectable.ListBaseAdapter.DetailButtonType.CameraView:
                        isCameraButton = true;
                        //fall through

                    case Selectable.ListBaseAdapter.DetailButtonType.VirtualView:
                        switch(group)
                        {
                            case Groups.Current:
                                //update sub page and norad ID
                                setSubPage(group, pageNum, (isCameraButton ? Globals.SubPageType.CameraLens : Globals.SubPageType.VirtualLens));
                                lensViewNoradID = itemID;
                                setMainGroup(mainGroup, true);
                                break;

                            case Groups.Calculate:
                                switch(pageNum)
                                {
                                    case Calculate.PageType.Passes:
                                    case Calculate.PageType.Intersection:
                                        //update sub page and norad ID
                                        setSubPage(group, pageNum, (isCameraButton ? Globals.SubPageType.CameraLens : Globals.SubPageType.VirtualLens));
                                        if(pageNum == Calculate.PageType.Intersection)
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
                        if(group == Groups.Current)
                        {
                            //update sub page and norad ID
                            setSubPage(group, pageNum, (buttonNum == Selectable.ListBaseAdapter.DetailButtonType.GlobeView ? Globals.SubPageType.Globe : Globals.SubPageType.Map));
                            mapViewNoradID = itemID;
                            setMainGroup(mainGroup, true);
                        }
                        break;

                    case Selectable.ListBaseAdapter.DetailButtonType.Notify:
                        //show pass notification settings
                        NotifySettingsActivity.show(activity, resultLauncher, itemID, observer);
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
                                        //if adapter exists
                                        if(currentPageAdapter != null)
                                        {
                                            //notify change
                                            currentPageAdapter.notifyPreview3dChanged(pageNum, itemID);
                                        }
                                        break;

                                    case Groups.Orbitals:
                                        //if adapter exists
                                        if(orbitalPageAdapter != null)
                                        {
                                            //notify change
                                            orbitalPageAdapter.notifyPreview3dChanged(pageNum, itemID);
                                        }
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
                                Calculate.calculateViews(activity, new CalculateService.ViewListItem[]{new CalculateService.ViewListItem(currentOrbital)}, null, observer, julianStartDate, julianEndDate, dayIncrement, new CalculateViewsTask.OnProgressChangedListener()
                                {
                                    @Override
                                    public void onProgressChanged(int progressType, int satelliteIndex, CalculateService.ViewListItem item, final ArrayList<CalculateViewsTask.OrbitalView> pathPoints)
                                    {
                                        switch(progressType)
                                        {
                                            case Globals.ProgressType.Success:
                                            case Globals.ProgressType.Failed:
                                                switch(group)
                                                {
                                                    case Groups.Current:
                                                        //if adapter exists
                                                        if(currentPageAdapter != null)
                                                        {
                                                            //notify change
                                                            currentPageAdapter.notifyGraphChanged(pageNum, currentOrbital, pathPoints, null, null);
                                                        }
                                                        break;

                                                    case Groups.Calculate:
                                                        if(progressType == Globals.ProgressType.Success && pageNum == Calculate.PageType.Intersection && currentOrbital2 != null)
                                                        {
                                                            //calculate remaining views
                                                            Calculate.calculateViews(activity, new CalculateService.ViewListItem[]{new CalculateService.ViewListItem(currentOrbital2)}, null, observer, julianStartDate, julianEndDate, dayIncrement, new CalculateViewsTask.OnProgressChangedListener()
                                                            {
                                                                @Override
                                                                public void onProgressChanged(int progressType, int satelliteIndex, CalculateService.ViewListItem item, ArrayList<CalculateViewsTask.OrbitalView> path2Points)
                                                                {
                                                                    switch(progressType)
                                                                    {
                                                                        case Globals.ProgressType.Success:
                                                                        case Globals.ProgressType.Failed:
                                                                            //if adapter exists
                                                                            if(calculatePageAdapter != null)
                                                                            {
                                                                                //notify change
                                                                                calculatePageAdapter.notifyGraphChanged(pageNum, currentOrbital, pathPoints, currentOrbital2, path2Points);
                                                                            }
                                                                            break;
                                                                    }
                                                                }
                                                            });
                                                        }
                                                        //else if adapter exists
                                                        else if(calculatePageAdapter != null)
                                                        {
                                                            //notify change
                                                            calculatePageAdapter.notifyGraphChanged(pageNum, currentOrbital, pathPoints, null, null);
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
            public void setAdapter(Selectable.ListFragment fragment, final int group, final int position, Selectable.ListBaseAdapter adapter)
            {
                int orbitalId;
                Bundle params;
                ArrayList<Integer> multiNoradId;

                switch(group)
                {
                    case Groups.Current:
                        switch(position)
                        {
                            case Current.PageType.Combined:
                                //get passes
                                setCurrentPassCalculations(true);
                                break;

                            case Current.PageType.Timeline:
                                //get timelines
                                updateCurrentTimelineCalculations();
                                break;
                        }
                        break;

                    case Groups.Calculate:
                        switch(position)
                        {
                            case Calculate.PageType.View:
                                Calculate.ViewAngles.Item[] viewItems = null;
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
                                    multiNoradId = params.getIntegerArrayList(Calculate.ParamTypes.MultiNoradId);
                                    if(orbitalId == Universe.IDs.Invalid && multiNoradId != null && multiNoradId.size() == 1)
                                    {
                                        orbitalId = multiNoradId.get(0);
                                    }
                                    Calculate.PageAdapter.notifyHeaderChanged(Calculate.PageType.View, orbitalId, Globals.getHeaderText(MainActivity.this, new Database.SatelliteData(MainActivity.this, orbitalId).getName(), Calculations.julianDateCalendar(params.getLong(Calculate.ParamTypes.StartDateMs), observer), Calculations.julianDateCalendar(params.getLong(Calculate.ParamTypes.EndDateMs), observer)));
                                }

                                //recreate tasks
                                updateRunningTasks(true);
                                break;

                            case Calculate.PageType.Passes:
                            case Calculate.PageType.Intersection:
                                boolean forIntersection = (position == Calculate.PageType.Intersection);
                                Calculate.Passes.Item[] passItems = null;
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
                                        Calculate.PageAdapter.setIntersectionItems(new Calculate.Passes.Item[0]);
                                        Calculate.PageAdapter.addIntersectionItems(passItems, 0);
                                    }
                                    else
                                    {
                                        Calculate.PageAdapter.setPassItems(new Calculate.Passes.Item[0]);
                                        Calculate.PageAdapter.addPassItems(passItems, 0);
                                    }
                                }
                                if(params != null)
                                {
                                    orbitalId = params.getInt(Calculate.ParamTypes.NoradId);
                                    Calculate.PageAdapter.notifyHeaderChanged(position, orbitalId, Globals.getHeaderText(MainActivity.this, (orbitalId != Universe.IDs.Invalid ? new Database.SatelliteData(MainActivity.this, orbitalId).getName() : null), Calculations.julianDateCalendar(params.getLong(Calculate.ParamTypes.StartDateMs), observer), Calculations.julianDateCalendar(params.getLong(Calculate.ParamTypes.EndDateMs), observer), (forIntersection ? params.getDouble(Calculate.ParamTypes.IntersectionDegs, -Double.MAX_VALUE) : -Double.MAX_VALUE)));
                                }

                                //recreate tasks
                                updateRunningTasks(true);
                                break;

                            case Calculate.PageType.Coordinates:
                                Calculate.Coordinates.Item[] coordinateItems = null;
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
                                    multiNoradId = params.getIntegerArrayList(Calculate.ParamTypes.MultiNoradId);
                                    if(orbitalId == Universe.IDs.Invalid && multiNoradId != null && multiNoradId.size() == 1)
                                    {
                                        orbitalId = multiNoradId.get(0);
                                    }
                                    Calculate.PageAdapter.notifyHeaderChanged(Calculate.PageType.Coordinates, orbitalId, Globals.getHeaderText(MainActivity.this, (orbitalId != Universe.IDs.Invalid ? new Database.SatelliteData(MainActivity.this, orbitalId).getName() : null), Calculations.julianDateCalendar(params.getLong(Calculate.ParamTypes.StartDateMs), observer), Calculations.julianDateCalendar(params.getLong(Calculate.ParamTypes.EndDateMs), observer)));
                                }

                                //recreate tasks
                                updateRunningTasks(true);
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
                int objectId = params.getInt(Calculate.ParamTypes.NoradId, Universe.IDs.Invalid);
                int object2Id = params.getInt(Calculate.ParamTypes.NoradId2, Universe.IDs.Invalid);
                long startDateMs = params.getLong(Calculate.ParamTypes.StartDateMs);
                long endDateMs = params.getLong(Calculate.ParamTypes.EndDateMs);
                double dayIncrement = 1;
                double intersection = params.getDouble(Calculate.ParamTypes.IntersectionDegs, 1.0);
                double elevationMin = params.getDouble(Calculate.ParamTypes.ElevationMinDegs, 0.0);
                double julianDateStart = Calculations.julianDateCalendar(startDateMs, observer);
                double julianDateEnd = Calculations.julianDateCalendar(endDateMs, observer);
                Bundle oldParams = Calculate.PageAdapter.getParams(page);
                Database.SatelliteData satellite = new Database.SatelliteData(MainActivity.this, objectId);
                Database.SatelliteData satellite2 = new Database.SatelliteData(MainActivity.this, object2Id);
                ArrayList<Integer> multiNoradId = params.getIntegerArrayList(Calculate.ParamTypes.MultiNoradId);
                int multiNoradIdLength = (multiNoradId != null ? multiNoradId.size() : 0);

                //if not waiting on a location update
                if(!pendingLocationUpdate)
                {
                    //if old params not set
                    if(oldParams == null)
                    {
                        //set to empty
                        oldParams = new Bundle();
                    }

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
                    params.putInt(Calculate.ParamTypes.NoradIdOld, oldParams.getInt(Calculate.ParamTypes.NoradId, Universe.IDs.Invalid)) ;
                    params.putInt(Calculate.ParamTypes.NoradId2Old, oldParams.getInt(Calculate.ParamTypes.NoradId2, Universe.IDs.Invalid)) ;
                    params.putIntegerArrayList(Calculate.ParamTypes.MultiNoradIdOld, oldParams.getIntegerArrayList(Calculate.ParamTypes.MultiNoradId));
                    Calculate.PageAdapter.setParams(page, params);

                    //update status/display
                    calculatePendingPage = page;
                    updateMainPager(false);
                    updateOptionsMenu();

                    //if on sub list
                    if(subPage == Globals.SubPageType.List)
                    {
                        //calculation based on page
                        switch(page)
                        {
                            case Calculate.PageType.View:
                                //start calculating
                                setCalculateViewCalculations(true, (multiNoradIdLength > 0 ? CalculateService.ViewListItem.getSatellites(MainActivity.this, multiNoradId) : new CalculateService.ViewListItem[]{new CalculateService.ViewListItem(satellite)}), julianDateStart, julianDateEnd, dayIncrement);
                                break;

                            case Calculate.PageType.Passes:
                                //reset last inputs and start calculating
                                setCalculatePassCalculations(true, satellite, julianDateStart, julianDateEnd, elevationMin);
                                break;

                            case Calculate.PageType.Coordinates:
                                //start calculating
                                setCalculateCoordinateCalculations(true, (multiNoradIdLength > 0 ? Database.SatelliteData.getSatellites(MainActivity.this, multiNoradId) : new Database.SatelliteData[]{satellite}), julianDateStart, julianDateEnd, dayIncrement);
                                break;

                            case Calculate.PageType.Intersection:
                                //reset last inputs and start calculating
                                setCalculateIntersectionCalculations(true, satellite, satellite2, julianDateStart, julianDateEnd, intersection, elevationMin);
                                break;
                        }
                    }

                    //update status
                    calculatePendingPage = -1;
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
    private Selectable.ListFragment.OnPageSetListener createOnPageSetListener()
    {
        return(new Selectable.ListFragment.OnPageSetListener()
        {
            @Override
            public void onPageSet(Selectable.ListFragment page, int pageNum, int subPageNum)
            {
                final FloatingActionStateButton fullscreenButton;

                //if a Current.Page
                if(page instanceof Current.Page)
                {
                    fullscreenButton = Current.getFullScreenButton();
                }
                //else if a Calculate.Page
                else if(page instanceof Calculate.Page)
                {
                    fullscreenButton = Current.getFullScreenButton();

                    switch(pageNum)
                    {
                        case Calculate.PageType.View:
                        case Calculate.PageType.Passes:
                        case Calculate.PageType.Coordinates:
                        case Calculate.PageType.Intersection:
                            //if getting input
                            if(subPageNum == Globals.SubPageType.Input)
                            {
                                ((Calculate.Page)page).setOnStartCalculationListener(startCalculationListener);
                            }
                            break;
                    }
                }
                else
                {
                    fullscreenButton = null;
                }

                //if fullscreen button exists
                if(fullscreenButton != null)
                {
                    //setup fullscreen button
                    fullscreenButton.setOnClickListener(new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View v)
                        {
                            boolean setFullscreen;
                            ActionBar pageActionBar = null;
                            FragmentActivity activity = page.getActivity();

                            //if can get action bar
                            if(activity instanceof AppCompatActivity)
                            {
                                //get action bar
                                pageActionBar = ((AppCompatActivity)activity).getSupportActionBar();
                            }

                             //set action bar
                            if(pageActionBar != null)
                            {
                                //get fullscreen status
                                setFullscreen = pageActionBar.isShowing();

                                //if fullscreen
                                if(setFullscreen)
                                {
                                    //hide action bar
                                    pageActionBar.hide();
                                }
                                else
                                {
                                    //show action bar
                                    pageActionBar.show();
                                }

                                //update titles, drawer, and button
                                mainPager.setSwipeEnabled(allowingPagerSwipe && !setFullscreen);
                                mainPagerTitles.setVisibility(!setFullscreen ? View.VISIBLE : View.GONE);
                                mainDrawerLayout.setDrawerLockMode(setFullscreen ? DrawerLayout.LOCK_MODE_LOCKED_CLOSED : DrawerLayout.LOCK_MODE_UNLOCKED);
                                fullscreenButton.setChecked(setFullscreen);
                            }
                        }
                    });
                }
            }
        });
    }

    //Creates an on paged changed listener
    private SwipeStateViewPager.OnPageChangeListener createOnPageChangedListener()
    {
        return(new SwipeStateViewPager.OnPageChangeListener()
        {
            private int lastGroup = -1;
            private int lastPosition = 0;

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            @Override
            public void onPageSelected(int position)
            {
                int subPage;
                int lastSubPage;
                boolean stopCamera = false;
                boolean startCamera = false;
                boolean allowSwipe = true;
                CameraLens cameraView = Current.getCameraView();

                cancelEditMode(mainPager);
                updateOptionsMenu();
                switch(mainGroup)
                {
                    case Groups.Current:
                        //if a valid last position
                        if(lastPosition < currentSubPage.length)
                        {
                            lastSubPage = currentSubPage[lastPosition];
                            if(lastPosition == Current.PageType.Combined)
                            {
                                //if were using lens
                                if(lastSubPage == Globals.SubPageType.CameraLens || lastSubPage == Globals.SubPageType.VirtualLens)
                                {
                                    //make sure camera stops
                                    stopCamera = true;
                                }
                            }
                        }

                        subPage = currentSubPage[position];
                        if(position == Current.PageType.Combined)
                        {
                            //if now using lens
                            if(subPage == Globals.SubPageType.CameraLens || subPage == Globals.SubPageType.VirtualLens)
                            {
                                //start camera
                                startCamera = true;
                            }

                            //don't allow swiping if on map/globe or a lens
                            allowSwipe = (subPage != Globals.SubPageType.Map && subPage != Globals.SubPageType.Globe && !startCamera);
                        }

                        //if coming from another group
                        if(lastGroup != mainGroup)
                        {
                            //update current calculation status
                            updateCurrentCalculations();
                        }
                        break;

                    case Groups.Calculate:
                        //if a valid last position
                        if(lastPosition < calculateSubPage.length)
                        {
                            lastSubPage = calculateSubPage[lastPosition];
                            switch(lastPosition)
                            {
                                case Calculate.PageType.View:
                                case Calculate.PageType.Passes:
                                case Calculate.PageType.Intersection:
                                    //if were using lens
                                    if(lastSubPage == Globals.SubPageType.CameraLens || lastSubPage == Globals.SubPageType.VirtualLens)
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
                                    break;
                            }
                        }

                        subPage = calculateSubPage[position];
                        switch(position)
                        {
                            case Calculate.PageType.View:
                            case Calculate.PageType.Passes:
                            case Calculate.PageType.Intersection:
                                //if now using lens
                                if(subPage == Globals.SubPageType.CameraLens || subPage == Globals.SubPageType.VirtualLens)
                                {
                                    //start camera
                                    startCamera = true;
                                }

                                //don't allow swiping if on a lens
                                allowSwipe = !startCamera;
                                break;

                            case Calculate.PageType.Coordinates:
                                //don't allow swiping if on map/globe
                                allowSwipe = (subPage != Globals.SubPageType.Map && subPage != Globals.SubPageType.Globe);
                                break;
                        }
                        break;
                }

                //if need to stop or start camera and camera exists
                if((stopCamera || startCamera) && cameraView != null)
                {
                    //if need to stop camera
                    if(stopCamera)
                    {
                        //stop camera
                        cameraView.stopCamera(true);
                    }

                    //if need to start camera
                    if(startCamera)
                    {
                        //start camera
                        cameraView.startCamera();
                    }
                }

                //update last group and position
                lastGroup = mainGroup;
                lastPosition = position;

                //update pager swipe
                allowingPagerSwipe = allowSwipe;
                mainPager.setSwipeEnabled(allowingPagerSwipe);
            }

            @Override
            public void onPageScrollStateChanged(int state) {}
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

    //Creates an on update needed listener
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

    //Creates an on child click listener
    private ExpandableListView.OnChildClickListener createOnSideMenuChildClickListener()
    {
        return(new ExpandableListView.OnChildClickListener()
        {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id)
            {
                boolean forSettings = (groupPosition == Groups.Settings);

                //if for settings
                if(forSettings)
                {
                    Intent startIntent = new Intent(MainActivity.this, SettingsActivity.class);
                    String startScreenValue;

                    //get arguments for position
                    switch(childPosition)
                    {
                        case 0:
                            startScreenValue = SettingsActivity.ScreenKey.Display;
                            break;

                        case 1:
                            startScreenValue = SettingsActivity.ScreenKey.Locations;
                            break;

                        case 2:
                            startScreenValue = SettingsActivity.ScreenKey.Notifications;
                            break;

                        case 3:
                            startScreenValue = SettingsActivity.ScreenKey.Updates;
                            break;

                        case 4:
                        default:
                            startScreenValue = "";
                            break;
                    }

                    //start settings activity
                    startIntent.putExtra(SettingsActivity.EXTRA_START_SCREEN, startScreenValue);
                    Globals.startActivityForResult(resultLauncher, startIntent, RequestCode.Settings);
                }
                else
                {
                    //update display and page selection
                    setMainGroup(groupPosition, false);
                    setMainPage(childPosition);
                }

                //close menu
                mainDrawerLayout.closeDrawers();
                if(forSettings && sideMenu != null)
                {
                    sideMenu.collapseGroup(groupPosition);
                    lastSideMenuPosition = groupPosition;
                }

                //click handled
                return(true);
            }
        });
    }

    //Creates an on group expand listener
    private ExpandableListView.OnGroupExpandListener createOnSideMenuGroupExpandListener()
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

        //clear any pending
        Current.clearMapPendingMarkerScale();
        Current.clearLensPendingStarMagnitude();

        return(new Runnable()
        {
            private final boolean[] updateList = new boolean[Current.PageType.PageCount];
            private final int lensVirtualLightType = Settings.getLensVirtualLightType(MainActivity.this);
            private int lastMapNoradId = Universe.IDs.None;
            private double lastJulianDate = Double.MAX_VALUE;

            @Override
            public void run()
            {
                int index;
                int combinedSubPage = currentSubPage[Current.PageType.Combined];
                boolean currentIsSatellite;
                boolean updateElapsed = false;
                boolean onMap = (combinedSubPage == Globals.SubPageType.Map || combinedSubPage == Globals.SubPageType.Globe);
                boolean onList = (combinedSubPage == Globals.SubPageType.List);
                boolean onLens = (combinedSubPage == Globals.SubPageType.CameraLens || combinedSubPage == Globals.SubPageType.VirtualLens);
                boolean haveMapSelected = haveSelectedMapSatellite();
                boolean haveLensSelected = haveSelectedLensSatellite();
                boolean mapMultiOrbitals = (onMap && !haveMapSelected);
                boolean mapSingleOrbital = (onMap && haveMapSelected);
                boolean lensMultiOrbitals = (onLens && !haveLensSelected);
                boolean lensSingleOrbital = (onLens && haveLensSelected);
                boolean onCurrentMapId;
                boolean onCurrentLensId;
                boolean onCurrentLensChildId;
                boolean havePendingMarkerScale = Current.getHaveMapPendingMarkerScale();
                boolean havePendingLensStarMagnitude = Current.getHaveLensPendingStarMagnitude();
                long travelSeconds;
                long currentSystemElapsedSeconds = (SystemClock.elapsedRealtime() / 1000);
                float pendingMarkerScale = Current.getMapPendingMarkerScale();
                float pendingLensStarMagnitude = Current.getLensPendingStarMagnitude();
                double julianDate;
                double sunElevationDegs = Double.MAX_VALUE;
                String coordinateString = null;
                TextView mapInfoText = Current.getMapInfoText();
                CameraLens cameraView = Current.getCameraView();
                CoordinatesFragment mapView = Current.getMapViewIfReady();
                TopographicDataType topographicData;
                Database.DatabaseSatellite currentLensOrbital = (lensSingleOrbital ? Database.getOrbital(MainActivity.this, lensViewNoradID) : null);
                ArrayList<Integer> currentLensChildIds = (currentLensOrbital != null ? currentLensOrbital.getChildIds() : null);
                TopographicDataType[] lookAngles = new TopographicDataType[currentSatellites.length];
                ArrayList<TopographicDataType> selectedLookAngles = null;
                ArrayList<Database.SatelliteData> selectedSatellites = null;

                //go through update list status
                for(index = 0; index < Current.PageType.PageCount; index++)
                {
                    //reset
                    updateList[index] = false;
                }

                //if not waiting on location update
                if(!pendingLocationUpdate)
                {
                    //if on map
                    if(onMap)
                    {
                        julianDate = Calculations.julianDateCalendar(Globals.getGMTTime(Current.mapMillisecondsPlayBar));
                    }
                    else
                    {
                        julianDate = Globals.getJulianDate();
                    }

                    //if enough time has passed
                    travelSeconds = (currentSystemElapsedSeconds - lastSystemElapsedSeconds.value);
                    if(travelSeconds >= 2)
                    {
                        //update later
                        updateElapsed = true;
                    }

                    //if showing current location on map
                    if(onMap && Current.currentLocationMarker != null && Current.currentLocationMarker.getInfoVisible())
                    {
                        //update info display
                        coordinateString = Globals.getCoordinateString(MainActivity.this, observer.geo.latitude, observer.geo.longitude, observer.geo.altitudeKm);
                        Current.currentLocationMarker.setInfoLocation();
                        Current.currentLocationMarker.setInfoVisible(true);
                    }

                    //go through each satellite
                    for(index = 0; index < currentSatellites.length; index++)
                    {
                        //get current orbital data, type, if star, and if in filter
                        Database.SatelliteData currentOrbitalData = currentSatellites[index];
                        byte currentOrbitalType = currentOrbitalData.getOrbitalType();
                        boolean isStar = (currentOrbitalType == Database.OrbitalType.Star);
                        boolean currentInFilter = currentOrbitalData.getInFilter();
                        boolean isStarForConstellation = (isStar && Current.showingConstellations && onLens);

                        //if current orbital TLE is accurate
                        if(currentOrbitalData.getTLEIsAccurate())
                        {
                            //get current satellite, ID, and look angle
                            boolean changedEnough = false;
                            SatelliteObjectType currentOrbital = currentOrbitalData.satellite;
                            int currentNoradId = currentOrbital.getSatelliteNum();
                            boolean isSun = (currentNoradId == Universe.IDs.Sun);
                            currentIsSatellite = (currentNoradId > 0);
                            onCurrentMapId = (onMap && currentNoradId == mapViewNoradID);
                            onCurrentLensId = (onLens && currentNoradId == lensViewNoradID);
                            onCurrentLensChildId = (onLens && currentLensChildIds != null && currentLensChildIds.contains(currentNoradId));

                            //if -in filter- or -a star being used in a constellation- or -is sun and need for lens-
                            if(currentInFilter || isStarForConstellation || (isSun && onLens))
                            {
                                Calculations.updateOrbitalPosition(currentOrbital, observer, julianDate, true);
                                topographicData = Calculations.getLookAngles(observer, currentOrbital, true);

                                //if the sun
                                if(isSun)
                                {
                                    //remember sun elevation
                                    sunElevationDegs = topographicData.elevation;

                                    //if have last elevation
                                    if(sunLastEl.value != Double.MAX_VALUE)
                                    {
                                        //if changed enough
                                        changedEnough = (Math.abs(sunElevationDegs - sunLastEl.value) >= Universe.Sun.MinElevationPhaseChange);
                                    }

                                    //if -had no last elevation- or -changed enough-
                                    if(sunLastEl.value == Double.MAX_VALUE || changedEnough)
                                    {
                                        //update last elevation
                                        sunLastEl.value = sunElevationDegs;
                                    }
                                }
                            }
                            else
                            {
                                topographicData = new TopographicDataType();
                            }

                            //if not a satellite
                            if(!currentIsSatellite)
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
                                    currentOrbital.geo.speedKmS = (currentInFilter ? (Calculations.getDistanceKm(currentOrbital.savedGeo, currentOrbital.geo) / travelSeconds) : 0);
                                    currentOrbital.savedGeo = new GeodeticDataType(currentOrbital.geo);
                                }
                            }

                            //if -on current lens orbital- or -on lens child being used- or -in filter- or -a star being used in a constellation-
                            if(onCurrentLensId || onCurrentLensChildId || currentInFilter || isStarForConstellation)
                            {
                                //if on current lens orbital or lens child being used
                                if(onCurrentLensId || onCurrentLensChildId)
                                {
                                    //if lists are not created yet
                                    if(selectedSatellites == null)
                                    {
                                        //create satellites
                                        selectedSatellites = new ArrayList<>(0);
                                    }
                                    if(selectedLookAngles == null)
                                    {
                                        //create look angles
                                        selectedLookAngles = new ArrayList<>(0);
                                    }

                                    //if on current lens orbital
                                    if(onCurrentLensId)
                                    {
                                        //always add current lens orbital to front
                                        selectedSatellites.add(0, currentOrbitalData);
                                        selectedLookAngles.add(0, topographicData);
                                    }
                                    else
                                    {
                                        //add child lens
                                        selectedSatellites.add(currentOrbitalData);
                                        selectedLookAngles.add(topographicData);
                                    }
                                }

                                //if -on list- or -viewing multiple lens orbitals- or -on current lens orbital-
                                if(onList || lensMultiOrbitals || onCurrentLensId)
                                {
                                    //remember look angle
                                    lookAngles[index] = topographicData;

                                    //if in filter, combined list exists, and showing list
                                    if(currentInFilter && onList && Current.PageAdapter.hasItems(Current.PageType.Combined))
                                    {
                                        //get current item
                                        Current.Combined.Item currentItem = Current.PageAdapter.getCombinedItemByNorad(currentNoradId);
                                        if(currentItem != null)
                                        {
                                            //update values
                                            currentItem.azimuth = (float)topographicData.azimuth;
                                            currentItem.elevation = (float)topographicData.elevation;
                                            currentItem.rangeKm = (float)topographicData.rangeKm;
                                            currentItem.speedKms = (float)currentOrbital.geo.speedKmS;
                                            currentItem.latitude = (float)currentOrbital.geo.latitude;
                                            currentItem.longitude = (float)currentOrbital.geo.longitude;
                                            currentItem.altitudeKm = (float)currentOrbital.geo.altitudeKm;

                                            //update list later
                                            updateList[Current.PageType.Combined] = true;
                                        }
                                    }
                                }
                            }

                            //if on map, -on current map orbital or using multiple-, map exists, and the orbital markers exist
                            if(onMap && (onCurrentMapId || mapMultiOrbitals) && mapView != null && mapView.getOrbitalCount() > 0)
                            {
                                //get current marker
                                final CoordinatesFragment.OrbitalBase currentMarker = mapView.getOrbital(mapSingleOrbital ? 0 : index);
                                if(currentMarker != null)
                                {
                                    //remember if current orbital is selected
                                    boolean currentOrbitalSelected = (onCurrentMapId || currentNoradId == mapView.getSelectedNoradId());

                                    //if -in filter- and -using multiple orbitals or current is selected-
                                    if(currentInFilter && (mapMultiOrbitals || currentOrbitalSelected))
                                    {
                                        //remember latitude, longitude, and if info under title
                                        double currentLatitude = currentOrbital.geo.latitude;
                                        double currentLongitude = currentOrbital.geo.longitude;
                                        double currentAltitudeKm = currentOrbital.geo.altitudeKm;
                                        boolean selectionChanged = (currentNoradId != lastMapNoradId);
                                        boolean infoUnderTitle = Settings.usingMapMarkerInfoTitle();

                                        //skip layout until done
                                        currentMarker.setSkipLayout(true);

                                        //update showing selected footprint
                                        currentMarker.setShowSelectedFootprint(currentIsSatellite && currentOrbitalSelected && Settings.usingMapFootprintAndSelected());

                                        //if current is selected and selection changed
                                        if(currentOrbitalSelected && selectionChanged)
                                        {
                                            //if a single playback marker
                                            if(onCurrentMapId)
                                            {
                                                //keep selection
                                                mapView.selectOrbital(currentNoradId);
                                            }

                                            //make sure info is displayed
                                            currentMarker.setInfoVisible(true);
                                        }

                                        //update coordinates display
                                        if(currentMarker.getInfoVisible())
                                        {
                                            //refresh window and follow
                                            coordinateString = Globals.getCoordinateString(MainActivity.this, currentLatitude, currentLongitude, currentAltitudeKm);
                                            if(infoUnderTitle)
                                            {
                                                currentMarker.setText(coordinateString);
                                            }

                                            //if selection or julian date changed
                                            if((selectionChanged || julianDate != lastJulianDate) && mapView.getFollowSelected())
                                            {
                                                //update map view
                                                mapView.moveCamera(currentLatitude, currentLongitude, (selectionChanged && !mapView.isMap() ? CoordinatesFragment.Utils.getZoom(currentAltitudeKm) : mapView.getCameraZoom()));
                                            }

                                            //update last map ID
                                            lastMapNoradId = currentNoradId;
                                        }
                                        else if(infoUnderTitle)
                                        {
                                            //clear old text
                                            currentMarker.setText(null);
                                        }

                                        //move marker location and update status
                                        currentMarker.setShowFootprint(currentIsSatellite && !currentOrbitalSelected && Settings.usingMapShowFootprint());
                                        if(havePendingMarkerScale)
                                        {
                                            currentMarker.setScale(pendingMarkerScale);
                                        }
                                        currentMarker.moveLocation(currentLatitude, currentLongitude, currentAltitudeKm);
                                        currentMarker.setVisible(true);

                                        //update layout
                                        currentMarker.setSkipLayout(false);
                                    }
                                    else
                                    {
                                        //hide marker
                                        currentMarker.setVisible(false);
                                    }
                                }
                            }
                        }
                    }

                    //if on map, map info text exists, and using marker bottom info
                    if(onMap && mapInfoText != null && Settings.usingMapMarkerInfoBottom())
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
                                    mapInfoText.setText(infoString.replace("\n", Globals.COORDINATE_SEPARATOR));
                                    mapInfoText.setVisibility(View.VISIBLE);
                                }
                                else
                                {
                                    //hide coordinates
                                    mapInfoText.setVisibility(View.GONE);
                                }
                            }
                        });
                    }

                    //go through current pages
                    for(index = 0; index < Current.PageType.PageCount; index++)
                    {
                        //if there is a pending sort
                        if(Current.PageAdapter.hasPendingSort(index))
                        {
                            //try to sort page and notify of change
                            try
                            {
                                Current.PageAdapter.sortItems(index, Settings.getCurrentSortBy(index));
                                Current.PageAdapter.setPendingSort(index, false);
                                updateList[index] = true;
                            }
                            catch(Exception ex)
                            {
                                //try again later
                            }
                        }
                    }

                    //if on lens and it exists
                    if(onLens && cameraView != null)
                    {
                        //if not using auto lens light
                        if(lensVirtualLightType != Settings.Options.LensView.LightType.Auto)
                        {
                            //update used elevation
                            sunElevationDegs = (lensVirtualLightType == Settings.Options.LensView.LightType.Day ? 90.0 : -90.0);
                        }

                        //update positions
                        cameraView.updatePositions((selectedSatellites != null ? selectedSatellites.toArray(new Database.SatelliteData[0]) : currentSatellites), (selectedLookAngles != null ? selectedLookAngles.toArray(new TopographicDataType[0]) : lookAngles), sunElevationDegs, true);

                        //if have pending magnitude
                        if(havePendingLensStarMagnitude)
                        {
                            //update magnitude
                            cameraView.setStarMagnitude(pendingLensStarMagnitude);
                        }

                        //set to lens delay
                        timerDelay = lensTimerDelay;
                    }
                    //else if on map and it exists
                    else if(onMap && mapView != null)
                    {
                        //set time map delay
                        timerDelay = mapTimerDelay;
                    }
                    else
                    {
                        //set to list delay
                        timerDelay = listTimerDelay;
                    }

                    //if trying to show view path and not calculated yet
                    if(cameraView != null && cameraView.showPaths && (currentViewAnglesTask == null || (currentViewAnglesTask.needViews && !currentViewAnglesTask.isRunning())))
                    {
                        //start calculating views
                        setCurrentViewCalculations(true, julianDate);
                    }

                    //if trying to show coordinate path and not calculated yet
                    if(mapView != null && Current.mapShowPaths && (currentCoordinatesTask == null || (currentCoordinatesTask.needCoordinates && !currentCoordinatesTask.isRunning())))
                    {
                        //start calculating locations
                        setCurrentCoordinateCalculations(true, julianDate);
                    }

                    //if trying to update timeline and not calculated yet
                    if(currentTimelineAnglesTask == null || (currentTimelineAnglesTask.needViews && !currentTimelineAnglesTask.isRunning()))
                    {
                        //start calculating timeline
                        setCurrentTimelineCalculations(true);
                    }

                    //if update needed
                    for(index = 0; index < Current.PageType.PageCount; index++)
                    {
                        if(updateList[index])
                        {
                            //update list
                            Current.PageAdapter.notifyItemsChanged(index);
                        }
                    }
                    if(havePendingMarkerScale)
                    {
                        //clear pending scale
                        Current.clearMapPendingMarkerScale();
                    }
                    if(havePendingLensStarMagnitude)
                    {
                        //clear pending magnitude
                        Current.clearLensPendingStarMagnitude();
                    }
                    if(updateElapsed)
                    {
                        //update last time
                        lastSystemElapsedSeconds.value = currentSystemElapsedSeconds;
                    }

                    //update status
                    lastJulianDate = julianDate;
                    firstRun.value = false;
                }
            }
        });
    }

    //Creates timer runnable for calculate
    private Runnable createCalculateRunnable()
    {
        final Globals.BoolObject firstRun = new Globals.BoolObject(true);

        return(new Runnable()
        {
            final int lensVirtualLightType = Settings.getLensVirtualLightType(MainActivity.this);
            Database.SatelliteData sunOrbital = null;
            Database.SatelliteData[] currentSatellites = null;
            CalculateViewsTask.OrbitalView[][] currentViews = null;

            @Override
            public void run()
            {
                int index;
                int index2;
                CameraLens cameraView = Current.getCameraView();

                //if lens exists
                if(cameraView != null)
                {
                    //if first run
                    if(firstRun.value)
                    {
                        int subPage;
                        int noradId;
                        int multiCount = 0;
                        int orbitalCount;
                        int firstViewsLength;
                        String zoneId;
                        Bundle params;
                        Context context = MainActivity.this;
                        Database.SatelliteData currentSatellite1 = null;
                        Database.SatelliteData currentSatellite2 = null;
                        CalculateViewsTask.OrbitalView[] views = null;
                        CalculateViewsTask.OrbitalView[] views2 = null;
                        Calculate.Passes.Item[] passesItems;
                        Calculate.ViewAngles.Item[] angleItems;
                        CalculateViewsTask.OrbitalView[] firstViews;
                        ArrayList<Integer> multiNoradId = null;
                        ArrayList<Database.SatelliteData> currentChildList = new ArrayList<>(0);
                        ArrayList<Database.SatelliteData> currentSatelliteList = new ArrayList<>(0);
                        ArrayList<CalculateViewsTask.OrbitalView[]> currentViewList = new ArrayList<>(0);

                        //if using auto lens light
                        if(lensVirtualLightType == Settings.Options.LensView.LightType.Auto)
                        {
                            //get sun orbital
                            sunOrbital = new Database.SatelliteData(context, Universe.IDs.Sun);
                        }

                        //if using lens and angles list exists
                        subPage = calculateSubPage[Calculate.PageType.View];
                        if((subPage == Globals.SubPageType.CameraLens || subPage == Globals.SubPageType.VirtualLens) && Calculate.PageAdapter.hasItems(Calculate.PageType.View))
                        {
                            CalculateViewsTask.OrbitalView[][] lensViews;
                            Database.DatabaseSatellite[] currentOrbitals;

                            //get items
                            zoneId = observer.timeZone.getID();
                            angleItems = Calculate.PageAdapter.getViewAngleItems();
                            orbitalCount = (angleItems.length > 0 ? angleItems[0].views.length : 0);
                            lensViews = new CalculateViewsTask.OrbitalView[orbitalCount][angleItems.length];
                            currentOrbitals = new Database.DatabaseSatellite[orbitalCount];
                            for(index = 0; index < orbitalCount; index++)
                            {
                                //if on the first index
                                if(index == 0)
                                {
                                    //go through each view
                                    for(index2 = 0; index2 < orbitalCount; index2++)
                                    {
                                        //set orbital
                                        currentOrbitals[index2] = Database.getOrbital(context, angleItems[0].views[index2].noradId);
                                    }
                                }

                                //go through each item
                                for(index2 = 0; index2 < angleItems.length; index2++)
                                {
                                    //remember current item, orbital, and possibly a previous time string
                                    Calculate.ViewAngles.Item currentAngleItem = angleItems[index2];
                                    Database.DatabaseSatellite currentOrbital = currentOrbitals[index];
                                    CalculateViewsTask.OrbitalView previousOrbitalItem = (index > 0 ? lensViews[index - 1][index2] : null);
                                    boolean havePrevious = (previousOrbitalItem != null);

                                    //set view
                                    lensViews[index][index2] = new CalculateViewsTask.OrbitalView(context, currentAngleItem.views[index].azimuth, currentAngleItem.views[index].elevation, currentAngleItem.views[index].rangeKm, currentAngleItem.julianDate, (havePrevious ? previousOrbitalItem.gmtTime : null), zoneId, currentAngleItem.views[index].illumination, (currentOrbital != null ? currentOrbital.getName() : null), currentAngleItem.views[index].phaseName, (havePrevious ? previousOrbitalItem.timeString : null));
                                }
                            }
                            currentViewList.addAll(Arrays.asList(lensViews));

                            //set params and current satellite
                            params = Calculate.PageAdapter.getParams(Calculate.PageType.View);
                            if(params != null)
                            {
                                noradId = params.getInt(Calculate.ParamTypes.NoradId, Universe.IDs.Invalid);
                                multiNoradId = params.getIntegerArrayList(Calculate.ParamTypes.MultiNoradId);
                                multiCount = (multiNoradId != null ? multiNoradId.size() : 0);
                                if(noradId == Universe.IDs.Invalid && multiCount == 1)
                                {
                                    noradId = multiNoradId.get(0);
                                }
                                currentSatellite1 = (multiCount > 1 ? null : (new Database.SatelliteData(context, noradId)));
                            }
                        }

                        //if using lens and passes list exists
                        subPage = calculateSubPage[Calculate.PageType.Passes];
                        if((subPage == Globals.SubPageType.CameraLens || subPage == Globals.SubPageType.VirtualLens) && Calculate.PageAdapter.hasItems(Calculate.PageType.Passes))
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
                        subPage = calculateSubPage[Calculate.PageType.Intersection];
                        if((subPage == Globals.SubPageType.CameraLens || subPage == Globals.SubPageType.VirtualLens) && Calculate.PageAdapter.hasItems(Calculate.PageType.Intersection))
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

                        //if current satellite is set
                        if(currentSatellite1 != null)
                        {
                            //add satellite to list
                            currentSatelliteList.add(currentSatellite1);

                            //if current satellite 2 is set
                            if(currentSatellite2 != null)
                            {
                                //add satellite 2 to list
                                currentSatelliteList.add(currentSatellite2);

                                //if both views are set
                                if(views != null && views2 != null)
                                {
                                    //add both satellite views to list
                                    currentViewList.add(views);
                                    currentViewList.add(views2);
                                }
                            }
                            else
                            {
                                //if views are set
                                if(views != null)
                                {
                                    //add first satellite views to list
                                    currentViewList.add(views);
                                }
                            }
                        }
                        //else if multiple satellites
                        else if(multiCount > 0)
                        {
                            //add selected multiple satellites to list
                            currentSatelliteList.addAll(Arrays.asList(Database.SatelliteData.getSatellites(context, multiNoradId)));
                        }

                        //remember first views and length
                        firstViews = (!currentViewList.isEmpty() ? currentViewList.get(0) : null);
                        firstViewsLength = (firstViews != null ? firstViews.length : 0);

                        //go through each satellite
                        for(Database.SatelliteData currentSatellite : currentSatelliteList)
                        {
                            //get any child IDs
                            ArrayList<Integer> childIds = currentSatellite.getChildIds();

                            //go through each ID
                            for(int currentId : childIds)
                            {
                                //get child satellite
                                Database.SatelliteData childSatellite = new Database.SatelliteData(context, currentId);

                                //if not already in child or main list
                                if(!currentChildList.contains(childSatellite) && !currentSatelliteList.contains(childSatellite))
                                {
                                    CalculateViewsTask.OrbitalView[] childViews = new CalculateViewsTask.OrbitalView[firstViewsLength];

                                    //go through first views and add equivalent to list
                                    for(index = 0; index < firstViewsLength; index++)
                                    {
                                        TopographicDataType topographicData;
                                        CalculateViewsTask.OrbitalView currentView = new CalculateViewsTask.OrbitalView(firstViews[index]);

                                        //set child views
                                        Calculations.updateOrbitalPosition(childSatellite.satellite, observer, currentView.julianDate, true);
                                        topographicData = Calculations.getLookAngles(observer, childSatellite.satellite, true);
                                        currentView.azimuth = topographicData.azimuth;
                                        currentView.elevation = topographicData.elevation;
                                        currentView.rangeKm = topographicData.rangeKm;
                                        childViews[index] = currentView;
                                    }

                                    //set as not in filter
                                    childSatellite.setInFilter(false);

                                    //add to lists
                                    currentChildList.add(childSatellite);
                                    currentViewList.add(childViews);
                                }
                            }
                        }

                        //if there were children satellites added
                        if(!currentChildList.isEmpty())
                        {
                            //add them to the list
                            currentSatelliteList.addAll(currentChildList);
                        }

                        //set current satellites, views, and manually reset camera parent states
                        currentSatellites = currentSatelliteList.toArray(new Database.SatelliteData[0]);
                        currentViews = currentViewList.toArray(new CalculateViewsTask.OrbitalView[0][]);
                        cameraView.resetParentStates(currentSatellites);
                    }

                    //if satellites, views, and bar are set
                    if(currentSatellites != null && currentViews != null && cameraView.playBar != null)
                    {
                        //get play index and progress and set topographic data
                        int playIndex = cameraView.playBar.getValue();
                        double subProgressPercent = cameraView.playBar.getSubProgressPercent();
                        double currentJulianDate;
                        double nextJulianDate;
                        double usedJulianDate = Double.MAX_VALUE;
                        TopographicDataType sunTopographicData;
                        TopographicDataType[] currentPlayTopographicData = new TopographicDataType[currentViews.length];

                        //go through each view set
                        for(index = 0; index < currentViews.length; index++)
                        {
                            //remember current satellite views
                            CalculateViewsTask.OrbitalView[] currentSatelliteViews = currentViews[index];
                            TopographicDataType currentTopographicData = null;

                            //if a valid play index
                            if(playIndex < currentSatelliteViews.length)
                            {
                                //remember current view and date
                                CalculateViewsTask.OrbitalView currentView = currentSatelliteViews[playIndex];
                                currentJulianDate = currentView.julianDate;

                                //if more indexes after current
                                if(playIndex + 1 < currentSatelliteViews.length)
                                {
                                    //get add distance index percentage to current view and remember next date
                                    CalculateViewsTask.OrbitalView nextView = currentSatelliteViews[playIndex + 1];
                                    currentTopographicData = new TopographicDataType();
                                    currentTopographicData.azimuth = Globals.normalizeAngle(currentView.azimuth + (Globals.degreeDistance(currentView.azimuth, nextView.azimuth) * subProgressPercent));
                                    currentTopographicData.elevation = Globals.normalizeAngle(currentView.elevation + (Globals.degreeDistance(currentView.elevation, nextView.elevation) * subProgressPercent));
                                    currentTopographicData.rangeKm = (currentView.rangeKm + nextView.rangeKm) / 2;
                                    nextJulianDate = nextView.julianDate;
                                }
                                else
                                {
                                    //set look angles
                                    currentTopographicData = new TopographicDataType(currentView.azimuth, currentView.elevation, currentView.rangeKm);
                                    nextJulianDate = Double.MAX_VALUE;
                                }

                                //if used date is not set yet
                                if(usedJulianDate == Double.MAX_VALUE)
                                {
                                    //set used date
                                    usedJulianDate = currentJulianDate + (nextJulianDate != Double.MAX_VALUE ? ((nextJulianDate - currentJulianDate) * subProgressPercent) : 0);
                                }
                            }
                            currentPlayTopographicData[index] = currentTopographicData;
                        }

                        //go through topographic data
                        for(index = 0; index < currentPlayTopographicData.length; index++)
                        {
                            //if not set
                            if(currentPlayTopographicData[index] == null)
                            {
                                //clear all data
                                currentPlayTopographicData = new TopographicDataType[0];
                            }
                        }

                        //show paths and update positions/travel
                        cameraView.showPaths = true;
                        if(sunOrbital != null && usedJulianDate != Double.MAX_VALUE)
                        {
                            Calculations.updateOrbitalPosition(sunOrbital.satellite, observer, usedJulianDate, false);
                            sunTopographicData = Calculations.getLookAngles(observer, sunOrbital.satellite, true);
                        }
                        else
                        {
                            sunTopographicData = new TopographicDataType(0.0, (lensVirtualLightType == Settings.Options.LensView.LightType.Day ? 90.0 : -90.0), 0.0);
                        }
                        cameraView.updatePositions(currentSatellites, currentPlayTopographicData, sunTopographicData.elevation, false);
                        for(index = 0; index < currentViews.length; index++)
                        {
                            cameraView.setTravel(index, currentViews[index]);
                        }

                        //update delay
                        timerDelay = lensTimerDelay;
                    }

                    //update status
                    firstRun.value = false;
                }
            }
        });
    }

    //Returns the current sub page
    private int getSubPage()
    {
        int page = getMainPage();
        return(mainGroup == Groups.Current ? currentSubPage[page] : mainGroup == Groups.Calculate ? calculateSubPage[page] : 0);
    }

    //Sets the sub page for the given group
    private void setSubPage(int group, int page, int subPage)
    {
        //handle based on group
        switch(group)
        {
            case Groups.Current:
                if(currentPageAdapter != null)
                {
                    currentSubPage[page] = subPage;
                    currentPageAdapter.setSubPage(page, subPage);

                    //clear any selected map and lens
                    mapViewNoradID = Integer.MAX_VALUE;
                    lensViewNoradID = Integer.MAX_VALUE;
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
                            if(subPage == Globals.SubPageType.CameraLens || subPage == Globals.SubPageType.VirtualLens)
                            {
                                //go through other pages that use lens
                                for(int currentOtherPage : new int[]{Calculate.PageType.View, Calculate.PageType.Passes, Calculate.PageType.Intersection})
                                {
                                    //if not the current page and page is using lens
                                    if(currentOtherPage != page && (calculateSubPage[currentOtherPage] == Globals.SubPageType.CameraLens || calculateSubPage[currentOtherPage] == Globals.SubPageType.VirtualLens))
                                    {
                                        //set to input
                                        calculateSubPage[currentOtherPage] = Globals.SubPageType.Input;
                                    }
                                }
                            }
                            break;
                    }
                }
                break;
        }
    }

    //Starts/stops current timer updates
    private void setTimer(boolean run)
    {
        //update timer delays
        updateTimerDelays();

        //if timer was running
        if(timerTask != null)
        {
            //cancel it
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
            final Runnable timerRunnable;

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
                    boolean onTimeline = (onCurrent && page == Current.PageType.Timeline);
                    boolean onCalculateLens = (onCalculate && page < Calculate.PageType.PageCount && (calculateSubPage[page] == Globals.SubPageType.CameraLens || calculateSubPage[page] == Globals.SubPageType.VirtualLens));
                    long repeatMs;
                    long startTimeMs = System.currentTimeMillis();

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
                                //go through each page while no pending sort found
                                for(index = 0; index < Current.PageType.PageCount && !pendingSort; index++)
                                {
                                    //update if pending sort
                                    pendingSort = Current.PageAdapter.hasPendingSort(index);
                                }
                            }

                            //if -no pending sort- and -on calculate but not showing any lens-
                            if(!pendingSort && (onCalculate && !onCalculateLens))
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

                        //wait only as long as needed to match delay
                        repeatMs = (onTimeline ? 2000 : timerDelay) - (System.currentTimeMillis() - startTimeMs);
                        if(repeatMs < 1)
                        {
                            //make sure task keeps running
                            repeatMs = 1;
                        }

                        //update delay
                        timerTask.setRepeatMs(repeatMs);
                    }
                }
            };
            timerTask = new ThreadTask<>(timerRunnable, timerDelay);
            timerTask.execute();
        }
    }

    //Gets current page action button
    private FloatingActionStateButton getCurrentActionButton()
    {
        //if page adapter is set
        if(currentPageAdapter != null)
        {
            //if page is set and a Current.Page
            Selectable.ListFragment page = currentPageAdapter.getPage(mainPager, Current.PageType.Combined);
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
        //get used lens satellites
        Database.SatelliteData[] lensSatellites = (run ? getLensSatellites(this) : null);

        //if task was running
        if(currentViewAnglesTask != null)
        {
            //cancel it
            currentViewAnglesTask.cancel(true);
        }

        //if want to run and have items
        if(run && lensSatellites != null && lensSatellites.length > 0)
        {
            //get action button
            final FloatingActionStateButton actionButton = getCurrentActionButton();

            //create and run task
            currentViewAnglesTask = Current.calculateViews(this, lensSatellites, haveSelectedLensSatellite(), observer, julianDate, julianDate + 1, 0.2 / 24, new CalculateViewsTask.OnProgressChangedListener()
            {
                @Override
                public void onProgressChanged(final int progressType, final int satelliteIndex, CalculateService.ViewListItem item, final ArrayList<CalculateViewsTask.OrbitalView> pathPoints)
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
                        case Globals.ProgressType.Failed:
                            //set runnable
                            progressRunnable = new Runnable()
                            {
                                @Override
                                public synchronized void run()
                                {
                                    CameraLens cameraView = Current.getCameraView();
                                    CalculateViewsTask.OrbitalView[] views = (pathPoints != null ? pathPoints.toArray(new CalculateViewsTask.OrbitalView[0]) : null);

                                    //if camera view still exists
                                    if(cameraView != null)
                                    {
                                        //set camera travel points
                                        cameraView.setTravel(satelliteIndex, views);
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
        boolean haveCombined;
        boolean onCurrent = (mainGroup == Groups.Current);
        CalculateService.PassData[] passItems;

        //if task was running
        if(currentPassesTask != null)
        {
            //cancel it
            currentPassesTask.cancel(true);
        }

        //if want to run, not waiting for location update, and have items
        haveCombined = (onCurrent && Current.PageAdapter.hasItems(Current.PageType.Combined));
        if(run && (!pendingLocationUpdate || locationSource != Database.LocationType.Current) && observer != null && observer.geo != null && (observer.geo.isSet()) && haveCombined)
        {
            //get items
            passItems = Current.PageAdapter.getCombinedItems();
            if(passItems != null)
            {
                //go through each item
                for(index = 0; index < passItems.length; index++)
                {
                    //reset calculating status
                    passItems[index].clearPass();
                }
            }

            //start task
            currentPassesTask = CalculateService.calculateOrbitalsPasses(this, passItems, observer, 0, Globals.getGMTTime(), new CalculateService.CalculateListener()
            {
                @Override
                public void onCalculated(int progressType, final CalculateService.PassData pass)
                {
                    //if pass it set and not none
                    if(pass != null)
                    {
                        boolean pendingSort = false;
                        Activity activity = MainActivity.this;
                        int sortBy = Settings.getCurrentSortBy(Current.PageType.Combined);
                        final Current.Combined.Item currentItem = (pass instanceof Current.Combined.Item ? (Current.Combined.Item)pass : null);

                        //if current item and observer are set
                        if(currentItem != null && observer != null)
                        {
                            //handle by any needed sort
                            switch(sortBy)
                            {
                                case Current.Items.SortBy.PassStartTime:
                                case Current.Items.SortBy.PassDuration:
                                case Current.Items.SortBy.MaxElevation:
                                    pendingSort = true;
                                    //fall through

                                default:
                                    final boolean needSort = pendingSort;
                                    activity.runOnUiThread(new Runnable()
                                    {
                                        @Override
                                        public void run()
                                        {
                                            //update displays
                                            currentItem.setLoading(activity, observer.timeZone, false);
                                            currentItem.updateDisplays(activity);

                                            //if need to sort
                                            if(needSort)
                                            {
                                                //set pending sort
                                                Current.PageAdapter.setPendingSort(Current.PageType.Combined, true);
                                            }
                                        }
                                    });
                                    break;
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
        final CoordinatesFragment mapView = Current.getMapViewIfReady();

        //if task was running
        if(currentCoordinatesTask != null)
        {
            //cancel it
            currentCoordinatesTask.cancel(true);
        }

        //if want to run and have items
        if(run && mapView != null && mapView.getOrbitalCount() > 0)
        {
            //get action button
            final FloatingActionStateButton actionButton = getCurrentActionButton();

            //run task
            currentCoordinatesTask = Current.calculateCoordinates(observer, julianDate, new Current.CalculatePathTask.OnProgressChangedListener()
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
                                    //update location points
                                    mapView.setPath(index, pathPoints);
                                    mapView.setPathVisible(index, (!haveSelectedMapSatellite() || mapViewNoradID == mapView.getOrbitalNoradId(index)));
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

    //Starts/stops current pass calculations
    private void setCurrentTimelineCalculations(boolean run)
    {
        boolean haveTimeline;
        boolean onCurrent = (mainGroup == Groups.Current);
        Current.Timeline.Item[] timelineItems;

        //if task was running
        if(currentTimelineAnglesTask != null)
        {
            //cancel it
            currentTimelineAnglesTask.cancel(true);
        }

        //if want to run, not waiting for location update, and have items
        haveTimeline = (onCurrent && Current.PageAdapter.hasItems(Current.PageType.Timeline));
        if(run && (!pendingLocationUpdate || locationSource != Database.LocationType.Current) && observer != null && observer.geo != null && (observer.geo.isSet()) && haveTimeline)
        {
            double julianDate;
            Calendar usedDate = Globals.getGMTTime();

            //start at beginning of current hour
            usedDate.set(Calendar.MINUTE, 0);
            usedDate.set(Calendar.SECOND, 0);
            usedDate.set(Calendar.MILLISECOND, 0);
            julianDate = Calculations.julianDateCalendar(usedDate);

            //get items
            timelineItems = Current.PageAdapter.getTimelineItems();

            //create and run task
            currentTimelineAnglesTask = Calculate.calculateViews(this, timelineItems, null, observer, julianDate, julianDate + (Settings.getTimelineUsedTotalMinutes(this) / 1440.0), 2 / Calculations.MinutesPerDay, new CalculateViewsTask.OnProgressChangedListener()
            {
                @Override
                public void onProgressChanged(int progressType, int satelliteIndex, CalculateService.ViewListItem item, ArrayList<CalculateViewsTask.OrbitalView> pathPoints)
                {
                    //if success and timeline item is set
                    if(progressType == Globals.ProgressType.Success && item instanceof Current.Timeline.Item)
                    {
                        boolean pendingSort = false;
                        int index;
                        int sortBy = Settings.getCurrentSortBy(Current.PageType.Timeline);
                        Activity activity = MainActivity.this;
                        Current.Timeline.Item currentItem = (Current.Timeline.Item)item;
                        CalculateViewsTask.ViewData[] views = new CalculateViewsTask.ViewData[pathPoints.size()];

                        //go through each point
                        for(index = 0; index < views.length; index++)
                        {
                            //copy view
                            views[index] = new CalculateViewsTask.ViewData(item.getSatelliteNum(), pathPoints.get(index));
                        }

                        //set views
                        currentItem.setViews(views);

                        //handle any needed sort
                        switch(sortBy)
                        {
                            case Current.Items.SortBy.PassStartTime:
                            case Current.Items.SortBy.PassDuration:
                            case Current.Items.SortBy.MaxElevation:
                                pendingSort = true;
                                //fall through

                            default:
                                final boolean needSort = pendingSort;
                                activity.runOnUiThread(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        //update displays
                                        currentItem.setLoading(false);
                                        currentItem.updateDisplays();

                                        //if need to sort
                                        if(needSort)
                                        {
                                            //set pending sort
                                            Current.PageAdapter.setPendingSort(Current.PageType.Timeline, true);
                                        }
                                    }
                                });
                                break;
                        }
                    }
                }
            });
        }
    }

    //Starts/stops calculate view calculations
    private void setCalculateViewCalculations(boolean run, final CalculateService.ViewListItem[] viewItems, final double julianDateStart, final double julianDateEnd, double dayIncrement)
    {
        boolean satelliteChanged;
        int index;
        int oldNoradId;
        Bundle params;
        CalculateService.ViewListItem firstItem = (viewItems != null && viewItems.length > 0 ? viewItems[0] : null);
        ArrayList<Integer> oldMultiNoradId = new ArrayList<>(0);
        Calculate.ViewAngles.Item[] savedViewItems;

        //if task was running
        if(calculateViewAnglesTask != null)
        {
            //cancel it
            calculateViewAnglesTask.cancel(true);
        }

        //if want to run and have an item
        if(run && firstItem != null)
        {
            //get any saved items and params
            savedViewItems = (Calculate.ViewAngles.Item[])Calculate.PageAdapter.getSavedItems(Calculate.PageType.View);
            params = Calculate.PageAdapter.getParams(Calculate.PageType.View);

            //check if params changed
            if(params != null && savedViewItems != null)
            {
                //get old satellite data
                oldNoradId = params.getInt(Calculate.ParamTypes.NoradIdOld, Universe.IDs.Invalid);
                if(oldNoradId != Universe.IDs.Invalid)
                {
                    oldMultiNoradId.add(oldNoradId);
                }
                else
                {
                    oldMultiNoradId = params.getIntegerArrayList(Calculate.ParamTypes.MultiNoradIdOld);
                }

                //look for satellite changes
                satelliteChanged = (oldMultiNoradId == null || oldMultiNoradId.size() != viewItems.length);
                if(!satelliteChanged)
                {
                    //go through each satellite while no change detected
                    for(index = 0; index < viewItems.length && !satelliteChanged; index++)
                    {
                        //check for changed ID
                        satelliteChanged = viewItems[index].getSatelliteNum() != oldMultiNoradId.get(index);
                    }
                }

                //if satellite(s), start, end, or increment changed
                if(satelliteChanged || (Calculations.julianDateCalendar(params.getLong(Calculate.ParamTypes.StartDateMs), observer) != julianDateStart || Calculations.julianDateCalendar(params.getLong(Calculate.ParamTypes.EndDateMs), observer) != julianDateEnd || Calculate.getDayIncrement(params) != dayIncrement))
                {
                    //ignore saved
                    savedViewItems = null;
                }
            }

            //start task
            calculateViewAnglesTask = Calculate.calculateViews(this, viewItems, savedViewItems, observer, julianDateStart, julianDateEnd, dayIncrement, new CalculateViewsTask.OnProgressChangedListener()
            {
                //items to use
                Calculate.ViewAngles.Item[] items = null;

                @Override
                public void onProgressChanged(int progressType, int satelliteIndex, CalculateService.ViewListItem item, final ArrayList<CalculateViewsTask.OrbitalView> pathPoints)
                {
                    int index;
                    Runnable viewAction = null;

                    //handle based on progress
                    switch(progressType)
                    {
                        case Globals.ProgressType.Started:
                            viewAction = new Runnable()
                            {
                                @Override
                                public synchronized void run()
                                {
                                    //update header and menu
                                    Calculate.PageAdapter.notifyHeaderChanged(Calculate.PageType.View, firstItem.getSatelliteNum(), Globals.getHeaderText(MainActivity.this, (viewItems.length == 1 ? firstItem.getName() : null), julianDateStart, julianDateEnd));
                                    updateOptionsMenu();
                                }
                            };
                            break;

                        case Globals.ProgressType.Success:
                            //remember current orbital
                            SatelliteObjectType currentOrbital = (satelliteIndex >= 0 && satelliteIndex < viewItems.length ? viewItems[satelliteIndex].satellite : null);

                            //if items not setup yet
                            if(items == null)
                            {
                                //setup items
                                items = new Calculate.ViewAngles.Item[pathPoints.size()];

                                //go through each item
                                for(index = 0; index < items.length; index++)
                                {
                                    //remember current item, view, and time
                                    Calculate.ViewAngles.Item currentItem = new Calculate.ViewAngles.Item(index, viewItems.length, currentOrbital);
                                    CalculateViewsTask.OrbitalView currentView = pathPoints.get(index);
                                    Calendar time = Globals.getLocalTime(currentView.gmtTime, observer.timeZone);

                                    //set shared values
                                    currentItem.id = firstItem.getSatelliteNum();
                                    currentItem.julianDate = currentView.julianDate;
                                    currentItem.time = time;
                                    items[index] = currentItem;
                                }
                            }

                            //go through each item/view
                            for(index = 0; index < items.length; index++)
                            {
                                //remember current item, view, and data
                                Calculate.ViewAngles.Item currentItem = items[index];
                                CalculateViewsTask.OrbitalView currentView = pathPoints.get(index);

                                //if orbital exists and index is within range
                                if(currentOrbital != null && satelliteIndex < currentItem.views.length)
                                {
                                    //set current data
                                    currentItem.views[satelliteIndex] = new CalculateViewsTask.ViewData(currentOrbital.getSatelliteNum(), currentView);
                                }
                            }
                            break;

                        case Globals.ProgressType.Finished:
                            //if params exist
                            if(params != null)
                            {
                                //update params with previously used ID(s)
                                params.putInt(Calculate.ParamTypes.NoradIdOld, params.getInt(Calculate.ParamTypes.NoradId, Universe.IDs.Invalid));
                                params.putIntegerArrayList(Calculate.ParamTypes.MultiNoradIdOld, params.getIntegerArrayList(Calculate.ParamTypes.MultiNoradId));
                                Calculate.PageAdapter.setParams(Calculate.PageType.View, params);
                            }
                            //fall through

                        case Globals.ProgressType.Cancelled:
                            viewAction = new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    //update list and menu
                                    Calculate.PageAdapter.setViewItems(items);
                                    Calculate.PageAdapter.notifyItemsChanged(Calculate.PageType.View);
                                    updateOptionsMenu();
                                }
                            };
                            break;
                    }

                    //if view action is set
                    if(viewAction != null)
                    {
                        //run action
                        MainActivity.this.runOnUiThread(viewAction);
                    }
                }
            });
        }
    }

    //Starts/stops given task path calculations and returns updated task
    private CalculateService.CalculatePathsTask setCalculatePathCalculations(boolean run, CalculateService.CalculatePathsTask task, byte calculateType, final Database.SatelliteData satellite1, final Database.SatelliteData satellite2, double julianDateStart, double julianDateEnd, double intersection, double minEl)
    {
        Bundle params;
        Calculate.Passes.Item currentItem = (satellite1 != null ? new Calculate.Passes.Item(MainActivity.this, 0, satellite1, satellite2, true) : null);
        final boolean forIntersections = (calculateType == CalculateService.CalculateType.OrbitalIntersections);
        final int pageType = (forIntersections ? Calculate.PageType.Intersection : Calculate.PageType.Passes);
        Calculate.Passes.Item[] savedPassItems;

        //if task was running
        if(task != null)
        {
            //cancel it
            task.cancel(true);
        }

        //if want to run and have item
        if(run && currentItem != null)
        {
            //get any saved items and params
            savedPassItems = (Calculate.Passes.Item[])Calculate.PageAdapter.getSavedItems(pageType);
            params = Calculate.PageAdapter.getParams(pageType);

            //check if params changed
            if(params != null && savedPassItems != null)
            {
                //if IDs, start date, end date, intersection, or minimum elevation changed
                if(params.getInt(Calculate.ParamTypes.NoradId) != currentItem.id || params.getInt(Calculate.ParamTypes.NoradId2, Universe.IDs.Invalid) != (forIntersections ? currentItem.satellite2.getSatelliteNum() : Universe.IDs.Invalid) || Calculations.julianDateCalendar(params.getLong(Calculate.ParamTypes.StartDateMs), observer) != julianDateStart || Calculations.julianDateCalendar(params.getLong(Calculate.ParamTypes.EndDateMs), observer) != julianDateEnd || params.getDouble(Calculate.ParamTypes.IntersectionDegs, 1.0) != intersection || params.getDouble(Calculate.ParamTypes.ElevationMinDegs) != minEl)
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
                                                Calculate.PageAdapter.setIntersectionItems(new Calculate.Passes.Item[0]);
                                                Calculate.PageAdapter.addIntersectionItem(new Calculate.Passes.Item(null, 0, null, true));
                                            }
                                            else
                                            {
                                                Calculate.PageAdapter.setPassItems(new Calculate.Passes.Item[0]);
                                                Calculate.PageAdapter.addPassItem(new Calculate.Passes.Item(null, 0, null, true));
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
                                        Calculate.Passes.Item lastItem;

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
                                    Calculate.PageAdapter.addIntersectionItem(new Calculate.Passes.Item(pass));
                                }
                                else
                                {
                                    Calculate.PageAdapter.addPassItem(new Calculate.Passes.Item(pass));
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
    private void setCalculateCoordinateCalculations(boolean run, final Database.SatelliteData[] satellites, final double julianDateStart, final double julianDateEnd, double dayIncrement)
    {
        boolean satelliteChanged;
        int index;
        int oldNoradId;
        Bundle params;
        ArrayList<Integer> oldMultiNoradId = new ArrayList<>(0);
        Database.SatelliteData firstSatellite;
        Calculate.Coordinates.Item[] savedCoordinateItems;

        //if task was running
        if(calculateCoordinatesTask != null)
        {
            //cancel it
            calculateCoordinatesTask.cancel(true);
        }

        //if want to run and have an item
        if(run && satellites != null && satellites.length > 0 && satellites[0] != null)
        {
            //get first satellite and any saved items and params
            firstSatellite = satellites[0];
            savedCoordinateItems = (Calculate.Coordinates.Item[])Calculate.PageAdapter.getSavedItems(Calculate.PageType.Coordinates);
            params = Calculate.PageAdapter.getParams(Calculate.PageType.Coordinates);

            //check if params changed
            if(params != null && savedCoordinateItems != null)
            {
                //get old satellite data
                oldNoradId = params.getInt(Calculate.ParamTypes.NoradIdOld, Universe.IDs.Invalid);
                if(oldNoradId != Universe.IDs.Invalid)
                {
                    oldMultiNoradId.add(oldNoradId);
                }
                else
                {
                    oldMultiNoradId = params.getIntegerArrayList(Calculate.ParamTypes.MultiNoradIdOld);
                }

                //look for satellite changes
                satelliteChanged = (oldMultiNoradId == null || oldMultiNoradId.size() != satellites.length);
                if(!satelliteChanged)
                {
                    //go through each satellite while no change detected
                    for(index = 0; index < satellites.length && !satelliteChanged; index++)
                    {
                        //check for changed ID
                        satelliteChanged = satellites[index].getSatelliteNum() != oldMultiNoradId.get(index);
                    }
                }

                //if satellite(s), start, end, or increment changed
                if(satelliteChanged || (Calculations.julianDateCalendar(params.getLong(Calculate.ParamTypes.StartDateMs), observer) != julianDateStart) || (Calculations.julianDateCalendar(params.getLong(Calculate.ParamTypes.EndDateMs), observer) != julianDateEnd) || (Calculate.getDayIncrement(params) != dayIncrement))
                {
                    //ignore saved
                    savedCoordinateItems = null;
                }
            }

            //start task
            calculateCoordinatesTask = Calculate.calculateCoordinates(this, satellites, savedCoordinateItems, observer, julianDateStart, julianDateEnd, dayIncrement, new CalculateCoordinatesTask.OnProgressChangedListener()
            {
                //items to use
                private Calculate.Coordinates.Item[] items = null;

                @Override
                public void onProgressChanged(int progressType, int satelliteIndex, final ArrayList<CalculateCoordinatesTask.OrbitalCoordinate> pathPoints)
                {
                    int index;
                    Runnable coordinateAction = null;

                    //handle based on progress
                    switch(progressType)
                    {
                        case Globals.ProgressType.Started:
                            coordinateAction = new Runnable()
                            {
                                @Override
                                public synchronized void run()
                                {
                                    //update header and menu
                                    Calculate.PageAdapter.notifyHeaderChanged(Calculate.PageType.Coordinates, firstSatellite.getSatelliteNum(), Globals.getHeaderText(MainActivity.this, (satellites.length == 1 ? firstSatellite.getName() : null), julianDateStart, julianDateEnd));
                                    updateOptionsMenu();
                                }
                            };
                            break;

                        case Globals.ProgressType.Success:
                            //if items not setup yet
                            if(items == null)
                            {
                                //setup items
                                items = new Calculate.Coordinates.Item[pathPoints.size()];

                                //go through each item
                                for(index = 0; index < items.length; index++)
                                {
                                    //remember current item, coordinate, and time
                                    Calculate.Coordinates.Item currentItem = new Calculate.Coordinates.Item(index, satellites.length);
                                    CalculateCoordinatesTask.OrbitalCoordinate currentCoordinate = pathPoints.get(index);
                                    Calendar time = Globals.getLocalTime(currentCoordinate.time, observer.timeZone);

                                    //set shared values
                                    currentItem.id = firstSatellite.getSatelliteNum();
                                    currentItem.julianDate = currentCoordinate.julianDate;
                                    currentItem.time = time;
                                    items[index] = currentItem;
                                }
                            }

                            //go through each item/view
                            for(index = 0; index < items.length; index++)
                            {
                                //remember current item, coordinate, and data
                                Calculate.Coordinates.Item currentItem = items[index];
                                CalculateCoordinatesTask.OrbitalCoordinate currentCoordinate = pathPoints.get(index);
                                CalculateCoordinatesTask.CoordinateData currentData = new CalculateCoordinatesTask.CoordinateData();

                                //if index is within range
                                if(satelliteIndex < currentItem.coordinates.length)
                                {
                                    //set current data
                                    currentData.noradId = satellites[satelliteIndex].getSatelliteNum();
                                    currentData.latitude = (float)currentCoordinate.latitude;
                                    currentData.longitude = (float)currentCoordinate.longitude;
                                    currentData.altitudeKm = (float)currentCoordinate.altitudeKm;
                                    currentData.speedKms = currentCoordinate.speedKmS;
                                    currentData.illumination = currentCoordinate.illumination;
                                    currentData.julianDate = currentCoordinate.julianDate;
                                    currentData.phaseName = currentCoordinate.phaseName;
                                    currentItem.coordinates[satelliteIndex] = currentData;
                                }
                            }
                            break;

                        case Globals.ProgressType.Finished:
                            //if params exist
                            if(params != null)
                            {
                                //update params with previously used ID(s)
                                params.putInt(Calculate.ParamTypes.NoradIdOld, params.getInt(Calculate.ParamTypes.NoradId, Universe.IDs.Invalid));
                                params.putIntegerArrayList(Calculate.ParamTypes.MultiNoradIdOld, params.getIntegerArrayList(Calculate.ParamTypes.MultiNoradId));
                                Calculate.PageAdapter.setParams(Calculate.PageType.Coordinates, params);
                            }
                            //fall through

                        case Globals.ProgressType.Cancelled:
                            coordinateAction = new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    //update list and menu
                                    Calculate.PageAdapter.setCoordinateItems(items);
                                    Calculate.PageAdapter.notifyItemsChanged(Calculate.PageType.Coordinates);
                                    updateOptionsMenu();
                                }
                            };
                            break;
                    }

                    //if coordinate action is set
                    if(coordinateAction != null)
                    {
                        //run action
                        MainActivity.this.runOnUiThread(coordinateAction);
                    }
                }
            });
        }
    }
}
