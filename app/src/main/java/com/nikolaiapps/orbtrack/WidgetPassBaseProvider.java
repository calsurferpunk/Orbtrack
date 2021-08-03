package com.nikolaiapps.orbtrack;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import androidx.appcompat.app.AppCompatDelegate;
import android.text.Html;
import android.view.View;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;


public abstract class WidgetPassBaseProvider extends AppWidgetProvider
{
    static
    {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    private static int dpWidth = Globals.getDeviceDp(null);
    private static final Class<?>[] widgetClasses = new Class[]{WidgetPassTinyProvider.class, WidgetPassSmallProvider.class, WidgetPassMediumProvider.class};
    private static final Class<?>[] alarmReceiverClasses = new Class[]{WidgetPassTinyProvider.AlarmReceiver.class, WidgetPassSmallProvider.AlarmReceiver.class, WidgetPassMediumProvider.AlarmReceiver.class};

    protected static final String ACTION_SETTINGS_CLICK = "com.nikolaiapps.orbtrack.WidgetPassBaseProvider.ACTION_SETTINGS_CLICK";
    protected static final String ACTION_UPDATE_PASS_ALARM = "com.nikolaiapps.orbtrack.WidgetPassBaseProvider.ACTION_UPDATE_PASS_ALARM";
    protected static final String ACTION_UPDATE_WIDGETS_ALARM = "com.nikolaiapps.orbtrack.WidgetPassBaseProvider.ACTION_UPDATE_WIDGETS_ALARM";
    protected static final String ACTION_UPDATE_LOCATION_ALARM = "com.nikolaiapps.orbtrack.WidgetPassBaseProvider.ACTION_UPDATE_LOCATION_ALARM";
    private static final String EXTRA_WIDGET_CLASS = "extraWidgetClass";
    private static final byte FLAG_WIDGET_FOLLOW = 0x01;
    private static final byte FLAG_WIDGET_INTERVAL = 0x02;
    private static final byte FLAG_WIDGET_ANY = 0x04;

    private static abstract class PassAlarmType
    {
        static final byte Start = 0;
        static final byte End = 1;
    }

    private static abstract class ParamTypes
    {
        static final String PassAlarmType = "passAlarmType";
    }

    //Widget data class
    public static class WidgetData
    {
        public final int widgetId;
        public final Class<?> widgetClass;
        public final Class<?> alarmReceiverClass;

        public WidgetData(int widgetId, Class<?> widgetClass, Class<?> alarmReceiverClass)
        {
            this.widgetId = widgetId;
            this.widgetClass = widgetClass;
            this.alarmReceiverClass = alarmReceiverClass;
        }
    }

    //Receiver to get alarm updates
    public static abstract class AlarmBaseReceiver extends BroadcastReceiver
    {
        public abstract Class<?> getWidgetClass();

        @Override
        public void onReceive(Context context, Intent intent)
        {
            int widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            //byte passAlarmType = intent.getByteExtra(ParamTypes.PassAlarmType, PassAlarmType.None);
            String action = intent.getAction();
            Class<?> alarmReceiverClass = getClass();
            Class<?> widgetClass = getWidgetClass();
            ArrayList<Integer> ids;

            //if action is set
            if(action != null)
            {
                //handle based on action
                switch(action)
                {
                    case Intent.ACTION_BOOT_COMPLETED:
                        //if any widgets are using location intervals
                        if(getWidgetIdList(context, widgetClass, null, FLAG_WIDGET_INTERVAL).size() > 0)
                        {
                            //set alarm
                            WidgetPassBaseProvider.updateLocationIntervalAlarm(context, alarmReceiverClass, WidgetBaseSetupActivity.getLocationGlobalInterval(context), true);
                        }

                        //set widgets update
                        setUpdateWidgetsAlarm(context, alarmReceiverClass);
                        break;

                    case ACTION_UPDATE_LOCATION_ALARM:
                        //update location
                        LocationService.getCurrentLocation(context, true, false, LocationService.PowerTypes.HighPowerThenBalanced);
                        break;

                    case ACTION_UPDATE_PASS_ALARM:
                        //if a valid widget ID and the update alarm
                        if(widgetId != AppWidgetManager.INVALID_APPWIDGET_ID)
                        {
                            //update pass
                            updateWidget(context, widgetClass, alarmReceiverClass, widgetId, AppWidgetManager.getInstance(context), getViews(context, widgetClass, widgetId), true);
                        }
                        break;

                    case ACTION_UPDATE_WIDGETS_ALARM:
                        //get IDs
                        ids = getWidgetIdList(context, widgetClass);

                        //update all widgets
                        for(int currentId : ids)
                        {
                            updateWidget(context, widgetClass, alarmReceiverClass, currentId, AppWidgetManager.getInstance(context), getViews(context, widgetClass, currentId), true);
                        }
                        break;
                }
            }
        }
    }

    private static LocationReceiver locationReceiver;

    //needs to be implemented
    public abstract void setActionReceivers(Context context, boolean use);
    public abstract boolean isFirstUpdate();
    public abstract void clearFirstUpdate();
    public abstract Class<?> getSetupClass();
    public abstract Class<?> getAlarmReceiverClass();

    @Override
    public void onEnabled(Context context)
    {
        Class<?> widgetClass = getClass();
        int[] widgetIds = getWidgetIds(context, widgetClass);

        //set action receivers
        setActionReceivers(context.getApplicationContext(), true);

        //update all widgets
        onUpdate(context, AppWidgetManager.getInstance(context), widgetIds);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] widgetIds)
    {
        int noradId;
        Class<?> widgetClass = getClass();
        Class<?> alarmReceiverClass = getAlarmReceiverClass();

        //update display DPI
        dpWidth = Globals.getDeviceDp(context);

        //if IDs are set
        if(widgetIds != null)
        {
            //go through each widget ID
            for(int widgetId : widgetIds)
            {
                //if a valid ID
                if(widgetId != AppWidgetManager.INVALID_APPWIDGET_ID)
                {
                    //update widget
                    noradId = WidgetBaseSetupActivity.getNoradID(context, widgetId);
                    updateWidget(context, widgetClass, alarmReceiverClass, widgetId, manager, getViews(context, widgetClass, widgetId), (isFirstUpdate() && noradId != Universe.IDs.Invalid));
                }
            }

            //done
            clearFirstUpdate();
        }
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        boolean hadFollow = false;
        boolean hadInterval = false;
        int widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        int[] widgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
        Class<?> widgetClass = getClass();
        Class<?> alarmReceiverClass = getAlarmReceiverClass();
        Class<?> setupClass = getSetupClass();
        String action = intent.getAction();
        Class<?> intentWidgetClass = (Class<?>)intent.getSerializableExtra(EXTRA_WIDGET_CLASS);
        ArrayList<Integer> excludeIds;

        //if intent widget class is known and not for this widget class
        if(intentWidgetClass != null && !intentWidgetClass.equals(widgetClass))
        {
            //stop
            return;
        }

        //if widget IDs are not set but widget ID is
        if((widgetIds == null || widgetIds.length == 0) && widgetId != AppWidgetManager.INVALID_APPWIDGET_ID)
        {
            //use widget ID instead
            widgetIds = new int[]{widgetId};
        }

        //if widget IDs and action are set
        if(widgetIds != null && action != null)
        {
            //go through each widget ID
            for(int currentId : widgetIds)
            {
                //if a valid ID
                if(currentId != AppWidgetManager.INVALID_APPWIDGET_ID)
                {
                    switch(action)
                    {
                        //show settings
                        case ACTION_SETTINGS_CLICK:
                            Intent setupIntent = new Intent(context, setupClass);
                            setupIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, currentId);
                            setupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(setupIntent);
                            break;

                        case AppWidgetManager.ACTION_APPWIDGET_DELETED:
                            //if using current location
                            if(WidgetBaseSetupActivity.getLocationSource(context, widgetId) == Database.LocationType.Current)
                            {
                                //if following
                                if(WidgetBaseSetupActivity.getLocationFollow(context, widgetId))
                                {
                                    //was following
                                    hadFollow = true;
                                }
                                //else if on interval
                                else if(WidgetBaseSetupActivity.getLocationInterval(context, widgetId))
                                {
                                    //was on interval
                                    hadInterval = true;
                                }
                            }
                            break;
                    }
                }
            }
        }

        //call base
        super.onReceive(context, intent);

        //if widgets are set, just deleted widget(s), and had following or on an interval
        if(widgetIds != null && action != null && action.equals(AppWidgetManager.ACTION_APPWIDGET_DELETED) && (hadFollow || hadInterval))
        {
            //get deleted widgets to exclude
            excludeIds = Globals.getList(widgetIds);

            //if had following but don't anymore
            if(hadFollow && getWidgetIdList(context, widgetClass, excludeIds, FLAG_WIDGET_FOLLOW).size() == 0)
            {
                //restart without being in foreground
                LocationService.restart(context, false, false);
            }

            //if had intervals but don't anymore
            if(hadInterval && getWidgetIdList(context, widgetClass, excludeIds, FLAG_WIDGET_INTERVAL).size() == 0)
            {
                //stop interval updates
                WidgetPassBaseProvider.updateLocationIntervalAlarm(context, alarmReceiverClass, 0, false);
            }
        }
    }

    @Override
    public void onDeleted(Context context, int[] widgetIds)
    {
        Class<?> alarmReceiverClass = getAlarmReceiverClass();

        //go through each widget ID
        for(int widgetId : widgetIds)
        {
            //if a valid ID
            if(widgetId != AppWidgetManager.INVALID_APPWIDGET_ID)
            {
                //remove any alarm and settings
                updatePassAlarm(context, alarmReceiverClass, widgetId, null, null, false);
                WidgetBaseSetupActivity.removeSettings(context, widgetId);
            }
        }
    }

    @Override
    public void onDisabled(Context context)
    {
        //remove action receivers
        setActionReceivers(context.getApplicationContext(), false);
        createLocationReceiver(context, false, false);
    }

    //Gets satellite for given widget ID
    private static Database.SatelliteData getSatellite(int widgetId, Context context)
    {
        //handle any updates
        Database.handleUpdates(context);

        //get satellite
        return(new Database.SatelliteData(context, WidgetBaseSetupActivity.getNoradID(context, widgetId)));
    }

    //Gets an intent for a pass alarm
    private static PendingIntent getPassAlarmIntent(Context context, Class<?> alarmReceiverClass, int widgetId, byte passAlarmType)
    {
        Intent alarmIntent = new Intent(context, alarmReceiverClass);
        PendingIntent alarmPendingIntent;

        //create and return intent
        alarmIntent.setAction(ACTION_UPDATE_PASS_ALARM);
        alarmIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        alarmIntent.putExtra(ParamTypes.PassAlarmType, passAlarmType);
        alarmPendingIntent = Globals.getPendingBroadcastIntent(context, (widgetId * 10) + passAlarmType, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        return(alarmPendingIntent);
    }

    //Gets an intent for an update widgets alarm
    private static PendingIntent getUpdateWidgetsIntent(Context context, Class<?> alarmReceiverClass)
    {
        Intent alarmIntent = new Intent(context, alarmReceiverClass);
        PendingIntent alaramPendingIntent;

        //create and return intent
        alarmIntent.setAction(ACTION_UPDATE_WIDGETS_ALARM);
        alaramPendingIntent = Globals.getPendingBroadcastIntent(context, 9999, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        return(alaramPendingIntent);
    }

    //Gets an intent for the location interval updates alarm
    private static PendingIntent getLocationIntervalIntent(Context context, Class<?> alarmReceiverClass)
    {
        Intent alarmIntent = new Intent(context, alarmReceiverClass);
        PendingIntent alarmPendingIntent;

        //create and return intent
        alarmIntent.setAction(ACTION_UPDATE_LOCATION_ALARM);
        alarmPendingIntent = Globals.getPendingBroadcastIntent(context, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        return(alarmPendingIntent);
    }

    //Gets an intent with action settings click
    private static PendingIntent getActionSettingsClickIntent(Context context, Class<?> widgetClass, int widgetId)
    {
        Intent intent = new Intent(context, widgetClass);
        intent.setAction(ACTION_SETTINGS_CLICK);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        intent.putExtra(EXTRA_WIDGET_CLASS, widgetClass);
        return(Globals.getPendingBroadcastIntent(context, widgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT));
    }

    //Sets border
    private static void setBorder(Context context, int widgetId, RemoteViews views, View parent, int viewId)
    {
        int alpha;
        int color;
        int argbColor = Color.BLACK;
        int borderType = WidgetBaseSetupActivity.BorderType.Round;
        int resourceId;
        boolean useParent = (parent != null);
        boolean useGlobalBackground = WidgetBaseSetupActivity.getGlobalBackground(context, widgetId);

        //if using global background color
        if(useGlobalBackground)
        {
            //get global background border and color
            borderType = WidgetBaseSetupActivity.getBorderType(context, widgetId);
            argbColor = WidgetBaseSetupActivity.getGlobalBackgroundColor(context, widgetId);
        }
        //else if widget pass border
        else if(viewId == R.id.Widget_Pass_Border)
        {
            //get pass border and color
            borderType = WidgetBaseSetupActivity.getBorderType(context, widgetId);
            argbColor = WidgetBaseSetupActivity.getBorderColor(context, widgetId);
        }

        //get border color and opacity
        color = Color.rgb(Color.red(argbColor), Color.green(argbColor), Color.blue(argbColor));
        alpha = Color.alpha(argbColor);

        //setup view
        resourceId = (borderType == WidgetBaseSetupActivity.BorderType.Round ? R.drawable.border_round : R.drawable.border_square);
        if(useParent)
        {
            ImageView currentView = parent.findViewById(viewId);
            if(currentView != null)
            {
                //set border type
                currentView.setImageResource(resourceId);

                //set border color and opacity
                currentView.setColorFilter(color);
                currentView.setImageAlpha(alpha);
            }
        }
        else
        {
            //set border type
            views.setImageViewResource(viewId, resourceId);

            //set border color and opacity
            views.setInt(viewId, "setColorFilter", color);
            views.setInt(viewId, "setImageAlpha", alpha);
        }
    }

    //Sets background
    private static void setViewBackground(Context context, int widgetId, RemoteViews views, View parent, int viewId)
    {
        int color = Color.BLACK;
        boolean useParent = (parent != null);
        boolean useGlobalBackground = WidgetBaseSetupActivity.getGlobalBackground(context, widgetId);

        //if using global background color
        if(useGlobalBackground)
        {
            //get global background color
            color = WidgetBaseSetupActivity.getGlobalBackgroundColor(context, widgetId);
        }
        //else if pass top layout
        else if(viewId == R.id.Widget_Pass_Top_Layout)
        {
            //get top color
            color = WidgetBaseSetupActivity.getTopBackgroundColor(context, widgetId);
        }
        //else if pass middle layout
        else if(viewId == R.id.Widget_Pass_Tiny_Middle_Layout || viewId == R.id.Widget_Pass_Middle_Layout)
        {
            //get middle color
            color = WidgetBaseSetupActivity.getMiddleBackgroundColor(context, widgetId);
        }
        //else if pass bottom layout
        else if(viewId == R.id.Widget_Pass_Bottom_Layout || viewId == R.id.Widget_Pass_Outdated_Text)
        {
            //get bottom color
            color = WidgetBaseSetupActivity.getBottomBackgroundColor(context, widgetId);
        }

        //set background color
        if(useParent)
        {
            View currentView = parent.findViewById(viewId);
            if(currentView != null)
            {
                currentView.setBackgroundColor(color);
            }
        }
        else
        {
            views.setInt(viewId, "setBackgroundColor", color);
        }
    }

    //Sets text
    private static void setViewText(Context context, Class<?> widgetClass, int widgetId, RemoteViews views, View parent, int viewId, String text)
    {
        boolean isBold;
        boolean isItalic;
        boolean useParent = (parent != null);
        boolean useGlobalText = WidgetBaseSetupActivity.getGlobalText(context, widgetId);
        float size = WidgetBaseSetupActivity.getDefaultTextSize(widgetClass, dpWidth);
        int color = Color.WHITE;
        int weight = Typeface.NORMAL;
        String setText = text;

        //if using global text color
        if(useGlobalText)
        {
            //get global text color and weight
            size = WidgetBaseSetupActivity.getGlobalTextSize(context, widgetClass, widgetId);
            color = WidgetBaseSetupActivity.getGlobalTextColor(context, widgetId);
            weight = WidgetBaseSetupActivity.getGlobalTextWeight(context, widgetId);
        }
        else
        {
            //handle based on view
            if(viewId == R.id.Widget_Pass_Name_Text)
            {
                size = WidgetBaseSetupActivity.getOrbitalTextSize(context, widgetClass, widgetId);
                color = WidgetBaseSetupActivity.getOrbitalTextColor(context, widgetId);
                weight = WidgetBaseSetupActivity.getOrbitalTextWeight(context, widgetId);
            }
            else if(viewId == R.id.Widget_Pass_Start_Text || viewId == R.id.Widget_Pass_Start_Direction_Text || viewId == R.id.Widget_Pass_Start_Time_Text)
            {
                size = WidgetBaseSetupActivity.getPassStartTextSize(context, widgetClass, widgetId);
                color = WidgetBaseSetupActivity.getPassStartTextColor(context, widgetId);
                weight = WidgetBaseSetupActivity.getPassStartTextWeight(context, widgetId);
            }
            else if(viewId == R.id.Widget_Pass_End_Text)
            {
                size = WidgetBaseSetupActivity.getPassEndTextSize(context, widgetClass, widgetId);
                color = WidgetBaseSetupActivity.getPassEndTextColor(context, widgetId);
                weight = WidgetBaseSetupActivity.getPassEndTextWeight(context, widgetId);
            }
            else if(viewId == R.id.Widget_Pass_El_Max_Text)
            {
                size = WidgetBaseSetupActivity.getPassElevationTextSize(context, widgetClass, widgetId);
                color = WidgetBaseSetupActivity.getPassElevationTextColor(context, widgetId);
                weight = WidgetBaseSetupActivity.getPassElevationTextWeight(context, widgetId);
            }
            else if(viewId == R.id.Widget_Pass_Az_Start_Text)
            {
                size = WidgetBaseSetupActivity.getPassAzimuthStartTextSize(context, widgetClass, widgetId);
                color = WidgetBaseSetupActivity.getPassAzimuthStartTextColor(context, widgetId);
                weight = WidgetBaseSetupActivity.getPassAzimuthStartTextWeight(context, widgetId);
            }
            else if(viewId == R.id.Widget_Pass_Az_End_Text)
            {
                size = WidgetBaseSetupActivity.getPassAzimuthEndTextSize(context, widgetClass, widgetId);
                color = WidgetBaseSetupActivity.getPassAzimuthEndTextColor(context, widgetId);
                weight = WidgetBaseSetupActivity.getPassAzimuthEndTextWeight(context, widgetId);
            }
            else if(viewId == R.id.Widget_Pass_Duration_Text)
            {
                size = WidgetBaseSetupActivity.getPassDurationTextSize(context, widgetClass, widgetId);
                color = WidgetBaseSetupActivity.getPassDurationTextColor(context, widgetId);
                weight = WidgetBaseSetupActivity.getPassDurationTextWeight(context, widgetId);
            }
            else if(viewId == R.id.Widget_Pass_Location_Text || viewId == R.id.Widget_Pass_Outdated_Text)
            {
                size = WidgetBaseSetupActivity.getLocationTextSize(context, widgetClass, widgetId);
                color = WidgetBaseSetupActivity.getLocationTextColor(context, widgetId);
                weight = WidgetBaseSetupActivity.getLocationTextWeight(context, widgetId);
            }
        }

        //update status
        isBold = WidgetBaseSetupActivity.isBold(weight);
        isItalic = WidgetBaseSetupActivity.isItalic(weight);

        //set color and text
        if(setText != null)
        {
            //update text
            if(isBold)
            {
                setText = "<b>" + setText + "</b>";
            }
            if(isItalic)
            {
                setText = "<i>" + setText + "</i>";
            }
        }
        if(useParent)
        {
            TextView currentView = parent.findViewById(viewId);
            if(currentView != null)
            {
                //set color
                currentView.setTextColor(color);

                if(setText != null)
                {
                    //update size and display
                    currentView.setTextSize(size);
                    currentView.setText(Html.fromHtml(setText));
                }
            }
        }
        else
        {
            //set color
            views.setTextColor(viewId, color);

            if(setText != null)
            {
                //update size and display
                views.setFloat(viewId, "setTextSize", size);
                views.setTextViewText(viewId, Html.fromHtml(setText));
            }
        }
    }

    //Sets bitmap
    private static void setImageViewBitmap(RemoteViews views, View parent, int viewId, Bitmap image, int imageId)
    {
        boolean useImage = (image != null);

        if(parent != null)
        {
            ImageView currentView = parent.findViewById(viewId);
            if(currentView != null)
            {
                if(useImage)
                {
                    currentView.setImageBitmap(image);
                }
                else if(imageId != Integer.MAX_VALUE)
                {
                    currentView.setImageResource(imageId);
                }
            }
        }
        else
        {
            if(useImage)
            {
                views.setImageViewBitmap(viewId, image);
            }
            else if(imageId != Integer.MAX_VALUE)
            {
                views.setImageViewResource(viewId, imageId);
            }
        }
    }
    private static void setImageViewBitmap(RemoteViews views, View parent, int viewId, Bitmap image)
    {
        setImageViewBitmap(views, parent, viewId, image, Integer.MAX_VALUE);
    }
    private static void setImageViewResource(RemoteViews views, View parent, int viewId, int imageId)
    {
        setImageViewBitmap(views, parent, viewId, null, imageId);
    }

    //Sets visibility
    private static void setViewVisibility(RemoteViews views, View parent, int viewId, boolean visible)
    {
        int visibility = (visible ? View.VISIBLE : View.GONE);

        if(parent != null)
        {
            View currentView = parent.findViewById(viewId);
            if(currentView != null)
            {
                currentView.setVisibility(visibility);
            }
        }
        else
        {
            views.setViewVisibility(viewId, visibility);
        }
    }

    //Gets widget views
    private static RemoteViews getViews(Context context, Class<?> widgetClass, int widgetId, long midPassTimeMs, View parent)
    {
        int iconId;
        int itemImageId;
        int noradId = WidgetBaseSetupActivity.getNoradID(context, widgetId);
        int globalImageColor = WidgetBaseSetupActivity.getGlobalImageColor(context, widgetId);
        int orbitalImageColor = WidgetBaseSetupActivity.getOrbitalImageColor(context, widgetId);
        int settingsImageColor = WidgetBaseSetupActivity.getSettingsImageColor(context, widgetId);
        int locationImageColor = WidgetBaseSetupActivity.getLocationImageColor(context, widgetId);
        byte orbitalType = WidgetBaseSetupActivity.getOrbitalType(context, widgetId);
        Database.SatelliteData currentSatelliteData = getSatellite(widgetId, context);
        boolean useParent = (parent != null);
        boolean useGlobalImage = WidgetBaseSetupActivity.getGlobalImage(context, widgetId);
        boolean useNormal = !widgetClass.equals(WidgetPassTinyProvider.class);
        boolean useExtended = widgetClass.equals(WidgetPassMediumProvider.class);
        boolean tleIsAccurate = (currentSatelliteData.database == null || currentSatelliteData.database.tleIsAccurate);
        RemoteViews views = (useParent ? null : new RemoteViews(context.getPackageName(), R.layout.widget_pass_view));
        PendingIntent clickIntent = (useParent ? null : getActionSettingsClickIntent(context, widgetClass, widgetId));

        //get image ID
        itemImageId = (useNormal ? R.id.Widget_Pass_Item_Image : R.id.Widget_Pass_Item_Tiny_Image);

        //update visibility
        setViewVisibility(views, parent, R.id.Widget_Pass_Settings_Button, useNormal);
        setViewVisibility(views, parent, R.id.Widget_Pass_Item_Image, useNormal);
        setViewVisibility(views, parent, R.id.Widget_Pass_Tiny_Middle_Layout, !useNormal);
        setViewVisibility(views, parent, R.id.Widget_Pass_Middle_Layout, useNormal);
        setViewVisibility(views, parent, R.id.Widget_Pass_Normal_Layout, useNormal && tleIsAccurate);
        setViewVisibility(views, parent, R.id.Widget_Pass_Extended_Layout, useExtended && tleIsAccurate);
        setViewVisibility(views, parent, R.id.Widget_Pass_Location_Image, useNormal && tleIsAccurate);
        setViewVisibility(views, parent, R.id.Widget_Pass_Location_Text, useNormal && tleIsAccurate);
        setViewVisibility(views, parent, R.id.Widget_Pass_Tiny_Start_Layout, !useNormal && tleIsAccurate);
        setViewVisibility(views, parent, R.id.Widget_Pass_Outdated_Text, !tleIsAccurate);

        //set border, background, and name
        setBorder(context, widgetId, views, parent, R.id.Widget_Pass_Border);
        setViewBackground(context, widgetId, views, parent, R.id.Widget_Pass_Top_Layout);
        setViewBackground(context, widgetId, views, parent, R.id.Widget_Pass_Tiny_Middle_Layout);
        setViewBackground(context, widgetId, views, parent, R.id.Widget_Pass_Middle_Layout);
        setViewBackground(context, widgetId, views, parent, R.id.Widget_Pass_Bottom_Layout);
        setViewBackground(context, widgetId, views, parent, R.id.Widget_Pass_Outdated_Text);
        setViewText(context, widgetClass, widgetId, views, parent, R.id.Widget_Pass_Name_Text, WidgetBaseSetupActivity.getName(context, widgetId));

        //if the moon
        if(noradId == Universe.IDs.Moon)
        {
            //set moon icon with phase
            setImageViewBitmap(views, parent, itemImageId, Universe.Moon.getPhaseImage(context, WidgetBaseSetupActivity.getLocation(context, widgetId), midPassTimeMs));
        }
        else
        {
            //get icon
            iconId = Globals.getOrbitalIconID(noradId, orbitalType);

            //if a satellite
            if(iconId == R.drawable.orbital_satellite || iconId == R.drawable.orbital_rocket || iconId == R.drawable.orbital_debris)
            {
                //set tinted satellite icon
                setImageViewBitmap(views, parent, itemImageId, Globals.getBitmap(context, iconId, (useGlobalImage ? globalImageColor : orbitalImageColor)));
            }
            else
            {
                //set non-tinted icon
                setImageViewResource(views, parent, itemImageId, iconId);
            }
        }

        //set font colors
        if(useNormal)
        {
            setViewText(context, widgetClass, widgetId, views, parent, R.id.Widget_Pass_Start_Text, null);
            setViewText(context, widgetClass, widgetId, views, parent, R.id.Widget_Pass_End_Text, null);
            setViewText(context, widgetClass, widgetId, views, parent, R.id.Widget_Pass_El_Max_Text, null);

            setViewText(context, widgetClass, widgetId, views, parent, R.id.Widget_Pass_Location_Text, (useParent ? WidgetBaseSetupActivity.getLocationName(context, widgetId) : null));
            setImageViewBitmap(views, parent, R.id.Widget_Pass_Location_Image, Globals.getBitmap(context, R.drawable.ic_my_location_black, (useGlobalImage ? globalImageColor : locationImageColor)));
        }
        else
        {
            setViewText(context, widgetClass, widgetId, views, parent, R.id.Widget_Pass_Start_Direction_Text, null);
            setViewText(context, widgetClass, widgetId, views, parent, R.id.Widget_Pass_Start_Time_Text, null);
        }
        if(useExtended)
        {
            setViewText(context, widgetClass, widgetId, views, parent, R.id.Widget_Pass_Az_Start_Text, null);
            setViewText(context, widgetClass, widgetId, views, parent, R.id.Widget_Pass_Az_End_Text, null);
            setViewText(context, widgetClass, widgetId, views, parent, R.id.Widget_Pass_Duration_Text, null);
        }
        setViewText(context, widgetClass, widgetId, views, parent, R.id.Widget_Pass_Outdated_Text, context.getString(R.string.text_outdated));

        //set settings
        if(!useParent)
        {
            views.setOnClickPendingIntent(R.id.Widget_Pass_Name_Text, clickIntent);
            if(useNormal)
            {
                views.setOnClickPendingIntent(R.id.Widget_Pass_Settings_Button, clickIntent);
            }
        }
        setImageViewBitmap(views, parent, R.id.Widget_Pass_Settings_Button, Globals.getBitmap(context, R.drawable.ic_settings_black, (useGlobalImage ? globalImageColor : settingsImageColor)));

        //return views
        return(views);
    }
    private static RemoteViews getViews(Context context, Class<?> widgetClass, int widgetId, long midPassTimeMs)
    {
        return(getViews(context, widgetClass, widgetId, midPassTimeMs, null));
    }
    public static RemoteViews getViews(Context context, Class<?> widgetClass, int widgetId)
    {
        return(getViews(context, widgetClass, widgetId, System.currentTimeMillis(), null));
    }

    //Updates preview widget
    public static void updatePreview(Context context, Class<?> widgetClass, View parent)
    {
        getViews(context, widgetClass, Integer.MAX_VALUE, System.currentTimeMillis(), parent);
        updatePass(context, widgetClass, null, Integer.MAX_VALUE, null, null, parent, null, TimeZone.getDefault());
    }

    //Gets existing widget ID list
    private static ArrayList<Integer> getWidgetIdList(Context context, Class<?> widgetClass)
    {
        return(Globals.getList(AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, widgetClass))));
    }
    private static ArrayList<Integer> getWidgetIdList(Context context, Class<?> widgetClass, ArrayList<Integer> excludeIds, byte flags)
    {
        boolean excluding = (excludeIds != null);
        boolean needAny = (flags == 0x00 || (flags & FLAG_WIDGET_ANY) == FLAG_WIDGET_ANY);
        boolean needFollow = ((flags & FLAG_WIDGET_FOLLOW) == FLAG_WIDGET_FOLLOW);
        boolean needInterval = ((flags & FLAG_WIDGET_INTERVAL) == FLAG_WIDGET_INTERVAL);
        ArrayList<Integer> ids = new ArrayList<>(0);
        ArrayList<Integer> allIds = getWidgetIdList(context, widgetClass);

        //go through each ID
        for(int currentId : allIds)
        {
            //if -not excluding- or -not an ID in the list-
            if(!excluding || !excludeIds.contains(currentId))
            {
                //if -needs to be following or using interval- and -is using current location-
                if((needFollow || needInterval) && WidgetBaseSetupActivity.getLocationSource(context, currentId) == Database.LocationType.Current)
                {
                    //if needs to be following and is following
                    if(needFollow && WidgetBaseSetupActivity.getLocationFollow(context, currentId))
                    {
                        //add it
                        ids.add(currentId);
                    }
                    //else if needs to use interval and is using interval
                    else if(needInterval && WidgetBaseSetupActivity.getLocationInterval(context, currentId))
                    {
                        //add it
                        ids.add(currentId);
                    }
                }
                //else if can be any
                else if(needAny)
                {
                    //add it
                    ids.add(currentId);
                }
            }
        }

        //return IDs
        return(ids);
    }

    //Get existing widget IDs
    public static int[] getWidgetIds(Context context, Class<?> widgetClass)
    {
        int index;
        ArrayList<Integer> ids = getWidgetIdList(context, widgetClass);
        int[] widgetIds = new int[ids.size()];

        //go through each widget ID
        for(index = 0; index < widgetIds.length; index++)
        {
            //set ID
            widgetIds[index] = ids.get(index);
        }

        //return IDs
        return(widgetIds);
    }

    //Gets existing widget data
    public static ArrayList<WidgetData> getWidgetData(Context context)
    {
        int index;
        ArrayList<WidgetData> widgets = new ArrayList<>(0);

        //go through each widget class
        for(index = 0; index < widgetClasses.length; index++)
        {
            //remember current class
            Class<?> currentClass = widgetClasses[index];

            //go through all widget IDs for class
            int[] ids = getWidgetIds(context, currentClass);
            for(int currentId : ids)
            {
                //add widget data
                widgets.add(new WidgetData(currentId, currentClass, alarmReceiverClasses[index]));
            }
        }

        //return widgets
        return(widgets);
    }

    //Sets pass for given widget
    private static void updatePass(Context context, Class<?> widgetClass, Class<?> alarmReceiverClass, int widgetId, AppWidgetManager manager, RemoteViews views, View parent, CalculateService.PassData currentPass, TimeZone zone)
    {
        boolean sameDay;
        boolean useStart;
        boolean useNormal = !widgetClass.equals(WidgetPassTinyProvider.class);
        boolean useExtended = widgetClass.equals(WidgetPassMediumProvider.class);
        boolean useParent = (parent != null);
        Calendar displayTime;
        Calendar displayLocalTime;
        Calendar currentLocalTime;
        String unknown = Globals.getUnknownString(context);
        Resources res = context.getResources();

        //if using parent
        if(useParent)
        {
            //setup example pass
            currentPass = new CalculateService.PassData(0, 123.456, 234.567, 0, 45.123, 0.75, 1.3, false, true, true, true, true, null, null, null, null, null, null, 85.23, context.getString(R.string.title_full));
        }

        //if pass is set
        if(currentPass != null)
        {
            //if using parent
            if(useParent)
            {
                //set example pass
                currentPass.passTimeStart = Calendar.getInstance();
                currentPass.passTimeStart.set(Calendar.HOUR_OF_DAY, 14);
                currentPass.passTimeStart.set(Calendar.MINUTE, 30);
                currentPass.passTimeEnd = Calendar.getInstance();
                currentPass.passTimeEnd.setTimeInMillis(currentPass.passTimeStart.getTimeInMillis());
                currentPass.passTimeEnd.add(Calendar.MINUTE, 90);
                currentPass.passDuration = Globals.getTimeBetween(context, currentPass.passTimeStart, currentPass.passTimeEnd);
                currentPass.passAzTravel = currentPass.passAzEnd - currentPass.passAzStart;
            }

            //set pass start, end, and max elevation text
            if(useNormal)
            {
                setViewText(context, widgetClass, widgetId, views, parent, R.id.Widget_Pass_Start_Text, Globals.Symbols.Up + Globals.getDateString(context, currentPass.passTimeStart, zone, false, true, true));
                setViewText(context, widgetClass, widgetId, views, parent, R.id.Widget_Pass_End_Text, Globals.Symbols.Down + Globals.getDateString(context, currentPass.passTimeEnd, zone, false, true, true));
                setViewText(context, widgetClass, widgetId, views, parent, R.id.Widget_Pass_El_Max_Text, Globals.Symbols.Elevating + (currentPass.isKnownPassElevationMax() ? Globals.getDegreeString(currentPass.passElMax) : unknown));
            }
            else
            {
                useStart = (currentPass.passTimeStart == null || currentPass.passTimeStart.getTimeInMillis() > System.currentTimeMillis());
                displayTime = (useStart ? currentPass.passTimeStart : currentPass.passTimeEnd);
                displayLocalTime = Globals.getLocalTime(displayTime, zone);
                currentLocalTime = Globals.getLocalTime(Calendar.getInstance(), zone);
                sameDay = (displayLocalTime != null && currentLocalTime != null && displayLocalTime.get(Calendar.DAY_OF_YEAR) == currentLocalTime.get(Calendar.DAY_OF_YEAR));

                setViewText(context, widgetClass, widgetId, views, parent, R.id.Widget_Pass_Start_Direction_Text, (useStart ? Globals.Symbols.Up : Globals.Symbols.Down));
                setViewText(context, widgetClass, widgetId, views, parent, R.id.Widget_Pass_Start_Time_Text, (!sameDay ? (Globals.getLocalDayString(context, displayTime, zone) + " ") : "") + Globals.getDateString(context, displayTime, zone, false, false, false, true).replace(" ", "&nbsp;"));
            }
            if(useExtended)
            {
                //set az start, end, and pass duration text
                setViewText(context, widgetClass, widgetId, views, parent, R.id.Widget_Pass_Az_Start_Text, Globals.Symbols.Up + (currentPass.passTimeStart != null ? Globals.getAzimuthDirectionString(res, currentPass.passAzStart) : unknown));
                setViewText(context, widgetClass, widgetId, views, parent, R.id.Widget_Pass_Az_End_Text, Globals.Symbols.Down + (currentPass.passTimeEnd != null ? Globals.getAzimuthDirectionString(res, currentPass.passAzEnd) : unknown));
                setViewText(context, widgetClass, widgetId, views, parent, R.id.Widget_Pass_Duration_Text, Globals.Symbols.Time + currentPass.passDuration);
            }
        }

        if(!useParent)
        {
            updateWidget(context, widgetClass, alarmReceiverClass, widgetId, manager, views, false);
        }
    }
    private static void updatePass(Context context, Class<?> widgetClass, Class<?> alarmReceiverClass, int widgetId, AppWidgetManager manager, RemoteViews views, CalculateService.PassData currentPass, TimeZone zone)
    {
        updatePass(context, widgetClass, alarmReceiverClass, widgetId, manager, views, null, currentPass, zone);
    }

    //Updates next update pass alarm
    public static void updatePassAlarm(Context context, Class<?> alarmReceiverClass, int widgetId, Calendar passStartTime, Calendar passTimeEnd, boolean use)
    {
        long startTimeMs;
        boolean alarmSet = false;
        AlarmManager manager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

        //if manager is set
        if(manager != null)
        {
            //if using alarm
            if(use)
            {
                //if there is a pass starting time
                if(passStartTime != null)
                {
                    //get time
                    startTimeMs = passStartTime.getTimeInMillis();

                    //if before time
                    if(startTimeMs > System.currentTimeMillis())
                    {
                        //set alarm to 4 seconds after pass starting time
                        Globals.setAlarm(context, manager, startTimeMs + 4000, getPassAlarmIntent(context, alarmReceiverClass, widgetId, PassAlarmType.Start), false);
                        alarmSet = true;
                    }
                }

                //if there is a pass ending time and alarm not already set
                if(passTimeEnd != null && !alarmSet)
                {
                    //set alarm to 4 seconds after pass ending time
                    Globals.setAlarm(context, manager, passTimeEnd.getTimeInMillis() + 4000, getPassAlarmIntent(context, alarmReceiverClass, widgetId, PassAlarmType.End), false);
                }
            }
            else
            {
                //cancel alarms
                manager.cancel(getPassAlarmIntent(context, alarmReceiverClass, widgetId, PassAlarmType.Start));
                manager.cancel(getPassAlarmIntent(context, alarmReceiverClass, widgetId, PassAlarmType.End));
            }
        }
    }

    //Updates the location interval alarm
    public static void updateLocationIntervalAlarm(Context context, Class<?> alarmReceiverClass, long intervalMs, boolean use)
    {
        PendingIntent alarmPendingIntent = getLocationIntervalIntent(context, alarmReceiverClass);
        AlarmManager manager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

        //if manager is set
        if(manager != null)
        {
            //if using alarm
            if(use)
            {
                //set alarm to interval, starting 1 interval after now
                manager.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis() + intervalMs, intervalMs, alarmPendingIntent);
            }
            else
            {
                //cancel alarm
                manager.cancel(alarmPendingIntent);
            }
        }
    }

    //Sends out an update widget alarm shortly
    public static void setUpdateWidgetsAlarm(Context context, Class<?> alarmReceiverClass)
    {
        PendingIntent alarmPendingIntent = getUpdateWidgetsIntent(context, alarmReceiverClass);
        AlarmManager manager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

        //if manager is set
        if(manager != null)
        {
            //set alarm to run once
            manager.set(AlarmManager.RTC, System.currentTimeMillis() + 1500, alarmPendingIntent);
        }
    }

    //Create calculate listener
    private static CalculateService.CalculateListener createCalculateListener(final Context context, final Class<?> widgetClass, final Class<?> alarmReceiverClass, final int widgetId)
    {
        return(new CalculateService.CalculateListener()
        {
            @Override
            public void onCalculated(int progressType, final CalculateService.PassData pass)
            {
                boolean havePass = (pass != null);
                boolean useNormal = !widgetClass.equals(WidgetPassTinyProvider.class);
                long midPassMs = (havePass ? pass.getMidPass() : System.currentTimeMillis());
                final byte locationSource = WidgetBaseSetupActivity.getLocationSource(context, widgetId);
                final AppWidgetManager manager = AppWidgetManager.getInstance(context);
                final Calculations.ObserverType location = WidgetBaseSetupActivity.getLocation(context, widgetId);
                final RemoteViews views = getViews(context, widgetClass, widgetId, midPassMs);

                //if for current location
                if(locationSource == Database.LocationType.Current)
                {
                    //if have pass
                    if(havePass)
                    {
                        //update pass
                        updatePass(context, widgetClass, alarmReceiverClass,  widgetId, manager, views, pass, TimeZone.getDefault());
                    }

                    //get resolved location
                    AddressUpdateService.getResolvedLocation(context, location.geo.latitude, location.geo.longitude, new AddressUpdateService.OnLocationResolvedListener()
                    {
                        @Override
                        public void onLocationResolved(String locationString, int resultCode)
                        {
                            String unknown = Globals.getUnknownString(context);
                            String locationName = (locationString == null || locationString.equals("") ? unknown : locationString);

                            //if a known location
                            if(!locationName.equals(unknown))
                            {
                                //update settings
                                WidgetBaseSetupActivity.setLocationName(context, widgetId, locationName);
                                if(!WidgetBaseSetupActivity.getLocationFollow(context, widgetId) && !WidgetBaseSetupActivity.getLocationInterval(context, widgetId))        //if not following or on an interval
                                {
                                    //set to online source to prevent future updates
                                    WidgetBaseSetupActivity.setLocationSource(context, widgetId, Database.LocationType.Online);
                                }
                            }

                            //set location text
                            setViewText(context, widgetClass, widgetId, views, null, R.id.Widget_Pass_Location_Text, locationName);
                            manager.partiallyUpdateAppWidget(widgetId, views);

                            //if have pass
                            if(havePass)
                            {
                                //set next alarm
                                updatePassAlarm(context, alarmReceiverClass, widgetId, pass.passTimeStart, pass.passTimeEnd, true);
                            }
                        }
                    }, true);
                }
                else
                {
                    //get timezone
                    LocationService.GetTimezoneTask timezoneTask = new LocationService.GetTimezoneTask(location.geo.latitude, location.geo.longitude, new LocationService.GetTimezoneTask.OnGotTimezoneListener()
                    {
                        @Override
                        public void onGotTimeZone(String zoneId)
                        {
                            //if have pass
                            if(havePass)
                            {
                                //update pass
                                updatePass(context, widgetClass, alarmReceiverClass, widgetId, manager, views, pass, TimeZone.getTimeZone(zoneId));
                            }

                            //if displaying location
                            if(useNormal)
                            {
                                //set location text
                                setViewText(context, widgetClass, widgetId, views, null, R.id.Widget_Pass_Location_Text, WidgetBaseSetupActivity.getLocationName(context, widgetId));
                                manager.partiallyUpdateAppWidget(widgetId, views);
                            }

                            //if have pass
                            if(havePass)
                            {
                                //set next alarm
                                updatePassAlarm(context, alarmReceiverClass, widgetId, pass.passTimeStart, pass.passTimeEnd, true);
                            }
                        }
                    });
                    timezoneTask.execute(context);
                }
            }
        });
    }

    //Create location receiver
    private static void createLocationReceiver(Context context, boolean runForeground, boolean use)
    {
        //if using and not set
        if(use && locationReceiver == null)
        {
            //listen for location updates
            locationReceiver = new LocationReceiver(LocationService.FLAG_START_GET_LOCATION | (runForeground ? LocationService.FLAG_START_RUN_FOREGROUND : LocationService.FLAG_START_NONE))
            {
                @Override
                protected void onRestart(Context context, boolean close, boolean checkClose)
                {
                    int index;
                    boolean usingFollow = false;

                    //if checking to close
                    if(checkClose)
                    {
                        //go through each widget class while none found using follow
                        for(index = 0; index < widgetClasses.length && !usingFollow; index++)
                        {
                            //check if any widgets of this class are using follow
                            usingFollow = (getWidgetIdList(context, widgetClasses[index], null, FLAG_WIDGET_FOLLOW).size() > 0);
                        }
                    }

                    //if forcing close and not using follow
                    if(close && !usingFollow)
                    {
                        //remove starting in foreground
                        startFlags &= ~LocationService.FLAG_START_RUN_FOREGROUND;
                    }

                    //if any are using follow
                    if(usingFollow)
                    {
                        //send restart
                        super.onRestart(context, false, false);
                    }
                }

                @Override
                protected void onGotLocation(Context context, Calculations.ObserverType updatedObserver)
                {
                    int index;
                    byte locationSource;
                    double julianStart = Calculations.julianDateCalendar(Globals.getGMTTime());
                    ArrayList<Integer> widgetIds;

                    //go through all widget classes
                    for(index = 0; index < widgetClasses.length; index++)
                    {
                        //go through all widgets of this class
                        widgetIds = getWidgetIdList(context, widgetClasses[index]);
                        for(int widgetId : widgetIds)
                        {
                            //if using current location
                            locationSource = WidgetBaseSetupActivity.getLocationSource(context, widgetId);
                            if(locationSource == Database.LocationType.Current)
                            {
                                //save location
                                WidgetBaseSetupActivity.setLocation(context, alarmReceiverClasses[index], widgetId, locationSource, -1, "", updatedObserver.geo.latitude, updatedObserver.geo.longitude, WidgetBaseSetupActivity.getLocationFollow(context, widgetId), WidgetBaseSetupActivity.getLocationInterval(context, widgetId), WidgetBaseSetupActivity.getLocationGlobalInterval(context), true);

                                //calculate with retrieved location
                                CalculateService.calculateOrbitalPass(context, widgetId, getSatellite(widgetId, context), updatedObserver, 0, julianStart, true, createCalculateListener(context, widgetClasses[index], alarmReceiverClasses[index], widgetId));
                            }
                        }
                    }
                }
            };
            locationReceiver.register(context);
        }
        //else if not using and is set
        else if(!use && locationReceiver != null)
        {
            //stop listening
            locationReceiver.unregister(context);
            locationReceiver = null;
        }
    }

    //Updates widget
    public static void updateWidget(final Context context, final Class<?> widgetClass, final Class<?> alarmReceiverClass, final int widgetId, AppWidgetManager manager, RemoteViews views, boolean getPass)
    {
        //update widget
        manager.updateAppWidget(widgetId, views);

        //if getting pass
        if(getPass)
        {
            //get location, satellite, and time range
            final byte locationSource = WidgetBaseSetupActivity.getLocationSource(context, widgetId);
            final double julianStart = Calculations.julianDateCalendar(Globals.getGMTTime());

            //get pass
            if(locationSource == Database.LocationType.Current)
            {
                //get location
                createLocationReceiver(context, WidgetBaseSetupActivity.getLocationFollow(context, widgetId), true);
                LocationService.getCurrentLocation(context, true, WidgetBaseSetupActivity.getLocationFollow(context, widgetId), LocationService.PowerTypes.HighPowerThenBalanced);
            }
            else
            {
                //get location
                final Calculations.ObserverType observer = WidgetBaseSetupActivity.getLocation(context, widgetId);

                //get altitude
                LocationService.GetAltitudeTask altitudeTask = new LocationService.GetAltitudeTask(observer.geo.latitude, observer.geo.longitude, new LocationService.GetAltitudeTask.OnGotAltitudeListener()
                {
                    @Override
                    public void onGotAltitude(Location result)
                    {
                        //set altitude
                        observer.geo.altitudeKm = result.getAltitude() / 1000;

                        //calculate with given location
                        CalculateService.calculateOrbitalPass(context, widgetId, getSatellite(widgetId, context), observer, 0, julianStart, true, createCalculateListener(context, widgetClass, alarmReceiverClass, widgetId));
                    }
                });
                altitudeTask.execute(context);
            }
        }
    }
    public static void updateWidget(Context context, int noradId)
    {
        int widgetId;
        Class<?> widgetClass;
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ArrayList<WidgetData> widgetsData = getWidgetData(context);

        //go through each widget
        for(WidgetData currentData : widgetsData)
        {
            //remember widget ID and class
            widgetId = currentData.widgetId;
            widgetClass = currentData.widgetClass;

            //if widget matches norad ID
            if(WidgetBaseSetupActivity.getNoradID(context, widgetId) == noradId)
            {
                //update the widget
                updateWidget(context, widgetClass, currentData.alarmReceiverClass, widgetId, manager, getViews(context, widgetClass, widgetId), true);
            }
        }
    }
}
