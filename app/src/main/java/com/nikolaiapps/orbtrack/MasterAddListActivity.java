package com.nikolaiapps.orbtrack;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.lifecycle.Observer;
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
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;


public class MasterAddListActivity extends BaseInputActivity
{
    public static abstract class ListType
    {
        static final byte MasterList = 0;
        static final byte VisibleList = 1;
        static final byte SelectSingleList = 2;
        static final byte SelectMultipleList = 3;
    }

    public static abstract class ParamTypes
    {
        static final String ListType = "listType";
        static final String SuccessCount = "successCount";
        static final String TotalCount = "totalCount";
        static final String ProgressType = "progressType";
        static final String AskUpdate = "askUpdate";
        static final String SelectedOrbitals = "selectedOrbitals";
        static final String ListNumber = "listNumber";
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
                int orbitalCount;
                boolean foundLaunchDate = false;
                Resources res;
                Context context = (Context)params[0];
                UpdateService.MasterListType masterList = (UpdateService.MasterListType)params[1];
                ArrayList<OrbitalFilterList.Item> items = new ArrayList<>(0);
                Selectable.ListItem[] selectedOrbitals = (Selectable.ListItem[])params[2];
                boolean useAllSelected = (boolean)params[3];
                boolean haveSelectedOrbitals = (!useAllSelected && selectedOrbitals != null);

                //if master list is set
                if(masterList != null)
                {
                    //go through each orbital
                    orbitalCount = masterList.satellites.size();
                    for(index = 0; index < orbitalCount; index++)
                    {
                        //remember current orbital, group, and item
                        UpdateService.MasterSatellite currentOrbital = masterList.satellites.get(index);
                        String currentOwner = currentOrbital.ownerCode;
                        ListItem newItem = new ListItem(currentOwner, currentOrbital, false);
                        int[] indexes = Globals.divideFind(newItem, items, new ListItem.Comparer(false));

                        //if item is not in the list yet
                        itemIndex = indexes[0];
                        if(itemIndex < 0)
                        {
                            //add to items
                            newItem.listIndex = indexes[1];
                            items.add(indexes[1], newItem);
                        }
                        else
                        {
                            //add to existing item categories
                            items.get(itemIndex).addOwner(currentOwner);
                        }

                        //if there is a launch date
                        if(currentOrbital.launchDateMs > 0)
                        {
                            //found
                            foundLaunchDate = true;
                        }

                        //if listener is set
                        if(progressChangedListener != null)
                        {
                            //send event
                            progressChangedListener.onProgressChanged(Globals.ProgressType.Running, null, index, orbitalCount);
                        }
                    }
                }
                else
                {
                    boolean[] orbitalIsSelected = null;
                    Database.DatabaseSatellite[] orbitals;

                    //get resources and orbitals
                    res = context.getResources();
                    orbitals = Database.getOrbitals(context);
                    orbitalCount = orbitals.length;

                    //if have selected orbitals
                    if(haveSelectedOrbitals)
                    {
                        //setup orbital is selected array
                        orbitalIsSelected = new boolean[orbitalCount];
                        Arrays.fill(orbitalIsSelected, false);

                        //go through each selected orbital
                        for(Selectable.ListItem currentItem : selectedOrbitals)
                        {
                            //remember current index
                            int currentIndex = currentItem.listIndex;

                            //if index is valid
                            if(currentIndex >= 0 && currentIndex < orbitalCount)
                            {
                                //set as selected
                                orbitalIsSelected[currentIndex] = true;
                            }
                        }
                    }

                    //go through each orbital
                    for(index = 0; index < orbitalCount; index++)
                    {
                        ListItem newItem;
                        UpdateService.MasterSatellite newSatellite;
                        Database.DatabaseSatellite currentSatellite = orbitals[index];
                        int currentId = currentSatellite.noradId;
                        String currentOwner = currentSatellite.ownerCode;
                        String[][] groups = Database.getSatelliteCategoriesEnglish(context, currentId);

                        //create satellite with categories
                        newSatellite = new UpdateService.MasterSatellite(currentId, currentSatellite.getName(), currentOwner, currentSatellite.ownerName, currentSatellite.orbitalType, currentSatellite.launchDateMs);
                        for(index2 = 0; index2 < groups.length; index2++)
                        {
                            //add category
                            newSatellite.categories.add(Database.LocaleCategory.getCategory(res, groups[index2][1]));
                        }

                        //create item
                        newItem = new ListItem(currentOwner, newSatellite, (haveSelectedOrbitals ? orbitalIsSelected[index] : currentSatellite.isSelected));

                        //if the sun
                        if(currentId == Universe.IDs.Sun)
                        {
                            //put it under the stars type for selection
                            newItem.satellite.orbitalType = Database.OrbitalType.Star;
                        }

                        //if item is not in the list yet
                        itemIndex = items.indexOf(newItem);
                        if(itemIndex < 0)
                        {
                            //add to items
                            newItem.listIndex = items.size();       //note; not -1 since before adding
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
                            progressChangedListener.onProgressChanged(Globals.ProgressType.Running, null, index, orbitalCount);
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
                    progressChangedListener.onProgressChanged(Globals.ProgressType.Finished, null, orbitalCount, orbitalCount);
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
        public static class ListItemHolder extends Selectable.ListDisplayItemHolder
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

        private final int listNumber;
        private final boolean isSingleSelect;
        private final boolean isVisibleSelect;
        private final LoadItemsTask loadItems;
        private final UpdateService.MasterListType masterList;
        private final OnItemCheckChangedListener itemCheckChangedListener;
        private final ArrayList<Selectable.ListItem> selectedOrbitals;

        public MasterListAdapter(final Context context, UpdateService.MasterListType masterList, ArrayList<Selectable.ListItem> selectedOrbitals, Globals.OnProgressChangedListener progressChangedListener, OrbitalFilterList.OnLoadItemsListener loadItemsListener, OnItemCheckChangedListener checkChangedListener, int listNumber, byte listType)
        {
            super(context);

            //set defaults
            loadingItems = true;
            this.itemsRefID = R.layout.image_checked_item;

            //set items and check changed listener
            this.listNumber = listNumber;
            this.isSingleSelect = (listType == ListType.SelectSingleList);
            this.isVisibleSelect = (listType == ListType.VisibleList);
            this.masterList = masterList;
            this.selectedOrbitals = (selectedOrbitals != null ? selectedOrbitals : new ArrayList<>(0));
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
        public MasterListAdapter(Context context, ArrayList<Selectable.ListItem> selectedOrbitals, OrbitalFilterList.OnLoadItemsListener loadItemsListener, OnItemCheckChangedListener checkChangedListener, int listNumber, byte listType)
        {
            this(context, null, selectedOrbitals, null, loadItemsListener, checkChangedListener, listNumber, listType);
        }

        @Override
        public void onAttachedToRecyclerView(@NonNull RecyclerView view)
        {
            super.onAttachedToRecyclerView(view);
            loadItems.execute(currentContext, masterList, selectedOrbitals.toArray(new Selectable.ListItem[0]), isVisibleSelect);
        }

        @Override @NonNull
        public ListItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
        {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(this.itemsRefID, parent, false);
            final ListItemHolder itemHolder = new ListItemHolder(itemView, R.id.Item_Checked_Image2, R.id.Item_Checked_Image1, R.id.Item_Checked_Text, R.id.Item_Checked_Progress);

            //if not selecting a single item
            if(!isSingleSelect)
            {
                //setup item click
                itemView.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        //invert checked state
                        itemHolder.checkBoxView.setChecked(!itemHolder.checkBoxView.isChecked());
                    }
                });
            }

            return(itemHolder);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position)
        {
            final OrbitalFilterList.Item currentItem;
            final ListItemHolder itemHolder = (ListItemHolder)holder;
            int showText = (loadingItems ? View.GONE : View.VISIBLE);
            int showProgress = (loadingItems ? View.VISIBLE : View.GONE);
            int showCheckbox = (!isSingleSelect ? showText : View.GONE);

            //update visibility
            itemHolder.itemText.setVisibility(showText);
            itemHolder.itemProgress.setVisibility(showProgress);
            itemHolder.checkBoxView.setVisibility(showCheckbox);

            //if not loading items
            if(!loadingItems)
            {
                //get current item
                currentItem = displayedItems.get(position);

                //set displays and update checked state
                itemHolder.orbitalImage.setBackgroundDrawable(Globals.getOrbitalIcon(currentContext, MainActivity.getObserver(), currentItem.satellite.noradId, currentItem.satellite.orbitalType));
                itemHolder.orbitalImage.setVisibility(View.VISIBLE);
                itemHolder.ownerImage.setBackgroundDrawable(Globals.getDrawableCombined(currentContext, Globals.getOwnerIconIDs(currentItem.satellite.ownerCode)));
                itemHolder.itemText.setText(currentItem.satellite.name);
                if(isSingleSelect)
                {
                    //setup item click
                    itemHolder.itemView.setOnClickListener(new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View v)
                        {
                            Intent data = new Intent();
                            MasterAddListActivity activity = (MasterAddListActivity)currentContext;
                            ArrayList<Selectable.ListItem> selectedOrbital = new ArrayList<>(1);

                            //add data and finish
                            selectedOrbital.add(currentItem);
                            data.putExtra(ParamTypes.ListNumber, listNumber);
                            data.putParcelableArrayListExtra(ParamTypes.SelectedOrbitals, selectedOrbital);
                            activity.sendResult(data, Globals.ProgressType.Finished);
                            activity.finish();
                        }
                    });
                }
                else
                {
                    //setup checkbox
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
    private Observer<Intent> updateReceiver;
    private Observer<Intent> listReceiver;

    //Displays
    private int itemSelectedCount;
    private boolean isSelectSingleList;
    private String startTitle;
    private LinearLayout masterLayout;
    private LinearLayout searchLayout;
    private RecyclerView addList;
    private MasterListAdapter addAdapter;
    private View ageLayout;
    private View groupLayout;
    private View ownerLayout;
    private View typeLayout;
    private View searchGroup;
    private SelectListInterface ownerList;
    private SelectListInterface groupList;
    private SelectListInterface ageList;
    private SelectListInterface typeList;
    private CustomSearchView searchView;
    private AppCompatImageButton showButton;
    private MaterialButton addButton;
    private MultiProgressDialog downloadProgress;
    private MultiProgressDialog addProgress;

    //Listeners
    private MasterListAdapter.OnItemCheckChangedListener checkChangedListener;

    //Creates local broadcast receiver
    private Observer<Intent> createLocalBroadcastReceiver(Observer<Intent> oldReceiver, byte receiverUpdateType, final MultiProgressDialog taskProgress, final OrbitalFilterList.Item[] selectedItems)
    {
        final Context context = this;
        final Resources res = this.getResources();
        final String overallString = res.getString(R.string.title_overall);
        final String savingString = res.getString(R.string.title_saving);
        final String loadingString = res.getString(R.string.title_loading);
        final String gettingString = res.getString(R.string.title_getting);

        //if old receiver is set
        if(oldReceiver != null)
        {
            //remove it
            UpdateService.removeObserver(oldReceiver);
        }

        //create receiver
        Observer<Intent> updateReceiver = new Observer<Intent>()
        {
            @Override
            public void onChanged(Intent intent)
            {
                //if intent isn't set
                if(intent == null)
                {
                    //stop
                    return;
                }

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
                boolean isGeneral = (messageType == NotifyService.MessageTypes.General);
                boolean isDownload = (messageType == NotifyService.MessageTypes.Download);
                UpdateService.MasterListType masterList = null;
                final Intent data = new Intent();

                //if not the update type being watched
                if(updateType != receiverUpdateType)
                {
                    //stop
                    return;
                }

                //handle based on update type
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
                                    taskProgress.setMessage(res.getQuantityString(R.plurals.title_space_of_space, (int)updateCount, updateValue, updateCount) + " (" + selectedItems[(int)updateIndex].satellite.name + ")");
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
                                    taskProgress.setMessage((messageType == NotifyService.MessageTypes.Save ? (savingString + " ") : messageType == NotifyService.MessageTypes.Load ? (loadingString + " ") :  !isDownload ? (gettingString + " ") : "") + section);
                                }
                                break;

                            case Globals.ProgressType.Success:
                                //if updating at least 1
                                if(updateCount > 0)
                                {
                                    //update message based on message type
                                    switch(messageType)
                                    {
                                        case NotifyService.MessageTypes.Save:
                                        case NotifyService.MessageTypes.Load:
                                            updateValue = updateIndex + 1;
                                            message = res.getQuantityString(R.plurals.title_space_of_space, (int)updateCount, updateValue, updateCount);
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
                                if(isGeneral)
                                {
                                    //get master list
                                    masterList = UpdateService.getMasterList();
                                    final boolean ranUpdate = masterList.justUpdated;
                                    final String title = res.getQuantityString(R.plurals.title_satellites, 2);
                                    final ArrayList<String> usedCategories = masterList.usedCategories;
                                    final ArrayList<UpdateService.MasterOwner> usedOwners = masterList.usedOwners;

                                    //add satellites
                                    addAdapter = new MasterListAdapter(MasterAddListActivity.this, masterList, null, new Globals.OnProgressChangedListener()
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
                                                                taskProgress.setMessage(title + " (" + res.getQuantityString(R.plurals.title_space_of_space, (int)updateCount, (updateIndex + 1), updateCount) + ")");
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
                                                    searchLayout.setVisibility(View.VISIBLE);
                                                    setupInputs(addAdapter, usedOwners, usedCategories, true, null);
                                                }
                                            });
                                        }
                                    }, checkChangedListener, -1, ListType.MasterList);
                                    addList.setAdapter(addAdapter);

                                    //don't dismiss
                                    allowDismiss = false;
                                }
                                //fall through

                            case Globals.ProgressType.Cancelled:
                                if(isGeneral)
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
                                                            updateMasterList(Settings.getSatelliteCatalogSource(context), true);
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
        UpdateService.observe(context, updateReceiver);

        //return receiver
        return(updateReceiver);
    }

    //Sets up inputs
    private void setupInputs(MasterListAdapter listAdapter, ArrayList<UpdateService.MasterOwner> usedOwners, ArrayList<String> usedCategories, boolean useAges, List<Byte> usedTypes)
    {
        //setup inputs
        listAdapter.setupInputs(searchGroup, (usedOwners != null ? ownerList : null), (usedCategories != null ? groupList : null), (useAges ? ageList : null), (usedTypes != null && usedTypes.size() > 1 ? typeList : null), ageLayout, groupLayout, ownerLayout, typeLayout, searchView, showButton, usedOwners, usedCategories, usedTypes, addAdapter.getHasLaunchDates());

        //update displays
        updateTitleCount();
        addButton.setEnabled(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(usingMaterial ? R.layout.master_list_material_layout : R.layout.master_list_layout);

        //get intent, source, and displays
        Intent addIntent = this.getIntent();
        byte listType = addIntent.getByteExtra(ParamTypes.ListType, ListType.VisibleList);
        boolean isVisibleList = (listType == ListType.VisibleList);
        isSelectSingleList = (listType == ListType.SelectSingleList);
        boolean isSelectMultipleList = (listType == ListType.SelectMultipleList);
        boolean askUpdate = addIntent.getBooleanExtra(ParamTypes.AskUpdate, false);
        int updateSource = addIntent.getIntExtra(Settings.PreferenceName.SatelliteSource, Database.UpdateSource.SpaceTrack);
        int listNumber = addIntent.getIntExtra(ParamTypes.ListNumber, 1);
        ArrayList<Selectable.ListItem> selectedOrbitals = addIntent.getParcelableArrayListExtra(ParamTypes.SelectedOrbitals);
        boolean haveSelectedOrbitals = (selectedOrbitals != null && selectedOrbitals.size() > 0);
        MaterialButton cancelButton = this.findViewById(R.id.Master_Cancel_Button);
        searchLayout = this.findViewById(R.id.Orbital_Search_Layout);
        searchGroup = this.findViewById(usingMaterial ? R.id.Orbital_Search_Lists_Layout : R.id.Orbital_Search_Table);
        ownerList = this.findViewById(usingMaterial ? R.id.Orbital_Search_Owner_Text_List : R.id.Orbital_Search_Owner_List);
        groupList = this.findViewById(usingMaterial ? R.id.Orbital_Search_Group_Text_List : R.id.Orbital_Search_Group_List);
        ageList = this.findViewById(usingMaterial ? R.id.Orbital_Search_Age_Text_List : R.id.Orbital_Search_Age_List);
        typeList = this.findViewById(usingMaterial ? R.id.Orbital_Search_Type_Text_List : R.id.Orbital_Search_Type_List);
        addList = this.findViewById(R.id.Master_Add_List);
        ownerLayout = this.findViewById(usingMaterial ? R.id.Orbital_Search_Owner_Layout : R.id.Orbital_Search_Owner_Row);
        groupLayout = this.findViewById(usingMaterial ? R.id.Orbital_Search_Group_Layout : R.id.Orbital_Search_Group_Row);
        ageLayout = this.findViewById(usingMaterial ? R.id.Orbital_Search_Age_Layout : R.id.Orbital_Search_Age_Row);
        typeLayout = this.findViewById(usingMaterial ? R.id.Orbital_Search_Type_Layout : R.id.Orbital_Search_Type_Row);
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
        startTitle = this.getString(isVisibleList ? R.string.title_select_visible : isSelectSingleList ? R.string.title_select_orbital : R.string.title_select_orbital_or_orbitals);
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

                //update title and add button
                updateTitleCount(itemSelectedCount);
                addButton.setEnabled(itemSelectedCount > 0);
            }
        };

        //if for visible or a select list
        if(isVisibleList || isSelectSingleList || isSelectMultipleList)
        {
            //create adapter
            addAdapter = new MasterListAdapter(this, selectedOrbitals, new OrbitalFilterList.OnLoadItemsListener()
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
                            //setup inputs and collapse
                            searchLayout.setVisibility(View.VISIBLE);
                            setupInputs(addAdapter, null, null, false, used.types);
                            addAdapter.showSearchInputs(false);
                            addButton.setEnabled(isVisibleList || haveSelectedOrbitals);
                        }
                    });
                }
            }, checkChangedListener, listNumber, listType);
            addAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver()
            {
                @Override public void onChanged()
                {
                    super.onChanged();

                    //if there are selected orbitals
                    if(haveSelectedOrbitals)
                    {
                        //get first selected position
                        int position = selectedOrbitals.get(0).listIndex;

                        //if position is valid
                        if(position >= 0)
                        {
                            //scroll to it
                            addList.scrollToPosition(position);
                        }
                    }
                }
            });
            addList.setAdapter(addAdapter);

            //setup add button
            if(isVisibleList || isSelectMultipleList)
            {
                //add click event and set text
                addButton.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        int index;
                        Intent data = new Intent();
                        OrbitalFilterList.Item[] items = addAdapter.getAllItems();
                        ArrayList<Selectable.ListItem> userSelectedOrbitals = new ArrayList<>(0);

                        //go through each item
                        for(index = 0; index < items.length; index++)
                        {
                            //remember current item
                            OrbitalFilterList.Item currentItem = items[index];

                            //if for select list
                            if(isSelectMultipleList)
                            {
                                //if current is selected
                                if(currentItem.isChecked)
                                {
                                    //add selected ID
                                    userSelectedOrbitals.add(currentItem);
                                }
                            }
                            else
                            {
                                //if checked state has changed
                                if(currentItem instanceof MasterListAdapter.ListItem && currentItem.isChecked != ((MasterListAdapter.ListItem)currentItem).startChecked)
                                {
                                    //save with updated select state
                                    Database.saveSatelliteVisible(MasterAddListActivity.this, currentItem.satellite.noradId, currentItem.isChecked);
                                }
                            }
                        }

                        //if not selecting multiple or at least 1 orbital selected
                        if(!isSelectMultipleList || userSelectedOrbitals.size() > 0)
                        {
                            //add data and finish
                            data.putExtra(ParamTypes.ListNumber, listNumber);
                            data.putParcelableArrayListExtra(ParamTypes.SelectedOrbitals, userSelectedOrbitals);
                            MasterAddListActivity.this.sendResult(data, Globals.ProgressType.Finished);
                            MasterAddListActivity.this.finish();
                        }
                    }
                });
                addButton.setText(isSelectMultipleList ? R.string.title_select : R.string.title_set);
            }
            addButton.setVisibility(isSelectSingleList ? View.GONE : View.VISIBLE);
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
            UpdateService.removeObserver(updateReceiver);
        }
        if(listReceiver != null)
        {
            //remove it
            UpdateService.removeObserver(listReceiver);
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        boolean granted = (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED);
        boolean retrying = (requestCode == Globals.PermissionType.PostNotificationsRetry);

        //handle response
        switch(requestCode)
        {
            case Globals.PermissionType.PostNotifications:
            case Globals.PermissionType.PostNotificationsRetry:
                //if granted
                if(granted)
                {
                    //if progress exists
                    if(downloadProgress != null)
                    {
                        //set as going to background and close dialog
                        downloadProgress.setTag(true);
                        downloadProgress.dismiss();
                    }
                }
                //else if not retrying
                else if(!retrying)
                {
                    //ask permission again
                    Globals.askPostNotificationsPermission(this, true);
                }
                break;
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuItem searchMenu;

        //create options menu
        getMenuInflater().inflate(R.menu.menu_master_layout, menu);
        searchMenu = menu.findItem(R.id.menu_search);
        searchView = (searchMenu != null ? (CustomSearchView)searchMenu.getActionView() : null);

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

            //update displays
            updateTitleCount();
            addButton.setEnabled(addAdapter.getSelectedItems().length > 0);

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
            updateReceiver = createLocalBroadcastReceiver(updateReceiver, UpdateService.UpdateType.UpdateSatellites, addProgress, selectedItems);
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
        listReceiver = createLocalBroadcastReceiver(listReceiver, UpdateService.UpdateType.GetMasterList, downloadProgress, null);
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
        this.setTitle(startTitle + (count > 0 && !isSelectSingleList ? (" (" + count + ")") : ""));
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

    @SuppressWarnings("SameParameterValue")
    private void sendResult(int resultCode)
    {
        Intent data = new Intent();

        //add any request code
        BaseInputActivity.setRequestCode(data, requestCode);

        //set result
        this.setResult(resultCode, data);
    }

    //Shows list
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
            intent.putExtra(Settings.PreferenceName.SatelliteSource, Settings.getSatelliteCatalogSource(context));
            Globals.startActivityForResult(launcher, intent, BaseInputActivity.RequestCode.MasterAddList);
        }
    }
    public static void showList(Activity context, ActivityResultLauncher<Intent> launcher, byte listType, byte requestCode, ArrayList<Selectable.ListItem> selectedOrbitals, int listNumber)
    {
        Intent intent = new Intent(context, MasterAddListActivity.class);
        intent.putExtra(ParamTypes.ListType, listType);
        intent.putExtra(ParamTypes.ListNumber, listNumber);
        intent.putExtra(ParamTypes.SelectedOrbitals, selectedOrbitals);
        Globals.startActivityForResult(launcher, intent, requestCode);
    }

    //Shows manual orbital input
    public static void showManual(Activity context, ActivityResultLauncher<Intent> launcher)
    {
        Intent intent = new Intent(context, ManualOrbitalInputActivity.class);
        Globals.startActivityForResult(launcher, intent, BaseInputActivity.RequestCode.ManualOrbitalInput);
    }
}