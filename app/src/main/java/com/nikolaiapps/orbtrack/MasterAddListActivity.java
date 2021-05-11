package com.nikolaiapps.orbtrack;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
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
import java.util.Comparator;


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

    private static class MasterListAdapter extends Selectable.ListBaseAdapter
    {
        //On load items listener
        private interface OnLoadItemsListener
        {
            void onLoaded(ArrayList<ListItem> items, boolean foundLaunchDate);
        }

        //Load items task
        private static class LoadItemsTask extends ThreadTask<Object, Void, Void>
        {
            private final Globals.OnProgressChangedListener progressChangedListener;
            private final OnLoadItemsListener loadItemsListener;

            public LoadItemsTask(Globals.OnProgressChangedListener progressChangedListener, OnLoadItemsListener loadItemsListener)
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
                ArrayList<ListItem> items = new ArrayList<>(0);
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
                        ListItem newItem = new ListItem(context, currentOwner, currentSatellite, false);
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
                        String[][] groups = Database.getSatelliteCategoriesEnglish(context, currentSatellite.norad);

                        //create satellite with categories
                        newSatellite = new UpdateService.MasterSatellite(currentSatellite.norad, currentSatellite.getName(), currentOwner, currentSatellite.ownerName, currentSatellite.launchDateMs);
                        for(index2 = 0; index2 < groups.length; index2++)
                        {
                            //add category
                            newSatellite.categories.add(Database.LocaleCategory.getCategory(res, groups[index2][1]));
                        }

                        //create item
                        newItem = new ListItem(context, currentOwner, newSatellite, currentSatellite.isSelected);

                        //if item is not in the list yet
                        itemIndex = items.indexOf(newItem);
                        if(itemIndex < 0)
                        {
                            //add to items
                            items.add(newItem);
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
        public static class ListItem extends Selectable.ListItem
        {
            public final boolean startChecked;
            public final long launchDateMs;
            private final Drawable orbitalIcon;
            private final Drawable ownerIcon;
            public final UpdateService.MasterSatellite satellite;
            public final ArrayList<String> ownerCodes;
            public final ArrayList<String> categories;

            public static class Comparer implements Comparator<ListItem>
            {
                private final boolean nameOnly;

                public Comparer(boolean byNameOnly)
                {
                    nameOnly = byNameOnly;
                }

                @Override
                public int compare(ListItem value1, ListItem value2)
                {
                    if(!nameOnly && value1.satellite.name.equals(value2.satellite.name))
                    {
                        return(Globals.intCompare(value1.satellite.noradId, value2.satellite.noradId));
                    }
                    else
                    {
                        return(value1.satellite.name.compareTo(value2.satellite.name));
                    }
                }
            }

            public ListItem(Context context, String own, UpdateService.MasterSatellite sat, boolean startCheck)
            {
                super(-1, -1, false, false, true, startCheck);

                int index;
                String currentGroup;
                Drawable[] ownerIcons = Settings.getOwnerIcons(context, sat.noradId, sat.ownerCode);

                startChecked = isChecked;
                launchDateMs = sat.launchDateMs;
                orbitalIcon = Globals.getOrbitalIcon(context, MainActivity.getObserver(), sat.noradId, sat.orbitalType);
                ownerIcon = Globals.getDrawable(context, ownerIcons);
                satellite = sat;
                ownerCodes = new ArrayList<>(0);
                categories = sat.categories;
                ownerCodes.add(own);

                //go through all groups
                for(index = 0; index < categories.size(); index++)
                {
                    //set group to upper case
                    currentGroup = categories.get(index);
                    if(currentGroup != null)
                    {
                        categories.set(index, currentGroup.toUpperCase());
                    }
                }
            }

            public void addOwner(String ownerCode)
            {
                if(!ownerCodes.contains(ownerCode))
                {
                    ownerCodes.add(ownerCode);
                }
            }

            public boolean equals(Object obj)
            {
                //if a ListItem
                if(obj instanceof ListItem)
                {
                    //equal if norad ID matches
                    ListItem other = (ListItem)obj;
                    return(other.satellite.noradId == this.satellite.noradId);
                }
                else
                {
                    //not equal
                    return(false);
                }
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

        private boolean loadingItems;
        private boolean foundLaunchDate;
        private ArrayList<ListItem> displayedItems;
        private ListItem[] allItems;

        public MasterListAdapter(final Context context, UpdateService.MasterListType masterList, Globals.OnProgressChangedListener listener)
        {
            super(context);

            //set default
            foundLaunchDate = false;
            loadingItems = true;
            displayedItems = new ArrayList<>(0);
            allItems = new ListItem[0];
            this.itemsRefID = R.layout.image_checked_item;

            //load items
            LoadItemsTask loadItems = new LoadItemsTask(listener, new OnLoadItemsListener()
            {
                @Override
                public void onLoaded(ArrayList<ListItem> items, boolean foundLaunchDate)
                {
                    displayedItems = items;
                    allItems = items.toArray(new ListItem[0]);
                    MasterListAdapter.this.loadingItems = false;
                    MasterListAdapter.this.foundLaunchDate = foundLaunchDate;
                    if(context instanceof Activity)
                    {
                        ((Activity)context).runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                MasterListAdapter.this.notifyDataSetChanged();
                            }
                        });
                    }
                }
            });
            loadItems.execute(context, masterList);
        }
        public MasterListAdapter(Context context)
        {
            this(context, null, null);
        }

        @Override
        public int getItemCount()
        {
            return(loadingItems ? 1 : displayedItems.size());
        }

        @Override
        public ListItem getItem(int position)
        {
            return(loadingItems ? null : displayedItems.get(position));
        }

        @Override
        public long getItemId(int position)
        {
            return(loadingItems ? 0 : displayedItems.get(position).satellite.noradId);
        }

        @Override
        public @NonNull ListItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
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
            final ListItem currentItem;
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
                itemHolder.orbitalImage.setBackgroundDrawable(currentItem.orbitalIcon);
                itemHolder.orbitalImage.setVisibility(View.VISIBLE);
                itemHolder.ownerImage.setBackgroundDrawable(currentItem.ownerIcon);
                itemHolder.itemText.setText(currentItem.satellite.name);
                itemHolder.checkBoxView.setOnCheckedChangeListener(null);
                itemHolder.checkBoxView.setChecked(currentItem.isChecked);
                itemHolder.checkBoxView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
                {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean checked)
                    {
                        currentItem.isChecked = checked;
                    }
                });
            }
        }

        //Sets visibility of views by group
        public void showViews(String ownerCode, String categoryName, long currentMs, int ageValue, String searchString)
        {
            int index;
            Resources res = currentContext.getResources();
            String listAllString = res.getString(R.string.title_list_all);
            String searchStringInsensitive = searchString.trim().toLowerCase();

            //clear current items
            displayedItems.clear();

            //go through all views
            for(index = 0; index < allItems.length; index++)
            {
                //remember current item
                ListItem item = allItems[index];

                //add to displayed if -category is "ALL" or matches given- and -age is any or within day range- and -search string is "" or contains given-
                if((ownerCode.equals(listAllString) || item.ownerCodes.contains(ownerCode)) && (categoryName.equals(listAllString) || item.categories.contains(categoryName)) && (ageValue == -1 || ((currentMs - item.launchDateMs) <= (ageValue * Calculations.MsPerDay))) && (searchStringInsensitive.equals("") || item.satellite.name.toLowerCase().contains(searchStringInsensitive) || String.valueOf(item.satellite.noradId).contains(searchStringInsensitive)))
                {
                    //add to displayed items
                    displayedItems.add(item);
                }
            }

            //refresh list
            this.notifyDataSetChanged();
        }

        //Gets all items
        public ListItem[] getAllItems()
        {
            return(loadingItems ? new ListItem[0] : allItems);
        }

        //Gets selected items
        public ListItem[] getSelectedItems()
        {
            int index;
            ArrayList<ListItem> selectedItems = new ArrayList<>(0);

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
            return(selectedItems.toArray(new ListItem[0]));
        }

        //Returns if a launch date was found
        public boolean hasLaunchDates()
        {
            return(foundLaunchDate);
        }
    }

    //Status
    private static int lastNeedUpdateDay = -1;
    private static int lastNeedUpdateYear = -1;

    //Data
    private BroadcastReceiver updateReceiver;
    private BroadcastReceiver listReceiver;

    //Displays
    private int listBgColor;
    private int listBgItemColor;
    private int listTextColor;
    private int listBgSelectedColor;
    private int listTextSelectedColor;
    private LinearLayout masterLayout;
    private RecyclerView addList;
    private MasterListAdapter addAdapter;
    private IconSpinner ownerList;
    private IconSpinner groupList;
    private IconSpinner ageList;
    private EditText searchText;
    private TableRow ageRow;
    private MaterialButton addButton;
    private MultiProgressDialog downloadProgress;
    private MultiProgressDialog addProgress;

    //Creates an on item selected listener
    private AdapterView.OnItemSelectedListener createOnItemSelectedListener(final MasterListAdapter listAdapter, final IconSpinner ownerList, final IconSpinner groupList, final IconSpinner ageList, final EditText masterSearchText)
    {
        return(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                //update visible items
                listAdapter.showViews((String)ownerList.getSelectedValue(""), groupList.getSelectedValue("").toString(), System.currentTimeMillis(), (int)ageList.getSelectedValue(0), masterSearchText.getText().toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    //Creates a text changed listener
    private TextWatcher createTextChangedListener(final MasterListAdapter listAdapter, final IconSpinner ownerList, final IconSpinner groupList, final IconSpinner ageList)
    {
        return(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after){ }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s)
            {
                //update visible items
                listAdapter.showViews((String)ownerList.getSelectedValue(""), groupList.getSelectedValue("").toString(), System.currentTimeMillis(), (int)ageList.getSelectedValue(0), s.toString());
            }
        });
    }

    //Creates local broadcast receiver
    private BroadcastReceiver createLocalBroadcastReceiver(BroadcastReceiver oldReceiver, final MultiProgressDialog taskProgress, final MasterListAdapter.ListItem[] selectedItems)
    {
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
                long updateValue = -1;
                final Resources res = MasterAddListActivity.this.getResources();
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
                                lockScreenOrientation(true);

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
                                    taskProgress.setMessage(updateValue + res.getString(R.string.text_space_of_space) + updateCount + " (" + selectedItems[(int)updateIndex].satellite.name + ")");
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

                                //unlock rotation
                                lockScreenOrientation(false);

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
                                    taskProgress.setMessage((messageType == UpdateService.MessageTypes.Save ? (res.getString(R.string.title_saving) + " ") : messageType == UpdateService.MessageTypes.Load ? (res.getString(R.string.title_loading) + " ") :  !isDownload ? (res.getString(R.string.title_getting) + " ") : "") + section);
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
                                            message = (updateValue + res.getString(R.string.text_space_of_space) + updateCount);
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
                                break;

                            case Globals.ProgressType.Finished:
                            case Globals.ProgressType.Failed:
                            case Globals.ProgressType.Denied:
                                if(messageType == UpdateService.MessageTypes.General)
                                {
                                    //get master list
                                    masterList = UpdateService.getMasterList();
                                    final String title = res.getQuantityString(R.plurals.title_satellites, 2);
                                    final String separator = res.getString(R.string.text_space_of_space);
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
                                                                taskProgress.setMessage(title + " (" + (updateIndex + 1) + separator + updateCount + ")");
                                                                taskProgress.setProgress(updateIndex + 1, updateCount);
                                                            }
                                                        });
                                                    }
                                                    break;

                                                case Globals.ProgressType.Finished:
                                                    //hide display
                                                    taskProgress.dismiss();

                                                    //setup inputs
                                                    MasterAddListActivity.this.runOnUiThread(new Runnable()
                                                    {
                                                        @Override
                                                        public void run()
                                                        {
                                                            //setup inputs
                                                            setupInputs(usedOwners, usedCategories);

                                                            //focus on search text
                                                            searchText.requestFocus();
                                                        }
                                                    });
                                                    break;
                                            }
                                        }
                                    });

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

    //Sets up owner list
    private void setupOwnerList(IconSpinner ownerList, ArrayList<UpdateService.MasterOwner> usedOwners, AdapterView.OnItemSelectedListener itemSelectedListener)
    {
        int index;
        Resources res = this.getResources();
        String listAllString = res.getString(R.string.title_list_all);
        String unknown = Globals.getUnknownString(this).toUpperCase();
        IconSpinner.Item[] owners;

        owners = new IconSpinner.Item[usedOwners.size() + 1];
        owners[0] = new IconSpinner.Item(listAllString, listAllString);
        for(index = 0; index < usedOwners.size(); index++)
        {
            UpdateService.MasterOwner currentItem = usedOwners.get(index);
            Drawable[] ownerIcons = Settings.getOwnerIcons(this, (currentItem.code == null ? Integer.MIN_VALUE : Integer.MAX_VALUE), currentItem.code);
            owners[index + 1] = new IconSpinner.Item(Globals.getDrawable(this, ownerIcons), null, null, (currentItem.name == null || currentItem.name.equals("") ? unknown : currentItem.name), currentItem.code);
        }
        ownerList.setAdapter(new IconSpinner.CustomAdapter(this, owners));
        ownerList.setBackgroundColor(listBgColor);
        ownerList.setBackgroundItemColor(listBgItemColor);
        ownerList.setBackgroundItemSelectedColor(listBgSelectedColor);
        ownerList.setTextColor(listTextColor);
        ownerList.setTextSelectedColor(listTextSelectedColor);
        ownerList.setOnItemSelectedListener(itemSelectedListener);
        ownerList.setEnabled(true);
    }

    //Sets up group list
    private void setupGroupList(IconSpinner groupList, ArrayList<String> usedCategories, AdapterView.OnItemSelectedListener itemSelectedListener)
    {
        Resources res = this.getResources();
        ArrayList<String> groups;

        groups = usedCategories;
        groups.add(0, res.getString(R.string.title_list_all));
        groupList.setAdapter(new IconSpinner.CustomAdapter(this, groups.toArray(new String[0])));
        groupList.setBackgroundColor(listBgColor);
        groupList.setBackgroundItemColor(listBgItemColor);
        groupList.setBackgroundItemSelectedColor(listBgSelectedColor);
        groupList.setTextColor(listTextColor);
        groupList.setTextSelectedColor(listTextSelectedColor);
        groupList.setOnItemSelectedListener(itemSelectedListener);
        groupList.setEnabled(true);
    }

    //Sets up age list
    private void setupAgeList(IconSpinner ageList, AdapterView.OnItemSelectedListener itemSelectedListener)
    {
        Resources res = this.getResources();
        String lastString = res.getString(R.string.title_last_plural);
        String daysString = res.getString(R.string.title_days);
        String monthsString = res.getString(R.string.title_months);

        ageList.setAdapter(new IconSpinner.CustomAdapter(this, new IconSpinner.Item[]
        {
            new IconSpinner.Item(res.getString(R.string.title_any), -1),
            new IconSpinner.Item(res.getString(R.string.title_today), 0),
            new IconSpinner.Item(lastString + " " + res.getString(R.string.text_3) + " " + daysString, 3),
            new IconSpinner.Item(lastString + " " + res.getString(R.string.text_7) + " " + daysString, 7),
            new IconSpinner.Item(lastString + " " + res.getString(R.string.text_14) + " " + daysString, 14),
            new IconSpinner.Item(lastString + " " + res.getString(R.string.text_30) + " " + daysString, 30),
            new IconSpinner.Item(lastString + " " + res.getString(R.string.text_3) + " " + monthsString, 93),
            new IconSpinner.Item(lastString + " " + res.getString(R.string.text_6) + " " + monthsString, 183),
            new IconSpinner.Item(res.getString(R.string.title_this_year), 366)
        }));
        ageList.setBackgroundColor(listBgColor);
        ageList.setBackgroundItemColor(listBgItemColor);
        ageList.setBackgroundItemSelectedColor(listBgSelectedColor);
        ageList.setTextColor(listTextColor);
        ageList.setTextSelectedColor(listTextSelectedColor);
        ageList.setOnItemSelectedListener(itemSelectedListener);
        ageList.setEnabled(true);
    }

    //Sets up inputs
    private void setupInputs(ArrayList<UpdateService.MasterOwner> usedOwners, ArrayList<String> usedCategories)
    {
        //create item selected listener
        AdapterView.OnItemSelectedListener itemSelectedListener = createOnItemSelectedListener(addAdapter, ownerList, groupList, ageList, searchText);

        //get owners, groups, and ages
        setupOwnerList(ownerList, usedOwners, itemSelectedListener);
        setupGroupList(groupList, usedCategories, itemSelectedListener);
        setupAgeList(ageList, itemSelectedListener);

        //update age row visibility
        ageRow.setVisibility(addAdapter.hasLaunchDates() ? View.VISIBLE : View.GONE);

        //setup name text
        searchText.addTextChangedListener(createTextChangedListener(addAdapter, ownerList, groupList, ageList));
        searchText.setEnabled(true);

        //update displays
        addList.setAdapter(addAdapter);
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
        final TableLayout searchGroup = this.findViewById(R.id.Master_Search_Table);
        final AppCompatImageButton showButton = this.findViewById(R.id.Master_Show_Button);
        final MaterialButton cancelButton = this.findViewById(R.id.Master_Cancel_Button);
        ownerList = this.findViewById(R.id.Master_Owner_List);
        groupList = this.findViewById(R.id.Master_Group_List);
        ageList = this.findViewById(R.id.Master_Age_List);
        addList = this.findViewById(R.id.Master_Add_List);
        searchText = this.findViewById(R.id.Master_Search_Text);
        ageRow = this.findViewById(R.id.Master_Age_Row);
        addButton = this.findViewById(R.id.Master_Add_Button);

        listBgColor = Globals.resolveColorID(this, R.attr.pageTitleBackground);
        listBgItemColor = Globals.resolveColorID(this, R.attr.pageBackground);
        listTextColor = Globals.resolveColorID(this, R.attr.defaultTextColor);
        listBgSelectedColor = Globals.resolveColorID(this, R.attr.columnBackground);
        listTextSelectedColor = Globals.resolveColorID(this, R.attr.columnTitleTextColor);
        downloadProgress = Globals.createProgressDialog(this);
        addProgress = Globals.createProgressDialog(this);

        //get display
        masterLayout = this.findViewById(R.id.Master_Layout);

        //setup list
        addList.setHasFixedSize(true);
        addList.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
        addList.setLayoutManager(new LinearLayoutManager(this));

        //setup title
        this.setTitle(isFilterList ? R.string.title_set_visible_orbitals : R.string.title_select_satellites);

        //setup show button
        showButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if(searchGroup.getVisibility() == View.VISIBLE)
                {
                    showButton.setImageResource(R.drawable.ic_expand_more);
                    searchGroup.setVisibility(View.GONE);
                }
                else
                {
                    showButton.setImageResource(R.drawable.ic_expand_less);
                    searchGroup.setVisibility(View.VISIBLE);
                }
            }
        });

        //setup cancel button
        cancelButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //close
                setResult(RESULT_CANCELED);
                MasterAddListActivity.this.finish();
            }
        });

        //if for visible list
        if(isFilterList)
        {
            int index;
            int index2;
            MasterListAdapter.ListItem[] allItems;
            ArrayList<String> usedCategories = new ArrayList<>(0);
            ArrayList<String> usedOwnerCodes = new ArrayList<>(0);
            ArrayList<UpdateService.MasterOwner> usedOwners = new ArrayList<>(0);

            //create adapter
            addAdapter = new MasterListAdapter(this);

            //setup add button
            addButton.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    int index;
                    MasterAddListActivity.MasterListAdapter.ListItem[] items = addAdapter.getAllItems();

                    //go through each item
                    for(index = 0; index < items.length; index++)
                    {
                        //remember current item
                        MasterAddListActivity.MasterListAdapter.ListItem currentItem = items[index];

                        //if checked state has changed
                        if(currentItem.isChecked != currentItem.startChecked)
                        {
                            //save with updated select state
                            Database.saveSatellite(MasterAddListActivity.this, currentItem.satellite.noradId, currentItem.isChecked);
                        }
                    }

                    setResult(RESULT_OK);
                    MasterAddListActivity.this.finish();
                }
            });
            addButton.setText(R.string.title_set);

            //get owners and categories
            allItems = addAdapter.getAllItems();
            for(index = 0; index < allItems.length; index++)
            {
                //remember current item, owner, and code
                MasterListAdapter.ListItem currentItem = allItems[index];
                UpdateService.MasterOwner currentOwner = new UpdateService.MasterOwner(currentItem.satellite.ownerCode, currentItem.satellite.ownerName);
                String ownerCode = currentOwner.code;

                //if owner is not in the list
                if(ownerCode != null && !usedOwnerCodes.contains(ownerCode))
                {
                    //add it
                    usedOwners.add(currentOwner);
                    usedOwnerCodes.add(ownerCode);
                }

                //go through each category
                for(index2 = 0; index2 < currentItem.categories.size(); index2++)
                {
                    //remember current category
                    String currentCategory = currentItem.categories.get(index2);

                    //if group is not in the list
                    if(!usedCategories.contains(currentCategory))
                    {
                        //add it
                        usedCategories.add(currentCategory);
                    }
                }
            }

            //sort owners and categories
            Collections.sort(usedOwners);
            Collections.sort(usedCategories);

            //setup inputs
            setupInputs(usedOwners, usedCategories);

            //hide search by default
            showButton.performClick();
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
                    final MasterListAdapter.ListItem[] selectedItems = (satelliteListAdapter != null ? satelliteListAdapter.getSelectedItems() : new MasterListAdapter.ListItem[0]);

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

    //Update satellites
    private void updateSatellites(final MasterListAdapter.ListItem[] selectedItems)
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
                MasterListAdapter.ListItem currentItem = selectedItems[index];

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
        if(updateIndex >= 0)
        {
            data.putExtra(ParamTypes.SuccessCount, updateIndex);
        }
        if(updateCount >= 0)
        {
            data.putExtra(ParamTypes.TotalCount, updateCount);
        }
        data.putExtra(ParamTypes.ProgressType, progressType);
        MasterAddListActivity.this.setResult((progressType == Globals.ProgressType.Finished ? RESULT_OK : RESULT_CANCELED), data);
        MasterAddListActivity.this.finish();
    }
    private void sendResult(Intent data, int progressType)
    {
        sendResult(data, -1, -1, progressType);
    }

    //Shows add list
    public static void showList(final Activity context, final boolean askUpdate, boolean confirmInternet)
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
                    showList(context, askUpdate, false);
                }
            });
        }
        else
        {
            Intent intent = new Intent(context, MasterAddListActivity.class);
            intent.putExtra(ParamTypes.ListType, ListType.MasterList);
            intent.putExtra(ParamTypes.AskUpdate, askUpdate);
            intent.putExtra(Settings.PreferenceName.SatelliteSource, Settings.getSatelliteSource(context));
            context.startActivityForResult(intent, BaseInputActivity.RequestCode.MasterAddList);
        }
    }

    //Shows manual orbital input
    public static void showManual(Activity context)
    {
        Intent intent = new Intent(context, ManualOrbitalInputActivity.class);
        context.startActivityForResult(intent, BaseInputActivity.RequestCode.ManualOrbitalInput);
    }
}