package com.nikolaiapps.orbtrack;


import android.content.Context;
import android.graphics.Color;
import android.widget.AdapterView;


public interface SelectListInterface
{
    void setOnItemSelectedListener(AdapterView.OnItemSelectedListener listener);
    void setAdapter(IconSpinner.CustomAdapter adapter);
    void setAllowAutoSelect(boolean allow);
    IconSpinner.CustomAdapter getAdapter();
    int getBackgroundColor();
    void setBackgroundColor(int color);
    void setBackgroundItemColor(int color);
    void setBackgroundItemSelectedColor(int color);
    void setTextColor(int color);
    void setTextSelectedColor(int color);
    void setSelectedText(String value);
    void setSelectedValue(Object value, Object defaultValue);
    boolean setSelectedValue(Object value);
    int getSelectedItemPosition();
    Object getSelectedValue(Object defaultValue);
    void setEnabled(boolean enabled);

    static int getBackgroundColor(IconSpinner.CustomAdapter adapter)
    {
        return(adapter != null ? adapter.getBackgroundColor() : Color.TRANSPARENT);
    }

    static void setBackgroundColor(IconSpinner.CustomAdapter adapter, int color)
    {
        if(adapter != null)
        {
            adapter.setBackgroundColor(color);
        }
    }

    static void setBackgroundItemColor(IconSpinner.CustomAdapter adapter, int color)
    {
        if(adapter != null)
        {
            adapter.setBackgroundItemColor(color);
        }
    }

    static void setBackgroundItemSelectedColor(IconSpinner.CustomAdapter adapter, int color)
    {
        if(adapter != null)
        {
            adapter.setBackgroundItemSelectedColor(color);
        }
    }

    static void setTextColor(IconSpinner.CustomAdapter adapter, int color)
    {
        if(adapter != null)
        {
            adapter.setTextColor(color);
        }
    }

    static void setTextSelectedColor(IconSpinner.CustomAdapter adapter, int color)
    {
        if(adapter != null)
        {
            adapter.setTextSelectedColor(color);
        }
    }

    static int setSelectedText(IconSpinner.CustomAdapter adapter, String value)
    {
        int index;
        int intValue = Globals.tryParseInt(value);
        IconSpinner.Item[] items;

        if(adapter != null)
        {
            items = adapter.getItems();
            for(index = 0; index < items.length; index++)
            {
                IconSpinner.Item currentItem = items[index];
                if(currentItem != null)
                {
                    if((currentItem.text != null && currentItem.text.equals(value)) || (currentItem.value instanceof Integer && intValue == (int)currentItem.value))
                    {
                        adapter.setSelectedIndex(index);
                        return(index);
                    }
                }
            }
        }

        return(-1);
    }

    static int setSelectedValue(IconSpinner.CustomAdapter adapter, Object value)
    {
        int index;

        if(adapter != null)
        {
            index = adapter.getItemIndex(value);
            if(index >= 0)
            {
                adapter.setSelectedIndex(index);
                return(index);
            }
        }

        return(-1);
    }
    static int setSelectedValue(IconSpinner.CustomAdapter adapter, Object value, Object defaultValue)
    {
        int index = setSelectedValue(adapter, value);

        if(index < 0)
        {
            setSelectedValue(adapter, defaultValue);
        }

        return(index);
    }

    static void loadAdapterIcons(Context context, IconSpinner.CustomAdapter adapter)
    {
        int iconHeightPx;
        IconSpinner.Item[] adapterItems;

        //if context and adapter exist
        if(context != null)
        {
            //get icon height
            iconHeightPx = (int)Globals.dpToPixels(context, 32);

            //go through each item
            adapterItems = adapter.getItems();
            for(IconSpinner.Item currentItem : adapterItems)
            {
                //preload icon for text display
                currentItem.loadIcons(context, iconHeightPx);
            }
        }
    }
}
