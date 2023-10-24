package com.nikolaiapps.orbtrack;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class OrbitalFilterList
{
    //On load items listener
    public interface OnLoadItemsListener
    {
        void onLoaded(ArrayList<Item> items, boolean foundLaunchDate);
    }

    //Select orbital filter list item
    protected static class Item extends Selectable.ListDisplayItem
    {
        public final long launchDateMs;
        public final UpdateService.MasterSatellite satellite;
        public final ArrayList<String> ownerCodes;
        public final ArrayList<String> categories;

        public static class Comparer implements Comparator<Item>
        {
            private final boolean nameOnly;

            public Comparer(boolean byNameOnly)
            {
                nameOnly = byNameOnly;
            }

            @Override
            public int compare(Item value1, Item value2)
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

        public Item(UpdateService.MasterSatellite satellite, String owner, boolean startCheck)
        {
            super(satellite.noradId, -1, false, false, true, startCheck);

            this.satellite = satellite;
            ownerCodes = new ArrayList<>(0);
            ownerCodes.add(owner);
            categories = new ArrayList<>(satellite.categories.size());
            for(String currentCategory : satellite.categories)
            {
                if(currentCategory != null)
                {
                    categories.add(currentCategory.toUpperCase());
                }
            }
            launchDateMs = satellite.launchDateMs;
        }
        public Item(Context context, Database.DatabaseSatellite satellite, int index, boolean canEdit, boolean isSelected)
        {
            super(satellite.noradId, index, canEdit, isSelected, false, false);

            int categoryIndex;
            boolean haveContext = (context != null);
            Resources res = (haveContext ? context.getResources() : null);
            String[][] groups = (haveContext ? Database.getSatelliteCategoriesEnglish(context, satellite.noradId) : new String[0][0]);

            this.satellite = new UpdateService.MasterSatellite(satellite.noradId, satellite.name, satellite.ownerCode, satellite.ownerName, satellite.orbitalType, satellite.launchDateMs);
            ownerCodes = new ArrayList<>(0);
            ownerCodes.add(satellite.ownerCode);
            categories = new ArrayList<>(0);
            for(categoryIndex = 0; categoryIndex < groups.length; categoryIndex++)
            {
                categories.add(Database.LocaleCategory.getCategory(res, groups[categoryIndex][1]).toUpperCase());
            }
            launchDateMs = satellite.launchDateMs;
        }

        public String getOwnerCode()
        {
            return(ownerCodes.size() > 0 ? ownerCodes.get(0) : null);
        }

        public String getOwner(Context context)
        {
            String localOwner = Database.LocaleOwner.getName(context, getOwnerCode());
            return(localOwner != null ? localOwner : satellite.ownerName);
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
            if(obj instanceof Item)
            {
                //equal if norad ID matches
                Item other = (Item)obj;
                return(other.satellite.noradId == this.satellite.noradId);
            }
            else
            {
                //not equal
                return(false);
            }
        }
    }

    protected abstract static class OrbitalListAdapter extends Selectable.ListBaseAdapter
    {
        private static int listBgColor;
        private static int listBgItemColor;
        private static int listTextColor;
        private static int listTitleTextColor;
        private static int listBgSelectedColor;
        private static int listTextSelectedColor;
        private String listAllString;
        private View searchTable;
        private AppCompatImageButton showButton;

        //Used data
        protected static class UsedData
        {
            final ArrayList<UpdateService.MasterOwner> owners;
            final ArrayList<String> categories;
            final ArrayList<Byte> types;

            public UsedData(ArrayList<UpdateService.MasterOwner> usedOwners, ArrayList<String> usedCategories, ArrayList<Byte> usedTypes)
            {
                owners = usedOwners;
                categories = usedCategories;
                types = usedTypes;
            }
        }

        protected boolean foundLaunchDate;
        protected ArrayList<Item> displayedItems;
        protected Item[] allItems;

        private void baseConstructor(Context context)
        {
            boolean haveContext = (context != null);

            foundLaunchDate = false;
            displayedItems = new ArrayList<>(0);
            allItems = new Item[0];
            listTitleTextColor = (haveContext ? Globals.resolveColorID(context, R.attr.titleTextColor) : Color.WHITE);
            listAllString = (haveContext ? context.getString(R.string.title_list_all) : "");
        }

        public OrbitalListAdapter(Context context)
        {
            super(context);
            baseConstructor(context);
        }
        public OrbitalListAdapter(Context context, String categoryTitle)
        {
            super(context, categoryTitle);
            baseConstructor(context);
        }

        @Override @NonNull
        public abstract RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType);

        @Override
        public abstract void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position);

        @Override
        public int getItemCount()
        {
            return(loadingItems ? 1 : displayedItems.size());
        }

        public int getAllItemCount()
        {
            return(allItems != null ? allItems.length : 0);
        }

        @Override
        public OrbitalFilterList.Item getItem(int position)
        {
            return(loadingItems || position < 0 || position >= displayedItems.size() ? null : displayedItems.get(position));
        }

        @Override
        public long getItemId(int position)
        {
            return(loadingItems || position < 0 || position >= displayedItems.size() ? 0 : displayedItems.get(position).satellite.noradId);
        }

        //Sets visibility of views by group
        @SuppressLint("NotifyDataSetChanged")
        private void showViews(String ownerCode, String categoryName, long currentMs, int ageValue, byte typeValue, String searchString)
        {
            int index;
            String searchStringInsensitive = searchString.trim().toLowerCase();

            //clear current items
            displayedItems.clear();

            //go through all views
            for(index = 0; index < allItems.length; index++)
            {
                //remember current item
                OrbitalFilterList.Item item = allItems[index];

                //add to displayed if -category is "ALL" or matches given- and -age is any or within day range- and -search string is "" or contains given-
                if((ownerCode.equals(listAllString) || item.ownerCodes.contains(ownerCode)) && (categoryName.equals(listAllString) || item.categories.contains(categoryName)) && (ageValue == -1 || ((currentMs - item.launchDateMs) <= (ageValue * Calculations.MsPerDay))) && (typeValue == -1 || item.satellite.orbitalType == typeValue) && (searchStringInsensitive.equals("") || item.satellite.name.toLowerCase().contains(searchStringInsensitive) || String.valueOf(item.satellite.noradId).contains(searchStringInsensitive)))
                {
                    //add to displayed items
                    displayedItems.add(item);
                }
            }

            //refresh list
            this.notifyDataSetChanged();
        }
        private void showViews(SelectListInterface ownerList, SelectListInterface groupList, SelectListInterface ageList, SelectListInterface typeList, CustomSearchView searchView)
        {
            showViews((ownerList != null ? (String)ownerList.getSelectedValue(listAllString) : listAllString), (groupList != null ? groupList.getSelectedValue(listAllString).toString() : listAllString), System.currentTimeMillis(), (ageList != null ? (int)ageList.getSelectedValue(-1) : -1), (typeList != null ? (byte)typeList.getSelectedValue(-1) : -1), (searchView != null ? searchView.getQuery().toString() : ""));
        }

        //Returns if a launch date was found
        public boolean getHasLaunchDates()
        {
            return(foundLaunchDate);
        }

        //Gets used data
        public UsedData getUsed()
        {
            int index;
            int index2;
            ArrayList<UpdateService.MasterOwner> usedOwners = new ArrayList<>(0);
            ArrayList<String> usedOwnerCodes = new ArrayList<>(0);
            ArrayList<String> usedCategories = new ArrayList<>(0);
            ArrayList<Byte> usedTypes = new ArrayList<>(0);

            //get owners and categories
            for(index = 0; index < allItems.length; index++)
            {
                //remember current item, owner, code, and type
                OrbitalFilterList.Item currentItem = allItems[index];
                UpdateService.MasterOwner currentOwner = new UpdateService.MasterOwner(currentItem.satellite.ownerCode, currentItem.satellite.ownerName);
                String ownerCode = currentOwner.code;
                byte currentType = currentItem.satellite.orbitalType;

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

                //if type is not in the list
                if(!usedTypes.contains(currentType))
                {
                    //add it
                    usedTypes.add(currentType);
                }
            }

            //sort owners, categories, and types
            Collections.sort(usedOwners);
            Collections.sort(usedCategories);
            Collections.sort(usedTypes);

            //return used data
            return(new UsedData(usedOwners, usedCategories, usedTypes));
        }

        //Sets up list adapter
        private void setupListAdapter(SelectListInterface inputList, IconSpinner.CustomAdapter listAdapter, AdapterView.OnItemSelectedListener itemSelectedListener)
        {
            if(inputList != null)
            {
                inputList.setAdapter(listAdapter);
                inputList.setBackgroundColor(listBgColor);
                inputList.setBackgroundItemColor(listBgItemColor);
                inputList.setBackgroundItemSelectedColor(listBgSelectedColor);
                inputList.setTextColor(listTextColor, listTitleTextColor);
                inputList.setTextSelectedColor(listTextSelectedColor);
                inputList.setOnItemSelectedListener(itemSelectedListener);
                inputList.setEnabled(true);
            }
        }

        //Sets up owner list
        private void setupOwnerList(SelectListInterface ownerList, ArrayList<UpdateService.MasterOwner> usedOwners, AdapterView.OnItemSelectedListener itemSelectedListener)
        {
            int index;
            String listAllString = (haveContext() ? currentContext.getString(R.string.title_list_all) : null);
            String unknown = Globals.getUnknownString(currentContext);
            IconSpinner.Item[] owners;

            if(usedOwners != null)
            {
                if(unknown != null)
                {
                    unknown = unknown.toUpperCase();
                }
                owners = new IconSpinner.Item[usedOwners.size() + 1];
                owners[0] = new IconSpinner.Item(listAllString, listAllString);
                for(index = 0; index < usedOwners.size(); index++)
                {
                    UpdateService.MasterOwner currentItem = usedOwners.get(index);
                    int[] ownerIconIds = Globals.getOwnerIconIDs(currentItem.code);
                    owners[index + 1] = new IconSpinner.Item(ownerIconIds, (currentItem.name == null || currentItem.name.equals("") ? unknown : currentItem.name), currentItem.code);
                }
                setupListAdapter(ownerList, new IconSpinner.CustomAdapter(currentContext, owners), itemSelectedListener);
            }
        }

        //Sets up group list
        private void setupGroupList(SelectListInterface groupList, ArrayList<String> usedCategories, AdapterView.OnItemSelectedListener itemSelectedListener)
        {
            ArrayList<String> groups;

            if(usedCategories != null)
            {
                groups = usedCategories;
                groups.add(0, (haveContext() ? currentContext.getString(R.string.title_list_all) : null));
                setupListAdapter(groupList, new IconSpinner.CustomAdapter(currentContext, groups.toArray(new String[0])), itemSelectedListener);
            }
        }

        //Sets up age list
        private void setupAgeList(SelectListInterface ageList, AdapterView.OnItemSelectedListener itemSelectedListener)
        {
            Resources res = (haveContext() ? currentContext.getResources() : null);
            String lastString = (res != null ? res.getString(R.string.title_last_plural) : null);
            String daysString = (res != null ? res.getString(R.string.title_days) : null);
            String monthsString = (res != null ? res.getString(R.string.title_months) : null);

            setupListAdapter(ageList, new IconSpinner.CustomAdapter(currentContext, res != null ? (new IconSpinner.Item[]
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
            }) : new IconSpinner.Item[0]), itemSelectedListener);
        }

        //Sets up type list
        private void setupTypeList(SelectListInterface typeList, List<Byte> usedTypes, AdapterView.OnItemSelectedListener itemSelectedListener)
        {
            Resources res = (haveContext() ? currentContext.getResources() : null);
            boolean haveRes = (res != null);
            String listAllString = (haveRes ? res.getString(R.string.title_list_all) : null);
            String unknown = Globals.getUnknownString(currentContext);
            ArrayList<IconSpinner.Item> types = new ArrayList<>(0);

            if(usedTypes != null)
            {
                types.add(new IconSpinner.Item(listAllString, (byte)-1));
                for(byte currentType : usedTypes)
                {
                    int currentId = Integer.MIN_VALUE;
                    String currentTypeString;

                    switch(currentType)
                    {
                        case Database.OrbitalType.Star:
                        case Database.OrbitalType.Sun:
                            currentTypeString = (haveRes ? res.getString(R.string.title_stars) : null);
                            break;

                        case Database.OrbitalType.Planet:
                            currentId = Universe.IDs.Saturn;
                            currentTypeString = (haveRes ? res.getString(R.string.title_planets) : null);
                            break;

                        case Database.OrbitalType.Satellite:
                            currentId = 1;
                            currentTypeString = (haveRes ? res.getString(R.string.title_satellites) : null);
                            break;

                        case Database.OrbitalType.RocketBody:
                            currentId = 1;
                            currentTypeString = (haveRes ? res.getString(R.string.title_rocket_bodies) : null);
                            break;

                        case Database.OrbitalType.Debris:
                            currentId = 1;
                            currentTypeString = (haveRes ? res.getString(R.string.title_debris) : null);
                            break;

                        case Database.OrbitalType.Constellation:
                            currentTypeString = (haveRes ? res.getString(R.string.title_constellations) : null);
                            break;

                        default:
                            currentTypeString = unknown;
                            break;
                    }

                    types.add(new IconSpinner.Item(Globals.getOrbitalIcon(currentContext, MainActivity.getObserver(), currentId, currentType), currentTypeString, currentType));
                }

                Collections.sort(types, new IconSpinner.Item.Comparer());
                setupListAdapter(typeList, new IconSpinner.CustomAdapter(currentContext, types.toArray(new IconSpinner.Item[0])), itemSelectedListener);
            }
        }

        //Shows/hides search inputs
        public void showSearchInputs(boolean show)
        {
            if(searchTable != null)
            {
                searchTable.setVisibility(show ? View.VISIBLE : View.GONE);
            }
            if(showButton != null)
            {
                showButton.setImageResource(show ? R.drawable.ic_expand_less : R.drawable.ic_expand_more);
            }
        }

        //Sets up inputs
        public void setupInputs(View searchGroup, SelectListInterface ownerList, SelectListInterface groupList, SelectListInterface ageList, SelectListInterface typeList, View ageLayout, View groupLayout, View ownerLayout, View typeLayout, CustomSearchView searchView, AppCompatImageButton showButton, ArrayList<UpdateService.MasterOwner> usedOwners, ArrayList<String> usedCategories, List<Byte> usedTypes, boolean hasLaunchDates)
        {
            boolean usingOwners = (ownerList != null);
            boolean usingGroups = (groupList != null);
            boolean usingAge = (ageList != null);
            boolean usingType = (typeList != null);

            this.searchTable = searchGroup;
            this.showButton = showButton;

            AdapterView.OnItemSelectedListener itemSelectedListener = new AdapterView.OnItemSelectedListener()
            {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
                {
                    //update visible items
                    OrbitalListAdapter.this.showViews(ownerList, groupList, ageList, typeList, searchView);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) { }
            };

            //get colors
            listBgColor = Globals.resolveColorID(currentContext, R.attr.colorAccentDark);
            listBgItemColor = Globals.resolveColorID(currentContext, android.R.attr.colorBackground);
            listTextColor = Globals.resolveColorID(currentContext, android.R.attr.textColor);
            listBgSelectedColor = Globals.resolveColorID(currentContext, R.attr.colorAccentVariant);
            listTextSelectedColor = Globals.resolveColorID(currentContext, R.attr.colorAccentLightest);

            //get owners, groups, and ages
            if(usingOwners)
            {
                setupOwnerList(ownerList, usedOwners, itemSelectedListener);
            }
            if(usingGroups)
            {
                setupGroupList(groupList, usedCategories, itemSelectedListener);
            }
            if(usingAge)
            {
                setupAgeList(ageList, itemSelectedListener);
            }
            if(usingType)
            {
                setupTypeList(typeList, usedTypes, itemSelectedListener);
            }

            //update visibility
            if(ownerLayout != null)
            {
                ownerLayout.setVisibility(usingOwners ? View.VISIBLE : View.GONE);
            }
            if(groupLayout != null)
            {
                groupLayout.setVisibility(usingGroups ? View.VISIBLE : View.GONE);
            }
            if(ageLayout != null)
            {
                ageLayout.setVisibility(hasLaunchDates && usingAge ? View.VISIBLE : View.GONE);
            }
            if(typeLayout != null)
            {
                typeLayout.setVisibility(usingType ? View.VISIBLE : View.GONE);
            }

            //setup search view
            if(searchView != null)
            {
                searchView.setQueryHint(searchView.getContext().getString(R.string.title_name_or_id));
                searchView.setOnQueryTextListener(new CustomSearchView.OnQueryTextListener()
                {
                    private void updateItems()
                    {
                        //update visible items
                        OrbitalListAdapter.this.showViews(ownerList, groupList, ageList, typeList, searchView);
                    }

                    @Override
                    public boolean onQueryTextSubmit(String query)
                    {
                        updateItems();
                        return(false);
                    }

                    @Override
                    public boolean onQueryTextChange(String newText)
                    {
                        updateItems();
                        return(false);
                    }
                });
            }

            //setup show button
            showButton.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    if(searchTable != null)
                    {
                        showSearchInputs(searchTable.getVisibility() == View.GONE);
                    }
                }
            });
        }
    }
}
