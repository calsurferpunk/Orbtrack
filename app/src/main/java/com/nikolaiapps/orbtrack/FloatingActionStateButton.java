package com.nikolaiapps.orbtrack;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.util.AttributeSet;
import android.view.animation.AnimationUtils;


public class FloatingActionStateButton extends FloatingActionButton
{
    public interface OnCheckedChangedListener
    {
        void onCheckedChanged(FloatingActionStateButton button, boolean isChecked);
    }

    private int normalTintColor;
    private int checkedTintColor;
    private boolean checked;
    private boolean showChecked;
    private OnCheckedChangedListener checkedChangedListener;

    public FloatingActionStateButton(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);

        //if attributes are not set
        if(attrs == null)
        {
            normalTintColor = Color.WHITE;
            checkedTintColor = Color.GRAY;
        }
        else
        {
            try(@SuppressLint({"NewApi", "LocalSuppress"}) TypedArray valueArray = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.FloatingActionStateButton, 0, 0))
            {
                normalTintColor = valueArray.getColor(R.styleable.FloatingActionStateButton_normalTint, Color.WHITE);
                checkedTintColor = valueArray.getColor(R.styleable.FloatingActionStateButton_checkedTint, Color.GRAY);
                valueArray.recycle();
            }
            catch(NoSuchMethodError | Exception noMethod)
            {
                //do nothing
            }
        }

        //force update
        showChecked = true;
        checked = true;
        setChecked(false, false);

        //set alpha
        setAlpha(0.85f);
    }
    public FloatingActionStateButton(Context context, AttributeSet attrs)
    {
        this(context, attrs, R.attr.floatingActionButtonStyle);
    }
    public FloatingActionStateButton(Context context)
    {
        this(context, null);
    }

    public void setOnCheckedChangedListener(OnCheckedChangedListener listener)
    {
        checkedChangedListener = listener;
    }

    public void setStateColors(int normalColor, int checkedColor)
    {
        normalTintColor = normalColor;
        checkedTintColor = checkedColor;
        setBackgroundTintList(ColorStateList.valueOf(checked && showChecked ? checkedTintColor : normalTintColor));
    }

    private void setChecked(boolean isChecked, boolean invalidate)
    {
        if(checked != isChecked)
        {
            checked = isChecked;
            setBackgroundTintList(ColorStateList.valueOf(checked && showChecked ? checkedTintColor : normalTintColor));

            if(checkedChangedListener != null)
            {
                checkedChangedListener.onCheckedChanged(this, isChecked);
            }

            if(invalidate)
            {
                this.invalidate();
            }
        }
    }
    public void setChecked(boolean isChecked)
    {
        setChecked(isChecked, true);
    }

    public boolean isChecked()
    {
        return(checked);
    }

    public void setShowChecked(boolean show)
    {
        showChecked = show;
    }

    //Set rotating enabled
    public void setRotating(boolean enabled)
    {
        if(enabled)
        {
            startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.rotate_forever));
        }
        else
        {
            clearAnimation();
        }
    }
}
