package com.nikolaiapps.orbtrack;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatButton;


public class BorderButton extends AppCompatButton
{
    private int borderColor;
    private Paint borderBrush;

    public BorderButton(Context context)
    {
        super(context);
        init();
    }

    public BorderButton(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init();
    }

    public BorderButton(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setBorderColor(int color)
    {
        borderColor = Color.argb(50, Color.red(color), Color.green(color), Color.blue(color));
    }

    private void init()
    {
        Context context = this.getContext();

        setBorderColor(Settings.getDarkTheme(context) ? Color.WHITE : Color.GRAY);

        borderBrush = new Paint();
        borderBrush.setAntiAlias(true);
        borderBrush.setStrokeWidth(Globals.dpToPixels(context, 5));
        borderBrush.setStyle(Paint.Style.STROKE);
    }

    @Override
    public void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        borderBrush.setColor(borderColor);
        canvas.drawRect(0, 0, getWidth(), getHeight(), borderBrush);
    }
}
