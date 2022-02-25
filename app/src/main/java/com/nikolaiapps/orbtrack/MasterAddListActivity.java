package com.nikolaiapps.orbtrack;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;


public class MasterAddListActivity extends BaseInputActivity
{
    public static abstract class ListType
    {
        static final byte MasterList = 0;
        static final byte VisibleList = 1;
    }

    public static abstract class ParamTypes
    {
        static final String ListType = "listType";
        static final String SuccessCount = "successCount";
        static final String TotalCount = "totalCount";
        static final String ProgressType = "progressType";
        static final String AskUpdate = "askUpdate";
    }

    private static class MasterListAdapter extends OrbitalFilterList.OrbitalListAdapter
    {
        //On item check changed listener
        public interface OnItemCheckChangedListener
        {
            void onCheckChanged(OrbitalFilterList.Item item);
        }

        //Load items task
        private static class LoadItemsTask extends ThreadTask<Object, Void, Void>
        {
            private final Globals.OnProgressChangedListener progressChangedListener;
            private final OrbitalFilterList.OnLoadItemsListener loadItemsListener;

            public LoadItemsTask(Globals.OnProgressChangedListener progressChangedListener, OrbitalFilterList.OnLoadItemsListener loadItemsListener)
            {
                this.progressChangedListener = progressChangedListener;
                this.loadItemsListener = loadItemsListener;
            }

            @Override
            protected Void doInBackground(Object... params)
            {
                int index;
                int index2;
                int itemIndex;
                int satelliteCount;
                boolean foundLaunchDate = false;
                Resources res;
                Context context = (Context)params[0];
                UpdateService.MasterListType masterList = (UpdateService.MasterListType)params[1];
                ArrayList<OrbitalFilterList.Item> items = new ArrayList<>(0);
                Database.DatabaseSatellite[] dbSatellites;

                //if master list is set
                if(masterList != null)
                {
                    //go through each satellite
                    satelliteCount = masterList.satellites.size();
                    for(index = 0; index < satelliteCount; index++)
                    {
                        //remember current satellite, group, and item
                        UpdateService.MasterSatellite currentSatellite = masterList.satellites.get(index);
                        String currentOwner = currentSatellite.ownerCode;
                        ListItem newItem = new ListItem(currentOwner, currentSatellite, false);
                        int[] indexes = Globals.divideFind(newItem, items, new ListItem.Comparer(false));

                        //if item is not in the list yet
                        itemIndex = indexes[0];
                        if(itemIndex < 0)
                        {
                            //add to items
                            items.add(indexes[1], newItem);
                        }
                        else
                        {
                            //add to existing item categories
                            items.get(itemIndex).addOwner(currentOwner);
                        }

                        //if there is a launch date
                        if(currentSatellite.launchDateMs > 0)
                        {
                            //found
                            foundLaunchDate = true;
                        }

                        //if listener is set
                        if(progressChangedListener != null)
                        {
                            //send event
                            progressChangedListener.onProgressChanged(Globals.ProgressType.Running, null, index, satelliteCount);
                        }
                    }
                }
                else
                {
                    //get resources and satellites
                    res = context.getResources();
                    dbSatellites = Database.getOrbitals(context);

                    //go through each satellite
                    satelliteCount = dbSatellites.length;
                    for(index = 0; index < satelliteCount; index++)
                    {
                        ListItem newItem;
                        UpdateService.MasterSatellite newSatellite;
                        Database.DatabaseSatellite currentSatellite = dbSatellites[index];
                        String currentOwner = currentSatellite.ownerCode;
                        String[][] groups = Database.getSatelliteCategoriesEnglish(context, currentSatellite.noradId);

                        //create satellite with categories
                        newSatellite = new UpdateService.MasterSatellite(currentSatellite.noradId, currentSatellite.getName(), currentOwner, currentSatellite.ownerName, currentSatellite.launchDateMs);
                        for(index2 = 0; index2 < groups.length; index2++)
                        {
                            //add category
                            newSatellite.categories.add(Database.LocaleCategory.getCategory(res, groups[index2][1]));
                        }

                        //create item
                        newItem = new ListItem(currentOwner, newSatellite, currentSatellite.isSelected);

                        //if item is not in the list yet
                        itemIndex = items.indexOf(newItem);
                        if(itemIndex < 0)
                        {
                            //add to items
                            items.add(newItem);
                        }
                        else
                        {
                            //add to existing item owners
                            items.get(itemIndex).addOwner(currentOwner);
                        }

                        //if there is a launch date
                        if(currentSatellite.launchDateMs > 0)
                        {
                            //found
                            foundLaunchDate = true;
                        }

                        //if listener is set
                        if(progressChangedListener != null)
                        {
                            //send event
                            progressChangedListener.onProgressChanged(Globals.ProgressType.Running, null, index, satelliteCount);
                        }
                    }
                }
                Collections.sort(items, new ListItem.Comparer(true));

                //if listeners are set
                if(loadItemsListener != null)
                {
                    //send event
                    loadItemsListener.onLoaded(items, foundLaunchDate);
                }
                if(progressChangedListener != null)
                {
                    //send event
                    progressChangedListener.onProgressChanged(Globals.ProgressType.Finished, null, satelliteCount, satelliteCount);
                }

                //done
                return(null);
            }
        }

        //List item
        public static class ListItem extends OrbitalFilterList.Item
        {
            public final boolean startChecked;

            public ListItem(String ownerCode, UpdateService.MasterSatellite sat, boolean startCheck)
            {
                super(sat, ownerCode, startCheck);

                startChecked = isChecked;
            }
        }

        //List item holder
        public static class ListItemHolder extends Selectable.ListItemHolder
        {
            final AppCompatImageView orbitalImage;
            final AppCompatImageView ownerImage;
            final TextView itemText;
            final CircularProgressIndicator itemProgress;

            public ListItemHolder(View viewItem, int orbitalImageID, int itemImageID, int itemTextID, int itemProgressID)
            {
                super(viewItem, R.id.Item_Checked_Check);
                orbitalImage = viewItem.findViewById(orbitalImageID);
                ownerImage = viewItem.findViewById(itemImageID);
                itemText = viewItem.findViewById(itemTextID);
                itemProgress = viewItem.findViewById(itemProgressID);
            }
        }

        private final LoadItemsTask loadItems;
        private final UpdateService.MasterListType masterList;
        private final OnItemCheckChangedListener itemCheckChangedListener;

        public MasterListAdapter(final Context context, UpdateService.MasterListType masterList, Globals.OnProgressChangedListener progressChangedListener, OrbitalFilterList.OnLoadItemsListener loadItemsListener, OnItemCheckChangedListener checkChangedListener)
        {
            super(context);

            //set defaults
            loadingItems = true;
            this.itemsRefID = R.layout.image_checked_item;

            //set check changed listener
            this.masterList = masterList;
            this.itemCheckChangedListener = checkChangedListener;

            //set load items task
            loadItems = new LoadItemsTask(progressChangedListener, new OrbitalFilterList.OnLoadItemsListener()
            {
                @Override
                public void onLoaded(ArrayList<OrbitalFilterList.Item> items, boolean foundLaunchDate)
                {
                    displayedItems = items;
                    allItems = items.toArray(new OrbitalFilterList.Item[0]);
                    loadingItems = false;
                    MasterListAdapter.this.foundLaunchDate = foundLaunchDate;
                    if(context instanceof Activity)
                    {
                        ((Activity)context).runOnUiThread(new Runnable()
                        {
                            @Override @SuppressLint("NotifyDataSetChanged")
                            public void run()
                            {
                                MasterListAdapter.this.notifyDataSetChanged();
                            }
                        });
                    }
                    if(loadItemsListener != null)
                    {
                        loadItemsListener.onLoaded(items, foundLaunchDate);
                    }
                }
            });
        }
        public MasterListAdapter(Context context, OrbitalFilterList.OnLoadItemsListener loadItemsListener, OnItemCheckChangedListener checkChangedListener)
        {
            this(context, null, null, loadItemsListener, checkChangedListener);
        }

        @Override
        public void onAttachedToRecyclerView(@NonNull RecyclerView view)
        {
            super.onAttachedToRecyclerView(view);
            loadItems.execute(currentContext, masterList);
        }

        @Override @NonNull
        public ListItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
        {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(this.itemsRefID, parent, false);
            final ListItemHolder itemHolder = new ListItemHolder(itemView, R.id.Item_Checked_Image2, R.id.Item_Checked_Image1, R.id.Item_Checked_Text, R.id.Item_Checked_Progress);

            itemView.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    itemHolder.checkBoxView.setChecked(!itemHolder.checkBoxView.isChecked());
                }
            });

            return(itemHolder);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position)
        {
            final OrbitalFilterList.Item currentItem;
            final ListItemHolder itemHolder = (ListItemHolder)holder;
            int showText = (loadingItems ? View.GONE : View.VISIBLE);
            int showProgress = (loadingItems ? View.VISIBLE : View.GONE);

            //update visibility
            itemHolder.itemText.setVisibility(showText);
            itemHolder.itemProgress.setVisibility(showProgress);
            itemHolder.checkBoxView.setVisibility(showText);

            //if not loading items
            if(!loadingItems)
            {
                //get current item
                currentItem = displayedItems.get(position);

                //set displays and update checked state
                itemHolder.orbitalImage.setBackgroundDrawable(Globals.getOrbitalIcon(currentContext, MainActivity.getObserver(), currentItem.satellite.noradId, currentItem.satellite.orbitalType));
                itemHolder.orbitalImage.setVisibility(View.VISIBLE);
                itemHolder.ownerImage.setBackgroundDrawable(Globals.getDrawable(currentContext, Globals.getOwnerIconIDs(currentItem.satellite.ownerCode)));
                itemHolder.itemText.setText(currentItem.satellite.name);
                itemHolder.checkBoxView.setOnCheckedChangeListener(null);
                itemHolder.checkBoxView.setChecked(currentItem.isChecked);
                itemHolder.checkBoxView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
                {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean checked)
                    {
                        //update checked state
                        currentItem.isChecked = checked;

                        //if listener is set
                        if(itemCheckChangedListener != null)
                        {
                            //call it
                            itemCheckChangedListener.onCheckChanged(currentItem);
                        }
                    }
                });
            }
        }

        //Gets all items
        public OrbitalFilterList.Item[] getAllItems()
        {
            return(loadingItems ? new ListItem[0] : allItems);
        }

        //Gets selected items
        public OrbitalFilterList.Item[] getSelectedItems()
        {
            int index;
            ArrayList<OrbitalFilterList.Item> selectedItems = new ArrayList<>(0);

            //if not loading items
            if(!loadingItems)
            {
                //go through all items
                for(index = 0; index < allItems.length; index++)
                {
                    //if item is selected
                    if(allItems[index].isChecked)
                    {
                        //add it to the list
                        selectedItems.add(allItems[index]);
                    }
                }
            }

            //return selected items as an array
            return(selectedItems.toArray(new OrbitalFilterList.Item[0]));
        }

        //Gets displayed items
        public OrbitalFilterList.Item[] getDisplayedItems()
        {
            return(displayedItems != null ? displayedItems.toArray(new OrbitalFilterList.Item[0]) : new ListItem[0]);
        }
    }

    //Status
    private static int lastNeedUpdateDay = -1;
    private static int lastNeedUpdateYear = -1;

    //Data
    private byte requestCode;
    private BroadcastReceiver updateReceiver;
    private BroadcastReceiver listReceiver;

    //Displays
    private int itemSelectedCount;
    private String startTitle;
    private LinearLayout masterLayout;
    private RecyclerView addList;
    private MasterListAdapter addAdapter;
    private TableLayout searchGroup;
    private IconSpinner ownerList;
    private IconSpinner groupList;
    private IconSpinner ageList;
    private EditText searchText;
    private TableRow ageRow;
    private AppCompatImageButton showButton;
    private MaterialButton addButton;
    private MultiProgressDialog downloadProgress;
    private MultiProgressDialog addProgress;

    //Listeners
    private MasterListAdapter.OnItemCheckChangedListener checkChangedListener;

    //Creates local broadcast receiver
    private BroadcastReceiver createLocalBroadcastReceiver(BroadcastReceiver oldReceiver, final MultiProgressDialog taskProgress, final OrbitalFilterList.Item[] selectedItems)
    {
        final Resources res = this.getResources();
        final String overallString = res.getString(R.string.title_overall);
        final String savingString = res.getString(R.string.title_saving);
        final String loadingString = res.getString(R.string.title_loading);
        final String gettingString = res.getString(R.string.title_getting);
        final String spaceOfSpaceString = res.getString(R.string.text_space_of_space);

        //if old receiver is set
        if(oldReceiver != null)
        {
            //remove it
            LocalBroadcastManager.getInstance(this).unregisterReceiver(oldReceiver);
        }

        //create receiver
        BroadcastReceiver updateReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(final Context context, Intent intent)
            {
                byte messageType = intent.getByteExtra(UpdateService.ParamTypes.MessageType, Byte.MAX_VALUE);
                byte updateType = intent.getByteExtra(UpdateService.ParamTypes.UpdateType, Byte.MAX_VALUE);
                boolean allowDismiss = true;
                final int progressType = intent.getIntExtra(UpdateService.ParamTypes.ProgressType, Byte.MAX_VALUE);
                final long updateIndex = intent.getLongExtra(UpdateService.ParamTypes.Index, 0);
                final long updateCount = intent.getLongExtra(UpdateService.ParamTypes.Count, 0);
                final int overall = (int)intent.getLongExtra(UpdateService.ParamTypes.SubIndex, -1);
                long updateValue = -1;
                String section = intent.getStringExtra(UpdateService.ParamTypes.Section);
                String message = "";
                boolean isDownload = (messageType == NotifyService.MessageTypes.Download);
                UpdateService.MasterListType masterList = null;
                final Intent data = new Intent();

                switch(updateType)
                {
                    case UpdateService.UpdateType.UpdateSatellites:
                        switch(progressType)
                        {
                            case Globals.ProgressType.Started:
                                //if add button exists
                                if(addButton != null)
                                {
                                    //disable it
                                    addButton.setEnabled(false);
                                }

                                //if progress exists, there are selected items, index is valid, and updating at least 1
                                if(taskProgress != null && selectedItems != null && updateIndex < selectedItems.length && updateCount > 0)
                                {
                                    //update progress
                                    updateValue = updateIndex + 1;
                                    taskProgress.setMessage(updateValue + spaceOfSpaceString + updateCount + " (" + selectedItems[(int)updateIndex].satellite.name + ")");
                                    taskProgress.setProgress(updateValue, updateCount);
                                }
                                break;

                            case Globals.ProgressType.Finished:
                            case Globals.ProgressType.Cancelled:
                            case Globals.ProgressType.Denied:
                                //if add button exists
                                if(addButton != null)
                                {
                                    //enable it
                                    addButton.setEnabled(true);
                                }

                                //if progress exists
                                if(taskProgress != null)
                                {
                                    //close it
                                    taskProgress.dismiss();
                                }

                                //if denied
                                if(progressType == Globals.ProgressType.Denied)
                                {
                                    //show login
                                    Globals.showAccountLogin(MasterAddListActivity.this, Globals.AccountType.SpaceTrack, updateType, new Globals.WebPageListener()
                                    {
                                        @Override
                                        public void onResult(Globals.WebPageData pageData, boolean success)
                                        {
                                            //if success or attempted to login
                                            if(success || pageData != null)
                                            {
                                                //try again
                                                MasterAddListActivity.this.runOnUiThread(new Runnable()
                                                {
                                                    @Override
                                                    public void run()
                                                    {
                                                        updateSatellites(selectedItems);
                                                    }
                                                });
                                            }
                                            //else cancelled
                                            else
                                            {
                                                //finished
                                                MasterAddListActivity.this.finish();
                                            }
                                        }
                                    });
                                }
                                else
                                {
                                    //send result
                                    sendResult(data, updateIndex, updateCount, progressType);
                                }
                                break;
                        }
                        break;

                    case UpdateService.UpdateType.GetMasterList:
                        switch(progressType)
                        {
                            case Globals.ProgressType.Started:
                                //if section exists
                                if(section != null)
                                {
                                    //update progress
                                    taskProgress.setMessage((messageType == UpdateService.MessageTypes.Save ? (savingString + " ") : messageType == UpdateService.MessageTypes.Load ? (loadingString + " ") :  !isDownload ? (gettingString + " ") : "") + section);
                                }
                                break;

                            case Globals.ProgressType.Success:
                                //if updating at least 1
                                if(updateCount > 0)
                                {
                                    //update message based on message type
                                    switch(messageType)
                                    {
                                        case UpdateService.MessageTypes.Save:
                                        case UpdateService.MessageTypes.Load:
                                            updateValue = updateIndex + 1;
                                            message = (updateValue + spaceOfSpaceString + updateCount);
                                            break;

                                        case NotifyService.MessageTypes.LoadPercent:
                                            updateValue = updateIndex + 1;
                                            break;

                                        default:
                                            updateValue = updateIndex;
                                            message = Globals.getByteString(res, updateValue);
                                            break;
                                    }
                                }
                                else if(isDownload)
                                {
                                    //update download progress
                                    message = Globals.getByteString(res, updateIndex);
                                }

                                //if message is not blank
                                if(message.length() > 0)
                                {
                                    //if section exists and is not blank
                                    if(section != null && section.length() > 0)
                                    {
                                        //add section to message
                                        message = section + " (" + message + ")";
                                    }

                                    //update message
                                    taskProgress.setMessage(message);
                                }

                                //update progress
                                if(updateValue >= 0)
                                {
                                    taskProgress.setProgress(updateValue, updateCount);
                                }
                                if(overall >= 0)
                                {
                                    taskProgress.setMessage2(overallString);
                                    taskProgress.setProgress2(overall);
                                }
                                break;

                            case Globals.ProgressType.Finished:
                            case Globals.ProgressType.Failed:
                            case Globals.ProgressType.Denied:
                                if(messageType == UpdateService.MessageTypes.General)
                                {
                                    //get master list
                                    masterList = UpdateService.getMasterList();
                                    final boolean ranUpdate = masterList.justUpdated;
                                    final String title = res.getQuantityString(R.plurals.title_satellites, 2);
                                    final ArrayList<String> usedCategories = masterList.usedCategories;
                                    final ArrayList<UpdateService.MasterOwner> usedOwners = masterList.usedOwners;

                                    //add satellites
                                    addAdapter = new MasterListAdapter(MasterAddListActivity.this, masterList, new Globals.OnProgressChangedListener()
                                    {
                                        @Override
                                        public void onProgressChanged(int progressType, String section, final long updateIndex, final long updateCount)
                                        {
                                            switch (progressType)
                                            {
                                                case Globals.ProgressType.Running:
                                                    //if on first, last, or enough have passed
                                                    if(updateIndex == 0 || (updateIndex + 1) == updateCount || updateIndex % 100 == 0)
                                                    {
                                                        //update progress
                                                        MasterAddListActivity.this.runOnUiThread(new Runnable()
                                                        {
                                                            @Override
                                                            public void run()
                                                            {
                                                                //update progress
                                                                taskProgress.setMessage(title + " (" + (updateIndex + 1) + spaceOfSpaceString + updateCount + ")");
                                                                taskProgress.setProgress(updateIndex + 1, updateCount);

                                                                //if ran update
                                                                if(ranUpdate)
                                                                {
                                                                    //update progress (90 - 100%)
                                                                    taskProgress.setMessage2(overallString);
                                                                    taskProgress.setProgress2(90 + (int)((updateIndex + 1) / (float)updateCount) * 10);
                                                                }
                                                            }
                                                        });
                                                    }
                                                    break;

                                                case Globals.ProgressType.Finished:
                                                    //hide display
                                                    taskProgress.dismiss();
                                                    break;
                                            }
                                        }
                                    }, new OrbitalFilterList.OnLoadItemsListener()
                                    {
                                        @Override
                                        public void onLoaded(ArrayList<OrbitalFilterList.Item> items, boolean foundLaunchDate)
                                        {
                                            //setup inputs
                                            MasterAddListActivity.this.runOnUiThread(new Runnable()
                                            {
                                                @Override
                                                public void run()
                                                {
                                                    //setup inputs
                                                    setupInputs(addAdapter, usedOwners, usedCategories);

                                                    //focus on search text
                                                    searchText.requestFocus();
                                                }
                                            });
                                        }
                                    }, checkChangedListener);
                                    addList.setAdapter(addAdapter);

                                    //don't dismiss
                                    allowDismiss = false;
                                }
                                //fall through

                            case Globals.ProgressType.Cancelled:
                                if(messageType == UpdateService.MessageTypes.General)
                                {
                                    //if allowing dismiss
                                    if(allowDismiss)
                                    {
                                        //hide display
                                        taskProgress.dismiss();
                                    }

                                    //if denied
                                    if(progressType == Globals.ProgressType.Denied)
                                    {
                                        //show login
                                        Globals.showAccountLogin(MasterAddListActivity.this, Globals.AccountType.SpaceTrack, updateType, new Globals.WebPageListener()
                                        {
                                            @Override
                                            public void onResult(Globals.WebPageData pageData, boolean success)
                                            {
                                                //if success or attempted to login
                                                if(success || pageData != null)
                                                {
                                                    //try again
                                                    MasterAddListActivity.this.runOnUiThread(new Runnable()
                                                    {
                                                        @Override
                                                        public void run()
                                                        {
                                                            updateMasterList(Settings.getSatelliteSource(context), true);
                                                        }
                                                    });
                                                }
                                                //else cancelled
                                                else
                                                {
                                                    //finished
                                                    MasterAddListActivity.this.finish();
                                                }
                                            }
                                        });
                                    }
                                    //else if just didn't finish
                                    else if(progressType == Globals.ProgressType.Cancelled || masterList.satellites.size() == 0)
                                    {
                                        //send result
                                        sendResult(data, progressType);
                                    }
                                }
                                break;
                        }
                        break;
                }
            }
        };

        //register receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(updateReceiver, new IntentFilter(UpdateService.UPDATE_FILTER));

        //return receiver
        return(updateReceiver);
    }

    //Sets up inputs
    private void setupInputs(MasterListAdapter listAdapter, ArrayList<UpdateService.MasterOwner> usedOwners, ArrayList<String> usedCategories)
    {
        //setup inputs
        listAdapter.setupInputs(searchGroup, ownerList, groupList, ageList, ageRow, searchText, showButton, usedOwners, usedCategories, addAdapter.getHasLaunchDates());

        //update title
        updateTitleCount();

        //update displays
        addButton.setEnabled(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.master_list_layout);

        //get intent, source, and displays
        Intent addIntent = this.getIntent();
        byte listType = addIntent.getByteExtra(ParamTypes.ListType, ListType.VisibleList);
        boolean isFilterList = (listType == ListType.VisibleList);
        boolean askUpdate = addIntent.getBooleanExtra(ParamTypes.AskUpdate, false);
        final int updateSource = addIntent.getIntExtra(Settings.PreferenceName.SatelliteSource, Database.UpdateSource.SpaceTrack);
        final MaterialButton cancelButton = this.findViewById(R.id.Master_Cancel_Button);
        searchGroup = this.findViewById(R.id.Orbital_Search_Table);
        ownerList = this.findViewById(R.id.Orbital_Search_Owner_List);
        groupList = this.findViewById(R.id.Orbital_Search_Group_List);
        ageList = this.findViewById(R.id.Orbital_Search_Age_List);
        addList = this.findViewById(R.id.Master_Add_List);
        searchText = this.findViewById(R.id.Orbital_Search_Text);
        ageRow = this.findViewById(R.id.Orbital_Search_Age_Row);
        showButton = this.findViewById(R.id.Orbital_Search_Show_Button);
        addButton = this.findViewById(R.id.Master_Add_Button);

        //create dialogs
        downloadProgress = Globals.createProgressDialog(this);
        addProgress = Globals.createProgressDialog(this);

        //get request code
        requestCode = BaseInputActivity.getRequestCode(addIntent);

        //get display
        masterLayout = this.findViewById(R.id.Master_Layout);

        //setup list
        addList.setHasFixedSize(true);
        addList.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
        addList.setLayoutManager(new LinearLayoutManager(this));

        //setup title
        itemSelectedCount = 0;
        startTitle = this.getString(isFilterList ? R.string.title_select_visible : R.string.title_select_satellites);
        this.setTitle(startTitle);

        //setup cancel button
        cancelButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //close
                MasterAddListActivity.this.sendResult(RESULT_CANCELED);
                MasterAddListActivity.this.finish();
            }
        });

        //setup check changed listener
        checkChangedListener = new MasterListAdapter.OnItemCheckChangedListener()
        {
            @Override
            public void onCheckChanged(OrbitalFilterList.Item item)
            {
                //if now checked
                if(item.isChecked)
                {
                    //add to count
                    itemSelectedCount++;
                }
                //else if unchecked and at least 1 selected
                else if(itemSelectedCount > 0)
                {
                    //remove from count
                    itemSelectedCount--;
                }

                //update title
                updateTitleCount(itemSelectedCount);
            }
        };

        //if for visible list
        if(isFilterList)
        {
            //create adapter
            addAdapter = new MasterListAdapter(this, new OrbitalFilterList.OnLoadItemsListener()
            {
                @Override
                public void onLoaded(ArrayList<OrbitalFilterList.Item> items, boolean foundLaunchDate)
                {
                    //get used data
                    final OrbitalFilterList.OrbitalListAdapter.UsedData used = addAdapter.getUsed();

                    //setup inputs
                    MasterAddListActivity.this.runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            //setup inputs
                            setupInputs(addAdapter, used.owners, used.categories);

                            //hide search by default
                            addAdapter.showSearchInputs(false);
                        }
                    });
                }
            }, checkChangedListener);
            addList.setAdapter(addAdapter);

            //setup add button
            addButton.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    int index;
                    OrbitalFilterList.Item[] items = addAdapter.getAllItems();

                    //go through each item
                    for(index = 0; index < items.length; index++)
                    {
                        //remember current item
                        OrbitalFilterList.Item currentItem = items[index];

                        //if checked state has changed
                        if(currentItem instanceof MasterListAdapter.ListItem && currentItem.isChecked != ((MasterListAdapter.ListItem)currentItem).startChecked)
                        {
                            //save with updated select state
                            Database.saveSatelliteVisible(MasterAddListActivity.this, currentItem.satellite.noradId, currentItem.isChecked);
                        }
                    }

                    MasterAddListActivity.this.sendResult(RESULT_OK);
                    MasterAddListActivity.this.finish();
                }
            });
            addButton.setText(R.string.title_set);
        }
        else
        {
            Calendar timeNow = Calendar.getInstance();
            int day = timeNow.get(Calendar.DAY_OF_YEAR);
            int year = timeNow.get(Calendar.YEAR);
            boolean haveData = (UpdateService.getMasterListAgeMs(this) != Long.MAX_VALUE);
            boolean needUpdate = UpdateService.getMasterListNeedUpdate(this, updateSource);
            boolean askedToday = (day == lastNeedUpdateDay && year == lastNeedUpdateYear);

            //setup add button
            addButton.setEnabled(false);
            addButton.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    MasterListAdapter satelliteListAdapter = (MasterListAdapter)addList.getAdapter();
                    final OrbitalFilterList.Item[] selectedItems = (satelliteListAdapter != null ? satelliteListAdapter.getSelectedItems() : new MasterListAdapter.ListItem[0]);

                    //update selected satellites
                    updateSatellites(selectedItems);
                }
            });

            //if -need update and -didn't already ask today- or -don't have data-- or -user asking for update-
            if((needUpdate && (!askedToday || !haveData)) || askUpdate)
            {
                //confirm to update
                showConfirmUpdateDialog(updateSource);

                //update last need update day and year
                lastNeedUpdateDay = day;
                lastNeedUpdateYear = year;
            }
            else
            {
                //use existing master list
                updateMasterList(updateSource, false);
            }
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        //if receivers are set
        if(updateReceiver != null)
        {
            //remove it
            LocalBroadcastManager.getInstance(this).unregisterReceiver(updateReceiver);
        }
        if(listReceiver != null)
        {
            //remove it
            LocalBroadcastManager.getInstance(this).unregisterReceiver(listReceiver);
        }

        //if progress displays are visible
        if(downloadProgress != null && downloadProgress.isShowing())
        {
            //close it
            downloadProgress.dismiss();
        }
        if(addProgress != null && addProgress.isShowing())
        {
            //close it
            addProgress.dismiss();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        //create options menu
        getMenuInflater().inflate(R.menu.menu_master_layout, menu);
        return(super.onCreateOptionsMenu(menu));
    }

    @Override @SuppressLint("NotifyDataSetChanged")
    public boolean onOptionsItemSelected(final MenuItem item)
    {
        int id = item.getItemId();
        boolean isAll = (id == R.id.menu_all);
        boolean isNone = (id == R.id.menu_none);

        //if -list exists- and -for all or none-
        if(addAdapter != null && (isAll || isNone))
        {
            //get current items to change
            OrbitalFilterList.Item[] currentItems = addAdapter.getDisplayedItems();

            //go through each item
            for(OrbitalFilterList.Item currentItem : currentItems)
            {
                //update checked state
                currentItem.setChecked(isAll);
            }

            //update title
            updateTitleCount();

            //update display
            addAdapter.notifyDataSetChanged();
        }

        return(true);
    }

    //Update satellites
    private void updateSatellites(final OrbitalFilterList.Item[] selectedItems)
    {
        int index;
        Resources res = this.getResources();
        ArrayList<Database.DatabaseSatellite> selectedSatellites = new ArrayList<>(0);

        //if at least 1 item selected
        if(selectedItems.length > 0)
        {
            //start showing progress
            Globals.setUpdateDialog(addProgress, res.getString(R.string.title_satellites_adding), MasterAddListActivity.this, UpdateService.UpdateType.UpdateSatellites, true);
            addProgress.show();

            //go through each item
            for(index = 0; index < selectedItems.length; index++)
            {
                //remember current item
                OrbitalFilterList.Item currentItem = selectedItems[index];

                //create and add new satellite
                Database.DatabaseSatellite currentSatellite = new Database.DatabaseSatellite(currentItem.satellite.name, currentItem.satellite.noradId, currentItem.satellite.ownerCode, currentItem.launchDateMs, currentItem.satellite.orbitalType);
                selectedSatellites.add(currentSatellite);
            }

            //get satellite TLE data
            updateReceiver = createLocalBroadcastReceiver(updateReceiver, addProgress, selectedItems);
            if(!UpdateService.updatingSatellites())
            {
                UpdateService.updateSatellites(MasterAddListActivity.this, res.getString(R.string.title_satellites_adding), selectedSatellites, false);
            }
        }
        else
        {
            //show none selected
            Globals.showSnackBar(masterLayout, res.getString(R.string.title_satellites_none_selected), true);
        }
    }

    //Starts updating master list
    private void updateMasterList(int updateSource, boolean getUpdate)
    {
        Resources res = this.getResources();

        //setup dialog and start showing progress
        Globals.setUpdateDialog(downloadProgress, res.getString(R.string.title_list_getting), this, UpdateService.UpdateType.GetMasterList, true);
        if(downloadProgress != null)
        {
            downloadProgress.show();
        }

        //get list
        listReceiver = createLocalBroadcastReceiver(listReceiver, downloadProgress, null);
        if(!UpdateService.updatingMasterList())
        {
            //get update when desired
            UpdateService.updateMasterList(this, updateSource, getUpdate, false, false);
        }
        else
        {
            //hide notification
            UpdateService.setNotificationVisible(UpdateService.UpdateType.GetMasterList, false);
        }
    }

    //Updates title count
    private void updateTitleCount(int count)
    {
        //add count to title for any selected
        this.setTitle(startTitle + (count > 0 ? (" (" + count + ")") : ""));
    }
    private void updateTitleCount()
    {
        //if adapter exists
        if(addAdapter != null)
        {
            //update title
            itemSelectedCount = addAdapter.getSelectedItems().length;
            updateTitleCount(itemSelectedCount);
        }
    }

    //Shows a confirm update dialog
    private void showConfirmUpdateDialog(final int updateSource)
    {
        long listAgeMs = UpdateService.getMasterListAgeMs(this);
        boolean haveAge = (listAgeMs != Long.MAX_VALUE);
        long listAgeDays = (haveAge ? (long)(listAgeMs / Calculations.MsPerDay) : 0);
        String message;
        Resources res = this.getResources();

        if(haveAge)
        {
            message = res.getString(R.string.text_list_is) + " " + ((listAgeDays > 0 ? res.getQuantityString(R.plurals.text_days, (int)listAgeDays, (int)listAgeDays) : (res.getString(R.string.text_less_than) + " " + res.getQuantityString(R.plurals.text_days, 1, 1))) + " " + res.getString(R.string.text_old));
        }
        else
        {
            message = res.getString(R.string.text_list_no_data);
        }
        message += ".  " + res.getString(R.string.text_update_now_question);

        Globals.showConfirmDialog(this, res.getString(R.string.title_list_update), message, res.getString(R.string.title_yes), res.getString(R.string.title_no), false, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int which)
            {
                //get update
                updateMasterList(updateSource, true);
            }
        }, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int which)
            {
                //use existing
                updateMasterList(updateSource, false);
            }
        }, null);
    }

    //Send result
    private void sendResult(Intent data, long updateIndex, long updateCount, int progressType)
    {
        //set result
        BaseInputActivity.setRequestCode(data, requestCode);
        if(updateIndex >= 0)
        {
            data.putExtra(ParamTypes.SuccessCount, updateIndex);
        }
        if(updateCount >= 0)
        {
            data.putExtra(ParamTypes.TotalCount, updateCount);
        }
        data.putExtra(ParamTypes.ProgressType, progressType);
        this.setResult((progressType == Globals.ProgressType.Finished ? RESULT_OK : RESULT_CANCELED), data);
        this.finish();
    }
    private void sendResult(Intent data, int progressType)
    {
        sendResult(data, -1, -1, progressType);
    }
    private void sendResult(int resultCode)
    {
        Intent data = new Intent();

        //add any request code
        BaseInputActivity.setRequestCode(data, requestCode);

        //set result
        this.setResult(resultCode, data);
    }

    //Shows add list
    public static void showList(final Activity context, ActivityResultLauncher<Intent> launcher, final boolean askUpdate, boolean confirmInternet)
    {
        //if confirm internet and should ask
        if(confirmInternet && Globals.shouldAskInternetConnection(context))
        {
            //get confirmation
            Globals.showConfirmInternetDialog(context, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    //don't ask this time
                    showList(context, launcher, askUpdate, false);
                }
            });
        }
        else
        {
            Intent intent = new Intent(context, MasterAddListActivity.class);
            intent.putExtra(ParamTypes.ListType, ListType.MasterList);
            intent.putExtra(ParamTypes.AskUpdate, askUpdate);
            intent.putExtra(Settings.PreferenceName.SatelliteSource, Settings.getSatelliteSource(context));
            Globals.startActivityForResult(launcher, intent, BaseInputActivity.RequestCode.MasterAddList);
        }
    }

    //Shows manual orbital input
    public static void showManual(Activity context, ActivityResultLauncher<Intent> launcher)
    {
        Intent intent = new Intent(context, ManualOrbitalInputActivity.class);
        Globals.startActivityForResult(launcher, intent, BaseInputActivity.RequestCode.ManualOrbitalInput);
    }
}