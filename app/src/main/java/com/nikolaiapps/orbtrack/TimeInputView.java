package com.nikolaiapps.orbtrack;


import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Parcelable;
import androidx.appcompat.widget.AppCompatEditText;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
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
            Context context = this.getContext();

            performClick();

            //if possibly a version with time picker dialog problems
            if(Build.VERSION.SDK_INT >= 22 && Build.VERSION.SDK_INT <= 23)
            {
                TimePicker timeView = new TimePicker(new ContextThemeWrapper(context, themeID));

                //set defaults
                if(Build.VERSION.SDK_INT > 22)
                {
                    timeView.setHour(currentHour);
                    timeView.setMinute(currentMinute);
                }
                else
                {
                    timeView.setCurrentHour(currentHour);
                    timeView.setCurrentMinute(currentMinute);
                }
                timeView.setIs24HourView(false);

                //show time picker
                new AlertDialog.Builder(context, themeID).setPositiveButton(R.string.title_ok, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)
                    {
                        if(Build.VERSION.SDK_INT > 22)
                        {
                            TimeInputView.this.onTimeSet(timeView, timeView.getHour(), timeView.getMinute());
                        }
                        else
                        {
                            TimeInputView.this.onTimeSet(timeView, timeView.getCurrentHour(), timeView.getCurrentMinute());
                        }
                    }
                }).setNegativeButton(R.string.title_cancel, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)
                    {
                        dialogInterface.dismiss();
                    }
                }).setView(timeView).show();
            }
            else
            {
                TimePickerDialog timeDialog;

                //show time picker
                if(Build.VERSION.SDK_INT <= 20)
                {
                    timeDialog = new TimePickerDialog(context, this, currentHour, currentMinute, false);
                    if(timeDialog.getWindow() != null)
                    {
                        timeDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                    }
                }
                else
                {
                    timeDialog = new TimePickerDialog(context, themeID, this, currentHour, currentMinute, false);
                }
                timeDialog.show();
            }
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
