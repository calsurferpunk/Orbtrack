package com.nikolaiapps.orbtrack;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;
import android.os.Parcelable;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseExpandableListAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.TimerTask;


public abstract class Calculate
{
    static final int EXTENDED_COLUMN_1_WIDTH_DP = 510;
    static final int EXTENDED_COLUMN_1_SHORT_WIDTH_DP = 450;
    static final int EXTENDED_COLUMN_2_SHORT_WIDTH_DP = 500;

    public static abstract class PageType
    {
        static final int View = 0;
        static final int Passes = 1;
        static final int Coordinates = 2;
        static final int Intersection = 3;
        static final int PageCount = 4;
    }

    @SuppressWarnings("SpellCheckingInspection")
    public static abstract class ParamTypes
    {
        static final String NoradId = "id";
        static final String NoradIdOld = "idOld";
        static final String NoradId2 = "id2";
        static final String NoradId2Old = "id2Old";
        static final String MultiNoradId = "multiId";
        static final String MultiNoradIdOld = "multiIdOld";
        static final String OrbitalIsSelected = "orbitalIsSelected";
        static final String StartDateMs = "startDate";
        static final String EndDateMs = "endDate";
        static final String IncrementUnit = "incUnit";
        static final String IncrementType = "incType";
        static final String ElevationMinDegs = "elMinDegs";
        static final String IntersectionDegs = "intscDegs";
    }

    public static abstract class IncrementType
    {
        static final int Seconds = 0;
        static final int Minutes = 1;
        static final int Hours = 2;
        static final int Days = 3;
    }

    public static class CalculateDataBase
    {
        public int noradId;
        public double illumination;
        public String phaseName;

        public CalculateDataBase()
        {
            this.noradId = Universe.IDs.Invalid;
            this.illumination = 0;
            this.phaseName = null;
        }
    }

    private static class ItemHolderBase extends RecyclerView.ViewHolder
    {
        public final View progressGroup;
        public final TextView timeText;
        public final TextView nameText;
        public final TextView dataGroup1Text;
        public final TextView dataGroup2Text;
        public final TextView dataGroup3Text;
        public final TextView dataGroup4Text;
        public final TextView dataGroup5Text;
        public final LinearLayout dataGroup;
        public final ExpandingListView subList;
        public final AppCompatImageView nameImage;

        public ItemHolderBase(View itemView, int dataGroup1TextId, int dataGroup2TextId, int dataGroup3TextId, int dataGroup4TextId, int dataGroup5TextId, int dataGroupId, int nameImageId, int timeTextId, int nameTextId, int progressGroupId, int subListId)
        {
            super(itemView);

            dataGroup1Text = itemView.findViewById(dataGroup1TextId);
            dataGroup2Text = itemView.findViewById(dataGroup2TextId);
            dataGroup3Text = (dataGroup3TextId > -1 ? (TextView)itemView.findViewById(dataGroup3TextId) : null);
            dataGroup4Text = (dataGroup4TextId > -1 ? (TextView)itemView.findViewById(dataGroup4TextId) : null);
            dataGroup5Text = (dataGroup5TextId > -1 ? (TextView)itemView.findViewById(dataGroup5TextId) : null);
            dataGroup = itemView.findViewById(dataGroupId);
            progressGroup = itemView.findViewById(progressGroupId);
            subList = (subListId > -1 ? itemView.findViewById(subListId) : null);
            timeText = (timeTextId > -1 ? (TextView)itemView.findViewById(timeTextId) : null);
            nameImage = (nameImageId > -1 ? (AppCompatImageView)itemView.findViewById(nameImageId) : null);
            nameText = (nameTextId > - 1 ? (TextView)itemView.findViewById(nameTextId) : null);

            if(subList != null && Settings.getMaterialTheme(subList.getContext()))
            {
                subList.setGroupIndicator(null);
            }
        }
    }

    //Angles
    public static abstract class ViewAngles
    {
        //Item
        public static class Item extends Selectable.ListItem
        {
            public boolean isLoading;
            public double julianDate;
            public Calendar time;
            public View progressGroup;
            public TextView timeText;
            public TextView azText;
            public TextView elText;
            public TextView rangeText;
            public TextView phaseText;
            public TextView illuminationText;
            public ExpandingListView subList;
            public LinearLayout dataGroup;
            public final CalculateViewsTask.ViewData[] views;

            public Item(int index, int viewCount)
            {
                super(Integer.MAX_VALUE, index, false, false, false, false);

                isLoading = (viewCount < 1);
                if(isLoading)
                {
                    viewCount = 1;
                }
                views = new CalculateViewsTask.ViewData[viewCount];
                for(index = 0; index < views.length; index++)
                {
                    views[index] = new CalculateViewsTask.ViewData();
                }
                julianDate = Double.MAX_VALUE;
                time = Globals.getCalendar(null, 0);
                timeText = null;
                azText = null;
                elText = null;
                rangeText = null;
                phaseText = null;
                illuminationText = null;
                progressGroup = null;
                dataGroup = null;
            }

            public void updateDisplays(Context context, int widthDp, boolean haveSun, boolean haveMoon)
            {
                CalculateViewsTask.ViewData currentView = views[0];

                if(azText != null)
                {
                    azText.setText(currentView.azimuth != Float.MAX_VALUE ? Globals.getDegreeString(currentView.azimuth) : "-");
                }
                if(elText != null)
                {
                    elText.setText(currentView.elevation != Float.MAX_VALUE ? Globals.getDegreeString(currentView.elevation) : "-");
                }
                if(rangeText != null)
                {
                    rangeText.setText(currentView.rangeKm != Float.MAX_VALUE && !Float.isInfinite(currentView.rangeKm) ? Globals.getKmUnitValueString(currentView.rangeKm, 0) : "-");
                }
                if(phaseText != null)
                {
                    phaseText.setText(currentView.phaseName == null ? "-" : currentView.phaseName);
                }
                if(illuminationText != null)
                {
                    illuminationText.setText(Globals.getIlluminationString(currentView.illumination));
                }
                if(timeText != null && time != null && context != null)
                {
                    timeText.setText(Globals.getDateTimeString(context, Globals.getLocalTime(time, time.getTimeZone()), true, true, false));
                }
                if(subList != null && context != null && views.length > 1)
                {
                    subList.setAdapter(new SubItemListAdapter(context, julianDate, views, widthDp, haveSun, haveMoon));
                }
            }

            public void setLoading(boolean loading, boolean tleIsAccurate)
            {
                isLoading = loading && tleIsAccurate;

                if(progressGroup != null)
                {
                    progressGroup.setVisibility(isLoading ? View.VISIBLE : View.GONE);
                }
                if(dataGroup != null)
                {
                    dataGroup.setVisibility(loading || !tleIsAccurate ? View.GONE : View.VISIBLE);
                }
                if(subList != null)
                {
                    subList.setVisibility(loading ? View.GONE : View.VISIBLE);
                }
            }
        }

        //Item holder
        public static class ItemHolder extends ItemHolderBase
        {
            private ItemHolder(View itemView, int azTextId, int elTextId, int rangeTextId, int phaseTextId, int illuminationTextId, int progressGroupId, int dataGroupId, int timeTextId, int subListId)
            {
                super(itemView, azTextId, elTextId, rangeTextId, phaseTextId, illuminationTextId, dataGroupId, -1, timeTextId, -1, progressGroupId, subListId);
            }
            public ItemHolder(View itemView, int azTextId, int elTextId, int rangeTextId, int phaseTextId, int illuminationTextId, int progressGroupId, int dataGroupId, int timeTextId)
            {
                this(itemView, azTextId, elTextId, rangeTextId, phaseTextId, illuminationTextId, progressGroupId, dataGroupId, timeTextId, -1);
            }
            public ItemHolder(View itemView, int progressGroupId, int subListId)
            {
                this(itemView, -1, -1, -1, -1, -1, progressGroupId, -1, -1, subListId);
            }
        }

        //Item list adapter
        public static class ItemListAdapter extends Selectable.ListBaseAdapter
        {
            private boolean haveSun;
            private boolean haveMoon;
            private final Current.Items viewItems;

            public ItemListAdapter(Context context, Item[] savedItems, int multiCount)
            {
                super(context);

                haveSun = haveMoon = false;

                //remember strings and layout ID
                this.itemsRefID = (usingMaterial ? R.layout.calculate_view_material_item : R.layout.calculate_view_item);

                //ID stays the same
                this.setHasStableIds(true);

                viewItems = new Current.Items(MainActivity.Groups.Calculate, PageType.View);

                //if there are saved items
                if(savedItems != null && savedItems.length > 0)
                {
                    //set as saved items
                    viewItems.set(savedItems);
                }
                else
                {
                    //set as empty
                    viewItems.set(new Item[]{new Item(0, 0)});
                }

                //remember layout ID
                forSubItems = (multiCount > 1);
                if(forSubItems)
                {
                    this.itemsRefSubId = this.itemsRefID;
                    this.itemsRefID = R.layout.calculate_multi_item;
                }

                updateHasItems();
            }

            @Override
            public int getItemCount()
            {
                return(viewItems.getCount());
            }

            @Override
            public Item getItem(int position)
            {
                return(viewItems.getViewItem(position));
            }

            @Override
            public long getItemId(int position)
            {
                Item currentItem = viewItems.getViewItem(position);
                return(currentItem != null ? currentItem.listIndex : Integer.MIN_VALUE);
            }

            @Override
            protected void setHeader(View header)
            {
                super.setHeader(header);
                header.setVisibility(View.VISIBLE);
            }

            @Override
            protected boolean showColumnTitles(int page)
            {
                return(true);
            }

            @Override
            protected void setColumnTitles(ViewGroup listColumns, TextView categoryText, int page)
            {
                String text;
                TextView timeText;
                TextView phaseText;
                TextView illuminationText;
                Resources res = currentContext.getResources();
                boolean isSun = (dataID == Universe.IDs.Sun);
                boolean isMoon = (dataID == Universe.IDs.Moon);

                if(!forSubItems)
                {
                    timeText = listColumns.findViewById(R.id.View_Time_Text);
                    timeText.setText(R.string.title_time);
                    timeText.setVisibility(View.VISIBLE);
                }
                else
                {
                    isSun = haveSun;
                    isMoon = haveMoon;
                }

                phaseText = listColumns.findViewById(R.id.View_Phase_Text);
                phaseText.setText(R.string.title_phase);
                phaseText.setVisibility((isSun || isMoon) && widthDp >= EXTENDED_COLUMN_1_SHORT_WIDTH_DP ? View.VISIBLE : View.GONE);

                illuminationText = listColumns.findViewById(R.id.View_Illumination_Text);
                illuminationText.setText(R.string.abbrev_illumination);
                illuminationText.setVisibility(isMoon && widthDp >= EXTENDED_COLUMN_2_SHORT_WIDTH_DP ? View.VISIBLE : View.GONE);
                listColumns.findViewById(R.id.View_Progress_Group).setVisibility(View.GONE);
                listColumns.findViewById(R.id.View_Data_Group).setVisibility(View.VISIBLE);
                ((TextView)listColumns.findViewById(R.id.View_Az_Text)).setText(R.string.title_azimuth);
                ((TextView)listColumns.findViewById(R.id.View_El_Text)).setText(R.string.title_elevation);
                text = res.getString(R.string.title_range) + " (" + Globals.getKmLabel(res) + ")";
                ((TextView)listColumns.findViewById(R.id.View_Range_Text)).setText(text);

                super.setColumnTitles(listColumns, categoryText, page);
            }

            @Override
            public @NonNull ItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
            {
                View itemView = LayoutInflater.from(parent.getContext()).inflate(this.itemsRefID, parent, false);
                ItemHolder itemHolder;

                if(forSubItems)
                {
                    itemHolder = new ItemHolder(itemView, R.id.Multi_Item_Progress_Group, R.id.Multi_Item_List);
                }
                else
                {
                    itemHolder = new ItemHolder(itemView, R.id.View_Az_Text, R.id.View_El_Text, R.id.View_Range_Text, R.id.View_Phase_Text, R.id.View_Illumination_Text, R.id.View_Progress_Group, R.id.View_Data_Group, R.id.View_Time_Text);
                }

                setItemSelector(itemView);
                setViewClickListeners(itemView, itemHolder);
                return(itemHolder);
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position)
            {
                Item currentItem = viewItems.getViewItem(position);
                ItemHolder itemHolder = (ItemHolder)holder;
                boolean isSun =  (currentItem.id == Universe.IDs.Sun);
                boolean isMoon = (currentItem.id == Universe.IDs.Moon);

                //get displays
                currentItem.timeText = itemHolder.timeText;
                if(currentItem.timeText != null)
                {
                    currentItem.timeText.setVisibility(View.VISIBLE);
                }

                if(forSubItems)
                {
                    isSun = haveSun;
                    isMoon = haveMoon;
                }

                currentItem.phaseText = itemHolder.dataGroup4Text;
                if(currentItem.phaseText != null)
                {
                    currentItem.phaseText.setVisibility((isSun || isMoon) && widthDp >= EXTENDED_COLUMN_1_SHORT_WIDTH_DP ? View.VISIBLE : View.GONE);
                }

                currentItem.illuminationText = itemHolder.dataGroup5Text;
                if(currentItem.illuminationText != null)
                {
                    currentItem.illuminationText.setVisibility(isMoon && widthDp >= EXTENDED_COLUMN_2_SHORT_WIDTH_DP ? View.VISIBLE : View.GONE);
                }

                currentItem.subList = itemHolder.subList;
                currentItem.progressGroup = itemHolder.progressGroup;
                currentItem.dataGroup = itemHolder.dataGroup;
                if(currentItem.dataGroup != null)
                {
                    currentItem.dataGroup.setVisibility(View.GONE);
                }
                currentItem.azText = itemHolder.dataGroup1Text;
                currentItem.elText = itemHolder.dataGroup2Text;
                currentItem.rangeText = itemHolder.dataGroup3Text;

                //update displays
                currentItem.setLoading(!hasItems, true);
                currentItem.updateDisplays(currentContext, widthDp, haveSun, haveMoon);
            }

            //Updates status of having items
            public void updateHasItems()
            {
                int index;
                int index2;
                int noradId;
                int count = viewItems.getCount();
                hasItems = (count > 1 || (count > 0 && !viewItems.getViewItem(0).isLoading));

                //reset
                haveSun = haveMoon = false;

                //go through needed items while sun or moon not found
                for(index = 0; index < (forSubItems ? 1 : count) && (!haveSun || !haveMoon); index++)
                {
                    //remember current item
                    Item currentItem = viewItems.getViewItem(index);
                    if(currentItem != null)
                    {
                        //if for sub items
                        if(forSubItems)
                        {
                            //go through each view
                            for(index2 = 0; index2 < currentItem.views.length && (!haveSun || !haveMoon); index2++)
                            {
                                //check if current is sun or moon
                                noradId = currentItem.views[index2].noradId;
                                haveSun = haveSun || (noradId == Universe.IDs.Sun);
                                haveMoon = haveMoon || (noradId == Universe.IDs.Moon);
                            }
                        }
                        else
                        {
                            //check if current is sun or moon
                            noradId = currentItem.id;
                            haveSun = haveSun || (noradId == Universe.IDs.Sun);
                            haveMoon = haveMoon || (noradId == Universe.IDs.Moon);
                        }
                    }
                }
            }
        }

        //Sub item list adapter
        private static class SubItemListAdapter extends SubItemBaseListAdapter
        {
            private final boolean haveSun;
            private final boolean haveMoon;

            public SubItemListAdapter(Context context, double julianDate, CalculateDataBase[] items, int widthDp, boolean haveSun, boolean haveMoon)
            {
                super(context, julianDate, items, (Settings.getMaterialTheme(context) ? R.layout.calculate_view_material_item : R.layout.calculate_view_item), R.id.View_Title_Text, R.id.View_Progress_Group, R.id.View_Data_Group, R.id.View_Item_Divider, widthDp);
                this.haveSun = haveSun;
                this.haveMoon = haveMoon;
            }

            @Override
            public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent)
            {
                boolean isSun;
                boolean isMoon;
                boolean showPhase = (widthDp >= EXTENDED_COLUMN_1_SHORT_WIDTH_DP);
                boolean showIllumination = (widthDp >= EXTENDED_COLUMN_2_SHORT_WIDTH_DP);
                TextView phaseText;
                TextView illuminationText;
                CalculateDataBase currentData = dataItems[childPosition];

                convertView = super.getChildView(groupPosition, childPosition, isLastChild, convertView, parent);
                if(currentData instanceof CalculateViewsTask.ViewData)
                {
                    CalculateViewsTask.ViewData currentViewData = (CalculateViewsTask.ViewData)currentData;
                    isSun = (currentData.noradId == Universe.IDs.Sun);
                    isMoon = (currentData.noradId == Universe.IDs.Moon);
                    ((TextView)convertView.findViewById(R.id.View_Az_Text)).setText(Globals.getDegreeString(currentViewData.azimuth));
                    ((TextView)convertView.findViewById(R.id.View_El_Text)).setText(Globals.getDegreeString(currentViewData.elevation));
                    ((TextView)convertView.findViewById(R.id.View_Range_Text)).setText(!Float.isInfinite(currentViewData.rangeKm) ? Globals.getKmUnitValueString(currentViewData.rangeKm, 0) : "");
                    phaseText = convertView.findViewById(R.id.View_Phase_Text);
                    if(phaseText != null)
                    {
                        phaseText.setText(currentViewData.phaseName);
                        phaseText.setVisibility(showPhase && (isSun || isMoon) ? View.VISIBLE : (showPhase && (haveSun || haveMoon)) ? View.INVISIBLE : View.GONE);
                    }
                    illuminationText = convertView.findViewById(R.id.View_Illumination_Text);
                    if(illuminationText != null)
                    {
                        illuminationText.setText(Globals.getIlluminationString(currentViewData.illumination));
                        illuminationText.setVisibility(showIllumination && isMoon ? View.VISIBLE : (showIllumination && haveMoon) ? View.INVISIBLE : View.GONE);
                    }
                }

                return(convertView);
            }
        }
    }

    //Passes
    public static abstract class Passes
    {
        //Item
        public static class Item extends CalculateService.PassData
        {
            private Drawable icon;

            public boolean isLoading;
            public boolean tleIsAccurate;
            public View timeStartUnder;
            public View elMaxUnder;
            public View progressBar;
            public AppCompatImageView nameImage;
            public TextView nameText;
            public TextView timeStartText;
            public TextView timeEndText;
            public TextView timeDurationText;
            public TextView elMaxText;
            public LinearProgressIndicator progressStatusBar;
            public LinearLayout dataGroup;

            private Item(int index, double azStart, double azEnd, double azTravel, double elMax, double closestAz, double closetEl, boolean calculating, boolean foundPass, boolean finishedCalculating, boolean foundPassStart, boolean usePathProgress, boolean usePassQuality, Calendar startTime, Calendar endTime, String duration, Parcelable[] views, Parcelable[] views2, Calculations.SatelliteObjectType sat, Calculations.SatelliteObjectType sat2, double illumination, String phaseName, boolean tleAccurate)
            {
                super(index, azStart, azEnd, azTravel, elMax, closestAz, closetEl, calculating, foundPass, finishedCalculating, foundPassStart, usePathProgress, usePassQuality, startTime, endTime, duration, views, views2, sat, sat2, illumination, phaseName);
                this.isLoading = false;
                this.icon = null;
                this.nameImage = null;
                this.nameText = null;
                this.progressBar = null;
                this.progressStatusBar = null;
                this.dataGroup = null;
                this.timeStartText = null;
                this.timeStartUnder = null;
                this.timeEndText = null;
                this.timeDurationText = null;
                this.elMaxText = null;
                this.elMaxUnder = null;
                this.tleIsAccurate = tleAccurate;
            }
            public Item(Context context, int index, Database.SatelliteData currentSatellite, Database.SatelliteData currentSatellite2, boolean usePathProgress, boolean usePassQuality, boolean loading)
            {
                this(index, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, false, false, false, false, usePathProgress, usePassQuality, null, null, "", null, null, (currentSatellite != null ? currentSatellite.satellite : null), null, 0, null, false);
                isLoading = loading;

                if(currentSatellite != null)
                {
                    id = currentSatellite.getSatelliteNum();
                    orbitalType = orbital2Type = currentSatellite.getOrbitalType();
                    icon = Globals.getOrbitalIcon(context, MainActivity.getObserver(), id, orbitalType);
                    name = currentSatellite.getName();
                    ownerCode = currentSatellite.getOwnerCode();
                    satellite = currentSatellite.satellite;
                    tleIsAccurate = currentSatellite.getTLEIsAccurate();
                }
                if(currentSatellite2 != null)
                {
                    satellite2 = currentSatellite2.satellite;
                    orbital2Type = currentSatellite2.getOrbitalType();
                }
            }
            public Item(Context context, int index, Database.SatelliteData currentSatellite, Database.SatelliteData currentSatellite2, boolean loading)
            {
                this(context, index, currentSatellite, currentSatellite2, false, false, loading);
            }
            public Item(Context context, int index, Database.SatelliteData currentSatellite, boolean loading)
            {
                this(context, index, currentSatellite, null, false, false, loading);
            }
            public Item(CalculateService.PassData passData)
            {
                this(passData.listIndex, passData.passAzStart, passData.passAzEnd, passData.passAzTravel, passData.passElMax, passData.passClosestAz, passData.passClosestEl, passData.passCalculating, passData.passCalculated, passData.passCalculateFinished, passData.passStartFound, passData.showPathProgress, passData.showPassQuality, passData.passTimeStart, passData.passTimeEnd, passData.passDuration, passData.passViews, passData.passViews2, passData.satellite, passData.satellite2, passData.illumination, passData.phaseName, true);
                name = passData.name;
                ownerCode = passData.ownerCode;
                owner2Code = passData.owner2Code;
                orbitalType = passData.orbitalType;
                orbital2Type = passData.orbital2Type;
                illumination = passData.illumination;
                phaseName = passData.phaseName;
            }

            public void setLoading(boolean loading, boolean tleIsAccurate)
            {
                isLoading = loading && tleIsAccurate;

                if(progressBar != null)
                {
                    progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
                }
                if(dataGroup != null)
                {
                    dataGroup.setVisibility(loading || !tleIsAccurate ? View.GONE : View.VISIBLE);
                }
            }

            public boolean inUnknownPassTimeStartNow()
            {
                Calendar timeNow = Globals.getGMTTime();
                return(!passStartFound && passTimeStart != null && timeNow.after(passTimeStart) && (passTimeEnd == null || timeNow.before(passTimeEnd)));
            }

            public void updateDisplays(Context context, TimeZone zone)
            {
                long timeMs;
                float elapsedPercent;

                if(context != null)
                {
                    if(nameImage != null && icon != null)
                    {
                        nameImage.setBackgroundDrawable(icon);
                    }

                    if(nameText != null)
                    {
                        nameText.setText(name);
                    }

                    if(timeStartText != null)
                    {
                        timeStartText.setText(inUnknownPassTimeStartNow() ? context.getResources().getString(R.string.title_now) : !passStartFound ? Globals.getUnknownString(context) : Globals.getDateString(context, passTimeStart, zone, true, false));
                    }

                    if(timeStartUnder != null)
                    {
                        timeMs = System.currentTimeMillis();
                        timeStartUnder.setVisibility(inUnknownPassTimeStartNow() || (passTimeStart != null && passStartFound && timeMs >= passTimeStart.getTimeInMillis() && (passTimeEnd == null || timeMs <= passTimeEnd.getTimeInMillis())) ? View.VISIBLE : View.GONE);
                    }

                    if(timeEndText != null)
                    {
                        timeEndText.setText(!passStartFound ? Globals.getUnknownString(context) : Globals.getDateString(context, passTimeEnd, zone, true, false));
                    }

                    if(timeDurationText != null)
                    {
                        timeDurationText.setText(!passStartFound && passTimeStart == null ? Globals.getUnknownString(context) : Globals.getTimeBetween(context, passTimeStart, passTimeEnd));
                    }

                    if(elMaxText != null)
                    {
                        elMaxText.setText(isKnownPassElevationMax() ? Globals.getDegreeString(passElMax) : "");
                    }

                    if(elMaxUnder != null)
                    {
                        elMaxUnder.setBackgroundColor(passElMax == Double.MAX_VALUE ? Color.TRANSPARENT : passElMax >= 60 ? Color.GREEN : passElMax >= 30 ? Color.YELLOW : Color.RED);
                    }

                    if(progressStatusBar != null)
                    {
                        elapsedPercent = (showPathProgress ? getPassProgressPercent(Globals.getGMTTime()) : Float.MAX_VALUE);
                        progressStatusBar.setVisibility(elapsedPercent != Float.MAX_VALUE ? View.VISIBLE : View.GONE);
                        if(elapsedPercent != Float.MAX_VALUE)
                        {
                            progressStatusBar.setProgress((int)elapsedPercent);
                        }
                    }
                }
            }
        }

        //Item holder
        public static class ItemHolder extends ItemHolderBase
        {
            public final View elMaxUnder;
            public final View timeStartUnder;

            public ItemHolder(View itemView, int timeStartTextId, int timeStartUnderId, int timeEndTextId, int timeDurationTextId, int elMaxTextId, int elMaxUnderId, int calculateProgressBarId, int dataGroupId)
            {
                super(itemView, timeStartTextId, timeDurationTextId, timeEndTextId, elMaxTextId, -1, dataGroupId, -1, -1, -1, calculateProgressBarId, -1);
                elMaxUnder = itemView.findViewById(elMaxUnderId);
                timeStartUnder = itemView.findViewById(timeStartUnderId);
            }
        }

        //Item list adapter
        public static class ItemListAdapter extends Selectable.ListBaseAdapter
        {
            private final Current.Items pathItems;

            public ItemListAdapter(Context context, int page, Item[] savedItems, Database.SatelliteData[] satellites)
            {
                super(context);

                pathItems = new Current.Items(MainActivity.Groups.Calculate, page);

                //if there are saved items
                if(savedItems != null && savedItems.length > 0)
                {
                    //set as saved items
                    pathItems.set(savedItems);
                    hasItems = true;
                }
                else
                {
                    //if satellites are not set
                    hasItems = (satellites != null);

                    //set as empty
                    pathItems.set(new Item[]{new Item(context, 0, null, true)});
                }

                //remember strings and layout ID
                this.itemsRefID = (usingMaterial ? R.layout.calculate_pass_material_item : R.layout.calculate_pass_item);

                //ID stays the same
                this.setHasStableIds(true);
            }

            @Override
            public int getItemCount()
            {
                return(pathItems.getCount());
            }

            @Override
            public Item getItem(int position)
            {
                return(pathItems.getPassItem(position));
            }

            @Override
            public long getItemId(int position)
            {
                Item currentItem = pathItems.getPassItem(position);
                return(currentItem != null ? currentItem.listIndex : Integer.MIN_VALUE);
            }

            @Override
            protected void setHeader(View header)
            {
                super.setHeader(header);
                header.setVisibility(View.VISIBLE);
            }

            @Override
            protected boolean showColumnTitles(int page)
            {
                return(true);
            }

            @Override
            protected void setColumnTitles(ViewGroup listColumns, TextView categoryText, int page)
            {
                String text;
                boolean showEnd = (widthDp >= EXTENDED_COLUMN_1_WIDTH_DP);
                TextView passTimeEndText;
                Resources res = currentContext.getResources();

                listColumns.findViewById(R.id.Pass_Data_Group).setVisibility(View.VISIBLE);
                ((TextView)listColumns.findViewById(R.id.Pass_Time_Start_Text)).setText(R.string.title_start);
                passTimeEndText = listColumns.findViewById(R.id.Pass_Time_End_Text);
                if(passTimeEndText != null)
                {
                    passTimeEndText.setText(R.string.title_end);
                    passTimeEndText.setVisibility(showEnd ? View.VISIBLE : View.GONE);
                }
                ((TextView)listColumns.findViewById(R.id.Pass_Time_Duration_Text)).setText(R.string.title_duration);
                text = res.getString(R.string.abbrev_elevation) + " " + res.getString(R.string.title_max);
                ((TextView)listColumns.findViewById(R.id.Pass_El_Max_Text)).setText(text);
                listColumns.findViewById(R.id.Pass_El_Max_Under).setVisibility(View.GONE);

                super.setColumnTitles(listColumns, categoryText, page);
            }

            @Override
            public @NonNull ItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
            {
                View itemView = LayoutInflater.from(parent.getContext()).inflate(this.itemsRefID, parent, false);
                ItemHolder itemHolder = new ItemHolder(itemView, R.id.Pass_Time_Start_Text, R.id.Pass_Time_Start_Under, R.id.Pass_Time_End_Text, R.id.Pass_Time_Duration_Text, R.id.Pass_El_Max_Text, R.id.Pass_El_Max_Under, R.id.Pass_Calculate_Progress_Bar, R.id.Pass_Data_Group);

                setItemSelector(itemView);
                setViewClickListeners(itemView, itemHolder);
                return(itemHolder);
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position)
            {
                boolean showEnd = (widthDp >= EXTENDED_COLUMN_1_WIDTH_DP);
                Item currentItem = pathItems.getPassItem(position);
                ItemHolder itemHolder = (ItemHolder)holder;

                //get displays
                currentItem.nameImage = itemHolder.nameImage;
                currentItem.nameText = itemHolder.nameText;
                itemHolder.progressGroup.setVisibility(View.VISIBLE);
                currentItem.progressBar = itemHolder.progressGroup;
                currentItem.progressStatusBar = null;
                currentItem.dataGroup = itemHolder.dataGroup;
                if(currentItem.dataGroup != null)
                {
                    currentItem.dataGroup.setVisibility(currentItem.tleIsAccurate ? View.VISIBLE : View.GONE);
                }
                currentItem.timeStartText = itemHolder.dataGroup1Text;
                currentItem.timeStartUnder = itemHolder.timeStartUnder;
                currentItem.timeDurationText = itemHolder.dataGroup2Text;
                currentItem.timeEndText = itemHolder.dataGroup3Text;
                if(currentItem.timeEndText != null)
                {
                    currentItem.timeEndText.setVisibility(showEnd ? View.VISIBLE : View.GONE);
                }
                currentItem.elMaxText = itemHolder.dataGroup4Text;
                currentItem.elMaxUnder = itemHolder.elMaxUnder;
                currentItem.elMaxUnder.setVisibility(View.GONE);

                //set displays
                currentItem.setLoading(!hasItems || !currentItem.passCalculateFinished, true);
                currentItem.updateDisplays(currentContext, MainActivity.getTimeZone());
            }

            @Override
            protected void onItemNonEditClick(Selectable.ListItem item, int pageNum)
            {
                final Item currentItem = (Item)item;

                //if pass has been calculated
                if(currentItem.passCalculated)
                {
                    final TimeZone defaultZone = TimeZone.getDefault();
                    final TimeZone currentZone = MainActivity.getTimeZone();
                    final long midPassTimeMs = currentItem.getMidPass();
                    final long currentTimeMs = System.currentTimeMillis();
                    final long passDurationMs = (currentItem.passTimeStart != null && currentItem.passTimeEnd != null ? (currentItem.passTimeEnd.getTimeInMillis() - currentItem.passTimeStart.getTimeInMillis()) : 0);
                    final boolean haveSatellite2 = (currentItem.satellite2 != null);
                    final boolean isSun = (item.id == Universe.IDs.Sun || (haveSatellite2 && currentItem.satellite2.getSatelliteNum() == Universe.IDs.Sun));
                    final boolean isMoon = (item.id == Universe.IDs.Moon || (haveSatellite2 && currentItem.satellite2.getSatelliteNum() == Universe.IDs.Moon));
                    final boolean useLocalZone = (!defaultZone.equals(currentZone) && defaultZone.getOffset(currentTimeMs) != currentZone.getOffset(currentTimeMs));
                    final Calculations.ObserverType location = MainActivity.getObserver();
                    final Resources res = currentContext.getResources();
                    final String unknownString = Globals.getUnknownString(currentContext);
                    final String azAbbrevString = res.getString(R.string.abbrev_azimuth);
                    final String startString = res.getString(R.string.title_start);
                    final String endString = res.getString(R.string.title_end);
                    final String localString = res.getString(R.string.title_local);
                    final String azTravelString = azAbbrevString + " " + res.getString(R.string.title_travel);
                    final String elMaxString = res.getString(R.string.abbrev_elevation) + " " + res.getString(R.string.title_max);
                    final Drawable orbital1Icon = Globals.getOrbitalIcon(currentContext, location, currentItem.id, currentItem.orbitalType, midPassTimeMs, 0);
                    final Drawable orbital2Icon = (haveSatellite2 ? Globals.getOrbitalIcon(currentContext, location, currentItem.satellite2.getSatelliteNum(), currentItem.orbital2Type) : null);
                    final TextView[] detailTexts;
                    final TextView[] detailTitles;

                    //create dialog
                    final ItemDetailDialog detailDialog = new ItemDetailDialog(currentContext, listInflater, new int[]{currentItem.id, currentItem.id2}, currentItem.name, new String[]{currentItem.ownerCode, currentItem.owner2Code}, new Drawable[]{orbital1Icon, orbital2Icon}, itemDetailButtonClickListener);
                    final View passProgressLayout = detailDialog.findViewById(R.id.Item_Detail_Progress_Layout);
                    final LinearProgressIndicator passProgress = detailDialog.findViewById(R.id.Item_Detail_Progress);
                    final TextView passProgressText = detailDialog.findViewById(R.id.Item_Detail_Progress_Text);
                    final Graph elevationGraph = detailDialog.findViewById(R.id.Item_Detail_Graph);
                    detailDialog.addGroup(R.string.title_pass, R.string.title_time_until, R.string.title_duration, R.string.title_placeholder, R.string.title_placeholder, (useLocalZone ? R.string.title_placeholder : R.string.empty), (useLocalZone ? R.string.title_placeholder : R.string.empty), R.string.title_placeholder, R.string.title_placeholder, R.string.title_placeholder, R.string.title_placeholder);
                    detailDialog.moveProgress();
                    if(haveSatellite2)
                    {
                        detailDialog.addGroup(R.string.title_closest_intersection_delta, R.string.abbrev_azimuth, R.string.abbrev_elevation);
                    }
                    if(isSun || isMoon)
                    {
                        detailDialog.addGroup(R.string.title_phase, R.string.title_name, (isMoon ? R.string.title_illumination : R.string.empty));
                    }

                    detailTexts = detailDialog.getDetailTexts();
                    detailTitles = detailDialog.getDetailTitles();
                    if(passDurationMs > 0)
                    {
                        detailDialog.addButton(pageNum, item.id, currentItem, DetailButtonType.Graph);
                    }
                    detailDialog.addButton(pageNum, item.id, currentItem, DetailButtonType.LensView).setVisibility(SensorUpdate.havePositionSensors(currentContext) ? View.VISIBLE : View.GONE);
                    if(!haveSatellite2)
                    {
                        detailDialog.addButton(pageNum, item.id, currentItem, DetailButtonType.Notify);
                        if(have3dPreview(item.id))
                        {
                            detailDialog.addButton(pageNum, item.id, currentItem, DetailButtonType.Preview3d);
                        }
                        detailDialog.addButton(pageNum, item.id, currentItem, DetailButtonType.Info);
                    }
                    detailDialog.show();

                    //start timer
                    detailDialog.setUpdateTask(new TimerTask()
                    {
                        @Override
                        public void run()
                        {
                            ((Activity)currentContext).runOnUiThread(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    int index;
                                    long elapsedMs;
                                    float elapsedPercent;
                                    String text;
                                    String title;
                                    Calendar timeNow = Globals.getGMTTime();

                                    //go through each display
                                    for(index = 0; index < detailTexts.length; index++)
                                    {
                                        //remember current detail text
                                        TextView currentDetailText = detailTexts[index];
                                        TextView currentTitleText = detailTitles[index];

                                        //reset text and title
                                        text = null;
                                        title = null;

                                        //get text
                                        switch(index)
                                        {
                                            case 0:
                                                title = Globals.getTimeUntilDescription(res, timeNow, currentItem.passTimeStart, currentItem.passTimeEnd);
                                                text = Globals.getTimeUntil(currentContext, res, timeNow, currentItem.passTimeStart, currentItem.passTimeEnd);
                                                break;

                                            case 1:
                                                text = (!currentItem.passDuration.equals("") ? currentItem.passDuration : unknownString);
                                                break;

                                            case 2:
                                                title = startString;
                                                text = (currentItem.inUnknownPassTimeStartNow() ? res.getString(R.string.title_now) : Globals.getDateString(currentContext, currentItem.passTimeStart, currentZone, false));
                                                break;

                                            case 3:
                                                title = endString;
                                                text = Globals.getDateString(currentContext, currentItem.passTimeEnd, currentZone, false);
                                                break;

                                            case 4:
                                                if(useLocalZone)
                                                {
                                                    title = localString + " " + startString;
                                                    text = (currentItem.inUnknownPassTimeStartNow() ? res.getString(R.string.title_now) : Globals.getDateString(currentContext, currentItem.passTimeStart, defaultZone, false));
                                                }
                                                break;

                                            case 5:
                                                if(useLocalZone)
                                                {
                                                    title = localString + " " + endString;
                                                    text = Globals.getDateString(currentContext, currentItem.passTimeEnd, defaultZone, false);
                                                }
                                                break;

                                            case 6:
                                                title = azAbbrevString + " " + startString;
                                                text = (currentItem.passTimeStart != null ? Globals.getAzimuthDirectionString(res, currentItem.passAzStart) : unknownString);
                                                break;

                                            case 7:
                                                title = azAbbrevString + " " + endString;
                                                text = (currentItem.passTimeEnd != null ? Globals.getAzimuthDirectionString(res, currentItem.passAzEnd) : unknownString);
                                                break;

                                            case 8:
                                                title = azTravelString;
                                                text = (currentItem.passTimeEnd != null ? Globals.getDegreeString(currentItem.passAzTravel) : unknownString);
                                                break;

                                            case 9:
                                                title = elMaxString;
                                                text = (currentItem.passElMax != Double.MAX_VALUE ? Globals.getDegreeString(currentItem.passElMax) : unknownString);
                                                break;

                                            case 10:
                                            case 12:
                                                if(haveSatellite2 && index == 10)
                                                {
                                                    text = (currentItem.passClosestAz != Double.MAX_VALUE ? Globals.getDegreeString(currentItem.passClosestAz) : unknownString);
                                                }
                                                else
                                                {
                                                    text = (currentItem.phaseName == null ? "-" : currentItem.phaseName);
                                                }
                                                break;

                                            case 11:
                                            case 13:
                                                if(haveSatellite2 && index == 11)
                                                {
                                                    text = (currentItem.passClosestEl != Double.MAX_VALUE ? Globals.getDegreeString(currentItem.passClosestEl) : unknownString);
                                                }
                                                else
                                                {
                                                    text = Globals.getIlluminationString(currentItem.illumination);
                                                }
                                                break;
                                        }

                                        //if display exists and text is set
                                        if(currentDetailText != null && text != null)
                                        {
                                            //set text
                                            currentDetailText.setText(text.replace("\r\n", ", "));
                                        }

                                        //if display exists and title is set
                                        if(currentTitleText != null && title != null)
                                        {
                                            //set title
                                            title += ":";
                                            currentTitleText.setText(title);
                                        }
                                    }

                                    //if progress layout exists
                                    if(passProgressLayout != null)
                                    {
                                        passProgressLayout.setVisibility(passDurationMs > 0 && timeNow.after(currentItem.passTimeStart) ? View.VISIBLE : View.GONE);
                                    }

                                    //if start time is set, progress displays exist, and duration set
                                    if(currentItem.passTimeStart != null && timeNow.after(currentItem.passTimeStart) && passProgress != null && passProgressText != null && passDurationMs > 0)
                                    {
                                        //if before end
                                        if(timeNow.before(currentItem.passTimeEnd))
                                        {
                                            elapsedMs = currentItem.passTimeEnd.getTimeInMillis() - timeNow.getTimeInMillis(); //currentTimeMs;
                                            elapsedPercent = (100 - ((elapsedMs / (float)passDurationMs) * 100));
                                        }
                                        else
                                        {
                                            elapsedPercent = 100;
                                        }

                                        passProgress.setProgress((int)elapsedPercent);
                                        text = Globals.getNumberString(elapsedPercent, 1) + "%";
                                        passProgressText.setText(text);
                                    }

                                    //update graph
                                    if(elevationGraph != null)
                                    {
                                        elevationGraph.setSelectedX(Calculations.julianDateCalendar(timeNow));
                                    }
                                }
                            });
                        }
                    });
                }
            }

            //Updates status of having items
            public void updateHasItems()
            {
                hasItems = (pathItems.getCount() > 0);
            }
        }
    }

    //Coordinates
    public static abstract class Coordinates
    {
        //Item
        public static class Item extends Selectable.ListItem
        {
            public boolean isLoading;
            public double julianDate;
            public Calendar time;
            public View progressGroup;
            public TextView speedText;
            public TextView timeText;
            public TextView latitudeText;
            public TextView longitudeText;
            public TextView altitudeText;
            public ExpandingListView subList;
            public LinearLayout dataGroup;
            public final CalculateCoordinatesTask.CoordinateData[] coordinates;

            public Item(int index, int coordinateCount)
            {
                super(Integer.MAX_VALUE, index, false, false, false, false);

                isLoading = (coordinateCount < 1);
                if(isLoading)
                {
                    coordinateCount = 1;
                }
                coordinates = new CalculateCoordinatesTask.CoordinateData[coordinateCount];
                for(index = 0; index < coordinates.length; index++)
                {
                    coordinates[index] = new CalculateCoordinatesTask.CoordinateData();
                }

                julianDate = Double.MAX_VALUE;
                time = Globals.julianDateToCalendar(julianDate);
                speedText = null;
                timeText = null;
                latitudeText = null;
                longitudeText = null;
                altitudeText = null;
                dataGroup = null;
                progressGroup = null;
            }

            public void updateDisplays(TimeZone zone, int widthDp)
            {
                String text;
                Context context = (latitudeText != null ? latitudeText.getContext() : longitudeText != null ? longitudeText.getContext() : subList != null ? subList.getContext() : null);
                Resources res = (context != null ? context.getResources() : null);
                CalculateCoordinatesTask.CoordinateData currentCoordinate = coordinates[0];

                if(latitudeText != null)
                {
                    latitudeText.setText(currentCoordinate.latitude != Float.MAX_VALUE ? Globals.getLatitudeDirectionString(res, currentCoordinate.latitude, 2) : "-");
                }
                if(longitudeText != null)
                {
                    longitudeText.setText(currentCoordinate.longitude != Float.MAX_VALUE ? Globals.getLongitudeDirectionString(res, currentCoordinate.longitude, 2) : "-");
                }
                if(altitudeText != null)
                {
                    altitudeText.setText(currentCoordinate.altitudeKm != Float.MAX_VALUE && !Float.isInfinite(currentCoordinate.altitudeKm) ? Globals.getKmUnitValueString(currentCoordinate.altitudeKm, 0) : "-");
                }
                if(speedText != null)
                {
                    text = (currentCoordinate.speedKms != Double.MAX_VALUE ? Globals.getKmUnitValueString(currentCoordinate.speedKms) : "-");
                    speedText.setText(text);
                }
                if(timeText != null)
                {
                    if(isLoading)
                    {
                        timeText.setText("");
                    }
                    else if(time != null)
                    {
                        timeText.setText(Globals.getDateString(timeText.getContext(), time, zone, true));
                    }
                }
                if(subList != null && context != null && coordinates.length > 1)
                {
                    subList.setAdapter(new SubItemListAdapter(context, julianDate, coordinates, widthDp));
                }
            }

            public void setLoading(boolean loading, boolean tleIsAccurate)
            {
                isLoading = loading && tleIsAccurate;

                if(progressGroup != null)
                {
                    progressGroup.setVisibility(isLoading ? View.VISIBLE : View.GONE);
                }
                if(dataGroup != null)
                {
                    dataGroup.setVisibility(loading || !tleIsAccurate ? View.GONE : View.VISIBLE);
                }
                if(subList != null)
                {
                    subList.setVisibility(loading ? View.GONE : View.VISIBLE);
                }
            }
        }

        //Item holder
        public static class ItemHolder extends ItemHolderBase
        {
            private ItemHolder(View itemView, int latTextId, int lonTextId, int altTextId, int progressGroupId, int dataGroupId, int speedTextId, int timeTextId, int subListId)
            {
                super(itemView, latTextId, lonTextId, altTextId, speedTextId, -1, dataGroupId, -1, timeTextId, -1, progressGroupId, subListId);
            }
            public ItemHolder(View itemView, int latTextId, int lonTextId, int altTextId, int speedTextId, int progressGroupId, int dataGroupId, int timeTextId)
            {
                this(itemView, latTextId, lonTextId, altTextId, progressGroupId, dataGroupId, speedTextId, timeTextId, -1);
            }
            public ItemHolder(View itemView, int progressGroupId, int subListId)
            {
                this(itemView, -1, -1, -1, progressGroupId, -1, -1, -1, subListId);
            }
        }

        //Item list adapter
        public static class ItemListAdapter extends Selectable.ListBaseAdapter
        {
            private final TimeZone currentZone;
            private final Current.Items coordinateItems;

            public ItemListAdapter(Context context, Item[] savedItems, int multiCount, TimeZone zone)
            {
                super(context);

                int itemId = (usingMaterial ? R.layout.calculate_coordinates_material_item : R.layout.calculate_coordinates_item);

                hasItems = false;
                currentZone = zone;
                coordinateItems = new Current.Items(MainActivity.Groups.Calculate, PageType.Coordinates);

                //if there are saved items
                if(savedItems != null && savedItems.length > 0)
                {
                    //set as saved items
                    coordinateItems.set(savedItems);
                    hasItems = true;
                }
                else
                {
                    //set as empty
                    coordinateItems.set(new Item[]{new Coordinates.Item(0, 0)});
                }

                //remember layout ID
                forSubItems = (multiCount > 1);
                this.itemsRefID = (forSubItems ? R.layout.calculate_multi_item : itemId);
                if(forSubItems)
                {
                    this.itemsRefSubId = itemId;
                }

                //ID stays the same
                this.setHasStableIds(true);
            }

            @Override
            public int getItemCount()
            {
                return(coordinateItems.getCount());
            }

            @Override
            public Item getItem(int position)
            {
                return(coordinateItems.getCoordinateItem(position));
            }

            @Override
            public long getItemId(int position)
            {
                Item currentItem = coordinateItems.getCoordinateItem(position);
                return(currentItem != null ? currentItem.listIndex : Integer.MIN_VALUE);
            }

            @Override
            protected void setHeader(View header)
            {
                super.setHeader(header);
                header.setVisibility(View.VISIBLE);
            }

            @Override
            protected boolean showColumnTitles(int page)
            {
                return(true);
            }

            @Override
            protected void setColumnTitles(ViewGroup listColumns, TextView categoryText, int page)
            {
                String text;
                boolean showSpeed = (widthDp >= EXTENDED_COLUMN_1_WIDTH_DP);
                TextView timeText;
                TextView speedText;
                Resources res = currentContext.getResources();

                if(!forSubItems)
                {
                    timeText = listColumns.findViewById(R.id.Coordinate_Time_Text);
                    timeText.setText(R.string.title_time);
                    timeText.setVisibility(View.VISIBLE);
                }
                speedText = listColumns.findViewById(R.id.Coordinate_Speed_Text);
                if(speedText != null)
                {
                    text = res.getString(R.string.abbrev_velocity) + " (" + Globals.getKmLabel(res) + "/" + res.getString(R.string.abbrev_seconds_lower) + ")";
                    speedText.setText(text);
                    speedText.setVisibility(showSpeed ? View.VISIBLE : View.GONE);
                }
                listColumns.findViewById(R.id.Coordinate_Progress_Group).setVisibility(View.GONE);
                listColumns.findViewById(R.id.Coordinate_Data_Group).setVisibility(View.VISIBLE);
                ((TextView)listColumns.findViewById(R.id.Coordinate_Latitude_Text)).setText(R.string.abbrev_latitude);
                ((TextView)listColumns.findViewById(R.id.Coordinate_Longitude_Text)).setText(R.string.abbrev_longitude);
                text = res.getString(R.string.abbrev_altitude) + " (" + Globals.getKmLabel(res) + ")";
                ((TextView)listColumns.findViewById(R.id.Coordinate_Altitude_Text)).setText(text);

                super.setColumnTitles(listColumns, categoryText, page);
            }

            @Override
            public @NonNull Coordinates.ItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
            {
                View itemView = LayoutInflater.from(parent.getContext()).inflate(this.itemsRefID, parent, false);
                ItemHolder itemHolder;

                if(forSubItems)
                {
                    itemHolder = new ItemHolder(itemView, R.id.Multi_Item_Progress_Group, R.id.Multi_Item_List);
                }
                else
                {
                    itemHolder = new ItemHolder(itemView, R.id.Coordinate_Latitude_Text, R.id.Coordinate_Longitude_Text, R.id.Coordinate_Altitude_Text, R.id.Coordinate_Speed_Text, R.id.Coordinate_Progress_Group, R.id.Coordinate_Data_Group, R.id.Coordinate_Time_Text);
                }

                setItemSelector(itemView);
                setViewClickListeners(itemView, itemHolder);
                return(itemHolder);
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position)
            {
                boolean showSpeed = (widthDp >= EXTENDED_COLUMN_1_WIDTH_DP);
                Item currentItem = coordinateItems.getCoordinateItem(position);
                ItemHolder itemHolder = (ItemHolder)holder;

                //get displays
                currentItem.timeText = itemHolder.timeText;
                if(currentItem.timeText != null)
                {
                    currentItem.timeText.setVisibility(View.VISIBLE);
                }

                currentItem.subList = itemHolder.subList;
                currentItem.progressGroup = itemHolder.progressGroup;
                currentItem.dataGroup = itemHolder.dataGroup;
                if(currentItem.dataGroup != null)
                {
                    currentItem.dataGroup.setVisibility(View.GONE);
                }
                currentItem.latitudeText = itemHolder.dataGroup1Text;
                currentItem.longitudeText = itemHolder.dataGroup2Text;
                currentItem.altitudeText = itemHolder.dataGroup3Text;
                currentItem.speedText = itemHolder.dataGroup4Text;
                if(currentItem.speedText != null)
                {
                    currentItem.speedText.setVisibility(showSpeed ? View.VISIBLE : View.GONE);
                }

                //update displays
                currentItem.setLoading(!hasItems, true);
                currentItem.updateDisplays(currentZone, widthDp);
            }

            //Updates status of having items
            public void updateHasItems()
            {
                int count = coordinateItems.getCount();
                hasItems = (count > 1 || (count > 0 && !coordinateItems.getCoordinateItem(0).isLoading));
            }
        }

        //Sub item list adapter
        private static class SubItemListAdapter extends SubItemBaseListAdapter
        {
            public SubItemListAdapter(Context context, double julianDate, CalculateDataBase[] items, int widthDp)
            {
                super(context, julianDate, items, (Settings.getMaterialTheme(context) ? R.layout.calculate_coordinates_material_item : R.layout.calculate_coordinates_item), R.id.Coordinate_Title_Text, R.id.Coordinate_Progress_Group, R.id.Coordinate_Data_Group, R.id.Coordinate_Divider, widthDp);
            }

            @Override
            public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent)
            {
                Context context = parent.getContext();
                Resources res = context.getResources();
                boolean showSpeed = (widthDp >= EXTENDED_COLUMN_1_WIDTH_DP);
                TextView speedText;
                CalculateDataBase currentData = dataItems[childPosition];

                convertView = super.getChildView(groupPosition, childPosition, isLastChild, convertView, parent);
                if(currentData instanceof CalculateCoordinatesTask.CoordinateData)
                {
                    CalculateCoordinatesTask.CoordinateData currentCoordinateData = (CalculateCoordinatesTask.CoordinateData)currentData;
                    ((TextView)convertView.findViewById(R.id.Coordinate_Latitude_Text)).setText(Globals.getLatitudeDirectionString(res, currentCoordinateData.latitude, 2));
                    ((TextView)convertView.findViewById(R.id.Coordinate_Longitude_Text)).setText(Globals.getLongitudeDirectionString(res, currentCoordinateData.longitude, 2));
                    ((TextView)convertView.findViewById(R.id.Coordinate_Altitude_Text)).setText(!Float.isInfinite(currentCoordinateData.altitudeKm) ? Globals.getKmUnitValueString(currentCoordinateData.altitudeKm, 0) : "");
                    speedText = convertView.findViewById(R.id.Coordinate_Speed_Text);
                    if(speedText != null)
                    {
                        speedText.setText(Globals.getKmUnitValueString(currentCoordinateData.speedKms));
                        speedText.setVisibility(showSpeed ? View.VISIBLE : View.GONE);
                    }
                }

                return(convertView);
            }
        }
    }

    //Sub item base list adapter
    public static class SubItemBaseListAdapter extends BaseExpandableListAdapter
    {
        protected final int widthDp;
        private final boolean usingMaterial;
        private final int layoutId;
        private final int titleId;
        private final int progressId;
        private final int dataId;
        private final int dividerId;
        private final double dataJulianDate;
        private final LayoutInflater listInflater;
        protected final CalculateDataBase[] dataItems;

        public SubItemBaseListAdapter(Context context, double julianDate, CalculateDataBase[] items, int layoutId, int titleId, int progressId, int dataId, int dividerId, int widthDp)
        {
            this.usingMaterial = Settings.getMaterialTheme(context);
            this.listInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            this.layoutId = layoutId;
            this.titleId = titleId;
            this.progressId = progressId;
            this.dataId = dataId;
            this.dividerId = dividerId;
            this.dataJulianDate = julianDate;
            this.dataItems = items;
            this.widthDp = widthDp;
        }

        @Override
        public int getGroupCount()
        {
            return(dataJulianDate != Double.MAX_VALUE ? 1 : 0);
        }

        @Override
        public int getChildrenCount(int groupPosition)
        {
            return(dataItems.length);
        }

        @Override
        public Object getGroup(int groupPosition)
        {
            return(dataJulianDate);
        }

        @Override
        public Object getChild(int groupPosition, int childPosition)
        {
            return(dataItems[childPosition]);
        }

        @Override
        public long getGroupId(int groupPosition)
        {
            return(-1);
        }

        @Override
        public long getChildId(int groupPosition, int childPosition)
        {
            return(-1);
        }

        @Override
        public boolean hasStableIds()
        {
            return(true);
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent)
        {
            TextView groupTitleText;
            AppCompatImageView groupTitleImage;
            AppCompatImageView groupIndicatorImage;

            if(convertView == null)
            {
                convertView = listInflater.inflate((usingMaterial ? R.layout.side_menu_list_material_group : R.layout.side_menu_list_group), parent, false);
            }
            groupTitleImage = convertView.findViewById(R.id.Group_Title_Image);
            if(usingMaterial && groupTitleImage != null)
            {
                groupTitleImage.setVisibility(View.GONE);
            }
            groupTitleText = convertView.findViewById(R.id.Group_Title_Text);
            groupTitleText.setText(Globals.getDateString(parent.getContext(), Globals.julianDateToCalendar(dataJulianDate), MainActivity.getTimeZone(), true).replace("\r\n", " "));
            groupIndicatorImage = convertView.findViewById(R.id.Group_Indicator_Image);
            if(usingMaterial && groupIndicatorImage != null)
            {
                groupIndicatorImage.setImageDrawable(Globals.getDrawable(listInflater.getContext(), (isExpanded ? R.drawable.ic_expand_less : R.drawable.ic_expand_more), true));
            }

            return(convertView);
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent)
        {
            Context context = parent.getContext();
            View dividerView;
            TextView titleText;
            CalculateDataBase currentData = dataItems[childPosition];
            Database.DatabaseSatellite currentOrbital = Database.getOrbital(context, currentData.noradId);

            if(convertView == null)
            {
                convertView = listInflater.inflate(layoutId, parent, false);
            }
            titleText = convertView.findViewById(titleId);
            titleText.setText(currentOrbital != null ? currentOrbital.getName() : null);
            titleText.setVisibility(View.VISIBLE);
            convertView.findViewById(progressId).setVisibility(View.GONE);
            convertView.findViewById(dataId).setVisibility(View.VISIBLE);
            convertView.setBackground(Globals.getListItemStateSelector(context, false));
            dividerView = convertView.findViewById(dividerId);
            if(dividerView != null)
            {
                dividerView.setVisibility(View.VISIBLE);
            }

            return(convertView);
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition)
        {
            return(true);
        }
    }

    public interface OnStartCalculationListener
    {
        void onStartCalculation(Bundle params);
    }

    private static final int[] incrementTypes = new int[]{IncrementType.Seconds, IncrementType.Minutes, IncrementType.Hours, IncrementType.Days};

    //Gets increment types
    private static String[] getIncrementTypes(Context context)
    {
        Resources res = context.getResources();
        return(new String[]{res.getString(R.string.title_seconds), res.getString(R.string.title_minutes), res.getString(R.string.title_hours), res.getString(R.string.title_days)});
    }

    //Gets increment type
    private static int getIncrementType(Context context, String increment)
    {
        int index = (increment != null ? Arrays.asList(getIncrementTypes(context)).indexOf(increment) : -1);

        //if a valid index
        if(index >= 0 && index < incrementTypes.length)
        {
            //return source at index
            return(incrementTypes[index]);
        }
        else
        {
            //default
            return(IncrementType.Days);
        }
    }

    //Page view
    public static class Page extends Selectable.ListFragment
    {
        public EditText viewUnitText;
        public EditText intersectionUnitText;
        public EditText elevationMinUnitText;
        public SelectListInterface orbitalList;
        public SelectListInterface orbital2List;
        public SelectListInterface viewUnitList;
        public DateInputView startDateText;
        public DateInputView endDateText;
        public TimeInputView startTimeText;
        public TimeInputView endTimeText;
        public MaterialButton selectButton;
        private OnStartCalculationListener startCalculationListener;
        private boolean[] orbitalIsSelected;

        @Override
        protected int getListColumns(Context context, int page)
        {
            Selectable.ListBaseAdapter listAdapter = getAdapter();
            int widthDp = Globals.getDeviceDp(context, true);

            //if adapter exists
            if(listAdapter != null)
            {
                //update status
                listAdapter.widthDp = widthDp;
            }

            //return default column count
            return(super.getListColumns(context, page));
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
        {
            int group = this.getGroupParam();
            int page = this.getPageParam();
            int subPage = this.getSubPageParam();
            View newView = null;
            Bundle params = this.getArguments();
            Context context = this.getContext();
            Selectable.ListBaseAdapter listAdapter;
            ArrayList<Integer> multiNoradId;
            Database.SatelliteData[] selectedOrbitals;

            if(savedInstanceState == null)
            {
                savedInstanceState = new Bundle();
            }
            if(params == null)
            {
                params = new Bundle();
            }
            multiNoradId = params.getIntegerArrayList(ParamTypes.MultiNoradId);
            savedInstanceState.putInt(Selectable.ParamTypes.PageNumber, page);
            savedInstanceState.putInt(ParamTypes.NoradId, params.getInt(ParamTypes.NoradId));
            savedInstanceState.putInt(ParamTypes.NoradId2, params.getInt(ParamTypes.NoradId2, Universe.IDs.Invalid));
            savedInstanceState.putBooleanArray(ParamTypes.OrbitalIsSelected, params.getBooleanArray(ParamTypes.OrbitalIsSelected));
            savedInstanceState.putIntegerArrayList(ParamTypes.MultiNoradId, multiNoradId);
            savedInstanceState.putLong(ParamTypes.StartDateMs, params.getLong(ParamTypes.StartDateMs));
            savedInstanceState.putLong(ParamTypes.EndDateMs, params.getLong(ParamTypes.EndDateMs));
            savedInstanceState.putDouble(ParamTypes.ElevationMinDegs, params.getDouble(ParamTypes.ElevationMinDegs, 0.0));
            savedInstanceState.putDouble(ParamTypes.IntersectionDegs, params.getDouble(ParamTypes.IntersectionDegs, 0.2));

            switch(page)
            {
                case PageType.View:
                    switch(subPage)
                    {
                        case Globals.SubPageType.Input:
                            savedInstanceState.putInt(ParamTypes.IncrementUnit, params.getInt(ParamTypes.IncrementUnit));
                            savedInstanceState.putInt(ParamTypes.IncrementType, params.getInt(ParamTypes.IncrementType));
                            break;

                        case Globals.SubPageType.List:
                        case Globals.SubPageType.Lens:
                            ViewAngles.Item[] savedItems = (ViewAngles.Item[])PageAdapter.getSavedItems(page);

                            switch(subPage)
                            {
                                case Globals.SubPageType.List:
                                    listAdapter = new ViewAngles.ItemListAdapter(context, savedItems, (multiNoradId != null ? multiNoradId.size() : 0));
                                    setChangeListeners(listAdapter, page);
                                    newView = this.onCreateView(inflater, container, listAdapter, group, page);
                                    break;

                                case Globals.SubPageType.Lens:
                                    selectedOrbitals = getSelectedOrbitals(context, multiNoradId, savedItems);
                                    newView = Current.onCreateLensView(this, inflater, container, selectedOrbitals, savedInstanceState, getUsingConstellations(selectedOrbitals));
                                    break;
                            }
                            break;

                        default:
                            break;
                    }
                    break;

                case PageType.Passes:
                case PageType.Intersection:
                    switch(subPage)
                    {
                        case Globals.SubPageType.List:
                        case Globals.SubPageType.Lens:
                            Passes.Item[] savedItems = (Passes.Item[])PageAdapter.getSavedItems(page);

                            switch(subPage)
                            {
                                case Globals.SubPageType.List:
                                    listAdapter = new Passes.ItemListAdapter(context, page, savedItems, null);
                                    setChangeListeners(listAdapter, page);
                                    newView = this.onCreateView(inflater, container, listAdapter, group, page);
                                    break;

                                case Globals.SubPageType.Lens:
                                    savedInstanceState.putInt(MainActivity.ParamTypes.PathDivisions, 8);
                                    savedInstanceState.putInt(MainActivity.ParamTypes.PassIndex, params.getInt(MainActivity.ParamTypes.PassIndex, 0));
                                    savedInstanceState.putBoolean(MainActivity.ParamTypes.GetPassItems, true);
                                    selectedOrbitals = getSelectedOrbitals(context, multiNoradId, savedItems);
                                    newView = Current.onCreateLensView(this, inflater, container, selectedOrbitals, savedInstanceState, getUsingConstellations(selectedOrbitals));
                                    break;
                            }
                            break;
                    }
                    break;

                case PageType.Coordinates:
                    switch(subPage)
                    {
                        case Globals.SubPageType.Input:
                            savedInstanceState.putInt(ParamTypes.IncrementUnit, params.getInt(ParamTypes.IncrementUnit));
                            savedInstanceState.putInt(ParamTypes.IncrementType, params.getInt(ParamTypes.IncrementType));
                            break;

                        case Globals.SubPageType.List:
                        case Globals.SubPageType.Map:
                        case Globals.SubPageType.Globe:
                            Coordinates.Item[] savedItems = (Coordinates.Item[])PageAdapter.getSavedItems(page);

                            switch(subPage)
                            {
                                case Globals.SubPageType.List:
                                    listAdapter = new Coordinates.ItemListAdapter(context, savedItems, (multiNoradId != null ? multiNoradId.size() : 0), MainActivity.getTimeZone());
                                    setChangeListeners(listAdapter, page);
                                    newView = this.onCreateView(inflater, container, listAdapter, group, page);
                                    break;

                                case Globals.SubPageType.Map:
                                case Globals.SubPageType.Globe:
                                    selectedOrbitals = getSelectedOrbitals(context, multiNoradId, savedItems);
                                    newView = Current.onCreateMapView(this, inflater, container, selectedOrbitals, (subPage == Globals.SubPageType.Globe), savedInstanceState);
                                    break;
                            }
                            break;
                    }
                    break;
            }

            //if view is not set yet
            if(newView == null)
            {
                //create view
                newView = Calculate.onCreateView(this, inflater, container, savedInstanceState);
            }

            //if listener is set
            if(pageSetListener != null)
            {
                //send event
                pageSetListener.onPageSet(this, page, subPage);
            }

            //return view
            return(newView);
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
        protected void onUpdateStarted() { }

        @Override
        protected void onUpdateFinished(boolean success) { }

        //Gets selected orbitals from inputs
        private Database.SatelliteData[] getSelectedOrbitals(Context context, ArrayList<Integer> multiNoradId, Selectable.ListItem[] savedItems)
        {
            //if multi norad ID exists
            if(multiNoradId != null)
            {
                //return orbitals for each ID
                return(Database.SatelliteData.getSatellites(context, multiNoradId));
            }
            //else if saved items exist
            else if(savedItems != null && savedItems.length > 0)
            {
                //remember first item and orbitals
                Selectable.ListItem firstItem = savedItems[0];
                Database.SatelliteData firstOrbital = new Database.SatelliteData(context, firstItem.id);
                Database.SatelliteData secondOrbital = null;

                //if first item has pass data
                if(firstItem instanceof CalculateService.PassData)
                {
                    //remember pass data and second orbital ID
                    CalculateService.PassData firstDataItem = (CalculateService.PassData)firstItem;
                    int secondId = firstDataItem.id2;

                    //if second orbital ID is valid
                    if(secondId != Universe.IDs.Invalid)
                    {
                        //set second orbital
                        secondOrbital = new Database.SatelliteData(context, secondId);
                    }
                }

                //if second orbital exists, return first and second, otherwise just first
                return(secondOrbital != null ? (new Database.SatelliteData[]{firstOrbital, secondOrbital}) : (new Database.SatelliteData[]{firstOrbital}));
            }
            else
            {
                //nothing
                return(null);
            }
        }

        //Sets orbital is selected
        private void setOrbitalIsSelected(boolean[] isSelected)
        {
            //update value and display
            orbitalIsSelected = isSelected;
            updateSelectButton();
        }
        private void setOrbitalIsSelected(Database.DatabaseSatellite[] orbitals)
        {
            int index;
            boolean haveOrbitals = (orbitals != null);

            //if orbitals length changed
            if(!haveOrbitals || orbitalIsSelected == null || orbitalIsSelected.length != orbitals.length)
            {
                //resize selected orbitals
                orbitalIsSelected = new boolean[haveOrbitals ? orbitals.length : 0];
            }

            //if have orbitals
            if(haveOrbitals)
            {
                //go through each orbital
                for(index = 0; index < orbitals.length; index++)
                {
                    //update if selected
                    orbitalIsSelected[index] = orbitals[index].isSelected;
                }
            }

            //update display
            updateSelectButton();
        }

        //Returns if there are any constellations in the given orbitals
        private boolean getUsingConstellations(Database.SatelliteData[] orbitals)
        {
            int[] orbitalTypeCount = Globals.getOrbitalTypeFilterCount(orbitals);
            return(orbitalTypeCount[Database.OrbitalType.Constellation] > 0);
        }

        //Updates select button text
        private void updateSelectButton()
        {
            int count = 0;
            String text;

            //if button exists
            if(selectButton != null)
            {
                //go through each value
                for(boolean isSelected : orbitalIsSelected)
                {
                    //if selected
                    if(isSelected)
                    {
                        //add to count
                        count++;
                    }
                }

                //update text with selected count
                text = this.getString(R.string.title_select) + " (" + count + ")";
                selectButton.setText(text);
            }
        }

        //Gets input values
        public Bundle getInputValues()
        {
            int index;
            int noradId = Universe.IDs.Invalid;
            int noradId2 = Universe.IDs.Invalid;
            int unitValue;
            int pageNumber = this.getPageParam();
            boolean validInputs = true;
            boolean allowMultiNoradId = false;
            double daysBetween;
            double elMin;
            double intersection;
            String unitType = "";
            Context context = this.getContext();
            Calendar startTime = Calendar.getInstance();
            Calendar endTime = Calendar.getInstance();
            Bundle pageParams = new Bundle();
            Resources res = (context != null ? context.getResources() : null);
            ArrayList<Integer> idList;
            Database.DatabaseSatellite[] orbitals;

            //if missing a date/time display or resources
            if(startDateText == null || startTimeText == null || endDateText == null || endTimeText == null || res == null)
            {
                //no values
                return(null);
            }

            //set start and end dates
            startTime.setTimeInMillis(startDateText.getDate().getTimeInMillis());
            endTime.setTimeInMillis(endDateText.getDate().getTimeInMillis());

            //set start and end times
            startTime.set(Calendar.HOUR_OF_DAY, startTimeText.getHour());
            startTime.set(Calendar.MINUTE, startTimeText.getMinute());
            endTime.set(Calendar.HOUR_OF_DAY, endTimeText.getHour());
            endTime.set(Calendar.MINUTE, endTimeText.getMinute());

            //clear seconds and ms
            startTime.set(Calendar.SECOND, 0);
            startTime.set(Calendar.MILLISECOND, 0);
            endTime.set(Calendar.SECOND, 0);
            endTime.set(Calendar.MILLISECOND, 0);

            //if start date is not at or before at end
            if(startDateText.getDate().compareTo(endDateText.getDate()) > 0)
            {
                //invalid
                startDateText.setError(res.getString(R.string.text_invalid_start_date));
                validInputs = false;
            }

            //if still valid inputs and start time is not before end
            if(validInputs && startTime.compareTo(endTime) >= 0)
            {
                //invalid
                startTimeText.setError(res.getString(R.string.text_invalid_start_time));
                validInputs = false;
            }

            //handle specific page inputs
            switch(pageNumber)
            {
                case PageType.View:
                case PageType.Coordinates:
                    allowMultiNoradId = true;

                    //get units
                    if(viewUnitList != null)
                    {
                        unitType = viewUnitList.getSelectedValue("").toString();
                    }
                    unitValue = Globals.tryParseInt(viewUnitText.getText().toString());
                    if(unitValue == Integer.MAX_VALUE || unitValue <= 0)
                    {
                        //invalid input
                        viewUnitText.setError(res.getString(R.string.text_invalid_unit));
                        validInputs = false;
                    }
                    else
                    {
                        //add units to params
                        pageParams.putInt(ParamTypes.IncrementUnit, unitValue);
                        pageParams.putInt(ParamTypes.IncrementType, getIncrementType(context, unitType));
                    }

                    //if too many increments (more than 100,000)
                    daysBetween = (endTime.getTimeInMillis() - startTime.getTimeInMillis()) / Calculations.MsPerDay;
                    if(validInputs && (daysBetween / getDayIncrement(pageParams)) > 100000L)
                    {
                        //invalid input
                        viewUnitText.setError(res.getString(R.string.desc_unit_size_error));
                        validInputs = false;
                    }
                    break;

                case PageType.Intersection:
                    //get units
                    intersection = Globals.tryParseDouble(intersectionUnitText.getText().toString());
                    if(intersection <= 0 || intersection > 300)
                    {
                        //invalid input
                        intersectionUnitText.setError(res.getString(R.string.text_invalid_unit));
                        validInputs = false;
                    }
                    if(validInputs)
                    {
                        //add units to params
                        pageParams.putDouble(ParamTypes.IntersectionDegs, intersection);
                    }
                    //fall through

                case PageType.Passes:
                    elMin = Globals.tryParseDouble(elevationMinUnitText.getText().toString());
                    if(elMin < -90 || elMin > 90)
                    {
                        //invalid input
                        elevationMinUnitText.setError(res.getString(R.string.text_invalid_unit));
                        validInputs= false;
                    }
                    if(validInputs)
                    {
                        //add units to params
                        pageParams.putDouble(ParamTypes.ElevationMinDegs, elMin);
                    }
                    break;
            }

            //if valid inputs
            if(validInputs)
            {
                //add values to params
                if(orbitalList != null)
                {
                    noradId = (int)orbitalList.getSelectedValue(Universe.IDs.Invalid);
                }
                if(pageNumber == PageType.Intersection)
                {
                    if(orbital2List != null)
                    {
                        noradId2 = (int)orbital2List.getSelectedValue(Universe.IDs.Invalid);
                    }
                }
                pageParams.putInt(Selectable.ParamTypes.PageNumber, pageNumber);
                pageParams.putInt(Selectable.ParamTypes.SubPageNumber, Globals.SubPageType.List);
                pageParams.putInt(ParamTypes.NoradId, noradId);
                pageParams.putInt(ParamTypes.NoradId2, noradId2);
                if(allowMultiNoradId && noradId == Universe.IDs.Invalid)
                {
                    //get orbitals and check for matching select length
                    orbitals = Database.getOrbitals(context);
                    if(orbitalIsSelected.length == orbitals.length)
                    {
                        //go through each orbital selection
                        idList = new ArrayList<>(0);
                        for(index = 0; index < orbitalIsSelected.length; index++)
                        {
                            //if selected
                            if(orbitalIsSelected[index])
                            {
                                //add ID to list
                                idList.add(orbitals[index].noradId);
                            }
                        }
                        pageParams.putIntegerArrayList(ParamTypes.MultiNoradId, idList);
                    }
                }
                pageParams.putBooleanArray(ParamTypes.OrbitalIsSelected, orbitalIsSelected);
                pageParams.putLong(ParamTypes.StartDateMs, startTime.getTimeInMillis());
                pageParams.putLong(ParamTypes.EndDateMs, endTime.getTimeInMillis());

                //return values
                return(pageParams);
            }
            else
            {
                //invalid values
                return(null);
            }
        }

        public void setOnStartCalculationListener(OnStartCalculationListener listener)
        {
            startCalculationListener = listener;
        }

        public void startCalculation(Bundle params)
        {
            if(startCalculationListener != null)
            {
                startCalculationListener.onStartCalculation(params);
            }
        }

        public void setChangeListeners(final Selectable.ListBaseAdapter listAdapter, final int page)
        {
            PageAdapter.setOrientationChangedListener(page, new OnOrientationChangedListener()
            {
                @Override
                public void orientationChanged()
                {
                    View rootView = Page.this.getView();
                    View listColumns = (rootView != null ? rootView.findViewById(listAdapter.itemsRootViewID) : null);
                    Selectable.ListBaseAdapter adapter;

                    if(listColumns != null)
                    {
                        Page.this.setListColumns(Page.this.getContext(), listColumns, page);
                    }

                    adapter = Page.this.getAdapter();
                    if(adapter != null)
                    {
                       setOrientationHeaderText(adapter.headerView);
                    }
                    if(Page.this.listParentView != null)
                    {
                        setOrientationHeaderText(Page.this.listParentView.findViewById(R.id.Header_Text));
                    }
                }
            });
            PageAdapter.setItemChangedListener(page, new OnItemsChangedListener()
            {
                @Override @SuppressLint("NotifyDataSetChanged")
                public void itemsChanged()
                {
                    View rootView;

                    //update displays
                    if(listAdapter instanceof ViewAngles.ItemListAdapter)
                    {
                        rootView = Page.this.getView();

                        ((ViewAngles.ItemListAdapter)listAdapter).updateHasItems();
                        if(rootView != null)
                        {
                            listAdapter.setColumnTitles(rootView.findViewById(listAdapter.itemsRootViewID), null, page);
                        }
                    }
                    else if(listAdapter instanceof Passes.ItemListAdapter)
                    {
                        ((Passes.ItemListAdapter)listAdapter).updateHasItems();
                    }
                    else //if(listAdapter instanceof Current.Coordinates.ItemListAdapter)
                    {
                        ((Coordinates.ItemListAdapter)listAdapter).updateHasItems();
                    }
                    listAdapter.notifyDataSetChanged();
                }
            });
            PageAdapter.setHeaderChangedListener(page, new OnHeaderChangedListener()
            {
                @Override
                public void headerChanged(int id, String text)
                {
                    View rootView;

                    if(listAdapter.headerView != null)
                    {
                        listAdapter.headerView.setTag(text);
                        setOrientationHeaderText(listAdapter.headerView);
                    }

                    if(page == PageType.View)
                    {
                        rootView = Page.this.getView();
                        if(rootView != null)
                        {
                            listAdapter.dataID = id;
                            listAdapter.setColumnTitles(rootView.findViewById(listAdapter.itemsRootViewID), null, page);
                        }
                    }
                }
            });
            PageAdapter.setGraphChangedListener(page, createOnGraphChangedListener(listAdapter));
            PageAdapter.setInformationChangedListener(page, createOnInformationChangedListener(listAdapter));
        }
    }

    //Page adapter
    public static class PageAdapter extends Selectable.ListFragmentAdapter
    {
        private static ViewAngles.Item[] viewItems;
        private static Passes.Item[] passItems;
        private static Coordinates.Item[] coordinateItems;
        private static Passes.Item[] intersectionItems;
        private static final Bundle[] params = new Bundle[PageType.PageCount];
        private final Bundle[] savedInputs;
        private final Bundle[] savedSubInputs;
        private static final Object[][] savedItems = new Object[PageType.PageCount][];
        private static final Selectable.ListFragment.OnOrientationChangedListener[] orientationChangedListeners = new Selectable.ListFragment.OnOrientationChangedListener[PageType.PageCount];
        private static final Selectable.ListFragment.OnItemsChangedListener[] itemsChangedListeners = new Selectable.ListFragment.OnItemsChangedListener[PageType.PageCount];
        private static final Selectable.ListFragment.OnHeaderChangedListener[] headerChangedListeners = new Selectable.ListFragment.OnHeaderChangedListener[PageType.PageCount];
        private static final Selectable.ListFragment.OnGraphChangedListener[] graphChangedListeners = new Selectable.ListFragment.OnGraphChangedListener[PageType.PageCount];
        private static final Selectable.ListFragment.OnInformationChangedListener[] informationChangedListeners = new Selectable.ListFragment.OnInformationChangedListener[PageType.PageCount];

        public PageAdapter(FragmentManager fm, View parentView, Selectable.ListFragment.OnItemDetailButtonClickListener detailListener, Selectable.ListFragment.OnAdapterSetListener adapterListener, Selectable.ListFragment.OnPageSetListener setListener, int[] subPg, Bundle savedInstanceState)
        {
            super(fm, parentView, null, null, null, detailListener, adapterListener, setListener, null, MainActivity.Groups.Calculate, subPg);

            int index;

            savedInputs = new Bundle[PageType.PageCount];
            for(index = 0; index < savedInputs.length; index++)
            {
                savedInputs[index] = savedInstanceState.getBundle(MainActivity.ParamTypes.CalculatePageInputs + index);
            }
            savedSubInputs = new Bundle[PageType.PageCount];
            for(index = 0; index < savedSubInputs.length; index++)
            {
                savedSubInputs[index] = savedInstanceState.getBundle(MainActivity.ParamTypes.CalculatePageSubInputs + index);
                if(savedSubInputs[index] == null)
                {
                    savedSubInputs[index] = new Bundle();
                }
            }
        }

        @Override
        public @NonNull Fragment getItem(int position)
        {
            //return page
            return(this.getItem(group, position, subPage[position], new Page()));
        }

        @Override
        public @NonNull Object instantiateItem(@NonNull final ViewGroup container, int position)
        {
            boolean haveSavedParams;
            int subPageNum = subPage[position];
            Bundle params;
            Bundle savedParams;
            Calendar dateNow = Calendar.getInstance();
            Calendar dateLater = Calendar.getInstance();
            Page newPage = (Page)setupItem((Page)super.instantiateItem(container, position));

            //set later date for later
            dateLater.add(Calendar.DATE, (position == PageType.Passes || position == PageType.Intersection ? 7 : 1));

            //setup page
            newPage.setOnPageSetListener(pageSetListener);
            newPage.setOnPageResumeListener(new Selectable.ListFragment.OnPageResumeListener()
            {
                @Override
                public void resumed(Selectable.ListFragment page)
                {
                    int pageNum = page.getPageParam();

                    //if saved inputs exist and within range
                    if(savedInputs != null && pageNum >= 0 && pageNum < savedInputs.length)
                    {
                        //restore page input values
                        setPageInputValues((Page)page, savedInputs[position]);
                    }
                }
            });
            newPage.setOnPagePausedListener(new Selectable.ListFragment.OnPagePauseListener()
            {
                @Override
                public void paused(Selectable.ListFragment page)
                {
                    int pageNum = page.getPageParam();
                    int subPageNum = page.getSubPageParam();

                    //if a valid page number
                    if(savedInputs != null && pageNum >= 0 && pageNum < savedInputs.length)
                    {
                        //handle based on sub page
                        if(subPageNum == Globals.SubPageType.Input)
                        {
                            //save inputs
                            savedInputs[pageNum] = ((Page)page).getInputValues();
                        }
                    }
                }
            });

            //get saved params
            savedParams = savedInputs[position];
            haveSavedParams = (savedParams != null);

            //set params
            params = newPage.getArguments();
            if(params == null)
            {
                params = new Bundle();
            }
            params.putInt(Selectable.ParamTypes.PageNumber, position);
            params.putInt(ParamTypes.NoradId, Integer.MAX_VALUE);
            params.putInt(ParamTypes.NoradId2, Universe.IDs.Invalid);
            params.putBooleanArray(ParamTypes.OrbitalIsSelected, null);
            if(position == PageType.View || position == PageType.Coordinates)
            {
                params.putIntegerArrayList(ParamTypes.MultiNoradId, (haveSavedParams ? savedParams.getIntegerArrayList(ParamTypes.MultiNoradId) : null));
            }
            params.putLong(ParamTypes.StartDateMs, dateNow.getTimeInMillis());
            params.putLong(ParamTypes.EndDateMs, dateLater.getTimeInMillis());
            params.putInt(ParamTypes.IncrementUnit, 5);
            params.putInt(ParamTypes.IncrementType, IncrementType.Minutes);
            params.putDouble(ParamTypes.ElevationMinDegs, 0.0);
            params.putDouble(ParamTypes.IntersectionDegs, 0.2);
            if(haveSavedParams && subPageNum == Globals.SubPageType.Input)
            {
                params.putAll(savedParams);
            }
            params.putInt(Selectable.ParamTypes.SubPageNumber, subPageNum);     //note: makes sure not to use sub page from saved
            switch(position)
            {
                case PageType.View:
                case PageType.Coordinates:
                    //do nothing
                    break;

                case PageType.Passes:
                    params.putInt(MainActivity.ParamTypes.PassIndex, savedSubInputs[PageType.Passes].getInt(MainActivity.ParamTypes.PassIndex, Integer.MAX_VALUE));
                    break;

                case PageType.Intersection:
                    params.putInt(MainActivity.ParamTypes.PassIndex, savedSubInputs[PageType.Intersection].getInt(MainActivity.ParamTypes.PassIndex, Integer.MAX_VALUE));
                    break;
            }
            newPage.setArguments(params);

            return(newPage);
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
                case PageType.View:
                    return(res.getString(R.string.title_view));

                case PageType.Passes:
                    return(res.getString(R.string.title_passes));

                case PageType.Coordinates:
                    return(res.getString(R.string.title_coordinates));

                case PageType.Intersection:
                    return(res.getString(R.string.title_intersection));

                default                 :
                    return(res.getString(R.string.title_invalid));
            }
        }

        //Set parameters
        public static void setParams(int page, Bundle bundle)
        {
            if(page >= 0 && page < PageType.PageCount)
            {
                params[page] = bundle;
            }
        }

        //Get parameters
        public static Bundle getParams(int page)
        {
            return(page >= 0 && page < PageType.PageCount ? params[page] : null);
        }

        //Sets items
        public static void setViewItems(ViewAngles.Item[] items)
        {
            viewItems = items;
        }
        public static void setViewItem(int index, ViewAngles.Item newItem)
        {
            if(viewItems != null && index < viewItems.length)
            {
                viewItems[index] = newItem;
            }
        }

        //Sets pass item(s)
        public static synchronized void setPassItems(Passes.Item[] items)
        {
            passItems = items;
        }
        public static synchronized void setPassItem(int index, Passes.Item newItem)
        {
            if(passItems != null && index < passItems.length)
            {
                passItems[index] = newItem;
            }
        }

        //Sets intersection item(s)
        public static synchronized void setIntersectionItems(Passes.Item[] items)
        {
            intersectionItems = items;
        }
        public static synchronized void setIntersectionItem(int index, Passes.Item newItem)
        {
            if(intersectionItems != null && index < intersectionItems.length)
            {
                intersectionItems[index] = newItem;
            }
        }

        //Adds pass item(s)
        public static synchronized void addPassItems(Passes.Item[] newItems, int insertIndex)
        {
            int index;
            int addLength = newItems.length;
            Passes.Item[] newItemArray = new Passes.Item[passItems.length + addLength];

            //go from start to insert index
            for(index = 0; index < insertIndex; index++)
            {
                newItemArray[index] = passItems[index];
            }

            //go from insert to end of new items
            for(index = insertIndex; index < newItemArray.length && (index - insertIndex) < addLength; index++)
            {
                newItemArray[index] = newItems[index - insertIndex];
            }

            //go from after new items to end
            for(index = insertIndex + addLength; index < newItemArray.length; index++)
            {
                newItemArray[index] = passItems[index - addLength];
            }

            //set items
            setPassItems(newItemArray);

            //update
            notifyItemsChanged(PageType.Passes);
        }
        public static synchronized void addPassItem(Passes.Item newItem)
        {
            int itemCount = passItems.length;
            ArrayList<Passes.Item> itemList = new ArrayList<>(Arrays.asList(passItems));

            //add new item
            itemList.add((itemCount > 0 ? (itemCount - 1) : 0), newItem);
            setPassItems(itemList.toArray(new Passes.Item[0]));

            //update
            notifyItemsChanged(PageType.Passes);
        }

        //Removes a pass item
        protected static synchronized void removePassItem(int index)
        {
            ArrayList<Passes.Item> itemList;

            //if a valid index
            if(passItems != null && index >= 0 && index < passItems.length)
            {
                //remove item
                itemList = new ArrayList<>(Arrays.asList(passItems));
                itemList.remove(index);
                passItems = itemList.toArray(new Passes.Item[0]);

                //update
                notifyItemsChanged(PageType.Passes);
            }
        }

        //Sets coordinate item(s)
        public static void setCoordinateItems(Coordinates.Item[] items)
        {
            coordinateItems = items;
        }
        public static void setCoordinateItem(int index, Coordinates.Item newItem)
        {
            if(coordinateItems != null && index < coordinateItems.length)
            {
                coordinateItems[index] = newItem;
            }
        }

        //Adds intersection item(s)
        public static synchronized void addIntersectionItems(Passes.Item[] newItems, int insertIndex)
        {
            int index;
            int addLength = newItems.length;
            Passes.Item[] newItemArray = new Passes.Item[intersectionItems.length + addLength];

            //go from start to insert index
            for(index = 0; index < insertIndex; index++)
            {
                newItemArray[index] = intersectionItems[index];
            }

            //go from insert to end of new items
            for(index = insertIndex; index < newItemArray.length && (index - insertIndex) < addLength; index++)
            {
                newItemArray[index] = newItems[index - insertIndex];
            }

            //go from after new items to end
            for(index = insertIndex + addLength; index < newItemArray.length; index++)
            {
                newItemArray[index] = intersectionItems[index - addLength];
            }

            //set items
            setIntersectionItems(newItemArray);

            //update
            notifyItemsChanged(PageType.Intersection);
        }
        public static synchronized void addIntersectionItem(Passes.Item newItem)
        {
            int itemCount = intersectionItems.length;
            ArrayList<Passes.Item> itemList = new ArrayList<>(Arrays.asList(intersectionItems));

            //add new item
            itemList.add((itemCount > 0 ? (itemCount - 1) : 0), newItem);
            setIntersectionItems(itemList.toArray(new Passes.Item[0]));

            //update
            notifyItemsChanged(PageType.Intersection);
        }

        //Removes an intersection item
        protected static synchronized void removeInterSectionItem(int index)
        {
            ArrayList<Passes.Item> itemList;

            //if a valid index
            if(intersectionItems != null && index >= 0 && index < intersectionItems.length)
            {
                //remove item
                itemList = new ArrayList<>(Arrays.asList(intersectionItems));
                itemList.remove(index);
                intersectionItems = itemList.toArray(new Passes.Item[0]);

                //update
                notifyItemsChanged(PageType.Intersection);
            }
        }

        //Get view angle item(s)
        public static ViewAngles.Item[] getViewAngleItems()
        {
            return(viewItems);
        }
        public static ViewAngles.Item getViewAngleItem(int index)
        {
            return(viewItems != null && index >= 0 && index < viewItems.length ? viewItems[index] : null);
        }

        //Gets pass item(s)
        public static synchronized Passes.Item[] getPassItems()
        {
            return(passItems);
        }
        public static synchronized Passes.Item getPassItem(int index)
        {
            return(passItems != null && index >= 0 && index < passItems.length ? passItems[index] : null);
        }

        //Gets coordinate item(s)
        public static Coordinates.Item[] getCoordinatesItems()
        {
            return(coordinateItems);
        }
        public static Coordinates.Item getCoordinatesItem(int index)
        {
            return(coordinateItems != null && index >= 0 && index < coordinateItems.length ? coordinateItems[index] : null);
        }

        //Gets intersection item(s)
        public static synchronized Passes.Item[] getIntersectionItems()
        {
            return(intersectionItems);
        }
        public static synchronized Passes.Item getIntersectionItem(int index)
        {
            return(intersectionItems != null && index >= 0 && index < intersectionItems.length ? intersectionItems[index] : null);
        }

        //Gets count of items in given page
        public static synchronized int getCount(int page)
        {
            switch(page)
            {
                case PageType.View:
                    return(viewItems == null ? 0 : viewItems.length);

                case PageType.Passes:
                    return(passItems == null ? 0 : passItems.length);

                case PageType.Coordinates:
                    return(coordinateItems == null ? 0 : coordinateItems.length);

                case PageType.Intersection:
                    return(intersectionItems == null ? 0 : intersectionItems.length);

                default:
                    return(0);
            }
        }

        //Returns true if given page has items
        public static synchronized boolean hasItems(int page)
        {
            int count = getCount(page);

            //if more than 1 item
            if(count > 1)
            {
                //definitely have items
                return(true);
            }
            //else have items if first is not loading
            else
            {
                switch(page)
                {
                    case PageType.View:
                        return(count > 0 && !viewItems[0].isLoading);

                    case PageType.Passes:
                        return(count > 0 && !passItems[0].isLoading);

                    case PageType.Coordinates:
                        return(count > 0 && !coordinateItems[0].isLoading);

                    case PageType.Intersection:
                        return(count > 0 && !intersectionItems[0].isLoading);

                    default:
                        return(false);
                }
            }
        }

        public static Object[] getSavedItems(int pageNum)
        {
            //if a valid page
            if(pageNum >= 0 && pageNum < savedItems.length)
            {
                //return saved items
                return(savedItems[pageNum]);
            }

            //invalid
            return(null);
        }

        public static void setSavedItems(int pageNum, Object[] saveItems)
        {
            //if a valid page
            if(pageNum >= 0 && pageNum < savedItems.length)
            {
                //set saved items
                savedItems[pageNum] = saveItems;
            }
        }

        public Bundle getSavedInputs(int pageNum)
        {
            //if a valid page
            if(pageNum >= 0 && pageNum < savedInputs.length)
            {
                return(savedInputs[pageNum]);
            }

            //invalid
            return(null);
        }

        public void setSavedInput(int pageNum, String paramName, Object value)
        {
            //if a valid page
            if(pageNum >= 0 && pageNum < savedInputs.length)
            {
                //if a bool array
                if(value instanceof boolean[])
                {
                    //set array
                    savedInputs[pageNum].putBooleanArray(paramName, (boolean[])value);
                }
            }
        }

        public void setSavedSubInput(int pageNum, String paramName, int paramValue)
        {
            //if a valid page
            if(pageNum >= 0 &&pageNum < savedSubInputs.length)
            {
                //set saved value
                savedSubInputs[pageNum].putInt(paramName, paramValue);
            }
        }

        //Sets orientation changed listener for the given page
        public static void setOrientationChangedListener(int position, Selectable.ListFragment.OnOrientationChangedListener listener)
        {
            //if a valid page
            if(position >= 0 && position < PageType.PageCount)
            {
                //set listener
                orientationChangedListeners[position] = listener;
            }
        }

        //Sets item changed listener for the given page
        public static void setItemChangedListener(int position, Selectable.ListFragment.OnItemsChangedListener listener)
        {
            //if a valid page
            if(position >= 0 && position < PageType.PageCount)
            {
                //set listener
                itemsChangedListeners[position] = listener;
            }
        }

        //Sets header changed listener for the given page
        public static void setHeaderChangedListener(int position, Selectable.ListFragment.OnHeaderChangedListener listener)
        {
            //if a valid page
            if(position >= 0 && position < PageType.PageCount)
            {
                //set listener
                headerChangedListeners[position] = listener;
            }
        }

        //Sets graph changed listener for the given page
        public static void setGraphChangedListener(int position, Selectable.ListFragment.OnGraphChangedListener listener)
        {
            //if a valid page
            if(position >= 0 && position < PageType.PageCount)
            {
                //set listener
                graphChangedListeners[position] = listener;
            }
        }

        //Sets information changed listener for the given page
        public static void setInformationChangedListener(int position, Selectable.ListFragment.OnInformationChangedListener listener)
        {
            //if a valid page
            if(position >= 0 && position < PageType.PageCount)
            {
                //set listener
                informationChangedListeners[position] = listener;
            }
        }

        //Calls orientation changed listener for the given page
        public static void notifyOrientationChangedListener(int position)
        {
            //if a valid page and listener exists
            if(position >= 0 && position < PageType.PageCount && orientationChangedListeners[position] != null)
            {
                //call listener
                orientationChangedListeners[position].orientationChanged();
            }
        }

        //Calls items changed listener for the given page
        public static void notifyItemsChanged(int position)
        {
            //if a valid page and listener exists
            if(position >= 0 && position < PageType.PageCount && itemsChangedListeners[position] != null)
            {
                //call listener
                itemsChangedListeners[position].itemsChanged();
            }
        }

        //Calls header changed listener for the given page
        public static void notifyHeaderChanged(int position, int id, String text)
        {
            //if a valid page and listener exists
            if(position >= 0 && position < PageType.PageCount && headerChangedListeners[position] != null)
            {
                //call listener
                headerChangedListeners[position].headerChanged(id, text);
            }
        }

        //Call graph changed listener for the given page
        public static void notifyGraphChanged(int position, Database.SatelliteData orbital1, ArrayList<CalculateViewsTask.OrbitalView> pathPoints, Database.SatelliteData orbital2, ArrayList<CalculateViewsTask.OrbitalView> path2Points)
        {
            //if a valid page and listener exists
            if(position >= 0 && position < PageType.PageCount && graphChangedListeners[position] != null)
            {
                //call listener
                graphChangedListeners[position].graphChanged(orbital1, pathPoints, orbital2, path2Points);
            }
        }

        //Calls information changed listener for the given page
        public static void notifyInformationChanged(int position, Spanned text)
        {
            //if a valid page and listener exists
            if(position >= 0 && position < PageType.PageCount && informationChangedListeners[position] != null)
            {
                //call listener
                informationChangedListeners[position].informationChanged(text);
            }
        }
    }

    //Gets unit type from params
    private static int getUnitType(Bundle params)
    {
        return(params.getInt(ParamTypes.IncrementType, IncrementType.Days));
    }

    //Gets day increment from params
    public static double getDayIncrement(Bundle params)
    {
        int unitValue;
        int unitType;
        double dayIncrement;

        //get units
        unitType = getUnitType(params);
        unitValue = params.getInt(ParamTypes.IncrementUnit);
        dayIncrement = unitValue;

        //convert value to days
        if(unitType == IncrementType.Seconds)
        {
            //convert to minutes
            dayIncrement /= 60.0;
            unitType = IncrementType.Minutes;
        }
        if(unitType == IncrementType.Minutes)
        {
            //convert to hours
            dayIncrement /= 60.0;
            unitType = IncrementType.Hours;
        }
        if(unitType == IncrementType.Hours)
        {
            //convert to days
            dayIncrement /= 24.0;
        }

        //return increment
        return(dayIncrement);
    }

    //Begin calculating view information
    public static CalculateViewsTask calculateViews(Context context, Database.SatelliteData[] satellites, ViewAngles.Item[] savedViewItems, Calculations.ObserverType observer, double julianStartDate, double julianEndDate, double dayIncrement, CalculateViewsTask.OnProgressChangedListener listener)
    {
        int index;
        CalculateViewsTask task;
        Bundle params = PageAdapter.getParams(PageType.View);
        int unitType = (params != null ? getUnitType(params) : IncrementType.Seconds);
        Calculations.SatelliteObjectType[] satelliteObjects = new Calculations.SatelliteObjectType[satellites != null ? satellites.length : 0];

        //start calculating for start and end dates with given increment
        task = new CalculateViewsTask(listener);
        for(index = 0; index < satelliteObjects.length; index++)
        {
            //set views
            satelliteObjects[index] = new Calculations.SatelliteObjectType(satellites[index].satellite);
        }
        task.execute(context, satelliteObjects, savedViewItems, observer, julianStartDate, julianEndDate, dayIncrement, dayIncrement, false, false, (unitType != IncrementType.Seconds), false);

        //return task
        return(task);
    }

    //Begin calculating view information
    public static CalculateCoordinatesTask calculateCoordinates(Context context, Database.SatelliteData[] satellites, Coordinates.Item[] savedCoordinateItems, Calculations.ObserverType observer, double julianStartDate, double julianEndDate, double dayIncrement, CalculateCoordinatesTask.OnProgressChangedListener listener)
    {
        int index;
        CalculateCoordinatesTask task;
        Calculations.SatelliteObjectType[] satelliteObjects = new Calculations.SatelliteObjectType[satellites != null ? satellites.length : 0];

        //start calculating for start and end dates with given increment
        task = new CalculateCoordinatesTask(listener);
        for(index = 0; index < satelliteObjects.length; index++)
        {
            //set coordinates
            satelliteObjects[index] = new Calculations.SatelliteObjectType(satellites[index].satellite);
        }
        task.execute(context, satelliteObjects, savedCoordinateItems, observer, julianStartDate, julianEndDate, dayIncrement, false, false);

        //return task
        return(task);
    }

    //Set page input values from saved state
    private static void setPageInputValues(Page page, Bundle savedInstanceState)
    {
        Context context = page.getContext();
        int orbitalId = Integer.MAX_VALUE;
        int orbitalId2 = Universe.IDs.Invalid;
        int pageNumber = PageType.View;
        int incrementType = IncrementType.Minutes;
        int incrementUnit = 10;
        double elMin = 0.0;
        double intersectionDegrees = 0.2;
        Calendar dateNow = Calendar.getInstance();
        Calendar dateLater = Calendar.getInstance();
        boolean[] orbitalIsSelected = null;
        String[] incrementTypeArray = (context != null ? getIncrementTypes(context) : null);

        //if there is a saved state
        if(savedInstanceState != null)
        {
            try
            {
                //get values
                orbitalId = savedInstanceState.getInt(ParamTypes.NoradId, orbitalId);
                orbitalId2 = savedInstanceState.getInt(ParamTypes.NoradId2, orbitalId2);
                orbitalIsSelected = savedInstanceState.getBooleanArray(ParamTypes.OrbitalIsSelected);
                pageNumber = savedInstanceState.getInt(Selectable.ParamTypes.PageNumber, PageType.View);
                dateNow.setTimeInMillis(savedInstanceState.getLong(ParamTypes.StartDateMs));
                dateLater.setTimeInMillis(savedInstanceState.getLong(ParamTypes.EndDateMs));
                incrementUnit = savedInstanceState.getInt(ParamTypes.IncrementUnit, incrementUnit);
                incrementType = savedInstanceState.getInt(ParamTypes.IncrementType, incrementType);
                elMin = savedInstanceState.getDouble(ParamTypes.ElevationMinDegs, elMin);
                intersectionDegrees = savedInstanceState.getDouble(ParamTypes.IntersectionDegs, intersectionDegrees);
            }
            catch(Exception ex)
            {
                //do nothing
            }

            //if orbital is valid
            if(orbitalId != Integer.MAX_VALUE)
            {
                //set orbital
                if(page.orbitalList != null)
                {
                    page.orbitalList.setSelectedValue(orbitalId);
                }
            }
            if(orbitalIsSelected != null)
            {
                page.setOrbitalIsSelected(orbitalIsSelected);
            }

            //set dates and times
            if(page.startDateText != null)
            {
                page.startDateText.setDate(dateNow);
            }
            if(page.startTimeText != null)
            {
                page.startTimeText.setTime(dateNow);
            }
            if(page.endDateText != null)
            {
                page.endDateText.setDate(dateLater);
            }
            if(page.endTimeText != null)
            {
                page.endTimeText.setTime(dateLater);
            }

            //handle based on page
            switch(pageNumber)
            {
                case PageType.View:
                case PageType.Coordinates:
                    //if increment types are set
                    if(incrementTypeArray != null)
                    {
                        //set unit displays
                        if(page.viewUnitText != null)
                        {
                            page.viewUnitText.setText(String.valueOf(incrementUnit));
                        }
                        if(page.viewUnitList != null)
                        {
                            page.viewUnitList.setSelectedValue(incrementTypeArray[incrementType], incrementTypeArray[IncrementType.Minutes]);
                        }
                    }
                    break;

                case PageType.Intersection:
                    //if orbital 2 is valid
                    if(orbitalId2 != Universe.IDs.Invalid)
                    {
                        //set orbital 2
                        if(page.orbital2List != null)
                        {
                            page.orbital2List.setSelectedValue(orbitalId2);
                        }
                    }
                    //fall through

                case PageType.Passes:
                    //set unit displays
                    if(page.elevationMinUnitText != null)
                    {
                        page.elevationMinUnitText.setText(String.valueOf(elMin));
                    }
                    if(page.intersectionUnitText != null)
                    {
                        page.intersectionUnitText.setText(String.valueOf(intersectionDegrees));
                    }
                    break;
            }
        }
    }

    //Set header text based on screen orientation
    public static void setOrientationHeaderText(View headerText)
    {
        int orientation;
        String headerValue;

        //if view is a TextView
        if(headerText instanceof TextView)
        {
            //get saved value
            headerValue = (String)headerText.getTag();
            if(headerValue != null)
            {
                //if horizontal orientation
                orientation = Globals.getScreenOrientation(headerText.getContext());
                if(orientation == Surface.ROTATION_90 || orientation == Surface.ROTATION_270)
                {
                    //change to single line value
                    headerValue = headerValue.replace("\n", " - ");
                }

                //update view
                ((TextView)headerText).setText(headerValue);
            }
        }
    }

    //Sets up given orbital list
    private static void setupOrbitalList(SelectListInterface orbitalList, IconSpinner.CustomAdapter orbitalAdapter, int backgroundColor, AdapterView.OnItemSelectedListener listener)
    {
        //if list exists
        if(orbitalList != null)
        {
            //set adapter, background, and listener
            orbitalList.setAdapter(orbitalAdapter);
            orbitalList.setBackgroundColor(backgroundColor);
            if(listener != null)
            {
                orbitalList.setOnItemSelectedListener(listener);
            }
        }
    }

    //Creates an orbital adapter list
    public static IconSpinner.CustomAdapter createOrbitalAdapter(Context context, SelectListInterface listView, Database.DatabaseSatellite[] orbitals, boolean usingMulti, int pendingValue)
    {
        IconSpinner.CustomAdapter adapter = new IconSpinner.CustomAdapter(context, listView, orbitals, usingMulti);
        listView.setSelectedValue(pendingValue);
        return(adapter);
    }

    //Create page
    private static View onCreateView(final Page page, LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        Context context = page.getContext();
        final boolean haveSavedInstance = (savedInstanceState != null);
        final int pageNumber = (haveSavedInstance ? savedInstanceState.getInt(Selectable.ParamTypes.PageNumber, PageType.View) : PageType.View);
        final int backgroundColor = Globals.resolveColorID(context, android.R.attr.colorBackground);
        final boolean onIntersection = (pageNumber == PageType.Intersection);
        final boolean usingMulti = (pageNumber == PageType.View || pageNumber == PageType.Coordinates);
        final boolean usingMaterial = Settings.getMaterialTheme(context);
        int viewRowVisibility = View.VISIBLE;
        int elevationMinVisibility = View.VISIBLE;
        int intersectionVisibility = (onIntersection ? View.VISIBLE : View.GONE);
        final Database.DatabaseSatellite[] orbitals;
        ViewGroup rootView = (ViewGroup)inflater.inflate((usingMaterial ? R.layout.calculate_input_material_layout : R.layout.calculate_input_layout), container, false);
        View viewRow = rootView.findViewById(R.id.Calculate_View_Row);
        View intersectionRow = rootView.findViewById(R.id.Calculate_Intersection_Row);
        View intersectionUnitLayout = rootView.findViewById(R.id.Calculate_Intersection_Unit_Layout);
        View elevationMinRow = rootView.findViewById(R.id.Calculate_Elevation_Min_Row);
        View elevationMinUnitLayout = rootView.findViewById(R.id.Calculate_Elevation_Min_Unit_Layout);
        View selectButtonLayout = rootView.findViewById(R.id.Calculate_Select_Layout);
        TextView orbital2ListTitle = rootView.findViewById(R.id.Calculate_Orbital2_List_Title);
        TextInputLayout endDateLayout = rootView.findViewById(R.id.Calculate_End_Date_Layout);
        TextInputLayout endTimeLayout = rootView.findViewById(R.id.Calculate_End_Time_Layout);
        TextInputLayout startDateLayout = rootView.findViewById(R.id.Calculate_Start_Date_Layout);
        TextInputLayout startTimeLayout = rootView.findViewById(R.id.Calculate_Start_Time_Layout);
        TextInputLayout orbital2TextLayout = rootView.findViewById(R.id.Calculate_Orbital2_Text_Layout);
        MaterialButton startButton = rootView.findViewById(R.id.Calculate_Start_Button);
        IconSpinner.CustomAdapter orbitalAdapter;
        IconSpinner.CustomAdapter orbital2Adapter;
        IconSpinner.CustomAdapter incrementAdapter;
        AdapterView.OnItemSelectedListener itemSelectedListener;
        String[] incrementTypeArray = (context != null ? getIncrementTypes(context) : null);

        //set page displays
        page.viewUnitText = rootView.findViewById(R.id.Calculate_View_Unit_Text);
        page.intersectionUnitText = rootView.findViewById(R.id.Calculate_Intersection_Unit_Text);
        page.elevationMinUnitText = rootView.findViewById(R.id.Calculate_Elevation_Min_Unit_Text);
        page.orbitalList = rootView.findViewById(usingMaterial ? R.id.Calculate_Orbital_Text_List : R.id.Calculate_Orbital_List);
        page.orbital2List = rootView.findViewById(usingMaterial ? R.id.Calculate_Orbital2_Text_List : R.id.Calculate_Orbital2_List);
        page.viewUnitList = rootView.findViewById(usingMaterial ? R.id.Calculate_View_Unit_Text_List : R.id.Calculate_View_Unit_List);
        page.startDateText = rootView.findViewById(R.id.Calculate_Start_Date_Text);
        page.endDateText = rootView.findViewById(R.id.Calculate_End_Date_Text);
        page.startTimeText = rootView.findViewById(R.id.Calculate_Start_Time_Text);
        page.endTimeText = rootView.findViewById(R.id.Calculate_End_Time_Text);
        page.selectButton = rootView.findViewById(R.id.Calculate_Select_Button);

        //load objects
        orbitals = Database.getOrbitals(context);
        for(Database.DatabaseSatellite currentOrbital : orbitals)
        {
            //remove any filter
            currentOrbital.setInFilter(true);
        }

        //set orbital list items
        orbitalAdapter = createOrbitalAdapter(context, page.orbitalList, orbitals, usingMulti, (haveSavedInstance ? savedInstanceState.getInt(ParamTypes.NoradId, Integer.MAX_VALUE) : Integer.MAX_VALUE));
        itemSelectedListener = new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                selectButtonLayout.setVisibility((usingMulti && position == 0) ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };
        setupOrbitalList(page.orbitalList, orbitalAdapter, backgroundColor, itemSelectedListener);
        if(usingMulti)
        {
            page.selectButton.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    Activity activity = page.getActivity();
                    if(activity instanceof MainActivity)
                    {
                        MainActivity mainActivity = (MainActivity)activity;
                        MasterAddListActivity.showList(mainActivity, mainActivity.getResultLauncher(), MasterAddListActivity.ListType.SelectList, BaseInputActivity.RequestCode.OrbitalSelectList, page.orbitalIsSelected);
                    }
                }
            });
            page.setOrbitalIsSelected(orbitals);
            selectButtonLayout.setVisibility(View.VISIBLE);
        }
        if(onIntersection)
        {
            orbital2Adapter = createOrbitalAdapter(context, page.orbital2List, orbitals, false, savedInstanceState.getInt(ParamTypes.NoradId2, Universe.IDs.Invalid));
            setupOrbitalList(page.orbital2List, orbital2Adapter, backgroundColor, null);
        }
        if(page.orbital2List instanceof IconSpinner)
        {
            ((IconSpinner)page.orbital2List).setVisibility(intersectionVisibility);
        }
        else if(orbital2TextLayout != null)
        {
            orbital2TextLayout.setVisibility(intersectionVisibility);
        }
        if(orbital2ListTitle != null)
        {
            orbital2ListTitle.setVisibility(intersectionVisibility);
        }
        if(startDateLayout != null)
        {
            startDateLayout.setStartIconDrawable(Globals.getDrawableYesNo(context, R.drawable.ic_calendar_month_white, 24, true, true));
        }
        if(startTimeLayout != null)
        {
            startTimeLayout.setStartIconDrawable(Globals.getDrawableYesNo(context, R.drawable.ic_clock_black, 24, true, true));
        }
        if(endDateLayout != null)
        {
            endDateLayout.setStartIconDrawable(Globals.getDrawableYesNo(context, R.drawable.ic_calendar_month_white, 24, true, false));
        }
        if(endTimeLayout != null)
        {
            endTimeLayout.setStartIconDrawable(Globals.getDrawableYesNo(context, R.drawable.ic_clock_black, 24, true, false));
        }

        //setup date and time listeners
        page.startDateText.setOnDateSetListener(new DateInputView.OnDateSetListener()
        {
            @Override
            public void onDateSet(DateInputView dateView, Calendar date)
            {
                //clear any start time error
                page.startTimeText.setError(null);
            }
        });
        page.startTimeText.setOnTimeSetListener(new TimeInputView.OnTimeSetListener()
        {
            @Override
            public void onTimeSet(TimeInputView timeView, int hour, int minute)
            {
                //clear any start date error
                page.startDateText.setError(null);
            }
        });

        //handle based on page
        switch(pageNumber)
        {
            case PageType.View:
            case PageType.Coordinates:
                //if increment types are set
                if(incrementTypeArray != null)
                {
                    //set unit adapter
                    incrementAdapter = new IconSpinner.CustomAdapter(context, incrementTypeArray);
                    if(page.viewUnitList != null)
                    {
                        page.viewUnitList.setAdapter(incrementAdapter);
                    }
                }

                //set visibility
                elevationMinVisibility = View.GONE;
                break;

            case PageType.Passes:
            case PageType.Intersection:
                //set visibility
                viewRowVisibility = View.GONE;
                break;
        }
        viewRow.setVisibility(viewRowVisibility);
        if(intersectionRow != null)
        {
            intersectionRow.setVisibility(intersectionVisibility);
        }
        if(intersectionUnitLayout != null)
        {
            intersectionUnitLayout.setVisibility(intersectionVisibility);
        }
        if(elevationMinRow != null)
        {
            elevationMinRow.setVisibility(elevationMinVisibility);
        }
        if(elevationMinUnitLayout != null)
        {
            elevationMinUnitLayout.setVisibility(elevationMinVisibility);
        }

        //setup button
        startButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Bundle inputParams = page.getInputValues();
                int noradId;
                ArrayList<Integer> multiNoradId;

                //if inputs are set
                if(inputParams != null)
                {
                    //get norad and multiple norad IDs
                    noradId = inputParams.getInt(ParamTypes.NoradId, Universe.IDs.Invalid);
                    multiNoradId = inputParams.getIntegerArrayList(ParamTypes.MultiNoradId);

                    //-not using multi or valid ID or valid multi IDs- or -not on intersection or different ID selections-
                    if((!usingMulti || (noradId != Universe.IDs.Invalid || (multiNoradId != null && multiNoradId.size() >= 1))) && (!onIntersection || noradId != inputParams.getInt(ParamTypes.NoradId2, Universe.IDs.Invalid)))
                    {
                        //start calculation
                        page.startCalculation(inputParams);
                    }
                }
            }
        });

        //set page input values
        setPageInputValues(page, savedInstanceState);

        //return view
        return(rootView);
    }
}
