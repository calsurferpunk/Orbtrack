package com.nikolaiapps.orbtrack;


import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatSpinner;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.TimeZone;


public class IconSpinner extends AppCompatSpinner implements SelectListInterface
{
    public static class Item
    {
        float rotate;
        final String text;
        String subText;
        final Object value;
        Drawable icon1;
        Drawable icon3;
        int icon1Id;
        int icon2Id;
        int icon3Id;
        int icon1Color;
        int icon1SelectedColor;
        int icon3Color;
        int icon3TintColor;
        int icon3SelectedColor;
        boolean iconsUseThemeTint;
        int[] icon1Ids;

        public static class Comparer implements Comparator<Item>
        {
            @Override
            public int compare(Item lhs, Item rhs)
            {
                return(lhs.text.compareTo(rhs.text));
            }
        }

        public Item(String txt, Object val)
        {
            rotate = 0;
            icon1Id = -1;
            icon2Id = -1;
            icon1 = null;
            icon3 = null;
            icon3Id = -1;
            text = txt;
            subText = null;
            value = val;
            icon1Color = icon1SelectedColor = icon3Color = icon3TintColor = icon3SelectedColor = Color.TRANSPARENT;
            iconsUseThemeTint = false;
            icon1Ids = null;
        }
        public Item(String txt, String subText, Object val)
        {
            this(txt, val);
            this.subText = subText;
        }
        public Item(Drawable icon3, String txt, Object val)
        {
            this(txt, val);
            this.icon3 = icon3;
        }
        public Item(int icon3Id, boolean iconsUseThemeTint, String txt, Object val)
        {
            this(txt, val);
            this.icon3Id = icon3Id;
            this.iconsUseThemeTint = iconsUseThemeTint;
        }
        public Item(int icon3Id, boolean iconsUseThemeTint, Object val)
        {
            this(icon3Id, iconsUseThemeTint, null, val);
        }
        public Item(int icon3Id, int tintColor, Object val)
        {
            this(icon3Id, false, null, val);
            this.icon3TintColor = tintColor;
        }
        public Item(int icon3Id, boolean iconsUseThemeTint, String txt, float rotateAngle)
        {
            this(icon3Id, iconsUseThemeTint, txt, txt);
            rotate = rotateAngle;
        }
        public Item(Drawable icon3, String txt, Object val, String subText)
        {
            this(icon3, txt, val);
            this.subText = subText;
        }
        public Item(int icon1Id, int icon1Color, int icon1SelectedColor, String txt, Object val)
        {
            this(txt, val);
            this.icon1Id = icon1Id;
            this.icon1Color = icon1Color;
            this.icon1SelectedColor = icon1SelectedColor;
        }
        public Item(int icon1Id, int icon1Color, int icon1SelectedColor, int icon2Id, Drawable icon3, int icon3Color, int icon3SelectedColor, boolean iconsUseThemeTint, String txt, Object val)
        {
            this(icon3, txt, val);
            this.icon1Id = icon1Id;
            this.icon2Id = icon2Id;
            this.icon1Color = icon1Color;
            this.icon1SelectedColor = icon1SelectedColor;
            this.icon3Color = icon3Color;
            this.icon3SelectedColor = icon3SelectedColor;
            this.iconsUseThemeTint = iconsUseThemeTint;
        }
        public Item(int[] icon1Ids, String txt, Object val)
        {
            this(-1, Color.TRANSPARENT, Color.TRANSPARENT, -1, null, Color.TRANSPARENT, Color.TRANSPARENT, false, txt, val);
            this.icon1Ids = icon1Ids;
        }
        public Item(Context context, int colorID, String txt, Object val)
        {
            this(txt, val);
            icon3 = new ColorDrawable(ContextCompat.getColor(context, colorID));
        }

        public void loadIcons(Context context, int iconHeightPx)
        {
            if(iconHeightPx > 0)
            {
                icon1 = (icon1Id > 0 ? Globals.getDrawable(context, icon1Id, iconsUseThemeTint) : icon1Ids != null ? Globals.getDrawableCombined(context, icon1Ids) : null);

                if(icon3 instanceof ColorDrawable)
                {
                    icon3 = Globals.getDrawableColor(context, (ColorDrawable)icon3, iconHeightPx);
                }
                else if(icon3 == null && icon3Id > 0)
                {
                    //noinspection SuspiciousNameCombination
                    icon3 = ((icon3TintColor != Color.TRANSPARENT ? Globals.getDrawableSized(context, icon3Id, iconHeightPx, iconHeightPx, icon3TintColor, false, false) : Globals.getDrawableSized(context, icon3Id, iconHeightPx, iconHeightPx, iconsUseThemeTint, false)));
                }
            }
        }

        private boolean usingIconColors(int color, int selectedColor)
        {
            return(color != Color.TRANSPARENT && selectedColor != Color.TRANSPARENT);
        }

        public boolean usingIcon1Colors()
        {
            return(usingIconColors(icon1Color, icon1SelectedColor));
        }

        public boolean usingIcon3Colors()
        {
            return(usingIconColors(icon3Color, icon3SelectedColor));
        }

        public Drawable getIcon(Context context)
        {
            Drawable icon1Result = (usingIcon1Colors() ? Globals.getDrawableTinted(icon1, icon1Color) : icon1);
            Drawable icon3Result = (usingIcon3Colors() ? Globals.getDrawableTinted(icon3, icon3Color) : icon3);
            return(context != null && icon1Result != null && icon3Result != null ? Globals.getDrawableCombined(context, icon1Result, icon3Result) : icon3Result != null ? icon3Result : icon1Result);
        }

        @Override @NonNull
        public String toString()
        {
            return(text != null ? text : value instanceof Integer ? String.valueOf((int)value) : value != null ? value.toString() : "");
        }
    }

    public static class CustomAdapter extends BaseAdapter implements Filterable
    {
        private class ItemFilter extends Filter
        {
            private final boolean useFilter;

            public ItemFilter(boolean allowFilter)
            {
                useFilter = allowFilter;
            }

            @Override
            protected FilterResults performFiltering(CharSequence constraint)
            {
                FilterResults results = new FilterResults();

                //if using filter and constraint is set
                if(useFilter && constraint != null && constraint.length() > 1)
                {
                    String constraintValue = constraint.toString().toLowerCase();
                    ArrayList<Item> resultItems = new ArrayList<>();

                    //go through each item
                    for(Item currentItem : items)
                    {
                        //remember current value
                        String currentValue = (currentItem.text != null ? currentItem.text.toLowerCase() : "");

                        //if value contains constraint
                        if(currentValue.contains(constraintValue))
                        {
                            //add item to results
                            resultItems.add(currentItem);
                        }
                    }

                    //set results
                    results.count = resultItems.size();
                    results.values = resultItems.toArray(new Item[0]);
                }
                else
                {
                    //set results
                    results.count = items.length;
                    results.values = items;
                }

                //return results
                return(results);
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results)
            {
                items = (Item[])results.values;
                notifyDataSetChanged();
            }
        }

        //On load items listener
        public interface OnLoadItemsListener
        {
            void onLoaded(Item[] loadedItems);
        }

        //Load items task
        private static class LoadItemsTask extends ThreadTask<Object, Void, Void>
        {
            private final OnLoadItemsListener loadItemsListener;

            public LoadItemsTask(OnLoadItemsListener loadItemsListener)
            {
                this.loadItemsListener = loadItemsListener;
            }

            @Override
            protected Void doInBackground(Object... params)
            {
                int index;
                int icon1Color = (int)params[3];
                int icon1SelectedColor = (int)params[4];
                int icon3Color = (int)params[5];
                int icon3SelectedColor = (int)params[6];
                int forceColorId = (int)params[7];
                boolean useIcons;
                boolean isLocation;
                boolean useIcon1Color;
                boolean useIcon3Color;
                boolean addMulti = (boolean)params[2];
                int offset = (addMulti ? 1 : 0);
                Context context = (Context)params[0];
                boolean haveContext = (context != null);
                Database.DatabaseSatellite[] orbitals = (Database.DatabaseSatellite[])params[1];
                IconSpinner.Item[] items;

                //go through each orbital
                items = new Item[orbitals.length + offset];
                if(addMulti)
                {
                    items[0] = new Item((haveContext ? R.drawable.ic_list_white : -1), icon3Color, icon3SelectedColor, (haveContext ? context.getString(R.string.title_multiple) : ""), Universe.IDs.Invalid);
                }
                for(index = 0; index < orbitals.length; index++)
                {
                    //remember current satellite and set item
                    Database.DatabaseSatellite currentSat = orbitals[index];
                    isLocation = (currentSat.noradId == Universe.IDs.CurrentLocation);
                    useIcons = (currentSat.noradId != Integer.MAX_VALUE && !isLocation);
                    useIcon1Color = !useIcons;
                    useIcon3Color = (useIcons && currentSat.noradId > 0 && (currentSat.orbitalType != Database.OrbitalType.Satellite || Settings.getSatelliteIconImageIsThemeable(context)));
                    int[] ownerIconIds = (useIcons ? Globals.getOwnerIconIDs(currentSat.ownerCode) : new int[]{isLocation ? R.drawable.ic_my_location_black : R.drawable.ic_search_black});
                    items[index + offset] = new Item(ownerIconIds[0], (useIcon1Color ? icon1Color : Color.TRANSPARENT), (useIcon1Color ? icon1SelectedColor : Color.TRANSPARENT), (ownerIconIds.length > 1 ? ownerIconIds[1] : -1), (useIcons ? Globals.getOrbitalIcon(context, MainActivity.getObserver(), currentSat.noradId, currentSat.orbitalType, forceColorId) : null), (useIcon3Color ? icon3Color : Color.TRANSPARENT), (useIcon3Color ? icon3SelectedColor : Color.TRANSPARENT), !useIcons, currentSat.getName(), currentSat.noradId);
                }

                //if listeners are set
                if(loadItemsListener != null)
                {
                    //send event
                    loadItemsListener.onLoaded(items);
                }

                //done
                return(null);
            }
        }

        private int defaultIconPx;
        private int defaultIconDp;
        private int selectedIndex;
        private int textColor;
        private int textSelectedColor;
        private int backgroundColor;
        private int backgroundItemColor;
        private int backgroundItemSelectedColor;
        private boolean usingText;
        private boolean usingIcon1;
        private boolean usingIcon3;
        private boolean usingIcon3Only;
        private boolean usingMaterial;
        private boolean loadingItems = false;
        private ItemFilter filter;
        private LayoutInflater listInflater;
        private Item[] items = new Item[0];

        private void getDefaultSize(Context context)
        {
            defaultIconDp = (usingMaterial ? 32 : 24);
            defaultIconPx = (int)(context != null ? Globals.dpToPixels(context, defaultIconDp) : 48);
        }

        private void BaseConstructor(Context context)
        {
            selectedIndex = -1;
            listInflater = (context != null ? (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) : null);
            textColor = textSelectedColor = Globals.resolveColorID(context, android.R.attr.textColor);
            backgroundColor = Globals.resolveColorID(context, R.attr.viewPagerBackground);
            backgroundItemColor = backgroundItemSelectedColor = Globals.resolveColorID(context, R.attr.pageBackground);
            usingMaterial = Settings.getMaterialTheme(context);
            getDefaultSize(context);

            updateUsing();
        }

        public CustomAdapter(Context context, Item[] items)
        {
            ArrayList<Item> usedItems = new ArrayList<>(0);

            for(Item currentItem : items)
            {
                if(currentItem != null)
                {
                    usedItems.add(currentItem);
                }
            }
            this.items = usedItems.toArray(new Item[0]);

            BaseConstructor(context);
        }
        public CustomAdapter(Context context, Object[] items)
        {
            int index;
            boolean useFloat = (items.length > 0 && items[0] instanceof Float);

            this.items = new Item[items.length];
            for(index = 0; index < this.items.length; index++)
            {
                Object currentCopyItem = items[index];
                this.items[index] = new Item((useFloat ? Globals.getNumberString((Float)currentCopyItem, 0) : (currentCopyItem != null ? currentCopyItem.toString() : "")), currentCopyItem);
            }

            BaseConstructor(context);
        }
        public CustomAdapter(Context context, Object[] items, Object[] values, int[] itemImageIds, String[] subTexts)
        {
            int index;

            getDefaultSize(context);

            this.items = new Item[items.length];
            for(index = 0; index < this.items.length; index++)
            {
                Object currentCopyItem = items[index];
                this.items[index] = new Item((itemImageIds != null ? Globals.getDrawableSized(context, itemImageIds[index], defaultIconDp, defaultIconDp, false, true) : null), (currentCopyItem != null ? currentCopyItem.toString() : ""), values[index], (subTexts != null ? subTexts[index] : null));
            }

            BaseConstructor(context);
        }
        public CustomAdapter(Context context, ArrayList<TimeZone> zones)
        {
            int index;

            items = new Item[zones.size()];
            for(index = 0; index < items.length; index++)
            {
                TimeZone currentZone = zones.get(index);
                items[index] = new Item(Globals.getGMTOffsetString(currentZone), currentZone.getID());
            }

            BaseConstructor(context);
        }
        public CustomAdapter(Context context, Database.DatabaseSatellite[] satellites, boolean addMulti, int icon1Color, int icon1SelectedColor, int icon3Color, int icon3SelectedColor, int forceColorId, OnLoadItemsListener listener)
        {
            BaseConstructor(context);

            //update status
            loadingItems = true;

            //load items
            LoadItemsTask loadItems = new LoadItemsTask(new OnLoadItemsListener()
            {
                @Override
                public void onLoaded(Item[] loadedItems)
                {
                    items = loadedItems;
                    updateUsing();
                    loadingItems = false;
                    if(context instanceof Activity)
                    {
                        ((Activity)context).runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                CustomAdapter.this.notifyDataSetChanged();
                            }
                        });
                    }
                    if(listener != null)
                    {
                        listener.onLoaded(loadedItems);
                    }
                }
            });
            loadItems.execute(context, satellites, addMulti, icon1Color, icon1SelectedColor, icon3Color, icon3SelectedColor, forceColorId);
        }
        public CustomAdapter(Context context, Database.DatabaseSatellite[] satellites, boolean addMulti, OnLoadItemsListener listener)
        {
            this(context, satellites, addMulti, Color.TRANSPARENT, Color.TRANSPARENT, (Settings.getDarkTheme(context) ? Color.WHITE : Color.BLACK), (Settings.getDarkTheme(context) ? Color.WHITE : Color.BLACK), 0, listener);
        }
        public CustomAdapter(Context context, Database.DatabaseSatellite[] satellites)
        {
            this(context, satellites, false, null);
        }

        private void updateUsing()
        {
            int index;
            boolean usingIcon2;

            //not using until found
            usingText = usingIcon1 = usingIcon2 = usingIcon3 = usingIcon3Only = false;

            //if items are set
            if(items != null)
            {
                //go through items while any using status is still false
                for(index = 0; index < items.length && (!usingText || !usingIcon1 || !usingIcon3); index++)
                {
                    //remember current item
                    Item currentItem = items[index];

                    //update using status
                    usingText = usingText || (currentItem.text != null);
                    usingIcon1 = usingIcon1 || (currentItem.icon1Id > 0 || currentItem.icon1Ids != null);
                    usingIcon2 = usingIcon2 || (currentItem.icon2Id > 0);
                    usingIcon3 = usingIcon3 || (currentItem.icon3Id > 0 || currentItem.icon3 != null);
                }
            }

            //update if using icon 3 only
            usingIcon3Only = (usingIcon3 && !usingText && !usingIcon1 && !usingIcon2);
        }

        @Override
        public int getCount()
        {
            return(loadingItems ? 1 : (items == null ) ? 0 : items.length);
        }

        @Override
        public Object getItem(int position)
        {
            return(loadingItems || (items == null) || (position < 0) || (position >= items.length) ? null : items[position]);
        }

        @Override
        public long getItemId(int position)
        {
            return(loadingItems ? 0 : position);
        }

        public Item[] getItems()
        {
            return(loadingItems ? new Item[0] : items);
        }

        public Object[] getItemValues()
        {
            int position;
            Object[] values = new Object[loadingItems ? 0 : items.length];

            //go through each time
            for(position = 0; position < values.length; position++)
            {
                //copy value
                values[position] = items[position].value;
            }

            //return values
            return(values);
        }

        @SuppressWarnings("unused")
        public Object getItemValue(int position)
        {
            return(loadingItems ? null : items[position].value);
        }

        public int getItemIndex(Object val)
        {
            int index;

            //if not loading items
            if(!loadingItems)
            {
                //go through each item
                for(index = 0; index < items.length; index++)
                {
                    //if a match
                    Item currentItem = items[index];
                    if(currentItem != null && currentItem.value != null && currentItem.value.equals(val))
                    {
                        //return index
                        return(index);
                    }
                }
            }

            //not found
            return(-1);
        }

        @Override
        public Filter getFilter()
        {
            //if filter is not set yet
            if(filter == null)
            {
                //set it
                filter = new ItemFilter(false);
            }

            //return filter
            return(filter);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            boolean isSelected = (position == selectedIndex);
            boolean haveItemImage1;
            boolean haveItemImage2;
            boolean haveItemImage3;
            int px = 0;
            int color;
            Drawable icon2;
            Context context;
            Rect iconBounds;
            LinearLayout itemBackground;
            AppCompatImageView itemImage1;
            AppCompatImageView itemImage2;
            AppCompatImageView itemImage3;
            TextView itemText;
            TextView itemSubText;
            CircularProgressIndicator itemProgress;
            Item currentItem = (items != null && position >= 0 && position < items.length ? items[position] : new Item("", ""));

            //set view
            if(convertView == null)
            {
                convertView = (listInflater != null ? listInflater.inflate((usingMaterial ? R.layout.icon_spinner_material_item : R.layout.icon_spinner_item), parent, false) : null);
                if(convertView == null)
                {
                    return(new View(parent.getContext()));
                }
            }
            context = convertView.getContext();
            convertView.setBackgroundColor(backgroundColor);
            if(usingIcon3Only)
            {
                LayoutParams viewParams = convertView.getLayoutParams();
                viewParams.width = LinearLayout.LayoutParams.WRAP_CONTENT;
                convertView.setLayoutParams(viewParams);
            }
            itemBackground = convertView.findViewById(R.id.Spinner_Item_Background);
            if(itemBackground != null)
            {
                itemBackground.setBackgroundColor(isSelected ? backgroundItemSelectedColor : backgroundItemColor);
                itemBackground.setVisibility(loadingItems ? View.GONE : View.VISIBLE);
            }

            //get views
            itemImage1 = convertView.findViewById(R.id.Spinner_Item_Image1);
            itemImage2 = convertView.findViewById(R.id.Spinner_Item_Image2);
            itemImage3 = convertView.findViewById(R.id.Spinner_Item_Image3);
            itemText = convertView.findViewById(R.id.Spinner_Item_Text);
            itemSubText = convertView.findViewById(R.id.Spinner_Item_Sub_Text);
            itemProgress = convertView.findViewById(R.id.Spinner_Item_Progress);
            if(itemProgress != null)
            {
                itemProgress.setVisibility(loadingItems ? View.VISIBLE : View.GONE);
            }

            //update status
            haveItemImage1 = (itemImage1 != null);
            haveItemImage2 = (itemImage2 != null);
            haveItemImage3 = (itemImage3 != null);

            //get icons
            icon2 = (currentItem.icon2Id > 0 ? Globals.getDrawable(context, currentItem.icon2Id, currentItem.iconsUseThemeTint) : null);
            if(haveItemImage1 || haveItemImage3)
            {
                px = (haveItemImage3 ? itemImage3 : itemImage1).getMeasuredHeight();
            }
            if(px <= 0)
            {
                px = defaultIconPx;
            }
            currentItem.loadIcons(context, px);

            //if not loading items
            if(!loadingItems)
            {
                //update displays
                if(haveItemImage1)
                {
                    if(currentItem.usingIcon1Colors())
                    {
                        color = (isSelected ? currentItem.icon1SelectedColor : currentItem.icon1Color);
                        itemImage1.setImageDrawable(Globals.getDrawableTinted(currentItem.icon1, color));
                        itemImage1.setColorFilter(color);
                    }
                    else
                    {
                        itemImage1.setImageDrawable(currentItem.icon1);
                        itemImage1.setColorFilter(Color.TRANSPARENT);
                    }
                    if(!usingIcon1)
                    {
                        itemImage1.setVisibility(View.GONE);
                    }
                }
                if(haveItemImage2)
                {
                    itemImage2.setImageDrawable(icon2);
                    if(icon2 != null && itemImage1 != null)
                    {
                        LayoutParams viewParams = itemImage1.getLayoutParams();
                        viewParams.width = itemImage2.getLayoutParams().width;
                        itemImage1.setLayoutParams(viewParams);
                        itemImage2.setVisibility(View.VISIBLE);
                    }
                }
                if(haveItemImage3)
                {
                    if(currentItem.usingIcon3Colors())
                    {
                        color = (isSelected ? currentItem.icon3SelectedColor : currentItem.icon3Color);
                        itemImage3.setImageDrawable(Globals.getDrawableTinted(currentItem.icon3, color));
                        itemImage3.setColorFilter(color);
                    }
                    else
                    {
                        itemImage3.setImageDrawable(currentItem.icon3);
                        itemImage3.setColorFilter(Color.TRANSPARENT);
                    }
                    if(currentItem.rotate != 0)
                    {
                        itemImage3.setRotation(currentItem.rotate);
                    }
                    if(!usingIcon3)
                    {
                        itemImage3.setVisibility(View.GONE);
                    }
                    if(usingIcon3Only)
                    {
                        LayoutParams imageParams = itemImage3.getLayoutParams();
                        iconBounds = (currentItem.icon3 != null ? currentItem.icon3.getBounds() : new Rect());
                        if(iconBounds.width() > 0)
                        {
                            imageParams.width = iconBounds.width();
                            imageParams.height = iconBounds.height();
                        }
                        else
                        {
                            imageParams.width = LinearLayout.LayoutParams.WRAP_CONTENT;
                            imageParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
                        }
                        itemImage3.setLayoutParams(imageParams);
                    }
                }
                if(itemText != null)
                {
                    itemText.setText(currentItem.text);
                    itemText.setTextColor(isSelected ? textSelectedColor : textColor);
                }
                if(itemSubText != null)
                {
                    if(currentItem.subText != null)
                    {
                        itemSubText.setText(currentItem.subText);
                        itemSubText.setTextColor(isSelected ? textSelectedColor : textColor);
                    }
                    else
                    {
                        itemSubText.setVisibility(View.GONE);
                    }
                }
            }

            //return view
            return(convertView);
        }

        public int getBackgroundColor()
        {
            return(backgroundColor);
        }

        public void setBackgroundColor(int color)
        {
            backgroundColor = color;
        }

        public int getBackgroundItemColor()
        {
            return(backgroundItemColor);
        }

        public void setBackgroundItemColor(int color)
        {
            backgroundItemColor = color;
        }

        public int getBackgroundItemSelectedColor()
        {
            return(backgroundItemSelectedColor);
        }

        public void setBackgroundItemSelectedColor(int color)
        {
            backgroundItemSelectedColor = color;
        }

        public int getTextColor()
        {
            return(textColor);
        }

        public void setTextColor(int color)
        {
            textColor = color;
        }

        public int getTextSelectedColor()
        {
            return(textSelectedColor);
        }

        public void setTextSelectedColor(int color)
        {
            textSelectedColor = color;
        }

        public Item getSelectedItem()
        {
            return((items != null) && (selectedIndex >= 0) && (selectedIndex < items.length) ? items[selectedIndex] : null);
        }

        public void setSelectedIndex(int index)
        {
            selectedIndex = index;
        }

        public boolean getUsingText()
        {
            return(usingText);
        }

        public boolean getUsingIcon3Only()
        {
            return(usingIcon3Only);
        }
    }

    private CustomAdapter currentAdapter;
    private OnItemSelectedListener itemSelectedListener;

    public IconSpinner(Context context, AttributeSet attrs, int defStyleAttr, int mode, Resources.Theme popupTheme)
    {
        super(context, attrs, defStyleAttr, mode, popupTheme);

        super.setOnItemSelectedListener(new OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                //update selected item and view
                currentAdapter.setSelectedIndex(position);
                view = currentAdapter.getView(position, view, parent);

                //if listener is set
                if(itemSelectedListener != null)
                {
                    //call it
                    itemSelectedListener.onItemSelected(parent, view, position, id);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {
                //if listener is set
                if(itemSelectedListener != null)
                {
                    //call it
                    itemSelectedListener.onNothingSelected(parent);
                }
            }
        });
    }
    public IconSpinner(Context context, AttributeSet attrs, int defStyleAttr, int mode)
    {
        this(context, attrs, defStyleAttr, mode, null);
    }
    public IconSpinner(Context context, AttributeSet attrs, int defStyleAttr)
    {
        this(context, attrs, defStyleAttr, -1);
    }
    public IconSpinner(Context context, AttributeSet attrs)
    {
        this(context, attrs, R.attr.spinnerStyle);
    }
    public IconSpinner(Context context, int mode)
    {
        this(context, null, R.attr.spinnerStyle, mode, null);
    }
    public IconSpinner(Context context)
    {
        this(context, -1);
    }

    @Override
    public void setOnItemSelectedListener(@Nullable OnItemSelectedListener listener)
    {
        itemSelectedListener = listener;
    }

    public void setAdapter(CustomAdapter adapter)
    {
        currentAdapter = adapter;
        SelectListInterface.loadAdapterIcons(getContext(), adapter);
        super.setAdapter(adapter);
    }

    public void setAllowAutoSelect(boolean allow)
    {
        //do nothing
    }

    public CustomAdapter getAdapter()
    {
        return(currentAdapter);
    }

    public int getBackgroundColor()
    {
        return(SelectListInterface.getBackgroundColor(currentAdapter));
    }

    public void setBackgroundColor(int color)
    {
        SelectListInterface.setBackgroundColor(currentAdapter, color);
        setPopupBackgroundDrawable(new ColorDrawable(color));
    }

    public void setBackgroundItemColor(int color)
    {
        SelectListInterface.setBackgroundItemColor(currentAdapter, color);
        setBackgroundItemSelectedColor(color);
    }

    public void setBackgroundItemSelectedColor(int color)
    {
        SelectListInterface.setBackgroundItemSelectedColor(currentAdapter, color);
    }

    public void setTextColor(int color)
    {
        SelectListInterface.setTextColor(currentAdapter, color);
        setTextSelectedColor(color);
    }

    public void setTextSelectedColor(int color)
    {
        SelectListInterface.setTextSelectedColor(currentAdapter, color);
    }

    public void setSelectedText(String value)
    {
        int index = SelectListInterface.setSelectedText(currentAdapter, value);

        if(index >= 0)
        {
            super.setSelection(index);
        }
    }

    public void setSelectedValue(Object value, Object defaultValue)
    {
        SelectListInterface.setSelectedValue(currentAdapter, value, defaultValue);
    }
    public boolean setSelectedValue(Object value)
    {
        int index = SelectListInterface.setSelectedValue(currentAdapter, value);
        boolean setSelection = (index >= 0);

        if(setSelection)
        {
            super.setSelection(index);
        }
        return(setSelection);
    }

    public Object getSelectedValue(Object defaultValue)
    {
        Object value;

        if(currentAdapter != null)
        {
            Object selected = getSelectedItem();
            value = (selected != null ? ((Item)selected).value : null);
        }
        else
        {
            value = super.getSelectedItem();
        }

        return(value != null ? value : defaultValue);
    }
}