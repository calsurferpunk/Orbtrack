package com.nikolaiapps.orbtrack;


import android.content.Context;
import android.util.AttributeSet;


public class EditTextSelect extends androidx.appcompat.widget.AppCompatEditText
{
    public interface OnSelectionChangedListener
    {
        void onSelectionChanged(int selStart, int selEnd);
    }

    private
    OnSelectionChangedListener selectionChangedListener;

    public EditTextSelect(Context context)
    {
        super(context);
    }
    public EditTextSelect(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }
    public EditTextSelect(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd)
    {
        super.onSelectionChanged(selStart, selEnd);

        if(selectionChangedListener != null)
        {
            selectionChangedListener.onSelectionChanged(selStart, selEnd);
        }
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener listener)
    {
        selectionChangedListener = listener;
    }
}
