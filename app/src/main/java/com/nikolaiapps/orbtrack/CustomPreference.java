package com.nikolaiapps.orbtrack;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;


public class CustomPreference extends Preference
{
    private float paddingLeft;
    private String sharedName = null;
    protected String titleText = null;

    public CustomPreference(@NonNull Context context, @Nullable @org.jetbrains.annotations.Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes)
    {
        super(context, attrs, defStyleAttr, defStyleRes);

        //set default
        paddingLeft = Globals.dpToPixels(context, 5);

        //if there are attributes, retrieve them
        if(attrs != null)
        {
            try(@SuppressLint({"NewApi", "LocalSuppress"}) TypedArray valueArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.CustomPreference, 0, 0))
            {
                paddingLeft = valueArray.getDimension(R.styleable.CustomPreference_paddingLeft, paddingLeft);
                titleText = valueArray.getString(R.styleable.CustomPreference_titleText);
                sharedName = valueArray.getString(R.styleable.CustomPreference_sharedName);
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

        //set shared name
        setSharedName(sharedName);
    }

    public CustomPreference(@NonNull Context context, @Nullable @org.jetbrains.annotations.Nullable AttributeSet attrs, int defStyleAttr)
    {
        this(context, attrs, defStyleAttr, 0);
    }

    @SuppressWarnings("unused")
    public CustomPreference(@NonNull Context context, @Nullable @org.jetbrains.annotations.Nullable AttributeSet attrs)
    {
        this(context, attrs, 0);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder)
    {
        super.onBindViewHolder(holder);

        ViewGroup.LayoutParams baseLayoutParams = holder.itemView.getLayoutParams();

        //if margin layout params
        if(baseLayoutParams instanceof ViewGroup.MarginLayoutParams)
        {
            //set layout params
            ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams)holder.itemView.getLayoutParams();
            layoutParams.leftMargin = (int)paddingLeft;
            holder.itemView.setLayoutParams(layoutParams);
        }
    }

    //Sets shared name
    public void setSharedName(String name)
    {
        //set shared name
        sharedName = name;

        //if shared name not set
        if(sharedName == null || sharedName.trim().length() == 0)
        {
            //use default
            sharedName = "Settings";
        }
    }

    //Gets shared preferences
    private SharedPreferences getPreferences(Context context)
    {
        return(context.getSharedPreferences(sharedName, Context.MODE_PRIVATE));
    }

    //Gets write settings
    protected SharedPreferences.Editor getWriteSettings(Context context)
    {
        return(getPreferences(context).edit());
    }
}
