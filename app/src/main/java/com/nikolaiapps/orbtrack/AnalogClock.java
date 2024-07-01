package com.nikolaiapps.orbtrack;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Calendar;

public class AnalogClock extends View
{
    private int hour;
    private int minute;
    private int halfWidth;
    private int halfHeight;
    private final int clockColor;
    private final int hourHandColor;
    private float radius;
    private float handThickness;
    private float frameThickness;
    private float hourHandLength;
    private float minuteHandLength;
    private final Paint drawBrush;

    public AnalogClock(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes)
    {
        super(context, attrs, defStyleAttr, defStyleRes);

        int[] colors;

        hour = 4;
        minute = 0;
        halfWidth = halfHeight = 0;
        radius = hourHandLength = minuteHandLength = 0;
        colors = Globals.resolveColorIDs(context, android.R.attr.textColor, R.attr.colorAccent);
        clockColor = colors[0];
        hourHandColor = colors[1];

        drawBrush = new Paint();
        drawBrush.setAntiAlias(true);
        drawBrush.setStyle(Paint.Style.STROKE);
    }
    public AnalogClock(Context context, @Nullable AttributeSet attrs, int defStyleAttr)
    {
        this(context, attrs, defStyleAttr, 0);
    }
    public AnalogClock(Context context, @Nullable AttributeSet attrs)
    {
        this(context, attrs, 0);
    }
    public AnalogClock(Context context)
    {
        this(context, null);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas)
    {
        super.onDraw(canvas);

        drawBrush.setColor(clockColor);
        drawBrush.setStrokeWidth(frameThickness);
        canvas.drawCircle(halfWidth, halfHeight, radius, drawBrush);

        drawBrush.setStrokeWidth(handThickness);
        drawHand(canvas, halfWidth, halfHeight, minuteHandLength, minute, 60);

        drawBrush.setColor(hourHandColor);
        drawHand(canvas, halfWidth, halfHeight, hourHandLength, hour + (minute / 60f), 12);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight)
    {
        float minSize;

        halfWidth = width / 2;
        halfHeight = height / 2;
        minSize = Math.min(halfWidth, halfHeight);
        handThickness = minSize * 0.25f;
        frameThickness = minSize * 0.2f;
        radius = minSize - (frameThickness / 2) - (minSize * 0.175f);
        hourHandLength = (radius * 0.6f);
        minuteHandLength = (radius * 0.75f);

        super.onSizeChanged(width, height, oldWidth, oldHeight);
    }

    private void drawHand(Canvas canvas, int centerX, int centerY, float length, float value, int max)
    {
        float usedValue = value % max;
        double angle = Math.toRadians(((usedValue / (float)max) * 360) - 90);
        double xCos = Math.cos(angle);
        double ySin = Math.sin(angle);
        double backLength = (length * 0.14);
        float startX = centerX - (float)(xCos * backLength);
        float startY = centerY - (float)(ySin * backLength);
        float endX = centerX + (float)(xCos * length);
        float endY = centerY + (float)(ySin * length);

        canvas.drawLine(startX, startY, endX, endY, drawBrush);
    }

    public void setTime(int hour, int minute)
    {
        if(this.hour != hour || this.minute != minute)
        {
            this.hour = hour;
            this.minute = minute;
            this.invalidate();
        }
    }
    public void setTime(Calendar time)
    {
        if(time != null)
        {
            setTime(time.get(Calendar.HOUR_OF_DAY), time.get(Calendar.MINUTE));
        }
    }
}
