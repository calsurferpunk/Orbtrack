package com.nikolaiapps.orbtrack;


import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.FragmentActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.material.slider.Slider;
import java.util.Calendar;
import java.util.TimeZone;


public class PlayBar extends LinearLayout
{
    public static abstract class ScaleType
    {
        static final int Speed = 0;
        static final int Time = 1;
    }

    public interface OnPlayBarChangedListener
    {
        void onProgressChanged(PlayBar seekBar, int progressValue, double subProgressPercent, boolean fromUser);
    }

    private boolean synced;
    private int minValue;
    private int maxValue;
    private int playPeriodMs;
    private int playScaleType;
    private double playScaleFactor;
    private long value2;
    private double playIndexIncrementUnits;
    private double playSubProgressPercent;
    private double playDelayUnits;
    private TimeZone zone;
    private final Drawable pauseDrawable;
    private final Drawable playDrawable;
    private final Drawable liveDrawable;
    private final Drawable syncDrawable;
    private ThreadTask<Void, Void, Void> playTask;
    private FragmentActivity playActivity;
    private final AppCompatButton cancelButton;
    private final AppCompatButton confirmButton;
    private final Slider seekSlider;
    private final AppCompatImageButton syncButton;
    private final AppCompatImageButton playButton;
    private final TextView valueText;
    private final TextView scaleText;
    private final TextView scaleTitle;
    private final LinearLayout buttonLayout;
    private OnClickListener syncButtonListener;
    private OnPlayBarChangedListener progressChangedListener;

    public PlayBar(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        int textColor;
        int buttonColor;
        LinearLayout rootView;
        AppCompatImageButton leftButton;
        AppCompatImageButton rightButton;

        //set defaults
        textColor = buttonColor = Color.WHITE;

        //if attributes are set
        if(attrs != null)
        {
            try(@SuppressLint({"NewApi", "LocalSuppress"}) TypedArray valueArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.PlayBar, 0, 0))
            {
                textColor = valueArray.getColor(R.styleable.PlayBar_textColor, Color.WHITE);
                buttonColor = valueArray.getColor(R.styleable.PlayBar_buttonColor, Color.WHITE);
                valueArray.recycle();
            }
            catch(NoSuchMethodError | Exception noMethod)
            {
                //do nothing
            }
        }

        //create displays
        rootView = (LinearLayout)LayoutInflater.from(context).inflate(R.layout.play_bar_view, this);
        leftButton = rootView.findViewById(R.id.Play_Bar_Left_Button);
        playButton = rootView.findViewById(R.id.Play_Bar_Play_Button);
        rightButton = rootView.findViewById(R.id.Play_Bar_Right_Button);
        syncButton = rootView.findViewById(R.id.Play_Bar_Sync_Button);
        cancelButton = rootView.findViewById(R.id.Play_Bar_Cancel_Button);
        confirmButton = rootView.findViewById(R.id.Play_Bar_Ok_Button);
        seekSlider = rootView.findViewById(R.id.Play_Bar_Seek_Slider);
        valueText = rootView.findViewById(R.id.Play_Bar_Value_Text);
        scaleText = rootView.findViewById(R.id.Play_Bar_Scale_Text);
        scaleTitle = rootView.findViewById(R.id.Play_Bar_Title);
        buttonLayout = rootView.findViewById(R.id.Play_Bar_Button_Layout);

        //get images
        playDrawable = Globals.getDrawable(context, R.drawable.ic_play_arrow_white, buttonColor, false);
        pauseDrawable = Globals.getDrawable(context, R.drawable.ic_pause_white, buttonColor, false);
        liveDrawable = Globals.getDrawableText(context, context.getString(R.string.title_live), 12, Color.WHITE);
        syncDrawable = Globals.getDrawable(context, R.drawable.ic_sync_white, buttonColor, false);

        //set images
        playButton.setBackgroundDrawable(playDrawable);
        leftButton.setBackgroundDrawable(Globals.getDrawable(context, R.drawable.ic_arrow_left_white, buttonColor, false));
        rightButton.setBackgroundDrawable(Globals.getDrawable(context, R.drawable.ic_arrow_right_white, buttonColor, false));

        //set colors
        valueText.setTextColor(textColor);
        scaleText.setTextColor(textColor);
        scaleTitle.setTextColor(textColor);

        //set defaults
        setValueTextVisible(false);
        resetPlayIncrements();
        setSynced(false);
        playTask = null;
        playPeriodMs = 1000;
        playScaleType = ScaleType.Speed;
        playScaleFactor = 1;
        playIndexIncrementUnits = 1;
        value2 = 0;
        setMax((int)seekSlider.getValueTo());
        setMin((int)seekSlider.getValueFrom());
        zone = TimeZone.getDefault();

        //setup seek buttons
        leftButton.setOnClickListener(createSeekManualOnClickListener(false));
        rightButton.setOnClickListener(createSeekManualOnClickListener(true));
    }
    public PlayBar(Context context)
    {
        this(context, null);
    }

    public int getValue()
    {
        return(seekSlider != null ? (int)(seekSlider.getValue()) : minValue);
    }

    public long getValue2()
    {
        return(value2);
    }

    private void updateValue(int value)
    {
        //if slider exists
        if(seekSlider != null)
        {
            //keep value within range
            if(value < minValue)
            {
                value = minValue;
            }
            else if(value > maxValue)
            {
                value = maxValue;
            }

            //set value
            seekSlider.setValue(value);
        }
    }

    public void setValue(int value, boolean forceChange)
    {
        //if a valid value
        if(value >= minValue && value <= maxValue)
        {
            //if value --is not changing- or -forced-- and listener is set
            if((value == (int)seekSlider.getValue() || forceChange) && progressChangedListener != null)
            {
                //still send event
                progressChangedListener.onProgressChanged(this, value, 0, false);
            }

            //update value and reset increments
            updateValue(value);
            resetPlayIncrements();
            setSynced(false);
        }
    }
    public void setValue(int value)
    {
        setValue(value, false);
    }

    public void setValues(int value, long value2)
    {
        this.value2 = value2;
        setValue(value);
    }

    public void setValueText(String text)
    {
        valueText.setText(text);
    }

    public void setValueTextVisible(boolean show)
    {
        valueText.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    public double getSubProgressPercent()
    {
        return(seekSlider != null ? playSubProgressPercent : 0);
    }

    public void setPlayIndexIncrementUnits(double incrementUnits)
    {
        playIndexIncrementUnits = incrementUnits;
    }

    public void setMin(int min)
    {
        if(minValue < maxValue)
        {
            minValue = min;
            seekSlider.setValueFrom(minValue);
        }
    }

    public void setMax(int max)
    {
        maxValue = max;
        seekSlider.setValueTo(maxValue);
    }

    public void setTitle(String title)
    {
        scaleTitle.setText(title);
        scaleTitle.setVisibility(title != null ? View.VISIBLE : View.GONE);
    }

    public void setButtonListeners(OnClickListener confirmListener, OnClickListener cancelListener)
    {
        cancelButton.setOnClickListener(cancelListener);
        confirmButton.setOnClickListener(confirmListener);
    }

    public void setButtonsVisible(boolean show)
    {
        buttonLayout.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    public void setScaleText(String text)
    {
        boolean showText = (text != null);
        ViewGroup.LayoutParams params;

        if(showText)
        {
            scaleText.setText(text);
            params = scaleText.getLayoutParams();
            params.width = LayoutParams.WRAP_CONTENT;
            scaleText.setLayoutParams(params);
        }
        scaleText.setClickable(false);
        scaleText.setVisibility(showText ? View.VISIBLE : View.GONE);
    }

    private void updatePlayScaleFactor()
    {
        boolean forTime = (playScaleType == ScaleType.Time);
        playScaleFactor = (forTime ? playPeriodMs : 1);
    }

    public void setPlayPeriod(int ms)
    {
        //update play period and scale factor
        playPeriodMs = ms;
        updatePlayScaleFactor();

        //if timer was running
        if(playTask != null)
        {
            //restart it
            startPlayTimer();
        }
    }

    public void setPlayScaleType(int scaleType)
    {
        boolean forTime = (scaleType == ScaleType.Time);
        LayoutParams params;

        //set scale type and defaults
        playScaleType = scaleType;
        updatePlayScaleFactor();
        if(forTime)
        {
            params = (LayoutParams)scaleText.getLayoutParams();
            params.width = params.height = LayoutParams.WRAP_CONTENT;
            scaleText.setLayoutParams(params);
        }
        scaleText.setBackground(forTime ? Globals.getDrawable(getContext(), R.drawable.ic_calendar_month_white) : null);
    }

    public void setTimeZone(TimeZone zone)
    {
        this.zone = zone;
    }

    private void setSynced(boolean isSynced)
    {
        boolean showSynced;

        //update status
        synced = isSynced;
        showSynced = (synced && playTask != null);

        //if button exists
        if(syncButton != null)
        {
            //update background/image
            syncButton.setBackgroundDrawable(showSynced ? null : syncDrawable);
            syncButton.setImageDrawable(showSynced ? liveDrawable : null);
        }
    }

    public void setSyncButtonListener(OnClickListener listener)
    {
        //set listener
        syncButtonListener = listener;

        //if button exists
        if(syncButton != null)
        {
            //apply listener
            syncButton.setOnClickListener(new OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    //if listener is set
                    if(syncButtonListener != null)
                    {
                        //call it
                        syncButtonListener.onClick(v);
                    }

                    //update status
                    setSynced(true);
                }
            });

            //update visibility
            syncButton.setVisibility(listener != null ? View.VISIBLE : View.GONE);
        }
    }

    public void sync()
    {
        //if button exists
        if(syncButton != null)
        {
            //update visibility
            syncButton.setVisibility(View.VISIBLE);
        }

        //if listener exists
        if(syncButtonListener != null)
        {
            //call it
            syncButtonListener.onClick(syncButton);
        }
    }

    public void setPlayActivity(FragmentActivity activity)
    {
        boolean usingActivity = (activity != null);

        //set activity
        playActivity = activity;

        //update displays
        playButton.setOnClickListener(createPlayButtonOnClickListener());
        playButton.setVisibility(usingActivity ? View.VISIBLE : View.GONE);
        if(usingActivity)
        {
            scaleText.setText(playScaleType == ScaleType.Time ? R.string.empty : R.string.text_1_x);
            scaleText.setOnClickListener(createScaleTextOnClickListener());
        }
        scaleText.setVisibility(usingActivity ? View.VISIBLE : View.GONE);
    }

    public void setOnSeekChangedListener(OnPlayBarChangedListener changedListener)
    {
        progressChangedListener = changedListener;
        seekSlider.addOnChangeListener(new Slider.OnChangeListener()
        {
            @Override
            public void onValueChange(@NonNull Slider seekSlider, float value, boolean fromUser)
            {
                int progressValue = (int)value;

                //keep rounded value inside range
                if(progressValue < minValue)
                {
                    progressValue = minValue;
                }
                else if(progressValue > maxValue)
                {
                    progressValue = maxValue;
                }

                //if user moved
                if(fromUser)
                {
                    //reset increments
                    resetPlayIncrements();
                    setSynced(false);
                }

                //if listener is set
                if(progressChangedListener != null)
                {
                    //call it
                    progressChangedListener.onProgressChanged(PlayBar.this, progressValue, playSubProgressPercent, fromUser);
                }
            }
        });
    }

    //Resets play increments
    private void resetPlayIncrements()
    {
        //reset delay increments
        playDelayUnits = playSubProgressPercent = 0;
    }

    //Calculates increments based on delay
    private int updatePlayIncrements(boolean forward, boolean fromUser)
    {
        int increments = 0;

        //update delay
        playDelayUnits += (playScaleFactor * (fromUser ? (1000.0 / playPeriodMs) : 1) * (fromUser && playScaleType == ScaleType.Time ? 15 : 1) * (forward ? 1 : -1));

        //if long enough delay
        if(Math.abs(playDelayUnits) >= playIndexIncrementUnits)
        {
            //get increments and update delay
            increments = (int)Math.floor(playDelayUnits / playIndexIncrementUnits);
            playDelayUnits -= (increments * playIndexIncrementUnits);
        }

        //update sub progress
        playSubProgressPercent = (playDelayUnits / playIndexIncrementUnits);

        //return increments
        return(increments);
    }

    //Stops the play timer and returns true if was running
    public boolean stopPlayTimer(boolean resetSynced)
    {
        boolean stopped = false;

        //if timer exists
        if(playTask != null)
        {
            //stop it
            playTask.cancel(true);
            playTask = null;
            stopped = true;
        }

        //if resetting sync status
        if(resetSynced)
        {
            //reset synced
            setSynced(false);
        }

        //set button to play
        playButton.setBackgroundDrawable(playDrawable);
        return(stopped);
    }
    public void stopPlayTimer()
    {
        stopPlayTimer(true);
    }

    //Starts the play timer
    private void startPlayTimer(boolean startSynced)
    {
        //stop any running timer
        stopPlayTimer(false);

        //if button exists
        if(playButton != null)
        {
            //set button to pause
            playButton.setBackgroundDrawable(pauseDrawable);
        }

        //if using speed scale and already at end
        if(playScaleType == ScaleType.Speed && (int)seekSlider.getValue() >= maxValue)
        {
            //reset value and increments
            updateValue(minValue);
            resetPlayIncrements();
        }

        //if starting synced
        if(startSynced)
        {
            //sync again
            sync();
        }

        //if using activity
        if(playActivity != null)
        {
            Runnable playRunnable;
            Runnable incrementRunnable;

            //create and start task
            incrementRunnable = new Runnable()
            {
                @Override
                public void run()
                {
                    int progressValue = (int)seekSlider.getValue();
                    int increments;
                    boolean wasSynced = synced;

                    //add to delay
                    increments = updatePlayIncrements(true, false);

                    //if there are increments
                    if(increments > 0)
                    {
                        //add increments
                        progressValue += increments;

                        //if after end
                        if(progressValue > maxValue)
                        {
                            //handle based on scale type
                            switch(playScaleType)
                            {
                                case ScaleType.Speed:
                                    //set to end and stop
                                    progressValue = maxValue;
                                    stopPlayTimer();
                                    break;

                                case ScaleType.Time:
                                    //set to start of next day
                                    progressValue = 0;
                                    setValues(progressValue, value2 + (long)(Calculations.SecondsPerDay * 1000));
                                    setSynced(wasSynced);
                                    break;
                            }

                            //reset increments
                            resetPlayIncrements();
                        }

                        //update progress
                        updateValue(progressValue);
                    }
                    //else if listener exists
                    else if(progressChangedListener != null)
                    {
                        //update sub progress
                        progressChangedListener.onProgressChanged(PlayBar.this, progressValue, playSubProgressPercent, false);
                    }

                    //sync with calling thread
                    synchronized(this)
                    {
                        //let waiting thread know this has completed
                        this.notify();
                    }
                }
            };
            playRunnable = new Runnable()
            {
                @Override
                public void run()
                {
                    long repeatMs;
                    long startTimeMs = System.currentTimeMillis();

                    //if task still exists
                    if(playTask != null)
                    {
                        playActivity.runOnUiThread(incrementRunnable);
                        try
                        {
                            //sync with increments
                            synchronized(incrementRunnable)
                            {
                                //wait for increments to finish
                                incrementRunnable.wait(2000);
                            }

                            //wait only as long as needed to match delay
                            repeatMs = playPeriodMs - (System.currentTimeMillis() - startTimeMs);
                            if(repeatMs < 1)
                            {
                                //make sure task keeps running
                                repeatMs = 1;
                            }
                        }
                        catch(Exception ex)
                        {
                            //stop on error
                            repeatMs = 0;
                        }

                        //update delay
                        playTask.setRepeatMs(repeatMs);
                    }
                }
            };
            playTask = new ThreadTask<>(playRunnable, playPeriodMs);
            playTask.execute();

            //update status
            setSynced(startSynced);
        }
    }
    private void startPlayTimer()
    {
        //start synced if previously synced
        startPlayTimer(synced);
    }

    //Creates a play button on click listener
    private View.OnClickListener createPlayButtonOnClickListener()
    {
        return(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                boolean setRunning = (playTask == null);

                //if set running
                if(setRunning)
                {
                    //start timer
                    startPlayTimer();
                }
                else
                {
                    //stop timer
                    stopPlayTimer();
                }
            }
        });
    }

    //Starts playing
    public void start(boolean startSynced)
    {
        //manually start timer
        startPlayTimer(startSynced);
    }
    public void start()
    {
        //start synced if previously synced
        start(synced);
    }

    //Creates an on seek manual click listener
    private View.OnClickListener createSeekManualOnClickListener(final boolean forward)
    {
        return(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                boolean clear = false;
                int progressValue = (int)seekSlider.getValue();
                int increments = updatePlayIncrements(forward, true);

                //if going forward
                if(forward)
                {
                    //if before end
                    if(progressValue < maxValue)
                    {
                        //go forward an increment
                        progressValue += increments;
                    }
                }
                else
                {
                    //if after first
                    if(progressValue > minValue)
                    {
                        //go backward an increment
                        progressValue += increments;
                    }
                }

                //make sure still within range
                if(forward && (progressValue + playSubProgressPercent) >= maxValue)
                {
                    progressValue = maxValue;
                    clear = true;
                }
                else if(!forward && (progressValue + playSubProgressPercent) <= 0)
                {
                    progressValue = 0;
                    clear = true;
                }
                if(clear)
                {
                    resetPlayIncrements();
                    increments = 0;
                }

                //if no increments and listener is set
                if(increments == 0 && progressChangedListener != null)
                {
                    //update sub progress
                    progressChangedListener.onProgressChanged(PlayBar.this, progressValue, playSubProgressPercent, true);
                }

                //update progress
                playSubProgressPercent = 0;
                updateValue(progressValue);
                setSynced(false);
            }
        });
    }

    //Creates an on speed text click listener
    private View.OnClickListener createScaleTextOnClickListener()
    {
        Resources res = getResources();

        return(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                String text;
                Calendar date;

                switch(playScaleType)
                {
                    case ScaleType.Speed:
                        //update speed
                        switch((int)playScaleFactor)
                        {
                            case 1:
                                playScaleFactor = 10;
                                break;

                            case 10:
                                playScaleFactor = 100;
                                break;

                            case 100:
                                playScaleFactor = 1000;
                                break;

                            default:
                                playScaleFactor = 1;
                                break;
                        }

                        //update display
                        text = (int)playScaleFactor + res.getString(R.string.text_x);
                        scaleText.setText(text);
                        break;

                    case ScaleType.Time:
                        //set date
                        date = Globals.clearCalendarTime(Globals.getLocalTime(Globals.getGMTTime(value2), zone));

                        //show calendar dialog
                        Globals.showDateDialog(getContext(), date, new DatePickerDialog.OnDateSetListener()
                        {
                            @Override
                            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth)
                            {
                                //get new date without time
                                Calendar newDate = Globals.clearCalendarTime(Globals.getLocalTime(Globals.getGMTTime(), zone));

                                //if new date is set
                                if(newDate != null)
                                {
                                    //set date values
                                    newDate.set(Calendar.YEAR, year);
                                    newDate.set(Calendar.MONTH, month);
                                    newDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                                    //update values
                                    setValues(getValue(), newDate.getTimeInMillis());
                                }
                            }
                        });
                        break;
                }
            }
        });
    }
}
