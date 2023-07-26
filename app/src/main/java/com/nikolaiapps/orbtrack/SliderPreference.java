package com.nikolaiapps.orbtrack;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceViewHolder;
import java.util.Locale;


public class SliderPreference extends CustomPreference
{
    //Valid scale types for preference value
    private static abstract class ScaleType
    {
        static final int Integer = 0;
        static final int Percent = 1;
    }

    private int scaleType;
    private int minValue;
    private int maxValue;
    private PlayBar sliderView;

    public SliderPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes)
    {
        super(context, attrs, defStyleAttr, defStyleRes);

        boolean usingMaterial = Settings.getMaterialTheme(context);
        this.setPersistent(false);
        this.setLayoutResource(usingMaterial ? R.layout.slider_preference_material_layout : R.layout.slider_preference_layout);

        //set defaults
        scaleType = ScaleType.Integer;
        minValue = 0;
        maxValue = 100;

        //if there are attributes, retrieve them
        if(attrs != null)
        {
            try(@SuppressLint({"NewApi", "LocalSuppress"}) TypedArray valueArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.SliderPreference, 0, 0))
            {
                scaleType = valueArray.getInt(R.styleable.SliderPreference_scaleType, ScaleType.Integer);
                minValue = (int)valueArray.getFloat(R.styleable.SliderPreference_minValue, 0);
                maxValue = (int)valueArray.getFloat(R.styleable.SliderPreference_maxValue, 100);
                valueArray.recycle();
            }
            catch(NoSuchMethodError noMethod)
            {
                //do nothing
            }
            catch(Exception ex)
            {
                //do nothing
            }
        }
    }

    public SliderPreference(Context context, AttributeSet attrs, int defStyleAttr)
    {
        this(context, attrs, defStyleAttr, 0);
    }

    @SuppressWarnings("unused")
    public SliderPreference(Context context, AttributeSet attrs)
    {
        this(context, attrs, 0);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder)
    {
        super.onBindViewHolder(holder);

        final Context context = this.getContext();
        final String preferenceName = this.getKey();
        final TextView titleView;
        final ViewGroup rootView;
        final SharedPreferences.Editor writeSettings = getWriteSettings(context);

        //get displays
        rootView = (ViewGroup)holder.itemView;
        titleView = rootView.findViewById(R.id.Slider_Preference_Title);
        sliderView = rootView.findViewById(R.id.Slider_Preference_Bar);

        //set displays
        rootView.setClickable(false);
        titleView.setText(titleText);
        sliderView.setMin(minValue);
        sliderView.setMax(maxValue);
        sliderView.setPlayIndexIncrementUnits(1);
        sliderView.setPlayActivity(null);
        sliderView.setOnSeekChangedListener(new PlayBar.OnPlayBarChangedListener()
        {
            @Override
            public void onProgressChanged(PlayBar seekBar, int progressValue, double subProgressPercent, boolean fromUser)
            {
                String scaleFormat = null;

                //save value and get format
                switch(scaleType)
                {
                    case ScaleType.Integer:
                        writeSettings.putInt(preferenceName, progressValue).apply();
                        scaleFormat = "%3d";
                        break;

                    case ScaleType.Percent:
                        writeSettings.putFloat(preferenceName, progressValue / 100f).apply();
                        scaleFormat = "%3d%%";
                        break;
                }

                //if format set
                if(scaleFormat != null)
                {
                    //update text
                    seekBar.setScaleText(String.format(Locale.US, scaleFormat, progressValue));
                }
            }
        });
        switch(scaleType)
        {
            case ScaleType.Integer:
                sliderView.setValue(Settings.getPreferenceInt(context, preferenceName), true);
                break;

            case ScaleType.Percent:
                sliderView.setValue((int)(Settings.getPreferenceFloat(context, preferenceName) * 100), true);
                break;
        }
    }

    public void setRange(int min, int max)
    {
        minValue = min;
        maxValue = max;

        //if slider exists
        if(sliderView != null)
        {
            sliderView.setMin(minValue);
            sliderView.setMax(maxValue);
        }
    }
}
