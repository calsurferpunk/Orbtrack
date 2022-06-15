package com.nikolaiapps.orbtrack;


import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.preference.PreferenceViewHolder;


public class SwitchPreference extends CustomPreference
{
    private boolean isChecked;
    private String subKey;
    private SwitchCompat switchView;

    public SwitchPreference(@NonNull Context context, @Nullable @org.jetbrains.annotations.Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes)
    {
        super(context, attrs, defStyleAttr, defStyleRes);

        this.persistBoolean(false);
        this.setLayoutResource(R.layout.switch_preference_layout);

        //set default
        isChecked = false;
        subKey = "";
    }

    public SwitchPreference(@NonNull Context context, @Nullable @org.jetbrains.annotations.Nullable AttributeSet attrs, int defStyleAttr)
    {
        this(context, attrs, defStyleAttr, 0);
    }

    @SuppressWarnings("unused")
    public SwitchPreference(@NonNull Context context, @Nullable @org.jetbrains.annotations.Nullable AttributeSet attrs)
    {
        this(context, attrs, 0);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder)
    {
        super.onBindViewHolder(holder);

        final CharSequence summary = this.getSummary();
        final TextView summaryView;
        final ViewGroup rootView;

        //get displays
        rootView = (ViewGroup)holder.itemView;
        summaryView = rootView.findViewById(R.id.Switch_Preference_Summary);
        switchView = holder.itemView.findViewById(R.id.Switch_Preference_Switch);

        //set displays
        if(summary != null && summary.length() > 0)
        {
            summaryView.setText(summary);
            summaryView.setVisibility(ViewGroup.VISIBLE);
        }
        switchView.setText(this.getTitle());
        switchView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked)
            {
                isChecked = checked;
                callOnPreferenceChangeListener();
            }
        });
        switchView.setChecked(isChecked);
    }

    //Sets sub key
    public void setSubKey(String name)
    {
        subKey = name;
    }

    //Gets sub key
    public String getSubKey()
    {
        return(subKey);
    }

    //Calls on preference change listener
    private void callOnPreferenceChangeListener()
    {
        OnPreferenceChangeListener listener = getOnPreferenceChangeListener();

        if(listener != null)
        {
            listener.onPreferenceChange(SwitchPreference.this, isChecked);
        }
    }

    //Set checked status
    public void setChecked(boolean checked)
    {
        isChecked = checked;

        if(switchView == null)
        {
            callOnPreferenceChangeListener();
        }
        else
        {
            switchView.setChecked(isChecked);
        }
    }
}
