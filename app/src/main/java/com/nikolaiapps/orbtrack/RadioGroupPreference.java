package com.nikolaiapps.orbtrack;


import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.core.widget.CompoundButtonCompat;
import androidx.preference.PreferenceViewHolder;


public class RadioGroupPreference extends ValueTypePreference
{
    private String titleText;
    private String pendingSetValue;
    private String[] itemTexts;
    private String[] itemValues;
    private AppCompatRadioButton[] radioButtons;

    public RadioGroupPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes)
    {
        super(context, attrs, defStyleAttr, defStyleRes);

        this.setLayoutResource(R.layout.radio_group_preference_layout);

        int resId;
        TypedArray valueArray;
        Resources res = context.getResources();

        //set defaults
        valueType = String.class;
        pendingSetValue = null;
        radioButtons = new AppCompatRadioButton[0];

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
            setValueType(valueArray.getInt(R.styleable.RadioGroupPreference_valueType, ClassType.String));
            valueArray.recycle();
        }

        //set shared name
        setSharedName(sharedName);

        //set to current preference value
        setSelectedValue(getPreferenceValue());
    }

    public RadioGroupPreference(Context context, AttributeSet attrs, int defStyleAttr)
    {
        this(context, attrs, defStyleAttr, 0);
    }

    @SuppressWarnings("unused")
    public RadioGroupPreference(Context context, AttributeSet attrs)
    {
        this(context, attrs, 0);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder)
    {
        super.onBindViewHolder(holder);

        int index;
        int count;
        final Context context = this.getContext();
        final TextView titleView;
        final RadioGroup groupView;
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
                radioButtons = new AppCompatRadioButton[count];
            }
            else
            {
                //get current checked button
                for(AppCompatRadioButton currentRadioButton : radioButtons)
                {
                    //remember current tag
                    Object currentTag = currentRadioButton.getTag();

                    //if checked and a string
                    if(currentRadioButton.isChecked() && currentTag instanceof String)
                    {
                        //remember value
                        pendingSetValue = (String)currentRadioButton.getTag();
                    }
                }

                //remove all old buttons
                groupView.removeAllViews();
            }

            //go through texts/values
            for(index = 0; index < count; index++)
            {
                //create and add radio button
                int accentColor = Globals.resolveColorID(context, R.attr.colorAccent);
                String currentValue = itemValues[index];
                AppCompatRadioButton currentRadioButton = new AppCompatRadioButton(new ContextThemeWrapper(context, R.style.RadioButton), null);
                CompoundButtonCompat.setButtonTintList(currentRadioButton, new ColorStateList(new int[][]{new int[]{-android.R.attr.state_checked}, new int[]{android.R.attr.state_checked}}, new int[]{accentColor, accentColor}));
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
                            for(AppCompatRadioButton currentRadioButton : radioButtons)
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
                            saveValue(currentValue);
                        }
                    }
                });
                groupView.addView(currentRadioButton);

                //set radio button
                if(radioButtons[index] != null)
                {
                    radioButtons[index] = null;
                }
                radioButtons[index] = currentRadioButton;
            }

            //clear any pending value
            pendingSetValue = null;
        }
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
                AppCompatRadioButton currentRadioButton = radioButtons[index];

                //checked if value matches
                currentRadioButton.setChecked(currentRadioButton.getTag().equals(value));
            }

            //clear pending value
            pendingSetValue = null;
        }
    }
}
