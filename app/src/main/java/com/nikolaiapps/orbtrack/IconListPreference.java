package com.nikolaiapps.orbtrack;


import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceViewHolder;


public class IconListPreference extends CustomPreference
{
    private IconSpinner iconList;
    private IconSpinner.CustomAdapter adapter;
    private Object pendingSetValue;
    private Object pendingDefaultValue;
    private Object[] itemValues;

    public IconListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes)
    {
        super(context, attrs, defStyleAttr, defStyleRes);

        this.setPersistent(false);
        this.setLayoutResource(R.layout.icon_list_preference_layout);
    }
    public IconListPreference(Context context, AttributeSet attrs, int defStyleAttr)
    {
        this(context, attrs, defStyleAttr, 0);
    }
    @SuppressWarnings("unused")
    public IconListPreference(Context context, AttributeSet attrs)
    {
        this(context, attrs, 0);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder)
    {
        super.onBindViewHolder(holder);

        final Context context = this.getContext();
        final String preferenceName = this.getKey();
        final CharSequence summary = this.getSummary();
        final TextView titleView;
        final TextView summaryView;
        final LinearLayout listParentView;
        final SharedPreferences.Editor writeSettings = getWriteSettings(context);
        View rootView = holder.itemView;

        //set defaults
        if(itemValues == null)
        {
            itemValues = new Object[0];
        }

        rootView.setClickable(false);

        //get displays
        titleView = rootView.findViewById(R.id.Icon_List_Preference_Title);
        summaryView = rootView.findViewById(R.id.Icon_List_Preference_Summary);
        listParentView = rootView.findViewById(R.id.Icon_List_Preference_List_Parent);
        iconList = rootView.findViewById(R.id.Icon_List_Preference_List);

        //set displays
        titleView.setText(titleText);
        if(summary != null && summary.length() > 0)
        {
            summaryView.setText(summary);
            summaryView.setVisibility(View.VISIBLE);
        }
        if(iconList.getAdapter() != null && pendingSetValue == null)
        {
            pendingSetValue = iconList.getSelectedValue(null);
        }
        iconList.setAdapter(adapter);
        if(adapter != null && adapter.getUsingIcon3Only())
        {
            ViewGroup.LayoutParams listParams = iconList.getLayoutParams();
            ViewGroup.LayoutParams parentParams = listParentView.getLayoutParams();

            listParams.width = LinearLayout.LayoutParams.WRAP_CONTENT;
            parentParams.width = LinearLayout.LayoutParams.WRAP_CONTENT;

            iconList.setLayoutParams(listParams);
            listParentView.setLayoutParams(parentParams);
        }
        if(pendingSetValue != null)
        {
            if(pendingDefaultValue != null)
            {
                iconList.setSelectedValue(pendingSetValue, pendingDefaultValue);
            }
            else
            {
                iconList.setSelectedValue(pendingSetValue);
            }

            pendingSetValue = null;
            pendingDefaultValue = null;
        }
        iconList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                //if position is valid and change saving allowed
                if(position >= 0 && position < itemValues.length && callChangeListener(itemValues[position]))
                {
                    Object itemValue = itemValues[position];

                    //save value
                    if(itemValue instanceof Boolean)
                    {
                        writeSettings.putBoolean(preferenceName, (boolean)itemValue);
                    }
                    else if(itemValue instanceof Byte)
                    {
                        writeSettings.putInt(preferenceName, (byte)itemValue);
                    }
                    else if(itemValue instanceof Integer)
                    {
                        writeSettings.putInt(preferenceName, (int)itemValue);
                    }
                    else if(itemValue instanceof Long)
                    {
                        writeSettings.putLong(preferenceName, (long)itemValue);
                    }
                    else if(itemValue instanceof String)
                    {
                        writeSettings.putString(preferenceName, (String)itemValue);
                    }
                    writeSettings.apply();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    //Sets adapter
    public void setAdapter(Context context, Object[] items, Object[] values, int[] itemImageIds, String[] subTexts)
    {
        adapter = new IconSpinner.CustomAdapter(context, items, values, itemImageIds, subTexts);
        itemValues = adapter.getItemValues();
    }
    public void setAdapter(Context context, IconSpinner.Item[] items, boolean refresh)
    {
        Object selectedValue;

        adapter = new IconSpinner.CustomAdapter(context, items);
        itemValues = adapter.getItemValues();

        if(refresh)
        {
            selectedValue = iconList.getSelectedValue(null);
            iconList.setAdapter(adapter);
            iconList.setSelectedValue(selectedValue);
        }
    }
    public void setAdapter(Context context, IconSpinner.Item[] items)
    {
        setAdapter(context, items, false);
    }

    //Sets selected value
    public void setSelectedValue(Object value, Object defaultValue)
    {
        if(iconList != null)
        {
            pendingSetValue = null;
            pendingDefaultValue = null;
            iconList.setSelectedValue(value, defaultValue);
        }
        else
        {
            pendingSetValue = value;
            pendingDefaultValue = defaultValue;
        }
    }
    public void setSelectedValue(Object value)
    {
        pendingDefaultValue = null;

        if(iconList != null)
        {
            pendingSetValue = null;
            iconList.setSelectedValue(value);
        }
        else
        {
            pendingSetValue = value;
        }
    }
}
