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
    private final SensorManager sensorsManager;
    private Sensor magnetSensor;
    private Sensor accelSensor;
    private Sensor gyroSensor;
    private float[] magnetValues;
    private float[] accelValues;
    private float[] gyroValues;
    private final float[] rotation;
    private final float[] rotation2;
    private final float[] orientation;
    private OnSensorChangedListener sensorChangedListener;

    public static boolean havePositionSensors(Context context)
    {
        SensorManager manager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
        return(manager != null && !manager.getSensorList(Sensor.TYPE_ACCELEROMETER).isEmpty() && !manager.getSensorList(Sensor.TYPE_MAGNETIC_FIELD).isEmpty());
    }

    @SuppressWarnings("SuspiciousNameCombination")
    public SensorUpdate(Context context, int orientationVal)
    {
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
            magnetSensor = sensorsManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            accelSensor = sensorsManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            gyroSensor = (!sensorsManager.getSensorList(Sensor.TYPE_GYROSCOPE).isEmpty() ? sensorsManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) : null);
        }

        magnetValues = new float[]{0, 0, 0};
        accelValues = new float[]{0, 0, 0};
        gyroValues = new float[]{0, 0 ,0};
        rotation = new float[9];
        rotation2 = new float[9];
        orientation = new float[3];

        needMagnet = needAccel = true;
        needGyro = (gyroSensor != null);
    }

    public void startUpdates(OnSensorChangedListener listener)
    {
        sensorsManager.registerListener(this, magnetSensor, SensorManager.SENSOR_DELAY_GAME);
        sensorsManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_GAME);
        if(gyroSensor != null)
        {
            sensorsManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_GAME);
        }
        sensorChangedListener = listener;
    }

    public void stopUpdates()
    {
        sensorsManager.unregisterListener(this, magnetSensor);
        sensorsManager.unregisterListener(this, accelSensor);
        if(gyroSensor != null)
        {
            sensorsManager.unregisterListener(this, gyroSensor);
        }
        sensorChangedListener = null;
    }

    private float[] applyLowPassFilter(float[] input, float[] result)
    {
        int index;

        if(result == null)
        {
            return(input);
        }

        for(index = 0; index < input.length; index++)
        {
            result[index] = result[index] + filterAlpha * (input[index] - result[index]);
        }

        return(result);
    }

    /*private void saveLog(String text)
    {
        File log = new File("sdcard/temp.txt");
        if(!log.exists())
        {
            try
            {
                log.createNewFile();
            }
            catch(Exception ex)
            {

            }
        }
        try
        {
            BufferedWriter writer = new BufferedWriter(new FileWriter(log, true));
            writer.append(text);
            writer.newLine();
            writer.close();
        }
        catch(Exception ex)
        {

        }
    }*/

    @Override
    public void onSensorChanged(SensorEvent event)
    {
        boolean isGyro = false;
        boolean invertGyro = false;
        int azGyroIndex = 0;
        float azDeg;
        float elDeg;
        long currentTime;

        //if magnetometer
        if(event.sensor.equals(magnetSensor))
        {
            //copy values and update
            magnetValues = applyLowPassFilter(event.values, magnetValues);
            needMagnet = false;
        }
        //else if accelerometer
        else if(event.sensor.equals(accelSensor))
        {
            //copy values and update
            accelValues = applyLowPassFilter(event.values, accelValues);
            needAccel = false;
        }
        //else if gyro
        else if(event.sensor.equals(gyroSensor))
        {
            //copy values and update
            gyroValues = event.values;
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

        //if have all values needed
        if(!needMagnet && !needAccel)
        {
            //get values
            SensorManager.getRotationMatrix(rotation, null, accelValues, magnetValues);
            SensorManager.remapCoordinateSystem(rotation, axisX, axisY, rotation2);
            SensorManager.getOrientation(rotation2, orientation);
            float roughElDeg = Math.round(Math.toDegrees(Math.acos(accelValues[2] / 10.0))) - 90;
            boolean aboveHorizon = (roughElDeg >= 0);

            if(aboveHorizon)
            {
                if(currentOrientation != Surface.ROTATION_90 && currentOrientation != Surface.ROTATION_270)
                {
                    orientation[1] = (float)(-Math.PI - orientation[1]);
                }
                orientation[0] += (float)Math.PI;
            }

            switch(currentOrientation)
            {
                case Surface.ROTATION_0:
                    //use y axis
                    azGyroIndex = 1;
                    break;

                case Surface.ROTATION_270:
                    invertGyro = true;
                    //fall through

                case Surface.ROTATION_90:
                    //use x axis
                    azGyroIndex = 0;
                    break;
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
                sensorChangedListener.onSensorChanged(azDeg, elDeg, event.sensor.getType(), event.accuracy);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }
}
