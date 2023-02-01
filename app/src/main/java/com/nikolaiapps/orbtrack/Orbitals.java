package com.nikolaiapps.orbtrack;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public abstract class Orbitals
{
    public static abstract class PageType
    {
        static final int Satellites = 0;
        static final int Stars = 1;
        static final int Planets = 2;
        private static final int PageCount = 3;
    }

    //Page list item
    public static class Item extends OrbitalFilterList.Item
    {
        public int color;
        public boolean isVisible;
        public final String text;
        public final long tleDateMs;
        public final Calculations.SatelliteObjectType satelliteObject;

        public Item(Context context, int index, Database.DatabaseSatellite currentSat, boolean canEd, boolean isSel)
        {
            super(context, currentSat, index, canEd, isSel);

            text = currentSat.getName();
            color = currentSat.pathColor;
            isVisible = currentSat.isSelected;
            tleDateMs = currentSat.tleDateMs;
            satelliteObject = Calculations.loadSatellite(currentSat);
        }
    }

    //Page list item holder
    public static class PageListItemHolder extends Selectable.ListItemHolder
    {
        final AppCompatImageView itemImage;
        final TextView itemText;
        final LinearLayout itemLayout;
        final LinearLayout tleAgeLayout;
        final TextView tleAgeText;
        final View tleUnder;
        final BorderButton colorButton;
        final AppCompatButton visibleButton;
        final CircularProgressIndicator progress;

        public PageListItemHolder(View viewItem, int itemLayoutId, int itemImageId, int itemTextId, int tleAgeLayoutId, int tleAgeTextId, int tleAgeUnderId, int colorButtonId, int visibleButtonId, int progressId)
        {
            super(viewItem, -1);
            itemLayout = viewItem.findViewById(itemLayoutId);
            itemImage = viewItem.findViewById(itemImageId);
            itemText = viewItem.findViewById(itemTextId);
            tleAgeLayout = viewItem.findViewById(tleAgeLayoutId);
            tleAgeText = viewItem.findViewById(tleAgeTextId);
            tleUnder = viewItem.findViewById(tleAgeUnderId);
            tleUnder.setTag("keepBg");
            colorButton = viewItem.findViewById(colorButtonId);
            visibleButton = viewItem.findViewById(visibleButtonId);
            progress = viewItem.findViewById(progressId);
        }
    }

    //Page list adapter
    public static class PageListAdapter extends OrbitalFilterList.OrbitalListAdapter
    {
        //Load items task
        private static class LoadItemsTask extends ThreadTask<Object, Void, Void>
        {
            private final OrbitalFilterList.OnLoadItemsListener loadItemsListener;

            public LoadItemsTask(OrbitalFilterList.OnLoadItemsListener loadItemsListener)
            {
                this.loadItemsListener = loadItemsListener;
            }

            @Override
            protected Void doInBackground(Object... params)
            {
                int index;
                int page = (int)params[1];
                boolean simple = (boolean)params[2];
                boolean foundLaunchDate = false;
                Context context = (Context)params[0];
                byte[] orbitalTypes = null;
                Item[] items;
                Database.DatabaseSatellite[] satellites;

                //setup items
                switch(page)
                {
                    case PageType.Satellites:
                    case PageType.Stars:
                    case PageType.Planets:
                        //get conditions
                        switch(page)
                        {
                            case PageType.Satellites:
                                orbitalTypes = new byte[]{Database.OrbitalType.Satellite, Database.OrbitalType.RocketBody, Database.OrbitalType.Debris};
                                break;

                            case PageType.Stars:
                                orbitalTypes = new byte[]{Database.OrbitalType.Star};
                                break;

                            case PageType.Planets:
                                orbitalTypes = new byte[]{Database.OrbitalType.Planet};
                                break;
                        }

                        //get items
                        satellites = Database.getOrbitals(context, orbitalTypes);
                        items = new Item[satellites.length];
                        for(index = 0; index < satellites.length; index++)
                        {
                            Database.DatabaseSatellite currentSat = satellites[index];
                            items[index] = new Item(context, index, currentSat, (page == PageType.Satellites && !simple), false);
                            if(currentSat.launchDateMs > 0)
                            {
                                foundLaunchDate = true;
                            }
                        }
                        break;

                    //invalid page
                    default:
                        items = new Item[0];
                        break;
                }

                //if listeners are set
                if(loadItemsListener != null)
                {
                    //send event
                    loadItemsListener.onLoaded(new ArrayList<>(Arrays.asList(items)), foundLaunchDate);
                }

                //done
                return(null);
            }
        }

        public ChooseColorDialog colorDialog;
        private final boolean simple;
        private final boolean forSetup;
        private final int currentPage;
        private final int columnTitleStringId;
        private final LoadItemsTask loadItems;

        public PageListAdapter(View parentView, int page, int titleStringId, String categoryTitle, boolean isSimple, OrbitalFilterList.OnLoadItemsListener loadItemsListener)
        {
            super(parentView, categoryTitle);

            //set defaults
            loadingItems = true;

            //set page
            currentPage = page;
            columnTitleStringId = titleStringId;
            simple = isSimple;
            forSetup = (columnTitleStringId > 0);
            this.itemsRefID = R.layout.orbitals_item;

            //set load items task
            loadItems = new LoadItemsTask(new OrbitalFilterList.OnLoadItemsListener()
            {
                @Override
                public void onLoaded(ArrayList<OrbitalFilterList.Item> items, boolean foundLaunchDate)
                {
                    displayedItems = items;
                    allItems = items.toArray(new OrbitalFilterList.Item[0]);
                    loadingItems = false;
                    PageListAdapter.this.foundLaunchDate = foundLaunchDate;
                    if(currentContext instanceof Activity)
                    {
                        ((Activity)currentContext).runOnUiThread(new Runnable()
                        {
                            @Override @SuppressLint("NotifyDataSetChanged")
                            public void run()
                            {
                                PageListAdapter.this.notifyDataSetChanged();
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

        @Override
        public void onAttachedToRecyclerView(@NonNull RecyclerView view)
        {
            super.onAttachedToRecyclerView(view);
            loadItems.execute(currentContext, currentPage, simple);
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
                int nonNameVisibility = (currentPage == PageType.Satellites && !forSetup ? View.VISIBLE : View.GONE);
                TextView tleDateText = listColumns.findViewById(R.id.Object_TLE_Age_Text);
                BorderButton colorButton = listColumns.findViewById(R.id.Object_Color_Button);
                AppCompatButton visibleButton = listColumns.findViewById(R.id.Object_Visible_Button);

                ((TextView)listColumns.findViewById(R.id.Object_Item_Text)).setText(columnTitleStringId > 0 ? columnTitleStringId : R.string.title_name);
                visibleButton.setVisibility(View.INVISIBLE);
                tleDateText.setText(R.string.title_tle_age);
                tleDateText.setVisibility(nonNameVisibility);
                listColumns.findViewById(R.id.Object_TLE_Age_Under).setVisibility(View.GONE);
                colorButton.setVisibility(View.GONE);
                listColumns.findViewById(R.id.Object_Color_Button_Replace).setVisibility(View.VISIBLE);
            }

            super.setColumnTitles(listColumns, categoryText, page);
        }

        private void setText(TextView view, String text)
        {
            if(view != null)
            {
                view.setText(text);
            }
        }

        @Override
        protected void onItemNonEditClick(Selectable.ListItem item, int pageNum)
        {
            int offset = 0;
            final Item currentItem = (Item)item;
            final Calculations.OrbitDataType currentOrbit = currentItem.satelliteObject.orbit;
            final Calculations.TLEDataType currentTLE = currentItem.satelliteObject.tle;
            final AppCompatImageButton notifyButton;
            final AppCompatImageButton preview3dButton;
            final AppCompatImageButton infoButton;
            final boolean onSatellite = (currentItem.id > 0);
            final boolean use3dPreview = have3dPreview(currentItem.id);
            final boolean haveContext = (currentContext != null);
            final Drawable itemIcon = (haveContext ? Globals.getOrbitalIcon(currentContext, MainActivity.getObserver(), currentItem.satellite.noradId, currentItem.satellite.orbitalType) : null);
            final ItemDetailDialog detailDialog = new ItemDetailDialog(currentContext, listInflater, currentItem.id, currentItem.text, currentItem.getOwnerCode(), itemIcon, itemDetailButtonClickListener);
            final Resources res = (haveContext ? currentContext.getResources() : null);
            final TextView[] titles;
            final TextView[] texts;
            final AppCompatImageButton[] buttons;

            //if no context
            if(!haveContext)
            {
                //stop
                return;
            }

            //get info displays
            buttons = detailDialog.getItemDetailButtons();
            notifyButton = buttons[DetailButtonType.Notify];
            preview3dButton = buttons[DetailButtonType.Preview3d];
            infoButton = buttons[DetailButtonType.Info];
            detailDialog.setupItemDetailButton(notifyButton, PageListAdapter.this, pageNum, currentItem.id, currentItem, DetailButtonType.Notify);
            if(use3dPreview)
            {
                detailDialog.setupItemDetailButton(preview3dButton, PageListAdapter.this, pageNum, currentItem.id, currentItem, DetailButtonType.Preview3d);
            }
            detailDialog.setupItemDetailButton(infoButton, PageListAdapter.this, pageNum, currentItem.id, currentItem, DetailButtonType.Info);

            //if on a satellite
            if(onSatellite)
            {
                //
                //get displays
                //
                titles = detailDialog.getItemDetailTitles();
                texts = detailDialog.getItemDetailTexts();
                setText(titles[offset], res.getString(R.string.title_owner) + ":");
                detailDialog.hideVerticalItemDetailDivider(offset);
                TextView ownerText = texts[offset++];
                TableRow.LayoutParams viewParams = (ownerText != null ? (TableRow.LayoutParams)ownerText.getLayoutParams() : null);

                if(viewParams != null)
                {
                    viewParams.span = 4;
                    ownerText.setLayoutParams(viewParams);
                }
                offset++;

                setText(titles[offset], res.getString(R.string.title_norad) + " " + res.getString(R.string.title_id) + ":");
                TextView noradText = texts[offset++];

                setText(titles[offset], res.getString(R.string.abbrev_international) + " " + res.getString(R.string.title_code) + ":");
                TextView internationalText = texts[offset++];

                setText(titles[offset], res.getString(R.string.title_launch_date) + ":");
                TextView launchDateText = texts[offset++];

                setText(titles[offset], res.getString(R.string.title_perigee) + ":");
                TextView perigeeText = texts[offset++];

                setText(titles[offset], res.getString(R.string.title_launch_number) + ":");
                TextView launchNumberText = texts[offset++];

                setText(titles[offset], res.getString(R.string.title_apogee) + ":");
                TextView apogeeText = texts[offset++];

                setText(titles[offset], res.getString(R.string.title_inclination) + ":");
                TextView inclinationText = texts[offset++];

                setText(titles[offset], res.getString(R.string.title_period) + ":");
                TextView periodText = texts[offset++];

                setText(titles[offset], res.getString(R.string.title_semi_major_axis) + ":");
                TextView majorText = texts[offset++];

                //update displays
                setText(ownerText, currentItem.getOwner(currentContext));
                setText(noradText, String.valueOf(currentTLE.satelliteNum));
                setText(internationalText, currentTLE.internationalCode);
                setText(perigeeText, Globals.getKmUnitValueString(currentOrbit.perigee) + " " + Globals.getKmLabel(res));
                setText(apogeeText, Globals.getKmUnitValueString(currentOrbit.apogee) + " " + Globals.getKmLabel(res));
                setText(inclinationText, Globals.getNumberString(currentTLE.inclinationDeg));
                setText(periodText, Globals.getNumberString(currentOrbit.periodMinutes) + " " + res.getString(R.string.abbrev_minutes_lower));
                setText(majorText, Globals.getKmUnitValueString(currentOrbit.semiMajorAxisKm) + " " + Globals.getKmLabel(res));
                setText(launchDateText, String.valueOf(currentTLE.launchYear));
                setText(launchNumberText, String.valueOf(currentTLE.launchNum));
            }
            //else if info button exists
            else if(infoButton != null)
            {
                //show info display
                infoButton.performClick();
            }

            //update display visibilities
            detailDialog.showItemDetailRows(offset);
            if(notifyButton != null)
            {
                notifyButton.setVisibility(!forSetup ? View.VISIBLE : View.GONE);
            }
            if(preview3dButton != null)
            {
                preview3dButton.setVisibility(use3dPreview ? View.VISIBLE : View.GONE);
            }
            if(infoButton != null)
            {
                infoButton.setVisibility(View.VISIBLE);
            }
            detailDialog.show();
        }

        @Override @NonNull
        public PageListItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
        {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(this.itemsRefID, parent, false);
            PageListItemHolder itemHolder = new PageListItemHolder(itemView, R.id.Object_Item_Layout, R.id.Object_Item_Image, R.id.Object_Item_Text, R.id.Object_TLE_Age_Layout, R.id.Object_TLE_Age_Text, R.id.Object_TLE_Age_Under, R.id.Object_Color_Button, R.id.Object_Visible_Button, R.id.Object_Progress);

            setViewClickListeners(itemView, itemHolder);
            return(itemHolder);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position)
        {
            boolean onSatellites = (currentPage == PageType.Satellites);
            final Resources res = (currentContext != null ? currentContext.getResources() : null);
            final PageListItemHolder itemHolder = (PageListItemHolder)holder;
            final OrbitalFilterList.Item filterItem = (position < displayedItems.size() ? displayedItems.get(position) : null);
            final Item currentItem = (loadingItems || !(filterItem instanceof Item) ? new Item(currentContext, 0, new Database.DatabaseSatellite("", Universe.IDs.Invalid, "", Globals.UNKNOWN_DATE_MS, Database.OrbitalType.Satellite), false, false) : (Item)filterItem);
            double ageDays = (System.currentTimeMillis() - currentItem.tleDateMs) / Calculations.MsPerDay;
            String dayText = Globals.getNumberString(ageDays, 1) + " " + (res != null ? res.getString(R.string.title_days) : "");
            Drawable itemIcon;

            //set visibility
            itemHolder.itemLayout.setVisibility(loadingItems ? View.GONE : View.VISIBLE);
            itemHolder.progress.setVisibility(loadingItems ? View.VISIBLE : View.GONE);

            //if not loading items
            if(!loadingItems)
            {
                //set displays
                itemIcon = Globals.getOrbitalIcon(currentContext, MainActivity.getObserver(), currentItem.satellite.noradId, currentItem.satellite.orbitalType);
                itemHolder.itemImage.setImageDrawable(itemIcon);
                itemHolder.itemText.setText(currentItem.text);
                if(onSatellites)
                {
                    itemHolder.tleAgeText.setText(dayText);
                    if(ageDays <= 2)
                    {
                        itemHolder.tleUnder.setVisibility(View.GONE);
                    }
                    else
                    {
                        itemHolder.tleUnder.setBackgroundColor(ageDays >= 14 ? Color.RED : ageDays >= 7 ? 0xFFFFA500 : Color.YELLOW);
                        itemHolder.tleUnder.setVisibility(View.VISIBLE);
                        itemHolder.tleAgeLayout.setVisibility(View.VISIBLE);
                    }
                }
                else
                {
                    itemHolder.tleAgeLayout.setVisibility(View.GONE);
                }
                itemHolder.colorButton.setBackgroundColor(currentItem.color);
                itemHolder.visibleButton.setBackgroundDrawable(Globals.getVisibleIcon(currentContext, currentItem.isVisible));

                //set events
                itemHolder.colorButton.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        colorDialog = new ChooseColorDialog(currentContext, currentItem.color);
                        colorDialog.setOnColorSelectedListener(new ChooseColorDialog.OnColorSelectedListener()
                        {
                            @Override
                            public void onColorSelected(int color)
                            {
                                int index;
                                Database.SatelliteData[] currentSatellites = MainActivity.getSatellites();

                                //get and save orbital in database
                                currentItem.color = color;
                                itemHolder.colorButton.setBackgroundColor(color);
                                Database.saveSatellitePathColor(currentContext, currentItem.id, color);

                                //go through currently selected satellites
                                for(index = 0; index < currentSatellites.length; index++)
                                {
                                    //remember current satellite
                                    Database.SatelliteData currentSatellite = currentSatellites[index];

                                    //if current satellite has a database and matches selected satellite
                                    if(currentSatellite.database != null && currentSatellite.getSatelliteNum() == currentItem.satelliteObject.getSatelliteNum())
                                    {
                                        //update selected satellite
                                        currentSatellite.database.pathColor = color;
                                    }
                                }
                            }
                        });
                        colorDialog.setIcon(itemIcon);
                        colorDialog.setTitle(res != null ? (res.getString(R.string.title_select) + " " + currentItem.text + " " + res.getString(R.string.title_color)) : currentItem.text);
                        colorDialog.show(currentContext);
                    }
                });
                itemHolder.visibleButton.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        int noradId = currentItem.id;
                        Database.DatabaseSatellite currentOrbital;

                        //update visibility and save orbital in database
                        currentItem.isVisible = !currentItem.isVisible;
                        itemHolder.visibleButton.setBackgroundDrawable(Globals.getVisibleIcon(currentContext, currentItem.isVisible));
                        Database.saveSatelliteVisible(currentContext, noradId, currentItem.isVisible);

                        //if item is visible again
                        if(currentItem.isVisible)
                        {
                            //if not a valid TLE
                            currentOrbital = Database.getOrbital(currentContext, noradId);
                            if(currentOrbital != null && !currentOrbital.tleIsAccurate)
                            {
                                //make sure not in excluded old norad IDs
                                MainActivity.updateExcludedOldNoradIds(noradId, false);
                            }
                        }

                        //update current usage
                        MainActivity.loadOrbitals(currentContext, holder.itemView);
                    }
                });
            }

            //set background
            setItemBackground(itemHolder.itemView, currentItem.isSelected);
        }
    }

    //Page view
    public static class Page extends Selectable.ListFragment
    {
        private final boolean simple;
        private final String categoryTitle;
        private UpdateReceiver updateReceiver;
        private UpdateReceiver saveReceiver;
        private Globals.PendingFile pendingSaveFile;
        private PageListAdapter listAdapter;
        private LinearLayout searchLayout;
        private TableLayout searchGroup;
        private IconSpinner ownerList;
        private IconSpinner groupList;
        private IconSpinner ageList;
        private EditText searchText;
        private TableRow ageRow;
        private AppCompatImageButton showButton;
        public static WeakReference<MultiProgressDialog> updateProgressReference;

        public Page(String title, boolean simple)
        {
            super();
            this.simple = simple;
            this.categoryTitle = title;
        }
        public Page()
        {
            super();
            this.simple = false;
            this.categoryTitle = null;
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
        {
            Context context = Page.this.getContext();
            int group = this.getGroupParam();
            int page = this.getPageParam();
            final boolean onSatellites = (page == PageType.Satellites);
            View newView;
            Intent serviceIntent;
            ArrayList<Database.DatabaseSatellite> satelliteList;

            //create adapter
            listAdapter = new PageListAdapter(listParentView, page, -1, categoryTitle, simple, new OrbitalFilterList.OnLoadItemsListener()
            {
                @Override
                public void onLoaded(ArrayList<OrbitalFilterList.Item> items, boolean foundLaunchDate)
                {
                    //if for satellites
                    if(onSatellites)
                    {
                        //get used data
                        final OrbitalFilterList.OrbitalListAdapter.UsedData used = listAdapter.getUsed();

                        if(context instanceof Activity)
                        {
                            ((Activity) context).runOnUiThread(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    //setup inputs and collapse
                                    searchLayout.setVisibility(simple ? View.GONE : View.VISIBLE);
                                    listAdapter.setupInputs(searchGroup, ownerList, groupList, ageList, ageRow, searchText, showButton, used.owners, used.categories, listAdapter.getHasLaunchDates());
                                    listAdapter.showSearchInputs(false);
                                }
                            });
                        }
                    }
                }
            });

            //create view
            newView = this.onCreateView(inflater, container, listAdapter, group, page);
            searchLayout = newView.findViewById(R.id.Orbital_Search_Layout);
            searchGroup = newView.findViewById(R.id.Orbital_Search_Table);
            ownerList = newView.findViewById(R.id.Orbital_Search_Owner_List);
            groupList = newView.findViewById(R.id.Orbital_Search_Group_List);
            ageList = newView.findViewById(R.id.Orbital_Search_Age_List);
            searchText = newView.findViewById(R.id.Orbital_Search_Text);
            ageRow = newView.findViewById(R.id.Orbital_Search_Age_Row);
            showButton = newView.findViewById(R.id.Orbital_Search_Show_Button);

            //if for satellites
            if(onSatellites)
            {
                //if update running
                serviceIntent = UpdateService.getIntent(UpdateService.UpdateType.UpdateSatellites);
                if(serviceIntent != null)
                {
                    //set selected items
                    satelliteList = Database.getSatelliteCacheData(context);

                    //go through each satellite
                    for(Database.DatabaseSatellite currentSatellite : satelliteList)
                    {
                        //select it
                        setItemSelected(getPosition(currentSatellite.noradId), true);
                    }

                    //run with existing
                    runTaskSelectedItems(UpdateService.UpdateType.UpdateSatellites, false);
                }
            }

            //set change listeners
            PageAdapter.setChangeListeners(page, new OnOrientationChangedListener()
            {
                @Override
                public void orientationChanged()
                {
                    //update display
                    if(listAdapter.colorDialog != null)
                    {
                        listAdapter.colorDialog.reload();
                    }
                }
            }, createOnPreview3dChangedListener(listAdapter), createOnInformationChangedListener(listAdapter));

            //return view
            return(newView);
        }

        @Override
        public void onDestroy()
        {
            Context context = this.getContext();

            //if context is set
            if(context != null)
            {
                //if receivers are set
                if(updateReceiver != null)
                {
                    //remove it
                    updateReceiver.unregister();
                }
                if(saveReceiver != null)
                {
                    //remove it
                    saveReceiver.unregister();
                }
            }

            super.onDestroy();
        }

        @Override
        public void onConfigurationChanged(@NonNull Configuration newConfig)
        {
            super.onConfigurationChanged(newConfig);

            int index;

            //update displays
            for(index =  0; index < PageType.PageCount; index++)
            {
                //if listener is set
                if(PageAdapter.orientationChangedListeners[index] != null)
                {
                    //call listener
                    PageAdapter.orientationChangedListeners[index].orientationChanged();
                }
            }
        }

        @Override
        protected boolean setupActionModeItems(MenuItem all, MenuItem none, MenuItem edit, MenuItem delete, MenuItem save, MenuItem sync)
        {
            boolean onEditSatellitesNoUpdate = (inEditMode && pageNum == PageType.Satellites && !UpdateService.updatingSatellites());

            //set visibility
            all.setVisible(onEditSatellitesNoUpdate);
            none.setVisible(onEditSatellitesNoUpdate);
            edit.setVisible(false);
            delete.setVisible(onEditSatellitesNoUpdate);
            save.setVisible(onEditSatellitesNoUpdate);
            sync.setVisible(onEditSatellitesNoUpdate);

            return(true);
        }

        @Override
        protected void onActionModeSelect(boolean all)
        {
            //select/deselect items
            setItemsSelected(all);
        }

        @Override
        protected void onActionModeDelete()
        {
            //show deletion dialog
            showConfirmDeleteDialog(R.plurals.title_satellites);
        }

        @Override
        protected int onActionModeConfirmDelete()
        {
            int deleteCount = 0;
            Context context = listParentView.getContext();
            Selectable.ListItem[] items = selectedItems.toArray(new Selectable.ListItem[0]);

            //go through selected items and close database
            for(Selectable.ListItem currentItem : items)
            {
                //delete satellite from database
                if(Database.deleteSatellite(context, currentItem.id))
                {
                    //update count
                    deleteCount++;
                }
            }

            //return delete count
            return(deleteCount);
        }

        @Override
        protected void onActionModeSave()
        {
            Resources res = this.getResources();

            Globals.showSelectDialog(this.getContext(), res.getString(R.string.title_save_format), AddSelectListAdapter.SelectType.SaveAs, new AddSelectListAdapter.OnItemClickListener()
            {
                @Override
                public void onItemClick(final int which)
                {
                    boolean isBackup = which == Globals.FileType.Backup;
                    String fileType = (isBackup ? Globals.FileExtensionType.JSON : Globals.FileExtensionType.TLE);
                    final Activity activity = Page.this.getActivity();
                    Resources res;

                    //if activity is set
                    if(activity != null)
                    {
                        //get resources
                        res = activity.getResources();

                        //show dialog
                        new EditValuesDialog(activity, new EditValuesDialog.OnSaveListener()
                        {
                            @Override
                            public void onSave(EditValuesDialog dialog, int itemIndex, int id, String textValue, String text2Value, double number1, double number2, double number3, String listValue, String list2Value, long dateValue, int color1, int color2, boolean visible)
                            {
                                //remember file source
                                int fileSourceType = Globals.getFileSource(activity, list2Value);

                                //set pending file
                                setSaveFileData(null, textValue, listValue, which, fileSourceType);

                                //if for others
                                if(fileSourceType == AddSelectListAdapter.FileSourceType.Others)
                                {
                                    ActivityResultLauncher<Intent> otherSaveLauncher = null;

                                    //if the main activity
                                    if(activity instanceof MainActivity)
                                    {
                                        //remember main activity
                                        MainActivity mainActivity = (MainActivity)activity;

                                        //get launcher and set pending file in activity
                                        otherSaveLauncher = mainActivity.getResultLauncher(BaseInputActivity.RequestCode.OthersSave);
                                        mainActivity.setSaveFileData(null, textValue, listValue, which, fileSourceType);
                                    }

                                    //get folder
                                    Globals.showOthersFolderSelect(otherSaveLauncher);

                                    //don't call on dismiss listener (prevent deselecting items too soon)
                                    dialog.setOnDismissListener(null);
                                }
                                else
                                {
                                    //save satellites
                                    runTaskSelectedItems(UpdateService.UpdateType.SaveFile);
                                }
                            }
                        }, new EditValuesDialog.OnDismissListener()
                        {
                            @Override
                            public void onDismiss(EditValuesDialog dialog, int saveCount)
                            {
                                //make sure no items are selected
                                setItemsSelected(false);
                            }
                        }).getFileLocation(res.getString(R.string.title_satellites_save_file), new int[]{0}, new String[]{res.getString(R.string.title_satellites) + (isBackup ? (" " + res.getString(R.string.title_backup)) : "")}, new String[]{fileType}, new String[]{fileType}, new String[]{res.getString(R.string.title_downloads)});
                    }
                }
            });
        }

        @Override
        protected void onActionModeSync()
        {
            int count = selectedItems.size();
            Resources res = this.getResources();
            String items = res.getQuantityString(R.plurals.title_satellites, count) + " (" + count + ")";

            //show dialog
            Globals.showConfirmDialog(this.getContext(), res.getString(R.string.title_update_selected) + " " + items + "?", null, res.getString(R.string.title_ok), res.getString(R.string.title_cancel), true, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    runTaskSelectedItems(UpdateService.UpdateType.UpdateSatellites);
                }
            }, null, null);
        }

        @Override
        protected void onUpdateStarted()
        {
            setItemClicksEnabled(false);
            refreshActionMode();
        }

        @Override
        protected void onUpdateFinished(boolean success)
        {
            refreshActionMode();
            setItemClicksEnabled(true);
        }

        //Gets update progress dialog
        public static MultiProgressDialog getUpdateProgress()
        {
            return(updateProgressReference != null ? updateProgressReference.get() : null);
        }

        //Creates an update receiver
        private UpdateReceiver createLocalUpdateReceiver(UpdateReceiver oldReceiver, byte updateType, final ArrayList<Database.DatabaseSatellite> satellites, final ArrayList<Integer> listIndexes, boolean showProgress)
        {
            Context context = this.getContext();
            Resources res = this.getResources();
            MultiProgressDialog taskProgress;
            boolean savingFile = (updateType == UpdateService.UpdateType.SaveFile);

            //if old receiver is set
            if(oldReceiver != null)
            {
                //remove it
                oldReceiver.unregister();
            }

            //create progress if using
            taskProgress = (showProgress ? Globals.createProgressDialog(context) : null);
            updateProgressReference = new WeakReference<>(taskProgress);
            if(showProgress)
            {
                Globals.setUpdateDialog(taskProgress, res.getString(savingFile ? R.string.title_saving : R.string.title_updating) + " " + res.getQuantityString(R.plurals.title_satellites, 2), updateType);
                taskProgress.show();
            }

            //create receiver
            UpdateReceiver receiver = new UpdateReceiver()
            {
                @Override
                protected View getParentView()
                {
                    return(Page.this.listParentView);
                }

                @Override
                protected void onProgress(long updateValue, long updateCount, String section)
                {
                    //if progress exists
                    if(taskProgress != null && (updateValue - 1) < satellites.size())
                    {
                        taskProgress.setMessage(res.getQuantityString(R.plurals.title_space_of_space, (int)updateCount, updateValue, updateCount) + (savingFile ? "" : (" (" + satellites.get((int)updateValue - 1).getName() + ")")));
                        taskProgress.setProgress(updateValue, updateCount);
                    }
                }

                @Override
                protected void onGeneralUpdate(int progressType, byte updateType, boolean ended, String section, int index, int count, File usedFile)
                {
                    boolean forDropbox = (section != null && section.equals(Globals.FileLocationType.Dropbox));
                    boolean forGoogleDrive = (section != null && section.equals(Globals.FileLocationType.GoogleDrive));

                    //handle based on progress type
                    switch(progressType)
                    {
                        case Globals.ProgressType.Finished:
                        case Globals.ProgressType.Cancelled:
                        case Globals.ProgressType.Denied:
                        case Globals.ProgressType.Failed:
                            //if were saving a file
                            if(savingFile)
                            {
                                //deselect items
                                setItemsSelected(false);
                            }

                            //close progress
                            if(taskProgress != null)
                            {
                                try
                                {
                                    taskProgress.dismiss();
                                }
                                catch(Exception ex)
                                {
                                    //do nothing
                                }
                            }

                            //show status
                            switch(progressType)
                            {
                                case Globals.ProgressType.Denied:
                                    //show denied
                                    Globals.showSnackBar(Page.this.listParentView, res.getString(R.string.text_login_failed), true);

                                    //if try to update satellites
                                    if(updateType == UpdateService.UpdateType.UpdateSatellites)
                                    {
                                        //if activity is set
                                        final Activity currentActivity = Page.this.getActivity();
                                        if(currentActivity != null)
                                        {
                                            //show login
                                            Globals.showAccountLogin(currentActivity, Globals.AccountType.SpaceTrack, updateType, new Globals.WebPageListener()
                                            {
                                                @Override
                                                public void onResult(Globals.WebPageData pageData, boolean success)
                                                {
                                                    //if success or attempted to login
                                                    if(success || pageData != null)
                                                    {
                                                        //try again
                                                        currentActivity.runOnUiThread(new Runnable()
                                                        {
                                                            @Override
                                                            public void run()
                                                            {
                                                                runTaskSelectedItems(UpdateService.UpdateType.UpdateSatellites);
                                                            }
                                                        });
                                                    }
                                                }
                                            });
                                        }
                                    }
                                    break;

                                case Globals.ProgressType.Failed:
                                    //show failed
                                    Globals.showSnackBar(Page.this.listParentView, res.getString(R.string.title_failed) + " " + res.getString(savingFile ? R.string.title_saving : R.string.title_updating), UpdateService.getError(updateType), true, true);
                                    break;

                                case Globals.ProgressType.Finished:
                                    //if for Dropbox or Google Drive
                                    if(forDropbox || forGoogleDrive)
                                    {
                                        //if activity exists
                                        Activity currentActivity = Page.this.getActivity();
                                        if(currentActivity instanceof MainActivity)
                                        {
                                            //get result launcher
                                            ActivityResultLauncher<Intent> launcher = ((MainActivity)currentActivity).getResultLauncher();

                                            //save file
                                            if(forGoogleDrive)
                                            {
                                                GoogleDriveAccess.start(currentActivity, launcher, usedFile, count, true);
                                            }
                                            else
                                            {
                                                DropboxAccess.start(currentActivity, launcher, usedFile, count, true);
                                            }
                                        }
                                    }
                                    else
                                    {
                                        //show success
                                        Globals.showSnackBar(Page.this.listParentView, res.getQuantityString(savingFile ? R.plurals.title_satellites_saved : R.plurals.title_satellites_updated, count, count) + (savingFile || (index == count) ? "" : (" (" + res.getQuantityString(R.plurals.title_space_of_space, count, index, count) + ")")), (count < 1));
                                    }
                                    break;
                            }

                            //if not saving file and weren't denied
                            //note: updating on denied causes wrong page instance on login retry
                            if(!savingFile && progressType != Globals.ProgressType.Denied)
                            {
                                //send event
                                onUpdateNeeded();
                            }
                            break;
                    }
                }

                @Override
                protected void onDownloadUpdate(int progressType, byte updateType, long updateValue, long updateCount)
                {
                    long updateIndex = updateValue - 1;

                    //if success, not saving a file, and a valid index
                    if(progressType == Globals.ProgressType.Success && updateType != UpdateService.UpdateType.SaveFile && updateIndex >= 0 && updateIndex < listIndexes.size())
                    {
                        //deselect item
                        setItemSelected(listIndexes.get((int)updateIndex), false);
                    }
                }
            };

            //register and receiver
            receiver.register(context);
            return(receiver);
        }

        //Sets save file data
        public void setSaveFileData(Uri outUri, String fileName, String fileExtension, int fileType, int fileSourceType)
        {
            pendingSaveFile = new Globals.PendingFile(outUri, fileName, fileExtension, fileType, fileSourceType);
        }

        //Run task on selected items
        public void runTaskSelectedItems(final byte updateType, boolean confirmInternet)
        {
            int index;
            int count;
            int fileSourceType;
            FragmentActivity activity = this.getActivity();
            boolean showProgress = true;
            boolean needDeselect = false;
            boolean askInternet = (confirmInternet && Globals.shouldAskInternetConnection(activity));
            Resources res = this.getResources();
            ArrayList<Integer> listIndexes = new ArrayList<>(0);
            ArrayList<Database.DatabaseSatellite> satellites = new ArrayList<>(0);
            int[] satelliteIds;

            //if no selected items
            if(selectedItems.size() < 1)
            {
                //running on all items
                setItemsSelected(true);
                needDeselect = true;
            }
            satelliteIds = new int[selectedItems.size()];

            //go through each item
            for(index = 0; index < selectedItems.size(); index++)
            {
                //get current item
                Item currentItem = (Item)selectedItems.get(index);

                //if on satellites page
                if(pageNum == PageType.Satellites)
                {
                    //add satellite ID and list index
                    satelliteIds[index] = currentItem.id;
                    listIndexes.add(currentItem.listIndex);
                }
            }

            //get all satellites
            satellites.addAll(Arrays.asList(Database.getOrbitals(activity, satelliteIds)));

            //if need to deselect items
            if(needDeselect)
            {
                //deselect all items
                setItemsSelected(false);
            }

            //if updating satellites
            count = satellites.size();
            if(count > 0)
            {
                //get file source
                fileSourceType = (pendingSaveFile != null ? pendingSaveFile.fileSourceType : Globals.FileSource.SDCard);

                //handle display
                switch(updateType)
                {
                    case UpdateService.UpdateType.SaveFile:
                        //show progress if -have permission for file SDCard- or -not asking about internet for Dropbox or GoogleDrive-
                        showProgress = (fileSourceType == Globals.FileSource.SDCard && Globals.haveWritePermission(activity)) || (!askInternet && (fileSourceType == Globals.FileSource.Dropbox || fileSourceType == Globals.FileSource.GoogleDrive)) || (fileSourceType == AddSelectListAdapter.FileSourceType.Others);
                        break;

                    case UpdateService.UpdateType.UpdateSatellites:
                        //show progress if not asking about internet
                        showProgress = !askInternet;
                        break;
                }

                //handle task
                switch(updateType)
                {
                    case UpdateService.UpdateType.SaveFile:
                        //handle based on file source
                        switch(fileSourceType)
                        {
                            case Globals.FileSource.SDCard:
                                //if have write permission
                                if(Globals.haveWritePermission(activity))
                                {
                                    //save file if not already
                                    saveReceiver = createLocalUpdateReceiver(saveReceiver, updateType, satellites, listIndexes, showProgress);
                                    if(!UpdateService.savingFile() && activity != null)
                                    {
                                       //save satellites
                                       UpdateService.saveFile(activity, satellites, pendingSaveFile);
                                    }
                                }
                                //else if don't have permission but can ask
                                else if(Globals.canAskWritePermission)
                                {
                                    //if activity is set
                                    if(activity != null)
                                    {
                                        //ask permission
                                        Globals.askWritePermission(activity, false);
                                    }
                                }
                                else
                                {
                                    //show denied and don't ask again
                                    Globals.showDenied(listParentView, res.getString(R.string.desc_permission_write_external_storage_deny));
                                    Globals.canAskWritePermission = false;
                                }
                                break;

                            case Globals.FileSource.GoogleDrive:
                            case Globals.FileSource.Dropbox:
                            case Globals.FileSource.Others:
                                //if asking about internet
                                if(askInternet)
                                {
                                    //get confirmation
                                    Globals.showConfirmInternetDialog(activity, new DialogInterface.OnClickListener()
                                    {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which)
                                        {
                                            //don't ask this time
                                            runTaskSelectedItems(updateType, false);
                                        }
                                    });
                                }
                                else
                                {
                                    //save file if not already
                                    saveReceiver = createLocalUpdateReceiver(saveReceiver, updateType, satellites, listIndexes, showProgress);
                                    if(!UpdateService.savingFile() && activity != null)
                                    {
                                        //save satellites
                                        UpdateService.saveFile(activity, satellites, pendingSaveFile);
                                    }
                                }
                                break;
                        }
                        break;

                    case UpdateService.UpdateType.UpdateSatellites:
                        //if asking about internet
                        if(askInternet)
                        {
                            //get confirmation
                            Globals.showConfirmInternetDialog(activity, new DialogInterface.OnClickListener()
                            {
                                @Override
                                public void onClick(DialogInterface dialog, int which)
                                {
                                    //don't ask this time
                                    runTaskSelectedItems(updateType, false);
                                }
                            });
                        }
                        else
                        {
                            //update satellites
                            updateReceiver = createLocalUpdateReceiver(updateReceiver, updateType, satellites, listIndexes, showProgress);
                            if(!UpdateService.updatingSatellites())
                            {
                                UpdateService.updateSatellites(activity, res.getQuantityString(R.plurals.title_satellites_updating, count), satellites, false);
                            }
                            else
                            {
                                UpdateService.setNotificationVisible(updateType, false);
                            }
                        }
                        break;
                }
            }
        }
        public void runTaskSelectedItems(final byte updateType)
        {
            runTaskSelectedItems(updateType, true);
        }
    }

    //Page adapter
    public static class PageAdapter extends Selectable.ListFragmentAdapter
    {
        private static final Selectable.ListFragment.OnOrientationChangedListener[] orientationChangedListeners = new Selectable.ListFragment.OnOrientationChangedListener[PageType.PageCount];
        private static final Selectable.ListFragment.OnPreview3dChangedListener[] preview3dChangedListeners = new Selectable.ListFragment.OnPreview3dChangedListener[PageType.PageCount];
        private static final Selectable.ListFragment.OnInformationChangedListener[] informationChangedListeners = new Selectable.ListFragment.OnInformationChangedListener[PageType.PageCount];

        public PageAdapter(FragmentManager fm, View parentView, Selectable.ListFragment.OnItemDetailButtonClickListener detailListener, Selectable.ListFragment.OnAdapterSetListener adapterListener, Selectable.ListFragment.OnUpdateNeededListener updateListener, Selectable.ListFragment.OnPageResumeListener resumeListener)
        {
            super(fm, parentView, null, updateListener, null,  detailListener, adapterListener, null, resumeListener, MainActivity.Groups.Orbitals, null);
        }

        @Override
        public @NonNull Fragment getItem(int position)
        {
            return(this.getItem(group, position, -1, new Page()));
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
                case PageType.Satellites:
                    return(res.getString(R.string.title_satellites));

                case PageType.Stars:
                    return(res.getString(R.string.title_stars));

                case PageType.Planets:
                    return(res.getString(R.string.title_moon_and_planets));

                default:
                    return(res.getString(R.string.title_invalid));
            }
        }

        //Gets if items are loading on page
        public boolean isPageLoadingItems(ViewGroup container, int pageNum)
        {
            Page page = (Page)getPage(container, pageNum);
            return(page != null && page.isListLoadingItems());
        }

        //Gets count of items on page
        public int getPageItemCount(ViewGroup container, int pageNum)
        {
            Page page = (Page)getPage(container, pageNum);
            return(page != null ? page.getListItemCount() : 0);
        }

        //Saves file for selected items
        public void saveFileSelectedItems(ViewGroup container, int pageNum, Uri outUri, String fileName, String fileExtension, int fileType, int fileSourceType)
        {
            Page page = (Page)getPage(container, pageNum);
            if(page != null)
            {
                //if file name is set
                if(fileName != null)
                {
                    //save values
                    page.setSaveFileData(outUri, fileName, fileExtension, fileType, fileSourceType);
                }

                //save file
                page.runTaskSelectedItems(UpdateService.UpdateType.SaveFile);
            }
        }

        //Updates selected items
        public void updateSelectedItems(ViewGroup container, int pageNum)
        {
            Page page = (Page)getPage(container, pageNum);
            if(page != null)
            {
                page.runTaskSelectedItems(UpdateService.UpdateType.UpdateSatellites);
            }
        }

        //Sets information changed listener for the given page
        public static void setChangeListeners(int position, Selectable.ListFragment.OnOrientationChangedListener orientationChangedListener, Selectable.ListFragment.OnPreview3dChangedListener preview3dChangedListener, Selectable.ListFragment.OnInformationChangedListener informationChangedListener)
        {
            //if a valid page
            if(position < PageType.PageCount)
            {
                //set listeners
                orientationChangedListeners[position] = orientationChangedListener;
                preview3dChangedListeners[position] = preview3dChangedListener;
                informationChangedListeners[position] = informationChangedListener;
            }
        }

        //Call preview 3d changed listener for the given page
        public static void notifyPreview3dChanged(int position, int noradId)
        {
            //if a valid page and listener exists
            if(position < Current.PageType.PageCount && preview3dChangedListeners[position] != null)
            {
                //call listener
                preview3dChangedListeners[position].preview3dChanged(noradId);
            }
        }

        //Calls information changed listener for the given page
        public static void notifyInformationChanged(int position, Spanned text)
        {
            //if a valid page and listener exists
            if(position < PageType.PageCount && informationChangedListeners[position] != null)
            {
                //call listener
                informationChangedListeners[position].informationChanged(text);
            }
        }
    }

    //Shows SD card file browser
    public static void showSDCardFileBrowser(Activity context, ActivityResultLauncher<Intent> launcher, View parentView)
    {
        Resources res = context.getResources();

        //if have permission to read from external storage
        if(Globals.haveReadPermission(context))
        {
            //show file browser
            Intent intent = new Intent(context, SDCardBrowserActivity.class);
            Globals.startActivityForResult(launcher, intent, BaseInputActivity.RequestCode.SDCardOpenItem);
        }
        //else if don't have permission but can ask
        else if(Globals.canAskReadPermission)
        {
            Globals.askReadPermission(context, false);
        }
        else
        {
            //show denied and don't ask again
            Globals.showDenied(parentView, res.getString(R.string.desc_permission_read_external_storage_deny));
            Globals.canAskReadPermission = false;
        }
    }

    //Shows google drive file browser
    public static void showGoogleDriveFileBrowser(final Activity context, ActivityResultLauncher<Intent> launcher, boolean confirmInternet)
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
                    showGoogleDriveFileBrowser(context, launcher, false);
                }
            });
        }
        else
        {
            GoogleDriveAccess.start(context, launcher, false);
        }
    }

    //Shows Dropbox browser
    private static void showDropboxBrowser(final Activity context, ActivityResultLauncher<Intent> launcher, boolean confirmInternet)
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
                    showDropboxBrowser(context, launcher, false);
                }
            });
        }
        else
        {
            DropboxAccess.start(context, launcher, false);
        }
    }

    //Shows other file browsers
    public static void showOthersFileBrowser(Activity context, ActivityResultLauncher<Intent> launcher)
    {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setType("*/*");
        intent = Intent.createChooser(intent, context.getString(R.string.title_select_file_or_files));
        Globals.startActivityForResult(launcher, intent, BaseInputActivity.RequestCode.OthersOpenItem);
    }

    //Shows an add dialog
    public static void showAddDialog(final Activity context, ActivityResultLauncher<Intent> resultLauncher, ActivityResultLauncher<Intent> otherOpenLauncher, final View parentView, final boolean askForce)
    {
        final Resources res = context.getResources();

        //show SatelliteSourceType dialog
        Globals.showSelectDialog(context, res.getString(R.string.title_select_source), AddSelectListAdapter.SelectType.SatelliteSource, new AddSelectListAdapter.OnItemClickListener()
        {
            @Override
            public void onItemClick(int which)
            {
                //handle based on source
                switch(which)
                {
                    case AddSelectListAdapter.SatelliteSourceType.Online:
                        MasterAddListActivity.showList(context, resultLauncher, askForce, true);
                        break;

                    case AddSelectListAdapter.SatelliteSourceType.File:
                        //show FileSourceType dialog
                        Globals.showSelectDialog(context, res.getString(R.string.title_file_select_source), AddSelectListAdapter.SelectType.FileSource, new AddSelectListAdapter.OnItemClickListener()
                        {
                            @Override
                            public void onItemClick(int which)
                            {
                                //handle based on source
                                switch(which)
                                {
                                    case AddSelectListAdapter.FileSourceType.SDCard:
                                        showSDCardFileBrowser(context, resultLauncher, parentView);
                                        break;

                                    case AddSelectListAdapter.FileSourceType.Dropbox:
                                        showDropboxBrowser(context, resultLauncher, true);
                                        break;

                                    case AddSelectListAdapter.FileSourceType.GoogleDrive:
                                        showGoogleDriveFileBrowser(context, resultLauncher, true);
                                        break;

                                    case AddSelectListAdapter.FileSourceType.Others:
                                        showOthersFileBrowser(context, otherOpenLauncher);
                                        break;
                                }
                            }
                        });
                        break;

                    case AddSelectListAdapter.SatelliteSourceType.Manual:
                        MasterAddListActivity.showManual(context, resultLauncher);
                        break;
                }
            }
        });
    }

    //Shows an edit orbital dialog
    private static void showEditOrbitalDialog(final Activity context, final ViewGroup container, final Selectable.ListFragmentAdapter adapter, ArrayList<Item> selectedItems, final ArrayList<Database.DatabaseSatellite> orbitals, final EditValuesDialog.OnDismissListener dismissListener)
    {
        int index;
        String unknownOwnerName = null;
        final Resources res = context.getResources();
        final List<String> ownerValuesList;
        ArrayList<UpdateService.MasterOwner> owners = Database.getOwners(context);
        int[] ids = new int[selectedItems.size()];
        final long[] dateValues = new long[ids.length];
        final String[] nameValues = new String[ids.length];
        final String[] ownerValues = new String[owners.size()];
        final String[] ownerCodes = new String[ownerValues.length];
        final String[] defaultOwnerValues = new String[ids.length];

        //if no items
        if(selectedItems.size() < 1)
        {
            //stop
            return;
        }

        //get list values
        for(index = 0; index < ownerValues.length; index++)
        {
            //remember current owner
            UpdateService.MasterOwner currentOwner = owners.get(index);

            //set owner ID and name
            ownerCodes[index] = currentOwner.code;
            ownerValues[index] = currentOwner.name;

            //if unknown owner
            if(currentOwner.code.equals("TBD"))
            {
                //remember unknown owner name
                unknownOwnerName = currentOwner.name;
            }
        }
        ownerValuesList = Arrays.asList(ownerValues);

        //get IDs and values
        for(index = 0; index < nameValues.length; index++)
        {
            //remember current item and owner
            Item currentItem = selectedItems.get(index);
            String currentOwner = currentItem.getOwner(context);

            //get current ID and values
            ids[index] = (orbitals != null ? index : currentItem.id);
            nameValues[index] = currentItem.text;
            defaultOwnerValues[index] = (currentOwner == null || currentOwner.equals("") ? unknownOwnerName : currentOwner);
            dateValues[index] = currentItem.launchDateMs;
        }

        //show dialog
        new EditValuesDialog(context, new EditValuesDialog.OnSaveListener()
        {
            @Override
            public void onSave(EditValuesDialog dialog, int itemIndex, int id, String textValue, String text2Value, double number1, double number2, double number3, String listValue, String list2Value, long dateValue, int color1, int color2, boolean visible)
            {
                String ownerCode = ownerCodes[ownerValuesList.indexOf(list2Value)];

                //if orbitals are set
                if(orbitals != null)
                {
                    //get current orbital
                    Database.DatabaseSatellite currentOrbital = orbitals.get(id);

                    //save satellite
                    Database.saveSatellite(context, currentOrbital.name, textValue, currentOrbital.noradId, ownerCode, dateValue, currentOrbital.tleLine1, currentOrbital.tleLine2, currentOrbital.tleDateMs, null, currentOrbital.updateDateMs, currentOrbital.pathColor, currentOrbital.orbitalType, true);
                }
                else
                {
                    //save satellite
                    Database.saveSatellite(context, id, textValue, ownerCode, dateValue);
                }
            }
        }, new EditValuesDialog.OnDismissListener()
        {
            @Override
            public void onDismiss(EditValuesDialog dialog, int saveCount)
            {
                //if adapter exists
                if(adapter != null)
                {
                    //if any were saved
                    if(saveCount > 0)
                    {
                        //need update
                        adapter.notifyDataSetChanged();
                    }

                    //end edit mode
                    adapter.cancelEditMode(container);
                }

                //if listener is set
                if(dismissListener != null)
                {
                    //call it
                    dismissListener.onDismiss(dialog, saveCount);
                }
            }
        }).getOrbital(res.getString(R.string.title_edit), ids, res.getString(R.string.title_name), nameValues, res.getString(R.string.title_owner), ownerValues, ownerCodes, defaultOwnerValues, res.getString(R.string.title_launch_date), dateValues);
    }

    //Shows an edit details dialog
    private static void showEditDetailsDialog(Activity context, SwipeStateViewPager pager, Selectable.ListFragmentAdapter adapter)
    {
        int index;
        ArrayList<Selectable.ListItem> selectedListItems = adapter.getSelectedItems(pager, PageType.Satellites);
        ArrayList<Item> selectedItems = new ArrayList<>(selectedListItems.size());

        //go through each item
        for(index = 0; index < selectedListItems.size(); index++)
        {
            //add converted item
            selectedItems.add((Item)selectedListItems.get(index));
        }

        //show dialog
        showEditOrbitalDialog(context, pager, adapter, selectedItems, null, null);
    }

    //Shows an edit dialog
    public static void showEditDialog(Activity context, SwipeStateViewPager pager, Selectable.ListFragmentAdapter adapter)
    {
        Resources res = context.getResources();
        ArrayList<Selectable.ListItem> selectedItems = adapter.getSelectedItems(pager, PageType.Satellites);
        int selectedItemCount = selectedItems.size();
        String editString = res.getString(R.string.title_edit);

        //if less than 2 selected items
        if(selectedItemCount < 2)
        {
            //show edit details dialog
            showEditDetailsDialog(context, pager, adapter);
        }
        else
        {
            //show EditType dialog
            Globals.showSelectDialog(context, res.getString(R.string.title_edit), AddSelectListAdapter.SelectType.Edit, new AddSelectListAdapter.OnItemClickListener()
            {
                @Override
                public void onItemClick(int which)
                {
                    //create dismiss listener
                    EditValuesDialog.OnDismissListener dismissListener = new EditValuesDialog.OnDismissListener()
                    {
                        @Override
                        public void onDismiss(EditValuesDialog dialog, int saveCount)
                        {
                            //if any were saved
                            if(saveCount > 0)
                            {
                                //need update
                                adapter.notifyDataSetChanged();
                            }

                            //end edit mode
                            adapter.cancelEditMode(pager);
                        }
                    };

                    //handle based on edit type
                    switch(which)
                    {
                        case AddSelectListAdapter.EditType.Color:
                            //sort items to keep in list order
                            Collections.sort(selectedItems, new Selectable.ListItem.Comparer());

                            //get colors using first and last selected item colors
                            new EditValuesDialog(context, new EditValuesDialog.OnSaveListener()
                            {
                                @Override
                                public void onSave(EditValuesDialog dialog, int itemIndex, int id, String textValue, String text2Value, double number1, double number2, double number3, String listValue, String list2Value, long dateValue, int color1, int color2, boolean visible)
                                {
                                    boolean isTransition = (color2 != Integer.MAX_VALUE);
                                    double redDelta = 0;
                                    double greenDelta = 0;
                                    double blueDelta = 0;
                                    double currentRed = 0;
                                    double currentGreen = 0;
                                    double currentBlue = 0;

                                    //if a transition
                                    if(isTransition)
                                    {
                                        //get color component and deltas
                                        currentRed = Color.red(color1);
                                        currentGreen = Color.green(color1);
                                        currentBlue = Color.blue(color1);
                                        redDelta = (Color.red(color2) - Color.red(color1)) / (selectedItemCount - 1.0);
                                        greenDelta = (Color.green(color2) - Color.green(color1)) / (selectedItemCount - 1.0);
                                        blueDelta = (Color.blue(color2) - Color.blue(color1)) / (selectedItemCount - 1.0);
                                    }

                                    //go through each selected item
                                    for(Selectable.ListItem currentItem : selectedItems)
                                    {
                                        //save color
                                        Database.saveSatellitePathColor(context, currentItem.id, (isTransition ? Color.rgb((int)currentRed, (int)currentGreen, (int)currentBlue) : color1));

                                        //if a transition
                                        if(isTransition)
                                        {
                                            //update color components
                                            currentRed += redDelta;
                                            currentGreen += greenDelta;
                                            currentBlue += blueDelta;
                                        }
                                    }
                                }
                            }, dismissListener).getEditColors(editString + " " + res.getString(R.string.title_colors), ((Item)selectedItems.get(0)).color, ((Item)(selectedItems.get(selectedItemCount - 1))).color);
                            break;

                        case AddSelectListAdapter.EditType.Visibility:
                            int visibleCount = 0;

                            //get visible count
                            for(Selectable.ListItem currentItem : selectedItems)
                            {
                                //if current item is visible
                                if(((Item)currentItem).isVisible)
                                {
                                    //add to count
                                    visibleCount++;
                                }
                            }

                            //get visible
                            new EditValuesDialog(context, new EditValuesDialog.OnSaveListener()
                            {
                                @Override
                                public void onSave(EditValuesDialog dialog, int itemIndex, int id, String textValue, String text2Value, double number1, double number2, double number3, String listValue, String list2Value, long dateValue, int color1, int color2, boolean visible)
                                {
                                    //go through each selected item
                                    for(Selectable.ListItem currentItem : selectedItems)
                                    {
                                        //get current norad ID and orbital
                                        int currentNoradId = currentItem.id;
                                        Database.DatabaseSatellite currentOrbital = Database.getOrbital(context, currentNoradId);

                                        //save visibility
                                        Database.saveSatelliteVisible(context, currentNoradId, visible);

                                        //if current orbital exists
                                        if(currentOrbital != null)
                                        {
                                            //update whether in excluded old norad IDs
                                            MainActivity.updateExcludedOldNoradIds(currentNoradId, currentOrbital.tleIsAccurate);
                                        }
                                    }

                                    //update current usage
                                    MainActivity.loadOrbitals(context, pager);
                                }
                            }, dismissListener).getVisible(editString + " " + res.getString(R.string.title_visible), visibleCount >= (int)Math.ceil(selectedItemCount / 2.0));
                            break;

                        case AddSelectListAdapter.EditType.Details:
                            //show edit details dialog
                            showEditDetailsDialog(context, pager, adapter);
                            break;
                    }
                }
            });
        }
    }

    //Shows a load orbital dialog
    public static void showLoadDialog(Activity context, ViewGroup container, Selectable.ListFragmentAdapter adapter, ArrayList<Database.DatabaseSatellite> loadOrbitals, EditValuesDialog.OnDismissListener dismissListener)
    {
        int index;
        ArrayList<Item> selectedItems = new ArrayList<>(loadOrbitals.size());

        //go through each orbital
        for(index = 0; index < loadOrbitals.size(); index++)
        {
            //add orbital
            selectedItems.add(new Item(context, index, loadOrbitals.get(index), false, true));
        }

        //show dialog
        showEditOrbitalDialog(context, container, adapter, selectedItems, loadOrbitals, dismissListener);
    }
}
