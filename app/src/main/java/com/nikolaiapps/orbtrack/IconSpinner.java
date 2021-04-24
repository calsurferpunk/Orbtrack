package com.nikolaiapps.orbtrack;


import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatSpinner;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.TimeZone;


public class IconSpinner extends AppCompatSpinner
{
    public static class Item
    {
        float rotate;
        Drawable icon1;
        Drawable icon2;
        Drawable icon3;
        String text;
        String subText;
        Object value;

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
            icon1 = null;
            icon2 = null;
            icon3 = null;
            text = txt;
            subText = null;
            value = val;
        }
        public Item(String txt, String sbTxt, Object val)
        {
            this(txt, val);
            subText = sbTxt;
        }
        public Item(Drawable icn3, String txt, Object val)
        {
            this(txt, val);
            icon3 = icn3;
        }
        public Item(Drawable icn3, String txt, Object val, float rotateAngle)
        {
            this(icn3, txt, val);
            rotate = rotateAngle;
        }
        public Item(Drawable icn3, String txt, Object val, String sbTxt)
        {
            this(icn3, txt, val);
            subText = sbTxt;
        }
        public Item(Drawable icn1, Drawable icn2, Drawable icn3, String txt, Object val)
        {
            this(icn3, txt, val);
            icon1 = icn1;
            icon2 = icn2;
        }
        public Item(Context context, int colorID, String txt, Object val)
        {
            this(txt, val);
            icon3 = new ColorDrawable(ContextCompat.getColor(context, colorID));
        }
    }

    public static class CustomAdapter extends BaseAdapter
    {
        private int backgroundColor;
        private int backgroundItemColor;
        private int textColor;
        private boolean usingIcon1;
        private boolean usingIcon3;
        private LayoutInflater listInflater;
        private final Item[] items;

        private void BaseConstructor(Context context)
        {
            listInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            backgroundColor = Globals.resolveColorID(context, R.attr.viewPagerBackground);
            backgroundItemColor = Globals.resolveColorID(context, R.attr.pageBackground);
            textColor = Globals.resolveColorID(context, R.attr.defaultTextColor);

            updateUsingIcons();
        }

        public CustomAdapter(Context context, Item[] itms)
        {
            ArrayList<Item> usedItems = new ArrayList<>(0);

            for(Item currentItem : itms)
            {
                if(currentItem != null)
                {
                    usedItems.add(currentItem);
                }
            }
            items = usedItems.toArray(new Item[0]);

            BaseConstructor(context);
        }
        public CustomAdapter(Context context, Object[] itms)
        {
            int index;
            boolean useFloat = (itms.length > 0 && itms[0] instanceof Float);

            items = new Item[itms.length];
            for(index = 0; index < items.length; index++)
            {
                items[index] = new Item((useFloat ? Globals.getNumberString((Float)itms[index], 0) : itms[index].toString()), itms[index]);
            }

            BaseConstructor(context);
        }
        public CustomAdapter(Context context, Object[] itms, int[] itmImgIds)
        {
            int index;

            items = new Item[itms.length];
            for(index = 0; index < items.length; index++)
            {
                items[index] = new Item(Globals.getDrawable(context, itmImgIds[index], false), itms[index].toString(), itms[index]);
            }

            BaseConstructor(context);
        }
        public CustomAdapter(Context context, Object[] itms, Object[] vals, int[] itmImgIds, String[] sbTxts)
        {
            int index;

            items = new Item[itms.length];
            for(index = 0; index < items.length; index++)
            {
                items[index] = new Item(Globals.getDrawable(context, itmImgIds[index], false), itms[index].toString(), vals[index], (sbTxts != null ? sbTxts[index] : null));
            }

            BaseConstructor(context);
        }
        public CustomAdapter(Context context, Object[] itms, int[] itmImgIds, String[] sbTxts)
        {
            this(context, itms, itms, itmImgIds, sbTxts);
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
        public CustomAdapter(Context context, Database.DatabaseSatellite[] satellites, boolean forceWhiteIcons)
        {
            int index;
            boolean useIcons;
            boolean isLocation;

            //go through each satellite
            items = new Item[satellites.length];
            for(index = 0; index < satellites.length; index++)
            {
                //remember current satellite and set item
                Database.DatabaseSatellite currentSat = satellites[index];
                isLocation = (currentSat.norad == Universe.IDs.CurrentLocation);
                useIcons = (currentSat.norad != Integer.MAX_VALUE && !isLocation);
                Drawable[] ownerIcons = (useIcons ? Settings.getOwnerIcons(context, currentSat.norad, currentSat.ownerCode) : new Drawable[]{Globals.getDrawable(context, (isLocation ? R.drawable.ic_my_location_black : R.drawable.ic_search_black), R.color.white)});
                items[index] = new Item(ownerIcons[0], (ownerIcons.length > 1 ? ownerIcons[1] : null), (useIcons ? Globals.getOrbitalIcon(context, MainActivity.getObserver(), currentSat.norad, currentSat.orbitalType, forceWhiteIcons) : null), currentSat.getName(), currentSat.norad);
            }

            BaseConstructor(context);
        }
        public CustomAdapter(Context context, Database.DatabaseSatellite[] satellites)
        {
            this(context, satellites, false);
        }

        private void updateUsingIcons()
        {
            int index;

            //not using until found
            usingIcon1 = usingIcon3 = false;

            //if items are set
            if(items != null)
            {
                //go through items while not using either icons
                for(index = 0; index < items.length && (!usingIcon1 || !usingIcon3); index++)
                {
                    //remember current item
                    Item currentItem = items[index];

                    //update if using icons
                    usingIcon1 = usingIcon1 || (currentItem.icon1 != null);
                    usingIcon3 = usingIcon3 || (currentItem.icon3 != null);
                }
            }
        }

        @Override
        public int getCount()
        {
            return(items.length);
        }

        @Override
        public Object getItem(int position)
        {
            return(items[position]);
        }

        @Override
        public long getItemId(int position)
        {
            return(position);
        }

        public Item[] getItems()
        {
            return(items);
        }

        public Object[] getItemValues()
        {
            int position;
            Object[] values = new Object[items.length];

            //go through each time
            for(position = 0; position < values.length; position++)
            {
                //copy value
                values[position] = items[position].value;
            }

            //return values
            return(values);
        }

        public Object getItemValue(int position)
        {
            return(items[position].value);
        }

        public int getItemIndex(Object val)
        {
            int index;

            //go through each item
            for(index = 0; index < items.length; index++)
            {
                //if a match
                if(items[index].value.equals(val))
                {
                    //return index
                    return(index);
                }
            }

            //not found
            return(-1);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            AppCompatImageView itemImage1;
            AppCompatImageView itemImage2;
            AppCompatImageView itemImage3;
            TextView itemText;
            TextView itemSubText;
            Item currentItem = items[position];

            //set view
            if(convertView == null)
            {
                convertView = listInflater.inflate(R.layout.icon_spinner_item, parent, false);
            }
            convertView.setBackgroundColor(backgroundColor);
            convertView.findViewById(R.id.Spinner_Item_Background).setBackgroundColor(backgroundItemColor);

            //get views
            itemImage1 = convertView.findViewById(R.id.Spinner_Item_Image1);
            itemImage2 = convertView.findViewById(R.id.Spinner_Item_Image2);
            itemImage3 = convertView.findViewById(R.id.Spinner_Item_Image3);
            itemText = convertView.findViewById(R.id.Spinner_Item_Text);
            itemSubText = convertView.findViewById(R.id.Spinner_Item_Sub_Text);

            //update displays
            itemImage1.setBackgroundDrawable(currentItem.icon1);
            if(!usingIcon1)
            {
                itemImage1.setVisibility(View.GONE);
            }
            itemImage2.setBackgroundDrawable(currentItem.icon2);
            if(currentItem.icon2 != null)
            {
                itemImage2.setVisibility(View.VISIBLE);
                Globals.setLayoutWidth(itemImage1, itemImage2.getLayoutParams().width);
            }
            itemImage3.setBackgroundDrawable(currentItem.icon3);
            if(currentItem.rotate != 0)
            {
                itemImage3.setRotation(currentItem.rotate);
            }
            if(!usingIcon3)
            {
                itemImage3.setVisibility(View.GONE);
            }
            itemText.setText(currentItem.text);
            itemText.setTextColor(textColor);
            if(currentItem.subText != null)
            {
                itemSubText.setText(currentItem.subText);
                itemSubText.setTextColor(textColor);
            }
            else
            {
                itemSubText.setVisibility(View.GONE);
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

        public void setBackgroundItemColor(int color)
        {
            backgroundItemColor = color;
        }

        public void setTextColor(int color)
        {
            textColor = color;
        }
    }

    private CustomAdapter currentAdapter;

    public IconSpinner(Context context)
    {
        super(context);
    }
    public IconSpinner(Context context, int mode)
    {
        super(context, mode);
    }
    public IconSpinner(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }
    public IconSpinner(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
    }
    public IconSpinner(Context context, AttributeSet attrs, int defStyleAttr, int mode)
    {
        super(context, attrs, defStyleAttr, mode);
    }
    public IconSpinner(Context context, AttributeSet attrs, int defStyleAttr, int mode, Resources.Theme popupTheme)
    {
        super(context, attrs, defStyleAttr, mode, popupTheme);
    }

    public void setAdapter(CustomAdapter adapter)
    {
        currentAdapter = adapter;
        super.setAdapter(adapter);
    }

    public int getBackgroundColor()
    {
        return(currentAdapter != null ? currentAdapter.getBackgroundColor() : Color.TRANSPARENT);
    }

    public void setBackgroundColor(int color)
    {
        if(currentAdapter != null)
        {
            currentAdapter.setBackgroundColor(color);
        }
    }

    public void setBackgroundItemColor(int color)
    {
        if(currentAdapter != null)
        {
            currentAdapter.setBackgroundItemColor(color);
        }
    }

    public void setTextColor(int color)
    {
        if(currentAdapter != null)
        {
            currentAdapter.setTextColor(color);
        }
    }

    public void setSelectedText(String value)
    {
        int index;
        Item[] items;

        if(currentAdapter != null)
        {
            items = currentAdapter.getItems();
            for(index = 0; index < items.length; index++)
            {
                if(items[index].text.equals(value))
                {
                    super.setSelection(index);
                    return;
                }
            }
        }
    }

    public void setSelectedValue(Object value, Object defaultValue)
    {
        if(!setSelectedValue(value))
        {
            setSelectedValue(defaultValue);
        }
    }
    public boolean setSelectedValue(Object value)
    {
        int index;

        if(currentAdapter != null)
        {
            index = currentAdapter.getItemIndex(value);
            if(index >= 0)
            {
                super.setSelection(index);
                return(true);
            }
        }

        return(false);
    }

    Object getSelectedValue(Object defaultValue)
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
