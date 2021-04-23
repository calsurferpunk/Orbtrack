package com.nikolaiapps.orbtrack;


import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager.widget.PagerAdapter;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.Task;
import java.util.ArrayList;


public class SetupActivity extends BaseInputActivity
{
    private static abstract class SetupPage
    {
        static final int Welcome = 0;
        static final int Display = 1;
        static final int Location = 2;
        static final int Updates = 3;
        static final int Satellites = 4;
        static final int Finished = 5;
        static final int PageCount = 6;
    }

    public static abstract class ParamTypes
    {
        static final String ChangedTheme = "changedTheme";
    }

    public static class Page extends Selectable.ListFragment
    {
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
        {
            int subPage = Settings.SubPageType.None;
            int page = this.getPageParam();
            int settingsPage = Settings.PageType.Other;
            boolean onWelcome = (page == SetupPage.Welcome);
            View inputGroup = null;
            LinearLayout.LayoutParams layoutParams;
            Selectable.ListBaseAdapter listAdapter = null;
            ViewGroup rootView = (ViewGroup)inflater.inflate(R.layout.setup_view, container, false);
            TextView text = rootView.findViewById(R.id.Setup_Text);
            TextView textTitle = rootView.findViewById(R.id.Setup_Text_Title);
            LinearLayout textLayout = rootView.findViewById(R.id.Setup_Text_Layout);

            //handle display based on page
            switch(page)
            {
                case SetupPage.Welcome:
                case SetupPage.Finished:
                    textLayout.setVisibility(View.VISIBLE);
                    textTitle.setText(onWelcome ? R.string.title_welcome : R.string.title_finished);
                    text.setText(onWelcome ? R.string.desc_quick_setup : R.string.desc_finished_setup);
                    break;

                case SetupPage.Display:
                    subPage = Settings.SubPageType.Display;
                    break;

                case SetupPage.Location:
                    listAdapter = new Settings.Locations.ItemListAdapter(rootView, R.string.title_location, null);
                    break;

                case SetupPage.Updates:
                    settingsPage = Settings.PageType.Updates;
                    break;

                case SetupPage.Satellites:
                    listAdapter = new Orbitals.PageListAdapter(rootView, Orbitals.PageType.Satellites, R.string.title_satellites, true);
                    PageAdapter.setInformationChangedListener(createOnInformationChangedListener(listAdapter));
                    break;
            }

            //if Settings.PageType.Updates or using sub page
            if(settingsPage == Settings.PageType.Updates || subPage != Settings.SubPageType.None)
            {
                //get input group
                inputGroup = Settings.Options.onCreateView(Page.this, settingsPage, subPage, inflater, container, true);
            }
            //else if using list adapter
            else if(listAdapter != null)
            {
                //get input group
                inputGroup = this.onCreateView(inflater, container, listAdapter, group, page);
            }

            //if input group is set
            if(inputGroup != null)
            {
                //replace placeholder
                Globals.replaceView(R.id.Setup_Input_View, inputGroup, rootView);

                //set height
                layoutParams = (LinearLayout.LayoutParams)inputGroup.getLayoutParams();
                layoutParams.height = 0;
                layoutParams.weight = 1;
                inputGroup.setLayoutParams(layoutParams);
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

    private static class PageAdapter extends Selectable.ListFragmentAdapter
    {
        private static Selectable.ListFragment.OnInformationChangedListener informationChangedListener;

        public PageAdapter(FragmentManager fm, View parentView,  Selectable.ListFragment.OnItemCheckChangedListener checkChangedListener, Selectable.ListFragment.OnItemDetailButtonClickListener detailButtonClickListener)
        {
            super(fm, parentView, null, null, null, checkChangedListener, detailButtonClickListener, null, null, -1, null);
        }

        @Override
        public @NonNull Fragment getItem(final int position)
        {
            return(this.getItem(-1, position, -1, new Page()));
        }

        @Override
        public int getCount()
        {
            return(SetupPage.PageCount);
        }

        //Sets on information changed listener
        public static void setInformationChangedListener(Selectable.ListFragment.OnInformationChangedListener listener)
        {
            informationChangedListener = listener;
        }

        //Calls information changed listener
        public static void notifyInformationChanged(Spanned text)
        {
            //if listener exists
            if(informationChangedListener != null)
            {
                //call listener
                informationChangedListener.informationChanged(text);
            }
        }
    }

    private byte locationSource;
    private static int startColorTheme;
    private static boolean startDarkTheme;
    private boolean updateNow;
    private boolean isLoading;
    private boolean needSaveCurrentLocation;
    private LinearLayout setupLayout;
    private SwipeStateViewPager setupPager;
    private AppCompatButton backButton;
    private AppCompatButton nextButton;
    private FloatingActionButton floatingButton;
    private TextView infoText;
    private ProgressBar loadingBar;
    private CheckBox inputCheckBox;
    private AlertDialog addCurrentLocationDialog;
    private LocationReceiver locationReceiver;
    private UpdateReceiver localUpdateReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.setup_layout);

        //looks for web protocol updates
        Globals.updateWebProtocols(this);

        //get layouts
        final LinearLayout progressLayout = this.findViewById(R.id.Setup_Progress_Layout);
        setupLayout = this.findViewById(R.id.Setup_Layout);

        //set defaults
        updateNow = false;
        isLoading = needSaveCurrentLocation = false;
        if(savedInstanceState == null)
        {
            startDarkTheme = Settings.getDarkTheme(this);
            startColorTheme = Settings.getColorTheme(this);
        }

        //setup buttons
        backButton = this.findViewById(R.id.Setup_Back_Button);
        backButton.setOnClickListener(createOnButtonClickListener(false));
        nextButton = this.findViewById(R.id.Setup_Next_Button);
        nextButton.setOnClickListener(createOnButtonClickListener(true));
        floatingButton = this.findViewById(R.id.Setup_Floating_Button);
        floatingButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                int setupPage = setupPager.getCurrentItem();

                switch(setupPage)
                {
                    case SetupPage.Location:
                        showAddLocationDialog();
                        break;

                    case SetupPage.Satellites:
                        Orbitals.showAddDialog(SetupActivity.this, setupLayout, false);
                        break;
                }
            }
        });
        floatingButton.setOnLongClickListener(new View.OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View v)
            {
                int setupPage = setupPager.getCurrentItem();

                switch(setupPage)
                {
                    case SetupPage.Satellites:
                        Orbitals.showAddDialog(SetupActivity.this, setupLayout, true);
                        return(true);
                }

                return(false);
            }
        });

        //set info text
        infoText = this.findViewById(R.id.Setup_Info_Text);

        //set loading bar
        loadingBar = this.findViewById(R.id.Setup_Loading_Bar);

        //setup checkbox
        inputCheckBox = this.findViewById(R.id.Setup_Input_CheckBox);
        inputCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                int page = setupPager.getCurrentItem();

                //if on SetupPage.Satellites
                if(page == SetupPage.Satellites)
                {
                    //update if updating
                    updateNow = isChecked;

                    //allow swiping if not updating
                    setupPager.setSwipeEnabled(!updateNow);
                }
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

        //setup pager
        setupPager = this.findViewById(R.id.Setup_Pager);
        setupPager.setAdapter(new PageAdapter(this.getSupportFragmentManager(), setupLayout, new Selectable.ListFragment.OnItemCheckChangedListener()
        {
            @Override
            public void itemCheckedChanged(int page, Selectable.ListItem item)
            {
                //if on location
                if(page == SetupPage.Location)
                {
                    //remember current item and if current location
                    Settings.Locations.Item currentItem = (Settings.Locations.Item)item;

                    //if now checked
                    if(item.isChecked)
                    {
                        Database.saveLocation(SetupActivity.this, currentItem.name, currentItem.locationType, true);
                    }
                }
            }
        }, new Selectable.ListFragment.OnItemDetailButtonClickListener()
        {
            @Override
            public void onClick(int group, int pageType, int itemID, Selectable.ListItem item, int buttonNum)
            {
                switch(buttonNum)
                {
                    case Selectable.ListBaseAdapter.DetailButtonType.Info:
                        //get information
                        UpdateService.getInformation(SetupActivity.this, group, new Database.SatelliteData(Database.getOrbital(SetupActivity.this, itemID)).satellite.tle);
                        break;
                }
            }
        }));
        setupPager.addOnPageChangeListener(new SwipeStateViewPager.OnPageChangeListener()
        {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            @Override
            public void onPageSelected(int position)
            {
                //update status
                Globals.setVisible(floatingButton, (position == SetupPage.Location || (position == SetupPage.Satellites && !UpdateService.modifyingSatellites())));
                updateProgress(progressLayout);
            }

            @Override
            public void onPageScrollStateChanged(int state) {}
        });

        //set receivers
        locationSource = Database.LocationType.Current;
        locationReceiver = createLocationReceiver();
        locationReceiver.register(this);
        localUpdateReceiver = createUpdateReceiver();
        localUpdateReceiver.register(this);

        //set page
        setupPager.setCurrentItem(SetupPage.Welcome);
        updateProgress(progressLayout);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);

        manager.unregisterReceiver(localUpdateReceiver);
        locationReceiver.unregister(this);
    }

    @Override
    public void onBackPressed()
    {
        int page = (setupPager != null ? setupPager.getCurrentItem() : -1);

        //if not loading
        if(!isLoading)
        {
            //if after first page
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        int progressType = Globals.ProgressType.Finished;
        long count;
        boolean isError = true;
        boolean isOkay = (resultCode == RESULT_OK);
        String message;
        Resources res = this.getResources();
        ArrayList<String> fileNames;

        //if no data
        if(data == null)
        {
            //set to empty
            data = new Intent();
        }

        //handle based on request code
        switch(requestCode)
        {
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
                Globals.showSnackBar(setupLayout, message, isError);
                //fall through

            case BaseInputActivity.RequestCode.ManualOrbitalInput:
                //if set and weren't denied
                if(isOkay && progressType != Globals.ProgressType.Denied)
                {
                    //update list
                    updateList();
                }
                break;

            case BaseInputActivity.RequestCode.MapLocationInput:
                //if set
                if(isOkay)
                {
                    //update display
                    updateList();
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
                    Globals.showSnackBar(setupLayout, res.getString(R.string.text_login_failed), true);
                }
                break;

            case BaseInputActivity.RequestCode.GoogleDriveOpenFile:
            case BaseInputActivity.RequestCode.DropboxOpenFile:
            case BaseInputActivity.RequestCode.OthersOpenItem:
                //if selected item
                if(isOkay)
                {
                    //handle open file request
                    handleActivityOpenFileRequest(this, setupLayout, data, requestCode);
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
                    Orbitals.showSDCardFileBrowser(this, setupLayout);
                }
                //else if not retrying
                else if(!retrying)
                {
                    //ask permission again
                    Globals.askReadPermission(this, true);
                }
                break;
        }
    }

    //Dismisses add current location dialog
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
    private void showAddLocationDialog()
    {
        Resources res = this.getResources();

        Globals.showSelectDialog(SetupActivity.this, res.getString(R.string.title_location_add), AddSelectListAdapter.SelectType.Location, new AddSelectListAdapter.OnItemClickListener()
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
                        LocationService.getCurrentLocation(SetupActivity.this, false, true, LocationService.PowerTypes.HighPowerThenBalanced);
                        break;

                    case AddSelectListAdapter.LocationSourceType.Custom:
                    case AddSelectListAdapter.LocationSourceType.Search:
                        MapLocationInputActivity.show(SetupActivity.this, which == AddSelectListAdapter.LocationSourceType.Search);
                        break;
                }
            }
        });
    }

    //Updates progress
    private void updateProgress(LinearLayout progressLayout)
    {
        int currentPage;
        int page = setupPager.getCurrentItem();
        boolean onUpdates = (page == SetupPage.Updates);
        boolean onSatellites = (page == SetupPage.Satellites);
        Resources res = this.getResources();

        //update visibility
        progressLayout.setVisibility(page == SetupPage.Welcome || page == SetupPage.Finished ? View.INVISIBLE : View.VISIBLE);

        //go through each page
        for(currentPage = 1; currentPage < SetupPage.PageCount - 1;  currentPage++)
        {
            //get progress box
            View progressBox = progressLayout.findViewWithTag(String.valueOf(currentPage - 1));
            if(progressBox != null)
            {
                //set progress box color
                progressBox.setBackgroundResource(currentPage <= page ? Globals.resolveAttributeID(SetupActivity.this, currentPage < page ? R.attr.actionBarBackground : R.attr.colorAccent) : R.color.light_gray);
            }
        }

        //allow swiping if not SetupPage.Satellites or not updating
        setupPager.setSwipeEnabled(page != SetupPage.Satellites || !updateNow);

        //update info text
        infoText.setText(onUpdates ? res.getString(R.string.text_spacetrack_create_account) : "");
        infoText.setVisibility(onUpdates ? View.VISIBLE : View.GONE);

        //update checkbox
        inputCheckBox.setChecked(onSatellites && updateNow);
        inputCheckBox.setText(onSatellites ? res.getString(R.string.desc_update_now) : "");
        inputCheckBox.setVisibility(onSatellites ? View.VISIBLE : View.GONE);

        //update buttons
        backButton.setText(page == SetupPage.Welcome ? R.string.title_skip : R.string.title_back);
        nextButton.setText(page == SetupPage.Finished ? R.string.title_finish : R.string.title_next);
    }

    //Update list
    private void updateList()
    {
        PagerAdapter currentAdapter = setupPager.getAdapter();

        //if adapter is set
        if(currentAdapter != null)
        {
            //update list
            currentAdapter.notifyDataSetChanged();
        }
    }

    //Update loading status
    private void setLoading(boolean loading)
    {
        int page = setupPager.getCurrentItem();
        boolean onSatellites = (page == SetupPage.Satellites);

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
                UpdateService.setNotificationVisible(UpdateService.UpdateType.UpdateSatellites, false);
            }
        }
    }

    //Creates an on button click listener
    private View.OnClickListener createOnButtonClickListener(final boolean next)
    {
        return(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                int page = setupPager.getCurrentItem();
                Intent data = new Intent();

                //add if changed theme (dark or color)
                data.putExtra(ParamTypes.ChangedTheme, (startDarkTheme != Settings.getDarkTheme(SetupActivity.this) || startColorTheme != Settings.getColorTheme(SetupActivity.this)));

                //handle based on page
                switch(page)
                {
                    case SetupPage.Welcome:
                        //if next
                        if(next)
                        {
                            //go to next page
                            setupPager.setCurrentItem(page + 1);
                        }
                        else
                        {
                            //cancelled
                            setResult(RESULT_CANCELED, data);
                            SetupActivity.this.finish();
                        }
                        break;

                    case SetupPage.Finished:
                        //if next
                        if(next)
                        {
                            //finished
                            setResult(RESULT_OK, data);
                            SetupActivity.this.finish();
                        }
                        //else fall through

                    default:
                        //if on SetupPage.Satellites, next, and need to update now
                        if(page == SetupPage.Satellites && next && updateNow)
                        {
                            updateSatellites(true);
                            break;
                        }
                        //else fall through

                        //go back or next
                        setupPager.setCurrentItem(page + (next ? 1 : -1));
                        break;
                }
            }
        });
    }

    //Creates a location receiver
    private LocationReceiver createLocationReceiver()
    {
        return(new LocationReceiver(LocationService.FLAG_START_GET_LOCATION)
        {
            @Override
            protected Activity getActivity()
            {
                return(SetupActivity.this);
            }

            @Override
            protected View getParentView()
            {
                return(setupLayout);
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
        //create receiver
        return(new UpdateReceiver()
        {
            @Override
            protected View getParentView()
            {
                return(setupLayout);
            }

            @Override
            protected void onLoadPending(ArrayList<Database.DatabaseSatellite> pendingLoadSatellites)
            {
                //show dialog to load pending
                Orbitals.showLoadDialog(SetupActivity.this, setupPager, (Selectable.ListFragmentAdapter) setupPager.getAdapter(), pendingLoadSatellites, new EditValuesDialog.OnDismissListener()
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
                //update display
                PageAdapter.notifyInformationChanged(infoText);
            }

            @Override
            protected void onGeneralUpdate(int progressType, byte updateType, boolean ended)
            {
                //if button and pager exist and on satellites
                if(floatingButton != null && setupPager != null && setupPager.getCurrentItem() == SetupPage.Satellites)
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
                                Globals.showAccountLogin(SetupActivity.this, Globals.AccountType.SpaceTrack, updateType, new Globals.WebPageListener()
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
}
