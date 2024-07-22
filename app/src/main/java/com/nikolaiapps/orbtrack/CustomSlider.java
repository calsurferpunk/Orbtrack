package com.nikolaiapps.orbtrack;


import android.content.Context;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.slider.Slider;


public class CustomSlider extends Slider
{
    public CustomSlider(@NonNull Context context, @Nullable @org.jetbrains.annotations.Nullable AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
    }
    public CustomSlider(@NonNull Context context, @Nullable @org.jetbrains.annotations.Nullable AttributeSet attrs)
    {
        this(context, attrs, R.attr.sliderStyle);
    }
    public CustomSlider(@NonNull Context context)
    {
        this(context, null);
    }

    @Override
    public void setValue(float value)
    {
        float minValue = getValueFrom();
        float maxValue = getValueTo();

        //keep value within range
        if(value < minValue)
        {
            value = minValue;
        }
        else if(value > maxValue)
        {
            value = maxValue;
        }

        try
        {
            //try to set value
            super.setValue(value);
        }
        catch(Exception ex)
        {
            //do nothing
        }
    }

    public void setRange(float minValue, float maxValue)
    {
        if(minValue < maxValue)
        {
            setValueFrom(minValue);
            setValueTo(maxValue);
        }
    }
}
