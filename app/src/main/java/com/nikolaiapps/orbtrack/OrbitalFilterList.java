package com.nikolaiapps.orbtrack;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;


public class OrbitalFilterList
{
    //On load items listener
    public interface OnLoadItemsListener
    {
        void onLoaded(ArrayList<Item> items, boolean foundLaunchDate);
    }

    //Select orbital filter list item
    protected static class Item extends Selectable.ListItem
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
            super(-1, -1, false, false, true, startCheck);

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

            this.satellite = new UpdateService.MasterSatellite(satellite.noradId, satellite.name, satellite.ownerCode, satellite.ownerName, satellite.launchDateMs);
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
        private static int listBgSelectedColor;
        private static int listTextSelectedColor;
        private TableLayout searchTable;
        private AppCompatImageButton showButton;

        //Used data
        protected static class UsedData
        {
            final ArrayList<UpdateService.MasterOwner> owners;
            final ArrayList<String> categories;

            public UsedData(ArrayList<UpdateService.MasterOwner> usedOwners, ArrayList<String> usedCategories)
            {
                owners = usedOwners;
                categories = usedCategories;
            }
        }

        protected boolean foundLaunchDate;
        protected ArrayList<Item> displayedItems;
        protected Item[] allItems;

        private void baseConstructor()
        {
            foundLaunchDate = false;
            displayedItems = new ArrayList<>(0);
            allItems = new Item[0];
        }

        public OrbitalListAdapter(Context context)
        {
            super(context);
            baseConstructor();
        }
        public OrbitalListAdapter(View parentView, String categoryTitle)
        {
            super(parentView, categoryTitle);
            baseConstructor();
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
        protected void showViews(String ownerCode, String categoryName, long currentMs, int ageValue, String searchString)
        {
            int index;
            String listAllString = (currentContext != null ? currentContext.getString(R.string.title_list_all) : null);
            String searchStringInsensitive = searchString.trim().toLowerCase();

            //clear current items
            displayedItems.clear();

            //go through all views
            for(index = 0; index < allItems.length; index++)
            {
                //remember current item
                OrbitalFilterList.Item item = allItems[index];

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

            //get owners and categories
            for(index = 0; index < allItems.length; index++)
            {
                //remember current item, owner, and code
                OrbitalFilterList.Item currentItem = allItems[index];
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

            //return used data
            return(new UsedData(usedOwners, usedCategories));
        }

        //Sets up owner list
        private void setupOwnerList(IconSpinner ownerList, ArrayList<UpdateService.MasterOwner> usedOwners, AdapterView.OnItemSelectedListener itemSelectedListener)
        {
            int index;
            String listAllString = (currentContext != null ? currentContext.getString(R.string.title_list_all) : null);
            String unknown = Globals.getUnknownString(currentContext);
            IconSpinner.Item[] owners;

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
            ownerList.setAdapter(new IconSpinner.CustomAdapter(currentContext, owners));
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
            ArrayList<String> groups;

            groups = usedCategories;
            groups.add(0, (currentContext != null ? currentContext.getString(R.string.title_list_all) : null));
            groupList.setAdapter(new IconSpinner.CustomAdapter(currentContext, groups.toArray(new String[0])));
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
            Resources res = (currentContext != null ? currentContext.getResources() : null);
            String lastString = (res != null ? res.getString(R.string.title_last_plural) : null);
            String daysString = (res != null ? res.getString(R.string.title_days) : null);
            String monthsString = (res != null ? res.getString(R.string.title_months) : null);

            ageList.setAdapter(new IconSpinner.CustomAdapter(currentContext, res != null ? (new IconSpinner.Item[]
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
            }) : new IconSpinner.Item[0]));
            ageList.setBackgroundColor(listBgColor);
            ageList.setBackgroundItemColor(listBgItemColor);
            ageList.setBackgroundItemSelectedColor(listBgSelectedColor);
            ageList.setTextColor(listTextColor);
            ageList.setTextSelectedColor(listTextSelectedColor);
            ageList.setOnItemSelectedListener(itemSelectedListener);
            ageList.setEnabled(true);
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
        public void setupInputs(TableLayout searchGroup, IconSpinner ownerList, IconSpinner groupList, IconSpinner ageList, TableRow ageRow, EditText searchText, AppCompatImageButton showButton, ArrayList<UpdateService.MasterOwner> usedOwners, ArrayList<String> usedCategories, boolean hasLaunchDates)
        {
            this.searchTable = searchGroup;
            this.showButton = showButton;

            searchText.setText("");

            AdapterView.OnItemSelectedListener itemSelectedListener = new AdapterView.OnItemSelectedListener()
            {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
                {
                    //update visible items
                    OrbitalListAdapter.this.showViews((String)ownerList.getSelectedValue(""), groupList.getSelectedValue("").toString(), System.currentTimeMillis(), (int)ageList.getSelectedValue(0), searchText.getText().toString());
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) { }
            };

            TextWatcher textChangedListener = new TextWatcher()
            {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after){ }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) { }

                @Override
                public void afterTextChanged(Editable s)
                {
                    //update visible items
                    OrbitalListAdapter.this.showViews((String)ownerList.getSelectedValue(""), groupList.getSelectedValue("").toString(), System.currentTimeMillis(), (int)ageList.getSelectedValue(0), s.toString());
                }
            };

            //get colors
            listBgColor = Globals.resolveColorID(currentContext, R.attr.pageTitleBackground);
            listBgItemColor = Globals.resolveColorID(currentContext, R.attr.pageBackground);
            listTextColor = Globals.resolveColorID(currentContext, R.attr.defaultTextColor);
            listBgSelectedColor = Globals.resolveColorID(currentContext, R.attr.columnBackground);
            listTextSelectedColor = Globals.resolveColorID(currentContext, R.attr.columnTitleTextColor);

            //get owners, groups, and ages
            setupOwnerList(ownerList, usedOwners, itemSelectedListener);
            setupGroupList(groupList, usedCategories, itemSelectedListener);
            setupAgeList(ageList, itemSelectedListener);

            //update age row visibility
            ageRow.setVisibility(hasLaunchDates ? View.VISIBLE : View.GONE);

            //setup name text
            searchText.addTextChangedListener(textChangedListener);
            searchText.setEnabled(true);

            //setup show button
            showButton.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    showSearchInputs(searchTable.getVisibility() == View.GONE);
                }
            });
        }
    }
}
