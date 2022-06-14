package com.nikolaiapps.orbtrack;


import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.SwitchCompat;
import androidx.preference.PreferenceViewHolder;


public class SwitchButtonPreference extends CustomPreference
{
    public interface OnCheckedChangedListener
    {
        void onCheckedChanged(String preferenceName, boolean isChecked);
    }

    private AppCompatButton button;
    private OnCheckedChangedListener checkedChangedListener;

    public SwitchButtonPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes)
    {
        super(context, attrs, defStyleAttr, defStyleRes);

        this.setPersistent(false);
        this.setLayoutResource(R.layout.switch_button_preference_layout);
    }

    public SwitchButtonPreference(Context context, AttributeSet attrs, int defStyleAttr)
    {
        this(context, attrs, defStyleAttr, 0);
    }

    @SuppressWarnings("unused")
    public SwitchButtonPreference(Context context, AttributeSet attrs)
    {
        this(context, attrs, 0);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder)
    {
        super.onBindViewHolder(holder);

        final Context context = this.getContext();
        final String preferenceName = this.getKey();
        final CharSequence summary = this.getSummary();
        final View buttonHolder;
        final TextView titleView;
        final TextView summaryView;
        final SwitchCompat switchView;
        final ViewGroup rootView;
        final SharedPreferences.Editor writeSettings = getWriteSettings(context);

        //get displays
        rootView = (ViewGroup)holder.itemView;
        buttonHolder = rootView.findViewById(R.id.Switch_Button_Preference_Button_Holder);
        if(button != null && button.getParent() == null && buttonHolder != null)
        {
            Globals.replaceView(R.id.Switch_Button_Preference_Button_Holder, button, (ViewGroup)buttonHolder.getParent());
        }
        titleView = rootView.findViewById(R.id.Switch_Button_Preference_Title);
        summaryView = rootView.findViewById(R.id.Switch_Button_Preference_Summary);
        switchView = rootView.findViewById(R.id.Switch_Button_Preference_Switch);

        //set displays
        rootView.setClickable(false);
        titleView.setText(titleText);
        if(summary != null && summary.length() > 0)
        {
            summaryView.setText(summary);
            summaryView.setVisibility(View.VISIBLE);
        }
        switchView.setChecked(Settings.getPreferenceBoolean(context, preferenceName));
        switchView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                //update preference setting
                writeSettings.putBoolean(preferenceName, isChecked).apply();

                //if listener is set
                if(checkedChangedListener != null)
                {
                    //call it
                    checkedChangedListener.onCheckedChanged(preferenceName, isChecked);
                }
            }
        });
    }

    //Sets the button
    public void setButton(AppCompatButton newButton)
    {
        button = newButton;
    }

    //Sets the button click listener
    public void setButtonOnClickListener(View.OnClickListener listener)
    {
        if(button != null)
        {
            button.setOnClickListener(listener);
        }
    }

    //Sets the on checked changed listener
    public void setCheckedChangedListener(OnCheckedChangedListener listener)
    {
        checkedChangedListener = listener;
    }
}
