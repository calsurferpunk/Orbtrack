package com.nikolaiapps.orbtrack;


import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;


public abstract class Calculate
{
    public static abstract class PageType
    {
        static final int View = 0;
        static final int Passes = 1;
        static final int Coordinates = 2;
        static final int Intersection = 3;
        static final int PageCount = 4;
    }

    public static abstract class SubPageType
    {
        static final int Input = 0;
        static final int List = 1;
        static final int Lens = 2;
        static final int Map = 3;
        static final int Globe = 4;
    }

    public static abstract class ParamTypes
    {
        static final String NoradId = "id";
        static final String NoradId2 = "id2";
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

    public interface OnStartCalculationListener
    {
        void onStartCalculation(Bundle params);
    }

    public interface OnPageSetListener
    {
        void onPageSet(Page page, int pageNum, int subPageNum);
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
        public IconSpinner orbitalList;
        public IconSpinner orbital2List;
        public IconSpinner viewUnitList;
        public DateInputView startDateText;
        public DateInputView endDateText;
        public TimeInputView startTimeText;
        public TimeInputView endTimeText;
        private OnStartCalculationListener startCalculationListener;
        private OnPageSetListener pageSetListener;

        @Override
        protected int getListColumns(Context context, int page)
        {
            Selectable.ListBaseAdapter listAdapter = getAdapter();
            //int orientation = Globals.getScreenOrientation(context);
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
            //String itemsParamName;
            Context context = this.getContext();
            Selectable.ListBaseAdapter listAdapter;

            if(savedInstanceState == null)
            {
                savedInstanceState = new Bundle();
            }
            if(params == null)
            {
                params = new Bundle();
            }
            savedInstanceState.putInt(Selectable.ParamTypes.PageNumber, page);
            savedInstanceState.putInt(ParamTypes.NoradId, params.getInt(ParamTypes.NoradId));
            savedInstanceState.putInt(ParamTypes.NoradId2, params.getInt(ParamTypes.NoradId2, Universe.IDs.Invalid));
            savedInstanceState.putLong(ParamTypes.StartDateMs, params.getLong(ParamTypes.StartDateMs));
            savedInstanceState.putLong(ParamTypes.EndDateMs, params.getLong(ParamTypes.EndDateMs));
            savedInstanceState.putDouble(ParamTypes.ElevationMinDegs, params.getDouble(ParamTypes.ElevationMinDegs, 0.0));
            savedInstanceState.putDouble(ParamTypes.IntersectionDegs, params.getDouble(ParamTypes.IntersectionDegs, 0.2));

            switch(page)
            {
                case PageType.View:
                    switch(subPage)
                    {
                        case SubPageType.Input:
                            savedInstanceState.putInt(ParamTypes.IncrementUnit, params.getInt(ParamTypes.IncrementUnit));
                            savedInstanceState.putInt(ParamTypes.IncrementType, params.getInt(ParamTypes.IncrementType));
                            break;

                        case SubPageType.List:
                        case SubPageType.Lens:
                            Current.ViewAngles.Item[] savedItems = (Current.ViewAngles.Item[])Calculate.PageAdapter.getSavedItems(page);

                            switch(subPage)
                            {
                                case SubPageType.List:
                                    listAdapter = new Current.ViewAngles.ItemListAdapter(context, savedItems);
                                    setChangeListeners(listAdapter, page);
                                    newView = this.onCreateView(inflater, container, listAdapter, group, page);
                                    break;

                                case SubPageType.Lens:
                                    newView = Current.onCreateLensView(this, inflater, container, savedInstanceState);
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
                        case SubPageType.List:
                        case SubPageType.Lens:
                            Current.Passes.Item[] savedItems = (Current.Passes.Item[])Calculate.PageAdapter.getSavedItems(page);

                            switch(subPage)
                            {
                                case SubPageType.List:
                                    listAdapter = new Current.Passes.ItemListAdapter(context, page, savedItems, null, MainActivity.getTimeZone(), true);
                                    setChangeListeners(listAdapter, page);
                                    newView = this.onCreateView(inflater, container, listAdapter, group, page);
                                    break;

                                case SubPageType.Lens:
                                    savedInstanceState.putInt(MainActivity.ParamTypes.PathDivisions, 8);
                                    savedInstanceState.putInt(MainActivity.ParamTypes.PassIndex, params.getInt(MainActivity.ParamTypes.PassIndex, 0));
                                    savedInstanceState.putBoolean(MainActivity.ParamTypes.GetPassItems, true);
                                    newView = Current.onCreateLensView(this, inflater, container, savedInstanceState);
                                    break;
                            }
                            break;
                    }
                    break;

                case PageType.Coordinates:
                    switch(subPage)
                    {
                        case SubPageType.Input:
                            savedInstanceState.putInt(ParamTypes.IncrementUnit, params.getInt(ParamTypes.IncrementUnit));
                            savedInstanceState.putInt(ParamTypes.IncrementType, params.getInt(ParamTypes.IncrementType));
                            break;

                        case SubPageType.List:
                        case SubPageType.Map:
                        case SubPageType.Globe:
                            Current.Coordinates.Item[] savedItems = (Current.Coordinates.Item[])Calculate.PageAdapter.getSavedItems(page);

                            switch(subPage)
                            {
                                case SubPageType.List:
                                    listAdapter = new Current.Coordinates.ItemListAdapter(context, savedItems, MainActivity.getTimeZone());
                                    setChangeListeners(listAdapter, page);
                                    newView = this.onCreateView(inflater, container, listAdapter, group, page);
                                    break;

                                case SubPageType.Map:
                                case SubPageType.Globe:
                                    newView = Current.Coordinates.onCreateMapView(this, inflater, container, (subPage == SubPageType.Globe), savedInstanceState);
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
        protected void onUpdateStarted() { }

        @Override
        protected void onUpdateFinished(boolean success) { }

        //Gets input values
        public Bundle getInputValues()
        {
            int unitValue;
            int pageNumber = this.getPageParam();
            boolean validInputs = true;
            double daysBetween;
            double elMin;
            double intersection;
            String unitType;
            Context context = this.getContext();
            Calendar startTime = Calendar.getInstance();
            Calendar endTime = Calendar.getInstance();
            Bundle pageParams = new Bundle();
            Resources res = (context != null ? context.getResources() : null);

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
                    //get units
                    unitType = viewUnitList.getSelectedValue("").toString();
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
                    if(intersection == Double.MAX_VALUE || intersection <= 0 || intersection > 300)
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
                    if(elMin == Double.MAX_VALUE || elMin < -90 || elMin > 90)
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
                pageParams.putInt(Selectable.ParamTypes.PageNumber, pageNumber);
                pageParams.putInt(Selectable.ParamTypes.SubPageNumber, SubPageType.List);
                pageParams.putInt(ParamTypes.NoradId, (int)orbitalList.getSelectedValue(Universe.IDs.Invalid));
                pageParams.putInt(ParamTypes.NoradId2, (pageNumber == PageType.Intersection ? (int)orbital2List.getSelectedValue(Universe.IDs.Invalid) : Universe.IDs.Invalid));
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

        public void setOnPageSetListener(OnPageSetListener listener)
        {
            pageSetListener = listener;
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

                    if(listColumns != null)
                    {
                        Page.this.setListColumns(Page.this.getContext(), listColumns, page);
                    }
                }
            });
            PageAdapter.setItemChangedListener(page, new OnItemsChangedListener()
            {
                @Override
                public void itemsChanged()
                {
                    //update displays
                    if(listAdapter instanceof Current.ViewAngles.ItemListAdapter)
                    {
                        ((Current.ViewAngles.ItemListAdapter)listAdapter).updateHasItems();
                    }
                    else if(listAdapter instanceof Current.Passes.ItemListAdapter)
                    {
                        ((Current.Passes.ItemListAdapter)listAdapter).updateHasItems();
                    }
                    else //if(listAdapter instanceof Current.Coordinates.ItemListAdapter)
                    {
                        ((Current.Coordinates.ItemListAdapter)listAdapter).updateHasItems();
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
                        ((TextView)listAdapter.headerView).setText(text);
                    }

                    switch(page)
                    {
                        case PageType.View:
                            rootView = Page.this.getView();
                            if(rootView != null)
                            {
                                listAdapter.dataID = id;
                                listAdapter.setColumnTitles(rootView.findViewById(listAdapter.itemsRootViewID), null, page);
                            }
                            break;
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
        private static Current.ViewAngles.Item[] viewItems;
        private static Current.Passes.Item[] passItems;
        private static Current.Coordinates.Item[] coordinateItems;
        private static Current.Passes.Item[] intersectionItems;
        private static final Bundle[] params = new Bundle[PageType.PageCount];
        private final Bundle[] savedInputs;
        private final Bundle[] savedSubInputs;
        private static final Object[][] savedItems = new Object[PageType.PageCount][];
        private final OnPageSetListener pageSetListener;
        private static final Selectable.ListFragment.OnOrientationChangedListener[] orientationChangedListeners = new Selectable.ListFragment.OnOrientationChangedListener[PageType.PageCount];
        private static final Selectable.ListFragment.OnItemsChangedListener[] itemsChangedListeners = new Selectable.ListFragment.OnItemsChangedListener[PageType.PageCount];
        private static final Selectable.ListFragment.OnHeaderChangedListener[] headerChangedListeners = new Selectable.ListFragment.OnHeaderChangedListener[PageType.PageCount];
        private static final Selectable.ListFragment.OnGraphChangedListener[] graphChangedListeners = new Selectable.ListFragment.OnGraphChangedListener[PageType.PageCount];
        private static final Selectable.ListFragment.OnInformationChangedListener[] informationChangedListeners = new Selectable.ListFragment.OnInformationChangedListener[PageType.PageCount];

        public PageAdapter(FragmentManager fm, View parentView, Selectable.ListFragment.OnItemDetailButtonClickListener detailListener, Selectable.ListFragment.OnAdapterSetListener adapterListener, OnPageSetListener pageListener, int[] subPg, Bundle savedInstanceState)
        {
            super(fm, parentView, null, null, null, null, detailListener, adapterListener, null, MainActivity.Groups.Calculate, subPg);

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
            pageSetListener = pageListener;
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
            int subPageNum = subPage[position];
            Bundle params;
            Bundle savedParams = null;
            Calendar dateNow = Calendar.getInstance();
            Calendar dateLater = Calendar.getInstance();
            Page newPage = (Page)setupItem((Page)super.instantiateItem(container, position));

            //set later date for later
            dateLater.add(Calendar.DATE, (position == PageType.Passes || position == PageType.Intersection ? 7 : 1));

            //create page
            newPage.setOnPageSetListener(pageSetListener);
            newPage.setOnPageDestroyListener(new Selectable.ListFragment.OnPageDestroyListener()
            {
                @Override
                public void destroyed(Selectable.ListFragment page)
                {
                    int pageNum = page.getPageParam();
                    int subPageNum = page.getSubPageParam();

                    //if a valid page number
                    if(pageNum >= 0 && pageNum < savedInputs.length)
                    {
                        //handle based on sub page
                        switch(subPageNum)
                        {
                            case SubPageType.Input:
                                //save inputs
                                savedInputs[pageNum] = ((Page)page).getInputValues();
                                break;
                        }
                    }
                }
            });

            //get saved params
            switch(subPageNum)
            {
                case SubPageType.Input:
                    savedParams = savedInputs[position];
                    break;
            }

            //set params
            params = newPage.getArguments();
            if(params == null)
            {
                params = new Bundle();
            }
            params.putInt(Selectable.ParamTypes.PageNumber, position);
            params.putInt(ParamTypes.NoradId, Integer.MAX_VALUE);
            params.putInt(ParamTypes.NoradId2, Universe.IDs.Invalid);
            params.putLong(ParamTypes.StartDateMs, dateNow.getTimeInMillis());
            params.putLong(ParamTypes.EndDateMs, dateLater.getTimeInMillis());
            params.putInt(ParamTypes.IncrementUnit, 5);
            params.putInt(ParamTypes.IncrementType, IncrementType.Minutes);
            params.putDouble(ParamTypes.ElevationMinDegs, 0.0);
            params.putDouble(ParamTypes.IntersectionDegs, 0.2);
            if(savedParams != null)
            {
                params.putAll(savedParams);
            }
            params.putInt(Selectable.ParamTypes.SubPageNumber, subPageNum);     //note: makes sure not to use sub page from saved
            switch(position)
            {
                case PageType.View:
                    //do nothing
                    break;

                case PageType.Passes:
                    params.putInt(MainActivity.ParamTypes.PassIndex, savedSubInputs[PageType.Passes].getInt(MainActivity.ParamTypes.PassIndex, Integer.MAX_VALUE));
                    break;

                case PageType.Coordinates:
                    //do nothing
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
        public static void setViewItems(Current.ViewAngles.Item[] items)
        {
            viewItems = items;
        }
        public static void setViewItem(int index, Current.ViewAngles.Item newItem)
        {
            if(viewItems != null && index < viewItems.length)
            {
                viewItems[index] = newItem;
            }
        }

        //Sets pass item(s)
        public static synchronized void setPassItems(Current.Passes.Item[] items)
        {
            passItems = items;
        }
        public static synchronized void setPassItem(int index, Current.Passes.Item newItem)
        {
            if(passItems != null && index < passItems.length)
            {
                passItems[index] = newItem;
            }
        }

        //Sets intersection item(s)
        public static synchronized void setIntersectionItems(Current.Passes.Item[] items)
        {
            intersectionItems = items;
        }
        public static synchronized void setIntersectionItem(int index, Current.Passes.Item newItem)
        {
            if(intersectionItems != null && index < intersectionItems.length)
            {
                intersectionItems[index] = newItem;
            }
        }

        //Adds pass item(s)
        public static synchronized void addPassItems(Current.Passes.Item[] newItems, int insertIndex)
        {
            int index;
            int addLength = newItems.length;
            Current.Passes.Item[] newItemArray = new Current.Passes.Item[passItems.length + addLength];

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
        public static synchronized void addPassItem(Current.Passes.Item newItem)
        {
            int itemCount = passItems.length;
            ArrayList<Current.Passes.Item> itemList = new ArrayList<>(Arrays.asList(passItems));

            //add new item
            itemList.add((itemCount > 0 ? (itemCount - 1) : 0), newItem);
            setPassItems(itemList.toArray(new Current.Passes.Item[0]));

            //update
            notifyItemsChanged(PageType.Passes);
        }

        //Removes a pass item
        protected static synchronized void removePassItem(int index)
        {
            ArrayList<Current.Passes.Item> itemList;

            //if a valid index
            if(passItems != null && index >= 0 && index < passItems.length)
            {
                //remove item
                itemList = new ArrayList<>(Arrays.asList(passItems));
                itemList.remove(index);
                passItems = itemList.toArray(new Current.Passes.Item[0]);

                //update
                notifyItemsChanged(PageType.Passes);
            }
        }

        //Sets coordinate item(s)
        public static void setCoordinateItems(Current.Coordinates.Item[] items)
        {
            coordinateItems = items;
        }
        public static void setCoordinateItem(int index, Current.Coordinates.Item newItem)
        {
            if(coordinateItems != null && index < coordinateItems.length)
            {
                coordinateItems[index] = newItem;
            }
        }

        //Adds intersection item(s)
        public static synchronized void addIntersectionItems(Current.Passes.Item[] newItems, int insertIndex)
        {
            int index;
            int addLength = newItems.length;
            Current.Passes.Item[] newItemArray = new Current.Passes.Item[intersectionItems.length + addLength];

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
        public static synchronized void addIntersectionItem(Current.Passes.Item newItem)
        {
            int itemCount = intersectionItems.length;
            ArrayList<Current.Passes.Item> itemList = new ArrayList<>(Arrays.asList(intersectionItems));

            //add new item
            itemList.add((itemCount > 0 ? (itemCount - 1) : 0), newItem);
            setIntersectionItems(itemList.toArray(new Current.Passes.Item[0]));

            //update
            notifyItemsChanged(PageType.Intersection);
        }

        //Removes an intersection item
        protected static synchronized void removeInterSectionItem(int index)
        {
            ArrayList<Current.Passes.Item> itemList;

            //if a valid index
            if(intersectionItems != null && index >= 0 && index < intersectionItems.length)
            {
                //remove item
                itemList = new ArrayList<>(Arrays.asList(intersectionItems));
                itemList.remove(index);
                intersectionItems = itemList.toArray(new Current.Passes.Item[0]);

                //update
                notifyItemsChanged(PageType.Intersection);
            }
        }

        //Get view angle item(s)
        public static Current.ViewAngles.Item[] getViewAngleItems()
        {
            return(viewItems);
        }
        public static Current.ViewAngles.Item getViewAngleItem(int index)
        {
            return(viewItems != null && index >= 0 && index < viewItems.length ? viewItems[index] : null);
        }

        //Gets pass item(s)
        public static synchronized Current.Passes.Item[] getPassItems()
        {
            return(passItems);
        }
        public static synchronized Current.Passes.Item getPassItem(int index)
        {
            return(passItems != null && index >= 0 && index < passItems.length ? passItems[index] : null);
        }

        //Gets coordinate item(s)
        public static Current.Coordinates.Item[] getCoordinatesItems()
        {
            return(coordinateItems);
        }
        public static Current.Coordinates.Item getCoordinatesItem(int index)
        {
            return(coordinateItems != null && index >= 0 && index < coordinateItems.length ? coordinateItems[index] : null);
        }

        //Gets intersection item(s)
        public static synchronized Current.Passes.Item[] getIntersectionItems()
        {
            return(intersectionItems);
        }
        public static synchronized Current.Passes.Item getIntersectionItem(int index)
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
            if(position >= 0 && position < Current.PageType.PageCount && graphChangedListeners[position] != null)
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
    public static CalculateViewsTask calculateViews(Context context, Database.SatelliteData satellite, Current.ViewAngles.Item[] savedViewItems, Calculations.ObserverType observer, double julianStartDate, double julianEndDate, double dayIncrement, CalculateViewsTask.OnProgressChangedListener listener)
    {
        CalculateViewsTask task;
        Bundle params = PageAdapter.getParams(Calculate.PageType.View);
        int unitType = (params != null ? getUnitType(params) : IncrementType.Seconds);

        //start calculating for start and end dates with given increment
        task = new CalculateViewsTask(listener);
        task.execute(context, new CalculateViewsTask.OrbitalPathBase[]{new CalculateViewsTask.OrbitalPathBase(satellite)}, savedViewItems, observer, julianStartDate, julianEndDate, dayIncrement, false, false, (unitType != IncrementType.Seconds), false);

        //return task
        return(task);
    }

    //Begin calculating view information
    public static CalculateCoordinatesTask calculateCoordinates(Context context, Database.SatelliteData satellite, Current.Coordinates.Item[] savedCoordinateItems, Calculations.ObserverType observer, double julianStartDate, double julianEndDate, double dayIncrement, CalculateCoordinatesTask.OnProgressChangedListener listener)
    {
        CalculateCoordinatesTask task;

        //start calculating for start and end dates with given increment
        task = new CalculateCoordinatesTask(listener);
        task.execute(context, new CalculateCoordinatesTask.CoordinateBase[]{new CalculateCoordinatesTask.CoordinateBase(satellite)}, savedCoordinateItems, observer, julianStartDate, julianEndDate, dayIncrement, false, false);

        //return task
        return(task);
    }

    private static View onCreateView(final Page page, LayoutInflater inflater, ViewGroup container, Bundle saveInstanceState)
    {
        int incrementUnit = 10;
        double elMin = 0.0;
        double intersectionDegs = 0.2;
        final int pageNumber = saveInstanceState.getInt(Selectable.ParamTypes.PageNumber, PageType.View);
        int orbitalId = Integer.MAX_VALUE;
        int orbitalId2 = Universe.IDs.Invalid;
        int incrementType = IncrementType.Minutes;
        int elRowVisibility = View.VISIBLE;
        int viewRowVisibility = View.VISIBLE;
        int intersectionRowVisibility = View.VISIBLE;
        String text;
        Context context = page.getContext();
        Database.DatabaseSatellite[] orbitals;
        Calendar dateNow = Calendar.getInstance();
        Calendar dateLater = Calendar.getInstance();
        ViewGroup rootView = (ViewGroup)inflater.inflate(R.layout.calculate_view_layout, container, false);
        View viewRow = rootView.findViewById(R.id.Calculate_View_Row);
        View intersectionRow = rootView.findViewById(R.id.Calculate_Intersection_Row);
        View elevationMinRow = rootView.findViewById(R.id.Calculate_Elevation_Min_Row);
        TextView orbital2ListTitle = rootView.findViewById(R.id.Calculate_Orbital2_List_Title);
        MaterialButton startButton = rootView.findViewById(R.id.Calculate_Start_Button);
        IconSpinner.CustomAdapter incrementAdapter;
        String[] incrementTypeArray = (context != null ? getIncrementTypes(context) : null);

        //set page displays
        page.viewUnitText = rootView.findViewById(R.id.Calculate_View_Unit_Text);
        page.intersectionUnitText = rootView.findViewById(R.id.Calculate_Intersection_Unit_Text);
        page.elevationMinUnitText = rootView.findViewById(R.id.Calculate_Elevation_Min_Unit_Text);
        page.orbitalList = rootView.findViewById(R.id.Calculate_Orbital_List);
        page.orbital2List = rootView.findViewById(R.id.Calculate_Orbital2_List);
        page.viewUnitList = rootView.findViewById(R.id.Calculate_View_Unit_List);
        page.startDateText = rootView.findViewById(R.id.Calculate_Start_Date_Text);
        page.endDateText = rootView.findViewById(R.id.Calculate_End_Date_Text);
        page.startTimeText = rootView.findViewById(R.id.Calculate_Start_Time_Text);
        page.endTimeText = rootView.findViewById(R.id.Calculate_End_Time_Text);

        //load objects
        orbitals = Database.getOrbitals(context);

        try
        {
            //get values
            orbitalId = saveInstanceState.getInt(ParamTypes.NoradId);
            orbitalId2 = saveInstanceState.getInt(ParamTypes.NoradId2, orbitalId2);
            dateNow.setTimeInMillis(saveInstanceState.getLong(ParamTypes.StartDateMs));
            dateLater.setTimeInMillis(saveInstanceState.getLong(ParamTypes.EndDateMs));
            incrementUnit = saveInstanceState.getInt(ParamTypes.IncrementUnit);
            incrementType = saveInstanceState.getInt(ParamTypes.IncrementType);
            elMin = saveInstanceState.getDouble(ParamTypes.ElevationMinDegs, elMin);
            intersectionDegs = saveInstanceState.getDouble(ParamTypes.IntersectionDegs, intersectionDegs);
        }
        catch(Exception ex)
        {
            //do nothing
        }

        //set orbital list items
        page.orbitalList.setAdapter(new IconSpinner.CustomAdapter(context, orbitals));
        if(orbitalId != Integer.MAX_VALUE)
        {
            page.orbitalList.setSelectedValue(orbitalId);
        }
        page.orbital2List.setAdapter(new IconSpinner.CustomAdapter(context, orbitals));
        if(orbitalId2 != Universe.IDs.Invalid)
        {
            page.orbital2List.setSelectedValue(orbitalId2);
        }
        page.orbital2List.setVisibility(pageNumber == PageType.Intersection ? View.VISIBLE : View.GONE);
        text = (context != null ? (context.getString(R.string.title_orbital) + " 2") : "");
        orbital2ListTitle.setText(text);
        orbital2ListTitle.setVisibility(pageNumber == PageType.Intersection ? View.VISIBLE : View.GONE);

        //set date and time texts
        page.startDateText.setDate(dateNow);
        page.startTimeText.setTime(dateNow);
        page.endDateText.setDate(dateLater);
        page.endTimeText.setTime(dateLater);

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
                    //set adapter
                    incrementAdapter = new IconSpinner.CustomAdapter(context, incrementTypeArray);

                    //set unit displays
                    page.viewUnitText.setText(String.valueOf(incrementUnit));
                    page.viewUnitList.setAdapter(incrementAdapter);
                    page.viewUnitList.setSelectedValue(incrementTypeArray[incrementType], incrementTypeArray[IncrementType.Minutes]);
                }

                //set visibility
                elRowVisibility = intersectionRowVisibility = View.GONE;
                break;

            case PageType.Passes:
                //set visibility
                intersectionRowVisibility = View.GONE;
                //fall through

            case PageType.Intersection:
                //set unit display
                page.elevationMinUnitText.setText(String.valueOf(elMin));
                page.intersectionUnitText.setText(String.valueOf(intersectionDegs));

                //set visibility
                viewRowVisibility = View.GONE;
                break;
        }
        viewRow.setVisibility(viewRowVisibility);
        intersectionRow.setVisibility(intersectionRowVisibility);
        elevationMinRow.setVisibility(elRowVisibility);

        //setup button
        startButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Bundle inputParams = page.getInputValues();

                //if inputs are set and -not on intersection- or -different ID selections-
                if(inputParams != null && (pageNumber != PageType.Intersection || inputParams.getInt(ParamTypes.NoradId, Universe.IDs.Invalid) != inputParams.getInt(ParamTypes.NoradId2, Universe.IDs.Invalid)))
                {
                    //start calculation
                    page.startCalculation(inputParams);
                }
            }
        });

        //return view
        return(rootView);
    }
}
