package com.nikolaiapps.orbtrack;


import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.nikolaiapps.orbtrack.Calculations.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.TimeZone;
import java.util.TimerTask;


public abstract class Current
{
    public static abstract class PageType
    {
        static final int View = 0;
        static final int Passes = 1;
        static final int Coordinates = 2;
        static final int Combined = 3;
        static final int PageCount = 4;
    }

    public static abstract class ParamTypes
    {
        static final String ID = "id";
        static final String AzimuthStart = "azStart";
        static final String AzimuthEnd = "azEnd";
        static final String AzimuthTravel = "azTravel";
        static final String ElevationMax = "elMax";
        static final String ClosestAzimuth = "closestAz";
        static final String ClosestElevation = "closestEl";
        static final String Calculating = "calculating";
        static final String FoundPass = "foundPass";
        static final String FinishedCalculating = "finishedCalc";
        static final String FoundPassStart = "foundPassStart";
        static final String PathProgress = "pathProgress";
        static final String ZoneStart = "zoneStart";
        static final String TimeStart = "timeStart";
        static final String ZoneEnd = "zoneEnd";
        static final String TimeEnd = "timeEnd";
        static final String Duration = "duration";
        static final String Views = "views";
        static final String Views2 = "views2";
        static final String Satellite = "satellite";
        static final String Illumination = "illum";
        static final String PhaseName = "pn";
        static final String TLEIsAccurate = "tleAc";
    }

    public static class Items
    {
        //note: order important for lists
        public static abstract class SortBy
        {
            static final int Name = 0;
            static final int Azimuth = 1;
            static final int Elevation = 2;
            static final int Range = 3;
            static final int PassStartTime = 4;
            static final int PassDuration = 5;
            static final int MaxElevation = 6;
            static final int Latitude = 7;
            static final int Longitude = 8;
            static final int Altitude = 9;
            static final int Count = 10;
        }

        private static class NoradIndex
        {
            private static class Comparer implements Comparator<NoradIndex>
            {
                @Override
                public int compare(NoradIndex value1, NoradIndex value2)
                {
                    return(Globals.intCompare(value1.noradId, value2.noradId));
                }
            }

            public final int noradId;
            public final int displayIndex;

            public NoradIndex(int noradId, int displayIndex)
            {
                this.noradId = noradId;
                this.displayIndex = displayIndex;
            }
        }

        public static final int[] sortByCombinedIds = new int[]{Settings.getSortByStringId(SortBy.Name), Settings.getSortByStringId(SortBy.Azimuth), Settings.getSortByStringId(SortBy.Elevation), Settings.getSortByStringId(SortBy.Range), Settings.getSortByStringId(SortBy.PassStartTime), Settings.getSortByStringId(SortBy.PassDuration), Settings.getSortByStringId(SortBy.MaxElevation), Settings.getSortByStringId(SortBy.Latitude), Settings.getSortByStringId(SortBy.Longitude), Settings.getSortByStringId(SortBy.Altitude)};
        public static final int[] sortByViewIds = new int[]{sortByCombinedIds[SortBy.Name], sortByCombinedIds[SortBy.Azimuth], sortByCombinedIds[SortBy.Elevation], sortByCombinedIds[SortBy.Range]};
        public static final int[] sortByPassesIds = new int[]{sortByCombinedIds[SortBy.Name], sortByCombinedIds[SortBy.PassStartTime], sortByCombinedIds[SortBy.PassDuration], sortByCombinedIds[SortBy.MaxElevation]};
        public static final int[] sortByCoordinateIds = new int[]{sortByCombinedIds[SortBy.Name], sortByCombinedIds[SortBy.Latitude], sortByCombinedIds[SortBy.Longitude], sortByCombinedIds[SortBy.Altitude]};
        private final int group;
        private final int page;

        public Items(int itemGroup, int itemPage)
        {
            group = itemGroup;
            page = itemPage;
        }

        public void set(Object[] items)
        {
            switch(group)
            {
                case MainActivity.Groups.Current:
                    switch(page)
                    {
                        case PageType.View:
                            PageAdapter.setViewItems((ViewAngles.Item[])items);
                            break;

                        case PageType.Passes:
                            PageAdapter.setPassItems((Passes.Item[])items);
                            break;

                        case PageType.Coordinates:
                            PageAdapter.setCoordinateItems((Coordinates.Item[])items);
                            break;

                        default:
                        case PageType.Combined:
                            PageAdapter.setCombinedItems((Combined.Item[])items);
                            break;
                    }
                    break;

                case MainActivity.Groups.Calculate:
                    switch(page)
                    {
                        case Calculate.PageType.View:
                            Calculate.PageAdapter.setViewItems((ViewAngles.Item[])items);
                            break;

                        case Calculate.PageType.Passes:
                            Calculate.PageAdapter.setPassItems((Passes.Item[])items);
                            break;

                        case Calculate.PageType.Coordinates:
                            Calculate.PageAdapter.setCoordinateItems((Coordinates.Item[])items);
                            break;

                        case Calculate.PageType.Intersection:
                            Calculate.PageAdapter.setIntersectionItems((Passes.Item[])items);
                    }
                    break;
            }
        }
        public void set(int index, Object newItem)
        {
            switch(group)
            {
                case MainActivity.Groups.Current:
                    switch(page)
                    {
                        case PageType.View:
                            PageAdapter.setViewItem(index, (ViewAngles.Item)newItem);
                            break;

                        case PageType.Passes:
                            PageAdapter.setPassItem(index, (Passes.Item)newItem);
                            break;

                        case PageType.Coordinates:
                            PageAdapter.setCoordinateItem(index, (Coordinates.Item)newItem);
                            break;

                        default:
                        case PageType.Combined:
                            PageAdapter.setCombinedItem(index, (Combined.Item)newItem);
                            break;
                    }
                    break;

                case MainActivity.Groups.Calculate:
                    switch(page)
                    {
                        case Calculate.PageType.View:
                            Calculate.PageAdapter.setViewItem(index, (ViewAngles.Item)newItem);
                            break;

                        case Calculate.PageType.Passes:
                            Calculate.PageAdapter.setPassItem(index, (Passes.Item)newItem);
                            break;

                        case Calculate.PageType.Coordinates:
                            Calculate.PageAdapter.setCoordinateItem(index, (Coordinates.Item)newItem);
                            break;

                        case Calculate.PageType.Intersection:
                            Calculate.PageAdapter.setIntersectionItem(index, (Passes.Item)newItem);
                            break;
                    }
                    break;
            }
        }

        public void sort(int sortBy)
        {
            if(group == MainActivity.Groups.Current)
            {
                PageAdapter.sortItems(page, sortBy);
            }
        }

        public Object[] get()
        {
            switch(group)
            {
                case MainActivity.Groups.Current:
                    switch(page)
                    {
                        case PageType.View:
                            return(PageAdapter.getViewAngleItems());

                        case PageType.Passes:
                            return(PageAdapter.getPassItems());

                        case PageType.Coordinates:
                            return(PageAdapter.getCoordinatesItems());

                        case PageType.Combined:
                            return(PageAdapter.getCombinedItems());
                    }
                    break;

                case MainActivity.Groups.Calculate:
                    switch(page)
                    {
                        case Calculate.PageType.View:
                            return(Calculate.PageAdapter.getViewAngleItems());

                        case Calculate.PageType.Passes:
                            return(Calculate.PageAdapter.getPassItems());

                        case Calculate.PageType.Coordinates:
                            return(Calculate.PageAdapter.getCoordinatesItems());

                        case Calculate.PageType.Intersection:
                            return(Calculate.PageAdapter.getIntersectionItems());
                    }
                    break;
            }

            return(null);
        }
        private Object get(int index)
        {
            switch(group)
            {
                case MainActivity.Groups.Current:
                    switch(page)
                    {
                        case PageType.View:
                            return(PageAdapter.getViewAngleItem(index));

                        case PageType.Passes:
                            return(PageAdapter.getPassItem(index));

                        case PageType.Coordinates:
                            return(PageAdapter.getCoordinatesItem(index));

                        case PageType.Combined:
                            return(PageAdapter.getCombinedItem(index));
                    }
                    break;

                case MainActivity.Groups.Calculate:
                    switch(page)
                    {
                        case Calculate.PageType.View:
                            return(Calculate.PageAdapter.getViewAngleItem(index));

                        case Calculate.PageType.Passes:
                            return(Calculate.PageAdapter.getPassItem(index));

                        case Calculate.PageType.Coordinates:
                            return(Calculate.PageAdapter.getCoordinatesItem(index));

                        case Calculate.PageType.Intersection:
                            return(Calculate.PageAdapter.getIntersectionItem(index));
                    }
                    break;
            }

            return(null);
        }

        public Combined.Item getCombinedItem(int index)
        {
            return((Combined.Item)get(index));
        }
        public ViewAngles.Item getViewItem(int index)
        {
            return((ViewAngles.Item)get(index));
        }
        public Passes.Item getPassItem(int index)
        {
            return((Passes.Item)get(index));
        }
        public Coordinates.Item getCoordinateItem(int index)
        {
            return((Coordinates.Item)get(index));
        }

        public int getCount()
        {
            switch(group)
            {
                case MainActivity.Groups.Current:
                    return(PageAdapter.getCount(page));

                case MainActivity.Groups.Calculate:
                    return(Calculate.PageAdapter.getCount(page));

                default:
                    return(0);
            }
        }

        public static int getSortByImageId(int stringId)
        {
            switch(stringId)
            {
                case R.string.title_azimuth:
                    return(R.drawable.compass_black_white);

                case R.string.title_elevation:
                    return(R.drawable.ic_arrow_back_white);

                case R.string.title_range:
                    return(R.drawable.distance);

                case R.string.title_pass_duration:
                    return(R.drawable.ic_timer_black);

                case R.string.title_latitude:
                case R.string.title_longitude:
                    return(R.drawable.earth_vertical_lines);

                default:
                case R.string.title_name:
                case R.string.title_pass_elevation:
                case R.string.title_pass_start:
                case R.string.title_altitude:
                    return(R.drawable.ic_launcher);
            }
        }

        public static int[] getSortByIds(int page)
        {
            switch(page)
            {
                case PageType.View:
                    return(sortByViewIds);

                case PageType.Passes:
                    return(sortByPassesIds);

                case PageType.Coordinates:
                    return(sortByCoordinateIds);

                case PageType.Combined:
                default:
                    return(sortByCombinedIds);
            }
        }
    }

    private static class ItemHolderBase extends RecyclerView.ViewHolder
    {
        public boolean movedName;
        public final View outdatedText;
        public final ViewGroup rootView;
        public final TextView nameText;
        public final TextView dataGroup1Text;
        public final TextView dataGroup2Text;
        public final TextView dataGroup3Text;
        public final TextView dataGroup4Text;
        public final TextView dataGroup5Text;
        public final TextView dataGroupTitle1Text;
        public final TextView dataGroupTitle2Text;
        public final TextView dataGroupTitle3Text;
        public final TextView dataGroupTitle4Text;
        public final TextView dataGroupTitle5Text;
        public final LinearLayout dataGroupTitles;
        public final LinearLayout dataGroup;
        public final LinearLayout nameGroup;
        public final AppCompatImageView nameImage;

        public ItemHolderBase(View itemView, int dataGroupTitlesID, int dataGroupTitle1ID, int dataGroup1TextID, int dataGroupTitle2ID, int dataGroup2TextID, int dataGroupTitle3ID, int dataGroup3TextID, int dataGroupTitle4ID, int dataGroup4TextID, int dataGroupTitle5ID, int dataGroup5TextID, int dataGroupID, int nameGroupID, int nameImageID, int nameTextID, int outdatedTextID)
        {
            super(itemView);

            movedName = false;
            rootView = (ViewGroup)itemView;
            outdatedText = (outdatedTextID > -1 ? itemView.findViewById(outdatedTextID) : null);
            dataGroup1Text = itemView.findViewById(dataGroup1TextID);
            dataGroup2Text = itemView.findViewById(dataGroup2TextID);
            dataGroup3Text = (dataGroup3TextID > -1 ? (TextView)itemView.findViewById(dataGroup3TextID) : null);
            dataGroup4Text = (dataGroup4TextID > -1 ? (TextView)itemView.findViewById(dataGroup4TextID) : null);
            dataGroup5Text = (dataGroup5TextID > -1 ? (TextView)itemView.findViewById(dataGroup5TextID) : null);
            dataGroupTitle1Text = itemView.findViewById(dataGroupTitle1ID);
            dataGroupTitle2Text = itemView.findViewById(dataGroupTitle2ID);
            dataGroupTitle3Text = (dataGroupTitle3ID > - 1 ? (TextView)itemView.findViewById(dataGroupTitle3ID) : null);
            dataGroupTitle4Text = (dataGroupTitle4ID > - 1 ? (TextView)itemView.findViewById(dataGroupTitle4ID) : null);
            dataGroupTitle5Text = (dataGroupTitle5ID > - 1 ? (TextView)itemView.findViewById(dataGroupTitle5ID) : null);
            dataGroupTitles = itemView.findViewById(dataGroupTitlesID);
            dataGroup = itemView.findViewById(dataGroupID);
            nameGroup = (nameGroupID > -1 ? (LinearLayout)itemView.findViewById(nameGroupID) : null);
            nameImage = (nameImageID > -1 ? (AppCompatImageView)itemView.findViewById(nameImageID) : null);
            nameText = (nameTextID > - 1 ? (TextView)itemView.findViewById(nameTextID) : null);
        }
    }

    //Item list adapter
    private static abstract class ItemListAdapterBase extends Selectable.ListBaseAdapter
    {
        private final int columnTextColor;
        private final int columnBackgroundColorId;
        protected boolean hasItems;
        protected boolean forCalculation;
        protected final boolean usingGrid;
        protected String dataGroupTitle1String;
        protected String dataGroupTitle2String;
        protected String dataGroupTitle3String;
        protected String dataGroupTitle4String;
        protected final String dataGroupTitle5String;

        public abstract void onBindViewHolder(@NonNull ItemHolderBase holder, int position);

        public ItemListAdapterBase(Context context, int page)
        {
            super(context);

            columnTextColor = Globals.resolveColorID(context, R.attr.defaultTextColor);
            columnBackgroundColorId = Globals.resolveAttributeID(context, R.attr.viewPagerBackground);
            hasItems = forCalculation = false;
            usingGrid = Settings.getUsingCurrentGridLayout(page);
            dataGroupTitle1String = dataGroupTitle2String = dataGroupTitle3String = dataGroupTitle4String = dataGroupTitle5String = null;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position)
        {
            ItemHolderBase itemHolder = (ItemHolderBase)holder;

            //update displays
            if(!forCalculation)
            {
                itemHolder.nameGroup.setVisibility(View.VISIBLE);
                if(usingGrid)
                {
                    if(!itemHolder.movedName)
                    {
                        itemHolder.nameGroup.removeView(itemHolder.nameText);
                        itemHolder.rootView.addView(itemHolder.nameText, 0);
                        itemHolder.movedName = true;
                        itemHolder.nameText.setTextColor(columnTextColor);
                        itemHolder.nameText.setBackgroundResource(columnBackgroundColorId);
                    }
                    itemHolder.dataGroup.setOrientation(LinearLayout.VERTICAL);
                    itemHolder.dataGroupTitle1Text.setText(dataGroupTitle1String);
                    itemHolder.dataGroupTitle2Text.setText(dataGroupTitle2String);
                    if(itemHolder.dataGroupTitle3Text != null)
                    {
                        itemHolder.dataGroupTitle3Text.setText(dataGroupTitle3String);
                    }
                    if(itemHolder.dataGroupTitle4Text != null)
                    {
                        itemHolder.dataGroupTitle4Text.setText(dataGroupTitle4String);
                    }
                    if(itemHolder.dataGroupTitle5Text != null)
                    {
                        itemHolder.dataGroupTitle5Text.setText(dataGroupTitle5String);
                    }
                    Globals.setLayoutWidth(itemHolder.dataGroup1Text, ViewGroup.LayoutParams.WRAP_CONTENT);
                    Globals.setLayoutWidth(itemHolder.dataGroup2Text, ViewGroup.LayoutParams.WRAP_CONTENT);
                    if(itemHolder.dataGroup3Text != null)
                    {
                        Globals.setLayoutWidth(itemHolder.dataGroup3Text, ViewGroup.LayoutParams.WRAP_CONTENT);
                    }
                    if(itemHolder.dataGroup4Text != null)
                    {
                        Globals.setLayoutWidth(itemHolder.dataGroup4Text, ViewGroup.LayoutParams.WRAP_CONTENT);
                    }
                }
            }

            //finish binding
            onBindViewHolder(itemHolder, position);
        }
    }

    //Combined
    public static abstract class Combined
    {
        //Item
        public static class Item extends CalculateService.PassData
        {
            public boolean tleIsAccurate;
            public float azimuth;
            public float elevation;
            public float rangeKm;
            public float speedKms;
            public float latitude;
            public float longitude;
            public float altitudeKm;
            private String name;
            private Drawable icon;
            private View elMaxUnder;
            private TextView azText;
            private TextView elText;
            private TextView nameText;
            private TextView rangeText;
            private TextView speedText;
            private TextView startText;
            private TextView startTitle;
            private TextView elMaxText;
            private TextView elMaxTitle;
            private TextView durationText;
            private TextView latitudeText;
            private TextView longitudeText;
            private LinearProgressIndicator passProgress;
            private CircularProgressIndicator passLoadingProgress;
            private LinearLayout passLayout;
            private AppCompatImageView azImage;
            private AppCompatImageView elImage;
            private AppCompatImageView nameImage;
            private AppCompatImageView speedImage;

            public static class Comparer implements Comparator<Item>
            {
                final int sort;

                public Comparer(int sortBy)
                {
                    sort = sortBy;
                }

                @Override
                public int compare(Item value1, Item value2)
                {
                    switch(sort)
                    {
                        case Items.SortBy.Azimuth:
                            return(Float.compare(value1.azimuth, value2.azimuth));

                        case Items.SortBy.Elevation:
                            return(Float.compare(value1.elevation, value2.elevation));

                        case Items.SortBy.Range:
                            return(Float.compare(value1.rangeKm, value2.rangeKm));

                        case Items.SortBy.PassStartTime:
                            return(Globals.passTimeCompare(value1.passTimeStart, value1.passTimeEnd, value2.passTimeStart, value2.passTimeEnd));

                        case Items.SortBy.PassDuration:
                            return(Globals.passDurationCompare(value1.passTimeStart, value1.passTimeEnd, value2.passTimeStart, value2.passTimeEnd));

                        case Items.SortBy.MaxElevation:
                            return(Globals.passMaxElevationCompare(value1.passElMax, value2.passElMax));

                        case Items.SortBy.Latitude:
                            return(Float.compare(value1.latitude, value2.latitude));

                        case Items.SortBy.Longitude:
                            return(Float.compare(value1.longitude, value2.longitude));

                        case Items.SortBy.Altitude:
                            return(Float.compare(value1.altitudeKm, value2.altitudeKm));

                        default:
                        case Items.SortBy.Name:
                            return(Globals.stringCompare(value1.name, value2.name));
                    }
                }
            }

            public Item(Context context, int index, Database.SatelliteData currentSatellite, boolean usePathProgress)
            {
                super(index, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, false, false, false, false, usePathProgress, null, null, "", null, null, (currentSatellite != null ? currentSatellite.satellite : null), 0, null);

                azimuth = elevation = rangeKm = speedKms = latitude = longitude = altitudeKm = Float.MAX_VALUE;

                if(currentSatellite != null)
                {
                    id = currentSatellite.getSatelliteNum();
                    orbitalType = orbital2Type = (currentSatellite.database != null ? currentSatellite.database.orbitalType : Database.OrbitalType.Satellite);
                    icon = Globals.getOrbitalIcon(context, MainActivity.getObserver(), id, orbitalType);
                    name = currentSatellite.getName();
                    satellite = currentSatellite.satellite;
                }

                tleIsAccurate = (currentSatellite != null && currentSatellite.database != null && currentSatellite.database.tleIsAccurate);
            }

            public void setLoading(boolean loading)
            {
                if(passLoadingProgress != null)
                {
                    passLoadingProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
                }
                if(passLayout != null)
                {
                    passLayout.setVisibility(loading ? View.GONE : View.VISIBLE);
                }
            }

            public boolean haveGeo()
            {
                return((latitude != 0 || longitude != 0 || altitudeKm != 0) && (latitude != Float.MAX_VALUE && longitude != Float.MAX_VALUE && altitudeKm != Float.MAX_VALUE));
            }

            public void updateDisplays(Context context, TimeZone zone)
            {
                Resources res = context.getResources();
                boolean haveGeo = haveGeo();
                float elapsedPercent;
                String text;
                String kmUnit = Globals.getKmLabel(res);

                if(zone == null)
                {
                    zone = TimeZone.getDefault();
                }

                if(nameImage != null && icon != null)
                {
                    nameImage.setBackgroundDrawable(icon);
                }

                if(nameText != null)
                {
                    nameText.setText(name);
                }

                if(azImage != null)
                {
                    azImage.setRotation(azimuth);
                }

                if(azText != null)
                {
                    azText.setText(azimuth != Float.MAX_VALUE ? Globals.getAzimuthDirectionString(res, azimuth) : "-");
                }

                if(elText != null)
                {
                    elText.setText(elevation != Float.MAX_VALUE ? Globals.getDegreeString(elevation) : "-");
                }

                if(elImage != null)
                {
                    elImage.setRotation(elevation >= 0 ? 90 : -90);
                }

                if(rangeText != null)
                {
                    text = (rangeKm != Float.MAX_VALUE ? (Globals.getNumberString(Globals.getKmUnitValue(rangeKm), 0) + " " + kmUnit) : "-");
                    rangeText.setText(text);
                }

                if(speedImage != null)
                {
                    speedImage.setRotation(speedKms >= 0 ? 180 : 0);
                }

                if(speedText != null)
                {
                    text = (speedKms != Float.MAX_VALUE ? (Globals.getNumberString(Globals.getKmUnitValue(speedKms)) + " " + kmUnit + "/s") : "-");
                    speedText.setText(text);
                }

                if(latitudeText != null)
                {
                    latitudeText.setText(haveGeo ? Globals.getLatitudeDirectionString(res, latitude, 2) : "-");
                }

                if(longitudeText != null)
                {
                    longitudeText.setText(haveGeo ? Globals.getLongitudeDirectionString(res, longitude, 2) : "-");
                }

                if(startTitle != null)
                {
                    startTitle.setText(Globals.Symbols.Up);
                }

                if(startText != null)
                {
                    startText.setText(inUnknownPassTimeStartNow() ? res.getString(R.string.title_now) : !passStartFound ? Globals.getUnknownString(context) : Globals.getDateString(context, passTimeStart, zone, true, false, true));
                }

                if(durationText != null)
                {
                    durationText.setText(!passStartFound ? Globals.getUnknownString(context) : Globals.getTimeBetween(context, passTimeStart, passTimeEnd));
                }

                if(elMaxTitle != null)
                {
                    elMaxTitle.setText(Globals.Symbols.Elevating);
                }

                if(elMaxText != null)
                {
                    elMaxText.setText(isKnownPassElevationMax() ? Globals.getDegreeString(passElMax) : "");
                }

                if(elMaxUnder != null)
                {
                    elMaxUnder.setBackgroundColor(passElMax == Double.MAX_VALUE ? Color.TRANSPARENT : passElMax >= 60 ? Color.GREEN : passElMax >= 30 ? Color.YELLOW : Color.RED);
                }

                if(passProgress != null)
                {
                    elapsedPercent = (showPathProgress ? getPassProgressPercent(Globals.getGMTTime()) : Float.MAX_VALUE);
                    passProgress.setVisibility(elapsedPercent != Float.MAX_VALUE ? View.VISIBLE : View.GONE);
                    if(elapsedPercent != Float.MAX_VALUE)
                    {
                        passProgress.setProgress((int)elapsedPercent);
                    }
                }
            }
        }

        //Item holder
        public static class ItemHolder extends RecyclerView.ViewHolder
        {
            public ItemHolder(@NonNull View itemView)
            {
                super(itemView);
            }
        }

        //Item list adapter
        public static class ItemListAdapter extends Selectable.ListBaseAdapter
        {
            private final TimeZone currentZone;
            private final Items combinedItems;

            public ItemListAdapter(Context context, Combined.Item[] savedItems, Database.SatelliteData[] orbitals, TimeZone zone)
            {
                super(context);

                int index;
                boolean usePathProgress = Settings.getListPathProgress(context);

                currentZone = zone;
                combinedItems = new Items(MainActivity.Groups.Current, PageType.Combined);

                //if there are saved items
                if(savedItems != null && savedItems.length > 0)
                {
                    //set as saved items
                    combinedItems.set(savedItems);
                }
                else
                {
                    //setup items
                    combinedItems.set(new Item[orbitals.length]);
                    for(index = 0; index < orbitals.length; index++)
                    {
                        combinedItems.set(index, new Item(context, index, orbitals[index], usePathProgress));
                    }
                }
                combinedItems.sort(Settings.getCurrentSortBy(currentContext, PageType.Combined));
            }

            @Override
            public @NonNull RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
            {
                View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.combined_current_item, parent, false);
                ItemHolder itemHolder = new ItemHolder(itemView);

                setViewClickListeners(itemView, itemHolder);
                return(itemHolder);
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position)
            {
                Item currentItem = combinedItems.getCombinedItem(position);
                View itemView = holder.itemView;
                View dataLayout;
                View outdatedText;

                dataLayout = itemView.findViewById(R.id.Combined_Item_Data_Layout);
                outdatedText = itemView.findViewById(R.id.Combined_Item_Outdated_Text);
                currentItem.azImage = itemView.findViewById(R.id.Combined_Item_Az_Image);
                currentItem.azText = itemView.findViewById(R.id.Combined_Item_Az_Text);
                currentItem.elText = itemView.findViewById(R.id.Combined_Item_El_Text);
                currentItem.elImage = itemView.findViewById(R.id.Combined_Item_Elevation_Image);
                currentItem.rangeText = itemView.findViewById(R.id.Combined_Item_Range_Text);
                currentItem.speedText = itemView.findViewById(R.id.Combined_Item_Speed_Text);
                currentItem.speedImage = itemView.findViewById(R.id.Combined_Item_Speed_Image);
                currentItem.startText = itemView.findViewById(R.id.Combined_Item_Start_Text);
                currentItem.startTitle = itemView.findViewById(R.id.Combined_Item_Start_Title);
                currentItem.elMaxText = itemView.findViewById(R.id.Combined_Item_El_Max_Text);
                currentItem.elMaxTitle = itemView.findViewById(R.id.Combined_Item_El_Max_Title);
                currentItem.elMaxUnder = itemView.findViewById(R.id.Combined_Item_El_Max_Under);
                currentItem.durationText = itemView.findViewById(R.id.Combined_Item_Duration_Text);
                currentItem.nameImage = itemView.findViewById(R.id.Combined_Item_Name_Image);
                currentItem.nameText = itemView.findViewById(R.id.Combined_Item_Name_Text);
                currentItem.passProgress = itemView.findViewById(R.id.Combined_Item_Pass_Progress);
                currentItem.passLoadingProgress = itemView.findViewById(R.id.Combined_Item_Pass_Loading_Progress);
                currentItem.passLayout = itemView.findViewById(R.id.Combined_Item_Pass_Layout);
                currentItem.latitudeText = itemView.findViewById(R.id.Combined_Item_Latitude_Text);
                currentItem.longitudeText = itemView.findViewById(R.id.Combined_Item_Longitude_Text);

                if(dataLayout != null)
                {
                    dataLayout.setVisibility(currentItem.tleIsAccurate ? View.VISIBLE : View.GONE);
                }
                if(outdatedText != null)
                {
                    outdatedText.setVisibility(currentItem.tleIsAccurate ? View.GONE : View.VISIBLE);
                }
                currentItem.setLoading(!currentItem.passCalculateFinished && currentItem.tleIsAccurate);
                currentItem.updateDisplays(currentContext, currentZone);
            }

            @Override
            protected void onItemNonEditClick(Selectable.ListItem item, int pageNum)
            {
                final Item currentItem = (Item)item;

                //if TLE is not accurate
                if(!currentItem.tleIsAccurate)
                {
                    //stop
                    return;
                }

                final TimeZone defaultZone = TimeZone.getDefault();
                final int mapDisplayType = Settings.getMapDisplayType(currentContext);
                final long currentTimeMs = System.currentTimeMillis();
                final long passDurationMs = currentItem.getPassDurationMs();
                final boolean isSun = (item.id == Universe.IDs.Sun);
                final boolean isMoon = (item.id == Universe.IDs.Moon);
                final boolean showPass = currentItem.passCalculated;
                final boolean useLocalZone = (!defaultZone.equals(currentZone) && defaultZone.getOffset(currentTimeMs) != currentZone.getOffset(currentTimeMs));
                final Resources res = currentContext.getResources();
                final String unknownString = Globals.getUnknownString(currentContext);
                final String azAbbrevString = res.getString(R.string.abbrev_azimuth);
                final String startString = res.getString(R.string.title_start);
                final String endString = res.getString(R.string.title_end);
                final String localString = res.getString(R.string.title_local);
                final String azTravelString = azAbbrevString + " " + res.getString(R.string.title_travel);
                final String elMaxString = res.getString(R.string.abbrev_elevation) + " " + res.getString(R.string.title_max);
                final TextView[] detailTexts;
                final TextView[] detailTitles;

                //create dialog
                final ItemDetailDialog detailDialog = new ItemDetailDialog(currentContext, listInflater, currentItem.id, currentItem.name, currentItem.icon, itemDetailButtonClickListener);
                final View passProgressLayout = detailDialog.findViewById(R.id.Item_Detail_Progress_Layout);
                final LinearProgressIndicator passProgress = detailDialog.findViewById(R.id.Item_Detail_Progress);
                final TextView passProgressText = detailDialog.findViewById(R.id.Item_Detail_Progress_Text);
                final Graph elevationGraph = detailDialog.findViewById(R.id.Item_Detail_Graph);
                detailDialog.addGroup(R.string.title_location, R.string.title_azimuth, R.string.title_elevation, R.string.title_range);
                detailDialog.addGroup(R.string.title_coordinates, R.string.title_latitude, R.string.title_longitude, R.string.title_altitude, R.string.abbrev_velocity);
                if(showPass)
                {
                    detailDialog.addGroup(R.string.title_pass, R.string.title_time_until, R.string.title_duration, R.string.title_placeholder, R.string.title_placeholder, (useLocalZone ? R.string.title_placeholder : R.string.empty), (useLocalZone ? R.string.title_placeholder : R.string.empty), R.string.title_placeholder, R.string.title_placeholder, R.string.title_placeholder, R.string.title_placeholder);
                    detailDialog.moveProgress();
                }
                if(isSun || isMoon)
                {
                    detailDialog.addGroup(R.string.title_phase, R.string.title_name, (isMoon ? R.string.title_illumination : R.string.empty));
                }

                detailTexts = detailDialog.getDetailTexts();
                detailTitles = detailDialog.getDetailTitles();
                detailDialog.addButton(PageType.Combined, item.id, currentItem, DetailButtonType.MapView).setVisibility(mapDisplayType == CoordinatesFragment.MapDisplayType.Map ? View.VISIBLE : View.GONE);
                detailDialog.addButton(PageType.Combined, item.id, currentItem, DetailButtonType.GlobeView).setVisibility(mapDisplayType == CoordinatesFragment.MapDisplayType.Globe ? View.VISIBLE : View.GONE);
                if(showPass && passDurationMs > 0)
                {
                    detailDialog.addButton(PageType.Combined, item.id, currentItem, DetailButtonType.Graph);
                }
                detailDialog.addButton(PageType.Combined, item.id, currentItem, DetailButtonType.LensView).setVisibility(SensorUpdate.havePositionSensors(currentContext) ? View.VISIBLE : View.GONE);
                detailDialog.addButton(PageType.Combined, item.id, currentItem, DetailButtonType.Notify);
                if(have3dPreview(item.id))
                {
                    detailDialog.addButton(PageType.Combined, item.id, currentItem, DetailButtonType.Preview3d);
                }
                detailDialog.addButton(PageType.Combined, item.id, currentItem, DetailButtonType.Info);
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
                                float elapsedPercent;
                                boolean haveGeo = currentItem.haveGeo();
                                String text;
                                String title;
                                String kmUnit = Globals.getKmLabel(res);
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
                                            text = (currentItem.azimuth != Float.MAX_VALUE ? Globals.getAzimuthDirectionString(res, currentItem.azimuth) : "-");
                                            break;

                                        case 1:
                                            text = (currentItem.elevation != Float.MAX_VALUE ? Globals.getDegreeString(currentItem.elevation) : "-");
                                            break;

                                        case 2:
                                            text = (currentItem.rangeKm != Float.MAX_VALUE ? Globals.getNumberString(Globals.getKmUnitValue(currentItem.rangeKm)) : "-") + " " + kmUnit;
                                            break;

                                        case 3:
                                            text = (haveGeo ? Globals.getLatitudeDirectionString(res, currentItem.latitude, 4) : "-");
                                            break;

                                        case 4:
                                            text = (haveGeo ? Globals.getLongitudeDirectionString(res, currentItem.longitude, 4) : "-");
                                            break;

                                        case 5:
                                            text = (haveGeo ? Globals.getNumberString(Globals.getKmUnitValue(currentItem.altitudeKm)) : "-") + " " + kmUnit;
                                            break;

                                        case 6:
                                            text = (currentItem.speedKms != Float.MAX_VALUE ? Globals.getNumberString(Globals.getKmUnitValue(currentItem.speedKms)) : "-") + " " + kmUnit + "/" + res.getString(R.string.abbrev_seconds_lower);
                                            break;

                                        case 7:
                                            title = Globals.getTimeUntilDescription(res, timeNow, currentItem.passTimeStart, currentItem.passTimeEnd);
                                            text = Globals.getTimeUntil(currentContext, res, timeNow, currentItem.passTimeStart, currentItem.passTimeEnd);
                                            break;

                                        case 8:
                                            text = (!currentItem.passDuration.equals("") ? currentItem.passDuration : unknownString);
                                            break;

                                        case 9:
                                            title = startString;
                                            text = (currentItem.inUnknownPassTimeStartNow() ? res.getString(R.string.title_now) : Globals.getDateString(currentContext, currentItem.passTimeStart, currentZone, true, false));
                                            break;

                                        case 10:
                                            title = endString;
                                            text = Globals.getDateString(currentContext, currentItem.passTimeEnd, currentZone, true, false);
                                            break;

                                        case 11:
                                            if(useLocalZone)
                                            {
                                                title = localString + " " + startString;
                                                text = (currentItem.inUnknownPassTimeStartNow() ? res.getString(R.string.title_now) : Globals.getDateString(currentContext, currentItem.passTimeStart, defaultZone, true, false));
                                            }
                                            break;

                                        case 12:
                                            if(useLocalZone)
                                            {
                                                title = localString + " " + endString;
                                                text = Globals.getDateString(currentContext, currentItem.passTimeEnd, defaultZone, true, false);
                                            }
                                            break;

                                        case 13:
                                            title = azAbbrevString + " " + startString;
                                            text = (currentItem.passTimeStart != null ? Globals.getAzimuthDirectionString(res, currentItem.passAzStart) : unknownString);
                                            break;

                                        case 14:
                                            title = azAbbrevString + " " + endString;
                                            text = (currentItem.passTimeEnd != null ? Globals.getAzimuthDirectionString(res, currentItem.passAzEnd) : unknownString);
                                            break;

                                        case 15:
                                            title = azTravelString;
                                            text = (currentItem.passTimeEnd != null ? Globals.getDegreeString(currentItem.passAzTravel) : unknownString);
                                            break;

                                        case 16:
                                            title = elMaxString;
                                            text = (currentItem.passElMax != Double.MAX_VALUE ? Globals.getDegreeString(currentItem.passElMax) : unknownString);
                                            break;

                                        case 17:
                                            text = (currentItem.phaseName == null ? "-" : currentItem.phaseName);
                                            break;

                                        case 18:
                                            text = Globals.getNumberString(currentItem.illumination, 1) + "%";
                                            break;
                                    }

                                    //if display exists and text is set
                                    if(currentDetailText != null && text != null)
                                    {
                                        //set text
                                        currentDetailText.setText(text);
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

                                //if there is a pass duration
                                if(passDurationMs > 0)
                                {
                                    //if able to get elapsed percent and displays exist
                                    elapsedPercent = currentItem.getPassProgressPercent(timeNow);
                                    if(elapsedPercent != Float.MAX_VALUE && passProgress != null && passProgressText != null)
                                    {
                                        //set displays
                                        passProgress.setProgress((int)elapsedPercent);
                                        text = Globals.getNumberString(elapsedPercent, 1) + "%";
                                        passProgressText.setText(text);
                                    }
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

                /*//if ready to display
                if(currentItem.passCalculated)
                {
                    final TimeZone defaultZone = TimeZone.getDefault();
                    final int mapDisplayType = Settings.getMapDisplayType(currentContext);
                    final long currentTimeMs = System.currentTimeMillis();
                    final long passDurationMs = (currentItem.passTimeStart != null && currentItem.passTimeEnd != null ? (currentItem.passTimeEnd.getTimeInMillis() - currentItem.passTimeStart.getTimeInMillis()) : 0);
                    final boolean isSun = (item.id == Universe.IDs.Sun);
                    final boolean isMoon = (item.id == Universe.IDs.Moon);
                    final boolean useLocalZone = (!defaultZone.equals(currentZone) && defaultZone.getOffset(currentTimeMs) != currentZone.getOffset(currentTimeMs));
                    final Resources res = currentContext.getResources();
                    final String unknownString = Globals.getUnknownString(currentContext);
                    final String azAbbrevString = res.getString(R.string.abbrev_azimuth);
                    final String startString = res.getString(R.string.title_start);
                    final String endString = res.getString(R.string.title_end);
                    final String localString = res.getString(R.string.title_local);
                    final String azTravelString = azAbbrevString + " " + res.getString(R.string.title_travel);
                    final String elMaxString = res.getString(R.string.abbrev_elevation) + " " + res.getString(R.string.title_max);
                    final TextView[] detailTexts;
                    final TextView[] detailTitles;

                    //create dialog
                    final ItemDetailDialog detailDialog = new ItemDetailDialog(currentContext, listInflater, currentItem.id, currentItem.name, currentItem.icon, itemDetailButtonClickListener);
                    final View passProgressLayout = detailDialog.findViewById(R.id.Item_Detail_Progress_Layout);
                    final ProgressBar passProgress = detailDialog.findViewById(R.id.Item_Detail_Progress);
                    final TextView passProgressText = detailDialog.findViewById(R.id.Item_Detail_Progress_Text);
                    final Graph elevationGraph = detailDialog.findViewById(R.id.Item_Detail_Graph);
                    detailDialog.addGroup(R.string.title_location, R.string.title_azimuth, R.string.title_elevation, R.string.title_range);
                    detailDialog.addGroup(R.string.title_coordinates, R.string.title_latitude, R.string.title_longitude, R.string.title_altitude, R.string.abbrev_velocity);
                    detailDialog.addGroup(R.string.title_pass, R.string.title_time_until, R.string.title_duration, R.string.title_placeholder, R.string.title_placeholder, (useLocalZone ? R.string.title_placeholder : R.string.empty), (useLocalZone ? R.string.title_placeholder : R.string.empty), R.string.title_placeholder, R.string.title_placeholder, R.string.title_placeholder, R.string.title_placeholder);
                    detailDialog.moveProgress();
                    if(isSun || isMoon)
                    {
                        detailDialog.addGroup(R.string.title_phase, R.string.title_name, (isMoon ? R.string.title_illumination : R.string.empty));
                    }

                    detailTexts = detailDialog.getDetailTexts();
                    detailTitles = detailDialog.getDetailTitles();
                    detailDialog.addButton(PageType.Combined, item.id, currentItem, DetailButtonType.MapView).setVisibility(mapDisplayType == CoordinatesFragment.MapDisplayType.Map ? View.VISIBLE : View.GONE);
                    detailDialog.addButton(PageType.Combined, item.id, currentItem, DetailButtonType.GlobeView).setVisibility(mapDisplayType == CoordinatesFragment.MapDisplayType.Globe ? View.VISIBLE : View.GONE);
                    if(passDurationMs > 0)
                    {
                        detailDialog.addButton(PageType.Combined, item.id, currentItem, DetailButtonType.Graph);
                    }
                    detailDialog.addButton(PageType.Combined, item.id, currentItem, DetailButtonType.LensView).setVisibility(SensorUpdate.havePositionSensors(currentContext) ? View.VISIBLE : View.GONE);
                    detailDialog.addButton(PageType.Combined, item.id, currentItem, DetailButtonType.Notify);
                    if(have3dPreview(item.id))
                    {
                        detailDialog.addButton(PageType.Combined, item.id, currentItem, DetailButtonType.Preview3d);
                    }
                    detailDialog.addButton(PageType.Combined, item.id, currentItem, DetailButtonType.Info);
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
                                    boolean haveGeo = currentItem.haveGeo();
                                    String text;
                                    String title;
                                    String kmUnit = Globals.getKmLabel(res);
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
                                                text = (currentItem.azimuth != Float.MAX_VALUE ? Globals.getAzimuthDirectionString(res, currentItem.azimuth) : "-");
                                                break;

                                            case 1:
                                                text = (currentItem.elevation != Float.MAX_VALUE ? Globals.getDegreeString(currentItem.elevation) : "-");
                                                break;

                                            case 2:
                                                text = (currentItem.rangeKm != Float.MAX_VALUE ? Globals.getNumberString(Globals.getKmUnitValue(currentItem.rangeKm)) : "-") + " " + kmUnit;
                                                break;

                                            case 3:
                                                text = (haveGeo ? Globals.getLatitudeDirectionString(res, currentItem.latitude, 4) : "-");
                                                break;

                                            case 4:
                                                text = (haveGeo ? Globals.getLongitudeDirectionString(res, currentItem.longitude, 4) : "-");
                                                break;

                                            case 5:
                                                text = (haveGeo ? Globals.getNumberString(Globals.getKmUnitValue(currentItem.altitudeKm)) : "-") + " " + kmUnit;
                                                break;

                                            case 6:
                                                text = (currentItem.speedKms != Float.MAX_VALUE ? Globals.getNumberString(Globals.getKmUnitValue(currentItem.speedKms)) : "-") + " " + kmUnit + "/" + res.getString(R.string.abbrev_seconds_lower);
                                                break;

                                            case 7:
                                                title = Globals.getTimeUntilDescription(res, timeNow, currentItem.passTimeStart, currentItem.passTimeEnd);
                                                text = Globals.getTimeUntil(currentContext, res, timeNow, currentItem.passTimeStart, currentItem.passTimeEnd);
                                                break;

                                            case 8:
                                                text = (!currentItem.passDuration.equals("") ? currentItem.passDuration : unknownString);
                                                break;

                                            case 9:
                                                title = startString;
                                                text = (currentItem.inUnknownPassTimeStartNow() ? res.getString(R.string.title_now) : Globals.getDateString(currentContext, currentItem.passTimeStart, currentZone, true, false));
                                                break;

                                            case 10:
                                                title = endString;
                                                text = Globals.getDateString(currentContext, currentItem.passTimeEnd, currentZone, true, false);
                                                break;

                                            case 11:
                                                if(useLocalZone)
                                                {
                                                    title = localString + " " + startString;
                                                    text = (currentItem.inUnknownPassTimeStartNow() ? res.getString(R.string.title_now) : Globals.getDateString(currentContext, currentItem.passTimeStart, defaultZone, true, false));
                                                }
                                                break;

                                            case 12:
                                                if(useLocalZone)
                                                {
                                                    title = localString + " " + endString;
                                                    text = Globals.getDateString(currentContext, currentItem.passTimeEnd, defaultZone, true, false);
                                                }
                                                break;

                                            case 13:
                                                title = azAbbrevString + " " + startString;
                                                text = (currentItem.passTimeStart != null ? Globals.getAzimuthDirectionString(res, currentItem.passAzStart) : unknownString);
                                                break;

                                            case 14:
                                                title = azAbbrevString + " " + endString;
                                                text = (currentItem.passTimeEnd != null ? Globals.getAzimuthDirectionString(res, currentItem.passAzEnd) : unknownString);
                                                break;

                                            case 15:
                                                title = azTravelString;
                                                text = (currentItem.passTimeEnd != null ? Globals.getDegreeString(currentItem.passAzTravel) : unknownString);
                                                break;

                                            case 16:
                                                title = elMaxString;
                                                text = (currentItem.passElMax != Double.MAX_VALUE ? Globals.getDegreeString(currentItem.passElMax) : unknownString);
                                                break;

                                            case 17:
                                                text = (currentItem.phaseName == null ? "-" : currentItem.phaseName);
                                                break;

                                            case 18:
                                                text = Globals.getNumberString(currentItem.illumination, 1) + "%";
                                                break;
                                        }

                                        //if display exists and text is set
                                        if(currentDetailText != null && text != null)
                                        {
                                            //set text
                                            currentDetailText.setText(text);
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
                                            elapsedMs = currentItem.passTimeEnd.getTimeInMillis() - timeNow.getTimeInMillis();
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
                }*/
            }

            @Override
            public int getItemCount()
            {
                return(combinedItems.getCount());
            }

            @Override
            public Item getItem(int position)
            {
                return(position >= 0 && position < combinedItems.getCount() ? combinedItems.getCombinedItem(position) : null);
            }

            @Override
            protected Item[] getItems()
            {
                return((Item[])combinedItems.get());
            }

            @Override
            public long getItemId(int position)
            {
                return(-1);
            }
        }
    }

    //Angles
    public static abstract class ViewAngles
    {
        //Item
        public static class Item extends CalculateViewsTask.ViewItemBase
        {
            private String name;
            public boolean tleIsAccurate;
            public double julianDate;
            public Calendar time;
            public Drawable icon;
            public AppCompatImageView nameImage;
            public TextView nameText;
            public TextView timeText;
            public Calculations.SatelliteObjectType satellite;
            public static final Parcelable.Creator<Item> CREATOR =  new Parcelable.Creator<Item>()
            {
                @Override
                public Item createFromParcel(Parcel source)
                {
                    Bundle bundle = source.readBundle(getClass().getClassLoader());
                    CalculateViewsTask.ViewItemBase itemBase = CalculateViewsTask.ViewItemBase.CREATOR.createFromParcel(source);

                    if(bundle == null)
                    {
                        bundle = new Bundle();
                    }

                    return(new Item(itemBase.listIndex, itemBase.azimuth, itemBase.elevation, itemBase.rangeKm, bundle.getDouble("jd"), bundle.getString("zone"), bundle.getLong("ms"), bundle.getBoolean("tleAc")));
                }

                @Override
                public Item[] newArray(int size)
                {
                    return(new Item[size]);
                }
            };

            public static class Comparer implements Comparator<Item>
            {
                final int sort;

                public Comparer(int sortBy)
                {
                    sort = sortBy;
                }

                @Override
                public int compare(Item value1, Item value2)
                {
                    switch(sort)
                    {
                        case Items.SortBy.Azimuth:
                            return(Float.compare(value1.azimuth, value2.azimuth));

                        case Items.SortBy.Elevation:
                            return(Float.compare(value1.elevation, value2.elevation));

                        case Items.SortBy.Range:
                            return(Float.compare(value1.rangeKm, value2.rangeKm));

                        default:
                        case Items.SortBy.Name:
                            return(Globals.stringCompare(value1.name, value2.name));
                    }
                }
            }

            public Item(int index, Drawable icn, String nm, SatelliteObjectType currentSatellite, boolean tleAccurate)
            {
                super(index);
                icon = icn;
                name = nm;
                nameImage = null;
                nameText = null;
                satellite = currentSatellite;
                if(satellite != null)
                {
                    id = currentSatellite.getSatelliteNum();
                }
                tleIsAccurate = tleAccurate;
            }
            public Item(int index, float az, float el, float range, double jd, String zone, long ms, boolean tleAccurate)
            {
                super(index, az, el, range);
                julianDate = jd;
                time = Globals.getCalendar(zone, ms);
                timeText = null;
                tleIsAccurate = tleAccurate;
            }
            public Item(int index, boolean loading)
            {
                this(index, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, Double.MAX_VALUE, null, 0, false);
                isLoading = loading;
            }

            @Override
            public void writeToParcel(Parcel dest, int flags)
            {
                super.writeToParcel(dest, flags);
                Bundle bundle = new Bundle();

                if(time != null)
                {
                    bundle.putDouble("jd", julianDate);
                    bundle.putString("zone", time.getTimeZone().getID());
                    bundle.putLong("ms", time.getTimeInMillis());
                    bundle.putBoolean("tleAc", tleIsAccurate);

                    dest.writeBundle(bundle);
                }
            }

            public void updateDisplays(Context context)
            {
                super.updateDisplays();

                if(nameImage != null && icon != null)
                {
                    nameImage.setBackgroundDrawable(icon);
                }
                if(nameText != null)
                {
                    nameText.setText(name);
                }
                if(timeText != null && time != null && context != null)
                {
                    timeText.setText(Globals.getDateTimeString(context, Globals.getLocalTime(time, time.getTimeZone()), true, true));
                }
            }
        }

        //Item holder
        public static class ItemHolder extends ItemHolderBase
        {
            public final TextView timeText;
            public final LinearLayout progressGroup;

            private ItemHolder(View itemView, int azTextID, int elTextID, int rangeTextID, int phaseTextID, int illuminationTextID, int progressGroupID, int dataGroupTitlesID, int dataGroupTitle1ID, int dataGroupTitle2ID, int dataGroupTitle3ID, int dataGroupTitle4ID, int dataGroupTitle5ID, int dataGroupID, int nameGroupID, int nameImageID, int nameTextID, int timeTextID, int outdatedTextID)
            {
                super(itemView, dataGroupTitlesID, dataGroupTitle1ID, azTextID, dataGroupTitle2ID, elTextID, dataGroupTitle3ID, rangeTextID, dataGroupTitle4ID, phaseTextID, dataGroupTitle5ID, illuminationTextID, dataGroupID, nameGroupID, nameImageID, nameTextID, outdatedTextID);
                progressGroup = itemView.findViewById(progressGroupID);
                timeText = (timeTextID > - 1 ? (TextView)itemView.findViewById(timeTextID) : null);
            }
            public ItemHolder(View itemView, int azTextID, int elTextID, int rangeTextID, int progressGroupID, int dataGroupTitlesID, int dataGroupTitle1ID, int dataGroupTitle2ID, int dataGroupTitle3ID, int dataGroupID, int nameGroupID, int nameImageID, int nameTextID, int outdatedTextID)
            {
                this(itemView, azTextID, elTextID, rangeTextID, -1, -1, progressGroupID, dataGroupTitlesID, dataGroupTitle1ID, dataGroupTitle2ID, dataGroupTitle3ID, -1, -1, dataGroupID, nameGroupID, nameImageID, nameTextID, -1, outdatedTextID);
            }
            public ItemHolder(View itemView, int azTextID, int elTextID, int rangeTextID, int phaseTextID, int illuminationTextID, int progressGroupID, int dataGroupTitlesID, int dataGroupTitle1ID, int dataGroupTitle2ID, int dataGroupTitle3ID, int dataGroupTitle4ID, int dataGroupTitle5ID, int dataGroupID, int timeTextID)
            {
                this(itemView, azTextID, elTextID, rangeTextID, phaseTextID, illuminationTextID, progressGroupID, dataGroupTitlesID, dataGroupTitle1ID, dataGroupTitle2ID, dataGroupTitle3ID, dataGroupTitle4ID, dataGroupTitle5ID, dataGroupID, -1, -1, -1, timeTextID, -1);
            }
        }

        //Item list adapter
        public static class ItemListAdapter extends ItemListAdapterBase
        {
            private Items viewItems;

            private ItemListAdapter(Context context)
            {
                super(context, PageType.View);

                Resources res = context.getResources();

                //remember strings and layout ID
                dataGroupTitle1String = res.getString(R.string.abbrev_azimuth) + ":";
                dataGroupTitle2String = res.getString(R.string.abbrev_elevation) + ":";
                dataGroupTitle3String = Globals.getKmLabel(res) + ":";
                this.itemsRefID = R.layout.current_view_item;
            }
            public ItemListAdapter(Context context, Database.SatelliteData[] satellites)
            {
                this(context);

                int index;

                //setup items
                viewItems = new Items(MainActivity.Groups.Current, PageType.View);
                viewItems.set(new Item[satellites.length]);
                for(index = 0; index < satellites.length; index++)
                {
                    Database.SatelliteData currentSatellite = satellites[index];
                    viewItems.set(index, new Item(index, Globals.getOrbitalIcon(context, MainActivity.getObserver(), currentSatellite.getSatelliteNum(), currentSatellite.getOrbitalType()), currentSatellite.getName(""), currentSatellite.satellite, (currentSatellite.database != null && currentSatellite.database.tleIsAccurate)));
                }
                viewItems.sort(Settings.getCurrentSortBy(currentContext, PageType.View));

                hasItems = (viewItems.getCount() > 0);
                forCalculation = false;
            }
            public ItemListAdapter(Context context, Item[] savedItems)
            {
                this(context);

                hasItems = false;
                forCalculation = true;
                viewItems = new Items(MainActivity.Groups.Calculate, PageType.View);

                //if there are saved items
                if(savedItems != null && savedItems.length > 0)
                {
                    //set as saved items
                    viewItems.set(savedItems);
                    hasItems = true;
                }
                else
                {
                    //set as empty
                    viewItems.set(new Item[]{new Item(0, true)});
                }
                viewItems.sort(Settings.getCurrentSortBy(currentContext, PageType.View));
            }

            @Override
            public int getItemCount()
            {
                return(viewItems.getCount());
            }

            @Override
            public Item getItem(int position)
            {
                return(position >= 0 && position < viewItems.getCount() ? viewItems.getViewItem(position) : null);
            }

            @Override
            protected Item[] getItems()
            {
                return((Item[])viewItems.get());
            }

            @Override
            public long getItemId(int position)
            {
                return(-1);
            }

            @Override
            protected void setHeader(View header)
            {
                super.setHeader(header);
                header.setVisibility(forCalculation ? View.VISIBLE : View.GONE);
            }

            @Override
            protected boolean showColumnTitles(int page)
            {
                return(forCalculation || !Settings.getUsingCurrentGridLayout(page));
            }

            @Override
            protected void setColumnTitles(ViewGroup listColumns, TextView categoryText, int page)
            {
                String text;
                TextView nameText;
                TextView timeText;
                TextView phaseText;
                TextView illuminationText;
                Resources res = currentContext.getResources();
                boolean isSun = (dataID == Universe.IDs.Sun);
                boolean isMoon = (dataID == Universe.IDs.Moon);

                if(forCalculation)
                {
                    timeText = listColumns.findViewById(R.id.Angles_Time_Text);
                    timeText.setText(R.string.title_time);
                    timeText.setVisibility(View.VISIBLE);

                    phaseText = listColumns.findViewById(R.id.Angles_Phase_Text);
                    phaseText.setText(R.string.title_phase);
                    phaseText.setVisibility((isSun || isMoon) && widthDp >= EXTENDED_COLUMN_1_SHORT_WIDTH_DP ? View.VISIBLE : View.GONE);

                    illuminationText = listColumns.findViewById(R.id.Angles_Illumination_Text);
                    illuminationText.setText(R.string.abbrev_illumination);
                    illuminationText.setVisibility(isMoon && widthDp >= EXTENDED_COLUMN_2_SHORT_WIDTH_DP ? View.VISIBLE : View.GONE);
                }
                else
                {
                    nameText = listColumns.findViewById(R.id.Angles_Name_Text);
                    nameText.setText(R.string.title_name);
                    listColumns.findViewById(R.id.Angles_Name_Group).setVisibility(View.VISIBLE);
                }
                listColumns.findViewById(R.id.Angles_Progress_Group).setVisibility(View.GONE);
                listColumns.findViewById(R.id.Angles_Data_Group).setVisibility(View.VISIBLE);
                ((TextView)listColumns.findViewById(R.id.Angles_Az_Text)).setText(R.string.title_azimuth);
                ((TextView)listColumns.findViewById(R.id.Angles_El_Text)).setText(R.string.title_elevation);
                text = res.getString(R.string.title_range) + " (" + Globals.getKmLabel(res) + ")";
                ((TextView)listColumns.findViewById(R.id.Angles_Range_Text)).setText(text);

                super.setColumnTitles(listColumns, categoryText, page);
            }

            @Override
            public @NonNull ItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
            {
                View itemView = LayoutInflater.from(parent.getContext()).inflate(this.itemsRefID, parent, false);
                ItemHolder itemHolder;

                if(forCalculation)
                {
                    itemHolder = new ItemHolder(itemView, R.id.Angles_Az_Text, R.id.Angles_El_Text, R.id.Angles_Range_Text, R.id.Angles_Phase_Text, R.id.Angles_Illumination_Text, R.id.Angles_Progress_Group, R.id.Angles_Data_Group_Titles, R.id.Angles_Az_Title, R.id.Angles_El_Title, R.id.Angles_Range_Title, R.id.Angles_Phase_Title, R.id.Angles_Illumination_Title, R.id.Angles_Data_Group, R.id.Angles_Time_Text);
                }
                else
                {
                    itemHolder = new ItemHolder(itemView, R.id.Angles_Az_Text, R.id.Angles_El_Text, R.id.Angles_Range_Text, R.id.Angles_Progress_Group, R.id.Angles_Data_Group_Titles, R.id.Angles_Az_Title, R.id.Angles_El_Title, R.id.Angles_Range_Title, R.id.Angles_Data_Group, R.id.Angles_Name_Group, R.id.Angles_Name_Image, R.id.Angles_Name_Text, R.id.Angles_Outdated_Text);
                }

                setViewClickListeners(itemView, itemHolder);
                return(itemHolder);
            }

            @Override
            public void onBindViewHolder(@NonNull ItemHolderBase holder, int position)
            {
                Item currentItem = viewItems.getViewItem(position);
                ItemHolder itemHolder = (ItemHolder)holder;
                boolean isSun =  (currentItem.id == Universe.IDs.Sun);
                boolean isMoon = (currentItem.id == Universe.IDs.Moon);

                //get displays
                if(forCalculation)
                {
                    currentItem.timeText = itemHolder.timeText;
                    currentItem.timeText.setVisibility(View.VISIBLE);

                    currentItem.phaseText = itemHolder.dataGroup4Text;
                    currentItem.phaseText.setVisibility((isSun || isMoon) && widthDp >= EXTENDED_COLUMN_1_SHORT_WIDTH_DP ? View.VISIBLE : View.GONE);

                    currentItem.illuminationText = itemHolder.dataGroup5Text;
                    currentItem.illuminationText.setVisibility(isMoon && widthDp >= EXTENDED_COLUMN_2_SHORT_WIDTH_DP ? View.VISIBLE : View.GONE);
                }
                else
                {
                    currentItem.nameImage = itemHolder.nameImage;
                    currentItem.nameText = itemHolder.nameText;
                }
                currentItem.progressGroup = itemHolder.progressGroup;
                if(itemHolder.outdatedText != null)
                {
                    itemHolder.outdatedText.setVisibility(forCalculation || currentItem.tleIsAccurate ? View.GONE : View.VISIBLE);
                }
                currentItem.dataGroup = itemHolder.dataGroup;
                if(currentItem.dataGroup != null)
                {
                    currentItem.dataGroup.setVisibility(currentItem.tleIsAccurate ? View.VISIBLE : View.GONE);
                }
                if(itemHolder.dataGroupTitles != null)
                {
                    itemHolder.dataGroupTitles.setVisibility(usingGrid && currentItem.tleIsAccurate ? View.VISIBLE : View.GONE);
                }
                currentItem.azText = itemHolder.dataGroup1Text;
                currentItem.elText = itemHolder.dataGroup2Text;
                currentItem.rangeText = itemHolder.dataGroup3Text;

                //update displays
                currentItem.setLoading(!hasItems, forCalculation || currentItem.tleIsAccurate);
                currentItem.updateDisplays(currentContext);
            }

            @Override
            protected void onItemNonEditClick(Selectable.ListItem item, int pageNum)
            {
                final Item currentItem = (Item)item;
                final boolean isSun = (currentItem.id == Universe.IDs.Sun);
                final boolean isMoon = (currentItem.id == Universe.IDs.Moon);
                final Resources res = currentContext.getResources();

                //if not for calculation and range has been set
                if(!forCalculation && currentItem.rangeKm != Float.MAX_VALUE)
                {
                    final TextView[] detailTexts;

                    //create dialog
                    final ItemDetailDialog detailDialog = new ItemDetailDialog(currentContext, listInflater, currentItem.id, currentItem.name, currentItem.icon, itemDetailButtonClickListener);
                    detailDialog.addGroup(R.string.title_location, R.string.title_azimuth, R.string.title_elevation, R.string.title_range);
                    if(isSun || isMoon)
                    {
                        detailDialog.addGroup(R.string.title_phase, R.string.title_name, (isMoon ? R.string.title_illumination : R.string.empty));
                    }

                    detailTexts = detailDialog.getDetailTexts();
                    detailDialog.addButton(PageType.View, item.id, currentItem, DetailButtonType.LensView).setVisibility(SensorUpdate.havePositionSensors(currentContext) ? View.VISIBLE : View.GONE);
                    detailDialog.addButton(PageType.View, item.id, currentItem, DetailButtonType.Notify);
                    if(have3dPreview(item.id))
                    {
                        detailDialog.addButton(PageType.View, item.id, currentItem, DetailButtonType.Preview3d);
                    }
                    detailDialog.addButton(PageType.View, item.id, currentItem, DetailButtonType.Info);
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
                                    String text;
                                    String kmUnit = Globals.getKmLabel(res);

                                    //go through each display
                                    for(index = 0; index < detailTexts.length; index++)
                                    {
                                        //remember current detail text
                                        TextView currentDetailText = detailTexts[index];

                                        //reset text and title
                                        text = null;

                                        //get text
                                        switch(index)
                                        {
                                            case 0:
                                                text = (currentItem.azimuth != Float.MAX_VALUE ? Globals.getAzimuthDirectionString(res, currentItem.azimuth) : "-");
                                                break;

                                            case 1:
                                                text = (currentItem.elevation != Float.MAX_VALUE ? Globals.getDegreeString(currentItem.elevation) : "-");
                                                break;

                                            case 2:
                                                text = (currentItem.rangeKm != Float.MAX_VALUE ? Globals.getNumberString(Globals.getKmUnitValue(currentItem.rangeKm)) : "-") + " " + kmUnit;
                                                break;

                                            case 3:
                                                text = (currentItem.phaseName == null ? "-" : currentItem.phaseName);
                                                break;

                                            case 4:
                                                text = Globals.getNumberString(currentItem.illumination, 1) + "%";
                                                break;
                                        }

                                        //if display exists and text is set
                                        if(currentDetailText != null && text != null)
                                        {
                                            //set text
                                            currentDetailText.setText(text);
                                        }
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
                int count = viewItems.getCount();
                hasItems = (count > 1 || (count > 0 && !viewItems.getViewItem(0).isLoading));
            }
        }
    }

    //Passes
    public static abstract class Passes
    {
        //Item
        public static class Item extends CalculateService.PassData implements Parcelable
        {
            private Drawable icon;

            public boolean isLoading;
            public boolean tleIsAccurate;
            public boolean allowDayAbbrev;
            public View timeStartUnder;
            public View elMaxUnder;
            public AppCompatImageView nameImage;
            public TextView nameText;
            public TextView timeStartText;
            public TextView timeStartTitleText;
            public TextView timeEndText;
            public TextView timeDurationText;
            public TextView elMaxText;
            public LinearProgressIndicator progressBar;
            public LinearProgressIndicator progressStatusBar;
            public LinearLayout dataGroup;
            public static final Creator<Item> CREATOR = new Parcelable.Creator<Item>()
            {
                @Override
                public Item createFromParcel(Parcel source)
                {
                    Bundle bundle = source.readBundle(getClass().getClassLoader());
                    if(bundle == null)
                    {
                        bundle = new Bundle();
                    }

                    return(new Item(bundle.getInt(ParamTypes.ID), bundle.getDouble(ParamTypes.AzimuthStart), bundle.getDouble(ParamTypes.AzimuthEnd), bundle.getDouble(ParamTypes.AzimuthTravel), bundle.getDouble(ParamTypes.ElevationMax), bundle.getDouble(ParamTypes.ClosestAzimuth), bundle.getDouble(ParamTypes.ClosestElevation), bundle.getBoolean(ParamTypes.Calculating), bundle.getBoolean(ParamTypes.FoundPass), bundle.getBoolean(ParamTypes.FinishedCalculating), bundle.getBoolean(ParamTypes.FoundPassStart), bundle.getBoolean(ParamTypes.PathProgress), bundle.getString(ParamTypes.ZoneStart), bundle.getLong(ParamTypes.TimeStart), bundle.getString(ParamTypes.ZoneEnd), bundle.getLong(ParamTypes.TimeEnd), bundle.getString(ParamTypes.Duration), bundle.getParcelableArray(ParamTypes.Views), bundle.getParcelableArray(ParamTypes.Views2), bundle.getParcelable(ParamTypes.Satellite), bundle.getDouble(ParamTypes.Illumination), bundle.getString(ParamTypes.PhaseName), bundle.getBoolean(ParamTypes.TLEIsAccurate)));
                }

                @Override
                public Item[] newArray(int size)
                {
                    return(new Item[size]);
                }
            };

            public static class Comparer implements Comparator<Item>
            {
                final int sort;

                public Comparer(int sortBy)
                {
                    sort = sortBy;
                }

                @Override
                public int compare(Item value1, Item value2)
                {
                    switch(sort)
                    {
                        case Items.SortBy.PassStartTime:
                            return(Globals.passTimeCompare(value1.passTimeStart, value1.passTimeEnd, value2.passTimeStart, value2.passTimeEnd));

                        case Items.SortBy.PassDuration:
                            return(Globals.passDurationCompare(value1.passTimeStart, value1.passTimeEnd, value2.passTimeStart, value2.passTimeEnd));

                        case Items.SortBy.MaxElevation:
                            return(Globals.passMaxElevationCompare(value1.passElMax, value2.passElMax));

                        default:
                        case Items.SortBy.Name:
                            return(Globals.stringCompare(value1.name, value2.name));
                    }
                }
            }

            private Item(int index, double azStart, double azEnd, double azTravel, double elMax, double closestAz, double closetEl, boolean calculating, boolean foundPass, boolean finishedCalculating, boolean foundPassStart, boolean usePathProgress, Calendar startTime, Calendar endTime, String duration, Parcelable[] views, Parcelable[] views2, Calculations.SatelliteObjectType sat, SatelliteObjectType sat2, double illum, String pn, boolean tleAccurate)
            {
                super(index, azStart, azEnd, azTravel, elMax, closestAz, closetEl, calculating, foundPass, finishedCalculating, foundPassStart, usePathProgress, startTime, endTime, duration, views, views2, sat, sat2, illum, pn);
                isLoading = false;
                icon = null;
                nameImage = null;
                nameText = null;
                progressBar = null;
                progressStatusBar = null;
                dataGroup = null;
                timeStartText = null;
                timeStartUnder = null;
                timeStartTitleText = null;
                timeEndText = null;
                timeDurationText = null;
                elMaxText = null;
                elMaxUnder = null;
                tleIsAccurate = tleAccurate;
            }
            public Item(int index, double azStart, double azEnd, double azTravel, double elMax, double closestAz, double closestEl, boolean calculating, boolean foundPass, boolean finishedCalculating, boolean foundPassStart, boolean usePathProgress, String zoneStart, long timeStart, String zoneEnd, long timeEnd, String duration, Parcelable[] views, Parcelable[] views2, Calculations.SatelliteObjectType sat, double illum, String pn, boolean tleAccurate)
            {
                this(index, azStart, azEnd, azTravel, elMax, closestAz, closestEl, calculating, foundPass, finishedCalculating, foundPassStart, usePathProgress, Globals.getCalendar(zoneStart, timeStart), Globals.getCalendar(zoneEnd, timeEnd), duration, views, views2, sat, null, illum, pn, tleAccurate);
            }
            public Item(Context context, int index, Database.SatelliteData currentSatellite, Database.SatelliteData currentSatellite2, boolean usePathProgress, boolean loading)
            {
                this(index, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, false, false, false, false, usePathProgress, null, null, "", null, null, (currentSatellite != null ? currentSatellite.satellite : null), null, 0, null, false);
                isLoading = loading;

                if(currentSatellite != null)
                {
                    id = currentSatellite.getSatelliteNum();
                    orbitalType = orbital2Type = (currentSatellite.database != null ? currentSatellite.database.orbitalType : Database.OrbitalType.Satellite);
                    icon = Globals.getOrbitalIcon(context, MainActivity.getObserver(), id, orbitalType);
                    name = currentSatellite.getName();
                    satellite = currentSatellite.satellite;
                    tleIsAccurate = (currentSatellite.database != null && currentSatellite.database.tleIsAccurate);
                }
                if(currentSatellite2 != null)
                {
                    satellite2 = currentSatellite2.satellite;
                    orbital2Type = currentSatellite2.getOrbitalType();
                }
            }
            public Item(Context context, int index, Database.SatelliteData currentSatellite, boolean usePathProgress, boolean loading)
            {
                this(context, index, currentSatellite, null, usePathProgress, loading);
            }
            public Item(Context context, int index, Database.SatelliteData currentSatellite, Database.SatelliteData currentSatellite2, boolean loading)
            {
                this(context, index, currentSatellite, currentSatellite2, false, loading);
            }
            public Item(Context context, int index, Database.SatelliteData currentSatellite, boolean loading)
            {
                this(context, index, currentSatellite, null, false, loading);
            }
            public Item(CalculateService.PassData passData)
            {
                this(passData.listIndex, passData.passAzStart, passData.passAzEnd, passData.passAzTravel, passData.passElMax, passData.passClosestAz, passData.passClosestEl, passData.passCalculating, passData.passCalculated, passData.passCalculateFinished, passData.passStartFound, passData.showPathProgress, passData.passTimeStart, passData.passTimeEnd, passData.passDuration, passData.passViews, passData.passViews2, passData.satellite, passData.satellite2, passData.illumination, passData.phaseName, true);
                name = passData.name;
                orbitalType = passData.orbitalType;
                orbital2Type = passData.orbital2Type;
                illumination = passData.illumination;
                phaseName = passData.phaseName;
            }

            @Override
            public int describeContents()
            {
                return(0);
            }

            @Override
            public void writeToParcel(Parcel dest, int flags)
            {
                Bundle bundle = new Bundle();

                bundle.putInt(ParamTypes.ID, id);
                bundle.putDouble(ParamTypes.AzimuthStart, passAzStart);
                bundle.putDouble(ParamTypes.AzimuthEnd, passAzEnd);
                bundle.putDouble(ParamTypes.AzimuthTravel, passAzTravel);
                bundle.putDouble(ParamTypes.ElevationMax, passElMax);
                bundle.putDouble(ParamTypes.ClosestAzimuth, passClosestAz);
                bundle.putDouble(ParamTypes.ClosestElevation, passClosestEl);
                bundle.putBoolean(ParamTypes.Calculating, passCalculating);
                bundle.putBoolean(ParamTypes.FoundPass, passCalculated);
                bundle.putBoolean(ParamTypes.FinishedCalculating, passCalculateFinished);
                bundle.putBoolean(ParamTypes.FoundPassStart, passStartFound);
                bundle.putBoolean(ParamTypes.PathProgress, showPathProgress);
                bundle.putString(ParamTypes.ZoneStart, passTimeStart != null ? passTimeStart.getTimeZone().getID() : "");
                bundle.putLong(ParamTypes.TimeStart, passTimeStart != null ? passTimeStart.getTimeInMillis() : 0);
                bundle.putString(ParamTypes.ZoneEnd, passTimeEnd != null ? passTimeEnd.getTimeZone().getID() : "");
                bundle.putLong(ParamTypes.TimeEnd, passTimeEnd != null ? passTimeEnd.getTimeInMillis() : 0);
                bundle.putString(ParamTypes.Duration, passDuration);
                bundle.putParcelableArray(ParamTypes.Views, passViews);
                bundle.putParcelableArray(ParamTypes.Views2, passViews2);
                bundle.putParcelable(ParamTypes.Satellite, satellite);
                bundle.putDouble(ParamTypes.Illumination, illumination);
                bundle.putString(ParamTypes.PhaseName, phaseName);
                bundle.putBoolean(ParamTypes.TLEIsAccurate, tleIsAccurate);

                dest.writeBundle(bundle);
            }

            public boolean equals(Item otherItem)
            {
                return(id == otherItem.id);
            }

            public void setProgress(int value)
            {
                if(progressBar != null)
                {
                    progressBar.setProgress(value);
                }
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
                        timeStartText.setText(inUnknownPassTimeStartNow() ? context.getResources().getString(R.string.title_now) : !passStartFound ? Globals.getUnknownString(context) : Globals.getDateString(context, passTimeStart, zone, true, false, allowDayAbbrev));

                        if(timeStartTitleText != null)
                        {
                            timeStartText.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener()
                            {
                                @Override
                                public void onGlobalLayout()
                                {
                                    int height = timeStartText.getHeight();

                                    //if height is known
                                    if(height > 0)
                                    {
                                        //remove listener
                                        timeStartText.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                                        //update height
                                        Globals.setLayoutHeight(timeStartTitleText, height);
                                    }
                                }
                            });
                        }
                    }

                    if(timeStartUnder != null)
                    {
                        timeMs = System.currentTimeMillis();
                        timeStartUnder.setVisibility(inUnknownPassTimeStartNow() || (passTimeStart != null && passStartFound && timeMs >= passTimeStart.getTimeInMillis() && (passTimeEnd == null || timeMs <= passTimeEnd.getTimeInMillis())) ? View.VISIBLE : View.GONE);
                    }

                    if(timeEndText != null)
                    {
                        timeEndText.setText(!passStartFound ? Globals.getUnknownString(context) : Globals.getDateString(context, passTimeEnd, zone, true, false, true));
                    }

                    if(timeDurationText != null)
                    {
                        timeDurationText.setText(!passStartFound ? Globals.getUnknownString(context) : Globals.getTimeBetween(context, passTimeStart, passTimeEnd));
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
            public final LinearProgressIndicator currentProgressBar;
            public final LinearProgressIndicator currentProgressLoadingBar;
            public final LinearProgressIndicator calculateProgressBar;
            public final LinearLayout timeStartLayout;
            public final LinearLayout elMaxLayout;
            public final View timeStartUnder;
            public final View elMaxUnder;

            public ItemHolder(View itemView, int nameImageID, int nameTextID, int timeStartLayoutID, int timeStartTextID, int timeStartUnderID, int timeEndTextID, int timeDurationTextID, int elMaxLayoutID, int elMaxTextID, int elMaxUnderID, int nameGroupID, int currentProgressBarID, int currentProgressLoadingBarID, int calculateProgressBarID, int dataGroupTitlesID, int dataGroupTitle1ID, int dataGroupTitle2ID, int dataGroupTitle3ID, int dataGroupTitle4ID, int dataGroupID, int outdatedTextID)
            {
                super(itemView, dataGroupTitlesID, dataGroupTitle1ID, timeStartTextID, dataGroupTitle2ID, timeDurationTextID, dataGroupTitle3ID, timeEndTextID, dataGroupTitle4ID, elMaxTextID, -1, -1, dataGroupID, nameGroupID, nameImageID, nameTextID, outdatedTextID);
                currentProgressBar = itemView.findViewById(currentProgressBarID);
                currentProgressLoadingBar = itemView.findViewById(currentProgressLoadingBarID);
                calculateProgressBar = itemView.findViewById(calculateProgressBarID);
                timeStartLayout = itemView.findViewById(timeStartLayoutID);
                timeStartUnder = itemView.findViewById(timeStartUnderID);
                elMaxLayout = itemView.findViewById(elMaxLayoutID);
                elMaxUnder = itemView.findViewById(elMaxUnderID);
            }
        }

        //Item list adapter
        public static class ItemListAdapter extends ItemListAdapterBase
        {
            private final TimeZone currentZone;
            private final Items pathItems;

            public ItemListAdapter(Context context, int page, Item[] savedItems, Database.SatelliteData[] satellites, TimeZone zone, boolean forCalc)
            {
                super(context, page);

                int index;
                boolean usePathProgress = (!forCalculation && Settings.getListPathProgress(context));

                forCalculation = forCalc;
                currentZone = zone;
                pathItems = new Items((forCalculation ? MainActivity.Groups.Calculate : MainActivity.Groups.Current), page);

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

                    //if for calculation
                    if(forCalculation)
                    {
                        //set as empty
                        pathItems.set(new Item[]{new Item(context, 0, null, true)});
                    }
                    else if(hasItems)
                    {
                        //setup items
                        pathItems.set(new Item[satellites.length]);
                        for(index = 0; index < satellites.length; index++)
                        {
                            pathItems.set(index, new Item(context, index, satellites[index], usePathProgress, false));
                        }
                    }
                }
                pathItems.sort(Settings.getCurrentSortBy(currentContext, page));

                //remember strings and layout ID
                dataGroupTitle1String = Globals.Symbols.Up;
                dataGroupTitle2String = Globals.Symbols.Time;
                dataGroupTitle4String = Globals.Symbols.Elevating;
                this.itemsRefID = R.layout.current_pass_item;
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
            protected Item[] getItems()
            {
                return((Item[]) pathItems.get());
            }

            @Override
            public long getItemId(int position)
            {
                return(-1);
            }

            @Override
            protected void setHeader(View header)
            {
                super.setHeader(header);
                header.setVisibility(forCalculation ? View.VISIBLE : View.GONE);
            }

            @Override
            protected boolean showColumnTitles(int page)
            {
                return(forCalculation || !Settings.getUsingCurrentGridLayout(page));
            }

            @Override
            protected void setColumnTitles(ViewGroup listColumns, TextView categoryText, int page)
            {
                String text;
                boolean showEnd = (!usingGrid && widthDp >= EXTENDED_COLUMN_1_WIDTH_DP);
                TextView passTimeEndText;
                Resources res = currentContext.getResources();

                if(!forCalculation)
                {
                    ((TextView)listColumns.findViewById(R.id.Pass_Name_Text)).setText(R.string.title_name);
                    listColumns.findViewById(R.id.Pass_Name_Group).setVisibility(View.VISIBLE);
                }
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
                ItemHolder itemHolder = new ItemHolder(itemView, R.id.Pass_Name_Image, R.id.Pass_Name_Text, R.id.Pass_Time_Start_Layout, R.id.Pass_Time_Start_Text, R.id.Pass_Time_Start_Under, R.id.Pass_Time_End_Text, R.id.Pass_Time_Duration_Text, R.id.Pass_El_Max_Layout, R.id.Pass_El_Max_Text, R.id.Pass_El_Max_Under, R.id.Pass_Name_Group, R.id.Pass_Current_Progress_Bar, R.id.Pass_Current_Progress_Loading_Bar, R.id.Pass_Calculate_Progress_Bar, R.id.Pass_Data_Group_Titles, R.id.Pass_Time_Start_Title, R.id.Pass_Time_Duration_Title, R.id.Pass_Time_End_Title, R.id.Pass_El_Max_Title, R.id.Pass_Data_Group, R.id.Pass_Outdated_Text);

                setViewClickListeners(itemView, itemHolder);
                return(itemHolder);
            }

            @Override
            public void onBindViewHolder(@NonNull ItemHolderBase holder, int position)
            {
                boolean showEnd = (!usingGrid && widthDp >= EXTENDED_COLUMN_1_WIDTH_DP);
                Item currentItem = pathItems.getPassItem(position);
                ItemHolder itemHolder = (ItemHolder)holder;

                //set properties
                currentItem.allowDayAbbrev = !forCalculation;

                //get displays
                currentItem.nameImage = itemHolder.nameImage;
                currentItem.nameText = itemHolder.nameText;
                if(forCalculation)
                {
                    itemHolder.calculateProgressBar.setVisibility(View.VISIBLE);
                }
                else
                {
                    itemHolder.currentProgressLoadingBar.setVisibility(View.VISIBLE);
                }
                currentItem.progressBar = (forCalculation ? itemHolder.calculateProgressBar : itemHolder.currentProgressLoadingBar);
                currentItem.progressStatusBar = (forCalculation ? null : itemHolder.currentProgressBar);
                if(itemHolder.outdatedText != null)
                {
                    itemHolder.outdatedText.setVisibility(forCalculation || currentItem.tleIsAccurate ? View.GONE : View.VISIBLE);
                }
                currentItem.dataGroup = itemHolder.dataGroup;
                if(currentItem.dataGroup != null)
                {
                    currentItem.dataGroup.setVisibility(currentItem.tleIsAccurate ? View.VISIBLE : View.GONE);
                }
                if(itemHolder.dataGroupTitles != null)
                {
                    itemHolder.dataGroupTitles.setVisibility(usingGrid && currentItem.tleIsAccurate ? View.VISIBLE : View.GONE);
                }
                currentItem.timeStartText = itemHolder.dataGroup1Text;
                if(!forCalculation && usingGrid)
                {
                    Globals.setLayoutWidth(itemHolder.timeStartLayout, ViewGroup.LayoutParams.WRAP_CONTENT);
                    Globals.setLayoutWidth(itemHolder.elMaxLayout, ViewGroup.LayoutParams.WRAP_CONTENT);
                }
                currentItem.timeStartUnder = itemHolder.timeStartUnder;
                currentItem.timeStartTitleText = itemHolder.dataGroupTitle1Text;
                currentItem.timeDurationText = itemHolder.dataGroup2Text;
                currentItem.timeEndText = itemHolder.dataGroup3Text;
                if(currentItem.timeEndText != null)
                {
                    currentItem.timeEndText.setVisibility(showEnd ? View.VISIBLE : View.GONE);
                }
                currentItem.elMaxText = itemHolder.dataGroup4Text;
                currentItem.elMaxUnder = itemHolder.elMaxUnder;
                currentItem.elMaxUnder.setVisibility(!forCalculation ? View.VISIBLE : View.GONE);

                //set displays
                currentItem.setLoading(!hasItems || !currentItem.passCalculateFinished, forCalculation || currentItem.tleIsAccurate);
                currentItem.updateDisplays(currentContext, currentZone);
            }

            @Override
            protected void onItemNonEditClick(Selectable.ListItem item, int pageNum)
            {
                final Item currentItem = (Item)item;

                //if pass has been calculated
                if(currentItem.passCalculated)
                {
                    final TimeZone defaultZone = TimeZone.getDefault();
                    final long midPassTimeMs = currentItem.getMidPass();
                    final long currentTimeMs = System.currentTimeMillis();
                    final long passDurationMs = (currentItem.passTimeStart != null && currentItem.passTimeEnd != null ? (currentItem.passTimeEnd.getTimeInMillis() - currentItem.passTimeStart.getTimeInMillis()) : 0);
                    final boolean haveSatellite2 = (currentItem.satellite2 != null);
                    final boolean isSun = (item.id == Universe.IDs.Sun || (haveSatellite2 && currentItem.satellite2.getSatelliteNum() == Universe.IDs.Sun));
                    final boolean isMoon = (item.id == Universe.IDs.Moon || (haveSatellite2 && currentItem.satellite2.getSatelliteNum() == Universe.IDs.Moon));
                    final boolean useLocalZone = (!defaultZone.equals(currentZone) && defaultZone.getOffset(currentTimeMs) != currentZone.getOffset(currentTimeMs));
                    final ObserverType location = MainActivity.getObserver();
                    final Resources res = currentContext.getResources();
                    final String unknownString = Globals.getUnknownString(currentContext);
                    final String azAbbrevString = res.getString(R.string.abbrev_azimuth);
                    final String startString = res.getString(R.string.title_start);
                    final String endString = res.getString(R.string.title_end);
                    final String localString = res.getString(R.string.title_local);
                    final String azTravelString = azAbbrevString + " " + res.getString(R.string.title_travel);
                    final String elMaxString = res.getString(R.string.abbrev_elevation) + " " + res.getString(R.string.title_max);
                    final Drawable orbital1Icon = Globals.getOrbitalIcon(currentContext, location, currentItem.id, currentItem.orbitalType, midPassTimeMs, 0);
                    final Drawable orbital2Icon = (haveSatellite2 ? Globals.getOrbitalIcon(currentContext, location, currentItem.satellite2.getSatelliteNum(), currentItem.orbital2Type, 0) : null);
                    final TextView[] detailTexts;
                    final TextView[] detailTitles;

                    //create dialog
                    final ItemDetailDialog detailDialog = new ItemDetailDialog(currentContext, listInflater, currentItem.id, currentItem.name, Globals.getDrawable(currentContext, orbital1Icon, orbital2Icon), itemDetailButtonClickListener);
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
                                                text = (currentItem.inUnknownPassTimeStartNow() ? res.getString(R.string.title_now) : Globals.getDateString(currentContext, currentItem.passTimeStart, currentZone, true, false));
                                                break;

                                            case 3:
                                                title = endString;
                                                text = Globals.getDateString(currentContext, currentItem.passTimeEnd, currentZone, true, false);
                                                break;

                                            case 4:
                                                if(useLocalZone)
                                                {
                                                    title = localString + " " + startString;
                                                    text = (currentItem.inUnknownPassTimeStartNow() ? res.getString(R.string.title_now) : Globals.getDateString(currentContext, currentItem.passTimeStart, defaultZone, true, false));
                                                }
                                                break;

                                            case 5:
                                                if(useLocalZone)
                                                {
                                                    title = localString + " " + endString;
                                                    text = Globals.getDateString(currentContext, currentItem.passTimeEnd, defaultZone, true, false);
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
                                                    text = Globals.getNumberString(currentItem.illumination, 1) + "%";
                                                }
                                                break;
                                        }

                                        //if display exists and text is set
                                        if(currentDetailText != null && text != null)
                                        {
                                            //set text
                                            currentDetailText.setText(text);
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
        private static ObserverType currentLocation = new ObserverType();
        private static boolean showSearch = true;
        private static boolean showZoom = true;
        private static boolean showDividers = false;
        private static boolean showTitlesAlways = true;
        private static FloatingActionStateButtonMenu settingsMenu = null;
        private static CoordinatesFragment.OrbitalBase moonMarker = null;

        public static boolean showPaths = false;
        public static TextView mapInfoText;
        public static CoordinatesFragment mapView;
        public static CoordinatesFragment.MarkerBase currentLocationMarker = null;

        //Item
        public static class Item extends CalculateCoordinatesTask.CoordinateItemBase
        {
            private String name;

            public boolean tleIsAccurate;
            public double julianDate;
            public Calendar time;
            public Drawable icon;
            public AppCompatImageView nameImage;
            public TextView nameText;
            public TextView speedText;
            public TextView timeText;
            public Calculations.SatelliteObjectType satellite;
            public static final Parcelable.Creator<Item> CREATOR =  new Parcelable.Creator<Item>()
            {
                @Override
                public Item createFromParcel(Parcel source)
                {
                    Bundle bundle = source.readBundle(getClass().getClassLoader());
                    CalculateCoordinatesTask.CoordinateItemBase itemBase = com.nikolaiapps.orbtrack.CalculateCoordinatesTask.CoordinateItemBase.CREATOR.createFromParcel(source);

                    if(bundle == null)
                    {
                        bundle = new Bundle();
                    }

                    return(new Item(itemBase.listIndex, itemBase.latitude, itemBase.longitude, itemBase.altitudeKm, bundle.getDouble("jd"), bundle.getBoolean("tleAc")));
                }

                @Override
                public Item[] newArray(int size)
                {
                    return(new Item[size]);
                }
            };

            public static class Comparer implements Comparator<Item>
            {
                final int sort;

                public Comparer(int sortBy)
                {
                    sort = sortBy;
                }

                @Override
                public int compare(Item value1, Item value2)
                {
                    switch(sort)
                    {
                        case Items.SortBy.Latitude:
                            return(Float.compare(value1.latitude, value2.latitude));

                        case Items.SortBy.Longitude:
                            return(Float.compare(value1.longitude, value2.longitude));

                        case Items.SortBy.Altitude:
                            return(Float.compare(value1.altitudeKm, value2.altitudeKm));

                        default:
                        case Items.SortBy.Name:
                            return(Globals.stringCompare(value1.name, value2.name));
                    }
                }
            }

            public Item(int index, Drawable icn, String nm, SatelliteObjectType currentSatellite, boolean tleAccurate)
            {
                super(index);
                icon = icn;
                name = nm;
                nameImage = null;
                nameText = null;
                speedText = null;
                satellite = currentSatellite;
                if(satellite != null)
                {
                    id = satellite.getSatelliteNum();
                }
                tleIsAccurate = tleAccurate;
            }
            public Item(int index, float lat, float lon, float alt, double jd, boolean tleAccurate)
            {
                super(index, lat, lon, alt);
                julianDate = jd;
                time = Globals.julianDateToCalendar(julianDate);
                timeText = null;
                tleIsAccurate = false;
            }
            public Item(int index)
            {
                this(index, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, Double.MAX_VALUE, false);
            }

            @Override
            public void writeToParcel(Parcel dest, int flags)
            {
                super.writeToParcel(dest, flags);
                Bundle bundle = new Bundle();

                if(time != null)
                {
                    bundle.putDouble("jd", julianDate);

                    dest.writeBundle(bundle);
                }
                bundle.putBoolean("tleAc", tleIsAccurate);
            }

            public boolean haveGeo()
            {
                return((latitude != 0 || longitude != 0 || altitudeKm != 0) && (latitude != Float.MAX_VALUE && longitude != Float.MAX_VALUE && altitudeKm != Float.MAX_VALUE));
            }

            public void updateDisplays(TimeZone zone)
            {
                super.updateDisplays();

                String text;

                if(nameImage != null && icon != null)
                {
                    nameImage.setBackgroundDrawable(icon);
                }
                if(nameText != null)
                {
                    nameText.setText(name);
                }
                if(speedText != null)
                {
                    text = (speedKms != Double.MAX_VALUE ? Globals.getNumberString(Globals.getKmUnitValue(speedKms)) : "-");
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
                        timeText.setText(Globals.getDateString(timeText.getContext(), time, zone, true, true));
                    }
                }
            }
        }

        //Item holder
        public static class ItemHolder extends ItemHolderBase
        {
            public final TextView timeText;
            public final LinearLayout progressGroup;

            private ItemHolder(View itemView, int latTextID, int lonTextID, int altTextID, int progressGroupID, int dataGroupTitlesID, int dataGroupTitle1ID, int dataGroupTitle2ID, int dataGroupTitle3ID, int dataGroupTitle4ID, int dataGroupID, int nameGroupID, int nameImageID, int nameTextID, int speedTextID, int timeTextID, int outdatedTextID)
            {
                super(itemView, dataGroupTitlesID, dataGroupTitle1ID, latTextID, dataGroupTitle2ID, lonTextID, dataGroupTitle3ID, altTextID, dataGroupTitle4ID, speedTextID, -1, -1, dataGroupID, nameGroupID, nameImageID, nameTextID, outdatedTextID);
                progressGroup = itemView.findViewById(progressGroupID);
                timeText = (timeTextID > -1 ? (TextView)itemView.findViewById(timeTextID) : null);
            }
            public ItemHolder(View itemView, int latTextID, int lonTextID, int altTextID, int speedTextID, int progressGroupID, int dataGroupTitlesID, int dataGroupTitle1ID, int dataGroupTitle2ID, int dataGroupTitle3ID, int dataGroupTitle4ID, int dataGroupID, int nameGroupID, int nameImageID, int nameTextID, int oudatedTextID)
            {
                this(itemView, latTextID, lonTextID, altTextID, progressGroupID, dataGroupTitlesID, dataGroupTitle1ID, dataGroupTitle2ID, dataGroupTitle3ID, dataGroupTitle4ID, dataGroupID, nameGroupID, nameImageID, nameTextID, speedTextID, -1, oudatedTextID);
            }
            public ItemHolder(View itemView, int latTextID, int lonTextID, int altTextID, int progressGroupID, int dataGroupTitlesID, int dataGroupTitle1ID, int dataGroupTitle2ID, int dataGroupTitle3ID, int dataGroupID, int timeTextID)
            {
                this(itemView, latTextID, lonTextID, altTextID, progressGroupID, dataGroupTitlesID, dataGroupTitle1ID, dataGroupTitle2ID, dataGroupTitle3ID, -1, dataGroupID, -1, -1, -1, -1, timeTextID, -1);
            }
        }

        //Item list adapter
        public static class ItemListAdapter extends ItemListAdapterBase
        {
            private final TimeZone currentZone;
            private final Items coordinateItems;

            public ItemListAdapter(Context context, Database.SatelliteData[] satellites, ObserverType location)
            {
                super(context, PageType.Coordinates);

                int index;
                Resources res = context.getResources();

                currentZone = location.timeZone;
                coordinateItems = new Items(MainActivity.Groups.Current, PageType.Coordinates);

                //setup items
                coordinateItems.set(new Item[satellites.length]);
                for(index = 0; index < satellites.length; index++)
                {
                    Database.SatelliteData currentSatellite = satellites[index];
                    coordinateItems.set(index, new Item(index, Globals.getOrbitalIcon(context, location, currentSatellite.getSatelliteNum(), currentSatellite.getOrbitalType()), currentSatellite.getName(""), currentSatellite.satellite, (currentSatellite.database != null && currentSatellite.database.tleIsAccurate)));
                }
                coordinateItems.sort(Settings.getCurrentSortBy(currentContext, PageType.Coordinates));

                updateHasItems();
                forCalculation = false;

                //remember strings and layout ID
                dataGroupTitle1String = res.getString(R.string.abbrev_latitude) + ":";
                dataGroupTitle2String = res.getString(R.string.abbrev_longitude) + ":";
                dataGroupTitle3String = Globals.getKmLabel(res) + ":";
                this.itemsRefID = R.layout.current_coordinates_item;
            }
            public ItemListAdapter(Context context, Item[] savedItems, TimeZone zone)
            {
                super(context, PageType.Coordinates);

                hasItems = false;
                forCalculation = true;
                currentZone = zone;
                coordinateItems = new Items(MainActivity.Groups.Calculate, PageType.Coordinates);

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
                    coordinateItems.set(new Item[]{new Coordinates.Item(0)});
                }
                coordinateItems.sort(Settings.getCurrentSortBy(currentContext, PageType.Coordinates));

                //remember layout ID
                this.itemsRefID = R.layout.current_coordinates_item;
            }

            @Override
            public int getItemCount()
            {
                return(coordinateItems.getCount());
            }

            @Override
            public Item getItem(int position)
            {
                return(position >= 0 ? PageAdapter.getCoordinatesItem(position) : null);
            }

            @Override
            protected Item[] getItems()
            {
                return((Item[])coordinateItems.get());
            }

            @Override
            public long getItemId(int position)
            {
                return(-1);
            }

            @Override
            protected void setHeader(View header)
            {
                super.setHeader(header);
                header.setVisibility(forCalculation ? View.VISIBLE : View.GONE);
            }

            @Override
            protected boolean showColumnTitles(int page)
            {
                return(forCalculation || !Settings.getUsingCurrentGridLayout(page));
            }

            @Override
            protected void setColumnTitles(ViewGroup listColumns, TextView categoryText, int page)
            {
                String text;
                boolean showSpeed = (!usingGrid && widthDp >= EXTENDED_COLUMN_1_WIDTH_DP);
                TextView nameText;
                TextView timeText;
                TextView speedText;
                Resources res = currentContext.getResources();

                if(forCalculation)
                {
                    timeText = listColumns.findViewById(R.id.Coordinate_Time_Text);
                    timeText.setText(R.string.title_time);
                    timeText.setVisibility(View.VISIBLE);
                }
                else
                {
                    nameText = listColumns.findViewById(R.id.Coordinate_Name_Text);
                    nameText.setText(R.string.title_name);
                    listColumns.findViewById(R.id.Coordinate_Name_Group).setVisibility(View.VISIBLE);
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

                if(forCalculation)
                {
                    itemHolder = new ItemHolder(itemView, R.id.Coordinate_Latitude_Text, R.id.Coordinate_Longitude_Text, R.id.Coordinate_Altitude_Text, R.id.Coordinate_Progress_Group, R.id.Coordinate_Data_Group_Titles, R.id.Coordinate_Latitude_Title, R.id.Coordinate_Longitude_Title, R.id.Coordinate_Altitude_Title, R.id.Coordinate_Data_Group, R.id.Coordinate_Time_Text);
                }
                else
                {
                    itemHolder = new ItemHolder(itemView, R.id.Coordinate_Latitude_Text, R.id.Coordinate_Longitude_Text, R.id.Coordinate_Altitude_Text, R.id.Coordinate_Speed_Text, R.id.Coordinate_Progress_Group, R.id.Coordinate_Data_Group_Titles, R.id.Coordinate_Latitude_Title, R.id.Coordinate_Longitude_Title, R.id.Coordinate_Altitude_Title, R.id.Coordinate_Speed_Title, R.id.Coordinate_Data_Group, R.id.Coordinate_Name_Group, R.id.Coordinate_Name_Image, R.id.Coordinate_Name_Text, R.id.Coordinate_Outdated_Text);
                }

                setViewClickListeners(itemView, itemHolder);
                return(itemHolder);
            }

            @Override
            public void onBindViewHolder(@NonNull ItemHolderBase holder, int position)
            {
                boolean showSpeed = (!usingGrid && widthDp >= EXTENDED_COLUMN_1_WIDTH_DP);
                Item currentItem = coordinateItems.getCoordinateItem(position);
                ItemHolder itemHolder = (ItemHolder)holder;

                //get displays
                if(forCalculation)
                {
                    currentItem.timeText = itemHolder.timeText;
                    currentItem.timeText.setVisibility(View.VISIBLE);
                }
                else
                {
                    currentItem.nameImage = itemHolder.nameImage;
                    currentItem.nameText = itemHolder.nameText;
                }
                currentItem.progressGroup = itemHolder.progressGroup;
                if(itemHolder.outdatedText != null)
                {
                    itemHolder.outdatedText.setVisibility(forCalculation || currentItem.tleIsAccurate ? View.GONE : View.VISIBLE);
                }
                currentItem.dataGroup = itemHolder.dataGroup;
                if(currentItem.dataGroup != null)
                {
                    currentItem.dataGroup.setVisibility(currentItem.tleIsAccurate ? View.VISIBLE : View.GONE);
                }
                if(itemHolder.dataGroupTitles != null)
                {
                    itemHolder.dataGroupTitles.setVisibility(usingGrid && currentItem.tleIsAccurate ? View.VISIBLE : View.GONE);
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
                currentItem.setLoading(!hasItems, forCalculation || currentItem.tleIsAccurate);
                currentItem.updateDisplays(currentZone);
            }

            @Override
            protected void onItemNonEditClick(Selectable.ListItem item, int pageNum)
            {
                final Item currentItem = (Item)item;

                //if not for calculation and altitude has been set
                if(!forCalculation && currentItem.altitudeKm != Float.MAX_VALUE)
                {
                    final int mapDisplayType = Settings.getMapDisplayType(currentContext);
                    final boolean isSun = (item.id == Universe.IDs.Sun);
                    final boolean isMoon = (item.id == Universe.IDs.Moon);
                    final Resources res = currentContext.getResources();
                    final TextView[] detailTexts;

                    //create dialog
                    final ItemDetailDialog detailDialog = new ItemDetailDialog(currentContext, listInflater, currentItem.id, currentItem.name, currentItem.icon, itemDetailButtonClickListener);
                    detailDialog.addGroup(R.string.title_coordinates, R.string.title_latitude, R.string.title_longitude, R.string.title_altitude, R.string.abbrev_velocity);
                    if(isSun || isMoon)
                    {
                        detailDialog.addGroup(R.string.title_phase, R.string.title_name, (isMoon ? R.string.title_illumination : R.string.empty));
                    }

                    detailTexts = detailDialog.getDetailTexts();
                    detailDialog.addButton(PageType.Coordinates, item.id, currentItem, DetailButtonType.MapView).setVisibility(mapDisplayType == CoordinatesFragment.MapDisplayType.Map ? View.VISIBLE : View.GONE);
                    detailDialog.addButton(PageType.Coordinates, item.id, currentItem, DetailButtonType.GlobeView).setVisibility(mapDisplayType == CoordinatesFragment.MapDisplayType.Globe ? View.VISIBLE : View.GONE);
                    detailDialog.addButton(PageType.Coordinates, item.id, currentItem, DetailButtonType.Notify);
                    if(have3dPreview(item.id))
                    {
                        detailDialog.addButton(PageType.Coordinates, item.id, currentItem, DetailButtonType.Preview3d);
                    }
                    detailDialog.addButton(PageType.Coordinates, item.id, currentItem, DetailButtonType.Info);
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
                                    boolean haveGeo = currentItem.haveGeo();
                                    String text;
                                    String kmUnit = Globals.getKmLabel(res);

                                    //go through each display
                                    for(index = 0; index < detailTexts.length; index++)
                                    {
                                        //remember current detail text
                                        TextView currentDetailText = detailTexts[index];

                                        //reset text and title
                                        text = null;

                                        //get text
                                        switch(index)
                                        {
                                            case 0:
                                                text = (haveGeo ? Globals.getLatitudeDirectionString(res, currentItem.latitude, 4) : "-");
                                                break;

                                            case 1:
                                                text = (haveGeo ? Globals.getLongitudeDirectionString(res, currentItem.longitude, 4) : "-");
                                                break;

                                            case 2:
                                                text = (haveGeo ? Globals.getNumberString(Globals.getKmUnitValue(currentItem.altitudeKm)) : "-") + " " + kmUnit;
                                                break;

                                            case 3:
                                                text = (currentItem.speedKms != Double.MAX_VALUE ? Globals.getNumberString(Globals.getKmUnitValue(currentItem.speedKms)) : "-") + " " + kmUnit + "/" + res.getString(R.string.abbrev_seconds_lower);
                                                break;

                                            case 4:
                                                text = (currentItem.phaseName == null ? "-" : currentItem.phaseName);
                                                break;

                                            case 5:
                                                text = Globals.getNumberString(currentItem.illumination, 1) + "%";
                                                break;
                                        }

                                        //if display exists and text is set
                                        if(currentDetailText != null && text != null)
                                        {
                                            //set text
                                            currentDetailText.setText(text);
                                        }
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
                int count = coordinateItems.getCount();
                hasItems = (count > 1 || (count > 0 && !coordinateItems.getCoordinateItem(0).isLoading));
            }
        }

        //Task to calculate path/coordinate information
        public static class CalculatePathTask extends ThreadTask<Object, Integer, Integer[]>
        {
            private final OnProgressChangedListener progressChangedListener;
            public boolean needCoordinates;

            //Progress listener
            public interface OnProgressChangedListener
            {
                void onProgressChanged(int index, int progressType, ArrayList<CoordinatesFragment.Coordinate> pathPoints);
            }

            //Constructor
            public CalculatePathTask(OnProgressChangedListener listener)
            {
                progressChangedListener = listener;
                needCoordinates = true;
            }

            @Override
            protected Integer[] doInBackground(Object... params)
            {
                int index;
                int orbitalCount;
                double period;
                double periodJulianEnd;
                double julianDate = (Double)params[1];
                double pathJulianDate;
                double pathJulianDateStart;
                double pathJulianDateEnd;
                ObserverType observer = (ObserverType)params[0];

                //if map exists
                if(mapView != null)
                {
                    //update progress
                    onProgressChanged(0, Globals.ProgressType.Started, null);

                    //go through each orbital while not cancelled
                    orbitalCount = mapView.getOrbitalCount();
                    for(index = 0; index < orbitalCount && !this.isCancelled(); index++)
                    {
                        //get current marker, satellite, and period
                        CoordinatesFragment.OrbitalBase currentOrbital = mapView.getOrbital(index);
                        Database.SatelliteData currentData = (currentOrbital != null ? currentOrbital.getData() : null);
                        SatelliteObjectType pathSatellite = new SatelliteObjectType(currentData != null ? currentData.satellite : null);
                        ArrayList<CoordinatesFragment.Coordinate> pathPoints = new ArrayList<>(0);
                        period = pathSatellite.orbit.periodMinutes;
                        periodJulianEnd = (period > 0 ? (julianDate + (period / Calculations.MinutesPerDay)) : Double.MAX_VALUE);

                        //update progress
                        publishProgress(index, Globals.ProgressType.Started);

                        //set to now and shortest of either 1 day later or period end
                        pathJulianDateStart = julianDate;
                        pathJulianDateEnd = Math.min((julianDate + 1), periodJulianEnd);
                        
                        //calculate points unless cancelled
                        for(pathJulianDate = pathJulianDateStart; pathJulianDate < pathJulianDateEnd && !this.isCancelled(); pathJulianDate += (0.01 / 24))        //note: incrementing in fractions of a day
                        {
                            //get next position
                            pathSatellite = Calculations.updateOrbitalPosition(pathSatellite, observer, pathJulianDate, true);

                            //add position to list
                            pathPoints.add(new CoordinatesFragment.Coordinate(pathSatellite.geo.latitude, pathSatellite.geo.longitude, pathSatellite.geo.altitudeKm));
                        }

                        //update progress
                        onProgressChanged(index, (!this.isCancelled() ? Globals.ProgressType.Success : Globals.ProgressType.Failed), pathPoints);
                    }

                    //update status and progress
                    if(!this.isCancelled())
                    {
                        needCoordinates = false;
                    }
                    onProgressChanged(0, (!this.isCancelled() ? Globals.ProgressType.Finished : Globals.ProgressType.Cancelled), null);
                }

                //return status
                return(new Integer[]{!this.isCancelled() ? Globals.ProgressType.Finished : Globals.ProgressType.Cancelled});
            }

            //Calls on progress changed listener
            private void onProgressChanged(int index, int progressType, ArrayList<CoordinatesFragment.Coordinate> pathPoints)
            {
                //if there is a listener
                if(progressChangedListener != null)
                {
                    progressChangedListener.onProgressChanged(index, progressType, pathPoints);
                }
            }

            @Override
            protected  void onPostExecute(Integer[] result)
            {
                //finished
                onProgressChanged(0, result[0], null);
            }

            @Override
            protected void onCancelled(Integer[] result)
            {
                //cancelled
                onProgressChanged(0, (result != null ? result[0] : Globals.ProgressType.Cancelled), null);
            }
        }

        //Begin calculating coordinate information
        public static CalculatePathTask calculateCoordinates(ObserverType observer, double julianDate, CalculatePathTask.OnProgressChangedListener listener)
        {
            CalculatePathTask task;

            //start calculating
            task = new CalculatePathTask(listener);
            task.execute(observer, julianDate);

            //return task
            return(task);
        }

        //setup search
        private static void setupSearch(final Context context, final FloatingActionStateButton showSearchButton, final IconSpinner searchList, final View searchListLayout)
        {
            //if show search button exists
            if(showSearchButton != null)
            {
                int textColor = Globals.resolveColorID(context, R.attr.defaultTextColor);
                int textSelectedColor = Globals.resolveColorID(context, R.attr.columnTitleTextColor);

                //setup search list
                searchList.setAdapter(new IconSpinner.CustomAdapter(context, Database.getSelectedOrbitals(context,  true, true), textColor, textSelectedColor, textColor, textSelectedColor, (Settings.getDarkTheme(context) ? R.color.white : R.color.black)));
                searchList.setBackgroundColor(Globals.resolveColorID(context, R.attr.pageTitleBackground));
                searchList.setBackgroundItemColor(Globals.resolveColorID(context, R.attr.pageBackground));
                searchList.setBackgroundItemSelectedColor(Globals.resolveColorID(context, R.attr.columnBackground));
                searchList.setTextColor(textColor);
                searchList.setTextSelectedColor(textSelectedColor);
                searchList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
                {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
                    {
                        int noradId = (int)searchList.getSelectedValue(Universe.IDs.None);
                        boolean isNone = (noradId == Universe.IDs.None);
                        boolean isLocation = (noradId == Universe.IDs.CurrentLocation);

                        //if location or anything
                        if(isLocation || !isNone)
                        {
                            //select it
                            mapView.selectOrbital(noradId);

                            //if location
                            if(isLocation)
                            {
                                //move to now since not updated later
                                mapView.moveCamera(currentLocation.geo.latitude, currentLocation.geo.longitude);
                            }
                        }
                        else
                        {
                            //select nothing
                            mapView.deselectCurrent();
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {}
                });
                searchList.setSelectedValue(Universe.IDs.None);

                //setup search button
                showSearchButton.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        //reverse state
                        showSearch = !showSearch;
                        showSearchButton.setChecked(showSearch);
                        Settings.setMapShowSearchList(context, showSearch);

                        //update visibility
                        searchListLayout.setVisibility(showSearch ? View.VISIBLE : View.GONE);
                    }
                });

                //set to opposite then perform click to set it correctly
                showSearch = !Settings.getMapShowSearchList(context);
                showSearchButton.performClick();
            }
        }

        //Setup zoom buttons
        private static void setupZoomButtons(final Context context, final FloatingActionStateButton showZoomButton, final View zoomLayout, FloatingActionButton zoomInButton, FloatingActionButton zoomOutButton)
        {
            //setup zoom buttons
            showZoomButton.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    //reverse state
                    showZoom = !showZoom;
                    showZoomButton.setChecked(showZoom);
                    Settings.setMapShowZoom(context, showZoom);

                    //update visibility
                    zoomLayout.setVisibility(showZoom ? View.VISIBLE : View.GONE);
                }
            });
            CoordinatesFragment.Utils.setupZoomButton(context, mapView, zoomInButton, true);
            CoordinatesFragment.Utils.setupZoomButton(context, mapView, zoomOutButton, false);

            //set to opposite then perform click to set it correctly
            showZoom = !Settings.getMapShowZoom(context);
            showZoomButton.performClick();

        }

        //Setups show titles always button
        private static void setupAlwaysShowTitlesButton(final Context context, final FloatingActionStateButton showTitlesAlwaysButton)
        {
            //if show titles always button exists
            if(showTitlesAlwaysButton != null)
            {
                //setup show labels always button
                showTitlesAlwaysButton.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        //reverse state
                        showTitlesAlways = !showTitlesAlways;
                        showTitlesAlwaysButton.setChecked(showTitlesAlways);
                        Settings.setMapShowLabelsAlways(context, showTitlesAlways);

                        //update visibility
                        mapView.setShowTitlesAlways(showTitlesAlways);
                    }
                });

                //set to opposite to then perform click to set it correctly
                showTitlesAlways = !Settings.getMapShowLabelsAlways(context);
                showTitlesAlwaysButton.performClick();
            }
        }

        //Setup latitude/longitude button
        private static void setupLatLonButton(final Context context, final FloatingActionStateButton showLatLonButton)
        {
            //setup latitude/longitude button
            showLatLonButton.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    //reverse state
                    showDividers = !showDividers;
                    showLatLonButton.setChecked(showDividers);
                    Settings.setMapShowGrid(context, showDividers);

                    //update visibility
                    mapView.setLatitudeLongitudeGridEnabled(showDividers);
                }
            });

            //set to opposite then perform click to set it correctly
            showDividers = !Settings.getMapShowGrid(context);
            showLatLonButton.performClick();
        }

        //Setup path button
        private static void setupPathButton(final FloatingActionStateButton showPathButton, final int mapViewNoradId)
        {
            final boolean allMapOrbitals = (mapViewNoradId == Integer.MAX_VALUE);

            showPathButton.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    int index;
                    int orbitalCount = mapView.getOrbitalCount();

                    //reverse state
                    showPaths = !showPaths;
                    showPathButton.setChecked(showPaths);

                    //update visibility
                    for(index = 0; index < orbitalCount; index++)
                    {
                        mapView.setPathVisible(index, (showPaths && (allMapOrbitals || mapViewNoradId == mapView.getOrbitalNoradId(index))));
                    }
                }
            });

            //set to opposite then perform click to set it correctly
            showPaths = !showPaths;
            showPathButton.performClick();
        }

        //Setup icon scale button
        private static void setupIconScaleButton(final Context context, final FloatingActionStateButton iconSaleButton, final PlayBar scaleBar)
        {
            iconSaleButton.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    boolean setScaleVisible = (scaleBar.getVisibility() == View.GONE);

                    //if showing scale now
                    if(setScaleVisible)
                    {
                        //set scale to current value
                        scaleBar.setValue((int)(Math.floor(Settings.getMapMarkerScale(context) * 100)));
                    }
                    else
                    {
                        //undo any changes
                        mapView.setMarkerScale(Settings.getMapMarkerScale(context));
                    }

                    //toggle scale bar visibility
                    scaleBar.setVisibility(setScaleVisible ? View.VISIBLE : View.GONE);

                    //close menu
                    settingsMenu.close();
                }
            });

            //setup bar
            scaleBar.setButtonsVisible(true);
            scaleBar.setMin(Settings.IconScaleMin);
            scaleBar.setMax(Settings.IconScaleMax);
            scaleBar.setPlayIndexIncrementUnits(1);
            if(context != null)
            {
                scaleBar.setTitle(context.getString(R.string.title_icon_scale));
            }
            scaleBar.setPlayActivity(null);
            scaleBar.setOnSeekChangedListener(new PlayBar.OnPlayBarChangedListener()
            {
                @Override
                public void onProgressChanged(PlayBar seekBar, int progressValue, double subProgressPercent, boolean fromUser)
                {
                    //if map exists
                    if(mapView != null)
                    {
                        //update scale
                        mapView.setMarkerScale(progressValue / 100f);

                        //update text
                        seekBar.setScaleText(String.format(Locale.US, "%3d%%", progressValue));
                    }
                }
            });
            scaleBar.setButtonListeners(new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    //set scale
                    Settings.setMapMarkerScale(context, scaleBar.getValue() / 100f);
                    scaleBar.setVisibility(View.GONE);
                }
            }, new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    //if map exists
                    if(mapView != null)
                    {
                        //undo any changes
                        mapView.setMarkerScale(Settings.getMapMarkerScale(context));
                    }
                    scaleBar.setVisibility(View.GONE);
                }
            });
        }

        //Setup play bar
        private static void setupPlayBar(final FragmentActivity activity, PlayBar playBar, final Current.Coordinates.Item[] playbackItems, final CoordinatesFragment.OrbitalBase playbackMarker)
        {
            final int itemLength = playbackItems.length;

            playBar.setMax(itemLength - 1);
            playBar.setPlayActivity(activity);
            playBar.setOnSeekChangedListener(new PlayBar.OnPlayBarChangedListener()
            {
                @Override
                public void onProgressChanged(PlayBar seekBar, int progressValue, double subProgressPercent, boolean fromUser)
                {
                    //if a valid value
                    if(progressValue >= 0 && progressValue < itemLength)
                    {
                        //remember current item and point
                        double latitude;
                        double longitude;
                        double altitudeKm;
                        Item nextItem;
                        Item currentItem = playbackItems[progressValue];
                        Calendar playTime = Globals.getGMTTime();

                        //set play time
                        playTime.setTimeInMillis(currentItem.time.getTimeInMillis());

                        //if more points after current
                        if(progressValue + 1 < playbackItems.length)
                        {
                            //get add distance index percentage to current point
                            nextItem = playbackItems[progressValue + 1];
                            latitude = Globals.normalizeLatitude(currentItem.latitude + (Globals.latitudeDistance(currentItem.latitude, nextItem.latitude) * subProgressPercent));
                            longitude = Globals.normalizeLongitude(currentItem.longitude + (Globals.longitudeDistance(currentItem.longitude, nextItem.longitude) * subProgressPercent));
                            altitudeKm = currentItem.altitudeKm + ((nextItem.altitudeKm - currentItem.altitudeKm) * subProgressPercent);

                            //add distance percentage to play time
                            playTime.add(Calendar.MILLISECOND, (int)((nextItem.time.getTimeInMillis() - currentItem.time.getTimeInMillis()) * subProgressPercent));
                        }
                        else
                        {
                            //set point
                            latitude = currentItem.latitude;
                            longitude = currentItem.longitude;
                            altitudeKm = currentItem.altitudeKm;
                        }

                        //update marker
                        if(playbackMarker.getInfoVisible())
                        {
                            String timeString = Globals.getDateTimeString(playTime);
                            String coordinateString = Globals.getCoordinateString(activity, latitude, longitude, altitudeKm);

                            playbackMarker.setText(timeString + "\n" + coordinateString);
                            if(mapInfoText != null && Settings.usingMapMarkerInfoBottom())
                            {
                                String text = timeString + "\n" + coordinateString.replace("\n", Globals.COORDINATE_SEPARATOR);
                                mapInfoText.setText(text);
                            }
                            mapView.moveCamera(latitude, longitude);
                        }
                        playbackMarker.moveLocation(latitude, longitude, altitudeKm);
                    }
                }
            });
            playBar.setVisibility(View.VISIBLE);
        }

        //Creates a map view
        public static View onCreateMapView(final Selectable.ListFragment page, LayoutInflater inflater, ViewGroup container, boolean forGlobe, final Bundle savedInstanceState)
        {
            final Context context = page.getContext();
            final ViewGroup rootView = (ViewGroup)inflater.inflate(R.layout.coordinates_map_layout, container, false);
            final LinearLayout searchListLayout = rootView.findViewById(R.id.Map_Search_List_Layout);
            final IconSpinner searchList;
            Bundle args = new Bundle();

            //setup lists and status
            final int mapViewNoradId = (savedInstanceState != null ? savedInstanceState.getInt(MainActivity.ParamTypes.CoordinateNoradId, Integer.MAX_VALUE) : Integer.MAX_VALUE);
            final Current.Coordinates.Item[] savedItems = (savedInstanceState != null ? (Current.Coordinates.Item[])Calculate.PageAdapter.getSavedItems(Calculate.PageType.Coordinates) : null);
            final boolean allMapOrbitals = (mapViewNoradId == Integer.MAX_VALUE);
            final boolean useSavedPath = (page instanceof Calculate.Page && savedItems != null && savedItems.length > 0);
            final boolean rotateAllowed = Settings.getMapRotateAllowed(context);
            final Database.SatelliteData currentSatellite = (useSavedPath ? new Database.SatelliteData(context, savedItems[0].id) : null);

            //get menu and zoom displays
            final FrameLayout mapFrameLayout = rootView.findViewById(R.id.Map_Frame_Layout);
            final LinearLayout zoomLayout = rootView.findViewById(R.id.Map_Zoom_Layout);
            final ImageView compassImage = rootView.findViewById(R.id.Map_Compass_Image);
            settingsMenu = mapFrameLayout.findViewById(R.id.Map_Settings_Menu);
            final FloatingActionStateButton showSearchButton = (allMapOrbitals && !useSavedPath ? settingsMenu.addMenuItem(R.drawable.ic_search_black, R.string.title_show_search) : null);
            final FloatingActionStateButton showZoomButton = settingsMenu.addMenuItem(R.drawable.ic_unfold_more_white, R.string.title_show_zoom);
            final FloatingActionStateButton showTitlesAlwaysButton = (!useSavedPath ? settingsMenu.addMenuItem(R.drawable.ic_title_black, R.string.title_show_labels_always) : null);
            final FloatingActionStateButton showLatLonButton = settingsMenu.addMenuItem(R.drawable.ic_language_black, R.string.title_show_latitude_longitude);
            final FloatingActionStateButton showPathButton = (!useSavedPath ? settingsMenu.addMenuItem(R.drawable.orbit, R.string.title_show_path) : null);
            final FloatingActionStateButton iconScaleButton = settingsMenu.addMenuItem(R.drawable.ic_width_black, R.string.title_set_icon_scale);
            final FloatingActionButton zoomInButton = zoomLayout.findViewById(R.id.Map_Zoom_In_Button);
            final FloatingActionButton zoomOutButton = zoomLayout.findViewById(R.id.Map_Zoom_Out_Button);

            //get displays
            args.putInt(Whirly.ParamTypes.MapLayerType, Settings.getMapLayerType(context, forGlobe));
            mapInfoText = rootView.findViewById(R.id.Coordinate_Info_Text);
            mapView = (forGlobe ? new Whirly.GlobeFragment() : new Whirly.MapFragment());
            mapView.setArguments(args);
            searchList = (allMapOrbitals && !useSavedPath ? (IconSpinner)rootView.findViewById(R.id.Map_Search_List) : null);
            page.playBar = (useSavedPath ? (PlayBar)rootView.findViewById(R.id.Coordinate_Play_Bar) : null);
            page.scaleBar = rootView.findViewById(R.id.Coordinate_Scale_Bar);
            page.getChildFragmentManager().beginTransaction().replace(R.id.Map_View, (Fragment)mapView).commit();

            //setup search
            if(searchList != null)
            {
                setupSearch(context, showSearchButton, searchList, searchListLayout);
            }

            //if map info text exists and not using background
            if(mapInfoText != null && !Settings.getMapMarkerShowBackground(context))
            {
                //set as shadowed text
                Globals.setShadowedText(mapInfoText);
            }

            //if using saved path
            if(useSavedPath)
            {
                //setup header
                TextView header = (TextView)Globals.replaceView(R.id.Coordinate_Header, R.layout.header_text_view, inflater, rootView);
                header.setText(Globals.getHeaderText(context, currentSatellite.getName(), savedItems[0].time, savedItems[savedItems.length - 1].time));
            }

            //setup map view
            mapView.setOnReadyListener(new CoordinatesFragment.OnReadyListener()
            {
                @Override
                public void ready()
                {
                    //setup compass
                    compassImage.setVisibility(rotateAllowed ? View.VISIBLE : View.GONE);
                    if(rotateAllowed)
                    {
                        compassImage.setOnClickListener(new View.OnClickListener()
                        {
                            @Override
                            public void onClick(View view)
                            {
                                //update compass and map
                                compassImage.setRotation(0);
                                mapView.setHeading(0);
                            }
                        });
                        mapView.setOnRotatedListener(new CoordinatesFragment.OnRotatedListener()
                        {
                            @Override
                            public void rotated(double degrees)
                            {
                                //update compass
                                compassImage.setRotation((float)degrees);
                            }
                        });
                    }

                    //watch for movement
                    mapView.setOnMovedListener(new CoordinatesFragment.OnMovedListener()
                    {
                        @Override
                        public void moved(boolean userMotion)
                        {
                            //if user moved
                            if(userMotion)
                            {
                                //close settings menu
                                settingsMenu.close();

                                //if not using saved path
                                if(!useSavedPath)
                                {
                                    //deselect current
                                    mapView.deselectCurrent();
                                }
                            }
                        }
                    });

                    //setup markers
                    mapView.setMarkerScale(Settings.getMapMarkerScale(context));
                    mapView.setMarkerShowBackground(Settings.getMapMarkerShowBackground(context));
                    mapView.setMarkerShowShadow(Settings.getMapMarkerShowShadow(context));
                    mapView.setInfoLocation(Settings.getMapMarkerInfoLocation(context));

                    //if current location marker exists
                    if(currentLocationMarker != null)
                    {
                        //remove it
                        currentLocationMarker.remove();
                    }

                    //add and set current location
                    currentLocationMarker = mapView.addMarker(context, Universe.IDs.CurrentLocation, currentLocation);
                    if(context != null)
                    {
                        currentLocationMarker.setTitle(context.getString(R.string.title_location_current));
                    }
                    setCurrentLocation(page.getActivity(), currentLocation);

                    //setup zoom buttons
                    setupZoomButtons(context, showZoomButton, zoomLayout, zoomInButton, zoomOutButton);

                    //setup always show titles button
                    setupAlwaysShowTitlesButton(context, showTitlesAlwaysButton);

                    //setup latitude/longitude button
                    setupLatLonButton(context, showLatLonButton);

                    //setup icon scale button
                    setupIconScaleButton(context, iconScaleButton, page.scaleBar);

                    //if not using saved path
                    if(!useSavedPath)
                    {
                        //add orbitals
                        addOrbitals(context);

                        //setup path button
                        setupPathButton(showPathButton, mapViewNoradId);

                        //select and move to current location
                        if(searchList != null)
                        {
                            searchList.setSelectedValue(Universe.IDs.CurrentLocation);
                        }
                        mapView.moveCamera(currentLocation.geo.latitude, currentLocation.geo.longitude, CoordinatesFragment.DefaultNearZoom);

                        //send selection changes
                        mapView.setOnItemSelectionChangedListener(new CoordinatesFragment.OnItemSelectionChangedListener()
                        {
                            @Override
                            public void itemSelected(int noradId)
                            {
                                //if list is set
                                if(searchList != null)
                                {
                                    //update selection
                                    searchList.setSelectedValue(noradId);
                                }
                                //else if norad ID changing and exists
                                else if(noradId != Universe.IDs.None && noradId != mapView.getSelectedNoradId() && mapView.getSelectedNoradId() != -1)
                                {
                                    mapView.selectOrbital(noradId);
                                }
                            }
                        });
                    }
                    else
                    {
                        int index;

                        //create markers and points
                        final int itemLength = savedItems.length;
                        final CoordinatesFragment.OrbitalBase playbackMarker = mapView.addOrbital(context, currentSatellite, currentLocation);
                        final ArrayList<CoordinatesFragment.Coordinate> points = new ArrayList<>(itemLength);

                        //if at least 2 saved items
                        if(savedItems.length > 1)
                        {
                            //get speed increment
                            page.playBar.setPlayIndexIncrementUnits((savedItems[1].time.getTimeInMillis() - savedItems[0].time.getTimeInMillis()) / 1000.0);
                        }

                        //go through saved points
                        for(index = 0; index < itemLength; index++)
                        {
                            //remember current item and point
                            Item currentItem = savedItems[index];
                            CoordinatesFragment.Coordinate currentPoint = new CoordinatesFragment.Coordinate(currentItem.latitude, currentItem.longitude, currentItem.altitudeKm);

                            //if on first point
                            if(index == 0)
                            {
                                String text;
                                String timeString = Globals.getDateTimeString(currentItem.time);
                                String coordinateString = Globals.getCoordinateString(context, currentPoint.latitude, currentPoint.longitude, currentPoint.altitudeKm);

                                //set to first point
                                playbackMarker.setText(timeString + "\n" + coordinateString);
                                playbackMarker.moveLocation(currentPoint.latitude, currentPoint.longitude, currentPoint.altitudeKm);
                                if(mapInfoText != null && Settings.usingMapMarkerInfoBottom())
                                {
                                    text = timeString  + "\n" + coordinateString.replace("\n", Globals.COORDINATE_SEPARATOR);
                                    mapInfoText.setText(text);
                                    mapInfoText.setVisibility(View.VISIBLE);
                                }
                            }

                            //add to points
                            points.add(currentPoint);
                        }

                        //setup play bar
                        setupPlayBar(page.getActivity(), page.playBar, savedItems, playbackMarker);

                        //set points
                        playbackMarker.setPath(points);
                        playbackMarker.setPathVisible(true);

                        //show marker and move to it
                        playbackMarker.setVisible(true);
                        playbackMarker.setInfoVisible(true);
                        mapView.moveCamera(playbackMarker.getGeo().latitude, playbackMarker.getGeo().longitude, CoordinatesFragment.Utils.getZoom(savedItems[0].altitudeKm));
                    }
                }
            });

            return(rootView);
        }

        //Sets current location
        public static void setCurrentLocation(final Activity activity, ObserverType location)
        {
            currentLocation = location;
            if(currentLocationMarker != null || moonMarker != null)
            {
                activity.runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        double currentLatitude = currentLocation.geo.latitude;
                        double currentLongitude = currentLocation.geo.longitude;
                        double currentAltitudeKm = currentLocation.geo.altitudeKm;

                        if(currentLocationMarker != null)
                        {
                            currentLocationMarker.moveLocation(currentLatitude, currentLongitude, currentAltitudeKm);
                            currentLocationMarker.setText(Globals.getCoordinateString(activity, currentLatitude, currentLongitude, currentAltitudeKm));
                            currentLocationMarker.setVisible(currentLatitude != 0 || currentLongitude != 0 || currentAltitudeKm != 0);
                        }
                        if(moonMarker != null)
                        {
                            moonMarker.setImage(Universe.Moon.getPhaseImage(activity, currentLocation, System.currentTimeMillis()));
                        }
                    }
                });
            }
        }

        //Adds all orbital makers
        private static void addOrbitals(Context context)
        {
            Database.SatelliteData[] orbitals;

            //if map is set
            if(mapView != null)
            {
                //remove all old markers/orbitals
                mapView.removeOrbitals();

                //add orbitals
                orbitals = MainActivity.getSatellites();
                for(Database.SatelliteData currentOrbital : orbitals)
                {
                    //add orbital
                    CoordinatesFragment.OrbitalBase newOrbital = mapView.addOrbital(context, currentOrbital, currentLocation);

                    //if the moon
                    if(newOrbital.getData().getSatelliteNum() == Universe.IDs.Moon)
                    {
                        //remember it
                        moonMarker = newOrbital;
                    }
                }
            }
        }

        //Returns if closed settings menu
        public static boolean closeSettingsMenu()
        {
            return(settingsMenu != null && settingsMenu.close());
        }
    }

    //Page view
    public static class Page extends Selectable.ListFragment
    {
        public FloatingActionStateButton actionButton;

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

            //return desired column count
            return(page != PageType.Combined && Settings.getCurrentGridLayout(context) ? (widthDp >=  600 ? (((widthDp - 600) / 200) + 3) : 2) : 1);
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
        {
            int group = this.getGroupParam();
            int page = this.getPageParam();
            int subPage = this.getSubPageParam();
            boolean createLens = false;
            boolean createMapView = false;
            View newView = null;
            Context context = this.getContext();
            final Selectable.ListBaseAdapter listAdapter;

            //set default
            actionButton = null;
            if(savedInstanceState == null)
            {
                savedInstanceState = new Bundle();
            }

            //set list adapter based on page
            switch(page)
            {
                case PageType.View:
                case PageType.Passes:
                    switch(page)
                    {
                        case PageType.Passes:
                            Passes.Item[] savedItems = (Passes.Item[])Current.PageAdapter.getSavedItems(page);
                            listAdapter = new Passes.ItemListAdapter(context, page, savedItems, MainActivity.getSatellites(), MainActivity.getTimeZone(), false);

                            if(subPage == Globals.SubPageType.Lens)
                            {
                                savedInstanceState.putInt(MainActivity.ParamTypes.PathDivisions, 8);
                                savedInstanceState.putBoolean(MainActivity.ParamTypes.ForceShowPaths, true);
                            }
                            break;

                        default:
                        case PageType.View:
                            listAdapter = new ViewAngles.ItemListAdapter(context, MainActivity.getSatellites());
                            break;
                    }

                    //set if need to create lens
                    createLens = (subPage == Globals.SubPageType.Lens);
                    break;

                case PageType.Coordinates:
                    listAdapter = new Coordinates.ItemListAdapter(context, MainActivity.getSatellites(), MainActivity.getObserver());

                    //set if need to create map view
                    createMapView = (subPage == Globals.SubPageType.Map || subPage == Globals.SubPageType.Globe);
                    break;

                case PageType.Combined:
                    Combined.Item[] savedItems = (Combined.Item[])Current.PageAdapter.getSavedItems(page);
                    listAdapter = new Combined.ItemListAdapter(context, savedItems, MainActivity.getSatellites(), MainActivity.getTimeZone());

                    //set if need to create lens/map view
                    createLens = (subPage == Globals.SubPageType.Lens);
                    createMapView = (subPage == Globals.SubPageType.Map || subPage == Globals.SubPageType.Globe);
                    break;

                default:
                    listAdapter = null;
                    break;
            }

            //if need to create lens
            if(createLens)
            {
                //set orbital views
                setOrbitalViews();

                //create view
                newView = onCreateLensView(this, inflater, container, savedInstanceState);
            }
            //else if need to create map view
            else if(createMapView)
            {
                //create view
                savedInstanceState.putInt(MainActivity.ParamTypes.CoordinateNoradId, MainActivity.mapViewNoradID);
                newView = Coordinates.onCreateMapView(this, inflater, container, (subPage == Globals.SubPageType.Globe), savedInstanceState);
            }

            //if view is not set yet
            if(newView == null && listAdapter != null)
            {
                //create view
                newView = this.onCreateView(inflater, container, listAdapter, group, page);
            }

            //set change listeners
            setChangeListeners(selectList, listAdapter, page);

            //return view
            return(newView);
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
        protected int onActionModeConfirmDelete() { return(0); }

        @Override
        protected void onActionModeSave() {}

        @Override
        protected void onActionModeSync() {}

        @Override
        protected void onUpdateStarted() {}

        @Override
        protected void onUpdateFinished(boolean success) {}

        public void setChangeListeners(final RecyclerView selectList, final Selectable.ListBaseAdapter listAdapter, final int page)
        {
            if(selectList != null)
            {
                PageAdapter.setOrientationChangedListener(page, new OnOrientationChangedListener()
                {
                    @Override
                    public void orientationChanged()
                    {
                        View rootView = Page.this.getView();
                        View listColumns = (rootView != null ? rootView.findViewById(listAdapter.itemsRootViewID) : null);

                        Page.this.setListColumns(Page.this.getContext(), listColumns, page);
                    }
                });
            }
            if(listAdapter != null)
            {
                PageAdapter.setItemChangedListener(page, new OnItemsChangedListener()
                {
                    @Override
                    public void itemsChanged()
                    {
                        FragmentActivity activity = Page.this.getActivity();

                        //perform any needed sorting
                        PageAdapter.sortItems(page);

                        //if able to get activity
                        if(activity != null)
                        {
                            //update displays
                            activity.runOnUiThread(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    listAdapter.notifyDataSetChanged();
                                }
                            });
                        }
                    }
                });
                PageAdapter.setGraphChangedListener(page, createOnGraphChangedListener(listAdapter));
                PageAdapter.setPreview3dChangedListener(page, createOnPreview3dChangedListener(listAdapter));
                PageAdapter.setInformationChangedListener(page, createOnInformationChangedListener(listAdapter));
            }
        }
    }

    //Page adapter
    public static class PageAdapter extends Selectable.ListFragmentAdapter
    {
        private static final int[] itemSortBy = new int[PageType.PageCount];
        private static final boolean[] pendingSort = new boolean[PageType.PageCount];
        private static Combined.Item[] combinedItems;
        private static ViewAngles.Item[] viewItems;
        private static Passes.Item[] passItems;
        private static Coordinates.Item[] coordinateItems;
        private static final Items.NoradIndex.Comparer noradIndexComparer = new Items.NoradIndex.Comparer();
        private static ArrayList<Items.NoradIndex> combinedNoradIndex;
        private static ArrayList<Items.NoradIndex> viewNoradIndex;
        private static ArrayList<Items.NoradIndex> passNoradIndex;
        private static ArrayList<Items.NoradIndex> coordinateNoradIndex;
        private static final Selectable.ListFragment.OnOrientationChangedListener[] orientationChangedListeners = new Selectable.ListFragment.OnOrientationChangedListener[PageType.PageCount];
        private static final Selectable.ListFragment.OnItemsChangedListener[] itemsChangedListeners = new Selectable.ListFragment.OnItemsChangedListener[PageType.PageCount];
        private static final Selectable.ListFragment.OnGraphChangedListener[] graphChangedListeners = new Selectable.ListFragment.OnGraphChangedListener[PageType.PageCount];
        private static final Selectable.ListFragment.OnPreview3dChangedListener[] preview3dChangedListeners = new Selectable.ListFragment.OnPreview3dChangedListener[PageType.PageCount];
        private static final Selectable.ListFragment.OnInformationChangedListener[] informationChangedListeners = new Selectable.ListFragment.OnInformationChangedListener[PageType.PageCount];
        private static final Object[][] savedItems = new Object[PageType.PageCount][];

        public PageAdapter(FragmentManager fm, View parentView, Selectable.ListFragment.OnItemDetailButtonClickListener detailListener, Selectable.ListFragment.OnAdapterSetListener adapterListener, int[] subPg)
        {
            super(fm, parentView, null, null, null, null, detailListener, adapterListener, null, MainActivity.Groups.Current, subPg);
        }

        @Override
        public @NonNull Fragment getItem(int position)
        {
            int subPosition = (Settings.getCombinedCurrentLayout(currentContext) ? PageType.Combined : position);
            return(this.getItem(group, position, subPage[subPosition], new Page()));
        }

        @Override
        public @NonNull Object instantiateItem(@NonNull  ViewGroup container, int position)
        {
            Bundle params;
            Page newPage = (Page)setupItem((Page)super.instantiateItem(container, position));

            //set params
            params = newPage.getArguments();
            if(params == null)
            {
                params = new Bundle();
            }
            newPage.setArguments(params);

            return(newPage);
        }

        @Override
        public int getCount()
        {
            return(Settings.getCombinedCurrentLayout(currentContext) ? 1 : (PageType.PageCount - 1));
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

                default                 :
                    return(res.getString(R.string.title_invalid));
            }
        }

        //Sets view item(s)
        public static void setViewItems(ViewAngles.Item[] items)
        {
            viewItems = items;
            viewNoradIndex = new ArrayList<>(0);
        }
        public static void setViewItem(int index, ViewAngles.Item newItem)
        {
            if(viewItems != null && index >= 0 && index < viewItems.length)
            {
                viewItems[index] = newItem;
            }
        }

        //Sets pass item(s)
        public static void setPassItems(Passes.Item[] items)
        {
            passItems = items;
            passNoradIndex = new ArrayList<>(0);
        }
        public static void setPassItem(int index, Passes.Item newItem)
        {
            if(passItems != null && index >= 0 && index < passItems.length)
            {
                passItems[index] = newItem;
            }
        }

        //Sets coordinate item(s)
        public static void setCoordinateItems(Coordinates.Item[] items)
        {
            coordinateItems = items;
            coordinateNoradIndex = new ArrayList<>(0);
        }
        public static void setCoordinateItem(int index, Coordinates.Item newItem)
        {
            if(coordinateItems != null && index >= 0 && index < coordinateItems.length)
            {
                coordinateItems[index] = newItem;
            }
        }

        //Sets combined item(s)
        public static void setCombinedItems(Combined.Item[] items)
        {
            combinedItems = items;
            combinedNoradIndex = new ArrayList<>(0);
        }
        public static void setCombinedItem(int index, Combined.Item newItem)
        {
            if(combinedItems != null && index >= 0 && index < combinedItems.length)
            {
                combinedItems[index] = newItem;
            }
        }

        //Sort item(s)
        public static void sortItems(int page, int sortBy)
        {
            int index;

            //if a valid page
            if(page >= 0 && page < itemSortBy.length)
            {
                //update sort by
                itemSortBy[page] = sortBy;
            }

            switch(page)
            {
                case PageType.View:
                    if(viewItems != null)
                    {
                        Arrays.sort(viewItems, new ViewAngles.Item.Comparer(sortBy));

                        viewNoradIndex.clear();
                        for(index = 0; index < viewItems.length; index++)
                        {
                            viewNoradIndex.add(new Items.NoradIndex(viewItems[index].satellite.getSatelliteNum(), index));
                        }
                        Collections.sort(viewNoradIndex, new Items.NoradIndex.Comparer());
                    }
                    break;

                case PageType.Passes:
                    if(passItems != null && passNoradIndex != null)
                    {
                        Arrays.sort(passItems, new Passes.Item.Comparer(sortBy));

                        passNoradIndex.clear();
                        for(index = 0; index < passItems.length; index++)
                        {
                            passNoradIndex.add(new Items.NoradIndex(passItems[index].satellite.getSatelliteNum(), index));
                        }
                        Collections.sort(passNoradIndex, new Items.NoradIndex.Comparer());
                    }
                    break;

                case PageType.Coordinates:
                    if(coordinateItems != null && coordinateNoradIndex != null)
                    {
                        Arrays.sort(coordinateItems, new Coordinates.Item.Comparer(sortBy));

                        coordinateNoradIndex.clear();
                        for(index = 0; index < coordinateItems.length; index++)
                        {
                            coordinateNoradIndex.add(new Items.NoradIndex(coordinateItems[index].satellite.getSatelliteNum(), index));
                        }
                        Collections.sort(coordinateNoradIndex, new Items.NoradIndex.Comparer());
                    }
                    break;

                default:
                case PageType.Combined:
                    if(combinedItems != null && combinedNoradIndex != null)
                    {
                        Arrays.sort(combinedItems, new Combined.Item.Comparer(sortBy));

                        combinedNoradIndex.clear();
                        for(index = 0; index < combinedItems.length; index++)
                        {
                            combinedNoradIndex.add(new Items.NoradIndex(combinedItems[index].satellite.getSatelliteNum(), index));
                        }
                        Collections.sort(combinedNoradIndex, new Items.NoradIndex.Comparer());
                    }
                    break;
            }
        }
        public static void sortItems(Context context, int page, boolean forceSort)
        {
            int sortBy = (page >= 0 && page < PageType.PageCount ? (forceSort && context != null ? Settings.getCurrentSortBy(context, page) : itemSortBy[page]) : Items.SortBy.Name);

            //if a needed sort (i.e. forced or not a constant value)
            if(forceSort || (sortBy != Items.SortBy.Name && sortBy != Items.SortBy.PassStartTime && sortBy != Items.SortBy.PassDuration && sortBy != Items.SortBy.MaxElevation))
            {
                //sort items
                sortItems(page, sortBy);
            }
        }
        public static void sortItems(int page)
        {
            sortItems(null, page, false);
        }

        //Gets count of items in given page
        public static int getCount(int page)
        {
            switch(page)
            {
                case PageType.View:
                    return(viewItems == null ? 0 : viewItems.length);

                case PageType.Passes:
                    return(passItems == null ? 0 : passItems.length);

                case PageType.Coordinates:
                    return(coordinateItems == null ? 0 : coordinateItems.length);

                default:
                case PageType.Combined:
                    return(combinedItems == null ? 0 : combinedItems.length);

            }
        }

        //Returns true if given page has items
        public static boolean hasItems(int page)
        {
            return(getCount(page) > 0);
        }

        //Gets combined item(s)
        public static Combined.Item[] getCombinedItems()
        {
            return(combinedItems);
        }
        public static Combined.Item getCombinedItem(int index)
        {
            return(combinedItems != null && index >= 0 && index < combinedItems.length ? combinedItems[index] : null);
        }
        public static Combined.Item getCombinedItemByNorad(int noradId)
        {
            int index = Globals.divideFind(new Items.NoradIndex(noradId, -1), combinedNoradIndex, noradIndexComparer)[0];
            return(getCombinedItem(index >= 0 ? combinedNoradIndex.get(index).displayIndex : -1));
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
        public static ViewAngles.Item getViewAngleItemByNorad(int noradId)
        {
            int index = Globals.divideFind(new Items.NoradIndex(noradId, -1), viewNoradIndex, noradIndexComparer)[0];
            return(getViewAngleItem(index >= 0 ? viewNoradIndex.get(index).displayIndex : -1));
        }

        //Gets pass item(s)
        public static Passes.Item[] getPassItems()
        {
            return(passItems);
        }
        public static Passes.Item getPassItem(int index)
        {
            return(passItems != null && index >= 0 && index < passItems.length ? passItems[index] : null);
        }
        public static Passes.Item getPassItemByNorad(int noradId)
        {
            int index = Globals.divideFind(new Items.NoradIndex(noradId, -1), passNoradIndex, noradIndexComparer)[0];
            return(getPassItem(index >= 0 ? passNoradIndex.get(index).displayIndex : -1));
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
        public static Coordinates.Item getCoordinatesItemByNorad(int noradId)
        {
            int index = Globals.divideFind(new Items.NoradIndex(noradId, -1), coordinateNoradIndex, noradIndexComparer)[0];
            return(getCoordinatesItem(index >= 0 ? coordinateNoradIndex.get(index).displayIndex : -1));
        }

        //Set pending
        public static void setPendingSort(int pageNum, boolean pending)
        {
            //if a valid page
            if(pageNum < pendingSort.length)
            {
                //set pending status
                pendingSort[pageNum] = pending;
            }
        }

        //Returns if pending sort for given page
        public static boolean hasPendingSort(int pageNum)
        {
            return(pageNum < pendingSort.length && pendingSort[pageNum]);
        }

        //Get saved items
        public static Object[] getSavedItems(int pageNum)
        {
            //if a valid page
            if(pageNum >= 0 && pageNum < savedItems.length)
            {
                //get saved items
                return(savedItems[pageNum]);
            }

            return(null);
        }

        //Set saved items
        public void setSavedItems(int pageNum, Object[] saveItems)
        {
            //if a valid page
            if(pageNum >= 0 && pageNum < savedItems.length)
            {
                //set saved items
                savedItems[pageNum] = saveItems;
            }
        }

        //Sets orientation changed listener for the given page
        public static void setOrientationChangedListener(int position, Selectable.ListFragment.OnOrientationChangedListener listener)
        {
            //if a valid page
            if(position < PageType.PageCount)
            {
                //set listener
                orientationChangedListeners[position] = listener;
            }
        }

        //Sets item changed listener for the given page
        public static void setItemChangedListener(int position, Selectable.ListFragment.OnItemsChangedListener listener)
        {
            //if a valid page
            if(position < PageType.PageCount)
            {
                //set listener
                itemsChangedListeners[position] = listener;
            }
        }

        //Sets graph changed listener for the given page
        public static void setGraphChangedListener(int position, Selectable.ListFragment.OnGraphChangedListener listener)
        {
            //if a valid page
            if(position < PageType.PageCount)
            {
                //set listener
                graphChangedListeners[position] = listener;
            }
        }

        //Sets preview 3d changed listener for the given page
        public static void setPreview3dChangedListener(int position, Selectable.ListFragment.OnPreview3dChangedListener listener)
        {
            //if a valid page
            if(position < PageType.PageCount)
            {
                //set listener
                preview3dChangedListeners[position] = listener;
            }
        }

        //Sets information changed listener for the given page
        public static void setInformationChangedListener(int position, Selectable.ListFragment.OnInformationChangedListener listener)
        {
            //if a valid page
            if(position < PageType.PageCount)
            {
                //set listener
                informationChangedListeners[position] = listener;
            }
        }

        //Calls orientation changed listener for the given page
        public static void notifyOrientationChangedListener(int position)
        {
            //if a valid page and listener exists
            if(position < PageType.PageCount && orientationChangedListeners[position] != null)
            {
                //call listener
                orientationChangedListeners[position].orientationChanged();
            }
        }

        //Calls item changed listener for the given page
        public static void notifyItemsChanged(int position)
        {
            //if a valid page and listener exists
            if(position < PageType.PageCount && itemsChangedListeners[position] != null)
            {
                //call listener
                itemsChangedListeners[position].itemsChanged();
            }
        }

        //Call graph changed listener for the given page
        public static void notifyGraphChanged(int position, Database.SatelliteData orbital, ArrayList<CalculateViewsTask.OrbitalView> pathPoints)
        {
            //if a valid page and listener exists
            if(position < PageType.PageCount && graphChangedListeners[position] != null)
            {
                //call listener
                graphChangedListeners[position].graphChanged(orbital, pathPoints, null, null);
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

    public static boolean showPaths = false;
    private static boolean showHorizon = false;
    public static boolean showCalibration = false;
    public static CameraLens cameraView;
    public static CalculateViewsTask.OrbitalPathBase[] orbitalViews = new CalculateViewsTask.OrbitalPathBase[0];

    //Begin calculating view information
    public static CalculateViewsTask calculateViews(Context context, ObserverType observer, double julianStartDate, double julianEndDate, double dayIncrement, CalculateViewsTask.OnProgressChangedListener listener)
    {
        CalculateViewsTask task;

        //start calculating for 1 day, 12 minute increments
        task = new CalculateViewsTask(listener);
        task.execute(context, orbitalViews, null, observer, julianStartDate, julianEndDate, dayIncrement, true, true, true, true);

        //return task
        return(task);
    }

    //Sets all orbital views
    private static void setOrbitalViews()
    {
        int index;
        Database.SatelliteData[] satellites;

        //get views
        satellites = MainActivity.getSatellites();
        orbitalViews = new CalculateViewsTask.OrbitalPathBase[satellites.length];
        for(index = 0; index < satellites.length; index++)
        {
            //set view
            orbitalViews[index] = new CalculateViewsTask.OrbitalPathBase(satellites[index]);
        }
    }

    //Creates lens view
    public static View onCreateLensView(Selectable.ListFragment pageFragment, LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        final Context context = pageFragment.getContext();
        final Resources res = (context != null ? context.getResources() : null);
        Bundle savedState = (savedInstanceState != null ? savedInstanceState : new Bundle());
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.lens_view, container, false);
        FrameLayout lensLayout = rootView.findViewById(R.id.Lens_Layout);
        int page = pageFragment.getPageParam();
        int passIndex = savedState.getInt(MainActivity.ParamTypes.PassIndex, 0);
        int pathDivisions = savedState.getInt(MainActivity.ParamTypes.PathDivisions, 8);
        boolean onCalculateView = (page == Calculate.PageType.View);
        boolean onCalculatePasses = (page == Calculate.PageType.Passes);
        boolean onCalculateIntersection = (page == Calculate.PageType.Intersection);
        ViewAngles.Item[] savedViewItems = (ViewAngles.Item[])Calculate.PageAdapter.getSavedItems(Calculate.PageType.View);
        Passes.Item[] savedPassItems = (savedState.getBoolean(MainActivity.ParamTypes.GetPassItems, false) ? (Passes.Item[])Calculate.PageAdapter.getSavedItems(onCalculateIntersection ? Calculate.PageType.Intersection : Calculate.PageType.Passes) : null);
        boolean forceShowPaths = savedState.getBoolean(MainActivity.ParamTypes.ForceShowPaths, false);
        boolean useSavedViewPath = (onCalculateView && savedViewItems != null && savedViewItems.length > 0);
        boolean useSavedPassPath = ((onCalculatePasses || onCalculateIntersection) && savedPassItems != null && passIndex < savedPassItems.length && savedPassItems[passIndex].passViews != null && savedPassItems[passIndex].passViews.length > 0);
        final boolean useSaved = (useSavedViewPath || useSavedPassPath);
        final Passes.Item currentSavedPathIem = (useSavedPassPath ? savedPassItems[passIndex] : null);
        final CalculateViewsTask.OrbitalView[] passViews = (useSavedPassPath ? currentSavedPathIem.passViews : null);
        final Database.SatelliteData currentSatellite = (useSaved ? new Database.SatelliteData(context, (useSavedViewPath ? savedViewItems[0].id : currentSavedPathIem.id)) : null);
        final FloatingActionStateButton showCalibrationButton;
        final FloatingActionStateButton showHorizonButton;
        final FloatingActionStateButton showPathButton;
        final LinearLayout buttonLayout = rootView.findViewById(R.id.Lens_Button_Layout);
        final MaterialButton selectButton = rootView.findViewById(R.id.Lens_Select_Button);
        final MaterialButton resetButton = rootView.findViewById(R.id.Lens_Reset_Button);
        final MaterialButton cancelButton = rootView.findViewById(R.id.Lens_Cancel_Button);

        //if camera is already set
        if(cameraView != null)
        {
            //clear it
            cameraView = null;
        }

        //create camera
        cameraView = new CameraLens(context);
        cameraView.updateAzDeclination(MainActivity.getObserver());
        cameraView.pathDivisions = pathDivisions;
        cameraView.showPaths = (showPaths || useSaved);       //if showing paths or using a saved path
        lensLayout.addView(cameraView, 0);              //add before menu

        //get help text
        cameraView.helpText = rootView.findViewById(R.id.Lens_Help_Text);

        //create settings menu
        cameraView.settingsMenu = rootView.findViewById(R.id.Lens_Settings_Menu);
        showCalibrationButton = (!useSaved ? cameraView.settingsMenu.addMenuItem(R.drawable.ic_filter_center_focus_black, R.string.title_align) : null);
        showHorizonButton = cameraView.settingsMenu.addMenuItem(R.drawable.ic_remove_black, R.string.title_show_horizon);
        showPathButton = (!useSaved && !forceShowPaths ? cameraView.settingsMenu.addMenuItem(R.drawable.orbit, R.string.title_show_path) : null);

        //setup calibration button
        showCalibration = false;
        if(showCalibrationButton != null)
        {
            showCalibrationButton.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    //reverse state
                    showCalibration = !showCalibration;
                    showCalibrationButton.setChecked(showCalibration);

                    //if camera is set
                    if(cameraView != null)
                    {
                        //update display
                        cameraView.showCalibration = showCalibration;
                        if(!showCalibration)
                        {
                            cameraView.resetAlignmentStatus();
                        }
                        buttonLayout.setVisibility(showCalibration ? View.VISIBLE : View.GONE);
                        selectButton.setText(R.string.title_select);
                        resetButton.setVisibility(View.VISIBLE);
                        cameraView.helpText.setVisibility(showCalibration || cameraView.hasCompassError() ? View.VISIBLE : View.GONE);
                        cameraView.helpText.setText(R.string.title_select_orbital);
                        cancelButton.setText(R.string.title_cancel);

                        //update event
                        selectButton.setOnClickListener(new View.OnClickListener()
                        {
                            @Override
                            public void onClick(View v)
                            {
                                //if no orbital selected yet
                                if(!cameraView.haveSelectedOrbital())
                                {
                                    //try to select nearest
                                    if(cameraView.selectNearest())
                                    {
                                        //update display
                                        selectButton.setText(R.string.title_align);
                                        resetButton.setVisibility(View.GONE);
                                        cameraView.helpText.setText(R.string.title_set_actual_position);
                                    }
                                }
                                else if(cameraView.needAlignmentCenter())
                                {
                                    //update status
                                    cameraView.setAlignmentCenter();

                                    //adjust offset
                                    cameraView.updateUserAzimuthOffset();

                                    //update display
                                    cameraView.helpText.setText(R.string.title_align_bottom_left);
                                    cancelButton.setText(R.string.title_skip);
                                }
                                else if(cameraView.needAlignmentBottomLeft())
                                {
                                    //update status
                                    cameraView.setAlignmentBottomLeft();

                                    //update display
                                    cameraView.helpText.setText(R.string.title_align_top_right);
                                }
                                else if(cameraView.needAlignmentTopRight())
                                {
                                    //update status
                                    cameraView.setAlignmentTopRight();

                                    //set alignment
                                    cameraView.setCalculatedAlignment();

                                    //stop calibration
                                    showCalibrationButton.performClick();
                                }
                            }
                        });

                        //if not showing calibration but have compass error
                        if(!showCalibration && cameraView.hasCompassError())
                        {
                            //set text for compass error
                            cameraView.helpText.setText(R.string.title_compass_inaccurate_fix);
                        }
                    }
                }
            });
        }
        resetButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //reset alignment
                cameraView.resetAlignment();

                //if button exists
                if(showCalibrationButton != null)
                {
                    //close, note: only shown when showing calibration, so will always close
                    showCalibrationButton.performClick();
                }
            }
        });
        cancelButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //if button exists
                if(showCalibrationButton != null)
                {
                    //cancel, note: only shown when showing calibration, so will always close
                    showCalibrationButton.performClick();
                }
            }
        });
        cameraView.setStopCalibrationListener(new CameraLens.OnStopCalibrationListener()
        {
            @Override
            public void stopCalibration()
            {
                //if button exists
                if(showCalibrationButton != null)
                {
                    //close, note: only shown when showing calibration, so will always close
                    showCalibrationButton.performClick();
                }
            }
        });

        //setup horizon button
        showHorizon = Settings.getLensShowHorizon(context);
        showHorizonButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //reverse state
                showHorizon = !showHorizon;
                showHorizonButton.setChecked(showHorizon);
                Settings.setLensShowHorizon(context, showHorizon);

                //if camera is set
                if(cameraView != null)
                {
                    //update display
                    cameraView.showHorizon = showHorizon;
                }
            }
        });

        //set to opposite then perform click to set it correctly
        showHorizon = !showHorizon;
        showHorizonButton.performClick();

        //if lens first run
        if(Settings.getLensFirstRun(context))
        {
            //show AR warning about importance of parental supervision and being aware of surroundings
            Globals.showConfirmDialog(context, Globals.getDrawable(context, R.drawable.ic_warning_black, true), (res != null ? res.getString(R.string.title_caution) : ""), (res != null ? res.getString(R.string.desc_ar_warning) : ""), (res != null ? res.getString(R.string.title_ok) : ""), null, false, null, null, new DialogInterface.OnDismissListener()
            {
                @Override
                public void onDismiss(DialogInterface dialogInterface)
                {
                    //if not using a saved path
                    if(!useSaved)
                    {
                        //show alignment dialog
                        cameraView.showCompassAlignmentDialog(new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                //if button exists
                                if(showCalibrationButton != null)
                                {
                                    //click it to show
                                    showCalibrationButton.performClick();
                                }
                            }
                        });
                    }
                }
            });
        }

        //if using a saved path
        if(useSaved)
        {
            //setup header
            TextView header = (TextView)Globals.replaceView(R.id.Lens_Header, R.layout.header_text_view, inflater, rootView);
            Calendar startTime = (useSavedViewPath ? savedViewItems[0].time : passViews[0].gmtTime);
            Calendar endTime = (useSavedViewPath ? savedViewItems[savedViewItems.length - 1].time : passViews[passViews.length - 1].gmtTime);
            header.setText(Globals.getHeaderText(context, (onCalculateIntersection && currentSavedPathIem != null ? currentSavedPathIem.name : currentSatellite.getName()), startTime, endTime));
            header.setVisibility(View.VISIBLE);

            //setup play bar
            cameraView.playBar = rootView.findViewById(R.id.Lens_Play_Bar);
            if(useSavedViewPath)
            {
                if(savedViewItems.length > 1)
                {
                    cameraView.playBar.setPlayIndexIncrementUnits((savedViewItems[1].julianDate - savedViewItems[0].julianDate) * Calculations.SecondsPerDay);
                }
                cameraView.playBar.setMax(savedViewItems.length - 1);
            }
            else
            {
                if(passViews.length > 1)
                {
                    cameraView.playBar.setPlayIndexIncrementUnits((passViews[1].julianDate - passViews[0].julianDate) * Calculations.SecondsPerDay);
                }
                cameraView.playBar.setMax(passViews.length - 1);
            }
            cameraView.playBar.setPlayActivity(pageFragment.getActivity());
            cameraView.playBar.setVisibility(View.VISIBLE);
        }
        else
        {
            //if forcing shown paths
            if(forceShowPaths)
            {
                //always show paths
                cameraView.showPaths = true;
            }
            else
            {
                //setup path button
                showPathButton.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        //reverse state
                        showPaths = !showPaths;
                        showPathButton.setChecked(showPaths);

                        //if camera is set
                        if(cameraView != null)
                        {
                            //update display
                            cameraView.showPaths = showPaths;
                        }
                    }
                });
                showPathButton.setChecked(showPaths);

                //if this is Current.Page
                if(pageFragment instanceof Page)
                {
                    //set action button
                    ((Page)pageFragment).actionButton = showPathButton;
                }
            }

            //remove play bar
            cameraView.playBar = null;
        }

        //return view
        return(rootView);
    }
}
