package com.nikolaiapps.orbtrack;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;


public class Graph extends View
{
    static final double LongestJulianDate = Calculations.julianDate(1, 1, 10000, 7, 59, 59);        //note: GMT of 12/31/9999 11:59:59 PM

    @SuppressWarnings("unused")
    static abstract class UnitType
    {
        static final byte Number = 1;
        static final byte JulianDate = 2;
        static final byte TimeDay = 3;
        static final byte Degree = 4;
        static final byte Speed = 5;
        static final byte Precipitation = 6;
    }

    static abstract class SelectType
    {
        static final byte Circle = 1;
        static final byte Area = 2;
        static final byte Image = 3;
    }

    public interface OnScrollListener
    {
        void onScrolled(float percent, boolean fromUser);
    }

    public interface OnSetItemsListener
    {
        void onSetItems(List<String> names, List<String> ids, List<Double> xMin, List<Double> xMax, List<Double> yMin, List<Double> yMax);
    }

    private static class Item
    {
        private int color;
        final boolean useArc;
        boolean updated;
        final double xMin;
        final double xMax;
        double yMin;
        double yMax;
        final String name;
        final String bssid;
        final Paint fillPaint;
        final Paint borderPaint;
        final Paint textPaint;

        Item(String name, String bssid, double xMin, double xMax, double yMin, double yMax, float textSize, boolean useArc)
        {
            this.name = name;
            this.bssid = bssid;
            this.xMin = xMin;
            this.xMax = xMax;
            this.yMin = yMin;
            this.yMax = yMax;
            this.color = Color.GRAY;

            fillPaint = new Paint();
            fillPaint.setAntiAlias(true);
            fillPaint.setStyle(Paint.Style.FILL);

            borderPaint = new Paint();
            borderPaint.setAntiAlias(true);
            borderPaint.setStrokeWidth(3);
            borderPaint.setStyle(Paint.Style.STROKE);

            textPaint = new Paint();
            textPaint.setAntiAlias(true);
            textPaint.setStyle(Paint.Style.FILL);
            textPaint.setTextSize(textSize);

            this.useArc = useArc;
            this.updated = true;
        }

        public int getColor()
        {
            return(this.color);
        }

        public void setColor(int color)
        {
            this.color = color;
            fillPaint.setColor(Globals.getColor(50, color));
            borderPaint.setColor(color);
            textPaint.setColor(color);
        }

        @Override
        public boolean equals(Object o1)
        {
            if(o1 instanceof Item)
            {
                Item compareItem = (Item)o1;
                return(name.equals(compareItem.name) && bssid.equals(compareItem.bssid));
            }
            else
            {
                return(false);
            }
        }
    }

    private static class Divisor
    {
        final double value;
        final String text;
        final Bitmap image;
        final Bitmap subImage1;
        final Bitmap subImage2;

        Divisor(String text, double value, Bitmap image, Bitmap subImage1, Bitmap subImage2)
        {
            this.text = text;
            this.value = value;
            this.image = image;
            this.subImage1 = subImage1;
            this.subImage2 = subImage2;
        }
    }

    private static final int[] itemColorList = new int[]{R.color.red, R.color.orange, R.color.yellow, R.color.green, R.color.blue, R.color.purple, R.color.red_300, R.color.orange_300, R.color.yellow_300, R.color.green_300, R.color.blue_300, R.color.purple};
    private int itemColorIndex;

    private int width;
    private int height;
    private int lineColor;
    private int fillColor;
    private int titleColor;
    private int screenWidth;
    private int gridAxisColor;
    private int backgroundColor;
    private int xValueHeight;
    private int yValueHeight;
    private int valuePadding;
    private int titlePadding;
    private int selectedWidth;
    private int xValueWidth;
    private int yValueWidth;
    private int xTitleWidth;
    private int yTitleWidth;
    private int yTitleHeight;
    private int xAxisDivisions;
    private int yAxisDivisions;
    private int valueDecimalPlaces;
    private int xAxisDivisionTextLines;
    private int xAxisDivisionImageTopOffset;
    private int xAxisDivisionImageBottomOffset;
    private int xAxisDivisionImageTitleWidest;
    private byte xUnits;
    private byte yUnits;
    private byte selectType;
    private boolean isPreview;
    private boolean allowScroll;
    private boolean allowUpdate;
    private boolean allowParentTouch;
    private boolean skipRepeatDays;
    private boolean showDataTitles;
    private boolean showGraphBorder;
    private boolean yDivisorTitlesVisible;
    private boolean xAxisDivisionLinesVisible;
    private boolean yAxisDivisionLinesVisible;
    private boolean updateScrollDisplays;
    private double xMin;
    private double xMax;
    private double yMin;
    private double yMax;
    private double xSpan;
    private double ySpan;
    private double xSelected;
    private double dataScaleX;
    private double dataScaleY;
    private double showDataTitlesMinYValue;
    private float textSizeSmall;
    private float textSizeMedium;
    private float textSizeLarge;
    private float textSizeSmallScale;
    private float startX;
    private float startY;
    private float deltaX;
    private float deltaY;
    private float deltaYScroll;
    private float deltaYScrollPercent;
    private Rect graphArea;
    private Paint drawBrush;
    private Paint measureBrush;
    private Path fillPath;
    private String xTitle;
    private String yTitle;
    private TimeZone valuesZone;
    private Bitmap backgroundImage;
    private Bitmap selectedImage;
    private Bitmap selectedImage2;
    private Bitmap userSelectedImage;
    private Bitmap userSelectedImage2;
    private OnScrollListener scrollListener;
    private OnSetItemsListener setItemsListener;
    private List<Integer> fillColors;
    private List<Double> xPoints;
    private List<Double> yPoints;
    private List<Double> y2Points;
    private List<Double> yPointsBottom;
    private int[] xAxisDivisionImageHeight;
    private int[] xAxisDivisionImageTitlesWidth;
    private int[] xAxisDivisionImageTitlesHeight;
    private float[] linePoints;
    private float[] line2Points;
    private float[] linePointsBottom;
    private Bitmap[] linePointsImages;
    private String[] xAxisDivisionImageTitles;
    private Divisor[] xAxisDivisors;
    private Divisor[] yAxisDivisors;
    private ArrayList<Item> items;

    public void baseConstructor(AttributeSet attrs)
    {
        Drawable backgroundDrawable;
        ColorDrawable backgroundColorDrawable;
        float[] sizes;

        sizes = Globals.dpsToPixels(this.getContext(), 10, 15, 20, 14, 4, 4, 80);
        textSizeSmall = sizes[0];
        textSizeMedium = sizes[1];
        textSizeLarge = sizes[2];
        selectedWidth = (int)sizes[3];
        valuePadding = (int)sizes[4];
        titlePadding = (int)sizes[5];
        deltaYScroll = sizes[6];
        deltaYScrollPercent = 1;
        textSizeSmallScale = xAxisDivisionTextLines = 1;
        xAxisDivisionImageTopOffset = xAxisDivisionImageBottomOffset = xAxisDivisionImageTitleWidest = 0;
        xAxisDivisions = yAxisDivisions = 6;
        showDataTitlesMinYValue = -Double.MAX_VALUE;
        xSelected = Double.MAX_VALUE;
        valueDecimalPlaces = 2;

        itemColorIndex = 0;
        width = height = 100;
        deltaX = deltaY = 0;
        graphArea = new Rect();
        backgroundImage = selectedImage = selectedImage2 = userSelectedImage = userSelectedImage2 = null;

        if(attrs != null)
        {
            try(@SuppressLint({"NewApi", "LocalSuppress"}) TypedArray valueArray = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.Graph, 0, 0))
            {
                textSizeSmall = valueArray.getDimension(R.styleable.Graph_dataTextSize, textSizeSmall);
                valueArray.recycle();
            }
            catch(NoSuchMethodError | Exception noMethod)
            {
                //do nothing
            }
        }

        backgroundDrawable = this.getBackground();
        if(backgroundDrawable instanceof ColorDrawable)
        {
            backgroundColorDrawable = (ColorDrawable)backgroundDrawable;
            backgroundColor = backgroundColorDrawable.getColor();
            backgroundColor = Globals.getColor(220, backgroundColor);
        }
        else
        {
            backgroundColor = -1;
        }

        measureBrush = createBrush();
        drawBrush = createBrush();
        setDataTextScale(textSizeSmallScale);

        fillPath = new Path();
        fillColors = null;
        xPoints = null;
        yPoints = null;
        y2Points = null;
        yPointsBottom = null;
        linePoints = null;
        line2Points = null;
        linePointsBottom = null;
        linePointsImages = null;
        xAxisDivisors = null;
        yAxisDivisors = null;
        items = new ArrayList<>(0);
        xAxisDivisionImageHeight = new int[]{0, 0, 0};
        xAxisDivisionImageTitlesWidth = new int[]{0, 0, 0};
        xAxisDivisionImageTitlesHeight = new int[]{0, 0, 0};
        xAxisDivisionImageTitles = new String[]{null, null, null};

        setColors(Color.BLACK, Color.GRAY);
        setUnitTypes(UnitType.Number, UnitType.Number);
        setTitles("X Title", "Y Title");
        setData(new ArrayList<>(0), new ArrayList<>(0), TimeZone.getDefault());

        selectType = SelectType.Circle;
        allowScroll = isPreview = showDataTitles = allowParentTouch = false;
        allowUpdate = skipRepeatDays = yDivisorTitlesVisible = xAxisDivisionLinesVisible = yAxisDivisionLinesVisible = showGraphBorder = updateScrollDisplays = true;
        this.setClickable(true);
        this.setFocusable(true);
    }

    public Graph(Context context)
    {
        super(context);
        baseConstructor(null);
    }
    public Graph(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        baseConstructor(attrs);
    }

    public void setScrollX(int value, boolean fromUser)
    {
        //update scroll
        this.setScrollX(value);

        //if allow scrolling
        if(allowScroll)
        {
            //need to update displays
            updateScrollDisplays = true;
        }

        //if listener is set
        if(scrollListener != null)
        {
            //percent of width with title offset
            scrollListener.onScrolled((value - yTitleHeight - titlePadding) / (float)width, fromUser);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);

        deltaX = deltaY = 0;
        setScrollX(0, false);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        boolean isDown;
        int action = event.getAction();
        int maxScroll;
        float currentX;
        float currentY;
        final ViewParent parent = this.getParent();

        //if allow scrolling or is preview
        if(allowScroll || isPreview)
        {
            switch(action)
            {
                case MotionEvent.ACTION_DOWN:
                    startX = event.getX();
                    startY = event.getY();
                    deltaY = 0;
                    if(isPreview)
                    {
                        deltaX = 0;
                    }
                    //fall through

                case MotionEvent.ACTION_UP:
                    //if parent exists
                    if(parent != null)
                    {
                        //prevent parent scrolling if down
                        isDown = (action == MotionEvent.ACTION_DOWN);
                        if(!allowParentTouch)
                        {
                            parent.requestDisallowInterceptTouchEvent(isDown);
                        }
                        if(isDown)
                        {
                            performClick();
                        }
                    }
                    break;

                case MotionEvent.ACTION_MOVE:
                    //get current
                    currentX = event.getX();
                    currentY = event.getY();

                    //if allow scrolling or within preview area
                    if(allowScroll || (currentX >= xSelected && currentX <= (xSelected + selectedWidth)))
                    {
                        //update deltas
                        deltaX += (allowScroll ? (startX - currentX) : (currentX - startX));
                        deltaY += (allowScroll ? (startY - currentY) : (currentY - startY));

                        if(Math.abs(deltaY) >= (deltaYScroll * deltaYScrollPercent) && parent != null)
                        {
                            parent.requestDisallowInterceptTouchEvent(false);
                        }

                        //if allow scrolling
                        if(allowScroll)
                        {
                            maxScroll = ((width - screenWidth) - (Math.max(xValueWidth, xAxisDivisionImageTitleWidest) / 2)) + getPaddingStart();

                            if(maxScroll >= 0)
                            {
                                if(deltaX < 0)
                                {
                                    deltaX = 0;
                                }
                                else if(deltaX > maxScroll)
                                {
                                    deltaX = maxScroll;
                                }

                                setScrollX((int)deltaX, true);
                            }
                        }
                        //else for preview
                        else
                        {
                            //if before start of graph and Y axis title
                            if(xSelected + deltaX + yTitleHeight + titlePadding < graphArea.left)
                            {
                                deltaX = graphArea.left - (float)xSelected - yTitleHeight - titlePadding;
                            }
                            //else if after graph and selection bar end
                            else if(xSelected + deltaX + selectedWidth > graphArea.right)
                            {
                                deltaX = graphArea.right - (float)xSelected - selectedWidth;
                            }

                            setSelectedX(xSelected + deltaX);

                            //if listener is set
                            if(scrollListener != null)
                            {
                                //percent of width with title offset
                                scrollListener.onScrolled((float)(xSelected + yTitleHeight + titlePadding) / width, true);
                            }
                        }

                        //update starting
                        startX = currentX;
                        startY = currentY;
                    }
                    break;
            }
        }

        return(super.onTouchEvent(event));
    }

    @Override
    public boolean performClick()
    {
        return(super.performClick());
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH)
    {
        width = w;
        height = h;
        screenWidth = Globals.getDevicePixels(this.getContext())[0];

        if(allowUpdate)
        {
            updateBackgroundImage();
        }
        updateSelectedImage();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        int measureWidth = resolveSizeAndState(getPaddingLeft() + getPaddingRight() + 100, widthMeasureSpec, 1);
        int measureHeight = resolveSizeAndState(getPaddingTop() + getPaddingBottom() + 400, heightMeasureSpec, 1);

        setMeasuredDimension(measureWidth, measureHeight);
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        int index;
        boolean scrolling = (allowScroll && updateScrollDisplays);
        boolean showDataImages = (showDataTitles && linePointsImages != null && linePointsImages.length == items.size());
        float y;
        float textHeight;
        double x;
        RectF itemArea;
        double[] selectedPoints;

        //if image is set
        if(backgroundImage != null)
        {
            //draw background
            canvas.drawBitmap(backgroundImage, 0, 0, drawBrush);

            //if there are items
            if(!items.isEmpty())
            {
                //save canvas and clip area
                canvas.save();
                canvas.clipRect(graphArea);

                //go through each item
                index = 0;
                for(Item currentItem : items)
                {
                    //draw item
                    itemArea = getScaledArea(currentItem);
                    if(currentItem.yMin != currentItem.yMax)
                    {
                        if(currentItem.useArc)
                        {
                            canvas.drawArc(itemArea, 180, 180, true, currentItem.fillPaint);
                            canvas.drawArc(itemArea, 180, 180, true, currentItem.borderPaint);
                        }
                        else
                        {
                            canvas.drawRect(itemArea, currentItem.fillPaint);
                            canvas.drawRect(itemArea, currentItem.borderPaint);
                        }
                        if(selectType != SelectType.Area)
                        {
                            if(showDataTitles)
                            {
                                x = getScaledX( currentItem.xMin + ((currentItem.xMax - currentItem.xMin) / 2));
                                if(currentItem.yMax >= showDataTitlesMinYValue)
                                {
                                    drawXText((showDataImages ? linePointsImages[index] : null), getYValueString(currentItem.yMax), x, (int)itemArea.top, canvas, currentItem.textPaint);
                                }
                                if(currentItem.yMin >= showDataTitlesMinYValue)
                                {
                                    drawXText(getYValueString(currentItem.yMin), x, (int)itemArea.bottom + yValueHeight, canvas, currentItem.textPaint);
                                }
                            }
                            else
                            {
                                textHeight = Globals.getTextHeight(currentItem.textPaint, currentItem.name);
                                y = (itemArea.top - (textHeight / 2f));
                                if(y < graphArea.top + textHeight)
                                {
                                    y = graphArea.top + textHeight;
                                }

                                canvas.drawText(currentItem.name, (int)(itemArea.centerX() - (Globals.getTextWidth(currentItem.textPaint, currentItem.name) / 2f)), y, currentItem.textPaint);
                            }
                        }
                    }
                    index++;
                }

                //restore canvas
                canvas.restore();
            }

            //if there is a selection and image is set
            if(xSelected != Double.MAX_VALUE && selectedImage != null)
            {
                //save canvas and set clipped area
                canvas.save();
                canvas.clipRect(graphArea);

                //if selected is an area
                if(selectType == SelectType.Area)
                {
                    //draw selection
                    canvas.drawBitmap(selectedImage, (float)xSelected, 1.0f, drawBrush);
                }
                else
                {
                    //go through selected images
                    for(index = 0; index < 2; index++)
                    {
                        List<Double> currentYPoints = (index == 0 ? yPoints : y2Points);
                        Bitmap currentImage = (index == 0 ? selectedImage : selectedImage2);

                        //if points and image are set
                        if(currentYPoints != null && currentImage != null)
                        {
                            //if found closest point
                            selectedPoints = getClosestY(currentYPoints, xSelected);
                            if(selectedPoints != null)
                            {
                                //draw selection
                                canvas.drawBitmap(currentImage, (float)getScaledX(selectedPoints[0]) - (int)(currentImage.getWidth() / 2f), (float)getScaledY(selectedPoints[1]) - (int)(currentImage.getHeight() / 2f), drawBrush);
                            }
                        }
                    }
                }

                //restore canvas
                canvas.restore();
            }

            //if scrolling
            if(scrolling)
            {
                if(yDivisorTitlesVisible && deltaX > 0 && backgroundColor != -1)
                {
                    drawBrush.setColor(backgroundColor);
                    drawBrush.setStyle(Paint.Style.FILL);
                    canvas.drawRect(0, 0, deltaX + graphArea.left, height, drawBrush);
                }

                //draw titles
                drawXTitle(canvas, drawBrush, true);
                drawYTitle(canvas, drawBrush, true);

                //draw dividers
                if(yDivisorTitlesVisible)
                {
                    drawXDividers(canvas, drawBrush, false);
                    drawYDividers(canvas, drawBrush, false, true, true);
                }
            }
        }
    }

    @Override
    public void setVisibility(int visibility)
    {
        updateScrollDisplays = true;
        super.setVisibility(visibility);
    }

    private Paint createBrush()
    {
        Paint brush = new Paint();
        brush.setAntiAlias(true);
        brush.setTextSize(getTextSmallSize());
        return(brush);
    }

    public synchronized void refresh()
    {
        if(allowUpdate)
        {
            updateBackgroundImage();
            this.invalidate();
        }
    }

    private void updateSelectedImage(Bitmap image, Paint brush, float x, float y, float right, float bottom)
    {
        Canvas canvas;

        //if image is set
        if(image != null)
        {
            canvas = new Canvas(image);

            switch(selectType)
            {
                case SelectType.Area:
                    brush.setColor(fillColor);
                    brush.setStyle(Paint.Style.FILL);
                    canvas.drawRect(x, y, right, bottom, brush);
                    brush.setColor(lineColor);
                    brush.setStyle(Paint.Style.STROKE);
                    canvas.drawRect(x, y, right, bottom, brush);
                    break;

                case SelectType.Image:
                    canvas.drawBitmap(image, x, y, brush);
                    break;

                default:
                case SelectType.Circle:
                    brush.setStrokeWidth(3);
                    brush.setColor(lineColor);
                    brush.setStyle(Paint.Style.FILL);
                    canvas.drawCircle(x, y, selectedWidth / 2f, brush);
                    brush.setColor(titleColor);
                    brush.setStyle(Paint.Style.STROKE);
                    canvas.drawCircle(x, y, selectedWidth / 2f, brush);
                    break;
            }
        }
    }
    private synchronized void updateSelectedImage()
    {
        int width = selectedWidth + 4;
        int height = selectedWidth + 4;
        float x;
        float y;
        float right = 0;
        float bottom = 0;
        Paint brush = createBrush();

        //set properties and images
        switch(selectType)
        {
            case SelectType.Area:
                x = graphArea.left;
                y = graphArea.top;
                right = selectedWidth;
                bottom = graphArea.bottom - 1;
                selectedImage = Bitmap.createBitmap((int)right, (int)bottom, Bitmap.Config.ARGB_8888);
                selectedImage2 = null;
                break;

            case SelectType.Image:
                if(userSelectedImage != null)
                {
                    selectedImage = Bitmap.createScaledBitmap(userSelectedImage, width, height, true);
                }
                if(userSelectedImage2 != null)
                {
                    selectedImage2 = Bitmap.createScaledBitmap(userSelectedImage2, width, height, true);
                }
                x = 0;
                y = 0;
                break;

            default:
            case SelectType.Circle:
                x = y = (selectedWidth / 2f) + 2;
                selectedImage = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                selectedImage2 = (y2Points != null ? Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888) : null);
                break;
        }

        //draw selections
        updateSelectedImage(selectedImage, brush, x, y, right, bottom);
        updateSelectedImage(selectedImage2, brush, x, y, right, bottom);
    }

    private void drawXText(Bitmap image, String value, double x, int textY, Canvas canvas, Paint brush)
    {
        int index = 0;
        int currentX;
        int textWidth;
        int textHeight = Globals.getTextHeight(brush, "0");
        int imageWidth = (image != null ? image.getWidth() : 0);
        String[] lines = value.split("\r\n");

        //set color
        brush.setColor(titleColor);

        //draw text
        for(String line : lines)
        {
            textWidth = Globals.getTextWidth(brush, line);
            currentX = (int)(x - (textWidth / 2)) + (imageWidth / 2);
            canvas.drawText(line, currentX, textY, brush);
            if(index == 0 && imageWidth > 0)
            {
                canvas.drawBitmap(image, (int)(x - (textWidth / 2f) - (imageWidth / 2f)), textY - textHeight, brush);
            }

            textY += (xValueHeight / xAxisDivisionTextLines);
            index++;
        }
    }
    private void drawXText(String value, double x, int textY, Canvas canvas, Paint brush)
    {
        drawXText(null, value, x, textY, canvas, brush);
    }
    private void drawXText(String value, double x, Canvas canvas, Paint brush)
    {
        drawXText(value, x, graphArea.bottom + (xValueHeight / xAxisDivisionTextLines), canvas, brush);
    }
    private String drawXText(double value, double x, Canvas canvas, Paint brush, String lastdayString)
    {
        int textY;
        String valueString;

        //set color
        brush.setColor(titleColor);

        //draw text
        textY = graphArea.bottom + (xValueHeight / xAxisDivisionTextLines);

        if(xUnits == UnitType.JulianDate)
        {
            valueString = getDayString(value, valuesZone);
            if(!skipRepeatDays || !valueString.equals(lastdayString))
            {
                canvas.drawText(valueString, (int)(x - (Globals.getTextWidth(brush, valueString) / 2)), textY, brush);
            }
            lastdayString = valueString;
            textY += (xValueHeight / xAxisDivisionTextLines);
        }
        valueString = getXValueString(value);
        canvas.drawText(valueString, (int)(x - (Globals.getTextWidth(brush, valueString) / 2)), textY, brush);

        return(lastdayString);
    }

    private int getXImageY(int subImageNum)
    {
        return(graphArea.bottom + xValueHeight + xAxisDivisionImageTopOffset + (subImageNum >= 1 ? (xAxisDivisionImageHeight[0] + xAxisDivisionImageBottomOffset) : 0) + (subImageNum >= 2 ? xAxisDivisionImageHeight[1] : 0));
    }

    private void drawXImage(Bitmap image, double x, Canvas canvas, Paint brush, int subImageNum)
    {
        if(image != null)
        {
            canvas.drawBitmap(image, (float)(x - (image.getWidth() / 2f)), getXImageY(subImageNum), brush);
        }
    }

    private void drawXImageTitle(int subImageNum, Canvas canvas, Paint brush, boolean forScroll)
    {
        int subIndex = (subImageNum >= 0 && subImageNum <= 2 ? subImageNum : 0);
        int textX = ((xAxisDivisionImageTitleWidest / 2) - (xAxisDivisionImageTitlesWidth[subIndex] / 2)) + (forScroll ? (int)deltaX : 0) + this.getPaddingLeft();
        String title = xAxisDivisionImageTitles[subIndex];

        if(title != null)
        {
            //set color
            brush.setColor(titleColor);

            //draw text
            canvas.drawText(title, textX, getXImageY(subIndex) + xAxisDivisionImageTitlesHeight[subIndex] + (int)((xAxisDivisionImageHeight[subIndex] / 2f) - (xAxisDivisionImageTitlesHeight[subIndex] / 2f)), brush);
        }
    }

    private void drawYText(String value, double y, Canvas canvas, Paint brush, boolean forScroll)
    {
        //set color
        brush.setColor(titleColor);

        //draw text
        canvas.drawText(value, (graphArea.left - Globals.getTextWidth(brush, value)) + (forScroll ? deltaX : 0), (int)(y + (yValueHeight / 2)), brush);
    }
    private void drawYText(double value, double y, Canvas canvas, Paint brush, boolean forScroll)
    {
        //draw text
        drawYText(getYValueString(value), y, canvas, brush, forScroll);
    }

    private void drawXDivision(int index, int divisionCount, double x, Canvas canvas, Paint brush, boolean dim)
    {
        //if showing division lines and not on first or last
        if(xAxisDivisionLinesVisible && index > 0 && index < divisionCount)
        {
            //set color
            brush.setColor(dim ? Globals.getColor(130, gridAxisColor) : gridAxisColor);

            //draw vertical line
            canvas.drawLine((int)x, graphArea.top, (int)x, graphArea.bottom, brush);
        }
    }

    private void drawYDivision(int index, int divisionCount, double y, Canvas canvas, Paint brush)
    {
        //if showing division lines and not on first or last
        if(yAxisDivisionLinesVisible && index > 0 && index < divisionCount)
        {
            //set color
            brush.setColor(gridAxisColor);

            //draw horizontal line
            canvas.drawLine(graphArea.left, (int)y, graphArea.right, (int)y, brush);
        }
    }

    private void drawXTitle(Canvas canvas, Paint brush, boolean forScroll)
    {
        if(xTitle != null)
        {
            brush.setColor(titleColor);
            brush.setStrokeWidth(1);
            brush.setTextSize(textSizeLarge);
            canvas.drawText(xTitle, (forScroll ? ((screenWidth / 2f) + deltaX) : graphArea.exactCenterX()) - (int)(xTitleWidth / 2f), height, brush);
        }
    }

    private void drawYTitle(Canvas canvas, Paint brush, boolean forScroll)
    {
        if(yTitle != null)
        {
            brush.setColor(titleColor);
            brush.setStrokeWidth(1);
            brush.setTextSize(textSizeLarge);

            canvas.save();
            canvas.translate(yTitleHeight / 2f, graphArea.centerY());
            canvas.rotate(-90);
            canvas.drawText(yTitle, (int)(yTitleWidth / -2f), (int)(yTitleHeight / 2f) + (forScroll ? deltaX : 0), brush);
            canvas.restore();
        }
    }

    private void drawXDividers(Canvas canvas, Paint brush, boolean forBackground)
    {
        int index;
        boolean dim;
        boolean onLast;
        double x;
        double value;
        double gridSpaceX = 0;
        double lastX = -Double.MAX_VALUE;
        double overlapX;
        String lastDayString = null;

        brush.getColor();
        brush.setStrokeWidth(1);
        brush.setStyle(Paint.Style.FILL);
        brush.setTextSize(getTextSmallSize());

        if(xAxisDivisions > 0)
        {
            gridSpaceX = (xSpan / xAxisDivisions);
        }
        if(gridSpaceX > 0)
        {
            if(forBackground)
            {
                value = xMin;
                for(index = 0; index <= xAxisDivisions; index++)
                {
                    //remember if on last, get x, and reset dimming
                    onLast = (index + 1 > xAxisDivisions);
                    x = getScaledX(value);
                    dim = false;

                    //measure any overlap
                    overlapX = (lastX >= 0 ? ((x - (xValueWidth / 2.0)) - (lastX + (xValueWidth / 2.0))) : 0);

                    //draw lines and text
                    if(onLast)
                    {
                        //always allow for last
                        lastDayString = null;
                    }

                    //if there is no overlap or on last
                    if(overlapX >= 0 || onLast)
                    {
                        //draw divison text
                        lastDayString = drawXText(value, x, canvas, brush, lastDayString);
                        lastX = x;
                    }
                    else
                    {
                        dim = true;
                    }

                    //draw division line
                    drawXDivision(index, xAxisDivisions, x, canvas, brush, dim);

                    //go to next
                    value += gridSpaceX;
                }
            }
        }
        else if(xAxisDivisors != null && xAxisDivisors.length > 0)
        {
            if(forBackground)
            {
                //go through each divisor
                for(index = 0; index < xAxisDivisors.length; index++)
                {
                    //remember current divisor
                    Divisor currentDivisor = xAxisDivisors[index];

                    //get x
                    x = getScaledX(currentDivisor.value);

                    //draw division, text, and image
                    drawXDivision((index == 0 ? 1 : index), xAxisDivisors.length, x, canvas, brush, false);        //note: still drawing first
                    drawXText(currentDivisor.text, x, canvas, brush);
                    drawXImage(currentDivisor.image, x, canvas, brush, 0);
                    drawXImage(currentDivisor.subImage1, x, canvas, brush, 1);
                    drawXImage(currentDivisor.subImage2, x, canvas, brush, 2);
                }
            }

            if(forBackground && !allowScroll || !forBackground && allowScroll)
            {
                //draw each division title
                for(index = 0; index <= 2; index++)
                {
                    drawXImageTitle(index, canvas, brush, allowScroll);
                }
            }
        }
    }

    private void drawYDividers(Canvas canvas, Paint brush, boolean drawLines, boolean drawTexts, boolean forScroll)
    {
        int index;
        double y;
        double value;
        double gridSpaceY = 0;

        brush.setStrokeWidth(1);
        brush.setStyle(Paint.Style.FILL);
        brush.setTextSize(getTextSmallSize());

        if(yAxisDivisions > 0)
        {
            gridSpaceY = (ySpan / yAxisDivisions);
        }
        if(gridSpaceY > 0)
        {
            value = yMin;
            for(index = 0; index <= yAxisDivisions; index++)
            {
                //get y
                y = getScaledY(value);

                //if drawing lines
                if(drawLines)
                {
                    //draw division
                    drawYDivision(index, yAxisDivisions, y, canvas, brush);
                }

                //if drawing texts
                if(drawTexts)
                {
                    //draw text
                    drawYText(value, y, canvas, brush, forScroll);
                }

                //go to next
                value += gridSpaceY;
            }
        }
        else if(yAxisDivisors != null && yAxisDivisors.length > 0)
        {
            for(index = 0; index < yAxisDivisors.length; index++)
            {
                //remember current divisor
                Divisor currentDivisor = yAxisDivisors[index];

                //get y
                y = getScaledY(currentDivisor.value);

                //if drawing lines
                if(drawLines)
                {
                    //draw division
                    drawYDivision((index == 0 ? 1 : index), yAxisDivisors.length, y, canvas, brush);        //note: still drawing first
                }

                //if drawing texts
                if(drawTexts)
                {
                    //draw text
                    drawYText(currentDivisor.text, y, canvas, brush, forScroll);
                }
            }
        }
    }

    private void drawLinePoints(Canvas canvas, Paint brush, List<Double> yDrawPoints, float[] lineDrawPoints, List<Double> yDrawPointsBottom, float[] lineDrawPointsBottom)
    {
        int dataCount = (xPoints != null ? xPoints.size() : 0);
        int lineCount = (lineDrawPoints != null ? lineDrawPoints.length : 0);
        int yPointCount = (yDrawPoints != null ? yDrawPoints.size() : 0);
        boolean usingFillColors = (fillColors != null);
        boolean usingYBottom = (yDrawPointsBottom != null);
        boolean showDataImages = (showDataTitles && linePointsImages != null && linePointsImages.length == yPointCount);
        int index;
        int pointOffset;
        int firstX = 0;
        int currentColor;
        int startColor = lineColor;
        int lastColor = (usingFillColors ? Integer.MIN_VALUE : lineColor);
        int fillColorsLength = (usingFillColors ? fillColors.size() : 0);
        double x = 0;
        double y = 0;
        double yBottom = 0;

        //if there are lines to draw
        if(dataCount > 0 && yPointCount >= dataCount && lineCount > 0)
        {
            //draw data
            fillPath.reset();
            canvas.save();
            canvas.clipRect(graphArea);
            for(index = 0; index < dataCount; index++)
            {
                //get point offset
                pointOffset = (index - 1) * 4;

                //if after first point and within bounds
                if(index > 0 && (pointOffset + 1) < lineDrawPoints.length)
                {
                    //add last points
                    lineDrawPoints[pointOffset] = (float)x;
                    lineDrawPoints[pointOffset + 1] = (float)y;
                    if(usingYBottom)
                    {
                        lineDrawPointsBottom[pointOffset] = (float)x;
                        lineDrawPointsBottom[pointOffset + 1] = (float)yBottom;
                    }
                }

                //get current axis values and color
                x = getScaledX(xPoints.get(index));
                y = getScaledY(yDrawPoints.get(index));
                if(usingYBottom)
                {
                    yBottom = getScaledY(yDrawPointsBottom.get(index));
                }
                currentColor = (index < fillColorsLength ? fillColors.get(index) : lineColor);

                //if on the first point
                if(index == 0)
                {
                    //move to start
                    fillPath.moveTo((int)x, (int)y);
                    firstX = (int)x;
                }
                else
                {
                    //move to next point in path
                    fillPath.lineTo((int)x, (int)y);

                    //if color changed
                    if(currentColor != lastColor)
                    {
                        //update color
                        setColors(titleColor, lastColor, false);

                        //move back to start of color and close path
                        fillPath.lineTo((int)x, graphArea.bottom);
                        fillPath.lineTo(firstX, graphArea.bottom);
                        fillPath.close();

                        //set color and draw
                        brush.setColor(fillColor);
                        canvas.drawPath(fillPath, brush);

                        //reset and move back to current
                        fillPath.reset();
                        fillPath.moveTo((int)x, (int)y);

                        //reset start
                        firstX = (int)x;
                    }

                    //if within bounds
                    if((pointOffset + 3) < lineDrawPoints.length)
                    {
                        //add current points
                        lineDrawPoints[pointOffset + 2] = (float)x;
                        lineDrawPoints[pointOffset + 3] = (float)y;
                        if(usingYBottom)
                        {
                            lineDrawPointsBottom[pointOffset + 2] = (float)x;
                            lineDrawPointsBottom[pointOffset + 3] = (float)yBottom;
                        }
                    }
                }

                //update last color
                lastColor = currentColor;
            }
            if(yDrawPointsBottom != null)
            {
                //go through bottom points in reverse
                for(index = dataCount - 1; index >= 0; index--)
                {
                    //get current axis values
                    x = getScaledX(xPoints.get(index));
                    y = getScaledY(yDrawPointsBottom.get(index));

                    //move to previous point in path
                    fillPath.lineTo((int)x, (int)y);
                }
            }
            else
            {
                fillPath.lineTo((int)x, graphArea.bottom);
                fillPath.lineTo(firstX, graphArea.bottom);
            }
            fillPath.close();
            brush.setColor(fillColor);
            canvas.drawPath(fillPath, brush);
            if(usingFillColors)
            {
                //set back to starting color
                setColors(titleColor, startColor, false);
            }
            brush.setColor(lineColor);
            brush.setStrokeWidth(3);
            canvas.drawLines(lineDrawPoints, brush);
            if(lineDrawPointsBottom != null)
            {
                canvas.drawLines(lineDrawPointsBottom, brush);
            }
            if(showDataTitles)
            {
                for(index = 0; index < yPointCount; index++)
                {
                    y = yDrawPoints.get(index);
                    if(y >= showDataTitlesMinYValue)
                    {
                        drawXText((showDataImages ? linePointsImages[index] : null), getYValueString(y), getScaledX(xPoints.get(index)), (int)getScaledY(y), canvas, brush);
                    }
                    if(lineDrawPointsBottom != null && yDrawPointsBottom != null)
                    {
                        y = yDrawPointsBottom.get(index);
                        if(y >= showDataTitlesMinYValue)
                        {
                            drawXText(getYValueString(y), getScaledX(xPoints.get(index)), (int)getScaledY(y) + yValueHeight, canvas, brush);
                        }
                    }
                }
            }
            canvas.restore();
        }
    }

    private synchronized void updateBackgroundImage()
    {
        boolean haveXAxisValueText = (xAxisDivisions > 0 || (xAxisDivisors != null && xAxisDivisors.length > 0 && xValueWidth > 0));
        boolean haveYAxisValueText = (yAxisDivisions > 0 || (yAxisDivisors != null && yAxisDivisors.length > 0 && yValueWidth > 0));
        Canvas canvas;
        Paint brush = createBrush();

        //if invalid data
        if(xMin >= xMax || yMin >= yMax)
        {
            //stop
            return;
        }

        //update scaling
        graphArea.set((yDivisorTitlesVisible ? Math.max(xAxisDivisionImageTitleWidest, ((yTitleHeight > 0 ? (yTitleHeight + titlePadding) : 0) + (haveYAxisValueText ? (yValueWidth + valuePadding) : (haveXAxisValueText ? (xValueWidth / 2) : 0)))) : 0) + this.getPaddingLeft(), (yValueHeight / 2) + this.getPaddingTop(), width - (haveXAxisValueText ? (xValueWidth / 2) : 0) - this.getPaddingRight(), height - (yTitleHeight > 0 ? (yTitleHeight + titlePadding) : 0) - (haveXAxisValueText ? xValueHeight : 0) - xAxisDivisionImageHeight[0] - (xAxisDivisionImageHeight[0] > 0 ? (xAxisDivisionImageTopOffset + xAxisDivisionImageBottomOffset) : 0) - xAxisDivisionImageHeight[1] - xAxisDivisionImageHeight[2] - this.getPaddingBottom());
        dataScaleX = (graphArea.width() / xSpan);
        dataScaleY = (graphArea.height() / ySpan);

        //create image
        backgroundImage = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(backgroundImage);

        //draw grid dividers
        drawXDividers(canvas, brush, true);
        drawYDividers(canvas, brush, true, !allowScroll, false);

        //draw lines points
        drawLinePoints(canvas, brush, yPoints, linePoints, yPointsBottom, linePointsBottom);
        drawLinePoints(canvas, brush, y2Points, line2Points, null, null);

        //if not scrolling
        if(!allowScroll)
        {
            //draw titles
            drawXTitle(canvas, brush, false);
            drawYTitle(canvas, brush, false);
        }

        //if showing graph border
        if(showGraphBorder)
        {
            //draw grid area
            brush.setColor(lineColor);
            brush.setStyle(Paint.Style.STROKE);
            canvas.drawRect(graphArea, brush);
        }
    }

    private double getScaledX(double value)
    {
        return((((value - xMin) * dataScaleX) + graphArea.left));
    }

    private double getScaledY(double value)
    {
        return(graphArea.bottom - ((value - yMin) * dataScaleY));
    }

    private RectF getScaledArea(Item item)
    {
        int swap;
        int left = (int)getScaledX(item.xMin);
        int top = (int)getScaledY(item.yMin);
        int right = (int)getScaledX(item.xMax);
        int bottom = (int)getScaledY(item.yMax);

        if(bottom < top)
        {
            swap = top;
            top = bottom;
            bottom = swap;
        }

        return(new RectF(left, top, right, bottom * (item.useArc ? 2 : 1)));
    }

    private String getValueString(double value, byte units, TimeZone zone)
    {
        switch(units)
        {
            case UnitType.JulianDate:
                return(Globals.getTimeString(this.getContext(), Globals.julianDateToCalendar(value, true), zone, false));

            case UnitType.Degree:
                return(Globals.getTemperatureString(value));

            case UnitType.Speed:
                return(Globals.getNumberString(value, 1));

            case UnitType.Precipitation:
                if(value <= 0.0)
                {
                    return("");
                }
                //else fall through

            default:
            case UnitType.Number:
                return(Globals.getNumberString(value, valueDecimalPlaces));
        }
    }

    private String getXValueString(double value)
    {
        return(getValueString(value, xUnits, valuesZone));
    }

    private String getYValueString(double value)
    {
        return(getValueString(value, yUnits, valuesZone));
    }

    private String getDayString(double julianDate, TimeZone zone)
    {
        return(Globals.getLocalDayString(this.getContext(), Globals.julianDateToCalendar(julianDate), zone));
    }

    public void setColors(int textsColor, int dataColor, boolean updateImage)
    {
        titleColor = textsColor;
        lineColor = dataColor;
        fillColor = Globals.getColor(100, dataColor);
        gridAxisColor = Globals.getColor(220, dataColor);

        if(updateImage)
        {
            updateSelectedImage();
            this.refresh();
        }
    }
    public void setColors(int textsColor, int dataColor)
    {
        setColors(textsColor, dataColor, true);
    }

    private float getTextSmallSize()
    {
        return(textSizeSmall * textSizeSmallScale);
    }

    public void setDataTextScale(float scale)
    {
        textSizeSmallScale = scale;

        drawBrush.setTextSize(getTextSmallSize());
        xValueHeight = yValueHeight = Globals.getTextHeight(drawBrush, Globals.getNumberString(0));

        this.refresh();
    }

    @SuppressWarnings("unused")
    public void setDeltaYScrollScale(float percentValue)
    {
        deltaYScrollPercent = percentValue;
    }

    public void setTitles(String x, String y)
    {
        Paint brush = createBrush();
        brush.setTextSize(textSizeLarge);

        xTitle = x;
        xTitleWidth = Globals.getTextWidth(brush, xTitle);

        yTitle = y;
        yTitleWidth = Globals.getTextWidth(brush, yTitle);
        yTitleHeight = Globals.getTextHeight(brush, yTitle);

        this.refresh();
    }

    public void setUnitTypes(byte xUnitType, byte yUnitType)
    {
        xUnits = xUnitType;
        yUnits = yUnitType;
        this.refresh();
    }

    @SuppressWarnings("unused")
    public void setValueDecimals(int decimalPlaces)
    {
        valueDecimalPlaces = decimalPlaces;
        this.refresh();
    }

    private void updateWidestTallestX(String value)
    {
        int valueWidth;
        int valueHeight = 0;
        String[] lines = (value != null ? value.split("\r\n") : new String[]{""});

        //make sure measuring correct size
        measureBrush.setTextSize(getTextSmallSize());

        //go through each line
        for(String currentLine : lines)
        {
            //if widest value text, remember width
            valueWidth = Globals.getTextWidth(measureBrush, currentLine);
            if(valueWidth > xValueWidth)
            {
                xValueWidth = valueWidth;
            }

            //update height
            valueHeight += (Globals.getTextHeight(measureBrush, currentLine) + valuePadding);
        }

        //if the tallest text, remember height
        if(valueHeight > xValueHeight)
        {
            xValueHeight = valueHeight;
        }
    }
    private void updateWidestTallestX(double value)
    {
        String valueString = getXValueString(value);

        if(xUnits == UnitType.JulianDate)
        {
            valueString += ("\r\n" + getDayString(value, valuesZone));
        }
        updateWidestTallestX(valueString);
    }

    private void updateWidestY(String value)
    {
        int valueWidth;

        //make sure measuring correct size
        measureBrush.setTextSize(getTextSmallSize());

        //if widest value text, remember width
        valueWidth = Globals.getTextWidth(measureBrush, value);
        if(valueWidth > yValueWidth)
        {
            yValueWidth = valueWidth;
        }
    }
    private void updateWidestY(double value)
    {
        updateWidestY(getYValueString(value));
    }

    private void setData(List<Double> x, List<Double> yTop, List<Double> yBottom, List<Double> y2, TimeZone zone)
    {
        int index;
        boolean usingY2 = (y2 != null);
        boolean usingYBottom = (yBottom != null);
        boolean usingJulianDates = (xUnits == UnitType.JulianDate);

        //reset x
        xMin = Float.MAX_VALUE;
        xMax = Float.MIN_VALUE;
        xValueWidth = Integer.MIN_VALUE;
        xAxisDivisors = null;

        //reset y
        yMin = Float.MAX_VALUE;
        yMax = Float.MIN_VALUE;
        yValueWidth = Integer.MIN_VALUE;
        yAxisDivisors = null;

        //if equal count for both axis pairs and have at least 2 points
        if(x != null && yTop != null && x.size() == yTop.size() && (!usingYBottom || yTop.size() == yBottom.size()) && (!usingY2 || y2.size() == yTop.size()) && x.size() >= 2)
        {
            valuesZone = (zone == null ? TimeZone.getDefault() : zone);
            xPoints = x;

            for(double value : xPoints)
            {
                //if smallest or largest value, remember it
                if(value < xMin)
                {
                    xMin = value;
                }
                if(value > xMax)
                {
                    xMax = value;
                }

                //if not using julian dates
                if(!usingJulianDates)
                {
                    //update widest and tallest
                    updateWidestTallestX(value);
                }
            }

            //if using julian dates
            if(usingJulianDates)
            {
                //update widest and tallest
                updateWidestTallestX(LongestJulianDate);
            }

            yPoints = yTop;
            y2Points = y2;
            yPointsBottom = yBottom;
            for(index = 0; index < yPoints.size(); index++)
            {
                double value = yPoints.get(index);
                double value2 = (usingY2 ? y2Points.get(index) : 0);
                double currentMin = Math.min(value, usingY2 ? value2 : Double.MAX_VALUE);
                double currentMax = Math.max(value, usingY2 ? value2 : -Double.MAX_VALUE);

                //if smallest or largest value, remember it
                if(!usingYBottom && currentMin < yMin)
                {
                    yMin = currentMin;
                }
                if(currentMax > yMax)
                {
                    yMax = currentMax;
                }

                //possibly update widest
                updateWidestY(value);
            }
            if(usingYBottom)
            {
                for(double value : yPointsBottom)
                {
                    if(value < yMin)
                    {
                        yMin = value;
                    }
                }
            }

            linePoints = new float[!xPoints.isEmpty() ? ((xPoints.size() - 1) * 4) : 0];
            line2Points = (usingY2 ? new float[linePoints.length] : null);
            linePointsBottom = (usingYBottom ? new float[linePoints.length] : null);
            linePointsImages = null;

            this.refresh();
        }
        else
        {
            linePoints = new float[0];
            line2Points = null;
            linePointsBottom = null;
            linePointsImages = null;
        }
    }
    public void setData(List<Double> x1, List<Double> y1, List<Double> y2, TimeZone zone)
    {
        setData(x1, y1, null, y2, zone);
    }
    @SuppressWarnings("unused")
    public void setData(List<Double> x, List<Double> yTop, List<Double> yBottom)
    {
        setData(x, yTop, yBottom, null, null);
    }
    public void setData(List<Double> x, List<Double> y, TimeZone zone)
    {
        setData(x, y, null, null, zone);
    }

    public void setFillColors(List<Integer> dataColors)
    {
        fillColors = dataColors;
    }

    @SuppressWarnings("unused")
    public void setDataImages(List<Bitmap> yImages)
    {
        int index;

        if(yImages == null)
        {
            linePointsImages = null;
        }
        else
        {
            linePointsImages = new Bitmap[yImages.size()];
            for(index = 0; index < linePointsImages.length; index++)
            {
                linePointsImages[index] = yImages.get(index);
            }
        }
    }

    public void setDataTitlesVisible(double minYValue, boolean showTitles)
    {
        showDataTitles = showTitles;
        showDataTitlesMinYValue = minYValue;
        this.refresh();
    }
    @SuppressWarnings("unused")
    public void setDataTitlesVisible(boolean showTitles)
    {
        setDataTitlesVisible(showDataTitlesMinYValue, showTitles);
    }

    @SuppressWarnings("unused")
    public void setBorderVisible(boolean showBorder)
    {
        showGraphBorder = showBorder;
        this.refresh();
    }

    @SuppressWarnings("unused")
    public void setDivisionLinesVisible(boolean showXLines, boolean showYLines)
    {
        xAxisDivisionLinesVisible = showXLines;
        yAxisDivisionLinesVisible = showYLines;
        this.refresh();
    }

    @SuppressWarnings("unused")
    public void setDivisorTitlesYVisible(boolean show)
    {
        yDivisorTitlesVisible = show;
        this.refresh();
    }

    @SuppressWarnings("unused")
    public void setRepeatDaySkip(boolean skip)
    {
        skipRepeatDays = skip;
    }

    public void clearItems()
    {
        //clear
        items.clear();

        //reset color
        itemColorIndex = 0;

        //update
        this.refresh();

        //if listener is set
        if(setItemsListener != null)
        {
            //call it
            setItemsListener.onSetItems(null, null, null, null, null, null);
        }
    }

    public void setItems(List<String> names, List<String> ids, List<Double> xMin, List<Double> xMax, List<Double> yMin, List<Double> yMax, int color, boolean showArcs)
    {
        int index;
        int index2;
        int idsSize = ids.size();
        int xMinSize = xMin.size();
        int xMaxSize = xMax.size();
        int yMinSize = yMin.size();
        Context context = this.getContext();

        //if equal count for all
        if(names.size() == idsSize && idsSize == xMinSize && xMinSize == xMaxSize && xMaxSize == yMinSize && yMinSize == yMax.size())
        {
            //update each item
            for(index = 0; index < xMinSize; index++)
            {
                //remember current item
                Item currentItem = new Item(names.get(index), ids.get(index), xMin.get(index), xMax.get(index), yMin.get(index), yMax.get(index), (showArcs ? textSizeMedium : getTextSmallSize()), showArcs);

                //if color is set
                if(color != -1)
                {
                    //set color and add item
                    currentItem.setColor(color);
                    items.add(currentItem);
                }
                else
                {
                    //if item is already in the list
                    index2 = items.indexOf(currentItem);
                    if(index2 >= 0)
                    {
                        //update item while keeping color
                        currentItem.setColor(items.get(index2).getColor());
                        items.set(index2, currentItem);
                    }
                    else
                    {
                        //add item while getting next color
                        currentItem.setColor(ContextCompat.getColor(context, itemColorList[itemColorIndex++]));
                        if(itemColorIndex >= itemColorList.length)
                        {
                            itemColorIndex = 0;
                        }
                        items.add(currentItem);
                    }
                }
            }

            //go through each item
            for(Item currentItem : items)
            {
                //if current item was not updated
                if(!currentItem.updated)
                {
                    //old and not found, so set signal level to -100 (0%)
                    currentItem.yMin = currentItem.yMax = -100;
                }

                //update status
                currentItem.updated = false;
            }

            //update display
            updateScrollDisplays = true;
            this.refresh();

            //if listener is set
            if(setItemsListener != null)
            {
                //call it
                setItemsListener.onSetItems(names, ids, xMin, xMax, yMin, yMax);
            }
        }
    }
    public void setItems(List<String> names, List<String> ids, List<Double> xMin, List<Double> xMax, List<Double> yMin, List<Double> yMax)
    {
        setItems(names, ids, xMin, xMax, yMin, yMax, -1, true);
    }

    private double[] getClosestY(List<Double> yPoints, double xValue)
    {
        int index;
        int dataCount = (xPoints != null ? xPoints.size() : 0);
        double yValue;
        double nextX;
        double nextY;
        double percent;
        double currentX;
        double currentY;
        double spanX;
        double spanY;

        //go through each data point
        for(index = 0; index + 1 < dataCount; index++)
        {
            //remember current and next
            currentX = xPoints.get(index);
            nextX = xPoints.get(index + 1);

            //if between/at current and next
            if(xValue >= currentX && xValue <= nextX)
            {
                //get x data
                spanX = (nextX - currentX);
                percent = (xValue - currentX) / spanX;

                //interpolate y
                currentY = yPoints.get(index);
                nextY = yPoints.get(index + 1);
                spanY = (nextY - currentY);
                yValue = currentY + (percent * spanY);

                //return current
                return(new double[]{xValue, yValue});
            }
        }

        //not found
        return(null);
    }

    public synchronized void setSelectedX(double xValue)
    {
        if(isPreview)
        {
            deltaX = deltaY = 0;
        }
        xSelected = xValue;
        this.refresh();
    }

    public void setSelectedWidth(int selectWidth)
    {
        if(selectWidth > 0)
        {
            selectedWidth = selectWidth;
            updateSelectedImage();
        }
    }

    public void setSelectedType(byte type)
    {
        selectType = type;

        if(selectType == SelectType.Area)
        {
            xSelected = 0;
        }
        if(selectType != SelectType.Image)
        {
            userSelectedImage = null;
            userSelectedImage2 = null;
        }
    }

    public void setSelectedImage(Bitmap image)
    {
        userSelectedImage = image;
    }

    public void setSelectedImage2(Bitmap image)
    {
        userSelectedImage2 = image;
    }

    @SuppressWarnings("unused")
    public void setAllowParentTouch(boolean allow)
    {
        allowParentTouch = allow;

        if(this.getParent() != null && allowParentTouch)
        {
            this.getParent().requestDisallowInterceptTouchEvent(false);
        }
    }

    @SuppressWarnings("unused")
    public void setAllowUpdate(boolean allow)
    {
        allowUpdate = allow;
    }

    @SuppressWarnings("unused")
    public void setScrollable(boolean canScroll)
    {
        //update status
        deltaX = deltaY = 0;
        allowScroll = updateScrollDisplays = canScroll;

        //update display
        setScrollX(0, false);
        this.refresh();
    }

    public void setOnScrollListener(OnScrollListener listener)
    {
        scrollListener = listener;
    }

    public void setOnSetItemsListener(OnSetItemsListener listener)
    {
        setItemsListener = listener;
    }

    public void setDivisorsX(List<String> names, List<Double> values, List<Bitmap> images, List<Bitmap> subImages1, List<Bitmap> subImages2)
    {
        int index;
        int lineCount;
        int length = values.size();
        boolean usingImages = (images != null && images.size() == length);
        boolean usingSubImages1 = (subImages1 != null && subImages1.size() == length);
        boolean usingSubImages2 = (subImages2 != null && subImages2.size() == length);
        boolean usingJulianDates = (xUnits == UnitType.JulianDate);
        String currentName;

        //reset
        xValueWidth = Integer.MIN_VALUE;
        xValueHeight = yValueHeight;        //note: yValueHeight is constant
        xAxisDivisionTextLines = (usingJulianDates ? 2 : 1);
        xAxisDivisors = null;
        xAxisDivisionImageHeight = new int[]{0, 0, 0};

        //if names and values are equal
        if(names.size() == length)
        {
            //set divisors
            xAxisDivisors = new Divisor[length];
            for(index = 0; index < length; index++)
            {
                Bitmap currentImage = (usingImages ? images.get(index) : null);
                Bitmap currentSubImage1 = (usingSubImages1 ? subImages1.get(index) : null);
                Bitmap currentSubImage2 = (usingSubImages2 ? subImages2.get(index) : null);

                //add divisor
                currentName = names.get(index);
                xAxisDivisors[index] = new Divisor(currentName, values.get(index), currentImage, currentSubImage1, currentSubImage2);

                //possibly update widest and tallest
                updateWidestTallestX(currentName);
                if(!usingJulianDates && currentName != null)
                {
                    lineCount = currentName.split("\r\n").length;
                    if(lineCount > xAxisDivisionTextLines)
                    {
                        xAxisDivisionTextLines = lineCount;
                    }
                }
                if(currentImage != null && currentImage.getHeight() > xAxisDivisionImageHeight[0])
                {
                    xAxisDivisionImageHeight[0] = currentImage.getHeight();
                }
                if(currentSubImage1 != null && currentSubImage1.getHeight() > xAxisDivisionImageHeight[1])
                {
                    xAxisDivisionImageHeight[1] = currentSubImage1.getHeight();
                }
                if(currentSubImage2 != null && currentSubImage2.getHeight() > xAxisDivisionImageHeight[2])
                {
                    xAxisDivisionImageHeight[2] = currentSubImage2.getHeight();
                }
            }
        }
    }
    public void setDivisorsX(List<String> names, List<Double> values, List<Bitmap> images, List<Bitmap> subImages)
    {
        setDivisorsX(names, values, images, subImages, null);
    }
    public void setDivisorsX(List<String> names, List<Double> values, List<Bitmap> images)
    {
        setDivisorsX(names, values, images, null);
    }
    public void setDivisorsX(List<String> names, List<Double> values)
    {
        setDivisorsX(names, values, null);
    }

    public void setDivisorsXTitles(String imagesTitle, String subImages1Title, String subImages2Title)
    {
        xAxisDivisionImageTitles[0] = imagesTitle;
        xAxisDivisionImageTitles[1] = subImages1Title;
        xAxisDivisionImageTitles[2] = subImages2Title;

        measureBrush.setTextSize(getTextSmallSize());
        xAxisDivisionImageTitlesWidth[0] = (imagesTitle != null ? Globals.getTextWidth(measureBrush, imagesTitle) : 0);
        xAxisDivisionImageTitlesHeight[0] = (imagesTitle != null ? Globals.getTextHeight(measureBrush, imagesTitle) : 0);
        xAxisDivisionImageTitlesWidth[1] = (subImages1Title != null ? Globals.getTextWidth(measureBrush, subImages1Title) : 0);
        xAxisDivisionImageTitlesHeight[1] = (subImages1Title != null ? Globals.getTextHeight(measureBrush, subImages1Title) : 0);
        xAxisDivisionImageTitlesWidth[2] = (subImages2Title != null ? Globals.getTextWidth(measureBrush, subImages2Title) : 0);
        xAxisDivisionImageTitlesHeight[2] = (subImages2Title != null ? Globals.getTextHeight(measureBrush, subImages2Title) : 0);

        xAxisDivisionImageTitleWidest = Math.max(xAxisDivisionImageTitlesWidth[0], xAxisDivisionImageTitlesWidth[1]);
        xAxisDivisionImageTitleWidest = Math.max(xAxisDivisionImageTitlesWidth[2], xAxisDivisionImageTitleWidest);

        this.refresh();
    }
    @SuppressWarnings("unused")
    public void setDivisorsXTitles(String imagesTitle, String subImages)
    {
        setDivisorsXTitles(imagesTitle, subImages, null);
    }
    @SuppressWarnings("unused")
    public void setDivisorsXTitles(String imagesTitle)
    {
        setDivisorsXTitles(imagesTitle, null, null);
    }

    public void setDivisorsY(List<String> names, List<Double> values)
    {
        int index;
        int length = values.size();
        String currentName;

        //reset
        yValueWidth = Integer.MIN_VALUE;
        yAxisDivisors = null;

        //if names and values are equal
        if(names.size() == length)
        {
            //set divisors
            yAxisDivisors = new Divisor[length];
            for(index = 0; index < length; index++)
            {
                //add divisor
                currentName = names.get(index);
                yAxisDivisors[index] = new Divisor(currentName, values.get(index), null, null, null);

                //possibly update widest
                updateWidestY(currentName);
            }
        }
    }

    public void setRangeX(double xMinimum, double xMaximum, int divisions)
    {
        boolean usingJulianDates = (xUnits == UnitType.JulianDate);

        //reset
        xValueWidth = Integer.MIN_VALUE;
        xAxisDivisors = null;

        //update
        xMin = xMinimum;
        xMax = xMaximum;
        xAxisDivisions = divisions;
        xSpan = xMax - xMin;

        if(divisions > 0)
        {
            xAxisDivisionTextLines = (usingJulianDates ? 2 : 1);

            //if using julian dates
            if(usingJulianDates)
            {
                //update widest and tallest
                updateWidestTallestX(LongestJulianDate);
            }
            else
            {
                //go through min and max
                for(double value : new double[]{xMin, xMax})
                {
                    //update widest and tallest
                    updateWidestTallestX(value);
                }
            }
        }
        else
        {
            xAxisDivisionImageHeight = new int[]{0, 0, 0};
        }

        this.refresh();
    }

    public void setRangeY(float yMinimum, float yMaximum, int divisions)
    {
        //reset
        yValueWidth = Integer.MIN_VALUE;
        yAxisDivisors = null;

        //update
        yMin = yMinimum;
        yMax = yMaximum;
        yAxisDivisions = divisions;
        ySpan = yMax - yMin;

        if(divisions > 0)
        {
            for(double value : new double[]{yMin, yMax})
            {
                //possibly update widest
                updateWidestY(value);
            }
        }

        this.refresh();
    }

    @SuppressWarnings("unused")
    public void setAxisDivisionImageOffsets(int xTopOffset, int xBottomOffset)
    {
        xAxisDivisionImageTopOffset = xTopOffset;
        xAxisDivisionImageBottomOffset = xBottomOffset;
        this.refresh();
    }

    @SuppressWarnings("unused")
    public void setAsPreview(final Graph parent)
    {
        isPreview = true;

        setSelectedType(Graph.SelectType.Area);
        setTitles("", "");

        setDivisorsX(Arrays.asList("", ""), Arrays.asList(parent.xMin, parent.xMax));
        setDivisorsY(Arrays.asList("", ""), Arrays.asList(parent.yMin, parent.yMax));

        setRangeX((float)parent.xMin, (float)parent.xMax, parent.xAxisDivisions);
        setRangeY((float)parent.yMin, (float)parent.yMax, parent.yAxisDivisions);

        setOnScrollListener(new OnScrollListener()
        {
            @Override
            public void onScrolled(float percent, boolean fromUser)
            {
                if(fromUser)
                {
                    //update parent
                    parent.deltaX = (percent * parent.width);
                    parent.setScrollX((int)parent.deltaX, false);
                }
            }
        });
        parent.setOnScrollListener(new OnScrollListener()
        {
            @Override
            public void onScrolled(float percent, boolean fromUser)
            {
                if(fromUser)
                {
                    //select same percent over
                    setSelectedX(width * percent);
                }
            }
        });
        parent.setOnSetItemsListener(new OnSetItemsListener()
        {
            @Override
            public void onSetItems(List<String> names, List<String> ids, List<Double> xMin, List<Double> xMax, List<Double> yMin, List<Double> yMax)
            {
                if(names == null || xMin == null || xMax == null || yMin == null || yMax == null)
                {
                    Graph.this.clearItems();
                }
                else
                {
                    Graph.this.setItems(names, ids, xMin, xMax, yMin, yMax);
                }
            }
        });
        this.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener()
        {
            @Override
            public void onGlobalLayout()
            {
                //percent parent graph area relative to preview graph area
                setSelectedWidth((int)((graphArea.width() / (float)parent.graphArea.width()) * graphArea.width()));
            }
        });
    }
}
