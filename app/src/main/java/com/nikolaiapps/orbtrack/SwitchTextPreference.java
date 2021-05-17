package com.nikolaiapps.orbtrack;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.widget.SwitchCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;


public class SwitchTextPreference extends Preference
{
    private boolean showSwitch;
    private float minValue;
    private float maxValue;
    private String sharedName;
    private String switchKey;
    private String titleText;
    private String suffixText;
    private String enabledValueText;
    private String disabledValueText;
    private EditText valueView;
    private TextView noSwitchTitle;
    private SwitchCompat switchView;

    public SwitchTextPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes)
    {
        super(context, attrs, defStyleAttr, defStyleRes);

        this.setPersistent(false);
        this.setLayoutResource(R.layout.switch_text_preference_layout);

        TypedArray valueArray;

        //set defaults
        showSwitch = true;
        switchKey = null;
        minValue = 0;
        maxValue = 100;
        enabledValueText = disabledValueText = null;

        //if there are attributes, retrieve them
        if(attrs != null)
        {
            valueArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.SwitchTextPreference, 0, 0);
            showSwitch = valueArray.getBoolean(R.styleable.SwitchTextPreference_showSwitch, true);
            switchKey = valueArray.getString(R.styleable.SwitchTextPreference_switchKey);
            minValue = valueArray.getFloat(R.styleable.SwitchTextPreference_minValue, 0);
            maxValue = valueArray.getFloat(R.styleable.SwitchTextPreference_maxValue, 100);
            sharedName = valueArray.getString(R.styleable.SwitchTextPreference_sharedName);
            titleText = valueArray.getString(R.styleable.SwitchTextPreference_titleText);
            suffixText = valueArray.getString(R.styleable.SwitchTextPreference_suffixText);
            valueArray.recycle();
        }

        //if shared name not set
        if(sharedName == null || sharedName.trim().length() == 0)
        {
            //use default
            sharedName = "Settings";
        }
    }

    public SwitchTextPreference(Context context, AttributeSet attrs, int defStyleAttr)
    {
        this(context, attrs, defStyleAttr, 0);
    }

    @SuppressWarnings("unused")
    public SwitchTextPreference(Context context, AttributeSet attrs)
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
        final TextView summaryView;
        final TextView valueSuffixView;
        final ViewGroup rootView;
        final SharedPreferences.Editor writeSettings = getWriteSettings(context);
        final CompoundButton.OnCheckedChangeListener checkedChangeListener;

        //get displays
        rootView = (ViewGroup)holder.itemView;
        valueView = rootView.findViewById(R.id.Switch_Text_Preference_Value_Text);
        summaryView = rootView.findViewById(R.id.Switch_Text_Preference_Summary);
        valueSuffixView = rootView.findViewById(R.id.Switch_Text_Preference_Value_Suffix);
        noSwitchTitle = rootView.findViewById(R.id.Switch_Text_Preference_No_Switch_Title);
        switchView = rootView.findViewById(R.id.Switch_Text_Preference_Switch);

        //set displays
        rootView.setClickable(false);
        valueView.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s)
            {
                String stringValue = s.toString();
                float value = Globals.tryParseFloat(stringValue);
                boolean save = (value != Float.MAX_VALUE && value >= minValue && value <= maxValue);

                //if saving
                if(save)
                {
                    //save setting
                    writeSettings.putFloat(preferenceName, value).apply();
                }

                //set/clear error
                valueView.setError(save ? null : context.getResources().getString(R.string.title_invalid));

                //if using switch, it exists, and not checked
                if(showSwitch && switchView != null && !switchView.isChecked())
                {
                    //remember disabled value
                    disabledValueText = stringValue;
                }
            }
        });
        valueSuffixView.setText(suffixText);
        if(summary != null && summary.length() > 0)
        {
            summaryView.setText(summary);
            summaryView.setVisibility(View.VISIBLE);
        }
        if(showSwitch)
        {
            switchView.setText(titleText);
            switchView.setChecked(Settings.getPreferenceBoolean(context, switchKey));
            checkedChangeListener = new CompoundButton.OnCheckedChangeListener()
            {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
                {
                    //if showing switch and value exists
                    if(showSwitch && valueView != null)
                    {
                        //update text and state
                        updateValueText(isChecked);
                        valueView.setEnabled(!isChecked);

                        //if saving switch
                        if(switchKey != null)
                        {
                            //save setting
                            writeSettings.putBoolean(switchKey, isChecked).apply();
                        }
                    }
                }
            };
            switchView.setOnCheckedChangeListener(checkedChangeListener);
            checkedChangeListener.onCheckedChanged(switchView, switchView.isChecked());
        }
        else
        {
            noSwitchTitle.setText(titleText);
            updateValueText(false);
        }
        setShowSwitch(showSwitch);
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

    //Updates value text based on switch state
    private void updateValueText(boolean switchChecked)
    {
        //use enabled text is switch checked, else disabled text
        valueView.setText(switchChecked ? enabledValueText : disabledValueText);
    }

    //Sets value text
    public void setValueText(String text)
    {
        enabledValueText = disabledValueText = text;
    }
    public void setValueText(String enabledText, String disabledText)
    {
        enabledValueText = enabledText;
        disabledValueText = disabledText;
    }

    //Sets if showing switch
    public void setShowSwitch(boolean show)
    {
        showSwitch = show;

        //if views exist
        if(noSwitchTitle != null)
        {
            //hide if showing switch
            noSwitchTitle.setVisibility(showSwitch ? View.GONE : View.VISIBLE);
        }
        if(switchView != null)
        {
            //show if showing switch
            switchView.setVisibility(showSwitch ? View.VISIBLE : View.GONE);
        }
    }
}
