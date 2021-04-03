package com.nikolaiapps.orbtrack;


import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import androidx.viewpager.widget.ViewPager;


public class SwipeStateViewPager extends ViewPager
{
    private boolean allowSwipe;

    public SwipeStateViewPager(Context context)
    {
        super(context);
        allowSwipe = true;
    }
    public SwipeStateViewPager(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        allowSwipe = true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev)
    {
        if(allowSwipe)
        {
            performClick();
        }

        return(allowSwipe && super.onTouchEvent(ev));
    }

    @Override
    public boolean performClick()
    {
        return(super.performClick());
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev)
    {
        return(allowSwipe && super.onInterceptTouchEvent(ev));
    }

    public void setSwipeEnabled(boolean enabled)
    {
        allowSwipe = enabled;
    }
}
