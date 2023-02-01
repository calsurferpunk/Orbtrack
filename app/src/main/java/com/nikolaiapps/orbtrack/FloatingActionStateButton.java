package com.nikolaiapps.orbtrack;


import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.util.AttributeSet;
import android.view.animation.AnimationUtils;


public class FloatingActionStateButton extends FloatingActionButton
{
    private int normalTintColor;
    private int checkedTintColor;
    private boolean checked;
    private boolean showChecked;

    public FloatingActionStateButton(Context context)
    {
        super(context);
        init(null);
    }

    public FloatingActionStateButton(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(attrs);
    }

    public FloatingActionStateButton(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(AttributeSet attrs)
    {
        //if attributes are not set
        if(attrs == null)
        {
            normalTintColor = Color.WHITE;
            checkedTintColor = Color.GRAY;
        }
        else
        {
            try(TypedArray valueArray = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.FloatingActionStateButton, 0, 0))
            {
                normalTintColor = valueArray.getColor(R.styleable.FloatingActionStateButton_normalTint, Color.WHITE);
                checkedTintColor = valueArray.getColor(R.styleable.FloatingActionStateButton_checkedTint, Color.GRAY);
                valueArray.recycle();
            }
        }

        //force update
        showChecked = true;
        checked = true;
        setChecked(false, false);

        //set alpha
        setAlpha(0.85f);
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
