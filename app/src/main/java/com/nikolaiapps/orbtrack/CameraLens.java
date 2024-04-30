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
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Range;
import android.util.SizeF;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.camera.camera2.interop.Camera2CameraInfo;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExposureState;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.Preview;
import androidx.camera.core.SurfaceOrientedMeteringPointFactory;
import androidx.camera.core.ZoomState;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import com.google.android.material.slider.LabelFormatter;
import com.google.android.material.slider.Slider;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;


public class CameraLens extends FrameLayout implements SensorUpdate.OnSensorChangedListener
{
    public interface OnReadyListener
    {
        void ready();
    }

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
                CameraLens.this.updateDisplay();

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
                return(Integer.compare(value1.id, value2.id));
            }
        }

        public final int id;
        public Bitmap image;
        public Bitmap lastImage;
        public double lastAzimuth;
        public double lastElevation;
        public double angleDirection;
        public double lastAngleDirection;

        public IconImage(int id, Bitmap image)
        {
            this.id = id;
            this.image = image;
            this.lastImage = null;

            angleDirection = Double.MAX_VALUE;
            lastAngleDirection = Double.MAX_VALUE;
            lastAzimuth = Double.MAX_VALUE;
            lastElevation = Double.MAX_VALUE;
        }

        public double getAngleDirection(double azimuth, double elevation, double centerX, double centerY, double lastCenterX, double lastCenterY)
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
                    angleDirection = Calculations.getBearing(lastCenterX, lastCenterY, centerX, centerY);
                }

                //update last status
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
            lastAngleDirection = copyFrom.lastAngleDirection;
        }
    }

    //Parent orbital with child properties
    private static class ParentOrbital
    {
        private static class Comparer implements Comparator<ParentOrbital>
        {
            @Override
            public int compare(ParentOrbital value1, ParentOrbital value2)
            {
                return(Integer.compare(value1.id, value2.id));
            }
        }

        public int id;
        public final int index;
        public final RelativeLocationProperties[][] childProperties;

        public ParentOrbital(int id, int index, int pointCount)
        {
            this.id = id;
            this.index = index;
            this.childProperties = new RelativeLocationProperties[pointCount][2];
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

    private static final float CLOSE_AREA_DEGREES = 15f;
    private static final float STAR_IMAGE_SCALE = (1.0f / 6.0f);
    private static final float STAR_TEXT_SCALE = 0.75f;

    public int pathDivisions;
    public boolean showPaths;
    public boolean showHorizon;
    public boolean showCalibration;
    public final boolean showPathDirections;
    public final boolean showOutsideArea;
    public final boolean showPathTimeNames;
    public final boolean hideDistantPathTimes;
    public final boolean hideConstellationStarPaths;
    public TextView helpText;
    public TextView sliderText;
    public PlayBar playBar;
    public Slider zoomBar;
    public Slider exposureBar;
    public FloatingActionStateButtonMenu settingsMenu;

    private static final IconImage.Comparer iconImageComparer = new IconImage.Comparer();
    private static final ParentOrbital parentOrbitalSearch = new ParentOrbital(Universe.IDs.None, -1, 0);
    private static final ParentOrbital.Comparer parentOrbitalComparer = new ParentOrbital.Comparer();
    private int azIndex;
    private int elIndex;
    private int orientation;
    private final int compassWidth;
    private final int indicator;
    private final int iconAlpha;
    private final int iconLength;
    private final int iconHalfLength;
    private final int iconScaleOffset;
    private final int starLength;
    private final int starHalfLength;
    private final int compassHeight;
    private int compassBorderWidth;
    private final int compassMargin;
    private final int textAlpha;
    private final int textColor;
    private final int textBgColor;
    private final int horizonColor;
    private final int horizonLineColor;
    private final int textShadowColor;
    private final int textShadowOffset;
    private final int constellationAlpha;
    private final int constellationSelectedAlpha;
    private int calibrateIndex;
    private int selectedNoradId;
    private int selectedOrbitalIndex;
    private final boolean allowZoom;
    private final boolean allowExposure;
    private boolean compassBad;
    private boolean compassHadBad;
    private boolean haveZoomValues;
    private boolean pendingOnDraw;
    private boolean pendingSetInParentFilter;
    private final boolean usingFilledBoxPath;
    private final boolean usingColorTextPath;
    private final boolean showingConstellations;
    private final boolean arrowDirectionCentered;
    private final boolean showingArrowDirection;
    private final boolean showIconIndicatorDirection;
    private final float textSize;
    private final float textOffset;
    private final float textPadding;
    private final float starTextSize;
    private final float starTextOffset;
    private float starMagnitude;
    private float compassCenterX;
    private float compassCenterY;
    private final float indicatorThickness;
    private final float timeCirclePxRadius;
    private final float pathDirectionShapeLength;
    private float azUserOffset;
    private float azDeclination;
    private final float[] azDegArray;
    private final float[] elDegArray;
    private final float[] azPx = new float[2];
    private final float[] elPx = new float[2];
    private final float[] trianglePoints = new float[12];
    private float cameraZoomMin;
    private float cameraZoomMax;
    private float cameraDegWidth;
    private float cameraDegHeight;
    private float cameraZoomRatio;
    private float useCameraDegWidth;
    private float useCameraDegHeight;
    private float cameraHardwareDegWidth;
    private float cameraHardwareDegHeight;
    private float exposureIncrement;
    private final float indicatorPxRadius;
    private final double defaultPathJulianDelta;
    private final String xString;
    private Camera currentCamera;
    private final LensOverlay currentCameraOverlay;
    private final PreviewView currentCameraView;
    private ProcessCameraProvider currentCameraProvider;
    private final Paint currentPaint;
    private final Path timePathShape;
    private Rect selectedArea;
    private final Rect iconArea;
    private final Rect iconScaledArea;
    private final RectF firstArea;
    private final RectF previousArea;
    private Bitmap starIconImage;
    private final Bitmap arrowDirection;
    private final Bitmap arrowDoubleDirection;
    private final Bitmap compassDirection;
    private final Drawable zoomImage;
    private final Drawable exposureImage;
    private Button calibrateOkayButton;
    private AlignmentAngle alignCenter;
    private AlignmentAngle alignBottomLeft;
    private AlignmentAngle alignTopRight;
    private final ValueAnimator compassBadAnimator;
    private SensorUpdate sensorUpdater;
    private UpdateThread updateThread;
    private OnReadyListener readyListener;
    private OnStopCalibrationListener stopCalibrationListener;
    private ScaleGestureDetector scaleDetector;
    private Range<Integer> exposureRange;
    private final ArrayList<IconImage> orbitalIcons;
    private final ArrayList<ParentOrbital> parentOrbitals;
    private Rect[] currentOrbitalAreas;
    private Database.SatelliteData[] currentOrbitals;
    private final Calculations.TopographicDataType[] calibrateAngles;
    private Calculations.TopographicDataType[] currentLookAngles;
    private CalculateViewsTask.OrbitalView[][] travelLookAngles;

    private class LensOverlay extends SurfaceView
    {
        public LensOverlay(Context context)
        {
            super(context);

            SurfaceHolder holder = this.getHolder();

            this.setWillNotDraw(false);
            this.setZOrderOnTop(true);
            this.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

            if(holder != null)
            {
                holder.setFormat(PixelFormat.TRANSPARENT);
            }
        }

        @Override
        protected void onDraw(Canvas canvas)
        {
            int index;
            int index2;
            int currentId;
            int travelLength;
            int lastTimeIndex;
            int lastTravelIndex;
            int usedTimeIndex;
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
            float currentAzPx;
            float currentElPx;
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
            double timeAzDelta;
            double timeElDelta;
            double timeAz2Delta;
            double timeEl2Delta;
            double currentAzDelta;
            double currentElDelta;
            double currentAzPxDelta;
            double currentElPxDelta;
            String currentName;
            String selectedName = "";
            Context context = getContext();
            Calculations.TopographicDataType selectedLookAngle = null;
            float[] centerPx;
            float[] center2Px;

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
                    //remember current orbital, type, if a star, in filter, selection, and if a star in a shown constellation
                    Database.SatelliteData currentOrbital = currentOrbitals[index];
                    boolean haveOrbital = (currentOrbital != null);
                    byte currentType = (haveOrbital ? currentOrbital.getOrbitalType() : Database.OrbitalType.Satellite);
                    boolean isStar = (currentType == Database.OrbitalType.Star);
                    boolean inFilter = (haveOrbital && currentOrbital.getInFilter());
                    boolean inParentFilter = (haveOrbital && (!currentOrbital.getInParentFilterSet() || currentOrbital.getInParentFilter()));
                    boolean haveSelected = haveSelectedOrbital();
                    boolean currentSelected = (selectedOrbitalIndex == index);

                    //if current orbital is set, -in filter or parent filter-, look angle is valid, and using orbital
                    if(haveOrbital && (inFilter || inParentFilter) && (index < currentLookAngles.length) && (!showCalibration || !haveSelected || currentSelected))
                    {
                        //remember current color, look, travel angles, and magnitude properties
                        int currentColor = currentOrbital.getPathColor();
                        double currentMagnitude = currentOrbital.getMagnitude();
                        boolean withinMagnitude = (isStar && currentMagnitude <= starMagnitude);        //note: lower magnitude = brighter star
                        boolean showOrbital = (currentSelected || (!isStar && inFilter) || (isStar && withinMagnitude));
                        Calculations.TopographicDataType currentLookAngle = currentLookAngles[index];
                        CalculateViewsTask.OrbitalView[] currentTravel = (index < travelLookAngles.length ? travelLookAngles[index] : null);

                        //if showing orbital
                        if(showOrbital)
                        {
                            //if current look angle or travel angles are set
                            if(currentLookAngle != null || currentTravel != null)
                            {
                                //setup paint
                                currentPaint.setColor(Globals.getColor((showCalibration && currentSelected ? 70 : 255), currentColor));
                                currentPaint.setStrokeWidth(indicatorThickness);
                            }

                            //if current travel is set, showing paths, not calibrating, and -on selection or -none selected and -not a star, not in parent filter, or not hiding constellation star paths---
                            if(currentTravel != null && showPaths && !showCalibration && (currentSelected || (!haveSelected && (!isStar || !inParentFilter || !hideConstellationStarPaths))))
                            {
                                //remember length and last index
                                travelLength = currentTravel.length;
                                lastTravelIndex = (travelLength - 1);

                                //if using preset path divisions and at least 2 points
                                if(pathDivisions != 0 && travelLength > 1)
                                {
                                    //get distance between start and end, divided by divisions
                                    pathJulianDelta = (currentTravel[lastTravelIndex].julianDate - currentTravel[0].julianDate) / pathDivisions;
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
                                    //remember colors
                                    int baseTextColor = (usingFilledBoxPath ? textColor : currentColor);
                                    int usedTextColor = (currentSelected ? baseTextColor : Globals.getColor(75, baseTextColor));
                                    int usedTextBgColor = (currentSelected ? textBgColor : Globals.getColor(30, textBgColor));
                                    int usedCurrentColor = Globals.getColor(currentSelected ? 150 : 20, currentColor);
                                    int timePathShapeColor = Globals.getColorAdded(10, usedCurrentColor);

                                    //reset first and previous areas along with status
                                    firstArea.setEmpty();
                                    previousArea.setEmpty();
                                    lastTimeIndex = -1;

                                    //set julian dates
                                    julianDateStart = currentTravel[0].julianDate;
                                    julianDateEnd = currentTravel[lastTravelIndex].julianDate;

                                    //go through each view
                                    for(index2 = 0; index2 < travelLength; index2++)
                                    {
                                        //remember current view and status
                                        boolean onFirst = (index2 == 0);
                                        CalculateViewsTask.OrbitalView currentView = currentTravel[index2];
                                        currentAzDelta = Globals.degreeDistance(currentAzDeg, currentView.azimuth);
                                        currentElDelta = Globals.degreeDistance(currentElDeg, currentView.elevation);
                                        centerPx = getCorrectedScreenPoints(currentAzDelta, currentElDelta, currentView.elevation, width, height, degToPxWidth, degToPxHeight);
                                        currentAzPx = centerPx[0];
                                        currentElPx = centerPx[1];

                                        //if on first
                                        if(onFirst)
                                        {
                                            //set last point
                                            azPx[1] = currentAzPx;
                                            elPx[1] = currentElPx;
                                        }

                                        //update line points and status
                                        azPx[0] = azPx[1];
                                        elPx[0] = elPx[1];
                                        azPx[1] = currentAzPx;
                                        elPx[1] = currentElPx;
                                        currentAzPxDelta = Math.abs(azPx[0] - azPx[1]);
                                        currentElPxDelta = Math.abs(elPx[0] - elPx[1]);

                                        //setup paint
                                        currentPaint.setStyle(Paint.Style.STROKE);

                                        //if not on first and not too long to draw
                                        if(!onFirst && currentAzPxDelta < widthDouble && currentElPxDelta < heightDouble)
                                        {
                                            //draw line between last and current view
                                            currentPaint.setColor(usedCurrentColor);
                                            canvas.drawLine(azPx[0], elPx[0], azPx[1], elPx[1], currentPaint);
                                        }

                                        //if -on first- or -on last- or --enough pixels between views- and -on last- or --needed time between views- and -more than 1/4 delta before end---
                                        if(onFirst || (index2 == lastTravelIndex) || ((currentAzPxDelta >= timeCirclePxRadius || currentElPxDelta >= timeCirclePxRadius) && (currentView.julianDate - julianDateStart >= pathJulianDelta) && (currentView.julianDate + pathJulianEndDelta < julianDateEnd)))
                                        {
                                            RectF bgArea;
                                            boolean closeArea = (Math.abs(currentAzDelta) <= (CLOSE_AREA_DEGREES / cameraZoomRatio) && Math.abs(currentElDelta) <= (CLOSE_AREA_DEGREES / cameraZoomRatio));
                                            boolean showPathTime = (!hideDistantPathTimes || closeArea);
                                            String usedTimeString = (showPathTimeNames ? currentView.getNameTimeString() : currentView.timeString);

                                            //if showing path time
                                            if(showPathTime)
                                            {
                                                //draw circle
                                                canvas.drawCircle(azPx[1], elPx[1], timeCirclePxRadius, currentPaint);
                                            }

                                            //get text area
                                            currentPaint.setTextSize(textSize);
                                            if(currentView.timeArea.isEmpty())
                                            {
                                                currentPaint.getTextBounds(usedTimeString, 0, usedTimeString.length(), currentView.timeArea);
                                            }
                                            currentView.timeArea.offsetTo((int) (azPx[1] - (currentView.timeArea.width() / 2f)), (int) (elPx[1] - textSize));

                                            //get background area
                                            bgArea = new RectF(currentView.timeArea.left - textPadding, currentView.timeArea.top - textOffset - textPadding, currentView.timeArea.right + textPadding, (currentView.timeArea.bottom - textOffset) + textPadding);

                                            //if -first area empty or not intersecting with current- and -previous area is empty or not intersecting with current-
                                            if((firstArea.isEmpty() || !firstArea.intersect(bgArea)) && (previousArea.isEmpty() || !previousArea.intersect(bgArea)))
                                            {
                                                //if showing path time
                                                if(showPathTime)
                                                {
                                                    //if using filled boxed
                                                    if(usingFilledBoxPath)
                                                    {
                                                        //draw text background
                                                        currentPaint.setColor(usedTextBgColor);
                                                        canvas.drawRect(bgArea, currentPaint);

                                                        //draw text border
                                                        currentPaint.setColor(usedCurrentColor);
                                                        currentPaint.setStyle(Paint.Style.STROKE);
                                                        canvas.drawRect(bgArea, currentPaint);
                                                    }

                                                    //set text draw style
                                                    currentPaint.setStyle(Paint.Style.FILL);

                                                    //if using color text
                                                    if(usingColorTextPath)
                                                    {
                                                        //draw shadow
                                                        currentPaint.setColor(textShadowColor);
                                                        canvas.drawText(usedTimeString, currentView.timeArea.left + textShadowOffset, currentView.timeArea.top + textShadowOffset, currentPaint);
                                                    }

                                                    //draw text
                                                    currentPaint.setColor(usedTextColor);
                                                    canvas.drawText(usedTimeString, currentView.timeArea.left, currentView.timeArea.top, currentPaint);
                                                }

                                                //if showing path directions
                                                if(showPathDirections)
                                                {
                                                    //if at least 1 valid previous time
                                                    if(lastTimeIndex >= 0 && lastTimeIndex < travelLength)
                                                    {
                                                        //if can get time in between current and last
                                                        usedTimeIndex = lastTimeIndex + ((index2 - lastTimeIndex) / 2);
                                                        if(usedTimeIndex > 0 && usedTimeIndex < travelLength)
                                                        {
                                                            //remember last and between time view
                                                            CalculateViewsTask.OrbitalView lastTimeView = currentTravel[lastTimeIndex];
                                                            CalculateViewsTask.OrbitalView betweenTimeView = currentTravel[usedTimeIndex];

                                                            //get middle point deltas and screen location
                                                            timeAzDelta = Globals.degreeDistance(currentAzDeg, betweenTimeView.azimuth);
                                                            timeElDelta = Globals.degreeDistance(currentElDeg, betweenTimeView.elevation);
                                                            centerPx = getCorrectedScreenPoints(timeAzDelta, timeElDelta, betweenTimeView.elevation, width, height, degToPxWidth, degToPxHeight);

                                                            //get last point deltas and screen location
                                                            timeAz2Delta = Globals.degreeDistance(currentAzDeg, lastTimeView.azimuth);
                                                            timeEl2Delta = Globals.degreeDistance(currentElDeg, lastTimeView.elevation);
                                                            center2Px = getCorrectedScreenPoints(timeAz2Delta, timeEl2Delta, lastTimeView.elevation, width, height, degToPxWidth, degToPxHeight);

                                                            //get direction
                                                            angleDirection = Calculations.getBearing(centerPx[0], centerPx[1], center2Px[0], center2Px[1]);

                                                            //set relative center, rotation, and color
                                                            currentPaint.setColor(timePathShapeColor);
                                                            currentPaint.setStyle(Paint.Style.FILL);
                                                            canvas.save();
                                                            canvas.translate(centerPx[0], centerPx[1]);
                                                            canvas.rotate((float)angleDirection - 90);

                                                            //draw direction shape
                                                            timePathShape.reset();
                                                            timePathShape.moveTo(0, -pathDirectionShapeLength);
                                                            timePathShape.lineTo(pathDirectionShapeLength, pathDirectionShapeLength);
                                                            timePathShape.lineTo(0, 0);
                                                            timePathShape.lineTo(-pathDirectionShapeLength, pathDirectionShapeLength);
                                                            timePathShape.lineTo(0, -pathDirectionShapeLength);
                                                            timePathShape.close();
                                                            canvas.drawPath(timePathShape, currentPaint);

                                                            //restore canvas rotation
                                                            canvas.restore();
                                                        }
                                                    }
                                                }
                                                lastTimeIndex = index2;

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
                        }

                        //if -current look angle is set- and -on selection, a star -in parent filter or within magnitude-, or in filter-
                        if(currentLookAngle != null && (currentSelected || (isStar && (inParentFilter || withinMagnitude)) || inFilter))
                        {
                            //determine relative location and remember current ID and name
                            RelativeLocationProperties relativeProperties = getRelativeLocationProperties(currentAzDeg, currentElDeg, currentLookAngle.azimuth, currentLookAngle.elevation, width, height, degToPxWidth, degToPxHeight, cameraZoomRatio, !isStar || !inParentFilter);
                            currentId = currentOrbital.getSatelliteNum();
                            currentName = currentOrbital.getName();

                            //if on selection
                            if(currentSelected)
                            {
                                //update selected properties
                                selectedId = currentId;
                                selectedType = currentType;
                                selectedName = currentName;
                                selectedColor = currentColor;
                                selectedCloseArea = relativeProperties.closeArea;
                                selectedOutsideArea = relativeProperties.outsideArea;
                                selectedLookAngle = currentLookAngle;
                            }

                            //if a star in a shown constellation
                            if(isStar && inParentFilter)
                            {
                                //set parent points
                                setParentPoints(currentOrbital, relativeProperties);
                            }

                            //if showing orbital
                            if(showOrbital)
                            {
                                //draw orbital
                                drawOrbital(context, canvas, currentId, currentType, currentName, currentColor, currentOrbitalAreas[index], relativeProperties.azCenterPx, relativeProperties.elCenterPx, currentLookAngle.azimuth, currentLookAngle.elevation, currentAzDeg, currentElDeg, indicatorPxRadius, width, height, degToPxWidth, degToPxHeight, currentSelected, relativeProperties.outsideArea);
                            }
                        }
                    }
                }

                //set default paint stroke and width
                currentPaint.setStyle(Paint.Style.STROKE);
                currentPaint.setStrokeWidth(indicatorThickness);

                //if calibrating
                if(showCalibration)
                {
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
                        drawOrbital(context, canvas, selectedId, selectedType, selectedName, selectedColor, selectedArea, alignCenterX, alignCenterY, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, indicatorPxRadius, width, height, degToPxWidth, degToPxHeight, true, false);
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
                    //go through parent orbitals
                    for(index = 0; index < parentOrbitals.size(); index++)
                    {
                        //remember current parent, orbital, and child properties
                        ParentOrbital currentParentOrbital = parentOrbitals.get(index);
                        int orbitalIndex = currentParentOrbital.index;
                        Database.SatelliteData currentOrbital = (orbitalIndex >= 0 && orbitalIndex < currentOrbitals.length ? currentOrbitals[orbitalIndex] : null);
                        RelativeLocationProperties[][] currentChildProperties = currentParentOrbital.childProperties;

                        //if able to get orbital, child properties, and in filter
                        if(currentOrbital != null && currentChildProperties != null && currentOrbital.getInFilter())
                        {
                            boolean notSelected = (currentOrbital.getSatelliteNum() != selectedNoradId);

                            //set line color
                            currentPaint.setColor(currentOrbital.getPathColor());

                            //make transparent
                            currentPaint.setAlpha(notSelected ? constellationAlpha : constellationSelectedAlpha);

                            //go through child properties
                            for(RelativeLocationProperties[] currentPoint : currentChildProperties)
                            {
                                //remember points and if have both
                                boolean haveBothPoints = (currentPoint.length == 2);
                                RelativeLocationProperties startPoint = (haveBothPoints ? currentPoint[0] : null);
                                RelativeLocationProperties endPoint = (haveBothPoints ? currentPoint[1] : null);

                                //if both points are set and at least 1 is within view
                                if(startPoint != null && endPoint != null && (!startPoint.outsideArea || !endPoint.outsideArea))
                                {
                                    //draw line between points
                                    canvas.drawLine(startPoint.azCenterPx, startPoint.elCenterPx, endPoint.azCenterPx, endPoint.elCenterPx, currentPaint);
                                }
                            }

                            //restore transparency
                            currentPaint.setAlpha(255);
                        }
                    }

                    //draw compass
                    compassCenterX = ((width - (compassWidth / 2f)) - 5) - compassMargin;
                    compassCenterY = (5 + (compassHeight / 2f)) + compassMargin;
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

                //if showing arrow direction and have a selection
                if(showingArrowDirection && haveSelectedOrbital())
                {
                    Calculations.TopographicDataType usedLookAngle = (showCalibration && calibrateIndex >= 0 ? (calibrateIndex < calibrateAngles.length ? calibrateAngles[calibrateIndex] : null) : selectedLookAngle);
                    boolean haveLookAngle = (usedLookAngle != null);
                    RelativeLocationProperties relativeCalibrateProperties = (showCalibration && haveLookAngle ? getRelativeLocationProperties(currentAzDeg, currentElDeg, usedLookAngle.azimuth, usedLookAngle.elevation, width, height, degToPxWidth, degToPxHeight, cameraZoomRatio, true) : null);
                    boolean haveProperties = (relativeCalibrateProperties != null);
                    boolean closeArea = (showCalibration ? (!haveProperties || relativeCalibrateProperties.closeArea) : selectedCloseArea);
                    boolean outsideArea = (showCalibration ? (!haveProperties || relativeCalibrateProperties.outsideArea) : selectedOutsideArea);

                    //if not too close and have look angles
                    if(!closeArea && haveLookAngle)
                    {
                        //draw arrow direction
                        angleDirection = Globals.getReverseAngleDirection(currentAzDeg, currentElDeg, usedLookAngle.azimuth, usedLookAngle.elevation);
                        Globals.drawBitmap(canvas, (outsideArea ? arrowDoubleDirection : arrowDirection), widthHalf, heightHalf, (arrowDirectionCentered ? 0 : ((CLOSE_AREA_DEGREES / 2) * (degToPxHeight / cameraZoomRatio))), (float)(angleDirection + 90), currentPaint);
                    }
                }

                //update status
                pendingOnDraw = false;
            }
        }
    }

    public CameraLens(Context context, Database.SatelliteData[] selectedOrbitals, boolean needConstellations)
    {
        super(context);

        int index;
        int pathType;
        int arrowDp = Settings.getLensDirectionSize(context);
        int averageCount = Settings.getLensAverageCount(context);
        int possibleAlpha;
        boolean darkTheme = Settings.getDarkTheme(context);
        Resources currentResources = context.getResources();
        DisplayMetrics metrics = currentResources.getDisplayMetrics();
        float[] dpPixels = Globals.dpsToPixels(context, 2, 5, 4, 16, 42, 14);

        selectedOrbitalIndex = -1;
        selectedNoradId = Universe.IDs.None;
        orientation = getCameraOrientation();
        textAlpha = Settings.getLensTextAlpha(context);
        textColor = (darkTheme ? Color.argb(160, 255, 255, 255) : Color.argb(160, 0, 0, 0));
        textBgColor = (darkTheme ? Color.argb(50, 0, 0, 0) : Color.argb(50, 255, 255, 255));
        horizonLineColor = Settings.getLensHorizonColor(context);
        horizonColor = Globals.getColor(70, horizonLineColor);
        textShadowColor = 0x50000000;
        textShadowOffset = 3;
        constellationAlpha = Settings.getLensConstellationAlpha(context);
        possibleAlpha = constellationAlpha + 100;
        constellationSelectedAlpha = (possibleAlpha > 200 ? Math.max(200, constellationAlpha) : Math.max(120, possibleAlpha));
        showPaths = showCalibration = compassBad = compassHadBad = false;
        allowZoom = Settings.getLensUseZoom(context);
        allowExposure = Settings.getLensUseExposure(context);
        showHorizon = Settings.getLensShowHorizon(context);
        showOutsideArea = Settings.getLensShowOutsideArea(context);
        hideDistantPathTimes = Settings.getLensHideDistantPathTimes(context);
        showPathDirections = Settings.getLensShowPathDirection(context);
        showPathTimeNames = Settings.getLensShowPathTimeNames(context);
        hideConstellationStarPaths = Settings.getLensHideConstellationStarPaths(context);
        showIconIndicatorDirection = Settings.getIndicatorIconShowDirection(context);
        xString = context.getString(R.string.text_x);
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 13, metrics);
        textOffset = textSize / 1.5f;
        textPadding = (textSize * 0.15f);
        starTextSize = textSize * STAR_TEXT_SCALE;
        starTextOffset = textOffset * STAR_TEXT_SCALE;
        starMagnitude = Settings.getLensStarMagnitude(context);
        indicatorThickness = dpPixels[0];
        timeCirclePxRadius = dpPixels[1];
        pathDirectionShapeLength = (timeCirclePxRadius * 2);
        iconAlpha = Settings.getLensIndicatorAlpha(context);
        iconLength = (int)dpPixels[4];
        iconHalfLength = (iconLength / 2);
        iconScaleOffset = (iconHalfLength / 2);
        starLength = (int)(dpPixels[4] * STAR_IMAGE_SCALE);
        starHalfLength = (starLength / 2);
        cameraHardwareDegWidth = cameraHardwareDegHeight = 45;
        cameraDegWidth = cameraDegHeight = useCameraDegWidth = useCameraDegHeight = Float.MAX_VALUE;
        cameraZoomRatio = 1.0f;
        indicator = Settings.getIndicator(context);
        indicatorPxRadius = Globals.dpToPixels(context, 24);
        pathType = Settings.getLensPathLabelType(context);
        usingFilledBoxPath = (pathType == Settings.Options.LensView.PathLabelType.FilledBox);
        usingColorTextPath = (pathType == Settings.Options.LensView.PathLabelType.ColorText);
        resetAlignmentStatus();
        timePathShape = new Path();
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

        currentCameraView = new PreviewView(context);
        currentCameraView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        this.addView(currentCameraView);

        currentCameraOverlay = new LensOverlay(context);
        this.addView(currentCameraOverlay);

        starIconImage = null;
        zoomImage = Globals.getDrawable(context, R.drawable.ic_search_white, R.color.white);
        exposureImage = Globals.getDrawable(context, R.drawable.ic_sun_white, R.color.white);
        arrowDirectionCentered = Settings.getLensDirectionCentered(context);
        showingArrowDirection = (arrowDp > 0);
        arrowDirection = (showingArrowDirection ? Globals.getBitmap(Globals.getDrawableCombined(context, 5, 5, true, Globals.getDrawableSized(context, R.drawable.ic_arrow_up_black, arrowDp, arrowDp, R.color.white, true), Globals.getDrawableSized(context, R.drawable.ic_arrow_up_black, arrowDp, arrowDp, R.color.black, true))) : null);
        arrowDoubleDirection = (showingArrowDirection ? Globals.getBitmap(Globals.getDrawableCombined(context, 5, 5, true, Globals.getDrawableSized(context, R.drawable.ic_arrow_up_double_black, arrowDp, arrowDp, R.color.white, true), Globals.getDrawableSized(context, R.drawable.ic_arrow_up_double_black, arrowDp, arrowDp, R.color.black, true))) : null);
        compassDirection = BitmapFactory.decodeResource(currentResources, R.drawable.compass);
        compassWidth = compassDirection.getWidth();
        compassHeight = compassDirection.getHeight();
        compassCenterX = compassCenterY = compassBorderWidth = 0;
        compassMargin = (int)dpPixels[5];
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
        readyListener = null;
        stopCalibrationListener = null;
        scaleDetector = null;

        showingConstellations = needConstellations;

        haveZoomValues = false;
        exposureIncrement = 1.0f;
        exposureRange = null;
        orbitalIcons = new ArrayList<>(0);
        parentOrbitals = new ArrayList<>(0);
        currentOrbitals = (selectedOrbitals != null ? selectedOrbitals : new Database.SatelliteData[0]);
        currentOrbitalAreas = new Rect[0];
        calibrateAngles = new Calculations.TopographicDataType[3];
        currentLookAngles = new Calculations.TopographicDataType[0];
        travelLookAngles = new CalculateViewsTask.OrbitalView[0][0];

        resetParentStates(currentOrbitals);
    }
    public CameraLens(Context context)
    {
        this(context, null, false);
    }

    @Override
    protected void onAttachedToWindow()
    {
        super.onAttachedToWindow();

        startCamera();

        //if listener set
        if(readyListener != null)
        {
            //call it
            readyListener.ready();
        }
    }

    @Override
    protected void onDetachedFromWindow()
    {
        super.onDetachedFromWindow();

        stopCamera(true);
    }

    @Override
    public boolean onInterceptHoverEvent(MotionEvent event)
    {
        return(true);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        int action = event.getAction();
        int pointerCount = event.getPointerCount();
        float x = event.getX();
        float y = event.getY();
        float compassHalfWidth = compassWidth / 2.0f;
        float compassHalfHeight = compassHeight / 2.0f;
        CameraControl cameraControls = (currentCamera != null ? currentCamera.getCameraControl() : null);

        //if multi touch, allowing zoom, have zoom values, and scale detection is set
        if(pointerCount > 1 && allowZoom && haveZoomValues && scaleDetector != null)
        {
            //stop focus and handle zoom scaling
            handleFocus(event, cameraControls, false);
            scaleDetector.onTouchEvent(event);
        }
        //else if compass reading is bad and within compass image bounds
        else if(compassBad && x >= (compassCenterX - compassHalfWidth) && x <= (compassCenterX + compassHalfWidth) && (y >= compassCenterY - compassHalfHeight) && (y <= compassCenterY + compassHalfHeight))
        {
            //show compass error dialog
            showCompassErrorDialog();
        }
        //else if released
        else if(action == MotionEvent.ACTION_UP)
        {
            //start focus
            handleFocus(event, cameraControls, true);
        }
        //else normal click
        else
        {
            performClick();
            super.onTouchEvent(event);
        }

        //return handled
        return(true);
    }

    @Override
    public boolean performClick()
    {
        return(super.performClick());
    }

    //Shows disappearing slider text
    private void setSliderText(Drawable imageValue, double previousValue, double currentValue, double nextValue)
    {
        boolean havePrevious = (previousValue != Double.MAX_VALUE);
        boolean haveNext = (nextValue != Double.MAX_VALUE);
        StringBuilder text = new StringBuilder();

        //if text exists
        if(sliderText != null)
        {
            //update text and hide shortly afterwards
            sliderText.setCompoundDrawablesRelativeWithIntrinsicBounds(imageValue, null, null, null);
            sliderText.removeCallbacks(null);
            text.append("<small>");
            if(havePrevious)
            {
                text.append(Globals.getNumberString(previousValue, 1));
                text.append(xString);
            }
            else
            {
                text.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
            }
            text.append("</small><b><big>&nbsp;&nbsp;");
            if(havePrevious)
            {
                text.append("|");
            }
            text.append("&nbsp;&nbsp;");
            text.append(Globals.getNumberString(currentValue, 1));
            text.append(xString);
            text.append("&nbsp;&nbsp;");
            if(haveNext)
            {
                text.append("|");
            }
            text.append("&nbsp;&nbsp;</big></b><small>");
            if(haveNext)
            {
                text.append(Globals.getNumberString(nextValue, 1));
                text.append(xString);
            }
            else
            {
                text.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
            }
            text.append("</small>");
            sliderText.setText(Globals.stringToHtml(text.toString()));
            sliderText.setVisibility(View.VISIBLE);
            sliderText.postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    sliderText.setVisibility(View.GONE);
                }
            }, 3000);
        }
    }

    //Sets exposure
    public void setExposure(int exposureIndex, CameraControl controls, boolean showText)
    {
        int nextIndex;
        int previousIndex;
        int minExposure;
        int maxExposure;

        //if allowing exposure, controls exist, and have exposure values
        if(allowExposure && controls != null && exposureRange != null)
        {
            //get min and max values
            minExposure = exposureRange.getLower();
            maxExposure = exposureRange.getUpper();

            //keep exposure within range
            if(exposureIndex < minExposure)
            {
                exposureIndex = minExposure;
            }
            else if(exposureIndex > maxExposure)
            {
                exposureIndex = maxExposure;
            }

            //remember previous and next indexes
            previousIndex = exposureIndex - 1;
            nextIndex = exposureIndex + 1;

            //if using exposure
            if(exposureRange.contains(exposureIndex))
            {
                //set exposure
                controls.setExposureCompensationIndex(exposureIndex);

                //if exposure bar exists
                if(exposureBar != null)
                {
                    //update value
                    exposureBar.setValue(exposureIndex);
                }
    
                //if showing text
                if(showText)
                {
                    //set text
                    setSliderText(exposureImage, (previousIndex < minExposure ? Double.MAX_VALUE : (previousIndex * exposureIncrement)), exposureIndex * exposureIncrement, (nextIndex > maxExposure ? Double.MAX_VALUE : (nextIndex * exposureIncrement)));
                }
            }
        }
    }

    //Sets zoom
    private void setZoom(float zoom, CameraControl controls, boolean showText)
    {
        float nextZoom;
        float previousZoom;

        //if allowing zoom, controls exist, and have zoom values
        if(allowZoom && controls != null && haveZoomValues)
        {
            //keep zoom within range
            if(zoom > cameraZoomMax)
            {
                zoom = cameraZoomMax;
            }
            else if(zoom < cameraZoomMin)
            {
                zoom = cameraZoomMin;
            }

            //remember previous and next zooms
            previousZoom = zoom - 1;
            nextZoom = zoom + 1;

            //set zoom
            cameraZoomRatio = zoom;
            controls.setZoomRatio(zoom);

            //if zoom bar exists
            if(zoomBar != null)
            {
                //update value
                zoomBar.setValue(zoom);
            }

            //if showing text
            if(showText)
            {
                //set text
                setSliderText(zoomImage, (previousZoom < cameraZoomMin ? Double.MAX_VALUE : previousZoom), zoom, (nextZoom > cameraZoomMax ? Double.MAX_VALUE : nextZoom));
            }
        }
    }
    private void setZoom(float zoom, CameraControl controls)
    {
        setZoom(zoom, controls, true);
    }
    public void setZoom(float zoom)
    {
        setZoom(zoom, (currentCamera != null ? currentCamera.getCameraControl() : null));
    }

    //Handles focus
    private void handleFocus(MotionEvent event, CameraControl controls, boolean use)
    {
        //if controls exist
        if(controls != null)
        {
            //if using
            if(use)
            {
                //start focus on touched point
                controls.startFocusAndMetering(new FocusMeteringAction.Builder(new SurfaceOrientedMeteringPointFactory(currentCameraOverlay.getWidth(), currentCameraOverlay.getHeight()).createPoint(event.getX(), event.getY()), FocusMeteringAction.FLAG_AF).build());
            }
            else
            {
                //stop focus
                controls.cancelFocusAndMetering();
            }
        }
    }

    //Draws orbital at the given position
    private void drawOrbital(Context context, Canvas canvas, int noradId, byte currentType, String currentName, int currentColor, Rect currentArea, float orbitalCenterX, float orbitalCenterY, double orbitalAzimuth, double orbitalElevation, double currentAzDeg, double currentElDeg, float indicatorPxRadius, int canvasWidth, int canvasHeight, float degToPxWidth, float degToPxHeight, boolean isSelected, boolean outsideArea)
    {
        boolean isStar = (currentType == Database.OrbitalType.Star);
        boolean isConstellation = (currentType == Database.OrbitalType.Constellation);
        float drawPxRadius = (indicatorPxRadius / (outsideArea ? 2 : 1)) * (isStar ? STAR_IMAGE_SCALE : 1);
        float usedTextSize = (isStar ? starTextSize : (textSize * 1.2f));
        float usedTextOffset = (isStar ? starTextOffset : textOffset);

        //if -not selected, outside of area, and not showing- or -showing calibration and -is a star or constellation--
        if((!isSelected && outsideArea && !showOutsideArea) || (showCalibration && (isStar || isConstellation)))
        {
            //stop and don't draw
            return;
        }

        //setup paint
        currentPaint.setColor(currentColor);
        currentPaint.setStyle(Paint.Style.STROKE);
        currentPaint.setAlpha(iconAlpha);

        //if not a constellation
        if(!isConstellation)
        {
            //draw indicator
            switch(indicator)
            {
                case Settings.Options.LensView.IndicatorType.Square:
                    canvas.drawRect(orbitalCenterX - drawPxRadius, orbitalCenterY - drawPxRadius, orbitalCenterX + drawPxRadius, orbitalCenterY + drawPxRadius, currentPaint);
                    break;

                case Settings.Options.LensView.IndicatorType.Triangle:
                    trianglePoints[0] = orbitalCenterX - drawPxRadius;        //bottom left
                    trianglePoints[1] = orbitalCenterY + drawPxRadius;        //bottom left
                    trianglePoints[2] = orbitalCenterX + drawPxRadius;        //bottom right
                    trianglePoints[3] = orbitalCenterY + drawPxRadius;        //bottom right

                    trianglePoints[4] = orbitalCenterX + drawPxRadius;        //bottom right
                    trianglePoints[5] = orbitalCenterY + drawPxRadius;        //bottom right
                    trianglePoints[6] = orbitalCenterX;                       //top center
                    trianglePoints[7] = orbitalCenterY - drawPxRadius;        //top center

                    trianglePoints[8] = orbitalCenterX;                       //top center
                    trianglePoints[9] = orbitalCenterY - drawPxRadius;        //top center
                    trianglePoints[10] = orbitalCenterX - drawPxRadius;       //bottom left
                    trianglePoints[11] = orbitalCenterY + drawPxRadius;       //bottom left
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
                            canvas.drawBitmap(starIconImage, orbitalCenterX - starHalfLength, orbitalCenterY - starHalfLength, currentPaint);
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
                                RelativeLocationProperties relativeProperties;

                                //use center and image
                                indicatorIcon.copyData(currentOrbitalIcon);
                                if(showIconIndicatorDirection && isSatellite)
                                {
                                    relativeProperties = getRelativeLocationProperties(currentAzDeg, currentElDeg, indicatorIcon.lastAzimuth, indicatorIcon.lastElevation, canvasWidth, canvasHeight, degToPxWidth, degToPxHeight, 1.0f, false);
                                    angleDirection = indicatorIcon.getAngleDirection(orbitalAzimuth, orbitalElevation, orbitalCenterX, orbitalCenterY, relativeProperties.azCenterPx, relativeProperties.elCenterPx);
                                    directionDelta = indicatorIcon.getAngleDirectionDelta();
                                }
                                if(angleDirection == Double.MAX_VALUE)
                                {
                                    angleDirection = 135;
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
                            iconArea.set((int)(orbitalCenterX - iconHalfLength), (int)(orbitalCenterY - iconHalfLength), (int)(orbitalCenterX + iconHalfLength), (int)(orbitalCenterY + iconHalfLength));
                            if(outsideArea)
                            {
                                iconScaledArea.set(iconArea.left + iconScaleOffset, iconArea.top + iconScaleOffset, iconArea.right - iconScaleOffset, iconArea.bottom - iconScaleOffset);
                                canvas.drawBitmap(indicatorIcon.image, null, iconScaledArea, currentPaint);
                            }
                            else
                            {
                                canvas.drawBitmap(indicatorIcon.image, iconArea.left, iconArea.top, currentPaint);
                            }
                        }
                        break;
                    }
                    //else fall through

                default:
                case Settings.Options.LensView.IndicatorType.Circle:
                    canvas.drawCircle(orbitalCenterX, orbitalCenterY, drawPxRadius, currentPaint);
                    break;
            }
        }

        //get text area
        currentPaint.setStrokeWidth(2);
        currentPaint.setTextSize(usedTextSize);
        if(currentArea != null)
        {
            if(currentArea.isEmpty())
            {
                //get area
                currentPaint.getTextBounds(currentName, 0, currentName.length(), currentArea);
            }
            currentArea.offsetTo((int)(orbitalCenterX - (currentArea.width() / 2f)), (int)((orbitalCenterY - (isConstellation ? 0 : indicatorPxRadius) - usedTextSize) + (usedTextOffset * (isStar ? 5.0f : indicator == Settings.Options.LensView.IndicatorType.Icon ? 2 : 1))));
            if(currentArea.left < 20)
            {
                //keep within left
                currentArea.offsetTo(20, currentArea.top);
            }
            if(currentArea.right > canvasWidth)
            {
                //keep within right
                currentArea.offsetTo(canvasWidth - currentArea.width(), currentArea.top);
            }
            if(currentArea.top < 20)
            {
                //keep within top
                currentArea.offsetTo(currentArea.left, 20);
            }
            if(currentArea.bottom > canvasHeight)
            {
                //keep within bottom
                currentArea.offsetTo(currentArea.left, canvasHeight - currentArea.height());
            }

            //draw shadow
            currentPaint.setColor(textShadowColor);
            currentPaint.setStyle(Paint.Style.FILL);
            canvas.drawText(currentName, currentArea.left + textShadowOffset, currentArea.top + textShadowOffset, currentPaint);

            //draw text
            currentPaint.setColor(currentColor);
            currentPaint.setAlpha(isSelected ? 255 : textAlpha);
            canvas.drawText(currentName, currentArea.left, currentArea.top, currentPaint);

            //if not selected
            if(!isSelected)
            {
                //restore transparency
                currentPaint.setAlpha(255);
            }
        }
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
                Settings.setLensFirstCalibrate(context, false);
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
        return((width / (useCameraDegWidth != Float.MAX_VALUE ? useCameraDegWidth : 1)) * cameraZoomRatio);
    }

    //Gets degree to pixel conversion for given height
    private float getDegreeToPixelHeight(int height)
    {
        return((height / (useCameraDegHeight != Float.MAX_VALUE ? useCameraDegHeight : 1)) * cameraZoomRatio);
    }

    //Gets screen points with spherical correction
    private float[] getCorrectedScreenPoints(double azDeltaDegrees, double elDeltaDegrees, double locationElDeg, int width, int height, float degToPxWidth, float degToPxHeight)
    {
        float azPx;
        float elPx;
        float currentAzScale = (showCalibration ? 1 : (float)Math.cos(Math.toRadians(locationElDeg)));
        float elDeltaReference = (showCalibration ? 0 : (float)(locationElDeg > 0 ? (90 - locationElDeg) : (locationElDeg + 90)));
        float elDeltaMultiply = (showCalibration ? 0 : (float)((Math.cos(Math.toRadians(azDeltaDegrees)) - 1) * -1));
        float elOffset = (showCalibration ? 0 : (elDeltaReference * elDeltaMultiply) * (locationElDeg > 0 ? 1 : -1) * (1 - currentAzScale));

        azPx = (width / 2f) + (float)(azDeltaDegrees * degToPxWidth * currentAzScale);
        elPx = (height / 2f) - (float)((elDeltaDegrees + elOffset) * degToPxHeight);
        return(new float[]{azPx, elPx});
    }

    //Gets relative location properties
    private RelativeLocationProperties getRelativeLocationProperties(double currentAzDeg, double currentElDeg, double locationAzDeg, double locationElDeg, int width, int height, float degToPxWidth, float degToPxHeight, float zoomRatio, boolean normalize)
    {
        float azCenterPx;
        float elCenterPx;
        float usedCloseDegrees = (CLOSE_AREA_DEGREES / zoomRatio);
        double azDeltaDegrees = Globals.degreeDistance(currentAzDeg, locationAzDeg);
        double elDeltaDegrees = Globals.degreeDistance(currentElDeg, locationElDeg);
        boolean closeArea;
        boolean outsideArea = false;
        float[] centerPx = getCorrectedScreenPoints(azDeltaDegrees, elDeltaDegrees, locationElDeg, width, height, degToPxWidth, degToPxHeight);

        //get centers and area properties
        azCenterPx = centerPx[0];
        if(azCenterPx > width)
        {
            if(normalize)
            {
                azCenterPx = width;
            }
            outsideArea = true;
        }
        else if(azCenterPx < 0)
        {
            if(normalize)
            {
                azCenterPx = 0;
            }
            outsideArea = true;
        }
        elCenterPx = centerPx[1];
        if(elCenterPx > height)
        {
            if(normalize)
            {
                elCenterPx = height;
            }
            outsideArea = true;
        }
        else if(elCenterPx < 0)
        {
            if(normalize)
            {
                elCenterPx = 0;
            }
            outsideArea = true;
        }
        closeArea = (Math.abs(azDeltaDegrees) <= usedCloseDegrees && Math.abs(elDeltaDegrees) <= usedCloseDegrees);

        //return results
        return(new RelativeLocationProperties(closeArea, outsideArea, azCenterPx, elCenterPx));
    }

    //Update orbital positions to display
    public void updatePositions(Database.SatelliteData[] orbitals, Calculations.TopographicDataType[] lookAngles, boolean checkLength)
    {
        int index;
        int orbitalsLength;
        boolean changed = false;

        //if there are orbitals and look angles that match in size
        if(orbitals != null && lookAngles != null && orbitals.length > 0 && (!checkLength || lookAngles.length == orbitals.length))
        {
            //update orbitals and look angles
            orbitalsLength = orbitals.length;
            if(checkLength && currentOrbitals.length != orbitalsLength)
            {
                //reset parent states
                resetParentStates(orbitals);
            }
            currentOrbitals = orbitals;
            currentLookAngles = lookAngles;

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
                changed = true;
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
                changed = true;
            }

            //if -parents orbitals not set and needed- or -need to be changed-
            if((parentOrbitals.isEmpty() && showingConstellations) || (checkLength && changed))
            {
                //clear parent orbitals
                parentOrbitals.clear();

                //go through each orbital
                for(index = 0; index < currentOrbitals.length; index++)
                {
                    //remember current orbital and lines
                    Database.SatelliteData currentOrbital = currentOrbitals[index];
                    Database.IdLine[] currentLines = currentOrbital.getLines();

                    //if a constellation with children
                    if(currentOrbital.getOrbitalType() == Database.OrbitalType.Constellation && currentLines != null)
                    {
                        //add to parent orbital list
                        parentOrbitals.add(new ParentOrbital(currentOrbital.getSatelliteNum(), index, currentLines.length));
                    }
                }

                //sort parent orbitals
                Collections.sort(parentOrbitals, parentOrbitalComparer);
            }

            //if no change, need to set in parent filter status, and not waiting for a draw
            if(!changed && pendingSetInParentFilter && !pendingOnDraw)
            {
                //update parent in filter status
                updateInParentFilter();
                pendingSetInParentFilter = false;
            }
        }
    }

    //Updates the display
    public void updateDisplay()
    {
        if(currentCameraOverlay != null)
        {
            currentCameraOverlay.postInvalidate();
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

    //Sets star magnitude
    public void setStarMagnitude(float magnitude)
    {
        //set magnitude
        starMagnitude = magnitude;
    }

    //Resets parent states
    public void resetParentStates(Database.SatelliteData[] orbitals)
    {
        //go through each orbital
        for(Database.SatelliteData currentOrbital : orbitals)
        {
            //remember any parent properties
            ArrayList<Database.ParentProperties> currentParentProperties = currentOrbital.getParentProperties();
            if(currentParentProperties != null)
            {
                //go through parent properties
                for(Database.ParentProperties currentParentProperty : currentParentProperties)
                {
                    //clear old index
                    currentParentProperty.index = -1;
                }
            }

            //reset in parent filter status
            currentOrbital.resetInParentFilter();
        }

        //need wait for a draw and to set in parent filter status
        pendingOnDraw = pendingSetInParentFilter = true;
    }

    //Sets parent points
    private void setParentPoints(Database.SatelliteData currentOrbital, RelativeLocationProperties relativeProperties)
    {
        int index;
        int lineIndex;
        int parentIndex;
        int propertyIndex;
        int noradId = currentOrbital.getSatelliteNum();
        ArrayList<Database.ParentProperties> parentProperties = currentOrbital.getParentProperties();

        //if have parents
        if(parentProperties != null)
        {
            //go through each parent ID
            for(propertyIndex = 0; propertyIndex < parentProperties.size(); propertyIndex++)
            {
                //remember current parent property and index
                Database.ParentProperties currentParentProperty = parentProperties.get(propertyIndex);
                parentIndex = currentParentProperty.index;

                //if parent index still unknown
                if(parentIndex < 0)
                {
                    //search for parent index and update
                    parentOrbitalSearch.id = currentParentProperty.id;
                    parentIndex = Globals.divideFind(parentOrbitalSearch, parentOrbitals, parentOrbitalComparer)[0];
                    currentParentProperty.index = parentIndex;
                }

                //if found parent index
                if(parentIndex >= 0 && parentIndex < parentOrbitals.size())
                {
                    //get current parent orbital and data
                    ParentOrbital currentParentOrbital = parentOrbitals.get(parentIndex);
                    index = currentParentOrbital.index;
                    Database.SatelliteData currentParentOrbitalData = (index >= 0 && index < currentOrbitals.length ? currentOrbitals[index] : null);

                    //if found parent orbital data
                    if(currentParentOrbitalData != null)
                    {
                        //get current lines and properties
                        Database.IdLine[] currentLines = currentParentOrbitalData.getLines();
                        RelativeLocationProperties[][] currentProperties = currentParentOrbital.childProperties;

                        //if lines and properties lengths match
                        if(currentLines != null && currentLines.length == currentProperties.length)
                        {
                            //go through each line
                            for(lineIndex = 0; lineIndex < currentLines.length; lineIndex++)
                            {
                                //remember current line
                                Database.IdLine currentLine = currentLines[lineIndex];

                                //if ID is a match
                                if(currentLine.startId == noradId)
                                {
                                    //set start location
                                    currentProperties[lineIndex][0] = relativeProperties;
                                }
                                if(currentLine.endId == noradId)
                                {
                                    //set end location
                                    currentProperties[lineIndex][1] = relativeProperties;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    //Updates if orbitals are in parent filter
    //note: if no parents, parent is considered to be main filter
    public void updateInParentFilter()
    {
        int index;
        int parentIndex;
        int orbitalIndex;
        boolean inFilter;

        //go through each orbital
        for(Database.SatelliteData currentOrbital : currentOrbitals)
        {
            //get any parents
            ArrayList<Database.ParentProperties> parentProperties = currentOrbital.getParentProperties();

            //if no parents
            if(parentProperties == null || parentProperties.isEmpty())
            {
                //remember if in filter directly
                inFilter = currentOrbital.getInFilter();
            }
            else
            {
                //reset
                inFilter = false;

                //go through each parent while not in filter
                for(index = 0; index < parentProperties.size() && !inFilter; index++)
                {
                    //remember current property
                    Database.ParentProperties currentProperty = parentProperties.get(index);

                    //if a valid parent index
                    parentIndex = currentProperty.index;
                    if(parentIndex >= 0 && parentIndex < parentOrbitals.size())
                    {
                        //if parent has a valid orbital index and is in filter
                        orbitalIndex = parentOrbitals.get(parentIndex).index;
                        if(orbitalIndex >= 0 && orbitalIndex < currentOrbitals.length && currentOrbitals[orbitalIndex].getInFilter())
                        {
                            //orbital used by parent in filter
                            inFilter = true;
                        }
                    }
                }
            }

            //set if in parent filter
            currentOrbital.setInParentFilter(inFilter);
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
                Database.SatelliteData currentOrbital = currentOrbitals[index];
                int currentId = (currentOrbital != null ? currentOrbital.getSatelliteNum() : Universe.IDs.Invalid);
                Calculations.TopographicDataType currentLookAngle = (currentOrbital != null ? currentLookAngles[index] : null);

                //if above the horizon, being used, a normally easily visible orbital, and the -current is the selected or there is no selected-
                if(currentLookAngle != null && currentLookAngle.elevation >= 0 && currentId >= Universe.IDs.Polaris && currentId <= Universe.IDs.Sun && (currentId == selectedNoradId || selectedNoradId == Universe.IDs.None))
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

    //Returns if able to zoom
    public boolean canZoom()
    {
        return(allowZoom && haveZoomValues);
    }

    //Returns if can set exposure
    public boolean canSetExposure()
    {
        return(allowExposure && exposureRange != null);
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
        azUserOffset += (float)Globals.degreeDistance(alignCenter.az, alignCenter.orbitalAz);
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

        if(currentCameraProvider != null)
        {
            try
            {
                currentCameraProvider.unbindAll();
            }
            catch(Exception ex)
            {
                //do nothing
            }
            if(kill)
            {
                currentCameraProvider = null;
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

    //Sets on ready listener
    public void setOnReadyListener(OnReadyListener listener)
    {
        readyListener = listener;
    }

    //Sets stop calibration listener
    public void setStopCalibrationListener(OnStopCalibrationListener listener)
    {
        stopCalibrationListener = listener;
    }

    //Sets up the camera
    @OptIn(markerClass = ExperimentalCamera2Interop.class)
    private void setupCamera(Context context, ProcessCameraProvider cameraProvider)
    {
        boolean cameraRotate = Settings.getLensRotate(context);
        boolean useAutoWidth = Settings.getLensAutoWidth(context);
        boolean useAutoHeight = Settings.getLensAutoHeight(context);
        int exposureIndex = 0;
        float maxFocus;
        float userDegWidth = Settings.getLensWidth(context);
        float userDegHeight = Settings.getLensHeight(context);
        String id;
        SizeF sensorSize;
        Preview preview = new Preview.Builder().build();
        CameraInfo info;
        CameraControl controls;
        CameraSelector selector;
        CameraCharacteristics characteristics;
        CameraManager manager = (CameraManager)context.getSystemService(Context.CAMERA_SERVICE);
        ExposureState exposure;
        ZoomState zoomState;
        float[] focalLengths;

        //set provider
        currentCameraProvider = cameraProvider;

        //try to open camera and preview
        selector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
        currentCameraView.setImplementationMode(cameraRotate ? PreviewView.ImplementationMode.COMPATIBLE : PreviewView.ImplementationMode.PERFORMANCE);
        preview.setSurfaceProvider(currentCameraView.getSurfaceProvider());
        currentCamera = currentCameraProvider.bindToLifecycle((LifecycleOwner)context, selector, preview);

        //get camera properties
        info = currentCamera.getCameraInfo();
        controls = currentCamera.getCameraControl();
        id = Camera2CameraInfo.from(info).getCameraId();
        exposure = info.getExposureState();
        try
        {
            //get characteristics
            characteristics = manager.getCameraCharacteristics(id);
            sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
            focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
            maxFocus = focalLengths[0];

            //get hardware degree width and height
            cameraHardwareDegWidth = (float)Math.toDegrees(Math.atan(sensorSize.getWidth() / (maxFocus * 2)));
            cameraHardwareDegHeight = (float)Math.toDegrees(Math.atan(sensorSize.getHeight() / (maxFocus * 2)));

            //save hardware degree width and height
            Settings.setLensWidthHardware(context, cameraHardwareDegWidth);
            Settings.setLensHeightHardware(context, cameraHardwareDegHeight);
        }
        catch(Exception ex)
        {
            //do nothing
        }

        //get desired camera degree view
        cameraDegWidth = (useAutoWidth ? cameraHardwareDegWidth : userDegWidth);
        cameraDegHeight = (useAutoHeight ? cameraHardwareDegHeight : userDegHeight);
        updateUsedWidthHeight();

        //set orientation
        preview.setTargetRotation(getRotation(cameraRotate ? ((orientation + 180) % 360) : orientation));

        //setup exposure
        exposureRange = null;
        exposureIncrement = 1.0f;
        if(exposure.isExposureCompensationSupported())
        {
            exposureIndex = exposure.getExposureCompensationIndex();
            exposureRange = exposure.getExposureCompensationRange();
            exposureIncrement = exposure.getExposureCompensationStep().floatValue();
        }
        if(exposureBar != null)
        {
            if(exposureRange != null)
            {
                exposureBar.setValueFrom(exposureRange.getLower());
                exposureBar.setValueTo(exposureRange.getUpper());
                exposureBar.setValue(exposureIndex);
                exposureBar.setLabelBehavior(LabelFormatter.LABEL_GONE);
                exposureBar.addOnChangeListener(new Slider.OnChangeListener()
                {
                    @Override
                    public void onValueChange(@NonNull Slider slider, float value, boolean fromUser)
                    {
                        //if from the user
                        if(fromUser)
                        {
                            //set exposure
                            setExposure((int)value, controls, true);
                        }
                    }
                });
            }
            exposureBar.setVisibility(allowExposure && exposureRange != null ? View.VISIBLE : View.GONE);
        }

        //setup zoom
        zoomState = info.getZoomState().getValue();
        if(zoomState != null)
        {
            cameraZoomMin = zoomState.getMinZoomRatio();
            cameraZoomMax = zoomState.getMaxZoomRatio();
        }
        else
        {
            cameraZoomMin = cameraZoomMax = 1.0f;
        }
        haveZoomValues = (cameraZoomMin != cameraZoomMax);
        if(haveZoomValues)
        {
            if(cameraZoomMin < 1.0f)
            {
                cameraZoomMin = 1.0f;
            }
            if(cameraZoomMax < 1.0f)
            {
                cameraZoomMax = 1.0f;
            }
        }
        scaleDetector = (allowZoom && haveZoomValues ? new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener()
        {
            @Override
            public boolean onScale(@NonNull ScaleGestureDetector detector)
            {
                ZoomState currentZoomState = info.getZoomState().getValue();
                float currentRatio = (currentZoomState != null ? currentZoomState.getZoomRatio() : 1.0f);

                //set zoom
                setZoom(currentRatio * detector.getScaleFactor(), controls);

                //handled
                return(true);
            }
        }) : null);
        if(zoomBar != null)
        {
            if(haveZoomValues)
            {
                zoomBar.setValueFrom(cameraZoomMin);
                zoomBar.setValueTo(cameraZoomMax);
                zoomBar.setValue(cameraZoomMin);
                zoomBar.setLabelBehavior(LabelFormatter.LABEL_GONE);
                zoomBar.addOnChangeListener(new Slider.OnChangeListener()
                {
                    @Override
                    public void onValueChange(@NonNull Slider slider, float value, boolean fromUser)
                    {
                        //if from the user
                        if(fromUser)
                        {
                            //set zoom
                            setZoom(Math.round(value / 0.5f) * 0.5f, controls);
                        }
                    }
                });
            }
            zoomBar.setVisibility(allowZoom && haveZoomValues ? View.VISIBLE : View.GONE);
        }
        setZoom(1.0f, controls, false);
    }

    //Starts the camera
    public void startCamera(boolean retrying)
    {
        Context context = this.getContext();
        boolean startSensors = false;
        boolean havePermission = Globals.haveCameraPermission(context);
        boolean useCamera = Settings.getLensUseCamera(context);
        float userDegWidth = Settings.getLensWidth(context);
        float userDegHeight = Settings.getLensHeight(context);
        Resources res = context.getResources();
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

        //if using camera and have permission
        if(useCamera && havePermission)
        {
            //stop camera if previously open
            stopCamera(false);

            try
            {
                //try to start camera
                if(currentCameraProvider == null)
                {
                    //get camera provider instance
                    cameraProviderFuture = ProcessCameraProvider.getInstance(context);
                    cameraProviderFuture.addListener(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            try
                            {
                                //setup camera
                                setupCamera(context, cameraProviderFuture.get());
                            }
                            catch (Exception ex)
                            {
                                //throw error
                                throw(new RuntimeException(ex));
                            }
                        }
                    }, ContextCompat.getMainExecutor(context));
                }
                else
                {
                    //setup camera
                    setupCamera(context, currentCameraProvider);
                }

                //start sensors
                startSensors = true;
            }
            catch(Exception ex)
            {
                currentCameraProvider = null;
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
                        startCamera(true);
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

    //Converts degrees to rotation
    private static int getRotation(int degrees)
    {
        switch(degrees)
        {
            case 90:
                return(Surface.ROTATION_90);

            case 180:
                return(Surface.ROTATION_180);

            case 270:
                return(Surface.ROTATION_270);

            default:
            case 0:
                return(Surface.ROTATION_0);
        }
    }

    //Gets current camera orientation
    private int getCameraOrientation()
    {
        int degrees;
        int rotation = Globals.getScreenOrientation(this.getContext());

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

        return(degrees);
    }

    //Returns if in portrait orientation
    private boolean inOrientationPortrait()
    {
        return(orientation == 90 || orientation == 270);
    }
}