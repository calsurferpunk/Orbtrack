package com.nikolaiapps.orbtrack;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceViewHolder;
import com.google.android.material.materialswitch.MaterialSwitch;


public class SwitchTextPreference extends ValueTypePreference
{
    private boolean showSwitch;
    private boolean switchEnabled;
    private boolean switchChecked;
    private boolean reverseEnabled;
    private float minValue;
    private float maxValue;
    private String switchKey;
    private String suffixText;
    private String enabledValueText;
    private String disabledValueText;
    private EditText valueView;
    private MaterialSwitch switchMaterialView;

    public SwitchTextPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes)
    {
        super(context, attrs, defStyleAttr, defStyleRes);

        this.setLayoutResource(R.layout.switch_text_preference_layout);

        //set defaults
        valueType = Integer.class;
        showSwitch = switchEnabled = true;
        switchChecked = reverseEnabled = false;
        switchKey = null;
        minValue = 0;
        maxValue = 100;
        enabledValueText = disabledValueText = null;

        //if there are attributes, retrieve them
        if(attrs != null)
        {
            try(@SuppressLint({"NewApi", "LocalSuppress"}) TypedArray valueArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.SwitchTextPreference, 0, 0))
            {
                showSwitch = valueArray.getBoolean(R.styleable.SwitchTextPreference_showSwitch, true);
                reverseEnabled = valueArray.getBoolean(R.styleable.SwitchTextPreference_reverseEnabled, false);
                switchKey = valueArray.getString(R.styleable.SwitchTextPreference_switchKey);
                minValue = valueArray.getFloat(R.styleable.SwitchTextPreference_minValue, 0);
                maxValue = valueArray.getFloat(R.styleable.SwitchTextPreference_maxValue, 100);
                suffixText = valueArray.getString(R.styleable.SwitchTextPreference_suffixText);
                setValueType(valueArray.getInt(R.styleable.SwitchTextPreference_valueType, ClassType.Integer));
                valueArray.recycle();
            }
            catch(NoSuchMethodError | Exception noMethod)
            {
                //do nothing
            }
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

    /** @noinspection SizeReplaceableByIsEmpty*/
    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder)
    {
        super.onBindViewHolder(holder);

        final Context context = this.getContext();
        final CharSequence summary = this.getSummary();
        final TextView summaryView;
        final TextView valueSuffixView;
        final TextView noSwitchTitle;
        final ViewGroup rootView;
        final CompoundButton.OnCheckedChangeListener checkedChangeListener;

        //get displays
        rootView = (ViewGroup)holder.itemView;
        valueView = rootView.findViewById(R.id.Switch_Text_Preference_Value_Text);
        summaryView = rootView.findViewById(R.id.Switch_Text_Preference_Summary);
        valueSuffixView = rootView.findViewById(R.id.Switch_Text_Preference_Value_Suffix);
        noSwitchTitle = rootView.findViewById(R.id.Switch_Text_Preference_No_Switch_Title);
        switchMaterialView = rootView.findViewById(R.id.Switch_Text_Preference_Material_Switch);

        //set displays
        rootView.setClickable(false);
        valueView.setImeOptions(EditorInfo.IME_ACTION_DONE);
        valueView.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s)
            {
                boolean save;
                String stringValue = s.toString();

                //save value if within range
                if(valueType == Integer.class)
                {
                    int value = Globals.tryParseInt(stringValue);
                    save = (value != Integer.MAX_VALUE && value >= (int)minValue && value <= (int)maxValue);
                }
                else
                {
                    float value = Globals.tryParseFloat(stringValue);
                    save = (value != Float.MAX_VALUE && value >= minValue && value <= maxValue);
                }

                //if saving
                if(save)
                {
                    saveValue(stringValue);
                }

                //set/clear error
                valueView.setError(save ? null : context.getResources().getString(R.string.title_invalid));
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
            setSwitchChecked(switchChecked || Settings.getPreferenceBoolean(context, switchKey));
            checkedChangeListener = new CompoundButton.OnCheckedChangeListener()
            {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
                {
                    boolean textCheckedValue;

                    //if showing switch and value exists
                    if(showSwitch && valueView != null)
                    {
                        //get text checked value
                        textCheckedValue = (reverseEnabled != isChecked);

                        //update text and state
                        updateValueText(textCheckedValue);
                        valueView.setEnabled(!textCheckedValue);

                        //if saving switch
                        if(switchKey != null)
                        {
                            //save setting
                            getWriteSettings(context).putBoolean(switchKey, isChecked).apply();
                        }
                    }
                }
            };
            if(switchMaterialView != null)
            {
                switchMaterialView.setOnCheckedChangeListener(checkedChangeListener);
                checkedChangeListener.onCheckedChanged(switchMaterialView, switchMaterialView.isChecked());
            }
            setSwitchEnabled(switchEnabled);
        }

        noSwitchTitle.setText(titleText);
        updateValueText(false);

        setShowSwitch(showSwitch);
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

    //Sets if switch is enabled
    public void setSwitchEnabled(boolean enabled)
    {
        switchEnabled = enabled;

        //if view exists
        if(switchMaterialView != null)
        {
            //set enabled state
            switchMaterialView.setEnabled(switchEnabled);
        }
    }

    //Sets if switch is checked
    public void setSwitchChecked(boolean checked)
    {
        switchChecked = checked;

        //if view exists
        if(switchMaterialView != null)
        {
            //set checked state
            switchMaterialView.setChecked(checked);
        }
    }

    //Sets if showing switch
    public void setShowSwitch(boolean show)
    {
        showSwitch = show;

        //if view exists
        if(switchMaterialView != null)
        {
            //show if showing switch
            switchMaterialView.setVisibility(showSwitch ? View.VISIBLE : View.GONE);
        }
    }
}
