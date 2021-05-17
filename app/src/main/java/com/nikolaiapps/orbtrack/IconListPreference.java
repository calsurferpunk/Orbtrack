package com.nikolaiapps.orbtrack;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import java.util.ArrayList;
import java.util.TimeZone;


public class IconListPreference extends Preference
{
    private String sharedName;
    private String titleText;
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

        TypedArray valueArray;

        //if there are attributes, retrieve them
        if(attrs != null)
        {
            valueArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.IconListPreference, 0, 0);
            sharedName = valueArray.getString(R.styleable.IconListPreference_sharedName);
            titleText = valueArray.getString(R.styleable.IconListPreference_titleText);
            valueArray.recycle();
        }

        //if shared name not set
        if(sharedName == null || sharedName.trim().length() == 0)
        {
            //use default
            sharedName = "Settings";
        }
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
    public void onBindViewHolder(PreferenceViewHolder holder)
    {
        super.onBindViewHolder(holder);

        final Context context = this.getContext();
        final String preferenceName = this.getKey();
        final CharSequence summary = this.getSummary();
        final TextView titleView;
        final TextView summaryView;
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
        iconList = rootView.findViewById(R.id.Icon_List_Preference_Layout);

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

    //Gets shared preferences
    private SharedPreferences getPreferences(Context context)
    {
        return(context.getSharedPreferences(sharedName, Context.MODE_PRIVATE));
    }

    //Gets write settings
    private SharedPreferences.Editor getWriteSettings(Context context)
    {
        return(getPreferences(context).edit());
    }

    //Sets adapter
    public void setAdapter(Context context, Object[] items, Object[] values, int[] itemImageIds, String[] subTexts)
    {
        adapter = new IconSpinner.CustomAdapter(context, items, values, itemImageIds, subTexts);
        itemValues = adapter.getItemValues();
    }
    public void setAdapter(Context context, Object[] items, int[] itemImageIds, String[] subTexts)
    {
        setAdapter(context, items, items, itemImageIds, subTexts);
    }
    public void setAdapter(Context context, Object[] items, int[] itemImageIds)
    {
        adapter = new IconSpinner.CustomAdapter(context, items, itemImageIds);
        itemValues = adapter.getItemValues();
    }
    public void setAdapter(Context context, Object[] items)
    {
        adapter = new IconSpinner.CustomAdapter(context, items);
        itemValues = adapter.getItemValues();
    }
    public void setAdapter(Context context, IconSpinner.Item[] items)
    {
        adapter = new IconSpinner.CustomAdapter(context, items);
        itemValues = adapter.getItemValues();
    }
    public void setAdapter(Context context, ArrayList<TimeZone> zones)
    {
        adapter = new IconSpinner.CustomAdapter(context, zones);
        itemValues = adapter.getItemValues();
    }

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
