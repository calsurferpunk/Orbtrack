package com.nikolaiapps.orbtrack;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;


public class RadioGroupPreference extends Preference
{
    //Valid class types for preference value
    private static abstract class ClassType
    {
        private static final int String = 0;
        private static final int Boolean = 1;
        private static final int Byte = 2;
        private static final int Integer = 3;
        private static final int Long = 4;
    }

    private Class<?> valueType;
    private String sharedName;
    private String titleText;
    private String pendingSetValue;
    private String[] itemTexts;
    private String[] itemValues;
    private RadioButton[] radioButtons;

    public RadioGroupPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes)
    {
        super(context, attrs, defStyleAttr, defStyleRes);

        this.setPersistent(false);
        this.setLayoutResource(R.layout.radio_group_preference_layout);

        int resId;
        TypedArray valueArray;
        Resources res = context.getResources();

        //set defaults
        valueType = String.class;
        pendingSetValue = null;
        radioButtons = new RadioButton[0];

        //if there are attributes, retrieve them
        if(attrs != null)
        {
            valueArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.RadioGroupPreference, 0, 0);
            sharedName = valueArray.getString(R.styleable.RadioGroupPreference_sharedName);
            titleText = valueArray.getString(R.styleable.RadioGroupPreference_titleText);
            resId = valueArray.getResourceId(R.styleable.RadioGroupPreference_itemTexts, 0);
            if(resId != 0)
            {
                itemTexts = res.getStringArray(resId);
            }
            resId = valueArray.getResourceId(R.styleable.RadioGroupPreference_itemValues, 0);
            if(resId != 0)
            {
                itemValues = res.getStringArray(resId);
            }
            setValueType(valueArray.getInt(R.styleable.RadioGroupPreference_valueType, 0));
            valueArray.recycle();
        }

        //if shared name not set
        if(sharedName == null || sharedName.trim().length() == 0)
        {
            //use default
            sharedName = "Settings";
        }

        //set to current preference value
        setSelectedValue(getPreferenceValue());
    }

    public RadioGroupPreference(Context context, AttributeSet attrs, int defStyleAttr)
    {
        this(context, attrs, defStyleAttr, 0);
    }

    public RadioGroupPreference(Context context, AttributeSet attrs)
    {
        this(context, attrs, 0);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder)
    {
        super.onBindViewHolder(holder);

        int index;
        int count;
        final Context context = this.getContext();
        final String preferenceName = this.getKey();
        final TextView titleView;
        final RadioGroup groupView;
        final SharedPreferences.Editor writeSettings = getWriteSettings(context);
        View rootView = holder.itemView;

        rootView.setClickable(false);

        //get displays
        titleView = rootView.findViewById(R.id.Radio_Group_Title);
        groupView = rootView.findViewById(R.id.Radio_Group_Group);

        //set displays
        titleView.setText(titleText);
        if(itemTexts != null && itemValues != null)
        {
            //get count that can be used
            count = Math.min(itemTexts.length, itemValues.length);

            //if buttons are not set yet
            if(radioButtons.length == 0 || radioButtons.length != count)
            {
                //set to shortest
                radioButtons = new RadioButton[count];
            }

            //go through texts/values
            for(index = 0; index < count; index++)
            {
                //create and add radio button
                RadioButton currentRadioButton = new RadioButton(new ContextThemeWrapper(context, R.style.RadioButton), null);
                String currentValue = itemValues[index];
                currentRadioButton.setTextColor(Globals.resolveColorID(context, R.attr.defaultTextColor));      //note: needed because bug setting color from R.style.RadioButton on dark/light theme switch
                currentRadioButton.setText(itemTexts[index]);
                currentRadioButton.setTag(currentValue);
                currentRadioButton.setChecked(pendingSetValue != null && pendingSetValue.equals(currentValue));
                currentRadioButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
                {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
                    {
                        //if checked
                        if(isChecked)
                        {
                            //go through all radio buttons
                            for(RadioButton currentRadioButton : radioButtons)
                            {
                                //if not the checked radio button
                                if(!buttonView.equals(currentRadioButton))
                                {
                                    //uncheck it
                                    //note: not sure why groupView/RadioGroup isn't doing this
                                    currentRadioButton.setChecked(false);
                                }
                            }

                            //save value
                            if(valueType == Boolean.class)
                            {
                                writeSettings.putBoolean(preferenceName, Boolean.parseBoolean(currentValue));
                            }
                            else if(valueType == Byte.class)
                            {
                                writeSettings.putInt(preferenceName, Byte.parseByte(currentValue));
                            }
                            else if(valueType == Integer.class)
                            {
                                writeSettings.putInt(preferenceName, Integer.parseInt(currentValue));
                            }
                            else if(valueType == Long.class)
                            {
                                writeSettings.putLong(preferenceName, Long.parseLong(currentValue));
                            }
                            else if(valueType == String.class)
                            {
                                writeSettings.putString(preferenceName, currentValue);
                            }
                            writeSettings.apply();
                        }
                    }
                });
                groupView.addView(currentRadioButton);

                //set radio button
                radioButtons[index] = currentRadioButton;
            }

            //clear any pending value
            pendingSetValue = null;
        }
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

    //Sets value type
    public void setValueType(int classType)
    {
        switch(classType)
        {
            case ClassType.Boolean:
                valueType = Boolean.class;
                break;

            case ClassType.Byte:
                valueType = Byte.class;
                break;

            case ClassType.Integer:
                valueType = Integer.class;
                break;

            case ClassType.Long:
                valueType = Long.class;
                break;

            default:
            case ClassType.String:
                valueType = String.class;
                break;
        }
    }

    //Gets current preference value as a string
    private String getPreferenceValue()
    {
        final Context context = this.getContext();
        final String preferenceName = this.getKey();
        final SharedPreferences readSettings = getPreferences(context);
        String value = null;

        if(valueType == Boolean.class)
        {
            value = String.valueOf(readSettings.getBoolean(preferenceName, false));
        }
        else if(valueType == Byte.class || valueType == Integer.class)
        {
            value = String.valueOf(readSettings.getInt(preferenceName, 0));
        }
        else if(valueType == Long.class)
        {
            value = String.valueOf(readSettings.getLong(preferenceName, 0));
        }
        else if(valueType == String.class)
        {
            value = String.valueOf(readSettings.getString(preferenceName, null));
        }

        return(value);
    }

    //Set selected value
    public void setSelectedValue(String value)
    {
        int index;

        //if no radio buttons yet
        if(radioButtons.length == 0)
        {
            //remember pending value
            pendingSetValue = value;
        }
        else
        {
            //go through radio buttons
            for(index = 0; index < radioButtons.length; index++)
            {
                //remember current radio button
                RadioButton currentRadioButton = radioButtons[index];

                //checked if value matches
                currentRadioButton.setChecked(currentRadioButton.getTag().equals(value));
            }

            //clear pending value
            pendingSetValue = null;
        }
    }
}
