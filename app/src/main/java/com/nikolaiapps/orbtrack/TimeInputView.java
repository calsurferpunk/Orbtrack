package com.nikolaiapps.orbtrack;


import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Parcelable;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.fragment.app.FragmentManager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TimePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
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

        themeID = Globals.getDialogThemeId(context);
        currentHour = currentTime.get(Calendar.HOUR_OF_DAY);
        currentMinute = currentTime.get(Calendar.MINUTE);
        setTime(currentHour, currentMinute);

        this.setCursorVisible(false);
        this.setFocusable(false);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        //if for the on touch-down event
        if(event.getAction() == MotionEvent.ACTION_UP)
        {
            Context context = this.getContext();
            FragmentManager manager = Globals.getFragmentManager(context);

            performClick();

            //if manager exists
            if(manager != null)
            {
                //show dialog and stop
                MaterialTimePicker timeDialog = new MaterialTimePicker.Builder().setHour(currentHour).setMinute(currentMinute).setTimeFormat(TimeFormat.CLOCK_12H).build();
                timeDialog.addOnPositiveButtonClickListener(new OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        TimeInputView.this.onTimeSet(null, timeDialog.getHour(), timeDialog.getMinute());
                    }
                });
                timeDialog.show(manager, "TimeDialog");
                return(true);
            }

            TimePickerDialog timeDialog;

            //show time picker
            timeDialog = new TimePickerDialog(context, themeID, this, currentHour, currentMinute, false);
            timeDialog.show();
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
