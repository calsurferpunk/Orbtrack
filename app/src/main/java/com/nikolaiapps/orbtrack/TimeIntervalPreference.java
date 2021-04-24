package com.nikolaiapps.orbtrack;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import java.util.Arrays;
import java.util.Calendar;


public class TimeIntervalPreference extends Preference
{
    public interface OnPreferenceChangedListener
    {
        void onPreferenceChanged(TimeIntervalPreference preference, Object newValue);
    }

    //Class to report combined interval values
    public static class IntervalValues
    {
        int hour;
        int minute;
        long intervalMs;

        public IntervalValues(int hour, int minute, long intervalMs)
        {
            this.hour = hour;
            this.minute = minute;
            this.intervalMs = intervalMs;
        }
    }

    //Frequency constants
    private static final long MsPerDay = (long)Calculations.MsPerDay;
    private static final long MsPer3Days = MsPerDay * 3;
    private static final long MsPer4Weeks = MsPerDay * 28;
    private static String[] frequencyItems = null;
    private static final Long[] FrequencyValues = new Long[]{MsPerDay, MsPerDay * 2, MsPer3Days, MsPerDay * 5, MsPerDay * 7, MsPerDay * 14, MsPer4Weeks};

    private String hourKey;
    private String minuteKey;
    private String intervalKey;
    private String sharedName;
    private IconSpinner frequencyList;
    private TimeInputView timeView;
    private OnPreferenceChangedListener changedListener;

    public TimeIntervalPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes)
    {
        super(context, attrs, defStyleAttr, defStyleRes);

        this.setPersistent(false);
        this.setLayoutResource(R.layout.time_interval_preference_layout);

        TypedArray valueArray;
        Resources res = context.getResources();

        //set defaults
        hourKey = null;
        minuteKey = null;
        intervalKey = null;
        changedListener = null;

        //if frequency items not set yet
        if(frequencyItems == null)
        {
            //set frequency items
            frequencyItems = new String[]
            {
                res.getQuantityString(R.plurals.text_days, 1, 1),
                res.getQuantityString(R.plurals.text_days, 2, 2),
                res.getQuantityString(R.plurals.text_days, 3, 3),
                res.getQuantityString(R.plurals.text_days, 5, 5),
                res.getQuantityString(R.plurals.text_weeks, 1, 1),
                res.getQuantityString(R.plurals.text_weeks, 2, 2),
                res.getQuantityString(R.plurals.text_weeks, 4, 4)
            };
        }

        //if there are attributes, retrieve them
        if(attrs != null)
        {
            valueArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.TimeIntervalPreference, 0, 0);
            hourKey = valueArray.getString(R.styleable.TimeIntervalPreference_hourKey);
            minuteKey = valueArray.getString(R.styleable.TimeIntervalPreference_minuteKey);
            intervalKey = valueArray.getString(R.styleable.TimeIntervalPreference_intervalKey);
            sharedName = valueArray.getString(R.styleable.TimeIntervalPreference_sharedName);
            valueArray.recycle();
        }

        //if shared name not set
        if(sharedName == null || sharedName.trim().length() == 0)
        {
            //use default
            sharedName = "Settings";
        }
    }
    public TimeIntervalPreference(Context context, AttributeSet attrs, int defStyleAttr)
    {
        this(context, attrs, defStyleAttr, 0);
    }
    public TimeIntervalPreference(Context context, AttributeSet attrs)
    {
        this(context, attrs, 0);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder)
    {
        super.onBindViewHolder(holder);

        final Context context = this.getContext();
        final Calendar now = Calendar.getInstance();
        final int currentHour = now.get(Calendar.HOUR_OF_DAY);
        final int currentMinute = now.get(Calendar.MINUTE);
        int index;
        View rootView = holder.itemView;
        SharedPreferences readSettings = getPreferences(context);
        IconSpinner.CustomAdapter frequencyAdapter = new IconSpinner.CustomAdapter(context, frequencyItems);

        rootView.setClickable(false);

        //set displays
        frequencyList = rootView.findViewById(R.id.Time_Interval_Preference_List);
        frequencyList.setAdapter(frequencyAdapter);
        if(intervalKey != null)
        {
            //if a valid index
            index = Arrays.asList(FrequencyValues).indexOf(readSettings.getLong(intervalKey, 0));
            if(index >= 0 && index < frequencyItems.length)
            {
                //set value
                frequencyList.setSelectedValue(frequencyItems[index]);
            }
        }
        frequencyList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                //if interval key exists position, is valid, and change saving allowed
                if(intervalKey != null && position >= 0 && position < FrequencyValues.length && callChangeListener(new IntervalValues(getHour(), getMinute(), FrequencyValues[position])))
                {
                    //save interval
                    getWriteSettings(context).putLong(intervalKey, FrequencyValues[position]).apply();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        timeView = rootView.findViewById(R.id.Time_Interval_Preference_Time_Text);
        if(hourKey != null || minuteKey != null)
        {
            timeView.setTime((hourKey != null ? readSettings.getInt(hourKey, currentHour) : currentHour), (minuteKey != null ? readSettings.getInt(minuteKey, currentMinute) : currentMinute));
        }
        timeView.setOnTimeSetListener(new TimeInputView.OnTimeSetListener()
        {
            @Override
            public void onTimeSet(TimeInputView timeView, int hour, int minute)
            {
                IntervalValues newValue = new IntervalValues(hour, minute, getIntervalMs());

                //if change saving allowed
                if(callChangeListener(newValue))
                {
                    SharedPreferences.Editor writeSettings = getWriteSettings(context);

                    //if keys exist
                    if(hourKey != null)
                    {
                        //save hour
                        writeSettings.putInt(hourKey, hour);
                    }
                    if(minuteKey != null)
                    {
                        //save minute
                        writeSettings.putInt(minuteKey, minute);
                    }
                    writeSettings.apply();

                    //if changed listener is set
                    if(changedListener != null)
                    {
                        //call it
                        changedListener.onPreferenceChanged(TimeIntervalPreference.this, newValue);
                    }
                }
            }
        });
    }

    //Sets on changed listener
    public void setOnPreferenceChangedListener(OnPreferenceChangedListener listener)
    {
        changedListener = listener;
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

    //Gets the interval value
    public long getIntervalMs()
    {
        //if index valid, return value
        int position = frequencyList.getSelectedItemPosition();
        return((position >= 0 && position < FrequencyValues.length) ? FrequencyValues[position] : 0);
    }

    //Sets interval value
    public void setIntervalMs(long timeMs)
    {
        //if a valid index
        int index = Arrays.asList(FrequencyValues).indexOf(timeMs);
        if(index >= 0 && index < frequencyItems.length)
        {
            //set value
            frequencyList.setSelectedValue(frequencyItems[index]);
        }
    }

    //Gets the time hour value
    public int getHour()
    {
        return(timeView.getHour());
    }

    //Gets the time minute value
    public int getMinute()
    {
        return(timeView.getMinute());
    }

    //Sets the time value
    public void setTime(int hour, int minute)
    {
        timeView.setTime(hour, minute);
    }
}
