package com.nikolaiapps.orbtrack;


import android.content.Context;


public class WidgetPassMediumProvider extends WidgetPassBaseProvider
{
    private static boolean firstUpdate = true;

    //Receiver to get alarm updates
    public static class AlarmReceiver extends WidgetPassBaseProvider.AlarmBaseReceiver
    {
        @Override
        public Class<?> getWidgetClass()
        {
            return(WidgetPassMediumProvider.class);
        }
    }

    //Configuration activity
    public static class SetupActivity extends WidgetBaseSetupActivity
    {
        @Override
        public Class<?> getWidgetClass()
        {
            return(WidgetPassMediumProvider.class);
        }

        @Override
        public Class<?> getAlarmReceiverClass()
        {
            return(AlarmReceiver.class);
        }
    }

    @Override
    public void setActionReceivers(Context context, boolean use)
    {
        setActionReceivers(context, (use ? new AlarmReceiver() : null), use);
    }

    @Override
    public Class<?> getSetupClass()
    {
        return(SetupActivity.class);
    }

    @Override
    public Class<?> getAlarmReceiverClass()
    {
        return(AlarmReceiver.class);
    }

    @Override
    public boolean isFirstUpdate()
    {
        return(firstUpdate);
    }

    @Override
    public void clearFirstUpdate()
    {
        firstUpdate = false;
    }
}
