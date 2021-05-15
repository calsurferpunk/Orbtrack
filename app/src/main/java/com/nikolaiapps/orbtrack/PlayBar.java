package com.nikolaiapps.orbtrack;


import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.FragmentActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatSeekBar;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import java.util.Timer;
import java.util.TimerTask;


public class PlayBar extends LinearLayout
{
    public interface OnPlayBarChangedListener
    {
        void onProgressChanged(PlayBar seekBar, int progressValue, double subProgressPercent, boolean fromUser);
    }

    private int minValue;
    private int maxValue;
    private int playScaleFactor;
    private double playIndexIncrementUnits;
    private double playSubProgressPercent;
    private double playDelayUnits;
    private Drawable pauseDrawable;
    private Drawable playDrawable;
    private Timer playTimer;
    private FragmentActivity playActivity;
    private AppCompatButton cancelButton;
    private AppCompatButton confirmButton;
    private AppCompatSeekBar seekBar;
    private AppCompatImageButton playButton;
    private TextView scaleText;
    private TextView scaleTitle;
    private LinearLayout buttonLayout;
    private OnPlayBarChangedListener progressChangedListener;

    private void baseConstructor(Context context, AttributeSet attrs)
    {
        int textColor;
        int buttonColor;
        TypedArray valueArray;
        LinearLayout rootView;
        AppCompatImageButton leftButton;
        AppCompatImageButton rightButton;

        //if attributes are set
        if(attrs == null)
        {
            textColor = buttonColor = Color.WHITE;
        }
        else
        {
            valueArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.PlayBar, 0, 0);
            textColor = valueArray.getColor(R.styleable.PlayBar_textColor, Color.WHITE);
            buttonColor = valueArray.getColor(R.styleable.PlayBar_buttonColor, Color.WHITE);
        }

        //create displays
        rootView = (LinearLayout)LayoutInflater.from(context).inflate(R.layout.play_bar_view, this);
        leftButton = rootView.findViewById(R.id.Play_Bar_Left_Button);
        playButton = rootView.findViewById(R.id.Play_Bar_Play_Button);
        rightButton = rootView.findViewById(R.id.Play_Bar_Right_Button);
        cancelButton = rootView.findViewById(R.id.Play_Bar_Cancel_Button);
        confirmButton = rootView.findViewById(R.id.Play_Bar_Ok_Button);
        seekBar = rootView.findViewById(R.id.Play_Bar_Seek_Bar);
        scaleText = rootView.findViewById(R.id.Play_Bar_Scale_Text);
        scaleTitle = rootView.findViewById(R.id.Play_Bar_Title);
        buttonLayout = rootView.findViewById(R.id.Play_Bar_Button_Layout);

        //get images
        playDrawable = Globals.getDrawable(context, R.drawable.ic_play_arrow_white, buttonColor, false);
        pauseDrawable = Globals.getDrawable(context, R.drawable.ic_pause_white, buttonColor, false);

        //set images
        playButton.setBackgroundDrawable(playDrawable);
        leftButton.setBackgroundDrawable(Globals.getDrawable(context, R.drawable.ic_arrow_left_white, buttonColor, false));
        rightButton.setBackgroundDrawable(Globals.getDrawable(context, R.drawable.ic_arrow_right_white, buttonColor, false));

        //set colors
        scaleText.setTextColor(textColor);
        scaleTitle.setTextColor(textColor);

        //set defaults
        playSubProgressPercent = playDelayUnits = 0;
        playScaleFactor = 1;
        playIndexIncrementUnits = 1;
        minValue = 0;
        maxValue = seekBar.getMax();

        //setup seek buttons
        leftButton.setOnClickListener(createSeekManualOnClickListener(false));
        rightButton.setOnClickListener(createSeekManualOnClickListener(true));
    }

    public PlayBar(Context context)
    {
        super(context);
        baseConstructor(context, null);
    }

    public PlayBar(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        baseConstructor(context, attrs);
    }

    public int getValue()
    {
        return(seekBar != null ? (seekBar.getProgress() + minValue) : minValue);
    }

    public void setValue(int value, boolean forceChange)
    {
        int actualValue;

        //if a valid value
        if(value >= minValue && value <= maxValue)
        {
            //get progress value used
            actualValue = value - minValue;

            //if value --is not changing- or -forced-- and listener is set
            if((actualValue == seekBar.getProgress() || forceChange) && progressChangedListener != null)
            {
                //still send event
                progressChangedListener.onProgressChanged(this, value, 0, false);
            }

            //update value
            seekBar.setProgress(actualValue);
            playDelayUnits = 0;
        }
    }
    public void setValue(int value)
    {
        setValue(value, false);
    }

    public double getSubProgressPercent()
    {
        return(seekBar != null ? playSubProgressPercent : 0);
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
            setMax(maxValue);
        }
    }

    public void setMax(int max)
    {
        maxValue = max;
        seekBar.setMax(maxValue - minValue);
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
            scaleText.setText(R.string.text_1_x);
            scaleText.setOnClickListener(createSpeedTextOnClickListener());
        }
        scaleText.setVisibility(usingActivity ? View.VISIBLE : View.GONE);
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

    public void setOnSeekChangedListener(OnPlayBarChangedListener changedListener)
    {
        progressChangedListener = changedListener;
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progressValue, boolean fromUser)
            {
                if(progressChangedListener != null)
                {
                    progressChangedListener.onProgressChanged(PlayBar.this, progressValue + minValue, playSubProgressPercent, fromUser);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });
    }

    //Calculates increments based on delay
    private int updatePlayIncrements(boolean forward)
    {
        int increments = 0;

        //update delay
        playDelayUnits += (playScaleFactor * (forward ? 1 : -1));

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

    //Stops the play timer
    public void stopPlayTimer()
    {
        //if timer exists
        if(playTimer != null)
        {
            //stop it
            playTimer.cancel();
            playTimer.purge();
            playTimer = null;
        }

        //set button to play
        playButton.setBackgroundDrawable(playDrawable);
    }

    //Starts the play timer
    private void startPlayTimer()
    {
        //stop any running timer
        stopPlayTimer();

        //set button to pause
        playButton.setBackgroundDrawable(pauseDrawable);

        //if already at end
        if(seekBar.getProgress() >= seekBar.getMax())
        {
            //reset
            playSubProgressPercent = 0;
            seekBar.setProgress(0);
        }

        //if using activity
        if(playActivity != null)
        {
            //create and start timer
            playTimer = new Timer();
            playTimer.schedule(new TimerTask()
            {
                @Override
                public void run()
                {
                    playActivity.runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            int actualMaxValue = seekBar.getMax();
                            int progressValue = seekBar.getProgress();
                            int increments;

                            //add to delay
                            increments = updatePlayIncrements(true);

                            //if there are increments
                            if(increments > 0)
                            {
                                //add increments
                                progressValue += increments;

                                //if at or past end
                                if(progressValue >= actualMaxValue)
                                {
                                    //set to end
                                    progressValue = actualMaxValue;
                                    playSubProgressPercent = 0;
                                }

                                //add to progress and continue
                                seekBar.setProgress(progressValue);

                                //if at or after end
                                if(progressValue >= actualMaxValue)
                                {
                                    //done
                                    stopPlayTimer();
                                }
                            }
                            //else if listener exists
                            else if(progressChangedListener != null)
                            {
                                //update sub progress
                                progressChangedListener.onProgressChanged(PlayBar.this, progressValue + minValue, playSubProgressPercent, false);
                            }
                        }
                    });
                }
            }, 0, 1000);
        }
    }

    //Creates a play button on click listener
    private View.OnClickListener createPlayButtonOnClickListener()
    {
        return(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                boolean setRunning = (playTimer == null);

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

    //Creates an on seek manual click listener
    private View.OnClickListener createSeekManualOnClickListener(final boolean forward)
    {
        return(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                int progressValue = seekBar.getProgress();
                int increments = updatePlayIncrements(forward);

                //if going forward
                if(forward)
                {
                    //if before end
                    if(progressValue < seekBar.getMax())
                    {
                        //go forward increment
                        progressValue += increments;
                    }
                }
                else
                {
                    //if before first
                    if(progressValue > 0)
                    {
                        //go back 1
                        progressValue += increments;
                    }
                }

                //if no increments and listener is set
                if(increments == 0 && progressChangedListener != null)
                {
                    //update sub progress
                    progressChangedListener.onProgressChanged(PlayBar.this, progressValue + minValue, playSubProgressPercent, true);
                }

                //update progress
                playSubProgressPercent = 0;
                seekBar.setProgress(progressValue);
            }
        });
    }

    //Creates an on speed text click listener
    private View.OnClickListener createSpeedTextOnClickListener()
    {
        return(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                String text;

                //update speed
                switch(playScaleFactor)
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
                text = playScaleFactor + PlayBar.this.getResources().getString(R.string.text_x);
                scaleText.setText(text);
            }
        });
    }
}
