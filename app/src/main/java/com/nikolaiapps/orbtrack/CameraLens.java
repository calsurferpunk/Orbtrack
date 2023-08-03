package com.nikolaiapps.orbtrack;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.Comparator;


public class CameraLens extends SurfaceView implements SurfaceHolder.Callback, SensorUpdate.OnSensorChangedListener
{
    public interface OnStopCalibrationListener
    {
        void stopCalibration();
    }

    //Thread to update display
    private class UpdateThread extends Thread
    {
        private boolean running;

        public UpdateThread()
        {
            running = false;
        }

        public void run()
        {
            //while want to run
            while(running)
            {
                //update display
                CameraLens.this.postInvalidate();

                //prevent running too quickly
                try
                {
                    //noinspection BusyWait
                    sleep(20);
                }
                catch(Exception ex)
                {
                    //do nothing
                }
            }
        }

        public void setRunning(boolean run)
        {
            //update status
            running = run;

            //if not wanting to run
            if(!running)
            {
                try
                {
                    //stop and wait up to 1 second
                    join(1000);
                }
                catch(Exception ex)
                {
                    //do nothing
                }
            }
            else
            {
                try
                {
                    //start running
                    start();
                }
                catch(Exception ex)
                {
                    //do nothing
                }
            }
        }
    }

    //Icon image for ID
    private static class IconImage
    {
        private static class Comparer implements Comparator<IconImage>
        {
            @Override
            public int compare(IconImage value1, IconImage value2)
            {
                return(Globals.intCompare(value1.id, value2.id));
            }
        }

        public final int id;
        public Bitmap image;
        public Bitmap lastImage;
        private double lastAzimuth;
        private double lastElevation;
        public double angleDirection;
        public double lastAngleDirection;

        public IconImage(int id, Bitmap image)
        {
            this.id = id;
            this.image = image;
            this.lastImage = null;

            angleDirection = 0;
            lastAngleDirection = Double.MAX_VALUE;
            lastAzimuth = Double.MAX_VALUE;
            lastElevation = Double.MAX_VALUE;
        }

        public double getAngleDirection(double azimuth, double elevation)
        {
            //remember last angle direction
            lastAngleDirection = angleDirection;

            //if a used ID
            if(id >= 0)
            {
                //if angles changed
                if(lastAzimuth != Double.MAX_VALUE && lastElevation != Double.MAX_VALUE && azimuth != Double.MAX_VALUE && elevation != Double.MAX_VALUE && (lastAzimuth != azimuth || lastElevation != elevation))
                {
                    //get angle direction
                    angleDirection = Globals.getAngleDirection(lastAzimuth, azimuth, lastElevation, elevation);
                }

                //update last angles
                lastAzimuth = azimuth;
                lastElevation = elevation;
            }

            //return angle direction
            return(angleDirection);
        }

        public double getAngleDirectionDelta()
        {
            return(lastAngleDirection != Double.MAX_VALUE ? Globals.degreeDistance(lastAngleDirection, angleDirection) : Double.MAX_VALUE);
        }

        public void copyData(IconImage copyFrom)
        {
            lastAzimuth = copyFrom.lastAzimuth;
            lastElevation = copyFrom.lastElevation;
            angleDirection = copyFrom.angleDirection;
        }
    }

    //View to show compass alignment
    private static class CompassAlignmentView extends View
    {
        private boolean reachedCenter;
        private final int solidColor;
        private final int transparentColor;
        private final float radius;
        private int centerX;
        private int centerY;
        private int currentX;
        private int currentY;
        private final Paint currentPaint;
        private ValueAnimator animX;

        public CompassAlignmentView(Context context)
        {
            super(context);

            int rgb;
            boolean darkTheme = Settings.getDarkTheme(context);

            //set defaults
            reachedCenter = false;
            rgb = (darkTheme ? 255 : 0);
            solidColor = (darkTheme ? Color.WHITE : Color.BLACK);
            transparentColor = Color.argb(70, rgb, rgb, rgb);
            radius = Globals.dpToPixels(context, 30);
            currentPaint = new Paint();
            currentPaint.setStrokeWidth(3);
            currentPaint.setStyle(Paint.Style.STROKE);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldW, int oldH)
        {
            //stop any animation
            stopAnimation();

            //reset
            centerX = w / 2;
            centerY = h / 2;
            currentX = 0;
            currentY = h / 2;

            //setup animation
            animX = ValueAnimator.ofInt(0, centerX + (w / 8), centerX, centerX);
            animX.setRepeatCount(ValueAnimator.INFINITE);
            animX.setRepeatMode(ValueAnimator.RESTART);
            animX.setDuration(6000);
            animX.setCurrentPlayTime(0);
            animX.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
            {
                @Override
                public void onAnimationUpdate(ValueAnimator animation)
                {
                    currentX = (int)animation.getAnimatedValue();

                    //set if reached
                    reachedCenter = (currentX >= centerX);

                    //update display
                    CompassAlignmentView.this.postInvalidate();
                }
            });

            //start animation
            animX.start();
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
        {
            int width = resolveSizeAndState(getPaddingLeft() + getPaddingRight() + 100, widthMeasureSpec, 1);
            int height = resolveSizeAndState(getPaddingTop() + getPaddingBottom() + 200, heightMeasureSpec, 1);

            setMeasuredDimension(width, height);
        }

        @Override
        protected void onDraw(Canvas canvas)
        {
            super.onDraw(canvas);

            //draw moving circle
            currentPaint.setColor(solidColor);
            canvas.drawCircle(currentX, currentY, radius, currentPaint);

            //if reached center
            if(reachedCenter)
            {
                //draw center circle
                currentPaint.setColor(transparentColor);
                canvas.drawCircle(centerX, centerY, radius, currentPaint);
            }
            else
            {
                //draw center cross
                canvas.drawLine(centerX - radius, centerY, centerX + radius, centerY, currentPaint);
                canvas.drawLine(centerX, centerY - radius, centerX, centerY + radius, currentPaint);
            }
        }

        //Stops any running animation
        private void stopAnimation()
        {
            //stop animation
            if(animX != null)
            {
                animX.cancel();
            }
        }
    }

    //View to show compass calibration
    private static class CompassCalibrateView extends View
    {
        private final int phoneWidth;
        private final int phoneHeight;
        private float lastX;
        private float lastY;
        private float currentX;
        private float currentY;
        private final Paint currentPaint;
        private final Bitmap phoneImage;
        private ValueAnimator animX;
        private final ArrayList<Float> pathPoints;

        public CompassCalibrateView(Context context)
        {
            super(context);

            boolean darkTheme = Settings.getDarkTheme(context);

            //set defaults
            currentPaint = new Paint();
            currentPaint.setStrokeWidth(3);
            currentPaint.setStyle(Paint.Style.STROKE);
            currentPaint.setColor(darkTheme ? Color.WHITE : Color.BLACK);
            phoneImage = Globals.getBitmap(context, R.drawable.ic_smartphone_black, true);
            phoneWidth = phoneImage.getWidth();
            phoneHeight = phoneImage.getHeight();
            pathPoints = new ArrayList<>(0);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldW, int oldH)
        {
            final int usedWidth;
            final float scaleX;
            final float scaleY;
            final float offsetX;

            //reset
            lastX = lastY = Float.MAX_VALUE;

            usedWidth = Math.min(w, h);
            scaleX = usedWidth / 2.5f;
            offsetX = scaleX + ((w / 2.0f) - scaleX - phoneWidth);
            scaleY = h / 2.5f;

            //start time animator
            animX = ValueAnimator.ofFloat(0.0f, 2.0f * (float)Math.PI);
            animX.setRepeatCount(ValueAnimator.INFINITE);
            animX.setRepeatMode(ValueAnimator.RESTART);
            animX.setInterpolator(new LinearInterpolator());
            animX.setDuration(3000);
            animX.setCurrentPlayTime(3000 / 2);
            animX.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
            {
                private boolean reachedMiddle = false;

                @Override
                public void onAnimationUpdate(ValueAnimator animation)
                {
                    float currentXFraction = animX.getAnimatedFraction();

                    float time = (float)animation.getAnimatedValue();
                    currentX = ((float)Math.cos(time) * scaleX) + offsetX;
                    currentY = (((float)Math.sin(2 * time) / 2) * scaleY) + (scaleY / 2);

                    //if just passing middle, going forward
                    if(!reachedMiddle && currentXFraction >= 0.5)
                    {
                        //restart points and update status
                        pathPoints.clear();
                        reachedMiddle = true;
                    }
                    //else if just passing middle, going reverse
                    else if(reachedMiddle && currentXFraction < 0.5)
                    {
                        //update status
                        reachedMiddle = false;
                    }

                    //if last points are set
                    if(lastX != Float.MAX_VALUE && lastY != Float.MAX_VALUE)
                    {
                        //add last and current points from image center
                        pathPoints.add(lastX + (int)(phoneWidth / 2f));
                        pathPoints.add(lastY + (int)(phoneHeight / 2f));
                        pathPoints.add(currentX + (int)(phoneWidth / 2f));
                        pathPoints.add(currentY + (int)(phoneHeight / 2f));
                    }

                    //update last points
                    lastX = currentX;
                    lastY = currentY;

                    //update display
                    CompassCalibrateView.this.postInvalidate();
                }
            });

            //start animation in both directions
            animX.start();
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
        {
            int width = resolveSizeAndState(getPaddingLeft() + getPaddingRight() + 100, widthMeasureSpec, 1);
            int height = resolveSizeAndState(getPaddingTop() + getPaddingBottom() + 300, heightMeasureSpec, 1);

            setMeasuredDimension(width, height);
        }

        @Override
        protected void onDraw(Canvas canvas)
        {
            super.onDraw(canvas);

            //draw phone at current animation location
            canvas.drawBitmap(phoneImage, currentX, currentY, currentPaint);
            if(pathPoints.size() >= 4)
            {
                canvas.drawLines(getPathPointsArray(), currentPaint);
            }
        }

        @Override
        protected void onDetachedFromWindow()
        {
            super.onDetachedFromWindow();

            stopAnimation();
        }

        //Gets path points array
        private float[] getPathPointsArray()
        {
            int index;
            float[] points = new float[pathPoints.size()];

            for(index = 0; index < points.length; index++)
            {
                points[index] = pathPoints.get(index);
            }

            return(points);
        }

        //Stops any running animation
        private void stopAnimation()
        {
            //stop animation
            if(animX != null)
            {
                animX.cancel();
            }

            //reset
            pathPoints.clear();
            lastX = lastY = Float.MAX_VALUE;
        }
    }

    private static class AlignmentAngle
    {
        public float az;
        public float el;
        public float orbitalAz;

        public AlignmentAngle()
        {
            az = el = orbitalAz = Float.MAX_VALUE;
        }

        public void set(float currentAz, float currentEl, float orbAz)
        {
            az = currentAz;
            el = currentEl;
            orbitalAz = orbAz;
        }

        public boolean isEmpty()
        {
            return(az == Float.MAX_VALUE || el == Float.MAX_VALUE);
        }
    }

    private static class RelativeLocationProperties
    {
        final public boolean closeArea;
        final public boolean outsideArea;
        final public float azCenterPx;
        final public float elCenterPx;

        public RelativeLocationProperties(boolean closeArea, boolean outsideArea, float azCenterPx, float elCenterPx)
        {
            this.closeArea = closeArea;
            this.outsideArea = outsideArea;
            this.azCenterPx = azCenterPx;
            this.elCenterPx = elCenterPx;
        }
    }

    private static final float CLOSE_AREA_DEGREES = 12f;
    private static final float STAR_SCALE = (1.0f / 3.5f);

    public int pathDivisions;
    public boolean showPaths;
    public boolean showHorizon;
    public boolean showCalibration;
    public boolean showOutsideArea;
    public TextView helpText;
    public PlayBar playBar;
    public FloatingActionStateButtonMenu settingsMenu;

    private static final IconImage.Comparer iconImageComparer = new IconImage.Comparer();
    private int orientation;
    private int azIndex;
    private int elIndex;
    private final int compassWidth;
    private final int indicator;
    private final int iconLength;
    private final int iconHalfLength;
    private final int iconScaleOffset;
    private final int starLength;
    private final int starHalfLength;
    private final int compassHeight;
    private int compassBorderWidth;
    private final int textColor;
    private final int textBgColor;
    private final int horizonColor;
    private final int horizonLineColor;
    private int calibrateIndex;
    private int selectedNoradId;
    private int selectedOrbitalIndex;
    private boolean compassBad;
    private boolean compassHadBad;
    private final boolean arrowDirectionCentered;
    private final boolean showIconIndicatorDirection;
    private final float textSize;
    private final float textOffset;
    private final float textPadding;
    private float compassCenterX;
    private float compassCenterY;
    private final float indicatorThickness;
    private final float timeCirclePxRadius;
    private float azUserOffset;
    private float azDeclination;
    private final float[] azDegArray;
    private final float[] elDegArray;
    private final float[] azPx = new float[2];
    private final float[] elPx = new float[2];
    private final float[] trianglePoints = new float[12];
    private float cameraDegWidth;
    private float cameraDegHeight;
    private float useCameraDegWidth;
    private float useCameraDegHeight;
    private float cameraHardwareDegWidth;
    private float cameraHardwareDegHeight;
    private final float indicatorPxRadius;
    private final double defaultPathJulianDelta;
    private Camera currentCamera;
    private final Paint iconPaint;
    private final Paint currentPaint;
    private Rect selectedArea;
    private final Rect iconArea;
    private final Rect iconScaledArea;
    private final RectF firstArea;
    private final RectF previousArea;
    private Bitmap starIconImage;
    private final Bitmap arrowDirection;
    private final Bitmap arrowDoubleDirection;
    private final Bitmap compassDirection;
    private Button calibrateOkayButton;
    private AlignmentAngle alignCenter;
    private AlignmentAngle alignBottomLeft;
    private AlignmentAngle alignTopRight;
    private final ValueAnimator compassBadAnimator;
    private final SurfaceHolder currentHolder;
    private SensorUpdate sensorUpdater;
    private UpdateThread updateThread;
    private OnStopCalibrationListener stopCalibrationListener;
    private final ArrayList<IconImage> orbitalIcons;
    private Rect[] currentOrbitalAreas;
    private Database.SatelliteData[] currentOrbitals;
    private final Calculations.TopographicDataType[] calibrateAngles;
    private Calculations.TopographicDataType[] currentLookAngles;
    private CalculateViewsTask.OrbitalView[][] travelLookAngles;

    public CameraLens(Context context, Database.SatelliteData[] selectedOrbitals)
    {
        super(context);

        int index;
        int averageCount = Settings.getLensAverageCount(context);
        boolean darkTheme = Settings.getDarkTheme(context);
        Resources currentResources = context.getResources();
        DisplayMetrics metrics = currentResources.getDisplayMetrics();
        float[] dpPixels = Globals.dpsToPixels(context, 2, 5, 4, 16, 42);

        selectedOrbitalIndex = -1;
        selectedNoradId = Universe.IDs.None;
        orientation = getCameraOrientation();
        textColor = (darkTheme ? Color.argb(160, 255, 255, 255) : Color.argb(160, 0, 0, 0));
        textBgColor = (darkTheme ? Color.argb(50, 0, 0, 0) : Color.argb(50, 255, 255, 255));
        horizonLineColor = Settings.getLensHorizonColor(context);
        horizonColor = Globals.getColor(70, horizonLineColor);
        showPaths = showCalibration = compassBad = compassHadBad = false;
        showHorizon = Settings.getLensShowHorizon(context);
        showOutsideArea = Settings.getLensShowOutsideArea(context);
        showIconIndicatorDirection = Settings.getIndicatorIconShowDirection(context);
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 16, metrics);
        textOffset = textSize / 1.5f;
        textPadding = (textSize * 0.15f);
        indicatorThickness = dpPixels[0];
        timeCirclePxRadius = dpPixels[1];
        iconLength = (int)dpPixels[4];
        iconHalfLength = (iconLength / 2);
        iconScaleOffset = (iconHalfLength / 2);
        starLength = (int)(dpPixels[4] * STAR_SCALE);
        starHalfLength = (starLength / 2);
        cameraHardwareDegWidth = cameraHardwareDegHeight = 45;
        cameraDegWidth = cameraDegHeight = useCameraDegWidth = useCameraDegHeight = Float.MAX_VALUE;
        indicator = Settings.getIndicator(context);
        indicatorPxRadius = Globals.dpToPixels(context, 36);
        resetAlignmentStatus();
        iconArea = new Rect();
        iconScaledArea = new Rect();
        firstArea = new RectF();
        previousArea = new RectF();
        azIndex = elIndex = pathDivisions = 0;
        azUserOffset = Settings.getLensAzimuthUserOffset(context);
        azDeclination = 0;
        defaultPathJulianDelta = Globals.truncate(1.0 / 24, 8);      //1 hour interval      note: setting to 8 places prevents rounding errors for 1 hour intervals
        azDegArray = new float[averageCount];
        elDegArray = new float[averageCount];
        for(index = 0; index < averageCount; index++)
        {
            azDegArray[index] = elDegArray[index] = 0;
        }

        currentPaint = new Paint();
        currentPaint.setAntiAlias(true);
        currentPaint.setTypeface(Typeface.create("Arial", Typeface.BOLD));
        currentPaint.setTextSize(textSize);
        iconPaint = new Paint();
        if(indicator == Settings.Options.LensView.IndicatorType.Icon)
        {
            iconPaint.setAntiAlias(true);
            iconPaint.setAlpha(80);
        }

        currentHolder = this.getHolder();
        currentHolder.addCallback(this);
        this.setWillNotDraw(false);

        starIconImage = null;
        arrowDirectionCentered = Settings.getLensDirectionCentered(context);
        arrowDirection = Globals.getBitmap(Globals.getDrawableCombined(context, 5, 5, true, Globals.getDrawable(context, R.drawable.ic_arrow_up_black, R.color.white), Globals.getDrawable(context, R.drawable.ic_arrow_up_black, R.color.black)));
        arrowDoubleDirection = Globals.getBitmap(Globals.getDrawableCombined(context, 5, 5, true, Globals.getDrawable(context, R.drawable.ic_arrow_up_double_black, R.color.white), Globals.getDrawable(context, R.drawable.ic_arrow_up_double_black, R.color.black)));
        compassDirection = BitmapFactory.decodeResource(currentResources, R.drawable.compass);
        compassWidth = compassDirection.getWidth();
        compassHeight = compassDirection.getHeight();
        compassCenterX = compassCenterY = compassBorderWidth = 0;
        compassBadAnimator = ValueAnimator.ofInt((int)dpPixels[2], (int)dpPixels[3]);
        compassBadAnimator.setDuration(300);
        compassBadAnimator.setRepeatCount(5);                                               //forward, reverse, forward, reverse, forward
        compassBadAnimator.setRepeatMode(ValueAnimator.REVERSE);
        compassBadAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
        {
            @Override
            public void onAnimationUpdate(ValueAnimator animation)
            {
                compassBorderWidth = (int)animation.getAnimatedValue();
            }
        });
        compassBadAnimator.addListener(new AnimatorListenerAdapter()
        {
            @Override
            public void onAnimationEnd(Animator animation, boolean isReverse)
            {
                Handler delayRestart;

                //if compass is still bad
                if(compassBad)
                {
                    //start animation again in 3 seconds
                    delayRestart = new Handler();
                    delayRestart.postDelayed(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            compassBadAnimator.start();
                        }
                    }, 3000);
                }
            }
        });

        updateThread = null;
        calibrateOkayButton = null;
        helpText = null;

        orbitalIcons = new ArrayList<>(0);
        currentOrbitals = (selectedOrbitals != null ? selectedOrbitals : new Database.SatelliteData[0]);
        currentOrbitalAreas = new Rect[0];
        calibrateAngles = new Calculations.TopographicDataType[3];
        currentLookAngles = new Calculations.TopographicDataType[0];
        travelLookAngles = new CalculateViewsTask.OrbitalView[0][0];
    }
    public CameraLens(Context context)
    {
        this(context, null);
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {}

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height)
    {
        //if no surface yet
        if(currentHolder.getSurface() == null)
        {
            //stop
            return;
        }

        //start camera
        startCamera(currentHolder, false);
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder)
    {
        stopCamera(true);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        float x = event.getX();
        float y = event.getY();
        float compassHalfWidth = compassWidth / 2.0f;
        float compassHalfHeight = compassHeight / 2.0f;

        //if compass reading is bad and within compass image bounds
        if(compassBad && x >= (compassCenterX - compassHalfWidth) && x <= (compassCenterX + compassHalfWidth) && (y >= compassCenterY - compassHalfHeight) && (y <= compassCenterY + compassHalfHeight))
        {
            showCompassErrorDialog();
            return(false);
        }
        else
        {
            performClick();
            return(super.onTouchEvent(event));
        }
    }

    @Override
    public boolean performClick()
    {
        return(super.performClick());
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        int index;
        int index2;
        int currentId;
        int travelLength;
        int selectedColor = Color.WHITE;
        int width = getWidth();
        int widthHalf = width / 2;
        int widthDouble = width * 2;
        int height = getHeight();
        int heightHalf = height / 2;
        int heightDouble = height * 2;
        int selectedId = Universe.IDs.Invalid;
        byte selectedType = Database.OrbitalType.Satellite;
        boolean selectedCloseArea = false;
        boolean selectedOutsideArea = false;
        float elCenterPx;
        float elDeltaDeg;
        float alignCenterX;
        float alignCenterY;
        float currentAzDeg = getAzDeg();
        float currentElDeg = getElDeg();
        float degToPxWidth = getDegreeToPixelWidth(width);
        float degToPxHeight = getDegreeToPixelHeight(height);
        double angleDirection;
        double julianDateStart;
        double julianDateEnd;
        double pathJulianDelta;
        double pathJulianEndDelta;
        double periodMinutes;
        double currentAzDelta;
        double currentElDelta;
        double currentAzPxDelta;
        double currentElPxDelta;
        String currentName;
        String selectedName = "";
        Context context = getContext();
        Calculations.TopographicDataType selectedLookAngle = null;

        //if camera area has been calculated
        if(useCameraDegWidth != Float.MAX_VALUE && useCameraDegHeight != Float.MAX_VALUE)
        {
            //if using horizon
            if(showHorizon)
            {
                //draw horizon/ground level
                elDeltaDeg = (float)Globals.degreeDistance(currentElDeg, 0);
                elCenterPx = heightHalf - (elDeltaDeg * degToPxHeight);
                if(elCenterPx < 0)
                {
                    elCenterPx = 0;
                }
                if(elCenterPx < height)
                {
                    //draw line
                    currentPaint.setStyle(Paint.Style.FILL);
                    currentPaint.setColor(horizonLineColor);
                    canvas.drawRect(0, elCenterPx, width, elCenterPx + 3, currentPaint);

                    //fill below
                    currentPaint.setColor(horizonColor);
                    canvas.drawRect(0, elCenterPx, width, height, currentPaint);
                }
            }

            //go through each orbital and look angle
            for(index = 0; index < currentOrbitals.length; index++)
            {
                //remember if on selection and current orbital
                boolean haveSelected = haveSelectedOrbital();
                boolean currentSelected = (selectedOrbitalIndex == index);
                Database.SatelliteData currentOrbital = currentOrbitals[index];

                //if current orbital is set, look angle is set, and using orbital
                if(currentOrbital != null && currentOrbital.database != null && index < currentLookAngles.length && (!showCalibration || !haveSelected || currentSelected))
                {
                    //remember current type, color, look, and travel angles
                    int currentColor = currentOrbital.database.pathColor;
                    byte currentType = currentOrbital.getOrbitalType();
                    Calculations.TopographicDataType currentLookAngle = currentLookAngles[index];
                    CalculateViewsTask.OrbitalView[] currentTravel = (index < travelLookAngles.length ? travelLookAngles[index] : null);

                    //if current look angle or travel angles are set
                    if(currentLookAngle != null || currentTravel != null)
                    {
                        //setup paint
                        currentPaint.setColor(Globals.getColor((showCalibration && currentSelected ? 70 : 255), currentColor));
                        currentPaint.setStrokeWidth(indicatorThickness);
                    }

                    //if current travel is set, showing paths, not calibrating, and -non selected or on selected-
                    if(currentTravel != null && showPaths && !showCalibration && (!haveSelected || currentSelected))
                    {
                        //remember length
                        travelLength = currentTravel.length;

                        //if using preset path divisions and at least 2 points
                        if(pathDivisions != 0 && travelLength > 1)
                        {
                            //get distance between start and end, divided by divisions
                            pathJulianDelta = (currentTravel[travelLength - 1].julianDate - currentTravel[0].julianDate) / pathDivisions;
                        }
                        else
                        {
                            //set default path julian delta
                            pathJulianDelta = defaultPathJulianDelta;

                            //remember look angles, and period
                            periodMinutes = currentOrbital.satellite.orbit.periodMinutes;

                            //if less than 1 day for period
                            if(periodMinutes > 0 && periodMinutes < Calculations.MinutesPerDay)
                            {
                                //divide path into 12 parts
                                pathJulianDelta = Globals.truncate((periodMinutes / Calculations.MinutesPerDay) / 12, 8);
                            }
                        }
                        pathJulianEndDelta = pathJulianDelta / 4;

                        //if there are travel views
                        if(travelLength > 0)
                        {
                            //reset first and previous areas
                            firstArea.setEmpty();
                            previousArea.setEmpty();

                            //setup paint
                            currentPaint.setStyle(Paint.Style.FILL);

                            //set julian dates
                            julianDateStart = currentTravel[0].julianDate;
                            julianDateEnd = currentTravel[travelLength - 1].julianDate;

                            //go through each view
                            for(index2 = 0; index2 < travelLength; index2++)
                            {
                                //remember current view and status
                                boolean onFirst = (index2 == 0);
                                CalculateViewsTask.OrbitalView currentView = currentTravel[index2];
                                currentAzDelta = Globals.degreeDistance(currentAzDeg, currentView.azimuth);
                                currentElDelta = Globals.degreeDistance(currentElDeg, currentView.elevation);

                                //if on first
                                if(onFirst)
                                {
                                    //set last point
                                    azPx[1] = (float)(widthHalf + (currentAzDelta * degToPxWidth));
                                    elPx[1] = (float)(heightHalf - (currentElDelta * degToPxHeight));
                                }

                                //update line points and status
                                azPx[0] = azPx[1];
                                elPx[0] = elPx[1];
                                azPx[1] = (float)(widthHalf + (currentAzDelta * degToPxWidth));
                                elPx[1] = (float)(heightHalf - (currentElDelta * degToPxHeight));
                                currentAzPxDelta = Math.abs(azPx[0] - azPx[1]);
                                currentElPxDelta = Math.abs(elPx[0] - elPx[1]);

                                //if not on first and not too long to draw
                                if(!onFirst && currentAzPxDelta < widthDouble && currentElPxDelta < heightDouble)
                                {
                                    //draw line between last and current view
                                    currentPaint.setColor(currentColor);
                                    canvas.drawLine(azPx[0], elPx[0], azPx[1], elPx[1], currentPaint);
                                }

                                //if -on first- or -on last- or --enough pixels between views- and -on last- or --needed time between views- and -more than 1/4 delta before end---
                                if(onFirst || (index2 == travelLength - 1) || ((currentAzPxDelta >= timeCirclePxRadius || currentElPxDelta >= timeCirclePxRadius) && (currentView.julianDate - julianDateStart >= pathJulianDelta) && (currentView.julianDate + pathJulianEndDelta < julianDateEnd)))
                                {
                                    RectF bgArea;

                                    //draw circle
                                    canvas.drawCircle(azPx[1], elPx[1], timeCirclePxRadius, currentPaint);

                                    //get text area
                                    if(currentView.timeArea.isEmpty())
                                    {
                                        currentPaint.getTextBounds(currentView.timeString, 0, currentView.timeString.length(), currentView.timeArea);
                                    }
                                    currentView.timeArea.offsetTo((int) (azPx[1] - (currentView.timeArea.width() / 2f)), (int) (elPx[1] - textSize));

                                    //get background area
                                    bgArea = new RectF(currentView.timeArea.left - textPadding, currentView.timeArea.top - textOffset - textPadding, currentView.timeArea.right + textPadding, (currentView.timeArea.bottom - textOffset) + textPadding);

                                    //if -first area empty or not intersecting with current- and -previous area is empty or not intersecting with current-
                                    if((firstArea.isEmpty() || !firstArea.intersect(bgArea)) && (previousArea.isEmpty() || !previousArea.intersect(bgArea)))
                                    {
                                        //draw text background
                                        currentPaint.setColor(textBgColor);
                                        canvas.drawRect(bgArea, currentPaint);

                                        //draw text border
                                        currentPaint.setColor(currentColor);
                                        currentPaint.setStyle(Paint.Style.STROKE);
                                        canvas.drawRect(bgArea, currentPaint);

                                        //draw text
                                        currentPaint.setColor(textColor);
                                        currentPaint.setStyle(Paint.Style.FILL);
                                        canvas.drawText(currentView.timeString, currentView.timeArea.left, currentView.timeArea.top, currentPaint);

                                        //remember previous area and starting julian date
                                        previousArea.set(bgArea);
                                        julianDateStart = currentView.julianDate;
                                    }

                                    //if were on the first
                                    if(onFirst)
                                    {
                                        //remember first area
                                        firstArea.set(bgArea);
                                    }
                                }
                            }
                        }
                    }

                    //if current look angle is set
                    if(currentLookAngle != null)
                    {
                        RelativeLocationProperties relativeProperties;

                        //remember current ID, type, name
                        currentId = currentOrbital.getSatelliteNum();
                        currentName = currentOrbital.getName();

                        //determine relative location
                        relativeProperties = getRelativeLocationProperties(currentAzDeg, currentElDeg, currentLookAngle.azimuth, currentLookAngle.elevation, width, height, degToPxWidth, degToPxHeight);

                        //if on selection
                        if(currentSelected)
                        {
                            //update selected properties
                            selectedId = currentId;
                            selectedType = currentType;
                            selectedName = currentName;
                            selectedColor = currentOrbital.database.pathColor;
                            selectedCloseArea = relativeProperties.closeArea;
                            selectedOutsideArea = relativeProperties.outsideArea;
                            selectedLookAngle = currentLookAngle;
                        }

                        //draw orbital
                        drawOrbital(context, canvas, currentId, currentType, currentName, currentColor, currentOrbitalAreas[index], relativeProperties.azCenterPx, relativeProperties.elCenterPx, currentLookAngle.azimuth, currentLookAngle.elevation, indicatorPxRadius, width, height, currentSelected, relativeProperties.outsideArea);
                    }
                }
            }

            //if calibrating
            if(showCalibration)
            {
                //set default paint stroke and width
                currentPaint.setStyle(Paint.Style.STROKE);
                currentPaint.setStrokeWidth(indicatorThickness);

                //if selected nearest orbital
                if(haveSelectedNearestOrbital())
                {
                    //if need to align center
                    if(needAlignmentCenter())
                    {
                        alignCenterX = widthHalf;
                        alignCenterY = heightHalf;
                        calibrateIndex = 0;
                    }
                    //else if need bottom left
                    else if(needAlignmentBottomLeft())
                    {
                        alignCenterX = (0.25f * width);
                        alignCenterY = (0.75f * height);
                        calibrateIndex = 1;
                    }
                    //else must need bottom right
                    else //if(needAlignmentTopRight())
                    {
                        alignCenterX = (0.75f * width);
                        alignCenterY = (0.25f * height);
                        calibrateIndex = 2;
                    }

                    //show offset orbital position
                    drawOrbital(context, canvas, selectedId, selectedType, selectedName, selectedColor, selectedArea, alignCenterX, alignCenterY, Double.MAX_VALUE, Double.MAX_VALUE, indicatorPxRadius, width, height, true, false);
                }
                else
                {
                    //show center
                    currentPaint.setColor(Color.WHITE);
                    canvas.drawLine(widthHalf - indicatorPxRadius, heightHalf, widthHalf + indicatorPxRadius, heightHalf, currentPaint);
                    canvas.drawLine(widthHalf, heightHalf - indicatorPxRadius, widthHalf, heightHalf + indicatorPxRadius, currentPaint);
                }
            }
            else
            {
                //draw compass
                compassCenterX = (width - (compassWidth / 2f)) - 5;
                compassCenterY = 5 + (compassHeight / 2f);
                if(compassBad)
                {
                    //if hasn't been bad yet
                    if(!compassHadBad)
                    {
                        //start animation
                        compassBadAnimator.start();
                    }

                    //draw border
                    currentPaint.setStyle(Paint.Style.FILL);
                    currentPaint.setColor(Color.RED);
                    canvas.drawCircle(compassCenterX, compassCenterY, (int)(compassWidth / 2f) + compassBorderWidth, currentPaint);

                    //has been bad
                    compassHadBad = true;
                }
                Globals.drawBitmap(canvas, compassDirection, compassCenterX, compassCenterY, currentAzDeg, currentPaint);
            }

            //if have a selection
            if(haveSelectedOrbital())
            {
                Calculations.TopographicDataType usedLookAngle = (showCalibration && calibrateIndex >= 0 ? (calibrateIndex < calibrateAngles.length ? calibrateAngles[calibrateIndex] : null) : selectedLookAngle);
                RelativeLocationProperties relativeCalibrateProperties = (showCalibration && usedLookAngle != null ? getRelativeLocationProperties(currentAzDeg, currentElDeg, usedLookAngle.azimuth, usedLookAngle.elevation, width, height, degToPxWidth, degToPxHeight) : null);
                boolean haveProperties = (relativeCalibrateProperties != null);
                boolean closeArea = (showCalibration ? (!haveProperties || relativeCalibrateProperties.closeArea) : selectedCloseArea);
                boolean outsideArea = (showCalibration ? (!haveProperties || relativeCalibrateProperties.outsideArea) : selectedOutsideArea);

                //if not too close and have look angles
                if(!closeArea && usedLookAngle != null)
                {
                    //draw arrow direction
                    angleDirection = Globals.getAngleDirection(currentAzDeg, currentElDeg, usedLookAngle.azimuth, usedLookAngle.elevation);
                    Globals.drawBitmap(canvas, (outsideArea ? arrowDoubleDirection : arrowDirection), widthHalf, heightHalf, (arrowDirectionCentered ? 0 : ((CLOSE_AREA_DEGREES / 2) * degToPxHeight)), (float)(angleDirection + 90), currentPaint);
                }
            }
        }

        //draw
        super.onDraw(canvas);
    }

    //Draws orbital at the given position
    private void drawOrbital(Context context, Canvas canvas, int noradId, byte currentType, String currentName, int currentColor, Rect currentArea, float centerX, float centerY, double azimuth, double elevation, float indicatorPxRadius, int canvasWidth, int canvasHeight, boolean isSelected, boolean outsideArea)
    {
        boolean isStar = (currentType == Database.OrbitalType.Star);
        boolean isConstellation = (currentType == Database.OrbitalType.Constellation);
        float drawPxRadius = (indicatorPxRadius / (outsideArea ? 2 : 1)) * (isStar ? STAR_SCALE : 1);

        //if not selected, outside of area, and not showing
        if(!isSelected && outsideArea && !showOutsideArea)
        {
            //stop and don't draw
            return;
        }

        //setup paint
        currentPaint.setColor(currentColor);
        currentPaint.setStyle(Paint.Style.STROKE);

        //if a constellation
        if(isConstellation)
        {
            //draw connecting stars
        }
        else
        {
            //draw indicator
            switch(indicator)
            {
                case Settings.Options.LensView.IndicatorType.Square:
                    canvas.drawRect(centerX - drawPxRadius, centerY - drawPxRadius, centerX + drawPxRadius, centerY + drawPxRadius, currentPaint);
                    break;

                case Settings.Options.LensView.IndicatorType.Triangle:
                    trianglePoints[0] = centerX - drawPxRadius;        //bottom left
                    trianglePoints[1] = centerY + drawPxRadius;        //bottom left
                    trianglePoints[2] = centerX + drawPxRadius;        //bottom right
                    trianglePoints[3] = centerY + drawPxRadius;        //bottom right

                    trianglePoints[4] = centerX + drawPxRadius;        //bottom right
                    trianglePoints[5] = centerY + drawPxRadius;        //bottom right
                    trianglePoints[6] = centerX;                       //top center
                    trianglePoints[7] = centerY - drawPxRadius;        //top center

                    trianglePoints[8] = centerX;                       //top center
                    trianglePoints[9] = centerY - drawPxRadius;        //top center
                    trianglePoints[10] = centerX - drawPxRadius;       //bottom left
                    trianglePoints[11] = centerY + drawPxRadius;       //bottom left
                    canvas.drawLines(trianglePoints, currentPaint);
                    break;

                case Settings.Options.LensView.IndicatorType.Icon:
                    if(context != null)
                    {
                        int iconId = Globals.getOrbitalIconId(context, noradId, currentType);

                        //if a star
                        if(isStar)
                        {
                            //if star icon image not set yet
                            if(starIconImage == null)
                            {
                                //create image
                                starIconImage = Globals.getBitmapSized(context, iconId, starLength, starLength, 0);
                            }

                            //draw image
                            canvas.drawBitmap(starIconImage, centerX - starHalfLength, centerY - starHalfLength, iconPaint);
                        }
                        else
                        {
                            boolean isSatellite = (noradId > 0);
                            IconImage indicatorIcon = new IconImage(noradId, null);
                            int[] indexes = Globals.divideFind(indicatorIcon, orbitalIcons, iconImageComparer);

                            //if already in the list
                            if(indexes[0] >= 0)
                            {
                                //remember current icon in list
                                double angleDirection = 135;
                                double directionDelta = 0;
                                Bitmap rotatedImage;
                                IconImage currentOrbitalIcon = orbitalIcons.get(indexes[0]);

                                //use center and image
                                indicatorIcon.copyData(currentOrbitalIcon);
                                if(showIconIndicatorDirection && isSatellite)
                                {
                                    angleDirection = indicatorIcon.getAngleDirection(azimuth, elevation);
                                    directionDelta = indicatorIcon.getAngleDirectionDelta();
                                }
                                if(directionDelta == Double.MAX_VALUE || Math.abs(directionDelta) >= 2.0)
                                {
                                    rotatedImage = Globals.getBitmapRotated(currentOrbitalIcon.image, angleDirection - 135);
                                }
                                else
                                {
                                    rotatedImage = (currentOrbitalIcon.lastImage != null ? currentOrbitalIcon.lastImage : currentOrbitalIcon.image);
                                }
                                indicatorIcon.image = rotatedImage;
                                if(rotatedImage != null)
                                {
                                    currentOrbitalIcon.lastImage = Globals.copyBitmap(rotatedImage);
                                }
                                currentOrbitalIcon.copyData(indicatorIcon);
                            }
                            else
                            {
                                //create image and add to list
                                indicatorIcon.image = Globals.getBitmapSized(context, iconId, iconLength, iconLength, (isSatellite && (currentType != Database.OrbitalType.Satellite || Settings.getSatelliteIconImageIsThemeable(context)) ? R.color.white : 0));
                                if(isSatellite)
                                {
                                    //add background highlight
                                    Drawable imageBg = Globals.getDrawableSized(context, iconId, indicatorIcon.image.getWidth(), indicatorIcon.image.getHeight(), R.color.black, false);
                                    indicatorIcon.image = Globals.getBitmap(Globals.getDrawableCombined(context, 2, 2, true, new BitmapDrawable(context.getResources(), indicatorIcon.image), imageBg));
                                }
                                orbitalIcons.add(indexes[1], indicatorIcon);
                            }

                            //draw image
                            iconArea.set((int)(centerX - iconHalfLength), (int)(centerY - iconHalfLength), (int)(centerX + iconHalfLength), (int)(centerY + iconHalfLength));
                            if(outsideArea)
                            {
                                iconScaledArea.set(iconArea.left + iconScaleOffset, iconArea.top + iconScaleOffset, iconArea.right - iconScaleOffset, iconArea.bottom - iconScaleOffset);
                                canvas.drawBitmap(indicatorIcon.image, null, iconScaledArea, iconPaint);
                            }
                            else
                            {
                                canvas.drawBitmap(indicatorIcon.image, iconArea.left, iconArea.top, iconPaint);
                            }
                        }
                        break;
                    }
                    //else fall through

                default:
                case Settings.Options.LensView.IndicatorType.Circle:
                    canvas.drawCircle(centerX, centerY, drawPxRadius, currentPaint);
                    break;
            }
        }

        //get text area
        currentPaint.setStrokeWidth(2);
        if(currentArea.isEmpty())
        {
            currentPaint.getTextBounds(currentName, 0, currentName.length(), currentArea);
        }
        currentArea.offsetTo((int)(centerX - (currentArea.width() / 2f)), (int)((centerY - (isConstellation ? 0 : indicatorPxRadius) - textSize) + (textOffset * (isStar || indicator == Settings.Options.LensView.IndicatorType.Icon ? 2 : 1))));
        if(currentArea.left < 20)
        {
            currentArea.offsetTo(20, currentArea.top);
        }
        if(currentArea.right > canvasWidth)
        {
            currentArea.offsetTo(canvasWidth - currentArea.width(), currentArea.top);
        }
        if(currentArea.top < 20)
        {
            currentArea.offsetTo(currentArea.left, 20);
        }
        if(currentArea.bottom > canvasHeight)
        {
            currentArea.offsetTo(currentArea.left, canvasHeight - currentArea.height());
        }

        //draw text
        currentPaint.setStyle(Paint.Style.FILL);
        canvas.drawText(currentName, currentArea.left, currentArea.top, currentPaint);
    }

    public void showCompassAlignmentDialog(DialogInterface.OnClickListener positiveListener)
    {
        final Context context = getContext();
        Resources res = context.getResources();

        Globals.showConfirmDialog(context, Globals.getDrawable(context, R.drawable.ic_filter_center_focus_black, true), new CompassAlignmentView(context), res.getString(R.string.title_compass_align), res.getString(R.string.desc_compass_align), res.getString(R.string.title_align), res.getString(R.string.title_skip), null, true, positiveListener, null, null, new DialogInterface.OnDismissListener()
        {
            @Override
            public void onDismiss(DialogInterface dialog)
            {
                Settings.setLensFirstRun(context, false);
            }
        });
    }

    public boolean hasCompassError()
    {
        return(compassBad);
    }

    private void showCompassErrorDialog()
    {
        Context context = getContext();
        Resources res = context.getResources();
        Button[] buttons;

        //if compass is still bad
        if(compassBad)
        {
            //get and set button
            buttons = Globals.showConfirmDialog(context, Globals.getDrawable(context, R.drawable.ic_error_black, true), new CompassCalibrateView(context), res.getString(R.string.title_compass_calibrate), res.getString(R.string.desc_compass_error), res.getString(R.string.title_ok), null, null, true, null, null, null, null);
            calibrateOkayButton = buttons[0];
        }
    }

    //Gets average for the given degree array
    private float getAverageDegree(float[] valueArray)
    {
        int index;
        double sinSum = 0;
        double cosSum = 0;
        double currentRad;

        //go through each value
        for(index = 0; index < valueArray.length; index++)
        {
            currentRad = Math.toRadians(valueArray[index]);
            sinSum += Math.sin(currentRad);
            cosSum += Math.cos(currentRad);
        }

        //return circular average/mean
        return(index > 0 ? (float)Math.toDegrees(Math.atan2(sinSum, cosSum)) : 0);
    }

    //Updates azimuth declination for the given observer
    public void updateAzDeclination(Calculations.ObserverType observer)
    {
        azDeclination = (new GeomagneticField((float)observer.geo.latitude, (float)observer.geo.longitude, (float)observer.geo.altitudeKm * 1000, System.currentTimeMillis())).getDeclination();
    }

    //Get averaged az degree
    private float getAzDeg()
    {
        return((float)Globals.normalizeAngle(getAverageDegree(azDegArray) + azDeclination + azUserOffset));
    }

    //Get averaged el degree
    private float getElDeg()
    {
        return((float)Globals.normalizeAngle(getAverageDegree(elDegArray)));
    }

    //Gets degree to pixel conversion for given width
    private float getDegreeToPixelWidth(int width)
    {
        return(width / (useCameraDegWidth != Float.MAX_VALUE ? useCameraDegWidth : 1));
    }

    //Gets degree to pixel conversion for given height
    private float getDegreeToPixelHeight(int height)
    {
        return(height / (useCameraDegHeight != Float.MAX_VALUE ? useCameraDegHeight : 1));
    }

    //Gets relative location properties
    private RelativeLocationProperties getRelativeLocationProperties(double currentAzDeg, double currentElDeg, double locationAzDeg, double locationElDeg, int width, int height, float degToPxWidth, float degToPxHeight)
    {
        float azCenterPx;
        float elCenterPx;
        float azDeltaDegrees = (float)Globals.degreeDistance(currentAzDeg, locationAzDeg);
        float elDeltaDegrees = (float)Globals.degreeDistance(currentElDeg, locationElDeg);
        boolean closeArea;
        boolean outsideArea = false;

        //get centers and area properties
        azCenterPx = (width / 2f) + (azDeltaDegrees * degToPxWidth);
        if(azCenterPx > width)
        {
            azCenterPx = width;
            outsideArea = true;
        }
        else if(azCenterPx < 0)
        {
            azCenterPx = 0;
            outsideArea = true;
        }
        elCenterPx = (height / 2f) - (elDeltaDegrees * degToPxHeight);
        if(elCenterPx > height)
        {
            elCenterPx = height;
            outsideArea = true;
        }
        else if(elCenterPx < 0)
        {
            elCenterPx = 0;
            outsideArea = true;
        }
        closeArea = (Math.abs(azDeltaDegrees) <= CLOSE_AREA_DEGREES && Math.abs(elDeltaDegrees) <= CLOSE_AREA_DEGREES);

        //return results
        return(new RelativeLocationProperties(closeArea, outsideArea, azCenterPx, elCenterPx));
    }

    //Update orbital positions to display
    public void updatePositions(Database.SatelliteData[] orbitals, Calculations.TopographicDataType[] lookAngles, boolean checkLength)
    {
        int index;
        int orbitalsLength;

        //if there are orbitals and look angles that match in size
        if(orbitals != null && lookAngles != null && orbitals.length > 0 && (!checkLength || lookAngles.length == orbitals.length))
        {
            //update orbitals and look angles
            currentOrbitals = orbitals;
            currentLookAngles = lookAngles;
            orbitalsLength = currentOrbitals.length;

            //if area has not been set or needs to be changed
            if(currentOrbitalAreas.length == 0 || (checkLength && currentOrbitalAreas.length != orbitalsLength))
            {
                //set area to same size
                currentOrbitalAreas = new Rect[orbitalsLength];
                for(index = 0; index < orbitalsLength; index++)
                {
                    //set empty
                    currentOrbitalAreas[index] = new Rect(0, 0, 0, 0);
                    currentOrbitalAreas[index].setEmpty();
                }
            }

            //if travel has not been set or needs to be changed
            if(travelLookAngles.length == 0 || (checkLength && travelLookAngles.length != orbitalsLength))
            {
                //set travel to same size
                travelLookAngles = new CalculateViewsTask.OrbitalView[orbitalsLength][];
                for(index = 0; index < orbitalsLength; index++)
                {
                    //set empty
                    travelLookAngles[index] = new CalculateViewsTask.OrbitalView[0];
                }
            }
        }
    }

    //Sets orbital travel/positions to display
    public void setTravel(int index, CalculateViewsTask.OrbitalView[] currentLookAngles)
    {
        //if look angles index is valid
        if(index < travelLookAngles.length)
        {
            //set look angles
            travelLookAngles[index] = currentLookAngles;
        }
    }

    //Tries to select nearest orbital
    public boolean selectNearest()
    {
        int index;
        int selectIndex = -1;
        float nearestDelta = Float.MAX_VALUE;
        float azDeltaDeg;
        float elDeltaDeg;
        float totalDelta;
        float currentAzDeg = getAzDeg();
        float currentElDeg = getElDeg();

        //if look angles match orbitals length
        if(currentLookAngles.length == currentOrbitals.length)
        {
            //if showing calibration
            if(showCalibration)
            {
                //go through each calibration angle
                for(index = 0; index < calibrateAngles.length; index++)
                {
                    //create new angles
                    calibrateAngles[index] = new Calculations.TopographicDataType();
                }
            }

            //go through each look angle
            for(index = 0; index < currentLookAngles.length; index++)
            {
                //remember current look angle
                Calculations.TopographicDataType currentLookAngle = (currentOrbitals[index] != null ? currentLookAngles[index] : null);

                //if above the horizon and being used
                if(currentLookAngle != null && currentLookAngle.elevation >= 0)
                {
                    //determine relative location
                    azDeltaDeg = (float)Math.abs(Globals.degreeDistance(currentAzDeg, currentLookAngle.azimuth));
                    elDeltaDeg = (float)Math.abs(Globals.degreeDistance(currentElDeg, currentLookAngle.elevation));
                    totalDelta = azDeltaDeg + elDeltaDeg;

                    //if both are within lens view, narrow enough error, and the smallest delta so far
                    if(azDeltaDeg < cameraDegWidth && azDeltaDeg < 20 && elDeltaDeg < cameraDegHeight && elDeltaDeg < 20 && totalDelta < 30 && totalDelta < nearestDelta)
                    {
                        //update status
                        selectIndex = index;
                        nearestDelta = totalDelta;
                        if(showCalibration)
                        {
                            calibrateAngles[0].azimuth = currentLookAngle.azimuth;
                            calibrateAngles[0].elevation = currentLookAngle.elevation;
                        }
                    }
                }
            }
        }

        //if an orbital was selected
        if(selectIndex >= 0)
        {
            //update selected
            selectedOrbitalIndex = selectIndex;
            selectedNoradId = currentOrbitals[selectIndex].getSatelliteNum();
            selectedArea.setEmpty();

            //if showing calibration
            if(showCalibration)
            {
                //remember widths, heights, and first angles
                int width = getWidth();
                int height = getHeight();
                float quarterWidthDegrees = (0.25f * width) / getDegreeToPixelWidth(width);
                float quarterHeightDegrees = (0.25f * height) / getDegreeToPixelHeight(height);
                Calculations.TopographicDataType firstAngles = calibrateAngles[0];

                //calculate bottom left and top right angles
                calibrateAngles[1].azimuth = firstAngles.azimuth + quarterWidthDegrees;
                calibrateAngles[1].elevation = firstAngles.elevation + quarterHeightDegrees;
                calibrateAngles[2].azimuth = firstAngles.azimuth - quarterWidthDegrees;
                calibrateAngles[2].elevation = firstAngles.elevation - quarterHeightDegrees;

                //update status
                calibrateIndex = 0;
            }
        }

        //return if selected nearest
        return(selectIndex >= 0);
    }

    //Selects orbital with given norad ID
    public void selectOrbital(int noradId)
    {
        int index;

        //if orbitals are set
        if(currentOrbitals != null)
        {
            //go through each orbital
            for(index = 0; index < currentOrbitals.length; index++)
            {
                //if a match
                if(currentOrbitals[index].getSatelliteNum() == noradId)
                {
                    //set selection and stop
                    selectedOrbitalIndex = index;
                    selectedNoradId = noradId;
                    return;
                }
            }
        }

        //not found
        selectedOrbitalIndex = -1;
        selectedNoradId = Universe.IDs.None;
    }

    //Gets selected norad ID
    public int getSelectedNoradId()
    {
        return(selectedNoradId);
    }

    //Returns if any valid orbital is selected
    public boolean haveSelectedOrbital()
    {
        return(selectedOrbitalIndex >= 0 && currentLookAngles != null && selectedOrbitalIndex < currentLookAngles.length);
    }

    //Returns if any valid nearest orbital is selected
    public boolean haveSelectedNearestOrbital()
    {
        return(calibrateIndex >= 0 && haveSelectedOrbital());
    }

    //Returns if need alignment for center
    public boolean needAlignmentCenter()
    {
        return(alignCenter.isEmpty());
    }

    //Returns if need bottom left alignment
    public boolean needAlignmentBottomLeft()
    {
        return(alignBottomLeft.isEmpty());
    }

    //Returns if need top right alignment
    public boolean needAlignmentTopRight()
    {
        return(alignTopRight.isEmpty());
    }

    //Sets center alignment
    public void setAlignmentCenter()
    {
        Calculations.TopographicDataType currentAngles = currentLookAngles[selectedOrbitalIndex];
        alignCenter.set(getAzDeg(), getElDeg(), (float)currentAngles.azimuth);
    }

    //Sets bottom left alignment
    public void setAlignmentBottomLeft()
    {
        Calculations.TopographicDataType currentAngles = currentLookAngles[selectedOrbitalIndex];
        alignBottomLeft.set(getAzDeg(), getElDeg(), (float)currentAngles.azimuth);
    }

    //Sets top right alignment
    public void setAlignmentTopRight()
    {
        Calculations.TopographicDataType currentAngles = currentLookAngles[selectedOrbitalIndex];
        alignTopRight.set(getAzDeg(), getElDeg(), (float)currentAngles.azimuth);
    }

    //Tries to update user azimuth offset
    public void updateUserAzimuthOffset()
    {
        //add difference to offset
        azUserOffset += Globals.degreeDistance(alignCenter.az, alignCenter.orbitalAz);
        Settings.setLensAzimuthUserOffset(getContext(), azUserOffset);
    }

    //Sets calculated alignment
    public void setCalculatedAlignment()
    {
        //set width and height
        cameraDegWidth = (float)(Math.abs(Globals.degreeDistance(alignBottomLeft.az, alignTopRight.az)) * 2);
        cameraDegHeight = (float)(Math.abs(Globals.degreeDistance(alignBottomLeft.el, alignTopRight.el)) * 2);
        saveWidthHeight(false);

        //update used width and height
        updateUsedWidthHeight();
    }

    //Resets alignment status
    public void resetAlignmentStatus()
    {
        calibrateIndex = -1;
        selectedOrbitalIndex = -1;
        selectedNoradId = Universe.IDs.None;
        selectedArea = new Rect(0, 0, 0, 0);
        selectedArea.setEmpty();

        alignCenter = new AlignmentAngle();
        alignBottomLeft = new AlignmentAngle();
        alignTopRight = new AlignmentAngle();
    }

    //Resets alignment
    public void resetAlignment()
    {
        //reset offset
        azUserOffset = 0;
        Settings.setLensAzimuthUserOffset(getContext(), azUserOffset);

        //reset width and height
        cameraDegWidth = cameraHardwareDegWidth;
        cameraDegHeight = cameraHardwareDegHeight;
        saveWidthHeight(true);

        //update used width and height
        updateUsedWidthHeight();
    }

    //Sets orientation
    public void updateOrientation()
    {
        //set orientation
        orientation = getCameraOrientation();

        //update used with and height
        updateUsedWidthHeight();
    }

    //Sets used camera width and height
    private void updateUsedWidthHeight()
    {
        boolean inPortrait = inOrientationPortrait();

        //use width/height according to orientation
        useCameraDegWidth = (inPortrait ? cameraDegWidth : cameraDegHeight);
        useCameraDegHeight = (inPortrait ? cameraDegHeight : cameraDegWidth);
    }

    //Saves camera width and height
    private void saveWidthHeight(boolean auto)
    {
        boolean inPortrait = inOrientationPortrait();
        Context context = this.getContext();

        //save width
        Settings.setLensWidth(context, (inPortrait ? cameraDegWidth : cameraDegHeight));
        Settings.setLensAutoWidth(context, auto);

        //save height
        Settings.setLensHeight(context, (inPortrait ? cameraDegHeight : cameraDegWidth));
        Settings.setLensAutoHeight(context, auto);
    }

    //Returns if closed settings menu
    public boolean closeSettingsMenu()
    {
        return(settingsMenu != null && settingsMenu.close());
    }

    //Stops calibration
    public void stopCalibration()
    {
        //if calibrating and listener is set
        if(showCalibration && stopCalibrationListener != null)
        {
            //send event
            stopCalibrationListener.stopCalibration();
        }
    }

    //Stops the camera
    public void stopCamera(boolean kill)
    {
        if(updateThread != null)
        {
            updateThread.setRunning(false);
            updateThread = null;
        }

        if(currentCamera != null)
        {
            try
            {
                currentCamera.stopPreview();
                if(kill)
                {
                    currentCamera.release();
                }
            }
            catch(Exception ex)
            {
                //do nothing
            }
            if(kill)
            {
                currentCamera = null;
            }
        }

        if(playBar != null && kill)
        {
            playBar.stopPlayTimer();
        }

        if(sensorUpdater != null)
        {
            sensorUpdater.stopUpdates();
            sensorUpdater = null;
        }
    }

    //Sets stop calibration listener
    public void setStopCalibrationListener(OnStopCalibrationListener listener)
    {
        stopCalibrationListener = listener;
    }

    //Starts the camera
    private void startCamera(SurfaceHolder holder, boolean retrying)
    {
        int cameraOrientation;
        boolean inPortrait = inOrientationPortrait();
        Context context = this.getContext();
        boolean startSensors = false;
        boolean havePermission = Globals.haveCameraPermission(context);
        boolean useCamera = Settings.getLensUseCamera(context);
        boolean useAutoWidth = Settings.getLensAutoWidth(context);
        boolean useAutoHeight = Settings.getLensAutoHeight(context);
        boolean cameraRotate = Settings.getLensRotate(context);
        float userDegWidth = Settings.getLensWidth(context);
        float userDegHeight = Settings.getLensHeight(context);
        Camera.Parameters cameraParams;
        Resources res = context.getResources();

        //get orientation
        cameraOrientation = (cameraRotate ? ((orientation + 180) % 360) : orientation);

        //if using camera and have permission
        if(useCamera && havePermission)
        {
            //stop camera if previously open
            stopCamera(false);

            try
            {
                //try to start camera
                if(currentCamera == null)
                {
                    //try to open camera
                    currentCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
                }
                cameraParams = currentCamera.getParameters();
                cameraParams.set("orientation", (inPortrait ? Camera.Parameters.SCENE_MODE_PORTRAIT : Camera.Parameters.SCENE_MODE_LANDSCAPE));
                cameraParams.set("rotation", cameraOrientation);
                cameraParams.setRotation(cameraOrientation);
                if(cameraParams.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
                {
                    //set auto focus
                    cameraParams.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                }
                currentCamera.setParameters(cameraParams);
                currentCamera.setDisplayOrientation(cameraOrientation);
                currentCamera.setPreviewDisplay(holder);
                currentCamera.startPreview();

                //get desired camera degree view
                cameraHardwareDegWidth = cameraParams.getHorizontalViewAngle();
                cameraHardwareDegHeight = cameraParams.getVerticalViewAngle();
                cameraDegWidth = (useAutoWidth ? cameraHardwareDegWidth : userDegWidth);
                cameraDegHeight = (useAutoHeight ? cameraHardwareDegHeight : userDegHeight);

                //start sensors
                startSensors = true;
            }
            catch(Exception ex)
            {
                currentCamera = null;
                Globals.showSnackBar(this, res.getString(R.string.text_camera_error), ex.getMessage(), true, true);
            }
        }
        //else if using camera, don't have permission, but can ask
        else if(useCamera && Globals.canAskCameraPermission)
        {
            //ask for permission
            Globals.askCameraPermission(context, retrying, new Globals.OnDenyListener()
            {
                @Override
                public void OnDeny(byte resultCode)
                {
                    //if were retrying
                    if(retrying)
                    {
                        //use without camera
                        startCamera(holder, true);
                    }
                }
            });
            if(retrying)
            {
                //don't ask again
                Globals.canAskCameraPermission = false;
            }
        }
        //else not using camera or can't get permission
        else
        {
            //if using camera but can't get permission
            if(useCamera)
            {
                //show denied and don't ask again
                Globals.showDenied(this, res.getString(R.string.desc_permission_camera_deny));
                Globals.canAskCameraPermission = false;
            }
            else
            {
                //stop camera if previously open
                stopCamera(false);
            }

            //set desired camera degree view
            cameraDegWidth = userDegWidth;
            cameraDegHeight = userDegHeight;

            //start sensors
            startSensors = true;
        }

        //if starting sensors
        if(startSensors)
        {
            //update used width and height
            updateUsedWidthHeight();

            //if sensors updater not set yet
            if(sensorUpdater == null)
            {
                //create it
                sensorUpdater = new SensorUpdate(context, Globals.getScreenOrientation(context));
            }

            //start sensors
            sensorUpdater.startUpdates(this);
        }

        //if update thread doesn't exist
        if(updateThread == null)
        {
            //create it
            updateThread = new UpdateThread();
        }

        //start/stop update thread
        updateThread.setRunning(startSensors);
    }
    public void startCamera(boolean retrying)
    {
        if(currentHolder != null)
        {
            startCamera(currentHolder, retrying);
        }
    }
    public void startCamera()
    {
        startCamera(false);
    }

    @Override
    public void onSensorChanged(float azDeg, float elDeg, int type, int accuracy)
    {
        //add az sample
        azDegArray[azIndex] = azDeg;
        azIndex = (azIndex + 1) % azDegArray.length;

        //add el sample
        elDegArray[elIndex] = elDeg;
        elIndex = (elIndex + 1) % elDegArray.length;

        //handle based on type
        if(type == Sensor.TYPE_MAGNETIC_FIELD)
        {
            switch(accuracy)
            {
                case SensorManager.SENSOR_STATUS_ACCURACY_HIGH:
                case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM:
                    //if had been bad
                    if(compassHadBad)
                    {
                        //if calibrate okay button exists
                        if(calibrateOkayButton != null)
                        {
                            //close displays
                            calibrateOkayButton.callOnClick();
                            calibrateOkayButton = null;
                        }

                        //if help text exists and not showing calibration
                        if(helpText != null && !showCalibration)
                        {
                            //clear and hide help
                            helpText.setOnClickListener(null);
                            helpText.setVisibility(View.GONE);
                        }
                    }

                    //update status
                    compassBad = compassHadBad = false;
                    break;

                case SensorManager.SENSOR_STATUS_ACCURACY_LOW:
                case SensorManager.SENSOR_STATUS_UNRELIABLE:
                    //update status
                    compassBad = true;
                    if(helpText != null && !showCalibration)
                    {
                        helpText.setOnClickListener(new OnClickListener()
                        {
                            @Override
                            public void onClick(View v)
                            {
                                showCompassErrorDialog();
                            }
                        });
                        helpText.setText(getResources().getString(R.string.title_compass_inaccurate_fix));
                        helpText.setVisibility(View.VISIBLE);
                    }
                    break;
            }
        }
    }

    //Gets current camera orientation
    private int getCameraOrientation()
    {
        int degrees;
        int rotation = Globals.getScreenOrientation(this.getContext());
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, cameraInfo);

        switch(rotation)
        {
            case Surface.ROTATION_90:
                degrees = 90;
                break;

            case Surface.ROTATION_180:
                degrees = 180;
                break;

            case Surface.ROTATION_270:
                degrees = 270;
                break;

            default:
            case Surface.ROTATION_0:
                degrees = 0;
                break;
        }

        return((cameraInfo.orientation - degrees + 360) % 360);
    }

    //Returns if in portrait orientation
    private boolean inOrientationPortrait()
    {
        return(orientation == 90 || orientation == 270);
    }
}