package com.nikolaiapps.orbtrack;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
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
import java.lang.ref.WeakReference;
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
        static final int Combined = 0;
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
                    PageAdapter.setCombinedItems((Combined.Item[])items);
                    break;

                case MainActivity.Groups.Calculate:
                    switch(page)
                    {
                        case Calculate.PageType.View:
                            Calculate.PageAdapter.setViewItems((Calculate.ViewAngles.Item[])items);
                            break;

                        case Calculate.PageType.Passes:
                            Calculate.PageAdapter.setPassItems((Calculate.Passes.Item[])items);
                            break;

                        case Calculate.PageType.Coordinates:
                            Calculate.PageAdapter.setCoordinateItems((Calculate.Coordinates.Item[])items);
                            break;

                        case Calculate.PageType.Intersection:
                            Calculate.PageAdapter.setIntersectionItems((Calculate.Passes.Item[])items);
                            break;
                    }
                    break;
            }
        }
        public void set(int index, Object newItem)
        {
            switch(group)
            {
                case MainActivity.Groups.Current:
                    PageAdapter.setCombinedItem(index, (Combined.Item)newItem);
                    break;

                case MainActivity.Groups.Calculate:
                    switch(page)
                    {
                        case Calculate.PageType.View:
                            Calculate.PageAdapter.setViewItem(index, (Calculate.ViewAngles.Item)newItem);
                            break;

                        case Calculate.PageType.Passes:
                            Calculate.PageAdapter.setPassItem(index, (Calculate.Passes.Item)newItem);
                            break;

                        case Calculate.PageType.Coordinates:
                            Calculate.PageAdapter.setCoordinateItem(index, (Calculate.Coordinates.Item)newItem);
                            break;

                        case Calculate.PageType.Intersection:
                            Calculate.PageAdapter.setIntersectionItem(index, (Calculate.Passes.Item)newItem);
                            break;
                    }
                    break;
            }
        }

        public void sort()
        {
            if(group == MainActivity.Groups.Current)
            {
                PageAdapter.sortItems(Settings.getCurrentSortBy());
            }
        }

        private Object get(int index)
        {
            switch(group)
            {
                case MainActivity.Groups.Current:
                    return(PageAdapter.getCombinedItem(index));

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
        public Calculate.ViewAngles.Item getViewItem(int index)
        {
            return((Calculate.ViewAngles.Item)get(index));
        }
        public Calculate.Passes.Item getPassItem(int index)
        {
            return((Calculate.Passes.Item)get(index));
        }
        public Calculate.Coordinates.Item getCoordinateItem(int index)
        {
            return((Calculate.Coordinates.Item)get(index));
        }

        public int getCount()
        {
            switch(group)
            {
                case MainActivity.Groups.Current:
                    return(PageAdapter.getItemCount());

                case MainActivity.Groups.Calculate:
                    return(Calculate.PageAdapter.getCount(page));

                default:
                    return(0);
            }
        }

        public static int getSortByImageId(int stringId)
        {
            if(stringId == R.string.title_azimuth)
            {
                return(R.drawable.compass_black_white);
            }
            else if(stringId == R.string.title_elevation)
            {
                return(R.drawable.ic_arrow_back_white);
            }
            else if(stringId == R.string.title_range)
            {
                return(R.drawable.distance);
            }
            else if(stringId == R.string.title_pass_duration)
            {
                return(R.drawable.ic_timer_black);
            }
            else if(stringId == R.string.title_latitude || stringId == R.string.title_longitude)
            {
                return(R.drawable.earth_vertical_lines);
            }
            else    //R.string.title_name, R.string.title_pass_elevation, R.string.title_pass_start, or R.string.title_altitude
            {
                return(R.drawable.ic_launcher_clear);
            }
        }

        public static int[] getSortByIds()
        {
            return(sortByCombinedIds);
        }
    }

    //Combined
    public static abstract class Combined
    {
        //Item
        public static class Item extends CalculateService.PassData
        {
            public final boolean tleIsAccurate;
            public float azimuth;
            public float elevation;
            public float rangeKm;
            public float speedKms;
            public float latitude;
            public float longitude;
            public float altitudeKm;
            private String name;
            private String ownerCode;
            private Drawable icon;
            private View passQualityView;
            private TextView azText;
            private TextView elText;
            private TextView nameText;
            private TextView rangeText;
            private TextView speedText;
            private TextView startText;
            private TextView elMaxText;
            private TextView elMaxTitle;
            private TextView durationText;
            private TextView latitudeText;
            private TextView longitudeText;
            private LinearProgressIndicator passProgress;
            private CircularProgressIndicator passLoadingProgress;
            private LinearLayout passLayout;
            private LinearLayout passStartLayout;
            private LinearLayout passDurationLayout;
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

            public Item(Context context, int index, Database.SatelliteData currentSatellite, boolean usePathProgress, boolean usePathQuality)
            {
                super(index, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, false, false, false, false, usePathProgress, usePathQuality, null, null, "", null, null, (currentSatellite != null ? currentSatellite.satellite : null), 0, null);

                azimuth = elevation = rangeKm = speedKms = latitude = longitude = altitudeKm = Float.MAX_VALUE;

                if(currentSatellite != null)
                {
                    id = currentSatellite.getSatelliteNum();
                    orbitalType = orbital2Type = currentSatellite.getOrbitalType();
                    icon = Globals.getOrbitalIcon(context, MainActivity.getObserver(), id, orbitalType);
                    name = currentSatellite.getName();
                    ownerCode = currentSatellite.getOwnerCode();
                    satellite = currentSatellite.satellite;
                }

                tleIsAccurate = (currentSatellite != null && currentSatellite.getTLEIsAccurate());
            }

            public void setLoading(boolean loading)
            {
                int passVisibility = (loading || !tleIsAccurate ? View.GONE : View.VISIBLE);

                if(passLoadingProgress != null)
                {
                    passLoadingProgress.setVisibility(loading && tleIsAccurate ? View.VISIBLE : View.GONE);
                }
                if(passLayout != null)
                {
                    passLayout.setVisibility(passVisibility);
                }
                if(passStartLayout != null)
                {
                    passStartLayout.setVisibility(passVisibility);
                }
                if(passDurationLayout != null)
                {
                    passDurationLayout.setVisibility(passVisibility);
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
                    text = (rangeKm != Float.MAX_VALUE ? (Globals.getKmUnitValueString(rangeKm, 0) + " " + kmUnit) : "-");
                    rangeText.setText(text);
                }

                if(speedImage != null)
                {
                    speedImage.setRotation(speedKms >= 0 ? 180 : 0);
                }

                if(speedText != null)
                {
                    text = (speedKms != Float.MAX_VALUE ? (Globals.getKmUnitValueString(speedKms) + " " + kmUnit + "/s") : "-");
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

                if(startText != null)
                {
                    startText.setText(inUnknownPassTimeStartNow() ? res.getString(R.string.title_now) : !passStartFound ? Globals.getUnknownString(context) : Globals.getDateString(context, passTimeStart, zone, true, false));
                }

                if(durationText != null)
                {
                    durationText.setText(!passStartFound && passTimeStart == null ? Globals.getUnknownString(context) : Globals.getTimeBetween(context, passTimeStart, passTimeEnd));
                }

                if(elMaxTitle != null)
                {
                    elMaxTitle.setText(Globals.Symbols.Elevating);
                    elMaxTitle.setVisibility(isKnownPassElevationMax() ? View.VISIBLE : View.INVISIBLE);
                }

                if(elMaxText != null)
                {
                    elMaxText.setText(isKnownPassElevationMax() ? Globals.getDegreeString(passElMax) : "");
                }

                if(passQualityView != null)
                {
                    passQualityView.setBackgroundColor(!showPassQuality || !isKnownPassElevationMax() ? Color.TRANSPARENT : passElMax >= 70 ? Color.CYAN : passElMax >= 45 ? Color.GREEN : passElMax >= 20 ? Color.YELLOW : Color.RED);
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
            private final boolean usingMaterial;
            private final Items combinedItems;

            public ItemListAdapter(Context context, Combined.Item[] savedItems, Database.SatelliteData[] orbitals)
            {
                super(context);

                int index;
                boolean usePathProgress = Settings.getListPathProgress(context);
                boolean usePassQuality = Settings.getListPassQuality(context);

                //remember using material and layout ID
                usingMaterial = Settings.getMaterialTheme(context);
                this.itemsRefID = (usingMaterial ? R.layout.current_combined_material_item : R.layout.current_combined_item);

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
                        combinedItems.set(index, new Item(context, index, orbitals[index], usePathProgress, usePassQuality));
                    }
                }
                combinedItems.sort();

                //ID stays the same
                this.setHasStableIds(true);
            }

            @Override
            public @NonNull RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
            {
                View itemView = LayoutInflater.from(parent.getContext()).inflate(this.itemsRefID, parent, false);
                ItemHolder itemHolder = new ItemHolder(itemView);

                setItemSelector(itemView);
                setViewClickListeners(itemView, itemHolder);
                return(itemHolder);
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position)
            {
                int sortBy = Settings.getCurrentSortBy();
                int visibility;
                Item currentItem = combinedItems.getCombinedItem(position);
                View itemView = holder.itemView;
                View dataLayout;
                View outdatedText;
                LinearLayout azLayout;
                LinearLayout elLayout;
                LinearLayout latitudeLayout;
                LinearLayout longitudeLayout;
                LinearLayout rangeLayout;
                LinearLayout speedLayout;

                dataLayout = itemView.findViewById(R.id.Combined_Item_Data_Layout);
                azLayout = itemView.findViewById(R.id.Combined_Item_Az_Layout);
                elLayout = itemView.findViewById(R.id.Combined_Item_El_Layout);
                latitudeLayout = itemView.findViewById(R.id.Combined_Item_Latitude_Layout);
                longitudeLayout = itemView.findViewById(R.id.Combined_Item_Longitude_Layout);
                rangeLayout = itemView.findViewById(R.id.Combined_Item_Range_Layout);
                speedLayout = itemView.findViewById(R.id.Combined_Item_Speed_Layout);
                outdatedText = itemView.findViewById(R.id.Combined_Item_Outdated_Text);
                currentItem.azImage = itemView.findViewById(R.id.Combined_Item_Az_Image);
                currentItem.azText = itemView.findViewById(R.id.Combined_Item_Az_Text);
                currentItem.elText = itemView.findViewById(R.id.Combined_Item_El_Text);
                currentItem.elImage = itemView.findViewById(R.id.Combined_Item_Elevation_Image);
                currentItem.rangeText = itemView.findViewById(R.id.Combined_Item_Range_Text);
                currentItem.speedText = itemView.findViewById(R.id.Combined_Item_Speed_Text);
                currentItem.speedImage = itemView.findViewById(R.id.Combined_Item_Speed_Image);
                currentItem.startText = itemView.findViewById(R.id.Combined_Item_Start_Text);
                currentItem.elMaxText = itemView.findViewById(R.id.Combined_Item_El_Max_Text);
                currentItem.elMaxTitle = itemView.findViewById(R.id.Combined_Item_El_Max_Title);
                currentItem.durationText = itemView.findViewById(R.id.Combined_Item_Duration_Text);
                currentItem.nameImage = itemView.findViewById(R.id.Combined_Item_Name_Image);
                currentItem.nameText = itemView.findViewById(R.id.Combined_Item_Name_Text);
                currentItem.passProgress = itemView.findViewById(R.id.Combined_Item_Pass_Progress);
                currentItem.passLoadingProgress = itemView.findViewById(R.id.Combined_Item_Pass_Loading_Progress);
                currentItem.passLayout = itemView.findViewById(R.id.Combined_Item_Pass_Layout);
                currentItem.passStartLayout = itemView.findViewById(R.id.Combined_Item_Pass_Start_Layout);
                currentItem.passDurationLayout = itemView.findViewById(R.id.Combined_Item_Pass_Duration_Layout);
                currentItem.passQualityView = itemView.findViewById(R.id.Combined_Item_Pass_Quality_View);
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
                if(usingMaterial)
                {
                    visibility = (currentItem.tleIsAccurate && (sortBy == Items.SortBy.Name || sortBy == Items.SortBy.Azimuth || sortBy == Items.SortBy.Elevation || sortBy == Items.SortBy.PassStartTime || sortBy == Items.SortBy.PassDuration || sortBy == Items.SortBy.MaxElevation) ? View.VISIBLE : View.INVISIBLE);
                    if(azLayout != null)
                    {
                        azLayout.setVisibility(visibility);
                    }
                    if(elLayout != null)
                    {
                        elLayout.setVisibility(visibility);
                    }

                    visibility = (currentItem.tleIsAccurate && (sortBy == Items.SortBy.Latitude || sortBy == Items.SortBy.Longitude) ? View.VISIBLE : View.GONE);
                    if(latitudeLayout != null)
                    {
                        latitudeLayout.setVisibility(visibility);
                    }
                    if(longitudeLayout != null)
                    {
                        longitudeLayout.setVisibility(visibility);
                    }

                    visibility = (currentItem.tleIsAccurate && (sortBy == Items.SortBy.Range || sortBy == Items.SortBy.Altitude) ? View.VISIBLE : View.GONE);
                    if(rangeLayout != null)
                    {
                        rangeLayout.setVisibility(visibility);
                    }
                    if(speedLayout != null)
                    {
                        speedLayout.setVisibility(visibility);
                    }
                }

                currentItem.setLoading(!currentItem.passCalculateFinished && currentItem.tleIsAccurate);
                currentItem.updateDisplays(currentContext, MainActivity.getTimeZone());
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
                final TimeZone currentZone = MainActivity.getTimeZone();
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
                final ItemDetailDialog detailDialog = new ItemDetailDialog(currentContext, listInflater, currentItem.id, currentItem.name, currentItem.ownerCode, currentItem.icon, itemDetailButtonClickListener);
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
                                            text = (currentItem.rangeKm != Float.MAX_VALUE ? Globals.getKmUnitValueString(currentItem.rangeKm) : "-") + " " + kmUnit;
                                            break;

                                        case 3:
                                            text = (haveGeo ? Globals.getLatitudeDirectionString(res, currentItem.latitude, 4) : "-");
                                            break;

                                        case 4:
                                            text = (haveGeo ? Globals.getLongitudeDirectionString(res, currentItem.longitude, 4) : "-");
                                            break;

                                        case 5:
                                            text = (haveGeo ? Globals.getKmUnitValueString(currentItem.altitudeKm) : "-") + " " + kmUnit;
                                            break;

                                        case 6:
                                            text = (currentItem.speedKms != Float.MAX_VALUE ? Globals.getKmUnitValueString(currentItem.speedKms) : "-") + " " + kmUnit + "/" + res.getString(R.string.abbrev_seconds_lower);
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
                                            text = (currentItem.inUnknownPassTimeStartNow() ? res.getString(R.string.title_now) : Globals.getDateString(currentContext, currentItem.passTimeStart, currentZone, false));
                                            break;

                                        case 10:
                                            title = endString;
                                            text = Globals.getDateString(currentContext, currentItem.passTimeEnd, currentZone, false);
                                            break;

                                        case 11:
                                            if(useLocalZone)
                                            {
                                                title = localString + " " + startString;
                                                text = (currentItem.inUnknownPassTimeStartNow() ? res.getString(R.string.title_now) : Globals.getDateString(currentContext, currentItem.passTimeStart, defaultZone, false));
                                            }
                                            break;

                                        case 12:
                                            if(useLocalZone)
                                            {
                                                title = localString + " " + endString;
                                                text = Globals.getDateString(currentContext, currentItem.passTimeEnd, defaultZone, false);
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
                                            text = Globals.getIlluminationString(currentItem.illumination);
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
            }

            @Override
            public int getItemCount()
            {
                return(combinedItems.getCount());
            }

            @Override
            public Item getItem(int position)
            {
                return(combinedItems.getCombinedItem(position));
            }

            @Override
            public long getItemId(int position)
            {
                Item currentItem = combinedItems.getCombinedItem(position);
                return(currentItem != null ? currentItem.id : Integer.MIN_VALUE);
            }
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
            return(1);
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
        {
            int group = this.getGroupParam();
            int page = this.getPageParam();
            int subPage = this.getSubPageParam();
            boolean createLens;
            boolean createMapView;
            View newView = null;
            Context context = this.getContext();
            Combined.Item[] savedItems = (Combined.Item[])Current.PageAdapter.getSavedItems();
            final Selectable.ListBaseAdapter listAdapter = new Combined.ItemListAdapter(context, savedItems, MainActivity.getSatellites());

            //set default
            actionButton = null;
            if(savedInstanceState == null)
            {
                savedInstanceState = new Bundle();
            }

            //set if need to create lens/map view
            createLens = (subPage == Globals.SubPageType.Lens);
            createMapView = (subPage == Globals.SubPageType.Map || subPage == Globals.SubPageType.Globe);

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
                newView = Current.onCreateMapView(this, inflater, container, (MainActivity.mapViewNoradID == Integer.MAX_VALUE ? MainActivity.getSatellites() : new Database.SatelliteData[]{new Database.SatelliteData(context, MainActivity.mapViewNoradID)}), (subPage == Globals.SubPageType.Globe), savedInstanceState);
            }

            //if view is not set yet
            if(newView == null)
            {
                //create view
                newView = this.onCreateView(inflater, container, listAdapter, group, page, false);
            }

            //set change listeners
            setChangeListeners(selectList, listAdapter, page);

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
                PageAdapter.setOrientationChangedListener(new OnOrientationChangedListener()
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
                PageAdapter.setItemChangedListener(new OnItemsChangedListener()
                {
                    @Override @SuppressLint("NotifyDataSetChanged")
                    public void itemsChanged()
                    {
                        FragmentActivity activity = Page.this.getActivity();
                        int sortBy = Settings.getCurrentSortBy();

                        //if not a constant value for sorting
                        if(sortBy != Items.SortBy.Name && sortBy != Items.SortBy.PassStartTime && sortBy != Items.SortBy.PassDuration && sortBy != Items.SortBy.MaxElevation)
                        {
                            //set pending sort
                            PageAdapter.setPendingSort(true);
                        }

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
                PageAdapter.setGraphChangedListener(createOnGraphChangedListener(listAdapter));
                PageAdapter.setPreview3dChangedListener(createOnPreview3dChangedListener(listAdapter));
                PageAdapter.setInformationChangedListener(createOnInformationChangedListener(listAdapter));
            }
        }
    }

    //Page adapter
    public static class PageAdapter extends Selectable.ListFragmentAdapter
    {
        private static boolean pendingSort;
        private static Combined.Item[] combinedItems;
        private static final Items.NoradIndex.Comparer noradIndexComparer = new Items.NoradIndex.Comparer();
        private static ArrayList<Items.NoradIndex> combinedNoradIndex;
        private static Selectable.ListFragment.OnOrientationChangedListener orientationChangedListener = null;
        private static Selectable.ListFragment.OnItemsChangedListener itemsChangedListener = null;
        private static Selectable.ListFragment.OnGraphChangedListener graphChangedListener = null;
        private static Selectable.ListFragment.OnPreview3dChangedListener preview3dChangedListener = null;
        private static Selectable.ListFragment.OnInformationChangedListener informationChangedListener = null;
        private static Object[] savedItems = null;

        public PageAdapter(FragmentManager fm, View parentView, Selectable.ListFragment.OnItemDetailButtonClickListener detailListener, Selectable.ListFragment.OnAdapterSetListener adapterListener, Selectable.ListFragment.OnPageSetListener setListener, int subPg)
        {
            super(fm, parentView, null, null, null, detailListener, adapterListener, setListener, null, MainActivity.Groups.Current, new int[]{subPg});
        }

        @Override
        public @NonNull Fragment getItem(int position)
        {
            return(this.getItem(group, position, subPage[PageType.Combined], new Page()));
        }

        @Override
        public @NonNull Object instantiateItem(@NonNull  ViewGroup container, int position)
        {
            Bundle params;
            Page newPage = (Page)setupItem((Page)super.instantiateItem(container, position));

            //setup page
            newPage.setOnPageSetListener(pageSetListener);

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
            return(1);
        }

        @Override
        public CharSequence getPageTitle(int position)
        {
            Resources res = currentContext.getResources();
            return(res.getString(R.string.title_invalid));
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
        public static void sortItems(int sortBy)
        {
            int index;

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
        }

        //Gets count of items in given page
        public static int getItemCount()
        {
            return(combinedItems == null ? 0 : combinedItems.length);
        }

        //Returns true if given page has items
        public static boolean hasItems()
        {
            return(getItemCount() > 0);
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

        //Set pending
        public static void setPendingSort(boolean pending)
        {
            pendingSort = pending;
        }

        //Returns if pending sort for given page
        public static boolean hasPendingSort()
        {
            return(pendingSort);
        }

        //Get saved items
        public static Object[] getSavedItems()
        {
            return(savedItems);
        }

        //Set saved items
        public void setSavedItems(Object[] saveItems)
        {
            //set saved items
            savedItems = saveItems;
        }

        //Sets orientation changed listener
        public static void setOrientationChangedListener(Selectable.ListFragment.OnOrientationChangedListener listener)
        {
            //set listener
            orientationChangedListener = listener;
        }

        //Sets item changed listener
        public static void setItemChangedListener(Selectable.ListFragment.OnItemsChangedListener listener)
        {
            //set listener
            itemsChangedListener = listener;
        }

        //Sets graph changed listener
        public static void setGraphChangedListener(Selectable.ListFragment.OnGraphChangedListener listener)
        {
            //set listener
            graphChangedListener = listener;
        }

        //Sets preview 3d changed listener
        public static void setPreview3dChangedListener(Selectable.ListFragment.OnPreview3dChangedListener listener)
        {
            //set listener
            preview3dChangedListener = listener;
        }

        //Sets information changed listener
        public static void setInformationChangedListener(Selectable.ListFragment.OnInformationChangedListener listener)
        {
            //set listener
            informationChangedListener = listener;
        }

        //Calls orientation changed listener
        public static void notifyOrientationChangedListener()
        {
            //if listener exists
            if(orientationChangedListener != null)
            {
                //call listener
                orientationChangedListener.orientationChanged();
            }
        }

        //Calls item changed listener
        public static void notifyItemsChanged()
        {
            //if listener exists
            if(itemsChangedListener != null)
            {
                //call listener
                itemsChangedListener.itemsChanged();
            }
        }

        //Call graph changed listener
        public static void notifyGraphChanged(Database.SatelliteData orbital, ArrayList<CalculateViewsTask.OrbitalView> pathPoints)
        {
            //if listener exists
            if(graphChangedListener != null)
            {
                //call listener
                graphChangedListener.graphChanged(orbital, pathPoints, null, null);
            }
        }

        //Call preview 3d changed listener
        public static void notifyPreview3dChanged(int noradId)
        {
            //if listener exists
            if(preview3dChangedListener != null)
            {
                //call listener
                preview3dChangedListener.preview3dChanged(noradId);
            }
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

    public static boolean mapShowPaths = false;
    public static boolean lensShowPaths = false;
    public static boolean lensShowCalibration = false;
    public static long mapMillisecondsPlayBar = 0;
    public static CoordinatesFragment.MarkerBase currentLocationMarker = null;
    public static Calculations.SatelliteObjectType[] orbitalViews = new Calculations.SatelliteObjectType[0];

    private static boolean lensShowHorizon = false;
    private static boolean mapViewReady = false;
    private static boolean mapShowToolbars = true;
    private static boolean mapShowZoom = true;
    private static boolean mapShowDividers = false;
    private static float mapPendingMarkerScale = Float.MAX_VALUE;
    private static FloatingActionStateButtonMenu mapSettingsMenu = null;
    private static CoordinatesFragment.OrbitalBase moonMarker = null;
    private static Calculations.ObserverType currentLocation = new Calculations.ObserverType();
    private static WeakReference<TextView> mapInfoTextReference;
    private static WeakReference<CameraLens> cameraViewReference;
    private static WeakReference<CoordinatesFragment> mapViewReference;
    private static WeakReference<FloatingActionButton> fullscreenButtonReference;

    //Gets camera view
    public static CameraLens getCameraView()
    {
        return(cameraViewReference != null ? cameraViewReference.get() : null);
    }

    //Gets map view
    private static CoordinatesFragment getMapView()
    {
        return(mapViewReference != null ? mapViewReference.get() : null);
    }

    //Gets fullscreen button
    public static FloatingActionButton getFullScreenButton()
    {
        return(fullscreenButtonReference != null ? fullscreenButtonReference.get() : null);
    }

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
        orbitalViews = new Calculations.SatelliteObjectType[satellites.length];
        for(index = 0; index < satellites.length; index++)
        {
            //set view
            orbitalViews[index] = new Calculations.SatelliteObjectType(satellites[index].satellite);
        }
    }

    //Creates lens view
    public static View onCreateLensView(Selectable.ListFragment pageFragment, LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        final Context context = pageFragment.getContext();
        final Resources res = (context != null ? context.getResources() : null);
        Bundle savedState = (savedInstanceState != null ? savedInstanceState : new Bundle());
        ViewGroup rootView = (ViewGroup)inflater.inflate(R.layout.current_lens_view, container, false);
        FrameLayout lensLayout = rootView.findViewById(R.id.Lens_Layout);
        int page = pageFragment.getPageParam();
        int passIndex = savedState.getInt(MainActivity.ParamTypes.PassIndex, 0);
        int pathDivisions = savedState.getInt(MainActivity.ParamTypes.PathDivisions, 8);
        boolean onCalculateView = (page == Calculate.PageType.View);
        boolean onCalculatePasses = (page == Calculate.PageType.Passes);
        boolean onCalculateIntersection = (page == Calculate.PageType.Intersection);
        Calculate.ViewAngles.Item[] savedViewItems = (Calculate.ViewAngles.Item[])Calculate.PageAdapter.getSavedItems(Calculate.PageType.View);
        Calculate.Passes.Item[] savedPassItems = (savedState.getBoolean(MainActivity.ParamTypes.GetPassItems, false) ? (Calculate.Passes.Item[])Calculate.PageAdapter.getSavedItems(onCalculateIntersection ? Calculate.PageType.Intersection : Calculate.PageType.Passes) : null);
        boolean forceShowPaths = savedState.getBoolean(MainActivity.ParamTypes.ForceShowPaths, false);
        boolean useSavedViewPath = (onCalculateView && savedViewItems != null && savedViewItems.length > 0);
        boolean useSavedPassPath = ((onCalculatePasses || onCalculateIntersection) && savedPassItems != null && passIndex < savedPassItems.length && savedPassItems[passIndex].passViews != null && savedPassItems[passIndex].passViews.length > 0);
        final boolean useSaved = (useSavedViewPath || useSavedPassPath);
        final CameraLens cameraView;
        final Calculate.Passes.Item currentSavedPathItem = (useSavedPassPath ? savedPassItems[passIndex] : null);
        final CalculateViewsTask.OrbitalView[] passViews = (useSavedPassPath && currentSavedPathItem != null ? currentSavedPathItem.passViews : null);
        final boolean havePassViews = (passViews != null && passViews.length > 0);
        final Database.SatelliteData currentSatellite = (useSaved ? new Database.SatelliteData(context, (useSavedViewPath ? savedViewItems[0].id : currentSavedPathItem != null ? currentSavedPathItem.id : Universe.IDs.Invalid)) : null);
        final FloatingActionButton fullscreenButton = rootView.findViewById(R.id.Lens_Fullscreen_Button);
        final FloatingActionStateButton showCalibrationButton;
        final FloatingActionStateButton showHorizonButton;
        final FloatingActionStateButton showPathButton;
        final LinearLayout buttonLayout = rootView.findViewById(R.id.Lens_Button_Layout);
        final MaterialButton selectButton = rootView.findViewById(R.id.Lens_Select_Button);
        final MaterialButton resetButton = rootView.findViewById(R.id.Lens_Reset_Button);
        final MaterialButton cancelButton = rootView.findViewById(R.id.Lens_Cancel_Button);

        //create camera
        cameraView = new CameraLens(context);
        cameraViewReference = new WeakReference<>(cameraView);
        fullscreenButtonReference = new WeakReference<>(fullscreenButton);
        cameraView.updateAzDeclination(MainActivity.getObserver());
        cameraView.pathDivisions = pathDivisions;
        cameraView.showPaths = (lensShowPaths || useSaved);       //if showing paths or using a saved path
        lensLayout.addView(cameraView, 0);              //add before menu

        //get help text
        cameraView.helpText = rootView.findViewById(R.id.Lens_Help_Text);

        //create settings menu
        cameraView.settingsMenu = rootView.findViewById(R.id.Lens_Settings_Menu);
        showCalibrationButton = (!useSaved ? cameraView.settingsMenu.addMenuItem(R.drawable.ic_filter_center_focus_black, R.string.title_align) : null);
        showHorizonButton = cameraView.settingsMenu.addMenuItem(R.drawable.ic_remove_black, R.string.title_show_horizon);
        showPathButton = (!useSaved && !forceShowPaths ? cameraView.settingsMenu.addMenuItem(R.drawable.orbit, R.string.title_show_path) : null);

        //setup calibration button
        lensShowCalibration = false;
        if(showCalibrationButton != null)
        {
            showCalibrationButton.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    //reverse state
                    lensShowCalibration = !lensShowCalibration;
                    showCalibrationButton.setChecked(lensShowCalibration);

                    //update display
                    cameraView.showCalibration = lensShowCalibration;
                    if(!lensShowCalibration)
                    {
                        cameraView.resetAlignmentStatus();
                    }
                    buttonLayout.setVisibility(lensShowCalibration ? View.VISIBLE : View.GONE);
                    selectButton.setText(R.string.title_select);
                    resetButton.setVisibility(View.VISIBLE);
                    cameraView.helpText.setVisibility(lensShowCalibration || cameraView.hasCompassError() ? View.VISIBLE : View.GONE);
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
                    if(!lensShowCalibration && cameraView.hasCompassError())
                    {
                        //set text for compass error
                        cameraView.helpText.setText(R.string.title_compass_inaccurate_fix);
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
        lensShowHorizon = Settings.getLensShowHorizon(context);
        showHorizonButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //reverse state
                lensShowHorizon = !lensShowHorizon;
                showHorizonButton.setChecked(lensShowHorizon);
                Settings.setLensShowHorizon(context, lensShowHorizon);

                //update display
                cameraView.showHorizon = lensShowHorizon;
            }
        });

        //set to opposite then perform click to set it correctly
        lensShowHorizon = !lensShowHorizon;
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
            Calendar startTime = (useSavedViewPath ? savedViewItems[0].time : havePassViews ? passViews[0].gmtTime : null);
            Calendar endTime = (useSavedViewPath ? savedViewItems[savedViewItems.length - 1].time : havePassViews ? passViews[passViews.length - 1].gmtTime : null);
            header.setTag(Globals.getHeaderText(context, (onCalculateIntersection && currentSavedPathItem != null ? currentSavedPathItem.name : currentSatellite.getName()), startTime, endTime));
            Calculate.setOrientationHeaderText(header);
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
            else if(havePassViews)
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
                        lensShowPaths = !lensShowPaths;
                        showPathButton.setChecked(lensShowPaths);

                        //update display
                        cameraView.showPaths = lensShowPaths;
                    }
                });
                showPathButton.setChecked(lensShowPaths);

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
            CoordinatesFragment mapView = getMapViewIfReady();
            Calculations.ObserverType observer = (Calculations.ObserverType)params[0];

            //if map is ready
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
                    Calculations.SatelliteObjectType pathSatellite = new Calculations.SatelliteObjectType(currentData != null ? currentData.satellite : null);
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
                        Calculations.updateOrbitalPosition(pathSatellite, observer, pathJulianDate, true);

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
    public static CalculatePathTask calculateCoordinates(Calculations.ObserverType observer, double julianDate, CalculatePathTask.OnProgressChangedListener listener)
    {
        CalculatePathTask task;

        //start calculating
        task = new CalculatePathTask(listener);
        task.execute(observer, julianDate);

        //return task
        return(task);
    }

    //Select orbital with the given ID
    private static void selectOrbital(int noradId)
    {
        boolean isNone = (noradId == Universe.IDs.None);
        boolean isLocation = (noradId == Universe.IDs.CurrentLocation);
        CoordinatesFragment mapView = getMapView();

        //if map view exists
        if(mapView != null)
        {
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
    }

    //setup search
    private static void setupSearch(final Context context, final FloatingActionStateButton showToolbarsButton, final IconSpinner searchList, final View searchListLayout, final PlayBar pagePlayBar, Database.SatelliteData[] selectedOrbitals)
    {
        boolean usingSearchList = (searchList != null);
        int textColor = Globals.resolveColorID(context, R.attr.defaultTextColor);
        int textSelectedColor = Globals.resolveColorID(context, R.attr.columnTitleTextColor);
        ArrayList<Database.DatabaseSatellite> selectedOrbitalList = new ArrayList<>(0);

        //setup selection list
        if(selectedOrbitals != null)
        {
            //add none and location
            selectedOrbitalList.add(new Database.DatabaseSatellite(context.getResources().getString(R.string.title_none), Universe.IDs.None, null, Globals.UNKNOWN_DATE_MS, Database.OrbitalType.Planet));
            selectedOrbitalList.add(new Database.DatabaseSatellite(context.getResources().getString(R.string.title_location_current), Universe.IDs.CurrentLocation, null, Globals.UNKNOWN_DATE_MS, Database.OrbitalType.Planet));

            //go through each selected orbital
            for(Database.SatelliteData currentData : selectedOrbitals)
            {
                //add current orbital to list
                selectedOrbitalList.add(currentData.database);
            }
        }

        //setup search list
        if(usingSearchList)
        {
            searchList.setAdapter(new IconSpinner.CustomAdapter(context, selectedOrbitalList.toArray(new Database.DatabaseSatellite[0]), false, textColor, textSelectedColor, textColor, textSelectedColor, (Settings.getDarkTheme(context) ? R.color.white : R.color.black), null));
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
                    //if list being shown
                    if(mapShowToolbars)
                    {
                        //update selection
                        selectOrbital((int)searchList.getSelectedValue(Universe.IDs.None));
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
            searchList.setSelectedValue(Universe.IDs.None);
        }

        //if show toolbars button exists
        if(showToolbarsButton != null)
        {
            //setup toolbars button
            showToolbarsButton.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    CoordinatesFragment mapView = getMapView();

                    //reverse state
                    mapShowToolbars = !mapShowToolbars;
                    showToolbarsButton.setChecked(mapShowToolbars);
                    Settings.setMapShowToolbars(context, mapShowToolbars);

                    //update visibility
                    if(searchListLayout != null)
                    {
                        searchListLayout.setVisibility(mapShowToolbars ? View.VISIBLE : View.GONE);
                    }
                    if(pagePlayBar != null)
                    {
                        pagePlayBar.setVisibility(mapShowToolbars ? View.VISIBLE : View.GONE);
                    }

                    //if search list and map exists and showing toolbars
                    if(searchList != null && mapView != null && mapShowToolbars)
                    {
                        //update selection
                        searchList.setSelectedValue(mapView.getSelectedNoradId());
                    }
                }
            });

            //set to opposite for later click called
            mapShowToolbars = !(Settings.getMapShowToolbars(context));
        }
    }

    //Gets map information text
    public static TextView getMapInfoText()
    {
        return(mapInfoTextReference != null ? mapInfoTextReference.get() : null);
    }

    //Gets map view if it is ready to use
    public static CoordinatesFragment getMapViewIfReady()
    {
        return(mapViewReady ? getMapView() : null);
    }

    //Handles any pending marker scale
    public static void handleMarkerScale()
    {
        CoordinatesFragment mapView = getMapViewIfReady();

        //if map view is ready and there is a pending marker scale
        if(mapView != null && mapPendingMarkerScale != Float.MAX_VALUE)
        {
            //set scale and update status
            mapView.setMarkerScale(mapPendingMarkerScale);
            mapPendingMarkerScale = Float.MAX_VALUE;
        }
    }

    //Setup zoom buttons
    private static void setupZoomButtons(final Context context, final FloatingActionStateButton showZoomButton, FloatingActionButton zoomInButton, FloatingActionButton zoomOutButton)
    {
        CoordinatesFragment mapView = getMapView();

        //setup zoom buttons
        showZoomButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                int buttonVisibility;

                //reverse state
                mapShowZoom = !mapShowZoom;
                showZoomButton.setChecked(mapShowZoom);
                Settings.setMapShowZoom(context, mapShowZoom);

                //update visibility
                buttonVisibility = (mapShowZoom ? View.VISIBLE : View.GONE);
                zoomInButton.setVisibility(buttonVisibility);
                zoomOutButton.setVisibility(buttonVisibility);
            }
        });
        CoordinatesFragment.Utils.setupZoomButton(context, mapView, zoomInButton, true);
        CoordinatesFragment.Utils.setupZoomButton(context, mapView, zoomOutButton, false);

        //set to opposite then perform click to set it correctly
        mapShowZoom = !Settings.getMapShowZoom(context);
        showZoomButton.performClick();

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
                CoordinatesFragment mapView = getMapView();

                //reverse state
                mapShowDividers = !mapShowDividers;
                showLatLonButton.setChecked(mapShowDividers);
                Settings.setMapShowGrid(context, mapShowDividers);

                //if map view exists
                if(mapView != null)
                {
                    //update visibility
                    mapView.setLatitudeLongitudeGridEnabled(mapShowDividers);
                }
            }
        });

        //set to opposite then perform click to set it correctly
        mapShowDividers = !Settings.getMapShowGrid(context);
        showLatLonButton.performClick();
    }

    //Setup footprint button
    private static void setupFootprintButton(Context context, final FloatingActionStateButton showFootprintButton)
    {
        //if button exists
        if(showFootprintButton != null)
        {
            showFootprintButton.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    //reverse state
                    boolean showFootprints = !Settings.usingMapShowFootprint();
                    showFootprintButton.setChecked(showFootprints);
                    Settings.setMapShowFootprint(context, showFootprints);
                }
            });

            //set to opposite then perform click to set it correctly
            Settings.setMapShowFootprint(context, !Settings.usingMapShowFootprint());
            showFootprintButton.performClick();
        }
    }

    //Setup path button
    private static void setupPathButton(final FloatingActionStateButton showPathButton, Database.SatelliteData[] selectedOrbitals)
    {
        final int selectedCount = (selectedOrbitals != null ? selectedOrbitals.length : 0);
        final Database.SatelliteData firstOrbital = (selectedCount > 0 ? selectedOrbitals[0] : null);

        showPathButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                int index;
                CoordinatesFragment mapView = getMapView();
                int orbitalCount = (mapView != null ? mapView.getOrbitalCount() : 0);

                //reverse state
                mapShowPaths = !mapShowPaths;
                showPathButton.setChecked(mapShowPaths);

                //update visibility
                for(index = 0; index < orbitalCount; index++)
                {
                    mapView.setPathVisible(index, (mapShowPaths && ((selectedCount > 1) || (firstOrbital != null && firstOrbital.getSatelliteNum() == mapView.getOrbitalNoradId(index)))));
                }
            }
        });

        //set to opposite then perform click to set it correctly
        mapShowPaths = !mapShowPaths;
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
                    mapPendingMarkerScale = Settings.getMapMarkerScale(context);
                }

                //toggle scale bar visibility
                scaleBar.setVisibility(setScaleVisible ? View.VISIBLE : View.GONE);

                //close menu
                mapSettingsMenu.close();
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
                //if map is ready
                if(getMapViewIfReady() != null)
                {
                    //update scale
                    mapPendingMarkerScale = progressValue / 100f;

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
                //if map is ready
                if(getMapViewIfReady() != null)
                {
                    //undo any changes
                    mapPendingMarkerScale = Settings.getMapMarkerScale(context);
                }
                scaleBar.setVisibility(View.GONE);
            }
        });
    }

    //Setup play bar
    private static void setupPlayBar(final FragmentActivity activity, PlayBar playBar, final Calculate.Coordinates.Item[] playbackItems, final CoordinatesFragment.OrbitalBase[] playbackMarkers)
    {
        CoordinatesFragment mapView = getMapViewIfReady();
        final boolean usingPlaybackItems = (playbackItems != null);
        final boolean usingMarkers = (playbackMarkers != null);
        final boolean singlePlaybackMarker = (usingMarkers && playbackMarkers.length == 1);
        final int min = 0;
        final int max = (usingPlaybackItems ? playbackItems.length : (int)(Calculations.SecondsPerDay * 1000)) - 1;
        final int scaleType = (usingPlaybackItems ? PlayBar.ScaleType.Speed : PlayBar.ScaleType.Time);
        int mapTimerDelay;
        int mapSelectedNoradId = (mapView != null ? mapView.getSelectedNoradId() : Universe.IDs.Invalid);

        //if play bar exists
        if(playBar != null)
        {
            //get timer delay
            mapTimerDelay = Settings.getMapUpdateDelay(playBar.getContext());

            //setup play bar
            playBar.setMin(min);
            playBar.setMax(max);
            playBar.setPlayPeriod(mapTimerDelay);
            playBar.setPlayScaleType(scaleType);
            playBar.setPlayActivity(activity);
            playBar.setValueTextVisible(true);
            playBar.setTimeZone(MainActivity.getTimeZone());
            playBar.setOnSeekChangedListener(new PlayBar.OnPlayBarChangedListener()
            {
                @Override
                public void onProgressChanged(PlayBar seekBar, int progressValue, double subProgressPercent, boolean fromUser)
                {
                    //if a valid value
                    if(progressValue >= min && progressValue <= max)
                    {
                        String timeString;

                        //if using playback items and markers exist
                        if(usingPlaybackItems && usingMarkers)
                        {
                            int index;
                            int currentItemIndex = (int)Math.floor(progressValue + subProgressPercent);
                            double latitude;
                            double longitude;
                            double altitudeKm;
                            Calculate.Coordinates.Item nextItem = (currentItemIndex + 1 < playbackItems.length ? playbackItems[currentItemIndex + 1] : null);
                            Calculate.Coordinates.Item currentItem = playbackItems[currentItemIndex];
                            TextView mapInfoText = getMapInfoText();
                            Calendar playTime = Globals.getGMTTime(currentItem.time);

                            //if more points after current
                            if(nextItem != null)
                            {
                                //add distance percentage to play time
                                playTime.add(Calendar.MILLISECOND, (int)((nextItem.time.getTimeInMillis() - currentItem.time.getTimeInMillis()) * subProgressPercent));
                            }

                            //get time string
                            timeString = Globals.getDateTimeString(playTime, true);

                            //go through each marker
                            for(index = 0; index < playbackMarkers.length; index++)
                            {
                                //remember current marker
                                CoordinatesFragment.OrbitalBase currentMarker = playbackMarkers[index];

                                //if more points after current
                                if(nextItem != null)
                                {
                                    //get add distance index percentage to current point
                                    latitude = Globals.normalizeLatitude(currentItem.coordinates[index].latitude + (Globals.latitudeDistance(currentItem.coordinates[index].latitude, nextItem.coordinates[index].latitude) * subProgressPercent));
                                    longitude = Globals.normalizeLongitude(currentItem.coordinates[index].longitude + (Globals.longitudeDistance(currentItem.coordinates[index].longitude, nextItem.coordinates[index].longitude) * subProgressPercent));
                                    altitudeKm = currentItem.coordinates[index].altitudeKm + ((nextItem.coordinates[index].altitudeKm - currentItem.coordinates[index].altitudeKm) * subProgressPercent);
                                }
                                else
                                {
                                    //set to current point
                                    latitude = currentItem.coordinates[index].latitude;
                                    longitude = currentItem.coordinates[index].longitude;
                                    altitudeKm = currentItem.coordinates[index].altitudeKm;
                                }

                                //update marker
                                if(currentMarker != null)
                                {
                                    Database.SatelliteData currentOrbital = currentMarker.getData();
                                    int currentNoradId = currentOrbital.getSatelliteNum();
                                    boolean currentIsSatellite = (currentNoradId > 0);
                                    boolean currentOrbitalSelected = (singlePlaybackMarker || currentNoradId == mapSelectedNoradId);

                                    //update showing selected footprint
                                    currentMarker.setShowSelectedFootprint(currentIsSatellite && currentOrbitalSelected && Settings.usingMapFootprintAndSelected());

                                    //if marker is visible
                                    if(currentMarker.getInfoVisible())
                                    {
                                        String coordinateString = Globals.getCoordinateString(activity, latitude, longitude, altitudeKm);

                                        //update coordinates
                                        currentMarker.setText(coordinateString);
                                        if(mapInfoText != null && Settings.usingMapMarkerInfoBottom())
                                        {
                                            mapInfoText.setText(coordinateString.replace("\n", Globals.COORDINATE_SEPARATOR));
                                            mapInfoText.setVisibility(View.VISIBLE);
                                        }

                                        //if map is ready
                                        if(mapView != null)
                                        {
                                            //update map view
                                            mapView.moveCamera(latitude, longitude);
                                        }
                                    }
                                    else
                                    {
                                        //clear old text
                                        currentMarker.setText(null);
                                    }

                                    //move marker location and update status
                                    currentMarker.setShowFootprint(currentIsSatellite && !currentOrbitalSelected && Settings.usingMapShowFootprint());
                                    currentMarker.moveLocation(latitude, longitude, altitudeKm);
                                    currentMarker.setVisible(true);
                                }
                            }
                        }
                        else
                        {
                            //set time in seconds
                            mapMillisecondsPlayBar = progressValue + playBar.getValue2();
                            timeString = Globals.getDateTimeString(Globals.getGMTTime(mapMillisecondsPlayBar), true);
                        }

                        //update time
                        playBar.setValueText(timeString);

                        //handle any needed update
                        handleMarkerScale();
                    }
                }
            });
            if(usingPlaybackItems)
            {
                //go to first point
                playBar.setValue(0, true);

                //if at least 2 saved items
                if(playbackItems.length > 1)
                {
                    //set speed increment to time between 2 points
                    playBar.setPlayIndexIncrementUnits((playbackItems[1].time.getTimeInMillis() - playbackItems[0].time.getTimeInMillis()) / (double)mapTimerDelay);
                }
            }
            else
            {
                //set sync button listener
                playBar.setSyncButtonListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        int timeMilliseconds;
                        long dateMilliseconds;
                        Calendar currentTime;

                        //get local time
                        currentTime = Globals.getLocalTime(Globals.getGMTTime(), MainActivity.getTimeZone());
                        if(currentTime == null)
                        {
                            currentTime = Calendar.getInstance();
                        }

                        //set to current time/date
                        timeMilliseconds = (int)(((currentTime.get(Calendar.HOUR_OF_DAY) * Calculations.SecondsPerHour) + (currentTime.get(Calendar.MINUTE) * 60) + currentTime.get(Calendar.SECOND)) * 1000) + currentTime.get(Calendar.MILLISECOND);
                        dateMilliseconds = Globals.clearCalendarTime(currentTime).getTimeInMillis();
                        playBar.setValues(timeMilliseconds, dateMilliseconds);
                    }
                });

                //set time increment and begin
                playBar.setPlayIndexIncrementUnits(1);
                playBar.start(true);
            }
            playBar.setVisibility(usingPlaybackItems || Settings.getMapShowToolbars(playBar.getContext()) ? View.VISIBLE : View.GONE);
        }
    }

    //Sets current location
    public static void setCurrentLocation(final Activity activity, Calculations.ObserverType location)
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

    //Adds given orbital and returns markers
    private static CoordinatesFragment.OrbitalBase[] addOrbitals(Context context, Database.SatelliteData[] orbitals, Calculate.Coordinates.Item[] savedItems)
    {
        int index;
        int index2;
        CoordinatesFragment mapView = getMapView();
        CoordinatesFragment.OrbitalBase[] markers;

        //if map and orbitals are set
        if(mapView != null && orbitals != null && orbitals.length > 0)
        {
            //remove all old markers/orbitals
            mapView.removeOrbitals();

            //initialize markers
            markers = new CoordinatesFragment.OrbitalBase[orbitals.length];

            //go through each orbital
            for(index = 0; index < orbitals.length; index++)
            {
                //add orbital
                Database.SatelliteData currentOrbital = orbitals[index];
                CoordinatesFragment.OrbitalBase newOrbital = mapView.addOrbital(context, currentOrbital, currentLocation);

                //remember marker
                markers[index] = newOrbital;

                //if the moon
                if(newOrbital.getData().getSatelliteNum() == Universe.IDs.Moon)
                {
                    //remember it
                    moonMarker = newOrbital;
                }
            }

            //if there are saved items
            if(savedItems != null && savedItems.length > 0)
            {
                //go through each  orbital
                for(index = 0; index < savedItems[0].coordinates.length && index < markers.length; index++)
                {
                    ArrayList<CoordinatesFragment.Coordinate> points = new ArrayList<>(0);

                    //go through each coordinate
                    for(index2 = 0; index2 < savedItems.length; index2++)
                    {
                        //get point and add to the list
                        Calculate.Coordinates.Item currentItem = savedItems[index2];
                        CoordinatesFragment.Coordinate currentPoint = new CoordinatesFragment.Coordinate(currentItem.coordinates[index].latitude, currentItem.coordinates[index].longitude, currentItem.coordinates[index].altitudeKm);
                        points.add(currentPoint);
                    }

                    //set points
                    markers[index].setPath(points);
                }
            }

            //return markers
            return(markers);
        }

        //invalid
        return(null);
    }

    //Creates a map view
    public static View onCreateMapView(final Selectable.ListFragment page, LayoutInflater inflater, ViewGroup container, Database.SatelliteData[] selectedOrbitals, boolean forGlobe, final Bundle savedInstanceState)
    {
        //update status
        mapViewReady = false;

        //clear time
        mapMillisecondsPlayBar = 0;

        //get context, main views, and lists
        final Context context = page.getContext();
        final ViewGroup rootView = (ViewGroup)inflater.inflate(R.layout.current_map_layout, container, false);
        final FrameLayout mapFrameLayout = rootView.findViewById(R.id.Map_Frame_Layout);
        final LinearLayout searchListLayout = rootView.findViewById(R.id.Map_Search_List_Layout);
        final IconSpinner searchList;

        //setup lists and status
        final Calculate.Coordinates.Item[] savedItems = (savedInstanceState != null ? (Calculate.Coordinates.Item[]) Calculate.PageAdapter.getSavedItems(Calculate.PageType.Coordinates) : null);
        final boolean haveSelected = (selectedOrbitals != null && selectedOrbitals.length > 0);
        final boolean multiSelected = (haveSelected && selectedOrbitals.length > 1);
        final boolean useSavedPath = (page instanceof Calculate.Page && savedItems != null && savedItems.length > 0);
        final boolean useMultiNoradId = (useSavedPath && savedItems[0].coordinates.length > 1);
        final boolean rotateAllowed = Settings.getMapRotateAllowed(context);
        final Database.SatelliteData currentSatellite = (useSavedPath && haveSelected && !useMultiNoradId ? selectedOrbitals[0] : null);

        //get menu and zoom displays
        final LinearLayout floatingButtonLayout = rootView.findViewById(R.id.Map_Floating_Button_Layout);
        final ImageView compassImage = rootView.findViewById(R.id.Map_Compass_Image);
        mapSettingsMenu = mapFrameLayout.findViewById(R.id.Map_Settings_Menu);
        final FloatingActionButton fullscreenButton = rootView.findViewById(R.id.Map_Fullscreen_Button);
        final FloatingActionStateButton showToolbarsButton = (multiSelected ? mapSettingsMenu.addMenuItem(R.drawable.ic_search_black, R.string.title_show_toolbars) : null);
        final FloatingActionStateButton showZoomButton = mapSettingsMenu.addMenuItem(R.drawable.ic_unfold_more_white, R.string.title_show_zoom);
        final FloatingActionStateButton showLatLonButton = mapSettingsMenu.addMenuItem(R.drawable.ic_language_black, R.string.title_show_latitude_longitude);
        final FloatingActionStateButton showFootprintButton = mapSettingsMenu.addMenuItem(R.drawable.ic_contrast_white, R.string.title_show_footprint);
        final FloatingActionStateButton showPathButton = mapSettingsMenu.addMenuItem(R.drawable.orbit, R.string.title_show_path);
        final FloatingActionStateButton iconScaleButton = mapSettingsMenu.addMenuItem(R.drawable.ic_width_black, R.string.title_set_icon_scale);
        final FloatingActionButton zoomInButton = floatingButtonLayout.findViewById(R.id.Map_Zoom_In_Button);
        final FloatingActionButton zoomOutButton = floatingButtonLayout.findViewById(R.id.Map_Zoom_Out_Button);
        mapSettingsMenu.setVisibility(View.GONE);

        //get displays
        Bundle args = new Bundle();
        final TextView mapInfoText = rootView.findViewById(R.id.Map_Coordinate_Info_Text);
        final CoordinatesFragment mapView = (forGlobe ? new Whirly.GlobeFragment() : new Whirly.MapFragment());
        mapViewReference = new WeakReference<>(mapView);
        mapInfoTextReference = new WeakReference<>(mapInfoText);
        Current.fullscreenButtonReference = new WeakReference<>(fullscreenButton);
        args.putInt(Whirly.ParamTypes.MapLayerType, Settings.getMapLayerType(context, forGlobe));
        mapView.setArguments(args);
        searchList = (multiSelected ? (IconSpinner)rootView.findViewById(R.id.Map_Search_List) : null);
        page.playBar = rootView.findViewById(R.id.Map_Coordinate_Play_Bar);
        page.scaleBar = rootView.findViewById(R.id.Map_Coordinate_Scale_Bar);
        page.getChildFragmentManager().beginTransaction().replace(R.id.Map_View, (Fragment)mapView).commit();

        //if search list layout exists
        if(searchListLayout != null)
        {
            //hide until map is ready
            searchListLayout.setVisibility(View.GONE);
        }

        //setup search displays
        setupSearch(context, showToolbarsButton, searchList, searchListLayout, page.playBar, (!useSavedPath || useMultiNoradId ? selectedOrbitals : null));

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
            TextView header = (TextView)Globals.replaceView(R.id.Map_Header, R.layout.header_text_view, inflater, rootView);
            header.setTag(Globals.getHeaderText(context, (currentSatellite != null ? currentSatellite.getName() : null), savedItems[0].time, savedItems[savedItems.length - 1].time));
            Calculate.setOrientationHeaderText(header);
        }

        //setup map view
        mapView.setOnReadyListener(new CoordinatesFragment.OnReadyListener()
        {
            @Override
            public void ready()
            {
                double firstZoom;
                CalculateCoordinatesTask.CoordinateData firstPoint;
                CoordinatesFragment.OrbitalBase[] playbackMarkers = null;

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
                            mapSettingsMenu.close();

                            //if multiple orbitals
                            if(multiSelected)
                            {
                                //deselect current
                                mapView.deselectCurrent();
                            }
                        }
                    }
                });

                //setup markers
                mapPendingMarkerScale = Settings.getMapMarkerScale(context);
                mapView.setMarkerShowBackground(Settings.getMapMarkerShowBackground(context));
                mapView.setMarkerShowShadow(Settings.getMapMarkerShowShadow(context));
                mapView.setInfoLocation(Settings.getMapMarkerInfoLocation(context));
                mapView.setShowTitlesAlways(Settings.getMapShowLabelAlways(context));

                //if current location marker exists
                if(currentLocationMarker != null)
                {
                    //remove it
                    currentLocationMarker.remove();
                }

                //add current location and set as first point
                currentLocationMarker = mapView.addMarker(context, Universe.IDs.CurrentLocation, currentLocation);
                if(context != null)
                {
                    currentLocationMarker.setTitle(context.getString(R.string.title_location_current));
                }
                firstPoint = new CalculateCoordinatesTask.CoordinateData();
                firstPoint.latitude = (float)currentLocation.geo.latitude;
                firstPoint.longitude = (float)currentLocation.geo.longitude;
                firstPoint.altitudeKm = (float)currentLocation.geo.altitudeKm;
                firstZoom = CoordinatesFragment.DefaultNearZoom;

                //setup buttons
                setupZoomButtons(context, showZoomButton, zoomInButton, zoomOutButton);
                setupLatLonButton(context, showLatLonButton);
                setupFootprintButton(context, showFootprintButton);
                setupIconScaleButton(context, iconScaleButton, page.scaleBar);

                //if not using saved path
                if(!useSavedPath)
                {
                    //add all orbitals
                    addOrbitals(context, MainActivity.getSatellites(), null);
                }
                else
                {
                    //add selected orbitals and get markers
                    playbackMarkers = addOrbitals(context, selectedOrbitals, savedItems);

                    //if have only 1 marker
                    if(playbackMarkers != null && playbackMarkers.length == 1)
                    {
                        //remember marker and first point/zoom
                        CoordinatesFragment.OrbitalBase playbackMarker = playbackMarkers[0];
                        firstPoint = savedItems[0].coordinates[0];
                        firstZoom = CoordinatesFragment.Utils.getZoom(firstPoint.altitudeKm);

                        //show info and selected footprint
                        playbackMarker.setInfoVisible(true);
                        playbackMarker.setShowSelectedFootprint((firstPoint.noradId > 0) && Settings.usingMapFootprintAndSelected());
                    }
                }

                //select current location
                if(searchList != null)
                {
                    searchList.setSelectedValue(Universe.IDs.CurrentLocation);
                }

                //send selection changes
                mapView.setOnItemSelectionChangedListener(new CoordinatesFragment.OnItemSelectionChangedListener()
                {
                    private int lastNoradId = Universe.IDs.Invalid;

                    @Override
                    public void itemSelected(int noradId)
                    {
                        //if not a recursive call
                        if(noradId != lastNoradId)
                        {
                            //update last norad ID
                            lastNoradId = noradId;

                            //if list is set
                            if(searchList != null)
                            {
                                //if list being shown
                                if(mapShowToolbars)
                                {
                                    //update selection
                                    searchList.setSelectedValue(noradId);
                                }
                                else
                                {
                                    //update selection
                                    selectOrbital(noradId);
                                }
                            }
                            //else if norad ID changing and exists
                            else if(noradId != Universe.IDs.None && noradId != mapView.getSelectedNoradId() && mapView.getSelectedNoradId() != -1)
                            {
                                mapView.selectOrbital(noradId);
                            }
                        }
                    }
                });

                //setup path button after markers have been added
                setupPathButton(showPathButton, selectedOrbitals);
                if(page instanceof Current.Page)
                {
                    ((Current.Page)page).actionButton = showPathButton;
                }

                //set current location
                setCurrentLocation(page.getActivity(), currentLocation);

                //move to first point
                mapView.moveCamera(firstPoint.latitude, firstPoint.longitude, firstZoom);

                //setup play bar
                setupPlayBar(page.getActivity(), page.playBar, (useSavedPath ? savedItems : null), playbackMarkers);

                //ready to show displays and use
                if(showToolbarsButton != null)
                {
                    //toggle displays
                    showToolbarsButton.performClick();
                }
                mapSettingsMenu.setVisibility(View.VISIBLE);
                mapViewReady = true;
            }
        });

        return(rootView);
    }

    //Returns if closed map settings menu
    public static boolean closeMapSettingsMenu()
    {
        return(mapSettingsMenu != null && mapSettingsMenu.close());
    }
}
