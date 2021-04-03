package com.nikolaiapps.orbtrack;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import androidx.appcompat.widget.AppCompatImageView;
import android.util.AttributeSet;


public class CursorImageView extends AppCompatImageView
{
    /*public interface OnCursorChangedListener
    {
        void onCursorChanged(float x, float y);
    }*/

    private boolean fixedX;
    private boolean fixedY;
    private int layoutWidth;
    private int layoutHeight;
    private float cursorX;
    private float cursorY;
    private float cursorRadius;
    private float cursorInnerRadius;
    private float cursorOuterRadius;
    private Paint cursorPaint;
    private Paint cursorBorder;
    //private OnCursorChangedListener cursorChangedListener;

    public CursorImageView(Context context)
    {
        super(context);
        init(context);
    }
    public CursorImageView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(context);
    }
    public CursorImageView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context)
    {
        float[] dpPixels = Globals.dpsToPixels(context, 2, 2, 4, 5, 6);
        float cursorThickness = dpPixels[0];
        float cursorBorderThickness = dpPixels[1];

        fixedX = fixedY = false;
        layoutWidth = layoutHeight = -1;
        cursorX = cursorY = 0;

        cursorInnerRadius = dpPixels[2];
        cursorRadius = dpPixels[3];
        cursorOuterRadius = dpPixels[4];

        cursorPaint = new Paint();
        cursorPaint.setAntiAlias(true);
        cursorPaint.setStyle(Paint.Style.STROKE);
        cursorPaint.setStrokeWidth(cursorThickness);
        cursorPaint.setColor(Color.WHITE);

        cursorBorder = new Paint();
        cursorBorder.setAntiAlias(true);
        cursorBorder.setStyle(Paint.Style.STROKE);
        cursorBorder.setStrokeWidth(cursorBorderThickness);
        cursorBorder.setColor(Color.BLACK);
    }

    @Override
    public void onDraw(Canvas canvas)
    {
        //draw background
        super.onDraw(canvas);

        //draw cursor
        canvas.drawCircle(cursorX, cursorY, cursorInnerRadius, cursorBorder);
        canvas.drawCircle(cursorX, cursorY, cursorOuterRadius, cursorBorder);
        canvas.drawCircle(cursorX, cursorY, cursorRadius, cursorPaint);
    }

    @Override
    public boolean performClick()
    {
        return(super.performClick());
    }

    public void setFixed(boolean fixX, boolean fixY)
    {
        fixedX = fixX;
        fixedY = fixY;
    }

    public float getCursorX()
    {
        return(cursorX);
    }

    public float getCursorY()
    {
        return(cursorY);
    }

    public void setCursor(float x, float y)
    {
        if(layoutWidth == -1)
        {
            layoutWidth = this.getLayoutParams().width;
        }
        if(layoutHeight == -1)
        {
            layoutHeight = this.getLayoutParams().height;
        }

        cursorX = (fixedX ? (int)(layoutWidth / 2f) : x);
        cursorY = (fixedY ? (int)(layoutHeight / 2f) : y);

        /*if(cursorChangedListener != null)
        {
            cursorChangedListener.onCursorChanged(cursorX, cursorY);
        }*/

        this.invalidate();
    }

    /*public void setOnCursorChangedListener(OnCursorChangedListener listener)
    {
        cursorChangedListener = listener;
    }*/
}
