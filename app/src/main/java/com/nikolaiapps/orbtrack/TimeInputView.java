package com.nikolaiapps.orbtrack;


import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Build;
import android.os.Parcelable;
import androidx.appcompat.widget.AppCompatEditText;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.TimePicker;
import java.text.DateFormat;
import java.util.Calendar;


public class TimeInputView extends AppCompatEditText implements TimePickerDialog.OnTimeSetListener
{
    public interface OnTimeSetListener
    {
        void onTimeSet(TimeInputView timeView, int hour, int minute);
    }

    private int themeID;
    private int currentHour;
    private int currentMinute;
    private OnTimeSetListener timeSetListener;

    public TimeInputView(Context context)
    {
        super(context);
        init(context);
    }

    public TimeInputView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(context);
    }

    public TimeInputView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context)
    {
        Calendar currentTime = Calendar.getInstance();

        themeID = Globals.getDialogThemeID(context);
        currentHour = currentTime.get(Calendar.HOUR_OF_DAY);
        currentMinute = currentTime.get(Calendar.MINUTE);
        setTime(currentHour, currentMinute);

        this.setCursorVisible(false);
        this.setFocusable(false);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        //if for the on touch down event
        if(event.getAction() == MotionEvent.ACTION_DOWN)
        {
            TimePickerDialog dateDialog;

            performClick();

            //show date picker
            if(Build.VERSION.SDK_INT <= 20)
            {
                dateDialog = new TimePickerDialog(this.getContext(), this, currentHour, currentMinute, false);
                if(dateDialog.getWindow() != null)
                {
                    dateDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                }
            }
            else
            {
                dateDialog = new TimePickerDialog(this.getContext(), themeID, this, currentHour, currentMinute, false);
            }
            dateDialog.show();
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
        setTime(currentHour, currentMinute);
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute)
    {
        //update time
        setTime(hourOfDay, minute);
    }

    public int getHour()
    {
        return(currentHour);
    }

    public int getMinute()
    {
        return(currentMinute);
    }

    public void setOnTimeSetListener(OnTimeSetListener listener)
    {
        timeSetListener = listener;
    }

    public void setTime(Calendar currentTime)
    {
        setTime(currentTime.get(Calendar.HOUR_OF_DAY), currentTime.get(Calendar.MINUTE));
    }

    public void setTime(int hourOfDay, int minute)
    {
        Calendar currentTime = Calendar.getInstance();
        DateFormat timeFormatter = DateFormat.getTimeInstance(DateFormat.SHORT);

        //update time
        currentHour = hourOfDay;
        currentMinute = minute;

        //update text
        currentTime.set(Calendar.HOUR_OF_DAY, currentHour);
        currentTime.set(Calendar.MINUTE, currentMinute);
        currentTime.set(Calendar.MILLISECOND, 0);
        this.setError(null);
        this.setText(timeFormatter.format(currentTime.getTime()));

        //if listener is set
        if(timeSetListener != null)
        {
            timeSetListener.onTimeSet(this, currentHour, currentMinute);
        }
    }
}
