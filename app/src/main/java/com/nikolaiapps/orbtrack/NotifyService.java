package com.nikolaiapps.orbtrack;


import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import java.util.Calendar;


public abstract class NotifyService extends IntentService
{
    //Param Types
    public static abstract class ParamTypes
    {
        static final String MessageType = "messageType";
        static final String ProgressType = "progressType";
        static final String RunForeground = "fromAlarm";
        static final String SubIndex = "subIndex";
        static final String SubCount = "subCount";
        static final String Id = "id";
        static final String Index = "index";
        static final String Count = "count";
        static final String NotifyTitle = "notifyTitle";
        static final String Section = "section";
    }

    //Message types
    public static abstract class MessageTypes
    {
        static final byte General = 0;
        static final byte Download = 1;
        static final byte Load = 2;
        static final byte LoadPercent = 3;
        static final byte Parse = 4;
        static final byte Save = 5;
    }

    //Notification settings
    protected static class NotifySettings
    {
        public String title;
        public String message;
        public int progress;
        public int maxProgress;
        public boolean indeterminate;
        public long timeMs;

        public NotifySettings()
        {
            title = "";
            message = "";
            progress = maxProgress = 0;
            indeterminate = false;
            timeMs = 0;
        }
        public NotifySettings(NotifySettings from)
        {
            title = from.title;
            message = from.message;
            progress = from.progress;
            maxProgress = from.maxProgress;
            indeterminate = from.indeterminate;
            timeMs = from.timeMs;
        }

        public boolean equals(NotifySettings other)
        {
            return(title.equals(other.title) && message.equals(other.message) && progress == other.progress && maxProgress == other.maxProgress && indeterminate == other.indeterminate);
        }
    }

    //Alarm receiver
    public static abstract class NotifyReceiver extends BroadcastReceiver
    {
        public static final String RetryAction = "RetryAction";
        public static final String DismissAction = "DismissAction";
        public static final String SettingsAction = "SettingsAction";
        public static final long MAX_WAKE_LOCK_MS = 900000;             //15 minutes

        protected abstract void onBootCompleted(Context context, long timeNowMs, AlarmManager manager);
        protected abstract void onRunService(Context context, Intent intent);
        protected abstract void onCloseNotification(Context context, Intent intent, NotificationManagerCompat manager);
        protected void onSettings(Context context, Intent intent) {}

        @Override
        public void onReceive(Context context, Intent intent)
        {
            boolean isRetry;
            boolean isDismiss;
            boolean runService = false;
            String action = intent.getAction();

            //if action is not set
            if(action == null)
            {
                //set to empty
                action = "";
            }

            //update status
            isRetry = action.equals(RetryAction);
            isDismiss = action.equals(DismissAction);

            //if from boot completed
            if(action.equals(Intent.ACTION_BOOT_COMPLETED))
            {
                //handle on boot completed
                onBootCompleted(context, Calendar.getInstance().getTimeInMillis(), (AlarmManager)context.getSystemService(Context.ALARM_SERVICE));
            }
            //else if settings
            else if(action.equals(SettingsAction))
            {
                //handle on settings
                onSettings(context, intent);
            }
            //else if retrying or dismissing
            else if(isRetry || isDismiss)
            {
                //close notification
                onCloseNotification(context, intent, NotificationManagerCompat.from(context));

                //run service if retrying
                runService = isRetry;
            }
            else
            {
                //run service
                runService = true;
            }

            //if running service
            if(runService)
            {
                //run service
                onRunService(context, intent);
            }
        }
    }

    private final String wakeTag;
    protected final String notifyChannelId;
    protected NotifySettings currentNotify;
    private NotifySettings lastNotify;
    private NotificationManagerCompat notifyManger;

    public NotifyService(String name, String channelId)
    {
        super(name);
        wakeTag = name;
        notifyChannelId = channelId;
    }

    protected abstract void onRunIntent(Intent intent);
    protected abstract void onClearIntent(byte index);

    @Override
    protected void onHandleIntent(Intent intent)
    {
        PowerManager powerService = (PowerManager)getSystemService(POWER_SERVICE);
        PowerManager.WakeLock keepAwake = (powerService != null ? powerService.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, wakeTag) : null);

        //stay awake
        if(keepAwake != null)
        {
            keepAwake.acquire(NotifyReceiver.MAX_WAKE_LOCK_MS);
        }

        //create notification channel
        Globals.createNotifyChannel(this, notifyChannelId);

        //create notification items
        currentNotify = new NotifySettings();
        lastNotify = new NotifySettings();
        notifyManger = NotificationManagerCompat.from(this);
        currentNotify.progress = 0;
        currentNotify.maxProgress = 0;
        currentNotify.indeterminate = true;

        //run intent
        onRunIntent(intent);

        //don't keep awake
        if(keepAwake != null)
        {
            keepAwake.release();
        }
    }

    //Updates notification and it's visibility
    protected void updateNotification(NotificationCompat.Builder notifyBuilder, int updateID, long limitMs, boolean showNotification)
    {
        long delayMs;

        //if showing notification
        if(showNotification)
        {
            //if notification exists and is changing
            if(notifyBuilder != null && !currentNotify.equals(lastNotify))
            {
                //if there is any limit
                if(limitMs > 0)
                {
                    //handle any wait
                    delayMs = System.currentTimeMillis() - lastNotify.timeMs;
                    if(delayMs < limitMs)
                    {
                        //sleep until at least 1000 ms has passed
                        SystemClock.sleep(limitMs - delayMs);
                    }
                }

                //update notification
                notifyBuilder.setContentTitle(currentNotify.title).setContentText(currentNotify.message);
                notifyBuilder.setProgress(currentNotify.maxProgress, currentNotify.progress, currentNotify.indeterminate);
                currentNotify.timeMs = System.currentTimeMillis();
                lastNotify = new NotifySettings(currentNotify);

                //show notification
                notifyManger.notify(updateID, notifyBuilder.build());
            }
        }
        else
        {
            //close notification
            notifyManger.cancel(updateID);
        }
    }

    //Sends a message
    protected void sendMessage(byte messageType, byte serviceIndex, String serviceParam, int id, String titleDesc, String section, String filter, Class<?> receiverClass, long subIndex, long subCount, long index, long count, int progressType, int updateID, int dismissID, int retryID, boolean showNotification, Bundle extraData)
    {
        boolean isGeneral = (messageType == MessageTypes.General);
        boolean isDownload = (messageType == MessageTypes.Download);
        boolean isFinished = (progressType == Globals.ProgressType.Finished);
        boolean isUpdateSatellites = (serviceIndex == UpdateService.UpdateType.UpdateSatellites);
        long limitMs = 0;
        final long division = (showNotification ? 20 : 100);
        final long baseInterval = (isDownload && !isUpdateSatellites ? Globals.WEB_READ_SIZE : (count / division));
        final long intervals = (baseInterval <= 0 ? 1 : baseInterval > 2048 ? 2048 : baseInterval);
        final long indexInterval = (index / intervals);
        final long indexIntervalStep = (intervals / (intervals >= division ? division : 1));
        boolean sendProgress = true;
        boolean haveSection = (section != null && !section.equals(""));
        boolean enoughProgress = (isFinished || intervals <= 20 || indexInterval == 0 || indexInterval == 1 || index >= (count - 1) || (indexInterval % indexIntervalStep) == 0);      //if no count or less than update divisions, on first index, on last index, or on an allowable index in between
        Intent intent;
        NotificationCompat.Builder notifyBuilder = (showNotification ? Globals.createNotifyBuilder(this, notifyChannelId) : null);

        //update notification
        switch(progressType)
        {
            case Globals.ProgressType.Cancelled:
            case Globals.ProgressType.Failed:
            case Globals.ProgressType.Denied:
            case Globals.ProgressType.Finished:
                //if the main update
                if(isGeneral)
                {
                    //set as done
                    onClearIntent(serviceIndex);

                    //if showing notification
                    if(showNotification)
                    {
                        Intent retryIntent;
                        Intent dismissIntent;
                        Resources res = this.getResources();

                        //create dismiss intent
                        dismissIntent = new Intent(this, receiverClass);
                        dismissIntent.setAction(NotifyReceiver.DismissAction);
                        dismissIntent.putExtra(serviceParam, serviceIndex);

                        //update message
                        currentNotify.title = res.getString(isFinished ? R.string.title_finished : R.string.title_failed) + " " + titleDesc;
                        currentNotify.message = "";
                        currentNotify.progress = 0;
                        currentNotify.maxProgress = 0;
                        currentNotify.indeterminate = false;
                        limitMs = 1000;     //make sure at least 1 second passed from last

                        //if didn't finish
                        if(!isFinished)
                        {
                            //create retry intent
                            retryIntent = new Intent(this, receiverClass);
                            retryIntent.setAction(NotifyReceiver.RetryAction);
                            retryIntent.putExtra(serviceParam, serviceIndex);

                            //add retry button
                            notifyBuilder.addAction(new NotificationCompat.Action(0, res.getString(R.string.title_retry), Globals.getPendingBroadcastIntent(this, retryID, retryIntent, PendingIntent.FLAG_UPDATE_CURRENT)));
                        }

                        //add dismiss button
                        notifyBuilder.addAction(new NotificationCompat.Action(0, res.getString(R.string.title_dismiss), Globals.getPendingBroadcastIntent(this, dismissID, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT)));
                        break;
                    }
                }
                //fall through

            default:
                //if enough progress to send
                if(enoughProgress)
                {
                    //update status
                    currentNotify.message = ((haveSection ? section : "") + (count > 0 ? ("\r\n(" + (int)((index / (float)count) * 100) + "%)") : ""));
                    currentNotify.progress = (int)index;
                    currentNotify.maxProgress = (int)count;
                    currentNotify.indeterminate = false;
                }
                else
                {
                    //don't send
                    sendProgress = false;
                }
                break;
        }

        //if still sending progress
        if(sendProgress)
        {
            //update broadcast
            intent = new Intent(filter);
            intent.putExtra(ParamTypes.MessageType, messageType);
            intent.putExtra(serviceParam, serviceIndex);
            if(haveSection)
            {
                intent.putExtra(ParamTypes.Section, section);
            }
            intent.putExtra(ParamTypes.Id, id);
            intent.putExtra(ParamTypes.SubIndex, subIndex);
            intent.putExtra(ParamTypes.SubCount, subCount);
            intent.putExtra(ParamTypes.Index, index);
            intent.putExtra(ParamTypes.Count, count);
            intent.putExtra(ParamTypes.ProgressType, progressType);
            if(extraData != null)
            {
                intent.putExtras(extraData);
            }

            //send intent and update notification
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            updateNotification(notifyBuilder, updateID, limitMs, showNotification);
        }
    }
}
