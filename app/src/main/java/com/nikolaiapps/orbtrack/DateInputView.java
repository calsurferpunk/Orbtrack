package com.nikolaiapps.orbtrack;


import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Build;
import android.os.Parcelable;
import androidx.appcompat.widget.AppCompatEditText;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.DatePicker;
import java.util.Calendar;
import java.util.TimeZone;


public class DateInputView extends AppCompatEditText implements DatePickerDialog.OnDateSetListener
{
    public interface OnDateSetListener
    {
        void onDateSet(DateInputView dateView, Calendar date);
    }

    private int themeID;
    private Calendar currentDate;
    private TimeZone zone;
    private OnDateSetListener dateSetListener;

    public DateInputView(Context context)
    {
        super(context);
        init(context);
    }

    public DateInputView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(context);
    }

    public DateInputView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context)
    {
        themeID = Globals.getDialogThemeID(context);
        currentDate = Calendar.getInstance();
        zone = currentDate.getTimeZone();
        setDate(currentDate);
        this.setCursorVisible(false);
        this.setFocusable(false);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        //if for the on touch down event
        if(event.getAction() == MotionEvent.ACTION_DOWN)
        {
            performClick();
            Globals.showDateDialog(this.getContext(), currentDate, this);
        }

        //handled
        return(true);
    }

    @Override
    public boolean performClick()
    {
        return(super.performClick());
    }

    @Override
    public void onRestoreInstanceState(Parcelable state)
    {
        super.onRestoreInstanceState(state);
        setDate(currentDate);
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth)
    {
        Calendar resultDate = Calendar.getInstance(zone);

        //update date
        resultDate.set(year, monthOfYear, dayOfMonth);
        setDate(resultDate);
    }

    public Calendar getDate()
    {
        return(currentDate);
    }

    public void setOnDateSetListener(OnDateSetListener listener)
    {
        dateSetListener = listener;
    }

    private void updateDisplay()
    {
        //update text
        this.setError(null);
        this.setText(Globals.getDateYearString(currentDate, zone));
    }

    public void setDate(Calendar date)
    {
        //update date
        currentDate.setTimeZone(zone);
        currentDate.setTimeInMillis(date.getTimeInMillis());

        //clear hours, minutes, seconds, and ms
        Globals.clearCalendarTime(currentDate);

        //update display
        updateDisplay();

        //if listener is set
        if(dateSetListener != null)
        {
            dateSetListener.onDateSet(this, currentDate);
        }
    }

    public void setTimeZone(TimeZone zn)
    {
        zone = zn;
        currentDate.setTimeZone(zn);
        updateDisplay();
    }
}
