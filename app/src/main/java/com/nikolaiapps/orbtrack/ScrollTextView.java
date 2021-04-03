package com.nikolaiapps.orbtrack;


import android.content.Context;
import androidx.appcompat.widget.AppCompatTextView;
import android.text.method.ScrollingMovementMethod;
import android.util.AttributeSet;
import android.view.MotionEvent;


public class ScrollTextView extends AppCompatTextView
{
    public ScrollTextView(Context context)
    {
        super(context);
        base();
    }

    public ScrollTextView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        base();
    }

    public ScrollTextView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        base();
    }

    private void base()
    {
        setMovementMethod(new ScrollingMovementMethod());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        boolean isDown;
        int action = event.getAction();

        switch(action)
        {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_UP:
                if(this.getParent() != null)
                {
                    isDown = (action == MotionEvent.ACTION_DOWN);

                    this.getParent().requestDisallowInterceptTouchEvent(isDown);
                    if(isDown)
                    {
                        performClick();
                    }
                }
                break;
        }

        return(super.onTouchEvent(event));
    }

    @Override
    public boolean performClick()
    {
        return(super.performClick());
    }
}
