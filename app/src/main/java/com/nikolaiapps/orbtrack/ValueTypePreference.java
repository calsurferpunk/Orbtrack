package com.nikolaiapps.orbtrack;


import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


public class ValueTypePreference extends CustomPreference
{
    //Valid class types for preference value
    protected static abstract class ClassType
    {
        protected static final int String = 0;
        protected static final int Boolean = 1;
        protected static final int Byte = 2;
        protected static final int Integer = 3;
        protected static final int Long = 4;
        protected static final int Float = 5;
    }

    protected Class<?> valueType;

    public ValueTypePreference(@NonNull Context context, @Nullable @org.jetbrains.annotations.Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes)
    {
        super(context, attrs, defStyleAttr, defStyleRes);

        this.setPersistent(false);

        //set defaults
        valueType = Integer.class;
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

            case ClassType.Float:
                valueType = Float.class;
                break;

            default:
            case ClassType.String:
                valueType = String.class;
                break;
        }
    }

    //Saves the value
    protected void saveValue(String currentValue)
    {
        String preferenceName = this.getKey();
        SharedPreferences.Editor writeSettings = getWriteSettings(this.getContext());

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
        else if(valueType == Float.class)
        {
            writeSettings.putFloat(preferenceName, Float.parseFloat(currentValue));
        }
        writeSettings.apply();
    }

    //Gets current preference value as a string
    protected String getPreferenceValue()
    {
        final Context context = this.getContext();
        final String preferenceName = this.getKey();
        String value = null;

        if(valueType == Boolean.class)
        {
            value = String.valueOf(Settings.getPreferenceBoolean(context, preferenceName));
        }
        else if(valueType == Byte.class || valueType == Integer.class)
        {
            value = String.valueOf(Settings.getPreferenceInt(context, preferenceName));
        }
        else if(valueType == Long.class)
        {
            value = String.valueOf(Settings.getPreferenceLong(context, preferenceName));
        }
        else if(valueType == String.class)
        {
            value = String.valueOf(Settings.getPreferenceString(context, preferenceName));
        }

        return(value);
    }
}
