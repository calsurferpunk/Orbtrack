package com.nikolaiapps.orbtrack;


import android.content.Context;
import android.content.IntentFilter;


public class WidgetPassTinyProvider extends WidgetPassBaseProvider
{
    private static boolean firstUpdate = true;
    private AlarmReceiver alarm;

    //Receiver to get alarm updates
    public static class AlarmReceiver extends WidgetPassBaseProvider.AlarmBaseReceiver
    {
        @Override
        public Class<?> getWidgetClass()
        {
            return(WidgetPassTinyProvider.class);
        }
    }

    //Configuration activity
    public static class SetupActivity extends WidgetBaseSetupActivity
    {
        @Override
        public Class<?> getWidgetClass()
        {
            return(WidgetPassTinyProvider.class);
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
        if(use)
        {
            alarm = new AlarmReceiver();
            context.registerReceiver(this, new IntentFilter(ACTION_SETTINGS_CLICK));
            context.registerReceiver(alarm, new IntentFilter(ACTION_UPDATE_PASS_ALARM));
            context.registerReceiver(alarm, new IntentFilter(ACTION_UPDATE_WIDGETS_ALARM));
            context.registerReceiver(alarm, new IntentFilter(ACTION_UPDATE_LOCATION_ALARM));
        }
        else if(alarm != null)
        {
            context.unregisterReceiver(alarm);
        }
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
