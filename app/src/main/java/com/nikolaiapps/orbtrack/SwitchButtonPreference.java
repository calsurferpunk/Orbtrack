package com.nikolaiapps.orbtrack;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.SwitchCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;


public class SwitchButtonPreference extends Preference
{
    private String sharedName;
    private String titleText;
    private AppCompatButton button;

    public SwitchButtonPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes)
    {
        super(context, attrs, defStyleAttr, defStyleRes);

        this.setPersistent(false);
        this.setLayoutResource(R.layout.switch_button_preference_layout);

        TypedArray valueArray;

        //if there are attributes, retrieve them
        if(attrs != null)
        {
            valueArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.SwitchButtonPreference, 0, 0);
            sharedName = valueArray.getString(R.styleable.SwitchButtonPreference_sharedName);
            titleText = valueArray.getString(R.styleable.SwitchButtonPreference_titleText);
            valueArray.recycle();
        }

        //if shared name not set
        if(sharedName == null || sharedName.trim().length() == 0)
        {
            //use default
            sharedName = "Settings";
        }
    }

    public SwitchButtonPreference(Context context, AttributeSet attrs, int defStyleAttr)
    {
        this(context, attrs, defStyleAttr, 0);
    }

    public SwitchButtonPreference(Context context, AttributeSet attrs)
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
        final View buttonHolder;
        final TextView titleView;
        final TextView summaryView;
        final SwitchCompat switchView;
        final ViewGroup rootView;
        final SharedPreferences readSettings = getPreferences(context);
        final SharedPreferences.Editor writeSettings = getWriteSettings(context);

        //get displays
        rootView = (ViewGroup)holder.itemView;
        buttonHolder = rootView.findViewById(R.id.Switch_Button_Preference_Button_Holder);
        if(button != null && buttonHolder != null)
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
        switchView.setChecked(readSettings.getBoolean(preferenceName, false));
        switchView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                writeSettings.putBoolean(preferenceName, isChecked).apply();
            }
        });
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
}
