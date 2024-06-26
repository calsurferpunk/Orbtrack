package com.nikolaiapps.orbtrack;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
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
import androidx.viewpager.widget.PagerTitleStrip;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
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
        static final int SolarSystem = 1;
        static final int Constellations = 2;
        static final int Stars = 3;
        static final int PageCount = 4;
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
            if(satellite != null)
            {
                satellite.orbitalType = currentSat.orbitalType;
            }
        }
    }

    //Page list item holder
    public static class PageListItemHolder extends Selectable.ListDisplayItemHolder
    {
        final AppCompatImageView itemImage;
        final TextView itemText;
        final LinearLayout tleAgeLayout;
        final LinearLayout itemAgeTextLayout;
        final TextView tleAgeText;
        final View tleUnder;
        final BorderButton colorButton;
        final AppCompatButton visibleButton;
        final CircularProgressIndicator progress;

        public PageListItemHolder(View viewItem, int itemImageId, int itemTextId, int itemAgeTextLayoutId, int tleAgeLayoutId, int tleAgeTextId, int tleAgeUnderId, int colorButtonId, int visibleButtonId, int progressId)
        {
            super(viewItem, -1);
            itemImage = viewItem.findViewById(itemImageId);
            itemText = viewItem.findViewById(itemTextId);
            itemAgeTextLayout = viewItem.findViewById(itemAgeTextLayoutId);
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
                    case PageType.SolarSystem:
                    case PageType.Constellations:
                    case PageType.Stars:
                        //get conditions
                        switch(page)
                        {
                            case PageType.Satellites:
                                orbitalTypes = new byte[]{Database.OrbitalType.Satellite, Database.OrbitalType.RocketBody, Database.OrbitalType.Debris};
                                break;

                            case PageType.SolarSystem:
                                orbitalTypes = new byte[]{Database.OrbitalType.Sun, Database.OrbitalType.Planet};
                                break;

                            case PageType.Constellations:
                                orbitalTypes = new byte[]{Database.OrbitalType.Constellation};
                                break;

                            case PageType.Stars:
                                orbitalTypes = new byte[]{Database.OrbitalType.Star};
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

        public PageListAdapter(Context context, int page, int titleStringId, String categoryTitle, boolean isSimple, OrbitalFilterList.OnLoadItemsListener loadItemsListener)
        {
            super(context, categoryTitle);

            //set defaults
            loadingItems = true;

            //set page
            currentPage = page;
            columnTitleStringId = titleStringId;
            simple = isSimple;
            forSetup = (columnTitleStringId > 0);
            this.itemsRefID = R.layout.orbitals_material_item;

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
            loadItems();
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
                boolean usingContext = haveContext();
                boolean onSatellites = (currentPage == PageType.Satellites);
                int nonNameVisibility = (onSatellites && !forSetup ? View.VISIBLE : View.GONE);
                String ageString = (usingContext ? currentContext.getString(R.string.title_tle_age) : "");
                String nameString = (usingContext ? currentContext.getString(columnTitleStringId > 0 ? columnTitleStringId : R.string.title_name) : "");
                String tleDateString = nameString + "/" + ageString;
                TextView itemText = listColumns.findViewById(R.id.Object_Item_Text);
                TextView tleDateText = listColumns.findViewById(R.id.Object_TLE_Age_Text);
                BorderButton colorButton = listColumns.findViewById(R.id.Object_Color_Button);
                AppCompatButton visibleButton = listColumns.findViewById(R.id.Object_Visible_Button);

                if(onSatellites)
                {
                    itemText.setVisibility(View.GONE);
                }
                else
                {
                    itemText.setText(nameString);
                    itemText.setTypeface(null, Typeface.NORMAL);
                }
                tleDateText.setText(tleDateString);
                tleDateText.setVisibility(nonNameVisibility);
                listColumns.findViewById(R.id.Object_TLE_Age_Under).setVisibility(View.GONE);
                visibleButton.setVisibility(View.INVISIBLE);
                colorButton.setVisibility(View.INVISIBLE);
            }

            super.setColumnTitles(listColumns, categoryText, page);
        }

        public void loadItems()
        {
            if(loadItems != null)
            {
                loadItems.execute(currentContext, currentPage, simple);
            }
        }

        private void setText(TextView view, String text)
        {
            if(view != null)
            {
                view.setText(text);
            }
        }

        @Override
        protected void onItemNonEditClick(Selectable.ListDisplayItem item, int pageNum)
        {
            final Item currentItem = (Item)item;
            final Calculations.OrbitDataType currentOrbit = currentItem.satelliteObject.orbit;
            final Calculations.TLEDataType currentTLE = currentItem.satelliteObject.tle;
            final AppCompatImageButton notifyButton;
            final AppCompatImageButton preview3dButton;
            final AppCompatImageButton infoButton;
            final boolean onSatellite = (currentItem.id > 0);
            final boolean use3dPreview = have3dPreview(currentItem.id);
            final boolean usingContext = haveContext();
            final Drawable itemIcon = (usingContext ? Globals.getOrbitalIcon(currentContext, MainActivity.getObserver(), currentItem.satellite.noradId, currentItem.satellite.orbitalType) : null);
            final ItemDetailDialog detailDialog = new ItemDetailDialog(currentContext, listInflater, currentItem.id, currentItem.text, currentItem.getOwnerCode(), itemIcon, itemDetailButtonClickListener);
            final Resources res = (usingContext ? currentContext.getResources() : null);
            final TextView[] texts;

            //if no context
            if(!usingContext)
            {
                //stop
                return;
            }

            //get buttons
            notifyButton = detailDialog.addButton(pageNum, item.id, currentItem, DetailButtonType.Notify);
            preview3dButton = detailDialog.addButton(pageNum, item.id, currentItem, DetailButtonType.Preview3d);
            infoButton = detailDialog.addButton(pageNum, item.id, currentItem, DetailButtonType.Info);

            //if on a satellite
            if(onSatellite)
            {
                //add titles
                detailDialog.addGroup(res.getString(R.string.title_owner), null,
                                                 res.getString(R.string.title_norad) + " " + res.getString(R.string.title_id), res.getString(R.string.abbrev_international) + " " + res.getString(R.string.title_code),
                                                 res.getString(R.string.title_launch_date), res.getString(R.string.title_perigee),
                                                 res.getString(R.string.title_launch_number), res.getString(R.string.title_apogee),
                                                 res.getString(R.string.title_inclination), res.getString(R.string.title_period),
                                                 res.getString(R.string.title_semi_major_axis));

                //get displays
                texts = detailDialog.getDetailTexts();
                if(texts != null && texts.length > 10)
                {
                    //update displays
                    setText(texts[0], currentItem.getOwner(currentContext));
                    setText(texts[2], String.valueOf(currentTLE.satelliteNum));
                    setText(texts[3], currentTLE.internationalCode);
                    setText(texts[4], String.valueOf(currentTLE.launchYear));
                    setText(texts[5], Globals.getKmUnitValueString(currentOrbit.perigee) + " " + Globals.getKmLabel(res));
                    setText(texts[6], String.valueOf(currentTLE.launchNum));
                    setText(texts[7], Globals.getKmUnitValueString(currentOrbit.apogee) + " " + Globals.getKmLabel(res));
                    setText(texts[8], Globals.getNumberString(currentTLE.inclinationDeg));
                    setText(texts[9], Globals.getNumberString(currentOrbit.periodMinutes) + " " + res.getString(R.string.abbrev_minutes_lower));
                    setText(texts[10], Globals.getKmUnitValueString(currentOrbit.semiMajorAxisKm) + " " + Globals.getKmLabel(res));
                }
            }
            //else if info button exists
            else if(infoButton != null)
            {
                //show info display
                infoButton.performClick();
            }

            //update display visibilities
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
            PageListItemHolder itemHolder = new PageListItemHolder(itemView, R.id.Object_Item_Image, R.id.Object_Item_Text, R.id.Object_Item_Text_Age_Layout, -1, R.id.Object_TLE_Age_Text, R.id.Object_TLE_Age_Under, R.id.Object_Color_Button, R.id.Object_Visible_Button, R.id.Object_Progress);

            setItemSelector(itemView);
            setViewClickListeners(itemView, itemHolder);
            return(itemHolder);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position)
        {
            final boolean onSatellites = (currentPage == PageType.Satellites);
            final boolean usingContext = haveContext();
            final int backgroundColor = (usingContext ? Globals.resolveColorID(currentContext, android.R.attr.colorBackground) : Color.WHITE);
            final Resources res = (usingContext ? currentContext.getResources() : null);
            final boolean haveRes = (res != null);
            final PageListItemHolder itemHolder = (PageListItemHolder)holder;
            final OrbitalFilterList.Item filterItem = (position < displayedItems.size() ? displayedItems.get(position) : null);
            final Item currentItem = (loadingItems || !(filterItem instanceof Item) ? new Item(currentContext, 0, new Database.DatabaseSatellite("", Universe.IDs.Invalid, "", Globals.UNKNOWN_DATE_MS, Database.OrbitalType.Satellite), false, false) : (Item)filterItem);
            double ageDays = (System.currentTimeMillis() - currentItem.tleDateMs) / Calculations.MsPerDay;
            String dayText = Globals.getNumberString(ageDays, 1) + " " + (haveRes ? res.getString(R.string.title_days) : "");
            Drawable itemIcon;

            //if holder is a ViewGroup
            if(itemHolder.itemView instanceof ViewGroup)
            {
                int index;
                ViewGroup itemView = (ViewGroup)itemHolder.itemView;

                //go through each child view
                for(index = 0; index < itemView.getChildCount(); index++)
                {
                    //set visibility
                    View currentView = itemView.getChildAt(index);
                    boolean isProgress = currentView.equals(itemHolder.progress);
                    currentView.setVisibility(loadingItems ? (isProgress ? View.VISIBLE : View.GONE) : (isProgress ? View.GONE : View.VISIBLE));
                }
            }

            //if not loading items
            if(!loadingItems)
            {
                //set displays
                itemIcon = Globals.getOrbitalIcon(currentContext, MainActivity.getObserver(), currentItem.satellite.noradId, currentItem.satellite.orbitalType, (Settings.getDarkTheme(currentContext) ? R.color.white : R.color.black));
                itemHolder.itemImage.setImageDrawable(itemIcon);
                if(simple)
                {
                    itemHolder.itemImage.setBackgroundColor(backgroundColor);
                }
                itemHolder.itemText.setText(currentItem.text);
                if(simple)
                {
                    itemHolder.itemText.setBackgroundColor(backgroundColor);
                }
                if(itemHolder.itemAgeTextLayout != null && simple)
                {
                    itemHolder.itemAgeTextLayout.setBackgroundColor(backgroundColor);
                }
                if(itemHolder.tleAgeLayout != null)
                {
                    itemHolder.tleAgeLayout.setVisibility(onSatellites ? View.VISIBLE : View.GONE);
                }
                itemHolder.tleAgeText.setVisibility(onSatellites ? View.VISIBLE : View.GONE);
                itemHolder.tleUnder.setVisibility(onSatellites && ageDays > 2 ? View.VISIBLE : View.GONE);
                if(onSatellites)
                {
                    itemHolder.tleAgeText.setText(dayText);
                    if(simple)
                    {
                        itemHolder.tleAgeText.setBackgroundColor(backgroundColor);
                    }
                    if(ageDays > 2)
                    {
                        itemHolder.tleUnder.setBackgroundColor(ageDays >= 14 ? Color.RED : ageDays >= 7 ? 0xFFFFA500 : Color.YELLOW);
                    }
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

                                    //if current matches selected satellite
                                    if(currentSatellite.getSatelliteNum() == currentItem.satelliteObject.getSatelliteNum())
                                    {
                                        //update selected satellite
                                        currentSatellite.setPathColor(color);
                                    }
                                }
                            }
                        });
                        colorDialog.setIcon(itemIcon);
                        colorDialog.setTitle(haveRes ? (res.getString(R.string.title_select) + " " + currentItem.text + " " + res.getString(R.string.title_color)) : currentItem.text);
                        colorDialog.show();
                    }
                });
                itemHolder.visibleButton.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        int noradId = currentItem.id;
                        byte orbitalType = currentItem.satellite.orbitalType;
                        Database.DatabaseSatellite currentOrbital = Database.getOrbital(currentContext, noradId);
                        boolean haveOrbital = (currentOrbital != null);
                        ArrayList<Database.ParentProperties> currentProperties = (haveOrbital ? currentOrbital.parentProperties : null);
                        ArrayList<Database.DatabaseSatellite> usedParentOrbitals = new ArrayList<>(0);

                        //if a star in a constellation
                        if(orbitalType == Database.OrbitalType.Star && currentProperties != null && !currentProperties.isEmpty())
                        {
                            //go through each parent
                            for(Database.ParentProperties currentProperty : currentProperties)
                            {
                                //if current parent is used and not in the list yet
                                Database.DatabaseSatellite currentParent = Database.getOrbital(currentContext, currentProperty.id);
                                if(currentParent != null && currentParent.isSelected && !usedParentOrbitals.contains(currentParent))
                                {
                                    //add parent to used list
                                    usedParentOrbitals.add(currentParent);
                                }
                            }
                        }

                        //if trying to disable orbital used by any selected parents
                        if(currentItem.isVisible && !usedParentOrbitals.isEmpty())
                        {
                            //if resources exist
                            if(haveRes)
                            {
                                int index;
                                StringBuilder parentsMessage = new StringBuilder();

                                //build used parents message
                                for(index = 0; index < usedParentOrbitals.size(); index++)
                                {
                                    //if after first
                                    if(index > 0)
                                    {
                                        //add separator
                                        parentsMessage.append(", ");

                                        //if no more after this
                                        if(index + 1 >= usedParentOrbitals.size())
                                        {
                                            //add "and "
                                            parentsMessage.append(res.getString(R.string.text_and));
                                            parentsMessage.append(" ");
                                        }
                                    }

                                    //add parent name
                                    parentsMessage.append(usedParentOrbitals.get(index).getName());
                                }

                                //show error dialog
                                Globals.showConfirmDialog(currentContext, res.getString(R.string.title_cant_disable), res.getQuantityString(R.plurals.desc_used_by_constellation, usedParentOrbitals.size(), currentOrbital.getName(), parentsMessage.toString()), res.getString(R.string.title_ok), null, true, null, null, null);
                            }
                        }
                        else
                        {
                            //update visibility and save orbital in database
                            currentItem.isVisible = !currentItem.isVisible;
                            itemHolder.visibleButton.setBackgroundDrawable(Globals.getVisibleIcon(currentContext, currentItem.isVisible));
                            Database.saveSatelliteVisible(currentContext, noradId, currentItem.isVisible);

                            //if item is visible again and orbital is set
                            if(currentItem.isVisible && haveOrbital)
                            {
                                //if a constellation
                                if(orbitalType == Database.OrbitalType.Constellation)
                                {
                                    //go through all children
                                    ArrayList<Integer> childIds = currentOrbital.getChildIds();
                                    for(Integer childId : childIds)
                                    {
                                        //enable child
                                        Database.saveSatelliteVisible(currentContext, childId, true);
                                    }

                                    //update stars displays
                                    PageAdapter.updatePage(PageType.Stars);
                                }

                                //if not a valid TLE
                                if(!currentOrbital.tleIsAccurate)
                                {
                                    //make sure not in excluded old norad IDs
                                    MainActivity.updateExcludedOldNoradIds(noradId, false);
                                }
                            }

                            //update current usage
                            MainActivity.loadOrbitals(currentContext, holder.itemView);
                        }
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
        private View ageLayout;
        private View groupLayout;
        private View ownerLayout;
        private View typeLayout;
        private View searchGroup;
        private CustomSearchView searchView;
        private LinearLayout searchLayout;
        private SelectListInterface ownerList;
        private SelectListInterface groupList;
        private SelectListInterface ageList;
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
        public View createView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
        {
            Context context = Page.this.getContext();
            int page = this.getPageParam();
            boolean onSatellites = (page == PageType.Satellites);
            View newView;
            Intent serviceIntent;
            ArrayList<Database.DatabaseSatellite> satelliteList;

            //create adapter
            listAdapter = new PageListAdapter(context, page, -1, categoryTitle, simple, new OrbitalFilterList.OnLoadItemsListener()
            {
                @Override
                public void onLoaded(ArrayList<OrbitalFilterList.Item> items, boolean foundLaunchDate)
                {
                    //if started from an activity
                    if(context instanceof Activity)
                    {
                        ((Activity)context).runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                //update inputs for page
                                setupInputs(page);
                            }
                        });
                    }
                }
            });

            //create view
            newView = this.onCreateView(inflater, container, listAdapter);
            searchLayout = newView.findViewById(R.id.Orbital_Search_Layout);
            searchGroup = newView.findViewById(R.id.Orbital_Search_Lists_Layout);
            ownerList = newView.findViewById(R.id.Orbital_Search_Owner_Text_List);
            groupList = newView.findViewById(R.id.Orbital_Search_Group_Text_List);
            ageList = newView.findViewById(R.id.Orbital_Search_Age_Text_List);
            ownerLayout = newView.findViewById(R.id.Orbital_Search_Owner_Layout);
            groupLayout = newView.findViewById(R.id.Orbital_Search_Group_Layout);
            ageLayout = newView.findViewById(R.id.Orbital_Search_Age_Layout);
            typeLayout = newView.findViewById(R.id.Orbital_Search_Type_Layout);
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
        public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater inflater)
        {
            //create options menu
            inflater.inflate(R.menu.menu_main_layout, menu);
            super.onCreateMenu(menu, inflater);
        }

        @Override
        public void onPrepareMenu(@NonNull Menu menu)
        {
            boolean onOrbitalSatellites = (pageNum == PageType.Satellites);
            int itemCount = (onOrbitalSatellites && listAdapter != null ? listAdapter.getAllItemCount() : getListItemCount());
            boolean haveSatellites = (onOrbitalSatellites && itemCount > 0);
            boolean onOrbitalSatellitesExistNoModify = (haveSatellites && !UpdateService.modifyingSatellites());

            MenuItem searchMenu = menu.findItem(R.id.menu_search);
            searchMenu.setVisible(!onOrbitalSatellites || onOrbitalSatellitesExistNoModify);
            searchView = (CustomSearchView)searchMenu.getActionView();
            if(searchView != null)
            {
                searchView.setOnSearchStateChangedListener(new CustomSearchView.OnSearchStateChangedListener()
                {
                    @Override
                    public void onSearchStateChanged(boolean visible)
                    {
                        PagerTitleStrip mainPagerTitles = MainActivity.getPagerTitles();
                        SwipeStateViewPager mainPager = MainActivity.getPager();

                        //if pager titles exists
                        if(mainPagerTitles != null)
                        {
                            //show when search hidden
                            mainPagerTitles.setVisibility(visible ? View.GONE : View.VISIBLE);
                        }

                        //if pager exists
                        if(mainPager != null)
                        {
                            //disable swiping searching
                            mainPager.setSwipeEnabled(!visible);
                        }

                        //if search layout exists
                        if(searchLayout != null)
                        {
                            //update visibility
                            searchLayout.setVisibility(visible && onOrbitalSatellitesExistNoModify ? View.VISIBLE : View.GONE);
                        }
                    }
                });
            }
            menu.findItem(R.id.menu_save).setVisible(onOrbitalSatellitesExistNoModify);
            menu.findItem(R.id.menu_update).setVisible(onOrbitalSatellitesExistNoModify);

            //update inputs for page
            setupInputs(pageNum);
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
            Selectable.ListDisplayItem[] items = selectedItems.toArray(new Selectable.ListDisplayItem[0]);

            //go through selected items and close database
            for(Selectable.ListDisplayItem currentItem : items)
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

        @Override
        protected OnOrientationChangedListener createOnOrientationChangedListener(RecyclerView list, Selectable.ListBaseAdapter listAdapter, int page)
        {
            if(listAdapter instanceof PageListAdapter)
            {
                PageListAdapter adapter = (PageListAdapter)listAdapter;

                return(new OnOrientationChangedListener()
                {
                    @Override
                    public void orientationChanged()
                    {
                        //update display
                        if(adapter.colorDialog != null)
                        {
                            adapter.colorDialog.reload();
                        }
                    }
                });
            }
            else
            {
                return(null);
            }
        }

        //Sets up inputs
        private void setupInputs(int page)
        {
            //if adapter exists
            if(listAdapter != null)
            {
                boolean onSatellites = (page == PageType.Satellites);
                OrbitalFilterList.OrbitalListAdapter.UsedData used = listAdapter.getUsed();

                //setup inputs and collapse
                listAdapter.setupInputs(searchGroup, (onSatellites ? ownerList : null), (onSatellites ? groupList : null), (onSatellites ? ageList : null), null, ageLayout, groupLayout, ownerLayout, typeLayout, searchView, showButton, used.owners, used.categories, used.types, listAdapter.getHasLaunchDates());
                listAdapter.showSearchInputs(false);
            }
        }

        //Updates displays
        public void updateDisplays()
        {
            if(listAdapter != null)
            {
                listAdapter.loadItems();
            }
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

        /** @noinspection ConstantValue*/
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
            if(selectedItems.isEmpty())
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
        private static final Selectable.ListFragment.OnUpdatePageListener[] updatePageListeners = new Selectable.ListFragment.OnUpdatePageListener[PageType.PageCount];

        public PageAdapter(FragmentManager fm, View parentView, Selectable.ListFragment.OnItemDetailButtonClickListener detailListener, Selectable.ListFragment.OnAdapterSetListener adapterListener, Selectable.ListFragment.OnUpdateNeededListener updateListener, Selectable.ListFragment.OnPageResumeListener resumeListener)
        {
            super(fm, parentView, null, updateListener, null,  detailListener, adapterListener, null, resumeListener, MainActivity.Groups.Orbitals, null);
        }

        @Override
        public @NonNull Fragment getItem(int position)
        {
            final Page newPage = new Page();

            updatePageListeners[position] = new Selectable.ListFragment.OnUpdatePageListener()
            {
                @Override
                public void updatePage(int page, int subPage)
                {
                    newPage.updateDisplays();
                }
            };

            return(this.getItem(group, position, -1, newPage));
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

                case PageType.SolarSystem:
                    return(res.getString(R.string.title_solar_system));

                case PageType.Constellations:
                    return(res.getString(R.string.title_constellations));

                case PageType.Stars:
                    return(res.getString(R.string.title_stars));

                default:
                    return(res.getString(R.string.title_invalid));
            }
        }

        //Gets if items are loading on page
        @SuppressWarnings("BooleanMethodIsAlwaysInverted")
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

        //Updates page at position
        public static void updatePage(int position)
        {
            if(position < updatePageListeners.length && updatePageListeners[position] != null)
            {
                updatePageListeners[position].updatePage(position, -1);
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
        if(selectedItems.isEmpty())
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
            defaultOwnerValues[index] = (currentOwner == null || currentOwner.isEmpty() ? unknownOwnerName : currentOwner);
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
        ArrayList<Selectable.ListDisplayItem> selectedListItems = adapter.getSelectedItems(pager, PageType.Satellites);
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
        ArrayList<Selectable.ListDisplayItem> selectedItems = adapter.getSelectedItems(pager, PageType.Satellites);
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
                            Collections.sort(selectedItems, new Selectable.ListDisplayItem.Comparer());

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
                                    for(Selectable.ListDisplayItem currentItem : selectedItems)
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
                            for(Selectable.ListDisplayItem currentItem : selectedItems)
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
                                    for(Selectable.ListDisplayItem currentItem : selectedItems)
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
