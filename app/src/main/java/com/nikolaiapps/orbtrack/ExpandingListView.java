package com.nikolaiapps.orbtrack;


import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.ExpandableListView;


public class ExpandingListView extends ExpandableListView
{
    public ExpandingListView(Context context)
    {
        super(context);
    }

    public ExpandingListView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public ExpandingListView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        ViewGroup.LayoutParams params;

        heightMeasureSpec = MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE >> 2, MeasureSpec.AT_MOST);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        params = getLayoutParams();
        params.height = getMeasuredHeight();
        setLayoutParams(params);
    }
}
