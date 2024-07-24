package com.nikolaiapps.orbtrack;


import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;
import android.view.Surface;


public class SensorUpdate implements SensorEventListener
{
    public interface OnSensorChangedListener
    {
        void onSensorChanged(float azDeg, float elDeg, int type, int accuracy);
    }

    private final int axisX;
    private final int axisY;
    private int gyroCount;
    private long gyroTime;
    private final int currentOrientation;
    private float gyroDelaySeconds;
    private final float filterAlpha;
    private float latchedAzDeg;
    private boolean needMagnet;
    private boolean needAccel;
    private boolean needGyro;
    private boolean usingRotation;
    private final SensorManager sensorsManager;
    private Sensor gyroSensor;
    private Sensor accelSensor;
    private Sensor magnetSensor;
    private Sensor rotationSensor;
    private final float[] gyroValues;
    private final float[] accelValues;
    private final float[] magnetValues;
    private final float[] rotationValues;
    private final float[] orientation;
    private final float[] rotationMatrix;
    private final float[] rotationAdjustedMatrix;
    private final float[] rotationLegacyMatrix;
    private final float[] rotationLegacyAdjustedMatrix;
    private OnSensorChangedListener sensorChangedListener;

    public static boolean havePositionSensors(Context context)
    {
        SensorManager manager = (context != null ? (SensorManager)context.getSystemService(Context.SENSOR_SERVICE) : null);
        return(manager != null && !manager.getSensorList(Sensor.TYPE_ACCELEROMETER).isEmpty() && !(manager.getSensorList(Sensor.TYPE_ROTATION_VECTOR).isEmpty() && manager.getSensorList(Sensor.TYPE_MAGNETIC_FIELD).isEmpty()));
    }

    @SuppressWarnings("SuspiciousNameCombination")
    public SensorUpdate(Context context, int orientationVal)
    {
        usingRotation = false;
        gyroCount = 0;
        gyroTime = SystemClock.elapsedRealtime();
        gyroDelaySeconds = 1.0f / 15;
        currentOrientation = orientationVal;
        filterAlpha = 0.25f;
        latchedAzDeg = 0;

        switch(orientationVal)
        {
            case Surface.ROTATION_90:
                axisX = SensorManager.AXIS_Y;
                axisY = SensorManager.AXIS_Z;
                break;

            case Surface.ROTATION_180:
                axisX = SensorManager.AXIS_MINUS_X;
                axisY = SensorManager.AXIS_MINUS_Y;
                break;

            case Surface.ROTATION_270:
                axisX = SensorManager.AXIS_MINUS_Y;
                axisY = SensorManager.AXIS_MINUS_Z;
                break;

            default:
            case Surface.ROTATION_0:
                axisX = SensorManager.AXIS_X;
                axisY = SensorManager.AXIS_Y;
                break;
        }

        sensorsManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
        if(sensorsManager != null)
        {
            rotationSensor = sensorsManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            usingRotation = (rotationSensor != null);

            magnetSensor = (usingRotation ? null : sensorsManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));
            accelSensor = sensorsManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            gyroSensor = (!sensorsManager.getSensorList(Sensor.TYPE_GYROSCOPE).isEmpty() ? sensorsManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) : null);
        }

        gyroValues = new float[]{0, 0 ,0};
        accelValues = new float[]{0, 0, 0};
        magnetValues = new float[]{0, 0, 0};
        rotationValues = new float[]{0, 0, 0, 0};
        orientation = new float[3];
        rotationMatrix = new float[16];
        rotationAdjustedMatrix = new float[16];
        rotationLegacyMatrix = new float[9];
        rotationLegacyAdjustedMatrix = new float[9];

        needMagnet = needAccel = true;
        needGyro = (gyroSensor != null);
    }

    public void startUpdates(OnSensorChangedListener listener)
    {
        if(usingRotation)
        {
            sensorsManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_GAME);
        }
        if(magnetSensor != null)
        {
            sensorsManager.registerListener(this, magnetSensor, SensorManager.SENSOR_DELAY_GAME);
        }
        if(accelSensor != null)
        {
            sensorsManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_GAME);
        }
        if(gyroSensor != null)
        {
            sensorsManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_GAME);
        }

        sensorChangedListener = listener;
    }

    public void stopUpdates()
    {
        if(usingRotation)
        {
            sensorsManager.unregisterListener(this, rotationSensor);
        }
        if(magnetSensor != null)
        {
            sensorsManager.unregisterListener(this, magnetSensor);
        }
        if(accelSensor != null)
        {
            sensorsManager.unregisterListener(this, accelSensor);
        }
        if(gyroSensor != null)
        {
            sensorsManager.unregisterListener(this, gyroSensor);
        }

        sensorChangedListener = null;
    }

    private void applyLowPassFilter(float[] input, float[] result)
    {
        int index;

        for(index = 0; index < input.length; index++)
        {
            result[index] = result[index] + filterAlpha * (input[index] - result[index]);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event)
    {
        boolean isGyro = false;
        boolean invertGyro = false;
        boolean haveOrientation = false;
        boolean aboveHorizon;
        int azGyroIndex;
        float azDeg;
        float elDeg;
        float roughElDeg;
        long currentTime;
        Sensor currentSensor = event.sensor;
        float[] currentValues = event.values;

        //if rotation
        if(currentSensor.equals(rotationSensor))
        {
            System.arraycopy(currentValues, 0, rotationValues, 0, rotationValues.length);
        }
        //else if magnetometer
        else if(currentSensor.equals(magnetSensor))
        {
            //copy values and update
            applyLowPassFilter(currentValues, magnetValues);
            needMagnet = false;
        }
        //else if accelerometer
        else if(currentSensor.equals(accelSensor))
        {
            //copy values and update
            applyLowPassFilter(currentValues, accelValues);
            needAccel = false;
        }
        //else if gyro
        else if(currentSensor.equals(gyroSensor))
        {
            //copy values and update
            applyLowPassFilter(currentValues, gyroValues);
            needGyro = false;
            isGyro = true;
            gyroCount++;

            //if 1 second has passed
            currentTime = SystemClock.elapsedRealtime();
            if(currentTime - gyroTime >= 1000)
            {
                //get average delay in ms between reads
                gyroDelaySeconds = 1.0f / gyroCount;

                //reset
                gyroCount = 0;
                gyroTime = currentTime;
            }
        }

        //if using rotation
        if(usingRotation)
        {
            //get values
            SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationValues);
            SensorManager.remapCoordinateSystem(rotationMatrix, axisX, axisY, rotationAdjustedMatrix);
            SensorManager.getOrientation(rotationAdjustedMatrix, orientation);

            //update status
            haveOrientation = true;
        }
        //else if have all needed values
        else if(!needMagnet && !needAccel)
        {
            //get values
            SensorManager.getRotationMatrix(rotationLegacyMatrix, null, accelValues, magnetValues);
            SensorManager.remapCoordinateSystem(rotationLegacyMatrix, axisX, axisY, rotationLegacyAdjustedMatrix);
            SensorManager.getOrientation(rotationLegacyAdjustedMatrix, orientation);

            //update status
            haveOrientation = true;
        }

        //if have all needed values
        if(haveOrientation && !needAccel)
        {
            //get rough elevation
            roughElDeg = Math.round(Math.toDegrees(Math.acos(accelValues[2] / 10.0))) - 90;
            aboveHorizon = (roughElDeg >= 0);

            if(aboveHorizon)
            {
                if(currentOrientation != Surface.ROTATION_90 && currentOrientation != Surface.ROTATION_270)
                {
                    orientation[1] = (float)(-Math.PI - orientation[1]);
                }
                orientation[0] += (float)Math.PI;
            }

            //convert values to degrees
            elDeg = ((float)Math.toDegrees(orientation[1]) * -1) - 90;
            if(aboveHorizon && elDeg < 0)
            {
                elDeg *= -1;
            }
            azDeg = (float)Math.toDegrees(orientation[0]);

            //normalize values
            elDeg = (float)Globals.normalizeAngle(elDeg);
            azDeg = (float)Globals.normalizePositiveAngle(azDeg);

            //if not waiting on gyro and within elevation crossover area
            if(!needGyro && elDeg >= -10 && elDeg <= 10)
            {
                //if just read gyro
                if(isGyro)
                {
                    //get gyro status
                    switch(currentOrientation)
                    {
                        case Surface.ROTATION_0:
                            //use y axis
                            azGyroIndex = 1;
                            break;

                        case Surface.ROTATION_270:
                            invertGyro = true;
                            //fall through

                        default:
                        case Surface.ROTATION_90:
                            //use x axis
                            azGyroIndex = 0;
                            break;
                    }

                    //get azimuth with gyro
                    azDeg = (float)Globals.normalizePositiveAngle(latchedAzDeg - Math.toDegrees(gyroValues[azGyroIndex] * gyroDelaySeconds * (invertGyro ? -1 : 1)));
                }
                else
                {
                    //get azimuth from latched
                    azDeg = latchedAzDeg;
                }
            }

            //update latched
            latchedAzDeg = azDeg;

            //if listener exists
            if(sensorChangedListener != null)
            {
                //send event
                sensorChangedListener.onSensorChanged(azDeg, elDeg, currentSensor.getType(), event.accuracy);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }
}
