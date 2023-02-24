package com.nikolaiapps.orbtrack;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import android.text.Editable;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
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
        public IconSpinner orbitalList;
        public IconSpinner orbital2List;
        public IconSpinner viewUnitList;
        public DateInputView startDateText;
        public DateInputView endDateText;
        public TimeInputView startTimeText;
        public TimeInputView endTimeText;
        public MaterialButton selectButton;
        public AutoCompleteTextView orbitalTextList;
        public AutoCompleteTextView orbital2TextList;
        public AutoCompleteTextView viewUnitTextList;
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
                            Current.ViewAngles.Item[] savedItems = (Current.ViewAngles.Item[])Calculate.PageAdapter.getSavedItems(page);

                            switch(subPage)
                            {
                                case Globals.SubPageType.List:
                                    listAdapter = new Current.ViewAngles.ItemListAdapter(context, savedItems, (multiNoradId != null ? multiNoradId.size() : 0));
                                    setChangeListeners(listAdapter, page);
                                    newView = this.onCreateView(inflater, container, listAdapter, group, page);
                                    break;

                                case Globals.SubPageType.Lens:
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
                        case Globals.SubPageType.List:
                        case Globals.SubPageType.Lens:
                            Current.Passes.Item[] savedItems = (Current.Passes.Item[])Calculate.PageAdapter.getSavedItems(page);

                            switch(subPage)
                            {
                                case Globals.SubPageType.List:
                                    listAdapter = new Current.Passes.ItemListAdapter(context, page, savedItems, null);
                                    setChangeListeners(listAdapter, page);
                                    newView = this.onCreateView(inflater, container, listAdapter, group, page);
                                    break;

                                case Globals.SubPageType.Lens:
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
                        case Globals.SubPageType.Input:
                            savedInstanceState.putInt(ParamTypes.IncrementUnit, params.getInt(ParamTypes.IncrementUnit));
                            savedInstanceState.putInt(ParamTypes.IncrementType, params.getInt(ParamTypes.IncrementType));
                            break;

                        case Globals.SubPageType.List:
                        case Globals.SubPageType.Map:
                        case Globals.SubPageType.Globe:
                            Current.Coordinates.Item[] savedItems = (Current.Coordinates.Item[])Calculate.PageAdapter.getSavedItems(page);

                            switch(subPage)
                            {
                                case Globals.SubPageType.List:
                                    listAdapter = new Current.Coordinates.ItemListAdapter(context, savedItems, (multiNoradId != null ? multiNoradId.size() : 0), MainActivity.getTimeZone());
                                    setChangeListeners(listAdapter, page);
                                    newView = this.onCreateView(inflater, container, listAdapter, group, page);
                                    break;

                                case Globals.SubPageType.Map:
                                case Globals.SubPageType.Globe:
                                    selectedOrbitals = (multiNoradId != null ? (Database.SatelliteData.getSatellites(context, multiNoradId)) : (savedItems != null && savedItems.length > 0) ? (new Database.SatelliteData[]{new Database.SatelliteData(context, savedItems[0].id)}) : null);
                                    newView = Current.Coordinates.onCreateMapView(this, inflater, container, selectedOrbitals, (subPage == Globals.SubPageType.Globe), savedInstanceState);
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
                    if(viewUnitTextList != null)
                    {
                        unitType = viewUnitTextList.getText().toString();
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
                if(orbitalTextList != null && orbitalTextList.getTag() instanceof Integer)
                {
                    noradId = (int) orbitalTextList.getTag();
                }
                if(pageNumber == PageType.Intersection)
                {
                    if(orbital2List != null)
                    {
                        noradId2 = (int)orbital2List.getSelectedValue(Universe.IDs.Invalid);
                    }
                    if(orbital2TextList != null && orbital2TextList.getTag() instanceof Integer)
                    {
                        noradId2 = (int) orbital2TextList.getTag();
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
                        setOrientationHeaderText(Page.this.listParentView.findViewById(R.id.Header_TextView));
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
                    if(listAdapter instanceof Current.ViewAngles.ItemListAdapter)
                    {
                        rootView = Page.this.getView();

                        ((Current.ViewAngles.ItemListAdapter)listAdapter).updateHasItems();
                        if(rootView != null)
                        {
                            listAdapter.setColumnTitles(rootView.findViewById(listAdapter.itemsRootViewID), null, page);
                        }
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
        private static Current.ViewAngles.Item[] viewItems;
        private static Current.Passes.Item[] passItems;
        private static Current.Coordinates.Item[] coordinateItems;
        private static Current.Passes.Item[] intersectionItems;
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
    public static CalculateViewsTask calculateViews(Context context, Database.SatelliteData[] satellites, Current.ViewAngles.Item[] savedViewItems, Calculations.ObserverType observer, double julianStartDate, double julianEndDate, double dayIncrement, CalculateViewsTask.OnProgressChangedListener listener)
    {
        int index;
        CalculateViewsTask task;
        Bundle params = PageAdapter.getParams(Calculate.PageType.View);
        int unitType = (params != null ? getUnitType(params) : IncrementType.Seconds);
        Calculations.SatelliteObjectType[] satelliteObjects = new Calculations.SatelliteObjectType[satellites != null ? satellites.length : 0];

        //start calculating for start and end dates with given increment
        task = new CalculateViewsTask(listener);
        for(index = 0; index < satelliteObjects.length; index++)
        {
            //set views
            satelliteObjects[index] = new Calculations.SatelliteObjectType(satellites[index].satellite);
        }
        task.execute(context, satelliteObjects, savedViewItems, observer, julianStartDate, julianEndDate, dayIncrement, false, false, (unitType != IncrementType.Seconds), false);

        //return task
        return(task);
    }

    //Begin calculating view information
    public static CalculateCoordinatesTask calculateCoordinates(Context context, Database.SatelliteData[] satellites, Current.Coordinates.Item[] savedCoordinateItems, Calculations.ObserverType observer, double julianStartDate, double julianEndDate, double dayIncrement, CalculateCoordinatesTask.OnProgressChangedListener listener)
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

    //Sets orbital text list value from given norad ID
    private static void setOrbitalTextList(Context context, AutoCompleteTextView orbitalTextList, int noradId)
    {
        if(orbitalTextList != null)
        {
            Database.DatabaseSatellite orbital = Database.getOrbital(context, noradId);
            ListAdapter adapter = orbitalTextList.getAdapter();
            Object firstItem = (adapter != null ? adapter.getItem(0) : null);
            boolean haveOrbital = (orbital != null);
            boolean canUseFirst = (firstItem instanceof String);

            if(haveOrbital || canUseFirst)
            {
                orbitalTextList.setText((haveOrbital ? orbital.getName() : (String)firstItem), false);
            }
        }
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
                setOrbitalTextList(context, page.orbitalTextList, orbitalId);
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
                        if(page.viewUnitTextList != null)
                        {
                            page.viewUnitTextList.setText(incrementTypeArray[incrementType]);
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
                        setOrbitalTextList(context, page.orbital2TextList, orbitalId2);
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

    //Sets up given orbital text list
    private static void setupOrbitalTextList(AutoCompleteTextView orbitalTextList, IconSpinner.CustomAdapter orbitalAdapter, Drawable background, Database.DatabaseSatellite[] orbitals, View selectButtonLayout, boolean usingMulti)
    {
        //if text exists
        if(orbitalTextList != null)
        {
            //set adapter, background, and listener
            orbitalTextList.setAdapter(orbitalAdapter);
            if(background != null)
            {
                orbitalTextList.setDropDownBackgroundDrawable(background);
            }
            orbitalTextList.setText(orbitals[0].getName(), false);
            orbitalTextList.addTextChangedListener(new TextWatcher()
            {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count)
                {
                    int index;
                    int noradId = Universe.IDs.Invalid;

                    //go through each orbital
                    for(index = 0; index < orbitals.length && noradId == Universe.IDs.Invalid; index++)
                    {
                        //if current orbital matches text
                        Database.DatabaseSatellite currentOrbital = orbitals[index];
                        if(currentOrbital.getName().equals(s.toString()))
                        {
                            //set norad ID
                            noradId = currentOrbital.noradId;
                        }
                    }

                    //set tag
                    orbitalTextList.setTag(noradId);

                    //if select button layout exists
                    if(selectButtonLayout != null)
                    {
                        //update visibility
                        selectButtonLayout.setVisibility((usingMulti && noradId == Universe.IDs.Invalid) ? View.VISIBLE : View.GONE);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    //Sets up given orbital list
    private static void setupOrbitalList(IconSpinner orbitalList, IconSpinner.CustomAdapter orbitalAdapter, Drawable background, AdapterView.OnItemSelectedListener listener)
    {
        //if list exists
        if(orbitalList != null)
        {
            //set adapter, background, and listener
            orbitalList.setAdapter(orbitalAdapter);
            if(background != null)
            {
                orbitalList.setPopupBackgroundDrawable(background);
            }
            if(listener != null)
            {
                orbitalList.setOnItemSelectedListener(listener);
            }
        }
    }

    //Create page
    private static View onCreateView(final Page page, LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        Context context = page.getContext();
        final int pageNumber = savedInstanceState.getInt(Selectable.ParamTypes.PageNumber, PageType.View);
        final boolean onIntersection = (pageNumber == PageType.Intersection);
        final boolean usingMulti = (pageNumber == PageType.View || pageNumber == PageType.Coordinates);
        final boolean usingMaterial = Settings.getMaterialTheme(context);
        int viewRowVisibility = View.VISIBLE;
        int elevationMinVisibility = View.VISIBLE;
        int intersectionVisibility = (onIntersection ? View.VISIBLE : View.GONE);
        final Database.DatabaseSatellite[] orbitals;
        ViewGroup rootView = (ViewGroup)inflater.inflate((usingMaterial ? R.layout.calculate_view_layout_material : R.layout.calculate_view_layout), container, false);
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
        IconSpinner.CustomAdapter incrementAdapter;
        String[] incrementTypeArray = (context != null ? getIncrementTypes(context) : null);
        ColorDrawable backgroundColorDrawable = (context != null ? new ColorDrawable(Globals.resolveColorID(context, R.attr.pageBackground)) : null);

        //set page displays
        page.viewUnitText = rootView.findViewById(R.id.Calculate_View_Unit_Text);
        page.intersectionUnitText = rootView.findViewById(R.id.Calculate_Intersection_Unit_Text);
        page.elevationMinUnitText = rootView.findViewById(R.id.Calculate_Elevation_Min_Unit_Text);
        page.orbitalList = rootView.findViewById(R.id.Calculate_Orbital_List);
        page.orbitalTextList = rootView.findViewById(R.id.Calculate_Orbital_Text_List);
        page.orbital2List = rootView.findViewById(R.id.Calculate_Orbital2_List);
        page.orbital2TextList = rootView.findViewById(R.id.Calculate_Orbital2_Text_List);
        page.viewUnitList = rootView.findViewById(R.id.Calculate_View_Unit_List);
        page.viewUnitTextList = rootView.findViewById(R.id.Calculate_View_Unit_Text_List);
        page.startDateText = rootView.findViewById(R.id.Calculate_Start_Date_Text);
        page.endDateText = rootView.findViewById(R.id.Calculate_End_Date_Text);
        page.startTimeText = rootView.findViewById(R.id.Calculate_Start_Time_Text);
        page.endTimeText = rootView.findViewById(R.id.Calculate_End_Time_Text);
        page.selectButton = rootView.findViewById(R.id.Calculate_Select_Button);

        //load objects
        orbitals = Database.getOrbitals(context);

        //set orbital list items
        orbitalAdapter = new IconSpinner.CustomAdapter(context, orbitals, usingMulti, new IconSpinner.CustomAdapter.OnLoadItemsListener()
        {
            @Override
            public void onLoaded(IconSpinner.Item[] loadedItems)
            {
                if(loadedItems.length > 0)
                {
                    String firstValue = loadedItems[0].toString();

                    if(page.orbitalTextList != null)
                    {
                        page.orbitalTextList.setText(firstValue, false);
                    }
                    if(page.orbital2TextList != null)
                    {
                        page.orbital2TextList.setText(firstValue, false);
                    }
                }
            }
        });
        setupOrbitalList(page.orbitalList, orbitalAdapter, backgroundColorDrawable, new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                selectButtonLayout.setVisibility((usingMulti && position == 0) ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        setupOrbitalTextList(page.orbitalTextList, orbitalAdapter, backgroundColorDrawable, orbitals, selectButtonLayout, usingMulti);
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
            setupOrbitalList(page.orbital2List, orbitalAdapter, backgroundColorDrawable, null);
            setupOrbitalTextList(page.orbital2TextList, orbitalAdapter, backgroundColorDrawable, orbitals, null, false);
        }
        if(page.orbital2List != null)
        {
            page.orbital2List.setVisibility(intersectionVisibility);
        }
        if(orbital2TextLayout != null)
        {
            orbital2TextLayout.setVisibility(intersectionVisibility);
        }
        if(orbital2ListTitle != null)
        {
            orbital2ListTitle.setVisibility(intersectionVisibility);
        }
        if(startDateLayout != null)
        {
            startDateLayout.setStartIconDrawable(Globals.getYesNoDrawable(context, R.drawable.ic_calendar_month_white, 24, true, true, true));
        }
        if(startTimeLayout != null)
        {
            startTimeLayout.setStartIconDrawable(Globals.getYesNoDrawable(context, R.drawable.ic_clock_black, 24, true, true, true));
        }
        if(endDateLayout != null)
        {
            endDateLayout.setStartIconDrawable(Globals.getYesNoDrawable(context, R.drawable.ic_calendar_month_white, 24, true, true, false));
        }
        if(endTimeLayout != null)
        {
            endTimeLayout.setStartIconDrawable(Globals.getYesNoDrawable(context, R.drawable.ic_clock_black, 24, true, true, false));
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
                    if(page.viewUnitTextList != null)
                    {
                        page.viewUnitTextList.setAdapter(incrementAdapter);
                        page.viewUnitTextList.setDropDownBackgroundDrawable(backgroundColorDrawable);
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
